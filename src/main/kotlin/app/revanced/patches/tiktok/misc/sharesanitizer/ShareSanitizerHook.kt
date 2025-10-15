package app.revanced.patches.tiktok.misc.sharesanitizer

/**
 * Hook implementation for TikTok Share Sanitizer.
 *
 * This is called from the bytecode injection in ShareSanitizerPatch.kt.
 * It orchestrates the complete sanitization pipeline:
 *
 * 1. Check if sanitizer is enabled (settings)
 * 2. Expand shortlinks if needed
 * 3. Normalize URL to canonical format
 * 4. Optionally append privacy message (settings)
 *
 * ## Call Site
 *
 * Injected at the entry of clipboard copy method (C98761aTc.LIZLLL):
 *
 * ```smali
 * invoke-static {p1, p2}, Lapp/revanced/.../ShareSanitizerHook;->sanitizeShareUrl(Ljava/lang/String;Landroid/content/Context;)Ljava/lang/String;
 * move-result-object p1
 * if-nez p1, :continue
 * return-void
 * :continue
 * # ... original clipboard write logic with sanitized p1 ...
 * ```
 *
 * ## Fail-Closed Design
 *
 * - Returns `null` on any error → clipboard write is aborted
 * - Shows user-facing toast with error message
 * - Never falls back to unsanitized URL
 *
 * ## Upstream Integration
 *
 * When porting to revanced-integrations, this class will be moved to:
 * ```
 * revanced-integrations/app/src/main/java/app/revanced/extension/tiktok/sharesanitizer/ShareSanitizerHook.kt
 * ```
 *
 * And will use ReVanced's toast utilities instead of direct Android Toast API.
 *
 * ## Testing Note
 *
 * This is a mock implementation for standalone testing. The Android `Context` parameter
 * cannot be used in JVM unit tests, so toasts are no-ops in test mode.
 */
object ShareSanitizerHook {
    /**
     * Sanitizes TikTok share URLs before clipboard write.
     *
     * This is the main entry point called from the patched bytecode.
     *
     * @param originalUrl The URL from TikTok's share logic (may be shortlink)
     * @param context Android context for toast messages (nullable for testing)
     * @return Sanitized canonical URL (potentially with message suffix), or null if sanitization fails
     */
    @JvmStatic
    fun sanitizeShareUrl(originalUrl: String?, context: Any?): String? {
        // Validate inputs
        if (originalUrl.isNullOrBlank()) {
            showToast(context, "Share failed: empty URL")
            return null
        }

        // Check if sanitizer is enabled
        if (!ShareSanitizerSettings.isEnabled()) {
            // Pass through original URL without sanitization
            return originalUrl
        }

        return try {
            // Step 1: Expand shortlinks if needed
            val expanded = if (isShortlink(originalUrl)) {
                expandShortlink(originalUrl, context) ?: return null
            } else {
                originalUrl
            }

            // Step 2: Normalize to canonical format
            val sanitized = normalizeUrl(expanded, context) ?: return null

            // Step 3: Optionally append privacy message
            if (ShareSanitizerSettings.shouldAppendMessage()) {
                sanitized + ShareSanitizerSettings.getPrivacyMessage()
            } else {
                sanitized
            }
        } catch (e: Exception) {
            showToast(context, "Share failed: ${e.message}")
            null
        }
    }

    /**
     * Expand a shortlink to its full destination URL.
     */
    private fun expandShortlink(url: String, context: Any?): String? {
        val expander = ShortlinkExpander.create(
            HttpClient.Config(
                maxRedirects = 5,
                timeoutSeconds = 3,
                maxRetries = 3
            )
        )

        return when (val result = expander.expand(url)) {
            is Result.Ok -> result.value
            is Result.Err -> {
                showToast(context, result.error.toToastMessage())
                null
            }
        }
    }

    /**
     * Normalize a URL to canonical TikTok format.
     */
    private fun normalizeUrl(url: String, context: Any?): String? {
        return when (val result = UrlNormalizer.normalize(url)) {
            is Result.Ok -> result.value
            is Result.Err -> {
                showToast(context, result.error.toToastMessage())
                null
            }
        }
    }

    /**
     * Check if a URL is a TikTok shortlink that needs expansion.
     */
    private fun isShortlink(url: String): Boolean {
        return url.contains("vm.tiktok.com") ||
               url.contains("vt.tiktok.com") ||
               url.contains("v.tiktok.com") ||
               url.contains("v16.tiktokv.com")
    }

    /**
     * Show a toast message to the user.
     *
     * In standalone mode: No-op (can't create toasts without Android runtime)
     * In upstream integration: Uses ReVanced toast utility
     *
     * @param context Android context (type Any? for testing compatibility)
     * @param message Message to display
     */
    private fun showToast(context: Any?, message: String) {
        // TODO: In upstream integration, replace with:
        // Utils.showToastShort(message)
        // or
        // Toast.makeText(context as? Context, message, Toast.LENGTH_SHORT).show()

        // For now, just log (no-op in standalone JVM tests)
        println("[ShareSanitizer] Toast: $message")
    }
}
