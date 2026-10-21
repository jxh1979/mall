// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// HP-F03（BSM「楼宇机电设备」L1408「楼宇设备报废由设备所有权主体确认，沿现有审批流程办理，记录批准人、处置方式及实际处置凭证；
// 残值或损失由财务记录并关联原设备。标记报废不等于实物处置或款项完成」）：报废走申请审批，审批/实物/款项三项分开展示与登记。
const EQUIPMENT = { id: "x", name: "3号冷却塔", equipmentType: "hvac", status: "normal" };
const SCRAP = {
  id: "fes_1", equipmentId: "x", equipmentName: "3号冷却塔", disposalMethod: "整机交回收商", approvalStatus: "approved",
  approvedBy: "usr_boss", disposalStatus: "pending", moneyStatus: "residual_pending_collection", completed: false,
};
const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const FACILITY_ONLY: EffectivePermissionsState = {
  status: "ready", value: { grants: ["facility.equipment.manage", "facility.work_order.manage"], denies: [], wildcard: false },
};

function makeApi(post: ReturnType<typeof vi.fn>, postForm?: ReturnType<typeof vi.fn>, scrap: Record<string, unknown> = SCRAP): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/facility/equipment") return [EQUIPMENT];
      if (url === "/api/v1/facility/equipment-scraps") return [scrap];
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    postForm,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, effectivePermissions = ALL) {
  const props = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

describe("FacilityWorkbenchPage 楼宇设备报废处置（HP-F03）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("状态下拉不再提供报废，申请报废按前端形状提交到 scrap-requests", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post));

    await waitFor(() => expect(screen.queryByTestId("facility-equipment-scrap-open-x")).not.toBeNull());
    const statuses = Array.from((screen.getByTestId("facility-equipment-status") as HTMLSelectElement).options).map((o) => o.value);
    expect(statuses).not.toContain("retired");

    fireEvent.click(screen.getByTestId("facility-equipment-scrap-open-x"));
    await waitFor(() => expect(screen.queryByTestId("facility-scrap-reason")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-scrap-reason"), { target: { value: "压缩机烧毁" } });
    fireEvent.change(screen.getByTestId("facility-scrap-method"), { target: { value: "整机交回收商" } });
    fireEvent.click(screen.getByTestId("facility-scrap-submit"));

    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/equipment/x/scrap-requests",
      expect.objectContaining({ reason: "压缩机烧毁", disposalMethod: "整机交回收商" })));
  });

  it("报废处置表分别显示审批、实物处置与款项，已批准未处置不算收尾", async () => {
    renderPage(makeApi(vi.fn()));

    await waitFor(() => expect(screen.queryByTestId("facility-scrap-fes_1")).not.toBeNull());
    const row = screen.getByTestId("facility-scrap-fes_1").textContent ?? "";
    expect(row).toContain("已批准");
    expect(row).toContain("待处置");
    expect(row).toContain("残值待收");
    expect(row).not.toContain("已全部收尾");
  });

  it("登记实际处置先上传 facility_equipment_scrap 凭证再提交 evidenceAttachmentIds", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const postForm = vi.fn(async (_path: string, _form: FormData) => ({ id: "att_1" }) as unknown);
    renderPage(makeApi(post, postForm));

    await waitFor(() => expect(screen.queryByTestId("facility-scrap-dispose-open-fes_1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-scrap-dispose-open-fes_1"));
    await waitFor(() => expect(screen.queryByTestId("facility-scrap-evidence")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-scrap-evidence"), {
      target: { files: [new File(["x"], "proof.png", { type: "image/png" })] },
    });
    await waitFor(() => expect(postForm).toHaveBeenCalled());
    const [path, form] = postForm.mock.calls[0] ?? [];
    expect(path).toBe("/api/v1/evidence/attachments/upload");
    expect(form?.get("business_type")).toBe("facility_equipment_scrap");
    expect(form?.get("business_id")).toBe("fes_1");

    fireEvent.change(screen.getByTestId("facility-scrap-disposal-note"), { target: { value: "回收商已拉走" } });
    fireEvent.click(screen.getByTestId("facility-scrap-disposal-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/equipment-scraps/fes_1/disposal",
      expect.objectContaining({ evidenceAttachmentIds: ["att_1"], disposalNote: "回收商已拉走" })));
  });

  it("财务登记残值/损失按元录入转分后提交到 finance", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post, undefined, { ...SCRAP, moneyStatus: "pending_finance" }));

    await waitFor(() => expect(screen.queryByTestId("facility-scrap-finance-open-fes_1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-scrap-finance-open-fes_1"));
    await waitFor(() => expect(screen.queryByTestId("facility-scrap-residual")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-scrap-residual"), { target: { value: "300.50" } });
    fireEvent.change(screen.getByTestId("facility-scrap-counterparty"), { target: { value: "recycler_x" } });
    fireEvent.change(screen.getByTestId("facility-scrap-loss"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("facility-scrap-finance-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/equipment-scraps/fes_1/finance",
      expect.objectContaining({ residualAmountCent: 30050, residualCounterpartyId: "recycler_x", lossAmountCent: 0 })));
  });

  it("无 finance.ar_ap.manage 时财务登记按钮不可用", async () => {
    renderPage(makeApi(vi.fn(), undefined, { ...SCRAP, moneyStatus: "pending_finance" }), FACILITY_ONLY);

    await waitFor(() => expect(screen.queryByTestId("facility-scrap-finance-open-fes_1")).not.toBeNull());
    expect((screen.getByTestId("facility-scrap-finance-open-fes_1") as HTMLButtonElement).disabled).toBe(true);
  });
});
