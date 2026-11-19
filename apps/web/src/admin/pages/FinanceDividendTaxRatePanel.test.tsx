// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AX 段第 271 条 271-R2（BSM:1776「……安全阈值，全平台统一可配，调整须经财务、税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置
// 上下限」「税率以参数形式配置，不在业务页面硬编码」）：持股分红页的分红代扣个税税率面板——列出全平台统一的税率版本（出厂 20%），提交新税率带税率、上下限、
// 生效起始日期、理由与财务、税务、法务三条线最终审批人；下限大于税率前端拦下不发请求。
const FACTORY = { id: "dtrv_factory", rate_bps: 2000, min_bps: null, max_bps: null, effective_from: null, status: "approved",
  reason: "出厂默认 20%（BSM:1776）", review_id: null };

const writes: { url: string; body: Record<string, unknown> }[] = [];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    if (method === "POST") {
      writes.push({ url, body: init?.body ? JSON.parse(String(init.body)) : {} });
      return Promise.resolve(envelope({ ...FACTORY, id: "dtrv_new", rate_bps: 2500, status: "pending_review" }));
    }
    if (url.includes("/dividends/tax-rates")) return Promise.resolve(envelope([FACTORY]));
    if (url.includes("/dividends/statements") || url.includes("/dividends/reserve-releases")) {
      return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
    }
    return Promise.resolve(envelope([]));
  }));
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
    navigateTo: vi.fn(),
  };
  render(<FinanceDividendsPage {...props} />);
}

function fill(rate: string, min: string, max: string) {
  fireEvent.change(screen.getByTestId("dividend-tax-rate-percent"), { target: { value: rate } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-min"), { target: { value: min } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-max"), { target: { value: max } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-from"), { target: { value: "2026-12-01" } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-reason"), { target: { value: "依据个人所得税法调整" } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-finance_review"), { target: { value: "usr_fin" } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-tax_review"), { target: { value: "usr_tax" } });
  fireEvent.change(screen.getByTestId("dividend-tax-rate-legal_review"), { target: { value: "usr_legal" } });
}

describe("FinanceDividendsPage 分红代扣个税税率（271-R2）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    writes.length = 0;
  });

  it("列出出厂版本 20%；下限大于税率前端拦下；填齐后按蛇形键提交税率、上下限、生效起始日期、理由与三条线最终审批人", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-tax-rate-row-dtrv_factory")?.textContent).toContain("20%"));
    expect(screen.getByTestId("dividend-tax-rate-row-dtrv_factory").textContent).toContain("出厂默认");
    expect(screen.getByTestId("dividend-tax-rate-row-dtrv_factory").textContent).toContain("已生效");

    fill("25", "30", "40");
    fireEvent.click(screen.getByTestId("dividend-tax-rate-submit"));
    await waitFor(() => expect(screen.getByTestId("dividend-tax-rate-error").textContent).toContain("下限 ≤ 税率 ≤ 上限"));
    expect(writes).toHaveLength(0);

    fill("25", "10", "30");
    fireEvent.click(screen.getByTestId("dividend-tax-rate-submit"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.url).toContain("/api/v1/finance/dividends/tax-rates");
    expect(writes[0]?.body).toMatchObject({
      rate_bps: 2500, min_bps: 1000, max_bps: 3000, effective_from: "2026-12-01", reason: "依据个人所得税法调整",
      parallel_lines: [
        { line_code: "finance_review", final_approver_id: "usr_fin" },
        { line_code: "tax_review", final_approver_id: "usr_tax" },
        { line_code: "legal_review", final_approver_id: "usr_legal" },
      ],
    });
  });
});
