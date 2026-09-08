package app.revanced.extension.dcinside.patches.management.floatingButton.gallScope;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;

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

    static void fetchPostListPage(String galleryType, String galleryId, int page, Callback callback) {
        HTTP_CLIENT.newCall(buildPostRequest(galleryType, galleryId, page)).enqueue(callback);
    }

    static void fetchCommentListPage(String galleryType, String galleryId, int page, String searchPos, Callback callback) {
        HTTP_CLIENT.newCall(buildCommentRequest(galleryType, galleryId, page, searchPos)).enqueue(callback);
    }

    private static Request buildPostRequest(String galleryType, String galleryId, int page) {
        String url = "https://gall.dcinside.com/" + resolveSegment(galleryType) + "board/lists/"
                + "?id=" + sanitizeGalleryId(galleryId)
                + "&page=" + page;

        return new Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", PC_USER_AGENT)
                .build();
    }

    private static Request buildCommentRequest(String galleryType, String galleryId, int page, String searchPos) {
        // 원본 템퍼몽키 스크립트와 동일하게 board/lists 엔드포인트에 s_type=search_comment 파라미터로 요청
        StringBuilder url = new StringBuilder("https://gall.dcinside.com/")
                .append(resolveSegment(galleryType))
                .append("board/lists/")
                .append("?id=").append(sanitizeGalleryId(galleryId))
                .append("&s_type=search_comment")
                .append("&s_keyword=%2520")
                .append("&page=").append(page);

        if (searchPos != null && !searchPos.isEmpty()) {
            url.append("&search_pos=").append(searchPos);
        }

        return new Request.Builder()
                .url(url.toString())
                .get()
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("User-Agent", PC_USER_AGENT)
                .build();
    }

    private static String resolveSegment(String galleryType) {
        if ("gallery".equalsIgnoreCase(galleryType)) {
            return "";
        }
        return galleryType + "/";
    }

    private static String sanitizeGalleryId(String galleryId) {
        if (galleryId != null && galleryId.startsWith("mi$")) {
            return galleryId.substring(3);
        }
        return galleryId;
    }
}