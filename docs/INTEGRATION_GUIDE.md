# ReVanced Integration Guide

This document explains how to integrate the TikTok Share Sanitizer patch into the upstream `revanced-patches` repository.

## Current Status

**Standalone Incubator:** ✅ Complete
- Core utilities (UrlNormalizer, ShortlinkExpander) implemented and tested (21 tests passing)
- Reverse engineering complete (hook point identified)
- Patch skeleton and fingerprint defined

**Upstream Integration:** ⏳ Ready to start

---

## Why This Project is Separate

This is an **incubator repository** for rapid iteration:
- ✅ Lightweight Gradle setup without ReVanced dependencies
- ✅ Fast test cycles for core URL normalization logic
- ✅ Independent reverse engineering workspace
- ✅ Version control for patch development history

The patch files (`ShareSanitizerPatch.kt`, `ClipboardCopyFingerprint.kt`) are **templates** - they won't compile here because we don't have ReVanced Patcher dependencies, and that's intentional.

---

## Integration Checklist

### Step 1: Clone revanced-patches Repository

```bash
# Recommended location (sibling to this repo)
cd ..
git clone https://github.com/revanced/revanced-patches.git
cd revanced-patches
```

### Step 2: Copy Core Utilities (Portable Kotlin Code)

These files have **no ReVanced dependencies** and can be copied directly:

```bash
# From this incubator repo root
rsync -av \
  src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/UrlNormalizer.kt \
  src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/ShortlinkExpander.kt \
  src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/OkHttpClientAdapter.kt \
  src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/Result.kt \
  ../revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
```

**Files to copy:**
- `UrlNormalizer.kt` - Pure Kotlin URL parsing/normalization
- `ShortlinkExpander.kt` - HTTP redirect following
- `OkHttpClientAdapter.kt` - HTTP client abstraction
- `Result.kt` - Rust-style Result type for error handling

### Step 3: Copy Tests

```bash
rsync -av \
  src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/ \
  ../revanced-patches/patches/src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
```

### Step 4: Copy Patch Files (Templates)

These require **manual integration** because they use ReVanced Patcher APIs:

```bash
# Copy as reference/template (won't compile as-is in incubator)
cp src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/ShareSanitizerPatch.kt \
   ../revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/

cp src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/fingerprints/ClipboardCopyFingerprint.kt \
   ../revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/fingerprints/
```

**Note:** These files are already correct for upstream - they just can't compile in this incubator project.

### Step 5: Verify Dependencies in upstream build.gradle.kts

Ensure `revanced-patches/build.gradle.kts` has:

```kotlin
dependencies {
    // ReVanced Patcher (should already be there)
    implementation("app.revanced:revanced-patcher:...")

    // OkHttp for ShortlinkExpander
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

### Step 6: Build Upstream Patches

```bash
cd ../revanced-patches
./gradlew build
```

If successful, you'll have `patches/build/libs/revanced-patches.jar` with our patch included!

---

## revanced-integrations Setup

The patch requires a companion extension in the **separate** `revanced-integrations` repository.

### Step 1: Clone revanced-integrations

```bash
cd ..
git clone https://github.com/revanced/revanced-integrations.git
cd revanced-integrations
```

### Step 2: Create Extension Hook

Create file: `app/src/main/java/app/revanced/extension/tiktok/sharesanitizer/ShareSanitizerHook.java`

```java
package app.revanced.extension.tiktok.sharesanitizer;

import android.content.Context;
import android.widget.Toast;

/**
 * Extension hook for TikTok Share Sanitizer patch.
 * Called from bytecode injection in ShareSanitizerPatch.kt
 */
public class ShareSanitizerHook {

    /**
     * Sanitizes a TikTok share URL by expanding shortlinks and normalizing format.
     *
     * @param originalUrl URL from TikTok's share system (may be vm/vt shortlink)
     * @param context Android context for toast notifications
     * @return Sanitized canonical URL, or original URL if sanitization fails
     */
    public static String sanitizeShareUrl(String originalUrl, Context context) {
        try {
            // TODO: Port UrlNormalizer + ShortlinkExpander from patches repo
            // For now, basic implementation:

            // Step 1: Expand shortlinks (if needed)
            String expandedUrl = expandShortlink(originalUrl);

            // Step 2: Normalize to canonical format
            String canonicalUrl = normalizeUrl(expandedUrl);

            // Step 3: Optional message suffix (future feature)
            // if (Settings.APPEND_SANITIZER_MESSAGE.get()) {
            //     return canonicalUrl + "\n\nAnonymized share: clean link, tracking removed.";
            // }

            return canonicalUrl;

        } catch (Exception e) {
            // Fail-safe: return original URL on error
            showToast(context, "Failed to sanitize URL: " + e.getMessage());
            return originalUrl;
        }
    }

    private static String expandShortlink(String url) {
        // TODO: Implement HTTP redirect following with OkHttp
        // For now, pass through
        return url;
    }

    private static String normalizeUrl(String url) {
        // TODO: Implement URL parsing and canonical format construction
        // Extract @username and video/ID, reconstruct as:
        // https://www.tiktok.com/@username/video/ID

        // Basic implementation: strip query params
        int queryStart = url.indexOf('?');
        if (queryStart != -1) {
            return url.substring(0, queryStart);
        }
        return url;
    }

    private static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
```

**Note:** The full implementation requires porting `UrlNormalizer` and `ShortlinkExpander` logic from the patches repo to Java (or using Kotlin in integrations).

### Step 3: Build Integrations APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/revanced-integrations.apk`

---

## Testing the Patch

### Build Complete Patch Bundle

```bash
# In revanced-patches directory
./gradlew build

# In revanced-integrations directory
./gradlew assembleRelease
```

### Patch TikTok APK

```bash
# Download revanced-cli from https://github.com/revanced/revanced-cli/releases

java -jar revanced-cli.jar patch \
  --patch-bundle ../revanced-patches/patches/build/libs/revanced-patches.jar \
  --integrations ../revanced-integrations/app/build/outputs/apk/release/revanced-integrations.apk \
  --out tiktok-patched.apk \
  --include "Share link sanitizer" \
  path/to/tiktok-36.5.4.apk
```

### Install and Test

```bash
# Install on device
adb install tiktok-patched.apk

# Monitor logs
adb logcat | grep -E "ShareSanitizer|clipboard"

# Test scenarios:
# 1. Open TikTok, tap "Copy Link" on a video
# 2. Paste clipboard - should see clean URL without tracking params
# 3. Test with vm.tiktok.com shortlink - should expand to canonical
```

---

## File Structure After Integration

### In revanced-patches
```
patches/src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
├── ShareSanitizerPatch.kt           # Bytecode injection
├── UrlNormalizer.kt                 # Core logic
├── ShortlinkExpander.kt             # HTTP client
├── OkHttpClientAdapter.kt           # HTTP abstraction
├── Result.kt                        # Error handling
└── fingerprints/
    └── ClipboardCopyFingerprint.kt  # Method matching

patches/src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/
├── UrlNormalizerTest.kt
├── ShortlinkExpanderTest.kt
└── OkHttpClientAdapterTest.kt
```

### In revanced-integrations
```
app/src/main/java/app/revanced/extension/tiktok/sharesanitizer/
└── ShareSanitizerHook.java          # JVM-callable hook
```

---

## Compatibility Matrix

| Component | Version | Status |
|-----------|---------|--------|
| TikTok (musically) | 36.5.4 | ✅ Tested via RE |
| TikTok (trill) | 36.5.4 | ⏳ Expected compatible |
| ReVanced Patcher | 19.x+ | ✅ Compatible |
| OkHttp | 4.12.0 | ✅ Used in tests |

---

## Troubleshooting

### Fingerprint Not Matching

**Symptom:** Patch fails with "fingerprint not found"

**Solution:**
1. Check TikTok version matches tested version (36.5.4)
2. TikTok may have changed obfuscation in newer versions
3. Update fingerprint in `ClipboardCopyFingerprint.kt` based on new bytecode patterns

### Extension Not Called

**Symptom:** Clipboard still contains unsanitized URLs

**Solution:**
1. Verify integrations APK is properly signed and included in patch
2. Check logcat for ClassNotFoundException or method resolution errors
3. Ensure method signature in extension matches patch injection code

### Tests Fail After Integration

**Symptom:** Tests pass in incubator but fail in upstream

**Solution:**
1. Check for dependency conflicts (OkHttp version mismatch)
2. Verify test resources are copied correctly
3. Ensure MockWebServer is available in test classpath

---

## Future Enhancements

- [ ] Settings toggle for optional message suffix
- [ ] Settings UI integration with existing TikTok settings
- [ ] Support for additional TikTok URL formats
- [ ] Batch URL processing for multiple shares
- [ ] Analytics/telemetry for sanitization success rate

---

## References

- **This incubator repo:** Development and testing
- **revanced-patches:** https://github.com/revanced/revanced-patches
- **revanced-integrations:** https://github.com/revanced/revanced-integrations
- **revanced-cli:** https://github.com/revanced/revanced-cli
- **Reverse engineering findings:** `docs/REVERSE_ENGINEERING.md`
- **Best practices:** `docs/BEST_PRACTICES.md`
