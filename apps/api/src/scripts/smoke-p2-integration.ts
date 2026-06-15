type LoginResponse = { accessToken: string };
type IdResponse = { id: string };

const baseUrl = process.env.P0_API_BASE_URL ?? "http://127.0.0.1:8080";
let token = "";

function unique(prefix: string) {
  return `${prefix}_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (token) headers.set("authorization", `Bearer ${token}`);
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
  const text = await response.text();
  if (!response.ok) throw new Error(`${path} ${response.status}: ${text || response.statusText}`);
  return (text ? JSON.parse(text) : null) as T;
}

function post<T>(path: string, body: unknown) {
  return request<T>(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });
}

function assertOk(value: unknown, messageText: string) {
  if (!value) throw new Error(messageText);
}

async function main() {
  const adminUsername = process.env.P0_ADMIN_USERNAME;
  const adminPassword = process.env.P0_ADMIN_PASSWORD;
  if (!adminUsername || !adminPassword) throw new Error("P0_ADMIN_USERNAME and P0_ADMIN_PASSWORD are required");

  const health = await request<{ ok: boolean }>("/api/health");
  const login = await post<LoginResponse>("/api/auth/login", { username: adminUsername, password: adminPassword });
  token = login.accessToken;

  const organization = await post<IdResponse>("/api/admin/organizations", {
    name: `P2 Org ${Date.now()}`,
    shortName: "P2",
    orgType: "platform",
    code: unique("p2_org"),
    status: "active",
  });
  const merchant = await post<IdResponse>("/api/admin/merchants", {
    organizationId: organization.id,
    merchantType: "self_operated",
    companyName: `P2 Merchant Co ${Date.now()}`,
    displayName: `P2 Merchant ${Date.now()}`,
    contactName: "P2 Owner",
    contactPhone: "18800002000",
    status: "active",
  });
  const venue = await post<IdResponse>("/api/admin/venues", {
    merchantId: merchant.id,
    venueType: "complex",
    name: `P2 Venue ${Date.now()}`,
    code: unique("p2_venue"),
    status: "active",
  });

  const sku = await post<{ sku: IdResponse }>("/api/integration/retail/skus/sync", {
    externalSkuId: unique("sku_ext"),
    merchantId: merchant.id,
    venueId: venue.id,
    skuCode: unique("sku"),
    skuName: "P2 Retail SKU",
    category: "souvenir",
    priceAmount: 3888,
  });
  assertOk(sku.sku.id, "retail sku sync failed");

  const robotTask = await post<{ id: string; status: string }>("/api/integration/robot/tasks", {
    externalTaskId: unique("robot_ext"),
    merchantId: merchant.id,
    venueId: venue.id,
    taskType: "delivery",
    pickupLocation: "Bar Counter",
    dropoffLocation: "Room A",
    assignedRobotCode: unique("robot"),
  });
  await post(`/api/integration/robot/tasks/callback`, {
    taskId: robotTask.id,
    eventType: "accepted",
    status: "accepted",
    externalEventId: unique("robot_evt"),
  });
  await post(`/api/integration/robot/tasks/callback`, {
    taskId: robotTask.id,
    eventType: "picked_up",
    status: "picked_up",
    externalEventId: unique("robot_evt"),
  });
  const signedTask = await post<{ id: string; status: string }>(`/api/integration/robot/tasks/${robotTask.id}/manual-sign`, {
    signedBy: "P2 Smoke",
    reason: "delivered at counter",
  });
  assertOk(signedTask.status === "signed", "robot manual sign failed");

  const abnormalRobot = await post<{ id: string }>("/api/integration/robot/tasks", {
    externalTaskId: unique("robot_bad"),
    merchantId: merchant.id,
    venueId: venue.id,
    pickupLocation: "Kitchen",
    dropoffLocation: "Table 9",
  });
  await post(`/api/integration/robot/tasks/callback`, {
    taskId: abnormalRobot.id,
    eventType: "exception",
    status: "exception",
    abnormalReason: "blocked route",
    externalEventId: unique("robot_evt"),
  });

  const camera = await post<{ id: string; cameraCode: string }>("/api/integration/video/cameras", {
    cameraCode: unique("cam"),
    cameraName: "P2 Gate Camera",
    merchantId: merchant.id,
    venueId: venue.id,
    provider: "p2-smoke",
    streamUrl: "rtsp://example.invalid/p2",
  });
  const evidence = await post<{ id: string; viewCount: number }>("/api/integration/video/evidences/bind", {
    cameraId: camera.id,
    eventType: "ticket.verify",
    objectType: "robot_task",
    objectId: robotTask.id,
    snapshotUrl: "https://example.invalid/snapshot.jpg",
    clipUrl: "https://example.invalid/clip.mp4",
  });
  const viewedEvidence = await post<{ viewCount: number }>(`/api/integration/video/evidences/${evidence.id}/view`, {});
  assertOk(viewedEvidence.viewCount >= 1, "video evidence view failed");

  const corpId = unique("corp");
  const staffUserId = unique("staff");
  await post("/api/integration/wecom/bindings", { corpId, staffUserId, merchantId: merchant.id });
  await post("/api/integration/wecom/conversations/sync", {
    conversationKey: unique("conv"),
    corpId,
    merchantId: merchant.id,
    staffUserId,
    customerName: "P2 Customer",
  });
  const message = await post<{ message: { id: string; riskLevel: string }; riskHit?: { id: string; status: string } }>("/api/integration/wecom/messages/sync", {
    conversationKey: unique("conv_msg"),
    corpId,
    merchantId: merchant.id,
    staffUserId,
    customerName: "P2 Risk Customer",
    msgId: unique("msg"),
    senderType: "customer",
    content: "我要投诉，刚才有人要求私下收款，手机号13800000000",
  });
  assertOk(message.message.riskLevel === "high" && message.riskHit?.id, "wecom risk hit failed");
  const reviewedRisk = await post<{ status: string }>(`/api/integration/wecom/risk-hits/${message.riskHit!.id}/review`, {
    reviewResult: "confirmed",
  });
  assertOk(reviewedRisk.status === "reviewed", "wecom risk review failed");

  const deviceCode = unique("dev");
  const device = await post<{ id: string; deviceCode: string }>("/api/integration/devices/register", {
    deviceCode,
    deviceType: "gate",
    merchantId: merchant.id,
    venueId: venue.id,
    status: "active",
  });
  await post("/api/integration/devices/heartbeat", {
    deviceId: device.id,
    deviceCode,
    eventSeq: unique("heartbeat"),
    status: "online",
  });
  const forcedOpen = await post<{ event: { eventType: string; status: string } }>("/api/integration/devices/events", {
    deviceId: device.id,
    deviceCode,
    eventType: "forced_open",
    eventSeq: unique("evt"),
    status: "abnormal",
  });
  assertOk(forcedOpen.event.eventType === "forced_open", "device event failed");
  const offlineBatch = await post<{ batch: { status: string; rejectedCount: number } }>("/api/integration/devices/offline-sync", {
    deviceId: device.id,
    deviceCode,
    events: [
      { deviceId: device.id, deviceCode, eventType: "scan", eventSeq: unique("offline"), status: "received" },
      { deviceId: device.id, deviceCode, eventType: "offline_conflict", eventSeq: unique("offline"), status: "conflict" },
    ],
  });
  assertOk(offlineBatch.batch.status === "partial" && offlineBatch.batch.rejectedCount === 1, "device offline sync failed");

  const [robotRows, evidences, riskHits, deviceEvents] = await Promise.all([
    request<unknown[]>("/api/integration/robot/tasks"),
    request<unknown[]>("/api/integration/video/evidences"),
    request<unknown[]>("/api/integration/wecom/risk-hits"),
    request<unknown[]>("/api/integration/devices/events"),
  ]);

  console.log(JSON.stringify({
    health: health.ok,
    login: Boolean(token),
    foundation: { organization: organization.id, merchant: merchant.id, venue: venue.id },
    retail: { sku: sku.sku.id },
    robot: { signed: signedTask.status, rows: robotRows.length },
    video: { camera: camera.cameraCode, evidenceViews: viewedEvidence.viewCount, rows: evidences.length },
    wecom: { messageRisk: message.message.riskLevel, reviewed: reviewedRisk.status, rows: riskHits.length },
    device: { deviceCode, offlineBatch: offlineBatch.batch.status, rows: deviceEvents.length },
  }, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
