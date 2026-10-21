const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const code=fs.readFileSync('res/raw/yandex_pay_categories.js','utf8');
const cards=[{innerText:'Аптеки\n5%',querySelector:()=>({innerText:'Аптеки'})},
  {innerText:'Электроника\n5%',querySelector:()=>({innerText:'Электроника'})}];
for(const [chip,pay] of [['Карта Пэй',true],['Другие карты',false]]) {
  const result=JSON.parse(vm.runInNewContext(code,{document:{querySelectorAll:()=>[],
    querySelector(selector) {
      if(selector.includes('current-page-section'))return {querySelectorAll:()=>cards};
      if(selector.includes('current-page-header'))return {innerText:'Выгода в октябре'};
      if(selector.includes('__activeChip'))return {innerText:chip};
      return null;
    }}}));
  assert.equal(result.ready,true);assert.equal(result.readOnly,true);
  assert.equal(result.month,'Выгода в октябре');assert.equal(result.items.length,2);
  assert.equal(result.items[0].rate,5);assert.equal(result.items[0].cardPay,pay);
  assert.equal(result.items[0].selected,true);
}
console.log('PASS: current Yandex benefits, period, rates, selected state and payment card restrictions.');
