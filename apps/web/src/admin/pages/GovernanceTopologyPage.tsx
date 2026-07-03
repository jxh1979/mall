import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, GovernanceSpaceRow, MerchantAdmissionRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const TEXT = {
  title: "集团/综合体拓扑",
  description: "展示集团、综合体、商户、项目和空间点位之间的治理关系，作为权限、合同、结算和运营责任归属的入口。",
  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces。",
  boundary: "集团、综合体、商户、项目和空间点位必须用同一套拓扑口径承接权限、合同、结算和审计；拓扑页只展示关系，不做主体变更。",
  loadFailed: "治理拓扑读取失败",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

export function GovernanceTopologyPage({ api, setGlobalError }: AdminPageProps) {
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);

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

  return (
    <section data-testid="governance-topology-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="governance-topology-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="governance_topology.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {entities.length === 0 && merchants.length === 0 && projects.length === 0 && spaces.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="governance-topology-empty">暂无治理拓扑数据</div>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="governance-topology-summary">
              <div className="admin-kpi-card"><span>经营主体</span><strong>{entities.length}</strong><small>主体口径</small></div>
              <div className="admin-kpi-card"><span>商户</span><strong>{merchants.length}</strong><small>商户档案</small></div>
              <div className="admin-kpi-card"><span>项目</span><strong>{projects.length}</strong><small>门店/场馆</small></div>
              <div className="admin-kpi-card"><span>空间点位</span><strong>{spaces.length}</strong><small>{attachedSpaces} 个已挂接</small></div>
            </div>

            <div className="admin-muted-action" data-testid="governance-topology-boundary">{TEXT.boundary}</div>

            <div className="admin-section-title">主体 - 商户 - 项目关系</div>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="governance-topology-table">
                <thead><tr><th>经营主体</th><th>主体编码</th><th>商户</th><th>公司名</th><th>项目</th><th>项目编码</th><th>业态</th><th>状态</th></tr></thead>
                <tbody>{merchants.map((merchant) => {
                  const entity = entities.find((item) => item.id === merchant.organizationId);
                  const merchantProjects = projects.filter((project) => project.merchantId === merchant.id);
                  const rows = merchantProjects.length ? merchantProjects : [undefined];
                  return rows.map((project, index) => (
                    <tr key={`${merchant.id}-${project?.id ?? "no-project"}-${index}`}>
                      <td>{display(entity?.name)}</td>
                      <td>{display(entity?.code)}</td>
                      <td>{display(merchant.displayName)}</td>
                      <td>{display(merchant.companyName)}</td>
                      <td>{display(project?.name)}</td>
                      <td>{display(project?.code)}</td>
                      <td>{display(project?.venueType)}</td>
                      <td>{display(project?.status ?? merchant.status)}</td>
                    </tr>
                  ));
                })}</tbody>
              </table>
            </div>

            <div className="admin-section-title">空间点位</div>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="governance-topology-spaces-table">
                <thead><tr><th>空间</th><th>编码</th><th>所属项目</th><th>区域</th><th>空间类型</th><th>点位类型</th><th>状态</th></tr></thead>
                <tbody>{spaces.map((space) => {
                  const project = projects.find((item) => item.id === space.venueId);
                  return <tr key={space.id}><td>{display(space.name)}</td><td>{display(space.code)}</td><td>{display(project?.name ?? space.venueId)}</td><td>{display(space.areaId)}</td><td>{display(space.areaType)}</td><td>{display(space.pointType)}</td><td>{formatBusinessStatus(space.status)}</td></tr>;
                })}</tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
