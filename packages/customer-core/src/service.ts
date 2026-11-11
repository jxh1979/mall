// 文件版本：V0.13
// 客人端业务服务层（三端共用）：封装客人用到的真实端点（客人接口 customer 前缀等），接口以 docs/openapi/openapi-p0.yaml 为准。
// 请求经注入的 CustomerApi 发出（各端自带 transport），本文件不依赖浏览器。

import { ApiError } from "./apiError";
import { CustomerApi, type CustomerFormFile } from "./client";
import { forgetWriteOperations } from "./operationKey";
import type {
  AmusementProject,
  BarInvitationCode,
  BarInvitationEntryCode,
  BarInvitationPreview,
  BarTableBillItem,
  CustomerBarReservation,
  ComplaintStoreOption,
  CustomerComplaint,
  CustomerConsent,
  CustomerSavedBottle,
  LevelNotice,
  MerchantCardExitNotice,
  PointsLedgerEntry,
  SavedBottlePickupCode,
  SavedBottleRenewal,
  StoredValueTopupRules,
  StudentCertification,
  VoucherRedeemCode,
  CustomerProject,
  CustomerTerms,
  PaymentClientLaunch,
  TicketChannelStatus,
  TicketExpiryReminder,
  TicketUsage,
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
  CustomerPaymentLaunchView,
  PaymentIntent,
  PointsRedemption,
  PointsSummary,
  RedemptionItem,
  StoredValueAccount,
  StoredValueTransaction,
  TerminalSwitchRow,
  TicketCredential,
  TicketOrder,
  TicketProduct,
  VoucherTransfer,
} from "./types";

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
  graceUntil?: string | null; paymentUrl?: string | null; launch?: PaymentClientLaunch | null };

export type CustomerLoginResponse = {
  customerId: string;
  maskedPhone: string;
  customerToken: string;
  expiresAt: string;
};

export type CustomerSessionInfo = {
  customerId: string;
  maskedPhone: string;
  expiresAt: string | null;
};

/** 上传文件：各端的文件句柄带文件名（App 为浏览器 File）。 */
export type CustomerUploadFile = { name: string };

function asFormFile(file: CustomerUploadFile): CustomerFormFile {
  return { field: "files", file, fileName: file.name };
}

export class CustomerService {
  constructor(private readonly api: CustomerApi) {}

  // 手机号＋短信验证码登录：返回会话令牌，由各端放进本端安全会话（App 系统安全存储、小程序内存），不进 URL、日志或分享内容。
  async phoneLogin(payload: { phone: string; smsCode: string }): Promise<CustomerLoginResponse> {
    const result = await this.api.post<CustomerLoginResponse>("/api/v1/customer/auth/phone-login", payload);
    // M725-F02：上一位客人没拿到结果的请求编号不沿用到这位客人身上（共用设备登出不刷新页面）；同一位客人重新登录照常沿用。
    forgetWriteOperations(result.customerId);
    return result;
  }

  // ROOT-05-R3 登录取码（注册同一场景）：短信供应商未配置时服务端 409 CUSTOMER_OTP_PROVIDER_UNAVAILABLE，不假成功。
  requestLoginSmsCode(phone: string): Promise<{ phoneMasked: string; requestedAt: string }> {
    return this.api.post<{ phoneMasked: string; requestedAt: string }>("/api/v1/customer/auth/sms-codes", { phone });
  }

  // ROOT-03-R3 当前会员条款与隐私条款（匿名可读）。
  getTerms(): Promise<CustomerTerms> {
    return this.api.get<CustomerTerms>("/api/v1/customer/terms");
  }

  // ROOT-03-R3 注册会员：短信验证＋生日（YYYY-MM-DD）＋满 18 周岁＋同意当前两份条款；成功即建会员并返回会话凭证（同登录）。
  async register(payload: {
    phone: string;
    smsCode: string;
    birthDate: string;
    memberTermsVersion: string;
    privacyTermsVersion: string;
  }): Promise<CustomerLoginResponse> {
    const result = await this.api.post<CustomerLoginResponse>("/api/v1/customer/auth/register", payload);
    forgetWriteOperations(result.customerId);
    return result;
  }

  // clients-R9 首页：综合体、营业时间与经审批发布的介绍、公告（匿名可读）。
  listProjects(): Promise<CustomerProject[]> {
    return this.api.get<CustomerProject[]>("/api/v1/customer/projects");
  }

  // clients-R9（BSM「连接游乐系统，展示可预定项目和场次」）：该综合体开放中的游乐项目与尚未结束的场次（匿名可读，只展示、不在此预约）。
  listAmusementProjects(projectId: string): Promise<AmusementProject[]> {
    return this.api.get<AmusementProject[]>(`/api/v1/customer/amusement/projects?project_id=${encodeURIComponent(projectId)}`);
  }

  // clients-R10 本人即将到期的未核销票（启动与回前台拉取并弹出）。
  listTicketExpiryReminders(): Promise<TicketExpiryReminder[]> {
    return this.api.get<TicketExpiryReminder[]>("/api/v1/customer/ticket-expiry-reminders");
  }

  // OBS-13-01 本人未读的会员等级通知（降级预警、已保级、降级补发；启动与回前台拉取并弹出）。
  listLevelNotices(): Promise<LevelNotice[]> {
    return this.api.get<LevelNotice[]>("/api/v1/customer/level-notices");
  }

  // 客人点「知道了」：回写本人通知已读，之后不再返回。
  markLevelNoticeRead(noticeId: string): Promise<null> {
    return this.api.post<null>(`/api/v1/customer/level-notices/${encodeURIComponent(noticeId)}/read`, {});
  }

  // ROOT-03-R29 我的票使用记录（只许本人持有的票）。
  listTicketUsages(ticketId: string): Promise<TicketUsage[]> {
    return this.api.get<TicketUsage[]>(`/api/v1/customer/tickets/${encodeURIComponent(ticketId)}/usages`);
  }

  // ROOT-03-R29 第三方渠道接入状态（匿名可读，未接入即置灰）。
  listTicketChannels(): Promise<TicketChannelStatus[]> {
    return this.api.get<TicketChannelStatus[]>("/api/v1/customer/ticket-channels");
  }

  // 启动时凭本端保存的令牌恢复会话；无效（401）或匿名（204）返回 null，由调用方清掉本端令牌、回登录页。
  // 断网、超时与服务端异常不算会话失效，原样抛出，由调用方留着本端令牌、下次再试。
  async restoreSession(): Promise<CustomerSessionInfo | null> {
    try {
      const info = await this.api.get<CustomerSessionInfo | null>("/api/v1/customer/session");
      // M725-F02：恢复会话也记下是谁，同一位客人会话失效后重新登录照常沿用没拿到结果的请求编号。
      if (info) forgetWriteOperations(info.customerId);
      return info;
    } catch (err) {
      if (err instanceof ApiError && err.detail.status === 401) return null;
      throw err;
    }
  }

  // 登出：带当前令牌撤销服务端会话（端点幂等；失败不阻断本端清令牌）。
  async logout(): Promise<void> {
    try {
      await this.api.post<null>("/api/v1/customer/auth/logout");
    } catch {
      // 服务端撤销失败不阻断登出（本端令牌照清，会话到期自灭）。
    }
  }

  // ROOT-03-R2：本端（app/miniapp）及全端已关闭的开关行，客户端据此隐藏入口（customerSwitchState）。
  listEffectiveTerminalSwitches(): Promise<TerminalSwitchRow[]> {
    return this.api.get<TerminalSwitchRow[]>("/api/v1/customer/terminal-switches/effective");
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

  // ROOT-03-R4/R18：本人订单发起在线付款（金额由服务端定），返回渠道按本端签发的拉起参数，交 launchDecision 判定怎么拉起。
  startTicketPayment(orderId: string, channel?: "wechat" | "alipay"): Promise<CustomerPaymentLaunchView> {
    return this.api.post<CustomerPaymentLaunchView>(`/api/v1/customer/orders/${encodeURIComponent(orderId)}/payments`, { channel });
  }

  // 回前台查单：服务端向渠道查单成功才记已付并出票。
  confirmTicketPayment(orderId: string, paymentIntentId: string): Promise<TicketOrder> {
    return this.api.post<TicketOrder>(
      `/api/v1/customer/orders/${encodeURIComponent(orderId)}/payments/${encodeURIComponent(paymentIntentId)}/confirm`);
  }

  // ROOT-03-R4/R18：本人订台发起定金在线付款与回前台查单。
  startDepositPayment(reservationId: string, channel?: "wechat" | "alipay"): Promise<CustomerPaymentLaunchView> {
    return this.api.post<CustomerPaymentLaunchView>(
      `/api/v1/customer/bar-reservations/${encodeURIComponent(reservationId)}/deposit-payments`, { channel });
  }

  confirmDepositPayment(reservationId: string, paymentIntentId: string): Promise<BarReservation> {
    return this.api.post<BarReservation>(
      `/api/v1/customer/bar-reservations/${encodeURIComponent(reservationId)}/deposit-payments/${encodeURIComponent(paymentIntentId)}/confirm`);
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
  // 第十八批 R 段第 152 条 152-R3（第十八批裁决 34「客户点击后确定自己是订台人还是参与者」）：asHolder＝选「我是订台人」（仅预览 holderClaimable 时可选），缺省按参与者。
  confirmBarInvitation(payload: BarInvitationCode, asHolder = false): Promise<BarInvitationMember> {
    return this.api.post<BarInvitationMember>("/api/v1/customer/bar-invitations/confirm", { ...payload, asHolder });
  }

  // clients-R6 我的订台（本人订台列表，由此进入邀请函）。
  listMyBarReservations(): Promise<CustomerBarReservation[]> {
    return this.api.get<CustomerBarReservation[]>("/api/v1/customer/bar-reservations");
  }

  // clients-R6 扫邀请函码预览（不占名额；防伪标记、卡座或可进人数被篡改 409）。
  previewBarInvitation(code: BarInvitationCode): Promise<BarInvitationPreview> {
    const query = [
      ["invitation_token", code.invitationToken],
      ["invitation_signature", code.invitationSignature],
      ["table_id", code.tableId],
      ["capacity", String(code.capacity)],
    ].map(([key, value]) => `${key}=${encodeURIComponent(value ?? "")}`).join("&");
    return this.api.get<BarInvitationPreview>(`/api/v1/customer/bar-invitations/preview?${query}`);
  }

  // clients-R6 开卡人移出尚未入场的被邀请人（名额释放）。
  removeBarInvitationMember(reservationId: string, memberId: string): Promise<BarInvitationMember> {
    return this.api.post<BarInvitationMember>(
      `/api/v1/customer/bar-invitations/${encodeURIComponent(reservationId)}/members/${encodeURIComponent(memberId)}/remove`);
  }

  // clients-R6 本人（已接受未入场，含开卡人）动态入场码。
  getBarInvitationEntryCode(reservationId: string): Promise<BarInvitationEntryCode> {
    return this.api.get<BarInvitationEntryCode>(`/api/v1/customer/bar-invitations/${encodeURIComponent(reservationId)}/entry-code`);
  }

  // ④ 我的订单 / 我的票 / 我的权益
  listOrders(): Promise<TicketOrder[]> {
    return this.api.get<TicketOrder[]>("/api/v1/customer/orders");
  }

  // ⑤ 积分摘要 + 等级
  getPoints(): Promise<PointsSummary> {
    return this.api.get<PointsSummary>("/api/v1/customer/points");
  }

  // BSM「积分明细」。
  listPointsLedger(): Promise<PointsLedgerEntry[]> {
    return this.api.get<PointsLedgerEntry[]>("/api/v1/customer/points/ledger");
  }

  // BSM「采集证件……前先单独取得同意，并留同意版本、时间和范围」：记一条同意（来源端由服务端取本会话锁定的登录端）。
  recordConsent(payload: { consentType: string; accepted: boolean; version: string }): Promise<CustomerConsent> {
    return this.api.post<CustomerConsent>("/api/v1/customer/consents", payload);
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

  // ROOT-03-R20：amountCent 不填＝付清待付金额；填了只付这一部分，其余在线补足。
  payTicketOrderWithStoredValue(orderId: string, payload: { accountId: string; phone: string; smsCode: string; amountCent?: number }): Promise<TicketOrder> {
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

  // TK-21-R3 自助注销：先预览各卡本金/赠送金（ROOT-03-R21 另带名下存酒），再带「是否放弃余额」「是否放弃名下存酒」注销
  // （自动转蛇形 waive_balance、abandon_saved_bottles）。
  getDeregisterPreview(): Promise<DeregisterPreview> {
    return this.api.get<DeregisterPreview>("/api/v1/customer/deregister/preview");
  }

  deregister(choice: { waiveBalance: boolean; abandonSavedBottles: boolean }): Promise<{ status: string }> {
    return this.api.post<{ status: string }>("/api/v1/customer/deregister", choice);
  }

  // ROOT-03-R30 充值前规则告知（merchantId 为空＝综合体通用卡）。
  getStoredValueTopupRules(scope: { projectId?: string; merchantId?: string }): Promise<StoredValueTopupRules> {
    const query = [["project_id", scope.projectId], ["merchant_id", scope.merchantId]]
      .filter(([, value]) => value).map(([key, value]) => `${key}=${encodeURIComponent(value!)}`).join("&");
    return this.api.get<StoredValueTopupRules>(`/api/v1/customer/stored-value/topup-rules${query ? `?${query}` : ""}`);
  }

  // ROOT-03-R31 商户储值卡退出公告（公示中的，本人持卡的排前）。
  listMerchantCardExitNotices(): Promise<MerchantCardExitNotice[]> {
    return this.api.get<MerchantCardExitNotice[]>("/api/v1/customer/merchant-card-exit-notices");
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

  // 回前台查单：渠道确认收款才成单（status=paid），未确认 409，不成单。
  confirmBarScanOrder(tableId: string, scanOrderId: string): Promise<BarScanOrder> {
    return this.api.post<BarScanOrder>(
      `/api/v1/customer/bar-tables/${encodeURIComponent(tableId)}/scan-orders/${encodeURIComponent(scanOrderId)}/confirm`);
  }

  // 本桌当前账单逐项（已上菜与未上菜）：响应 {table_id, items, total_cent} 经统一信封解包只留 items（见 client.unwrap），合计由页面按行加总。
  getBarTableCurrentBill(tableId: string): Promise<BarTableBillItem[]> {
    return this.api.get<BarTableBillItem[]>(`/api/v1/customer/bar-tables/${encodeURIComponent(tableId)}/current-bill`);
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

  // ROOT-03-R32 券动态核销码（每次取码撤旧码，明文只在本响应）。
  getVoucherRedeemCode(voucherInstanceId: string): Promise<VoucherRedeemCode> {
    return this.api.get<VoucherRedeemCode>(`/api/v1/customer/vouchers/${encodeURIComponent(voucherInstanceId)}/redeem-code`);
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

  // BSM「我的存酒」：本人名下全部存酒（跨店汇总，每条标明所属酒吧）、动态取酒码、本人转存（短信验证码确认）。
  listMySavedBottles(): Promise<CustomerSavedBottle[]> {
    return this.api.get<CustomerSavedBottle[]>("/api/v1/customer/my-saved-bottles");
  }

  getSavedBottlePickupCode(savedBottleId: string): Promise<SavedBottlePickupCode> {
    return this.api.get<SavedBottlePickupCode>(`/api/v1/customer/saved-bottles/${encodeURIComponent(savedBottleId)}/pickup-code`);
  }

  requestSavedBottleRenewalCode(savedBottleId: string): Promise<{ savedBottleId: string; requestedAt: string }> {
    return this.api.post(`/api/v1/customer/saved-bottles/${encodeURIComponent(savedBottleId)}/renewal-code`);
  }

  renewSavedBottle(savedBottleId: string, smsCode: string): Promise<SavedBottleRenewal> {
    return this.api.post<SavedBottleRenewal>(`/api/v1/customer/saved-bottles/${encodeURIComponent(savedBottleId)}/renew`, { smsCode });
  }

  // clients-R8 投诉（文字＋照片、视频、语音，附件合计不超过 9 个，类型由服务端按扩展名判定）、我的投诉、满意度（1～5）；
  // 第十八批 M 段第 111 条 111-R4：涉及门店选填（第十八批裁决 28「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正」）。
  submitComplaint(payload: { projectId: string; title: string; description?: string; storeMerchantId?: string;
    files: CustomerUploadFile[] }): Promise<CustomerComplaint> {
    const fields: Record<string, string> = { projectId: payload.projectId, title: payload.title };
    if (payload.description) fields.description = payload.description;
    if (payload.storeMerchantId) fields.storeMerchantId = payload.storeMerchantId;
    return this.api.postForm<CustomerComplaint>("/api/v1/customer/complaints", fields, payload.files.map(asFormFile));
  }

  // 111-R4：投诉表单「涉及门店」选项＝所选综合体的在营商户。
  listComplaintStores(projectId: string): Promise<ComplaintStoreOption[]> {
    return this.api.get<ComplaintStoreOption[]>(`/api/v1/customer/complaints/store-options?project_id=${encodeURIComponent(projectId)}`);
  }

  listMyComplaints(): Promise<CustomerComplaint[]> {
    return this.api.get<CustomerComplaint[]>("/api/v1/customer/complaints");
  }

  rateComplaint(workOrderId: string, score: number): Promise<CustomerComplaint> {
    return this.api.post<CustomerComplaint>(`/api/v1/customer/complaints/${encodeURIComponent(workOrderId)}/satisfaction`, { score });
  }

  // R28 学生认证：带证件采集单独同意的编号、学信网《学籍在线验证报告》在线验证码与证件照片（1～9 张）。
  applyStudentCertification(payload: { consentId: string; chsiVerificationCode: string; files: CustomerUploadFile[] }): Promise<StudentCertification> {
    return this.api.postForm<StudentCertification>("/api/v1/customer/student-certifications",
      { consentId: payload.consentId, chsiVerificationCode: payload.chsiVerificationCode }, payload.files.map(asFormFile));
  }

  getStudentCertification(): Promise<StudentCertification | null> {
    return this.api.get<StudentCertification | null>("/api/v1/customer/student-certifications/current");
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
    const key = /^[一-龥]/.test(code) ? "plate_no" : "temp_code";
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
