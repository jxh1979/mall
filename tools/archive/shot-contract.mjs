import { chromium } from '@playwright/test';
const BASE = process.env.BASE || 'http://127.0.0.1:5174';
const browser = await chromium.launch();
let page;
try {
  page = await browser.newPage({ viewport: { width: 1480, height: 1040 } });
  page.on('console', m => { if (m.type()==='error') console.log('  [console.error]', m.text().slice(0,120)); });
  await page.goto(`${BASE}/admin/contracts`, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.getByTestId('login-view').waitFor({ state: 'visible', timeout: 30000 });
  await page.getByTestId('login-username').fill('admin');
  await page.getByTestId('login-password').fill('admin-pass');
  await page.getByTestId('login-submit').click();
  await page.getByTestId('admin-shell').waitFor({ state: 'visible', timeout: 30000 });
  console.log('admin-shell OK');
  // 不硬 goto；先看 deep-link 是否还原，否则 SPA 内导航
  try {
    await page.getByTestId('contract-workbench-page').waitFor({ state: 'visible', timeout: 7000 });
    console.log('deep-link restored');
  } catch {
    console.log('nav via ADM-CONT-001');
    await page.getByTestId('admin-nav-ADM-CONT-001').click({ timeout: 8000 });
    await page.getByTestId('contract-workbench-page').waitFor({ state: 'visible', timeout: 20000 });
  }
  await page.waitForTimeout(1000);
  await page.screenshot({ path: '/tmp/contract-workbench.png', fullPage: true });
  const title = await page.getByTestId('admin-page-title').innerText().catch(() => '?');
  const rows = await page.getByTestId('contract-flow-table').innerText().catch(() => '?');
  console.log('SHOT OK | title=', title, '| 审批链表=', rows.replace(/\n/g,' ').slice(0,80));
} catch (e) {
  console.log('SHOT FAIL', e.message.slice(0,140));
  if (page) await page.screenshot({ path: '/tmp/contract-workbench.png', fullPage: true }).catch(() => {});
} finally { await browser.close(); }
