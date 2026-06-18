import type { JsonBody, JsonValue } from "./types";

function getApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");
  if (window.location.port === "3000") return `${window.location.protocol}//${window.location.hostname}:8080`;
  return "";
}

export const apiBaseUrl = getApiBaseUrl();
let apiAccessToken = "";
const openApiPrefix = ["/api", "open"].join("/");
const formalApiPrefix = ["/api", "v1"].join("/");
const authApiPrefix = [formalApiPrefix, "auth"].join("/");
export const t3aReadOnlyApiPaths = {
  robot: "/api/v1/analytics/robot",
  metricDefinitions: "/api/v1/analytics/metric-definitions",
  aiQualityTasks: "/api/v1/analytics/ai-quality/tasks",
  aiQualityResults: "/api/v1/analytics/ai-quality/results",
  customerIntents: "/api/v1/analytics/customer-intents",
  staffServiceScores: "/api/v1/analytics/staff-service-score",
  merchantHealth: "/api/v1/analytics/merchant-health",
  metricAnomalies: "/api/v1/analytics/metric-anomalies",
  businessMetricSnapshots: "/api/v1/analytics/business-metric-snapshots",
} as const;
export const t3aReadOnlyAnalyticsMarker = "T3A_READONLY_ANALYTICS_READY";
export const t3aReadOnlyBoundaryText = "只读分析，不发控制指令，不改变票价、核销、支付、库存、合同或结算状态。";

export function setApiAccessToken(token: string) {
  apiAccessToken = token;
}

export function hasApiAccessToken() {
  return Boolean(apiAccessToken);
}

function isPlainJsonObject(value: unknown): value is Record<string, JsonValue> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function toSnakeCaseKey(key: string) {
  return key.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase();
}

function toCamelCaseKey(key: string) {
  return key.replace(/_([a-z0-9])/g, (_, letter: string) => letter.toUpperCase());
}

function mapJsonKeys(value: unknown, convertKey: (key: string) => string, preserveNestedKeys = new Set<string>()): JsonValue {
  if (Array.isArray(value)) {
    return value.map((item) => mapJsonKeys(item, convertKey, preserveNestedKeys));
  }

  if (isPlainJsonObject(value)) {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([, item]) => item !== undefined)
        .map(([key, item]) => [
          convertKey(key),
          preserveNestedKeys.has(key) ? (item as JsonValue) : mapJsonKeys(item, convertKey, preserveNestedKeys),
        ]),
    );
  }

  if (value === undefined) return null;
  return value as JsonValue;
}

function normalizeOutgoingBody(path: string, body: JsonBody) {
  if (path.startsWith(`${openApiPrefix}/`)) return body;
  return mapJsonKeys(body, toSnakeCaseKey, new Set(["payload"]));
}

function shouldAttachIdempotencyKey(path: string) {
  return path.startsWith(`${formalApiPrefix}/`) && !path.startsWith(`${authApiPrefix}/`);
}

function nextIdempotencyKey() {
  return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizePostBody(path: string, body: JsonBody) {
  const normalized = normalizeOutgoingBody(path, body);
  if (
    !shouldAttachIdempotencyKey(path) ||
    !isPlainJsonObject(normalized) ||
    "idempotency_key" in normalized ||
    "idempotencyKey" in normalized
  ) {
    return normalized;
  }
  return { ...normalized, idempotency_key: nextIdempotencyKey() };
}

function normalizeIncomingBody(value: unknown) {
  return mapJsonKeys(value, toCamelCaseKey);
}

function unwrapApiPayload(value: unknown) {
  const normalized = normalizeIncomingBody(value);
  if (isPlainJsonObject(normalized) && "code" in normalized && "data" in normalized) {
    const data = normalized.data;
    if (isPlainJsonObject(data) && Array.isArray(data.items)) return data.items;
    return data;
  }
  return normalized;
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (apiAccessToken) headers.set("authorization", `Bearer ${apiAccessToken}`);
  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  const text = await response.text();
  if (!response.ok) throw new Error(`${path} ${response.status}: ${text || response.statusText}`);
  return (text ? unwrapApiPayload(JSON.parse(text)) : null) as T;
}

export function getJson<T>(path: string) {
  return requestJson<T>(path);
}

export function postJson<T>(path: string, body: JsonBody) {
  return requestJson<T>(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(normalizePostBody(path, body)),
  });
}
