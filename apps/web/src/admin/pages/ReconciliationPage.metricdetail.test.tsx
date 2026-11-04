import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

// 本页 KPI 聚焦状态写在 URL ?kpi= 上，jsdom URL 跨用例保留 → 每例重置，避免上一例的聚焦态泄漏。
beforeEach(() => {
  window.history.replaceState({}, "", "/");
});

import { ReconciliationPage } from "./ReconciliationPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DIFFERENCES = [
  { id: "diff-1", differenceNo: "D-1", amount: 300, status: "open", providerCode: "wechat" },
  { id: "diff-2", differenceNo: "D-2", amount: -150, status: "closed", providerCode: "alipay" },
];

// 统一端点比本地列表多一笔（历史差异不在当前列表接口里）：接线后数字以统一端点为准。
const DETAIL = { metricKey: "finance.recon_diff_count", valueType: "additive", summaryValue: 3, rows: [{ id: "diff-1" }, { id: "diff-2" }, { id: "diff-3" }], totalRows: 3, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        return DETAIL;
      }
      if (url.includes("/reconciliation/differences")) return DIFFERENCES;
      if (url.includes("/reconciliation/batches")) return [{ id: "batch-1", batchNo: "B-1", amount: 1000, status: "open", providerCode: "wechat" }];
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 20 }),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
  };
  render(<ReconciliationPage {...props} />);
  return navigateTo;
}

describe("ReconciliationPage 首批接线：对账差异笔数改走统一明细端点", () => {
  it("差异笔数=统一端点合计=明细行数（数字=明细合计一致性）", async () => {
    const api = makeApi();
    renderPage(api);

    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences").textContent).toBe(String(DETAIL.rows.length)));
    expect(DETAIL.rows.length).toBe(DETAIL.summaryValue);
  });

  it("差异聚焦视图内提供「完整明细」出口 → 统一明细页（保留本页聚焦交互）", async () => {
    const navigateTo = renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-metric-detail-differences"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "finance.recon_diff_count||");
  });

  it("统一端点失败（弱权限财务账号）→ 回退本地列表计数并隐藏出口", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences").textContent).toBe(String(DIFFERENCES.length)));
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("reconciliation-metric-detail-differences")).toBeNull();
  });
});
