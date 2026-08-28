package app.revanced.extension.dcinside.patches.management.floatingButton;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * 플로팅 버튼 다이얼로그들의 공통 "상태 머신" 뼈대. GallScopeSession의 구조를 그대로 기반형으로 삼는다.
 * <p>
 * 서브클래스는 자신의 Step enum({@code S})을 정의하고, {@link #buildStep(AlertDialog.Builder, Enum)}에서
 * 각 스텝에 맞는 다이얼로그 내용을 채운다. 상태 전환({@link #transitionTo(Enum)})과 다이얼로그
 * 재생성/표시 흐름은 이 클래스가 담당한다.
 * <p>
 * GallScope와 겹치지 않는 세션 고유 로직(예: DcBanListSession의 자동 진행, window inset 대응)은
 * 베이스에 훅을 두지 않고, 해당 세션이 {@link #renderStep()}을 오버라이드해서 직접 얹는다.
 *
 * @param <S> 이 세션이 사용하는 Step enum 타입
 */
public abstract class DialogSession<S extends Enum<S>> {

    protected final Context context;
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());
    protected final int textColor;
    protected final int secondaryColor;

    protected S currentStep;
    protected AlertDialog currentDialog;

    protected DialogSession(Context context, S initialStep) {
        this.context = context;
        this.currentStep = initialStep;
        this.textColor = DialogUiUtils.resolveDialogTextColor(context);
        this.secondaryColor = DialogUiUtils.applyAlpha(textColor, 0x80);
    }

    /**
     * 다이얼로그 제목. 스텝마다 제목이 달라져야 하면 {@link #buildStep}에서
     * {@code builder.setTitle(...)}로 덮어써도 된다(AlertDialog.Builder는 재설정을 허용한다).
     */
    protected abstract String getDialogTitle();

    /**
     * 현재 스텝(step)에 맞는 다이얼로그 내용을 builder에 채운다.
     * 필요하다면 이 안에서 비동기 작업을 시작하고, 완료 시 {@link #transitionTo(Enum)}으로 다음 스텝으로 넘어간다.
     */
    protected abstract void buildStep(AlertDialog.Builder builder, S step);

    public void start() {
        renderStep();
    }

    /**
     * GallScopeSession의 원래 renderStep() 흐름 그대로: 기존 다이얼로그 dismiss →
     * Builder 생성 → buildStep()으로 내용 채움 → show(). 세션 고유의 부가 동작(자동 진행 판단,
     * show 이후 후처리 등)이 필요한 서브클래스는 이 메서드를 오버라이드해서 앞뒤로 로직을 덧붙인다.
     */
    protected void renderStep() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        int dialogThemeResId = DialogUiUtils.resolveDialogTheme(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogThemeResId)
                .setTitle(getDialogTitle())
                .setCancelable(true);

        buildStep(builder, currentStep);

        currentDialog = builder.create();
        currentDialog.show();
    }

    protected void transitionTo(S nextStep) {
        this.currentStep = nextStep;
        mainHandler.post(this::renderStep);
    }
}