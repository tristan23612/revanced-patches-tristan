package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import androidx.annotation.NonNull;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class DcBanListApiClient {
    private static final String SIGN_ORIGIN = "https://sign.dcinside.com";
    private static final String GALLERY_ORIGIN = "https://gall.dcinside.com";
    private static final String REDIRECT_URL = "https://www.dcinside.com/";
    private static final String USER_AGENT = "Mozilla/5.0";

    private DcBanListApiClient() {}

    static final class AuthenticatedSession {
        private final OkHttpClient httpClient;

        private AuthenticatedSession(OkHttpClient httpClient) {
            this.httpClient = httpClient;
        }
    }

    private static final class InMemoryCookieJar implements CookieJar {
        private final Map<String, Cookie> cookies = new HashMap<>();

        @Override
        public synchronized void saveFromResponse(@NonNull HttpUrl url, List<Cookie> receivedCookies) {
            for (Cookie cookie : receivedCookies) {
                String key = cookie.domain() + "|" + cookie.path() + "|" + cookie.name();
                if (cookie.expiresAt() <= System.currentTimeMillis()) {
                    cookies.remove(key);
                } else {
                    cookies.put(key, cookie);
                }
            }
        }

        @NonNull
        @Override
        public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
            long now = System.currentTimeMillis();
            List<Cookie> matchingCookies = new ArrayList<>();
            cookies.values().removeIf(cookie -> cookie.expiresAt() <= now);
            for (Cookie cookie : cookies.values()) {
                if (cookie.matches(url)) matchingCookies.add(cookie);
            }
            return matchingCookies;
        }

        synchronized boolean hasSessionCookie() {
            for (Cookie cookie : cookies.values()) {
                if (cookie.name().matches("(?i).*(sso_token|PHPSESSID|csid).*")) return true;
            }
            return false;
        }
    }

    private static OkHttpClient createHttpClient(CookieJar cookieJar) {
        return new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    static AuthenticatedSession login(String userId, String password) throws IOException {
        InMemoryCookieJar cookieJar = new InMemoryCookieJar();
        OkHttpClient httpClient = createHttpClient(cookieJar);

        HttpUrl loginPageUrl = Objects.requireNonNull(HttpUrl.parse(SIGN_ORIGIN + "/login"))
                .newBuilder()
                .addQueryParameter("s_url", REDIRECT_URL)
                .build();
        Request loginPageRequest = new Request.Builder()
                .url(loginPageUrl)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("User-Agent", USER_AGENT)
                .get()
                .build();

        String loginHtml;
        try (Response response = httpClient.newCall(loginPageRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DCInside 로그인 페이지 요청 실패 (HTTP " + response.code() + ")");
            }
            loginHtml = response.body().string();
        }

        Document loginDocument = Jsoup.parse(loginHtml);
        Element tokenInput = loginDocument.selectFirst("input[name=ci_t]");
        String ciToken = tokenInput == null ? "" : tokenInput.attr("value").trim();
        if (ciToken.isEmpty()) {
            throw new IOException("로그인 페이지에서 ci_t 값을 찾지 못했습니다. 로그인 양식이 변경되었을 수 있습니다.");
        }

        FormBody formBody = new FormBody.Builder()
                .add("ci_t", ciToken)
                .add("s_url", REDIRECT_URL)
                .add("tieup", "")
                .add("url", "")
                .add("ssl", "Y")
                .add("user_id", userId)
                .add("pw", password)
                .build();
        Request loginRequest = new Request.Builder()
                .url(SIGN_ORIGIN + "/login/member_check")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", SIGN_ORIGIN)
                .header("Referer", loginPageUrl.toString())
                .header("User-Agent", USER_AGENT)
                .post(formBody)
                .build();

        try (Response response = httpClient.newCall(loginRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DCInside 로그인에 실패했습니다 (HTTP " + response.code() + ")");
            }
            String responseHtml = response.body().string();
            boolean loginFormStillVisible = Jsoup.parse(responseHtml)
                    .selectFirst("input[name=user_id], input[name=pw]") != null;
            if (!cookieJar.hasSessionCookie() && loginFormStillVisible) {
                throw new IOException("DCInside 로그인이 완료되지 않았습니다. 계정 또는 추가 인증을 확인하세요.");
            }
        }

        return new AuthenticatedSession(httpClient);
    }

    static String resolveGalleryPath(String galleriesJson, String galleryId) throws JSONException {
        if (galleryId == null || galleryId.trim().isEmpty()) {
            throw new JSONException("현재 갤러리 ID를 확인할 수 없습니다.");
        }
        JSONArray galleries = new JSONArray(galleriesJson);
        String currentGalleryId = galleryId.trim();
        for (int i = 0; i < galleries.length(); i++) {
            JSONObject gallery = galleries.optJSONObject(i);
            if (gallery == null || !currentGalleryId.equals(gallery.optString("id", "").trim())) continue;

            String path = gallery.optString("path", "").trim();
            if (path.equals("mgallery") || path.equals("gallery") || path.equals("mini")) return path;
            throw new JSONException("갤러리 " + galleryId + "의 path는 mgallery, gallery, mini 중 하나여야 합니다.");
        }
        throw new JSONException("GALLERIES_JSON에 현재 갤러리 " + galleryId + " 설정이 없습니다.");
    }

    static Document fetchBanListPageSync(
            AuthenticatedSession session,
            String galleryPath,
            String galleryId,
            int page
    ) throws IOException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(GALLERY_ORIGIN))
                .newBuilder()
                .addPathSegment(galleryPath)
                .addPathSegments("management/block")
                .addQueryParameter("id", galleryId)
                .addQueryParameter("p", String.valueOf(page))
                .build();

        String previousPage = url.newBuilder()
                .setQueryParameter("p", String.valueOf(Math.max(1, page - 1)))
                .build()
                .toString();
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Referer", previousPage)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();

        try (Response response = session.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("차단 목록 " + page + "페이지 요청 실패 (HTTP " + response.code() + ")");
            }

            String html = response.body().string();
            if (html.length() > 2_000_000) {
                throw new IOException("차단 목록 " + page + "페이지 응답이 비정상적으로 큽니다.");
            }

            Document document = Jsoup.parse(html);
            if (!DcBanListPcHtmlParser.hasBlockListRows(document)) {
                if (html.matches("(?is).*(sign\\.dcinside\\.com/login|name=[\"']user_id[\"']).*")) {
                    throw new IOException("DCInside 로그인 세션이 유지되지 않았습니다.");
                }
                throw new IOException("차단 목록 " + page + "페이지에서 PC 차단 목록 행을 찾지 못했습니다.");
            }
            return document;
        }
    }
}
