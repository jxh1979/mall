// 文件版本：V0.08
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
  // ROOT-03-R27（BSM「多种资格价不叠加，选择最终价格最低的一项」）：带会话时今日按本人资格系统判定的价格与命中的资格名称；匿名为空。
  myPriceCent?: number | null;
  myPriceBasis?: string | null;
  // clients-R9（BSM「购票前须以门票守则的形式向客人告知该票的规则，包括能否退款、有效期、年龄限制与可使用范围」）。
  refundable?: boolean;
  validityDays?: number | null;
  usageScope?: string | null;
  entitlements?: string[];
  ageLimitText?: string | null;
  // PGM「可以按全局、综合体……开关」：票种所属综合体（与下单同一口径，不唯一为空），购票请求带 X-Project-Id。
  projectId?: string | null;
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
  // AUD-077 转赠待领取标记（"transferring"）；clients-R40 起发起转赠不冻结，原持有人照常可出示与核销、可改转他人。
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
  // clients-R40（BSM「转出时，系统须同时向转出人与受赠人明示这一场次限制」）：多人票同票已有人入场＝本人份只能在该场次内入场。
  sessionRestricted?: boolean;
  sessionLabel?: string | null;
  sessionEnded?: boolean;
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
  // clients-R40：sessionEnded＝该人份权益所在场次已结束、权益已终结，不发生领取。
  sessionRestricted?: boolean;
  sessionLabel?: string | null;
  sessionEnded?: boolean;
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
  // ROOT-03-R20（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」）：订单视图的已付、待付（分）与已收各笔；
  // 下单接口只回订单原字段，此时待付＝订单金额。
  paidAmountCent?: number;
  remainingCent?: number;
  payments?: { channel: string; amountCent: number; status: string; paidAt: string | null }[];
};

// ROOT-03-R3：当前会员条款与隐私条款（版本随正文变化，注册时回传两份版本）。
export type CustomerTermsDocument = { version: string; content: string };
export type CustomerTerms = { memberTerms: CustomerTermsDocument; privacyPolicy: CustomerTermsDocument };

// clients-R9 客人端首页：启用中的综合体、各场地营业时间（dayOfWeek 0＝周日）、经审批发布的介绍与公告（第十三批裁决第 5 条，未发布为空）。
export type CustomerBusinessHours = { venueId: string; venueName: string; dayOfWeek: number; openTime: string; closeTime: string };
export type CustomerProject = {
  projectId: string;
  projectName: string;
  address: string | null;
  intro: string | null;
  announcement: string | null;
  businessHours: CustomerBusinessHours[];
};

// clients-R9（BSM「连接游乐系统，展示可预定项目和场次」）：开放中的游乐项目及尚未结束（待开或开放中）的场次（后端 CustomerShowcaseService.AmusementProject）。
export type AmusementSession = { sessionId: string; startTime: string; endTime: string; capacity: number; usedCapacity: number; status: string };
export type AmusementProject = { amusementProjectId: string; projectName: string; openStatus: string; sessions: AmusementSession[] };

// OBS-13-01 会员等级通知（BSM L820）：downgrade_warning＝降级预警（targetLevel＝按发出当日成长值测算将降至的等级，cycleEnd＝保护期结束）；
// level_kept＝已保级；downgraded＝降级生效后补发（targetLevel＝新等级）。等级名称为等级配置的中文名。
export type LevelNotice = { id: string; noticeType: "downgrade_warning" | "level_kept" | "downgraded"; currentLevel: string; currentLevelName: string;
  targetLevel: string; targetLevelName: string; cycleEnd: string | null; createdAt: string };

// clients-R10 未核销票到期提醒（ticketId＝我的票的权益编号）。
export type TicketExpiryReminder = { ticketId: string; parentTicketId: string | null; ticketName: string | null; orderId: string | null; validTo: string };

// ROOT-03-R29 我的票使用记录（结果、时间、核销商户）与第三方渠道接入状态。
export type TicketUsage = { result: string; resultLabel: string; verifiedAt: string; merchantName: string | null };
export type TicketChannelStatus = { channel: string; name: string; enabled: boolean };

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
  // ROOT-03-R19：台位正被有效预留占着（照常列出、不可下单）。clients-R9（BSM「订台页面须写明该卡座适用的最低消费金额与服务费比例」）：
  // 服务费比例（基点；卡座与包厢取所属项目当日已批准的收费规则，未配置为空；散台与吧台位为 0）；应付定金（按本酒吧定金规则现算，
  // 0＝本酒吧未配定金规则、不开放自助订台——服务端 409 BAR_SELF_RESERVATION_UNAVAILABLE）。
  reserved?: boolean;
  serviceFeeRateBps?: number | null;
  depositAmountCent?: number;
  // PGM「可以按全局、综合体……开关」：台位所属酒吧的综合体，订台请求带 X-Project-Id。
  projectId?: string | null;
};

// clients-R6「我的订台」（后端 BarCustomerInvitationService.ReservationView）：invitationStatus none 未签发／active 已签发／revoked 已失效。
export type CustomerBarReservation = {
  reservationId: string;
  barId: string;
  barName: string | null;
  tableId: string;
  tableName: string | null;
  tableType: string | null;
  reservedAt: string | null;
  latestArrivalAt: string | null;
  guestCount: number;
  depositAmountCent: number;
  depositStatus: string;
  status: string;
  invitationStatus: string;
};

// 邀请函二维码载荷（口令、防伪标记、卡座、可进人数，预览与接受都原样回传核验，ROOT-03-R25）。
export type BarInvitationCode = { invitationToken: string; invitationSignature: string; tableId: string; capacity: number };

// clients-R6 扫邀请函码预览（不占名额）。
export type BarInvitationPreview = {
  reservationId: string;
  barId: string;
  barName: string | null;
  tableId: string;
  tableName: string | null;
  reservedAt: string | null;
  capacity: number;
  remaining: number;
  // 第十八批 R 段第 152 条（第十八批裁决 34「客户点击后确定自己是订台人还是参与者」）：这张订台还没有订台人（员工代订、一体机订台尚未被认领），可选「我是订台人」。
  holderClaimable: boolean;
};

// clients-R6 本人动态入场码（明文只在内存里，按服务端有效期显示）。
export type BarInvitationEntryCode = { reservationId: string; memberId: string; code: string; expiresAt: string; refreshIntervalSeconds: number };

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
  // ROOT-03-R30（BSM「每笔充值（……支付通路）、每笔扣款（商户……）、每笔退款与回冲」）：充值支付通路、扣款商户（通票为空＝综合体）、回冲所指原流水。
  paymentChannel?: string | null;
  merchantId?: string | null;
  merchantName?: string | null;
  originalTxnId?: string | null;
};

// ROOT-03-R30（ENUM「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」）：充值前规则（三段原句由服务端给）。
export type StoredValueTopupRules = {
  cardScope: string;
  projectId: string | null;
  merchantId: string | null;
  bonusBps: number;
  consumeOrder: string;
  consumeOrderText: string;
  bonusRuleText: string;
  refundRuleText: string;
  refundAllowed: boolean | null;
  refundRatioBps: number | null;
};

// ROOT-03-R31 商户储值卡退出公告（ENUM：可用到哪天、能否退、退多少比例、怎么退、找谁退；holdsCard＝本人持该商户未销户的卡）。
export type MerchantCardExitNotice = {
  id: string;
  merchantId: string;
  merchantName: string | null;
  projectId: string | null;
  useUntil: string;
  refundable: boolean;
  refundRatioBps: number;
  refundMethod: string | null;
  refundContact: string | null;
  publishFrom: string;
  publishUntil: string;
  holdsCard: boolean;
};

// BSM「我的存酒」安全投影（每条标明所属酒吧；handedOverToPlatform＝门店退出后由平台公司接收，pickupLocation 为指定领取地点）。
export type CustomerSavedBottle = {
  id: string;
  barProjectId: string;
  barName?: string | null;
  bottleName: string;
  remainingQuantity: number;
  unit: string;
  expireAt: string | null;
  status?: string;
  bottleLabelValue?: string | null;
  pickupLocation?: string | null;
  handedOverToPlatform?: boolean;
};

export type SavedBottlePickupCode = { savedBottleId: string; code: string; expiresAt: string; refreshIntervalSeconds: number };
export type SavedBottleRenewal = { id: string; savedBottleId: string; newExpireAt: string; status: string };

// 第十八批 M 段第 111 条 111-R4：投诉表单「涉及门店」选项（后端 CustomerComplaintService.StoreOption）。
export type ComplaintStoreOption = { merchantId: string; merchantName: string };

// clients-R8 我的投诉（后端 CustomerComplaintService.ComplaintView）。
export type CustomerComplaint = {
  workOrderId: string;
  code: string;
  title: string;
  description: string | null;
  status: string;
  statusLabel: string;
  submittedAt: string;
  resolution: string | null;
  satisfaction: number | null;
  mediaCount: number;
};

// R28 我的学生认证（status pending／approved／rejected；validUntil＝审核人按学信网报告填写的预计毕业日期）。
export type StudentCertification = {
  id: string;
  status: string;
  statusLabel: string;
  validUntil: string | null;
  rejectReason: string | null;
  submittedAt: string;
  reviewedAt: string | null;
};

// BSM「采集证件……前先单独取得同意，并留同意版本、时间和范围」：同意记录（后端 CustomerConsentRecord）。
export type CustomerConsent = { id: string; consentType: string; accepted: boolean; version: string | null; acceptedAt: string | null };

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

// ROOT-03-R7：扫码点单待付单（付款成功前不成单；barOrderId 在付款确认成功后才有）；下单返回带渠道签发的拉起参数（同在线付款发起）。
export type BarScanOrder = {
  scanOrderId: string;
  tableId: string;
  amountCent: number;
  status: string;
  barOrderId?: string | null;
  paymentIntentId: string;
  channel?: string | null;
  gatewayStatus?: string | null;
  paymentUrl?: string | null;
  launch?: PaymentClientLaunch | null;
};

// ROOT-03-R7 本桌当前账单的一行（只含该散台当前开台会话已成单的酒水，不含任何客人身份）。
export type BarTableBillItem = { productName: string; unitPriceCent: number; quantity: number; amountCent: number; served: boolean };

// 积分商城：兑换目录项（安全投影，不含库存定位等内部字段）+ 兑换记录。
export type RedemptionItem = {
  id: string;
  itemName: string;
  itemType: string;
  pointsCost: number;
  stockQuantity: number;
  status: string;
};

// BSM「积分明细」：积分流水（后端 CustomerPointsLedgerRecord；description 为后端内部说明，不直接展示）。
export type PointsLedgerEntry = {
  id: string;
  pointsDelta: number;
  balanceAfter: number;
  sourceType: string;
  changeType?: string | null;
  status?: string | null;
  description?: string | null;
  createdAt: string;
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
  // ROOT-03-R32 起客人端不再下发固定券码（恒为空），出示走动态核销码 getVoucherRedeemCode。
  code?: string | null;
  // ROOT-03-R30（BSM「已失效详情显示原因以及是否退回积分……可用券只有在券种允许时才显示转赠入口」）：作废原因、退回积分
  // （积分兑换券作废后退回的积分，非积分兑换券为空）、可转赠（券种允许且仍可用）。
  invalidReason?: string | null;
  pointsRefunded?: number | null;
  transferable?: boolean;
};

// ROOT-03-R32 券动态核销码（按全场统一刷新间隔过期）。
export type VoucherRedeemCode = { voucherInstanceId: string; code: string; expiresAt: string; refreshIntervalSeconds: number };

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
  savedBottles: CustomerSavedBottle[];
};

// ROOT-03-R18：在线付款发起的返回（后端 CustomerPaymentService.launchView）。launch 为渠道按付款端签发的拉起参数，
// kind 闭集 h5_url／app_wechat／app_alipay／miniapp_wechat，params 原样交给支付 SDK；渠道未启用或未配置时 launch 为空。
export type PaymentClientLaunch = { kind: string; params: Record<string, string> | null };

export type CustomerPaymentLaunchView = {
  paymentIntentId: string;
  businessOrderId: string;
  amountCent: number;
  channel: string | null;
  gatewayStatus: string | null;
  paymentUrl: string | null;
  launch: PaymentClientLaunch | null;
};

// ROOT-03-R2：本端（app/miniapp）及全端（*）已关闭的开关行（后端 TerminalCapabilitySwitchRecord）。
export type TerminalSwitchRow = {
  terminalType: string;
  capabilityCode: string;
  scopeType: string;
  scopeId: string;
  enabled?: boolean;
  reason?: string | null;
};
