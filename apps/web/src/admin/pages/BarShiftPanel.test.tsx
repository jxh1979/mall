// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarShiftPanel } from "./BarShiftPanel";
import type { AdminApiClient, AdminUser } from "../types";

// 第十八批 P 段第 133 条 133-R1（BSM:1081「收银员开班记备用金，班结按本班次汇总各支付方式应收（现金/扫码/POS/退款/小费），录入实点现金算长短款差异。
// 收银不应出现差额，出现任何金额的差额一律进复核、定位责任，不设容差」；BSM:940 按该次登录会话定位到操作人；ENUM:115 收银交班状态）。
const SHIFTS = [
  { id: "s1", barId: "bar_a", cashierUserId: "u1", status: "open", openedAt: "2026-11-03T10:00:00Z", openingCashCent: 20000 },
  { id: "s2", barId: "bar_a", cashierUserId: "u2", status: "open", openedAt: "2026-11-03T10:05:00Z", openingCashCent: 10000 },
  { id: "s3", barId: "bar_a", cashierUserId: "u1", status: "closed", openingCashCent: 20000, declaredCashCent: 29900, systemCashCent: 10000, cashDiffCent: -100, needsReview: true },
  { id: "s4", barId: "bar_a", cashierUserId: "u1", status: "closed", openingCashCent: 20000, declaredCashCent: 30000, systemCashCent: 10000, cashDiffCent: 0, needsReview: false },
];
const BARS = [
  { id: "bar_a", merchantName: "一号酒吧", merchantType: "bar" },
  { id: "bar_b", merchantName: "二号酒吧", merchantType: "bar" },
];

function setup({ grants = ["bar.shift.manage", "bar.shift.close"], user = { id: "u1", username: "收银员甲", accountType: "staff" } as AdminUser,
  post = vi.fn().mockResolvedValue({ id: "s-new", status: "open" }) } = {}) {
  const get = vi.fn(async (url: string) => (url === "/api/v1/bar/shifts" ? SHIFTS : url === "/api/v1/platform/merchants" ? BARS : []));
  const api = { get, getPaged: vi.fn(), post, put: vi.fn(), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
  const setActionResult = vi.fn();
  render(<BarShiftPanel api={api} user={user} setActionResult={setActionResult} setGlobalError={vi.fn()}
    effectivePermissions={{ status: "ready", value: { grants, denies: [], wildcard: false } }} />);
  return { get, post, setActionResult };
}

describe("收银交班对账区块", () => {
  afterEach(() => vi.restoreAllMocks());

  it("列表按交班状态显示开班、有差异待复核与短款、已平账", async () => {
    setup();
    await waitFor(() => expect(screen.queryByTestId("bar-shift-row-s3")?.textContent ?? "").toContain("有差异待复核"));
    expect(screen.getByTestId("bar-shift-row-s3").textContent).toContain("短款 1.00");
    expect(screen.getByTestId("bar-shift-row-s1").textContent).toContain("开班");
    expect(screen.getByTestId("bar-shift-row-s4").textContent).toContain("已平账");
  });

  it("平台侧账号开班：选酒吧、收银员取登录人、不发容差", async () => {
    const { post } = setup({ user: { id: "u1", username: "平台收银", accountType: "platform_admin" } });
    await waitFor(() => expect((screen.queryByTestId("bar-shift-open-bar") as HTMLSelectElement | null)?.value).toBe("bar_a"));
    expect(screen.queryByLabelText(/收银员/)).toBeNull();
    fireEvent.change(screen.getByTestId("bar-shift-open-bar"), { target: { value: "bar_b" } });
    fireEvent.change(screen.getByLabelText("备用金"), { target: { value: "200" } });
    await waitFor(() => expect((screen.getByTestId("bar-shift-open-bar") as HTMLSelectElement).value).toBe("bar_b"));
    await waitFor(() => expect((screen.getByLabelText("备用金") as HTMLInputElement).value).toBe("200"));
    fireEvent.click(screen.getByTestId("bar-shift-open-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/open", expect.objectContaining({
      barId: "bar_b", cashierUserId: "u1", openingCashCent: 20000, idempotencyKey: expect.any(String) })));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("cashDiffThresholdCent");
  });

  it("商户侧账号开班：酒吧固定取本账号商户、不读平台商户列表", async () => {
    const { get, post } = setup({ user: { id: "u1", username: "酒吧收银", accountType: "merchant", merchantId: "bar_a" } });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-row-s1")).not.toBeNull());
    expect(screen.queryByTestId("bar-shift-open-bar")).toBeNull();
    fireEvent.change(screen.getByLabelText("备用金"), { target: { value: "150.5" } });
    await waitFor(() => expect((screen.getByLabelText("备用金") as HTMLInputElement).value).toBe("150.5"));
    fireEvent.click(screen.getByTestId("bar-shift-open-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/open",
      expect.objectContaining({ barId: "bar_a", cashierUserId: "u1", openingCashCent: 15050 })));
    expect(get).not.toHaveBeenCalledWith("/api/v1/platform/merchants");
  });

  it("班结只给当班收银员本人：填实点现金、确认后提交并显示有差异已进复核", async () => {
    const post = vi.fn().mockResolvedValue({ id: "s1", status: "closed", systemCashCent: 0, declaredCashCent: 19900, cashDiffCent: -100, needsReview: true });
    const { setActionResult } = setup({ post });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-close-s1")).not.toBeNull());
    expect(screen.queryByTestId("bar-shift-close-s2")).toBeNull();
    fireEvent.click(screen.getByTestId("bar-shift-close-s1"));
    await waitFor(() => expect(screen.getByLabelText("实点现金")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("实点现金"), { target: { value: "199" } });
    await waitFor(() => expect((screen.getByLabelText("实点现金") as HTMLInputElement).value).toBe("199"));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("bar-shift-close-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/s1/close",
      expect.objectContaining({ declaredCashCent: 19900, idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(screen.getByTestId("bar-shift-close-result").textContent).toContain("有差异"));
    expect(setActionResult.mock.calls[0]?.[0]).not.toHaveProperty("openedAt");
  });

  it("没有班结权限不出班结按钮，没有交班管理权限整块不渲染", async () => {
    setup({ grants: ["bar.shift.manage"] });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-row-s1")).not.toBeNull());
    expect(screen.queryByTestId("bar-shift-close-s1")).toBeNull();
    const view = setup({ grants: ["bar.shift.close"] });
    await waitFor(() => expect(screen.getAllByTestId("bar-shift-panel")).toHaveLength(1));
    expect(view.get).not.toHaveBeenCalled();
  });
});
