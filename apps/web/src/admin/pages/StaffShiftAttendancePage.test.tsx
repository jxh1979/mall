import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const SHIFTS = [
  { id: "sh-1", user_id: "staff-1", start_at: "2026-07-05T09:00:00Z", end_at: "2026-07-05T18:00:00Z", point_space_ids: ["sp-a", "sp-b"], note: "大厅" },
];
const ATTENDANCE = [
  { id: "at-1", user_id: "staff-1", punch_type: "check_in", punched_at: "2026-07-05T09:10:00Z", method: "nfc", judgement: "late", space_id: "sp-a", device_fingerprint: "fp-xyz" },
];

function listEnvelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/staff/shifts")) return Promise.resolve(listEnvelope(SHIFTS));
    if (url.includes("/staff/attendance")) return Promise.resolve(listEnvelope(ATTENDANCE));
    return Promise.resolve(listEnvelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StaffShiftAttendancePage {...props} />);
}

describe("StaffShiftAttendancePage 行明细抽屉（整行可点·枚举中文）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/staff/shifts");
  });

  it("点排班行 → 抽屉含员工+点位+备注", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-row-sh-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-row-sh-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("staff-1");
    expect(text).toContain("sp-a、sp-b");
    expect(text).toContain("大厅");
  });

  it("点考勤行 → 抽屉含中文类型/方式/判定，无英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("attendance-row-at-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("attendance-row-at-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("上班"); // check_in
    expect(text).toContain("NFC"); // nfc method label
    expect(text).toContain("迟到"); // late judgement
    expect(text).not.toContain("check_in");
    expect(text).not.toContain("late");
  });

  it("考勤表内类型/方式/判定列为中文，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("attendance-table")).toBeTruthy());
    const table = screen.getByTestId("attendance-table").textContent ?? "";
    expect(table).toContain("上班");
    expect(table).toContain("迟到");
    expect(table).not.toContain("check_in");
    expect(table).not.toContain("late");
  });

  it("点排班行 → Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-row-sh-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-row-sh-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("日期控件为中文 CnDatePicker，无原生 type=date / mm-dd-yyyy 残留", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-date")).toBeTruthy());
    const trigger = screen.getByTestId("shift-date");
    // 组件渲染为按钮触发器（非原生 input）
    expect(trigger.tagName).toBe("BUTTON");
    // 显示中文日期，且不含英文/斜杠格式
    expect(trigger.textContent ?? "").toMatch(/年.*月.*日/);
    expect(trigger.textContent ?? "").not.toMatch(/[A-Za-z]/);
    // 全页无原生 type="date"
    expect(document.querySelector('input[type="date"]')).toBeNull();
    expect(document.querySelector('input[type="month"]')).toBeNull();
  });
});
