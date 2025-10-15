# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

This is a standalone incubator for a ReVanced patch that sanitizes TikTok share links, removing tracking parameters and normalizing URLs to the canonical form `https://www.tiktok.com/@USER/video/ID`. The goal is to develop and validate the patch independently before submitting to the upstream `revanced-patches` repository.

**Important**: The ReVanced integrations repository was archived on October 26, 2024. Patches now use direct integration patterns rather than separate integrations.

## Build System

**Java 17 Required**: Project uses Kotlin with JVM toolchain 17. Java 17 path is configured in `gradle.properties`.

**Common Commands:**
```bash
# Build project
gradle build

# Run all tests
gradle test

# Run specific test class
gradle test --tests UrlNormalizerTest

# Clean build artifacts
gradle clean

# View test report
open build/reports/tests/test/index.html
```

## Current ReVanced Architecture (2024-2025)

### Major Changes
- **Integrations Repository Archived**: October 26, 2024 - read-only now
- **Direct Integration**: Extensions embedded directly in patches using `sharedExtensionPatch`
- **ReVanced Patcher v21.0.0**: Core library actively maintained
- **Self-Contained Patches**: Each patch includes its own dependencies

### Target Structure (Upstream)
```
revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
├── Fingerprints.kt           # Method/class identification
├── ShareSanitizerPatch.kt    # Main patch implementation
└── Hooks.kt                  # Hook implementations
```

### Current Implementation

**Core Components:**

1. **UrlNormalizer** (`src/main/kotlin/.../sharesanitizer/UrlNormalizer.kt`)
   - Pure Kotlin utility for URL normalization
   - Accepts any TikTok URL variant (www/vm/vt/regional domains)
   - Extracts `@username` and `video/ID` using regex pattern
   - Reconstructs canonical form, stripping query params/fragments/trailing slashes
   - Decodes percent-encoded path segments
   - Validates TikTok domains and URL format
   - **Test Coverage**: 10 unit tests

2. **HTTP Layer** (`src/main/kotlin/.../sharesanitizer/OkHttpClientAdapter.kt`)
   - HTTP abstraction interface with OkHttp 5.0.0 implementation
   - HEAD request with GET fallback for servers that reject HEAD
   - Redirect chain protection (max depth: 5)
   - 3-second timeout for deterministic behavior
   - **Test Coverage**: 7 integration tests

3. **Shortlink Expander** (`src/main/kotlin/.../sharesanitizer/ShortlinkExpander.kt`)
   - Handles TikTok shortlink expansion (vm.tiktok.com, vt.tiktok.com)
   - Integrates with HTTP client for redirect following
   - **Test Coverage**: 4 unit tests

4. **ReVanced Integration** (`src/main/kotlin/.../sharesanitizer/ShareSanitizerPatch.kt`)
   - Main patch with fingerprint matching and bytecode injection
   - Uses ReVanced Patcher framework patterns
   - Implements fail-closed security model

### Implementation Status

- **Core Infrastructure**: Complete with Java 17 toolchain and Gradle build system
- **URL Normalization**: Production-ready with comprehensive test coverage (10 tests)
- **HTTP Client Layer**: Robust OkHttp 5.0.0 implementation (7 tests)
- **Shortlink Expansion**: Full TikTok shortlink support (4 tests)
- **ReVanced Integration**: Complete patch implementation ready for upstream
- **Total Tests**: 21 comprehensive tests

### Critical Gap: Reverse Engineering

**Required for Upstream**: TikTok method identification through JADX/Bytecode Viewer
- Share clipboard write methods
- Share intent construction
- Settings integration points
- Hook injection targets

## Development Methodology

### Security Model
- **Fail-Closed Philosophy**: If any sanitization step fails (expansion timeout, invalid format, network error), the patch blocks the share action and displays a toast. Never falls back to unsanitized URLs.
- **Zero Trust**: All incoming URLs are treated as potentially malicious and subject to full validation.
- **Deterministic Behavior**: Network operations have strict timeouts and fallback mechanisms.

### Current ReVanced Patterns (2024-2025)

**Patch Structure**:
```kotlin
@Suppress("unused")
val shareSanitizerPatch = bytecodePatch(
    name = "Share link sanitizer",
    description = "Removes tracking parameters from TikTok share links.",
    compatiblePackages = compatiblePackage(
        "com.zhiliaoapp.musically",
        ["36.5.4"]  // Tested versions
    )
) {
    // Patch implementation using ReVanced Patcher v21.0.0
}
```

**Fingerprint Pattern**:
```kotlin
@Suppress("unused")
internal object ShareClipboardFingerprint : MethodFingerprint(
    strings = listOf("share", "clipboard"),
    customFingerprint = { methodDef, classDef ->
        // Custom logic to identify TikTok share methods
    }
)
```

**Dependencies Available in ReVanced**:
- ReVanced Patcher v21.0.0
- OkHttp 5.0.0-alpha.14
- Smali 3.0.5
- Android toolchain

### Integration Strategy

**Current Approach**: Self-contained patches with direct integration
- No separate integrations repository needed
- Extensions embedded using `sharedExtensionPatch`
- Settings integration via existing TikTok settings patterns

**For Upstream PR**:
1. Structure matches `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/`
2. Follow current ReVanced Patcher v21.0.0 patterns
3. Use existing TikTok settings integration approach
4. Include comprehensive compatibility annotations

### Testing Strategy
- **Unit Tests**: JVM tests with MockWebServer for HTTP layer
- **Manual Validation**: Required for each TikTok version
- **Compatibility Testing**: Against specific TikTok build numbers
- **Integration Testing**: Real APK validation before upstream PR

## Project Structure

### Source Code Organization
```
src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
├── ShareSanitizerPatch.kt     # Main patch with bytecode injection
├── UrlNormalizer.kt           # URL processing logic (10 tests)
├── ShortlinkExpander.kt       # Shortlink expansion (4 tests)
├── OkHttpClientAdapter.kt     # HTTP client implementation (7 tests)
├── HttpClient.kt              # HTTP abstraction interface
├── Settings.kt                # Settings key definitions
├── ShareSanitizerSettings.kt  # Settings access layer
├── Result.kt                  # Type-safe Result monad
├── SanitizerError.kt          # Sealed error types
├── StringResources.kt         # User-facing messages
└── fingerprints/
    └── ClipboardCopyFingerprint.kt  # TikTok method fingerprints
```

### Configuration Files
- `gradle.properties` - Java 17 toolchain settings
- `build.gradle.kts` - Build configuration with ReVanced dependencies
- `gradle/libs.versions.toml` - Version catalog (Kotlin 2.1.0, OkHttp 5.0.0)
- `build-env/config/versions.txt` - Tool version configurations
- `build-env/scripts/` - Build automation scripts

### Documentation
- `instructions.md` - Detailed functional/technical requirements
- `README.md` - User-facing build and installation guide
- `CLAUDE.md` - This development guidance document

### Test Suite
- `src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/` - Comprehensive tests (21 total)

## Commit Style

Use Conventional Commits. One line only. Max 72 chars. No body.

**Format:**
```
<type>(<scope>): <imperative summary>
```

**Types:** feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert

**Rules:**
- Lowercase type and scope
- No trailing period
- Use verbs in imperative: "add", "fix", "refactor"
- Keep under 72 chars total
- Group changes into the smallest logical commits

**Examples:**
```
feat(api): add search endpoint
fix(auth): handle expired tokens
refactor(ui): simplify header component
test(db): add migration tests
chore: update deps
```

**Breaking changes** (still one line):
```
feat(api)!: drop legacy v1 routes
```

## Upstream Integration Strategy

### ReVanced Repository Structure (2024-2025)
**Critical Context**: ReVanced uses monorepo with direct integration. All TikTok patches live in `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/`. This standalone repo is an incubator only.

### Development Workflow
1. **Development Branch**: All ReVanced development happens on `dev` branch
2. **Fork and Create Branch**: Fork repository, create branch from `dev`
3. **Follow Conventions**: Use ReVanced Patcher v21.0.0 patterns
4. **Pull Request**: Submit PR to `dev` branch, reference related issues

### Current Status for Upstream
**✅ Ready Components:**
- Package structure aligned (`app.revanced.patches.tiktok.misc.sharesanitizer.*`)
- Core logic implemented and tested (21 tests)
- ReVanced Patcher patterns followed
- Dependencies compatible with ReVanced v21.0.0

**🔄 Required for Upstream:**
1. **Reverse Engineering**: TikTok method identification (JADX/Bytecode Viewer)
2. **Fingerprint Creation**: Share clipboard and intent methods
3. **Settings Integration**: Hook into existing TikTok settings pattern
4. **Compatibility Testing**: Manual APK validation

### Migration Requirements
**What changes for upstream:**
1. **Structure**: Move to `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/`
2. **Files**: Separate into `Fingerprints.kt`, `ShareSanitizerPatch.kt`, `Hooks.kt`
3. **Dependencies**: Use ReVanced's OkHttp 5.0.0 and Patcher v21.0.0
4. **Testing**: Manual validation against specific TikTok build numbers

**What stays the same:**
- Core sanitization logic (UrlNormalizer, ShortlinkExpander)
- Test suite structure and coverage
- Package naming conventions
- Security model and fail-closed approach

### Acceptance Criteria
ReVanced accepts patches that provide:
- Customizations and privacy enhancements
- Ad-blocking functionality
- User experience improvements
- **Does NOT accept**: Payment circumvention patches

### Settings Integration Pattern
Based on current TikTok patches in ReVanced:
```kotlin
// Pattern from existing TikTok settings
val settingsPatch = bytecodePatch(
    name = "Share sanitizer settings",
    description = "Adds settings for share link sanitization."
) {
    // Hook into TikTok's existing settings structure
}
```

## Key Dependencies and Versions

### Current ReVanced Stack (2024-2025)
- **ReVanced Patcher**: v21.0.0 (core library)
- **OkHttp**: 5.0.0-alpha.14 (HTTP client)
- **Smali**: 3.0.5 (Dalvik bytecode)
- **Kotlin**: 2.1.0
- **Android Gradle Plugin**: 8.2.2

### TikTok Compatibility
- **Tested Version**: v36.5.4 (com.zhiliaoapp.musically)
- **Target**: com.zhiliaoapp.musically & com.ss.android.ugc.trill
- **Build Numbers**: Must be tracked for each release

### Critical Next Steps

1. **Reverse Engineering Required**:
   - Download TikTok APK v36.5.4 from APKMirror
   - Use JADX/Bytecode Viewer to analyze share functionality
   - Identify clipboard write methods and share intent construction
   - Document method signatures and class structures

2. **Fingerprint Development**:
   - Create `ShareClipboardFingerprint` for method identification
   - Develop custom fingerprint logic for TikTok obfuscation patterns
   - Test fingerprint against multiple TikTok builds

3. **Settings Integration**:
   - Study existing TikTok settings patches in ReVanced
   - Hook into TikTok's settings structure
   - Implement toggle for sanitization functionality

## Resources and References

### ReVanced Documentation
- **Main Repository**: https://github.com/ReVanced/revanced-patches
- **Development Branch**: `dev` (all development happens here)
- **Patcher Documentation**: Built into ReVanced Patcher v21.0.0
- **Contribution Guidelines**: In repository README

### Tools Required
- **JADX**: https://github.com/skylot/jadx (APK analysis)
- **Bytecode Viewer**: https://github.com/Konloki/bytecode-viewer
- **APKMirror**: https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok/
- **ReVanced CLI**: https://github.com/ReVanced/revanced-cli

### Architecture References
- **Current TikTok Patches**: Study existing patches in `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/`
- **Settings Pattern**: Reference `tiktok/misc/settings/` for integration examples
- **Fingerprint Patterns**: Review MethodFingerprint usage across ReVanced codebase
