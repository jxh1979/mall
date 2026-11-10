// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps } from "../types";

describe("转台明确目标当班服务人员", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });
  it("未指定接手人不提交，填写后随真实场次版本提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn(async (url: string) => url === "/api/v1/bar/table-sessions"
        ? [{ id: "session-1", tableId: "old-table", barId: "bar-1", status: "open", sessionVersion: 3, serviceStaffId: "old-staff" }]
        : []),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = { api, user: { id: "operator", username: "主管", accountType: "staff" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarTableSessionsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("bar-session-select-session-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-session-reason"), { target: { value: "客人换台" } });
    fireEvent.change(screen.getByTestId("bar-transfer-target-table"), { target: { value: "next-table" } });
    fireEvent.click(screen.getByTestId("bar-transfer-submit"));
    expect(screen.getByTestId("bar-checkin-error").textContent).toContain("实际当班接手人");
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("bar-transfer-target-staff"), { target: { value: "next-staff" } });
    fireEvent.click(screen.getByTestId("bar-transfer-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/session-1/transfer",
      expect.objectContaining({ targetTableId: "next-table", targetServiceStaffId: "next-staff", sessionVersion: 3 })));
  });

  // 第十八批 AB 段第 190 条（控制席并入；BSM:1079「每次拆单、合单、转台、并台、拆台和撤销转台均须留痕。记录内容包括操作类型、原因、操作人、时间……」）：
  // 转/合/拆原因框不预填「主管审批通过」，不填原因不发转台请求。
  it("转/合/拆原因默认为空，不填原因不发转台请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn(async (url: string) => url === "/api/v1/bar/table-sessions"
        ? [{ id: "session-1", tableId: "old-table", barId: "bar-1", status: "open", sessionVersion: 3 }]
        : []),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = { api, user: { id: "operator", username: "主管", accountType: "staff" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarTableSessionsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("bar-session-select-session-1")).toBeTruthy());
    expect((screen.getByTestId("bar-session-reason") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-transfer-target-table"), { target: { value: "next-table" } });
    fireEvent.change(screen.getByTestId("bar-transfer-target-staff"), { target: { value: "next-staff" } });
    fireEvent.click(screen.getByTestId("bar-transfer-submit"));
    expect(screen.getByTestId("bar-checkin-error").textContent).toContain("必须填写原因");
    expect(post).not.toHaveBeenCalled();
  });

  // 第十八批 AB 段第 190 条 190-R2（BSM:1079「达到上限后须主管审批才能继续转台」）：转台确认提示超限须主管审批；
  // 转台被 409 拦下（已提交超限转台申请）时页面显示后端提示，含审批单号。
  it("转台达到次数上限被拦时显示含审批单号的提示", async () => {
    const message = "本会话已转台 1 次，达到本酒吧上限 1 次，须主管审批后才能继续转台，已提交审批，审批单号：review_190r2";
    const envelope = (data: unknown) => new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
    const list = (items: unknown[]) => envelope({ items, page: 1, page_size: 50, total: items.length });
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        return Promise.resolve(new Response(JSON.stringify({ success: false, code: "STATE_CONFLICT", detail_code: "BAR_TRANSFER_LIMIT_APPROVAL_REQUIRED", message }),
          { status: 409, headers: { "content-type": "application/json" } }));
      }
      if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
      if (url.includes("/bar/table-sessions")) {
        return Promise.resolve(list([{ id: "session-1", table_id: "old-table", bar_id: "bar-1", status: "open", session_version: 3 }]));
      }
      return Promise.resolve(list([]));
    }));
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "店员", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    render(<BarTableSessionsPage {...props} />);
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByTestId("bar-session-select-session-1")).not.toBeNull(), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-session-reason"), { target: { value: "客人换台" } });
    fireEvent.change(screen.getByTestId("bar-transfer-target-table"), { target: { value: "next-table" } });
    fireEvent.change(screen.getByTestId("bar-transfer-target-staff"), { target: { value: "next-staff" } });
    fireEvent.click(screen.getByTestId("bar-transfer-submit"));
    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("达到本酒吧转台次数上限时须主管审批"));
    await waitFor(() => expect(screen.getByTestId("bar-checkin-error").textContent).toContain("审批单号：review_190r2"), { timeout: 5000 });
  });
});
