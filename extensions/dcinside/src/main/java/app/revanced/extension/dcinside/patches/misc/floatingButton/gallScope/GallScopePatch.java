package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import app.revanced.extension.dcinside.settings.Settings;

@SuppressLint({"DiscouragedApi", "SetTextI18n"})
public class GallScopePatch {
    private static final int BATCH_SIZE = 5;

    public static void setGallScopeButtonVisibility(View targetView, boolean visible, String gallScopeButtonIdName) {
        if (targetView == null) return;

        View rootView = targetView.getRootView();
        int resId = rootView.getContext().getResources().getIdentifier(
                gallScopeButtonIdName, "id", rootView.getContext().getPackageName());

        View gallScopeButton = rootView.findViewById(resId);
        if (gallScopeButton != null) {
            gallScopeButton.setOnClickListener(buttonView -> new GallScopeSession(buttonView.getContext(), null).start());
            gallScopeButton.setVisibility(visible && Settings.SHOW_GALL_SCOPE_BUTTON.get() ? View.VISIBLE : View.GONE);
        }
    }

    public static void setupGallScopeButton(View view, String gallScopeButtonIdName) {
        if (view == null) return;

        int resId = view.getContext().getResources().getIdentifier(
                gallScopeButtonIdName, "id", view.getContext().getPackageName());

        View button = view.findViewById(resId);
        if (button != null) {
            button.setOnClickListener(buttonView -> new GallScopeSession(buttonView.getContext(), null).start());
        }
    }

    /**
     * 게시글 화면 등 외부에서 식별코드를 이미 알고 있을 때 바로 페이지 범위 입력부터 시작
     */
    public static void showGallScopeDialogWithUserId(Context context, String prefillUserId) {
        new GallScopeSession(context, prefillUserId).start();
    }

    private static int resolveDialogTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(
                context.getResources().getIdentifier("alertDialogTheme", "attr", context.getPackageName()),
                typedValue,
                true
        );
        return typedValue.resourceId;
    }

    private static class GallScopeSession {
        private enum Step {
            IDENTIFIER_INPUT,
            PAGE_RANGE_INPUT,
            PARSING,
            RESULT,
            ERROR
        }

        private final Context context;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private Step currentStep;
        private AlertDialog currentDialog;

        private String targetUserId;
        private int firstSearchedPage = -1;
        private int startPage;
        private int endPage;
        private int rangeSize;

        private final List<JSONObject> resultsList = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean rangeExceeded = false;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private final AtomicInteger lastValidPage = new AtomicInteger(0);
        private AtomicInteger completedPages;
        private AtomicInteger nextPageToLaunch;
        private AtomicInteger activeRequests;
        private int totalPagesInBatch;

        private GallScopeSession(Context context, String prefillUserId) {
            this.context = context;
            if (prefillUserId != null && !prefillUserId.trim().isEmpty()) {
                this.targetUserId = prefillUserId.trim();
                this.currentStep = Step.PAGE_RANGE_INPUT;
            } else {
                this.currentStep = Step.IDENTIFIER_INPUT;
            }
        }

        private void start() {
            renderStep();
        }

        private void renderStep() {
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }

            int dialogThemeResId = resolveDialogTheme(context);
            AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogThemeResId)
                    .setTitle("갤스코프")
                    .setCancelable(false);

            String errorMessage = "요청 처리 중 오류가 발생했습니다.";

            switch (currentStep) {
                case IDENTIFIER_INPUT -> {
                    TextView message = new TextView(context);
                    message.setText("검색할 유저의 식별코드를 입력하세요.");

                    EditText input = new EditText(context);
                    input.setHint("식별코드 또는 IP 입력");
                    input.setHintTextColor(0xFF9E9E9E);
                    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                    input.setGravity(android.view.Gravity.CENTER);

                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    int rowHeightPx = (int) (36 * context.getResources().getDisplayMetrics().density);

                    LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, rowHeightPx, 2f);
                    LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, rowHeightPx, 1f);

                    row.addView(new View(context), spacerParams);
                    row.addView(input, inputParams);
                    row.addView(new View(context), spacerParams);

                    LinearLayout container = wrapWithPadding(message, row);

                    builder.setView(container)
                            .setPositiveButton("다음", (d, w) -> {
                                String id = input.getText().toString().trim();
                                if (id.isEmpty()) {
                                    Toast.makeText(context, "식별코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                    transitionTo(Step.IDENTIFIER_INPUT);
                                    return;
                                }
                                targetUserId = id;
                                transitionTo(Step.PAGE_RANGE_INPUT);
                            })
                            .setNegativeButton("취소", null);
                }

                case PAGE_RANGE_INPUT -> {
                    TextView message = new TextView(context);
                    message.setText("검색할 페이지 범위를 입력하세요.\n(" + targetUserId + ")");

                    EditText startInput = new EditText(context);
                    startInput.setHint("시작 페이지");
                    startInput.setHintTextColor(0xFF9E9E9E);
                    startInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    startInput.setGravity(android.view.Gravity.CENTER);
                    startInput.setText(String.valueOf(endPage > 0 ? endPage + 1 : 1));

                    EditText endInput = new EditText(context);
                    endInput.setHint("끝 페이지");
                    endInput.setHintTextColor(0xFF9E9E9E);
                    endInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    endInput.setGravity(android.view.Gravity.CENTER);
                    endInput.setText(String.valueOf(
                            (endPage > 0 ? endPage + 1 : 1) + (rangeSize > 0 ? rangeSize - 1 : 9)));

                    TextView tilde = new TextView(context);
                    tilde.setText("~");
                    tilde.setGravity(android.view.Gravity.CENTER);
                    tilde.setTextSize(16);

                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    int rowHeightPx = (int) (36 * context.getResources().getDisplayMetrics().density);

                    LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, rowHeightPx, 2f);
                    LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, rowHeightPx, 1f);
                    LinearLayout.LayoutParams tildeParams = new LinearLayout.LayoutParams(0, rowHeightPx, 1f);

                    row.addView(new View(context), spacerParams);
                    row.addView(startInput, inputParams);
                    row.addView(tilde, tildeParams);
                    row.addView(endInput, inputParams);
                    row.addView(new View(context), spacerParams);

                    LinearLayout container = wrapWithPadding(message, row);

                    builder.setView(container)
                            .setPositiveButton("검색 시작", (d, w) -> {
                                int startPage, endPage;
                                try {
                                    startPage = Integer.parseInt(startInput.getText().toString().trim());
                                    endPage = Integer.parseInt(endInput.getText().toString().trim());
                                } catch (NumberFormatException ex) {
                                    Toast.makeText(context, "올바른 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                    transitionTo(Step.PAGE_RANGE_INPUT);
                                    return;
                                }
                                if (startPage <= 0 || endPage <= 0 || startPage > endPage) {
                                    Toast.makeText(context, "페이지 범위가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                                    transitionTo(Step.PAGE_RANGE_INPUT);
                                    return;
                                }
                                this.startPage = startPage;
                                this.endPage = endPage;
                                rangeSize = endPage - startPage + 1;
                                if (firstSearchedPage == -1) {
                                    firstSearchedPage = startPage;
                                }
                                transitionTo(Step.PARSING);
                            })
                            .setNegativeButton("취소", null);
                }

                case PARSING -> {
                    builder.setMessage(progressText(0, rangeSize));
                    startBatchFetch();
                }

                case RESULT -> {
                    List<JSONObject> snapshot;
                    synchronized (resultsList) {
                        snapshot = new ArrayList<>(resultsList);
                    }

                    int displayEndPage = Math.min(endPage, lastValidPage.get());

                    TextView message = new TextView(context);
                    String rangeText = firstSearchedPage + "~" + displayEndPage + "페이지";
                    if (rangeExceeded) {
                        rangeText += " (마지막 페이지 도달)";
                    }
                    message.setText(targetUserId + " 스코프 결과\n" + snapshot.size() + "건 (" + rangeText + ")");

                    ListView listView = getListView(snapshot);
                    LinearLayout container = wrapWithPadding(message, listView);

                    builder.setView(container)
                            .setNeutralButton("복사", (d, w) -> copyResultsToClipboard(snapshot));

                    if (!rangeExceeded) {
                        builder.setPositiveButton("계속 검색", (d, w) -> {
                            startPage = lastValidPage.get() + 1;
                            endPage = startPage + rangeSize - 1;
                            transitionTo(Step.PARSING);
                        });
                    }

                    builder.setNegativeButton("닫기", null);
                }

                case ERROR -> builder
                        .setMessage(errorMessage)
                        .setPositiveButton("확인", null);
            }

            currentDialog = builder.create();
            currentDialog.show();
        }

        @NonNull
        private ListView getListView(List<JSONObject> snapshot) {
            int maxHeightPx = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.5f);

            ListView listView = new ListView(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int heightSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST);
                    super.onMeasure(widthMeasureSpec, heightSpec);
                }
            };

            listView.setAdapter(new ArrayAdapter<JSONObject>(context, 0, snapshot) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    LinearLayout row;
                    TextView titleView;
                    TextView subView;

                    if (convertView == null) {
                        row = new LinearLayout(context);
                        row.setOrientation(LinearLayout.VERTICAL);
                        int padding = (int) (8 * context.getResources().getDisplayMetrics().density);
                        row.setPadding(padding, padding, padding, padding);

                        titleView = new TextView(context);
                        titleView.setSingleLine(true);
                        titleView.setEllipsize(TextUtils.TruncateAt.END);
                        titleView.setTextSize(16);

                        subView = new TextView(context);
                        subView.setTextSize(13);
                        subView.setTextColor(0xFF9E9E9E);

                        row.addView(titleView);
                        row.addView(subView);
                        row.setTag(new View[]{titleView, subView});
                    } else {
                        row = (LinearLayout) convertView;
                        View[] tag = (View[]) row.getTag();
                        titleView = (TextView) tag[0];
                        subView = (TextView) tag[1];
                    }

                    JSONObject item = getItem(position);
                    String title = item != null ? item.optString("title", "") : "";
                    String reply = item != null ? item.optString("replyCount", "0") : "0";

                    String date = item != null ? item.optString("date", "") : "";
                    String views = item != null ? item.optString("views", "") : "";
                    String recommend = item != null ? item.optString("recommend", "") : "";

                    titleView.setText(title + "  [" + reply + "]");
                    subView.setText(date + ", 조회: " + views + ", 추천: " + recommend);

                    return row;
                }
            });

            listView.setOnItemClickListener((parent, view, position, id) -> {
                JSONObject item = snapshot.get(position);
                String postNo = item.optString("postNo", "");
                if (postNo.isEmpty()) return;

                try {
                    int postNoInt = Integer.parseInt(postNo);
                    Intent intent = new Intent();
                    intent.setClassName(context.getPackageName(), "com.dcinside.app.PostReadActivity");
                    intent.putExtra("com.dcinside.app.extra.GALLERY_ID", JsonHookPatch.galleryId);
                    intent.putExtra("com.dcinside.app.extra.POST_NUMBER", postNoInt);
                    context.startActivity(intent);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "게시글 번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "게시글을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });

            return listView;
        }

        private LinearLayout wrapWithPadding(View... children) {
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

        private static int resolveDialogPreferredPadding(Context context) {
            TypedValue typedValue = new TypedValue();

            // AppCompat 테마가 정의하는 attr 우선 시도
            int appcompatAttrId = context.getResources().getIdentifier(
                    "dialogPreferredPadding", "attr", context.getPackageName());
            if (appcompatAttrId != 0 &&
                    context.getTheme().resolveAttribute(appcompatAttrId, typedValue, true)) {
                return TypedValue.complexToDimensionPixelSize(
                        typedValue.data, context.getResources().getDisplayMetrics());
            }

            // 프레임워크 기본 attr로 폴백
            if (context.getTheme().resolveAttribute(
                    android.R.attr.dialogPreferredPadding, typedValue, true)) {
                return TypedValue.complexToDimensionPixelSize(
                        typedValue.data, context.getResources().getDisplayMetrics());
            }

            // 최후 폴백 (attr 자체가 없는 극히 드문 경우)
            return (int) (16 * context.getResources().getDisplayMetrics().density);
        }

        private void transitionTo(Step nextStep) {
            this.currentStep = nextStep;
            mainHandler.post(this::renderStep);
        }

        private String progressText(int completed, int total) {
            return "게시글을 검색하고 있습니다...\n(" + completed + " / " + total + " 페이지)";
        }

        private void updateProgressMessageInPlace(int completed, int total) {
            mainHandler.post(() -> {
                if (currentDialog != null && currentDialog.isShowing()) {
                    currentDialog.setMessage(progressText(completed, total));
                }
            });
        }

        /**
         * startPage~endPage 범위를 슬라이딩 윈도우 방식(BATCH_SIZE 동시 요청)으로 병렬 파싱.
         */
        private void startBatchFetch() {
            totalPagesInBatch = endPage - startPage + 1;
            completedPages = new AtomicInteger(0);
            nextPageToLaunch = new AtomicInteger(startPage);
            activeRequests = new AtomicInteger(0);

            int initial = Math.min(BATCH_SIZE, totalPagesInBatch);
            for (int i = 0; i < initial; i++) {
                launchNextPage();
            }
        }

        private void launchNextPage() {
            if (rangeExceeded) return;
            int page = nextPageToLaunch.getAndIncrement();
            if (page > endPage) return;

            activeRequests.incrementAndGet();

            GallScopeApiClient.fetchListPage(
                    JsonHookPatch.galleryType,
                    JsonHookPatch.galleryId,
                    page,
                    new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                            onPageDone();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {
                            try (response) {
                                if (response.isSuccessful()) {
                                    String html = response.body().string();
                                    Document doc = Jsoup.parse(html);

                                    if (GallScopeHtmlParser.isPageOutOfRange(doc, page)) {
                                        rangeExceeded = true;
                                    } else {
                                        lastValidPage.updateAndGet(v -> Math.max(v, page));
                                        JSONArray pageResults = GallScopeHtmlParser.parseAndFilterByUserId(doc, targetUserId);
                                        for (int i = 0; i < pageResults.length(); i++) {
                                            resultsList.add(pageResults.getJSONObject(i));
                                        }
                                    }
                                }
                            } catch (java.io.IOException | JSONException ignored) {
                                // 개별 페이지 파싱 실패는 무시하고 다음 페이지로 진행
                            }
                            onPageDone();
                        }
                    }
            );
        }

        private void onPageDone() {
            activeRequests.decrementAndGet();
            int done = completedPages.incrementAndGet();
            updateProgressMessageInPlace(done, totalPagesInBatch);

            if (!rangeExceeded && nextPageToLaunch.get() <= endPage) {
                mainHandler.postDelayed(this::launchNextPage, 500);
            } else if (activeRequests.get() == 0) {
                if (finished.compareAndSet(false, true)) {
                    sortResultsByPostNoDesc();
                    transitionTo(Step.RESULT);
                }
            }
        }

        private void sortResultsByPostNoDesc() {
            synchronized (resultsList) {
                resultsList.sort((a, b) -> {
                    int postNoA = a.optInt("postNo", 0);
                    int postNoB = b.optInt("postNo", 0);
                    return Integer.compare(postNoB, postNoA); // 내림차순
                });
            }
        }

        private void copyResultsToClipboard(List<JSONObject> snapshot) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(targetUserId).append(" 스코프 결과\n");
            stringBuilder.append(snapshot.size()).append("개의 글을 찾았습니다.\n");
            for (JSONObject item : snapshot) {
                stringBuilder.append(item.optString("date", "")).append("\t")
                        .append(item.optString("title", "")).append("\t")
                        .append(item.optString("url", "")).append("\n");
            }

            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("GallScope Results", stringBuilder.toString()));
            Toast.makeText(context, "결과가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}