package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

final class DcBanListHtmlParser {
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
        Element firstCaption = captions.isEmpty() ? null : captions.get(0);

        String[] header = parseHeader(firstCaption);
        String nickname = header[0];
        String identifierFromHeader = header[1];

        boolean hasIp = firstCaption != null && firstCaption.selectFirst(".ip") != null;
        String identifier = hasIp
                ? (identifierFromHeader + " + IP").trim()
                : identifierFromHeader;

        String contentType = "", contentTitle = "";
        String reason = "", duration = "", dateTime = "", manager = "";

        for (int i = 1; i < captions.size(); i++) {
            Element titEl = captions.get(i).selectFirst(".tit");
            Element txtEl = captions.get(i).selectFirst(".txt");
            if (txtEl == null) continue;

            if (titEl == null) {
                String txtText = txtEl.text().trim();
                if (contentTitle.isEmpty() && !txtText.isEmpty()) {
                    contentTitle = txtText;
                }
                continue;
            }

            String label = titEl.text().trim();
            switch (label) {
                case "게시글":
                case "댓글":
                    contentType = label;
                    Element lnkgo = txtEl.selectFirst(".lnkgo");
                    contentTitle = (lnkgo != null ? lnkgo.text() : txtEl.text()).trim();
                    break;
                case "사유":
                    reason = txtEl.text().trim();
                    break;
                case "기간":
                    duration = txtEl.text().trim();
                    break;
                case "처리 일시":
                    dateTime = txtEl.text().trim();
                    break;
                case "처리자":
                    manager = txtEl.text().trim();
                    break;
                default:
                    break; // "차단 상태" 등 불필요한 항목은 무시
            }
        }

        String content = contentType.isEmpty() ? contentTitle : "[" + contentType + "] " + contentTitle;

        JSONObject record = new JSONObject();
        record.put("nickname", nickname);
        record.put("identifier", identifier);
        record.put("content", content);
        record.put("reason", reason);
        record.put("duration", duration);
        record.put("dateTime", dateTime);
        record.put("manager", manager);

        return record;
    }

    static int extractTotalPages(Document doc) {
        int total = parseIntSafe(inputValue(doc, "#total"), 0);
        int slidePage = parseIntSafe(inputValue(doc, "#slidePage"), 100);
        if (slidePage <= 0) slidePage = 100;
        int totalPages = (int) Math.ceil((double) total / slidePage);
        return Math.max(totalPages, 1);
    }

    /**
     * Parses the header element to extract a nickname and an optional identifier.
     * The header typically contains text in the format "nickname (identifier)".
     * If the identifier is absent, only the nickname is returned.
     *
     * @param firstCaption the first caption element containing the header to parse;
     *                     can be null if no caption is provided
     * @return a String array containing two elements:
     *         - [0]: the parsed nickname (non-null, potentially empty if parsing fails)
     *         - [1]: the parsed identifier, or an empty string if no identifier is present
     */
    private static String[] parseHeader(Element firstCaption) {
        if (firstCaption == null) return new String[]{"", ""};
        Element titEl = firstCaption.selectFirst(".tit");
        if (titEl == null) return new String[]{"", ""};

        Element tit = titEl.clone();
        tit.select(".tip_box, .ip").remove(); // 부가 요소만 제거하면 나머지는 형태가 동일해짐

        String fullText = tit.text().trim();
        int openIdx = fullText.lastIndexOf('(');
        int closeIdx = fullText.lastIndexOf(')');
        if (openIdx >= 0 && closeIdx == fullText.length() - 1 && closeIdx > openIdx) {
            String nickname = fullText.substring(0, openIdx).trim();
            String identifier = fullText.substring(openIdx + 1, closeIdx).trim();
            return new String[]{nickname, identifier};
        }

        // 괄호 형태가 아니면 전체를 nickname으로, identifier는 빈 값
        return new String[]{fullText, ""};
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