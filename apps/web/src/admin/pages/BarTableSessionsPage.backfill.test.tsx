// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps, BarTableSessionRow } from "../types";

// 第十八批 F 段第 14 条 14-R7：后台酒吧页发起销售归属补录（14-R3）与服务责任区间补录（14-R6），补录行与原记录并列、标「人工补录」。
// BSM:1642「补录记录与原始记录并列展示，标明“人工补录”」「以通话录音为凭证的，录音起始时间必须早于客人到店时间、且早于客人开卡时间达到后台配置的时长」；
// BSM:1021「来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」；BSM:1613「卡座或桌台由谁主责服务……是否中途换人」。
const SESSION: BarTableSessionRow = { id: "s-1", tableId: "t-1", barId: "bar-1", status: "open", guestSource: "walk_in", sessionVersion: 2, openedAt: "2026-10-28T12:00:00Z" };
const BACKFILLS = "/api/v1/evidence/manual-backfills";
const salesRow = (status: string) => ({ id: "mb-1", targetType: "bar_session_sales", targetId: "s-1", proposed: { salesStaffId: "u-sales" },
  originalSnapshot: { guestSource: "walk_in", sessionSales: null, reservationId: null, reservationSales: null, orders: [], bills: [] }, reason: "前台漏登记",
  responsibleStaffId: "u-desk", evidenceAttachmentIds: ["att-9"], recordingStartedAt: "2026-10-28T10:00:00Z", reviewId: "rev-1", status });
const HISTORY = [
  { id: "r-1", tableId: "t-1", userId: "u-a", payerCompanyId: "co-1", startedAt: "2026-10-28T12:30:00Z", reason: "开台后首次交接" },
  { id: "r-2", tableId: "t-1", userId: "u-a", payerCompanyId: "co-1", startedAt: "2026-10-28T12:00:00Z", endedAt: "2026-10-28T12:30:00Z",
    reason: "人工补录（补录单 mb-9）：开台时忘登记服务员" },
];
const SERVICE_BACKFILLS = [
  { id: "mb-8", targetType: "bar_session_service", targetId: "s-1", proposed: { userId: "u-b", startedAt: "2026-10-28T13:00:00Z", endedAt: "2026-10-28T13:10:00Z" },
    evidenceAttachmentIds: ["att-8"], reviewId: "rev-8", status: "pending" },
  { id: "mb-9", targetType: "bar_session_service", targetId: "s-1", proposed: { userId: "u-a", startedAt: "2026-10-28T12:00:00Z", endedAt: "2026-10-28T12:30:00Z" },
    evidenceAttachmentIds: ["att-7"], reviewId: "rev-9", status: "approved" },
];

function renderPage(session: BarTableSessionRow, sales: unknown[] = [], reservations: unknown[] = [], history: unknown[] = HISTORY) {
  const post = vi.fn().mockResolvedValue({ id: "mb-new", status: "pending", reviewId: "rev-new" });
  const api = {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/bar/table-sessions") return [session];
      if (url === "/api/v1/bar/reservations") return reservations;
      if (url.startsWith(`${BACKFILLS}?target_type=bar_session_sales`)) return sales;
      if (url.startsWith(`${BACKFILLS}?target_type=bar_session_service`)) return SERVICE_BACKFILLS;
      if (url.endsWith("/service-responsibilities")) return history;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    postForm: vi.fn().mockResolvedValue({ id: "att-1", contentType: "audio/mp4" }),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = { api, user: { id: "u-desk", username: "前台", accountType: "staff" }, setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
  render(<BarTableSessionsPage {...props} />);
  return { api, post };
}

async function uploadRecording(testId: string) {
  fireEvent.change(screen.getByTestId(`${testId}-evidence`), { target: { files: [new File(["voice"], "通话录音.m4a", { type: "audio/mp4" })] } });
  await waitFor(() => expect((screen.getByTestId(`${testId}-submit`) as HTMLButtonElement).disabled).toBe(false));
}

describe("卡座与订台页人工补录（第十八批 F 段第 14 条 14-R7）", () => {
  it("销售补录行与原记录『无销售（自来客）』并列、标「人工补录」，待复核时现值仍为自来客", async () => {
    const { api } = renderPage(SESSION, [salesRow("pending")]);
    await waitFor(() => expect(screen.queryByTestId("bar-sales-backfill-mb-1")?.textContent ?? "").toContain("人工补录"));
    expect(api.get).toHaveBeenCalledWith(`${BACKFILLS}?target_type=bar_session_sales&target_id=s-1`);
    const row = screen.getByTestId("bar-sales-backfill-mb-1").textContent ?? "";
    expect(row).toContain("无销售（自来客）");
    expect(row).toContain("u-sales");
    expect(row).toContain("待复核");
    expect(row).toContain("rev-1");
    expect(screen.getByTestId("bar-sales-current").textContent).toContain("客人来源：自来客；销售：无");
    expect(screen.getByTestId("bar-sales-current").textContent).not.toContain("人工补录");
  });

  it("补录已通过：销售现值旁标「人工补录」，补录行的原记录仍显示『无销售（自来客）』", async () => {
    renderPage({ ...SESSION, salesStaffId: "u-sales" }, [salesRow("approved")]);
    await waitFor(() => expect(screen.queryByTestId("bar-sales-current")?.textContent ?? "").toContain("人工补录"));
    expect(screen.getByTestId("bar-sales-current").textContent).toContain("u-sales");
    const row = screen.getByTestId("bar-sales-backfill-mb-1").textContent ?? "";
    expect(row).toContain("无销售（自来客）");
    expect(row).toContain("已通过");
  });

  it("会话或其订台已有销售时不给「补录销售归属」按钮；无销售的开台会话给", async () => {
    renderPage({ ...SESSION, salesStaffId: "u-x" });
    await waitFor(() => expect(screen.queryByTestId("bar-sales-current")?.textContent ?? "").toContain("u-x"));
    expect(screen.queryByTestId("bar-sales-backfill-open")).toBeNull();
  });

  it("订台有销售时也不给补录按钮", async () => {
    renderPage({ ...SESSION, reservationId: "rsv-1" }, [], [{ id: "rsv-1", tableId: "t-1", salesStaffId: "u-booker" }]);
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-row-rsv-1")).not.toBeNull());
    expect(screen.queryByTestId("bar-sales-backfill-open")).toBeNull();
  });

  it("销售补录：附通话录音未填录音起始时间前端拦截不发请求；补填后提交体为蛇形键", async () => {
    const { api, post } = renderPage(SESSION);
    await waitFor(() => expect(screen.queryByTestId("bar-sales-backfill-open")?.textContent ?? "").toContain("补录销售归属"));
    fireEvent.click(screen.getByTestId("bar-sales-backfill-open"));
    await waitFor(() => expect(screen.queryByTestId("bar-sales-backfill-form")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-sales-backfill-staff"), { target: { value: "u-sales" } });
    fireEvent.change(screen.getByTestId("bar-sales-backfill-form-reason"), { target: { value: "前台漏登记" } });
    fireEvent.change(screen.getByTestId("bar-sales-backfill-form-responsible"), { target: { value: "u-desk" } });
    await uploadRecording("bar-sales-backfill-form");
    expect(vi.mocked(api.postForm!).mock.calls[0]![1].get("business_type")).toBe("manual_backfill");
    fireEvent.click(screen.getByTestId("bar-sales-backfill-form-submit"));
    await waitFor(() => expect(screen.queryByTestId("bar-sales-backfill-form-error")?.textContent ?? "").toContain("录音起始时间"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("bar-sales-backfill-form-recording"), { target: { value: "2026-10-28T18:00" } });
    fireEvent.click(screen.getByTestId("bar-sales-backfill-form-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/s-1/sales-backfill", expect.objectContaining({
      sales_staff_id: "u-sales", reason: "前台漏登记", responsible_staff_id: "u-desk", evidence_attachment_ids: ["att-1"],
      recording_started_at: new Date("2026-10-28T18:00").toISOString(),
    })));
  });

  it("服务责任历史表补录区间显示「人工补录」、原区间行不变；待复核补录并列在下方（已通过的已进历史表）", async () => {
    const { api } = renderPage(SESSION);
    await waitFor(() => expect(screen.queryByText("查看责任历史")).not.toBeNull());
    fireEvent.click(screen.getByText("查看责任历史"));
    await waitFor(() => expect(screen.queryByTestId("bar-service-backfill-mb-8")?.textContent ?? "").toContain("待复核"));
    expect(api.get).toHaveBeenCalledWith(`${BACKFILLS}?target_type=bar_session_service&target_id=s-1`);
    const history = Array.from(screen.getByTestId("bar-service-history").querySelectorAll("tbody tr")).map((tr) => tr.textContent ?? "");
    expect(history).toHaveLength(2);
    expect(history[0]).toContain("开台后首次交接");
    expect(history[0]).not.toContain("人工补录");
    expect(history[1]).toContain("人工补录");
    expect(screen.getByTestId("bar-service-backfill-mb-8").textContent).toContain("人工补录");
    expect(screen.getByTestId("bar-service-backfill-mb-8").textContent).toContain("u-b");
    expect(screen.queryByTestId("bar-service-backfill-mb-9")).toBeNull();
  });

  it("服务责任补录：附通话录音未填录音起始时间前端拦截；补填后提交体含 user_id、started_at、ended_at", async () => {
    const { post } = renderPage(SESSION);
    await waitFor(() => expect(screen.queryByTestId("bar-service-backfill-open")?.textContent ?? "").toContain("补录服务责任区间"));
    await waitFor(() => expect((screen.getByTestId("bar-service-backfill-open") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("bar-service-backfill-open"));
    await waitFor(() => expect(screen.queryByTestId("bar-service-backfill-form")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-service-backfill-user"), { target: { value: "u-a" } });
    fireEvent.change(screen.getByTestId("bar-service-backfill-start"), { target: { value: "2026-10-28T20:00" } });
    fireEvent.change(screen.getByTestId("bar-service-backfill-end"), { target: { value: "2026-10-28T20:30" } });
    fireEvent.change(screen.getByTestId("bar-service-backfill-form-reason"), { target: { value: "开台时忘登记服务员" } });
    fireEvent.change(screen.getByTestId("bar-service-backfill-form-responsible"), { target: { value: "u-desk" } });
    await uploadRecording("bar-service-backfill-form");
    fireEvent.click(screen.getByTestId("bar-service-backfill-form-submit"));
    await waitFor(() => expect(screen.queryByTestId("bar-service-backfill-form-error")?.textContent ?? "").toContain("录音起始时间"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("bar-service-backfill-form-recording"), { target: { value: "2026-10-28T19:00" } });
    fireEvent.click(screen.getByTestId("bar-service-backfill-form-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/s-1/service-backfill", expect.objectContaining({
      user_id: "u-a", started_at: new Date("2026-10-28T20:00").toISOString(), ended_at: new Date("2026-10-28T20:30").toISOString(),
      reason: "开台时忘登记服务员", responsible_staff_id: "u-desk", evidence_attachment_ids: ["att-1"],
      recording_started_at: new Date("2026-10-28T19:00").toISOString(),
    })));
  });

  // 18F 复核修复：开台、交接时刻都由服务端取（带秒和秒以下），表单只到分钟就补不严空档，补录批准后卡座小费仍是 BAR_SERVICE_HISTORY_INCOMPLETE
  // （BSM:1077「以真实交接记录为依据」）。默认带出空档两端的服务端原串，不改时间即原样提交。
  it("服务责任补录：开始、结束默认带出开台到首次交接的精确时刻，不改时间提交即原样发出", async () => {
    const { post } = renderPage({ ...SESSION, openedAt: "2026-10-28T12:00:42.123456Z" }, [], [],
      [{ id: "r-1", tableId: "t-1", userId: "u-a", payerCompanyId: "co-1", startedAt: "2026-10-28T12:30:17.654321Z", reason: "开台后首次交接" }]);
    await waitFor(() => expect((screen.queryByTestId("bar-service-backfill-open") as HTMLButtonElement | null)?.disabled).toBe(false));
    fireEvent.click(screen.getByTestId("bar-service-backfill-open"));
    await waitFor(() => expect(screen.queryByTestId("bar-service-backfill-form")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-service-backfill-user"), { target: { value: "u-b" } });
    fireEvent.change(screen.getByTestId("bar-service-backfill-form-reason"), { target: { value: "开台时忘登记服务员" } });
    fireEvent.change(screen.getByTestId("bar-service-backfill-form-responsible"), { target: { value: "u-desk" } });
    await uploadRecording("bar-service-backfill-form");
    fireEvent.change(screen.getByTestId("bar-service-backfill-form-recording"), { target: { value: "2026-10-28T19:00" } });
    fireEvent.click(screen.getByTestId("bar-service-backfill-form-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/s-1/service-backfill", expect.objectContaining({
      user_id: "u-b", started_at: "2026-10-28T12:00:42.123456Z", ended_at: "2026-10-28T12:30:17.654321Z",
    })));
  });

  it("服务责任历史按本地时间显示，与并列的补录行同一口径", async () => {
    renderPage(SESSION);
    await waitFor(() => expect(screen.queryByText("查看责任历史")).not.toBeNull());
    fireEvent.click(screen.getByText("查看责任历史"));
    await waitFor(() => expect(screen.getByTestId("bar-service-history").querySelectorAll("tbody tr")).toHaveLength(2));
    const first = screen.getByTestId("bar-service-history").querySelector("tbody tr")?.textContent ?? "";
    expect(first).toContain(new Date("2026-10-28T12:30:00Z").toLocaleString("zh-CN", { hour12: false }));
  });
});
