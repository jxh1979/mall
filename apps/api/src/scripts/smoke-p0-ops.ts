type JsonObject = Record<string, unknown>;

type IdResponse = { id: string };
type TicketProductResponse = { id: string };
type EntitlementTemplateResponse = { id: string };
type IssuedTicketResponse = { issuedEntitlements: Array<{ entitlementCode: string }> };
type PreviewResponse = { allowed: boolean };
type VerificationResponse = { status: string };
type BarBillResponse = { id: string; totalAmount: number; status: string };
type BarOrderItemResponse = { id: string; quantity: number; lineAmount: number; status: string };
type SettlementBillResponse = { id: string; totalAmount: number; status: string };
type AmusementSessionResponse = { id: string };
type RiskAlertResponse = { id: string; status: string; alertType?: string; objectId?: string; summary?: string };
type OpenAppResponse = {
  id: string;
  appCode: string;
  initialSecret?: string;
  credentials?: Array<{ loginAccount: string; status: string; passwordHash?: string; secretEncrypted?: string }>;
};

const baseUrl = process.env.P0_API_BASE_URL ?? 'http://127.0.0.1:8080';
let accessToken = '';

function randomPassword() {
  return `P0-${Date.now()}-${Math.random().toString(16).slice(2)}-Aa1!`;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (accessToken) headers.set('authorization', `Bearer ${accessToken}`);
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(`${path} ${response.status}: ${text}`);
  return body as T;
}

async function post<T>(path: string, body: JsonObject): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
}

async function patch<T>(path: string, body: JsonObject): Promise<T> {
  return request<T>(path, {
    method: 'PATCH',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
}

async function postFailure(path: string, body: JsonObject) {
  const headers = new Headers({ 'content-type': 'application/json' });
  if (accessToken) headers.set('authorization', `Bearer ${accessToken}`);
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}

function assertOk(condition: unknown, message: string) {
  if (!condition) throw new Error(message);
}

async function main() {
  const stamp = Date.now();
  const adminUsername = process.env.P0_ADMIN_USERNAME;
  const adminPassword = process.env.P0_ADMIN_PASSWORD;
  if (!adminUsername || !adminPassword) {
    throw new Error('P0_ADMIN_USERNAME and P0_ADMIN_PASSWORD are required');
  }
  const testPassword = process.env.P0_TEST_PASSWORD ?? randomPassword();
  const health = await request<{ ok: boolean }>('/api/health');
  assertOk(health.ok, 'health check failed');

  const login = await post<{ accessToken: string }>('/api/auth/login', {
    username: adminUsername,
    password: adminPassword,
  });
  assertOk(login.accessToken, 'admin login failed');
  accessToken = login.accessToken;

  const organization = await post<IdResponse>('/api/admin/organizations', {
    orgType: 'platform',
    name: `P0 Ops Org ${stamp}`,
    shortName: 'P0 Ops',
    code: `p0-ops-org-${stamp}`,
  });
  const merchant = await post<IdResponse>('/api/admin/merchants', {
    organizationId: organization.id,
    merchantType: 'platform',
    companyName: `P0 Ops Merchant Co ${stamp}`,
    displayName: `P0 Ops Merchant ${stamp}`,
    contactName: 'P0 Ops',
    contactPhone: '18800000009',
    status: 'active',
  });
  const venue = await post<IdResponse>('/api/admin/venues', {
    merchantId: merchant.id,
    venueType: 'complex',
    name: `P0 Ops Venue ${stamp}`,
    code: `p0-ops-venue-${stamp}`,
    floor: '1F',
    capacity: 100,
    status: 'active',
  });
  const area = await post<IdResponse>('/api/admin/areas', {
    venueId: venue.id,
    name: `P0 Ops Area ${stamp}`,
    areaType: 'entrance',
    floor: '1F',
    capacity: 50,
  });
  const point = await post<IdResponse>('/api/admin/location-points', {
    areaId: area.id,
    venueId: venue.id,
    merchantId: merchant.id,
    pointType: 'gate',
    name: `P0 Ops Point ${stamp}`,
    code: `p0-ops-point-${stamp}`,
    capacity: 30,
  });
  const user = await post<{ username: string }>('/api/admin/users', {
    username: `p0_ops_user_${stamp}`,
    password: testPassword,
    accountType: 'staff',
    organizationId: organization.id,
    merchantId: merchant.id,
    status: 'active',
  });
  assertOk(user.username, 'user creation failed');

  const product = await post<TicketProductResponse>('/api/admin/ticket-products', {
    productName: `P0 Ops Pass ${stamp}`,
    productType: 'package',
    priceAmount: 8800,
    status: 'active',
  });
  const template = await post<EntitlementTemplateResponse>('/api/admin/entitlement-templates', {
    name: `P0 Ops Entry ${stamp}`,
    entitlementType: 'entry',
    merchantId: merchant.id,
    venueId: venue.id,
    defaultTotalTimes: 1,
    defaultValueAmount: 8800,
  });
  await post(`/api/admin/ticket-products/${product.id}/entitlements`, {
    entitlementTemplateId: template.id,
    quantity: 1,
    totalTimes: 1,
    valueAmount: 8800,
  });
  const issued = await post<IssuedTicketResponse>(`/api/admin/ticket-products/${product.id}/issue`, {
    purchaserName: 'P0 Ops Visitor',
  });
  const entitlementCode = issued.issuedEntitlements[0]?.entitlementCode;
  assertOk(entitlementCode, 'missing issued entitlement');
  const preview = await post<PreviewResponse>('/api/verifications/preview', { entitlementCode });
  const verification = await post<VerificationResponse>('/api/verifications', { entitlementCode, source: 'p0-ops-smoke' });
  assertOk(preview.allowed && verification.status === 'success', 'ticket verification failed');
  const failedVerification = await postFailure('/api/verifications', { entitlementCode, source: 'p0-ops-smoke-repeat' });
  assertOk(failedVerification.status === 400, 'repeat verification should fail');
  const alertsAfterRepeat = await request<RiskAlertResponse[]>('/api/risk/alerts');
  const repeatAlert = alertsAfterRepeat.find(
    (alert) => alert.status === 'open' && alert.objectId === entitlementCode && alert.alertType === 'verification_failed',
  );
  assertOk(repeatAlert, 'repeat verification risk alert missing');
  const resolvedRepeatAlert = await post<RiskAlertResponse>(`/api/risk/alerts/${repeatAlert!.id}/resolve`, {
    reason: 'P0 ops smoke repeat verification handled',
  });
  assertOk(resolvedRepeatAlert.status === 'resolved', 'repeat verification risk alert resolve failed');
  const settlementBill = await post<SettlementBillResponse>('/api/merchant/settlement-bills/generate', {
    merchantId: merchant.id,
  });
  assertOk(settlementBill.status === 'generated' && settlementBill.totalAmount > 0, 'settlement bill generation failed');

  const table = await post<IdResponse>('/api/admin/bar/tables', {
    merchantId: merchant.id,
    venueId: venue.id,
    tableName: `P0 Ops Table ${stamp}`,
    capacity: 4,
    minSpendAmount: 10000,
  });
  const drink = await post<IdResponse>('/api/admin/bar/products', {
    merchantId: merchant.id,
    venueId: venue.id,
    productName: `P0 Ops Drink ${stamp}`,
    category: 'drink',
    priceAmount: 3500,
  });
  const order = await post<IdResponse>('/api/admin/bar/orders', {
    tableId: table.id,
    merchantId: merchant.id,
    venueId: venue.id,
  });
  await post('/api/admin/bar/inventory-flows', {
    productId: drink.id,
    merchantId: merchant.id,
    venueId: venue.id,
    flowType: 'inbound',
    quantity: 20,
    reason: 'P0 ops smoke inbound',
  });
  const barItem = await post<BarOrderItemResponse>(`/api/admin/bar/orders/${order.id}/items`, { productId: drink.id, quantity: 2 });
  const changedItem = await patch<BarOrderItemResponse>(`/api/admin/bar/order-items/${barItem.id}`, {
    quantity: 3,
    reason: 'P0 ops smoke quantity change',
  });
  assertOk(changedItem.quantity === 3, 'bar order item quantity update failed');
  const bill = await post<BarBillResponse>(`/api/admin/bar/orders/${order.id}/bill`, {});
  const paid = await post<BarBillResponse>(`/api/admin/bar/bills/${bill.id}/pay`, {
    paidAmount: bill.totalAmount,
    paymentMethod: 'offline',
  });
  assertOk(paid.status === 'paid', 'bar payment failed');

  const project = await post<IdResponse>('/api/admin/amusement/projects', {
    merchantId: merchant.id,
    venueId: venue.id,
    projectName: `P0 Ops Project ${stamp}`,
    capacity: 10,
  });
  await post(`/api/admin/amusement/projects/${project.id}/rules`, {
    ruleType: 'age',
    ruleValue: 'manual_check',
  });
  const session = await post<AmusementSessionResponse>(`/api/admin/amusement/projects/${project.id}/sessions`, {
    sessionName: `P0 Ops Session ${stamp}`,
    startsAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    endsAt: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
    capacity: 10,
  });
  const amusement = await post<{ status: string }>(`/api/admin/amusement/sessions/${session.id}/verify`, {
    manualReason: 'P0 ops smoke manual allow',
  });
  assertOk(amusement.status === 'manual_allowed', 'amusement manual allow failed');

  const riskAlert = await post<RiskAlertResponse>('/api/risk/alerts', {
    alertType: 'manual',
    riskLevel: 'suspicious',
    objectType: 'ticket',
    objectId: product.id,
    summary: `P0 Ops Risk ${stamp}`,
  });
  const resolved = await post<RiskAlertResponse>(`/api/risk/alerts/${riskAlert.id}/resolve`, {
    reason: 'P0 ops smoke resolved',
  });
  assertOk(resolved.status === 'resolved', 'risk resolve failed');

  const webhook = await post<{ reserved: boolean }>('/api/integration/webhooks/device/p0.ops', {
    requestId: `ops-${stamp}`,
    payload: { reserved: true },
  });
  assertOk(webhook.reserved, 'reserved webhook failed');

  const openApp = await post<OpenAppResponse>('/api/open-apps', {
    appName: `P0 Ops App ${stamp}`,
    appType: 'reserved_partner',
    loginAccount: `p0_ops_app_${stamp}`,
    password: testPassword,
    contactName: 'P0 Ops Partner',
    contactPhone: '18800000010',
  });
  assertOk(openApp.initialSecret && openApp.credentials?.[0]?.loginAccount, 'open app creation failed');
  assertOk(!openApp.credentials?.[0]?.passwordHash && !openApp.credentials?.[0]?.secretEncrypted, 'open app leaked sensitive fields');
  const openToken = await post<{ accessToken: string; dynamicEndpoint: string }>('/api/open-auth/token', {
    loginAccount: `p0_ops_app_${stamp}`,
    password: testPassword,
  });
  assertOk(openToken.accessToken && openToken.dynamicEndpoint, 'open app auth failed');

  const events = await request<unknown[]>('/api/events?limit=20');
  const overview = await request<Record<string, number>>('/api/analytics/overview');

  console.log(
    JSON.stringify(
      {
        health: health.ok,
        login: Boolean(login.accessToken),
        foundation: { organization: organization.id, merchant: merchant.id, venue: venue.id, area: area.id, point: point.id },
        ticket: {
          previewAllowed: preview.allowed,
          verificationStatus: verification.status,
          repeatFailed: failedVerification.status,
          repeatRiskResolved: resolvedRepeatAlert.status,
        },
        settlement: { status: settlementBill.status, totalAmount: settlementBill.totalAmount },
        bar: { billStatus: paid.status, changedQuantity: changedItem.quantity },
        amusement: { status: amusement.status },
        risk: { status: resolved.status },
        integration: { reserved: webhook.reserved },
        openApp: { appCode: openApp.appCode, dynamicEndpoint: openToken.dynamicEndpoint },
        events: events.length,
        overview,
      },
      null,
      2,
    ),
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
