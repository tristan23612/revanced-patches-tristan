package app.revanced.patches.dcinside.misc.hook

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.hook.json.addJsonHook
import app.revanced.patches.dcinside.misc.hook.json.jsonHook
import app.revanced.patches.dcinside.misc.hook.json.jsonHookPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference

fun hookPatch(
    name: String,
    hookClassDescriptor: String,
    preferenceKey: String,
) = bytecodePatch(name) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        jsonHookPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "misc.hook.hookPatch")

        PreferenceScreen.MISC.addPreferences(
            SwitchPreference(preferenceKey),
        )

        addJsonHook(jsonHook(hookClassDescriptor))
    }
}