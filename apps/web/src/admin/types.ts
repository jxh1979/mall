// 文件版本：V0.03
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
  | "settlement"
  | "vendor"
  | "governance"
  | "integration"
  | "access"
  | "risk"
  | "audit"
  | "compliance"
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
  getPaged<T>(path: string): Promise<{ items: T[]; total: number; page: number; size: number }>;
  post<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  put<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  patch<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  delete<T>(path: string): Promise<T>;
  /** multipart 上传（图片等）：不做 JSON 键转换，浏览器自动设 multipart 边界。可选=既有测试 mock 无需改。 */
  postForm?<T>(path: string, form: FormData): Promise<T>;
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
  /** M002-F49：重新读取当前账号的有效权限并更新 effectivePermissions，返回新值；页面改了本人权限后调用。AdminApp 注入。 */
  refreshEffectivePermissions?: () => Promise<EffectivePermissions>;
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
  paidAt?: string;
};

export type TicketRefundRow = {
  id: string;
  targetId?: string;
  refundAmountCent?: number;
  reason?: string;
  refundChannel?: string;
  status?: string;
  requestedBy?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt?: string;
  merchantId?: string;
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
  paidAt?: string;
  // 后端 BarBillSummary（G/V349）：开单时按员工岗位快照的提成角色档 + 提成台账真实落库费率/金额。
  // 页面显示提成比例一律读 commissionRateBp，不写死 15%/10%（配置中心改费率后页面须跟着变，否则对账对不上）。
  salesRoleType?: "sales" | "non_sales";
  commissionRateBp?: number | null;
  commissionAmountCent?: number | null;
};

export type BarOrderRow = {
  id: string;
  tableId?: string;
  // 后端 BarOrderRecord.sales_staff_id（adminClient 已 snake->camel）= 这单的销售归属人（员工 user_id）。
  // 注意：有归属人 ≠ 按销售人员费率计提——费率档看的是该员工在员工档案里有没有「销售岗」标识（G/V349），
  // 拆分统计一律读 bills[0].salesRoleType，不要拿本字段的有无当销售档判据（旧口径正是这么错的）。
  salesStaffId?: string;
  status?: string;
  /** 列表接口实际返回字段（BarOrderRecord.totalAmountCent，分）；totalAmount 为旧字段兼容保留。 */
  totalAmountCent?: number;
  totalAmount?: number;
  createdAt?: string;
  paidAt?: string;
  // 后端 BarOrderView.bills（每单最新在前）：账单号+支付状态，供账单列/结算/提成定位账单；修复前后端从不返回。
  bills?: BarBillRow[];
};

/**
 * V345（BAR-VOID-A1）退菜退酒申请流水（后端 BarOrderItemVoidRecord）。
 * approvalMode=approval_first 表示「先审后行」：审批通过前账单与库存一律未动（effectsAppliedAt 为空）；
 * immediate 表示低价值/出品前撤单的即时生效路径，审批为事后抽检、驳回才对称冲销。
 */
export type BarOrderItemVoidRow = {
  id: string;
  orderId?: string;
  orderItemId?: string;
  voidType?: string;
  quantity?: number;
  amountCent?: number;
  wasDelivered?: boolean;
  returnToStock?: boolean;
  reason?: string;
  status?: string;
  evidenceAttachmentIds?: string[];
  createdAt?: string;
  reviewedByUserId?: string;
  reviewedAt?: string;
  reviewReason?: string;
  approvalMode?: string;
  reasonCode?: string;
  highValueThresholdCent?: number;
  effectsAppliedAt?: string;
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
  /** V256 图形化点单 POS：分类 FK（null=未分类）+ 商品图片相对路径。 */
  categoryId?: string | null;
  imageUrl?: string | null;
  /** V262 存档/删除范式：archivedAt 非空=已存档；deleteStatus none|pending|approved。 */
  archivedAt?: string | null;
  deleteStatus?: string;
};

/** V256 酒吧商品分类（图形化点单 POS 阶段1）。 */
export type BarProductCategoryRow = {
  id: string;
  barId?: string;
  categoryName?: string;
  sortOrder?: number;
  status?: string;
  /** V262 存档/删除范式：archivedAt 非空=已存档；deleteStatus none|pending|approved。 */
  archivedAt?: string | null;
  deleteStatus?: string;
};

/** V257 酒吧套餐 + 明细。 */
export type BarComboItemRow = {
  id: string;
  productId: string;
  productName?: string;
  quantity: number;
};
export type BarComboRow = {
  id: string;
  barId?: string;
  comboName?: string;
  priceCent?: number;
  internalSettlementPriceCent?: number | null;
  imageUrl?: string | null;
  status?: string;
  items?: BarComboItemRow[];
  /** V262 存档/删除范式：archivedAt 非空=已存档；deleteStatus none|pending|approved。 */
  archivedAt?: string | null;
  deleteStatus?: string;
};

export type StoredWineRow = {
  id: string;
  savedNo?: string;
  customerId?: string;
  customerName?: string;
  itemId?: string;
  itemName?: string;
  /** 列表接口实际返回字段（SavedBottleRecord.remainingQuantity）；quantity 为旧字段兼容保留。 */
  remainingQuantity?: number;
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

/** 游乐项目↔权益类型绑定（V341 软删范式：存档轴 + 删除轴，delete_status='approved' 为墓碑）。 */
export type AmusementProjectEntitlementRow = {
  id: string;
  projectId?: string;
  entitlementType: string;
  createdAt?: string;
  archivedAt?: string | null;
  archivedBy?: string | null;
  archiveReason?: string | null;
  deleteStatus?: string | null;
  deleteRequestedBy?: string | null;
  deleteRequestedAt?: string | null;
  deleteReason?: string | null;
};

export type AmusementSessionRow = {
  id: string;
  projectId?: string;
  sessionName?: string;
  startsAt?: string;
  endsAt?: string;
  startTime?: string;
  endTime?: string;
  capacity?: number | string;
  usedCapacity?: number | string;
  lockedCapacity?: number | string;
  status?: string;
};

// V232 补时间第2批：游乐核销记录（核销时间 createdAt + 对应场次 sessionId + 核销结果）。
export type AmusementVerificationRow = {
  id: string;
  projectId?: string;
  sessionId?: string;
  deviceId?: string;
  staffId?: string;
  verificationResult?: string;
  verifiedPeople?: number | string;
  createdAt?: string;
  // 阶段3：出场/释放语义 + 关联通票号（LEFT JOIN ticket_verifications.verification_no）。
  direction?: string;
  releasedAt?: string;
  entryVerificationId?: string;
  entitlementId?: string;
  locationPointId?: string;
  linkedTicketNo?: string;
};

// 阶段3：并发在场容量快照（在场容量看板）。
export type AmusementCapacitySnapshotRow = {
  id: string;
  projectId?: string;
  sessionId?: string;
  locationPointId?: string;
  deviceId?: string;
  dimension?: string;
  capacityLimit?: number | string;
  occupiedCount?: number | string;
  updatedAt?: string;
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
  paidAt?: string;
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
  /** V296 成本台账（移动加权，整数分）；null=无成本基准（存量未录进货价，不编造）。 */
  unitCostCent?: number | null;
  totalCostCent?: number | null;
};

export type FinanceLedgerRow = {
  id: string;
  /** 后端 ar-ap-items 真实凭证号字段（item_no → itemNo）。 */
  itemNo?: string;
  billNo?: string;
  sourceDoc?: string;
  sourceId?: string;
  counterpartyId?: string;
  sourceType?: string; // ar_ap 来源类型（含退租相关 termination_penalty/deposit_refund）
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

export type BusinessReviewLockRow = {
  id?: string;
  projectId?: string | null;
  domain?: string;
  operatorNote?: string;
  metricCount?: number;
  lockedBy?: string | null;
  lockedAt?: string;
  idempotencyKey?: string;
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
  dispositionNote?: string | null;
  disposedBy?: string | null;
  disposedAt?: string | null;
  escalatedAlertId?: string | null;
};

// 异常处置历史时间线条目(append-only,来自 audit_events 的 disposition_recorded 事件)。
// who=authContext/actorId、when=occurredAt、from->to/说明/生成的告警号在 metadata 内。
export type AnomalyDispositionEventRow = {
  id?: string;
  occurredAt?: string;
  actorId?: string | null;
  authContext?: string | null;
  eventName?: string;
  // adminClient 深度 snake->camel:后端 metadata 键 from_status/to_status/escalated_alert_id 到前端已转驼峰。
  metadata?: {
    action?: string;
    fromStatus?: string;
    toStatus?: string;
    note?: string;
    escalatedAlertId?: string;
  } | null;
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
  sourceId?: string;
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

// F-OPS-08 物业费费率（项目级、可按期）。effectivePeriod 空=该项目默认费率。
export type PropertyFeeRateRow = {
  id: string;
  projectId?: string;
  effectivePeriod?: string | null;
  ratePerSquareMeterCent?: number;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

// F-OPS-08 物业费逐户计费留痕（不退不补；本次生成返回值）。
export type PropertyFeeBillRow = {
  id: string;
  projectId?: string;
  period?: string;
  merchantId?: string;
  areaSquareMeter?: number | string;
  ratePerSquareMeterCent?: number;
  amountCent?: number;
  arApItemId?: string;
  createdAt?: string;
};

export type MerchantExpenseEntryRow = {
  id: string;
  merchantId?: string;
  period?: string;
  expenseType?: string;
  amountCent?: number;
  source?: string;
  sourceRef?: string | null;
  note?: string | null;
  operatorUserId?: string | null;
  createdAt?: string;
};

export type MerchantExpenseSummaryRow = {
  merchantId?: string;
  period?: string;
  expenseType?: string;
  totalCent?: number;
};

// 自动挂接金额漂移行（上游改账后台账净额与上游当前金额不一致；处置=红字冲销旧行+手工录入修正额）。
export type MerchantExpenseDriftRow = {
  entryId?: string;
  merchantId?: string;
  source?: string;
  sourceRef?: string;
  ledgerNetCent?: number;
  upstreamAmountCent?: number;
};

export type MerchantExpenseSyncResult = {
  period?: string;
  energyInserted?: number;
  propertyInserted?: number;
  contractPropertyInserted?: number;
  rentInserted?: number;
  drift?: MerchantExpenseDriftRow[];
  // 分红期间关账锁跳过清单：该月被 approved/paid 正式分红单覆盖（关账）的商户 id，自动挂接对其跳过。
  lockedMerchants?: string[];
};

// 持股分红（V299）：方案配置 / 期间计算单 / 算式明细行 / 期间作业结果。
export type DividendPlanRow = {
  id: string;
  merchantId?: string;
  principalUserId?: string;
  principalName?: string | null;
  platformShareBps?: number;
  taxRateBps?: number;
  periodType?: string;
  revenueDomains?: string[];
  includeInventoryCost?: boolean;
  expenseTypes?: string[];
  effectiveFrom?: string | null;
  status?: string;
  note?: string | null;
  updatedBy?: string | null;
  createdAt?: string;
  updatedAt?: string;
  // V303 扣除项三开关 + 发放保护线参数。
  lossCarryforwardEnabled?: boolean;
  arrearsOffsetEnabled?: boolean;
  payoutFloorEnabled?: boolean;
  payoutFloorBps?: number;
  payoutFloorFixedCent?: number;
  // V304 发放保护线留存·释放规则。
  reserveReleaseMode?: string;
  reserveReleaseDelayPeriods?: number;
};

// V304 发放保护线留存·待释放/已释放台账行。
export type DividendReserveLedgerRow = {
  statementId?: string;
  merchantId?: string;
  principalUserId?: string;
  periodLabel?: string;
  periodStart?: string;
  periodEnd?: string;
  statementStatus?: string;
  reserveWithheldCent?: number;
  released?: boolean;
  triggerType?: string | null;
  releasedBy?: string | null;
  releasedAt?: string | null;
};

export type DividendStatementRow = {
  id: string;
  merchantId?: string;
  planId?: string;
  kind?: string;
  periodLabel?: string;
  periodStart?: string;
  periodEnd?: string;
  principalUserId?: string;
  platformShareBps?: number;
  taxRateBps?: number;
  revenueDomains?: string[];
  includeInventoryCost?: boolean;
  expenseTypes?: string[];
  revenueCent?: number;
  costCent?: number;
  expenseCent?: number;
  netProfitCent?: number;
  distributableCent?: number;
  platformShareCent?: number;
  principalGrossCent?: number;
  taxCent?: number;
  principalNetCent?: number;
  noCostBasisCount?: number;
  status?: string;
  rejectReason?: string | null;
  arApItemId?: string | null;
  createdAt?: string;
  updatedAt?: string;
  // V303 三扣除入账留痕：亏损结转链 / 欠款抵扣 / 留存 / 实发额（挂应付发放口径）。
  lossCarryforwardEnabled?: boolean;
  lossCarryforwardInCent?: number;
  lossCarryforwardAppliedCent?: number;
  lossCarryforwardAfterCent?: number;
  arrearsOffsetCent?: number;
  reserveWithheldCent?: number;
  payoutCent?: number;
};

export type DividendStatementLineRow = {
  id: string;
  statementId?: string;
  merchantId?: string;
  seq?: number;
  lineType?: string;
  amountCent?: number;
  detail?: string | null;
  createdAt?: string;
};

export type DividendStatementDetail = {
  statement?: DividendStatementRow;
  lines?: DividendStatementLineRow[];
};

export type DividendComputeSkipRow = {
  merchantId?: string;
  reason?: string;
};

export type DividendComputeResult = {
  periodLabel?: string;
  kind?: string;
  computed?: DividendStatementRow[];
  skipped?: DividendComputeSkipRow[];
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



// 平台域（platform）后端 JSON 为 snake_case（application.yml 全局 SNAKE_CASE），
// adminClient.unwrap 已把响应键统一转 camelCase——前端按「后端真实字段的 camel 形」读。
// 旧的臆造字段（name/code/orgType、displayName/companyName 等）后端不存在，保留 optional 供他页过渡，勿再新增读取。
export type BusinessEntityRow = {
  id: string;
  entityName?: string;
  entityRole?: string;
  unifiedSocialCreditCode?: string;
  legalRepresentative?: string;
  bankAccountRef?: string;
  status?: string;
  createdAt?: string;
  name?: string;
  code?: string;
  orgType?: string;
};

export type GovernanceProjectRow = {
  id: string;
  merchantId?: string;
  venueType?: string;
  name?: string;
  code?: string;
  floor?: string;
  capacity?: number;
  status?: string;
};

export type GovernanceSpaceRow = {
  id: string;
  projectId?: string;
  merchantId?: string;
  parentSpaceId?: string;
  spaceName?: string;
  spaceType?: string;
  floorNo?: string;
  areaSquareMeter?: number | string;
  status?: string;
  // 旧臆造字段（保留 optional 防他页引用报错）
  venueId?: string;
  areaId?: string;
  name?: string;
  code?: string;
  areaType?: string;
  pointType?: string;
};

export type MerchantAdmissionRow = {
  id: string;
  merchantName?: string;
  merchantType?: string;
  businessEntityId?: string;
  contractEntityId?: string;
  settlementEntityId?: string;
  contactName?: string;
  contactPhone?: string;
  ownerPerson?: string;
  mallBlockCode?: string;
  areaSquareMeter?: number | string;
  leasedAreaSquareMeter?: number | string;
  secondaryContactName?: string;
  secondaryContactPhone?: string;
  rentPerSquareMeter?: number | string;
  cooperationForm?: string;
  status?: string;
  createdAt?: string;
  admissionStep?: string;
  organizationId?: string;
  displayName?: string;
  companyName?: string;
  riskLevel?: string;
  auditEvidenceRef?: string;
  // V259 存档/删除范式：archivedAt 非空=已存档；deleteStatus none|pending|approved。
  archivedAt?: string | null;
  deleteStatus?: string;
};

export type SubjectBindingRow = {
  id: string;
  merchantId?: string;
  operationEntityId?: string;
  contractEntityId?: string;
  settlementEntityId?: string;
  status?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  changeReason?: string;
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

// 集中数据清理作业（Tier-D 集中清理工具；对应后端 DataCleanupJobRecord）。
export type DataCleanupJobRow = {
  id: string;
  scopeType: string;
  filterParams?: Record<string, unknown>;
  status: string;
  previewCount: number;
  previewSample?: { id: string; archivedAt?: string | null }[];
  reason?: string | null;
  requestedBy?: string | null;
  requestedAt?: string | null;
  approvedBy?: string | null;
  approvedAt?: string | null;
  executedAt?: string | null;
  deletedCount?: number | null;
};

// 真操作留痕行（audit_events 表；对应后端 AuditEvent record 的 JSON 字段）。
// 与 AuditEventRow（证据表 evidence.events）不同：这是「谁/何时/对什么对象/做了什么/结果」的操作日志。
export type AuditOperationEventRow = {
  id?: string;
  occurredAt?: string;
  eventName?: string;
  eventResult?: string;
  actorType?: string;
  actorId?: string;
  terminal?: string;
  permissionCode?: string;
  businessType?: string;
  businessId?: string;
  requestId?: string;
  eventCategory?: string;
  subjectType?: string;
  metadata?: Record<string, unknown>;
};

// KPI 统一明细（指标登记簿，2026-07-18 方案A）：GET /api/v1/analytics/metric-detail 响应。
export type MetricDetailColumn = { key: string; label: string };

export type MetricDetailPayload = {
  metricKey: string;
  name: string;
  valueType: "additive" | "ratio" | "balance";
  unit: string;
  caliber: string;
  source: string;
  part: "numerator" | "denominator" | null;
  from: string | null;
  to: string | null;
  dataAsOf: string;
  summaryValue: number | string | null;
  numeratorValue?: number | string | null;
  denominatorValue?: number | string | null;
  summaryNote?: string | null;
  columns: MetricDetailColumn[];
  rows: Array<Record<string, unknown>>;
  page: number;
  size: number;
  totalRows: number;
  traceLinkPageId?: string | null;
  traceLinkColumnKey?: string | null;
};

// ── 积分/券统一台账（V301 方案A）：定义/实例/核销记录/全部券聚合 ──────────
export type VoucherDefinitionRow = {
  id: string;
  name?: string;
  voucherKind?: string;
  redeemMethod?: string;
  settlementMode?: string;
  merchantId?: string | null;
  faceValueCent?: number | null;
  inventoryItemId?: string | null;
  stockLocationId?: string | null;
  stockQuantity?: number | null;
  internalPriceCent?: number | null;
  validDays?: number | null;
  validUntil?: string | null;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type VoucherInstanceRow = {
  id: string;
  definitionId?: string;
  customerId?: string;
  code?: string;
  sourceType?: string;
  sourceRef?: string | null;
  status?: string;
  issuedAt?: string;
  expiresAt?: string | null;
  redeemedAt?: string | null;
  revokedAt?: string | null;
  revokeReason?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type VoucherRedemptionRow = {
  id: string;
  voucherInstanceId?: string;
  definitionId?: string;
  voucherKind?: string;
  customerId?: string;
  merchantId?: string | null;
  operatorUserId?: string | null;
  redeemMethod?: string;
  settlementMode?: string;
  settlementAmountCent?: number | null;
  arApItemId?: string | null;
  stockMovementRef?: string | null;
  note?: string | null;
  redeemedAt?: string;
  createdAt?: string;
};

// "全部券"聚合行（读层并入：统一台账 ∪ 通票第三方合作券，计次模型带剩余次数）。
export type VoucherOverviewRow = {
  id: string;
  source?: string;
  name?: string;
  voucherKind?: string;
  customerId?: string | null;
  status?: string;
  remainingTimes?: number | null;
  expiresAt?: string | null;
};

// 财务退款：请求成功不等于实际退款成功。
export type ArApPaymentRow = { id: string; arApItemId: string; amountCent: number; paymentMethod: string; paymentTime: string; bankFlowRef?: string | null; createdAt: string };
export type FinanceRefundRow = {
  id: string; targetId: string; refundAmountCent: number; reason: string; refundChannel: string; refundDirection: string;
  status: "requested" | "processing" | "failed" | "success"; createdAt: string; sourceType: "ar_ap_payment" | "stored_value_account";
  merchantId?: string | null; projectId?: string | null; requestedBy: string; originalPaymentRef?: string | null;
  gatewayProvider?: string | null; gatewayRef?: string | null; updatedAt: string; succeededAt?: string | null;
};
export type FinanceRefundDetail = { refund: FinanceRefundRow; attempts: {
  id: string; refundId: string; operation: "submit" | "retry" | "query" | "execution" | "confirm"; status: string;
  gatewayProvider?: string | null; gatewayRef?: string | null; message?: string | null;
  paymentReference?: string | null; paymentOperatorId?: string | null; verifiedBy?: string | null;
  evidenceAttachmentIds: string[]; createdAt: string;
}[] };
export type ContractRentBillRow = { id: string; contractId: string; merchantId: string; projectId: string; period: string; feeType: string; amountCent: number; arApItemId: string; createdAt: string; turnoverRuleId: string | null };
