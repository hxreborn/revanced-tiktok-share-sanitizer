# ReVanced TikTok Share Sanitizer

Minimal Kotlin module that sanitizes TikTok share links by removing tracking parameters and normalizing URLs to their canonical form.

## Core Functionality

Sanitizes TikTok share links by:
1. **Expanding shortlinks** (`vm.tiktok.com`, `vt.tiktok.com`) via HTTP redirect following
2. **Normalizing URLs** to canonical form: `https://www.tiktok.com/@USER/video/ID`
3. **Stripping tracking** (query parameters, fragments)
4. **Failing closed** - blocks share if sanitization fails

## Project Structure

```
src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
├── UrlNormalizer.kt          # Pure URL parsing/normalization
├── HttpClient.kt             # HTTP abstraction interface
├── OkHttpClientAdapter.kt    # OkHttp implementation
└── ShortlinkExpander.kt      # Shortlink expansion with HTTP

src/test/kotlin/.../sharesanitizer/
├── UrlNormalizerTest.kt      # URL normalization tests
├── OkHttpClientAdapterTest.kt # HTTP layer tests
├── ShortlinkExpanderTest.kt  # Shortlink expansion tests
└── ShareSanitizerHookTest.kt # Patch hook tests
```

## Build & Test

```bash
# Run all tests
gradle test

# Build
gradle build

# View test report
open build/reports/tests/test/index.html
```

## Key Files

- **CLAUDE.md** - Coding guidelines for this repository
- **AGENTS.md** - Agent and developer guidance
- **LICENSE** - GPLv3 license

## Dependencies

- `kotlin-stdlib` (JVM 17)
- `okhttp:4.12.0` (runtime)
- `mockwebserver:4.12.0` (test only)
- `junit-jupiter:5.10.1` (test only)
