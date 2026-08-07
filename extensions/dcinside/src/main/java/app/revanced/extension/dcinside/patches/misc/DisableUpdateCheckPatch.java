package app.revanced.extension.dcinside.patches.misc;

import app.revanced.extension.dcinside.settings.Settings;

public class DisableUpdateCheckPatch {
    public static boolean shouldDisableUpdateCheck() {
        return Settings.DISABLE_UPDATE_CHECK.get();
    }
}
