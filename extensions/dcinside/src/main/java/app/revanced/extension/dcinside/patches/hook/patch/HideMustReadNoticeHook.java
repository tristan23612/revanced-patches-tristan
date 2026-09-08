package app.revanced.extension.dcinside.patches.hook.patch;

import android.util.Log;
import androidx.annotation.NonNull;
import app.revanced.extension.dcinside.patches.hook.json.BaseJsonHook;
import app.revanced.extension.dcinside.settings.Settings;
import org.json.JSONObject;

public final class HideMustReadNoticeHook extends BaseJsonHook {
    public static final HideMustReadNoticeHook INSTANCE = new HideMustReadNoticeHook();

    private static final String TAG = "ReVanced";

    private HideMustReadNoticeHook() {
    }

    @Override
    public JSONObject apply(@NonNull JSONObject jsonObject) {
        if (!Settings.HIDE_MUST_READ_NOTICE.get()) return jsonObject;

        try {
            if (!jsonObject.has("gall_info")) return jsonObject;

            JSONObject gallInfo = jsonObject.getJSONArray("gall_info").getJSONObject(0);
            if (gallInfo.has("must_read")) {
                gallInfo.remove("must_read");
                Log.d(TAG, "HideMustReadNoticeHook: removed must_read=" + gallInfo.optString("must_read"));
            }

            return jsonObject;
        } catch (Exception e) {
            Log.e(TAG, "HideMustReadNoticeHook: failed to parse JSON", e);
            return jsonObject;
        }
    }
}
