import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FloorPlanImportPage } from "./FloorPlanImportPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PLANS = [
  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "published" },
  { id: "fp-2", plan_name: "二层平面图", building_code: "B1", floor_no: "2F", version_no: "v1", status: "draft" },
];
const IMPORTS = [
  { id: "imp-1", source_name: "cad导出", status: "processing", candidate_count: 3, error_count: 1 },
];
const DETAILS = {
  task: { id: "imp-1", status: "processing" },
  candidates: [
    { id: "cd-1", candidate_no: 1, shape_type: "shop", ref_type: "space", ref_id: "sp-1", label: "A101", status: "pending" },
  ],
  errors: [
    { id: "er-1", line_no: 5, field_name: "geometry", error_code: "INVALID_POLYGON", error_message: "多边形无效" },
  ],
};

function dataEnvelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/imports/imp-1/candidates")) return Promise.resolve(dataEnvelope(DETAILS));
      if (url.includes("/floor-plans/fp-1/imports")) return Promise.resolve(listEnvelope(IMPORTS));
      if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
      return Promise.resolve(listEnvelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FloorPlanImportPage {...fullProps} />);
}

// 打开候选/错误明细上下文（选平面图→选导入任务）后回调，供明细行断言复用。
async function openDetails() {
  renderPage();
  await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
  fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
  await waitFor(() => expect(screen.getByTestId("floor-plan-import-select-imp-1")).toBeTruthy());
  fireEvent.click(screen.getByTestId("floor-plan-import-select-imp-1"));
  await waitFor(() => expect(screen.getByTestId("floor-plan-candidates-table")).toBeTruthy());
}

describe("FloorPlanImportPage 英文全清", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("平面图表状态列渲染中文，不含原始英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plans-table")).toBeTruthy());
    const table = screen.getByTestId("floor-plans-table").textContent ?? "";
    expect(table).toContain("已上架"); // published
    expect(table).toContain("草稿"); // draft
    expect(table).not.toContain("published");
    expect(table).not.toContain("draft");
  });

  it("候选明细表类型/关联/状态渲染中文，不含原始英文码", async () => {
    await openDetails();
    const table = screen.getByTestId("floor-plan-candidates-table").textContent ?? "";
    expect(table).toContain("商铺"); // shapeType shop
    expect(table).toContain("空间"); // refType space
    expect(table).toContain("待处理"); // status pending
    expect(table).not.toContain("shop");
    expect(table).not.toContain("pending");
  });
});

describe("FloorPlanImportPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举", async () => {
    await openDetails();
    fireEvent.click(screen.getByTestId("floor-plan-candidate-row-cd-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("商铺"); // shape_type shop
    expect(text).toContain("空间"); // ref_type space
    expect(text).toContain("待处理"); // status pending
    expect(text).toContain("A101"); // label
    expect(text).not.toContain("shop");
    expect(text).not.toContain("pending");
  });

  it("点错误行弹出抽屉含行号/说明，Esc 关闭", async () => {
    await openDetails();
    fireEvent.click(screen.getByTestId("floor-plan-error-row-er-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("geometry");
    expect(text).toContain("多边形无效");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
