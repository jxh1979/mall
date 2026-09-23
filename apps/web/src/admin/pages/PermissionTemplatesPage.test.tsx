import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PermissionTemplatesPage } from "./PermissionTemplatesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const TEMPLATES = [
  { id: "tpl-1", template_name: "基础员工", template_type: "base", status: "active", permission_codes: ["access.user.manage", "finance.view"] },
  { id: "tpl-2", template_name: "临时提权", template_type: "temporary", status: "inactive", valid_until: "2026-08-01T00:00:00Z", single_use: true, permission_codes: ["access.role.assign"] },
];
const PERMISSIONS = [
  { permission_code: "access.user.manage", permission_name: "管理员工", domain: "access" },
  { permission_code: "finance.view", permission_name: "查看财务", domain: "finance" },
];
const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("permission-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<PermissionTemplatesPage {...fullProps} />);
}

describe("PermissionTemplatesPage 行明细抽屉 + 零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("点模板行 → 抽屉出现且含该模板字段（类型中文/有效性/权限点）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-row-tpl-2"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("临时提权");
    expect(drawer.textContent).toContain("临时模板");
    expect(drawer.textContent).toContain("单次有效");
    expect(drawer.textContent).toContain("access.role.assign");
    expect(drawer.textContent).not.toContain("temporary");
  });

  it("点模板行第一格单选不误触抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-select-tpl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-select-tpl-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-row-tpl-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("模板表类型/状态列为中文，不含原英文枚举 base/temporary/inactive", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-templates-table")).toBeTruthy());
    const text = screen.getByTestId("permission-templates-table").textContent ?? "";
    expect(text).toContain("基础模板");
    expect(text).toContain("临时模板");
    expect(text).toContain("有效");
    expect(text).toContain("未激活");
    expect(text).not.toContain("temporary");
    expect(text).not.toContain("inactive");
  });
});

// M038-001（模板类型 role 中文）、M038-006（编辑保存的时间往返）。
describe("M038 权限模板页回归", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  const TIMED = [
    { id: "tpl-role", template_name: "平台管理员模板", template_type: "role", status: "active", permission_codes: ["access.user.manage"],
      valid_from: "2026-06-14T00:00:00Z", valid_until: "2026-06-15T00:00:00Z" },
  ];

  it("库内类型 role 显示为「角色模板」，编辑下拉也有这一项（001）", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("permission-templates")) return Promise.resolve(envelope(TIMED));
      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<PermissionTemplatesPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-role")).toBeTruthy());
    const cells = screen.getByTestId("permission-template-row-tpl-role").querySelectorAll("td");
    expect(cells[2]?.textContent).toBe("角色模板");
    const editType = screen.getByTestId("permission-template-edit-type") as HTMLSelectElement;
    await waitFor(() => expect(editType.value).toBe("role"));
    expect(Array.from(editType.options).map((option) => option.textContent)).toContain("角色模板");
  });

  it("北京时间下选中带生效/截止时间的模板原样保存，发出的仍是同一时刻（006）", async () => {
    vi.stubEnv("TZ", "Asia/Shanghai");
    expect(new Date("2026-06-14T00:00:00Z").getHours()).toBe(8); // 时区确已生效，否则本用例无意义
    let putBody: Record<string, unknown> | null = null;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "PUT" && url.includes("/permission-templates/tpl-role")) {
        putBody = JSON.parse(String(init?.body));
        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "tpl-role" } }),
          { status: 200, headers: { "content-type": "application/json" } }));
      }
      if (url.includes("permission-templates")) return Promise.resolve(envelope(TIMED));
      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<PermissionTemplatesPage {...props} />);
    await waitFor(() => expect((screen.getByTestId("permission-template-edit-valid-from") as HTMLInputElement).value).toBe("2026-06-14T08:00"));
    expect((screen.getByTestId("permission-template-edit-valid-until") as HTMLInputElement).value).toBe("2026-06-15T08:00");
    fireEvent.click(screen.getByTestId("permission-template-update"));
    await waitFor(() => expect(putBody).toBeTruthy());
    expect(new Date(String(putBody!.valid_from)).getTime()).toBe(new Date("2026-06-14T00:00:00Z").getTime());
    expect(new Date(String(putBody!.valid_until)).getTime()).toBe(new Date("2026-06-15T00:00:00Z").getTime());
  });
});

// M038-011：复制模板、应用模板提交未完成时再次点击不发第二笔，按钮置为禁用。
describe("M038-011 权限模板页复制/应用防连点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function renderPending() {
    const posts: string[] = [];
    let release: (value: Response) => void = () => undefined;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(url);
        return new Promise<Response>((resolve) => { release = resolve; });
      }
      if (url.includes("permission-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<PermissionTemplatesPage {...props} />);
    const ok = () => release(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "tpl-copy" } }),
      { status: 200, headers: { "content-type": "application/json" } }));
    return { posts, ok };
  }

  it("连点两次「复制模板」只发一笔，提交中按钮禁用，完成后恢复", async () => {
    const { posts, ok } = renderPending();
    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("permission-template-copy"));
    fireEvent.click(screen.getByTestId("permission-template-copy"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]).toContain("/permission-templates/tpl-1/copy");
    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(true));
    expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(true);
    ok();
    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(false));
    expect(posts.filter((url) => url.includes("/copy"))).toHaveLength(1);
  });

  it("连点两次「应用模板」并都确认，只发一笔", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { posts, ok } = renderPending();
    await waitFor(() => expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("permission-template-apply"));
    fireEvent.click(screen.getByTestId("permission-template-apply"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]).toContain("/permission-templates/tpl-1/apply");
    await waitFor(() => expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(true));
    ok();
    await waitFor(() => expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(false));
    expect(posts.filter((url) => url.includes("/apply"))).toHaveLength(1);
  });
});
