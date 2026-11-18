/* Four additional real-browser E2E scenarios against a live DaySpark server. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');

const BASE = process.env.DAYSPARK_URL || 'http://127.0.0.1:8080';
const OUT = path.resolve(process.env.SYSTEM_EVIDENCE_DIR || 'system-test-evidence/expanded');
fs.mkdirSync(OUT, {recursive:true});
const results = [];
const dateInKorea = (delta = 0) => new Date(Date.now() + delta * 86400000)
    .toLocaleDateString('sv-SE', {timeZone:'Asia/Seoul'});
const weekdays = ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'];
const weekdayNow = () => new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Seoul'})).getDay();
function save() {
  fs.writeFileSync(path.join(OUT,'results.json'),
    JSON.stringify({base:BASE,cases:results},null,2));
}
function logResult(id, name, status, detail) {
  results.push({id,name,status,detail:String(detail||''),at:new Date().toISOString()});
  console.log('[' + status + '] ' + id + ' ' + name + (detail ? ' — ' + detail : ''));
  save();
}
async function snap(page, name) {
  await page.screenshot({path:path.join(OUT,name+'.png'),fullPage:true});
}
async function createDare(page, title, options={}) {
  await page.goto(BASE+'/challenges/new');
  await page.locator('input[name="title"]').fill(title);
  await page.locator('select[name="category"]').selectOption('STUDY');
  await page.locator('input[name="minutes"]').fill('2');
  await page.locator('input[name="targetValue"]').fill(String(options.target||1));
  await page.locator('select[name="targetUnit"]').selectOption({label:options.unit||'회'});
  if(options.customDay) {
    await page.locator('input[name="repeatType"][value="CUSTOM"]').check();
    await page.locator('input[name="repeatDays"][value="'+options.customDay+'"]').check();
  }
  if(options.review) {
    await page.locator('#reviewEnabled').check({force:true});
    await page.locator('#reviewBaseDate').fill(dateInKorea(-1));
    await page.locator('#reviewIntervals').fill('1,3,7');
  }
  await Promise.all([
    page.waitForURL(url=>url.pathname.startsWith('/challenges') &&
      !url.pathname.startsWith('/challenges/new'),{timeout:15000}),
    page.getByRole('button',{name:/도전 만들기/}).click()
  ]);
  const card=page.locator('.manage-card').filter({has:page.getByRole('heading',{name:title})});
  await card.waitFor({state:'visible'});
  const href=await card.locator('a[href$="/edit"]').getAttribute('href');
  assert.ok(href, 'edit link must exist');
  const match=href.match(/\/challenges\/(\d+)\/edit/);
  assert.ok(match, 'challenge id must be present in edit link');
  return match[1];
}

(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true,args:['--no-sandbox']});
 const context=await browser.newContext({
   viewport:{width:1440,height:900},locale:'ko-KR',timezoneId:'Asia/Seoul'
 });
 const page=await context.newPage();
 page.setDefaultTimeout(15000);
 const suffix=Date.now().toString().slice(-9);
 let failures=0;
 async function runCase(id,name,fn) {
   try { await fn(); logResult(id,name,'PASS','실제 Chrome UI 및 서버 상태 확인'); }
   catch(err) {
     failures++;
     logResult(id,name,'FAIL',err.stack||err.message||err);
     try {await snap(page,id.toLowerCase()+'-failure');}catch(_){}
   }
 }
 try {
   await runCase('SYS-04','반복 요일 변경 → 오늘·달력 반영',async()=>{
     const title='SYS04-반복요일-'+suffix;
     const day=weekdayNow(), todayDay=weekdays[day], tomorrowDay=weekdays[(day+1)%7];
     const id=await createDare(page,title,{customDay:tomorrowDay});
     await page.goto(BASE+'/calendar/day?date='+dateInKorea());
     assert.equal(await page.locator('.cal-detail-row a').filter({hasText:title}).count(),0,
       '오늘이 아닌 요일 반복은 오늘 달력에 표시되지 않아야 함');
     await snap(page,'sys04-before');

     await page.goto(BASE+'/challenges/'+id+'/edit');
     await page.locator('input[name="repeatDays"][value="'+todayDay+'"]').check();
     await Promise.all([
       page.waitForURL(url=>url.pathname.startsWith('/challenges') && !url.pathname.includes('/edit')),
       page.getByRole('button',{name:/수정 저장/}).click()
     ]);
     await page.goto(BASE+'/calendar/day?date='+dateInKorea());
     await page.locator('.cal-detail-row a').filter({hasText:title}).waitFor({state:'visible'});
     await snap(page,'sys04-calendar-updated');
     await page.goto(BASE+'/');
     await page.locator('.task-card').filter({has:page.getByRole('heading',{name:title})}).waitFor();
   });

   await runCase('SYS-05','타이머 일시정지 → 재개 → 중간 종료',async()=>{
     const title='SYS05-타이머-'+suffix;
     await createDare(page,title,{target:10,unit:'분'});
     await page.goto(BASE+'/focus');
     await page.locator('#focusDare').selectOption({label:title});
     await page.locator('#focusMinutes').fill('2');
     await page.locator('#breakMinutes').fill('0');
     await page.locator('#repeatCount').fill('1');
     const beforeSummary=await (await page.request.get(BASE+'/api/focus/summary')).json();
     await page.locator('#startTimer').click();
     await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('집중 시작'));
     await page.locator('#pauseTimer').click();
     await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('일시정지'));
     const paused1=await (await page.request.get(BASE+'/api/focus/current')).json();
     assert.equal(paused1.status,'PAUSED');
     await page.waitForTimeout(1500);
     const paused2=await (await page.request.get(BASE+'/api/focus/current')).json();
     assert.equal(paused2.status,'PAUSED');
     assert.ok(Math.abs(paused1.remainingMillis-paused2.remainingMillis)<200,
       '일시정지 중에는 타이머 시간이 감소하지 않아야 함');
     await snap(page,'sys05-paused');

     await page.locator('#pauseTimer').click();
     await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('이어갑니다'));
     const running=await (await page.request.get(BASE+'/api/focus/current')).json();
     assert.equal(running.status,'RUNNING');
     page.once('dialog',dialog=>dialog.accept());
     await page.locator('#resetTimer').click();
     await page.waitForFunction(()=>document.querySelector('#timerStatus')?.textContent.includes('종료해서') ||
       document.querySelector('#timerStatus')?.textContent.includes('종료했어요'));
     const current=await (await page.request.get(BASE+'/api/focus/current')).json();
     assert.equal(current.active,false,'종료 후 활성 세션 없음');
     const afterSummary=await (await page.request.get(BASE+'/api/focus/summary')).json();
     assert.equal(afterSummary.todayCount,beforeSummary.todayCount,'중간 종료는 완료 회차로 기록하지 않음');
     await snap(page,'sys05-stopped');
   });

   await runCase('SYS-06','복습 일정 설정 → 복습 완료 → 달력 표시',async()=>{
     const title='SYS06-복습-'+suffix;
     const id=await createDare(page,title,{review:true});
     await page.goto(BASE+'/');
     const card=page.locator('.review-home-card').filter({has:page.getByRole('heading',{name:title})});
     await card.waitFor();
     await snap(page,'sys06-review-due');
     await Promise.all([
       page.waitForURL(url=>url.pathname==='/' || url.pathname.endsWith(';jsessionid')),
       card.getByRole('button',{name:'복습 완료'}).click()
     ]);
     await page.locator('.review-home-card.completed').filter({
       has:page.getByRole('heading',{name:title})
     }).waitFor();
     await snap(page,'sys06-review-completed');
     await page.goto(BASE+'/challenges/'+id);
     await page.locator('.review-row.is-done').filter({hasText:'1차 복습'}).waitFor();
     await page.goto(BASE+'/calendar/day?date='+dateInKorea());
     const reviewRow=page.locator('.cal-detail-row').filter({hasText:title});
     await reviewRow.getByText('완료됨').waitFor();
     await snap(page,'sys06-calendar-review-done');
   });

   await runCase('SYS-07','도전 삭제 → 목록 및 완료 기록 제거',async()=>{
     const title='SYS07-삭제-'+suffix;
     await createDare(page,title);
     await page.goto(BASE+'/');
     const task=page.locator('.task-card').filter({has:page.getByRole('heading',{name:title})});
     await task.locator('button.complete-trigger').click();
     await page.locator('#completeModal[aria-hidden="false"]').waitFor();
     await page.locator('#note').fill('SYS07-deleted-evidence-'+suffix);
     await Promise.all([
       page.waitForURL(url=>url.pathname==='/',{timeout:15000}),
       page.locator('#completeModal form button[type=submit]').click()
     ]);
     await page.goto(BASE+'/history');
     await page.getByRole('heading',{name:title}).waitFor();
     await page.goto(BASE+'/challenges');
     const card=page.locator('.manage-card').filter({has:page.getByRole('heading',{name:title})});
     await card.waitFor();
     await snap(page,'sys07-before-delete');
     page.once('dialog',dialog=>dialog.accept());
     await Promise.all([
       page.waitForURL(url=>url.pathname.startsWith('/challenges') &&
         !/\/\d+\/delete/.test(url.pathname),{timeout:15000}),
       card.getByRole('button',{name:'삭제'}).click()
     ]);
     assert.equal(await page.getByRole('heading',{name:title}).count(),0,
       '삭제된 도전은 목록에서 사라져야 함');
     await snap(page,'sys07-after-delete');
     await page.goto(BASE+'/history');
     assert.equal(await page.getByRole('heading',{name:title}).count(),0,
       '삭제된 도전의 완료 기록도 제거되어야 함');
     await snap(page,'sys07-history-clean');
   });
 } finally {
   await browser.close();
   fs.writeFileSync(path.join(OUT,'results.json'),
     JSON.stringify({base:BASE,status:failures?'FAIL':'PASS',cases:results},null,2));
 }
 if(failures)process.exitCode=1;
})().catch(error=>{
  console.error('FATAL',error);
  fs.writeFileSync(path.join(OUT,'fatal.txt'),String(error.stack||error));
  process.exitCode=2;
});
