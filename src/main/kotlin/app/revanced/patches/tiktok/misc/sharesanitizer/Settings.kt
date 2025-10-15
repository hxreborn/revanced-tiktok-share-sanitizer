package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Settings for TikTok Share Sanitizer patch.
 *
 * This is a standalone incubator file. When integrating with upstream revanced-patches,
 * these settings will be moved to revanced-integrations and wired into the TikTok settings UI.
 *
 * ## Setting Keys
 * These keys will be used in SharedPreferences when integrated with ReVanced settings:
 *
 * ### revanced_tiktok_share_sanitizer_enabled
 * - **Type**: Boolean
 * - **Default**: true
 * - **Purpose**: Master toggle for the sanitizer patch
 * - **UI Label**: "Sanitize share links"
 * - **UI Description**: "Remove tracking parameters from shared TikTok links"
 *
 * ### revanced_tiktok_share_sanitizer_append_message
 * - **Type**: Boolean
 * - **Default**: false
 * - **Purpose**: Append privacy message to sanitized URLs
 * - **UI Label**: "Add privacy message"
 * - **UI Description**: "Append 'Sanitized: tracking removed' to shared links"
 * - **Message Text**: "\n\nSanitized: tracking removed"
 *
 * ## Integration Notes
 *
 * When porting to upstream, create these classes in revanced-integrations:
 *
 * ```kotlin
 * // revanced-integrations/app/src/main/java/app/revanced/extension/tiktok/settings/Settings.kt
 * enum class Settings(val key: String, val defaultValue: Any) {
 *     SHARE_SANITIZER_ENABLED("revanced_tiktok_share_sanitizer_enabled", true),
 *     SHARE_SANITIZER_APPEND_MESSAGE("revanced_tiktok_share_sanitizer_append_message", false);
 *
 *     val boolean: Boolean
 *         get() = SharedPrefHelper.getBoolean(key, defaultValue as Boolean)
 * }
 * ```
 *
 * And wire into the TikTok SettingsPatch to add preferences to the settings UI.
 */
object Settings {
    /**
     * Setting keys used by SharedPreferences.
     */
    object Keys {
        const val ENABLED = "revanced_tiktok_share_sanitizer_enabled"
        const val APPEND_MESSAGE = "revanced_tiktok_share_sanitizer_append_message"
    }

    /**
     * Default values for settings.
     */
    object Defaults {
        const val ENABLED = true
        const val APPEND_MESSAGE = false
    }

    /**
     * UI strings for settings menu.
     * These will be used when creating the settings UI in revanced-integrations.
     */
    object UI {
        const val CATEGORY_TITLE = "Share Sanitizer"
        const val CATEGORY_SUMMARY = "Privacy-focused link sharing"

        const val ENABLED_TITLE = "Sanitize share links"
        const val ENABLED_SUMMARY = "Remove tracking parameters from shared TikTok links"

        const val APPEND_MESSAGE_TITLE = "Add privacy message"
        const val APPEND_MESSAGE_SUMMARY = "Append 'Sanitized: tracking removed' to shared links"
    }

    /**
     * The message appended to sanitized URLs when enabled.
     */
    const val PRIVACY_MESSAGE = "\n\nSanitized: tracking removed"
}
