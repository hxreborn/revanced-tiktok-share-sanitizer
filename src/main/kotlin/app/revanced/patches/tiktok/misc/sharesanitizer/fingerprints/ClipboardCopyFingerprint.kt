package app.revanced.patches.tiktok.misc.sharesanitizer.fingerprints

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for the TikTok clipboard copy method that writes share URLs to the clipboard.
 *
 * **Target Method:** `C98761aTc.LIZLLL(String, Context, Cert, View)`
 * **Location:** `p003X/C98761aTc.java:190`
 * **Behavior:** Copies video share link to clipboard when user taps "Copy Link"
 *
 * **Call Chain:**
 * ```
 * User taps "Copy Link" button
 *   → CopyLinkChannel.LJI()
 *   → C98761aTc.LIZLLL(url, context, cert, view) ← THIS METHOD
 *   → ClipData.newPlainText(url, url)
 *   → clipboard.setPrimaryClip()
 * ```
 *
 * **Why This Works:**
 * - Parameter signature is unique: 4 params with specific types including BPEA Cert
 * - Method performs clipboard operations (ClipboardManager + ClipData)
 * - First parameter is the URL string we need to sanitize
 *
 * **Version Compatibility:**
 * - Tested on TikTok 36.5.4 (com.zhiliaoapp.musically)
 * - Should work on 36.5.4 (com.ss.android.ugc.trill)
 *
 * @see app.revanced.patches.tiktok.misc.sharesanitizer.ShareSanitizerPatch
 */
internal val clipboardCopyFingerprint = fingerprint {
    returns("V")  // void method

    // Unique parameter signature - the key to finding this method
    parameters(
        "Ljava/lang/String;",                    // p1: content (the URL to sanitize!)
        "Landroid/content/Context;",             // p2: context
        "Lcom/bytedance/bpea/basics/Cert;",      // p3: cert (TikTok's BPEA permission system)
        "Landroid/view/View;"                    // p4: view
    )

    // Match on method behavior - these operations happen in sequence
    strings(
        "clipboard",  // Getting clipboard service
        "content"     // Parameter validation
    )
}
