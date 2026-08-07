package app.revanced.patches.dcinside.post.list

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.hook.json.jsonHookPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.doRecursively
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val QUICK_POST_MANAGEMENT_PATCH_EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/post/list/QuickPostManagementPatch;"

@Suppress("unused")
val quickPostManagementPatch = bytecodePatch(
    name = "Quick post management",
    description = "Adds option to manage posts using manager permission.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        jsonHookPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "post.list.quickPostManagementPatch")

        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("revanced_enable_quick_post_management")
        )

        jsonApiPostListHookMethod.apply {
            addInstructions(
                0,
                $$"""
                    invoke-static/range {p0 .. p0}, $$QUICK_POST_MANAGEMENT_PATCH_EXTENSION_CLASS_DESCRIPTOR->hookGalleryID(Ljava/lang/String;)V
                """
            )
        }

        addQueryParameterHookMethod.addInstructions(
            0,
            $$"""
                move-object/from16 v0, p1
                move-object/from16 v1, p2
                invoke-static {v0, v1}, $$QUICK_POST_MANAGEMENT_PATCH_EXTENSION_CLASS_DESCRIPTOR->hookParam(Ljava/lang/String;Ljava/lang/String;)V
            """
        )

        postItemBindMethodMatch.let {
            it.method.apply {
                val postItemIndex = it[1]
                val postItemRegister = getInstruction<OneRegisterInstruction>(postItemIndex).registerA

                val postNoMethodReference = getInstruction(it[3]).methodReference

                val insertIndex = it[-1]
                var insertSmali = $$"""
                    move-object/from16 v0, p1
                    iget-object v0, v0, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                    
                    invoke-virtual { v$$postItemRegister }, $$postNoMethodReference
                    move-result v1
                    
                    invoke-static {v0, v1}, $$QUICK_POST_MANAGEMENT_PATCH_EXTENSION_CLASS_DESCRIPTOR->setLongClickListener(Landroid/view/View;I)V
                """

                addInstructions(insertIndex, insertSmali)
            }
        }
    }
}