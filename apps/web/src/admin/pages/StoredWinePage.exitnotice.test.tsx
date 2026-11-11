// 文件版本：V0.02
// FS-017（BSM L1072 ③「退出前清点存酒并通知客人在原店取回」）：点清点通知 → POST …/saved-bottles/exit-notice {merchant_id}，并显示返回清点数。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/remedies") ? [] : ROWS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({ merchantId: "m_1", bottleCount: 2, notifiedCount: 2, quantityByItem: { item_1: 3 } })),
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

describe("StoredWinePage 退出清点通知（FS-017）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("填退出门店 id 点「清点并通知客人原店取回」→ POST …/exit-notice {merchant_id: m_1}，不含 operator_note，显示清点 2 瓶、通知 2 位", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-exit-notice")).not.toBeNull());
    fireEvent.change(screen.getByTestId("stored-wine-exit-merchant"), { target: { value: "m_1" } });
    fireEvent.click(screen.getByTestId("stored-wine-exit-notice"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/exit-notice",
      expect.objectContaining({ merchant_id: "m_1" }),
    ));
    // 第十八批 AF 段 198-R5：服务端不读 operator_note，请求体不再带（本页备注框只随补救登记提交）。
    expect(vi.mocked(api.post).mock.calls.find(([path]) => path === "/api/v1/inventory/saved-bottles/exit-notice")![1]).not.toHaveProperty("operator_note");
    await waitFor(() => {
      const text = screen.getByTestId("stored-wine-exit-result").textContent ?? "";
      expect(text).toContain("2 瓶");
      expect(text).toContain("2 位");
    });
  });
});
