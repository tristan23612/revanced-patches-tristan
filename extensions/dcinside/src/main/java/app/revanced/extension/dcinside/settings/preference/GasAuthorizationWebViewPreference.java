package app.revanced.extension.dcinside.settings.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.revanced.extension.dcinside.settings.Settings;
import app.revanced.extension.shared.Logger;

@SuppressWarnings({"unused", "deprecation"})
public class GasAuthorizationWebViewPreference extends Preference {

    {
        setOnPreferenceClickListener(pref -> {
            try {
                GoogleWebViewDialogHelper.showFullscreenWebViewDialog(
                        pref.getContext(),
                        Settings.DC_BAN_LIST_GAS_URL.get()
                );
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to show Google login WebView", ex);
            }
            return true;
        });
    }

    public GasAuthorizationWebViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public GasAuthorizationWebViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GasAuthorizationWebViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GasAuthorizationWebViewPreference(Context context) {
        super(context);
    }
}