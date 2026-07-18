import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessUsersPage } from "./AccessUsersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// AUD-033：账号绑定外部协作机构——选账号+选机构→POST /partner/orgs/{id}/bind-user。
const USERS = [
  { id: "u-1", username: "外包甲管理员", account_type: "outsourced_vendor", status: "active", organization_id: "org-1" },
  { id: "u-2", username: "平台管理员乙", account_type: "platform_admin", status: "active", organization_id: "org-1" },
];
const ORGS = [
  { id: "epo-1", org_name: "维保甲公司", status: "active" },
  { id: "epo-2", org_name: "已停用机构", status: "suspended" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("AccessUsersPage 账号绑定外部协作机构", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("选账号与active机构提交 → 调 bind-user 端点;停用机构不出现在下拉", async () => {
    const calls: Array<{ url: string; body: unknown }> = [];
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (init?.method === "POST" && url.includes("/bind-user")) {
          calls.push({ url, body: JSON.parse(String(init.body)) });
          return Promise.resolve(new Response(
            JSON.stringify({ success: true, code: "OK", message: "success", data: null }),
            { status: 200, headers: { "content-type": "application/json" } },
          ));
        }
        if (url.includes("partner/orgs")) return Promise.resolve(envelope(ORGS));
        if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
        return Promise.resolve(envelope([]));
      }),
    );
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AccessUsersPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("partner-bind-user")).toBeTruthy());
    // 复核 Important:非外包类账号不得出现在绑定下拉(误绑平台账号=外部方可停用之)。
    const userSelect = screen.getByTestId("partner-bind-user") as HTMLSelectElement;
    await waitFor(() => expect(userSelect.options.length).toBe(2)); // 空选项 + 外包账号
    expect(Array.from(userSelect.options).map((o) => o.textContent ?? "").join("|")).not.toContain("平台管理员乙");
    const orgSelect = screen.getByTestId("partner-bind-org") as HTMLSelectElement;
    await waitFor(() => expect(orgSelect.options.length).toBe(2)); // 空选项 + active 机构;停用机构被过滤
    expect(Array.from(orgSelect.options).map((o) => o.textContent)).not.toContain("已停用机构");

    fireEvent.change(screen.getByTestId("partner-bind-user"), { target: { value: "u-1" } });
    fireEvent.change(orgSelect, { target: { value: "epo-1" } });
    fireEvent.click(screen.getByTestId("partner-bind-submit"));

    await waitFor(() => expect(calls).toHaveLength(1));
    const bindCall = calls[0]!;
    expect(bindCall.url).toContain("/api/v1/partner/orgs/epo-1/bind-user");
    expect((bindCall.body as Record<string, unknown>).user_id).toBe("u-1");
  });
});
