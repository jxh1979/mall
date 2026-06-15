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
    name: `P3 Org ${Date.now()}`,
    shortName: "P3",
    orgType: "platform",
    code: unique("p3_org"),
    status: "active",
  });
  const merchant = await post<IdResponse>("/api/admin/merchants", {
    organizationId: organization.id,
    merchantType: "self_operated",
    companyName: `P3 Merchant Co ${Date.now()}`,
    displayName: `P3 Merchant ${Date.now()}`,
    contactName: "P3 Owner",
    contactPhone: "18800003000",
    status: "active",
  });

  const metric = await post<{ id: string; metricCode: string }>("/api/analytics/metric-definitions", {
    metricCode: unique("metric"),
    metricName: "P3 GMV",
    category: "finance",
    unit: "cent",
    description: "P3 smoke metric",
  });
  assertOk(metric.id, "metric definition failed");

  const corpId = unique("corp");
  const staffUserId = unique("staff");
  const message = await post<{ message: { id: string; riskLevel: string }; riskHit?: { id: string } }>("/api/integration/wecom/messages/sync", {
    conversationKey: unique("conv"),
    corpId,
    merchantId: merchant.id,
    staffUserId,
    customerName: "P3 Customer",
    msgId: unique("msg"),
    senderType: "customer",
    content: "生日团建想包场，但是之前退款处理太慢我要投诉",
  });
  assertOk(message.message.id && message.riskHit?.id, "seed wecom risk failed");

  const aiTask = await post<{ task: { id: string; status: string }; result: { id: string; riskLevel: string; score: number } }>("/api/ai/quality/tasks", {
    taskType: "wecom_quality",
    objectType: "wecom_message",
    objectId: message.message.id,
    processNow: true,
  });
  assertOk(aiTask.task.status === "completed" && aiTask.result.riskLevel !== "normal", "ai quality task failed");

  const staffScores = await post<{ rows: Array<{ staffUserId: string; score: number }> }>("/api/ai/staff-service-score/recompute", {});
  assertOk(staffScores.rows.some((row) => row.staffUserId === staffUserId), "staff service score failed");

  const healthScores = await post<{ rows: Array<{ merchantId?: string; score: number }> }>("/api/analytics/merchant-health/recompute", {});
  assertOk(healthScores.rows.some((row) => row.merchantId === merchant.id), "merchant health recompute failed");

  const anomaly = await post<{ severity: string; explanation: string; changeRate: number }>("/api/analytics/metric-anomalies/explain", {
    metricCode: metric.metricCode,
    metricName: "P3 GMV",
    baselineValue: 10000,
    currentValue: 3800,
  });
  assertOk(anomaly.severity === "high" && anomaly.explanation, "metric anomaly explanation failed");

  const robotTask = await post<{ id: string }>("/api/integration/robot/tasks", {
    externalTaskId: unique("p3_robot"),
    merchantId: merchant.id,
    pickupLocation: "Warehouse",
    dropoffLocation: "Counter",
  });
  await post(`/api/integration/robot/tasks/${robotTask.id}/manual-sign`, { signedBy: "P3 Smoke" });
  const robotAnalytics = await request<{ total: number; completionRate: number }>("/api/analytics/robot");
  assertOk(robotAnalytics.total > 0 && robotAnalytics.completionRate >= 0, "robot analytics failed");

  const [qualityResults, intents, scoreRows, healthRows, anomalyRows] = await Promise.all([
    request<unknown[]>("/api/ai/quality/results"),
    request<unknown[]>("/api/ai/customer-intents"),
    request<unknown[]>("/api/ai/staff-service-score"),
    request<unknown[]>("/api/analytics/merchant-health"),
    request<unknown[]>("/api/analytics/metric-anomalies"),
  ]);

  console.log(JSON.stringify({
    health: health.ok,
    login: Boolean(token),
    metric: metric.metricCode,
    ai: { task: aiTask.task.status, riskLevel: aiTask.result.riskLevel, score: aiTask.result.score, qualityRows: qualityResults.length },
    intents: intents.length,
    staffScores: scoreRows.length,
    merchantHealth: healthRows.length,
    anomaly: { severity: anomaly.severity, changeRate: anomaly.changeRate, rows: anomalyRows.length },
    robotAnalytics,
  }, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
