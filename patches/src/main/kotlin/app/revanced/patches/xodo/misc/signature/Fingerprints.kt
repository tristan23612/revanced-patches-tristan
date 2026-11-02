package app.revanced.patches.xodo.misc.signature

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.spoofSignatureMethodMatch by composingFirstMethod {
    strings(
        "completeReaderMainActivity"
    )
}