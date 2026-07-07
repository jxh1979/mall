import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { SpaceTypesPage } from "./SpaceTypesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-7：新增空间类型改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
const ROWS = [{ id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" }];

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

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<SpaceTypesPage {...props} />);
  return { setActionResult };
}

describe("SpaceTypesPage 引导式新增空间类型(phase-7)", () => {
  it("默认只留入口按钮，不展示弹窗字段", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    expect(screen.queryByTestId("space-types-guided")).toBeNull();
  });

  it("入口→步进→提交：带正确 body 且成功后关闭弹窗", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post);
    const { setActionResult } = renderPage(api);
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-create-open"));

    fireEvent.change(screen.getByLabelText("类型编码"), { target: { value: "floor" } });
    fireEvent.change(screen.getByLabelText("显示名"), { target: { value: "楼层" } });
    fireEvent.change(screen.getByLabelText("排序"), { target: { value: "200" } });
    fireEvent.click(screen.getByTestId("space-types-guided-next"));
    await waitFor(() => expect(screen.getByTestId("space-types-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-save"));

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types", expect.objectContaining({
      typeCode: "floor", typeName: "楼层", sortOrder: 200, status: "active",
    }));
    await waitFor(() => expect(screen.queryByTestId("space-types-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_saved" }));
  });

  it("步内校验拦截：编码/显示名缺失则不进下一步、不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-create-open"));
    fireEvent.change(screen.getByLabelText("类型编码"), { target: { value: "floor" } });
    // 显示名留空
    fireEvent.click(screen.getByTestId("space-types-guided-next"));
    expect(screen.getByTestId("space-types-guided-error").textContent).toContain("请填写类型编码与显示名");
    expect(screen.queryByTestId("space-types-guided-summary")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });
});
