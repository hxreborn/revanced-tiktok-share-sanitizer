package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patches.tiktok.misc.sharesanitizer.SanitizerError.ExpansionError
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
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

    override fun followRedirects(url: String): Result<String, ExpansionError> {
        var currentUrl = url
        var redirectCount = 0

        while (redirectCount < config.maxRedirects) {
            // Try HEAD first (lighter), fallback to GET if rejected
            val headResult = tryRequest(currentUrl, "HEAD")

            val nextUrl = if (headResult is Result.Err && headResult.error === MethodNotAllowed) {
                // HEAD returned 405, try GET
                when (val getResult = tryRequest(currentUrl, "GET")) {
                    is Result.Ok -> getResult.value
                    is Result.Err -> return err(getResult.error)
                }
            } else {
                when (headResult) {
                    is Result.Ok -> headResult.value
                    is Result.Err -> return err(headResult.error)
                }
            }

            // If no redirect, we're done
            if (nextUrl == currentUrl) {
                return ok(currentUrl)
            }

            currentUrl = nextUrl
            redirectCount++
        }

        return err(ExpansionError.TooManyRedirects(url, config.maxRedirects))
    }

    /**
     * Internal marker for 405 Method Not Allowed (caller should try GET).
     * Not exposed outside this class.
     */
    private object MethodNotAllowed : ExpansionError("Method not allowed (internal marker)")

    /**
     * Attempts a request with the given method.
     * Returns Ok with redirect location if present, or Ok with original URL if no redirect.
     * Returns Err with MethodNotAllowed marker if 405, or other ExpansionError.
     */
    private fun tryRequest(url: String, method: String): Result<String, ExpansionError> {
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
                        val location = response.header("Location")
                            ?: return err(ExpansionError.NoRedirect(url))
                        ok(location)
                    }

                    // Success with no redirect
                    response.isSuccessful -> ok(url)

                    // 405 Method Not Allowed - caller should try GET
                    response.code == 405 -> err(MethodNotAllowed)

                    // Other errors
                    else -> err(ExpansionError.InvalidResponse(url, response.code))
                }
            }
        } catch (e: SocketTimeoutException) {
            err(ExpansionError.Timeout(url))
        } catch (e: IOException) {
            err(ExpansionError.NetworkFailure(url, e.message ?: "Unknown network error"))
        } catch (e: Exception) {
            err(ExpansionError.NetworkFailure(url, e.message ?: "Unknown error"))
        }
    }
}
