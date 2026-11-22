# Vendored engines in `apkkit`

These are copied source trees, not Gradle dependencies, so Dependabot/Renovate
cannot update them. Each one has to be re-synced by hand.

| Engine | Upstream | Where it lives | Notes |
|---|---|---|---|
| APKEditor (REAndroid) | https://github.com/REAndroid/APKEditor | `apkkit/src/main/java/com/reandroid/**` | Fork with local changes (protect check, logging hooks). Upstream tag at time of sync: V1.4.9 |
| smali / baksmali / dexlib2 | https://github.com/google/smali | `apkkit/src/main/java/com/android/tools/smali/**` (685 files) | Modified fork: 231 files differ from upstream 3.0.6 and 49 extra files exist, so a version bump means re-applying local patches, not a drop-in copy |
| arsc-parser | bundled jar | `app/libs/arsc-parser.jar` | No source in repo |

Dependencies that *are* declared and therefore auto-updatable live in
`app/build.gradle` (jadx 1.5.6, apksig, BouncyCastle, commons-*, zip4j, guava)
and `apkkit/build.gradle`.
