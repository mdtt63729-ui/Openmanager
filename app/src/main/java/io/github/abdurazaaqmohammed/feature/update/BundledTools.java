package io.github.abdurazaaqmohammed.feature.update;

/**
 * Versions of the engines compiled into this build.
 *
 * Bump these whenever the matching dependency or vendored source is updated:
 *   jadx        -> app/build.gradle (io.github.skylot:jadx-*)
 *   smali       -> apkkit/src/main/java/com/android/tools/smali (see apkkit/UPSTREAM.md)
 *   APKEditor   -> apkkit/src/main/java/com/reandroid (see apkkit/UPSTREAM.md)
 *
 * The updater compares them with the newest upstream release and lists only
 * the engines that are actually behind.
 */
public final class BundledTools {

    public static final String JADX = "1.5.6";
    public static final String SMALI = "3.0.6";
    public static final String APKEDITOR = "1.4.9";

    private BundledTools() {}
}
