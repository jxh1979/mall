import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { RiskAlertsPage } from "./RiskAlertsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程：创建预警收起为「＋ 创建预警」入口 → 两步引导（分类 / 对象与摘要）→ 确认页提交。
const ALERTS = [
  { id: "ra-1", alert_type: "staff_behavior", risk_level: "high", object_type: "bar_order", object_id: "o-1", summary: "异常折扣", status: "open" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi() {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("risk-alerts")) return Promise.resolve(envelope(ALERTS));
    return Promise.resolve(envelope([]));
  });
  vi.stubGlobal("fetch", fetchMock);
  return { api: new AdminApi("test-token"), fetchMock };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const { api, fetchMock } = makeApi();
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<RiskAlertsPage {...props} />);
  return { fetchMock, setActionResult };
}

function postCalls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.filter((c) => String(c[0]).includes("/analytics/risk-alerts") && !String(c[0]).includes("/resolve") && (c[1]?.method ?? "GET") === "POST");
}

describe("RiskAlertsPage 引导式创建预警", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/risk/alerts");
  });

  it("默认只留入口按钮，不展示弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-create-open")).toBeTruthy());
    expect(screen.queryByTestId("risk-alert-guided")).toBeNull();
  });

  it("入口→两步→确认提交：POST /risk-alerts 且成功后关闭弹窗", async () => {
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alert-create-open"));

    // 第一步：分类（默认值即合法）
    await waitFor(() => expect(screen.getByTestId("risk-alert-type")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alert-guided-next"));

    // 第二步：对象与摘要
    await waitFor(() => expect(screen.getByTestId("risk-alert-summary-input")).toBeTruthy());
    fireEvent.change(screen.getByTestId("risk-alert-summary-input"), { target: { value: "复核异常折扣" } });
    fireEvent.click(screen.getByTestId("risk-alert-guided-next"));

    // 确认页
    await waitFor(() => expect(screen.getByTestId("risk-alert-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alert-create"));

    await waitFor(() => expect(postCalls(fetchMock).length).toBe(1));
    await waitFor(() => expect(screen.queryByTestId("risk-alert-guided")).toBeNull());
  });

  it("第二步校验拦截：摘要清空不到确认页、不提交", async () => {
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alert-create-open"));
    fireEvent.click(screen.getByTestId("risk-alert-guided-next"));
    await waitFor(() => expect(screen.getByTestId("risk-alert-summary-input")).toBeTruthy());
    fireEvent.change(screen.getByTestId("risk-alert-summary-input"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("risk-alert-guided-next"));
    expect(screen.getByTestId("risk-alert-guided-error").textContent).toBeTruthy();
    expect(screen.queryByTestId("risk-alert-guided-summary")).toBeNull();
    expect(postCalls(fetchMock).length).toBe(0);
  });
});
