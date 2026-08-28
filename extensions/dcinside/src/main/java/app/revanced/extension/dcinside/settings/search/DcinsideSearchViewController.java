package app.revanced.extension.dcinside.settings.search;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import app.revanced.extension.dcinside.settings.DcinsideActivityHook;
import app.revanced.extension.dcinside.settings.preference.DcinsidePreferenceFragment;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.search.BaseSearchResultItem;
import app.revanced.extension.shared.settings.search.BaseSearchResultsAdapter;
import app.revanced.extension.shared.settings.search.BaseSearchViewController;
import app.revanced.extension.shared.ui.CustomDialog;
import app.revanced.extension.shared.ui.Dim;

/**
 * Dcinside-specific search view controller implementation.
 */
@SuppressWarnings("deprecation")
public class DcinsideSearchViewController extends BaseSearchViewController {

    public static DcinsideSearchViewController addSearchViewComponents(Activity activity, Toolbar toolbar, DcinsidePreferenceFragment fragment) {
        return new DcinsideSearchViewController(activity, toolbar, fragment);
    }

    private DcinsideSearchViewController(Activity activity, Toolbar toolbar, DcinsidePreferenceFragment fragment) {
        super(activity, toolbar, new PreferenceFragmentAdapter(fragment));
        applyDcinsideBackgrounds();

        applyDcinsideSearchCursorColor();
    }

    /**
     * Overrides the search view and overlay backgrounds set by the shared
     * controller (which use generic Utils colors) with dcinside's own
     * windowBackgroundColor theme attribute.
     */
    private void applyDcinsideBackgrounds() {
        Integer windowBackgroundColor = DcinsideActivityHook.resolveThemeColorAttr(
                activity, "windowBackgroundColor");
        if (windowBackgroundColor == null) {
            windowBackgroundColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(
                    activity, android.R.attr.windowBackground);
        }
        if (windowBackgroundColor == null) {
            return;
        }

        overlayContainer.setBackgroundColor(windowBackgroundColor);

        GradientDrawable searchBackground = new GradientDrawable();
        searchBackground.setShape(GradientDrawable.RECTANGLE);
        searchBackground.setCornerRadius(searchView.getBackground() instanceof GradientDrawable existing
                ? existing.getCornerRadius()
                : 0);
        searchBackground.setColor(windowBackgroundColor);
        searchView.setBackground(searchBackground);

        applyDcinsideSearchTextColors();
    }

    private void applyDcinsideSearchTextColors() {
        Integer textColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(
                activity, android.R.attr.textColorPrimary);
        if (textColor == null) {
            return;
        }

        EditText searchEditText = searchView.findViewById(
                Utils.getResourceIdentifierOrThrow(null, "android:id/search_src_text"));
        searchEditText.setTextColor(textColor);
        searchEditText.setHintTextColor(Utils.adjustColorBrightness(textColor, 0.6f));
    }

    @Override
    protected void showSearchHistory() {
        super.showSearchHistory();
        applyDcinsideOverlayTextColors();
        setupSearchHistoryDialogs();
    }

    private void applyDcinsideOverlayTextColors() {
        Integer primaryColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(activity, android.R.attr.textColorPrimary);
        Integer secondaryColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(activity, android.R.attr.textColorSecondary);
        if (primaryColor == null) return;

        DcinsideActivityHook.applyTextColorToViewGroup(overlayContainer, primaryColor, secondaryColor);
    }

    /**
     * 개별 및 전체 검색 기록 삭제 클릭 시 CustomDialog 구조를 유지하며 테마를 적용합니다.
     * 테마 적용은 {@link CustomDialog#themeApplier} 훅을 통해 자동으로 처리되므로,
     * 여기서는 다이얼로그 생성 및 액션 바인딩만 담당합니다.
     */
    private void setupSearchHistoryDialogs() {
        // 1. 전체 검색 기록 삭제 버튼 핸들러
        View clearAllButton = overlayContainer.findViewById(
                Utils.getResourceIdentifierOrThrow(ResourceType.ID, "clear_history_button")
        );
        if (clearAllButton != null) {
            clearAllButton.setOnClickListener(v -> CustomDialog.create(
                    activity,
                    "검색 기록 삭제",
                    "전체 검색 기록을 삭제하시겠습니까?",
                    null,
                    null,
                    () -> {
                        searchHistoryManager.clearAllSearchHistory();
                        showSearchHistory();
                    },
                    () -> {},
                    null,
                    null,
                    true
            ).first.show());
        }

        // 2. 개별 검색 기록 항목 삭제 아이콘 클릭 리스너 재바인딩
        LinearLayout historyListView = overlayContainer.findViewById(
                Utils.getResourceIdentifierOrThrow(ResourceType.ID, "search_history_list")
        );
        if (historyListView != null) {
            int deleteIconId = Utils.getResourceIdentifierOrThrow(ResourceType.ID, "delete_icon");
            int historyTextId = Utils.getResourceIdentifierOrThrow(ResourceType.ID, "history_text");

            for (int i = 0; i < historyListView.getChildCount(); i++) {
                View itemView = historyListView.getChildAt(i);
                ImageView deleteIcon = itemView.findViewById(deleteIconId);
                TextView historyTextView = itemView.findViewById(historyTextId);

                if (deleteIcon != null && historyTextView != null) {
                    String query = historyTextView.getText().toString();
                    // 동적으로 생성된 개별 삭제 버튼의 기존 클릭 이벤트 덮어쓰기
                    deleteIcon.setOnClickListener(v -> CustomDialog.create(
                            activity,
                            query,
                            "검색 기록에서 삭제하시겠습니까?",
                            null,
                            null,
                            () -> {
                                searchHistoryManager.removeSearchQuery(query);
                                showSearchHistory(); // UI 갱신 및 재바인딩
                            },
                            () -> {},
                            null,
                            null,
                            true
                    ).first.show());
                }
            }
        }
    }

    @Override
    protected BaseSearchResultsAdapter createSearchResultsAdapter() {
        return new DcinsideSearchResultsAdapter(activity, filteredSearchItems, fragment, this);
    }

    @Override
    protected boolean isSpecialPreferenceGroup(Preference preference) {
        return false;
    }

    @Override
    protected void setupSpecialPreferenceListeners(BaseSearchResultItem item) {
    }

    @Override
    protected void setupToolbarMenu() {
        super.setupToolbarMenu();
        MenuItem search = toolbar.getMenu().findItem(ID_ACTION_SEARCH);
        Drawable icon = search.getIcon();
        if (icon != null) {
            DcinsideActivityHook.tintForCurrentTheme(icon);
        }
    }

    @Override
    protected void openSearch() {
        applyDcinsideSearchCursorColor();
        super.openSearch();
    }

    private void applyDcinsideSearchCursorColor() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        Integer textColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(activity, android.R.attr.textColorPrimary);
        if (textColor == null) return;

        EditText searchEditText = searchView.findViewById(Utils.getResourceIdentifierOrThrow(null, "android:id/search_src_text"));
        if (searchEditText == null) return;

        // 기존 시스템 Cursor Drawable을 획득하여 Tint 적용
        Drawable cursorDrawable = searchEditText.getTextCursorDrawable();
        if (cursorDrawable != null) {
            Drawable mutated = cursorDrawable.mutate();
            mutated.setTint(textColor);
            searchEditText.setTextCursorDrawable(mutated);
        } else {
            // Fallback: 신규 생성 시 높이값(-1) 대신 명시적 LineHeight 설정
            GradientDrawable newCursor = new GradientDrawable();
            newCursor.setShape(GradientDrawable.RECTANGLE);
            newCursor.setSize(Dim.dp2, searchEditText.getLineHeight());
            newCursor.setColor(textColor);
            searchEditText.setTextCursorDrawable(newCursor);
        }
    }

    // Static method for Activity finish.
    public static boolean handleFinish(DcinsideSearchViewController searchViewController) {
        if (searchViewController != null && searchViewController.isSearchActive()) {
            searchViewController.closeSearch();
            return true;
        }
        return false;
    }

    // Adapter to wrap DcinsidePreferenceFragment to BasePreferenceFragment interface.
    private record PreferenceFragmentAdapter(DcinsidePreferenceFragment fragment) implements BasePreferenceFragment {
        @Override
        public PreferenceScreen getPreferenceScreenForSearch() {
            return fragment.getPreferenceScreenForSearch();
        }

        @Override
        public View getView() {
            return fragment.getView();
        }

        @Override
        public Activity getActivity() {
            return fragment.getActivity();
        }
    }
}