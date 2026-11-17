// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorWorkOrdersPage } from "./VendorWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const WORK_ORDERS = [
  { id: "wo-1", code: "WO-1", work_order_type: "repair", priority: "high", status: "pending", title: "空调报修", assignee_id: "u-9", project_id: "p-1" },
  { id: "wo-2", code: "WO-2", work_order_type: "cleaning", priority: "low", status: "completed", title: "保洁完成", assignee_id: "u-9", project_id: "p-1" },
];
const EQUIPMENT = [{ id: "eq-1", name: "中央空调", equipment_type: "facility", status: "normal", project_id: "p-1" }];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/equipment")) return Promise.resolve(envelope(EQUIPMENT));
    if (url.includes("/work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "vendor", accountType: "outsourced_vendor" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorWorkOrdersPage {...props} />);
}

describe("VendorWorkOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/work-orders");
  });

  it("点“待处理” → URL 带 ?kpi=work-orders&subset=pending，聚焦视图只剩工单表且只含未完工行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-orders-kpi-pending")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-work-orders-kpi-pending"));
    expect(window.location.search).toContain("kpi=work-orders");
    expect(window.location.search).toContain("subset=pending");
    expect(screen.getByTestId("vendor-work-orders-kpi-focus-banner").textContent).toContain("正在查看：待处理工单（1）");
    expect(screen.getByTestId("vendor-work-order-select-wo-1")).toBeTruthy();
    expect(screen.queryByTestId("vendor-work-order-select-wo-2")).toBeNull();
    expect(screen.queryByTestId("vendor-work-orders-summary")).toBeNull();
    expect(screen.queryByTestId("vendor-work-order-equipment-table")).toBeNull();
  });

  it("聚焦后点「返回全部」→ 参数清除，恢复 KPI 卡+设备表+全量工单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-orders-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-work-orders-kpi-total"));
    fireEvent.click(screen.getByTestId("vendor-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("vendor-work-orders-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("vendor-work-orders-summary")).toBeTruthy();
    expect(screen.getByTestId("vendor-work-order-select-wo-2")).toBeTruthy();
  });

  it("KPI 子标签与状态列为中文，不含英文枚举/字段名", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-orders-summary")).toBeTruthy());
    const summary = screen.getByTestId("vendor-work-orders-summary").textContent ?? "";
    expect(summary).not.toContain("assignee");
    expect(summary).not.toContain("status");
    const table = screen.getByTestId("vendor-work-orders-table").textContent ?? "";
    expect(table).toContain("报修");
    expect(table).toContain("已完成");
    expect(table).not.toContain("repair");
    expect(table).not.toContain("completed");
  });
});

describe("VendorWorkOrdersPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/work-orders");
  });

  it("点工单行 → 抽屉含该行字段（类型中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-order-row-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-work-order-row-wo-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("WO-1");
    expect(drawer.textContent).toContain("空调报修");
    expect(drawer.textContent).toContain("报修");
    expect(drawer.textContent).not.toContain("repair");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点设备行 → 抽屉含设备字段，状态中文无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-order-equipment-row-eq-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-work-order-equipment-row-eq-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("中央空调");
    expect(drawer.textContent).toContain("正常");
    expect(drawer.textContent).not.toContain("normal");
  });
});


describe("VendorWorkOrdersPage 副资源降级（AUD-032）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/work-orders");
  });

  function forbidden(): Response {
    return new Response(
      JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }),
      { status: 403, headers: { "content-type": "application/json" } },
    );
  }

  it("设备接口 403 → 工单主表照常渲染，设备区块显示无权限降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/equipment")) return Promise.resolve(forbidden());
      if (url.includes("/work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "vendor", accountType: "outsourced_vendor" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<VendorWorkOrdersPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("vendor-work-order-row-wo-1")).toBeTruthy());
    expect(screen.getByTestId("vendor-work-orders-table")).toBeTruthy();
    expect(screen.getByTestId("vendor-work-order-equipment-degraded").textContent).toContain("无权");
    expect(screen.queryByTestId("vendor-work-order-equipment-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(props.setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});

describe("VendorWorkOrdersPage 故障处置规则确认（HP-F01）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/work-orders");
  });

  it("维保单位对待确认规则点「确认」→ POST fault-rules/{id}/confirm；已确认过的不再给按钮", async () => {
    const rules = [
      { id: "ffr-1", project_id: "p-1", equipment_type: "elevator", fault_scenario: "轿厢困人", handling_requirement: "停用并到场", status: "pending_confirmation" },
      { id: "ffr-2", project_id: "p-1", equipment_type: "hvac", fault_scenario: "主机停机", handling_requirement: "到场检查", status: "pending_confirmation", vendor_confirmed_at: "2026-10-21T01:00:00Z" },
    ];
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/fault-rules")) return Promise.resolve(envelope(rules));
      if (url.includes("/work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
      return Promise.resolve(envelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "vendor", accountType: "outsourced_vendor" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<VendorWorkOrdersPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("vendor-fault-rule-confirm-ffr-1")).not.toBeNull());
    expect(screen.queryByTestId("vendor-fault-rule-confirm-ffr-2")).toBeNull();
    fireEvent.click(screen.getByTestId("vendor-fault-rule-confirm-ffr-1"));

    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => String(url).endsWith("/api/v1/facility/fault-rules/ffr-1/confirm")
      && (init as RequestInit | undefined)?.method === "POST")).toBe(true));
  });
});

// 第十八批 AP 段第 229 条 229-R5（PGM:968「……人工只补原因和证据。……」）：处理备注、完成说明不预填；完成说明为空拦下，处理备注空着不发。
describe("VendorWorkOrdersPage 不预填处理备注与完成说明（第 229 条）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("两框初始为空；完成说明为空不发请求；填写后完成，备注空着不带 note", async () => {
    const posts: Array<{ url: string; body: Record<string, unknown> }> = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({ url, body: JSON.parse(String(init?.body)) as Record<string, unknown> });
        return Promise.resolve(envelope([]));
      }
      if (url.includes("/equipment")) return Promise.resolve(envelope(EQUIPMENT));
      if (url.includes("/work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
      return Promise.resolve(envelope([]));
    }));
    render(<VendorWorkOrdersPage api={new AdminApi("test-token")} user={{ id: "u1", username: "vendor", accountType: "outsourced_vendor" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("vendor-work-order-select-wo-1")).toBeTruthy());
    expect((screen.getByTestId("vendor-work-order-note") as HTMLInputElement).value).toBe("");
    expect((screen.getByTestId("vendor-work-order-resolution") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("vendor-work-order-select-wo-1"));
    fireEvent.click(screen.getByTestId("vendor-work-order-complete"));
    expect(screen.getByTestId("vendor-work-order-error").textContent).toBe("完成说明不能为空。");
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("vendor-work-order-resolution"), { target: { value: "更换压缩机电容" } });
    fireEvent.click(screen.getByTestId("vendor-work-order-complete"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/facility/work-orders/wo-1/complete");
    expect(posts[0]!.body).toMatchObject({ resolution: "更换压缩机电容" });
    expect(posts[0]!.body).not.toHaveProperty("note");
  });
});
