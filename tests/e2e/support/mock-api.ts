// 文件版本：V0.06
import type { Page, Route } from "@playwright/test";

type ApiData = Record<string, unknown> | Array<Record<string, unknown>> | null;

type MockUser = {
  id?: string;
  username?: string;
  accountType?: string;
  organizationId?: string | null;
  merchantId?: string | null;
};

type MockOptions = {
  empty?: boolean;
  authFailure?: boolean;
  user?: MockUser;
  forbiddenPath?: string;
  forbiddenPaths?: string[];
  errorPath?: string;
  errorPaths?: string[];
  postForbiddenPaths?: string[];
  postErrorPaths?: string[];
};

const now = "2026-06-15T12:00:00.000Z";

// Generates filler rows so the high-volume log tables have more than one page of data.
// Named/original rows are always listed first, so existing content assertions stay on page 1.
function fillRows<T>(count: number, factory: (index: number) => T): T[] {
  return Array.from({ length: count }, (_, index) => factory(index));
}

function envelope(data: ApiData) {
  return { code: "OK", message: "ok", data };
}

function json(route: Route, data: ApiData, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(status >= 400 ? data : envelope(data)),
  });
}

function pathMatches(pathname: string, patterns: Array<string | undefined>) {
  return patterns.filter(Boolean).some((pattern) => pathname.includes(pattern as string));
}

function mockUser(user?: MockUser) {
  return {
    id: user?.id ?? "user-1",
    username: user?.username ?? "admin",
    accountType: user?.accountType ?? "platform_admin",
    organizationId: user?.organizationId ?? "org-1",
    merchantId: user?.merchantId ?? null,
  };
}

function ticketProduct(id: string, name: string, priceAmount: number) {
  return { id, productCode: id.toUpperCase(), productName: name, productType: "package", priceAmount, status: "active", createdAt: now };
}

function providerAdapter(id: string, providerCode: string, adapterType: string) {
  return { id, providerCode, adapterType, mode: "sdk_api", capabilities: ["webhook", "polling"], environment: "test", status: "active", webhookEnabled: true, pollingCheckEnabled: true, credentialSecretRef: "cred://test/provider", productionReady: false, operatorNote: "mock", evidenceAttachmentIds: [], createdAt: now, updatedAt: now };
}

function dynamicRule(id: string, ticketTypeId: string, ruleName: string, status: string) {
  return { id, ticketTypeId, ruleName, priority: 10, effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", salesChannel: "web", adjustmentType: "percent_delta", adjustmentRate: 10, status, conditionRule: {}, evidenceAttachmentIds: [], createdAt: now, updatedAt: now };
}

function createState(empty = false) {
  const tickets = empty ? [] : [ticketProduct("ticket-1", "Weekend Pass", 9900)];
  const providerAdapters = empty ? [] : [providerAdapter("adapter-1", "mock-video", "video")];
  const amusementSessions = empty ? [] : [{ id: "session-1", projectId: "amuse-1", sessionName: "Morning Session", startsAt: now, endsAt: now, capacity: 20, usedCapacity: 3, lockedCapacity: 2, status: "scheduled" }];
  const businessBindings = empty ? [] : [{ id: "binding-1", merchant_id: "merchant-1", operation_entity_id: "operator-entity-1", contract_entity_id: "contract-entity-1", settlement_entity_id: "settlement-entity-1", status: "active", effective_from: "2026-06-01", effective_to: "2027-05-31", change_reason: "合同主体确认", created_at: now, updated_at: now }];
  const contracts = empty ? [] : [{ id: "contract-1", admissionStep: "合同草稿/审批", merchantId: "merchant-1", contractSubjectId: "contract-subject-1", operationSubjectId: "operator-subject-1", settlementSubjectId: "settlement-subject-1", effectiveDate: "2026-06-01", expiryDate: "2027-05-31", rentTerms: { billingCycle: "monthly", baseRent: 12000 }, shareholderRatioForReport: 0.25, attachmentIds: ["att-contract-1"], lifecycleStatus: "draft", status: "draft", auditEvidenceRef: "audit://admission/contract-1" }];
  const ticketOrders = empty ? [] : [
    { id: "ticket-order-1", orderNo: "TO-MOCK-1", ticketTypeId: "ticket-1", quantity: 1, visitDate: "2026-06-20", salesChannel: "web", amountCent: 9900, payStatus: "paid", issueStatus: "not_issued", status: "created" },
    ...fillRows(11, (i) => ({ id: `ticket-order-fill-${i + 2}`, orderNo: `TO-FILL-${i + 2}`, ticketTypeId: "ticket-1", quantity: 1, visitDate: "2026-06-21", salesChannel: i % 2 ? "miniapp" : "web", amountCent: 9900, payStatus: i % 2 ? "unpaid" : "paid", issueStatus: "not_issued", status: "created" })),
  ];
  const savedBottles = empty ? [] : [{ id: "saved-1", savedNo: "SAVED-1", customerName: "Guest A", itemName: "Wine", quantity: 1, status: "stored" }];
  const barTableSessions = empty ? [] : [{ id: "session-1", barId: "bar-main", merchantId: "merchant-1", businessDate: "2026-06-15", tableId: "table-1", reservationId: "reservation-1", customerId: "customer-1", guestCount: 2, status: "open", sessionVersion: 1, openedAt: now }, { id: "session-2", barId: "bar-main", merchantId: "merchant-1", businessDate: "2026-06-15", tableId: "table-2", reservationId: null, customerId: "customer-2", guestCount: 2, status: "open", sessionVersion: 1, openedAt: now }];
  const safetyInspections = empty ? [] : [{ id: "safety-1", inspectionNo: "SAFE-1", projectId: "amuse-1", projectName: "Adventure", templateId: "safety-template-1", overallResult: "pending_rectification", inspectionStatus: "created", rectificationStatus: "pending", reopenStatus: "pending", riskLevel: "medium", status: "open", evidenceAttachmentIds: ["att-safety-1"], inspectedAt: now, createdAt: now, updatedAt: now }];
  const reconciliationBatches = empty ? [] : [{ id: "recon-batch-1", batchNo: "REC-1", amount: 9900, status: "open" }];
  const reconciliationDifferences = empty ? [] : [{ id: "recon-diff-1", differenceNo: "DIFF-1", amount: 100, status: "unresolved" }];
  const retryTasks = empty ? [] : [{ id: "retry-1", taskNo: "RT-1", providerCode: "mock-video", eventType: "webhook", status: "pending" }];
  const compensationTasks = empty ? [] : [{ id: "comp-1", taskNo: "CP-1", providerCode: "mock-video", eventType: "polling_check", status: "pending" }];
  const orderSyncs = empty ? [] : [{ id: "sync-1", orderNo: "SYNC-1", providerCode: "mock-pos", syncType: "order_paid", status: "processed", updatedAt: now }];
  const voucherVerifications = empty ? [] : [
    { id: "voucher-1", verificationNo: "VCH-1", channelCode: "mock-channel", verificationType: "coupon_verify", status: "success", updatedAt: now },
    ...fillRows(8, (i) => ({ id: `voucher-fill-${i + 2}`, verificationNo: `VCH-FILL-${i + 2}`, channelCode: "mock-channel", verificationType: "coupon_verify", status: i % 2 ? "pending" : "success", updatedAt: now })),
  ];
  const metricSnapshots = empty ? [] : [
    { snapshotId: "metric-1", metricKey: "gmv_amount_cents", domain: "complex", periodType: "day", businessDate: "2026-06-15", segmentType: "complex", segmentId: "complex-1", actualValue: 880000, baselineValue: 760000, varianceRate: 0.158, trendDirection: "up", projectionMethod: "rolling_7d", projectedValue: 920000, confidenceScore: 0.86, payload: { source: "mock" }, createdAt: now },
    { snapshotId: "metric-2", metricKey: "bar_order_count", domain: "bar", periodType: "day", businessDate: "2026-06-15", segmentType: "bar", segmentId: "bar-main", actualValue: 42, baselineValue: 38, varianceRate: 0.105, trendDirection: "up", projectionMethod: "rolling_7d", projectedValue: 44, confidenceScore: 0.8, payload: { source: "mock" }, createdAt: now },
  ];
  const metricDefinitions = empty ? [] : [
    { metricKey: "gmv_amount_cents", name: "GMV", domain: "complex", unit: "cent", calculationNote: "paid ticket + bar + merchant revenue", ownerRole: "OPS_MANAGER", status: "active", createdAt: now, updatedAt: now },
    { metricKey: "bar_order_count", name: "Bar Order Count", domain: "bar", unit: "count", calculationNote: "paid bar orders", ownerRole: "BAR_MANAGER", status: "active", createdAt: now, updatedAt: now },
  ];
  const metricAnomalies = empty ? [] : [
    { id: "ano_revenue_net_2026-07-01", metricKey: "revenue.net", domain: "finance", riskLevel: "high", observedValue: 1900000, expectedValue: 1000000, summary: "revenue.net 当日 1900000 较前一经营日 1000000 变动 90%", status: "open", detectedAt: now, resolvedAt: null },
  ];
  const riskAlerts = empty ? [] : [
    { id: "risk-1", ruleId: "risk-rule-1", alertType: "staff_behavior", riskLevel: "high", objectType: "bar_order", objectId: "order-1", summary: "Mock risk", ownerUserId: "ops-1", status: "open", evidenceAttachmentIds: ["att-risk-1"], createdAt: now, updatedAt: now, resolvedAt: null as string | null },
    ...fillRows(11, (i) => ({ id: `risk-fill-${i + 2}`, ruleId: "risk-rule-1", alertType: "customer_risk", riskLevel: i % 2 ? "suspicious" : "high", objectType: "ticket_order", objectId: `order-${i + 2}`, summary: `Filler risk ${i + 2}`, ownerUserId: "ops-1", status: "open", evidenceAttachmentIds: [] as string[], createdAt: now, updatedAt: now, resolvedAt: null as string | null })),
  ];
  const evidenceEvents = empty ? [] : [
    { id: "event-1", eventType: "ticket.order.created", eventCategory: "ticketing", sourceType: "web", result: "success", riskLevel: "normal", traceId: "event-trace-1", objectType: "ticket_order", objectId: "ticket-order-1", summary: "ticket order created", beforeAfter: { before: {}, after: { status: "created" } }, occurredAt: now },
    { id: "event-2", eventType: "finance.adjustment.blocked", eventCategory: "finance", sourceType: "web", result: "blocked", riskLevel: "high", traceId: "event-trace-2", objectType: "finance_item", objectId: "bill-1", summary: "adjustment blocked without reason", beforeAfter: { before: { status: "pending" }, after: { status: "pending" } }, occurredAt: now },
    ...fillRows(10, (i) => ({ id: `event-fill-${i + 3}`, eventType: "bar.order.settled", eventCategory: "bar", sourceType: "web", result: "success", riskLevel: "normal", traceId: `event-trace-fill-${i + 3}`, objectType: "bar_order", objectId: `order-${i + 3}`, summary: `bar order settled ${i + 3}`, beforeAfter: { before: {}, after: { status: "settled" } }, occurredAt: now })),
  ];
  const merchantWorkOrders = empty ? [] : [{ id: "mswo-1", merchantId: "merchant-1", spaceId: "area-1", contractId: "contract-1", workOrderType: "repair", priority: "high", status: "created", title: "\u7a7a调漏水", description: "\u5546户报修空调漏水", reportedByUserId: "staff_reporter", reportedChannel: "web", ownerDepartment: "", ownerUserId: "", dueAt: "2026-06-16T10:00:00.000Z", evidenceAttachmentIds: ["att-photo-1"], authorizedScope: "merchant-1:work-orders", auditEvidenceRef: "audit://merchant-scope/work-order-1", createdAt: now, updatedAt: now }];
  const merchantZoneFeeRecipients = empty ? [] : [{ id: "mfnr-1", merchantId: "merchant-1", feeEvent: "rent", recipientName: "财务小张", recipientPhone: "15577000001", status: "active", createdAt: now, updatedAt: now }];
  const merchantZoneSettlements = empty ? [] : [{ id: "cst-1", contractId: "contract-1", triggerType: "move_out", status: "pending_settlement", settlementItems: [], disputeReason: null, createdAt: now, updatedAt: now }];
  const merchantZoneContracts = empty ? [] : [{ id: "contract-1", merchantId: "merchant-1", effectiveDate: "2026-07-01", expiryDate: "2027-06-30", lifecycleStatus: "effective", depositCent: 500000, projectId: "project_1", spaceIds: ["space-1"], renewedFromContractId: null, createdAt: now, updatedAt: now }];
  const merchantBills = empty ? [] : [{ id: "merchant-bill-1", merchantId: "merchant-1", billNo: "MBILL-1", title: "物业费账单", totalAmount: 12000, totalAmountCent: 12000000, currency: "CNY", status: "pending_confirm", billPeriodStart: "2026-06-01", billPeriodEnd: "2026-06-30", dueDate: "2026-07-15", agingDays: 0, lines: [{ id: "mbl-1", feeType: "property_fee", description: "物业费 2026-06", amountCent: 12000000, currency: "CNY" }], authorizedScope: "merchant-1:billing", auditEvidenceRef: "audit://merchant-scope/bill-1", createdAt: now, updatedAt: now }];
  const facilityEquipment = empty ? [] : [
    { id: "eq-hvac-1", name: "空调主机", equipmentType: "hvac", status: "normal", spaceId: "area-1", projectId: "venue-1", brand: "MockAir", responsibleUserId: "facility-owner", maintenanceCycleDays: 90, nextMaintenanceDue: "2026-07-15", alarmSource: null, alarmNote: null },
    { id: "eq-power-1", name: "配电柜", equipmentType: "power", status: "maintenance", spaceId: "area-2", projectId: "venue-1", brand: "MockPower", responsibleUserId: "facility-owner", maintenanceCycleDays: 180, nextMaintenanceDue: "2026-08-01", alarmSource: null, alarmNote: null },
  ];
  const facilityWorkOrders = empty ? [] : [
    { id: "fwo-1", code: "VND-WO-1", workOrderType: "repair", priority: "high", status: "assigned", title: "空调主机维保", description: "外包维保待处理", source: "platform", spaceId: "area-1", equipmentId: "eq-hvac-1", reporterId: "staff-ops", reporterType: "staff", assigneeId: "vendor-user", responseDueAt: now, handleDueAt: "2026-06-16T18:00:00.000Z", assignedAt: now, respondedAt: null, completedAt: null, verifiedAt: null, verifierId: null, satisfaction: null, overtime: false, resolution: null, projectId: "venue-1", merchantId: null, createdAt: now, updatedAt: now },
    { id: "fwo-2", code: "OTHER-WO-1", workOrderType: "cleaning", priority: "normal", status: "assigned", title: "公共区保洁", description: "其他外包账号任务", source: "platform", spaceId: "area-2", equipmentId: null, reporterId: "staff-ops", reporterType: "staff", assigneeId: "other-vendor", responseDueAt: now, handleDueAt: "2026-06-17T18:00:00.000Z", assignedAt: now, respondedAt: null, completedAt: null, verifiedAt: null, verifierId: null, satisfaction: null, overtime: false, resolution: null, projectId: "venue-1", merchantId: null, createdAt: now, updatedAt: now },
  ];
  const facilityInspectionTemplates = empty ? [] : [
    { id: "template-security-1", name: "夜间安保模板", inspectionType: "security", cycleDays: 1, abnormalMode: "to_work_order", active: true, checklist: [{ label: "门禁状态" }, { label: "消防通道" }], scopeNote: "夜间重点区域巡查" },
    { id: "template-cleaning-1", name: "公共区保洁模板", inspectionType: "cleaning", cycleDays: 1, abnormalMode: "light", active: true, checklist: [{ label: "地面清洁" }, { label: "垃圾桶清运" }], scopeNote: "公共区清洁要求" },
  ];
  const facilityInspectionTasks = empty ? [] : [
    { id: "fit-1", templateId: "template-security-1", inspectionType: "security", status: "scheduled", title: "夜间安保巡检", scheduleMode: "manual", routePoints: [] as unknown[], assigneeId: "vendor-user", scheduledDate: "2026-06-15", dueDate: "2026-06-15", startedAt: null, completedAt: null, workOrderId: null as string | null, projectId: "venue-1", spaceId: "area-1", createdAt: now, updatedAt: now },
    { id: "fit-2", templateId: "template-cleaning-1", inspectionType: "cleaning", status: "scheduled", title: "其他保洁巡检", scheduleMode: "manual", routePoints: [] as unknown[], assigneeId: "other-vendor", scheduledDate: "2026-06-15", dueDate: "2026-06-15", startedAt: null, completedAt: null, workOrderId: null as string | null, projectId: "venue-1", spaceId: "area-2", createdAt: now, updatedAt: now },
  ];
  const facilityInspectionRecords = empty ? [] : [{ id: "fir-1", taskId: "fit-1", result: "normal", photos: ["att-vendor-inspection"], qrCode: "QR-1", gpsLat: null, gpsLng: null, checklistResult: [] as unknown[], note: "mock record", abnormalNote: null as string | null, inspectorId: "vendor-user", recordedAt: now, createdAt: now }];
  const siteEntryReports = empty ? [] : [
    { id: "ser-1", projectId: "venue-1", spaceId: "area-1", workScope: "店铺装修围挡施工", plannedStartDate: "2026-06-20", plannedEndDate: "2026-06-22", crewCount: 4, contactName: "王工", contactPhone: "13800000000", vendorUserId: "vendor-user", status: "in_progress", reviewComment: "同意进场", acceptanceComment: null, completionAttachmentIds: [] as string[], completionNote: null as string | null, completionUploadedAt: null as string | null, createdBy: "vendor-user", createdAt: now, updatedAt: now, deletedAt: null },
    { id: "ser-2", projectId: "venue-1", spaceId: "area-2", workScope: "其他队伍施工", plannedStartDate: "2026-06-21", plannedEndDate: "2026-06-23", crewCount: 3, contactName: "李工", contactPhone: "13900000000", vendorUserId: "other-vendor", status: "approved", reviewComment: "同意进场", acceptanceComment: null, completionAttachmentIds: [] as string[], completionNote: null as string | null, completionUploadedAt: null as string | null, createdBy: "other-vendor", createdAt: now, updatedAt: now, deletedAt: null },
  ];
  const supervisorReviewPolicies = empty ? [] : [{ id: "policy-1", policyCode: "contract_violation_l3", actionType: "contract_violation", minAmountCent: 0, requiredLevel: 3, blocksByDefault: true, requiredEvidenceTypes: ["violation_summary", "contract_snapshot"], status: "active" }, { id: "policy-2", policyCode: "base_definition_violation_l3", actionType: "base_definition_violation", minAmountCent: 0, requiredLevel: 3, blocksByDefault: true, requiredEvidenceTypes: ["violation_summary", "definition_snapshot"], status: "active" }];
  return {
    tickets,
    providerAdapters,
    businessBindings,
    contracts,
    ticketOrders,
    savedBottles,
    barTableSessions,
    safetyInspections,
    reconciliationBatches,
    reconciliationDifferences,
    retryTasks,
    compensationTasks,
    orderSyncs,
    voucherVerifications,
    metricSnapshots,
    metricDefinitions,
    metricAnomalies,
    riskAlerts,
    evidenceEvents,
    merchantWorkOrders,
    merchantBills,
    merchantZoneSettlements,
    merchantZoneFeeRecipients,
    merchantZoneContracts,
    facilityEquipment,
    facilityWorkOrders,
    facilityInspectionTemplates,
    facilityInspectionTasks,
    facilityInspectionRecords,
    siteEntryReports,
    supervisorReviewPolicies,
    issuedEntitlements: empty ? [] : [{ id: "ent-1", entitlementCode: "ENT-0001", entitlementType: "entry", totalTimes: 1, usedTimes: 0, valueAmount: 9900, status: "active", createdAt: now }],
    verifications: empty ? [] : [{ id: "ver-1", verificationNo: "VER-0001", status: "success", source: "mock", createdAt: now }],
    dynamicPricingRules: empty ? [] : [dynamicRule("rule-1", "ticket-1", "Weekend Boost", "draft")],
    groupBookings: empty ? [] : [{ id: "gb-1", projectId: "amuse-1", sessionId: "session-1", bookingType: "group", customerName: "Team A", partySize: 6, reservedCapacity: 6, bookingStatus: "approved", evidenceAttachmentIds: [], createdAt: now, updatedAt: now }],
    amusementSessions,
    users: empty ? [] : [{ id: "user-1", username: "admin", accountType: "platform_admin", status: "active", organizationId: "org-1" }],
    permissionTemplates: empty ? [] : [{ id: "template-basic", templateName: "普通员工", templateType: "base", status: "active", validUntil: null, singleUse: false, permissions: ["analytics.dashboard.view"] }],
    // 对齐后端 ArApItemRecord 真实字段：item_no/amount_cent/paid_amount_cent/unpaid_amount_cent/due_date。
    financeItems: empty ? [] : [{ id: "bill-1", itemNo: "BILL-1", sourceType: "merchant_bill", sourceId: "SO-1", counterparty: "Main Merchant", direction: "receivable", amountCent: 9900, paidAmountCent: 0, unpaidAmountCent: 9900, currency: "CNY", dueDate: "2026-07-15", status: "pending", createdAt: now }],
    readinessItems: empty ? [] : [{ id: "blocker-1", blockerCode: "SEC-P0-OPS-RECOVERY-001", title: "NAS 恢复演练", owner: "IT", status: "open", evidenceRef: null, dueAt: "2026-06-30" }],
  };
}

type MockState = ReturnType<typeof createState>;

function isOutsourcedVendor(options: MockOptions) {
  return options.user?.accountType === "outsourced_vendor";
}

function assignedRows<T extends Record<string, unknown>>(rows: T[], options: MockOptions, key: keyof T): T[] {
  if (!isOutsourcedVendor(options)) return rows;
  const actorId = options.user?.id ?? "vendor-user";
  return rows.filter((row) => row[key] === actorId);
}

function fixedRows(pathname: string, state: MockState, options: MockOptions = {}): ApiData {
  const empty: Record<string, unknown>[] = [];
  const routes: Record<string, ApiData> = {
    "/api/v1/system/health": { ok: true, service: "mock-api", time: now },
    "/api/v1/amusement/business-day-configs": { projects: [{ id: "venue-1", name: "本综合体" }], versions: [] },
    "/api/v1/inventory/purchase-workbench-options": { projects: [], entities: [], merchants: [], suppliers: [], inventory_items: [], warehouses: [], contracts: [] },
    "/api/v1/inventory/purchase-requests": [],

    "/api/v1/analytics/dashboards/p0-overview": { merchants: 1, ticketProducts: state.tickets.length, issuedTickets: state.issuedEntitlements.length, verifications: state.verifications.length, barOrders: 1, amusementSessions: state.amusementSessions.length, openRiskAlerts: 1, auditEvents: 2 },
    "/api/v1/platform/business-entities": [{ id: "org-1", entityName: "Platform Org", entityRole: "operating", unifiedSocialCreditCode: "91310000MA1FL000XQ", name: "Platform Org", code: "ORG", orgType: "platform", status: "active", createdAt: now }],
    "/api/v1/platform/business-entity-bindings": state.businessBindings,
    "/api/v1/contracts": { items: state.contracts, page: 1, pageSize: 500, total: state.contracts.length },
    "/api/v1/platform/supervisor-review-policies": state.supervisorReviewPolicies,
    "/api/v1/platform/supervisor-review/pending": [{ decision: { reviewId: "review-pending-1", policyCode: "contract_violation_l3", actionType: "contract_violation", objectType: "contract", objectId: "contract-1", initiatedBy: "staff-initiator-2", requiredLevel: 3 }, schedule: null }],
    "/api/v1/platform/merchants": [{ id: "bar-main", merchantName: "主酒吧", merchantType: "bar", status: "active" }, { id: "merchant-1", merchantName: "Main Merchant", projectId: "venue-1", businessEntityId: "org-1", contactName: "Jack", admissionStep: "商户档案", organizationId: "org-1", displayName: "Main Merchant", companyName: "Main Company", mallBlockCode: "1F-A", areaSquareMeter: 128.5, leasedAreaSquareMeter: 120, secondaryContactName: "Jack", rentPerSquareMeter: 12.3, cooperationForm: "fixed_rent_plus_commission", status: "active", riskLevel: "normal", auditEvidenceRef: "audit://admission/merchant-1" }],
    "/api/v1/platform/projects": [{ id: "venue-1", merchantId: "merchant-1", name: "Main Venue", code: "VENUE", venueType: "complex", status: "active" }],
    "/api/v1/platform/spaces": [{ id: "area-1", spaceName: "Gate A", projectId: "venue-1", spaceType: "gate", floorNo: "1F", areaSquareMeter: 20, venueId: "venue-1", areaId: "area-1", name: "Gate A", code: "GATE-A", areaType: "operation", pointType: "gate", status: "active" }],
    "/api/v1/platform/space-types": [{ id: "sptype_building", typeCode: "building", typeName: "楼栋", sortOrder: 10, status: "active" }, { id: "sptype_shop", typeCode: "shop", typeName: "商铺/铺位", sortOrder: 40, status: "active" }],
    "/api/v1/partner/subordinates": [{ id: "epu-1", username: "vendor-staff-1", partnerOrgId: "epo-1", accountType: "outsourced_vendor", status: "active" }],
    "/api/v1/contracts/space-rental-status": [{ spaceId: "shop-1", spaceName: "L1-001", rentalStatus: "let", color: null, daysToExpiry: 200 }, { spaceId: "shop-2", spaceName: "L1-002", rentalStatus: "expiring", color: "#f59e0b", daysToExpiry: 15 }, { spaceId: "shop-3", spaceName: "L1-003", rentalStatus: "available", color: null, daysToExpiry: null }],
    "/api/v1/access/users": state.users,
    "/api/v1/access/roles": [{ id: "role-1", roleCode: "BAR_MANAGER", roleName: "酒吧经理" }, { id: "role-2", roleCode: "FINANCE", roleName: "财务" }],
    "/api/v1/access/permissions": [{ code: "analytics.dashboard.view", domain: "analytics", permissionName: "查看总控驾驶舱" }, { code: "finance.ar_ap.view", domain: "finance", permissionName: "查看应收应付" }, { code: "bar.table_session.view", domain: "bar", permissionName: "查看卡座会话" }, { code: "bar.table.view", domain: "bar", permissionName: "查看卡座" }, { code: "bar.reservation.view", domain: "bar", permissionName: "查看订台" }, { code: "bar.order.view", domain: "bar", permissionName: "查看酒吧订单" }, { code: "access.permission_template.view", domain: "access", permissionName: "查看权限模板" }, { code: "access.permission_template.manage", domain: "access", permissionName: "维护权限模板" }, { code: "access.permission_template.apply", domain: "access", permissionName: "应用权限模板" }],
    "/api/v1/access/permission-templates": state.permissionTemplates,
    "/api/v1/access/temporary-grants": empty,
    "/api/v1/analytics/merchants": [{ merchantId: "merchant-1", merchantName: "Main Merchant", merchantType: "complex", revenueAmountCents: 880000, orderCount: 8, riskAlertCount: 1, settlementStatus: "pending", businessDate: "2026-06-15", createdAt: now, updatedAt: now }],
    "/api/v1/customer-accounts": [
      { id: "cust-1", maskedPhone: "139****1234", status: "active", suspendedUntil: null, createdAt: now },
    ],
    "/api/v1/ticketing/ticket-types": state.tickets,
    "/api/v1/ticketing/entitlement-templates": [{ id: "tpl-1", name: "入园1次", entitlementType: "entry", status: "active" }],
    "/api/v1/ticketing/price-rules": state.priceRules ?? [],
    "/api/v1/ticketing/dynamic-pricing-rules": state.dynamicPricingRules,
    "/api/v1/ticketing/orders": state.ticketOrders,
    "/api/v1/ticketing/entitlement-templates": [{ id: "tpl-1", name: "Entry", entitlementType: "entry", defaultTotalTimes: 1, defaultValueAmount: 9900, status: "active" }],
    "/api/v1/ticketing/issued-entitlements": state.issuedEntitlements,
    "/api/v1/ticketing/verifications": state.verifications,
    "/api/v1/finance/ar-ap-items": state.financeItems,
    "/api/v1/finance/ar-ap-items/export": { id: "arap-export-1", format: "csv", rowCount: state.financeItems.length, rows: state.financeItems, csvText: "item_no,direction,status\nBILL-1,receivable,pending", exportedAt: now },
    "/api/v1/finance/property-fee/rates": [{ id: "pfr-1", projectId: "venue-1", effectivePeriod: null, ratePerSquareMeterCent: 2500, status: "active", createdAt: now, updatedAt: now }],
    "/api/v1/finance/merchant-expenses": { items: [{ id: "mex_m_1", merchantId: "merchant-1", period: "2026-07", expenseType: "labor", amountCent: 500000, source: "manual", sourceRef: null, note: "七月人工", operatorUserId: "usr_admin", createdAt: now }, { id: "mex_ae_1", merchantId: "merchant-1", period: "2026-07", expenseType: "energy", amountCent: 88800, source: "auto_energy", sourceRef: "arap_energy_1", note: "能耗费自动挂接", operatorUserId: "usr_admin", createdAt: now }], page: 1, pageSize: 20, total: 2 },
    "/api/v1/finance/dividends/plans": [{ id: "divp_1", merchantId: "merchant-1", principalUserId: "usr_principal", principalName: "主理人甲", platformShareBps: 8000, taxRateBps: 2000, periodType: "quarterly", revenueDomains: ["bar", "ticket"], includeInventoryCost: true, expenseTypes: ["rent_allocation", "energy", "property_fee", "labor", "amortization", "other"], status: "active", createdAt: now, updatedAt: now }],
    "/api/v1/finance/dividends/statements": { items: [{ id: "divs_q2", merchantId: "merchant-1", planId: "divp_1", kind: "official", periodLabel: "2026-Q2", periodStart: "2026-04-01", periodEnd: "2026-06-30", principalUserId: "usr_principal", platformShareBps: 8000, taxRateBps: 2000, revenueCent: 1020000, costCent: 150000, expenseCent: 220000, netProfitCent: 650000, distributableCent: 650000, platformShareCent: 520000, principalGrossCent: 130000, taxCent: 26000, principalNetCent: 104000, noCostBasisCount: 0, status: "approved", createdAt: now }], page: 1, pageSize: 20, total: 1 },
    "/api/v1/finance/merchant-expenses/summary": [{ merchantId: "merchant-1", period: "2026-07", expenseType: "labor", totalCent: 500000 }, { merchantId: "merchant-1", period: "2026-07", expenseType: "energy", totalCent: 88800 }],
    "/api/v1/vouchers/definitions": { items: [{ id: "vdef_1", name: "满 100 减 50 券", voucherKind: "merchant_discount", redeemMethod: "in_store_staff", settlementMode: "none", merchantId: "merchant-1", faceValueCent: 5000, validDays: 30, status: "active", createdAt: now, updatedAt: now }], page: 1, pageSize: 200, total: 1 },
    "/api/v1/vouchers/instances": { items: [{ id: "vi_1", definitionId: "vdef_1", customerId: "customer-1", code: "VCAAA", sourceType: "points_redemption", sourceRef: "redemption_1", status: "issued", issuedAt: now, expiresAt: null, createdAt: now, updatedAt: now }], page: 1, pageSize: 20, total: 1 },
    "/api/v1/vouchers/redemptions": { items: [], page: 1, pageSize: 20, total: 0 },
    "/api/v1/vouchers/overview": { items: [{ id: "vi_1", source: "unified", name: "满 100 减 50 券", voucherKind: "merchant_discount", customerId: "customer-1", status: "issued", remainingTimes: 1, expiresAt: null }, { id: "ie_1", source: "pass_partner", name: "餐厅第三方券", voucherKind: "external_partner", customerId: "customer-1", status: "active", remainingTimes: 3, expiresAt: null }], page: 1, pageSize: 20, total: 2 },
    "/api/v1/finance/reconciliation/batches": state.reconciliationBatches,
    "/api/v1/finance/reconciliation/differences": state.reconciliationDifferences,
    "/api/v1/bar/tables": [{ id: "table-1", tableCode: "A01", tableName: "A01", capacity: 4, guestLimit: 4, minSpendAmount: 10000, barId: "bar-main", status: "active" }, { id: "table-2", tableCode: "A02", tableName: "A02", capacity: 6, guestLimit: 6, minSpendAmount: 12000, barId: "bar-main", status: "active" }],
    "/api/v1/bar/products": [{ id: "drink-1", barId: "bar-main", productName: "Mock Drink", productType: "spirit", priceCent: 3000, internalSettlementPriceCent: 1200, status: "active", soldOut: false }],
    "/api/v1/bar/production-tickets": empty,
    "/api/v1/bar/reservations": [{ id: "reservation-1", reservationNo: "RSV-1", customerName: "Guest A", partySize: 2, depositAmount: 2000, status: "reserved", reservedAt: now }],
    "/api/v1/bar/orders": [{ id: "order-1", tableId: "table-1", salesStaffId: "staff_sales_1", status: "paid", totalAmount: 6000, createdAt: now, items: [{ id: "order-item-1", productName: "精酿啤酒", quantity: 1, amountCent: 6000, deliveryStatus: "pending" }], bills: [{ id: "bill-bar-1", status: "open", salesRoleType: "sales", commissionRateBp: 1500 }] }],
    "/api/v1/bar/table-sessions": state.barTableSessions,
    "/api/v1/inventory/stock-movements": empty,
    "/api/v1/inventory/items": [{ id: "item-1", itemCode: "WINE", itemName: "Wine", category: "drink", unit: "bottle", status: "active", minQuantity: 2 }],
    "/api/v1/inventory/stock-balances": [{ id: "stock-1", itemId: "item-1", locationType: "bar", quantity: 10 }],
    "/api/v1/inventory/supply-entities": [{ id: "supply-1", name: "平台酒水供货公司", entityType: "liquor", level: "platform", status: "active" }],
    "/api/v1/inventory/transfer-orders": [{ id: "trf-1", transferType: "supply", fromOwnerType: "supply_entity", fromOwnerId: "supply-1", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "shipped", createdAt: now, updatedAt: now, lines: [{ id: "trfl-1", itemId: "item-1", quantity: 10, unitPriceCent: 5000, traceCodes: ["TRACE-A"] }] }],
    "/api/v1/inventory/transfer-orders/trf-1": { id: "trf-1", transferType: "supply", fromOwnerType: "supply_entity", fromOwnerId: "supply-1", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "shipped", createdAt: now, updatedAt: now, lines: [{ id: "trfl-1", itemId: "item-1", quantity: 10, unitPriceCent: 5000, traceCodes: ["TRACE-A"] }] },
    "/api/v1/inventory/saved-bottles": state.savedBottles,
    "/api/v1/amusement/projects": [{ id: "amuse-1", projectCode: "AMUSE", projectName: "Adventure", capacity: 20, status: "active", sessions: state.amusementSessions }],
    "/api/v1/amusement/sessions": state.amusementSessions,
    "/api/v1/amusement/verifications": empty,
    "/api/v1/amusement/reservations": empty,
    "/api/v1/amusement/queue-entries": empty,
    "/api/v1/amusement/device-statuses": empty,
    "/api/v1/amusement/safety-inspections": state.safetyInspections,
    "/api/v1/amusement/group-bookings": state.groupBookings,
    "/api/v1/analytics/risk-rules": [{ id: "risk-rule-1", ruleCode: "RISK", ruleName: "Manual Risk", ruleType: "manual", threshold: 1, status: "active" }],
    "/api/v1/analytics/risk-alerts": state.riskAlerts,
    "/api/v1/evidence/events": state.evidenceEvents,
    "/api/v1/integration/provider-adapters": state.providerAdapters,
    "/api/v1/integration/order-syncs": state.orderSyncs,
    "/api/v1/integration/channel-ticket-type-maps": empty,
    "/api/v1/integration/channel-store-maps": empty,
    "/api/v1/integration/voucher-verifications": state.voucherVerifications,
    "/api/v1/integration/retry-tasks": state.retryTasks,
    "/api/v1/integration/compensation-tasks": state.compensationTasks,
    "/api/v1/integration/retail/skus": empty,
    "/api/v1/integration/robot/tasks": empty,
    "/api/v1/integration/robot/task-events": empty,
    "/api/v1/integration/video/cameras": empty,
    "/api/v1/integration/wecom/bindings": empty,
    "/api/v1/integration/wecom/conversations": empty,
    "/api/v1/integration/wecom/messages": empty,
    "/api/v1/integration/wecom/risk-hits": empty,
    "/api/v1/integration/devices": empty,
    "/api/v1/integration/devices/events": empty,
    "/api/v1/integration/devices/offline-sync-batches": empty,
    "/api/v1/terminals/devices": [
      { id: "device-navigation-1", device_code: "WORK-001", terminal_type: "staff_work_phone", merchant_id: "merchant-1", location_point_id: "point-1", staff_id: null, status: "returned", bound_at: null, updated_at: now },
    ],
    "/api/v1/terminals/mdm/app-whitelist": [
      { id: "mdm_app_1", package_name: "com.tencent.wework", app_label: "企业微信", scope_type: "global", scope_id: "global", enabled: true, reason: null, created_at: now, updated_at: now },
    ],
    "/api/v1/terminals/mdm/site-whitelist": [
      { id: "mdm_site_1", domain: "work.weixin.qq.com", description: "企业微信网页", scope_type: "global", scope_id: "global", enabled: true, reason: null, created_at: now, updated_at: now },
    ],
    "/api/v1/terminals/capability-switches": [
      { id: "tcs_term_web", terminalType: "web", capabilityCode: "terminal.enabled", scopeType: "global", scopeId: "global", enabled: true, reason: "Web 前端总开关" },
      { id: "tcs_term_kiosk", terminalType: "kiosk", capabilityCode: "terminal.enabled", scopeType: "global", scopeId: "global", enabled: true, reason: "一体机总开关" },
      { id: "tcs_camera_scan_off", terminalType: "tablet", capabilityCode: "camera.scan", scopeType: "global", scopeId: "global", enabled: false, reason: "相机扫码" },
      { id: "tcs_sms_master", terminalType: "*", capabilityCode: "sms_notification", scopeType: "global", scopeId: "global", enabled: false, reason: "短信通知总开关，默认关" },
      { id: "tcs_sms_stored_wine", terminalType: "*", capabilityCode: "sms.notify.stored_wine", scopeType: "global", scopeId: "global", enabled: true, reason: "存酒域开关" },
      { id: "tcs_sms_wine_deposit", terminalType: "*", capabilityCode: "sms.notify.stored_wine.deposit", scopeType: "global", scopeId: "global", enabled: true, reason: "存酒成功" },
      { id: "tcs_face_verification_off", terminalType: "*", capabilityCode: "face.verification.enabled", scopeType: "global", scopeId: "global", enabled: false, reason: "默认关，待供应商接入" },
      { id: "tcs_int_payment_off", terminalType: "*", capabilityCode: "integration.payment.sync", scopeType: "global", scopeId: "global", enabled: false, reason: "默认关" },
    ],
    "/api/v1/ops/production-readiness/items": state.readinessItems,
    "/api/v1/ops/production-readiness/checks": [{ id: "check-1", status: "open", message: "NAS 恢复演练待回填" }],
    "/api/v1/analytics/robot": empty,
    "/api/v1/analytics/metric-definitions": state.metricDefinitions,
    // KPI 统一明细(指标登记簿): 数字=明细合计同源(summary_value == Σ rows.amount_cent)。
    "/api/v1/analytics/metric-detail": { metric_key: "revenue.ticket", name: "票务收入", value_type: "additive",
      unit: "分", caliber: "已支付票务订单实付金额合计(分)。可加口径=区间求和。", source: "票务订单台账(实时查询)。",
      part: null, from: "2026-06-15", to: "2026-06-15", data_as_of: now, summary_value: 12500, summary_note: null,
      columns: [{ key: "orderNo", label: "订单号" }, { key: "amountCent", label: "实付金额（分）" }],
      rows: [{ id: "to-1", orderNo: "TK-1", amountCent: 10000 }, { id: "to-2", orderNo: "TK-2", amountCent: 2500 }],
      page: 1, size: 20, total_rows: 2, trace_link_page_id: "ADM-TKT-003", trace_link_column_key: "id" },
    "/api/v1/analytics/anomaly-config": { surge_pct: 0.5, high_pct: 0.8, enabled: true },
    "/api/v1/analytics/ai-thresholds": { local_llm_min_confidence: 0.6, benchmark_deviation: 0.2 },
    "/api/v1/analytics/notification-windows": [
      { window_key: "contract.renewal", days: 30, description: "商户合同续约提醒提前天数" },
      { window_key: "inventory.saved_bottle", days: 7, description: "存酒临期提醒提前天数" },
      { window_key: "leasing.lead_follow_up", days: 3, description: "招商线索未跟进提醒天数" },
    ],
    "/api/v1/settlement/invoice-items/summary": { subjects: [], rules: [], over_issued: [] },
    "/api/v1/settlement/invoice-items": { items: [], page: 1, page_size: 200, total: 0 },
    "/api/v1/staff/payroll/tax-versions": [
      { id: "tax-version-navigation", category: "测试税目", version: "V0.01", effective_from: "2026-06-01",
        applicability_basis: "仅用于导航回归的专业依据样例", calculation_basis: "导航样例，不作为实际计税依据",
        rounding_rule: "truncate_cent_each_step", reason: "验证完整版本展示", status: "draft", review_id: null,
        evidence_attachment_ids: [], created_by: "user-1", created_at: now, decided_at: null,
        brackets: [{ seq: 1, income_floor_cent: 0, income_ceiling_cent: null, income_base_bps: 10000,
          flat_deduction_cent: 0, rate_bps: 2000, quick_deduction_cent: 0 }] },
    ],
    "/api/v1/staff/payroll/tax-brackets": [
      { id: "ptb_labor_1", category: "labor_remuneration", seq: 1, income_floor_cent: 1, income_ceiling_cent: 400000, income_base_bps: 10000, flat_deduction_cent: 80000, rate_bps: 2000, quick_deduction_cent: 0 },
      { id: "ptb_labor_2", category: "labor_remuneration", seq: 2, income_floor_cent: 400001, income_ceiling_cent: 2500000, income_base_bps: 8000, flat_deduction_cent: 0, rate_bps: 2000, quick_deduction_cent: 0 },
    ],
    "/api/v1/analytics/holiday-calendar": [
      { holiday_name: "春节", holiday_year: 2026, start_date: "2026-02-15", end_date: "2026-02-22", source: "manual" },
      { holiday_name: "春节", holiday_year: 2025, start_date: "2025-01-28", end_date: "2025-02-04", source: "seed" },
    ],
    "/api/v1/analytics/holiday-comparison": { holiday_name: "春节", current_year: 2026,
      current_start: "2026-02-15", current_end: "2026-02-22", previous_year: 2025,
      previous_start: "2025-01-28", previous_end: "2025-02-04", rows: [
      { metric_key: "revenue.net", label: "净收入", unit: "cent", aggregation: "monthly_sum",
        current_total: 8000000, current_daily_avg: 1000000, previous_total: 6400000, previous_daily_avg: 800000, yoy_pct: 0.25 },
    ] },
    "/api/v1/analytics/ai-quality/tasks": empty,
    "/api/v1/analytics/ai-quality/results": empty,
    "/api/v1/analytics/customer-intents": empty,
    "/api/v1/analytics/staff-service-score": empty,
    "/api/v1/analytics/merchant-health": empty,
    "/api/v1/services/catalog/all": [
      { id: "svc-e2e-1", name: "点舞", price_cent: 30000, duration_minutes: 15, active: true },
    ],
    "/api/v1/services/orders": [
      { id: "svo-e2e-1", item_name: "点舞", price_cent: 30000, table_id: "tbl-1", performer_user_id: "usr_dancer_1",
        status: "created", mgmt_fee_cent: 3000, dancer_share_cent: 27000, created_at: "2026-07-02T20:00:00Z" },
    ],
    "/api/v1/staff/profiles": [
      { id: "profile-navigation-1", user_id: "usr_staff_1", name: "示例员工", merchant_id: null, registration_status: "approved", employment_status: "employed", status: "active", sales_position: true, archived_at: null, delete_status: "none" },
    ],
    "/api/v1/staff/payroll": [
      { id: "pay-e2e-1", user_id: "usr_staff_1", month: "2026-07", base_cent: 800000, commission_cent: 120000,
        bonus_cent: 50000, penalty_cent: 20000, tax_cent: 0, total_cent: 950000 },
    ],
    "/api/v1/staff/leaves": [
      { id: "lv-e2e-1", user_id: "usr_staff_1", leave_type: "annual", start_date: "2026-07-10",
        end_date: "2026-07-11", reason: "年假", status: "pending" },
    ],
    "/api/v1/staff/shifts": [
      { id: "shf-e2e-1", user_id: "usr_staff_1", work_date: "2026-07-02", start_at: "2026-07-02T02:00:00Z",
        end_at: "2026-07-02T10:30:00Z", point_space_ids: ["sp_point_1", "sp_point_2"], note: "早班" },
    ],
    "/api/v1/staff/attendance": [
      { id: "att-e2e-1", user_id: "usr_staff_1", punch_type: "check_in", punched_at: "2026-07-02T02:05:00Z",
        method: "nfc", judgement: "late", space_id: "sp_point_1" },
    ],
    "/api/v1/analytics/monthly-comparison": { month: "2026-07", rows: [
      { metric_key: "revenue.net", label: "净收入", domain: "finance", unit: "cent", aggregation: "monthly_sum",
        current: 3000000, prev_month: 2000000, mom_pct: 0.5, yoy_same_month: null, yoy_pct: null },
      { metric_key: "ar.balance", label: "欠费余额", domain: "finance", unit: "cent", aggregation: "period_end",
        current: 700000, prev_month: null, mom_pct: null, yoy_same_month: null, yoy_pct: null },
    ] },
    "/api/v1/analytics/reports/monthly-export": { month: "2026-07", rows: [
      { metric_key: "revenue.net", label: "净收入", domain: "finance", unit: "cent", aggregation: "monthly_sum",
        current: 3000000, prev_month: 2000000, mom_pct: 0.5, yoy_same_month: null, yoy_pct: null },
    ] },
    "/api/v1/analytics/dashboards/facility": { business_date: "2026-07-01", metrics: {
      "wo.response_hours_p50": { value: 2.5, unit: "hours" },
      "wo.handle_hours_p50": { value: 6, unit: "hours" },
      "wo.overdue_rate": { value: 0.25, unit: "rate" },
      "wo.backlog": { value: 7, unit: "count" },
      "mwo.handle_hours_p50": { value: 5, unit: "hours" },
      "mwo.overdue_rate": { value: 0.5, unit: "rate" },
      "mwo.backlog": { value: 3, unit: "count" },
    } },
    "/api/v1/analytics/work-order-sla/overdue-orders": [
      { work_order_id: "wo-e2e-1", code: "WO-2026-001", title: "示例超时工单", status: "processing", priority: "high", handle_due_at: "2026-07-01T02:00:00Z", overdue_hours: 6.5 },
    ],
    "/api/v1/analytics/dashboards/energy": { business_date: "2026-07-01", metrics: {
      "energy.total_electricity": { value: 1200.5, unit: "kwh" },
      "energy.total_water": { value: 88, unit: "ton" },
      "energy.electricity_per_sqm": { value: 2.4, unit: "kwh_per_sqm" },
      "energy.abnormal_count": { value: 3, unit: "count" },
    } },
    "/api/v1/analytics/energy/abnormal-readings": [
      { reading_id: "rd-e2e-1", meter_id: "meter-1", meter_name: "示例电表", meter_type: "electricity", period: "2026-07", consumption: 40, abnormal_reason: "surge", read_at: "2026-07-01T12:00:00Z" },
    ],
    "/api/v1/analytics/dashboards/leasing": { businessDate: "2026-07-01", metrics: {
      "occupancy.rate": { value: 0.8, unit: "rate" },
      "occupancy.total_area": { value: 1000, unit: "sqm" },
      "occupancy.rentable_area": { value: 1000, unit: "sqm" },
      "occupancy.leased_area": { value: 800, unit: "sqm" },
      "occupancy.signed_pending_area": { value: 120, unit: "sqm" },
      "occupancy.parking_rate": { value: 0.25, unit: "rate" },
      "contract.expiring_90d": { value: 7, unit: "count" },
    } },
    "/api/v1/analytics/occupancy/vacant-spaces": [
      { spaceId: "space-v1", spaceName: "示例空铺", spaceType: "shop", floorNo: "2F", areaSqm: 60, rentalStatus: "vacant" },
    ],
    "/api/v1/analytics/occupancy/expiring-contracts": [
      { contractId: "contract-exp-1", merchantId: "merchant-1", merchantName: "到期示例商户", expiryDate: "2026-08-15", daysLeft: 45, boundArea: 100 },
    ],
    "/api/v1/analytics/dashboards/finance": { businessDate: "2026-07-01", metrics: {
      "ar.balance": { value: 4200000, unit: "cent" },
      "ar.aging_0_30": { value: 1200000, unit: "cent" },
      "ar.aging_31_60": { value: 900000, unit: "cent" },
      "ar.aging_61_90": { value: 600000, unit: "cent" },
      "ar.aging_90p": { value: 1500000, unit: "cent" },
      "ar.collected_mtd": { value: 6000000, unit: "cent" },
      "ar.receivable_mtd": { value: 8000000, unit: "cent" },
      "ar.collection_rate_mtd": { value: 0.75, unit: "rate" },
    } },
    "/api/v1/analytics/finance-receivables/debtors": [
      { merchantId: "merchant-1", merchantName: "欠费示例商户", outstandingCent: 1500000, oldestDueDate: "2026-03-20", overdueDays: 103, bucket: "a90p" },
      { merchantId: "merchant-2", merchantName: "轻微逾期商户", outstandingCent: 300000, oldestDueDate: "2026-06-15", overdueDays: 16, bucket: "a0_30" },
    ],
    "/api/v1/merchant-service/work-orders": state.merchantWorkOrders,
    "/api/v1/merchant-billing/bills": state.merchantBills,
    "/api/v1/merchant-portal/settlements": state.merchantZoneSettlements,
    "/api/v1/merchant-billing/fee-notification-recipients": state.merchantZoneFeeRecipients,
    "/api/v1/merchant-portal/contracts": state.merchantZoneContracts,
    "/api/v1/facility/equipment": state.facilityEquipment,
    "/api/v1/facility/equipment/maintenance-due": empty,
    "/api/v1/facility/work-orders": assignedRows(state.facilityWorkOrders, options, "assigneeId"),
    "/api/v1/facility/work-orders/overdue": state.facilityWorkOrders.filter((row) => row.overtime),
    "/api/v1/facility/inspections/templates": state.facilityInspectionTemplates,
    "/api/v1/facility/inspections/tasks": assignedRows(state.facilityInspectionTasks, options, "assigneeId"),
    "/api/v1/facility/site-entry-reports": assignedRows(state.siteEntryReports, options, "vendorUserId"),
    "/api/v1/analytics/metric-anomalies": state.metricAnomalies,
    "/api/v1/compliance/reminders": { items: [{ id: "crm-1", reminder_type: "renewal", source_type: "contract", source_id: "contract-1", due_date: "2026-08-01", reminder_at: "2026-07-15", severity: "high", status: "open", blocking_suggested: true, message: "示例到期提醒" }] },
    "/api/v1/compliance/contract-key-dates": { items: [{ id: "ckd-1", contract_id: "contract-1", merchant_id: "merchant-1", date_type: "renewal", title: "示例续约日", due_date: "2026-08-01", status: "open", source_type: "contract", human_confirmed: false }] },
    "/api/v1/compliance/expiry-items": { items: [{ id: "cei-1", subject_type: "merchant", subject_id: "merchant-1", merchant_id: "merchant-1", item_type: "business_license", item_name: "示例营业执照", document_no: "BL-001", issuing_authority: "市监局", valid_from: "2025-01-01", valid_to: "2026-12-31", expiry_action: "block", status: "active", source_type: "manual", human_confirmed: true }] },
    "/api/v1/compliance/wecom/risk-hits": { items: [{ hit_id: "wrh-1", conversation_id: "conv-1", message_id: "msg-1", rule_code: "PRIVATE_CONTACT", risk_level: "high", matched_snippet: "示例脱敏片段", status: "open", reviewer_id: null, review_note: null, project_id: "project_1" }] },
    "/api/v1/compliance/wecom/retention-policies": { items: [{ id: "wrp-1", scope_type: "global", scope_id: null, retention_days: 180, expiry_action: "archive", enabled: true, updated_at: "2026-06-15T12:00:00.000Z" }] },
    "/api/v1/analytics/business-metric-snapshots": state.metricSnapshots,
    "/api/v1/analytics/group-overview": { asOf: "2026-06-19T00:00:00Z", businessDate: "2026-06-19", projects: [{ projectId: "project_1", projectName: "综合体一号", merchantCount: 3, metrics: {} }] },
    "/api/v1/floor-plans": [{ id: "fp-1", plan_name: "示例平面图", building_code: "B1", floor_no: "1F", version_no: "v1", status: "draft" }],
    "/api/v1/analytics/robot": [{ metric_id: "rm-1", robot_id: "robot-1", provider_code: "mockbot", task_count: 10, success_count: 9, failure_count: 1, business_date: "2026-07-01", summary: "示例机器人" }],
  };
  return routes[pathname] ?? empty;
}

async function requestBody(route: Route) {
  try {
    return route.request().postDataJSON() as Record<string, unknown>;
  } catch {
    return {};
  }
}

function bodyValue(body: Record<string, unknown>, camelKey: string, snakeKey = camelKey) {
  return body[camelKey] ?? body[snakeKey];
}

// 按账号类型返回有效权限（与 admin 前端 hasPermissions 对齐：DENY 优先 / wildcard / fail-safe）。
// platform_admin → wildcard；其它账号给最小可用 grants，平台页（如财务）默认无权访问。
function effectivePermissionsFor(options: MockOptions) {
  const accountType = options.user?.accountType ?? "platform_admin";
  if (accountType === "platform_admin") {
    return { grants: ["bar.payroll.view", "bar.payroll.manage"], denies: [], wildcard: true, computedAt: now };
  }
  if (accountType === "merchant_operator") {
    // 对齐页面-权限矩阵 ADM-MER-002（MERCHANT_OPERATOR 可用商户授权工作台，需工单+账单两项读权限），
    // 加商户专区（ADM-MER-005/006/007）结算/合同/费用通知权限（商户专区一期落地口径）。
    return {
      grants: [
        "merchant.service_work_order.view",
        "merchant.billing.view",
        "merchant.portal.settlement.view",
        "contract.view",
        "merchant.portal.fee_recipient.manage",
        "finance.dividend.view",
      ],
      denies: [],
      wildcard: false,
      computedAt: now,
    };
  }
  if (accountType === "outsourced_vendor") {
    return {
      grants: [
        "facility.work_order.view_assigned",
        "facility.work_order.process_assigned",
        "facility.equipment.view",
        "facility.inspection.view_assigned",
        "facility.inspection.record_assigned",
        "facility.inspection.template.view",
        "facility.inspection.report_exception_assigned",
        "facility.site_entry.view_assigned",
        "facility.site_entry.create_assigned",
        "facility.site_entry.submit_assigned",
        "facility.site_entry.upload_completion_assigned",
        "partner.subordinate.view",
        "partner.subordinate.manage",
      ],
      denies: [],
      wildcard: false,
      computedAt: now,
    };
  }
  // 其它受限账号（如 bar_operator）：仅授予自身域读权限，不含平台财务等。
  return { grants: ["bar.table_session.view", "bar.table.view", "bar.reservation.view", "bar.order.view", "bar.table.guest_checkin", "bar.table_session.transfer", "bar.table_session.merge", "bar.table_session.renew"], denies: [], wildcard: false, computedAt: now };
}

export async function installMockApi(page: Page, options: MockOptions = {}) {
  const state = createState(Boolean(options.empty));
  // 高危二次确认（HighRiskConfirm 使用 window.confirm）在自动化下默认会被 dismiss，
  // 统一自动 accept，使写动作成功路径可被验收；precheck 失败时不会弹框，不受影响。
  page.on("dialog", (dialog) => {
    // prompt（存档/删除理由）需给非空值，否则前端â理由必填â守卫会中止写动作。
    void dialog.accept(dialog.type() === "prompt" ? "e2e-reason" : undefined);
  });
  await page.route(/\/api\/(v1|open)\//, async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const isWrite = ["POST", "PUT", "PATCH", "DELETE"].includes(method);
    const forbiddenPaths = [options.forbiddenPath, ...(options.forbiddenPaths ?? [])];
    const errorPaths = [options.errorPath, ...(options.errorPaths ?? [])];

    if (isWrite && pathMatches(url.pathname, options.postForbiddenPaths ?? [])) {
      return json(route, { code: "FORBIDDEN", message: "mock forbidden" }, 403);
    }
    if (isWrite && pathMatches(url.pathname, options.postErrorPaths ?? [])) {
      return json(route, { code: "SERVER_ERROR", message: "mock server error" }, 500);
    }
    if (pathMatches(url.pathname, forbiddenPaths)) {
      return json(route, { code: "FORBIDDEN", message: "mock forbidden" }, 403);
    }
    if (pathMatches(url.pathname, errorPaths)) {
      return json(route, { code: "SERVER_ERROR", message: "mock server error" }, 500);
    }

    if (url.pathname === "/api/v1/access/effective-permissions") {
      return json(route, effectivePermissionsFor(options));
    }
    if (isWrite) return handleWrite(route, url.pathname, state, options);
    if (url.pathname === "/api/v1/analytics/metric-detail") {
      // KPI 页 useMetricKpiValue 按 metric 拉明细取 summary_value；按指标返回对应值（与仪表盘同源），
      // 未覆盖指标退回既有票务收入明细桩（metric-detail 抽屉用例断言 TK-1/TK-2 行）。
      const metric = url.searchParams.get("metric") ?? "";
      const summaryByMetric: Record<string, number> = {
        "energy.total_electricity": 1200.5,
        "energy.electricity_per_sqm": 2.4,
      };
      if (Object.prototype.hasOwnProperty.call(summaryByMetric, metric)) {
        return json(route, { metric_key: metric, name: metric, value_type: "additive", unit: "", caliber: "", source: "mock", part: null, from: null, to: null, data_as_of: now, summary_value: summaryByMetric[metric], summary_note: null, columns: [], rows: [], page: 1, size: 1, total_rows: 0 });
      }
    }
    return json(route, fixedRows(url.pathname, state, options));
  });
}

async function handleWrite(route: Route, pathname: string, state: MockState, options: MockOptions) {
  const body = await requestBody(route);
  if (pathname === "/api/v1/auth/login") {
    // 对齐真实后端：登录失败 code=UNAUTHORIZED + detail_code=INVALID_CREDENTIALS（前端翻译为「账号或密码不正确」）。
    if (options.authFailure) return json(route, { code: "UNAUTHORIZED", detail_code: "INVALID_CREDENTIALS", message: "mock invalid credentials" }, 401);
    return json(route, { accessToken: "mock-token", refreshToken: "mock-refresh", sessionId: "session-1", tokenType: "Bearer", user: mockUser(options.user) });
  }
  if (pathname === "/api/v1/access/users") {
    const user = {
      id: `user-${state.users.length + 1}`,
      username: String(bodyValue(body, "username") ?? "new-staff"),
      phone: String(bodyValue(body, "phone") ?? ""),
      accountType: String(bodyValue(body, "accountType", "account_type") ?? "staff"),
      status: String(bodyValue(body, "status") ?? "active"),
      organizationId: String(bodyValue(body, "organizationId", "organization_id") ?? "org-1"),
      merchantId: String(bodyValue(body, "merchantId", "merchant_id") ?? "merchant-1"),
      consentReference: String(bodyValue(body, "consentReference", "consent_reference") ?? ""),
      evidenceAttachmentIds: (bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") as string[] | undefined) ?? [],
    };
    state.users.push(user);
    return json(route, user);
  }
  if (pathname === "/api/v1/access/permission-templates") {
    const permissions = (bodyValue(body, "permissionCodes", "permission_codes") as string[] | undefined) ?? [];
    const template = {
      id: `template-${state.permissionTemplates.length + 1}`,
      templateName: String(bodyValue(body, "templateName", "template_name") ?? "临时模板"),
      templateType: String(bodyValue(body, "templateType", "template_type") ?? "temporary"),
      status: "active",
      validFrom: bodyValue(body, "validFrom", "valid_from") ?? null,
      validUntil: bodyValue(body, "validUntil", "valid_until") ?? null,
      singleUse: Boolean(bodyValue(body, "singleUse", "single_use")),
      permissions,
      permissionCodes: permissions,
    };
    state.permissionTemplates.push(template);
    return json(route, template);
  }
  if (/^\/api\/v1\/access\/permission-templates\/[^/]+$/.test(pathname) && route.request().method() === "PUT") {
    const id = pathname.split("/").at(-1)!;
    const existing = state.permissionTemplates.find((template) => template.id === id);
    const permissions = (bodyValue(body, "permissionCodes", "permission_codes") as string[] | undefined) ?? existing?.permissions ?? [];
    const updated = {
      ...(existing ?? { id }),
      templateName: String(bodyValue(body, "templateName", "template_name") ?? existing?.templateName ?? id),
      templateType: String(bodyValue(body, "templateType", "template_type") ?? existing?.templateType ?? "base"),
      status: "active",
      validFrom: bodyValue(body, "validFrom", "valid_from") ?? existing?.validFrom ?? null,
      validUntil: bodyValue(body, "validUntil", "valid_until") ?? existing?.validUntil ?? null,
      singleUse: Boolean(bodyValue(body, "singleUse", "single_use")),
      permissions,
      permissionCodes: permissions,
    };
    if (existing) Object.assign(existing, updated);
    else state.permissionTemplates.push(updated);
    return json(route, updated);
  }
  if (/^\/api\/v1\/access\/permission-templates\/[^/]+\/copy$/.test(pathname)) {
    const sourceId = pathname.split("/").at(-2)!;
    const source = state.permissionTemplates.find((template) => template.id === sourceId);
    const permissions = source?.permissions ?? [];
    const template = { id: `template-copy-${state.permissionTemplates.length + 1}`, templateName: String(bodyValue(body, "templateName", "template_name") ?? "普通员工01"), templateType: "copy", status: "active", validFrom: source?.validFrom ?? null, validUntil: source?.validUntil ?? null, singleUse: Boolean(source?.singleUse), permissions, permissionCodes: permissions };
    state.permissionTemplates.push(template);
    return json(route, template);
  }
  if (/^\/api\/v1\/access\/permission-templates\/[^/]+\/apply$/.test(pathname)) {
    const mode = String(bodyValue(body, "mode") ?? "append");
    const targetUserIds = (bodyValue(body, "targetUserIds", "target_user_ids") as string[] | undefined) ?? [];
    return json(route, { status: mode === "single_use" ? "single_use_applied" : "applied", singleUse: mode === "single_use", targetCount: targetUserIds.length });
  }
  const tableSessionAction = pathname.match(/^\/api\/v1\/bar\/table-sessions\/([^/]+)\/(transfer|merge|renew)$/);
  if (tableSessionAction) {
    const [, sessionId, action] = tableSessionAction;
    const session = state.barTableSessions.find((item) => item.id === sessionId);
    if (!session) return json(route, { code: "NOT_FOUND", message: "session not found" }, 404);
    if (action === "transfer") {
      session.tableId = String(bodyValue(body, "targetTableId", "target_table_id") ?? session.tableId);
      session.sessionVersion = Number(session.sessionVersion ?? 1) + 1;
      return json(route, { id: `adj-${action}-1`, sessionId, adjustmentType: "transfer", toTableId: session.tableId, status: "transferred" });
    }
    if (action === "merge") {
      session.status = "merged";
      session.sessionVersion = Number(session.sessionVersion ?? 1) + 1;
      return json(route, { id: `adj-${action}-1`, sessionId, targetSessionId: bodyValue(body, "mergedSessionId", "merged_session_id"), adjustmentType: "merge", status: "merged" });
    }
    session.sessionVersion = Number(session.sessionVersion ?? 1) + 1;
    return json(route, { id: `adj-${action}-1`, sessionId, renewedUntil: bodyValue(body, "renewedUntil", "renewed_until"), adjustmentType: "renew", status: "renewed" });
  }
  if (pathname === "/api/v1/platform/business-entities") return json(route, { id: "org-created", name: "Created Org", orgType: "platform", status: "active", createdAt: now });
  if (pathname === "/api/v1/customer-accounts/lookup") {
    // 客人账号按手机号精确查（POST body 传参，PII 不进 URL）；返回数组，页面取 rows[0]。
    return json(route, [{ id: "cust-1", maskedPhone: "139****1234", status: "active", suspendedUntil: null, createdAt: now }]);
  }
  if (pathname === "/api/v1/platform/merchants") return json(route, { id: "merchant-created", organizationId: "org-created", displayName: "Created Merchant", companyName: "Created Company", mallBlockCode: "1F-A", areaSquareMeter: 128.5, leasedAreaSquareMeter: 120, secondaryContactName: "Jack", rentPerSquareMeter: 12.3, cooperationForm: "fixed_rent_plus_commission", status: "active", riskLevel: "normal" });
  if (pathname === "/api/v1/platform/projects") return json(route, { id: "venue-created", merchantId: "merchant-created", name: "Created Venue", venueType: "complex", status: "active" });
  if (pathname === "/api/v1/finance/merchant-expenses") return json(route, { id: "mex_m_created", merchantId: String(bodyValue(body, "merchantId", "merchant_id") ?? "merchant-1"), period: String(bodyValue(body, "period") ?? "2026-07"), expenseType: String(bodyValue(body, "expenseType", "expense_type") ?? "labor"), amountCent: Number(bodyValue(body, "amountCent", "amount_cent") ?? 0), source: "manual", sourceRef: null, note: String(bodyValue(body, "note") ?? ""), operatorUserId: "usr_admin", createdAt: now });
  if (/^\/api\/v1\/finance\/merchant-expenses\/[^/]+\/reverse$/.test(pathname)) return json(route, { id: `mex_r_${pathname.split("/").at(-2)}`, merchantId: "merchant-1", period: "2026-07", expenseType: "labor", amountCent: -500000, source: "manual", sourceRef: pathname.split("/").at(-2), note: String(bodyValue(body, "note") ?? ""), operatorUserId: "usr_admin", createdAt: now });
  if (pathname === "/api/v1/finance/merchant-expenses/sync-auto") return json(route, { period: String(bodyValue(body, "period") ?? "2026-07"), energyInserted: 1, propertyInserted: 1, rentInserted: 1 });
  if (pathname === "/api/v1/vouchers/definitions") return json(route, { id: "vdef_created", name: String(bodyValue(body, "name") ?? "新券"), voucherKind: String(bodyValue(body, "voucherKind", "voucher_kind") ?? "merchant_discount"), settlementMode: String(bodyValue(body, "settlementMode", "settlement_mode") ?? "none"), status: "active", createdAt: now, updatedAt: now });
  if (/^\/api\/v1\/vouchers\/instances\/[^/]+\/revoke$/.test(pathname)) return json(route, { id: pathname.split("/").at(-2), status: "revoked", revokeReason: String(bodyValue(body, "reason") ?? ""), updatedAt: now });
  if (pathname === "/api/v1/vouchers/instances") return json(route, { definitionId: String(bodyValue(body, "definitionId", "definition_id") ?? "vdef_1"), customerId: String(bodyValue(body, "customerId", "customer_id") ?? "customer-1"), quantity: Number(bodyValue(body, "quantity") ?? 1), instances: [] });
  if (pathname === "/api/v1/vouchers/redemptions") return json(route, { id: "vrr_created", voucherInstanceId: String(bodyValue(body, "voucherInstanceId", "voucher_instance_id") ?? "vi_1"), settlementMode: "none", redeemedAt: now });
  if (pathname === "/api/v1/finance/property-fee/rates") return json(route, { id: "pfr-created", projectId: String(bodyValue(body, "projectId", "project_id") ?? "venue-1"), effectivePeriod: bodyValue(body, "effectivePeriod", "effective_period") ?? null, ratePerSquareMeterCent: Number(bodyValue(body, "ratePerSquareMeterCent", "rate_per_square_meter_cent") ?? 2500), status: "active", createdAt: now, updatedAt: now });
  if (pathname === "/api/v1/finance/property-fee/bills") return json(route, [{ id: "pfb-1", projectId: String(bodyValue(body, "projectId", "project_id") ?? "venue-1"), period: String(bodyValue(body, "period") ?? "2026-07"), merchantId: "merchant-1", areaSquareMeter: 120, ratePerSquareMeterCent: 2500, amountCent: 300000, arApItemId: "arap-pf-1", createdAt: now }]);
  if (pathname === "/api/v1/platform/spaces") return json(route, { id: "space-created", areaId: "space-created", name: "Created Space", areaType: "operation", pointType: "gate", status: "active" });
  if (/^\/api\/v1\/platform\/business-entity-bindings\/[^/]+\/change$/.test(pathname)) return json(route, { id: pathname.split("/").at(-2), status: "binding_changed", reason: bodyValue(body, "reason") });
  if (pathname === "/api/v1/platform/contracts") {
    const contract = {
      id: `contract-${state.contracts.length + 1}`,
      admissionStep: "合同草稿/审批",
      merchantId: String(bodyValue(body, "merchantId", "merchant_id") ?? "merchant-1"),
      contractSubjectId: String(bodyValue(body, "contractSubjectId", "contract_subject_id") ?? "contract-subject-1"),
      operationSubjectId: String(bodyValue(body, "operationSubjectId", "operation_subject_id") ?? "operator-subject-1"),
      settlementSubjectId: String(bodyValue(body, "settlementSubjectId", "settlement_subject_id") ?? "settlement-subject-1"),
      effectiveDate: String(bodyValue(body, "effectiveDate", "effective_date") ?? "2026-07-01"),
      expiryDate: String(bodyValue(body, "expiryDate", "expiry_date") ?? "2027-06-30"),
      rentTerms: bodyValue(body, "rentTerms", "rent_terms") ?? { billingCycle: "monthly", baseRent: 12000 },
      shareholderRatioForReport: Number(bodyValue(body, "shareholderRatioForReport", "shareholder_ratio_for_report") ?? 25),
      attachmentIds: bodyValue(body, "attachmentIds", "attachment_ids") ?? [],
      lifecycleStatus: "draft",
      status: "draft",
      auditEvidenceRef: "audit://admission/contract-created",
      createdAt: now,
      updatedAt: now,
    };
    state.contracts.push(contract);
    return json(route, contract);
  }
  if (/^\/api\/v1\/platform\/contracts\/[^/]+\/approve$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const contract = state.contracts.find((item) => item.id === id);
    const decision = String(bodyValue(body, "decision") ?? "approve");
    const lifecycleStatus = decision === "approve" || decision === "approved" ? "approved" : decision === "reject" ? "rejected" : "returned";
    if (contract) {
      contract.lifecycleStatus = lifecycleStatus;
      contract.status = lifecycleStatus;
      contract.auditEvidenceRef = "audit://contract/approved";
      contract.updatedAt = now;
    }
    return json(route, { ...(contract ?? { id }), approvalStatus: lifecycleStatus, status: lifecycleStatus, evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? [], comment: bodyValue(body, "comment") });
  }
  if (pathname === "/api/v1/platform/supervisor-review/evaluate") return json(route, { reviewId: "review-demo", policyCode: "contract_violation_l3", actionType: "contract_violation", objectType: String(bodyValue(body, "objectType", "object_type") ?? "contract"), objectId: String(bodyValue(body, "objectId", "object_id") ?? "contract-1"), blocked: true, reviewStatus: "pending_review", requiredLevel: 3, decision: "blocked", reason: String(bodyValue(body, "violationSummary", "violation_summary") ?? "mock violation"), evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? [], evaluatedAt: now });
  if (/^\/api\/v1\/platform\/supervisor-review\/[^/]+\/approve$/.test(pathname)) return json(route, { reviewId: pathname.split("/").at(-2), policyCode: "manual_supervisor_review", actionType: "supervisor_review", objectType: "business_operation", objectId: pathname.split("/").at(-2), blocked: false, reviewStatus: String(bodyValue(body, "decision") ?? "approved"), requiredLevel: 3, reviewerLevel: Number(bodyValue(body, "reviewerLevel", "reviewer_level") ?? 3), decision: String(bodyValue(body, "decision") ?? "approved"), reason: String(bodyValue(body, "comment") ?? "mock approval"), evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? [], evaluatedAt: now });
  if (pathname === "/api/v1/ticketing/ticket-types") {
    const product = ticketProduct(`ticket-${state.tickets.length + 1}`, String(bodyValue(body, "ticketName", "ticket_name") ?? bodyValue(body, "productName", "product_name") ?? "P0 Web Pass"), Number(bodyValue(body, "weekdayPriceCent", "weekday_price_cent") ?? bodyValue(body, "priceAmount", "price_amount") ?? 9900));
    state.tickets.push(product);
    return json(route, product);
  }
  if (/^\/api\/v1\/finance\/reconciliation\/differences\/[^/]+\/close$/.test(pathname)) {
    return json(route, { id: pathname.split("/").at(-2), status: "closed", closeReason: String(bodyValue(body, "closeReason", "close_reason") ?? "") });
  }
  if (pathname === "/api/v1/ticketing/price-rules") {
    state.priceRules = state.priceRules ?? [];
    const rule = { id: `rule-${state.priceRules.length + 1}`, ticketTypeId: String(bodyValue(body, "ticketTypeId", "ticket_type_id") ?? ""), variantType: String(bodyValue(body, "variantType", "variant_type") ?? ""), priceMode: String(bodyValue(body, "priceMode", "price_mode") ?? ""), discountRate: bodyValue(body, "discountRate", "discount_rate"), weekdayPriceCent: bodyValue(body, "weekdayPriceCent", "weekday_price_cent"), status: "active" };
    state.priceRules.push(rule);
    return json(route, rule);
  }
  if (/^\/api\/v1\/ticketing\/ticket-types\/[^/]+\/publish$/.test(pathname)) {
    return json(route, { id: pathname.split("/").at(-2), status: "published" });
  }
  if (pathname === "/api/v1/ticketing/entitlement-templates") return json(route, { id: "tpl-new", name: "P0 Web Entry Entitlement", entitlementType: "entry", defaultTotalTimes: 1, defaultValueAmount: 9900, status: "active" });
  if (/^\/api\/v1\/ticketing\/ticket-types\/[^/]+\/entitlements$/.test(pathname)) return json(route, { id: "link-1", status: "active" });
  if (/^\/api\/v1\/ticketing\/ticket-types\/[^/]+\/issue$/.test(pathname)) {
    const entitlement = { id: `ent-${state.issuedEntitlements.length + 1}`, entitlementCode: `ENT-${state.issuedEntitlements.length + 1000}`, entitlementType: "entry", totalTimes: 1, usedTimes: 0, valueAmount: 9900, status: "active", createdAt: now };
    state.issuedEntitlements.push(entitlement);
    return json(route, { ticketCode: "TICKET-1000", issuedEntitlements: [entitlement] });
  }
  if (pathname === "/api/v1/ticketing/verifications/preview") return json(route, { status: "preview_ok", entitlementCode: "ENT-0001" });
  if (pathname === "/api/v1/ticketing/verifications") {
    const verification = { id: `ver-${state.verifications.length + 1}`, verificationNo: `VER-${state.verifications.length + 1000}`, status: "success", source: "web-p0", createdAt: now };
    state.verifications.push(verification);
    return json(route, verification);
  }
  if (pathname === "/api/v1/ticketing/dynamic-pricing-rules") {
    const rule = dynamicRule(`rule-${state.dynamicPricingRules.length + 1}`, state.tickets[0]?.id ?? "ticket-1", "Weekend Boost", "draft");
    state.dynamicPricingRules.push(rule);
    return json(route, rule);
  }
  if (/^\/api\/v1\/ticketing\/dynamic-pricing-rules\/[^/]+\/publish$/.test(pathname)) {
    const rule = state.dynamicPricingRules[0] ?? dynamicRule("rule-1", "ticket-1", "Weekend Boost", "draft");
    rule.status = "published";
    return json(route, rule);
  }
  if (pathname === "/api/v1/ticketing/dynamic-pricing/simulations") return json(route, { ticketTypeId: state.tickets[0]?.id ?? "ticket-1", visitDate: "2026-06-20", salesChannel: "web", basePriceCent: 9900, adjustmentCent: 990, finalPriceCent: 10890, matchedRuleId: state.dynamicPricingRules[0]?.id, explanation: "mock dynamic pricing simulation" });
  if (pathname === "/api/v1/ticketing/orders") {
    const rule = state.dynamicPricingRules.find((item) => item.status === "published");
    return json(route, { id: "ticket-order-1", orderNo: "TO-MOCK-1", ticketTypeId: state.tickets[0]?.id ?? "ticket-1", quantity: 1, visitDate: "2026-06-20", salesChannel: "web", amountCent: rule ? 10890 : 9900, dynamicPricingRuleId: rule?.id, baseAmountCent: 9900, dynamicAdjustmentCent: rule ? 990 : 0, payStatus: "unpaid", issueStatus: "not_issued", status: "created" });
  }
  if (/^\/api\/v1\/ticketing\/orders\/[^/]+\/issue$/.test(pathname)) return json(route, { id: pathname.split("/").at(-2), issueStatus: "issued", status: "issued" });
  if (pathname === "/api/v1/finance/ar-ap-items") return json(route, { id: "bill-created", billNo: "BILL-CREATED", merchantId: "merchant-1", totalAmount: 9900, status: "pending", createdAt: now });
  if (pathname === "/api/v1/integration/polling-checks") return json(route, { id: "polling-check-1", status: "recorded", operatorNote: String(bodyValue(body, "operatorNote", "operator_note") ?? ""), checkedEndpoints: bodyValue(body, "checkedEndpoints", "checked_endpoints") ?? [], pendingCount: bodyValue(body, "pendingCount", "pending_count") ?? 0, sourceCount: bodyValue(body, "sourceCount", "source_count") ?? 0, productionBoundary: "platform_task_status_only_not_supplier_execution", checkedAt: now });
  if (/^\/api\/v1\/finance\/ar-ap-items\/[^/]+\/payments$/.test(pathname)) return json(route, { id: "payment-1", status: "partial", amount: bodyValue(body, "amount"), reason: bodyValue(body, "reason") });
  if (pathname === "/api/v1/bar/tables") return json(route, { id: "table-created", tableCode: "A02", tableName: "A02", capacity: 4, minSpendAmount: 10000, status: "active" });
  if (pathname === "/api/v1/bar/products") return json(route, { id: "drink-created", barId: "bar-main", productName: "Mock Drink", productType: "spirit", priceCent: 3000, internalSettlementPriceCent: 1200, status: "active", soldOut: false });
  if (pathname === "/api/v1/bar/orders") return json(route, { id: "order-created", tableId: "table-created", status: "ordered", totalAmount: 3000, createdAt: now, items: [], bills: [] });
  if (/^\/api\/v1\/bar\/orders\/[^/]+\/items$/.test(pathname)) return json(route, { id: "order-item-1", status: "ordered", quantity: 1, lineAmount: 3000 });
  if (/^\/api\/v1\/bar\/orders\/[^/]+\/bill$/.test(pathname)) return json(route, { id: "bill-bar-1", status: "billed", paidAmount: 0, totalAmount: 3000 });
  if (/^\/api\/v1\/bar\/orders\/[^/]+\/deliver$/.test(pathname)) return json(route, { id: pathname.split("/").at(-2), status: "delivered" });
  if (/^\/api\/v1\/bar\/bills\/[^/]+\/commissions\/calculate$/.test(pathname)) return json(route, { id: "commission-1", billId: pathname.split("/").at(-3), salesRate: 0.15, nonSalesRate: 0.1, includeTicketAmount: false, status: "calculated" });
  if (/^\/api\/v1\/bar\/bills\/[^/]+\/settle$/.test(pathname)) return json(route, { id: "bill-bar-1", status: "paid", paidAmount: 3000, totalAmount: 3000 });
  if (/^\/api\/v1\/bar\/reservations\/[^/]+\/check-in-guest$/.test(pathname)) return json(route, { id: "checkin-1", status: "checked_in", guestCount: bodyValue(body, "guestCount", "guest_count") });
  if (pathname === "/api/v1/inventory/stock-movements") return json(route, { id: "move-1", status: "success", from_location_id: bodyValue(body, "from_location_id"), to_location_id: bodyValue(body, "to_location_id"), items: bodyValue(body, "items"), trace_code: bodyValue(body, "trace_code") });
  if (pathname === "/api/v1/inventory/items") return json(route, { id: "item-created", itemCode: "NEW", itemName: "New Item", status: "active" });
  if (pathname === "/api/v1/inventory/saved-bottles") return json(route, { id: "saved-1", savedNo: "SAVED-1", customerName: "Guest", status: "stored" });
  if (/^\/api\/v1\/inventory\/saved-bottles\/[^/]+\/claim$/.test(pathname)) return json(route, { id: "claim-1", status: "claimed", claim_items: bodyValue(body, "claim_items"), verification_code: bodyValue(body, "verification_code") });
  if (pathname === "/api/v1/amusement/projects") return json(route, { id: "amuse-created", projectCode: "P0WEB", projectName: "P0 Web Project", capacity: 20, status: "active", sessions: [] });
  if (/^\/api\/v1\/amusement\/projects\/[^/]+\/rules$/.test(pathname)) return json(route, { id: "amuse-rule-1", ruleType: "age", status: "active" });
  if (pathname === "/api/v1/amusement/sessions") return json(route, { id: "session-created", projectId: "amuse-created", sessionName: "P0 Web Session", startsAt: now, endsAt: now, capacity: 10, usedCapacity: 0, lockedCapacity: 0, status: "scheduled" });
  if (pathname === "/api/v1/amusement/verifications") return json(route, { id: "amuse-ver-1", status: "success" });
  if (pathname === "/api/v1/amusement/group-bookings") return json(route, { id: "gb-created", bookingStatus: "pending" });
  if (pathname === "/api/v1/amusement/safety-inspections") {
    const inspection = {
      id: `safety-${state.safetyInspections.length + 1}`,
      inspectionNo: `SAFE-${state.safetyInspections.length + 1}`,
      projectId: String(bodyValue(body, "projectId", "project_id") ?? "amuse-1"),
      projectName: "Adventure",
      templateId: String(bodyValue(body, "templateId", "template_id") ?? "safety-template-1"),
      overallResult: String(bodyValue(body, "overallResult", "overall_result") ?? "pending_rectification"),
      inspectionStatus: "created",
      rectificationStatus: "pending",
      reopenStatus: "pending",
      evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") as string[] | undefined ?? ["att-amusement-safety-create"],
      inspectedAt: String(bodyValue(body, "inspectedAt", "inspected_at") ?? now),
      createdAt: now,
      updatedAt: now,
      items: bodyValue(body, "items") ?? [],
    };
    state.safetyInspections.push(inspection);
    return json(route, inspection);
  }
  if (/^\/api\/v1\/amusement\/safety-inspections\/[^/]+\/submit$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const inspection = state.safetyInspections.find((item) => item.id === id) ?? state.safetyInspections[0];
    if (inspection) {
      inspection.inspectionStatus = "submitted";
      inspection.rectificationStatus = "pending";
      inspection.reopenStatus = "pending";
      inspection.updatedAt = now;
      inspection.evidenceAttachmentIds = bodyValue(body, "attachmentIds", "attachment_ids") as string[] | undefined ?? inspection.evidenceAttachmentIds;
    }
    return json(route, inspection ?? { id, inspectionStatus: "submitted", rectificationStatus: "pending" });
  }
  if (/^\/api\/v1\/amusement\/safety-inspections\/[^/]+\/rectify$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const inspection = state.safetyInspections.find((item) => item.id === id) ?? state.safetyInspections[0];
    if (inspection) {
      inspection.inspectionStatus = "submitted";
      inspection.rectificationStatus = "rectification_submitted";
      inspection.reopenStatus = "pending";
      inspection.operatorNote = String(bodyValue(body, "comment") ?? bodyValue(body, "operatorNote", "operator_note") ?? "rectified");
      inspection.evidenceAttachmentIds = bodyValue(body, "attachmentIds", "attachment_ids") as string[] | undefined ?? inspection.evidenceAttachmentIds;
      inspection.updatedAt = now;
    }
    return json(route, inspection ?? { id, rectificationStatus: "rectification_submitted", status: "rectified" });
  }
  if (/^\/api\/v1\/amusement\/safety-inspections\/[^/]+\/recheck$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const inspection = state.safetyInspections.find((item) => item.id === id) ?? state.safetyInspections[0];
    if (inspection) {
      inspection.rectificationStatus = "rechecked";
      inspection.reopenStatus = "pending";
      inspection.evidenceAttachmentIds = bodyValue(body, "attachmentIds", "attachment_ids") as string[] | undefined ?? inspection.evidenceAttachmentIds;
      inspection.updatedAt = now;
    }
    return json(route, inspection ?? { id, rectificationStatus: "rechecked", reopenStatus: "pending" });
  }
  if (/^\/api\/v1\/amusement\/safety-inspections\/[^/]+\/approve-reopen$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const inspection = state.safetyInspections.find((item) => item.id === id) ?? state.safetyInspections[0];
    if (inspection) {
      inspection.reopenStatus = "approved";
      inspection.evidenceAttachmentIds = bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") as string[] | undefined ?? inspection.evidenceAttachmentIds;
      inspection.updatedAt = now;
    }
    return json(route, inspection ?? { id, reopenStatus: "approved" });
  }
  if (/^\/api\/v1\/amusement\/group-bookings\/[^/]+\/release$/.test(pathname)) {
    const booking = state.groupBookings[0];
    if (booking) booking.bookingStatus = "released";
    return json(route, booking ?? null);
  }
  if (/^\/api\/v1\/amusement\/group-bookings\/[^/]+\/approve$/.test(pathname)) {
    const booking = state.groupBookings[0];
    if (booking) booking.bookingStatus = "approved";
    return json(route, booking ?? null);
  }
  if (pathname === "/api/v1/analytics/risk-alerts") {
    const alert = {
      id: `risk-${state.riskAlerts.length + 1}`,
      ruleId: String(bodyValue(body, "ruleId", "rule_id") ?? "risk-rule-1"),
      alertType: String(bodyValue(body, "alertType", "alert_type") ?? "staff_behavior"),
      riskLevel: String(bodyValue(body, "riskLevel", "risk_level") ?? "high"),
      objectType: String(bodyValue(body, "objectType", "object_type") ?? "bar_order"),
      objectId: String(bodyValue(body, "objectId", "object_id") ?? "order-1"),
      summary: String(bodyValue(body, "summary") ?? "mock risk alert"),
      ownerUserId: "ops-1",
      status: "open",
      evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? [],
      createdAt: now,
      updatedAt: now,
      resolvedAt: null as string | null,
    };
    state.riskAlerts.push(alert);
    return json(route, alert);
  }
  if (/^\/api\/v1\/analytics\/risk-alerts\/[^/]+\/resolve$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const alert = state.riskAlerts.find((item) => item.id === id);
    if (alert) {
      alert.status = String(bodyValue(body, "action") ?? "resolved");
      alert.evidenceAttachmentIds = bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? alert.evidenceAttachmentIds;
      alert.updatedAt = now;
      alert.resolvedAt = now;
    }
    return json(route, alert ?? { id, status: String(bodyValue(body, "action") ?? "resolved") });
  }
  if (/^\/api\/v1\/integration\/retry-tasks\/[^/]+\/run-now$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const task = state.retryTasks.find((item) => item.id === id) ?? state.retryTasks[0];
    if (task) {
      task.status = "ready";
      task.retryCount = Number(task.retryCount ?? 0) + 1;
    }
    return json(route, task ?? { id, status: "ready", retryCount: 1 });
  }
  if (/^\/api\/v1\/integration\/compensation-tasks\/[^/]+\/resolve$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const task = state.compensationTasks.find((item) => item.id === id) ?? state.compensationTasks[0];
    if (task) {
      task.status = String(bodyValue(body, "status") ?? "resolved");
      task.evidenceAttachmentIds = bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") as string[] | undefined ?? [];
    }
    return json(route, task ?? { id, status: String(bodyValue(body, "status") ?? "resolved"), evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? [] });
  }
  if (pathname === "/api/v1/evidence/events") {
    const events = Array.isArray(body.events) ? body.events : [];
    const created = events.map((event, index) => ({
      id: `event-${state.evidenceEvents.length + index + 1}`,
      eventType: String((event as Record<string, unknown>).event_type ?? "evidence.events_reported"),
      eventCategory: String((event as Record<string, unknown>).event_category ?? "audit"),
      sourceType: "web",
      result: String((event as Record<string, unknown>).result ?? "success"),
      riskLevel: "normal",
      traceId: `event-trace-export-${state.evidenceEvents.length + index + 1}`,
      objectType: "audit_export",
      objectId: String(body.idempotency_key ?? `audit-export-${index}`),
      summary: String((event as Record<string, unknown>).summary ?? body.operator_note ?? "audit export"),
      beforeAfter: { before: {}, after: { exported: true } },
      payload: (event as Record<string, unknown>).payload ?? {},
      occurredAt: String((event as Record<string, unknown>).occurred_at ?? now),
    }));
    state.evidenceEvents.push(...created);
    return json(route, { status: "evidence.events_reported", events: created, evidenceAttachmentIds: bodyValue(body, "evidenceAttachmentIds", "evidence_attachment_ids") ?? [] });
  }
  if (pathname === "/api/v1/integration/provider-adapters") {
    const adapter = providerAdapter(`adapter-${state.providerAdapters.length + 1}`, `provider-${state.providerAdapters.length + 1}`, String(bodyValue(body, "adapterType", "adapter_type") ?? "reservation_resource"));
    adapter.credentialSecretRef = String(bodyValue(body, "credentialSecretRef", "credential_secret_ref") ?? "cred://test/provider");
    state.providerAdapters.push(adapter);
    return json(route, adapter);
  }
  if (/^\/api\/v1\/integration\/provider-adapters\/[^/]+\/mark-production-ready$/.test(pathname)) return json(route, { id: pathname.split("/").at(-2), productionReady: true, status: "ready" });
  if (/^\/api\/v1\/integration\/provider-adapters\/[^/]+\/test-mock$/.test(pathname)) return json(route, { id: "adapter_test_mock_1", testStatus: "passed", result: {} });
  const facilityWorkOrderAction = pathname.match(/^\/api\/v1\/facility\/work-orders\/([^/]+)\/(start|complete|accept)$/);
  if (facilityWorkOrderAction) {
    const [, id, action] = facilityWorkOrderAction;
    const workOrder = state.facilityWorkOrders.find((item) => item.id === id) ?? state.facilityWorkOrders[0];
    if (workOrder) {
      workOrder.status = action === "start" ? "processing" : action === "complete" ? "completed" : "accepted";
      workOrder.updatedAt = now;
    }
    return json(route, { ...(workOrder ?? { id }), action, resolution: bodyValue(body, "resolution"), satisfaction: bodyValue(body, "satisfaction"), note: bodyValue(body, "note") });
  }
  const facilityInspectionStart = pathname.match(/^\/api\/v1\/facility\/inspections\/tasks\/([^/]+)\/start$/);
  if (facilityInspectionStart) {
    const id = facilityInspectionStart[1];
    const task = state.facilityInspectionTasks.find((item) => item.id === id) ?? state.facilityInspectionTasks[0];
    if (task) {
      task.status = "in_progress";
      task.updatedAt = now;
    }
    return json(route, { ...(task ?? { id }), note: bodyValue(body, "note") });
  }
  const facilityInspectionRecord = pathname.match(/^\/api\/v1\/facility\/inspections\/tasks\/([^/]+)\/records$/);
  if (facilityInspectionRecord) {
    const taskId = facilityInspectionRecord[1];
    const task = state.facilityInspectionTasks.find((item) => item.id === taskId) ?? state.facilityInspectionTasks[0];
    if (task) {
      task.status = "completed";
      task.updatedAt = now;
    }
    return json(route, { id: `fir-${state.facilityInspectionRecords.length + 1}`, taskId, result: String(bodyValue(body, "result") ?? "normal"), photos: bodyValue(body, "photos") ?? [], checklistResult: bodyValue(body, "checklistResult", "checklist_result") ?? [], note: bodyValue(body, "note"), abnormalNote: bodyValue(body, "abnormalNote", "abnormal_note"), inspectorId: String(bodyValue(body, "inspectorId", "inspector_id") ?? "vendor-user"), recordedAt: now, createdAt: now });
  }
  const facilityInspectionException = pathname.match(/^\/api\/v1\/facility\/inspections\/tasks\/([^/]+)\/report-exception$/);
  if (facilityInspectionException) {
    const taskId = facilityInspectionException[1];
    const task = state.facilityInspectionTasks.find((item) => item.id === taskId) ?? state.facilityInspectionTasks[0];
    if (task) {
      task.status = "abnormal";
      task.updatedAt = now;
      task.workOrderId = task.workOrderId ?? "fwo-exception-1";
    }
    const record = {
      id: `fir-${state.facilityInspectionRecords.length + 1}`,
      taskId,
      result: "abnormal",
      photos: bodyValue(body, "photos") ?? [],
      qrCode: bodyValue(body, "qrCode", "qr_code") ?? "QR-EXCEPTION",
      gpsLat: bodyValue(body, "gpsLat", "gps_lat") ?? null,
      gpsLng: bodyValue(body, "gpsLng", "gps_lng") ?? null,
      checklistResult: bodyValue(body, "checklistResult", "checklist_result") ?? [],
      note: bodyValue(body, "note"),
      abnormalNote: bodyValue(body, "abnormalNote", "abnormal_note"),
      inspectorId: String(bodyValue(body, "inspectorId", "inspector_id") ?? "vendor-user"),
      recordedAt: now,
      createdAt: now,
    };
    state.facilityInspectionRecords.push(record as (typeof state.facilityInspectionRecords)[number]);
    return json(route, record);
  }
  if (pathname === "/api/v1/facility/site-entry-reports") {
    const report = { id: `ser-${state.siteEntryReports.length + 1}`, projectId: String(bodyValue(body, "projectId", "project_id") ?? "venue-1"), spaceId: String(bodyValue(body, "spaceId", "space_id") ?? "area-1"), workScope: String(bodyValue(body, "workScope", "work_scope") ?? "店铺装修围挡施工"), plannedStartDate: String(bodyValue(body, "plannedStartDate", "planned_start_date") ?? "2026-06-20"), plannedEndDate: String(bodyValue(body, "plannedEndDate", "planned_end_date") ?? "2026-06-22"), crewCount: Number(bodyValue(body, "crewCount", "crew_count") ?? 1), contactName: String(bodyValue(body, "contactName", "contact_name") ?? "王工"), contactPhone: String(bodyValue(body, "contactPhone", "contact_phone") ?? "13800000000"), vendorUserId: options.user?.id ?? "vendor-user", status: "submitted", reviewComment: "", acceptanceComment: null, completionAttachmentIds: [] as string[], completionNote: null as string | null, completionUploadedAt: null as string | null, createdBy: options.user?.id ?? "vendor-user", createdAt: now, updatedAt: now, deletedAt: null };
    state.siteEntryReports.push(report);
    return json(route, report);
  }
  if (/^\/api\/v1\/facility\/site-entry-reports\/[^/]+\/submit$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const report = state.siteEntryReports.find((item) => item.id === id) ?? state.siteEntryReports[0];
    if (report) {
      report.status = "in_progress";
      report.updatedAt = now;
    }
    return json(route, { ...(report ?? { id }), note: bodyValue(body, "note") });
  }
  if (/^\/api\/v1\/facility\/site-entry-reports\/[^/]+\/completion$/.test(pathname)) {
    const id = pathname.split("/").at(-2);
    const report = state.siteEntryReports.find((item) => item.id === id) ?? state.siteEntryReports[0];
    const attachmentIds = bodyValue(body, "attachmentIds", "attachment_ids") ?? bodyValue(body, "completionAttachmentIds", "completion_attachment_ids") ?? [];
    if (report) {
      report.completionAttachmentIds = Array.isArray(attachmentIds) ? attachmentIds.map(String) : [String(attachmentIds)];
      report.completionNote = String(bodyValue(body, "note") ?? bodyValue(body, "completionNote", "completion_note") ?? "完工材料已上传");
      report.completionUploadedAt = now;
      report.updatedAt = now;
    }
    return json(route, report ?? { id, completionAttachmentIds: attachmentIds, completionUploadedAt: now });
  }
  if (pathname === "/api/v1/merchant-service/work-orders") {
    const workOrder = { id: `mswo-${state.merchantWorkOrders.length + 1}`, merchantId: String(bodyValue(body, "merchantId", "merchant_id") ?? "merchant-1"), spaceId: String(bodyValue(body, "spaceId", "space_id") ?? "area-1"), contractId: String(bodyValue(body, "contractId", "contract_id") ?? "contract-1"), workOrderType: String(bodyValue(body, "workOrderType", "work_order_type") ?? "repair"), priority: String(bodyValue(body, "priority") ?? "normal"), status: "created", title: String(bodyValue(body, "title") ?? "???"), description: String(bodyValue(body, "description") ?? "mock merchant service work order"), reportedByUserId: String(bodyValue(body, "reportedByUserId", "reported_by_user_id") ?? "staff_reporter"), reportedChannel: String(bodyValue(body, "reportedChannel", "reported_channel") ?? "web"), ownerDepartment: "", ownerUserId: "", dueAt: String(bodyValue(body, "dueAt", "due_at") ?? now), evidenceAttachmentIds: [], createdAt: now, updatedAt: now };
    state.merchantWorkOrders.push(workOrder);
    return json(route, workOrder);
  }
  if (/^\/api\/v1\/merchant-service\/work-orders\/[^/]+\/assign$/.test(pathname)) {
    const workOrder = state.merchantWorkOrders[0];
    if (workOrder) {
      workOrder.status = "assigned";
      workOrder.ownerDepartment = String(bodyValue(body, "ownerDepartment", "owner_department") ?? "property");
      workOrder.ownerUserId = String(bodyValue(body, "ownerUserId", "owner_user_id") ?? "staff_owner");
      workOrder.updatedAt = now;
    }
    return json(route, workOrder ?? { id: pathname.split("/").at(-2), status: "assigned" });
  }
  if (/^\/api\/v1\/merchant-service\/work-orders\/[^/]+\/actions$/.test(pathname)) {
    const workOrder = state.merchantWorkOrders[0];
    if (workOrder) {
      workOrder.status = String(bodyValue(body, "targetStatus", "target_status") ?? "processing");
      workOrder.updatedAt = now;
    }
    return json(route, workOrder ?? { id: pathname.split("/").at(-2), status: "processing" });
  }
  if (/^\/api\/v1\/merchant-service\/work-orders\/[^/]+\/close$/.test(pathname)) {
    const workOrder = state.merchantWorkOrders[0];
    if (workOrder) {
      workOrder.status = "closed";
      workOrder.resolution = String(bodyValue(body, "resolution") ?? "closed");
      workOrder.updatedAt = now;
    }
    return json(route, workOrder ?? { id: pathname.split("/").at(-2), status: "closed" });
  }
  if (/^\/api\/v1\/ops\/production-readiness\/items\/[^/]+$/.test(pathname)) {
    const item = state.readinessItems[0];
    if (item) {
      item.status = String(bodyValue(body, "status") ?? "verified");
      item.evidenceRef = String(bodyValue(body, "evidenceRef", "evidence_ref") ?? "evidence://mock");
    }
    return json(route, item ?? { id: "blocker-1", status: "verified" });
  }
  if (pathname.startsWith("/api/open/")) return json(route, { accepted: true, requestId: "mock-open-webhook" });
  if (pathname.startsWith("/api/v1/floor-plans")) return json(route, { id: "floor-plan-mock", status: "success" });
  return json(route, { id: "mock-post", status: "success" });
}
