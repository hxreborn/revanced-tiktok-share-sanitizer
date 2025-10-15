# Extension Integration Guide

This document outlines the integration points for the TikTok Share Sanitizer patch with the ReVanced extension system.

## Extension Structure

The share sanitizer functionality will be integrated into the TikTok extension module:

```
extensions/tiktok/src/main/kotlin/app/revanced/extension/tiktok/sharesanitizer/
├── ShareSanitizerPatch.kt          # Extension entry point
├── ShareSanitizer.kt               # Core sanitization logic
└── ShareSanitizerSettings.kt       # Extension settings (if needed)
```

## Settings Integration

Add to `extensions/tiktok/src/main/kotlin/app/revanced/extension/tiktok/settings/Settings.kt`:

```kotlin
// In the Settings object
@JvmField
val SHARE_SANITIZER_ENABLED = BooleanSetting(
    "revanced_tiktok_share_sanitizer_enabled",
    false,
    true // Export to preferences
)

@JvmField
val SHARE_SANITIZER_APPEND_MESSAGE = BooleanSetting(
    "revanced_tiktok_share_sanitizer_append_message",
    false,
    true
)
```

## Extension Implementation

### ShareSanitizer.kt
```kotlin
package app.revanced.extension.tiktok.sharesanitizer

import app.revanced.extension.tiktok.settings.Settings

object ShareSanitizer {

    fun sanitizeUrl(originalUrl: String?): String? {
        if (!Settings.SHARE_SANITIZER_ENABLED.get()) {
            return originalUrl
        }

        return originalUrl?.let { url ->
            try {
                // Call the core sanitization logic
                // This will be implemented as a bridge to the Kotlin code
                normalizeTikTokUrl(url)
            } catch (e: Exception) {
                // Log error and return null to maintain fail-closed behavior
                // The patch will show a toast and block the share
                null
            }
        }
    }

    private fun normalizeTikTokUrl(url: String): String {
        // Core URL normalization logic will be ported here
        // or called via JNI from the patch code
        return url // Placeholder
    }

    fun shouldAppendMessage(): Boolean {
        return Settings.SHARE_SANITIZER_APPEND_MESSAGE.get()
    }
}
```

### ShareSanitizerPatch.kt
```kotlin
package app.revanced.extension.tiktok.sharesanitizer

import app.revanced.extension.tiktok.settings.Settings

object ShareSanitizerPatch {

    @JvmStatic
    fun interceptShareIntent(originalUrl: String?): String? {
        return ShareSanitizer.sanitizeUrl(originalUrl)
    }

    @JvmStatic
    fun getShareSuffix(): String? {
        return if (ShareSanitizer.shouldAppendMessage()) {
            "\n\nShared via ReVanced"
        } else {
            null
        }
    }
}
```

## Patch Integration Points

The ReVanced patch will hook into TikTok's share functionality at these points:

### 1. Clipboard Copy Hook
```kotlin
// In the patch's execute block
clipboardCopyFingerprint.method?.let { method ->
    // Hook the clipboard copy method
    interceptClipboardCopy(method)
}
```

### 2. Share Intent Construction
```kotlin
// Hook share intent creation
shareIntentFingerprint.method?.let { method ->
    interceptShareIntent(method)
}
```

## Settings Integration

The extension will register its settings in the preferences:

```kotlin
// In the appropriate preferences category
addSwitch(
    "Share Sanitizer",
    "Enable share link sanitization",
    Settings.SHARE_SANITIZER_ENABLED
)

addSwitch(
    "Append Message",
    "Append 'Shared via ReVanced' to sanitized shares",
    Settings.SHARE_SANITIZER_APPEND_MESSAGE
)
```

## Fail-Closed Implementation

The extension will implement fail-closed behavior:

```kotlin
fun sanitizeWithFailClosed(originalUrl: String?): String? {
    return try {
        val sanitized = ShareSanitizer.sanitizeUrl(originalUrl)
        if (sanitized == null || sanitized != originalUrl && !isValidTikTokUrl(sanitized)) {
            // Invalid sanitization result - block the share
            showToast("Share sanitization failed")
            null // Signal to block the share
        } else {
            sanitized
        }
    } catch (e: Exception) {
        showToast("Share sanitization failed")
        null // Signal to block the share
    }
}
```

## Integration Dependencies

The patch will depend on:
- `sharedExtensionPatch` - For extension integration
- `settingsPatch` - For settings access

## Testing Integration

Extension tests will be added to:
```
extensions/tiktok/src/test/kotlin/app/revanced/extension/tiktok/sharesanitizer/
├── ShareSanitizerTest.kt
└── ShareSanitizerPatchTest.kt
```

## Migration Notes

1. **Kotlin Code Porting**: The core sanitization logic from this standalone project will need to be ported to the extension module
2. **JNI Integration**: Consider using JNI to call the existing Kotlin code from Java extension methods
3. **Settings Migration**: Settings will be migrated to the extension's Settings object
4. **Dependency Resolution**: Ensure all dependencies are available in the extension module

## Next Steps

1. Complete reverse engineering of TikTok's share functionality
2. Identify exact hook points and fingerprint patterns
3. Port core sanitization logic to extension module
4. Implement fail-closed behavior with proper error handling
5. Add comprehensive testing for extension integration