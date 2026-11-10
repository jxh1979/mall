// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { ApiError } from "../../shared/apiError";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AA 段第 202 条 202-R2（BSM:1069「反结（重开已结账单）、沽清（卖完、临时下架）均需主管审批留痕」；第十八批裁决 39）：
// 订单页反结入口——只对已结账账单、持 bar.bill.reopen 可点，原因必填；首次提交被拦下送主管复核，批准后原样再点一次。
function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function renderPage(billStatus: string, grants: string[], post = vi.fn().mockResolvedValue({})) {
  const get = vi.fn().mockImplementation((url: string) => url === "/api/v1/bar/orders"
    ? Promise.resolve([{ id: "ord-1", tableId: "T-1", totalAmountCent: 30000, status: "paid", createdAt: "2026-11-06T10:00:00Z",
      bills: [{ id: "bill-1", status: billStatus }] }])
    : Promise.resolve([]));
  const api = {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "staff", accountType: "staff" },
    setActionResult, setGlobalError: vi.fn(), effectivePermissions: perms(grants),
  };
  render(<BarOrdersPage {...props} />);
  return { get, post, setActionResult };
}

async function selectOrder() {
  await waitFor(() => expect(screen.queryByTestId("bar-bill-reopen")).not.toBeNull());
  await userEvent.click(screen.getByTestId("bar-order-select-ord-1"));
  await waitFor(() => expect(screen.getByTestId("bar-bill-reopen-status").textContent).toContain("bill-1"));
}

afterEach(() => vi.restoreAllMocks());

describe("BarOrdersPage 反结（重开已结账单）", () => {
  it("没有 bar.bill.reopen 权限时反结按钮不可用", async () => {
    renderPage("paid", ["bar.order.view"]);
    await selectOrder();
    expect((screen.getByTestId("bar-bill-reopen") as HTMLButtonElement).disabled).toBe(true);
  });

  it("选中未结账账单时反结按钮不可用", async () => {
    renderPage("unpaid", ["bar.order.view", "bar.bill.reopen"]);
    await selectOrder();
    expect((screen.getByTestId("bar-bill-reopen") as HTMLButtonElement).disabled).toBe(true);
  });

  it("已结账账单原因为空时不发请求并提示原因必填；填好原因确认后按原因提交", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { post } = renderPage("paid", ["bar.order.view", "bar.bill.reopen"]);
    await selectOrder();
    expect((screen.getByTestId("bar-bill-reopen") as HTMLButtonElement).disabled).toBe(false);

    await userEvent.click(screen.getByTestId("bar-bill-reopen"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("原因"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("bar-bill-reopen-reason"), "漏记一瓶香槟");
    await userEvent.click(screen.getByTestId("bar-bill-reopen"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/reopen", { reason: "漏记一瓶香槟" }));
  });

  it("首次提交被拦下送主管复核时，显示「已提交主管审批」与「原样再点一次」，原因保留原文", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = vi.fn().mockRejectedValue(new ApiError("操作已被拦截，等待主管复核，复核单号：review_1",
      { detail: { status: 409 } as never, code: "STATE_CONFLICT", detailCode: "ADMISSION_BLOCKED" }));
    renderPage("paid", ["bar.order.view", "bar.bill.reopen"], post);
    await selectOrder();

    await userEvent.type(screen.getByTestId("bar-bill-reopen-reason"), "漏记一瓶香槟");
    await userEvent.click(screen.getByTestId("bar-bill-reopen"));
    await waitFor(() => expect(screen.queryByTestId("bar-bill-reopen-note")?.textContent).toContain("已提交主管审批"));
    expect(screen.getByTestId("bar-bill-reopen-note").textContent).toContain("原样再点一次");
    expect(screen.getByTestId("bar-orders-error").textContent).toContain("review_1");
    expect((screen.getByTestId("bar-bill-reopen-reason") as HTMLInputElement).value).toBe("漏记一瓶香槟");
  });

  it("反结成功后重新拉订单列表，结果区动作为「酒吧账单已反结」", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { get, post, setActionResult } = renderPage("paid", ["bar.order.view", "bar.bill.reopen"]);
    await selectOrder();
    const before = get.mock.calls.filter(([url]) => url === "/api/v1/bar/orders").length;

    await userEvent.type(screen.getByTestId("bar-bill-reopen-reason"), "漏记一瓶香槟");
    await userEvent.click(screen.getByTestId("bar-bill-reopen"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_bill_reopened", orderId: "ord-1" })));
    expect(post).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(get.mock.calls.filter(([url]) => url === "/api/v1/bar/orders").length).toBe(before + 1));
    expect(VALUE_LABELS.bar_bill_reopened).toBe("酒吧账单已反结");
  });
});
