import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { SpaceTypesPage } from "./SpaceTypesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(routes: Record<string, unknown>): AdminApiClient {
  const get = vi.fn((url: string) => {
    const key = Object.keys(routes).find((r) => url.startsWith(r));
    return Promise.resolve(key ? routes[key] : []);
  });
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function baseProps(api: AdminApiClient, extra: Partial<AdminPageProps>): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  } as AdminPageProps;
}

describe("全链下钻 P2-A：源页项目/空间引用成可点链接", () => {
  it("项目与场次-场次项目列点击 → navigateTo(ADM-GOV-001, projectId)", async () => {
    const navigateTo = vi.fn();
    const api = makeApi({
      "/api/v1/amusement/projects": [{ id: "amuse-1", projectName: "过山车", projectCode: "P1", status: "active" }],
      "/api/v1/amusement/sessions": [{ id: "sess-1", sessionName: "上午场", projectId: "amuse-1", capacity: 10, status: "open" }],
    });
    render(<AmusementSessionsPage {...baseProps(api, { navigateTo })} />);
    const link = await screen.findByTestId("trace-link-ADM-GOV-001-amuse-1");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-001", "amuse-1");
  });

  it("无 navigateTo 时项目引用降级纯文本（不可点）", async () => {
    const api = makeApi({
      "/api/v1/amusement/projects": [{ id: "amuse-1", projectName: "过山车", projectCode: "P1", status: "active" }],
      "/api/v1/amusement/sessions": [{ id: "sess-1", sessionName: "上午场", projectId: "amuse-1", capacity: 10, status: "open" }],
    });
    render(<AmusementSessionsPage {...baseProps(api, {})} />);
    await screen.findByText("上午场");
    expect(screen.queryByTestId("trace-link-ADM-GOV-001-amuse-1")).toBeNull();
  });
});

describe("全链下钻 P2-A：SpaceTypes(ADM-GOV-007) 按 ?focus= 定位", () => {
  it("带 focusObjectId(命中 typeCode) → 高亮该空间类型行", async () => {
    const api = makeApi({
      "/api/v1/platform/space-types": [
        { id: "st-1", typeCode: "floor", typeName: "楼层", sortOrder: 100, status: "active" },
        { id: "st-2", typeCode: "kiosk", typeName: "一体机位", sortOrder: 200, status: "active" },
      ],
    });
    render(<SpaceTypesPage {...baseProps(api, { focusObjectId: "kiosk" })} />);
    const focused = await screen.findByTestId("space-type-kiosk");
    await waitFor(() => expect(focused.className).toContain("admin-row-selected"));
    // 红绿：非焦点行不得高亮（定位真的挑了行，不是全表高亮）。
    expect(screen.getByTestId("space-type-floor").className ?? "").not.toContain("admin-row-selected");
  });

  it("无 focusObjectId → 无行高亮", async () => {
    const api = makeApi({
      "/api/v1/platform/space-types": [
        { id: "st-1", typeCode: "floor", typeName: "楼层", sortOrder: 100, status: "active" },
      ],
    });
    render(<SpaceTypesPage {...baseProps(api, {})} />);
    const row = await screen.findByTestId("space-type-floor");
    expect(row.className ?? "").not.toContain("admin-row-selected");
  });
});
