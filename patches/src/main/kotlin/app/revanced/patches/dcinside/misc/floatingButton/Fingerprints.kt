package app.revanced.patches.dcinside.misc.floatingButton

import app.revanced.patcher.*
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.floatingButtonVisibilityMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Z")
    returnType("V")
    instructions(
        "postListQuickWrite"(),
    )
}