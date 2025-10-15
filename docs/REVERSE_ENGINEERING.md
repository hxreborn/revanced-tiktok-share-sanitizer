# TikTok APK Reverse Engineering Findings

This document captures bytecode patterns, method signatures, and hook points discovered during TikTok APK analysis for the Share Sanitizer patch.

## Overview

**Status:** Phase 4 not started - awaiting reverse engineering
**Target TikTok Versions:** TBD (recommended: 36.5.4)
**Tools Used:** JADX, Bytecode Viewer, apktool

## Hook Point 1: Share Intent Construction

**Status:** 🔍 Not yet identified

**Target Behavior:** Intercept the method that constructs the share intent payload before it's passed to Android's share framework.

### Search Strategy
1. Decompile TikTok APK with JADX
2. Search for:
   - `Intent.ACTION_SEND` usages
   - `ClipboardManager.setPrimaryClip` calls
   - String literals: `"https://vm.tiktok.com"`, `"https://vt.tiktok.com"`, `"https://www.tiktok.com"`
3. Trace call hierarchy to identify share entry point

### Expected Patterns
```java
// Example pattern to look for (actual code will vary)
Intent shareIntent = new Intent(Intent.ACTION_SEND);
shareIntent.putExtra(Intent.EXTRA_TEXT, videoUrl);
shareIntent.setType("text/plain");
startActivity(Intent.createChooser(shareIntent, "Share"));
```

### Findings

**Class Name:** TBD
**Method Signature:** TBD
**Bytecode Pattern:** TBD

```smali
# Paste relevant Smali instructions here
```

**Injection Point:** TBD (instruction index)

---

## Hook Point 2: Clipboard Write

**Status:** 🔍 Not yet identified

**Target Behavior:** Intercept clipboard operations when user taps "Copy Link" in share sheet.

### Search Strategy
1. Search for `ClipboardManager` usages
2. Look for `setPrimaryClip(ClipData)` calls
3. Identify context: share dialog, video player, profile pages

### Expected Patterns
```java
ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
ClipData clip = ClipData.newPlainText("video_link", videoUrl);
clipboard.setPrimaryClip(clip);
```

### Findings

**Class Name:** TBD
**Method Signature:** TBD
**Bytecode Pattern:** TBD

```smali
# Paste relevant Smali instructions here
```

**Injection Point:** TBD

---

## URL Generation Logic

**Status:** 🔍 Not yet identified

**Target Behavior:** Understand how TikTok generates `vm.tiktok.com` and `vt.tiktok.com` shortlinks vs. canonical `www.tiktok.com/@user/video/id` URLs.

### Questions to Answer
- Does TikTok generate shortlinks server-side or client-side?
- Are shortlinks cached/stored in the app?
- What triggers canonical URL vs. shortlink generation?

### Findings

**Server-side or client-side?** TBD
**Shortlink generation logic:** TBD
**Relevant classes/methods:** TBD

---

## Fingerprint Creation

### Fingerprint 1: ShareIntentFingerprint

**Status:** ⏳ Blocked on Hook Point 1 findings

**Purpose:** Match the method that constructs share intents

```kotlin
// Placeholder - will be implemented after RE
val shareIntentFingerprint = fingerprint {
    returns("V") // void
    parameters("Landroid/content/Context;", "Ljava/lang/String;")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        // ... more opcodes
    )
    strings("https://vm.tiktok.com", "https://www.tiktok.com")
}
```

### Fingerprint 2: ClipboardWriteFingerprint

**Status:** ⏳ Blocked on Hook Point 2 findings

```kotlin
// Placeholder
val clipboardWriteFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/lang/String;")
    // ... opcodes
}
```

---

## Tested Versions

| TikTok Version | Package | Build Date | Status | Notes |
|----------------|---------|------------|--------|-------|
| 36.5.4 | `com.zhiliaoapp.musically` | TBD | ⏳ Not tested | Recommended target |
| 36.5.4 | `com.ss.android.ugc.trill` | TBD | ⏳ Not tested | Global variant |

---

## ReVanced Patch Skeleton

Based on findings above, the patch structure will be:

```kotlin
package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patcher.patch.bytecodePatch

val shareSanitizerPatch = bytecodePatch(
    name = "Share Sanitizer",
    description = "Removes tracking parameters from TikTok share links."
) {
    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4"),
        "com.zhiliaoapp.musically"("36.5.4")
    )

    execute {
        // Hook point 1: Share intent construction
        shareIntentFingerprint.match(context).method.addInstructions(
            index = 0, // TBD based on RE findings
            """
                invoke-static {p1}, Lapp/revanced/extension/tiktok/sharesanitizer/ShareSanitizerHook;->sanitizeUrl(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
            """
        )

        // Hook point 2: Clipboard write (if separate from above)
        // TBD
    }
}
```

---

## Extension Hook (revanced-integrations)

The companion extension class will be:

```kotlin
package app.revanced.extension.tiktok.sharesanitizer

import app.revanced.patches.tiktok.misc.sharesanitizer.ShortlinkExpander
import app.revanced.patches.tiktok.misc.sharesanitizer.UrlNormalizer

object ShareSanitizerHook {
    @JvmStatic
    fun sanitizeUrl(originalUrl: String): String {
        return try {
            val expanded = if (ShortlinkExpander.isShortlink(originalUrl)) {
                ShortlinkExpander.expand(originalUrl)
            } else {
                originalUrl
            }

            val normalized = UrlNormalizer.normalize(expanded)

            if (ShareSanitizerSettings.shouldAppendMessage()) {
                "$normalized\n\nAnonymized share: clean link, tracking removed."
            } else {
                normalized
            }
        } catch (e: Exception) {
            // Fail closed: return empty string to block share
            // Toast will be shown by patch
            ""
        }
    }
}
```

---

## Next Steps

1. **Download TikTok APK** (version 36.5.4 recommended)
   - US variant: `com.zhiliaoapp.musically`
   - Global variant: `com.ss.android.ugc.trill`

2. **Decompile with JADX**
   ```bash
   jadx -d output/ tiktok-36.5.4.apk
   ```

3. **Search for hook points** (see strategies above)

4. **Document findings** in this file (class names, method signatures, Smali)

5. **Create fingerprints** based on bytecode patterns

6. **Implement ShareSanitizerPatch.kt** with fingerprints and injection logic

7. **Test with ReVanced CLI**
   ```bash
   revanced-cli patch \
     --patch-bundle revanced-patches.jar \
     --integrations revanced-integrations.apk \
     --out tiktok-patched.apk \
     tiktok-36.5.4.apk
   ```

8. **Manual validation** on device with logcat monitoring

---

## Notes

- **Compatibility:** Each TikTok version may have different bytecode; fingerprints must be resilient
- **Regional Variants:** Test both `musically` (US) and `trill` (global) packages
- **Obfuscation:** TikTok may use ProGuard/R8; expect obfuscated class/method names
- **Updates:** Document any breaking changes in newer TikTok versions
