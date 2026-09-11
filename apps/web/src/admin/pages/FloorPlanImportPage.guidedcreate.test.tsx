import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FloorPlanImportPage } from "./FloorPlanImportPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-10：新增平面图改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖「新增平面图」主创建流；导入任务/发布候选等动作不在本期改造范围。
const PLANS = [
  { id: "fp-1", plan_name: "一层平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "published" },
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

let lastPostBody: unknown = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/floor-plans")) {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
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

  it("入口→步进→提交：POST 新建平面图带名称/楼栋/楼层，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-create-open"));

    fireEvent.change(screen.getByTestId("floor-plan-project"), { target: { value: "prj-1" } });
    fireEvent.change(screen.getByTestId("floor-plan-name"), { target: { value: "三层平面图" } });
    fireEvent.change(screen.getByTestId("floor-plan-building"), { target: { value: "B2" } });
    fireEvent.change(screen.getByTestId("floor-plan-floor"), { target: { value: "3F" } });
    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next")); // 步1→确认
    await waitFor(() => expect(screen.getByTestId("floor-plan-create-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/floor-plans");
    expect((lastPostBody as { project_id: string }).project_id).toBe("prj-1");
    expect((lastPostBody as { plan_name: string }).plan_name).toBe("三层平面图");
    expect((lastPostBody as { building_code: string }).building_code).toBe("B2");
    expect((lastPostBody as { floor_no: string }).floor_no).toBe("3F");
    expect((lastPostBody as { status: string }).status).toBe("draft");
    await waitFor(() => expect(screen.queryByTestId("floor-plan-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "floor_plan_created" }));
  });

  it("步内校验拦截：名称留空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("floor-plan-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("floor-plan-create-open"));
    fireEvent.change(screen.getByTestId("floor-plan-project"), { target: { value: "prj-1" } });
    fireEvent.change(screen.getByTestId("floor-plan-name"), { target: { value: "  " } });
    fireEvent.click(screen.getByTestId("floor-plan-create-guided-next"));
    expect(screen.getByTestId("floor-plan-create-guided-error").textContent).toContain("名称");
    expect(screen.queryByTestId("floor-plan-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
