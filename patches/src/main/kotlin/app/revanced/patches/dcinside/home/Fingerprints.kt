package app.revanced.patches.dcinside.home

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.shared.misc.mapping.ResourceType
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.mainBestFilterMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_main_best_filter"),
        Opcode.RETURN_VOID(),
    )
}

internal val BytecodePatchContext.liveBestItemMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_live_best_item"),
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
        )
    )
}

internal val BytecodePatchContext.mainBottomMethodMatch by composingFirstMethod {
    instructions(
        ResourceType.LAYOUT("view_main_bottom"),
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
        )
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