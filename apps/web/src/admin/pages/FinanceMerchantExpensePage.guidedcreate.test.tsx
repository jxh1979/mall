import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceMerchantExpensePage } from "./FinanceMerchantExpensePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 商户费用台账：引导式手工录入（理由必填两步校验）+ 红字冲销 + 期间汇总卡。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "商户甲", status: "active" }];
const ENTRIES = [
  { id: "mex_m_1", merchant_id: "merchant-1", period: "2026-07", expense_type: "labor", amount_cent: 500000, source: "manual", source_ref: null, note: "七月人工", operator_user_id: "usr_admin", created_at: "2026-07-18T00:00:00Z" },
  { id: "mex_ae_1", merchant_id: "merchant-1", period: "2026-07", expense_type: "energy", amount_cent: 88800, source: "auto_energy", source_ref: "arap_energy_1", note: "能耗费自动挂接", operator_user_id: "usr_admin", created_at: "2026-07-18T00:00:00Z" },
];
const SUMMARY = [
  { merchant_id: "merchant-1", period: "2026-07", expense_type: "labor", total_cent: 500000 },
  { merchant_id: "merchant-1", period: "2026-07", expense_type: "energy", total_cent: 88800 },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostUrl: string | null = null;
let lastPostBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        if (url.includes("/reverse")) return Promise.resolve(envelope({ id: "mex_r_mex_m_1", amount_cent: -500000 }));
        if (url.includes("/sync-auto")) return Promise.resolve(envelope({ period: "2026-07", energy_inserted: 1, property_inserted: 0 }));
        return Promise.resolve(envelope({ id: "mex_m_new" }));
      }
      if (url.includes("/merchant-expenses/summary")) return Promise.resolve(envelope(SUMMARY));
      if (url.includes("/merchant-expenses")) {
        return Promise.resolve(envelope({ items: ENTRIES, page: 1, page_size: 20, total: ENTRIES.length }));
      }
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceMerchantExpensePage {...props} />);
  return { setActionResult };
}

describe("FinanceMerchantExpensePage 商户费用台账", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostUrl = null;
    lastPostBody = null;
  });

  it("列表 + 期间汇总卡展示；默认不展示录入弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-expense-row-mex_m_1")).toBeTruthy());
    expect(screen.getByTestId("merchant-expense-row-mex_ae_1")).toBeTruthy();
    await waitFor(() => expect(screen.getByTestId("merchant-expense-summary-total").textContent).toContain("5,888.00"));
    expect(screen.queryByTestId("merchant-expense-create-guided")).toBeNull();
    // 自动挂接行不提供冲销以外的限制——手工行与自动行都可冲销，冲销行（manual+source_ref）没有冲销按钮。
    expect(screen.getByTestId("merchant-expense-reverse-open-mex_ae_1")).toBeTruthy();
  });

  it("引导式手工录入：步进→理由必填→提交带正确 body（元转分）", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-expense-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-expense-create-open"));

    // singlePage 模式：全部字段一屏。理由留空点下一步 → 校验拦截，不进确认页。
    fireEvent.change(screen.getByTestId("merchant-expense-create-period"), { target: { value: "2026-07" } });
    fireEvent.change(screen.getByTestId("merchant-expense-create-type"), { target: { value: "amortization" } });
    fireEvent.change(screen.getByTestId("merchant-expense-create-amount"), { target: { value: "1200.50" } });
    fireEvent.click(screen.getByTestId("merchant-expense-create-guided-next"));
    expect(screen.getByTestId("merchant-expense-create-guided-error").textContent).toContain("理由");
    expect(screen.queryByTestId("merchant-expense-create-guided-summary")).toBeNull();

    fireEvent.change(screen.getByTestId("merchant-expense-create-note"), { target: { value: "七月设备摊销" } });
    fireEvent.click(screen.getByTestId("merchant-expense-create-guided-next"));

    await waitFor(() => expect(screen.getByTestId("merchant-expense-create-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-expense-create-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    const body = lastPostBody as Record<string, unknown>;
    expect(body.merchant_id).toBe("merchant-1");
    expect(body.period).toBe("2026-07");
    expect(body.expense_type).toBe("amortization");
    expect(body.amount_cent).toBe(120050);
    expect(body.note).toBe("七月设备摊销");
    await waitFor(() => expect(screen.queryByTestId("merchant-expense-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "merchant_expense_created" }));
  });

  it("红字冲销：理由必填，提交打到 /{id}/reverse", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-expense-reverse-open-mex_m_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-expense-reverse-open-mex_m_1"));

    fireEvent.click(screen.getByTestId("merchant-expense-reverse-guided-next"));
    expect(screen.getByTestId("merchant-expense-reverse-guided-error").textContent).toContain("理由");
    fireEvent.change(screen.getByTestId("merchant-expense-reverse-note"), { target: { value: "金额录错，冲销更正" } });
    fireEvent.click(screen.getByTestId("merchant-expense-reverse-guided-next"));
    await waitFor(() => expect(screen.getByTestId("merchant-expense-reverse-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-expense-reverse-save"));

    await waitFor(() => expect(lastPostUrl).toContain("/api/v1/finance/merchant-expenses/mex_m_1/reverse"));
    expect((lastPostBody as Record<string, unknown>).note).toBe("金额录错，冲销更正");
    await waitFor(() => expect(screen.queryByTestId("merchant-expense-reverse-guided")).toBeNull());
  });
});
