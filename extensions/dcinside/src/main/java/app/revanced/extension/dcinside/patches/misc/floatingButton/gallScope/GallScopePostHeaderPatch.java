package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;
import app.revanced.extension.dcinside.settings.Settings;

public class GallScopePostHeaderPatch {
    private GallScopePostHeaderPatch() {}

    @SuppressLint("DiscouragedApi")
    public static void setGallScopeClickListener(View view) {
        if (view == null || !Settings.SHOW_USER_ID.get() || !Settings.SHOW_GALL_SCOPE_BUTTON.get()) return;

        // 1. 동적 리소스 ID 탐색 및 예외 처리
        int resId = view.getResources().getIdentifier(
                "revanced_user_id",
                "id",
                view.getContext().getPackageName()
        );
        if (resId == 0) return;

        TextView userIdView = view.findViewById(resId);
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
}
