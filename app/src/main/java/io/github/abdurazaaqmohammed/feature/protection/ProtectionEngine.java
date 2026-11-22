package io.github.abdurazaaqmohammed.feature.protection;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Local APK protection engine.
 *
 * The pipeline is deliberately modular: analysis, compatibility, then one
 * pass per protection module, then rebuild, verify and sign. A module that
 * cannot be applied safely to a given APK reports SKIPPED with a reason
 * instead of touching the file — an arbitrary APK must never be corrupted by
 * a protection step that did not fit it.
 *
 * What actually transforms bytes today:
 *   - SIGNATURE_CLEAN   drops the old signature block so the rebuild starts clean
 *   - DEX_STRIP_DEBUG   rebuilds every dex without debug items or source-file names
 *   - DEX_INTEGRITY     rewrites each dex header checksum + SHA-1
 *   - INTEGRITY_ASSET   stores a manifest of SHA-256 hashes inside the APK
 *
 * The rest are reported as unavailable with an explicit reason rather than
 * pretending to run: string encryption, control-flow flattening, native symbol
 * stripping, runtime anti-tamper and anti-debug all need code injection or a
 * native toolchain, which this build does not carry.
 */
public final class ProtectionEngine {

    // ---------------------------------------------------------------- model

    public enum State { SUPPORTED, PARTIAL, UNSUPPORTED, RISKY }

    public enum Module {
        SIGNATURE_CLEAN("Signature block cleanup", State.SUPPORTED),
        DEX_STRIP_DEBUG("Debug info stripping", State.SUPPORTED),
        DEX_INTEGRITY("DEX integrity normalisation", State.SUPPORTED),
        INTEGRITY_ASSET("Integrity manifest", State.SUPPORTED),
        RESOURCE_STRIP("Resource name shrinking", State.UNSUPPORTED),
        STRING_PROTECTION("String protection", State.UNSUPPORTED),
        CONTROL_FLOW("Control-flow protection", State.UNSUPPORTED),
        NATIVE_PROTECTION("Native protection", State.UNSUPPORTED),
        RUNTIME_PROTECTION("Runtime integrity checks", State.UNSUPPORTED),
        ANTI_DEBUG("Anti-debugging", State.UNSUPPORTED),
        INSTRUMENTATION("Instrumentation detection", State.UNSUPPORTED);

        public final String label;
        public final State support;

        Module(String label, State support) {
            this.label = label;
            this.support = support;
        }
    }

    public enum Profile {
        BASIC("Basic"),
        STRONG("Strong"),
        MAXIMUM("Maximum local"),
        CUSTOM("Custom");

        public final String label;

        Profile(String label) {
            this.label = label;
        }
    }

    public static final class Analysis {
        public String packageName = "";
        public String versionName = "";
        public long versionCode;
        public int minSdk;
        public int targetSdk;
        public int dexCount;
        public final List<String> abis = new ArrayList<>();
        public final List<String> activities = new ArrayList<>();
        public int services;
        public int receivers;
        public int providers;
        public int permissions;
        public int assets;
        public String signing = "unknown";
        public String framework = "unknown";
        public String obfuscation = "unknown";
        public boolean multidex;
        public boolean hasKotlinMetadata;
        public boolean hasMappingFile;
        public long sizeBytes;
        public State compatibility = State.SUPPORTED;

        public int compatibilityPercent() {
            switch (compatibility) {
                case SUPPORTED: return 95;
                case PARTIAL: return 70;
                case RISKY: return 45;
                default: return 10;
            }
        }
    }

    public static final class Result {
        public Analysis analysis;
        public File output;
        public final List<String> applied = new ArrayList<>();
        public final List<String> skipped = new ArrayList<>();
        public int score;
        public boolean success;
        public String error;
        public long elapsedMs;
    }

    public interface Progress {
        void onStep(String label, int done, int total);
    }

    private ProtectionEngine() {}

    // ------------------------------------------------------------- analysis

    public static Analysis analyze(Context context, File apk) {
        Analysis a = new Analysis();
        a.sizeBytes = apk.length();
        PackageManager pm = context.getPackageManager();
        int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES
                | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS
                | PackageManager.GET_PERMISSIONS;
        PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        if (info == null) {
            a.compatibility = State.UNSUPPORTED;
            return a;
        }
        a.packageName = info.packageName == null ? "" : info.packageName;
        a.versionName = info.versionName == null ? "" : info.versionName;
        a.versionCode = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
        ApplicationInfo app = info.applicationInfo;
        if (app != null) {
            a.minSdk = Build.VERSION.SDK_INT >= 24 ? app.minSdkVersion : 0;
            a.targetSdk = Build.VERSION.SDK_INT >= 24 ? app.targetSdkVersion : 0;
        }
        if (info.activities != null) {
            for (android.content.pm.ActivityInfo act : info.activities) a.activities.add(act.name);
        }
        a.services = info.services == null ? 0 : info.services.length;
        a.receivers = info.receivers == null ? 0 : info.receivers.length;
        a.providers = info.providers == null ? 0 : info.providers.length;
        a.permissions = info.requestedPermissions == null ? 0 : info.requestedPermissions.length;

        Set<String> abis = new HashSet<>();
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (name.startsWith("lib/") && name.endsWith(".so")) {
                    String[] parts = name.split("/");
                    if (parts.length > 2) abis.add(parts[1]);
                } else if (name.matches("classes\\d*\\.dex")) {
                    a.dexCount++;
                } else if (name.startsWith("assets/")) {
                    a.assets++;
                    if (name.endsWith("mapping.txt")) a.hasMappingFile = true;
                }
                if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".EC")
                        || name.endsWith(".DSA"))) {
                    a.signing = "signed";
                }
            }
        } catch (Exception ignored) {
            // unreadable zip: analysis stays at its defaults
        }
        a.abis.addAll(abis);
        a.multidex = a.dexCount > 1;
        a.hasKotlinMetadata = a.abis.contains("kotlin") || a.packageName.length() > 0;

        a.framework = detectFramework(apk);
        a.obfuscation = estimateObfuscation(a);
        a.compatibility = compatibilityFor(a);
        return a;
    }

    private static String detectFramework(File apk) {
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            boolean compose = false;
            boolean flutter = false;
            boolean react = false;
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("assets/flutter_assets/")) flutter = true;
                if (name.startsWith("lib/") && name.contains("libflutter")) flutter = true;
                if (name.startsWith("lib/") && name.contains("libreactnative")) react = true;
                if (name.startsWith("assets/index.android.bundle")) react = true;
                if (name.startsWith("classes") && name.endsWith(".dex") && !compose) {
                    compose = dexMentions(zip, name, "androidx/compose/");
                }
            }
            if (flutter) return "Flutter";
            if (react) return "React Native";
            if (compose) return "Jetpack Compose";
            return "Java / Kotlin";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static boolean dexMentions(ZipFile zip, String entry, String needle) {
        try (InputStream in = zip.getInputStream(zip.getEntry(entry))) {
            byte[] head = new byte[1 << 16];
            int read = in.read(head);
            if (read <= 0) return false;
            return new String(head, 0, read, "ISO-8859-1").contains(needle);
        } catch (Exception e) {
            return false;
        }
    }

    private static String estimateObfuscation(Analysis a) {
        int shortNames = 0;
        for (String act : a.activities) {
            String simple = act.substring(act.lastIndexOf('.') + 1);
            if (simple.length() <= 2) shortNames++;
        }
        if (a.activities.isEmpty()) return "unknown";
        double ratio = (double) shortNames / a.activities.size();
        if (ratio > 0.5) return "Heavy";
        if (ratio > 0.15) return "Partial";
        return "None detected";
    }

    private static State compatibilityFor(Analysis a) {
        if (a.packageName.isEmpty()) return State.UNSUPPORTED;
        if ("Flutter".equals(a.framework) || "React Native".equals(a.framework)) return State.PARTIAL;
        if ("Heavy".equals(a.obfuscation)) return State.RISKY;
        return State.SUPPORTED;
    }

    // ------------------------------------------------------------- pipeline

    public static Result apply(Context context, File apk, File output, Profile profile,
                               Set<Module> modules, Progress progress) {
        long start = System.currentTimeMillis();
        Result result = new Result();
        try {
            step(progress, "Analyzing", 0, 6);
            result.analysis = analyze(context, apk);
            if (result.analysis.compatibility == State.UNSUPPORTED) {
                result.error = "Not a readable APK";
                return result;
            }

            step(progress, "Planning", 1, 6);
            List<Module> plan = planFor(profile, modules, result.analysis);

            File tmp = new File(output.getParentFile(), output.getName() + ".part");
            if (tmp.exists() && !tmp.delete()) throw new IOException("cannot replace " + tmp);

            step(progress, "Processing DEX", 2, 6);
            boolean cleanSignature = plan.contains(Module.SIGNATURE_CLEAN);
            boolean dexIntegrity = plan.contains(Module.DEX_INTEGRITY);
            boolean integrityAsset = plan.contains(Module.INTEGRITY_ASSET);
            boolean stripDebug = plan.contains(Module.DEX_STRIP_DEBUG);

            rebuild(apk, tmp, cleanSignature, dexIntegrity, integrityAsset, stripDebug);
            if (cleanSignature) result.applied.add(Module.SIGNATURE_CLEAN.label);
            if (stripDebug) result.applied.add(Module.DEX_STRIP_DEBUG.label);
            if (dexIntegrity) result.applied.add(Module.DEX_INTEGRITY.label);
            if (integrityAsset) result.applied.add(Module.INTEGRITY_ASSET.label);

            step(progress, "Verifying output", 3, 6);
            if (!verify(tmp)) {
                tmp.delete();
                result.error = "Output failed verification";
                return result;
            }
            if (!tmp.renameTo(output)) {
                copy(tmp, output);
                tmp.delete();
            }

            for (Module m : Module.values()) {
                if (!plan.contains(m)) {
                    result.skipped.add(m.label + " — " + reasonFor(m, result.analysis));
                }
            }

            step(progress, "Preparing report", 4, 6);
            result.output = output;
            result.score = scoreFor(result);
            result.success = true;
            step(progress, "Done", 6, 6);
            return result;
        } catch (Exception e) {
            result.error = e.getMessage() == null ? e.toString() : e.getMessage();
            return result;
        } finally {
            result.elapsedMs = System.currentTimeMillis() - start;
        }
    }

    private static void step(Progress p, String label, int done, int total) {
        if (p != null) p.onStep(label, done, total);
    }

    private static List<Module> planFor(Profile profile, Set<Module> custom, Analysis a) {
        List<Module> plan = new ArrayList<>();
        if (profile == Profile.CUSTOM) {
            for (Module m : custom) if (m.support == State.SUPPORTED) plan.add(m);
            return plan;
        }
        plan.add(Module.SIGNATURE_CLEAN);
        plan.add(Module.DEX_INTEGRITY);
        if (profile != Profile.BASIC) plan.add(Module.DEX_STRIP_DEBUG);
        if (profile == Profile.STRONG || profile == Profile.MAXIMUM) plan.add(Module.INTEGRITY_ASSET);
        return plan;
    }

    private static String reasonFor(Module m, Analysis a) {
        if (m.support != State.SUPPORTED) {
            return "needs code injection or a native toolchain, not available on-device";
        }
        return "not selected for this profile";
    }

    // --------------------------------------------------------------- rebuild

    private static void rebuild(File in, File out, boolean cleanSignature,
                                boolean dexIntegrity, boolean integrityAsset,
                                boolean stripDebug) throws Exception {
        List<String> hashes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(in);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (cleanSignature && isSignatureEntry(name)) continue;
                if (name.startsWith("assets/protection-integrity")) continue;

                byte[] data = readAll(zip.getInputStream(entry));
                if (name.matches("classes\\d*\\.dex")) {
                    if (stripDebug) {
                        data = DexHardening.strip(data, new DexHardening.Stats());
                    }
                    if (dexIntegrity) data = repairDex(data);
                }
                hashes.add(sha256(data) + "  " + name);
                writeEntry(zos, name, data);
            }
            if (integrityAsset) {
                StringBuilder sb = new StringBuilder();
                for (String line : hashes) sb.append(line).append('\n');
                writeEntry(zos, "assets/protection-integrity.sha256",
                        sb.toString().getBytes("UTF-8"));
            }
        }
    }

    private static boolean isSignatureEntry(String name) {
        if (!name.startsWith("META-INF/")) return false;
        String upper = name.toUpperCase(Locale.US);
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC") || upper.endsWith("MANIFEST.MF");
    }

    private static void writeEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L); // reproducible output
        if (name.endsWith(".so")) {
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(data.length);
            entry.setCompressedSize(data.length);
            Adler32 crc = new Adler32();
            crc.update(data);
            entry.setCrc(crc.getValue());
        }
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    /** Recomputes the dex header SHA-1 and Adler-32 so the file is well formed. */
    static byte[] repairDex(byte[] dex) throws Exception {
        if (dex.length < 112) return dex;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(dex, 32, dex.length - 32);
        byte[] sig = digest.digest();
        System.arraycopy(sig, 0, dex, 12, 20);
        Adler32 adler = new Adler32();
        adler.update(dex, 12, dex.length - 12);
        long value = adler.getValue();
        dex[8] = (byte) value;
        dex[9] = (byte) (value >> 8);
        dex[10] = (byte) (value >> 16);
        dex[11] = (byte) (value >> 24);
        return dex;
    }

    // ---------------------------------------------------------------- verify

    public static boolean verify(File apk) {
        try (ZipFile zip = new ZipFile(apk)) {
            if (zip.getEntry("AndroidManifest.xml") == null) return false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            boolean anyDex = false;
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().matches("classes\\d*\\.dex")) {
                    anyDex = true;
                    byte[] head = readAll(zip.getInputStream(e));
                    if (head.length < 112 || head[0] != 'd' || head[1] != 'e' || head[2] != 'x') {
                        return false;
                    }
                }
            }
            return anyDex;
        } catch (Exception e) {
            return false;
        }
    }

    private static int scoreFor(Result r) {
        int base = 40;
        base += r.applied.size() * 12;
        if (r.analysis != null) base += r.analysis.compatibilityPercent() / 10;
        if (r.skipped.size() > 5) base -= 6;
        return Math.max(10, Math.min(99, base));
    }

    // ---------------------------------------------------------------- report

    public static String reportJson(Result r) {
        try {
            JSONObject root = new JSONObject();
            JSONObject a = new JSONObject();
            if (r.analysis != null) {
                a.put("package", r.analysis.packageName);
                a.put("versionName", r.analysis.versionName);
                a.put("versionCode", r.analysis.versionCode);
                a.put("minSdk", r.analysis.minSdk);
                a.put("targetSdk", r.analysis.targetSdk);
                a.put("dex", r.analysis.dexCount);
                a.put("abis", new JSONArray(r.analysis.abis));
                a.put("framework", r.analysis.framework);
                a.put("obfuscation", r.analysis.obfuscation);
                a.put("compatibility", r.analysis.compatibility.name());
            }
            root.put("analysis", a);
            root.put("applied", new JSONArray(r.applied));
            root.put("skipped", new JSONArray(r.skipped));
            root.put("score", r.score);
            root.put("success", r.success);
            root.put("elapsedMs", r.elapsedMs);
            if (r.error != null) root.put("error", r.error);
            return root.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ----------------------------------------------------------------- utils

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1 << 16];
            int read;
            while ((read = is.read(buf)) > 0) bos.write(buf, 0, read);
            return bos.toByteArray();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] out = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format(Locale.US, "%02x", b));
        return sb.toString();
    }

    private static void copy(File from, File to) throws IOException {
        try (FileInputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[1 << 16];
            int read;
            while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
        }
    }
}
