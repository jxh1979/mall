import type { MetricDefinition, RobotAnalyticsSummary, RobotMetric } from "./types";

export function formatMoney(amount: number | null | undefined) {
  return `¥${((amount ?? 0) / 100).toFixed(2)}`;
}

export function formatRate(value: number | null | undefined) {
  return `${((value ?? 0) / 100).toFixed(2)}%`;
}

export function formatPercentPoint(value: number | null | undefined) {
  return `${(value ?? 0).toFixed(2)}%`;
}

export function formatMetricValue(metricKey: string | null | undefined, value: number | null | undefined) {
  if ((metricKey ?? "").includes("amount_cents")) return formatMoney(value);
  return (value ?? 0).toLocaleString("zh-CN", { maximumFractionDigits: 4 });
}
export function averageNumber(values: Array<number | null | undefined>) {
  const validValues = values.filter((value): value is number => typeof value === "number" && Number.isFinite(value));
  if (validValues.length === 0) return 0;
  return validValues.reduce((sum, value) => sum + value, 0) / validValues.length;
}

export function formatRatio(numerator: number, denominator: number) {
  if (denominator <= 0) return "0.00%";
  return `${((numerator / denominator) * 100).toFixed(2)}%`;
}

export function formatConfidence(value: number | null | undefined) {
  return `${((value ?? 0) * 100).toFixed(0)}%`;
}

export function latestBusinessDate(rows: Array<{ businessDate?: string }>) {
  return rows.map((row) => row.businessDate).filter(Boolean).sort().at(-1) ?? "-";
}

export function summarizeRobotMetrics(metrics: RobotMetric[]): RobotAnalyticsSummary {
  const taskCount = metrics.reduce((sum, metric) => sum + metric.taskCount, 0);
  const successCount = metrics.reduce((sum, metric) => sum + metric.successCount, 0);
  const failureCount = metrics.reduce((sum, metric) => sum + metric.failureCount, 0);
  return {
    taskCount,
    successCount,
    failureCount,
    completionRate: taskCount > 0 ? successCount / taskCount : 0,
    abnormalRate: taskCount > 0 ? failureCount / taskCount : 0,
  };
}

export function metricDisplayName(metricKey: string, definitions: MetricDefinition[]) {
  return definitions.find((item) => item.metricKey === metricKey)?.name ?? metricKey;
}


export function toCents(value: number | null | undefined) {
  return Math.round((value ?? 0) * 100);
}

export function formatTime(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

export function statusColor(status: string) {
  if (["active", "success", "paid", "scheduled", "reserved", "normal"].includes(status)) return "green";
  if (["open", "billing", "pending"].includes(status)) return "blue";
  if (["manual_allowed", "idle", "cleaning"].includes(status)) return "gold";
  if (["failed", "abnormal", "locked", "high"].includes(status)) return "red";
  return "default";
}
