package app.revanced.extension.dcinside.patches.misc.dcBanList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Insets;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;

public class DcBanListPatch {
    private static final String TAG = "ReVanced_DCInside";

    private static final Pattern HEADER_PATTERN = Pattern.compile("^([\\s\\S]+?)\\s*(?:\\(([^)]+)\\))?$");

    public static void setDcBanListButtonVisibility(View targetView, boolean visible, String dcBanListButtonIdName) {
        if (targetView == null) return;

        View rootView = targetView.getRootView();
        int resId = rootView.getContext().getResources().getIdentifier(
                dcBanListButtonIdName,
                "id",
                rootView.getContext().getPackageName()
        );

        View dcBanListButton = rootView.findViewById(resId);
        if (dcBanListButton != null) {
            // boolean 조건에 따라 가시성 플래그 매핑
            dcBanListButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public static void setupDcBanListButton(View view, String dcBanListButtonIdName) {
        if (view == null) return;

        int resId = view.getContext().getResources().getIdentifier(
                dcBanListButtonIdName,
                "id",
                view.getContext().getPackageName()
        );

        View dcBanListButton = view.findViewById(resId);
        if (dcBanListButton != null) {
            dcBanListButton.setOnClickListener(buttonView -> showExportDialog(buttonView.getContext()));
        }
    }

    private static void showExportDialog(Context context) {
        new ExportSession(context).start();
    }

    @SuppressLint("DiscouragedApi")
    private static int resolveDialogTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(
                context.getResources().getIdentifier("alertDialogTheme", "attr", context.getPackageName()),
                typedValue,
                true
        );
        return typedValue.resourceId;
    }

    private static class ExportSession {
        private enum Step {
            OAUTH_CONFIRMATION,
            CHECKING_AUTH,
            SHEET_ID_CONFIRMATION,
            FETCHING_LAST_RECORD,
            CREATE_SHEET_CONFIRMATION,
            PARSING,
            UPLOAD_CONFIRMATION,
            UPLOAD_IN_PROGRESS,
            UPLOAD_COMPLETE,
            ERROR
        }

        private final Context context;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private Step currentStep = Step.OAUTH_CONFIRMATION;
        private AlertDialog currentDialog;

        private String targetSheetId = "";
        private JSONArray banListJsonArray = new JSONArray();
        private JSONObject lastKnownRecord;

        private ExportSession(Context context) {
            this.context = context;
        }

        private void start() {
            renderStep();
        }

        private void renderStep() {
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }

            int dialogThemeResId = resolveDialogTheme(context);

            AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogThemeResId)
                    .setTitle("차단 내역 내보내기")
                    .setCancelable(false);

            switch (currentStep) {
                case OAUTH_CONFIRMATION -> builder
                        .setMessage("구글 권한 확인 및 계정 연동 상태를 검증합니다.")
                        .setPositiveButton("권한 확인", (d, w) -> checkAuthStatus()) // 수정: 비동기 검증 실행
                        .setNegativeButton("취소", null);

                case CHECKING_AUTH -> builder
                        .setMessage("구글 계정 권한을 확인하고 있습니다..."); // 추가: 검증 대기 UI

                case SHEET_ID_CONFIRMATION -> {
                    EditText input = new EditText(context);
                    input.setHint("스프레드시트 ID 입력");
                    if (!targetSheetId.isEmpty()) {
                        input.setText(targetSheetId);
                    }

                    FrameLayout container = new FrameLayout(context);
                    int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
                    container.setPadding(padding, 0, padding, 0);
                    container.addView(input);

                    builder.setMessage("연동할 구글 스프레드시트 ID를 입력하세요.")
                            .setView(container)
                            .setPositiveButton("내보내기", (d, w) -> {
                                String id = input.getText().toString().trim();
                                if (id.isEmpty()) {
                                    Toast.makeText(context, "시트 ID를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                    transitionTo(Step.SHEET_ID_CONFIRMATION);
                                    return;
                                }
                                targetSheetId = id;
                                transitionTo(Step.FETCHING_LAST_RECORD);
                            })
                            .setNegativeButton("취소", null);
                }

                case FETCHING_LAST_RECORD -> {
                    builder.setMessage("이전 업로드 기록을 확인하고 있습니다...");
                    fetchLastKnownRecord();
                }

                case CREATE_SHEET_CONFIRMATION -> builder
                        .setMessage("새로운 시트로 차단 내역을 업로드하시겠습니까?")
                        .setPositiveButton("확인", (d, w) -> transitionTo(Step.PARSING))
                        .setNegativeButton("취소", null);

                case PARSING -> {
                    builder.setMessage("디시인사이드 차단 목록을 수집 중입니다...");
                    new Thread(this::parseBanList).start();
                }

                case UPLOAD_CONFIRMATION -> builder
                        .setMessage(banListJsonArray.length() + "건의 신규 차단내역을 업로드하시겠습니까?")
                        .setPositiveButton("확인", (d, w) -> transitionTo(Step.UPLOAD_IN_PROGRESS))
                        .setNegativeButton("취소", null);

                case UPLOAD_IN_PROGRESS -> {
                    builder.setMessage("구글 시트로 데이터를 전송 중입니다...");
                    mainHandler.post(this::executeUploadTask);
                }

                case UPLOAD_COMPLETE -> builder
                        .setMessage("성공적으로 업로드되었습니다.")
                        .setPositiveButton("확인", null);

                case ERROR -> builder
                        .setMessage("인증 또는 처리 중 오류가 발생했습니다. 다시 시도해주세요.")
                        .setPositiveButton("재시도", (d, w) -> transitionTo(Step.OAUTH_CONFIRMATION))
                        .setNegativeButton("취소", null);
            }

            currentDialog = builder.create();
            currentDialog.show();

            if (currentDialog.getWindow() != null) {
                currentDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                currentDialog.getWindow().setDecorFitsSystemWindows(false);

                View rootView = currentDialog.findViewById(android.R.id.content);
                rootView.setOnApplyWindowInsetsListener((view, insets) -> {
                    // Framework API 직접 사용
                    Insets imeInsets = insets.getInsets(WindowInsets.Type.ime());
                    Insets systemBarsInsets = insets.getInsets(WindowInsets.Type.systemBars());

                    int bottomPadding = Math.max(imeInsets.bottom, systemBarsInsets.bottom);
                    view.setPadding(
                            systemBarsInsets.left,
                            systemBarsInsets.top,
                            systemBarsInsets.right,
                            bottomPadding
                    );

                    return WindowInsets.CONSUMED;
                });
            }
        }

        private void transitionTo(Step nextStep) {
            this.currentStep = nextStep;
            mainHandler.post(this::renderStep);
        }

        // 핵심 로직: GAS 권한 검증 요청
        private void checkAuthStatus() {
            transitionTo(Step.CHECKING_AUTH);

            GasApiClient.sendGet("?check=true", new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "GAS Auth check failed", e);
                    transitionTo(Step.ERROR);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (response) {
                        if (response.isSuccessful()) {
                            String responseText = response.body().string().trim();

                            if (responseText.contains("AUTH_OK") || response.code() == 200) {
                                transitionTo(Step.SHEET_ID_CONFIRMATION);
                                return;
                            }
                        }
                        Log.e(TAG, "GAS Auth check unauthorized. Code: " + response.code());
                        transitionTo(Step.ERROR);
                    }
                }
            });
        }

        private void fetchLastKnownRecord() {
            new Thread(() -> {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("action", "getLastKnownRecord");
                    payload.put("sheetId", targetSheetId);
                    payload.put("galleryId", JsonHookPatch.galleryId);

                    GasApiClient.sendPost("", payload.toString(), new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.w(TAG, "getLastKnownRecord 조회 실패 - 전체 수집으로 진행", e);
                            lastKnownRecord = null;
                            transitionTo(Step.PARSING);
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            try (response) {
                                if (!response.isSuccessful()) {
                                    Log.w(TAG, "getLastKnownRecord HTTP 오류: " + response.code() + " - 전체 수집으로 진행");
                                    lastKnownRecord = null;
                                    transitionTo(Step.PARSING);
                                    return;
                                }

                                String responseText = response.body().string().trim();
                                JSONObject jsonResponse = new JSONObject(responseText);

                                if ("success".equals(jsonResponse.optString("status"))) {
                                    lastKnownRecord = jsonResponse.optJSONObject("lastKnownRecord"); // 최초 기록이 없으면 null일 수 있음 - 정상
                                } else {
                                    Log.w(TAG, "getLastKnownRecord 서버 응답 실패 - 전체 수집으로 진행: "
                                            + jsonResponse.optString("message"));
                                    lastKnownRecord = null;
                                }
                            } catch (JSONException e) {
                                Log.w(TAG, "getLastKnownRecord 응답 파싱 실패 - 전체 수집으로 진행", e);
                                lastKnownRecord = null;
                            }
                            if (lastKnownRecord == null || lastKnownRecord.length() == 0) {
                                transitionTo(Step.CREATE_SHEET_CONFIRMATION);
                            }
                            else {
                                transitionTo(Step.PARSING);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "getLastKnownRecord 요청 준비 중 예외 - 전체 수집으로 진행", e);
                    lastKnownRecord = null;
                    transitionTo(Step.PARSING);
                }
            }).start();
        }

        /**
         * 디시인사이드 모바일 차단내역 페이지를 순차적으로 요청/파싱
         */
        private void parseBanList() {
            String galleryType = JsonHookPatch.galleryType;
            String galleryId = JsonHookPatch.galleryId;

            JSONArray collected = new JSONArray();

            try {
                // 1페이지 요청
                Response firstResponse = DcApiClient.fetchBanListPageSync(galleryType, galleryId, 1);
                String firstHtml;
                try (firstResponse) {
                    if (!firstResponse.isSuccessful()) {
                        Log.e(TAG, "차단내역 1페이지 요청 실패. code=" + firstResponse.code());
                        transitionTo(Step.ERROR);
                        return;
                    }
                    firstHtml = firstResponse.body().string();
                }

                Document firstDoc = Jsoup.parse(firstHtml);
                int totalPages = extractTotalPages(firstDoc);

                boolean shouldStop = appendRecordsUntilDuplicate(collected, parsePage(firstDoc));
                if (shouldStop || totalPages <= 1) {
                    finishParsing(collected);
                    return;
                }

                for (int page = 2; page <= totalPages; page++) {
                    Response response = DcApiClient.fetchBanListPageSync(galleryType, galleryId, page);
                    String html;
                    try (response) {
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "차단내역 " + page + "페이지 요청 실패. code=" + response.code());
                            transitionTo(Step.ERROR);
                            return;
                        }
                        html = response.body().string();
                    }

                    Document doc = Jsoup.parse(html);
                    shouldStop = appendRecordsUntilDuplicate(collected, parsePage(doc));
                    if (shouldStop) break;
                }

                finishParsing(collected);
            } catch (IOException e) {
                Log.e(TAG, "차단내역 수집 중 네트워크 오류", e);
                transitionTo(Step.ERROR);
            } catch (Exception e) {
                Log.e(TAG, "차단내역 파싱 중 예외 발생", e);
                transitionTo(Step.ERROR);
            }
        }

        private void finishParsing(JSONArray collected) {
            banListJsonArray = collected;
            transitionTo(Step.UPLOAD_CONFIRMATION);
        }

        /**
         * 파싱된 레코드들을 collected에 순서대로 담다가, lastKnownRecord와 동일한 레코드를 만나면 중단.
         *
         * @return true면 중복 레코드를 만나 중단됨(더 이상 다음 페이지를 요청하지 않아도 됨)
         */
        private boolean appendRecordsUntilDuplicate(JSONArray collected, JSONArray pageRecords) throws JSONException {
            for (int i = 0; i < pageRecords.length(); i++) {
                JSONObject record = pageRecords.getJSONObject(i);
                if (lastKnownRecord != null && isSameEntry(record, lastKnownRecord)) {
                    return true;
                }
                collected.put(record);
            }
            return false;
        }

        private boolean isSameEntry(JSONObject a, JSONObject b) throws JSONException {
            return eq(a, b, "nickname")
                    && eq(a, b, "identifier")
                    && eq(a, b, "content")
                    && eq(a, b, "reason")
                    && eq(a, b, "duration")
                    && eq(a, b, "dateTime")
                    && eq(a, b, "manager");
        }

        private boolean eq(JSONObject a, JSONObject b, String key) {
            return a.optString(key, "").equals(b.optString(key, ""));
        }

        private JSONArray parsePage(Document doc) throws JSONException {
            JSONArray records = new JSONArray();
            Elements rows = doc.select("li .item");

            for (Element row : rows) {
                records.put(parseMobileRow(row));
            }

            return records;
        }

        private JSONObject parseMobileRow(Element row) throws JSONException {
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

        private String captionText(Map<String, Element> captionMap, String label) {
            Element el = captionMap.get(label);
            return el != null ? el.text().trim() : "";
        }

        private String text(Element parent, String selector) {
            Element el = parent.selectFirst(selector);
            return el != null ? el.text().trim() : "";
        }

        private int extractTotalPages(Document doc) {
            int total = parseIntSafe(inputValue(doc, "#total"), 0);
            int slidePage = parseIntSafe(inputValue(doc, "#slidePage"), 100);
            if (slidePage <= 0) slidePage = 100;
            int totalPages = (int) Math.ceil((double) total / slidePage);
            return Math.max(totalPages, 1);
        }

        private String inputValue(Document doc, String selector) {
            Element el = doc.selectFirst(selector);
            return el != null ? el.attr("value") : "";
        }

        private int parseIntSafe(String value, int fallback) {
            try {
                return Integer.parseInt(value.trim());
            } catch (Exception e) {
                return fallback;
            }
        }

        private void executeUploadTask() {
            new Thread(() -> {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("action", "uploadToGoogleSheet");
                    payload.put("sheetId", targetSheetId);
                    payload.put("galleryId", JsonHookPatch.galleryId);
                    payload.put("banList", banListJsonArray);

                    GasApiClient.sendPost("", payload.toString(), new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.e(TAG, "GAS API 전송 실패", e);
                            transitionTo(Step.ERROR);
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            try (response) {
                                if (!response.isSuccessful()) {
                                    Log.e(TAG, "GAS HTTP 응답 에러 코드: " + response.code());
                                    transitionTo(Step.ERROR);
                                    return;
                                }

                                String responseText = response.body().string().trim();
                                JSONObject jsonResponse = new JSONObject(responseText);

                                if ("success".equals(jsonResponse.optString("status"))) {
                                    transitionTo(Step.UPLOAD_COMPLETE);
                                } else {
                                    String serverMessage = jsonResponse.optString("message", "Unknown Server Error");
                                    Log.e(TAG, "Google 스프레드시트 업데이트 실패: " + serverMessage);
                                    transitionTo(Step.ERROR);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "응답 JSON 파싱 실패", e);
                                transitionTo(Step.ERROR);
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "파싱 및 페이로드 생성 중 예외 발생", e);
                    transitionTo(Step.ERROR);
                }
            }).start();
        }
    }
}