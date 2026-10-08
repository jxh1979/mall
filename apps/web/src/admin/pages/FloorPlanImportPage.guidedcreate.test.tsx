import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FloorPlanImportPage } from "./FloorPlanImportPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-10：新增平面图改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// M018-001/005：请求体按后端 FloorPlanService.validateCreateRequest / V023 契约（楼层、底图地址、底图宽高、版本号必填，status active），
// 请求编号由 AdminApi 按「一次操作」管理（M725-F02：页面不再自己保存编号，发出的是 admin-web- 前缀）。
const PLANS = [
  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "active" },
];

function dataEnvelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}

let lastPostBody: Record<string, unknown> | null = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/floor-plans")) {
        lastPostUrl = url;
        lastPostBody = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : null;
        return Promise.resolve(dataEnvelope({ id: "fp-new" }));
      }
      if (url.endsWith("/floor-plans")) return Promise.resolve(listEnvelope(PLANS));
      if (url.includes("/platform/projects")) return Promise.resolve(dataEnvelope([{ id: "prj-1", name: "主项目" }]));
      return Promise.resolve(listEnvelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FloorPlanImportPage {...props} />);
  return { setActionResult };
}

async function openCreate() {
  await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
  fireEvent.click(screen.getByTestId("floor-plan-create-open"));
}

function fillPlanForm(overrides: Partial<Record<string, string>> = {}) {
  const values: Record<string, string> = {
    "floor-plan-project": "prj-1",
    "floor-plan-name": "三层平面图",
    "floor-plan-building": "B2",
    "floor-plan-floor": "3F",
    "floor-plan-image-url": "https://assets.example.com/floor-3.png",
    "floor-plan-image-width": "1600",
    "floor-plan-image-height": "1200",
    "floor-plan-version": "v1",
    ...overrides,
  };
  for (const [testId, value] of Object.entries(values)) {
    fireEvent.change(screen.getByTestId(testId), { target: { value } });
  }
}

describe("FloorPlanImportPage 引导式新增平面图(phase-10)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
  });

  it("页面默认只留入口按钮，不展示平面图字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
    expect(screen.queryByTestId("floor-plan-name")).toBeNull();
    expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull();
  });

  // M018-001/005：POST 体 = 后端必填契约（楼层/底图/宽高/版本号 + status active），请求编号由 AdminApi 生成
  it("入口→步进→提交：POST 新建平面图带后端必填字段与 active 状态，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await openCreate();
    fillPlanForm();
    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next")); // 步1→确认
    await waitFor(() => expect(screen.getByTestId("floor-plan-create-guided-summary")).toBeTruthy());
    expect(screen.getByTestId("floor-plan-create-guided-summary").textContent).toContain("启用");
    expect(screen.getByTestId("floor-plan-create-guided-summary").textContent).not.toContain("草稿");
    fireEvent.click(screen.getByTestId("floor-plan-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/floor-plans");
    expect(lastPostBody).toMatchObject({
      project_id: "prj-1",
      plan_name: "三层平面图",
      building_code: "B2",
      floor_no: "3F",
      image_url: "https://assets.example.com/floor-3.png",
      image_width: 1600,
      image_height: 1200,
      version_no: "v1",
      status: "active",
    });
    expect(String(lastPostBody?.idempotency_key)).toMatch(/^admin-web-/);
    await waitFor(() => expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "floor_plan_created" }));
  });

  it("步内校验拦截：名称留空则不进下一步、不提交", async () => {
    renderPage();
    await openCreate();
    fillPlanForm({ "floor-plan-name": "  " });
    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("名称");
    expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });

  // M018-001：楼层为后端必填（floor_no NOT NULL），前端不再当选填
  it("步内校验拦截：楼层或底图留空则不进下一步、不提交", async () => {
    renderPage();
    await openCreate();
    fillPlanForm({ "floor-plan-floor": "" });
    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("楼层");
    fillPlanForm({ "floor-plan-image-url": " " });
    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("底图");
    expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
