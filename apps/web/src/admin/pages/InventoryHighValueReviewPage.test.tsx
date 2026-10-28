// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { InventoryHighValueReviewPage } from "./InventoryHighValueReviewPage";
import { ApiError } from "../../shared/apiError";
import type { AdminApiClient, EffectivePermissionsState } from "../types";

// 第十八批第 24 条 24-R2：库存高价值复核后台审批页（ADM-INV-004）。
const ROWS = [
  { id: "hvr-dmg", subjectType: "damage_report", subjectId: "adj-1", reviewStatus: "pending", createdBy: "u-staff",
    createdAt: "2026-10-28T02:00:00Z", photoAttachmentIds: ["att-p"], videoAttachmentIds: ["att-v"],
    subjectSummary: { itemName: "进口威士忌", quantity: 2, unit: "瓶", reason: "碰落破损" } },
  { id: "hvr-dsp", subjectType: "saved_bottle_stock_disposal", subjectId: "adj-2", reviewStatus: "pending", createdBy: "u-staff",
    createdAt: "2026-10-28T03:00:00Z", supervisorApprovedBy: "u-sup",
    subjectSummary: { itemName: "存酒转库存", quantity: 1, unit: "瓶", reason: "过期报废" } },
];

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(ROWS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

function renderPage(api: AdminApiClient, grants: string[] | "all" = "all") {
  const effectivePermissions: EffectivePermissionsState = grants === "all"
    ? { status: "ready", value: { grants: [], denies: [], wildcard: true } }
    : { status: "ready", value: { grants, denies: [], wildcard: false } };
  render(<InventoryHighValueReviewPage api={api} user={{ id: "u1", username: "主管", accountType: "staff" }}
    setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={effectivePermissions} />);
}

describe("InventoryHighValueReviewPage 高价值复核（第十八批第 24 条）", () => {
  it("列出待复核单：中文对象类型、物品与数量、照片视频链接；批准发出 decision=approved", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("hvr-row-hvr-dmg")?.textContent ?? "").toContain("高价值报损"));
    const row = screen.getByTestId("hvr-row-hvr-dmg").textContent ?? "";
    expect(row).toContain("进口威士忌 × 2 瓶");
    expect(row).toContain("照片1");
    expect(row).toContain("视频1");
    expect(screen.getByTestId("hvr-row-hvr-dsp").textContent).toContain("存酒库房存货处置");

    fireEvent.change(screen.getByTestId("hvr-opinion"), { target: { value: "核对无误" } });
    fireEvent.click(screen.getByTestId("hvr-approve-hvr-dmg"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/high-value-reviews/hvr-dmg/review",
      expect.objectContaining({ decision: "approved", reason: "核对无误", idempotency_key: expect.any(String) })));
  });

  it("存酒库房存货处置：门店主管已批后只剩财务批准，财务按钮带 role=finance；只持财务码的账号批不了普通复核单", async () => {
    const api = makeApi();
    renderPage(api, ["finance.ar_ap.manage"]);
    await waitFor(() => expect(screen.queryByTestId("hvr-approve-fin-hvr-dsp")).not.toBeNull());
    expect(screen.queryByTestId("hvr-approve-sup-hvr-dsp")).toBeNull();
    expect((screen.getByTestId("hvr-approve-hvr-dmg") as HTMLButtonElement).disabled).toBe(true);

    fireEvent.click(screen.getByTestId("hvr-approve-fin-hvr-dsp"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/high-value-reviews/hvr-dsp/review",
      expect.objectContaining({ decision: "approved", role: "finance" })));
  });

  it("发起人自批被后端拒绝时显示中文原因", async () => {
    const post = vi.fn().mockRejectedValue(new ApiError("Conflict", {
      detail: { path: "x", status: 409, requestId: null, raw: "" }, code: "CONFLICT", detailCode: "HIGH_VALUE_REVIEW_SELF_APPROVAL" }));
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("hvr-approve-hvr-dmg")).not.toBeNull());
    fireEvent.click(screen.getByTestId("hvr-approve-hvr-dmg"));
    await waitFor(() => expect(screen.getByTestId("hvr-error").textContent).toContain("审批人不得是发起人"));
  });
});
