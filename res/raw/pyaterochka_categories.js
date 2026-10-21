(function () {
    var buttons = Array.from(document.querySelectorAll('button,[role="button"]'));
    var text = document.body ? document.body.innerText : '';
    var next = '__NEXT_MONTH__';
    var current = '__CURRENT_MONTH__';
    var blocked = /Возвращайтесь позже|можно будет с\s+\d+/i.test(text);
    if (blocked) {
        window.__esiNextBlocked = true;
        var close = buttons.find(function(b) { return b.innerText.trim() === 'Хорошо'; });
        if (close) { close.click(); return JSON.stringify({ready:false}); }
    }
    var tab = buttons.find(function (b) { return new RegExp('^' + next + '(?:\\s|$)', 'i').test(b.innerText.trim()); });
    if (tab && !window.__esiNextMonth && !window.__esiNextBlocked) {
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
        var active = buttons.find(function(b) {
            return [current,next].some(function(m) { return b.innerText.trim() === m || b.innerText.trim().indexOf(m + ' ') === 0; }) &&
                ((b.getAttribute && b.getAttribute('aria-selected') === 'true') || /(?:^|\s)text-black(?:\s|$)/.test(b.className || ''));
        });
        var month = active ? [current,next].find(function(m) { return active.innerText.trim().indexOf(m) === 0; }) : window.__esiNextBlocked ? current : '';
        return JSON.stringify({ready: true, items: items, limit: count ? Number(count[1]) : items.length,
            readOnly:!!window.__esiNextBlocked || !count,
            month: month ? 'Категории Пятёрочки — ' + month : ''});
    }
    var open = Array.from(document.querySelectorAll('a,button,[role="button"],span,div')).reverse().find(function (b) {
        return b.innerText.trim() === 'Выбрать' && b.getClientRects().length > 0;
    });
    if (open && !window.__esiPickerOpened) { window.__esiPickerOpened = true; open.click(); }
    return JSON.stringify({ready: false});
})()
