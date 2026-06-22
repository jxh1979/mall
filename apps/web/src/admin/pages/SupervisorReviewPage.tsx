import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, SupervisorReviewPolicyRow } from "../types";

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

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, evidenceAttachmentIds: request.evidenceAttachmentIds, result };
}

function reviewIdFrom(result: unknown) {
  if (result && typeof result === "object" && "reviewId" in result) return String((result as { reviewId?: unknown }).reviewId ?? "");
  return "";
}

const SUPERVISOR_REVIEW_PERMISSION = "governance.supervisor_review.manage";

export function SupervisorReviewPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SupervisorReviewPolicyRow[]>([]);
  const [latestReviewId, setLatestReviewId] = React.useState("review-demo");
  const [actionType, setActionType] = React.useState("contract_violation");
  const [objectType, setObjectType] = React.useState("contract");
  const [objectId, setObjectId] = React.useState("contract-1");
  const [amountCent, setAmountCent] = React.useState("0");
  const [violationSummary, setViolationSummary] = React.useState("合同主体与基础定义不一致");
  const [reviewerLevel, setReviewerLevel] = React.useState("3");
  const [approvalComment, setApprovalComment] = React.useState("证据齐备，同意放行");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<SupervisorReviewPolicyRow[]>("/api/v1/platform/supervisor-review-policies"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const blockedCount = rows.filter((row) => row.blocksByDefault).length;
  const activeCount = rows.filter((row) => (row.status ?? "active") === "active").length;
  const maxRequiredLevel = rows.reduce((max, row) => Math.max(max, row.requiredLevel ?? 0), 0);

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

  return (
    <section data-testid="supervisor-review-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="supervisor-review-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="governance.supervisor_review.policy_read / evaluated / approved" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="supervisor-review-summary">
          <div className="admin-kpi-card"><span>{TEXT.total}</span><strong>{rows.length}</strong><small>{TEXT.policy}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.defaultBlocked}</span><strong>{blockedCount}</strong><small>blocksByDefault</small></div>
          <div className="admin-kpi-card"><span>{TEXT.active}</span><strong>{activeCount}</strong><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.maxLevel}</span><strong>{maxRequiredLevel}</strong><small>{TEXT.reviewerLevel}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="supervisor-review-form">
          <label>{TEXT.actionType}<select data-testid="supervisor-review-action-type" value={actionType} onChange={(event) => setActionType(event.target.value)}><option value="contract_violation">contract_violation</option><option value="base_definition_violation">base_definition_violation</option><option value="billing_discount">billing_discount</option><option value="manual_release">manual_release</option></select></label>
          <label>{TEXT.objectType}<input data-testid="supervisor-review-object-type" value={objectType} onChange={(event) => setObjectType(event.target.value)} /></label>
          <label>{TEXT.objectId}<input data-testid="supervisor-review-object-id" value={objectId} onChange={(event) => setObjectId(event.target.value)} /></label>
          <label>{TEXT.amountCent}<input data-testid="supervisor-review-amount-cent" type="number" value={amountCent} onChange={(event) => setAmountCent(event.target.value)} /></label>
          <label>{TEXT.violationSummary}<input data-testid="supervisor-review-violation-summary" value={violationSummary} onChange={(event) => setViolationSummary(event.target.value)} /></label>
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
            impact={`将放行复核记录 ${latestReviewId}，被阻断的高风险动作随即生效并写入主管审批留痕。`}
            disabled={busyAction !== ""}
            precheck={validateApprove}
            onConfirm={() => void approveReview()}
            permissionsState={effectivePermissions}
            permissions={[SUPERVISOR_REVIEW_PERMISSION]}
            data-testid="supervisor-review-approve"
          />
        </div>
        <div className="admin-muted-action" data-testid="supervisor-review-boundary">{TEXT.boundary}</div>
        {formError ? <div data-testid="supervisor-review-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="supervisor-review-table">
            <thead><tr><th>{TEXT.policy}</th><th>{TEXT.actionType}</th><th>{TEXT.reviewerLevel}</th><th>{TEXT.defaultBlocked}</th><th>{TEXT.requiredEvidence}</th><th>{TEXT.status}</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id}>
                <td>{row.policyCode ?? row.id}</td>
                <td>{row.actionType ?? "-"}</td>
                <td>{displayValue(row.requiredLevel)}</td>
                <td>{displayValue(row.blocksByDefault)}</td>
                <td>{displayValue(row.requiredEvidenceTypes)}</td>
                <td>{row.status ?? "-"}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
