import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps, MerchantZoneContractRow } from "../types";

const TEXT = {
  title: "我的合同",
  description: "本商户合同的关键信息(租期 / 空间 / 状态 / 保证金)只读;改价、变更与审批仍走平台侧权威流程。",
  loadFailed: "合同读取失败",
};

const STATUS_LABELS: Record<string, string> = {
  draft: "起草中",
  pending_approval: "审批中",
  pending_confirmation: "待确认",
  effective: "履约中",
  expired: "已到期",
  pending_settlement: "结算中",
  terminated: "已终止",
  renewed: "已续签",
};

// ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}

function money(cent: number | null | undefined) {
  if (cent === undefined || cent === null) return "-";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} CNY`;
}

export function MerchantZoneContractsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneContractRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantZoneContractRow[]>("/api/v1/merchant-portal/contracts"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const activeCount = rows.filter((row) => row.lifecycleStatus === "effective").length;
  const paged = usePagedRows(rows, 10);

  return (
    <section data-testid="merchant-zone-contracts-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_portal.contract.viewed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="merchant-zone-contracts-summary">
          <div className="admin-kpi-card"><span>合同总数</span><strong>{rows.length}</strong><small>本商户</small></div>
          <div className="admin-kpi-card"><span>履约中</span><strong>{activeCount}</strong><small>当前有效</small></div>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-zone-contracts-table">
            <thead>
              <tr><th>合同</th><th>租期</th><th>空间</th><th>状态</th><th>保证金</th><th>续签自</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id}>
                  <td className="admin-id-cell" title={row.id}>
                    <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.id}>{shortId(row.id)}</TraceLink>
                  </td>
                  <td>{row.effectiveDate ?? "-"} ~ {row.expiryDate ?? "-"}</td>
                  <td>{(row.spaceIds ?? []).length > 0
                    ? (row.spaceIds ?? []).map((sid, i) => <React.Fragment key={sid}>{i > 0 ? ", " : ""}<TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={sid}>{sid}</TraceLink></React.Fragment>)
                    : "-"}</td>
                  <td>{STATUS_LABELS[row.lifecycleStatus ?? ""] ?? row.lifecycleStatus ?? "-"}</td>
                  <td className="admin-num">{money(row.depositCent)}</td>
                  <td className="admin-id-cell" title={row.renewedFromContractId ?? undefined}>
                    {row.renewedFromContractId
                      ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.renewedFromContractId}>{shortId(row.renewedFromContractId)}</TraceLink>
                      : shortId(row.renewedFromContractId)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-zone-contracts-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
