// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SupervisorReviewPage } from "./SupervisorReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const POLICIES = [
  { id: "sp-1", policy_code: "POL-1", action_type: "contract_violation", required_level: 3, blocks_by_default: true, required_evidence_types: ["contract"], status: "active" },
];
const PENDING = [
  { decision: { review_id: "review_p1", policy_code: "contract_violation_l3", action_type: "contract_violation", object_type: "contract", object_id: "c1", initiated_by: "u-9", approval_level: "two_levels_up", required_level: 3, review_status: "pending_review" },
    schedule: { review_id: "review_p1", beneficiary_id: null, due_at: "2026-09-07T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
];

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
    if (url.includes("supervisor-review/evaluate")) return Promise.resolve(raw({ review_id: "review_1", blocked: true, review_status: "pending_review", required_level: 3 }));
    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope(PENDING));
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

// M087-A-02（PGM-1.7.1「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全……」）
describe("SupervisorReviewPage 指定最终审批人与审批链入口", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/supervisor-review");
  });

  it.each(["usr_gm", ""])("评估时最终审批人 %s：填写则带字段，不填则不带", async (approver) => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("supervisor-review-violation-summary"), { target: { value: "合同主体不一致" } });
    fireEvent.change(screen.getByTestId("supervisor-review-final-approver"), { target: { value: approver } });
    fireEvent.click(screen.getByTestId("supervisor-review-evaluate"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/supervisor-review/evaluate"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/supervisor-review/evaluate"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    if (approver) expect(body.final_approver_id).toBe(approver);
    else expect(body).not.toHaveProperty("final_approver_id");
  });

  it.each(["", "/complex"])("部署前缀 %s：待复核清单审批链链接保留前缀与 review_id", async (prefix) => {
    window.history.replaceState({}, "", `${prefix}/admin/governance/supervisor-review`);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-pending-review_p1")).toBeTruthy());
    const link = screen.getByTestId("supervisor-review-chain-link-review_p1") as HTMLAnchorElement;
    expect(link.getAttribute("href")).toBe(`${prefix}/admin/governance/approval-chain?review_id=review_p1`);
    expect(link.textContent).toContain("审批链");
  });
});
