package app.revanced.extension.dcinside.patches.hook.json;

import android.util.Log;
import app.revanced.extension.dcinside.patches.hook.patch.DummyHook;
import app.revanced.extension.dcinside.utils.json.JsonUtils;
import app.revanced.extension.dcinside.utils.stream.StreamUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class JsonHookPatch {
    public static final JsonHookPatch INSTANCE = new JsonHookPatch();

    private static final String TAG = "ReVanced_DCInside";
    private static final List<JsonHook> hooks;

    public static boolean managerSkill = false;
    public static String galleryType = "";

    static {
        hooks = new ArrayList<>();
        hooks.add(DummyHook.INSTANCE);
    }

    private JsonHookPatch() {
    }

    public static InputStream parseJsonHook(@NotNull InputStream jsonInputStream, boolean isListRequest) {
        JSONArray jsonArray;
        try {
            jsonArray = JsonUtils.parseJsonArray(jsonInputStream);
        } catch (IOException | JSONException e) {
            return jsonInputStream;
        }

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.optJSONObject(i);
                if (jsonObject == null) continue;

                updateGalleryMetadata(jsonObject, isListRequest);

                for (JsonHook hook : hooks) {
                    jsonObject = hook.hook(jsonObject);
                }
                jsonArray.put(i, jsonObject);
            }

            return StreamUtils.INSTANCE.fromString(jsonArray.toString());
        } catch (Exception e) {
            Log.e(TAG, "jsonHook: failed to parse JSON", e);
            return StreamUtils.INSTANCE.fromString(jsonArray.toString());
        }
    }

    private static void updateGalleryMetadata(@NotNull JSONObject jsonObject, boolean isListRequest) {
        JSONObject infoObj;

        JSONArray gallInfoArray = jsonObject.optJSONArray("gall_info");
        if (gallInfoArray != null && gallInfoArray.length() > 0) {
            infoObj = gallInfoArray.optJSONObject(0);
        } else {
            infoObj = jsonObject.optJSONObject("view_info");
        }

        if (infoObj == null) return;

        if (infoObj.optBoolean("is_minor", false)) {
            galleryType = "mgallery";
        } else if (infoObj.optBoolean("is_mini", false)) {
            galleryType = "mini";
        } else {
            galleryType = "gallery";
        }

        // managerSkill은 리스트 요청 응답에서만 갱신 (게시글 화면 진입 시 다른 갤러리 값으로 오염되는 것 방지)
        if (!isListRequest) return;

        JSONObject viewMain = jsonObject.optJSONObject("view_main");
        if (viewMain != null && viewMain.has("managerskill")) {
            managerSkill = viewMain.optBoolean("managerskill", false);
        } else {
            managerSkill = infoObj.optBoolean("managerskill", false);
        }
    }
}