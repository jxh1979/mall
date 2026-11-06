// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 2026-10-18 操作员裁决：销假改为「本人申请＋主管批准」——员工本人申请后请假单为 revoke_requested（待销假审批），
// 有排班管理权的主管在本页批准（→ 已销假，BSM L1481 冲正）或驳回（假仍有效）。
const PAYROLL = [{ user_id: "u-9", base_salary_cent: 800000, net_pay_cent: 760000 }];
const REVOKE_REQUESTED = {
  id: "lv_rr", user_id: "u-9", leave_type: "personal", start_date: "2099-09-10", end_date: "2099-09-11", reason: "事假",
  status: "revoke_requested", review_note: "准", revoke_requested_by: "u-9", revoke_request_reason: "已到岗",
};

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function stubFetch(posts: { url: string; body: Record<string, unknown> }[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST" && url.includes("/staff/leaves/lv_rr/revoke")) {
      const body = JSON.parse(String(init.body)) as Record<string, unknown>;
      posts.push({ url, body });
      return Promise.resolve(dataEnvelope({ ...REVOKE_REQUESTED, status: body.decision === "approve" ? "revoked" : "approved" }));
    }
    if (url.includes("/staff/leaves")) return Promise.resolve(listEnvelope(url.includes("status=revoke_requested") ? [REVOKE_REQUESTED] : []));
    if (url.includes("/staff/payroll")) return Promise.resolve(listEnvelope(PAYROLL));
    return Promise.resolve(listEnvelope([]));
  }));
}

function renderPage() {
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<StaffPayrollLeavePage {...props} />);
}

describe("StaffPayrollLeavePage 销假审批（本人申请＋主管批准）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("待销假审批的请假单列出、状态显示「待销假审批」并带员工申请理由；批准走 revoke 端点 decision=approve", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    stubFetch(posts);
    renderPage();

    await waitFor(() => expect(screen.queryByTestId("leave-row-lv_rr")).not.toBeNull());
    const row = screen.getByTestId("leave-row-lv_rr");
    expect(row.textContent).toContain("待销假审批");
    expect(row.textContent).toContain("已到岗");
    expect(row.textContent).not.toContain("revoke_requested");

    // RowActions 两个以上动作收进「操作 ▾」下拉：先点触发器再点菜单项。
    fireEvent.click(screen.getByTestId("leave-actions-lv_rr"));
    await waitFor(() => expect(screen.queryByTestId("leave-revoke-approve-lv_rr")).not.toBeNull());
    fireEvent.click(screen.getByTestId("leave-revoke-approve-lv_rr"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]?.url).toContain("/api/v1/staff/leaves/lv_rr/revoke");
    expect(posts[0]?.url).not.toContain("revoke-request");
    expect(posts[0]?.body.decision).toBe("approve");
  });

  it("驳回销假走 revoke 端点 decision=reject（假仍有效）", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    stubFetch(posts);
    renderPage();

    await waitFor(() => expect(screen.queryByTestId("leave-actions-lv_rr")).not.toBeNull());
    fireEvent.click(screen.getByTestId("leave-actions-lv_rr"));
    await waitFor(() => expect(screen.queryByTestId("leave-revoke-reject-lv_rr")).not.toBeNull());
    expect(screen.queryByTestId("leave-approve-lv_rr")).toBeNull();
    fireEvent.click(screen.getByTestId("leave-revoke-reject-lv_rr"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]?.url).toContain("/api/v1/staff/leaves/lv_rr/revoke");
    expect(posts[0]?.body.decision).toBe("reject");
  });
});

// 第十八批 W 段 209-R1（PGM:291「用户看到的每一处文字都是中文，含状态、枚举、日期、按钮、提示、报错、导出与短信。」）：
// 请假审批与销假审批被后端拒绝（如已被他人处理）时，页面经全局报错显示中文原文，不记已审批。修复前按钮 void 调用无 catch，什么都不显示。
describe("StaffPayrollLeavePage 请假与销假审批被拒时显示报错（209-R1）", () => {
  afterEach(() => vi.unstubAllGlobals());

  const PENDING = { id: "lv_p", user_id: "u-9", leave_type: "personal", start_date: "2099-09-12", end_date: "2099-09-12", reason: "事假", status: "pending" };
  const message = "请假申请已审批，不能重复处理";

  function stubRejected() {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST" && /\/staff\/leaves\/[^/]+\/(review|revoke)$/.test(url)) {
        return Promise.resolve(new Response(JSON.stringify({ success: false, code: "STATE_CONFLICT", message }), { status: 409, headers: { "content-type": "application/json" } }));
      }
      if (url.includes("/staff/leaves")) return Promise.resolve(listEnvelope(url.includes("status=revoke_requested") ? [REVOKE_REQUESTED] : [PENDING]));
      if (url.includes("/staff/payroll")) return Promise.resolve(listEnvelope(PAYROLL));
      return Promise.resolve(listEnvelope([]));
    }));
  }
  function renderWithSpies() {
    const setActionResult = vi.fn();
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);
    return { setActionResult, setGlobalError };
  }

  it("请假「通过」接口回 409 时显示后端中文原文，不记已审批", async () => {
    stubRejected();
    const { setActionResult, setGlobalError } = renderWithSpies();
    await waitFor(() => expect(screen.queryByTestId("leave-actions-lv_p")).not.toBeNull());

    fireEvent.click(screen.getByTestId("leave-actions-lv_p"));
    await waitFor(() => expect(screen.queryByTestId("leave-approve-lv_p")).not.toBeNull());
    fireEvent.click(screen.getByTestId("leave-approve-lv_p"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("操作冲突：" + message));
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "leave_reviewed" }));
  });

  it("「批准销假」接口回 409 时显示后端中文原文，不记已审批", async () => {
    stubRejected();
    const { setActionResult, setGlobalError } = renderWithSpies();
    await waitFor(() => expect(screen.queryByTestId("leave-actions-lv_rr")).not.toBeNull());

    fireEvent.click(screen.getByTestId("leave-actions-lv_rr"));
    await waitFor(() => expect(screen.queryByTestId("leave-revoke-approve-lv_rr")).not.toBeNull());
    fireEvent.click(screen.getByTestId("leave-revoke-approve-lv_rr"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("操作冲突：" + message));
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "leave_revoke_reviewed" }));
  });
});
