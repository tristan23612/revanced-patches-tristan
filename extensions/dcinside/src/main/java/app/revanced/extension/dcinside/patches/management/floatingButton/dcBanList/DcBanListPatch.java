package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import android.view.View;
import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import app.revanced.extension.dcinside.settings.Settings;

public final class DcBanListPatch {
    private DcBanListPatch() {
    }

    public static void setDcBanListButtonVisibility(View targetView, boolean visible, String dcBanListButtonIdName) {
        if (targetView == null) return;

        View rootView = targetView.getRootView();
        int resId = rootView.getContext().getResources().getIdentifier(
                dcBanListButtonIdName,
                "id",
                rootView.getContext().getPackageName()
        );

        View dcBanListButton = rootView.findViewById(resId);
        if (dcBanListButton != null) {
            dcBanListButton.setOnClickListener(buttonView ->
                    new DcBanListSession(buttonView.getContext()).start());
            dcBanListButton.setVisibility(
                    visible && Settings.SHOW_DC_BAN_LIST_BUTTON.get() && JsonHookPatch.managerSkill
                            ? View.VISIBLE
                            : View.GONE
            );
        }
    }
}