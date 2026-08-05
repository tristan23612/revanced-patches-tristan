package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.settingsPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.returnEarly

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/dcinside/patches/ads/HideAdsPatch;"

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hide ads across the app.",
) {
    compatibleWith("com.dcinside.app.android")

    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    apply {
        addResources("dcinside", "ads.hideAdsPatch")

        PreferenceScreen.ADS.addPreferences(
            SwitchPreference("revanced_hide_ads"),
        )

        shouldLoadAdMethod.addInstructions(
            0,
            $$"""
                invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->shouldHideAds()Z
                move-result v0
                if-eqz v0, :show_ads
                
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                return-object v0
                :show_ads
            """
        )

        shouldShowFooterAdMethod.addInstructions(
            0,
            $$"""
                invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->shouldHideAds()Z
                move-result v0
                if-eqz v0, :show_ads
                
                return-void
                :show_ads
            """
        )
        setMinimumHeightMethod.returnEarly(0)

        setMinimumHeightMethod.addInstructions(
            0,
            """
                invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->shouldHideAds()Z
                move-result v0
                if-eqz v0, :show_ads
                
                const/4 v0, 0x0
                return v0
                :show_ads
            """
        )
    }
}
