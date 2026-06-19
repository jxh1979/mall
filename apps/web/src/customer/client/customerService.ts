// 客人端业务服务层：封装 P0 客旅用到的真实端点（全部 /api/v1/customer/* 与 finance/ticketing）
// 所有接口均在 docs/openapi/openapi-p0.json 契约内；P1 能力（积分明细/礼物兑换）不在此暴露。

import { CustomerApi } from "./customerClient";
import type {
  BarReservation,
  BarReservationOption,
  CheckInRecord,
  Membership,
  PaymentIntent,
  PointsSummary,
  StoredValueAccount,
  TicketCredential,
  TicketOrder,
  TicketProduct,
} from "../types";

export class CustomerService {
  private readonly api: CustomerApi;

  constructor(customerToken: string) {
    this.api = new CustomerApi(customerToken);
  }

  // ① 票种浏览
  listTicketProducts(): Promise<TicketProduct[]> {
    return this.api.get<TicketProduct[]>("/api/v1/customer/ticket-products");
  }

  // ② 官方购票下单
  createTicketOrder(payload: {
    ticketTypeId: string;
    quantity: number;
    visitDate?: string;
    contactPhone?: string;
    buyerNote?: string;
  }): Promise<TicketOrder> {
    return this.api.post<TicketOrder>("/api/v1/customer/ticket-orders", payload);
  }

  // 支付意图（购票 / 订台定金共用）
  createPaymentIntent(payload: {
    businessOrderType: string;
    businessOrderId: string;
    amountCent: number;
    currency: string;
    channel?: string;
  }): Promise<PaymentIntent> {
    return this.api.post<PaymentIntent>("/api/v1/finance/payment-intents", {
      channel: "aggregate_pay",
      ...payload,
    });
  }

  // ③ 订台资源 + 订台（需定金）
  listBarReservationOptions(): Promise<BarReservationOption[]> {
    return this.api.get<BarReservationOption[]>("/api/v1/customer/bar-reservation-options");
  }

  createBarReservation(payload: {
    barId: string;
    tableId: string;
    tableType?: string;
    reservationTime: string;
    guestCount: number;
    contactPhone?: string;
  }): Promise<BarReservation> {
    return this.api.post<BarReservation>("/api/v1/customer/bar-reservations", payload);
  }

  // ④ 我的订单 / 我的票 / 我的权益
  listOrders(): Promise<TicketOrder[]> {
    return this.api.get<TicketOrder[]>("/api/v1/customer/orders");
  }

  // ⑤ 积分摘要 + 等级
  getPoints(): Promise<PointsSummary> {
    return this.api.get<PointsSummary>("/api/v1/customer/points");
  }

  // 连续签到（V089）
  checkIn(sourceTerminal = "customer-web"): Promise<CheckInRecord> {
    return this.api.post<CheckInRecord>("/api/v1/customer/check-ins", { sourceTerminal });
  }

  listCheckIns(): Promise<CheckInRecord[]> {
    return this.api.get<CheckInRecord[]>("/api/v1/customer/check-ins");
  }

  // ⑥ 验票二维码出示（凭证刷新走同一 GET）
  getTicketQr(ticketId: string): Promise<TicketCredential> {
    return this.api.get<TicketCredential>(`/api/v1/customer/tickets/${encodeURIComponent(ticketId)}/qr`);
  }

  // ⑦ 储值 / 会员（V090，可选展示）
  getStoredValueAccount(): Promise<StoredValueAccount | null> {
    return this.api.get<StoredValueAccount | null>("/api/v1/customer/stored-value/account");
  }

  listMemberships(): Promise<Membership[]> {
    return this.api.get<Membership[]>("/api/v1/customer/memberships");
  }
}
