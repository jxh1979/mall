// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";

vi.mock("./pages/BarPayrollPage", () => ({ BarPayrollPage: () => <section data-testid="bar-payroll-page">酒吧小费与服务费</section> }));
vi.mock("./pages/PayrollTaxBracketsPage", () => ({ PayrollTaxBracketsPage: () => <section data-testid="payroll-tax-brackets-page">税表与专业认定</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

function mount(grants: string[], wildcard: boolean, denies: string[] = [], route = "/admin/bar/service-payroll") {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const data = String(input).endsWith("/api/v1/access/effective-permissions")
      ? { grants, denies, wildcard } : [];
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
  }));
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "finance-1", username: "finance", accountType: "platform_admin" }, sessionId: "session-1" }));
  window.history.replaceState({}, "", route);
  render(<AdminApp />);
}

describe("税务原件读权限入口", () => {
  it.each(["staff.payroll.manage", "staff.tax_bracket.manage"])("仅有 %s 仍能按独立范围进入税档", async permission => {
    mount([permission], false, [], "/admin/staff/tax-brackets");
    expect(await screen.findByTestId("payroll-tax-brackets-page")).toBeTruthy();
  });
});

describe("酒吧工资敏感入口", () => {
  it("真实查看授权可从直链进入专属页面", async () => {
    mount(["bar.payroll.view"], false);
    expect(await screen.findByTestId("bar-payroll-page")).toBeTruthy();
    expect(screen.getByTestId("admin-nav-ADM-BAR-007")).toBeTruthy();
  });
  it.each([
    { grants: [], wildcard: true, denies: [] },
    { grants: ["bar.payroll.manage"], wildcard: false, denies: [] },
    { grants: ["bar.payroll.view"], wildcard: true, denies: ["bar.payroll.view"] },
  ])("无逐码查看权或显式拒绝不能打开敏感金额页", async ({ grants, wildcard, denies }) => {
    mount(grants, wildcard, denies);
    await waitFor(() => expect(screen.getByTestId("admin-permission-denied")).toBeTruthy());
    expect(screen.queryByTestId("bar-payroll-page")).toBeNull();
  });
});
