package app.revanced.extension.dcinside.patches.misc.dcBanList;

import android.webkit.CookieManager;
import java.util.concurrent.TimeUnit;
import app.revanced.extension.dcinside.settings.preference.GoogleLoginWebViewPreference;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class GasApiClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String GAS_URL = "https://script.google.com/macros/s/AKfycbwemheJRFnqqM7NAN3kZ_P_3Cc0Q9F4YTXplxChghon3VEm0oLhS_RtsJ57ocfEP2s/exec";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    /**
     * GET 요청 (상태/인증 확인 등)
     */
    public static void sendGet(String queryString, Callback callback) {
        executeRequest("GET", queryString, null, callback);
    }

    /**
     * POST 요청 (데이터 업로드 등)
     */
    public static void sendPost(String queryString, String jsonPayload, Callback callback) {
        executeRequest("POST", queryString, jsonPayload, callback);
    }

    /**
     * 공통 요청 캡슐화 (중복 로직 제거 및 HTTP Method 분기)
     */
    private static void executeRequest(String method, String queryString, String jsonPayload, Callback callback) {
        CookieManager cookieManager = CookieManager.getInstance();

        String googleCookie = cookieManager.getCookie("https://script.google.com");
        if (googleCookie == null || googleCookie.isEmpty()) {
            googleCookie = cookieManager.getCookie("https://accounts.google.com");
        }

        String fullUrl = (queryString != null && !queryString.isEmpty()) ? GAS_URL + queryString : GAS_URL;

        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", GoogleLoginWebViewPreference.MOBILE_CHROME_USER_AGENT);

        if (googleCookie != null && !googleCookie.isEmpty()) {
            requestBuilder.addHeader("Cookie", googleCookie);
        }

        // HTTP Method 지정
        if ("POST".equalsIgnoreCase(method)) {
            RequestBody body = RequestBody.create(jsonPayload != null ? jsonPayload : "", JSON_MEDIA_TYPE);
            requestBuilder.post(body);
        } else {
            requestBuilder.get(); // GET 방식 명시
        }

        HTTP_CLIENT.newCall(requestBuilder.build()).enqueue(callback);
    }
}