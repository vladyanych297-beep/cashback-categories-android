(function() {
    var start = '__START_ISO__', end = '__END_ISO__', range = start + '/' + end;
    var chip = document.querySelector('[data-qa-type="date-chip"]');
    if (!chip) return JSON.stringify({ready:false});
    if (window.__esiTbankRange !== range) {
        var calendar = document.querySelector('[data-qa-type="calendar-popup.popoverBlock"]');
        if (!calendar) { chip.click(); return JSON.stringify({ready:false, preparing:true}); }
        var stage = window.__esiTbankRangeStage || 'start';
        if (stage === 'apply') {
            var done = Array.from(calendar.querySelectorAll('button')).find(function(b) { return b.innerText.trim() === 'Готово'; });
            if (done) { done.click(); window.__esiTbankRange = range; window.__esiTbankRangeStage = ''; }
            return JSON.stringify({ready:false, preparing:true});
        }
        var target = stage === 'start' ? start : end;
        var day = calendar.querySelector('[data-date="' + target + '"]');
        if (day && day.getAttribute('aria-disabled') !== 'true') {
            day.click(); window.__esiTbankRangeStage = stage === 'start' ? 'end' : 'apply';
            return JSON.stringify({ready:false, preparing:true});
        }
        var days = calendar.querySelectorAll('[data-date]');
        if (days.length) {
            var middle = days[Math.floor(days.length / 2)].getAttribute('data-date');
            var direction = target < middle ? 'Назад' : 'Вперёд';
            var arrow = calendar.querySelector('button[aria-label="' + direction + '"]');
            if (arrow && !arrow.disabled) arrow.click();
        }
        return JSON.stringify({ready:false, preparing:true});
    }
    var result = esiTbankExtract();
    if (result.modelLoading) return JSON.stringify({ready:false});
    var empty = /нет операций|операций пока нет|операции не найдены|ничего не найдено/i.test(document.body.innerText);
    if (!result.count && !empty) return JSON.stringify({ready:false});
    var root = document.scrollingElement || document.documentElement;
    var feed = document.querySelector('[data-qa-type="atom-operations-feed-root"]');
    for (var parent = feed && feed.parentElement; parent && parent !== document.body; parent = parent.parentElement) {
        if (parent.scrollHeight > parent.clientHeight + 100 && /auto|scroll/.test(getComputedStyle(parent).overflowY)) { root = parent; break; }
    }
    result.ready = true;
    result.period = chip.innerText;
    result.bottom = result.modelComplete || empty || root.scrollTop + root.clientHeight >= root.scrollHeight - 12;
    if (!result.bottom) root.scrollTop += Math.max(400, root.clientHeight * .65);
    return JSON.stringify(result);
})()
