package app.revanced.patches.tiktok.misc.sharesanitizer.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Fingerprint for `C98761aTc.LIZLLL(String, Context, Cert, View)`, the method responsible for copying
 * the TikTok share URL to the clipboard when the user taps **Copy link**.
 *
 * Matching strategy:
 * - **Signature:** public final void with the distinctive four-parameter list (String, Context, Cert, View)
 * - **Behavioral opcodes:** lookup of the clipboard service followed by `ClipData.newPlainText`
 * - **String literals:** `"clipboard"` and `"content"` appear together in the method
 * - **Sanity check:** ensure the class and method names match the obfuscated identifiers we documented
 *   and that the method casts the clipboard service to `ClipboardManager`
 */
internal val clipboardCopyFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters(
        "Ljava/lang/String;",
        "Landroid/content/Context;",
        "Lcom/bytedance/bpea/basics/Cert;",
        "Landroid/view/View;"
    )

    opcodes(
        Opcode.INVOKE_STATIC,     // Intrinsics.checkNotNullParameter(content, "content")
        Opcode.INVOKE_STATIC,     // Context.getSystemService("clipboard")
        Opcode.CHECK_CAST,        // Cast to ClipboardManager
        Opcode.INVOKE_STATIC,     // ClipData.newPlainText
        Opcode.MOVE_RESULT_OBJECT // Store ClipData
    )

    strings("clipboard", "content")

    custom { method, classDef ->
        classDef.type.contains("aT") &&
            method.name == "LIZLLL" &&
            method.implementation?.instructions?.any { instruction ->
                instruction.opcode == Opcode.CHECK_CAST &&
                    (instruction as? ReferenceInstruction)?.reference
                        ?.toString()
                        ?.contains("android/content/ClipboardManager") == true
            } == true
    }
}
