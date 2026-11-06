// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 W 段 199-R1（契约 TicketIssueRequest：必填 idempotency_key、reason，additionalProperties false）：
// 后台出票按契约发固定出票原因 reason，不再发后端不认的 operatorNote（原先每次 400「原因未填写」）。
const ORDERS = [
  { id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" },
  { id: "o2", orderNo: "TO-2", payStatus: "paid", issueStatus: "not_issued" },
];

function renderPage(post: ReturnType<typeof vi.fn>) {
  const api = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes("/ticketing/orders")) return Promise.resolve(ORDERS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
  const perms: EffectivePermissionsState = { status: "ready", value: { grants: ["ticket.ticket.issue"], denies: [], wildcard: false } };
  const props: AdminPageProps = { api, user: { id: "u1", username: "staff", accountType: "staff" }, setActionResult: vi.fn(),
    setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<TicketOrdersPage {...props} />);
}

afterEach(() => vi.restoreAllMocks());

describe("TicketOrdersPage 出票请求体（199-R1）", () => {
  it("选订单、点出票并确认后按契约发 reason，不发 operatorNote", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = vi.fn().mockResolvedValue({});
    renderPage(post);
    // 先等默认选中第一行的被动 effect 落定，再改选。
    await waitFor(() => expect((screen.queryByTestId("ticket-order-select-o1") as HTMLInputElement | null)?.checked).toBe(true));
    await userEvent.click(screen.getByTestId("ticket-order-select-o2"));
    await waitFor(() => expect((screen.getByTestId("ticket-order-select-o2") as HTMLInputElement).checked).toBe(true));

    await userEvent.click(screen.getByTestId("ticket-order-issue"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/orders/o2/issue", { reason: "完整后台验收出票" }));
  });
});
