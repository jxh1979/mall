// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 N 段第 120 条 120-R3（BSM:987「支持订台下单：选择酒吧、日期、时段、卡座/桌台/吧台位、人数和订台备注；订台需要支付定金。」）：
// 卡座与订台页的订台列表只读列出订台备注。
describe("订台列表的订台备注", () => {
  afterEach(() => vi.restoreAllMocks());
  it("订台列表有「订台备注」列，显示该订台的备注", async () => {
    const api = {
      get: vi.fn(async (url: string) => url === "/api/v1/bar/reservations"
        ? [{ id: "resv_remark_1", tableId: "table_1", guestCount: 2, depositStatus: "pending", status: "reserved", remark: "靠舞台，生日布置" }]
        : []),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = { api, user: { id: "manager", username: "店长", accountType: "staff" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("bar-reservation-row-resv_remark_1")).toBeTruthy());
    const header = screen.getByTestId("bar-reservations-table").querySelector("thead")?.textContent ?? "";
    expect(header).toContain("订台备注");
    expect(screen.getByTestId("bar-reservation-row-resv_remark_1").textContent ?? "").toContain("靠舞台，生日布置");
  });
});
