# APK protection engine

Implements the PRD in `feature/protection`. Everything runs on the device; no
server is contacted.

## Pipeline

```
APK → ApkAnalyzer → Compatibility → ProtectionEngine → Rebuild → Verify → (Sign) → Protected APK
```

`ProtectionEngine.apply()` is the single entry point. Modules are independent:
one that cannot run reports `SKIPPED` with a reason and the rest continue, so an
arbitrary APK is never corrupted by a step that did not fit it.

## Module status

| Module | Status | What it does |
|---|---|---|
| SIGNATURE_CLEAN | **applied** | drops `META-INF/*.SF/.RSA/.DSA/.EC` and `MANIFEST.MF` so the rebuild starts unsigned |
| DEX_STRIP_DEBUG | **applied** | rebuilds every dex through dexlib2 without per-method debug items (line numbers, local names) and without the source-file attribute — the code behaves the same, a decompiler gets far less to reconstruct |
| DEX_INTEGRITY | **applied** | rewrites each `classes*.dex` header SHA-1 and Adler-32 so the file is well formed |
| INTEGRITY_ASSET | **applied** | stores `assets/protection-integrity.sha256` — SHA-256 of every entry |
| RESOURCE_STRIP | skipped | needs resource-table rewriting with arsc re-encoding |
| STRING_PROTECTION | skipped | needs smali rewriting plus an injected decryptor class |
| R8 / renaming | skipped | R8 runs at build time on sources, not on an already-built APK |
| CONTROL_FLOW | skipped | needs smali rewriting of every method |
| NATIVE_PROTECTION | skipped | needs an NDK toolchain per ABI |
| RUNTIME_PROTECTION | skipped | needs injected code that runs at startup |
| ANTI_DEBUG | skipped | needs injected code |
| INSTRUMENTATION | skipped | needs injected code |

The UI lists every module with its state; nothing claims to be applied unless
it actually changed bytes.

## Analysis

`ApkAnalyzer` (inside `ProtectionEngine`) reports package, version, min/target
SDK, DEX count and multidex, ABIs from `lib/<abi>/*.so`, activities, services,
receivers, providers, permissions, asset count, signature presence, framework
(Flutter / React Native / Jetpack Compose / Java+Kotlin), an obfuscation
estimate from activity name lengths, and a compatibility state
(SUPPORTED / PARTIAL / RISKY / UNSUPPORTED).

## Verification

`ProtectionEngine.verify()` re-opens the output and checks that
`AndroidManifest.xml` exists, at least one `classes*.dex` is present and every
DEX header starts with the `dex` magic. A failed check deletes the partial file
and reports the error.

## Signing

The engine does not sign. The output is written next to the input as
`<name>_protected.apk` and signed from the file menu, which already has the
keystore and protection-sheet flow.

## Limits

No local protection makes an APK unbreakable; the UI carries the PRD's
disclaimer verbatim. The four skipped runtime modules need code injection
(smali generation plus a loader), which this build does not carry.
