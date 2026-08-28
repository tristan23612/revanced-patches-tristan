package app.revanced.extension.dcinside.patches.management.floatingButton;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

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

    protected abstract String getDialogTitle();

    protected abstract void buildStep(AlertDialog.Builder builder, S step);

    public void start() {
        renderStep();
    }

    protected void renderStep() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(getDialogTitle())
                .setCancelable(true);

        buildStep(builder, currentStep);

        currentDialog = builder.create();

        if (currentDialog.getWindow() != null) {
            android.widget.EditText temp = new android.widget.EditText(context);
            currentDialog.getWindow().setBackgroundDrawable(temp.getBackground());
        }

        currentDialog.show();
    }

    protected void transitionTo(S nextStep) {
        this.currentStep = nextStep;
        mainHandler.post(this::renderStep);
    }
}