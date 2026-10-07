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

    public enum TokenType {
        VIEW,
        INGEST
    }

    static void sendGet(
            String queryString,
            TokenType tokenType,
            Callback callback
    ) {
        String token = switch (tokenType) {
            case VIEW -> Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_VIEW_TOKEN.get();
            case INGEST -> Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_INGEST_TOKEN.get();
        };

        executeRequest("GET", queryString, null, token, callback);
    }

    public static void sendPost(
            String queryString,
            String jsonPayload,
            TokenType tokenType,
            Callback callback
    ) {
        String token = switch (tokenType) {
            case VIEW -> Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_VIEW_TOKEN.get();
            case INGEST -> Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_INGEST_TOKEN.get();
        };

        executeRequest("POST", queryString, jsonPayload, token, callback);
    }

    private static void executeRequest(
            String method,
            String queryString,
            String jsonPayload,
            String token,
            Callback callback
    ) {
        String base = Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_URL.get();
        String fullUrl = (queryString != null && !queryString.isEmpty()) ? base + queryString : base;

        Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .header("Authorization", "Bearer " + token);

        if ("POST".equalsIgnoreCase(method)) {
            requestBuilder.post(RequestBody.create(jsonPayload != null ? jsonPayload : "", JSON_MEDIA_TYPE));
        } else {
            requestBuilder.get();
        }

        HTTP_CLIENT.newCall(requestBuilder.build()).enqueue(callback);
    }
}