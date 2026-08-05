package app.revanced.patches.dcinside.post.write.pum

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/post/write/pum/DisablePostPumOptionPatch;"

@Suppress("unused")
val disablePostPumOptionPatch = bytecodePatch(
    name = "Disable post Pum option",
    description = "Disables the Pum option by default when opening the post write screen.",
) {
    compatibleWith(
        "com.dcinside.app.android"(
            "5.3.2"
        )
    )

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "post.write.pum.disablePostPumOptionPatch")

        PreferenceScreen.GENERAL.addPreferences(
            SwitchPreference("revanced_disable_post_pum_option"),
        )

        var postWriteActivityDefiningClass = ""
        var notAllowedPumEnableFieldReference = ""
        postWriteActivityHelperMethodMatch.let {
            it.method.apply {
                postWriteActivityDefiningClass = definingClass

                val notAllowedPumEnableFieldIndex = it[2]
                notAllowedPumEnableFieldReference = getInstruction<TwoRegisterInstruction>(notAllowedPumEnableFieldIndex).fieldReference!!.toString()
            }
        }

        postWriteActivityInitMethodMatch(postWriteActivityDefiningClass).method.addInstructions(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->shouldDisablePostPumOption()Z
                move-result v0
                iput-boolean v0, p0, $notAllowedPumEnableFieldReference
            """
        )
    }
}