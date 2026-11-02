// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 M 段第 117 条 117-R3（BSM:1380「派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，
// 同时在工单上标明费用承担方……承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。」）：楼宇运营工作台工单行「用料」面板。
const ORDERS = [
  { id: "fwo_1", code: "FWO-1", workOrderType: "repair", priority: "normal", status: "processing", title: "商户门锁坏了", merchantId: "M" },
  { id: "fwo_2", code: "FWO-2", workOrderType: "repair", priority: "normal", status: "assigned", title: "灯具不亮", merchantId: null },
];
const STOCK = [{ warehouseId: "W", warehouseName: "工程配件库", locationId: "L", itemId: "I", itemName: "门锁锁芯", availableQuantity: 2 }];
const MERCHANTS = [{ merchantId: "M", merchantName: "用料回归商户" }];
const BLANK = { lines: [], totalCostCent: 0, bearer: null, disputes: [], options: { stock: STOCK, merchants: MERCHANTS } };
const MARKED = { ...BLANK, bearer: { costBearer: "merchant", merchantId: "M", merchantName: "用料回归商户" } };
const PENDING = {
  ...MARKED,
  disputes: [{ id: "d1", fromCostBearer: "merchant", fromMerchantId: "M", claimedCostBearer: "property", claimedMerchantId: null,
    reason: "属楼宇主体设备", status: "pending", raisedAt: "2026-11-01T02:00:00Z" }],
};
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const postMock = () => vi.fn(async (_url: string, _body?: unknown) => ({}) as unknown);

function makeApi(materials: unknown, post = postMock()): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/facility/work-orders") return ORDERS;
      if (url.endsWith("/materials")) return materials;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props = {
    api,
    user: { id: "usr_mgr", username: "mgr", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

async function openMaterials(id: string) {
  await waitFor(() => expect(screen.queryByTestId(`facility-work-order-actions-${id}`)).not.toBeNull());
  fireEvent.click(screen.getByTestId(`facility-work-order-actions-${id}`));
  await waitFor(() => expect(screen.queryByTestId(`facility-work-order-materials-${id}`)).not.toBeNull());
  fireEvent.click(screen.getByTestId(`facility-work-order-materials-${id}`));
}

describe("FacilityWorkbenchPage 工单用料（第 117 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("处理中的工单点「用料」读该单用料；选库房、物料与数量领用出库，只提交库位、物料与数量", async () => {
    const post = postMock();
    const api = makeApi(BLANK, post);
    renderPage(api);
    await openMaterials("fwo_1");
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/facility/work-orders/fwo_1/materials"));
    await waitFor(() => expect(screen.queryByTestId("work-order-materials-issue")).not.toBeNull());
    fireEvent.change(screen.getByTestId("work-order-materials-warehouse"), { target: { value: "W" } });
    fireEvent.change(screen.getByTestId("work-order-materials-stock"), { target: { value: "I::L" } });
    fireEvent.change(screen.getByTestId("work-order-materials-quantity"), { target: { value: "1" } });
    fireEvent.click(screen.getByTestId("work-order-materials-issue-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/fwo_1/materials", { locationId: "L", itemId: "I", quantity: 1 }));
  });

  it("选「商户承担」与商户后标明费用承担方", async () => {
    const post = postMock();
    renderPage(makeApi(BLANK, post));
    await openMaterials("fwo_1");
    await waitFor(() => expect(screen.queryByTestId("work-order-materials-bearer")).not.toBeNull());
    expect(screen.getByTestId("work-order-materials-bearer").textContent).toContain("公区设施与楼宇主体设备的用料由物业承担");
    fireEvent.click(screen.getByTestId("work-order-materials-bearer-merchant"));
    fireEvent.change(screen.getByTestId("work-order-materials-bearer-merchant-id"), { target: { value: "M" } });
    fireEvent.click(screen.getByTestId("work-order-materials-bearer-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/fwo_1/cost-bearer", { costBearer: "merchant", merchantId: "M" }));
  });

  it("承担方争议不填原因不发请求并提示必填；填了原因提交主张的承担方与原因", async () => {
    const post = postMock();
    renderPage(makeApi(MARKED, post));
    await openMaterials("fwo_1");
    await waitFor(() => expect(screen.queryByTestId("work-order-materials-dispute")).not.toBeNull());
    fireEvent.click(screen.getByTestId("work-order-materials-dispute-submit"));
    await waitFor(() => expect(screen.getByTestId("work-order-materials-error").textContent).toContain("请填写争议原因"));
    expect(post.mock.calls.some(([url]) => String(url).endsWith("/bearer-dispute"))).toBe(false);
    fireEvent.change(screen.getByTestId("work-order-materials-dispute-reason"), { target: { value: "属楼宇主体设备" } });
    fireEvent.click(screen.getByTestId("work-order-materials-dispute-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/fwo_1/bearer-dispute",
      expect.objectContaining({ claimedCostBearer: "property", reason: "属楼宇主体设备" })));
  });

  it("有待上级判定的争议时不显示承担方与争议表单，争议记录显示「待上级判定」", async () => {
    renderPage(makeApi(PENDING));
    await openMaterials("fwo_1");
    await waitFor(() => expect(screen.queryByTestId("work-order-materials-disputes")).not.toBeNull());
    expect(screen.getByTestId("work-order-materials-disputes").textContent).toContain("待上级判定");
    expect(screen.queryByTestId("work-order-materials-bearer")).toBeNull();
    expect(screen.queryByTestId("work-order-materials-dispute")).toBeNull();
  });

  it("已派单未开始的工单不显示「领用出库」", async () => {
    renderPage(makeApi(BLANK));
    await openMaterials("fwo_2");
    await waitFor(() => expect(screen.queryByTestId("work-order-materials-bearer")).not.toBeNull());
    expect(screen.queryByTestId("work-order-materials-issue")).toBeNull();
  });
});
