// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AC 段第 210 条 210-R3（BSM:1774「……以及该期间的各项税费（含增值税、所得税及其他应由该主体承担的税费）；全部扣完、税后剩下的是本期经营口径净利润」；
// 第十八批裁决 42「分红扣本期内已生效的申报金额合计」）：计算单详情列出该期税费合计与逐税种「该期税费」明细行。
const STATEMENT = {
  id: "divs_tax", merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-Q1",
  period_start: "2026-01-01", period_end: "2026-03-31", revenue_cent: 1000000, cost_cent: 0, expense_cent: 30000,
  period_tax_cent: 62345, net_profit_cent: 907655, status: "draft", legal_distributable_cent: null,
};
const LINES = [
  { id: "divs_tax_1", statement_id: "divs_tax", seq: 1, line_type: "tax_period", amount_cent: -50000,
    detail: "企业所得税实际申报金额（已生效登记，所属期 2026-01～2026-03）" },
  { id: "divs_tax_2", statement_id: "divs_tax", seq: 2, line_type: "tax_period", amount_cent: -12345,
    detail: "增值税实际申报金额（已生效登记，所属期 2026-01）" },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dividends/statements/divs_tax")) return Promise.resolve(envelope({ statement: STATEMENT, lines: LINES }));
      if (url.includes("/dividends/reserve-releases")) return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      if (url.includes("/dividends/statements")) return Promise.resolve(envelope({ items: [STATEMENT], page: 1, page_size: 20, total: 1 }));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope([{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }]));
      return Promise.resolve(envelope([]));
    }),
  );
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u-fin", username: "finance", accountType: "platform" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceDividendsPage {...props} />);
}

describe("FinanceDividendsPage 该期税费", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("计算单详情列出该期税费合计与逐税种「该期税费」行", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-statement-row-divs_tax")?.textContent).toContain("酒吧甲"));
    fireEvent.click(screen.getByRole("button", { name: "查看分红计算单明细" }));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).not.toBeNull());
    const fields = Array.from(screen.getByTestId("row-detail-drawer").querySelectorAll(".admin-drawer-field"))
      .map((field) => `${field.querySelector("dt")?.textContent}=${field.querySelector("dd")?.textContent}`);
    expect(fields).toContain("该期税费合计=¥623.45");
    expect(fields).toContain("该期税费=¥-500.00（企业所得税实际申报金额（已生效登记，所属期 2026-01～2026-03））");
    expect(fields).toContain("该期税费=¥-123.45（增值税实际申报金额（已生效登记，所属期 2026-01））");
  });
});
