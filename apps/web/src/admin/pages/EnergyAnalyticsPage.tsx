import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
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

export function EnergyAnalyticsPage({ api, setGlobalError }: AdminPageProps) {
  const [dashboard, setDashboard] = React.useState<EnergyDashboard | null>(null);
  const [abnormal, setAbnormal] = React.useState<AbnormalRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [dash, rows] = await Promise.all([
        api.get<EnergyDashboard>("/api/v1/analytics/dashboards/energy"),
        api.get<AbnormalRow[]>("/api/v1/analytics/energy/abnormal-readings?limit=200"),
      ]);
      setDashboard(dash);
      setAbnormal(rows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const metrics = dashboard?.metrics ?? {};
  const electricity = num(metrics["energy.total_electricity"]);
  const water = num(metrics["energy.total_water"]);
  const gas = num(metrics["energy.total_gas"]);
  const heatCold = num(metrics["energy.total_heat_cold"]);
  const perSqm = num(metrics["energy.electricity_per_sqm"]);
  const abnormalCount = num(metrics["energy.abnormal_count"]);

  const paged = usePagedRows(abnormal, 10);

  return (
    <section data-testid="energy-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.dashboard_domain.viewed / analytics.abnormal_readings.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="energy-freshness">数据截至经营日:{dashboard?.businessDate ?? "—"}(快照口径,异常明细为实时同口径;环比/同比随快照逐日积累后自动可比)</div>
        <div className="admin-kpi-grid" data-testid="energy-summary">
          <div className="admin-kpi-card"><span>用电</span><strong data-testid="energy-total-electricity">{qty(electricity, "kWh")}</strong><small>当日经营窗合计</small></div>
          <div className="admin-kpi-card"><span>用水</span><strong>{qty(water, "吨")}</strong><small>当日经营窗合计</small></div>
          <div className="admin-kpi-card"><span>用气</span><strong>{qty(gas, "m³")}</strong><small>当日经营窗合计</small></div>
          <div className="admin-kpi-card"><span>冷热量</span><strong>{qty(heatCold, "单位")}</strong><small>当日经营窗合计</small></div>
          <div className="admin-kpi-card"><span>每平米电耗</span><strong data-testid="energy-per-sqm">{qty(perSqm, "kWh/㎡")}</strong><small>分母=可租面积</small></div>
          <div className="admin-kpi-card"><span>异常读数</span><strong data-testid="energy-abnormal-count">{abnormalCount === null ? "—" : abnormalCount}</strong><small>待核查</small></div>
        </div>
        <h3>异常读数明细(实时,与异常计数同口径)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-abnormal-table">
            <thead><tr><th>表计</th><th>类型</th><th>抄表期</th><th>用量</th><th>异常原因</th><th>抄表时间</th></tr></thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.readingId}>
                  <td>{row.meterName ?? row.meterId ?? "—"}</td>
                  <td>{TYPE_LABELS[row.meterType ?? ""] ?? row.meterType ?? "—"}</td>
                  <td>{row.period ?? "—"}</td>
                  <td>{row.consumption === undefined ? "—" : new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(Number(row.consumption))}</td>
                  <td>{REASON_LABELS[row.abnormalReason ?? ""] ?? row.abnormalReason ?? "—"}</td>
                  <td>{localTime(row.readAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="energy-abnormal-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
