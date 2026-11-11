// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationEventsPage } from "./IntegrationEventsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, IntegrationEventRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const RETRY: IntegrationEventRow[] = [
  { id: "retry-1", taskNo: "RT-1", providerCode: "prov-a", eventType: "webhook", status: "pending", retryCount: 2, updatedAt: "2026-07-05T13:20:00Z" },
];
const COMPENSATION: IntegrationEventRow[] = [{ id: "comp-1", taskNo: "CT-1", providerCode: "prov-b", status: "pending" }];
const ORDER_SYNC: IntegrationEventRow[] = [{ id: "sync-1", orderNo: "TKT-9001", syncType: "order", status: "matched" }];
const VOUCHER: IntegrationEventRow[] = [{ id: "vv-1", verificationNo: "VV-1", status: "processed" }];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("retry-tasks")) return RETRY;
      if (url.includes("compensation-tasks")) return COMPENSATION;
      if (url.includes("order-syncs")) return ORDER_SYNC;
      if (url.includes("voucher-verifications")) return VOUCHER;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<IntegrationEventsPage {...props} />);
}

describe("IntegrationEventsPage 行明细抽屉", () => {
  it("停售拒收回流显示原路待退编号并能前往财务核实", async () => {
    const api = makeApi();
    vi.mocked(api.get).mockImplementation(async (url) => url.includes("order-syncs")
      ? [{ id: "blocked-sync", status: "failed", providerPayload: { refundId: "refund-blocked", rejectionCode: "PARK_WIDE_FAULT_SALES_SUSPENDED" } }] : []);
    const navigateTo = vi.fn();
    renderPage({ api, navigateTo });
    fireEvent.click(await screen.findByTestId("integration-event-row-blocked-sync"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("订单未受理且不出票");
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("refund-blocked");
    fireEvent.click(screen.getByRole("button", { name: "前往财务台账查看退款实际状态" }));
    expect(navigateTo).toHaveBeenCalledWith("ADM-FIN-001");
  });
  it("零金额停售拒收只显示拒收结果，不造退款待办", async () => {
    const api = makeApi();
    vi.mocked(api.get).mockImplementation(async (url) => url.includes("order-syncs")
      ? [{ id: "zero-blocked", status: "failed", providerPayload: { rejectionCode: "PARK_WIDE_FAULT_SALES_SUSPENDED" } }] : []);
    renderPage({ api });
    fireEvent.click(await screen.findByTestId("integration-event-row-zero-blocked"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("订单未受理且不出票");
    expect(screen.queryByRole("button", { name: "前往财务台账查看退款实际状态" })).toBeNull();
  });
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/integrations/events");
  });

  it("点事件行 → 抽屉含该行值（状态中文·时间中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-event-row-retry-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-event-row-retry-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("RT-1");
    expect(drawer.textContent).toContain("重试任务");
    expect(drawer.textContent).toContain("待处理");
    expect(drawer.textContent).toContain("2026年7月5日 13:20");
    // 抽屉零英文枚举：状态码不得原文出现
    expect(drawer.textContent).not.toContain("pending");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("表格更新时间中文化，不出现 ISO 时间戳原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-events-table")).toBeTruthy());
    const table = screen.getByTestId("integration-events-table").textContent ?? "";
    expect(table).toContain("2026年7月5日 13:20");
    expect(table).not.toContain("2026-07-05T13:20:00Z");
  });

  it("点行内任务号 TraceLink 不触发抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-event-row-retry-1")).toBeTruthy());
    const cell = screen.getByTestId("integration-event-row-retry-1").querySelector("td:nth-child(2)") as HTMLElement;
    fireEvent.click(cell);
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

// 第十八批 AF 段 198-R5：立即重试入口只读重试原因与附件，不读 action；契约删字段，网页停发写死的 "run_now"。
describe("IntegrationEventsPage 立即重试请求体", () => {
  afterEach(() => vi.restoreAllMocks());

  it("立即重试只发重试原因，不带写死的 action", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage({ api });
    await waitFor(() => expect(screen.getByTestId("integration-event-row-retry-1")).toBeTruthy());
    fireEvent.change(screen.getByTestId("integration-events-retry-reason"), { target: { value: "供应商超时后人工触发重试" } });

    fireEvent.click(screen.getByTestId("integration-events-run-retry"));

    const runNow = "/api/v1/integration/retry-tasks/retry-1/run-now";
    await waitFor(() => expect(vi.mocked(api.post).mock.calls.some(([path]) => path === runNow)).toBe(true));
    const body = vi.mocked(api.post).mock.calls.find(([path]) => path === runNow)?.[1];
    expect(body).toMatchObject({ reason: "供应商超时后人工触发重试" });
    expect(body).not.toHaveProperty("action");
  });
});

// 第十八批 AG 段 255-R3：补偿任务处置的操作人由服务端取登录人，网页停发 operator_user_id。
describe("IntegrationEventsPage 处理补偿请求体", () => {
  afterEach(() => vi.restoreAllMocks());

  it("处理补偿不带 operator_user_id", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage({ api });
    await waitFor(() => expect(screen.getByTestId("integration-event-row-comp-1")).toBeTruthy());

    fireEvent.click(screen.getByTestId("integration-events-resolve-compensation"));

    const resolve = "/api/v1/integration/compensation-tasks/comp-1/resolve";
    await waitFor(() => expect(vi.mocked(api.post).mock.calls.some(([path]) => path === resolve)).toBe(true));
    const body = vi.mocked(api.post).mock.calls.find(([path]) => path === resolve)?.[1];
    expect(body).toMatchObject({ status: "resolved", resolution_note: "已核对订单和券核销回流，处理补偿任务" });
    expect(body).not.toHaveProperty("operator_user_id");
  });
});
