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
  // 此用例以 API 级（不依赖 UI 选择器/账号 seed，最稳）校验：无认证直接访问受保护业务端点，真实后端必须拒绝（非 2xx，期望 401/403），而非返回数据。
  test("REAL-UI-03 protected business endpoints reject unauthenticated access (server-side denial, not just frontend hiding)", async ({ request }) => {
    await expectRealBackendReady(request);
    for (const path of [
      "/api/v1/finance/ar-ap-items",
      "/api/v1/platform/merchants",
      "/api/v1/access/users",
    ]) {
      const res = await request.get(`${backendBaseUrl}${path}`);
      // 服务端拒绝证据：未认证请求绝不能被成功服务（res.ok()=2xx）；预期 401/403。
      expect(res.ok(), `${path} should be denied without auth but returned ${res.status()}`).toBeFalsy();
    }
  });
});
