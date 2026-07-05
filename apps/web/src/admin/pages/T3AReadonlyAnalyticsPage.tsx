import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { TechDetails } from "../components/TechDetails";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { formatMetricDomain } from "../i18n/metricLabels";
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

// KPI 聚焦范式（照 OrganizationMerchantPage 样板）：点 KPI 数字=带 ?kpi= 参数重进本页，只渲染对应明细表。
// 机器人/商户健康/员工服务/客户意向/指标定义各有承载表；指标快照本页无明细表 → 保留纯数字不可点（见报告说明）。
type KpiTarget = "robot" | "merchant" | "staff" | "intent" | "definition";
function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const t = new URLSearchParams(window.location.search).get("kpi");
  return t === "robot" || t === "merchant" || t === "staff" || t === "intent" || t === "definition" ? t : null;
}
function kpiFocusTitle(target: KpiTarget): string {
  return { robot: "机器人指标", merchant: "商户健康分", staff: "员工服务分", intent: "客户意向", definition: "指标定义" }[target];
}

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

/**
 * T3-A 只读经营分析：把机器人、商户健康、员工服务、客户意向、指标定义、指标快照六类分析读接口
 * 聚合成一个只读视图。全页只读——只发 GET、不发任何控制指令,供只读/审计账号安全查看。
 */
export function T3AReadonlyAnalyticsPage({ api, setGlobalError }: AdminPageProps) {
  const [data, setData] = React.useState<T3AData | null>(null);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(target: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", target);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(target);
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

  const robot = data ? summarizeRobotMetrics(data.robot) : { robots: 0, tasks: 0, success: 0 };
  const show = (target: KpiTarget) => !kpiFocus || kpiFocus === target;

  return (
    <section data-testid="t3a-readonly-analytics-page">
      <AdminPageHeader title="T3-A 只读经营分析" description="只读分析视图：聚合机器人、商户健康、员工服务、客户意向、指标定义与指标快照六类只读接口，只看不动、不发任何控制指令。" />
      <AuditTrailHint event="analytics.readonly.viewed" />
      <TechDetails><div className="admin-generic-evidence" data-testid="t3a-evidence">{t3aReadOnlyApiPaths.join(" / ")}</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {/* T3A_READONLY_ANALYTICS_VIEW_START：以下为只读视图区域,禁止任何写/POST(门禁强制)。 */}
        {data ? (
          <div data-testid="t3a-readonly-view">
            <p className="admin-muted-action" data-testid="t3a-readonly-note">只读分析，不发控制指令：本页仅聚合读取分析域快照，不做任何写入或设备控制。</p>
            {kpiFocus ? (
              <div className="admin-state" data-testid="t3a-kpi-focus-banner">
                正在查看：{kpiFocusTitle(kpiFocus)}
                <button type="button" data-testid="t3a-kpi-focus-clear" onClick={exitKpiFocus}>清除筛选，返回全部</button>
              </div>
            ) : (
              <div className="admin-kpi-grid" data-testid="t3a-summary">
                <div className="admin-kpi-card admin-kpi-card--clickable"><span>机器人</span><button type="button" className="admin-kpi-value" data-testid="t3a-robot-count" onClick={() => enterKpiFocus("robot")}>{robot.robots}</button><small>任务 {robot.tasks} / 成功 {robot.success}</small></div>
                <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户健康分</span><button type="button" className="admin-kpi-value" data-testid="t3a-kpi-merchant" onClick={() => enterKpiFocus("merchant")}>{data.merchant.length}</button></div>
                <div className="admin-kpi-card admin-kpi-card--clickable"><span>员工服务分</span><button type="button" className="admin-kpi-value" data-testid="t3a-kpi-staff" onClick={() => enterKpiFocus("staff")}>{data.staff.length}</button></div>
                <div className="admin-kpi-card admin-kpi-card--clickable"><span>客户意向</span><button type="button" className="admin-kpi-value" data-testid="t3a-kpi-intent" onClick={() => enterKpiFocus("intent")}>{data.intent.length}</button></div>
                <div className="admin-kpi-card admin-kpi-card--clickable"><span>指标定义</span><button type="button" className="admin-kpi-value" data-testid="t3a-kpi-definition" onClick={() => enterKpiFocus("definition")}>{data.definition.length}</button></div>
                <div className="admin-kpi-card"><span>指标快照</span><strong>{data.snapshot.length}</strong></div>
              </div>
            )}
            {show("robot") && (<>
              <h3>机器人指标</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="t3a-robot-table">
                  <thead><tr><th>机器人</th><th>供应商</th><th>任务</th><th>成功</th><th>失败</th><th>日期</th></tr></thead>
                  <tbody>{data.robot.slice(0, 50).map((row) => (<tr key={row.metricId}><td>{row.robotId ?? "-"}</td><td>{row.providerCode ?? "-"}</td><td>{row.taskCount ?? 0}</td><td>{row.successCount ?? 0}</td><td>{row.failureCount ?? 0}</td><td>{row.businessDate ?? "-"}</td></tr>))}</tbody>
                </table>
              </div>
            </>)}
            {show("merchant") && (<>
              <h3>商户健康分</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="t3a-merchant-table">
                  <thead><tr><th>商户</th><th>分数</th><th>等级</th><th>风险数</th><th>逾期应收(分)</th></tr></thead>
                  <tbody>{data.merchant.slice(0, 50).map((row) => (<tr key={row.merchantId}><td>{row.merchantId ?? "-"}</td><td>{row.score ?? "-"}</td><td>{formatBusinessStatus(row.scoreLevel)}</td><td>{row.riskCount ?? 0}</td><td>{row.overdueArAmountCents ?? 0}</td></tr>))}</tbody>
                </table>
              </div>
            </>)}
            {show("staff") && (<>
              <h3>员工服务分</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="t3a-staff-table">
                  <thead><tr><th>员工</th><th>岗位</th><th>服务分</th><th>投诉</th><th>风险</th></tr></thead>
                  <tbody>{data.staff.slice(0, 50).map((row) => (<tr key={row.scoreId}><td>{row.staffName ?? "-"}</td><td>{formatBusinessStatus(row.roleCode)}</td><td>{row.serviceScore ?? "-"}</td><td>{row.complaintCount ?? 0}</td><td>{formatBusinessStatus(row.riskLevel)}</td></tr>))}</tbody>
                </table>
              </div>
            </>)}
            {show("intent") && (<>
              <h3>客户意向</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="t3a-intent-table">
                  <thead><tr><th>客户</th><th>意向类型</th><th>风险</th><th>摘要</th></tr></thead>
                  <tbody>{data.intent.slice(0, 50).map((row) => (<tr key={row.intentId}><td>{row.customerRef ?? "-"}</td><td>{row.intentType ?? "-"}</td><td>{formatBusinessStatus(row.riskLevel)}</td><td>{row.summary ?? "-"}</td></tr>))}</tbody>
                </table>
              </div>
            </>)}
            {show("definition") && (<>
              <h3>指标定义</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="t3a-definition-table">
                  <thead><tr><th>指标</th><th>名称</th><th>业务域</th><th>单位</th><th>状态</th></tr></thead>
                  <tbody>{data.definition.slice(0, 50).map((row) => (<tr key={row.metricKey}><td>{row.metricKey ?? "-"}</td><td>{row.name ?? "-"}</td><td>{row.domain ? formatMetricDomain(row.domain) : "-"}</td><td>{row.unit ?? "-"}</td><td>{formatBusinessStatus(row.status)}</td></tr>))}</tbody>
                </table>
              </div>
            </>)}
          </div>
        ) : null}
        {/* T3A_READONLY_ANALYTICS_VIEW_END */}
      </AdminDataState>
    </section>
  );
}
