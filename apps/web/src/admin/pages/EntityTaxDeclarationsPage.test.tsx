// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { EntityTaxDeclarationsPage } from "./EntityTaxDeclarationsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AC 段第 210 条 210-R2（第十八批裁决 42：财务按公司、税种、所属期手工登记「实际申报金额」，经财务、税务、法务审核并由顶层平台公司老板
// 最终确认后生效）：税费申报登记页列表中文显示，登记弹窗按蛇形键提交。
const ROWS = [
  { id: "etd_1", business_entity_id: "be_1", entity_name: "甲酒吧公司", tax_type: "vat", period_start_month: "2026-01", period_end_month: "2026-03",
    declared_amount_cent: 1234567, reason: "一季度增值税", evidence_attachment_ids: [], status: "pending_review", review_id: "review_1",
    created_by: "usr_fin", created_at: "2026-04-10T02:00:00Z", approved_at: null },
];

function text(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200 });
}

let lastPost: { url: string; body: Record<string, unknown> } | null = null;

function renderPage(setActionResult: AdminPageProps["setActionResult"] = vi.fn()) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      lastPost = { url, body: JSON.parse(String(init?.body)) };
      return Promise.resolve(text({ ...ROWS[0], id: "etd_new", tax_type: "cultural_fee" }));
    }
    if (url.includes("/platform/business-entities")) {
      return Promise.resolve(text({ items: [{ id: "be_1", entity_name: "甲酒吧公司", entity_role: "management_company", status: "active" }], page: 1, page_size: 500, total: 1 }));
    }
    if (url.includes("/finance/entity-taxes")) return Promise.resolve(text({ items: ROWS, page: 1, page_size: 20, total: ROWS.length }));
    return Promise.resolve(text([]));
  }));
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
  render(<EntityTaxDeclarationsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={setActionResult} setGlobalError={vi.fn()} effectivePermissions={permissions} />);
}

describe("EntityTaxDeclarationsPage 税费申报登记", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
  });

  it("列表的税种与状态显示中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("entity-tax-row-etd_1")?.textContent).toContain("增值税"));
    const row = screen.getByTestId("entity-tax-row-etd_1").textContent ?? "";
    expect(row).toContain("甲酒吧公司");
    expect(row).toContain("2026-01～2026-03");
    expect(row).toContain("12,345.67");
    expect(row).toContain("待财务、税务、法务审核与最高确认");
    expect(row).not.toContain("vat");
    expect(row).not.toContain("pending_review");
  });

  it("登记弹窗按蛇形键提交（金额元转分，三条线最终审批人）", async () => {
    const setActionResult = vi.fn();
    renderPage(setActionResult);
    await waitFor(() => expect(screen.queryByTestId("entity-tax-row-etd_1")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.click(screen.getByTestId("entity-tax-create-open"));

    fireEvent.change(screen.getByTestId("entity-tax-create-entity"), { target: { value: "be_1" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-type"), { target: { value: "cultural_fee" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-start"), { target: { value: "2026-04" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-end"), { target: { value: "2026-06" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-amount"), { target: { value: "1200.50" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-reason"), { target: { value: "二季度文化事业建设费申报表" } });
    fireEvent.click(screen.getByTestId("entity-tax-create-guided-next"));
    expect(screen.getByTestId("entity-tax-create-guided-error").textContent).toContain("最终审批人");

    fireEvent.change(screen.getByTestId("entity-tax-create-finance_review"), { target: { value: "usr_f" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-tax_review"), { target: { value: "usr_t" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-legal_review"), { target: { value: "usr_l" } });
    fireEvent.click(screen.getByTestId("entity-tax-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-create-guided-summary").textContent).toContain("文化事业建设费"));
    fireEvent.click(screen.getByTestId("entity-tax-create-save"));

    await waitFor(() => expect(lastPost?.url).toContain("/api/v1/finance/entity-taxes"));
    expect(lastPost?.body).toMatchObject({
      business_entity_id: "be_1",
      tax_type: "cultural_fee",
      period_start_month: "2026-04",
      period_end_month: "2026-06",
      declared_amount_cent: 120050,
      reason: "二季度文化事业建设费申报表",
      parallel_lines: [
        { line_code: "finance_review", final_approver_id: "usr_f" },
        { line_code: "tax_review", final_approver_id: "usr_t" },
        { line_code: "legal_review", final_approver_id: "usr_l" },
      ],
    });
    expect(typeof lastPost?.body.idempotency_key).toBe("string");
    await waitFor(() => expect(screen.queryByTestId("entity-tax-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "entity_tax_declared" }));
  });
});
