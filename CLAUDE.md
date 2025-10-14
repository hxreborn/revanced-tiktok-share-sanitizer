# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

This is a standalone incubator for a ReVanced patch that sanitizes TikTok share links, removing tracking parameters and normalizing URLs to the canonical form `https://www.tiktok.com/@USER/video/ID`. The goal is to develop and validate the patch independently before submitting to the upstream `revanced-patches` repository.

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

## Architecture

**Core Components:**

1. **UrlNormalizer** (`src/main/kotlin/.../sharesanitizer/UrlNormalizer.kt`)
   - Pure Kotlin utility for URL normalization
   - Accepts any TikTok URL variant (www/vm/vt/regional domains)
   - Extracts `@username` and `video/ID` using regex pattern
   - Reconstructs canonical form, stripping query params/fragments/trailing slashes
   - Decodes percent-encoded path segments
   - Validates TikTok domains and URL format

2. **HTTP Layer** (planned, not yet implemented)
   - Will use abstraction over OkHttp to resolve `vm`/`vt` shortlinks
   - HEAD request with GET fallback
   - Redirect chain protection (max depth: 5)
   - Configurable timeout (default: 3s)

3. **ReVanced Patch** (planned, requires reverse engineering)
   - Will hook TikTok's share intent construction
   - Intercept clipboard writes
   - Apply sanitization pipeline: expand → normalize → clipboard
   - Fail closed: block share if sanitization fails
   - Settings toggle for optional message suffix

**Development Stages:**

- **Phase 1** ✅: Minimal Gradle scaffold with Java 17 toolchain
- **Phase 2** ✅: URL normalization logic with comprehensive tests (10 tests, 100% passing)
- **Phase 3** (in progress): HTTP client abstraction + shortlink expansion
- **Phase 4** (blocked): ReVanced integration (requires reverse engineering TikTok APK)

## Design Constraints

**Fail-Closed Philosophy**: If any sanitization step fails (expansion timeout, invalid format, network error), the patch must block the share action and show a toast. Never fall back to unsanitized URLs.

**No Upstream Dependencies Yet**: Current implementation uses only Kotlin stdlib and Java URI utilities. OkHttp and ReVanced Patcher dependencies will be added in Phase 3.

**Testing Strategy**: JVM unit tests with MockWebServer for HTTP layer. Integration tests with ReVanced runtime simulation will come later. Manual APK validation against specific TikTok build versions required before upstream PR.

**Upstream Compatibility Goal**: Final patch structure must match `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/` conventions. This standalone repo allows faster iteration before integration.

## Key Files

- `instructions.md` - Detailed functional/technical requirements and project roadmap
- `src/main/kotlin/.../UrlNormalizer.kt` - Core URL normalization logic
- `src/test/kotlin/.../UrlNormalizerTest.kt` - Comprehensive test suite (10 tests)
- `gradle.properties` - Java 17 path configuration
- `build.gradle.kts` - Lightweight Kotlin JVM project (no ReVanced deps yet)

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

## Important Notes

- **Decision Gates**: After each phase, validate approach before proceeding (see `instructions.md` for gates)
- **Reverse Engineering Blocker**: Phase 4 requires JADX/Bytecode Viewer analysis of TikTok APK to identify hook points
- **Compatibility Tracking**: Each patch version must be tagged with tested TikTok build number
- **Settings Key**: Future toggle will use `revanced_tiktok_share_sanitizer_append_message` (default: false)
