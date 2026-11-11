import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AuditEventsPage } from "./AuditEventsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, AuditEventRow, EffectivePermissionsState } from "../types";

const ROWS: AuditEventRow[] = [
  { id: "e1", eventType: "login", eventCategory: "auth", result: "success", traceId: "t-1", summary: "登录成功" },
  { id: "e2", eventType: "block", eventCategory: "security", result: "blocked", traceId: "t-2", summary: "越权阻断" },
];

function pagedResolver() {
  // total=42 > pageSize，可翻页；当前页 2 行。
  return vi.fn().mockResolvedValue({ items: ROWS, total: 42, page: 1, size: 10 });
}

function makeApi(getPaged = pagedResolver()): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([]),
    getPaged,
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AuditEventsPage {...fullProps} />);
}

describe("AuditEventsPage 服务端分页", () => {
  it("首次渲染调用 getPaged 且事件总数用服务端 total", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("page=1")));
    // 事件总数 KPI 用服务端 total（42），而非当前页行数（2）。
    expect(screen.getByTestId("audit-events-summary").textContent).toContain("42");
  });

  it("翻页调用 getPaged 带 page=2（可翻更早历史）", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("audit-events-pager-next"));
    await waitFor(() => expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("page=2")));
  });
});

describe("AuditEventsPage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  it("点“本页成功”数字 → ?kpi=events&status=success，横幅+表只剩成功子集，KPI 卡/控件隐藏", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-success")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-kpi-success"));
    expect(window.location.search).toContain("kpi=events");
    expect(window.location.search).toContain("status=success");
    expect(screen.getByTestId("audit-events-kpi-focus-banner").textContent).toContain("正在查看：本页成功事件（1）");
    expect(screen.queryByTestId("audit-events-summary")).toBeNull();
    expect(screen.queryByTestId("audit-events-controls")).toBeNull();
    const text = screen.getByTestId("audit-events-table").textContent ?? "";
    expect(text).toContain("登录成功");
    expect(text).not.toContain("越权阻断");
  });

  it("点“本页失败/阻断”数字 → 失败子集(1)", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-failed")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-kpi-failed"));
    expect(screen.getByTestId("audit-events-kpi-focus-banner").textContent).toContain("（1）");
    const text = screen.getByTestId("audit-events-table").textContent ?? "";
    expect(text).toContain("越权阻断");
    expect(text).not.toContain("登录成功");
  });

  it("「返回全部」→ 恢复 KPI 卡+控件+全量表", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-success")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-kpi-success"));
    fireEvent.click(screen.getByTestId("audit-events-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("audit-events-summary")).toBeTruthy();
    expect(screen.getByTestId("audit-events-controls")).toBeTruthy();
    expect(screen.getByTestId("audit-events-table").textContent).toContain("越权阻断");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/audit/events?kpi=events&status=failed");
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("audit-events-summary")).toBeNull();
    expect(screen.getByTestId("audit-events-table").textContent).not.toContain("登录成功");
  });

  it("分类/来源渲染为中文映射（eventCategory security→安防）", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-table")).toBeTruthy());
    expect(screen.getByTestId("audit-events-table").textContent).toContain("安防");
  });
});

describe("AuditEventsPage 行明细抽屉", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  it("点事件行 → 抽屉含该行分类（中文）与摘要，无英文枚举，Esc 关闭", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-row-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-row-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("安防");
    expect(drawer.textContent).toContain("越权阻断");
    expect(drawer.textContent).not.toContain("security");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-select-0")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-select-0"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

describe("AuditEventsPage 操作日志反查（audit_events）", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  const OP_ROWS = [
    {
      id: "a1",
      occurredAt: "2026-07-05T10:00:00Z",
      eventName: "contract.activated",
      eventResult: "success",
      actorType: "staff",
      actorId: "u9",
      terminal: "web",
      businessType: "contract",
      businessId: "c1",
    },
    { id: "a2", occurredAt: "2026-07-05T10:01:00Z", eventName: "voucher.redeemed", eventResult: "success", actorType: "staff", actorId: "u8", terminal: "staff_work_phone", businessType: "contract", businessId: "c1" },
    { id: "a3", occurredAt: "2026-07-05T10:02:00Z", eventName: "contract.updated", eventResult: "success", actorType: "staff", actorId: "u7", terminal: "merchant_portal", businessType: "contract", businessId: "c1" },
    { id: "a4", occurredAt: "2026-07-05T10:03:00Z", eventName: "contract.expired", eventResult: "success", actorType: "system", actorId: "job", terminal: null, businessType: "contract", businessId: "c1" },
  ];

  function renderWithFocus(api: AdminApiClient, focusObjectId?: string) {
    const fullProps: AdminPageProps = {
      api,
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo: vi.fn(),
      focusObjectId,
    };
    render(<AuditEventsPage {...fullProps} />);
  }

  it("对象反查（focus=contract:c1）→ 切操作日志视图并按 business_type/business_id 服务端过滤", async () => {
    const api = makeApi();
    (api.get as ReturnType<typeof vi.fn>).mockResolvedValue(OP_ROWS);
    renderWithFocus(api, "contract:c1");
    // 关键逻辑：对象引用 → 操作日志服务端过滤参数传递。
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        expect.stringContaining("/api/v1/audit/operation-events"),
      ),
    );
    const calls = (api.get as ReturnType<typeof vi.fn>).mock.calls;
    const calledWith = String(calls[0]?.[0] ?? "");
    expect(calledWith).toContain("business_type=contract");
    expect(calledWith).toContain("business_id=c1");
    // 呈现真操作留痕行（谁/做了什么/对象）。
    expect(screen.getByTestId("audit-events-operations-table").textContent).toContain("contract.activated");
    expect(screen.getByTestId("audit-events-operations-focus-banner").textContent).toContain("contract:c1");
  });

  // 第十八批 N 段第 122 条 122-R2（PGM:745「受控的十一类终端：……」）：终端列与明细按终端中文名显示，没有终端信息的显示「—」。
  it("终端列与明细按终端中文名显示", async () => {
    const api = makeApi();
    (api.get as ReturnType<typeof vi.fn>).mockResolvedValue(OP_ROWS);
    renderWithFocus(api, "contract:c1");
    const terminalCell = (index: number) => screen.getByTestId(`audit-events-op-row-${index}`).children[5]?.textContent;
    // 等两路被动加载（证据分页、操作日志）都落定再看、再点。
    await waitFor(() => expect(terminalCell(3)).toBe("—"));
    await waitFor(() => expect(api.getPaged).toHaveBeenCalled());
    expect(terminalCell(0)).toBe("Web 前端");
    expect(terminalCell(1)).toBe("员工工作手机");
    expect(terminalCell(2)).toBe("商户门户");
    fireEvent.click(screen.getByTestId("audit-events-op-row-1"));
    await waitFor(() => expect(screen.getByTestId("row-detail-drawer")).toBeTruthy());
    expect(within(screen.getByTestId("row-detail-drawer")).getByText("终端").nextElementSibling?.textContent).toBe("员工工作手机");
  });

  it("无对象反查时默认证据视图，可手动切到操作日志视图", async () => {
    const api = makeApi();
    (api.get as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    renderWithFocus(api);
    await waitFor(() => expect(screen.getByTestId("audit-events-summary")).toBeTruthy());
    // 证据视图不调用 operation-events。
    expect(api.get).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("audit-events-view-operations"));
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(expect.stringContaining("/api/v1/audit/operation-events")),
    );
  });
});

// M725-F02（治理地图第 303 行「同一个操作被重复提交，系统只认一次」）：导出留痕第一次提交服务端可能已登记、响应却丢了，
// 操作员再点导出——这是同一次导出，不是新的一次：发生时间沿用第一次点击时取的，请求内容逐字不变，带首发的提交编号。
// 真实页面经真实客户端，只桩网络层。
describe("AuditEventsPage 导出留痕没拿到结果再点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/audit/events");
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
      vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
        if ((init?.method ?? "GET").toUpperCase() === "POST") {
          posts.push(String(init?.body));
          // 第一次：响应丢在路上。
          if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
          return Promise.resolve(new Response(
            JSON.stringify({ success: true, code: "OK", message: "success", data: { accepted_count: 1 } }),
            { status: 200, headers: { "content-type": "application/json" } },
          ));
        }
        return Promise.resolve(new Response(
          JSON.stringify({ success: true, code: "OK", message: "success", data: { items: [{ id: "e1", event_type: "login", event_category: "auth", result: "success", trace_id: "t-1", summary: "登录成功" }], page: 1, page_size: 10, total: 1 } }),
          { status: 200, headers: { "content-type": "application/json" } },
        ));
      }),
    );
    const fullProps: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AuditEventsPage {...fullProps} />);

    await waitFor(() => expect(screen.getByTestId("audit-events-export")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("audit-events-export"));
    await waitFor(() => expect(screen.getByTestId("audit-events-form-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    fireEvent.click(screen.getByTestId("audit-events-export"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(JSON.parse(posts[1]!).events[0].occurred_at).toBe(JSON.parse(posts[0]!).events[0].occurred_at);
    expect(JSON.parse(posts[1]!).idempotency_key).toBe(JSON.parse(posts[0]!).idempotency_key);
    expect(posts[1]).toBe(posts[0]);
  });

  // 记住的发生时间跟着「这次导出」走，不跟着页面组件走：第一次没拿到结果后切到别的菜单再回来（页面重新挂载、列表重读后
  // 没有变化），同样的内容再导出——仍是同一次导出。
  it("切页回来再导出：请求内容与提交编号仍与第一次相同", async () => {
    // 页面按点击取当前时刻、用「前缀-毫秒」现生成编号；让每次取到的时刻与毫秒必然不同。
    const realToISOString = Date.prototype.toISOString;
    let now = 1_760_000_000_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
    vi.spyOn(Date.prototype, "toISOString").mockImplementation(() => realToISOString.call(new Date((now += 1000))));
    const posts: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
        if ((init?.method ?? "GET").toUpperCase() === "POST") {
          posts.push(String(init?.body));
          // 第一次：响应丢在路上。
          if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
          return Promise.resolve(new Response(
            JSON.stringify({ success: true, code: "OK", message: "success", data: { accepted_count: 1 } }),
            { status: 200, headers: { "content-type": "application/json" } },
          ));
        }
        return Promise.resolve(new Response(
          JSON.stringify({ success: true, code: "OK", message: "success", data: { items: [{ id: "e1", event_type: "login", event_category: "auth", result: "success", trace_id: "t-1", summary: "登录成功" }], page: 1, page_size: 10, total: 1 } }),
          { status: 200, headers: { "content-type": "application/json" } },
        ));
      }),
    );
    const fullProps: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    // 等列表读完、填导出原因、点导出。
    async function submitExport() {
      await waitFor(() => expect(screen.getByTestId("audit-events-export")).toBeTruthy(), { timeout: 5000 });
      fireEvent.change(screen.getByTestId("audit-events-export-reason"), { target: { value: "切页回来再导出" } });
      fireEvent.click(screen.getByTestId("audit-events-export"));
    }
    const firstVisit = render(<AuditEventsPage {...fullProps} />);
    await submitExport();
    await waitFor(() => expect(screen.getByTestId("audit-events-form-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    // 切到别的菜单再回来：页面重新挂载。
    firstVisit.unmount();
    render(<AuditEventsPage {...fullProps} />);
    await submitExport();
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(JSON.parse(posts[1]!).events[0].occurred_at).toBe(JSON.parse(posts[0]!).events[0].occurred_at);
    expect(JSON.parse(posts[1]!).idempotency_key).toBe(JSON.parse(posts[0]!).idempotency_key);
    expect(posts[1]).toBe(posts[0]);
  });
});

// 第十八批 AE 段第 198 条 198-R3⑥：导出登记只发设备、登记人与事件，导出原因记在 events[0].summary；
// 不再带 operator_note 与写死的假附件 att-audit-export（服务端不读）。真实页面经真实客户端，只桩网络层。
describe("AuditEventsPage 导出登记请求体", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  it("198-R3⑥：请求体不含 operator_note、evidence_attachment_ids，events[0].summary 为所填原因", async () => {
    const posts: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(String(init?.body));
        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { accepted_count: 1 } }),
          { status: 200, headers: { "content-type": "application/json" } }));
      }
      return Promise.resolve(new Response(
        JSON.stringify({ success: true, code: "OK", message: "success", data: { items: [{ id: "e1", event_type: "login", event_category: "auth", result: "success", trace_id: "t-1", summary: "登录成功" }], page: 1, page_size: 10, total: 1 } }),
        { status: 200, headers: { "content-type": "application/json" } },
      ));
    }));
    render(<AuditEventsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("audit-events-table").textContent).toContain("登录成功"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("audit-events-export-reason"), { target: { value: "季度审计抽查导出" } });
    fireEvent.click(screen.getByTestId("audit-events-export"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    const body = JSON.parse(posts[0]!) as Record<string, unknown> & { events: Array<{ summary: string }> };
    expect(body).not.toHaveProperty("operator_note");
    expect(body).not.toHaveProperty("evidence_attachment_ids");
    expect(body.events[0]?.summary).toBe("季度审计抽查导出");
  });
});
