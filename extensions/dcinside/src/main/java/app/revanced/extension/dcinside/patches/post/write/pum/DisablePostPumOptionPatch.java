package app.revanced.extension.dcinside.patches.post.write.pum;

import android.util.Log;
import app.revanced.extension.dcinside.settings.Settings;

public class DisablePostPumOptionPatch {
    public static boolean shouldDisablePostPumOption() {
        return Settings.DISABLE_POST_PUM_OPTION.get();
    }
}
