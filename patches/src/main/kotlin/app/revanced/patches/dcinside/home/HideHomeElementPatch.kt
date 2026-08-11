package app.revanced.patches.dcinside.home

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.findElementByAttributeValueOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/home/HideHomeElementPatch;"

private val hideHomeElementResourcePatch = resourcePatch {
    apply {
        document("res/values/dimens.xml").use { document ->
            document.documentElement.childNodes.findElementByAttributeValueOrThrow(
                "name",
                "divider"
            ).textContent = "0dp"
        }
    }
}

@Suppress("unused")
val hideHomeElementPatch = bytecodePatch(
    name = "Hide home elements",
    description = "Add options to hide home elements of main feed.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
        hideHomeElementResourcePatch,
    )

    apply {
        addResources("dcinside", "home.hideHomeElementPatch")

        PreferenceScreen.FEED.addPreferences(
            SwitchPreference("revanced_hide_dcbest"),
            SwitchPreference("revanced_hide_recommended_galleries"),
            SwitchPreference("revanced_hide_crowd"),
            SwitchPreference("revanced_hide_new_galleries"),
            SwitchPreference("revanced_hide_recent"),
        )

        mainBestFilterMethodMatch.let {
            it.method.apply {
                addInstructions(
                    it[-1],
                    $$"""
                        iget-object v0, p0, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                        invoke-static {v0}, $$EXTENSION_CLASS_DESCRIPTOR->hideDcbestView(Landroid/view/View;)V
                    """
                )
            }
        }

        liveBestItemMethodMatch.let {
            it.method.apply {
                addInstructions(
                    it[-1],
                    $$"""
                        iget-object v0, p0, Landroidx/recyclerview/widget/RecyclerView$ViewHolder;->itemView:Landroid/view/View;
                        invoke-static { v0 }, $$EXTENSION_CLASS_DESCRIPTOR->hideDcbestView(Landroid/view/View;)V
                    """
                )
            }
        }

        mainLiveBestMoreMethodMatch.let {
            it.method.apply {
                val viewIndex = it[-1]
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstructions(
                    viewIndex + 1,
                    $$"""
                        invoke-static/range { v$$viewRegister .. v$$viewRegister }, $$EXTENSION_CLASS_DESCRIPTOR->hideDcbestView(Landroid/view/View;)V
                    """
                )
            }
        }

        mainRecommendGalleriesMethodMatch.let {
            it.method.apply {
                val viewIndex = it[-1]
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstructions(
                    viewIndex + 1,
                    $$"""
                        invoke-static/range { v$$viewRegister .. v$$viewRegister }, $$EXTENSION_CLASS_DESCRIPTOR->hideRecommendedGalleriesView(Landroid/view/View;)V
                    """
                )
            }
        }

        mainCrowdMethodMatch.let {
            it.method.apply {
                addInstructions(
                    it[-1],
                    $$"""
                        invoke-static { p0 }, $$EXTENSION_CLASS_DESCRIPTOR->hideCrowdView(Landroid/view/View;)V
                    """
                )
            }
        }

        mainSetNewGalleriesMethod.addInstructions(
            0,
            $$"""
                invoke-static {p1}, $$EXTENSION_CLASS_DESCRIPTOR->hideNewGalleries(Ljava/util/List;)Ljava/util/List;
                move-result-object p1
            """
        )

        mainRecentMethodMatch.let {
            it.method.apply {
                addInstructions(
                    it[-1],
                    $$"""
                        invoke-static/range { p0 .. p0 }, $$EXTENSION_CLASS_DESCRIPTOR->hideRecentView(Landroid/view/View;)V
                    """
                )
            }
        }
    }
}