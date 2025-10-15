package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.sharesanitizer.fingerprints.clipboardCopyFingerprint

/**
 * Share Link Sanitizer Patch for TikTok
 *
 * Removes tracking parameters and normalizes TikTok share URLs before they reach the clipboard.
 *
 * ## What It Does
 * - Intercepts the clipboard copy operation when user taps "Copy Link"
 * - Expands shortlinks (vm.tiktok.com, vt.tiktok.com) to full URLs
 * - Normalizes to canonical format: https://www.tiktok.com/@USER/video/ID
 * - Strips tracking parameters (u_code, etc.) and query strings
 *
 * ## Hook Point
 * Injects at the entry of `C98761aTc.LIZLLL()` method which writes URLs to clipboard.
 * The first parameter (p1) contains the URL string - we transform it before clipboard write.
 *
 * ## Architecture
 * - **Patch (this file):** Bytecode injection using ReVanced Patcher
 * - **Fingerprint:** Locates the clipboard method via parameter signature
 * - **Extension (revanced-integrations):** Pure Kotlin sanitization logic
 *   - UrlNormalizer: URL parsing and canonical format construction
 *   - ShortlinkExpander: HTTP redirect following with timeout
 *   - ShareSanitizerHook: Orchestrates the sanitization pipeline
 *
 * ## Extension Integration
 * This patch expects a companion extension in the revanced-integrations repository:
 * ```
 * app/revanced/extension/tiktok/sharesanitizer/ShareSanitizerHook.kt
 * ```
 *
 * The extension must provide:
 * ```kotlin
 * object ShareSanitizerHook {
 *     @JvmStatic
 *     fun sanitizeShareUrl(originalUrl: String, context: Context): String
 * }
 * ```
 *
 * ## Compatibility
 * - TikTok 36.5.4 (com.zhiliaoapp.musically) ✅
 * - TikTok 36.5.4 (com.ss.android.ugc.trill) - Expected compatible
 *
 * ## Failure Handling
 * - Network timeout: Returns original URL (fail-safe)
 * - Invalid format: Returns expanded URL if expansion succeeded
 * - Extension shows toast notification on errors
 *
 * @see clipboardCopyFingerprint
 * @see UrlNormalizer
 * @see ShortlinkExpander
 */
@Suppress("unused")
val shareSanitizerPatch = bytecodePatch(
    name = "Share link sanitizer",
    description = "Removes tracking parameters from TikTok share links and normalizes URLs."
) {
    compatibleWith(
        "com.zhiliaoapp.musically"("36.5.4"),  // US variant
        "com.ss.android.ugc.trill"("36.5.4")   // Global variant
    )

    execute {
        // Find the clipboard copy method using fingerprint
        clipboardCopyFingerprint.match(classes).let { result ->
            result.method.addInstructions(
                0,  // Inject at method entry, before any processing
                """
                    # Original parameters at method entry:
                    # p0 = this (C98761aTc instance)
                    # p1 = content (String - the URL to sanitize)
                    # p2 = context (Context)
                    # p3 = cert (Cert - BPEA permission system)
                    # p4 = view (View)

                    # Call our sanitizer extension
                    # Takes: (String url, Context context) -> String sanitizedUrl
                    invoke-static {p1, p2}, Lapp/revanced/extension/tiktok/sharesanitizer/ShareSanitizerHook;->sanitizeShareUrl(Ljava/lang/String;Landroid/content/Context;)Ljava/lang/String;
                    move-result-object p1

                    # p1 now contains the sanitized URL
                    # The original method continues with the sanitized URL
                    # ClipData.newPlainText(p1, p1) will use our clean URL
                """
            )
        }
    }
}
