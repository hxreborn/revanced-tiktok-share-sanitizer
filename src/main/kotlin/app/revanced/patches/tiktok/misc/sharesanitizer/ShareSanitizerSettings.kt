package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Settings accessor for Share Sanitizer patch.
 *
 * This is a mock implementation for the standalone incubator. When integrated with
 * upstream revanced-integrations, this will be replaced with actual SharedPreferences access.
 *
 * ## Upstream Integration
 *
 * In revanced-integrations, this class will be replaced with:
 *
 * ```kotlin
 * package app.revanced.extension.tiktok.sharesanitizer
 *
 * import app.revanced.extension.shared.settings.BaseSettings
 *
 * object ShareSanitizerSettings {
 *     private val preferences = BaseSettings.preferences
 *
 *     @JvmStatic
 *     fun isEnabled(): Boolean {
 *         return preferences.getBoolean(
 *             Settings.Keys.ENABLED,
 *             Settings.Defaults.ENABLED
 *         )
 *     }
 *
 *     @JvmStatic
 *     fun shouldAppendMessage(): Boolean {
 *         return preferences.getBoolean(
 *             Settings.Keys.APPEND_MESSAGE,
 *             Settings.Defaults.APPEND_MESSAGE
 *         )
 *     }
 *
 *     @JvmStatic
 *     fun getPrivacyMessage(): String {
 *         return Settings.PRIVACY_MESSAGE
 *     }
 * }
 * ```
 *
 * ## Current Behavior (Standalone)
 *
 * For testing in this incubator, settings are hardcoded to defaults:
 * - Sanitizer is always enabled
 * - Message suffix is never appended
 *
 * This allows testing the core sanitization logic without needing Android SharedPreferences.
 */
object ShareSanitizerSettings {
    /**
     * Check if the sanitizer is enabled.
     *
     * In standalone mode: always returns true
     * In upstream integration: reads from SharedPreferences
     */
    @JvmStatic
    fun isEnabled(): Boolean {
        // TODO: Replace with SharedPreferences access when integrated
        return Settings.Defaults.ENABLED
    }

    /**
     * Check if privacy message should be appended to sanitized URLs.
     *
     * In standalone mode: always returns false
     * In upstream integration: reads from SharedPreferences
     */
    @JvmStatic
    fun shouldAppendMessage(): Boolean {
        // TODO: Replace with SharedPreferences access when integrated
        return Settings.Defaults.APPEND_MESSAGE
    }

    /**
     * Get the privacy message text.
     */
    @JvmStatic
    fun getPrivacyMessage(): String {
        return Settings.PRIVACY_MESSAGE
    }
}
