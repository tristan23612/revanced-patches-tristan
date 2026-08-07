package app.revanced.patches.dcinside.misc.update

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.returnEarly

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/misc/DisableUpdateCheckPatch;"

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable update check",
    description = "Add option to disable update check",
    use = false,
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "misc.update.disableUpdateCheckPatch")

        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("revanced_disable_update_check"),
        )

        updateCheckMethod.apply {
            addInstructions(
                0,
                $$"""
                invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->shouldDisableUpdateCheck()Z
                move-result v0
                if-eqz v0, :check_update
                
                const/4 v0, 0x0
                return v0
                :check_update                
                """
            )
        }
    }
}