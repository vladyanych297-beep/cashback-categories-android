(function () {
    var buttons = Array.from(document.querySelectorAll('button,[role="button"]'));
    var text = document.body ? document.body.innerText : '';
    var next = '__NEXT_MONTH__';
    var tab = buttons.find(function (b) { return new RegExp('^' + next + '(?:\\s|$)', 'i').test(b.innerText.trim()); });
    if (tab && !window.__esiNextMonth) {
        window.__esiNextMonth = true;
        tab.click();
        return JSON.stringify({ready: false});
    }
    var items = [], seen = {};
    buttons.forEach(function (button) {
        var t = button.innerText.replace(/\s+/g, ' ').trim();
        var match = t.match(/^(.+?)\s+(\d+(?:[.,]\d+)?)%\s*$/);
        if (!match || /кешбэк|кэшбэк|подписк|выбирайте/i.test(match[1])) return;
        var name = match[1].replace(/\s+И на товары со скидкой.*$/i, '').trim();
        if (seen[name]) return;
        seen[name] = true;
        items.push({name: name, rate: Number(match[2].replace(',', '.'))});
    });
    if (items.length) {
        var count = text.match(/Выбер(?:ите|и)\s+(\d+)/i);
        return JSON.stringify({ready: true, items: items, limit: count ? Number(count[1]) : 3,
            month: 'Категории Пятёрочки — ' + (window.__esiNextMonth ? next : 'доступный период')});
    }
    var open = Array.from(document.querySelectorAll('a,button,[role="button"],span,div')).reverse().find(function (b) {
        return b.innerText.trim() === 'Выбрать' && b.getClientRects().length > 0;
    });
    if (open && !window.__esiPickerOpened) { window.__esiPickerOpened = true; open.click(); }
    return JSON.stringify({ready: false});
})()
