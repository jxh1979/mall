import { expect, test } from "@playwright/test";
import { installMockApi } from "./support/mock-api";

async function login(page: import("@playwright/test").Page, path = "/", username = "admin", password = "admin-pass") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill(username);
  await page.getByTestId("login-password").fill(password);
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("app-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

async function expectNoMaterialHorizontalOverflow(page: import("@playwright/test").Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(120);
}

test.describe("web admin UI acceptance", () => {
  test("P0-UI-01 login, query dashboard, visual checkpoint, logout", async ({ page }) => {
    await installMockApi(page);
    await login(page);

    await expect(page.getByText("\u8fd0\u8425\u603b\u89c8").first()).toBeVisible();
    await expect(page.getByText("Weekend Pass")).toBeVisible();
    await expect(page).toHaveScreenshot("dashboard-shell.png", {
      fullPage: true,
      animations: "disabled",
      mask: [page.locator(".page-title-row small"), page.locator(".section-extra small")],
    });

    await page.getByTestId("logout-button").click();
    await expect(page.getByTestId("login-view")).toBeVisible();
  });

  test("P0-UI-02 form validation blocks empty login", async ({ page }) => {
    await installMockApi(page);
    await page.goto("/");
    await page.getByTestId("login-submit").click();
    await expect(page.locator(".ant-form-item-explain-error")).toHaveCount(2);
  });

  test("P0-UI-03 ticket issuing and P2 dynamic pricing container have real assertions", async ({ page }) => {
    await installMockApi(page);
    await login(page, "/?page=ticketing");

    await page.getByTestId("ticket-flow-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("TICKET-1000");
    await expect(page.getByTestId("action-result")).toContainText("ENT-");

    await page.getByTestId("dynamic-pricing-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("Weekend Boost");

    await page.locator('[data-testid^="publish-dynamic-pricing-"]').first().click();
    await page.getByTestId("confirm-high-impact-ok").click();
    await expect(page.getByTestId("action-result")).toContainText("published");

    await page.getByTestId("dynamic-pricing-simulation-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("finalPriceCent");

    await page.getByTestId("dynamic-pricing-order-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("dynamicPricingRuleId");
    await expect(page.getByTestId("action-result")).toContainText("dynamicAdjustmentCent");
    await expect(page.getByTestId("action-result")).toContainText("10890");
  });

  test("P1-UI-01 provider adapter creation and group booking release cover integration and cancel-like flows", async ({ page }) => {
    await installMockApi(page);
    await login(page, "/?page=interfaces");

    await expect(page.getByRole("cell", { name: "mock-video", exact: true })).toBeVisible();
    await page.getByTestId("provider-adapter-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("provider-2");

    await login(page, "/?page=amusement");
    await expect(page.getByText("Team A")).toBeVisible();
    await page.locator('[data-testid^="release-group-booking-"]').first().click();
    await page.getByTestId("confirm-high-impact-ok").click();
    await expect(page.getByTestId("action-result")).toContainText("released");
  });

  test("P0-UI-04 permission and backend error state are visible", async ({ page }) => {
    await installMockApi(page, { forbiddenPath: "/api/v1/analytics/dashboards/p0-overview" });
    await page.goto("/");
    await page.getByTestId("login-username").fill("admin");
    await page.getByTestId("login-password").fill("admin-pass");
    await page.getByTestId("login-submit").click();

    await expect(page.getByTestId("app-shell")).toBeVisible();
    await expect(page.getByTestId("global-error")).toContainText("403");
  });

  test("P0-UI-05 empty state is explicit when business lists are empty", async ({ page }) => {
    await installMockApi(page, { empty: true });
    await page.goto("/?page=ticketing");
    await page.getByTestId("login-username").fill("admin");
    await page.getByTestId("login-password").fill("admin-pass");
    await page.getByTestId("login-submit").click();

    await expect(page.getByText("\u7968\u79cd").first()).toBeVisible();
    await expect(page.locator(".ant-empty").first()).toBeVisible();
  });

  test("P0-UI-06 narrow viewport keeps navigation and content usable", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await installMockApi(page);
    await login(page);

    await page.getByTestId("sidebar-toggle").click();
    await expect(page.getByTestId("app-shell")).toBeVisible();
    await expect(page.getByText("票务核销").first()).toBeVisible();
    const contentWidth = await page.locator(".main-layout > .content").evaluate((element) => element.getBoundingClientRect().width);
    expect(contentWidth).toBeGreaterThanOrEqual(360);
    await expectNoMaterialHorizontalOverflow(page);
    await expect(page).toHaveScreenshot("mobile-dashboard.png", {
      fullPage: true,
      animations: "disabled",
      mask: [page.locator(".page-title-row small"), page.locator(".section-extra small")],
    });
  });

  test("P0-UI-07 role matrix keeps permitted role shells explicit", async ({ page }) => {
    await installMockApi(page, {
      user: { username: "finance-op", accountType: "finance_operator" },
    });
    await login(page, "/?page=analytics", "finance-op");

    await expect(page.locator(".user-chip")).toHaveText("finance-op");
    await expect(page.getByText("\u7ecf\u8425\u5206\u6790").first()).toBeVisible();
    await expect(page.getByText("Main Merchant").first()).toBeVisible();
  });

  test("P0-UI-08 role matrix shows forbidden backend access without hiding the audit trail", async ({ page }) => {
    await installMockApi(page, {
      user: { username: "merchant-op", accountType: "merchant_operator" },
      forbiddenPaths: ["/api/v1/analytics/dashboards/p0-overview"],
    });
    await login(page, "/?page=ticketing", "merchant-op");

    await expect(page.locator(".user-chip")).toHaveText("merchant-op");
    await expect(page.getByTestId("global-error")).toContainText("403");
    await expect(page.getByTestId("global-error")).toContainText("FORBIDDEN");
  });

  test("P0-UI-09 failed login stays on auth screen and surfaces backend reason", async ({ page }) => {
    await installMockApi(page, { authFailure: true });
    await page.goto("/");
    await page.getByTestId("login-username").fill("admin");
    await page.getByTestId("login-password").fill("wrong-pass");
    await page.getByTestId("login-submit").click();

    await expect(page.getByTestId("login-view")).toBeVisible();
    await expect(page.getByTestId("global-error")).toContainText("401");
    await expect(page.getByTestId("app-shell")).toHaveCount(0);
  });

  test("P0-UI-10 failed business action records an actionable error result", async ({ page }) => {
    await installMockApi(page, {
      postErrorPaths: ["/api/v1/ticketing/dynamic-pricing-rules"],
    });
    await login(page, "/?page=ticketing");

    await page.getByTestId("dynamic-pricing-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("SERVER_ERROR");
    await expect(page.getByTestId("action-result")).toContainText("dynamic-pricing-rules");
  });

  test("P0-UI-11 mobile bar critical path remains operable", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await installMockApi(page);
    await login(page, "/?page=bar");

    await expect(page.getByRole("button", { name: "开台、点单、出账、支付" })).toBeVisible();
    await page.getByTestId("bar-flow-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("paid");
    await expectNoMaterialHorizontalOverflow(page);
  });

  test("P0-UI-12 mobile amusement critical path remains operable", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await installMockApi(page);
    await login(page, "/?page=amusement");

    await expect(page.getByRole("button", { name: "创建项目和场次" })).toBeVisible();
    await page.getByTestId("amusement-flow-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("P0 Web Session");
    await expectNoMaterialHorizontalOverflow(page);
  });
  test("P2-UI-13 platform governance topology shows group to venue relationships", async ({ page }) => {
    await installMockApi(page);
    await login(page, "/?page=foundation");

    await expect(page.getByTestId("platform-topology-view")).toBeVisible();
    await expect(page.getByTestId("platform-topology-view")).toContainText("Platform Org");
    await expect(page.getByTestId("platform-topology-view")).toContainText("Main Merchant");
    await expect(page.getByTestId("platform-topology-view")).toContainText("Main Venue");
    await expect(page.getByTestId("platform-topology-view")).toContainText("Gate A");
    await expect(page.getByTestId("platform-topology-node-org-1")).toContainText("\u5546\u6237 1");
    await expect(page.getByTestId("platform-topology-node-merchant-1")).toContainText("\u7efc\u5408\u4f53 1");
    await expect(page.getByTestId("platform-topology-summary")).toContainText("\u7ec4\u7ec7 1");
    await expect(page.getByTestId("platform-topology-summary")).toContainText("\u70b9\u4f4d 1");
  });

});
