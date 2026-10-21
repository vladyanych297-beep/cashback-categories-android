const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/ru/local/lentacashback/LentaActivity.java', 'utf8');
function script(name) {
  const start = source.indexOf('private static final String ' + name + ' =');
  const end = source.indexOf(';\n', start);
  return [...source.slice(start, end).matchAll(/"(?:[^"\\]|\\.)*"/g)]
    .map(m => JSON.parse(m[0])).join('');
}
for (const label of ['Магазин', 'Магазины']) {
  let clicks = 0;
  const tab = {textContent: label, className: 'segment active', children: [],
    closest() { return this; }, getAttribute() { return null; }, click() { clicks++; }};
  const link = {href: 'https://lenta.com/my-account/order/shop/45%2F2026-10-20/'};
  const context = {window: {}, document: {body: {innerText: 'История покупок'},
    querySelectorAll(selector) {
      if (selector === '[role=tab]' || selector.includes('button.segment')) return [tab];
      if (selector.includes('a[href*=')) return [link];
      return [];
    }}};
  const result = JSON.parse(vm.runInNewContext(script('HISTORY_JS'), context));
  assert.equal(result.ready, true);
  assert.equal(result.shopFound, true);
  assert.equal(result.count, 1);
  assert.equal(result.source, 'links');
  assert.equal(clicks, 0);
}
const detail = JSON.parse(vm.runInNewContext(script('DETAIL_JS'), {
  location: {href:'https://lenta.com/my-account-mobile/order/shop/45%2F2026-10-20/'},
  document: {body: {innerText:'Экономия с Лентой\n20,00 ₽'},
    querySelectorAll: () => [{innerText:'Сметана\n50,00 ₽/шт\n100,00 ₽ · 2 шт'}]}
}));
assert.equal(detail.ready, true);
assert.equal(detail.date, '2026-10-20');
assert.equal(detail.url, 'https://lenta.com/my-account/order/shop/45%2F2026-10-20/');
assert.equal(detail.paid, 100);
assert.equal(detail.items[0].name, 'Сметана');
console.log('PASS: both store tab labels, receipt links, mobile receipt parsing and canonical identity.');
