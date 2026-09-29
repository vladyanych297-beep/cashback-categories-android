package ru.local.lentacashback;

import android.graphics.Color;

public final class MagnitActivity extends StoreActivity {
    private static final String HISTORY_JS = "(function(){" +
            "var text=document.body?document.body.innerText:'';" +
            "var cards=Array.from(document.querySelectorAll('.unit-transaction-list-item')).filter(function(e){return e.innerText.indexOf('Покупка')===0});" +
            "return JSON.stringify({ready:text.indexOf('История операций')>=0,count:cards.length,more:false});" +
            "})()";
    private static final String DETAIL_JS = "(function(){" +
            "var text=document.body?document.body.innerText:'';" +
            "var money=function(s){return Number((s||'').replace(/[^0-9,.-]/g,'').replace(',','.'))||0};" +
            "var nodes=Array.from(document.querySelectorAll('.unit-transaction-detail-product'));" +
            "var items=nodes.map(function(n){var title=n.querySelector('.unit-transaction-detail-product__title');var sum=n.querySelector('.unit-transaction-detail-product__sum');" +
            "return{name:title?title.innerText.trim():'',paid:money(sum?sum.innerText:'')}}).filter(function(x){return x.name});" +
            "var dm=text.match(/([0-9]{2})\\.([0-9]{2})\\.([0-9]{4})/);" +
            "var total=text.match(/Итого\\s+([0-9\\s]+[,.][0-9]{2})\\s*₽/);" +
            "var base=text.match(/Товаров на сумму\\s+([0-9\\s]+[,.][0-9]{2})\\s*₽/);" +
            "return JSON.stringify({ready:items.length>0,url:location.href,date:dm?dm[3]+'-'+dm[2]+'-'+dm[1]:'',items:items," +
            "paid:total?money(total[1]):items.reduce(function(a,x){return a+x.paid},0),base:base?money(base[1]):0,saving:0});" +
            "})()";
    @Override protected String storeName() { return "Магнит"; }
    @Override protected int storeColor() { return Color.rgb(218, 31, 38); }
    @Override protected String storageName() { return "store_magnit_v1"; }
    @Override protected String loginUrl() { return "https://magnit.ru/profile"; }
    @Override protected String historyUrl() { return "https://magnit.ru/profile/transactions"; }
    @Override protected String categoriesUrl() { return "https://magnit.ru/profile/favorite-categories"; }
    @Override protected String historyScript() { return HISTORY_JS; }
    @Override protected String detailScript() { return DETAIL_JS; }
    @Override protected String categoriesScript() { return rawScript("magnit_categories").replace("__NEXT_MONTH__", new String[]{"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"}[java.time.LocalDate.now().plusMonths(1).getMonthValue() - 1]); }
    @Override protected String applyRecommendationsScript(String namesJson) {
        return "(function(names){" +
                "if(window.__esiCategoriesApplied)return JSON.stringify({count:0,already:true});window.__esiCategoriesApplied=true;" +
                "var norm=function(s){return(s||'').toLowerCase().replace(/\\s+/g,' ').trim()};" +
                "var cards=Array.from(document.querySelectorAll('.cashback-card'));var count=0;" +
                "cards.forEach(function(card){var name=norm((card.innerText.split('\\n')[0]||''));var wanted=names.some(function(n){return norm(n)===name});" +
                "var box=card.querySelector('input[type=checkbox]');if(box&&!box.disabled&&box.checked!==wanted){box.click();count++}});" +
                "if(count)setTimeout(function(){var save=Array.from(document.querySelectorAll('button')).find(function(b){return b.innerText.trim().indexOf('Сохранить выбор')===0});if(save)save.click()},700);" +
                "return JSON.stringify({count:count})})(" + namesJson + ")";
    }
    @Override protected String openReceiptScript(int index) {
        return "(function(){var a=Array.from(document.querySelectorAll('.unit-transaction-list-item')).filter(function(e){return e.innerText.indexOf('Покупка')===0});if(a[" + index + "])a[" + index + "].click()})()";
    }
    @Override protected String loadMoreScript() { return "void(0)"; }
    @Override protected boolean allowedHost(String host) { return host.equals("magnit.ru") || host.endsWith(".magnit.ru"); }
}
