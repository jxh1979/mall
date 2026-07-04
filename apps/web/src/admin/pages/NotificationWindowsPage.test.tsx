import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { NotificationWindowsPage } from "./NotificationWindowsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const WINDOWS = [
  { windowKey: "contract.renewal", days: 30, description: "商户合同续约提醒提前天数" },
  { windowKey: "leasing.lead_follow_up", days: 3, description: "招商线索未跟进提醒天数" },
];
function makeApi(get = vi.fn().mockResolvedValue(WINDOWS), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<NotificationWindowsPage {...props} />);
}

describe("NotificationWindowsPage 提醒窗配置(V219)", () => {
  it("读回提醒窗并展示中文描述", async () => {
    const get = vi.fn().mockResolvedValue(WINDOWS);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/analytics/notification-windows"));
    await waitFor(() => expect(screen.getByTestId("notification-windows-table").textContent).toContain("续约"));
  });
});
