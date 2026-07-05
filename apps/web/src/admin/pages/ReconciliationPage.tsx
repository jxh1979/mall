import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
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
  closeReady: "差异处理",
  focusClear: "清除筛选，返回全部",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点承载表 KPI 数字=带 ?kpi= 参数重进本页，
// 只渲染对应那张承载表，感知上=新页面。对账两张承载表：batches=对账批次、differences=对账差异。
// 差异金额/正式关闭接口/选中差异三卡为聚合/状态值，本页无独立承载表→保持不可点。
type KpiTarget = "batches" | "differences";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "batches" && table !== "differences") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  return focus.table === "batches" ? TEXT.batches : TEXT.differences;
}

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
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  // 点 KPI 数字 → pushState 只改查询串（pathname 原样保留子路径前缀链路），进聚焦视图。
  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
  }

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

  const pagedBatches = usePagedRows(batches, 10);
  const pagedDifferences = usePagedRows(differences, 10);

  const batchesSection = (
    <>
      <h3>{TEXT.batches}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="reconciliation-batches-table">
          <thead><tr><th>{TEXT.batchNo}</th><th>{TEXT.amount}</th><th>{TEXT.status}</th><th>{TEXT.provider}</th></tr></thead>
          <tbody>{pagedBatches.pageRows.map((batch) => <tr key={batch.id}><td>{batch.batchNo ?? batch.id}</td><td>{formatAmount(batch.amount ?? batch.totalAmount)}</td><td>{formatBusinessStatus(batch.status)}</td><td><TraceLink navigateTo={navigateTo} pageId="ADM-INT-001" objectId={batch.providerCode}>{batch.providerCode ?? "-"}</TraceLink></td></tr>)}</tbody>
        </table>
        <TablePager testid="reconciliation-batches-pager" page={pagedBatches.page} pageCount={pagedBatches.pageCount} total={pagedBatches.total} onPrev={pagedBatches.prev} onNext={pagedBatches.next} />
      </div>
    </>
  );

  // 差异区：复核表单（对选中差异操作）+ 差异承载表。聚焦「对账差异」时连同表单一起呈现，操作不断。
  const differencesSection = (
    <>
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
      <h3>{TEXT.differences}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="reconciliation-differences-table">
          <thead><tr><th>{TEXT.selected}</th><th>{TEXT.differenceNo}</th><th>{TEXT.amount}</th><th>{TEXT.status}</th><th>{TEXT.provider}</th></tr></thead>
          <tbody>{pagedDifferences.pageRows.map((item) => <tr key={item.id}><td><input type="radio" data-testid={`reconciliation-select-${item.id}`} checked={(selected?.id ?? "") === item.id} onChange={() => setSelectedId(item.id)} /></td><td>{item.differenceNo ?? item.id}</td><td>{formatAmount(item.amount ?? item.differenceAmount ?? item.diffAmount)}</td><td>{formatBusinessStatus(item.status)}</td><td><TraceLink navigateTo={navigateTo} pageId="ADM-INT-001" objectId={item.providerCode}>{item.providerCode ?? "-"}</TraceLink></td></tr>)}</tbody>
        </table>
        <TablePager testid="reconciliation-differences-pager" page={pagedDifferences.page} pageCount={pagedDifferences.pageCount} total={pagedDifferences.total} onPrev={pagedDifferences.prev} onNext={pagedDifferences.next} />
      </div>
    </>
  );

  return (
    <section data-testid="reconciliation-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="reconciliation-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="finance.reconciliation_batch.viewed / finance_reconciliation.difference_found / local.manual_review_logged" />
      <AdminDataState loading={loading} error={error} empty={batches.length === 0 && differences.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            {/* 聚焦视图：只渲染被点 KPI 对应那张承载表，其余表/KPI 卡全隐藏，感知上=新页面。 */}
            <div className="admin-state" data-testid="reconciliation-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{kpiFocus.table === "batches" ? batches.length : differences.length}）
              <button type="button" data-testid="reconciliation-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "batches" ? batchesSection : differencesSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="reconciliation-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.batches}</span>
                <button type="button" className="admin-kpi-value" data-testid="reconciliation-kpi-batches" onClick={() => enterKpiFocus("batches")}>{batches.length}</button>
                <small>{TEXT.batchNo}</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.differences}</span>
                <button type="button" className="admin-kpi-value" data-testid="reconciliation-kpi-differences" onClick={() => enterKpiFocus("differences")}>{differences.length}</button>
                <small>{TEXT.unresolved}: {unresolvedCount}</small>
              </div>
              {/* 差异金额=聚合金额、正式关闭接口=状态、选中差异=选择态，均无独立承载表→不可点。 */}
              <div className="admin-kpi-card"><span>{TEXT.amount}</span><strong>{formatAmount(totalDifference)}</strong><small>{TEXT.status}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.closeApi}</span><strong>已就绪</strong><small>{TEXT.closeReady}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected?.differenceNo ?? selected?.id ?? "-"}</strong><small>{formatBusinessStatus(selected?.status)}</small></div>
            </div>

            {differencesSection}
            {batchesSection}
          </>
        )}
      </AdminDataState>
    </section>
  );
}
