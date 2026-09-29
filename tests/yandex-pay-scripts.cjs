const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const extract = fs.readFileSync(path.join(__dirname, '../res/raw/yandex_pay_extract.js'), 'utf8');
const categories = fs.readFileSync(path.join(__dirname, '../res/raw/yandex_pay_categories.js'), 'utf8');
const history = fs.readFileSync(path.join(__dirname, '../res/raw/yandex_pay_history.js'), 'utf8');

function operation(text, time, id) {
    const metadata = [{type: 'TEST', operationId: id, operationTimestamp: time}];
    return {innerText: text, href: 'https://bank.yandex.ru/my/transactions-history/operation?id=' + encodeURIComponent('V4::' + JSON.stringify(metadata))};
}
const nodes = [
    operation('Тестовый магазин\nСупермаркеты\n−1\u00a0234,56\u00a0₽\n+12\nКарта Пэй', '2026-06-21T22:30:00Z', 'one'),
    operation('Пополнение\nАвтопополнение\n+1000 ₽\nКарта Пэй', '2026-06-22T10:00:00Z', 'credit'),
    operation('Перевод\nПереводы\n−1000 ₽\nКарта Пэй', '2026-06-22T10:00:00Z', 'transfer'),
    operation('Другой магазин\nЭлектроника и бытовая техника\n-100 ₽\nКарта · 0000', '2026-06-22T10:00:00Z', 'other'),
    operation('Магазин\nСупермаркеты\n−99 ₽\nОтменён', '2026-06-22T10:00:00Z', 'cancelled'),
    {innerText: 'Магазин\nСупермаркеты\n−99 ₽', href: 'https://bank.yandex.ru/my/transactions-history/operation?id=unknown'}
];
const result = vm.runInNewContext(extract + '\nesiPayExtract()', {
    document: {querySelectorAll: () => nodes}, location: {origin: 'https://bank.yandex.ru'}, URL, Date
});
assert.equal(result.count, 6);
assert.equal(result.items.length, 2);
assert.equal(result.items[0].cents, 123456);
assert.equal(result.items[0].date, '2026-06-22', 'Bank day must use Moscow timezone');
assert.equal(result.items[0].id, 'TEST:one');
assert.equal(result.items[0].cardPay, true);
assert.equal(result.items[1].cardPay, false);
assert.equal(result.unreadable, 1);

function card(text, selected = false) {
    return {tagName: 'DIV', innerText: text, querySelector: selector => selector === '[role="button"]' ? {innerText: text} : {checked: selected}};
}
const cards = [card('1%\u00a0Все покупки'), card('−50% Еда и Деливери\nСкидка на доставку'), card('100% ОСАГО\nС Яндекс Заботой', true)];
const categoryResult = JSON.parse(vm.runInNewContext(categories, {document: {
    querySelector: () => ({innerText: 'Выберите 5 из 12 категорий на октябрь'}),
    querySelectorAll: selector => selector === '[data-test-id="selector-page-list-item"]' ? cards : [
        {tagName: 'H2', innerText: 'С вашей картой Пэй'}, cards[0], cards[1],
        {tagName: 'H2', innerText: 'С картой любого банка'}, cards[2]]
}}));
assert.equal(categoryResult.ready, true);
assert.equal(categoryResult.limit, 5);
assert.equal(categoryResult.items[0].cardPay, true);
assert.equal(categoryResult.items[1].kind, 'discount');
assert.equal(categoryResult.items[2].cardPay, false);
assert.equal(categoryResult.items[2].selected, true);
assert.equal(categoryResult.items[2].rate, 100);

const prepared = history.replaceAll('__START_LABEL__', '22 Июнь 2026').replaceAll('__END_LABEL__', '29 Сентябрь 2026')
    .replaceAll('__START_INPUT__', '22.06.2026').replaceAll('__END_INPUT__', '29.09.2026')
    .replaceAll('__START_SHORT__', '22.06').replaceAll('__END_SHORT__', '29.09');
let clicked;
const inputs = [{value: '01.09.2026'}, {value: '29.09.2026'}];
const buttons = ['22 Июнь 2026', '29 Сентябрь 2026'].map(label => ({getAttribute: () => label, click: () => {clicked = label;}}));
const dialog = {querySelectorAll: selector => selector.startsWith('input') ? inputs : buttons};
const dateDocument = {querySelector: selector => selector.includes('date-filter-chip') ? {} : dialog};
assert.equal(JSON.parse(vm.runInNewContext(prepared, {document: dateDocument})).preparing, true);
assert.equal(clicked, '22 Июнь 2026');
inputs[0].value = '22.06.2026'; inputs[1].value = '';
vm.runInNewContext(prepared, {document: dateDocument});
assert.equal(clicked, '29 Сентябрь 2026');
let scrolled = false;
const readDocument = {
    querySelector: selector => selector.includes('date-filter-chip') ? {innerText: '22.06 – 29.09'} : null,
    scrollingElement: {scrollTop: 0, scrollHeight: 5000}, body: {innerText: 'История покупок'}
};
const batch = JSON.parse(vm.runInNewContext(prepared, {document: readDocument,
    window: {innerHeight: 700, scrollBy: () => {scrolled = true;}}, esiPayExtract: () => ({count: 2, items: [{id: 'one'}]})}));
assert.equal(batch.ready, true); assert.equal(batch.bottom, false); assert.equal(scrolled, true);
readDocument.scrollingElement.scrollTop = 4300;
assert.equal(JSON.parse(vm.runInNewContext(prepared, {document: readDocument,
    window: {innerHeight: 700}, esiPayExtract: () => ({count: 2, items: []})})).bottom, true);
const mobileRoute = {querySelector: selector => selector.includes('date-filter-chip') ? {innerText: ''} : dialog,
    scrollingElement: {scrollTop: 4300, scrollHeight: 5000}, body: {innerText: 'История'}};
const routed = JSON.parse(vm.runInNewContext(prepared, {document: mobileRoute, URL,
    location: {href: 'https://bank.yandex.ru/my/transactions-history?dateFrom=2026-06-22&dateTo=2026-09-29'},
    window: {innerHeight: 700}, esiPayExtract: () => ({count: 2, items: []})}));
assert.equal(routed.ready, true);
console.log('PASS: Yandex Pay DOM parsing, Moscow dates, category percentages, card restrictions, calendar selection, and scrolling.');
