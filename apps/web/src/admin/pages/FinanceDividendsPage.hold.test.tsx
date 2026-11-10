// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AC 段第 211 条 211-R1（BSM:1777「若因特殊情况需要整期暂不分红或暂缓分红的，须由主理人与娱乐平台公司协商一致后执行，并记录原因与协商结果」；
// 第十八批裁决 43「平台财务在持股分红页按商户按期登记（暂不分红或暂缓分红，原因、协商结果必填，不另走审批）；……「暂缓分红」等协商恢复后由财务登记恢复，
// 再照常出单」）：持股分红页的登记列表、登记弹窗与暂缓行的「恢复分红」。
const HOLDS = [
  {
    id: "divh_defer", merchant_id: "merchant-1", period_label: "2026-08", hold_type: "defer", reason: "主理人装修停业",
    negotiation_result: "双方同意待十月复业后再分", recorded_by: "usr_fin", recorded_at: "2026-09-02T02:00:00Z",
    lifted_by: null, lifted_at: null, lift_result: null,
  },
  {
    id: "divh_skip", merchant_id: "merchant-1", period_label: "2026-07", hold_type: "skip", reason: "门店整改",
    negotiation_result: "该月不分红", recorded_by: "usr_fin", recorded_at: "2026-08-02T02:00:00Z",
    lifted_by: null, lifted_at: null, lift_result: null,
  },
  {
    id: "divh_lifted", merchant_id: "merchant-1", period_label: "2026-06", hold_type: "defer", reason: "资金周转",
    negotiation_result: "暂缓一个月", recorded_by: "usr_fin", recorded_at: "2026-07-02T02:00:00Z",
    lifted_by: "usr_fin", lifted_at: "2026-07-20T02:00:00Z", lift_result: "双方同意恢复",
  },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

let writes: Array<{ url: string; body: Record<string, unknown> }> = [];

function renderPage() {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        writes.push({ url, body: JSON.parse(String(init?.body)) });
        return Promise.resolve(envelope(HOLDS[0]));
      }
      if (url.includes("/dividends/holds")) return Promise.resolve(envelope(HOLDS));
      if (url.includes("/dividends/statements") || url.includes("/dividends/reserve-releases")) {
        return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      }
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

describe("FinanceDividendsPage 暂不分红／暂缓分红登记", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    writes = [];
  });

  it("列表显示商户名与中文类别，「恢复分红」只在未恢复的暂缓行出现", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-hold-row-divh_defer")?.textContent).toContain("酒吧甲"));
    const deferRow = screen.getByTestId("dividend-hold-row-divh_defer").textContent;
    expect(deferRow).toContain("暂缓分红");
    expect(deferRow).toContain("主理人装修停业");
    expect(deferRow).toContain("双方同意待十月复业后再分");
    expect(screen.getByTestId("dividend-hold-row-divh_skip").textContent).toContain("暂不分红");
    expect(screen.getByTestId("dividend-hold-row-divh_lifted").textContent).toContain("已恢复");
    expect(screen.queryByTestId("dividend-hold-lift-open-divh_defer")).not.toBeNull();
    expect(screen.queryByTestId("dividend-hold-lift-open-divh_skip")).toBeNull();
    expect(screen.queryByTestId("dividend-hold-lift-open-divh_lifted")).toBeNull();
  });

  it("登记弹窗必填校验后按蛇形键提交；暂缓行「恢复分红」提交恢复的协商结果", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-hold-row-divh_defer")?.textContent).toContain("酒吧甲"));
    fireEvent.click(screen.getByTestId("dividend-hold-open"));
    fireEvent.change(screen.getByTestId("dividend-hold-period"), { target: { value: "2026-09" } });
    fireEvent.click(screen.getByTestId("dividend-hold-guided-next"));
    expect(screen.getByTestId("dividend-hold-guided-error").textContent).toContain("暂不分红或暂缓分红");
    expect(screen.queryByTestId("dividend-hold-guided-summary")).toBeNull();

    fireEvent.change(screen.getByTestId("dividend-hold-type"), { target: { value: "defer" } });
    fireEvent.change(screen.getByTestId("dividend-hold-reason"), { target: { value: "主理人装修停业" } });
    fireEvent.change(screen.getByTestId("dividend-hold-negotiation"), { target: { value: "双方同意待十月复业后再分" } });
    fireEvent.click(screen.getByTestId("dividend-hold-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-hold-guided-summary").textContent).toContain("暂缓分红"));
    fireEvent.click(screen.getByTestId("dividend-hold-save"));
    await waitFor(() => expect(writes.length).toBe(1));
    expect(writes[0]!.url).toContain("/api/v1/finance/dividends/holds");
    expect(writes[0]!.body).toMatchObject({
      merchant_id: "merchant-1", period_label: "2026-09", hold_type: "defer", reason: "主理人装修停业",
      negotiation_result: "双方同意待十月复业后再分",
    });
    expect(writes[0]!.body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(screen.queryByTestId("dividend-hold-guided")).toBeNull());

    fireEvent.click(screen.getByTestId("dividend-hold-lift-open-divh_defer"));
    fireEvent.click(screen.getByTestId("dividend-hold-lift-guided-next"));
    expect(screen.getByTestId("dividend-hold-lift-guided-error").textContent).toContain("协商结果");
    fireEvent.change(screen.getByTestId("dividend-hold-lift-result"), { target: { value: "复业后双方同意恢复分红" } });
    fireEvent.click(screen.getByTestId("dividend-hold-lift-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-hold-lift-guided-summary").textContent).toContain("复业后双方同意恢复分红"));
    fireEvent.click(screen.getByTestId("dividend-hold-lift-save"));
    await waitFor(() => expect(writes.length).toBe(2));
    expect(writes[1]!.url).toContain("/api/v1/finance/dividends/holds/divh_defer/lift");
    expect(writes[1]!.body).toMatchObject({ lift_result: "复业后双方同意恢复分红" });
    expect(writes[1]!.body.idempotency_key).toBeTruthy();
  });
});
