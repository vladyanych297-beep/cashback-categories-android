const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const code = fs.readFileSync('res/raw/magnit_categories.js','utf8')
  .replaceAll('__NEXT_MONTH__','Ноябрь').replaceAll('__CURRENT_MONTH__','октябрь 2026');
const active = ['Чипсы\n5%', 'Молоко\n20%'].map(innerText => ({innerText,
  querySelector:()=>({innerText:innerText.split('\n')[0]})}));
const locked = JSON.parse(vm.runInNewContext(code, {window:{}, document:{
  body:{innerText:'Категории с кешбэком\nУвеличить до 13 категорий'},
  querySelector:()=>({querySelectorAll:()=>[]}),
  querySelectorAll:selector=>selector==='.favorite-section-categories-item'?active:[]
}}));
assert.equal(locked.readOnly,true);
assert.equal(locked.month,'Действующие категории Магнита — октябрь 2026');
assert.equal(locked.limit,2);
assert.equal(locked.items[1].rate,20);
const cards = [{innerText:'Молоко\n20%',querySelector:()=>({checked:true})},
  {innerText:'Чипсы\n5%',querySelector:()=>({checked:false})},
  {innerText:'Кешбэк с подпиской\n30%',querySelector:()=>null}];
const picker = JSON.parse(vm.runInNewContext(code, {window:{}, document:{
  body:{innerText:'Выберите ещё 9 категорий с кешбэком в ноябре'},
  querySelector:()=>null, querySelectorAll:selector=>selector==='.cashback-card'?cards:[]
}}));
assert.equal(picker.limit,10);
assert.equal(picker.items.length,2);
assert.equal(picker.readOnly,undefined);
console.log('PASS: Magnit active categories, rates, locked selection, remaining slots and subscription exclusion.');
