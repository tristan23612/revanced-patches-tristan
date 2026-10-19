package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
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
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressLint({"DiscouragedApi", "SetTextI18n"})
final class DcBanListSession extends DialogSession<DcBanListSession.Step> {
    private static final String TAG = "ReVanced_DCInside";

    static final String CONTENT_COLUMN = "게시글 / 댓글";

    enum Step {
        CLOUDFLARE_WORKER_INFO_INPUT,
        DCINSIDE_SETTINGS_INPUT,
        ACTION_SELECTION,
        IDENTIFIER_INPUT,
        IDENTIFIER_FETCHING,
        IDENTIFIER_RESULT,
        IDENTIFIER_ERROR,
        FETCHING_LATEST_DATA,
        INITIAL_INGEST_CONFIRMATION,
        PARSING,
        INGEST_CONFIRMATION,
        INGEST_IN_PROGRESS,
        INGEST_COMPLETE,
        INGEST_UNNECESSARY,
        ERROR
    }

    private JSONArray banListJsonArray = new JSONArray();
    private JSONObject latestData;

    private String identifier = "";
    private List<String> identifierHeaders = new ArrayList<>();
    private List<JSONObject> identifierResults = new ArrayList<>();
    private String errorMessage = "인증 또는 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
    private String identifierErrorMessage = "DB 데이터를 불러오지 못했습니다.";
    private String ingestResultMessage = "차단 내역 업로드가 완료되었습니다.";

    /**
     * Worker 정보 입력에서 확인/취소 시 복귀할 Step. 입력 단계로 전환하는 지점마다
     * 갱신하고, 최초 진입 시에는 생성자에서 resolveActionStep()으로 미리 계산해둔다.
     */
    private Step cloudflareWorkerInfoReturnStep = null;

    DcBanListSession(Context context) {
        super(context, resolveInitialStep());

        cloudflareWorkerInfoReturnStep = currentStep == Step.CLOUDFLARE_WORKER_INFO_INPUT
                ? resolveActionStep()
                : currentStep;
    }

    static boolean hasAnyEnabledAction() {
        return Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get() || Settings.ENABLE_DC_BAN_LIST_BAN_LIST_INGEST.get();
    }

    private static Step resolveActionStep() {
        boolean identifierEnabled = Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get();
        boolean ingestEnabled = Settings.ENABLE_DC_BAN_LIST_BAN_LIST_INGEST.get();

        if (identifierEnabled && !ingestEnabled) {
            return Step.IDENTIFIER_INPUT;
        }
        return Step.ACTION_SELECTION;
    }

    private static Step resolveInitialStep() {
        boolean missingCloudflareWorkerUrl = Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_URL.get().isEmpty();
        boolean missingCloudflareWorkerViewToken = Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get() && Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_VIEW_TOKEN.get().isEmpty();
        boolean missingCloudflareWorkerIngestToken = Settings.ENABLE_DC_BAN_LIST_BAN_LIST_INGEST.get() && Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_INGEST_TOKEN.get().isEmpty();

        if (missingCloudflareWorkerUrl || missingCloudflareWorkerViewToken || missingCloudflareWorkerIngestToken) {
            return Step.CLOUDFLARE_WORKER_INFO_INPUT;
        }
        if (areDcInsideSettingsMissing()) return Step.DCINSIDE_SETTINGS_INPUT;
        return resolveActionStep();
    }

    private static boolean areDcInsideSettingsMissing() {
        if (!Settings.ENABLE_DC_BAN_LIST_BAN_LIST_INGEST.get()) return false;
        return Settings.DC_BAN_LIST_GALLERIES_JSON.get().trim().isEmpty()
                || Settings.DC_BAN_LIST_DCINSIDE_USER_ID.get().trim().isEmpty()
                || Settings.DC_BAN_LIST_DCINSIDE_PASSWORD.get().isEmpty();
    }

    private Step resolveCloudflareWorkerInfoReturnStep() {
        Step returnStep = cloudflareWorkerInfoReturnStep == null
                ? resolveActionStep()
                : cloudflareWorkerInfoReturnStep;
        return returnStep == Step.ACTION_SELECTION && areDcInsideSettingsMissing()
                ? Step.DCINSIDE_SETTINGS_INPUT
                : returnStep;
    }

    @Override
    protected String getDialogTitle() {
        return "차단 내역";
    }

    @Override
    protected void renderStep() {
        super.renderStep();
    }

    @Override
    protected void buildStep(AlertDialog.Builder builder, Step step) {
        switch (step) {
            case CLOUDFLARE_WORKER_INFO_INPUT -> {
                TextView message = new TextView(context);
                message.setText("연동할 Cloudflare Worker 정보를 입력하세요.");

                int rowHeightPx = (int) (36 * context.getResources().getDisplayMetrics().density);

                String workerUrl = Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_URL.get();

                boolean identifierEnabled = Settings.ENABLE_DC_BAN_LIST_IDENTIFIER_SEARCH.get();
                String workerViewToken = Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_VIEW_TOKEN.get();

                boolean ingestEnabled = Settings.ENABLE_DC_BAN_LIST_BAN_LIST_INGEST.get();
                String workerIngestToken = Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_INGEST_TOKEN.get();

                EditText urlInput = DialogUiUtils.createInputField(context, "", "URL 입력", secondaryColor);
                urlInput.setText(workerUrl);
                LinearLayout urlRow = DialogUiUtils.createLabeledInputRow(context, "Worker URL", urlInput, rowHeightPx);

                EditText viewTokenInput = DialogUiUtils.createInputField(context, "", "View Token 입력", secondaryColor);
                viewTokenInput.setText(workerViewToken);
                LinearLayout viewTokenRow = DialogUiUtils.createLabeledInputRow(context, "View Token", viewTokenInput, rowHeightPx);
                viewTokenRow.setVisibility(identifierEnabled ? View.VISIBLE : View.GONE);

                EditText ingestTokenInput = DialogUiUtils.createInputField(context, "", "Ingest Token 입력", secondaryColor);
                ingestTokenInput.setText(workerIngestToken);
                LinearLayout ingestTokenRow = DialogUiUtils.createLabeledInputRow(context, "Ingest Token", ingestTokenInput, rowHeightPx);
                ingestTokenRow.setVisibility(ingestEnabled ? View.VISIBLE : View.GONE);

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, urlRow, viewTokenRow, ingestTokenRow);

                builder.setView(container)
                        .setPositiveButton("확인", (d, w) -> {
                            String newUrl = urlInput.getText().toString().trim();
                            String newViewToken = viewTokenInput.getText().toString().trim();
                            String newIngestToken = ingestTokenInput.getText().toString().trim();
                            if (newUrl.isEmpty() || (identifierEnabled && newViewToken.isEmpty()) || (ingestEnabled && newIngestToken.isEmpty())) {
                                Toast.makeText(context, "Cloudflare Worker 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                transitionTo(Step.CLOUDFLARE_WORKER_INFO_INPUT);
                                return;
                            }

                            Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_URL.save(newUrl);
                            if (identifierEnabled) {
                                Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_VIEW_TOKEN.save(newViewToken);
                            }
                            if (ingestEnabled) {
                                Settings.DC_BAN_LIST_CLOUDFLARE_WORKER_INGEST_TOKEN.save(newIngestToken);
                            }
                            transitionTo(resolveCloudflareWorkerInfoReturnStep());
                        })
                        .setNegativeButton("취소", (d, w) -> {
                            if (cloudflareWorkerInfoReturnStep != null) {
                                transitionTo(cloudflareWorkerInfoReturnStep);
                            }
                        });
            }

            case DCINSIDE_SETTINGS_INPUT -> {
                TextView message = new TextView(context);
                message.setText("앱에서 PC 차단 목록을 수집하기 위한 정보를 입력하세요.\n갤러리 JSON 예: [{\"id\":\"onshinproject\",\"path\":\"mgallery\"}]");

                int rowHeightPx = (int) (36 * context.getResources().getDisplayMetrics().density);
                int galleryRowHeightPx = (int) (72 * context.getResources().getDisplayMetrics().density);

                EditText galleriesInput = DialogUiUtils.createInputField(context, "", "갤러리 설정 JSON", secondaryColor);
                galleriesInput.setSingleLine(false);
                galleriesInput.setMinLines(2);
                galleriesInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                galleriesInput.setText(Settings.DC_BAN_LIST_GALLERIES_JSON.get());
                LinearLayout galleriesRow = DialogUiUtils.createLabeledInputRow(context, "Galleries", galleriesInput, galleryRowHeightPx);

                EditText userIdInput = DialogUiUtils.createInputField(context, "", "DCInside ID", secondaryColor);
                userIdInput.setSingleLine(true);
                userIdInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                userIdInput.setText(Settings.DC_BAN_LIST_DCINSIDE_USER_ID.get());
                LinearLayout userIdRow = DialogUiUtils.createLabeledInputRow(context, "DCInside ID", userIdInput, rowHeightPx);

                EditText passwordInput = DialogUiUtils.createInputField(context, "", "DCInside 비밀번호", secondaryColor);
                passwordInput.setSingleLine(true);
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordInput.setText(Settings.DC_BAN_LIST_DCINSIDE_PASSWORD.get());
                LinearLayout passwordRow = DialogUiUtils.createLabeledInputRow(context, "비밀번호", passwordInput, rowHeightPx);

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, galleriesRow, userIdRow, passwordRow);
                builder.setView(container)
                        .setPositiveButton("확인", (d, w) -> {
                            String galleriesJson = galleriesInput.getText().toString().trim();
                            String userId = userIdInput.getText().toString().trim();
                            String password = passwordInput.getText().toString();
                            if (galleriesJson.isEmpty() || userId.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "갤러리 JSON과 DCInside 계정 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                transitionTo(Step.DCINSIDE_SETTINGS_INPUT);
                                return;
                            }
                            try {
                                DcBanListApiClient.resolveGalleryPath(galleriesJson, CustomNetworkInterceptorPatch.galleryId);
                            } catch (JSONException e) {
                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                                transitionTo(Step.DCINSIDE_SETTINGS_INPUT);
                                return;
                            }

                            Settings.DC_BAN_LIST_GALLERIES_JSON.save(galleriesJson);
                            Settings.DC_BAN_LIST_DCINSIDE_USER_ID.save(userId);
                            Settings.DC_BAN_LIST_DCINSIDE_PASSWORD.save(password);
                            transitionTo(resolveActionStep());
                        })
                        .setNegativeButton("취소", null);
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

                if (Settings.ENABLE_DC_BAN_LIST_BAN_LIST_INGEST.get()) {
                    Button banListIngestButton = new Button(context);
                    banListIngestButton.setText("차단 내역 내보내기");
                    banListIngestButton.setOnClickListener(v -> {
                        currentDialog.dismiss();
                        transitionTo(Step.FETCHING_LATEST_DATA);
                    });
                    banListIngestButton.setBackground(DialogUiUtils.createOutlineButtonBackground(secondaryColor));
                    banListIngestButton.setTextColor(textColor);
                    buttons.add(banListIngestButton);
                }

                for (int i = 0; i < buttons.size(); i++) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(buttonParams);
                    if (i > 0) params.leftMargin = buttonMargin;
                    if (i < buttons.size() - 1) params.rightMargin = buttonMargin;
                    buttonRow.addView(buttons.get(i), params);
                }

                LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, buttonRow);

                builder.setView(container)
                        .setNeutralButton("Worker 정보 변경", (d, w) -> {
                            cloudflareWorkerInfoReturnStep = Step.ACTION_SELECTION;
                            transitionTo(Step.CLOUDFLARE_WORKER_INFO_INPUT);
                        })
                        .setNegativeButton("취소", null);
            }

            case IDENTIFIER_INPUT -> {
                TextView message = new TextView(context);
                message.setText("DB에서 정확히 일치하는 식별코드를 찾습니다.");

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
                            identifier = normalizeIdentifier(input.getText().toString());
                            if (identifier.isEmpty()) {
                                Toast.makeText(context, "식별코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                transitionTo(Step.IDENTIFIER_INPUT);
                                return;
                            }
                            transitionTo(Step.IDENTIFIER_FETCHING);
                        })
                        .setNeutralButton("Worker 정보 변경", (d, w) -> {
                            cloudflareWorkerInfoReturnStep = Step.IDENTIFIER_INPUT;
                            transitionTo(Step.CLOUDFLARE_WORKER_INFO_INPUT);
                        })
                        .setNegativeButton("취소", null);
            }

            case IDENTIFIER_FETCHING -> {
                builder.setTitle("식별코드 조회")
                        .setMessage("DB에서 식별코드를 찾고 있습니다...");
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

            case FETCHING_LATEST_DATA -> {
                builder.setMessage("이전 업로드 기록을 확인하고 있습니다...");
                fetchLatestData();
            }

            case INITIAL_INGEST_CONFIRMATION -> builder
                    .setMessage("DB에 기존 기록이 없습니다. 차단 목록 전체를 수집해 업로드하시겠습니까?")
                    .setPositiveButton("확인", (d, w) -> transitionTo(Step.PARSING))
                    .setNegativeButton("취소", null);

            case PARSING -> {
                builder.setMessage("디시인사이드 차단 목록을 수집 중입니다...");
                new Thread(this::parseBanList).start();
            }

            case INGEST_CONFIRMATION -> builder
                    .setMessage(banListJsonArray.length() + "건의 신규 차단내역을 업로드하시겠습니까?")
                    .setPositiveButton("확인", (d, w) -> transitionTo(Step.INGEST_IN_PROGRESS))
                    .setNegativeButton("취소", null);

            case INGEST_IN_PROGRESS -> {
                builder.setMessage("Worker로 데이터를 전송 중입니다...");
                mainHandler.post(this::executeIngestTask);
            }

            case INGEST_COMPLETE -> builder
                    .setMessage(ingestResultMessage)
                    .setPositiveButton("확인", null);

            case INGEST_UNNECESSARY -> builder
                    .setMessage("0건의 데이터가 수집되었습니다.\n업로드가 필요하지 않습니다.")
                    .setPositiveButton("확인", null);

            case ERROR -> builder
                    .setMessage(errorMessage)
                    .setPositiveButton("재시도", (d, w) -> transitionTo(Step.ACTION_SELECTION))
                    .setNegativeButton("취소", null);
        }
    }

    private void fetchLatestData() {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("action", "getLatestData");
                payload.put("galleryId", CustomNetworkInterceptorPatch.galleryId);

                CloudflareWorkerClient.sendPost("", payload.toString(), CloudflareWorkerClient.TokenType.VIEW, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "getLatestData 조회 실패", e);
                        errorMessage = "이전 업로드 기록을 확인하지 못했습니다.\n(네트워크 오류 또는 응답 지연: " + e.getClass().getSimpleName() + ")";
                        transitionTo(Step.ERROR);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (response) {
                            if (!response.isSuccessful()) {
                                Log.e(TAG, "getLatestData HTTP 오류: " + response.code());
                                errorMessage = "이전 업로드 기록을 확인하지 못했습니다.\n(HTTP " + response.code() + ")";
                                transitionTo(Step.ERROR);
                                return;
                            }

                            String responseText = response.body().string().trim();
                            JSONObject jsonResponse = new JSONObject(responseText);

                            if ("success".equals(jsonResponse.optString("status"))) {
                                latestData = jsonResponse.optJSONObject("latestData"); // 최초 기록이 없으면 null일 수 있음 - 정상
                            } else {
                                String serverMessage = jsonResponse.optString("message", "Unknown Server Error");
                                Log.e(TAG, "getLatestData 서버 응답 실패: " + serverMessage);
                                errorMessage = "이전 업로드 기록을 확인하지 못했습니다.\n(" + serverMessage + ")";
                                transitionTo(Step.ERROR);
                                return;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "getLatestData 응답 파싱 실패", e);
                            errorMessage = "이전 업로드 기록 응답을 해석하지 못했습니다.";
                            transitionTo(Step.ERROR);
                            return;
                        }
                        if (latestData == null || latestData.length() == 0) {
                            transitionTo(Step.INITIAL_INGEST_CONFIRMATION);
                        }
                        else {
                            transitionTo(Step.PARSING);
                        }
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "getLatestData 요청 준비 중 예외", e);
                errorMessage = "요청 준비 중 오류가 발생했습니다.\n(" + e.getClass().getSimpleName() + ")";
                transitionTo(Step.ERROR);
            }
        }).start();
    }

    /** 앱에서 DCInside PC 차단내역 페이지를 로그인 후 순차적으로 요청/파싱 */
    private void parseBanList() {
        String galleryId = CustomNetworkInterceptorPatch.galleryId;

        JSONArray collected = new JSONArray();

        try {
            String galleryPath = DcBanListApiClient.resolveGalleryPath(
                    Settings.DC_BAN_LIST_GALLERIES_JSON.get(), galleryId);
            DcBanListApiClient.AuthenticatedSession session = DcBanListApiClient.login(
                    Settings.DC_BAN_LIST_DCINSIDE_USER_ID.get(),
                    Settings.DC_BAN_LIST_DCINSIDE_PASSWORD.get());

            Document firstPage = DcBanListApiClient.fetchBanListPageSync(session, galleryPath, galleryId, 1);
            int lastPage = DcBanListPcHtmlParser.extractLastPage(firstPage, 1);
            boolean shouldStop = appendRecordsUntilDuplicate(
                    collected, DcBanListPcHtmlParser.parsePage(firstPage));
            if (shouldStop || lastPage <= 1) {
                finishParsing(collected);
                return;
            }

            for (int page = 2; page <= lastPage; page++) {
                Document document = DcBanListApiClient.fetchBanListPageSync(
                        session, galleryPath, galleryId, page);
                lastPage = Math.max(lastPage, DcBanListPcHtmlParser.extractLastPage(document, page));
                shouldStop = appendRecordsUntilDuplicate(
                        collected, DcBanListPcHtmlParser.parsePage(document));
                if (shouldStop) break;
            }

            finishParsing(collected);
        } catch (IOException e) {
            Log.e(TAG, "PC 차단내역 수집 중 네트워크 오류", e);
            errorMessage = "PC 차단내역을 수집하지 못했습니다.\n(" + e.getMessage() + ")";
            transitionTo(Step.ERROR);
        } catch (JSONException e) {
            Log.e(TAG, "갤러리 설정 또는 PC 차단내역 파싱 오류", e);
            errorMessage = "갤러리 설정을 읽거나 PC 차단내역을 파싱하지 못했습니다.\n(" + e.getMessage() + ")";
            transitionTo(Step.ERROR);
        } catch (Exception e) {
            Log.e(TAG, "PC 차단내역 파싱 중 예외 발생", e);
            errorMessage = "PC 차단내역 처리 중 오류가 발생했습니다.\n(" + e.getClass().getSimpleName() + ")";
            transitionTo(Step.ERROR);
        }
    }

    private void finishParsing(JSONArray collected) {
        if (collected.length() == 0) {
            transitionTo(Step.INGEST_UNNECESSARY);
            return;
        }

        banListJsonArray = collected;
        transitionTo(Step.INGEST_CONFIRMATION);
    }

    /**
     * 파싱된 레코드들을 collected에 순서대로 담다가, latestData와 동일한 레코드를 만나면 중단.
     *
     * @return true면 중복 레코드를 만나 중단됨(더 이상 다음 페이지를 요청하지 않아도 됨)
     */
    private boolean appendRecordsUntilDuplicate(JSONArray collected, JSONArray pageRecords) throws JSONException {
        for (int i = 0; i < pageRecords.length(); i++) {
            JSONObject record = pageRecords.getJSONObject(i);
            if (latestData != null && isSameEntry(record, latestData)) {
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

    private void executeIngestTask() {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("action", "ingest");
                payload.put("galleryId", CustomNetworkInterceptorPatch.galleryId);
                payload.put("banList", banListJsonArray);
                payload.put("updateRefreshStatus", true);

                CloudflareWorkerClient.sendPost("", payload.toString(), CloudflareWorkerClient.TokenType.INGEST, true, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e(TAG, "Cloudflare Worker 업로드 요청 실패", e);
                        transitionTo(Step.ERROR);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (response) {
                            if (!response.isSuccessful()) {
                                Log.e(TAG, "Cloudflare Worker HTTP 오류: " + response.code());
                                String responseText = response.body() == null ? "" : response.body().string();
                                String serverMessage = "HTTP " + response.code();
                                try {
                                    serverMessage = new JSONObject(responseText).optString("message", serverMessage);
                                } catch (JSONException ignored) {
                                    // 응답 본문이 JSON이 아니면 HTTP 상태 코드를 표시한다.
                                }
                                errorMessage = "Cloudflare Worker가 업로드를 시작하지 않았습니다.\n(" + serverMessage + ")";
                                transitionTo(Step.ERROR);
                                return;
                            }

                            String responseText = response.body().string().trim();
                            JSONObject jsonResponse = new JSONObject(responseText);

                            if ("success".equals(jsonResponse.optString("status"))) {
                                JSONObject database = jsonResponse.optJSONObject("database");
                                JSONObject snapshot = jsonResponse.optJSONObject("snapshot");
                                int inserted = database != null
                                        ? database.optInt("inserted", jsonResponse.optInt("inserted", banListJsonArray.length()))
                                        : jsonResponse.optInt("inserted", banListJsonArray.length());

                                StringBuilder resultMessage = new StringBuilder()
                                        .append("D1 저장 완료: ").append(inserted).append("건");
                                if (snapshot == null) {
                                    resultMessage.append("\nKV 갱신 상태를 응답에서 확인할 수 없습니다.");
                                } else {
                                    switch (snapshot.optString("status")) {
                                        case "updated" -> {
                                            resultMessage.append("\nKV 스냅샷 갱신 완료: ")
                                                    .append(snapshot.optInt("rows")).append("건");
                                            if (!snapshot.optBoolean("cachePurged", true)) {
                                                resultMessage.append("\n단, CDN 캐시 비우기에 실패했습니다: ")
                                                        .append(snapshot.optString("message", "알 수 없는 오류"))
                                                        .append("\nD1 저장은 완료됐으므로 업로드를 다시 시도하지 마세요.");
                                            }
                                        }
                                        case "failed" -> resultMessage.append("\nKV 스냅샷 갱신 실패: ")
                                                .append(snapshot.optString("message", "알 수 없는 오류"))
                                                .append("\nD1 저장은 완료됐습니다. 업로드를 다시 시도하지 마세요.");
                                        case "skipped" -> resultMessage.append("\nKV 스냅샷 갱신은 생략되었습니다.");
                                        default -> resultMessage.append("\nKV 갱신 상태를 확인할 수 없습니다.");
                                    }
                                }
                                JSONObject refreshStatus = jsonResponse.optJSONObject("refreshStatus");
                                if (refreshStatus == null) {
                                    resultMessage.append("\n웹 갱신 상태를 업데이트했는지 확인할 수 없습니다.");
                                } else if (refreshStatus.optBoolean("updated")) {
                                    resultMessage.append("\n웹 갱신 상태도 업데이트했습니다.");
                                } else {
                                    resultMessage.append("\n웹 갱신 상태는 업데이트하지 않았습니다: ")
                                            .append(refreshStatus.optString("message", "알 수 없는 오류"));
                                }
                                ingestResultMessage = resultMessage.toString();
                                transitionTo(Step.INGEST_COMPLETE);
                            } else {
                                String serverMessage = jsonResponse.optString("message", "Unknown Server Error");
                                Log.e(TAG, "Cloudflare Worker 업로드 실패: " + serverMessage);
                                errorMessage = "Cloudflare Worker 업로드에 실패했습니다.\n(" + serverMessage + ")";
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

    private void fetchIdentifierResults() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("action", "searchIdentifier");
            payload.put("galleryId", CustomNetworkInterceptorPatch.galleryId);
            payload.put("identifier", identifier);
        } catch (JSONException e) {
            identifierErrorMessage = "요청을 만들지 못했습니다.";
            transitionTo(Step.IDENTIFIER_ERROR);
            return;
        }

        CloudflareWorkerClient.sendPost("", payload.toString(), CloudflareWorkerClient.TokenType.VIEW, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException error) {
                identifierErrorMessage = "데이터를 불러오지 못했습니다.";
                transitionTo(Step.IDENTIFIER_ERROR);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful()) {
                        identifierErrorMessage = "데이터를 불러오지 못했습니다. (HTTP " + response.code() + ")";
                        transitionTo(Step.IDENTIFIER_ERROR);
                        return;
                    }
                    JSONObject json = new JSONObject(response.body().string());
                    if (!"success".equals(json.optString("status"))) {
                        identifierErrorMessage = "조회 실패: " + json.optString("message", "Unknown Server Error");
                        transitionTo(Step.IDENTIFIER_ERROR);
                        return;
                    }
                    List<String> headers = new ArrayList<>();
                    JSONArray headerArray = json.getJSONArray("headers");
                    for (int i = 0; i < headerArray.length(); i++) headers.add(headerArray.getString(i));
                    List<JSONObject> rows = new ArrayList<>();
                    JSONArray rowArray = json.getJSONArray("rows");
                    for (int i = 0; i < rowArray.length(); i++) rows.add(rowArray.getJSONObject(i));
                    identifierHeaders = headers;
                    identifierResults = rows;
                    transitionTo(Step.IDENTIFIER_RESULT);
                } catch (JSONException error) {
                    identifierErrorMessage = "응답 형식을 읽지 못했습니다.";
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
                    titleView.setTextColor(textColor);
                    detailView = new TextView(context);
                    detailView.setTextSize(13);
                    detailView.setTextColor(secondaryColor);
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
                titleView.setText(item == null ? "" : item.optString(CONTENT_COLUMN, ""));
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

    private static String normalizeIdentifier(String value) {
        String s = value == null ? "" : value.trim();
        if (s.endsWith("+ IP")) s = s.substring(0, s.length() - "+ IP".length());
        return s.trim();
    }
}
