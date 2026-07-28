import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// V341（v7 审计 R-12）：游乐项目↔权益绑定由物理删改软删。
// 前端契约：列表默认不显示已归档；「查看已归档」带出已存档/已删除行并给「恢复」入口；停用（存档）强制填理由。

const PROJECTS = [
  { id: "amuse-1", project_code: "AM-1", project_name: "旋转木马", capacity: 30, status: "active" },
];

const ACTIVE_ENTITLEMENTS = [
  { id: "ament_1", project_id: "amuse-1", entitlement_type: "amusement_pass", created_at: "2026-07-01T00:00:00Z", delete_status: "none" },
];

const ARCHIVED_ENTITLEMENTS = [
  ...ACTIVE_ENTITLEMENTS,
  {
    id: "ament_2", project_id: "amuse-1", entitlement_type: "amusement_single",
    created_at: "2026-07-01T00:00:00Z", archived_at: "2026-07-20T00:00:00Z",
    archive_reason: "赛季结束下线", delete_status: "approved", delete_reason: "配错不再用",
  },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

type Call = { url: string; method: string; body: Record<string, unknown> | null };

function makeApi(calls: Call[]): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = String(init?.method ?? "GET").toUpperCase();
      calls.push({ url, method, body: init?.body ? JSON.parse(String(init.body)) : null });
      if (url.includes("/entitlements")) {
        return Promise.resolve(envelope(url.includes("include_archived=true") ? ARCHIVED_ENTITLEMENTS : ACTIVE_ENTITLEMENTS));
      }
      if (url.includes("/amusement/projects")) return Promise.resolve(envelope(PROJECTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(calls: Call[], setGlobalError = vi.fn()) {
  const fullProps: AdminPageProps = {
    api: makeApi(calls),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AmusementSessionsPage {...fullProps} />);
}

describe("AmusementSessionsPage 权益绑定软删（V341 / R-12）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/amusement/sessions");
  });

  it("默认只读生效中绑定；勾选「查看已归档」后带 include_archived=true 并显示已删除行与「恢复」入口", async () => {
    const calls: Call[] = [];
    renderPage(calls);
    await waitFor(() => expect(screen.getByTestId("amusement-entitlement-row-amusement_pass")).toBeTruthy());

    // 默认视图：请求不带 include_archived，已归档行不显示。
    const firstRead = calls.find((c) => c.url.includes("/entitlements"));
    expect(firstRead?.url).not.toContain("include_archived");
    expect(screen.queryByTestId("amusement-entitlement-row-amusement_single")).toBeNull();

    fireEvent.click(screen.getByTestId("amusement-entitlement-show-archived"));
    await waitFor(() => expect(screen.getByTestId("amusement-entitlement-row-amusement_single")).toBeTruthy());
    expect(calls.some((c) => c.url.includes("include_archived=true"))).toBe(true);
    const archivedRow = screen.getByTestId("amusement-entitlement-row-amusement_single");
    expect(archivedRow.textContent).toContain("已删除（可恢复）");
    expect(archivedRow.textContent).toContain("赛季结束下线");
    expect(screen.getByTestId("amusement-entitlement-restore-amusement_single")).toBeTruthy();
  });

  it("停用（存档）理由为空 → 报错且不发请求；填了理由 → POST /archive 带 reason", async () => {
    const calls: Call[] = [];
    const setGlobalError = vi.fn();
    renderPage(calls, setGlobalError);
    await waitFor(() => expect(screen.getByTestId("amusement-entitlement-archive-amusement_pass")).toBeTruthy());

    vi.spyOn(window, "prompt").mockReturnValueOnce("   ");
    fireEvent.click(screen.getByTestId("amusement-entitlement-archive-amusement_pass"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("存档理由不能为空"));
    expect(calls.some((c) => c.method === "POST" && c.url.includes("/archive"))).toBe(false);

    vi.spyOn(window, "prompt").mockReturnValueOnce("配错权益类型");
    fireEvent.click(screen.getByTestId("amusement-entitlement-archive-amusement_pass"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.includes("/entitlements/amusement_pass/archive"))).toBe(true));
    const archiveCall = calls.find((c) => c.method === "POST" && c.url.includes("/archive"));
    expect(archiveCall?.body?.reason).toBe("配错权益类型");
  });

  it("已删除（墓碑）行点「恢复」→ POST /unarchive（数据可恢复，不是物理删）", async () => {
    const calls: Call[] = [];
    renderPage(calls);
    await waitFor(() => expect(screen.getByTestId("amusement-entitlement-show-archived")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-entitlement-show-archived"));
    await waitFor(() => expect(screen.getByTestId("amusement-entitlement-restore-amusement_single")).toBeTruthy());

    fireEvent.click(screen.getByTestId("amusement-entitlement-restore-amusement_single"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.includes("/entitlements/amusement_single/unarchive"))).toBe(true));
  });
});
