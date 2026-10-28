// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 C 段第 15 条（BSM:1468「经手员工的爽约率超过后台配置的上限时……自动停用其代订权限，由店长复核后恢复」）：
// 卡座与订台页列出代订权限停用中的经手员工，店长「复核恢复」二次确认后 POST /staff-booking-suspensions/{id}/restore。
describe("员工代订权限停用与店长复核恢复", () => {
  afterEach(() => vi.restoreAllMocks());
  it("列出停用中的经手员工，复核恢复提交恢复", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn(async (url: string) => url === "/api/v1/bar/staff-booking-suspensions"
        ? [{ id: "sus1", merchantId: "bar1", staffUserId: "staff-a", status: "suspended", statMonth: "2026-10", noShowCount: 2, bookingCount: 3, limitPct: 50, suspendedAt: "2026-10-20T12:00:00Z" }]
        : []),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const props: AdminPageProps = { api, user: { id: "manager", username: "店长", accountType: "staff" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("bar-staff-booking-suspension-sus1")).toBeTruthy());
    const row = screen.getByTestId("bar-staff-booking-suspension-sus1").textContent ?? "";
    expect(row).toContain("staff-a");
    expect(row).toContain("2026-10");
    expect(row).toContain("2/3");
    expect(row).toContain("50%");

    fireEvent.click(screen.getByTestId("bar-staff-booking-restore-sus1"));
    // 幂等键由后台请求客户端统一补上。
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/staff-booking-suspensions/sus1/restore", {}));
  });
});
