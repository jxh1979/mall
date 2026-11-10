// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SupervisorReviewPage } from "./SupervisorReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AD 段第 204 条 204-R2（PGM:529「后台配置的时限（业务参数，按审批类型可配，出厂默认 3 个工作日，涉钱审批出厂默认 1 个工作日，
// 须同时配置上下限）」；PGM:291「用户看到的每一处文字都是中文」）：主管复核页「审批时限（工作日）」区块——清单按中文审批类型列出，
// 修改三值同配、前端先校验，保存走 PUT /api/v1/platform/approval-deadlines/{action_type}，后端报错原文上屏，无 admission.policy.manage 不可改。
const POLICIES = [
  { id: "sp-1", policy_code: "contract_violation_l3", action_type: "contract_violation", required_level: 3, blocks_by_default: true, required_evidence_types: ["violation_summary"], status: "active" },
];

function deadline(actionType: string, money: boolean) {
  const days = money ? 1 : 3;
  return { action_type: actionType, money, factory_working_days: days, deadline_working_days: days, min_working_days: 1, max_working_days: 30, customized: false };
}

const DEADLINES = [
  deadline("access.role_viewable_projects.reduce", false),
  deadline("amusement.manual_override", false),
  deadline("bar.bill.unpaid_close", true),
  deadline("bar.bill_waiver", true),
  deadline("bill_waiver", true),
  deadline("contract.new", false),
  deadline("staff.payroll.adjustment.add", true),
];

const INVALID = "审批时限须在上下限之间，下限不小于 1 个工作日";

function raw(data: unknown, status = 200): Response {
  return new Response(JSON.stringify({ success: status < 400, code: "OK", message: "success", data }), { status, headers: { "content-type": "application/json" } });
}

function makeApi(options: { rejectPut?: boolean } = {}): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("supervisor-review-policies")) return Promise.resolve(raw(POLICIES));
    if (url.includes("/approval-deadlines/") && init?.method === "PUT") {
      if (options.rejectPut) {
        return Promise.resolve(new Response(JSON.stringify({ success: false, code: "PARAM_INVALID", detail_code: "VALIDATION_FAILED", message: INVALID }),
          { status: 400, headers: { "content-type": "application/json" } }));
      }
      return Promise.resolve(raw({ ...deadline("staff.payroll.adjustment.add", true), deadline_working_days: 5, min_working_days: 2, max_working_days: 10, customized: true }));
    }
    if (url.includes("/approval-deadlines")) return Promise.resolve(raw(DEADLINES));
    return Promise.resolve(raw([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view", "governance.supervisor_review.manage"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS, options: Parameters<typeof makeApi>[0] = {}) {
  const props: AdminPageProps = {
    api: makeApi(options),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<SupervisorReviewPage {...props} />);
  return props;
}

function calls(method: string, part: string) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) =>
    String(input).includes(part) && ((init as RequestInit | undefined)?.method ?? "GET") === method);
}

function cells(actionType: string) {
  return Array.from(screen.getByTestId(`approval-deadline-row-${actionType}`).querySelectorAll("td")).map((cell) => cell.textContent);
}

function fill(testId: string, value: string) {
  fireEvent.change(screen.getByTestId(testId), { target: { value } });
}

async function openEditor(actionType: string) {
  await waitFor(() => expect(screen.queryByTestId(`approval-deadline-edit-${actionType}`)).not.toBeNull());
  await waitFor(() => expect(calls("GET", "/approval-deadlines")).toHaveLength(1));
  fireEvent.click(screen.getByTestId(`approval-deadline-edit-${actionType}`));
  await waitFor(() => expect(screen.queryByTestId("approval-deadline-form")).not.toBeNull());
}

describe("SupervisorReviewPage 第十八批 AD 段 204-R2：审批时限区块", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("① 清单显示中文审批类型、涉钱出厂 1 其余 3，区块里没有英文动作码", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("approval-deadline-row-bar.bill.unpaid_close")).not.toBeNull());
    expect(cells("staff.payroll.adjustment.add")).toEqual(["员工奖惩入账", "是", "1", "1", "1", "30", "修改"]);
    expect(cells("contract.new")).toEqual(["新建合同", "否", "3", "3", "1", "30", "修改"]);
    expect(cells("bar.bill.unpaid_close").slice(0, 3)).toEqual(["包厢关台未付清确认", "是", "1"]);
    expect(cells("amusement.manual_override")[0]).toBe("游乐人工干预");
    expect(cells("access.role_viewable_projects.reduce")[0]).toBe("减少角色可查看项目");
    const table = screen.getByTestId("approval-deadline-table").textContent ?? "";
    expect(table).not.toContain("未识别类型");
    expect(table).not.toMatch(/[A-Za-z]/);
  });

  it("② 改为下限 2、时限 5、上限 10 保存：PUT 路径带该审批类型、请求体三值，成功提示并重读", async () => {
    const props = renderPage();
    await openEditor("staff.payroll.adjustment.add");
    fill("approval-deadline-min", "2");
    fill("approval-deadline-days", "5");
    fill("approval-deadline-max", "10");
    fireEvent.click(screen.getByTestId("approval-deadline-save"));
    await waitFor(() => expect(calls("PUT", "/approval-deadlines/")).toHaveLength(1));
    const [input, init] = calls("PUT", "/approval-deadlines/")[0]!;
    expect(String(input)).toContain("/api/v1/platform/approval-deadlines/staff.payroll.adjustment.add");
    const body = JSON.parse(String((init as RequestInit).body));
    expect(body).toMatchObject({ deadline_working_days: 5, min_working_days: 2, max_working_days: 10 });
    expect(body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("员工奖惩入账 5 个工作日")));
    await waitFor(() => expect(calls("GET", "/approval-deadlines")).toHaveLength(2));
  });

  it("③ 下限大于时限：不发请求，给中文提示", async () => {
    const props = renderPage();
    await openEditor("contract.new");
    fill("approval-deadline-min", "6");
    fill("approval-deadline-days", "5");
    fill("approval-deadline-max", "10");
    fireEvent.click(screen.getByTestId("approval-deadline-save"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(INVALID));
    expect(calls("PUT", "/approval-deadlines/")).toHaveLength(0);
  });

  it("④ 后端 400：显示后端中文原文，表格不变", async () => {
    const props = renderPage(ADMIN_PERMS, { rejectPut: true });
    await openEditor("staff.payroll.adjustment.add");
    fill("approval-deadline-min", "2");
    fill("approval-deadline-days", "5");
    fill("approval-deadline-max", "10");
    fireEvent.click(screen.getByTestId("approval-deadline-save"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining(INVALID)));
    expect(props.setActionResult).not.toHaveBeenCalled();
    expect(cells("staff.payroll.adjustment.add")).toEqual(["员工奖惩入账", "是", "1", "1", "1", "30", "修改"]);
  });

  it("⑤ 不持 admission.policy.manage：修改按钮不可用", async () => {
    renderPage(VIEW_ONLY_PERMS);
    await waitFor(() => expect(screen.queryByTestId("approval-deadline-edit-contract.new")).not.toBeNull());
    expect((screen.getByTestId("approval-deadline-edit-contract.new") as HTMLButtonElement).disabled).toBe(true);
  });

  it("⑥ 第十八批裁决 47：两行免单各叫各的——bill_waiver「免单」，bar.bill_waiver「酒吧账单免单」", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("approval-deadline-row-bar.bill_waiver")).not.toBeNull());
    expect(cells("bill_waiver")[0]).toBe("免单");
    expect(cells("bar.bill_waiver")[0]).toBe("酒吧账单免单");
  });
});
