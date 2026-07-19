import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { CnDatePicker } from "../components/CnDatePicker";
import { TablePager } from "../components/TablePager";
import { TraceLink } from "../components/TraceLink";
import { formatApiErrorDetail } from "../../shared/apiError";
import { metricDetailFocus, parseMetricDetailFocus } from "../components/MetricDetailEntry";
import { formatBusinessStatus } from "../i18n/statusLabels";
import type { AdminPageProps, MetricDetailPayload } from "../types";

// 登记指标目录（与后端 MetricRegistry 一致，中文名/口径以接口返回为权威；此处仅供直达选择与面包屑回源）。
const CATALOG: Array<{ key: string; name: string; sourcePageId: string; sourceLabel: string }> = [
  { key: "revenue.ticket", name: "票务收入", sourcePageId: "ADM-OV-001", sourceLabel: "总控驾驶舱" },
  { key: "finance.ar_balance", name: "应收欠费余额", sourcePageId: "ADM-OV-001", sourceLabel: "总控驾驶舱" },
  { key: "finance.recon_diff_count", name: "对账差异笔数", sourcePageId: "ADM-FIN-002", sourceLabel: "对账差异" },
  { key: "energy.total_electricity", name: "用电量", sourcePageId: "ADM-ANA-007", sourceLabel: "能耗分析" },
  { key: "energy.electricity_per_sqm", name: "每平米电耗", sourcePageId: "ADM-ANA-007", sourceLabel: "能耗分析" },
  // 第二批（酒吧域）
  { key: "revenue.bar", name: "酒吧收入", sourcePageId: "ADM-OV-001", sourceLabel: "总控驾驶舱" },
  { key: "bar.order_count", name: "酒吧订单数", sourcePageId: "ADM-BAR-002", sourceLabel: "订单/出品/账单" },
  { key: "bar.order_amount", name: "酒吧订单金额", sourcePageId: "ADM-BAR-002", sourceLabel: "订单/出品/账单" },
  { key: "bar.open_order_count", name: "未完结酒吧订单", sourcePageId: "ADM-BAR-002", sourceLabel: "订单/出品/账单" },
  { key: "bar.saved_bottle_count", name: "存酒登记总数", sourcePageId: "ADM-BAR-003", sourceLabel: "存酒" },
  { key: "bar.saved_bottle_stored", name: "在存存酒", sourcePageId: "ADM-BAR-003", sourceLabel: "存酒" },
  { key: "bar.saved_bottle_quantity", name: "存酒剩余数量", sourcePageId: "ADM-BAR-003", sourceLabel: "存酒" },
  // 第三批（票务域）
  { key: "ticket.order_count", name: "售票订单数", sourcePageId: "ADM-TKT-003", sourceLabel: "出票与核销追踪" },
  { key: "ticket.paid_order_count", name: "已支付订单数", sourcePageId: "ADM-TKT-003", sourceLabel: "出票与核销追踪" },
  { key: "ticket.pending_issue_count", name: "待出票订单数", sourcePageId: "ADM-TKT-003", sourceLabel: "出票与核销追踪" },
  { key: "amusement.session_count", name: "游乐场次数", sourcePageId: "ADM-AMU-001", sourceLabel: "项目与场次" },
  { key: "amusement.session_capacity", name: "游乐场次总容量", sourcePageId: "ADM-AMU-001", sourceLabel: "项目与场次" },
  { key: "amusement.group_booking_count", name: "团体预约数", sourcePageId: "ADM-AMU-002", sourceLabel: "团体/包场" },
  { key: "amusement.group_reserved_capacity", name: "团体占用容量", sourcePageId: "ADM-AMU-002", sourceLabel: "团体/包场" },
  { key: "amusement.entry_count", name: "游乐入场核销数", sourcePageId: "ADM-AMU-005", sourceLabel: "游乐核销记录" },
  { key: "amusement.exit_count", name: "游乐出场核销数", sourcePageId: "ADM-AMU-005", sourceLabel: "游乐核销记录" },
  { key: "amusement.inspection_count", name: "安全巡检数", sourcePageId: "ADM-AMU-003", sourceLabel: "安全巡检/事故" },
  { key: "amusement.inspection_pending_rectification", name: "待整改巡检数", sourcePageId: "ADM-AMU-003", sourceLabel: "安全巡检/事故" },
];

const VALUE_TYPE_LABELS: Record<string, string> = { additive: "可加（区间求和）", ratio: "比率（分子/分母）", balance: "余额（区间末值）" };
const PAGE_SIZE = 20;

function formatNumber(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return "—";
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return "—";
  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 4 }).format(parsed);
}

function formatCell(value: unknown): string {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "number") return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 4 }).format(value);
  if (typeof value === "string") {
    // ISO 时间 → 中文本地时间；纯枚举词 → 业务中文（未知码原样，与各域列表页同一字典）。
    if (/^\d{4}-\d{2}-\d{2}T/.test(value)) return new Date(value).toLocaleString("zh-CN", { hour12: false });
    if (/^[a-z][a-z_]*$/.test(value)) return formatBusinessStatus(value);
    return value;
  }
  return String(value);
}

/**
 * KPI 统一明细页（指标登记簿，2026-07-18 拍板方案A）：各页 KPI 数字的「完整明细」出口落点。
 * 表头口径说明 + 日期筛选 + 分页表格 + 行级 TraceLink 下钻 + 面包屑回源页；
 * 比率类分「分子明细/分母明细」两页签。URL ?focus=metric|from|to 全量承载状态（链接可发同事）。
 * CSV 导出（查看/导出分权范式）列为后续项，本期只做查看。
 */
export function MetricDetailPage({ api, setGlobalError, navigateTo, focusObjectId }: AdminPageProps) {
  const initial = parseMetricDetailFocus(focusObjectId);
  const [metric, setMetric] = React.useState(initial?.metric ?? "");
  const [dateFrom, setDateFrom] = React.useState(initial?.from ?? "");
  const [dateTo, setDateTo] = React.useState(initial?.to ?? "");
  const [applied, setApplied] = React.useState({ from: initial?.from ?? "", to: initial?.to ?? "" });
  const [part, setPart] = React.useState<"numerator" | "denominator">("numerator");
  const [page, setPage] = React.useState(1);
  const [data, setData] = React.useState<MetricDetailPayload | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<{ message: string; detail: string | null } | null>(null);

  const load = React.useCallback(async () => {
    if (!metric) return;
    setLoading(true);
    setGlobalError(null);
    try {
      const params = new URLSearchParams({ metric, page: String(page), size: String(PAGE_SIZE) });
      if (applied.from) params.set("from", applied.from);
      if (applied.to) params.set("to", applied.to);
      if (part === "denominator") params.set("part", "denominator");
      const payload = await api.get<MetricDetailPayload>(`/api/v1/analytics/metric-detail?${params.toString()}`);
      setData(payload);
      setError(null);
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : "指标明细读取失败";
      setError({ message, detail: formatApiErrorDetail(loadError) });
      setGlobalError(message);
    } finally {
      setLoading(false);
    }
  }, [api, metric, page, applied, part, setGlobalError]);

  React.useEffect(() => {
    void load();
  }, [load]);

  // 已在本页时再次从其它页入口进入（focus 变化）：按新 metric|from|to 重置视图。
  React.useEffect(() => {
    const next = parseMetricDetailFocus(focusObjectId);
    if (!next) return;
    setMetric(next.metric);
    setDateFrom(next.from ?? "");
    setDateTo(next.to ?? "");
    setApplied({ from: next.from ?? "", to: next.to ?? "" });
    setPart("numerator");
    setPage(1);
  }, [focusObjectId]);

  function selectMetric(nextMetric: string) {
    setMetric(nextMetric);
    setPart("numerator");
    setPage(1);
    setData(null);
    syncUrl(nextMetric, applied.from, applied.to);
  }

  function applyRange() {
    setApplied({ from: dateFrom, to: dateTo });
    setPage(1);
    syncUrl(metric, dateFrom, dateTo);
  }

  // 状态全量进 URL（pushState 只改查询串，保留子路径前缀链路）：明细链接可直接发同事复核。
  function syncUrl(nextMetric: string, from: string, to: string) {
    if (typeof window === "undefined" || !nextMetric) return;
    const params = new URLSearchParams(window.location.search);
    params.set("focus", metricDetailFocus(nextMetric, from || null, to || null));
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
  }

  const catalogEntry = CATALOG.find((item) => item.key === metric);
  const isRatio = data?.valueType === "ratio";
  const pageCount = data ? Math.max(1, Math.ceil(data.totalRows / PAGE_SIZE)) : 1;
  const columns = data?.columns ?? [];

  return (
    <section data-testid="metric-detail-page">
      <AdminPageHeader
        title="指标明细"
        description="KPI 数字的统一完整明细：口径一处权威登记，数字=明细合计同源可核，链接可直接分享。"
      />
      <div className="admin-form-row" data-testid="metric-detail-breadcrumb">
        {catalogEntry && navigateTo ? (
          <button type="button" className="admin-trace-link" data-testid="metric-detail-back" onClick={() => navigateTo(catalogEntry.sourcePageId)}>
            ← 返回来源页：{catalogEntry.sourceLabel}
          </button>
        ) : null}
        <label>
          指标
          <select data-testid="metric-detail-metric-select" value={metric} onChange={(event) => selectMetric(event.target.value)}>
            <option value="">请选择登记指标</option>
            {CATALOG.map((item) => (
              <option key={item.key} value={item.key}>{item.name}（{item.key}）</option>
            ))}
          </select>
        </label>
        <label>起始<CnDatePicker data-testid="metric-detail-from" value={dateFrom} onChange={setDateFrom} /></label>
        <label>结束<CnDatePicker data-testid="metric-detail-to" value={dateTo} onChange={setDateTo} /></label>
        <button type="button" data-testid="metric-detail-apply" onClick={applyRange} disabled={!metric}>筛选时间</button>
      </div>
      {!metric ? (
        <div className="admin-state" data-testid="metric-detail-empty">请选择要核对的登记指标（或从各页 KPI 卡的「完整明细」入口进入）。</div>
      ) : (
        <AdminDataState loading={loading && !data} error={error} empty={!loading && !data} onRetry={load}>
          {data ? (
            <>
              <div className="admin-kpi-grid" data-testid="metric-detail-summary-panel">
                <div className="admin-kpi-card">
                  <span>{data.name}（{VALUE_TYPE_LABELS[data.valueType] ?? data.valueType}）</span>
                  <strong data-testid="metric-detail-summary">{formatNumber(data.summaryValue)} {data.unit}</strong>
                  <small>合计校验值：与明细行同一查询同源，逐页累加应恒等于此数</small>
                </div>
                {isRatio ? (
                  <div className="admin-kpi-card">
                    <span>分子 / 分母</span>
                    <strong data-testid="metric-detail-ratio-parts">{formatNumber(data.numeratorValue)} / {formatNumber(data.denominatorValue)}</strong>
                    <small>分子=区间合计；分母=当前时点值（历史区间查询时为当前值）</small>
                  </div>
                ) : null}
              </div>
              <div className="admin-state" data-testid="metric-detail-caliber">
                <strong>口径说明：</strong>{data.caliber}
                <br />
                <strong>数据源：</strong>{data.source}
                <br />
                <strong>数据截至：</strong>{new Date(data.dataAsOf).toLocaleString("zh-CN", { hour12: false })}
                {data.from || data.to ? <>（区间：{data.from ?? "不限"} ~ {data.to ?? "不限"}）</> : "（区间：全部历史）"}
                {data.summaryNote ? <><br /><strong>说明：</strong><span data-testid="metric-detail-summary-note">{data.summaryNote}</span></> : null}
              </div>
              {isRatio ? (
                <div className="admin-form-row" data-testid="metric-detail-part-tabs">
                  <button type="button" data-testid="metric-detail-part-numerator" disabled={part === "numerator"}
                    onClick={() => { setPart("numerator"); setPage(1); }}>分子明细</button>
                  <button type="button" data-testid="metric-detail-part-denominator" disabled={part === "denominator"}
                    onClick={() => { setPart("denominator"); setPage(1); }}>分母明细</button>
                </div>
              ) : null}
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="metric-detail-table">
                  <thead>
                    <tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr>
                  </thead>
                  <tbody>
                    {data.rows.map((row, index) => {
                      const traceId = data.traceLinkColumnKey ? String(row[data.traceLinkColumnKey] ?? "") : "";
                      return (
                        <tr key={traceId || index} data-testid={`metric-detail-row-${traceId || index}`}>
                          {columns.map((column, columnIndex) => (
                            <td key={column.key}>
                              {columnIndex === 0 && data.traceLinkPageId && traceId ? (
                                <TraceLink navigateTo={navigateTo} pageId={data.traceLinkPageId} objectId={traceId}>
                                  {formatCell(row[column.key])}
                                </TraceLink>
                              ) : (
                                formatCell(row[column.key])
                              )}
                            </td>
                          ))}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                {data.rows.length === 0 ? <div className="admin-state" data-testid="metric-detail-no-rows">当前区间没有明细行。</div> : null}
                <TablePager
                  testid="metric-detail-pager"
                  page={page}
                  pageCount={pageCount}
                  total={data.totalRows}
                  onPrev={() => setPage((current) => Math.max(1, current - 1))}
                  onNext={() => setPage((current) => Math.min(pageCount, current + 1))}
                />
              </div>
            </>
          ) : null}
        </AdminDataState>
      )}
    </section>
  );
}
