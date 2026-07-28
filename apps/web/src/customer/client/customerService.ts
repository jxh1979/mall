// 客人端业务服务层：封装 P0 客旅用到的真实端点（全部 /api/v1/customer/* 与 finance/ticketing）
// 所有接口均在 docs/openapi/openapi-p0.json 契约内；P1 能力（积分明细/礼物兑换）不在此暴露。

import { CustomerApi } from "./customerClient";
import type {
  CustomerTicket,
  CustomerVoucher,
  TicketTransfer,
  TicketTransferPreview,
  BarInvitation,
  BarInvitationMember,
  BarReservation,
  BarReservationOption,
  BarScanMenuItem,
  BarScanOrder,
  CheckInRecord,
  PaymentIntent,
  PointsRedemption,
  PointsSummary,
  RedemptionItem,
  StoredValueAccount,
  StoredValueTransaction,
  TicketCredential,
  TicketOrder,
  TicketProduct,
  VoucherTransfer,
} from "../types";

export class CustomerService {
  private readonly api: CustomerApi;

  constructor(customerToken: string) {
    this.api = new CustomerApi(customerToken);
  }

  // ① 票种浏览（AUD-076：barId 传入=专属入口锁定该酒吧；不传=公共入口全量）。
  listTicketProducts(barId?: string): Promise<TicketProduct[]> {
    const query = barId ? `?bar_id=${encodeURIComponent(barId)}` : "";
    return this.api.get<TicketProduct[]>(`/api/v1/customer/ticket-products${query}`);
  }

  // ② 官方购票下单
  createTicketOrder(payload: {
    ticketTypeId: string;
    quantity: number;
    visitDate?: string;
    contactPhone?: string;
    buyerNote?: string;
    // AUD-076 单店票挂酒吧：单店票必填该票所属酒吧 id（通票不传）；请求体自动 snake_case 为 bar_id。
    barId?: string;
    // V359：需拍照的票种带上每张票的入场人脸照（base64，不含 data URI 前缀）；其余票种不传。
    facePhotos?: string[];
    // V368：拍照前那块告知上勾的同意。带照片就必须为 true——服务端据此往同意台账落一条留痕，
    //   没勾同意直接拒（没有同意就不该有照片）。
    faceConsent?: boolean;
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

  // ③a 订卡邀请函：持函人查看自己的邀请函（展示二维码 token / 容量 / 已确认成员，供转发）
  getMyBarInvitation(reservationId: string): Promise<BarInvitation> {
    return this.api.get<BarInvitation>(`/api/v1/customer/bar-invitations/${encodeURIComponent(reservationId)}`);
  }

  // ③a 订卡邀请函：朋友确认接受（两步之第一步，占一个名额）
  confirmBarInvitation(invitationToken: string): Promise<BarInvitationMember> {
    return this.api.post<BarInvitationMember>("/api/v1/customer/bar-invitations/confirm", { invitationToken });
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

  // 1.8 我的票：订单内票实例（点票出码，无需手输票 ID）
  listOrderTickets(orderId: string): Promise<CustomerTicket[]> {
    return this.api.get<CustomerTicket[]>(`/api/v1/customer/orders/${encodeURIComponent(orderId)}/tickets`);
  }

  // ⑥ 验票二维码出示（凭证刷新走同一 GET）
  getTicketQr(ticketId: string): Promise<TicketCredential> {
    return this.api.get<TicketCredential>(`/api/v1/customer/tickets/${encodeURIComponent(ticketId)}/qr`);
  }

  // ⑦ 储值 / 会员（V090，可选展示）
  getStoredValueAccount(): Promise<StoredValueAccount | null> {
    return this.api.get<StoredValueAccount | null>("/api/v1/customer/stored-value/account");
  }

  // AUD-061 我的全部卡（通用卡 + 各商户卡分卡余额）
  getStoredValueAccounts(): Promise<StoredValueAccount[]> {
    return this.api.get<StoredValueAccount[]>("/api/v1/customer/stored-value/accounts");
  }

  // AUD-061 充值（绑支付意图，待柜台确认收款到账）
  topupStoredValue(payload: { projectId?: string; merchantId?: string; amountCent: number; channel?: string }): Promise<PaymentIntent> {
    return this.api.post<PaymentIntent>("/api/v1/customer/stored-value/topup", { channel: "aggregate_pay", ...payload });
  }

  // AUD-061 消费（即时扣本卡余额；platform_collect 卡消费逐笔挂应付商户）
  consumeStoredValue(payload: { projectId?: string; merchantId?: string; amountCent: number; reason?: string }): Promise<StoredValueTransaction> {
    return this.api.post<StoredValueTransaction>("/api/v1/customer/stored-value/consume", payload);
  }

  // ⑧ 扫码点单（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId -> 看菜单 / 自助下单（不重复核龄）
  listBarScanMenu(sessionId: string): Promise<BarScanMenuItem[]> {
    return this.api.get<BarScanMenuItem[]>(
      `/api/v1/customer/bar-sessions/${encodeURIComponent(sessionId)}/menu`,
    );
  }

  placeBarScanOrder(
    sessionId: string,
    items: Array<{ productId: string; quantity: number }>,
  ): Promise<BarScanOrder> {
    return this.api.post<BarScanOrder>(
      `/api/v1/customer/bar-sessions/${encodeURIComponent(sessionId)}/scan-orders`,
      { items },
    );
  }

  // ⑨ 积分商城：兑换目录 / 发起兑换 / 兑换记录（GET/POST /customer/points/redemption*）
  listRedemptionItems(): Promise<RedemptionItem[]> {
    return this.api.get<RedemptionItem[]>("/api/v1/customer/points/redemption-items");
  }

  redeemPoints(catalogItemId: string, quantity: number): Promise<PointsRedemption> {
    return this.api.post<PointsRedemption>("/api/v1/customer/points/redemptions", {
      catalogItemId,
      quantity,
      sourceTerminal: "customer-web",
    });
  }

  listRedemptions(): Promise<PointsRedemption[]> {
    return this.api.get<PointsRedemption[]>("/api/v1/customer/points/redemptions");
  }

  // 我的券包（只读聚合）：积分兑换券 / 营销赠券 / 通票第三方券，按 token 强制本人 customer_id。
  listMyVouchers(): Promise<CustomerVoucher[]> {
    return this.api.get<CustomerVoucher[]>("/api/v1/customer/vouchers");
  }

  // V313 券转赠（会员间转移，范式对齐门票转赠）。channel=phone 需 receiverPhone；link 服务端生成一次性领取码。
  initiateVoucherTransfer(req: {
    voucherInstanceId: string;
    channel: "phone" | "link";
    receiverPhone?: string;
  }): Promise<VoucherTransfer> {
    return this.api.post<VoucherTransfer>("/api/v1/customer/voucher-transfers", req);
  }

  claimVoucherTransfer(transferId: string, claimCode?: string): Promise<VoucherTransfer> {
    return this.api.post<VoucherTransfer>(
      `/api/v1/customer/voucher-transfers/${encodeURIComponent(transferId)}/claim`,
      { claimCode },
    );
  }

  revokeVoucherTransfer(transferId: string): Promise<VoucherTransfer> {
    return this.api.post<VoucherTransfer>(
      `/api/v1/customer/voucher-transfers/${encodeURIComponent(transferId)}/revoke`,
      {},
    );
  }

  previewVoucherTransfer(claimCode: string): Promise<VoucherTransfer> {
    return this.api.get<VoucherTransfer>(
      `/api/v1/customer/voucher-transfers/preview?claim_code=${encodeURIComponent(claimCode)}`,
    );
  }

  listIncomingVoucherTransfers(): Promise<VoucherTransfer[]> {
    return this.api.get<VoucherTransfer[]>("/api/v1/customer/voucher-transfers/incoming");
  }

  listOutgoingVoucherTransfers(): Promise<VoucherTransfer[]> {
    return this.api.get<VoucherTransfer[]>("/api/v1/customer/voucher-transfers/outgoing");
  }

  // AUD-077 门票转赠
  listMyTickets(): Promise<CustomerTicket[]> {
    return this.api.get<CustomerTicket[]>("/api/v1/customer/my-tickets");
  }

  initiateTicketTransfer(req: {
    parentTicketId: string;
    slotIndex?: number;
    channel: "phone" | "link";
    receiverPhone?: string;
  }): Promise<TicketTransfer> {
    return this.api.post<TicketTransfer>("/api/v1/customer/ticket-transfers", req);
  }

  claimTicketTransfer(transferId: string, claimCode?: string): Promise<TicketTransfer> {
    return this.api.post<TicketTransfer>(
      `/api/v1/customer/ticket-transfers/${encodeURIComponent(transferId)}/claim`,
      { claimCode },
    );
  }

  revokeTicketTransfer(transferId: string): Promise<TicketTransfer> {
    return this.api.post<TicketTransfer>(
      `/api/v1/customer/ticket-transfers/${encodeURIComponent(transferId)}/revoke`,
      {},
    );
  }

  previewTicketTransfer(claimCode: string): Promise<TicketTransferPreview> {
    return this.api.get<TicketTransferPreview>(
      `/api/v1/customer/ticket-transfers/preview?claim_code=${encodeURIComponent(claimCode)}`,
    );
  }

  listIncomingTicketTransfers(): Promise<TicketTransfer[]> {
    return this.api.get<TicketTransfer[]>("/api/v1/customer/ticket-transfers/incoming");
  }

  listOutgoingTicketTransfers(): Promise<TicketTransfer[]> {
    return this.api.get<TicketTransfer[]>("/api/v1/customer/ticket-transfers/outgoing");
  }
}
