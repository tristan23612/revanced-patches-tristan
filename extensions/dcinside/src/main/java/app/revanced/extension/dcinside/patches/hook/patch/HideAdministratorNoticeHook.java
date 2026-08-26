package app.revanced.extension.dcinside.patches.hook.patch;

import android.util.Log;
import androidx.annotation.NonNull;
import app.revanced.extension.dcinside.patches.hook.json.BaseJsonHook;
import app.revanced.extension.dcinside.settings.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

public final class HideAdministratorNoticeHook extends BaseJsonHook {
    public static final HideAdministratorNoticeHook INSTANCE = new HideAdministratorNoticeHook();

    private static final String TAG = "ReVanced";

    private HideAdministratorNoticeHook() {
    }

    @Override
    public JSONObject apply(@NonNull JSONObject jsonObject) {
        if (!Settings.HIDE_ADMINISTRATOR_NOTICE.get()) return jsonObject;

        try {
            if (!jsonObject.has("gall_list")) return jsonObject;

            JSONArray gallList = jsonObject.getJSONArray("gall_list");
            JSONArray filtered = new JSONArray();

            for (int i = 0; i < gallList.length(); i++) {
                JSONObject post = gallList.getJSONObject(i);
                String no = post.optString("no");
                if (!no.isEmpty()) {
                    filtered.put(post);
                } else {
                    Log.d(TAG, "HideAdministratorNoticeHook: filtered out post subject=" + post.optString("subject"));
                }
            }

            jsonObject.put("gall_list", filtered);
            return jsonObject;
        } catch (Exception e) {
            Log.e(TAG, "HideAdministratorNoticeHook: failed to parse JSON", e);
            return jsonObject;
        }
    }
}