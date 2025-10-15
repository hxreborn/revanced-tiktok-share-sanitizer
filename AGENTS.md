# Repository Guidelines - ReVanced Patch Development (2025)

## Project Structure & Module Organization
- **Patch sources**: `src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/`
  - `ShareSanitizerPatch.kt` - Main patch declaration with `bytecodePatch { }` DSL
  - `fingerprints/` - Method fingerprints for bytecode matching
  - Core logic classes (UrlNormalizer, ShortlinkExpander, etc.)
- **Tests**: `src/test/kotlin/` mirrors production structure
- **Build environment**: `build-env/` contains APK patching pipeline
  - `scripts/` - 4-step build process (setup → build → patch → sign)
  - `tools/` - ReVanced CLI, integrations APK
  - `build/` - Working directory for APKs and JARs
  - `config/versions.txt` - Locked tool versions for reproducibility
- **Reference APKs**: Store in `build-env/build/`, never commit to git
- `instructions.md` - Functional requirements and roadmap

## ReVanced Build Pipeline (Local Development)
```bash
# One-time setup
./build-env/scripts/1-setup.sh     # Downloads CLI v5.0.1, integrations, creates keystore

# Build and test cycle
./gradlew test                      # Run unit tests (URL normalization, HTTP client)
./build-env/scripts/2-build-patch.sh # Compile patch JAR from sources

# APK patching
cp ~/Downloads/tiktok-*.apk build-env/build/tiktok-base.apk
./build-env/scripts/3-patch-apk.sh  # Apply patches with ReVanced CLI
./build-env/scripts/4-sign.sh       # Sign with apksigner/jarsigner
adb install build-env/build/tiktok-signed.apk
```

## Development Commands
- `./gradlew test` - Run JUnit 5 test suite
- `./gradlew build -x test` - Build without tests (useful when Maven is down)
- `./gradlew clean` - Reset build outputs
- `java -jar build-env/tools/revanced-cli-*.jar list-patches -p patches.jar` - List available patches
- `jadx-gui tiktok-base.apk` - Reverse engineer for fingerprint development

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
- Use Conventional Commits: `<type>(<scope>): <imperative summary>` in lowercase, ≤72 chars, **no multiline bodies**
- Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert; `!` for breaking changes
- Keep commits atomic and focused; prefer many small commits over large blends
- Pull requests must include: scope summary, testing evidence (CLI output), APK versions tested
- Remove build artifacts (`build/`, `.gradle/`, `build-env/build/*.apk`) before opening PR

## ReVanced-Specific: Fingerprint Development

### Fingerprint Structure
```kotlin
fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("V")
    parameters("Landroid/content/Context;", "Ljava/lang/String;")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    )
    strings("clipboard", "text/plain")  // Literal strings in method
    custom { method, _ -> 
        method.name == "copyToClipboard"
    }
}
```

### Best Practices
- Start broad (strings, return type), refine iteratively
- Opcodes are more stable than line numbers
- Test against 3+ TikTok versions before declaring stable
- Document target versions in patch metadata

## ReVanced-Specific: Patch Declaration

```kotlin
@Suppress("unused")
val shareSanitizerPatch = bytecodePatch(
    name = "Share link sanitizer",
    description = "Removes tracking parameters from TikTok share URLs",
) {
    compatibleWith("com.zhiliaoapp.musically"("36.5.4", "36.5.5"))
    
    execute {
        val method = clipboardCopyFingerprint.method
        method.addInstructions(0, """
            invoke-static {p1}, Lapp/revanced/patches/tiktok/misc/sharesanitizer/UrlNormalizer;->normalize(Ljava/lang/String;)Ljava/lang/String;
            move-result-object p1
        """)
    }
}
```

## Debugging & Troubleshooting

### Common Issues
1. **Fingerprint mismatch**: APK version incompatible, update fingerprints
2. **Maven 526 errors**: Repository down, use cached JARs or wait
3. **NoClassDefFoundError**: Missing dependency (revanced-patcher, OkHttp)
4. **Patch not applied**: Check CLI output for SEVERE errors

### Debug Commands
```bash
# Reverse engineer APK
jadx-gui tiktok-base.apk

# List patches in JAR
java -jar revanced-cli.jar list-patches -p patches.jar

# Verbose patching
java -jar revanced-cli.jar patch --debug -p patches.jar base.apk

# Check method signatures
javap -v -cp tiktok-base.apk com.ss.android.ugc.aweme.SomeClass
```

## Upstream Contribution Process

1. **Test Coverage**: Validate against 3+ TikTok versions from APKMirror
2. **Structure**: Follow `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/`
3. **Integration**: Hook into existing settings UI when applicable
4. **Documentation**: Update patches README with feature description
5. **Metadata**: Include `compatibleWith()` declarations with tested versions
6. **PR Template**: Include test APK hashes, version matrix, feature demo

## Version Compatibility Matrix

Track tested versions in patch:
```kotlin
compatibleWith(
    "com.zhiliaoapp.musically"(
        "36.5.4",  // ✅ Tested Oct 2025
        "36.5.5",  // ✅ Tested Oct 2025  
        "37.0.0",  // ⚠️ Fingerprint update needed
    )
)
```

## Repository Maintenance

- **Version locks**: `build-env/config/versions.txt` for reproducible builds
- **Tool updates**: Update CLI/integrations quarterly or for critical fixes
- **APK storage**: Never commit APKs; document URLs in README
- **Cache management**: Clear `~/.gradle/caches/` if builds fail mysteriously
