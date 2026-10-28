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

describe("MerchantZoneBillsPage 行明细抽屉（整行可点·日期中文）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/bills");
  });

  it("点账单行 -> 抽屉含账单号+金额+中文状态，来源合同 TraceLink 保留", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-bill-row-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-bill-row-b-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("B-1");
    expect(text).toContain("已出账");
    expect(text).not.toContain("issued");
    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-c-1"]')).toBeTruthy();
  });

  it("点账单行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-bill-row-b-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-bill-row-b-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点来源合同 TraceLink 触发下钻、不弹抽屉（stopPropagation）", async () => {
    const navigateTo = vi.fn();
    const props: AdminPageProps = {
      api: makeApi(),
      user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo,
    };
    render(<MerchantZoneBillsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-CONT-001-c-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("trace-link-ADM-CONT-001-c-1"));
    expect(navigateTo).toHaveBeenCalled();
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

// 18E 第 21 条（BSM:1323「商户专区须设一张费用总表……系统须向商户出具一张综合账单，把当期各名目的费用汇总列示」）。
describe("MerchantZoneBillsPage 费用总表与当期综合账单", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/bills");
  });

  const STATEMENT = {
    month: "2026-09",
    merchant_id: "m-1",
    merchant_name: "测试店",
    fee_items: [
      { fee_name: "固定租金", billing_basis: "按合同约定", bearer: "测试店", amount_cent: 1000000 },
      { fee_name: "会员系统服务费", billing_basis: "每份商户合同在“按有效会员消费金额比例”或“固定月费”中二选一", bearer: "测试店", amount_cent: null },
    ],
    bill: {
      lines: [{ fee_name: "固定租金", amount_cent: 1000000, paid_amount_cent: 400000, unpaid_amount_cent: 600000 }],
      total_amount_cent: 1000000,
      paid_amount_cent: 400000,
      unpaid_amount_cent: 600000,
    },
  };

  it("没有账单时也显示两块，未出账名目显示「本期未出账」，切换月份请求带 ?month=", async () => {
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/fee-statement")) {
        return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: STATEMENT }),
          { status: 200, headers: { "content-type": "application/json" } }));
      }
      return Promise.resolve(envelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo: vi.fn(),
    };
    render(<MerchantZoneBillsPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-fee-statement")?.textContent ?? "").toContain("费用总表"));
    expect(screen.getByTestId("merchant-zone-fee-statement").textContent).toContain("当期综合账单");
    const items = screen.getByTestId("merchant-zone-fee-items").textContent ?? "";
    expect(items).toContain("按合同约定");
    expect(items).toContain("本期未出账");
    expect(items).toContain("10,000.00");
    const bill = screen.getByTestId("merchant-zone-fee-bill").textContent ?? "";
    expect(bill).toContain("合计");
    expect(bill).toContain("4,000.00");
    expect(bill).toContain("6,000.00");
    const month = screen.getByTestId("merchant-zone-fee-month") as HTMLInputElement;
    expect(month.type).toBe("month");
    expect(month.value).toBe("2026-09");
    fireEvent.change(month, { target: { value: "2026-08" } });
    await waitFor(() => expect(fetchMock.mock.calls.map((call) => String(call[0])).some((url) =>
      url.includes("/api/v1/merchant-portal/fee-statement?month=2026-08"))).toBe(true));
  });
});
