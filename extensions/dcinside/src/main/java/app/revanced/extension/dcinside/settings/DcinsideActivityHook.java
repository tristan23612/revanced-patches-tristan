package app.revanced.extension.dcinside.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.BaseActivityHook;
import app.revanced.extension.shared.ui.CustomDialog;
import app.revanced.extension.dcinside.settings.preference.DcinsidePreferenceFragment;
import app.revanced.extension.dcinside.settings.search.DcinsideSearchViewController;

@SuppressWarnings("deprecation")
public class DcinsideActivityHook extends BaseActivityHook {
    @SuppressLint("StaticFieldLeak")
    private static Activity currentActivity;

    @SuppressLint("StaticFieldLeak")
    public static DcinsideSearchViewController searchViewController;

    /**
     * Cache of resolved attr resource IDs, keyed by attr name.
     * Attr IDs never change at runtime, so this avoids repeated
     * {@link android.content.res.Resources#getIdentifier} lookups on every
     * toolbar/dialog customization.
     */
    private static final Map<String, Integer> attrIdCache = new HashMap<>();

    @SuppressWarnings("unused")
    public static void initialize(Activity parentActivity) {
        currentActivity = parentActivity;
        // Register the CustomDialog theme hook so every CustomDialog.create() call
        // (restart dialog, setting confirmation dialog, search history dialogs, etc.)
        // automatically picks up dcinside's theme attrs instead of the generic Utils colors.
        CustomDialog.themeApplier = DcinsideActivityHook::applyDialogTheme;
        BaseActivityHook.initialize(new DcinsideActivityHook(), parentActivity);
    }

    @Override
    protected void customizeActivityTheme(Activity activity) {
        // No-op: 디시인사이드 기본 적용 테마 유지
    }

    @Override
    protected int getToolbarBackgroundColor() {
        return getToolbarBackgroundColorStatic();
    }

    public static int getToolbarBackgroundColorStatic() {
        Integer color = resolveThemeColorAttr(currentActivity, "dcToolbarColor");
        if (color != null) {
            return color;
        }
        return Utils.getAppBackgroundColor();
    }

    @Override
    protected Drawable getNavigationIcon() {
        return DcinsidePreferenceFragment.getBackButtonDrawable();
    }

    public static void tintForCurrentTheme(Drawable drawable) {
        Integer textColor = resolveThemeColorAttr(currentActivity, "dcToolbarTextColor");
        if (textColor != null) {
            drawable.setTint(textColor);
        }
    }

    @Override
    protected View.OnClickListener getNavigationClickListener(Activity activity) {
        return v -> activity.onBackPressed();
    }

    @Override
    protected void onPostToolbarSetup(Activity activity, Toolbar toolbar, PreferenceFragment fragment) {
        Integer textColor = resolveThemeColorAttr(activity, "dcToolbarTextColor");
        if (textColor != null) {
            toolbar.setTitleTextColor(textColor);
            toolbar.setSubtitleTextColor(textColor);
        }

        if (fragment instanceof DcinsidePreferenceFragment) {
            searchViewController = DcinsideSearchViewController.addSearchViewComponents(activity, toolbar, (DcinsidePreferenceFragment) fragment);
        }

        paintStatusBarStrip(activity);
    }

    private void paintStatusBarStrip(Activity activity) {
        ViewGroup decorContent = activity.findViewById(android.R.id.content);

        View scrim = new View(activity);
        scrim.setBackgroundColor(getToolbarBackgroundColor());
        decorContent.addView(scrim, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0));

        decorContent.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarTop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? insets.getInsets(WindowInsets.Type.statusBars()).top
                    : 0;
            ViewGroup.LayoutParams lp = scrim.getLayoutParams();
            lp.height = statusBarTop;
            scrim.setLayoutParams(lp);
            return insets;
        });
        decorContent.requestApplyInsets();
    }

    @Override
    protected PreferenceFragment createPreferenceFragment() {
        return new DcinsidePreferenceFragment();
    }

    public static void finalizeNestedScreenDialog(ViewGroup rootView) {
        if (rootView.getChildCount() > 0 && rootView.getChildAt(0) instanceof Toolbar toolbar) {
            pushContentBelowToolbar(rootView, toolbar);
            Drawable navigationIcon = toolbar.getNavigationIcon();
            if (navigationIcon != null) {
                tintForCurrentTheme(navigationIcon);
            }
        }

        applyCurrentTheme(rootView);
        paintDialogStatusBarStrip(rootView);

        rootView.setVisibility(View.VISIBLE);
    }

    private static void paintDialogStatusBarStrip(ViewGroup rootView) {
        rootView.setClipToPadding(false);
        rootView.setClipChildren(false);

        int statusBarTop = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = rootView.getRootWindowInsets();
            if (insets != null) {
                statusBarTop = insets.getInsets(WindowInsets.Type.statusBars()).top;
            }
        }
        if (statusBarTop == 0) {
            return;
        }

        View scrim = new View(rootView.getContext());
        scrim.setBackgroundColor(getToolbarBackgroundColorStatic());

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, statusBarTop);
        lp.topMargin = -statusBarTop;

        rootView.addView(scrim, 0, lp);
    }

    private static void applyCurrentTheme(ViewGroup rootView) {
        if (currentActivity == null) {
            return;
        }

        Integer backgroundColor = resolveThemeColorAttr(currentActivity, "windowBackgroundColor");
        if (backgroundColor == null) {
            backgroundColor = resolveAndroidThemeColorAttr(currentActivity, android.R.attr.windowBackground);
        }
        if (backgroundColor != null) {
            rootView.setBackgroundColor(backgroundColor);
        }

        Integer textColor = resolveAndroidThemeColorAttr(currentActivity, android.R.attr.textColorPrimary);
        if (textColor != null) {
            tintTextViewsRecursively(rootView, textColor);
        }
    }

    private static void pushContentBelowToolbar(ViewGroup rootView, Toolbar toolbar) {
        if (rootView.getChildCount() < 2 || rootView.getChildAt(0) != toolbar) {
            return;
        }

        List<View> remainingChildren = new ArrayList<>();
        for (int i = 1; i < rootView.getChildCount(); i++) {
            remainingChildren.add(rootView.getChildAt(i));
        }

        rootView.removeAllViews();

        FrameLayout remainingContainer = new FrameLayout(rootView.getContext());
        for (View child : remainingChildren) {
            remainingContainer.addView(child, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        }

        LinearLayout stack = new LinearLayout(rootView.getContext());
        stack.setOrientation(LinearLayout.VERTICAL);

        stack.addView(toolbar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        stack.addView(remainingContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f));

        rootView.addView(stack, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private static void tintTextViewsRecursively(View view, int textColor) {
        if (view instanceof Toolbar) {
            return;
        }
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(textColor);
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                tintTextViewsRecursively(group.getChildAt(i), textColor);
            }
        }
    }

    /**
     * Theme hook registered on {@link CustomDialog#themeApplier}. Invoked for every dialog
     * built via {@link CustomDialog#create}, including ones triggered from shared code
     * (e.g. AbstractPreferenceFragment's restart/confirmation dialogs) that dcinside cannot
     * otherwise override, since CustomDialog's own colors come from generic Utils calls that
     * know nothing about dcinside's custom theme attrs.
     */
    public static void applyDialogTheme(Context context, LinearLayout mainLayout) {
        Integer bgColor = resolveThemeColorAttr(context, "windowBackgroundColor");
        if (bgColor == null) {
            bgColor = resolveAndroidThemeColorAttr(context, android.R.attr.windowBackground);
        }
        if (bgColor != null) {
            // mainLayout already has a rounded ShapeDrawable background set by CustomDialog;
            // recolor it in place instead of replacing the shape, to keep the rounded corners.
            Drawable background = mainLayout.getBackground();
            if (background instanceof ShapeDrawable shape) {
                shape.getPaint().setColor(bgColor);
            } else {
                mainLayout.setBackgroundColor(bgColor);
            }
        }

        Integer textColor = resolveAndroidThemeColorAttr(context, android.R.attr.textColorPrimary);
        Integer secondaryTextColor = resolveAndroidThemeColorAttr(context, android.R.attr.textColorSecondary);
        if (textColor != null) {
            applyTextColorToViewGroup(mainLayout, textColor, secondaryTextColor);
        }
    }

    /**
     * Recursively applies theme text colors to a ViewGroup. Shared between the CustomDialog
     * theme hook and the search overlay (moved here from DcinsideSearchViewController so both
     * can reuse the same logic).
     * <p>
     * Clickable TextViews (e.g. dialog buttons) are skipped to preserve their own fixed
     * background/text styling, such as CustomDialog's OK/Cancel button inversion.
     */
    public static void applyTextColorToViewGroup(ViewGroup group, int primaryColor, @Nullable Integer secondaryColor) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView tv) {
                if (tv.isClickable()) {
                    continue;
                }
                if (secondaryColor != null && (tv.getId() == android.R.id.summary || tv.getAlpha() < 1.0f)) {
                    tv.setTextColor(secondaryColor);
                } else {
                    tv.setTextColor(primaryColor);
                }
            } else if (child instanceof ViewGroup vg) {
                applyTextColorToViewGroup(vg, primaryColor, secondaryColor);
            }
        }
    }

    @SuppressWarnings("unused")
    public static boolean handleBackPress() {
        return DcinsideSearchViewController.handleFinish(searchViewController);
    }

    @SuppressLint("DiscouragedApi")
    @Nullable
    public static Integer resolveThemeColorAttr(Context context, String attrName) {
        if (context == null) {
            return null;
        }
        Integer attrId = attrIdCache.get(attrName);
        if (attrId == null) {
            attrId = context.getResources().getIdentifier(attrName, "attr", context.getPackageName());
            attrIdCache.put(attrName, attrId);
        }
        if (attrId == 0) {
            return null;
        }
        return resolveAndroidThemeColorAttr(context, attrId);
    }

    @Nullable
    public static Integer resolveAndroidThemeColorAttr(Context context, int attrId) {
        if (context == null) {
            return null;
        }
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrId, typedValue, true)) {
            return typedValue.data;
        }
        return null;
    }
}