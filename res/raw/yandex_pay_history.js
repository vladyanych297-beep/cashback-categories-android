(function () {
    var startLabel = '__START_LABEL__', endLabel = '__END_LABEL__';
    var startInput = '__START_INPUT__', endInput = '__END_INPUT__';
    var startShort = '__START_SHORT__', endShort = '__END_SHORT__';
    var filter = document.querySelector('[data-testid="date-filter-chip"]');
    if (!filter) return JSON.stringify({ready: false});
    var routeReady = false;
    if (typeof location !== 'undefined') {
        var query = new URL(location.href).searchParams;
        routeReady = query.get('dateFrom') === startInput.split('.').reverse().join('-')
            && query.get('dateTo') === endInput.split('.').reverse().join('-');
    }
    // Use the explicit date range without reopening calendar sheets.
    var dialog = routeReady ? null : document.querySelector('[role="dialog"]');
    if (dialog) {
        var inputs = Array.from(dialog.querySelectorAll('input:not([type="hidden"])'));
        if (inputs.length < 2) return JSON.stringify({ready: false, preparing: true});
        var label = inputs[0].value !== startInput ? startLabel : inputs[1].value !== endInput ? endLabel : '';
        if (label) {
            var dateButton = Array.from(dialog.querySelectorAll('button[aria-label]')).find(function (b) {
                return b.getAttribute('aria-label') === label;
            });
            if (dateButton && !dateButton.disabled) dateButton.click();
            else {
                // Older months may be mounted after scrolling the calendar upward.
                var scroll = Array.from(dialog.querySelectorAll('div')).find(function (e) {
                    return e.scrollHeight > e.clientHeight + 100 && /auto|scroll/.test(getComputedStyle(e).overflowY);
                });
                if (scroll) scroll.scrollTop = 0;
            }
            return JSON.stringify({ready: false, preparing: true});
        }
        var apply = Array.from(dialog.querySelectorAll('button')).find(function (b) { return b.innerText.trim() === 'Выбрать'; });
        if (apply) apply.click();
        return JSON.stringify({ready: false, preparing: true});
    }
    var period = filter.innerText;
    if (!routeReady && (period.indexOf(startShort) < 0 || period.indexOf(endShort) < 0)) {
        var open = filter.querySelector('button') || filter;
        if (open) open.click();
        return JSON.stringify({ready: false, preparing: true});
    }
    var result = esiPayExtract();
    var empty = /нет операций|операций пока нет|ничего не найдено|операции не найдены/i.test(document.body.innerText);
    if (!result.count && !empty) return JSON.stringify({ready: false});
    var root = document.scrollingElement || document.documentElement;
    result.ready = true;
    result.period = period;
    result.bottom = empty || root.scrollTop + window.innerHeight >= root.scrollHeight - 12;
    if (!result.bottom) window.scrollBy(0, Math.max(400, window.innerHeight * 0.75));
    return JSON.stringify(result);
})()
