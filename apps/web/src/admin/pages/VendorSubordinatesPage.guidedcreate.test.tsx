import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorSubordinatesPage } from "./VendorSubordinatesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-10：开通下属账号改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖「开通下属」主创建流；停用/完成情况查看等动作不在本期改造范围。
const SUBORDINATES = [
  { id: "s-1", username: "现场员工甲", partner_org_id: "org-1", account_type: "outsourced_vendor", status: "active" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function raw(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/partner/subordinates")) {
      lastPostUrl = url;
      lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
      return Promise.resolve(raw({ id: "s-new" }));
    }
    if (url.includes("/completion")) return Promise.resolve(envelope([]));
    if (url.includes("/subordinates")) return Promise.resolve(envelope(SUBORDINATES));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "机构管理员", accountType: "outsourced_vendor" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorSubordinatesPage {...props} />);
  return { setActionResult };
}

describe("VendorSubordinatesPage 引导式开通下属(phase-10)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
    window.history.replaceState({}, "", "/admin/vendor/subordinates");
  });

  it("页面默认只留入口按钮，不展示下属登录名输入", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-add-open")).toBeTruthy());
    expect(screen.queryByTestId("vendor-subordinate-username")).toBeNull();
    expect(screen.queryByTestId("vendor-subordinate-add-guided")).toBeNull();
  });

  it("入口→填名→确认→提交：POST 带 username，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-add-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-subordinate-add-open"));

    fireEvent.change(screen.getByTestId("vendor-subordinate-username"), { target: { value: "现场员工丙" } });
    fireEvent.click(screen.getByTestId("vendor-subordinate-add-guided-next")); // 步1→确认
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-add-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-subordinate-add"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/partner/subordinates");
    expect((lastPostBody as { username: string }).username).toBe("现场员工丙");
    await waitFor(() => expect(screen.queryByTestId("vendor-subordinate-add-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "partner_subordinate_created" }));
  });

  it("步内校验拦截：登录名留空则不进确认、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-add-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-subordinate-add-open"));
    fireEvent.change(screen.getByTestId("vendor-subordinate-username"), { target: { value: "  " } });
    fireEvent.click(screen.getByTestId("vendor-subordinate-add-guided-next"));
    expect(screen.getByTestId("vendor-subordinate-add-guided-error").textContent).toContain("登录名");
    expect(screen.queryByTestId("vendor-subordinate-add-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
