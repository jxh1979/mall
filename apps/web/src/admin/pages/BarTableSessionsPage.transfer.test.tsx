// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

describe("转台明确目标当班服务人员", () => {
  afterEach(() => vi.restoreAllMocks());
  it("未指定接手人不提交，填写后随真实场次版本提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn(async (url: string) => url === "/api/v1/bar/table-sessions"
        ? [{ id: "session-1", tableId: "old-table", barId: "bar-1", status: "open", sessionVersion: 3, serviceStaffId: "old-staff" }]
        : []),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = { api, user: { id: "operator", username: "主管", accountType: "staff" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarTableSessionsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("bar-session-select-session-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-transfer-target-table"), { target: { value: "next-table" } });
    fireEvent.click(screen.getByTestId("bar-transfer-submit"));
    expect(screen.getByTestId("bar-checkin-error").textContent).toContain("实际当班接手人");
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("bar-transfer-target-staff"), { target: { value: "next-staff" } });
    fireEvent.click(screen.getByTestId("bar-transfer-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/session-1/transfer",
      expect.objectContaining({ targetTableId: "next-table", targetServiceStaffId: "next-staff", sessionVersion: 3 })));
  });
});
