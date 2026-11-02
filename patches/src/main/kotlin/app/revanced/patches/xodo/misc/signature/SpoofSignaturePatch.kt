package app.revanced.patches.xodo.misc.signature

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.returnEarly

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature",
    description = "Spoofs the signature of the app."
) {
    compatibleWith("com.xodo.pdf.reader")

    apply {
        spoofSignatureMethodMatch.method.returnEarly()
    }
}