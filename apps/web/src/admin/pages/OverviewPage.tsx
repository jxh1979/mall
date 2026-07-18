import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import { MiniBars } from "../components/MiniChart";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, OverviewPayload } from "../types";

function metricValue(data: OverviewPayload | null, metricCode: string): number | null {
  const metric = data?.metrics?.[metricCode];
  if (!metric || metric.value === null || metric.value === undefined) return null;
  const parsed = Number(metric.value);
  return Number.isFinite(parsed) ? parsed : null;
}

function metricText(data: OverviewPayload | null, metricCode: string): string {
  const value = metricValue(data, metricCode);
  return value === null ? "—" : String(value);
}

// 同口径指标对比柱（revenue.* 同为「分」，traffic/verify/member 同为人次/数量），
// 以内联 SVG 迷你柱状图替代纯文字趋势，不引入图表依赖。
const REVENUE_BARS: Array<{ code: string; label: string }> = [
  { code: "revenue.ticket", label: "票务" },
  { code: "revenue.bar", label: "酒吧" },
];
const VOLUME_BARS: Array<{ code: string; label: string }> = [
  { code: "traffic.amusement", label: "游乐核销" },
  { code: "verify.done", label: "票务核销" },
  { code: "member.new", label: "新增会员" },
  { code: "risk.pending_review", label: "待复核" },
];

export function OverviewPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
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
  const ticketDisplay = ticketKpi.status === "ready" && ticketKpi.value !== null ? ticketKpi.value : metricValue(data, "revenue.ticket");
  const arDisplay = arKpi.status === "ready" && arKpi.value !== null ? arKpi.value : metricValue(data, "ar.balance");

  const revenueValues = REVENUE_BARS.map((item) =>
    (item.code === "revenue.ticket" ? ticketDisplay : metricValue(data, item.code)) ?? 0);
  const volumeValues = VOLUME_BARS.map((item) => metricValue(data, item.code) ?? 0);
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
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-bar"><span>酒吧收入</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-bar-value" onClick={() => navigateTo?.("ADM-BAR-002")}>{metricText(data, "revenue.bar")}</button><small>已结算账单实收（分），查看酒吧账单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-amusement"><span>游乐核销</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-amusement-value" onClick={() => navigateTo?.("ADM-AMU-001")}>{metricText(data, "traffic.amusement")}</button><small>成功核销人次，查看游乐核销</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-verify"><span>票务核销</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-verify-value" onClick={() => navigateTo?.("ADM-TKT-003")}>{metricText(data, "verify.done")}</button><small>成功核销数，查看出票与核销追踪</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-member"><span>新增会员</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-member-value" onClick={() => navigateTo?.("ADM-CUS-001")}>{metricText(data, "member.new")}</button><small>当日新建，查看客人账号管理</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-risk"><span>待复核</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-risk-value" onClick={() => navigateTo?.("ADM-GOV-006")}>{metricText(data, "risk.pending_review")}</button><small>复核 + 风险预警，查看主管复核</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable" data-testid="overview-kpi-ar"><span>应收欠费余额</span><button type="button" className="admin-kpi-value" data-testid="overview-kpi-ar-value" onClick={() => navigateTo?.("ADM-ANA-004")}>{arDisplay === null ? "—" : String(arDisplay)}</button><small>逾期未清应收（分），查看欠费与回款{arKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="finance.ar_balance" to={asOfDay} testid="overview-metric-detail-ar" /></> : null}</small></div>
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
