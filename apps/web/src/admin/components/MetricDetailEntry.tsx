import React from "react";
import type { AdminApiClient, MetricDetailPayload } from "../types";

/** 统一明细页（指标登记簿，2026-07-18 方案A）页面 id。 */
export const METRIC_DETAIL_PAGE_ID = "ADM-ANA-012";

/** 组合统一明细页 focus 参数：metric|from|to（入口带同区间 → 数字与明细同区间同源，链接可直接发同事）。 */
export function metricDetailFocus(metric: string, from?: string | null, to?: string | null): string {
  return [metric, from ?? "", to ?? ""].join("|");
}

/** 解析 focus 参数（统一明细页用）：兼容纯 metric 与 metric|from|to。 */
export function parseMetricDetailFocus(focus?: string): { metric: string; from: string | null; to: string | null } | null {
  if (!focus) return null;
  const [metric, from, to] = focus.split("|");
  if (!metric) return null;
  return { metric, from: from || null, to: to || null };
}

/**
 * KPI 卡「完整明细」入口（保留原数字点击=本页聚焦/跳源页交互不动，只加出口）：
 * 跳统一明细页并携带与数字相同的区间，明细页顶部合计=本卡数字（同一端点同源）。
 */
export function MetricDetailLink({ navigateTo, metric, from, to, testid }: {
  navigateTo?: (pageId: string, objectId?: string) => void;
  metric: string;
  from?: string | null;
  to?: string | null;
  testid?: string;
}) {
  if (!navigateTo) return null;
  return (
    <button
      type="button"
      className="admin-trace-link"
      data-testid={testid ?? `metric-detail-link-${metric}`}
      title="查看该数字的完整明细（统一明细页，可核可分享）"
      onClick={() => navigateTo(METRIC_DETAIL_PAGE_ID, metricDetailFocus(metric, from, to))}
    >
      完整明细
    </button>
  );
}

export type MetricKpiState = {
  /** ready=统一端点取数成功；fallback=失败（含无权限），页面回退既有取值，体验不降级。 */
  status: "loading" | "ready" | "fallback";
  value: number | null;
};

/**
 * KPI 数字改走统一明细端点（与明细同源，数字=明细合计按构造成立）。
 * 失败（403/网络等）→ status=fallback，调用页回退既有快照/本地值并隐藏「完整明细」入口，
 * 弱权限账号保持既有体验（fail-safe 不 fail-broken）。
 */
export function useMetricKpiValue(
  api: AdminApiClient,
  metric: string,
  opts: { from?: string | null; to?: string | null; enabled?: boolean },
): MetricKpiState {
  const [state, setState] = React.useState<MetricKpiState>({ status: "loading", value: null });
  const { from, to } = opts;
  const enabled = opts.enabled !== false;
  React.useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    const params = new URLSearchParams({ metric, page: "1", size: "1" });
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    api
      .get<MetricDetailPayload>(`/api/v1/analytics/metric-detail?${params.toString()}`)
      .then((payload) => {
        if (cancelled) return;
        const raw = payload?.summaryValue;
        setState({ status: "ready", value: raw === null || raw === undefined ? null : Number(raw) });
      })
      .catch(() => {
        if (!cancelled) setState({ status: "fallback", value: null });
      });
    return () => {
      cancelled = true;
    };
  }, [api, metric, from, to, enabled]);
  return state;
}
