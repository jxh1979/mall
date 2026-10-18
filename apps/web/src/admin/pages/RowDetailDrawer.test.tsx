// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import { FinanceArApLedgerPage } from "./FinanceArApLedgerPage";
import { ServiceOrdersPage } from "./ServiceOrdersPage";
import { cnDate } from "../components/RowDetailDrawer";
import type { PayrollStatementDetail } from "./staffPayrollTypes";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, FinanceLedgerRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function baseProps(api: AdminApiClient): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
}

describe("cnDate 中文日期格式化（禁 ISO/mm-dd 直出）", () => {
  it("纯日期 → 年月日", () => {
    expect(cnDate("2026-07-05")).toBe("2026年7月5日");
  });
  it("带时间 → 年月日 时:分", () => {
    expect(cnDate("2026-07-05T13:20:00Z")).toBe("2026年7月5日 13:20");
  });
  it("空值 → 破折号", () => {
    expect(cnDate(undefined)).toBe("—");
  });
});

describe("薪酬页 整行可点 → 行明细抽屉", () => {
  const PAYROLL: [PayrollStatementDetail["statement"]] = [{ id: "pr-1", userId: "staff-7", month: "2026-07", baseCent: 800000, commissionCent: 120000, bonusCent: 50000, penaltyCent: 0, taxCent: 30000, totalCent: 940000 }];
  const DETAIL: PayrollStatementDetail = {
    statement: PAYROLL[0], status: "closed", statementKind: "monthly",
    schemeVersionId: "scheme-1", factsId: "facts-1", paymentDueDate: "2026-08-05",
    paymentDueBasis: {}, arApItemId: "ap-1", paidAmountCent: 0,
    lines: [], worktime: null, corrections: [], payments: [], pendingItems: [],
  };
  const LEAVES = [{ id: "lv-1", userId: "staff-7", leaveType: "sick", startDate: "2026-07-03", endDate: "2026-07-05", reason: "感冒", status: "pending" }];

  function makeApi(): AdminApiClient {
    return {
      get: vi.fn(async (url: string) => {
        if (url === "/api/v1/staff/payroll/statements/pr-1") return DETAIL;
        // 页面分两次拉 ?status=pending 与 ?status=revoke_requested（2026-10-18 销假裁决），只给待审那次返回 lv-1，避免同 testid 重复
        if (url === "/api/v1/staff/leaves?status=pending") return LEAVES;
        if (/\/staff\/payroll\?/.test(url)) return PAYROLL;
        return [];
      }),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }

  it("点薪资单行 → 抽屉打开且显示该行金额（带 ¥）", async () => {
    const api = makeApi();
    render(<StaffPayrollLeavePage {...baseProps(api)} />);
    await waitFor(() => expect(screen.getByTestId("payroll-row-pr-1")).toBeTruthy());
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
    fireEvent.click(screen.getByTestId("payroll-row-pr-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    expect(api.get).toHaveBeenCalledWith("/api/v1/staff/payroll/statements/pr-1");
    expect(drawer).toBeTruthy();
    expect(drawer.textContent).toContain("应发净额");
    expect(drawer.textContent).toContain("¥9,400.00"); // 应发净额 940000 分，未实际付款
    expect(drawer.textContent).toContain("实际已付");
    expect(drawer.textContent).toContain("¥0.00");
    expect(drawer.textContent).toContain("已定格待付");
    expect(drawer.textContent).toMatch(/2026年0?7月/); // 所属月份中文
  });

  it("Esc 关闭抽屉", async () => {
    render(<StaffPayrollLeavePage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("payroll-row-pr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-row-pr-1"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点遮罩关闭抽屉", async () => {
    render(<StaffPayrollLeavePage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("payroll-row-pr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-row-pr-1"));
    fireEvent.click(await screen.findByTestId("row-detail-overlay"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("请假行抽屉：类型/状态枚举中文、日期中文，不含英文原文", async () => {
    render(<StaffPayrollLeavePage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("leave-row-lv-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leave-row-lv-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("病假");
    expect(drawer.textContent).toContain("待审");
    expect(drawer.textContent).toContain("2026年7月3日");
    // 值区不得出现枚举英文原码/ISO 原文
    expect(drawer.textContent).not.toContain("sick");
    expect(drawer.textContent).not.toContain("pending");
    expect(drawer.textContent).not.toContain("2026-07-03");
  });
});

describe("应收应付台账 整行可点 → 行明细抽屉（单选不触发行抽屉）", () => {
  const ROWS: FinanceLedgerRow[] = [
    { id: "ar-1", itemNo: "AR-0001", direction: "receivable", status: "open", amountCent: 10000, paidAmountCent: 0, unpaidAmountCent: 10000, dueDate: "2026-08-01" },
  ];
  function makeApi(): AdminApiClient {
    return {
      get: vi.fn().mockResolvedValue([]),
      getPaged: vi.fn().mockResolvedValue({ items: ROWS, total: 1, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }

  it("点台账行 → 抽屉打开，方向枚举中文、金额¥、到期日中文", async () => {
    render(<FinanceArApLedgerPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("finance-ledger-row-ar-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("¥100.00");
    expect(drawer.textContent).toContain("2026年8月1日");
    expect(drawer.textContent).not.toContain("receivable"); // 方向已中文化
    expect(drawer.textContent).not.toContain("2026-08-01");
  });

  it("点行内单选按钮不打开抽屉（stopPropagation），仅选中该行", async () => {
    render(<FinanceArApLedgerPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("finance-ledger-select-ar-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("finance-ledger-select-ar-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

describe("服务点单 整行可点 → 行明细抽屉（履约人 TraceLink 不触发行抽屉）", () => {
  const CATALOG = [{ id: "c-1", name: "点舞", priceCent: 20000, durationMinutes: 10, active: true }];
  const ORDERS = [{ id: "so-1", itemName: "点舞", priceCent: 20000, tableId: "T-1", performerUserId: "staff-9", status: "paid", mgmtFeeCent: 6000, dancerShareCent: 14000, createdAt: "2026-07-05T10:00:00Z" }];
  function makeApi(): AdminApiClient {
    return {
      get: vi.fn(async (url: string) => (url.includes("/catalog") ? CATALOG : ORDERS)),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }

  it("点服务单行 → 抽屉显示单号/状态中文/下单时间中文", async () => {
    render(<ServiceOrdersPage {...baseProps(makeApi())} navigateTo={vi.fn()} />);
    await waitFor(() => expect(screen.getByTestId("service-order-row-so-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("service-order-row-so-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("so-1");
    expect(drawer.textContent).toContain("已收款"); // paid → 中文
    expect(drawer.textContent).toContain("2026年7月5日");
  });

  it("点履约人 TraceLink 触发下钻而非行抽屉", async () => {
    const navigateTo = vi.fn();
    render(<ServiceOrdersPage {...baseProps(makeApi())} navigateTo={navigateTo} />);
    await waitFor(() => expect(screen.getByTestId("service-order-row-so-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("trace-link-ADM-ACC-001-staff-9"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ACC-001", "staff-9");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
