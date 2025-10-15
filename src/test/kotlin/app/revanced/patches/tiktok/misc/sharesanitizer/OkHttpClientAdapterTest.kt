package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patches.tiktok.misc.sharesanitizer.SanitizerError.ExpansionError
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkHttpClientAdapterTest {

    private lateinit var server: MockWebServer
    private lateinit var client: HttpClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()

        val config = HttpClient.Config(maxRedirects = 5, timeoutSeconds = 3)
        client = OkHttpClientAdapter(config)
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `follows single redirect`() {
        val finalUrl = "https://www.tiktok.com/@user123/video/7123456789012345678"

        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", finalUrl)
        )

        when (val result = client.followRedirects(server.url("/abc123").toString())) {
            is Result.Ok -> assertEquals(finalUrl, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `returns URL immediately if no redirect`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val testUrl = server.url("/page").toString()

        when (val result = client.followRedirects(testUrl)) {
            is Result.Ok -> assertEquals(testUrl, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `follows chained redirects`() {
        val intermediate = server.url("/intermediate").toString()
        val finalUrl = "https://www.tiktok.com/@user123/video/7123456789012345678"

        // First redirect
        server.enqueue(
            MockResponse()
                .setResponseCode(301)
                .setHeader("Location", intermediate)
        )

        // Second redirect
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", finalUrl)
        )

        when (val result = client.followRedirects(server.url("/start").toString())) {
            is Result.Ok -> assertEquals(finalUrl, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `returns error on redirect loop exceeding max redirects`() {
        val url1 = server.url("/step1").toString()
        val url2 = server.url("/step2").toString()

        // Create a chain of redirects longer than maxRedirects (5)
        repeat(6) { index ->
            val nextUrl = if (index % 2 == 0) url2 else url1
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", nextUrl)
            )
        }

        when (val result = client.followRedirects(url1)) {
            is Result.Ok -> throw AssertionError("Expected Err, got Ok: ${result.value}")
            is Result.Err -> assertTrue(result.error is ExpansionError.TooManyRedirects)
        }
    }

    @Test
    fun `returns error on missing Location header`() {
        server.enqueue(MockResponse().setResponseCode(302)) // No Location header

        when (val result = client.followRedirects(server.url("/broken").toString())) {
            is Result.Ok -> throw AssertionError("Expected Err, got Ok: ${result.value}")
            is Result.Err -> assertTrue(result.error is ExpansionError.NoRedirect)
        }
    }

    @Test
    fun `returns error on 404 not found`() {
        server.enqueue(MockResponse().setResponseCode(404))

        when (val result = client.followRedirects(server.url("/notfound").toString())) {
            is Result.Ok -> throw AssertionError("Expected Err, got Ok: ${result.value}")
            is Result.Err -> assertTrue(result.error is ExpansionError.InvalidResponse)
        }
    }

    @Test
    fun `falls back to GET when HEAD returns 405`() {
        val finalUrl = "https://www.tiktok.com/@user123/video/7123456789012345678"

        // HEAD returns 405
        server.enqueue(MockResponse().setResponseCode(405))

        // GET succeeds with redirect
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", finalUrl)
        )

        when (val result = client.followRedirects(server.url("/api").toString())) {
            is Result.Ok -> assertEquals(finalUrl, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }
}
