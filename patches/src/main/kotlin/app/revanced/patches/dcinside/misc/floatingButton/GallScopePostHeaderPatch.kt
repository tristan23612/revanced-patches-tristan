package app.revanced.patches.dcinside.misc.floatingButton

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.post.userId.showUserIdPatch
import app.revanced.util.findFreeRegister

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/misc/floatingButton/gallScope/GallScopePostHeaderPatch;"

internal val gallScopePostHeaderPatch = bytecodePatch {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        showUserIdPatch,
    )

    apply {
        postHeaderSetupMethodMatch.let {
            it.method.apply {
                addInstructions(
                    0,
                    """
                        move-object/from16 v0, p0
                        
                        invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->setGallScopeClickListener(Landroid/view/View;)V
                    """
                )
            }
        }
    }
}