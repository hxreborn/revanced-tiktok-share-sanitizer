# Upstream Migration Guide

This document outlines the strategy for migrating this standalone incubator into the official `revanced-patches` monorepo.

> **Fork Tracking**: See [FORK_TRACKING.md](FORK_TRACKING.md) for forked repository details, branch names, and sync workflows.

## Context

ReVanced maintains all Android patches in a single monorepo at `github.com/ReVanced/revanced-patches`. TikTok patches live under `patches/src/main/kotlin/app/revanced/patches/tiktok/`. This standalone repo is intentionally separate to enable rapid iteration without upstream build/CI overhead.

## Current State

**What We Have:**
- ✅ Package structure matches upstream: `app.revanced.patches.tiktok.misc.sharesanitizer.*`
- ✅ Core sanitization logic (UrlNormalizer, ShortlinkExpander, HttpClient)
- ✅ 21 passing unit tests (100% coverage)
- ✅ Standalone Gradle build for local iteration

**What's Missing for Upstream:**
- ❌ `ShareSanitizerPatch.kt` with ReVanced patch declaration
- ❌ Fingerprints for TikTok share/clipboard methods
- ❌ Compatibility metadata (`@Patch` annotations with version constraints)
- ❌ Settings UI integration (Privacy category toggle)
- ❌ Patch description strings for ReVanced Manager
- ❌ Integration with existing TikTok settings infrastructure

## Migration Strategy

### Phase 1: Reverse Engineering (Prerequisite)

**Goal:** Identify TikTok APK hook points for share intent and clipboard writes.

**Actions:**
1. Download target TikTok APK version (e.g., v35.0.4)
2. Decompile with JADX or Bytecode Viewer
3. Locate share dialog/intent construction:
   - Search for `Intent.ACTION_SEND`, `ClipboardManager`, `setClipData`
   - Identify method signatures and class hierarchies
4. Document fingerprints (method patterns, opcodes, string literals)
5. Record findings in `docs/REVERSE_ENGINEERING.md`

**Deliverable:** Method signatures + bytecode patterns for patching

---

### Phase 2: Create Patch Scaffold

**Goal:** Build `ShareSanitizerPatch.kt` with proper ReVanced wiring.

**File:** `src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/ShareSanitizerPatch.kt`

**Template:**
```kotlin
package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patcher.patch.bytecodePatch

@Patch(
    name = "Sanitize share links",
    description = "Removes tracking parameters from TikTok share links",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage("com.zhiliaoapp.musically", ["35.0.4"])
    ]
)
val shareSanitizerPatch = bytecodePatch(
    name = "Sanitize share links",
    description = "Removes tracking from TikTok share links",
) {
    execute {
        // Fingerprint matching + bytecode injection here
        // Hook share intent construction
        // Intercept clipboard writes
        // Apply: expand → normalize → clipboard/share
    }
}
```

**Actions:**
1. Add ReVanced Patcher dependency to local `build.gradle.kts` temporarily
2. Define fingerprints for identified methods
3. Implement bytecode hooks using `addInstructions` or `replaceWith`
4. Add fail-closed error handling (toast on failure)

---

### Phase 3: Settings Integration

**Goal:** Add Privacy category toggle for optional message suffix.

**Reference:** Existing TikTok patches in `patches/.../tiktok/misc/settings/`

**Actions:**
1. Extend `SettingsPatch` to add new preference:
   ```kotlin
   SwitchPreference(
       key = "revanced_tiktok_share_sanitizer_append_message",
       title = "Append anonymization notice",
       summary = "Add \"Anonymized share: clean link, tracking removed.\" to shares",
       default = false
   )
   ```
2. Read preference value in `ShareSanitizerPatch` execution
3. Conditionally append message based on user setting

---

### Phase 4: Upstream Export

**Goal:** Transplant code into `revanced-patches` monorepo without local Gradle scaffold.

**Export Script (`scripts/export-upstream.sh`):**
```bash
#!/bin/bash
# Copy only sources/tests, exclude Gradle files
DEST="$1" # Path to revanced-patches clone

rsync -av --exclude='build/' --exclude='.gradle/' \
  --exclude='build.gradle.kts' --exclude='settings.gradle.kts' \
  --exclude='gradle.properties' --exclude='gradlew*' \
  src/main/kotlin/ "$DEST/patches/src/main/kotlin/"

rsync -av src/test/kotlin/ "$DEST/patches/src/test/kotlin/"

echo "Exported to $DEST"
echo "Next: cd $DEST && ./gradlew build test"
```

**Manual Steps:**
1. Run export script to copy sources
2. Verify tests pass in upstream module: `./gradlew :patches:test --tests *ShareSanitizer*`
3. Add patch to upstream registry (if required by build system)
4. Update CHANGELOG.md with new patch entry
5. Create PR with:
   - Commit message: `feat(tiktok): add share link sanitizer patch`
   - Description: functionality, tested versions, privacy benefits
   - Link to reverse engineering documentation

---

### Phase 5: Post-Merge Cleanup

**After PR Merged:**
1. Archive this standalone repo with "MERGED" notice
2. Update README to point to upstream patch location
3. Tag final version with tested TikTok build number
4. Document any upstream-specific quirks for future maintainers

---

## Key Differences: Standalone vs Upstream

| Aspect | Standalone (This Repo) | Upstream (`revanced-patches`) |
|--------|------------------------|-------------------------------|
| Build | Separate `build.gradle.kts` | Monorepo `patches` module |
| Tests | `src/test/kotlin/` | `patches/src/test/kotlin/` |
| Dependencies | Declared locally | Inherited from root project |
| Patch Registration | N/A | Auto-discovered by module |
| Settings UI | Mocked/stubbed | Extends existing `SettingsPatch` |
| Compatibility | Self-managed | `@Patch` annotations + CI enforcement |
| Publishing | N/A | Maven + GitHub Packages |

---

## Migration Checklist

### Pre-Migration (Done Here)
- [x] Core logic implemented (UrlNormalizer, ShortlinkExpander)
- [x] Unit tests at 100% (21 tests)
- [x] Package structure matches upstream
- [ ] Reverse engineering complete (Phase 1)
- [ ] `ShareSanitizerPatch.kt` implemented (Phase 2)
- [ ] Settings toggle integrated (Phase 3)

### During Migration
- [ ] Export script tested
- [ ] Sources copied to upstream clone
- [ ] Upstream tests pass
- [ ] Patch description strings added
- [ ] Compatibility versions documented

### Post-Migration
- [ ] PR submitted to `revanced-patches`
- [ ] Code review feedback addressed
- [ ] PR merged
- [ ] Standalone repo archived

---

## Resources

- **Upstream Repo**: https://github.com/ReVanced/revanced-patches
- **Existing TikTok Patches**: `patches/src/main/kotlin/app/revanced/patches/tiktok/`
- **Patch API Docs**: https://github.com/ReVanced/revanced-patcher
- **Settings Example**: `patches/.../tiktok/misc/settings/SettingsPatch.kt`
