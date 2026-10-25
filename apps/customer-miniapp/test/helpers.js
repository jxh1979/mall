// 文件版本：V0.01
// 小程序单测替身：miniprogram-simulate 加载页面与组件；wx 全局按「方法 路径」路由假响应（响应体用后端真实的蛇形键与统一信封），
// 记录全部请求、存储读写与跳转。每个用例先 freshModules() 清掉小程序源码的模块缓存（会话、请求编号等内存状态不串用例）。
import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { expect, vi } from "vitest";

export const require = createRequire(import.meta.url);
const simulate = require("miniprogram-simulate");

export const APP_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const ROOT = path.join(APP_ROOT, "miniprogram");

export function freshModules() {
  for (const key of Object.keys(require.cache)) if (key.startsWith(ROOT)) delete require.cache[key];
}

export const ok = (data) => ({ status: 200, body: { code: "OK", message: "成功", data } });
export const fail = (status, detailCode, message) => ({ status, body: { code: status === 401 ? "UNAUTHORIZED" : "STATE_CONFLICT", detail_code: detailCode, message } });

/**
 * routes：{"GET /api/v1/customer/orders": ok([...]) | (req) => 响应 | "network-fail"}；未登记的路径按 404。
 * 返回 { wx, requests, store }。
 */
export function installWx(routes = {}, storage = {}) {
  const requests = [];
  const store = { ...storage };
  const wx = {
    request: vi.fn((options) => {
      const fullPath = options.url.replace(/^https:\/\/[^/]+/, "");
      const key = `${options.method} ${fullPath.split("?")[0]}`;
      // 带文件的提交（multipart，请求体是 ArrayBuffer）：form 为按 UTF-8 解出的原文，供断言字段与文件。
      const binary = options.data && typeof options.data === "object" && options.data.byteLength !== undefined;
      const req = { ...options, path: fullPath, key, body: typeof options.data === "string" ? JSON.parse(options.data) : undefined,
        form: binary ? Buffer.from(options.data).toString("utf8") : undefined };
      requests.push(req);
      const route = routes[key];
      const res = typeof route === "function" ? route(req) : route ?? { status: 404, body: { code: "NOT_FOUND", message: "不存在" } };
      Promise.resolve(res).then((r) => {
        if (r === "network-fail") options.fail?.({ errMsg: "request:fail" });
        else if (r !== "pending") options.success?.({ statusCode: r.status, data: JSON.stringify(r.body) });
      });
    }),
    getStorageSync: vi.fn((k) => store[k] ?? ""),
    setStorageSync: vi.fn((k, v) => {
      store[k] = v;
    }),
    removeStorageSync: vi.fn((k) => {
      delete store[k];
    }),
    setStorage: vi.fn(),
    reLaunch: vi.fn(),
    navigateTo: vi.fn(),
    redirectTo: vi.fn(),
    switchTab: vi.fn(),
    navigateBack: vi.fn((o) => o?.fail?.({ errMsg: "navigateBack:fail" })),
    showToast: vi.fn(),
    showModal: vi.fn((o) => {
      o?.success?.({ confirm: true, cancel: false });
      o?.complete?.();
    }),
    requestPayment: vi.fn((o) => {
      o?.success?.({});
      o?.complete?.({});
    }),
    scanCode: vi.fn(),
    // 本地临时文件：读出的内容是「文件:<路径>」，便于断言上传的是哪个文件。
    getFileSystemManager: vi.fn(() => ({ readFileSync: (filePath) => new TextEncoder().encode(`文件:${filePath}`).buffer })),
    chooseMedia: vi.fn((o) => o.success({ tempFiles: [{ tempFilePath: `http://tmp/${o.mediaType[0]}-1.${o.mediaType[0] === "video" ? "mp4" : "jpg"}`, size: 1 }] })),
    getRecorderManager: vi.fn(() => recorder),
  };
  // 录音管理器（全局唯一）：stop() 即回调 onStop 交出录音文件。
  const recorder = {
    onStop: vi.fn((listener) => {
      recorder.listener = listener;
    }),
    onError: vi.fn((listener) => {
      recorder.errorListener = listener;
    }),
    start: vi.fn(),
    stop: vi.fn(() => recorder.listener?.({ tempFilePath: "http://tmp/voice-1.mp3", duration: 3000 })),
  };
  globalThis.wx = wx;
  return { wx, requests, store };
}

/** 加载页面或组件（相对 miniprogram/ 的路径，不带扩展名），挂上并按页面口径调 onLoad、onShow。 */
export function mount(rel, { query = {}, properties } = {}) {
  const id = simulate.load(path.join(ROOT, rel), { compiler: "simulate", rootPath: ROOT });
  const comp = simulate.render(id, properties);
  comp.attach(document.createElement("parent-wrapper"));
  comp.instance.onLoad?.(query);
  comp.instance.onShow?.();
  return comp;
}

export async function flush(times = 6) {
  for (let i = 0; i < times; i += 1) await new Promise((resolve) => setTimeout(resolve, 0));
}

/** 逐层进入自定义组件查节点：q(comp, "#payment", "#pay-online")。 */
export function q(comp, ...selectors) {
  let node = comp;
  for (const selector of selectors) {
    node = node?.querySelector(selector);
  }
  return node ?? null;
}

/** 先断言节点在，再派发事件（第一步就是断言：缺节点报断言失败，不是空指针）。 */
export function fire(comp, selectors, event, detail) {
  const node = q(comp, ...selectors);
  expect(node, `找不到 ${selectors.join(" ")}`).not.toBeNull();
  node.dispatchEvent(event, detail === undefined ? undefined : { detail });
}

export const text = (node) => node?.dom?.textContent ?? "";

/** 请求头取值（键一律小写发出）。 */
export const header = (req, name) => req.header?.[name];
