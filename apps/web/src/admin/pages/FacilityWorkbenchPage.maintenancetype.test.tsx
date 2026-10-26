// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十六批第 35 条（ENUM「物业工单类型」L260「……巡检、内部协作工单（内部部门间发起与流转）、其他服务请求」）；
// 第 36 条（ENUM「设备维保类型」L267「定期保养、例行巡检、故障维修、安全检测、部件更换」；BSM L1408「维保分……」）。
const EQUIPMENT = { id: "x", name: "3号冷却塔", equipmentType: "hvac", status: "normal", lastMaintenanceType: "safety_test" };
const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url === "/api/v1/facility/equipment" ? [EQUIPMENT] : [])),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ALL,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

describe("FacilityWorkbenchPage 内部协作工单与设备维保类型（第十六批第 35、36 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("新建工单与工单策略的类型下拉都有「内部协作工单」", async () => {
    renderPage(makeApi(vi.fn()));

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-type")).not.toBeNull());
    for (const testId of ["facility-work-order-type", "facility-policy-type"]) {
      const options = Array.from((screen.getByTestId(testId) as HTMLSelectElement).options).map((o) => [o.value, o.textContent]);
      expect(options).toContainEqual(["internal_collaboration", "内部协作工单"]);
    }
  });

  it("维保完成须先选五类维保类型之一，带 maintenance_type 提交", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);

    await waitFor(() => expect(screen.queryByTestId("facility-equipment-maintenance-done-x")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-equipment-maintenance-done-x"));
    await waitFor(() => expect(screen.queryByTestId("facility-maintenance-type")).not.toBeNull());
    const labels = Array.from((screen.getByTestId("facility-maintenance-type") as HTMLSelectElement).options)
      .filter((o) => o.value).map((o) => o.textContent);
    expect(labels).toEqual(["定期保养", "例行巡检", "故障维修", "安全检测", "部件更换"]);

    fireEvent.click(screen.getByTestId("facility-maintenance-done-submit"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("请选择本次维保类型"));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-maintenance-type"), { target: { value: "part_replacement" } });
    fireEvent.click(screen.getByTestId("facility-maintenance-done-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    const [path] = post.mock.calls[0] as unknown as [string];
    expect(path).toMatch(/^\/api\/v1\/facility\/equipment\/x\/maintenance-done\?idempotency_key=.+&maintenance_type=part_replacement$/);
  });

  it("设备明细显示最近维保类型中文", async () => {
    renderPage(makeApi(vi.fn()));

    await waitFor(() => expect(screen.queryByTestId("facility-equipment-x")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-equipment-x"));
    await waitFor(() => expect(screen.queryByText("最近维保类型")).not.toBeNull());
    expect(screen.queryByText("安全检测")).not.toBeNull();
  });
});
