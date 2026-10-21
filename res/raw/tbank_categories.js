(function() {
    var title = document.querySelector('[data-qa-type="mobile-luca-cashback-title"]');
    var cards = Array.from(document.querySelectorAll('[data-qa-type="category_item"]'));
    if (title && cards.length) {
        var seen = {}, items = [];
        cards.forEach(function(card) {
            var text = card.innerText.replace(/\s+/g,' ').trim();
            var match = text.match(/^([0-9]+(?:[.,][0-9]+)?)%\s*(.+)$/);
            if (!match || seen[match[2]]) return;
            seen[match[2]] = true;
            items.push({name:match[2].trim(), rate:Number(match[1].replace(',','.')), kind:'cashback',
                cardPay:true, selected:!!document.querySelector('[data-qa-type="mobile-luca-cashback-task-success"]'),
                description:'Условия, допустимые MCC и лимиты определяет программа Т-Банка.'});
        });
        var count = document.body.innerText.match(/Выбер(?:ите|и)\s+([0-9]+)/i);
        return JSON.stringify({ready:items.length>0, month:title.innerText, items:items,
            limit:count?Number(count[1]):items.length, readOnly:!count, programUrl:location.href});
    }
    var programs = Array.from(document.querySelectorAll('[data-qa-type^="mobile-bonuses-main-aggregated-card-"]'))
        .filter(function(card) { return /Повышенный\s+к[эе]шб[эе]к/i.test(card.innerText); });
    if (programs.length && __AUTO_OPEN__ && !window.__esiTbankProgramOpened) {
        var upcoming = programs.find(function(card) { return card.innerText.toLowerCase().indexOf('__NEXT_MONTH_STEM__') >= 0; });
        window.__esiTbankProgramOpened = true; (upcoming || programs[0]).click();
    }
    return JSON.stringify({ready:false, chooseProgram:programs.length>0});
})()
