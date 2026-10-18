// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdmissionAuditsPage } from "./AdmissionAuditsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const MERCHANTS = [{ id: "m-1", display_name: "商户甲", company_name: "甲公司", cooperation_form: "fixed_rent", risk_level: "low", status: "active" }];
const BINDINGS = [{ id: "b-1", merchant_id: "m-1", operation_entity_id: "operation-1", contract_entity_id: "contract-entity-1", settlement_entity_id: "settlement-1", status: "active", effective_from: "2026-01-01", effective_to: "2026-12-31", change_reason: "合同主体确认" }];
const CONTRACTS = [{ id: "c-1", merchant_id: "m-1", contract_subject_id: "cs-1", operation_subject_id: "os-1", settlement_subject_id: "ss-1", lifecycle_status: "effective", status: "effective" }];

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
      if (url.includes("business-entity-bindings")) return Promise.resolve(envelope(BINDINGS));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("contracts")) return Promise.resolve(envelope(CONTRACTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(navigateTo = vi.fn()) {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    navigateTo,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AdmissionAuditsPage {...fullProps} />);
}

describe("AdmissionAuditsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/admission-audits");
  });

  it("真实 snake_case 绑定字段显示三类主体编号并关联真实商户", async () => {
    const navigateTo = vi.fn();
    renderPage(navigateTo);
    const row = await screen.findByTestId("admission-audits-binding-row-b-1");
    for (const value of ["m-1", "operation-1", "contract-entity-1", "settlement-1", "b-1", "合同主体确认"]) {
      expect(row.textContent).toContain(value);
    }
    const merchantLink = within(row).getByRole("button", { name: "m-1" });
    fireEvent.keyDown(merchantLink, { key: "Enter" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.click(merchantLink);
    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "m-1");
  });

  it("三类资料按 merchant_id 关联；有草稿也只说明资料存在", async () => {
    const api = makeApi();
    vi.mocked(fetch).mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("business-entity-bindings")) return Promise.resolve(envelope([
        ...BINDINGS,
        { ...BINDINGS[0], id: "m-2", merchant_id: "other-merchant" },
        { ...BINDINGS[0], id: "b-duplicate" },
      ]));
      if (url.includes("merchants")) return Promise.resolve(envelope([...MERCHANTS, { ...MERCHANTS[0], id: "m-2" }]));
      if (url.includes("contracts")) return Promise.resolve(envelope([
        { ...CONTRACTS[0], lifecycle_status: "draft", status: "draft" },
        { ...CONTRACTS[0], id: "c-2", merchant_id: "m-2" },
      ]));
      return Promise.resolve(envelope([]));
    });
    render(<AdmissionAuditsPage api={api} user={{ id: "u1", username: "admin", accountType: "platform_admin" }} setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    const summary = await screen.findByTestId("admission-audits-summary");
    const card = within(summary).getByText("三类资料关联").closest(".admin-kpi-card");
    expect(card?.querySelector("strong")?.textContent).toBe("1");
    expect(card?.textContent).toContain("不代表审批通过");
    expect(summary.textContent).not.toContain("准入闭环");
  });

  it("点“合同审批”数字 → ?kpi=contracts，只剩合同表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-contracts")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-kpi-contracts"));
    expect(window.location.search).toContain("kpi=contracts");
    expect(screen.getByTestId("admission-audits-kpi-focus-banner").textContent).toContain("正在查看：合同审批（1）");
    expect(screen.getByTestId("admission-audits-contracts-table")).toBeTruthy();
    expect(screen.queryByTestId("admission-audits-merchants-table")).toBeNull();
    expect(screen.queryByTestId("admission-audits-bindings-table")).toBeNull();
    expect(screen.queryByTestId("admission-audits-summary")).toBeNull();
  });

  it("点“主体绑定”数字 → 只剩绑定表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-bindings")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-kpi-bindings"));
    expect(screen.getByTestId("admission-audits-bindings-table")).toBeTruthy();
    expect(screen.queryByTestId("admission-audits-contracts-table")).toBeNull();
  });

  it("「返回全部」→ 恢复三表+KPI 卡", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-contracts")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-kpi-contracts"));
    fireEvent.click(screen.getByTestId("admission-audits-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("admission-audits-merchants-table")).toBeTruthy();
    expect(screen.getByTestId("admission-audits-bindings-table")).toBeTruthy();
    expect(screen.getByTestId("admission-audits-contracts-table")).toBeTruthy();
    expect(screen.getByTestId("admission-audits-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/governance/admission-audits?kpi=merchants");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("admission-audits-merchants-table")).toBeTruthy();
    expect(screen.queryByTestId("admission-audits-contracts-table")).toBeNull();
  });

  it("合作形式/合同状态渲染为中文，不含英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-merchants-table")).toBeTruthy());
    expect(screen.getByTestId("admission-audits-merchants-table").textContent).toContain("固定租金");
    expect(screen.getByTestId("admission-audits-merchants-table").textContent).not.toContain("fixed_rent");
    expect(screen.getByTestId("admission-audits-contracts-table").textContent).toContain("生效中");
    expect(screen.getByTestId("admission-audits-contracts-table").textContent).not.toContain("effective");
  });
});

describe("AdmissionAuditsPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/admission-audits");
  });

  it("点合同行 → 抽屉含该行状态（中文）与商户，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-contract-row-c-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-contract-row-c-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("生效中");
    expect(drawer.textContent).toContain("m-1");
    expect(drawer.textContent).not.toContain("effective");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点主体绑定行 → 抽屉含该行绑定主体", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-binding-row-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-binding-row-b-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    for (const value of ["m-1", "operation-1", "contract-entity-1", "settlement-1", "b-1", "合同主体确认", "2026年1月1日", "2026年12月31日"]) {
      expect(drawer.textContent).toContain(value);
    }
    expect(drawer.textContent).not.toContain("留痕证据");
  });
});

// M725-F02（治理地图第 303 行「同一个操作被重复提交，系统只认一次」）：登记导出留痕第一次提交服务端可能已登记、响应却丢了，
// 弹窗留着，操作员再点——这是同一次登记，不是新的一次：发生时间沿用第一次提交时取的，请求内容逐字不变，带首发的提交编号。
describe("AdmissionAuditsPage 登记导出留痕没拿到结果再点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/admission-audits");
  });

  it("第二次请求的内容与提交编号都与第一次相同", async () => {
    // 页面按点击取当前时刻、用「前缀-毫秒」现生成编号；让每次取到的时刻与毫秒必然不同。
    const realToISOString = Date.prototype.toISOString;
    let now = 1_760_000_000_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
    vi.spyOn(Date.prototype, "toISOString").mockImplementation(() => realToISOString.call(new Date((now += 1000))));
    const posts: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if ((init?.method ?? "GET").toUpperCase() === "POST") {
          posts.push(String(init?.body));
          // 第一次：响应丢在路上。
          if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
          return Promise.resolve(new Response(
            JSON.stringify({ success: true, code: "OK", message: "success", data: { accepted_count: 1 } }),
            { status: 200, headers: { "content-type": "application/json" } },
          ));
        }
        if (url.includes("business-entity-bindings")) return Promise.resolve(envelope(BINDINGS));
        if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
        if (url.includes("contracts")) return Promise.resolve(envelope(CONTRACTS));
        return Promise.resolve(envelope([]));
      }),
    );
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AdmissionAuditsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("admission-audits-create-open")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("admission-audits-create-open"));
    fireEvent.click(screen.getByTestId("admission-audits-guided-next"));
    await waitFor(() => expect(screen.getByTestId("admission-audits-guided-summary")).toBeTruthy(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("admission-audits-export"));
    await waitFor(() => expect(screen.getByTestId("admission-audits-guided-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    await waitFor(() => expect(screen.getByTestId("admission-audits-export").className).not.toContain("ant-btn-loading"), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("admission-audits-export"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(JSON.parse(posts[1]!).events[0].occurred_at).toBe(JSON.parse(posts[0]!).events[0].occurred_at);
    expect(JSON.parse(posts[1]!).idempotency_key).toBe(JSON.parse(posts[0]!).idempotency_key);
    expect(posts[1]).toBe(posts[0]);
  });

  // 记住的发生时间跟着「这次登记」走，不跟着页面组件走：第一次没拿到结果后切到别的菜单再回来（页面重新挂载），
  // 同样的内容再登记——仍是同一次登记。
  it("切页回来再登记：请求内容与提交编号仍与第一次相同", async () => {
    // 页面按点击取当前时刻、用「前缀-毫秒」现生成编号；让每次取到的时刻与毫秒必然不同。
    const realToISOString = Date.prototype.toISOString;
    let now = 1_760_000_000_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
    vi.spyOn(Date.prototype, "toISOString").mockImplementation(() => realToISOString.call(new Date((now += 1000))));
    const posts: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if ((init?.method ?? "GET").toUpperCase() === "POST") {
          posts.push(String(init?.body));
          // 第一次：响应丢在路上。
          if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
          return Promise.resolve(new Response(
            JSON.stringify({ success: true, code: "OK", message: "success", data: { accepted_count: 1 } }),
            { status: 200, headers: { "content-type": "application/json" } },
          ));
        }
        if (url.includes("business-entity-bindings")) return Promise.resolve(envelope(BINDINGS));
        if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
        if (url.includes("contracts")) return Promise.resolve(envelope(CONTRACTS));
        return Promise.resolve(envelope([]));
      }),
    );
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    // 打开引导弹窗、填导出原因、核对、提交。
    async function submitExport() {
      await waitFor(() => expect(screen.getByTestId("admission-audits-create-open")).toBeTruthy(), { timeout: 5000 });
      fireEvent.click(screen.getByTestId("admission-audits-create-open"));
      fireEvent.change(screen.getByTestId("admission-audits-export-reason"), { target: { value: "切页回来再登记" } });
      fireEvent.click(screen.getByTestId("admission-audits-guided-next"));
      await waitFor(() => expect(screen.getByTestId("admission-audits-guided-summary")).toBeTruthy(), { timeout: 5000 });
      fireEvent.click(screen.getByTestId("admission-audits-export"));
    }
    const firstVisit = render(<AdmissionAuditsPage {...props} />);
    await submitExport();
    await waitFor(() => expect(screen.getByTestId("admission-audits-guided-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    // 切到别的菜单再回来：页面重新挂载。
    firstVisit.unmount();
    render(<AdmissionAuditsPage {...props} />);
    await submitExport();
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(JSON.parse(posts[1]!).events[0].occurred_at).toBe(JSON.parse(posts[0]!).events[0].occurred_at);
    expect(JSON.parse(posts[1]!).idempotency_key).toBe(JSON.parse(posts[0]!).idempotency_key);
    expect(posts[1]).toBe(posts[0]);
  });
});
