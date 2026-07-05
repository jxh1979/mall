import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ReceivablesAnalyticsPage } from "./ReceivablesAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = { businessDate: "2026-07-05", metrics: { "ar.balance": { value: 123456 } } };
const DEBTORS = [
  { merchantId: "m-7", merchantName: "老王烧烤", outstandingCent: 456700, oldestDueDate: "2026-06-01", overdueDays: 34, bucket: "a31_60" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/dashboards/finance") ? DASH : DEBTORS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
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

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("receivables-debtor-row-m-7")).toBeTruthy());
    fireEvent.click(screen.getByTestId("receivables-debtor-row-m-7"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
