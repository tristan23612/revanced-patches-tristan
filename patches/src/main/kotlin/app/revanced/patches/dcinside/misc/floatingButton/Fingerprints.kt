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

internal val BytecodePatchContext.postHeaderSetupMethodMatch by composingFirstMethod {
    parameterTypes("Lcom/dcinside/app/model/PostInfo;", "Z", "Ljava/lang/String;")
    returnType("V")
    instructions(
        "info"(),
        "readHeaderSubject"(),
        "readHeaderMemberIc"(),
        "readHeaderUserMemo"(),
    )
}