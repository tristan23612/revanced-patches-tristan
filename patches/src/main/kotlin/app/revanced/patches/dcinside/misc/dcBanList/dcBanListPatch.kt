package app.revanced.patches.dcinside.misc.dcBanList

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.NonInteractivePreference

@Suppress("unused")
val dcBanListPatch = bytecodePatch(
    name = "DC Ban List",
    description = "Adds DC Ban List to the app."
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "misc.dcBanList.dcBanListPatch")

        PreferenceScreen.MISC.addPreferences(
            NonInteractivePreference(
                key = "revanced_dc_ban_list",
                tag = "app.revanced.extension.dcinside.settings.preference.GoogleLoginWebViewPreference",
                selectable = true,
            )
        )
    }
}