package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Base class for all sanitization errors.
 *
 * Provides user-friendly error messages for Phase 4 toast notifications.
 */
sealed class SanitizerError(val message: String) {

    /**
     * URL normalization errors.
     */
    sealed class NormalizationError(message: String) : SanitizerError(message) {
        data class InvalidFormat(val url: String, val cause: String? = null) :
            NormalizationError("Invalid URL format: $url${cause?.let { " ($it)" } ?: ""}")

        data class NoHost(val url: String) :
            NormalizationError("URL has no host: $url")

        data class NotTikTok(val url: String, val host: String) :
            NormalizationError("Not a TikTok URL: $url (host: $host)")

        data class InvalidPath(val url: String, val path: String) :
            NormalizationError("URL does not contain valid @user/video/id format: $url")
    }

    /**
     * Shortlink expansion errors.
     */
    sealed class ExpansionError(message: String) : SanitizerError(message) {
        data class NetworkFailure(val url: String, val cause: String) :
            ExpansionError("Network error expanding $url: $cause")

        data class Timeout(val url: String) :
            ExpansionError("Timeout expanding $url")

        data class TooManyRedirects(val url: String, val maxRedirects: Int) :
            ExpansionError("Too many redirects expanding $url (max: $maxRedirects)")

        data class NoRedirect(val url: String) :
            ExpansionError("Short link did not redirect: $url")

        data class InvalidResponse(val url: String, val statusCode: Int) :
            ExpansionError("Invalid response expanding $url: HTTP $statusCode")
    }

    /**
     * Generic HTTP client errors.
     */
    sealed class HttpError(message: String) : SanitizerError(message) {
        data class RequestFailed(val url: String, val cause: String) :
            HttpError("HTTP request failed for $url: $cause")

        data class IOError(val url: String, val cause: String) :
            HttpError("I/O error for $url: $cause")
    }

    /**
     * Returns a short user-friendly message suitable for toasts.
     */
    fun toToastMessage(): String = when (this) {
        is NormalizationError.InvalidFormat -> "Invalid URL format"
        is NormalizationError.NoHost -> "Invalid URL"
        is NormalizationError.NotTikTok -> "Not a TikTok URL"
        is NormalizationError.InvalidPath -> "Invalid TikTok video URL"
        is ExpansionError.NetworkFailure -> "Network error"
        is ExpansionError.Timeout -> "Request timeout"
        is ExpansionError.TooManyRedirects -> "Too many redirects"
        is ExpansionError.NoRedirect -> "Failed to expand short link"
        is ExpansionError.InvalidResponse -> "Server error"
        is HttpError.RequestFailed -> "Network error"
        is HttpError.IOError -> "Network error"
        else -> "Error sanitizing URL" // Catch-all for internal errors
    }

    override fun toString(): String = message
}
