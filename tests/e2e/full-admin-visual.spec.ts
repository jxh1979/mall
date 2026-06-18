import { expect, test } from "@playwright/test";
import { installMockApi } from "./support/mock-api";

async function adminLogin(page: import("@playwright/test").Page, path = "/admin/overview") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill("admin");
  await page.getByTestId("login-password").fill("admin-pass");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("admin-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

async function expectNoDocumentOverflow(page: import("@playwright/test").Page, allowedPixels = 12) {
  const metrics = await page.evaluate(() => ({
    overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    width: document.documentElement.clientWidth,
  }));
  expect(metrics.overflow, `document horizontal overflow at ${metrics.width}px viewport`).toBeLessThanOrEqual(allowedPixels);
}

test.describe("full admin visual and responsive acceptance", () => {
  test("ADM-VIS-01 desktop overview and production evidence pages keep a stable visual baseline", async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await installMockApi(page);
    await adminLogin(page, "/admin/overview");

    await expect(page.getByTestId("admin-page-title")).toContainText("总控驾驶舱");
    await expect(page.getByTestId("admin-overview")).toContainText("票务");
    await expectNoDocumentOverflow(page);
    await expect(page).toHaveScreenshot("full-admin-overview-desktop.png", {
      fullPage: true,
      animations: "disabled",
    });

    await page.getByTestId("admin-nav-ADM-OPS-001").click();
    await expect(page).toHaveURL(/\/admin\/ops\/readiness$/);
    await expect(page.getByTestId("admin-page-title")).toContainText("生产 blocker 与证据");
    await expect(page.getByTestId("ops-readiness-page")).toContainText("NAS");
    await expectNoDocumentOverflow(page);
    await expect(page).toHaveScreenshot("full-admin-ops-readiness-desktop.png", {
      fullPage: true,
      animations: "disabled",
    });
  });

  test("ADM-VIS-02 mobile admin routes keep nav, tables and primary actions usable", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await installMockApi(page);
    await adminLogin(page, "/admin/bar/orders");

    await expect(page.getByTestId("admin-page-title")).toContainText("订单/出品/账单");
    await expect(page.getByTestId("bar-orders-page")).toBeVisible();
    await expect(page.getByTestId("bar-orders-table")).toContainText("ORD-1");
    await expectNoDocumentOverflow(page, 24);
    await expect(page).toHaveScreenshot("full-admin-bar-orders-mobile.png", {
      fullPage: true,
      animations: "disabled",
    });

    await page.getByTestId("admin-nav-ADM-TKT-002").click();
    await expect(page).toHaveURL(/\/admin\/ticketing\/dynamic-pricing$/);
    await expect(page.getByTestId("dynamic-pricing-page")).toBeVisible();
    await expect(page.getByTestId("dynamic-pricing-table")).toContainText("Weekend Boost");
    await expectNoDocumentOverflow(page, 24);
    await expect(page).toHaveScreenshot("full-admin-dynamic-pricing-mobile.png", {
      fullPage: true,
      animations: "disabled",
    });
  });
});