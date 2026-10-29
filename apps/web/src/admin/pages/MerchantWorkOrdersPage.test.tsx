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

  it("点“高优先级”数字 → ?kpi=high，聚焦横幅+表只剩高优先级子集(2)，KPI 卡/创建表单隐藏", async () => {
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

  it("「返回全部」→ 恢复 KPI 卡+创建表单+全量表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-high")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-high"));
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-work-orders-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-work-order-create-form")).toBeTruthy();
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
// 建单不再由页面写死截止时间（由后端按处理时限算）；处理时限按类型×优先级保存（PUT，蛇形键）并列出已配置行。
describe("MerchantWorkOrdersPage 处理时限", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function json(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
  }

  it("列出已配置时限；保存发 PUT（work_order_type/priority/handle_minutes）；建单请求不带 due_at", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      const method = init?.method ?? "GET";
      if (String(url).includes("/work-order-policies") && method === "GET") {
        return Promise.resolve(json([{ work_order_type: "complaint", priority: "low", handle_minutes: 240 }]));
      }
      if (method === "PUT") return Promise.resolve(json({ work_order_type: "repair", priority: "critical", handle_minutes: 90 }));
      if (method === "POST") return Promise.resolve(json({ id: "wo-new" }));
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

    await waitFor(() => expect((screen.getByTestId("merchant-work-order-policy-save") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("merchant-work-order-create-open"));
    fireEvent.click(screen.getByTestId("merchant-work-order-guided-next"));
    fireEvent.click(screen.getByTestId("merchant-work-order-create"));
    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => init?.method === "POST" && String(url).endsWith("/api/v1/merchant-service/work-orders"))).toBe(true));
    const [, postInit] = fetchMock.mock.calls.find(([url, init]) => init?.method === "POST" && String(url).endsWith("/api/v1/merchant-service/work-orders"))!;
    const created = JSON.parse(String(postInit.body));
    expect(created).toMatchObject({ work_order_type: "repair", priority: "high" });
    expect(created).not.toHaveProperty("due_at");
  });
});
