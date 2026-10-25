// 版本：V0.02
import { defineConfig, devices } from "@playwright/test";

// 后台与经销商门户在 apps/web（5173）上验收；客人端在客人专用构建 apps/customer-app（5174）上验收
// （PGM「Web 前端（业务人员，客人无网页端）」：apps/web 已不含客人壳）。
const ignoredEverywhere = ["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"];

export default defineConfig({
  testDir: "./tests/e2e",
  testIgnore: ignoredEverywhere,
  globalSetup: "./tests/e2e/support/global-setup.ts",
  timeout: 30_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  reporter: [["list"], ["html", { outputFolder: "playwright-report", open: "never" }]],
  use: {
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command: "npm --workspace apps/web run dev -- --host 127.0.0.1",
      url: "http://127.0.0.1:5173",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      command: "npm --workspace apps/customer-app run dev -- --host 127.0.0.1 --strictPort",
      url: "http://127.0.0.1:5174",
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
  projects: [
    {
      name: "chromium",
      testIgnore: [...ignoredEverywhere, "customer-journey.spec.ts"],
      use: { ...devices["Desktop Chrome"], baseURL: "http://127.0.0.1:5173" },
    },
    {
      name: "customer-app",
      testMatch: ["customer-journey.spec.ts"],
      use: { ...devices["Pixel 7"], baseURL: "http://127.0.0.1:5174" },
    },
  ],
});
