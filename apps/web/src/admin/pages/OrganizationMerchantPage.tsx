import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow, MerchantAdmissionRow } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";

const TEXT = {
  title: "组织与商户档案",
  description: "维护经营主体、商户和项目档案，确保运营主体、合同主体和结算主体后续可以唯一绑定、可追溯。",
  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects；创建商户调用 /api/v1/platform/merchants。",
  boundary: "组织与商户档案只维护基础档案，不在本页替代四类主体绑定；运营主体、合同主体和结算主体必须在绑定和合同流程中统一留痕。",
  loadFailed: "组织与商户档案读取失败",
  merchantNameRequired: "商户名称不能为空。",
  contactPhoneInvalid: "联系电话必须是 11 位中国大陆手机号。",
};

const MERCHANT_TYPES = ["tenant", "retail", "catering", "entertainment", "service"];
const COOPERATION_FORMS = ["fixed_rent_plus_commission", "revenue_share", "fixed_rent"];

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

// KPI 卡可点击下钻的目标表标识（本页三张表）。
type DrillTarget = "entities" | "merchants" | "projects";

export function OrganizationMerchantPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [merchantName, setMerchantName] = React.useState("二期测试商户");
  const [merchantType, setMerchantType] = React.useState("tenant");
  const [contactName, setContactName] = React.useState("运营联系人");
  const [contactPhone, setContactPhone] = React.useState("13800000000");
  const [cooperationForm, setCooperationForm] = React.useState("fixed_rent_plus_commission");
  const [formError, setFormError] = React.useState("");

  // KPI 下钻状态（样板模式，供全站照抄）：
  // - activeTable：点 KPI 后被定位/高亮的那张表；
  // - merchantStatusFilter：状态子集 KPI（正常商户 active）点击时对商户表做客户端筛选，
  //   仅过滤已加载的 merchants 内存数组，不发起后端查询。null=不筛选（全量）。
  const [activeTable, setActiveTable] = React.useState<DrillTarget | null>(null);
  const [merchantStatusFilter, setMerchantStatusFilter] = React.useState<string | null>(null);
  const entitiesTableRef = React.useRef<HTMLDivElement>(null);
  const merchantsTableRef = React.useRef<HTMLDivElement>(null);
  const projectsTableRef = React.useRef<HTMLDivElement>(null);

  // 点 KPI 数字 → 定位到对应表 + 高亮该表区块；status 有值时对商户表做状态筛选。
  function drillToTable(target: DrillTarget, status: string | null = null) {
    setActiveTable(target);
    setMerchantStatusFilter(target === "merchants" ? status : null);
    const ref = target === "entities" ? entitiesTableRef : target === "merchants" ? merchantsTableRef : projectsTableRef;
    const el = ref.current;
    if (el && typeof el.scrollIntoView === "function") {
      el.scrollIntoView({ block: "center" });
    }
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [nextEntities, nextMerchants, nextProjects] = await Promise.all([
        api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities"),
        api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants"),
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
    if (!/^1[3-9]\d{9}$/.test(contactPhone.trim())) {
      setFormError(TEXT.contactPhoneInvalid);
      return;
    }
    setFormError("");
    setGlobalError(null);
    const request = {
      idempotencyKey: `merchant-profile-create-${Date.now()}`,
      operatorNote: "完整后台组织与商户档案验收创建",
      evidenceAttachmentIds: ["att-merchant-profile-create"],
      merchantName,
      merchantType,
      businessEntityId: entities[0]?.id ?? "org-1",
      contractEntityId: "contract-subject-1",
      settlementEntityId: "settlement-subject-1",
      contactName,
      contactPhone: contactPhone.trim(),
      mallBlockCode: "1F-A",
      areaSquareMeter: 128.5,
      leasedAreaSquareMeter: 120,
      secondaryContactName: "备用联系人",
      secondaryContactPhone: "13900000000",
      rentPerSquareMeter: 12.3,
      cooperationForm,
      status: "active",
    };
    const result = await api.post<unknown>("/api/v1/platform/merchants", request);
    setActionResult({ action: "merchant_profile_created", evidenceAttachmentIds: request.evidenceAttachmentIds, request, result });
    await reload();
  }

  const visibleMerchants = merchantStatusFilter ? merchants.filter((row) => row.status === merchantStatusFilter) : merchants;

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
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="organization-merchant-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>经营主体</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => drillToTable("entities")}>{entities.length}</button>
                <small>组织档案</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>商户档案</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => drillToTable("merchants")}>{merchants.length}</button>
                <small>商户档案数</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>项目档案</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => drillToTable("projects")}>{projects.length}</button>
                <small>项目数</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>正常商户</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-active-merchants" onClick={() => drillToTable("merchants", "active")}>{merchants.filter((row) => row.status === "active").length}</button>
                <small>正常商户数</small>
              </div>
            </div>

            <div className="admin-form-grid">
              <label>商户名称<input data-testid="organization-merchant-name" value={merchantName} onChange={(event) => setMerchantName(event.target.value)} /></label>
              <label>商户类型<select data-testid="organization-merchant-type" value={merchantType} onChange={(event) => setMerchantType(event.target.value)}>{MERCHANT_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}</select></label>
              <label>联系人<input data-testid="organization-merchant-contact-name" value={contactName} onChange={(event) => setContactName(event.target.value)} /></label>
              <label>联系电话<input data-testid="organization-merchant-contact-phone" value={contactPhone} onChange={(event) => setContactPhone(event.target.value)} /></label>
              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}>{COOPERATION_FORMS.map((form) => <option key={form} value={form}>{form}</option>)}</select></label>
            </div>
            <div className="admin-form-row">
              <PermissionButton permissionsState={effectivePermissions} permissions={["merchant.profile.manage"]} data-testid="organization-merchant-create" onClick={() => void createMerchant()}>创建商户档案</PermissionButton>
              <span className="admin-muted-action" data-testid="organization-merchant-boundary">{TEXT.boundary}</span>
            </div>

            <div className="admin-section-title">经营主体</div>
            <div ref={entitiesTableRef} className={`admin-table-scroll${activeTable === "entities" ? " admin-section-highlight" : ""}`} data-testid="organization-entities-section">
              <table className="admin-table" data-testid="organization-entities-table">
                <thead><tr><th>主体</th><th>编码</th><th>类型</th><th>状态</th></tr></thead>
                <tbody>{entities.map((row) => <tr key={row.id}><td>{display(row.name)}</td><td>{display(row.code)}</td><td>{display(row.orgType)}</td><td>{formatBusinessStatus(row.status)}</td></tr>)}</tbody>
              </table>
            </div>

            <div className="admin-section-title">商户档案{merchantStatusFilter ? <span className="admin-muted-action" data-testid="organization-merchants-filter-hint">（已按“正常商户”筛选）</span> : null}</div>
            <div ref={merchantsTableRef} className={`admin-table-scroll${activeTable === "merchants" ? " admin-section-highlight" : ""}`} data-testid="organization-merchants-section">
              <table className="admin-table" data-testid="organization-merchants-table">
                <thead><tr><th>商户</th><th>公司名</th><th>区块</th><th>面积</th><th>合作形式</th><th>风险</th><th>状态</th><th>留痕证据</th></tr></thead>
                <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={row.id === focusObjectId ? "admin-row-selected" : undefined}><td>{display(row.displayName)}</td><td>{display(row.companyName)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{display(row.cooperationForm)}</td><td>{formatBusinessStatus(row.riskLevel)}</td><td>{formatBusinessStatus(row.status)}</td><td>{display(row.auditEvidenceRef)}</td></tr>)}</tbody>
              </table>
            </div>

            <div className="admin-section-title">项目档案</div>
            <div ref={projectsTableRef} className={`admin-table-scroll${activeTable === "projects" ? " admin-section-highlight" : ""}`} data-testid="organization-projects-section">
              <table className="admin-table" data-testid="organization-projects-table">
                <thead><tr><th>项目</th><th>编码</th><th>商户</th><th>业态</th><th>状态</th></tr></thead>
                <tbody>{projects.map((row) => <tr key={row.id}><td>{display(row.name)}</td><td>{display(row.code)}</td><td>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{display(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td></tr>)}</tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
