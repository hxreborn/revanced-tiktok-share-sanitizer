package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Expands TikTok shortlinks (vm.tiktok.com, vt.tiktok.com) to their full destination URLs.
 */
class ShortlinkExpander(private val httpClient: HttpClient) {

    /**
     * Expands a TikTok URL if it's a shortlink, otherwise returns it unchanged.
     *
     * @param url The URL to potentially expand
     * @return The expanded URL if it was a shortlink, or the original URL
     * @throws HttpException if shortlink expansion fails
     */
    fun expand(url: String): String {
        return if (isShortlink(url)) {
            httpClient.followRedirects(url)
        } else {
            url
        }
    }

    /**
     * Checks if a URL is a TikTok shortlink that needs expansion.
     */
    private fun isShortlink(url: String): Boolean {
        val host = try {
            java.net.URI(url).host?.lowercase()
        } catch (e: Exception) {
            return false
        }

        return host == "vm.tiktok.com" || host == "vt.tiktok.com"
    }

    companion object {
        /**
         * Creates a ShortlinkExpander with default OkHttp client configuration.
         */
        fun create(config: HttpClient.Config = HttpClient.Config()): ShortlinkExpander {
            return ShortlinkExpander(OkHttpClientAdapter(config))
        }
    }
}
