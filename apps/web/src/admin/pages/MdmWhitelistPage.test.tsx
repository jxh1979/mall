// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MdmWhitelistPage } from "./MdmWhitelistPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// ROOT-05 R5（PGM「工作手机预装软件清单和网站白名单由后台维护」）：后台维护页走真实 AdminApi，断言线上蛇形请求体。
const APPS = [{ id: "mdm_app_1", package_name: "com.tencent.wework", app_label: "企业微信", scope_type: "global", scope_id: "global", enabled: true, reason: null, created_at: "2026-10-01T00:00:00Z", updated_at: "2026-10-01T00:00:00Z" }];
const SITES = [{ id: "mdm_site_1", domain: "work.weixin.qq.com", description: "企业微信网页", scope_type: "project", scope_id: "proj_1", enabled: true, reason: null, created_at: "2026-10-01T00:00:00Z", updated_at: "2026-10-01T00:00:00Z" }];

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function stubFetch() {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "PUT") return Promise.resolve(ok({ ...JSON.parse(String(init.body)), id: url.split("/").pop() }));
    if (url.includes("/app-whitelist")) return Promise.resolve(ok({ items: APPS, page: 1, page_size: 50, total: 1 }));
    if (url.includes("/site-whitelist")) return Promise.resolve(ok({ items: SITES, page: 1, page_size: 50, total: 1 }));
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function putCalls(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.filter((call) => (call[1] as RequestInit | undefined)?.method === "PUT");
}

function renderPage(effectivePermissions: EffectivePermissionsState) {
  const props: AdminPageProps = {
    api: new AdminApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<MdmWhitelistPage {...props} />);
}

const MANAGER: EffectivePermissionsState = { status: "ready", value: { grants: ["mdm.policy.manage"], denies: [], wildcard: false } };
const VIEWER: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: false } };

describe("MdmWhitelistPage 工作手机白名单维护", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("listsAppAndSiteWhitelistFromBackend", async () => {
    stubFetch();
    renderPage(MANAGER);
    await waitFor(() => expect(screen.queryByTestId("mdm-app-whitelist-table")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("mdm-app-whitelist-table").textContent).toContain("com.tencent.wework"));
    expect(screen.getByTestId("mdm-app-whitelist-table").textContent).toContain("全局");
    expect(screen.getByTestId("mdm-site-whitelist-table").textContent).toContain("work.weixin.qq.com");
    expect(screen.getByTestId("mdm-site-whitelist-table").textContent).toContain("综合体");
  });

  it("createAppEntrySubmitsPutWithSnakeCaseBody", async () => {
    const fetchMock = stubFetch();
    renderPage(MANAGER);
    await waitFor(() => expect(screen.queryByTestId("mdm-app-create-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("mdm-app-create-open"));
    fireEvent.change(screen.getByTestId("mdm-app-package-name"), { target: { value: "com.example.pos" } });
    fireEvent.change(screen.getByTestId("mdm-app-label"), { target: { value: "收银" } });
    fireEvent.click(screen.getByTestId("mdm-app-guided-next"));
    fireEvent.click(screen.getByTestId("mdm-app-save"));
    await waitFor(() => expect(putCalls(fetchMock)).toHaveLength(1));
    const [url, init] = putCalls(fetchMock)[0] as [string, RequestInit];
    expect(url).toMatch(/\/api\/v1\/terminals\/mdm\/app-whitelist\/mdm_app_[0-9a-f]{32}$/);
    const body = JSON.parse(String(init.body));
    expect(body).toMatchObject({ package_name: "com.example.pos", app_label: "收银", scope_type: "global", enabled: true });
    expect(body.idempotency_key).toEqual(expect.any(String));
  });

  it("disableEntrySubmitsEnabledFalse", async () => {
    const fetchMock = stubFetch();
    renderPage(MANAGER);
    await waitFor(() => expect(screen.queryByTestId("mdm-site-toggle-mdm_site_1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("mdm-site-toggle-mdm_site_1"));
    await waitFor(() => expect(putCalls(fetchMock)).toHaveLength(1));
    const [url, init] = putCalls(fetchMock)[0] as [string, RequestInit];
    expect(url).toMatch(/\/api\/v1\/terminals\/mdm\/site-whitelist\/mdm_site_1$/);
    expect(JSON.parse(String(init.body))).toMatchObject({ domain: "work.weixin.qq.com", scope_type: "project", scope_id: "proj_1", enabled: false });
  });

  it("buttonsDisabledWithoutMdmPolicyManage", async () => {
    stubFetch();
    renderPage(VIEWER);
    await waitFor(() => expect(screen.queryByTestId("mdm-app-toggle-mdm_app_1")).not.toBeNull());
    expect(screen.getByTestId("mdm-app-toggle-mdm_app_1")).toBeDisabled();
    expect(screen.getByTestId("mdm-app-create-open")).toBeDisabled();
    expect(screen.getByTestId("mdm-site-create-open")).toBeDisabled();
  });
});
