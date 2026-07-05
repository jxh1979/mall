import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, GovernanceSpaceRow, MerchantAdmissionRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";

const TEXT = {
  title: "集团/综合体拓扑",
  description: "展示集团、综合体、商户、项目和空间点位之间的治理关系，作为权限、合同、结算和运营责任归属的入口。",
  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces。",
  boundary: "集团、综合体、商户、项目和空间点位必须用同一套拓扑口径承接权限、合同、结算和审计；拓扑页只展示关系，不做主体变更。",
  loadFailed: "治理拓扑读取失败",
  focusClear: "清除筛选，返回全部",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<merchants|spaces>；点 KPI 数字=带参重进本页只渲染对应表。
// 经营主体/项目无独立承载表（并入主体-商户-项目合并表），其 KPI 数字保持纯文本不可点。
type KpiTarget = "merchants" | "spaces";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "merchants" && table !== "spaces") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  return focus.table === "spaces" ? "空间点位" : "商户";
}

export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, navigateTo }: AdminPageProps) {
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [nextEntities, nextMerchants, nextProjects, nextSpaces] = await Promise.all([
        api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities"),
        api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants"),
        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
        api.get<GovernanceSpaceRow[]>("/api/v1/platform/spaces"),
      ]);
      setEntities(nextEntities);
      setMerchants(nextMerchants);
      setProjects(nextProjects);
      setSpaces(nextSpaces);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const projectIds = new Set(projects.map((project) => project.id));
  const attachedSpaces = spaces.filter((space) => space.venueId && projectIds.has(space.venueId)).length;

  // 下钻定位:从别的页点商户/综合体进来(?focus=id)→ 滚动到拓扑中对应节点行(商户或项目)并高亮;无 focus 时不动。
  React.useEffect(() => {
    if (!focusObjectId || (merchants.length === 0 && spaces.length === 0)) return;
    const el = typeof document !== "undefined"
      ? document.querySelector(`[data-focus-node="${focusObjectId}"]`)
      : null;
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView({ block: "center" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusObjectId, merchants.length, projects.length, spaces.length]);

  // 一行是否命中下钻焦点(商户 id 或项目 id)。
  const isFocused = (merchantId?: string, projectId?: string) =>
    !!focusObjectId && (focusObjectId === merchantId || focusObjectId === projectId);

  const topologySection = (
    <>
      <div className="admin-section-title">主体 - 商户 - 项目关系</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="governance-topology-table">
          <thead><tr><th>经营主体</th><th>主体编码</th><th>商户</th><th>公司名</th><th>项目</th><th>项目编码</th><th>业态</th><th>状态</th></tr></thead>
          <tbody>{merchants.map((merchant) => {
            const entity = entities.find((item) => item.id === merchant.organizationId);
            const merchantProjects = projects.filter((project) => project.merchantId === merchant.id);
            const rows = merchantProjects.length ? merchantProjects : [undefined];
            return rows.map((project, index) => (
              <tr key={`${merchant.id}-${project?.id ?? "no-project"}-${index}`}
                data-focus-node={focusObjectId && (focusObjectId === merchant.id || focusObjectId === project?.id) ? focusObjectId : undefined}
                className={isFocused(merchant.id, project?.id) ? "admin-row-selected" : undefined}>
                <td>{display(entity?.name)}</td>
                <td>{display(entity?.code)}</td>
                <td>{merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.displayName)}</TraceLink> : display(merchant.displayName)}</td>
                <td>{display(merchant.companyName)}</td>
                <td>{display(project?.name)}</td>
                <td>{display(project?.code)}</td>
                <td>{display(project?.venueType)}</td>
                <td>{formatBusinessStatus(project?.status ?? merchant.status)}</td>
              </tr>
            ));
          })}</tbody>
        </table>
      </div>
    </>
  );

  const spacesSection = (
    <>
      <div className="admin-section-title">空间点位</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="governance-topology-spaces-table">
          <thead><tr><th>空间</th><th>编码</th><th>所属项目</th><th>区域</th><th>空间类型</th><th>点位类型</th><th>状态</th></tr></thead>
          <tbody>{spaces.map((space) => {
            const project = projects.find((item) => item.id === space.venueId);
            const spaceFocused = !!focusObjectId && (focusObjectId === space.id || focusObjectId === space.venueId);
            return <tr key={space.id} className={spaceFocused ? "admin-row-selected" : undefined}><td>{display(space.name)}</td><td>{display(space.code)}</td><td>{display(project?.name ?? space.venueId)}</td><td>{display(space.areaId)}</td><td>{display(space.areaType)}</td><td>{display(space.pointType)}</td><td>{formatBusinessStatus(space.status)}</td></tr>;
          })}</tbody>
        </table>
      </div>
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "spaces" ? spaces.length : merchants.length) : 0;

  return (
    <section data-testid="governance-topology-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="governance-topology-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="governance_topology.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {entities.length === 0 && merchants.length === 0 && projects.length === 0 && spaces.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="governance-topology-empty">暂无治理拓扑数据</div>
        ) : kpiFocus ? (
          <>
            <div className="admin-state" data-testid="governance-topology-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{focusCount}）
              <button type="button" data-testid="governance-topology-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "spaces" ? spacesSection : topologySection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="governance-topology-summary">
              <div className="admin-kpi-card"><span>经营主体</span><strong>{entities.length}</strong><small>主体口径</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchants.length}</button><small>商户档案</small></div>
              <div className="admin-kpi-card"><span>项目</span><strong>{projects.length}</strong><small>门店/场馆</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空间点位</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-spaces" onClick={() => enterKpiFocus("spaces")}>{spaces.length}</button><small>{attachedSpaces} 个已挂接</small></div>
            </div>

            <div className="admin-muted-action" data-testid="governance-topology-boundary">{TEXT.boundary}</div>

            {topologySection}
            {spacesSection}
          </>
        )}
      </AdminDataState>
    </section>
  );
}
