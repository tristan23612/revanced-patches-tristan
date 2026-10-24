package app.revanced.extension.dcinside.patches.misc;

import app.revanced.extension.dcinside.settings.Settings;

public class EnableOneStoreFeaturesPatch {
    public static boolean oneStoreFeaturesEnabled() {
        return Settings.ENABLE_ONESTORE_FEATURES.get();
    }
}
