package app.revanced.extension.dcinside.settings.search;

import android.content.Context;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;
import app.revanced.extension.dcinside.settings.DcinsideActivityHook;
import app.revanced.extension.shared.settings.search.BaseSearchResultItem;
import app.revanced.extension.shared.settings.search.BaseSearchResultsAdapter;
import app.revanced.extension.shared.settings.search.BaseSearchViewController;

import java.util.List;

/**
 * Dcinside-specific search results adapter.
 */
@SuppressWarnings("deprecation")
public class DcinsideSearchResultsAdapter extends BaseSearchResultsAdapter {

    public DcinsideSearchResultsAdapter(Context context, List<BaseSearchResultItem> items,
                                       BaseSearchViewController.BasePreferenceFragment fragment,
                                       BaseSearchViewController searchViewController) {
        super(context, items, fragment, searchViewController);
    }

    @Override
    protected PreferenceScreen getMainPreferenceScreen() {
        return fragment.getPreferenceScreenForSearch();
    }

    @Override
    protected void bindDataToViewHolder(BaseSearchResultItem item, Object holder,
                                        BaseSearchResultItem.ViewType viewType, View view) {
        super.bindDataToViewHolder(item, holder, viewType, view);

        Integer primaryColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(getContext(), android.R.attr.textColorPrimary);
        Integer secondaryColor = DcinsideActivityHook.resolveAndroidThemeColorAttr(getContext(), android.R.attr.textColorSecondary);
        Integer accentColor = DcinsideActivityHook.resolveThemeColorAttr(getContext(), "colorAccent");
        if (accentColor == null) {
            accentColor = primaryColor;
        }

        // 카테고리 제목(Group Header) 텍스트 색상 적용
        if (viewType == BaseSearchResultItem.ViewType.GROUP_HEADER) {
            TextView pathView = view.findViewById(ID_PREFERENCE_PATH);
            if (pathView != null && accentColor != null) {
                pathView.setTextColor(accentColor);
            }
        } else {
            // 일반 검색 결과 항목 타이틀 및 서머리 텍스트 색상 적용
            TextView titleView = view.findViewById(ID_PREFERENCE_TITLE);
            TextView summaryView = view.findViewById(ID_PREFERENCE_SUMMARY);
            if (titleView != null && primaryColor != null) {
                titleView.setTextColor(primaryColor);
            }
            if (summaryView != null && secondaryColor != null) {
                summaryView.setTextColor(secondaryColor);
            }
        }
    }
}
