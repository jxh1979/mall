import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { RiskAlertsPage } from "./RiskAlertsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 单页表单弹窗：创建预警收起为「＋ 创建预警」入口 → 一屏表单 → 提交。
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

describe("RiskAlertsPage 单页表单创建预警", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/risk/alerts");
  });

  it("默认只留入口按钮，不展示弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-create-open")).toBeTruthy());
    expect(screen.queryByTestId("risk-alert-guided")).toBeNull();
  });

  it("入口→一屏填→提交：POST /risk-alerts 且成功后关闭弹窗", async () => {
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alert-create-open"));

    // 单页表单：所有字段一屏，填摘要后直接提交
    await waitFor(() => expect(screen.getByTestId("risk-alert-summary-input")).toBeTruthy());
    // 第十八批 AP 段第 229 条 229-R5、229-R10④：对象编号、摘要不预填。
    expect((screen.getByTestId("risk-alert-object-id") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("risk-alert-summary-input") as HTMLInputElement).value).toBe("");
    fireEvent.change(screen.getByTestId("risk-alert-summary-input"), { target: { value: "复核异常折扣" } });
    fireEvent.click(screen.getByTestId("risk-alert-create"));

    await waitFor(() => expect(postCalls(fetchMock).length).toBe(1));
    // 第 229 条 229-R3⑤、229-R10④：新建预警不带写死附件、不挂写死规则编号。
    const body = JSON.parse(String(postCalls(fetchMock)[0]![1].body)) as Record<string, unknown>;
    expect(body).not.toHaveProperty("evidence_attachment_ids");
    expect(body).not.toHaveProperty("rule_id");
    await waitFor(() => expect(screen.queryByTestId("risk-alert-guided")).toBeNull());
  });

  it("校验拦截：摘要清空提交被拦、不发请求", async () => {
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alert-create-open"));
    await waitFor(() => expect(screen.getByTestId("risk-alert-summary-input")).toBeTruthy());
    fireEvent.change(screen.getByTestId("risk-alert-summary-input"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("risk-alert-create"));
    expect(screen.getByTestId("risk-alert-guided-error").textContent).toBeTruthy();
    expect(postCalls(fetchMock).length).toBe(0);
  });
});

// 第十八批 AP 段第 229 条 229-R3⑤、229-R5：处理原因不预填；处理请求不带写死附件。
describe("RiskAlertsPage 处理不预填、不写死附件（第 229 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/risk/alerts");
  });

  it("处理原因初始为空；填写后处理，请求体不带写死附件", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alert-select-ra-1")).toBeTruthy());
    expect((screen.getByTestId("risk-alert-resolution-reason") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("risk-alert-select-ra-1"));
    fireEvent.change(screen.getByTestId("risk-alert-resolution-reason"), { target: { value: "复核后确认误报" } });
    fireEvent.click(screen.getByTestId("risk-alert-resolve"));

    const resolveCalls = () => fetchMock.mock.calls.filter((c) => String(c[0]).includes("/risk-alerts/ra-1/resolve"));
    await waitFor(() => expect(resolveCalls()).toHaveLength(1));
    const body = JSON.parse(String(resolveCalls()[0]![1].body)) as Record<string, unknown>;
    expect(body).toMatchObject({ reason: "复核后确认误报" });
    expect(body).not.toHaveProperty("evidence_attachment_ids");
  });
});
