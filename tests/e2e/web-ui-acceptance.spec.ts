import { expect, test } from "@playwright/test";
import { installMockApi } from "./support/mock-api";

async function login(page: import("@playwright/test").Page, path = "/") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill("admin");
  await page.getByTestId("login-password").fill("admin-pass");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("app-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
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
    await expect(page.getByTestId("action-result")).toContainText("active");

    await page.getByTestId("dynamic-pricing-simulation-form").locator('button[type="submit"]').click();
    await expect(page.getByTestId("action-result")).toContainText("finalPriceCent");
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
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(120);
    await expect(page).toHaveScreenshot("mobile-dashboard.png", {
      fullPage: true,
      animations: "disabled",
      mask: [page.locator(".page-title-row small"), page.locator(".section-extra small")],
    });
  });
});
