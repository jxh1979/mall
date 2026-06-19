import {
  ApiOutlined,
  AuditOutlined,
  BankOutlined,
  BarChartOutlined,
  CoffeeOutlined,
  DashboardOutlined,
  FieldTimeOutlined,
  QrcodeOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { Badge, Breadcrumb, Segmented, Tag } from "antd";
import type { MenuProps } from "antd";
import React from "react";
import { formatMoney, formatTime, statusColor } from "./formatters";
import type { Snapshot } from "./types";
import { SectionHeading } from "./ui";

export type SectionKey = 'dashboard' | 'foundation' | 'floorplan' | 'ticketing' | 'bar' | 'amusement' | 'analytics' | 'interfaces' | 'security' | 'events';
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

export const sectionMeta: Record<SectionKey, SectionMeta> = {
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
  floorplan: {
    title: 'P2 平面图导入',
    menuLabel: '商场平面图',
    description: '图片底图 + 多边形坐标，支持 SVG/标准 JSON 导入候选，人工确认后发布。',
    groupKey: 'platform',
    groupLabel: '平台治理',
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
    title: "T3-A 只读经营分析",
    menuLabel: "T3-A 分析",
    description: "跨业务经营趋势、风险摘要和指标口径，只读查看。",
    groupKey: "insight",
    groupLabel: "分析决策",
    icon: <BarChartOutlined />,
  },
};

const navigationGroups: Array<{ key: NavigationGroupKey; label: string; icon: React.ReactNode; children: SectionKey[] }> = [
  { key: "overview", label: "总览", icon: <DashboardOutlined />, children: ["dashboard"] },
  { key: "operations", label: "业务运营", icon: <CoffeeOutlined />, children: ["ticketing", "bar", "amusement"] },
  { key: "platform", label: "平台治理", icon: <BankOutlined />, children: ["foundation", "floorplan", "interfaces", "security", "events"] },
  { key: "insight", label: "分析决策", icon: <BarChartOutlined />, children: ["analytics"] },
];

const fallbackNavigationGroup = navigationGroups[0]!;
export const defaultOpenMenuKeys = navigationGroups.map((group) => group.key);

const sectionQueryValues = new Set<SectionKey>(['dashboard', 'foundation', 'floorplan', 'ticketing', 'bar', 'amusement', 'analytics', 'interfaces', 'security', 'events']);
const businessEntryPaths: Record<BusinessEntryKey, string> = {
  ticketing: "/ticketing/",
  bar: "/bar/",
  amusement: "/amusement/",
};

function normalizeSection(value: string | null | undefined): SectionKey | null {
  if (!value) return null;
  return sectionQueryValues.has(value as SectionKey) ? (value as SectionKey) : null;
}

// legacy 已退役：仅在显式 /legacy 前缀下进入（main.tsx 分流）。
// 这里保留前缀，避免导航后 URL 丢失 /legacy 而在刷新时落入默认的 admin 壳。
const LEGACY_PATH_PREFIX = "/legacy";

function legacyBasePath(): string {
  return window.location.pathname.startsWith(LEGACY_PATH_PREFIX) ? LEGACY_PATH_PREFIX : "/";
}

export function getInitialSectionFromLocation(): SectionKey {
  const url = new URL(window.location.href);
  const page = normalizeSection(url.searchParams.get("page"));
  if (page) return page;
  const hash = normalizeSection(url.hash.replace(/^#\/?/, ""));
  if (hash) return hash;
  const pathSection = normalizeSection(
    url.pathname.replace(new RegExp(`^${LEGACY_PATH_PREFIX}`), "").replace(/^\/+|\/+$/g, ""),
  );
  return pathSection ?? "dashboard";
}

export function sectionUrl(section: SectionKey) {
  const base = legacyBasePath();
  if (section === "dashboard") return base;
  return `${base}?page=${section}`;
}

export const menuItems: MenuProps["items"] = navigationGroups.map((group) => ({
  key: group.key,
  icon: group.icon,
  label: group.label,
  children: group.children.map((section) => ({
    key: section,
    icon: sectionMeta[section].icon,
    label: sectionMeta[section].menuLabel,
  })),
}));

function metricValue(snapshot: Snapshot, metricCode: string): number | string {
  const metric = snapshot.overview.metrics?.[metricCode];
  if (!metric || metric.value === null || metric.value === undefined) {
    return "—";
  }
  return metric.value;
}

function metricItems(snapshot: Snapshot) {
  const { integration } = snapshot;
  return [
    { label: "票务收入", value: metricValue(snapshot, "revenue.ticket"), hint: "已支付实付（分）" },
    { label: "酒吧收入", value: metricValue(snapshot, "revenue.bar"), hint: "已结算实收（分）" },
    { label: "票务核销", value: metricValue(snapshot, "verify.done"), hint: "成功核销" },
    { label: "游乐核销", value: metricValue(snapshot, "traffic.amusement"), hint: "成功核销人次" },
    { label: "新增会员", value: metricValue(snapshot, "member.new"), hint: "当日新建" },
    { label: "第三方接口", value: integration.interfaces.length, hint: "接口目录" },
    { label: "待复核", value: metricValue(snapshot, "risk.pending_review"), hint: "复核 + 风险预警" },
  ];
}

export type PageStat = {
  label: string;
  value: string | number;
  tone?: "default" | "good" | "warning" | "danger";
};

function openRiskCount(snapshot: Snapshot) {
  return snapshot.riskAlerts.filter((item) => !["closed", "resolved", "reviewed"].includes(item.status)).length;
}

export function getSectionStats(section: SectionKey, snapshot: Snapshot): PageStat[] {
  const activeInterfaces = snapshot.integration.interfaces.filter((item) => item.status !== "reserved").length;
  const highRiskAlerts = snapshot.riskAlerts.filter((item) => item.riskLevel === "high" || item.riskLevel === "critical").length;

  const stats: Record<SectionKey, PageStat[]> = {
    dashboard: [
      { label: "API", value: snapshot.health.ok ? "正常" : "异常", tone: snapshot.health.ok ? "good" : "danger" },
      { label: "待处理风险", value: openRiskCount(snapshot), tone: openRiskCount(snapshot) > 0 ? "warning" : "good" },
      { label: "待复核", value: metricValue(snapshot, "risk.pending_review") },
      { label: "已启用接口", value: activeInterfaces },
    ],
    foundation: [
      { label: "组织", value: snapshot.organizations.length },
      { label: "商户", value: snapshot.merchantsRaw.length },
      { label: "场馆", value: snapshot.venues.length },
      { label: "账号", value: snapshot.users.length },
    ],
    floorplan: [
      { label: '平面图', value: snapshot.floorPlans.length },
      { label: '区域点位', value: snapshot.floorPlanShapes.length },
      { label: '导入任务', value: snapshot.floorPlanImports.length },
      { label: '待发布候选', value: snapshot.floorPlanImportDetails?.candidates.filter((item) => item.status === 'pending').length ?? 0 },
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
      { label: "T3-A 指标", value: snapshot.businessMetricSnapshots.length },
      { label: "商户健康", value: snapshot.merchantHealthScores.length },
      { label: "员工评分", value: snapshot.staffServiceScores.length },
      { label: "只读异常", value: snapshot.metricAnomalies.length },
    ],
    interfaces: [
      { label: "接口目录", value: snapshot.integration.interfaces.length },
      { label: "\u4f9b\u5e94\u5546\u9002\u914d\u5668", value: snapshot.providerAdapters.length },
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

export function findNavigationGroup(section: SectionKey) {
  return navigationGroups.find((group) => group.children.includes(section)) ?? fallbackNavigationGroup;
}

export function PageHeader({
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

export function DashboardPage({ activeSection, snapshotValue, onSelect }: { activeSection: SectionKey; snapshotValue: Snapshot; onSelect: (section: SectionKey) => void }) {
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
      <BusinessEntryIndex activeSection={activeSection} snapshot={snapshotValue} onSelect={onSelect} />
      <WorkspaceMap activeSection={activeSection} snapshot={snapshotValue} onSelect={onSelect} />
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
