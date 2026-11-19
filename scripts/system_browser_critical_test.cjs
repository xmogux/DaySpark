/* Core DaySpark workflows previously unverified; real Chrome + live server + isolated H2. */
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const {chromium} = require('playwright');
const BASE = process.env.DAYSPARK_URL || 'http://127.0.0.1:8080';
const OUT = path.resolve(process.env.SYSTEM_EVIDENCE_DIR || 'system-test-evidence/critical');
fs.mkdirSync(OUT,{recursive:true});
const results=[];
const stamp = Date.now().toString().slice(-10);
const todayKST = () => new Date().toLocaleDateString('sv-SE',{timeZone:'Asia/Seoul'});
function result(id,name,status,detail) {
  results.push({id,name,status,detail:String(detail||''),at:new Date().toISOString()});
  console.log('['+status+'] '+id+' '+name+' — '+String(detail||''));
  fs.writeFileSync(path.join(OUT,'results.json'),JSON.stringify({base:BASE,cases:results},null,2));
}
async function screenshot(page,id) {
  await page.screenshot({path:path.join(OUT,id+'.png'),fullPage:true});
}
async function add(page,title,opt={}) {
  await page.goto(BASE+'/challenges/new');
  await page.locator('[name=title]').fill(title);
  await page.locator('[name=category]').selectOption('STUDY');
  await page.locator('[name=minutes]').fill('2');
  await page.locator('[name=targetValue]').fill(String(opt.target??1));
  await page.locator('[name=targetUnit]').selectOption({label:opt.unit||'회'});
  if (opt.weekly) await page.locator('[name=weeklyTarget]').fill(String(opt.weekly));
  await Promise.all([
    page.waitForURL(u=>u.pathname.startsWith('/challenges')&&!u.pathname.includes('/new'),{timeout:15000}),
    page.locator('form.challenge-form button[type=submit]').click()
  ]);
  const card=page.locator('.manage-card').filter({has:page.getByRole('heading',{name:title})});
  await card.waitFor();
  const url=await card.locator('a[href*="/edit"]').getAttribute('href');
  const m=url?.match(/\/challenges\/(\d+)\/edit/);
  assert.ok(m,'challenge ID in edit URL');
  return m[1];
}
async function homeTask(page,title,klass='.task-card') {
  await page.goto(BASE+'/');
  const task=page.locator(klass).filter({has:page.getByRole('heading',{name:title})});
  await task.waitFor();
  return task;
}
async function finishToday(page,title,note,photo) {
  const task=await homeTask(page,title);
  await task.locator('button.complete-trigger').click();
  await page.locator('#completeModal[aria-hidden="false"]').waitFor();
  await page.locator('#note').fill(note);
  if(photo) await page.locator('#image').setInputFiles(photo);
  await Promise.all([
    page.waitForURL(u=>u.pathname==='/',{timeout:15000}),
    page.locator('#completeModal form button[type=submit]').click()
  ]);
  await page.locator('.task-card.completed').filter({has:page.getByRole('heading',{name:title})}).waitFor();
}
function focusState(page){return page.request.get(BASE+'/api/focus/current').then(r=>r.json());}
function summary(page){return page.request.get(BASE+'/api/focus/summary').then(r=>r.json());}
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true,args:['--no-sandbox']});
 const context=await browser.newContext({viewport:{width:1440,height:900},locale:'ko-KR',timezoneId:'Asia/Seoul'});
 const page=await context.newPage();
 page.setDefaultTimeout(15000);
 let failures=0;
 async function test(id,name,fn) {
   try { await fn(); result(id,name,'PASS','브라우저 화면과 서버 결과 확인'); }
   catch(e){failures++;result(id,name,'FAIL',e.stack||e.message||e);try{await screenshot(page,id+'-failure')}catch(_){}}
 }
 try {
  await test('SYS-08','도전 쉬기·재개와 오늘 계획 반영',async()=>{
    const title='SYS08-쉬기-'+stamp;
    await add(page,title);
    await homeTask(page,title);
    await page.goto(BASE+'/challenges');
    const card=page.locator('.manage-card').filter({has:page.getByRole('heading',{name:title})});
    await Promise.all([
      page.waitForURL(u=>u.pathname.startsWith('/challenges')),
      card.getByRole('button',{name:'쉬기'}).click()
    ]);
    await page.locator('.manage-card.inactive').filter({has:page.getByRole('heading',{name:title})}).waitFor();
    await page.goto(BASE+'/');
    assert.equal(await page.locator('.task-card').filter({has:page.getByRole('heading',{name:title})}).count(),0);
    await screenshot(page,'sys08-paused');
    await page.goto(BASE+'/challenges');
    const card2=page.locator('.manage-card.inactive').filter({has:page.getByRole('heading',{name:title})});
    await Promise.all([
      page.waitForURL(u=>u.pathname.startsWith('/challenges')),
      card2.getByRole('button',{name:'재개'}).click()
    ]);
    await homeTask(page,title);
    await screenshot(page,'sys08-resumed');
  });

  await test('SYS-09','주간 목표 설정과 완료 횟수 표시',async()=>{
    const title='SYS09-주간목표-'+stamp;
    const id=await add(page,title,{weekly:2});
    const task=await homeTask(page,title);
    assert.match(await task.innerText(),/이번 주\s*0\s*\/\s*2회/);
    await finishToday(page,title,'주간목표 검증');
    const done=page.locator('.task-card.completed').filter({has:page.getByRole('heading',{name:title})});
    assert.match(await done.innerText(),/이번 주\s*1\s*\/\s*2회/);
    await page.goto(BASE+'/challenges/'+id);
    assert.equal(await page.locator('input[name=weeklyTarget]').inputValue(),'2');
    await screenshot(page,'sys09-weekly');
  });

  await test('SYS-10','실제 PNG 업로드·조회·완료 취소 시 사진 삭제',async()=>{
    const title='SYS10-사진-'+stamp;
    await add(page,title);
    const onePixel=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9YKJt0QAAAAASUVORK5CYII=','base64');
    await finishToday(page,title,'사진 업로드 검증',{name:'proof.png',mimeType:'image/png',buffer:onePixel});
    await page.goto(BASE+'/history');
    const record=page.locator('.history-card').filter({has:page.getByRole('heading',{name:title})});
    await record.waitFor();
    const img=record.locator('img[src^="/uploads/"]');
    const src=await img.getAttribute('src');
    assert.ok(src,'uploaded photo must be linked in record');
    const response=await page.request.get(BASE+src);
    assert.equal(response.status(),200);
    assert.match(response.headers()['content-type']||'',/image\/png/);
    assert.deepEqual(await response.body(),onePixel);
    await screenshot(page,'sys10-photo-history');
    await page.goto(BASE+'/');
    const completed=page.locator('.task-card.completed').filter({has:page.getByRole('heading',{name:title})});
    page.once('dialog',dialog=>dialog.accept());
    await Promise.all([
      page.waitForURL(u=>u.pathname==='/',{timeout:15000}),
      completed.locator('form.undo-form button[type=submit]').click()
    ]);
    assert.equal((await page.request.get(BASE+src)).status(),404,'undo deletes uploaded file');
    await page.goto(BASE+'/history');
    assert.equal(await page.getByRole('heading',{name:title}).count(),0);
  });

  await test('SYS-11','진행 중인 집중 도전을 다른 도전으로 전환',async()=>{
    const a='SYS11-A-'+stamp,b='SYS11-B-'+stamp;
    const aid=await add(page,a,{unit:'분',target:10});
    const bid=await add(page,b,{unit:'분',target:10});
    await page.goto(BASE+'/focus');
    await page.locator('#focusDare').selectOption(aid);
    await page.locator('#focusMinutes').fill('2');
    await page.locator('#startTimer').click();
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('집중 시작'));
    const first=await focusState(page);assert.equal(String(first.dareId),aid);
    page.once('dialog',d=>d.accept());
    await page.locator('#focusDare').selectOption(bid);
    await page.waitForFunction(t=>document.querySelector('#timerStatus')?.textContent.includes(t),b);
    const second=await focusState(page);
    assert.equal(String(second.dareId),bid);
    assert.notEqual(second.id,first.id);
    assert.equal(second.status,'RUNNING');
    assert.equal((await (await page.request.post(BASE+'/api/focus/'+first.id+'/stop')).json()).status,'STOPPED');
    await screenshot(page,'sys11-switched');
    page.once('dialog',d=>d.accept());
    await page.locator('#resetTimer').click();
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('종료'));
  });

  await test('SYS-12','진행 중인 세션 새로고침 복원',async()=>{
    const title='SYS12-복원-'+stamp;
    const did=await add(page,title,{unit:'분',target:15});
    await page.goto(BASE+'/focus');
    await page.locator('#focusDare').selectOption(did);
    await page.locator('#focusMinutes').fill('2');
    await page.locator('#startTimer').click();
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('집중 시작'));
    const before=await focusState(page);
    await page.reload();
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('복원'));
    const after=await focusState(page);
    assert.equal(after.id,before.id);
    assert.equal(after.status,'RUNNING');
    assert.equal(await page.locator('#focusDare').inputValue(),did);
    assert.equal(await page.locator('#focusMinutes').inputValue(),'2');
    await screenshot(page,'sys12-restored');
    page.once('dialog',d=>d.accept());
    await page.locator('#resetTimer').click();
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('종료'));
  });

  await test('SYS-13','실제 1분×2회 반복 집중 누적',async()=>{
    const title='SYS13-두회차-'+stamp;
    const did=await add(page,title,{unit:'분',target:2});
    await page.goto(BASE+'/focus');
    const before=await summary(page);
    await page.locator('#focusDare').selectOption(did);
    await page.locator('#focusMinutes').fill('1');
    await page.locator('#breakMinutes').fill('0');
    await page.locator('#repeatCount').fill('2');
    await page.locator('#startTimer').click();
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('집중 시작'));
    await page.waitForFunction(()=>document.querySelector('#roundLabel')?.textContent.includes('2 / 2회차'),null,{timeout:110000,polling:500});
    await screenshot(page,'sys13-second-round');
    await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('2회 반복 완료'),null,{timeout:110000,polling:500});
    const after=await summary(page);
    assert.equal(after.todayCount,before.todayCount+2,'two completed focus rounds');
    assert.equal(after.todayMinutes,before.todayMinutes+2,'exact two focused minutes earned');
    await screenshot(page,'sys13-two-rounds-done');
    await homeTask(page,title,'.task-card.completed');
  });

  await test('SYS-14','허용되지 않은 사진 확장자 거부',async()=>{
    const title='SYS14-유효성-'+stamp;
    await add(page,title);
    const task=await homeTask(page,title);
    await task.locator('button.complete-trigger').click();
    await page.locator('#completeModal[aria-hidden="false"]').waitFor();
    await page.locator('#image').setInputFiles({name:'bad.txt',mimeType:'text/plain',buffer:Buffer.from('not a valid photo')});
    await Promise.all([
      page.waitForURL(u=>u.pathname==='/',{timeout:15000}),
      page.locator('#completeModal form button[type=submit]').click()
    ]);
    assert.match(await page.locator('.toast.error').innerText(),/JPG|PNG|WEBP|GIF/);
    assert.equal(await page.locator('.task-card.completed').filter({has:page.getByRole('heading',{name:title})}).count(),0);
    await screenshot(page,'sys14-invalid-photo');
  });
 } finally {
   await browser.close();
   fs.writeFileSync(path.join(OUT,'results.json'),
      JSON.stringify({base:BASE,status:failures?'FAIL':'PASS',cases:results},null,2));
 }
 if(failures)process.exitCode=1;
})().catch(e=>{
 console.error('FATAL',e);
 fs.writeFileSync(path.join(OUT,'fatal.txt'),String(e.stack||e));
 process.exitCode=2;
});