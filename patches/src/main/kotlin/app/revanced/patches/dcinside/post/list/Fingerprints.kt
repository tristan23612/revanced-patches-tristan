package app.revanced.patches.dcinside.post.list

import app.revanced.patcher.*
import app.revanced.patcher.invoke
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.postItemBindMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    instructions(
        method { returnType == "Lcom/dcinside/app/response/PostItem;" },
        after(
            Opcode.MOVE_RESULT_OBJECT(),
        ),
        "null cannot be cast to non-null type com.dcinside.app.post.fragments.PostListItemHolder"(),
        method {
            parameterTypes.isEmpty() && returnType == "I" && definingClass == "Lcom/dcinside/app/response/PostItem;"
        },
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.addQueryParameterHookMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    definingClass($$"Lokhttp3/HttpUrl$Builder;")
    name("addQueryParameter")
}

internal val BytecodePatchContext.jsonApiPostListHookMethod by gettingFirstMethodDeclaratively {
    strings("api_postList")
}