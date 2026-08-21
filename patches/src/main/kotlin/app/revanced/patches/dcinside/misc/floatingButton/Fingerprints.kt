package app.revanced.patches.dcinside.misc.floatingButton

import app.revanced.patcher.*
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.floatingButtonVisibilityMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Z")
    returnType("V")
    instructions(
        "postListQuickWrite"(),
    )
}

internal val BytecodePatchContext.postListOnViewCreatedMethodMatch by composingFirstMethod("postListQuickWrite") {
    name("onViewCreated")
    accessFlags(AccessFlags.PUBLIC)
    parameterTypes("Landroid/view/View;", "Landroid/os/Bundle;")
    returnType("V")
    instructions(
        method { name == "onViewCreated" }
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