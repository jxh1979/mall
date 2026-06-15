import {
  ApiOutlined,
  AuditOutlined,
  BankOutlined,
  BarChartOutlined,
  CoffeeOutlined,
  DashboardOutlined,
  FieldTimeOutlined,
  LockOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PlusOutlined,
  QrcodeOutlined,
  ReloadOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import {
  Alert,
  Badge,
  Breadcrumb,
  Button,
  ConfigProvider,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Select,
  Segmented,
  Spin,
  Table,
  Tag,
  message,
  theme,
} from "antd";
import type { MenuProps } from "antd";
import type { ColumnsType } from "antd/es/table";
import "antd/dist/reset.css";
import React from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const { Header, Sider, Content } = Layout;

type JsonBody = Record<string, unknown>;
type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };
type HealthResponse = { ok: boolean; service?: string; time?: string };
type ApiUser = { id: string; username: string; accountType: string; organizationId?: string | null; merchantId?: string | null };
type LoginResponse = { accessToken: string; refreshToken: string; sessionId: string; tokenType: string; user: ApiUser };
type Overview = {
  merchants: number;
  ticketProducts: number;
  issuedTickets: number;
  verifications: number;
  barOrders: number;
  amusementSessions: number;
  openRiskAlerts: number;
  auditEvents: number;
};
type InterfaceItem = { code: string; name: string; method: string; path: string; status: string };
type IntegrationResponse = { policy: string; interfaces: InterfaceItem[] };
type Organization = { id: string; name: string; code?: string; orgType: string; status: string; createdAt: string };
type Merchant = { id: string; organizationId: string; displayName: string; companyName: string; status: string; riskLevel: string };
type Venue = { id: string; merchantId: string; name: string; code?: string; venueType: string; status: string };
type Area = { id: string; venueId?: string; name: string; areaType: string; status: string };
type LocationPoint = { id: string; areaId: string; name: string; code?: string; pointType: string; status: string };
type UserAccount = { id: string; username: string; accountType: string; status: string; merchantId?: string; organizationId?: string };
type MerchantRow = {
  merchant?: { id: string; displayName: string; status: string; riskLevel: string };
  verifications: number;
  settlementAmount?: { _sum?: { amount?: number | null } };
  barOrders: number;
};
type TicketProduct = {
  id: string;
  productCode: string;
  productName: string;
  productType: string;
  priceAmount: number;
  status: string;
  createdAt: string;
};
type TicketDynamicPricingRule = {
  id: string;
  ticketTypeId: string;
  ruleName: string;
  priority: number;
  effectiveFrom: string;
  effectiveTo: string;
  salesChannel?: string;
  adjustmentType: string;
  adjustmentRate?: number;
  adjustmentAmountCent?: number;
  minPriceCent?: number;
  maxPriceCent?: number;
  status: string;
  conditionRule: Record<string, JsonValue>;
  operatorNote?: string;
  evidenceAttachmentIds: string[];
  createdAt: string;
  updatedAt: string;
};
type TicketDynamicPricingSimulation = {
  ticketTypeId: string;
  visitDate: string;
  salesChannel: string;
  basePriceCent: number;
  adjustmentCent: number;
  finalPriceCent: number;
  matchedRuleId?: string;
  explanation: string;
};
type EntitlementTemplate = {
  id: string;
  name: string;
  entitlementType: string;
  defaultTotalTimes: number;
  defaultValueAmount: number;
  status: string;
};
type IssuedEntitlement = {
  id: string;
  entitlementCode: string;
  entitlementType: string;
  totalTimes: number;
  usedTimes: number;
  valueAmount: number;
  status: string;
  createdAt: string;
};
type VerificationRecord = { id: string; verificationNo: string; status: string; source: string; resultReason?: string; createdAt: string };
type SettlementBill = { id: string; billNo: string; merchantId?: string; totalAmount: number; status: string; createdAt: string };
type BarTable = { id: string; tableCode: string; tableName: string; capacity?: number; minSpendAmount: number; status: string };
type BarProduct = { id: string; productName: string; category: string; priceAmount: number; status: string };
type BarInventoryFlow = { id: string; flowType: string; quantity: number; reason?: string; createdAt: string; product?: BarProduct };
type BarInventoryItem = { id: string; itemCode: string; itemName: string; category: string; unit: string; status: string; minQuantity: number };
type BarStockBalance = { id: string; itemId: string; locationType: string; quantity: number; item?: BarInventoryItem };
type BarStockMovement = {
  id: string;
  movementType: string;
  quantity: number;
  fromLocationType?: string;
  toLocationType?: string;
  afterFromQuantity?: number | null;
  afterToQuantity?: number | null;
  createdAt: string;
  item?: BarInventoryItem;
  product?: BarProduct;
};
type SavedBottle = {
  id: string;
  savedNo: string;
  customerName: string;
  customerPhone?: string;
  remainingValue: number;
  remainingUnit: string;
  status: string;
  storageLocation?: string;
  expiresAt?: string;
  product?: BarProduct;
};
type BarProductionTicket = { id: string; ticketNo: string; department: string; quantity: number; status: string; createdAt: string };
type BarReservation = { id: string; reservationNo: string; customerName: string; partySize: number; depositAmount: number; status: string; reservedAt: string };
type BarOrder = {
  id: string;
  orderNo: string;
  tableId: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  items?: Array<{ id: string; productId: string; quantity: number; lineAmount: number; status: string }>;
  bills?: Array<{ id: string; status: string; paidAmount: number; totalAmount: number }>;
};
type BarBill = { id: string; status: string; paidAmount: number; totalAmount: number };
type AmusementProject = {
  id: string;
  projectCode: string;
  projectName: string;
  capacity: number;
  status: string;
  sessions?: AmusementSession[];
};
type AmusementSession = {
  id: string;
  projectId: string;
  sessionName: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  usedCapacity: number;
  lockedCapacity?: number;
  status: string;
};
type AmusementVerification = { id: string; verificationNo: string; status: string; resultReason?: string; createdAt: string };
type AmusementReservation = { id: string; reservationNo: string; customerName: string; reservedCapacity: number; status: string; createdAt: string };
type AmusementQueueEntry = { id: string; queueNo: string; customerName?: string; partySize: number; status: string; createdAt: string };
type AmusementDeviceStatus = { id: string; deviceName: string; deviceCode: string; status: string; updatedAt: string };
type AmusementInspectionRecord = { id: string; result: string; status: string; createdAt: string };
type AmusementFaultRecord = { id: string; faultNo: string; faultLevel: string; summary: string; status: string; createdAt: string };
type AmusementGroupBooking = {
  id: string;
  projectId: string;
  sessionId: string;
  bookingType: string;
  customerName: string;
  contactPhone?: string;
  partySize: number;
  reservedCapacity: number;
  bookingStatus: string;
  approvalNote?: string;
  operatorNote?: string;
  evidenceAttachmentIds: string[];
  approvedAt?: string;
  releasedAt?: string;
  createdAt: string;
  updatedAt: string;
};
type RiskRule = { id: string; ruleCode: string; ruleName: string; ruleType: string; threshold: number; status: string };
type RiskAlert = { id: string; alertType: string; riskLevel: string; summary: string; status: string; createdAt: string };
type AuditEvent = { id: string; eventType: string; eventCategory: string; sourceType: string; result: string; riskLevel: string; summary: string; occurredAt: string };
type OpenApp = { id: string; appCode: string; appName: string; appType: string; status: string; credentials?: Array<{ loginAccount: string; status: string }> };
type ThirdPartyOrderSync = { id: string; businessType: string; syncType: string; externalBusinessId: string; amount: number; status: string; createdAt: string };
type ThirdPartyVoucherVerification = { id: string; businessType: string; externalVerificationId: string; status: string; createdAt: string };
type IntegrationRetryTask = { id: string; businessType: string; taskType: string; status: string; attemptCount: number; createdAt: string };
type IntegrationCompensationTask = { id: string; businessType: string; taskType: string; status: string; createdAt: string };
type ReconciliationBatch = { id: string; businessType: string; reconDate: string; status: string; diffCount: number; createdAt: string };
type ReconciliationDifference = { id: string; businessType: string; diffType: string; status: string; createdAt: string };
type IntegrationSku = { id: string; skuCode: string; skuName: string; category?: string; priceAmount: number; status: string; createdAt: string };
type RobotTask = { id: string; taskNo: string; taskType: string; pickupLocation?: string; dropoffLocation?: string; assignedRobotCode?: string; status: string; createdAt: string };
type RobotTaskEvent = { id: string; eventType: string; taskStatus: string; occurredAt: string };
type CameraDevice = { id: string; cameraCode: string; cameraName: string; provider?: string; status: string; createdAt: string };
type VideoEvidenceLink = { id: string; evidenceNo: string; eventType: string; objectType?: string; status: string; viewCount: number; createdAt: string };
type WecomBinding = { id: string; corpId: string; staffUserId: string; status: string; createdAt: string };
type WecomConversation = { id: string; conversationKey: string; staffUserId: string; customerName?: string; status: string; updatedAt: string };
type WecomMessage = { id: string; msgId: string; senderType: string; sanitizedContent?: string; riskLevel: string; sentAt: string };
type WecomRiskHit = { id: string; riskType: string; riskLevel: string; keyword?: string; summary: string; status: string; createdAt: string };
type DeviceIntegrationDevice = { id: string; deviceCode: string; deviceType: string; status: string; createdAt: string };
type DeviceIntegrationEvent = { id: string; deviceCode?: string; eventType: string; status: string; occurredAt: string };
type DeviceOfflineSyncBatch = { id: string; batchNo: string; totalCount: number; rejectedCount: number; status: string; createdAt: string };
type RobotAnalytics = { total: number; signed: number; delivered: number; abnormal: number; pending: number; completionRate: number; abnormalRate: number };
type MetricDefinition = { id: string; metricCode: string; metricName: string; category: string; status: string; createdAt: string };
type AiQualityTask = { id: string; taskNo: string; taskType: string; objectType?: string; status: string; retryCount: number; createdAt: string };
type AiQualityResult = { id: string; sourceType: string; objectType?: string; riskLevel: string; score: number; summary: string; createdAt: string };
type CustomerIntent = { id: string; intentType: string; label: string; confidence: number; status: string; createdAt: string };
type StaffServiceScore = { id: string; staffUserId?: string; score: number; riskPenalty: number; sampleSize: number; snapshotDate: string; createdAt: string };
type MerchantHealthScore = { id: string; merchantId?: string; score: number; operationsScore: number; financeScore: number; riskScore: number; serviceScore: number; snapshotDate: string; createdAt: string };
type MetricAnomalyExplanation = { id: string; metricCode: string; metricName: string; severity: string; changeRate: number; explanation: string; createdAt: string };
type BusinessMetricSnapshot = {
  snapshotId: string;
  metricKey: string;
  domain: string;
  periodType: string;
  businessDate: string;
  segmentType: string;
  segmentId?: string;
  actualValue: number;
  baselineValue?: number;
  varianceRate?: number;
  trendDirection: string;
  projectionMethod: string;
  projectedValue?: number;
  confidenceScore?: number;
  payload: Record<string, JsonValue>;
  createdAt: string;
};

type Snapshot = {
  health: HealthResponse;
  overview: Overview;
  integration: IntegrationResponse;
  organizations: Organization[];
  merchantsRaw: Merchant[];
  venues: Venue[];
  areas: Area[];
  locationPoints: LocationPoint[];
  users: UserAccount[];
  merchants: MerchantRow[];
  tickets: TicketProduct[];
  ticketDynamicPricingRules: TicketDynamicPricingRule[];
  entitlementTemplates: EntitlementTemplate[];
  issuedEntitlements: IssuedEntitlement[];
  verifications: VerificationRecord[];
  settlementBills: SettlementBill[];
  barTables: BarTable[];
  barProducts: BarProduct[];
  barInventoryFlows: BarInventoryFlow[];
  barInventoryItems: BarInventoryItem[];
  barStockBalances: BarStockBalance[];
  barStockMovements: BarStockMovement[];
  savedBottles: SavedBottle[];
  barProductionTickets: BarProductionTicket[];
  barReservations: BarReservation[];
  barOrders: BarOrder[];
  amusementProjects: AmusementProject[];
  amusementSessions: AmusementSession[];
  amusementVerifications: AmusementVerification[];
  amusementReservations: AmusementReservation[];
  amusementQueueEntries: AmusementQueueEntry[];
  amusementDeviceStatuses: AmusementDeviceStatus[];
  amusementInspectionRecords: AmusementInspectionRecord[];
  amusementFaults: AmusementFaultRecord[];
  amusementGroupBookings: AmusementGroupBooking[];
  riskRules: RiskRule[];
  riskAlerts: RiskAlert[];
  events: AuditEvent[];
  openApps: OpenApp[];
  thirdPartyOrderSyncs: ThirdPartyOrderSync[];
  thirdPartyVoucherVerifications: ThirdPartyVoucherVerification[];
  integrationRetryTasks: IntegrationRetryTask[];
  integrationCompensationTasks: IntegrationCompensationTask[];
  reconciliationBatches: ReconciliationBatch[];
  reconciliationDifferences: ReconciliationDifference[];
  integrationSkus: IntegrationSku[];
  robotTasks: RobotTask[];
  robotTaskEvents: RobotTaskEvent[];
  cameraDevices: CameraDevice[];
  videoEvidenceLinks: VideoEvidenceLink[];
  wecomBindings: WecomBinding[];
  wecomConversations: WecomConversation[];
  wecomMessages: WecomMessage[];
  wecomRiskHits: WecomRiskHit[];
  deviceIntegrationDevices: DeviceIntegrationDevice[];
  deviceIntegrationEvents: DeviceIntegrationEvent[];
  deviceOfflineSyncBatches: DeviceOfflineSyncBatch[];
  robotAnalytics: RobotAnalytics;
  metricDefinitions: MetricDefinition[];
  aiQualityTasks: AiQualityTask[];
  aiQualityResults: AiQualityResult[];
  customerIntents: CustomerIntent[];
  staffServiceScores: StaffServiceScore[];
  merchantHealthScores: MerchantHealthScore[];
  metricAnomalyExplanations: MetricAnomalyExplanation[];
  businessMetricSnapshots: BusinessMetricSnapshot[];
  loadedAt: string;
};

type SectionKey = "dashboard" | "foundation" | "ticketing" | "bar" | "amusement" | "analytics" | "interfaces" | "security" | "events";
type NavigationGroupKey = "overview" | "operations" | "platform" | "insight";
type BusinessEntryKey = Extract<SectionKey, "ticketing" | "bar" | "amusement">;

type SectionMeta = {
  title: string;
  menuLabel: string;
  description: string;
  groupKey: NavigationGroupKey;
  groupLabel: string;
  icon: React.ReactNode;
};

const sectionMeta: Record<SectionKey, SectionMeta> = {
  dashboard: {
    title: "运营总览",
    menuLabel: "控制台",
    description: "关键指标、最近业务流和系统健康状态。",
    groupKey: "overview",
    groupLabel: "总览",
    icon: <DashboardOutlined />,
  },
  ticketing: {
    title: "票务核销",
    menuLabel: "票务核销",
    description: "票种、权益、出票、核销和结算。",
    groupKey: "operations",
    groupLabel: "业务运营",
    icon: <QrcodeOutlined />,
  },
  bar: {
    title: "酒吧经营",
    menuLabel: "酒吧经营",
    description: "开台、点单、结账、库存、存酒和出品。",
    groupKey: "operations",
    groupLabel: "业务运营",
    icon: <CoffeeOutlined />,
  },
  amusement: {
    title: "游乐运营",
    menuLabel: "游乐运营",
    description: "项目、场次、预约、排队、设备和巡检。",
    groupKey: "operations",
    groupLabel: "业务运营",
    icon: <RocketOutlined />,
  },
  foundation: {
    title: "平台地基",
    menuLabel: "平台地基",
    description: "组织、商户、场馆、区域、点位和账号。",
    groupKey: "platform",
    groupLabel: "平台治理",
    icon: <BankOutlined />,
  },
  interfaces: {
    title: "第三方接口",
    menuLabel: "第三方接口",
    description: "开放应用、预留接口、订单同步、重试和对账。",
    groupKey: "platform",
    groupLabel: "平台治理",
    icon: <ApiOutlined />,
  },
  security: {
    title: "风控预警",
    menuLabel: "风控预警",
    description: "风险规则、预警处理和异常闭环。",
    groupKey: "platform",
    groupLabel: "平台治理",
    icon: <SafetyCertificateOutlined />,
  },
  events: {
    title: "事件留痕",
    menuLabel: "事件留痕",
    description: "最近事件、审计记录和业务时间线。",
    groupKey: "platform",
    groupLabel: "平台治理",
    icon: <AuditOutlined />,
  },
  analytics: {
    title: "经营看板",
    menuLabel: "经营看板",
    description: "商户经营、业务量、结算和风险概览。",
    groupKey: "insight",
    groupLabel: "分析决策",
    icon: <BarChartOutlined />,
  },
};

const navigationGroups: Array<{ key: NavigationGroupKey; label: string; icon: React.ReactNode; children: SectionKey[] }> = [
  { key: "overview", label: "总览", icon: <DashboardOutlined />, children: ["dashboard"] },
  { key: "operations", label: "业务运营", icon: <CoffeeOutlined />, children: ["ticketing", "bar", "amusement"] },
  { key: "platform", label: "平台治理", icon: <BankOutlined />, children: ["foundation", "interfaces", "security", "events"] },
  { key: "insight", label: "分析决策", icon: <BarChartOutlined />, children: ["analytics"] },
];

const fallbackNavigationGroup = navigationGroups[0]!;
const defaultOpenMenuKeys = navigationGroups.map((group) => group.key);

const sectionQueryValues = new Set<SectionKey>(["dashboard", "foundation", "ticketing", "bar", "amusement", "analytics", "interfaces", "security", "events"]);
const businessEntryPaths: Record<BusinessEntryKey, string> = {
  ticketing: "/ticketing/",
  bar: "/bar/",
  amusement: "/amusement/",
};

function normalizeSection(value: string | null | undefined): SectionKey | null {
  if (!value) return null;
  return sectionQueryValues.has(value as SectionKey) ? (value as SectionKey) : null;
}

function getInitialSectionFromLocation(): SectionKey {
  const url = new URL(window.location.href);
  const page = normalizeSection(url.searchParams.get("page"));
  if (page) return page;
  const hash = normalizeSection(url.hash.replace(/^#\/?/, ""));
  if (hash) return hash;
  const pathSection = normalizeSection(url.pathname.replace(/^\/+|\/+$/g, ""));
  return pathSection ?? "dashboard";
}

function sectionUrl(section: SectionKey) {
  if (section === "dashboard") return "/";
  return `/?page=${section}`;
}

const menuItems: MenuProps["items"] = navigationGroups.map((group) => ({
  key: group.key,
  icon: group.icon,
  label: group.label,
  children: group.children.map((section) => ({
    key: section,
    icon: sectionMeta[section].icon,
    label: sectionMeta[section].menuLabel,
  })),
}));

function getApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");
  if (window.location.port === "3000") return `${window.location.protocol}//${window.location.hostname}:8080`;
  return "";
}

const apiBaseUrl = getApiBaseUrl();
let apiAccessToken = "";
const openApiPrefix = ["/api", "open"].join("/");
const formalApiPrefix = ["/api", "v1"].join("/");
const authApiPrefix = [formalApiPrefix, "auth"].join("/");

function setApiAccessToken(token: string) {
  apiAccessToken = token;
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

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (apiAccessToken) headers.set("authorization", `Bearer ${apiAccessToken}`);
  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  const text = await response.text();
  if (!response.ok) throw new Error(`${path} ${response.status}: ${text || response.statusText}`);
  return (text ? unwrapApiPayload(JSON.parse(text)) : null) as T;
}

function getJson<T>(path: string) {
  return requestJson<T>(path);
}

function postJson<T>(path: string, body: JsonBody) {
  return requestJson<T>(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(normalizePostBody(path, body)),
  });
}

function formatMoney(amount: number | null | undefined) {
  return `¥${((amount ?? 0) / 100).toFixed(2)}`;
}

function formatRate(value: number | null | undefined) {
  return `${((value ?? 0) / 100).toFixed(2)}%`;
}

function formatPercentPoint(value: number | null | undefined) {
  return `${(value ?? 0).toFixed(2)}%`;
}

function formatMetricValue(metricKey: string, value: number | null | undefined) {
  if (metricKey.includes("amount_cents")) return formatMoney(value);
  return (value ?? 0).toLocaleString("zh-CN", { maximumFractionDigits: 4 });
}

function toCents(value: number | null | undefined) {
  return Math.round((value ?? 0) * 100);
}

function formatTime(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function statusColor(status: string) {
  if (["active", "success", "paid", "scheduled", "reserved", "normal"].includes(status)) return "green";
  if (["open", "billing", "pending"].includes(status)) return "blue";
  if (["manual_allowed", "idle", "cleaning"].includes(status)) return "gold";
  if (["failed", "abnormal", "locked", "high"].includes(status)) return "red";
  return "default";
}

function metricItems(snapshot: Snapshot) {
  const { overview, integration } = snapshot;
  return [
    { label: "商户", value: overview.merchants, hint: "经营主体" },
    { label: "票务产品", value: overview.ticketProducts, hint: `出票 ${overview.issuedTickets}` },
    { label: "核销", value: overview.verifications, hint: "成功核销" },
    { label: "酒吧订单", value: overview.barOrders, hint: "开台到结账" },
    { label: "游乐场次", value: overview.amusementSessions, hint: "容量池" },
    { label: "第三方接口", value: integration.interfaces.length, hint: "接口目录" },
    { label: "风险预警", value: overview.openRiskAlerts, hint: "未处理" },
    { label: "事件", value: overview.auditEvents, hint: "审计留痕" },
  ];
}

type PageStat = {
  label: string;
  value: string | number;
  tone?: "default" | "good" | "warning" | "danger";
};

function openRiskCount(snapshot: Snapshot) {
  return snapshot.riskAlerts.filter((item) => !["closed", "resolved", "reviewed"].includes(item.status)).length;
}

function getSectionStats(section: SectionKey, snapshot: Snapshot): PageStat[] {
  const activeInterfaces = snapshot.integration.interfaces.filter((item) => item.status !== "reserved").length;
  const highRiskAlerts = snapshot.riskAlerts.filter((item) => item.riskLevel === "high" || item.riskLevel === "critical").length;

  const stats: Record<SectionKey, PageStat[]> = {
    dashboard: [
      { label: "API", value: snapshot.health.ok ? "正常" : "异常", tone: snapshot.health.ok ? "good" : "danger" },
      { label: "待处理风险", value: openRiskCount(snapshot), tone: openRiskCount(snapshot) > 0 ? "warning" : "good" },
      { label: "审计事件", value: snapshot.overview.auditEvents },
      { label: "已启用接口", value: activeInterfaces },
    ],
    foundation: [
      { label: "组织", value: snapshot.organizations.length },
      { label: "商户", value: snapshot.merchantsRaw.length },
      { label: "场馆", value: snapshot.venues.length },
      { label: "账号", value: snapshot.users.length },
    ],
    ticketing: [
      { label: "票种", value: snapshot.tickets.length },
      { label: "已出票", value: snapshot.issuedEntitlements.length },
      { label: "核销", value: snapshot.verifications.length },
      { label: "结算单", value: snapshot.settlementBills.length },
    ],
    bar: [
      { label: "桌台", value: snapshot.barTables.length },
      { label: "订单", value: snapshot.barOrders.length },
      { label: "库存物料", value: snapshot.barInventoryItems.length },
      { label: "存酒", value: snapshot.savedBottles.length },
    ],
    amusement: [
      { label: "项目", value: snapshot.amusementProjects.length },
      { label: "场次", value: snapshot.amusementSessions.length },
      { label: "预约", value: snapshot.amusementReservations.length },
      { label: "故障", value: snapshot.amusementFaults.length, tone: snapshot.amusementFaults.length > 0 ? "warning" : "good" },
    ],
    analytics: [
      { label: "指标", value: snapshot.metricDefinitions.length },
      { label: "AI任务", value: snapshot.aiQualityTasks.length },
      { label: "客户意图", value: snapshot.customerIntents.length },
      { label: "异常解释", value: snapshot.metricAnomalyExplanations.length },
    ],
    interfaces: [
      { label: "接口目录", value: snapshot.integration.interfaces.length },
      { label: "开放应用", value: snapshot.openApps.length },
      { label: "重试任务", value: snapshot.integrationRetryTasks.length, tone: snapshot.integrationRetryTasks.length > 0 ? "warning" : "default" },
      { label: "对账差异", value: snapshot.reconciliationDifferences.length, tone: snapshot.reconciliationDifferences.length > 0 ? "warning" : "good" },
    ],
    security: [
      { label: "规则", value: snapshot.riskRules.length },
      { label: "待处理", value: openRiskCount(snapshot), tone: openRiskCount(snapshot) > 0 ? "warning" : "good" },
      { label: "高风险", value: highRiskAlerts, tone: highRiskAlerts > 0 ? "danger" : "good" },
      { label: "企微命中", value: snapshot.wecomRiskHits.length },
    ],
    events: [
      { label: "事件", value: snapshot.events.length },
      { label: "设备事件", value: snapshot.deviceIntegrationEvents.length },
      { label: "机器人事件", value: snapshot.robotTaskEvents.length },
      { label: "视频证据", value: snapshot.videoEvidenceLinks.length },
    ],
  };

  return stats[section];
}

function ResultBox({ value }: { value: unknown }) {
  if (!value) return null;
  return <pre className="result-box">{JSON.stringify(value, null, 2)}</pre>;
}

function SectionHeading({ icon, title, extra }: { icon: React.ReactNode; title: string; extra?: React.ReactNode }) {
  return (
    <div className="section-heading">
      {icon}
      <h1>{title}</h1>
      {extra ? <div className="section-extra">{extra}</div> : null}
    </div>
  );
}

function findNavigationGroup(section: SectionKey) {
  return navigationGroups.find((group) => group.children.includes(section)) ?? fallbackNavigationGroup;
}

function PageHeader({
  activeSection,
  loadedAt,
  stats,
  healthOk,
  onSectionChange,
}: {
  activeSection: SectionKey;
  loadedAt?: string;
  stats: PageStat[];
  healthOk: boolean;
  onSectionChange: (section: SectionKey) => void;
}) {
  const meta = sectionMeta[activeSection];
  const group = findNavigationGroup(activeSection);
  const siblingOptions = group.children.map((section) => ({
    label: sectionMeta[section].menuLabel,
    value: section,
  }));

  return (
    <div className="page-header">
      <div className="page-meta-row">
        <Breadcrumb
          items={[
            { title: meta.groupLabel },
            { title: meta.title },
          ]}
        />
        <Badge status={healthOk ? "success" : "error"} text={healthOk ? "系统在线" : "系统异常"} />
      </div>
      <div className="page-title-row">
        <div className="page-title-main">
          <span className="page-title-icon">{meta.icon}</span>
          <div>
            <h1>{meta.title}</h1>
            <p>{meta.description}</p>
          </div>
        </div>
        {loadedAt ? <small>更新 {formatTime(loadedAt)}</small> : null}
      </div>
      <div className="page-stat-strip">
        {stats.map((item) => (
          <div className={`page-stat page-stat-${item.tone ?? "default"}`} key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>
      {siblingOptions.length > 1 ? (
        <Segmented className="section-switcher" value={activeSection} options={siblingOptions} onChange={(value) => onSectionChange(value as SectionKey)} />
      ) : null}
    </div>
  );
}

function WorkspaceMap({
  activeSection,
  snapshot,
  onSelect,
}: {
  activeSection: SectionKey;
  snapshot: Snapshot;
  onSelect: (section: SectionKey) => void;
}) {
  return (
    <section className="section-block workspace-map">
      <SectionHeading icon={<DashboardOutlined />} title="模块地图" extra={<small>按业务域进入对应工作台</small>} />
      <div className="workspace-groups">
        {navigationGroups.map((group) => (
          <div className="workspace-group" key={group.key}>
            <div className="workspace-group-title">{group.label}</div>
            <div className="workspace-section-grid">
              {group.children.map((section) => {
                const meta = sectionMeta[section];
                const statsText = getSectionStats(section, snapshot)
                  .slice(0, 2)
                  .map((item) => `${item.label} ${item.value}`)
                  .join(" / ");
                return (
                  <button
                    type="button"
                    className={`workspace-card${activeSection === section ? " active" : ""}`}
                    key={section}
                    onClick={() => onSelect(section)}
                  >
                    <span className="workspace-card-icon">{meta.icon}</span>
                    <span className="workspace-card-copy">
                      <strong>{meta.menuLabel}</strong>
                      <small>{statsText}</small>
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function BusinessEntryIndex({ activeSection, snapshot, onSelect }: { activeSection: SectionKey; snapshot: Snapshot; onSelect: (section: SectionKey) => void }) {
  const entries: Array<{ key: BusinessEntryKey; title: string; detail: string; icon: React.ReactNode }> = [
    { key: "ticketing", title: "票务独立入口", detail: `票种 ${snapshot.tickets.length} / 核销 ${snapshot.verifications.length}`, icon: <QrcodeOutlined /> },
    { key: "bar", title: "酒吧独立入口", detail: `订单 ${snapshot.barOrders.length} / 存酒 ${snapshot.savedBottles.length}`, icon: <CoffeeOutlined /> },
    { key: "amusement", title: "游乐独立入口", detail: `项目 ${snapshot.amusementProjects.length} / 场次 ${snapshot.amusementSessions.length}`, icon: <RocketOutlined /> },
  ];

  return (
    <section className="section-block business-entry-index">
      <SectionHeading icon={<RocketOutlined />} title="业务独立入口" extra={<small>编写阶段采用索引页入口</small>} />
      <div className="business-entry-grid">
        {entries.map((entry) => (
          <a
            className={`business-entry-card${activeSection === entry.key ? " active" : ""}`}
            href={businessEntryPaths[entry.key]}
            key={entry.key}
            onClick={(event) => {
              event.preventDefault();
              onSelect(entry.key);
            }}
          >
            <span className="business-entry-icon">{entry.icon}</span>
            <span className="business-entry-copy">
              <strong>{entry.title}</strong>
              <small>{entry.detail}</small>
              <em>{businessEntryPaths[entry.key]}</em>
            </span>
          </a>
        ))}
      </div>
    </section>
  );
}

function LatestPanel({ icon, title, rows }: { icon: React.ReactNode; title: string; rows: Array<{ key: string; title: string; meta: string; status: string }> }) {
  return (
    <div className="panel">
      <div className="panel-title">{icon}<span>{title}</span></div>
      <div className="record-list">
        {rows.map((row) => <div className="record-row" key={row.key}><div><strong>{row.title}</strong><small>{row.meta}</small></div><Tag color={statusColor(row.status)}>{row.status}</Tag></div>)}
      </div>
    </div>
  );
}

function SimpleTable<T extends { id?: string; key?: React.Key }>({ title, data, columns }: { title: string; data: T[]; columns: ColumnsType<T> }) {
  return (
    <div className="table-panel">
      <div className="panel-title panel-title-row"><span>{title}</span><Badge className="panel-count" count={data.length} overflowCount={999} showZero /></div>
      <Table className="data-table" columns={columns} dataSource={data} rowKey={(row, index) => String(row.id ?? row.key ?? index)} pagination={false} size="small" scroll={{ x: true }} />
    </div>
  );
}

function DataTables({ snapshotValue }: { snapshotValue: Snapshot }) {
  return (
    <div className="table-grid two">
      <SimpleTable title="组织与商户" data={snapshotValue.merchantsRaw.slice(0, 10)} columns={[
        { title: "商户", dataIndex: "displayName" },
        { title: "公司", dataIndex: "companyName" },
        { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
      ]} />
      <SimpleTable title="场馆与点位" data={snapshotValue.locationPoints.slice(0, 10)} columns={[
        { title: "点位", dataIndex: "name" },
        { title: "编码", dataIndex: "code" },
        { title: "类型", dataIndex: "pointType" },
        { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
      ]} />
    </div>
  );
}

function App() {
  const [snapshot, setSnapshot] = React.useState<Snapshot | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [authUser, setAuthUser] = React.useState<ApiUser | null>(null);
  const [activeSection, setActiveSection] = React.useState<SectionKey>(() => getInitialSectionFromLocation());
  const [openMenuKeys, setOpenMenuKeys] = React.useState<string[]>(defaultOpenMenuKeys);
  const [sidebarCollapsed, setSidebarCollapsed] = React.useState(false);
  const [lastResult, setLastResult] = React.useState<unknown>(null);
  const [loginForm] = Form.useForm();
  const [ticketFlowForm] = Form.useForm();
  const [verifyForm] = Form.useForm();
  const [settlementForm] = Form.useForm();
  const [foundationForm] = Form.useForm();
  const [userForm] = Form.useForm();
  const [barFlowForm] = Form.useForm();
  const [inventoryForm] = Form.useForm();
  const [inventoryItemForm] = Form.useForm();
  const [stockMovementForm] = Form.useForm();
  const [savedBottleForm] = Form.useForm();
  const [savedBottlePickupForm] = Form.useForm();
  const [amusementFlowForm] = Form.useForm();
  const [amusementVerifyForm] = Form.useForm();
  const [amusementGroupBookingForm] = Form.useForm();
  const [dynamicPricingForm] = Form.useForm();
  const [dynamicPricingSimulationForm] = Form.useForm();
  const [riskForm] = Form.useForm();
  const [openAppForm] = Form.useForm();

  const navigateToSection = React.useCallback((section: SectionKey) => {
    setActiveSection(section);
    const nextUrl = sectionUrl(section);
    if (`${window.location.pathname}${window.location.search}` !== nextUrl) {
      window.history.pushState({ section }, "", nextUrl);
    }
  }, []);

  React.useEffect(() => {
    const handlePopState = () => setActiveSection(getInitialSectionFromLocation());
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const loadData = React.useCallback(async () => {
    if (!apiAccessToken) return;
    setLoading(true);
    setError(null);
    try {
      const [
        health,
        overview,
        integration,
        organizations,
        merchantsRaw,
        venues,
        areas,
        locationPoints,
        users,
        merchants,
        tickets,
        ticketDynamicPricingRules,
        entitlementTemplates,
        issuedEntitlements,
        verifications,
        settlementBills,
        barTables,
        barProducts,
        barInventoryFlows,
        barInventoryItems,
        barStockBalances,
        barStockMovements,
        savedBottles,
        barProductionTickets,
        barReservations,
        barOrders,
        amusementProjects,
        amusementSessions,
        amusementVerifications,
        amusementReservations,
        amusementQueueEntries,
        amusementDeviceStatuses,
        amusementInspectionRecords,
        amusementFaults,
        amusementGroupBookings,
        riskRules,
        riskAlerts,
        events,
        openApps,
        thirdPartyOrderSyncs,
        thirdPartyVoucherVerifications,
        integrationRetryTasks,
        integrationCompensationTasks,
        reconciliationBatches,
        reconciliationDifferences,
        integrationSkus,
        robotTasks,
        robotTaskEvents,
        cameraDevices,
        videoEvidenceLinks,
        wecomBindings,
        wecomConversations,
        wecomMessages,
        wecomRiskHits,
        deviceIntegrationDevices,
        deviceIntegrationEvents,
        deviceOfflineSyncBatches,
        robotAnalytics,
        metricDefinitions,
        aiQualityTasks,
        aiQualityResults,
        customerIntents,
        staffServiceScores,
        merchantHealthScores,
        metricAnomalyExplanations,
        businessMetricSnapshots,
      ] = await Promise.all([
        getJson<HealthResponse>("/api/v1/system/health"),
        getJson<Overview>("/api/v1/analytics/dashboards/p0-overview"),
        getJson<IntegrationResponse>("/api/v1/integration/provider-adapters"),
        getJson<Organization[]>("/api/v1/platform/business-entities"),
        getJson<Merchant[]>("/api/v1/platform/merchants"),
        getJson<Venue[]>("/api/v1/platform/projects"),
        getJson<Area[]>("/api/v1/platform/spaces"),
        getJson<LocationPoint[]>("/api/v1/platform/spaces"),
        getJson<UserAccount[]>("/api/v1/access/users"),
        getJson<MerchantRow[]>("/api/v1/analytics/merchants"),
        getJson<TicketProduct[]>("/api/v1/ticketing/ticket-types"),
        getJson<TicketDynamicPricingRule[]>("/api/v1/ticketing/dynamic-pricing-rules"),
        getJson<EntitlementTemplate[]>("/api/v1/ticketing/entitlement-templates"),
        getJson<IssuedEntitlement[]>("/api/v1/ticketing/issued-entitlements"),
        getJson<VerificationRecord[]>("/api/v1/ticketing/verifications"),
        getJson<SettlementBill[]>("/api/v1/finance/ar-ap-items"),
        getJson<BarTable[]>("/api/v1/bar/tables"),
        getJson<BarProduct[]>("/api/v1/bar/products"),
        getJson<BarInventoryFlow[]>("/api/v1/inventory/stock-movements"),
        getJson<BarInventoryItem[]>("/api/v1/inventory/items"),
        getJson<BarStockBalance[]>("/api/v1/inventory/stock-balances"),
        getJson<BarStockMovement[]>("/api/v1/inventory/stock-movements"),
        getJson<SavedBottle[]>("/api/v1/inventory/saved-bottles"),
        getJson<BarProductionTicket[]>("/api/v1/bar/production-tickets"),
        getJson<BarReservation[]>("/api/v1/bar/reservations"),
        getJson<BarOrder[]>("/api/v1/bar/orders"),
        getJson<AmusementProject[]>("/api/v1/amusement/projects"),
        getJson<AmusementSession[]>("/api/v1/amusement/sessions"),
        getJson<AmusementVerification[]>("/api/v1/amusement/verifications"),
        getJson<AmusementReservation[]>("/api/v1/amusement/reservations"),
        getJson<AmusementQueueEntry[]>("/api/v1/amusement/queue-entries"),
        getJson<AmusementDeviceStatus[]>("/api/v1/amusement/device-statuses"),
        getJson<AmusementInspectionRecord[]>("/api/v1/amusement/safety-inspections"),
        getJson<AmusementFaultRecord[]>("/api/v1/amusement/manual-overrides"),
        getJson<AmusementGroupBooking[]>("/api/v1/amusement/group-bookings"),
        getJson<RiskRule[]>("/api/v1/analytics/risk-rules"),
        getJson<RiskAlert[]>("/api/v1/analytics/risk-alerts"),
        getJson<AuditEvent[]>("/api/v1/evidence/events?limit=80"),
        getJson<OpenApp[]>("/api/v1/integration/provider-adapters"),
        getJson<ThirdPartyOrderSync[]>("/api/v1/integration/order-syncs"),
        getJson<ThirdPartyVoucherVerification[]>("/api/v1/integration/voucher-verifications"),
        getJson<IntegrationRetryTask[]>("/api/v1/integration/retry-tasks"),
        getJson<IntegrationCompensationTask[]>("/api/v1/integration/compensation-tasks"),
        getJson<ReconciliationBatch[]>("/api/v1/finance/reconciliation/batches"),
        getJson<ReconciliationDifference[]>("/api/v1/finance/reconciliation/differences"),
        getJson<IntegrationSku[]>("/api/v1/integration/retail/skus"),
        getJson<RobotTask[]>("/api/v1/integration/robot/tasks"),
        getJson<RobotTaskEvent[]>("/api/v1/integration/robot/task-events"),
        getJson<CameraDevice[]>("/api/v1/integration/video/cameras"),
        getJson<VideoEvidenceLink[]>("/api/v1/evidence/events"),
        getJson<WecomBinding[]>("/api/v1/integration/wecom/bindings"),
        getJson<WecomConversation[]>("/api/v1/integration/wecom/conversations"),
        getJson<WecomMessage[]>("/api/v1/integration/wecom/messages"),
        getJson<WecomRiskHit[]>("/api/v1/integration/wecom/risk-hits"),
        getJson<DeviceIntegrationDevice[]>("/api/v1/integration/devices"),
        getJson<DeviceIntegrationEvent[]>("/api/v1/integration/devices/events"),
        getJson<DeviceOfflineSyncBatch[]>("/api/v1/integration/devices/offline-sync-batches"),
        getJson<RobotAnalytics>("/api/v1/analytics/robot"),
        getJson<MetricDefinition[]>("/api/v1/analytics/metric-definitions"),
        getJson<AiQualityTask[]>("/api/v1/analytics/ai-quality/tasks"),
        getJson<AiQualityResult[]>("/api/v1/analytics/ai-quality/results"),
        getJson<CustomerIntent[]>("/api/v1/analytics/customer-intents"),
        getJson<StaffServiceScore[]>("/api/v1/analytics/staff-service-score"),
        getJson<MerchantHealthScore[]>("/api/v1/analytics/merchant-health"),
        getJson<MetricAnomalyExplanation[]>("/api/v1/analytics/metric-anomalies"),
        getJson<BusinessMetricSnapshot[]>("/api/v1/analytics/business-metric-snapshots"),
      ]);
      setSnapshot({
        health,
        overview,
        integration,
        organizations,
        merchantsRaw,
        venues,
        areas,
        locationPoints,
        users,
        merchants,
        tickets,
        ticketDynamicPricingRules,
        entitlementTemplates,
        issuedEntitlements,
        verifications,
        settlementBills,
        barTables,
        barProducts,
        barInventoryFlows,
        barInventoryItems,
        barStockBalances,
        barStockMovements,
        savedBottles,
        barProductionTickets,
        barReservations,
        barOrders,
        amusementProjects,
        amusementSessions,
        amusementVerifications,
        amusementReservations,
        amusementQueueEntries,
        amusementDeviceStatuses,
        amusementInspectionRecords,
        amusementFaults,
        amusementGroupBookings,
        riskRules,
        riskAlerts,
        events,
        openApps,
        thirdPartyOrderSyncs,
        thirdPartyVoucherVerifications,
        integrationRetryTasks,
        integrationCompensationTasks,
        reconciliationBatches,
        reconciliationDifferences,
        integrationSkus,
        robotTasks,
        robotTaskEvents,
        cameraDevices,
        videoEvidenceLinks,
        wecomBindings,
        wecomConversations,
        wecomMessages,
        wecomRiskHits,
        deviceIntegrationDevices,
        deviceIntegrationEvents,
        deviceOfflineSyncBatches,
        robotAnalytics,
        metricDefinitions,
        aiQualityTasks,
        aiQualityResults,
        customerIntents,
        staffServiceScores,
        merchantHealthScores,
        metricAnomalyExplanations,
        businessMetricSnapshots,
        loadedAt: new Date().toISOString(),
      });
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "数据加载失败");
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    if (authUser) void loadData();
  }, [authUser, loadData]);

  async function handleLogin(values: Record<string, string>) {
    setLoading(true);
    setError(null);
    try {
      const login = await postJson<LoginResponse>("/api/v1/auth/login", {
        loginName: values.username,
        password: values.password,
      });
      setApiAccessToken(login.accessToken);
      setAuthUser(login.user);
      message.success("登录成功");
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "登录失败");
    } finally {
      setLoading(false);
    }
  }

  async function runAction<T>(label: string, action: () => Promise<T>, reset?: () => void) {
    try {
      const result = await action();
      setLastResult(result);
      message.success(`${label}完成`);
      reset?.();
      await loadData();
      return result;
    } catch (actionError) {
      const detail = actionError instanceof Error ? actionError.message : `${label}失败`;
      setLastResult({ error: detail });
      message.error(detail);
      throw actionError;
    }
  }

  async function handleFoundationFlow(values: Record<string, string>) {
    await runAction("基础资料创建", async () => {
      const organization = await postJson<Organization>("/api/v1/platform/business-entities", {
        orgType: "platform",
        name: values.organizationName,
        shortName: values.organizationName,
        code: `org-${Date.now()}`,
      });
      const merchant = await postJson<Merchant>("/api/v1/platform/merchants", {
        organizationId: organization.id,
        merchantType: "platform",
        companyName: `${values.merchantName} Company`,
        displayName: values.merchantName,
        contactName: values.contactName || "P0 Operator",
        contactPhone: values.contactPhone || "18800000002",
        status: "active",
      });
      const venue = await postJson<Venue>("/api/v1/platform/projects", {
        merchantId: merchant.id,
        venueType: "complex",
        name: values.venueName,
        code: `venue-${Date.now()}`,
        floor: "1F",
        capacity: 300,
        status: "active",
      });
      const area = await postJson<Area>("/api/v1/platform/spaces", {
        venueId: venue.id,
        name: values.areaName,
        areaType: "operation",
        floor: "1F",
        capacity: 100,
      });
      const point = await postJson<LocationPoint>("/api/v1/platform/spaces", {
        areaId: area.id,
        venueId: venue.id,
        merchantId: merchant.id,
        pointType: "gate",
        name: values.pointName,
        code: `point-${Date.now()}`,
        capacity: 60,
      });
      return { organization, merchant, venue, area, point };
    }, () => foundationForm.resetFields());
  }

  async function handleCreateUser(values: Record<string, string>) {
    await runAction("账号创建", async () => {
      const organizationId = values.organizationId || snapshot?.organizations[0]?.id;
      const merchantId = values.merchantId || snapshot?.merchantsRaw[0]?.id;
      return postJson<UserAccount>("/api/v1/access/users", {
        username: values.username,
        password: values.password,
        phone: values.phone,
        accountType: values.accountType || "staff",
        organizationId,
        merchantId,
        status: "active",
      });
    }, () => userForm.resetFields());
  }

  async function handleTicketFlow(values: Record<string, string | number>) {
    await runAction("票务闭环", async () => {
      const product = await postJson<TicketProduct>("/api/v1/ticketing/ticket-types", {
        productName: values.productName,
        productType: "package",
        priceAmount: toCents(Number(values.priceYuan)),
        status: "active",
      });
      const template = await postJson<EntitlementTemplate>("/api/v1/ticketing/entitlement-templates", {
        name: values.entitlementName,
        entitlementType: values.entitlementType || "entry",
        defaultTotalTimes: Number(values.totalTimes || 1),
        defaultValueAmount: toCents(Number(values.priceYuan)),
      });
      await postJson(`/api/v1/ticketing/ticket-types/${product.id}/entitlements`, {
        entitlementTemplateId: template.id,
        quantity: 1,
        totalTimes: Number(values.totalTimes || 1),
        valueAmount: toCents(Number(values.priceYuan)),
      });
      const issued = await postJson<{ ticketCode: string; issuedEntitlements: IssuedEntitlement[] }>(`/api/v1/ticketing/ticket-types/${product.id}/issue`, {
        purchaserName: values.purchaserName,
        purchaserPhone: values.purchaserPhone,
      });
      verifyForm.setFieldValue("entitlementCode", issued.issuedEntitlements[0]?.entitlementCode);
      return { product, template, issued };
    }, () => ticketFlowForm.resetFields());
  }

  async function handlePreview() {
    const entitlementCode = verifyForm.getFieldValue("entitlementCode");
    await runAction("核销预检", () => postJson("/api/v1/ticketing/verifications/preview", { entitlementCode }));
  }

  async function handleVerify() {
    const entitlementCode = verifyForm.getFieldValue("entitlementCode");
    await runAction("核销", () => postJson("/api/v1/ticketing/verifications", { entitlementCode, source: "web-p0" }));
  }

  function dynamicCondition(values: Record<string, string | number>) {
    const key = String(values.conditionKey || "").trim();
    const value = String(values.conditionValue || "").trim();
    return key && value ? { [key]: value } : {};
  }

  async function handleDynamicPricingRule(values: Record<string, string | number>) {
    await runAction("P2 动态定价规则", () => postJson<TicketDynamicPricingRule>("/api/v1/ticketing/dynamic-pricing-rules", {
      ticketTypeId: values.ticketTypeId,
      ruleName: values.ruleName,
      priority: Number(values.priority || 0),
      effectiveFrom: values.effectiveFrom,
      effectiveTo: values.effectiveTo,
      salesChannel: values.salesChannel || undefined,
      adjustmentType: values.adjustmentType,
      adjustmentRate: values.adjustmentType === "percent_delta" ? Number(values.adjustmentRate || 0) : undefined,
      adjustmentAmountCent: values.adjustmentType === "fixed_delta" ? toCents(Number(values.adjustmentAmountYuan || 0)) : undefined,
      minPriceCent: values.minPriceYuan === undefined || values.minPriceYuan === "" ? undefined : toCents(Number(values.minPriceYuan)),
      maxPriceCent: values.maxPriceYuan === undefined || values.maxPriceYuan === "" ? undefined : toCents(Number(values.maxPriceYuan)),
      conditionRule: dynamicCondition(values),
      operatorNote: values.operatorNote || undefined,
    }), () => dynamicPricingForm.resetFields());
  }

  async function handlePublishDynamicPricingRule(ruleId: string) {
    await runAction("P2 动态定价发布", () => postJson<TicketDynamicPricingRule>(`/api/v1/ticketing/dynamic-pricing-rules/${ruleId}/publish`, {
      action: "publish",
      reason: "Web 管理端发布动态定价规则",
    }));
  }

  async function handleDynamicPricingSimulation(values: Record<string, string | number>) {
    await runAction("P2 动态定价模拟", () => postJson<TicketDynamicPricingSimulation>("/api/v1/ticketing/dynamic-pricing/simulations", {
      ticketTypeId: values.ticketTypeId,
      visitDate: values.visitDate,
      salesChannel: values.salesChannel || "web",
      context: dynamicCondition(values),
    }));
  }

  async function handleSettlementGenerate(values: Record<string, string>) {
    await runAction("结算单生成", () => postJson("/api/v1/finance/ar-ap-items", {
      merchantId: values.merchantId || undefined,
    }), () => settlementForm.resetFields());
  }

  async function handleBarFlow(values: Record<string, string | number>) {
    await runAction("酒吧闭环", async () => {
      const table = values.tableId
        ? snapshot?.barTables.find((item) => item.id === values.tableId)
        : await postJson<BarTable>("/api/v1/bar/tables", {
            tableName: values.tableName || `P0 Table ${Date.now()}`,
            capacity: Number(values.capacity || 4),
            minSpendAmount: toCents(Number(values.minSpendYuan || 100)),
          });
      const product = values.productId
        ? snapshot?.barProducts.find((item) => item.id === values.productId)
        : await postJson<BarProduct>("/api/v1/bar/products", {
            productName: values.productName || `P0 Drink ${Date.now()}`,
            category: values.category || "drink",
            priceAmount: toCents(Number(values.priceYuan || 30)),
          });
      if (!table || !product) throw new Error("缺少桌台或商品");
      const order = await postJson<BarOrder>("/api/v1/bar/orders", { tableId: table.id });
      const item = await postJson(`/api/v1/bar/orders/${order.id}/items`, { productId: product.id, quantity: Number(values.quantity || 1) });
      const bill = await postJson<BarBill>(`/api/v1/bar/orders/${order.id}/bill`, {});
      const paid = await postJson<BarBill>(`/api/v1/bar/bills/${bill.id}/settle`, {
        paidAmount: bill.totalAmount,
        paymentMethod: "offline",
      });
      return { table, product, order, item, bill, paid };
    }, () => barFlowForm.resetFields());
  }

  async function handleInventoryFlow(values: Record<string, string | number>) {
    await runAction("库存流水登记", () => postJson("/api/v1/inventory/stock-movements", {
      productId: values.productId || undefined,
      flowType: values.flowType || "inbound",
      quantity: Number(values.quantity || 0),
      reason: values.reason || undefined,
    }), () => inventoryForm.resetFields());
  }

  async function handleInventoryItemCreate(values: Record<string, string | number>) {
    await runAction("P1 库存物料创建", () => postJson("/api/v1/inventory/items", {
      merchantId: values.merchantId || undefined,
      venueId: values.venueId || undefined,
      itemName: values.itemName,
      category: values.category || "drink",
      unit: values.unit || "bottle",
      minQuantity: Number(values.minQuantity || 0),
    }), () => inventoryItemForm.resetFields());
  }

  async function handleStockMovement(values: Record<string, string | number>) {
    const movementType = String(values.movementType || "inbound");
    const basePayload = {
      merchantId: values.merchantId || undefined,
      venueId: values.venueId || undefined,
      itemId: values.itemId,
      productId: values.productId || undefined,
      reason: values.reason || undefined,
    };
    const path =
      movementType === "transfer"
        ? "/api/v1/inventory/stock-movements/transfer"
        : movementType === "count"
          ? "/api/v1/inventory/stock-adjustments"
          : `/api/v1/inventory/stock-movements/${movementType}`;
    const payload =
      movementType === "transfer"
        ? {
            ...basePayload,
            quantity: Number(values.quantity || 0),
            fromLocationType: values.fromLocationType || "warehouse_stock",
            toLocationType: values.toLocationType || "bar_stock",
          }
        : movementType === "count"
          ? {
              ...basePayload,
              countedQuantity: Number(values.countedQuantity || values.quantity || 0),
              locationType: values.locationType || "bar_stock",
            }
          : {
              ...basePayload,
              quantity: Number(values.quantity || 0),
              locationType: values.locationType || (movementType === "inbound" ? "warehouse_stock" : "bar_stock"),
            };
    await runAction("P1 库存操作", () => postJson(path, payload), () => stockMovementForm.resetFields());
  }

  async function handleSavedBottleCreate(values: Record<string, string | number>) {
    await runAction("P1 存酒登记", () => postJson("/api/v1/inventory/saved-bottles", {
      merchantId: values.merchantId || undefined,
      venueId: values.venueId || undefined,
      productId: values.productId || undefined,
      customerName: values.customerName,
      customerPhone: values.customerPhone || undefined,
      storageLocation: values.storageLocation || undefined,
      initialUnit: values.initialUnit || "percent",
      initialValue: Number(values.initialValue || 100),
      expiresAt: values.expiresAt || undefined,
    }), () => savedBottleForm.resetFields());
  }

  async function handleSavedBottlePickup(values: Record<string, string | number>) {
    const savedBottleId = values.savedBottleId || snapshot?.savedBottles[0]?.id;
    if (!savedBottleId) throw new Error("没有可取用的存酒");
    await runAction("P1 存酒取用", () => postJson(`/api/v1/inventory/saved-bottles/${savedBottleId}/claim`, {
      quantity: Number(values.quantity || 0),
      reason: values.reason || undefined,
    }), () => savedBottlePickupForm.resetFields());
  }

  async function handleAmusementFlow(values: Record<string, string | number>) {
    await runAction("游乐项目和场次创建", async () => {
      const project = await postJson<AmusementProject>("/api/v1/amusement/projects", {
        projectName: values.projectName,
        capacity: Number(values.capacity || 20),
      });
      await postJson(`/api/v1/amusement/projects/${project.id}/rules`, { ruleType: "age", ruleValue: "P0 manual check" });
      const startsAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();
      const endsAt = new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString();
      const session = await postJson<AmusementSession>(`/api/v1/amusement/sessions`, {
        sessionName: values.sessionName,
        startsAt,
        endsAt,
        capacity: Number(values.sessionCapacity || values.capacity || 20),
      });
      amusementVerifyForm.setFieldValue("sessionId", session.id);
      return { project, session };
    }, () => amusementFlowForm.resetFields());
  }

  async function handleAmusementVerify(values: Record<string, string>) {
    await runAction("游乐放行", () => {
      const sessionId = values.sessionId || snapshot?.amusementSessions[0]?.id;
      if (!sessionId) throw new Error("没有可用场次");
      return postJson(`/api/v1/amusement/verifications`, {
        entitlementCode: values.entitlementCode || undefined,
        manualReason: values.manualReason || "P0 web manual allow",
      });
    }, () => amusementVerifyForm.resetFields());
  }

  async function handleAmusementGroupBooking(values: Record<string, string | number>) {
    const session = snapshot?.amusementSessions.find((item) => item.id === values.sessionId);
    if (!session) throw new Error("请选择可用场次");
    await runAction("P2 团体/包场申请", () => postJson<AmusementGroupBooking>("/api/v1/amusement/group-bookings", {
      projectId: session.projectId,
      sessionId: session.id,
      bookingType: values.bookingType || "group",
      customerName: values.customerName,
      contactPhone: values.contactPhone || undefined,
      partySize: Number(values.partySize || 1),
      reservedCapacity: Number(values.reservedCapacity || values.partySize || 1),
      operatorNote: values.operatorNote || undefined,
    }), () => amusementGroupBookingForm.resetFields());
  }

  async function handleApproveGroupBooking(bookingId: string) {
    await runAction("P2 团体/包场审批", () => postJson<AmusementGroupBooking>(`/api/v1/amusement/group-bookings/${bookingId}/approve`, {
      approvalNote: "Web 管理端审批并锁定容量",
    }));
  }

  async function handleReleaseGroupBooking(bookingId: string) {
    await runAction("P2 团体/包场释放", () => postJson<AmusementGroupBooking>(`/api/v1/amusement/group-bookings/${bookingId}/release`, {
      reason: "Web 管理端释放已锁定容量",
    }));
  }

  function confirmHighImpactAction(title: string, content: string, action: () => Promise<void>) {
    Modal.confirm({
      title,
      content,
      okText: "确认",
      cancelText: "取消",
      onOk: action,
    });
  }

  async function handleRiskAlert(values: Record<string, string>) {
    await runAction("风险预警创建", () => postJson("/api/v1/analytics/risk-alerts", {
      ruleId: values.ruleId || undefined,
      alertType: values.alertType || "manual",
      riskLevel: values.riskLevel || "normal",
      objectType: values.objectType || "p0",
      objectId: values.objectId || undefined,
      summary: values.summary,
    }), () => riskForm.resetFields());
  }

  async function handleResolveRisk(alertId: string) {
    await runAction("风险处理", () => postJson(`/api/v1/analytics/risk-alerts/${alertId}/resolve`, { reason: "P0 页面处理" }));
  }

  async function handleWebhookReserved() {
    await runAction("第三方预留 Webhook", () => postJson("/api/open/d/p0-frontend-test/webhooks/device/p0.frontend-test", {
      requestId: `web-${Date.now()}`,
      payload: { source: "P0 web console", reserved: true },
    }));
  }

  async function handleCreateOpenApp(values: Record<string, string>) {
    await runAction("开放应用创建", () => postJson("/api/v1/integration/provider-adapters", {
      appName: values.appName,
      appType: values.appType || "reserved_partner",
      contactName: values.contactName || "P0 Contact",
      contactPhone: values.contactPhone || "18800000003",
      callbackUrl: values.callbackUrl || undefined,
      loginAccount: values.loginAccount,
      password: values.password,
    }), () => openAppForm.resetFields());
  }

  const interfaceColumns: ColumnsType<InterfaceItem> = [
    { title: "编码", dataIndex: "code", key: "code", width: 190 },
    { title: "名称", dataIndex: "name", key: "name", width: 220 },
    { title: "方法", dataIndex: "method", key: "method", width: 78, render: (value) => <Tag color="blue">{value}</Tag> },
    { title: "路径", dataIndex: "path", key: "path" },
    { title: "状态", dataIndex: "status", key: "status", width: 96, render: (value) => <Tag>{value}</Tag> },
  ];

  function renderDashboard(snapshotValue: Snapshot) {
    return (
      <>
        <section className="summary-grid">
          {metricItems(snapshotValue).map((item) => (
            <div className="metric" key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              <small>{item.hint}</small>
            </div>
          ))}
        </section>
        <BusinessEntryIndex activeSection={activeSection} snapshot={snapshotValue} onSelect={navigateToSection} />
        <WorkspaceMap activeSection={activeSection} snapshot={snapshotValue} onSelect={navigateToSection} />
        <section className="section-block operations-grid">
          <SectionHeading icon={<FieldTimeOutlined />} title="最新业务流" extra={<small>更新时间 {formatTime(snapshotValue.loadedAt)}</small>} />
          <div className="panel-grid">
            <LatestPanel icon={<QrcodeOutlined />} title="票务权益" rows={snapshotValue.tickets.slice(0, 5).map((ticket) => ({
              key: ticket.id,
              title: ticket.productName,
              meta: `${ticket.productCode} · ${formatMoney(ticket.priceAmount)}`,
              status: ticket.status,
            }))} />
            <LatestPanel icon={<CoffeeOutlined />} title="酒吧经营" rows={snapshotValue.barOrders.slice(0, 5).map((order) => ({
              key: order.id,
              title: order.orderNo,
              meta: `${formatMoney(order.totalAmount)} · ${order.items?.length ?? 0} 项`,
              status: order.status,
            }))} />
            <LatestPanel icon={<RocketOutlined />} title="游乐运营" rows={snapshotValue.amusementProjects.slice(0, 5).map((project) => ({
              key: project.id,
              title: project.projectName,
              meta: `${project.sessions?.length ?? 0} 场次 · 已用 ${project.sessions?.reduce((sum, session) => sum + session.usedCapacity, 0) ?? 0}`,
              status: project.status,
            }))} />
          </div>
        </section>
      </>
    );
  }

  function renderFoundation(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<BankOutlined />} title="平台地基操作" />
        <div className="action-grid two">
          <div className="action-panel">
            <h2>一键创建组织、商户、场馆、区域、点位</h2>
            <Form form={foundationForm} layout="vertical" onFinish={handleFoundationFlow} initialValues={{
              organizationName: "P0 Operation Org",
              merchantName: `P0 Merchant ${Date.now()}`,
              venueName: "P0 Venue",
              areaName: "P0 Area",
              pointName: "P0 Gate Point",
            }}>
              <div className="form-grid two">
                <Form.Item name="organizationName" label="组织名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="merchantName" label="商户名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="venueName" label="场馆名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="areaName" label="区域名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="pointName" label="点位名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="contactPhone" label="联系电话"><Input /></Form.Item>
              </div>
              <Button type="primary" icon={<PlusOutlined />} htmlType="submit">创建基础资料</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>创建员工账号</h2>
            <Form form={userForm} layout="vertical" onFinish={handleCreateUser} initialValues={{ accountType: "staff" }}>
              <div className="form-grid two">
                <Form.Item name="username" label="账号" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password /></Form.Item>
                <Form.Item name="phone" label="手机"><Input /></Form.Item>
                <Form.Item name="accountType" label="账号类型"><Select options={[{ value: "staff", label: "员工" }, { value: "platform_admin", label: "平台管理员" }]} /></Form.Item>
                <Form.Item name="organizationId" label="组织"><Select allowClear options={snapshotValue.organizations.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
                <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建账号</Button>
            </Form>
          </div>
        </div>
        <DataTables snapshotValue={snapshotValue} />
      </section>
    );
  }

  function renderTicketing(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<QrcodeOutlined />} title="票务、权益、出票、核销" />
        <div className="action-grid two">
          <div className="action-panel">
            <h2>创建票种并出票</h2>
            <Form form={ticketFlowForm} layout="vertical" onFinish={handleTicketFlow} initialValues={{
              productName: `P0 Web Pass ${Date.now()}`,
              entitlementName: "P0 Web Entry Entitlement",
              entitlementType: "entry",
              priceYuan: 99,
              totalTimes: 1,
              purchaserName: "P0 Visitor",
            }}>
              <div className="form-grid two">
                <Form.Item name="productName" label="票种名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="priceYuan" label="售价（元）" rules={[{ required: true }]}><InputNumber min={0} className="full" /></Form.Item>
                <Form.Item name="entitlementName" label="权益名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="entitlementType" label="权益类型"><Select options={["entry", "drink", "amusement", "coupon"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="totalTimes" label="可用次数"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="purchaserName" label="购买人"><Input /></Form.Item>
                <Form.Item name="purchaserPhone" label="手机号"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建并出票</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>核销预检与执行</h2>
            <Form form={verifyForm} layout="vertical">
              <Form.Item name="entitlementCode" label="权益码" rules={[{ required: true }]}>
                <Select
                  showSearch
                  optionFilterProp="label"
                  options={snapshotValue.issuedEntitlements.slice(0, 80).map((item) => ({
                    value: item.entitlementCode,
                    label: `${item.entitlementCode} · ${item.status} · ${item.usedTimes}/${item.totalTimes}`,
                  }))}
                />
              </Form.Item>
              <div className="button-row">
                <Button onClick={() => void handlePreview()}>核销预检</Button>
                <Button type="primary" onClick={() => void handleVerify()}>确认核销</Button>
              </div>
            </Form>
          </div>
          <div className="action-panel">
            <h2>生成结算单</h2>
            <Form form={settlementForm} layout="vertical" onFinish={handleSettlementGenerate}>
              <Form.Item name="merchantId" label="商户">
                <Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} />
              </Form.Item>
              <Button type="primary" htmlType="submit">生成待结算账单</Button>
            </Form>
          </div>
        </div>
        <div className="action-grid two">
          <div className="action-panel">
            <h2>P2 动态定价规则</h2>
            <Alert className="status-alert" type="info" showIcon message="仅用于规则容器和模拟，不改变当前正式订单实收。" />
            <Form form={dynamicPricingForm} layout="vertical" onFinish={handleDynamicPricingRule} initialValues={{ priority: 10, salesChannel: "web", adjustmentType: "percent_delta", adjustmentRate: 10, minPriceYuan: 0 }}>
              <div className="form-grid two">
                <Form.Item name="ticketTypeId" label="票种" rules={[{ required: true }]}><Select options={snapshotValue.tickets.map((item) => ({ value: item.id, label: `${item.productName} · ${formatMoney(item.priceAmount)}` }))} /></Form.Item>
                <Form.Item name="ruleName" label="规则名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="effectiveFrom" label="生效开始" rules={[{ required: true }]}><Input placeholder="2026-06-01" /></Form.Item>
                <Form.Item name="effectiveTo" label="生效结束" rules={[{ required: true }]}><Input placeholder="2026-06-30" /></Form.Item>
                <Form.Item name="salesChannel" label="渠道"><Select options={["admin", "kiosk", "miniapp", "app", "web", "channel"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="priority" label="优先级"><InputNumber className="full" /></Form.Item>
                <Form.Item name="adjustmentType" label="调整类型"><Select options={[{ value: "percent_delta", label: "百分比调整" }, { value: "fixed_delta", label: "固定金额调整" }]} /></Form.Item>
                <Form.Item name="adjustmentRate" label="百分比"><InputNumber className="full" /></Form.Item>
                <Form.Item name="adjustmentAmountYuan" label="固定金额（元）"><InputNumber className="full" /></Form.Item>
                <Form.Item name="minPriceYuan" label="最低价（元）"><InputNumber min={0} className="full" /></Form.Item>
                <Form.Item name="maxPriceYuan" label="最高价（元）"><InputNumber min={0} className="full" /></Form.Item>
                <Form.Item name="conditionKey" label="条件字段"><Input placeholder="inventory_level" /></Form.Item>
                <Form.Item name="conditionValue" label="条件值"><Input placeholder="low" /></Form.Item>
                <Form.Item name="operatorNote" label="备注"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建动态定价草稿</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>P2 动态定价模拟</h2>
            <Form form={dynamicPricingSimulationForm} layout="vertical" onFinish={handleDynamicPricingSimulation} initialValues={{ salesChannel: "web" }}>
              <div className="form-grid two">
                <Form.Item name="ticketTypeId" label="票种" rules={[{ required: true }]}><Select options={snapshotValue.tickets.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
                <Form.Item name="visitDate" label="游玩日期" rules={[{ required: true }]}><Input placeholder="2026-06-20" /></Form.Item>
                <Form.Item name="salesChannel" label="渠道"><Select options={["admin", "kiosk", "miniapp", "app", "web", "channel"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="conditionKey" label="条件字段"><Input placeholder="inventory_level" /></Form.Item>
                <Form.Item name="conditionValue" label="条件值"><Input placeholder="low" /></Form.Item>
              </div>
              <Button htmlType="submit">模拟价格</Button>
            </Form>
          </div>
        </div>
        <div className="table-grid two">
          <SimpleTable title="P2 动态定价规则" data={snapshotValue.ticketDynamicPricingRules.slice(0, 12)} columns={[
            { title: "规则", dataIndex: "ruleName" },
            { title: "渠道", dataIndex: "salesChannel", render: (value) => value || "全部" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "有效期", key: "period", render: (_, row) => `${row.effectiveFrom} 至 ${row.effectiveTo}` },
            { title: "调整", key: "adjustment", render: (_, row) => row.adjustmentType === "percent_delta" ? formatPercentPoint(row.adjustmentRate) : formatMoney(row.adjustmentAmountCent) },
            { title: "操作", key: "actions", render: (_, row) => row.status === "draft" ? <Button size="small" onClick={() => confirmHighImpactAction("发布动态定价规则", "仅发布规则容器和模拟能力，不改变当前正式订单实收。", () => handlePublishDynamicPricingRule(row.id))}>发布</Button> : <Tag>{row.status}</Tag> },
          ]} />
          <SimpleTable title="票种" data={snapshotValue.tickets.slice(0, 12)} columns={[
            { title: "名称", dataIndex: "productName" },
            { title: "价格", dataIndex: "priceAmount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="核销记录" data={snapshotValue.verifications.slice(0, 12)} columns={[
            { title: "核销号", dataIndex: "verificationNo" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="结算单" data={snapshotValue.settlementBills.slice(0, 12)} columns={[
            { title: "单号", dataIndex: "billNo" },
            { title: "金额", dataIndex: "totalAmount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
        </div>
      </section>
    );
  }

  function renderBar(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<CoffeeOutlined />} title="酒吧开台、点单、结账" />
        <div className="action-grid two">
          <div className="action-panel">
            <h2>一键完成酒吧闭环</h2>
            <Form form={barFlowForm} layout="vertical" onFinish={handleBarFlow} initialValues={{ quantity: 2, priceYuan: 30, minSpendYuan: 100, capacity: 4 }}>
              <div className="form-grid two">
                <Form.Item name="tableId" label="已有桌台"><Select allowClear options={snapshotValue.barTables.map((item) => ({ value: item.id, label: `${item.tableName} · ${item.status}` }))} /></Form.Item>
                <Form.Item name="tableName" label="新桌台名"><Input placeholder="不选已有桌台时创建" /></Form.Item>
                <Form.Item name="capacity" label="人数"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="minSpendYuan" label="低消（元）"><InputNumber min={0} className="full" /></Form.Item>
                <Form.Item name="productId" label="已有商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: `${item.productName} · ${formatMoney(item.priceAmount)}` }))} /></Form.Item>
                <Form.Item name="productName" label="新商品名"><Input placeholder="不选已有商品时创建" /></Form.Item>
                <Form.Item name="priceYuan" label="单价（元）"><InputNumber min={0} className="full" /></Form.Item>
                <Form.Item name="quantity" label="数量"><InputNumber min={1} className="full" /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">开台、点单、出账、支付</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>登记库存流水</h2>
            <Form form={inventoryForm} layout="vertical" onFinish={handleInventoryFlow} initialValues={{ flowType: "inbound", quantity: 10, reason: "P0 页面入库" }}>
              <div className="form-grid two">
                <Form.Item name="productId" label="商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
                <Form.Item name="flowType" label="类型"><Select options={["inbound", "consume", "loss", "gift"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="quantity" label="数量"><InputNumber className="full" /></Form.Item>
                <Form.Item name="reason" label="原因"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">登记库存</Button>
            </Form>
          </div>
        </div>
        <div className="action-grid two">
          <div className="action-panel">
            <h2>P1 库存物料</h2>
            <Form form={inventoryItemForm} layout="vertical" onFinish={handleInventoryItemCreate} initialValues={{ category: "drink", unit: "bottle", minQuantity: 0 }}>
              <div className="form-grid two">
                <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
                <Form.Item name="venueId" label="场馆"><Select allowClear options={snapshotValue.venues.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
                <Form.Item name="itemName" label="物料名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="category" label="分类"><Select options={["drink", "food", "consumable"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="unit" label="单位"><Select options={["bottle", "box", "piece", "ml", "kg"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="minQuantity" label="最低库存"><InputNumber min={0} className="full" /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建物料</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>P1 出入库、调拨、盘点</h2>
            <Form form={stockMovementForm} layout="vertical" onFinish={handleStockMovement} initialValues={{ movementType: "inbound", quantity: 10, locationType: "warehouse_stock", fromLocationType: "warehouse_stock", toLocationType: "bar_stock" }}>
              <div className="form-grid two">
                <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
                <Form.Item name="venueId" label="场馆"><Select allowClear options={snapshotValue.venues.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
                <Form.Item name="itemId" label="物料" rules={[{ required: true }]}><Select options={snapshotValue.barInventoryItems.map((item) => ({ value: item.id, label: `${item.itemName} · ${item.unit}` }))} /></Form.Item>
                <Form.Item name="productId" label="关联商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
                <Form.Item name="movementType" label="动作"><Select options={["inbound", "outbound", "transfer", "damage", "count"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="quantity" label="数量/盘点数"><InputNumber min={0} className="full" /></Form.Item>
                <Form.Item name="locationType" label="库位"><Select options={["warehouse_stock", "bar_stock"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="fromLocationType" label="调出"><Select options={["warehouse_stock", "bar_stock"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="toLocationType" label="调入"><Select options={["bar_stock", "warehouse_stock"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="reason" label="原因"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">提交库存动作</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>P1 存酒登记</h2>
            <Form form={savedBottleForm} layout="vertical" onFinish={handleSavedBottleCreate} initialValues={{ initialUnit: "percent", initialValue: 100 }}>
              <div className="form-grid two">
                <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
                <Form.Item name="venueId" label="场馆"><Select allowClear options={snapshotValue.venues.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
                <Form.Item name="productId" label="酒水商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
                <Form.Item name="customerName" label="客户" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="customerPhone" label="手机号"><Input /></Form.Item>
                <Form.Item name="storageLocation" label="存放位置"><Input /></Form.Item>
                <Form.Item name="initialUnit" label="计量"><Select options={["percent", "bottle", "ml"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="initialValue" label="初始余量"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="expiresAt" label="到期时间"><Input placeholder="2026-12-31T23:59:59.000Z" /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">登记存酒</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>P1 存酒取用</h2>
            <Form form={savedBottlePickupForm} layout="vertical" onFinish={handleSavedBottlePickup} initialValues={{ quantity: 10 }}>
              <div className="form-grid two">
                <Form.Item name="savedBottleId" label="存酒" rules={[{ required: true }]}><Select options={snapshotValue.savedBottles.map((item) => ({ value: item.id, label: `${item.savedNo} · ${item.customerName} · ${item.remainingValue}${item.remainingUnit}` }))} /></Form.Item>
                <Form.Item name="quantity" label="取用量"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="reason" label="原因"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">确认取酒</Button>
            </Form>
          </div>
        </div>
        <div className="table-grid two">
          <SimpleTable title="桌台" data={snapshotValue.barTables.slice(0, 12)} columns={[
            { title: "桌台", dataIndex: "tableName" },
            { title: "低消", dataIndex: "minSpendAmount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="订单" data={snapshotValue.barOrders.slice(0, 12)} columns={[
            { title: "订单号", dataIndex: "orderNo" },
            { title: "金额", dataIndex: "totalAmount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="库存流水" data={snapshotValue.barInventoryFlows.slice(0, 12)} columns={[
            { title: "商品", dataIndex: ["product", "productName"] },
            { title: "类型", dataIndex: "flowType" },
            { title: "数量", dataIndex: "quantity" },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="P1 物料" data={snapshotValue.barInventoryItems.slice(0, 12)} columns={[
            { title: "物料", dataIndex: "itemName" },
            { title: "分类", dataIndex: "category" },
            { title: "单位", dataIndex: "unit" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 库存余额" data={snapshotValue.barStockBalances.slice(0, 12)} columns={[
            { title: "物料", dataIndex: ["item", "itemName"] },
            { title: "库位", dataIndex: "locationType" },
            { title: "数量", dataIndex: "quantity" },
          ]} />
          <SimpleTable title="P1 库存动作" data={snapshotValue.barStockMovements.slice(0, 12)} columns={[
            { title: "物料", dataIndex: ["item", "itemName"] },
            { title: "动作", dataIndex: "movementType" },
            { title: "数量", dataIndex: "quantity" },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="P1 存酒" data={snapshotValue.savedBottles.slice(0, 12)} columns={[
            { title: "编号", dataIndex: "savedNo" },
            { title: "客户", dataIndex: "customerName" },
            { title: "余量", key: "remaining", render: (_, row) => `${row.remainingValue}${row.remainingUnit}` },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 出品票" data={snapshotValue.barProductionTickets.slice(0, 12)} columns={[
            { title: "出品号", dataIndex: "ticketNo" },
            { title: "部门", dataIndex: "department" },
            { title: "数量", dataIndex: "quantity" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 酒吧预订" data={snapshotValue.barReservations.slice(0, 12)} columns={[
            { title: "预订号", dataIndex: "reservationNo" },
            { title: "客户", dataIndex: "customerName" },
            { title: "人数", dataIndex: "partySize" },
            { title: "订金", dataIndex: "depositAmount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
        </div>
      </section>
    );
  }

  function renderAmusement(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<RocketOutlined />} title="游乐项目、场次、放行" />
        <div className="action-grid two">
          <div className="action-panel">
            <h2>创建项目和场次</h2>
            <Form form={amusementFlowForm} layout="vertical" onFinish={handleAmusementFlow} initialValues={{ projectName: `P0 Web Project ${Date.now()}`, sessionName: "P0 Web Session", capacity: 20, sessionCapacity: 10 }}>
              <div className="form-grid two">
                <Form.Item name="projectName" label="项目名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="capacity" label="项目容量"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="sessionName" label="场次名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="sessionCapacity" label="场次容量"><InputNumber min={1} className="full" /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建项目和场次</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>核销或人工放行</h2>
            <Form form={amusementVerifyForm} layout="vertical" onFinish={handleAmusementVerify} initialValues={{ manualReason: "P0 页面人工放行" }}>
              <Form.Item name="sessionId" label="场次" rules={[{ required: true }]}>
                <Select options={snapshotValue.amusementSessions.map((item) => ({ value: item.id, label: `${item.sessionName} · ${item.usedCapacity}/${item.capacity}` }))} />
              </Form.Item>
              <Form.Item name="entitlementCode" label="权益码"><Input placeholder="为空时按人工放行" /></Form.Item>
              <Form.Item name="manualReason" label="人工原因"><Input /></Form.Item>
              <Button type="primary" htmlType="submit">执行放行</Button>
            </Form>
          </div>
        </div>
        <div className="action-grid two">
          <div className="action-panel">
            <h2>P2 团体/包场容量申请</h2>
            <Alert className="status-alert" type="info" showIcon message="审批时由后端锁定容量；前端只展示结果，不自行裁决可锁定。" />
            <Form form={amusementGroupBookingForm} layout="vertical" onFinish={handleAmusementGroupBooking} initialValues={{ bookingType: "group", partySize: 2, reservedCapacity: 2 }}>
              <div className="form-grid two">
                <Form.Item name="sessionId" label="场次" rules={[{ required: true }]}><Select options={snapshotValue.amusementSessions.map((item) => ({ value: item.id, label: `${item.sessionName} · 已用 ${item.usedCapacity} · 锁定 ${item.lockedCapacity ?? 0} / ${item.capacity}` }))} /></Form.Item>
                <Form.Item name="bookingType" label="类型"><Select options={[{ value: "group", label: "团体" }, { value: "private", label: "包场" }]} /></Form.Item>
                <Form.Item name="customerName" label="客户" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="contactPhone" label="联系电话"><Input /></Form.Item>
                <Form.Item name="partySize" label="人数"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="reservedCapacity" label="锁定容量"><InputNumber min={1} className="full" /></Form.Item>
                <Form.Item name="operatorNote" label="备注"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建待审批申请</Button>
            </Form>
          </div>
        </div>
        <div className="table-grid two">
          <SimpleTable title="P2 团体/包场" data={snapshotValue.amusementGroupBookings.slice(0, 12)} columns={[
            { title: "客户", dataIndex: "customerName" },
            { title: "类型", dataIndex: "bookingType" },
            { title: "人数", dataIndex: "partySize" },
            { title: "锁定容量", dataIndex: "reservedCapacity" },
            { title: "状态", dataIndex: "bookingStatus", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "操作", key: "actions", render: (_, row) => <div className="button-row compact">{row.bookingStatus === "pending_approval" ? <Button size="small" onClick={() => confirmHighImpactAction("审批并锁定容量", "审批后由后端事务锁定该场次容量。", () => handleApproveGroupBooking(row.id))}>审批锁定</Button> : null}{row.bookingStatus === "approved" ? <Button size="small" onClick={() => confirmHighImpactAction("释放已锁定容量", "释放后该申请占用的场次容量会被归还。", () => handleReleaseGroupBooking(row.id))}>释放</Button> : null}</div> },
          ]} />
          <SimpleTable title="P2 场次容量池" data={snapshotValue.amusementSessions.slice(0, 12)} columns={[
            { title: "场次", dataIndex: "sessionName" },
            { title: "已用", dataIndex: "usedCapacity" },
            { title: "锁定", dataIndex: "lockedCapacity", render: (value) => value ?? 0 },
            { title: "容量", dataIndex: "capacity" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="游乐项目" data={snapshotValue.amusementProjects.slice(0, 12)} columns={[
            { title: "项目", dataIndex: "projectName" },
            { title: "容量", dataIndex: "capacity" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="游乐放行记录" data={snapshotValue.amusementVerifications.slice(0, 12)} columns={[
            { title: "编号", dataIndex: "verificationNo" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="P1 预约" data={snapshotValue.amusementReservations.slice(0, 12)} columns={[
            { title: "预约号", dataIndex: "reservationNo" },
            { title: "客户", dataIndex: "customerName" },
            { title: "人数", dataIndex: "reservedCapacity" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 排队" data={snapshotValue.amusementQueueEntries.slice(0, 12)} columns={[
            { title: "排队号", dataIndex: "queueNo" },
            { title: "客户", dataIndex: "customerName" },
            { title: "人数", dataIndex: "partySize" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 设备状态" data={snapshotValue.amusementDeviceStatuses.slice(0, 12)} columns={[
            { title: "设备", dataIndex: "deviceName" },
            { title: "编码", dataIndex: "deviceCode" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 巡检与故障" data={snapshotValue.amusementFaults.slice(0, 12)} columns={[
            { title: "故障号", dataIndex: "faultNo" },
            { title: "级别", dataIndex: "faultLevel" },
            { title: "摘要", dataIndex: "summary" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
        </div>
      </section>
    );
  }

  function renderAnalytics(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<BarChartOutlined />} title="经营看板" />
        <div className="summary-grid compact">
          {metricItems(snapshotValue).map((item) => <div className="metric" key={item.label}><span>{item.label}</span><strong>{item.value}</strong><small>{item.hint}</small></div>)}
        </div>
        <SimpleTable title="商户经营" data={snapshotValue.merchants.map((row, index) => ({ key: row.merchant?.id ?? index, name: row.merchant?.displayName ?? "未命名商户", status: row.merchant?.status ?? "-", risk: row.merchant?.riskLevel ?? "normal", verifications: row.verifications, barOrders: row.barOrders, settlement: row.settlementAmount?._sum?.amount ?? 0 }))} columns={[
          { title: "商户", dataIndex: "name" },
          { title: "核销", dataIndex: "verifications" },
          { title: "酒吧订单", dataIndex: "barOrders" },
          { title: "结算金额", dataIndex: "settlement", render: formatMoney },
          { title: "风险", dataIndex: "risk", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <div className="summary-grid compact">
          <div className="metric"><span>P3 机器人任务</span><strong>{snapshotValue.robotAnalytics.total}</strong><small>完成率 {formatRate(snapshotValue.robotAnalytics.completionRate)}</small></div>
          <div className="metric"><span>P3 机器人异常</span><strong>{snapshotValue.robotAnalytics.abnormal}</strong><small>异常率 {formatRate(snapshotValue.robotAnalytics.abnormalRate)}</small></div>
          <div className="metric"><span>P3 AI质检</span><strong>{snapshotValue.aiQualityResults.length}</strong><small>任务 {snapshotValue.aiQualityTasks.length}</small></div>
          <div className="metric"><span>P3 客户意图</span><strong>{snapshotValue.customerIntents.length}</strong><small>企微需求识别</small></div>
        </div>
        <div className="table-grid two">
          <SimpleTable title="P2 经营指标快照" data={snapshotValue.businessMetricSnapshots.slice(0, 12).map((row) => ({ ...row, key: row.snapshotId }))} columns={[
            { title: "指标", dataIndex: "metricKey" },
            { title: "领域", dataIndex: "domain" },
            { title: "日期", dataIndex: "businessDate" },
            { title: "实际值", key: "actual", render: (_, row) => formatMetricValue(row.metricKey, row.actualValue) },
            { title: "基准", key: "baseline", render: (_, row) => formatMetricValue(row.metricKey, row.baselineValue) },
            { title: "变化", dataIndex: "varianceRate", render: formatPercentPoint },
            { title: "趋势", dataIndex: "trendDirection", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P3 指标字典" data={snapshotValue.metricDefinitions.slice(0, 12)} columns={[
            { title: "编码", dataIndex: "metricCode" },
            { title: "名称", dataIndex: "metricName" },
            { title: "分类", dataIndex: "category" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P3 AI任务" data={snapshotValue.aiQualityTasks.slice(0, 12)} columns={[
            { title: "任务号", dataIndex: "taskNo" },
            { title: "类型", dataIndex: "taskType" },
            { title: "对象", dataIndex: "objectType" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P3 AI质检结果" data={snapshotValue.aiQualityResults.slice(0, 12)} columns={[
            { title: "来源", dataIndex: "sourceType" },
            { title: "对象", dataIndex: "objectType" },
            { title: "评分", dataIndex: "score" },
            { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "摘要", dataIndex: "summary" },
          ]} />
          <SimpleTable title="P3 客户意图" data={snapshotValue.customerIntents.slice(0, 12)} columns={[
            { title: "类型", dataIndex: "intentType" },
            { title: "标签", dataIndex: "label" },
            { title: "置信度", dataIndex: "confidence" },
            { title: "状态", dataIndex: "status" },
          ]} />
          <SimpleTable title="P3 员工服务评分" data={snapshotValue.staffServiceScores.slice(0, 12)} columns={[
            { title: "员工", dataIndex: "staffUserId" },
            { title: "评分", dataIndex: "score" },
            { title: "风险扣分", dataIndex: "riskPenalty" },
            { title: "样本", dataIndex: "sampleSize" },
          ]} />
          <SimpleTable title="P3 商户健康评分" data={snapshotValue.merchantHealthScores.slice(0, 12)} columns={[
            { title: "商户ID", dataIndex: "merchantId" },
            { title: "总分", dataIndex: "score" },
            { title: "经营", dataIndex: "operationsScore" },
            { title: "财务", dataIndex: "financeScore" },
            { title: "风险", dataIndex: "riskScore" },
          ]} />
          <SimpleTable title="P3 指标异常解释" data={snapshotValue.metricAnomalyExplanations.slice(0, 12)} columns={[
            { title: "指标", dataIndex: "metricName" },
            { title: "变化", dataIndex: "changeRate", render: formatRate },
            { title: "级别", dataIndex: "severity", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "解释", dataIndex: "explanation" },
          ]} />
        </div>
      </section>
    );
  }

  function renderInterfaces(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<ApiOutlined />} title="第三方预留接口与开放应用" extra={<Button onClick={() => void handleWebhookReserved()}>发送预留 Webhook</Button>} />
        <div className="action-grid two">
          <div className="action-panel">
            <h2>接口目录</h2>
            <Table className="data-table" columns={interfaceColumns} dataSource={snapshotValue.integration.interfaces} rowKey="code" pagination={false} size="small" />
          </div>
          <div className="action-panel">
            <h2>创建开放应用</h2>
            <Form form={openAppForm} layout="vertical" onFinish={handleCreateOpenApp} initialValues={{ appName: `P0 Reserved App ${Date.now()}`, appType: "reserved_partner", loginAccount: `p0_app_${Date.now()}` }}>
              <div className="form-grid two">
                <Form.Item name="appName" label="应用名称" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="appType" label="类型"><Input /></Form.Item>
                <Form.Item name="loginAccount" label="登录账号" rules={[{ required: true }]}><Input /></Form.Item>
                <Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password /></Form.Item>
                <Form.Item name="callbackUrl" label="回调地址"><Input placeholder="https://example.com/callback" /></Form.Item>
                <Form.Item name="contactName" label="联系人"><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建应用</Button>
            </Form>
          </div>
        </div>
        <SimpleTable title="开放应用" data={snapshotValue.openApps.slice(0, 12)} columns={[
          { title: "应用", dataIndex: "appName" },
          { title: "编码", dataIndex: "appCode" },
          { title: "类型", dataIndex: "appType" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <div className="table-grid two">
          <SimpleTable title="P1 餐饮/零售订单同步" data={snapshotValue.thirdPartyOrderSyncs.slice(0, 12)} columns={[
            { title: "业务", dataIndex: "businessType" },
            { title: "类型", dataIndex: "syncType" },
            { title: "外部单号", dataIndex: "externalBusinessId" },
            { title: "金额", dataIndex: "amount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 券核销同步" data={snapshotValue.thirdPartyVoucherVerifications.slice(0, 12)} columns={[
            { title: "业务", dataIndex: "businessType" },
            { title: "外部核销号", dataIndex: "externalVerificationId" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="P1 重试任务" data={snapshotValue.integrationRetryTasks.slice(0, 12)} columns={[
            { title: "业务", dataIndex: "businessType" },
            { title: "任务", dataIndex: "taskType" },
            { title: "次数", dataIndex: "attemptCount" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 补偿任务" data={snapshotValue.integrationCompensationTasks.slice(0, 12)} columns={[
            { title: "业务", dataIndex: "businessType" },
            { title: "任务", dataIndex: "taskType" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="P1 对账批次" data={snapshotValue.reconciliationBatches.slice(0, 12)} columns={[
            { title: "业务", dataIndex: "businessType" },
            { title: "日期", dataIndex: "reconDate" },
            { title: "差异数", dataIndex: "diffCount" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P1 对账差异" data={snapshotValue.reconciliationDifferences.slice(0, 12)} columns={[
            { title: "业务", dataIndex: "businessType" },
            { title: "类型", dataIndex: "diffType" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "createdAt", render: formatTime },
          ]} />
          <SimpleTable title="P2 零售 SKU" data={snapshotValue.integrationSkus.slice(0, 12)} columns={[
            { title: "编码", dataIndex: "skuCode" },
            { title: "名称", dataIndex: "skuName" },
            { title: "分类", dataIndex: "category" },
            { title: "价格", dataIndex: "priceAmount", render: formatMoney },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 机器人任务" data={snapshotValue.robotTasks.slice(0, 12)} columns={[
            { title: "任务号", dataIndex: "taskNo" },
            { title: "类型", dataIndex: "taskType" },
            { title: "机器人", dataIndex: "assignedRobotCode" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 机器人事件" data={snapshotValue.robotTaskEvents.slice(0, 12)} columns={[
            { title: "事件", dataIndex: "eventType" },
            { title: "状态", dataIndex: "taskStatus", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "occurredAt", render: formatTime },
          ]} />
          <SimpleTable title="P2 摄像头" data={snapshotValue.cameraDevices.slice(0, 12)} columns={[
            { title: "编码", dataIndex: "cameraCode" },
            { title: "名称", dataIndex: "cameraName" },
            { title: "服务商", dataIndex: "provider" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 视频证据" data={snapshotValue.videoEvidenceLinks.slice(0, 12)} columns={[
            { title: "证据号", dataIndex: "evidenceNo" },
            { title: "事件", dataIndex: "eventType" },
            { title: "对象", dataIndex: "objectType" },
            { title: "调阅", dataIndex: "viewCount" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 企微绑定" data={snapshotValue.wecomBindings.slice(0, 12)} columns={[
            { title: "企业", dataIndex: "corpId" },
            { title: "员工", dataIndex: "staffUserId" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 企微会话" data={snapshotValue.wecomConversations.slice(0, 12)} columns={[
            { title: "会话", dataIndex: "conversationKey" },
            { title: "员工", dataIndex: "staffUserId" },
            { title: "客户", dataIndex: "customerName" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 企微消息" data={snapshotValue.wecomMessages.slice(0, 12)} columns={[
            { title: "消息", dataIndex: "msgId" },
            { title: "发送方", dataIndex: "senderType" },
            { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "sentAt", render: formatTime },
          ]} />
          <SimpleTable title="P2 企微风险" data={snapshotValue.wecomRiskHits.slice(0, 12)} columns={[
            { title: "类型", dataIndex: "riskType" },
            { title: "关键词", dataIndex: "keyword" },
            { title: "级别", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 接入设备" data={snapshotValue.deviceIntegrationDevices.slice(0, 12)} columns={[
            { title: "编码", dataIndex: "deviceCode" },
            { title: "类型", dataIndex: "deviceType" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
          <SimpleTable title="P2 设备事件" data={snapshotValue.deviceIntegrationEvents.slice(0, 12)} columns={[
            { title: "设备", dataIndex: "deviceCode" },
            { title: "事件", dataIndex: "eventType" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
            { title: "时间", dataIndex: "occurredAt", render: formatTime },
          ]} />
          <SimpleTable title="P2 离线同步批次" data={snapshotValue.deviceOfflineSyncBatches.slice(0, 12)} columns={[
            { title: "批次", dataIndex: "batchNo" },
            { title: "总数", dataIndex: "totalCount" },
            { title: "拒绝", dataIndex: "rejectedCount" },
            { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          ]} />
        </div>
      </section>
    );
  }

  function renderSecurity(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<SafetyCertificateOutlined />} title="风控预警" />
        <div className="action-grid two">
          <div className="action-panel">
            <h2>创建风险预警</h2>
            <Form form={riskForm} layout="vertical" onFinish={handleRiskAlert} initialValues={{ riskLevel: "normal", alertType: "manual", summary: "P0 页面人工风险预警" }}>
              <div className="form-grid two">
                <Form.Item name="ruleId" label="规则"><Select allowClear options={snapshotValue.riskRules.map((item) => ({ value: item.id, label: item.ruleName }))} /></Form.Item>
                <Form.Item name="riskLevel" label="等级"><Select options={["normal", "suspicious", "high"].map((value) => ({ value, label: value }))} /></Form.Item>
                <Form.Item name="alertType" label="类型"><Input /></Form.Item>
                <Form.Item name="objectType" label="对象类型"><Input /></Form.Item>
                <Form.Item name="summary" label="摘要" rules={[{ required: true }]}><Input /></Form.Item>
              </div>
              <Button type="primary" htmlType="submit">创建预警</Button>
            </Form>
          </div>
          <div className="action-panel">
            <h2>未处理预警</h2>
            <div className="record-list">
              {snapshotValue.riskAlerts.slice(0, 10).map((alert) => (
                <div className="record-row" key={alert.id}>
                  <div><strong>{alert.summary}</strong><small>{alert.alertType} · {formatTime(alert.createdAt)}</small></div>
                  <div className="row-actions"><Tag color={statusColor(alert.riskLevel)}>{alert.riskLevel}</Tag>{alert.status === "open" ? <Button size="small" onClick={() => void handleResolveRisk(alert.id)}>处理</Button> : <Tag>{alert.status}</Tag>}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    );
  }

  function renderEvents(snapshotValue: Snapshot) {
    return (
      <section className="section-block">
        <SectionHeading icon={<AuditOutlined />} title="事件留痕" />
        <SimpleTable title="最近事件" data={snapshotValue.events} columns={[
          { title: "类型", dataIndex: "eventType" },
          { title: "分类", dataIndex: "eventCategory" },
          { title: "摘要", dataIndex: "summary" },
          { title: "结果", dataIndex: "result", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "occurredAt", render: formatTime },
        ]} />
      </section>
    );
  }

  function renderContent(snapshotValue: Snapshot) {
    let sectionContent: React.ReactNode = renderDashboard(snapshotValue);
    if (activeSection === "foundation") sectionContent = renderFoundation(snapshotValue);
    if (activeSection === "ticketing") sectionContent = renderTicketing(snapshotValue);
    if (activeSection === "bar") sectionContent = renderBar(snapshotValue);
    if (activeSection === "amusement") sectionContent = renderAmusement(snapshotValue);
    if (activeSection === "analytics") sectionContent = renderAnalytics(snapshotValue);
    if (activeSection === "interfaces") sectionContent = renderInterfaces(snapshotValue);
    if (activeSection === "security") sectionContent = renderSecurity(snapshotValue);
    if (activeSection === "events") sectionContent = renderEvents(snapshotValue);

    return (
      <>
        <PageHeader
          activeSection={activeSection}
          loadedAt={snapshotValue.loadedAt}
          stats={getSectionStats(activeSection, snapshotValue)}
          healthOk={snapshotValue.health.ok}
          onSectionChange={navigateToSection}
        />
        {sectionContent}
      </>
    );
  }

  const activeMeta = sectionMeta[activeSection];
  const activeGroup = findNavigationGroup(activeSection);

  function handleSidebarToggle() {
    const nextValue = !sidebarCollapsed;
    if (!nextValue && openMenuKeys.length === 0) setOpenMenuKeys(defaultOpenMenuKeys);
    setSidebarCollapsed(nextValue);
  }

  if (!authUser) {
    return (
      <ConfigProvider theme={{ algorithm: theme.defaultAlgorithm, token: { colorPrimary: "#0f766e", borderRadius: 6, fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif" } }}>
        <Layout className="app-shell auth-shell">
          <Content className="auth-content">
            <div className="auth-panel">
              <div className="brand auth-brand"><BankOutlined /><span>综合体管理系统</span></div>
              {error ? <Alert className="status-alert" type="error" message="登录失败" description={error} /> : null}
              <Form form={loginForm} layout="vertical" onFinish={handleLogin}>
                <Form.Item name="username" label="账号" rules={[{ required: true }]}><Input prefix={<LockOutlined />} autoComplete="username" /></Form.Item>
                <Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password autoComplete="current-password" /></Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block>登录</Button>
              </Form>
            </div>
          </Content>
        </Layout>
      </ConfigProvider>
    );
  }

  return (
    <ConfigProvider theme={{ algorithm: theme.defaultAlgorithm, token: { colorPrimary: "#0f766e", borderRadius: 6, fontFamily: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif" } }}>
      <Layout className="app-shell">
        <Sider width={248} collapsedWidth={72} collapsed={sidebarCollapsed} className={`sidebar${sidebarCollapsed ? " sidebar-collapsed" : ""}`}>
          <div className="brand"><BankOutlined />{sidebarCollapsed ? null : <span>综合体管理系统</span>}</div>
          <Menu
            mode="inline"
            selectedKeys={[activeSection]}
            openKeys={openMenuKeys}
            onOpenChange={(keys) => {
              if (!sidebarCollapsed) setOpenMenuKeys(keys);
            }}
            onClick={({ key }) => navigateToSection(key as SectionKey)}
            items={menuItems}
          />
        </Sider>
        <Layout className="main-layout">
          <Header className="topbar">
            <div className="topbar-left">
              <Button
                className="icon-button"
                icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={handleSidebarToggle}
                aria-label={sidebarCollapsed ? "展开导航" : "收起导航"}
              />
              <div className="topbar-title"><strong>{activeMeta.title}</strong><span>{activeGroup.label} · {apiBaseUrl || "同源 API"}</span></div>
            </div>
            <div className="topbar-actions">
              <span className="user-chip">{authUser.username}</span>
              <Badge status={snapshot?.health.ok ? "success" : "warning"} text={snapshot?.health.ok ? "API 正常" : "等待数据"} />
              <Button icon={<ReloadOutlined />} loading={loading} onClick={() => void loadData()}>刷新</Button>
            </div>
          </Header>
          <Content className="content">
            {error ? <Alert className="status-alert" type="error" message="后端数据读取失败" description={error} /> : null}
            {loading && !snapshot ? <div className="loading-state"><Spin size="large" /></div> : null}
            {snapshot ? renderContent(snapshot) : null}
            <ResultBox value={lastResult} />
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
}

createRoot(document.getElementById("root")!).render(<React.StrictMode><App /></React.StrictMode>);
