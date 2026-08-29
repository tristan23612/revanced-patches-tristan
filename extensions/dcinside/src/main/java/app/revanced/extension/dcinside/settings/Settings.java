package app.revanced.extension.dcinside.settings;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

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
    public static final BooleanSetting HIDE_DCBEST = new BooleanSetting("revanced_hide_dcbest", FALSE, TRUE);
    public static final BooleanSetting HIDE_RECOMMENDED_GALLERIES = new BooleanSetting("revanced_hide_recommended_galleries", FALSE, TRUE);
    public static final BooleanSetting HIDE_CROWD = new BooleanSetting("revanced_hide_crowd", FALSE, TRUE);
    public static final BooleanSetting HIDE_NEW_GALLERIES = new BooleanSetting("revanced_hide_new_galleries", FALSE, TRUE);
    public static final BooleanSetting HIDE_RECENT = new BooleanSetting("revanced_hide_recent", FALSE, TRUE);

    // GENERAL
    public static final BooleanSetting DISABLE_POST_PUM_OPTION = new BooleanSetting("revanced_disable_post_pum_option", TRUE, FALSE);
    public static final BooleanSetting HIDE_BOTTOM_LIKE_POSTS = new BooleanSetting("revanced_hide_bottom_like_posts", FALSE, FALSE);

    // MANAGEMENT
    public static final BooleanSetting SHOW_USER_ID = new BooleanSetting("revanced_show_user_id", TRUE, FALSE);
    public static final BooleanSetting ENABLE_USER_ID_GALL_SCOPE = new BooleanSetting("revanced_enable_user_id_gall_scope", FALSE, Setting.parent(SHOW_USER_ID));
    public static final BooleanSetting SHOW_DC_BAN_LIST_BUTTON = new BooleanSetting("revanced_show_dc_ban_list_button", FALSE, FALSE);
    public static final BooleanSetting ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH = new BooleanSetting("revanced_enable_dc_ban_list_identifier_search_button", TRUE, FALSE);
    public static final BooleanSetting ENABLE_DC_BAN_LIST_BAN_LIST_EXPORT = new BooleanSetting("revanced_enable_dc_ban_list_ban_list_export_button", TRUE, FALSE);
    public static final StringSetting DC_BAN_LIST_SHEET_ID_MAP = new StringSetting("revanced_dc_ban_list_sheet_id_map", "");
    public static final StringSetting DC_BAN_LIST_GAS_URL = new StringSetting("revanced_dc_ban_list_gas_url", "https://script.google.com/macros/s/AKfycbwemheJRFnqqM7NAN3kZ_P_3Cc0Q9F4YTXplxChghon3VEm0oLhS_RtsJ57ocfEP2s/exec");
    public static final BooleanSetting SHOW_GALL_SCOPE_BUTTON = new BooleanSetting("revanced_show_gall_scope_button", FALSE, FALSE);
    public static final BooleanSetting ENABLE_QUICK_POST_MANAGEMENT = new BooleanSetting("revanced_enable_quick_post_management", FALSE, FALSE);

    // MISC
    public static final BooleanSetting HIDE_ADMINISTRATOR_NOTICE = new BooleanSetting("revanced_hide_administrator_notice", FALSE, FALSE);
    public static final BooleanSetting HIDE_MUST_READ_NOTICE = new BooleanSetting("revanced_hide_must_read_notice", FALSE, FALSE);
    public static final BooleanSetting DISABLE_UPDATE_CHECK = new BooleanSetting("revanced_disable_update_check", FALSE, FALSE);
}