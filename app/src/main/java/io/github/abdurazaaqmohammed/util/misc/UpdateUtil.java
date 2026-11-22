package io.github.abdurazaaqmohammed.util.misc;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.abdurazaaqmohammed.MPManager.R;
import io.github.abdurazaaqmohammed.feature.update.UpdateBottomSheet;
import io.github.codehasan.colorpicker.extensions.Extensions;

public class UpdateUtil {

    public static final String PREF_LAST_VER_CHECKED = "lastVerChecked";
    public static final String PREF_DOWNLOAD_ID = "downloadId";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** Tag that names an app version: "1.0.9" or "v1.0.9". */
    private static final Pattern VERSION_TAG =
            Pattern.compile("^v?(\\d+(?:\\.\\d+)*)$");

    /** Release APK asset prefix, configured in gradle.properties. */
    private static final String APP_APK_PREFIX =
            io.github.abdurazaaqmohammed.MPManager.BuildConfig.UPDATE_APK_PREFIX.toLowerCase();

    /** An MP-Manager release: newest first, tag, APK asset and changelog. */
    private static final class AppRelease {
        String version;
        String apkName;
        String apkUrl;
        String changelog;
    }

    public static void checkForUpdates(boolean toast, AppCompatActivity context) {
        Resources rss = context.getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastVerChecked = prefs.getString(PREF_LAST_VER_CHECKED, null);
        new Thread(() -> {
            try {
                String json = read(getHttpURLConnection());
                String currentVer;
                try {
                    currentVer = (context).getPackageManager().getPackageInfo((context).getPackageName(), 0).versionName;
                } catch (Exception e) {
                    currentVer = null;
                }
                AppRelease release = latestAppRelease(json);
                if (release == null) {
                    if (toast) Extensions.showMessage(context, R.string.no_update_found);
                    return;
                }
                char[] curr = TextUtils.isEmpty(currentVer) ? new char[] { '1', '0', '9' }
                        : currentVer.replace(".", "").toCharArray();
                char[] latest = release.version.replace(".", "").toCharArray();
                boolean newVer = false;
                int maxLength = Math.max(curr.length, latest.length);
                for (int i = 0; i < maxLength; i++) {
                    char currChar = i < curr.length ? curr[i] : '0';
                    char latestChar = i < latest.length ? latest[i] : '0';

                    if (latestChar > currChar) {
                        newVer = true;
                        break;
                    } else if (latestChar < currChar) {
                        break;
                    }
                }
                if (!newVer) {
                    if (toast) Extensions.showMessage(context, R.string.no_update_found);
                    return;
                }
                if (!toast && !TextUtils.isEmpty(lastVerChecked) && lastVerChecked.equals(release.version)) {
                    return;
                }
                // The version, the changelog and the in-app download live in the
                // bottom sheet; it also records PREF_LAST_VER_CHECKED on dismiss.
                MAIN_HANDLER.post(() -> {
                    if (context.isFinishing() || context.isDestroyed()) return;
                    UpdateBottomSheet.newInstance(release.version, release.apkName,
                                    release.apkUrl, release.changelog)
                            .show(context.getSupportFragmentManager(), "update_sheet");
                });
            } catch (Exception e) {
                if (toast) Extensions.showMessage(context, R.string.failed_to_check_for_update);
            }
        }).start();
    }

    /**
     * Newest release that is actually the app: a version tag plus an
     * MP-Manager.*.apk asset. The repo also hosts distribution releases
     * (tool-packs, native-tools) that share this API but are not app
     * updates, so they must not be offered as one.
     */
    private static AppRelease latestAppRelease(String json) throws Exception {
        JSONArray releases = new JSONArray(json);
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.optJSONObject(i);
            if (release == null) continue;
            String tag = release.optString("tag_name", "").trim();
            Matcher tagMatcher = VERSION_TAG.matcher(tag);
            if (!tagMatcher.find()) continue;
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) continue;
            for (int j = 0; j < assets.length(); j++) {
                JSONObject asset = assets.optJSONObject(j);
                if (asset == null) continue;
                String name = asset.optString("name", "");
                String url = asset.optString("browser_download_url", "");
                if (!name.toLowerCase().startsWith(APP_APK_PREFIX) || url.isEmpty()) continue;
                AppRelease out = new AppRelease();
                out.version = tagMatcher.group(1);
                out.apkName = name;
                out.apkUrl = url;
                out.changelog = release.optString("body", "");
                return out;
            }
        }
        return null;
    }

    private static String read(HttpURLConnection conn) throws IOException {
        try (InputStream inputStream = conn.getInputStream();
             InputStreamReader in = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(in)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    @NonNull
    private static HttpURLConnection getHttpURLConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/"
                + io.github.abdurazaaqmohammed.MPManager.BuildConfig.UPDATE_REPO + "/releases").openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        return conn;
    }
}