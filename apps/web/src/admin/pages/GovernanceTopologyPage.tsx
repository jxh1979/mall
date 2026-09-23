// 文件版本：V0.04
import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { TablePager } from "../components/TablePager";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, GovernanceSpaceRow, MerchantAdmissionRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

type MerchantRow = MerchantAdmissionRow & { projectId?: string | null };
type SpaceTypeRow = { typeCode: string; typeName: string };
const PROJECT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_activation: "待启用", active: "已启用", suspended: "暂停", terminated: "终止", archived: "归档" };
const ORGANIZATION_STATUS_LABELS: Record<string, string> = { pending_admission: "待准入", active: "正常", suspended: "暂停", terminated: "终止", archived: "归档", blacklisted: "黑名单" };
function formatTopologyStatus(project: GovernanceProjectRow | undefined, merchant: MerchantRow) {
  const code = project?.status ?? merchant.status;
  return code ? (project ? PROJECT_STATUS_LABELS : ORGANIZATION_STATUS_LABELS)[code] ?? "未识别状态" : "—";
}

// M012-016：字典显示名优先；保留原有中文种子作为兼容显示。
const SPACE_TYPE_LABELS: Record<string, string> = { building: "楼栋", floor: "楼层", zone: "区块/区域", shop: "商铺/铺位", public_area: "公共区域", parking: "车位/车库", booth: "摊位/展位", other: "其他" };
function formatSpaceType(code: string | null | undefined, types: SpaceTypeRow[]) {
  return code ? types.find((type) => type.typeCode === code)?.typeName || SPACE_TYPE_LABELS[code] || "未识别类型" : "—";
}
// M012-004：空间状态闭集 active/unavailable（M009-036）页内映射「可用/不可用」，不直出英文。
const SPACE_STATUS_LABELS: Record<string, string> = { active: "可用", unavailable: "不可用" };
function formatSpaceStatus(code: string | null | undefined) {
  return code ? SPACE_STATUS_LABELS[code] ?? "未识别状态" : "—";
}

const TEXT = {
  title: "集团/综合体拓扑",
  description: "展示集团、综合体、商户、项目和空间点位之间的治理关系，作为权限、合同、结算和运营责任归属的入口。",
  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects、/api/v1/platform/spaces、/api/v1/platform/space-types。",
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

export function GovernanceTopologyPage({ api, setGlobalError, focusObjectId, navigateTo, effectivePermissions }: AdminPageProps) {
  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
  const entityRows = useServerPagedRows(
    (page, size) => {
      setGlobalError(null);
      return canEntityView
      ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size });
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const merchantRows = useServerPagedRows(
    (page, size) => {
      setGlobalError(null);
      return api.getPaged<MerchantRow>("/api/v1/platform/merchants?page=" + page + "&size=" + size);
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const projectRows = useServerPagedRows(
    (page, size) => {
      setGlobalError(null);
      return api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size);
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const spaceRows = useServerPagedRows(
    (page, size) => {
      setGlobalError(null);
      return api.getPaged<GovernanceSpaceRow>("/api/v1/platform/spaces?page=" + page + "&size=" + size);
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const entities = entityRows.rows;
  const merchants = merchantRows.rows;
  const projects = projectRows.rows;
  const spaces = spaceRows.rows;
  const [spaceTypes, setSpaceTypes] = React.useState<SpaceTypeRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：拓扑关系表 / 空间点位表整行可点 → 该行完整字段（枚举中文·商户可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

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
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const dictionary = useAdminResource(
    async () => setSpaceTypes(await api.get<SpaceTypeRow[]>("/api/v1/platform/space-types")),
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );
  const resources = [entityRows, merchantRows, projectRows, spaceRows, dictionary];
  const loading = resources.some((resource) => resource.loading);
  const error = resources.find((resource) => resource.error)?.error ?? null;
  const reload = () => resources.forEach((resource) => { void resource.reload(); });

  const projectIds = new Set(projects.map((project) => project.id));
  // M012-003：后台 SpaceRecord 以 projectId 挂接项目（无 venueId；types.ts 的 venueId 为旧臆造字段）。
  const attachedSpaces = spaces.filter((space) => space.projectId && projectIds.has(space.projectId)).length;

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

  function openTopologyDetail(entity: BusinessEntityRow | undefined, merchant: MerchantRow, project: GovernanceProjectRow | undefined) {
    setDetail({
      title: `拓扑关系 · ${display(merchant.merchantName)}`,
      fields: [
        { label: "经营主体", value: entity ? display(entity.entityName) : canEntityView && merchant.businessEntityId ? "主体资料未加载" : "-" },
        { label: "统一社会信用代码", value: display(entity?.unifiedSocialCreditCode) },
        { label: "商户", value: merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName) },
        { label: "项目", value: display(project?.name) },
        { label: "项目编码", value: display(project?.code) },
        { label: "业态", value: formatBusinessStatus(project?.venueType) },
        { label: "状态", value: formatTopologyStatus(project, merchant) },
      ],
    });
  }

  function openSpaceDetail(space: GovernanceSpaceRow, project: GovernanceProjectRow | undefined) {
    setDetail({
      title: `空间点位 · ${display(space.spaceName)}`,
      fields: [
        { label: "空间", value: display(space.spaceName) },
        { label: "所属项目", value: display(project?.name ?? space.projectId) },
        { label: "楼层", value: display(space.floorNo) },
        { label: "面积(㎡)", value: display(space.areaSquareMeter) },
        { label: "空间类型", value: formatSpaceType(space.spaceType, spaceTypes) },
        { label: "状态", value: formatSpaceStatus(space.status) },
      ],
    });
  }

  const topologySection = (
    <>
      <div className="admin-section-title">主体 - 商户 - 项目关系</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="governance-topology-table">
          <thead><tr><th>经营主体</th><th>统一社会信用代码</th><th>商户</th><th>项目</th><th>项目编码</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{merchants.map((merchant) => {
            const entity = entities.find((item) => item.id === merchant.businessEntityId);
            const merchantProjects = projects.filter((project) => project.id === merchant.projectId);
            const rows = merchantProjects.length ? merchantProjects : [undefined];
            return rows.map((project, index) => (
              <tr key={`${merchant.id}-${project?.id ?? "no-project"}-${index}`}
                data-testid={`governance-topology-row-${merchant.id}-${project?.id ?? "none"}`}
                data-focus-node={focusObjectId && (focusObjectId === merchant.id || focusObjectId === project?.id) ? focusObjectId : undefined}
                className={`admin-row-clickable${isFocused(merchant.id, project?.id) ? " admin-row-selected" : ""}`}
                tabIndex={0}
                onClick={() => openTopologyDetail(entity, merchant, project)}
                onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openTopologyDetail(entity, merchant, project); } }}>
                <td>{entity ? display(entity.entityName) : canEntityView && merchant.businessEntityId ? "主体资料未加载" : "-"}</td>
                <td>{display(entity?.unifiedSocialCreditCode)}</td>
                <td onClick={(event) => event.stopPropagation()}>{merchant.id ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={merchant.id}>{display(merchant.merchantName)}</TraceLink> : display(merchant.merchantName)}</td>
                <td>{display(project?.name)}</td>
                <td>{display(project?.code)}</td>
                <td>{formatBusinessStatus(project?.venueType)}</td>
                <td>{formatTopologyStatus(project, merchant)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看拓扑关系详情" onClick={(event) => { event.stopPropagation(); openTopologyDetail(entity, merchant, project); }}>查看详情</button></td></tr>
            ));
          })}</tbody>
        </table>
        <TablePager testid="governance-merchants-pager" page={merchantRows.page} pageCount={merchantRows.pageCount} total={merchantRows.total} onPrev={merchantRows.prev} onNext={merchantRows.next} />
        {canEntityView && entityRows.pageCount > 1 ? <><span>经营主体资料分页（用于关系表）</span><TablePager testid="governance-entities-pager" page={entityRows.page} pageCount={entityRows.pageCount} total={entityRows.total} onPrev={entityRows.prev} onNext={entityRows.next} /></> : null}
      </div>
    </>
  );

  const spacesSection = (
    <>
      <div className="admin-section-title">空间点位</div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="governance-topology-spaces-table">
          <thead><tr><th>空间</th><th>所属项目</th><th>楼层</th><th>面积(㎡)</th><th>空间类型</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{spaces.map((space) => {
            const project = projects.find((item) => item.id === space.projectId);
            const spaceFocused = !!focusObjectId && (focusObjectId === space.id || focusObjectId === space.projectId);
            return <tr key={space.id} data-testid={`governance-space-row-${space.id}`} className={`admin-row-clickable${spaceFocused ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openSpaceDetail(space, project)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openSpaceDetail(space, project); } }}><td>{display(space.spaceName)}</td><td>{display(project?.name ?? space.projectId)}</td><td>{display(space.floorNo)}</td><td>{display(space.areaSquareMeter)}</td><td>{formatSpaceType(space.spaceType, spaceTypes)}</td><td>{formatSpaceStatus(space.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看空间点位详情" onClick={(event) => { event.stopPropagation(); openSpaceDetail(space, project); }}>查看详情</button></td></tr>;
          })}</tbody>
        </table>
        <TablePager testid="governance-spaces-pager" page={spaceRows.page} pageCount={spaceRows.pageCount} total={spaceRows.total} onPrev={spaceRows.prev} onNext={spaceRows.next} />
      </div>
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "spaces" ? spaceRows.total : merchantRows.total) : 0;

  return (
    <section data-testid="governance-topology-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="governance-topology-evidence">{TEXT.evidence}</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 && spaceRows.total === 0 ? (
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
              <div className="admin-kpi-card"><span>经营主体</span><strong>{entityRows.total}</strong><small>主体口径</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchantRows.total}</button><small>商户档案</small></div>
              <div className="admin-kpi-card"><span>项目</span><strong>{projectRows.total}</strong><small>门店/场馆</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>空间点位</span><button type="button" className="admin-kpi-value" data-testid="governance-topology-kpi-spaces" onClick={() => enterKpiFocus("spaces")}>{spaceRows.total}</button><small>本页 {attachedSpaces} 个已挂接</small></div>
            </div>

            <div className="admin-muted-action" data-testid="governance-topology-boundary">{TEXT.boundary}</div>

            {topologySection}
            {spacesSection}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
