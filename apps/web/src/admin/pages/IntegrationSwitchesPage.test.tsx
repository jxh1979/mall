// 文件版本：V0.01
// 第十八批 W 段第 194 条同根（控制席并入，同终端开关矩阵页 194-R4）：第三方开关页切开关把本次操作说明以 operator_note 提交，
// 后端写进该行说明（PGM:908「终端开关启用、停用、灰度开放和被拒绝访问。」须留痕）；修复前请求体不带说明，该行说明被写成空值。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationSwitchesPage } from "./IntegrationSwitchesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const AT = "2026-11-06T00:00:00Z";
const ITEM = {
  id: "tcs_int_payment_off", terminal_type: "*", capability_code: "integration.payment.sync", scope_type: "global", scope_id: "global",
  enabled: false, reason: "第三方默认关闭", created_at: AT, updated_at: AT,
};
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), {
    status: 200, headers: { "content-type": "application/json" },
  });
}

function puts(fetchMock: ReturnType<typeof vi.fn>): Record<string, unknown>[] {
  return fetchMock.mock.calls
    .filter(([, init]) => (init as RequestInit | undefined)?.method === "PUT")
    .map(([, init]) => JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>);
}

describe("第三方开关页", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("切开关把本次操作说明以 operator_note 提交", async () => {
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) =>
      Promise.resolve(envelope(init?.method === "PUT"
        ? { ...ITEM, enabled: true }
        : { items: [ITEM], page: 1, page_size: 50, total: 1 })));
    vi.stubGlobal("fetch", fetchMock);
    const props: AdminPageProps = {
      api: new AdminApi(),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<IntegrationSwitchesPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("switch-state-integration.payment.sync")?.textContent).toBe("关闭"));
    fireEvent.click(screen.getByTestId("switch-toggle-integration.payment.sync"));
    await waitFor(() => expect(puts(fetchMock)).toHaveLength(1));
    expect(puts(fetchMock)[0]).toMatchObject({ enabled: true, operator_note: "后台打开：聚合支付回流/对账" });
  });
});
