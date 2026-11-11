import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ApprovalChainPage } from "./ApprovalChainPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 文件版本：V0.09

function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function envelope(items: unknown[]): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }), { status: 200, headers: { "content-type": "application/json" } });
}

const PENDING = [
  { decision: { review_id: "review_c1", policy_code: "contract_violation_l3", action_type: "contract_violation", object_type: "contract", object_id: "c1", initiated_by: "usr_l1", approval_level: "two_levels_up", required_level: 3, review_status: "pending_review" },
    schedule: { review_id: "review_c1", beneficiary_id: null, due_at: "2026-09-09T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
  { decision: { review_id: "review_orphan", policy_code: "permission_grant_l2", action_type: "access.grant_request", object_type: "access_grant_request", object_id: "agr_1", initiated_by: "usr_orphan", approval_level: "two_levels_up", required_level: 2, review_status: "pending_review" },
    schedule: null },
  { decision: { review_id: "review_tax", action_type: "finance.tax_rule.adjust", object_type: "payroll_tax_version", object_id: "tax-version-1", initiated_by: "tax-editor", review_status: "pending_review", reason: '原版：V0.01；完整新版本：{"category":"经专业确认的税目","version":"V0.02","effectiveFrom":"2099-08-01","applicabilityBasis":"专业适用依据原文","calculationBasis":"经确认的逐档公式","roundingRule":"truncate_cent_each_step","brackets":[{"seq":1,"incomeFloorCent":0,"incomeCeilingCent":null,"incomeBaseBps":10000,"flatDeductionCent":0,"rateBps":1500,"quickDeductionCent":0}],"reason":"专业税务核定后调整"}', evidence_attachment_ids: ["proof tax/1"] }, schedule: null },
  { decision: { review_id: "review_assessment", action_type: "finance.tax_rule.adjust", object_type: "payroll_tax_assessment", object_id: "assessment-1", initiated_by: "tax-editor", review_status: "pending_review", reason: '既有服务收入专业税务认定（非正式申报）：{"userId":"employee-1","month":"2099-07","payerCompanyId":"payer-1","identityBasis":"专业纳税身份核验","transactionBasis":"服务交易专业结论","professionalBasis":"凭证核验结论","incomeCent":1000000,"taxCent":100000,"taxVersionId":"tax-version-1","sourceLines":[{"sourceId":"service-1","amountCent":1000000}]}', evidence_attachment_ids: [] }, schedule: null },
];
const NODES = [
  { node_id: "n1", review_id: "review_c1", line_code: "main", seq: 1, approver_kind: "user", approver_id: "usr_a", status: "approved", decided_by: "usr_a", decided_at: "2026-09-06T03:00:00Z", forwarded_to: null, forwarded_by: null, escalation_count: 0 },
  { node_id: "n2", review_id: "review_c1", line_code: "main", seq: 2, approver_kind: "user", approver_id: "usr_c", status: "pending", decided_by: null, decided_at: null, forwarded_to: null, forwarded_by: null, escalation_count: 1 },
  { node_id: "n3", review_id: "review_c1", line_code: "finance_review", seq: 1, approver_kind: "user", approver_id: "usr_fin", status: "pending", decided_by: null, decided_at: null, forwarded_to: "usr_x", forwarded_by: "usr_fin", escalation_count: 0 },
];

function makeApi(extraPending: unknown[] = []): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope([...PENDING, ...extraPending]));
    if (url.includes("/review_c1/chain/regenerate")) return Promise.resolve(raw(NODES));
    if (url.includes("/review_orphan/chain/regenerate")) return Promise.resolve(raw([NODES[1]]));
    if (url.includes("/review_c1/chain")) return Promise.resolve(raw(NODES));
    if (url.includes("/review_orphan/chain")) return Promise.resolve(raw([]));
    if (url.includes("/review_prepayment/chain")) return Promise.resolve(raw([{ ...NODES[1], line_code: "business_review" }]));
    if (url.endsWith("/review_prepayment")) return Promise.resolve(raw({
      review_id: "review_prepayment", review_status: "pending_review", object_type: "purchase_prepayment_resolution",
      object_id: "prepayment-resolution-1", evidence_attachment_ids: [], reason: JSON.stringify({
        purchase_id: "purchase-1", resolution_type: "refund_unused", source_payment_id: "payment-1",
        amount_cent: 30000, refund_due_date: "2099-08-20", contract_basis: "余量取消后按合同退款",
        contract_snapshot: { contract: { contract_mode: "platform_purchase_distribution" }, approved_at: "2099-07-01" },
      }),
    }));
    if (url.includes("/review_day/chain")) return Promise.resolve(raw(NODES));
    if (url.endsWith("/review_day")) return Promise.resolve(raw({
      review_id: "review_day", review_status: "pending_review", object_type: "amusement_business_day_config",
      object_id: "day-1", evidence_attachment_ids: [], reason: "日切调整\n" + JSON.stringify({
        projectId: "project-1", previousVersions: [{ version: "V0.01", zoneId: "Asia/Shanghai", cutoffHour: 10, status: "approved" }],
        proposedConfig: { zone_id: "Asia/Shanghai", cutoff_hour: 9, min_cutoff_hour: 7, max_cutoff_hour: 12, effective_at: "2099-08-01T00:00:00Z" },
      }),
    }));
    if (url.includes("/review_grace/chain")) return Promise.resolve(raw([{ node_id: "n-grace", review_id: "review_grace", line_code: "main", seq: 1, approver_kind: "master", status: "pending" }]));
    if (url.endsWith("/review_grace")) return Promise.resolve(raw({
      review_id: "review_grace", review_status: "pending_review", action_type: "compliance.expiry_item.grace", object_type: "compliance_expiry_item",
      object_id: "lic-1", evidence_attachment_ids: [], reason: "新证在办，等待发证\n" + JSON.stringify({
        item_name: "食品经营许可证", item_type: "food_license", document_no: "FD-1", valid_to: "2026-10-20", grace_days: 30,
        grace_until: "2026-11-19", merchant_id: "mch-1",
      }),
    }));
    if (url.includes("/review_tax/chain")) return Promise.resolve(raw(NODES));
    if (url.includes("/review_assessment/chain")) return Promise.resolve(raw(NODES));
    if (url.includes("/review_master/chain")) return Promise.resolve(raw([{ node_id: "n-master", review_id: "review_master", line_code: "main", seq: 1, approver_kind: "master", status: "pending" }]));
    if (url.includes("/review_final/chain")) return Promise.resolve(raw([]));
    if (url.endsWith("/review_master") && init?.method !== "POST") return Promise.resolve(raw({ ...PENDING.find(row => row.decision.review_id === "review_tax")?.decision, review_id: "review_master" }));
    if (url.endsWith("/review_final") && init?.method !== "POST") return Promise.resolve(raw({ ...PENDING.find(row => row.decision.review_id === "review_tax")?.decision, review_id: "review_final", review_status: "approved" }));
    if (url.endsWith("/review_missing") && init?.method !== "POST") return Promise.resolve(new Response(JSON.stringify({ success: false, code: "SUPERVISOR_REVIEW_SCOPE_FORBIDDEN", message: "无权查看该原件" }), { status: 403, headers: { "content-type": "application/json" } }));
    if (init?.method !== "POST") {
      const original = PENDING.find(row => url.endsWith(`/supervisor-review/${row.decision.review_id}`));
      if (original) return Promise.resolve(raw(original.decision));
    }
    if (url.includes("/approve")) return Promise.resolve(raw({ review_id: "review_c1", review_status: "pending_review", decision: "blocked" }));
    if (url.includes("/forward")) return Promise.resolve(raw({ review_id: "review_c1", forwarded_to: "usr_y" }));
    return Promise.resolve(raw([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS, focusObjectId?: string, extraPending: unknown[] = []) {
  const props: AdminPageProps = {
    api: makeApi(extraPending),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
    focusObjectId,
  };
  render(<ApprovalChainPage {...props} />);
  return props;
}

// M087-A-02（PGM-1.7.1 逐级审批、任一节点驳回整单退回、平行线全部通过才生效）
describe("ApprovalChainPage 审批链与节点裁决", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/approval-chain");
  });

  it("选单后展示主线与平行线节点、当前节点标记、转交与升级信息", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("approval-chain-pending-review_c1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("approval-chain-select-review_c1"));
    await waitFor(() => expect(screen.getByTestId("approval-chain-nodes")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-node-main-1").textContent).toContain("已通过");
    expect(screen.getByTestId("approval-chain-node-main-2").textContent).toContain("待审（当前）");
    expect(screen.getByTestId("approval-chain-node-main-2").textContent).toContain("直接上级");
    expect(screen.getByTestId("approval-chain-node-finance_review-1").textContent).toContain("usr_x");
    expect(screen.getByTestId("approval-chain-node-main-2").textContent).not.toMatch(/pending|approved|user/);
  });

  it("通过当前节点 → 提交 decision=approved、不发复核人层级（198-R5）并给出中文结果；驳回按钮受 approve 权限门控", async () => {
    const props = renderPage(ADMIN_PERMS, "review_c1");
    await waitFor(() => expect(screen.getByTestId("approval-chain-nodes")).toBeTruthy());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("approval-chain-approve"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/review_c1/approve"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/review_c1/approve"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.decision).toBe("approved");
    expect(body.comment).toBe("证据齐备，同意");
    expect(body.evidence_attachment_ids).toEqual([]);
    expect(body).not.toHaveProperty("reviewer_level");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("复核单 review_c1 当前节点已通过，等待后续节点审批"));

    vi.unstubAllGlobals();
    renderPage(VIEW_ONLY, "review_c1");
    await waitFor(() => expect(screen.getAllByTestId("approval-chain-reject").length).toBeGreaterThan(0));
    const rejects = screen.getAllByTestId("approval-chain-reject") as HTMLButtonElement[];
    expect(rejects[rejects.length - 1]?.disabled).toBe(true);
  });

  it("链未生成的单据提示先维护直接上级，并可重生成链（manage 门禁）", async () => {
    const props = renderPage(ADMIN_PERMS, "review_orphan");
    await waitFor(() => expect(screen.getByTestId("approval-chain-unresolved")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-unresolved").textContent).toContain("直接上级未维护");
    fireEvent.click(screen.getByTestId("approval-chain-regenerate"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/review_orphan/chain/regenerate"))).toBe(true));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("复核单 review_orphan 审批链已重生成，共 1 个节点"));
  });

  it("转交当前节点提交 forwarded_to，不发写死备注（198-R5）", async () => {
    const props = renderPage(ADMIN_PERMS, "review_c1");
    await waitFor(() => expect(screen.getByTestId("approval-chain-nodes")).toBeTruthy());
    fireEvent.change(screen.getByTestId("approval-chain-forward-target"), { target: { value: "usr_y" } });
    fireEvent.click(screen.getByTestId("approval-chain-forward"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/review_c1/forward"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/review_c1/forward"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.forwarded_to).toBe("usr_y");
    expect(body).not.toHaveProperty("operator_note");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("复核单 review_c1 当前节点已转交给 usr_y"));
  });

  it("预付余额原件展示真实付款、合同依据和金额，不暴露未翻译的处理枚举", async () => {
    renderPage(VIEW_ONLY, "review_prepayment");
    await waitFor(() => expect(screen.getByTestId("approval-chain-original").textContent).toContain("退回未用预付款"));
    const text = screen.getByTestId("approval-chain-original").textContent ?? "";
    for (const expected of ["原实际付款编号payment-1", "金额（分）30000", "退款约定到期日2099-08-20", "合同及双方确认依据", "平台统采分销"]) expect(text).toContain(expected);
    expect(text).not.toMatch(/refund_unused|source_payment_id|contract_mode/);
    expect(screen.getByText("业务审核线")).toBeTruthy();
  });

  it("游乐营业日审批原件同时展示旧版本和拟生效的时区、上下限", async () => {
    renderPage(VIEW_ONLY, "review_day");
    await waitFor(() => expect(screen.getByTestId("approval-chain-original").textContent).toContain("变更前适用版本"));
    const text = screen.getByTestId("approval-chain-original").textContent ?? "";
    expect(text).toContain("拟变更配置");
    expect(text).toContain("日切小时下限7");
    expect(text).toContain("日切小时上限12");
    expect(text).toContain("Asia/Shanghai");
    expect(text).toContain("2099-08-01T00:00:00Z");
    expect(text).not.toMatch(/previousVersions|proposedConfig|cutoffHour|approved/);
  });

  it("证照宽限期原件：有效期止不标成合同结束日期，证照类型显示中文", async () => {
    renderPage(VIEW_ONLY, "review_grace");
    await waitFor(() => expect(screen.getByTestId("approval-chain-original").textContent).toContain("有效期止2026-10-20"));
    const text = screen.getByTestId("approval-chain-original").textContent ?? "";
    expect(text).toContain("证照类型食品经营许可证");
    expect(text).not.toContain("合同结束日期");
    expect(text).not.toContain("food_license");
    // 第十八批 I 段第 87 条 87-R4（PGM:287 零英文；PGM:529「最高权限本人」）：最高权限节点审批人不显示「USB」。
    await waitFor(() => expect(screen.getByTestId("approval-chain-node-main-1").textContent).toContain("最高权限本人"));
    expect(screen.getByTestId("approval-chain-node-main-1").textContent).not.toContain("USB");
  });

  it("真实审批角色读取完整税档原件和受控附件，不依赖税档维护权限或假证据", async () => {
    const reviewer: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view", "governance.supervisor_review.approve", "evidence.timeline.view"], denies: [], wildcard: false } };
    renderPage(reviewer, "review_tax");
    await waitFor(() => expect(screen.getByTestId("approval-chain-original")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-original").textContent).toContain("专业适用依据原文");
    expect(screen.getByTestId("approval-chain-original").textContent).toContain("1500");
    expect(screen.getByRole("link", { name: "查看原始凭证 1" })).toHaveAttribute("href", "/api/v1/evidence/attachments/proof%20tax%2F1/file");
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("approval-chain-approve"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => String(url).includes("/review_tax/approve"))).toBe(true));
    const call = fetchMock.mock.calls.find(([url]) => String(url).includes("/review_tax/approve"));
    expect(JSON.parse(String(call?.[1]?.body)).evidence_attachment_ids).toEqual(["proof tax/1"]);
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes("/staff/payroll/tax-"))).toBe(false);
  });

  it("专业认定原件显示身份、交易、来源金额和发放主体", async () => {
    renderPage(VIEW_ONLY, "review_assessment");
    await waitFor(() => expect(screen.getByTestId("approval-chain-original")).toBeTruthy());
    const text = screen.getByTestId("approval-chain-original").textContent;
    for (const fact of ["纳税身份依据", "专业纳税身份核验", "服务交易专业结论", "原收入（分）", "1000000", "核定税额（分）", "payer-1", "service-1"]) expect(text).toContain(fact);
  });

  it("未读到原待审单时不能仅凭编号审批", async () => {
    renderPage(ADMIN_PERMS, "review_missing");
    await waitFor(() => expect(screen.getByTestId("approval-chain-pending-review_c1")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-approve")).toBeDisabled();
    expect(screen.queryByTestId("approval-chain-original")).toBeNull();
  });

  it("最高权限逐笔读到跨项目原件后可确认，不要求单据出现在待审列表", async () => {
    renderPage(ADMIN_PERMS, "review_master");
    expect(screen.queryByTestId("approval-chain-pending-review_master")).toBeNull();
    await waitFor(() => expect(screen.getByTestId("approval-chain-original")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-original").textContent).toContain("专业适用依据原文");
    await waitFor(() => expect(screen.getByTestId("approval-chain-approve")).not.toBeDisabled());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("approval-chain-approve"));
    await waitFor(() => expect(vi.mocked(globalThis.fetch).mock.calls.some(([url]) => String(url).endsWith("/review_master/approve"))).toBe(true));
  });

  // 18F 复核（PGM:287「界面全中文（所有终端零英文）」）：交接链与补录页、卡座与订台页的补录行把复核单号链到本页，三类人工补录的动作类型显示中文。
  it("三类人工补录复核单的动作类型显示中文，不显示动作码", async () => {
    const backfills = [["rv_bf_trace", "inventory.trace.scan_backfill", "交接扫码补录"], ["rv_bf_sales", "bar.sales.backfill", "销售归属补录"],
      ["rv_bf_service", "bar.service.backfill", "服务责任补录"]];
    renderPage(ADMIN_PERMS, undefined, backfills.map(([id, action]) => ({ decision: { review_id: id, action_type: action, object_type: "manual_backfill",
      object_id: `mb_${id}`, initiated_by: "usr_desk", review_status: "pending_review" }, schedule: null })));
    await waitFor(() => expect(screen.queryByTestId("approval-chain-pending-rv_bf_trace")?.textContent ?? "").toContain("交接扫码补录"));
    for (const [id, action, label] of backfills) {
      const text = screen.getByTestId(`approval-chain-pending-${id}`).textContent ?? "";
      expect(text).toContain(label);
      expect(text).not.toContain(action);
    }
  });

  // 第 23 条复核（PGM:687「这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）」；PGM:686 采购价）：读到的复核单原件（含到货单价）报给外壳，
  // 已终结、不在待审列表里的也报。
  it("读到的复核单原件报为这一次看到的对象，已终结不在待审列表的也报", async () => {
    const reportViewedObjects = vi.fn();
    render(<ApprovalChainPage api={makeApi([])} user={{ id: "u1", username: "admin", accountType: "platform_admin" }} setActionResult={vi.fn()}
      setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} focusObjectId="review_final" reportViewedObjects={reportViewedObjects} />);

    await waitFor(() => expect(reportViewedObjects).toHaveBeenCalledWith("supervisor_review", [{ id: "review_final" }]));
  });

  // 第十八批 I 段第 87 条 87-R1～R3（PGM:287「状态、枚举取值……必须全部为中文」「不得以“这是标识”为由保留英文」）：待审表「动作类型」「对象」「档位」
  // 三列覆盖会建复核单的全部码，显示中文，与主管复核页同名（两页共用 reviewLabels.ts）。码闭集按三路建单调用处逐个核对：
  // ① evaluate：SupervisorReviewService.java:70-191 策略表（permission_grant_l1、l2 在 :830-832 被拒，不列）；
  // ② 准入闸：V068:65-66、V099:37-59、V541:94 中 outcome=block 且有 @GuardedAction(admissionAction) 的行（amusement.manual_override 无注解，不列）；
  // ③ AccessService 直接建单：:76（经 :723）、:1010、:1175、:1310、:1551。对象码取三路调用处传入的 objectType。
  it("待审表动作类型、对象、档位三列覆盖全部复核码，显示中文，与主管复核页同名", async () => {
    const evaluateActions = ["staff.payroll.scheme.change", "staff.payroll.period.confirm", "staff.performance.points.deduct", "staff.performance.points.appeal",
      "staff.payroll.calendar.publish", "staff.performance.rule.publish", "bar.payroll_rule.adjust", "amusement.business_day.configure", "bar.business_day.configure",
      "settlement.high_value_threshold.configure", "compliance.expiry_item.grace", "security.export.editable_copy", "inventory.purchase_request",
      "inventory.purchase_terminate", "inventory.supply_contract", "inventory.purchase_return", "inventory.purchase_prepayment_resolution", "contract.new",
      "contract.expiry_termination", "contract.change", "energy.reading_anomaly_confirm", "energy.review_settings", "dealer.delivery_resolution",
      "inventory.quarantine_disposition", "inventory.transfer_order.cancel", "inventory.transfer_order.unreturnable", "facility.equipment.scrap", "work_order.bearer_dispute",
      "leasing.blacklist_exception", "leasing.intent.key_stage", "merchant.project.add", "inventory.trace.scan_backfill", "bar.sales.backfill", "bar.service.backfill",
      "parking.release.waive", "ticket.reward.rule", "ticket.reward.national", "ticket.reward.sales_contract", "staff.shift.publish", "staff.attendance.receipt.review",
      "staff.payroll.penalty_cap.adjust", "manual_release", "bill_waiver", "refund", "ar_ap_adjustment", "contract_violation", "base_definition_violation",
      "permission_grant_l3", "finance.tax_rule.adjust", "staff.profile.employment_status.change", "staff.payroll.adjustment.add", "staff.payroll.adjustment.appeal",
      "bar.table_session.transfer_over_limit"];
    const admissionActions = ["bar.bill_waiver", "amusement.reopen", "bar.bill_reopen", "amusement.incident_close", "amusement.group_booking_approve",
      "bar.order_item_void", "bar.shift_close", "ticketing.refund_approve", "ticketing.dynamic_pricing_publish", "ticketing.verification_reverse",
      "inventory.stock_adjust", "inventory.purchase_receive", "inventory.saved_bottle_claim", "finance.refund", "finance.offline_payment_confirm",
      "finance.locked_backfill_approve", "customer.home_content.publish"];
    const accessActions = ["access.role_viewable_projects.reduce", "access.position_role_template.delete", "access.scope_change", "access.scope_change.money",
      "access.grant_request", "access.grant_request.money"];
    const objectTypes = ["staff_profile", "staff_payroll_adjustment", "staff_payroll_adjustment_appeal", "contract", "bar_bill", "access_grant_request",
      "facility_equipment_scrap", "work_order_bearer_dispute", "parking_manual_release", "leasing_blacklist_exception", "leasing_intent", "merchant_project_request", "compliance_expiry_item",
      "export_file", "manual_backfill", "ar_ap_adjustment", "role_viewable_projects", "contract_change", "contract_settlement", "ticket_reward_config",
      "ticket_points_billing_rule", "ticket_points_calendar", "ticket_points_portal_outage", "ticket_points_objection_resolution", "purchase_prepayment_resolution",
      "inventory_transfer_order", "inventory_quarantine_disposition", "supply_contract", "inventory_purchase_request", "purchase_quantity_resolution",
      "purchase_return", "amusement_business_day_config", "energy_reading_verification", "energy_review_settings", "high_value_threshold",
      "dealer_return_freight", "dealer_return", "dealer_order_refund", "dealer_delivery_resolution", "staff_performance_score", "staff_payroll_scheme",
      "staff_payroll_calendar", "staff_payroll_period", "staff_performance_rule", "staff_attendance_receipt_review", "payroll_tax_version",
      "payroll_tax_assessment", "staff_shift_batch", "staff_penalty_cap", "bar_payroll_rule", "bar_business_day_config", "inspection",
      "amusement_maintenance_record", "amusement_incident", "amusement_group_booking", "bar_order_item", "bar_shift", "ticket_refund",
      "ticket_dynamic_pricing_rule", "verification", "inventory_stock_adjustment", "saved_bottle", "finance_refund", "finance_offline_payment_confirmation",
      "customer_home_content", "position_role_template", "bar_table_transfer_limit_approval"];
    const levels = ["one_level_up", "two_levels_up", "top_authority", "specific_role", "hr_three_levels_up"];
    const actions = [...evaluateActions, ...admissionActions, ...accessActions];
    const rows = actions.map((action, i) => ({ decision: { review_id: `rv87_${i}`, action_type: action, object_type: objectTypes[i % objectTypes.length],
      object_id: `obj_${i}`, initiated_by: "usr_l1", approval_level: levels[i % levels.length], review_status: "pending_review" }, schedule: null }));
    renderPage(ADMIN_PERMS, undefined, rows);
    await waitFor(() => expect(screen.queryByTestId(`approval-chain-pending-rv87_${actions.length - 1}`)).toBeTruthy());
    const cells = (id: string) => Array.from(screen.getByTestId(`approval-chain-pending-${id}`).children).map((cell) => cell.textContent ?? "");
    rows.forEach(({ decision }, i) => {
      const [, action = "", object = "", , level = ""] = cells(decision.review_id);
      const [objectLabel, objectId] = object.split(" / ");
      for (const label of [action, objectLabel, level]) {
        expect(label, `${decision.action_type} / ${decision.object_type} / ${decision.approval_level}`).not.toMatch(/[A-Za-z]/);
        expect(label, `${decision.action_type} / ${decision.object_type} / ${decision.approval_level}`).not.toBe("未识别类型");
      }
      expect(objectId).toBe(`obj_${i}`);
    });
    expect(cells(`rv87_${actions.indexOf("staff.shift.publish")}`)[1]).toBe("发布排班");
    expect(cells(`rv87_${actions.indexOf("bar.bill_waiver")}`)[1]).toBe("酒吧账单免单"); // 第十八批裁决 47
    expect(cells(`rv87_${actions.indexOf("merchant.project.add")}`)[1]).toBe("商户加经营项目");
    const licence = objectTypes.indexOf("compliance_expiry_item");
    expect(cells(`rv87_${licence}`)[2]).toBe(`证照 / obj_${licence}`);
    expect(cells("review_c1")[2]).toBe("合同 / c1");
    expect(cells("review_c1")[4]).toBe("两级上级审批");
    expect(cells(`rv87_${levels.indexOf("hr_three_levels_up")}`)[4]).toBe("人力会签＋直属领导向上三级复核");
  });

  it("逐笔原件已经终结时仍可查看但不能再次裁决", async () => {
    renderPage(ADMIN_PERMS, "review_final");
    await waitFor(() => expect(screen.getByTestId("approval-chain-original")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-approve")).toBeDisabled();
    expect(screen.getByTestId("approval-chain-reject")).toBeDisabled();
  });
});
