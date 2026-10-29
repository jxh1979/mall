// 文件版本：V0.09
import React from "react";
import { withBase } from "../../basePath";
import { formatReviewValue } from "../i18n/reviewLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, SupervisorReviewPolicyRow } from "../types";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { ManualBackfillPolicySection } from "./ManualBackfillPolicySection";

const TEXT = {
  title: "主管复核与违规阻断",
  description: "覆盖高风险业务动作的默认阻断、主管复核评估和审批放行留痕；放行动作必须带幂等键和审批意见，按策略提供证据附件。",
  evidence: "接口证据：读取主管复核策略、提交评估、提交审批。路径为系统内部接口。",
  loadFailed: "主管复核策略读取失败",
  actionFailed: "主管复核操作失败",
  total: "策略总数",
  defaultBlocked: "默认阻断",
  active: "启用策略",
  maxLevel: "最高级别",
  actionType: "动作类型",
  objectType: "对象类型",
  objectId: "对象编号",
  amountCent: "金额（分）",
  beneficiary: "直接受益人用户编号（不是登录名）",
  evidenceAttachment: "证据附件编号",
  forwardHint: "填写用户编号（不是登录名）；不得转交给申请人或直接受益人",
  violationSummary: "违规说明",
  reviewerLevel: "所需主管级别",
  approvalComment: "审批意见",
  evaluate: "评估违规动作",
  approve: "主管审批放行",
  reject: "主管驳回退回起草人",
  boundary: "违规动作必须先阻断并生成待复核记录；主管审批只能放行当前复核单，不得绕过策略要求的证据、幂等键和审计留痕。",
  policy: "策略",
  requiredEvidence: "所需证据",
  status: "状态",
  focusClear: "清除筛选，返回全部",
  pending: "待复核清单",
  noPending: "当前没有待复核记录",
  dueAt: "到期",
  forwardTarget: "转交给（用户编号，不是登录名）",
  forward: "转交审批",
  selectForForward: "转交",
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
function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  const reviewId = reviewIdFrom(result) || "未生成";
  if (action === "supervisor_review_forwarded") {
    return `复核单 ${reviewId} 已转交给 ${String(request.forwardedTo ?? "")}`;
  }
  if (action === "supervisor_review_evaluated") {
    const blocked = Boolean((result as { blocked?: unknown } | null)?.blocked);
    return blocked ? `已生成待复核记录：${reviewId}` : `评估结果：服务端判定本次不走本页复核（${reviewId}）`;
  }
  if (action === "supervisor_review_rejected") {
    return `复核单 ${reviewId} 已驳回，整单退回起草人`;
  }
  return (result as { reviewStatus?: string } | null)?.reviewStatus === "approved"
    ? `复核单 ${reviewId} 全部节点已通过，单据生效`
    : `复核单 ${reviewId} 当前节点已通过，等待后续节点审批`;
}

function reviewIdFrom(result: unknown) {
  if (result && typeof result === "object" && "reviewId" in result) return String((result as { reviewId?: unknown }).reviewId ?? "");
  return "";
}

function sameAccount(candidate: string, actor?: { id?: string; username?: string } | null) {
  const value = candidate.trim();
  if (!value || !actor) return false;
  return Boolean(actor.id && value === actor.id) || Boolean(actor.username && value === actor.username);
}

const SUPERVISOR_REVIEW_PERMISSION = "governance.supervisor_review.manage";
// M007-F-08（PGM-1.10 一个单一功能一个权限点）：评估用 manage，审批用 approve，与后端端点门禁一致。
const SUPERVISOR_APPROVE_PERMISSION = "governance.supervisor_review.approve";

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染策略表对应子集。
// URL 约定：?kpi=policies[&subset=blocked|active]。
type KpiFocus = { subset: "all" | "blocked" | "active" };
type PendingRow = {
  decision: { reviewId: string; policyCode?: string | null; actionType?: string | null; objectType?: string | null; objectId?: string | null; initiatedBy?: string | null; approvalLevel?: string | null; requiredLevel?: number | null };
  schedule?: { dueAt?: string | null; escalationCount?: number | null; forwardedTo?: string | null; beneficiaryId?: string | null } | null;
};

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

export function SupervisorReviewPage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SupervisorReviewPolicyRow[]>([]);
  const [pendingRows, setPendingRows] = React.useState<PendingRow[]>([]);
  const [forwardReviewId, setForwardReviewId] = React.useState("");
  const [forwardTarget, setForwardTarget] = React.useState("");
  const [latestReviewId, setLatestReviewId] = React.useState("");
  const [actionType, setActionType] = React.useState("contract_violation");
  const [objectType, setObjectType] = React.useState("合同");
  const [objectId, setObjectId] = React.useState("contract-1");
  const [amountCent, setAmountCent] = React.useState("0");
  const [violationSummary, setViolationSummary] = React.useState("");
  const [beneficiaryId, setBeneficiaryId] = React.useState("");
  const [evidenceAttachment, setEvidenceAttachment] = React.useState("");
  const [approvalComment, setApprovalComment] = React.useState("");
  // M087-A-02（PGM-1.7.1「发起单据时只需指定该事项的最终审批人」）：可空，空则按档位下限沿直接上级链生成。
  const [finalApproverId, setFinalApproverId] = React.useState("");
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
      setPendingRows(await api.get<PendingRow[]>("/api/v1/platform/supervisor-review/pending"));
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
    const finalApprover = finalApproverId.trim();
    const beneficiary = beneficiaryId.trim();
    if (["bill_waiver", "refund", "ar_ap_adjustment"].includes(actionType) && !beneficiary) {
      setFormError("请填写直接受益人用户编号");
      return;
    }
    if (finalApprover && sameAccount(finalApprover, user)) {
      setFormError("不能指定申请人为最终审批人");
      return;
    }
    if (finalApprover && beneficiary && finalApprover === beneficiary) {
      setFormError("不能指定直接受益人为最终审批人");
      return;
    }
    const request = {
      idempotencyKey: nextKey("supervisor-review-evaluate"),
      operatorNote: "完整后台主管复核评估",
      evidenceAttachmentIds: evidenceAttachment.trim() ? [evidenceAttachment.trim()] : [],
      actionType,
      objectType,
      objectId,
      amountCent: Number(amountCent || 0),
      violationSummary,
      finalApproverId: finalApproverId.trim() || undefined,
      beneficiaryId: beneficiaryId.trim() || undefined,
    };
    await runAction("supervisor_review_evaluated", request, () => api.post("/api/v1/platform/supervisor-review/evaluate", request));
  }

  // M007-A-02（PGM-1.7.1 转发：不限层级，但不得转给申请人本人或直接受益人——由后端校验）。
  function selectedPending(reviewId: string) {
    return pendingRows.find((row) => row.decision.reviewId === reviewId);
  }

  async function forwardReview() {
    if (!forwardReviewId || !forwardTarget.trim()) {
      setFormError("请先选择待复核记录并填写转交用户编号（不是登录名）");
      return;
    }
    const pending = selectedPending(forwardReviewId);
    const target = forwardTarget.trim();
    if (!pending?.decision.initiatedBy) {
      setFormError("申请人未登记，不能转交");
      return;
    }
    if (target === pending.decision.initiatedBy || (sameAccount(pending.decision.initiatedBy, user) && sameAccount(target, user))) {
      setFormError("不得转交给申请人本人");
      return;
    }
    if (pending.schedule?.beneficiaryId && (target === pending.schedule.beneficiaryId || (sameAccount(pending.schedule.beneficiaryId, user) && sameAccount(target, user)))) {
      setFormError("不得转交给直接受益人");
      return;
    }
    const request = {
      idempotencyKey: nextKey("supervisor-review-forward"),
      operatorNote: "后台转交审批",
      forwardedTo: forwardTarget.trim(),
      comment: approvalComment,
    };
    await runAction("supervisor_review_forwarded", request, () => api.post(`/api/v1/platform/supervisor-review/${forwardReviewId}/forward`, request));
  }

  function validateDecisionActor() {
    if (!hasPendingReview) {
      setFormError("请先评估违规动作并生成待复核记录");
      return false;
    }
    const pending = selectedPending(latestReviewId);
    if (pending && !pending.decision.initiatedBy) {
      setFormError("申请人未登记，不能放行或驳回");
      return false;
    }
    const initiator = pending?.decision.initiatedBy ?? user?.id;
    if (initiator && sameAccount(initiator, user)) {
      setFormError("不能复核自己发起的单");
      return false;
    }
    if (pending?.schedule?.beneficiaryId && sameAccount(pending.schedule.beneficiaryId, user)) {
      setFormError("直接受益人不能复核与自己相关的申请");
      return false;
    }
    if (!approvalComment.trim()) {
      setFormError("审批意见不能为空");
      return false;
    }
    return true;
  }

  function validateDecision() {
    if (!validateDecisionActor()) {
      return false;
    }
    const pending = selectedPending(latestReviewId);
    const policy = rows.find((row) => row.policyCode === pending?.decision.policyCode);
    const needsAttachment = policy?.requiredEvidenceTypes?.includes("evidence_attachment");
    if (needsAttachment && !evidenceAttachment.trim()) {
      setFormError("请填写证据附件编号");
      return false;
    }
    return true;
  }

  async function approveReview() {
    if (!validateDecision()) {
      return;
    }
    const request = {
      idempotencyKey: nextKey("supervisor-review-approve"),
      operatorNote: "完整后台主管审批放行",
      evidenceAttachmentIds: evidenceAttachment.trim() ? [evidenceAttachment.trim()] : [],
      decision: "approved",
      comment: approvalComment,
    };
    await runAction("supervisor_review_approved", request, () => api.post(`/api/v1/platform/supervisor-review/${latestReviewId}/approve`, request));
  }
  // M007-R6-03（PGM-1.7.1「任一节点驳回则整单退回起草人」）：驳回当前节点，整单退回起草人。
  async function rejectReview() {
    if (!validateDecision()) {
      return;
    }
    const request = {
      idempotencyKey: nextKey("supervisor-review-reject"),
      operatorNote: "完整后台主管驳回",
      evidenceAttachmentIds: evidenceAttachment.trim() ? [evidenceAttachment.trim()] : [],
      decision: "rejected",
      comment: approvalComment,
    };
    await runAction("supervisor_review_rejected", request, () => api.post(`/api/v1/platform/supervisor-review/${latestReviewId}/approve`, request));
  }

  // 第十三批裁决第 5 条「上级审批后发布：运营编辑后走审批，批准才对客人展示」：审批客人端首页内容前先读这次要发布的原文（不盲批）。
  async function openHomeContentDraft(projectId: string) {
    try {
      const content = await api.get<{ intro?: string | null; announcement?: string | null; draftIntro?: string | null;
        draftAnnouncement?: string | null; draftSubmittedBy?: string | null; draftSubmittedAt?: string | null }>(
        `/api/v1/home-contents/${encodeURIComponent(projectId)}`);
      setDetail({
        title: `待发布的客人端首页内容 · ${projectId}`,
        fields: [
          { label: "综合体介绍（待发布）", value: content?.draftIntro || "（无）" },
          { label: "活动公告（待发布）", value: content?.draftAnnouncement || "（无）" },
          { label: "提交人", value: content?.draftSubmittedBy ?? "-" },
          { label: "提交时间", value: content?.draftSubmittedAt ? cnDate(content.draftSubmittedAt) : "-" },
          { label: "当前已发布的介绍", value: content?.intro || "（尚未发布）" },
        ],
      });
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "读取待发布内容失败");
    }
  }

  function openPolicyDetail(row: SupervisorReviewPolicyRow) {
    setDetail({
      title: `${TEXT.policy}明细 · ${row.policyCode ?? row.id}`,
      fields: [
        { label: TEXT.policy, value: row.policyCode ?? row.id },
        { label: TEXT.actionType, value: formatReviewValue(row.actionType) },
        { label: TEXT.reviewerLevel, value: displayValue(row.requiredLevel) },
        { label: TEXT.defaultBlocked, value: displayValue(row.blocksByDefault) },
        { label: TEXT.requiredEvidence, value: Array.isArray(row.requiredEvidenceTypes) ? row.requiredEvidenceTypes.map((item) => formatReviewValue(String(item))).join("、") : displayValue(row.requiredEvidenceTypes) },
        { label: TEXT.status, value: formatReviewValue(row.status) },
      ],
    });
  }

  const policyTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="supervisor-review-table">
        <thead><tr><th>{TEXT.policy}</th><th>{TEXT.actionType}</th><th>{TEXT.reviewerLevel}</th><th>{TEXT.defaultBlocked}</th><th>{TEXT.requiredEvidence}</th><th>{TEXT.status}</th><th>详情</th></tr></thead>
        <tbody>{(kpiFocus ? focusRows : rows).map((row) => (
          <tr key={row.policyCode} className="admin-row-clickable" tabIndex={0} data-testid={`supervisor-review-row-${row.policyCode}`}
            onClick={() => openPolicyDetail(row)}
            onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}>
            <td>{row.policyCode ?? row.id}</td>
            <td>{formatReviewValue(row.actionType)}</td>
            <td>{displayValue(row.requiredLevel)}</td>
            <td>{displayValue(row.blocksByDefault)}</td>
            <td>{Array.isArray(row.requiredEvidenceTypes) ? row.requiredEvidenceTypes.map((item) => formatReviewValue(String(item))).join("、") : displayValue(row.requiredEvidenceTypes)}</td>
            <td>{formatReviewValue(row.status)}</td>
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

            <h3>{TEXT.pending}</h3>
            {pendingRows.length === 0 ? <div className="admin-muted-action" data-testid="supervisor-review-pending-empty">{TEXT.noPending}</div> : (
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="supervisor-review-pending">
                  <thead><tr><th>复核单</th><th>{TEXT.actionType}</th><th>对象</th><th>发起人</th><th>{TEXT.beneficiary}</th><th>审批层级</th><th>{TEXT.dueAt}</th><th>状态</th><th></th></tr></thead>
                  <tbody>{pendingRows.map((row) => (
                    <tr key={row.decision.reviewId} data-testid={`supervisor-review-pending-${row.decision.reviewId}`}>
                      <td>{row.decision.reviewId}</td>
                      <td>{formatReviewValue(row.decision.actionType ?? "")}</td>
                      <td>{formatReviewValue(row.decision.objectType ?? "")} / {row.decision.objectId ?? "-"}</td>
                      <td>{row.decision.initiatedBy ?? "-"}</td>
                      <td>{row.schedule?.beneficiaryId || "-"}</td>
                      <td>{formatReviewValue(row.decision.approvalLevel ?? "")}（主管级 {displayValue(row.decision.requiredLevel)}）</td>
                      <td>{row.schedule?.dueAt ? cnDate(row.schedule.dueAt) : "-"}</td>
                      <td>{(row.schedule?.escalationCount ?? 0) > 0 ? `已升级 ${row.schedule?.escalationCount} 次` : "待审"}{row.schedule?.forwardedTo ? `，已转交给 ${row.schedule.forwardedTo}` : ""}</td>
                      <td>{row.decision.objectType === "customer_home_content" && row.decision.objectId ? <button type="button" data-testid={`supervisor-review-home-content-${row.decision.reviewId}`} onClick={() => void openHomeContentDraft(row.decision.objectId as string)}>查看待发布内容</button> : null} <a data-testid={`supervisor-review-chain-link-${row.decision.reviewId}`} href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.decision.reviewId)}`)}>审批链</a> <button type="button" data-testid={`supervisor-review-pending-select-${row.decision.reviewId}`} onClick={() => { setForwardReviewId(row.decision.reviewId); setLatestReviewId(row.decision.reviewId); }}>{TEXT.selectForForward}</button></td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            )}
            <div className="admin-inline-form" data-testid="supervisor-review-forward-form">
              <span>{forwardReviewId ? `转交复核单 ${forwardReviewId}` : "未选择复核单"}</span>
              <input aria-label={TEXT.forwardTarget} data-testid="supervisor-review-forward-target" placeholder={TEXT.forwardHint} value={forwardTarget} onChange={(event) => setForwardTarget(event.target.value)} />
              <PermissionButton permissionsState={effectivePermissions} permissions={[SUPERVISOR_APPROVE_PERMISSION]} disabled={busyAction !== "" || !forwardReviewId} data-testid="supervisor-review-forward" onClick={() => void forwardReview()}>{TEXT.forward}</PermissionButton>
            </div>

            <div className="admin-form-grid" data-testid="supervisor-review-form">
              <label>{TEXT.actionType}<select data-testid="supervisor-review-action-type" value={actionType} onChange={(event) => { setActionType(event.target.value); resetPendingReview(); }}><option value="contract_violation">合同违规</option><option value="base_definition_violation">基础定义违规</option><option value="bill_waiver">免单</option><option value="refund">退款</option><option value="ar_ap_adjustment">调账</option><option value="manual_release">人工放行</option></select></label>
              <label>{TEXT.objectType}<input data-testid="supervisor-review-object-type" value={objectType} onChange={(event) => { setObjectType(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.objectId}<input data-testid="supervisor-review-object-id" value={objectId} onChange={(event) => { setObjectId(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.amountCent}<input data-testid="supervisor-review-amount-cent" type="number" value={amountCent} onChange={(event) => { setAmountCent(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.violationSummary}<input data-testid="supervisor-review-violation-summary" value={violationSummary} onChange={(event) => { setViolationSummary(event.target.value); resetPendingReview(); }} /></label>
              <label>主线最终审批人用户编号（不是登录名，可空）<input data-testid="supervisor-review-final-approver" value={finalApproverId} onChange={(event) => { setFinalApproverId(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.beneficiary}<input data-testid="supervisor-review-beneficiary" value={beneficiaryId} onChange={(event) => { setBeneficiaryId(event.target.value); resetPendingReview(); }} /></label>
              <label>{TEXT.evidenceAttachment}<input data-testid="supervisor-review-evidence-attachment" value={evidenceAttachment} onChange={(event) => setEvidenceAttachment(event.target.value)} /></label>
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
                impact={`将对复核单 ${latestReviewId || "未生成"} 批准当前节点并写入主管审批留痕；全部节点通过后整单才生效。`}
                disabled={busyAction !== "" || !hasPendingReview}
                precheck={validateDecision}
                onConfirm={() => void approveReview()}
                permissionsState={effectivePermissions}
                permissions={[SUPERVISOR_APPROVE_PERMISSION]}
                data-testid="supervisor-review-approve"
              />
              <HighRiskConfirm
                label={TEXT.reject}
                impact={`将驳回复核单 ${latestReviewId || "未生成"} 当前节点，整单退回起草人并写入主管审批留痕。`}
                disabled={busyAction !== "" || !hasPendingReview}
                precheck={validateDecision}
                onConfirm={() => void rejectReview()}
                permissionsState={effectivePermissions}
                permissions={[SUPERVISOR_APPROVE_PERMISSION]}
                data-testid="supervisor-review-reject"
              />
            </div>
            <div className="admin-muted-action" data-testid="supervisor-review-current-review">当前待审批复核单：{latestReviewId || "未生成"}</div>
            <div className="admin-muted-action" data-testid="supervisor-review-boundary">{TEXT.boundary}</div>
            {formError ? <div data-testid="supervisor-review-error" className="admin-inline-error">{formError}</div> : null}

            {policyTable}
            <ManualBackfillPolicySection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
