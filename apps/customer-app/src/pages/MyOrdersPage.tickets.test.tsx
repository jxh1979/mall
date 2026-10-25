// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { MyOrdersPage } from "./MyOrdersPage";

// BSM「查看我的订单、我的票、我的权益和使用记录」；BSM「连接员工工作手机核销，核销后客人能看到使用记录」；
// BSM「第三方团购（美团/大众点评/抖音）购买的票按渠道来源在“我的订单/我的票”统一展示，渠道未接入前相关入口置灰、不展示伪造数据」；
// BSM「发起转赠后不冻结该权益」。
const ORDERS = [{ id: "order-1", orderNo: "T1", ticketTypeId: "tt-1", payStatus: "paid", issueStatus: "issued", status: "paid",
  salesChannel: "app", amountCent: 8800, quantity: 1 }];
const TICKET = { id: "e-1", ticketName: "夜场单人票", entitlementType: "entry", status: "active", totalTimes: 1, usedTimes: 0,
  remainingTimes: 1, numPeople: 1, validFrom: null, validTo: "2026-10-30T00:00:00Z", parentTicketId: "pt-1", slotIndex: 1,
  ticketNo: "TK-1", transferState: null, compensationKind: null };
const CHANNELS_OFF = [{ channel: "meituan", name: "美团", enabled: false }, { channel: "dianping", name: "大众点评", enabled: false },
  { channel: "douyin", name: "抖音", enabled: false }];

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listOrders: vi.fn().mockResolvedValue(ORDERS),
    listMyTickets: vi.fn().mockResolvedValue([TICKET]),
    listTicketChannels: vi.fn().mockResolvedValue(CHANNELS_OFF),
    listTicketUsages: vi.fn().mockResolvedValue([{ result: "success", resultLabel: "成功", verifiedAt: "2026-10-24T12:00:00Z", merchantName: "一号酒吧" }]),
    getTicketQr: vi.fn().mockReturnValue(new Promise(() => undefined)),
    ...overrides,
  };
}

function renderPage(service: ReturnType<typeof makeService>) {
  render(<MyOrdersPage service={service as unknown as CustomerService} />);
}

describe("MyOrdersPage 我的票、使用记录与渠道置灰", () => {
  it("ticketUsageRecordsShown：我的票按本人持有展示，每张票可看使用记录", async () => {
    const service = makeService();
    renderPage(service);
    await waitFor(() => expect(screen.queryByTestId("ticket-item")?.textContent ?? "").toContain("夜场单人票"));
    fireEvent.click(screen.getByTestId("ticket-usages-open"));
    await waitFor(() => expect(service.listTicketUsages).toHaveBeenCalledWith("e-1"));
    await waitFor(() => expect(screen.queryByTestId("ticket-usage-item")?.textContent ?? "").toContain("成功"));
    expect(screen.getByTestId("ticket-usage-item").textContent).toContain("一号酒吧");
  });

  it("pendingTransferTicketStillShowsCode：转赠待领取的票不冻结，照常可出示动态码", async () => {
    const service = makeService({ listMyTickets: vi.fn().mockResolvedValue([{ ...TICKET, transferState: "transferring" }]) });
    renderPage(service);
    await waitFor(() => expect(screen.queryByTestId("ticket-item")?.textContent ?? "").toContain("转赠待领取"));
    expect(screen.getByTestId("ticket-qr-open")).not.toBeDisabled();
    fireEvent.click(screen.getByTestId("ticket-qr-open"));
    await waitFor(() => expect(service.getTicketQr).toHaveBeenCalledWith("e-1"));
  });

  it("channelOrdersEntryGreyedWhenNotConnected：第三方渠道未接入时入口置灰、不展示样例", async () => {
    const service = makeService();
    renderPage(service);
    await waitFor(() => expect(screen.queryByTestId("ticket-channel-meituan")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("ticket-channel-meituan")).toBeDisabled());
    expect(screen.getByTestId("ticket-channel-meituan").textContent).toContain("美团（渠道未接入）");
    expect(screen.getByTestId("ticket-channel-dianping")).toBeDisabled();
    expect(screen.getByTestId("ticket-channel-douyin")).toBeDisabled();
    expect(screen.getAllByTestId("order-item")).toHaveLength(1);
  });
});
