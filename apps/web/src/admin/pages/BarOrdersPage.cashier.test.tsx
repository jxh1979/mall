// 文件版本：V0.02
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

  // 第十八批 P 段第 133 条 133-R1（BSM:1081「收银员开班记备用金」）：订单列表为空时页面显示「暂无数据」，收银交班区块仍在、能开班。
  it("订单列表为空时收银交班区块仍渲染并能开班", async () => {
    const post = vi.fn().mockResolvedValue({ id: "shift-1", status: "open" });
    const api = {
      get: vi.fn(async (url: string) => (url === "/api/v1/platform/merchants"
        ? [{ id: "bar_a", merchantName: "一号酒吧", merchantType: "bar" }]
        : [])),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    render(<BarOrdersPage api={api} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()}
      effectivePermissions={{ status: "ready", value: { wildcard: true, grants: [], denies: [] } }} />);

    await waitFor(() => expect(screen.getByTestId("admin-empty").textContent).toContain("暂无数据"));
    await waitFor(() => expect(screen.queryByTestId("bar-shift-panel")).not.toBeNull());
    await waitFor(() => expect((screen.getByTestId("bar-shift-open-bar") as HTMLSelectElement).value).toBe("bar_a"));
    fireEvent.change(screen.getByLabelText("备用金"), { target: { value: "200" } });
    await waitFor(() => expect((screen.getByLabelText("备用金") as HTMLInputElement).value).toBe("200"));
    fireEvent.click(screen.getByTestId("bar-shift-open-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/shifts/open",
      expect.objectContaining({ barId: "bar_a", cashierUserId: "u1", openingCashCent: 20000 })));
  });
});
