import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-7：新增场次改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
const PROJECTS = [{ id: "amuse-1", project_code: "AM-1", project_name: "旋转木马", capacity: 30, status: "active" }];
const SESSIONS = [
  { id: "s-1", project_id: "amuse-1", session_name: "上午场", starts_at: "2026-06-20T10:00:00Z", ends_at: "2026-06-20T11:00:00Z", capacity: 12, used_capacity: 3, locked_capacity: 2, status: "open" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/amusement/sessions")) {
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(envelope([{ id: "s-new" }]));
      }
      if (url.includes("/amusement/projects")) return Promise.resolve(envelope(PROJECTS));
      if (url.includes("/amusement/verifications")) return Promise.resolve(envelope([]));
      if (url.includes("/amusement/sessions")) return Promise.resolve(envelope(SESSIONS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AmusementSessionsPage {...props} />);
  return { setActionResult };
}

describe("AmusementSessionsPage 引导式新增场次(phase-7)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    window.history.replaceState({}, "", "/admin/amusement/sessions");
  });

  it("页面默认不展示弹窗字段，只留入口按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-session-create-open")).toBeTruthy());
    expect(screen.queryByTestId("amusement-session-name")).toBeNull();
    expect(screen.queryByTestId("amusement-session-guided")).toBeNull();
  });

  it("入口→步进→提交：带正确 body（场次名/容量）且成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-session-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-session-create-open"));

    fireEvent.change(screen.getByTestId("amusement-session-name"), { target: { value: "验收场次" } });
    fireEvent.change(screen.getByTestId("amusement-session-capacity"), { target: { value: "20" } });
    fireEvent.click(screen.getByTestId("amusement-session-guided-next"));
    // 确认页出现汇总，点主按钮提交
    await waitFor(() => expect(screen.getByTestId("amusement-session-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-session-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    // 客户端把 body key 转 snake_case 后再发出。
    expect((lastPostBody as { session_name: string }).session_name).toBe("验收场次");
    expect((lastPostBody as { capacity: number }).capacity).toBe(20);
    await waitFor(() => expect(screen.queryByTestId("amusement-session-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "amusement_session_created" }));
  });

  it("步内校验拦截：场次名清空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-session-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-session-create-open"));
    fireEvent.change(screen.getByTestId("amusement-session-name"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("amusement-session-guided-next"));
    expect(screen.getByTestId("amusement-session-guided-error").textContent).toContain("场次名称不能为空");
    expect(screen.queryByTestId("amusement-session-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
