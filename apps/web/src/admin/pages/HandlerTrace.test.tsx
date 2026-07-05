import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { VendorWorkOrdersPage } from "./VendorWorkOrdersPage";
import { AccessUsersPage } from "./AccessUsersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = {
  status: "ready",
  value: { grants: [], denies: [], wildcard: true },
};

function makeApi(byEndpoint: Record<string, unknown[]>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => byEndpoint[url] ?? []),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function baseProps(api: AdminApiClient, extra: Partial<AdminPageProps> = {}): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
}

describe("处理人链下钻（P2-B）", () => {
  it("外包工单页：点处理人跳 ADM-ACC-001 并带员工账号 id", async () => {
    const orders = [
      { id: "wo-1", code: "WO-0001", status: "assigned", title: "空调维修", assigneeId: "user-77" },
    ];
    const navigateTo = vi.fn();
    render(
      <VendorWorkOrdersPage
        {...baseProps(makeApi({ "/api/v1/facility/work-orders": orders }), { navigateTo })}
      />,
    );

    const link = await screen.findByTestId("trace-link-ADM-ACC-001-user-77");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-ACC-001", "user-77");
  });

  it("外包工单页：处理人为空时降级为纯文本，不产生 trace-link 按钮", async () => {
    const orders = [{ id: "wo-2", code: "WO-0002", status: "pending", title: "无派单" }];
    render(
      <VendorWorkOrdersPage
        {...baseProps(makeApi({ "/api/v1/facility/work-orders": orders }))}
      />,
    );
    await screen.findByTestId("vendor-work-order-select-wo-2");
    expect(screen.queryByTestId(/^trace-link-ADM-ACC-001-/)).toBeNull();
  });

  it("员工账号页（目标）：focusObjectId 命中账号 -> 行高亮定位", async () => {
    const users = [
      { id: "user-77", username: "zhangsan", accountType: "staff", status: "active" },
      { id: "user-88", username: "lisi", accountType: "staff", status: "active" },
    ];
    render(
      <AccessUsersPage
        {...baseProps(makeApi({ "/api/v1/access/users": users }), { focusObjectId: "user-77" })}
      />,
    );

    await waitFor(() => {
      const row = screen.getByTestId("access-user-row-user-77");
      expect(row.className).toContain("admin-row-selected");
    });
    // 非命中行不高亮
    const other = screen.getByTestId("access-user-row-user-88");
    expect(other.className || "").not.toContain("admin-row-selected");
  });

  it("员工账号页（目标）：无 focus 时任何行都不带选中态（向后兼容）", async () => {
    const users = [{ id: "user-77", username: "zhangsan", accountType: "staff", status: "active" }];
    render(<AccessUsersPage {...baseProps(makeApi({ "/api/v1/access/users": users }))} />);
    const row = await screen.findByTestId("access-user-row-user-77");
    expect(row.className || "").not.toContain("admin-row-selected");
  });
});
