package app.revanced.patches.dcinside.post.view

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.bottomLikePostsMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    instructions(
        "vwTitle"(),
        field { type == "Lcom/lsjwzh/widget/recyclerviewpager/LoopRecyclerViewPager;" },
        "vwRecycler"(),
    )
}