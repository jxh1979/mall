import { expect, test, type Page } from "@playwright/test";
import { installCustomerMockApi } from "./support/customer-mock-api";

// 客人端两端一致性验收：购票 / 订台 / 积分 / 钱包真实流程跑通 UI -> mock 后端 -> UI 回显。
// 与 web-ui-real-backend.spec.ts（真实后端，默认 skip）互补：本 spec 用 mock 后端保证可在
// 无网/无后端环境下持续验收客旅闭环；真实后端 E2E 基建（REAL_BACKEND_E2E）仍是上层手段。

async function customerLogin(page: Page, path = "/customer") {
  await page.goto(path);
  await expect(page.getByTestId("customer-login-view")).toBeVisible();
  await page.getByTestId("login-phone").fill("13800138888");
  await page.getByTestId("login-sms").fill("123456");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("customer-app")).toBeVisible();
  await expect(page.getByTestId("customer-phone")).toContainText("138****8888");
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

    await expect(page.getByText("首页 / 票种").first()).toBeVisible();
    await page.getByTestId("customer-logout").click();
    await expect(page.getByTestId("customer-login-view")).toBeVisible();
  });

  test("CUST-02 buy ticket creates order and aggregate payment intent", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await page.getByRole("menuitem", { name: "购票" }).click();
    await expect(page.getByTestId("buy-ticket-page")).toBeVisible();

    await selectAntdOption(page, "buy-ticket-type", "周末通票");
    await page.getByTestId("buy-ticket-submit").click();

    const result = page.getByTestId("buy-ticket-result");
    await expect(result).toBeVisible();
    await expect(result).toContainText("订单已创建");
    await expect(result).toContainText("TO-CUST-1");
    await expect(result).toContainText("已发起聚合支付");
  });

  test("CUST-03 bar reservation creates reservation and deposit intent", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await page.getByRole("menuitem", { name: "订台" }).click();
    await expect(page.getByTestId("bar-reservation-page")).toBeVisible();

    await selectAntdOption(page, "reservation-table", /A01 卡座/);
    // 到店时段：在 DatePicker(showTime) 输入框直接键入日期时间并回车确认，规避面板动画不稳。
    const timeInput = page.getByTestId("bar-reservation-page").locator(".ant-picker input");
    await timeInput.click();
    await timeInput.fill("2026-06-20 19:30:00");
    await timeInput.press("Enter");

    await page.getByTestId("reservation-submit").click();

    const result = page.getByTestId("reservation-result");
    await expect(result).toBeVisible();
    await expect(result).toContainText("订台已创建");
    await expect(result).toContainText("定金");
    await expect(result).toContainText("已发起定金支付意图");
  });

  test("CUST-04 points page shows balance/level and check-in accrues streak", async ({ page }) => {
    await installCustomerMockApi(page);
    await customerLogin(page);

    await page.getByRole("menuitem", { name: "积分 / 签到" }).click();
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

    await page.getByRole("menuitem", { name: "储值 / 会员" }).click();
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
    await page.getByRole("menuitem", { name: "购票" }).click();
    await selectAntdOption(page, "buy-ticket-type", "周末通票");
    await page.getByTestId("buy-ticket-submit").click();
    await expect(page.getByTestId("buy-ticket-result")).toContainText("TO-CUST-1");

    await page.getByRole("menuitem", { name: "我的订单 / 票 / 二维码" }).click();
    await expect(page.getByTestId("my-orders-page")).toBeVisible();
    await expect(page.getByTestId("order-item").first()).toContainText("TO-CUST-1");

    // 1.8：订单直接展开票实例，点票出码（无需手输票 ID）。
    await page.getByTestId("order-tickets-toggle").first().click();
    await expect(page.getByTestId("ticket-item").first()).toContainText("周末通票");
    await expect(page.getByTestId("ticket-item").first()).toContainText("剩余 1/1 次");
    await page.getByTestId("ticket-qr-open").first().click();
    await expect(page.getByTestId("ticket-qr-view")).toBeVisible();
    await expect(page.getByTestId("qr-code").locator("svg")).toBeVisible();
    // 1.7 口径：二维码可见但凭证明文不落 DOM。
    await expect(page.getByTestId("ticket-qr-view")).toContainText("ent-1");
    await expect(page.locator("body")).not.toContainText("cred-customer-1");
  });

  test("CUST-07 buy ticket surfaces backend rejection as a visible error", async ({ page }) => {
    await installCustomerMockApi(page, { orderFailure: true });
    await customerLogin(page);

    await page.getByRole("menuitem", { name: "购票" }).click();
    await selectAntdOption(page, "buy-ticket-type", "周末通票");
    await page.getByTestId("buy-ticket-submit").click();

    await expect(page.getByTestId("buy-ticket-page")).toContainText("库存不足");
    await expect(page.getByTestId("buy-ticket-result")).toHaveCount(0);
  });
});
