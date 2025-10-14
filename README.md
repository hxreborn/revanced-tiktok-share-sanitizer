# ReVanced TikTok Share Sanitizer

A standalone incubator for a ReVanced patch that cleans TikTok share links.

## Status

- ✅ Minimal Gradle/Kotlin scaffold
- 🚧 Core URL processing logic and tests
- ⏳ Reverse engineering notes and upstream integration

Refer to `instructions.md` for the current high-level roadmap and open questions.

## Project Layout

- `build.gradle.kts`, `settings.gradle.kts` &mdash; lightweight JVM module for local iteration.
- `src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/` &mdash; patch sources (empty scaffold for now).
- `src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/` &mdash; JVM test space.
- `instructions.md` &mdash; functional/technical requirements gathered so far.

## Getting Started

```bash
./gradlew test
```

> Gradle wrapper is not committed yet; install Gradle 8 locally or add the wrapper before collaborating.

## Upstream Prep Checklist

- [ ] Implement `UrlNormalizer`, HTTP abstraction, and short-link expansion with >80% test coverage.
- [ ] Document reverse engineering prerequisites (`docs/REVERSE_ENGINEERING.md`).
- [ ] Align build/test configuration with `revanced-patches` conventions (no standalone Gradle build files in final PR).
- [ ] Provide compatibility notes and manual testing protocol (`docs/TESTING.md`).

## License

This project will adopt the same GPLv3 license as upstream ReVanced patches once it is ready to publish.
