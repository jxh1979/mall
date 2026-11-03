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

// M038-001（模板类型中文；M001-F20 起模板只有基础模板（角色级）与临时模板两种）、M038-006（编辑保存的时间往返）。
describe("M038 权限模板页回归", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  const TIMED = [
    { id: "tpl-role", template_name: "平台管理员模板", template_type: "base", status: "active", permission_codes: ["access.user.manage"],
      valid_from: "2026-06-14T00:00:00Z", valid_until: "2026-06-15T00:00:00Z" },
  ];

  it("基础模板显示为「基础模板（角色级）」，两个类型下拉只有基础模板（角色级）与临时模板（001 / M001-F20）", async () => {
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
    expect(cells[2]?.textContent).toBe("基础模板（角色级）");
    const editType = screen.getByTestId("permission-template-edit-type") as HTMLSelectElement;
    await waitFor(() => expect(editType.value).toBe("base"));
    expect(Array.from(editType.options).map((option) => option.value)).toEqual(["base", "temporary"]);
    expect(Array.from(editType.options).map((option) => option.textContent)).toEqual(["基础模板（角色级）", "临时模板"]);
    fireEvent.click(screen.getByTestId("permission-template-create-open"));
    const createType = screen.getByTestId("permission-template-type") as HTMLSelectElement;
    expect(Array.from(createType.options).map((option) => option.value)).toEqual(["base", "temporary"]);
    expect(Array.from(createType.options).map((option) => option.textContent)).toEqual(["基础模板（角色级）", "临时模板"]);
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
    expect(putBody).not.toHaveProperty("data_scope"); // M001-F25：修改不写死数据范围
  });

  // M001-F18（控制席 2026-10-02 裁定）：输入框只到分钟；选中带秒的模板、不改时间直接保存，请求里的生效 / 截止与原值逐字一致，
  // 不因按分钟回填而截短（截短会把生效判为放宽而 409、把截止判为收窄而给在用员工写无实义的改前改后留痕）。
  it("选中带秒的模板、不改时间保存，请求里的生效与截止时间与原值逐字一致（M001-F18）", async () => {
    vi.stubEnv("TZ", "Asia/Shanghai");
    const seconds = [{ ...TIMED[0], id: "tpl-seconds", valid_from: "2026-06-14T00:00:30Z", valid_until: "2026-06-15T00:00:45Z" }];
    let putBody: Record<string, unknown> | null = null;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "PUT" && url.includes("/permission-templates/tpl-seconds")) {
        putBody = JSON.parse(String(init?.body));
        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "tpl-seconds" } }),
          { status: 200, headers: { "content-type": "application/json" } }));
      }
      if (url.includes("permission-templates")) return Promise.resolve(envelope(seconds));
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
    expect(putBody!.valid_from).toBe("2026-06-14T00:00:30Z");
    expect(putBody!.valid_until).toBe("2026-06-15T00:00:45Z");
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
    const bodies: string[] = [];
    let release: (value: Response) => void = () => undefined;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(url);
        bodies.push(String(init?.body ?? ""));
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
    return { posts, bodies, ok };
  }

  it("连点两次「复制模板」只发一笔，提交中按钮禁用，完成后恢复；复制请求不带模板类型（M001-F19/F20：复制出的是新模板，未填项沿用源模板）", async () => {
    const { posts, bodies, ok } = renderPending();
    await waitFor(() => expect((screen.getByTestId("permission-template-copy") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("permission-template-copy"));
    fireEvent.click(screen.getByTestId("permission-template-copy"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]).toContain("/permission-templates/tpl-1/copy");
    expect(bodies[0]).not.toMatch(/template_?type/i);
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

  // M001-F21：套用方式只用契约闭集里的取值——不勾单次发 append，勾选单次发 single_use。
  it("应用模板发出的套用方式在契约闭集内：默认 append，勾选单次为 single_use（M001-F21）", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const { bodies, ok } = renderPending();
    // 第 125 条：表单回填在被动 effect 里，等回填值出现（或复位）后再操作，否则补跑的 effect 会把勾选冲掉。
    // 先等首次回填（模板名回填为「基础员工」）再勾「单次有效」。
    await waitFor(() => {
      expect((screen.getByTestId("permission-template-edit-name") as HTMLInputElement).value).toBe("基础员工");
      expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(false);
    }, { timeout: 5000 });
    fireEvent.click(screen.getByTestId("permission-template-edit-single-use"));
    fireEvent.click(screen.getByTestId("permission-template-apply"));
    await waitFor(() => expect(bodies).toHaveLength(1));
    expect(JSON.parse(bodies[0]!).mode).toBe("single_use");
    ok();
    // 应用后重读，回填 effect 按模板 tpl-1（非单次）把勾选复位；复位即说明重读后的回填已执行，再点第二笔。
    await waitFor(() => {
      expect((screen.getByTestId("permission-template-edit-single-use") as HTMLInputElement).checked).toBe(false);
      expect((screen.getByTestId("permission-template-apply") as HTMLButtonElement).disabled).toBe(false);
    }, { timeout: 5000 });
    fireEvent.click(screen.getByTestId("permission-template-apply"));
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(JSON.parse(bodies[1]!).mode).toBe("append");
    expect(bodies.map((body) => JSON.parse(body).mode).every((mode: string) => ["append", "merge", "single_use"].includes(mode))).toBe(true);
    ok();
  });
});
