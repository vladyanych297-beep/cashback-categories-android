function esiPayExtract() {
    var links = Array.from(document.querySelectorAll('a[href*="/my/transactions-history/operation"]'));
    var items = [], unreadable = 0;
    links.forEach(function (a) {
        var lines = a.innerText.split('\n').map(function (s) { return s.trim(); }).filter(Boolean);
        var amount = lines.find(function (s) { return /^[−–-]\s*[0-9][0-9\s.,]*\s*₽/.test(s); });
        if (!amount) return;
        var category = lines[1] || '';
        if (/перевод|пополн|снятие|погашение|комисси|проценты|начисление|возврат|списание баллов/i.test(category)) return;
        if (/отмен[её]н|отклон[её]н/i.test(a.innerText)) return;
        try {
            // The timestamp is part of the visible operation link, not hidden page state.
            var id = new URL(a.href, location.origin).searchParams.get('id') || '';
            var start = id.indexOf('[{'), end = id.lastIndexOf('}]');
            var metadata = start >= 0 && end >= start ? JSON.parse(id.slice(start, end + 2)) : [];
            var timed = metadata.find(function (x) { return x.operationTimestamp; });
            if (!timed) { unreadable++; return; }
            var date = new Date(new Date(timed.operationTimestamp).getTime() + 180 * 60000).toISOString().slice(0, 10);
            var cents = Math.round(Number(amount.replace(/[−–-]/g, '').replace(/[\s₽]/g, '').replace(',', '.')) * 100);
            if (!Number.isFinite(cents) || cents <= 0) return;
            var identity = metadata.find(function (x) { return x.chainId || x.operationId; });
            var key = identity ? identity.type + ':' + (identity.chainId || identity.operationId) : id;
            items.push({id: key, date: date, merchant: lines[0] || 'Покупка', category: category,
                cents: cents, cardPay: lines.some(function (s) { return s === 'Карта Пэй'; })});
        } catch (error) { unreadable++; }
    });
    return {items: items, count: links.length, unreadable: unreadable};
}
