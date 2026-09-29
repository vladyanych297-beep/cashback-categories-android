package ru.local.lentacashback;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class YandexPayActivity extends Activity {
    private static final String HISTORY = "https://bank.yandex.ru/my/transactions-history";
    private static final String CATEGORIES = "https://sp.yandex.ru/cashback";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private enum Phase { IDLE, HISTORY, CATEGORIES }
    private final Handler handler = new Handler();
    private final Map<String, JSONObject> operations = new LinkedHashMap<>();
    private JSONArray offers = new JSONArray();
    private String period = "", historyScript, categoriesScript;
    private LocalDate syncDay;
    private int limit = 5, attempts, bottomPolls;
    private boolean evaluating, browserVisible, incompleteHistory;
    private Phase phase = Phase.IDLE;
    private WebView web;
    private ScrollView dashboard;
    private LinearLayout body;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(35, 35, 40));
        syncDay = LocalDate.now();
        try {
            JSONArray saved = new JSONArray(prefs().getString("operations", "[]"));
            for (int i = 0; i < saved.length(); i++) {
                JSONObject row = saved.optJSONObject(i);
                if (row != null && withinPeriod(row, syncDay)) operations.put(row.optString("id"), row);
            }
            offers = new JSONArray(prefs().getString("offers", "[]"));
            period = prefs().getString("period", "");
            limit = prefs().getInt("limit", 5);
        } catch (Exception error) { /* Saved data can be refreshed independently. */ }
        try {
            historyScript = script("yandex_pay_extract") + "\n" + script("yandex_pay_history");
            categoriesScript = script("yandex_pay_categories");
        } catch (Exception error) { historyScript = categoriesScript = ""; }
        makeUi();
        render();
        web.loadUrl(HISTORY);
    }

    private SharedPreferences prefs() { return getSharedPreferences("yandex_pay_v1", MODE_PRIVATE); }
    private void makeUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 249, 252));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        TextView title = text(root, "Яндекс Пэй", 22, true);
        title.setBackgroundColor(Color.rgb(35, 35, 40));
        title.setTextColor(Color.WHITE);
        title.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout actions = new LinearLayout(this);
        root.addView(actions);
        action(actions, "Назад", () -> finish());
        action(actions, "Обзор", () -> { phase = Phase.IDLE; showDashboard(); });
        action(actions, "Войти", () -> { phase = Phase.IDLE; showBrowser(); web.loadUrl(HISTORY); });
        action(actions, "Обновить", () -> startSync());
        status = text(root, "Войдите в Яндекс Пэй и нажмите «Обновить».", 13, false);
        status.setPadding(dp(16), dp(4), dp(16), dp(8));
        dashboard = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(8), dp(18), dp(28));
        dashboard.addView(body);
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
                if ("https".equalsIgnoreCase(uri.getScheme()) && allowedHost(host)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) { schedule(900); }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        web.setVisibility(View.GONE);
    }

    private boolean allowedHost(String host) {
        return host != null && (host.equals("yandex.ru") || host.endsWith(".yandex.ru")
                || host.equals("yandex.com") || host.endsWith(".yandex.com") || host.equals("ya.ru"));
    }
    private void startSync() {
        syncDay = LocalDate.now();
        operations.entrySet().removeIf(entry -> !withinPeriod(entry.getValue(), syncDay));
        saveOperations();
        phase = Phase.HISTORY;
        attempts = bottomPolls = 0;
        incompleteHistory = false;
        say("Считываю покупки за 100 дней: " + DATE.format(syncDay.minusDays(99)) + " — " + DATE.format(syncDay) + "…");
        showBrowser();
        web.loadUrl(HISTORY + "?dateFrom=" + syncDay.minusDays(99) + "&dateTo=" + syncDay);
        schedule(1100);
    }
    private void startCategories() {
        phase = Phase.CATEGORIES;
        attempts = 0;
        showBrowser();
        say("Считываю доступные категории Яндекс Пэй…");
        web.loadUrl(CATEGORIES);
        schedule(1100);
    }
    private final Runnable ticker = this::tick;
    private void schedule(long delay) {
        handler.removeCallbacks(ticker);
        if (phase != Phase.IDLE) handler.postDelayed(ticker, delay);
    }
    private void tick() {
        if (phase == Phase.IDLE || evaluating) return;
        if (++attempts > 180) {
            phase = Phase.IDLE;
            say("Не удалось загрузить данные. Завершите вход и повторите обновление. Уже прочитанные покупки сохранены.");
            return;
        }
        String url = web.getUrl();
        if (url == null) { schedule(1000); return; }
        Uri uri = Uri.parse(url);
        if (!allowedHost(uri.getHost())) { phase = Phase.IDLE; say("Вернитесь в личный кабинет Яндекс Пэй и повторите обновление."); return; }
        String code = phase == Phase.HISTORY ? preparedHistory() : categoriesScript;
        if (code.isEmpty()) { phase = Phase.IDLE; say("Не удалось загрузить модуль Яндекс Пэй."); return; }
        final Phase reading = phase;
        evaluating = true;
        web.evaluateJavascript(code, value -> {
            evaluating = false;
            if (phase != reading) return;
            try {
                JSONObject data = new JSONObject((String) new JSONTokener(value).nextValue());
                if (data.optBoolean("preparing")) {
                    say("Выбираю период 100 дней в календаре Яндекс Пэй…");
                    schedule(1000); return;
                }
                if (!data.optBoolean("ready")) { schedule(1000); return; }
                if (reading == Phase.HISTORY) receiveHistory(data); else receiveCategories(data);
            } catch (Exception error) { schedule(1000); }
        });
    }
    private void receiveHistory(JSONObject data) {
        incompleteHistory |= data.optInt("unreadable") > 0;
        int added = 0;
        JSONArray rows = data.optJSONArray("items");
        if (rows != null) for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null || !withinPeriod(row, syncDay)) continue;
            String id = row.optString("id");
            if (id.isEmpty()) continue;
            if (!operations.containsKey(id)) added++;
            operations.put(id, row);
        }
        saveOperations();
        if (added > 0) attempts = 0;
        bottomPolls = data.optBoolean("bottom") && added == 0 ? bottomPolls + 1 : 0;
        say("Загружаю историю за 100 дней: покупок " + operations.size() + "…");
        if (bottomPolls >= 8) {
            if (!incompleteHistory) prefs().edit().putString("history_updated", syncDay.toString()).apply();
            startCategories();
        } else schedule(1100);
    }
    private void receiveCategories(JSONObject data) {
        offers = data.optJSONArray("items");
        if (offers == null) offers = new JSONArray();
        period = data.optString("month");
        limit = Math.max(1, Math.min(12, data.optInt("limit", 5)));
        prefs().edit().putString("offers", offers.toString()).putString("period", period).putInt("limit", limit).putString("categories_checked", LocalDate.now().toString()).apply();
        phase = Phase.IDLE;
        say("Готово: покупок " + operations.size() + ", доступных категорий " + offers.length() + "."
                + (incompleteHistory ? " У части операций не прочитана дата; история неполная." : ""));
        showDashboard();
    }

    private String preparedHistory() {
        LocalDate start = syncDay.minusDays(99);
        return historyScript.replace("__START_LABEL__", calendarLabel(start)).replace("__END_LABEL__", calendarLabel(syncDay))
                .replace("__START_INPUT__", DATE.format(start)).replace("__END_INPUT__", DATE.format(syncDay))
                .replace("__START_SHORT__", DATE.format(start).substring(0, 5)).replace("__END_SHORT__", DATE.format(syncDay).substring(0, 5));
    }
    private String calendarLabel(LocalDate date) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return date.getDayOfMonth() + " " + months[date.getMonthValue() - 1] + " " + date.getYear();
    }
    private boolean withinPeriod(JSONObject row, LocalDate today) {
        try {
            LocalDate date = LocalDate.parse(row.optString("date"));
            return !date.isBefore(today.minusDays(99)) && !date.isAfter(today);
        } catch (Exception invalidDate) { return false; }
    }
    private void saveOperations() { prefs().edit().putString("operations", new JSONArray(operations.values()).toString()).apply(); }
    private String script(String name) throws Exception {
        int id = getResources().getIdentifier(name, "raw", getPackageName());
        try (InputStream stream = getResources().openRawResource(id); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096]; int read;
            while ((read = stream.read(bytes)) >= 0) out.write(bytes, 0, read);
            return new String(out.toByteArray(), "UTF-8");
        }
    }

    private void render() {
        body.removeAllViews();
        LocalDate today = LocalDate.now();
        List<YandexPayEngine.Operation> parsed = new ArrayList<>();
        for (JSONObject row : operations.values()) parsed.add(new YandexPayEngine.Operation(row.optString("id"), row.optString("date"),
                row.optString("merchant"), row.optString("category"), row.optLong("cents"), row.optBoolean("cardPay")));
        List<YandexPayEngine.Offer> available = new ArrayList<>();
        boolean fresh = CashbackPeriod.fresh(prefs().getString("categories_checked", ""), period, today);
        for (int i = 0; fresh && i < offers.length(); i++) {
            JSONObject offer = offers.optJSONObject(i);
            if (offer != null) available.add(new YandexPayEngine.Offer(offer.optString("name"), offer.optDouble("rate"),
                    offer.optString("kind"), offer.optBoolean("cardPay", true), offer.optString("description"), offer.optBoolean("selected")));
        }
        YandexPayEngine.Report report = YandexPayEngine.calculate(parsed, available, today);
        text(body, "Яндекс Пэй — последние 100 дней", 23, true);
        text(body, DATE.format(today.minusDays(99)) + " — " + DATE.format(today), 16, true);
        text(body, "Сумма покупок: " + money(report.total), 20, true);
        text(body, "Покупок за период: " + report.count, 14, false);
        String updated = prefs().getString("history_updated", "");
        if (!updated.isEmpty()) text(body, "История обновлена: " + updated, 13, false);
        text(body, "Расчёт по операциям Яндекс Пэй. Пополнения, переводы, снятие наличных и возвраты исключены. Данные хранятся только на телефоне.", 13, false);
        text(body, "Рекомендованные категории", 21, true);
        if (!fresh) text(body, "Категории требуют обновления для текущего месяца. Старые ставки не используются для рекомендаций.", 15, true);
        int shown = 0;
        for (YandexPayEngine.Recommendation recommendation : report.recommendations) {
            if (shown >= limit || recommendation.estimate() <= 0) continue;
            shown++;
            text(body, shown + ". " + recommendation.offer.name + " — покупки " + money(recommendation.spend), 17, true);
            text(body, "Ставка " + percentage(recommendation.offer.rate) + "% · возможный кешбэк ≈ " + money(recommendation.estimate()), 13, false);
        }
        if (shown == 0) text(body, "Войдите и обновите покупки и категории для расчёта рекомендаций.", 15, false);
        text(body, "Оценка предполагает выполнение условий категории и не учитывает ограничения и лимиты начисления. Суммы разных предложений могут пересекаться.", 13, false);
        button(body, "Обновить категории", this::startCategories);
        button(body, "Выбрать и подтвердить на сайте Яндекса", () -> { phase = Phase.IDLE; showBrowser(); web.loadUrl(CATEGORIES); say("Проверьте доступный месяц, условия и подтвердите выбор на странице Яндекса."); });
        text(body, period.isEmpty() ? "Доступные категории" : period, 20, true);
        for (YandexPayEngine.Recommendation recommendation : report.recommendations) {
            YandexPayEngine.Offer offer = recommendation.offer;
            text(body, offer.name + " — " + ("discount".equals(offer.kind) ? "скидка " : "") + percentage(offer.rate) + "%" + (offer.selected ? " · выбрана" : ""), 17, true);
            text(body, "Покупки за 100 дней: " + money(recommendation.spend) + " · операций: " + recommendation.count, 14, false);
            text(body, offer.cardPay ? "С картой Пэй" : "С картой любого банка", 13, false);
            if (!offer.description.isEmpty()) text(body, offer.description, 13, false);
            if ("discount".equals(offer.kind)) text(body, "Скидка на доставку не рассчитывается от полной суммы заказа.", 13, false);
        }
        text(body, "Покупки по банковским категориям", 21, true);
        for (YandexPayEngine.Group group : report.groups) {
            text(body, group.name + " — " + money(group.cents), 17, true);
            text(body, "Покупок: " + group.count, 13, false);
            LinearLayout merchants = new LinearLayout(this);
            merchants.setOrientation(LinearLayout.VERTICAL);
            merchants.setVisibility(View.GONE);
            button(body, "Магазины и сервисы (" + group.merchants.size() + ")", () ->
                    merchants.setVisibility(merchants.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
            body.addView(merchants);
            List<YandexPayEngine.Group> sorted = new ArrayList<>(group.merchants.values());
            Collections.sort(sorted, (a, b) -> Long.compare(b.cents, a.cents));
            for (YandexPayEngine.Group merchant : sorted) text(merchants, merchant.name + " — " + money(merchant.cents) + " · покупок: " + merchant.count, 14, false);
        }
        text(body, "© 2026 ESI.Company", 12, false);
    }

    private void showBrowser() { browserVisible = true; dashboard.setVisibility(View.GONE); web.setVisibility(View.VISIBLE); }
    private void showDashboard() { browserVisible = false; web.setVisibility(View.GONE); dashboard.setVisibility(View.VISIBLE); render(); }
    private void say(String value) { status.setText(value); }
    private String money(long cents) { return String.format(new Locale("ru", "RU"), "%,.2f ₽", cents / 100.0); }
    private String percentage(double rate) { return String.format(new Locale("ru", "RU"), "%s", rate == (int) rate ? String.valueOf((int) rate) : String.valueOf(rate)); }
    private TextView text(LinearLayout parent, String value, int size, boolean strong) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(strong ? Color.rgb(15, 44, 88) : Color.rgb(54, 65, 82));
        view.setPadding(0, dp(strong ? 12 : 4), 0, dp(4)); parent.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    private void button(LinearLayout parent, String label, Runnable callback) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setOnClickListener(v -> callback.run()); parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }
    private void action(LinearLayout parent, String label, Runnable callback) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(11); button.setOnClickListener(v -> callback.run()); parent.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
    @Override public void onBackPressed() { if (browserVisible) { phase = Phase.IDLE; showDashboard(); } else super.onBackPressed(); }
    @Override protected void onDestroy() { handler.removeCallbacks(ticker); if (web != null) web.destroy(); super.onDestroy(); }
    @Override protected void onResume() { super.onResume(); if (body != null && !browserVisible) render(); }
}
