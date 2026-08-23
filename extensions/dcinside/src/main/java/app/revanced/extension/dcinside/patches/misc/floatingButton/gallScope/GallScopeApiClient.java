package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

import java.util.concurrent.TimeUnit;

import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

final class GallScopeApiClient {
    private static final String PC_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private GallScopeApiClient() {
    }

    static void fetchListPage(String galleryType, String galleryId, int page, Callback callback) {
        HTTP_CLIENT.newCall(buildRequest(galleryType, galleryId, page)).enqueue(callback);
    }

    private static Request buildRequest(String galleryType, String galleryId, int page) {
        String segment;
        if ("gallery".equalsIgnoreCase(galleryType)) {
            segment = "";
        } else {
            segment = galleryType + "/";
        }

        String sanitizedGalleryId = galleryId;
        if (sanitizedGalleryId != null && sanitizedGalleryId.startsWith("mi$")) {
            sanitizedGalleryId = sanitizedGalleryId.substring(3);
        }

        String url = "https://gall.dcinside.com/" + segment + "board/lists/"
                + "?id=" + sanitizedGalleryId
                + "&page=" + page;

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", PC_USER_AGENT)
                .build();
    }
}