package app.revanced.extension.dcinside.patches.ads;

import app.revanced.extension.dcinside.settings.Settings;

public class HideAdsPatch {
    public static boolean shouldHideAds() {
        return Settings.HIDE_ADS.get();
    }
}
