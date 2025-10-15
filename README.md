# ReVanced TikTok Share Sanitizer

Kotlin module that sanitizes TikTok share links by removing tracking parameters and normalizing to canonical form: `https://www.tiktok.com/@USER/video/ID`

## Features

- Expands shortlinks (`vm.tiktok.com`, `vt.tiktok.com`) via redirect following
- Normalizes all TikTok URL variants to canonical format
- Strips tracking parameters and URL fragments
- Fails closed - blocks share on sanitization errors

## Quick Start

```bash
gradle test    # Run 21 unit tests
gradle build   # Compile and package
```

## Structure

```
src/main/kotlin/.../sharesanitizer/
├── UrlNormalizer.kt       # URL parsing/normalization
├── ShortlinkExpander.kt   # HTTP redirect resolution
└── OkHttpClientAdapter.kt # HTTP client implementation

src/test/kotlin/.../sharesanitizer/
└── *Test.kt               # 21 JUnit tests (100% passing)
```

## Requirements

- Java 17 (configured in `gradle.properties`)
- Kotlin stdlib + OkHttp 4.12.0

## Documentation

- **CLAUDE.md** - Development guidelines and architecture
- **LICENSE** - GPLv3
