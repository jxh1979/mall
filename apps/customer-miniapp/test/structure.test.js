// 文件版本：V0.01
// 小程序工程结构检查（160 没有微信开发者工具，只能检查源码）：页面与组件四件套齐全；不用 web-view、不跳别的小程序；
// 只有 lib/api.js 发网络请求、读写存储与读本地文件（上传附件）；app.json 页面清单齐全、每页标题全中文；界面文字全中文（PGM「界面全中文（所有终端零英文）」）；客人包不含后台、经销商门户与员工侧收款代码。
import fs from "node:fs";
import path from "node:path";
import { expect, test } from "vitest";

import { ROOT } from "./helpers.js";

const GENERATED = [path.join(ROOT, "core"), path.join(ROOT, "vendor")];

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else out.push(full);
  }
  return out;
}

const sourceFiles = walk(ROOT).filter((file) => !GENERATED.some((dir) => file.startsWith(dir)));
const read = (file) => fs.readFileSync(file, "utf8");
const appJson = JSON.parse(read(path.join(ROOT, "app.json")));

// 客人端页面清单（BSM「客人端包括 App 和小程序，两者同步建设」，与官方 App 同一套页面）：四个标签页、二级页、客人链接入口与暂停服务页。
const REQUIRED_PAGES = [
  "pages/home/index", "pages/orders/index", "pages/bar/index", "pages/me/index",
  "pages/login/index", "pages/buy/index", "pages/transfer/index", "pages/parking/index",
  "pages/reserve/index", "pages/reservations/index", "pages/invitation/index", "pages/scan/index",
  "pages/points/index", "pages/wallet/index", "pages/vouchers/index", "pages/bottles/index",
  "pages/complaints/index", "pages/student/index", "pages/account/index", "pages/deregister/index",
  "pages/link/index", "pages/paused/index",
];

function unitsWithComponents() {
  const units = new Set(appJson.pages.map((page) => path.join(ROOT, page)));
  for (const unit of [...units]) {
    const json = JSON.parse(read(`${unit}.json`));
    for (const ref of Object.values(json.usingComponents ?? {})) units.add(path.resolve(path.dirname(unit), ref));
  }
  return [...units];
}

test("everyRequiredPageRegisteredWithChineseTitle", () => {
  expect([...appJson.pages].sort()).toEqual([...REQUIRED_PAGES].sort());
  for (const page of appJson.pages) {
    const unit = path.join(ROOT, page);
    expect(fs.existsSync(`${unit}.json`), `${page}.json`).toBe(true);
    // 每页都有自己的标题，且标题里没有英文字母。
    expect(JSON.parse(read(`${unit}.json`)).navigationBarTitleText, page).toMatch(/^[^A-Za-z]+$/);
  }
});

test("everyPageAndComponentHasFourFiles", () => {
  const units = unitsWithComponents();
  expect(units.length).toBeGreaterThanOrEqual(appJson.pages.length + 2);
  for (const unit of units) {
    for (const ext of [".js", ".json", ".wxml", ".wxss"]) expect(fs.existsSync(`${unit}${ext}`), `${unit}${ext}`).toBe(true);
  }
  // 页面一律用 Component() 构造：微信要求其 json 含 usingComponents 定义段；组件 json 须声明 component。
  for (const page of appJson.pages) {
    const unit = path.join(ROOT, page);
    expect(read(`${unit}.js`), page).toContain("Component({");
    expect(JSON.parse(read(`${unit}.json`)).usingComponents, page).toBeDefined();
  }
  for (const unit of units.filter((u) => u.includes(`${path.sep}components${path.sep}`))) {
    expect(JSON.parse(read(`${unit}.json`)).component, unit).toBe(true);
  }
});

test("noWebViewAndNoJumpToOtherMiniProgram", () => {
  for (const file of sourceFiles) {
    const content = read(file);
    expect(content.includes("<web-view"), file).toBe(false);
    expect(content.includes("navigateToMiniProgram"), file).toBe(false);
  }
});

test("networkAndStorageOnlyInLibApi", () => {
  const api = path.join(ROOT, "lib", "api.js");
  for (const file of sourceFiles.filter((f) => f.endsWith(".js") && f !== api)) {
    expect(/wx\.(request|uploadFile|downloadFile|connectSocket|getFileSystemManager|\w*Storage\w*)\b/.test(read(file)), file).toBe(false);
  }
  expect(read(api)).toContain("wx.request(");
});

test("uiTextIsChinese", () => {
  const titles = [appJson.window.navigationBarTitleText, ...appJson.tabBar.list.map((item) => item.text)];
  for (const unit of unitsWithComponents()) titles.push(JSON.parse(read(`${unit}.json`)).navigationBarTitleText ?? "");
  for (const title of titles) expect(/[A-Za-z]/.test(title), title).toBe(false);
  expect(appJson.tabBar.list.map((item) => item.text)).toEqual(["首页", "票与订单", "酒吧", "我的"]);
  for (const file of sourceFiles.filter((f) => f.endsWith(".wxml"))) {
    const visible = read(file).replace(/<!--[\s\S]*?-->/g, "").replace(/\{\{[\s\S]*?\}\}/g, "").replace(/<[^>]*>/g, "");
    expect(/[A-Za-z]/.test(visible), `${file}: ${visible.match(/[^\s]*[A-Za-z][^\s]*/)?.[0]}`).toBe(false);
  }
});

test("bundleHasNoStaffAdminOrDealerCode", () => {
  for (const file of walk(ROOT)) {
    const content = read(file);
    for (const marker of ["AdminApp", "DealerPortalApp", "admin-shell", "dealer-portal", "/api/v1/auth/login", "/api/v1/finance/payment-intents"]) {
      expect(content.includes(marker), `${file} 含 ${marker}`).toBe(false);
    }
  }
});
