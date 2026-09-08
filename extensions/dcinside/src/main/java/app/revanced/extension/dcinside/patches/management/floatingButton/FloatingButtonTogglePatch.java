package app.revanced.extension.dcinside.patches.management.floatingButton;

import android.annotation.SuppressLint;
import android.view.View;
import app.revanced.extension.dcinside.settings.Settings;

@SuppressLint("DiscouragedApi")
public final class FloatingButtonTogglePatch {
    public static void setFloatingButtonToggleVisibility(View targetView, boolean visible, String idPrefix) {
        if (targetView == null) return;

        View rootView = targetView.getRootView();
        int toggleId = rootView.getResources().getIdentifier(idPrefix + "_toggle", "id", rootView.getContext().getPackageName());
        int subContainerId = rootView.getResources().getIdentifier(idPrefix + "_sub_container", "id", rootView.getContext().getPackageName());

        View toggleButton = rootView.findViewById(toggleId);
        View subContainer = rootView.findViewById(subContainerId);
        if (toggleButton == null) return;

        // 매번 최신 뷰 기준으로 리스너 재부착 (setup 훅과 별개로 안전망 역할)
        if (subContainer != null) {
            View toggleIcon = findImageViewChild(toggleButton);
            if (toggleIcon == null) toggleIcon = toggleButton; // 못 찾으면 폴백

            View iconToRotate = toggleIcon;
            toggleButton.setOnClickListener(v -> {
                boolean currentlyExpanded = subContainer.getVisibility() == View.VISIBLE;
                animateToggle(iconToRotate, subContainer, !currentlyExpanded);
            });
        }

        boolean shouldShow = visible && (Settings.SHOW_DC_BAN_LIST_BUTTON.get() || Settings.SHOW_GALL_SCOPE_BUTTON.get());
        toggleButton.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private static View findImageViewChild(View parent) {
        if (!(parent instanceof android.view.ViewGroup group)) return null;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof android.widget.ImageView) {
                return child;
            }
        }
        return null;
    }

    private static void animateToggle(View toggleIcon, View subContainer, boolean expand) {
        if (expand) {
            subContainer.setVisibility(View.VISIBLE);
            subContainer.setAlpha(0f);
            subContainer.setTranslationY(40f);
            subContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .start();
        } else {
            subContainer.animate()
                    .alpha(0f)
                    .translationY(40f)
                    .setDuration(200)
                    .withEndAction(() -> subContainer.setVisibility(View.GONE))
                    .start();
        }

        toggleIcon.animate()
                .rotation(expand ? 45f : 0f)
                .setDuration(200)
                .start();
    }
}
