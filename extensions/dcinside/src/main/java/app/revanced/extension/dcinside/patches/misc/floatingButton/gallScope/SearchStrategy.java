package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

import org.json.JSONObject;

import java.util.List;

/**
 * 검색 모드(게시글/댓글)별 fetch-parse 로직을 캡슐화하는 전략 인터페이스.
 * GallScopeSession은 이 인터페이스만 알고, 실제 페이지 순회/파싱 방식은 구현체가 담당한다.
 */
interface SearchStrategy {

    /**
     * 검색을 시작(또는 "계속 검색" 시 이어서 재개)한다.
     * 구현체는 내부적으로 비동기 요청을 반복하다가 종료 조건에 도달하면 콜백을 호출해야 한다.
     */
    void start(SearchContext context, SearchCallback callback);

    /**
     * 세션이 각 전략에 전달하는 검색 파라미터.
     * rangeSize는 "이번 배치에서 조회할 페이지(또는 누적 조회 횟수) 한도"를 의미.
     */
    final class SearchContext {
        final String galleryType;
        final String galleryId;
        final String targetUserId;
        final int startPage; // 게시글 모드: 실제 시작 페이지 / 댓글 모드는 전략 내부 상태 사용, 참고용
        final int endPage;   // 게시글 모드: 실제 끝 페이지
        final int rangeSize;

        SearchContext(String galleryType, String galleryId, String targetUserId,
                      int startPage, int endPage, int rangeSize) {
            this.galleryType = galleryType;
            this.galleryId = galleryId;
            this.targetUserId = targetUserId;
            this.startPage = startPage;
            this.endPage = endPage;
            this.rangeSize = rangeSize;
        }
    }

    /**
     * 전략 → 세션으로의 진행 상황/결과 통지.
     */
    interface SearchCallback {
        void onProgress(int done, int total);

        /**
         * @param results       이번 배치에서 수집된 결과 (누적 아님 - 세션이 기존 리스트에 append)
         * @param lastPage      이번 배치에서 마지막으로 유효했던 페이지(또는 누적 카운트) - "계속 검색" 시 이어받기용
         * @param rangeExceeded true면 더 이상 다음 페이지/검색이 없음 (RESULT에서 "계속 검색" 버튼 숨김)
         */
        void onComplete(List<JSONObject> results, int lastPage, boolean rangeExceeded);

        void onError();
    }
}