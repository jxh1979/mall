import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantWorkOrdersPage } from "./MerchantWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 3 行：wo-1 处理中/高、wo-2 已关闭/低、wo-3 处理中/紧急 → 总数3、待处理(未终结)2、高优先级(high/critical)2。
const ROWS = [
  { id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing" },
  { id: "wo-2", merchant_id: "m-1", work_order_type: "service", title: "工单乙", priority: "low", status: "closed" },
  { id: "wo-3", merchant_id: "m-1", work_order_type: "complaint", title: "工单丙", priority: "critical", status: "processing" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope(ROWS))));
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
  render(<MerchantWorkOrdersPage {...fullProps} />);
}

describe("MerchantWorkOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  it("点“高优先级”数字 → ?kpi=high，聚焦横幅+表只剩高优先级子集(2)，KPI 卡隐藏、无建单表单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-high")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-high"));
    expect(window.location.search).toContain("kpi=high");
    expect(screen.getByTestId("merchant-work-orders-kpi-focus-banner").textContent).toContain("正在查看：高优先级工单（2）");
    expect(screen.queryByTestId("merchant-work-orders-summary")).toBeNull();
    expect(screen.queryByTestId("merchant-work-order-create-form")).toBeNull();
    // 表只含高优先级/紧急两行，不含低优先级 wo-2。
    const text = screen.getByTestId("merchant-work-orders-table").textContent ?? "";
    expect(text).toContain("工单甲");
    expect(text).toContain("工单丙");
    expect(text).not.toContain("工单乙");
  });

  it("点“待处理”数字 → 未终结子集(2)", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-pending")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-pending"));
    expect(screen.getByTestId("merchant-work-orders-kpi-focus-banner").textContent).toContain("（2）");
    const text = screen.getByTestId("merchant-work-orders-table").textContent ?? "";
    expect(text).not.toContain("工单乙");
  });

  it("「返回全部」→ 恢复 KPI 卡+全量表（无建单表单）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-high")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-high"));
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-work-orders-summary")).toBeTruthy();
    expect(screen.queryByTestId("merchant-work-order-create-form")).toBeNull();
    expect(screen.getByTestId("merchant-work-orders-table").textContent).toContain("工单乙");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/merchant/work-orders?kpi=all");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("merchant-work-orders-summary")).toBeNull();
    expect(screen.getByTestId("merchant-work-orders-table")).toBeTruthy();
  });
});

// 第 93 条 93-R2：ENUM:264「商户服务工单状态｜……（商户专区发起；……）」、BSM:1372「商户自己在商户专区报的单」——后台不代商户建单，
// 只派单、处理、关闭；不再提交写死的 merchant-1 / area-1 / contract-1。
describe("MerchantWorkOrdersPage 无建单入口", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  it("93-R2：全套权限也没有建单入口；派单只调 /{id}/assign", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => Promise.resolve(String(url).includes("/owner-candidates")
      ? envelope([{ user_id: "usr_pa", username: "pa", staff_name: "张师傅" }])
      : envelope(ROWS.map((row) => (row.id === "wo-1" ? { ...row, status: "created" } : row)))));
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-row-wo-1")).toBeTruthy());
    expect(screen.queryByTestId("merchant-work-order-create-open")).toBeNull();
    fireEvent.click(screen.getByTestId("merchant-work-order-select-wo-1"));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-owner-user").textContent).toContain("张师傅"));
    fireEvent.change(screen.getByTestId("merchant-work-order-owner-user"), { target: { value: "usr_pa" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-assign"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST" && String(url).endsWith("/api/v1/merchant-service/work-orders/wo-1/assign"))).toBe(true));
    expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST" && String(url).endsWith("/api/v1/merchant-service/work-orders"))).toBe(false);
    const text = screen.getByTestId("merchant-work-orders-page").textContent ?? "";
    for (const fixed of ["merchant-1", "area-1", "contract-1"]) expect(text).not.toContain(fixed);
  });
});

// 第 93 条 93-R1：商户服务工单类型＝报修/保洁/投诉/其他服务请求（BSM:1323、裁决 D-12），service 显示「其他服务请求」而不是共享字典的「服务」。
describe("MerchantWorkOrdersPage 工单类型", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  it("93-R1：时限类型下拉是四类；service 行与抽屉显示「其他服务请求」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-row-wo-2").textContent).toContain("其他服务请求"));
    const select = screen.getByTestId("merchant-work-order-policy-type") as HTMLSelectElement;
    expect(Array.from(select.options).map((option) => [option.value, option.textContent])).toEqual([
      ["repair", "报修"], ["cleaning", "保洁"], ["complaint", "投诉"], ["service", "其他服务请求"],
    ]);
    fireEvent.click(screen.getByTestId("merchant-work-order-row-wo-2"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("其他服务请求");
  });
});

describe("MerchantWorkOrdersPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  it("点工单行 → 抽屉含该行标题/类型（中文）/状态，无英文枚举，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-row-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-order-row-wo-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("工单甲");
    expect(drawer.textContent).toContain("报修");
    expect(drawer.textContent).toContain("处理中");
    expect(drawer.textContent).not.toContain("repair");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-select-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-order-select-wo-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

// 第 77 条 77-R3：BSM:1385「响应/处理时限按「工单类型×优先级」矩阵后台配置」「商户服务工单不设首次响应环节」——
// 处理时限按类型×优先级保存（PUT，蛇形键）并列出已配置行。
describe("MerchantWorkOrdersPage 处理时限", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function json(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
  }

  it("列出已配置时限；保存发 PUT（work_order_type/priority/handle_minutes）", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      const method = init?.method ?? "GET";
      if (String(url).includes("/work-order-policies") && method === "GET") {
        return Promise.resolve(json([{ work_order_type: "complaint", priority: "low", handle_minutes: 240 }]));
      }
      if (method === "PUT") return Promise.resolve(json({ work_order_type: "repair", priority: "critical", handle_minutes: 90 }));
      return Promise.resolve(envelope(ROWS));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-policy-complaint-low")?.textContent ?? "").toContain("240"));

    fireEvent.change(screen.getByTestId("merchant-work-order-policy-type"), { target: { value: "repair" } });
    fireEvent.change(screen.getByTestId("merchant-work-order-policy-priority"), { target: { value: "critical" } });
    fireEvent.change(screen.getByTestId("merchant-work-order-policy-minutes"), { target: { value: "90" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-policy-save"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => init?.method === "PUT")).toBe(true));
    const [putUrl, putInit] = fetchMock.mock.calls.find(([, init]) => init?.method === "PUT")!;
    expect(String(putUrl)).toContain("/api/v1/merchant-service/work-order-policies");
    expect(JSON.parse(String(putInit.body))).toMatchObject({ work_order_type: "repair", priority: "critical", handle_minutes: 90 });
  });
});

// 第十八批 L 段第 102 条：BSM:1380「派单到具体人员或班组」；BSM:1385「商户投诉须由商户确认投诉已解决；……客户关怀部记录相关信息及关闭缘由后，可以关闭投诉」
// 「满意度在回访/验收时由提单方评分」；ENUM:264「待商户确认、已处理待关闭」；第十八批裁决 26（报修、其他服务请求的关单方式在处理时限配置里三选一）。
describe("MerchantWorkOrdersPage 第 102 条 派单负责人与关单", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  const ROWS_102 = [
    { id: "wo-c", merchant_id: "m-1", work_order_type: "repair", title: "新报修", priority: "high", status: "created" },
    { id: "wo-p", merchant_id: "m-1", work_order_type: "complaint", title: "噪音投诉", priority: "high", status: "resolved" },
    { id: "wo-m", merchant_id: "m-1", work_order_type: "complaint", title: "异味投诉", priority: "low", status: "pending_merchant" },
  ];

  function json(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
  }

  function mockFetch() {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      const method = init?.method ?? "GET";
      if (String(url).includes("/owner-candidates")) return Promise.resolve(json([{ user_id: "usr_pa", username: "pa", staff_name: "张师傅" }]));
      if (String(url).includes("/work-order-close-methods") && method === "GET") {
        return Promise.resolve(json([{ work_order_type: "repair", close_method: "staff_acceptance" }, { work_order_type: "service", close_method: "handler" }]));
      }
      if (method !== "GET") return Promise.resolve(json({ id: "wo-c", status: "assigned" }));
      if (String(url).includes("/work-order-policies")) return Promise.resolve(json([]));
      return Promise.resolve(envelope(ROWS_102));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    return fetchMock;
  }

  const posts = (fetchMock: ReturnType<typeof vi.fn>, suffix: string) =>
    fetchMock.mock.calls.filter(([url, init]) => init?.method === "POST" && String(url).endsWith(suffix));

  it("102-R1：负责人是候选下拉、不预填 staff_owner；不选点派单出错不发请求；选候选后派单 body 带所选负责人", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-owner-user").tagName).toBe("SELECT"));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-owner-user").textContent).toContain("张师傅（usr_pa）"));
    const owner = screen.getByTestId("merchant-work-order-owner-user") as HTMLSelectElement;
    expect(owner.value).toBe("");
    expect(owner.textContent).toContain("请选择负责人");
    expect(owner.textContent).not.toContain("staff_owner");
    expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith("/api/v1/merchant-service/work-orders/wo-c/owner-candidates"))).toBe(true);

    fireEvent.click(screen.getByTestId("merchant-work-order-assign"));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-error").textContent).toContain("派单必须选择负责人。"));
    expect(posts(fetchMock, "/wo-c/assign")).toHaveLength(0);

    fireEvent.change(owner, { target: { value: "usr_pa" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-assign"));
    await waitFor(() => expect(posts(fetchMock, "/wo-c/assign")).toHaveLength(1));
    expect(JSON.parse(String(posts(fetchMock, "/wo-c/assign")[0]![1].body))).toMatchObject({ owner_user_id: "usr_pa" });
  });

  it("102-R2：状态列显示「待商户确认」「已处理待关闭」；投诉不填关闭缘由点关闭出错不发请求，填了才发且带 close_reason，满意度不填发 null", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-row-wo-m").textContent).toContain("待商户确认"));
    expect(screen.getByTestId("merchant-work-order-row-wo-p").textContent).toContain("已处理待关闭");
    const target = screen.getByTestId("merchant-work-order-target-status") as HTMLSelectElement;
    expect(Array.from(target.options).map((option) => option.textContent)).toEqual(expect.arrayContaining(["待商户确认", "已处理待关闭"]));

    // 第 125 条：列表到达后默认选中首行在被动 effect 里，本页单选框 checked 又回落首行、没有按选中发的请求可等；
    // effect 没落定就点 wo-p 会被补跑的 effect 改回 wo-c，所以没勾上就再点一次，直到 wo-p 勾上（点已勾上的单选不触发 onChange、不多发请求）。
    await waitFor(() => {
      const woP = screen.getByTestId("merchant-work-order-select-wo-p") as HTMLInputElement;
      if (!woP.checked) fireEvent.click(woP);
      expect(woP.checked).toBe(true);
    });
    fireEvent.click(screen.getByTestId("merchant-work-order-close"));
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-error").textContent).toContain("投诉工单由客户关怀部关闭须填写关闭缘由。"));
    expect(posts(fetchMock, "/wo-p/close")).toHaveLength(0);

    fireEvent.change(screen.getByTestId("merchant-work-order-close-reason"), { target: { value: "商户多次联系未回复" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-close"));
    await waitFor(() => expect(posts(fetchMock, "/wo-p/close")).toHaveLength(1));
    expect(JSON.parse(String(posts(fetchMock, "/wo-p/close")[0]![1].body))).toMatchObject({ close_reason: "商户多次联系未回复", satisfaction_score: null });
  });

  it("102-R3：处理时限小节列出报修、其他服务请求的关单方式；保存发 PUT（work_order_type/close_method）", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-close-method-repair")?.textContent ?? "").toContain("员工验收后关闭"));
    expect(screen.getByTestId("merchant-work-order-close-method-service").textContent).toContain("处理人完成即结束");
    const type = screen.getByTestId("merchant-work-order-close-method-type") as HTMLSelectElement;
    expect(Array.from(type.options).map((option) => [option.value, option.textContent])).toEqual([["repair", "报修"], ["service", "其他服务请求"]]);
    const method = screen.getByTestId("merchant-work-order-close-method") as HTMLSelectElement;
    expect(Array.from(method.options).map((option) => option.textContent)).toEqual(["处理人完成即结束", "员工验收后关闭", "须商户确认"]);

    fireEvent.change(type, { target: { value: "repair" } });
    fireEvent.change(method, { target: { value: "merchant_confirm" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-close-method-save"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "PUT" && String(url).includes("/work-order-close-methods"))).toBe(true));
    const [, putInit] = fetchMock.mock.calls.find(([url, init]) => init?.method === "PUT" && String(url).includes("/work-order-close-methods"))!;
    expect(JSON.parse(String(putInit.body))).toMatchObject({ work_order_type: "repair", close_method: "merchant_confirm" });
  });

  // 第十八批 AE 段第 198 条 198-R3①（BSM:940「收银、核销、收款、放行等现场动作，一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」）：
  // 派单、处理、关闭的请求体都不再带写死的 operator_user_id，操作人由服务端取登录人。
  it("198-R3①：派单、处理、关闭三次请求体都不含 operator_user_id", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.getByTestId("merchant-work-order-owner-user").textContent).toContain("张师傅（usr_pa）"));
    fireEvent.change(screen.getByTestId("merchant-work-order-owner-user"), { target: { value: "usr_pa" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-assign"));
    await waitFor(() => expect(posts(fetchMock, "/wo-c/assign")).toHaveLength(1));
    await waitFor(() => expect((screen.getByTestId("merchant-work-order-process") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("merchant-work-order-process"));
    await waitFor(() => expect(posts(fetchMock, "/wo-c/actions")).toHaveLength(1));
    await waitFor(() => expect((screen.getByTestId("merchant-work-order-close") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("merchant-work-order-close"));
    await waitFor(() => expect(posts(fetchMock, "/wo-c/close")).toHaveLength(1));
    for (const suffix of ["/wo-c/assign", "/wo-c/actions", "/wo-c/close"]) {
      expect(JSON.parse(String(posts(fetchMock, suffix)[0]![1].body))).not.toHaveProperty("operator_user_id");
    }
  });
});

// 第十八批 M 段第 111 条：111-R2 第十八批裁决 27「派单后，重复提或误提的由客户关怀部填原因取消」；
// 111-R3 BSM:1385「客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环」。
describe("MerchantWorkOrdersPage 客户关怀部取消与客诉跟进", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  const ROWS_111 = [
    { id: "wo-dup", merchant_id: "m-1", work_order_type: "repair", title: "重复报修", priority: "high", status: "processing" },
    { id: "wo-cp", merchant_id: "m-1", work_order_type: "complaint", title: "噪音投诉", priority: "medium", status: "closed" },
  ];

  function json(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
  }

  function mockFetch() {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if ((init?.method ?? "GET") === "POST") return Promise.resolve(json({ id: "wo-dup", status: "cancelled" }));
      if (String(url).includes("/complaint-followups/")) return Promise.resolve(json({ chain: "merchant_service", work_order_id: "wo-cp" }));
      if (String(url).endsWith("/api/v1/merchant-service/work-orders")) return Promise.resolve(envelope(ROWS_111));
      return Promise.resolve(json([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    return fetchMock;
  }

  it("选中已派单的重复单：未填取消原因不发请求；填原因点「取消工单」POST /staff-cancel 带幂等键与原因", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-cancel")).not.toBeNull());
    fireEvent.click(screen.getByTestId("merchant-work-order-select-wo-dup"));
    fireEvent.click(screen.getByTestId("merchant-work-order-cancel"));
    expect(screen.getByTestId("merchant-work-order-error").textContent).toContain("取消原因");
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);
    fireEvent.change(screen.getByTestId("merchant-work-order-cancel-reason"), { target: { value: "与上午那张重复" } });
    fireEvent.click(screen.getByTestId("merchant-work-order-cancel"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST"
      && String(url).endsWith("/api/v1/merchant-service/work-orders/wo-dup/staff-cancel"))).toBe(true));
    const [, postInit] = fetchMock.mock.calls.find(([, init]) => init?.method === "POST")!;
    const body = JSON.parse(String(postInit.body));
    expect(body).toMatchObject({ reason: "与上午那张重复" });
    expect(body.idempotency_key).toBeTruthy();
  });

  it("投诉行有「客诉跟进」，点开读该单的商户链跟进；非投诉行没有", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-complaint-followup-wo-cp")).not.toBeNull());
    expect(screen.queryByTestId("merchant-work-order-complaint-followup-wo-dup")).toBeNull();
    fireEvent.click(screen.getByTestId("merchant-work-order-complaint-followup-wo-cp"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith("/api/v1/complaint-followups/merchant_service/wo-cp"))).toBe(true));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    await waitFor(() => expect(screen.getByTestId("complaint-loop-status").textContent).toContain("待客户关怀部确认闭环"));
  });
});

// 第十八批 M 段第 117 条 117-R5：BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方」；
// 第十八批裁决 29「不加状态，停在原状态」：商户服务工单承担方有争议时停在原状态转上级判定。承担的商户就是提单商户，不另选。
describe("MerchantWorkOrdersPage 工单用料（第 117 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  const ROWS_117 = [{ id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "水龙头漏水", priority: "high", status: "processing" }];
  const MATERIALS = {
    lines: [], total_cost_cent: 0, bearer: { cost_bearer: "merchant", merchant_id: "m-1", merchant_name: "甲商户" }, disputes: [],
    options: { stock: [{ warehouse_id: "W", warehouse_name: "工程配件库", location_id: "L", item_id: "I", item_name: "阀芯", available_quantity: 3 }], merchants: [] },
  };

  function json(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
  }

  function mockFetch() {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (String(url).endsWith("/materials")) return Promise.resolve(json(MATERIALS));
      if (String(url).endsWith("/api/v1/merchant-service/work-orders")) return Promise.resolve(envelope(ROWS_117));
      return Promise.resolve(json([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    return fetchMock;
  }

  it("选中工单点「用料」读该单用料；领用出库只提交库位、物料与数量；商户承担不另选商户，有争议块", async () => {
    const fetchMock = mockFetch();
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-select-wo-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("merchant-work-order-select-wo-1"));
    await waitFor(() => expect(screen.queryByTestId("merchant-work-order-materials")).not.toBeNull());
    fireEvent.click(screen.getByTestId("merchant-work-order-materials"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url]) => String(url).endsWith("/api/v1/merchant-service/work-orders/wo-1/materials"))).toBe(true));
    await waitFor(() => expect(screen.queryByTestId("work-order-materials-issue")).not.toBeNull());
    fireEvent.change(screen.getByTestId("work-order-materials-warehouse"), { target: { value: "W" } });
    fireEvent.change(screen.getByTestId("work-order-materials-stock"), { target: { value: "I::L" } });
    fireEvent.change(screen.getByTestId("work-order-materials-quantity"), { target: { value: "1" } });
    fireEvent.click(screen.getByTestId("work-order-materials-issue-submit"));
    const issued = () => fetchMock.mock.calls.filter(([url, init]) => init?.method === "POST"
      && String(url).endsWith("/api/v1/merchant-service/work-orders/wo-1/materials"));
    await waitFor(() => expect(issued()).toHaveLength(1));
    const body = JSON.parse(String(issued()[0]![1].body));
    expect(body).toMatchObject({ location_id: "L", item_id: "I", quantity: 1 });
    expect(body.warehouse_id).toBeUndefined();

    fireEvent.click(screen.getByTestId("work-order-materials-bearer-merchant"));
    expect(screen.queryByTestId("work-order-materials-bearer-merchant-id")).toBeNull();
    expect(screen.getByTestId("work-order-materials-dispute")).toBeTruthy();
    expect(screen.queryByTestId("work-order-materials-claimed-merchant-id")).toBeNull();
  });
});
