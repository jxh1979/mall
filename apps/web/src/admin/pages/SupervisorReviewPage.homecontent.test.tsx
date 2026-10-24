// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SupervisorReviewPage } from "./SupervisorReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十三批裁决第 5 条「上级审批后发布：运营编辑后走审批，批准才对客人展示」：审批人在主管复核页审批客人端首页内容前，能读到这次要发布的原文（不是盲批）。
// 后台请求按真实形状（AdminApi 收发蛇形键）。

const PENDING = [
  { decision: { review_id: "review_home", policy_code: "customer_home_content_publish", action_type: "customer.home_content.publish",
      object_type: "customer_home_content", object_id: "prj_1", initiated_by: "usr_editor", approval_level: "one_level_up", required_level: 1,
      review_status: "pending_review" },
    schedule: { review_id: "review_home", beneficiary_id: null, due_at: "2026-11-01T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
  { decision: { review_id: "review_bill", policy_code: "bar_bill_waiver_block", action_type: "bar.bill_waiver", object_type: "bar_bill",
      object_id: "b1", initiated_by: "u-9", approval_level: "one_level_up", required_level: 2, review_status: "pending_review" },
    schedule: { review_id: "review_bill", beneficiary_id: null, due_at: "2026-11-01T02:00:00Z", escalation_count: 0, escalated_at: null, forwarded_to: null } },
];

function json(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(gets: string[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    gets.push(url);
    if (url.includes("supervisor-review-policies")) {
      return Promise.resolve(json({ items: [{ policy_code: "customer_home_content_publish", action_type: "customer.home_content.publish",
        required_level: 1, blocks_by_default: true, required_evidence_types: [], status: "active" }], page: 1, page_size: 50, total: 1 }));
    }
    if (url.includes("supervisor-review/pending")) return Promise.resolve(json({ items: PENDING, page: 1, page_size: 50, total: PENDING.length }));
    if (url.includes("/home-contents/prj_1")) {
      return Promise.resolve(json({ project_id: "prj_1", intro: null, announcement: null, published_by: null, published_at: null,
        draft_intro: "城市娱乐综合体，含酒吧与游乐。", draft_announcement: "本周六晚有乐队演出。", draft_submitted_by: "usr_editor",
        draft_submitted_at: "2026-10-22T10:00:00Z" }));
    }
    return Promise.resolve(json({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  const perms: EffectivePermissionsState = { status: "ready", value: { grants: ["governance.supervisor_review.view", "governance.supervisor_review.approve"], denies: [], wildcard: false } };
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "usr_sup", username: "主管", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<SupervisorReviewPage {...props} />);
}

describe("SupervisorReviewPage 客人端首页内容审批前看原文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("待复核的首页内容可打开待发布原文；其他复核单不出现该入口", async () => {
    const gets: string[] = [];
    renderPage(gets);
    await waitFor(() => expect(screen.queryByTestId("supervisor-review-home-content-review_home")).not.toBeNull(), { timeout: 3000 });
    expect(screen.queryByTestId("supervisor-review-home-content-review_bill")).toBeNull();
    fireEvent.click(screen.getByTestId("supervisor-review-home-content-review_home"));
    await waitFor(() => expect(screen.queryByText("城市娱乐综合体，含酒吧与游乐。")).not.toBeNull(), { timeout: 3000 });
    expect(screen.queryByText("本周六晚有乐队演出。")).not.toBeNull();
    expect(gets.some((url) => url.includes("/api/v1/home-contents/prj_1"))).toBe(true);
  });
});
