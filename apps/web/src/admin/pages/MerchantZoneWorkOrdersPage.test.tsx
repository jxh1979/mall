import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneWorkOrdersPage } from "./MerchantZoneWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 3 行：2 未关闭 + 1 已关闭 → 总数3、进行中(未终结)2。
const ROWS = [
  { id: "wo-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing", due_at: "2026-07-10T09:00:00Z", resolution: "更换密封圈", created_at: "2026-07-05T08:30:00Z" },
  { id: "wo-2", work_order_type: "service", title: "工单乙", priority: "low", status: "closed" },
  { id: "wo-3", work_order_type: "complaint", title: "工单丙", priority: "medium", status: "created" },
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
    user: { id: "u1", username: "merchant", accountType: "merchant", merchantId: "m-1" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<MerchantZoneWorkOrdersPage {...fullProps} />);
}

describe("MerchantZoneWorkOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  it("点“进行中”数字 → ?kpi=open，横幅(2)+表只剩未关闭子集，KPI 卡隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-open"));
    expect(window.location.search).toContain("kpi=open");
    expect(screen.getByTestId("merchant-zone-work-orders-kpi-focus-banner").textContent).toContain("正在查看：进行中工单（2）");
    expect(screen.queryByTestId("merchant-zone-work-orders-summary")).toBeNull();
    const text = screen.getByTestId("merchant-zone-work-orders-table").textContent ?? "";
    expect(text).not.toContain("工单乙");
    expect(text).toContain("工单甲");
  });

  it("点“工单总数”数字 → 全量子集", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-total"));
    expect(screen.getByTestId("merchant-zone-work-orders-kpi-focus-banner").textContent).toContain("（3）");
    expect(screen.getByTestId("merchant-zone-work-orders-table").textContent).toContain("工单乙");
  });

  it("「返回全部」→ 恢复 KPI 卡+全量表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-open"));
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-work-orders-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-work-orders-table").textContent).toContain("工单乙");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/merchant-zone/work-orders?kpi=open");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("merchant-zone-work-orders-summary")).toBeNull();
  });
});

describe("MerchantZoneWorkOrdersPage 行明细抽屉（整行可点·日期中文·零英文枚举）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  it("点工单行 → 抽屉含标题+中文类型/优先级/状态+中文时间，无英文枚举与 ISO 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-row-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-row-wo-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("工单甲");
    expect(text).toContain("报修");
    expect(text).toContain("处理中");
    expect(text).toContain("2026年7月10日");
    expect(text).toContain("2026年7月5日");
    expect(text).not.toContain("repair");
    expect(text).not.toContain("processing");
    expect(text).not.toContain("2026-07-10T");
  });

  it("列内截止/提交时间为中文，不直出 ISO 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-work-orders-table").textContent ?? "";
    expect(table).toContain("2026年7月10日");
    expect(table).not.toContain("2026-07-10T");
  });

  it("点工单行 → Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-row-wo-3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-row-wo-3"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

// 第十八批 L 段第 102 条 102-R2／R3：BSM:1385「商户投诉须由商户确认投诉已解决」「未点击关闭」「满意度在回访/验收时由提单方评分」；
// ENUM:264「待商户确认、已处理待关闭」；第十八批裁决 26（报修、其他服务请求配成「须商户确认」的，同样由商户确认关闭）。
describe("MerchantZoneWorkOrdersPage 商户确认已解决", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  const ROWS_102 = [
    { id: "wo-pm", work_order_type: "complaint", title: "噪音投诉", priority: "high", status: "pending_merchant" },
    { id: "wo-pr", work_order_type: "complaint", title: "异味投诉", priority: "high", status: "processing" },
    { id: "wo-rm", work_order_type: "repair", title: "门锁报修", priority: "medium", status: "resolved" },
    { id: "wo-sv", work_order_type: "service", title: "搬运申请", priority: "low", status: "resolved" },
  ];

  function json(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
  }

  it("待商户确认的投诉与配成须商户确认的报修有「确认已解决」，处理中与按配置不由商户确认的没有；确认发 POST /merchant-confirm 带可选评分；已处理待关闭显示中文", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      const method = init?.method ?? "GET";
      if (method === "POST") return Promise.resolve(json({ id: "wo-pm", status: "closed" }));
      if (String(url).includes("/work-order-close-methods")) {
        return Promise.resolve(json([{ work_order_type: "repair", close_method: "merchant_confirm" }, { work_order_type: "service", close_method: "staff_acceptance" }]));
      }
      return Promise.resolve(envelope(ROWS_102));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantZoneWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "merchant", accountType: "merchant", merchantId: "m-1" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("merchant-zone-work-order-confirm-wo-pm")).not.toBeNull());
    expect(screen.queryByTestId("merchant-zone-work-order-confirm-wo-rm")).not.toBeNull();
    expect(screen.queryByTestId("merchant-zone-work-order-confirm-wo-pr")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-work-order-confirm-wo-sv")).toBeNull();
    expect(screen.getByTestId("merchant-zone-work-order-row-wo-sv").textContent).toContain("已处理待关闭");

    fireEvent.click(screen.getByTestId("merchant-zone-work-order-confirm-wo-pm"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.change(screen.getByTestId("merchant-zone-work-order-confirm-score"), { target: { value: "4" } });
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-confirm-submit"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST"
      && String(url).endsWith("/api/v1/merchant-service/work-orders/wo-pm/merchant-confirm"))).toBe(true));
    const [, postInit] = fetchMock.mock.calls.find(([, init]) => init?.method === "POST")!;
    expect(JSON.parse(String(postInit.body))).toMatchObject({ satisfaction_score: 4 });
  });
});

// 第十八批 M 段第 111 条 111-R2：ENUM:264「已取消（商户专区发起……）」；第十八批裁决 27「派单前商户自己取消；派单后，重复提或误提的由客户关怀部填原因取消」。
describe("MerchantZoneWorkOrdersPage 派单前取消", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  const ROWS_111 = [
    { id: "wo-cr", work_order_type: "repair", title: "门锁报修", priority: "medium", status: "created" },
    { id: "wo-as", work_order_type: "cleaning", title: "地面保洁", priority: "low", status: "assigned" },
  ];

  it("已提交的单有「取消」、已派单的没有；未填原因不发请求；填原因后 POST /cancel 带幂等键与原因", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      if ((init?.method ?? "GET") === "POST") {
        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "wo-cr", status: "cancelled" } }),
          { status: 200, headers: { "content-type": "application/json" } }));
      }
      if (String(url).includes("/work-order-close-methods")) return Promise.resolve(envelope([]));
      return Promise.resolve(envelope(ROWS_111));
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<MerchantZoneWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "merchant", accountType: "merchant", merchantId: "m-1" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("merchant-zone-work-order-cancel-wo-cr")).not.toBeNull());
    expect(screen.queryByTestId("merchant-zone-work-order-cancel-wo-as")).toBeNull();

    fireEvent.click(screen.getByTestId("merchant-zone-work-order-cancel-wo-cr"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-cancel-submit"));
    expect(fetchMock.mock.calls.some(([, init]) => init?.method === "POST")).toBe(false);
    fireEvent.change(screen.getByTestId("merchant-zone-work-order-cancel-reason"), { target: { value: "重复提单" } });
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-cancel-submit"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST"
      && String(url).endsWith("/api/v1/merchant-service/work-orders/wo-cr/cancel"))).toBe(true));
    const [, postInit] = fetchMock.mock.calls.find(([, init]) => init?.method === "POST")!;
    const body = JSON.parse(String(postInit.body));
    expect(body).toMatchObject({ reason: "重复提单" });
    expect(body.idempotency_key).toBeTruthy();
  });
});
