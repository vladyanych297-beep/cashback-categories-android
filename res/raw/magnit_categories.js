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
        if (!open) {
            var active = Array.from(document.querySelectorAll('.favorite-section-categories-item'));
            var chosen = active.map(function(card) {
                var title = card.querySelector('.favorite-section-categories-item__title');
                var rate = (card.innerText.match(/([0-9]+(?:[.,][0-9]+)?)%/) || [])[1];
                return title && rate ? {name:title.innerText.trim(), rate:Number(rate.replace(',', '.')), selected:true} : null;
            }).filter(Boolean);
            if (chosen.length) return JSON.stringify({ready:true, readOnly:true, limit:chosen.length,
                month:'Действующие категории Магнита — __CURRENT_MONTH__', items:chosen});
        }
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
    var slots = heading.match(/Выберите\s+(ещ[её]\s+)?([0-9]+)/i);
    var checked = cards.filter(function(card) { var box=card.querySelector('input[type="checkbox"]'); return box && box.checked; }).length;
    return JSON.stringify({ready: items.length > 0, month: heading, items: items,
        limit:slots ? Number(slots[2]) + (slots[1] ? checked : 0) : 5});
})()
