(function () {
    var cards = document.querySelectorAll('[data-test-id="selector-page-list-item"]');
    if (!cards.length) return JSON.stringify({ready: false});
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
