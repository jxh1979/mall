// 文件版本：V0.05
// FS-017（BSM L1072）：作废后对客补存/赔付——选中已作废行出现补存/赔付面板，选「赔付」填 120 元 → POST …/remedy 蛇形键 amount_cent=12000。
// 第十八批 AP 段 254-R3（BSM:1170「存酒登记必须拍照、填写人工备注……」）：同品补存须填补存瓶人工备注，作为 operator_note 发出；
// 赔付不发 operator_note；领取表单不再有备注框与「完整后台验收领取」默认值。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
  { id: "c-1", savedNo: "SW-0009", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "cancelled", createdAt: "2026-07-02" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/remedies") ? [] : ROWS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({ id: "sbr_1", customerDeliveryStatus: "pending_approval" })),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StoredWinePage {...props} />);
}

describe("StoredWinePage 补存/赔付（FS-017）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("选中已作废行 → 出现补存/赔付面板；选「赔付」填 120 元 → POST …/saved-bottles/c-1/remedy，remedy_type=compensation、amount_cent=12000", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    // 第 125 条：本页单选框 checked 回落首行，首行勾上不能当就绪信号；要等首行补存记录请求发出（选中已落定到 w-1）再点 c-1，
    // 否则补跑的选中 effect 会把选择改回 w-1、面板不出现。
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    expect(screen.queryByTestId("stored-wine-remedy-panel")).toBeNull();
    fireEvent.click(screen.getByTestId("stored-wine-select-c-1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-remedy-panel")).not.toBeNull());
    fireEvent.change(screen.getByTestId("stored-wine-remedy-type"), { target: { value: "compensation" } });
    fireEvent.change(screen.getByTestId("stored-wine-remedy-amount"), { target: { value: "120" } });
    fireEvent.click(screen.getByTestId("stored-wine-remedy-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/c-1/remedy",
      expect.objectContaining({ remedy_type: "compensation", amount_cent: 12000 }),
    ));
    // 254-R3：赔付不读备注，不发 operator_note。
    expect(vi.mocked(api.post).mock.calls.find(([path]) => path === "/api/v1/inventory/saved-bottles/c-1/remedy")![1])
      .not.toHaveProperty("operator_note");
  });

  it("254-R3：同品补存缺人工备注 → 提示且不发请求；填写后 operator_note 等于所填；领取表单无备注框与验收默认值", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    expect(screen.queryByTestId("stored-wine-claim-note")).toBeNull();
    expect(screen.getByTestId("stored-wine-page").textContent).not.toContain("完整后台验收领取");
    fireEvent.click(screen.getByTestId("stored-wine-select-c-1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-remedy-panel")).not.toBeNull());
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/c-1/remedies"));
    fireEvent.change(screen.getByTestId("stored-wine-remedy-photos"), { target: { value: "att_p1" } });
    fireEvent.change(screen.getByTestId("stored-wine-remedy-label"), { target: { value: "LBL-NEW" } });
    fireEvent.change(screen.getByTestId("stored-wine-remedy-location"), { target: { value: "store-1" } });
    fireEvent.click(screen.getByTestId("stored-wine-remedy-submit"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-remedy-error").textContent).toContain("人工备注"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(vi.mocked(api.post).mock.calls.filter(([path]) => path === "/api/v1/inventory/saved-bottles/c-1/remedy")).toHaveLength(0);

    fireEvent.change(screen.getByTestId("stored-wine-remedy-note"), { target: { value: "补存新瓶，客人确认" } });
    fireEvent.click(screen.getByTestId("stored-wine-remedy-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/c-1/remedy",
      expect.objectContaining({ remedy_type: "replacement", operator_note: "补存新瓶，客人确认", registration_photo_refs: ["att_p1"] }),
    ));
  });

  it("未作废行不出现补存/赔付面板", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-select-w-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-remedy-list")).not.toBeNull());
    expect(screen.queryByTestId("stored-wine-remedy-panel")).toBeNull();
  });
});

// 第十八批 AM 段第 241 条 241-R2、241-R6（BSM:1803；第十八批裁决 55、65「存酒对客赔付」、68）：已批准的赔付在登记赔款交付结案前先发起付款确认。
describe("StoredWinePage 存酒赔付付款确认", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("已批准的赔付行显示付款确认面板，发起路径为该赔付单的付款确认接口", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      let data: unknown = [];
      if (init?.method === "POST") {
        posts.push({ url, body: JSON.parse(String(init.body)) as Record<string, unknown> });
        data = { confirmation_id: "pc_w", status: "pending_review" };
      } else if (url.includes("/api/v1/finance/payment-confirmations?")) {
        data = { payment_type: "saved_bottle_compensation", payment_type_label: "存酒对客赔付", required_line_codes: ["finance_review", "legal_review"], confirmations: [] };
      } else if (url.includes("/saved-bottles/c-1/remedies")) {
        data = [{ id: "sbr_1", saved_bottle_id: "c-1", remedy_type: "compensation", amount_cent: 12000, bearer_type: "bar",
          customer_delivery_status: "approved", recovery_party_type: "none", recovery_status: "none" }];
      } else if (url.endsWith("/api/v1/inventory/saved-bottles")) {
        data = [
          { id: "w-1", saved_no: "SW-0001", customer_name: "客户甲", item_name: "威士忌", remaining_quantity: 2, status: "stored", created_at: "2026-07-01" },
          { id: "c-1", saved_no: "SW-0009", customer_name: "客户乙", item_name: "红酒", remaining_quantity: 1, status: "cancelled", created_at: "2026-07-02" },
        ];
      }
      return new Response(JSON.stringify({ success: true, code: "OK", data }));
    });
    vi.stubGlobal("fetch", fetchMock);
    renderPage(new AdminApi("test"));
    // 首行补存记录请求发出（选中已落定到 w-1）后再点 c-1，同上一用例。
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/saved-bottles/w-1/remedies"))).toBe(true), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("stored-wine-select-c-1"));
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-line-legal_review")).not.toBeNull());
    expect(screen.getByTestId("payment-confirmation-panel").textContent).toContain("存酒对客赔付");
    fireEvent.change(screen.getByTestId("payment-confirmation-reason"), { target: { value: "作废存酒对客赔款" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-final-approver"), { target: { value: "usr_boss" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-finance_review"), { target: { value: "usr_f" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-legal_review"), { target: { value: "usr_l" } });
    fireEvent.click(screen.getByTestId("payment-confirmation-submit"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/inventory/saved-bottles/remedies/sbr_1/payment-confirmations");
    expect(posts[0]!.body).toMatchObject({ reason: "作废存酒对客赔款", final_approver_id: "usr_boss" });
  });
});
