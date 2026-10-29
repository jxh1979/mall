import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ReceivablesAnalyticsPage } from "./ReceivablesAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = { businessDate: "2026-07-05", metrics: { "ar.balance": { value: 123456 } } };
const DEBTORS = [
  { merchantId: "m-7", merchantName: "老王烧烤", outstandingCent: 456700, oldestDueDate: "2026-06-01", overdueDays: 34, bucket: "a31_60" },
];

// 18F 第 80 条：跨项目经营的商户在两个项目各一行。
const SPLIT_DEBTORS = [
  { merchantId: "m-8", merchantName: "跨项目商户", outstandingCent: 100, oldestDueDate: "2026-06-01", overdueDays: 34, bucket: "a31_60", projectId: "prj_a" },
  { merchantId: "m-8", merchantName: "跨项目商户", outstandingCent: 900, oldestDueDate: "2026-06-01", overdueDays: 34, bucket: "a31_60", projectId: "prj_b" },
];

function makeApi(debtors: unknown[] = DEBTORS): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/dashboards/finance") ? DASH : debtors)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(debtors?: unknown[]) {
  const props: AdminPageProps = {
    api: makeApi(debtors),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
  render(<ReceivablesAnalyticsPage {...props} />);
}

describe("ReceivablesAnalyticsPage 欠费商户行明细抽屉", () => {
  it("点欠费商户行 → 抽屉含该行值(账龄段中文·应收日中文)，不直出 ISO/英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("receivables-debtor-row-m-7")).toBeTruthy());
    fireEvent.click(screen.getByTestId("receivables-debtor-row-m-7"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("老王烧烤");
    expect(text).toContain("31-60 天"); // bucket a31_60 → 中文
    expect(text).toContain("2026年6月1日"); // oldestDueDate ISO → 中文
    expect(text).not.toContain("a31_60");
    expect(text).not.toContain("2026-06-01");
  });

  it("18F 第 80 条：同一商户在两个项目各一行、各显示自己的项目，抽屉也带项目", async () => {
    renderPage(SPLIT_DEBTORS);
    await waitFor(() => expect(screen.queryByTestId("receivables-debtor-row-m-8-prj_a")?.textContent).toContain("prj_a"));
    expect(screen.getByTestId("receivables-debtor-row-m-8-prj_a").textContent).toContain("1.00");
    expect(screen.getByTestId("receivables-debtor-row-m-8-prj_b").textContent).toContain("prj_b");
    expect(screen.getByTestId("receivables-debtor-row-m-8-prj_b").textContent).toContain("9.00");
    expect(within(screen.getByTestId("receivables-debtors-table")).getByText("项目")).toBeTruthy();
    fireEvent.click(screen.getByTestId("receivables-debtor-row-m-8-prj_b"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("prj_b");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("receivables-debtor-row-m-7")).toBeTruthy());
    fireEvent.click(screen.getByTestId("receivables-debtor-row-m-7"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
