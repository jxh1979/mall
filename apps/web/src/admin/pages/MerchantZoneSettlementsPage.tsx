import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps, MerchantZoneSettlementRow } from "../types";

const DISPUTE_PERMS = ["merchant.portal.settlement.dispute"];

const TEXT = {
  title: "我的对账",
  description: "查看与本商户相关的退租 / 到期结算单;对未结清的结算单可提交异议留言,平台裁决后继续结算。资金流程仍由平台侧权威闭环。",
  loadFailed: "对账数据读取失败",
  actionFailed: "异议提交失败",
  reasonRequired: "提交异议必须填写理由。",
  noSelection: "请先选择要提异议的结算单。",
  submit: "提交异议",
};

const TRIGGER_LABELS: Record<string, string> = {
  move_out: "退租",
  expiry: "到期",
  early_termination: "提前终止",
};

const STATUS_LABELS: Record<string, string> = {
  in_progress: "进行中",
  pending_settlement: "待结算",
  deposit_offsetting: "保证金抵扣中",
  arrears_collecting: "欠费追缴中",
  disputed: "有异议",
  settled: "已结清",
};

const DISPUTABLE = new Set(["in_progress", "pending_settlement", "deposit_offsetting", "arrears_collecting"]);

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function MerchantZoneSettlementsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneSettlementRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [reason, setReason] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantZoneSettlementRow[]>("/api/v1/merchant-portal/settlements"));
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

  async function submitDispute() {
    if (!selected) {
      setFormError(TEXT.noSelection);
      return;
    }
    if (!reason.trim()) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-zone-settlement-dispute"), reason };
    await runAction("merchant_zone_settlement_disputed", request,
      () => api.post(`/api/v1/merchant-portal/settlements/${selected.id}/dispute`, request));
    setReason("");
  }

  const openCount = rows.filter((row) => row.status !== "settled").length;
  const disputedCount = rows.filter((row) => row.status === "disputed").length;
  const paged = usePagedRows(rows, 10);

  return (
    <section data-testid="merchant-zone-settlements-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_portal.settlement.viewed / merchant_portal.settlement.disputed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="merchant-zone-settlements-summary">
          <div className="admin-kpi-card"><span>结算单</span><strong>{rows.length}</strong><small>本商户</small></div>
          <div className="admin-kpi-card"><span>未结清</span><strong>{openCount}</strong><small>进行中/待结算</small></div>
          <div className="admin-kpi-card"><span>有异议</span><strong>{disputedCount}</strong><small>待平台裁决</small></div>
        </div>
        <div className="admin-form-row" data-testid="merchant-zone-settlement-dispute-form">
          <label>异议理由<input data-testid="merchant-zone-settlement-dispute-reason" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="如:金额与合同约定不符" /></label>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={DISPUTE_PERMS}
            data-testid="merchant-zone-settlement-dispute-submit"
            disabled={busyAction === "merchant_zone_settlement_disputed" || !selected || !DISPUTABLE.has(selected.status ?? "")}
            onClick={submitDispute}
          >{TEXT.submit}</PermissionButton>
        </div>
        {formError ? <div className="admin-form-error" data-testid="merchant-zone-settlement-form-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-zone-settlements-table">
            <thead>
              <tr><th>选择</th><th>结算单</th><th>合同</th><th>触发</th><th>状态</th><th>异议理由</th><th>开账时间</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id} className={row.id === selectedId ? "admin-row-selected" : undefined}>
                  <td><input type="radio" name="mz-settlement" data-testid={`merchant-zone-settlement-pick-${row.id}`} checked={row.id === selectedId} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.id}</td>
                  <td><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink></td>
                  <td>{TRIGGER_LABELS[row.triggerType ?? ""] ?? row.triggerType ?? "-"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
                  <td>{row.disputeReason ?? "-"}</td>
                  <td>{row.createdAt ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-zone-settlements-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
