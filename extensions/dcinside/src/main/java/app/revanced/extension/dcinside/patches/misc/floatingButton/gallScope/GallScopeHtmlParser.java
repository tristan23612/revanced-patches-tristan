package app.revanced.extension.dcinside.patches.misc.floatingButton.gallScope;

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

    private GallScopeHtmlParser() {
    }

    /**
     * 게시글 목록 페이지를 파싱해서, 주어진 식별코드(고정닉 ID 또는 유동 IP)와
     * 일치하는 글만 걸러서 반환한다.
     *
     * @param doc          목록 페이지 Document
     * @param targetUserId 검색할 식별코드 (예: "hapbi" 또는 IP 문자열)
     */
    static JSONArray parseAndFilterByUserId(Document doc, String targetUserId) throws JSONException {
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

            results.put(parseRow(row, writerEl, uid, ip));
        }

        return results;
    }

    private static JSONObject parseRow(Element row, Element writerEl, String uid, String ip) throws JSONException {
        String postNo = text(row, "td.gall_num");
        String subject = text(row, "td.gall_subject");

        Element titleAnchor = row.selectFirst("td.gall_tit.ub-word > a");
        String title = titleAnchor != null ? titleAnchor.ownText().trim() : "";
        String url = titleAnchor != null ? toAbsoluteUrl(titleAnchor.attr("href")) : "";

        String replyRaw = text(row, "a.reply_numbox");
        String replyCount = extractReplyCount(replyRaw);

        String nickname = writerEl.attr("data-nick").trim();

        Element dateEl = row.selectFirst("td.gall_date");
        String date = dateEl != null ? dateEl.attr("title").trim() : "";

        String views = text(row, "td.gall_count");
        String recommend = text(row, "td.gall_recommend");

        JSONObject record = new JSONObject();
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

    static boolean isPageOutOfRange(Document doc, int requestedPage) {
        Element currentPageEl = doc.selectFirst("div.bottom_paging_box em");
        if (currentPageEl == null) return false; // 못 찾으면 판단 보류(안전하게 통과)

        try {
            int actualPage = Integer.parseInt(currentPageEl.text().trim());
            return actualPage != requestedPage;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String toAbsoluteUrl(String href) {
        if (href == null || href.isEmpty()) return "";
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        return href.startsWith("/") ? BASE_URL + href : BASE_URL + "/" + href;
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