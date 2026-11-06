// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PAYROLL = [{ id: "pay_1", user_id: "u-9", base_cent: 800000, commission_cent: 5000, total_cent: 805000, payer_company_id: "ent-platform", tax_assessment_id: "tax_assessment_locked", tax_version_id: "tax_version_locked" }];
const SETTLEMENTS = [{ id: "bcs_1", month: "2026-09", merchant_id: "mch-bar", company_id: "ent-platform", amount_cent: 5000, calculation_count: 2, staff_count: 1, ar_ap_item_id: "arap_dct_1", due_date: "2026-10-05" }];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("StaffPayrollLeavePage 提成月结拨付（CG-07，BSM「营销人员的归属与报酬」「派驻人员的成本与报酬」）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("工资单显示发放主体；拨付记录按月列出；点击拨付按当月调后端", async () => {
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/staff/commission-settlements/close")) return Promise.resolve(dataEnvelope({ month: "2026-09", settlements: 1 }));
      if (url.includes("/staff/commission-settlements")) return Promise.resolve(listEnvelope(SETTLEMENTS));
      if (url.includes("/staff/payroll/statements/pay_1")) return Promise.resolve(dataEnvelope({ statement: PAYROLL[0], status: "closed", statement_kind: "monthly", paid_amount_cent: 0, lines: [], worktime: null, corrections: [], payments: [], pending_items: [] }));
      if (url.includes("/staff/payroll?")) return Promise.resolve(listEnvelope(PAYROLL));
      return Promise.resolve(listEnvelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const setActionResult = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);

    await waitFor(() => expect(screen.getByTestId("payroll-payer-pay_1").textContent).toBe("ent-platform"));
    const settlement = await screen.findByTestId("commission-settlement-row-bcs_1");
    expect(settlement.textContent).toContain("mch-bar");
    expect(settlement.textContent).toContain("ent-platform");
    expect(settlement.textContent).toContain("2026年10月05日");
    expect(settlement.textContent).toContain("arap_dct_1");

    fireEvent.click(screen.getByTestId("payroll-commission-close"));
    await waitFor(() => expect(fetchMock.mock.calls.some((call) => String(call[0]).includes("/api/v1/staff/commission-settlements/close"))).toBe(true));
    const closeCall = fetchMock.mock.calls.find((call) => String(call[0]).includes("/api/v1/staff/commission-settlements/close"));
    const body = JSON.parse(String((closeCall?.[1] as RequestInit | undefined)?.body ?? "{}")) as { month?: string };
    expect(body.month).toMatch(/^\d{4}-\d{2}$/);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "commission_settlement_closed", settlements: 1 })));
    fireEvent.click(screen.getByRole("button", { name: "查看薪资单详情" }));
    expect(await screen.findByText("tax_assessment_locked")).toBeTruthy();
    expect(screen.getByText("tax_version_locked")).toBeTruthy();
  });

  // 第十八批 V 段 159-R8（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；PGM:291 报错提示中文）：
  // 后端以 409 COMMISSION_DATA_NOT_FINAL 拒绝拨付时，页面经全局报错显示「操作冲突：」加后端原文。修复前按钮 void 调用无 catch，什么都不显示。
  it("拨付被拒（营业日数据未标最终）时显示后端中文原文", async () => {
    const message = "以下营业日数据尚未标记为最终，不能办理提成月结拨付：2026-09-20 提成酒吧：当日退款与冲正记录";
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/staff/commission-settlements/close")) return Promise.resolve(new Response(JSON.stringify({ success: false, code: "STATE_CONFLICT", detail_code: "COMMISSION_DATA_NOT_FINAL", message }), { status: 409, headers: { "content-type": "application/json" } }));
      if (url.includes("/staff/commission-settlements")) return Promise.resolve(listEnvelope(SETTLEMENTS));
      if (url.includes("/staff/payroll?")) return Promise.resolve(listEnvelope(PAYROLL));
      return Promise.resolve(listEnvelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const setGlobalError = vi.fn();
    const setActionResult = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);

    await waitFor(() => expect(screen.getByTestId("payroll-payer-pay_1").textContent).toBe("ent-platform"));
    await waitFor(() => expect(screen.getByTestId("commission-settlement-row-bcs_1").textContent).toContain("mch-bar"));
    fireEvent.click(screen.getByTestId("payroll-commission-close"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("操作冲突：" + message));
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "commission_settlement_closed" }));
  });
});
