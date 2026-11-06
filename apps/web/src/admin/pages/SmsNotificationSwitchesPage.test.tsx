// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SmsNotificationSwitchesPage } from "./SmsNotificationSwitchesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function row(capabilityCode: string) {
  return { id: `tcs_${capabilityCode}`, terminalType: "*", capabilityCode, scopeId: "global", enabled: true };
}

function props(rows: ReturnType<typeof row>[]): AdminPageProps {
  const api = {
    get: vi.fn(async () => rows),
    getPaged: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
}

const AT = "2026-11-06T00:00:00Z";

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

describe("短信通知开关页", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("开关行只显示中文通知项，不再显示能力码（第十八批 W 段第 194 条 194-R2，PGM:287）", async () => {
    render(<SmsNotificationSwitchesPage {...props([row("sms_notification"), row("sms.notify.stored_wine.deposit")])} />);
    await waitFor(() => expect(screen.queryByTestId("sms-switch-sms_notification")?.textContent ?? "").toContain("短信通知总开关"));
    expect(screen.getByTestId("sms-switch-sms_notification").textContent).not.toContain("sms_notification");
    expect(screen.getByTestId("sms-switch-sms.notify.stored_wine.deposit").textContent).not.toContain("sms.notify");
    expect(screen.getByTestId("sms-switches-table").textContent).not.toContain("能力码");
  });

  it("订台短信开关行显示中文名（OBS-13-03，BSM 第 1596、1598 行）", async () => {
    render(<SmsNotificationSwitchesPage {...props([
      row("sms.notify.bar_reservation"),
      row("sms.notify.bar_reservation.confirmed"),
      row("sms.notify.bar_reservation.arrival_reminder"),
      row("sms.notify.bar_reservation.timeout_released"),
      row("sms.notify.bar_reservation.cancelled"),
      row("sms.notify.bar_reservation.deposit_forfeited"),
      row("sms.notify.bar_reservation.deposit_refunded"),
    ])} />);
    await waitFor(() => expect(screen.queryByTestId("sms-switch-sms.notify.bar_reservation.confirmed")?.textContent ?? "").toContain("订台成功确认"));
    expect(screen.getByTestId("sms-switch-sms.notify.bar_reservation").textContent).toContain("订台（域开关）");
    expect(screen.getByTestId("sms-switch-sms.notify.bar_reservation.arrival_reminder").textContent).toContain("最晚到店前提醒");
    expect(screen.getByTestId("sms-switch-sms.notify.bar_reservation.timeout_released").textContent).toContain("超时释放台位");
    expect(screen.getByTestId("sms-switch-sms.notify.bar_reservation.cancelled").textContent).toContain("订台取消确认");
    expect(screen.getByTestId("sms-switch-sms.notify.bar_reservation.deposit_forfeited").textContent).toContain("定金没收");
    expect(screen.getByTestId("sms-switch-sms.notify.bar_reservation.deposit_refunded").textContent).toContain("定金原路退回");
  });

  it("已接触发点的短信域与事件开关行显示中文名且可切（PGM 第 742 行缺行即关，须能在后台显式打开或关闭）", async () => {
    const labels: Record<string, string> = {
      "sms.notify.stored_value.pending_refund": "储值待退款提醒",
      "sms.notify.stored_wine.exit_notice": "酒吧退出存酒告知",
      "sms.notify.stored_wine.handover": "存酒移交平台保管告知",
      "sms.notify.service": "服务单（域开关）",
      "sms.notify.service.catalog_fee_changed": "服务项目管理费变更",
      "sms.notify.facility": "设施（域开关）",
      "sms.notify.facility.alarm_takeover": "设施告警接管",
      "sms.notify.ticketing": "票务（域开关）",
      "sms.notify.ticketing.ticket_transfer_created": "门票转赠领取",
      "sms.notify.voucher": "券（域开关）",
      "sms.notify.voucher.voucher_transfer_created": "券转赠领取",
      "sms.notify.bar": "酒吧（域开关）",
      "sms.notify.bar.pending_clear_overdue": "待清台超时提醒",
    };
    render(<SmsNotificationSwitchesPage {...props(Object.keys(labels).map(row))} />);
    await waitFor(() => expect(screen.queryByTestId("sms-switch-sms.notify.facility.alarm_takeover")?.textContent ?? "").toContain("设施告警接管"));
    for (const [code, label] of Object.entries(labels)) {
      expect(screen.getByTestId(`sms-switch-${code}`).textContent).toContain(label);
      expect(screen.getByTestId(`sms-switch-toggle-${code}`)).toBeTruthy();
    }
  });

  it("通票分账方案变更通告的结算域与事件开关行显示中文名且可切（第十八批第 2 条，ENUM 第 52 行档位表变更须提前向受影响的酒吧商户发出通告）", async () => {
    render(<SmsNotificationSwitchesPage {...props([
      row("sms.notify.settlement"),
      row("sms.notify.settlement.distribution_scheme_notice"),
    ])} />);
    await waitFor(() => expect(screen.queryByTestId("sms-switch-sms.notify.settlement.distribution_scheme_notice")?.textContent ?? "")
      .toContain("通票分账方案变更通告"));
    expect(screen.getByTestId("sms-switch-sms.notify.settlement").textContent).toContain("结算（域开关）");
    expect(screen.getByTestId("sms-switch-toggle-sms.notify.settlement.distribution_scheme_notice")).toBeTruthy();
  });

  it("总开关行可见可切；不可关闭的短信显示「开启（不可关闭）」且不给开关（OBS-13-04，BSM 第 1598 行）", async () => {
    render(<SmsNotificationSwitchesPage {...props([
      row("sms_notification"),
      row("sms.notify.stored_value.topup"),
      row("sms.notify.merchant_fee.rent"),
      row("sms.notify.stored_wine.expired"),
      row("sms.notify.merchant_fee.rent_dunning"),
    ])} />);
    await waitFor(() => expect(screen.queryByTestId("sms-switch-sms_notification")).not.toBeNull());
    expect(screen.getByTestId("sms-switch-toggle-sms_notification")).toBeTruthy();
    for (const code of ["sms.notify.stored_value.topup", "sms.notify.merchant_fee.rent"]) {
      expect(screen.getByTestId(`sms-switch-state-${code}`).textContent).toBe("开启（不可关闭）");
      expect(screen.queryByTestId(`sms-switch-toggle-${code}`)).toBeNull();
    }
    expect(screen.getByTestId("sms-switch-sms.notify.merchant_fee.rent").textContent).toContain("房租账单");
    expect(screen.getByTestId("sms-switch-toggle-sms.notify.stored_wine.expired")).toBeTruthy();
    expect(screen.getByTestId("sms-switch-sms.notify.merchant_fee.rent_dunning").textContent).toContain("房租催缴");
    expect(screen.getByTestId("sms-switch-toggle-sms.notify.merchant_fee.rent_dunning")).toBeTruthy();
  });

  it("切开关把本次操作说明以 operator_note 提交（第十八批 W 段第 194 条同根，同终端开关矩阵页 194-R4；PGM:908 终端开关启用、停用须留痕）", async () => {
    const item = {
      id: "tcs_sms_stored_wine_deposit", terminal_type: "*", capability_code: "sms.notify.stored_wine.deposit",
      scope_type: "global", scope_id: "global", enabled: true, reason: "存酒成功短信", created_at: AT, updated_at: AT,
    };
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) =>
      Promise.resolve(envelope(init?.method === "PUT"
        ? { ...item, enabled: false }
        : { items: [item], page: 1, page_size: 50, total: 1 })));
    vi.stubGlobal("fetch", fetchMock);
    render(<SmsNotificationSwitchesPage {...props([])} api={new AdminApi()} />);
    await waitFor(() => expect(screen.queryByTestId("sms-switch-state-sms.notify.stored_wine.deposit")?.textContent).toBe("开启"));
    fireEvent.click(screen.getByTestId("sms-switch-toggle-sms.notify.stored_wine.deposit"));
    await waitFor(() => expect(puts(fetchMock)).toHaveLength(1));
    expect(puts(fetchMock)[0]).toMatchObject({ enabled: false, operator_note: "后台关闭：存酒成功" });
  });
});
