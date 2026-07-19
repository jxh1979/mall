import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

type MetricValue = { value?: number | string | null; unit?: string | null; note?: string | null };
type EnergyDashboard = { businessDate?: string; metrics?: Record<string, MetricValue> };
type AbnormalRow = { readingId: string; meterId?: string; meterName?: string; meterType?: string; period?: string; consumption?: number | string; abnormalReason?: string; readAt?: string };

const TEXT = {
  title: "能耗分析",
  description: "当日用了多少电/水/气、每平米电耗高不高、哪些读数异常——电/水/气/冷热单位不同不混算,每平米电耗分母=可租面积(与出租率同分母)。",
  loadFailed: "能耗分析读取失败",
};

const TYPE_LABELS: Record<string, string> = { electricity: "电", water: "水", gas: "气", heat_cold: "冷热" };
const REASON_LABELS: Record<string, string> = { surge: "突增", drop: "突降", zero: "零读数", reverse: "倒走" };

function num(metric?: MetricValue): number | null {
  if (!metric || metric.value === null || metric.value === undefined) return null;
  return Number(metric.value);
}

function qty(value: number | null, unit: string) {
  if (value === null || !Number.isFinite(value)) return "—";
  return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value)} ${unit}`;
}

function localTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "—";
}

function consumptionText(value?: number | string) {
  return value === undefined || value === null ? "—" : new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(Number(value));
}

export function EnergyAnalyticsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [dashboard, setDashboard] = React.useState<EnergyDashboard | null>(null);
  const [abnormal, setAbnormal] = React.useState<AbnormalRow[]>([]);
  const [abnormalNotice, setAbnormalNotice] = React.useState<string | null>(null);
  // 行明细抽屉：整行可点 → 该异常读数完整字段（类型/原因中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：异常读数明细是仪表盘之外的下钻列表，可能因权限/500 单独失败——
      // 降级为区块提示，不拖垮当日能耗指标主内容。
      const [dash, rows] = await Promise.all([
        api.get<EnergyDashboard>("/api/v1/analytics/dashboards/energy"),
        loadSecondary(() => api.get<AbnormalRow[]>("/api/v1/analytics/energy/abnormal-readings?limit=200"), [] as AbnormalRow[]),
      ]);
      setDashboard(dash);
      setAbnormal(rows.data);
      setAbnormalNotice(rows.notice);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const metrics = dashboard?.metrics ?? {};
  // 首批接线（指标登记簿方案A）：用电/每平米电耗两个数字改走统一明细端点（与明细页同源）；
  // 区间=当前经营日（与快照口径同日）；端点失败 → 回退快照值并隐藏「完整明细」入口。
  const businessDate = dashboard?.businessDate ?? "";
  const electricityKpi = useMetricKpiValue(api, "energy.total_electricity", { from: businessDate, to: businessDate, enabled: !!businessDate });
  const perSqmKpi = useMetricKpiValue(api, "energy.electricity_per_sqm", { from: businessDate, to: businessDate, enabled: !!businessDate });
  const electricity = electricityKpi.status === "ready" && electricityKpi.value !== null
    ? electricityKpi.value : num(metrics["energy.total_electricity"]);
  const water = num(metrics["energy.total_water"]);
  const gas = num(metrics["energy.total_gas"]);
  const heatCold = num(metrics["energy.total_heat_cold"]);
  const perSqm = perSqmKpi.status === "ready" && perSqmKpi.value !== null
    ? perSqmKpi.value : num(metrics["energy.electricity_per_sqm"]);
  const abnormalCount = num(metrics["energy.abnormal_count"]);

  const paged = usePagedRows(abnormal, 10);

  function openAbnormalDetail(row: AbnormalRow) {
    setDetail({
      title: `异常读数明细 · ${row.meterName ?? row.meterId ?? "—"}`,
      fields: [
        { label: "表计", value: row.meterName ?? row.meterId ?? "—" },
        { label: "类型", value: TYPE_LABELS[row.meterType ?? ""] ?? row.meterType ?? "—" },
        { label: "抄表期", value: row.period ?? "—" },
        { label: "用量", value: consumptionText(row.consumption) },
        { label: "异常原因", value: REASON_LABELS[row.abnormalReason ?? ""] ?? row.abnormalReason ?? "—" },
        { label: "抄表时间", value: localTime(row.readAt) },
        { label: "读数编号", value: row.readingId },
      ],
    });
  }

  return (
    <section data-testid="energy-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.dashboard_domain.viewed / analytics.abnormal_readings.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="energy-freshness">数据截至经营日:{dashboard?.businessDate ?? "—"}(快照口径,异常明细为实时同口径;环比/同比随快照逐日积累后自动可比)</div>
        <div className="admin-kpi-grid" data-testid="energy-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>用电</span><button type="button" className="admin-kpi-value" data-testid="energy-total-electricity" onClick={() => navigateTo?.("ADM-ENE-001")}>{qty(electricity, "kWh")}</button><small>当日经营窗合计，查看能源工作台{electricityKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="energy.total_electricity" from={businessDate} to={businessDate} testid="energy-metric-detail-electricity" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>用水</span><button type="button" className="admin-kpi-value" data-testid="energy-total-water" onClick={() => navigateTo?.("ADM-ENE-001")}>{qty(water, "吨")}</button><small>当日经营窗合计，查看能源工作台</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>用气</span><button type="button" className="admin-kpi-value" data-testid="energy-total-gas" onClick={() => navigateTo?.("ADM-ENE-001")}>{qty(gas, "m³")}</button><small>当日经营窗合计，查看能源工作台</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>冷热量</span><button type="button" className="admin-kpi-value" data-testid="energy-total-heat-cold" onClick={() => navigateTo?.("ADM-ENE-001")}>{qty(heatCold, "单位")}</button><small>当日经营窗合计，查看能源工作台</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>每平米电耗</span><button type="button" className="admin-kpi-value" data-testid="energy-per-sqm" onClick={() => navigateTo?.("ADM-ENE-001")}>{qty(perSqm, "kWh/㎡")}</button><small>分母=可租面积，查看能源工作台{perSqmKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="energy.electricity_per_sqm" from={businessDate} to={businessDate} testid="energy-metric-detail-per-sqm" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>异常读数</span><button type="button" className="admin-kpi-value" data-testid="energy-abnormal-count" onClick={() => navigateTo?.("ADM-ENE-001")}>{abnormalCount === null ? "—" : abnormalCount}</button><small>待核查，查看能源工作台</small></div>
        </div>
        <h3>异常读数明细(实时,与异常计数同口径)</h3>
        {abnormalNotice ? (
          <div className="admin-state" data-testid="energy-abnormal-degraded">{abnormalNotice}</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-abnormal-table">
            <thead><tr><th>表计</th><th>类型</th><th>抄表期</th><th>用量</th><th>异常原因</th><th>抄表时间</th><th>详情</th></tr></thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.readingId} className="admin-row-clickable" tabIndex={0} data-testid={`energy-abnormal-row-${row.readingId}`}
                  onClick={() => openAbnormalDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAbnormalDetail(row); } }}>
                  <td>{row.meterName ?? row.meterId ?? "—"}</td>
                  <td>{TYPE_LABELS[row.meterType ?? ""] ?? row.meterType ?? "—"}</td>
                  <td>{row.period ?? "—"}</td>
                  <td>{consumptionText(row.consumption)}</td>
                  <td>{REASON_LABELS[row.abnormalReason ?? ""] ?? row.abnormalReason ?? "—"}</td>
                  <td>{localTime(row.readAt)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看异常读数详情" onClick={(event) => { event.stopPropagation(); openAbnormalDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="energy-abnormal-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
