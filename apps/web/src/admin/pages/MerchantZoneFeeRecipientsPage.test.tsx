import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneFeeRecipientsPage } from "./MerchantZoneFeeRecipientsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const RECIPIENTS = [
  { id: "fr-1", fee_event: "rent", recipient_name: "王店长", recipient_phone: "13800000001", status: "active" },
  { id: "fr-2", fee_event: "utilities", recipient_name: "李工", recipient_phone: "13800000002", status: "inactive" },
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
    if (url.includes("fee-notification-recipients")) return Promise.resolve(envelope(RECIPIENTS));
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
  };
  render(<MerchantZoneFeeRecipientsPage {...props} />);
}

describe("MerchantZoneFeeRecipientsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/fee-recipients");
  });

  it("点“生效中” → URL 带 ?kpi=recipients&subset=active，聚焦只剩生效接收人", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-fee-recipients-kpi-active")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipients-kpi-active"));
    expect(window.location.search).toContain("kpi=recipients");
    expect(window.location.search).toContain("subset=active");
    expect(screen.getByTestId("merchant-zone-fee-recipients-kpi-focus-banner").textContent).toContain("正在查看：生效中接收人（1）");
    expect(screen.getByTestId("merchant-zone-fee-recipient-off-fr-1")).toBeTruthy();
    expect(screen.queryByTestId("merchant-zone-fee-recipients-summary")).toBeNull();
    // 创建表单在聚焦视图外，始终可见（非表，不进聚焦）。
    expect(screen.getByTestId("merchant-zone-fee-recipient-form")).toBeTruthy();
  });

  it("聚焦后点「返回全部」→ 恢复 KPI 卡+全量接收人", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-fee-recipients-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipients-kpi-total"));
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipients-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-fee-recipients-summary")).toBeTruthy();
  });

  it("费用事件列为中文，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-fee-recipients-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-fee-recipients-table").textContent ?? "";
    expect(table).toContain("租金");
    expect(table).toContain("能耗");
    expect(table).not.toContain("rent");
    expect(table).not.toContain("utilities");
  });
});
