// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import { T3AReadonlyAnalyticsPage } from "./T3AReadonlyAnalyticsPage";
import { WecomComplianceReviewPage } from "./WecomComplianceReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BE 段第 378 条 378-R1（ENUM:217「风险四级 低（进列表）、中（待复核）、高（生成工单+通知主管）、严重（可冻结/停用/强制审批）」；
// PGM:287「界面全中文（所有终端零英文）」）：企微合规复核、经营分析、只读经营分析三页的风险等级按四级名显示。
// 三页的风险等级取值都是 normal、suspicious、high、critical（V017、V106 的 CHECK）。

const PERMISSIONS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const USER = { id: "u1", username: "合规专员", accountType: "platform_admin" };
const LEVELS = [["normal", "低"], ["suspicious", "中"], ["high", "高"], ["critical", "严重"]] as const;

function clientOf(resolve: (path: string) => unknown): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((path: string) => Promise.resolve(resolve(path))),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function propsOf(api: AdminApiClient): AdminPageProps {
  return { api, user: USER, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMISSIONS };
}

const cells = (testId: string) => Array.from(screen.getByTestId(testId).querySelectorAll("td")).map((td) => td.textContent ?? "");

describe("风险等级按四级名显示（第 378 条）", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("企微合规复核页：命中列表的风险等级显示低、中、高、严重，不显示英文码", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const data = String(input).includes("/wecom/risk-hits")
        ? { items: LEVELS.map(([code], i) => ({ hit_id: `hit-${i}`, rule_code: "ABUSE", risk_level: code, matched_snippet: "片段", status: "open" })) }
        : { items: [] };
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
        { status: 200, headers: { "content-type": "application/json" } }));
    }));
    render(<WecomComplianceReviewPage {...propsOf(new AdminApi("test-token"))} />);
    await waitFor(() => expect(screen.queryByTestId("wecom-risk-hit-row-hit-0")).not.toBeNull(), { timeout: 3000 });
    LEVELS.forEach(([code, label], i) => {
      const row = cells(`wecom-risk-hit-row-hit-${i}`);
      expect(row).toContain(label);
      expect(row).not.toContain(code);
    });
  });

  it("经营分析页：异常指标的风险等级显示四级名，不显示「可疑」「紧急」「正常」", async () => {
    const anomalies = LEVELS.map(([code], i) => ({ id: `ano-${i}`, metricKey: "revenue.bar", riskLevel: code, observedValue: 300, expectedValue: 100,
      summary: "波动", status: "open", detectedAt: "2026-07-02T00:00:00Z" }));
    const snapshots = [{ snapshotId: "s1", metricKey: "revenue.bar", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90,
      varianceRate: 0.1, trendDirection: "up", segmentType: "platform" }];
    render(<BusinessAnalyticsPage {...propsOf(clientOf((path) => path.includes("/analytics/metric-anomalies") ? anomalies
      : path.includes("/analytics/business-metric-snapshots") ? snapshots
        : path.includes("/analytics/traffic-hourly") ? { buckets: [] } : []))} />);
    await waitFor(() => expect(screen.queryByTestId("business-analytics-anomaly-row-ano-0")).not.toBeNull(), { timeout: 3000 });
    LEVELS.forEach(([, label], i) => {
      const row = cells(`business-analytics-anomaly-row-ano-${i}`);
      expect(row).toContain(label);
      for (const old of ["可疑", "紧急", "正常"]) expect(row).not.toContain(old);
    });
  });

  it("只读经营分析页：员工服务分与客户意向的风险列显示四级名", async () => {
    render(<T3AReadonlyAnalyticsPage {...propsOf(clientOf((path) => path.includes("/staff-service-score")
      ? [{ scoreId: "ss-1", staffName: "张三", roleCode: "supervisor", serviceScore: 88, complaintCount: 0, riskLevel: "normal" }]
      : path.includes("/customer-intents") ? [{ intentId: "in-1", customerRef: "C-9", intentType: "咨询入驻", riskLevel: "critical", summary: "有意向" }] : []))} />);
    await waitFor(() => expect(screen.queryByTestId("t3a-staff-row-ss-1")).not.toBeNull(), { timeout: 3000 });
    await waitFor(() => expect(screen.queryByTestId("t3a-intent-row-in-1")).not.toBeNull(), { timeout: 3000 });
    expect(cells("t3a-staff-row-ss-1")).toContain("低");
    expect(cells("t3a-staff-row-ss-1")).not.toContain("正常");
    expect(cells("t3a-intent-row-in-1")).toContain("严重");
    expect(cells("t3a-intent-row-in-1")).not.toContain("紧急");
  });
});
