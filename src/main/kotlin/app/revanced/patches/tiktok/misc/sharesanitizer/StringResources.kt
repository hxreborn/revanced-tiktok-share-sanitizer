package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Centralized string resources for the Share Sanitizer patch.
 * These would normally be loaded from Android string resources in a full ReVanced integration.
 */
object StringResources {

    // Settings Category
    const val CATEGORY_TITLE = "Share Sanitizer"
    const val CATEGORY_SUMMARY = "Privacy-focused link sharing"

    // Settings Items
    const val ENABLED_TITLE = "Sanitize share links"
    const val ENABLED_SUMMARY = "Remove tracking parameters from shared TikTok links"
    const val APPEND_MESSAGE_TITLE = "Add privacy message"
    const val APPEND_MESSAGE_SUMMARY = "Append 'Sanitized: tracking removed' to shared links"

    // Toast Messages
    const val EMPTY_URL_ERROR = "Share failed: empty URL"
    const val ERROR_PREFIX = "Share failed:"

    // Privacy Message
    const val PRIVACY_MESSAGE = "\n\nSanitized: tracking removed"

    // Error Messages
    const val ERROR_INVALID_FORMAT = "Invalid URL format"
    const val ERROR_INVALID_URL = "Invalid URL"
    const val ERROR_NOT_TIKTOK = "Not a TikTok URL"
    const val ERROR_INVALID_PATH = "Invalid TikTok video URL"
    const val ERROR_NETWORK = "Network error"
    const val ERROR_TIMEOUT = "Request timeout"
    const val ERROR_TOO_MANY_REDIRECTS = "Too many redirects"
    const val ERROR_NO_REDIRECT = "Failed to expand short link"
    const val ERROR_SERVER = "Server error"
    const val ERROR_UNKNOWN = "Error sanitizing URL"

    /**
     * Gets the localized error message for the given error type.
     * In a full ReVanced integration, this would use Android's resource system.
     */
    fun getErrorMessage(error: SanitizerError): String {
        return when (error) {
            is SanitizerError.NormalizationError.InvalidFormat -> ERROR_INVALID_FORMAT
            is SanitizerError.NormalizationError.NoHost -> ERROR_INVALID_URL
            is SanitizerError.NormalizationError.NotTikTok -> ERROR_NOT_TIKTOK
            is SanitizerError.NormalizationError.InvalidPath -> ERROR_INVALID_PATH
            is SanitizerError.ExpansionError.NetworkFailure -> ERROR_NETWORK
            is SanitizerError.ExpansionError.Timeout -> ERROR_TIMEOUT
            is SanitizerError.ExpansionError.TooManyRedirects -> ERROR_TOO_MANY_REDIRECTS
            is SanitizerError.ExpansionError.NoRedirect -> ERROR_NO_REDIRECT
            is SanitizerError.ExpansionError.InvalidResponse -> ERROR_SERVER
            is SanitizerError.HttpError.RequestFailed -> ERROR_NETWORK
            is SanitizerError.HttpError.IOError -> ERROR_NETWORK
            else -> ERROR_UNKNOWN
        }
    }
}