// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneContractsPage } from "./MerchantZoneContractsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const CONTRACTS = [
  { id: "contract-aaaa1111", lifecycle_status: "effective", effective_date: "2026-01-01", expiry_date: "2026-12-31", deposit_cent: 100000, space_ids: ["sp-1", "sp-2"], renewed_from_contract_id: "contract-bbbb2222" },
  { id: "contract-bbbb2222", lifecycle_status: "expired", effective_date: "2025-01-01", expiry_date: "2025-12-31", deposit_cent: 50000, space_ids: [] },
  { id: "contract-cccc3333", lifecycle_status: "weird_status", effective_date: "2024-01-01", expiry_date: "2024-12-31", deposit_cent: 0, space_ids: [] },
  { id: "contract-dddd4444", lifecycle_status: "drafting", effective_date: "2027-01-01", expiry_date: "2027-12-31", deposit_cent: 0, space_ids: [] },
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
  const view = render(<MerchantZoneContractsPage {...props} />);
  return { props, container: view.container };
}

describe("MerchantZoneContractsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/contracts");
  });

  it("点“已生效” → URL 带 ?kpi=contracts&subset=effective，聚焦只剩已生效合同", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-kpi-effective")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-effective"));
    expect(window.location.search).toContain("kpi=contracts");
    expect(window.location.search).toContain("subset=effective");
    expect(screen.getByTestId("merchant-zone-contracts-kpi-focus-banner").textContent).toContain("正在查看：已生效合同（1）");
    expect(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111")).toBeTruthy();
    expect(screen.queryByTestId("merchant-zone-contract-row-contract-bbbb2222")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-contracts-summary")).toBeNull();
  });

  it("聚焦后点「返回全部」→ 恢复 KPI 卡+全量合同", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-total"));
    fireEvent.click(screen.getByTestId("merchant-zone-contracts-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-contracts-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-contract-row-contract-bbbb2222")).toBeTruthy();
  });

  it("生命周期状态列为中文：未知码给中文占位，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-contracts-table").textContent ?? "";
    expect(table).toContain("已生效");
    expect(table).toContain("已到期");
    expect(table).toContain("未知状态");
    expect(table).not.toContain("履约中");
    expect(table).not.toContain("起草中");
    expect(table).not.toContain("effective");
    expect(table).not.toContain("expired");
    expect(table).not.toContain("weird_status");
    expect(table).toContain("元");
    expect(table).not.toContain("CNY");
  });

  it("后端起草态真码 drafting 显示「草拟」，不落到未知状态（M023-007）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-dddd4444")).toBeTruthy());
    const rowText = screen.getByTestId("merchant-zone-contract-row-contract-dddd4444").textContent ?? "";
    expect(rowText).toContain("草拟");
    expect(rowText).not.toContain("未知状态");
    expect(rowText).not.toContain("drafting");
    fireEvent.click(screen.getByTestId("merchant-zone-contract-row-contract-dddd4444"));
    const drawerText = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawerText).toContain("草拟");
    expect(drawerText).not.toContain("未知状态");
    expect(drawerText).not.toContain("drafting");
  });

  it("租期列为中文日期，不含 ISO 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contracts-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-contracts-table").textContent ?? "";
    expect(table).toContain("2026年1月1日");
    expect(table).not.toContain("2026-01-01");
  });

  it("合同号 / 空间 / 续签自为纯文本，不渲染商户点了没反应的下钻按钮（M023-005）", async () => {
    const { container, props } = renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111")).toBeTruthy());
    expect(container.querySelector('[data-testid^="trace-link-"]')).toBeNull();
    const row = screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111").textContent ?? "";
    expect(row).toContain("aaaa1111");
    expect(row).toContain("sp-1、sp-2");
    expect(row).toContain("bbbb2222");
    expect(props.navigateTo).not.toHaveBeenCalled();
  });
});

describe("MerchantZoneContractsPage 行明细抽屉（整行可点·日期中文）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/contracts");
  });

  it("点合同行 -> 抽屉含合同号全量、租期中文日期、中文状态、押金与空间，无下钻假按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-contract-row-contract-aaaa1111"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("contract-aaaa1111");
    expect(text).toContain("2026年1月1日");
    expect(text).toContain("已生效");
    expect(text).not.toContain("履约中");
    expect(text).toContain("1,000.00 元");
    expect(text).toContain("sp-1、sp-2");
    expect(text).toContain("contract-bbbb2222");
    expect(text).not.toContain("CNY");
    expect(text).not.toContain("effective");
    expect(drawer.querySelector('[data-testid^="trace-link-"]')).toBeNull();
  });

  it("点合同行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-contract-row-contract-bbbb2222")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-contract-row-contract-bbbb2222"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
