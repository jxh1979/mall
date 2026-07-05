import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 各端点返回一行样本，覆盖全部 8 张业务表；状态/枚举/日期均取英文原文与 ISO 串，
// 断言渲染后为中文（statusBadge/zh）与 cnDate，抽屉含该行值。
const EQUIPMENT = [{ id: "eq-1", name: "1号客梯", equipmentType: "elevator", status: "alarm", brand: "三菱", responsibleUserId: "u-eng", spaceId: "sp-1", maintenanceCycleDays: 90, nextMaintenanceDue: "2026-08-01", alarmSource: "iot", alarmNote: "超速" }];
const MAINT_DUE = [{ id: "eq-2", name: "2号货梯", equipmentType: "elevator", status: "normal", responsibleUserId: "u-eng2", nextMaintenanceDue: "2026-07-20" }];
const WORK_ORDERS = [{ id: "wo-1", code: "WO-0001", workOrderType: "repair", priority: "urgent", status: "processing", title: "客梯异响", equipmentId: "eq-1", assigneeId: "u-fix", satisfaction: 5, overtime: false }];
const OVERDUE = [{ id: "wo-2", code: "WO-0002", workOrderType: "cleaning", priority: "normal", status: "assigned", title: "大堂保洁", assigneeId: "u-clean" }];
const TEMPLATES = [{ id: "tpl-1", name: "消防巡检模板", inspectionType: "fire", cycleDays: 7, abnormalMode: "to_work_order", active: true }];
const TASKS = [{ id: "task-1", templateId: "tpl-1", inspectionType: "fire", status: "pending", title: "月度消防巡检", scheduleMode: "template", assigneeId: "u-insp", scheduledDate: "2026-07-10", dueDate: "2026-07-15", workOrderId: null }];
const RECORDS = [{ id: "rec-1", taskId: "task-1", result: "abnormal", inspectorId: "u-insp", abnormalNote: "灭火器过期", recordedAt: "2026-07-11T09:30:00Z" }];

function makeApi(): AdminApiClient {
  const get = vi.fn((url: string) => {
    if (url.includes("/maintenance-due")) return Promise.resolve(MAINT_DUE);
    if (url.includes("/work-orders/overdue")) return Promise.resolve(OVERDUE);
    if (url.includes("/work-orders")) return Promise.resolve(WORK_ORDERS);
    if (url.includes("/equipment")) return Promise.resolve(EQUIPMENT);
    if (url.includes("/inspections/templates")) return Promise.resolve(TEMPLATES);
    if (url.includes("/inspections/tasks/") && url.includes("/records")) return Promise.resolve(RECORDS);
    if (url.includes("/inspections/tasks")) return Promise.resolve(TASKS);
    return Promise.resolve([]);
  });
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

function drawer() {
  return screen.getByTestId("row-detail-drawer");
}

describe("FacilityWorkbenchPage 整行明细抽屉 + 零英文", () => {
  it("设备台账整行可点 → 抽屉含该设备中文枚举与中文日期", async () => {
    renderPage();
    const row = await screen.findByTestId("facility-equipment-eq-1");
    // 表格内枚举/日期已中文化（无英文原文）。
    expect(row.textContent).toContain("电梯扶梯"); // equipmentType=elevator → 中文
    expect(row.textContent).toContain("告警"); // status=alarm → 中文
    expect(row.textContent).not.toContain("alarm");
    expect(row.textContent).not.toContain("elevator");
    expect(row.textContent).not.toContain("2026-08-01"); // ISO 原文不出现
    expect(row.textContent).toContain("2026年8月1日"); // cnDate
    fireEvent.click(row);
    const d = drawer();
    expect(within(d).getByText("设备明细 · 1号客梯")).toBeTruthy();
    expect(within(d).getByText("告警")).toBeTruthy();
    expect(within(d).getByText("2026年8月1日")).toBeTruthy();
    expect(within(d).getByText("物联网")).toBeTruthy(); // alarmSource=iot
    expect(d.textContent).not.toContain("elevator");
    expect(d.textContent).not.toContain("alarm");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    fireEvent.click(await screen.findByTestId("facility-equipment-eq-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("行内履约人 TraceLink 点击不误弹抽屉（stopPropagation）", async () => {
    const navigateTo = vi.fn();
    renderPage(navigateTo);
    await screen.findByTestId("facility-work-order-wo-1");
    fireEvent.click(screen.getByTestId("trace-link-ADM-ACC-001-u-fix"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ACC-001", "u-fix");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull(); // 未弹抽屉
  });

  it("工单表整行可点 → 抽屉含工单中文状态", async () => {
    renderPage();
    const row = await screen.findByTestId("facility-work-order-wo-1");
    expect(row.textContent).toContain("处理中"); // processing
    expect(row.textContent).not.toContain("processing");
    expect(row.textContent).not.toContain("repair");
    fireEvent.click(row);
    expect(within(drawer()).getByText("工单明细 · WO-0001")).toBeTruthy();
    expect(within(drawer()).getByText("处理中")).toBeTruthy();
  });

  it("维保到期/逾期工单/SLA/巡检模板/任务四表整行可点 → 抽屉", async () => {
    renderPage();
    // 维保到期
    fireEvent.click(await screen.findByTestId("facility-maintenance-due-eq-2"));
    expect(within(drawer()).getByText("维保到期明细 · 2号货梯")).toBeTruthy();
    expect(within(drawer()).getByText("2026年7月20日")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    // 逾期工单
    fireEvent.click(await screen.findByTestId("facility-work-order-overdue-wo-2"));
    expect(within(drawer()).getByText("逾期工单明细 · WO-0002")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    // SLA 策略（默认行）
    fireEvent.click(await screen.findByTestId("facility-work-order-policy-repair-urgent"));
    expect(within(drawer()).getByText(/SLA 策略明细/)).toBeTruthy();
    expect(within(drawer()).getByText("30 分钟")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    // 巡检模板
    fireEvent.click(await screen.findByTestId("facility-inspection-template-tpl-1"));
    expect(within(drawer()).getByText("巡检模板明细 · 消防巡检模板")).toBeTruthy();
    expect(within(drawer()).getByText("转工单")).toBeTruthy(); // abnormalMode=to_work_order
  });

  it("巡检任务整行可点 → 抽屉含中文排程/日期；巡检记录表整行可点", async () => {
    renderPage();
    const task = await screen.findByTestId("facility-inspection-task-task-1");
    expect(task.textContent).toContain("按模板"); // scheduleMode=template
    expect(task.textContent).not.toContain("template");
    expect(task.textContent).not.toContain("2026-07-15");
    expect(task.textContent).toContain("2026年7月15日");
    fireEvent.click(task);
    expect(within(drawer()).getByText("巡检任务明细 · 月度消防巡检")).toBeTruthy();
    expect(within(drawer()).getByText("待巡检")).toBeTruthy(); // status=pending
    fireEvent.keyDown(window, { key: "Escape" });
    // 先点"记录"按钮加载巡检记录，再点记录行。
    fireEvent.click(screen.getByTestId("facility-inspection-select-task-task-1"));
    const rec = await screen.findByTestId("facility-inspection-record-rec-1");
    expect(rec.textContent).toContain("异常"); // result=abnormal
    expect(rec.textContent).not.toContain("abnormal");
    fireEvent.click(rec);
    expect(within(drawer()).getByText("巡检记录明细 · rec-1")).toBeTruthy();
    expect(within(drawer()).getByText("2026年7月11日 09:30")).toBeTruthy();
  });
});
