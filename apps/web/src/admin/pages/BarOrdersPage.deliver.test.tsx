// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow } from "../types";

// 第十批 TK-26-R2（BSM L1025「账单上逐项列出物品名称与价格，并分别标注已上菜与未上菜」；BSM L1080 出品票据按场景）：
// 后台按明细勾选送达，只把勾选的未上菜明细发给 /deliver；已上菜明细不再发送。
const ORDERS: BarOrderRow[] = [
  {
    id: "ord-2", tableId: "T-2", totalAmountCent: 11000, status: "ordered", createdAt: "2026-07-05T10:00:00Z",
    items: [
      { id: "i1", productName: "啤酒", quantity: 2, amountCent: 6000, deliveryStatus: "pending" },
      { id: "i2", productName: "红酒", quantity: 1, amountCent: 5000, deliveryStatus: "pending" },
      { id: "i3", productName: "小吃", quantity: 1, amountCent: 0, deliveryStatus: "delivered" },
    ],
  },
];

describe("酒吧订单按明细勾选送达", () => {
  afterEach(() => vi.restoreAllMocks());

  it("默认勾选全部未上菜明细，取消一条后只提交勾选明细", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn(async (url: string) => (url === "/api/v1/bar/orders" ? ORDERS : [])),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
    };
    render(<BarOrdersPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-order-item-check-i1")).toBeTruthy());
    expect((screen.getByTestId("bar-order-item-check-i1") as HTMLInputElement).checked).toBe(true);
    expect((screen.getByTestId("bar-order-item-check-i2") as HTMLInputElement).checked).toBe(true);
    expect(screen.getByTestId("bar-order-item-row-i3").textContent).toContain("已上菜");
    expect(screen.getByTestId("bar-order-item-row-i1").textContent).toContain("未上菜");

    fireEvent.click(screen.getByTestId("bar-order-item-check-i2"));
    fireEvent.click(screen.getByTestId("bar-order-deliver"));

    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/orders/ord-2/deliver",
      expect.objectContaining({ deliveredItems: [{ orderItemId: "i1", quantity: 2 }] })));
    expect((post.mock.calls[0]?.[1] as { deliveredItems: unknown[] }).deliveredItems).toHaveLength(1);
  });
});
