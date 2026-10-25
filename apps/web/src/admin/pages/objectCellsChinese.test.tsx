// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";
import { AdmissionAuditsPage } from "./AdmissionAuditsPage";
import { AmusementGroupBookingsPage } from "./AmusementGroupBookingsPage";
import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AmusementVerificationRecordsPage } from "./AmusementVerificationRecordsPage";
import { AuditEventsPage } from "./AuditEventsPage";
import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
import { OrganizationMerchantPage } from "./OrganizationMerchantPage";

// OBS-14-02（PGM「全局原则 · 界面全中文（所有终端零英文）」）：页面单元格与详情里的对象值不再 JSON 上屏，复用结果区的中文展示。
const OBJ = { name: "张三", note: "补录" };
const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function stubFetch(routes: [string, unknown[]][]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const items = routes.find(([part]) => String(input).includes(part))?.[1] ?? [];
    const body = { success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 200, total: items.length } };
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } }));
  }));
}

function props(api: AdminApiClient): AdminPageProps {
  return { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMS };
}

const CASES: [string, React.ComponentType<AdminPageProps>, [string, unknown[]][], string][] = [
  ["团体/包场·场次", AmusementGroupBookingsPage, [["/amusement/group-bookings", [{ id: "b-1", project_id: "amuse-1", session_id: OBJ, booking_type: "reserved", customer_name: "团体甲", party_size: 20, reserved_capacity: 20, booking_status: "approved" }]]], "amusement-group-booking-row-b-1"],
  ["治理拓扑·楼层", GovernanceTopologyPage, [["spaces", [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: OBJ, area_square_meter: 120, space_type: "shop", status: "active" }]], ["projects", [{ id: "p-1", name: "项目一", code: "PJ-1", status: "active" }]]], "governance-space-row-s-1"],
  ["组织商户·联系人", OrganizationMerchantPage, [["merchants", [{ id: "m-1", project_id: "p-1", merchant_name: "商户甲", contact_name: OBJ, status: "active" }]]], "organization-merchant-row-m-1"],
  ["游乐场次·项目容量", AmusementSessionsPage, [["/amusement/projects", [{ id: "amuse-1", project_code: "AM-1", project_name: "旋转木马", capacity: OBJ, status: "active" }]]], "amusement-project-row-amuse-1"],
  ["准入审计·联系人", AdmissionAuditsPage, [["business-entity-bindings", []], ["merchants", [{ id: "m-1", merchant_name: "商户甲", contact_name: OBJ, status: "active" }]]], "admission-audits-merchant-row-m-1"],
  ["游乐核销记录·核销人数", AmusementVerificationRecordsPage, [["/amusement/verifications", [{ id: "v-1", project_id: "amuse-1", session_id: "s-1", direction: "entry", verified_people: OBJ }]]], "amu-verif-row-v-1"],
];

describe("页面单元格与详情的对象值中文展示", () => {
  afterEach(() => vi.unstubAllGlobals());

  it.each(CASES)("%s：对象值按中文字段名展示，不出 JSON", async (_name, Page, routes, testid) => {
    stubFetch(routes);
    render(<Page {...props(new AdminApi("test-token"))} />);
    await waitFor(() => expect(screen.getByTestId(testid).textContent).toContain("名称：张三"));
    const text = screen.getByTestId(testid).textContent ?? "";
    expect(text).toContain("备注：补录");
    expect(text).not.toContain('"name"');
  });

  it("审计事件「查看详情」：变更前后与事件按中文字段名展示，不出 JSON", async () => {
    const row = { id: "e1", eventType: "login", result: "success", traceId: "t-1", summary: "登录成功", beforeAfter: { before: { name: "张三" }, after: { name: "李四" } } };
    const api: AdminApiClient = {
      get: vi.fn().mockResolvedValue([]),
      getPaged: vi.fn().mockResolvedValue({ items: [row], total: 1, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    };
    render(<AuditEventsPage {...props(api)} />);
    await waitFor(() => expect((screen.getByTestId("audit-events-show-detail") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("audit-events-show-detail"));
    const text = screen.getByTestId("audit-events-detail").textContent ?? "";
    expect(text).toContain("变更前后：\n  变更前：\n    名称：张三\n  变更后：\n    名称：李四");
    expect(text).toContain("追踪号：t-1");
    expect(text).not.toContain('"beforeAfter"');
  });
});
