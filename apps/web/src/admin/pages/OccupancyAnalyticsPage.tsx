import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps } from "../types";

type MetricValue = { value?: number | string | null; unit?: string | null; note?: string | null };
type LeasingDashboard = { businessDate?: string; metrics?: Record<string, MetricValue> };
type VacantRow = { spaceId: string; spaceName?: string; spaceType?: string; floorNo?: string; areaSqm?: number | string; rentalStatus?: string };
type ExpiringRow = { contractId: string; merchantId?: string; merchantName?: string; expiryDate?: string; daysLeft?: number; boundArea?: number | string };

const TEXT = {
  title: "出租与到期",
  description: "铺位租没租出去、空在哪、哪些合同快到期——分母=商铺+摊位且排除人工不可租(车位单列),已签未生效单列「已签待交」不算已租。",
  loadFailed: "出租分析读取失败",
};

const RENTAL_LABELS: Record<string, string> = { vacant: "空置", negotiating: "洽谈中", moved_out: "已退租" };
const TYPE_LABELS: Record<string, string> = { shop: "商铺", booth: "摊位" };

function num(metric?: MetricValue): number | null {
  if (!metric || metric.value === null || metric.value === undefined) return null;
  return Number(metric.value);
}

function sqm(value: number | null | undefined) {
  if (value === null || value === undefined || !Number.isFinite(value)) return "—";
  return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value)} ㎡`;
}

function pct(value: number | null) {
  return value === null ? "—" : `${(value * 100).toFixed(2)}%`;
}

export function OccupancyAnalyticsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [dashboard, setDashboard] = React.useState<LeasingDashboard | null>(null);
  const [vacant, setVacant] = React.useState<VacantRow[]>([]);
  const [expiring, setExpiring] = React.useState<ExpiringRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [dash, vacantRows, expiringRows] = await Promise.all([
        api.get<LeasingDashboard>("/api/v1/analytics/dashboards/leasing"),
        api.get<VacantRow[]>("/api/v1/analytics/occupancy/vacant-spaces?limit=200"),
        api.get<ExpiringRow[]>("/api/v1/analytics/occupancy/expiring-contracts?window_days=90&limit=200"),
      ]);
      setDashboard(dash);
      setVacant(vacantRows ?? []);
      setExpiring(expiringRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const metrics = dashboard?.metrics ?? {};
  const rate = num(metrics["occupancy.rate"]);
  const rentable = num(metrics["occupancy.rentable_area"]);
  const leased = num(metrics["occupancy.leased_area"]);
  const signedPending = num(metrics["occupancy.signed_pending_area"]);
  const parkingRate = num(metrics["occupancy.parking_rate"]);
  const expiring90 = num(metrics["contract.expiring_90d"]);
  const vacantArea = rentable !== null && leased !== null ? rentable - leased : null;

  const pagedVacant = usePagedRows(vacant, 10);
  const pagedExpiring = usePagedRows(expiring, 10);

  return (
    <section data-testid="occupancy-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.dashboard_domain.viewed / analytics.vacant_spaces.viewed / analytics.expiring_contracts.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="occupancy-freshness">数据截至经营日:{dashboard?.businessDate ?? "—"}(期末时点口径,明细为实时同口径)</div>
        <div className="admin-kpi-grid" data-testid="occupancy-summary">
          <div className="admin-kpi-card"><span>出租率</span><strong data-testid="occupancy-rate">{pct(rate)}</strong><small>已租 {sqm(leased)} / 可租 {sqm(rentable)}</small></div>
          <div className="admin-kpi-card"><span>空置面积</span><strong>{sqm(vacantArea)}</strong><small>可招商</small></div>
          <div className="admin-kpi-card"><span>已签待交</span><strong>{sqm(signedPending)}</strong><small>签约未生效,不计已租</small></div>
          <div className="admin-kpi-card"><span>90 天内到期</span><strong data-testid="occupancy-expiring">{expiring90 === null ? "—" : expiring90}</strong><small>生效合同</small></div>
          <div className="admin-kpi-card"><span>车位出租率</span><strong>{pct(parkingRate)}</strong><small>单列口径,不与铺位混算</small></div>
        </div>
        <h3>空置铺位明细(实时,与出租率同口径)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="occupancy-vacant-table">
            <thead><tr><th>铺位</th><th>类型</th><th>楼层</th><th>面积</th><th>状态</th></tr></thead>
            <tbody>
              {pagedVacant.pageRows.map((row) => (
                <tr key={row.spaceId}>
                  <td>{row.spaceName ?? row.spaceId}</td>
                  <td>{TYPE_LABELS[row.spaceType ?? ""] ?? row.spaceType ?? "—"}</td>
                  <td>{row.floorNo ?? "—"}</td>
                  <td>{sqm(row.areaSqm === undefined ? null : Number(row.areaSqm))}</td>
                  <td>{RENTAL_LABELS[row.rentalStatus ?? ""] ?? row.rentalStatus ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="occupancy-vacant-pager" page={pagedVacant.page} pageCount={pagedVacant.pageCount} total={pagedVacant.total} onPrev={pagedVacant.prev} onNext={pagedVacant.next} />
        </div>
        <h3>到期预警(90 天内,按到期日升序)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="occupancy-expiring-table">
            <thead><tr><th>合同</th><th>商户</th><th>到期日</th><th>剩余天数</th><th>绑定面积</th></tr></thead>
            <tbody>
              {pagedExpiring.pageRows.map((row) => (
                <tr key={row.contractId}>
                  <td><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId}</TraceLink></td>
                  <td>{row.merchantName ?? row.merchantId ?? "—"}</td>
                  <td>{row.expiryDate ?? "—"}</td>
                  <td>{row.daysLeft ?? "—"}</td>
                  <td>{sqm(row.boundArea === undefined ? null : Number(row.boundArea))}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="occupancy-expiring-pager" page={pagedExpiring.page} pageCount={pagedExpiring.pageCount} total={pagedExpiring.total} onPrev={pagedExpiring.prev} onNext={pagedExpiring.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
