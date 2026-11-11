// 文件版本：V0.02
// FS-017（BSM L1072 ④）：门店退出交接——填退出门店与指定领取地点、逐瓶接收状态 → POST …/saved-bottles/handover 蛇形键
// items[0].condition='damaged'；交接后表格该行「保管方/领取地点」显示领取地点；清单只含退出门店的在存、未过期存酒（平台视图下别家酒吧不塞进 items）。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", barProjectId: "m_1", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
  { id: "w-2", savedNo: "SW-0002", barProjectId: "m_1", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "stored", createdAt: "2026-07-02" },
];
const HANDED_OVER: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", barProjectId: "m_1", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "disputed_frozen", createdAt: "2026-07-01", pickupLocation: "平台仓 A 区", custodianEntityId: "ent_platform" },
  { id: "w-2", savedNo: "SW-0002", barProjectId: "m_1", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "stored", createdAt: "2026-07-02", pickupLocation: "平台仓 A 区", custodianEntityId: "ent_platform" },
];
/** 平台视图：两家酒吧各有在存存酒，另有退出门店一瓶已过期——交接 items 只含退出门店 m_1 未过期的 w-1。 */
const ROWS_TWO_BARS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", barProjectId: "m_1", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01", expireAt: "2099-01-01T00:00:00Z" },
  { id: "w-2", savedNo: "SW-0002", barProjectId: "m_2", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "stored", createdAt: "2026-07-02", expireAt: "2099-01-01T00:00:00Z" },
  { id: "w-3", savedNo: "SW-0003", barProjectId: "m_1", customerName: "客户丙", itemName: "白兰地", remainingQuantity: 1, status: "stored", createdAt: "2026-07-03", expireAt: "2020-01-01T00:00:00Z" },
];

function makeApi(rows: StoredWineRow[] = ROWS): AdminApiClient {
  let handedOver = false;
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/remedies") ? [] : handedOver ? HANDED_OVER : rows)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async (path: string) => {
      if (path.endsWith("/handover")) {
        handedOver = true;
        return { id: "sbh_1", receivedIntact: 1, damagedOrShort: 1 };
      }
      return {};
    }),
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

describe("StoredWinePage 门店退出交接（FS-017）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("退出区块填领取地点与逐瓶状态 → POST …/handover body.items[0].condition==='damaged'，不含 operator_note；交接后表格显示领取地点", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-exit-block")).not.toBeNull());
    expect(screen.getByTestId("stored-wine-custody-w-1").textContent).toBe("原店");
    fireEvent.change(screen.getByTestId("stored-wine-exit-merchant"), { target: { value: "m_1" } });
    fireEvent.change(screen.getByTestId("stored-wine-handover-location"), { target: { value: "平台仓 A 区" } });
    fireEvent.change(screen.getByTestId("stored-wine-handover-photos"), { target: { value: "att_photo" } });
    fireEvent.change(screen.getByTestId("stored-wine-handover-videos"), { target: { value: "att_video" } });
    fireEvent.change(screen.getByTestId("stored-wine-handover-condition-w-1"), { target: { value: "damaged" } });
    fireEvent.change(screen.getByTestId("stored-wine-handover-qty-w-1"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("stored-wine-handover"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/handover",
      expect.objectContaining({
        merchant_id: "m_1",
        pickup_location: "平台仓 A 区",
        photo_attachment_ids: ["att_photo"],
        video_attachment_ids: ["att_video"],
        items: [
          { saved_bottle_id: "w-1", received_quantity: 0, condition: "damaged" },
          { saved_bottle_id: "w-2", received_quantity: 1, condition: "intact" },
        ],
      }),
    ));
    // 第十八批 AF 段 198-R5：服务端不读 operator_note，请求体不再带（本页备注框只随补救登记提交）。
    expect(vi.mocked(api.post).mock.calls.find(([path]) => path === "/api/v1/inventory/saved-bottles/handover")![1]).not.toHaveProperty("operator_note");
    await waitFor(() => expect(screen.getByTestId("stored-wine-custody-w-2").textContent).toBe("平台仓 A 区"));
  });

  it("两家酒吧各有在存存酒：填退出门店 m_1 后清单与提交的 items 只含 m_1 未过期的瓶子", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi(ROWS_TWO_BARS);
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-exit-block")).not.toBeNull());
    fireEvent.change(screen.getByTestId("stored-wine-exit-merchant"), { target: { value: "m_1" } });
    fireEvent.change(screen.getByTestId("stored-wine-handover-location"), { target: { value: "平台仓 A 区" } });
    await waitFor(() => expect(screen.queryByTestId("stored-wine-handover-item-w-2")).toBeNull());
    expect(screen.queryByTestId("stored-wine-handover-item-w-3")).toBeNull();
    expect(screen.queryByTestId("stored-wine-handover-item-w-1")).not.toBeNull();
    fireEvent.click(screen.getByTestId("stored-wine-handover"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/handover",
      expect.objectContaining({
        merchant_id: "m_1",
        items: [{ saved_bottle_id: "w-1", received_quantity: 2, condition: "intact" }],
      }),
    ));
  });
});
