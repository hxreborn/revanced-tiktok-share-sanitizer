# ReVanced TikTok Share Sanitizer – Project Instructions

> **Note:** This is the **canonical requirements document** for the project. For current implementation status and build instructions, see [README.md](README.md).

## Goal
Deliver a ReVanced patch that sanitizes TikTok share links so only canonical, privacy-safe URLs reach the clipboard or downstream apps.

## Functional requirements
- Intercept the share sheet payload before TikTok hands it to Android’s share framework.
- Expand `vm`/`vt` shortlinks to their destination URL prior to transformation.
- Normalize every link to the canonical form `https://www.tiktok.com/@USER/video/ID`.
- Force hostname `www.tiktok.com`; drop query strings, fragments, and trailing slash.
- Decode percent-encoded path segments in the resulting URL.
- Optionally append the phrase `Anonymized share: clean link, tracking removed.` controlled by a user setting (default: off).
- Copy the sanitized URL to the clipboard; ensure no fallback to the original URL.
- Fail closed: if sanitization or expansion fails, prevent the share action and surface a non-intrusive error toast/dialog.

## Non-functional requirements
- Implement as a standalone patch repo first; keep history clean and logically grouped.
- Version the patch against validated TikTok APK build numbers and document compatibility.
- Provide toggles within ReVanced settings (category: “Privacy”) with safe defaults.
- Keep code self-contained: no network I/O beyond resolving TikTok shortlinks via HEAD/GET.
- Include unit or instrumentation coverage for link parsing and error paths.
- Maintain lint and formatting parity with upstream `revanced-patches`.

## Technical approach
1. **Reverse engineering**
   - Use JADX or Bytecode Viewer to locate TikTok share intent construction and clipboard writes.
   - Identify instrumentation points for ReVanced’s patching framework (e.g., `replace`, `addHook`, `inlineHook`).
2. **Link expansion**
   - Implement a lightweight HTTP client (OkHttp is already available in the patch toolkit) to resolve `vm`/`vt` redirects.
   - Enforce short timeout and reuse ReVanced’s coroutine/async utilities where possible.
3. **URL normalization**
   - Parse resolved URLs with Java/Kotlin URI utilities.
   - Extract `@USER` and `video/ID` segments; validate format.
   - Reconstruct canonical URL and run percent-decoding on path segments.
4. **Failure handling**
   - Wrap all processing in try/catch; abort share path on errors.
   - Emit a localized toast using ReVanced’s resource helpers.
5. **Settings integration**
   - Expose a Boolean preference for the optional message suffix.
   - Default to `false`; store under `revanced_tiktok_share_sanitizer_append_message`.
6. **Clipboard override**
   - Hook the method responsible for constructing share text and replace clipboard content with sanitized URL (+ optional message).
7. **Testing**
   - Add JVM tests for URL parsing and expansion logic using mocked redirects.
   - Manually verify patched APK against the latest tested TikTok build; capture logs.

## Deliverables
- `share-sanitizer` standalone repo containing:
  - Patch source and metadata.
  - README with setup, build, and testing instructions.
  - Compatibility table (TikTok build → patch version).
- Pull request to `revanced-patches` once stable:
  - Include changelog entry, integration tests, and documentation updates.
- `instructions.md` (this file) kept in sync with project evolution.

## Implementation Status

**Phases 1-3 Complete (see README.md for current status):**
- ✅ Gradle scaffold, URL normalization, HTTP expansion (21 tests passing)

**Remaining (Phase 4):**
1. ✅ Document TikTok share flow entry points and target methods → `docs/REVERSE_ENGINEERING.md`
2. Integrate hooks, settings toggle, and clipboard override → `ShareSanitizerPatch.kt`
   - [x] Copy-link bytecode hook with fail-closed sanitizer injection
   - [x] Settings structure and integration layer (Settings.kt, ShareSanitizerSettings.kt)
   - [x] ShareSanitizerHook with settings support and 15 passing tests
   - [ ] Upstream SharedPreferences wiring (requires revanced-integrations)
   - [ ] SettingsPatch dependency and UI integration
3. Run end-to-end validation; tag version with tested TikTok build number
