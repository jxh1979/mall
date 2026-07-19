import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// V304 发放保护线留存·待释放台账 + 手动释放（re-tag 待释放→已释放，全额返还主理人）。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const PENDING = {
  statement_id: "divs_pending_res", merchant_id: "merchant-1", principal_user_id: "usr_principal",
  period_label: "2026-Q2", period_start: "2026-04-01", period_end: "2026-06-30",
  statement_status: "approved", reserve_withheld_cent: 16000, released: false,
  trigger_type: null, released_by: null, released_at: null,
};
const RELEASED = {
  statement_id: "divs_released_res", merchant_id: "merchant-1", principal_user_id: "usr_principal",
  period_label: "2026-Q1", period_start: "2026-01-01", period_end: "2026-03-31",
  statement_status: "paid", reserve_withheld_cent: 12000, released: true,
  trigger_type: "manual", released_by: "usr_admin", released_at: "2026-07-19T04:00:00Z",
};

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastWriteUrl: string | null = null;
function makeApi(): AdminApi {
  lastWriteUrl = null;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method === "POST" || method === "PUT") {
        lastWriteUrl = url;
        return Promise.resolve(envelope({ id: "divrr_1", statement_id: "divs_pending_res", reserve_cent: 16000 }));
      }
      if (url.includes("/dividends/reserve-releases")) {
        const released = new URL(url, "http://x").searchParams.get("released");
        const items = released === "released" ? [RELEASED] : released === "pending" ? [PENDING] : [PENDING, RELEASED];
        return Promise.resolve(envelope({ items, page: 1, page_size: 20, total: items.length }));
      }
      if (url.includes("/dividends/statements")) {
        return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      }
      if (url.includes("/dividends/plans")) return Promise.resolve(envelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u-admin", username: "admin", accountType: "platform" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceDividendsPage {...props} />);
}

describe("FinanceDividendsPage 发放保护线留存·释放", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("默认待释放台账展示留存额 + 待释放状态 + 释放按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-row-divs_pending_res")).toBeTruthy());
    const row = screen.getByTestId("dividend-reserve-row-divs_pending_res");
    expect(row.textContent).toContain("160.00");    // 留存额（元）
    expect(row.textContent).toContain("待释放");
    expect(screen.getByTestId("dividend-reserve-release-divs_pending_res")).toBeTruthy();
  });

  it("点释放留存 → 二次确认后 POST 到 release-reserve 端点", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);   // 资金动作二次确认
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-release-divs_pending_res")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-reserve-release-divs_pending_res"));
    await waitFor(() => expect(lastWriteUrl).toContain("/dividends/statements/divs_pending_res/release-reserve"));
  });

  it("二次确认取消 → 不发起释放请求", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-release-divs_pending_res")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-reserve-release-divs_pending_res"));
    expect(lastWriteUrl).toBeNull();
  });

  it("切到已释放筛选：展示已释放状态 + 释放方式/操作人，无释放按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-row-divs_pending_res")).toBeTruthy());
    fireEvent.change(screen.getByTestId("dividend-reserve-filter-status"), { target: { value: "released" } });
    await waitFor(() => expect(screen.getByTestId("dividend-reserve-row-divs_released_res")).toBeTruthy());
    const row = screen.getByTestId("dividend-reserve-row-divs_released_res");
    expect(row.textContent).toContain("已释放");
    expect(row.textContent).toContain("手动");
    expect(screen.queryByTestId("dividend-reserve-release-divs_released_res")).toBeNull();
  });
});
