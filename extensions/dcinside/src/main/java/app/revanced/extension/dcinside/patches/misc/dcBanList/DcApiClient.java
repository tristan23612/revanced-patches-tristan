package app.revanced.extension.dcinside.patches.misc.dcBanList;

import java.util.concurrent.TimeUnit;

import java.io.IOException;

import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DcApiClient {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    /**
     * 모바일 차단내역 페이지 요청.
     * @param galleryType JsonHookPatch.galleryType ("mini" | "mgallery")
     * @param galleryId 갤러리 ID
     * @param page 요청할 페이지 번호
     */
    public static void fetchBanListPage(String galleryType, String galleryId, int page, Callback callback) {
        HTTP_CLIENT.newCall(buildRequest(galleryType, galleryId, page)).enqueue(callback);
    }

    /**
     * 순차 루프(parseBanList)에서 쓰는 동기 호출. 반드시 백그라운드 스레드에서만 호출할 것.
     */
    public static Response fetchBanListPageSync(String galleryType, String galleryId, int page) throws IOException {
        return HTTP_CLIENT.newCall(buildRequest(galleryType, galleryId, page)).execute();
    }

    private static Request buildRequest(String galleryType, String galleryId, int page) {
        String segment = "mini".equals(galleryType) ? "mini" : "minor";

        String url = "https://m.dcinside.com/management/" + segment + "/avoid/"
                + galleryId
                + "?page=" + page
                + "&app_id=" + JsonHookPatch.appId
                + "&confirm_id=" + JsonHookPatch.userId;

        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
