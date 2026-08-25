package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

import android.net.Uri;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class GallScopeHtmlParser {

    private static final String BASE_URL = "https://gall.dcinside.com";
    private static final Pattern REPLY_COUNT_PATTERN = Pattern.compile("\\[(\\d+)]");
    private static final Pattern SEARCH_POS_PATTERN = Pattern.compile("search_pos=(\\d+)");

    private GallScopeHtmlParser() {
    }

    // ==================== 게시글 파싱 ====================

    static JSONArray parseAndFilterPostsByUserId(Document doc, String targetUserId) throws JSONException {
        JSONArray results = new JSONArray();
        Elements rows = doc.select("tr.ub-content.us-post");

        for (Element row : rows) {
            Element writerEl = row.selectFirst("td.gall_writer");
            if (writerEl == null) continue;

            String uid = writerEl.attr("data-uid").trim();
            String ip = writerEl.attr("data-ip").trim();
            String identifier = !uid.isEmpty() ? uid : ip;

            if (identifier.isEmpty() || !identifier.equals(targetUserId)) {
                continue;
            }

            results.put(parsePostRow(row, writerEl, uid, ip));
        }

        return results;
    }

    private static JSONObject parsePostRow(Element row, Element writerEl, String uid, String ip) throws JSONException {
        String postNo = text(row, "td.gall_num");
        String subject = text(row, "td.gall_subject");

        Element titleAnchor = row.selectFirst("td.gall_tit.ub-word > a");
        String title = titleAnchor != null ? titleAnchor.ownText().trim() : "";
        String url = titleAnchor != null ? toMobileUrl(toAbsoluteUrl(titleAnchor.attr("href"))) : "";

        String replyRaw = text(row, "a.reply_numbox");
        String replyCount = extractReplyCount(replyRaw);

        String nickname = writerEl.attr("data-nick").trim();

        Element dateEl = row.selectFirst("td.gall_date");
        String date = dateEl != null ? dateEl.attr("title").trim() : "";

        String views = text(row, "td.gall_count");
        String recommend = text(row, "td.gall_recommend");

        JSONObject record = new JSONObject();
        record.put("type", "post");
        record.put("postNo", postNo);
        record.put("subject", subject);
        record.put("title", title);
        record.put("url", url);
        record.put("replyCount", replyCount);
        record.put("nickname", nickname);
        record.put("userId", uid);
        record.put("ip", ip);
        record.put("date", date);
        record.put("views", views);
        record.put("recommend", recommend);

        return record;
    }

    static boolean isPostPageOutOfRange(Document doc, int requestedPage) {
        Element currentPageEl = doc.selectFirst("div.bottom_paging_box em");
        if (currentPageEl == null) return false; // 못 찾으면 판단 보류(안전하게 통과)

        try {
            int actualPage = Integer.parseInt(currentPageEl.text().trim());
            return actualPage != requestedPage;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==================== 댓글 파싱 ====================

    /**
     * 댓글 검색 결과 페이지를 파싱해서 대상 식별코드와 일치하는 댓글만 반환.
     * 스키마를 게시글 스키마와 통일: content -> title, postNo/subject는 URL에서 파싱 가능하면 채움.
     */
    static JSONArray parseAndFilterCommentsByUserId(Document doc, String targetUserId) throws JSONException {
        JSONArray results = new JSONArray();
        Elements rows = doc.select(".listwrap2 .search.search_comment");

        for (Element row : rows) {
            Element writerEl = row.selectFirst("td.gall_writer, .gall_writer");
            if (writerEl == null) continue;

            String uid = writerEl.attr("data-uid").trim();
            String ip = writerEl.attr("data-ip").trim();
            String identifier = !uid.isEmpty() ? uid : ip;

            if (identifier.isEmpty() || !identifier.equals(targetUserId)) {
                continue;
            }

            results.put(parseCommentRow(row, writerEl, uid, ip));
        }

        return results;
    }

    private static JSONObject parseCommentRow(Element row, Element writerEl, String uid, String ip) throws JSONException {
        Element linkEl = row.selectFirst("div.sch_cmt a");
        String href = linkEl != null ? linkEl.attr("href") : "";
        String content = linkEl != null ? linkEl.text().trim() : "";
        String absoluteUrl = toAbsoluteUrl(href);

        String postNo = "";
        String fcno = "";

        if (!href.isEmpty()) {
            Uri uri = Uri.parse(href);
            String noParam = uri.getQueryParameter("no");
            String fcnoParam = uri.getQueryParameter("fcno");
            postNo = noParam != null ? noParam : "";
            fcno = fcnoParam != null ? fcnoParam : "";
        }

        String nickname = writerEl.attr("data-nick").trim();
        String date = text(row, "td.gall_date");

        JSONObject record = new JSONObject();
        record.put("type", "comment");
        record.put("postNo", postNo);
        record.put("subject", "");
        record.put("title", content);
        record.put("url", toMobileUrl(absoluteUrl));
        record.put("replyCount", "");
        record.put("nickname", nickname);
        record.put("userId", uid);
        record.put("ip", ip);
        record.put("date", date);
        record.put("views", "");
        record.put("recommend", "");
        record.put("fcno", fcno);

        return record;
    }

    /**
     * 댓글 검색 결과 페이지에서 "다음 검색" 링크(a.search_next)를 찾아 search_pos 값을 추출.
     * 없으면 null 반환 (= 마지막 페이지로 간주).
     */
    static String extractCommentSearchNext(Document doc) {
        Element nextEl = doc.selectFirst("div.bottom_paging_wrap a.search_next");
        if (nextEl == null) return null;

        String href = nextEl.attr("href");
        Matcher m = SEARCH_POS_PATTERN.matcher(href);
        return m.find() ? m.group(1) : null;
    }

    // ==================== 공용 유틸 ====================

    private static String toAbsoluteUrl(String href) {
        if (href == null || href.isEmpty()) return "";
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        return href.startsWith("/") ? BASE_URL + href : BASE_URL + "/" + href;
    }

    static String toMobileUrl(String pcUrl) {
        if (pcUrl == null || pcUrl.isEmpty()) return pcUrl;

        Uri uri = Uri.parse(pcUrl);
        String path = uri.getPath();
        if (path == null || !path.contains("/board/view/")) return pcUrl;

        String galleryId = uri.getQueryParameter("id");
        String postNo = uri.getQueryParameter("no");
        if (galleryId == null || postNo == null) return pcUrl;

        String segment = path.contains("/mini/") ? "mini" : "board";

        return "https://m.dcinside.com/" + segment + "/" + galleryId + "/" + postNo;
    }

    private static String extractReplyCount(String raw) {
        if (raw == null || raw.isEmpty()) return "0";
        Matcher matcher = REPLY_COUNT_PATTERN.matcher(raw);
        return matcher.find() ? matcher.group(1) : "0";
    }

    private static String text(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }
}