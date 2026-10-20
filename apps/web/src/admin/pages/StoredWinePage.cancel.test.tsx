// 文件版本：V0.01
// FS-017（BSM L1072「存酒作废需照片+视频双证据+主管复核」）：作废表单缺视频 id 时 precheck 拦截并提示；填齐后 POST …/cancel 含双证据。
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
    post: vi.fn(async () => ({ id: "w-1", status: "disputed_frozen" })),
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

describe("StoredWinePage 作废（FS-017）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("缺视频 id 时 precheck 拦截并提示；填齐照片+视频后 POST …/cancel 含 photo_attachment_ids 与 video_attachment_ids", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-form")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: "酒瓶破损" } });
    fireEvent.change(screen.getByTestId("stored-wine-cancel-photos"), { target: { value: "att_photo" } });
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-error").textContent).toContain("视频"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(api.post).not.toHaveBeenCalledWith(expect.stringContaining("/cancel"), expect.anything());

    fireEvent.change(screen.getByTestId("stored-wine-cancel-videos"), { target: { value: "att_video" } });
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/cancel",
      expect.objectContaining({ reason: "酒瓶破损", photo_attachment_ids: ["att_photo"], video_attachment_ids: ["att_video"] }),
    ));
  });
});
