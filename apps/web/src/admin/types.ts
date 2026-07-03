export type AdminUser = {
  id: string;
  username: string;
  accountType: string;
  organizationId?: string | null;
  merchantId?: string | null;
};

export type EffectivePermissions = {
  grants: string[];
  denies: string[];
  wildcard: boolean;
  computedAt?: string;
};

export type EffectivePermissionsState =
  | { status: "loading" }
  | { status: "ready"; value: EffectivePermissions }
  | { status: "error" };

// 导航业务域分组键（frontend-admin §4.3：菜单按业务域聚合，避免 30 项平铺）。
export type AdminNavGroup =
  | "overview"
  | "merchant"
  | "ticketing"
  | "bar"
  | "amusement"
  | "inventory"
  | "finance"
  | "vendor"
  | "governance"
  | "integration"
  | "access"
  | "risk"
  | "audit"
  | "ops";

/**
 * 账号形态（用户裁决：菜单按形态直接隐藏）：
 * - platform：平台/员工账号（无 merchantId、非外包）；
 * - merchant：商户归属账号（user.merchantId 非空）；
 * - vendor：外包机构账号（accountType=outsourced_vendor）。
 */
export type AdminAccountForm = "platform" | "merchant" | "vendor";

export type AdminRoute = {
  pageId: string;
  label: string;
  route: string;
  module: string;
  /** 导航业务域分组键；缺省时回落到 module 文案展示。 */
  group?: AdminNavGroup;
  requiredPermissions: string[];
  /** 仅这些账号形态可见/可进入；缺省=全部形态。不匹配的账号菜单不渲染，直链显示业务化提示。 */
  accountForms?: AdminAccountForm[];
};

export type AdminApiClient = {
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  put<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  patch<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  delete<T>(path: string): Promise<T>;
};

export type AdminPageProps = {
  api: AdminApiClient;
  user: AdminUser;
  setActionResult: (value: unknown) => void;
  setGlobalError: (value: string | null) => void;
  /**
   * 当前账号的有效权限状态，供页面做按钮级权限门控（useCan / PermissionButton /
   * HighRiskConfirm 的 permissionsState）。AdminApp 注入；缺省视为 loading（fail-safe）。
   */
  effectivePermissions: EffectivePermissionsState;
  /** 全链下钻：跳到承载某对象的后台页并定位（TraceLink 用）。AdminApp 注入。 */
  navigateTo?: (pageId: string, objectId?: string) => void;
  /** 下钻进入本页时携带的对象 id（URL ?focus=），供本页定位/高亮/过滤。 */
  focusObjectId?: string;
};

// T12 经营看板：以 metric_code 为权威键的指标 map（与后端 OverviewResponse 对齐）。
// value 可能为 null（数据源未就绪/源表无 merchant_id 维度，不伪造）。
export type MetricValue = {
  value: number | null;
  unit?: string | null;
  note?: string | null;
  dimension?: Record<string, unknown> | null;
};

export type OverviewPayload = {
  asOf: string;
  scope: string | null;
  businessDate: string;
  metrics: Record<string, MetricValue>;
};

// P2 集团/跨项目汇总（R6-D13）：按 project 维 rollup（当前单综合体即单 project）。
export type GroupProjectRollup = {
  projectId: string;
  projectName: string;
  merchantCount: number;
  metrics: Record<string, MetricValue>;
  belowBenchmarkMetrics?: string[];
};

export type GroupOverviewPayload = {
  asOf: string;
  businessDate: string;
  groupBy?: string;
  groupAverages?: Record<string, MetricValue>;
  projects: GroupProjectRollup[];
};

export type AccessUserRow = {
  id: string;
  username: string;
  accountType: string;
  status?: string;
  phone?: string | null;
  phoneHash?: string | null;
  organizationId?: string | null;
  merchantId?: string | null;
  createdVia?: string | null;
  consentReference?: string | null;
  evidenceAttachmentIds?: string[];
  operatorNote?: string | null;
};

export type PermissionDefinitionRow = {
  permissionCode?: string;
  code?: string;
  permissionName?: string;
  name?: string;
  domain?: string;
  riskLevel?: string;
  requiresApproval?: boolean;
  status?: string;
};

export type PermissionTemplateRow = {
  id: string;
  templateCode?: string;
  templateName?: string;
  name?: string;
  description?: string | null;
  templateType?: string;
  sourceTemplateId?: string | null;
  status?: string;
  validFrom?: string | null;
  validUntil?: string | null;
  singleUse?: boolean;
  permissionCodes?: string[];
  permissions?: string[];
};



export type TicketOrderRow = {
  id: string;
  orderNo?: string;
  ticketTypeId?: string;
  quantity?: number;
  visitDate?: string;
  salesChannel?: string;
  amountCent?: number;
  baseAmountCent?: number;
  dynamicAdjustmentCent?: number;
  dynamicPricingRuleId?: string;
  payStatus?: string;
  issueStatus?: string;
  status?: string;
  createdAt?: string;
};

export type TicketIssuedEntitlementRow = {
  id: string;
  entitlementCode?: string;
  entitlementType?: string;
  totalTimes?: number;
  usedTimes?: number;
  valueAmount?: number;
  status?: string;
  createdAt?: string;
};

export type TicketVerificationRow = {
  id: string;
  verificationNo?: string;
  status?: string;
  source?: string;
  createdAt?: string;
};

export type TicketDynamicPricingRuleRow = {
  id: string;
  ticketTypeId?: string;
  ruleName?: string;
  priority?: number;
  effectiveFrom?: string;
  effectiveTo?: string;
  salesChannel?: string;
  adjustmentType?: string;
  adjustmentRate?: number;
  minPriceCent?: number;
  maxPriceCent?: number;
  status?: string;
};

export type TicketDynamicPricingSimulationResult = {
  ticketTypeId?: string;
  visitDate?: string;
  salesChannel?: string;
  basePriceCent?: number;
  adjustmentCent?: number;
  finalPriceCent?: number;
  matchedRuleId?: string;
  explanation?: string;
};

export type BarTableRow = {
  id: string;
  tableCode?: string;
  tableName?: string;
  capacity?: number;
  guestLimit?: number;
  barId?: string;
  status?: string;
};

export type BarReservationRow = {
  id: string;
  barId?: string;
  tableId?: string;
  customerId?: string;
  guestCount?: number;
  allowedPassFreeGuestCount?: number;
  depositStatus?: string;
  status?: string;
  reservedAt?: string;
  createdAt?: string;
};

export type BarTableSessionRow = {
  id: string;
  barId?: string;
  merchantId?: string;
  businessDate?: string;
  tableId?: string;
  reservationId?: string;
  customerId?: string;
  guestCount?: number;
  status?: string;
  salesStaffId?: string;
  serviceStaffId?: string;
  openedAt?: string;
  closedAt?: string | null;
  sessionVersion?: number;
};


export type BarBillRow = {
  id?: string;
  billNo?: string;
  status?: string;
  paidAmount?: number;
  totalAmount?: number;
};

export type BarOrderRow = {
  id: string;
  orderNo?: string;
  tableId?: string;
  billId?: string | null;
  sellerRole?: string;
  commissionRate?: string;
  status?: string;
  totalAmount?: number;
  createdAt?: string;
  bills?: BarBillRow[];
};

export type BarProductRow = {
  id: string;
  barId?: string;
  productName?: string;
  productType?: string;
  priceCent?: number;
  currency?: string;
  status?: string;
  soldOut?: boolean;
  /** V152 内部结算价（分）：平台↔酒吧成本结算价基；null/未录入=不参与结算/提成扣减。 */
  internalSettlementPriceCent?: number | null;
};

export type StoredWineRow = {
  id: string;
  savedNo?: string;
  customerId?: string;
  customerName?: string;
  itemId?: string;
  itemName?: string;
  quantity?: number;
  status?: string;
  createdAt?: string;
};

export type ContractRow = {
  id: string;
  admissionStep?: string;
  merchantId?: string;
  contractSubjectId?: string;
  operationSubjectId?: string;
  settlementSubjectId?: string;
  effectiveDate?: string;
  expiryDate?: string;
  rentTerms?: Record<string, unknown>;
  shareholderRatioForReport?: number;
  attachmentIds?: string[];
  evidenceAttachmentIds?: string[];
  lifecycleStatus?: string;
  status?: string;
  auditEvidenceRef?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type SupervisorReviewPolicyRow = {
  id: string;
  policyCode?: string;
  actionType?: string;
  minAmountCent?: number;
  requiredLevel?: number;
  blocksByDefault?: boolean;
  requiredEvidenceTypes?: string[];
  status?: string;
};


export type AmusementProjectRow = {
  id: string;
  projectCode?: string;
  projectName?: string;
  capacity?: number | string;
  status?: string;
};

export type AmusementSessionRow = {
  id: string;
  projectId?: string;
  sessionName?: string;
  startsAt?: string;
  endsAt?: string;
  capacity?: number | string;
  usedCapacity?: number | string;
  lockedCapacity?: number | string;
  status?: string;
};

export type AmusementGroupBookingRow = {
  id: string;
  projectId?: string;
  sessionId?: string;
  bookingType?: string;
  customerName?: string;
  partySize?: number | string;
  reservedCapacity?: number | string;
  bookingStatus?: string;
  evidenceAttachmentIds?: string[];
  createdAt?: string;
  updatedAt?: string;
};

export type AmusementSafetyInspectionRow = {
  id: string;
  inspectionNo?: string;
  projectId?: string;
  projectName?: string;
  templateId?: string;
  items?: Array<Record<string, unknown>>;
  overallResult?: string;
  riskLevel?: string;
  inspectionStatus?: string;
  rectificationStatus?: string;
  reopenStatus?: string;
  status?: string;
  inspectedAt?: string;
  submittedAt?: string | null;
  recheckedAt?: string | null;
  approvedAt?: string | null;
  operatorNote?: string | null;
  evidenceAttachmentIds?: string[];
  attachmentIds?: string[];
  createdAt?: string;
  updatedAt?: string;
};

export type InventoryItemRow = {
  id: string;
  itemCode?: string;
  itemName?: string;
  category?: string;
  unit?: string;
  status?: string;
  minQuantity?: number;
};

export type StockBalanceRow = {
  id: string;
  itemId?: string;
  locationId?: string;
  locationType?: string;
  quantity?: number;
  status?: string;
};

export type StockMovementRow = {
  id?: string;
  itemId?: string;
  quantity?: number;
  movementType?: string;
  status?: string;
  createdAt?: string;
};

export type FinanceLedgerRow = {
  id: string;
  /** 后端 ar-ap-items 真实凭证号字段（item_no → itemNo）。 */
  itemNo?: string;
  billNo?: string;
  sourceDoc?: string;
  counterparty?: string;
  direction?: string;
  /** 单位：分（amount_cent / paid_amount_cent / unpaid_amount_cent）。 */
  amountCent?: number;
  paidAmountCent?: number;
  unpaidAmountCent?: number;
  dueDate?: string;
  totalAmount?: number;
  amount?: number;
  paidAmount?: number;
  status?: string;
};


export type ReconciliationBatchRow = {
  id: string;
  batchNo?: string;
  amount?: number;
  totalAmount?: number;
  status?: string;
  providerCode?: string;
};

export type ReconciliationDifferenceRow = {
  id: string;
  differenceNo?: string;
  amount?: number;
  differenceAmount?: number;
  diffAmount?: number;
  status?: string;
  providerCode?: string;
};

export type MerchantWorkOrderRow = {
  id: string;
  merchantId?: string;
  spaceId?: string;
  contractId?: string;
  workOrderType?: string;
  priority?: string;
  status?: string;
  title?: string;
  description?: string;
  reportedByUserId?: string;
  reportedChannel?: string;
  ownerDepartment?: string;
  ownerUserId?: string;
  dueAt?: string;
  closedAt?: string | null;
  resolution?: string | null;
  satisfactionScore?: number | null;
  evidenceAttachmentIds?: string[];
  createdAt?: string;
  updatedAt?: string;
};

export type ProviderAdapterRow = {
  id: string;
  providerCode?: string;
  adapterType?: string;
  mode?: string;
  credentialSecretRef?: string;
  credentialRef?: string;
  status?: string;
  productionReady?: boolean;
};

export type CapabilitySwitchRow = {
  id: string;
  terminalType: string;
  capabilityCode: string;
  scopeType: string;
  scopeId: string;
  enabled: boolean;
  reason?: string | null;
};

export type IntegrationEventRow = {
  id?: string;
  taskNo?: string;
  orderNo?: string;
  verificationNo?: string;
  providerCode?: string;
  channelCode?: string;
  eventType?: string;
  syncType?: string;
  verificationType?: string;
  status?: string;
  retryCount?: number;
  rawPayloadHash?: string;
  errorCode?: string;
  occurredAt?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type RiskAlertRow = {
  id: string;
  ruleId?: string;
  alertType?: string;
  riskLevel?: string;
  objectType?: string;
  objectId?: string;
  summary?: string;
  ownerUserId?: string;
  status?: string;
  evidenceAttachmentIds?: string[];
  createdAt?: string;
  updatedAt?: string;
  resolvedAt?: string | null;
};

export type AuditEventRow = {
  id?: string;
  eventType?: string;
  event_type?: string;
  eventCategory?: string;
  event_category?: string;
  result?: string;
  riskLevel?: string;
  risk_level?: string;
  sourceType?: string;
  source_type?: string;
  summary?: string;
  traceId?: string;
  trace_id?: string;
  requestId?: string;
  request_id?: string;
  objectType?: string;
  object_type?: string;
  objectId?: string;
  object_id?: string;
  targetType?: string;
  target_type?: string;
  targetId?: string;
  target_id?: string;
  beforeAfter?: Record<string, unknown>;
  before_after?: Record<string, unknown>;
  payload?: Record<string, unknown>;
  occurredAt?: string;
  occurred_at?: string;
  receivedAt?: string;
  received_at?: string;
};

export type BusinessMetricSnapshotRow = {
  snapshotId?: string;
  metricKey?: string;
  domain?: string;
  periodType?: string;
  businessDate?: string;
  segmentType?: string;
  segmentId?: string | null;
  actualValue?: number | string;
  baselineValue?: number | string;
  varianceRate?: number | string;
  trendDirection?: string;
  projectionMethod?: string;
  projectedValue?: number | string;
  confidenceScore?: number | string;
  payload?: Record<string, unknown>;
  createdAt?: string;
};

export type MetricDefinitionRow = {
  metricKey: string;
  name?: string;
  domain?: string;
  unit?: string;
  calculationNote?: string;
  ownerRole?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type MetricAnomalyRow = {
  id: string;
  metricKey?: string;
  domain?: string;
  riskLevel?: string;
  observedValue?: number | string;
  expectedValue?: number | string;
  summary?: string;
  status?: string;
  detectedAt?: string;
  resolvedAt?: string | null;
};

export type MerchantScopeWorkOrderRow = {
  id: string;
  merchantId?: string;
  workOrderType?: string;
  priority?: string;
  status?: string;
  title?: string;
  authorizedScope?: string;
  auditEvidenceRef?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type MerchantZoneBillLineRow = {
  id: string;
  feeType?: string;
  description?: string;
  amountCent?: number;
  currency?: string;
};

export type MerchantZoneBillRow = {
  id: string;
  merchantId?: string;
  billNo?: string;
  billPeriodStart?: string;
  billPeriodEnd?: string;
  dueDate?: string;
  totalAmountCent?: number;
  currency?: string;
  status?: string;
  agingDays?: number;
  sourceType?: string;
  lines?: MerchantZoneBillLineRow[];
  createdAt?: string;
  updatedAt?: string;
};

export type MerchantZoneFeeRecipientRow = {
  id: string;
  merchantId?: string;
  feeEvent?: string;
  recipientName?: string;
  recipientPhone?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type MerchantZoneSettlementRow = {
  id: string;
  contractId?: string;
  triggerType?: string;
  status?: string;
  settlementItems?: unknown[];
  disputeReason?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type MerchantZoneContractRow = {
  id: string;
  merchantId?: string;
  effectiveDate?: string;
  expiryDate?: string;
  lifecycleStatus?: string;
  depositCent?: number | null;
  projectId?: string;
  spaceIds?: string[];
  renewedFromContractId?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type MerchantScopeBillRow = {
  id: string;
  merchantId?: string;
  billNo?: string;
  title?: string;
  totalAmount?: number | string;
  status?: string;
  authorizedScope?: string;
  auditEvidenceRef?: string;
  createdAt?: string;
  updatedAt?: string;
};



export type BusinessEntityRow = {
  id: string;
  name?: string;
  code?: string;
  orgType?: string;
  status?: string;
  createdAt?: string;
};

export type GovernanceProjectRow = {
  id: string;
  merchantId?: string;
  name?: string;
  code?: string;
  venueType?: string;
  status?: string;
};

export type GovernanceSpaceRow = {
  id: string;
  venueId?: string;
  areaId?: string;
  name?: string;
  code?: string;
  areaType?: string;
  pointType?: string;
  status?: string;
};

export type MerchantAdmissionRow = {
  id: string;
  admissionStep?: string;
  organizationId?: string;
  displayName?: string;
  companyName?: string;
  mallBlockCode?: string;
  areaSquareMeter?: number | string;
  leasedAreaSquareMeter?: number | string;
  secondaryContactName?: string;
  rentPerSquareMeter?: number | string;
  cooperationForm?: string;
  status?: string;
  riskLevel?: string;
  auditEvidenceRef?: string;
};

export type SubjectBindingRow = {
  id: string;
  admissionStep?: string;
  businessObjectId?: string;
  operatorSubject?: string;
  contractSubject?: string;
  contractSubjectId?: string;
  settlementSubject?: string;
  settlementSubjectId?: string;
  status?: string;
  auditEvidenceRef?: string;
};

export type ReadinessItemRow = {
  id: string;
  blockerCode?: string;
  title?: string;
  owner?: string;
  status?: string;
  evidenceRef?: string | null;
  dueAt?: string | null;
};
