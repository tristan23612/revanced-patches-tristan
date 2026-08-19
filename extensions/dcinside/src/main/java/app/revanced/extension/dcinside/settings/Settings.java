package app.revanced.extension.dcinside.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.StringSetting;

/**
 * Settings specific to the DCInside app.
 * <p>
 * Extends {@link BaseSettings} so that shared settings (language, debug,
 * menu icons, search history, etc.) are loaded together with this class.
 */
public class Settings extends BaseSettings {
    // ADS
    public static final BooleanSetting HIDE_ADS = new BooleanSetting("revanced_hide_ads", TRUE, TRUE);
    // FEED
    public static final BooleanSetting HIDE_DCBEST = new BooleanSetting("revanced_hide_dcbest", TRUE, TRUE);
    public static final BooleanSetting HIDE_RECOMMENDED_GALLERIES = new BooleanSetting("revanced_hide_recommended_galleries", TRUE, TRUE);
    public static final BooleanSetting HIDE_CROWD = new BooleanSetting("revanced_hide_crowd", TRUE, TRUE);
    public static final BooleanSetting HIDE_NEW_GALLERIES = new BooleanSetting("revanced_hide_new_galleries", TRUE, TRUE);
    public static final BooleanSetting HIDE_RECENT = new BooleanSetting("revanced_hide_recent", TRUE, TRUE);
    // GENERAL
    public static final BooleanSetting DISABLE_POST_PUM_OPTION = new BooleanSetting("revanced_disable_post_pum_option", TRUE, FALSE);
    public static final BooleanSetting SHOW_USER_ID = new BooleanSetting("revanced_show_user_id", TRUE, FALSE);
    // MISC
    public static final BooleanSetting ENABLE_QUICK_POST_MANAGEMENT = new BooleanSetting("revanced_enable_quick_post_management", FALSE, FALSE);
    public static final BooleanSetting HIDE_ADMINISTRATOR_NOTICE = new BooleanSetting("revanced_hide_administrator_notice", TRUE, FALSE);
    public static final BooleanSetting HIDE_MUST_READ_NOTICE = new BooleanSetting("revanced_hide_must_read_notice", TRUE, FALSE);
    public static final BooleanSetting DISABLE_UPDATE_CHECK = new BooleanSetting("revanced_disable_update_check", FALSE, FALSE);
    public static final BooleanSetting HIDE_BOTTOM_LIKE_POSTS = new BooleanSetting("revanced_hide_bottom_like_posts", TRUE, FALSE);
    public static final BooleanSetting SHOW_DC_BAN_LIST_BUTTON = new BooleanSetting("revanced_show_dc_ban_list_button", FALSE, TRUE);
    public static final StringSetting DC_BAN_LIST_SHEET_ID_MAP = new StringSetting("revanced_dc_ban_list_sheet_id_map", "");
}