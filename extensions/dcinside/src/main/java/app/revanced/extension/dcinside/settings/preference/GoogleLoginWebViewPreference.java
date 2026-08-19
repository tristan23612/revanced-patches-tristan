package app.revanced.extension.dcinside.settings.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import app.revanced.extension.shared.Logger;

/**
 * Tapping this preference opens an in-app WebView dialog for Google account login,
 * so the session cookie can be reused for later GAS(Google Apps Script) requests.
 */
@SuppressWarnings({"unused", "deprecation"})
public class GoogleLoginWebViewPreference extends Preference {
    private static final String LOGIN_URL = "https://accounts.google.com/";

    public static final String MOBILE_CHROME_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    @SuppressLint("DiscouragedApi")
    private static int resolveDialogTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(
                context.getResources().getIdentifier("alertDialogTheme", "attr", context.getPackageName()),
                typedValue,
                true
        );
        return typedValue.resourceId;
    }

    {
        setOnPreferenceClickListener(pref -> {
            try {
                showLoginDialog(pref.getContext());
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to show Google login WebView", ex);
            }
            return true;
        });
    }

    private void showLoginDialog(Context context) {
        WebView webView = new WebView(context);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(MOBILE_CHROME_USER_AGENT);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());

        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(webView);

        dialog.setOnDismissListener(dialogInterface -> {
            cookieManager.flush();
            webView.destroy();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        webView.loadUrl(LOGIN_URL);
    }

    public GoogleLoginWebViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public GoogleLoginWebViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GoogleLoginWebViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoogleLoginWebViewPreference(Context context) {
        super(context);
    }
}