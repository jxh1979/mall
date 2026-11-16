// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PaymentConfirmationPanel } from "./PaymentConfirmationPanel";
import { AdminApi } from "../client/adminClient";
import type { EffectivePermissionsState } from "../types";

// 第十八批 AM 段第 241 条 241-R2（BSM:1803「发起时指定这条主线的最终审批人……财务、法务及相关部门作为平行的相关部门线，各指定该部门的最终审批人……
// 主线与各条平行线都通过才完成付款确认」；第十八批裁决 66「每类必带财务、法务两线，其他部门线由发起人按单另加」）：通用付款确认面板。
const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW = { object_type: "ar_ap_item", object_id: "ap-1", payment_type: "purchase_inbound", payment_type_label: "采购入库",
  required_line_codes: ["finance_review", "legal_review", "business_review"], confirmations: [] as unknown[] };
const item = (id: string, status: string) => ({ confirmation_id: id, amount_cent: 30000, status, review_id: `rev-${id}`, requested_by: "usr_fin",
  requested_at: "2026-11-16T02:00:00Z", decided_at: null });

function text(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

function mount(view: unknown, status = 200) {
  const posts: { url: string; body: Record<string, unknown> }[] = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body)) as Record<string, unknown> });
      return text({ success: true, code: "OK", message: "success", data: { confirmation_id: "pc_new", payment_type: "purchase_inbound",
        payment_type_label: "采购入库", object_type: "ar_ap_item", object_ids: ["ap-1", "ap-2"], amount_cent: 30000, status: "pending_review", review_id: "rev_new" } });
    }
    if (url.includes("/api/v1/finance/payment-confirmations?")) {
      return status === 200 ? text({ success: true, code: "OK", message: "success", data: view })
        : text({ success: false, code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "无权查看" }, status);
    }
    return text({ success: true, code: "OK", message: "success", data: [] });
  });
  vi.stubGlobal("fetch", fetchMock);
  const setActionResult = vi.fn();
  render(<PaymentConfirmationPanel api={new AdminApi("test-token")} effectivePermissions={PERMS} setActionResult={setActionResult}
    objectType="ar_ap_item" objectId="ap-1" createPath="/api/v1/finance/ar-ap-payment-confirmations" permission="finance.payment.register"
    extraBody={{ arApItemIds: ["ap-1", "ap-2"] }} summary="已勾选 2 笔" />);
  return { posts, setActionResult, fetchMock };
}

describe("PaymentConfirmationPanel 通用付款确认面板", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("按 GET 返回的必带线逐线填最终审批人；提交带主线最终审批人、各线与另加线、附加字段，路径为传入的发起路径", async () => {
    const { posts, setActionResult, fetchMock } = mount(VIEW);
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-line-business_review")).not.toBeNull());
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain("object_type=ar_ap_item&object_id=ap-1");
    const panel = screen.getByTestId("payment-confirmation-panel").textContent ?? "";
    expect(panel).toContain("采购入库");
    expect(panel).toContain("已勾选 2 笔");
    expect(panel).toContain("财务审核线");
    expect(panel).toContain("法务审核线");

    fireEvent.click(screen.getByTestId("payment-confirmation-submit"));
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-error")?.textContent ?? "").toContain("最终审批人"));
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("payment-confirmation-reason"), { target: { value: "十一月供应商货款" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-final-approver"), { target: { value: "usr_boss" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-finance_review"), { target: { value: "usr_f" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-legal_review"), { target: { value: "usr_l" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-business_review"), { target: { value: "usr_b" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-tax_review"), { target: { value: "usr_t" } });
    fireEvent.click(screen.getByTestId("payment-confirmation-submit"));

    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/finance/ar-ap-payment-confirmations");
    expect(posts[0]!.body).toMatchObject({
      reason: "十一月供应商货款",
      final_approver_id: "usr_boss",
      ar_ap_item_ids: ["ap-1", "ap-2"],
      parallel_lines: [
        { line_code: "finance_review", final_approver_id: "usr_f" },
        { line_code: "legal_review", final_approver_id: "usr_l" },
        { line_code: "business_review", final_approver_id: "usr_b" },
        { line_code: "tax_review", final_approver_id: "usr_t" },
      ],
    });
    expect(typeof posts[0]!.body.idempotency_key).toBe("string");
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "payment_confirmation_requested" })));
  });

  it("有待审批的确认时不显示发起表单，显示审批链链接", async () => {
    mount({ ...VIEW, confirmations: [item("pc_1", "pending_review")] });
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-row-pc_1")).not.toBeNull());
    const row = screen.getByTestId("payment-confirmation-row-pc_1");
    expect(row.textContent).toContain("300.00");
    expect(row.querySelector("a")?.getAttribute("href")).toContain("/admin/governance/approval-chain?review_id=rev-pc_1");
    expect(screen.queryByTestId("payment-confirmation-form")).toBeNull();
  });

  it("状态中文名逐键对齐 V629 status 闭集（待审批、已通过、已驳回）；只有已驳回的可重新发起", async () => {
    mount({ ...VIEW, confirmations: [item("pc_a", "approved"), item("pc_r", "rejected"), item("pc_p", "pending_review")] });
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-row-pc_p")).not.toBeNull());
    expect(screen.getByTestId("payment-confirmation-row-pc_a").textContent).toContain("已通过");
    expect(screen.getByTestId("payment-confirmation-row-pc_r").textContent).toContain("已驳回");
    expect(screen.getByTestId("payment-confirmation-row-pc_p").textContent).toContain("待审批");
    const table = screen.getByTestId("payment-confirmation-table").textContent ?? "";
    for (const code of ["approved", "rejected", "pending_review"]) expect(table).not.toContain(code);

    vi.unstubAllGlobals();
    mount({ ...VIEW, confirmations: [item("pc_x", "rejected")] });
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-row-pc_x")).not.toBeNull());
    expect(screen.queryByTestId("payment-confirmation-form")).not.toBeNull();
  });

  it("读不到（403）整块不显示", async () => {
    const { fetchMock } = mount(null, 403);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-panel")).toBeNull());
  });
});
