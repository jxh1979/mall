import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StaffProfilesPage } from "./StaffProfilesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const PROFILES = [
  { id: "sp-1", name: "张三", merchantId: null, userId: "usr_1", registrationStatus: "approved", status: "active", salesPosition: false },
  { id: "sp-2", name: "李四", merchantId: "mch-1", userId: "usr_2", registrationStatus: "approved", status: "active", salesPosition: true },
];

function makeApi() {
  return {
    get: vi.fn(async () => PROFILES),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StaffProfilesPage {...props} />);
}

describe("StaffProfilesPage 员工档案销售岗维护（ADM-STF-004）", () => {
  it("列出员工并显示销售岗是/否", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-profiles-table")).toBeTruthy());
    expect(screen.getByTestId("staff-profile-sales-position-sp-1").textContent).toBe("否");
    expect(screen.getByTestId("staff-profile-sales-position-sp-2").textContent).toBe("是");
  });

  it("不填事由不许改：销售岗决定提成按哪档费率发，必须留痕", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profile-toggle-sales-sp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("staff-profile-toggle-sales-sp-1"));
    expect(screen.getByTestId("staff-profiles-error").textContent).toContain("改动事由");
    expect(api.post).not.toHaveBeenCalled();
  });

  it("填事由后设为销售岗 → 带 salesPosition/reason 调后端", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profiles-reason")).toBeTruthy());
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "转岗销售" } });
    fireEvent.click(screen.getByTestId("staff-profile-toggle-sales-sp-1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/staff/profiles/sp-1/sales-position",
      { salesPosition: true, reason: "转岗销售" },
    ));
  });

  it("已是销售岗的员工按钮为「取消销售岗」，点击下发 false", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profile-toggle-sales-sp-2")).toBeTruthy());
    expect(screen.getByTestId("staff-profile-toggle-sales-sp-2").textContent).toContain("取消销售岗");
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "转岗后勤" } });
    fireEvent.click(screen.getByTestId("staff-profile-toggle-sales-sp-2"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/staff/profiles/sp-2/sales-position",
      { salesPosition: false, reason: "转岗后勤" },
    ));
  });
});
