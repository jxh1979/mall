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
import { withBase } from "../../basePath";

// 文件版本：V0.02

// M087-A-02（PGM-1.7.1「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全……逐级审批」「任一节点驳回则整单退回起草人」
// 「主线与各条平行线全部通过……才生效」）：审批链与节点裁决页——查看某复核单的主线/平行线节点，逐节点通过、驳回或转交；
// 组织层级补齐后可重生成未生成的链。

type PendingRow = {
  decision: { reviewId: string; reviewStatus: string; actionType?: string | null; objectType?: string | null; objectId?: string | null; initiatedBy?: string | null; approvalLevel?: string | null; reason?: string | null; evidenceAttachmentIds?: string[] };
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
const LINE_LABELS: Record<string, string> = { business_review: "业务审核线", hr_review: "人力审核线（人力会签）", finance_review: "财务审核线", tax_review: "税务审核线", legal_review: "法务审核线" };

const TAX_FIELD_LABELS: Record<string, string> = {
  id: "原件编号", category: "专业认定税目", version: "版本", effectiveFrom: "生效日期", applicabilityBasis: "适用依据",
  calculationBasis: "计算依据", roundingRule: "金额舍入规则", brackets: "完整税档", seq: "档次", incomeFloorCent: "收入下限（分）",
  incomeCeilingCent: "收入上限（分，无上限为空）", incomeBaseBps: "收入基数（万分比）", flatDeductionCent: "固定减除（分）",
  rateBps: "税率（万分比）", quickDeductionCent: "速算扣除（分）", reason: "申请理由", userId: "员工账号", month: "工资月份",
  projectId: "原所属项目", merchantId: "原所属商户", payerCompanyId: "工资发放主体", taxDate: "税务适用日期",
  identityBasis: "纳税身份依据", transactionBasis: "交易性质依据", professionalBasis: "专业认定依据", incomeCent: "原收入（分）",
  taxCent: "核定税额（分）", taxVersionId: "适用税表编号", sourceLines: "原收入明细", sourceId: "业务来源编号",
  amountCent: "金额（分）", sourceFingerprint: "原收入指纹", calculationSnapshot: "计算原件", versionId: "适用版本编号",
  bracket: "所用档位", grossCent: "计算收入（分）", afterBaseCent: "基数后金额（分）", taxableCent: "计税金额（分）",
  beforeQuickCent: "速算扣除前税额（分）", status: "提交时状态", reviewId: "复核单号", evidenceAttachmentIds: "原件附件编号",
  createdBy: "起草人", createdAt: "起草时间", decidedAt: "决定时间", decidedBy: "决定人",
  previousVersions: "变更前适用版本", proposedConfig: "拟变更配置", zoneId: "所在地时区",
  cutoffHour: "日切小时", minCutoffHour: "日切小时下限", maxCutoffHour: "日切小时上限",
  // 第十八批第 24 条：高价值判定阈值最高权限审批原件。
  highValueThresholdCent: "高价值判定阈值（分）", highValueThresholdMinCent: "阈值下限（分）", highValueThresholdMaxCent: "阈值上限（分）",
  effectiveAt: "拟生效时间", finalApproverId: "组织链最终审批人", attachmentIds: "依据附件编号", idempotencyKey: "提交编号",
  contract: "供货合同原件", terms: "合同执行条款", contractSnapshot: "采购锁定的合同", purchase: "采购申请原件",
  contractNo: "合同编号", supplierId: "供应商编号", itemId: "商品编号", contractMode: "供货方式",
  supplyScope: "供货范围", priceTerms: "价格条款", paymentTermDays: "原合同账期天数", validFrom: "合同开始日期",
  validTo: "合同结束日期", summary: "合同说明", purchasableScope: "可采购商品编号", approvalStatus: "审批状态",
  updatedAt: "更新时间", purchaserEntityId: "我方采购主体编号", settlementMethod: "结算方式", priceBasis: "计价依据",
  unitPrices: "各商品锁定单价（分）", dueAnchor: "到期起算事件", termDays: "账期天数", fixedDueDate: "明确到期日",
  signedOn: "签约日期", prepaymentAmountCent: "预付金额（分）", consignmentSaleBasis: "寄售售出确认依据",
  prepaymentRemainderTerms: "预付余额处理约定", requestNo: "采购申请单号", quantity: "数量",
  expectedArrivalDate: "预计到货日", supplyContractId: "供货合同编号", approvedBy: "批准人", orderedAt: "下单时间",
  actualArrivalDate: "实际到货日", receivedAt: "验收时间", receivedBy: "验收人", purchaseId: "采购申请编号",
  receiptId: "入库验收编号", resolutionType: "处理方式", returnUnitPriceCent: "退货确认单价（分）",
  confirmedAmountCent: "退货确认金额（分）", responsibilityTerms: "费用与责任约定",
  supplierRefundDueDate: "供应商退款到期日", consignmentSaleId: "原寄售售出记录编号",
  lockedAt: "条款锁定时间", approvedAt: "原合同批准时间", contractBasis: "合同及双方确认依据",
  sourcePaymentId: "原实际付款编号", refundDueDate: "退款约定到期日", acceptedQuantity: "累计合格接收数量",
  terminatedQuantity: "已批准终止数量", remainingQuantity: "仍待处理数量", originalItemId: "原应付编号",
  payables: "关联应付原件", payments: "原付款记录", paidAmountCent: "已付金额（分）", unpaidAmountCent: "未付金额（分）",
  availableAmountCent: "尚未使用金额（分）", proposedResolution: "拟处理方案", currentPrepayment: "原预付款情况",
  prepaymentResolution: "预付款余额处理原件", receipts: "历次验收原件", receipt: "原入库验收",
  consignmentSale: "原寄售售出确认", arrivedQuantity: "本次实到数量", rejectedQuantity: "本次拒收数量",
  warehouseId: "仓库编号", locationId: "库位编号", batchId: "原库存批次", movementId: "原库存变动编号",
  unitCostCent: "到货单价（分）", payableId: "原应付编号", confirmedBy: "确认人", confirmedAt: "确认时间",
};

function taxFact(value: unknown): React.ReactNode {
  if (Array.isArray(value)) return value.length ? <ol>{value.map((item, index) => <li key={index}>{taxFact(item)}</li>)}</ol> : "无";
  if (value && typeof value === "object") return <dl>{Object.entries(value).map(([key, item]) => {
    const field = key.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase());
    return <React.Fragment key={key}><dt>{TAX_FIELD_LABELS[field] ?? key}</dt><dd>{["status", "approvalStatus"].includes(field) && typeof item === "string" ? formatBusinessStatus(item) : taxFact(item)}</dd></React.Fragment>;
  })}</dl>;
  if (value === "truncate_cent_each_step") return "按专业依据逐步截断到分";
  if (value === "draft") return "草案";
  const purchaseValues: Record<string, string> = {
    cash_on_delivery: "货到付款", monthly: "按月结算", credit_term: "约定账期", prepaid: "预付款", consignment: "寄售",
    contract_unit_price: "合同固定单价", accepted_actual_unit_price: "合格接收的实际单价", contract_signed: "签约日",
    ordered: "采购下单日", each_receipt: "各次合格接收日", all_fulfilled: "全部数量处理完成日", month_end: "对应月份月末",
    fixed_date: "合同明确日期", confirmed_sale: "寄售确认售出日", replenishment: "补货跟进", terminate: "批准终止余量",
    platform_self_use: "平台自用采购", merchant_self_purchase: "商户自采", platform_purchase_distribution: "平台统采分销",
    refund_unused: "退回未用预付款", reduce_unpaid: "减少未付预付款义务",
  };
  if (typeof value === "string" && purchaseValues[value]) return purchaseValues[value];
  return value == null ? "未填写／不适用" : String(value);
}

function ReviewOriginal({ decision }: { decision: PendingRow["decision"] }) {
  const reason = decision.reason ?? "";
  let content: React.ReactNode = <p style={{ whiteSpace: "pre-wrap", overflowWrap: "anywhere" }}>{reason || "本单未记录原始说明"}</p>;
  if (["payroll_tax_version", "payroll_tax_assessment", "amusement_business_day_config", "bar_business_day_config", "high_value_threshold", "supply_contract", "inventory_purchase_request", "purchase_quantity_resolution", "purchase_return", "purchase_prepayment_resolution"].includes(decision.objectType ?? "")) {
    const start = reason.indexOf("{");
    if (start >= 0) {
      try { content = <><p>{reason.slice(0, start)}</p>{taxFact(JSON.parse(reason.slice(start)))}</>; }
      catch { /* 原说明完整保留，解析失败不得伪造或省略税务原件。 */ }
    }
  }
  return <section data-testid="approval-chain-original"><h3>提交时原始内容与凭证</h3>{content}
    <div>{(decision.evidenceAttachmentIds ?? []).map((id, index) => <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">查看原始凭证 {index + 1} </a>)}</div>
  </section>;
}

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
  const [original, setOriginal] = React.useState<PendingRow["decision"] | null>(null);
  const [originalError, setOriginalError] = React.useState<string | null>(null);
  const selectedReview = original?.reviewId === reviewId ? original : null;
  const canDecide = selectedReview?.reviewStatus === "pending_review" && chainLoaded;

  async function loadOriginal(id: string) {
    setOriginal(null);
    setOriginalError(null);
    setChainLoaded(false);
    try {
      const [record, chain] = await Promise.all([
        api.get<PendingRow["decision"]>(`/api/v1/platform/supervisor-review/${encodeURIComponent(id)}`),
        api.get<ChainNode[]>(`/api/v1/platform/supervisor-review/${encodeURIComponent(id)}/chain`),
      ]);
      if (record.reviewId !== id) throw new Error("复核原件与当前单据不一致，请重新读取");
      setOriginal(record);
      setNodes(chain);
      setChainLoaded(true);
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : TEXT.loadFailed;
      setOriginalError(message);
      setGlobalError(message);
    }
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setPendingRows(await api.get<PendingRow[]>("/api/v1/platform/supervisor-review/pending"));
      if (reviewId) await loadOriginal(reviewId);
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
    await loadOriginal(id);
  }

  function requireReview() {
    if (!reviewId) {
      setFormError(TEXT.unselected);
      return false;
    }
    if (!canDecide) {
      setFormError("请先读取当前复核单原件和审批链，确认仍待审批");
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
      evidenceAttachmentIds: selectedReview?.evidenceAttachmentIds ?? [],
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
        {selectedReview ? <ReviewOriginal decision={selectedReview} /> : null}
        {selectedReview && selectedReview.reviewStatus !== "pending_review" ? <p>原单状态：{formatBusinessStatus(selectedReview.reviewStatus)}，不能再次裁决。</p> : null}
        {originalError ? <p role="alert" className="admin-inline-error">{originalError}</p> : null}
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
            disabled={busyAction !== "" || !canDecide}
            precheck={requireReview}
            onConfirm={() => void decide("approved")}
            permissionsState={effectivePermissions}
            permissions={[APPROVE_PERMISSION]}
            data-testid="approval-chain-approve"
          />
          <HighRiskConfirm
            label={TEXT.reject}
            impact={`将驳回复核单 ${reviewId || "未选择"}，整单退回起草人，其余待审节点作废。`}
            disabled={busyAction !== "" || !canDecide}
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
