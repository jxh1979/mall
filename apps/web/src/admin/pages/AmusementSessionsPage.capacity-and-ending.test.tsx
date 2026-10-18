// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";

afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/admin/amusement/sessions"); });

function setup() {
  const fetcher = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const items = url.endsWith("/amusement/projects")
      ? [{ id: "a1", project_name: "游乐项目", capacity: 30, open_status: "open" }]
      : url.endsWith("/amusement/sessions")
        ? [{ id: "s1", project_id: "a1", start_time: "2026-10-18T08:00:00Z",
            end_time: "2026-10-18T22:00:00Z", status: "open", capacity: 1, used_capacity: 1, locked_capacity: 1 }]
        : [];
    return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK",
      data: init?.method === "POST" ? { id: "ending1" } : { items, total: items.length } }),
      { status: 200, headers: { "content-type": "application/json" } }));
  });
  vi.stubGlobal("fetch", fetcher);
  render(<AmusementSessionsPage api={new AdminApi("token")}
    user={{ id: "u1", username: "操作员", accountType: "platform_admin" }}
    setActionResult={vi.fn()} setGlobalError={vi.fn()}
    effectivePermissions={{ status: "ready", value: { grants: [], denies: [], wildcard: true } }} />);
  return fetcher;
}

it("场次不再单独设置或显示容量，项目硬容量按唯一项目汇总", async () => {
  setup();
  await screen.findByTestId("amusement-sessions-table");
  expect(screen.getByTestId("amusement-sessions-kpi-capacity").textContent).toBe("30");
  expect(screen.getByTestId("amusement-sessions-table").textContent).not.toContain("锁定");
  fireEvent.click(screen.getByTestId("amusement-session-create-open"));
  expect(screen.queryByTestId("amusement-session-capacity")).toBeNull();
});

it("提前结束必须确认实际清场，不能直接人工改在玩人数", async () => {
  const fetcher = setup();
  await screen.findByTestId("amusement-sessions-table");
  fireEvent.click(screen.getByTestId("amusement-session-end-s1"));
  expect(screen.getByTestId("amusement-session-end-submit")).toBeDisabled();
  fireEvent.change(screen.getByLabelText("实际清场时刻"), { target: { value: "2026-10-18T10:00" } });
  fireEvent.change(screen.getByLabelText("提前结束原因"), { target: { value: "已完成清场" } });
  fireEvent.click(screen.getByLabelText("确认现场全部客人已离场"));
  fireEvent.click(screen.getByTestId("amusement-session-end-submit"));
  await waitFor(() => expect(fetcher.mock.calls.some(([url, init]) =>
    String(url).endsWith("/amusement/sessions/s1/end") && init?.method === "POST")).toBe(true));
  const call = fetcher.mock.calls.find(([url, init]) =>
    String(url).endsWith("/amusement/sessions/s1/end") && init?.method === "POST")!;
  const body = JSON.parse(String(call[1]?.body));
  expect(body.clearance_confirmed).toBe(true);
  expect(body).not.toHaveProperty("occupied_count");
});
