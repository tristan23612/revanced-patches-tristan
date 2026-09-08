package app.revanced.extension.dcinside.patches.management.floatingButton.gallScope;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressLint("DiscouragedApi")
public class GallScopePatch {

    public static void setGallScopeButtonVisibility(View targetView, boolean visible, String gallScopeButtonIdName) {
        if (targetView == null) return;

        View rootView = targetView.getRootView();
        int resId = rootView.getContext().getResources().getIdentifier(
                gallScopeButtonIdName, "id", rootView.getContext().getPackageName());

        View gallScopeButton = rootView.findViewById(resId);
        if (gallScopeButton != null) {
            gallScopeButton.setOnClickListener(buttonView -> new GallScopeSession(buttonView.getContext(), null, null, null).start());
            gallScopeButton.setVisibility(visible && Settings.SHOW_GALL_SCOPE_BUTTON.get() ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 게시글 화면 등 외부에서 식별코드를 이미 알고 있을 때 바로 모드 선택부터 시작
     */
    public static void showGallScopeDialogWithUserId(Context context, String prefillUserId, String prefillGalleryType, String prefillGalleryId) {
        if (!Settings.ENABLE_USER_ID_GALL_SCOPE.get()) {
            return;
        }

        new GallScopeSession(context, prefillUserId, prefillGalleryType, prefillGalleryId).start();
    }
}