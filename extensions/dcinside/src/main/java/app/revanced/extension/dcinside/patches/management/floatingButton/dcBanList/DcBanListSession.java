package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;
import app.revanced.extension.dcinside.patches.hook.okhttp.CustomNetworkInterceptorPatch;
import app.revanced.extension.dcinside.patches.management.DialogSession;
import app.revanced.extension.dcinside.patches.management.DialogUiUtils;
import app.revanced.extension.dcinside.settings.Settings;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressLint({"DiscouragedApi", "SetTextI18n"})
final class DcBanListSession extends DialogSession<DcBanListSession.Step> {
    private static final String TAG = "ReVanced_DCInside";

    enum Step {
        SHEET_ID_INPUT,
        ACTION_SELECTION,
        IDENTIFIER_INPUT,
        IDENTIFIER_FETCHING,
        IDENTIFIER_RESULT,
        IDENTIFIER_ERROR,
        OAUTH_CONFIRMATION,
        CHECKING_AUTH,
        NEED_AUTHORIZATION,
        FETCHING_LAST_RECORD,
        CREATE_SHEET_CONFIRMATION,
        PARSING,
        UPLOAD_CONFIRMATION,
        UPLOAD_IN_PROGRESS,
        UPLOAD_COMPLETE,
        UPLOAD_UNNECESSARY,
        ERROR
    }

    private boolean autoProcess = false;
    private String targetSheetId = "";
    private JSONArray banListJsonArray = new JSONArray();
    private JSONObject lastKnownRecord;

    private String identifier = "";
    private List<String> identifierHeaders = new ArrayList<>();
    private List<JSONObject> identifierResults = new ArrayList<>();
    private String identifierErrorMessage = "시트 데이터를 불러오지 못했습니다.";

    /**
     * SHEET_ID_INPUT에서 확인/취소 시 복귀할 Step. SHEET_ID_INPUT으로 전환하는 지점마다
     * 갱신하고, 최초 진입 시에는 생성자에서 resolveActionStep()으로 미리 계산해둔다.
     */
    private Step sheetIdReturnStep;

    DcBanListSession(Context context) {
        super(context, resolveInitialStep());

        if (currentStep != Step.SHEET_ID_INPUT) {
            targetSheetId = loadSavedSheetId();
        }

        sheetIdReturnStep = currentStep == Step.SHEET_ID_INPUT
                ? resolveActionStep()
                : currentStep;
    }

    static boolean hasAnyEnabledAction() {
        return Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get() || Settings.ENABLE_DC_BAN_LIST_BAN_LIST_EXPORT.get();
    }

    /**
     * 시트 ID가 이미 있다는 전제 하에, 활성화된 분기 개수에 따라 진입해야 할 Step을 반환.
     * 단일 분기면 그 Step으로 바로, 둘 다 켜졌으면 ACTION_SELECTION으로.
     */
    private static Step resolveActionStep() {
        boolean identifierEnabled = Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get();
        boolean exportEnabled = Settings.ENABLE_DC_BAN_LIST_BAN_LIST_EXPORT.get();

        if (identifierEnabled && !exportEnabled) {
            return Step.IDENTIFIER_INPUT;
        }
        if (exportEnabled && !identifierEnabled) {
            return Step.OAUTH_CONFIRMATION;
        }
        return Step.ACTION_SELECTION;
    }

    private static Step resolveInitialStep() {
        if (loadSavedSheetId().isEmpty()) {
            return Step.SHEET_ID_INPUT;
        }
        return resolveActionStep();
    }

    @Override
    protected String getDialogTitle() {
        return "차단 내역";
    }

    @Override
    protected void renderStep() {
        if (autoProcess && tryAutoAdvance(currentStep)) {
            return;
        }

        super.renderStep();
    }

    /**
     * DcBanListSession 고유의 "자동 진행" 판단. 다이얼로그를 새로 그리지 않고 바로 다음 스텝으로
     * 건너뛸 수 있으면 true를 반환하고 내부에서 transitionTo()를 호출한다.
     */
    private boolean tryAutoAdvance(Step step) {
        return switch (step) {
            case OAUTH_CONFIRMATION -> {
                checkAuthStatus();
                yield true;
            }
            case CREATE_SHEET_CONFIRMATION -> {
                transitionTo(Step.PARSING);
                yield true;
            }
            default -> false; // CHECKING_AUTH, PARSING, UPLOAD_IN_PROGRESS, ERROR 등은 원래도 버튼 없는 진행 단계
        };
    }

    @Override
    protected void buildStep(AlertDialog.Builder builder, Step step) {
        switch (step) {
            case SHEET_ID_INPUT -> {
                TextView message = new TextView(context);
                message.setText("연동할 구글 스프레드시트 ID를 입력하세요.\n(식별코드 조회, 차단 내역 내보내기 공통)");

                String existingSheetId = targetSheetId.isEmpty() ? loadSavedSheetId() : targetSheetId;
                boolean isEditingExisting = !existingSheetId.isEmpty();

                EditText input = new EditText(context);
                input.setHint(isEditingExisting ? existingSheetId : "스프레드시트 ID 입력");
                input.setHintTextColor(secondaryColor);
                input.setSingleLine(true);
                input.setGravity(android.view.Gravity.CENTER);

                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);

                int rowHeightPx = (int) (36 * context.getResources().getDisplayMetrics().density);

                LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, rowHeightPx, 4f);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, rowHeightPx, 1f);

                row.addView(new View(context), spacerParams);
                row.addView(input, inputParams);
                row.addView(new View(context), spacerParams);

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, row);

                builder.setView(container)
                        .setPositiveButton("확인", (d, w) -> {
                            String id = input.getText().toString().trim();
                            if (id.isEmpty()) {
                                if (isEditingExisting) {
                                    // 변경 없이 그대로 진행 - 기존 시트 ID 유지
                                    targetSheetId = existingSheetId;
                                    transitionTo(sheetIdReturnStep);
                                    return;
                                }
                                Toast.makeText(context, "시트 ID를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                transitionTo(Step.SHEET_ID_INPUT);
                                return;
                            }
                            targetSheetId = id;
                            saveSheetIdToMap(id);
                            transitionTo(sheetIdReturnStep);
                        })
                        .setNegativeButton("취소", isEditingExisting ? (d, w) -> transitionTo(sheetIdReturnStep) : null);
            }

            case ACTION_SELECTION -> {
                TextView message = new TextView(context);
                message.setText("원하는 작업을 선택하세요.");

                LinearLayout buttonRow = new LinearLayout(context);
                buttonRow.setOrientation(LinearLayout.HORIZONTAL);

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                int buttonMargin = (int) (4 * context.getResources().getDisplayMetrics().density);

                List<View> buttons = new ArrayList<>();

                if (Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get()) {
                    Button identifierSearchButton = new Button(context);
                    identifierSearchButton.setText("식별코드 조회");
                    identifierSearchButton.setOnClickListener(v -> {
                        currentDialog.dismiss();
                        transitionTo(Step.IDENTIFIER_INPUT);
                    });
                    identifierSearchButton.setBackground(DialogUiUtils.createOutlineButtonBackground(secondaryColor));
                    identifierSearchButton.setTextColor(textColor);
                    buttons.add(identifierSearchButton);
                }

                if (Settings.ENABLE_DC_BAN_LIST_BAN_LIST_EXPORT.get()) {
                    Button banListExportButton = new Button(context);
                    banListExportButton.setText("차단 내역 내보내기");
                    banListExportButton.setOnClickListener(v -> {
                        currentDialog.dismiss();
                        transitionTo(Step.OAUTH_CONFIRMATION);
                    });
                    banListExportButton.setBackground(DialogUiUtils.createOutlineButtonBackground(secondaryColor));
                    banListExportButton.setTextColor(textColor);
                    buttons.add(banListExportButton);
                }

                for (int i = 0; i < buttons.size(); i++) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonParams);
                    if (i > 0) params.leftMargin = buttonMargin;
                    if (i < buttons.size() - 1) params.rightMargin = buttonMargin;
                    buttonRow.addView(buttons.get(i), params);
                }

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, buttonRow);

                builder.setView(container)
                        .setNeutralButton("시트 ID 변경", (d, w) -> {
                            sheetIdReturnStep = Step.ACTION_SELECTION;
                            transitionTo(Step.SHEET_ID_INPUT);
                        })
                        .setNegativeButton("취소", null);
            }

            case IDENTIFIER_INPUT -> {
                TextView message = new TextView(context);
                message.setText("저장된 시트에서 정확히 일치하는 식별코드를 찾습니다.");

                EditText input = new EditText(context);
                input.setHint("식별코드 입력");
                input.setHintTextColor(secondaryColor);
                input.setSingleLine(true);
                input.setGravity(android.view.Gravity.CENTER);

                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);

                int rowHeightPx = (int) (36 * context.getResources().getDisplayMetrics().density);

                LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, rowHeightPx, 2f);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, rowHeightPx, 1f);

                row.addView(new View(context), spacerParams);
                row.addView(input, inputParams);
                row.addView(new View(context), spacerParams);

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, row);

                builder.setTitle("식별코드 조회")
                        .setView(container)
                        .setPositiveButton("조회", (d, w) -> {
                            identifier = DcBanListCsvParser.normalizeIdentifier(input.getText().toString());
                            if (identifier.isEmpty()) {
                                Toast.makeText(context, "식별코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                transitionTo(Step.IDENTIFIER_INPUT);
                                return;
                            }
                            transitionTo(Step.IDENTIFIER_FETCHING);
                        })
                        .setNeutralButton("시트 ID 변경", (d, w) -> {
                            sheetIdReturnStep = Step.IDENTIFIER_INPUT;
                            transitionTo(Step.SHEET_ID_INPUT);
                        })
                        .setNegativeButton("취소", null);
            }

            case IDENTIFIER_FETCHING -> {
                builder.setTitle("식별코드 조회")
                        .setMessage("시트에서 식별코드를 찾고 있습니다...");
                fetchIdentifierResults();
            }

            case IDENTIFIER_RESULT -> {
                builder.setTitle("식별코드 조회");
                showIdentifierResults(builder);
            }

            case IDENTIFIER_ERROR -> builder
                    .setTitle("식별코드 조회")
                    .setMessage(identifierErrorMessage)
                    .setPositiveButton("재시도", (d, w) -> transitionTo(Step.IDENTIFIER_FETCHING))
                    .setNegativeButton("닫기", null);

            case OAUTH_CONFIRMATION -> builder
                    .setMessage("구글 권한 확인 및 계정 연동 상태를 검증합니다.")
                    .setNeutralButton("시트 ID 변경", (d, w) -> {
                        sheetIdReturnStep = Step.OAUTH_CONFIRMATION;
                        transitionTo(Step.SHEET_ID_INPUT);
                    })
                    .setPositiveButton("권한 확인", (d, w) -> checkAuthStatus())
                    .setNegativeButton("자동 진행", (d, w) -> {
                        autoProcess = true;
                        transitionTo(Step.OAUTH_CONFIRMATION); // renderStep 재진입 → tryAutoAdvance가 가로챔
                    });

            case CHECKING_AUTH -> builder
                    .setMessage("구글 계정 권한을 확인하고 있습니다...");

            case NEED_AUTHORIZATION -> builder
                    .setMessage("""
                            GAS 인증에 실패하였습니다.
                            아래 경로에서 인증을 진행해주세요.
                            설정 > ReVanced 설정 > 기타 > 플로팅 버튼 > DC BanList > GAS 인증""")
                    .setNegativeButton("취소", null);

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
                    .setMessage(banListJsonArray.length() + "건의 신규 차단내역이 성공적으로 업로드되었습니다.")
                    .setPositiveButton("확인", null);

            case UPLOAD_UNNECESSARY -> builder
                    .setMessage("0건의 데이터가 수집되었습니다.\n업로드가 필요하지 않습니다.")
                    .setPositiveButton("확인", null);

            case ERROR -> builder
                    .setMessage("인증 또는 처리 중 오류가 발생했습니다. 다시 시도해주세요.")
                    .setPositiveButton("재시도", (d, w) -> transitionTo(Step.OAUTH_CONFIRMATION))
                    .setNegativeButton("취소", null);
        }
    }

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

                        if (responseText.contains("AUTH_OK")) {
                            transitionTo(Step.FETCHING_LAST_RECORD);
                            return;
                        }
                    }
                    Log.e(TAG, "GAS Auth check unauthorized. Code: " + response.code());
                    transitionTo(Step.NEED_AUTHORIZATION);
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
                payload.put("galleryId", CustomNetworkInterceptorPatch.galleryId);

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
        String galleryId = CustomNetworkInterceptorPatch.galleryId;

        JSONArray collected = new JSONArray();

        try {
            // 1페이지 요청
            Response firstResponse = DcBanListApiClient.fetchBanListPageSync(galleryType, galleryId, 1);
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
            int totalPages = DcBanListHtmlParser.extractTotalPages(firstDoc);

            boolean shouldStop = appendRecordsUntilDuplicate(collected, DcBanListHtmlParser.parsePage(firstDoc));
            if (shouldStop || totalPages <= 1) {
                finishParsing(collected);
                return;
            }

            for (int page = 2; page <= totalPages; page++) {
                Response response = DcBanListApiClient.fetchBanListPageSync(galleryType, galleryId, page);
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
                shouldStop = appendRecordsUntilDuplicate(collected, DcBanListHtmlParser.parsePage(doc));
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
        if (collected.length() == 0) {
            transitionTo(Step.UPLOAD_UNNECESSARY);
        }

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

    private boolean isSameEntry(JSONObject a, JSONObject b) {
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

    private void executeUploadTask() {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("action", "uploadToGoogleSheet");
                payload.put("sheetId", targetSheetId);
                payload.put("galleryId", CustomNetworkInterceptorPatch.galleryId);
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

    private static String loadSavedSheetId() {
        try {
            String json = Settings.DC_BAN_LIST_SHEET_ID_MAP.get();
            if (json.isEmpty()) return "";
            JSONObject map = new JSONObject(json);
            return map.optString(CustomNetworkInterceptorPatch.galleryId, "");
        } catch (JSONException e) {
            Log.w(TAG, "sheetId 맵 파싱 실패", e);
            return "";
        }
    }

    private void saveSheetIdToMap(String sheetId) {
        try {
            String json = Settings.DC_BAN_LIST_SHEET_ID_MAP.get();
            JSONObject map = json.isEmpty() ? new JSONObject() : new JSONObject(json);
            map.put(CustomNetworkInterceptorPatch.galleryId, sheetId);
            Settings.DC_BAN_LIST_SHEET_ID_MAP.save(map.toString());
        } catch (JSONException e) {
            Log.e(TAG, "sheetId 맵 저장 실패", e);
        }
    }

    private void fetchIdentifierResults() {
        DcBanListSheetClient.fetchFirstSheetCsv(targetSheetId, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException error) {
                identifierErrorMessage = "시트 데이터를 불러오지 못했습니다.";
                transitionTo(Step.IDENTIFIER_ERROR);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        identifierErrorMessage = "시트 데이터를 불러오지 못했습니다. (HTTP " + response.code() + ")";
                        transitionTo(Step.IDENTIFIER_ERROR);
                        return;
                    }

                    DcBanListCsvParser.CsvTable table = DcBanListCsvParser.parse(response.body().string());
                    if (!table.headers.contains(DcBanListCsvParser.IDENTIFIER_COLUMN)) {
                        identifierErrorMessage = "시트에 '식별코드' 열이 없습니다.";
                        transitionTo(Step.IDENTIFIER_ERROR);
                        return;
                    }

                    identifierHeaders = table.headers;
                    identifierResults = DcBanListCsvParser.findExactIdentifierMatches(table, identifier);
                    transitionTo(Step.IDENTIFIER_RESULT);
                } catch (JSONException error) {
                    identifierErrorMessage = "시트 CSV 형식을 읽지 못했습니다.";
                    transitionTo(Step.IDENTIFIER_ERROR);
                }
            }
        });
    }

    private void showIdentifierResults(AlertDialog.Builder builder) {
        List<JSONObject> snapshot = new ArrayList<>(identifierResults);
        TextView message = new TextView(context);
        message.setText(identifier + " 식별코드 조회 결과\n" + snapshot.size() + "건");
        ListView listView = getIdentifierResultListView(snapshot);
        LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, listView);
        builder.setView(container)
                .setNeutralButton("복사", (d, w) -> copyIdentifierResults(snapshot))
                .setPositiveButton("닫기", null);
    }

    @NonNull
    private ListView getIdentifierResultListView(List<JSONObject> snapshot) {
        ListView listView = DialogUiUtils.createHeightLimitedListView(context, 0.5f);
        listView.setAdapter(new ArrayAdapter<JSONObject>(context, 0, snapshot) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                LinearLayout row;
                TextView titleView;
                TextView detailView;
                if (convertView == null) {
                    row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.VERTICAL);
                    int padding = (int) (8 * context.getResources().getDisplayMetrics().density);
                    row.setPadding(padding, padding, padding, padding);
                    titleView = new TextView(context);
                    titleView.setTextSize(16);
                    detailView = new TextView(context);
                    detailView.setTextSize(13);
                    detailView.setTextColor(DialogUiUtils.resolveSecondaryTextColor(context));
                    row.addView(titleView);
                    row.addView(detailView);
                    row.setTag(new View[]{titleView, detailView});
                } else {
                    row = (LinearLayout) convertView;
                    View[] views = (View[]) row.getTag();
                    titleView = (TextView) views[0];
                    detailView = (TextView) views[1];
                }
                JSONObject item = getItem(position);
                titleView.setText(item == null ? "" : item.optString(DcBanListCsvParser.CONTENT_COLUMN, ""));
                detailView.setText(formatAllIdentifierColumns(item));
                return row;
            }
        });
        return listView;
    }

    private String formatAllIdentifierColumns(JSONObject row) {
        if (row == null) return "";
        StringBuilder text = new StringBuilder();
        for (String header : identifierHeaders) {
            if (text.length() > 0) text.append('\n');
            text.append(header).append(": ").append(row.optString(header, ""));
        }
        return text.toString();
    }

    private void copyIdentifierResults(List<JSONObject> snapshot) {
        StringBuilder text = new StringBuilder();
        text.append(identifier).append(" 식별코드 조회 결과\n").append(snapshot.size()).append("건\n\n");
        for (JSONObject row : snapshot) text.append(formatAllIdentifierColumns(row)).append("\n\n");
        DialogUiUtils.copyToClipboard(context, "DC BanList identifier results", text.toString());
    }
}