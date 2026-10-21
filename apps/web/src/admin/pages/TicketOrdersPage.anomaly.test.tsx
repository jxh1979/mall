// 文件版本：V0.01
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// TK-14（ENUM「核销记录状态」「异常状态的统一出口」）：核销记录表里成功行可提起异常复核；待处置异常只给有处置权限的人两个出口，原因必填；
// 拒绝、失败行不能提起；已处置行显示处置结果、处置人和时间。
const VERIFICATIONS = [
  { id: "v-ok", verificationNo: "V-OK", status: "success", createdAt: "2026-10-20T02:00:00Z" },
  { id: "v-rej", verificationNo: "V-REJ", status: "rejected", resultReason: "entitlement_used_up" },
  { id: "v-fail", verificationNo: "V-FAIL", status: "failed", resultReason: "window_expired" },
  { id: "v-disputed", verificationNo: "V-DIS", status: "success" },
  { id: "a-open", verificationNo: "A-OPEN", status: "anomaly", resultReason: "现场未放行", anomalyOfVerificationId: "v-disputed" },
  {
    id: "a-done", verificationNo: "A-DONE", status: "anomaly", resultReason: "交付争议", anomalyOfVerificationId: "v-ok-old",
    anomalyResolution: "voided_reversed", anomalyResolvedBy: "usr_supervisor", anomalyResolvedAt: "2026-10-20T03:00:00Z",
    anomalyResolutionReason: "核实未放行",
  },
];

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function renderPage(grants: string[], post = vi.fn().mockResolvedValue({})) {
  const api = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes("/ticketing/orders")) return Promise.resolve([{ id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" }]);
      if (url.includes("/ticketing/verifications")) return Promise.resolve(VERIFICATIONS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "staff", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: perms(grants),
  };
  render(<TicketOrdersPage {...props} />);
  return post;
}

afterEach(() => vi.restoreAllMocks());

describe("TicketOrdersPage 核销异常复核（TK-14）", () => {
  it("成功行可提起异常复核，原因必填；拒绝、失败行没有提起入口", async () => {
    const post = renderPage(["verification.view", "verification.execute"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-verification-row-v-ok")).not.toBeNull());

    expect(within(screen.getByTestId("ticket-verification-row-v-rej")).queryByTestId("ticket-verification-raise-anomaly-v-rej")).toBeNull();
    expect(within(screen.getByTestId("ticket-verification-row-v-fail")).queryByTestId("ticket-verification-raise-anomaly-v-fail")).toBeNull();
    // 已有待处置异常的成功行不再给提起入口
    expect(screen.queryByTestId("ticket-verification-raise-anomaly-v-disputed")).toBeNull();

    await userEvent.click(screen.getByTestId("ticket-verification-raise-anomaly-v-ok"));
    await waitFor(() => expect(screen.getByTestId("ticket-orders-error").textContent).toContain("原因"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("ticket-verification-anomaly-reason"), "闸机未放行");
    await userEvent.click(screen.getByTestId("ticket-verification-raise-anomaly-v-ok"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/verifications/v-ok/anomaly", { reason: "闸机未放行" }));
  });

  it("待处置异常只给有处置权限的人两个出口，原因为空不能提交，作废并冲正带 resolution 提交", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = renderPage(["verification.view", "ticket.verify.anomaly.resolve"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-verification-row-a-open")).not.toBeNull());
    expect(screen.getByTestId("ticket-verification-row-a-open").textContent).toContain("待主管处置");

    await userEvent.click(screen.getByTestId("ticket-verification-resolve-voided_reversed-a-open"));
    await waitFor(() => expect(screen.getByTestId("ticket-orders-error").textContent).toContain("原因"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("ticket-verification-anomaly-reason"), "核实未放行");
    await userEvent.click(screen.getByTestId("ticket-verification-resolve-voided_reversed-a-open"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/ticketing/verification-anomalies/a-open/resolve", { resolution: "voided_reversed", reason: "核实未放行" }));
    expect(screen.getByTestId("ticket-verification-resolve-restored-a-open")).toBeTruthy();
  });

  it("没有处置权限只看到待主管处置，没有处置按钮；已处置行显示结果、处置人和时间", async () => {
    renderPage(["verification.view", "verification.execute"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-verification-row-a-open")).not.toBeNull());
    expect(screen.getByTestId("ticket-verification-row-a-open").textContent).toContain("待主管处置");
    expect(screen.queryByTestId("ticket-verification-resolve-restored-a-open")).toBeNull();
    expect(screen.queryByTestId("ticket-verification-resolve-voided_reversed-a-open")).toBeNull();
    const done = screen.getByTestId("ticket-verification-row-a-done").textContent ?? "";
    expect(done).toContain("作废并冲正");
    expect(done).toContain("usr_supervisor");
    expect(done).toContain("核实未放行");
    expect(done).toContain("2026");
  });
});
