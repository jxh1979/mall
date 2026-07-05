import { expect, test } from "@playwright/test";
import { installMockApi } from "./support/mock-api";

async function adminLogin(page: import("@playwright/test").Page, path: string) {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill("admin");
  await page.getByTestId("login-password").fill("admin-pass");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("admin-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

test("ADM-FIN-004 物业费管理：费率设置 + 账单生成走通", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await installMockApi(page);
  await adminLogin(page, "/admin/finance/property-fee");

  await expect(page.getByTestId("admin-page-title")).toContainText("物业费管理");
  await expect(page.getByTestId("finance-property-fee-page")).toBeVisible();
  // 费率列表已渲染 mock 默认费率
  await expect(page.getByTestId("property-fee-rates-table")).toContainText("默认");
  await page.screenshot({ path: "playwright-report/property-fee-01-landing.png", fullPage: true });

  // 设费率：填单价 → 保存
  await page.getByTestId("property-fee-rate-amount").fill("28.50");
  await page.getByTestId("property-fee-rate-save").click();
  await expect(page.getByTestId("property-fee-rate-amount")).toHaveValue("");
  await page.screenshot({ path: "playwright-report/property-fee-02-rate-saved.png", fullPage: true });

  // 出账：填期 + 勾商户 → 生成
  await page.getByTestId("property-fee-bill-period").fill("2026-07");
  await page.getByTestId("property-fee-merchant-select-merchant-1").check();
  await page.getByTestId("property-fee-bill-run").click();
  await expect(page.getByTestId("property-fee-bills-table")).toBeVisible();
  await expect(page.getByTestId("property-fee-bills-table")).toContainText("3,000.00");
  await page.screenshot({ path: "playwright-report/property-fee-03-billed.png", fullPage: true });

  // 行明细抽屉
  await page.getByTestId("property-fee-bill-row-pfb-1").click();
  await expect(page.getByText("物业费账单明细")).toBeVisible();
  await page.screenshot({ path: "playwright-report/property-fee-04-bill-detail.png", fullPage: true });
});
