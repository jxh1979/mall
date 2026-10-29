// 文件版本：V0.12
import React from "react";
import { formatActionResult } from "../components/ActionResultView";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { auditTraceField } from "../components/AuditTrailHint";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { TablePager } from "../components/TablePager";
import { hasPermissions } from "../security/PermissionGate";
import type { AccessUserRow, AdminPageProps, BusinessEntityRow, GovernanceProjectRow, MerchantAdmissionRow } from "../types";
import { Can, PermissionButton } from "../security/Can";
import { shortId } from "../format/ids";
import { RowActions, type RowAction } from "../components/RowActions";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

type MerchantRow = MerchantAdmissionRow & { projectId?: string | null };
// 第十八批 E 段第 20 条（治理地图第 659 行「一个商户要在别的项目经营的，由能管理商户档案的人在商户档案里加经营项目，要经过审批才生效。」）：
// GET /api/v1/platform/merchants/{merchant_id}/operating-projects 的返回（主项目、已生效的经营项目、申请记录、可申请的项目）。
type ProjectOption = { projectId: string; projectName?: string | null };
type OperatingProjectRequestRow = { id: string; projectId: string; projectName?: string | null; status: string; reason?: string | null; requestedAt?: string | null; decidedAt?: string | null };
type OperatingProjectsView = { primaryProjectId?: string | null; primaryProjectName?: string | null; operatingProjects?: ProjectOption[]; requests?: OperatingProjectRequestRow[]; projectOptions?: ProjectOption[] };
const OPERATING_REQUEST_STATUS_LABELS: Record<string, string> = { pending_approval: "待审批", approved: "已批准", rejected: "已驳回" };
const ORGANIZATION_STATUS_LABELS: Record<string, string> = { pending_admission: "待准入", active: "正常", suspended: "暂停", terminated: "终止", archived: "归档", blacklisted: "黑名单" };
const PROJECT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_activation: "待启用", active: "已启用", suspended: "暂停", terminated: "终止", archived: "归档" };
function formatOrganizationStatus(code?: string | null) {
  return code ? ORGANIZATION_STATUS_LABELS[code] ?? "未识别状态" : "—";
}
function formatProjectStatus(code?: string | null) {
  return code ? PROJECT_STATUS_LABELS[code] ?? "未识别状态" : "—";
}

const TEXT = {
  title: "组织与商户档案",
  description: "维护经营主体、商户和项目档案，确保运营主体、合同主体和结算主体后续可以唯一绑定、可追溯。",
  evidence: "接口证据：读取 /api/v1/platform/business-entities、/api/v1/platform/merchants、/api/v1/platform/projects；创建商户调用 /api/v1/platform/merchants。",
  boundary: "组织与商户档案只维护基础档案，不在本页替代四类主体绑定；运营主体、合同主体和结算主体必须在绑定和合同流程中统一留痕。",
  loadFailed: "组织与商户档案读取失败",
  merchantNameRequired: "商户名称不能为空。",
  merchantTypeRequired: "必须选择商户业态（酒吧／餐饮／零售／游乐／传媒／其他）。",
  supervisingCompanyRequired: "业态为其他时，必须指定一家主管公司并说明理由。",
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
// FS-024（ENUM「商户业态」条：选“其他”的商户，必须在商户档案里指定一家主管公司并说明理由，未指定的不得开通任何结算与分账）：
// 主管公司只能从在用的城市项目公司／域主管公司中选（与后端 PlatformService.SUPERVISING_COMPANY_ROLES 同词表）。
const SUPERVISING_COMPANY_ROLES = ["project_company", "management_company"];
function cooperationLabel(code: string | null | undefined) {
  return COOPERATION_FORMS.find((form) => form.code === code)?.label ?? formatBusinessStatus(code);
}

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return formatActionResult(value);
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
  if (focus.status === "active") return "本页正常商户";
  return focus.status ? `本页商户档案（${formatOrganizationStatus(focus.status)}）` : "商户档案";
}

export function OrganizationMerchantPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  // M012-006：不预填验收默认值；商户名/联系人/电话/合作形式均由用户填写或选择。
  const [merchantName, setMerchantName] = React.useState("");
  // 业态必填 → 不预选，逼招商自己选；选错＝通票分账份额拆给错的业态。
  const [merchantType, setMerchantType] = React.useState("");
  const [contactName, setContactName] = React.useState("");
  const [contactPhone, setContactPhone] = React.useState("");
  const [cooperationForm, setCooperationForm] = React.useState("");
  // FS-024：业态为其他时必填的主管公司（在用主体 id）与指定理由。
  const [supervisingCompanyId, setSupervisingCompanyId] = React.useState("");
  const [supervisingCompanyReason, setSupervisingCompanyReason] = React.useState("");
  // 第十八批 E 段第 19 条（治理地图第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」）：
  // 能看到本人项目以外的项目就选所属项目（18G 同根补扫：角色只勾了 B、只看得到 B 一个时也显示）；不选＝后端按本人所在项目落。
  const [merchantProjectId, setMerchantProjectId] = React.useState("");
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
  // 第十八批 E 段第 20 条：商户行「经营项目」——当前查看的商户与其经营项目；加经营项目申请表单（项目、理由、最终审批人）。
  const [operating, setOperating] = React.useState<{ merchant: MerchantRow; view: OperatingProjectsView | null } | null>(null);
  const [operatingProjectId, setOperatingProjectId] = React.useState("");
  const [operatingReason, setOperatingReason] = React.useState("");
  const [operatingApprover, setOperatingApprover] = React.useState("");
  const [approverUsers, setApproverUsers] = React.useState<AccessUserRow[]>([]);
  const [operatingError, setOperatingError] = React.useState("");

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

  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);
  const entityRows = useServerPagedRows(
    (page, size) => {
      setGlobalError(null);
      return canEntityView ? api.getPaged<BusinessEntityRow>("/api/v1/platform/business-entities?page=" + page + "&size=" + size)
      : Promise.resolve({ items: [] as BusinessEntityRow[], total: 0, page: 1, size });
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const merchantRows = useServerPagedRows(
    (page, size, query) => {
      setGlobalError(null);
      return api.getPaged<MerchantRow>("/api/v1/platform/merchants?" + (query || "includeArchived=false") + "&page=" + page + "&size=" + size);
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const projectRows = useServerPagedRows(
    (page, size) => {
      setGlobalError(null);
      return api.getPaged<GovernanceProjectRow>("/api/v1/platform/projects?page=" + page + "&size=" + size);
    },
    500, { onError: setGlobalError, fallbackMessage: TEXT.loadFailed });
  const entities = entityRows.rows;
  const merchants = merchantRows.rows;
  const projects = projectRows.rows;
  const pickProject = projects.some((row) => row.id !== user?.organizationId);
  // FS-024：主管公司候选＝在用的城市项目公司／域主管公司；列表与详情按 id 回显主体名称。
  const supervisingCandidates = entities.filter((row) => row.status === "active" && SUPERVISING_COMPANY_ROLES.includes(row.entityRole ?? ""));
  function supervisingCompanyName(id?: string | null) {
    if (!id) return "-";
    return entities.find((row) => row.id === id)?.entityName ?? id;
  }
  function supervisingCompanyError() {
    return merchantType === "other" && (!supervisingCompanyId || !supervisingCompanyReason.trim()) ? TEXT.supervisingCompanyRequired : null;
  }
  const resources = [entityRows, merchantRows, projectRows];
  const loading = resources.some((resource) => resource.loading);
  const error = resources.find((resource) => resource.error)?.error ?? null;
  const reload = () => resources.forEach((resource) => resource.reload());

  React.useEffect(() => {
    if (!merchantRows.loading && merchantRows.page > merchantRows.pageCount) merchantRows.search(merchantRows.keyword);
  }, [merchantRows.loading, merchantRows.page, merchantRows.pageCount, merchantRows.keyword]);

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
    const supervisingError = supervisingCompanyError();
    if (supervisingError) {
      setFormError(supervisingError);
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
      ...(merchantProjectId ? { projectId: merchantProjectId } : {}),
      // FS-024：只在业态为其他时提交主管公司与理由（后端对其他业态填写一律拒绝）。
      ...(merchantType === "other" ? { supervisingCompanyId, supervisingCompanyReason: supervisingCompanyReason.trim() } : {}),
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
    merchantRows.search(new URLSearchParams({ includeArchived: String(showArchived), ...(typeFilter ? { merchantType: typeFilter } : {}) }).toString());
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
    const reason = window.prompt("删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：");
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

  // 第十批 TK-27（BSM L1058「待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）」）：
  // 酒吧商户配置待清台提醒时限——先读当前值，再按值、下限、上限三者同填提交（后端校验值在闭区间内）。
  async function configurePendingClearAlert(id: string) {
    setGlobalError(null);
    try {
      const current = await api.get<{ valueMinutes?: number | null; minMinutes?: number | null; maxMinutes?: number | null; effectiveMinutes: number }>(
        `/api/v1/platform/merchants/${id}/pending-clear-alert`);
      const configured = current.valueMinutes != null;
      const value = window.prompt(`待清台提醒时限（分钟）；${configured ? "当前已配置" : "未配置，按出厂 15 分钟"}：`, String(current.effectiveMinutes));
      if (value === null) return;
      const min = window.prompt("时限下限（分钟，须与时限、上限同时配置）：", configured ? String(current.minMinutes) : "");
      if (min === null) return;
      const max = window.prompt("时限上限（分钟，须与时限、下限同时配置）：", configured ? String(current.maxMinutes) : "");
      if (max === null) return;
      const v = Number(value.trim()), lo = Number(min.trim()), hi = Number(max.trim());
      if (![v, lo, hi].every((n) => Number.isInteger(n) && n >= 0) || lo > v || v > hi) {
        setGlobalError("须同时填写时限及其上下限（非负整数分钟），时限须在上下限内");
        return;
      }
      await api.put(`/api/v1/platform/merchants/${id}/pending-clear-alert`, {
        idempotencyKey: `merchant-pending-clear-alert-${id}-${Date.now()}`,
        valueMinutes: v,
        minMinutes: lo,
        maxMinutes: hi,
      });
      setActionResult({ action: "merchant_pending_clear_alert_configured", id });
    } catch (actionError) {
      reportWriteFailure(actionError, "待清台提醒时限保存失败");
    }
  }

  // 第十八批 C 段：酒吧商户两项按酒吧可配的百分比参数（值与上下限同配，整数 %）——第 15 条员工代订爽约率上限（BSM:1468，未配置不自动停用）、
  // 第 17 条损耗率阈值（BSM:2015，出厂 5%；第十八批裁决 4 标准损耗＝标准出酒量×该阈值）。先读当前值，再三者同填提交（后端校验闭区间）。
  async function configureMerchantPercentParam(id: string, path: "staff-booking-no-show-limit" | "pour-loss-rate", label: string, unconfiguredHint: string) {
    setGlobalError(null);
    try {
      const current = await api.get<{ valuePct?: number | null; minPct?: number | null; maxPct?: number | null; effectivePct?: number | null }>(
        `/api/v1/platform/merchants/${id}/${path}`);
      const configured = current.valuePct != null;
      const value = window.prompt(`${label}（%）；${configured ? "当前已配置" : unconfiguredHint}：`, current.effectivePct != null ? String(current.effectivePct) : "");
      if (value === null) return;
      const min = window.prompt(`${label}下限（%，须与值、上限同时配置）：`, configured ? String(current.minPct) : "");
      if (min === null) return;
      const max = window.prompt(`${label}上限（%，须与值、下限同时配置）：`, configured ? String(current.maxPct) : "");
      if (max === null) return;
      const v = Number(value.trim()), lo = Number(min.trim()), hi = Number(max.trim());
      if (![v, lo, hi].every((n) => Number.isInteger(n) && n >= 0 && n <= 100) || lo > v || v > hi) {
        setGlobalError(`须同时填写${label}及其上下限（0～100 的整数 %），值须在上下限内`);
        return;
      }
      await api.put(`/api/v1/platform/merchants/${id}/${path}`, {
        idempotencyKey: `merchant-${path}-${id}-${Date.now()}`,
        valuePct: v,
        minPct: lo,
        maxPct: hi,
      });
      setActionResult({ action: path === "pour-loss-rate" ? "merchant_pour_loss_rate_configured" : "merchant_staff_booking_no_show_limit_configured", id });
    } catch (actionError) {
      reportWriteFailure(actionError, `${label}保存失败`);
    }
  }

  async function loadOperatingProjects(row: MerchantRow) {
    try {
      setOperating({ merchant: row, view: await api.get<OperatingProjectsView>(`/api/v1/platform/merchants/${row.id}/operating-projects`) });
    } catch (loadError) {
      setOperating({ merchant: row, view: null });
      setOperatingError(loadError instanceof Error ? loadError.message : "经营项目读取失败");
    }
  }

  // 第十八批 E 段第 20 条：打开某商户的经营项目；持商户档案管理权限的取员工名单供选最终审批人（取不到即空列表，照招商黑名单例外放行的写法）。
  async function openOperatingProjects(row: MerchantRow) {
    setOperatingError("");
    setOperatingProjectId("");
    setOperatingReason("");
    setOperatingApprover("");
    await loadOperatingProjects(row);
    if (hasPermissions(effectivePermissions, ["merchant.profile.manage"])) {
      try {
        setApproverUsers((await api.get<AccessUserRow[]>("/api/v1/access/users")) ?? []);
      } catch { setApproverUsers([]); }
    }
  }

  // 第十八批 E 段第 20 条：提交加经营项目申请（项目、理由必填；最终审批人不指定＝直接上级审批），审批在主管复核待办办理，批准才生效。
  async function requestOperatingProject() {
    if (!operating) return;
    if (!operatingProjectId) { setOperatingError("请选择要加的经营项目。"); return; }
    if (!operatingReason.trim()) { setOperatingError("加经营项目必须填写理由。"); return; }
    setOperatingError("");
    const request = {
      idempotencyKey: `merchant-operating-project-${operating.merchant.id}-${Date.now()}`,
      projectId: operatingProjectId,
      reason: operatingReason.trim(),
      ...(operatingApprover ? { finalApproverId: operatingApprover } : {}),
    };
    try {
      const result = await api.post<unknown>(`/api/v1/platform/merchants/${operating.merchant.id}/operating-projects`, request);
      setActionResult({ action: "merchant_operating_project_requested", request, result });
      setOperatingProjectId("");
      setOperatingReason("");
      setOperatingApprover("");
      await loadOperatingProjects(operating.merchant);
    } catch (submitError) {
      setOperatingError(submitError instanceof Error ? submitError.message : "加经营项目申请提交失败，请稍后重试。");
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
        { label: "状态", value: formatOrganizationStatus(row.status) },
      ],
    });
  }

  function merchantProjectName(row: MerchantRow) {
    return projects.find((project) => project.id === row.projectId)?.name ?? (row.projectId ? "项目资料未加载" : "-");
  }

  function openMerchantDetail(row: MerchantRow) {
    setDetail({
      title: `商户档案 · ${display(row.merchantName)}`,
      fields: [
        { label: "商户", value: display(row.merchantName) },
        { label: "所属项目", value: merchantProjectName(row) },
        { label: "业态", value: row.merchantType ? formatBusinessStatus(row.merchantType) : "-" },
        { label: "主管公司", value: row.merchantType === "other" ? supervisingCompanyName(row.supervisingCompanyId) : "-" },
        { label: "主管公司指定理由", value: row.merchantType === "other" ? display(row.supervisingCompanyReason) : "-" },
        { label: "联系人", value: display(row.contactName) },
        { label: "联系电话", value: display(row.contactPhone) },
        { label: "区块", value: display(row.mallBlockCode) },
        { label: "面积", value: display(row.leasedAreaSquareMeter ?? row.areaSquareMeter) },
        { label: "合作形式", value: cooperationLabel(row.cooperationForm) },
        { label: "状态", value: formatOrganizationStatus(row.status) },
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
        { label: "业态", value: formatBusinessStatus(row.venueType) },
        { label: "状态", value: formatProjectStatus(row.status) },
      ],
    });
  }

  const entitiesSection = (
    <>
      <div className="admin-section-title">经营主体</div>
      <div className="admin-table-scroll" data-testid="organization-entities-section">
        <table className="admin-table" data-testid="organization-entities-table">
          <thead><tr><th>主体</th><th>统一社会信用代码</th><th>主体角色</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{entities.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-entity-row-${row.id}`} onClick={() => openEntityDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openEntityDetail(row); } }}><td>{display(row.entityName)}</td><td>{display(row.unifiedSocialCreditCode)}</td><td>{formatBusinessStatus(row.entityRole)}</td><td>{formatOrganizationStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看经营主体详情" onClick={(event) => { event.stopPropagation(); openEntityDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="organization-entities-pager" page={entityRows.page} pageCount={entityRows.pageCount} total={entityRows.total} onPrev={entityRows.prev} onNext={entityRows.next} />
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
          ...(row.merchantType === "bar"
            ? [
                { key: "pending-clear-alert", label: "待清台提醒时限", permissions: ["merchant.profile.manage"], onClick: () => void configurePendingClearAlert(row.id), testId: `merchant-pending-clear-alert-${row.id}` },
                { key: "no-show-limit", label: "代订爽约率上限", permissions: ["merchant.profile.manage"], onClick: () => void configureMerchantPercentParam(row.id, "staff-booking-no-show-limit", "代订爽约率上限", "未配置，不自动停用"), testId: `merchant-no-show-limit-${row.id}` },
                { key: "pour-loss-rate", label: "损耗率阈值", permissions: ["merchant.profile.manage"], onClick: () => void configureMerchantPercentParam(row.id, "pour-loss-rate", "损耗率阈值", "未配置，按出厂 5%"), testId: `merchant-pour-loss-rate-${row.id}` },
              ]
            : []),
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
          <thead><tr><th>商户</th><th>所属项目</th><th>业态</th><th>主管公司</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{merchantProjectName(row)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{row.merchantType === "other" ? supervisingCompanyName(row.supervisingCompanyId) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatOrganizationStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}<Can permissionsState={effectivePermissions} permissions={["merchant.profile.view"]}><button type="button" data-testid={`merchant-operating-projects-${row.id}`} onClick={() => void openOperatingProjects(row)}>经营项目</button></Can></td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="organization-merchants-pager" page={merchantRows.page} pageCount={merchantRows.pageCount} total={merchantRows.total} onPrev={merchantRows.prev} onNext={merchantRows.next} />
      </div>
      {operating ? (
        <div className="admin-form-grid" data-testid="merchant-operating-projects-panel">
          <span className="admin-muted-action">经营项目：{display(operating.merchant.merchantName)}。加经营项目须经审批才生效（按组织层级逐级审批，申请人不能审批自己的申请）；批准后各经营项目的员工与主项目的员工一样维护这份商户档案。</span>
          {operating.view ? (
            <>
              <div>主项目：{display(operating.view.primaryProjectName ?? operating.view.primaryProjectId)}</div>
              <div>已生效的经营项目：{operating.view.operatingProjects?.length ? operating.view.operatingProjects.map((row) => display(row.projectName ?? row.projectId)).join("、") : "无"}</div>
              <table className="admin-table" data-testid="merchant-operating-project-requests">
                <thead><tr><th>申请经营项目</th><th>状态</th><th>理由</th><th>申请时间</th><th>审批时间</th></tr></thead>
                <tbody>{(operating.view.requests ?? []).map((row) => <tr key={row.id} data-testid={`merchant-operating-project-request-${row.id}`}><td>{display(row.projectName ?? row.projectId)}</td><td>{OPERATING_REQUEST_STATUS_LABELS[row.status] ?? "未识别状态"}</td><td>{display(row.reason)}</td><td>{cnDate(row.requestedAt)}</td><td>{cnDate(row.decidedAt)}</td></tr>)}</tbody>
              </table>
              <Can permissionsState={effectivePermissions} permissions={["merchant.profile.manage"]}>
                <div className="admin-form-grid" data-testid="merchant-operating-project-form">
                  <label>加经营项目<select data-testid="merchant-operating-project-select" value={operatingProjectId} onChange={(event) => setOperatingProjectId(event.target.value)}><option value="">请选择项目</option>{(operating.view.projectOptions ?? []).map((row) => <option key={row.projectId} value={row.projectId}>{display(row.projectName ?? row.projectId)}</option>)}</select></label>
                  <label>理由<input data-testid="merchant-operating-project-reason" value={operatingReason} onChange={(event) => setOperatingReason(event.target.value)} placeholder="必填" /></label>
                  <label>最终审批人<select data-testid="merchant-operating-project-approver" value={operatingApprover} onChange={(event) => setOperatingApprover(event.target.value)}><option value="">不指定（直接上级审批）</option>{approverUsers.map((user) => <option key={user.id} value={user.id}>{user.username ?? user.id}（{shortId(user.id)}）</option>)}</select></label>
                  <div className="admin-form-row"><button type="button" data-testid="merchant-operating-project-submit" onClick={() => void requestOperatingProject()}>提交加经营项目申请</button></div>
                </div>
              </Can>
            </>
          ) : null}
          {operatingError ? <div className="admin-inline-error" data-testid="merchant-operating-project-error">{operatingError}</div> : null}
          <div className="admin-form-row"><button type="button" data-testid="merchant-operating-projects-close" onClick={() => setOperating(null)}>关闭</button></div>
        </div>
      ) : null}
    </>
  );

  const projectsSection = (
    <>
      <div className="admin-section-title">项目档案</div>
      <div className="admin-table-scroll" data-testid="organization-projects-section">
        <table className="admin-table" data-testid="organization-projects-table">
          <thead><tr><th>项目</th><th>编码</th><th>业态</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.target === event.currentTarget && (event.key === "Enter" || event.key === " ")) { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td>{formatBusinessStatus(row.venueType)}</td><td>{formatProjectStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="organization-projects-pager" page={projectRows.page} pageCount={projectRows.pageCount} total={projectRows.total} onPrev={projectRows.prev} onNext={projectRows.next} />
      </div>
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "entities" ? entityRows.total : kpiFocus.table === "projects" ? projectRows.total : merchantStatusFilter ? visibleMerchants.length : merchantRows.total) : 0;

  return (
    <section data-testid="organization-merchant-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      {focusObjectId && (
        <div className="admin-state" data-testid="organization-merchant-focus">
          {!loading && !error && (!kpiFocus || kpiFocus.table === "merchants") && visibleMerchants.some((row) => row.id === focusObjectId)
            ? <>已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属）</>
            : "当前列表尚未定位到该商户，请检查筛选或翻页"}
        </div>
      )}
      <TechDetails><div className="admin-generic-evidence" data-testid="organization-merchant-evidence">{TEXT.evidence}</div></TechDetails>
      {formError ? <div className="admin-inline-error" data-testid="organization-merchant-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {entityRows.total === 0 && merchantRows.total === 0 && projectRows.total === 0 ? (
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
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-entities" onClick={() => enterKpiFocus("entities")}>{entityRows.total}</button>
                <small>组织档案</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>商户档案</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-merchants" onClick={() => enterKpiFocus("merchants")}>{merchantRows.total}</button>
                <small>商户档案数</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>项目档案</span>
                <button type="button" className="admin-kpi-value" data-testid="organization-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projectRows.total}</button>
                <small>项目数</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>本页正常商户</span>
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
            description: "填写商户名称并选择业态（业态决定通票分账时这家商户的份额归哪一类，必填）；业态为其他时须指定主管公司并说明理由。",
            validate: () => (!merchantName.trim() ? TEXT.merchantNameRequired : !merchantType ? TEXT.merchantTypeRequired : supervisingCompanyError()),
            content: (
              <>
                <label>商户名称<input data-testid="organization-merchant-name" value={merchantName} onChange={(event) => setMerchantName(event.target.value)} /></label>
                {pickProject ? <label>所属项目<select data-testid="organization-merchant-project" value={merchantProjectId} onChange={(event) => setMerchantProjectId(event.target.value)}><option value="">本人所在项目</option>{projects.map((row) => <option key={row.id} value={row.id}>{display(row.name)}</option>)}</select></label> : null}
                <label>业态<select data-testid="organization-merchant-type" value={merchantType} onChange={(event) => setMerchantType(event.target.value)}><option value="">请选择业态</option>{MERCHANT_TYPES.map((type) => <option key={type} value={type}>{formatBusinessStatus(type)}</option>)}</select></label>
                {merchantType === "other" ? (
                  <>
                    <label>主管公司<select data-testid="organization-merchant-supervising-company" value={supervisingCompanyId} onChange={(event) => setSupervisingCompanyId(event.target.value)}><option value="">请选择主管公司（在用的城市项目公司／域主管公司）</option>{supervisingCandidates.map((row) => <option key={row.id} value={row.id}>{display(row.entityName)}</option>)}</select></label>
                    <label>指定理由<textarea data-testid="organization-merchant-supervising-reason" value={supervisingCompanyReason} onChange={(event) => setSupervisingCompanyReason(event.target.value)} /></label>
                  </>
                ) : null}
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
          ...(pickProject ? [{ label: "所属项目", value: merchantProjectId ? display(projects.find((row) => row.id === merchantProjectId)?.name) : "本人所在项目" }] : []),
          { label: "业态", value: merchantType ? formatBusinessStatus(merchantType) : "未选择" },
          ...(merchantType === "other" ? [{ label: "主管公司", value: supervisingCompanyName(supervisingCompanyId) }, { label: "指定理由", value: supervisingCompanyReason }] : []),
          { label: "联系人", value: contactName },
          { label: "联系电话", value: contactPhone },
          { label: "合作形式", value: cooperationForm ? cooperationLabel(cooperationForm) : "未选择" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
