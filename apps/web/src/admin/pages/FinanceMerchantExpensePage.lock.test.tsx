import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceMerchantExpensePage } from "./FinanceMerchantExpensePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 分红期间关账锁 UI：已关账月份的商户 → 显示锁定提示 + 禁用录入/冲销。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "商户甲", status: "active" }];
const ENTRIES = [
  { id: "mex_m_1", merchant_id: "merchant-1", period: "2026-07", expense_type: "labor", amount_cent: 500000, source: "manual", source_ref: null, note: "七月人工", operator_user_id: "usr_admin", created_at: "2026-07-18T00:00:00Z" },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastCreatePostUrl: string | null = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        if (url.endsWith("/finance/merchant-expenses")) lastCreatePostUrl = url;
        return Promise.resolve(envelope({ id: "mex_m_new" }));
      }
      // 关账锁清单：该月 merchant-1 已关账。必须在 /merchant-expenses 通配之前判定（子路径）。
      if (url.includes("/merchant-expenses/period-locks")) return Promise.resolve(envelope(["merchant-1"]));
      if (url.includes("/merchant-expenses/summary")) return Promise.resolve(envelope([]));
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
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceMerchantExpensePage {...props} />);
}

describe("FinanceMerchantExpensePage 分红期间关账锁", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastCreatePostUrl = null;
  });

  it("已关账月份显示锁定提示，且该商户行不再提供红字冲销按钮", async () => {
    renderPage();
    // 固定期间为 2026-07（与台账行月份一致，锁定判定生效，不依赖系统时钟）。
    fireEvent.change(screen.getByTestId("merchant-expense-filter-period"), { target: { value: "2026-07" } });
    await waitFor(() => expect(screen.getByTestId("merchant-expense-lock-panel")).toBeTruthy());
    expect(screen.getByTestId("merchant-expense-lock-merchant-1")).toBeTruthy();
    // 关账行的冲销按钮被撤下（后端 requirePeriodOpen 是硬闸，前端同步禁用）。
    await waitFor(() => expect(screen.getByTestId("merchant-expense-row-mex_m_1")).toBeTruthy());
    expect(screen.queryByTestId("merchant-expense-reverse-open-mex_m_1")).toBeNull();
  });

  it("对已关账商户+月份的手工录入被前置拦截，不发起创建请求", async () => {
    renderPage();
    fireEvent.change(screen.getByTestId("merchant-expense-filter-period"), { target: { value: "2026-07" } });
    await waitFor(() => expect(screen.getByTestId("merchant-expense-lock-panel")).toBeTruthy());

    fireEvent.click(screen.getByTestId("merchant-expense-create-open"));
    fireEvent.change(screen.getByTestId("merchant-expense-create-period"), { target: { value: "2026-07" } });
    fireEvent.change(screen.getByTestId("merchant-expense-create-amount"), { target: { value: "100.00" } });
    fireEvent.click(screen.getByTestId("merchant-expense-create-guided-next"));
    fireEvent.change(screen.getByTestId("merchant-expense-create-note"), { target: { value: "关账后补录" } });
    fireEvent.click(screen.getByTestId("merchant-expense-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("merchant-expense-create-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-expense-create-save"));

    await waitFor(() =>
      expect(screen.getByTestId("merchant-expense-create-guided-error").textContent).toContain("关账"));
    expect(lastCreatePostUrl).toBeNull();
  });
});
