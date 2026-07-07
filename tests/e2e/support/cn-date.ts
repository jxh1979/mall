import { expect, type Locator, type Page } from "@playwright/test";

/**
 * 操作自研中文日期选择器 CnDatePicker（页面已按「零英文」把原生 input[type=date] 换成按钮+弹层日历，
 * fill() 不再适用）：点触发按钮打开弹层 → 逐月翻到目标年月 → 点目标日。value=YYYY-MM-DD。
 */
export async function fillCnDate(page: Page, trigger: Locator, value: string) {
  const [year, month, day] = value.split("-").map(Number);
  await trigger.click();
  const pop = page.locator(".cn-date-pop");
  await expect(pop).toBeVisible();
  const title = pop.locator(".cn-date-title");
  const target = year * 12 + (month - 1);
  // ponytail: 上限 60 次翻页（跨 5 年），防死循环；测试用例日期都在当前视图前后一两年内。
  for (let i = 0; i < 60; i += 1) {
    const text = await title.innerText();
    const match = /(\d+)年(\d+)月/.exec(text);
    if (!match) throw new Error(`无法解析日历标题: ${text}`);
    const current = Number(match[1]) * 12 + (Number(match[2]) - 1);
    if (current === target) break;
    await pop.getByRole("button", { name: current < target ? "下个月" : "上个月" }).click();
  }
  await pop.locator(".cn-date-grid").getByRole("button", { name: String(day), exact: true }).click();
  await expect(pop).toHaveCount(0);
}
