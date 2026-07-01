import { chromium } from '@playwright/test';
const BASE = process.env.BASE || 'http://127.0.0.1:5174';
const b = await chromium.launch();
const page = await b.newPage({ viewport: { width: 1280, height: 1000 } });
try {
  await page.goto(`${BASE}/admin/contracts`, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.getByTestId('login-username').fill('admin');
  await page.getByTestId('login-password').fill('admin-pass');
  await page.getByTestId('login-submit').click();
  await page.getByTestId('admin-shell').waitFor({ state: 'visible', timeout: 30000 });
  const wb = page.getByTestId('contract-workbench-page');
  await wb.waitFor({ state: 'visible', timeout: 20000 }).catch(async () => {
    await page.getByTestId('admin-nav-ADM-CONT-001').click({ timeout: 8000 });
    await wb.waitFor({ state: 'visible', timeout: 20000 });
  });
  await page.waitForTimeout(900);
  const box = await wb.boundingBox();
  console.log('workbench box h=', box && Math.round(box.height));
  await wb.screenshot({ path: '/tmp/contract-full.png' });   // 元素截图=含溢出全高
  console.log('FULL SHOT OK');
} catch(e){ console.log('FAIL', e.message.slice(0,160)); }
finally{ await b.close(); }
