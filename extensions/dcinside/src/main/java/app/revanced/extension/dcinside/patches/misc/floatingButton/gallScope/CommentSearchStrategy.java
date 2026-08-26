package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 댓글 검색 전략 - search_pos 커서 기반이라 병렬화 불가, 순차 재귀 방식으로 처리.
 * "계속 검색" 시 이어받아야 하는 상태(commentSearchPos, commentRequestPage, 누적 카운트)는
 * 인스턴스 필드로 유지되므로, 세션은 동일한 CommentSearchStrategy 인스턴스를 재사용해야 한다.
 */
final class CommentSearchStrategy implements SearchStrategy {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<JSONObject> resultsList = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean rangeExceeded = false;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    // search_pos 갱신 시 1로 리셋되는 요청용 페이지 번호
    private int commentRequestPage = 1;
    // 리셋되지 않는 누적 조회 페이지(요청) 수 - 진행률/종료 판단용
    private int commentAccumulatedPageCount = 0;
    // 이번 배치 시작 시점의 누적 카운트 (진행률 계산 기준점)
    private int commentAccumulatedStartCount = 0;
    private String commentSearchPos = "";
    private Document prevCommentDoc;

    private SearchContext context;
    private SearchCallback callback;

    @Override
    public void start(SearchContext context, SearchCallback callback) {
        this.context = context;
        this.callback = callback;

        resultsList.clear();
        rangeExceeded = false;
        finished.set(false);
        commentAccumulatedStartCount = commentAccumulatedPageCount;

        fetchNextCommentPage();
    }

    /**
     * 새 검색 대상(식별코드 변경 등)으로 완전히 초기화할 때 세션에서 호출.
     * 같은 인스턴스를 "계속 검색"에 재사용하되, 새 세션/새 대상일 땐 리셋 필요.
     */
    void reset() {
        commentSearchPos = "";
        commentRequestPage = 1;
        commentAccumulatedPageCount = 0;
        commentAccumulatedStartCount = 0;
        prevCommentDoc = null;
    }

    private void fetchNextCommentPage() {
        int limit = commentAccumulatedStartCount + context.rangeSize;
        if (commentAccumulatedPageCount >= limit) {
            finishCommentFetch();
            return;
        }

        GallScopeApiClient.fetchCommentListPage(
                context.galleryType,
                context.galleryId,
                commentRequestPage,
                commentSearchPos,
                new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                        finishCommentFetch();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (!response.isSuccessful()) {
                                finishCommentFetch();
                                return;
                            }

                            String html = response.body().string();

                            int finalPage = extractPageParam(response.request().url().toString());
                            boolean redirected = finalPage != commentRequestPage;

                            if (redirected) {
                                handleCommentRedirect();
                                return;
                            }

                            Document doc = Jsoup.parse(html);
                            prevCommentDoc = doc;

                            boolean noCommentRows = doc.select(".listwrap2 .search.search_comment").isEmpty();
                            if (noCommentRows) {
                                // 검색 대상 자체가 없는 페이지 = 사실상 끝
                                finishCommentFetch();
                                return;
                            }

                            JSONArray pageResults =
                                    GallScopeHtmlParser.parseAndFilterCommentsByUserId(doc, context.targetUserId);
                            for (int i = 0; i < pageResults.length(); i++) {
                                resultsList.add(pageResults.getJSONObject(i));
                            }

                            commentAccumulatedPageCount++;
                            commentRequestPage++;

                            int done = commentAccumulatedPageCount - commentAccumulatedStartCount;
                            mainHandler.post(() -> callback.onProgress(done, context.rangeSize));

                            mainHandler.postDelayed(CommentSearchStrategy.this::fetchNextCommentPage, 100);
                        } catch (JSONException e) {
                            finishCommentFetch();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
    }

    private void handleCommentRedirect() {
        String nextSearchPos = (prevCommentDoc != null)
                ? GallScopeHtmlParser.extractCommentSearchNext(prevCommentDoc)
                : null;

        if (nextSearchPos == null) {
            // "다음 검색" 링크가 없음 = 마지막 페이지 도달
            rangeExceeded = true;
            finishCommentFetch();
            return;
        }

        commentSearchPos = nextSearchPos;
        commentRequestPage = 1;
        commentAccumulatedPageCount++; // 리다이렉트 자체도 1회 조회 시도로 누적 카운트

        int done = commentAccumulatedPageCount - commentAccumulatedStartCount;
        mainHandler.post(() -> callback.onProgress(done, context.rangeSize));

        mainHandler.postDelayed(this::fetchNextCommentPage, 500);
    }

    private void finishCommentFetch() {
        if (finished.compareAndSet(false, true)) {
            List<JSONObject> snapshot;
            synchronized (resultsList) {
                snapshot = new ArrayList<>(resultsList);
            }
            int lastPage = commentAccumulatedPageCount;
            mainHandler.post(() -> callback.onComplete(snapshot, lastPage, rangeExceeded));
        }
    }

    private int extractPageParam(String url) {
        try {
            Uri uri = Uri.parse(url);
            String pageStr = uri.getQueryParameter("page");
            return pageStr != null ? Integer.parseInt(pageStr) : 1;
        } catch (Exception e) {
            return commentRequestPage; // 파싱 실패 시 리다이렉트 아닌 것으로 간주
        }
    }
}