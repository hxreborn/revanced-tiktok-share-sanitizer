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
 * - **UI Label**: StringResources.ENABLED_TITLE
 * - **UI Description**: StringResources.ENABLED_SUMMARY
 *
 * ### revanced_tiktok_share_sanitizer_append_message
 * - **Type**: Boolean
 * - **Default**: false
 * - **Purpose**: Append privacy message to sanitized URLs
 * - **UI Label**: StringResources.APPEND_MESSAGE_TITLE
 * - **UI Description**: StringResources.APPEND_MESSAGE_SUMMARY
 * - **Message Text**: StringResources.PRIVACY_MESSAGE
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
     * NOTE: These strings now reference StringResources for centralization.
     */
    object UI {
        const val CATEGORY_TITLE = StringResources.CATEGORY_TITLE
        const val CATEGORY_SUMMARY = StringResources.CATEGORY_SUMMARY

        const val ENABLED_TITLE = StringResources.ENABLED_TITLE
        const val ENABLED_SUMMARY = StringResources.ENABLED_SUMMARY

        const val APPEND_MESSAGE_TITLE = StringResources.APPEND_MESSAGE_TITLE
        const val APPEND_MESSAGE_SUMMARY = StringResources.APPEND_MESSAGE_SUMMARY
    }

    /**
     * The message appended to sanitized URLs when enabled.
     * NOTE: This now references StringResources for centralization.
     */
    const val PRIVACY_MESSAGE = StringResources.PRIVACY_MESSAGE
}
