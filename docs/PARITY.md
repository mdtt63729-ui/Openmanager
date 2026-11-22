# MT Manager parity

What MT Manager offers, what this app does about it, and where the code lives.
The MT column follows the vendor's VIP feature list (mt.cc/vip) plus the
feature table published with 2.26.x. "Free" here means it costs nothing in
MP Manager — everything below is GPLv3, nothing is behind a paywall.

Legend: **yes** = implemented and wired · **partial** = works, narrower than MT
· **no** = not implemented

## File manager

| Feature | MT | MP Manager | Where |
|---|---|---|---|
| Dual pane, root, batch ops | yes | yes | `ui/main/MainActivity.java` |
| Archives browsed as folders (zip/rar/zst) | yes | yes | `feature/files` |
| Recycle bin, checksums | yes | yes | `feature/files` |
| Search inside archives | partial | yes | `feature/files` |
| Root / Shizuku / Dhizuku | root | yes, all three | `privilege/` |

## Network storage

| Feature | MT | MP Manager | Where |
|---|---|---|---|
| FTP server + client, FTPS | yes (FTPS = VIP) | yes, free | `feature/ftp` |
| SMB, WebDAV | VIP | yes, free | `feature/files/network` |
| SFTP | VIP | **no** | needs an SSH library |
| Encrypted saved profiles | partial | yes | `NetworkProfileStore` |

## DEX

| Feature | MT | MP Manager | Where |
|---|---|---|---|
| Dex editor (classes/methods/fields) | yes | yes | `modder/hub/dexeditor` |
| Dex editor ++ (multi-file, search refs, jumps) | yes | yes | `DexEditorActivity` |
| Smali → Java | 8 engines | **jadx only** | `Smali2Java.java` |
| Java → smali / rebuild | yes | yes (`Smali.assemble`) | `DexEditorActivity` |
| Dex repair | VIP | yes, free | `DexTools.repairDex` |
| Dex merge / redivide | VIP | merge yes, split no | `DexTools.mergeDexOption` |
| Dex string replace | yes | yes | `showDexStringReplaceDialog` |
| Dex string decrypt | VIP | yes, free | `showDexStringDecryptDialog` |
| Anti-confusion (obfuscated names) | VIP | yes, free | `feature/apk/DexAntiConfuse.java` |
| Dex compare | VIP | yes, free | `feature/diff` |
| Method flowchart | VIP | **no** | |

## Resources

| Feature | MT | MP Manager | Where |
|---|---|---|---|
| ARSC editor | yes | yes | `feature/apk` ARSC editor |
| ARSC editor ++ | VIP | yes, free | `ArscEditorPlusActivity` |
| Resource query | VIP | yes, free | `querier_title` flow |
| ARSC compare | VIP | yes, free | `feature/diff` |
| AXML decompile / rebuild | yes (size limit free) | yes, unlimited, free | `apkkit` XmlCoder |
| XML search / replace | VIP | partial | text search only |
| Add/remove localisations, translate | yes | yes | `feature/translation` |

## APK

| Feature | MT | MP Manager | Where |
|---|---|---|---|
| Sign, batch sign | yes | yes | `ApkSignatureTools` |
| Custom keystore / JKS import | VIP | yes, free | `SignatureKeyDialog` |
| Certificate viewer, signature health | partial | yes | `ApkSignatureTools.showCertificateDialog` |
| Signature verification killer | VIP (hook) | **partial** — removes the signature, does not hook checks | `ApkSignatureTools.removeSignature` |
| APK clone, custom package name | yes | yes | `mt/modder/hub/apkCloner` |
| APKS/XAPK → APK (antisplit) | VIP | yes, free | `APKExtractorActivity` |
| Zipalign / optimise / ultra compress | yes | yes | `ApkBatchTools` |
| LSPatch integration | no | yes | `lspatch-patcher.jar` |
| IL2CPP dumper | no | yes | `il2cppdumper-main` |
| Frida gadget patching | no | yes | `feature/apk/frida` |

## Beyond both

MP Manager also ships: media player with EXIF editing, collage studio, OCR,
SQLite viewer/editor, command shell, partition dumper, boot image tools, ELF
inspector, Logcat reader, plugin packs, a built-in MCP server for AI tooling,
and an in-app updater. MT Manager has none of these.

## Where the remaining gap is

1. **Smali → Java engines** — MT bundles Jadx, Jadx Simple/Fallback, FernFlower,
   VineFlower, JD-Core, Procyon and CFR. This build bundles Jadx 1.5.6 only.
   Closing it needs the extra engines as dependencies.
2. **SFTP** — needs an SSH/SFTP library that is not in the dependency set.
3. **Hook-based signature killer** — MT injects a runtime hook; MP only removes
   the signature block, so apps that compare their own signature still notice.
4. **Method flowchart** — a CFG view over smali basic blocks; not started.

Everything else on MT's VIP sheet is already free here.
