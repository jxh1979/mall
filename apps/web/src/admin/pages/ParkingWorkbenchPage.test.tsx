// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneStaffVehiclesPage, ParkingWorkbenchPage } from "./ParkingWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// HP-P01（BSM「停车场」L1434～L1438）：参数按项目、值与上下限同配；人工放行记原因与车牌，免费／减免须指定物业主管审批。
function envelope(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(status === 200 ? { success: true, code: "OK", message: "success", data }
    : { success: false, code: "CONFLICT", detail_code: "PARKING_POLICY_NOT_CONFIGURED", message: "该项目尚未配置停车业务参数" }),
  { status, headers: { "content-type": "application/json" } });
}

function fetchCalls(): unknown[][] {
  return (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
}

function sent(method: string, path: string): Record<string, unknown> | null {
  const call = fetchCalls().find((c) => String(c[0]).includes(path) && (c[1] as { method?: string })?.method === method);
  return call ? (JSON.parse(String((call[1] as { body: string }).body)) as Record<string, unknown>) : null;
}

function props(): AdminPageProps {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("/platform/projects")) return Promise.resolve(envelope([{ id: "proj-1", name: "一号综合体" }]));
    if (url.includes("/parking/policies/") && (init?.method ?? "GET") === "GET") return Promise.resolve(envelope(null, 409));
    if (url.includes("/parking/overview")) return Promise.resolve(envelope({ project_id: "proj-1", occupancy: null, sessions: [] }));
    if (url.includes("/parking/credits")) return Promise.resolve(envelope({ items: CREDITS, page: 1, page_size: 1, total: 1 }));
    if (url.includes("/parking/")) return Promise.resolve(envelope({ items: [], page: 1, page_size: 0, total: 0 }));
    return Promise.resolve(envelope([]));
  }));
  const perms: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
  return { api: new AdminApi("t"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(),
    setGlobalError: vi.fn(), effectivePermissions: perms, navigateTo: vi.fn() };
}

// 后端消费抵免台账形状（蛇形键）：一次已使用的抵免、两笔构成资格的消费与按 3:2 的成本分担（决策台账 N02）。
const CREDITS = [{
  id: "pcr-1", customer_id: "cus-1", business_date: "2026-10-21", threshold_cent: 20000, credit_minutes: 120, eligible_amount_cent: 50000,
  status: "used", offset_hours: 2, hour_price_cent: 1000, cost_cent: 2000,
  components: [
    { source_type: "bar_bill", source_id: "bill-1", merchant_id: "m-a", merchant_name: "商户A", amount_cent: 30000 },
    { source_type: "service_order", source_id: "svc-1", merchant_id: "m-b", merchant_name: "商户B", amount_cent: 20000 },
  ],
  allocations: [
    { merchant_id: "m-a", bearer_entity_id: "be-a", share_cent: 1200, cost_month: "2026-10-01" },
    { merchant_id: "m-b", bearer_entity_id: "be-b", share_cent: 800, cost_month: "2026-10-01" },
  ],
}];

const FIELDS: Array<[string, string]> = [
  ["免费时长（分钟）", "30"], ["超出后每小时费率（元）", "5"], ["每日封顶（元）", "30"], ["离场宽限时限（分钟）", "15"],
  ["消费抵免门槛（元）", "200"], ["抵免时长（分钟）", "120"], ["员工车辆优惠（%，100＝免费）", "50"],
];

describe("ParkingWorkbenchPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("policyTabRequiresBoundsBeforeSubmit：七项值与上下限缺一不能提交，填齐后按分与万分比 PUT 蛇形键", async () => {
    render(<ParkingWorkbenchPage {...props()} />);
    await waitFor(() => expect(screen.queryByTestId("parking-policy-form")).not.toBeNull());
    for (const [label, value] of FIELDS) {
      fireEvent.change(screen.getByLabelText(`${label}`), { target: { value } });
      fireEvent.change(screen.getByLabelText(`${label}下限`), { target: { value: "0" } });
      if (label !== "每日封顶（元）") fireEvent.change(screen.getByLabelText(`${label}上限`), { target: { value: "1000" } });
    }
    fireEvent.click(screen.getByTestId("parking-policy-save"));
    await waitFor(() => expect(screen.getByTestId("parking-policy-error").textContent).toContain("须同时填写值及上下限"));
    expect(sent("PUT", "/parking/policies/proj-1")).toBeNull();

    fireEvent.change(screen.getByLabelText("每日封顶（元）上限"), { target: { value: "100" } });
    fireEvent.change(screen.getByLabelText("员工车辆优惠（%，100＝免费）上限"), { target: { value: "100" } });
    fireEvent.click(screen.getByTestId("parking-policy-save"));
    await waitFor(() => expect(sent("PUT", "/parking/policies/proj-1")).not.toBeNull());
    expect(sent("PUT", "/parking/policies/proj-1")).toMatchObject({
      free_minutes: 30, free_minutes_min: 0, free_minutes_max: 1000,
      hourly_rate_cent: 500, daily_cap_cent: 3000, daily_cap_cent_max: 10000,
      credit_threshold_cent: 20000, staff_discount_bp: 5000, staff_discount_bp_max: 10000,
    });
  });

  it("人工放行：申请减免须填金额与物业主管，POST 带原因、车牌与最终审批人", async () => {
    render(<ParkingWorkbenchPage {...props()} />);
    await waitFor(() => expect(screen.queryByTestId("parking-policy-form")).not.toBeNull());
    fireEvent.click(screen.getByRole("tab", { name: "人工放行" }));
    await waitFor(() => expect(screen.queryByTestId("parking-release-form")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("车牌"), { target: { value: "粤A12345" } });
    fireEvent.change(screen.getByLabelText("放行原因"), { target: { value: "fee_dispute" } });
    fireEvent.change(screen.getByLabelText("核对说明"), { target: { value: "客人称已缴" } });
    fireEvent.change(screen.getByLabelText("免费或减免"), { target: { value: "reduction" } });
    fireEvent.change(screen.getByLabelText("减免金额（元）"), { target: { value: "5" } });
    fireEvent.change(screen.getByLabelText("审批的物业主管账号"), { target: { value: "boss-1" } });
    fireEvent.click(screen.getByTestId("parking-release-save"));
    await waitFor(() => expect(sent("POST", "/parking/manual-releases")).not.toBeNull());
    expect(sent("POST", "/parking/manual-releases")).toMatchObject({ project_id: "proj-1", plate_no: "粤A12345",
      release_reason: "fee_dispute", reason: "客人称已缴", waive_type: "reduction", reduction_cent: 500, final_approver_id: "boss-1" });
  });

  // HP-P01（BSM「停车场」章「消费抵免」条）：后台列出抵免、构成资格的消费及其归属主体、实际抵掉小时与按组成比例的成本分担。
  it("消费抵免台账：列出抵免状态、构成消费与成本分担", async () => {
    render(<ParkingWorkbenchPage {...props()} />);
    await waitFor(() => expect(screen.queryByTestId("parking-policy-form")).not.toBeNull());
    await waitFor(() => expect(screen.queryByRole("tab", { name: "消费抵免台账" })).not.toBeNull());
    fireEvent.click(screen.getByRole("tab", { name: "消费抵免台账" }));
    await waitFor(() => expect(screen.queryByTestId("parking-credit-pcr-1")).not.toBeNull());
    const row = screen.getByTestId("parking-credit-pcr-1").textContent ?? "";
    expect(row).toContain("已使用");
    expect(row).toContain("¥500.00");
    expect(row).toContain("2 小时");
    expect(row).toContain("¥20.00");
    const detail = screen.getByTestId("parking-credit-detail-pcr-1").textContent ?? "";
    expect(detail).toContain("酒吧账单");
    expect(detail).toContain("商户A ¥300.00");
    expect(detail).toContain("商户A 承担 ¥12.00");
    expect(detail).toContain("商户B 承担 ¥8.00");
  });

  it("商户专区员工车辆：登记本商户员工车牌，POST 带项目、员工账号与车牌", async () => {
    const p = props();
    render(<MerchantZoneStaffVehiclesPage {...p} user={{ id: "m-admin", username: "m", accountType: "merchant_admin", organizationId: "proj-1", merchantId: "m-1" }} />);
    await waitFor(() => expect(screen.queryByTestId("parking-staff-vehicle-form")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("员工账号"), { target: { value: "staff-9" } });
    fireEvent.change(screen.getByLabelText("员工车牌"), { target: { value: "粤B99999" } });
    fireEvent.click(screen.getByTestId("parking-staff-vehicle-save"));
    await waitFor(() => expect(sent("POST", "/parking/staff-vehicles")).not.toBeNull());
    expect(sent("POST", "/parking/staff-vehicles")).toMatchObject({ project_id: "proj-1", user_id: "staff-9", plate_no: "粤B99999" });
  });
});
