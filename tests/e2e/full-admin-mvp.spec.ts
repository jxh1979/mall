import { expect, test } from "@playwright/test";
import { installMockApi } from "./support/mock-api";

async function adminLogin(page: import("@playwright/test").Page, path = "/admin/overview", username = "admin", password = "admin-pass") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill(username);
  await page.getByTestId("login-password").fill(password);
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("admin-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

async function expectNoMaterialHorizontalOverflow(page: import("@playwright/test").Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(80);
}

test.describe("full admin MVP acceptance", () => {
  test("ADM-MVP-01 login, overview route and eight MVP navigation entries", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/overview");

    await expect(page.getByTestId("admin-page-title")).toContainText("总控驾驶舱");
    await expect(page.getByTestId("admin-overview")).toContainText("票务");
    await expect(page.getByTestId("admin-overview")).toContainText("酒吧");
    await expect(page.getByTestId("admin-overview")).toContainText("财务");

    for (const pageId of ["ADM-OV-001", "ADM-ACC-001", "ADM-ACC-002", "ADM-TKT-001", "ADM-BAR-001", "ADM-FIN-001", "ADM-INT-001", "ADM-OPS-001"]) {
      await expect(page.getByTestId(`admin-nav-${pageId}`)).toBeVisible();
    }

    await page.getByTestId("admin-logout").click();
    await expect(page.getByTestId("login-view")).toBeVisible();
  });

  test("ADM-MVP-02 account and permission template workflows are actionable", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/access/users");

    await expect(page.getByTestId("admin-page-title")).toContainText("员工账号");
    // 引导式流程一期：主按钮打开分步弹窗 → 账号信息 → 归属与授权 → 状态与权限 → 确认页创建。
    await page.getByTestId("access-user-create-open").click();
    await page.getByTestId("access-user-name").fill("new-staff");
    await page.getByTestId("access-user-password").fill("ChangeMe@123");
    await page.getByTestId("access-user-phone").fill("13800000001");
    await page.getByTestId("access-user-account-type").selectOption("sales");
    await page.getByTestId("access-user-flow-next").click();
    await page.getByTestId("access-user-org").fill("org-1");
    await page.getByTestId("access-user-merchant").fill("merchant-1");
    await page.getByTestId("access-user-consent").fill("consent-staff-001");
    await page.getByTestId("access-user-evidence").fill("ev-staff-001,ev-hr-001");
    await page.getByTestId("access-user-flow-next").click();
    await page.getByTestId("access-user-status").selectOption("active");
    await expect(page.getByTestId("access-user-template")).toBeVisible();
    await page.getByTestId("access-user-flow-next").click();
    await page.getByTestId("access-user-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("创建员工并赋权");
    await expect(page.getByTestId("admin-action-result")).toContainText("new-staff");

    await page.getByTestId("admin-nav-ADM-ACC-002").click();
    await expect(page).toHaveURL(/\/admin\/access\/permission-templates$/);
    await page.getByTestId("permission-template-name").fill("普通员工01");
    await page.getByTestId("permission-template-type").selectOption("temporary");
    await page.getByTestId("permission-template-valid-until").fill("2026-07-01T10:00");
    await page.getByTestId("permission-template-single-use").check();
    await page.getByTestId("permission-code-access.permission_template.view").check();
    await page.getByTestId("permission-code-bar.table_session.view").check();
    await page.getByTestId("permission-template-save").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("保存权限模板");
    await page.getByTestId("permission-template-copy").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("复制模板");
    await page.getByTestId("permission-template-target-users").fill("user-1,user-2");
    await page.getByTestId("permission-template-apply").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("单次有效");
  });

  test("ADM-MVP-03 ticketing policy validates weekday/weekend/holiday base price before publish", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/ticketing/policies");

    // 引导式流程 phase-2：新建票种改为分步弹窗（基本信息→定价→确认）。
    await page.getByTestId("ticket-create-open").click();
    // 步1 基本信息：票种名有默认值，直接下一步。
    await page.getByTestId("ticket-policy-next").click();
    // 步2 定价：缺节假日价 → 下一步触发校验报错。
    await page.getByTestId("ticket-weekday-price").fill("100");
    await page.getByTestId("ticket-weekend-price").fill("120");
    await page.getByTestId("ticket-policy-next").click();
    await expect(page.getByTestId("ticket-policy-error")).toContainText("节假日");

    await page.getByTestId("ticket-holiday-price").fill("150");
    await expect(page.getByTestId("ticket-derived-preview")).toContainText("新人票");
    await expect(page.getByTestId("ticket-derived-preview")).toContainText("学生票");
    // 定价合法 → 进确认页 → 发布。
    await page.getByTestId("ticket-policy-next").click();
    await page.getByTestId("ticket-publish").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("published");
  });

  test("ADM-MVP-04 bar table session blocks over-limit check-in and records valid check-in", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/bar/table-sessions");

    await expect(page.getByTestId("admin-page-title")).toContainText("卡座与订台");
    await page.getByTestId("bar-checkin-count").fill("7");
    await page.getByTestId("bar-checkin-submit").click();
    await expect(page.getByTestId("bar-checkin-error")).toContainText("人数超限");

    await page.getByTestId("bar-checkin-count").fill("4");
    await page.getByTestId("bar-checkin-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("checked_in");

    await page.getByTestId("bar-session-select-session-1").check();
    await page.getByTestId("bar-transfer-target-table").fill("table-2");
    await page.getByTestId("bar-transfer-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("transferred");

    await page.getByTestId("bar-merge-target-session").fill("session-2");
    await page.getByTestId("bar-merge-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merged");

    await page.getByTestId("bar-renew-minutes").fill("60");
    await page.getByTestId("bar-renew-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("renewed");
  });

  test("ADM-MVP-05 finance ledger requires adjustment reason and supports partial payment", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/finance/ar-ap-ledger");

    await page.getByTestId("finance-payment-amount").fill("50");
    await page.getByTestId("finance-payment-submit").click();
    await expect(page.getByTestId("finance-ledger-error")).toContainText("原因");

    await page.getByTestId("finance-payment-reason").fill("客户部分付款");
    await page.getByTestId("finance-payment-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("partial");
    // 导出后端已落地（FinanceController.exportArApItems / OpenAPI finance_ar_ap.exported）：点击后真导出并回显 exported 快照，不再是“接口未就绪”占位。
    await page.getByTestId("finance-export").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("exported");
  });

  test("ADM-MVP-06 integration adapter rejects plaintext secret and supports mock test", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/integrations/adapters");

    // 引导式流程 phase-6：新增适配器改分步引导弹窗——先开弹窗，凭证填在步内，「下一步」触发步内校验。
    await page.getByTestId("integration-create-open").click();
    await page.getByTestId("integration-credential-ref").fill("plaintext-secret");
    await page.getByTestId("integration-next").click();
    await expect(page.getByTestId("integration-error")).toContainText("凭证引用");

    await page.getByTestId("integration-credential-ref").fill("cred://prod/provider/mock");
    await page.getByTestId("integration-next").click();
    await page.getByTestId("integration-save").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("provider");
    await page.getByTestId("integration-test-mock").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("mock_passed");
  });

  test("ADM-MVP-07 ops readiness cannot verify blocker without evidence ref", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/ops/readiness");

    await page.getByTestId("ops-verify-blocker").click();
    await expect(page.getByTestId("ops-readiness-error")).toContainText("证据引用");

    await page.getByTestId("ops-evidence-ref").fill("evidence://nas/restore-drill/20260616");
    await page.getByTestId("ops-verify-blocker").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("verified");
  });

  test("ADM-MVP-08 role permission and mobile layout are enforceable", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await installMockApi(page, { user: { username: "bar-op", accountType: "bar_operator" } });
    await adminLogin(page, "/admin/finance/ar-ap-ledger", "bar-op");

    await expect(page.getByTestId("admin-permission-denied")).toContainText("权限不足");
    await expect(page.getByTestId("finance-ledger-table")).toHaveCount(0);
    await expectNoMaterialHorizontalOverflow(page);
  });

  // 承接退役 legacy 控制台的两条鉴权错误呈现验收（P0-UI-09/04/08）：
  // legacy 壳删除后，这两类后端错误的 UI 呈现改在 admin 壳验证。
  // 错误翻译层上线后（fix/web-critical）：用户界面只看中文业务话术，不再直出状态码/接口报文。
  test("ADM-MVP-09 failed login surfaces business wording and stays on auth screen", async ({ page }) => {
    await installMockApi(page, { authFailure: true });
    await page.goto("/admin/overview");
    await page.getByTestId("login-username").fill("admin");
    await page.getByTestId("login-password").fill("wrong-pass");
    await page.getByTestId("login-submit").click();

    await expect(page.getByTestId("login-view")).toBeVisible();
    await expect(page.getByTestId("global-error")).toContainText("账号或密码不正确");
    await expect(page.getByTestId("global-error")).not.toContainText("{");
    await expect(page.getByTestId("admin-shell")).toHaveCount(0);
  });

  test("ADM-MVP-10 forbidden backend access surfaces business wording without hiding the audit trail", async ({ page }) => {
    await installMockApi(page, { forbiddenPath: "/api/v1/analytics/dashboards/p0-overview" });
    await adminLogin(page, "/admin/overview");

    await expect(page.getByTestId("global-error")).toContainText("当前账号无权进行此操作");
  });
});
