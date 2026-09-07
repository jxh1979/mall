import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OrgHierarchyPage } from "./OrgHierarchyPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

const HIERARCHY = [{ user_id: "usr_a", superior_id: "usr_b", line_code: "finance_review", updated_by: "usr_admin", updated_at: "2026-09-06T02:00:00Z" }];
const LINES = [{ line_code: "finance_review", line_name: "财务审核线", min_start_rank: 6, entry_user_id: "usr_fin_entry", status: "active" }];
const USERS = [{ id: "usr_a", username: "a" }, { id: "usr_b", username: "b" }];

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("/org-hierarchy/") && init?.method === "PUT") return Promise.resolve(raw({ user_id: "usr_a", superior_id: "usr_c", line_code: null }));
    if (url.includes("/approval-lines/") && init?.method === "PUT") return Promise.resolve(raw({ line_code: "legal_review", line_name: "法务审核线", min_start_rank: 3, entry_user_id: null, status: "active" }));
    if (url.includes("/org-hierarchy")) return Promise.resolve(raw(HIERARCHY));
    if (url.includes("/approval-lines")) return Promise.resolve(raw(LINES));
    if (url.includes("/access/users")) return Promise.resolve(raw(USERS));
    return Promise.resolve(raw([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["access.user.view"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<OrgHierarchyPage {...props} />);
  return props;
}

// M087-A-01 / A-03（PGM-1.7.1 组织层级「直接上级」与平行审批线「最低起点层级」）
describe("OrgHierarchyPage 组织层级与审批线维护", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("展示直接上级表与审批线表；保存直接上级提交 superior_id 且结果为中文句子", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-row-usr_a")).toBeTruthy());
    expect(screen.getByTestId("org-hierarchy-row-usr_a").textContent).toContain("usr_b");
    expect(screen.getByTestId("approval-line-row-finance_review").textContent).toContain("财务审核线");
    expect(screen.getByTestId("approval-line-row-finance_review").textContent).toContain("启用");

    fireEvent.change(screen.getByTestId("org-hierarchy-superior"), { target: { value: "usr_c" } });
    fireEvent.click(screen.getByTestId("org-hierarchy-save"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input, init]) => String(input).includes("/org-hierarchy/usr_a") && (init as RequestInit | undefined)?.method === "PUT")).toBe(true));
    const call = fetchMock.mock.calls.find(([input, init]) => String(input).includes("/org-hierarchy/usr_a") && (init as RequestInit | undefined)?.method === "PUT");
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.superior_id).toBe("usr_c");
    expect(body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("已把 usr_a 的直接上级设为 usr_c"));
  });

  it("直接上级不能是本人；最低起点层级越界在页面即拒绝", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("org-hierarchy-superior"), { target: { value: "usr_a" } });
    fireEvent.click(screen.getByTestId("org-hierarchy-save"));
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-error").textContent).toContain("直接上级不能是本人"));

    fireEvent.change(screen.getByTestId("approval-line-code"), { target: { value: "legal_review" } });
    fireEvent.change(screen.getByTestId("approval-line-name"), { target: { value: "法务审核线" } });
    fireEvent.change(screen.getByTestId("approval-line-rank"), { target: { value: "31" } });
    fireEvent.click(screen.getByTestId("approval-line-save"));
    await waitFor(() => expect(screen.getByTestId("org-hierarchy-error").textContent).toContain("1 到 30"));
  });

  it("保存审批线提交 min_start_rank 与 status；只有 view 权限时保存按钮不可用", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("approval-line-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("approval-line-code"), { target: { value: "legal_review" } });
    fireEvent.change(screen.getByTestId("approval-line-name"), { target: { value: "法务审核线" } });
    fireEvent.change(screen.getByTestId("approval-line-rank"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("approval-line-save"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/approval-lines/legal_review"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/approval-lines/legal_review"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.min_start_rank).toBe(3);
    expect(body.status).toBe("active");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^已保存平行审批线 legal_review/)));

    vi.unstubAllGlobals();
    renderPage(VIEW_ONLY);
    await waitFor(() => expect(screen.getAllByTestId("org-hierarchy-save").length).toBeGreaterThan(0));
    const buttons = screen.getAllByTestId("org-hierarchy-save") as HTMLButtonElement[];
    expect(buttons[buttons.length - 1]?.disabled).toBe(true);
  });
});
