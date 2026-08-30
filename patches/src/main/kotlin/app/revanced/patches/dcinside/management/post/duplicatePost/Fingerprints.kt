package app.revanced.patches.dcinside.management.post.duplicatePost

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

internal val BytecodePatchContext.postHeaderSetupMethodMatch by composingFirstMethod {
    parameterTypes("Lcom/dcinside/app/model/PostInfo;", "Z", "Ljava/lang/String;")
    returnType("V")
    strings(
        "info",
        "readHeaderSubject",
        "readHeaderMemberIc",
        "readHeaderUserMemo",
    )
}