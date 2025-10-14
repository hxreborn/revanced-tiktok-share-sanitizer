package app.revanced.patches.tiktok.misc.sharesanitizer

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

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

        val result = client.followRedirects(server.url("/abc123").toString())

        assertEquals(finalUrl, result)
    }

    @Test
    fun `returns URL immediately if no redirect`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val testUrl = server.url("/page").toString()
        val result = client.followRedirects(testUrl)

        assertEquals(testUrl, result)
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

        val result = client.followRedirects(server.url("/start").toString())

        assertEquals(finalUrl, result)
    }

    @Test
    fun `throws on redirect loop exceeding max redirects`() {
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

        assertThrows<HttpException> {
            client.followRedirects(url1)
        }
    }

    @Test
    fun `throws on missing Location header`() {
        server.enqueue(MockResponse().setResponseCode(302)) // No Location header

        assertThrows<HttpException> {
            client.followRedirects(server.url("/broken").toString())
        }
    }

    @Test
    fun `throws on 404 not found`() {
        server.enqueue(MockResponse().setResponseCode(404))

        assertThrows<HttpException> {
            client.followRedirects(server.url("/notfound").toString())
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

        val result = client.followRedirects(server.url("/api").toString())

        assertEquals(finalUrl, result)
    }
}
