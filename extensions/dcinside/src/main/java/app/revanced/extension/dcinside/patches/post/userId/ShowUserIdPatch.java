package app.revanced.extension.dcinside.patches.post.userId;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import app.revanced.extension.dcinside.patches.hook.okhttp.CustomNetworkInterceptorPatch;
import app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope.GallScopePatch;
import app.revanced.extension.dcinside.settings.Settings;

import java.util.HashMap;
import java.util.Map;

public class ShowUserIdPatch {
    private ShowUserIdPatch() {}

    private static final int DEFAULT_USER_ID_COLOR = Color.parseColor("#9E9E9E");

    private static final String TAG = "ReVanced_DCInside";

    @SuppressLint({"DiscouragedApi", "SetTextI18n"})
    public static void setUserId(View view, String userId, CharSequence charSequence) {
        if (view == null || !Settings.SHOW_USER_ID.get()) return;

        try {
            Context context = view.getContext();

            int targetSpaceId = context.getResources().getIdentifier(
                    "revanced_gallery_data_space", "id", context.getPackageName()
            );

            View galleryDataSpaceView = view.findViewById(targetSpaceId);
            if (galleryDataSpaceView != null) {
                Map<String, String> galleryData = new HashMap<>();
                galleryData.put("gallery_id", CustomNetworkInterceptorPatch.galleryId);
                galleryData.put("gallery_type", JsonHookPatch.galleryType);

                galleryDataSpaceView.setTag(galleryData);
            }

            int targetTextViewId = context.getResources().getIdentifier(
                    "revanced_user_id", "id", context.getPackageName()
            );

            if (targetTextViewId == 0) return;

            TextView userIdTextView = view.findViewById(targetTextViewId);
            if (userIdTextView == null) return;

            if (!TextUtils.isEmpty(userId)) {
                userIdTextView.setText(userId + " ");
                int color = extractMemoColor(charSequence);
                userIdTextView.setTextColor(color != 0 ? color : DEFAULT_USER_ID_COLOR);
                userIdTextView.setVisibility(View.VISIBLE);
            } else {
                userIdTextView.setVisibility(View.GONE);
            }

            setGallScopeUserIdClickListener(view);
        } catch (Exception e) {
            Log.e(TAG, "Error in setUserId", e);
        }
    }

    @SuppressLint("DiscouragedApi")
    private static void setGallScopeUserIdClickListener(View view) {
        if (view == null || !Settings.SHOW_USER_ID.get() || !Settings.SHOW_GALL_SCOPE_BUTTON.get()) return;

        Context context = view.getContext();

        int resId = view.getResources().getIdentifier(
                "revanced_user_id",
                "id",
                context.getPackageName()
        );
        if (resId == 0) return;

        TextView userIdView = view.findViewById(resId);
        if (userIdView == null) return;

        String userId = userIdView.getText().toString().trim();

        int targetSpaceId = context.getResources().getIdentifier(
                "revanced_gallery_data_space", "id", context.getPackageName()
        );

        View spaceView = view.findViewById(targetSpaceId);
        String galleryId;
        String galleryType;
        if (spaceView != null && spaceView.getTag() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> galleryData = (Map<String, String>) spaceView.getTag();

            galleryId = galleryData.get("gallery_id");
            galleryType = galleryData.get("gallery_type");
        } else {
            galleryId = "";
            galleryType = "";
        }

        userIdView.setOnClickListener(v -> {
            GallScopePatch.showGallScopeDialogWithUserId(v.getContext(), userId, galleryType, galleryId);
        });
    }

    public static String addUserId(String userName, String userId, String userIp) {
        if ((!TextUtils.isEmpty(userId) || !TextUtils.isEmpty(userIp)) && Settings.SHOW_USER_ID.get()) {
            return userName + " (" + userId + userIp + ")";
        }
        return userName;
    }

    private static int extractMemoColor(CharSequence charSequence) {
        if (!(charSequence instanceof Spanned spanned)) return 0;
        ForegroundColorSpan[] spans = spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class);
        if (spans.length == 0) return 0;
        return spans[0].getForegroundColor();
    }
}
