package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import android.webkit.CookieManager;
import app.revanced.extension.dcinside.settings.Settings;
import app.revanced.extension.dcinside.settings.preference.GoogleWebViewDialogHelper;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class GasApiClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    /**
     * GET 요청
     */
    public static void sendGet(String queryString, Callback callback) {
        executeRequest("GET", queryString, null, callback);
    }

    /**
     * POST 요청
     */
    public static void sendPost(String queryString, String jsonPayload, Callback callback) {
        executeRequest("POST", queryString, jsonPayload, callback);
    }

    private static void executeRequest(String method, String queryString, String jsonPayload, Callback callback) {
        CookieManager cookieManager = CookieManager.getInstance();

        String googleCookie = cookieManager.getCookie("https://script.google.com");
        if (googleCookie == null || googleCookie.isEmpty()) {
            googleCookie = cookieManager.getCookie("https://accounts.google.com");
        }

        String fullUrl = (queryString != null && !queryString.isEmpty()) ? Settings.DC_BAN_LIST_GAS_URL.get() + queryString : Settings.DC_BAN_LIST_GAS_URL.get();

        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", GoogleWebViewDialogHelper.MOBILE_CHROME_USER_AGENT);

        if (googleCookie != null && !googleCookie.isEmpty()) {
            requestBuilder.addHeader("Cookie", googleCookie);
        }

        if ("POST".equalsIgnoreCase(method)) {
            RequestBody body = RequestBody.create(jsonPayload != null ? jsonPayload : "", JSON_MEDIA_TYPE);
            requestBuilder.post(body);
        } else {
            requestBuilder.get();
        }

        HTTP_CLIENT.newCall(requestBuilder.build()).enqueue(callback);
    }
}