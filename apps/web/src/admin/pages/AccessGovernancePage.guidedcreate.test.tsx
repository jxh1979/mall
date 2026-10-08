import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessGovernancePage } from "./AccessGovernancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-9：新增岗位角色模板改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖岗位角色模板主创建流；授权申请/审批工作流不在本期改造范围。
const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];
const ROLES = [{ id: "role-1", role_name: "运营经理", role_code: "ops", status: "active" }];
const TEMPLATES = [
  { id: "prt-1", position_code: "ops_manager", staff_level_id: "lvl_m1", role_id: "role-1", template_id: "tpl-1", organization_id: "org-1", status: "active" },
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
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("position-role-templates")) {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(raw({ id: "prt-new" }));
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
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
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AccessGovernancePage {...props} />);
  return { setActionResult };
}

describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
  });

  it("页面默认只留入口按钮，不展示模板字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    expect(screen.queryByTestId("access-position-role-template-position")).toBeNull();
    expect(screen.queryByTestId("access-position-role-template-guided")).toBeNull();
  });

  it("入口→步进→提交：POST 新建模板带岗位/职级/角色，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    // 等角色名单加载完，弹窗打开时 roleId 默认落到首个角色。
    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
    fireEvent.click(screen.getByTestId("access-position-role-template-create-open"));

    fireEvent.change(screen.getByTestId("access-position-role-template-position"), { target: { value: "front_desk" } });
    fireEvent.change(screen.getByTestId("access-position-role-template-level"), { target: { value: "lvl_m2_upper" } });
    fireEvent.click(screen.getByTestId("access-position-role-template-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).not.toContain("prt-"); // 新建走 POST 集合路径，非 PUT /{id}
    expect((lastPostBody as { position_code: string }).position_code).toBe("front_desk");
    expect((lastPostBody as { staff_level_id: string }).staff_level_id).toBe("lvl_m2_upper");
    expect((lastPostBody as { role_id: string }).role_id).toBe("role-1");
    await waitFor(() => expect(screen.queryByTestId("access-position-role-template-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^岗位模板已保存/)); // PGM-1.18：结果提示全中文
  });

  it("步内校验拦截：岗位清空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-create-open"));
    fireEvent.change(screen.getByTestId("access-position-role-template-position"), { target: { value: "  " } });
    fireEvent.click(screen.getByTestId("access-position-role-template-guided-next"));
    expect(screen.getByTestId("access-position-role-template-guided-error").textContent).toContain("岗位");
    expect(screen.queryByTestId("access-position-role-template-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});

// M038-010：发起授权申请的引导提交（入口→类型与用户→权限码与过期时间→提交）此前没有测试。
describe("AccessGovernancePage 引导式发起授权申请（M038-010）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("入口→步进→提交：POST /grant-requests 带类型、目标用户、权限码、过期时间与单次有效", async () => {
    let grantPost: { url: string; body: Record<string, unknown> } | null = null;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/api/v1/access/grant-requests")) {
        grantPost = { url, body: JSON.parse(String(init?.body)) };
        return Promise.resolve(raw({ id: "agr-new", status: "pending" }));
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }));
    const setActionResult = vi.fn();
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult,
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AccessGovernancePage {...props} />);
    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
    await waitFor(() => expect((screen.getByLabelText("目标用户") as HTMLSelectElement).value).toBe("u-1"));
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步1→步2
    fireEvent.change(screen.getByLabelText("临时权限码"), { target: { value: "finance.refund.create" } });
    fireEvent.change(screen.getByLabelText("过期时间"), { target: { value: "2026-12-31T18:00" } });
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("access-grant-request-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create"));

    await waitFor(() => expect(grantPost).toBeTruthy());
    const body = grantPost!.body;
    expect(body.request_type).toBe("temporary_permission");
    expect(body.target_user_id).toBe("u-1");
    expect(body.permission_codes).toEqual(["finance.refund.create"]);
    expect(new Date(String(body.expires_at)).getTime()).toBe(new Date("2026-12-31T18:00").getTime());
    expect(body.single_use).toBe(true);
    await waitFor(() => expect(screen.queryByTestId("access-grant-request-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^授权申请已提交：agr-new（待处理）/));
  });

  it("步内校验拦截：临时权限未填过期时间则不进确认、不提交", async () => {
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AccessGovernancePage {...props} />);
    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
    await waitFor(() => expect((screen.getByLabelText("目标用户") as HTMLSelectElement).value).toBe("u-1"));
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 过期时间未填
    expect(screen.getByTestId("access-grant-request-guided-error").textContent).toContain("过期时间");
    expect(screen.queryByTestId("access-grant-request-guided-summary")).toBeNull();
    expect(fetchMock.mock.calls.some(([, init]) => ((init as RequestInit | undefined)?.method ?? "GET").toUpperCase() === "POST")).toBe(false);
  });
});

// M736（W1；操作员 2026-10-01 说明「权限配置表格当中有一个可以查看全部项目选项……可以点击+打开」、2026-10-03「三类各自单列」）：
// 「④ 角色」标签的角色表与「＋ 新建角色」两步引导。
describe("AccessGovernancePage 角色表与引导式新建角色（M736）", () => {
  afterEach(() => vi.unstubAllGlobals());

  const ROLE_ROWS = [
    { id: "role-all", role_name: "集团财务督导", status: "active", permission_codes: ["access.user.view"], view_all_projects: true, viewable_project_ids: [] },
    { id: "role-two", role_name: "区域财务督导", status: "active", permission_codes: ["access.user.view", "access.role.view"], view_all_projects: false, viewable_project_ids: ["org_b", "be_city"] },
    // 两项都没有的应答（与本模块之前的应答同形）：按普通角色显示。
    { id: "role-plain", role_name: "运营经理", status: "active", permission_codes: [] },
  ];
  const PERMISSIONS = [
    { permission_code: "access.user.view", permission_name: "查看账号", domain: "access" },
    { permission_code: "finance.ar_ap.view", permission_name: "查看应收应付", domain: "finance" },
  ];
  const PROJECT_OPTIONS = [
    { id: "org_a", name: "一号综合体", kind: "project" },
    { id: "org_b", name: "二号综合体", kind: "project" },
    { id: "be_city", name: "城南项目公司", kind: "project_company" },
    { id: "be_mgmt", name: "酒吧域主管公司", kind: "management_company" },
  ];
  const CREATE_CODES = ["access.role.assign", "access.role.view", "access.role.manage", "access.permission.view"];
  function perms(grants: string[]): EffectivePermissionsState {
    return { status: "ready", value: { grants, denies: [], wildcard: false } };
  }
  type Sent = { url: string; body: Record<string, unknown> };

  function renderRoles(grants: string[], onCreate: () => Response = () => raw({ id: "role-new" })) {
    const reads: string[] = [];
    const creates: Sent[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        creates.push({ url, body: JSON.parse(String(init?.body)) });
        return Promise.resolve(onCreate());
      }
      reads.push(url);
      if (url.includes("role-project-options")) return Promise.resolve(envelope(PROJECT_OPTIONS));
      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLE_ROWS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u-9", username: "项目管理员", accountType: "staff" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: perms(grants),
    };
    render(<AccessGovernancePage {...props} />);
    return { props, creates, readCount: (fragment: string) => reads.filter((url) => url.includes(fragment)).length };
  }

  // 打开引导并等权限字典、候选清单读到。
  async function openGuide(onCreate?: () => Response) {
    const page = renderRoles(CREATE_CODES, onCreate);
    await waitFor(() => expect(screen.getByTestId("access-role-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-role-create-open"));
    await waitFor(() => expect(screen.getByTestId("access-role-create-code-access.user.view")).toBeTruthy());
    return page;
  }
  const button = (testId: string) => screen.getByTestId(testId) as HTMLButtonElement;
  // 填写页 → 确认页（记下确认页文字）→ 提交，等到第 count 笔创建请求发出。
  async function submitGuide(creates: Sent[], count: number) {
    fireEvent.click(screen.getByTestId("access-role-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("access-role-create-guided-summary")).toBeTruthy());
    const summary = screen.getByTestId("access-role-create-guided-summary").textContent ?? "";
    await waitFor(() => expect(button("access-role-create-save").disabled).toBe(false));
    fireEvent.click(button("access-role-create-save"));
    await waitFor(() => expect(creates).toHaveLength(count));
    return { ...creates[count - 1]!, summary };
  }

  it.each(["access.role.manage", "access.permission.view"])("缺 %s 不显示「＋ 新建角色」，角色表照常", async (missing: string) => {
    renderRoles(CREATE_CODES.filter((code) => code !== missing));
    await waitFor(() => expect(screen.getByTestId("access-roles-table").textContent).toContain("集团财务督导"));
    expect(screen.queryByTestId("access-role-create-open")).toBeNull();
  });

  it("没有 access.role.view 不显示「④ 角色」，页面说明仍是三块", async () => {
    renderRoles(["access.role.assign"]);
    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table")).toBeTruthy());
    expect(screen.queryByTestId("access-roles-table")).toBeNull();
    expect(screen.getByTestId("access-governance-guide").textContent).toContain("访问治理三块");
    expect(screen.getByTestId("access-governance-guide").textContent).not.toContain("④");
  });

  it("角色表「可以查看全部项目」「可查看项目」两列按返回体显示；展开「＋」列出条目名称，候选清单只在展开时读一次", async () => {
    const { readCount } = renderRoles(["access.role.assign", "access.role.view", "access.role.manage"]);
    await waitFor(() => expect(screen.getByTestId("access-role-row-role-all")).toBeTruthy());
    expect(screen.getByTestId("access-governance-guide").textContent).toContain("访问治理四块");
    const cells = (id: string) => Array.from(screen.getByTestId(`access-role-row-${id}`).querySelectorAll("td")).map((cell) => cell.textContent ?? "");
    expect(cells("role-all").slice(0, 5)).toEqual(["集团财务督导", "有效", "1 个", "是", "全部项目"]);
    expect(cells("role-two").slice(2, 4)).toEqual(["2 个", "—"]);
    expect(cells("role-two")[4]).toContain("已选 2 个");
    expect(cells("role-plain").slice(2, 5)).toEqual(["—", "—", "—"]);
    expect(screen.getByTestId("access-roles-table").querySelectorAll("thead th")).toHaveLength(cells("role-all").length);
    // 勾了全部的、都没勾的没有「＋」。
    expect(screen.queryByTestId("access-role-projects-toggle-role-all")).toBeNull();
    expect(screen.queryByTestId("access-role-projects-toggle-role-plain")).toBeNull();
    // 页面加载不读候选清单与权限字典。
    expect(readCount("role-project-options")).toBe(0);
    expect(readCount("access/permissions")).toBe(0);

    fireEvent.click(button("access-role-projects-toggle-role-two"));
    await waitFor(() => expect(screen.getByTestId("access-role-projects-role-two").textContent).toBe("二号综合体、城南项目公司"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull(); // 点「＋」不误开行详情
    fireEvent.click(button("access-role-projects-toggle-role-two")); // 收起
    expect(screen.queryByTestId("access-role-projects-role-two")).toBeNull();
    fireEvent.click(button("access-role-projects-toggle-role-two")); // 再展开：不再请求
    expect(screen.getByTestId("access-role-projects-role-two").textContent).toBe("二号综合体、城南项目公司");
    expect(readCount("role-project-options")).toBe(1);

    fireEvent.click(screen.getByTestId("access-role-row-role-all"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("集团财务督导");
    expect(drawer).toContain("全部项目");
    expect(drawer).not.toContain("active");
  });

  it("没有 access.role.manage：可查看项目只显示个数，不发候选清单请求", async () => {
    const { readCount } = renderRoles(["access.role.assign", "access.role.view", "access.permission.view"]);
    await waitFor(() => expect(screen.getByTestId("access-role-row-role-two").textContent).toContain("已选 2 个"));
    expect(screen.queryByTestId("access-role-projects-toggle-role-two")).toBeNull();
    expect(readCount("role-project-options")).toBe(0);
  });

  it("两步引导：名称与说明、权限配置；权限点按菜单模块分组，表单没有数据范围；打开时权限字典与候选清单各读一次", async () => {
    const { readCount, creates } = await openGuide();
    const guide = screen.getByTestId("access-role-create-guided");
    expect(within(guide).getByText("名称与说明")).toBeTruthy();
    expect(within(guide).getByText("权限配置")).toBeTruthy();
    expect(within(screen.getByTestId("access-role-create-permission-table")).getByText("账号权限")).toBeTruthy();
    expect(guide.textContent).not.toContain("数据范围");
    expect(readCount("access/permissions")).toBe(1);
    expect(readCount("role-project-options")).toBe(1);
    // 名称没填：停在填写页，不提交。
    fireEvent.click(screen.getByTestId("access-role-create-guided-next"));
    expect(screen.getByTestId("access-role-create-guided-error").textContent).toContain("角色名称必填");
    expect(screen.queryByTestId("access-role-create-guided-summary")).toBeNull();
    expect(creates).toHaveLength(0);
  });

  it("勾「可以查看全部项目」：逐项清单收起并禁用、已勾条目清空，提交 view_all_projects=true 且不带清单", async () => {
    const { props, creates } = await openGuide();
    fireEvent.change(screen.getByTestId("access-role-create-name"), { target: { value: "  集团财务督导二  " } });
    fireEvent.click(screen.getByTestId("access-role-create-code-access.user.view"));
    fireEvent.click(button("access-role-create-projects-toggle"));
    fireEvent.click(screen.getByTestId("access-role-create-project-org_b"));
    fireEvent.click(screen.getByTestId("access-role-create-view-all"));
    expect(button("access-role-create-projects-toggle").disabled).toBe(true);
    expect(screen.queryByTestId("access-role-create-project-org_b")).toBeNull();

    const { url, body } = await submitGuide(creates, 1);
    expect(url.endsWith("/api/v1/access/roles")).toBe(true);
    expect(body.role_name).toBe("集团财务督导二");
    expect(body.permission_codes).toEqual(["access.user.view"]);
    expect(body.view_all_projects).toBe(true);
    expect("viewable_project_ids" in body).toBe(false);
    expect("data_scope" in body).toBe(false);
    await waitFor(() => expect(screen.queryByTestId("access-role-create-guided")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith("角色已创建：role-new");
  });

  it("勾过全部再取消：之前勾的条目不回来", async () => {
    const { creates } = await openGuide();
    fireEvent.change(screen.getByTestId("access-role-create-name"), { target: { value: "区域财务督导三" } });
    fireEvent.click(button("access-role-create-projects-toggle"));
    fireEvent.click(screen.getByTestId("access-role-create-project-org_b"));
    fireEvent.click(screen.getByTestId("access-role-create-view-all"));
    fireEvent.click(screen.getByTestId("access-role-create-view-all"));
    expect(button("access-role-create-projects-toggle").disabled).toBe(false);
    fireEvent.click(button("access-role-create-projects-toggle"));
    expect((screen.getByTestId("access-role-create-project-org_b") as HTMLInputElement).checked).toBe(false);
    const { body } = await submitGuide(creates, 1);
    expect(body.view_all_projects).toBe(false);
    expect(body.viewable_project_ids).toEqual([]);
  });

  it("点「＋」展开：项目、城市项目公司、域主管公司三组各自单列；勾两条就提交这两条编号", async () => {
    const { creates } = await openGuide();
    fireEvent.change(screen.getByTestId("access-role-create-name"), { target: { value: "区域财务督导二" } });
    expect(screen.queryByTestId("access-role-create-projects-project")).toBeNull(); // 默认收起
    fireEvent.click(button("access-role-create-projects-toggle"));
    expect(screen.getByTestId("access-role-create-projects-project").textContent).toBe("项目一号综合体二号综合体");
    expect(screen.getByTestId("access-role-create-projects-project_company").textContent).toBe("城市项目公司城南项目公司");
    expect(screen.getByTestId("access-role-create-projects-management_company").textContent).toBe("域主管公司酒吧域主管公司");
    fireEvent.click(screen.getByTestId("access-role-create-project-org_b"));
    fireEvent.click(screen.getByTestId("access-role-create-project-be_city"));

    const { body, summary } = await submitGuide(creates, 1);
    expect(summary).toContain("城南项目公司、二号综合体");
    expect(body.view_all_projects).toBe(false);
    expect(body.viewable_project_ids).toEqual(["be_city", "org_b"]);
  });

  it("两项都不勾是普通角色；创建成功后再开引导是空表单，第二次照样能建", async () => {
    const { creates, readCount } = await openGuide();
    fireEvent.change(screen.getByTestId("access-role-create-name"), { target: { value: "普通角色甲" } });
    fireEvent.change(screen.getByTestId("access-role-create-description"), { target: { value: "只管本项目" } });
    const first = await submitGuide(creates, 1);
    expect(first.body.description).toBe("只管本项目");
    expect(first.body.view_all_projects).toBe(false);
    expect(first.body.viewable_project_ids).toEqual([]);
    await waitFor(() => expect(screen.queryByTestId("access-role-create-guided")).toBeNull());

    // 成功后角色表重新读取，入口按钮回来再开第二次。
    await waitFor(() => expect(readCount("access/roles")).toBe(2));
    await waitFor(() => expect(screen.getByTestId("access-role-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-role-create-open"));
    expect((screen.getByTestId("access-role-create-name") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("access-role-create-description") as HTMLInputElement).value).toBe("");
    await waitFor(() => expect(readCount("role-project-options")).toBe(2)); // 每次打开各读一次
    fireEvent.change(screen.getByTestId("access-role-create-name"), { target: { value: "普通角色乙" } });
    const second = await submitGuide(creates, 2);
    expect(second.body.role_name).toBe("普通角色乙");
    expect("description" in second.body).toBe(false);
    expect(second.body.idempotency_key).not.toBe(first.body.idempotency_key);
  });

  it("后端拒绝：弹窗留开并显示后端的中文提示，页面同时报出，改掉后再提交能成", async () => {
    let rejected = false;
    const { props, creates } = await openGuide(() => {
      if (rejected) return raw({ id: "role-new" });
      rejected = true;
      return new Response(
        JSON.stringify({ success: false, code: "PARAM_INVALID", detail_code: "VALIDATION_FAILED", message: "可查看项目清单里有无法识别或已停用的条目：be_mgmt" }),
        { status: 400, headers: { "content-type": "application/json" } },
      );
    });
    fireEvent.change(screen.getByTestId("access-role-create-name"), { target: { value: "区域财务督导四" } });
    fireEvent.click(button("access-role-create-projects-toggle"));
    fireEvent.click(screen.getByTestId("access-role-create-project-be_mgmt"));
    await submitGuide(creates, 1);
    await waitFor(() => expect(screen.getByTestId("access-role-create-guided-error").textContent).toContain("无法识别或已停用的条目：be_mgmt"));
    expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("无法识别或已停用的条目"));
    expect(props.setActionResult).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("access-role-create-guided-prev"));
    fireEvent.click(screen.getByTestId("access-role-create-project-be_mgmt"));
    const second = await submitGuide(creates, 2);
    expect(second.body.viewable_project_ids).toEqual([]);
    await waitFor(() => expect(screen.queryByTestId("access-role-create-guided")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith("角色已创建：role-new");
  });
});

// M736 第二步（W2～W8；操作员 2026-10-08 确认的治理地图追加句「可跨项目角色分配出去以后，可以直接在原角色上增加或减少可查看的项目：
// 增加的，所有持有人要各自重新审批通过，才能查看新增的项目；减少的，也要审批通过后才生效，审批期间持有人仍能查看。」）：
// 「④ 角色」表每行的「改可查看项目」弹窗。真实页面经真实客户端，只桩网络层；每条用例对「保存」只点一次。
describe("AccessGovernancePage 改可查看项目（M736 第二步）", () => {
  afterEach(() => vi.unstubAllGlobals());

  const ROLE_ROWS = [
    { id: "role-two", role_name: "区域财务督导", status: "active", permission_codes: ["access.user.view"], view_all_projects: false, viewable_project_ids: ["org_b", "be_city", "org_gone"] },
    { id: "role-all", role_name: "集团财务督导", status: "active", permission_codes: ["access.user.view"], view_all_projects: true, viewable_project_ids: ["org_b", "org_gone"] },
    { id: "role-reducing", role_name: "集团运营督导", status: "active", permission_codes: ["access.user.view"], view_all_projects: true, viewable_project_ids: [] },
    { id: "role-plain", role_name: "运营经理", status: "active", permission_codes: [] },
  ];
  // org_gone 是角色上原有、候选清单里已经没有的旧条目。
  const PROJECT_OPTIONS = [
    { id: "org_a", name: "一号综合体", kind: "project" },
    { id: "org_b", name: "二号综合体", kind: "project" },
    { id: "be_city", name: "城南项目公司", kind: "project_company" },
    { id: "be_mgmt", name: "酒吧域主管公司", kind: "management_company" },
  ];
  type Holder = { user_id: string; username: string; viewable_ids: string[]; reapproval_pending: boolean };
  type View = { role_id: string; view_all_projects: boolean; viewable_project_ids: string[]; pending_removal_ids: string[]; pending_reapprovals: Holder[] };
  // 现状接口的应答（每条用例一份新的，发起重新审批后会改）。
  function views(): Record<string, View> {
    return {
      // be_city 的减少在待审；张三还没发起重新审批，李四有一张还批得动的待审申请。
      "role-two": { role_id: "role-two", view_all_projects: false, viewable_project_ids: ["org_b", "be_city", "org_gone"], pending_removal_ids: ["be_city"],
        pending_reapprovals: [
          { user_id: "u-zhang", username: "张三", viewable_ids: ["org_b"], reapproval_pending: false },
          { user_id: "u-li", username: "李四", viewable_ids: ["org_b"], reapproval_pending: true },
        ] },
      // 勾着全部、清单留着；王五对「全部」这一勾与 org_b 都还没重新审批。
      "role-all": { role_id: "role-all", view_all_projects: true, viewable_project_ids: ["org_b", "org_gone"], pending_removal_ids: [],
        pending_reapprovals: [{ user_id: "u-wang", username: "王五", viewable_ids: ["*", "org_b"], reapproval_pending: false }] },
      // 去掉「可以查看全部项目」的减少在待审。
      "role-reducing": { role_id: "role-reducing", view_all_projects: true, viewable_project_ids: [], pending_removal_ids: ["*"], pending_reapprovals: [] },
      "role-plain": { role_id: "role-plain", view_all_projects: false, viewable_project_ids: [], pending_removal_ids: [], pending_reapprovals: [] },
    };
  }
  const EDIT_CODES = ["access.role.assign", "access.role.view", "access.role.manage", "access.grant_request.apply"];
  type Sent = { url: string; body: Record<string, unknown> };

  function renderEdit(grants: string[], onPut: () => Response = () => raw({})) {
    const current = views();
    const reads: string[] = [];
    const puts: Sent[] = [];
    const posts: Sent[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method === "PUT") {
        puts.push({ url, body: JSON.parse(String(init?.body)) });
        return Promise.resolve(onPut());
      }
      if (method === "POST") {
        const body = JSON.parse(String(init?.body)) as Record<string, unknown>;
        posts.push({ url, body });
        // 申请建好以后，这个持有人在现状里就是「已有还批得动的待审申请」。
        for (const holder of current[String(body.role_id)]?.pending_reapprovals ?? []) {
          if (holder.user_id === body.target_user_id) holder.reapproval_pending = true;
        }
        return Promise.resolve(raw({ id: "agr-re-1", status: "pending" }));
      }
      reads.push(url);
      // 现状接口的地址也含 access/roles，要先于角色列表匹配。
      const viewOf = /access\/roles\/([^/]+)\/viewable-projects$/.exec(url);
      if (viewOf) return Promise.resolve(raw(current[viewOf[1]!]));
      if (url.includes("role-project-options")) return Promise.resolve(envelope(PROJECT_OPTIONS));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLE_ROWS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u-9", username: "项目管理员", accountType: "staff" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants, denies: [], wildcard: false } },
    };
    render(<AccessGovernancePage {...props} />);
    return { props, puts, posts, readCount: (suffix: string) => reads.filter((url) => url.endsWith(suffix)).length };
  }

  // 点角色行的「改可查看项目」，等现状与候选清单读到、弹窗打开。
  async function openEdit(roleId: string, grants: string[] = EDIT_CODES, onPut?: () => Response) {
    const page = renderEdit(grants, onPut);
    await waitFor(() => expect(screen.getByTestId(`access-role-projects-edit-${roleId}`)).toBeTruthy());
    fireEvent.click(screen.getByTestId(`access-role-projects-edit-${roleId}`));
    await waitFor(() => expect(screen.getByTestId("access-role-edit-guided")).toBeTruthy());
    return page;
  }
  const box = (testId: string) => screen.getByTestId(testId) as HTMLInputElement;
  const holderRows = () => Array.from(screen.getByTestId("access-role-edit-holders").querySelectorAll("tbody tr"))
    .map((row) => Array.from(row.querySelectorAll("td")).map((cell) => cell.textContent ?? ""));
  // 请求体里这次增减用到的键（请求编号、操作备注每次都带，不算）。
  const deltaKeys = (body: Record<string, unknown>) => Object.keys(body).filter((key) => key !== "idempotency_key" && key !== "operator_note").sort();
  // 填写页 → 确认页（记下确认页文字）→ 保存，等修改请求发出。
  async function save(puts: Sent[]) {
    fireEvent.click(screen.getByTestId("access-role-edit-guided-next"));
    const summary = screen.getByTestId("access-role-edit-guided-summary").textContent ?? "";
    fireEvent.click(screen.getByTestId("access-role-edit-save"));
    await waitFor(() => expect(puts).toHaveLength(1));
    return { ...puts[0]!, summary };
  }

  it("没有 access.role.manage 不显示「改可查看项目」", async () => {
    renderEdit(EDIT_CODES.filter((code) => code !== "access.role.manage"));
    await waitFor(() => expect(screen.getByTestId("access-role-row-role-two")).toBeTruthy());
    expect(screen.queryByTestId("access-role-projects-edit-role-two")).toBeNull();
  });

  it("打开按现状回显：现有条目已勾，减少在待审的条目禁用并标「（减少待审批）」，列出待重新审批的持有人，页面上没有说明文字", async () => {
    const { readCount } = await openEdit("role-two");
    expect(readCount("/access/roles/role-two/viewable-projects")).toBe(1);
    expect(readCount("/access/role-project-options")).toBe(1);
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull(); // 点按钮不误开行详情
    expect(box("access-role-edit-view-all").checked).toBe(false);
    expect(box("access-role-edit-view-all").disabled).toBe(false);
    // 逐个勾选的角色打开就是展开的。
    expect(box("access-role-edit-project-org_a").checked).toBe(false);
    expect(box("access-role-edit-project-org_b").checked).toBe(true);
    expect(box("access-role-edit-project-org_b").disabled).toBe(false);
    expect(box("access-role-edit-project-be_city").checked).toBe(true);
    expect(box("access-role-edit-project-be_city").disabled).toBe(true);
    expect(screen.getByTestId("access-role-edit-projects-project").textContent).toBe("项目一号综合体二号综合体");
    expect(screen.getByTestId("access-role-edit-projects-project_company").textContent).toBe("城市项目公司城南项目公司（减少待审批）");
    expect(screen.getByTestId("access-role-edit-projects-management_company").textContent).toBe("域主管公司酒吧域主管公司");
    expect(Array.from(screen.getByTestId("access-role-edit-holders").querySelectorAll("thead th")).map((cell) => cell.textContent)).toEqual(["账号", "待重新审批的项目", "操作"]);
    expect(holderRows()).toEqual([["张三", "二号综合体", "发起重新审批"], ["李四", "二号综合体", "已发起，待审批"]]);
    expect(screen.queryByTestId("access-role-edit-final-approver")).toBeNull();
    // 治理地图那句追加句不放到页面上。
    const text = document.body.textContent ?? "";
    expect(text).not.toContain("可以直接在原角色上");
    expect(text).not.toContain("所有持有人要各自重新审批通过");
    expect(text).not.toContain("审批期间持有人仍能查看");
  });

  it("去掉「可以查看全部项目」的减少在待审：勾选框禁用并标「（减少待审批）」；没有变化不进确认页、不提交", async () => {
    const { puts } = await openEdit("role-reducing");
    expect(box("access-role-edit-view-all").checked).toBe(true);
    expect(box("access-role-edit-view-all").disabled).toBe(true);
    expect(box("access-role-edit-view-all").closest("td")?.textContent).toContain("（减少待审批）");
    expect(screen.queryByTestId("access-role-edit-holders")).toBeNull(); // 没有待重新审批的持有人，这一节不出现
    fireEvent.click(screen.getByTestId("access-role-edit-guided-next"));
    expect(screen.getByTestId("access-role-edit-guided-error").textContent).toBe("可查看项目没有变化");
    expect(screen.queryByTestId("access-role-edit-guided-summary")).toBeNull();
    expect(puts).toHaveLength(0);
  });

  it("增加一条：只提交这次增加的条目，提示所有持有人要各自重新审批通过；成功后角色表重新读取", async () => {
    const { props, puts, readCount } = await openEdit("role-two");
    fireEvent.click(box("access-role-edit-project-org_a"));
    expect(screen.queryByTestId("access-role-edit-final-approver")).toBeNull(); // 没有减少，不出现最终审批人输入框
    const { url, body, summary } = await save(puts);
    expect(summary).toContain("增加一号综合体");
    expect(summary).toContain("减少（须审批）—");
    expect(url.endsWith("/api/v1/access/roles/role-two/viewable-projects")).toBe(true);
    expect(deltaKeys(body)).toEqual(["add_viewable_project_ids"]);
    expect(body.add_viewable_project_ids).toEqual(["org_a"]);
    expect(body.operator_note).toBe("后台改可查看项目");
    expect(typeof body.idempotency_key).toBe("string");
    await waitFor(() => expect(screen.queryByTestId("access-role-edit-guided")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith("已增加 1 个可查看项目：所有持有人要各自重新审批通过，才能查看新增的项目。");
    await waitFor(() => expect(readCount("/api/v1/access/roles")).toBe(2));
  });

  it("减少一条并指定最终审批人：只提交这次减少的条目与最终审批人，提示审批通过后才生效", async () => {
    const { props, puts } = await openEdit("role-two");
    fireEvent.click(box("access-role-edit-project-org_b"));
    fireEvent.change(box("access-role-edit-final-approver"), { target: { value: " u-boss " } });
    const { body, summary } = await save(puts);
    expect(summary).toContain("增加—");
    expect(summary).toContain("减少（须审批）二号综合体");
    expect(summary).toContain("主线最终审批人u-boss");
    // be_city（减少已在待审）、org_gone（候选清单里没有）都没动，不在这次减少里。
    expect(deltaKeys(body)).toEqual(["final_approver_id", "remove_viewable_project_ids"]);
    expect(body.remove_viewable_project_ids).toEqual(["org_b"]);
    expect(body.final_approver_id).toBe("u-boss");
    await waitFor(() => expect(screen.queryByTestId("access-role-edit-guided")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith("减少的 1 个可查看项目已提交审批：审批通过后才生效，审批期间持有人仍能查看。");
  });

  it("原来逐个勾选的勾上全部：清单收起、已勾条目不清空，只提交 add_view_all_projects=true", async () => {
    const { props, puts } = await openEdit("role-two");
    fireEvent.click(box("access-role-edit-view-all"));
    expect(screen.queryByTestId("access-role-edit-projects-project")).toBeNull();
    expect((screen.getByTestId("access-role-edit-projects-toggle") as HTMLButtonElement).disabled).toBe(true);
    // 去掉再勾上：清单按原样展开，原来勾的还在。
    fireEvent.click(box("access-role-edit-view-all"));
    expect(box("access-role-edit-project-org_b").checked).toBe(true);
    expect(box("access-role-edit-project-be_city").checked).toBe(true);
    fireEvent.click(box("access-role-edit-view-all"));
    const { body, summary } = await save(puts);
    expect(summary).toContain("增加全部项目");
    expect(deltaKeys(body)).toEqual(["add_view_all_projects"]);
    expect(body.add_view_all_projects).toBe(true);
    await waitFor(() => expect(screen.queryByTestId("access-role-edit-guided")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith("已勾选“可以查看全部项目”：所有持有人要各自重新审批通过，才能查看新增的项目。");
  });

  it("原来勾着全部的去掉：清单按原样展开，提交 remove_view_all_projects=true；候选清单里没有的旧条目不算减少", async () => {
    const { props, puts } = await openEdit("role-all");
    expect(box("access-role-edit-view-all").checked).toBe(true);
    expect(screen.queryByTestId("access-role-edit-projects-project")).toBeNull();
    expect(holderRows()).toEqual([["王五", "全部项目、二号综合体", "发起重新审批"]]);
    fireEvent.click(box("access-role-edit-view-all"));
    expect(box("access-role-edit-project-org_b").checked).toBe(true);
    expect(box("access-role-edit-project-org_a").checked).toBe(false);
    fireEvent.click(box("access-role-edit-project-org_b"));
    const { body, summary } = await save(puts);
    expect(summary).toContain("减少（须审批）全部项目、二号综合体");
    expect(deltaKeys(body)).toEqual(["remove_view_all_projects", "remove_viewable_project_ids"]);
    expect(body.remove_view_all_projects).toBe(true);
    expect(body.remove_viewable_project_ids).toEqual(["org_b"]); // org_gone 没人动它，不在里面
    await waitFor(() => expect(screen.queryByTestId("access-role-edit-guided")).toBeNull());
    expect(props.setActionResult).toHaveBeenCalledWith("减少的 1 个可查看项目已提交审批：审批通过后才生效，审批期间持有人仍能查看。去掉“可以查看全部项目”已提交审批：审批通过后才生效，审批期间持有人仍能查看。");
  });

  it("发起重新审批：POST /grant-requests 带类型、持有人与角色；成功后重读现状，这一行变成「已发起，待审批」，弹窗不关", async () => {
    const { props, puts, posts, readCount } = await openEdit("role-two");
    fireEvent.click(screen.getByTestId("access-role-edit-reapprove-u-zhang"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url.endsWith("/api/v1/access/grant-requests")).toBe(true);
    expect(posts[0]!.body.request_type).toBe("role_project_reapproval");
    expect(posts[0]!.body.target_user_id).toBe("u-zhang");
    expect(posts[0]!.body.role_id).toBe("role-two");
    await waitFor(() => expect(screen.queryByTestId("access-role-edit-reapprove-u-zhang")).toBeNull());
    expect(holderRows()).toEqual([["张三", "二号综合体", "已发起，待审批"], ["李四", "二号综合体", "已发起，待审批"]]);
    expect(readCount("/access/roles/role-two/viewable-projects")).toBe(2);
    expect(screen.getByTestId("access-role-edit-guided")).toBeTruthy();
    expect(props.setActionResult).toHaveBeenCalledWith("重新审批申请已提交：agr-re-1（待处理）");
    expect(puts).toHaveLength(0);
  });

  it("缺 access.grant_request.apply：持有人照常列出，不显示「发起重新审批」", async () => {
    await openEdit("role-two", EDIT_CODES.filter((code) => code !== "access.grant_request.apply"));
    expect(holderRows()).toEqual([["张三", "二号综合体", ""], ["李四", "二号综合体", "已发起，待审批"]]);
    expect(screen.queryByTestId("access-role-edit-reapprove-u-zhang")).toBeNull();
  });

  it("后端拒绝：409 的中文提示原样显示在弹窗内，弹窗不关，页面同时报出", async () => {
    const { props, puts } = await openEdit("role-plain", EDIT_CODES, () => new Response(
      JSON.stringify({ success: false, code: "STATE_CONFLICT", detail_code: "INVALID_STATE", message: "角色的可查看项目或待审批的减少已变化，请刷新后重试" }),
      { status: 409, headers: { "content-type": "application/json" } },
    ));
    expect(screen.queryByTestId("access-role-edit-projects-project")).toBeNull(); // 没勾选过的角色：清单收起
    fireEvent.click(screen.getByTestId("access-role-edit-projects-toggle"));
    fireEvent.click(box("access-role-edit-project-org_a"));
    await save(puts);
    await waitFor(() => expect(screen.getByTestId("access-role-edit-guided-error").textContent).toContain("角色的可查看项目或待审批的减少已变化，请刷新后重试"));
    expect(screen.getByTestId("access-role-edit-save")).toBeTruthy(); // 还停在确认页
    expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("角色的可查看项目或待审批的减少已变化，请刷新后重试"));
    expect(props.setActionResult).not.toHaveBeenCalled();
  });
});
