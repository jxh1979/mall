// 文件版本：V0.01
// FS-016 存酒持有人核验（BSM L1073 / L1198 / PGM L878）：领取与转让都向当前持有人发短信验证码；
// 转让表单按蛇形键提交；检索只按手机号走 POST lookup（手机号不进 URL）。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
  { id: "w-2", savedNo: "SW-0002", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "stored", createdAt: "2026-07-02" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async () => ROWS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async (path: string) => (path.endsWith("/lookup") ? [ROWS[1]] : {})),
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

describe("StoredWinePage 持有人核验（FS-016）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("领取表单不再写死手机号；点「向当前持有人发送验证码」→ POST …/verification-code {scene: claim}", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-claim-send-code")).not.toBeNull());
    expect((screen.getByTestId("stored-wine-claim-phone") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.click(screen.getByTestId("stored-wine-claim-send-code"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/verification-code",
      expect.objectContaining({ scene: "claim" }),
    ));
  });

  it("转让分组：发码 scene=transfer，提交转让 → POST …/transfer 含 to_name_or_nickname/to_phone/verification_code", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-transfer-form")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.click(screen.getByTestId("stored-wine-transfer-send-code"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/verification-code",
      expect.objectContaining({ scene: "transfer" }),
    ));
    fireEvent.change(screen.getByTestId("stored-wine-transfer-to-name"), { target: { value: "受让人乙" } });
    fireEvent.change(screen.getByTestId("stored-wine-transfer-to-phone"), { target: { value: "13900139000" } });
    fireEvent.change(screen.getByTestId("stored-wine-transfer-code"), { target: { value: "000000" } });
    fireEvent.click(screen.getByTestId("stored-wine-transfer"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/transfer",
      expect.objectContaining({ to_name_or_nickname: "受让人乙", to_phone: "13900139000", verification_code: "000000" }),
    ));
  });

  it("检索只按手机号：输入手机号点「检索」→ POST …/saved-bottles/lookup {phone}，表内只剩命中记录", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-search-submit")).not.toBeNull());
    fireEvent.change(screen.getByTestId("stored-wine-search"), { target: { value: "13900139000" } });
    fireEvent.click(screen.getByTestId("stored-wine-search-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/lookup",
      { phone: "13900139000" },
    ));
    await waitFor(() => {
      const table = screen.getByTestId("stored-wine-table").textContent ?? "";
      expect(table).toContain("SW-0002");
      expect(table).not.toContain("SW-0001");
    });
    expect(screen.getByTestId("stored-wine-search").getAttribute("placeholder")).not.toContain("客户");
  });
});
