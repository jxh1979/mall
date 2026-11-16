// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OrgHierarchyPage } from "./OrgHierarchyPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

const HIERARCHY = [{ user_id: "usr_a", superior_id: "usr_b", line_code: "finance_review", updated_by: "usr_admin", updated_at: "2026-09-06T02:00:00Z" }];
const LINES = [
  { line_code: "finance_review", line_name: "财务审核线", min_start_rank: 6, entry_user_id: "usr_fin_entry", status: "active" },
  { line_code: "legal_review", line_name: "法务审核线", min_start_rank: 5, entry_user_id: null, status: "active" },
  { line_code: "business_review", line_name: "业务审核线", min_start_rank: 4, entry_user_id: null, status: "active" },
  { line_code: "tax_review", line_name: "税务审核线", min_start_rank: 4, entry_user_id: null, status: "inactive" },
];
// 第十八批 AM 段第 241 条 241-R8：付款确认各支付类型在财务、法务两线之外的必带线。
const FLOWS = [
  { payment_type: "purchase_inbound", payment_type_label: "采购入库", line_codes: ["business_review"] },
  { payment_type: "staff_payroll", payment_type_label: "员工工资", line_codes: [] },
];
const USERS = [{ id: "usr_a", username: "a" }, { id: "usr_b", username: "b" }];

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("/payment-confirmation-flows/") && init?.method === "PUT") {
      return Promise.resolve(raw({ payment_type: "staff_payroll", payment_type_label: "员工工资", line_codes: ["business_review"] }));
    }
    if (url.includes("/payment-confirmation-flows")) return Promise.resolve(raw(FLOWS));
    if (url.includes("/org-hierarchy/") && init?.method === "PUT") return Promise.resolve(raw({ user_id: "usr_a", superior_id: "usr_c", line_code: null }));
    if (url.includes("/approval-lines/") && init?.method === "PUT") return Promise.resolve(raw({ line_code: "legal_review", line_name: "法务审核线", min_start_rank: 3, entry_user_id: null, status: "active" }));
    if (url.includes("/org-hierarchy")) return Promise.resolve(raw(HIERARCHY));
    if (url.includes("/approval-lines")) return Promise.resolve(raw(LINES));
    if (url.includes("/access/users")) return Promise.resolve(raw(USERS));
    return Promise.resolve(raw([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["access.user.view"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<OrgHierarchyPage {...props} />);
  return props;
}

// M087-A-01 / A-03（PGM-1.7.1 组织层级「直接上级」与平行审批线「最低起点层级」）
describe("OrgHierarchyPage 组织层级与审批线维护", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("展示直接上级表与审批线表；保存直接上级提交 superior_id 且结果为中文句子", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-row-usr_a")).toBeTruthy());
    expect(screen.getByTestId("org-hierarchy-row-usr_a").textContent).toContain("usr_b");
    expect(screen.getByTestId("approval-line-row-finance_review").textContent).toContain("财务审核线");
    expect(screen.getByTestId("approval-line-row-finance_review").textContent).toContain("启用");

    fireEvent.change(screen.getByTestId("org-hierarchy-superior"), { target: { value: "usr_c" } });
    fireEvent.click(screen.getByTestId("org-hierarchy-save"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input, init]) => String(input).includes("/org-hierarchy/usr_a") && (init as RequestInit | undefined)?.method === "PUT")).toBe(true));
    const call = fetchMock.mock.calls.find(([input, init]) => String(input).includes("/org-hierarchy/usr_a") && (init as RequestInit | undefined)?.method === "PUT");
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.superior_id).toBe("usr_c");
    expect(body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("已把 usr_a 的直接上级设为 usr_c"));
  });

  it("直接上级不能是本人；最低起点层级越界在页面即拒绝", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("org-hierarchy-superior"), { target: { value: "usr_a" } });
    fireEvent.click(screen.getByTestId("org-hierarchy-save"));
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-error").textContent).toContain("直接上级不能是本人"));

    fireEvent.change(screen.getByTestId("approval-line-code"), { target: { value: "legal_review" } });
    fireEvent.change(screen.getByTestId("approval-line-name"), { target: { value: "法务审核线" } });
    fireEvent.change(screen.getByTestId("approval-line-rank"), { target: { value: "31" } });
    fireEvent.click(screen.getByTestId("approval-line-save"));
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-error").textContent).toContain("1 到 30"));
  });

  it("保存审批线提交 min_start_rank 与 status、不发写死备注（198-R5）；只有 view 权限时保存按钮不可用", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("approval-line-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("approval-line-code"), { target: { value: "legal_review" } });
    fireEvent.change(screen.getByTestId("approval-line-name"), { target: { value: "法务审核线" } });
    fireEvent.change(screen.getByTestId("approval-line-rank"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("approval-line-hr"), { target: { value: "usr_hr" } });
    fireEvent.click(screen.getByTestId("approval-line-save"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/approval-lines/legal_review"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/approval-lines/legal_review"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.min_start_rank).toBe(3);
    expect(body.hr_final_approver_id).toBe("usr_hr");
    expect(body.status).toBe("active");
    expect(body).not.toHaveProperty("operator_note");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^已保存平行审批线 legal_review/)));

    vi.unstubAllGlobals();
    renderPage(VIEW_ONLY);
    await waitFor(() => expect(screen.getAllByTestId("org-hierarchy-save").length).toBeGreaterThan(0));
    const buttons = screen.getAllByTestId("org-hierarchy-save") as HTMLButtonElement[];
    expect(buttons[buttons.length - 1]?.disabled).toBe(true);
  });

  // 第十八批 AM 段第 241 条 241-R8（BSM:1803「具体流程按支付类型分别设定」；第十八批裁决 66「每类必带财务、法务两线」、67「审批线页设定」）。
  it("付款确认流程表显示支付类型中文名与两线之外的必带线；只能勾在用的其他线；保存发 PUT", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.queryByTestId("payment-flow-row-purchase_inbound")).not.toBeNull());
    const purchase = screen.getByTestId("payment-flow-row-purchase_inbound").textContent ?? "";
    expect(purchase).toContain("采购入库");
    expect(purchase).toContain("业务审核线");
    expect(purchase).not.toContain("business_review");
    expect(screen.getByTestId("payment-flow-row-staff_payroll").textContent).toContain("员工工资");

    fireEvent.click(screen.getByTestId("payment-flow-select-staff_payroll"));
    await waitFor(() => expect((screen.getByTestId("payment-flow-type") as HTMLSelectElement).value).toBe("staff_payroll"));
    expect(screen.queryByTestId("payment-flow-line-finance_review")).toBeNull();
    expect(screen.queryByTestId("payment-flow-line-legal_review")).toBeNull();
    expect(screen.queryByTestId("payment-flow-line-tax_review")).toBeNull();
    fireEvent.click(screen.getByTestId("payment-flow-line-business_review"));
    fireEvent.click(screen.getByTestId("payment-flow-save"));

    const fetchMock = vi.mocked(globalThis.fetch);
    const isPut = ([input, init]: unknown[]) => String(input).includes("/payment-confirmation-flows/staff_payroll") && (init as RequestInit | undefined)?.method === "PUT";
    await waitFor(() => expect(fetchMock.mock.calls.some(isPut)).toBe(true));
    const body = JSON.parse(String((fetchMock.mock.calls.find(isPut)?.[1] as RequestInit | undefined)?.body));
    expect(body.line_codes).toEqual(["business_review"]);
    expect(body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("员工工资")));
  });
});
