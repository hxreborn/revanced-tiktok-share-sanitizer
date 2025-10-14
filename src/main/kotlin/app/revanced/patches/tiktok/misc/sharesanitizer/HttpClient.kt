package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * HTTP client abstraction for resolving URL redirects.
 * Abstracts the underlying HTTP implementation (OkHttp) for testability and swappability.
 */
interface HttpClient {
    /**
     * Follows redirects and returns the final destination URL.
     *
     * @param url The URL to resolve
     * @return The final destination URL after following all redirects
     * @throws HttpException if the request fails or exceeds limits
     */
    fun followRedirects(url: String): String

    /**
     * Configuration for HTTP client behavior.
     */
    data class Config(
        val maxRedirects: Int = 5,
        val timeoutSeconds: Int = 3,
        val userAgent: String = "TikTok-Share-Sanitizer/1.0"
    )
}

/**
 * Exception thrown when HTTP operations fail.
 */
class HttpException(message: String, cause: Throwable? = null) : Exception(message, cause)
