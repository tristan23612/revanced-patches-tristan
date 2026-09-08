package app.revanced.extension.dcinside.patches.management.floatingButton.gallScope;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 게시글 검색 전략 - startPage~endPage 범위를 슬라이딩 윈도우(BATCH_SIZE 동시 요청) 방식으로 병렬 파싱.
 */
final class PostSearchStrategy implements SearchStrategy {
    private static final int BATCH_SIZE = 5;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<JSONObject> resultsList = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean rangeExceeded = false;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicInteger lastValidPage = new AtomicInteger(0);
    private AtomicInteger completedPages;
    private AtomicInteger nextPageToLaunch;
    private AtomicInteger activeRequests;
    private int totalPagesInBatch;

    private SearchContext context;
    private SearchCallback callback;

    @Override
    public void start(SearchContext context, SearchCallback callback) {
        this.context = context;
        this.callback = callback;

        resultsList.clear();
        rangeExceeded = false;
        finished.set(false);

        totalPagesInBatch = context.endPage - context.startPage + 1;
        completedPages = new AtomicInteger(0);
        nextPageToLaunch = new AtomicInteger(context.startPage);
        activeRequests = new AtomicInteger(0);

        int initial = Math.min(BATCH_SIZE, totalPagesInBatch);
        for (int i = 0; i < initial; i++) {
            launchNextPostPage();
        }
    }

    private void launchNextPostPage() {
        if (rangeExceeded) return;
        int page = nextPageToLaunch.getAndIncrement();
        if (page > context.endPage) return;

        activeRequests.incrementAndGet();

        GallScopeApiClient.fetchPostListPage(
                context.galleryType,
                context.galleryId,
                page,
                new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                        onPostPageDone();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                String html = response.body().string();
                                Document doc = Jsoup.parse(html);

                                if (GallScopeHtmlParser.isPostPageOutOfRange(doc, page)) {
                                    rangeExceeded = true;
                                } else {
                                    lastValidPage.updateAndGet(v -> Math.max(v, page));
                                    JSONArray pageResults =
                                            GallScopeHtmlParser.parseAndFilterPostsByUserId(doc, context.targetUserId);
                                    for (int i = 0; i < pageResults.length(); i++) {
                                        resultsList.add(pageResults.getJSONObject(i));
                                    }
                                }
                            }
                        } catch (java.io.IOException | JSONException ignored) {
                            // 개별 페이지 파싱 실패는 무시하고 다음 페이지로 진행
                        }
                        onPostPageDone();
                    }
                }
        );
    }

    private void onPostPageDone() {
        activeRequests.decrementAndGet();
        int done = completedPages.incrementAndGet();
        mainHandler.post(() -> callback.onProgress(done, totalPagesInBatch));

        if (!rangeExceeded && nextPageToLaunch.get() <= context.endPage) {
            mainHandler.postDelayed(this::launchNextPostPage, 500);
        } else if (activeRequests.get() == 0) {
            if (finished.compareAndSet(false, true)) {
                sortResultsByPostNoDesc();
                List<JSONObject> snapshot;
                synchronized (resultsList) {
                    snapshot = new ArrayList<>(resultsList);
                }
                mainHandler.post(() -> callback.onComplete(snapshot, lastValidPage.get(), rangeExceeded));
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
}