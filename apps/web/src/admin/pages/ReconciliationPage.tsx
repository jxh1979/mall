import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, ReconciliationBatchRow, ReconciliationDifferenceRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { formatBusinessStatus } from "../i18n/statusLabels";

const RESOLVE_PERM = ["finance.reconciliation.resolve"];

const TEXT = {
  title: "对账差异",
  description: "查看对账批次和差异；对选中差异填原因+附件后可正式关闭（调用后端 close 接口，落库+进审计）。",
  evidence: "接口证据：读取 batches/differences；正式关闭调用 POST /api/v1/finance/reconciliation/differences/{id}/close。",
  loadFailed: "对账数据读取失败",
  noDiff: "当前没有可复核差异。",
  reasonRequired: "差异关闭必须填写原因。",
  attachmentRequired: "差异关闭/复核必须绑定附件证据。",
  batches: "对账批次",
  differences: "对账差异",
  unresolved: "未处理",
  amount: "差异金额",
  closeApi: "正式关闭接口",
  reason: "复核原因",
  attachment: "附件证据",
  submit: "正式关闭差异",
  boundary: "差异关闭必须有原因和附件；提交后调用后端正式关闭接口，落 close_reason 并进审计，无法撤销。",
  batchNo: "批次号",
  differenceNo: "差异号",
  status: "状态",
  provider: "渠道",
  selected: "选中差异",
};

function amountValue(value: unknown) {
  return Number(value ?? 0);
}

function formatAmount(value: unknown) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(amountValue(value) / 100);
}

export function ReconciliationPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [batches, setBatches] = React.useState<ReconciliationBatchRow[]>([]);
  const [differences, setDifferences] = React.useState<ReconciliationDifferenceRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(differences);
  const [reason, setReason] = React.useState("");
  const [attachmentRef, setAttachmentRef] = React.useState("");
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [batchRows, differenceRows] = await Promise.all([
        api.get<ReconciliationBatchRow[]>("/api/v1/finance/reconciliation/batches"),
        api.get<ReconciliationDifferenceRow[]>("/api/v1/finance/reconciliation/differences"),
      ]);
      setBatches(batchRows);
      setDifferences(differenceRows);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const unresolvedCount = differences.filter((item) => !["resolved", "ignored", "matched"].includes(item.status ?? "")).length;
  const totalDifference = differences.reduce((sum, item) => sum + amountValue(item.amount ?? item.differenceAmount ?? item.diffAmount), 0);

  function validateReview() {
    if (!selected) {
      setFormError(TEXT.noDiff);
      return false;
    }
    if (!reason.trim()) {
      setFormError(TEXT.reasonRequired);
      return false;
    }
    if (!attachmentRef.trim()) {
      setFormError(TEXT.attachmentRequired);
      return false;
    }
    return true;
  }

  async function submitReview() {
    if (!selected) {
      setFormError(TEXT.noDiff);
      return;
    }
    setFormError("");
    setGlobalError(null);
    try {
      await api.post(`/api/v1/finance/reconciliation/differences/${selected.id}/close`, {
        idempotencyKey: `reconciliation-close-${selected.id}-${Date.now()}`,
        closeReason: reason,
        evidenceAttachmentIds: [attachmentRef],
      });
      setActionResult({
        action: "reconciliation_difference_closed",
        differenceId: selected.id,
        status: "closed",
        reason,
        evidenceAttachmentIds: [attachmentRef],
      });
      setReason("");
      setAttachmentRef("");
      await reload();
    } catch (submitError) {
      setGlobalError(submitError instanceof Error ? submitError.message : String(submitError));
    }
  }

  return (
    <section data-testid="reconciliation-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="reconciliation-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="finance.reconciliation_batch.viewed / finance_reconciliation.difference_found / local.manual_review_logged" />
      <AdminDataState loading={loading} error={error} empty={batches.length === 0 && differences.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="reconciliation-summary">
          <div className="admin-kpi-card"><span>{TEXT.batches}</span><strong>{batches.length}</strong><small>{TEXT.batchNo}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.differences}</span><strong>{differences.length}</strong><small>{TEXT.unresolved}: {unresolvedCount}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.amount}</span><strong>{formatAmount(totalDifference)}</strong><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.closeApi}</span><strong>已就绪</strong><small>POST .../differences/close</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected?.differenceNo ?? selected?.id ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="reconciliation-review-form">
          <label>{TEXT.reason}<input data-testid="reconciliation-review-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
          <label>{TEXT.attachment}<input data-testid="reconciliation-review-attachment" value={attachmentRef} onChange={(event) => setAttachmentRef(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.submit}
            impact="将对选中差异调用后端正式关闭接口，落 close_reason 并进审计，作为对账调账依据，无法撤销。"
            precheck={validateReview}
            onConfirm={submitReview}
            permissionsState={effectivePermissions}
            permissions={RESOLVE_PERM}
            data-testid="reconciliation-review-submit"
          />
          <span className="admin-muted-action" data-testid="reconciliation-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="reconciliation-error" className="admin-inline-error">{formError}</div> : null}

        <h3>{TEXT.batches}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="reconciliation-batches-table">
            <thead><tr><th>{TEXT.batchNo}</th><th>{TEXT.amount}</th><th>{TEXT.status}</th><th>{TEXT.provider}</th></tr></thead>
            <tbody>{batches.map((batch) => <tr key={batch.id}><td>{batch.batchNo ?? batch.id}</td><td>{formatAmount(batch.amount ?? batch.totalAmount)}</td><td>{formatBusinessStatus(batch.status)}</td><td><TraceLink navigateTo={navigateTo} pageId="ADM-INT-001" objectId={batch.providerCode}>{batch.providerCode ?? "-"}</TraceLink></td></tr>)}</tbody>
          </table>
        </div>
        <h3>{TEXT.differences}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="reconciliation-differences-table">
            <thead><tr><th>{TEXT.selected}</th><th>{TEXT.differenceNo}</th><th>{TEXT.amount}</th><th>{TEXT.status}</th><th>{TEXT.provider}</th></tr></thead>
            <tbody>{differences.map((item) => <tr key={item.id}><td><input type="radio" data-testid={`reconciliation-select-${item.id}`} checked={(selected?.id ?? "") === item.id} onChange={() => setSelectedId(item.id)} /></td><td>{item.differenceNo ?? item.id}</td><td>{formatAmount(item.amount ?? item.differenceAmount ?? item.diffAmount)}</td><td>{formatBusinessStatus(item.status)}</td><td><TraceLink navigateTo={navigateTo} pageId="ADM-INT-001" objectId={item.providerCode}>{item.providerCode ?? "-"}</TraceLink></td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
