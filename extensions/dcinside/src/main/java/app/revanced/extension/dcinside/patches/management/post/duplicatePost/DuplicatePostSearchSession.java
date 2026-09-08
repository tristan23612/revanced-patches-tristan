package app.revanced.extension.dcinside.patches.management.post.duplicatePost;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;

import app.revanced.extension.dcinside.patches.management.DialogSession;
import app.revanced.extension.dcinside.patches.management.DialogUiUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

@SuppressLint({"DiscouragedApi", "SetTextI18n"})
final class DuplicatePostSearchSession extends DialogSession<DuplicatePostSearchSession.Step> {
    private static final String TAG = "ReVanced_DCInside";

    enum Step {
        LOADING,
        RESULT,
        ERROR
    }

    private final String title;
    private List<DuplicatePostSearchResult> results = new ArrayList<>();
    private String errorMessage = "검색 요청 중 오류가 발생했습니다.";

    DuplicatePostSearchSession(Context context, String title) {
        super(context, Step.LOADING);
        this.title = title;
    }

    @Override
    protected String getDialogTitle() {
        return "순회검사";
    }

    @Override
    protected void renderStep() {
        super.renderStep();
        if (currentStep == Step.LOADING) {
            fetchResults();
        }
    }

    private void fetchResults() {
        DuplicatePostSearchApiClient.fetchSearchResult(title, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Search request failed", e);
                errorMessage = "검색 요청 중 오류가 발생했습니다.";
                transitionTo(Step.ERROR);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful()) {
                        errorMessage = "검색 요청 중 오류가 발생했습니다. (HTTP " + response.code() + ")";
                        transitionTo(Step.ERROR);
                        return;
                    }
                    Document doc = Jsoup.parse(response.body().string());
                    results = DuplicatePostSearchHtmlParser.parse(doc);
                    transitionTo(Step.RESULT);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing search result", e);
                    errorMessage = "검색 결과를 읽어오지 못했습니다.";
                    transitionTo(Step.ERROR);
                }
            }
        });
    }

    @Override
    protected void buildStep(AlertDialog.Builder builder, Step step) {
        switch (step) {
            case LOADING -> builder
                    .setMessage("\"" + title + "\" 검색 중입니다...")
                    .setNegativeButton("취소", null);

            case ERROR -> builder
                    .setMessage(errorMessage)
                    .setPositiveButton("확인", null);

            case RESULT -> showResults(builder);
        }
    }

    private void showResults(AlertDialog.Builder builder) {
        List<DuplicatePostSearchResult> snapshot = new ArrayList<>(results);

        if (snapshot.isEmpty()) {
            builder.setMessage("\"" + title + "\" 검색 결과가 없습니다.")
                    .setPositiveButton("닫기", null);
            return;
        }

        TextView message = new TextView(context);
        message.setText("\"" + title + "\" 검색 결과\n" + snapshot.size() + "건");

        ListView listView = getResultListView(snapshot);
        LinearLayout container = DialogUiUtils.wrapWithPadding(context, message, listView);

        builder.setView(container)
                .setPositiveButton("닫기", null);
    }

    @NonNull
    private ListView getResultListView(List<DuplicatePostSearchResult> snapshot) {
        ListView listView = DialogUiUtils.createHeightLimitedListView(context, 0.5f);
        listView.setAdapter(new ArrayAdapter<DuplicatePostSearchResult>(context, 0, snapshot) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                LinearLayout row;
                TextView titleView;
                TextView contentView;
                TextView subView;
                if (convertView == null) {
                    row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.VERTICAL);
                    int padding = (int) (8 * context.getResources().getDisplayMetrics().density);
                    row.setPadding(padding, padding, padding, padding);

                    titleView = new TextView(context);
                    titleView.setSingleLine(true);
                    titleView.setEllipsize(TextUtils.TruncateAt.END);
                    titleView.setTextSize(16);

                    contentView = new TextView(context);
                    contentView.setSingleLine(true);
                    contentView.setEllipsize(TextUtils.TruncateAt.END);
                    contentView.setTextSize(16);

                    subView = new TextView(context);
                    subView.setTextSize(13);
                    subView.setTextColor(DialogUiUtils.resolveSecondaryTextColor(context));

                    row.addView(titleView);
                    row.addView(contentView);
                    row.addView(subView);
                    row.setTag(new View[]{titleView, contentView, subView});
                } else {
                    row = (LinearLayout) convertView;
                    View[] views = (View[]) row.getTag();
                    titleView = (TextView) views[0];
                    contentView = (TextView) views[1];
                    subView = (TextView) views[2];
                }

                DuplicatePostSearchResult item = getItem(position);
                titleView.setText(item == null ? "" : item.title());
                contentView.setText(item == null ? "" : item.content());
                subView.setText(item == null ? "" : (item.galleryName() + " " + item.dateTime()));
                return row;
            }
        });
        return listView;
    }
}