import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ReconciliationPage } from "./ReconciliationPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const BATCHES = [
  { id: "b-1", batchNo: "REC-2026-07-01", amount: 100000, status: "matched", providerCode: "wechatpay" },
];
const DIFFERENCES = [
  { id: "d-1", differenceNo: "DIFF-001", amount: 2500, status: "pending", providerCode: "wechatpay" },
  { id: "d-2", differenceNo: "DIFF-002", amount: 800, status: "ignored", providerCode: "alipay" },
];

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/reconciliation/batches")) return Promise.resolve(BATCHES);
    if (path.includes("/reconciliation/differences")) return Promise.resolve(DIFFERENCES);
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver()): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient = makeApi()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<ReconciliationPage {...props} />);
}

describe("ReconciliationPage KPI 聚焦视图（带参重进本页）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("点“对账差异”数字 → URL 带 ?kpi=differences，只剩差异承载表+复核表单，批次表/KPI 卡隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    expect(window.location.search).toContain("kpi=differences");
    expect(screen.getByTestId("reconciliation-kpi-focus-banner").textContent).toContain("正在查看：对账差异（2）");
    expect(screen.getByTestId("reconciliation-differences-table")).toBeTruthy();
    expect(screen.getByTestId("reconciliation-review-form")).toBeTruthy();
    expect(screen.queryByTestId("reconciliation-batches-table")).toBeNull();
    expect(screen.queryByTestId("reconciliation-summary")).toBeNull();
  });

  it("点“对账批次”数字 → 只剩批次承载表，差异表/复核表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-batches")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-batches"));
    expect(window.location.search).toContain("kpi=batches");
    expect(screen.getByTestId("reconciliation-batches-table")).toBeTruthy();
    expect(screen.queryByTestId("reconciliation-differences-table")).toBeNull();
    expect(screen.queryByTestId("reconciliation-review-form")).toBeNull();
  });

  it("聚焦视图点「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    fireEvent.click(screen.getByTestId("reconciliation-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("reconciliation-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("reconciliation-summary")).toBeTruthy();
    expect(screen.getByTestId("reconciliation-batches-table")).toBeTruthy();
    expect(screen.getByTestId("reconciliation-differences-table")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图（参数从 URL 重建）", async () => {
    window.history.replaceState({}, "", "/admin/finance/reconciliation?kpi=batches");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("reconciliation-batches-table")).toBeTruthy();
    expect(screen.queryByTestId("reconciliation-differences-table")).toBeNull();
  });

  it("浏览器后退（popstate）→ 退出聚焦视图回完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
    fireEvent.popState(window);
    await waitFor(() => expect(screen.queryByTestId("reconciliation-kpi-focus-banner")).toBeNull());
    expect(screen.getByTestId("reconciliation-summary")).toBeTruthy();
  });

  it("选中差异卡状态与差异表状态列渲染为中文枚举，不直出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-summary")).toBeTruthy());
    // 选中首个差异（status=pending）→ KPI 卡状态应显示中文“待处理”，不是 "pending"。
    fireEvent.click(screen.getByTestId("reconciliation-select-d-1"));
    const summary = screen.getByTestId("reconciliation-summary").textContent ?? "";
    expect(summary).toContain("待处理");
    expect(summary).not.toContain("pending");
    // 差异金额卡的口径说明不再直出接口路径英文。
    expect(summary).not.toContain("POST");
    const diffTable = screen.getByTestId("reconciliation-differences-table").textContent ?? "";
    expect(diffTable).toContain("待处理");
    expect(diffTable).toContain("已忽略");
    expect(diffTable).not.toContain("pending");
    expect(diffTable).not.toContain("ignored");
  });
});
