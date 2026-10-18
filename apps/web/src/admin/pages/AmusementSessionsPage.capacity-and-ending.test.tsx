// 文件版本：V0.04
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
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
            end_time: "2026-10-18T22:00:00Z", status: "open", capacity: 1, used_capacity: 1, locked_capacity: 1 },
          { id: "s2", project_id: "a1", start_time: "2026-10-17T08:00:00Z", end_time: "2026-10-17T22:00:00Z", status: "closed" }]
        : url.endsWith("/amusement/sessions/s2/endings")
          ? [{ id: "end1", mode: "early", actual_end_at: "2026-10-17T20:00:00Z", planned_end_at: "2026-10-17T22:00:00Z",
              cleared_people: 1, confirmed_by: "u1", reason: "负责人现场清场完成", business_date: "2026-10-17", config_version_id: "cfg1" }]
          : url.endsWith("/amusement/sessions/s2/followups")
            ? [{ id: "f1", source_type: "reservation", source_id: "resv1", status: "pending" },
              { id: "f2", source_type: "entry", source_id: "entry1", entitlement_id: "ent1", order_id: "ord1", status: "pending" },
              { id: "f3", source_type: "entry", source_id: "entry2", entitlement_id: "ent2", order_id: "ord1", status: "completed",
                resolution_type: "compensation_issued", resolution_id: "afcb1", resolved_by: "u9" }]
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

// TK18-R2：ENUM L142 / BSM L1293 场次状态用「已结束」；「已关闭」是项目开放状态（BSM L1292）的词，场次行不得沿用共享码表。
it("已结束场次显示为已结束", async () => {
  setup();
  const row = await screen.findByTestId("amusement-session-row-s2");
  expect(within(row).getByText("已结束")).toBeTruthy();
  expect(row.textContent).not.toContain("已关闭");
});

// TK18-R3（COVERAGE_ONLY）：BSM L1263「已售但未履行的权益按既有取消、补偿规则处理并留痕」——已结束场次详情读 endings/followups，
// 显示提前结束留痕（模式、释放人数）与待按原规则处理的待办行。
it("已结束场次详情显示提前结束留痕与待办", async () => {
  setup();
  fireEvent.click(await screen.findByTestId("amusement-session-row-s2"));
  const history = screen.getByTestId("amusement-session-ending-history");
  await waitFor(() => expect(history.textContent).toContain("负责人确认提前结束"));
  expect(history.textContent).toContain("释放本场在场人数：1");
  expect(history.textContent).toContain("待按原规则处理");
});

// TK18-R2-COMP（2026-10-18 操作员裁决补充第 3 条）：已入场客人比照 ENUM L148 全园故障补偿规则补偿，发放前须游乐公司负责人与
// 工程或安全岗位共同确认；发放后入场待办写回补偿批次。未入场的票不建补偿待办。
it("已入场待办显示补偿待发放与发放结果", async () => {
  setup();
  fireEvent.click(await screen.findByTestId("amusement-session-row-s2"));
  const history = screen.getByTestId("amusement-session-ending-history");
  await waitFor(() => expect(history.textContent).toContain("补偿待发放"));
  expect(history.textContent).toContain("游乐公司负责人与工程或安全岗位共同确认");
  expect(history.textContent).toContain("已按全园故障补偿规则发放补偿");
  expect(history.textContent).toContain("afcb1");
  expect(history.textContent).toContain("待按原规则处理");
});
