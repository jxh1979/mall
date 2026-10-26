// 文件版本：V0.02
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { SmsNotificationSwitchesPage } from "./SmsNotificationSwitchesPage";
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

describe("短信通知开关页", () => {
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
});
