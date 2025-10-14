# Repository Guidelines

## Project Structure & Module Organization
- Kotlin sources live under `src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/`.
- JVM tests mirror production structure in `src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/`.
- `build.gradle.kts` and `settings.gradle.kts` define the standalone module used for incubating the patch before upstreaming.
- `instructions.md` captures functional requirements; keep it updated when assumptions change.

## Build, Test, and Development Commands
- `./gradlew test` &mdash; run the JVM test suite (add the Gradle wrapper or use your local Gradle 8 install).
- `./gradlew jar` &mdash; package the current Kotlin sources into a local snapshot for manual inspection.
- `./gradlew clean` &mdash; reset build outputs if Gradle cache gets stale.

## Coding Style & Naming Conventions
- Kotlin style: 4-space indentation, trailing commas where IntelliJ/Kotlin formatter permits, prefer expression-bodied functions when they improve clarity.
- Package naming should follow `app.revanced.patches.tiktok.[domain].[feature]`.
- Use descriptive file names: e.g., `UrlNormalizer.kt`, `ShortlinkExpanderTest.kt`.
- Apply Kotlin’s official formatter (`ktfmt` or IntelliJ default) before pushing.

## Testing Guidelines
- Use JUnit 5 (`org.junit.jupiter`) with Kotlin test helpers; MockK is the preferred mocking library once added.
- Name tests with intent-revealing method names, e.g., `normalize_vm_link_returns_canonical_url`.
- Target ≥80% coverage for core URL-processing classes; add regression cases for every discovered edge case.
- Run `./gradlew test` before committing and after significant refactors.

## Commit & Pull Request Guidelines
- Use Conventional Commits: `<type>(<scope>): <imperative summary>` in lowercase, ≤72 chars, no body; types = feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert; `!` allowed for breaking change (e.g., `feat(api)!: drop legacy v1 routes`).
- Keep commits focused and logical; prefer many small commits over large blends.
- Pull requests must include: scope summary, testing evidence (command output or screenshots), and links to tracking issues or reverse-engineering notes.
- Remove build artifacts (`build/`, `.gradle/`) and non-essential tooling directories (`.claude/`) before opening a PR.
