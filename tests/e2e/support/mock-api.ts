import type { Page, Route } from "@playwright/test";

type ApiData = Record<string, unknown> | Array<Record<string, unknown>> | null;

type MockOptions = {
  empty?: boolean;
  forbiddenPath?: string;
  errorPath?: string;
};

const now = "2026-06-15T12:00:00.000Z";

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

function createState(empty = false) {
  const tickets = empty ? [] : [ticketProduct("ticket-1", "Weekend Pass", 9900)];
  const providerAdapters = empty ? [] : [providerAdapter("adapter-1", "mock-video", "video")];
  const amusementSessions = empty ? [] : [{ id: "session-1", projectId: "amuse-1", sessionName: "Morning Session", startsAt: now, endsAt: now, capacity: 20, usedCapacity: 3, lockedCapacity: 2, status: "scheduled" }];
  return {
    tickets,
    providerAdapters,
    issuedEntitlements: empty ? [] : [{ id: "ent-1", entitlementCode: "ENT-0001", entitlementType: "entry", totalTimes: 1, usedTimes: 0, valueAmount: 9900, status: "active", createdAt: now }],
    verifications: empty ? [] : [{ id: "ver-1", verificationNo: "VER-0001", status: "success", source: "mock", createdAt: now }],
    dynamicPricingRules: empty ? [] : [dynamicRule("rule-1", "ticket-1", "Weekend Boost", "draft")],
    groupBookings: empty ? [] : [{ id: "gb-1", projectId: "amuse-1", sessionId: "session-1", bookingType: "group", customerName: "Team A", partySize: 6, reservedCapacity: 6, bookingStatus: "approved", evidenceAttachmentIds: [], createdAt: now, updatedAt: now }],
    amusementSessions,
  };
}

function ticketProduct(id: string, name: string, priceAmount: number) {
  return { id, productCode: id.toUpperCase(), productName: name, productType: "package", priceAmount, status: "active", createdAt: now };
}

function providerAdapter(id: string, providerCode: string, adapterType: string) {
  return { id, providerCode, adapterType, mode: "sdk_api", capabilities: [], environment: "test", status: "active", webhookEnabled: true, pollingCheckEnabled: true, credentialSecretRef: "cred://test/provider", productionReady: false, operatorNote: "mock", evidenceAttachmentIds: [], createdAt: now, updatedAt: now };
}

function dynamicRule(id: string, ticketTypeId: string, ruleName: string, status: string) {
  return { id, ticketTypeId, ruleName, priority: 10, effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", salesChannel: "web", adjustmentType: "percent_delta", adjustmentRate: 10, status, conditionRule: {}, evidenceAttachmentIds: [], createdAt: now, updatedAt: now };
}

function fixedRows(pathname: string, state: ReturnType<typeof createState>): ApiData {
  const empty: Record<string, unknown>[] = [];
  const routes: Record<string, ApiData> = {
    "/api/v1/system/health": { ok: true, service: "mock-api", time: now },
    "/api/v1/analytics/dashboards/p0-overview": { merchants: 1, ticketProducts: state.tickets.length, issuedTickets: state.issuedEntitlements.length, verifications: state.verifications.length, barOrders: 1, amusementSessions: state.amusementSessions.length, openRiskAlerts: 1, auditEvents: 2 },
    "/api/v1/integration/provider-adapters": state.providerAdapters,
    "/api/v1/platform/business-entities": [{ id: "org-1", name: "Platform Org", code: "ORG", orgType: "platform", status: "active", createdAt: now }],
    "/api/v1/platform/merchants": [{ id: "merchant-1", organizationId: "org-1", displayName: "Main Merchant", companyName: "Main Company", status: "active", riskLevel: "normal" }],
    "/api/v1/platform/projects": [{ id: "venue-1", merchantId: "merchant-1", name: "Main Venue", code: "VENUE", venueType: "complex", status: "active" }],
    "/api/v1/platform/spaces": [{ id: "area-1", venueId: "venue-1", areaId: "area-1", name: "Gate A", code: "GATE-A", areaType: "operation", pointType: "gate", status: "active" }],
    "/api/v1/access/users": [{ id: "user-1", username: "admin", accountType: "platform_admin", status: "active", organizationId: "org-1" }],
    "/api/v1/analytics/merchants": [{ merchantId: "merchant-1", merchantName: "Main Merchant", merchantType: "complex", revenueAmountCents: 880000, orderCount: 8, riskAlertCount: 1, settlementStatus: "pending", businessDate: "2026-06-15", createdAt: now, updatedAt: now }],
    "/api/v1/ticketing/ticket-types": state.tickets,
    "/api/v1/ticketing/dynamic-pricing-rules": state.dynamicPricingRules,
    "/api/v1/ticketing/entitlement-templates": [{ id: "tpl-1", name: "Entry", entitlementType: "entry", defaultTotalTimes: 1, defaultValueAmount: 9900, status: "active" }],
    "/api/v1/ticketing/issued-entitlements": state.issuedEntitlements,
    "/api/v1/ticketing/verifications": state.verifications,
    "/api/v1/finance/ar-ap-items": [{ id: "bill-1", billNo: "BILL-1", merchantId: "merchant-1", totalAmount: 9900, status: "pending", createdAt: now }],
    "/api/v1/bar/tables": [{ id: "table-1", tableCode: "A01", tableName: "A01", capacity: 4, minSpendAmount: 10000, status: "active" }],
    "/api/v1/bar/products": [{ id: "drink-1", productName: "Mock Drink", category: "drink", priceAmount: 3000, status: "active" }],
    "/api/v1/inventory/stock-movements": empty,
    "/api/v1/inventory/items": [{ id: "item-1", itemCode: "WINE", itemName: "Wine", category: "drink", unit: "bottle", status: "active", minQuantity: 2 }],
    "/api/v1/inventory/stock-balances": [{ id: "stock-1", itemId: "item-1", locationType: "bar", quantity: 10 }],
    "/api/v1/inventory/saved-bottles": empty,
    "/api/v1/bar/production-tickets": empty,
    "/api/v1/bar/reservations": empty,
    "/api/v1/bar/orders": [{ id: "order-1", orderNo: "ORD-1", tableId: "table-1", status: "paid", totalAmount: 6000, createdAt: now, items: [], bills: [] }],
    "/api/v1/amusement/projects": [{ id: "amuse-1", projectCode: "AMUSE", projectName: "Adventure", capacity: 20, status: "active", sessions: state.amusementSessions }],
    "/api/v1/amusement/sessions": state.amusementSessions,
    "/api/v1/amusement/verifications": empty,
    "/api/v1/amusement/reservations": empty,
    "/api/v1/amusement/queue-entries": empty,
    "/api/v1/amusement/device-statuses": empty,
    "/api/v1/amusement/safety-inspections": empty,
    "/api/v1/amusement/manual-overrides": empty,
    "/api/v1/amusement/group-bookings": state.groupBookings,
    "/api/v1/analytics/risk-rules": [{ id: "risk-rule-1", ruleCode: "RISK", ruleName: "Manual Risk", ruleType: "manual", threshold: 1, status: "active" }],
    "/api/v1/analytics/risk-alerts": [{ id: "risk-1", alertType: "manual", riskLevel: "high", summary: "Mock risk", status: "open", createdAt: now }],
    "/api/v1/evidence/events": [{ id: "event-1", eventType: "login", eventCategory: "audit", sourceType: "web", result: "success", riskLevel: "normal", summary: "mock", occurredAt: now }],
    "/api/v1/integration/order-syncs": empty,
    "/api/v1/integration/voucher-verifications": empty,
    "/api/v1/integration/retry-tasks": empty,
    "/api/v1/integration/compensation-tasks": empty,
    "/api/v1/finance/reconciliation/batches": empty,
    "/api/v1/finance/reconciliation/differences": empty,
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
    "/api/v1/analytics/robot": empty,
    "/api/v1/analytics/metric-definitions": empty,
    "/api/v1/analytics/ai-quality/tasks": empty,
    "/api/v1/analytics/ai-quality/results": empty,
    "/api/v1/analytics/customer-intents": empty,
    "/api/v1/analytics/staff-service-score": empty,
    "/api/v1/analytics/merchant-health": empty,
    "/api/v1/analytics/metric-anomalies": empty,
    "/api/v1/analytics/business-metric-snapshots": empty,
    "/api/v1/floor-plans": empty,
  };
  return routes[pathname] ?? empty;
}

export async function installMockApi(page: Page, options: MockOptions = {}) {
  const state = createState(Boolean(options.empty));
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (options.forbiddenPath && url.pathname.includes(options.forbiddenPath)) {
      return json(route, { code: "FORBIDDEN", message: "mock forbidden" }, 403);
    }
    if (options.errorPath && url.pathname.includes(options.errorPath)) {
      return json(route, { code: "SERVER_ERROR", message: "mock server error" }, 500);
    }

    if (request.method() === "POST") {
      return handlePost(route, url.pathname, state);
    }
    return json(route, fixedRows(url.pathname, state));
  });
}

async function handlePost(route: Route, pathname: string, state: ReturnType<typeof createState>) {
  if (pathname === "/api/v1/auth/login") {
    return json(route, { accessToken: "mock-token", refreshToken: "mock-refresh", sessionId: "session-1", tokenType: "Bearer", user: { id: "user-1", username: "admin", accountType: "platform_admin" } });
  }
  if (pathname === "/api/v1/ticketing/ticket-types") {
    const product = ticketProduct(`ticket-${state.tickets.length + 1}`, "P0 Web Pass", 9900);
    state.tickets.push(product);
    return json(route, product);
  }
  if (pathname === "/api/v1/ticketing/entitlement-templates") {
    return json(route, { id: "tpl-new", name: "P0 Web Entry Entitlement", entitlementType: "entry", defaultTotalTimes: 1, defaultValueAmount: 9900, status: "active" });
  }
  if (/^\/api\/v1\/ticketing\/ticket-types\/[^/]+\/entitlements$/.test(pathname)) {
    return json(route, { id: "link-1", status: "active" });
  }
  if (/^\/api\/v1\/ticketing\/ticket-types\/[^/]+\/issue$/.test(pathname)) {
    const entitlement = { id: `ent-${state.issuedEntitlements.length + 1}`, entitlementCode: `ENT-${state.issuedEntitlements.length + 1000}`, entitlementType: "entry", totalTimes: 1, usedTimes: 0, valueAmount: 9900, status: "active", createdAt: now };
    state.issuedEntitlements.push(entitlement);
    return json(route, { ticketCode: "TICKET-1000", issuedEntitlements: [entitlement] });
  }
  if (pathname === "/api/v1/ticketing/verifications/preview") {
    return json(route, { status: "preview_ok", entitlementCode: "ENT-0001" });
  }
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
    rule.status = "active";
    return json(route, rule);
  }
  if (pathname === "/api/v1/ticketing/dynamic-pricing/simulations") {
    return json(route, { ticketTypeId: state.tickets[0]?.id ?? "ticket-1", visitDate: "2026-06-20", salesChannel: "web", basePriceCent: 9900, adjustmentCent: 990, finalPriceCent: 10890, matchedRuleId: state.dynamicPricingRules[0]?.id, explanation: "mock dynamic pricing simulation" });
  }
  if (pathname === "/api/v1/integration/provider-adapters") {
    const adapter = providerAdapter(`adapter-${state.providerAdapters.length + 1}`, `provider-${state.providerAdapters.length + 1}`, "reservation_resource");
    state.providerAdapters.push(adapter);
    return json(route, adapter);
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
  if (pathname.startsWith("/api/open/")) {
    return json(route, { accepted: true, requestId: "mock-open-webhook" });
  }
  return json(route, { id: "mock-post", status: "success" });
}
