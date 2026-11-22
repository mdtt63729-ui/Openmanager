<h1 align="center">Open Manager</h1>

<p align="center">
  A free, open-source, dual-pane file manager for Android with a full APK
  workbench: sign, decompile, edit dex and resources, patch and dump — plus
  media tools, a shell, OCR and a built-in MCP server for AI tooling.
</p>

## Features

| Area | Highlights |
|---|---|
| File manager | Dual-pane navigation, advanced search (including inside archives), archives browsed as folders (ZIP, RAR, zst, backups), recycle bin, checksums, root / Shizuku / Dhizuku access |
| Network | FTP server and client with FTPS, WebDAV and SMB clients, server discovery, encrypted saved profiles |
| Media | Audio and video player with background mini player, image editor, EXIF editing, collage studio |
| APK tools | Install and sign APKs, optional protection step before signing, decompile and build, dex and ARSC editors, manifest quick edit, LSPatch integrated mode, IL2CPP dumper, Jiagu unpacker, native library patcher |
| Editors | Text editor with syntax highlighting and AXML decoding, hex editor, compare text, archives, resources and dex files |
| Tools | Command shell, Smart App Ops, SetEdit, SQLite viewer and editor, partition dumper, boot image tools, ELF inspector, Logcat reader, OCR, plugins and tool packs |
| AI and MCP | Built-in MCP server: let AI tools drive the app's file, ZIP and APK engines with a strict access policy |
| Updates | In-app update sheet: release notes, old → new version, download and install without leaving the app |

## Build

```bash
./gradlew assembleFullRelease     # full flavour (includes the LSPatch engine)
./gradlew assembleLiteRelease     # lite flavour
```

Requirements: JDK 21, Android SDK 37, NDK 29 (only when the prebuilt zstd
library is missing). Minimum Android version is 6.0 (API 23).

Release builds produce a universal APK plus per-ABI APKs
(`arm64-v8a`, `armeabi-v7a`, `x86_64`) thanks to ABI splits.

## Configure your fork

Two properties in `gradle.properties` decide where the app looks for its own
updates. Nothing in the code points at any other repository:

```properties
updateRepo=YOUR_GITHUB_USER/Open-Manager
updateApkPrefix=open-manager.
```

- `updateRepo` — `owner/repo` whose **releases** are checked by the in-app
  updater and linked from the About screen.
- `updateApkPrefix` — lower-case prefix of the release APK asset name, e.g.
  `open-manager.1.2.0.apk`.

The tool packs in `app/src/main/assets/packs.json` and the native tools in
`NativeToolManager` use the same repository, so publish a `tool-packs-v1` and a
`native-tools-v1` release there if you want those downloads to work.

## Releases

`.github/workflows/release.yml` builds both flavours on every `v*` tag and
attaches every APK (universal + per ABI) to the GitHub Release.

## License

GPL-3.0. See [LICENSE](LICENSE). This project is a fork of an earlier GPL-3.0
Android file manager; the licence and the copyright notices in the source files
are kept as the GPL requires. Rebranding and re-pointing the update channel do
not remove those notices.
