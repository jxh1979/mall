// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "../AdminApp";
import { AdminApi } from "../client/adminClient";
import { OperatingParamsPage } from "./OperatingParamsPage";

const RANGE_MANAGE = "platform.sensitive_operation_range.manage";
const RANGE_PATH = "/api/v1/platform/projects/proj_1/sensitive-operation-range";

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

function stubFetch(grants: string[]) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    let data: unknown = [];
    if (url.endsWith("/api/v1/access/effective-permissions")) data = { grants, denies: [], wildcard: false };
    else if (url.includes("/sensitive-operation-range")) {
      data = init?.method === "PUT"
        ? { project_id: "proj_1", center_latitude: 31.2304, center_longitude: 121.4737, radius_meters: 500, min_radius_meters: 100, max_radius_meters: 2000 }
        : { project_id: "proj_1" };
    }
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function putBody(fetchMock: ReturnType<typeof stubFetch>): Record<string, unknown> | null {
  const call = fetchMock.mock.calls.find((c) => String(c[0]).endsWith(RANGE_PATH) && c[1]?.method === "PUT");
  return call ? (JSON.parse(String(call[1]?.body)) as Record<string, unknown>) : null;
}

function mountApp(grants: string[]) {
  stubFetch(grants);
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "ops-1", username: "ops", accountType: "staff" }, sessionId: "session-1" }));
  window.history.replaceState({}, "", "/admin/analytics/operating-params");
  render(<AdminApp />);
}

// 第 123 条 123-R1（BSM:1630「授权范围按项目在后台配置（业务参数，按项目可配，须同时配置上下限）。」）
describe("经营参数页 敏感操作授权范围（按项目）", () => {
  it("选项目查询后填圆心、半径与上下限保存，PUT 请求体为蛇形键", async () => {
    const fetchMock = stubFetch([RANGE_MANAGE]);
    render(<OperatingParamsPage api={new AdminApi("")} user={{ id: "u1", username: "ops", accountType: "staff" }} setActionResult={vi.fn()}
      setGlobalError={vi.fn()} effectivePermissions={{ status: "ready", value: { grants: [RANGE_MANAGE], denies: [], wildcard: false } }} />);
    // 先等页面被动加载落定（表格出现）与新区块出现，再操作。
    await waitFor(() => expect(screen.queryByTestId("operating-params-table")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("sensitive-range-project")).not.toBeNull());
    fireEvent.change(screen.getByTestId("sensitive-range-project"), { target: { value: "proj_1" } });
    fireEvent.click(screen.getByTestId("sensitive-range-query"));
    await waitFor(() => expect(screen.getByTestId("sensitive-range-current").textContent ?? "").toContain("未配置"));
    fireEvent.change(screen.getByTestId("sensitive-range-latitude"), { target: { value: "31.2304" } });
    fireEvent.change(screen.getByTestId("sensitive-range-longitude"), { target: { value: "121.4737" } });
    fireEvent.change(screen.getByTestId("sensitive-range-radius"), { target: { value: "500" } });
    fireEvent.change(screen.getByTestId("sensitive-range-min"), { target: { value: "100" } });
    fireEvent.change(screen.getByTestId("sensitive-range-max"), { target: { value: "2000" } });
    fireEvent.click(screen.getByTestId("sensitive-range-save"));
    await waitFor(() => {
      const body = putBody(fetchMock);
      expect(body).toBeTruthy();
      expect(Object.keys(body!).sort()).toEqual(
        ["center_latitude", "center_longitude", "idempotency_key", "max_radius_meters", "min_radius_meters", "radius_meters"]);
      expect(body).toMatchObject({ center_latitude: 31.2304, center_longitude: 121.4737, radius_meters: 500, min_radius_meters: 100, max_radius_meters: 2000 });
    });
    await waitFor(() => expect(screen.getByTestId("sensitive-range-current").textContent ?? "").toContain("500"));
  });

  it("只持授权范围配置权限也能打开经营参数页并看到该区块", async () => {
    mountApp([RANGE_MANAGE]);
    await waitFor(() => expect(screen.queryByTestId("sensitive-range-project")).not.toBeNull());
    expect(screen.queryByTestId("admin-permission-denied")).toBeNull();
  });

  it("不持授权范围配置权限时经营参数页不显示该区块", async () => {
    mountApp(["analytics.dashboard.view"]);
    await waitFor(() => expect(screen.queryByTestId("operating-params-table")).not.toBeNull());
    expect(screen.queryByTestId("sensitive-range-project")).toBeNull();
  });
});
