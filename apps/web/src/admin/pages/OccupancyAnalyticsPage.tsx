import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

type MetricValue = { value?: number | string | null; unit?: string | null; note?: string | null };
type LeasingDashboard = { businessDate?: string; metrics?: Record<string, MetricValue> };
type VacantRow = { spaceId: string; spaceName?: string; spaceType?: string; floorNo?: string; areaSqm?: number | string; rentalStatus?: string };
type ExpiringRow = { contractId: string; merchantId?: string; merchantName?: string; expiryDate?: string; daysLeft?: number; boundArea?: number | string };

const TEXT = {
  title: "出租与到期",
  description: "铺位租没租出去、空在哪、哪些合同快到期——分母=商铺+摊位且排除人工不可租(车位单列),已签未生效单列「已签待交」不算已租。",
  loadFailed: "出租分析读取失败",
  focusClear: "清除筛选，返回全部",
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

// KPI 聚焦（照样板）：本页多为比率/面积指标（无承载表，不可点）；仅「空置面积」承载空置铺位明细表、
// 「90 天内到期」承载到期预警表两卡可点。?kpi=<vacant|expiring>。
type KpiTable = "vacant" | "expiring";

function readKpiFocusFromUrl(): KpiTable | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  return table === "vacant" || table === "expiring" ? table : null;
}

export function OccupancyAnalyticsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [dashboard, setDashboard] = React.useState<LeasingDashboard | null>(null);
  const [vacant, setVacant] = React.useState<VacantRow[]>([]);
  const [vacantNotice, setVacantNotice] = React.useState<string | null>(null);
  const [expiring, setExpiring] = React.useState<ExpiringRow[]>([]);
  const [expiringNotice, setExpiringNotice] = React.useState<string | null>(null);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTable | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该空置铺位/到期合同完整字段（类型/状态中文·面积·日期中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTable) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(table);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：空置铺位/到期预警两张下钻表可能因权限/500 单独失败——
      // 各自降级为区块提示，不拖垮出租率等仪表盘 KPI 主内容。
      const [dash, vacantRows, expiringRows] = await Promise.all([
        api.get<LeasingDashboard>("/api/v1/analytics/dashboards/leasing"),
        loadSecondary(() => api.get<VacantRow[]>("/api/v1/analytics/occupancy/vacant-spaces?limit=200"), [] as VacantRow[]),
        loadSecondary(() => api.get<ExpiringRow[]>("/api/v1/analytics/occupancy/expiring-contracts?window_days=90&limit=200"), [] as ExpiringRow[]),
      ]);
      setDashboard(dash);
      setVacant(vacantRows.data);
      setVacantNotice(vacantRows.notice);
      setExpiring(expiringRows.data);
      setExpiringNotice(expiringRows.notice);
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

  function openVacantDetail(row: VacantRow) {
    setDetail({
      title: `空置铺位明细 · ${row.spaceName ?? row.spaceId}`,
      fields: [
        { label: "铺位", value: row.spaceName ?? row.spaceId },
        { label: "类型", value: TYPE_LABELS[row.spaceType ?? ""] ?? row.spaceType ?? "—" },
        { label: "楼层", value: row.floorNo ?? "—" },
        { label: "面积", value: sqm(row.areaSqm === undefined ? null : Number(row.areaSqm)) },
        { label: "状态", value: RENTAL_LABELS[row.rentalStatus ?? ""] ?? row.rentalStatus ?? "—" },
        { label: "铺位编号", value: row.spaceId },
      ],
    });
  }

  function openExpiringDetail(row: ExpiringRow) {
    setDetail({
      title: `到期预警明细 · ${row.merchantName ?? row.merchantId ?? row.contractId}`,
      fields: [
        {
          label: "合同",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId}</TraceLink>,
        },
        { label: "商户", value: row.merchantName ?? row.merchantId ?? "—" },
        { label: "到期日", value: cnDate(row.expiryDate) },
        { label: "剩余天数", value: row.daysLeft === undefined ? "—" : `${row.daysLeft} 天` },
        { label: "绑定面积", value: sqm(row.boundArea === undefined ? null : Number(row.boundArea)) },
      ],
    });
  }

  const vacantSection = (
    <>
      <h3>空置铺位明细(实时,与出租率同口径)</h3>
      {vacantNotice ? (
        <div className="admin-state" data-testid="occupancy-vacant-degraded">{vacantNotice}</div>
      ) : (
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="occupancy-vacant-table">
          <thead><tr><th>铺位</th><th>类型</th><th>楼层</th><th>面积</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>
            {pagedVacant.pageRows.map((row) => (
              <tr key={row.spaceId} className="admin-row-clickable" tabIndex={0} data-testid={`occupancy-vacant-row-${row.spaceId}`}
                onClick={() => openVacantDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openVacantDetail(row); } }}>
                <td>{row.spaceName ?? row.spaceId}</td>
                <td>{TYPE_LABELS[row.spaceType ?? ""] ?? row.spaceType ?? "—"}</td>
                <td>{row.floorNo ?? "—"}</td>
                <td>{sqm(row.areaSqm === undefined ? null : Number(row.areaSqm))}</td>
                <td>{RENTAL_LABELS[row.rentalStatus ?? ""] ?? row.rentalStatus ?? "—"}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空置铺位详情" onClick={(event) => { event.stopPropagation(); openVacantDetail(row); }}>查看详情</button></td></tr>
            ))}
          </tbody>
        </table>
        <TablePager testid="occupancy-vacant-pager" page={pagedVacant.page} pageCount={pagedVacant.pageCount} total={pagedVacant.total} onPrev={pagedVacant.prev} onNext={pagedVacant.next} />
      </div>
      )}
    </>
  );

  const expiringSection = (
    <>
      <h3>到期预警(90 天内,按到期日升序)</h3>
      {expiringNotice ? (
        <div className="admin-state" data-testid="occupancy-expiring-degraded">{expiringNotice}</div>
      ) : (
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="occupancy-expiring-table">
          <thead><tr><th>合同</th><th>商户</th><th>到期日</th><th>剩余天数</th><th>绑定面积</th><th>详情</th></tr></thead>
          <tbody>
            {pagedExpiring.pageRows.map((row) => (
              <tr key={row.contractId} className="admin-row-clickable" tabIndex={0} data-testid={`occupancy-expiring-row-${row.contractId}`}
                onClick={() => openExpiringDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openExpiringDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId}</TraceLink></td>
                <td>{row.merchantName ?? row.merchantId ?? "—"}</td>
                <td>{cnDate(row.expiryDate)}</td>
                <td>{row.daysLeft ?? "—"}</td>
                <td>{sqm(row.boundArea === undefined ? null : Number(row.boundArea))}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看到期预警详情" onClick={(event) => { event.stopPropagation(); openExpiringDetail(row); }}>查看详情</button></td></tr>
            ))}
          </tbody>
        </table>
        <TablePager testid="occupancy-expiring-pager" page={pagedExpiring.page} pageCount={pagedExpiring.pageCount} total={pagedExpiring.total} onPrev={pagedExpiring.prev} onNext={pagedExpiring.next} />
      </div>
      )}
    </>
  );

  return (
    <section data-testid="occupancy-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.dashboard_domain.viewed / analytics.vacant_spaces.viewed / analytics.expiring_contracts.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="occupancy-kpi-focus-banner">
              正在查看：{kpiFocus === "vacant" ? "空置铺位明细" : "到期预警"}（{kpiFocus === "vacant" ? vacant.length : expiring.length}）
              <button type="button" data-testid="occupancy-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus === "vacant" ? vacantSection : expiringSection}
          </>
        ) : (
          <>
            <div className="admin-muted-action" data-testid="occupancy-freshness">数据截至经营日:{dashboard?.businessDate ?? "—"}(期末时点口径,明细为实时同口径)</div>
            <div className="admin-kpi-grid" data-testid="occupancy-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>出租率</span><button type="button" className="admin-kpi-value" data-testid="occupancy-rate" onClick={() => navigateTo?.("ADM-LEASE-002")}>{pct(rate)}</button><small>已租 {sqm(leased)} / 可租 {sqm(rentable)}，查看租赁状态看板</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空置面积</span><button type="button" className="admin-kpi-value" data-testid="occupancy-kpi-vacant" onClick={() => enterKpiFocus("vacant")}>{sqm(vacantArea)}</button><small>可招商,查看空置铺位</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已签待交</span><button type="button" className="admin-kpi-value" data-testid="occupancy-signed-pending" onClick={() => navigateTo?.("ADM-LEASE-002")}>{sqm(signedPending)}</button><small>签约未生效,不计已租，查看租赁状态看板</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>90 天内到期</span><button type="button" className="admin-kpi-value" data-testid="occupancy-kpi-expiring" onClick={() => enterKpiFocus("expiring")}>{expiring90 === null ? "—" : expiring90}</button><small>生效合同,查看到期预警</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>车位出租率</span><button type="button" className="admin-kpi-value" data-testid="occupancy-parking-rate" onClick={() => navigateTo?.("ADM-LEASE-002")}>{pct(parkingRate)}</button><small>单列口径,不与铺位混算，查看租赁状态看板</small></div>
            </div>
            {vacantSection}
            {expiringSection}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
