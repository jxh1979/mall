import { expect, test, type APIRequestContext, type Page } from "@playwright/test";

// V194 客人账号四态生命周期 · 真实后端全链活证据
// 正常 active / 封禁 suspended（到期自动恢复）/ 黑名单 blacklisted（永久）/ 注销 deregistered（数据保留）。
// 全链打真实后端真实库：客人真登录拿 token → 平台按手机号查到账号 → 封禁后客人动作/登录被拒 →
// 恢复后可用 → 拉黑永久拒 → 客人自助注销后被拒但账号仍可被平台查到（数据物理保留）。
// UI 活证据：ADM-CUS-001 客人账号管理页在真实后端上查询并截图。

const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? "http://127.0.0.1:8080";
const username = process.env.REAL_BACKEND_USERNAME ?? "admin";
const password = process.env.REAL_BACKEND_PASSWORD ?? "admin-pass";
const OTP = process.env.REAL_BACKEND_CUSTOMER_OTP ?? "000000";
const PHONE = "13977665544"; // 活证据专用手机号

async function bearer(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${backendBaseUrl}/api/v1/auth/login`, {
    data: { login_name: username, password },
  });
  expect(res.ok(), await res.text()).toBeTruthy();
  const body = await res.json();
  return `Bearer ${body.data.access_token}`;
}

async function customerLogin(request: APIRequestContext): Promise<{ status: number; token?: string; code?: string }> {
  const res = await request.post(`${backendBaseUrl}/api/v1/customer/auth/phone-login`, {
    data: { phone: PHONE, sms_code: OTP, scene: "miniapp" },
  });
  const body = await res.json().catch(() => ({}));
  return {
    status: res.status(),
    token: body?.data?.customer_token,
    code: body?.error?.code ?? body?.code,
  };
}

async function adminPost(request: APIRequestContext, auth: string, path: string, data: object) {
  const res = await request.post(`${backendBaseUrl}${path}`, { headers: { authorization: auth }, data });
  expect(res.ok(), `${path}: ${await res.text()}`).toBeTruthy();
  return (await res.json()).data;
}

test.describe("V194 客人账号四态生命周期 · 真实后端活证据", () => {
  test.beforeEach(() => {
    test.skip(process.env.REAL_BACKEND_E2E !== "1", "set REAL_BACKEND_E2E=1 to run");
  });

  test("CUS-LIFE-01 封禁→拒动作拒登录→恢复→拉黑永久拒→自助注销后数据保留", async ({ request }) => {
    const auth = await bearer(request);

    // 客人真登录（若曾被前轮测试拉黑/注销，先恢复再登录）。
    let login = await customerLogin(request);
    if (login.status === 403) {
      const found = await request.get(
        `${backendBaseUrl}/api/v1/customer-accounts?phone=${PHONE}`, { headers: { authorization: auth } });
      const rows = (await found.json()).data;
      await adminPost(request, auth, `/api/v1/customer-accounts/${rows[0].id}/reinstate`, { reason: "e2e reset" });
      login = await customerLogin(request);
    }
    expect(login.status, "客人登录应成功").toBe(200);
    const customerToken = login.token!;

    // 平台按手机号查到账号（掩码投影，明文不出网）。
    const lookup = await request.get(
      `${backendBaseUrl}/api/v1/customer-accounts?phone=${PHONE}`, { headers: { authorization: auth } });
    expect(lookup.ok(), await lookup.text()).toBeTruthy();
    const account = (await lookup.json()).data[0];
    expect(account, "应查到账号").toBeTruthy();
    expect(account.masked_phone ?? account.maskedPhone).not.toBe(PHONE); // 掩码，非明文
    const customerId = account.id;

    // ① 封禁（未来到期）→ 客人动作被拒 + 再登录被拒。
    const until = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString();
    const suspended = await adminPost(request, auth, `/api/v1/customer-accounts/${customerId}/suspend`,
      { suspendedUntil: until, reason: "e2e 活证据封禁" });
    expect(suspended.status).toBe("suspended");

    const blockedAction = await request.get(`${backendBaseUrl}/api/v1/customer/orders`, {
      headers: { "X-Customer-Token": customerToken },
    });
    expect(blockedAction.status(), "封禁期内动作应 403").toBe(403);
    expect((await blockedAction.json()).error?.code ?? (await blockedAction.json()).code).toBeDefined();

    const blockedLogin = await customerLogin(request);
    expect(blockedLogin.status, "封禁期内登录应 403").toBe(403);

    // ② 恢复 → 登录立即可用。
    await adminPost(request, auth, `/api/v1/customer-accounts/${customerId}/reinstate`, { reason: "e2e 解封" });
    const back = await customerLogin(request);
    expect(back.status, "恢复后登录应成功").toBe(200);

    // ③ 拉黑（永久）→ 登录被拒。
    const blacklisted = await adminPost(request, auth, `/api/v1/customer-accounts/${customerId}/blacklist`,
      { reason: "e2e 活证据拉黑" });
    expect(blacklisted.status).toBe("blacklisted");
    const blackLogin = await customerLogin(request);
    expect(blackLogin.status, "拉黑后登录应 403").toBe(403);

    // ④ 恢复 → 客人自助注销 → 动作被拒，但平台仍能查到账号（历史数据物理保留）。
    await adminPost(request, auth, `/api/v1/customer-accounts/${customerId}/reinstate`, { reason: "e2e 解黑" });
    const again = await customerLogin(request);
    expect(again.status).toBe(200);
    const dereg = await request.post(`${backendBaseUrl}/api/v1/customer/deregister`, {
      headers: { "X-Customer-Token": again.token! },
    });
    expect(dereg.ok(), await dereg.text()).toBeTruthy();
    expect((await dereg.json()).data.status).toBe("deregistered");

    const afterDereg = await request.get(`${backendBaseUrl}/api/v1/customer/orders`, {
      headers: { "X-Customer-Token": again.token! },
    });
    expect(afterDereg.status(), "注销后动作应 403").toBe(403);

    const retained = await request.get(
      `${backendBaseUrl}/api/v1/customer-accounts?phone=${PHONE}`, { headers: { authorization: auth } });
    const retainedRows = (await retained.json()).data;
    expect(retainedRows.length, "注销后账号数据应物理保留可被平台查到").toBe(1);
    expect(retainedRows[0].status).toBe("deregistered");
  });

  test("CUS-LIFE-02 ADM-CUS-001 客人账号管理页在真实后端查询并截图", async ({ page }) => {
    await page.goto("/admin/customers/accounts");
    await page.getByTestId("login-username").fill(username);
    await page.getByTestId("login-password").fill(password);
    await page.getByTestId("login-submit").click();
    await expect(page.getByTestId("admin-shell")).toBeVisible();

    await page.getByLabel("客人手机号").fill(PHONE);
    await page.getByTestId("customer-account-search-button").click();
    await expect(page.getByTestId("customer-account-table")).toBeVisible();
    await expect(page.getByTestId("customer-account-status")).toBeVisible();
    await page.screenshot({ path: "test-results/adm-cus-001-live.png", fullPage: true });
  });
});
