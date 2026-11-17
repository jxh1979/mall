// 文件版本：V0.01
// 第十八批 AP 段第 254 条 254-R5（BSM:1073「……须同时满足四项：①由店长发起并经另一名主管复核，普通员工不得单独放酒；……③由该客户对应的营销人员
// 确认身份与取酒申请；④只允许存酒客户本人到场办理，不允许他人代领。……」）：选中存酒后按瓶列出人工例外取酒申请，按状态只给一个动作
// （已发起→另一主管复核、已复核→营销确认、营销已确认→本人到场放酒）；发起原因必填、每次发起幂等键不同；放酒须勾本人到场，数量键 quantity。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import { formatActionResult } from "../components/ActionResultView";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
];

const PICKUPS = [
  { id: "mp-1", savedBottleId: "w-1", status: "initiated", historyCondition: "orders_within_1m", reason: "客人手机遗失", initiatedBy: "usr_mgr", initiatedAt: "2026-07-20T02:00:00Z" },
  { id: "mp-2", savedBottleId: "w-1", status: "reviewed", historyCondition: "orders_within_3m", reason: "客人手机停机", initiatedBy: "usr_mgr", initiatedAt: "2026-07-19T02:00:00Z", reviewedBy: "usr_sup", reviewedAt: "2026-07-19T03:00:00Z" },
  { id: "mp-3", savedBottleId: "w-1", status: "marketing_confirmed", historyCondition: "orders_total", reason: "客人手机遗失", initiatedBy: "usr_mgr", initiatedAt: "2026-07-18T02:00:00Z", reviewedBy: "usr_sup", marketingConfirmedBy: "usr_mkt" },
  { id: "mp-4", savedBottleId: "w-1", status: "dispensed", historyCondition: "orders_total", reason: "客人手机遗失", initiatedBy: "usr_mgr", initiatedAt: "2026-07-17T02:00:00Z", dispensedBy: "usr_clerk" },
];

function makeApi(post = vi.fn(async () => ({}))): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/manual-pickups") ? PICKUPS
      : path.endsWith("/remedies") || path.endsWith("/renewals") ? [] : ROWS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "店长", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StoredWinePage {...props} />);
}

function postsTo(api: AdminApiClient, path: string) {
  return vi.mocked(api.post).mock.calls.filter(([called]) => called === path);
}

// 首行回落选中后等该瓶申请读回、列表落定，再点按钮。
async function waitPickups(api: AdminApiClient) {
  await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/manual-pickups"), { timeout: 5000 });
  await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-row-mp-4")).not.toBeNull());
}

describe("StoredWinePage 人工例外取酒（第十八批 AP 段 254-R5）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("按瓶列出申请：各状态只出对应的一个动作，命中条件与状态显示中文", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-row-mp-1")).not.toBeNull(), { timeout: 5000 });
    await waitPickups(api);

    expect(screen.getByTestId("stored-wine-manual-pickup-row-mp-1").textContent).toContain("近 1 个月笔数达标");
    expect(screen.getByTestId("stored-wine-manual-pickup-row-mp-1").textContent).toContain("已发起");
    expect(screen.queryByTestId("stored-wine-manual-pickup-review-mp-1")).not.toBeNull();
    expect(screen.queryByTestId("stored-wine-manual-pickup-confirm-marketing-mp-1")).toBeNull();
    expect(screen.queryByTestId("stored-wine-manual-pickup-dispense-mp-1")).toBeNull();

    expect(screen.queryByTestId("stored-wine-manual-pickup-review-mp-2")).toBeNull();
    expect(screen.queryByTestId("stored-wine-manual-pickup-confirm-marketing-mp-2")).not.toBeNull();
    expect(screen.queryByTestId("stored-wine-manual-pickup-dispense-mp-2")).toBeNull();

    expect(screen.queryByTestId("stored-wine-manual-pickup-review-mp-3")).toBeNull();
    expect(screen.queryByTestId("stored-wine-manual-pickup-confirm-marketing-mp-3")).toBeNull();
    expect(screen.queryByTestId("stored-wine-manual-pickup-dispense-mp-3")).not.toBeNull();

    expect(screen.getByTestId("stored-wine-manual-pickup-row-mp-4").querySelector("button")).toBeNull();
    expect(screen.getByTestId("stored-wine-manual-pickup-row-mp-4").textContent).toContain("累计笔数达标");
  }, 10_000);

  it("发起：原因为空不发请求并提示；填写后 POST …/manual-pickups 只带 idempotency_key 与 reason，两次发起幂等键不同", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    let now = 1_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1));
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-initiate")).not.toBeNull(), { timeout: 5000 });
    await waitPickups(api);

    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-initiate"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-manual-pickup-error").textContent).toContain("发起人工例外取酒须填写原因"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/manual-pickups")).toHaveLength(0);

    fireEvent.change(screen.getByTestId("stored-wine-manual-pickup-reason"), { target: { value: "客人手机遗失" } });
    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-initiate"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/manual-pickups")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/manual-pickups")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-manual-pickup-w-1-/),
      reason: "客人手机遗失",
    });

    await waitFor(() => expect((screen.getByTestId("stored-wine-manual-pickup-initiate") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.change(screen.getByTestId("stored-wine-manual-pickup-reason"), { target: { value: "客人手机停机" } });
    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-initiate"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/manual-pickups")).toHaveLength(2));
    const [first, second] = postsTo(api, "/api/v1/inventory/saved-bottles/w-1/manual-pickups")
      .map(([, body]) => (body as { idempotency_key: string }).idempotency_key);
    expect(second).not.toBe(first);
  }, 10_000);

  it("复核、营销确认只带 idempotency_key；放酒须勾本人到场，请求体 claim_items 用 quantity 键、present_in_person 为 true", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-dispense-mp-3")).not.toBeNull(), { timeout: 5000 });
    await waitPickups(api);

    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-review-mp-1"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-1/review")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-1/review")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-manual-pickup-review-mp-1-/),
    });

    await waitFor(() => expect((screen.getByTestId("stored-wine-manual-pickup-confirm-marketing-mp-2") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-confirm-marketing-mp-2"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-2/confirm-marketing")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-2/confirm-marketing")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-manual-pickup-confirm-marketing-mp-2-/),
    });

    await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-in-person-mp-3")).not.toBeNull());
    expect((screen.getByTestId("stored-wine-manual-pickup-dispense-mp-3") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-dispense-mp-3"));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-3/dispense")).toHaveLength(0);

    fireEvent.change(screen.getByTestId("stored-wine-manual-pickup-quantity-mp-3"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-in-person-mp-3"));
    await waitFor(() => expect((screen.getByTestId("stored-wine-manual-pickup-dispense-mp-3") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-dispense-mp-3"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-3/dispense")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/manual-pickups/mp-3/dispense")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-manual-pickup-dispense-mp-3-/),
      present_in_person: true,
      claim_items: [{ saved_bottle_id: "w-1", quantity: 2 }],
    });
  }, 10_000);

  it("服务端拒绝（如自审）原文上屏", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi(vi.fn(async () => { throw new Error("操作冲突：人工例外取酒须由另一名主管复核"); }));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-review-mp-1")).not.toBeNull(), { timeout: 5000 });
    await waitPickups(api);

    fireEvent.click(screen.getByTestId("stored-wine-manual-pickup-review-mp-1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-manual-pickup-error")?.textContent ?? "").toContain("人工例外取酒须由另一名主管复核"));
  }, 10_000);
});

// 18AP 收尾复核修正（PGM:287「全局原则 · 界面全中文（所有终端零英文）……枚举取值……」）：本段新动作的回包与请求体进结果区，
// 取值（验证码用途、命中的消费历史条件、营销确认依据、业务参数键）与字段名（客户本人到场）都显示中文。
describe("存酒新动作结果区全中文（18AP 收尾复核修正）", () => {
  it("发隐私同意验证码、人工例外取酒发起与营销确认与放酒、保存有效期与转存参数：不出英文码，也不出「其他信息」", () => {
    const pickup = { id: "mpk_1", savedBottleId: "w-1", reason: "客人手机遗失", initiatedBy: "usr_mgr", status: "initiated" };
    const results = [
      { action: "stored_wine_deposit_consent_code_sent", requestBody: { idempotency_key: "k1", customer_phone: "13800138000" },
        result: { savedBottleId: null, scene: "deposit", requestedAt: "2026-07-20T02:00:00Z" } },
      ...["orders_within_1m", "orders_within_3m", "orders_total"].map((historyCondition) => ({
        action: "stored_wine_manual_pickup_initiated", savedBottleId: "w-1", requestBody: { idempotency_key: "k2", reason: "客人手机遗失" },
        result: { ...pickup, historyCondition } })),
      ...["attributed_marketer", "marketer_supervisor", "on_duty_marketing_supervisor"].map((marketingConfirmBasis) => ({
        action: "stored_wine_manual_pickup_marketing_confirmed", savedBottleId: "w-1", requestBody: { idempotency_key: "k3" },
        result: { ...pickup, historyCondition: "orders_total", marketingConfirmBasis, status: "marketing_confirmed" } })),
      { action: "stored_wine_manual_pickup_dispensed", savedBottleId: "w-1",
        requestBody: { idempotency_key: "k4", present_in_person: true, claim_items: [{ saved_bottle_id: "w-1", quantity: 1 }] },
        result: { ...pickup, historyCondition: "orders_total", marketingConfirmBasis: "attributed_marketer", claimId: "claim_1", status: "dispensed" } },
      ...["validity_days", "max_renewals", "extension_days"].map((policyKey) => ({
        action: "saved_bottle_renewal_policy_saved", result: { policyKey, scopeId: "project:prj_1", numericValue: 30, minValue: 7, maxValue: 90, version: 2 } })),
    ];
    const text = results.map((result) => formatActionResult(result)).join("\n");
    for (const code of ["deposit", "orders_within_1m", "orders_within_3m", "orders_total", "attributed_marketer", "marketer_supervisor",
      "on_duty_marketing_supervisor", "validity_days", "max_renewals", "extension_days", "其他信息"]) {
      expect(text).not.toContain(code);
    }
    expect(text).toContain("验证码用途：存酒登记隐私同意");
    expect(text).toContain("历史条件：近 1 个月笔数达标");
    expect(text).toContain("历史条件：累计笔数达标");
    expect(text).toContain("营销确认依据：该客户对应的营销人员确认");
    expect(text).toContain("营销确认依据：无归属营销，由当班营销主管确认");
    expect(text).toContain("客户本人到场：是");
    expect(text).toContain("业务参数键：存酒有效期天数（按项目）");
    expect(text).toContain("业务参数键：转存顺延天数（按酒吧）");
  });
});
