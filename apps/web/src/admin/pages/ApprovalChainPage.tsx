import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { TechDetails } from "../components/TechDetails";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { cnDate } from "../components/RowDetailDrawer";

// M087-A-02（PGM-1.7.1「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全……逐级审批」「任一节点驳回则整单退回起草人」
// 「主线与各条平行线全部通过……才生效」）：审批链与节点裁决页——查看某复核单的主线/平行线节点，逐节点通过、驳回或转交；
// 组织层级补齐后可重生成未生成的链。

type PendingRow = {
  decision: { reviewId: string; actionType?: string | null; objectType?: string | null; objectId?: string | null; initiatedBy?: string | null; approvalLevel?: string | null };
  schedule?: { dueAt?: string | null; escalationCount?: number | null; forwardedTo?: string | null } | null;
};
type ChainNode = {
  nodeId: string; reviewId: string; lineCode: string; seq: number; approverKind: string; approverId?: string | null; status: string;
  decidedBy?: string | null; decidedAt?: string | null; forwardedTo?: string | null; forwardedBy?: string | null; escalationCount?: number | null;
};

const VIEW_PERMISSION = "governance.supervisor_review.view";
const MANAGE_PERMISSION = "governance.supervisor_review.manage";
const APPROVE_PERMISSION = "governance.supervisor_review.approve";

const TEXT = {
  title: "审批链与节点裁决",
  description: "每张复核单按组织层级生成主线节点（发起人的直接上级逐级到最终审批人），需要平行线的另有各部门线节点；逐节点裁决，全部通过才生效，任一节点驳回整单退回起草人。组织层级未维护的单据链未生成，须先维护直接上级再重生成。",
  evidence: "接口证据：GET /api/v1/platform/supervisor-review/pending；GET /api/v1/platform/supervisor-review/{review_id}/chain；POST …/approve、…/forward、…/chain/regenerate。",
  loadFailed: "审批链读取失败",
  actionFailed: "审批链操作失败",
  pending: "待复核单据",
  noPending: "当前没有待复核记录",
  chain: "审批链节点",
  noChain: "该单据尚未生成审批链（发起人的直接上级未维护）",
  unselected: "请先在待复核单据中选择一张单据",
  line: "审批线",
  seq: "序号",
  kind: "节点类型",
  approver: "审批人",
  status: "状态",
  forwardedTo: "已转交给",
  escalations: "升级次数",
  decidedBy: "裁决人",
  comment: "审批意见",
  approve: "通过当前节点",
  reject: "驳回（整单退回起草人）",
  forward: "转交当前节点",
  forwardTarget: "转交给（账号）",
  regenerate: "重生成审批链",
  select: "查看链",
  chosen: "当前单据",
};

const KIND_ZH: Record<string, string> = { user: "直接上级", master: "最高权限主会话", role: "指定角色" };
const STATUS_ZH: Record<string, string> = { pending: "待审", approved: "已通过", rejected: "已驳回", cancelled: "已作废" };
const LINE_LABELS: Record<string, string> = { hr_review: "人力审核线（人力会签）", finance_review: "财务审核线", tax_review: "税务审核线", legal_review: "法务审核线" };

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function readReviewIdFromUrl(): string {
  if (typeof window === "undefined") return "";
  return new URLSearchParams(window.location.search).get("review_id") ?? "";
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  const reviewId = String(request.reviewId ?? "");
  if (action === "chain_node_approved") {
    const status = result && typeof result === "object" && "reviewStatus" in result ? String((result as { reviewStatus?: unknown }).reviewStatus ?? "") : "";
    return status === "approved" ? `复核单 ${reviewId} 全部节点已通过，单据生效` : `复核单 ${reviewId} 当前节点已通过，等待后续节点审批`;
  }
  if (action === "chain_node_rejected") return `复核单 ${reviewId} 已在当前节点驳回，整单退回起草人`;
  if (action === "chain_node_forwarded") return `复核单 ${reviewId} 当前节点已转交给 ${String(request.forwardedTo ?? "")}`;
  const count = Array.isArray(result) ? result.length : 0;
  return `复核单 ${reviewId} 审批链已重生成，共 ${count} 个节点`;
}

export function ApprovalChainPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const [pendingRows, setPendingRows] = React.useState<PendingRow[]>([]);
  const [reviewId, setReviewId] = React.useState<string>(() => focusObjectId || readReviewIdFromUrl());
  const [nodes, setNodes] = React.useState<ChainNode[]>([]);
  const [chainLoaded, setChainLoaded] = React.useState(false);
  const [comment, setComment] = React.useState("证据齐备，同意");
  const [forwardTarget, setForwardTarget] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setPendingRows(await api.get<PendingRow[]>("/api/v1/platform/supervisor-review/pending"));
      if (reviewId) {
        setNodes(await api.get<ChainNode[]>(`/api/v1/platform/supervisor-review/${encodeURIComponent(reviewId)}/chain`));
        setChainLoaded(true);
      }
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function selectReview(id: string) {
    setReviewId(id);
    setChainLoaded(false);
    setGlobalError(null);
    try {
      setNodes(await api.get<ChainNode[]>(`/api/v1/platform/supervisor-review/${encodeURIComponent(id)}/chain`));
      setChainLoaded(true);
    } catch (loadError) {
      setGlobalError(loadError instanceof Error ? loadError.message : TEXT.loadFailed);
    }
  }

  function requireReview() {
    if (!reviewId) {
      setFormError(TEXT.unselected);
      return false;
    }
    if (!comment.trim()) {
      setFormError("审批意见不能为空");
      return false;
    }
    return true;
  }

  async function decide(decision: "approved" | "rejected") {
    if (!requireReview()) return;
    const request = {
      reviewId,
      idempotencyKey: nextKey(`chain-${decision}`),
      operatorNote: comment.trim(),
      evidenceAttachmentIds: ["att-approval-chain"],
      reviewerLevel: null,
      decision,
      comment: comment.trim(),
    };
    await runAction(decision === "approved" ? "chain_node_approved" : "chain_node_rejected", request, () =>
      api.post(`/api/v1/platform/supervisor-review/${encodeURIComponent(reviewId)}/approve`, {
        idempotencyKey: request.idempotencyKey, operatorNote: request.operatorNote, evidenceAttachmentIds: request.evidenceAttachmentIds,
        reviewerLevel: request.reviewerLevel, decision: request.decision, comment: request.comment,
      }));
  }

  async function forwardNode() {
    if (!reviewId || !forwardTarget.trim()) {
      setFormError("请先选择单据并填写转交账号");
      return;
    }
    const request = { reviewId, idempotencyKey: nextKey("chain-forward"), operatorNote: "审批链转交", forwardedTo: forwardTarget.trim(), comment: comment.trim() };
    await runAction("chain_node_forwarded", request, () => api.post(`/api/v1/platform/supervisor-review/${encodeURIComponent(reviewId)}/forward`, {
      idempotencyKey: request.idempotencyKey, operatorNote: request.operatorNote, forwardedTo: request.forwardedTo, comment: request.comment,
    }));
  }

  async function regenerate() {
    if (!reviewId) {
      setFormError(TEXT.unselected);
      return;
    }
    await runAction("chain_regenerated", { reviewId }, () => api.post(`/api/v1/platform/supervisor-review/${encodeURIComponent(reviewId)}/chain/regenerate`, {}));
  }

  const lines = Array.from(new Set(nodes.map((node) => node.lineCode)));
  const currentNodeIds = new Set(lines.map((line) => nodes.filter((node) => node.lineCode === line && node.status === "pending").sort((a, b) => a.seq - b.seq)[0]?.nodeId).filter(Boolean));

  return (
    <section data-testid="approval-chain-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="approval-chain-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="governance.supervisor_review.chain_generated / node_decided / forwarded / chain_regenerated" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>{TEXT.pending}</h3>
        {pendingRows.length === 0 ? <div className="admin-muted-action" data-testid="approval-chain-pending-empty">{TEXT.noPending}</div> : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="approval-chain-pending">
              <thead><tr><th>复核单</th><th>动作类型</th><th>对象</th><th>发起人</th><th>档位</th><th>到期</th><th></th></tr></thead>
              <tbody>{pendingRows.map((row) => (
                <tr key={row.decision.reviewId} data-testid={`approval-chain-pending-${row.decision.reviewId}`} className={row.decision.reviewId === reviewId ? "admin-row-selected" : undefined}>
                  <td>{row.decision.reviewId}</td>
                  <td>{formatBusinessStatus(row.decision.actionType ?? "")}</td>
                  <td>{row.decision.objectType ?? "-"} / {row.decision.objectId ?? "-"}</td>
                  <td>{row.decision.initiatedBy ?? "-"}</td>
                  <td>{formatBusinessStatus(row.decision.approvalLevel ?? "")}</td>
                  <td>{row.schedule?.dueAt ? cnDate(row.schedule.dueAt) : "-"}</td>
                  <td><button type="button" data-testid={`approval-chain-select-${row.decision.reviewId}`} onClick={() => void selectReview(row.decision.reviewId)}>{TEXT.select}</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}

        <h3>{TEXT.chain}</h3>
        <div className="admin-muted-action" data-testid="approval-chain-current">{TEXT.chosen}：{reviewId || "未选择"}</div>
        {reviewId && chainLoaded && nodes.length === 0 ? (
          <div className="admin-inline-form" data-testid="approval-chain-unresolved">
            <span>{TEXT.noChain}</span>
            <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE_PERMISSION]} disabled={busyAction !== ""} data-testid="approval-chain-regenerate" onClick={() => void regenerate()}>{TEXT.regenerate}</PermissionButton>
          </div>
        ) : null}
        {nodes.length > 0 ? (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="approval-chain-nodes">
              <thead><tr><th>{TEXT.line}</th><th>{TEXT.seq}</th><th>{TEXT.kind}</th><th>{TEXT.approver}</th><th>{TEXT.status}</th><th>{TEXT.forwardedTo}</th><th>{TEXT.escalations}</th><th>{TEXT.decidedBy}</th></tr></thead>
              <tbody>{nodes.map((node) => (
                <tr key={node.nodeId} data-testid={`approval-chain-node-${node.lineCode}-${node.seq}`} className={currentNodeIds.has(node.nodeId) ? "admin-row-selected" : undefined}>
                  <td>{node.lineCode === "main" ? "主线" : (LINE_LABELS[node.lineCode] ?? node.lineCode)}</td>
                  <td>{node.seq}</td>
                  <td>{KIND_ZH[node.approverKind] ?? node.approverKind}</td>
                  <td>{node.approverId ?? (node.approverKind === "master" ? "USB 最高权限" : "按角色解析")}</td>
                  <td>{(STATUS_ZH[node.status] ?? node.status) + (currentNodeIds.has(node.nodeId) ? "（当前）" : "")}</td>
                  <td>{node.forwardedTo ?? "-"}</td>
                  <td>{node.escalationCount ?? 0}</td>
                  <td>{node.decidedBy ? `${node.decidedBy}${node.decidedAt ? ` · ${cnDate(node.decidedAt)}` : ""}` : "-"}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : null}

        <div className="admin-form-grid" data-testid="approval-chain-form">
          <label>{TEXT.comment}<input data-testid="approval-chain-comment" value={comment} onChange={(event) => setComment(event.target.value)} /></label>
          <label>{TEXT.forwardTarget}<input data-testid="approval-chain-forward-target" value={forwardTarget} onChange={(event) => setForwardTarget(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="approval-chain-actions">
          <HighRiskConfirm
            label={TEXT.approve}
            impact={`将对复核单 ${reviewId || "未选择"} 的当前节点记录通过；全部节点通过后单据生效。`}
            disabled={busyAction !== "" || !reviewId}
            precheck={requireReview}
            onConfirm={() => void decide("approved")}
            permissionsState={effectivePermissions}
            permissions={[APPROVE_PERMISSION]}
            data-testid="approval-chain-approve"
          />
          <HighRiskConfirm
            label={TEXT.reject}
            impact={`将驳回复核单 ${reviewId || "未选择"}，整单退回起草人，其余待审节点作废。`}
            disabled={busyAction !== "" || !reviewId}
            precheck={requireReview}
            onConfirm={() => void decide("rejected")}
            permissionsState={effectivePermissions}
            permissions={[APPROVE_PERMISSION]}
            data-testid="approval-chain-reject"
          />
          <PermissionButton permissionsState={effectivePermissions} permissions={[APPROVE_PERMISSION]} disabled={busyAction !== "" || !reviewId} data-testid="approval-chain-forward" onClick={() => void forwardNode()}>{TEXT.forward}</PermissionButton>
        </div>
        {formError ? <div data-testid="approval-chain-error" className="admin-inline-error">{formError}</div> : null}
      </AdminDataState>
    </section>
  );
}
