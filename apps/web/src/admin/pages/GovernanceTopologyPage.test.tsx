// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// mock 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。platform 域 snake_case 响应。
// M012-005：夹具只用现行闭集——主体角色 platform/project_company/management_company（V410），空间类型使用现行字典。
const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "management_company", status: "active" }];
const MERCHANTS = [
  { id: "m-1", project_id: "p-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
  { id: "m-2", project_id: "p-1", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
];
const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: null, venue_type: "bar", status: "active" }];
// M012-003：后台 SpaceRecord 以 project_id 挂接项目（无 venue_id）。
const SPACES = [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: "L1", area_square_meter: 120, space_type: "shop", status: "active" }];

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
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
      if (url.includes("spaces")) return Promise.resolve(envelope(SPACES));
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
  render(<GovernanceTopologyPage {...fullProps} />);
  return fullProps;
}

describe("GovernanceTopologyPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/topology");
  });

  it("点“空间点位”数字 → URL 带 ?kpi=spaces，只剩空间表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-spaces")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-kpi-spaces"));
    expect(window.location.search).toContain("kpi=spaces");
    expect(screen.getByTestId("governance-topology-kpi-focus-banner").textContent).toContain("正在查看：空间点位（1）");
    expect(screen.getByTestId("governance-topology-spaces-table")).toBeTruthy();
    expect(screen.queryByTestId("governance-topology-table")).toBeNull();
    expect(screen.queryByTestId("governance-topology-summary")).toBeNull();
  });

  it("点“商户”数字 → 只剩主体-商户-项目表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-kpi-merchants"));
    expect(window.location.search).toContain("kpi=merchants");
    expect(screen.getByTestId("governance-topology-table")).toBeTruthy();
    expect(screen.queryByTestId("governance-topology-spaces-table")).toBeNull();
  });

  it("「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-spaces")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-kpi-spaces"));
    fireEvent.click(screen.getByTestId("governance-topology-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("governance-topology-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("governance-topology-table")).toBeTruthy();
    expect(screen.getByTestId("governance-topology-spaces-table")).toBeTruthy();
    expect(screen.getByTestId("governance-topology-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/governance/topology?kpi=spaces");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("governance-topology-spaces-table")).toBeTruthy();
    expect(screen.queryByTestId("governance-topology-table")).toBeNull();
  });

  it.each([["active", "已启用"], ["suspended", "暂停"]])("项目状态%s显示为%s", async (status, label) => {
    renderPage((url) => url.includes("/projects") ? envelope([{ ...PROJECTS[0], status }]) : undefined);
    await waitFor(() => expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
    const text = screen.getByTestId("governance-topology-table").textContent ?? "";
    expect(text).toContain(label);
    expect(text).not.toContain("已封禁");
    expect(text).not.toContain("suspended");
  });
});

describe("GovernanceTopologyPage 真实字段与全中文（M012-003/004）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/topology");
  });

  it("项目业态与空间类型渲染为中文，不直出 bar/shop 英文码（PGM-001）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
    const topology = screen.getByTestId("governance-topology-table").textContent ?? "";
    expect(topology).toContain("酒吧");
    expect(topology).not.toMatch(/\bbar\b/);
    const spaces = screen.getByTestId("governance-topology-spaces-table").textContent ?? "";
    expect(spaces).toContain("商铺/铺位");
    expect(spaces).not.toMatch(/\bshop\b/);
    // M012-004：空间状态闭集 active/unavailable 页内映射「可用/不可用」（M009-036 口径）。
    expect(spaces).toContain("可用");
  });

  it("「已挂接」按后台 project_id 计数（读臆造 venue_id 时恒为 0，本用例必红）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-summary")).toBeTruthy());
    expect(screen.getByTestId("governance-topology-summary").textContent).toContain("1 个已挂接");
  });

});

describe("GovernanceTopologyPage 行明细抽屉（拓扑表 / 空间表整行可点）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/topology");
  });

  it("点拓扑关系行 → 抽屉含主体/商户/项目与中文状态、中文业态", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-row-m-1-p-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-row-m-1-p-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("平台经营主体");
    expect(drawer.textContent).toContain("商户甲");
    expect(drawer.textContent).toContain("项目一");
    expect(drawer.textContent).toContain("已启用");
    expect(drawer.textContent).toContain("酒吧");
    expect(drawer.textContent).not.toContain("active");
  });

  it("点空间点位行 → 抽屉含空间名/所属项目/面积/中文空间类型", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-space-row-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-space-row-s-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("空间甲");
    expect(drawer.textContent).toContain("项目一");
    expect(drawer.textContent).toContain("120");
    expect(drawer.textContent).toContain("商铺/铺位");
  });

  it("Esc 关闭抽屉 + 点遮罩关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-space-row-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-space-row-s-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
    fireEvent.click(screen.getByTestId("governance-topology-row-m-1-p-1"));
    fireEvent.click(screen.getByTestId("row-detail-overlay"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

describe("GovernanceTopologyPage 无 entity.binding.view 角色不请求主体（根因修 AUD-auxload·防白屏）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/topology");
  });

  it("缺主体查看权限：business-entities 端点根本不请求，整页不白屏，商户/项目/空间照常渲染", async () => {
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      // business-entities 若被请求本会 403；这里给它也能成功，以证明"没请求"而非"请求了被吞"
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
      if (url.includes("spaces")) return Promise.resolve(envelope(SPACES));
      return Promise.resolve(envelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const SCOPED: EffectivePermissionsState = {
      status: "ready",
      value: { grants: ["governance.project.view", "governance.space.view", "merchant.profile.view"], denies: [], wildcard: false },
    };
    const setGlobalError = vi.fn();
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "viewer", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError,
      effectivePermissions: SCOPED,
    };
    render(<GovernanceTopologyPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-merchants")).toBeTruthy());
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    // 缺权限副资源：根本不发请求（不再"请求→403→reject 整个 Promise.all→白屏"）
    expect(urls.some((u) => u.includes("business-entities"))).toBe(false);
    // 有权限的主内容仍请求、页面正常渲染
    expect(urls.some((u) => u.includes("merchants"))).toBe(true);
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
    const row = screen.getByTestId("governance-topology-row-m-1-p-1");
    expect(row.querySelector("td")?.textContent).toBe("-");
    fireEvent.click(row);
    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("主体资料未加载");
    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("平台经营主体");
  });
});


describe("M012 本次归并回归", () => {
  afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/admin/governance/topology"); });

  it("商户按真实 project_id 归属当前综合体，项目没有反向 merchant_id 也能显示", async () => {
    renderPage();
    expect((await screen.findByTestId("governance-topology-row-m-2-p-1")).textContent).toContain("项目一");
  });

  it("现有自定义空间类型按字典中文名称展示", async () => {
    renderPage((url) => url.includes("/space-types")
      ? envelope([{ id: "type-kiosk", type_code: "kiosk", type_name: "一体机位", status: "active" }])
      : url.includes("/spaces") ? envelope([{ ...SPACES[0], space_type: "kiosk" }]) : undefined);
    const row = await screen.findByTestId("governance-space-row-s-1");
    expect(row.textContent).toContain("一体机位");
    expect(row.textContent).not.toContain("kiosk");
  });

  it.each(["merchants", "spaces"])("超过默认500条的%s能看到总数并读取第二页", async (resource) => {
    const rows = Array.from({ length: 501 }, (_, i) => resource === "merchants"
      ? { ...MERCHANTS[0], id: "m-page-" + i, merchant_name: "分页商户" + i }
      : { ...SPACES[0], id: "s-page-" + i, space_name: "分页空间" + i });
    renderPage((url) => {
      if (!url.includes("/" + resource)) return undefined;
      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
    });
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-" + resource).textContent).toBe("501"));
    fireEvent.click(screen.getByTestId("governance-" + resource + "-pager-next"));
    const last = resource === "merchants" ? "governance-topology-row-m-page-500-p-1" : "governance-space-row-s-page-500";
    expect(await screen.findByTestId(last)).toBeTruthy();
  });

  it("商户来源按钮可以用Enter下钻，行自身Enter仍打开详情", async () => {
    const navigateTo = vi.fn();
    renderPage(undefined, { navigateTo });
    const button = await screen.findByTestId("trace-link-ADM-GOV-002-m-1");
    button.focus();
    await userEvent.setup().keyboard("{Enter}");
    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-1");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.keyDown(screen.getByTestId("governance-topology-row-m-1-p-1"), { key: "Enter" });
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
  });
  it("主体资料在第二页时显示未加载，翻页后呈现真实主体", async () => {
    const rows = Array.from({ length: 501 }, (_, i) => ({ ...ENTITIES[0], id: "org-page-" + i, entity_name: "经营主体" + i }));
    renderPage((url) => {
      if (url.includes("/merchants")) return envelope([{ ...MERCHANTS[0], business_entity_id: "org-page-500" }]);
      if (!url.includes("/business-entities")) return undefined;
      const page = Number(new URL(url, "http://localhost").searchParams.get("page") || 1);
      return envelope(rows.slice((page - 1) * 500, page * 500), 501, page);
    });
    const row = await screen.findByTestId("governance-topology-row-m-1-p-1");
    expect(row.textContent).toContain("主体资料未加载");
    fireEvent.click(screen.getByTestId("governance-entities-pager-next"));
    await waitFor(() => expect(screen.getByTestId("governance-topology-row-m-1-p-1").textContent).toContain("经营主体500"));
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
  await screen.findByTestId("governance-topology-table");
  expect(props.setGlobalError).toHaveBeenLastCalledWith(null);
});
