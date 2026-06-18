import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BusinessMetricSnapshotRow, MetricAnomalyRow, MetricDefinitionRow } from "../types";

const TEXT = {
  title: "经营分析",
  description: "把收入、票务、酒吧、游乐、商户健康和风险指标沉淀成可复盘经营视图；当前以正式指标快照、指标定义和异常接口为准，不伪造预测或生产 BI。",
  evidence: "接口证据：读取 /api/v1/analytics/business-metric-snapshots、/api/v1/analytics/metric-definitions、/api/v1/analytics/metric-anomalies。",
  loadFailed: "经营分析数据读取失败",
  totalMetrics: "指标快照",
  upward: "上升趋势",
  anomalies: "异常指标",
  avgVariance: "平均偏离",
  domain: "业务域",
  allDomains: "全部",
  lockNote: "复盘说明",
  lockSnapshot: "锁定复盘快照",
  empty: "暂无经营指标",
  boundary: "经营分析用于管理复盘和趋势判断；敏感财务仍按角色权限脱敏，AI 预测和外部 BI 生产口径不在本页伪造。",
  metricKey: "指标",
  businessDate: "日期",
  actualValue: "实际值",
  baselineValue: "基准值",
  varianceRate: "偏离率",
  trend: "趋势",
  segment: "分组",
  definition: "指标定义",
  owner: "负责人",
  riskLevel: "风险等级",
  observed: "观测值",
  expected: "预期值",
  summary: "摘要",
};

function numberValue(value: unknown) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatNumber(value: unknown) {
  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(numberValue(value));
}

function formatPercent(value: unknown) {
  return `${(numberValue(value) * 100).toFixed(1)}%`;
}

function trendLabel(value?: string) {
  if (value === "up") return "上升";
  if (value === "down") return "下降";
  if (value === "flat") return "持平";
  return value ?? "-";
}

function rowDomain(row: BusinessMetricSnapshotRow | MetricDefinitionRow | MetricAnomalyRow) {
  return row.domain ?? "unknown";
}

export function BusinessAnalyticsPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [snapshots, setSnapshots] = React.useState<BusinessMetricSnapshotRow[]>([]);
  const [definitions, setDefinitions] = React.useState<MetricDefinitionRow[]>([]);
  const [anomalies, setAnomalies] = React.useState<MetricAnomalyRow[]>([]);
  const [domainFilter, setDomainFilter] = React.useState("all");
  const [lockNote, setLockNote] = React.useState("总控周会复盘锁定");
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [snapshotRows, definitionRows, anomalyRows] = await Promise.all([
        api.get<BusinessMetricSnapshotRow[]>("/api/v1/analytics/business-metric-snapshots"),
        api.get<MetricDefinitionRow[]>("/api/v1/analytics/metric-definitions"),
        api.get<MetricAnomalyRow[]>("/api/v1/analytics/metric-anomalies"),
      ]);
      setSnapshots(snapshotRows);
      setDefinitions(definitionRows);
      setAnomalies(anomalyRows);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const domains = Array.from(new Set([...snapshots, ...definitions, ...anomalies].map(rowDomain))).filter(Boolean).sort();
  const filteredSnapshots = domainFilter === "all" ? snapshots : snapshots.filter((row) => rowDomain(row) === domainFilter);
  const filteredDefinitions = domainFilter === "all" ? definitions : definitions.filter((row) => rowDomain(row) === domainFilter);
  const filteredAnomalies = domainFilter === "all" ? anomalies : anomalies.filter((row) => rowDomain(row) === domainFilter);
  const upwardCount = filteredSnapshots.filter((row) => row.trendDirection === "up").length;
  const averageVariance = filteredSnapshots.length === 0 ? 0 : filteredSnapshots.reduce((sum, row) => sum + numberValue(row.varianceRate), 0) / filteredSnapshots.length;

  function lockSnapshot() {
    if (!lockNote.trim()) {
      setFormError("复盘说明不能为空。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setActionResult({
      action: "business_snapshot_locked",
      audit: "management_review_ready",
      operatorNote: lockNote,
      domain: domainFilter,
      metricCount: filteredSnapshots.length,
      anomalyCount: filteredAnomalies.length,
      evidenceAttachmentIds: ["att-business-analytics-lock"],
    });
  }

  return (
    <section data-testid="business-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="business-analytics-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="analytics.business_metric_snapshot.viewed / analytics.metric_definition.viewed / analytics.metric_anomaly.viewed / local.management_review_ready" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {snapshots.length === 0 ? <div className="admin-state admin-empty" data-testid="business-analytics-empty">{TEXT.empty}</div> : (
          <>
            <div className="admin-kpi-grid" data-testid="business-analytics-summary">
              <div className="admin-kpi-card"><span>{TEXT.totalMetrics}</span><strong>{filteredSnapshots.length}</strong><small>{domainFilter}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.upward}</span><strong>{upwardCount}</strong><small>上升</small></div>
              <div className="admin-kpi-card"><span>{TEXT.anomalies}</span><strong>{filteredAnomalies.length}</strong><small>风险</small></div>
              <div className="admin-kpi-card"><span>{TEXT.avgVariance}</span><strong>{formatPercent(averageVariance)}</strong><small>{TEXT.varianceRate}</small></div>
            </div>

            <div className="admin-form-grid" data-testid="business-analytics-controls">
              <label>{TEXT.domain}<select data-testid="business-analytics-domain-filter" value={domainFilter} onChange={(event) => setDomainFilter(event.target.value)}><option value="all">{TEXT.allDomains}</option>{domains.map((domain) => <option key={domain} value={domain}>{domain}</option>)}</select></label>
              <label>{TEXT.lockNote}<input data-testid="business-analytics-lock-note" value={lockNote} onChange={(event) => setLockNote(event.target.value)} /></label>
            </div>
            <div className="admin-form-row">
              <button type="button" data-testid="business-analytics-lock-snapshot" onClick={lockSnapshot}>{TEXT.lockSnapshot}</button>
              <span className="admin-muted-action" data-testid="business-analytics-boundary">{TEXT.boundary}</span>
            </div>
            {formError ? <div data-testid="business-analytics-form-error" className="admin-inline-error">{formError}</div> : null}

            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="business-analytics-table">
                <thead><tr><th>{TEXT.businessDate}</th><th>{TEXT.metricKey}</th><th>{TEXT.domain}</th><th>{TEXT.actualValue}</th><th>{TEXT.baselineValue}</th><th>{TEXT.varianceRate}</th><th>{TEXT.trend}</th><th>{TEXT.segment}</th></tr></thead>
                <tbody>{filteredSnapshots.map((row) => <tr key={row.snapshotId ?? `${row.metricKey}-${row.businessDate}`}><td>{row.businessDate ?? "-"}</td><td>{row.metricKey ?? "-"}</td><td>{row.domain ?? "-"}</td><td>{formatNumber(row.actualValue)}</td><td>{formatNumber(row.baselineValue)}</td><td>{formatPercent(row.varianceRate)}</td><td>{trendLabel(row.trendDirection)}</td><td>{row.segmentType ?? "-"}:{row.segmentId ?? "-"}</td></tr>)}</tbody>
              </table>
            </div>

            <h3>{TEXT.definition}</h3>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="business-analytics-definitions-table">
                <thead><tr><th>{TEXT.metricKey}</th><th>{TEXT.definition}</th><th>{TEXT.domain}</th><th>{TEXT.owner}</th><th>Unit</th><th>状态</th></tr></thead>
                <tbody>{filteredDefinitions.map((row) => <tr key={row.metricKey}><td>{row.metricKey}</td><td>{row.name ?? "-"}</td><td>{row.domain ?? "-"}</td><td>{row.ownerRole ?? "-"}</td><td>{row.unit ?? "-"}</td><td>{row.status ?? "-"}</td></tr>)}</tbody>
              </table>
            </div>

            <h3>{TEXT.anomalies}</h3>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="business-analytics-anomalies-table">
                <thead><tr><th>{TEXT.metricKey}</th><th>{TEXT.domain}</th><th>{TEXT.riskLevel}</th><th>{TEXT.observed}</th><th>{TEXT.expected}</th><th>{TEXT.summary}</th><th>状态</th></tr></thead>
                <tbody>{filteredAnomalies.map((row) => <tr key={row.id}><td>{row.metricKey ?? "-"}</td><td>{row.domain ?? "-"}</td><td>{row.riskLevel ?? "-"}</td><td>{formatNumber(row.observedValue)}</td><td>{formatNumber(row.expectedValue)}</td><td>{row.summary ?? "-"}</td><td>{row.status ?? "-"}</td></tr>)}</tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
