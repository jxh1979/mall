import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneBillsPage } from "./MerchantZoneBillsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const BILLS = [
  { id: "b-1", bill_no: "B-1", status: "issued", total_amount_cent: 100000, currency: "CNY", aging_days: 0, source_type: "contract", source_id: "c-1" },
  { id: "b-2", bill_no: "B-2", status: "overdue", total_amount_cent: 50000, currency: "CNY", aging_days: 12, source_type: "contract", source_id: "c-2" },
  { id: "b-3", bill_no: "B-3", status: "closed", total_amount_cent: 30000, currency: "CNY", aging_days: 0, source_type: "manual", source_id: null },
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
    if (url.includes("/bills")) return Promise.resolve(envelope(BILLS));
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
  render(<MerchantZoneBillsPage {...props} />);
}

describe("MerchantZoneBillsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/bills");
  });

  it("点“逾期/账龄” → URL 带 ?kpi=bills&subset=overdue，聚焦只剩逾期账单，来源合同 TraceLink 保留", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-bills-kpi-overdue")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-bills-kpi-overdue"));
    expect(window.location.search).toContain("kpi=bills");
    expect(window.location.search).toContain("subset=overdue");
    expect(screen.getByTestId("merchant-zone-bills-kpi-focus-banner").textContent).toContain("正在查看：逾期/账龄（1）");
    const table = screen.getByTestId("merchant-zone-bills-table").textContent ?? "";
    expect(table).toContain("B-2");
    expect(table).not.toContain("B-1");
    expect(screen.queryByTestId("merchant-zone-bills-summary")).toBeNull();
    expect(screen.getByTestId("trace-link-ADM-CONT-001-c-2")).toBeTruthy();
  });

  it("点“未结账单” → 排除已结清，点「返回全部」恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-bills-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-bills-kpi-open"));
    expect(screen.getByTestId("merchant-zone-bills-kpi-focus-banner").textContent).toContain("正在查看：未结账单（2）");
    fireEvent.click(screen.getByTestId("merchant-zone-bills-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-bills-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-bills-table").textContent).toContain("B-3");
  });

  it("状态列为中文，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-bills-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-bills-table").textContent ?? "";
    expect(table).toContain("已逾期");
    expect(table).toContain("已结清");
    expect(table).not.toContain("overdue");
    expect(table).not.toContain("closed");
  });
});
