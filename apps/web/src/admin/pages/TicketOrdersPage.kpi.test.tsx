import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketOrderRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ORDERS: TicketOrderRow[] = [
  { id: "o-1", orderNo: "TKT-0001", ticketTypeId: "tt-1", quantity: 1, payStatus: "paid", issueStatus: "issued", status: "paid", amountCent: 9900, createdAt: "2026-07-05T09:00:00Z", paidAt: "2026-07-05T10:30:00Z" },
  { id: "o-2", orderNo: "TKT-0002", ticketTypeId: "tt-2", quantity: 2, payStatus: "unpaid", issueStatus: "not_issued", status: "created", amountCent: 19800, createdAt: "2026-07-05T09:15:00Z" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/orders") ? ORDERS : [])),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<TicketOrdersPage {...props} />);
}

describe("TicketOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/ticketing/orders");
  });

  it("点“已支付”KPI → URL 带 ?kpi=paid，只呈现已支付订单（未支付隐藏），KPI 卡与出票操作隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-kpi-paid")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-orders-kpi-paid"));
    expect(window.location.search).toContain("kpi=paid");
    expect(screen.getByTestId("ticket-orders-kpi-focus-banner").textContent).toContain("已支付（1）");
    const table = screen.getByTestId("ticket-orders-table").textContent ?? "";
    expect(table).toContain("TKT-0001");
    expect(table).not.toContain("TKT-0002");
    expect(screen.queryByTestId("ticket-orders-summary")).toBeNull();
    expect(screen.queryByTestId("ticket-order-actions")).toBeNull();
  });

  it("聚焦点「返回全部」→ 参数清除，KPI 卡与两条订单都恢复", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-kpi-not-issued")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-orders-kpi-not-issued"));
    fireEvent.click(screen.getByTestId("ticket-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("ticket-orders-summary")).toBeTruthy();
    const table = screen.getByTestId("ticket-orders-table").textContent ?? "";
    expect(table).toContain("TKT-0001");
    expect(table).toContain("TKT-0002");
  });

  it("带参直链 ?kpi=not_issued → 直接进聚焦视图（只剩未出票订单）", async () => {
    window.history.replaceState({}, "", "/admin/ticketing/orders?kpi=not_issued");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("ticket-orders-kpi-focus-banner").textContent).toContain("待出票（1）");
    const table = screen.getByTestId("ticket-orders-table").textContent ?? "";
    expect(table).toContain("TKT-0002");
    expect(table).not.toContain("TKT-0001");
  });

  it("浏览器后退（popstate）→ 退出聚焦视图", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-kpi-paid")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-orders-kpi-paid"));
    expect(screen.getByTestId("ticket-orders-kpi-focus-banner")).toBeTruthy();
    window.history.replaceState({}, "", "/admin/ticketing/orders");
    fireEvent.popState(window);
    await waitFor(() => expect(screen.queryByTestId("ticket-orders-kpi-focus-banner")).toBeNull());
    expect(screen.getByTestId("ticket-orders-summary")).toBeTruthy();
  });

  it("KPI 子标签为中文，不含 paid/not_issued 英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-summary")).toBeTruthy());
    const text = screen.getByTestId("ticket-orders-summary").textContent ?? "";
    expect(text).not.toMatch(/\bpaid\b/);
    expect(text).not.toMatch(/not_issued/);
    expect(text).toContain("已支付订单");
    expect(text).toContain("未出票订单");
  });

  it("表格支付/出票状态渲染为中文（已支付/未出票）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-table")).toBeTruthy());
    const table = screen.getByTestId("ticket-orders-table").textContent ?? "";
    expect(table).toContain("已支付");
    expect(table).toContain("未出票");
  });

  it("V231：表格展示「下单时间/支付时间」两列，中文格式；未支付支付时间显「—」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-orders-table")).toBeTruthy());
    const head = screen.getByTestId("ticket-orders-table").querySelector("thead")?.textContent ?? "";
    expect(head).toContain("下单时间");
    expect(head).toContain("支付时间");
    // 已支付订单：支付时间为中文格式（含 年/月/日），非 ISO 原文。
    const paidCell = screen.getByTestId("ticket-order-paid-at-o-1").textContent ?? "";
    expect(paidCell).toContain("2026年7月5日");
    expect(paidCell).not.toContain("T10:30");
    // 未支付订单：支付时间显「—」。
    expect(screen.getByTestId("ticket-order-paid-at-o-2").textContent).toBe("—");
  });
});

describe("TicketOrdersPage 行明细抽屉（整行可点·时间中文，与单选/下钻/高亮并存）", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/ticketing/orders");
  });

  it("点订单行 -> 抽屉含订单号+中文支付/出票状态+下单/支付时间中文，无英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-order-row-o-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-order-row-o-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("TKT-0001");
    expect(text).toContain("已支付");
    expect(text).toContain("已出票");
    expect(text).toContain("2026年7月5日 09:00");
    expect(text).toContain("2026年7月5日 10:30");
    expect(text).not.toContain("paid");
    expect(text).not.toContain("issued");
  });

  it("点订单行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-order-row-o-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-order-row-o-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点单选按钮只选中订单、不弹抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-order-select-o-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-order-select-o-1"));
    expect((screen.getByTestId("ticket-order-select-o-1") as HTMLInputElement).checked).toBe(true);
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点票种 TraceLink 触发下钻、不弹抽屉（stopPropagation）", async () => {
    const navigateTo = vi.fn();
    renderPage({ navigateTo });
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-TKT-001-tt-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("trace-link-ADM-TKT-001-tt-1"));
    expect(navigateTo).toHaveBeenCalled();
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("P1-B 下钻定位高亮与整行可点并存（focus 行同时有 clickable 与 focus 类）", async () => {
    renderPage({ focusObjectId: "o-1" });
    await waitFor(() => expect(screen.getByTestId("ticket-order-row-o-1")).toBeTruthy());
    const row = screen.getByTestId("ticket-order-row-o-1");
    expect(row.className).toContain("admin-row-clickable");
    expect(row.className).toContain("admin-row-selected");
  });
});
