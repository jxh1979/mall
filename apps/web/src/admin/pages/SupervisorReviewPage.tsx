import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, SupervisorReviewPolicyRow } from "../types";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

const TEXT = {
  title: "主管复核与违规阻断",
  description: "覆盖高风险业务动作的默认阻断、主管复核评估和审批放行留痕；所有放行动作必须带幂等键、证据附件和审批意见。",
  evidence: "接口证据：读取 /api/v1/platform/supervisor-review-policies；评估 POST /api/v1/platform/supervisor-review/evaluate；审批 POST /api/v1/platform/supervisor-review/{review_id}/approve。",
  loadFailed: "主管复核策略读取失败",
  actionFailed: "主管复核操作失败",
  total: "策略总数",
  defaultBlocked: "默认阻断",
  active: "启用策略",
  maxLevel: "最高级别",
  actionType: "动作类型",
  objectType: "对象类型",
  objectId: "对象 ID",
  amountCent: "金额(分)",
  violationSummary: "违规说明",
  reviewerLevel: "主管级别",
  approvalComment: "审批意见",
  evaluate: "评估违规动作",
  approve: "主管审批放行",
  boundary: "违规动作必须先阻断并生成待复核记录；主管审批只能放行当前 review_id，不得绕过证据附件、幂等键和审计留痕。",
  policy: "策略",
  requiredEvidence: "所需证据",
  status: "状态",
  focusClear: "清除筛选，返回全部",
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (Array.isArray(value)) return value.length ? value.join(", ") : "-";
  return String(value);
}

// M007-A-05（PGM-1.18）：主壳对对象型结果原样 JSON 上屏，这里只交中文句子（复核单号为内部标识）。
function makeResult(action: string, _request: Record<string, unknown>, result: unknown) {
  const reviewId = reviewIdFrom(result) || "未生成";
  if (action === "supervisor_review_evaluated") {
    const blocked = Boolean((result as { blocked?: unknown } | null)?.blocked);
    return blocked ? `已生成待复核记录：${reviewId}` : `评估结果：允许，无需复核（${reviewId}）`;
  }
  return `复核单 ${reviewId} 已审批通过`;
}

function reviewIdFrom(result: unknown) {
  if (result && typeof result === "object" && "reviewId" in result) return String((result as { reviewId?: unknown }).reviewId ?? "");
  return "";
}

const SUPERVISOR_REVIEW_PERMISSION = "governance.supervisor_review.manage";
// M007-F-08（PGM-1.10 一个单一功能一个权限点）：评估用 manage，审批用 approve，与后端端点门禁一致。
const SUPERVISOR_APPROVE_PERMISSION = "governance.supervisor_review.approve";

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染策略表对应子集。
// URL 约定：?kpi=policies[&subset=blocked|active]。
type KpiFocus = { subset: "all" | "blocked" | "active" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "policies") return null;
  const subset = params.get("subset");
  return { subset: subset === "blocked" || subset === "active" ? subset : "all" };
}

function focusTitle(subset: KpiFocus["subset"]): string {
  if (subset === "blocked") return TEXT.defaultBlocked;
  if (subset === "active") return TEXT.active;
  return TEXT.total;
}

export function SupervisorReviewPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SupervisorReviewPolicyRow[]>([]);
  const [latestReviewId, setLatestReviewId] = React.useState("");
  const [actionType, setActionType] = React.useState("contract_violation");
  const [objectType, setObjectType] = React.useState("contract");
  const [objectId, setObjectId] = React.useState("contract-1");
  const [amountCent, setAmountCent] = React.useState("0");
  const [violationSummary, setViolationSummary] = React.useState("合同主体与基础定义不一致");
  const [reviewerLevel, setReviewerLevel] = React.useState("3");
  const [approvalComment, setApprovalComment] = React.useState("证据齐备，同意放行");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该复核策略完整字段（枚举中文·布尔中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: KpiFocus["subset"]) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "policies");
    if (subset !== "all") params.set("subset", subset);
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<SupervisorReviewPolicyRow[]>("/api/v1/platform/supervisor-review-policies"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const blockedRows = rows.filter((row) => row.blocksByDefault);
  const activeRows = rows.filter((row) => (row.status ?? "active") === "active");
  const blockedCount = blockedRows.length;
  const activeCount = activeRows.length;
  const maxRequiredLevel = rows.reduce((max, row) => Math.max(max, row.requiredLevel ?? 0), 0);
  const hasPendingReview = latestReviewId.trim().length > 0;

  const focusRows = kpiFocus?.subset === "blocked" ? blockedRows : kpiFocus?.subset === "active" ? activeRows : rows;

  function resetPendingReview() {
    if (latestReviewId) setLatestReviewId("");
  }

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
    onSuccess: (result) => {
      const nextReviewId = reviewIdFrom(result);
      if (nextReviewId) setLatestReviewId(nextReviewId);
    },
  });

  async function evaluateViolation() {
    if (!violationSummary.trim()) {
      setFormError("违规说明不能为空");
      return;
    }
    const request = {
      idempotencyKey: nextKey("supervisor-review-evaluate"),
      operatorNote: "完整后台主管复核评估",
      evidenceAttachmentIds: ["att-supervisor-review-evaluate"],
      actionType,
      objectType,
      objectId,
      amountCent: Number(amountCent || 0),
      violationSummary,
    };
    await runAction("supervisor_review_evaluated", request, () => api.post("/api/v1/platform/supervisor-review/evaluate", request));
  }

  function validateApprove() {
    if (!hasPendingReview) {
      setFormError("请先评估违规动作并生成待复核记录");
      return false;
    }
    if (!approvalComment.trim()) {
      setFormError("审批意见不能为空");
      return false;
    }
    return true;
  }

  async function approveReview() {
    if (!validateApprove()) {
      return;
    }
    const request = {
      idempotencyKey: nextKey("supervisor-review-approve"),
      operatorNote: "完整后台主管审批放行",
      evidenceAttachmentIds: ["att-supervisor-review-approve"],
      reviewerLevel: Number(reviewerLevel || 3),
      decision: "approved",
      comment: approvalComment,
    };
    await runAction("supervisor_review_approved", request, () => api.post(`/api/v1/platform/supervisor-review/${latestReviewId}/approve`, request));
  }

  function openPolicyDetail(row: SupervisorReviewPolicyRow) {
    setDetail({
      title: `${TEXT.policy}明细 · ${row.policyCode ?? row.id}`,
      fields: [
        { label: TEXT.policy, value: row.policyCode ?? row.id },
        { label: TEXT.actionType, value: formatBusinessStatus(row.actionType) },
        { label: TEXT.reviewerLevel, value: displayValue(row.requiredLevel) },
        { label: TEXT.defaultBlocked, value: displayValue(row.blocksByDefault) },
        { label: TEXT.requiredEvidence, value: displayValue(row.requiredEvidenceTypes) },
        { label: TEXT.status, value: formatBusinessStatus(row.status) },
      ],
    });
  }

  const policyTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="supervisor-review-table">
        <thead><tr><th>{TEXT.policy}</th><th>{TEXT.actionType}</th><th>{TEXT.reviewerLevel}</th><th>{TEXT.defaultBlocked}</th><th>{TEXT.requiredEvidence}</th><th>{TEXT.status}</th><th>详情</th></tr></thead>
        <tbody>{(kpiFocus ? focusRows : rows).map((row) => (
          <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`supervisor-review-row-${row.id}`}
            onClick={() => openPolicyDetail(row)}
            onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
            <td>{row.policyCode ?? row.id}</td>
            <td>{formatBusinessStatus(row.actionType)}</td>
            <td>{displayValue(row.requiredLevel)}</td>
            <td>{displayValue(row.blocksByDefault)}</td>
            <td>{displayValue(row.requiredEvidenceTypes)}</td>
            <td>{formatBusinessStatus(row.status)}</td>
          <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看复核策略详情" onClick={(event) => { event.stopPropagation(); openPolicyDetail(row); }}>查看详情</button></td></tr>
        ))}</tbody>
      </table>
    </div>
  );

  return (
    <section data-testid="supervisor-review-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="supervisor-review-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="governance.supervisor_review.policy_read / evaluated / approved" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="supervisor-review-kpi-focus-banner">
              正在查看：{focusTitle(kpiFocus.subset)}（{focusRows.length}）
              <button type="button" data-testid="supervisor-review-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {policyTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="supervisor-review-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.total}</span><button type="button" className="admin-kpi-value" data-testid="supervisor-review-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>{TEXT.policy}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.defaultBlocked}</span><button type="button" className="admin-kpi-value" data-testid="supervisor-review-kpi-blocked" onClick={() => enterKpiFocus("blocked")}>{blockedCount}</button><small>默认拦截</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.active}</span><button type="button" className="admin-kpi-value" data-testid="supervisor-review-kpi-active" onClick={() => enterKpiFocus("active")}>{activeCount}</button><small>{TEXT.status}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.maxLevel}</span><strong>{maxRequiredLevel}</strong><small>{TEXT.reviewerLevel}</small></div>
            </div>

            <div className="admin-form-grid" data-testid="supervisor-review-form">
              <label>{TEXT.actionType}<select data-testid="supervisor-review-action-type" value={actionType} onChange={(event) => { setActionType(event.target.value); resetPendingReview(); }}><option value="contract_violation">合同违规</option><option value="base_definition_violation">基础定义违规</option><option value="bill_waiver">免单</option><option value="refund">退款</option><option value="ar_ap_adjustment">调账</option><option value="manual_release">人工放行</option></select></label>
              <label>{TEXT.objectType}<input data-testid="supervisor-review-object-type" value={objectType} onChange={(event) => { setObjectType(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.objectId}<input data-testid="supervisor-review-object-id" value={objectId} onChange={(event) => { setObjectId(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.amountCent}<input data-testid="supervisor-review-amount-cent" type="number" value={amountCent} onChange={(event) => { setAmountCent(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.violationSummary}<input data-testid="supervisor-review-violation-summary" value={violationSummary} onChange={(event) => { setViolationSummary(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.reviewerLevel}<input data-testid="supervisor-review-reviewer-level" type="number" value={reviewerLevel} onChange={(event) => setReviewerLevel(event.target.value)} /></label>
              <label>{TEXT.approvalComment}<input data-testid="supervisor-review-approval-comment" value={approvalComment} onChange={(event) => setApprovalComment(event.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="supervisor-review-actions">
              <PermissionButton
                permissionsState={effectivePermissions}
                permissions={[SUPERVISOR_REVIEW_PERMISSION]}
                disabled={busyAction !== ""}
                data-testid="supervisor-review-evaluate"
                onClick={() => void evaluateViolation()}
              >{TEXT.evaluate}</PermissionButton>
              <HighRiskConfirm
                label={TEXT.approve}
                impact={`将对复核单 ${latestReviewId || "未生成"} 记录审批通过结论并写入主管审批留痕。`}
                disabled={busyAction !== "" || !hasPendingReview}
                precheck={validateApprove}
                onConfirm={() => void approveReview()}
                permissionsState={effectivePermissions}
                permissions={[SUPERVISOR_APPROVE_PERMISSION]}
                data-testid="supervisor-review-approve"
              />
            </div>
            <div className="admin-muted-action" data-testid="supervisor-review-current-review">当前待审批 review_id：{latestReviewId || "未生成"}</div>
            <div className="admin-muted-action" data-testid="supervisor-review-boundary">{TEXT.boundary}</div>
            {formError ? <div data-testid="supervisor-review-error" className="admin-inline-error">{formError}</div> : null}

            {policyTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
