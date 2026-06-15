type JsonObject = Record<string, unknown>;

type IdResponse = { id: string };
type LoginResponse = { accessToken: string };
type SessionResponse = { id: string; usedCapacity: number; capacity: number };
type ReservationResponse = { reservation: { id: string; status: string }; queue: { id: string; status: string; queueNo: string } };
type QueueEntryResponse = { id: string; queueNo: string; status: string; partySize: number };
type DeviceStatusResponse = { id: string; deviceCode: string; status: string };
type InspectionTemplateResponse = { id: string; templateName: string };
type InspectionRecordResponse = { id: string; result: string };
type FaultResponse = { id: string; status: string; summary: string };

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
    name: `P1 Amusement Org ${stamp}`,
    shortName: 'P1 Amu',
    code: `p1-amu-org-${stamp}`,
  });
  const merchant = await post<IdResponse>('/api/admin/merchants', {
    organizationId: organization.id,
    merchantType: 'amusement',
    companyName: `P1 Amusement Merchant Co ${stamp}`,
    displayName: `P1 Amusement Merchant ${stamp}`,
    contactName: 'P1 Amusement',
    contactPhone: '18800000201',
    status: 'active',
  });
  const venue = await post<IdResponse>('/api/admin/venues', {
    merchantId: merchant.id,
    venueType: 'amusement',
    name: `P1 Amusement Venue ${stamp}`,
    code: `p1-amu-venue-${stamp}`,
    floor: '2F',
    capacity: 80,
    status: 'active',
  });
  const project = await post<IdResponse>('/api/admin/amusement/projects', {
    merchantId: merchant.id,
    venueId: venue.id,
    projectName: `P1 Wheel ${stamp}`,
    capacity: 20,
  });
  const session = await post<SessionResponse>(`/api/admin/amusement/projects/${project.id}/sessions`, {
    sessionName: `P1 Session ${stamp}`,
    startsAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    endsAt: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
    capacity: 5,
  });

  const reservation = await post<ReservationResponse>(`/api/admin/amusement/sessions/${session.id}/reservations`, {
    customerName: 'P1 Reservation Guest',
    customerPhone: '18800000202',
    partySize: 2,
    reservedCapacity: 2,
  });
  assertOk(reservation.reservation.status === 'queued' && reservation.queue.status === 'waiting', 'reservation queue failed');
  const called = await post<QueueEntryResponse>(`/api/admin/amusement/queue-entries/${reservation.queue.id}/call`, {});
  const admitted = await post<QueueEntryResponse>(`/api/admin/amusement/queue-entries/${reservation.queue.id}/admit`, {});
  assertOk(called.status === 'called' && admitted.status === 'admitted', 'queue admit failed');

  const walkIn = await post<QueueEntryResponse>(`/api/admin/amusement/sessions/${session.id}/queue-entries`, {
    customerName: 'P1 Walkin Guest',
    partySize: 1,
  });
  const passed = await post<QueueEntryResponse>(`/api/admin/amusement/queue-entries/${walkIn.id}/pass`, {});
  assertOk(passed.status === 'passed', 'queue pass failed');

  const device = await post<DeviceStatusResponse>('/api/admin/amusement/device-statuses', {
    projectId: project.id,
    merchantId: merchant.id,
    venueId: venue.id,
    deviceName: `P1 Main Device ${stamp}`,
    status: 'normal',
  });
  const maintenance = await post<DeviceStatusResponse>(`/api/admin/amusement/device-statuses/${device.id}/change`, {
    status: 'maintenance',
    statusReason: 'P1 smoke maintenance',
  });
  assertOk(maintenance.status === 'maintenance', 'device status change failed');

  const template = await post<InspectionTemplateResponse>('/api/admin/amusement/inspection-templates', {
    projectId: project.id,
    templateName: `P1 Daily Check ${stamp}`,
    checklist: ['seat belt', 'emergency stop'],
  });
  const inspection = await post<InspectionRecordResponse>('/api/admin/amusement/inspection-records', {
    templateId: template.id,
    projectId: project.id,
    sessionId: session.id,
    result: 'abnormal',
    abnormalItems: ['emergency stop'],
    remark: 'P1 smoke abnormal inspection',
  });
  assertOk(inspection.result === 'abnormal', 'inspection submit failed');

  const faults = await request<FaultResponse[]>(`/api/admin/amusement/faults?projectId=${project.id}`);
  const openFault = faults.find((fault) => fault.status === 'open');
  assertOk(openFault, 'fault created by abnormal inspection missing');
  const resolved = await post<FaultResponse>(`/api/admin/amusement/faults/${openFault!.id}/resolve`, {
    resolution: 'P1 smoke resolved',
  });
  assertOk(resolved.status === 'resolved', 'fault resolve failed');

  const queues = await request<QueueEntryResponse[]>(`/api/admin/amusement/queue-entries?sessionId=${session.id}`);
  const devices = await request<DeviceStatusResponse[]>(`/api/admin/amusement/device-statuses?projectId=${project.id}`);

  console.log(
    JSON.stringify(
      {
        health: health.ok,
        login: Boolean(login.accessToken),
        foundation: { organization: organization.id, merchant: merchant.id, venue: venue.id },
        session: { id: session.id, capacity: session.capacity },
        queue: { reservation: admitted.status, walkIn: passed.status, rows: queues.length },
        device: { code: device.deviceCode, status: maintenance.status, rows: devices.length },
        inspection: { template: template.templateName, result: inspection.result, fault: resolved.status },
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
