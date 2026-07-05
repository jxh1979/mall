import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { IntegrationEventsPage } from "./IntegrationEventsPage";
import { TicketOrdersPage } from "./TicketOrdersPage";
import type {
  AdminApiClient,
  AdminPageProps,
  BarOrderRow,
  EffectivePermissionsState,
  IntegrationEventRow,
  TicketOrderRow,
} from "../types";

const ADMIN_PERMS: EffectivePermissionsState = {
  status: "ready",
  value: { grants: [], denies: [], wildcard: true },
};

// 按 endpoint 分发 mock 数据；未命中的读取返回空数组。
function makeApi(byEndpoint: Record<string, unknown[]>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => byEndpoint[url] ?? []),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function baseProps(api: AdminApiClient, extra: Partial<AdminPageProps> = {}): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
}

describe("票务订单链下钻（P1-B）", () => {
  it("酒吧订单页：点订单号跳 ADM-TKT-003 并带订单 id", async () => {
    const orders: BarOrderRow[] = [
      { id: "bar-ord-1", orderNo: "BAR-0001", tableId: "T-1", status: "billed", totalAmount: 8800 },
    ];
    const navigateTo = vi.fn();
    render(<BarOrdersPage {...baseProps(makeApi({ "/api/v1/bar/orders": orders }), { navigateTo })} />);

    const link = await screen.findByTestId("trace-link-ADM-TKT-003-bar-ord-1");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-TKT-003", "bar-ord-1");
  });

  it("集成事件页：订单同步行的订单号跳 ADM-TKT-003；非订单行降级为纯文本", async () => {
    const orderSync: IntegrationEventRow[] = [
      { id: "sync-1", orderNo: "TKT-9001", syncType: "order", status: "pending" },
    ];
    const retryTask: IntegrationEventRow[] = [
      { id: "retry-1", taskNo: "RT-1", status: "pending" },
    ];
    const navigateTo = vi.fn();
    render(
      <IntegrationEventsPage
        {...baseProps(
          makeApi({
            "/api/v1/integration/order-syncs": orderSync,
            "/api/v1/integration/retry-tasks": retryTask,
          }),
          { navigateTo },
        )}
      />,
    );

    // 订单同步行携带 orderNo -> 可点，落 ADM-TKT-003
    const link = await screen.findByTestId("trace-link-ADM-TKT-003-TKT-9001");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-TKT-003", "TKT-9001");

    // 重试任务行无 orderNo -> 降级纯文本，不产生 trace-link 按钮
    expect(screen.queryByTestId("trace-link-ADM-TKT-003-RT-1")).toBeNull();
    expect(screen.getByText("RT-1")).toBeTruthy();
  });

  it("出票追踪页（目标）：focusObjectId 命中订单 -> 横幅 + 行高亮 + 置顶", async () => {
    // 12 条订单，被定位的排在末尾，验证会被置顶到第 1 页
    const orders: TicketOrderRow[] = Array.from({ length: 12 }, (_, i) => ({
      id: `ord-${i + 1}`,
      orderNo: `TKT-${1000 + i}`,
      payStatus: "paid",
      issueStatus: "not_issued",
    }));
    const focusId = "ord-12";
    render(
      <TicketOrdersPage
        {...baseProps(makeApi({ "/api/v1/ticketing/orders": orders }), { focusObjectId: focusId })}
      />,
    );

    // 横幅出现
    const banner = await screen.findByTestId("ticket-orders-focus");
    expect(banner.textContent).toContain(focusId);

    // 被定位订单置顶到第 1 页并高亮（选中态 radio 勾选）
    await waitFor(() => {
      const radio = screen.getByTestId(`ticket-order-select-${focusId}`) as HTMLInputElement;
      expect(radio.checked).toBe(true);
    });
  });

  it("出票追踪页（目标）：无 focus 时不渲染定位横幅（向后兼容）", async () => {
    const orders: TicketOrderRow[] = [{ id: "ord-1", orderNo: "TKT-1", payStatus: "paid" }];
    render(<TicketOrdersPage {...baseProps(makeApi({ "/api/v1/ticketing/orders": orders }))} />);
    await screen.findByTestId("ticket-orders-table");
    expect(screen.queryByTestId("ticket-orders-focus")).toBeNull();
  });
});
