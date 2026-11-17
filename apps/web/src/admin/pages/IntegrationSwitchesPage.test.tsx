// 文件版本：V0.02
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

  // 第十八批 AN 段第 228 条 228-R1（PGM:287「……提示……必须全部为中文……」；ENUM:79「……不使用英文缩写代替」）：
  // 关闭口径、开启要求、接入状态与页头说明不再夹英文；「餐饮 POS」「零售 POS」「线下 POS」是 HTML 自身用词（ENUM:208、BSM:990），保留。
  it("九个第三方开关的关闭口径、开启要求、接入状态与页头说明全中文", async () => {
    const codes = [
      "face.verification.enabled", "integration.face_age.sync", "integration.payment.sync", "integration.ticket_channel.sync", "integration.wecom.sync",
      "integration.catering_pos.sync", "integration.retail_pos.sync", "integration.robot.sync", "integration.video.sync",
    ];
    const items = codes.map((code, index) => ({ ...ITEM, id: `tcs_${index}`, capability_code: code }));
    vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope({ items, page: 1, page_size: 50, total: items.length }))));
    render(<IntegrationSwitchesPage api={new AdminApi()} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => {
      const table = screen.getByTestId("integration-switches-table").textContent ?? "";
      for (const text of ["开发包", "凭证引用", "商品规格", "回调/轮询", "判龄失败一律拒绝", "人工填写的证据链接"]) expect(table).toContain(text);
    });
    expect(screen.getAllByTestId(/^switch-row-/)).toHaveLength(9);
    expect((screen.getByTestId("integration-switches-page").textContent ?? "").replace(/POS/g, "")).not.toMatch(/[A-Za-z]/);
  });
});
