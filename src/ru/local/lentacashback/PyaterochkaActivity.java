package ru.local.lentacashback;

import android.graphics.Color;
import java.time.LocalDate;

public final class PyaterochkaActivity extends StoreActivity {
    private static final String HISTORY_JS = "(function(){" +
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
            "var now=new Date(),mo=dm?months[dm[2]]:0,yr=now.getFullYear();if(mo>now.getMonth()+1)yr--;" +
            "var date=dm?yr+'-'+String(mo).padStart(2,'0')+'-'+String(Number(dm[1])).padStart(2,'0'):'';" +
            "var receiptId=(new URL(location.href)).searchParams.get('popup_id')||location.href;" +
            "return JSON.stringify({ready:location.href.indexOf('receiptsAndPointsDetailsPopup')>=0&&items.length>0,url:'pyaterochka:'+receiptId,date:date,items:items," +
            "paid:items.reduce(function(a,x){return a+x.paid},0),base:0,saving:0});" +
            "})()";
    private static final String CATEGORIES_JS = "(function(){" +
            "var text=document.body?document.body.innerText:'';" +
            "var links=Array.from(document.querySelectorAll('a[href*=\"/special-offers/\"]'));" +
            "var items=links.map(function(a){var t=a.innerText.replace(/\\s+/g,' ').trim();var r=t.match(/\\+([0-9]+)%/);" +
            "var n=t.match(/Баллы за (.+?)(?: от [0-9]| по [0-9]| Активировать|$)/i);return r&&n?{rate:Number(r[1]),name:n[1].trim()}:null}).filter(Boolean);" +
            "window.__esiOfferPolls=(window.__esiOfferPolls||0)+1;" +
            "return JSON.stringify({ready:items.length>0||window.__esiOfferPolls>12,month:'Персональные предложения Пятёрочки',items:items});" +
            "})()";

    @Override protected String storeName() { return "Пятёрочка"; }
    @Override protected int storeColor() { return Color.rgb(22, 145, 72); }
    @Override protected String storageName() { return "store_pyaterochka_v1"; }
    @Override protected String loginUrl() { return "https://x5club.ru/lk"; }
    @Override protected String historyUrl() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(3);
        return "https://x5club.ru/lk/history?page=0&startDate=" + start + "&endDate=" + end + "&codeTc=all";
    }
    @Override protected String categoriesUrl() { return "https://5ka.ru/special-offers/"; }
    @Override protected String historyScript() { return HISTORY_JS; }
    @Override protected String detailScript() { return DETAIL_JS; }
    @Override protected String categoriesScript() { return CATEGORIES_JS; }
    @Override protected String applyRecommendationsScript(String namesJson) {
        return "(function(names){" +
                "var norm=function(s){return(s||'').toLowerCase().replace(/\\s+/g,' ').trim()};var count=0;" +
                "var links=Array.from(document.querySelectorAll('a[href*=\"/special-offers/\"]'));" +
                "names.forEach(function(name){var n=norm(name);var link=links.find(function(a){return norm(a.innerText).indexOf(n)>=0});" +
                "if(!link)return;var b=Array.from(link.querySelectorAll('button,[data-qa=\"special-offers-button\"]')).find(function(x){return x.innerText.trim()==='Активировать'});" +
                "if(b){b.click();count++}});return JSON.stringify({count:count})})(" + namesJson + ")";
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
