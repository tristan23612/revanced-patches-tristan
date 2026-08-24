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
import app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope.GallScopePatch;
import app.revanced.extension.dcinside.settings.Settings;

public class ShowUserIdPatch {
    private ShowUserIdPatch() {}

    private static final int DEFAULT_USER_ID_COLOR = Color.parseColor("#9E9E9E");

    private static final String TAG = "ReVanced_DCInside";

    @SuppressLint({"DiscouragedApi", "SetTextI18n"})
    public static void setUserId(View view, String userId, CharSequence charSequence) {
        if (view == null || !Settings.SHOW_USER_ID.get()) return;

        try {
            Context context = view.getContext();

            int targetTextViewId = context.getResources().getIdentifier(
                    "revanced_user_id", "id", context.getPackageName()
            );

            if (targetTextViewId == 0) return;

            TextView userIdTextView = view.findViewById(targetTextViewId);
            if (userIdTextView == null) return;

            if (!TextUtils.isEmpty(userId)) {
                userIdTextView.setText("(" + userId + ") ");
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

        View rootView = view.getRootView();
        // 1. 동적 리소스 ID 탐색 및 예외 처리
        int resId = rootView.getResources().getIdentifier(
                "revanced_user_id",
                "id",
                rootView.getContext().getPackageName()
        );
        if (resId == 0) return;

        TextView userIdView = rootView.findViewById(resId);
        if (userIdView == null) return;

        // 2. 일반 클릭 리스너 바인딩
        userIdView.setOnClickListener(v -> {
            CharSequence rawText = userIdView.getText();
            if (rawText == null || rawText.length() == 0) return;

            // 3. 정규식을 통한 공백 및 괄호('(', ')') 일체 제거
            String cleanedUserId = rawText.toString().replaceAll("[()\\s]", "");
            if (cleanedUserId.isEmpty()) return;

            GallScopePatch.showGallScopeDialogWithUserId(v.getContext(), cleanedUserId);
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
