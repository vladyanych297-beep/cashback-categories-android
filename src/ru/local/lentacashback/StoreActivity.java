package ru.local.lentacashback;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;

abstract class StoreActivity extends Activity {
    private enum Phase { IDLE, HISTORY, DETAIL, CATEGORIES }
    private final Handler handler = new Handler();
    private Phase phase = Phase.IDLE;
    private WebView web;
    private ScrollView dashboard;
    private LinearLayout dashboardBody;
    private TextView status;
    private JSONArray receipts = new JSONArray();
    private JSONArray categories = new JSONArray();
    private String categoryMonth = "";
    private int targetIndex = -1;
    private int receiptCount = 0;
    private int attempts = 0;
    private boolean evaluating;
    private boolean showingBrowser;

    protected abstract String storeName();
    protected abstract int storeColor();
    protected abstract String storageName();
    protected abstract String loginUrl();
    protected abstract String historyUrl();
    protected abstract String categoriesUrl();
    protected abstract String historyScript();
    protected abstract String detailScript();
    protected abstract String categoriesScript();
    protected abstract String applyRecommendationsScript(String namesJson);
    protected abstract String openReceiptScript(int index);
    protected abstract String loadMoreScript();
    protected abstract boolean allowedHost(String host);
    protected void resetHistoryPeriods() {}
    protected boolean advanceHistoryPeriod() { return false; }
    protected String historyPeriodLabel() { return ""; }
    protected LocalDate analysisStartDate() { return LocalDate.now().minusMonths(3); }
    protected String analysisPeriodLabel() { return "последние 3 месяца"; }
    protected boolean includeReceipt(JSONObject receipt) { return true; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            receipts = new JSONArray(getSharedPreferences(storageName(), MODE_PRIVATE).getString("receipts", "[]"));
            receipts = deduplicateReceipts(receipts);
            getSharedPreferences(storageName(), MODE_PRIVATE).edit().putString("receipts", receipts.toString()).apply();
            categories = new JSONArray(getSharedPreferences(storageName(), MODE_PRIVATE).getString("categories", "[]"));
            categoryMonth = getSharedPreferences(storageName(), MODE_PRIVATE).getString("month", "");
        } catch (Exception ignored) {}
        makeUi();
        renderDashboard();
        web.loadUrl(loginUrl());
    }

    private void makeUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 249, 252));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        getWindow().setStatusBarColor(storeColor());
        TextView title = new TextView(this);
        title.setText(storeName());
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setPadding(dp(16), dp(14), dp(16), dp(14));
        title.setBackgroundColor(storeColor());
        root.addView(title);

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(6), dp(5), dp(6), dp(4));
        root.addView(actions);
        addButton(actions, "Назад", v -> finish());
        addButton(actions, "Обзор", v -> showDashboard());
        addButton(actions, "Войти", v -> { phase = Phase.IDLE; showBrowser(); web.loadUrl(loginUrl()); });
        addButton(actions, "Обновить", v -> startSync());
        status = new TextView(this);
        status.setPadding(dp(16), dp(4), dp(16), dp(8));
        status.setTextColor(Color.rgb(68, 76, 88));
        root.addView(status);

        dashboard = new ScrollView(this);
        dashboardBody = new LinearLayout(this);
        dashboardBody.setOrientation(LinearLayout.VERTICAL);
        dashboardBody.setPadding(dp(18), dp(8), dp(18), dp(28));
        dashboard.addView(dashboardBody);
        root.addView(dashboard, new LinearLayout.LayoutParams(-1, 0, 1));

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString().replace("; wv", "").replace("Version/4.0 ", ""));
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if ("https".equalsIgnoreCase(uri.getScheme()) && host != null && allowedHost(host)) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) { scheduleTick(900); }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        web.setVisibility(View.GONE);
    }

    private void startSync() {
        resetHistoryPeriods();
        receipts = deduplicateReceipts(receipts);
        getSharedPreferences(storageName(), MODE_PRIVATE).edit().putString("receipts", receipts.toString()).apply();
        phase = Phase.HISTORY;
        targetIndex = -1;
        receiptCount = 0;
        attempts = 0;
        String period = historyPeriodLabel();
        say("Считываю чеки " + storeName() + (period.isEmpty() ? "" : ": " + period) + "…");
        showBrowser();
        web.loadUrl(historyUrl());
        scheduleTick(1100);
    }

    private void tick() {
        if (phase == Phase.IDLE || evaluating) return;
        if (++attempts > 150) { phase = Phase.IDLE; say("Не удалось прочитать страницу. Проверьте вход и повторите обновление."); return; }
        String url = web.getUrl();
        try {
            String host = url == null ? null : Uri.parse(url).getHost();
            if (host == null || !allowedHost(host)) { phase = Phase.IDLE; say("Завершите вход, затем нажмите «Обновить»."); return; }
        } catch (Exception error) { phase = Phase.IDLE; say("Не удалось определить страницу входа."); return; }
        evaluating = true;
        if (phase == Phase.HISTORY) evalJson(historyScript(), this::processHistory);
        else if (phase == Phase.DETAIL) evalJson(detailScript(), this::processDetail);
        else if (phase == Phase.CATEGORIES) evalJson(categoriesScript(), this::processCategories);
    }

    private void processHistory(JSONObject data) {
        if (data.optBoolean("preparing")) {
            String period = historyPeriodLabel();
            say("Подготавливаю историю " + storeName() + (period.isEmpty() ? "" : ": " + period) + "…");
            scheduleTick(1200);
            return;
        }
        if (!data.optBoolean("ready")) { scheduleTick(900); return; }
        int count = data.optInt("count");
        if (data.optBoolean("more")) {
            say("Загружаю историю: найдено чеков " + count + "…");
            web.evaluateJavascript(loadMoreScript(), null);
            scheduleTick(1300);
            return;
        }
        if (targetIndex < 0) {
            if (count == 0) { finishHistoryPeriod(); return; }
            receiptCount = count;
            targetIndex = 0;
            attempts = 0;
        }
        if (targetIndex >= receiptCount) { finishHistoryPeriod(); return; }
        phase = Phase.DETAIL;
        String period = historyPeriodLabel();
        say((period.isEmpty() ? "" : period + ": ") + "чек " + (targetIndex + 1) + " из " + receiptCount + "…");
        web.evaluateJavascript(openReceiptScript(targetIndex), null);
        scheduleTick(1000);
    }

    private void processDetail(JSONObject data) {
        if (!data.optBoolean("ready")) { scheduleTick(850); return; }
        String id = receiptKey(data);
        boolean exists = false;
        for (int i = 0; i < receipts.length(); i++) {
            JSONObject receipt = receipts.optJSONObject(i);
            if (receipt != null && id.equals(receiptKey(receipt))) { exists = true; break; }
        }
        if (!exists && includeReceipt(data)) receipts.put(data);
        getSharedPreferences(storageName(), MODE_PRIVATE).edit().putString("receipts", receipts.toString()).apply();
        targetIndex++;
        attempts = 0;
        phase = Phase.HISTORY;
        if (targetIndex >= receiptCount) finishHistoryPeriod();
        else { web.loadUrl(historyUrl()); scheduleTick(1000); }
    }

    private void finishHistoryPeriod() {
        if (advanceHistoryPeriod()) {
            phase = Phase.HISTORY;
            targetIndex = -1;
            receiptCount = 0;
            attempts = 0;
            say("Загружаю " + historyPeriodLabel() + "…");
            web.loadUrl(historyUrl());
            scheduleTick(1100);
        } else {
            openCategories();
        }
    }

    private void openCategories() {
        phase = Phase.CATEGORIES;
        attempts = 0;
        say("Считываю категории " + storeName() + "…");
        web.loadUrl(categoriesUrl());
        scheduleTick(1000);
    }

    private void processCategories(JSONObject data) {
        if (!data.optBoolean("ready")) { scheduleTick(900); return; }
        categories = data.optJSONArray("items");
        if (categories == null) categories = new JSONArray();
        categoryMonth = data.optString("month");
        getSharedPreferences(storageName(), MODE_PRIVATE).edit().putString("categories", categories.toString()).putString("month", categoryMonth).apply();
        phase = Phase.IDLE;
        JSONArray names = recommendedNames();
        if (names.length() == 0) {
            say("Готово: " + receipts.length() + " чеков, " + categories.length() + " категорий. Подходящих категорий не найдено.");
            showDashboard();
            return;
        }
        say("Применяю рекомендованные категории " + storeName() + "…");
        evaluating = true;
        evalJson(applyRecommendationsScript(names.toString()), this::processAppliedRecommendations);
    }

    private JSONArray recommendedNames() {
        JSONArray names = new JSONArray();
        List<RecommendationEngine.Recommendation> ranked = RecommendationEngine.rankBetween(receipts, categories, analysisStartDate(), LocalDate.now());
        for (RecommendationEngine.Recommendation recommendation : ranked) {
            if (names.length() >= 5 || recommendation.spend <= 0) break;
            names.put(recommendation.name);
        }
        return names;
    }

    private void processAppliedRecommendations(JSONObject data) {
        final int count = data.optInt("count");
        handler.postDelayed(() -> {
            say(count > 0
                    ? "Готово: автоматически применено рекомендаций — " + count + "."
                    : "Готово: рекомендованные категории уже применены или недоступны для выбора.");
            showDashboard();
        }, 1800);
    }

    private interface Result { void accept(JSONObject data); }
    private void evalJson(String script, Result result) {
        web.evaluateJavascript(script, value -> {
            evaluating = false;
            try {
                Object inner = new JSONTokener(value).nextValue();
                result.accept(new JSONObject((String) inner));
            } catch (Exception error) { scheduleTick(900); }
        });
    }

    private void renderDashboard() {
        if (dashboardBody == null) return;
        dashboardBody.removeAllViews();
        addText("Рекомендованные категории — " + storeName(), 23, true);
        addText("Данные этого магазина хранятся отдельно и только на телефоне.", 14, false);
        addText("Сохранено чеков: " + receipts.length() + ". Категорий: " + categories.length() + ".", 15, false);
        if (!categoryMonth.isEmpty()) addText(categoryMonth, 16, true);
        List<RecommendationEngine.Recommendation> ranked = RecommendationEngine.rankBetween(receipts, categories, analysisStartDate(), LocalDate.now());
        int shown = 0;
        for (RecommendationEngine.Recommendation r : ranked) {
            if (shown >= 5 || r.spend <= 0) break;
            shown++;
            addText(shown + ". " + r.name + " — " + format(r.spend) + " ₽ за " + analysisPeriodLabel(), 17, true);
            addText("Ставка " + format(r.rate) + "% · ожидаемый кешбэк ≈ " + format(r.score) + " ₽ · позиций: " + r.items, 13, false);
        }
        if (shown == 0) addText("Пока нет данных для рекомендации. Войдите и обновите историю.", 16, false);
        addText("Оценка строится по названиям товаров; точные правила начисления определяет торговая сеть.", 13, false);
        Button apply = new Button(this);
        apply.setText("Применить рекомендации сейчас");
        apply.setAllCaps(false);
        apply.setOnClickListener(v -> {
            phase = Phase.CATEGORIES;
            attempts = 0;
            say("Проверяю доступные категории " + storeName() + "…");
            showBrowser();
            web.loadUrl(categoriesUrl());
            scheduleTick(1000);
        });
        dashboardBody.addView(apply, new LinearLayout.LayoutParams(-1, dp(54)));
        addText("© 2026 ESI.Company", 12, false);
    }

    private void showBrowser() { showingBrowser = true; dashboard.setVisibility(View.GONE); web.setVisibility(View.VISIBLE); }
    private void showDashboard() { showingBrowser = false; web.setVisibility(View.GONE); dashboard.setVisibility(View.VISIBLE); renderDashboard(); }
    private void say(String text) { status.setText(text); }
    private void scheduleTick(long delay) { handler.removeCallbacks(ticker); if (phase != Phase.IDLE) handler.postDelayed(ticker, delay); }
    private final Runnable ticker = this::tick;
    private void addButton(LinearLayout row, String label, View.OnClickListener action) {
        Button button = new Button(this); button.setText(label); button.setTextSize(11); button.setAllCaps(false); button.setOnClickListener(action);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1); p.setMargins(dp(1), 0, dp(1), 0); row.addView(button, p);
    }
    private void addText(String text, int size, boolean strong) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(strong ? Color.rgb(15, 44, 88) : Color.rgb(54, 65, 82));
        view.setPadding(0, dp(strong ? 10 : 4), 0, dp(4)); dashboardBody.addView(view, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    private String format(double value) { return String.format(new Locale("ru", "RU"), "%,.0f", value); }

    private JSONArray deduplicateReceipts(JSONArray source) {
        JSONArray unique = new JSONArray();
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < source.length(); i++) {
            JSONObject receipt = source.optJSONObject(i);
            if (receipt == null || !includeReceipt(receipt)) continue;
            String key = receiptKey(receipt);
            if (keys.add(key)) unique.put(receipt);
        }
        return unique;
    }

    private String receiptKey(JSONObject receipt) {
        String value = receipt.optString("url");
        if (value.startsWith("pyaterochka:")) return value;
        try {
            String id = Uri.parse(value).getQueryParameter("popup_id");
            if (id != null && !id.isEmpty()) return "pyaterochka:" + id;
        } catch (Exception ignored) {}
        return value;
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onBackPressed() {
        if (showingBrowser) { phase = Phase.IDLE; showDashboard(); } else super.onBackPressed();
    }
    @Override protected void onDestroy() { handler.removeCallbacks(ticker); if (web != null) web.destroy(); super.onDestroy(); }
}
