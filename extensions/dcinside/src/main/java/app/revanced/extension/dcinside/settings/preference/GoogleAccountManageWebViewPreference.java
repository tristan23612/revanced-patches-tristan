package app.revanced.extension.dcinside.settings.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.revanced.extension.shared.Logger;

@SuppressWarnings({"unused", "deprecation"})
public class GoogleAccountManageWebViewPreference extends Preference {

    {
        setOnPreferenceClickListener(pref -> {
            try {
                GoogleWebViewDialogHelper.showFullscreenWebViewDialog(
                        pref.getContext(),
                        "https://accounts.google.com/"
                );
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to show Google account WebView", ex);
            }
            return true;
        });
    }

    public GoogleAccountManageWebViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public GoogleAccountManageWebViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GoogleAccountManageWebViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoogleAccountManageWebViewPreference(Context context) {
        super(context);
    }
}