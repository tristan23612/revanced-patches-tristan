package app.revanced.extension.dcinside.patches.misc.dcBanList;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;

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
            CHECKING_AUTH,         // 추가: 인증 상태 검증 중 단계
            SHEET_ID_CONFIRMATION,
            PARSING,
            UPLOAD_IN_PROGRESS,
            UPLOAD_COMPLETE,
            ERROR
        }

        private final Context context;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private Step currentStep = Step.OAUTH_CONFIRMATION;
        private AlertDialog currentDialog;

        private String targetSheetId = "";

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
                                transitionTo(Step.PARSING);
                            })
                            .setNegativeButton("취소", null);
                }

                case PARSING -> {
                    builder.setMessage("디시인사이드 차단 목록을 수집 중입니다...");
                    mainHandler.post(this::executeParsingTask);
                }

                case UPLOAD_IN_PROGRESS -> builder.setMessage("구글 시트로 데이터를 전송 중입니다...");

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
                currentDialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                );
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

        private void executeParsingTask() {
            new Thread(() -> {
                try {
                    // 1. 차단 목록 데이터 및 갤러리 정보 추출 (실제 파싱 로직 적용 구간)
                    JSONArray banListJsonArray = new JSONArray(); // 차단 목록 JSONArray 변환

                    // 2. GAS API 규격에 맞는 JSON Payload 구성 (org.json 활용으로 이스케이프 오류 방지)
                    JSONObject payload = new JSONObject();
                    payload.put("action", "uploadToGoogleSheet");
                    payload.put("sheetId", targetSheetId);
                    payload.put("galleryId", JsonHookPatch.galleryId);
                    payload.put("banList", banListJsonArray);

                    transitionTo(Step.UPLOAD_IN_PROGRESS);

                    // 3. POST 요청 전송 (개편된 GasApiClient.sendPost 호출)
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

                                // 4. 응답 JSON 파싱 및 status 속성 검증
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