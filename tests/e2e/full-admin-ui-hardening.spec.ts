// 文件版本：V0.01
import { test, expect, type Page } from "@playwright/test";
import { installMockApi } from "./support/mock-api";
import { fillCnDate } from "./support/cn-date";

async function adminLogin(page: Page, path = "/admin/overview") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill("admin");
  await page.getByTestId("login-password").fill("admin-pass");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("admin-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

async function openNav(page: Page, pageId: string) {
  await page.getByTestId(`admin-nav-${pageId}`).click();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

test.describe("full admin UI hardening acceptance", () => {
  test("ADM-UI-HARDEN-01 restructured pages expose root + headered scrollable tables", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/access/users");

    await openNav(page, "ADM-ACC-001");
    await expect(page.getByTestId("access-users-page")).toBeVisible();
    await expect(page.getByTestId("access-users-table").locator("thead")).toContainText("账号类型");

    await openNav(page, "ADM-ACC-002");
    await expect(page.getByTestId("permission-templates-page")).toBeVisible();
    await expect(page.getByTestId("permission-templates-table").locator("thead")).toContainText("模板名称");

    await openNav(page, "ADM-BAR-001");
    await expect(page.getByTestId("bar-table-sessions-page")).toBeVisible();
    await expect(page.getByTestId("bar-table-sessions-table").locator("thead")).toContainText("卡座");

    await openNav(page, "ADM-FIN-001");
    await expect(page.getByTestId("finance-ledger-page")).toBeVisible();
    await expect(page.getByTestId("finance-ledger-table").locator("thead")).toContainText("方向");

    await openNav(page, "ADM-INT-001");
    await expect(page.getByTestId("integration-adapters-page")).toBeVisible();
    await expect(page.getByTestId("integration-adapters-table").locator("thead")).toContainText("供应商");

    await openNav(page, "ADM-OPS-001");
    await expect(page.getByTestId("ops-readiness-table").locator("thead")).toContainText("责任人");

    // Restructured tables live in a horizontal-scroll wrapper, so the page itself never overflows.
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(80);
  });

  test("ADM-UI-HARDEN-02 shared empty and error states expose stable testids", async ({ page }) => {
    // 共享整页空态 admin-empty 仅在只读列表页出现：可录入页（员工账号等）按 27ab627 既定设计走
    // empty={false}+表格内联空提示以保留录入表单，不再用会遮挡表单的整页空态。故改用只读审计事件页验收共享空/错态。
    await installMockApi(page, { empty: true });
    await adminLogin(page, "/admin/audit/events");
    await openNav(page, "ADM-EVD-001");
    await expect(page.getByTestId("admin-empty")).toBeVisible();

    await installMockApi(page, { errorPath: "/api/v1/evidence/events" });
    // 会话现持久化到 localStorage（fix/web-critical）：先清会话再刷新，才能回到登录视图重新登录。
    await page.evaluate(() => window.localStorage.clear());
    await page.reload();
    await adminLogin(page, "/admin/audit/events");
    await openNav(page, "ADM-EVD-001");
    await expect(page.getByTestId("admin-error")).toContainText("读取失败");
    await expect(page.getByTestId("admin-retry")).toBeVisible();
  });

  test("ADM-UI-HARDEN-03 overview date-range filter is operable and KPIs are addressable", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/overview");

    await expect(page.getByTestId("overview-kpi-ticket")).toBeVisible();
    await expect(page.getByTestId("overview-kpi-bar")).toBeVisible();

    await fillCnDate(page, page.getByTestId("overview-date-from"), "2026-06-01");
    await fillCnDate(page, page.getByTestId("overview-date-to"), "2026-06-30");
    await page.getByTestId("overview-date-apply").click();

    await expect(page.getByTestId("overview-date-applied")).toContainText("2026-06-01");
    await expect(page.getByTestId("overview-date-applied")).toContainText("2026-06-30");
    // dashboard survives the filtered refetch
    await expect(page.getByTestId("overview-kpi-ticket")).toBeVisible();
  });

  test("ADM-UI-HARDEN-04 money inputs reject non-positive values before any write", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/ticketing/policies");

    await openNav(page, "ADM-TKT-001");
    // 引导式流程：新建票种一屏表单，下一步校验平日价必须 > 0。
    await page.getByTestId("ticket-create-open").click();
    await page.getByTestId("ticket-weekday-price").fill("0");
    await page.getByTestId("ticket-weekend-price").fill("100");
    await page.getByTestId("ticket-holiday-price").fill("120");
    await page.getByTestId("ticket-policy-next").click(); // 定价步校验
    await expect(page.getByTestId("ticket-policy-error")).toContainText("平日价");
    await expect(page.getByTestId("admin-action-result")).toHaveCount(0);
    // 关闭引导弹窗，避免遮罩拦截后续导航点击。
    await page.getByTestId("ticket-policy-cancel").click();

    await openNav(page, "ADM-FIN-001");
    await page.getByTestId("finance-payment-amount").fill("0");
    await page.getByTestId("finance-payment-reason").fill("测试核销");
    await page.getByTestId("finance-payment-submit").click();
    await expect(page.getByTestId("finance-ledger-error")).toContainText("大于 0");
  });

  test("ADM-UI-HARDEN-05 high-volume log pages support client-side search filter", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/risk/alerts");

    // Risk alerts: non-matching keyword hides the table and shows a distinct filter-empty state
    await openNav(page, "ADM-RISK-001");
    await expect(page.getByTestId("risk-alerts-table")).toBeVisible();
    await page.getByTestId("risk-alerts-search").fill("zzz-no-such-row");
    await expect(page.getByTestId("risk-alerts-filter-empty")).toBeVisible();
    await expect(page.getByTestId("risk-alerts-table")).toHaveCount(0);
    await page.getByTestId("risk-alerts-search").fill("staff_behavior");
    await expect(page.getByTestId("risk-alerts-table")).toContainText("Mock risk");

    // Integration events: same filter contract
    await openNav(page, "ADM-INT-002");
    await expect(page.getByTestId("integration-events-table")).toBeVisible();
    await page.getByTestId("integration-events-search").fill("zzz-no-such-row");
    await expect(page.getByTestId("integration-events-filter-empty")).toBeVisible();
    await page.getByTestId("integration-events-search").fill("mock");
    await expect(page.getByTestId("integration-events-table")).toBeVisible();

    // Ticket orders: filter narrows to the matching order
    await openNav(page, "ADM-TKT-003");
    await expect(page.getByTestId("ticket-orders-table")).toBeVisible();
    await page.getByTestId("ticket-orders-search").fill("zzz-no-such-row");
    await expect(page.getByTestId("ticket-orders-filter-empty")).toBeVisible();
    await page.getByTestId("ticket-orders-search").fill("TO-MOCK");
    await expect(page.getByTestId("ticket-orders-table")).toContainText("TO-MOCK-1");
  });

  test("ADM-UI-HARDEN-06 log pages paginate filtered rows and reset on search", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/risk/alerts");

    // Risk alerts: 12 rows / page size 10 → two pages; named row stays on page 1
    await openNav(page, "ADM-RISK-001");
    await expect(page.getByTestId("risk-alerts-pager-status")).toContainText("第 1 / 2 页");
    await expect(page.getByTestId("risk-alerts-pager-status")).toContainText("共 12 条");
    await expect(page.getByTestId("risk-alerts-table")).toContainText("Mock risk");
    await expect(page.getByTestId("risk-alerts-pager-prev")).toBeDisabled();
    await page.getByTestId("risk-alerts-pager-next").click();
    await expect(page.getByTestId("risk-alerts-pager-status")).toContainText("第 2 / 2 页");
    await expect(page.getByTestId("risk-alerts-pager-next")).toBeDisabled();
    await expect(page.getByTestId("risk-alerts-table")).toContainText("Filler risk");
    await page.getByTestId("risk-alerts-pager-prev").click();
    await expect(page.getByTestId("risk-alerts-pager-status")).toContainText("第 1 / 2 页");

    // Searching narrows to a single page and resets pagination
    await page.getByTestId("risk-alerts-search").fill("Mock risk");
    await expect(page.getByTestId("risk-alerts-pager-status")).toContainText("第 1 / 1 页");
    await expect(page.getByTestId("risk-alerts-pager-status")).toContainText("共 1 条");

    // Integration events: the four named source rows must all remain on page 1
    await openNav(page, "ADM-INT-002");
    await expect(page.getByTestId("integration-events-pager-status")).toContainText("第 1 / 2 页");
    const intTable = page.getByTestId("integration-events-table");
    await expect(intTable).toContainText("RT-1");
    await expect(intTable).toContainText("CP-1");
    await expect(intTable).toContainText("SYNC-1");
    await expect(intTable).toContainText("VCH-1");

    // Ticket orders: named order on page 1, fillers on page 2
    await openNav(page, "ADM-TKT-003");
    await expect(page.getByTestId("ticket-orders-pager-status")).toContainText("共 12 条");
    await expect(page.getByTestId("ticket-orders-table")).toContainText("TO-MOCK-1");
    await page.getByTestId("ticket-orders-pager-next").click();
    await expect(page.getByTestId("ticket-orders-table")).toContainText("TO-FILL");

    // Audit events: 12 events, named trace stays on page 1
    await openNav(page, "ADM-EVD-001");
    await expect(page.getByTestId("audit-events-pager-status")).toContainText("共 12 条");
    await expect(page.getByTestId("audit-events-table")).toContainText("event-trace-1");
  });

  test("ADM-UI-HARDEN-07 organization merchant create exposes editable fields with phone validation", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/governance/organizations");
    await openNav(page, "ADM-GOV-002");

    // 引导式流程一期：主按钮打开分步弹窗；非法手机号在「联系人」步被拦截，不发任何写请求。
    await page.getByTestId("organization-merchant-create-open").click();
    await page.getByTestId("organization-merchant-type").selectOption("retail");
    await page.getByTestId("organization-merchant-contact-name").fill("王经理");
    await page.getByTestId("organization-merchant-contact-phone").fill("12345");
    await page.getByTestId("organization-merchant-flow-next").click();
    await expect(page.getByTestId("organization-merchant-flow-error")).toContainText("手机号");
    await expect(page.getByTestId("admin-action-result")).toHaveCount(0);

    // Valid input flows the matrix key_fields (type/contact) into the create request
    await page.getByTestId("organization-merchant-contact-phone").fill("13912345678");
    await page.getByTestId("organization-merchant-flow-next").click();
    await page.getByTestId("organization-merchant-create").click();
    const result = page.getByTestId("admin-action-result");
    await expect(result).toContainText("商户档案已创建");
    await expect(result).toContainText("零售");
    await expect(result).toContainText("王经理");
    await expect(result).toContainText("13912345678");
  });
});
