import { BarChartOutlined } from "@ant-design/icons";
import { Alert, Tag } from "antd";
import { t3aReadOnlyAnalyticsMarker, t3aReadOnlyBoundaryText } from "../apiClient";
import { averageNumber, formatConfidence, formatMetricValue, formatMoney, formatPercentPoint, formatRatio, latestBusinessDate, metricDisplayName, statusColor, summarizeRobotMetrics } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

export type AnalyticsReadOnlyPageProps = {
  snapshotValue: Snapshot;
};

export function AnalyticsReadOnlyPage({ snapshotValue }: AnalyticsReadOnlyPageProps) {
  const robotSummary = summarizeRobotMetrics(snapshotValue.robotMetrics);
  const averageMerchantHealth = averageNumber(snapshotValue.merchantHealthScores.map((item) => item.score));
  const averageStaffScore = averageNumber(snapshotValue.staffServiceScores.map((item) => item.serviceScore));
  const averageAiQuality = averageNumber(snapshotValue.aiQualityResults.map((item) => item.qualityScore));
  const latestMetricDate = latestBusinessDate(snapshotValue.businessMetricSnapshots);

  return (
    <section className="section-block" data-boundary={t3aReadOnlyAnalyticsMarker}>
      {/* T3A_READONLY_ANALYTICS_VIEW_START */}
      <SectionHeading
        icon={<BarChartOutlined />}
        title="T3-A 只读经营分析"
        extra={<Tag color="blue">analytics.dashboard.view</Tag>}
      />
      <Alert className="status-alert" type="info" showIcon message={t3aReadOnlyBoundaryText} />
      <div className="summary-grid compact">
        <div className="metric"><span>经营指标</span><strong>{snapshotValue.businessMetricSnapshots.length}</strong><small>最新日期 {latestMetricDate}</small></div>
        <div className="metric"><span>商户健康均分</span><strong>{averageMerchantHealth.toFixed(0)}</strong><small>{snapshotValue.merchantHealthScores.length} 个评分</small></div>
        <div className="metric"><span>员工服务均分</span><strong>{averageStaffScore.toFixed(0)}</strong><small>{snapshotValue.staffServiceScores.length} 个样本</small></div>
        <div className="metric"><span>客户意图</span><strong>{snapshotValue.customerIntents.length}</strong><small>只读摘要</small></div>
        <div className="metric"><span>机器人任务</span><strong>{robotSummary.taskCount}</strong><small>完成率 {formatRatio(robotSummary.successCount, robotSummary.taskCount)}</small></div>
        <div className="metric"><span>AI 质检均分</span><strong>{averageAiQuality.toFixed(0)}</strong><small>{snapshotValue.aiQualityResults.length} 条结果</small></div>
      </div>
      <div className="table-grid two">
        <SimpleTable title="T3-A 经营指标快照" data={snapshotValue.businessMetricSnapshots.slice(0, 12).map((row) => ({ ...row, key: row.snapshotId }))} columns={[
          { title: "指标", key: "metric", render: (_, row) => metricDisplayName(row.metricKey, snapshotValue.metricDefinitions) },
          { title: "领域", dataIndex: "domain" },
          { title: "时间窗", dataIndex: "periodType" },
          { title: "日期", dataIndex: "businessDate" },
          { title: "实际值", key: "actual", render: (_, row) => formatMetricValue(row.metricKey, row.actualValue) },
          { title: "基准", key: "baseline", render: (_, row) => formatMetricValue(row.metricKey, row.baselineValue) },
          { title: "变化", dataIndex: "varianceRate", render: formatPercentPoint },
          { title: "来源口径", key: "method", render: (_, row) => `${row.projectionMethod} · ${formatConfidence(row.confidenceScore)}` },
        ]} />
        <SimpleTable title="T3-A 商户经营摘要" data={snapshotValue.merchants.slice(0, 12).map((row) => ({ ...row, key: row.merchantId }))} columns={[
          { title: "商户", dataIndex: "merchantName" },
          { title: "类型", dataIndex: "merchantType" },
          { title: "营收", dataIndex: "revenueAmountCents", render: formatMoney },
          { title: "订单", dataIndex: "orderCount" },
          { title: "风险", dataIndex: "riskAlertCount" },
          { title: "结算", dataIndex: "settlementStatus", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="T3-A 商户健康" data={snapshotValue.merchantHealthScores.slice(0, 12).map((row) => ({ ...row, key: row.merchantId }))} columns={[
          { title: "商户ID", dataIndex: "merchantId" },
          { title: "分数", dataIndex: "score" },
          { title: "等级", dataIndex: "scoreLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "风险数", dataIndex: "riskCount" },
          { title: "逾期应收", dataIndex: "overdueArAmountCents", render: formatMoney },
          { title: "摘要", dataIndex: "summary" },
        ]} />
        <SimpleTable title="T3-A 员工服务评分" data={snapshotValue.staffServiceScores.slice(0, 12).map((row) => ({ ...row, key: row.scoreId }))} columns={[
          { title: "员工", dataIndex: "staffName" },
          { title: "角色", dataIndex: "roleCode" },
          { title: "服务分", dataIndex: "serviceScore" },
          { title: "投诉", dataIndex: "complaintCount" },
          { title: "正向事件", dataIndex: "positiveEventCount" },
          { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="T3-A 客户意图摘要" data={snapshotValue.customerIntents.slice(0, 12).map((row) => ({ ...row, key: row.intentId }))} columns={[
          { title: "类型", dataIndex: "intentType" },
          { title: "来源", dataIndex: "sourceType" },
          { title: "置信度", dataIndex: "confidenceScore", render: formatConfidence },
          { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "摘要", dataIndex: "summary" },
        ]} />
        <SimpleTable title="T3-A 机器人经营指标" data={snapshotValue.robotMetrics.slice(0, 12).map((row) => ({ ...row, key: row.metricId }))} columns={[
          { title: "机器人", dataIndex: "robotId" },
          { title: "服务商", dataIndex: "providerCode" },
          { title: "任务", dataIndex: "taskCount" },
          { title: "成功", dataIndex: "successCount" },
          { title: "失败", dataIndex: "failureCount" },
          { title: "日期", dataIndex: "businessDate" },
          { title: "摘要", dataIndex: "summary" },
        ]} />
        <SimpleTable title="T3-A AI 质检只读结果" data={snapshotValue.aiQualityResults.slice(0, 12).map((row) => ({ ...row, key: row.resultId }))} columns={[
          { title: "来源", dataIndex: "sourceType" },
          { title: "评分", dataIndex: "qualityScore" },
          { title: "置信度", dataIndex: "confidenceScore", render: formatConfidence },
          { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "发现", dataIndex: "findingCount" },
          { title: "摘要", dataIndex: "summary" },
        ]} />
        <SimpleTable title="T3-A 指标口径" data={snapshotValue.metricDefinitions.slice(0, 12).map((row) => ({ ...row, key: row.metricKey }))} columns={[
          { title: "编码", dataIndex: "metricKey" },
          { title: "名称", dataIndex: "name" },
          { title: "领域", dataIndex: "domain" },
          { title: "单位", dataIndex: "unit" },
          { title: "口径", dataIndex: "calculationNote" },
          { title: "责任角色", dataIndex: "ownerRole" },
        ]} />
        <SimpleTable title="T3-A 指标异常" data={snapshotValue.metricAnomalies.slice(0, 12).map((row) => ({ ...row, key: row.id }))} columns={[
          { title: "指标", key: "metric", render: (_, row) => metricDisplayName(row.metricKey, snapshotValue.metricDefinitions) },
          { title: "领域", dataIndex: "domain" },
          { title: "观察值", key: "observed", render: (_, row) => formatMetricValue(row.metricKey, row.observedValue) },
          { title: "期望值", key: "expected", render: (_, row) => formatMetricValue(row.metricKey, row.expectedValue) },
          { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "摘要", dataIndex: "summary" },
        ]} />
      </div>
      {/* T3A_READONLY_ANALYTICS_VIEW_END */}
    </section>
  );
}
