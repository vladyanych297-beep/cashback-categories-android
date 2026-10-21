function esiTbankExtract() {
    var selector = '[data-qa-type="atom-operations-feed-operation-root"]';
    var rows = Array.from(document.querySelectorAll(selector));
    var items = [], unreadable = 0, seen = {}, presented = null, modelLoading = false;
    var feed = document.querySelector('[data-qa-type="atom-operations-feed-root"]');
    var key = feed && Object.keys(feed).find(function(k) { return k.indexOf('__reactFiber') === 0; });
    var fiber = key && feed[key];
    // The feed virtualizes DOM rows. Its presentation groups contain the full loaded range.
    for (var depth = 0; fiber && depth < 14; depth++, fiber = fiber.return) {
        var props = fiber.memoizedProps;
        if (props && Array.isArray(props.groupedOperations)) {
            presented = [];
            props.groupedOperations.forEach(function(group) {
                if (Array.isArray(group.operations)) group.operations.forEach(function(operation) {
                    if (operation && operation.operationId) presented.push(operation);
                });
            });
            modelLoading = !!props.loading;
            break;
        }
    }
    function accept(operation, row) {
        if (!operation) { unreadable++; return; }
        var money = operation.amount && operation.amount.params;
        var merchant = typeof operation.title === 'string' ? operation.title : '';
        var category = operation.subtitle && operation.subtitle.text || '';
        if (row) {
            var title = row.querySelector('[data-qa-type="atom-operations-feed-operation-title"]');
            var subtitle = row.querySelector('[data-qa-type="atom-operations-feed-operation-subtitle"]');
            merchant = title ? title.innerText.trim() : merchant;
            category = subtitle ? subtitle.innerText.trim() : category;
        }
        if (!money || money.currency !== 'RUB') return;
        if (money.sign !== 'force-negative') return;
        if (/перевод|пополн|снятие|погашение|комисси|проценты|возврат|начисление|списание баллов/i.test(category)) return;
        var status = operation.tag && operation.tag.text || '';
        if (/отмен[аёе]|отклон[её]н/i.test(status + ' ' + (row ? row.innerText : ''))) return;
        var id = operation.operationId, timestamp = Number(operation.operationTime);
        if (!id.value || !Number.isFinite(timestamp) || timestamp <= 0) { unreadable++; return; }
        var date = new Date(timestamp + 3 * 60 * 60 * 1000).toISOString().slice(0, 10);
        var cents = Math.round(Math.abs(Number(money.value)) * 100);
        if (!merchant || !category || !Number.isFinite(cents) || cents <= 0) { unreadable++; return; }
        var identity = String(id.source || '') + ':' + String(id.value);
        if (seen[identity]) return;
        seen[identity] = true;
        items.push({id:identity, date:date, merchant:merchant, category:category, cents:cents, cardPay:true});
    }
    if (presented) presented.forEach(function(operation) { accept(operation, null); });
    else rows.forEach(function(row) {
        var key = Object.keys(row).find(function(k) { return k.indexOf('__reactFiber') === 0; });
        var fiber = key && row[key], operation = null;
        for (var depth = 0; fiber && depth < 14; depth++, fiber = fiber.return) {
            if (fiber.memoizedProps && fiber.memoizedProps.operationId) { operation = fiber.memoizedProps; break; }
        }
        accept(operation, row);
    });
    return {items:items, count:presented ? presented.length : rows.length, unreadable:unreadable,
        modelComplete:presented !== null && !modelLoading, modelLoading:modelLoading};
}