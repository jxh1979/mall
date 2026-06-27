import { chromium } from '@playwright/test';
const BASE = process.env.BASE || 'http://127.0.0.1:5174';
const pages = [
  { id:'ADM-FAC-001', route:'/admin/facility/workbench', name:'facility' },
  { id:'ADM-ENE-001', route:'/admin/energy/workbench', name:'energy' },
  { id:'ADM-ACC-003', route:'/admin/access/governance', name:'governance' },
];
const b = await chromium.launch();
for (const p of pages) {
  const page = await b.newPage({ viewport:{ width:1280, height:1040 }, deviceScaleFactor:1.25 });
  try {
    await page.goto(`${BASE}${p.route}`, { waitUntil:'domcontentloaded', timeout:60000 });
    await page.getByTestId('login-username').fill('admin');
    await page.getByTestId('login-password').fill('admin-pass');
    await page.getByTestId('login-submit').click();
    await page.getByTestId('admin-shell').waitFor({ state:'visible', timeout:30000 });
    await page.waitForTimeout(1800);
    const title = await page.getByTestId('admin-page-title').innerText().catch(()=>'(无title)');
    const err = await page.locator('text=/读取失败|未授权|403|Not Found|无权限/').count().catch(()=>0);
    console.log(`${p.name} ${p.id}: title="${title}" 错误标记=${err}`);
    await page.screenshot({ path:`/tmp/ui-${p.name}.png`, fullPage:true });
  } catch(e){ console.log(`${p.name} FAIL ${e.message.slice(0,110)}`); await page.screenshot({path:`/tmp/ui-${p.name}.png`}).catch(()=>{}); }
  finally { await page.close(); }
}
await b.close();
