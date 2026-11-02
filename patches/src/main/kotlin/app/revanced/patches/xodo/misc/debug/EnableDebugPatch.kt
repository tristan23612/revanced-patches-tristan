package app.revanced.patches.xodo.misc.debug

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val enableDebugPatch = bytecodePatch(
    name = "Enable debug",
    description = "Enables debug features.",
    use = false,
) {
    compatibleWith("com.xodo.pdf.reader")

    apply {
        setDebugMethodMatch.method.addInstructions(
            0,
            """
                const/4 p1, 0x1
            """
        )
    }
}