// 文件版本：V0.01
// FS-017（BSM L1072「对客实际交付结案与内部追收状态分别记录」）：补存/赔付记录同时展示对客「已结案」与追收「待收回」两列；
// 「追收已收回」→ POST …/remedies/{id}/recovery/settle。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRemedyRow, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "c-1", savedNo: "SW-0009", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "cancelled", createdAt: "2026-07-02" },
];
const REMEDIES: StoredWineRemedyRow[] = [
  { id: "sbr_1", savedBottleId: "c-1", remedyType: "compensation", amountCent: 12000, bearerType: "original_merchant",
    customerDeliveryStatus: "closed", recoveryPartyType: "staff", recoveryPartyId: "usr_staff_x", recoveryAmountCent: 12000, recoveryStatus: "pending" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/remedies") ? REMEDIES : ROWS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({ ...REMEDIES[0], recoveryStatus: "recovered" })),
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

describe("StoredWinePage 对客结案与内部追收分别记录（FS-017）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("记录行同时显示对客「已结案」与追收「待收回」；点「追收已收回」→ POST …/remedies/sbr_1/recovery/settle", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-select-c-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-c-1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-remedy-row-sbr_1")).not.toBeNull());
    expect(screen.getByTestId("stored-wine-remedy-delivery-sbr_1").textContent).toBe("已结案");
    expect(screen.getByTestId("stored-wine-remedy-recovery-sbr_1").textContent).toContain("待收回");
    fireEvent.change(screen.getByTestId("stored-wine-remedy-evidence"), { target: { value: "receipt_1" } });
    fireEvent.click(screen.getByTestId("stored-wine-remedy-recovery-settle-sbr_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/remedies/sbr_1/recovery/settle",
      expect.objectContaining({ evidence_ids: ["receipt_1"] }),
    ));
  });
});
