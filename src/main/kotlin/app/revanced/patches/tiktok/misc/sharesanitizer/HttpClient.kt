package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patches.tiktok.misc.sharesanitizer.SanitizerError.ExpansionError

/**
 * HTTP client abstraction for resolving URL redirects.
 * Abstracts the underlying HTTP implementation (OkHttp) for testability and swappability.
 */
interface HttpClient {
    /**
     * Follows redirects and returns the final destination URL.
     *
     * @param url The URL to resolve
     * @return Result containing the final destination URL or ExpansionError
     */
    fun followRedirects(url: String): Result<String, ExpansionError>

    /**
     * Configuration for HTTP client behavior.
     */
    data class Config(
        val maxRedirects: Int = 5,
        val timeoutSeconds: Int = 3,
        val userAgent: String = "TikTok-Share-Sanitizer/1.0"
    )
}
