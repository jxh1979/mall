import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, OverviewPayload } from "../types";

function metricText(data: OverviewPayload | null, metricCode: string): string {
  const metric = data?.metrics?.[metricCode];
  if (!metric || metric.value === null || metric.value === undefined) {
    return "—";
  }
  return String(metric.value);
}

export function OverviewPage({ api, setGlobalError }: AdminPageProps) {
  const [data, setData] = React.useState<OverviewPayload | null>(null);
  const [dateFrom, setDateFrom] = React.useState("");
  const [dateTo, setDateTo] = React.useState("");
  const [appliedRange, setAppliedRange] = React.useState<string | null>(null);

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
    void reload();
  }

  return (
    <section data-testid="admin-overview">
      <AdminPageHeader title="总控驾驶舱" description="汇总票务、酒吧、游乐、财务、第三方和审计异常，支持部分失败可见。" />
      <div className="admin-form-row" data-testid="overview-date-filter">
        <label>起始<input type="date" data-testid="overview-date-from" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} /></label>
        <label>结束<input type="date" data-testid="overview-date-to" value={dateTo} onChange={(event) => setDateTo(event.target.value)} /></label>
        <button type="button" data-testid="overview-date-apply" onClick={applyDateRange}>筛选时间</button>
        {appliedRange ? <span className="admin-muted-action" data-testid="overview-date-applied">已筛选：{appliedRange}</span> : null}
      </div>
      <AdminDataState loading={loading} error={error} empty={!data} onRetry={reload}>
        <div className="admin-kpi-grid">
          <div className="admin-kpi-card" data-testid="overview-kpi-ticket"><span>票务收入</span><strong>{metricText(data, "revenue.ticket")}</strong><small>已支付订单实付（分）</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-bar"><span>酒吧收入</span><strong>{metricText(data, "revenue.bar")}</strong><small>已结算账单实收（分）</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-amusement"><span>游乐核销</span><strong>{metricText(data, "traffic.amusement")}</strong><small>成功核销人次</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-verify"><span>票务核销</span><strong>{metricText(data, "verify.done")}</strong><small>成功核销数</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-member"><span>新增会员</span><strong>{metricText(data, "member.new")}</strong><small>当日新建</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-risk"><span>待复核</span><strong>{metricText(data, "risk.pending_review")}</strong><small>复核 + 风险预警</small></div>
        </div>
      </AdminDataState>
    </section>
  );
}
