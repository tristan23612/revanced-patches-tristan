package app.revanced.extension.dcinside.patches.management.floatingButton.gallScope;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import app.revanced.extension.dcinside.patches.hook.okhttp.CustomNetworkInterceptorPatch;
import app.revanced.extension.dcinside.patches.management.DialogSession;
import app.revanced.extension.dcinside.patches.management.DialogUiUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 갤스코프 다이얼로그 상태 머신. 실제 fetch/파싱 로직은 SearchStrategy 구현체(PostSearchStrategy,
 * CommentSearchStrategy)에 위임하고, 이 클래스는 UI 스텝 전환과 결과 표시만 담당한다.
 */
@SuppressLint({"DiscouragedApi", "SetTextI18n"})
final class GallScopeSession extends DialogSession<GallScopeSession.Step> {

    private static final String TAG = "ReVanced_DCInside";

    enum Step {
        IDENTIFIER_INPUT,
        SEARCH_MODE_SELECTION,
        PAGE_RANGE_INPUT,
        PARSING,
        RESULT,
        ERROR
    }

    private enum SearchMode {
        POST("게시글"), COMMENT("댓글");

        private final String label;

        SearchMode(String label) {
            this.label = label;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    private String targetUserId;
    private String galleryType;
    private String galleryId;
    private SearchMode searchMode;

    private int firstSearchedPage = -1;
    private int startPage;
    private int endPage;
    private int rangeSize;

    // RESULT 화면 표시/누적용 - 전략 실행 결과를 세션이 보관
    private final List<JSONObject> resultsList = new ArrayList<>();
    private boolean rangeExceeded = false;
    private int lastValidPage = 0;

    // 전략 인스턴스는 세션 생명주기 동안 유지 (댓글 모드는 "계속 검색" 시 내부 상태 이어받기 필요)
    private final PostSearchStrategy postSearchStrategy = new PostSearchStrategy();
    private final CommentSearchStrategy commentSearchStrategy = new CommentSearchStrategy();

    GallScopeSession(Context context, String prefillUserId, String prefillGalleryType, String prefillGalleryId) {
        super(context, resolveInitialStep(prefillUserId, prefillGalleryType, prefillGalleryId));

        if (currentStep == Step.SEARCH_MODE_SELECTION) {
            this.targetUserId = prefillUserId.trim();
            this.galleryType = prefillGalleryType.trim();
            this.galleryId = prefillGalleryId.trim();
        }
    }

    private static Step resolveInitialStep(String prefillUserId, String prefillGalleryType, String prefillGalleryId) {
        boolean hasPrefill = prefillUserId != null && !prefillUserId.trim().isEmpty()
                && prefillGalleryType != null && !prefillGalleryType.trim().isEmpty()
                && prefillGalleryId != null && !prefillGalleryId.trim().isEmpty();
        return hasPrefill ? Step.SEARCH_MODE_SELECTION : Step.IDENTIFIER_INPUT;
    }

    @Override
    protected String getDialogTitle() {
        return "갤스코프";
    }

    @Override
    protected void buildStep(AlertDialog.Builder builder, Step step) {
        switch (step) {
            case IDENTIFIER_INPUT -> {
                TextView message = new TextView(context);
                message.setText("검색할 유저의 식별코드를 입력하세요.");

                EditText input = new EditText(context);
                input.setHint("식별코드 또는 IP 입력");
                input.setHintTextColor(secondaryColor);
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

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, row);

                builder.setView(container)
                        .setPositiveButton("다음", (d, w) -> {
                            String targetUserId = input.getText().toString().trim();
                            if (targetUserId.isEmpty()) {
                                Toast.makeText(context, "식별코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                transitionTo(Step.IDENTIFIER_INPUT);
                                return;
                            }
                            this.targetUserId = targetUserId;
                            galleryId = CustomNetworkInterceptorPatch.galleryId;
                            galleryType = JsonHookPatch.galleryType;
                            transitionTo(Step.SEARCH_MODE_SELECTION);
                        })
                        .setNegativeButton("취소", null);
            }

            case SEARCH_MODE_SELECTION -> {
                TextView message = new TextView(context);
                message.setText("검색 방식을 선택하세요.\n(" + targetUserId + ")");

                Button postButton = new Button(context);
                postButton.setText("게시글 검색");
                postButton.setOnClickListener(v -> {
                    searchMode = SearchMode.POST;
                    currentDialog.dismiss();
                    transitionTo(Step.PAGE_RANGE_INPUT);
                });
                postButton.setBackground(DialogUiUtils.createOutlineButtonBackground(secondaryColor));
                postButton.setTextColor(textColor);

                Button commentButton = new Button(context);
                commentButton.setText("댓글 검색");
                commentButton.setOnClickListener(v -> {
                    searchMode = SearchMode.COMMENT;
                    commentSearchStrategy.reset();
                    currentDialog.dismiss();
                    transitionTo(Step.PAGE_RANGE_INPUT);
                });
                commentButton.setBackground(DialogUiUtils.createOutlineButtonBackground(secondaryColor));
                commentButton.setTextColor(textColor);

                LinearLayout buttonRow = new LinearLayout(context);
                buttonRow.setOrientation(LinearLayout.HORIZONTAL);

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                int buttonMargin = (int) (4 * context.getResources().getDisplayMetrics().density);

                LinearLayout.LayoutParams postButtonParams = new LinearLayout.LayoutParams(buttonParams);
                postButtonParams.rightMargin = buttonMargin;
                LinearLayout.LayoutParams commentButtonParams = new LinearLayout.LayoutParams(buttonParams);
                commentButtonParams.leftMargin = buttonMargin;

                buttonRow.addView(postButton, postButtonParams);
                buttonRow.addView(commentButton, commentButtonParams);

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, buttonRow);

                builder.setView(container)
                        .setNegativeButton("취소", null);
            }

            case PAGE_RANGE_INPUT -> {
                TextView message = new TextView(context);
                String rangeLabel = "검색할 페이지 범위를 입력하세요.\n(" + targetUserId + "의 " + searchMode + ")";
                message.setText(rangeLabel);

                EditText startInput = new EditText(context);
                startInput.setHint("시작 페이지");
                startInput.setHintTextColor(secondaryColor);
                startInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                startInput.setGravity(android.view.Gravity.CENTER);
                startInput.setText(String.valueOf(endPage > 0 ? endPage + 1 : 1));

                EditText endInput = new EditText(context);
                endInput.setHint("끝 페이지");
                endInput.setHintTextColor(secondaryColor);
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

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, row);

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
                                Toast.makeText(context, "범위가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
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
                runSearch();
            }

            case RESULT -> {
                List<JSONObject> snapshot = new ArrayList<>(resultsList);

                int displayEndPage = Math.min(endPage > 0 ? endPage : lastValidPage, lastValidPage);

                TextView message = new TextView(context);
                String rangeText = firstSearchedPage + "~" + displayEndPage + "페이지";
                if (rangeExceeded) {
                    rangeText += " (마지막 페이지 도달)";
                }
                message.setText(targetUserId + " " + searchMode + " 스코프 결과\n" + snapshot.size() + "건 (" + rangeText + ")");

                ListView listView = getListView(snapshot);
                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, listView);

                builder.setView(container)
                        .setNeutralButton("복사", (d, w) -> copyResultsToClipboard(snapshot));

                if (!rangeExceeded) {
                    builder.setPositiveButton("계속 검색", (d, w) -> {
                        if (searchMode != SearchMode.COMMENT) {
                            startPage = lastValidPage + 1;
                            endPage = startPage + rangeSize - 1;
                        }
                        transitionTo(Step.PARSING);
                    });
                }

                builder.setNegativeButton("닫기", null);
            }

            case ERROR -> builder
                    .setMessage("요청 처리 중 오류가 발생했습니다.")
                    .setPositiveButton("확인", null);
        }
    }

    private void runSearch() {
        SearchStrategy.SearchContext searchContext = new SearchStrategy.SearchContext(
                galleryType,
                galleryId,
                targetUserId,
                startPage,
                endPage,
                rangeSize
        );

        SearchStrategy strategy = (searchMode == SearchMode.COMMENT) ? commentSearchStrategy : postSearchStrategy;

        strategy.start(searchContext, new SearchStrategy.SearchCallback() {
            @Override
            public void onProgress(int done, int total) {
                if (currentDialog != null && currentDialog.isShowing()) {
                    currentDialog.setMessage(progressText(done, total));
                }
            }

            @Override
            public void onComplete(List<JSONObject> results, int lastPage, boolean exceeded) {
                resultsList.addAll(results);
                lastValidPage = Math.max(lastValidPage, lastPage);
                rangeExceeded = exceeded;
                transitionTo(Step.RESULT);
            }

            @Override
            public void onError() {
                transitionTo(Step.ERROR);
            }
        });
    }

    @NonNull
    private ListView getListView(List<JSONObject> snapshot) {
        ListView listView = DialogUiUtils.createHeightLimitedListView(context, 0.5f);

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
                    subView.setTextColor(secondaryColor);

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
                String type = item != null ? item.optString("type", "post") : "post";

                if ("comment".equals(type)) {
                    String nickname = item.optString("nickname", "");
                    String date = item.optString("date", "");
                    titleView.setText(title);
                    subView.setText("작성자: " + nickname + " | " + date);
                } else {
                    String reply = item != null ? item.optString("replyCount", "0") : "0";
                    String date = item != null ? item.optString("date", "") : "";
                    String views = item != null ? item.optString("views", "") : "";
                    String recommend = item != null ? item.optString("recommend", "") : "";
                    titleView.setText(title + "  [" + reply + "]");
                    subView.setText(date + ", 조회: " + views + ", 추천: " + recommend);
                }

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
                intent.putExtra("com.dcinside.app.extra.GALLERY_ID", galleryId);
                intent.putExtra("com.dcinside.app.extra.POST_NUMBER", postNoInt);

                if ("comment".equals(item.optString("type", "post"))) {
                    String fcno = item.optString("fcno", "");
                    if (!fcno.isEmpty()) {
                        try {
                            intent.putExtra("com.dcinside.app.extra.COMMENT_NUMBER", Integer.parseInt(fcno));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                context.startActivity(intent);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "게시글 번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "게시글을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return listView;
    }

    private String progressText(int completed, int total) {
        return "검색하고 있습니다...\n(" + completed + " / " + total + "페이지" + ")";
    }

    private void copyResultsToClipboard(List<JSONObject> snapshot) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(targetUserId).append(" ").append(searchMode).append(" 스코프 결과\n");
        stringBuilder.append(snapshot.size()).append("개의 결과를 찾았습니다.\n\n");
        for (JSONObject item : snapshot) {
            stringBuilder.append(item.optString("date", "")).append("\t")
                    .append(item.optString("title", "")).append("\n")
                    .append(item.optString("url", "")).append("\n\n");
        }

        DialogUiUtils.copyToClipboard(context, "GallScope Results", stringBuilder.toString());
    }
}