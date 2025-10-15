# ReVanced Official Documentation – Field Notes

This project keeps a local clone of [`revanced-documentation`](https://github.com/ReVanced/revanced-documentation) at `../revanced-documentation`.  
The repository uses Git submodules, so always run `git submodule update --init --recursive` after cloning to materialize the linked guides.

## Development Environment
- **Source:** [`docs/revanced-development/1_setup.md`](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-development/1_setup.md)  
- **Key takeaways for this patch:**
  - Clone `revanced-cli`, `revanced-patches`, and (when needed) `revanced-patcher` locally before iterating on a patch.
  - Authenticate to GitHub Packages and set `gpr.user` / `gpr.key` in `~/.gradle/gradle.properties`; otherwise Gradle will fail to resolve the `app.revanced:revanced-patcher` dependency when we wire the incubator into the upstream build.
  - Use Java 17 and keep IntelliJ run configurations pointed at the latest patch bundle.

## Patch Anatomy & Lifecycle
- **Source:** [`docs/revanced-patcher/2_2_patch_anatomy.md`](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-patcher/2_2_patch_anatomy.md)  
- **Highlights for the TikTok Share Sanitizer:**
  - `bytecodePatch { ... }` is the canonical entry point; keep `name`/`description` concise and user facing.
  - Declare `compatibleWith` for every tested TikTok package/version and add `dependsOn(...)` once we hook into the shared settings patch.
  - Prefer `extendWith` to ship heavier logic in the integrations DEX instead of bloating the bytecode edit.

## Fingerprinting Guidance
- **Source:** [`docs/revanced-patcher/2_2_1_fingerprinting.md`](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-patcher/2_2_1_fingerprinting.md)  
- **Application to this project:**
  - Use parameter lists, access flags, opcode patterns, and durable string literals—avoid volatile obfuscated names.
  - Keep fingerprints as narrow as possible so that obfuscation churn triggers obvious build failures instead of silent false matches.
  - Capture clipboard-specific opcodes (`CHECK_CAST` to `ClipboardManager`, `ClipData.newPlainText`) to anchor the share-hook fingerprint as TikTok updates.

## Project Structure & Naming
- **Source:** [`docs/revanced-patcher/3_structure_and_conventions.md`](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-patcher/3_structure_and_conventions.md)  
- **Reminders for the incubator:**
  - Organizer convention is `📦package/ 🔍Fingerprints.kt` + `🧩SomePatch.kt`; our `fingerprints/ClipboardCopyFingerprint.kt` aligns with this.
  - Keep documentation close to non-obvious bytecode edits—reference this file whenever the hook logic changes.
  - Keep patches minimal: core sanitization belongs in the integrations module, not inside the injected smali snippet.

## Advanced Patch APIs
- **Source:** [`docs/revanced-patcher/4_apis.md`](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-patcher/4_apis.md)  
- **Relevant APIs we already rely on:**
  - `addInstructionsWithLabels(...)` plus `ExternalLabel` allows the fail-closed branch we inject before `ClipData.newPlainText`.
  - `navigate(...)` is useful for future hooks (e.g., share intent builders) if a simple index injection isn’t stable.
  - `proxy(...)`/`classBy {}` are available if we end up mutating auxiliary classes (currently unnecessary).

## CLI Workflow for Validation
- **Source:** [`docs/revanced-cli/1_usage.md`](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-cli/1_usage.md)  
- **Usage hints:**
  - Bundle our patch into a `.rvp` via the upstream build and run `revanced-cli patch ... --include "Share link sanitizer"` to exercise the TikTok APK.
  - Remember to supply the matching integrations build with `--integrations` so the sanitizer hook resolves.

## Maintenance Checklist
- Pull upstream docs periodically: `git -C ../revanced-documentation pull --recurse-submodules`.
- When the official guidance changes, update these notes and cross-link the affected section (e.g., new fingerprint heuristics or API semantics).
- Keep `instructions.md` in sync whenever upstream documentation invalidates an assumption captured there.
