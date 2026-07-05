import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
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
type HolidayPeriod = { holidayName: string; holidayYear: number; startDate?: string; endDate?: string; source?: string };
type HolidayRow = {
  metricKey: string; label?: string; unit?: string; aggregation?: string;
  currentTotal?: number | string | null; currentDailyAvg?: number | string | null;
  previousTotal?: number | string | null; previousDailyAvg?: number | string | null;
  yoyPct?: number | string | null;
};
type HolidayComparison = {
  holidayName?: string; currentYear?: number; currentStart?: string; currentEnd?: string;
  previousYear?: number | null; previousStart?: string | null; previousEnd?: string | null;
  rows?: HolidayRow[];
};

const TEXT = {
  title: "月度对比",
  description: "本月与上月(环比)、去年同月(同比)怎么变——收入等流量数=月累计,欠费/出租率等存量数=月末时点;快照逐日积累,历史月份数据攒够后环比同比自然可比,缺的月份标「积累中」不硬凑。",
  loadFailed: "月度对比读取失败",
  exportDenied: "导出经营报表需要独立的导出权限(与查看分权),请走临时授权申请",
};

const AGG_LABELS: Record<string, string> = { monthly_sum: "月累计", period_end: "期末时点" };

// 数据孤岛消除:月度对比每个指标按业务域跳到承载该数据的业务页(落到源页即可溯源)。
const DOMAIN_PAGE: Record<string, string> = {
  finance: "ADM-FIN-001",
  ticketing: "ADM-TKT-003",
  bar: "ADM-BAR-002",
  customer: "ADM-CUS-001",
  amusement: "ADM-AMU-001",
  energy: "ADM-ENE-001",
  leasing: "ADM-LEASE-002",
  facility: "ADM-FAC-001",
};

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

export function MonthlyComparisonPage({ api, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [month, setMonth] = React.useState(defaultMonth);
  const [data, setData] = React.useState<ComparisonResponse | null>(null);
  const [exporting, setExporting] = React.useState(false);
  const [holidays, setHolidays] = React.useState<HolidayPeriod[]>([]);
  const [selectedHoliday, setSelectedHoliday] = React.useState("");
  const [holidayData, setHolidayData] = React.useState<HolidayComparison | null>(null);
  const [holidayError, setHolidayError] = React.useState<string | null>(null);
  const canExport = hasPermissions(effectivePermissions, ["analytics.report.export"]);
  // 行明细抽屉：整行可点 → 该指标对比完整字段（口径中文·金额/率格式化）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openComparisonDetail(row: ComparisonRow) {
    setDetail({
      title: `月度对比明细 · ${row.label ?? row.metricKey}`,
      fields: [
        { label: "指标", value: row.label ?? row.metricKey },
        { label: "口径", value: AGG_LABELS[row.aggregation ?? ""] ?? row.aggregation ?? "—" },
        { label: "本月", value: fmt(row.current, row.unit) },
        { label: "上月", value: fmt(row.prevMonth, row.unit) },
        { label: "环比", value: pct(row.momPct) },
        { label: "去年同月", value: fmt(row.yoySameMonth, row.unit) },
        { label: "同比", value: pct(row.yoyPct) },
      ],
    });
  }

  function openHolidayDetail(row: HolidayRow) {
    setDetail({
      title: `节假日对比明细 · ${row.label ?? row.metricKey}`,
      fields: [
        { label: "指标", value: row.label ?? row.metricKey },
        { label: "口径", value: row.aggregation === "monthly_sum" ? "区间累计" : "期末时点" },
        { label: "今年合计", value: fmt(row.currentTotal, row.unit) },
        { label: "今年日均", value: row.aggregation === "monthly_sum" ? fmt(row.currentDailyAvg, row.unit) : "—" },
        { label: "去年合计", value: fmt(row.previousTotal, row.unit) },
        { label: "去年日均", value: row.aggregation === "monthly_sum" ? fmt(row.previousDailyAvg, row.unit) : "—" },
        { label: "同比", value: pct(row.yoyPct) },
      ],
    });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const response = await api.get<ComparisonResponse>(`/api/v1/analytics/monthly-comparison?month=${month}`);
      setData(response);
      const holidayRows = await api.get<HolidayPeriod[]>("/api/v1/analytics/holiday-calendar");
      setHolidays(holidayRows ?? []);
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

  const loadHolidayComparison = async (value: string) => {
    setSelectedHoliday(value);
    setHolidayError(null);
    setHolidayData(null);
    if (!value) return;
    const [name = "", year = ""] = value.split("|");
    if (!name || !year) return;
    try {
      const result = await api.get<HolidayComparison>(
        `/api/v1/analytics/holiday-comparison?holiday=${encodeURIComponent(name)}&year=${year}`);
      setHolidayData(result);
    } catch (e) {
      setHolidayError(e instanceof Error ? e.message : "节假日对比读取失败");
    }
  };

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
                <tr key={row.metricKey} className="admin-row-clickable" tabIndex={0} data-testid={`monthly-comparison-row-${row.metricKey}`}
                  onClick={() => openComparisonDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openComparisonDetail(row); } }}>
                  <td onClick={(event) => { if (DOMAIN_PAGE[row.domain ?? ""]) event.stopPropagation(); }}>{DOMAIN_PAGE[row.domain ?? ""] ? <button type="button" className="admin-trace-link" data-testid={`monthly-comparison-metric-${row.metricKey}`} onClick={() => navigateTo?.(DOMAIN_PAGE[row.domain ?? ""]!)}>{row.label ?? row.metricKey}</button> : (row.label ?? row.metricKey)}</td>
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
      <h3>节假日对比(今年 vs 去年同名假期;流量看总额与日均,天数不同看日均才公平)</h3>
      <div className="admin-toolbar" data-testid="holiday-comparison-controls">
        <label>
          节假日:
          <select data-testid="holiday-comparison-select" value={selectedHoliday}
                  onChange={(event) => void loadHolidayComparison(event.target.value)}>
            <option value="">请选择</option>
            {holidays.map((h) => (
              <option key={`${h.holidayName}-${h.holidayYear}`} value={`${h.holidayName}|${h.holidayYear}`}>
                {h.holidayYear} {h.holidayName}
              </option>
            ))}
          </select>
        </label>
        {holidayData ? (
          <span className="admin-muted-action" data-testid="holiday-comparison-range">
            本期 {holidayData.currentStart} ~ {holidayData.currentEnd}
            {holidayData.previousStart ? `;上年同名 ${holidayData.previousStart} ~ ${holidayData.previousEnd}` : ";上年无同名假期(积累中)"}
          </span>
        ) : null}
      </div>
      {holidayError ? <div className="admin-state admin-error" data-testid="holiday-comparison-error">{holidayError}</div> : null}
      {holidayData ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="holiday-comparison-table">
            <thead><tr><th>指标</th><th>口径</th><th>今年合计</th><th>今年日均</th><th>去年合计</th><th>去年日均</th><th>同比</th></tr></thead>
            <tbody>
              {(holidayData.rows ?? []).map((row) => (
                <tr key={row.metricKey} className="admin-row-clickable" tabIndex={0} data-testid={`holiday-comparison-row-${row.metricKey}`}
                  onClick={() => openHolidayDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openHolidayDetail(row); } }}>
                  <td>{row.label ?? row.metricKey}</td>
                  <td>{row.aggregation === "monthly_sum" ? "区间累计" : "期末时点"}</td>
                  <td>{fmt(row.currentTotal, row.unit)}</td>
                  <td>{row.aggregation === "monthly_sum" ? fmt(row.currentDailyAvg, row.unit) : "—"}</td>
                  <td>{fmt(row.previousTotal, row.unit)}</td>
                  <td>{row.aggregation === "monthly_sum" ? fmt(row.previousDailyAvg, row.unit) : "—"}</td>
                  <td>{pct(row.yoyPct)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
