const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const code = fs.readFileSync('res/raw/pyaterochka_categories.js', 'utf8').replace('__NEXT_MONTH__', 'Октябрь');
const labels = ['Мюсли 30%', 'Almette - натуральный творожный сыр И на товары со скидкой 20%',
    'Вода 16%', 'Йогурты 10%', 'Кешбэк с подпиской 30%', 'Вода 16%'];
let clicks = 0;
const buttons = labels.map(innerText => ({innerText}));
buttons.unshift({innerText: 'Октябрь 3', click: () => clicks++});
const context = {window: {}, document: {body: {innerText: 'Выберите 3'}, querySelectorAll: () => buttons}};
assert.equal(JSON.parse(vm.runInNewContext(code, context)).ready, false);
const result = JSON.parse(vm.runInNewContext(code, context));
assert.equal(clicks, 1);
assert.equal(result.limit, 3);
assert.equal(result.items.length, 4);
assert.equal(result.items[1].name, 'Almette - натуральный творожный сыр');
assert.equal(result.items[1].rate, 20);
assert.equal(result.month, 'Категории Пятёрочки — Октябрь');
let opened = 0;
const hidden = {innerText: 'Выбрать', getClientRects: () => [], click: () => { throw new Error('Hidden link'); }};
const visible = {innerText: 'Выбрать', getClientRects: () => [1], click: () => opened++};
const opening = {window: {}, document: {body: {innerText: ''},
    querySelectorAll: selector => selector === 'button,[role="button"]' ? [] : [hidden, visible]}};
assert.equal(JSON.parse(vm.runInNewContext(code, opening)).ready, false);
assert.equal(opened, 1);
console.log('PASS: actual category card labels, next month, promotion text, limits and duplicates.');
