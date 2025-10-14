package app.revanced.patches.tiktok.misc.sharesanitizer

import java.net.URI
import java.net.URLDecoder

/**
 * Normalizes TikTok URLs to canonical form: https://www.tiktok.com/@USER/video/ID
 *
 * Handles:
 * - Various TikTok domains (www, vm, vt, regional)
 * - Query parameters and fragments
 * - Percent-encoded paths
 * - Trailing slashes
 */
object UrlNormalizer {

    private val VIDEO_PATH_REGEX = Regex("""/@([^/]+)/video/(\d+)""")

    /**
     * Normalizes a TikTok URL to canonical form.
     *
     * @param url The TikTok URL to normalize (already expanded if it was a shortlink)
     * @return Canonical URL: https://www.tiktok.com/@USER/video/ID
     * @throws IllegalArgumentException if URL is not a valid TikTok video URL
     */
    fun normalize(url: String): String {
        val uri = try {
            URI(url.trim().removeSuffix("/"))
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL format: $url", e)
        }

        // Validate it's a TikTok domain
        val host = uri.host?.lowercase() ?: throw IllegalArgumentException("URL has no host: $url")
        if (!isTikTokDomain(host)) {
            throw IllegalArgumentException("Not a TikTok URL: $url")
        }

        // Decode percent-encoded path
        val decodedPath = URLDecoder.decode(uri.path, "UTF-8")

        // Extract @username and video ID
        val match = VIDEO_PATH_REGEX.find(decodedPath)
            ?: throw IllegalArgumentException("URL does not contain valid @user/video/id format: $url")

        val (username, videoId) = match.destructured

        // Reconstruct canonical URL (no query, no fragment, no trailing slash)
        return "https://www.tiktok.com/@$username/video/$videoId"
    }

    /**
     * Checks if a hostname is a valid TikTok domain.
     */
    private fun isTikTokDomain(host: String): Boolean {
        return host == "www.tiktok.com" ||
               host == "tiktok.com" ||
               host == "vm.tiktok.com" ||
               host == "vt.tiktok.com" ||
               host.endsWith(".tiktok.com")
    }
}
