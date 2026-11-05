// 文件版本：V0.02
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
  { id: "s4", barId: "bar_a", cashierUserId: "u1", status: "closed", openingCashCent: 20000, declaredCashCent: 30000, systemCashCent: 10000, cashDiffCent: 0, needsReview: false, handoverToUserId: "u2" },
];
// 第十八批 R 段第 142 条 142-R3：对账页复核区块用的班次——开班、短款待复核、长款待复核、已平账未签、收银员已签且无接班人、双方已签、已归档。
const REVIEW_SHIFTS = [
  ...SHIFTS,
  { id: "s5", barId: "bar_a", cashierUserId: "u2", status: "closed", openingCashCent: 20000, declaredCashCent: 30100, systemCashCent: 10000, cashDiffCent: 100, needsReview: true },
  { id: "s6", barId: "bar_a", cashierUserId: "u2", status: "closed", cashDiffCent: 0, needsReview: false, handoverToUserId: null, cashierSignedAt: "2026-11-03T12:00:00Z" },
  { id: "s7", barId: "bar_a", cashierUserId: "u2", status: "closed", cashDiffCent: 0, needsReview: false, handoverToUserId: "u3", cashierSignedAt: "2026-11-03T12:00:00Z", successorSignedAt: "2026-11-03T12:05:00Z" },
  { id: "s8", barId: "bar_a", cashierUserId: "u2", status: "closed", cashDiffCent: 0, needsReview: false, cashierSignedAt: "2026-11-03T12:00:00Z", successorSignedAt: "2026-11-03T12:10:00Z", witnessedAt: "2026-11-03T12:10:00Z" },
];
const BARS = [
  { id: "bar_a", merchantName: "一号酒吧", merchantType: "bar" },
  { id: "bar_b", merchantName: "二号酒吧", merchantType: "bar" },
];

function setup({ grants = ["bar.shift.manage", "bar.shift.close"], user = { id: "u1", username: "收银员甲", accountType: "staff" } as AdminUser,
  post = vi.fn().mockResolvedValue({ id: "s-new", status: "open" }), shifts = SHIFTS as Record<string, unknown>[], reviewOnly = false } = {}) {
  const get = vi.fn(async (url: string) => (url === "/api/v1/bar/shifts" ? shifts : url === "/api/v1/platform/merchants" ? BARS : []));
  const api = { get, getPaged: vi.fn(), post, put: vi.fn(), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
  const setActionResult = vi.fn();
  render(<BarShiftPanel api={api} user={user} setActionResult={setActionResult} setGlobalError={vi.fn()} reviewOnly={reviewOnly}
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

// 第十八批 R 段第 142 条 142-R3（BSM:1081「出现任何金额的差额一律进复核、定位责任」「当班收银员与接班人双方确认签字（电子确认同效；
// 营业日最后一班没有接班人的，由财务跟班代替接班人确认签字），财务跟班见证后归档」；BSM:1848「财务看退款、差异、日结和结算依据。」）。
const FINANCE = { id: "f1", username: "财务甲", accountType: "staff" } as AdminUser;
const shiftGets = (get: { mock: { calls: unknown[][] } }) => get.mock.calls.filter((call) => call[0] === "/api/v1/bar/shifts").length;

describe("交班单签字与财务复核见证", () => {
  afterEach(() => vi.restoreAllMocks());

  it("当班收银员本人对已平账未签的班次办交班单签字，有差异待复核的行不出签字", async () => {
    const { get, post } = setup({ post: vi.fn().mockResolvedValue({ id: "s4" }) });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-sign-s4")).not.toBeNull());
    expect(screen.queryByTestId("bar-shift-sign-s3")).toBeNull();
    expect(screen.getByTestId("bar-shift-row-s4").textContent).toContain("待收银员签字");
    expect(screen.getByTestId("bar-shift-row-s3").textContent).toContain("待复核平账");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("bar-shift-sign-s4"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/s4/sign", expect.objectContaining({ idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(shiftGets(get)).toBe(2));
  });

  it("接班人本人同样出签字，当班收银员与接班人以外的人不出", async () => {
    setup({ user: { id: "u2", username: "收银员乙", accountType: "staff" } as AdminUser });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-sign-s4")).not.toBeNull());
    const view = setup({ user: { id: "u3", username: "收银员丙", accountType: "staff" } as AdminUser });
    await waitFor(() => expect(shiftGets(view.get)).toBe(1));
    await waitFor(() => expect(screen.getAllByTestId("bar-shift-row-s4")).toHaveLength(2));
    expect(screen.getAllByTestId("bar-shift-sign-s4")).toHaveLength(1);
  });

  it("对账页复核区块：没有日结管理权限整块不渲染、不读班次", async () => {
    const { get } = setup({ reviewOnly: true });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-panel")).toBeNull());
    expect(get).not.toHaveBeenCalled();
  });

  it("对账页复核区块不出开班、班结与签字；短款行确认后复核平账，不带长款原因", async () => {
    const { post } = setup({ reviewOnly: true, user: { ...FINANCE, id: "u1" }, grants: ["finance.daily_close.manage", "bar.shift.manage", "bar.shift.close"],
      shifts: REVIEW_SHIFTS, post: vi.fn().mockResolvedValue({ id: "s3" }) });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-resolve-s3")).not.toBeNull());
    expect(screen.queryByTestId("bar-shift-open-form")).toBeNull();
    expect(screen.queryByTestId("bar-shift-close-s1")).toBeNull();
    expect(screen.queryByTestId("bar-shift-sign-s4")).toBeNull();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("bar-shift-resolve-s3"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/s3/resolve", expect.objectContaining({ idempotencyKey: expect.any(String) })));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("overageCause");
  });

  it("长款行未填录像查明的原因不发请求，填了带原因复核平账", async () => {
    const { post } = setup({ reviewOnly: true, user: FINANCE, grants: ["finance.daily_close.manage"], shifts: REVIEW_SHIFTS,
      post: vi.fn().mockResolvedValue({ id: "s5" }) });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-resolve-s5")).not.toBeNull());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("bar-shift-resolve-s5"));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("录像查明的原因"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("录像查明的原因 s5"), { target: { value: "录像见客人多付" } });
    await waitFor(() => expect((screen.getByLabelText("录像查明的原因 s5") as HTMLInputElement).value).toBe("录像见客人多付"));
    fireEvent.click(screen.getByTestId("bar-shift-resolve-s5"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/s5/resolve", expect.objectContaining({ overageCause: "录像见客人多付" })));
  });

  it("签齐后见证归档；没有接班人的显「代接班人签字并见证归档」；已归档与未签齐的不出；后端拒绝原文显示", async () => {
    const { post } = setup({ reviewOnly: true, user: FINANCE, grants: ["finance.daily_close.manage"], shifts: REVIEW_SHIFTS,
      post: vi.fn().mockRejectedValue(new Error("本班次交班单已见证归档")) });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-witness-s6")).not.toBeNull());
    expect(screen.getByTestId("bar-shift-witness-s6").textContent).toBe("代接班人签字并见证归档");
    expect(screen.getByTestId("bar-shift-witness-s7").textContent).toBe("见证归档");
    expect(screen.queryByTestId("bar-shift-witness-s4")).toBeNull();
    expect(screen.queryByTestId("bar-shift-witness-s8")).toBeNull();
    expect(screen.getByTestId("bar-shift-row-s8").textContent).toContain("已归档");
    expect(screen.getByTestId("bar-shift-row-s6").textContent).toContain("待财务见证");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("bar-shift-witness-s6"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/s6/witness", expect.objectContaining({ idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toBe("本班次交班单已见证归档"));
  });
});

// 第十八批 R 段第 142 条 142-R4（BSM:1081「单人当班或能够明确定位到具体收银员时，由该收银员补足；多人共同使用同一钱箱时，由整个当班组共同补足。」
// 「当班收银员与接班人双方确认签字」；BSM:1489、ENUM:283）：同一钱箱多人当班——开班中、成员未签的已平账、有成员的短款待复核。
const MEMBER_SHIFTS = [
  { id: "m1", barId: "bar_a", cashierUserId: "u1", status: "open", openingCashCent: 20000, members: [{ userId: "u2", joinedAt: "2026-11-05T10:00:00Z", signedAt: null }] },
  { id: "m2", barId: "bar_a", cashierUserId: "u1", status: "closed", cashDiffCent: 0, needsReview: false, cashierSignedAt: "2026-11-05T12:00:00Z",
    members: [{ userId: "u2", joinedAt: "2026-11-05T10:00:00Z", signedAt: null }] },
  { id: "m3", barId: "bar_a", cashierUserId: "u1", status: "closed", cashDiffCent: -100, needsReview: true, members: [{ userId: "u2", joinedAt: "2026-11-05T10:00:00Z" }] },
];

describe("同一钱箱多人当班", () => {
  afterEach(() => vi.restoreAllMocks());

  it("本人没有开班中班次时，他人开班中的班次出「加入本钱箱」，点了发加入请求；本人已有开班中班次的不出", async () => {
    const { post } = setup({ user: { id: "u3", username: "收银员丙", accountType: "staff" } as AdminUser, post: vi.fn().mockResolvedValue({ id: "s1" }) });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-join-s1")).not.toBeNull());
    expect(screen.queryByTestId("bar-shift-join-s2")).not.toBeNull();
    fireEvent.click(screen.getByTestId("bar-shift-join-s1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/s1/join", expect.objectContaining({ idempotencyKey: expect.any(String) })));
    const view = setup();
    await waitFor(() => expect(shiftGets(view.get)).toBe(1));
    await waitFor(() => expect(screen.getAllByTestId("bar-shift-row-s2")).toHaveLength(2));
    expect(screen.getAllByTestId("bar-shift-join-s2")).toHaveLength(1);
  });

  it("成员显示在当班组成员列，成员同样出班结交班与交班单签字，不再出加入；成员未签时交班单显待收银员签字", async () => {
    setup({ user: { id: "u2", username: "收银员乙", accountType: "staff" } as AdminUser, shifts: MEMBER_SHIFTS });
    await waitFor(() => expect(screen.queryByTestId("bar-shift-close-m1")).not.toBeNull());
    expect(screen.getByTestId("bar-shift-row-m1").textContent).toContain("u2");
    expect(screen.queryByTestId("bar-shift-join-m1")).toBeNull();
    expect(screen.queryByTestId("bar-shift-sign-m2")).not.toBeNull();
    expect(screen.getByTestId("bar-shift-row-m2").textContent).toContain("待收银员签字");
  });

  it("对账页：有成员的短款行出责任人选择、默认当班组共同补足，选成员后复核带责任人；成员未签齐不出见证", async () => {
    const { post } = setup({ reviewOnly: true, user: FINANCE, grants: ["finance.daily_close.manage"], shifts: MEMBER_SHIFTS,
      post: vi.fn().mockResolvedValue({ id: "m3" }) });
    await waitFor(() => expect(screen.queryByLabelText("责任人 m3")).not.toBeNull());
    const select = screen.getByLabelText("责任人 m3") as HTMLSelectElement;
    expect(select.value).toBe("");
    expect(Array.from(select.options).map((o) => o.textContent)).toEqual(["当班组共同补足", "u1", "u2"]);
    expect(screen.queryByTestId("bar-shift-witness-m2")).toBeNull();
    fireEvent.change(select, { target: { value: "u2" } });
    await waitFor(() => expect((screen.getByLabelText("责任人 m3") as HTMLSelectElement).value).toBe("u2"));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("bar-shift-resolve-m3"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/m3/resolve", expect.objectContaining({ responsibleUserId: "u2" })));
  });
});
