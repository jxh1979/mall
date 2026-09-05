// 版本：V0.01
import { defineConfig, devices } from "@playwright/test";

if (process.env.REAL_BACKEND_E2E !== "1") {
  throw new Error("真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。");
}

const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? "http://127.0.0.1:8080";
const webPort = Number(process.env.REAL_BACKEND_WEB_PORT ?? "5174");
if (!Number.isInteger(webPort) || webPort < 1 || webPort > 65535) {
  throw new Error("REAL_BACKEND_WEB_PORT 必须是 1 至 65535 的整数端口。");
}
const webBaseUrl = `http://127.0.0.1:${webPort}`;

export default defineConfig({
  testDir: "./tests/e2e",
  testMatch: ["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"],
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  // json reporter 让 run-real-backend-ui-e2e.mjs 能在 Playwright 退出后核对
  // passed/skipped/failed 计数，堵住"全部 skip 仍退出 0"的假绿（AUD-022）。
  reporter: [
    ["list"],
    ["html", { outputFolder: "playwright-report-real-backend", open: "never" }],
    ["json", { outputFile: "test-results/real-backend-results.json" }],
  ],
  use: {
    baseURL: webBaseUrl,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command: `npm --workspace apps/web run dev -- --host 127.0.0.1 --port ${webPort} --strictPort`,
    url: webBaseUrl,
    reuseExistingServer: false,
    timeout: 120_000,
    env: {
      ...process.env,
      VITE_API_PROXY_TARGET: backendBaseUrl,
    },
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],
});
