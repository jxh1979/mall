// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十一批 TK-04-R2（BSM L822「POS、现金等其他付款方式，由收银在收款时现场录入会员号累计」；PGM L878 会员号即注册手机号）：
// 结账表单可选填会员手机号，随结账请求体 memberPhone 提交（adminClient.mapKeys 转蛇形 member_phone）。
describe("酒吧结账录入会员手机号", () => {
  afterEach(() => vi.restoreAllMocks());

  it("填会员手机号后结账请求体含 memberPhone，不填则不带", async () => {
    const post = vi.fn().mockResolvedValue({ id: "bill-1", settlementStatus: "settled" });
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
    fireEvent.change(screen.getByLabelText("本次付款凭据编号"), { target: { value: "pay-1" } });
    await waitFor(() => expect(screen.queryByLabelText("会员手机号（选填）")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("会员手机号（选填）"), { target: { value: " 13900001111 " } });
    fireEvent.click(screen.getByTestId("bar-order-settle"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/settle",
      expect.objectContaining({ memberPhone: "13900001111", paymentId: "pay-1" })));

    fireEvent.change(screen.getByLabelText("会员手机号（选填）"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("本次付款凭据编号"), { target: { value: "pay-2" } });
    fireEvent.click(screen.getByTestId("bar-order-settle"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2));
    expect(post.mock.calls[1]?.[1]).not.toHaveProperty("memberPhone");
  });
});
