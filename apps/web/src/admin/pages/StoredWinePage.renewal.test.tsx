// 文件版本：V0.01
// FS-025 存酒转存（延期）（ENUM L120 / BSM L1072）：员工代办转存向当前持有人发码（scene=renew）、按蛇形键提交 /renewals；
// 待复核的转存由当班营销主管在本页复核（/renewals/{id}/review，decision=approve|reject；驳回须填原因）。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
];

const PENDING_RENEWAL = {
  id: "rnw_1",
  savedBottleId: "w-1",
  extensionDays: 30,
  previousExpireAt: "2026-08-01T00:00:00Z",
  newExpireAt: "2026-08-31T00:00:00Z",
  renewalCount: 1,
  maxRenewals: 2,
  operatorId: "usr_staff",
  confirmationMethod: "sms",
  status: "pending_review",
  createdAt: "2026-07-20T00:00:00Z",
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/renewals") ? [PENDING_RENEWAL] : ROWS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({})),
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

describe("StoredWinePage 转存（延期）（FS-025）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("转存分组：发码 scene=renew，提交 → POST …/renewals 含 verification_code（蛇形键）", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-renewal-form")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.click(screen.getByTestId("stored-wine-renewal-send-code"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/verification-code",
      expect.objectContaining({ scene: "renew" }),
    ));
    fireEvent.change(screen.getByTestId("stored-wine-renewal-code"), { target: { value: "000000" } });
    fireEvent.click(screen.getByTestId("stored-wine-renewal"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/renewals",
      expect.objectContaining({ verification_code: "000000", idempotency_key: expect.any(String) }),
    ));
  });

  it("选中存酒后列出待复核转存，点「营销主管复核」→ POST …/renewals/rnw_1/review", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-select-w-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/renewals"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-renewal-row-rnw_1")).not.toBeNull());
    expect(screen.getByTestId("stored-wine-renewal-row-rnw_1").textContent).toContain("待营销主管复核");
    fireEvent.click(screen.getByTestId("stored-wine-renewal-review-rnw_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/renewals/rnw_1/review",
      expect.objectContaining({ idempotency_key: expect.any(String), decision: "approve" }),
    ));
  });

  it("营销主管驳回：填驳回原因后点「驳回代办转存」→ POST …/review decision=reject 带 reason；不填原因不发请求", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-select-w-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-renewal-reject-rnw_1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-renewal-reject-rnw_1"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-renewal-error")).not.toBeNull());
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("stored-wine-renewal-reject-reason-rnw_1"), { target: { value: "营销主管不认可" } });
    fireEvent.click(screen.getByTestId("stored-wine-renewal-reject-rnw_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/renewals/rnw_1/review",
      expect.objectContaining({ decision: "reject", reason: "营销主管不认可" }),
    ));
  });
});
