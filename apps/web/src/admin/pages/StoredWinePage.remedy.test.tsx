// 文件版本：V0.03
// FS-017（BSM L1072）：作废后对客补存/赔付——选中已作废行出现补存/赔付面板，选「赔付」填 120 元 → POST …/remedy 蛇形键 amount_cent=12000。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
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
      // 第十八批 AF 段 198-R5：补救登记服务端读 operator_note，本页备注框照发。
      expect.objectContaining({ remedy_type: "compensation", amount_cent: 12000, operator_note: expect.any(String) }),
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
