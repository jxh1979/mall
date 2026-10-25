// 文件版本：V0.01
import { expect, test, type Page } from "@playwright/test";
import { installCustomerMockApi } from "./support/customer-mock-api";

// 客人端两端一致性验收：购票 / 订台 / 积分 / 钱包真实流程跑通 UI -> mock 后端 -> UI 回显。
// 跑在客人专用构建 apps/customer-app 上（PGM「Web 前端（业务人员，客人无网页端）」：apps/web 已不含客人壳）。
// 与 web-ui-real-backend.spec.ts（真实后端，默认 skip）互补：本 spec 用 mock 后端保证可在无网/无后端环境下持续验收客旅闭环。

async function customerLogin(page: Page) {
  await page.goto("/");
  await expect(page.getByTestId("customer-login-view")).toBeVisible();
  await page.getByTestId("login-phone").fill("13800138888");
  await page.getByTestId("login-sms").fill("123456");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("customer-app")).toBeVisible();
}

// 移动布局：底部标签进入，再点二级页入口。
async function openPage(page: Page, tab: "home" | "tickets" | "bar" | "me", entry: string) {
  await page.getByTestId(`customer-tab-${tab}`).click();
  await page.getByTestId(`customer-entry-${entry}`).click();
}

async function selectAntdOption(page: Page, triggerTestId: string, optionText: string | RegExp) {
  // antd Select：点击触发器打开下拉，等待下拉浮层可见后再点选项内容，规避动画期不稳定。
  await page.getByTestId(triggerTestId).locator(".ant-select-selector").click();
  const option = page.locator(".ant-select-dropdown:visible .ant-select-item-option").filter({ hasText: optionText }).first();
  await option.waitFor({ state: "visible" });
  await option.click();
}

test.describe("customer journey acceptance (mock backend)", () => {
  test("CUST-01 phone login then logout returns to login view", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await expect(page.locator(".customer-page-title")).toHaveText("首页");
    await page.getByTestId("customer-tab-me").click();
    await expect(page.getByTestId("customer-phone")).toContainText("138****8888");
    await page.getByTestId("customer-logout").click();
    await expect(page.getByTestId("customer-login-view")).toBeVisible();
  });

  // BSM「通过官方 App/小程序购票、预定、确认订单、查看支付结果」；OpenAPI：支付渠道未配置时客户端显示「支付暂不可用，未扣款」。
  test("CUST-02 buy ticket creates order and shows payment unavailable when no gateway is configured", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await openPage(page, "home", "buy");
    await expect(page.getByTestId("buy-ticket-page")).toBeVisible();

    await selectAntdOption(page, "buy-ticket-type", "周末通票");
    await page.getByTestId("buy-ticket-submit").click();

    const result = page.getByTestId("buy-ticket-result");
    await expect(result).toBeVisible();
    await expect(result).toContainText("TO-CUST-1");
    await expect(result.getByTestId("order-payment-remaining")).toContainText("还需支付 ¥99.00");
    await result.getByTestId("order-pay-online").click();
    await expect(result.getByTestId("order-payment-unavailable")).toContainText("支付暂不可用，未扣款");
    await expect(result).not.toContainText("支付成功");
  });

  // BSM「订台页面须写明该卡座适用的最低消费金额与服务费比例」「客人自助订台必须支付定金……每一笔订台都必须填写最晚到店时间」：
  // 下单后付定金走本人订台定金在线付款；支付渠道未配置时显示「支付暂不可用，未扣款」，不显示定金已付。
  test("CUST-03 bar reservation shows table rules then deposit payment is unavailable", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await openPage(page, "bar", "reserve");
    await expect(page.getByTestId("bar-reservation-page")).toBeVisible();
    await expect(page.getByTestId("reservation-option-table-1")).toContainText("最低消费 ¥1000.00");
    await expect(page.getByTestId("reservation-option-table-1")).toContainText("服务费 10%");

    await page.getByTestId("reservation-pick-table-1").click();
    await page.getByLabel("到店时间", { exact: true }).fill("2026-06-20T19:30");
    await page.getByLabel("最晚到店时间", { exact: true }).fill("2026-06-20T20:30");
    await page.getByTestId("reservation-submit").click();

    const result = page.getByTestId("reservation-result");
    await expect(result).toContainText("待支付定金 ¥200.00");
    await result.getByTestId("deposit-pay").click();
    await expect(result.getByTestId("deposit-unavailable")).toContainText("支付暂不可用，未扣款");
    await expect(result).not.toContainText("定金已支付");
  });

  test("CUST-04 points page shows balance/level and check-in accrues streak", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await openPage(page, "me", "points");
    await expect(page.getByTestId("points-page")).toBeVisible();
    // antd Statistic 千分位渲染为 1,280
    await expect(page.getByText(/1[,，]?280/)).toBeVisible();
    await expect(page.getByText("黄金会员")).toBeVisible();

    await page.getByTestId("check-in-button").click();
    // 签到成功后 reload 拉回签到记录列表，记录出现即证明两端一致（toast 短暂不稳，改断言列表）。
    await expect(page.getByTestId("check-in-record").first()).toContainText("连续 1 天");
    await expect(page.getByTestId("check-in-record").first()).toContainText("+10 积分");
  });

  test("CUST-05 wallet shows per-card stored value balances", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await openPage(page, "me", "wallet");
    await expect(page.getByTestId("wallet-page")).toBeVisible();
    // ⑧ 储值钱包（AUD-061）多卡模型：分卡余额（通用卡 + 商户卡）。
    const firstCard = page.getByTestId("wallet-card-item").first();
    await expect(firstCard).toContainText("综合体通用卡");
    await expect(firstCard).toContainText("588.00");
  });

  test("CUST-06 my orders reflect a freshly created ticket order", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    // 先购票，再到「我的订单」核对两端一致（订单进入列表）。
    await openPage(page, "home", "buy");
    await selectAntdOption(page, "buy-ticket-type", "周末通票");
    await page.getByTestId("buy-ticket-submit").click();
    await expect(page.getByTestId("buy-ticket-result")).toContainText("TO-CUST-1");

    await openPage(page, "tickets", "orders");
    await expect(page.getByTestId("my-orders-page")).toBeVisible();
    await expect(page.getByTestId("order-item").first()).toContainText("TO-CUST-1");

    // 我的票＝本人当前持有的票，点票出码（无需手输票 ID）。
    await expect(page.getByTestId("ticket-item").first()).toContainText("周末通票");
    await expect(page.getByTestId("ticket-item").first()).toContainText("剩余 1/1 次");
    await page.getByTestId("ticket-qr-open").first().click();
    await expect(page.getByTestId("ticket-qr-view")).toBeVisible();
    await expect(page.getByTestId("qr-code").locator("svg")).toBeVisible();
    // 1.7 口径：二维码可见但凭证明文不落 DOM。
    await expect(page.locator("body")).not.toContainText("cred-customer-1");
  });

  test("CUST-07 buy ticket surfaces backend rejection as a visible error", async ({ page }) => {
    await installCustomerMockApi(page, { orderFailure: true });
    await customerLogin(page);

    await openPage(page, "home", "buy");
    await selectAntdOption(page, "buy-ticket-type", "周末通票");
    await page.getByTestId("buy-ticket-submit").click();

    await expect(page.getByTestId("buy-ticket-page")).toContainText("库存不足");
    await expect(page.getByTestId("buy-ticket-result")).toHaveCount(0);
  });
});
