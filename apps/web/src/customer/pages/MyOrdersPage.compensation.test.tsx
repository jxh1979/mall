// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { MyOrdersPage } from "./MyOrdersPage";
import type { CustomerService } from "../client/customerService";

// TK-13（ENUM L148「“我的票”按对应人份或次份显示补偿标注及其实际有效期，不额外增加专用通知渠道」；BSM L862）：
// 完整补发（full_reissue）的补偿行按人份单列补偿标注与该人份实际补偿有效期，不并入原票「剩余 x/y 次」。
const ORIGINAL_VALID_TO = "2026-10-25T00:00:00Z";
const COMPENSATION_VALID_TO = "2027-10-21T00:00:00Z";
const base = { ticketName: "双人通票", numPeople: 1, validFrom: "2026-10-01T00:00:00Z", parentTicketId: "tkt-1", ticketNo: "T-1" };
const TICKETS = [
  { ...base, id: "e-a1", entitlementType: "amusement_pass", status: "used", totalTimes: 1, usedTimes: 1, remainingTimes: 0, validTo: ORIGINAL_VALID_TO, slotIndex: 1 },
  { ...base, id: "e-a2", entitlementType: "amusement_pass", status: "active", totalTimes: 1, usedTimes: 0, remainingTimes: 1, validTo: ORIGINAL_VALID_TO, slotIndex: 2 },
  { ...base, id: "e-c1", entitlementType: "amusement_pass", status: "active", totalTimes: 1, usedTimes: 0, remainingTimes: 1, validTo: COMPENSATION_VALID_TO, slotIndex: 1, compensationKind: "full_reissue" },
  { ...base, id: "e-c2", entitlementType: "bar_drink", status: "active", totalTimes: 2, usedTimes: 0, remainingTimes: 2, validTo: COMPENSATION_VALID_TO, slotIndex: 1, compensationKind: "full_reissue" },
];

it("TK-13 完整补发按人份显示补偿标注与实际补偿有效期，补偿行不并入原票总次数", async () => {
  const service = {
    listOrders: vi.fn().mockResolvedValue([{ id: "order-1", orderNo: "T1", payStatus: "paid", issueStatus: "issued",
      status: "paid", salesChannel: "app", amountCent: 8800, quantity: 1 }]),
    listOrderTickets: vi.fn().mockResolvedValue(TICKETS),
  };
  render(<MyOrdersPage service={service as unknown as CustomerService} />);
  await waitFor(() => expect(screen.getByTestId("order-tickets-toggle")).toBeTruthy());
  fireEvent.click(screen.getByTestId("order-tickets-toggle"));
  await waitFor(() => expect(screen.getByTestId("ticket-item").textContent).toContain("剩余 1/2 次"));
  const tag = screen.getByTestId("ticket-compensation-tag");
  expect(tag.textContent).toContain("补偿（第1人）");
  expect(tag.textContent).toContain(`补偿有效期至 ${new Date(COMPENSATION_VALID_TO).toLocaleDateString()}`);
  expect(screen.getAllByTestId("ticket-compensation-tag")).toHaveLength(1);
  expect(within(screen.getByTestId("ticket-compensation")).getAllByTestId("ticket-qr-open")).toHaveLength(2);
});
