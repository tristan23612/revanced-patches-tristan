package app.revanced.patches.dcinside.misc.onestore

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/misc/EnableOneStoreFeaturesPatch;"

@Suppress("unused")
val EnableOneStoreFeaturesPatch = bytecodePatch(
    name = "Enable OneStore features",
    description = "Enable OneStore features.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "misc.onestore.enableOneStoreFeaturesPatch")

        PreferenceScreen.MISC.addPreferences(
            SwitchPreference("revanced_enable_onestore_features")
        )

        storeConfigurationMethodMatch.classDef.methods.filter {
            it.parameters.isEmpty() && it.returnType == "Z"
        }.forEach { method ->
            method.apply {
                addInstructions(
                    0,
                    $$"""
                        invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->oneStoreFeaturesEnabled()Z
                        move-result v0
                        return v0
                    """
                )
            }
        }
    }
}