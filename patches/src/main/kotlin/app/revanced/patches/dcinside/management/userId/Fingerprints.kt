package app.revanced.patches.dcinside.management.userId

import app.revanced.patcher.*
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal fun BytecodePatchContext.postSearchItemOnBindViewHolderMethodMatch(targetMethodMatch: CompositeMatch) = firstMethodComposite {
    definingClass("Lcom/dcinside/app/post/fragments/")
    parameterTypes($$"Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "I")
    accessFlags(AccessFlags.PUBLIC)
    name("onBindViewHolder")
    instructions(
        "viewHolder"(),
        method {
            targetMethodMatch.method.definingClass == definingClass && targetMethodMatch.method.name == name
        },
        after(
            Opcode.RETURN_VOID(),
        )
    )
}

internal val BytecodePatchContext.normalPostItemBindMethodMatch by composingFirstMethod {
    returnType("V")
    strings(
        "null cannot be cast to non-null type com.dcinside.app.post.fragments.PostListItemHolder",
        "dcbest",
    )
}

internal val BytecodePatchContext.postSearchItemBindMethodMatch by composingFirstMethod {
    returnType("V")
    instructions(
        method { returnType == POST_ITEM_CLASS_DESCRIPTOR },
        after(
            Opcode.MOVE_RESULT_OBJECT(),
        ),
        ""(),
        "key"(),
        "dcbest"(),
        Opcode.RETURN_VOID(),
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
        method { name == "setVisibility" },
        method { name == "setVisibility" },
        Opcode.IF_EQZ(),
    )
}

internal val BytecodePatchContext.postReplySetupMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE)
    returnType("V")
    instructions(
        Opcode.MOVE_OBJECT_FROM16(),
        Opcode.MOVE_OBJECT_FROM16(),
        Opcode.MOVE_OBJECT_FROM16(), // PostReplyItem
        Opcode.INVOKE_VIRTUAL(),
        Opcode.MOVE_RESULT_OBJECT(),
        method { name == "setVisibility" },
        method { name == "setVisibility" },
        ".*"(),
        "owner"(),
        method { name == "setVisibility" },
        method { name == "setVisibility" },
        method { returnType == "Landroid/widget/ImageView;" && parameterTypes.isEmpty() },
        Opcode.MOVE_RESULT_OBJECT(),
        method { name == "setVisibility" },
    )
}

internal val BytecodePatchContext.postHistoryRealmSetupMethodMatch by composingFirstMethod {
    parameterTypes("L", "Lcom/dcinside/app/model/PostInfo;")
    returnType("V")
    strings(
        "this.where(T::class.java)",
        "key",
        "this.createObject(T::class.java, primaryKeyValue)",
    )
}

internal val BytecodePatchContext.postHistoryRealmReSetupMethodMatch by composingFirstMethod {
    strings(
        "call to 'resume' before 'invoke' with coroutine",
        "this.where(T::class.java)",
        "key",
        "this.createObject(T::class.java, primaryKeyValue)",
    )
}

context(_: BytecodePatchContext)
internal fun ClassDef.getStringGetterMethod(fieldName: String, compositeMatch: CompositeMatch? = null) = firstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Ljava/lang/String;")
    parameterTypes()
    instructions(
        allOf(
            Opcode.IGET_OBJECT(),
            field { name == fieldName }
        ),
        after(
            Opcode.RETURN_OBJECT()
        )
    )
    if (compositeMatch != null) {
        custom {
            compositeMatch.method.indexOfFirstInstruction {
                methodReference?.let {
                    it.definingClass == definingClass && it.name == name
                } == true
            } != -1
        }
    }
}