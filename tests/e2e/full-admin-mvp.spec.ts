// 文件版本：V0.05
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
    // 两条路径：员工默认走注册申请（BSM:1463）；外部协作机构账号由管理员开通并赋权（PGM:681）。
    await page.getByTestId("access-user-create-open").click();
    await page.getByTestId("access-user-name").fill("张三");
    await page.getByTestId("access-user-flow-next").click();
    await page.getByTestId("access-user-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("员工注册申请已提交");
    // 提交后弹窗已关闭、姓名已清空：重新打开，先选外部协作账号类型（密码框才渲染），再填账号与初始密码。
    await page.getByTestId("access-user-create-open").click();
    await page.getByTestId("access-user-account-type").selectOption("outsourced_vendor");
    await page.getByTestId("access-user-name").fill("new-staff");
    await page.getByTestId("access-user-password").fill("ChangeMe@123");
    await page.getByTestId("access-user-org").fill("org-1");
    await page.getByTestId("access-user-merchant").selectOption("merchant-1");
    await page.getByTestId("access-user-consent").fill("consent-staff-001");
    await page.getByTestId("access-user-evidence").fill("ev-staff-001,ev-hr-001");
    await page.getByTestId("access-user-status").selectOption("active");
    await expect(page.getByTestId("access-user-template")).toBeVisible();
    await page.getByTestId("access-user-flow-next").click();
    await page.getByTestId("access-user-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("创建员工并赋权");
    await expect(page.getByTestId("admin-action-result")).toContainText("new-staff");

    await page.getByTestId("admin-nav-ADM-ACC-002").click();
    await expect(page).toHaveURL(/\/admin\/access\/permission-templates$/);
    // 引导式流程 phase-11：新建权限模板改为「＋新建权限模板」按钮 → 分步弹窗（模板信息→选权限→确认）。
    await page.getByTestId("permission-template-create-open").click();
    // 步1 模板信息：临时模板须填截止时间，单次有效。
    await page.getByTestId("permission-template-name").fill("普通员工01");
    await page.getByTestId("permission-template-type").selectOption("temporary");
    await page.getByTestId("permission-template-valid-until").fill("2026-07-01T10:00");
    await page.getByTestId("permission-template-single-use").check();
    // 单屏：权限点与模板信息同屏，直接勾选。
    await page.getByTestId("permission-template-new-code-access.permission_template.view").check();
    await page.getByTestId("permission-template-new-code-bar.table_session.view").check();
    await page.getByTestId("permission-template-create-guided-next").click();
    // 确认步提交。
    await expect(page.getByTestId("permission-template-create-guided-summary")).toBeVisible();
    await page.getByTestId("permission-template-save").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("保存权限模板");
    await page.getByTestId("permission-template-copy").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("复制模板");
    // 应用动作按内联「选中编辑」表单的单次有效开关决定 append/single_use；勾上以单次有效方式应用。
    await page.getByTestId("permission-template-edit-single-use").check();
    await page.getByTestId("permission-template-target-users").fill("user-1,user-2");
    await page.getByTestId("permission-template-apply").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("单次有效");
  });

  test("ADM-MVP-03 ticketing policy validates weekday/weekend/holiday base price before publish", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/ticketing/policies");

    // 引导式流程（AUD-015）：基本信息 → 绑权益+发布理由 → 确认页 → 发布。
    await page.getByTestId("ticket-create-open").click();
    // 一屏填：票种名有默认值，缺节假日价 → 下一步触发校验报错。
    await page.getByTestId("ticket-weekday-price").fill("100");
    await page.getByTestId("ticket-weekend-price").fill("120");
    await page.getByTestId("ticket-policy-next").click();
    await expect(page.getByTestId("ticket-policy-error")).toContainText("节假日");

    await page.getByTestId("ticket-holiday-price").fill("150");
    await expect(page.getByTestId("ticket-derived-preview")).toContainText("新人票");
    await expect(page.getByTestId("ticket-derived-preview")).toContainText("学生票");
    // AUD-015：定价合法 → 绑权益步（必选模板+发布理由）→ 确认页 → 发布（create→bind→publish 三段）。
    await page.getByTestId("ticket-policy-next").click();
    await page.getByTestId("ticket-create-template").selectOption("tpl-1");
    await page.getByTestId("ticket-publish-reason").fill("e2e 上架验证");
    await page.getByTestId("ticket-policy-next").click();
    await page.getByTestId("ticket-publish").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("票务规则已发布");
  });

  test("ADM-MVP-04 bar table session transfer and merge", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/bar/table-sessions");

    await expect(page.getByTestId("admin-page-title")).toContainText("卡座与订台");
    await page.getByTestId("bar-session-select-session-1").check();
    await page.getByTestId("bar-transfer-target-table").fill("table-2");
    await page.getByTestId("bar-transfer-target-staff").fill("staff-2");
    await page.getByTestId("bar-transfer-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("状态：已转移");

    await page.getByTestId("bar-merge-target-session").fill("session-2");
    await page.getByTestId("bar-merge-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("状态：已合并");
  });

  test("ADM-MVP-05 finance ledger requires adjustment reason and supports partial payment", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/finance/ar-ap-ledger");

    await page.getByTestId("finance-payment-amount").fill("50");
    await page.getByTestId("finance-payment-submit").click();
    await expect(page.getByTestId("finance-ledger-error")).toContainText("原因");

    await page.getByTestId("finance-payment-reason").fill("客户部分付款");
    await page.getByTestId("finance-payment-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("状态：部分收付");
    // 导出后端已落地（FinanceController.exportArApItems / OpenAPI finance_ar_ap.exported）：点击后真导出并回显 exported 快照，不再是“接口未就绪”占位。
    await page.getByTestId("finance-export").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("状态：已导出");
  });

  test("ADM-MVP-06 integration adapter requires provider code, type and mode and supports mock test", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/integrations/adapters");

    // 引导式流程 phase-6：新增适配器改分步引导弹窗，「下一步」触发步内校验。
    // OBS-14-01：按后端 ProviderAdapterRequest 真实契约填写——供应商编码、类型（闭集）、接入模式均必填；凭证引用不在新增请求里。
    await page.getByTestId("integration-create-open").click();
    await page.getByTestId("integration-next").click();
    await expect(page.getByTestId("integration-error")).toContainText("请填写供应商编码");

    await page.getByTestId("integration-provider-code").fill("prov-park");
    await page.getByTestId("integration-adapter-type").selectOption("parking");
    await page.getByTestId("integration-mode").selectOption("mock");
    await page.getByTestId("integration-next").click();
    await page.getByTestId("integration-save").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("已保存供应商适配器");
    await page.getByTestId("integration-test-mock").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("模拟测试通过");
  });

  test("ADM-MVP-07 ops readiness cannot verify blocker without evidence ref", async ({ page }) => {
    await installMockApi(page);
    await adminLogin(page, "/admin/ops/readiness");

    await page.getByTestId("ops-verify-blocker").click();
    await expect(page.getByTestId("ops-readiness-error")).toContainText("证据引用");

    await page.getByTestId("ops-evidence-ref").fill("evidence://nas/restore-drill/20260616");
    await page.getByTestId("ops-verify-blocker").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("状态：已核验");
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
