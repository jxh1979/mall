import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneContractsPage } from "./MerchantZoneContractsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const CONTRACTS = [
  { id: "contract-aaaa1111", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", deposit_cent: 100000, space_ids: ["sp-1"] },
  { id: "contract-bbbb2222", lifecycle_status: "expired", effective_date: "2025-01-01", expiry_date: "2025-12-31", deposit_cent: 50000, space_ids: [] },
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
    if (url.includes("/contracts")) return Promise.resolve(envelope(CONTRACTS));
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
  render(<MerchantZoneContractsPage {...props} />);
}

describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/contracts");
  });

  it("点“履约中” → URL 带 ?kpi=contracts&subset=effective，聚焦只剩履约中合同，合同下钻 TraceLink 保留", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-kpi-effective")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-effective"));
    expect(window.location.search).toContain("kpi=contracts");
    expect(window.location.search).toContain("subset=effective");
    expect(screen.getByTestId("merchant-zone-contracts-kpi-focus-banner").textContent).toContain("正在查看：履约中合同（1）");
    const table = screen.getByTestId("merchant-zone-contracts-table").textContent ?? "";
    expect(table).toContain("aaaa1111");
    expect(table).not.toContain("bbbb2222");
    expect(screen.queryByTestId("merchant-zone-contracts-summary")).toBeNull();
    expect(screen.getByTestId("trace-link-ADM-CONT-001-contract-aaaa1111")).toBeTruthy();
  });

  it("聚焦后点「返回全部」→ 恢复 KPI 卡+全量合同", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-total"));
    fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-contracts-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-contracts-table").textContent).toContain("bbbb2222");
  });

  it("生命周期状态列为中文，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-contracts-table").textContent ?? "";
    expect(table).toContain("履约中");
    expect(table).toContain("已到期");
    expect(table).not.toContain("effective");
    expect(table).not.toContain("expired");
  });
});
