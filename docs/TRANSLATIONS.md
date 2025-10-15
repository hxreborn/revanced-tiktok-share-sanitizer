# Translations for TikTok Share Sanitizer

## Overview

This document describes the translation structure for the TikTok Share Sanitizer patch. The project uses Android's string resource system for localization, which is compatible with ReVanced's upstream translation workflow.

## Structure

### Resource Files
- **Base strings**: `src/main/res/values/strings.xml` (English)
- **Translations**: `src/main/res/values-<locale>/strings.xml`

### Supported Languages
- English (default) - `values/`
- Spanish - `values-es/`
- French - `values-fr/`
- German - `values-de/`
- Japanese - `values-ja/`
- Chinese (Simplified) - `values-zh-rCN/`
- Portuguese (Brazil) - `values-pt-rBR/`
- Russian - `values-ru/`

### String Categories
1. **Settings UI** - Category titles, summaries, and labels
2. **Toast Messages** - User feedback messages
3. **Privacy Messages** - Appended text to sanitized URLs
4. **Error Messages** - Localized error descriptions

## Implementation

### StringResources.kt
Centralized string constants that reference Android string resources:
```kotlin
object StringResources {
    const val CATEGORY_TITLE = "Share Sanitizer"
    const val ENABLED_TITLE = "Sanitize share links"
    // ... other constants

    fun getErrorMessage(error: SanitizerError): String {
        // Maps error types to localized messages
    }
}
```

### Code Integration
All user-facing strings are centralized in `StringResources.kt`:
- Settings.kt references `StringResources.*`
- ShareSanitizerHook.kt uses `StringResources.ERROR_PREFIX`
- SanitizerError.kt uses `StringResources.getErrorMessage()`

## Upstream Integration

When merging into `revanced-patches`:

1. **Move string resources** to ReVanced's resource system
2. **Update StringResources.kt** to use Android's `Resources.getString()`
3. **Remove hardcoded constants** - use proper Android resource loading
4. **Follow ReVanced's translation workflow** for community translations

### Example Upstream Integration
```kotlin
// In revanced-integrations
object StringResources {
    private fun getString(resId: Int): String {
        return Utils.getContext().getString(resId)
    }

    val CATEGORY_TITLE: String get() = getString(R.string.category_title)
    // ... other properties
}
```

## Adding New Languages

1. Create directory: `src/main/res/values-<locale>/`
2. Copy `strings.xml` from `values/`
3. Translate all strings, keeping placeholders
4. Test with the respective locale

## Translation Guidelines

- **Keep technical terms** consistent (e.g., "URL", "tracking")
- **Maintain tone** - clear, concise, user-friendly
- **Preserve placeholders** like `%s`, `%d`
- **Test UI layout** with translated strings (some languages are longer)
- **Follow Android localization best practices**

## Current Status

✅ Translation structure implemented
✅ 7 major languages supported
✅ Code centralized in StringResources
⏳ Awaiting upstream integration for Android resource loading

## Notes

- This standalone project uses string constants for compatibility
- In full ReVanced integration, Android's resource system will be used
- Translations are ready for upstream merge
- Community contributions welcome for additional languages