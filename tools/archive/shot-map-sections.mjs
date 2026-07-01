import { chromium } from '@playwright/test';
const b = await chromium.launch();
const page = await b.newPage({ viewport: { width: 1100, height: 900 }, deviceScaleFactor: 1.5 });
try {
  await page.goto('file:///home/james/complex-management/docs/business-system-map-for-operations-v0.1.html', { waitUntil: 'load', timeout: 30000 });
  for (const id of ['leasing-contract','building-ops']) {
    const el = page.locator('#'+id);
    await el.scrollIntoViewIfNeeded();
    await el.screenshot({ path: `/tmp/map-${id}.png` });
    const h = (await el.boundingBox())?.height;
    console.log(`#${id} 截图 OK, 高=${h&&Math.round(h)}`);
  }
} catch(e){ console.log('FAIL', e.message.slice(0,160)); }
finally { await b.close(); }
