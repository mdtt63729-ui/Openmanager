package io.github.abdurazaaqmohammed.feature.protection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Owner marker written into a protected APK.
 *
 * The marker records which modules ran and, when a code was issued, a salted
 * PBKDF2 hash of that code. This app can therefore recognise its own output and
 * ask for the code before it takes the protection back off. Another tool sees
 * an ordinary APK with one extra asset.
 *
 * Limits, stated plainly: the hash is local, so a determined attacker can brute
 * force a short code offline, and anyone reading this class learns nothing that
 * helps — the code itself is never stored, only its hash. The marker hides no
 * code; it gates the undo step.
 */
public final class ProtectionMarker {

    public static final String ASSET = "assets/protection-marker.json";
    private static final String INTEGRITY_ASSET = "assets/protection-integrity.sha256";
    private static final int ITERATIONS = 12000;
    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private ProtectionMarker() {}

    // ------------------------------------------------------------------ code

    /** Human friendly code: XXXX-XXXX-XXXX-XXXX, no look-alike characters. */
    public static String newCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- embed

    /** Writes the marker into {@code apk}. {@code code} may be null for no lock. */
    public static boolean embed(File apk, List<String> modules, String code) {
        File tmp = new File(apk.getParentFile(), apk.getName() + ".marker");
        try {
            byte[] marker = payload(modules, code).getBytes("UTF-8");
            try (ZipFile zip = new ZipFile(apk);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmp))) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.isDirectory() || ASSET.equals(e.getName())) continue;
                    copyEntry(zos, e.getName(), readAll(zip.getInputStream(e)));
                }
                copyEntry(zos, ASSET, marker);
            }
            if (!tmp.renameTo(apk)) throw new IOException("cannot replace " + apk);
            return true;
        } catch (Exception e) {
            tmp.delete();
            return false;
        }
    }

    // ----------------------------------------------------------------- read

    /** Marker contents, or null when the APK is not ours. */
    public static JSONObject read(File apk) {
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry(ASSET);
            if (entry == null) return null;
            return new JSONObject(new String(readAll(zip.getInputStream(entry)), "UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isOurs(File apk) {
        return read(apk) != null;
    }

    public static boolean hasCode(File apk) {
        JSONObject json = read(apk);
        return json != null && !json.optString("hash", "").isEmpty();
    }

    public static List<String> modulesOf(JSONObject marker) {
        List<String> out = new ArrayList<>();
        if (marker == null) return out;
        JSONArray array = marker.optJSONArray("modules");
        if (array == null) return out;
        for (int i = 0; i < array.length(); i++) out.add(array.optString(i));
        return out;
    }

    /** True when {@code code} opens this APK. Constant-time comparison. */
    public static boolean verifyCode(File apk, String code) {
        JSONObject json = read(apk);
        if (json == null) return false;
        String stored = json.optString("hash", "");
        String salt = json.optString("salt", "");
        if (stored.isEmpty() || salt.isEmpty()) return false;
        String candidate = hash(normalise(code), salt);
        return MessageDigest.isEqual(stored.getBytes(), candidate.getBytes());
    }

    /** "abcd-efgh" and "ABCDEFGH" are the same code. */
    private static String normalise(String code) {
        if (code == null) return "";
        return code.replace("-", "").replace(" ", "").trim().toUpperCase(Locale.US);
    }

    // ---------------------------------------------------------------- strip

    /**
     * Copies {@code in} to {@code out} with the marker, the integrity manifest
     * and any signature block removed — the owner's unlock.
     */
    public static boolean strip(File in, File out) {
        try (ZipFile zip = new ZipFile(in);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (ASSET.equals(name) || INTEGRITY_ASSET.equals(name)) continue;
                if (isSignatureEntry(name)) continue;
                copyEntry(zos, name, readAll(zip.getInputStream(e)));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------- internals

    private static String payload(List<String> modules, String code) throws Exception {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        for (String m : modules) array.put(m);
        json.put("modules", array);
        json.put("ts", System.currentTimeMillis());
        json.put("app", io.github.abdurazaaqmohammed.MPManager.BuildConfig.APPLICATION_ID);
        if (code != null && !code.isEmpty()) {
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            String salt = hex(saltBytes);
            json.put("salt", salt);
            json.put("hash", hash(normalise(code), salt));
        }
        return json.toString();
    }

    /** PBKDF2-HMAC-SHA1, 12k rounds: cheap on a phone, slow to brute force. */
    private static String hash(String code, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), fromHex(salt),
                    ITERATIONS, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return hex(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            return "";
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format(Locale.US, "%02x", b));
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        int len = hex.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static boolean isSignatureEntry(String name) {
        if (!name.startsWith("META-INF/")) return false;
        String upper = name.toUpperCase(Locale.US);
        return upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA")
                || upper.endsWith(".EC") || upper.endsWith("MANIFEST.MF");
    }

    private static void copyEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1 << 16];
            int read;
            while ((read = is.read(buf)) > 0) bos.write(buf, 0, read);
            return bos.toByteArray();
        }
    }
}
