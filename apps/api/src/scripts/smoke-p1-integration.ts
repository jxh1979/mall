type JsonObject = Record<string, unknown>;

type IdResponse = { id: string };
type LoginResponse = { accessToken: string };
type SyncResponse<T> = { accepted: boolean; businessProcessed: boolean; record: T };
type OrderSyncRecord = { id: string; businessType: string; syncType: string; externalBusinessId: string; amount: number; status: string };
type VoucherRecord = { id: string; businessType: string; externalVerificationId: string; status: string };
type RetryTask = { id: string; status: string; attemptCount: number };
type CompensationTask = { id: string; status: string };
type ReconciliationBatch = { id: string; status: string; diffCount: number };
type ReconciliationDifference = { id: string; status: string; diffType: string };

const baseUrl = process.env.P1_API_BASE_URL ?? process.env.P0_API_BASE_URL ?? 'http://127.0.0.1:8080';
let accessToken = '';

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

function assertOk(condition: unknown, message: string) {
  if (!condition) throw new Error(message);
}

async function main() {
  const stamp = Date.now();
  const adminUsername = process.env.P0_ADMIN_USERNAME;
  const adminPassword = process.env.P0_ADMIN_PASSWORD;
  if (!adminUsername || !adminPassword) throw new Error('P0_ADMIN_USERNAME and P0_ADMIN_PASSWORD are required');

  const health = await request<{ ok: boolean }>('/api/health');
  assertOk(health.ok, 'health check failed');
  const login = await post<LoginResponse>('/api/auth/login', { username: adminUsername, password: adminPassword });
  accessToken = login.accessToken;

  const organization = await post<IdResponse>('/api/admin/organizations', {
    orgType: 'platform',
    name: `P1 Integration Org ${stamp}`,
    shortName: 'P1 Int',
    code: `p1-int-org-${stamp}`,
  });
  const merchant = await post<IdResponse>('/api/admin/merchants', {
    organizationId: organization.id,
    merchantType: 'platform',
    companyName: `P1 Integration Merchant Co ${stamp}`,
    displayName: `P1 Integration Merchant ${stamp}`,
    contactName: 'P1 Integration',
    contactPhone: '18800000301',
    status: 'active',
  });
  const venue = await post<IdResponse>('/api/admin/venues', {
    merchantId: merchant.id,
    venueType: 'complex',
    name: `P1 Integration Venue ${stamp}`,
    code: `p1-int-venue-${stamp}`,
    floor: '1F',
    capacity: 200,
    status: 'active',
  });

  const cateringOrder = await post<SyncResponse<OrderSyncRecord>>('/api/integration/catering/orders/sync', {
    merchantId: merchant.id,
    venueId: venue.id,
    externalOrderId: `cat-order-${stamp}`,
    amount: 12800,
    status: 'paid',
    payload: { table: 'C01' },
    createRetryTask: true,
  });
  assertOk(cateringOrder.accepted && cateringOrder.record.syncType === 'order', 'catering order sync failed');

  const cateringRefund = await post<SyncResponse<OrderSyncRecord>>('/api/integration/catering/refunds/sync', {
    merchantId: merchant.id,
    venueId: venue.id,
    externalRefundId: `cat-refund-${stamp}`,
    amount: 2800,
    status: 'refunded',
    payload: { reason: 'customer refund' },
    createCompensationTask: true,
  });
  assertOk(cateringRefund.record.syncType === 'refund', 'catering refund sync failed');

  const cateringVoucher = await post<SyncResponse<VoucherRecord>>('/api/integration/catering/vouchers/verify', {
    merchantId: merchant.id,
    venueId: venue.id,
    externalVerificationId: `cat-voucher-${stamp}`,
    entitlementCode: `demo-ent-${stamp}`,
    amount: 5000,
    status: 'verified',
  });
  assertOk(cateringVoucher.record.status === 'verified', 'catering voucher sync failed');

  const retailOrder = await post<SyncResponse<OrderSyncRecord>>('/api/integration/retail/orders/sync', {
    merchantId: merchant.id,
    venueId: venue.id,
    externalOrderId: `ret-order-${stamp}`,
    amount: 9900,
    status: 'paid',
  });
  const retailRefund = await post<SyncResponse<OrderSyncRecord>>('/api/integration/retail/refunds/sync', {
    merchantId: merchant.id,
    venueId: venue.id,
    externalRefundId: `ret-refund-${stamp}`,
    amount: 1900,
    status: 'refunded',
  });
  const retailVoucher = await post<SyncResponse<VoucherRecord>>('/api/integration/retail/vouchers/verify', {
    merchantId: merchant.id,
    venueId: venue.id,
    externalVerificationId: `ret-voucher-${stamp}`,
    amount: 3000,
    status: 'verified',
  });
  assertOk(retailOrder.record.businessType === 'retail' && retailRefund.record.syncType === 'refund' && retailVoucher.record.status === 'verified', 'retail sync failed');

  const retryTasks = await request<RetryTask[]>('/api/integration/retry-tasks');
  const retry = retryTasks.find((task) => task.status === 'pending');
  assertOk(retry, 'retry task missing');
  const retried = await post<RetryTask>(`/api/integration/retry-tasks/${retry!.id}/run-now`, {});
  assertOk(retried.status === 'succeeded', 'retry run failed');

  const compensationTasks = await request<CompensationTask[]>('/api/integration/compensation-tasks');
  const compensation = compensationTasks.find((task) => task.status === 'open');
  assertOk(compensation, 'compensation task missing');
  const compensated = await post<CompensationTask>(`/api/integration/compensation-tasks/${compensation!.id}/resolve`, {
    resolution: 'P1 smoke compensated',
  });
  assertOk(compensated.status === 'resolved', 'compensation resolve failed');

  const batch = await post<ReconciliationBatch>('/api/integration/reconciliation/batches', {
    businessType: 'catering',
    reconDate: new Date().toISOString().slice(0, 10),
    thirdPartyCount: 99,
    thirdPartyAmount: 999999,
  });
  assertOk(batch.status === 'diff' && batch.diffCount > 0, 'reconciliation diff batch failed');
  const diffs = await request<ReconciliationDifference[]>(`/api/integration/reconciliation/differences?batchId=${batch.id}`);
  assertOk(diffs.length > 0, 'reconciliation differences missing');
  const firstDiff = diffs[0];
  if (!firstDiff) throw new Error('reconciliation differences missing');
  const resolvedDiff = await post<ReconciliationDifference>(`/api/integration/reconciliation/differences/${firstDiff.id}/resolve`, {
    resolution: 'P1 smoke accepted platform value',
  });
  assertOk(resolvedDiff.status === 'resolved', 'reconciliation difference resolve failed');

  const orderRows = await request<OrderSyncRecord[]>('/api/integration/order-syncs');
  const voucherRows = await request<VoucherRecord[]>('/api/integration/voucher-verifications');

  console.log(
    JSON.stringify(
      {
        health: health.ok,
        login: Boolean(login.accessToken),
        foundation: { organization: organization.id, merchant: merchant.id, venue: venue.id },
        catering: { order: cateringOrder.record.status, refund: cateringRefund.record.status, voucher: cateringVoucher.record.status },
        retail: { order: retailOrder.record.status, refund: retailRefund.record.status, voucher: retailVoucher.record.status },
        tasks: { retry: retried.status, compensation: compensated.status },
        reconciliation: { batch: batch.status, diffCount: batch.diffCount, resolvedDiff: resolvedDiff.status },
        rows: { orders: orderRows.length, vouchers: voucherRows.length },
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
