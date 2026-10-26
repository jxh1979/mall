// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { NotificationWindowsPage } from "./NotificationWindowsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const WINDOWS = [
  { windowKey: "contract.renewal", days: 30, description: "商户合同续约提醒提前天数" },
  { windowKey: "leasing.lead_follow_up", days: 3, description: "招商线索未跟进提醒天数" },
];
function makeApi(get = vi.fn().mockImplementation(async (path: string) => (path.includes("reminder-policy") ? [] : WINDOWS)), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<NotificationWindowsPage {...props} />);
}

describe("NotificationWindowsPage 提醒窗配置(V219)", () => {
  it("读回提醒窗并展示中文描述", async () => {
    const get = vi.fn().mockImplementation(async (path: string) => (path.includes("reminder-policy") ? [] : WINDOWS));
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/analytics/notification-windows"));
    await waitFor(() => expect(screen.getByTestId("notification-windows-table").textContent).toContain("续约"));
  });
});

// 复核修（BSM:1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、第 7 天、
// 第 3 天、第 1 天）」）：后台能读出当前节点（出厂行＋各项目行），并按项目改节点。
describe("NotificationWindowsPage 存酒到期提醒节点", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("读出出厂节点 14/7/3/1，按本项目改节点提交蛇形请求体", async () => {
    const sent: { method: string; url: string; body: unknown }[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method !== "GET") sent.push({ method, url, body: JSON.parse(String(init?.body)) });
      const data = url.includes("/inventory/saved-bottles/reminder-policy")
        ? (method === "GET" ? { items: [{ scope_id: "global", reminder_days: [14, 7, 3, 1] }], page: 1, page_size: 1, total: 1 } : { scope_id: "project:prj_a", reminder_days: [10, 5, 1] })
        : { items: [], page: 1, page_size: 50, total: 0 };
      return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } }));
    }));
    renderPage(new AdminApi("test-token"));
    await waitFor(() => expect(screen.queryByTestId("saved-bottle-reminder-policy-table")?.textContent ?? "").toContain("到期前第 14、7、3、1 天"));
    expect(screen.getByTestId("saved-bottle-reminder-policy-table").textContent).toContain("出厂默认");
    fireEvent.change(screen.getByLabelText("存酒提醒项目 id"), { target: { value: "prj_a" } });
    fireEvent.change(screen.getByLabelText("存酒提醒节点"), { target: { value: "10,5，1" } });
    fireEvent.click(screen.getByTestId("saved-bottle-reminder-policy-save"));
    await waitFor(() => expect(sent).toHaveLength(1));
    expect(sent[0]!.method).toBe("PUT");
    expect(sent[0]!.url).toContain("/api/v1/inventory/saved-bottles/reminder-policy");
    expect(sent[0]!.body).toMatchObject({ scope_id: "project:prj_a", reminder_days: [10, 5, 1] });
    expect(sent[0]!.body).toHaveProperty("idempotency_key");
  });
});
