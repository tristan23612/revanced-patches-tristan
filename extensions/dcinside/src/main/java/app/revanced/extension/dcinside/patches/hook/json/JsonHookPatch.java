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

    public static InputStream parseJsonHook(@NotNull InputStream jsonInputStream) {
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

                if (jsonObject.has("gall_info")) {
                    JSONArray gallInfoArray = jsonObject.optJSONArray("gall_info");
                    if (gallInfoArray != null && gallInfoArray.length() > 0) {
                        JSONObject gallInfo = gallInfoArray.optJSONObject(0);
                        if (gallInfo != null) {
                            managerSkill = gallInfo.optBoolean("managerskill", false);

                            if (gallInfo.optBoolean("is_minor", false)) {
                                galleryType = "mgallery";
                            } else if (gallInfo.optBoolean("is_mini", false)) {
                                galleryType = "mini";
                            } else {
                                galleryType = "gallery";
                            }
                        }
                    }
                }

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
}