// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 O 段第 127 条 127-R3（BSM:940「收银、核销、收款、放行等现场动作，一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」；
// BSM:1066「现金（记收款人）」）：收款人由服务端取执行结账的那一次登录会话的操作人，结账表单不再手填现金收款人，提交体不带 cashierStaffId。
describe("酒吧结账收款人取登录人", () => {
  afterEach(() => vi.restoreAllMocks());

  it("选现金结账不出现现金收款人输入，提交体不含 cashierStaffId", async () => {
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
    await waitFor(() => expect(screen.getByLabelText("本次付款凭据编号")).toBeTruthy());
    fireEvent.change(screen.getByTestId("bar-settle-channel"), { target: { value: "cash" } });
    await waitFor(() => expect((screen.getByTestId("bar-settle-channel") as HTMLSelectElement).value).toBe("cash"));
    expect(screen.queryByText("现金收款人")).toBeNull();
    expect(screen.queryByTestId("bar-settle-cashier")).toBeNull();

    fireEvent.change(screen.getByLabelText("本次付款凭据编号"), { target: { value: "cash-1" } });
    await waitFor(() => expect((screen.getByLabelText("本次付款凭据编号") as HTMLInputElement).value).toBe("cash-1"));
    fireEvent.click(screen.getByTestId("bar-order-settle"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/settle",
      expect.objectContaining({ paymentChannel: "cash", paymentId: "cash-1" })));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("cashierStaffId");
  });
});
