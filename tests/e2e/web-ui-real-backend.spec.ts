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
  // AUD-022: 一旦 REAL_BACKEND_E2E=1（由 beforeEach 保证进入这里的前提），认证失败就是真实失败，
  // 绝不能 test.skip 当成功退出。账号失效/权限错配/端点缺失都必须让这组用例变红。
  expect(
    apiLogin.ok(),
    `REAL_BACKEND_E2E is enabled but real backend login for '${username}' failed with ${apiLogin.status()}: ` +
      `${await apiLogin.text()}. Auth failure is a real failure, not a skip — fix credentials ` +
      `(REAL_BACKEND_USERNAME/REAL_BACKEND_PASSWORD or start backend with P0_ADMIN_USERNAME/P0_ADMIN_PASSWORD).`,
  ).toBeTruthy();
}

// legacy 控制台退役后，真实后端 UI 验收改在 admin 完整后台壳上进行。
async function loginAgainstRealBackend(page: Page, path = "/admin/overview") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill(username);
  await page.getByTestId("login-password").fill(password);
  await page.getByTestId("login-submit").click();

  await expect(page.getByTestId("admin-shell")).toBeVisible();
  await expect(page.locator(".admin-user-chip")).toHaveText(username);
  await expect(page.locator(".loading-state")).toHaveCount(0);
  await expect(page.getByTestId("global-error")).toHaveCount(0);
}

test.describe("real backend UI acceptance", () => {
  test.beforeEach(() => {
    test.skip(process.env.REAL_BACKEND_E2E !== "1", "set REAL_BACKEND_E2E=1 to run real backend UI acceptance");
  });

  test("REAL-UI-01 health, login, admin overview and logout use the real backend", async ({ page, request }) => {
    await expectRealBackendReady(request);
    await loginAgainstRealBackend(page);

    await expect(page.getByTestId("admin-page-title")).toContainText("总控驾驶舱");

    await page.getByTestId("admin-logout").click();
    await expect(page.getByTestId("login-view")).toBeVisible();
  });

  test("REAL-UI-02 admin core business pages load real backend data without global errors", async ({ page, request }) => {
    await expectRealBackendReady(request);

    for (const path of [
      "/admin/ticketing/policies",
      "/admin/bar/table-sessions",
      "/admin/finance/ar-ap-ledger",
      "/admin/integrations/adapters",
    ]) {
      await loginAgainstRealBackend(page, path);
      await expect(page.getByTestId("admin-page-title")).toBeVisible();
      await expect(page.getByTestId("global-error")).toHaveCount(0);
    }
  });

  // P1-12（A8-03 红线补强）：UI 验收矩阵 §5 红线要求"权限拦截必须有服务端拒绝证据，不能只靠前端隐藏"。
  // 此用例以 API 级（不依赖 UI 选择器/账号 seed，最稳）校验：无认证直接访问受保护业务端点，真实后端必须拒绝。
  test("REAL-UI-03 protected business endpoints reject unauthenticated access (server-side denial, not just frontend hiding)", async ({ request }) => {
    await expectRealBackendReady(request);
    for (const path of [
      "/api/v1/finance/ar-ap-items",
      "/api/v1/platform/merchants",
      "/api/v1/access/users",
    ]) {
      const res = await request.get(`${backendBaseUrl}${path}`);
      // AUD-022: 精确断言 401/403。旧断言只要求"非 2xx"，404（端点不存在）/500（服务器错误）
      // 也会被误当成"拒绝证据"通过——那证明不了服务端真的做了鉴权拦截。
      expect(
        [401, 403],
        `${path} must be denied for unauthenticated access with 401/403 (server-side denial), but returned ${res.status()}`,
      ).toContain(res.status());
    }
  });
});
