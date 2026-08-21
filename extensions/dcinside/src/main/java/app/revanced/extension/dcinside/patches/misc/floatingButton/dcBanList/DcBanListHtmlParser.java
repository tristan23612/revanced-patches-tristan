package app.revanced.extension.dcinside.patches.misc.floatingButton.dcBanList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DcBanListHtmlParser {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([\\s\\S]+?)\\s*(?:\\(([^)]+)\\))?$");

    private DcBanListHtmlParser() {}

    static JSONArray parsePage(Document doc) throws JSONException {
        JSONArray records = new JSONArray();
        Elements rows = doc.select("li .item");

        for (Element row : rows) {
            records.put(parseMobileRow(row));
        }

        return records;
    }

    static JSONObject parseMobileRow(Element row) throws JSONException {
        Elements captions = row.select(".mg-block-caption");

        String headerText = captions.isEmpty() ? "" : text(captions.get(0), ".tit");
        String ipText = captions.isEmpty() ? "" : text(captions.get(0), ".ip");

        Matcher matcher = HEADER_PATTERN.matcher(headerText);
        String nickname = "";
        String identifierFromHeader = "";
        if (matcher.matches()) {
            nickname = matcher.group(1) != null ? Objects.requireNonNull(matcher.group(1)).trim() : "";
            identifierFromHeader = matcher.group(2) != null ? Objects.requireNonNull(matcher.group(2)).trim() : "";
        }
        String identifier = (identifierFromHeader + " " + ipText).trim();

        // 동적 캡션(2번째 항목부터)을 label -> value Element 맵으로 구성
        Map<String, Element> captionMap = new LinkedHashMap<>();
        for (int i = 1; i < captions.size(); i++) {
            Element cap = captions.get(i);
            Element titEl = cap.selectFirst(".tit");
            Element txtEl = cap.selectFirst(".txt");
            if (titEl != null && txtEl != null) {
                captionMap.put(titEl.text().trim(), txtEl);
            }
        }

        String contentType = captionMap.containsKey("게시글") ? "게시글"
                : (captionMap.containsKey("댓글") ? "댓글" : "");
        Element contentEl = captionMap.containsKey("게시글") ? captionMap.get("게시글") : captionMap.get("댓글");

        String contentTitle = "";
        if (contentEl != null) {
            Element lnkgo = contentEl.selectFirst(".lnkgo");
            contentTitle = (lnkgo != null ? lnkgo.text() : contentEl.text()).trim();
        }
        String content = contentType.isEmpty() ? contentTitle : "[" + contentType + "] " + contentTitle;

        JSONObject record = new JSONObject();
        record.put("nickname", nickname);
        record.put("identifier", identifier);
        record.put("content", content);
        record.put("reason", captionText(captionMap, "사유"));
        record.put("duration", captionText(captionMap, "기간"));
        record.put("dateTime", captionText(captionMap, "처리 일시"));
        record.put("manager", captionText(captionMap, "처리자"));

        return record;
    }

    static int extractTotalPages(Document doc) {
        int total = parseIntSafe(inputValue(doc, "#total"), 0);
        int slidePage = parseIntSafe(inputValue(doc, "#slidePage"), 100);
        if (slidePage <= 0) slidePage = 100;
        int totalPages = (int) Math.ceil((double) total / slidePage);
        return Math.max(totalPages, 1);
    }

    private static String captionText(Map<String, Element> captionMap, String label) {
        Element el = captionMap.get(label);
        return el != null ? el.text().trim() : "";
    }

    private static String text(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el != null ? el.text().trim() : "";
    }

    private static String inputValue(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el != null ? el.attr("value") : "";
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

}
