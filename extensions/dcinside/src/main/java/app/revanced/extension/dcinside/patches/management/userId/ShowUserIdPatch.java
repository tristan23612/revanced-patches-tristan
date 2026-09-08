package app.revanced.extension.dcinside.patches.management.userId;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import app.revanced.extension.dcinside.patches.hook.okhttp.CustomNetworkInterceptorPatch;
import app.revanced.extension.dcinside.patches.management.floatingButton.gallScope.GallScopePatch;
import app.revanced.extension.dcinside.settings.Settings;

import java.util.HashMap;
import java.util.Map;

@SuppressLint({"DiscouragedApi", "SetTextI18n"})
public class ShowUserIdPatch {
    private ShowUserIdPatch() {}

    private static final int DEFAULT_USER_ID_COLOR = Color.parseColor("#9E9E9E");

    private static final String[] MEMO_VIEW_CANDIDATES = {
            "read_header_user_memo",
            "reply_user_memo",
            "post_list_item_counts",
    };

    private static final String TAG = "ReVanced_DCInside";

    public static void setUserId(View view, String userId) {
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
                userIdTextView.setText(" " + userId + " ");
                int color = extractMemoColorFromView(view);
                userIdTextView.setTextColor(color != 0 ? color : DEFAULT_USER_ID_COLOR);
                userIdTextView.setVisibility(View.VISIBLE);
            } else {
                userIdTextView.setVisibility(View.GONE);
            }

            setGallScopeUserIdClickListener(view);
            setGallScopeUserIdLongClickListener(view);
        } catch (Exception e) {
            Log.e(TAG, "Error in setUserIdTest", e);
        }
    }

    private static int extractMemoColorFromView(View view) {
        try {
            for (String idName : MEMO_VIEW_CANDIDATES) {
                int color = extractSpanColor(view, idName);
                if (color != 0) return color;
            }
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting memo color", e);
            return 0;
        }
    }

    private static int extractSpanColor(View view, String idName) {
        Context context = view.getContext();
        int id = context.getResources().getIdentifier(idName, "id", context.getPackageName());
        if (id == 0) return 0;

        View found = view.findViewById(id);
        if (!(found instanceof TextView textView)) return 0;
        if (textView.getVisibility() != View.VISIBLE) return 0;

        CharSequence text = textView.getText();
        if (TextUtils.isEmpty(text) || text.charAt(0) != '-') return 0;

        if (text instanceof Spanned spanned) {
            ForegroundColorSpan[] spans = spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class);
            if (spans.length > 0) return spans[0].getForegroundColor();
        }

        return textView.getCurrentTextColor();
    }

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

    private static void setGallScopeUserIdLongClickListener(View view) {
        if (view == null || !Settings.SHOW_USER_ID.get()) return;

        Context context = view.getContext();

        int resId = context.getResources().getIdentifier(
                "revanced_user_id",
                "id",
                context.getPackageName()
        );
        if (resId == 0) return;

        TextView userIdView = view.findViewById(resId);
        if (userIdView == null) return;

        userIdView.setOnLongClickListener(v -> {
            String userId = userIdView.getText().toString().trim();
            if (TextUtils.isEmpty(userId)) return false;

            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("User ID", userId);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(context, "User ID가 복사되었습니다: " + userId, Toast.LENGTH_SHORT).show();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy userId to clipboard", e);
            }
            return false;
        });
    }

    public static String addUserId(String userName, String userId, String userIp) {
        if ((!TextUtils.isEmpty(userId) || !TextUtils.isEmpty(userIp)) && Settings.SHOW_USER_ID.get()) {
            return userName + " (" + userId + userIp + ")";
        }
        return userName;
    }
}
