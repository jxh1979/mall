// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// FS-014（BSM L1785「正式分红应付款形成前，由财务……确认依法可分配金额，本次分配不得超出该金额」）：
// 草稿正式单须先登记依法可分配金额才可提交审阅；登记按元录入、以分提交到 legal-confirmation。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const BASE = {
  merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-Q3",
  period_start: "2026-07-01", period_end: "2026-09-30", net_profit_cent: 1000000, distributable_cent: 1000000,
  principal_gross_cent: 200000, tax_cent: 40000, principal_net_cent: 160000, payout_cent: 160000, status: "draft",
};
const UNCONFIRMED = { ...BASE, id: "divs_nolegal", legal_distributable_cent: null };
const CONFIRMED = { ...BASE, id: "divs_legal", merchant_id: "merchant-1", legal_distributable_cent: 900000 };

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastWriteUrl: string | null = null;
let lastWriteBody: Record<string, unknown> | null = null;
function makeApi(): AdminApi {
  lastWriteUrl = null;
  lastWriteBody = null;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method === "POST" || method === "PUT") {
        lastWriteUrl = url;
        lastWriteBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(envelope({ ...CONFIRMED, id: "divs_new" }));
      }
      if (url.includes("/dividends/reserve-releases")) return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      if (url.includes("/dividends/statements")) {
        return Promise.resolve(envelope({ items: [UNCONFIRMED, CONFIRMED], page: 1, page_size: 20, total: 2 }));
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
    user: { id: "u-fin", username: "finance", accountType: "platform" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceDividendsPage {...props} />);
}

describe("FinanceDividendsPage 依法可分配金额前置", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("草稿单未登记核定时不显示提交审阅，显示登记入口", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-statement-row-divs_nolegal")).not.toBeNull());
    expect(screen.queryByTestId("dividend-submit-divs_nolegal")).toBeNull();
    expect(screen.queryByTestId("dividend-legal-open-divs_nolegal")).not.toBeNull();
    expect(screen.getByTestId("dividend-statement-row-divs_nolegal").textContent).toContain("须先登记依法可分配金额");
  });

  it("已登记核定的草稿单显示提交审阅", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-submit-divs_legal")).not.toBeNull());
  });

  it("登记弹窗按元录入，以分提交到 legal-confirmation 并带财税核算依据与分配决议", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-statement-row-divs_nolegal")).not.toBeNull());
    expect(screen.queryByTestId("dividend-legal-open-divs_nolegal")).not.toBeNull();
    fireEvent.click(screen.getByTestId("dividend-legal-open-divs_nolegal"));
    await waitFor(() => expect(screen.queryByTestId("dividend-legal-amount")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dividend-legal-amount"), { target: { value: "9000.50" } });
    fireEvent.change(screen.getByTestId("dividend-legal-tax-ref"), { target: { value: "财税核算报告 TA-2026Q3" } });
    fireEvent.change(screen.getByTestId("dividend-legal-resolution-ref"), { target: { value: "股东会分配决议 R-2026-10" } });
    fireEvent.click(screen.getByTestId("dividend-legal-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("dividend-legal-guided-summary")).not.toBeNull());
    fireEvent.click(screen.getByTestId("dividend-legal-save"));
    await waitFor(() => expect(lastWriteUrl).toContain("/dividends/statements/divs_nolegal/legal-confirmation"));
    expect(lastWriteBody?.legal_distributable_cent).toBe(900050);
    expect(lastWriteBody?.tax_accounting_ref).toBe("财税核算报告 TA-2026Q3");
    expect(lastWriteBody?.resolution_ref).toBe("股东会分配决议 R-2026-10");
  });
});
