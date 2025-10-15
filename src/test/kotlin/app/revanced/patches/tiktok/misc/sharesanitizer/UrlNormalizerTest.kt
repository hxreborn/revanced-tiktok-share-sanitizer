package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patches.tiktok.misc.sharesanitizer.SanitizerError.NormalizationError
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UrlNormalizerTest {

    @Test
    fun `normalizes standard TikTok URL`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `strips query parameters`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678?is_from_webapp=1&sender_device=pc"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `strips fragments`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678#trending"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `removes trailing slash`() {
        val input = "https://www.tiktok.com/@user123/video/7123456789012345678/"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `forces www hostname`() {
        val input = "https://tiktok.com/@user123/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `decodes percent-encoded username`() {
        val input = "https://www.tiktok.com/@user%20name/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user name/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `handles regional TikTok domains`() {
        val input = "https://vm.tiktok.com/@user123/video/7123456789012345678"
        val expected = "https://www.tiktok.com/@user123/video/7123456789012345678"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> assertEquals(expected, result.value)
            is Result.Err -> throw AssertionError("Expected Ok, got Err: ${result.error}")
        }
    }

    @Test
    fun `returns error on non-TikTok URL`() {
        val input = "https://youtube.com/watch?v=abc"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> throw AssertionError("Expected Err, got Ok: ${result.value}")
            is Result.Err -> assertTrue(result.error is NormalizationError.NotTikTok)
        }
    }

    @Test
    fun `returns error on invalid video URL format`() {
        val input = "https://www.tiktok.com/@user123"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> throw AssertionError("Expected Err, got Ok: ${result.value}")
            is Result.Err -> assertTrue(result.error is NormalizationError.InvalidPath)
        }
    }

    @Test
    fun `returns error on malformed URL`() {
        val input = "not a url"

        when (val result = UrlNormalizer.normalize(input)) {
            is Result.Ok -> throw AssertionError("Expected Err, got Ok: ${result.value}")
            is Result.Err -> assertTrue(result.error is NormalizationError.InvalidFormat)
        }
    }
}
