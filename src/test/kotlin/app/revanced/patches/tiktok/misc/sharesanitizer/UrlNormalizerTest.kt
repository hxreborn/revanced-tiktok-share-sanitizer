package app.revanced.patches.tiktok.misc.sharesanitizer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UrlNormalizerTest {

    @Test
    fun `normalizes standard TikTok URL`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `strips query parameters`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678?is_from_webapp=1&sender_device=pc"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `strips fragments`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678#trending"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `removes trailing slash`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678/"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `forces www hostname`() {
        val input = "https://tiktok.com/@user123/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `decodes percent-encoded username`() {
        val input = "https://www.tiktok.com/@user%20name/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user name/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `handles regional TikTok domains`() {
        val input = "https://vm.tiktok.com/@user123/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"
        assertEquals(expected, UrlNormalizer.normalize(input))
    }

    @Test
    fun `throws on non-TikTok URL`() {
        assertThrows<IllegalArgumentException> {
            UrlNormalizer.normalize("https://youtube.com/watch?v=abc")
        }
    }

    @Test
    fun `throws on invalid video URL format`() {
        assertThrows<IllegalArgumentException> {
            UrlNormalizer.normalize("https://www.tiktok.com/@user123")
        }
    }

    @Test
    fun `throws on malformed URL`() {
        assertThrows<IllegalArgumentException> {
            UrlNormalizer.normalize("not a url")
        }
    }
}
