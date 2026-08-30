package app.revanced.extension.dcinside.patches.management.post.duplicatePost;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Callback;

final class DuplicatePostSearchApiClient {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private static final String SEARCH_URL_PREFIX = "https://search.dcinside.com/post/sort/latest/q/";

    private DuplicatePostSearchApiClient() {
    }

    static void fetchSearchResult(String title, Callback callback) {
        Request request = new Request.Builder().url(buildSearchUrl(title)).get().build();
        HTTP_CLIENT.newCall(request).enqueue(callback);
    }

    static String buildSearchUrl(String title) {
        return SEARCH_URL_PREFIX + encodeQuery(title);
    }

    private static String encodeQuery(String title) {
        String trimmed = title == null ? "" : title.trim();
        byte[] bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            sb.append('.');
            String hex = Integer.toHexString(b & 0xFF).toUpperCase();
            if (hex.length() < 2) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}