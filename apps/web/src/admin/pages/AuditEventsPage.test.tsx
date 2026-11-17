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

  // 227-R2：操作日志视图另取一次标签字典（客户端已转驼峰）；按路径分派假响应。227-R3：补 OP_ROWS 用到的事件名。
  const OP_LABELS = {
    businessTypes: [{ code: "contract", name: "合同" }],
    actorTypes: [{ code: "staff", name: "普通员工" }],
    eventNames: [
      { code: "contract.activated", name: "合同生效" },
      { code: "contract.expired", name: "合同到期" },
      { code: "contract.updated", name: "修改合同" },
      { code: "voucher.redeemed", name: "统一核销" },
    ],
    permissions: [],
  };
  const byPath = (path: string) => Promise.resolve(path.includes("/api/v1/audit/labels") ? OP_LABELS : OP_ROWS);

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
    (api.get as ReturnType<typeof vi.fn>).mockImplementation(byPath);
    renderWithFocus(api, "contract:c1");
    // 关键逻辑：对象引用 → 操作日志服务端过滤参数传递。
    await waitFor(() =>
      expect(api.get).toHaveBeenCalledWith(
        expect.stringContaining("/api/v1/audit/operation-events"),
      ),
    );
    const calls = (api.get as ReturnType<typeof vi.fn>).mock.calls;
    const calledWith = String(calls.find((call) => String(call[0]).includes("/api/v1/audit/operation-events"))?.[0] ?? "");
    expect(calledWith).toContain("business_type=contract");
    expect(calledWith).toContain("business_id=c1");
    // 呈现真操作留痕行（谁/做了什么/对象）；227-R3：「操作」列显示事件名中文名，不显示原码。
    await waitFor(() => expect(screen.getByTestId("audit-events-operations-table").textContent).toContain("合同生效"));
    expect(screen.getByTestId("audit-events-operations-table").textContent).not.toContain("contract.activated");
    // 227-R2：反查横幅显示业务类型中文名与业务编号。
    await waitFor(() => expect(screen.getByTestId("audit-events-operations-focus-banner").textContent).toContain("合同：c1"));
    expect(screen.getByTestId("audit-events-operations-focus-banner").textContent).not.toContain("contract:");
  });

  // 第十八批 N 段第 122 条 122-R2（PGM:745「受控的十一类终端：……」）：终端列与明细按终端中文名显示，没有终端信息的显示「—」。
  it("终端列与明细按终端中文名显示", async () => {
    const api = makeApi();
    (api.get as ReturnType<typeof vi.fn>).mockImplementation(byPath);
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
    fireEvent.change(screen.getByTestId("audit-events-export-reason"), { target: { value: "季度审计抽查导出" } });
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
    // 第十八批 AP 段第 229 条 229-R5（PGM:685）：导出原因不预填「验收导出审计事件」，人填。
    expect((screen.getByTestId("audit-events-export-reason") as HTMLInputElement).value).toBe("");
    fireEvent.change(screen.getByTestId("audit-events-export-reason"), { target: { value: "季度审计抽查导出" } });
    fireEvent.click(screen.getByTestId("audit-events-export"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    const body = JSON.parse(posts[0]!) as Record<string, unknown> & { events: Array<{ summary: string }> };
    expect(body).not.toHaveProperty("operator_note");
    expect(body).not.toHaveProperty("evidence_attachment_ids");
    expect(body.events[0]?.summary).toBe("季度审计抽查导出");
  });
});

// 第十八批 AN 段第 227 条 227-R1（PGM:287「……展示给用户的一切文字……必须全部为中文，不得出现任何英文……」；PGM:299）：
// 证据事件类型与分类按中文名显示，页内筛选认页面上看到的中文名；导出后结果区不放英文留痕事件名；操作日志明细的分类显示中文名。
// 真实页面经真实客户端，只桩网络层；证据事件按后端 EvidenceEventRecord 形状（正常业务只有网页两种导出登记）。
describe("AuditEventsPage 证据事件类型与分类中文名", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  const AT = "2026-11-16T02:00:00Z";
  const EVIDENCE_ITEMS = [
    { id: "ev-1", device_code: "web-admin", staff_id: "u1", event_type: "merchant.admission.audit_exported", event_category: "governance", result: "success", summary: "年度复核导出", payload: {}, occurred_at: AT, received_at: AT },
    { id: "ev-2", device_code: "web-admin", staff_id: "u1", event_type: "audit_events_exported", event_category: "audit", result: "success", summary: "季度审计抽查导出", payload: {}, occurred_at: AT, received_at: AT },
  ];
  const OPERATION_ITEMS = [
    { id: "op-1", occurred_at: AT, event_name: "points.earned", event_result: "success", actor_type: "staff", actor_id: "u2", terminal: "web", business_type: "customer_points", business_id: "cp-1", permission_code: "not_applicable", request_id: "rq-1", event_category: "loyalty" },
  ];

  function stubFetch() {
    const posts: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      let data: unknown = { items: EVIDENCE_ITEMS, page: 1, page_size: 10, total: 2 };
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(String(init?.body));
        data = { accepted: 1, received_at: AT };
      } else if (String(input).includes("/api/v1/audit/operation-events")) {
        data = OPERATION_ITEMS;
      }
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
        { status: 200, headers: { "content-type": "application/json" } }));
    }));
    return posts;
  }

  function renderReal(setActionResult = vi.fn()) {
    render(<AuditEventsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={setActionResult} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
  }

  it("表格、行明细与查看详情显示中文事件类型与分类，按中文名可筛中", async () => {
    stubFetch();
    renderReal();
    await waitFor(() => {
      const text = screen.getByTestId("audit-events-table").textContent ?? "";
      expect(text).toContain("准入审计已导出");
      expect(text).toContain("治理");
      expect(text).toContain("审计事件已导出");
      expect(text).toContain("审计");
    }, { timeout: 3000 });
    expect(screen.getByTestId("audit-events-table").textContent).not.toMatch(/[A-Za-z]/);
    expect(screen.getByTestId("audit-events-controls").textContent).not.toMatch(/[A-Za-z]/);
    expect(screen.getByTestId("audit-events-boundary").textContent).not.toMatch(/[A-Za-z]/);

    fireEvent.click(screen.getByTestId("audit-events-row-0"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("准入审计已导出");
    expect(drawer.textContent).toContain("治理");
    expect(drawer.textContent).not.toMatch(/[A-Za-z]/);
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());

    fireEvent.click(screen.getByTestId("audit-events-show-detail"));
    const detail = screen.getByTestId("audit-events-detail").textContent ?? "";
    expect(detail).toContain("准入审计已导出");
    expect(detail).not.toContain("merchant.admission.audit_exported");

    fireEvent.change(screen.getByTestId("audit-events-search"), { target: { value: "准入" } });
    expect(screen.getByTestId("audit-events-row-0").textContent).toContain("年度复核导出");
    expect(screen.queryByTestId("audit-events-row-1")).toBeNull();
  });

  it("导出登记成功后结果区不放留痕事件名", async () => {
    const posts = stubFetch();
    const setActionResult = vi.fn();
    renderReal(setActionResult);
    await waitFor(() => expect(screen.getByTestId("audit-events-table").textContent).toContain("季度审计抽查导出"), { timeout: 3000 });
    // 第十八批 AP 段 229-R5：导出原因不预填，人填。
    fireEvent.change(screen.getByTestId("audit-events-export-reason"), { target: { value: "季度审计抽查导出" } });
    fireEvent.click(screen.getByTestId("audit-events-export"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "audit_events_exported" })), { timeout: 3000 });
    expect(posts).toHaveLength(1);
    expect(setActionResult.mock.calls.at(-1)?.[0]).not.toHaveProperty("audit");
  });

  it("操作日志明细的分类显示中文名，请求编号标签为中文", async () => {
    stubFetch();
    renderReal();
    await waitFor(() => expect(screen.getByTestId("audit-events-table").textContent).toContain("年度复核导出"), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("audit-events-view-operations"));
    await waitFor(() => expect(screen.getByTestId("audit-events-op-row-0").textContent).toContain("u2"), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("audit-events-op-row-0"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(within(drawer).getByText("分类").nextElementSibling?.textContent).toBe("会员积分");
    expect(drawer.textContent).not.toContain("loyalty");
    expect(within(drawer).getByText("请求编号").nextElementSibling?.textContent).toBe("rq-1");
  });
});

// 第十八批 AN 段第 227 条 227-R2（PGM:287「……字段名、状态、枚举取值……必须全部为中文……不得以“这是标识”为由保留英文。」；
// 决策台账 194-R2「能力手填框改为下拉（显示中文、提交原码）」）：操作日志视图的业务类型、操作人类型、权限按标签接口显示中文名，
// 业务类型筛选为下拉（显示中文、提交原码）。真实页面经真实客户端，只桩网络层，假响应按后端蛇形键。
describe("AuditEventsPage 操作日志业务类型、操作人类型、权限中文名", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  const AT = "2026-11-16T02:00:00Z";
  const OPERATION_ITEMS = [
    { id: "op-1", occurred_at: AT, event_name: "staff_device.rebound", event_result: "success", actor_type: "staff", actor_id: "u3", terminal: "web", business_type: "staff_device_credential", business_id: "cred-1", permission_code: "staff.device.review", request_id: "rq-1" },
    { id: "op-2", occurred_at: AT, event_name: "points.earned", event_result: "success", actor_type: "customer", actor_id: "c9", terminal: "web", business_type: "customer_points", business_id: "cp-1", permission_code: "not_applicable", request_id: "rq-2", event_category: "loyalty" },
  ];
  const LABELS = {
    business_types: [{ code: "customer_points", name: "客人积分" }, { code: "staff_device_credential", name: "员工私人手机绑定" }],
    actor_types: [{ code: "customer", name: "客人" }, { code: "staff", name: "普通员工" }],
    event_names: [],
    permissions: [{ code: "not_applicable", name: "无需员工权限" }, { code: "staff.device.review", name: "审核员工设备" }],
  };

  function stubFetch() {
    const gets: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      gets.push(url);
      let data: unknown = { items: [{ id: "ev-1", device_code: "web-admin", staff_id: "u1", event_type: "audit_events_exported", event_category: "audit", result: "success", summary: "季度审计抽查导出", payload: {}, occurred_at: AT, received_at: AT }], page: 1, page_size: 10, total: 1 };
      if (url.includes("/api/v1/audit/operation-events")) data = OPERATION_ITEMS;
      else if (url.includes("/api/v1/audit/labels")) data = LABELS;
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
        { status: 200, headers: { "content-type": "application/json" } }));
    }));
    return gets;
  }

  it("表格与明细显示业务类型、操作人类型、权限中文名，业务类型下拉提交原码", async () => {
    const gets = stubFetch();
    render(<AuditEventsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("audit-events-table").textContent).toContain("季度审计抽查导出"), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("audit-events-view-operations"));
    await waitFor(() => {
      const text = screen.getByTestId("audit-events-operations-table").textContent ?? "";
      expect(text).toContain("员工私人手机绑定");
      expect(text).toContain("客人积分");
    }, { timeout: 3000 });
    const table = screen.getByTestId("audit-events-operations-table").textContent ?? "";
    expect(table).not.toContain("staff_device_credential");
    expect(table).not.toContain("customer_points");

    fireEvent.click(screen.getByTestId("audit-events-op-row-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(within(drawer).getByText("操作人").nextElementSibling?.textContent).toBe("客人 / c9");
    expect(within(drawer).getByText("业务类型").nextElementSibling?.textContent).toBe("客人积分");
    expect(within(drawer).getByText("权限").nextElementSibling?.textContent).toBe("无需员工权限");
    expect(drawer.textContent).not.toContain("not_applicable");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());

    const select = screen.getByTestId("audit-events-op-business-type") as HTMLSelectElement;
    expect(select.tagName).toBe("SELECT");
    expect(within(select).getByText("员工私人手机绑定")).toBeTruthy();
    fireEvent.change(select, { target: { value: "staff_device_credential" } });
    fireEvent.click(screen.getByTestId("audit-events-op-query"));
    await waitFor(() => expect(gets.filter((url) => url.includes("/api/v1/audit/operation-events")).at(-1)).toContain("business_type=staff_device_credential"), { timeout: 3000 });
  });
});

// 第十八批 AN 段第 227 条 227-R3（PGM:287「……展示给用户的一切文字……必须全部为中文，不得出现任何英文……」）：
// 操作日志视图「操作」列、明细标题与「操作」字段按标签接口 event_names 显示事件名中文名，未登记的显示「其他操作」。
// 真实页面经真实客户端，只桩网络层，假响应按后端蛇形键。
describe("AuditEventsPage 操作日志事件名中文名", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  const AT = "2026-11-16T02:00:00Z";
  const OPERATION_ITEMS = [
    { id: "op-1", occurred_at: AT, event_name: "device.policy_violation", event_result: "success", actor_type: "staff", actor_id: "u3", terminal: "web", business_type: "mdm_policy_violation", business_id: "dev-1", permission_code: "terminal.device.lifecycle", request_id: "rq-1" },
    { id: "op-2", occurred_at: AT, event_name: "unlisted.event_code", event_result: "failure", actor_type: "staff", actor_id: "u4", terminal: "web", business_type: "mdm_policy_violation", business_id: "dev-2", permission_code: "terminal.device.lifecycle", request_id: "rq-2" },
  ];
  const LABELS = {
    business_types: [{ code: "mdm_policy_violation", name: "设备管控违规" }],
    actor_types: [{ code: "staff", name: "普通员工" }],
    event_names: [{ code: "device.policy_violation", name: "记录设备违规" }],
    permissions: [{ code: "terminal.device.lifecycle", name: "终端设备生命周期管理" }],
  };

  function stubFetch() {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      let data: unknown = { items: [{ id: "ev-1", device_code: "web-admin", staff_id: "u1", event_type: "audit_events_exported", event_category: "audit", result: "success", summary: "季度审计抽查导出", payload: {}, occurred_at: AT, received_at: AT }], page: 1, page_size: 10, total: 1 };
      if (url.includes("/api/v1/audit/operation-events")) data = OPERATION_ITEMS;
      else if (url.includes("/api/v1/audit/labels")) data = LABELS;
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
        { status: 200, headers: { "content-type": "application/json" } }));
    }));
  }

  it("「操作」列、明细标题与字段显示事件名中文名，未登记的显示「其他操作」", async () => {
    stubFetch();
    render(<AuditEventsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("audit-events-table").textContent).toContain("季度审计抽查导出"), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("audit-events-view-operations"));
    await waitFor(() => expect(screen.getByTestId("audit-events-op-row-0").children[0]?.textContent).toBe("记录设备违规"), { timeout: 3000 });
    expect(screen.getByTestId("audit-events-op-row-1").children[0]?.textContent).toBe("其他操作");
    expect(screen.getByTestId("audit-events-operations-table").textContent).not.toContain("device.policy_violation");

    fireEvent.click(screen.getByTestId("audit-events-op-row-0"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(screen.getByTestId("row-detail-title").textContent).toBe("操作日志明细 · 记录设备违规");
    expect(within(drawer).getByText("操作").nextElementSibling?.textContent).toBe("记录设备违规");
    expect(document.body.textContent).not.toContain("device.policy_violation");
    expect(document.body.textContent).not.toContain("unlisted.event_code");
  });
});
