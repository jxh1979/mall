// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AW 段 295-R2（BSM:785「要让某一个票种可以退款，必须逐个票种单独开启，每次开启都须经高敏权限审批并留痕，不得整批打开」）：
// 票种列表里不可退的票种逐个点「申请开启可退」，填理由与最终审批人提交审批。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(postSpy: ReturnType<typeof vi.fn>) {
  const api = {
    get: vi.fn(async (path: string) => {
      if (path.includes("/ticketing/ticket-types")) {
        return { items: [
          { id: "tt-no", ticketName: "不可退日票", weekdayPriceCent: 9900, weekendPriceCent: 9900, holidayPriceCent: 9900,
            status: "published", stockQuantity: null, refundable: false, barId: null },
          { id: "tt-yes", ticketName: "可退日票", weekdayPriceCent: 9900, weekendPriceCent: 9900, holidayPriceCent: 9900,
            status: "published", stockQuantity: null, refundable: true, barId: null },
        ] } as unknown;
      }
      if (path.includes("/qr-refresh-config")) return { refreshIntervalSeconds: 15 } as unknown;
      return { items: [] } as unknown;
    }),
    post: postSpy,
    put: vi.fn(async () => ({} as unknown)),
  } as unknown as AdminApiClient;
  const props = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
  return props;
}

describe("TicketRefundableRequest 票种申请开启可退", () => {
  afterEach(() => { vi.clearAllMocks(); });

  it("不可退票种点申请开启可退→填理由与审批人→调用 refundable-enable-requests，可退票种不出按钮", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({ ticketTypeId: "tt-no", reviewId: "rv-1",
      reviewStatus: "pending_review", refundable: false } as unknown));
    const props = renderPage(postSpy);

    await waitFor(() => expect(screen.queryByTestId("ticket-refundable-request-tt-no")).toBeTruthy());
    expect(screen.queryByTestId("ticket-refundable-request-tt-yes")).toBeNull();

    fireEvent.click(screen.getByTestId("ticket-refundable-request-tt-no"));
    await waitFor(() => expect(screen.queryByTestId("ticket-refundable-request-reason")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-refundable-request-submit"));
    expect(screen.getByTestId("ticket-refundable-request-error").textContent).toContain("理由");
    expect(postSpy).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("ticket-refundable-request-reason"), { target: { value: "合同约定该票可退" } });
    fireEvent.change(screen.getByTestId("ticket-refundable-request-approver"), { target: { value: "usr_boss" } });
    fireEvent.click(screen.getByTestId("ticket-refundable-request-submit"));

    await waitFor(() => expect(postSpy).toHaveBeenCalled());
    const [path, body] = postSpy.mock.calls[0] as [string, Record<string, unknown>];
    expect(path).toBe("/api/v1/ticketing/ticket-types/tt-no/refundable-enable-requests");
    expect(body.reason).toBe("合同约定该票可退");
    expect(body.final_approver_id).toBe("usr_boss");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "ticket_type_refundable_enable_submitted", note: expect.stringContaining("批准后该票种才可退") })));
  });
});
