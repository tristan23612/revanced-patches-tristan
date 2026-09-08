package app.revanced.extension.dcinside.patches.management.post.duplicatePost;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

final class DuplicatePostSearchHtmlParser {
    private DuplicatePostSearchHtmlParser() {
    }

    static List<DuplicatePostSearchResult> parse(Document doc) {
        List<DuplicatePostSearchResult> results = new ArrayList<>();
        Elements rows = doc.select("ul.sch_result_list > li");

        for (Element row : rows) {
            Element titleEl = row.selectFirst("a.tit_txt");
            Element contentEl = row.selectFirst("p.link_dsc_txt:not(.dsc_sub)");
            Element subEl = row.selectFirst("p.link_dsc_txt.dsc_sub");
            if (titleEl == null || subEl == null) continue;

            Element galleryNameEl = subEl.selectFirst("a.sub_txt");
            if (galleryNameEl == null) continue;
            Element dateEl = subEl.selectFirst("span.date_time");
            if (dateEl == null) continue;

            String title = titleEl.text().trim();
            String content = contentEl != null ? contentEl.text().trim() : "";
            String galleryName = galleryNameEl.text().trim();
            String dateTime = dateEl.text().trim();

            if (title.isEmpty()) continue;

            results.add(new DuplicatePostSearchResult(title, content, galleryName, dateTime));
        }
        return results;
    }
}