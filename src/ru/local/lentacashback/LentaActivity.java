package ru.local.lentacashback;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.List;
import java.util.Locale;

public final class LentaActivity extends Activity {
    private static final String HISTORY = "https://lenta.com/my-account/order/";
    private static final String CATEGORIES = "https://lenta.com/my-account/loyalty-select/";
    private static final String PREFS = "local_history_v1";
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
    private int emptyHistoryPolls = 0;
    private String historyDiagnostic = "";
    private boolean evaluating = false;
    private boolean showingBrowser = false;

    private static final String HISTORY_JS = "(function(){" +
            "var body=document.body?document.body.innerText:'';" +
            "var tab=Array.from(document.querySelectorAll('[role=tab]')).find(function(e){return e.textContent.trim()==='Магазины'});" +
            "if(!tab)tab=Array.from(document.querySelectorAll('[role=tablist] button,[role=tablist] a,[role=tablist] span')).find(function(e){return e.textContent.trim()==='Магазины' && e.children.length===0});" +
            "var shop=tab&&(tab.closest('[role=tab],button,a')||tab);" +
            "var active=!!shop&&(shop.getAttribute('aria-selected')==='true'||shop.getAttribute('aria-current')==='page'||" +
            "shop.getAttribute('data-state')==='active'||/(^|[^a-z])(active|selected)($|[^a-z])/i.test(shop.className||''));" +
            "if(shop&&!window.__lentaCashbackShopClicked){window.__lentaCashbackShopClicked=true;if(!active){shop.click();" +
            "return JSON.stringify({ready:false,switching:true,shopFound:true,count:0,more:false,empty:false})}};" +
            "var cards=document.querySelectorAll('.purchases-item');" +
            "var links=Array.from(document.querySelectorAll('a[href*=\"/order/shop/\"]')).filter(function(e,i,a){return a.findIndex(function(x){return x.href===e.href})===i});" +
            "var source=cards.length?'cards':links.length?'links':'none';" +
            "var count=source==='cards'?cards.length:links.length;" +
            "if(!count&&shop&&window.__lentaCashbackShopClicked&&!window.__lentaCashbackHistoryScrolled){" +
            "window.__lentaCashbackHistoryScrolled=true;window.scrollTo(0,document.body.scrollHeight);" +
            "return JSON.stringify({ready:false,scrolling:true,shopFound:true,count:0})};" +
            "var more=Array.from(document.querySelectorAll('button')).some(function(b){return b.textContent.trim()==='Показать ещё'});" +
            "return JSON.stringify({ready:body.indexOf('История покупок')>=0,count:count,source:source,more:more," +
            "shopFound:!!shop,shopClicked:!!window.__lentaCashbackShopClicked," +
            "shopActive:active||!!shop&&(shop.getAttribute('aria-selected')!=='false')," +
            "limit:body.indexOf('Храним историю только за последние три месяца')>=0,empty:body.indexOf('Пока нет покупок')>=0});" +
            "})()";
    private static final String DETAIL_JS = "(function(){" +
            "var nodes=Array.from(document.querySelectorAll('.omni-profile-order-item__body'));" +
            "var text=document.body.innerText;" +
            "var parse=function(s){return Number((s||'').replace(/[^0-9,.-]/g,'').replace(',','.'))||0};" +
            "var get=function(label){var m=text.match(new RegExp(label+'\\\\n([^\\\\n]+)'));return m?parse(m[1]):0};" +
            "var items=nodes.map(function(n){var a=n.innerText.split('\\n').map(function(s){return s.trim()}).filter(Boolean);" +
            "var last=a[a.length-1]||'';return {name:a[0]||'',paid:parse(last.split('·')[0])}}).filter(function(x){return x.name});" +
            "return JSON.stringify({ready:items.length>0 && text.indexOf('Экономия с Лентой')>=0," +
            "url:location.href,date:(location.href.match(/20[0-9]{2}-[0-9]{2}-[0-9]{2}/)||[])[0]||''," +
            "items:items,paid:items.reduce(function(a,x){return a+x.paid},0),base:get('Товары без карты Лента'),saving:get('Экономия с Лентой')});" +
            "})()";
    private static final String CATEGORIES_JS = "(function(){" +
            "var heading=(document.body.innerText.match(/Выберите 5 категорий кешбэка на[^\\n]*/)||[])[0]||'';" +
            "var cards=Array.from(document.querySelectorAll('.favorites-categories-list-page-category'));" +
            "var items=cards.map(function(e){var m=e.innerText.match(/([0-9]+)%\\s*([^\\n]+)/);" +
            "return m?{rate:Number(m[1]),name:m[2].trim()}:null}).filter(Boolean);" +
            "return JSON.stringify({ready:items.length>0,month:heading,items:items});" +
            "})()";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            receipts = new JSONArray(prefs().getString(PREFS + "_receipts", "[]"));
            categories = new JSONArray(prefs().getString(PREFS + "_categories", "[]"));
            categoryMonth = prefs().getString(PREFS + "_month", "");
        } catch (Exception ignored) {}
        makeUi();
        renderDashboard();
        web.loadUrl(HISTORY);
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
        getWindow().setStatusBarColor(Color.rgb(0, 78, 170));

        TextView title = new TextView(this);
        title.setText("Категории Ленты");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setPadding(dp(16), dp(14), dp(16), dp(14));
        title.setBackgroundColor(Color.rgb(0, 78, 170));
        root.addView(title);

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(8), dp(6), dp(8), dp(4));
        root.addView(actions);
        addButton(actions, "Обзор", new View.OnClickListener() {
            @Override public void onClick(View v) { showDashboard(); }
        });
        addButton(actions, "Войти в Ленту", new View.OnClickListener() {
            @Override public void onClick(View v) {
                phase = Phase.IDLE;
                showBrowser();
                web.loadUrl(HISTORY);
            }
        });
        addButton(actions, "Обновить", new View.OnClickListener() {
            @Override public void onClick(View v) { startSync(); }
        });

        status = new TextView(this);
        status.setPadding(dp(16), dp(4), dp(16), dp(8));
        status.setTextColor(Color.rgb(68, 76, 88));
        root.addView(status);

        dashboard = new ScrollView(this);
        dashboardBody = new LinearLayout(this);
        dashboardBody.setOrientation(LinearLayout.VERTICAL);
        dashboardBody.setPadding(dp(18), dp(10), dp(18), dp(30));
        dashboard.addView(dashboardBody);
        root.addView(dashboard, new LinearLayout.LayoutParams(-1, 0, 1));

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // Some responsive sites reject the legacy Version/4.0 WebView token.
        settings.setUserAgentString(settings.getUserAgentString()
                .replace("; wv", "")
                .replace("Version/4.0 ", ""));
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if ("https".equalsIgnoreCase(uri.getScheme()) && host != null &&
                        (host.equals("lenta.com") || host.endsWith(".lenta.com"))) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) { scheduleTick(800); }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() == 403) {
                    phase = Phase.IDLE;
                    say("Лента ответила 403 во встроенном браузере. Вход на этом устройстве пока недоступен.");
                    showDashboard();
                }
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        web.setVisibility(View.GONE);
    }

    private void addButton(LinearLayout row, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1);
        p.setMargins(dp(2), 0, dp(2), 0);
        row.addView(button, p);
    }

    private int dp(int pixels) { return (int)(pixels * getResources().getDisplayMetrics().density + .5f); }
    private void showBrowser() {
        showingBrowser = true;
        dashboard.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
    }
    private void showDashboard() {
        showingBrowser = false;
        web.setVisibility(View.GONE);
        dashboard.setVisibility(View.VISIBLE);
        renderDashboard();
    }
    private void say(String message) { status.setText(message); }

    private void startSync() {
        phase = Phase.HISTORY;
        targetIndex = -1;
        receiptCount = 0;
        attempts = 0;
        emptyHistoryPolls = 0;
        historyDiagnostic = "";
        showBrowser();
        say("Считываю магазинные чеки…");
        web.loadUrl(HISTORY);
        scheduleTick(1000);
    }

    private void scheduleTick(long delayMs) {
        handler.removeCallbacks(ticker);
        if (phase != Phase.IDLE) handler.postDelayed(ticker, delayMs);
    }
    private final Runnable ticker = new Runnable() {
        @Override public void run() { tick(); }
    };

    private void tick() {
        if (phase == Phase.IDLE || evaluating) return;
        if (++attempts > 120) {
            phase = Phase.IDLE;
            say("Не удалось прочитать страницу: " + historyDiagnostic + ". Проверьте вкладку «Магазины» и попробуйте снова.");
            return;
        }
        String url = web.getUrl();
        if (url == null || !url.startsWith("https://lenta.com/my-account/")) {
            phase = Phase.IDLE;
            say("Завершите вход в Ленту, затем нажмите «Обновить».");
            return;
        }
        evaluating = true;
        if (phase == Phase.HISTORY) evalJson(HISTORY_JS, new Result() {
            @Override public void accept(JSONObject data) { processHistory(data); }
        });
        else if (phase == Phase.DETAIL) evalJson(DETAIL_JS, new Result() {
            @Override public void accept(JSONObject data) { processDetail(data); }
        });
        else if (phase == Phase.CATEGORIES) evalJson(CATEGORIES_JS, new Result() {
            @Override public void accept(JSONObject data) { processCategories(data); }
        });
    }

    private interface Result { void accept(JSONObject data); }
    private void evalJson(String script, final Result callback) {
        web.evaluateJavascript(script, value -> {
            evaluating = false;
            try {
                Object inner = new JSONTokener(value).nextValue();
                callback.accept(new JSONObject((String)inner));
            } catch (Exception error) {
                historyDiagnostic = "ошибка чтения страницы: " + error.getClass().getSimpleName();
                scheduleTick(900);
            }
        });
    }

    private void processHistory(JSONObject data) {
        if (data.optBoolean("switching")) {
            say("Переключаю историю на вкладку «Магазины»…");
            scheduleTick(1200);
            return;
        }
        if (data.optBoolean("scrolling")) {
            say("Загружаю чеки ниже на странице…");
            scheduleTick(1300);
            return;
        }
        if (!data.optBoolean("ready")) { scheduleTick(900); return; }
        int count = data.optInt("count");
        boolean more = data.optBoolean("more");
        historyDiagnostic = "вкладка «Магазины»: " + (data.optBoolean("shopFound") ? "найдена" : "не найдена")
                + ", чеков: " + count + ", источник: " + data.optString("source")
                + ", пустая история: " + data.optBoolean("empty");
        if (count == 0 && !data.optBoolean("shopClicked")) {
            say("Ищу вкладку «Магазины»…");
            scheduleTick(900);
            return;
        }
        if (more && (targetIndex < 0 || count <= targetIndex)) {
            say("Загружаю историю: найдено чеков " + count + "…");
            web.evaluateJavascript("(function(){var b=Array.from(document.querySelectorAll('button')).find(function(x){return x.textContent.trim()==='Показать ещё'});if(b)b.click()})()", null);
            scheduleTick(1300);
            return;
        }
        if (targetIndex < 0) {
            if (count == 0) {
                if (!data.optBoolean("shopActive") || !data.optBoolean("empty") || ++emptyHistoryPolls < 5) {
                    say("Проверяю магазинные покупки: пока 0 чеков…");
                    scheduleTick(900);
                    return;
                }
            }
            receiptCount = count;
            targetIndex = 0;
            attempts = 0;
        }
        if (targetIndex >= receiptCount) { openCategories(); return; }
        if (count <= targetIndex) { scheduleTick(900); return; }
        phase = Phase.DETAIL;
        say("Чек " + (targetIndex + 1) + " из " + receiptCount + "…");
        if ("links".equals(data.optString("source"))) {
            web.evaluateJavascript("(function(){var a=Array.from(document.querySelectorAll('a[href*=\"/order/shop/\"]')).filter(function(e,i,all){return all.findIndex(function(x){return x.href===e.href})===i});if(a[" + targetIndex + "])a[" + targetIndex + "].click()})()", null);
        } else {
            web.evaluateJavascript("document.querySelectorAll('.purchases-item')[" + targetIndex + "].click()", null);
        }
        scheduleTick(1100);
    }

    private void processDetail(JSONObject data) {
        if (!data.optBoolean("ready")) { scheduleTick(850); return; }
        String url = data.optString("url");
        if (!url.contains("/order/shop/")) { scheduleTick(850); return; }
        boolean exists = false;
        for (int i = 0; i < receipts.length(); i++) {
            if (url.equals(receipts.optJSONObject(i).optString("url"))) { exists = true; break; }
        }
        if (!exists) receipts.put(data);
        prefs().edit().putString(PREFS + "_receipts", receipts.toString()).apply();
        targetIndex++;
        attempts = 0;
        phase = Phase.HISTORY;
        if (targetIndex >= receiptCount) openCategories();
        else {
            web.loadUrl(HISTORY);
            scheduleTick(1100);
        }
    }

    private void openCategories() {
        phase = Phase.CATEGORIES;
        attempts = 0;
        say("Считываю категории следующего месяца…");
        web.loadUrl(CATEGORIES);
        scheduleTick(1100);
    }

    private void processCategories(JSONObject data) {
        if (!data.optBoolean("ready")) { scheduleTick(900); return; }
        categories = data.optJSONArray("items");
        if (categories == null) categories = new JSONArray();
        categoryMonth = data.optString("month");
        prefs().edit()
                .putString(PREFS + "_categories", categories.toString())
                .putString(PREFS + "_month", categoryMonth).apply();
        phase = Phase.IDLE;
        applyRecommendedCategories();
    }

    private void applyRecommendedCategories() {
        JSONArray names = new JSONArray();
        List<RecommendationEngine.Recommendation> ranked = RecommendationEngine.rank(receipts, categories);
        for (RecommendationEngine.Recommendation recommendation : ranked) {
            if (names.length() >= 5 || recommendation.spend <= 0) break;
            names.put(recommendation.name);
        }
        if (names.length() < 5) {
            say("Готово: для автоматического выбора Ленты найдено только " + names.length() + " из 5 категорий.");
            showDashboard();
            return;
        }
        say("Применяю 5 рекомендованных категорий Ленты…");
        String script = "(function(names){" +
                "if(window.__esiLentaApplied)return JSON.stringify({count:0,already:true});window.__esiLentaApplied=true;" +
                "var norm=function(s){return(s||'').toLowerCase().replace(/\\s+/g,' ').trim()};" +
                "var cards=Array.from(document.querySelectorAll('.favorites-categories-list-page-category'));var count=0;" +
                "names.forEach(function(name){var n=norm(name);var card=cards.find(function(e){var m=e.innerText.match(/([0-9]+)%\\s*([^\\n]+)/);return m&&norm(m[2])===n});if(card){card.click();count++}});" +
                "if(count===5)setTimeout(function(){var b=Array.from(document.querySelectorAll('button')).find(function(x){return /Выбрано\\s*5\\s*\\/\\s*5/.test(x.innerText)});if(b)b.click();" +
                "setTimeout(function(){var c=Array.from(document.querySelectorAll('button')).find(function(x){return /^(Подтвердить|Сохранить)$/.test(x.innerText.trim())});if(c)c.click()},700)},700);" +
                "return JSON.stringify({count:count})})(" + names.toString() + ")";
        evaluating = true;
        evalJson(script, data -> handler.postDelayed(() -> {
            int count = data.optInt("count");
            say(count == 5
                    ? "Готово: 5 рекомендованных категорий Ленты выбраны автоматически."
                    : "Автоматический выбор Ленты недоступен; откройте страницу выбора.");
            showDashboard();
        }, 2200));
    }

    private void renderDashboard() {
        if (dashboardBody == null) return;
        dashboardBody.removeAllViews();
        addText("Рекомендованные категории — Лента", 23, true);
        addText("Для синхронизации войдите в личный кабинет на странице «Ленты» внутри приложения и нажмите «Обновить». Данные чеков хранятся только на этом телефоне.", 15, false);
        addText("Сохранено чеков: " + receipts.length() + ". Доступно категорий: " + categories.length() + ".", 15, false);
        if (!categoryMonth.isEmpty()) addText(categoryMonth, 16, true);

        List<RecommendationEngine.Recommendation> ranked = RecommendationEngine.rank(receipts, categories);
        int shown = 0;
        for (RecommendationEngine.Recommendation r : ranked) {
            if (shown >= 5 || r.spend <= 0) break;
            shown++;
            addText(shown + ". " + r.name + " — " + format(r.spend) + " ₽ за последние 90 дней", 17, true);
            addText("Ставка " + format(r.rate) + "% · ожидаемый кешбэк ≈ " + format(r.score) + " ₽ · позиций в чеках: " + r.items, 13, false);
        }
        if (shown == 0) addText("Пока нет данных для рекомендации. Откройте «Ленту» и обновите историю.", 16, false);
        addText("После обновления приложение автоматически выбирает пять рекомендаций в порядке ожидаемой выгоды. На первом уровне начисление идёт по первой категории, на пятом — по всем пяти. История магазина ограничена последними тремя месяцами.", 14, false);

        Button select = new Button(this);
        select.setText("Применить рекомендации сейчас");
        select.setAllCaps(false);
        select.setOnClickListener(v -> {
            phase = Phase.CATEGORIES;
            attempts = 0;
            say("Проверяю доступные категории Ленты…");
            showBrowser();
            web.loadUrl(CATEGORIES);
            scheduleTick(1000);
        });
        dashboardBody.addView(select, new LinearLayout.LayoutParams(-1, dp(54)));
        PurchaseStatisticsView.append(this, dashboardBody, receipts, categories,
                getSharedPreferences("lenta_purchase_report_v1", MODE_PRIVATE),
                java.time.LocalDate.now().minusDays(90), java.time.LocalDate.now());
        addText("Неофициальное приложение. Не связано с «Лентой».", 12, false);
        addText("© 2026 ESI.Company", 12, false);
    }

    private String format(double value) { return String.format(new Locale("ru", "RU"), "%,.0f", value); }
    private SharedPreferences prefs() { return getSharedPreferences("MainActivity", MODE_PRIVATE); }
    private void addText(String text, int size, boolean strong) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(strong ? Color.rgb(15, 44, 88) : Color.rgb(54, 65, 82));
        view.setPadding(0, dp(strong ? 12 : 5), 0, dp(5));
        dashboardBody.addView(view, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override public void onBackPressed() {
        if (showingBrowser) {
            if (phase != Phase.IDLE) {
                phase = Phase.IDLE;
                say("Обновление остановлено. Уже считанные чеки сохранены.");
            }
            showDashboard();
        } else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
