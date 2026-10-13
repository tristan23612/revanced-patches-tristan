package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DcBanListPcHtmlParser {
    private static final Pattern PAGE_PARAMETER = Pattern.compile("[?&]p=(\\d+)(?:&|$)");

    private DcBanListPcHtmlParser() {}

    static JSONArray parsePage(Document document) throws JSONException {
        JSONArray records = new JSONArray();
        for (Element row : document.select("tbody tr")) {
            if (row.selectFirst(".blocknik") != null) records.put(parseRow(row));
        }
        return records;
    }

    private static JSONObject parseRow(Element row) throws JSONException {
        Element nicknameCell = row.selectFirst(".blocknik");
        String nickname = "";
        String identifierText = "";
        int nonemptyIndex = 0;
        for (Element paragraph : nicknameCell.select("p")) {
            String value = clean(paragraph.text());
            if (value.isEmpty()) continue;
            if (nonemptyIndex++ == 0) nickname = value;
            else if (nonemptyIndex == 2) identifierText = value;
        }
        identifierText = identifierText.replaceAll("(?i)\\s*\\+\\s*IP\\s*$", "").trim();
        String identifier = identifierText.replaceFirst("^\\(", "").replaceFirst("\\)$", "").trim();
        if (nicknameCell.selectFirst(".txtip") != null) identifier += " + IP";

        Element contentCell = row.selectFirst(".blockcontent");
        String contentType = clean(contentCell == null || contentCell.selectFirst("em") == null
                ? "" : contentCell.selectFirst("em").text());
        String contentTitle = clean(contentCell == null || contentCell.selectFirst("a") == null
                ? "" : contentCell.selectFirst("a").text());
        String content = contentType.equals("직접 차단")
                ? contentType
                : !contentType.isEmpty() && !contentTitle.isEmpty()
                ? "[" + contentType + "] " + contentTitle
                : !contentTitle.isEmpty() ? contentTitle : contentType;

        Element dateCell = row.selectFirst(".blockday");
        String date = clean(dateCell == null || dateCell.selectFirst(".block_date") == null
                ? "" : dateCell.selectFirst(".block_date").text());
        String time = clean(dateCell == null || dateCell.selectFirst(".block_time") == null
                ? "" : dateCell.selectFirst(".block_time").text()).replaceFirst("^처리\\s*시간\\s*:\\s*", "");
        String manager = clean(dateCell == null || dateCell.selectFirst(".block_conduct") == null
                ? "" : dateCell.selectFirst(".block_conduct").text()).replaceFirst("^처리자\\s*:\\s*", "");

        JSONObject record = new JSONObject();
        record.put("nickname", nickname);
        record.put("identifier", identifier);
        record.put("content", content);
        record.put("reason", clean(row.selectFirst(".blockreason") == null ? "" : row.selectFirst(".blockreason").text()));
        record.put("duration", clean(row.selectFirst(".blocktime") == null ? "" : row.selectFirst(".blocktime").text()));
        record.put("dateTime", date.isEmpty() ? time : time.isEmpty() ? date : date + " " + time);
        record.put("manager", manager);
        return record;
    }

    static int extractLastPage(Document document, int currentPage) {
        Element endLink = document.selectFirst(".bottom_paging_box a.page_end[href]");
        if (endLink != null) {
            int lastPage = extractPageNumber(endLink.attr("href"));
            if (lastPage > 0) return Math.max(currentPage, lastPage);
        }

        int maximumPage = currentPage;
        for (Element link : document.select(".bottom_paging_box a[href]")) {
            maximumPage = Math.max(maximumPage, extractPageNumber(link.attr("href")));
        }
        return maximumPage;
    }

    static boolean hasBlockListRows(Document document) {
        return !document.select("tbody tr .blocknik").isEmpty();
    }

    private static int extractPageNumber(String href) {
        Matcher matcher = PAGE_PARAMETER.matcher(href);
        if (!matcher.find()) return 0;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
