package app.revanced.patches.dcinside.home

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal fun BytecodePatchContext.getMainSpaceDimensionMethodMatch(dimensionName: String) = firstMethodComposite {
    instructions(
        ResourceType.DIMEN(dimensionName),
        allOf(
            Opcode.INVOKE_STATIC(),
            method { parameterTypes.size == 1 && parameterTypes[0] == "I" && returnType == "I" },
        ),
        after(
            Opcode.MOVE_RESULT(),
        ),
    )
}

internal val BytecodePatchContext.bestPickItemMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_best_pick_item"),
        allOf(
            Opcode.IGET_OBJECT(),
            field { definingClass == $$"Landroidx/recyclerview/widget/RecyclerView$ViewHolder;" && type == "Landroid/view/View;" }
        ),
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.mainBestPickTitleMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_main_best_pick_title"),
        after(
            method { name == "inflate" }
        ),
        after(
            Opcode.MOVE_RESULT_OBJECT(),
        ),
    )
}

internal val BytecodePatchContext.mainBestFilterMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_main_best_filter"),
        allOf(
            Opcode.IGET_OBJECT(),
            field { definingClass == $$"Landroidx/recyclerview/widget/RecyclerView$ViewHolder;" && type == "Landroid/view/View;" }
        ),
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.liveBestItemMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_live_best_item"),
        allOf(
            Opcode.IGET_OBJECT(),
            field { definingClass == $$"Landroidx/recyclerview/widget/RecyclerView$ViewHolder;" && type == "Landroid/view/View;" }
        ),
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.mainLiveBestMoreMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_main_live_best_more"),
        after(
            method { name == "inflate" }
        ),
        after(
            Opcode.MOVE_RESULT_OBJECT(),
        ),
    )
}

internal val BytecodePatchContext.mainRecommendGalleriesMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_recommend_galleries"),
        after(
            method { name == "inflate" }
        ),
        after(
            Opcode.MOVE_RESULT_OBJECT(),
        ),
    )
}

internal val BytecodePatchContext.mainCrowdMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_crowd"),
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.mainSetNewGalleriesMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC)
    name("setNewGalleries")
    parameterTypes("Ljava/util/List;")
}

internal val BytecodePatchContext.mainRecentMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_recent_basic"),
        Opcode.RETURN_VOID(),
    )
}