package app.revanced.patches.tiktok.misc.sharesanitizer

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ShortlinkExpanderTest {

    // Mock HTTP client that returns a fixed destination
    private class MockHttpClient(private val destination: String) : HttpClient {
        var callCount = 0

        override fun followRedirects(url: String): String {
            callCount++
            return destination
        }
    }

    @Test
    fun `expands vm_tiktok_com shortlink`() {
        val destination = "https://www.tiktok.com/@user123/video/7123456789012345678"
        val mockClient = MockHttpClient(destination)
        val expander = ShortlinkExpander(mockClient)

        val result = expander.expand("https://vm.tiktok.com/abc123")

        assertEquals(destination, result)
        assertEquals(1, mockClient.callCount, "Should call HTTP client for vm.tiktok.com")
    }

    @Test
    fun `expands vt_tiktok_com shortlink`() {
        val destination = "https://www.tiktok.com/@user123/video/7123456789012345678"
        val mockClient = MockHttpClient(destination)
        val expander = ShortlinkExpander(mockClient)

        val result = expander.expand("https://vt.tiktok.com/xyz789")

        assertEquals(destination, result)
        assertEquals(1, mockClient.callCount, "Should call HTTP client for vt.tiktok.com")
    }

    @Test
    fun `does not expand www_tiktok_com URLs`() {
        val mockClient = MockHttpClient("should-not-be-used")
        val expander = ShortlinkExpander(mockClient)

        val regularUrl = "https://www.tiktok.com/@user123/video/7123456789012345678"
        val result = expander.expand(regularUrl)

        assertEquals(regularUrl, result)
        assertEquals(0, mockClient.callCount, "Should not call HTTP client for www.tiktok.com")
    }

    @Test
    fun `does not expand non-tiktok URLs`() {
        val mockClient = MockHttpClient("should-not-be-used")
        val expander = ShortlinkExpander(mockClient)

        val externalUrl = "https://youtube.com/watch?v=abc"
        val result = expander.expand(externalUrl)

        assertEquals(externalUrl, result)
        assertEquals(0, mockClient.callCount, "Should not call HTTP client for non-TikTok URLs")
    }
}
