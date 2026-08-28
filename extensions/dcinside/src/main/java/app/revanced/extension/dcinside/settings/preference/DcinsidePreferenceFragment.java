package app.revanced.extension.dcinside.settings.preference;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toolbar;
import app.revanced.extension.dcinside.settings.DcinsideActivityHook;
import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.ResourceType;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.preference.ToolbarPreferenceFragment;

/**
 * Preference fragment for ReVanced settings.
 */
@SuppressWarnings("deprecation")
public class DcinsidePreferenceFragment extends ToolbarPreferenceFragment {
    /**
     * The main PreferenceScreen used to display the current set of preferences.
     */
    private PreferenceScreen preferenceScreen;

    /**
     * Initializes the preference fragment.
     */
    @Override
    protected void initialize() {
        super.initialize();

        try {
            preferenceScreen = getPreferenceScreen();
            Utils.sortPreferenceGroups(preferenceScreen);
            setPreferenceScreenToolbar(preferenceScreen);
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Called when the fragment starts.
     */
    @Override
    public void onStart() {
        super.onStart();
        try {
            // Initialize search controller if needed.
            if (DcinsideActivityHook.searchViewController != null) {
                // Trigger search data collection after fragment is ready.
                DcinsideActivityHook.searchViewController.initializeSearchData();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onStart failure", ex);
        }
    }

    /**
     * Sets the toolbar for all nested preference screens.
     */
    @Override
    protected void customizeToolbar(Toolbar toolbar) {
        DcinsideActivityHook.setToolbarLayoutParams(toolbar);
        toolbar.setBackgroundColor(DcinsideActivityHook.getToolbarBackgroundColorStatic());

        Integer textColor = DcinsideActivityHook.resolveThemeColorAttr(toolbar.getContext(), "dcToolbarTextColor");
        if (textColor != null) {
            toolbar.setTitleTextColor(textColor);
        }
    }

    @Override
    protected void customizeDialogBackground(ViewGroup rootView) {
        rootView.setVisibility(View.INVISIBLE);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        DcinsideActivityHook.finalizeNestedScreenDialog(rootView);
                    }
                });
    }

    @Override
    protected void onPostToolbarSetup(Toolbar toolbar, Dialog preferenceScreenDialog) {
        if (DcinsideActivityHook.searchViewController != null
                && DcinsideActivityHook.searchViewController.isSearchActive()) {
            toolbar.post(() -> DcinsideActivityHook.searchViewController.closeSearch());
        }
    }

    /**
     * Returns the preference screen for external access by SearchViewController.
     */
    public PreferenceScreen getPreferenceScreenForSearch() {
        return preferenceScreen;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public static Drawable getBackButtonDrawable() {
        final int backButtonResource = Utils.getResourceIdentifierOrThrow(
                ResourceType.DRAWABLE, "revanced_settings_toolbar_arrow_left");
        Drawable drawable = Utils.getContext().getResources().getDrawable(backButtonResource);
        DcinsideActivityHook.tintForCurrentTheme(drawable);
        return drawable;
    }
}