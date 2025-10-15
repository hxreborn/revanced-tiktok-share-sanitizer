package app.revanced.patches.tiktok.misc.sharesanitizer

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
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
 * - Optionally appends privacy message (configurable via settings)
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
 *   - ShareSanitizerSettings: Settings access layer
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
 *     fun sanitizeShareUrl(originalUrl: String, context: Context): String?
 * }
 * ```
 *
 * ## Settings
 * Two user-configurable settings are available (see Settings.kt):
 *
 * 1. **revanced_tiktok_share_sanitizer_enabled** (default: true)
 *    - Master toggle for the sanitizer
 *    - When disabled, URLs pass through unchanged
 *
 * 2. **revanced_tiktok_share_sanitizer_append_message** (default: false)
 *    - Appends "\n\nSanitized: tracking removed" to shared links
 *    - Only applies when sanitizer is enabled
 *
 * Settings integration requires:
 * - Adding preferences to TikTok settings UI (via SettingsPatch dependency)
 * - SharedPreferences access in ShareSanitizerSettings
 *
 * ## Compatibility
 * - TikTok 36.5.4 (com.zhiliaoapp.musically) ✅
 * - TikTok 36.5.4 (com.ss.android.ugc.trill) - Expected compatible
 *
 * ## Failure Handling
 * - Sanitizer returns `null` on error → clipboard write is aborted (fail-closed)
 * - Extension is responsible for showing a user-facing toast with the failure reason
 *
 * @see clipboardCopyFingerprint
 * @see UrlNormalizer
 * @see ShortlinkExpander
 * @see ShareSanitizerHook
 * @see Settings
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
        // The fingerprint resolves to C98761aTc.LIZLLL(String, Context, Cert, View)
        val match = clipboardCopyFingerprint.match(classes)
        val method = match.method

        // Prepare branch target: the original first instruction of the method
        val firstInstruction = method.instructions.first()

        method.addInstructionsWithLabels(
            0,
            """
                invoke-static {p1, p2}, Lapp/revanced/extension/tiktok/sharesanitizer/ShareSanitizerHook;->sanitizeShareUrl(Ljava/lang/String;Landroid/content/Context;)Ljava/lang/String;
                move-result-object p1
                if-nez p1, :share_sanitizer_continue
                return-void
            """.trimIndent(),
            ExternalLabel("share_sanitizer_continue", firstInstruction),
        )
    }
}
