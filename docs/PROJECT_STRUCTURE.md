# Project Structure Recommendations

Based on Gradle/Kotlin best practices (2025) and ReVanced conventions, this document analyzes the current structure and suggests improvements.

## Current Structure Assessment

### ✅ What's Good

1. **Clean separation of concerns:**
   ```
   src/main/kotlin/          # Production code
   src/test/kotlin/          # Tests
   docs/                     # Documentation
   workspace/                # RE artifacts
   ```

2. **Documentation-first approach:**
   - Clear README, CLAUDE.md, and instructions.md
   - Comprehensive RE documentation
   - Integration guides for upstream

3. **Test coverage:**
   - 21 tests passing for core utilities
   - MockWebServer for HTTP testing
   - JUnit 5 modern test framework

4. **Version control hygiene:**
   - Atomic commits with conventional commit messages
   - Appropriate gitignore for build artifacts
   - RE workspace properly excluded

### ⚠️ Areas for Improvement

## Recommended Enhancements

### 1. Adopt Version Catalog (High Priority)

**Current:** Dependencies hardcoded in `build.gradle.kts`

**Recommended:** Use `gradle/libs.versions.toml`

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.22"
okhttp = "4.12.0"
junit = "5.10.1"

[libraries]
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }

[bundles]
testing = ["junit-jupiter", "junit-platform-launcher", "okhttp-mockwebserver"]

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
```

**Then in `build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.okhttp)
    testImplementation(libs.bundles.testing)
}
```

**Benefits:**
- Centralized dependency management
- Easier updates across project
- Type-safe accessors in build scripts
- IDE autocomplete for dependencies

### 2. Add Build Conventions (Medium Priority)

**Current:** Single `build.gradle.kts` at root

**Recommended:** Add `buildSrc/` for convention plugins

```
buildSrc/
├── build.gradle.kts
└── src/main/kotlin/
    ├── kotlin-conventions.gradle.kts     # Common Kotlin config
    └── testing-conventions.gradle.kts    # Test setup
```

**Example `buildSrc/src/main/kotlin/kotlin-conventions.gradle.kts`:**
```kotlin
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
        allWarningsAsErrors.set(true)
    }
}

repositories {
    mavenCentral()
}
```

**Benefits:**
- Reusable build logic
- Easier to maintain as project grows
- Follows Gradle best practices

### 3. Modularize Project (Future Consideration)

**Current:** Single module

**Future:** Could split into modules if project grows:
```
settings.gradle.kts
├── core/                   # UrlNormalizer, ShortlinkExpander
├── patch/                  # ShareSanitizerPatch, fingerprints
└── integration-tests/      # E2E tests with patched APK
```

**When to do this:**
- If adding more patches (e.g., comment sanitizer, profile link cleaner)
- If core utilities become reusable across patches
- If build time becomes an issue

**Don't do this now:** Current single-module structure is appropriate for the scope.

### 4. Improve Test Organization

**Current:** All tests in `src/test/kotlin/`

**Recommended:** Separate test types

```
src/
├── test/kotlin/           # Unit tests (fast, no network)
├── integrationTest/kotlin/  # Integration tests (with MockWebServer)
└── functionalTest/kotlin/   # E2E tests (with real APK)
```

**In `build.gradle.kts`:**
```kotlin
testing {
    suites {
        val test by getting(JvmTestSuite::class)

        val integrationTest by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(libs.okhttp.mockwebserver)
            }
        }
    }
}
```

**Benefits:**
- Faster feedback (run unit tests first)
- Clear separation of concerns
- Can run different test suites in CI

### 5. Add Build Reproducibility

**Recommended additions:**

```kotlin
// build.gradle.kts
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Build-Timestamp" to System.currentTimeMillis()
        )
    }
}
```

### 6. Enhance Documentation Structure

**Current:**
```
docs/
├── BEST_PRACTICES.md
├── INTEGRATION_GUIDE.md
├── REVERSE_ENGINEERING.md
├── UPSTREAM_FIT_ASSESSMENT.md
└── UPSTREAM_MIGRATION.md
```

**Recommended additions:**

```
docs/
├── architecture/
│   ├── DESIGN_DECISIONS.md      # ADRs (Architecture Decision Records)
│   ├── TESTING_STRATEGY.md
│   └── ERROR_HANDLING.md
├── development/
│   ├── BEST_PRACTICES.md        # (existing)
│   ├── LOCAL_DEVELOPMENT.md     # Setup guide
│   └── TROUBLESHOOTING.md
├── integration/
│   ├── INTEGRATION_GUIDE.md     # (existing)
│   ├── UPSTREAM_MIGRATION.md    # (existing)
│   └── UPSTREAM_FIT_ASSESSMENT.md
└── reverse-engineering/
    ├── REVERSE_ENGINEERING.md   # (existing)
    ├── APK_ANALYSIS.md          # Tools and techniques
    └── FINGERPRINTS.md          # How to create fingerprints
```

### 7. Add CI/CD Configuration

**File:** `.github/workflows/ci.yml`

```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/gradle-build-action@v3

      - name: Run tests
        run: gradle test --no-daemon

      - name: Upload test results
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: build/reports/tests/
```

### 8. Add Code Quality Tools

**File:** `.editorconfig`
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_style = space
indent_size = 4
max_line_length = 120

[*.md]
trim_trailing_whitespace = false
```

**File:** `detekt.yml` (Kotlin linter)
```yaml
detekt:
  parallel: true
  config: detekt.yml

complexity:
  CyclomaticComplexMethod:
    active: true
    threshold: 15
```

**Add to `build.gradle.kts`:**
```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

detekt {
    config.setFrom(files("detekt.yml"))
    buildUponDefaultConfig = true
}
```

## Priority Recommendations

### Do Now (Phase 4c - Polish)

1. ✅ **Add version catalog** - Takes 15 minutes, huge maintainability win
2. ✅ **Add CI/CD workflow** - Automates testing
3. ✅ **Add .editorconfig** - Consistent formatting

### Do Later (Post-Phase 4)

4. **Add build conventions** - When adding more patches
5. **Separate test types** - When test suite grows
6. **Modularize** - Only if adding multiple patches

### Don't Do Yet

- ❌ Multi-module build - Premature for single patch
- ❌ KSP migration - No annotation processing yet
- ❌ Remote build cache - Not needed for incubator project

## Current Structure Is Good!

**Important:** Your current structure is **already solid** for an incubator project:

✅ Clean source organization
✅ Comprehensive documentation
✅ Proper test coverage
✅ Atomic git commits
✅ Clear upstream migration path

The recommendations above are **enhancements**, not fixes. The project is well-structured for its current scope.

## Comparison to ReVanced Upstream

### What Matches

- ✅ Package structure: `app.revanced.patches.{app}.{category}.{feature}`
- ✅ Fingerprints in separate `fingerprints/` subdirectory
- ✅ Tests alongside production code
- ✅ Gradle + Kotlin DSL

### What's Different (Intentional)

- **No multi-module build** - Upstream has 100+ patches, we have 1
- **No patch bundling** - Upstream builds JAR, we're incubating
- **No integration tests** - Upstream tests against APKs, we test pure logic
- **Standalone dependencies** - We don't depend on upstream artifacts

These differences are **by design** for rapid iteration.

## Next Steps

If you want to implement any recommendations:

1. **Version catalog** - See above example
2. **CI workflow** - See above example
3. **Documentation structure** - Organize existing docs into subdirectories
4. **Build conventions** - Add when needed

Would you like me to implement any of these enhancements?
