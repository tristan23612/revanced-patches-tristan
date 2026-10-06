package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import app.revanced.extension.dcinside.settings.Settings;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class CloudflareWorkerClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    static boolean isConfigured() {
        HttpUrl url = HttpUrl.parse(Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_URL.get().trim());
        return url != null
                && url.isHttps()
                && !Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_TOKEN.get().trim().isEmpty();
    }

    static void sendGet(String queryString, Callback callback) {
        executeRequest("GET", queryString, null, callback);
    }

    public static void sendPost(String queryString, String jsonPayload, Callback callback) {
        executeRequest("POST", queryString, jsonPayload, callback);
    }

    private static void executeRequest(String method, String queryString, String jsonPayload, Callback callback) {
        String base = Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_URL.get();
        String fullUrl = (queryString != null && !queryString.isEmpty()) ? base + queryString : base;

        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .header("Authorization", "Bearer " + Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_TOKEN.get().trim());

        if ("POST".equalsIgnoreCase(method)) {
            requestBuilder.post(RequestBody.create(jsonPayload != null ? jsonPayload : "", JSON_MEDIA_TYPE));
        } else {
            requestBuilder.get();
        }

        HTTP_CLIENT.newCall(requestBuilder.build()).enqueue(callback);
    }
}