import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, OverviewPayload } from "../types";

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
          <div className="admin-kpi-card" data-testid="overview-kpi-ticket"><span>票务</span><strong>{data?.ticketProducts ?? 0}</strong><small>票种 / 出票 {data?.issuedTickets ?? 0}</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-bar"><span>酒吧</span><strong>{data?.barOrders ?? 0}</strong><small>订单闭环</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-amusement"><span>游乐</span><strong>{data?.amusementSessions ?? 0}</strong><small>场次运行</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-finance"><span>财务</span><strong>{data?.merchants ?? 0}</strong><small>主体 / 商户汇总</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-integration"><span>第三方</span><strong>{data?.verifications ?? 0}</strong><small>核销与回调</small></div>
          <div className="admin-kpi-card" data-testid="overview-kpi-audit"><span>审计</span><strong>{data?.auditEvents ?? 0}</strong><small>留痕事件</small></div>
        </div>
      </AdminDataState>
    </section>
  );
}
