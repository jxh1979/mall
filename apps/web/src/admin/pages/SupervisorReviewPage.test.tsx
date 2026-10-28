// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SupervisorReviewPage } from "./SupervisorReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const POLICIES = [
  { id: "sp-1", policy_code: "POL-1", action_type: "contract_violation", required_level: 3, blocks_by_default: true, required_evidence_types: ["contract"], status: "active" },
  { id: "sp-2", policy_code: "POL-2", action_type: "billing_discount", required_level: 2, blocks_by_default: false, required_evidence_types: [], status: "disabled" },
];

const REAL_POLICIES = [
  {"policy_code": "staff_shift_publish", "action_type": "staff.shift.publish", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
  {"policy_code": "staff_penalty_cap_adjust", "action_type": "staff.payroll.penalty_cap.adjust", "required_evidence_types": ["reason", "before_after"], "status": "active", "blocks_by_default": true},
  {"policy_code": "manual_release_l2", "action_type": "manual_release", "required_evidence_types": ["operator_note", "evidence_attachment"], "status": "active", "blocks_by_default": true},
  {"policy_code": "bill_waiver_l3", "action_type": "bill_waiver", "required_evidence_types": ["reason", "evidence_attachment"], "status": "active", "blocks_by_default": true},
  {"policy_code": "refund_l2", "action_type": "refund", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
  {"policy_code": "ar_ap_adjustment_l3", "action_type": "ar_ap_adjustment", "required_evidence_types": ["reason", "before_after"], "status": "active", "blocks_by_default": true},
  {"policy_code": "contract_violation_l3", "action_type": "contract_violation", "required_evidence_types": ["violation_summary", "contract_snapshot"], "status": "active", "blocks_by_default": true},
  {"policy_code": "base_definition_violation_l3", "action_type": "base_definition_violation", "required_evidence_types": ["violation_summary", "definition_snapshot"], "status": "active", "blocks_by_default": true},
  {"policy_code": "permission_grant_l1", "action_type": "permission_grant_l1", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
  {"policy_code": "permission_grant_l2", "action_type": "permission_grant_l2", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
  {"policy_code": "permission_grant_l3", "action_type": "permission_grant_l3", "required_evidence_types": ["reason"], "status": "active", "blocks_by_default": true},
  {"policy_code": "tax_rule_adjustment_l3", "action_type": "finance.tax_rule.adjust", "required_evidence_types": ["reason", "before_after"], "status": "active", "blocks_by_default": true},
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

const PENDING = [
  { decision: { review_id: "review_p1", policy_code: "bar_bill_waiver_block", action_type: "bar.bill_waiver", object_type: "bar_bill", object_id: "b1", initiated_by: "u-9", approval_level: "one_level_up", required_level: 2, review_status: "pending_review" },
    schedule: { review_id: "review_p1", beneficiary_id: "usr_benef", due_at: "2026-09-07T02:00:00Z", escalation_count: 1, escalated_at: "2026-09-06T02:00:00Z", forwarded_to: null } },
  { decision: { review_id: "review_p2", policy_code: "contract_violation_l3", action_type: "contract_violation", object_type: "合同", object_id: "c1", initiated_by: "u-9", approval_level: "two_levels_up", required_level: 3, review_status: "pending_review" },
    schedule: { review_id: "review_p2", beneficiary_id: null, due_at: "2026-09-07T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
  { decision: { review_id: "review_p3", policy_code: "refund_l2", action_type: "refund", object_type: "票据", object_id: "t1", initiated_by: null, approval_level: "one_level_up", required_level: 2, review_status: "pending_review" },
    schedule: { review_id: "review_p3", beneficiary_id: "usr_benef", due_at: "2026-09-07T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
  { decision: { review_id: "review_p4", policy_code: "bar_bill_waiver_block", action_type: "bar.bill_waiver", object_type: "bar_bill", object_id: "b2", initiated_by: "u1", approval_level: "one_level_up", required_level: 2, review_status: "pending_review" },
    schedule: { review_id: "review_p4", beneficiary_id: "usr_benef", due_at: "2026-09-07T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
  { decision: { review_id: "review_p5", policy_code: "bar_bill_waiver_block", action_type: "bar.bill_waiver", object_type: "bar_bill", object_id: "b3", initiated_by: "u-9", approval_level: "one_level_up", required_level: 2, review_status: "pending_review" },
    schedule: { review_id: "review_p5", beneficiary_id: "u1", due_at: "2026-09-07T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
];

function raw(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(options: { policies?: unknown[]; pending?: unknown[]; reviewStatus?: string; rejectAction?: string } = {}): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("supervisor-review-policies")) return Promise.resolve(envelope(options.policies ?? POLICIES));
    if (url.includes("supervisor-review/evaluate")) return Promise.resolve(raw({ review_id: "review_1", blocked: true, review_status: "pending_review", required_level: 3 }));
    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope(options.pending ?? PENDING));
    if (options.rejectAction && url.endsWith(`/${options.rejectAction}`)) return Promise.resolve(new Response(JSON.stringify({ success: false, code: "BENEFICIARY_REQUIRED", message: "直接受益人未登记" }), { status: 400, headers: { "content-type": "application/json" } }));
    if (url.includes("/approve")) return Promise.resolve(raw({ review_id: "review_p1", review_status: options.reviewStatus ?? "approved" }));
    if (url.includes("/forward")) return Promise.resolve(raw({ review_id: "review_p1", forwarded_to: "usr_x", due_at: "2026-09-09T02:00:00Z", escalation_count: 0 }));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const MANAGE_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view", "governance.supervisor_review.manage"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS, options: Parameters<typeof makeApi>[0] = {}) {
  const props: AdminPageProps = {
    api: makeApi(options),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<SupervisorReviewPage {...props} />);
  return props;
}

async function evaluateOnce() {
  await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
  fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
  fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
  await waitFor(() => expect(screen.getByTestId("supervisor-review-current-review").textContent).toContain("review_1"));
}

// M007-A-05 / F-08（PGM-1.18 全中文；PGM-1.10 一个功能一个权限点）
describe("SupervisorReviewPage 结果提示全中文 + 审批权限点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/supervisor-review");
  });

  it("评估成功 → 交给主壳的是中文句子，且审批按钮对持有 approve 权限者可用", async () => {
    const props = renderPage();
    await evaluateOnce();
    expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^已生成待复核记录：review_1/));
    expect((screen.getByTestId("supervisor-review-approve") as HTMLButtonElement).disabled).toBe(false);
  });

  it("只有 manage 没有 approve 权限 → 能评估、不能审批", async () => {
    renderPage(MANAGE_ONLY_PERMS);
    await evaluateOnce();
    expect((screen.getByTestId("supervisor-review-approve") as HTMLButtonElement).disabled).toBe(true);
  });

  // M007-R6-03（PGM-1.7.1「任一节点驳回则整单退回起草人」）：本页可驳回，提交 decision=rejected，结果为中文句子；无 approve 权限时不可用。
  it("驳回 → 提交 decision=rejected 且结果为中文句子", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p1"));
    fireEvent.change(screen.getByTestId("supervisor-review-evidence-attachment"), { target: { value: "att-1" } });
    fireEvent.change(screen.getByTestId("supervisor-review-approval-comment"), { target: { value: "证据不足，退回起草人" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("supervisor-review-reject"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/supervisor-review/review_p1/approve"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/review_p1/approve"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.decision).toBe("rejected");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^复核单 .* 已驳回，整单退回起草人/)));
  });

  it("只有 manage 没有 approve 权限 → 不能驳回", async () => {
    renderPage(MANAGE_ONLY_PERMS);
    await evaluateOnce();
    expect((screen.getByTestId("supervisor-review-reject") as HTMLButtonElement).disabled).toBe(true);
  });

  it("待复核清单展示到期与升级次数；转交提交 forwarded_to 且结果为中文句子（PGM-1.7.1）", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-review_p1")).toBeTruthy());
    const row = screen.getByTestId("supervisor-review-pending-review_p1").textContent ?? "";
    expect(row).toContain("review_p1");
    expect(row).toContain("已升级 1 次");
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p1"));
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_x" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/supervisor-review/review_p1/forward"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/forward"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.forwarded_to).toBe("usr_x");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^复核单 review_p1 已转交给 usr_x/)));
  });

  it("待复核清单展示直接受益人", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-review_p1")).toBeTruthy());
    expect(screen.getByTestId("supervisor-review-pending-review_p1").textContent ?? "").toContain("usr_benef");
    expect(screen.getByTestId("supervisor-review-pending-review_p2").textContent ?? "").toContain("-");
  });

  it("合同违规待审行没有直接受益人 → 仍可提交合法转交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p2"));
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_x" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input]) => String(input).includes("/review_p2/forward"))).toBe(true));
  });

  it("退款待审行没有申请人 → 转交被拦且不发请求", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p3"));
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_x" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("申请人未登记，不能转交");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/forward"))).toBe(false);
  });

  it("申请人用登录名转交给自己 → 被拦", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p4")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p4"));
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "admin" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不得转交给申请人本人");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/forward"))).toBe(false);
  });

  it("直接受益人用登录名转交给自己 → 被拦", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p5")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p5"));
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "admin" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不得转交给直接受益人");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/forward"))).toBe(false);
  });

  it("不得转交给申请人本人", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p1"));
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "u-9" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不得转交给申请人本人");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/forward"))).toBe(false);
  });

  it("最终审批人不能是当前申请人", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: "u1" } });
    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不能指定申请人为最终审批人");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/evaluate"))).toBe(false);
  });

  it("最终审批人填登录名也按申请人拦住", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: "admin" } });
    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不能指定申请人为最终审批人");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/evaluate"))).toBe(false);
  });

  it("最终审批人不能是直接受益人", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
    fireEvent.change(screen.getByTestId("supervisor-review-beneficiary"), { target: { value: "usr_benef" } });
    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: "usr_benef" } });
    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不能指定直接受益人为最终审批人");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/evaluate"))).toBe(false);
  });

  it("待审行没有申请人 → 不能放行或驳回", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p3"));
    fireEvent.change(screen.getByTestId("supervisor-review-approval-comment"), { target: { value: "同意放行" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("申请人未登记，不能放行或驳回");
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/approve"))).toBe(false);
  });

  it("待审动作对不上目录 → 提交服务端校验", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-select-review_p1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-pending-select-review_p1"));
    fireEvent.change(screen.getByTestId("supervisor-review-approval-comment"), { target: { value: "同意放行" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([input]) => String(input).includes("/review_p1/approve"))).toBe(true));
  });

  it("动作类型选项与服务端策略一致：无「账单折扣」，有免单 / 退款 / 调账", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-action-type")).toBeTruthy());
    const options = Array.from((screen.getByTestId("supervisor-review-action-type") as HTMLSelectElement).options).map((option) => option.value);
    expect(options).not.toContain("billing_discount");
    expect(options).toEqual(expect.arrayContaining(["bill_waiver", "refund", "ar_ap_adjustment"]));
  });
});

describe("SupervisorReviewPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/supervisor-review");
  });

  it("点“默认阻断” → URL 带 ?kpi=policies&subset=blocked，聚焦只剩默认阻断策略", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-kpi-blocked")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-kpi-blocked"));
    expect(window.location.search).toContain("kpi=policies");
    expect(window.location.search).toContain("subset=blocked");
    expect(screen.getByTestId("supervisor-review-kpi-focus-banner").textContent).toContain("正在查看：默认阻断（1）");
    const table = screen.getByTestId("supervisor-review-table").textContent ?? "";
    expect(table).toContain("POL-1");
    expect(table).not.toContain("POL-2");
    expect(screen.queryByTestId("supervisor-review-summary")).toBeNull();
    expect(screen.queryByTestId("supervisor-review-form")).toBeNull();
  });

  it("聚焦后点「返回全部」→ 恢复 KPI 卡+评估表单+全量策略", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-kpi-total"));
    fireEvent.click(screen.getByTestId("supervisor-review-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("supervisor-review-summary")).toBeTruthy();
    expect(screen.getByTestId("supervisor-review-form")).toBeTruthy();
    expect(screen.getByTestId("supervisor-review-table").textContent).toContain("POL-2");
  });

  it("KPI 子标签与动作类型列为中文，不含 blocksByDefault/英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-summary")).toBeTruthy());
    const summary = screen.getByTestId("supervisor-review-summary").textContent ?? "";
    expect(summary).not.toContain("blocksByDefault");
    const table = screen.getByTestId("supervisor-review-table").textContent ?? "";
    expect(table).toContain("合同违规");
    expect(table).not.toContain("contract_violation");
  });
});

describe("SupervisorReviewPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/supervisor-review");
  });

  it("点策略行 → 抽屉含该行动作类型（中文）与默认阻断（是/否），无英文枚举，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-row-POL-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-row-POL-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("合同违规");
    expect(drawer.textContent).toContain("是");
    expect(drawer.textContent).not.toContain("contract_violation");
    expect(drawer.textContent).not.toContain("blocksByDefault");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

describe("M043 真实审批信封与既有规则", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/supervisor-review"); });

  async function select(id = "review_p1") {
    await waitFor(() => expect(screen.getByTestId(`supervisor-review-pending-select-${id}`)).toBeTruthy());
    fireEvent.click(screen.getByTestId(`supervisor-review-pending-select-${id}`));
    fireEvent.change(screen.getByTestId("supervisor-review-approval-comment"), { target: { value: "已核对凭据" } });
    vi.spyOn(window, "confirm").mockReturnValue(true);
  }

  it.each([
    ["pending_review", "当前节点已通过，等待后续节点审批"],
    ["approved", "全部节点已通过，单据生效"],
  ])("批准返回 %s 如实显示节点状态", async (status, message) => {
    const props = renderPage(ADMIN_PERMS, { reviewStatus: status });
    await select();
    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining(message)));
    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/review_p1/approve"));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ decision: "approved", evidence_attachment_ids: [], comment: "已核对凭据" });
    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining("当前节点"));
  });

  it.each(["approve", "reject"])("%s 按 policyCode 而非动作名要求免单附件", async (button) => {
    const pending = [{ ...PENDING[0], decision: { ...PENDING[0]!.decision, policy_code: "bill_waiver_l3" } }];
    const props = renderPage(ADMIN_PERMS, { policies: REAL_POLICIES, pending });
    await select();
    fireEvent.click(screen.getByTestId(`supervisor-review-${button}`));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("请填写证据附件编号");
    expect(window.confirm).not.toHaveBeenCalled();
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/approve"))).toBe(false);
    fireEvent.change(screen.getByTestId("supervisor-review-evidence-attachment"), { target: { value: " att-1 " } });
    fireEvent.click(screen.getByTestId(`supervisor-review-${button}`));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/approve"));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ evidence_attachment_ids: ["att-1"], decision: button === "approve" ? "approved" : "rejected" });
  });

  it.each(["contract_violation_l3", "refund_l2"])("%s 非附件证据不强迫填附件", async (policyCode) => {
    const pending = [{ ...PENDING[0], decision: { ...PENDING[0]!.decision, policy_code: policyCode, action_type: policyCode === "refund_l2" ? "refund" : "contract_violation" } }];
    const props = renderPage(ADMIN_PERMS, { policies: REAL_POLICIES, pending });
    await select();
    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/approve"));
    expect(JSON.parse(String(call?.[1]?.body)).evidence_attachment_ids).toEqual([]);
  });

  it.each(["approve", "forward"])("%s 如实显示服务端拒绝，不报告成功", async (action) => {
    const pending = [{ ...PENDING[0], schedule: { ...PENDING[0]!.schedule, beneficiary_id: null } }];
    const props = renderPage(ADMIN_PERMS, { pending, rejectAction: action });
    await select();
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_x" } });
    fireEvent.click(screen.getByTestId(`supervisor-review-${action}`));
    await waitFor(() => expect(screen.getByTestId("supervisor-review-error").textContent).toContain("直接受益人未登记"));
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).endsWith(`/${action}`))).toBe(true);
    expect(props.setActionResult).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({ status: "failed", message: "直接受益人未登记" }));
  });

  it.each([
    ["review_p4", "不能复核自己发起的单"],
    ["review_p5", "直接受益人不能复核"],
  ])("%s 放行拒绝利益冲突", async (id, message) => {
    renderPage();
    await select(id);
    fireEvent.click(screen.getByTestId("supervisor-review-approve"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain(message);
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/approve"))).toBe(false);
  });

  it.each(["bill_waiver", "refund", "ar_ap_adjustment"])("%s 评估先提示受益人，填写后提交", async (action) => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-action-type")).toBeTruthy());
    fireEvent.change(screen.getByTestId("supervisor-review-action-type"), { target: { value: action } });
    fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "已核对业务原因" } });
    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("请填写直接受益人用户编号");
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/evaluate"))).toBe(false);
    fireEvent.change(screen.getByTestId("supervisor-review-beneficiary"), { target: { value: "usr_benef" } });
    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
    await waitFor(() => expect(screen.getByTestId("supervisor-review-current-review").textContent).toContain("review_1"));
    const call = vi.mocked(fetch).mock.calls.find(([url]) => String(url).includes("/evaluate"));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ action_type: action, beneficiary_id: "usr_benef", amount_cent: 0 });
  });

  it("真实目录、待审枚举及抽屉均显示中文，未知枚举不泄漏原码", async () => {
    renderPage(ADMIN_PERMS, { policies: [...REAL_POLICIES, { policy_code: "unknown-policy-id", action_type: "future_action", required_evidence_types: ["future_evidence"], status: "active" }] });
    await select();
    const table = screen.getByTestId("supervisor-review-table");
    for (const row of REAL_POLICIES) {
      expect(table.textContent).toContain(row.policy_code);
      const actionCell = screen.getByText(row.policy_code).closest("tr")?.children[1];
      expect(actionCell?.textContent).toMatch(/[\u4e00-\u9fff]/);
      expect(actionCell?.textContent).not.toContain(row.action_type);
      const evidenceCell = screen.getByText(row.policy_code).closest("tr")?.children[4];
      for (const code of row.required_evidence_types) expect(evidenceCell?.textContent).not.toContain(code);
    }
    for (const row of REAL_POLICIES) {
      const policyRow = screen.getByTestId(`supervisor-review-row-${row.policy_code}`);
      fireEvent.click(policyRow);
      expect(screen.getByTestId("row-detail-drawer").textContent).toContain(row.policy_code);
      fireEvent.keyDown(window, { key: "Escape" });
      await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
    }
    expect(table.textContent).not.toContain("future_action");
    expect(table.textContent).not.toContain("future_evidence");
    const pending = screen.getByTestId("supervisor-review-pending").textContent;
    for (const code of ["bar.bill_waiver", "bar_bill", "one_level_up", "two_levels_up"]) expect(pending).not.toContain(code);
    fireEvent.click(screen.getByText("bill_waiver_l3"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("免单");
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("证据附件");
    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("evidence_attachment");
    expect(screen.getByTestId("supervisor-review-forward-target").getAttribute("aria-label")).toContain("用户编号");
    expect(screen.getByTestId("supervisor-review-forward-target").getAttribute("placeholder")).toContain("不是登录名");
    expect(screen.getByTestId("supervisor-review-final-approver").closest("label")?.textContent).toContain("用户编号");
    expect(screen.getByTestId("supervisor-review-beneficiary").closest("label")?.textContent).toContain("用户编号");
  });

  it("证照宽限期申请的动作类型与对象显示中文", async () => {
    const pending = [{ decision: { review_id: "review_g1", policy_code: "compliance_grace_period", action_type: "compliance.expiry_item.grace", object_type: "compliance_expiry_item", object_id: "lic-1", initiated_by: "u-m", approval_level: "top_authority", required_level: 3, review_status: "pending_review" }, schedule: null }];
    const policies = [{ policy_code: "compliance_grace_period", action_type: "compliance.expiry_item.grace", required_evidence_types: ["reason"], status: "active", blocks_by_default: true }];
    renderPage(ADMIN_PERMS, { policies, pending });
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-review_g1").textContent).toContain("证照宽限期申请"));
    const row = screen.getByTestId("supervisor-review-pending-review_g1").textContent ?? "";
    expect(row).toContain("证照 / lic-1");
    expect(row).not.toContain("未识别类型");
    expect(screen.getByTestId("supervisor-review-row-compliance_grace_period").textContent).toContain("证照宽限期申请");
  });

  it("第三人不能按用户编号转交给直接受益人", async () => {
    renderPage();
    await select();
    fireEvent.change(screen.getByTestId("supervisor-review-forward-target"), { target: { value: "usr_benef" } });
    fireEvent.click(screen.getByTestId("supervisor-review-forward"));
    expect(screen.getByTestId("supervisor-review-error").textContent).toContain("不得转交给直接受益人");
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/forward"))).toBe(false);
  });
});
