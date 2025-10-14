package app.revanced.patches.tiktok.misc.sharesanitizer

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp implementation of HttpClient interface.
 * Handles redirect following with HEAD request (GET fallback).
 */
class OkHttpClientAdapter(
    private val config: HttpClient.Config = HttpClient.Config()
) : HttpClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .followRedirects(false) // Manual redirect handling for better control
        .build()

    override fun followRedirects(url: String): String {
        var currentUrl = url
        var redirectCount = 0

        while (redirectCount < config.maxRedirects) {
            // Try HEAD first (lighter), fallback to GET if rejected
            val finalUrl = tryRequest(currentUrl, "HEAD")
                ?: tryRequest(currentUrl, "GET")
                ?: throw HttpException("Both HEAD and GET requests failed for: $currentUrl")

            // If no redirect, we're done
            if (finalUrl == currentUrl) {
                return currentUrl
            }

            currentUrl = finalUrl
            redirectCount++
        }

        throw HttpException("Exceeded max redirects ($config.maxRedirects) for: $url")
    }

    /**
     * Attempts a request with the given method.
     * Returns the redirect location if present, or the original URL if no redirect.
     * Returns null if the request fails (405 or network error only).
     */
    private fun tryRequest(url: String, method: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .method(method, null)
                .header("User-Agent", config.userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    // Redirect codes
                    response.code in 300..399 -> {
                        response.header("Location")
                            ?: throw HttpException("Redirect response without Location header: ${response.code}")
                    }

                    // Success with no redirect
                    response.isSuccessful -> url

                    // 405 Method Not Allowed - caller should try GET
                    response.code == 405 -> null

                    // Other errors
                    else -> throw HttpException("HTTP error ${response.code} for: $url")
                }
            }
        } catch (e: HttpException) {
            throw e // Re-throw HttpException
        } catch (e: IOException) {
            null // Network failure, let caller try alternative method
        }
    }
}
