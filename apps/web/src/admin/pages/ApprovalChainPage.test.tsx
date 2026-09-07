import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ApprovalChainPage } from "./ApprovalChainPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function envelope(items: unknown[]): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }), { status: 200, headers: { "content-type": "application/json" } });
}

const PENDING = [
  { decision: { review_id: "review_c1", policy_code: "contract_violation_l3", action_type: "contract_violation", object_type: "contract", object_id: "c1", initiated_by: "usr_l1", approval_level: "two_levels_up", required_level: 3, review_status: "pending_review" },
    schedule: { review_id: "review_c1", beneficiary_id: null, due_at: "2026-09-09T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
  { decision: { review_id: "review_orphan", policy_code: "permission_grant_l2", action_type: "access.grant_request", object_type: "access_grant_request", object_id: "agr_1", initiated_by: "usr_orphan", approval_level: "two_levels_up", required_level: 2, review_status: "pending_review" },
    schedule: null },
];
const NODES = [
  { node_id: "n1", review_id: "review_c1", line_code: "main", seq: 1, approver_kind: "user", approver_id: "usr_a", status: "approved", decided_by: "usr_a", decided_at: "2026-09-06T03:00:00Z", forwarded_to: null, forwarded_by: null, escalation_count: 0 },
  { node_id: "n2", review_id: "review_c1", line_code: "main", seq: 2, approver_kind: "user", approver_id: "usr_c", status: "pending", decided_by: null, decided_at: null, forwarded_to: null, forwarded_by: null, escalation_count: 1 },
  { node_id: "n3", review_id: "review_c1", line_code: "finance_review", seq: 1, approver_kind: "user", approver_id: "usr_fin", status: "pending", decided_by: null, decided_at: null, forwarded_to: "usr_x", forwarded_by: "usr_fin", escalation_count: 0 },
];

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("supervisor-review/pending")) return Promise.resolve(envelope(PENDING));
    if (url.includes("/review_c1/chain/regenerate")) return Promise.resolve(raw(NODES));
    if (url.includes("/review_orphan/chain/regenerate")) return Promise.resolve(raw([NODES[1]]));
    if (url.includes("/review_c1/chain")) return Promise.resolve(raw(NODES));
    if (url.includes("/review_orphan/chain")) return Promise.resolve(raw([]));
    if (url.includes("/approve")) return Promise.resolve(raw({ review_id: "review_c1", review_status: "pending_review", decision: "blocked" }));
    if (url.includes("/forward")) return Promise.resolve(raw({ review_id: "review_c1", forwarded_to: "usr_y" }));
    return Promise.resolve(raw([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS, focusObjectId?: string) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
    focusObjectId,
  };
  render(<ApprovalChainPage {...props} />);
  return props;
}

// M087-A-02（PGM-1.7.1 逐级审批、任一节点驳回整单退回、平行线全部通过才生效）
describe("ApprovalChainPage 审批链与节点裁决", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/approval-chain");
  });

  it("选单后展示主线与平行线节点、当前节点标记、转交与升级信息", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("approval-chain-pending-review_c1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("approval-chain-select-review_c1"));
    await waitFor(() => expect(screen.getByTestId("approval-chain-nodes")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-node-main-1").textContent).toContain("已通过");
    expect(screen.getByTestId("approval-chain-node-main-2").textContent).toContain("待审（当前）");
    expect(screen.getByTestId("approval-chain-node-main-2").textContent).toContain("直接上级");
    expect(screen.getByTestId("approval-chain-node-finance_review-1").textContent).toContain("usr_x");
    expect(screen.getByTestId("approval-chain-node-main-2").textContent).not.toMatch(/pending|approved|user/);
  });

  it("通过当前节点 → 提交 decision=approved 并给出中文结果；驳回按钮受 approve 权限门控", async () => {
    const props = renderPage(ADMIN_PERMS, "review_c1");
    await waitFor(() => expect(screen.getByTestId("approval-chain-nodes")).toBeTruthy());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("approval-chain-approve"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/review_c1/approve"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/review_c1/approve"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.decision).toBe("approved");
    expect(body.comment).toBe("证据齐备，同意");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("复核单 review_c1 当前节点已通过，等待后续节点审批"));

    vi.unstubAllGlobals();
    renderPage(VIEW_ONLY, "review_c1");
    await waitFor(() => expect(screen.getAllByTestId("approval-chain-reject").length).toBeGreaterThan(0));
    const rejects = screen.getAllByTestId("approval-chain-reject") as HTMLButtonElement[];
    expect(rejects[rejects.length - 1]?.disabled).toBe(true);
  });

  it("链未生成的单据提示先维护直接上级，并可重生成链（manage 门禁）", async () => {
    const props = renderPage(ADMIN_PERMS, "review_orphan");
    await waitFor(() => expect(screen.getByTestId("approval-chain-unresolved")).toBeTruthy());
    expect(screen.getByTestId("approval-chain-unresolved").textContent).toContain("直接上级未维护");
    fireEvent.click(screen.getByTestId("approval-chain-regenerate"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/review_orphan/chain/regenerate"))).toBe(true));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("复核单 review_orphan 审批链已重生成，共 1 个节点"));
  });

  it("转交当前节点提交 forwarded_to", async () => {
    const props = renderPage(ADMIN_PERMS, "review_c1");
    await waitFor(() => expect(screen.getByTestId("approval-chain-nodes")).toBeTruthy());
    fireEvent.change(screen.getByTestId("approval-chain-forward-target"), { target: { value: "usr_y" } });
    fireEvent.click(screen.getByTestId("approval-chain-forward"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/review_c1/forward"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/review_c1/forward"));
    expect(JSON.parse(String((call?.[1] as RequestInit | undefined)?.body)).forwarded_to).toBe("usr_y");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith("复核单 review_c1 当前节点已转交给 usr_y"));
  });
});
