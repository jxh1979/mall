export type AdminUser = {
  id: string;
  username: string;
  accountType: string;
  organizationId?: string | null;
  merchantId?: string | null;
  permissions?: string[];
};

export type AdminRoute = {
  pageId: string;
  label: string;
  route: string;
  module: string;
  requiredPermissions: string[];
};

export type AdminApiClient = {
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  put<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  patch<T>(path: string, body?: Record<string, unknown>): Promise<T>;
};

export type AdminPageProps = {
  api: AdminApiClient;
  user: AdminUser;
  setActionResult: (value: unknown) => void;
  setGlobalError: (value: string | null) => void;
};

export type OverviewPayload = {
  merchants?: number;
  ticketProducts?: number;
  issuedTickets?: number;
  verifications?: number;
  barOrders?: number;
  amusementSessions?: number;
  openRiskAlerts?: number;
  auditEvents?: number;
};

export type AccessUserRow = {
  id: string;
  username: string;
  accountType: string;
  status?: string;
  organizationId?: string | null;
};

export type PermissionTemplateRow = {
  id: string;
  templateName?: string;
  name?: string;
  templateType?: string;
  status?: string;
  validUntil?: string | null;
  singleUse?: boolean;
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
  billNo?: string;
  sourceDoc?: string;
  counterparty?: string;
  direction?: string;
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
