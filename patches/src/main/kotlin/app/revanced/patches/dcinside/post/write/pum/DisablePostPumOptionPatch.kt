package app.revanced.patches.dcinside.post.write.pum

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/post/write/pum/DisablePostPumOptionPatch;"

@Suppress("unused")
val disablePostPumOptionPatch = bytecodePatch(
    name = "Disable post Pum option",
    description = "Add option to disables the Pum option by default when opening the post write screen.",
) {
    compatibleWith("com.dcinside.app.android")

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

        writeConfigNotAllowedPumSharedPreferenceMethodMatch.let {
            it.method.apply {
                val defaultBooleanIndex = it[0]
                val defaultBooleanRegister = getInstruction<OneRegisterInstruction>(defaultBooleanIndex).registerA

                addInstructions(
                    defaultBooleanIndex + 1,
                    $$"""
                        invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->shouldDisablePostPumOption()Z
                        move-result v$${defaultBooleanRegister}
                    """
                )
            }
        }
    }
}