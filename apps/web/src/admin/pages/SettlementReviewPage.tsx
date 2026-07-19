import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { TablePager } from "../components/TablePager";
import type { AdminPageProps } from "../types";
import type { PagedResult } from "../client/adminClient";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

// 平台侧结算审阅(审计断层②):只读列出通票核销产生的结算流水,供平台操作员查看。
// 无写操作(确认/入账由月度 Job 负责);后端 /api/v1/settlement/transactions,权限 finance.settlement.view。
const PAGE_SIZE = 20;

export type SettlementRow = {
  id: string;
  transactionNo: string;
  projectId: string | null;
  merchantId: string | null;
  exclusiveMerchantId: string | null;
  ticketTypeId: string | null;
  entitlementType: string | null;
  beneficiaryType: string | null;
  verifiedCount: number;
  numPeople: number;
  verifiedAmountCent: number;
  settlementStatus: string | null;
  createdAt: string;
};

function centToYuan(cent: number | undefined | null): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

export function SettlementReviewPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [projectDraft, setProjectDraft] = React.useState("");
  const [merchantDraft, setMerchantDraft] = React.useState("");
  const [projectFilter, setProjectFilter] = React.useState("");
  const [merchantFilter, setMerchantFilter] = React.useState("");
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<SettlementRow>> => {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (projectFilter.trim()) params.set("projectId", projectFilter.trim());
      if (merchantFilter.trim()) params.set("merchantId", merchantFilter.trim());
      return api.getPaged<SettlementRow>(`/api/v1/settlement/transactions?${params.toString()}`);
    },
    [api, projectFilter, merchantFilter],
  );

  const rows = useServerPagedRows<SettlementRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: "结算流水读取失败",
  });

  function submitSearch() {
    setProjectFilter(projectDraft);
    setMerchantFilter(merchantDraft);
  }

  // 过滤条件变化后回第 1 页重取。
  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectFilter, merchantFilter]);

  function openDetail(row: SettlementRow) {
    setDetail({
      title: `结算流水明细 · ${row.transactionNo}`,
      fields: [
        { label: "流水号", value: row.transactionNo },
        { label: "项目", value: row.projectId ?? "—" },
        { label: "商户", value: row.merchantId ?? "—" },
        { label: "专属商户", value: row.exclusiveMerchantId ?? "—" },
        { label: "权益类型", value: formatBusinessStatus(row.entitlementType ?? "—") },
        { label: "受益方", value: formatBusinessStatus(row.beneficiaryType ?? "—") },
        { label: "核销张数", value: String(row.verifiedCount) },
        { label: "人数", value: String(row.numPeople) },
        { label: "核销金额", value: `¥${centToYuan(row.verifiedAmountCent)}` },
        { label: "结算状态", value: formatBusinessStatus(row.settlementStatus ?? "—") },
        { label: "发生时间", value: cnDate(row.createdAt) },
      ],
    });
  }

  // 第四批接线（指标登记簿方案A）：结算笔数/核销金额/核销张数改走统一明细端点（平台口径全量）。
  // 有项目/商户筛选时=非平台口径 → enabled 关，卡回退本地（笔数=分页 total、金额/张数=不适用）并隐藏「完整明细」入口。
  const noFilter = !projectFilter.trim() && !merchantFilter.trim();
  const countKpi = useMetricKpiValue(api, "settlement.transaction_count", { enabled: noFilter });
  const amountKpi = useMetricKpiValue(api, "settlement.verified_amount", { enabled: noFilter });
  const ticketKpi = useMetricKpiValue(api, "settlement.verified_count", { enabled: noFilter });
  const countDisplay = countKpi.status === "ready" && countKpi.value !== null ? countKpi.value : rows.total;

  const empty = !rows.loading && rows.total === 0 && !projectFilter && !merchantFilter;

  return (
    <section data-testid="settlement-review-page">
      <AdminPageHeader title="通票结算审阅" description="只读查看通票核销产生的结算流水（项目/商户/核销张数/金额/状态）。确认与入账由月度结算负责。" />
      <AdminDataState loading={rows.loading} error={rows.error} empty={empty} onRetry={rows.reload}>
        <div className="admin-kpi-grid" data-testid="settlement-review-summary">
          <div className="admin-kpi-card"><span>结算笔数</span><strong className="admin-kpi-value--text" data-testid="settlement-review-kpi-count">{countDisplay}</strong><small>{noFilter ? "平台全量" : "当前筛选"}{noFilter && countKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="settlement.transaction_count" testid="settlement-review-metric-detail-count" /> : null}</small></div>
          <div className="admin-kpi-card"><span>核销金额合计（元）</span><strong className="admin-kpi-value--text" data-testid="settlement-review-kpi-amount">{noFilter && amountKpi.status === "ready" && amountKpi.value !== null ? centToYuan(amountKpi.value) : "—"}</strong><small>{noFilter && amountKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="settlement.verified_amount" testid="settlement-review-metric-detail-amount" /> : "筛选态平台合计不适用"}</small></div>
          <div className="admin-kpi-card"><span>核销张数合计</span><strong className="admin-kpi-value--text" data-testid="settlement-review-kpi-tickets">{noFilter && ticketKpi.status === "ready" && ticketKpi.value !== null ? ticketKpi.value : "—"}</strong><small>{noFilter && ticketKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="settlement.verified_count" testid="settlement-review-metric-detail-tickets" /> : "筛选态平台合计不适用"}</small></div>
        </div>
        <div className="admin-list-toolbar" data-testid="settlement-review-filter">
          <input
            data-testid="settlement-review-project"
            value={projectDraft}
            onChange={(event) => setProjectDraft(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") submitSearch(); }}
            placeholder="项目（回车查询，可空）"
          />
          <input
            data-testid="settlement-review-merchant"
            value={merchantDraft}
            onChange={(event) => setMerchantDraft(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") submitSearch(); }}
            placeholder="商户（回车查询，可空）"
          />
          <button type="button" data-testid="settlement-review-search-submit" onClick={submitSearch}>查询</button>
          <span className="admin-muted-action" data-testid="settlement-review-count">共 {rows.total} 条</span>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-review-table">
            <thead><tr><th>流水号</th><th>项目</th><th>商户</th><th>核销张数</th><th>核销金额（元）</th><th>结算状态</th><th>详情</th></tr></thead>
            <tbody>{rows.rows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`settlement-review-row-${row.id}`}
                onClick={() => openDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDetail(row); } }}>
                <td>{row.transactionNo}</td>
                <td>{row.projectId ?? "—"}</td>
                <td>{row.merchantId ?? row.exclusiveMerchantId ?? "—"}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{row.verifiedCount}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.verifiedAmountCent)}</td>
                <td>{formatBusinessStatus(row.settlementStatus ?? "—")}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看结算流水详情" onClick={(event) => { event.stopPropagation(); openDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
          <TablePager testid="settlement-review-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
