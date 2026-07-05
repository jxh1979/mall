import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

type MetricValue = { value?: number | string | null; unit?: string | null; note?: string | null };
type FinanceDashboard = { businessDate?: string; metrics?: Record<string, MetricValue> };
type DebtorRow = {
  merchantId?: string | null;
  merchantName?: string | null;
  outstandingCent?: number | string;
  oldestDueDate?: string;
  overdueDays?: number;
  bucket?: string;
};

const TEXT = {
  title: "欠费与回款",
  description: "谁欠费、欠多久、多少进入坏账区间、本月回款缺口多少——账龄为快照期末口径,明细为实时同口径钻取(同一过滤条件,构造上对得上)。",
  loadFailed: "欠费分析读取失败",
};

const BUCKETS: Array<{ key: string; metric: string; label: string }> = [
  { key: "a0_30", metric: "ar.aging_0_30", label: "1-30 天" },
  { key: "a31_60", metric: "ar.aging_31_60", label: "31-60 天" },
  { key: "a61_90", metric: "ar.aging_61_90", label: "61-90 天" },
  { key: "a90p", metric: "ar.aging_90p", label: "90 天以上(坏账预警)" },
];

const BUCKET_LABELS: Record<string, string> = Object.fromEntries(BUCKETS.map((b) => [b.key, b.label]));

function cents(metric?: MetricValue): number | null {
  if (!metric || metric.value === null || metric.value === undefined) return null;
  return Number(metric.value);
}

function money(cent: number | null | undefined) {
  if (cent === null || cent === undefined || !Number.isFinite(cent)) return "—";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
}

export function ReceivablesAnalyticsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [dashboard, setDashboard] = React.useState<FinanceDashboard | null>(null);
  const [debtors, setDebtors] = React.useState<DebtorRow[]>([]);
  const [bucketFilter, setBucketFilter] = React.useState<string>("all");
  // 行明细抽屉：整行可点 → 该欠费商户完整字段（账龄段中文·金额元·应收日中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [dash, rows] = await Promise.all([
        api.get<FinanceDashboard>("/api/v1/analytics/dashboards/finance"),
        api.get<DebtorRow[]>("/api/v1/analytics/finance-receivables/debtors?limit=200"),
      ]);
      setDashboard(dash);
      setDebtors(rows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const metrics = dashboard?.metrics ?? {};
  const balance = cents(metrics["ar.balance"]);
  const aging90p = cents(metrics["ar.aging_90p"]);
  const collected = cents(metrics["ar.collected_mtd"]);
  const receivableMtd = cents(metrics["ar.receivable_mtd"]);
  const rateRaw = metrics["ar.collection_rate_mtd"]?.value;
  const rate = rateRaw === null || rateRaw === undefined ? null : Number(rateRaw);

  const bucketValues = BUCKETS.map((bucket) => ({ ...bucket, amount: cents(metrics[bucket.metric]) ?? 0 }));
  const bucketTotal = bucketValues.reduce((sum, bucket) => sum + bucket.amount, 0);

  const filteredDebtors = bucketFilter === "all" ? debtors : debtors.filter((row) => row.bucket === bucketFilter);
  const paged = usePagedRows(filteredDebtors, 10, bucketFilter);

  function openDebtorDetail(row: DebtorRow) {
    setDetail({
      title: `欠费商户明细 · ${row.merchantName ?? row.merchantId ?? "未挂商户(散项)"}`,
      fields: [
        {
          label: "商户",
          value: row.merchantId
            ? <TraceLink navigateTo={navigateTo} pageId="ADM-FIN-001" objectId={row.merchantId}>{row.merchantName ?? row.merchantId}</TraceLink>
            : "未挂商户(散项)",
        },
        { label: "未清金额", value: money(row.outstandingCent === undefined ? null : Number(row.outstandingCent)) },
        { label: "最早应收日", value: cnDate(row.oldestDueDate) },
        { label: "逾期天数", value: row.overdueDays ?? "—" },
        { label: "账龄段", value: BUCKET_LABELS[row.bucket ?? ""] ?? row.bucket ?? "—" },
      ],
    });
  }

  return (
    <section data-testid="receivables-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.dashboard_domain.viewed / analytics.ar_debtors.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="receivables-freshness">数据截至经营日:{dashboard?.businessDate ?? "—"}(每日快照,约 5 分钟刷新)</div>
        <div className="admin-kpi-grid" data-testid="receivables-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>欠费总额</span><button type="button" className="admin-kpi-value" data-testid="receivables-balance" onClick={() => navigateTo?.("ADM-FIN-001")}>{money(balance)}</button><small>逾期未清(期末时点)，查看应收应付台账</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>坏账预警区</span><button type="button" className="admin-kpi-value" data-testid="receivables-aging90p" onClick={() => navigateTo?.("ADM-FIN-001")}>{money(aging90p)}</button><small>逾期 90 天以上，查看应收应付台账</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>本月回款率</span><button type="button" className="admin-kpi-value" data-testid="receivables-rate" onClick={() => navigateTo?.("ADM-FIN-001")}>{rate === null ? "—" : `${(rate * 100).toFixed(2)}%`}</button><small>实收 {money(collected)} / 应收 {money(receivableMtd)}，查看应收应付台账</small></div>
        </div>
        <h3>账龄分布</h3>
        <div data-testid="receivables-aging-bars">
          {bucketValues.map((bucket) => {
            const pct = bucketTotal > 0 ? Math.round((bucket.amount / bucketTotal) * 100) : 0;
            return (
              <div key={bucket.key} style={{ display: "flex", alignItems: "center", gap: 8, margin: "4px 0" }}>
                <span style={{ width: 170, fontSize: 13 }}>{bucket.label}</span>
                <div style={{ flex: 1, background: "var(--surface-2, #eee)", height: 16 }}>
                  <div style={{ width: `${pct}%`, minWidth: bucket.amount > 0 ? 2 : 0, height: "100%", background: bucket.key === "a90p" ? "#c0392b" : "#4a7fb5" }} />
                </div>
                <span style={{ width: 140, fontSize: 13, textAlign: "right" }} data-testid={`receivables-bucket-${bucket.key}`}>{money(bucket.amount)}</span>
              </div>
            );
          })}
        </div>
        <h3>欠费商户明细(实时,与账龄同口径)</h3>
        <div className="admin-form-row">
          <label>账龄段<select data-testid="receivables-bucket-filter" value={bucketFilter} onChange={(event) => setBucketFilter(event.target.value)}>
            <option value="all">全部</option>
            {BUCKETS.map((bucket) => <option key={bucket.key} value={bucket.key}>{bucket.label}</option>)}
          </select></label>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="receivables-debtors-table">
            <thead>
              <tr><th>商户</th><th>未清金额</th><th>最早应收日</th><th>逾期天数</th><th>账龄段</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row, index) => (
                <tr key={row.merchantId ?? `row-${index}`} className="admin-row-clickable" tabIndex={0} data-testid={`receivables-debtor-row-${row.merchantId ?? index}`}
                  onClick={() => openDebtorDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDebtorDetail(row); } }}>
                  <td onClick={(event) => { if (row.merchantId) event.stopPropagation(); }}>{row.merchantId
                    ? <TraceLink navigateTo={navigateTo} pageId="ADM-FIN-001" objectId={row.merchantId}>{row.merchantName ?? row.merchantId}</TraceLink>
                    : "未挂商户(散项)"}</td>
                  <td>{money(row.outstandingCent === undefined ? null : Number(row.outstandingCent))}</td>
                  <td>{cnDate(row.oldestDueDate)}</td>
                  <td>{row.overdueDays ?? "—"}</td>
                  <td>{BUCKET_LABELS[row.bucket ?? ""] ?? row.bucket ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="receivables-debtors-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
