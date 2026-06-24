// 客人端响应类型（对齐后端 customer 域 record，camelCase 化后口径）

export type TicketProduct = {
  ticketTypeId: string;
  ticketName: string;
  weekdayPriceCent: number | null;
  weekendPriceCent: number | null;
  holidayPriceCent: number | null;
  currency: string;
  status: string;
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
  projectId: string;
  balanceCent: number;
  status: string;
  createdAt: string;
  updatedAt: string;
};

export type Membership = {
  id: string;
  customerId: string;
  planId: string;
  projectId: string;
  startAt: string;
  expireAt: string;
  status: string;
  autoRenew: boolean;
  paymentIntentId: string | null;
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

export type BarScanOrder = {
  id: string;
  status: string;
  totalAmountCent: number;
  orderSource: string;
};
