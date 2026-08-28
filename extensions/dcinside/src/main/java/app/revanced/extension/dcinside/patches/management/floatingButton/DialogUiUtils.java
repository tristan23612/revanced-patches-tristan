package app.revanced.extension.dcinside.patches.management.floatingButton;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;

/**
 * 플로팅 버튼 다이얼로그 세션들에서 공통으로 쓰는 테마/색상/레이아웃/클립보드 유틸.
 * 상태를 갖지 않는 정적 헬퍼 모음이며, {@link DialogSession}과 별개로 단독 사용도 가능하다.
 */
@SuppressLint("DiscouragedApi")
public final class DialogUiUtils {

    private DialogUiUtils() {
    }

    public static int resolveDialogTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(
                context.getResources().getIdentifier("alertDialogTheme", "attr", context.getPackageName()),
                typedValue,
                true
        );
        return typedValue.resourceId;
    }

    public static int resolveDialogTextColor(Context context) {
        int dialogThemeResId = resolveDialogTheme(context);
        Context themedContext = new ContextThemeWrapper(context, dialogThemeResId);

        TypedValue typedValue = new TypedValue();
        themedContext.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);

        if (typedValue.resourceId != 0) {
            return themedContext.getResources().getColor(typedValue.resourceId, themedContext.getTheme());
        }
        return typedValue.data;
    }

    public static int applyAlpha(int color, int alpha) {
        // alpha: 0~255
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * 다이얼로그 본문에서 부가 설명용으로 쓰는 보조 텍스트 색상.
     * {@link #resolveDialogTextColor}에 알파를 입힌 값과 별개로,
     * 시스템의 textColorSecondary 속성을 직접 쓰고 싶을 때 사용한다.
     */
    public static int resolveSecondaryTextColor(Context context) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)) {
            return typedValue.resourceId != 0
                    ? context.getResources().getColor(typedValue.resourceId, context.getTheme())
                    : typedValue.data;
        }
        return 0xFF757575;
    }

    /**
     * dialogPreferredPadding 속성을 appcompat → framework → 기본값(16dp) 순으로 resolve.
     */
    public static int resolveDialogPreferredPadding(Context context) {
        TypedValue typedValue = new TypedValue();

        int appcompatAttrId = context.getResources().getIdentifier(
                "dialogPreferredPadding", "attr", context.getPackageName());
        if (appcompatAttrId != 0 &&
                context.getTheme().resolveAttribute(appcompatAttrId, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    typedValue.data, context.getResources().getDisplayMetrics());
        }

        if (context.getTheme().resolveAttribute(
                android.R.attr.dialogPreferredPadding, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    typedValue.data, context.getResources().getDisplayMetrics());
        }

        return (int) (16 * context.getResources().getDisplayMetrics().density);
    }

    public static Drawable createOutlineButtonBackground(int borderColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(2, borderColor);
        drawable.setCornerRadius(8);
        return drawable;
    }

    /**
     * 여러 View를 세로로 쌓고, 다이얼로그 좌우 여백(dialogPreferredPadding)을 적용한 컨테이너를 만든다.
     * 각 child가 이미 자체 LayoutParams(LinearLayout.LayoutParams)를 갖고 있으면 그대로 존중하고,
     * 없으면 MATCH_PARENT/WRAP_CONTENT + topMargin(padding/2)을 기본값으로 부여한다.
     */
    @NonNull
    public static LinearLayout wrapWithPadding(Context context, View... children) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        int padding = resolveDialogPreferredPadding(context);
        container.setPadding(padding, 0, padding, 0);

        for (View child : children) {
            ViewGroup.LayoutParams existingParams = child.getLayoutParams();
            boolean hasOwnParams = existingParams instanceof LinearLayout.LayoutParams;

            LinearLayout.LayoutParams params = hasOwnParams
                    ? (LinearLayout.LayoutParams) existingParams
                    : new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            if (!hasOwnParams) {
                params.topMargin = padding / 2;
            }

            container.addView(child, params);
        }

        return container;
    }

    /**
     * 화면 높이의 일정 비율을 넘지 않도록 onMeasure를 제한한 ListView를 생성한다.
     * heightRatio는 0.0~1.0 사이 값 (예: 0.5f → 화면 높이의 50%까지만).
     */
    @NonNull
    public static ListView createHeightLimitedListView(Context context, float heightRatio) {
        int maxHeightPx = (int) (context.getResources().getDisplayMetrics().heightPixels * heightRatio);

        return new ListView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int heightSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, heightSpec);
            }
        };
    }

    public static void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(context, "결과가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
    }
}