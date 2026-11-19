// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BA 段第 314 条 314-R1（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」）：最高权限本人提交分红计算单即审批通过，
// 提交接口返回已审批的单，结果提示按「分红已批准」显示。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const DRAFT = {
  id: "divs_master", merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-Q3",
  period_start: "2026-07-01", period_end: "2026-09-30", net_profit_cent: 1000000, distributable_cent: 1000000,
  principal_gross_cent: 200000, tax_cent: 40000, principal_net_cent: 160000, payout_cent: 160000, status: "draft",
  legal_distributable_cent: 1000000,
};

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "text/plain" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method === "POST" && url.includes("/dividends/statements/divs_master/submit")) {
        return Promise.resolve(envelope({ ...DRAFT, status: "approved", submitted_by: "usr_master_authority", approved_by: "usr_master_authority" }));
      }
      if (url.includes("/dividends/reserve-releases")) return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      if (url.includes("/dividends/statements")) return Promise.resolve(envelope({ items: [DRAFT], page: 1, page_size: 20, total: 1 }));
      if (url.includes("/dividends/plans")) return Promise.resolve(envelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("FinanceDividendsPage 最高权限本人提交即审批通过", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("提交接口返回已审批的单时，结果按分红已批准显示", async () => {
    const setActionResult = vi.fn();
    const props: AdminPageProps = {
      api: makeApi(),
      user: { id: "usr_master_authority", username: "master", accountType: "master_authority" },
      setActionResult,
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<FinanceDividendsPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("dividend-submit-divs_master")).not.toBeNull());
    await waitFor(() => expect(screen.queryByText("加载中")).toBeNull());

    fireEvent.click(screen.getByTestId("dividend-submit-divs_master"));

    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "dividend_approved" })));
  });
});
