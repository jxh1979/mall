// 文件版本：V0.02
import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint, auditTraceField } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, MerchantAdmissionRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowActions, type RowAction } from "../components/RowActions";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

const TEXT = {
  title: "组织与商户档案",
  description: "维护经营主体、商户和项目档案，确保运营主体、合同主体和结算主体后续可以唯一绑定、可追溯。",
  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects；创建商户调用 /api/v1/platform/merchants。",
  boundary: "组织与商户档案只维护基础档案，不在本页替代四类主体绑定；运营主体、合同主体和结算主体必须在绑定和合同流程中统一留痕。",
  loadFailed: "组织与商户档案读取失败",
  merchantNameRequired: "商户名称不能为空。",
  merchantTypeRequired: "必须选择商户业态（酒吧／餐饮／零售／游乐／传媒／其他）。",
  contactPhoneRequired: "联系电话不能为空。",
  cooperationPlaceholder: "请选择合作形式",
  focusClear: "清除筛选，返回全部",
};

// V348+V384 商户业态六值（真源 HTML §商户管理 + §收入归属）：每个商户必须归入一个业态，招商录入时必填；
// 业态是通票分账按业态拆份额与分流结算的依据。娱乐平台公司对应的即酒吧业态，不另设「娱乐」业态；
// 传媒＝演出提供方（HTML「演出票 → 传媒公司」）。
const MERCHANT_TYPES = ["bar", "catering", "retail", "amusement", "media", "other"];
// M009-003：合作方式六类（BEQR-148/BSM-028/PGM-026：自营/租赁/联营/代运营/服务商接入/其他）；代码与后端 PlatformService.MERCHANT_COOPERATION_FORMS 同词表。
const COOPERATION_FORMS: ReadonlyArray<{ code: string; label: string }> = [
  { code: "self_operated", label: "自营" },
  { code: "lease", label: "租赁" },
  { code: "joint_operation", label: "联营" },
  { code: "managed_operation", label: "代运营" },
  { code: "service_provider", label: "服务商接入" },
  { code: "other", label: "其他" },
];
function cooperationLabel(code: string | null | undefined) {
  return COOPERATION_FORMS.find((form) => form.code === code)?.label ?? formatBusinessStatus(code);
}

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

// KPI 聚焦视图（用户终裁：点 KPI 数字=带筛选参数重新进入本页，URL 变化+只渲染对应表，感知上进入新页面）。
// URL 约定（全站铺开照抄）：?kpi=<entities|merchants|projects>[&status=<状态子集>]；刷新/直链带参直接进聚焦视图。
type KpiTarget = "entities" | "merchants" | "projects";
type KpiFocus = { table: KpiTarget; status: string | null };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  const table = params.get("kpi");
  if (table !== "entities" && table !== "merchants" && table !== "projects") return null;
  return { table, status: params.get("status") };
}

// 聚焦横幅标题：与被点的 KPI 卡名一致。
function kpiFocusTitle(focus: KpiFocus): string {
  if (focus.table === "entities") return "经营主体";
  if (focus.table === "projects") return "项目档案";
  if (focus.status === "active") return "正常商户";
  return focus.status ? `商户档案（${formatBusinessStatus(focus.status)}）` : "商户档案";
}

export function OrganizationMerchantPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  // M012-006：不预填验收默认值；商户名/联系人/电话/合作形式均由用户填写或选择。
  const [merchantName, setMerchantName] = React.useState("");
  // 业态必填 → 不预选，逼招商自己选；选错＝通票分账份额拆给错的业态。
  const [merchantType, setMerchantType] = React.useState("");
  const [contactName, setContactName] = React.useState("");
  const [contactPhone, setContactPhone] = React.useState("");
  const [cooperationForm, setCooperationForm] = React.useState("");
  const [formError, setFormError] = React.useState("");
  // 引导式创建流（一期范式）：主按钮 → 分步弹窗 → 确认创建。
  const [createOpen, setCreateOpen] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：三张业务表（经营主体/商户/项目）整行可点 → 该行完整字段（枚举中文·引用可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // V260 存档/删除范式：显示已存档商户开关（后端按 includeArchived 过滤）。
  const [showArchived, setShowArchived] = React.useState(false);
  // V348 按业态筛商户（后端 merchantType 过滤）：""＝全部业态。
  const [typeFilter, setTypeFilter] = React.useState("");

  // 浏览器后退/前进也能进出聚焦视图：pushState 配合页面级 popstate 监听。
  // AdminApp 的 popstate 只重建 route/focus，本页的 ?kpi= 参数由本页自理，互不干扰。
  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  // 点 KPI 数字 → 带参重新进入本页。pathname 原样保留（生产=/complex/admin/…，子路径前缀由门户
  // rewrite+withBase 链路负责，本页不动 pathname 就不会破坏 /complex），只改查询串。
  function enterKpiFocus(table: KpiTarget, status: string | null = null) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    if (status) params.set("status", status);
    else params.delete("status");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table, status });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  // 「返回全部」：清掉 kpi/status 参数（保留 focus 等其它参数），恢复完整页面。
  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("status");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 跨域副资源不请求（根因修）：主体端点需 entity.binding.view，本页 gate 仅 merchant.profile.view + governance.project.view——
      // 缺该权限角色 403 会 reject 整个 Promise.all 致整页白屏；缺权限则不请求、主体下拉降级为空。
      const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
      const [nextEntities, nextMerchants, nextProjects] = await Promise.all([
        canEntityView ? api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities") : Promise.resolve([] as BusinessEntityRow[]),
        api.get<MerchantAdmissionRow[]>(`/api/v1/platform/merchants?includeArchived=${showArchived}${typeFilter ? `&merchantType=${typeFilter}` : ""}`),
        api.get<GovernanceProjectRow[]>("/api/v1/platform/projects"),
      ]);
      setEntities(nextEntities);
      setMerchants(nextMerchants);
      setProjects(nextProjects);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  // 下钻定位：从别的页点商户进来（?focus=商户id）→ 滚动高亮该商户档案行；无 focus 或未命中时行为不变。
  React.useEffect(() => {
    if (!focusObjectId || merchants.length === 0) return;
    if (!merchants.some((row) => row.id === focusObjectId)) return;
    const el = typeof document !== "undefined" ? document.querySelector(`[data-testid="organization-merchant-row-${focusObjectId}"]`) : null;
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView({ block: "center" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusObjectId, merchants.length]);

  async function createMerchant() {
    if (!merchantName.trim()) {
      setFormError(TEXT.merchantNameRequired);
      return;
    }
    if (!merchantType) {
      setFormError(TEXT.merchantTypeRequired);
      return;
    }
    // M012-007：三份 HTML 无联系电话格式规则，只做非空校验（操作员 2.2 答删除前端 11 位大陆手机号正则）。
    if (!contactPhone.trim()) {
      setFormError(TEXT.contactPhoneRequired);
      return;
    }
    setFormError("");
    setGlobalError(null);
    // M012-006：只提交用户填写的档案字段；经营/合同/结算主体由四类主体绑定流程承接（页内边界文案「不在本页替代四类主体绑定」），
    // 不再静默提交主体 id/区块/面积/备用联系人/租金/状态/验收备注/证据附件（后台对这些均可空，状态缺省由后台决定）。
    const request = {
      idempotencyKey: `merchant-profile-create-${Date.now()}`,
      merchantName,
      merchantType,
      contactName,
      contactPhone: contactPhone.trim(),
      ...(cooperationForm ? { cooperationForm } : {}),
    };
    // 范式约定（引导式流程）：提交失败 = 弹窗留开 + error prop 在弹窗内显错，不静默。
    try {
      const result = await api.post<unknown>("/api/v1/platform/merchants", request);
      setActionResult({ action: "merchant_profile_created", request, result });
      setCreateOpen(false);
      await reload();
    } catch (submitError) {
      setFormError(submitError instanceof Error ? submitError.message : "创建商户失败，请稍后重试。");
    }
  }

  // 切换「显示已存档」/「业态」后重新拉取（后端按 includeArchived + merchantType 过滤），首次挂载不触发。
  const firstArchivedToggle = React.useRef(true);
  React.useEffect(() => {
    if (firstArchivedToggle.current) {
      firstArchivedToggle.current = false;
      return;
    }
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showArchived, typeFilter]);

  // M012-008：存档/删除写路径失败须可见（与创建流一致），不成未处理拒绝。
  function reportWriteFailure(error: unknown, fallback: string) {
    const message = error instanceof Error ? error.message : fallback;
    setGlobalError(message);
    setActionResult({ action: "merchant_write_failed", status: "failed", message });
  }

  async function archiveMerchant(id: string, archive: boolean) {
    setGlobalError(null);
    let reason: string | undefined;
    if (archive) {
      const r = window.prompt("存档（下线）理由：");
      if (r === null) return;
      if (!r.trim()) { setGlobalError("存档理由必填"); return; }
      reason = r.trim();
    }
    try {
      await api.post(`/api/v1/platform/merchants/${id}/${archive ? "archive" : "unarchive"}`, {
        idempotencyKey: `merchant-${archive ? "archive" : "unarchive"}-${id}-${Date.now()}`,
        ...(reason ? { reason } : {}),
      });
      setActionResult({ action: archive ? "merchant_archived" : "merchant_unarchived", id });
      await reload();
    } catch (actionError) {
      reportWriteFailure(actionError, archive ? "商户存档失败" : "取消存档失败");
    }
  }

  async function requestDeleteMerchant(id: string) {
    const reason = window.prompt("删除理由（提交后需上级审批）：");
    if (reason === null || !reason.trim()) { setGlobalError("删除理由必填"); return; }
    setGlobalError(null);
    try {
      await api.post(`/api/v1/platform/merchants/${id}/delete-request`, {
        idempotencyKey: `merchant-delete-request-${id}-${Date.now()}`,
        reason,
      });
      setActionResult({ action: "merchant_delete_requested", id });
      await reload();
    } catch (actionError) {
      reportWriteFailure(actionError, "商户删除申请失败");
    }
  }

  async function approveDeleteMerchant(id: string, decision: "approved" | "rejected") {
    setGlobalError(null);
    try {
      const decided = await api.post<{ deleted?: boolean }>(`/api/v1/platform/merchants/${id}/delete-approve`, {
        idempotencyKey: `merchant-delete-approve-${id}-${decision}-${Date.now()}`,
        decision,
      });
      setActionResult({ action: `merchant_delete_${decision}`, id, deleted: decided?.deleted ?? false });
      await reload();
    } catch (actionError) {
      reportWriteFailure(actionError, "商户删除审批失败");
    }
  }

  // 状态子集 KPI（正常商户）聚焦时对商户表做客户端筛选：仅过滤已加载的内存数组，不发起后端查询。
  const merchantStatusFilter = kpiFocus?.table === "merchants" ? kpiFocus.status : null;
  const visibleMerchants = merchantStatusFilter ? merchants.filter((row) => row.status === merchantStatusFilter) : merchants;

  function openEntityDetail(row: BusinessEntityRow) {
    setDetail({
      title: `经营主体 · ${display(row.entityName)}`,
      fields: [
        { label: "主体", value: display(row.entityName) },
        { label: "统一社会信用代码", value: display(row.unifiedSocialCreditCode) },
        { label: "主体角色", value: formatBusinessStatus(row.entityRole) },
        { label: "法定代表人", value: display(row.legalRepresentative) },
        { label: "状态", value: formatBusinessStatus(row.status) },
      ],
    });
  }

  function openMerchantDetail(row: MerchantAdmissionRow) {
    setDetail({
      title: `商户档案 · ${display(row.merchantName)}`,
      fields: [
        { label: "商户", value: display(row.merchantName) },
        { label: "业态", value: row.merchantType ? formatBusinessStatus(row.merchantType) : "-" },
        { label: "联系人", value: display(row.contactName) },
        { label: "联系电话", value: display(row.contactPhone) },
        { label: "区块", value: display(row.mallBlockCode) },
        { label: "面积", value: display(row.leasedAreaSquareMeter ?? row.areaSquareMeter) },
        { label: "合作形式", value: cooperationLabel(row.cooperationForm) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        auditTraceField(navigateTo, "merchant", row.id),
      ],
    });
  }

  function openProjectDetail(row: GovernanceProjectRow) {
    setDetail({
      title: `项目档案 · ${display(row.name)}`,
      fields: [
        { label: "项目", value: display(row.name) },
        { label: "编码", value: display(row.code) },
        { label: "商户", value: row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId) },
        { label: "业态", value: formatBusinessStatus(row.venueType) },
        { label: "状态", value: formatBusinessStatus(row.status) },
      ],
    });
  }

  const entitiesSection = (
    <>
      <div className="admin-section-title">经营主体</div>
      <div className="admin-table-scroll" data-testid="organization-entities-section">
        <table className="admin-table" data-testid="organization-entities-table">
          <thead><tr><th>主体</th><th>统一社会信用代码</th><th>主体角色</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{entities.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-entity-row-${row.id}`} onClick={() => openEntityDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEntityDetail(row); } }}><td>{display(row.entityName)}</td><td>{display(row.unifiedSocialCreditCode)}</td><td>{formatBusinessStatus(row.entityRole)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看经营主体详情" onClick={(event) => { event.stopPropagation(); openEntityDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
      </div>
    </>
  );

  function merchantArchiveLabel(row: MerchantAdmissionRow): string {
    if (row.deleteStatus === "pending") return "待审批删除";
    if (row.archivedAt) return "已存档";
    return "生效中";
  }

  function merchantActions(row: MerchantAdmissionRow) {
    const actions: RowAction[] = row.deleteStatus === "pending"
      ? [
          { key: "approve", label: "批准删除", permissions: ["merchant.profile.approve"], onClick: () => void approveDeleteMerchant(row.id, "approved"), testId: `merchant-delete-approve-${row.id}` },
          { key: "reject", label: "驳回", permissions: ["merchant.profile.approve"], danger: true, onClick: () => void approveDeleteMerchant(row.id, "rejected"), testId: `merchant-delete-reject-${row.id}` },
        ]
      : row.archivedAt
      ? [
          { key: "unarchive", label: "取消存档", permissions: ["merchant.profile.manage"], onClick: () => void archiveMerchant(row.id, false), testId: `merchant-unarchive-${row.id}` },
          { key: "delete-request", label: "申请删除", permissions: ["merchant.profile.manage"], danger: true, onClick: () => void requestDeleteMerchant(row.id), testId: `merchant-delete-request-${row.id}` },
        ]
      : [
          { key: "archive", label: "存档", permissions: ["merchant.profile.manage"], onClick: () => void archiveMerchant(row.id, true), testId: `merchant-archive-${row.id}` },
        ];
    return <RowActions actions={actions} permissionsState={effectivePermissions} testId={`merchant-actions-${row.id}`} />;
  }

  const merchantsSection = (
    <>
      <div className="admin-section-title">商户档案</div>
      <label className="admin-inline-check" data-testid="merchant-show-archived">
        <input type="checkbox" checked={showArchived} onChange={(event) => setShowArchived(event.target.checked)} />
        显示已存档商户
      </label>
      <label className="admin-inline-check">
        业态
        <select data-testid="merchant-type-filter" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
          <option value="">全部业态</option>
          {MERCHANT_TYPES.map((type) => <option key={type} value={type}>{formatBusinessStatus(type)}</option>)}
        </select>
      </label>
      <div className="admin-table-scroll" data-testid="organization-merchants-section">
        <table className="admin-table" data-testid="organization-merchants-table">
          {/* 后端无“公司名/风险/留痕证据”字段（原按臆造字段读致整表“-”），列对齐真实字段：联系人/联系电话替换。 */}
          <thead><tr><th>商户</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
      </div>
    </>
  );

  const projectsSection = (
    <>
      <div className="admin-section-title">项目档案</div>
      <div className="admin-table-scroll" data-testid="organization-projects-section">
        <table className="admin-table" data-testid="organization-projects-table">
          <thead><tr><th>项目</th><th>编码</th><th>商户</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
      </div>
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "entities" ? entities.length : kpiFocus.table === "projects" ? projects.length : visibleMerchants.length) : 0;

  return (
    <section data-testid="organization-merchant-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      {focusObjectId && (
        <div className="admin-state" data-testid="organization-merchant-focus">已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</div>
      )}
      <TechDetails><div className="admin-generic-evidence" data-testid="organization-merchant-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="organization.viewed / merchant_profile.created" />
      {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {entities.length === 0 && merchants.length === 0 && projects.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="organization-merchant-empty">暂无组织与商户档案</div>
        ) : kpiFocus ? (
          <>
            {/* 聚焦视图：只渲染 KPI 对应那张表，其余表/表单/KPI 卡全部隐藏，感知上=新页面。 */}
            <div className="admin-state" data-testid="organization-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{focusCount}）
              <button type="button" data-testid="organization-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "entities" ? entitiesSection : kpiFocus.table === "merchants" ? merchantsSection : projectsSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="organization-merchant-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>经营主体</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => enterKpiFocus("entities")}>{entities.length}</button>
                <small>组织档案</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>商户档案</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchants.length}</button>
                <small>商户档案数</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>项目档案</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projects.length}</button>
                <small>项目数</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>正常商户</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-active-merchants" onClick={() => enterKpiFocus("merchants", "active")}>{merchants.filter((row) => row.status === "active").length}</button>
                <small>正常商户数</small>
              </div>
            </div>

            {/* 引导式流程一期：内联填表收起为主按钮，点击弹出分步引导（GuidedFlowModal）。 */}
            <div className="admin-form-row">
              <PermissionButton permissionsState={effectivePermissions} permissions={["merchant.profile.manage"]} data-testid="organization-merchant-create-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ 新建商户</PermissionButton>
              <span className="admin-muted-action" data-testid="organization-merchant-boundary">{TEXT.boundary}</span>
            </div>

            {entitiesSection}
            {merchantsSection}
            {projectsSection}
          </>
        )}
      </AdminDataState>
      <GuidedFlowModal
        open={createOpen}
        title="新建商户档案"
        testId="organization-merchant-flow"
        submitTestId="organization-merchant-create"
        submitLabel="创建商户档案"
        error={formError}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void createMerchant()}
        steps={[
          {
            title: "基本信息",
            description: "填写商户名称并选择业态（业态决定通票分账时这家商户的份额归哪一类，必填）。",
            validate: () => (!merchantName.trim() ? TEXT.merchantNameRequired : merchantType ? null : TEXT.merchantTypeRequired),
            content: (
              <>
                <label>商户名称<input data-testid="organization-merchant-name" value={merchantName} onChange={(event) => setMerchantName(event.target.value)} /></label>
                <label>业态<select data-testid="organization-merchant-type" value={merchantType} onChange={(event) => setMerchantType(event.target.value)}><option value="">请选择业态</option>{MERCHANT_TYPES.map((type) => <option key={type} value={type}>{formatBusinessStatus(type)}</option>)}</select></label>
              </>
            ),
          },
          {
            title: "联系人",
            description: "填写商户对接联系人及电话。",
            validate: () => (contactPhone.trim() ? null : TEXT.contactPhoneRequired),
            content: (
              <>
                <label>联系人<input data-testid="organization-merchant-contact-name" value={contactName} onChange={(event) => setContactName(event.target.value)} /></label>
                <label>联系电话<input data-testid="organization-merchant-contact-phone" value={contactPhone} onChange={(event) => setContactPhone(event.target.value)} /></label>
              </>
            ),
          },
          {
            title: "经营与租赁",
            description: "选择与商户约定的合作形式。",
            content: (
              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}><option value="">{TEXT.cooperationPlaceholder}</option>{COOPERATION_FORMS.map((form) => <option key={form.code} value={form.code}>{form.label}</option>)}</select></label>
            ),
          },
        ]}
        summary={[
          { label: "商户名称", value: merchantName },
          { label: "业态", value: merchantType ? formatBusinessStatus(merchantType) : "未选择" },
          { label: "联系人", value: contactName },
          { label: "联系电话", value: contactPhone },
          { label: "合作形式", value: cooperationForm ? cooperationLabel(cooperationForm) : "未选择" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
