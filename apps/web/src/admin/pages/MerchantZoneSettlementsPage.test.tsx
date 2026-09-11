import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneSettlementsPage } from "./MerchantZoneSettlementsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const SETTLEMENTS = [
  { id: "st-1", contract_id: "c-1", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null },
  { id: "st-2", contract_id: "c-2", trigger_type: "expiry", status: "pending_settlement", dispute_reason: "金额不符" },
  { id: "st-3", contract_id: "c-3", trigger_type: "expiry", status: "settled", dispute_reason: null },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/settlements")) return Promise.resolve(envelope(SETTLEMENTS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
  render(<MerchantZoneSettlementsPage {...props} />);
}

describe("MerchantZoneSettlementsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  it("没有「有异议」KPI，本页不提交异议", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-summary")).toBeTruthy());
    expect(screen.queryByTestId("merchant-zone-settlements-kpi-disputed")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-settlement-dispute-form")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-settlement-dispute-submit")).toBeNull();
    const page = screen.getByTestId("merchant-zone-settlements-page").textContent ?? "";
    expect(page).not.toContain("有异议");
    expect(page).not.toContain("待平台裁决");
    expect(page).not.toContain("提交异议");
  });

  it("点“未结清” → 聚焦排除已结清，点「返回全部」恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlements-kpi-open"));
    expect(screen.getByTestId("merchant-zone-settlements-kpi-focus-banner").textContent).toContain("正在查看：未结清（2）");
    expect(screen.queryByTestId("merchant-zone-settlement-row-st-3")).toBeNull();
    fireEvent.click(screen.getByTestId("merchant-zone-settlements-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-settlements-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-settlement-row-st-3")).toBeTruthy();
  });

  it("触发/状态列为中文，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-settlements-table").textContent ?? "";
    expect(table).toContain("退租");
    expect(table).toContain("待结算");
    expect(table).toContain("已结清");
    expect(table).not.toContain("有异议");
    expect(table).not.toContain("进行中");
    expect(table).not.toContain("move_out");
    expect(table).not.toContain("pending_settlement");
  });
});

describe("MerchantZoneSettlementsPage 行明细抽屉（整行可点·时间中文）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  it("点结算单行 -> 抽屉含结算单号+中文触发/状态，合同 TraceLink 保留", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-row-st-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-row-st-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("st-1");
    expect(text).toContain("退租");
    expect(text).toContain("待结算");
    expect(text).not.toContain("move_out");
    expect(text).not.toContain("pending_settlement");
    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-c-1"]')).toBeTruthy();
  });

  it("点结算单行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-row-st-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-row-st-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("合同下钻 TraceLink 触发下钻、不弹抽屉", async () => {
    const navigateTo = vi.fn();
    const props: AdminPageProps = {
      api: makeApi(),
      user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo,
    };
    render(<MerchantZoneSettlementsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-CONT-001-c-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("trace-link-ADM-CONT-001-c-1"));
    expect(navigateTo).toHaveBeenCalled();
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
