// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ExceptionAlertsPage } from "./ExceptionAlertsPage";
import { AdminApi } from "../client/adminClient";
import { ActionResultView } from "../components/ActionResultView";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BC 段第 369 条 369-R2（BSM:1657「新增「异常预警」页面，集中汇集账户与出入场行为异常……供风控岗与管理层查看与处置。
// ……每条预警的查看、转办与处置结论均须留痕。」；ENUM:217 风险四级；第十二批裁决 14「由人在异常队列里按审批结果处理关闭」）：
// 进页按平台、票务、停车三类来源取统一风控预警队列；点行取单条明细；按状态出认领、转办、处置。假响应按接口真实形状（蛇形键文本体）。

const LIST = "/api/v1/evidence/risk-alerts?source_domain=platform,ticketing,parking&limit=200";
const ALERTS = [
  { alert_id: "alert_1", source_domain: "ticketing", source_ref: "tk_parent_9", alert_type: "ticket.frequent_entry", risk_level: "suspicious",
    status: "open", summary: "该票第 2 人份 10 分钟内出入 3 次", first_handler_id: null, resolution_conclusion: null, project_id: "prj_cs",
    created_at: "2026-11-19T03:04:05Z" },
  { alert_id: "alert_2", source_domain: "platform", source_ref: "cus_7", alert_type: "customer.churn", risk_level: "high",
    status: "handling", summary: "同一手机号 30 天内注销注册 3 次", first_handler_id: "u_risk", resolution_conclusion: null, project_id: null,
    created_at: "2026-11-18T03:04:05Z" },
  { alert_id: "alert_3", source_domain: "parking", source_ref: "pks_1", alert_type: "parking.fee_evasion", risk_level: "high",
    status: "resolved", summary: "粤A12345 未缴费离场", first_handler_id: "u_risk", resolution_conclusion: "已补缴", project_id: "prj_cs",
    created_at: "2026-11-17T03:04:05Z" },
];
const DETAIL = {
  alert: ALERTS[0],
  actions: [
    { action_id: "ract_1", alert_id: "alert_1", action_type: "report", actor_id: "system", from_status: null, to_status: "open",
      reason: "该票第 2 人份 10 分钟内出入 3 次", evidence_refs: [], created_at: "2026-11-19T03:04:05Z" },
    { action_id: "ract_2", alert_id: "alert_1", action_type: "claim", actor_id: "u_risk", from_status: "open", to_status: "handling",
      reason: null, evidence_refs: [], created_at: "2026-11-19T04:00:00Z" },
    { action_id: "ract_3", alert_id: "alert_1", action_type: "transfer", actor_id: "u_risk", from_status: "handling", to_status: "handling",
      reason: "转办给 u_x", evidence_refs: [], created_at: "2026-11-19T05:00:00Z" },
  ],
};
const ADMIN: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

let fetchMock: ReturnType<typeof vi.fn>;

// 结果区照 AdminApp 用 ActionResultView 显示页面交给 setActionResult 的对象。
function WithResult(props: AdminPageProps) {
  const [result, setResult] = React.useState<unknown>(null);
  return <><ExceptionAlertsPage {...props} setActionResult={setResult} />{result === null ? null : <ActionResultView value={result} />}</>;
}

function renderPage(alerts: unknown[] = ALERTS, detail: unknown = DETAIL, postResult: (url: string) => unknown = () => ALERTS[0]) {
  fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST") return Promise.resolve(ok(postResult(url)));
    if (url.endsWith("/api/v1/evidence/risk-alerts/alert_1")) return Promise.resolve(ok(detail));
    return Promise.resolve(ok(alerts));
  });
  vi.stubGlobal("fetch", fetchMock);
  const props: AdminPageProps = {
    api: new AdminApi(""),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN,
  };
  render(<WithResult {...props} />);
}

const calls = () => fetchMock.mock.calls.map((call: unknown[]) => {
  const init = call[1] as RequestInit | undefined;
  return {
    url: String(call[0]),
    method: init?.method ?? "GET",
    body: init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : null,
  };
});
const posted = (suffix: string) => calls().filter((call) => call.method === "POST" && call.url.endsWith(suffix));
const cell = (alertId: string, index: number) => screen.getByTestId(`exception-alert-row-${alertId}`).querySelectorAll("td")[index]?.textContent;

async function pageReady() {
  await waitFor(() => expect(screen.getByTestId("exception-alert-row-alert_3")).toBeTruthy());
}

describe("ExceptionAlertsPage 异常预警（369-R2）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("进页按平台、票务、停车三类来源取统一预警队列，类型、等级与状态显示中文", async () => {
    renderPage();
    await waitFor(() => expect(calls().some((call) => call.url.endsWith(LIST))).toBe(true));
    await pageReady();
    const table = screen.getByTestId("exception-alerts-table").textContent ?? "";
    expect(table).toContain("高频出入场");
    expect(table).toContain("同一手机号注销注册超限");
    expect(table).toContain("停车逃费");
    expect(table).toContain("待处理");
    expect(table).toContain("处理中");
    expect(table).toContain("已处置");
    expect(cell("alert_1", 2)).toBe("中");
    expect(cell("alert_2", 2)).toBe("高");
    expect(table).not.toContain("customer.churn");
    expect(table).not.toContain("suspicious");
    expect(table).not.toContain("open");
  });

  it("状态筛选带 status 重新取", async () => {
    renderPage();
    await pageReady();
    fireEvent.change(screen.getByLabelText("状态"), { target: { value: "handling" } });
    await waitFor(() => expect(calls().some((call) => call.url.endsWith(`${LIST}&status=handling`))).toBe(true));
  });

  it("点行取单条明细，抽屉列出所属综合体、关联编号与处置记录中文", async () => {
    renderPage();
    await pageReady();
    fireEvent.click(screen.getByTestId("exception-alert-row-alert_1"));
    await waitFor(() => expect(calls().some((call) => call.url.endsWith("/api/v1/evidence/risk-alerts/alert_1"))).toBe(true));
    await waitFor(() => expect(screen.getByTestId("row-detail-drawer").textContent).toContain("tk_parent_9"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    for (const text of ["所属综合体", "prj_cs", "关联编号", "处置记录", "上报", "认领", "转办", "系统", "转办给 u_x"]) {
      expect(drawer).toContain(text);
    }
    expect(drawer).not.toContain("ticket.frequent_entry");
  });

  it("按状态出动作：待处理出认领与处置，处理中出转办与处置，已处置不出动作；必填项为空不提交", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPage();
    await pageReady();

    fireEvent.click(screen.getByTestId("exception-alert-select-alert_1"));
    await waitFor(() => expect(screen.getByTestId("exception-alert-claim")).toBeTruthy());
    expect(screen.queryByTestId("exception-alert-transfer")).toBeNull();
    expect(screen.getByTestId("exception-alert-resolve")).toBeTruthy();
    fireEvent.click(screen.getByTestId("exception-alert-claim"));
    await waitFor(() => expect(posted("/api/v1/evidence/risk-alerts/alert_1/claim")).toHaveLength(1));
    expect(posted("/api/v1/evidence/risk-alerts/alert_1/claim")[0]?.body?.idempotency_key).toBeTruthy();

    await pageReady();
    fireEvent.click(screen.getByTestId("exception-alert-select-alert_2"));
    await waitFor(() => expect(screen.getByTestId("exception-alert-transfer")).toBeTruthy());
    expect(screen.queryByTestId("exception-alert-claim")).toBeNull();
    fireEvent.click(screen.getByTestId("exception-alert-transfer"));
    expect(screen.getByTestId("exception-alert-error").textContent).toContain("请填写转办给哪位员工（员工编号）");
    expect(posted("/api/v1/evidence/risk-alerts/alert_2/transfer")).toHaveLength(0);
    fireEvent.change(screen.getByLabelText("转办给（员工编号）"), { target: { value: "u_x" } });
    fireEvent.click(screen.getByTestId("exception-alert-transfer"));
    await waitFor(() => expect(posted("/api/v1/evidence/risk-alerts/alert_2/transfer")).toHaveLength(1));
    expect(posted("/api/v1/evidence/risk-alerts/alert_2/transfer")[0]?.body).toMatchObject({ target_handler_id: "u_x" });

    await pageReady();
    fireEvent.click(screen.getByTestId("exception-alert-resolve"));
    expect(screen.getByTestId("exception-alert-error").textContent).toContain("请填写处置结论");
    expect(posted("/api/v1/evidence/risk-alerts/alert_2/resolve")).toHaveLength(0);
    fireEvent.change(screen.getByLabelText("处置结论"), { target: { value: "核实为借票，已提醒" } });
    fireEvent.click(screen.getByTestId("exception-alert-resolve"));
    await waitFor(() => expect(posted("/api/v1/evidence/risk-alerts/alert_2/resolve")).toHaveLength(1));
    expect(posted("/api/v1/evidence/risk-alerts/alert_2/resolve")[0]?.body).toMatchObject({ resolution_conclusion: "核实为借票，已提醒" });

    await pageReady();
    fireEvent.click(screen.getByTestId("exception-alert-select-alert_3"));
    await waitFor(() => expect(screen.queryByTestId("exception-alert-actions")).toBeNull());
  });

  // 18BC 收尾复核修正（PGM:287「展示给用户的一切文字，包括……状态、枚举取值……必须全部为中文」）：认领、转办、处置成功后结果区只写动作名与预警编号，
  // 不把整条预警记录（来源业务域、首报人、共用码表的状态名）搬上结果区。
  it("认领、转办、处置成功后结果区只写动作名与预警编号", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const parking = { ...ALERTS[2], status: "handling", resolution_conclusion: null };
    renderPage([ALERTS[0], ALERTS[1], parking], DETAIL, (url) => (url.includes("/alert_1/")
      ? { ...ALERTS[0], status: "handling", first_reported_by: "system", first_handler_id: "u_risk", escalated_to: null, resolved_by: null,
        updated_at: "2026-11-19T04:00:00Z" }
      : { ...parking, status: url.endsWith("/resolve") ? "resolved" : "handling", first_reported_by: "system:parking", escalated_to: null,
        resolved_by: url.endsWith("/resolve") ? "u_risk" : null, resolution_conclusion: url.endsWith("/resolve") ? "已补缴" : null,
        updated_at: "2026-11-19T05:00:00Z" }));
    await pageReady();
    const result = () => screen.getByTestId("admin-action-result").textContent ?? "";
    const enabled = (testId: string) => waitFor(() => expect((screen.getByTestId(testId) as HTMLButtonElement).disabled).toBe(false));
    const chineseOnly = () => {
      for (const text of ["parking", "ticketing", "system:", "已解决"]) expect(result()).not.toContain(text);
    };

    fireEvent.click(screen.getByTestId("exception-alert-select-alert_1"));
    await enabled("exception-alert-claim");
    fireEvent.click(screen.getByTestId("exception-alert-claim"));
    await waitFor(() => expect(screen.queryByTestId("admin-action-result")?.textContent ?? "").toContain("异常预警已认领"));
    expect(result()).toContain("alert_1");
    chineseOnly();

    fireEvent.click(screen.getByTestId("exception-alert-select-alert_3"));
    await enabled("exception-alert-transfer");
    fireEvent.change(screen.getByLabelText("转办给（员工编号）"), { target: { value: "u_x" } });
    fireEvent.click(screen.getByTestId("exception-alert-transfer"));
    await waitFor(() => expect(result()).toContain("异常预警已转办"));
    expect(result()).toContain("alert_3");
    chineseOnly();

    await enabled("exception-alert-resolve");
    fireEvent.change(screen.getByLabelText("处置结论"), { target: { value: "已补缴" } });
    fireEvent.click(screen.getByTestId("exception-alert-resolve"));
    await waitFor(() => expect(result()).toContain("异常预警已处置"));
    expect(result()).toContain("alert_3");
    chineseOnly();
  });

  // 18BC 收尾复核修正（PGM:287「……日期时间等，必须全部为中文」；BSM:1438「人工放行必须记录原因、操作人、车牌和时间」）：
  // 上报时间与处置记录时间按北京时间显示，与停车人工放行摘要里的北京时间同口径。
  it("上报时间与处置记录时间按北京时间显示", async () => {
    const late = { ...ALERTS[0], created_at: "2026-11-19T17:30:00Z" };
    renderPage([late, ALERTS[1], ALERTS[2]], { alert: late, actions: [{ ...DETAIL.actions[0], created_at: "2026-11-19T17:30:00Z" }] });
    await pageReady();
    expect(cell("alert_1", 6)).toBe("2026年11月20日 01:30");
    fireEvent.click(screen.getByTestId("exception-alert-row-alert_1"));
    await waitFor(() => expect(screen.getByTestId("row-detail-drawer").textContent).toContain("tk_parent_9"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer.split("2026年11月20日 01:30").length - 1).toBe(2);
    expect(drawer).not.toContain("17:30");
  });
});
