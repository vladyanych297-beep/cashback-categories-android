const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function box(checked) { return {checked, disabled:false, click() {this.checked = !this.checked;}}; }
const oldBox = box(true), chosenBox = box(false);
const cards = [
    {innerText:'Вода\n10%', querySelector: () => oldBox},
    {innerText:'Уход за волосами\n15%', querySelector: () => chosenBox}
];
const source = fs.readFileSync('src/ru/local/lentacashback/MagnitActivity.java','utf8');
const method = source.slice(source.indexOf('protected String applyRecommendationsScript'), source.indexOf('protected String openReceiptScript'));
const code = [...method.matchAll(/"(?:[^"\\]|\\.)*"/g)].map(m => JSON.parse(m[0])).join('').replace('})()', '})(["Уход за волосами"])');
let saved = 0;
const context = {window:{}, setTimeout: callback => callback(),
    document:{querySelectorAll: selector => selector === '.cashback-card' ? cards : [{innerText:'Сохранить выбор', click:() => saved++}]}};
vm.runInNewContext(code,context);
assert.equal(oldBox.checked,false); assert.equal(chosenBox.checked,true); assert.equal(saved,1);
vm.runInNewContext(code,context);
assert.equal(saved,1);
const pyro = fs.readFileSync('res/raw/pyaterochka_apply.js','utf8').replace('__NAMES__','["Вода"]');
const water = box(false), yoghurt = box(true);
const buttons = [
    {innerText:'Вода 16%',parentElement:{querySelectorAll:()=>[water]}},
    {innerText:'Йогурты 10%',parentElement:{querySelectorAll:()=>[yoghurt]}}
];
vm.runInNewContext(pyro,{document:{querySelectorAll:()=>buttons},setTimeout:()=>{}});
assert.equal(water.checked,true); assert.equal(yoghurt.checked,false);
for(const filename of ['StoreActivity.java','LentaActivity.java']) {
    const code = fs.readFileSync('src/ru/local/lentacashback/'+filename,'utf8');
    const process = code.slice(code.indexOf('private void processCategories('),code.indexOf('private boolean categoriesFresh'));
    assert.ok(!process.includes('applyRecommendationsScript'));
    assert.ok(!process.includes('applyRecommendedCategories('));
    assert.ok(process.includes('if (reviewRequested)'));
    assert.ok(code.includes('Phase.VERIFY'));
    assert.ok(code.includes('reviewedSignature.equals'));
}
console.log('PASS: confirmed selections replace old choices, repeated scripts, and refresh never saves without approval.');
