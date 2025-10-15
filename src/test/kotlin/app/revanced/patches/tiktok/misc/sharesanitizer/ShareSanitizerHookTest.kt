package app.revanced.patches.tiktok.misc.sharesanitizer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for ShareSanitizerHook integration.
 *
 * Note: These tests use mock context (null) since we're in JVM environment.
 * Real Android integration will require instrumentation tests with actual Context.
 */
class ShareSanitizerHookTest {

    @Test
    fun `sanitizeShareUrl handles null input`() {
        val result = ShareSanitizerHook.sanitizeShareUrl(null, null)
        assertNull(result, "Should return null for null URL")
    }

    @Test
    fun `sanitizeShareUrl handles empty input`() {
        val result = ShareSanitizerHook.sanitizeShareUrl("", null)
        assertNull(result, "Should return null for empty URL")
    }

    @Test
    fun `sanitizeShareUrl handles blank input`() {
        val result = ShareSanitizerHook.sanitizeShareUrl("   ", null)
        assertNull(result, "Should return null for blank URL")
    }

    @Test
    fun `sanitizeShareUrl normalizes standard URL`() {
        val input = "https://www.tiktok.com/@testuser/video/1234567890?u_code=tracking"
        val expected = "https://www.tiktok.com/@testuser/video/1234567890"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertEquals(expected, result, "Should strip tracking parameters")
    }

    @Test
    fun `sanitizeShareUrl handles already canonical URL`() {
        val input = "https://www.tiktok.com/@testuser/video/1234567890"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertEquals(input, result, "Should pass through canonical URL")
    }

    @Test
    fun `sanitizeShareUrl handles trailing slash`() {
        val input = "https://www.tiktok.com/@testuser/video/1234567890/"
        val expected = "https://www.tiktok.com/@testuser/video/1234567890"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertEquals(expected, result, "Should remove trailing slash")
    }

    @Test
    fun `sanitizeShareUrl handles fragment`() {
        val input = "https://www.tiktok.com/@testuser/video/1234567890#anchor"
        val expected = "https://www.tiktok.com/@testuser/video/1234567890"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertEquals(expected, result, "Should remove fragment")
    }

    @Test
    fun `sanitizeShareUrl rejects invalid TikTok URL`() {
        val input = "https://www.youtube.com/watch?v=test"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertNull(result, "Should reject non-TikTok URL")
    }

    @Test
    fun `sanitizeShareUrl rejects malformed URL`() {
        val input = "not-a-url"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertNull(result, "Should reject malformed URL")
    }

    @Test
    fun `sanitizeShareUrl rejects URL without video ID`() {
        val input = "https://www.tiktok.com/@testuser"

        val result = ShareSanitizerHook.sanitizeShareUrl(input, null)

        assertNull(result, "Should reject URL without video path")
    }

    @Test
    fun `settings check - enabled by default`() {
        assertTrue(ShareSanitizerSettings.isEnabled(), "Sanitizer should be enabled by default")
    }

    @Test
    fun `settings check - message disabled by default`() {
        assertFalse(ShareSanitizerSettings.shouldAppendMessage(), "Message suffix should be disabled by default")
    }

    @Test
    fun `settings check - privacy message format`() {
        val message = ShareSanitizerSettings.getPrivacyMessage()
        assertTrue(message.contains("Sanitized"), "Privacy message should mention sanitization")
        assertTrue(message.startsWith("\n\n"), "Privacy message should start with newlines for separation")
    }

    @Test
    fun `integration test - full sanitization pipeline`() {
        // Simulate typical share URL from TikTok with tracking
        val dirtyUrl = "https://www.tiktok.com/@user123/video/9876543210?u_code=abc123&utm_source=copy#video"
        val cleanUrl = "https://www.tiktok.com/@user123/video/9876543210"

        val result = ShareSanitizerHook.sanitizeShareUrl(dirtyUrl, null)

        assertNotNull(result, "Should successfully sanitize URL")
        assertEquals(cleanUrl, result, "Should produce clean canonical URL")
        assertFalse(result!!.contains("u_code"), "Should remove tracking parameter")
        assertFalse(result.contains("utm_source"), "Should remove UTM parameter")
        assertFalse(result.contains("#"), "Should remove fragment")
    }
}
