// 文件版本：V0.08
// 客人端业务服务层：封装 P0 客旅用到的真实端点（全部 /api/v1/customer/* 与 finance/ticketing）
// 所有接口均在 docs/openapi/openapi-p0.json 契约内；P1 能力（积分明细/礼物兑换）不在此暴露。

import { CustomerApi } from "./customerClient";
import type {
  CustomerTicket,
  DeregisterPreview,
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

export type TicketUseReward = { id: string; parentTicketId: string; slotIndex: number; useIndex: number; points: number; growth: number; waitingHours: number; verifiedAt: string; availableAt: string; status: string };
export type TicketRewardSummary = { rewards: TicketUseReward[]; pendingRecovery: number };
// 第十一批 TK-04-R5（BSM L877 客人端只展示结果；ENUM L77 与消费单据双向可查）：商户消费积分与成长值结果（已扣除已退部分）。
export type MerchantConsumptionReward = { id: string; sourceType: string; sourceId: string; sourceNo: string; merchantName: string; points: number; growth: number; status: string; completedAt: string; availableAt: string | null; confirmedAt: string | null };
export type TicketRewardRule = { id: string; ticketTypeId: string; ticketName: string; projectId: string; projectName: string; kind: string; version: string; fixedValue: number; effectiveAt: string; publishedAt: string };

// HP-P01（BSM L1435／L1433）：在场车辆停车费查询与缴费；付款确认后返回离场宽限截止。
export type ParkingSessionQuote = { parkingSessionId: string; projectId: string; plateNo: string | null; tempCode: string | null;
  enteredAt: string; grossFeeCent?: number; feeCent: number; paidCent: number; dueCent: number; graceUntil: string | null;
  creditId?: string | null; creditStatus?: string | null; creditMinutes?: number; creditOffsetHours?: number };
// HP-P01（BSM「停车场」章「消费抵免」条）：本项目当日消费抵免——可用／已用／已撤销／未达标、已累计有效消费、门槛与抵免时长。
export type ParkingCredit = { projectId: string; businessDate: string; status: "available" | "used" | "revoked" | "not_reached";
  eligibleAmountCent: number; thresholdCent: number; creditMinutes: number };
export type ParkingPayment = { id: string; parkingSessionId?: string; amountCent: number; status: string; paidAt?: string | null;
  graceUntil?: string | null; paymentUrl?: string | null };

export class CustomerService {
  private readonly api: CustomerApi;

  constructor(customerToken: string) {
    this.api = new CustomerApi(customerToken);
  }

  listTicketRewards(): Promise<TicketRewardSummary> {
    return this.api.get<TicketRewardSummary>("/api/v1/customer/ticket-rewards");
  }

  listMerchantRewards(): Promise<MerchantConsumptionReward[]> {
    return this.api.get<MerchantConsumptionReward[]>("/api/v1/customer/merchant-rewards");
  }

  listTicketRewardRules(): Promise<TicketRewardRule[]> {
    return this.api.get<TicketRewardRule[]>("/api/v1/customer/ticket-reward-rules");
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
    // ROOT-03-R19（BSM 第 1062 行）：每笔订台必填最晚到店时间。
    latestArrivalAt: string;
  }): Promise<BarReservation> {
    return this.api.post<BarReservation>("/api/v1/customer/bar-reservations", payload);
  }

  // ③a 订卡邀请函：持函人查看自己的邀请函（展示二维码 token / 容量 / 已确认成员，供转发）
  getMyBarInvitation(reservationId: string): Promise<BarInvitation> {
    return this.api.get<BarInvitation>(`/api/v1/customer/bar-invitations/${encodeURIComponent(reservationId)}`);
  }

  // ③a 订卡邀请函：朋友确认接受（两步之第一步，占一个名额）。ROOT-03-R25：二维码载荷的防伪标记、卡座、可进人数一并回传核验。
  confirmBarInvitation(payload: {
    invitationToken: string;
    invitationSignature: string;
    tableId: string;
    capacity: number;
  }): Promise<BarInvitationMember> {
    return this.api.post<BarInvitationMember>("/api/v1/customer/bar-invitations/confirm", payload);
  }

  // ④ 我的订单 / 我的票 / 我的权益
  listOrders(): Promise<TicketOrder[]> {
    return this.api.get<TicketOrder[]>("/api/v1/customer/orders");
  }

  // ⑤ 积分摘要 + 等级
  getPoints(): Promise<PointsSummary> {
    return this.api.get<PointsSummary>("/api/v1/customer/points");
  }

  // 连续签到（V089）；来源终端由服务端取本会话锁定的登录端（ROOT-03-R2），不再上送。
  checkIn(): Promise<CheckInRecord> {
    return this.api.post<CheckInRecord>("/api/v1/customer/check-ins", {});
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

  requestStoredValueCode(phone: string): Promise<unknown> {
    return this.api.post("/api/v1/customer/stored-value/sms", { phone });
  }

  payTicketOrderWithStoredValue(orderId: string, payload: { accountId: string; phone: string; smsCode: string }): Promise<TicketOrder> {
    return this.api.post<TicketOrder>(`/api/v1/customer/orders/${encodeURIComponent(orderId)}/stored-value/pay`, payload);
  }

  // ⑦ 储值 / 会员（V090，可选展示）
  getStoredValueAccount(): Promise<StoredValueAccount | null> {
    return this.api.get<StoredValueAccount | null>("/api/v1/customer/stored-value/account");
  }

  // AUD-061 我的全部卡（通用卡 + 各商户卡分卡余额）
  getStoredValueAccounts(): Promise<StoredValueAccount[]> {
    return this.api.get<StoredValueAccount[]>("/api/v1/customer/stored-value/accounts");
  }

  // 线上充值返回真实渠道拉起信息；入账须由服务端核验渠道成功回执。
  topupStoredValue(payload: { projectId?: string; merchantId?: string; amountCent: number; channel?: string }): Promise<PaymentIntent> {
    return this.api.post<PaymentIntent>("/api/v1/customer/stored-value/topup", { ...payload });
  }

  settleStoredValueTopup(payload: { paymentIntentId: string; projectId?: string; merchantId?: string }): Promise<StoredValueTransaction> {
    return this.api.post<StoredValueTransaction>("/api/v1/customer/stored-value/settle", payload);
  }

  // TK-21-R3 自助注销：先预览各卡本金/赠送金（ROOT-03-R21 另带名下存酒），再带「是否放弃余额」注销（waiveBalance 自动转蛇形 waive_balance）。
  getDeregisterPreview(): Promise<DeregisterPreview> {
    return this.api.get<DeregisterPreview>("/api/v1/customer/deregister/preview");
  }

  deregister(waiveBalance: boolean): Promise<{ status: string }> {
    return this.api.post<{ status: string }>("/api/v1/customer/deregister", { waiveBalance });
  }

  getStoredValueTransactions(accountId: string, page = 1): Promise<StoredValueTransaction[]> {
    return this.api.get<StoredValueTransaction[]>(
      `/api/v1/customer/stored-value/accounts/${encodeURIComponent(accountId)}/transactions?page=${page}&size=20`);
  }

  // ⑧ 扫码点单（ROOT-03-R7）：扫散台固定桌台码得 tableId -> 看菜单 / 下单即付款（付款成功才成单，不重复核龄）
  listBarScanMenu(tableId: string): Promise<BarScanMenuItem[]> {
    return this.api.get<BarScanMenuItem[]>(
      `/api/v1/customer/bar-tables/${encodeURIComponent(tableId)}/menu`,
    );
  }

  placeBarScanOrder(
    tableId: string,
    items: Array<{ productId: string; quantity: number }>,
  ): Promise<BarScanOrder> {
    return this.api.post<BarScanOrder>(
      `/api/v1/customer/bar-tables/${encodeURIComponent(tableId)}/scan-orders`,
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

  // 有牌车按车牌（省份简称开头）查，无牌车按入口领取的临时码查。
  findParkingSession(projectId: string, code: string): Promise<ParkingSessionQuote> {
    const key = /^[\u4e00-\u9fa5]/.test(code) ? "plate_no" : "temp_code";
    return this.api.get<ParkingSessionQuote>(
      `/api/v1/customer/parking/sessions?project_id=${encodeURIComponent(projectId)}&${key}=${encodeURIComponent(code)}`);
  }

  getParkingCredit(projectId: string): Promise<ParkingCredit> {
    return this.api.get<ParkingCredit>(`/api/v1/customer/parking/credits?project_id=${encodeURIComponent(projectId)}`);
  }

  createParkingPayment(parkingSessionId: string): Promise<ParkingPayment> {
    return this.api.post<ParkingPayment>("/api/v1/customer/parking/payments", { parkingSessionId });
  }

  confirmParkingPayment(paymentId: string): Promise<ParkingPayment> {
    return this.api.post<ParkingPayment>(`/api/v1/customer/parking/payments/${encodeURIComponent(paymentId)}/confirm`);
  }

  listOutgoingTicketTransfers(): Promise<TicketTransfer[]> {
    return this.api.get<TicketTransfer[]>("/api/v1/customer/ticket-transfers/outgoing");
  }
}
