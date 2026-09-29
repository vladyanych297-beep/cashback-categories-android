(function () {
    var text = document.body ? document.body.innerText : '';
    var next = '__NEXT_MONTH__';
    var nextTab = Array.from(document.querySelectorAll('button,[role="tab"]')).find(function(b) {
        return new RegExp('^' + next + '(?:\\s|$)', 'i').test(b.innerText.trim());
    });
    if (nextTab && !window.__esiNextMonth) {
        window.__esiNextMonth = true; nextTab.click(); return JSON.stringify({ready: false});
    }
    var cards = Array.from(document.querySelectorAll('.cashback-card'));
    if (!cards.length) {
        var main = document.querySelector('main');
        var open = main && Array.from(main.querySelectorAll('button')).find(function(b) { return b.innerText.trim() === 'Выбрать'; });
        if (open && !window.__esiPickerOpened) { window.__esiPickerOpened = true; open.click(); }
        return JSON.stringify({ready: false});
    }
    var seen = {}, items = [];
    cards.forEach(function(card) {
        var lines = card.innerText.split('\n').map(function(s) { return s.replace(/\s+/g, ' ').trim(); }).filter(Boolean);
        var rate = (lines.join(' ').match(/([0-9]+)%/) || [])[1];
        var name = lines.find(function(s) { return !/^[0-9]+%$/.test(s) && !/подписк/i.test(s); });
        if (!name || !rate || /подписк/i.test(lines[0] || '') || seen[name]) return;
        seen[name] = true; items.push({name: name, rate: Number(rate)});
    });
    var heading = (text.match(/Выберите[^\n]+категори[^\n]*/) || [])[0] || '';
    var selected = document.querySelector('[role="tab"][aria-selected="true"]');
    if (!heading && selected) heading = 'Категории Магнита — ' + selected.innerText.trim();
    return JSON.stringify({ready: items.length > 0, month: heading, items: items});
})()
