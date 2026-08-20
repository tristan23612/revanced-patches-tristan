package app.revanced.extension.dcinside.settings.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Message;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Shared WebView dialog builder used by Google account related preferences
 * (login, manage account, etc). Handles common WebSettings, popup window
 * support (window.open), and fullscreen dialog presentation.
 */
public final class GoogleWebViewDialogHelper {
    public static final String MOBILE_CHROME_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    private GoogleWebViewDialogHelper() {
    }

    @SuppressLint("DiscouragedApi")
    static int resolveDialogTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(
                context.getResources().getIdentifier("alertDialogTheme", "attr", context.getPackageName()),
                typedValue,
                true
        );
        return typedValue.resourceId;
    }

    /**
     * Creates and shows a fullscreen WebView dialog loading the given URL.
     * Supports popup windows (window.open) via a nested fullscreen WebView dialog.
     */
    static void showFullscreenWebViewDialog(Context context, String url) {
        WebView webView = new WebView(context);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        applyDefaultSettings(webView);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                showPopupWebViewDialog(context, resultMsg);
                return true;
            }
        });

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
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        webView.loadUrl(url);
    }

    private static void showPopupWebViewDialog(Context context, Message resultMsg) {
        WebView popupWebView = new WebView(context);
        applyDefaultSettings(popupWebView);

        Dialog popupDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        popupDialog.setContentView(popupWebView);
        popupDialog.setOnDismissListener(d -> popupWebView.destroy());

        popupWebView.setWebViewClient(new WebViewClient());
        popupWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView window) {
                popupDialog.dismiss();
            }
        });

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(popupWebView);
        resultMsg.sendToTarget();

        popupDialog.show();
    }

    private static void applyDefaultSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(MOBILE_CHROME_USER_AGENT);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
    }
}