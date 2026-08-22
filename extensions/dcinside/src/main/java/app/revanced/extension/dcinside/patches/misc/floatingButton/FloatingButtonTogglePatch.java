package app.revanced.extension.dcinside.patches.misc.floatingButton;

import android.annotation.SuppressLint;
import android.view.View;
import app.revanced.extension.dcinside.settings.Settings;

@SuppressLint("DiscouragedApi")
public final class FloatingButtonTogglePatch {
    public static void setFloatingButtonToggleVisibility(View targetView, boolean visible, String idPrefix) {
        if (targetView == null) return;

        View rootView = targetView.getRootView();
        int toggleId = rootView.getResources().getIdentifier(
                idPrefix + "_toggle", "id", rootView.getContext().getPackageName());

        View toggleButton = rootView.findViewById(toggleId);
        if (toggleButton != null) {
            boolean shouldShow = visible && (Settings.SHOW_DC_BAN_LIST_BUTTON.get() || Settings.SHOW_GALL_SCOPE_BUTTON.get());
            toggleButton.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    public static void setupFloatingButtonToggle(View view, String idPrefix) {
        if (view == null) return;

        View rootView = view.getRootView();
        int toggleButtonResId = rootView.getResources().getIdentifier(idPrefix + "_toggle", "id", rootView.getContext().getPackageName());
        int subContainerResId = rootView.getResources().getIdentifier(idPrefix + "_sub_container", "id", rootView.getContext().getPackageName());

        View toggleButton = rootView.findViewById(toggleButtonResId);
        View subContainer = rootView.findViewById(subContainerResId);
        if (toggleButton == null || subContainer == null) return;

        // 컨테이너가 아니라 그 안의 아이콘 ImageView만 찾는다
        View toggleIcon = findImageViewChild(toggleButton);
        if (toggleIcon == null) toggleIcon = toggleButton; // 못 찾으면 폴백

        View iconToRotate = toggleIcon;
        toggleButton.setOnClickListener(v -> {
            boolean currentlyExpanded = subContainer.getVisibility() == View.VISIBLE;
            animateToggle(iconToRotate, subContainer, !currentlyExpanded);
        });
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
