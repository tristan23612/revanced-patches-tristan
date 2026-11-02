package app.revanced.patches.xodo.misc.debug

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.setDebugMethodMatch by composingFirstMethod {
    definingClass("Lcom/pdftron/pdf/utils/F;")
    name("setDebug")
    parameterTypes("Z")
    returnType("V")
}