// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AQ 段第 234 条 234-R1（BSM:521「每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效」；
// PGM:666「持股分红的方案配置……由最高权限审批」）：方案表每行一个版本；修改＝在生效中或待生效版本上提交新版本（生效起始日期与理由必填），
// 待审批版本可跳审批原件，同商户有待审批版本时各行「修改」禁用。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const BASE = {
  merchant_id: "merchant-1", principal_user_id: "usr_principal", platform_share_bps: 8000, tax_rate_bps: 2000,
  period_type: "semiannual", period_min_months: 3, period_max_months: 12, revenue_domains: ["bar"], payout_pay_slot: 1,
  status: "active", reason: "按工商登记持股比例",
};
const IN_FORCE = { ...BASE, id: "divp_now", effective_from: "2026-01-01", version_state: "in_force" };
const SUPERSEDED = { ...BASE, id: "divp_old", effective_from: "2025-01-01", version_state: "superseded" };
const PENDING = {
  ...BASE, id: "divp_new", platform_share_bps: 7000, effective_from: "2026-12-01", reason: "主理人增资",
  status: "pending_review", review_id: "review_div_1", version_state: "pending",
};

let plans: unknown[] = [IN_FORCE, SUPERSEDED];
const writes: { method: string; url: string; body: Record<string, unknown> }[] = [];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    if (method === "POST" || method === "PUT") {
      writes.push({ method, url, body: init?.body ? JSON.parse(String(init.body)) : {} });
      plans = [PENDING, IN_FORCE, SUPERSEDED];
      return Promise.resolve(envelope(PENDING));
    }
    if (url.includes("/dividends/plans")) return Promise.resolve(envelope(plans));
    if (url.includes("/dividends/statements") || url.includes("/dividends/reserve-releases")) {
      return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
    }
    if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const navigateTo = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
  };
  render(<FinanceDividendsPage {...props} />);
  return { navigateTo };
}

describe("FinanceDividendsPage 分红方案版本（234-R1）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    plans = [IN_FORCE, SUPERSEDED];
    writes.length = 0;
  });

  it("已被取代的版本不能修改；生效起始日期必填；提交新版本带生效起始日期与理由，待审批行可看审批原件、同商户修改禁用", async () => {
    const { navigateTo } = renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-row-divp_now").textContent).toContain("酒吧甲"));
    expect(screen.getByTestId("dividend-plan-row-divp_now").textContent).toContain("生效中");
    expect(screen.getByTestId("dividend-plan-row-divp_old").textContent).toContain("已被新版本取代");
    expect(screen.queryByTestId("dividend-plan-edit-divp_old")).toBeNull();

    fireEvent.click(screen.getByTestId("dividend-plan-edit-divp_now"));
    fireEvent.change(screen.getByTestId("dividend-plan-effective-from"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    expect(screen.getByTestId("dividend-plan-guided-error").textContent).toContain("请填写生效起始日期");
    expect(writes).toHaveLength(0);

    fireEvent.change(screen.getByTestId("dividend-plan-effective-from"), { target: { value: "2026-12-01" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    expect(screen.getByTestId("dividend-plan-guided-error").textContent).toContain("请填写调整理由");
    fireEvent.change(screen.getByTestId("dividend-plan-reason"), { target: { value: "主理人增资" } });
    fireEvent.change(screen.getByTestId("dividend-plan-share"), { target: { value: "70" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-plan-guided-summary").textContent).toContain("2026-12-01"));
    expect(screen.getByTestId("dividend-plan-save").textContent).toContain("提交最高权限审批");
    fireEvent.click(screen.getByTestId("dividend-plan-save"));

    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]).toMatchObject({
      method: "PUT", body: { effective_from: "2026-12-01", reason: "主理人增资", platform_share_bps: 7000 },
    });
    expect(writes[0]?.url).toContain("/api/v1/finance/dividends/plans/divp_now");

    await waitFor(() => expect(screen.getByTestId("dividend-plan-row-divp_new").textContent).toContain("待审批"));
    expect(screen.getByTestId("dividend-plan-notice").textContent).toContain("已提交最高权限审批，批准后自生效起始日起生效");
    expect((screen.getByTestId("dividend-plan-edit-divp_now") as HTMLButtonElement).disabled).toBe(true);
    expect(screen.queryByTestId("dividend-plan-edit-divp_new")).toBeNull();
    fireEvent.click(screen.getByTestId("dividend-plan-review-divp_new"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-009", "review_div_1");
  });
});
