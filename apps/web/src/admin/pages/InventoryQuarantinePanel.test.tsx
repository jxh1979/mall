// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { InventoryQuarantinePanel } from "./InventoryQuarantinePanel";
import type { AdminApiClient } from "../types";

const lot = { id: "lot_1", itemId: "item_1", quantity: 10, remainingQuantity: 10, receivedAt: "2026-10-01T00:00:00Z", sourceId: "resolution_1" };
function setup(plans: unknown[] = []) {
  const api = { get: vi.fn(async (path: string) => path.endsWith("/warehouses") ? [{ id: "wh_1", name: "项目中心仓" }] : path.includes("?warehouse_id=") ? [lot] : plans), post: vi.fn().mockResolvedValue({ status: "pending_approval" }), postForm: vi.fn().mockResolvedValue({ id: "proof_1" }) } as unknown as AdminApiClient;
  render(<InventoryQuarantinePanel api={api} canRead canManage />);
  return { api };
}
async function openLot() {
  fireEvent.click(screen.getByRole("button", { name: "隔离库存与后续处置" }));
  fireEvent.click(await screen.findByRole("button", { name: "查看处置" }));
  await screen.findByText("item_1 的隔离处置");
}
describe("隔离库存实际处置", () => {
  it("方案申请只提交主管及财务审批，不能把尚未实际处置的十件改成可售", async () => {
    const { api } = setup(); await openLot();
    fireEvent.click(screen.getByRole("button", { name: "新建隔离处置方案" }));
    fireEvent.change(screen.getByLabelText("隔离处置数量"), { target: { value: "4" } });
    fireEvent.change(screen.getByLabelText("处置依据"), { target: { value: "复检确认可售" } });
    fireEvent.change(screen.getByLabelText("主管最终审批人"), { target: { value: "supervisor" } });
    fireEvent.change(screen.getByLabelText("财务最终审批人"), { target: { value: "finance" } });
    fireEvent.click(screen.getByRole("button", { name: "提交处置审批" }));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("隔离处置凭证"), { target: { files: [new File(["image"], "复检.png", { type: "image/png" })] } });
    await screen.findByText("复检.png（已上传）");
    fireEvent.click(screen.getByRole("button", { name: "提交处置审批" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/quarantine/lot_1/dispositions", expect.objectContaining({ quantity: 4, dispositionType: "release_resalable", finalApproverId: "supervisor", parallelLines: [{ lineCode: "finance_review", finalApproverId: "finance" }], evidenceAttachmentIds: ["proof_1"] })));
    expect(screen.getByText("方案已提交审批，当前隔离余额尚未改变。")).toBeTruthy();
    expect(api.post).not.toHaveBeenCalledWith(expect.stringContaining("/complete"), expect.anything());
  });
  it("已批准报损仍须照片和视频及实际完成动作，不能只凭审批扣减", async () => {
    const { api } = setup([{ id: "disp_1", quantity: 4, dispositionType: "scrap", status: "approved", reason: "不可复售", reviewId: "review_1" }]); await openLot();
    fireEvent.click(await screen.findByRole("button", { name: "登记实际处置完成" }));
    fireEvent.change(screen.getByLabelText("隔离处置凭证"), { target: { files: [new File(["image"], "实物.png", { type: "image/png" })] } });
    await screen.findByText("实物.png（已上传）");
    fireEvent.click(screen.getByRole("button", { name: "确认实际处置" }));
    await screen.findByText("报损须同时上传照片和视频"); expect(api.post).not.toHaveBeenCalled();
    vi.mocked(api.postForm!).mockResolvedValueOnce({ id: "proof_video" });
    fireEvent.change(screen.getByLabelText("隔离处置凭证"), { target: { files: [new File(["video"], "处置.mp4", { type: "video/mp4" })] } });
    await screen.findByText("处置.mp4（已上传）");
    fireEvent.click(screen.getByRole("button", { name: "确认实际处置" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/quarantine/dispositions/disp_1/complete", expect.objectContaining({ completedAt: expect.any(String), evidenceAttachmentIds: ["proof_1", "proof_video"] })));
  });
});
