// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SupervisorReviewPage } from "./SupervisorReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 F 段第 14 条 14-R8（BSM 第 1642 行「补录一律进入主管复核队列」「补录时限……须同时配置上下限」「后台配置的时长……须同时配置上下限」；
// BSM 第 1911 行「审批异常和补录」）：待复核清单补录三类动作显示中文名；人工补录参数块两组分开提交、组内值与上下限同填。
const POLICIES = [
  { id: "sp-1", policy_code: "manual_backfill_bar_sales", action_type: "bar.sales.backfill", required_level: 0, blocks_by_default: true, required_evidence_types: ["reason"], status: "active" },
];

function pending(reviewId: string, policyCode: string, actionType: string, approvalLevel: string) {
  return {
    decision: { review_id: reviewId, policy_code: policyCode, action_type: actionType, object_type: "manual_backfill", object_id: `mb-${reviewId}`, initiated_by: "u-9", approval_level: approvalLevel, required_level: 0, review_status: "pending_review" },
    schedule: { review_id: reviewId, beneficiary_id: "u-8", due_at: "2026-10-30T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null },
  };
}

const PENDING = [
  pending("review_b1", "manual_backfill_bar_sales", "bar.sales.backfill", "specific_role"),
  pending("review_b2", "manual_backfill_trace_scan", "inventory.trace.scan_backfill", "one_level_up"),
  pending("review_b3", "manual_backfill_bar_service", "bar.service.backfill", "one_level_up"),
];

const POLICY = { project_id: "__global__", window_days: 3, window_min_days: null, window_max_days: null, recording_lead_minutes: 30, lead_min_minutes: null, lead_max_minutes: null, updated_by: null, updated_at: "2026-10-29T00:00:00Z" };

function envelope(items: unknown[]): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }), { status: 200, headers: { "content-type": "application/json" } });
}
function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("supervisor-review-policies")) return Promise.resolve(envelope(POLICIES));
    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope(PENDING));
    if (url.includes("manual-backfill-policies")) return Promise.resolve(raw(POLICY));
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
  };
  render(<SupervisorReviewPage {...props} />);
  return props;
}

function policyPuts() {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) =>
    String(input).includes("manual-backfill-policies") && (init as RequestInit | undefined)?.method === "PUT");
}

function fill(testId: string, value: string) {
  fireEvent.change(screen.getByTestId(testId), { target: { value } });
}

describe("SupervisorReviewPage 第十八批 F 段 14-R8：人工补录", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("待复核清单：三类补录动作与补录单显示中文名，不显示未识别类型", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("supervisor-review-pending-review_b1")).not.toBeNull());
    const sales = screen.getByTestId("supervisor-review-pending-review_b1").textContent ?? "";
    expect(sales).toContain("销售归属补录");
    expect(sales).toContain("人工补录单");
    expect(sales).not.toContain("未识别类型");
    expect(screen.getByTestId("supervisor-review-pending-review_b2").textContent).toContain("交接扫码补录");
    expect(screen.getByTestId("supervisor-review-pending-review_b3").textContent).toContain("服务责任补录");
  });

  it("参数块：补录时限组只填天数不填上下限，报错且不发请求", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.queryByTestId("manual-backfill-policy")).not.toBeNull());
    expect(screen.getByTestId("manual-backfill-policy").textContent).toContain("安全阈值，调整须最高权限本人办理");
    fill("manual-backfill-policy-project", "proj-a");
    fill("manual-backfill-window-days", "5");
    fireEvent.click(screen.getByTestId("manual-backfill-window-save"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("补录时限须同时填写")));
    expect(policyPuts()).toHaveLength(0);
  });

  it("参数块：只改录音提前量时，请求体只带录音提前量一组（不带补录时限组）", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.queryByTestId("manual-backfill-policy")).not.toBeNull());
    fill("manual-backfill-policy-project", "proj-a");
    fireEvent.click(screen.getByTestId("manual-backfill-policy-query"));
    await waitFor(() => expect(screen.getByTestId("manual-backfill-policy-current").textContent).toContain("录音提前 30 分钟"));
    fill("manual-backfill-lead-minutes", "45");
    fill("manual-backfill-lead-min", "10");
    fill("manual-backfill-lead-max", "60");
    fireEvent.click(screen.getByTestId("manual-backfill-lead-save"));
    await waitFor(() => expect(policyPuts()).toHaveLength(1));
    const call = policyPuts()[0];
    expect(String(call?.[0])).toContain("/api/v1/evidence/manual-backfill-policies/proj-a");
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body).toMatchObject({ recording_lead_minutes: 45, lead_min_minutes: 10, lead_max_minutes: 60 });
    expect(body.idempotency_key).toBeTruthy();
    for (const key of ["window_days", "window_min_days", "window_max_days"]) expect(body).not.toHaveProperty(key);
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringContaining("录音提前 45 分钟")));
  });
});
