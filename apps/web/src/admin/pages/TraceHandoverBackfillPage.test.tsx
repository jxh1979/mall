// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { TraceHandoverBackfillPage } from "./TraceHandoverBackfillPage";
import type { AdminApiClient, EffectivePermissionsState } from "../types";

// 第十八批 F 段第 14 条 14-R4：交接链与补录（ADM-INV-005）。BSM:1201「补录记录与原始扫码记录并列展示、互不覆盖」；
// BSM:1642「补录记录与原始记录并列展示，标明“人工补录”」。
const BASE = "/api/v1/inventory/trace-handovers";
const HANDOVER = { id: "th-1", traceCode: "TR-001", overallStatus: "in_progress", currentSegment: "kitchen_receive", createdAt: "2026-10-28T01:00:00Z" };
const step = (seq: number, segment: string, stepStatus: string, extra: Record<string, unknown> = {}) =>
  ({ id: `st-${seq}`, handoverId: "th-1", segment, seq, actorType: segment === "dispatch" ? "robot" : "staff", stepStatus, ...extra });
const CHAIN = {
  handover: HANDOVER,
  steps: [
    step(1, "out_verify", "done", { operatorUserId: "u-wh", scanAt: "2026-10-28T01:05:00Z" }),
    step(2, "dispatch", "pending"),
    step(3, "kitchen_receive", "pending"),
    step(4, "staff_pickup", "pending"),
    step(5, "deliver_customer", "pending"),
  ],
  exceptions: [],
  backfills: [{ id: "mb-1", targetType: "trace_handover_step", targetId: "th-1", targetPart: "kitchen_receive", occurredAt: "2026-10-28T01:20:00Z",
    responsibleStaffId: "u-kitchen", evidenceAttachmentIds: ["att-9"], reviewId: "rev-1", status: "pending", submittedBy: "u-clerk",
    submittedAt: "2026-10-28T02:00:00Z", reason: "扫码枪没电" }],
};

function makeApi() {
  const api: AdminApiClient = {
    get: vi.fn(async (path: string) => (path.startsWith(`${BASE}?`) ? [HANDOVER] : CHAIN)) as AdminApiClient["get"],
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({ id: "mb-2", status: "pending", reviewId: "rev-2" }),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
    postForm: vi.fn().mockResolvedValue({ id: "att-1" }) as AdminApiClient["postForm"],
  };
  return api;
}

async function openChain(api: AdminApiClient) {
  const effectivePermissions: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
  render(<TraceHandoverBackfillPage api={api} user={{ id: "u-clerk", username: "库管", accountType: "staff" }}
    setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={effectivePermissions} />);
  await waitFor(() => expect(screen.queryByTestId("thb-trace-code")).not.toBeNull());
  fireEvent.change(screen.getByTestId("thb-trace-code"), { target: { value: "TR-001" } });
  fireEvent.click(screen.getByTestId("thb-search"));
  await waitFor(() => expect(screen.queryByTestId("thb-open-th-1")).not.toBeNull());
  fireEvent.click(screen.getByTestId("thb-open-th-1"));
}

describe("TraceHandoverBackfillPage 交接链与补录（第十八批 F 段第 14 条 14-R4）", () => {
  it("原扫码五段与补录行并列：补录行标「人工补录」，原段仍显示原状态；复核单号链到审批链", async () => {
    const api = makeApi();
    await openChain(api);
    await waitFor(() => expect(screen.queryByTestId("thb-step-kitchen_receive")?.textContent ?? "").toContain("待扫码"));
    expect(api.get).toHaveBeenCalledWith(`${BASE}?trace_code=TR-001`);
    expect(api.get).toHaveBeenCalledWith(`${BASE}/th-1`);
    expect(screen.getByTestId("thb-step-kitchen_receive").textContent).toContain("后厨接收");
    expect(screen.getByTestId("thb-step-out_verify").textContent).toContain("已扫码");
    const backfill = screen.getByTestId("thb-backfill-mb-1").textContent ?? "";
    expect(backfill).toContain("人工补录");
    expect(backfill).toContain("后厨接收");
    expect(backfill).toContain("u-kitchen");
    expect(backfill).toContain("待复核");
    expect(screen.getByTestId("thb-review-mb-1").getAttribute("href")).toContain("/admin/governance/approval-chain?review_id=rev-1");
  });

  it("已扫码段、机器人配送段、已有待复核补录的段不给补录按钮；仍待扫码且无补录的人工段给", async () => {
    const api = makeApi();
    await openChain(api);
    await waitFor(() => expect(screen.queryByTestId("thb-backfill-open-staff_pickup")).not.toBeNull());
    expect(screen.queryByTestId("thb-backfill-open-deliver_customer")).not.toBeNull();
    expect(screen.queryByTestId("thb-backfill-open-out_verify")).toBeNull();
    expect(screen.queryByTestId("thb-backfill-open-dispatch")).toBeNull();
    expect(screen.queryByTestId("thb-backfill-open-kitchen_receive")).toBeNull();
  });

  it("未上传证据时提交按钮禁用；上传后提交，请求体含原时点、原因、责任人与证据", async () => {
    const api = makeApi();
    await openChain(api);
    await waitFor(() => expect(screen.queryByTestId("thb-backfill-open-staff_pickup")).not.toBeNull());
    fireEvent.click(screen.getByTestId("thb-backfill-open-staff_pickup"));
    await waitFor(() => expect((screen.getByTestId("thb-submit") as HTMLButtonElement).disabled).toBe(true));
    fireEvent.change(screen.getByTestId("thb-time"), { target: { value: "09:30" } });
    fireEvent.change(screen.getByTestId("thb-reason"), { target: { value: "服务员取酒时扫码枪没电" } });
    fireEvent.change(screen.getByTestId("thb-responsible"), { target: { value: "u-runner" } });
    fireEvent.change(screen.getByTestId("thb-evidence"), { target: { files: [new File(["voice"], "通话录音.m4a", { type: "audio/mp4" })] } });
    await waitFor(() => expect((screen.getByTestId("thb-submit") as HTMLButtonElement).disabled).toBe(false));
    const form = vi.mocked(api.postForm!).mock.calls[0]![1];
    expect(form.get("business_type")).toBe("manual_backfill");
    fireEvent.click(screen.getByTestId("thb-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(`${BASE}/th-1/segments/staff_pickup/backfill`, expect.objectContaining({
      idempotency_key: expect.any(String), occurred_at: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/), reason: "服务员取酒时扫码枪没电",
      responsible_staff_id: "u-runner", evidence_attachment_ids: ["att-1"],
    })));
  });
});
