import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { useMetricKpiValue } from "./MetricDetailEntry";
import type { AdminApiClient } from "../types";

// 项目作用域收紧（2026-07-19）前端断言：后端对 platform_only 指标向项目级返回 403 后，
// useMetricKpiValue fail-safe 回退（status=fallback、value=null）→ 调用页据此隐藏「完整明细」入口、
// 不显示平台全量数字（项目级看不到平台口径 KPI）；category-A 指标取数成功则显示本项目 summaryValue。

function Probe({ api, metric }: { api: AdminApiClient; metric: string }) {
  const state = useMetricKpiValue(api, metric, {});
  return <div data-testid="state">{`${state.status}:${state.value === null ? "null" : String(state.value)}`}</div>;
}

function apiResolving(payload: unknown): AdminApiClient {
  return { get: vi.fn().mockResolvedValue(payload) } as unknown as AdminApiClient;
}

function apiRejecting(err: unknown): AdminApiClient {
  return { get: vi.fn().mockRejectedValue(err) } as unknown as AdminApiClient;
}

describe("useMetricKpiValue 项目作用域 fail-safe", () => {
  it("取数成功 → ready + 本项目数值（项目级 category-A 指标显示本综合体 summaryValue）", async () => {
    render(<Probe api={apiResolving({ summaryValue: 42 })} metric="finance.ar_balance" />);
    await waitFor(() => expect(screen.getByTestId("state").textContent).toBe("ready:42"));
  });

  it("platform_only 指标项目级 403 → fallback + null（隐藏完整明细入口，不显示平台全量）", async () => {
    render(
      <Probe
        api={apiRejecting({ status: 403, detailCode: "PLATFORM_METRIC_PROJECT_FORBIDDEN" })}
        metric="revenue.ticket"
      />,
    );
    await waitFor(() => expect(screen.getByTestId("state").textContent).toBe("fallback:null"));
  });

  it("任意失败（网络/权限等）→ fallback + null（fail-safe 不 fail-broken）", async () => {
    render(<Probe api={apiRejecting(new Error("network"))} metric="revenue.bar" />);
    await waitFor(() => expect(screen.getByTestId("state").textContent).toBe("fallback:null"));
  });
});
