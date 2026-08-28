package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;

/** Anonymous CSV reader for sheets shared with anyone who has the link. */
final class DcBanListSheetClient {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    private DcBanListSheetClient() {
    }

    static void fetchFirstSheetCsv(String sheetId, Callback callback) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("docs.google.com")
                .addPathSegment("spreadsheets")
                .addPathSegment("d")
                .addPathSegment(sheetId)
                .addPathSegment("export")
                .addQueryParameter("format", "csv")
                .build();

        Request request = new Request.Builder().url(url).get().build();
        HTTP_CLIENT.newCall(request).enqueue(callback);
    }
}
