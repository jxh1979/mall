// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantScopePage } from "./MerchantScopePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M023-001：夹具按后端真实返回形状（MerchantWorkOrderRecord / MerchantBillRecord 的蛇形键），不自带后端不存在的字段。
const WORK_ORDERS = [
  { id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing" },
  { id: "wo-2", merchant_id: "m-2", work_order_type: "repair", title: "工单乙", priority: "high", status: "processing" },
];
const BILLS = [
  { id: "bl-1", merchant_id: "m-1", bill_no: "B-1", total_amount_cent: 10000, currency: "CNY", status: "issued" },
  { id: "bl-2", merchant_id: "m-2", bill_no: "B-2", total_amount_cent: 20000, currency: "CNY", status: "issued" },
  { id: "bl-3", merchant_id: "m-1", bill_no: "B-3", total_amount_cent: 2550, currency: "CNY", status: "viewed" },
  { id: "bl-4", merchant_id: "m-1", bill_no: "B-4", total_amount_cent: 0, currency: "CNY", status: "weird_code" },
];

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
      if (url.includes("work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
      if (url.includes("bills")) return Promise.resolve(envelope(BILLS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(user: AdminPageProps["user"] = { id: "u1", username: "merchant", accountType: "merchant_admin", merchantId: "m-1" }) {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user,
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
  return render(<MerchantScopePage {...fullProps} />);
}

describe("MerchantScopePage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/scope");
  });

  it("点“商户账单”数字 → ?kpi=bills，只剩账单表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-bills")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-bills"));
    expect(window.location.search).toContain("kpi=bills");
    expect(screen.getByTestId("merchant-scope-kpi-focus-banner").textContent).toContain("正在查看：商户账单（3）");
    expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-work-orders-table")).toBeNull();
    expect(screen.queryByTestId("merchant-scope-summary")).toBeNull();
  });

  it("点“商户工单”数字 → 只剩工单表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-work-orders")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-work-orders"));
    expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-bills-table")).toBeNull();
  });

  it("「返回全部」→ 恢复两表+KPI 卡", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-bills")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-bills"));
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy();
    expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy();
    expect(screen.getByTestId("merchant-scope-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/merchant/scope?kpi=workOrders");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-bills-table")).toBeNull();
  });

  it("工单类型渲染为中文，不含英文枚举 repair", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy());
    const text = screen.getByTestId("merchant-scope-work-orders-table").textContent ?? "";
    expect(text).toContain("报修");
    expect(text).not.toContain("repair");
  });

  // 第 93 条 93-R1：类型中文名与商户专区同一张四类表（BSM:1323「其他服务请求」）。
  it("93-R1：工单类型 service 显示为「其他服务请求」", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => Promise.resolve(envelope(String(input).includes("work-orders")
      ? [{ id: "wo-s", merchant_id: "m-1", work_order_type: "service", title: "工单丁", priority: "low", status: "created" }] : []))));
    render(<MerchantScopePage api={new AdminApi("test-token")} user={{ id: "u1", username: "merchant", accountType: "merchant_admin", merchantId: "m-1" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} navigateTo={vi.fn()} />);
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-order-row-wo-s").textContent).toContain("其他服务请求"));
  });

  it("本商户看不见其他商户的工单和账单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-order-row-wo-1")).toBeTruthy());
    expect(screen.queryByTestId("merchant-scope-work-order-row-wo-2")).toBeNull();
    expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-bill-row-bl-2")).toBeNull();
  });

  it("平台管理员无商户范围时不请求、不看全量，给中文提示", async () => {
    renderPage({ id: "u-admin", username: "admin", accountType: "platform_admin" });
    await waitFor(() => expect(screen.getByTestId("admin-error")).toBeTruthy());
    expect(screen.getByTestId("admin-error").textContent).toContain("缺少商户授权范围");
    expect(screen.queryByTestId("merchant-scope-work-order-row-wo-1")).toBeNull();
    expect(screen.queryByTestId("merchant-scope-bill-row-bl-1")).toBeNull();
    expect(vi.mocked(fetch)).not.toHaveBeenCalled();
  });
});

describe("MerchantScopePage 按后端真实字段展示（M023-001/002/003/005）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/scope");
  });

  it("金额读 total_amount_cent 分转元，账单金额 KPI 为本商户合计", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
    expect(screen.getByTestId("merchant-scope-bill-row-bl-1").textContent).toContain("100.00 元");
    expect(screen.getByTestId("merchant-scope-bill-row-bl-3").textContent).toContain("25.50 元");
    expect(screen.getByTestId("merchant-scope-kpi-bill-amount").textContent).toBe("125.50 元");
  });

  it("账单状态用账单字典：issued→已出账（非票务义「已出票」）、viewed→已查看、未知码→未知状态，无英文原码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy());
    const text = screen.getByTestId("merchant-scope-bills-table").textContent ?? "";
    expect(text).toContain("已出账");
    expect(text).not.toContain("已出票");
    expect(text).toContain("已查看");
    expect(text).toContain("未知状态");
    expect(text).not.toContain("issued");
    expect(text).not.toContain("viewed");
    expect(text).not.toContain("weird_code");
  });

  it("不渲染后端不存在的列，也没有不调后端的「确认授权范围」按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-summary")).toBeTruthy());
    const page = screen.getByTestId("merchant-scope-page").textContent ?? "";
    expect(page).not.toContain("授权范围0");
    expect(screen.getByTestId("merchant-scope-work-orders-table").textContent).not.toContain("留痕证据");
    expect(screen.getByTestId("merchant-scope-bills-table").textContent).not.toContain("留痕证据");
    expect(screen.getByTestId("merchant-scope-bills-table").querySelector("thead")?.textContent).toBe("商户账单号金额状态详情");
    expect(screen.queryByTestId("merchant-scope-confirm")).toBeNull();
  });

  it("接口证据写真实权限码与真实留痕事件", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-summary")).toBeTruthy());
    const evidence = screen.getByTestId("merchant-scope-evidence").textContent ?? "";
    expect(evidence).toContain("merchant.service_work_order.view");
    expect(evidence).toContain("merchant.billing.view");
    expect(evidence).not.toContain("merchant.portal.read");
    const page = screen.getByTestId("merchant-scope-page").textContent ?? "";
    expect(page).toContain("merchant_billing.bill.viewed");
    expect(page).not.toContain("merchant.scope_confirmed");
  });

  it("商户号为纯文本，不渲染点了没反应的下钻按钮", async () => {
    const { container } = renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
    expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();
    expect(screen.getByTestId("merchant-scope-bill-row-bl-1").textContent).toContain("m-1");
  });
});

describe("MerchantScopePage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/scope");
  });

  it("点工单行 → 抽屉含该行类型（中文）与状态，无英文枚举，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-order-row-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-work-order-row-wo-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("报修");
    expect(drawer.textContent).toContain("处理中");
    expect(drawer.textContent).not.toContain("repair");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点账单行 → 抽屉含该行账单号、分转元金额与中文状态", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-bill-row-bl-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("B-1");
    expect(text).toContain("100.00 元");
    expect(text).toContain("已出账");
    expect(text).not.toContain("CNY");
  });
});

// 第十八批 L 段第 102 条 102-R2：ENUM:264「待商户确认、已处理待关闭」——商户服务工单状态用本链中文名，不走共享字典的「商户待处理」「已解决」。
describe("MerchantScopePage 商户服务工单状态中文名", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("102-R2：pending_merchant 显示「待商户确认」，resolved 显示「已处理待关闭」", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => Promise.resolve(envelope(String(input).includes("work-orders")
      ? [{ id: "wo-pm", merchant_id: "m-1", work_order_type: "complaint", title: "工单戊", priority: "low", status: "pending_merchant" },
        { id: "wo-rs", merchant_id: "m-1", work_order_type: "repair", title: "工单己", priority: "low", status: "resolved" }] : []))));
    render(<MerchantScopePage api={new AdminApi("test-token")} user={{ id: "u1", username: "merchant", accountType: "merchant_admin", merchantId: "m-1" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} navigateTo={vi.fn()} />);
    await waitFor(() => expect(screen.queryByTestId("merchant-scope-work-order-row-wo-pm")?.textContent ?? "").toContain("待商户确认"));
    expect(screen.getByTestId("merchant-scope-work-order-row-wo-rs").textContent).toContain("已处理待关闭");
  });
});
