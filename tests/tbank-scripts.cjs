const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const {X509Certificate,createHash}=require('node:crypto');
const extract=fs.readFileSync('res/raw/tbank_extract.js','utf8');
function row(id,category,sign='force-negative',currency='RUB',date='2026-07-13T21:30:00Z') {
  const values={title:{innerText:'Магазин'},subtitle:{innerText:category},amount:{innerText:sign==='force-negative'?'−123,45 ₽':'+123,45 ₽'}};
  return {innerText:'Магазин',querySelector:s=>values[s.match(/operation-(title|subtitle|amount)/)[1]],
    __reactFiberTest:{memoizedProps:{operationId:{value:id,source:'bank'},operationTime:Date.parse(date),amount:{params:{value:123.45,sign,currency}}}}};
}
const rows=[row('one','Супермаркеты'),row('one','Супермаркеты'),row('two','Супермаркеты'),
  row('credit','Супермаркеты','force-positive'),row('transfer','Переводы'),row('foreign','Кино','force-negative','USD'),
  {innerText:'Нераспознанная операция'}];
const parsed=vm.runInNewContext(extract+'\nesiTbankExtract()', {document:{querySelector:()=>null,querySelectorAll:()=>rows}});
assert.equal(parsed.items.length,2);assert.equal(parsed.items[0].date,'2026-07-14');
assert.equal(parsed.items[0].cents,12345);assert.equal(parsed.unreadable,1);
assert.notEqual(parsed.items[0].id,parsed.items[1].id);
const presentation = ['hidden-one','hidden-two'].map(id=>({operationId:{value:id,source:'bank'},
  operationTime:Date.parse('2026-07-14T10:00:00Z'),title:'Магазин',subtitle:{text:'Супермаркеты'},
  amount:{params:{value:123.45,sign:'force-negative',currency:'RUB'}}}));
const model=vm.runInNewContext(extract+'\nesiTbankExtract()', {document:{
  querySelector:()=>({__reactFiberFeed:{memoizedProps:{groupedOperations:[{operations:presentation}],loading:false}}}),
  querySelectorAll:()=>[]}});
assert.equal(model.items.length,2);assert.equal(model.count,2);assert.equal(model.modelComplete,true);
const categories=fs.readFileSync('res/raw/tbank_categories.js','utf8').replace('__AUTO_OPEN__','true').replace('__NEXT_MONTH_STEM__','ноябр');
const items=['1% Все покупки','5% Кино','5% Кино'].map(innerText=>({innerText}));
const offers=JSON.parse(vm.runInNewContext(categories,{document:{body:{innerText:''},
  querySelector:s=>s.includes('title')?{innerText:'Повышенный кэшбэк в октябре'}:s.includes('task-success')?{}:null,
  querySelectorAll:s=>s.includes('category_item')?items:[]},location:{href:'https://www.tbank.ru/mybank/bonuses/high-cashback/offer/test/'}}));
assert.equal(offers.readOnly,true);assert.equal(offers.items.length,2);assert.equal(offers.items[1].rate,5);
assert.equal(offers.items[0].selected,true);assert.equal(offers.month,'Повышенный кэшбэк в октябре');
let opened='';
const programs=['Повышенный кэшбэк в октябре','Повышенный кэшбэк в ноябре'].map(innerText=>({innerText,click:()=>opened=innerText}));
const navigation=JSON.parse(vm.runInNewContext(categories,{window:{},document:{querySelector:()=>null,
  querySelectorAll:s=>s.includes('aggregated-card')?programs:[]}}));
assert.equal(navigation.ready,false);assert.equal(opened,'Повышенный кэшбэк в ноябре');
let month=10,open=false;const chosen=[],state={};
const calendar={querySelector(s){
  const date=s.match(/data-date="([^"]+)"/);
  if(date)return Number(date[1].slice(5,7))===month?{getAttribute:()=>null,click:()=>chosen.push(date[1])}:null;
  if(s.includes('Назад'))return {click:()=>month--};
  if(s.includes('Вперёд'))return {click:()=>month++};
},querySelectorAll(s){return s==='button'?[{innerText:'Готово',click:()=>open=false}]:[{getAttribute:()=>`2026-${String(month).padStart(2,'0')}-15`}];}};
const history=fs.readFileSync('res/raw/tbank_history.js','utf8').replace('__START_ISO__','2026-07-14').replace('__END_ISO__','2026-10-21');
const context={window:state,esiTbankExtract:()=>({items:[],count:1,unreadable:0}),document:{body:{innerText:''},
  scrollingElement:{scrollTop:0,clientHeight:1000,scrollHeight:1000},querySelector(s){
    if(s.includes('date-chip'))return {innerText:'14 июля − 21 окт',click:()=>open=true};
    if(s.includes('popoverBlock'))return open?calendar:null;
    return null;
  }}};
let result;for(let i=0;i<15;i++)result=JSON.parse(vm.runInNewContext(history,context));
assert.deepEqual(chosen,['2026-07-14','2026-10-21']);assert.equal(result.ready,true);
assert.equal(state.__esiTbankRange,'2026-07-14/2026-10-21');
const cert=new X509Certificate(fs.readFileSync('res/raw/russian_trusted_root.pem'));
assert.equal(cert.verify(cert.publicKey),true);
assert.equal(createHash('sha256').update(cert.raw).digest('hex'),'d26d2d0231b7c39f92cc738512ba54103519e4405d68b5bd703e9788ca8ecf31');
console.log('PASS: T-Bank identities, Moscow dates, kopecks, non-purchases, selected categories, 100-day calendar and official CA fingerprint.');
