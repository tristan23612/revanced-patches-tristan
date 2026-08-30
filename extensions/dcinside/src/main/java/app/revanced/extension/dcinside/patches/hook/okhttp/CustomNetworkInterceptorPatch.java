package app.revanced.extension.dcinside.patches.hook.okhttp;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import okhttp3.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public final class CustomNetworkInterceptorPatch implements Interceptor {
    private static final String TAG = "ReVanced_DCInside";

    private static final List<String> URL_FILTER_KEYWORD_LIST = List.of(
            "gall_list_new",
            "gall_view_new"
    );

    public static String appId = "";
    public static String userId = "";
    public static String galleryId = "";

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        boolean isTargetUrl = false;
        boolean isListRequest = false;
        String path = request.url().encodedPath();
        for (String keyword : URL_FILTER_KEYWORD_LIST) {
            if (path.contains(keyword)) {
                isTargetUrl = true;
                isListRequest = keyword.equals("gall_list_new");
                break;
            }
        }

        if (!isTargetUrl) {
            return chain.proceed(request);
        }

        galleryId = request.url().queryParameter("id");

        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            Log.e(TAG, "<-- [FAILED] " + path + " : " + e.getMessage());
            throw e;
        }

        ResponseBody body = response.body();
        try {
            String encoding = response.header("Content-Encoding");
            boolean isGzip = encoding != null && encoding.equalsIgnoreCase("gzip");

            InputStream responseStream = body.byteStream();
            if (isGzip) {
                responseStream = new GZIPInputStream(responseStream);
            }
            byte[] rawBytes = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                rawBytes = responseStream.readAllBytes();
            }

            InputStream modifiedStream = JsonHookPatch.parseJsonHook(new ByteArrayInputStream(rawBytes), isListRequest);
            byte[] modifiedData = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                modifiedData = modifiedStream.readAllBytes();
            }

            if (Objects.requireNonNull(modifiedData).length == 0) {
                Log.w(TAG, "JsonHookPatch returned empty stream, falling back to raw data.");
                modifiedData = rawBytes;
            }

            MediaType contentType = body.contentType();
            Response.Builder responseBuilder = response.newBuilder();

            if (isGzip) {
                responseBuilder.removeHeader("Content-Encoding");
            }

            return responseBuilder
                    .body(ResponseBody.create(modifiedData, contentType))
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "<-- [FAILED] " + path + " : " + e.getMessage());
            return response;
        }
    }

    public static void hookParam(String key, String value) {
        if (key == null || value == null) {
            return;
        }

        if ("app_id".equals(key)) {
            appId = value;
        } else if ("user_id".equals(key) || "confirm_id".equals(key)) {
            userId = value;
        }
    }
}
