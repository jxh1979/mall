// 文件版本：V0.04
// 客人端响应类型（对齐后端 customer 域 record，camelCase 化后口径）

export type TicketProduct = {
  ticketTypeId: string;
  ticketName: string;
  weekdayPriceCent: number | null;
  weekendPriceCent: number | null;
  holidayPriceCent: number | null;
  currency: string;
  status: string;
  // AUD-076 单店票挂酒吧：所属酒吧 id 与实时名称（null=综合体通票，展示“综合体通用”）。
  barId?: string | null;
  barName?: string | null;
};

// 1.8 我的票：订单内票实例安全投影（后端 CustomerTicketResponse）。
export type CustomerTicket = {
  id: string;
  ticketName: string | null;
  entitlementType: string;
  status: string;
  totalTimes: number;
  usedTimes: number;
  remainingTimes: number;
  numPeople: number;
  validFrom: string | null;
  validTo: string | null;
  // AUD-066 父票聚合：按父票聚合展示（票名+N人+剩余次数），每人一个 slot 各自出码。
  parentTicketId?: string | null;
  slotIndex?: number;
  ticketNo?: string | null;
  // AUD-077 转赠中标记（"transferring"=该票转赠冻结中不可用）。
  transferState?: string | null;
  // TK-13 补偿标注（"full_reissue"=原票原人份下的完整补发补偿行，其 validTo 即实际补偿有效期；null=原票行）。
  compensationKind?: string | null;
};

// AUD-077 门票转赠记录（后端 TicketTransferResponse 安全投影）。
export type TicketTransfer = {
  id: string;
  parentTicketId: string;
  slotIndex: number;
  channel: string;
  status: string;
  claimCode?: string | null;
  receiverPhoneMasked?: string | null;
  ticketName?: string | null;
  voidReason?: string | null;
  createdAt: string | null;
  claimedAt?: string | null;
};

// AUD-077 领取预览（不算领取）。
export type TicketTransferPreview = {
  transferId: string;
  parentTicketId: string;
  slotIndex: number;
  channel: string;
  status: string;
  ticketName: string | null;
  numPeople: number;
  totalTimes: number;
};

export type TicketOrder = {
  id: string;
  orderNo: string;
  customerId: string;
  ticketTypeId: string;
  priceRuleId: string | null;
  quantity: number;
  visitDate: string | null;
  salesChannel: string;
  buyerPhone: string;
  amountCent: number;
  currency: string;
  payStatus: string;
  issueStatus: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  // AUD-076 单店票挂酒吧：订单快照的酒吧 id（null=通票）。
  barId?: string | null;
  // 售票时确认的综合体范围，缺失时不能用限定综合体的储值卡。
  projectId?: string | null;
};

export type TicketCredential = {
  id: string;
  ticketId: string;
  orderId: string;
  credentialToken: string;
  credentialType: string;
  issuedAt: string;
  expiresAt: string;
  refreshedCount: number;
};

export type BarReservationOption = {
  barId: string;
  tableId: string;
  tableType: string;
  tableName: string;
  capacity: number;
  allowedPassFreeGuestCount: number;
  minConsumptionCent: number;
  currency: string;
  status: string;
};

export type BarReservation = {
  id: string;
  barId: string;
  tableId: string;
  customerId: string;
  reservedAt: string;
  guestCount: number;
  depositAmountCent: number;
  depositStatus: string;
  orderSource: string;
  status: string;
  depositIntentId: string | null;
  createdAt: string;
};

// V155 订卡邀请函（客人端展示/转发/确认）
export type BarInvitationMember = {
  id: string;
  reservationId?: string;
  barId?: string;
  memberIdentifier?: string;
  memberCustomerId?: string | null;
  isHolder?: boolean;
  status?: string;
  confirmedAt?: string | null;
  enteredAt?: string | null;
};

export type BarInvitation = {
  reservationId: string;
  barId?: string;
  tableId?: string;
  invitationToken?: string | null;
  invitationSignature?: string | null;
  capacity?: number;
  status?: string;
  issuedAt?: string | null;
  confirmedCount?: number;
  enteredCount?: number;
  remaining?: number;
  members?: BarInvitationMember[];
};

export type PointsSummary = {
  customerId: string;
  balance: number;
  level: string;
  recentSummary: Record<string, unknown> | null;
};

export type CheckInRecord = {
  id: string;
  customerId: string;
  checkInDate: string;
  streak: number;
  awardedPoints: number;
  sourceTerminal: string | null;
  createdAt: string;
};

export type StoredValueAccount = {
  id: string;
  customerId: string;
  projectId: string | null;
  merchantId: string | null;
  cardScope: string;
  fundOwnership: string | null;
  balanceCent: number;
  // CG-06a：本金 / 赠送金两段分列（BSM「我的余额」），balanceCent = principalCent + bonusCent。
  principalCent: number;
  bonusCent: number;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type StoredValueTransaction = {
  id: string;
  accountId: string;
  customerId: string;
  txnType: string;
  amountCent: number;
  balanceAfterCent: number;
  // CG-06a：本笔变动中的本金 / 赠送金部分（带方向）。
  principalCent: number;
  bonusCent: number;
  reason: string | null;
  createdAt: string;
};

export type PaymentIntent = {
  id?: string;
  status?: string;
  [key: string]: unknown;
};

export type CustomerSession = {
  customerId: string;
  maskedPhone: string;
  customerToken: string;
  expiresAt: string;
};

// 扫码点单（绑桌台会话）：菜单项为安全投影，不含内部结算价。
export type BarScanMenuItem = {
  productId: string;
  productName: string;
  productType: string;
  priceCent: number;
  currency: string;
  soldOut: boolean;
};

// ROOT-03-R7：扫码点单待付单（付款成功前不成单；barOrderId 在付款确认成功后才有）。
export type BarScanOrder = {
  scanOrderId: string;
  tableId: string;
  amountCent: number;
  status: string;
  barOrderId?: string | null;
  paymentIntentId: string;
  gatewayStatus?: string;
};

// 积分商城：兑换目录项（安全投影，不含库存定位等内部字段）+ 兑换记录。
export type RedemptionItem = {
  id: string;
  itemName: string;
  itemType: string;
  pointsCost: number;
  stockQuantity: number;
  status: string;
};

export type PointsRedemption = {
  id: string;
  itemName: string;
  quantity: number;
  totalPoints: number;
  status: string;
  createdAt: string;
};

// 我的券包聚合行（对齐后端 VoucherOverviewRecord；刻意不含内部结算价/成本字段）。
export type CustomerVoucher = {
  id: string;
  source: string; // unified | pass_partner
  name: string;
  voucherKind: string; // merchant_discount | in_store_goods | external_partner
  customerId: string;
  status: string;
  remainingTimes: number | null;
  expiresAt: string | null;
  faceValueCent: number | null;
  redeemMethod: string | null; // in_store_staff | onsite_code | external_reflow
  // V312：统一台账券的核销码（出示给店员/核销点扫码核销）；通票第三方券为 null。可选以兼容旧 fixture。
  code?: string | null;
};

// V313 券转赠记录（后端 VoucherTransferResponse 安全投影；claimCode 仅发起人视图携带）。
export type VoucherTransfer = {
  id: string;
  voucherInstanceId: string;
  voucherName: string | null;
  channel: string; // phone | link
  status: string; // pending | claimed | revoked | voided
  claimCode?: string | null;
  receiverPhoneMasked?: string | null;
  voidReason?: string | null;
  createdAt: string | null;
  claimedAt?: string | null;
};

// TK-21-R3（BSM「账户注销」：注销前提醒名下储值卡余额）：注销预览每张卡的本金/赠送金（后端 CustomerDeregisterPreviewResponse.StoredValueCard）。
export type DeregisterPreviewCard = {
  accountId: string;
  cardScope: string; // project=综合体通用卡 | merchant=商户级卡
  merchantId: string | null;
  principalCent: number;
  bonusCent: number;
};

// ROOT-03-R21（BSM 第 863 行「发起注销时系统须提醒其名下尚有存酒与储值卡余额」）：注销预览＝各卡余额＋名下在保管存酒（与「我的存酒」同一投影）。
export type DeregisterPreview = {
  storedValueCards: DeregisterPreviewCard[];
  savedBottles: { id: string; barProjectId: string; bottleName: string; remainingQuantity: number; unit: string; expireAt: string | null }[];
};
