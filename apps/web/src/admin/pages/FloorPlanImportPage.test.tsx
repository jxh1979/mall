import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FloorPlanImportPage } from "./FloorPlanImportPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 夹具只用后端闭集（FloorPlanService STATUSES / IMPORT_TASK_STATUSES / IMPORT_CANDIDATE_STATUSES、FloorPlanImportParser 错误字段/错误码）——M018-002。
const PLANS = [
  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" },
  { id: "fp-2", plan_name: "二层平面图", building_code: "B1", floor_no: "2F", version_no: "v1", status: "archived" },
];
const IMPORTS = [
  { id: "imp-1", source_name: "cad导出", status: "parsed", candidate_count: 3, error_count: 1 },
];
const DETAILS = {
  task: { id: "imp-1", status: "parsed" },
  candidates: [
    { id: "cd-1", candidate_no: 1, shape_type: "shop", ref_type: "space", ref_id: "sp-1", label: "A101", status: "pending" },
    { id: "cd-2", candidate_no: 2, shape_type: "corridor", ref_type: "space", ref_id: "sp-2", label: "旧数据", status: "pending" },
  ],
  errors: [
    { id: "er-1", line_no: 5, field_name: "points", error_code: "POINTS_TOO_FEW", error_message: "多边形至少需要 3 个顶点" },
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
function errorEnvelope(status: number, code: string, message: string): Response {
  return new Response(
    JSON.stringify({ success: false, code, message, data: null }),
    { status, headers: { "content-type": "application/json" } },
  );
}

let lastPost: { url: string; body: Record<string, unknown> } | null = null;
let projectsRequests = 0;
function makeApi(options: { importsStatus?: number; projectsStatus?: number } = {}): AdminApi {
  projectsRequests = 0;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        lastPost = { url, body: init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {} };
        if (url.endsWith("/publish")) return Promise.resolve(dataEnvelope({ id: "imp-1", status: "published" }));
        if (url.endsWith("/imports")) return Promise.resolve(dataEnvelope({ id: "imp-2", status: "parsed" }));
        return Promise.resolve(dataEnvelope({ id: "fp-new" }));
      }
      if (url.includes("/imports/imp-1/candidates")) return Promise.resolve(dataEnvelope(DETAILS));
      if (url.includes("/floor-plans/fp-1/imports")) {
        if (options.importsStatus === 403) return Promise.resolve(errorEnvelope(403, "PERMISSION_DENIED", "当前账号无权查看导入任务"));
        return Promise.resolve(listEnvelope(IMPORTS));
      }
      if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
      if (url.includes("/platform/projects")) {
        projectsRequests += 1;
        if (options.projectsStatus === 403) return Promise.resolve(errorEnvelope(403, "PERMISSION_DENIED", "当前账号无权查看项目"));
        return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
      }
      return Promise.resolve(listEnvelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
// 只授平面图读写 + 导入只读，不授 floor_plan.import.manage（V034 独立高危码）
const NO_IMPORT_MANAGE: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["floor_plan.view", "floor_plan.manage", "floor_plan.import.view"], denies: [], wildcard: false },
};
const VIEW_ONLY: EffectivePermissionsState = { status: "ready", value: { grants: ["floor_plan.view"], denies: [], wildcard: false } };

function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS, apiOptions: { importsStatus?: number; projectsStatus?: number } = {}) {
  const setGlobalError = vi.fn();
  const setActionResult = vi.fn();
  const fullProps: AdminPageProps = {
    api: makeApi(apiOptions),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError,
    effectivePermissions: perms,
  };
  render(<FloorPlanImportPage {...fullProps} />);
  return { setGlobalError, setActionResult };
}

// 打开候选/错误明细上下文（选平面图→选导入任务）后回调，供明细行断言复用。
async function openDetails(perms: EffectivePermissionsState = ADMIN_PERMS) {
  const spies = renderPage(perms);
  await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
  fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
  await waitFor(() => expect(screen.getByTestId("floor-plan-import-select-imp-1")).toBeTruthy());
  fireEvent.click(screen.getByTestId("floor-plan-import-select-imp-1"));
  await waitFor(() => expect(screen.getByTestId("floor-plan-candidates-table")).toBeTruthy());
  return spies;
}

describe("FloorPlanImportPage 英文全清（M018-002：后端闭集全部有中文）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
  });

  it("平面图表状态列按后端闭集渲染中文，不含原始英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plans-table")).toBeTruthy());
    const table = screen.getByTestId("floor-plans-table").textContent ?? "";
    expect(table).toContain("启用"); // active
    expect(table).toContain("已存档"); // archived
    expect(table).not.toContain("active");
    expect(table).not.toContain("archived");
  });

  it("导入任务表状态 parsed 渲染「已解析」，不直出英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-imports-table")).toBeTruthy());
    const table = screen.getByTestId("floor-plan-imports-table").textContent ?? "";
    expect(table).toContain("已解析");
    expect(table).not.toContain("parsed");
  });

  it("候选明细表类型/关联/状态渲染中文，不含原始英文码", async () => {
    await openDetails();
    const table = screen.getByTestId("floor-plan-candidates-table").textContent ?? "";
    expect(table).toContain("铺位"); // shapeType shop
    expect(table).toContain("空间"); // refType space
    expect(table).toContain("待复核"); // status pending
    expect(table).not.toContain("shop");
    expect(table).not.toContain("pending");
  });

  it("导入错误表按解析器真实字段/错误码渲染中文，不显示未识别", async () => {
    await openDetails();
    const table = screen.getByTestId("floor-plan-errors-table").textContent ?? "";
    expect(table).toContain("顶点"); // field points
    expect(table).toContain("顶点不足"); // POINTS_TOO_FEW
    expect(table).toContain("多边形至少需要 3 个顶点");
    expect(table).not.toContain("POINTS_TOO_FEW");
    expect(table).not.toContain("未识别");
  });
});

describe("FloorPlanImportPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
  });

  it("点候选行弹出抽屉，含类型/关联/状态中文，无英文枚举", async () => {
    await openDetails();
    fireEvent.click(screen.getByTestId("floor-plan-candidate-row-cd-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("铺位"); // shape_type shop
    expect(text).toContain("空间"); // ref_type space
    expect(text).toContain("待复核"); // status pending
    expect(text).toContain("A101"); // label
    expect(text).not.toContain("shop");
    expect(text).not.toContain("pending");
  });

  it("点错误行弹出抽屉含行号/字段/错误码中文与说明，Esc 关闭", async () => {
    await openDetails();
    fireEvent.click(screen.getByTestId("floor-plan-error-row-er-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("顶点");
    expect(text).toContain("顶点不足");
    expect(text).toContain("多边形至少需要 3 个顶点");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});

describe("FloorPlanImportPage 闭集与导入格式", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
  });

  it("未知候选种类显示未识别，不显示其他或走道", async () => {
    await openDetails();
    const table = screen.getByTestId("floor-plan-candidates-table").textContent ?? "";
    expect(table).toContain("铺位");
    expect(table).toContain("未识别");
    expect(table).not.toContain("其他");
    expect(table).not.toContain("走道");
  });

  it("创建导入下拉只有矢量图和标准数据", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-import-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-import-open"));
    const text = screen.getByTestId("floor-plan-import-guided").textContent ?? "";
    expect(text).toContain("矢量图");
    expect(text).toContain("标准数据");
    expect(text).not.toContain("geojson");
  });

  // M018-007/005/006：来源名称必填（后端 source_name NOT NULL）；POST 体与幂等键
  it("创建导入：来源名称留空被步内拦截；填齐后 POST 带格式/名称/内容且带请求编号", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-import-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-import-open"));
    fireEvent.change(screen.getByTestId("floor-plan-import-payload"), { target: { value: "<svg viewBox=\"0 0 10 10\"></svg>" } });
    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
    expect(screen.getByTestId("floor-plan-import-guided-error").textContent).toContain("来源名称");
    expect(lastPost).toBeNull();

    fireEvent.change(screen.getByTestId("floor-plan-import-name"), { target: { value: "cad导出2" } });
    fireEvent.click(screen.getByTestId("floor-plan-import-guided-next"));
    await waitFor(() => expect(screen.getByTestId("floor-plan-import-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-import-create"));
    await waitFor(() => expect(lastPost).toBeTruthy());
    expect(lastPost?.url).toContain("/floor-plans/fp-1/imports");
    expect(lastPost?.body).toMatchObject({ source_format: "svg", source_name: "cad导出2", source_payload: "<svg viewBox=\"0 0 10 10\"></svg>" });
    expect(String(lastPost?.body.idempotency_key)).toMatch(/^admin-web-/);
  });
});

describe("FloorPlanImportPage 发布、权限与下钻错误（M018-003/004/005/008）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
  });

  it("未勾选候选时发布被拦截（不弹确认、不请求）；勾选后确认 POST 只带勾选的候选且带请求编号", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { setGlobalError } = await openDetails();
    fireEvent.click(screen.getByTestId("floor-plan-publish"));
    expect(setGlobalError).toHaveBeenCalledWith("请勾选要发布的区域");
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(lastPost).toBeNull();

    fireEvent.click(screen.getByLabelText("A101"));
    fireEvent.click(screen.getByTestId("floor-plan-publish"));
    expect(confirmSpy).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(lastPost).toBeTruthy());
    expect(lastPost?.url).toContain("/floor-plans/fp-1/imports/imp-1/publish");
    expect(lastPost?.body).toMatchObject({ candidate_ids: ["cd-1"] });
    expect(String(lastPost?.body.idempotency_key)).toMatch(/^admin-web-/);
  });

  it("无 floor_plan.import.manage 时创建导入与发布按钮禁用（新增平面图仍可用）", async () => {
    await openDetails(NO_IMPORT_MANAGE);
    expect(screen.getByTestId("floor-plan-import-open")).toBeDisabled();
    expect(screen.getByTestId("floor-plan-publish")).toBeDisabled();
    expect(screen.getByTestId("floor-plan-create-open")).not.toBeDisabled();
  });

  it("无 floor_plan.import.view 时不显示下钻按钮", async () => {
    renderPage(VIEW_ONLY);
    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
    expect(screen.queryByTestId("floor-plan-select-fp-1")).toBeNull();
  });

  it("下钻读取导入任务失败（403）时报错到全局，不静默", async () => {
    const { setGlobalError } = renderPage(ADMIN_PERMS, { importsStatus: 403 });
    await waitFor(() => expect(screen.getByTestId("floor-plan-select-fp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-select-fp-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("无权")));
  });
});

// M018-010：项目清单是副资源——只授 floor_plan.view 的账号不请求它、平面图表照常；projects 403 只降级提示，不整页「平面图读取失败」。
describe("FloorPlanImportPage 项目清单副资源降级（M018-010）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
  });

  it("仅 floor_plan.view：不请求项目清单，平面图表照常渲染", async () => {
    const { setGlobalError } = renderPage(VIEW_ONLY);
    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
    expect(projectsRequests).toBe(0);
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining("读取失败"));
  });

  it("项目清单 403：平面图表仍渲染，新建弹窗显示降级提示，不报整页读取失败", async () => {
    const { setGlobalError } = renderPage(ADMIN_PERMS, { projectsStatus: 403 });
    await waitFor(() => expect(screen.getByTestId("floor-plans-table").textContent).toContain("一层平面图"));
    expect(projectsRequests).toBe(1);
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.stringContaining("读取失败"));
    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
    expect(screen.getByTestId("floor-plan-projects-degraded").textContent).toMatch(/无权|加载失败/);
  });
});
