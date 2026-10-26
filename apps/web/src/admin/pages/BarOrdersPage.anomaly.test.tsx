// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十六批第 16 条（ENUM「账单状态」异常；ENUM「异常状态的统一出口」；操作员裁决补充第 2 条「客人对账单提出争议时员工登记置异常。
// 之后都走主管「更正后恢复」或「作废并冲正」」）：订单页显示账单「异常」，有登记入口；异常账单只给有处置权限的主管两个出口，原因必填。
function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function renderPage(billStatus: string, grants: string[]) {
  const post = vi.fn().mockResolvedValue({});
  const api = {
    get: vi.fn().mockImplementation((url: string) => url === "/api/v1/bar/orders"
      ? Promise.resolve([{ id: "ord-1", tableId: "T-1", totalAmountCent: 30000, status: "billed", createdAt: "2026-10-26T10:00:00Z",
        bills: [{ id: "bill-1", status: billStatus }] }])
      : Promise.resolve([])),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "staff", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: perms(grants),
  };
  render(<BarOrdersPage {...props} />);
  return post;
}

afterEach(() => vi.restoreAllMocks());

describe("BarOrdersPage 账单争议与异常处置", () => {
  it("待结账单可登记争议，原因必填，按原因提交置异常", async () => {
    const post = renderPage("unpaid", ["bar.order.view", "bar.bill.adjust"]);
    await waitFor(() => expect(screen.queryByTestId("bar-bill-anomaly-raise")).not.toBeNull());
    await userEvent.click(screen.getByTestId("bar-order-select-ord-1"));
    expect(screen.getByTestId("bar-bill-anomaly-status").textContent).toContain("未支付");

    await userEvent.click(screen.getByTestId("bar-bill-anomaly-raise"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("原因"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("bar-bill-anomaly-reason"), "客人称未点第二瓶香槟");
    await userEvent.click(screen.getByTestId("bar-bill-anomaly-raise"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/anomaly", { reason: "客人称未点第二瓶香槟" }));
  });

  it("异常账单显示「异常」，主管二选一处置，原因为空不能提交，作废并冲正带 resolution 提交", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = renderPage("abnormal", ["bar.order.view", "bar.bill.anomaly.resolve"]);
    await waitFor(() => expect(screen.getByTestId("bar-orders-table").textContent).toContain("bill-1 / 异常"));
    await userEvent.click(screen.getByTestId("bar-order-select-ord-1"));
    await waitFor(() => expect(screen.queryByTestId("bar-bill-anomaly-status")?.textContent).toContain("异常"));
    expect(screen.queryByTestId("bar-bill-anomaly-raise")).toBeNull();

    await userEvent.click(screen.getByTestId("bar-bill-anomaly-voided_reversed"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("原因"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("bar-bill-anomaly-reason"), "整单录错卡座");
    await userEvent.click(screen.getByTestId("bar-bill-anomaly-voided_reversed"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/anomaly/resolve",
      { resolution: "voided_reversed", reason: "整单录错卡座" }));
    expect(screen.getByTestId("bar-bill-anomaly-restored").textContent).toContain("更正后恢复");
  });

  it("没有处置权限的员工看到异常，处置按钮不可用", async () => {
    renderPage("abnormal", ["bar.order.view", "bar.bill.adjust"]);
    await waitFor(() => expect(screen.getByTestId("bar-orders-table").textContent).toContain("bill-1 / 异常"));
    await userEvent.click(screen.getByTestId("bar-order-select-ord-1"));
    await waitFor(() => expect(screen.queryByTestId("bar-bill-anomaly-restored")).not.toBeNull());
    expect((screen.getByTestId("bar-bill-anomaly-restored") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("bar-bill-anomaly-voided_reversed") as HTMLButtonElement).disabled).toBe(true);
  });
});
