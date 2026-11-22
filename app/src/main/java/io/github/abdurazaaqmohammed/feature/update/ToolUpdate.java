package io.github.abdurazaaqmohammed.feature.update;

/**
 * One engine that ships inside the app and has a newer upstream release.
 *
 * These are compiled in (they are Gradle dependencies or vendored sources in
 * {@code apkkit/}), so a tool update is delivered by updating the app itself —
 * it cannot be swapped in at runtime.
 */
public final class ToolUpdate {

    /** Stable id, also the drawable name to look for: "tool_&lt;id&gt;". */
    public final String id;
    /** Display name, e.g. "jadx". */
    public final String name;
    /** Version compiled into this build. */
    public final String bundled;
    /** Newest version published upstream. */
    public final String latest;
    /** Avatar colour used when no tool icon drawable exists. */
    public final int color;

    public ToolUpdate(String id, String name, String bundled, String latest, int color) {
        this.id = id;
        this.name = name;
        this.bundled = bundled;
        this.latest = latest;
        this.color = color;
    }
}
