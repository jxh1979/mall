// 文件版本：V0.01
// 第十八批 W 段第 194 条（PGM:287「……展示给用户的一切文字，包括……字段名、状态、枚举取值……必须全部为中文……
// 可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。」）：
// 194-R2 能力列显示中文名（权限类取接口带回的 capability_name，短信、第三方开关取各自目录，其余取本页小表），范围列非全局显示「范围中文名：编号」，
// 灰度试点的能力改为下拉（显示中文名、提交原码）；194-R4 切开关时本次操作说明以 operatorNote 提交（后端只认 operator_note，写进该行说明）。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TerminalSwitchMatrixPage } from "./TerminalSwitchMatrixPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const AT = "2026-11-06T00:00:00Z";
function row(id: string, terminalType: string, code: string, name: string | null, scopeType = "global", scopeId = "global") {
  return {
    id, terminal_type: terminalType, capability_code: code, capability_name: name, scope_type: scopeType, scope_id: scopeId,
    enabled: true, reason: "说明", created_at: AT, updated_at: AT,
  };
}
const ROWS = [
  row("tcs_term_web", "web", "terminal.enabled", null),
  row("tcs_optin_bar.reservation.create", "*", "bar.reservation.create", "创建酒吧预约"),
  row("tcs_sms_notification_off", "*", "sms_notification", null),
  row("tcs_mdm_managed_work_devices_off", "*", "managed_work_devices", null),
  row("tcs_kiosk_ticketing_KIOSK-001", "kiosk", "ticketing", null, "device", "KIOSK-001"),
];
const CANARY = [{ rollout: { id: "tcr_1", terminal_type: "*", capability_code: "bar.reservation.create", active: true, reason: "试点" }, targets: [] }];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), {
    status: 200, headers: { "content-type": "application/json" },
  });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/canary-rollouts")) return Promise.resolve(envelope({ items: CANARY, page: 1, page_size: 50, total: 1 }));
    if (url.includes("/capability-switches")) return Promise.resolve(envelope({ items: ROWS, page: 1, page_size: 50, total: ROWS.length }));
    return Promise.resolve(envelope({}));
  });
  vi.stubGlobal("fetch", fetchMock);
  const props: AdminPageProps = {
    api: new AdminApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<TerminalSwitchMatrixPage {...props} />);
  return { fetchMock, props };
}

function writes(fetchMock: ReturnType<typeof vi.fn>, method: string, path: string): Record<string, unknown>[] {
  return fetchMock.mock.calls
    .filter(([input, init]) => String(input).includes(path) && (init as RequestInit | undefined)?.method === method)
    .map(([, init]) => JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>);
}

describe("终端开关矩阵页中文显示（第 194 条）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("能力列显示中文名，售票开关范围列为「设备：设备号」，能力与范围两列除设备号外没有英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("terminal-capability-name-tcs_optin_bar.reservation.create")?.textContent).toBe("创建酒吧预约"));
    expect(screen.getByTestId("terminal-capability-name-tcs_sms_notification_off").textContent).toBe("短信通知总开关");
    expect(screen.getByTestId("terminal-capability-name-tcs_mdm_managed_work_devices_off").textContent).toBe("工作手机/平板/一体机设备管控");
    expect(screen.getByTestId("terminal-capability-name-tcs_kiosk_ticketing_KIOSK-001").textContent).toBe("售票能力");
    expect(screen.getByTestId("terminal-capability-scope-tcs_kiosk_ticketing_KIOSK-001").textContent).toBe("设备：KIOSK-001");
    for (const item of ROWS.filter((r) => r.capability_code !== "terminal.enabled")) {
      const text = `${screen.getByTestId(`terminal-capability-name-${item.id}`).textContent}${
        screen.getByTestId(`terminal-capability-scope-${item.id}`).textContent}`.replace(item.scope_id, "");
      expect(text).not.toMatch(/[A-Za-z]{2}/);
    }
    expect(screen.getByTestId("terminal-canary-rollout-tcr_1").querySelector("h3")?.textContent).toContain("创建酒吧预约");
    expect(screen.getByTestId("terminal-canary-rollout-tcr_1").querySelector("h3")?.textContent).not.toContain("bar.reservation.create");
  });

  it("灰度试点的能力下拉显示中文名，选中后提交原能力码", async () => {
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.queryByTestId("terminal-capability-name-tcs_optin_bar.reservation.create")?.textContent).toBe("创建酒吧预约"));
    const select = screen.getByTestId("terminal-canary-capability-code") as HTMLSelectElement;
    const labels = [...select.options].filter((option) => option.value).map((option) => option.textContent ?? "");
    expect(labels).toContain("创建酒吧预约");
    expect(labels).toContain("售票能力");
    for (const label of labels) expect(label).not.toMatch(/[A-Za-z]{2}/);
    fireEvent.change(select, { target: { value: "bar.reservation.create" } });
    fireEvent.change(screen.getByTestId("terminal-canary-rollout-reason"), { target: { value: "先在一家酒吧试点" } });
    fireEvent.click(screen.getByTestId("terminal-canary-rollout-open"));
    await waitFor(() => expect(writes(fetchMock, "PUT", "/api/v1/terminals/canary-rollouts")).toHaveLength(1));
    expect(writes(fetchMock, "PUT", "/api/v1/terminals/canary-rollouts")[0]).toMatchObject({
      capability_code: "bar.reservation.create", active: true, reason: "先在一家酒吧试点",
    });
  });

  it("切开关把本次操作说明以 operator_note 提交（不再发后端不认的 reason）", async () => {
    const { fetchMock } = renderPage();
    await waitFor(() => expect(screen.queryByTestId("terminal-capability-name-tcs_kiosk_ticketing_KIOSK-001")?.textContent).toBe("售票能力"));
    fireEvent.click(screen.getByTestId("terminal-capability-toggle-tcs_kiosk_ticketing_KIOSK-001"));
    await waitFor(() => expect(writes(fetchMock, "PUT", "/api/v1/terminals/capability-switches/tcs_kiosk_ticketing_KIOSK-001")).toHaveLength(1));
    const body = writes(fetchMock, "PUT", "/api/v1/terminals/capability-switches/tcs_kiosk_ticketing_KIOSK-001")[0]!;
    expect(body).toMatchObject({ enabled: false, operator_note: "后台关闭：触摸屏一体机 / 售票能力" });
    expect(body).not.toHaveProperty("reason");
  });
});
