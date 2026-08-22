package app.revanced.extension.dcinside.patches.misc.floatingButton.dcBanList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Insets;
import android.os.Build;
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

import app.revanced.extension.dcinside.settings.Settings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Objects;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.revanced.extension.dcinside.patches.hook.json.JsonHookPatch;

@SuppressLint("DiscouragedApi")
public class DcBanListPatch {
    private static final String TAG = "ReVanced_DCInside";

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
            dcBanListButton.setOnClickListener(buttonView -> showExportDialog(buttonView.getContext()));
            dcBanListButton.setVisibility(visible && Settings.SHOW_DC_BAN_LIST_BUTTON.get() && JsonHookPatch.managerSkill ? View.VISIBLE : View.GONE);
        }
    }

    private static void showExportDialog(Context context) {
        new ExportSession(context).start();
    }

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
            NEED_AUTHORIZATION,
            SHEET_ID_CONFIRMATION,
            FETCHING_LAST_RECORD,
            CREATE_SHEET_CONFIRMATION,
            PARSING,
            UPLOAD_CONFIRMATION,
            UPLOAD_IN_PROGRESS,
            UPLOAD_COMPLETE,
            UPLOAD_UNNECESSARY,
            ERROR
        }

        private final Context context;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private Step currentStep = Step.OAUTH_CONFIRMATION;
        private AlertDialog currentDialog;

        private boolean autoProcess = false;
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
            if (autoProcess && tryAutoAdvance()) {
                return;
            }

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
                        .setNeutralButton("자동 진행", (d, w) -> {
                            autoProcess = true;
                            transitionTo(Step.OAUTH_CONFIRMATION); // renderStep 재진입 → tryAutoAdvance가 가로챔
                        })
                        .setPositiveButton("권한 확인", (d, w) -> checkAuthStatus()) // 수정: 비동기 검증 실행
                        .setNegativeButton("취소", null);

                case CHECKING_AUTH -> builder
                        .setMessage("구글 계정 권한을 확인하고 있습니다...");

                case NEED_AUTHORIZATION -> builder
                        .setMessage("""
                                GAS 인증에 실패하였습니다.
                                아래 경로에서 인증을 진행해주세요.
                                설정 > ReVanced 설정 > 기타 > 플로팅 버튼 > DC BanList > GAS 인증""")
                        .setNegativeButton("취소", null);

                case SHEET_ID_CONFIRMATION -> {
                    EditText input = new EditText(context);
                    input.setHint("스프레드시트 ID 입력");
                    input.setHintTextColor(0xFF9E9E9E);
                    String prefill = targetSheetId.isEmpty() ? loadSavedSheetId() : targetSheetId;
                    if (!prefill.isEmpty()) {
                        input.setText(prefill);
                    }

                    FrameLayout container = new FrameLayout(context);
                    int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
                    container.setPadding(padding, 0, padding, 0);
                    container.addView(input);

                    builder.setMessage("연동할 구글 스프레드시트 ID를 입력하세요.")
                            .setView(container)
                            .setPositiveButton("확인", (d, w) -> {
                                String id = input.getText().toString().trim();
                                if (id.isEmpty()) {
                                    Toast.makeText(context, "시트 ID를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                    transitionTo(Step.SHEET_ID_CONFIRMATION);
                                    return;
                                }
                                targetSheetId = id;
                                saveSheetIdToMap(id);
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

            currentDialog = builder.create();
            currentDialog.show();

            if (currentDialog.getWindow() != null) {
                currentDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    currentDialog.getWindow().setDecorFitsSystemWindows(false);
                }

                View rootView = currentDialog.findViewById(android.R.id.content);
                rootView.setOnApplyWindowInsetsListener((view, insets) -> {
                    // Framework API 직접 사용
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Insets imeInsets = insets.getInsets(WindowInsets.Type.ime());
                        Insets systemBarsInsets = insets.getInsets(WindowInsets.Type.systemBars());

                        int bottomPadding = Math.max(Objects.requireNonNull(imeInsets).bottom, Objects.requireNonNull(systemBarsInsets).bottom);
                        view.setPadding(
                                systemBarsInsets.left,
                                systemBarsInsets.top,
                                systemBarsInsets.right,
                                bottomPadding
                        );

                        return WindowInsets.CONSUMED;
                    }
                    return insets;
                });
            }
        }

        private boolean tryAutoAdvance() {
            return switch (currentStep) {
                case OAUTH_CONFIRMATION -> {
                    checkAuthStatus();
                    yield true;
                }
                case SHEET_ID_CONFIRMATION -> {
                    String savedSheetId = loadSavedSheetId();
                    if (savedSheetId.isEmpty()) {
                        yield false; // 저장된 값 없으면 사용자 입력 필요 → 다이얼로그 띄움
                    }
                    targetSheetId = savedSheetId;
                    transitionTo(Step.FETCHING_LAST_RECORD);
                    yield true;
                }
                case CREATE_SHEET_CONFIRMATION -> {
                    transitionTo(Step.PARSING);
                    yield true;
                }
                case UPLOAD_CONFIRMATION -> false;
                case UPLOAD_COMPLETE -> false;
                default -> false; // CHECKING_AUTH, PARSING, UPLOAD_IN_PROGRESS, ERROR 등은 원래도 버튼 없는 진행 단계
            };
        }

        private void transitionTo(Step nextStep) {
            this.currentStep = nextStep;
            mainHandler.post(this::renderStep);
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
                                transitionTo(Step.SHEET_ID_CONFIRMATION);
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

        private String loadSavedSheetId() {
            try {
                String json = Settings.DC_BAN_LIST_SHEET_ID_MAP.get();
                if (json.isEmpty()) return "";
                JSONObject map = new JSONObject(json);
                return map.optString(JsonHookPatch.galleryId, "");
            } catch (JSONException e) {
                Log.w(TAG, "sheetId 맵 파싱 실패", e);
                return "";
            }
        }

        private void saveSheetIdToMap(String sheetId) {
            try {
                String json = Settings.DC_BAN_LIST_SHEET_ID_MAP.get();
                JSONObject map = json.isEmpty() ? new JSONObject() : new JSONObject(json);
                map.put(JsonHookPatch.galleryId, sheetId);
                Settings.DC_BAN_LIST_SHEET_ID_MAP.save(map.toString());
            } catch (JSONException e) {
                Log.e(TAG, "sheetId 맵 저장 실패", e);
            }
        }
    }
}