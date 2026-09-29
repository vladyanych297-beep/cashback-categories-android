package ru.local.lentacashback;

import android.graphics.Color;
import java.time.LocalDate;

public final class PyaterochkaActivity extends StoreActivity {
    private int historyPeriod;
    private PyaterochkaHistoryWindow historyWindow = new PyaterochkaHistoryWindow(LocalDate.now());
    private static final String HISTORY_JS = "(function(){__PERIOD_PREP__" +
            "var text=document.body?document.body.innerText:'';" +
            "var close=Array.from(document.querySelectorAll('button')).find(function(b){return b.innerText.trim()==='Закрыть'});" +
            "if(close&&text.indexOf('Важные новости кешбэка')>=0){close.click();return JSON.stringify({ready:false,preparing:true})}" +
            "var filter=Array.from(document.querySelectorAll('button')).find(function(b){return b.innerText.trim()==='Пятёрочка'});" +
            "if(filter&&!window.__cashbackFiveFilter){window.__cashbackFiveFilter=true;filter.click();return JSON.stringify({ready:false,preparing:true})}" +
            "var cards=Array.from(document.querySelectorAll('button')).filter(function(b){return b.innerText.trim().indexOf('Покупка в Пятёрочке')===0});" +
            "var more=Array.from(document.querySelectorAll('button')).some(function(b){return b.innerText.trim()==='Показать ещё'});" +
            "return JSON.stringify({ready:text.indexOf('История')>=0,count:cards.length,more:more});" +
            "})()";
    private static final String DETAIL_JS = "(function(){" +
            "var text=document.body?document.body.innerText:'';" +
            "var nodes=Array.from(document.querySelectorAll('h3.line-clamp-2'));" +
            "var items=nodes.map(function(n){var t=n.parentElement?n.parentElement.innerText:'';var mm=Array.from(t.matchAll(/([0-9]+)[\\s ]+([0-9]{2})\\s*₽/g));" +
            "var m=mm.length?mm[mm.length-1]:null;return{name:n.innerText.trim(),paid:m?Number(m[1].replace(/\\s/g,''))+Number(m[2])/100:0}}).filter(function(x){return x.name});" +
            "var dm=text.match(/([0-9]{1,2})\\s+(ЯНВ|ФЕВ|МАР|АПР|МАЙ|ИЮН|ИЮЛ|АВГ|СЕНТ|ОКТ|НОЯ|ДЕК)[^0-9]/);" +
            "var months={ЯНВ:1,ФЕВ:2,МАР:3,АПР:4,МАЙ:5,ИЮН:6,ИЮЛ:7,АВГ:8,СЕНТ:9,ОКТ:10,НОЯ:11,ДЕК:12};" +
            "var mo=dm?months[dm[2]]:0,yr=__HISTORY_YEAR__;" +
            "var date=dm?yr+'-'+String(mo).padStart(2,'0')+'-'+String(Number(dm[1])).padStart(2,'0'):'';" +
            "var receiptId=(new URL(location.href)).searchParams.get('popup_id')||location.href;" +
            "return JSON.stringify({ready:location.href.indexOf('receiptsAndPointsDetailsPopup')>=0&&items.length>0,url:'pyaterochka:'+receiptId,date:date,items:items," +
            "paid:items.reduce(function(a,x){return a+x.paid},0),base:0,saving:0});" +
            "})()";
    @Override protected String storeName() { return "Пятёрочка"; }
    @Override protected int storeColor() { return Color.rgb(22, 145, 72); }
    @Override protected String storageName() { return "store_pyaterochka_v1"; }
    @Override protected int requiredCategories(int limit) { return limit; }
    @Override protected String loginUrl() { return "https://x5club.ru/lk"; }
    @Override protected String historyUrl() {
        return "https://x5club.ru/lk/history";
    }
    @Override protected void resetHistoryPeriods() {
        historyPeriod = 0;
        historyWindow = new PyaterochkaHistoryWindow(LocalDate.now());
    }
    @Override protected LocalDate analysisStartDate() { return new PyaterochkaHistoryWindow(LocalDate.now()).start; }
    @Override protected String analysisPeriodLabel() { return "последние 100 дней"; }
    @Override protected boolean includeReceipt(org.json.JSONObject receipt) {
        return historyWindow.contains(receipt.optString("date"));
    }
    @Override protected boolean advanceHistoryPeriod() {
        historyPeriod++;
        return historyPeriod < historyWindow.monthCount;
    }
    @Override protected String historyPeriodLabel() {
        String[] names = {"январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"};
        LocalDate target = historyWindow.month(historyPeriod);
        return names[target.getMonthValue() - 1] + " — месяц " + (historyPeriod + 1) + " из " + historyWindow.monthCount + " (100 дней)";
    }
    @Override protected String categoriesUrl() { return "https://x5club.ru/lk"; }
    @Override protected String historyScript() {
        String[] full = {"январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"};
        String[] shortNames = {"Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сент", "Окт", "Ноя", "Дек"};
        LocalDate target = historyWindow.month(historyPeriod);
        String targetFull = full[target.getMonthValue() - 1] + " " + target.getYear();
        String targetShort = shortNames[target.getMonthValue() - 1];
        String prepare =
                "var targetFull='" + targetFull + "',targetShort='" + targetShort + "';" +
                "var selector=document.querySelector('[aria-label^=\"Выбрать период\"]');" +
                "var dialog=document.querySelector('dialog[open]');" +
                "if(dialog){" +
                "if(window.__esiChoosing!==targetFull){var label=Array.from(dialog.querySelectorAll('span')).find(function(x){return x.innerText.trim().toLowerCase()===targetFull});" +
                "var monthButton=label&&label.closest('button');if(monthButton){monthButton.click();window.__esiChoosing=targetFull;return JSON.stringify({ready:false,preparing:true})}}" +
                "var apply=Array.from(dialog.querySelectorAll('button')).find(function(x){return x.innerText.trim()==='Выбрать'});" +
                "if(apply){apply.click();window.__esiChoosing='';return JSON.stringify({ready:false,preparing:true})}" +
                "return JSON.stringify({ready:false,preparing:true})}" +
                "if(selector&&(selector.getAttribute('aria-label')||'').indexOf(targetShort)<0){selector.click();return JSON.stringify({ready:false,preparing:true})}";
        return HISTORY_JS.replace("__PERIOD_PREP__", prepare);
    }
    @Override protected String detailScript() {
        return DETAIL_JS.replace("__HISTORY_YEAR__", String.valueOf(historyWindow.month(historyPeriod).getYear()));
    }
    @Override protected String categoriesScript() {
        String[] names = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return rawScript("pyaterochka_categories").replace("__NEXT_MONTH__", names[LocalDate.now().plusMonths(1).getMonthValue() - 1])
                .replace("__CURRENT_MONTH__", names[LocalDate.now().getMonthValue() - 1]);
    }
    @Override protected String applyRecommendationsScript(String namesJson) {
        return rawScript("pyaterochka_apply").replace("__NAMES__", namesJson);
    }
    @Override protected String openReceiptScript(int index) {
        return "(function(){var a=Array.from(document.querySelectorAll('button')).filter(function(b){return b.innerText.trim().indexOf('Покупка в Пятёрочке')===0});if(a[" + index + "])a[" + index + "].click()})()";
    }
    @Override protected String loadMoreScript() {
        return "(function(){var b=Array.from(document.querySelectorAll('button')).find(function(x){return x.innerText.trim()==='Показать ещё'});if(b)b.click()})()";
    }
    @Override protected boolean allowedHost(String host) {
        return host.equals("5ka.ru") || host.endsWith(".5ka.ru") || host.equals("x5club.ru") || host.endsWith(".x5club.ru") || host.equals("x5.ru") || host.endsWith(".x5.ru") || host.equals("x5id.ru") || host.endsWith(".x5id.ru");
    }
}
