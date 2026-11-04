// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { FinanceDailyClosePanel } from "./FinanceDailyClosePanel";
import type { AdminApiClient, AdminUser } from "../types";

// 第十八批 P 段第 133 条 133-R3（BSM:1668「……最后形成日结和结算依据。」；ENUM:188 对账口径含「按日结」；
// BSM:1682「按营业日生成日结批次。」；BSM:2023「已拍板：日结锁定后不能直接补录影响金额，必须上级审批通过后补录。」）。
const CLOSES = [
  { id: "dc1", businessDate: "2026-11-02", scopeType: "bar", scopeId: "bar_a", status: "locked", lockedAt: "2026-11-03T02:00:00Z" },
];
const SUMMARIES = [
  { id: "sm1", dailyCloseId: "dc1", sourceStream: "bar_deposit", amountCent: 10000, refundCent: 0, orderCount: 1 },
  { id: "sm2", dailyCloseId: "dc1", sourceStream: "bar", amountCent: 25000, refundCent: 500, orderCount: 3 },
];
const BARS = [
  { id: "bar_a", merchantName: "一号酒吧", merchantType: "bar" },
  { id: "bar_b", merchantName: "二号酒吧", merchantType: "bar" },
];
const STAFF = { id: "u1", username: "财务甲", accountType: "staff" } as AdminUser;
const MERCHANT = { id: "u1", username: "酒吧财务", accountType: "merchant", merchantId: "bar_a" } as AdminUser;

function setup({ grants = ["finance.daily_close.manage", "finance.daily_close.lock"], user = STAFF,
  post = vi.fn().mockResolvedValue({ id: "dc-new", status: "locked" }) } = {}) {
  const get = vi.fn(async (url: string) => (url.endsWith("/summaries") ? SUMMARIES : url === "/api/v1/platform/merchants" ? BARS : []));
  const getPaged = vi.fn(async () => ({ items: CLOSES, total: 1, page: 1, size: 20 }));
  const api = { get, getPaged, post, put: vi.fn(), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
  render(<FinanceDailyClosePanel api={api} user={user} setGlobalError={vi.fn()}
    effectivePermissions={{ status: "ready", value: { grants, denies: [], wildcard: false } }} />);
  return { get, getPaged, post };
}

// 中文日期选择器：选年 → 选月 → 选日（组件不读环境时钟，默认翻到兜底年月）。
async function pickDate(year: string, month: string, day: string) {
  fireEvent.click(screen.getByTestId("daily-close-date"));
  fireEvent.click(await screen.findByRole("button", { name: "选择年月" }));
  fireEvent.click(screen.getByRole("button", { name: "选择年份" }));
  fireEvent.click(screen.getByRole("button", { name: year }));
  fireEvent.click(screen.getByRole("button", { name: month }));
  fireEvent.click(screen.getByRole("button", { name: day }));
  await waitFor(() => expect(screen.getByTestId("daily-close-date").textContent).toContain(`${year}年`));
}

describe("日结区块", () => {
  afterEach(() => vi.restoreAllMocks());

  it("列表显示中文范围类型与状态，按页读日结列表", async () => {
    const { getPaged } = setup();
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")?.textContent ?? "").toContain("已锁定"));
    expect(screen.getByTestId("daily-close-row-dc1").textContent).toContain("酒吧");
    expect(screen.getByTestId("daily-close-row-dc1").textContent).not.toContain("locked");
    expect(getPaged).toHaveBeenCalledWith("/api/v1/finance/daily-closes?page=1&size=20");
  });

  it("点某一行后显示七源汇总中文名和金额", async () => {
    const { get } = setup();
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-dc1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-dc1"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-summary-table")?.textContent ?? "").toContain("订台定金"));
    expect(screen.getByTestId("daily-close-summary-table").textContent).toContain("100.00");
    expect(screen.getByTestId("daily-close-summary-table").textContent).not.toContain("bar_deposit");
    expect(get).toHaveBeenCalledWith("/api/v1/finance/daily-closes/dc1/summaries");
  });

  it("平台侧账号锁定：选营业日、范围类型选酒吧、选酒吧，确认后提交且不发强制日结", async () => {
    const { get, getPaged, post } = setup();
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    fireEvent.change(screen.getByTestId("daily-close-scope-type"), { target: { value: "bar" } });
    await waitFor(() => expect((screen.queryByTestId("daily-close-scope-bar") as HTMLSelectElement | null)?.value).toBe("bar_a"));
    fireEvent.change(screen.getByTestId("daily-close-scope-bar"), { target: { value: "bar_b" } });
    await waitFor(() => expect((screen.getByTestId("daily-close-scope-bar") as HTMLSelectElement).value).toBe("bar_b"));
    await pickDate("2026", "11月", "2");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-lock-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes", expect.objectContaining({
      businessDate: "2026-11-02", scopeType: "bar", scopeId: "bar_b", idempotencyKey: expect.any(String) })));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("forceClose");
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/finance/daily-closes/dc-new/summaries"));
    await waitFor(() => expect(getPaged).toHaveBeenCalledTimes(2));
  });

  it("商户侧账号锁定：范围对象固定为本账号商户、范围类型只有商户与酒吧，409 时显示原文", async () => {
    const post = vi.fn().mockRejectedValue(new Error("该营业日已完成日结锁定：日期 2026-11-02，范围 bar/bar_a"));
    const { get } = setup({ user: MERCHANT, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    expect(screen.queryByTestId("daily-close-scope-bar")).toBeNull();
    const options = [...(screen.getByTestId("daily-close-scope-type") as HTMLSelectElement).options].map((o) => o.textContent);
    expect(options).toEqual(["商户", "酒吧"]);
    fireEvent.change(screen.getByTestId("daily-close-scope-type"), { target: { value: "bar" } });
    await waitFor(() => expect((screen.getByTestId("daily-close-scope-type") as HTMLSelectElement).value).toBe("bar"));
    await pickDate("2026", "11月", "2");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-lock-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes",
      expect.objectContaining({ scopeType: "bar", scopeId: "bar_a" })));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("该营业日已完成日结锁定"));
    expect(get).not.toHaveBeenCalledWith("/api/v1/platform/merchants");
  });

  it("没有锁定权限不出锁定表单，没有日结查看权限整块不渲染", async () => {
    setup({ grants: ["finance.daily_close.manage"] });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    expect(screen.queryByTestId("daily-close-lock-form")).toBeNull();
    const view = setup({ grants: ["finance.daily_close.lock"] });
    await waitFor(() => expect(screen.getAllByTestId("finance-daily-close-panel")).toHaveLength(1));
    expect(view.getPaged).not.toHaveBeenCalled();
  });
});
