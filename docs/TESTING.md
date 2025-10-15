# TikTok Share Sanitizer - Testing Guide

This document explains how to test the Share Sanitizer patch at different stages of development.

## Table of Contents

1. [Unit Testing (Current)](#unit-testing-current)
2. [Integration Testing (Requires Upstream)](#integration-testing-requires-upstream)
3. [End-to-End APK Testing](#end-to-end-apk-testing)
4. [Manual Validation Checklist](#manual-validation-checklist)

---

## Unit Testing (Current)

### ✅ Available Now (Standalone Repo)

These tests run in JVM without needing Android runtime or ReVanced infrastructure.

**Run all tests:**
```bash
gradle test
```

**Run specific test suite:**
```bash
# URL normalization (10 tests)
gradle test --tests UrlNormalizerTest

# HTTP client (7 tests)
gradle test --tests OkHttpClientAdapterTest

# Shortlink expansion (4 tests)
gradle test --tests ShortlinkExpanderTest

# Settings integration (15 tests)
gradle test --tests ShareSanitizerHookTest
```

**View HTML test report:**
```bash
open build/reports/tests/test/index.html
```

### Current Test Coverage

```
✅ 36 total tests passing (100% success rate)

Core Logic:
- UrlNormalizerTest: 10 tests
  ├─ Canonical URL formats
  ├─ Query parameter stripping
  ├─ Fragment removal
  ├─ Percent-encoding handling
  ├─ Empty username/videoId validation
  └─ TikTok domain validation

- OkHttpClientAdapterTest: 7 tests
  ├─ Redirect following
  ├─ HEAD/GET fallback
  ├─ Timeout handling
  ├─ Exponential backoff retry
  ├─ Max redirect protection
  └─ Error handling

- ShortlinkExpanderTest: 4 tests
  ├─ Shortlink detection
  ├─ Expansion logic
  └─ Pass-through for non-shortlinks

- ShareSanitizerHookTest: 15 tests
  ├─ Null/empty input handling
  ├─ Full sanitization pipeline
  ├─ Settings defaults
  └─ Error scenarios
```

### What Unit Tests DON'T Cover

❌ **Cannot test in standalone repo:**
- Bytecode injection (requires ReVanced Patcher)
- Clipboard interception (requires Android runtime)
- Toast messages (requires Android Context)
- Settings UI (requires Android Activity)
- Actual TikTok APK behavior

---

## Integration Testing (Requires Upstream)

### ⏳ Available After Upstream Merge

These tests require the ReVanced monorepo infrastructure.

### Prerequisites

1. Fork `revanced-patches`: https://github.com/ReVanced/revanced-patches
2. Fork `revanced-integrations`: https://github.com/ReVanced/revanced-integrations
3. Install ReVanced CLI: https://github.com/ReVanced/revanced-cli

### Setup Integration Testing Environment

```bash
# Clone your forks
git clone https://github.com/YOUR_USERNAME/revanced-patches
git clone https://github.com/YOUR_USERNAME/revanced-integrations
git clone https://github.com/ReVanced/revanced-cli

# Build integrations
cd revanced-integrations
./gradlew build
# Output: app/build/outputs/apk/release/revanced-integrations.apk

# Build patches
cd ../revanced-patches
./gradlew build
# Output: build/libs/revanced-patches.jar
```

### Migrate Your Patch

**Step 1: Copy sources to revanced-patches**
```bash
# From this standalone repo
cp -r src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer \
  ../revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/

# Copy tests
cp -r src/test/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer \
  ../revanced-patches/patches/src/test/kotlin/app/revanced/patches/tiktok/misc/
```

**Step 2: Copy extension to revanced-integrations**
```bash
# Move hook logic to integrations
mkdir -p ../revanced-integrations/app/src/main/java/app/revanced/extension/tiktok/sharesanitizer

cp src/main/kotlin/app/revanced/patches/tiktok/misc/sharesanitizer/{UrlNormalizer,ShortlinkExpander,HttpClient,OkHttpClientAdapter,Result,SanitizerError,Settings,ShareSanitizerHook}.kt \
  ../revanced-integrations/app/src/main/java/app/revanced/extension/tiktok/sharesanitizer/
```

**Step 3: Update ShareSanitizerSettings.kt for Android**
```kotlin
// In revanced-integrations
package app.revanced.extension.tiktok.sharesanitizer

import app.revanced.extension.shared.settings.BaseSettings

object ShareSanitizerSettings {
    private val preferences = BaseSettings.preferences

    @JvmStatic
    fun isEnabled(): Boolean {
        return preferences.getBoolean("revanced_tiktok_share_sanitizer_enabled", true)
    }

    @JvmStatic
    fun shouldAppendMessage(): Boolean {
        return preferences.getBoolean("revanced_tiktok_share_sanitizer_append_message", false)
    }

    @JvmStatic
    fun getPrivacyMessage(): String = Settings.PRIVACY_MESSAGE
}
```

**Step 4: Rebuild both repos**
```bash
# Rebuild integrations
cd ../revanced-integrations
./gradlew build

# Rebuild patches
cd ../revanced-patches
./gradlew build
```

**Step 5: Run upstream tests**
```bash
cd ../revanced-patches
./gradlew test --tests "*ShareSanitizer*"
```

---

## End-to-End APK Testing

### 🎯 Full Patch Validation with Real TikTok APK

### Prerequisites

- TikTok 36.5.4 APK in `apk/orig/` directory
- ReVanced CLI: https://github.com/ReVanced/revanced-cli/releases
- ReVanced Patches JAR (built from upstream)
- ReVanced Integrations APK (built from upstream)
- Android device or emulator with ADB access

### Option A: Test Using ReVanced Manager (Easiest)

**Recommended for users, not developers**

1. Install ReVanced Manager: https://revanced.app/download
2. Select TikTok APK
3. Choose "Share link sanitizer" patch
4. Patch and install

### Option B: Test Using ReVanced CLI (Developer Testing)

**This is what you want for development/validation**

#### 1. Download/Build Required Components

```bash
# Create testing workspace
mkdir -p ~/revanced-testing
cd ~/revanced-testing

# Download ReVanced CLI (or use existing)
wget https://github.com/ReVanced/revanced-cli/releases/latest/download/revanced-cli.jar

# Copy your built artifacts
cp ~/revanced-patches/build/libs/revanced-patches.jar ./
cp ~/revanced-integrations/app/build/outputs/apk/release/revanced-integrations.apk ./

# Copy TikTok APK
cp ~/revanced-tiktok-share-sanitizer/apk/orig/tiktok-36.5.4.apk ./
```

#### 2. List Available Patches

```bash
java -jar revanced-cli.jar list-patches \
  --patch-bundle revanced-patches.jar \
  --with-packages
```

**Expected output should include:**
```
Share link sanitizer
  Description: Removes tracking parameters from TikTok share links and normalizes URLs.
  Compatible packages:
    - com.zhiliaoapp.musically (36.5.4)
    - com.ss.android.ugc.trill (36.5.4)
```

#### 3. Patch TikTok APK

**Include ONLY your patch:**
```bash
java -jar revanced-cli.jar patch \
  --patch-bundle revanced-patches.jar \
  --integrations revanced-integrations.apk \
  --include "Share link sanitizer" \
  --out tiktok-sanitizer-patched.apk \
  tiktok-36.5.4.apk
```

**Include ALL recommended patches + yours:**
```bash
java -jar revanced-cli.jar patch \
  --patch-bundle revanced-patches.jar \
  --integrations revanced-integrations.apk \
  --out tiktok-all-patched.apk \
  tiktok-36.5.4.apk
```

**Expected output:**
```
[INFO] Loading patches...
[INFO] Found 1 patch(es)
[INFO] Executing patches...
[INFO] Share link sanitizer: Executing...
[INFO] Share link sanitizer: Completed
[INFO] Saving APK...
[INFO] Saved to: tiktok-sanitizer-patched.apk
```

#### 4. Install on Device

```bash
# Uninstall existing TikTok (if any)
adb uninstall com.zhiliaoapp.musically

# Install patched APK
adb install tiktok-sanitizer-patched.apk

# Verify installation
adb shell pm list packages | grep tiktok
```

---

## Manual Validation Checklist

### Test Scenarios

Once the patched APK is installed, perform these manual tests:

#### ✅ Scenario 1: Standard URL Sanitization

**Steps:**
1. Open TikTok app
2. Navigate to any video
3. Tap Share → Copy Link
4. Paste the clipboard content into a text editor

**Expected Result:**
```
✅ URL format: https://www.tiktok.com/@username/video/1234567890
✅ No query parameters (u_code, utm_source, etc.)
✅ No fragments (#video, #anchor)
✅ No trailing slash
```

**Failure Indicators:**
```
❌ URL contains: ?u_code=...
❌ URL contains: ?utm_source=...
❌ URL contains: #video
❌ URL ends with trailing /
```

#### ✅ Scenario 2: Shortlink Expansion

**Steps:**
1. Find a video that generates a shortlink (vm.tiktok.com)
2. Tap Share → Copy Link
3. Wait 3 seconds (expansion time)
4. Paste clipboard content

**Expected Result:**
```
✅ URL expanded to canonical format
✅ No vm.tiktok.com or vt.tiktok.com in result
✅ Contains @username and video/ID
```

**Failure Indicators:**
```
❌ URL still contains: vm.tiktok.com/...
❌ Clipboard unchanged (expansion failed)
```

#### ✅ Scenario 3: Network Timeout (Fail-Closed)

**Steps:**
1. Enable airplane mode on device
2. Try to share a video with shortlink
3. Tap Share → Copy Link

**Expected Result:**
```
✅ Toast appears: "Share failed: could not expand shortlink" (or similar)
✅ Clipboard remains unchanged (not updated with unsanitized URL)
✅ No crash or freeze
```

**Failure Indicators:**
```
❌ Clipboard updated with vm.tiktok.com URL (fail-open)
❌ No error message shown
❌ App crashes
```

#### ✅ Scenario 4: Settings Toggle (If Integrated)

**Steps:**
1. Open TikTok Settings → ReVanced → Share Sanitizer
2. Disable "Sanitize share links"
3. Share a video with tracking params
4. Check clipboard

**Expected Result:**
```
✅ Original unsanitized URL in clipboard (feature disabled)
```

**Re-enable and test:**
5. Enable "Sanitize share links"
6. Share same video
7. Check clipboard

**Expected Result:**
```
✅ Sanitized URL in clipboard
```

#### ✅ Scenario 5: Privacy Message (If Setting Enabled)

**Steps:**
1. Open Settings → Share Sanitizer → "Add privacy message" (enable)
2. Share any video
3. Check clipboard

**Expected Result:**
```
https://www.tiktok.com/@user/video/123

Sanitized: tracking removed
```

---

## Monitoring with ADB Logcat

### View Real-Time Logs

```bash
# Filter for your patch
adb logcat | grep -i "ShareSanitizer"

# View all ReVanced extension logs
adb logcat | grep -i "revanced"

# View clipboard-related logs
adb logcat | grep -i "clipboard"
```

### Expected Log Patterns

**Successful sanitization:**
```
I ShareSanitizerHook: Original URL: https://www.tiktok.com/@user/video/123?u_code=abc
I ShareSanitizerHook: Sanitized URL: https://www.tiktok.com/@user/video/123
I ShareSanitizerHook: Clipboard updated
```

**Failure (network timeout):**
```
E ShareSanitizerHook: Expansion timeout for vm.tiktok.com/abc
E ShareSanitizerHook: Returning null (fail-closed)
I Toast: Share failed: Request timeout
```

---

## Debugging Failed Tests

### Patch Not Applied

**Symptom:** URLs still contain tracking parameters

**Troubleshooting:**
1. Verify patch is in the bundle:
   ```bash
   java -jar revanced-cli.jar list-patches --patch-bundle revanced-patches.jar | grep -i share
   ```

2. Check logcat for fingerprint failure:
   ```bash
   adb logcat | grep -i "fingerprint"
   ```

3. Verify TikTok version matches compatibility annotation:
   ```bash
   adb shell dumpsys package com.zhiliaoapp.musically | grep versionName
   ```

### Crash on Share

**Symptom:** App crashes when tapping "Copy Link"

**Troubleshooting:**
1. Check logcat for exceptions:
   ```bash
   adb logcat | grep -E "FATAL|AndroidRuntime"
   ```

2. Common causes:
   - Missing integrations APK
   - Package name mismatch in extension
   - Null pointer in ShareSanitizerHook

### Clipboard Not Updated

**Symptom:** Old clipboard content remains after share

**Troubleshooting:**
1. Check if hook is called:
   ```bash
   adb logcat | grep "ShareSanitizerHook"
   ```

2. Verify bytecode injection:
   - Decompile patched APK with JADX
   - Find `LIZLLL` method in `C98761aTc` class
   - Check for `invoke-static` to `ShareSanitizerHook`

---

## Performance Testing

### Measure Sanitization Overhead

```bash
# Time from share tap to clipboard update
adb shell "am start -a android.intent.action.VIEW -d 'https://www.tiktok.com/@test/video/123'"

# Then share and measure with logcat timestamps
adb logcat -v time | grep "ShareSanitizer"
```

**Target Performance:**
- No shortlink: <100ms
- With shortlink: <3.5s (3s timeout + processing)

---

## Test Matrix

| Scenario | TikTok Version | Android Version | Status |
|----------|----------------|-----------------|--------|
| Standard URL | 36.5.4 US | Android 13 | ⬜ Not Tested |
| Shortlink | 36.5.4 US | Android 13 | ⬜ Not Tested |
| Network offline | 36.5.4 US | Android 13 | ⬜ Not Tested |
| Settings toggle | 36.5.4 US | Android 13 | ⬜ Not Tested |
| Standard URL | 36.5.4 Global | Android 12 | ⬜ Not Tested |
| Shortlink | 36.5.4 Global | Android 12 | ⬜ Not Tested |

**Update this table as you complete tests.**

---

## Continuous Integration (Future)

### Automated APK Testing (Advanced)

For upstream CI/CD:

```yaml
# .github/workflows/patch-test.yml
name: Test TikTok Patch

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: 17

      - name: Run unit tests
        run: ./gradlew test

      - name: Build patch
        run: ./gradlew build

      - name: Download TikTok APK
        run: |
          # Download from trusted mirror
          wget https://... -O tiktok.apk

      - name: Patch APK
        run: |
          java -jar revanced-cli.jar patch \
            --patch-bundle build/libs/revanced-patches.jar \
            --include "Share link sanitizer" \
            tiktok.apk

      - name: Verify patch applied
        run: |
          # Decompile and check for bytecode injection
          jadx -d out/ tiktok-patched.apk
          grep -r "ShareSanitizerHook" out/
```

---

## Summary

### Testing Stages

1. **Unit Tests** ✅ (Available now in standalone repo)
   - 36 tests passing
   - JVM-based, no Android required
   - Fast iteration (<5s)

2. **Integration Tests** ⏳ (Requires upstream merge)
   - Test patch application
   - Verify bytecode injection
   - Check fingerprint matching

3. **APK Tests** ⏳ (Requires device/emulator)
   - Manual validation checklist
   - Real-world scenarios
   - Performance monitoring

### Next Steps

1. ✅ Complete unit tests (DONE)
2. ⬜ Fork upstream repos (revanced-patches, revanced-integrations)
3. ⬜ Migrate patch code to forks
4. ⬜ Build patched APK with ReVanced CLI
5. ⬜ Run manual validation checklist
6. ⬜ Submit PR to upstream with test results

---

## Resources

- **ReVanced CLI**: https://github.com/ReVanced/revanced-cli
- **ReVanced Manager**: https://revanced.app
- **Testing Template**: Copy this file to your upstream fork
- **Bug Reports**: Include logcat output and test scenario

---

**Document Version:** 1.0
**Last Updated:** 2025-10-15
**Maintainer:** Share Sanitizer Development Team
