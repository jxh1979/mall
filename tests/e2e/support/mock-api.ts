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
  const businessBindings = empty ? [] : [{ id: "binding-1", admissionStep: "主体绑定", businessObjectId: "merchant-1", operatorSubject: "Main Operator Co", contractSubject: "Main Contract Co", contractSubjectId: "contract-subject-1", settlementSubject: "Main Settlement Co", settlementSubjectId: "settlement-subject-1", status: "active", auditEvidenceRef: "audit://admission/binding-1" }];
  const contracts = empty ? [] : [{ id: "contract-1", admissionStep: "合同草稿/审批", merchantId: "merchant-1", contractSubjectId: "contract-subject-1", operationSubjectId: "operator-subject-1", settlementSubjectId: "settlement-subject-1", effectiveDate: "2026-06-01", expiryDate: "2027-05-31", rentTerms: { billingCycle: "monthly", baseRent: 12000 }, shareholderRatioForReport: 0.25, attachmentIds: ["att-contract-1"], lifecycleStatus: "draft", status: "draft", auditEvidenceRef: "audit://admission/contract-1" }];
  const ticketOrders = empty ? [] : [
    { id: "ticket-order-1", orderNo: "TO-MOCK-1", ticketTypeId: "ticket-1", quantity: 1, visitDate: "2026-06-20", salesChannel: "web", amountCent: 9900, payStatus: "paid", issueStatus: "not_issued", status: "created" },
    ...fillRows(11, (i) => ({ id: `ticket-order-fill-${i + 2}`, orderNo: `TO-FILL-${i + 2}`, ticketTypeId: "ticket-1", quantity: 1, visitDate: "2026-06-21", salesChannel: i % 2 ? "miniapp" : "web", amountCent: 9900, payStatus: i % 2 ? "unpaid" : "paid", issueStatus: "not_issued", status: "created" })),
  ];
  const savedBottles = empty ? [] : [{ id: "saved-1", savedNo: "SAVED-1", customerName: "Guest A", itemName: "Wine", quantity: 1, status: "stored" }];
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
    { id: "anomaly-1", metricKey: "gmv_amount_cents", domain: "complex", riskLevel: "high", observedValue: 650000, expectedValue: 880000, summary: "Revenue dropped below threshold", status: "open", detectedAt: now, resolvedAt: null },
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
  const merchantBills = empty ? [] : [{ id: "merchant-bill-1", merchantId: "merchant-1", billNo: "MBILL-1", title: "物业费账单", totalAmount: 12000, status: "pending_confirm", authorizedScope: "merchant-1:billing", auditEvidenceRef: "audit://merchant-scope/bill-1", createdAt: now, updatedAt: now }];
  const supervisorReviewPolicies = empty ? [] : [{ id: "policy-1", policyCode: "contract_violation_l3", actionType: "contract_violation", minAmountCent: 0, requiredLevel: 3, blocksByDefault: true, requiredEvidenceTypes: ["violation_summary", "contract_snapshot"], status: "active" }, { id: "policy-2", policyCode: "base_definition_violation_l3", actionType: "base_definition_violation", minAmountCent: 0, requiredLevel: 3, blocksByDefault: true, requiredEvidenceTypes: ["violation_summary", "definition_snapshot"], status: "active" }];
  return {
    tickets,
    providerAdapters,
    businessBindings,
    contracts,
    ticketOrders,
    savedBottles,
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
    supervisorReviewPolicies,
    issuedEntitlements: empty ? [] : [{ id: "ent-1", entitlementCode: "ENT-0001", entitlementType: "entry", totalTimes: 1, usedTimes: 0, valueAmount: 9900, status: "active", createdAt: now }],
    verifications: empty ? [] : [{ id: "ver-1", verificationNo: "VER-0001", status: "success", source: "mock", createdAt: now }],
    dynamicPricingRules: empty ? [] : [dynamicRule("rule-1", "ticket-1", "Weekend Boost", "draft")],
    groupBookings: empty ? [] : [{ id: "gb-1", projectId: "amuse-1", sessionId: "session-1", bookingType: "group", customerName: "Team A", partySize: 6, reservedCapacity: 6, bookingStatus: "approved", evidenceAttachmentIds: [], createdAt: now, updatedAt: now }],
    amusementSessions,
    users: empty ? [] : [{ id: "user-1", username: "admin", accountType: "platform_admin", status: "active", organizationId: "org-1" }],
    permissionTemplates: empty ? [] : [{ id: "template-basic", templateName: "普通员工", templateType: "base", status: "active", validUntil: null, singleUse: false, permissions: ["overview.read"] }],
    financeItems: empty ? [] : [{ id: "bill-1", billNo: "BILL-1", sourceDoc: "SO-1", counterparty: "Main Merchant", direction: "receivable", totalAmount: 9900, paidAmount: 0, status: "pending", createdAt: now }],
    readinessItems: empty ? [] : [{ id: "blocker-1", blockerCode: "SEC-P0-OPS-RECOVERY-001", title: "NAS 恢复演练", owner: "IT", status: "open", evidenceRef: null, dueAt: "2026-06-30" }],
  };
}

type MockState = ReturnType<typeof createState>;

function fixedRows(pathname: string, state: MockState): ApiData {
  const empty: Record<string, unknown>[] = [];
  const routes: Record<string, ApiData> = {
    "/api/v1/system/health": { ok: true, service: "mock-api", time: now },
    "/api/v1/analytics/dashboards/p0-overview": { merchants: 1, ticketProducts: state.tickets.length, issuedTickets: state.issuedEntitlements.length, verifications: state.verifications.length, barOrders: 1, amusementSessions: state.amusementSessions.length, openRiskAlerts: 1, auditEvents: 2 },
    "/api/v1/platform/business-entities": [{ id: "org-1", name: "Platform Org", code: "ORG", orgType: "platform", status: "active", createdAt: now }],
    "/api/v1/platform/business-entity-bindings": state.businessBindings,
    "/api/v1/platform/contracts": state.contracts,
    "/api/v1/platform/supervisor-review-policies": state.supervisorReviewPolicies,
    "/api/v1/platform/merchants": [{ id: "merchant-1", admissionStep: "商户档案", organizationId: "org-1", displayName: "Main Merchant", companyName: "Main Company", mallBlockCode: "1F-A", areaSquareMeter: 128.5, leasedAreaSquareMeter: 120, secondaryContactName: "Jack", rentPerSquareMeter: 12.3, cooperationForm: "fixed_rent_plus_commission", status: "active", riskLevel: "normal", auditEvidenceRef: "audit://admission/merchant-1" }],
    "/api/v1/platform/projects": [{ id: "venue-1", merchantId: "merchant-1", name: "Main Venue", code: "VENUE", venueType: "complex", status: "active" }],
    "/api/v1/platform/spaces": [{ id: "area-1", venueId: "venue-1", areaId: "area-1", name: "Gate A", code: "GATE-A", areaType: "operation", pointType: "gate", status: "active" }],
    "/api/v1/access/users": state.users,
    "/api/v1/access/roles": [{ id: "role-1", roleCode: "BAR_MANAGER", roleName: "酒吧经理" }, { id: "role-2", roleCode: "FINANCE", roleName: "财务" }],
    "/api/v1/access/permissions": [{ code: "overview.read" }, { code: "finance.ledger.read" }, { code: "bar.table.read" }, { code: "permission_template.read" }],
    "/api/v1/access/permission-templates": state.permissionTemplates,
    "/api/v1/access/temporary-grants": empty,
    "/api/v1/analytics/merchants": [{ merchantId: "merchant-1", merchantName: "Main Merchant", merchantType: "complex", revenueAmountCents: 880000, orderCount: 8, riskAlertCount: 1, settlementStatus: "pending", businessDate: "2026-06-15", createdAt: now, updatedAt: now }],
    "/api/v1/ticketing/ticket-types": state.tickets,
    "/api/v1/ticketing/dynamic-pricing-rules": state.dynamicPricingRules,
    "/api/v1/ticketing/orders": state.ticketOrders,
    "/api/v1/ticketing/entitlement-templates": [{ id: "tpl-1", name: "Entry", entitlementType: "entry", defaultTotalTimes: 1, defaultValueAmount: 9900, status: "active" }],
    "/api/v1/ticketing/issued-entitlements": state.issuedEntitlements,
    "/api/v1/ticketing/verifications": state.verifications,
    "/api/v1/finance/ar-ap-items": state.financeItems,
    "/api/v1/finance/reconciliation/batches": state.reconciliationBatches,
    "/api/v1/finance/reconciliation/differences": state.reconciliationDifferences,
    "/api/v1/bar/tables": [{ id: "table-1", tableCode: "A01", tableName: "A01", capacity: 4, guestLimit: 4, minSpendAmount: 10000, barId: "bar-main", status: "active" }],
    "/api/v1/bar/products": [{ id: "drink-1", productName: "Mock Drink", category: "drink", priceAmount: 3000, status: "active" }],
    "/api/v1/bar/production-tickets": empty,
    "/api/v1/bar/reservations": [{ id: "reservation-1", reservationNo: "RSV-1", customerName: "Guest A", partySize: 2, depositAmount: 2000, status: "reserved", reservedAt: now }],
    "/api/v1/bar/orders": [{ id: "order-1", orderNo: "ORD-1", tableId: "table-1", billId: "bill-bar-1", sellerRole: "sales", commissionRate: "15%", status: "paid", totalAmount: 6000, createdAt: now, items: [], bills: [{ id: "bill-bar-1", status: "open" }] }],
    "/api/v1/inventory/stock-movements": empty,
    "/api/v1/inventory/items": [{ id: "item-1", itemCode: "WINE", itemName: "Wine", category: "drink", unit: "bottle", status: "active", minQuantity: 2 }],
    "/api/v1/inventory/stock-balances": [{ id: "stock-1", itemId: "item-1", locationType: "bar", quantity: 10 }],
    "/api/v1/inventory/saved-bottles": state.savedBottles,
    "/api/v1/amusement/projects": [{ id: "amuse-1", projectCode: "AMUSE", projectName: "Adventure", capacity: 20, status: "active", sessions: state.amusementSessions }],
    "/api/v1/amusement/sessions": state.amusementSessions,
    "/api/v1/amusement/verifications": empty,
    "/api/v1/amusement/reservations": empty,
    "/api/v1/amusement/queue-entries": empty,
    "/api/v1/amusement/device-statuses": empty,
    "/api/v1/amusement/safety-inspections": state.safetyInspections,
    "/api/v1/amusement/manual-overrides": empty,
    "/api/v1/amusement/group-bookings": state.groupBookings,
    "/api/v1/analytics/risk-rules": [{ id: "risk-rule-1", ruleCode: "RISK", ruleName: "Manual Risk", ruleType: "manual", threshold: 1, status: "active" }],
    "/api/v1/analytics/risk-alerts": state.riskAlerts,
    "/api/v1/evidence/events": state.evidenceEvents,
    "/api/v1/integration/provider-adapters": state.providerAdapters,
    "/api/v1/integration/order-syncs": state.orderSyncs,
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
    "/api/v1/ops/production-readiness/items": state.readinessItems,
    "/api/v1/ops/production-readiness/checks": [{ id: "check-1", status: "open", message: "NAS 恢复演练待回填" }],
    "/api/v1/analytics/robot": empty,
    "/api/v1/analytics/metric-definitions": state.metricDefinitions,
    "/api/v1/analytics/ai-quality/tasks": empty,
    "/api/v1/analytics/ai-quality/results": empty,
    "/api/v1/analytics/customer-intents": empty,
    "/api/v1/analytics/staff-service-score": empty,
    "/api/v1/analytics/merchant-health": empty,
    "/api/v1/merchant-service/work-orders": state.merchantWorkOrders,
    "/api/v1/merchant-billing/bills": state.merchantBills,
    "/api/v1/analytics/metric-anomalies": state.metricAnomalies,
    "/api/v1/analytics/business-metric-snapshots": state.metricSnapshots,
    "/api/v1/analytics/group-overview": { asOf: "2026-06-19T00:00:00Z", businessDate: "2026-06-19", projects: [{ projectId: "project_1", projectName: "综合体一号", merchantCount: 3, metrics: {} }] },
    "/api/v1/floor-plans": empty,
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
    return { grants: [], denies: [], wildcard: true, computedAt: now };
  }
  if (accountType === "merchant_operator") {
    return {
      grants: ["merchant.portal.read", "merchant.service_work_order.view"],
      denies: [],
      wildcard: false,
      computedAt: now,
    };
  }
  // 其它受限账号（如 bar_operator）：仅授予自身域读权限，不含平台财务等。
  return { grants: ["bar.table.read", "bar.order.read"], denies: [], wildcard: false, computedAt: now };
}

export async function installMockApi(page: Page, options: MockOptions = {}) {
  const state = createState(Boolean(options.empty));
  // 高危二次确认（HighRiskConfirm 使用 window.confirm）在自动化下默认会被 dismiss，
  // 统一自动 accept，使写动作成功路径可被验收；precheck 失败时不会弹框，不受影响。
  page.on("dialog", (dialog) => {
    void dialog.accept();
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
    return json(route, fixedRows(url.pathname, state));
  });
}

async function handleWrite(route: Route, pathname: string, state: MockState, options: MockOptions) {
  const body = await requestBody(route);
  if (pathname === "/api/v1/auth/login") {
    if (options.authFailure) return json(route, { code: "UNAUTHORIZED", message: "mock invalid credentials" }, 401);
    return json(route, { accessToken: "mock-token", refreshToken: "mock-refresh", sessionId: "session-1", tokenType: "Bearer", user: mockUser(options.user) });
  }
  if (pathname === "/api/v1/access/users") {
    const user = { id: `user-${state.users.length + 1}`, username: String(bodyValue(body, "username") ?? "new-staff"), accountType: String(bodyValue(body, "accountType", "account_type") ?? "bar_manager"), status: "active", organizationId: "org-1" };
    state.users.push(user);
    return json(route, user);
  }
  if (pathname === "/api/v1/access/permission-templates") {
    const template = { id: `template-${state.permissionTemplates.length + 1}`, templateName: String(bodyValue(body, "templateName", "template_name") ?? "临时模板"), templateType: "temporary", status: "active", singleUse: Boolean(bodyValue(body, "singleUse", "single_use")) };
    state.permissionTemplates.push(template);
    return json(route, template);
  }
  if (/^\/api\/v1\/access\/permission-templates\/[^/]+\/copy$/.test(pathname)) {
    const template = { id: "template-copy", templateName: String(bodyValue(body, "templateName", "template_name") ?? "普通员工01"), templateType: "copy", status: "active", singleUse: false };
    state.permissionTemplates.push(template);
    return json(route, template);
  }
  if (/^\/api\/v1\/access\/permission-templates\/[^/]+\/apply$/.test(pathname)) {
    return json(route, { status: Boolean(bodyValue(body, "singleUse", "single_use")) ? "single_use_applied" : "applied", singleUse: Boolean(bodyValue(body, "singleUse", "single_use")) });
  }
  if (pathname === "/api/v1/platform/business-entities") return json(route, { id: "org-created", name: "Created Org", orgType: "platform", status: "active", createdAt: now });
  if (pathname === "/api/v1/platform/merchants") return json(route, { id: "merchant-created", organizationId: "org-created", displayName: "Created Merchant", companyName: "Created Company", mallBlockCode: "1F-A", areaSquareMeter: 128.5, leasedAreaSquareMeter: 120, secondaryContactName: "Jack", rentPerSquareMeter: 12.3, cooperationForm: "fixed_rent_plus_commission", status: "active", riskLevel: "normal" });
  if (pathname === "/api/v1/platform/projects") return json(route, { id: "venue-created", merchantId: "merchant-created", name: "Created Venue", venueType: "complex", status: "active" });
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
    const product = ticketProduct(`ticket-${state.tickets.length + 1}`, String(bodyValue(body, "productName", "product_name") ?? "P0 Web Pass"), Number(bodyValue(body, "priceAmount", "price_amount") ?? 9900));
    state.tickets.push(product);
    return json(route, product);
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
  if (/^\/api\/v1\/finance\/ar-ap-items\/[^/]+\/payments$/.test(pathname)) return json(route, { id: "payment-1", status: "partial", amount: bodyValue(body, "amount"), reason: bodyValue(body, "reason") });
  if (pathname === "/api/v1/bar/tables") return json(route, { id: "table-created", tableCode: "A02", tableName: "A02", capacity: 4, minSpendAmount: 10000, status: "active" });
  if (pathname === "/api/v1/bar/products") return json(route, { id: "drink-created", productName: "Mock Drink", category: "drink", priceAmount: 3000, status: "active" });
  if (pathname === "/api/v1/bar/orders") return json(route, { id: "order-created", orderNo: "ORD-CREATED", tableId: "table-created", status: "ordered", totalAmount: 3000, createdAt: now, items: [], bills: [] });
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
  if (/^\/api\/v1\/integration\/provider-adapters\/[^/]+\/test-mock$/.test(pathname)) return json(route, { id: "mock-test-1", status: "mock_passed", latencyMs: 32 });
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
