package app.revanced.extension.dcinside.patches.misc.floatingButton.dcBanList;

import app.revanced.extension.dcinside.patches.hook.okhttp.CustomNetworkInterceptorPatch;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DcBanListApiClient {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

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
                + "&app_id=" + CustomNetworkInterceptorPatch.appId
                + "&confirm_id=" + CustomNetworkInterceptorPatch.userId;

        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
