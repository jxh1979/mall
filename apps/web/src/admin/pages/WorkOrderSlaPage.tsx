import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";

type MetricValue = { value?: number | string | null; unit?: string | null; note?: string | null };
type FacilityDashboard = { businessDate?: string; metrics?: Record<string, MetricValue> };
type OverdueRow = { workOrderId: string; code?: string; title?: string; status?: string; priority?: string; assigneeId?: string; handleDueAt?: string; completedAt?: string; overdueHours?: number | string };

const TEXT = {
  title: "工单时效",
  description: "两类工单分行对照:设施工单(物业报修/保洁)看响应与处理快不快;商户服务工单(商户专区提交)看处理与超时。超时率分母=处理时限落在当日经营窗的单,积压=此刻还没干完的活。",
  loadFailed: "工单时效读取失败",
};

const STATUS_LABELS: Record<string, string> = {
  pending_assign: "待派单", assigned: "已派单", processing: "处理中", pending_review: "待复核",
  pending_acceptance: "待验收", accepted: "已验收", closed: "已关闭", suspended: "已挂起", overtime: "已超时",
  created: "已提交", pending_merchant: "待商户确认", resolved: "已处理待关闭", cancelled: "已取消",
};
const PRIORITY_LABELS: Record<string, string> = { urgent: "紧急", critical: "紧急", high: "高", normal: "普通", medium: "中", low: "低" };

function num(metric?: MetricValue): number | null {
  if (!metric || metric.value === null || metric.value === undefined) return null;
  return Number(metric.value);
}

function hours(value: number | null) {
  return value === null ? "—" : `${value.toFixed(1)} 小时`;
}

function pct(value: number | null) {
  return value === null ? "—" : `${(value * 100).toFixed(2)}%`;
}

function localTime(value?: string) {
  return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "—";
}

function OverdueTable({ testid, rows, completedLabel }: { testid: string; rows: OverdueRow[]; completedLabel: string }) {
  const paged = usePagedRows(rows, 10);
  return (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid={testid}>
        <thead><tr><th>编号</th><th>标题</th><th>状态</th><th>优先级</th><th>处理时限</th><th>{completedLabel}</th><th>已超时</th></tr></thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.workOrderId}>
              <td>{row.code ?? row.workOrderId}</td>
              <td>{row.title ?? "—"}</td>
              <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
              <td>{PRIORITY_LABELS[row.priority ?? ""] ?? row.priority ?? "—"}</td>
              <td>{localTime(row.handleDueAt)}</td>
              <td>{row.completedAt ? localTime(row.completedAt) : "未完成"}</td>
              <td>{row.overdueHours === undefined ? "—" : `${Number(row.overdueHours).toFixed(1)} 小时`}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <TablePager testid={`${testid.replace(/-table$/, "")}-pager`} page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
    </div>
  );
}

export function WorkOrderSlaPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [dashboard, setDashboard] = React.useState<FacilityDashboard | null>(null);
  const [overdue, setOverdue] = React.useState<OverdueRow[]>([]);
  const [overdueNotice, setOverdueNotice] = React.useState<string | null>(null);
  const [merchantOverdue, setMerchantOverdue] = React.useState<OverdueRow[]>([]);
  const [merchantOverdueNotice, setMerchantOverdueNotice] = React.useState<string | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：两张超时明细下钻表可能因权限/500 单独失败——各自降级为区块提示，
      // 不拖垮响应/处理/超时率等仪表盘 KPI 主内容。
      const [dash, rows, merchantRows] = await Promise.all([
        api.get<FacilityDashboard>("/api/v1/analytics/dashboards/facility"),
        loadSecondary(() => api.get<OverdueRow[]>("/api/v1/analytics/work-order-sla/overdue-orders?limit=200"), [] as OverdueRow[]),
        loadSecondary(() => api.get<OverdueRow[]>("/api/v1/analytics/work-order-sla/overdue-orders?source=merchant&limit=200"), [] as OverdueRow[]),
      ]);
      setDashboard(dash);
      setOverdue(rows.data);
      setOverdueNotice(rows.notice);
      setMerchantOverdue(merchantRows.data);
      setMerchantOverdueNotice(merchantRows.notice);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const metrics = dashboard?.metrics ?? {};
  const responseP50 = num(metrics["wo.response_hours_p50"]);
  const handleP50 = num(metrics["wo.handle_hours_p50"]);
  const overdueRate = num(metrics["wo.overdue_rate"]);
  const backlog = num(metrics["wo.backlog"]);
  const mwoHandleP50 = num(metrics["mwo.handle_hours_p50"]);
  const mwoOverdueRate = num(metrics["mwo.overdue_rate"]);
  const mwoBacklog = num(metrics["mwo.backlog"]);

  return (
    <section data-testid="wo-sla-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.dashboard_domain.viewed / analytics.overdue_work_orders.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="wo-sla-freshness">数据截至经营日:{dashboard?.businessDate ?? "—"}(快照口径,超时明细为实时同口径)</div>
        <h3>设施工单(物业报修/保洁)</h3>
        <div className="admin-kpi-grid" data-testid="wo-sla-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>响应中位数</span><button type="button" className="admin-kpi-value" data-testid="wo-response-p50" onClick={() => navigateTo?.("ADM-FAC-001")}>{hours(responseP50)}</button><small>接单到首次响应，查看楼宇运营工单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>处理中位数</span><button type="button" className="admin-kpi-value" data-testid="wo-handle-p50" onClick={() => navigateTo?.("ADM-FAC-001")}>{hours(handleP50)}</button><small>派单到完成，查看楼宇运营工单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>超时率</span><button type="button" className="admin-kpi-value" data-testid="wo-overdue-rate" onClick={() => navigateTo?.("ADM-FAC-001")}>{pct(overdueRate)}</button><small>分母=时限落当日窗的单，查看楼宇运营工单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>当前积压</span><button type="button" className="admin-kpi-value" data-testid="wo-backlog" onClick={() => navigateTo?.("ADM-FAC-001")}>{backlog === null ? "—" : backlog}</button><small>未完成存量(时点)，查看楼宇运营工单</small></div>
        </div>
        <h3>商户服务工单(商户专区提交)</h3>
        <div className="admin-kpi-grid" data-testid="mwo-sla-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>处理中位数</span><button type="button" className="admin-kpi-value" data-testid="mwo-handle-p50" onClick={() => navigateTo?.("ADM-MER-001")}>{hours(mwoHandleP50)}</button><small>提交到关单，查看商户服务工单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>超时率</span><button type="button" className="admin-kpi-value" data-testid="mwo-overdue-rate" onClick={() => navigateTo?.("ADM-MER-001")}>{pct(mwoOverdueRate)}</button><small>含超时升级判定,已取消不计，查看商户服务工单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>当前积压</span><button type="button" className="admin-kpi-value" data-testid="mwo-backlog" onClick={() => navigateTo?.("ADM-MER-001")}>{mwoBacklog === null ? "—" : mwoBacklog}</button><small>未完成存量(不含已处理待关闭)，查看商户服务工单</small></div>
        </div>
        <div className="admin-muted-action">商户服务工单流转没有「首次响应」节点,响应中位数不适用(不伪造)。</div>
        <h3>设施工单超时明细(实时,与超时率分子同口径)</h3>
        {overdueNotice ? (
          <div className="admin-state" data-testid="wo-overdue-degraded">{overdueNotice}</div>
        ) : (
          <OverdueTable testid="wo-overdue-table" rows={overdue} completedLabel="完成时间" />
        )}
        <h3>商户服务工单超时明细(实时,与商户单超时率分子同口径)</h3>
        {merchantOverdueNotice ? (
          <div className="admin-state" data-testid="mwo-overdue-degraded">{merchantOverdueNotice}</div>
        ) : (
          <OverdueTable testid="mwo-overdue-table" rows={merchantOverdue} completedLabel="关单时间" />
        )}
      </AdminDataState>
    </section>
  );
}
