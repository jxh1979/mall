import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps } from "../types";

type ComparisonRow = {
  metricKey: string;
  label?: string;
  domain?: string;
  unit?: string;
  aggregation?: string;
  current?: number | string | null;
  prevMonth?: number | string | null;
  momPct?: number | string | null;
  yoySameMonth?: number | string | null;
  yoyPct?: number | string | null;
};
type ComparisonResponse = { month?: string; rows?: ComparisonRow[] };

const TEXT = {
  title: "月度对比",
  description: "本月与上月(环比)、去年同月(同比)怎么变——收入等流量数=月累计,欠费/出租率等存量数=月末时点;快照逐日积累,历史月份数据攒够后环比同比自然可比,缺的月份标「积累中」不硬凑。",
  loadFailed: "月度对比读取失败",
  exportDenied: "导出经营报表需要独立的导出权限(与查看分权),请走临时授权申请",
};

const AGG_LABELS: Record<string, string> = { monthly_sum: "月累计", period_end: "期末时点" };

function fmt(value: number | string | null | undefined, unit?: string) {
  if (value === null || value === undefined) return "积累中";
  const num = Number(value);
  if (!Number.isFinite(num)) return "积累中";
  switch (unit) {
    case "cent":
      return `¥${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(num / 100)}`;
    case "rate":
      return `${(num * 100).toFixed(2)}%`;
    case "sqm":
      return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(num)} ㎡`;
    case "kwh":
      return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(num)} kWh`;
    case "ton":
      return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(num)} 吨`;
    case "m3":
      return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(num)} m³`;
    default:
      return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(num);
  }
}

function pct(value: number | string | null | undefined) {
  if (value === null || value === undefined) return "积累中";
  const num = Number(value);
  if (!Number.isFinite(num)) return "积累中";
  const sign = num > 0 ? "+" : "";
  return `${sign}${(num * 100).toFixed(2)}%`;
}

function defaultMonth() {
  return new Date().toISOString().slice(0, 7);
}

export function MonthlyComparisonPage({ api, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [month, setMonth] = React.useState(defaultMonth);
  const [data, setData] = React.useState<ComparisonResponse | null>(null);
  const [exporting, setExporting] = React.useState(false);
  const canExport = hasPermissions(effectivePermissions, ["analytics.report.export"]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const response = await api.get<ComparisonResponse>(`/api/v1/analytics/monthly-comparison?month=${month}`);
      setData(response);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const isFirstMonthChange = React.useRef(true);
  React.useEffect(() => {
    if (isFirstMonthChange.current) {
      isFirstMonthChange.current = false;
      return;
    }
    void reload();
  }, [month, reload]);

  const rows = data?.rows ?? [];

  const exportCsv = async () => {
    setExporting(true);
    try {
      const report = await api.get<ComparisonResponse>(`/api/v1/analytics/reports/monthly-export?month=${month}`);
      const header = "指标,口径,本月,上月,环比,去年同月,同比";
      const lines = (report.rows ?? []).map((row) => [
        row.label ?? row.metricKey,
        AGG_LABELS[row.aggregation ?? ""] ?? row.aggregation ?? "",
        fmt(row.current, row.unit),
        fmt(row.prevMonth, row.unit),
        pct(row.momPct),
        fmt(row.yoySameMonth, row.unit),
        pct(row.yoyPct),
      ].map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(","));
      const blob = new Blob(["﻿" + [header, ...lines].join("\n")], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `月度经营对比-${report.month ?? month}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "导出失败");
    } finally {
      setExporting(false);
    }
  };

  return (
    <section data-testid="monthly-comparison-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.monthly_comparison.viewed / analytics.monthly_report.exported" />
      <div className="admin-toolbar">
        <label>
          月份:
          <input
            data-testid="monthly-comparison-month"
            type="month"
            value={month}
            onChange={(event) => setMonth(event.target.value || defaultMonth())}
          />
        </label>
        <button
          type="button"
          data-testid="monthly-comparison-export"
          disabled={!canExport || exporting}
          title={canExport ? "导出当前月份对比报表 CSV" : TEXT.exportDenied}
          onClick={exportCsv}
        >
          {exporting ? "导出中…" : "导出 CSV"}
        </button>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="monthly-comparison-table">
            <thead><tr><th>指标</th><th>口径</th><th>本月</th><th>上月</th><th>环比</th><th>去年同月</th><th>同比</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.metricKey}>
                  <td>{row.label ?? row.metricKey}</td>
                  <td>{AGG_LABELS[row.aggregation ?? ""] ?? row.aggregation ?? "—"}</td>
                  <td>{fmt(row.current, row.unit)}</td>
                  <td>{fmt(row.prevMonth, row.unit)}</td>
                  <td>{pct(row.momPct)}</td>
                  <td>{fmt(row.yoySameMonth, row.unit)}</td>
                  <td>{pct(row.yoyPct)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
