# ReVanced TikTok Share Sanitizer

A standalone incubator for a ReVanced patch that removes tracking parameters from TikTok share links. Develops the core sanitization logic independently before integration into the upstream [`revanced-patches`](https://github.com/ReVanced/revanced-patches) monorepo.

## Status

- ✅ **Phase 1**: Gradle/Kotlin scaffold with Java 17 toolchain
- ✅ **Phase 2**: URL normalization (10 tests, 100% passing)
- ✅ **Phase 3**: HTTP shortlink expansion (21 tests total, OkHttp 4.12.0)
- ⏳ **Phase 4**: ReVanced patch integration (blocked on reverse engineering)

**Current Test Suite:** 21/21 passing (1.5s)

- `UrlNormalizerTest`: 10 tests
- `ShortlinkExpanderTest`: 4 tests
- `OkHttpClientAdapterTest`: 7 tests

## What It Does

Sanitizes TikTok share links by:
1. **Expanding shortlinks** (`vm.tiktok.com`, `vt.tiktok.com`) via HTTP redirect following
2. **Normalizing URLs** to canonical form: `https://www.tiktok.com/@USER/video/ID`
3. **Stripping tracking** (query parameters, fragments, encoded parameters)
4. **Failing closed** - blocks share if sanitization fails (no fallback to unsanitized URLs)

Optional: Append `"Anonymized share: clean link, tracking removed."` (user toggle, default: off)

## Project Layout

```
src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
├── UrlNormalizer.kt          # Pure URL parsing/normalization
├── HttpClient.kt             # HTTP abstraction interface
├── OkHttpClientAdapter.kt    # OkHttp implementation (HEAD/GET fallback)
└── ShortlinkExpander.kt      # Shortlink detection + expansion

src/test/kotlin/.../sharesanitizer/
├── UrlNormalizerTest.kt      # 10 URL normalization tests
├── OkHttpClientAdapterTest.kt # 7 HTTP layer tests
└── ShortlinkExpanderTest.kt  # 4 shortlink logic tests

docs/
├── UPSTREAM_MIGRATION.md     # Strategy for revanced-patches PR
├── REVERSE_ENGINEERING.md    # Template for Phase 4 APK analysis
└── BEST_PRACTICES.md         # ReVanced patch patterns & conventions

apk/                           # Reference TikTok APKs for RE & manual tests

build.gradle.kts              # Standalone build (not for upstream)
instructions.md               # Canonical requirements doc (design reference)
CLAUDE.md                     # AI assistant guidance
```

## Getting Started

### Prerequisites
- Java 17 (configured in `gradle.properties`)
- Gradle 8+ (or use `gradle` wrapper)

### Build & Test
```bash
# Run all tests
gradle test

# Run specific test class
gradle test --tests UrlNormalizerTest

# View test report
open build/reports/tests/test/index.html

# Build
gradle build
```

## Upstream Integration Strategy

**Critical:** This is a standalone incubator. ReVanced uses a monorepo structure where all patches live in `patches/src/main/kotlin/`. Before submitting a PR:

1. **Complete reverse engineering** - Identify TikTok share/clipboard hook points (Phase 4)
2. **Create `ShareSanitizerPatch.kt`** - Implement `bytecodePatch { }` with fingerprints
3. **Integrate settings** - Extend existing TikTok `SettingsPatch` for user toggle
4. **Export sources** - Copy only `.kt` files (exclude `build.gradle.kts`, etc.)
5. **Test in monorepo** - Verify in `revanced-patches/patches` module
6. **Submit PR** - Include compatibility annotations, patch descriptions

See [`docs/UPSTREAM_MIGRATION.md`](docs/UPSTREAM_MIGRATION.md) for detailed migration plan.

### Why Standalone?
- ✅ Faster iteration without monorepo build overhead
- ✅ Clean git history focused on feature development
- ✅ Independent testing/validation before upstream submission
- ✅ Package structure already matches upstream (`app.revanced.patches.tiktok.misc.*`)

## Upstream Prep Checklist

### Core Logic (Complete)
- [x] Implement `UrlNormalizer` with TikTok domain handling
- [x] HTTP abstraction layer (swappable OkHttp)
- [x] Shortlink expansion with redirect chain protection
- [x] >80% test coverage (achieved: 100%)

### Upstream Integration (Remaining)
- [ ] Document reverse engineering findings (`docs/REVERSE_ENGINEERING.md`)
- [ ] Create `ShareSanitizerPatch.kt` with fingerprints + hooks
- [ ] Integrate with existing TikTok settings UI
- [ ] Add compatibility annotations for tested TikTok versions
- [ ] Write export script to copy sources to upstream
- [ ] Manual APK testing protocol (`docs/TESTING.md`)

## Key Design Decisions

1. **Result-based error handling** - Rust-inspired `Result<T, E>` type for explicit error handling (no exceptions)
2. **Typed error hierarchy** - `SanitizerError` with user-friendly toast messages for Phase 4
3. **OkHttp 4.12.0 (stable)** - Not alpha version to avoid upstream friction
4. **HTTP abstraction interface** - Allows swapping HTTP client if needed
5. **HEAD with GET fallback** - Handles servers rejecting HEAD requests
6. **Max 5 redirects, 3s timeout** - Hardcoded limits for deterministic behavior
7. **Fail-closed philosophy** - Never fall back to unsanitized URLs on error

## Dependencies

- `kotlin-stdlib` (JVM 17)
- `okhttp:4.12.0` (runtime)
- `mockwebserver:4.12.0` (test only)
- `junit-jupiter:5.10.1` (test only)

**Note:** ReVanced Patcher dependency not added yet (Phase 4 requirement)

## Contributing

This repo is an incubator for upstream contribution. To contribute:
1. Ensure tests pass: `gradle test`
2. Follow [Conventional Commits](https://www.conventionalcommits.org/) (max 72 chars)
3. Keep package structure aligned with upstream
4. Document any TikTok APK findings in `docs/REVERSE_ENGINEERING.md`

## License

This project will adopt the GPLv3 license to match upstream ReVanced patches once merged.

## Resources

- **Upstream Repo**: https://github.com/ReVanced/revanced-patches
- **Existing TikTok Patches**: `patches/src/main/kotlin/app/revanced/patches/tiktok/`
- **ReVanced Patcher Docs**: https://github.com/ReVanced/revanced-patcher
- **Project Requirements**: [`instructions.md`](instructions.md)
- **Migration Guide**: [`docs/UPSTREAM_MIGRATION.md`](docs/UPSTREAM_MIGRATION.md)
