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
    await page.getByTestId("access-user-name").fill("new-staff");
    await page.getByTestId("access-user-role").fill("BAR_MANAGER");
    await expect(page.getByTestId("access-user-template")).toBeVisible();
    await page.getByTestId("access-user-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("创建员工并赋权");
    await expect(page.getByTestId("admin-action-result")).toContainText("new-staff");

    await page.getByTestId("admin-nav-ADM-ACC-002").click();
    await expect(page).toHaveURL(/\/admin\/access\/permission-templates$/);
    await page.getByTestId("permission-template-copy").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("复制模板");
    await page.getByTestId("permission-template-single-use").check();
    await page.getByTestId("permission-template-apply").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("单次有效");
  });

  test("ADM-MVP-03 ticketing policy validates weekday/weekend/holiday base price before publish", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/ticketing/policies");

    await page.getByTestId("ticket-weekday-price").fill("100");
    await page.getByTestId("ticket-weekend-price").fill("120");
    await page.getByTestId("ticket-publish").click();
    await expect(page.getByTestId("ticket-policy-error")).toContainText("节假日");

    await page.getByTestId("ticket-holiday-price").fill("150");
    await expect(page.getByTestId("ticket-derived-preview")).toContainText("新人票");
    await expect(page.getByTestId("ticket-derived-preview")).toContainText("学生票");
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
    await expect(page.getByTestId("bar-transfer-action")).toContainText("需主管审批");
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
    // 导出后端接口未就绪：按 P0 要求去除“假留痕”，按钮禁用并提示，不再伪造导出结果。
    await expect(page.getByTestId("finance-export")).toBeDisabled();
  });

  test("ADM-MVP-06 integration adapter rejects plaintext secret and supports mock test", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/integrations/adapters");

    await page.getByTestId("integration-credential-ref").fill("plaintext-secret");
    await page.getByTestId("integration-save").click();
    await expect(page.getByTestId("integration-error")).toContainText("凭证引用");

    await page.getByTestId("integration-credential-ref").fill("cred://prod/provider/mock");
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
});
