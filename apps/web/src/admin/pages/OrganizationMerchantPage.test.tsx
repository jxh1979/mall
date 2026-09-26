// 文件版本：V0.09
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// mock 按后端真实 snake_case 响应结构造数据（platform 域 Jackson 全局 SNAKE_CASE，字段以实测响应为准），
// 并走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。字段再错位（读臆造字段名）本文件必红。
const ENTITIES = [
  { id: "org-1", entity_name: "平台经营主体", entity_role: "management_company", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
];
const MERCHANTS = [
  { id: "m-1", project_id: "p-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "lease", status: "active" },
  { id: "m-2", project_id: "p-1", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
  // M012-008：已存档商户（行内动作为取消存档/申请删除下拉）。status 取 suspended 以不改变「正常商户（1）」断言。
  { id: "m-3", project_id: "p-1", merchant_name: "已存档商户丙", contact_name: "联系人丙", status: "suspended", archived_at: "2026-07-01T00:00:00Z", delete_status: "none" },
];
const PROJECTS = [
  { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: null, venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
];

function envelope(items: unknown[], total = items.length, page = 1): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page, page_size: 500, total } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(respond?: (url: string, init?: RequestInit) => Response | undefined): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const response = respond?.(url, init);
      if (response) return Promise.resolve(response);
      // 写路径（创建/存档/删除申请）默认返回成功信封；失败场景由用例 mockImplementationOnce 覆盖。
      if (init?.method === "POST") return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "m-new" } }), { status: 200, headers: { "content-type": "application/json" } }));
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(respond?: (url: string, init?: RequestInit) => Response | undefined, overrides: Partial<AdminPageProps> = {}) {
  const fullProps: AdminPageProps = {
    api: makeApi(respond),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...overrides,
  };
  render(<OrganizationMerchantPage {...fullProps} />);
  return fullProps;
}

// 聚焦视图断言：只剩商户表（active 行），其余表/表单/KPI 卡全隐藏，横幅在场。
function expectActiveMerchantsFocusView() {
  expect(screen.getByTestId("organization-kpi-focus-banner").textContent).toContain("正在查看：本页正常商户（1）");
  expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
  expect(screen.queryByTestId("organization-merchant-row-m-2")).toBeNull();
  expect(screen.queryByTestId("organization-entities-table")).toBeNull();
  expect(screen.queryByTestId("organization-projects-table")).toBeNull();
  expect(screen.queryByTestId("organization-merchant-summary")).toBeNull();
  expect(screen.queryByTestId("organization-merchant-create-open")).toBeNull();
}

describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    // pushState 会跨用例残留 URL，逐例复位。
    window.history.replaceState({}, "", "/admin/governance/organizations");
  });

  it("点“正常商户”数字 → URL 带 ?kpi=merchants&status=active，页面进入聚焦视图", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    expect(window.location.search).toContain("kpi=merchants");
    expect(window.location.search).toContain("status=active");
    expectActiveMerchantsFocusView();
  });

  it("聚焦视图点「返回全部」→ 参数清除，恢复完整页面（三表+表单+KPI 卡都在）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    fireEvent.click(screen.getByTestId("organization-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("organization-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("organization-entities-table")).toBeTruthy();
    expect(screen.getByTestId("organization-projects-table")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-row-m-2")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-summary")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-create-open")).toBeTruthy();
  });

  it("带参刷新/直链进入 → 直接呈现聚焦视图（参数从 URL 重建）", async () => {
    window.history.replaceState({}, "", "/admin/governance/organizations?kpi=merchants&status=active");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-focus-banner")).toBeTruthy());
    expectActiveMerchantsFocusView();
  });

  it("浏览器后退（popstate）→ 退出聚焦视图回完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    expect(screen.getByTestId("organization-kpi-focus-banner")).toBeTruthy();
    // jsdom 的 history.back 异步且不稳，直接模拟浏览器后退的效果：URL 回退+popstate 事件。
    window.history.replaceState({}, "", "/admin/governance/organizations");
    fireEvent.popState(window);
    await waitFor(() => expect(screen.queryByTestId("organization-kpi-focus-banner")).toBeNull());
    expect(screen.getByTestId("organization-merchant-summary")).toBeTruthy();
  });

  it("三表按后端真实字段渲染出真实值（字段错位时单元格是“-”，本用例必红）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-entities-table")).toBeTruthy());
    // 经营主体表：主体名/统一社会信用代码/角色中文。
    const entitiesText = screen.getByTestId("organization-entities-table").textContent ?? "";
    expect(entitiesText).toContain("平台经营主体");
    expect(entitiesText).toContain("91330100MA27XW8H");
    expect(entitiesText).toContain("业态主体（管理公司）"); // M012-005：V410 三角色，不再用已删的 operation
    // 商户表：商户名/联系人/电话/区块/面积（租赁面积优先）/合作形式中文。
    const m1 = screen.getByTestId("organization-merchant-row-m-1").textContent ?? "";
    expect(m1).toContain("正常商户甲");
    expect(m1).toContain("联系人甲");
    expect(m1).toContain("13800000001");
    expect(m1).toContain("1F-A");
    expect(m1).toContain("120");
    expect(m1).toContain("租赁"); // M012-005：合作方式六类，不再用已退役的 fixed_rent
    // 项目表：项目名/编码/商户下钻（merchant_id）/业态。
    const projectsText = screen.getByTestId("organization-projects-table").textContent ?? "";
    expect(projectsText).toContain("项目一");
    expect(projectsText).toContain("PJ-1");
    expect(projectsText).not.toContain("m-1");
    expect(screen.getByTestId("organization-merchant-row-m-1").textContent).toContain("项目一");
    // M012-004：业态列中文（PGM-001 零英文），不再断言英文码。
    expect(projectsText).toContain("酒吧");
    expect(projectsText).not.toMatch(/\bbar\b/);
  });

  it("KPI 子标签渲染为中文，不含英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-merchant-summary")).toBeTruthy());
    const text = screen.getByTestId("organization-merchant-summary").textContent ?? "";
    expect(text).not.toContain("merchant profiles");
    expect(text).not.toContain("projects");
    expect(text).not.toContain("active");
    expect(text).toContain("商户档案数");
    expect(text).toContain("项目数");
    expect(text).toContain("正常商户数");
  });

  it("引导式弹窗内商户类型/合作形式下拉显示中文，不含英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-merchant-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-merchant-create-open"));
    // 第一步：基本信息（商户类型下拉在此步）。
    // V348/V384 业态六值：酒吧/餐饮/零售/游乐/传媒/其他，全中文、不预选（必填）。M012-005
    const typeText = screen.getByTestId("organization-merchant-type").textContent ?? "";
    expect(typeText).toContain("酒吧");
    expect(typeText).toContain("餐饮");
    expect(typeText).toContain("游乐");
    expect(typeText).toContain("传媒");
    expect(typeText).toContain("其他");
    expect(typeText).not.toContain("amusement");
    expect(typeText).not.toContain("catering");
    // 单屏模式：经营与租赁（合作形式下拉）与基本信息同屏，无需步进。
    const coopText = screen.getByTestId("organization-merchant-cooperation").textContent ?? "";
    // M009-003：合作形式下拉只提供 HTML 六类（自营/租赁/联营/代运营/服务商接入/其他），不再是收费模板码。
    expect(coopText).toContain("租赁");
    expect(coopText).toContain("服务商接入");
    expect(coopText).not.toContain("固定租金");
    expect(coopText).not.toContain("fixed_rent");
    expect(coopText).not.toContain("revenue_share");
  });

  it("引导式创建流：分步校验拦截 → 确认页汇总 → 创建提交并关闭弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-merchant-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-merchant-create-open"));

    // 单屏模式：所有字段同屏，「下一步」一次跑完各步校验（按字段顺序命中）。
    // 商户名空 → 命中商户名校验。
    fireEvent.change(screen.getByTestId("organization-merchant-name"), { target: { value: " " } });
    fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
    expect(screen.getByTestId("organization-merchant-flow-error").textContent).toContain("商户名称不能为空");
    // 补商户名但业态未选 → 命中业态必填校验（V348：业态是通票分账拆份额的依据，不给默认值）。
    fireEvent.change(screen.getByTestId("organization-merchant-name"), { target: { value: "引导流程商户" } });
    fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
    expect(screen.getByTestId("organization-merchant-flow-error").textContent).toContain("必须选择商户业态");
    // 选业态但电话为空 → 命中「联系电话不能为空」（M012-007：不再前端写死 11 位大陆手机号规则；M012-006：电话不预填）。
    fireEvent.change(screen.getByTestId("organization-merchant-type"), { target: { value: "bar" } });
    expect((screen.getByTestId("organization-merchant-contact-phone") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
    expect(screen.getByTestId("organization-merchant-flow-error").textContent).toContain("联系电话不能为空");
    // 修正电话 → 下一步进确认页。
    fireEvent.change(screen.getByTestId("organization-merchant-contact-phone"), { target: { value: "13912345678" } });
    fireEvent.click(screen.getByTestId("organization-merchant-flow-next"));
    const summary = screen.getByTestId("organization-merchant-flow-summary");
    expect(summary.textContent).toContain("引导流程商户");
    expect(summary.textContent).toContain("13912345678");
    expect(summary.textContent).not.toContain("fixed_rent");

    // 确认页点「创建商户档案」→ POST 发出、弹窗关闭。
    fireEvent.click(screen.getByTestId("organization-merchant-create"));
    await waitFor(() => expect(screen.queryByTestId("organization-merchant-flow")).toBeNull());
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    const postCall = fetchMock.mock.calls.find(([, init]) => (init as RequestInit | undefined)?.method === "POST");
    expect(postCall).toBeTruthy();
    expect(String(postCall![0])).toContain("/api/v1/platform/merchants");
    const body = String((postCall![1] as RequestInit).body);
    expect(body).toContain("引导流程商户");
    // M012-006：不再静默提交验收默认值/假主体/假证据/租金面积。
    expect(body).not.toContain("contract-subject-1");
    expect(body).not.toContain("att-merchant-profile-create");
    expect(body).not.toContain("1F-A");
    expect(body).not.toContain("二期测试商户");
  });
});

describe("OrganizationMerchantPage 行明细抽屉（三业务表整行可点）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/organizations");
  });

  it("点经营主体行 → 抽屉含主体名/信用代码/角色中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-entity-row-org-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-entity-row-org-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("平台经营主体");
    expect(drawer.textContent).toContain("91330100MA27XW8H");
    expect(drawer.textContent).toContain("业态主体（管理公司）");
  });

  it("点商户行 → 抽屉含商户名/联系人/合作形式中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-merchant-row-m-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("正常商户甲");
    expect(drawer.textContent).toContain("联系人甲");
    expect(drawer.textContent).toContain("租赁");
    expect(drawer.textContent).not.toContain("fixed_rent");
  });

  it("点项目行 → 抽屉含项目名/编码；抽屉零英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-project-row-p-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-project-row-p-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("项目一");
    expect(drawer.textContent).toContain("PJ-1");
  });

  it("Esc 关闭抽屉 + 点遮罩关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-merchant-row-m-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
    fireEvent.click(screen.getByTestId("organization-entity-row-org-1"));
    fireEvent.click(screen.getByTestId("row-detail-overlay"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

describe("OrganizationMerchantPage 存档/删除写路径（M012-008）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/governance/organizations");
  });

  function postCalls() {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    return fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === "POST");
  }

  it("存档：空理由被拦截不发请求（BSM-004 存档须填理由）；填写理由后 POST /merchants/{id}/archive 且请求体含理由", async () => {
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("   ");
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-archive-m-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-archive-m-1"));
    expect(props.setGlobalError).toHaveBeenCalledWith("存档理由必填");
    expect(postCalls()).toHaveLength(0);

    prompt.mockReturnValue("退场清理");
    fireEvent.click(screen.getByTestId("merchant-archive-m-1"));
    await waitFor(() => expect(postCalls()).toHaveLength(1));
    const [url, init] = postCalls()[0]!;
    expect(String(url)).toContain("/api/v1/platform/merchants/m-1/archive");
    expect(String((init as RequestInit).body)).toContain("退场清理");
  });

  it("已存档商户：申请删除 → POST /merchants/{id}/delete-request 带理由", async () => {
    vi.spyOn(window, "prompt").mockReturnValue("录入错误，退场清理");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-actions-m-3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-actions-m-3"));
    await waitFor(() => expect(screen.getByTestId("merchant-delete-request-m-3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-delete-request-m-3"));
    // M012-019：申请框须说明密钥本人会直接执行，不能承诺所有提交均待审批。
    expect(window.prompt).toHaveBeenCalledWith("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
    await waitFor(() => expect(postCalls()).toHaveLength(1));
    const [url, init] = postCalls()[0]!;
    expect(String(url)).toContain("/api/v1/platform/merchants/m-3/delete-request");
    expect(String((init as RequestInit).body)).toContain("录入错误，退场清理");
  });

  it("写路径失败可见：存档接口报错 → setGlobalError 收到错误信息，不成未处理拒绝", async () => {
    vi.spyOn(window, "prompt").mockReturnValue("退场清理");
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-archive-m-1")).toBeTruthy());
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockImplementationOnce(() => Promise.resolve(new Response(
      JSON.stringify({ success: false, code: "MERCHANT_NOT_FOUND", message: "商户不存在" }),
      { status: 404, headers: { "content-type": "application/json" } },
    )));
    fireEvent.click(screen.getByTestId("merchant-archive-m-1"));
    // ApiError.message 经 shared/apiError 翻译层改写为中文业务话术（不一定含后端原文），此处只锁「失败被 catch 并以字符串报给 setGlobalError」。
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.any(String)));
  });
});


describe("M012 本次组织档案回归", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); window.history.replaceState({}, "", "/admin/governance/organizations"); });

  it("缺主体查看权仍展示商户且不请求主体接口", async () => {
    renderPage(undefined, { effectivePermissions: { status: "ready", value: { grants: ["merchant.profile.view", "governance.project.view"], denies: [], wildcard: false } } });
    expect(await screen.findByTestId("organization-merchant-row-m-1")).toBeTruthy();
    expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).includes("/business-entities"))).toBe(false);
  });

  it("暂停、待准入和黑名单是不同的中文商户状态", async () => {
    renderPage((url) => url.includes("/merchants")
      ? envelope([{ ...MERCHANTS[0], status: "pending_admission" }, MERCHANTS[1], { ...MERCHANTS[2], status: "blacklisted" }])
      : undefined);
    expect((await screen.findByTestId("organization-merchant-row-m-1")).textContent).toContain("待准入");
    expect(screen.getByTestId("organization-merchant-row-m-2").textContent).toContain("暂停");
    expect(screen.getByTestId("organization-merchant-row-m-3").textContent).toContain("黑名单");
    expect(screen.getByTestId("organization-merchants-table").textContent).not.toContain("已封禁");
  });

  it("分页保留商户归档和业态筛选参数，后页商户可达", async () => {
    // 页面按接口返回的当页条目渲染、按 total/500 算页数：第 1 页只回两条代表行、total 仍为 501，下一页照样可用。
    // 原先第 1 页回满 500 行，本用例两次渲染 500 行商户表（每行含操作列），160 满载时超 5 秒（flaky-org-merchant.md）。
    const pages: Record<number, unknown[]> = {
      1: [0, 1].map((i) => ({ ...MERCHANTS[0], id: "m-page-" + i, merchant_name: "分页商户" + i })),
      2: [{ ...MERCHANTS[0], id: "m-page-500", merchant_name: "分页商户500" }],
    };
    renderPage((url) => {
      if (!url.includes("/merchants")) return undefined;
      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
      return envelope(pages[page] ?? [], 501, page);
    });
    await screen.findByTestId("organization-merchant-row-m-page-0");
    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
    expect(await screen.findByTestId("organization-merchant-row-m-page-500")).toBeTruthy();
    const pageTwoUrl = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).find(url => url.includes("/merchants") && url.includes("page=2"));
    expect(pageTwoUrl).toContain("includeArchived=false");
    expect(screen.getByTestId("organization-merchants-pager-status").textContent).toContain("共 501 条");
    fireEvent.click(screen.getByTestId("merchant-show-archived").querySelector("input")!);
    fireEvent.change(await screen.findByTestId("merchant-type-filter"), { target: { value: "retail" } });
    await waitFor(() => {
      const urls = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter(url => url.includes("/merchants"));
      const params = new URL(urls.at(-1)!, "http://localhost").searchParams;
      expect(params.get("includeArchived")).toBe("true");
      expect(params.get("merchantType")).toBe("retail");
      expect(params.get("page")).toBe("1");
    });
    await screen.findByTestId("organization-merchant-row-m-page-0");
    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
    await screen.findByTestId("organization-merchant-row-m-page-500");
    const urls = vi.mocked(fetch).mock.calls.map(([url]) => String(url)).filter(url => url.includes("/merchants"));
    expect(urls.at(-1)).toContain("merchantType=retail");
    expect(urls.at(-1)).toContain("includeArchived=true");

  });

  it("商户存档按钮用Enter触发原操作而不打开行抽屉", async () => {
    const prompt = vi.spyOn(window, "prompt").mockReturnValue(null);
    renderPage();
    const button = await screen.findByTestId("merchant-archive-m-1");
    button.focus();
    await userEvent.setup().keyboard("{Enter}");
    expect(prompt).toHaveBeenCalledWith("存档（下线）理由：");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
  it("存档最后一页的唯一商户后回到仍有记录的第一页", async () => {
    let archived = false;
    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-last-" + i }));
    renderPage((url, init) => {
      if (init?.method === "POST" && url.includes("/archive")) { archived = true; return envelope([]); }
      if (!url.includes("/merchants")) return undefined;
      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
      const current = archived ? rows.slice(0, 500) : rows;
      return envelope(current.slice((page - 1) * 500, page * 500), current.length, page);
    });
    await screen.findByTestId("organization-merchant-row-m-last-0");
    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
    await screen.findByTestId("organization-merchant-row-m-last-500");
    vi.spyOn(window, "prompt").mockReturnValue("结束经营");
    fireEvent.click(screen.getByTestId("merchant-archive-m-last-500"));
    await waitFor(() => expect(screen.getByTestId("organization-merchants-pager-status").textContent).toContain("第 1 / 1 页"));
    expect(await screen.findByTestId("organization-merchant-row-m-last-0")).toBeTruthy();
  });

});


it("M012-013：重试成功后清除全局读取错误", async () => {
  let failed = false;
  const props = renderPage((url, init) => {
    if (init?.method !== "POST" && url.includes("/merchants") && !failed) {
      failed = true;
      return new Response(JSON.stringify({ success: false, code: "TEMPORARY_ERROR", message: "临时读取失败" }), { status: 503, headers: { "content-type": "application/json" } });
    }
    return undefined;
  });
  await screen.findByTestId("admin-retry");
  expect(props.setGlobalError).toHaveBeenCalledWith("服务暂时不可用，请稍后重试");
  vi.mocked(props.setGlobalError).mockClear();
  fireEvent.click(screen.getByTestId("admin-retry"));
  await screen.findByTestId("organization-merchants-table");
  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
});

describe("M012-018：下钻提示只报告当前可见商户", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/governance/organizations");
  });

  it("后页目标未加载时不称已定位，翻页看到目标后才显示成功", async () => {
    const rows = Array.from({ length: 501 }, (_, i) => ({ ...MERCHANTS[0], id: "m-focus-" + i }));
    renderPage((url, init) => {
      if (init?.method === "POST" || !url.includes("/merchants")) return undefined;
      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
    }, { focusObjectId: "m-focus-500" });
    expect(screen.getByTestId("organization-merchant-focus").textContent).not.toContain("已从下钻定位到商户");
    await screen.findByTestId("organization-merchant-row-m-focus-0");
    expect(screen.queryByTestId("organization-merchant-row-m-focus-500")).toBeNull();
    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
    fireEvent.click(screen.getByTestId("organization-merchants-pager-next"));
    await screen.findByTestId("organization-merchant-row-m-focus-500");
    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：m-focus-500");
  });

  it.each([
    ["?kpi=merchants&status=active", "m-2", "organization-merchants-table"],
    ["?kpi=projects", "m-1", "organization-projects-table"],
  ])("目标被当前视图隐藏时不称已定位：%s", async (query, target, table) => {
    window.history.replaceState({}, "", "/admin/governance/organizations" + query);
    renderPage(undefined, { focusObjectId: target });
    await screen.findByTestId(table);
    expect(screen.queryByTestId("organization-merchant-row-" + target)).toBeNull();
    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("当前列表尚未定位到该商户，请检查筛选或翻页");
    fireEvent.click(screen.getByTestId("organization-kpi-focus-clear"));
    await screen.findByTestId("organization-merchant-row-" + target);
    expect(screen.getByTestId("organization-merchant-focus").textContent).toContain("已从下钻定位到商户：" + target);
  });
});
