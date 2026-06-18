import { expect, test, type APIRequestContext, type Page } from "@playwright/test";

const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? "http://127.0.0.1:8080";
const username = process.env.REAL_BACKEND_USERNAME ?? "admin";
const password = process.env.REAL_BACKEND_PASSWORD ?? "admin-pass";

async function expectRealBackendReady(request: APIRequestContext) {
  const health = await request.get(`${backendBaseUrl}/api/v1/system/health`);
  expect(health.ok(), await health.text()).toBeTruthy();

  const apiLogin = await request.post(`${backendBaseUrl}/api/v1/auth/login`, {
    data: { login_name: username, password },
  });
  test.skip(
    apiLogin.status() === 401 || apiLogin.status() === 403,
    "real backend credentials are not available; set REAL_BACKEND_USERNAME/REAL_BACKEND_PASSWORD or start backend with P0_ADMIN_USERNAME/P0_ADMIN_PASSWORD",
  );
  expect(apiLogin.ok(), await apiLogin.text()).toBeTruthy();
}

async function loginAgainstRealBackend(page: Page, path = "/") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill(username);
  await page.getByTestId("login-password").fill(password);
  await page.getByTestId("login-submit").click();

  await expect(page.getByTestId("app-shell")).toBeVisible();
  await expect(page.locator(".user-chip")).toHaveText(username);
  await expect(page.locator(".loading-state")).toHaveCount(0);
  await expect(page.getByTestId("global-error")).toHaveCount(0);
}

test.describe("real backend UI acceptance", () => {
  test.beforeEach(() => {
    test.skip(process.env.REAL_BACKEND_E2E !== "1", "set REAL_BACKEND_E2E=1 to run real backend UI acceptance");
  });

  test("REAL-UI-01 health, login, dashboard and logout use the real backend", async ({ page, request }) => {
    await expectRealBackendReady(request);
    await loginAgainstRealBackend(page);

    await expect(page.getByText("运营总览").first()).toBeVisible();
    await expect(page.getByText("API 正常").first()).toBeVisible();

    await page.getByTestId("logout-button").click();
    await expect(page.getByTestId("login-view")).toBeVisible();
  });

  test("REAL-UI-02 core business pages load real backend snapshots without proxy fallback", async ({ page, request }) => {
    await expectRealBackendReady(request);

    for (const pageCase of [
      {
        path: "/?page=ticketing",
        title: "票务核销",
        expectedTexts: ["创建票种并出票", "P2 Dynamic Pricing Rules", "票种", "核销记录"],
      },
      {
        path: "/?page=bar",
        title: "酒吧经营",
        expectedTexts: ["一键完成酒吧闭环", "桌台", "订单", "P1 存酒"],
      },
      {
        path: "/?page=amusement",
        title: "游乐运营",
        expectedTexts: ["创建项目和场次", "P2 团体/包场", "游乐项目", "P1 设备状态"],
      },
      {
        path: "/?page=interfaces",
        title: "第三方接口",
        expectedTexts: ["第三方预留接口与开放应用", "接口目录", "供应商适配器", "P1 重试任务"],
      },
    ]) {
      await loginAgainstRealBackend(page, pageCase.path);
      await expect(page.getByText(pageCase.title).first()).toBeVisible();
      for (const text of pageCase.expectedTexts) {
        await expect(page.getByText(text).first()).toBeVisible();
      }
      await expect(page.getByText("同源 API")).toBeVisible();
    }
  });
});