# Roadmap — what to add next

Ordered by how much it closes the gap with MT Manager, then by effort.

## 1. Decompiler engines (biggest gap)
- Add CFR, Vineflower and Procyon as Gradle dependencies and expose a picker
  next to "View as Java" (MT offers eight engines; this build has jadx only).
- Each engine behind one interface (`JavaEngine`) so the picker stays simple.
- Effort: medium. Risk: low. Needs: Maven access to add the artifacts.

## 2. Dex to C (native protection)
- Real dex2c compiles every method to C and runs it through the NDK at build
  time. On-device that means bundling a clang toolchain and a stub loader.
- Practical middle step: ship a prebuilt native loader and encrypt dex at rest,
  decrypting into memory at startup. Still needs a compiled .so per ABI.
- The sign-time sheet already has the slot; it stays disabled until an engine
  exists, so nobody is promised protection that is not there.

## 3. SFTP
- Add an SSH library (e.g. sshj or Apache MINA SSHD) and reuse the existing
  network-profile store; the FTP/SMB/WebDAV UI already fits.

## 4. Hook-based signature killer
- MT injects a runtime hook. A closer open-source equivalent: bundle a small
  hooking layer (e.g. Pine or LSPlant) behind the existing LSPatch integration
  instead of stripping the signature block.

## 5. Method flowchart
- Build a CFG from smali basic blocks (labels, `if-*`, `goto`, `packed-switch`)
  and render with a simple layered layout; the dex editor already has the
  instruction stream.

## 6. Small wins
- XML search/replace inside AXML (code search, not just text).
- Dex redivide (split one dex into several) — merge already exists.
- Cloud backup of settings, bookmarks and network profiles.
- Per-file "protect" presets (remember the sign-time choices per project).
- ABI-aware release notes: the new release workflow already publishes
  universal + arm64-v8a + armeabi-v7a + x86_64 APKs.

## 7. AI integration
- The MCP server already exists (`mcp/` package). Next: an in-app assistant
  panel that drives the same tools (explain a smali method, suggest a patch,
  summarise a diff) with the user's own API key.
