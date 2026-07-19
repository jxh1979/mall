import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketOrderRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const ORDERS: TicketOrderRow[] = [
  { id: "o-1", orderNo: "TKT-0001", ticketTypeId: "tt-1", quantity: 1, payStatus: "paid", issueStatus: "issued", status: "paid", amountCent: 9900, createdAt: "2026-07-05T09:00:00Z", paidAt: "2026-07-05T10:30:00Z" },
];

// failing=会抛错的路径子串（issued-entitlements 或 verifications）；主资源 /orders 恒成功。
function makeApi(failing: string): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes(failing)) throw new Error("boom");
      if (url.includes("/orders")) return ORDERS;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<TicketOrdersPage {...props} />);
}

describe("TicketOrdersPage 副资源降级（批2）", () => {
  afterEach(() => window.history.replaceState({}, "", "/admin/ticketing/orders"));

  it("权益接口失败 → 出票主表照常，权益KPI显“—”不静默归零，降级提示出现，整页不报错", async () => {
    const setGlobalError = vi.fn();
    renderPage(makeApi("issued-entitlements"), setGlobalError);
    await waitFor(() => expect(screen.getByTestId("ticket-orders-table")).toBeTruthy());
    expect(screen.getByTestId("ticket-orders-table").textContent).toContain("TKT-0001");
    // 权益副资源失败：KPI 显“—”而非 0
    expect(screen.getByTestId("ticket-orders-kpi-entitlements").textContent).toBe("—");
    expect(screen.getByTestId("ticket-orders-entitlements-degraded")).toBeTruthy();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });

  it("核销接口失败 → 核销KPI显“—”不静默归零，降级提示出现，主表仍在", async () => {
    renderPage(makeApi("verifications"));
    await waitFor(() => expect(screen.getByTestId("ticket-orders-table")).toBeTruthy());
    expect(screen.getByTestId("ticket-orders-kpi-verifications").textContent).toBe("—");
    expect(screen.getByTestId("ticket-orders-verifications-degraded")).toBeTruthy();
  });
});
