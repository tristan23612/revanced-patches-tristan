package app.revanced.extension.dcinside.patches.management.post.duplicatePost;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import app.revanced.extension.dcinside.settings.Settings;

@SuppressLint("DiscouragedApi")
public final class DuplicatePostSearchPatch {
    private DuplicatePostSearchPatch() {
    }

    public static void setupSearchButton(View view) {
        if (view == null) return;

        View rootView = view.getRootView();

        int buttonId = rootView.getContext().getResources().getIdentifier(
                "revanced_duplicate_post_search_button", "id", rootView.getContext().getPackageName());
        int subjectId = rootView.getContext().getResources().getIdentifier(
                "read_header_subject", "id", rootView.getContext().getPackageName());
        if (buttonId == 0 || subjectId == 0) return;

        View button = rootView.findViewById(buttonId);
        TextView subjectView = rootView.findViewById(subjectId);
        if (button == null || subjectView == null) return;

        boolean enabled = Settings.SHOW_DUPLICATE_POST_SEARCH_BUTTON.get();
        button.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) return;

        button.setOnClickListener(v -> {
            String title = subjectView.getText().toString();
            if (title.startsWith("[")) {
                int endIndex = title.indexOf("]");
                if (endIndex > 0) {
                    title = title.substring(endIndex + 2).trim();
                }
            }
            new DuplicatePostSearchSession(v.getContext(), title).start();
        });
    }
}