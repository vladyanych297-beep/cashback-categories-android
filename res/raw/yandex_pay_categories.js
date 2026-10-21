(function () {
    var cards = document.querySelectorAll('[data-test-id="selector-page-list-item"]');
    if (!cards.length) {
        var current = document.querySelector('[data-test-id="current-page-section"]');
        var heading = document.querySelector('[data-test-id="current-page-header"]');
        if (!current || !heading) return JSON.stringify({ready:false});
        var activeChip = document.querySelector('[class*="__activeChip"]');
        var pay = !activeChip || !/Другие карты/.test(activeChip.innerText);
        var chosen = Array.from(current.querySelectorAll('li')).map(function(card) {
            var title = card.querySelector('[class*="__title"]');
            var rate = card.innerText.match(/([0-9]+(?:[.,][0-9]+)?)\s*%/);
            return title && rate ? {name:title.innerText.trim(), rate:Number(rate[1].replace(',','.')),
                kind:'cashback', cardPay:pay, description:'Действующая категория; условия и лимиты смотрите на сайте Яндекса.', selected:true} : null;
        }).filter(Boolean);
        return JSON.stringify({ready:chosen.length>0, readOnly:true, month:heading.innerText,
            limit:chosen.length, items:chosen});
    }
    var items = [], cardPay = true;
    Array.from(document.querySelectorAll('h2,[data-test-id="selector-page-list-item"]')).forEach(function (e) {
        if (e.tagName === 'H2') { cardPay = !/любого банка/i.test(e.innerText); return; }
        var title = e.querySelector('[role="button"]');
        var lines = (title ? title.innerText : e.innerText).split('\n').map(function (s) { return s.trim(); }).filter(Boolean);
        var match = (lines[0] || '').match(/^([−–-]?)([0-9]+(?:[.,][0-9]+)?)%\s*(.+)$/);
        if (!match) return;
        var checkbox = e.querySelector('input[type="checkbox"]');
        items.push({name: match[3].trim(), rate: Number(match[2].replace(',', '.')),
            kind: match[1] ? 'discount' : 'cashback', cardPay: cardPay,
            description: lines.slice(1).join(' '), selected: !!checkbox && checkbox.checked});
    });
    var heading = document.querySelector('h1');
    var text = heading ? heading.innerText : '';
    var limit = text.match(/Выберите\s+([0-9]+)/i);
    return JSON.stringify({ready: items.length > 0, month: text, limit: limit ? Number(limit[1]) : 5, items: items});
})()
