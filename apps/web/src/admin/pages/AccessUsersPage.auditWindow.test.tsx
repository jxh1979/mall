// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessUsersPage } from "./AccessUsersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BX 段第 372 条 372-R5（第十八批裁决 77「开外审权限必须划定数据起止日期」；PGM:682「外部审计/监管只能查看划定的范围与时间段」）：
// 员工账号页显示并划定外审数据时段。
const USERS = [
  { id: "u-1", username: "外审甲", account_type: "outsourced_vendor", status: "active", organization_id: "org-1" },
  { id: "u-2", username: "员工乙", account_type: "staff", status: "active", organization_id: "org-1" },
];
const WINDOWS = [{ user_id: "u-1", data_from: "2026-01-01", data_to: "2026-12-31" }];

function text(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

function envelope(items: unknown[]): Response {
  return text(200, { success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(putResponse: () => Response, windowsResponse: () => Response = () => envelope(WINDOWS)) {
  const puts: Array<{ url: string; body: Record<string, unknown> }> = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "PUT" && url.includes("/audit-data-window")) {
        puts.push({ url, body: JSON.parse(String(init.body)) as Record<string, unknown> });
        return Promise.resolve(putResponse());
      }
      if (url.includes("audit-data-windows")) return Promise.resolve(windowsResponse());
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }),
  );
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u0", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AccessUsersPage {...props} />);
  return puts;
}

async function fillAndSubmit() {
  const userSelect = screen.getByTestId("audit-window-user") as HTMLSelectElement;
  await waitFor(() => expect(userSelect.options.length).toBe(2)); // 空选项＋外部人员账号；员工账号不能划定
  fireEvent.change(userSelect, { target: { value: "u-1" } });
  fireEvent.change(screen.getByTestId("audit-window-from"), { target: { value: "2026-03-01" } });
  fireEvent.change(screen.getByTestId("audit-window-to"), { target: { value: "2026-09-30" } });
  fireEvent.click(screen.getByTestId("audit-window-submit"));
}

describe("AccessUsersPage 外审数据时段（372-R5）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("账号表显示已划定的时段；选外部人员账号、填起止日期点划定 → PUT 该账号的外审数据时段", async () => {
    const puts = renderPage(() => text(200, { success: true, code: "OK", message: "success",
      data: { user_id: "u-1", data_from: "2026-03-01", data_to: "2026-09-30" } }));
    await waitFor(() => expect(screen.getByText("2026-01-01～2026-12-31")).toBeTruthy());

    await fillAndSubmit();

    await waitFor(() => expect(puts).toHaveLength(1));
    expect(puts[0]!.url).toContain("/api/v1/access/users/u-1/audit-data-window");
    expect(puts[0]!.body.data_from).toBe("2026-03-01");
    expect(puts[0]!.body.data_to).toBe("2026-09-30");
    expect(typeof puts[0]!.body.idempotency_key).toBe("string");
    await waitFor(() => expect(screen.getByText("2026-03-01～2026-09-30")).toBeTruthy());
  });

  it("服务端拒绝（起晚于止）→ 页内显示服务端原因", async () => {
    renderPage(() => text(400, { success: false, code: "VALIDATION_FAILED", message: "数据起始日期不能晚于截止日期" }));
    await waitFor(() => expect(screen.getByText("2026-01-01～2026-12-31")).toBeTruthy());

    await fillAndSubmit();

    await waitFor(() => expect(screen.getByTestId("access-users-error").textContent).toContain("数据起始日期不能晚于截止日期"));
  });

  // useAdminResource.loadSecondary「有权限才走 loadSecondary（此时失败=真故障，保留区块提示）」；同页权限模板副资源同范式。
  it("外审数据时段读取失败 → 账号表上方区块提示，时段列不当作未划定显示", async () => {
    renderPage(() => text(200, {}), () => text(500, { success: false, code: "INTERNAL_ERROR", message: "服务暂时不可用" }));

    await waitFor(() => expect(screen.queryByTestId("access-user-audit-window-degraded")?.textContent ?? "").toContain("外审数据时段"));
    expect(screen.getByTestId("access-user-row-u-1").textContent).toContain("读取失败");
  });
});
