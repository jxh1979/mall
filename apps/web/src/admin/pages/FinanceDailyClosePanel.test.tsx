// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { FinanceDailyClosePanel } from "./FinanceDailyClosePanel";
import type { AdminApiClient, AdminUser } from "../types";

// 第十八批 P 段第 133 条 133-R3（BSM:1668「……最后形成日结和结算依据。」；ENUM:188 对账口径含「按日结」；
// BSM:1682「按营业日生成日结批次。」；BSM:2023「已拍板：日结锁定后不能直接补录影响金额，必须上级审批通过后补录。」）。
// 第十八批 R 段第 143 条 143-R1（ENUM:190「待生成、待确认、有差异、已确认、已锁定、已归档」）：生成、确认、锁定三步，查不到即「待生成」。
// 143-R4：已锁定、已归档的日结展开后列出被它锁定的台账与锁定后补录，可在此申请补录、审批补录。
const CLOSES = [
  { id: "dc1", businessDate: "2026-11-02", scopeType: "bar", scopeId: "bar_a", status: "locked", lockedAt: "2026-11-03T02:00:00Z" },
];
const STEPS = [
  { id: "d1", businessDate: "2026-11-03", scopeType: "merchant", scopeId: "bar_a", status: "pending_confirm", lockedAt: null },
  { id: "d2", businessDate: "2026-11-02", scopeType: "merchant", scopeId: "bar_a", status: "confirmed", confirmedAt: "2026-11-03T03:00:00Z", lockedAt: null },
  { id: "d3", businessDate: "2026-11-01", scopeType: "merchant", scopeId: "bar_a", status: "locked", confirmedAt: "2026-11-02T03:00:00Z", lockedAt: "2026-11-02T04:00:00Z" },
];
// 143-R5（第十八批裁决 33）：已归档的日结。
const ARCHIVED = { id: "d4", businessDate: "2026-10-31", scopeType: "merchant", scopeId: "bar_a", status: "archived",
  confirmedAt: "2026-11-01T03:00:00Z", lockedAt: "2026-11-01T04:00:00Z", archivedAt: "2026-12-25T05:00:00Z",
  finalMarkCategories: ["orders_verifications", "refunds_reversals", "production_handover", "attendance", "anomaly_resolutions"] };
// 143-R4（ENUM:190「……在原营业日的日结锁定表单上关联显示该笔补录的金额与情况，两边互相可查」）：被 d3 锁定的台账与锁定后补录。
// 第 142 条 142-R1：长款复核平账写的营业外收入台账方向为内部核算（internal_accounting），锁定时按到期日与商户一并锁进该酒吧日结。
const LOCKED_ITEMS = [{ id: "it1", itemNo: "AR-0001", direction: "receivable", amountCent: 10000, dueDate: "2026-11-01" },
  { id: "it2", itemNo: "IA-0001", direction: "internal_accounting", amountCent: 100, dueDate: "2026-11-01" }];
const ADJUSTMENTS = [
  { id: "adj1", arApItemId: "it1", itemNo: "AR-0001", lockedDayCloseId: "d3", adjustmentAmountCent: 500, approvalStatus: "approved",
    requestedBy: "财务甲", approvedBy: "财务乙", reason: "漏记收入", createdAt: "2026-11-03T02:00:00Z", decidedAt: "2026-11-03T05:00:00Z",
    originalBusinessDate: "2026-11-01" },
  { id: "adj2", arApItemId: "it1", itemNo: "AR-0001", lockedDayCloseId: "d3", adjustmentAmountCent: -300, approvalStatus: "pending",
    requestedBy: "财务丙", approvedBy: null, reason: "多记冲减", createdAt: "2026-11-04T02:00:00Z", decidedAt: null,
    originalBusinessDate: "2026-11-01" },
];
const BACKFILL = ["finance.daily_close.manage", "finance.daily_close.lock", "finance.ar_ap.view", "finance.locked_backfill.request",
  "finance.locked_backfill.approve"];
const SUMMARIES = [
  { id: "sm1", dailyCloseId: "dc1", sourceStream: "bar_deposit", amountCent: 10000, refundCent: 0, orderCount: 1 },
  { id: "sm2", dailyCloseId: "dc1", sourceStream: "bar", amountCent: 25000, refundCent: 500, orderCount: 3 },
  { id: "sm3", dailyCloseId: "dc1", sourceStream: "low_value_difference", amountCent: 200000, refundCent: 0, orderCount: 2 },
];
const BARS = [
  { id: "bar_a", merchantName: "一号酒吧", merchantType: "bar" },
  { id: "bar_b", merchantName: "二号酒吧", merchantType: "bar" },
];
const STAFF = { id: "u1", username: "财务甲", accountType: "staff" } as AdminUser;
const MERCHANT = { id: "u1", username: "酒吧财务", accountType: "merchant", merchantId: "bar_a" } as AdminUser;

function setup({ grants = ["finance.daily_close.manage", "finance.daily_close.lock"], user = STAFF,
  post = vi.fn().mockResolvedValue({ id: "dc-new", status: "pending_confirm" }), rows = CLOSES as Record<string, unknown>[],
  current = [] as Record<string, unknown>[] } = {}) {
  const get = vi.fn(async (url: string) => (url.endsWith("/summaries") ? SUMMARIES : url.endsWith("/adjustments") ? ADJUSTMENTS
    : url === "/api/v1/platform/merchants" ? BARS : []));
  // 列表按页读；带营业日与范围的是生成表单查「当前状态」。
  const getPaged = vi.fn(async (url: string) => (url.startsWith("/api/v1/finance/ar-ap-items")
    ? { items: LOCKED_ITEMS, total: LOCKED_ITEMS.length, page: 1, size: 200 } : url.includes("business_date=")
    ? { items: current, total: current.length, page: 1, size: 1 } : { items: rows, total: rows.length, page: 1, size: 20 }));
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

  // 第十八批 Y 段第 182 条 182-R1（ENUM:191「低价值差异单独列示带账进入下期」）：汇总表单列低价值未关闭差异（含上期带入）。
  it("汇总表单列低价值未关闭差异的中文名与金额", async () => {
    setup();
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")?.textContent ?? "").toContain("已锁定"));
    fireEvent.click(screen.getByTestId("daily-close-open-dc1"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-summary-table")?.textContent ?? "").toContain("低价值未关闭差异（含上期带入）"));
    expect(screen.getByTestId("daily-close-summary-table").textContent).toContain("2000.00");
    expect(screen.getByTestId("daily-close-summary-table").textContent).not.toContain("low_value_difference");
  });

  it("平台侧账号生成：选营业日、范围类型选酒吧、选酒吧，查不到日结显示待生成，生成后提交且不发强制日结", async () => {
    const { get, getPaged, post } = setup();
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    fireEvent.change(screen.getByTestId("daily-close-scope-type"), { target: { value: "bar" } });
    await waitFor(() => expect((screen.queryByTestId("daily-close-scope-bar") as HTMLSelectElement | null)?.value).toBe("bar_a"));
    fireEvent.change(screen.getByTestId("daily-close-scope-bar"), { target: { value: "bar_b" } });
    await waitFor(() => expect((screen.getByTestId("daily-close-scope-bar") as HTMLSelectElement).value).toBe("bar_b"));
    await pickDate("2026", "11月", "2");
    await waitFor(() => expect(screen.queryByTestId("daily-close-current-status")?.textContent ?? "").toContain("待生成"));
    expect(getPaged).toHaveBeenCalledWith("/api/v1/finance/daily-closes?business_date=2026-11-02&scope_type=bar&scope_id=bar_b&page=1&size=1");
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-generate-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes", expect.objectContaining({
      businessDate: "2026-11-02", scopeType: "bar", scopeId: "bar_b", idempotencyKey: expect.any(String) })));
    expect(confirm).not.toHaveBeenCalled();
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("forceClose");
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/finance/daily-closes/dc-new/summaries"));
    await waitFor(() => expect(getPaged.mock.calls.filter(([url]) => url === "/api/v1/finance/daily-closes?page=1&size=20")).toHaveLength(2));
  });

  // 第十八批 S 段第 161 条 161-R3（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：票务、游乐不再单独出日结。
  it("平台侧账号范围类型只有项目、商户、酒吧", async () => {
    setup();
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    const options = [...(screen.getByTestId("daily-close-scope-type") as HTMLSelectElement).options].map((o) => o.textContent);
    expect(options).toEqual(["项目", "商户", "酒吧"]);
  });

  it("所选营业日与范围已生成日结时显示其当前状态", async () => {
    setup({ user: MERCHANT, current: STEPS.slice(1, 2) });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    await pickDate("2026", "11月", "2");
    await waitFor(() => expect(screen.queryByTestId("daily-close-current-status")?.textContent ?? "").toContain("已确认"));
  });

  it("列表显示待确认、已确认、已锁定；待确认行确认、已确认行二次确认后锁定", async () => {
    const post = vi.fn().mockResolvedValue({ id: "d1", status: "confirmed" });
    const { getPaged } = setup({ rows: STEPS, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-d1")?.textContent ?? "").toContain("待确认"));
    expect(screen.getByTestId("daily-close-row-d2").textContent).toContain("已确认");
    expect(screen.getByTestId("daily-close-row-d3").textContent).toContain("已锁定");
    expect(screen.queryByTestId("daily-close-lock-d1")).toBeNull();
    expect(screen.queryByTestId("daily-close-confirm-d2")).toBeNull();
    expect(screen.queryByTestId("daily-close-confirm-d3")).toBeNull();
    expect(screen.queryByTestId("daily-close-lock-d3")).toBeNull();
    await waitFor(() => expect(screen.queryByTestId("daily-close-confirm-d1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-confirm-d1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d1/confirm",
      expect.objectContaining({ idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(getPaged.mock.calls.filter(([url]) => url === "/api/v1/finance/daily-closes?page=1&size=20")).toHaveLength(2));
    await waitFor(() => expect((screen.getByTestId("daily-close-lock-d2") as HTMLButtonElement).disabled).toBe(false));
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-lock-d2"));
    expect(confirm.mock.calls[0]?.[0]).toContain("日结锁定后不能直接补录影响金额，必须上级审批通过后补录。");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d2/lock",
      expect.objectContaining({ idempotencyKey: expect.any(String) })));
  });

  it("已锁定行出「归档」，二次确认文案为裁决 33 原句，提交到归档接口", async () => {
    const post = vi.fn().mockResolvedValue({ ...STEPS[2], status: "archived" });
    const { getPaged } = setup({ rows: STEPS, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-d3")?.textContent ?? "").toContain("已锁定"));
    expect(screen.queryByTestId("daily-close-archive-d1")).toBeNull();
    expect(screen.queryByTestId("daily-close-archive-d2")).toBeNull();
    await waitFor(() => expect((screen.queryByTestId("daily-close-archive-d3") as HTMLButtonElement | null)?.disabled).toBe(false));
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-archive-d3"));
    expect(confirm.mock.calls[0]?.[0]).toContain("归档后只读可查，锁定后补录照常走上级审批。");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d3/archive",
      expect.objectContaining({ idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(getPaged.mock.calls.filter(([url]) => url === "/api/v1/finance/daily-closes?page=1&size=20")).toHaveLength(2));
  });

  it("已归档行只读：显示「已归档」与归档时间，没有确认、锁定、归档与标记按钮，七源汇总照常可看", async () => {
    const { get } = setup({ rows: [ARCHIVED] });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-d4")?.textContent ?? "").toContain("已归档"));
    expect(screen.getByTestId("daily-close-row-d4").textContent).toContain("2026年12月25日 05:00");
    expect(screen.getByTestId("daily-close-table").textContent).toContain("归档时间");
    for (const action of ["confirm", "lock", "archive"]) {
      expect(screen.queryByTestId(`daily-close-${action}-d4`)).toBeNull();
    }
    fireEvent.click(screen.getByTestId("daily-close-open-d4"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-summary-table")?.textContent ?? "").toContain("订台定金"));
    expect(get).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d4/summaries");
    expect(screen.getByTestId("daily-close-final-marks").textContent).toContain("已标记为最终");
    expect(screen.queryByTestId("daily-close-final-mark-attendance")).toBeNull();
  });

  // 第十八批 R 段第 143 条 143-R6（ENUM:190「待生成、待确认、有差异……」；ENUM:115「有差异待复核」）：本营业日本范围还有未平账的
  // 收银班次时后端返回 has_difference；未平账的班次见证不了，确认必被班次交接闸拦，所以不出「确认」。
  it("有差异行显示「有差异」且不出「确认」，待确认行照常出「确认」", async () => {
    const differ = { id: "d5", businessDate: "2026-11-04", scopeType: "merchant", scopeId: "bar_a", status: "has_difference", lockedAt: null };
    setup({ rows: [differ, ...STEPS.slice(0, 1)] });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-d5")?.textContent ?? "").toContain("有差异"));
    expect(screen.getByTestId("daily-close-row-d5").textContent).not.toContain("has_difference");
    await waitFor(() => expect(screen.queryByTestId("daily-close-confirm-d1")).not.toBeNull());
    expect(screen.queryByTestId("daily-close-confirm-d5")).toBeNull();
  });

  it("确认被拒时显示后端原文", async () => {
    const post = vi.fn().mockRejectedValue(new Error("该营业日有待主管处置的酒吧账单异常，处置完毕后才能确认日结锁定"));
    setup({ rows: STEPS, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-confirm-d1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-confirm-d1"));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("该营业日有待主管处置的酒吧账单异常"));
  });

  it("列表显示五类标记数；展开待确认行列出五类原词，已标的类不出按钮，未标的类二次确认后标记为最终", async () => {
    const rows = [{ ...STEPS[0], finalMarkCategories: ["orders_verifications"] }, ...STEPS.slice(1)];
    const post = vi.fn().mockResolvedValue({ ...STEPS[0], finalMarkCategories: ["orders_verifications", "attendance"] });
    setup({ rows, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-d1")?.textContent ?? "").toContain("1/5"));
    fireEvent.click(screen.getByTestId("daily-close-open-d1"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-final-marks")?.textContent ?? "").toContain("当日考勤记录"));
    const text = screen.getByTestId("daily-close-final-marks").textContent ?? "";
    for (const name of ["当日订单与核销记录", "当日退款与冲正记录", "当日出品与物品交接记录", "当日考勤记录", "当日异常处置记录"]) {
      expect(text).toContain(name);
    }
    expect(text).toContain("已标记为最终");
    expect(screen.queryByTestId("daily-close-final-mark-orders_verifications")).toBeNull();
    await waitFor(() => expect((screen.getByTestId("daily-close-final-mark-attendance") as HTMLButtonElement).disabled).toBe(false));
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-final-mark-attendance"));
    expect(confirm.mock.calls[0]?.[0]).toContain("标记后不可撤销，事后发现错误只能走冲正，不得改回未最终。");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d1/final-marks",
      expect.objectContaining({ category: "attendance", idempotencyKey: expect.any(String) })));
  });

  it("已确认行展开后列出五类但不出标记按钮", async () => {
    setup({ rows: STEPS });
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-d2")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-d2"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-final-marks")?.textContent ?? "").toContain("当日考勤记录"));
    expect(screen.queryByTestId("daily-close-final-mark-attendance")).toBeNull();
    expect(screen.queryByTestId("daily-close-final-mark-orders_verifications")).toBeNull();
  });

  it("商户侧账号生成：范围对象固定为本账号商户、范围类型只有商户与酒吧，409 时显示原文", async () => {
    const post = vi.fn().mockRejectedValue(new Error("该营业日该范围已生成日结：日期 2026-11-02，范围 bar/bar_a"));
    const { get } = setup({ user: MERCHANT, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
    expect(screen.queryByTestId("daily-close-scope-bar")).toBeNull();
    const options = [...(screen.getByTestId("daily-close-scope-type") as HTMLSelectElement).options].map((o) => o.textContent);
    expect(options).toEqual(["商户", "酒吧"]);
    fireEvent.change(screen.getByTestId("daily-close-scope-type"), { target: { value: "bar" } });
    await waitFor(() => expect((screen.getByTestId("daily-close-scope-type") as HTMLSelectElement).value).toBe("bar"));
    await pickDate("2026", "11月", "2");
    await waitFor(() => expect(screen.queryByTestId("daily-close-current-status")?.textContent ?? "").toContain("待生成"));
    fireEvent.click(screen.getByTestId("daily-close-generate-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes",
      expect.objectContaining({ scopeType: "bar", scopeId: "bar_a" })));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("该营业日该范围已生成日结"));
    expect(get).not.toHaveBeenCalledWith("/api/v1/platform/merchants");
  });

  it("展开已锁定行出「本日结锁定的台账」与「锁定后补录」两张表；申请补录提交调整额（分，可负）与原因，待审批的补录可驳回", async () => {
    const post = vi.fn().mockResolvedValue({ id: "adj3", approvalStatus: "pending" });
    const { get, getPaged } = setup({ rows: STEPS, post, grants: BACKFILL });
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-d3")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-d3"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-locked-items")?.textContent ?? "").toContain("AR-0001"));
    expect(getPaged).toHaveBeenCalledWith("/api/v1/finance/ar-ap-items?locked_day_close_id=d3&page=1&size=200");
    expect(screen.getByTestId("daily-close-locked-items").textContent).toContain("应收");
    expect(screen.getByTestId("daily-close-locked-items").textContent).toContain("100.00");
    await waitFor(() => expect(screen.queryByTestId("daily-close-adjustments")?.textContent ?? "").toContain("漏记收入"));
    expect(get).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d3/adjustments");
    const text = screen.getByTestId("daily-close-adjustments").textContent ?? "";
    for (const value of ["财务甲", "财务乙", "2026年11月3日 05:00", "2026年11月1日", "5.00", "-3.00", "已通过", "待处理"]) {
      expect(text).toContain(value);
    }
    expect(screen.queryByTestId("daily-close-adjustment-reject-adj1")).toBeNull();
    await waitFor(() => expect((screen.getByTestId("daily-close-backfill-it1") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.change(screen.getByTestId("daily-close-backfill-amount-it1"), { target: { value: "-5" } });
    fireEvent.change(screen.getByTestId("daily-close-backfill-reason-it1"), { target: { value: "漏记冲减" } });
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("daily-close-backfill-it1"));
    expect(confirm.mock.calls[0]?.[0]).toContain("日结锁定后不能直接补录影响金额，必须上级审批通过后补录。");
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/ar-ap-adjustments", expect.objectContaining({
      arApItemId: "it1", adjustmentAmountCent: -500, reason: "漏记冲减", idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(get.mock.calls.filter(([url]) => url === "/api/v1/finance/daily-closes/d3/adjustments")).toHaveLength(2));
    await waitFor(() => expect((screen.getByTestId("daily-close-adjustment-reject-adj2") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("daily-close-adjustment-reject-adj2"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/ar-ap-adjustments/adj2/approve",
      expect.objectContaining({ decision: "reject", idempotencyKey: expect.any(String) })));
  });

  it("本日结锁定的台账方向列显示中文，长款营业外收入台账显示「内部核算」", async () => {
    setup({ rows: STEPS, grants: BACKFILL });
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-d3")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-d3"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-locked-items")?.textContent ?? "").toContain("IA-0001"));
    const text = screen.getByTestId("daily-close-locked-items").textContent ?? "";
    expect(text).toContain("应收");
    expect(text).toContain("内部核算");
    expect(text).not.toContain("internal_accounting");
  });

  // 143-R1：确认时后端按当前数据重算七源汇总（锁定不再重算）；已展开的行确认后重读汇总，锁定前看到的就是确认时的金额。
  it("已展开的待确认行确认后重读七源汇总，显示重算后的金额", async () => {
    const post = vi.fn().mockResolvedValue({ ...STEPS[0], status: "confirmed" });
    const { get } = setup({ rows: STEPS, post });
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-d1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-d1"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-summary-table")?.textContent ?? "").toContain("250.00"));
    get.mockImplementation(async (url: string) => (url.endsWith("/summaries")
      ? [{ id: "sm2", dailyCloseId: "d1", sourceStream: "bar", amountCent: 30000, refundCent: 500, orderCount: 4 }] : []));
    await waitFor(() => expect((screen.getByTestId("daily-close-confirm-d1") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("daily-close-confirm-d1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/finance/daily-closes/d1/confirm",
      expect.objectContaining({ idempotencyKey: expect.any(String) })));
    await waitFor(() => expect(screen.getByTestId("daily-close-summary-table").textContent).toContain("300.00"));
    expect(get.mock.calls.filter(([url]) => url === "/api/v1/finance/daily-closes/d1/summaries")).toHaveLength(2);
  });

  it("没有补录申请与审批权限：两张表照常显示，不出申请补录、批准、驳回；展开待确认行不出两张表", async () => {
    setup({ rows: STEPS, grants: ["finance.daily_close.manage", "finance.ar_ap.view"] });
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-d3")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-d3"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-adjustments")?.textContent ?? "").toContain("多记冲减"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-locked-items")?.textContent ?? "").toContain("AR-0001"));
    expect(screen.queryByTestId("daily-close-backfill-it1")).toBeNull();
    expect(screen.queryByTestId("daily-close-adjustment-approve-adj2")).toBeNull();
    expect(screen.queryByTestId("daily-close-adjustment-reject-adj2")).toBeNull();
    await waitFor(() => expect((screen.getByTestId("daily-close-open-d1") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("daily-close-open-d1"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-final-marks")?.textContent ?? "").toContain("当日考勤记录"));
    expect(screen.queryByTestId("daily-close-adjustments")).toBeNull();
    expect(screen.queryByTestId("daily-close-locked-items")).toBeNull();
  });

  it("已归档行展开后两张补录表照常显示，申请补录与审批照常可办", async () => {
    setup({ rows: [ARCHIVED], grants: BACKFILL });
    await waitFor(() => expect(screen.queryByTestId("daily-close-open-d4")).not.toBeNull());
    fireEvent.click(screen.getByTestId("daily-close-open-d4"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-adjustments")?.textContent ?? "").toContain("漏记收入"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-backfill-it1")).not.toBeNull());
    expect(screen.queryByTestId("daily-close-adjustment-approve-adj2")).not.toBeNull();
  });

  it("没有锁定权限不出生成表单与确认、锁定按钮，没有日结查看权限整块不渲染", async () => {
    const first = setup({ grants: ["finance.daily_close.manage"], rows: STEPS });
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-d1")).not.toBeNull());
    expect(screen.queryByTestId("daily-close-generate-form")).toBeNull();
    expect(screen.queryByTestId("daily-close-confirm-d1")).toBeNull();
    expect(screen.queryByTestId("daily-close-lock-d2")).toBeNull();
    expect(screen.queryByTestId("daily-close-archive-d3")).toBeNull();
    expect(first.post).not.toHaveBeenCalled();
    const view = setup({ grants: ["finance.daily_close.lock"] });
    await waitFor(() => expect(screen.getAllByTestId("finance-daily-close-panel")).toHaveLength(1));
    expect(view.getPaged).not.toHaveBeenCalled();
  });
});
