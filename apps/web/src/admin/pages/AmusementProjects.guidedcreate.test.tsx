import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-8：新建项目改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
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
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/amusement/projects")) {
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(envelope([{ id: "p-new" }]));
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

describe("AmusementSessionsPage 引导式新建项目(phase-8)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    window.history.replaceState({}, "", "/admin/amusement/sessions");
  });

  it("页面默认不展示项目弹窗字段，只留入口按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-project-create-open")).toBeTruthy());
    expect(screen.queryByTestId("amusement-project-name")).toBeNull();
    expect(screen.queryByTestId("amusement-project-guided")).toBeNull();
  });

  it("入口→步进→提交：带正确 body（项目名/容量）且成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-project-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-project-create-open"));

    fireEvent.change(screen.getByTestId("amusement-project-name"), { target: { value: "新星球大冒险" } });
    fireEvent.change(screen.getByTestId("amusement-project-capacity"), { target: { value: "45" } });
    fireEvent.click(screen.getByTestId("amusement-project-guided-next"));
    await waitFor(() => expect(screen.getByTestId("amusement-project-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-project-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect((lastPostBody as { project_name: string }).project_name).toBe("新星球大冒险");
    expect((lastPostBody as { capacity: number }).capacity).toBe(45);
    await waitFor(() => expect(screen.queryByTestId("amusement-project-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "amusement_project_created" }));
  });

  it("步内校验拦截：项目名清空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-project-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-project-create-open"));
    fireEvent.change(screen.getByTestId("amusement-project-name"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("amusement-project-guided-next"));
    expect(screen.getByTestId("amusement-project-guided-error").textContent).toContain("项目名称不能为空");
    expect(screen.queryByTestId("amusement-project-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
