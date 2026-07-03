import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";

// T3A_READONLY_ANALYTICS_READY：T3-A 只读经营分析视图——只聚合读取分析域快照(机器人/商户健康/员工服务/客户意向/指标定义/指标快照),
// 不发任何控制指令、不做任何写入(无 POST/写路径),供只读账号安全查看。2026-06 改版丢失前端后重建为独立 admin 页。
export type RobotMetric = { metricId?: string; robotId?: string; providerCode?: string; taskCount?: number; successCount?: number; failureCount?: number; businessDate?: string; summary?: string };
export type AnalyticsMerchantSummary = { merchantId?: string; score?: number; scoreLevel?: string; riskCount?: number; overdueArAmountCents?: number; summary?: string };
type StaffServiceScore = { scoreId?: string; staffName?: string; roleCode?: string; serviceScore?: number; complaintCount?: number; riskLevel?: string };
type CustomerIntent = { intentId?: string; customerRef?: string; intentType?: string; riskLevel?: string; summary?: string };
type MetricDefinition = { metricKey?: string; name?: string; domain?: string; unit?: string; status?: string };
type MetricSnapshot = { metricKey?: string; segmentType?: string; actualValue?: number | string; businessDate?: string };

type T3AData = {
  robot: RobotMetric[]; merchant: AnalyticsMerchantSummary[]; staff: StaffServiceScore[];
  intent: CustomerIntent[]; definition: MetricDefinition[]; snapshot: MetricSnapshot[];
};

/** T3-A 只读的 6 个分析读接口(纯读,不含任何写路径)。 */
const t3aReadOnlyApiPaths = [
  "/api/v1/analytics/business-metric-snapshots",
  "/api/v1/analytics/merchant-health",
  "/api/v1/analytics/staff-service-score",
  "/api/v1/analytics/customer-intents",
  "/api/v1/analytics/robot",
  "/api/v1/analytics/metric-definitions",
];

/** 机器人指标只读派生:机器人台数/任务总数/成功总数(仅前端汇总,不落库、不控制设备)。 */
function summarizeRobotMetrics(metrics: RobotMetric[]) {
  return {
    robots: new Set(metrics.map((item) => item.robotId).filter(Boolean)).size,
    tasks: metrics.reduce((sum, item) => sum + (item.taskCount ?? 0), 0),
    success: metrics.reduce((sum, item) => sum + (item.successCount ?? 0), 0),
  };
}

function asRows<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  const items = (value as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

/** 渲染 T3-A 只读分析视图:纯展示(KPI + 明细表),无操作按钮、无写请求。 */
function renderT3AReadOnlyAnalytics(data: T3AData) {
  const robot = summarizeRobotMetrics(data.robot);
  return (
    <div data-testid="t3a-readonly-view">
      <p className="admin-muted-action" data-testid="t3a-readonly-note">只读分析，不发控制指令：本页仅聚合读取分析域快照，不做任何写入或设备控制。</p>
      <div className="admin-kpi-grid" data-testid="t3a-summary">
        <div className="admin-kpi-card"><span>机器人</span><strong data-testid="t3a-robot-count">{robot.robots}</strong><small>任务 {robot.tasks} / 成功 {robot.success}</small></div>
        <div className="admin-kpi-card"><span>商户健康分</span><strong>{data.merchant.length}</strong></div>
        <div className="admin-kpi-card"><span>员工服务分</span><strong>{data.staff.length}</strong></div>
        <div className="admin-kpi-card"><span>客户意向</span><strong>{data.intent.length}</strong></div>
        <div className="admin-kpi-card"><span>指标定义</span><strong>{data.definition.length}</strong></div>
        <div className="admin-kpi-card"><span>指标快照</span><strong>{data.snapshot.length}</strong></div>
      </div>
      <h3>机器人指标</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="t3a-robot-table">
          <thead><tr><th>机器人</th><th>供应商</th><th>任务</th><th>成功</th><th>失败</th><th>日期</th></tr></thead>
          <tbody>{data.robot.slice(0, 50).map((row) => (<tr key={row.metricId}><td>{row.robotId ?? "-"}</td><td>{row.providerCode ?? "-"}</td><td>{row.taskCount ?? 0}</td><td>{row.successCount ?? 0}</td><td>{row.failureCount ?? 0}</td><td>{row.businessDate ?? "-"}</td></tr>))}</tbody>
        </table>
      </div>
      <h3>商户健康分</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="t3a-merchant-table">
          <thead><tr><th>商户</th><th>分数</th><th>等级</th><th>风险数</th><th>逾期应收(分)</th></tr></thead>
          <tbody>{data.merchant.slice(0, 50).map((row) => (<tr key={row.merchantId}><td>{row.merchantId ?? "-"}</td><td>{row.score ?? "-"}</td><td>{row.scoreLevel ?? "-"}</td><td>{row.riskCount ?? 0}</td><td>{row.overdueArAmountCents ?? 0}</td></tr>))}</tbody>
        </table>
      </div>
      <h3>员工服务分</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="t3a-staff-table">
          <thead><tr><th>员工</th><th>岗位</th><th>服务分</th><th>投诉</th><th>风险</th></tr></thead>
          <tbody>{data.staff.slice(0, 50).map((row) => (<tr key={row.scoreId}><td>{row.staffName ?? "-"}</td><td>{row.roleCode ?? "-"}</td><td>{row.serviceScore ?? "-"}</td><td>{row.complaintCount ?? 0}</td><td>{row.riskLevel ?? "-"}</td></tr>))}</tbody>
        </table>
      </div>
      <h3>客户意向</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="t3a-intent-table">
          <thead><tr><th>客户</th><th>意向类型</th><th>风险</th><th>摘要</th></tr></thead>
          <tbody>{data.intent.slice(0, 50).map((row) => (<tr key={row.intentId}><td>{row.customerRef ?? "-"}</td><td>{row.intentType ?? "-"}</td><td>{row.riskLevel ?? "-"}</td><td>{row.summary ?? "-"}</td></tr>))}</tbody>
        </table>
      </div>
      <h3>指标定义</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="t3a-definition-table">
          <thead><tr><th>指标</th><th>名称</th><th>业务域</th><th>单位</th><th>状态</th></tr></thead>
          <tbody>{data.definition.slice(0, 50).map((row) => (<tr key={row.metricKey}><td>{row.metricKey ?? "-"}</td><td>{row.name ?? "-"}</td><td>{row.domain ?? "-"}</td><td>{row.unit ?? "-"}</td><td>{row.status ?? "-"}</td></tr>))}</tbody>
        </table>
      </div>
    </div>
  );
}

/**
 * T3-A 只读经营分析：把机器人、商户健康、员工服务、客户意向、指标定义、指标快照六类分析读接口
 * 聚合成一个只读视图。全页只读——只发 GET、不发任何控制指令,供只读/审计账号安全查看。
 */
export function T3AReadonlyAnalyticsPage({ api, setGlobalError }: AdminPageProps) {
  const [data, setData] = React.useState<T3AData | null>(null);
  const { loading, error, reload } = useAdminResource(
    async () => {
      // 只读并发拉 6 个分析接口;某接口权限不足(如 risk_alert.view)时用 allSettled 局部降级,不整页崩。
      const settled = await Promise.allSettled(t3aReadOnlyApiPaths.map((path) => api.get<unknown>(path)));
      const at = (index: number): unknown => {
        const outcome = settled[index];
        return outcome && outcome.status === "fulfilled" ? outcome.value : [];
      };
      setData({
        snapshot: asRows<MetricSnapshot>(at(0)), merchant: asRows<AnalyticsMerchantSummary>(at(1)),
        staff: asRows<StaffServiceScore>(at(2)), intent: asRows<CustomerIntent>(at(3)),
        robot: asRows<RobotMetric>(at(4)), definition: asRows<MetricDefinition>(at(5)),
      });
    },
    { onError: setGlobalError, fallbackMessage: "只读分析读取失败", onStart: () => setGlobalError(null) },
  );

  return (
    <section data-testid="t3a-readonly-analytics-page">
      <AdminPageHeader title="T3-A 只读经营分析" description="只读分析视图：聚合机器人、商户健康、员工服务、客户意向、指标定义与指标快照六类只读接口，只看不动、不发任何控制指令。" />
      <AuditTrailHint event="analytics.readonly.viewed" />
      <TechDetails><div className="admin-generic-evidence" data-testid="t3a-evidence">{t3aReadOnlyApiPaths.join(" / ")}</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {/* T3A_READONLY_ANALYTICS_VIEW_START：以下为只读视图区域,禁止任何写/POST(门禁强制)。 */}
        {data ? renderT3AReadOnlyAnalytics(data) : null}
        {/* T3A_READONLY_ANALYTICS_VIEW_END */}
      </AdminDataState>
    </section>
  );
}
