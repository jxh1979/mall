import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, ReconciliationBatchRow, ReconciliationDifferenceRow } from "../types";

const TEXT = {
  title: "对账差异",
  description: "查看对账批次和差异，执行本地人工复核留痕；当前 OpenAPI 未声明正式差异关闭写接口，因此不伪造关闭动作。",
  evidence: "接口证据：读取 /api/v1/finance/reconciliation/batches、/api/v1/finance/reconciliation/differences；正式关闭接口未声明，当前仅本地人工复核留痕。",
  loadFailed: "对账数据读取失败",
  noDiff: "当前没有可复核差异。",
  reasonRequired: "差异复核必须填写原因。",
  attachmentRequired: "差异关闭/复核必须绑定附件证据。",
  batches: "对账批次",
  differences: "对账差异",
  unresolved: "未处理",
  amount: "差异金额",
  apiGap: "正式关闭接口未声明",
  reason: "复核原因",
  attachment: "附件证据",
  submit: "记录人工复核",
  boundary: "差异关闭必须有原因和附件；当前只写本地审计结果，正式关闭/忽略需要后续补 OpenAPI 写接口和后端权限。",
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

export function ReconciliationPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
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

  function submitReview() {
    if (!selected) {
      setFormError(TEXT.noDiff);
      return;
    }
    setFormError("");
    setGlobalError(null);
    setActionResult({
      action: "reconciliation_manual_reviewed",
      differenceId: selected.id,
      status: "manual_review_logged",
      reason,
      evidenceAttachmentIds: [attachmentRef],
      apiGap: "formal_resolution_api_not_declared",
    });
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
          <div className="admin-kpi-card"><span>{TEXT.apiGap}</span><strong>是</strong><small>formal_resolution_api_not_declared</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected?.differenceNo ?? selected?.id ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="reconciliation-review-form">
          <label>{TEXT.reason}<input data-testid="reconciliation-review-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
          <label>{TEXT.attachment}<input data-testid="reconciliation-review-attachment" value={attachmentRef} onChange={(event) => setAttachmentRef(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.submit}
            impact="将对选中差异写入人工复核留痕，作为对账调账依据进入审计，无法撤销。"
            precheck={validateReview}
            onConfirm={submitReview}
            data-testid="reconciliation-review-submit"
          />
          <span className="admin-muted-action" data-testid="reconciliation-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="reconciliation-error" className="admin-inline-error">{formError}</div> : null}

        <h3>{TEXT.batches}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="reconciliation-batches-table">
            <thead><tr><th>{TEXT.batchNo}</th><th>{TEXT.amount}</th><th>{TEXT.status}</th><th>{TEXT.provider}</th></tr></thead>
            <tbody>{batches.map((batch) => <tr key={batch.id}><td>{batch.batchNo ?? batch.id}</td><td>{formatAmount(batch.amount ?? batch.totalAmount)}</td><td>{batch.status ?? "-"}</td><td>{batch.providerCode ?? "-"}</td></tr>)}</tbody>
          </table>
        </div>
        <h3>{TEXT.differences}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="reconciliation-differences-table">
            <thead><tr><th>{TEXT.selected}</th><th>{TEXT.differenceNo}</th><th>{TEXT.amount}</th><th>{TEXT.status}</th><th>{TEXT.provider}</th></tr></thead>
            <tbody>{differences.map((item) => <tr key={item.id}><td><input type="radio" data-testid={`reconciliation-select-${item.id}`} checked={(selected?.id ?? "") === item.id} onChange={() => setSelectedId(item.id)} /></td><td>{item.differenceNo ?? item.id}</td><td>{formatAmount(item.amount ?? item.differenceAmount ?? item.diffAmount)}</td><td>{item.status ?? "-"}</td><td>{item.providerCode ?? "-"}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
