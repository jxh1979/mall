// 文件版本：V0.04
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import { MiniBars } from "../components/MiniChart";
import { useAdminResource } from "../hooks/useAdminResource";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, OverviewPayload } from "../types";

function metricValue(data: OverviewPayload | null, metricCode: string): number | null {
  const metric = data?.metrics?.[metricCode];
  if (!metric || metric.value === null || metric.value === undefined) return null;
  const parsed = Number(metric.value);
  return Number.isFinite(parsed) ? parsed : null;
}

// 同口径指标对比柱（revenue.* 同为「分」，traffic/verify/member 同为人次/数量），
// 以内联 SVG 迷你柱状图替代纯文字趋势，不引入图表依赖。
const REVENUE_BARS: Array<{ code: string; label: string }> = [
  { code: "revenue.ticket", label: "票务" },
  { code: "revenue.bar", label: "酒吧" },
];
// ROOT-04 补录数量的状态另列（裁决 M02「补录提交即计数，状态另列」）。
const BACKFILL_STATUSES = ["pending", "approved", "rejected"];

const VOLUME_BARS: Array<{ code: string; label: string }> = [
  { code: "traffic.amusement", label: "游乐核销" },
  { code: "verify.done", label: "票务核销" },
  { code: "member.new", label: "新增会员" },
  { code: "risk.pending_review", label: "待复核" },
];

export function OverviewPage({ api, setGlobalError, navigateTo, effectivePermissions }: AdminPageProps) {
  const [data, setData] = React.useState<OverviewPayload | null>(null);
  const [dateFrom, setDateFrom] = React.useState("");
  const [dateTo, setDateTo] = React.useState("");
  const [appliedRange, setAppliedRange] = React.useState<string | null>(null);
  const [appliedDates, setAppliedDates] = React.useState<{ from: string; to: string }>({ from: "", to: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const params = new URLSearchParams();
      if (dateFrom) params.set("from", dateFrom);
      if (dateTo) params.set("to", dateTo);
      const query = params.toString();
      setData(await api.get<OverviewPayload>(`/api/v1/analytics/dashboards/p0-overview${query ? `?${query}` : ""}`));
    },
    { onError: setGlobalError, fallbackMessage: "总控数据读取失败", onStart: () => setGlobalError(null) },
  );

  function applyDateRange() {
    setAppliedRange(dateFrom || dateTo ? `${dateFrom || "不限"} ~ ${dateTo || "不限"}` : null);
    setAppliedDates({ from: dateFrom, to: dateTo });
    void reload();
  }

  // 首批接线（指标登记簿方案A）：票务收入/应收欠费余额两个 KPI 数字改走统一明细端点（与明细页同源，
  // 数字=明细合计按构造成立）；as-of 日与总览端点同规则（to 优先，退化 from，再退最新经营日）。
  // 端点失败（含无权限）→ 回退既有快照值并隐藏「完整明细」入口，体验不降级。
  const asOfDay = appliedDates.to || appliedDates.from || data?.businessDate || "";
  const ticketKpi = useMetricKpiValue(api, "revenue.ticket", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const arKpi = useMetricKpiValue(api, "finance.ar_balance", { to: asOfDay, enabled: !!asOfDay });
  // 第二批接线（酒吧域）：酒吧收入卡改走统一明细端点（与明细页同源，已确认商品收入加月结服务费扣留），失败回退快照值。
  const barKpi = useMetricKpiValue(api, "revenue.bar", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const ticketDisplay = ticketKpi.status === "ready" && ticketKpi.value !== null ? ticketKpi.value : metricValue(data, "revenue.ticket");
  const arDisplay = arKpi.status === "ready" && arKpi.value !== null ? arKpi.value : metricValue(data, "ar.balance");
  const barDisplay = barKpi.status === "ready" && barKpi.value !== null ? barKpi.value : metricValue(data, "revenue.bar");
  // 第六批接线（总控驾驶舱剩余卡）：游乐核销/票务核销/新增会员/待复核四卡改走统一明细端点（与明细页同源，谓词逐字抄总览 ETL 聚合），
  // 失败（含无权限）回退既有快照值并隐藏「完整明细」入口。可加类同经营日 as-of（from=to），待复核为余额类（仅 to）。
  const amusementKpi = useMetricKpiValue(api, "traffic.amusement", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const verifyKpi = useMetricKpiValue(api, "verify.done", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const memberKpi = useMetricKpiValue(api, "member.new", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const riskKpi = useMetricKpiValue(api, "risk.pending_review", { to: asOfDay, enabled: !!asOfDay });
  const amusementDisplay = amusementKpi.status === "ready" && amusementKpi.value !== null ? amusementKpi.value : metricValue(data, "traffic.amusement");
  const verifyDisplay = verifyKpi.status === "ready" && verifyKpi.value !== null ? verifyKpi.value : metricValue(data, "verify.done");
  const memberDisplay = memberKpi.status === "ready" && memberKpi.value !== null ? memberKpi.value : metricValue(data, "member.new");
  const riskDisplay = riskKpi.status === "ready" && riskKpi.value !== null ? riskKpi.value : metricValue(data, "risk.pending_review");
  // ROOT-04：第三方回流收入（单列，不计入净收入）、采购到货（按商品及单位分列、不出合计，裁决 M01）、补录数量（提交即计数、状态另列，裁决 M02）。
  // 同上：统一明细端点优先，失败（含无权限）回退总览值并隐藏「完整明细」入口。
  const thirdpartyKpi = useMetricKpiValue(api, "revenue.thirdparty", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const receivedKpi = useMetricKpiValue(api, "supply.received", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const backfillKpi = useMetricKpiValue(api, "risk.manual_backfill", { from: asOfDay, to: asOfDay, enabled: !!asOfDay });
  const thirdpartyDisplay = thirdpartyKpi.status === "ready" && thirdpartyKpi.value !== null ? thirdpartyKpi.value : metricValue(data, "revenue.thirdparty");
  const receivedGroups = (receivedKpi.status === "ready" && receivedKpi.groups
    ? receivedKpi.groups
    : (data?.metrics?.["supply.received"]?.dimension?.groups as Array<Record<string, unknown>> | undefined)) ?? [];
  const backfillValue = metricValue(data, "risk.manual_backfill");
  const backfillStatusCounts = (data?.metrics?.["risk.manual_backfill"]?.dimension?.statusCounts ?? {}) as Record<string, unknown>;
  // 第十八批 S 段第 173 条 173-R1（BSM:1824「财务：日结状态、对账差异、结算待确认。」；BSM:1747「待生成、待确认、有差异、已确认、已锁定、已归档」）：
  // 看板所看营业日、查看人可见范围内各日结按状态计数，复用日结列表接口（范围与「有差异」由后端算）；一笔都没有即「待生成」（同 143-R1）。
  // 状态不能相加，不进指标目录、不写快照。持 finance.daily_close.manage 才请求与显示；失败显示「—」。
  // ponytail: 每日日结按范围一行，200 行够用，超了再分页。
  const canSeeDailyClose = hasPermissions(effectivePermissions, ["finance.daily_close.manage"]);
  const [dailyCloseStatuses, setDailyCloseStatuses] = React.useState<string[] | null>(null);
  React.useEffect(() => {
    setDailyCloseStatuses(null);
    if (!canSeeDailyClose || !asOfDay) return;
    let cancelled = false;
    api
      .getPaged<{ status?: string }>(`/api/v1/finance/daily-closes?business_date=${asOfDay}&page=1&size=200`)
      .then((page) => { if (!cancelled) setDailyCloseStatuses(page.items.map((row) => row.status ?? "")); })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [api, canSeeDailyClose, asOfDay]);
  const dailyCloseDisplay = dailyCloseStatuses === null ? "—" : dailyCloseStatuses.length === 0 ? "待生成"
    : Object.entries(KEY_VALUE_LABELS.daily_close_status ?? {}).map(([code, label]) => `${label} ${dailyCloseStatuses.filter((status) => status === code).length}`).join(" · ");
  // 第十八批 Y 段第 183 条 183-R1（BSM:1824「财务：日结状态、对账差异、结算待确认。」；BSM:1839「卡片上的数字就是它下面明细的合计」）：
  // 复用指标 finance.recon_diff_count（所看营业日新发现的对账差异笔数，含已关闭，平台口径）。持 finance.reconciliation.view 才请求与显示；
  // 端点拒绝（如项目级账号 403）显示「—」、不出完整明细入口。
  const canSeeReconciliation = hasPermissions(effectivePermissions, ["finance.reconciliation.view"]);
  const reconDiffKpi = useMetricKpiValue(api, "finance.recon_diff_count", { from: asOfDay, to: asOfDay, enabled: canSeeReconciliation && !!asOfDay });
  // 第十八批 Y 段第 183 条 183-R2（BSM:1824「财务：……结算待确认。」；第十八批裁决 40「数演出结算挂起待确认的、积分月结账单有未处理异议的，
  // 分两个数显示，不相加」）：取查询时状态（不随所看营业日变、不进指标目录、不写快照）。只请求所持权限那一类，没权限的那一类不显示；失败显示「—」。
  const canSeeMediaPending = hasPermissions(effectivePermissions, ["media.performance.view"]);
  const canSeePointsObjections = hasPermissions(effectivePermissions, ["ticket.points.billing.view"]);
  const [mediaPending, setMediaPending] = React.useState<number | null>(null);
  const [pointsObjections, setPointsObjections] = React.useState<number | null>(null);
  React.useEffect(() => {
    if (!canSeeMediaPending) return;
    let cancelled = false;
    api
      .get<{ pendingCount: number }>("/api/v1/settlement/media-shows/pending-settlement-count")
      .then((result) => { if (!cancelled) setMediaPending(result.pendingCount); })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [api, canSeeMediaPending]);
  React.useEffect(() => {
    if (!canSeePointsObjections) return;
    let cancelled = false;
    api
      .get<Array<{ openObjections: number }>>("/api/v1/loyalty/ticket-points-billing/bills")
      .then((bills) => { if (!cancelled) setPointsObjections(bills.filter((bill) => bill.openObjections > 0).length); })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, [api, canSeePointsObjections]);

  const revenueValues = REVENUE_BARS.map((item) =>
    (item.code === "revenue.ticket" ? ticketDisplay : item.code === "revenue.bar" ? barDisplay : metricValue(data, item.code)) ?? 0);
  // 量对比柱与卡片头数同源（走统一端点显示值，回退快照），避免柱=快照而头数=实时的漂移。
  const volumeDisplayByCode: Record<string, number | null> = {
    "traffic.amusement": amusementDisplay,
    "verify.done": verifyDisplay,
    "member.new": memberDisplay,
    "risk.pending_review": riskDisplay,
  };
  const volumeValues = VOLUME_BARS.map((item) => (volumeDisplayByCode[item.code] ?? metricValue(data, item.code)) ?? 0);
  const hasRevenue = revenueValues.some((value) => value > 0);
  const hasVolume = volumeValues.some((value) => value > 0);

  return (
    <section data-testid="admin-overview">
      <AdminPageHeader title="总控驾驶舱" description="汇总票务、酒吧、游乐、财务、第三方和审计异常，支持部分失败可见。" />
      <div className="admin-form-row" data-testid="overview-date-filter">
        <label>起始<CnDatePicker data-testid="overview-date-from" value={dateFrom} onChange={setDateFrom} /></label>
        <label>结束<CnDatePicker data-testid="overview-date-to" value={dateTo} onChange={setDateTo} /></label>
        <button type="button" data-testid="overview-date-apply" onClick={applyDateRange}>筛选时间</button>
        {appliedRange ? <span className="admin-muted-action" data-testid="overview-date-applied">已筛选：{appliedRange}</span> : null}
      </div>
      <AdminDataState loading={loading} error={error} empty={!data} onRetry={reload}>
        <div className="admin-kpi-grid">
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-ticket"><span>票务收入</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-ticket-value" onClick={() => navigateTo?.("ADM-TKT-003")}>{ticketDisplay === null ? "—" : String(ticketDisplay)}</button><small>已支付订单实付（分），查看出票与核销追踪{ticketKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="revenue.ticket" from={asOfDay} to={asOfDay} testid="overview-metric-detail-ticket" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-bar"><span>酒吧收入</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-bar-value" onClick={() => navigateTo?.("ADM-BAR-002")}>{barDisplay === null ? "—" : String(barDisplay)}</button><small>已确认商品收入＋月结服务费扣留（分），查看酒吧账单{barKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="revenue.bar" from={asOfDay} to={asOfDay} testid="overview-metric-detail-bar" /></> : null}</small>{data?.metrics?.["revenue.bar"]?.note ? <small>{data.metrics["revenue.bar"].note}</small> : null}</div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-amusement"><span>游乐核销</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-amusement-value" onClick={() => navigateTo?.("ADM-AMU-001")}>{amusementDisplay === null ? "—" : String(amusementDisplay)}</button><small>成功核销记录数（入场+出场），查看游乐核销{amusementKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="traffic.amusement" from={asOfDay} to={asOfDay} testid="overview-metric-detail-amusement" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-verify"><span>票务核销</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-verify-value" onClick={() => navigateTo?.("ADM-TKT-003")}>{verifyDisplay === null ? "—" : String(verifyDisplay)}</button><small>成功核销数，查看出票与核销追踪{verifyKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="verify.done" from={asOfDay} to={asOfDay} testid="overview-metric-detail-verify" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-member"><span>新增会员</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-member-value" onClick={() => navigateTo?.("ADM-CUS-001")}>{memberDisplay === null ? "—" : String(memberDisplay)}</button><small>当日新建，查看客人账号管理{memberKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="member.new" from={asOfDay} to={asOfDay} testid="overview-metric-detail-member" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-risk"><span>待复核</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-risk-value" onClick={() => navigateTo?.("ADM-GOV-006")}>{riskDisplay === null ? "—" : String(riskDisplay)}</button><small>复核 + 风险预警，查看主管复核{riskKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="risk.pending_review" to={asOfDay} testid="overview-metric-detail-risk" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-ar"><span>应收欠费余额</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-ar-value" onClick={() => navigateTo?.("ADM-ANA-004")}>{arDisplay === null ? "—" : String(arDisplay)}</button><small>逾期未清应收（分），查看欠费与回款{arKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="finance.ar_balance" to={asOfDay} testid="overview-metric-detail-ar" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-thirdparty"><span>第三方回流收入</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-thirdparty-value" onClick={() => navigateTo?.("ADM-INT-002")}>{thirdpartyDisplay === null ? "—" : String(thirdpartyDisplay)}</button><small>餐饮/零售已入账回流订单（分）；渠道团购票已计入票务收入；不计入净收入{thirdpartyKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="revenue.thirdparty" from={asOfDay} to={asOfDay} testid="overview-metric-detail-thirdparty" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-received"><span>采购到货</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-received-value" onClick={() => navigateTo?.("ADM-INV-003")}>{receivedGroups.length === 0 ? "当日无采购到货" : receivedGroups.map((group, index) => {
            const unit = String(group.unit ?? "");
            const rejected = Number(group.rejectedQuantity ?? 0);
            return <span key={String(group.itemId ?? index)} style={{ display: "block" }}>{`${String(group.itemName ?? "")} ${Number(group.acceptedQuantity ?? 0)}${unit}`}{rejected > 0 ? `（拒收 ${rejected}${unit}）` : null}</span>;
          })}</button><small>按商品及单位分列合格接收，不同单位不相加，查看采购单{receivedKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="supply.received" from={asOfDay} to={asOfDay} testid="overview-metric-detail-received" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-backfill"><span>补录数量</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-backfill-value" onClick={() => navigateTo?.("ADM-GOV-006")}>{backfillValue === null ? "—" : String(backfillValue)}</button><small data-testid="overview-kpi-backfill-status">{BACKFILL_STATUSES.map((status) => `${formatBusinessStatus(status)} ${Number(backfillStatusCounts[status] ?? 0)}`).join(" · ")}</small><small>提交即计一笔（锁账后补录、领取时刻补证），查看主管复核{backfillKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="risk.manual_backfill" from={asOfDay} to={asOfDay} testid="overview-metric-detail-backfill" /></> : null}</small></div>
          {canSeeDailyClose ? <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-dayclose"><span>日结状态</span><button type="button" className="admin-kpi-value" style={{ fontSize: 14, whiteSpace: "normal" }} data-testid="overview-kpi-dayclose-value" onClick={() => navigateTo?.("ADM-FIN-002")}>{dailyCloseDisplay}</button><small>所看营业日各范围日结，查不到即待生成，查看对账差异页日结区块</small></div> : null}
          {canSeeReconciliation ? <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-recondiff"><span>对账差异</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-recondiff-value" onClick={() => navigateTo?.("ADM-FIN-002")}>{reconDiffKpi.status === "ready" && reconDiffKpi.value !== null ? String(reconDiffKpi.value) : "—"}</button><small>所看营业日新发现的对账差异笔数（含已关闭），查看对账差异页{reconDiffKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="finance.recon_diff_count" from={asOfDay} to={asOfDay} testid="overview-metric-detail-recondiff" /></> : null}</small></div> : null}
          {canSeeMediaPending || canSeePointsObjections ? <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-settlepending"><span>结算待确认</span><span data-testid="overview-kpi-settlepending-value">{canSeeMediaPending ? <button type="button" className="admin-kpi-value" style={{ fontSize: 14 }} data-testid="overview-kpi-settlepending-media" onClick={() => navigateTo?.("ADM-STL-004")}>演出结算 {mediaPending ?? "—"}</button> : null}{canSeeMediaPending && canSeePointsObjections ? " · " : null}{canSeePointsObjections ? <button type="button" className="admin-kpi-value" style={{ fontSize: 14 }} data-testid="overview-kpi-settlepending-points" onClick={() => navigateTo?.("ADM-FIN-008")}>积分账单异议 {pointsObjections ?? "—"}</button> : null}</span><small>演出结算挂起待确认的条数、积分月结账单有未处理异议的张数，分开计、不相加，点各数查看</small></div> : null}
        </div>
        {hasRevenue || hasVolume ? (
          <div className="admin-chart-panel" data-testid="overview-charts">
            {hasRevenue ? (
              <div className="admin-chart-block" data-testid="overview-chart-revenue">
                <span>收入对比（票务 / 酒吧，单位：分）</span>
                <MiniBars values={revenueValues} width={160} height={48} testid="overview-revenue-bars" ariaLabel="票务与酒吧收入对比" />
                <small className="admin-muted-action">{REVENUE_BARS.map((item, index) => `${item.label} ${revenueValues[index]}`).join(" · ")}</small>
              </div>
            ) : null}
            {hasVolume ? (
              <div className="admin-chart-block" data-testid="overview-chart-volume">
                <span>核销 / 会员 / 风险量对比</span>
                {/* 直读横向条形：每行 名称 + 条 + 数值，0 值也可读；替代无标注的悬空色块。 */}
                <div className="admin-hbars" data-testid="overview-volume-bars" role="img" aria-label="核销会员风险量对比">
                  {VOLUME_BARS.map((item, index) => {
                    const value = volumeValues[index] ?? 0;
                    const max = Math.max(...volumeValues, 1);
                    return (
                      <div className="admin-hbar-row" key={item.code} data-testid={`overview-volume-bar-${item.code}`}>
                        <span className="admin-hbar-label">{item.label}</span>
                        <span className="admin-hbar-track"><span className="admin-hbar-fill" style={{ width: `${Math.round((value / max) * 100)}%` }} /></span>
                        <span className="admin-hbar-value admin-num">{value}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}
      </AdminDataState>
    </section>
  );
}
