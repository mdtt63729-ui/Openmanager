package io.github.abdurazaaqmohammed.feature.update;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds engines bundled in this build that are behind their upstream release.
 *
 * Sources: Maven Central metadata for jadx, GitHub tags for the vendored
 * smali and APKEditor trees. Every lookup is independent, so one failing host
 * only hides its own row.
 */
public final class ToolUpdateChecker {

    private static final String JADX_METADATA =
            "https://repo.maven.apache.org/maven2/io/github/skylot/jadx-core/maven-metadata.xml";
    private static final String SMALI_LATEST =
            "https://api.github.com/repos/google/smali/releases/latest";
    private static final String APKEDITOR_TAGS =
            "https://api.github.com/repos/REAndroid/APKEditor/tags";

    private static final Pattern RELEASE_TAG = Pattern.compile("<release>([^<]+)</release>");
    private static final Pattern JSON_TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    private static final int JADX_COLOR = 0xFF1B5E20;
    private static final int SMALI_COLOR = 0xFF37474F;
    private static final int APKEDITOR_COLOR = 0xFF4527A0;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(List<ToolUpdate> tools);
    }

    private ToolUpdateChecker() {}

    public static void check(Callback callback) {
        new Thread(() -> {
            List<ToolUpdate> out = new ArrayList<>();

            addIfNewer(out, "jadx", "jadx", BundledTools.JADX, JADX_COLOR,
                    latestFromMaven(JADX_METADATA));
            addIfNewer(out, "smali", "smali / baksmali", BundledTools.SMALI, SMALI_COLOR,
                    latestFromGithub(SMALI_LATEST, JSON_TAG_NAME));
            addIfNewer(out, "apkeditor", "APKEditor", BundledTools.APKEDITOR, APKEDITOR_COLOR,
                    latestFromGithub(APKEDITOR_TAGS, JSON_NAME));

            MAIN.post(() -> callback.onResult(out));
        }).start();
    }

    private static void addIfNewer(List<ToolUpdate> out, String id, String name,
                                   String bundled, int color, String latest) {
        if (latest == null || latest.isEmpty()) return;
        String clean = stripPrefix(latest);
        if (isNewer(clean, bundled)) {
            out.add(new ToolUpdate(id, name, bundled, clean, color));
        }
    }

    /** "v1.5.6", "V1.4.9" and "3.0.6" all become plain versions. */
    private static String stripPrefix(String tag) {
        String t = tag.trim();
        while (t.length() > 0 && (t.charAt(0) == 'v' || t.charAt(0) == 'V')) t = t.substring(1);
        return t;
    }

    /** Numeric comparison, part by part: 1.5.10 is newer than 1.5.6. */
    static boolean isNewer(String candidate, String current) {
        if (current == null || current.isEmpty()) return true;
        String[] a = candidate.split("[^0-9]+");
        String[] b = current.split("[^0-9]+");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int x = part(a, i);
            int y = part(b, i);
            if (x != y) return x > y;
        }
        return false;
    }

    private static int part(String[] parts, int i) {
        if (i >= parts.length || parts[i].isEmpty()) return 0;
        try {
            return Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String latestFromMaven(String url) {
        String body = read(url);
        if (body == null) return null;
        Matcher m = RELEASE_TAG.matcher(body);
        return m.find() ? m.group(1) : null;
    }

    private static String latestFromGithub(String url, Pattern pattern) {
        String body = read(url);
        if (body == null) return null;
        Matcher m = pattern.matcher(body);
        return m.find() ? m.group(1) : null;
    }

    private static String read(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "MP-Manager-update-check");
            try (InputStream in = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
