package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patches.tiktok.misc.sharesanitizer.SanitizerError.NormalizationError
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
     * @return Result containing canonical URL or NormalizationError
     */
    fun normalize(url: String): Result<String, NormalizationError> {
        val uri = try {
            URI(url.trim().removeSuffix("/"))
        } catch (e: Exception) {
            return err(NormalizationError.InvalidFormat(url, e.message))
        }

        // Validate it's a TikTok domain
        val host = uri.host?.lowercase()
            ?: return err(NormalizationError.NoHost(url))

        if (!isTikTokDomain(host)) {
            return err(NormalizationError.NotTikTok(url, host))
        }

        // Decode percent-encoded path
        val decodedPath = URLDecoder.decode(uri.path, "UTF-8")

        // Extract @username and video ID
        val match = VIDEO_PATH_REGEX.find(decodedPath)
            ?: return err(NormalizationError.InvalidPath(url, uri.path))

        val (username, videoId) = match.destructured

        // Validate username and video ID are not empty
        if (username.isBlank()) {
            return err(NormalizationError.InvalidPath(url, "Empty username in path: ${uri.path}"))
        }
        if (videoId.isBlank()) {
            return err(NormalizationError.InvalidPath(url, "Empty video ID in path: ${uri.path}"))
        }

        // Validate video ID contains only digits
        if (!videoId.all { it.isDigit() }) {
            return err(NormalizationError.InvalidPath(url, "Invalid video ID format: $videoId"))
        }

        // Reconstruct canonical URL (no query, no fragment, no trailing slash)
        return ok("https://www.tiktok.com/@$username/video/$videoId")
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
