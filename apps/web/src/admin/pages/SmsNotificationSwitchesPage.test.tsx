// 文件版本：V0.01
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
