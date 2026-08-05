package app.revanced.extension.dcinside.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;

/**
 * Settings specific to the DCInside app.
 * <p>
 * Extends {@link BaseSettings} so that shared settings (language, debug,
 * menu icons, search history, etc.) are loaded together with this class.
 */
public class Settings extends BaseSettings {
    public static final BooleanSetting HIDE_ADS = new BooleanSetting("revanced_hide_ads", TRUE, TRUE);
    public static final BooleanSetting DISABLE_POST_PUM_OPTION = new BooleanSetting("revanced_disable_post_pum_option", TRUE, FALSE);
}