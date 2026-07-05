import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 后端 /saved-bottles 实际字段（含 expireAt/updatedAt/remainingQuantity），页级抽屉读取。
const ROWS = [
  { id: "sb-1", savedNo: "SB-1", customerName: "王先生", itemName: "茅台", quantity: 2, remainingQuantity: 2, unit: "瓶", status: "stored", createdAt: "2026-07-01T09:00:00Z", updatedAt: "2026-07-03T18:00:00Z", expireAt: "2026-12-31T00:00:00Z" },
  { id: "sb-2", savedNo: "SB-2", customerName: "李女士", itemName: "红酒", quantity: 1, status: "consumed", createdAt: "2026-06-01T09:00:00Z" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(ROWS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(): void {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StoredWinePage {...props} />);
}

describe("StoredWinePage 行明细抽屉 + 零英文", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/inventory/stored-wine");
  });

  it("点存酒行 → 抽屉含状态（中文）+ 存/取/到期时间（中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-row-sb-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("stored-wine-row-sb-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("存酒中");
    // 存酒/取酒更新/到期三时间全中文
    expect(drawer.textContent).toContain("存酒时间");
    expect(drawer.textContent).toContain("到期时间");
    expect(drawer.textContent).toContain("2026年7月1日");
    expect(drawer.textContent).toContain("2026年12月31日");
    expect(drawer.textContent).not.toContain("stored");
    expect(drawer.textContent).not.toContain("2026-07-01T");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-select-sb-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("stored-wine-select-sb-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("表格时间列渲染中文（无 ISO 时间戳漏出）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-table")).toBeTruthy());
    const table = screen.getByTestId("stored-wine-table").textContent ?? "";
    expect(table).toContain("2026年7月1日");
    expect(table).not.toContain("2026-07-01T");
    expect(table).not.toContain("consumed");
  });
});
