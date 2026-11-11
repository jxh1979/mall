// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessUsersPage } from "./AccessUsersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。access 域 snake_case 响应。
const USERS = [
  { id: "u-1", username: "张三", account_type: "supervisor", status: "active", phone: "13800000001", organization_id: "org-1", merchant_id: "m-1" },
  { id: "u-2", username: "李四", account_type: "sales", status: "disabled", organization_id: "org-1" },
];
const TEMPLATES = [{ id: "tpl-1", template_name: "基础员工", permission_codes: ["a.b"] }];
const ENTITIES = [{ id: "org-1", name: "总部主体", code: "ORG-1" }];
const MERCHANTS = [{ id: "m-1", display_name: "商户甲" }];

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
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
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
  render(<AccessUsersPage {...fullProps} />);
}

describe("AccessUsersPage 行明细抽屉 + 零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("点员工行 → 抽屉出现且含该行字段值（账号/类型中文/组织显名）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-row-u-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("张三");
    expect(drawer.textContent).toContain("主管");
    expect(drawer.textContent).toContain("总部主体");
    expect(drawer.textContent).toContain("商户甲");
    expect(drawer.textContent).not.toContain("13800000001");
  });

  it("Esc 关闭抽屉 + 点遮罩关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-row-u-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
    fireEvent.click(screen.getByTestId("access-user-row-u-2"));
    fireEvent.click(screen.getByTestId("row-detail-overlay"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("表格状态/账号类型列为中文，不含原英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-users-table")).toBeTruthy());
    const text = screen.getByTestId("access-users-table").textContent ?? "";
    expect(text).toContain("主管");
    expect(text).toContain("销售人员");
    expect(text).toContain("有效");
    expect(text).toContain("已禁用");
    expect(text).not.toContain("supervisor");
    expect(text).not.toContain("active");
    expect(text).not.toContain("disabled");
  });
});

// M038-001（开通方式中文）、M038-009（初始状态下拉与表格同一套用词）。
describe("M038 员工账号页回归", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("账号详情的开通方式为中文：历史账号 / 后台手动录入（001）", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("access/users")) return Promise.resolve(envelope([
        { id: "u-legacy", username: "老账号", account_type: "staff", status: "active", created_via: "legacy" },
        { id: "u-manual", username: "外协甲", account_type: "outsourced_vendor", status: "active", created_via: "manual_admin" },
      ]));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AccessUsersPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-legacy")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-row-u-legacy"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("历史账号");
    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("legacy");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
    fireEvent.click(screen.getByTestId("access-user-row-u-manual"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("后台手动录入");
    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("manual_admin");
  });

  it("新建外部协作账号的初始状态下拉与账号表同一套用词（009）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-create-open"));
    fireEvent.change(screen.getByTestId("access-user-account-type"), { target: { value: "outsourced_vendor" } });
    fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "vendor-a" } });
    fireEvent.change(screen.getByTestId("access-user-password"), { target: { value: "init-pass" } });
    const status = await waitFor(() => screen.getByTestId("access-user-status") as HTMLSelectElement);
    expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "待激活", "已禁用"]);
  });
});
// M038-012：新建外部协作账号时账号已落库、套用权限模板被拒——账号照常入表并关弹窗，提示写清「账号已创建；套用权限模板失败」。
describe("M038-012 员工账号页创建后套模板失败", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("账号 POST 成功、套模板 403：账号只发一笔且入表，弹窗关闭，提示含「账号已创建」与失败原因；套模板不发写死备注（198-R5）", async () => {
    const posts: string[] = [];
    const applyBodies: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(url);
        if (url.includes("/apply")) {
          applyBodies.push(String(init?.body));
          return Promise.resolve(new Response(
            JSON.stringify({ success: false, code: "GRANT_REQUIRES_APPROVAL", message: "以下权限的授予需要审批：[a.b]" }),
            { status: 403, headers: { "content-type": "application/json" } },
          ));
        }
        return Promise.resolve(new Response(
          JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "u-new", username: "vendor-a", account_type: "outsourced_vendor", status: "active", organization_id: "org-1" } }),
          { status: 200, headers: { "content-type": "application/json" } },
        ));
      }
      if (url.includes("permission-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AccessUsersPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-create-open"));
    fireEvent.change(screen.getByTestId("access-user-account-type"), { target: { value: "outsourced_vendor" } });
    fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "vendor-a" } });
    fireEvent.change(screen.getByTestId("access-user-password"), { target: { value: "init-pass" } });
    fireEvent.change(screen.getByTestId("access-user-org"), { target: { value: "org-1" } });
    await waitFor(() => expect((screen.getByTestId("access-user-template") as HTMLSelectElement).value).toBe("tpl-1"));
    fireEvent.click(screen.getByTestId("access-user-flow-next"));
    fireEvent.click(await waitFor(() => screen.getByTestId("access-user-create")));
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-new")).toBeTruthy());
    expect(posts.filter((url) => url.includes("/access/users"))).toHaveLength(1);
    expect(posts.filter((url) => url.includes("/permission-templates/tpl-1/apply"))).toHaveLength(1);
    expect(JSON.parse(applyBodies[0]!)).not.toHaveProperty("operator_note");
    await waitFor(() => expect(screen.queryByTestId("access-user-create")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/创建员工：vendor-a.*账号已创建；套用权限模板 tpl-1 失败：/));
    expect(props.setGlobalError).toHaveBeenLastCalledWith(expect.stringMatching(/\S/));
  });
});
