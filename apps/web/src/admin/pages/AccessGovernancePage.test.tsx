import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { AccessGovernancePage } from "./AccessGovernancePage";
import AdminApp from "../AdminApp";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, AdminUser, EffectivePermissions, EffectivePermissionsState } from "../types";

const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];
const ROLES = [{ id: "role-1", role_name: "运营经理", role_code: "ops", status: "active" }];
const TEMPLATES = [
  { id: "prt-1", position_code: "ops_manager", staff_level_id: "lvl_m1", role_id: "role-1", template_id: "tpl-1", organization_id: "org-1", status: "active" },
  { id: "prt-2", position_code: "ops_manager", staff_level_id: "lvl_m5_middle", role_id: "role-1", template_id: "tpl-1", organization_id: "org-1", status: "active" },
  { id: "prt-3", position_code: "ops_manager", staff_level_id: "lvl_m9_upper", role_id: "role-1", template_id: "tpl-1", organization_id: "org-1", status: "active" },
];
const GRANTS = [
  { id: "gr-1", request_type: "temporary_permission", target_user_id: "u-1", permission_codes: ["access.role.assign"], status: "pending", single_use: true, expires_at: "2026-08-01T10:00:00Z", reason: "临时救火", created_at: "2026-07-05T09:00:00Z" },
];
const SUMMARY = {
  position_recommendations: [{ position_code: "ops_manager", staff_level_id: "lvl_m1", permission_code: "access.role.assign", request_count: 3, recommendation: "建议固化进岗位模板" }],
  user_anomalies: [{ user_id: "u-1", request_count: 5, summary: "临时授权过于频繁" }],
};

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

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
      if (url.includes("grant-requests")) return Promise.resolve(envelope(GRANTS));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
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
  render(<AccessGovernancePage {...fullProps} />);
  return fullProps;
}

// M006-G-09（PGM-1.18「提示……必须全中文」）：主壳对对象型结果原样 JSON 上屏，本模块三个页面交出的结果只能是中文句子，
// 不得交对象字面量，也不得含 snake_case 英文动作码（如 partner_org_user_bound）。
describe("M006-G-09 结果提示全中文", () => {
  it("治理页 / 账号页 / 权限模板页交给主壳的结果都是中文句子", () => {
    // 用 Vite 的 ?raw 读取页面源码（web tsconfig 无 node 类型；vite/client 已声明 import.meta.glob）。
    const sources = import.meta.glob<string>("./{AccessGovernancePage,AccessUsersPage,PermissionTemplatesPage}.tsx", { query: "?raw", import: "default", eager: true });
    expect(Object.keys(sources)).toHaveLength(3);
    for (const [page, source] of Object.entries(sources)) {
      const calls = source.split("\n").filter((line: string) => line.includes("setActionResult("));
      expect(calls.length, page).toBeGreaterThan(0);
      for (const line of calls) {
        expect(line, `${page}: ${line.trim()}`).not.toMatch(/setActionResult\(\{/);
        expect(line, `${page}: ${line.trim()}`).not.toMatch(/"[a-z]+(?:_[a-z]+)+"/);
      }
    }
  });
});

describe("AccessGovernancePage 行明细抽屉 + 零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("职级三十档：上 / 中 / 下三档在岗位模板表全部中文化（ENUM-1.9）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-2")).toBeTruthy());
    expect(screen.getByTestId("access-position-role-template-prt-2").textContent).toContain("M5·中");
    expect(screen.getByTestId("access-position-role-template-prt-2").textContent).not.toContain("middle");
    expect(screen.getByTestId("access-position-role-template-prt-3").textContent).toContain("M9·上");
  });

  it("员工单独授予 / 显式拒绝有写入口：显式拒绝按目标用户提交（PGM-1.10.1）", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-override-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("覆盖用户"), { target: { value: "u-1" } });
    fireEvent.change(screen.getByLabelText("覆盖权限码"), { target: { value: "access.role.assign" } });
    fireEvent.change(screen.getByLabelText("覆盖效果"), { target: { value: "deny" } });
    fireEvent.click(screen.getByTestId("access-user-override-set"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/api/v1/access/users/u-1/permission-overrides"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/permission-overrides"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.effect).toBe("deny");
    expect(body.permission_codes).toEqual(["access.role.assign"]);
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^已设置用户 u-1 的显式拒绝/)));
  });

  it("点岗位模板行 → 抽屉出现且含岗位/角色（角色中文名）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-prt-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).not.toContain("ops_manager"); // 岗位码已中文化
    expect(drawer.textContent).toContain("运营经理");
    expect(drawer.textContent).toContain("有效");
    expect(drawer.textContent).not.toContain("active");
  });

  it("点授权申请行 → 抽屉含状态中文与中文过期时间", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-gr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-gr-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("临时权限");
    expect(drawer.textContent).toContain("待处理");
    expect(drawer.textContent).toContain("2026年8月1日");
    expect(drawer.textContent).not.toContain("pending");
  });

  it("点智能总结行 → 抽屉含岗位建议内容", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-summary-position-ops_manager")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-summary-position-ops_manager"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("岗位");
    expect(drawer.textContent).toContain("建议固化进岗位模板");
  });

  it("点操作按钮不误触抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-actions-gr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-actions-gr-1"));
    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-approve-gr-1"));
    // 审批会触发 reload；抽屉不应因点操作按钮弹出。
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-prt-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("授权申请表状态/类型为中文，审批按钮为中文，不含原英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-requests-table")).toBeTruthy());
    const text = screen.getByTestId("access-grant-requests-table").textContent ?? "";
    expect(text).toContain("临时权限");
    expect(text).toContain("待处理");
    fireEvent.click(screen.getByTestId("access-grant-actions-gr-1"));
    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-1")).toBeTruthy());
    expect(screen.getByTestId("access-grant-approve-gr-1").textContent).toContain("通过");
    expect(screen.getByTestId("access-grant-reject-gr-1").textContent).toContain("驳回");
    expect(text).not.toContain("pending");
    expect(text).not.toContain("approve");
    expect(text).not.toContain("reject");
  });

  it("智能总结表首列为中文（岗位/用户），不含 position/user 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-summary-table")).toBeTruthy());
    const text = screen.getByTestId("access-grant-summary-table").textContent ?? "";
    expect(text).toContain("岗位");
    expect(text).toContain("用户");
    expect(text).not.toContain("position");
    expect(text).not.toMatch(/\buser\b/);
  });
});

// M038 回归：取值全中文（001）、未登记岗位（003）、恢复文案（004）、写动作失败提示与防双击（005）、表头列数（007）、职级 30 档下拉（008）、状态用词（009）。
describe("M038 访问治理页回归", () => {
  afterEach(() => vi.unstubAllGlobals());

  const M038_GRANTS = [
    { id: "gr-a", request_type: "user_scope_change", target_user_id: "u-1", status: "applied" },
    { id: "gr-b", request_type: "position_high_risk_release", target_user_id: "u-1", status: "pending" },
    { id: "gr-c", request_type: "role_permission", target_user_id: "u-1", status: "rejected" },
    // M002-F01：员工单独授予 / 解除员工显式拒绝两类申请同样中文上屏。
    { id: "gr-d", request_type: "user_permission_grant", target_user_id: "u-1", status: "pending" },
    { id: "gr-e", request_type: "user_permission_deny_removal", target_user_id: "u-1", status: "applied" },
  ];
  const M038_TEMPLATES = [
    { id: "prt-x", position_code: "front_desk", staff_level_id: "lvl_m2_upper", role_id: "role-1", status: "active" },
    { id: "prt-arch", position_code: "ops_manager", staff_level_id: "lvl_m1_lower", role_id: "role-1", status: "inactive", archived_at: "2026-08-01T00:00:00Z" },
    { id: "prt-del", position_code: "ops_manager", staff_level_id: "lvl_m1_lower", role_id: "role-1", status: "inactive", archived_at: "2026-08-01T00:00:00Z", deleted_at: "2026-08-02T00:00:00Z", delete_review_id: "rv-1" },
  ];
  type Write = { url: string; body: Record<string, unknown> };

  function renderM038(onWrite: (write: Write) => Promise<Response>) {
    const writes: Write[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() !== "GET") {
        const write = { url, body: init?.body ? JSON.parse(String(init.body)) : {} };
        writes.push(write);
        return onWrite(write);
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
      if (url.includes("grant-requests")) return Promise.resolve(envelope(M038_GRANTS));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(M038_TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
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
    render(<AccessGovernancePage {...props} />);
    return { props, writes };
  }

  it("授权申请的三种状态与内部类型全部中文上屏（001）", async () => {
    renderM038(() => Promise.resolve(raw({})));
    await waitFor(() => expect(screen.getByTestId("access-grant-request-gr-a")).toBeTruthy());
    const text = screen.getByTestId("access-grant-requests-table").textContent ?? "";
    for (const label of ["已生效", "待处理", "已驳回", "人员范围变更", "岗位高危权限释放", "角色权限", "员工单独授予", "解除员工显式拒绝"]) expect(text).toContain(label);
    for (const code of ["applied", "user_scope_change", "position_high_risk_release", "role_permission", "user_permission_grant", "user_permission_deny_removal"]) expect(text).not.toContain(code);
    fireEvent.click(screen.getByTestId("access-grant-request-gr-a"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("人员范围变更");
    expect(drawer).toContain("已生效");
    expect(drawer).not.toContain("applied");
  });

  it("审批不填意见时留痕为中文「通过」，结果提示为中文状态（001）", async () => {
    const { props, writes } = renderM038(() => Promise.resolve(raw({ id: "gr-b", status: "applied" })));
    await waitFor(() => expect(screen.getByTestId("access-grant-actions-gr-b")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-actions-gr-b"));
    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-b")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-approve-gr-b"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]!.url).toContain("/grant-requests/gr-b/review");
    expect(writes[0]!.body.comment).toBe("通过");
    expect(writes[0]!.body.operator_note).toBe("通过");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("已生效")));
  });

  it("未映射的岗位码显示为「未登记岗位（岗位码）」（003）", async () => {
    renderM038(() => Promise.resolve(raw({})));
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-x")).toBeTruthy());
    const cells = screen.getByTestId("access-position-role-template-prt-x").querySelectorAll("td");
    expect(cells[1]?.textContent).toBe("未登记岗位（front_desk）");
    expect(cells[2]?.textContent).toBe("M2·上");
  });

  it("已存档的按钮写「取消存档」、已删除的写「恢复」，不再写「停用模板」（004）", async () => {
    renderM038(() => Promise.resolve(raw({})));
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-arch")).toBeTruthy());
    const buttonTexts = (rowId: string) => Array.from(screen.getByTestId(`access-position-role-template-${rowId}`).querySelectorAll("button")).map((button) => button.textContent);
    expect(buttonTexts("prt-arch")).toContain("取消存档");
    expect(buttonTexts("prt-del")).toContain("恢复");
    expect(buttonTexts("prt-del")).not.toContain("取消存档");
    expect(screen.getByTestId("access-position-role-templates-table").textContent).not.toContain("停用模板");
  });

  it("装配角色被后端拒绝时页面报出原因（005）", async () => {
    const { props, writes } = renderM038(() => Promise.resolve(new Response(
      JSON.stringify({ success: false, code: "GRANT_REQUIRES_APPROVAL", message: "以下权限的授予需要审批：[finance.refund.create]" }),
      { status: 403, headers: { "content-type": "application/json" } },
    )));
    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
    fireEvent.click(screen.getByTestId("access-role-assign"));
    await waitFor(() => expect(writes).toHaveLength(1));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringMatching(/\S/)));
    expect(props.setActionResult).not.toHaveBeenCalled();
  });

  it("提交未完成时再次点击不发第二笔（005）", async () => {
    let release: (value: Response) => void = () => undefined;
    const { writes } = renderM038(() => new Promise<Response>((resolve) => { release = resolve; }));
    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
    fireEvent.click(screen.getByTestId("access-role-assign"));
    fireEvent.click(screen.getByTestId("access-role-assign"));
    await waitFor(() => expect(writes).toHaveLength(1));
    await waitFor(() => expect((screen.getByTestId("access-role-assign") as HTMLButtonElement).disabled).toBe(true));
    release(raw({}));
    await waitFor(() => expect((screen.getByTestId("access-role-assign") as HTMLButtonElement).disabled).toBe(false));
    expect(writes.filter((write) => write.url.includes("/role-assignments"))).toHaveLength(1);
  });

  it("智能总结表表头列数等于数据行单元格数（007）", async () => {
    renderM038(() => Promise.resolve(raw({})));
    await waitFor(() => expect(screen.getByTestId("access-grant-summary-position-ops_manager")).toBeTruthy());
    const table = screen.getByTestId("access-grant-summary-table");
    expect(table.querySelectorAll("thead th")).toHaveLength(table.querySelectorAll("tbody tr")[0]!.querySelectorAll("td").length);
  });

  it("职级是 30 档下拉，范围变更处另有「沿用原职级」（008）", async () => {
    renderM038(() => Promise.resolve(raw({})));
    await waitFor(() => expect(screen.getByTestId("access-user-position-level")).toBeTruthy());
    const options = Array.from((screen.getByTestId("access-user-position-level") as HTMLSelectElement).options);
    expect(options).toHaveLength(30);
    expect(options[0]!.value).toBe("lvl_m1_lower");
    expect(options[0]!.textContent).toBe("M1·下");
    expect(options[29]!.value).toBe("lvl_m10_upper");
    expect(options[29]!.textContent).toBe("M10·上");
    const scopeOptions = Array.from((screen.getByTestId("access-user-scope-level") as HTMLSelectElement).options);
    expect(scopeOptions).toHaveLength(31);
    expect(scopeOptions[0]!.textContent).toBe("沿用原职级");
  });

  it("岗位模板弹窗里的状态用词与表格同一套（009）", async () => {
    renderM038(() => Promise.resolve(raw({})));
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    await waitFor(() => expect((screen.getByLabelText("装配角色") as HTMLSelectElement).value).toBe("role-1"));
    fireEvent.click(screen.getByTestId("access-position-role-template-create-open")); // 引导弹窗为单页填写，状态下拉就在第一页
    const status = await waitFor(() => screen.getByTestId("access-position-role-template-status") as HTMLSelectElement);
    expect(Array.from(status.options).map((option) => option.textContent)).toEqual(["有效", "未激活"]);
  });
});

// M002-F34 第（二）部分（依据 M002-F08 裁决「本人可以直接解除，不会卡死」；操作员 2026-09-30「最高权限本人给自己设拒绝后界面卡住的情况,这是不允许的。」）：
// 最高权限本人给自己设了显式拒绝后，仍能从导航与直链进访问治理工作台、选中自己、点「移除」解除；其余门禁不变。
describe("M002-F34 最高权限本人自设拒绝后界面不卡死", () => {
  const MASTER: AdminUser = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
  const PLATFORM_ADMIN: AdminUser = { id: "u1", username: "admin", accountType: "platform_admin" };
  const STAFF: AdminUser = { id: "u-1", username: "张三", accountType: "staff" };
  const F34_USERS = [
    { id: "u-1", username: "张三", account_type: "staff", status: "active" },
    { id: "u1", username: "admin", account_type: "platform_admin", status: "active" },
  ];
  const DENY_CASES: string[][] = [["access.role.assign"], ["access.role.assign", "access.user.view"]];
  // 与后端有效集同形：wildcard=false，grants 已扣掉 denies。
  const GRANTS = ["access.role.assign", "access.user.view", "access.role.view", "access.grant_request.view", "access.grant_request.apply"];
  function perms(denies: string[]): EffectivePermissionsState {
    return { status: "ready", value: { grants: GRANTS.filter((code) => !denies.includes(code)), denies, wildcard: false } };
  }
  // 与后端读接口门禁同口径：缺码即 403。
  const READ_CODES: Array<[string, string]> = [
    ["position-role-templates", "access.role.assign"], ["access/users", "access.user.view"],
    ["access/roles", "access.role.view"], ["grant-requests", "access.grant_request.view"],
  ];
  type Write = { url: string; body: Record<string, unknown> };

  function stubBackend(state: EffectivePermissionsState): Write[] {
    const writes: Write[] = [];
    const value: EffectivePermissions = state.status === "ready" ? state.value : { grants: [], denies: [], wildcard: false };
    const readable = (code: string) => value.grants.includes(code) && !value.denies.includes(code);
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() !== "GET") {
        writes.push({ url, body: init?.body ? JSON.parse(String(init.body)) : {} });
        return Promise.resolve(raw({}));
      }
      if (url.includes("effective-permissions")) return Promise.resolve(raw(value));
      if (url.includes("capability-switches")) return Promise.resolve(raw([]));
      for (const [fragment, code] of READ_CODES) {
        if (url.includes(fragment) && !readable(code)) {
          return Promise.resolve(new Response(JSON.stringify({ success: false, code: "PERMISSION_DENIED", message: "权限不足" }), { status: 403, headers: { "content-type": "application/json" } }));
        }
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(F34_USERS));
      return Promise.resolve(envelope([]));
    }));
    return writes;
  }

  function renderPageAs(user: AdminUser, state: EffectivePermissionsState) {
    const writes = stubBackend(state);
    render(<AccessGovernancePage api={new AdminApi("test-token")} user={user} setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={state} />);
    return writes;
  }
  function fillOverride(userId: string, effect: "deny" | "grant") {
    fireEvent.change(screen.getByLabelText("覆盖用户"), { target: { value: userId } });
    fireEvent.change(screen.getByLabelText("覆盖权限码"), { target: { value: "access.role.assign" } });
    fireEvent.change(screen.getByLabelText("覆盖效果"), { target: { value: effect } });
  }
  const button = (testId: string) => screen.getByTestId(testId) as HTMLButtonElement;
  const overrideForm = () => waitFor(() => expect(screen.getByTestId("access-user-override-form")).toBeTruthy());

  function renderAppAs(user: AdminUser, state: EffectivePermissionsState, path: string) {
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user, sessionId: "s-f34" }));
    window.history.pushState({}, "", path);
    stubBackend(state);
    render(<AdminApp />);
  }

  // 主壳懒加载页面：等待上限 3 秒（低于用例默认 5 秒），找不到时以「找不到元素」断言失败而非用例超时。
  beforeEach(() => {
    // jsdom 的元素没有 scrollTo；主壳换页时会调用。
    if (typeof HTMLElement.prototype.scrollTo !== "function") HTMLElement.prototype.scrollTo = () => undefined;
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    window.localStorage.clear();
    window.history.pushState({}, "", "/");
  });

  it.each(DENY_CASES)("最高权限拒绝了 %s 等：页内能选中本人，点「移除」发出解除请求，第二次也能发", async (...denies: string[]) => {
    const writes = renderPageAs(MASTER, perms(denies));
    await overrideForm();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    fillOverride("usr_master_authority", "deny");
    expect((screen.getByLabelText("覆盖用户") as HTMLSelectElement).value).toBe("usr_master_authority");
    expect(button("access-user-override-remove").disabled).toBe(false);
    expect(button("access-user-override-set").disabled).toBe(true); // 设置仍按分配角色权限
    fireEvent.click(button("access-user-override-remove"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]!.url).toContain("/api/v1/access/users/usr_master_authority/permission-overrides/remove");
    expect(writes[0]!.body.effect).toBe("deny");
    expect(writes[0]!.body.permission_codes).toEqual(["access.role.assign"]);
    await waitFor(() => expect(button("access-user-override-remove").disabled).toBe(false));
    fireEvent.click(button("access-user-override-remove"));
    await waitFor(() => expect(writes).toHaveLength(2));
    expect(writes[1]!.url).toContain("/api/v1/access/users/usr_master_authority/permission-overrides/remove");
  });

  it("最高权限选他人或选单独授予时，「移除」仍按分配角色权限判定", async () => {
    renderPageAs(MASTER, perms(["access.role.assign"]));
    await overrideForm();
    fillOverride("u-1", "deny");
    expect(button("access-user-override-remove").disabled).toBe(true);
    fillOverride("usr_master_authority", "grant");
    expect(button("access-user-override-remove").disabled).toBe(true);
  });

  it.each([PLATFORM_ADMIN, STAFF])("$username（$accountType）拒绝了分配角色权限：页内「移除」本人拒绝仍不可用", async (user: AdminUser) => {
    renderPageAs(user, perms(["access.role.assign"]));
    await overrideForm();
    fillOverride(user.id, "deny");
    expect((screen.getByLabelText("覆盖用户") as HTMLSelectElement).value).toBe(user.id);
    expect(button("access-user-override-remove").disabled).toBe(true);
  });

  it.each(DENY_CASES)("最高权限拒绝了 %s 等：直链进访问治理工作台，导航项可点", async (...denies: string[]) => {
    renderAppAs(MASTER, perms(denies), "/admin/access/governance");
    expect(await screen.findByTestId("access-governance-page", {}, { timeout: 3000 })).toBeTruthy();
    expect(screen.queryByTestId("admin-permission-denied")).toBeNull();
    expect(button("admin-nav-ADM-ACC-003").disabled).toBe(false);
  });

  it("最高权限拒绝了分配角色权限：从别的页点导航进访问治理工作台（同码的组织层级页仍拒）", async () => {
    renderAppAs(MASTER, perms(["access.role.assign", "access.user.view"]), "/admin/access/org-hierarchy");
    expect(await screen.findByTestId("admin-permission-denied")).toBeTruthy();
    expect(button("admin-nav-ADM-ACC-004").disabled).toBe(true);
    fireEvent.click(button("admin-nav-ADM-ACC-003"));
    expect(await screen.findByTestId("access-governance-page", {}, { timeout: 3000 })).toBeTruthy();
  });

  // M002-F49（操作员裁决 M002-F34「界面上始终能进访问治理工作台、选中自己、点『移除』解除」）：从正常权限开始，同页给本人设拒绝，
  // 后端有效集随写入改变、同码副资源 403——页面不整页报错，本人选择器与「移除」仍在；移除后有效集恢复、副资源重新读取，全程不刷新页面。
  function stubLiveBackend() {
    const denied = new Set<string>();
    const writes: Write[] = [];
    let templateReads = 0;
    const current = (): EffectivePermissions => ({ grants: GRANTS.filter((code) => !denied.has(code)), denies: [...denied], wildcard: false });
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() !== "GET") {
        const body = init?.body ? JSON.parse(String(init.body)) : {};
        writes.push({ url, body });
        if (url.includes("/users/usr_master_authority/permission-overrides") && body.effect === "deny") {
          for (const code of body.permission_codes as string[]) {
            if (url.endsWith("/remove")) denied.delete(code);
            else denied.add(code);
          }
        }
        return Promise.resolve(raw(current()));
      }
      if (url.includes("effective-permissions")) return Promise.resolve(raw(current()));
      if (url.includes("capability-switches")) return Promise.resolve(raw([]));
      for (const [fragment, code] of READ_CODES) {
        if (url.includes(fragment) && denied.has(code)) {
          return Promise.resolve(new Response(JSON.stringify({ success: false, code: "PERMISSION_DENIED", message: "权限不足" }), { status: 403, headers: { "content-type": "application/json" } }));
        }
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
      if (url.includes("position-role-templates")) {
        templateReads += 1;
        return Promise.resolve(envelope(TEMPLATES));
      }
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(F34_USERS));
      return Promise.resolve(envelope([]));
    }));
    return { writes, templateReads: () => templateReads };
  }

  it.each(DENY_CASES)("最高权限从正常权限同页给自己设 %s 等拒绝：不整页报错，能选中本人点「移除」，移除后恢复且不刷新页面", async (...codes: string[]) => {
    const live = stubLiveBackend();
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: MASTER, sessionId: "s-f49" }));
    window.history.pushState({}, "", "/admin/access/governance");
    render(<AdminApp />);
    expect(await screen.findByTestId("access-user-override-form", {}, { timeout: 3000 })).toBeTruthy();
    await waitFor(() => expect(live.templateReads()).toBeGreaterThan(0));
    await waitFor(() => expect(button("access-user-override-set").disabled).toBe(false));
    fireEvent.change(screen.getByLabelText("覆盖用户"), { target: { value: "usr_master_authority" } });
    fireEvent.change(screen.getByLabelText("覆盖权限码"), { target: { value: codes.join(",") } });
    fireEvent.change(screen.getByLabelText("覆盖效果"), { target: { value: "deny" } });
    fireEvent.click(button("access-user-override-set"));
    await waitFor(() => expect(live.writes).toHaveLength(1));
    await waitFor(() => expect(button("access-user-override-set").disabled).toBe(true)); // 有效集已刷新：设置按分配角色权限禁用
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect((screen.getByLabelText("覆盖用户") as HTMLSelectElement).value).toBe("usr_master_authority");
    expect(button("access-user-override-remove").disabled).toBe(false);
    const readsWhileDenied = live.templateReads();
    fireEvent.click(button("access-user-override-remove"));
    await waitFor(() => expect(live.writes).toHaveLength(2));
    expect(live.writes[1]!.url).toContain("/api/v1/access/users/usr_master_authority/permission-overrides/remove");
    expect(live.writes[1]!.body.permission_codes).toEqual(codes);
    await waitFor(() => expect(button("access-user-override-set").disabled).toBe(false));
    await waitFor(() => expect(live.templateReads()).toBeGreaterThan(readsWhileDenied));
    expect(screen.queryByTestId("admin-error")).toBeNull();
  });

  it.each([PLATFORM_ADMIN, STAFF])("$username（$accountType）拒绝了分配角色权限：工作台直链与导航仍不可用", async (user: AdminUser) => {
    renderAppAs(user, perms(["access.role.assign"]), "/admin/access/governance");
    expect(await screen.findByTestId("admin-permission-denied")).toBeTruthy();
    expect(screen.queryByTestId("access-governance-page")).toBeNull();
    expect(button("admin-nav-ADM-ACC-003").disabled).toBe(true);
  });
});
