# ReVanced Patches Best Practices

This document synthesizes best practices from the official ReVanced documentation and existing TikTok patches, applied specifically to this share sanitizer project.

## Table of Contents
1. [Patch Architecture](#patch-architecture)
2. [Patch Types and When to Use Them](#patch-types-and-when-to-use-them)
3. [Structure and Organization](#structure-and-organization)
4. [Fingerprinting and Bytecode Hooking](#fingerprinting-and-bytecode-hooking)
5. [Settings Integration](#settings-integration)
6. [Testing and Validation](#testing-and-validation)
7. [Compatibility and Versioning](#compatibility-and-versioning)
8. [Upstream Integration Readiness](#upstream-integration-readiness)

---

## Patch Architecture

### Core Principles
- **Modular Design**: ReVanced patches are small, focused modifications that can declare dependencies on other patches
- **Execution Model**: The `execute` function is the entry point; dependencies are executed before the main patch
- **Context Objects**: Different patch types provide unique APIs to interact with the APK

### Patch Declaration Pattern
```kotlin
val myPatch = bytecodePatch(
    name = "Descriptive Name",
    description = "Clear, user-facing explanation of what this patch does."
) {
    dependsOn(otherPatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4"),          // TikTok global
        "com.zhiliaoapp.musically"("36.5.4")           // TikTok US
    )

    execute {
        // Patch implementation
    }
}
```

**Key Observations:**
- Package names matter: TikTok has two package identifiers (`trill` for global, `musically` for US)
- Version compatibility must be explicitly declared with tested build numbers
- Descriptive names appear in ReVanced Manager UI

---

## Patch Types and When to Use Them

### 1. BytecodePatch
**Use for:** Modifying Dalvik VM bytecode (Java/Kotlin code behavior)

**When to choose:**
- Hooking method calls (e.g., intercepting share intent construction)
- Modifying control flow (e.g., bypassing login requirements)
- Injecting custom logic into existing methods

**Performance note:** Does not trigger resource decoding; preferred for most patches.

**Share Sanitizer Application:**
This patch will be a `BytecodePatch` because we need to:
- Hook TikTok's share intent construction method
- Intercept clipboard write operations
- Inject URL sanitization logic before share completes

### 2. ResourcePatch
**Use for:** Modifying decoded APK resources (XML layouts, strings, themes)

**When to choose:**
- Adding new UI elements (e.g., settings entries in XML)
- Modifying strings or localization
- Changing themes or styles

**Performance warning:** Decoding and building resources is time/memory-intensive; avoid if possible.

### 3. RawResourcePatch
**Use for:** Modifying arbitrary files in the APK without decoding

**When to choose:**
- Direct file manipulation without resource decoding overhead
- Binary resource modifications

**Share Sanitizer Application:**
We may need a companion `ResourcePatch` or integration with existing `SettingsPatch` to add the toggle for the optional message suffix.

---

## Structure and Organization

### Package Naming (Upstream-Compatible)
```
app.revanced.patches.tiktok.misc.sharesanitizer/
├── ShareSanitizerPatch.kt          # Main patch declaration
├── UrlNormalizer.kt                # Pure utility (no Android deps)
├── ShortlinkExpander.kt            # HTTP client abstraction
└── fingerprints/
    └── ShareIntentFingerprint.kt   # Bytecode pattern matching
```

**Current Status:** ✅ Already aligned with `app.revanced.patches.tiktok.misc.*` convention

### File Naming Conventions
- **Patches:** End with `Patch.kt` (e.g., `ShareSanitizerPatch.kt`)
- **Fingerprints:** End with `Fingerprint.kt`, stored in `fingerprints/` subdirectory
- **Utilities:** Descriptive names without suffix (e.g., `UrlNormalizer.kt`)

### Existing TikTok Patch Categories
```
tiktok/
├── feedfilter/      # Content feed modifications
├── interaction/     # User interaction patches
├── misc/            # Miscellaneous patches (our target)
└── shared/          # Common utilities
```

**Share Sanitizer Placement:** `misc/sharesanitizer/` fits naturally alongside existing `misc/settings/` and `misc/login/`.

---

## Fingerprinting and Bytecode Hooking

### What Are Fingerprints?
Fingerprints are bytecode pattern matchers that locate specific methods or code blocks in the APK. They abstract away hardcoded offsets, making patches version-resilient.

### Fingerprint Pattern (from SettingsPatch)
```kotlin
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

execute {
    // Find method using fingerprint
    val method = methodFingerprint.match(context).method

    // Inject instructions at specific index
    method.addInstructions(
        index,
        """
            invoke-static {}, Lapp/revanced/extension/tiktok/ShareSanitizerHook;->sanitizeShareUrl()Ljava/lang/String;
            move-result-object v0
        """
    )
}
```

### Key Injection Techniques
1. **addInstructions**: Inject Smali instructions at a specific index
2. **addInstructionsWithLabels**: Inject with conditional branching support
3. **getInstruction**: Retrieve existing instruction for analysis/modification

### Share Sanitizer Hook Points (Reverse Engineering Required)
**To identify:**
1. Use JADX to decompile TikTok APK
2. Search for:
   - `ClipboardManager.setPrimaryClip` (clipboard writes)
   - `Intent.ACTION_SEND` construction (share intents)
   - String concatenation with `https://vm.tiktok.com` or `https://vt.tiktok.com`
3. Document method signatures, class names, and instruction patterns

**Fingerprint Strategy:**
- Match on unique opcode sequences around share logic
- Use string literals (e.g., "https://") as anchor points
- Validate matches with multiple TikTok versions before finalizing

---

## Settings Integration

### Existing TikTok Settings Pattern
From `SettingsPatch.kt`:
```kotlin
private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/settings/TikTokActivityHook;"

dependsOn(sharedExtensionPatch)
```

### How Settings Work
1. **Extension Dependency**: Settings require a companion Java/Kotlin extension in the `revanced-integrations` repository
2. **Hook Class**: A dedicated class (e.g., `ShareSanitizerSettings`) exposes static methods callable from Smali
3. **Settings UI**: Integrated via existing `SettingsPatch` or resource modifications

### Share Sanitizer Settings Design
**Setting Key:** `revanced_tiktok_share_sanitizer_append_message`

**Implementation Approach:**
1. Add dependency on `settingsPatch` in our patch declaration
2. Create extension class:
   ```kotlin
   // In revanced-integrations repo
   package app.revanced.extension.tiktok.sharesanitizer

   object ShareSanitizerSettings {
       @JvmStatic
       fun shouldAppendMessage(): Boolean {
           return Settings.APPEND_SANITIZER_MESSAGE.boolean
       }
   }
   ```
3. Call from injected Smali:
   ```smali
   invoke-static {}, Lapp/revanced/extension/tiktok/sharesanitizer/ShareSanitizerSettings;->shouldAppendMessage()Z
   ```

**Default Value:** `false` (fail-closed, minimal output)

---

## Testing and Validation

### Testing Strategy Layers

#### 1. Unit Tests (Current: ✅ Implemented)
- **Scope:** Pure Kotlin utilities (`UrlNormalizer`, `ShortlinkExpander`)
- **Tools:** JUnit 5, MockWebServer for HTTP mocking
- **Coverage Target:** ≥80% for core URL processing logic
- **Current Status:** 11 tests, 100% passing

**Example Test Pattern:**
```kotlin
@Test
fun `normalize www link returns canonical url`() {
    val input = "https://www.tiktok.com/@user/video/123?foo=bar"
    val expected = "https://www.tiktok.com/@user/video/123"
    assertEquals(expected, UrlNormalizer.normalize(input))
}
```

#### 2. Integration Tests (Blocked: Requires ReVanced Runtime)
- **Scope:** Patch execution with simulated TikTok environment
- **Tools:** ReVanced test harness (if available), or manual APK validation
- **Validation Points:**
  - Share intent construction hook triggers
  - Clipboard content matches sanitized URL
  - Settings toggle affects output message

#### 3. Manual APK Testing (Phase 4)
**Procedure:**
1. Apply patch to specific TikTok APK build
2. Install on physical device or emulator
3. Test scenarios:
   - Share video with `www` link → expect canonical URL
   - Share video with `vm` shortlink → expect expanded canonical URL
   - Enable message suffix → expect appended text
   - Trigger network timeout → expect share blocked + toast
4. Capture logcat output for debugging

**Compatibility Matrix Format:**
| TikTok Version | Package | Patch Version | Status |
|----------------|---------|---------------|--------|
| 36.5.4         | musically | 1.0.0       | ✅ Tested |
| 36.5.4         | trill     | 1.0.0       | ✅ Tested |

---

## Compatibility and Versioning

### Version Declaration Pattern
```kotlin
compatibleWith(
    "com.ss.android.ugc.trill"("36.5.4"),
    "com.zhiliaoapp.musically"("36.5.4")
)
```

### Best Practices
1. **Test Multiple Builds:** Validate against at least 2-3 recent TikTok versions before declaring compatibility
2. **Document Breaking Changes:** If TikTok refactors share logic, fingerprints may break; version patch accordingly
3. **Conservative Declarations:** Only declare versions you've manually tested
4. **Regional Variants:** Always test both `trill` (global) and `musically` (US) if applicable

### Versioning Strategy for This Project
- **Phase 3 (Current):** Pure utility layer, no version dependencies yet
- **Phase 4 (Blocked):** After identifying hook points, lock to tested TikTok build
- **Upstream PR:** Include compatibility table with manual test evidence

---

## Upstream Integration Readiness

### Current Gaps vs. Upstream ReVanced
1. ❌ **Separate Gradle Project:** Must drop standalone `build.gradle.kts`/`settings.gradle.kts`
2. ❌ **Missing Patch Declaration:** Need `ShareSanitizerPatch.kt` with `bytecodePatch { }` block
3. ❌ **No Fingerprints:** Requires reverse engineering to create `ShareIntentFingerprint.kt`
4. ❌ **No Extension Integration:** Need companion code in `revanced-integrations` repo
5. ✅ **Package Structure:** Already matches `app.revanced.patches.tiktok.misc.*`
6. ✅ **Core Logic:** `UrlNormalizer` and `ShortlinkExpander` are portable

### Pre-PR Checklist
- [ ] Create `ShareSanitizerPatch.kt` with proper patch declaration
- [ ] Add fingerprints for share intent construction and clipboard writes
- [ ] Implement extension hook in `revanced-integrations`
- [ ] Integrate with existing `SettingsPatch` for toggle UI
- [ ] Add compatibility annotations for tested TikTok versions
- [ ] Write patch description strings for ReVanced Manager UI
- [ ] Test on physical device with multiple TikTok builds
- [ ] Document reverse engineering findings (method names, signatures)
- [ ] Create export script to copy only Kotlin sources (exclude Gradle scaffold)
- [ ] Update `CHANGELOG.md` in upstream repo

### Export Script Strategy
```bash
#!/bin/bash
# Copy only portable sources, exclude build scaffold
rsync -av --exclude='build/' --exclude='gradle/' \
    src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/ \
    ../revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/

rsync -av --exclude='build/' \
    src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/ \
    ../revanced-patches/patches/src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
```

---

## Quick Reference for This Project

### Current Development Phase: Phase 3 ✅
- [x] Gradle scaffold with Java 17 toolchain
- [x] URL normalization logic with comprehensive tests (10 tests)
- [x] HTTP client abstraction with OkHttp (11 tests total)
- [ ] ReVanced integration (blocked: requires reverse engineering)

### Next Steps (Phase 4)
1. **Reverse Engineering:**
   - Download target TikTok APK (recommended: 36.5.4)
   - Use JADX to identify share intent construction method
   - Document bytecode patterns for fingerprinting
2. **Create Patch Skeleton:**
   - `ShareSanitizerPatch.kt` with `bytecodePatch { }` declaration
   - `ShareIntentFingerprint.kt` based on JADX findings
3. **Extension Setup:**
   - Clone `revanced-integrations` repo
   - Add `ShareSanitizerHook.kt` under `app/revanced/extension/tiktok/sharesanitizer/`
   - Integrate with existing settings framework
4. **Testing:**
   - Manual APK patching with ReVanced CLI
   - Device validation with logcat monitoring
   - Compatibility matrix documentation

### Key Design Decisions Informed by Best Practices
1. **Fail-Closed Philosophy:** Aligns with ReVanced security mindset (never fall back to unsafe behavior)
2. **Minimal Dependencies:** Pure Kotlin utilities reduce coupling to Android APIs
3. **Fingerprint-Based Hooking:** Preferred over hardcoded offsets for version resilience
4. **Settings Integration:** Follows existing TikTok patch patterns (`SettingsPatch` dependency)
5. **Standalone Incubator:** Accelerates iteration; export only after validation

---

## Additional Resources
- **Official Documentation:** [ReVanced Patcher Docs](https://github.com/ReVanced/revanced-patcher/tree/main/docs)
- **Patch Examples:** [ReVanced Patches Repository](https://github.com/ReVanced/revanced-patches)
- **TikTok Patches:** `patches/src/main/kotlin/app/revanced/patches/tiktok/`
- **Settings Reference:** `tiktok/misc/settings/SettingsPatch.kt`
- **Bytecode Tools:** JADX, Bytecode Viewer, apktool
- **Smali Reference:** [smali/baksmali Documentation](https://github.com/JesusFreke/smali)

---

## Conclusion
This project is well-positioned for upstream integration. The core utilities are production-ready and follow ReVanced conventions. The main blocker is reverse engineering TikTok's share flow to create proper fingerprints and bytecode hooks. Once Phase 4 completes, the patch will be mergeable into `revanced-patches` with minimal refactoring.

**Estimated Effort for Phase 4:**
- Reverse engineering: 4-8 hours (APK analysis + fingerprint creation)
- Patch integration: 2-4 hours (bytecode hooking + extension setup)
- Testing & validation: 2-4 hours (manual APK testing across versions)
- Documentation: 1-2 hours (compatibility matrix + PR description)

**Total:** ~10-18 hours to production-ready upstream PR.
