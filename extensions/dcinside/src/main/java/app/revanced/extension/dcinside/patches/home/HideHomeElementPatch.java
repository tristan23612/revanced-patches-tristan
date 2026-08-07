package app.revanced.extension.dcinside.patches.home;

import android.view.View;
import android.view.ViewGroup;
import app.revanced.extension.dcinside.settings.Settings;

import java.util.Collections;
import java.util.List;

public class HideHomeElementPatch {

    public static void hideDcbestView(View view) {
        if (view == null || !Settings.HIDE_DCBEST.get()) {
            return;
        }
        hideView(view);
    }

    public static void hideRecommendedGalleriesView(View view) {
        if (view == null || !Settings.HIDE_RECOMMENDED_GALLERIES.get()) {
            return;
        }
        hideView(view);
    }

    public static void hideCrowdView(View view) {
        if (view == null || !Settings.HIDE_CROWD.get()) {
            return;
        }
        hideView(view);
    }

    public static List<?> hideNewGalleries(List<?> list) {
        if (list == null || !Settings.HIDE_NEW_GALLERIES.get()) {
            return list;
        }
        return Collections.emptyList();
    }

    public static void hideRecentView(View view) {
        if (view == null || !Settings.HIDE_RECENT.get()) {
            return;
        }
        hideView(view);
    }

    private static void hideView(View view) {
        if (view == null) return;

        view.setVisibility(View.GONE);
        view.setPadding(0, 0, 0, 0);
        view.setMinimumWidth(0);
        view.setMinimumHeight(0);

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = 0;
            params.height = 0;

            if (params instanceof ViewGroup.MarginLayoutParams marginParams) {
                marginParams.setMargins(0, 0, 0, 0);
            }
            view.setLayoutParams(params);
        }

        if (view instanceof ViewGroup viewGroup) {
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                hideView(viewGroup.getChildAt(i));
            }
        }
    }
}
