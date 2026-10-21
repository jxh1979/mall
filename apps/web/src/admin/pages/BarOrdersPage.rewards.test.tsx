// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十一批 TK-04-R5（ENUM L77「从消费单据可以反查该单产生的积分与成长值记录」）：账单明细显示本单商户消费积分与成长值结果。
describe("酒吧账单反查本单消费奖励", () => {
  afterEach(() => vi.restoreAllMocks());

  it("点订单行 → 明细显示本单积分、成长值与确认状态", async () => {
    const get = vi.fn(async (url: string) => {
      if (url === "/api/v1/bar/orders") {
        return [{ id: "ord-1", tableId: "T-1", totalAmountCent: 30000, status: "paid", createdAt: "2026-10-20T10:00:00Z", bills: [{ id: "bill-1", status: "settled" }] }];
      }
      if (url === "/api/v1/bar/bills/bill-1/rewards") return [{ id: "mcr1", points: 15, growth: 150, status: "pending" }];
      return [];
    });
    const api = {
      get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
    };
    render(<BarOrdersPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-order-row-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-order-row-ord-1"));
    await waitFor(() => expect(screen.getByTestId("row-detail-drawer").textContent).toContain("本单消费奖励15 积分 · 150 成长值 · 待确认"));
    expect(get).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/rewards");
  });
});
