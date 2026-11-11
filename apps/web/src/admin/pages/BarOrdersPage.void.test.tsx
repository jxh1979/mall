// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 AF 段第 198 条 198-R5（BarOrderService.voidOrderItem 读明细、原因、原因码、证据、回库与账单，不读 operator_note）：
// 发起退菜退酒的请求体不再带写死的 operatorNote。
describe("酒吧发起退菜退酒请求体", () => {
  afterEach(() => vi.restoreAllMocks());

  it("发起退菜退酒请求体不含 operatorNote", async () => {
    const post = vi.fn().mockResolvedValue({ id: "void-1", status: "pending_review" });
    const api = {
      get: vi.fn(async (url: string) => (url === "/api/v1/bar/orders"
        ? [{ id: "ord-1", tableId: "T-1", totalAmountCent: 30000, status: "ordered", createdAt: "2026-07-05T10:00:00Z", bills: [{ id: "bill-1", status: "unsettled" }] }]
        : [])),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarOrdersPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-order-select-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-order-select-ord-1"));
    await waitFor(() => expect(screen.getByLabelText("本次付款凭据编号")).toBeTruthy());
    fireEvent.change(screen.getByTestId("bar-void-order-item-id"), { target: { value: "item-1" } });
    fireEvent.change(screen.getByTestId("bar-void-reason"), { target: { value: "客人反馈变质" } });
    await waitFor(() => expect((screen.getByTestId("bar-void-reason") as HTMLInputElement).value).toBe("客人反馈变质"));
    fireEvent.click(screen.getByTestId("bar-void-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/orders/ord-1/void-item",
      expect.objectContaining({ orderItemId: "item-1", reason: "客人反馈变质" })));
    const body = post.mock.calls.find(([path]) => path === "/api/v1/bar/orders/ord-1/void-item")![1];
    expect(Object.keys(body as object)).not.toContain("operatorNote");
  });
});
