import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { Sparkline, MiniBars } from "../components/MiniChart";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BusinessMetricSnapshotRow, MetricAnomalyRow, MetricDefinitionRow } from "../types";

const TEXT = {
  title: "经营分析",
  description: "把收入、票务、酒吧、游乐、商户健康和风险指标沉淀成可复盘经营视图；当前以正式指标快照、指标定义和异常接口为准，不伪造预测或生产 BI。",
  evidence: "接口证据：读取 /api/v1/analytics/business-metric-snapshots、/api/v1/analytics/traffic-hourly；指标定义/异常检测未启用（二期）。",
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
  const [domainFilter, setDomainFilter] = React.useState("all");
  const [lockNote, setLockNote] = React.useState("总控周会复盘锁定");
  const [formError, setFormError] = React.useState("");
  // T4.3 分时段客流：traffic-hourly 端点的 24 桶（hour/count）。
  const [hourly, setHourly] = React.useState<Array<{ hour: number; count: number }>>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      // S1(2026-07-02 拍板):指标定义/指标异常为 V017 演示种子、无生产管道——下架不再拉取,
      // 页面显式标「未启用」防误导决策;真实异常检测/口径治理管道排二期。
      const [snapshotRows, hourlyResp] = await Promise.all([
        api.get<BusinessMetricSnapshotRow[]>("/api/v1/analytics/business-metric-snapshots"),
        api.get<{ businessDate?: string; buckets?: Array<{ hour: number; count: number }> }>("/api/v1/analytics/traffic-hourly"),
      ]);
      setSnapshots(snapshotRows);
      setHourly(hourlyResp?.buckets ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const domains = Array.from(new Set(snapshots.map(rowDomain))).filter(Boolean).sort();
  const filteredSnapshots = domainFilter === "all" ? snapshots : snapshots.filter((row) => rowDomain(row) === domainFilter);

  const upwardCount = filteredSnapshots.filter((row) => row.trendDirection === "up").length;
  const averageVariance = filteredSnapshots.length === 0 ? 0 : filteredSnapshots.reduce((sum, row) => sum + numberValue(row.varianceRate), 0) / filteredSnapshots.length;

  // 按指标聚合时间序列：同一 metricKey 下按 businessDate 升序的 actualValue 构成 sparkline，
  // 并用「实际值 vs 基准值」迷你柱状图替代纯文字趋势（不引入图表依赖）。
  const series = React.useMemo(() => {
    const byMetric = new Map<string, BusinessMetricSnapshotRow[]>();
    for (const row of filteredSnapshots) {
      const key = row.metricKey ?? "metric";
      const list = byMetric.get(key) ?? [];
      list.push(row);
      byMetric.set(key, list);
    }
    return Array.from(byMetric.entries()).slice(0, 4).map(([metricKey, list]) => {
      const ordered = [...list].sort((a, b) => String(a.businessDate ?? "").localeCompare(String(b.businessDate ?? "")));
      return {
        metricKey,
        actual: ordered.map((row) => numberValue(row.actualValue)),
        baseline: ordered.map((row) => numberValue(row.baselineValue)),
        latest: ordered[ordered.length - 1],
      };
    });
  }, [filteredSnapshots]);

  // T4.2/T4.3 三图派生：取最新经营日的平台口径指标值（不受业务域筛选影响，恒为管理层视图）。
  function latestPlatformMetric(metricKey: string): number | null {
    const rows = snapshots.filter((row) => row.metricKey === metricKey && (!row.segmentType || row.segmentType === "platform"));
    if (rows.length === 0) return null;
    const latest = [...rows].sort((a, b) => String(b.businessDate ?? "").localeCompare(String(a.businessDate ?? "")))[0];
    return latest?.actualValue == null ? null : numberValue(latest.actualValue);
  }
  const perSqmCent = latestPlatformMetric("revenue.per_sqm"); // 坪效（分/㎡）
  const barRevenue = latestPlatformMetric("revenue.bar") ?? 0;
  const ticketRevenue = latestPlatformMetric("revenue.ticket") ?? 0;
  const categoryTotal = barRevenue + ticketRevenue;
  const barShare = categoryTotal > 0 ? (barRevenue / categoryTotal) * 100 : 0;
  const ticketShare = categoryTotal > 0 ? (ticketRevenue / categoryTotal) * 100 : 0;
  const peakBucket = hourly.reduce<{ hour: number; count: number } | null>(
    (best, b) => (best == null || numberValue(b.count) > numberValue(best.count) ? b : best),
    null,
  );

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
      anomalyCount: null,
      evidenceAttachmentIds: ["att-business-analytics-lock"],
    });
  }

  return (
    <section data-testid="business-analytics-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="business-analytics-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="analytics.business_metric_snapshot.viewed / local.management_review_ready" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {snapshots.length === 0 ? <div className="admin-state admin-empty" data-testid="business-analytics-empty">{TEXT.empty}</div> : (
          <>
            <div className="admin-kpi-grid" data-testid="business-analytics-summary">
              <div className="admin-kpi-card"><span>{TEXT.totalMetrics}</span><strong>{filteredSnapshots.length}</strong><small>{domainFilter}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.upward}</span><strong>{upwardCount}</strong><small>上升</small></div>
              <div className="admin-kpi-card"><span>{TEXT.anomalies}</span><strong>未启用</strong><small>二期</small></div>
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

            {series.length > 0 ? (
              <div className="admin-chart-panel" data-testid="business-analytics-charts">
                {series.map((item) => (
                  <div className="admin-chart-block" key={item.metricKey} data-testid={`business-analytics-chart-${item.metricKey}`}>
                    <span>{item.metricKey}（实际值趋势 / 实际 vs 基准）</span>
                    <Sparkline values={item.actual} width={140} height={36} testid={`business-analytics-sparkline-${item.metricKey}`} ariaLabel={`${item.metricKey} 实际值趋势`} />
                    <MiniBars values={[numberValue(item.latest?.actualValue), numberValue(item.latest?.baselineValue)]} width={80} height={36} testid={`business-analytics-bars-${item.metricKey}`} ariaLabel={`${item.metricKey} 实际值与基准值`} />
                    <small className="admin-muted-action">趋势 {trendLabel(item.latest?.trendDirection)} · 实际 {formatNumber(item.latest?.actualValue)} · 基准 {formatNumber(item.latest?.baselineValue)}</small>
                  </div>
                ))}
              </div>
            ) : null}

            <div className="admin-chart-panel" data-testid="business-analytics-pingxiao">
              <div className="admin-chart-block" data-testid="business-analytics-per-sqm">
                <span>坪效（净收入 / 可经营面积）</span>
                <strong data-testid="business-analytics-per-sqm-value">
                  {perSqmCent == null ? "未配置面积" : `${formatNumber(perSqmCent / 100)} 元/㎡`}
                </strong>
                <small className="admin-muted-action">面积在「经营参数」页维护；未配置时此处显示未配置、不伪造。</small>
              </div>
              <div className="admin-chart-block" data-testid="business-analytics-category-share">
                <span>品类占比（酒吧 vs 票务实收）</span>
                <MiniBars values={[barRevenue, ticketRevenue]} width={90} height={36} testid="business-analytics-category-bars" ariaLabel="酒吧与票务收入占比" />
                <small className="admin-muted-action">
                  {categoryTotal > 0 ? `酒吧 ${barShare.toFixed(1)}% · 票务 ${ticketShare.toFixed(1)}%` : "暂无收入数据"}
                </small>
              </div>
              <div className="admin-chart-block" data-testid="business-analytics-hourly">
                <span>分时段客流（24 小时核销分布）</span>
                <MiniBars values={hourly.map((bucket) => numberValue(bucket.count))} width={240} height={36} testid="business-analytics-hourly-bars" ariaLabel="分时段客流分布" />
                <small className="admin-muted-action">
                  {peakBucket && numberValue(peakBucket.count) > 0
                    ? `高峰时段 ${String(peakBucket.hour).padStart(2, "0")}:00（${formatNumber(peakBucket.count)} 人次）`
                    : "暂无客流数据"}
                </small>
              </div>
            </div>

            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="business-analytics-table">
                <thead><tr><th>{TEXT.businessDate}</th><th>{TEXT.metricKey}</th><th>{TEXT.domain}</th><th>{TEXT.actualValue}</th><th>{TEXT.baselineValue}</th><th>{TEXT.varianceRate}</th><th>{TEXT.trend}</th><th>{TEXT.segment}</th></tr></thead>
                <tbody>{filteredSnapshots.map((row) => <tr key={row.snapshotId ?? `${row.metricKey}-${row.businessDate}`}><td>{row.businessDate ?? "-"}</td><td>{row.metricKey ?? "-"}</td><td>{row.domain ?? "-"}</td><td>{formatNumber(row.actualValue)}</td><td>{formatNumber(row.baselineValue)}</td><td>{formatPercent(row.varianceRate)}</td><td>{trendLabel(row.trendDirection)}</td><td>{row.segmentType ?? "-"}:{row.segmentId ?? "-"}</td></tr>)}</tbody>
              </table>
            </div>

            <h3>{TEXT.definition}</h3>
            <div className="admin-state admin-empty" data-testid="business-analytics-definitions-disabled">
              功能未启用:指标口径治理(定义/负责人/单位)的真实管道在二期建设中;此前显示的是建系统时的演示数据,已下架防误导。
            </div>

            <h3>{TEXT.anomalies}</h3>
            <div className="admin-state admin-empty" data-testid="business-analytics-anomalies-disabled">
              功能未启用:指标异常自动检测在二期建设中;此前显示的是演示数据,已下架防误导。
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
