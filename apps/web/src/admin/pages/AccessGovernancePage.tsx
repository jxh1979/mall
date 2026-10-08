// 文件版本：V0.10
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { Tabs } from "antd";
import { loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import { RowActions, type RowAction } from "../components/RowActions";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { groupPermissionsByMenuModule, permissionCode } from "../adminRoutes";
import type { AccessUserRow, AdminPageProps, PermissionDefinitionRow } from "../types";

type BoundedInt = { value: number; min: number; max: number };
type GrantPolicy = { windowDays: BoundedInt; positionThreshold: BoundedInt; userThreshold: BoundedInt;
  riskReviews: Record<"low" | "medium" | "high" | "critical", { approvalLevel: string; requiredLevel: number }> };
type RoleRow = { id: string; roleName?: string | null; roleCode?: string | null; status?: string | null; permissionCodes?: string[];
  viewAllProjects?: boolean; viewableProjectIds?: string[] };
type RoleProjectOption = { id: string; name: string; kind: string };
// M736 第二步：角色可查看项目的现状。pendingRemovalIds＝减少在待审的条目；pendingReapprovals＝还没重新审批的持有人。
type RoleViewableProjectsView = { roleId: string; viewAllProjects?: boolean; viewableProjectIds?: string[]; pendingRemovalIds?: string[];
  pendingReapprovals?: Array<{ userId: string; username?: string | null; viewableIds?: string[]; reapprovalPending?: boolean }> };
type RoleEdit = { role: RoleRow; base: RoleViewableProjectsView; viewAll: boolean; ids: string[]; open: boolean; finalApproverId: string };
type PositionRoleTemplateRow = {
  id: string; positionCode: string; staffLevelId: string; roleId?: string | null; templateId?: string | null;
  organizationId?: string | null; status?: string | null; createdBy?: string | null;
  archivedAt?: string | null; deletedAt?: string | null; deleteReviewId?: string | null; deleteReason?: string | null;
};
type AccessGrantRequestRow = {
  id: string; requestType: string; targetUserId: string; targetRoleId?: string | null; templateId?: string | null;
  positionCode?: string | null; staffLevelId?: string | null; permissionCodes?: string[]; expiresAt?: string | null;
  singleUse?: boolean; status: string; reviewId?: string | null; reason?: string | null; requestedBy?: string | null;
  decidedBy?: string | null; decidedAt?: string | null; createdAt?: string | null;
};
type AccessGrantPositionSummaryRow = {
  positionCode: string; staffLevelId?: string | null; permissionCode?: string | null; requestCount: number; recommendation?: string | null;
};
type AccessGrantUserSummaryRow = { userId: string; requestCount: number; summary?: string | null };
type AccessGrantSummary = {
  positionRecommendations?: AccessGrantPositionSummaryRow[];
  userAnomalies?: AccessGrantUserSummaryRow[];
};

const ASSIGN = ["access.role.assign"];

// 岗位/职级码中文化（对抗审计 #4：访问治理表原显英文机读码）。岗位码有限、职级码按 lvl_m<N>_<lower/upper> 规则解析。
const POSITION_LABELS: Record<string, string> = {
  auditor_internal: "内部审计",
  auditor_external: "外部审计",
  outsourced_construction: "外包施工",
  outsourced_facility: "外包设施",
  outsourced_security_cleaning: "外包安保保洁",
  ops_manager: "运营经理",
};
function zhPosition(code?: string | null): string {
  if (!code) return "—";
  // M038-003：未映射的岗位码不裸露英文——中文说明在前，岗位码作为系统内部标识符放括号里。
  return POSITION_LABELS[code] ?? `未登记岗位（${code}）`;
}
// M038-008：职级是速查表写明的 30 档闭集（M1–M10 × 上/中/下），编号与库内 staff_levels 一致；页内生成下拉，不手填。
const STAFF_LEVEL_IDS: string[] = [];
for (let main = 1; main <= 10; main += 1) {
  for (const band of ["lower", "middle", "upper"]) STAFF_LEVEL_IDS.push(`lvl_m${main}_${band}`);
}
function staffLevelOptions() {
  return STAFF_LEVEL_IDS.map((id) => <option key={id} value={id}>{zhLevel(id)}</option>);
}
// M038-001：授权申请的状态与类型按后端闭集在页内给全中文（公共字典缺「已生效」与三种内部类型）。
const GRANT_STATUS_LABELS: Record<string, string> = { pending: "待处理", applied: "已生效", rejected: "已驳回" };
const GRANT_TYPE_LABELS: Record<string, string> = {
  user_role: "用户角色", user_position: "用户职位", temporary_permission: "临时权限",
  role_permission: "角色权限", user_scope_change: "人员范围变更", position_high_risk_release: "岗位高危权限释放",
  user_permission_grant: "员工单独授予", user_permission_deny_removal: "解除员工显式拒绝",
  role_project_reapproval: "重新审批可查看项目",
};
// M002-F01：员工单独授予 / 解除员工显式拒绝两类申请只填权限码（不填过期时间、不带单次有效）。
// M002-F20：岗位高危权限释放（释放单被驳回后对同一挂起再次发起）同样只填权限码。
const OVERRIDE_GRANT_TYPES = ["user_permission_grant", "user_permission_deny_removal", "position_high_risk_release"];
function grantNeedsCodes(requestType: string): boolean {
  return requestType === "temporary_permission" || OVERRIDE_GRANT_TYPES.includes(requestType);
}
function grantStatusLabel(code?: string | null): string {
  return code ? GRANT_STATUS_LABELS[code] ?? formatBusinessStatus(code) : "—";
}
function grantTypeLabel(code?: string | null): string {
  return code ? GRANT_TYPE_LABELS[code] ?? formatBusinessStatus(code) : "—";
}
function zhLevel(code?: string | null): string {
  if (!code) return "—";
  const m = /^lvl_m(\d+)(?:_(lower|middle|upper))?$/.exec(code);
  if (m) return m[2] ? `M${m[1]}·${m[2] === "upper" ? "上" : m[2] === "middle" ? "中" : "下"}` : `M${m[1]}`;
  return code;
}
const GRANT_APPLY = ["access.grant_request.apply"];
const GRANT_VIEW = ["access.grant_request.view"];
const GRANT_REVIEW = ["access.grant_request.review", "governance.supervisor_review.approve"];
// M736：新建角色要按菜单模块勾权限点，除建角色权限外还要能读权限字典。
const ROLE_CREATE = ["access.role.manage", "access.permission.view"];
// M736（操作员 2026-10-03 裁决「三类各自单列」）：逐个勾选的清单里项目、城市项目公司、在用的域主管公司各列为独立条目，勾哪条只管哪条。
const ROLE_PROJECT_KINDS: Array<[string, string]> = [["project", "项目"], ["project_company", "城市项目公司"], ["management_company", "域主管公司"]];
const NEW_ROLE = { roleName: "", description: "", permissionCodes: [] as string[], viewAllProjects: false, viewableProjectIds: [] as string[] };
const ROLE_NAME_REQUIRED = "角色名称必填，去掉首尾空白后不能超过 120 个字";
// M736 第二步：待审减少、待重新审批的条目里代表「可以查看全部项目」这一勾（与后端 AuthorizationService.ALL_PROJECTS 同值）。
const ALL_PROJECTS = "*";

function splitTokens(value: string) {
  return value.split(/[\n,，;；\s]+/).map((item) => item.trim()).filter(Boolean);
}

function toIso(value: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toISOString();
}

function centFromYuan(value: string) {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.round(parsed * 100) : undefined;
}

function roleLabel(row: RoleRow | undefined) {
  return row ? `${row.roleName ?? row.roleCode ?? row.id} (${row.id})` : "-";
}

function viewableProjectsLabel(row: RoleRow) {
  if (row.viewAllProjects) return "全部项目";
  return row.viewableProjectIds?.length ? `已选 ${row.viewableProjectIds.length} 个` : "—";
}

function toggled(list: string[], value: string, checked: boolean) {
  const set = new Set(list);
  if (checked) set.add(value);
  else set.delete(value);
  return [...set].sort();
}

function statusBadge(status?: string | null, label: (code: string) => string = formatBusinessStatus) {
  const value = status || "";
  const kind = value === "approved" || value === "active" ? "is-ok" : value === "rejected" ? "is-danger" : value === "pending" ? "is-warn" : "is-info";
  // 徽标文字中文化（业务HTML零英文规则）；颜色仍按原始码判定。
  return <span className={`admin-badge ${kind}`}>{value ? label(value) : "—"}</span>;
}

// M736 第二步：「改可查看项目」弹窗这次的增减（页面拿编辑后的状态与打开时读到的现状算，后端在锁内再核）。
// 勾着全部时只看「全部」这一勾；逐个勾选的清单始终保留，所以去掉全部时清单里没动的条目不算减少。
function viewableProjectsDelta(base: RoleViewableProjectsView, viewAll: boolean, ids: string[]) {
  const baseIds = base.viewableProjectIds ?? [];
  if (viewAll) return { addAll: !base.viewAllProjects, add: [] as string[], removeAll: false, remove: [] as string[] };
  return {
    addAll: false,
    add: ids.filter((id) => !baseIds.includes(id)),
    removeAll: Boolean(base.viewAllProjects),
    remove: baseIds.filter((id) => !ids.includes(id)),
  };
}

// M736 第二步：「可以查看全部项目」一行与三组逐项勾选行（放在 <tbody> 里），新建角色引导与「改可查看项目」弹窗共用。
// viewAllLocked、lockedIds＝减少在待审的条目：勾选框禁用，后面标「（减少待审批）」。
function ViewableProjectRows({ prefix, viewAll, ids, open, options, lockedIds = [], viewAllLocked = false, onViewAll, onIds, onOpen }: {
  prefix: string; viewAll: boolean; ids: string[]; open: boolean; options: RoleProjectOption[];
  lockedIds?: string[]; viewAllLocked?: boolean;
  onViewAll: (checked: boolean) => void; onIds: (ids: string[]) => void; onOpen: (open: boolean) => void;
}) {
  return (
    <>
      <tr>
        <th scope="row">可以查看全部项目</th>
        <td>
          <input type="checkbox" data-testid={`${prefix}-view-all`} aria-label="可以查看全部项目" checked={viewAll} disabled={viewAllLocked} onChange={(event) => onViewAll(event.target.checked)} />
          {viewAllLocked ? "（减少待审批）" : null}
          <button type="button" data-testid={`${prefix}-projects-toggle`} aria-label="展开逐个勾选项目" aria-expanded={open} disabled={viewAll} onClick={() => onOpen(!open)}>{open ? "－" : "＋"}</button>
        </td>
      </tr>
      {open ? ROLE_PROJECT_KINDS.map(([kind, label]) => (
        <tr key={kind} data-testid={`${prefix}-projects-${kind}`}>
          <th scope="row">{label}</th>
          <td>{options.filter((option) => option.kind === kind).map((option) => (
            <label key={option.id} className="admin-checkbox">
              <input type="checkbox" data-testid={`${prefix}-project-${option.id}`} checked={ids.includes(option.id)} disabled={lockedIds.includes(option.id)} onChange={(event) => onIds(toggled(ids, option.id, event.target.checked))} />{option.name}{lockedIds.includes(option.id) ? "（减少待审批）" : null}
            </label>
          ))}</td>
        </tr>
      )) : null}
    </>
  );
}

export function AccessGovernancePage({ api, user, setActionResult, setGlobalError, effectivePermissions, refreshEffectivePermissions }: AdminPageProps) {
  // M038-005：页内写动作统一走 runWrite——提交中置忙（防双击发两笔）、失败把后端中文消息报到页面。
  const [writeBusy, setWriteBusy] = React.useState(false);
  const writeBusyRef = React.useRef(false);
  async function runWrite(action: () => Promise<void>, fallbackMessage: string) {
    if (writeBusyRef.current) return;
    writeBusyRef.current = true;
    setWriteBusy(true);
    setGlobalError(null);
    try {
      await action();
    } catch (actionError) {
      setGlobalError(actionError instanceof Error ? actionError.message : fallbackMessage);
    } finally {
      writeBusyRef.current = false;
      setWriteBusy(false);
    }
  }
  const [users, setUsers] = React.useState<AccessUserRow[]>([]);
  const [roles, setRoles] = React.useState<RoleRow[]>([]);
  const [positionTemplates, setPositionTemplates] = React.useState<PositionRoleTemplateRow[]>([]);
  const [includeArchivedTemplates, setIncludeArchivedTemplates] = React.useState(false);
  const [lifecycleReason, setLifecycleReason] = React.useState("");
  const [grantRequests, setGrantRequests] = React.useState<AccessGrantRequestRow[]>([]);
  const [grantPage, setGrantPage] = React.useState(1);
  const [grantTotal, setGrantTotal] = React.useState(0);
  const [grantPageBusy, setGrantPageBusy] = React.useState(false);
  const [grantPolicy, setGrantPolicy] = React.useState<GrantPolicy | null>(null);
  const [grantPolicyReason, setGrantPolicyReason] = React.useState("");
  const [summary, setSummary] = React.useState<AccessGrantSummary | null>(null);
  const [summaryNotice, setSummaryNotice] = React.useState<string | null>(null);
  const [templateForm, setTemplateForm] = React.useState({ id: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", roleId: "", templateId: "", status: "active" });
  const [positionForm, setPositionForm] = React.useState({ userId: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", baseSalaryYuan: "", salaryDetail: "", operatorNote: "" });
  // M087-A-04：人员范围变更表单（所属商户 / 跨项目）。
  const [scopeForm, setScopeForm] = React.useState({ userId: "", organizationId: "", merchantId: "", positionCode: "", staffLevelId: "", reason: "", finalApproverId: "" });
  const [roleForm, setRoleForm] = React.useState({ userId: "", roleId: "", operatorNote: "" });
  const [overrideForm, setOverrideForm] = React.useState({ userId: "", permissionCodes: "", effect: "deny", reason: "" });
  const [grantForm, setGrantForm] = React.useState({
    requestType: "temporary_permission", targetUserId: "", roleId: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower",
    permissionCodes: "access.role.assign", expiresAt: "", singleUse: true, reason: "",
  });
  const [reviewForm, setReviewForm] = React.useState({ comment: "", evidenceAttachmentIds: "" });
  // 行明细抽屉：四张业务表（岗位模板/授权申请/智能总结/角色）整行可点 → 该行完整字段（枚举中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-9：岗位角色模板收起为主按钮 + 分步引导弹窗（其余授权申请/审批工作流原样不动）。
  const [templateOpen, setTemplateOpen] = React.useState(false);
  const [templateError, setTemplateError] = React.useState("");
  const [templateSaving, setTemplateSaving] = React.useState(false);
  // 引导式流程 phase-11：授权申请收起为主按钮 + 分步引导弹窗（岗位模板弹窗/审批/定岗/装配角色原样不动）。
  const [grantOpen, setGrantOpen] = React.useState(false);
  const [grantError, setGrantError] = React.useState("");
  const [grantSaving, setGrantSaving] = React.useState(false);
  // M736（操作员 2026-10-01 说明「权限配置表格当中有一个可以查看全部项目选项……这个可以查看项目的选项，可以点击+打开」）：
  // 「④ 角色」标签的角色表与「＋ 新建角色」两步引导（名称与说明、权限配置）。
  const [roleCreateOpen, setRoleCreateOpen] = React.useState(false);
  const [roleCreateError, setRoleCreateError] = React.useState("");
  const [newRole, setNewRole] = React.useState(NEW_ROLE);
  const [newRoleProjectsOpen, setNewRoleProjectsOpen] = React.useState(false);
  const [rolePermissions, setRolePermissions] = React.useState<PermissionDefinitionRow[]>([]);
  // 可查看项目的候选条目：null＝还没读过。
  const [roleProjectOptions, setRoleProjectOptions] = React.useState<RoleProjectOption[] | null>(null);
  const [expandedRoleId, setExpandedRoleId] = React.useState("");
  // M736 第二步（操作员 2026-10-08 确认的治理地图追加句「可跨项目角色分配出去以后，可以直接在原角色上增加或减少可查看的项目」）：
  // 角色行「改可查看项目」弹窗。null＝没打开；base＝打开时读到的现状，viewAll、ids＝正在编辑的勾选。
  const [roleEdit, setRoleEdit] = React.useState<RoleEdit | null>(null);
  const [roleEditError, setRoleEditError] = React.useState("");
  const roleEditDelta = roleEdit ? viewableProjectsDelta(roleEdit.base, roleEdit.viewAll, roleEdit.ids) : null;
  const groupedRolePermissions = React.useMemo(() => groupPermissionsByMenuModule(rolePermissions), [rolePermissions]);

  // 无权限副资源不请求（根因修）：授权列表/概要卡需 access.grant_request.view、用户清单需 access.user.view、角色清单需 access.role.view，
  // 本页 gate 为 access.role.assign 或 access.role.manage 其一（M736-F08），不含这三码——缺权限裸 await 会 403 reject 整个 Promise.all 致整页报错，故一并门控
  //（position-role-templates 需 access.role.assign，按 canRoleAssign 门控，见下方 M002-F34 说明）。
  const canGrantView = hasPermissions(effectivePermissions, ["access.grant_request.view"]);
  const canUserView = hasPermissions(effectivePermissions, ["access.user.view"]);
  const canRoleView = hasPermissions(effectivePermissions, ["access.role.view"]);
  // M736：权限字典与可查看项目候选不随页面加载请求——打开「＋ 新建角色」时各读一次；角色表展开「＋」要显示条目名称而候选还没读过时也读。
  // 候选清单接口按 access.role.manage 门控，没有这个码的人不发请求（会 403），角色表只显示个数。
  const canRoleManage = hasPermissions(effectivePermissions, ["access.role.manage"]);
  const canRoleCreate = hasPermissions(effectivePermissions, ROLE_CREATE);
  // M736 第二步：「改可查看项目」弹窗里「发起重新审批」按钮按申请权限显隐。
  const canGrantApply = hasPermissions(effectivePermissions, GRANT_APPLY);
  // M002-F37（治理地图第 651 行）：员工单独授予、解除员工显式拒绝、岗位模板高危权限释放三类申请只能由具备分配/取消分配角色权限的人发起。
  const canRoleAssign = hasPermissions(effectivePermissions, ASSIGN);
  // M002-F34（依据 M002-F08 裁决「本人可以直接解除，不会卡死」）：最高权限本人拒绝了自己的 access.role.assign 时仍能进本页，
  // 同码的 position-role-templates 此时会 403，一并按 canRoleAssign 门控，免得整页报错、盖住解除入口。
  const isMaster = user.id === "usr_master_authority" && user.username === "master-authority";
  const { loading, error, reload } = useAdminResource(
    async () => {
      const [userRows, roleRows, templateRows, requestRows, grantSummary] = await Promise.all([
        canUserView ? api.get<AccessUserRow[]>("/api/v1/access/users") : Promise.resolve([] as AccessUserRow[]),
        canRoleView ? api.get<RoleRow[]>("/api/v1/access/roles") : Promise.resolve([] as RoleRow[]),
        canRoleAssign ? api.get<PositionRoleTemplateRow[]>(`/api/v1/access/position-role-templates?include_archived=${includeArchivedTemplates}`) : Promise.resolve([] as PositionRoleTemplateRow[]),
        canGrantView ? api.getPaged<AccessGrantRequestRow>(`/api/v1/access/grant-requests?page=${grantPage}`) : Promise.resolve({ items: [] as AccessGrantRequestRow[], total: 0, page: 1, size: 50 }),
        // 智能总结=纯统计副资源（GRANT_VIEW 权限，无权账号可能 403）：单独降级，不拖垮角色装配/审批主流程。
        loadSecondaryIfPermitted(canGrantView, () => api.get<AccessGrantSummary>("/api/v1/access/grant-requests/summary"), null as AccessGrantSummary | null),
      ]);
      setUsers(userRows ?? []);
      setRoles(roleRows ?? []);
      setPositionTemplates(templateRows ?? []);
      setGrantRequests(requestRows.items);
      setGrantPage(requestRows.page);
      setGrantTotal(requestRows.total);
      setSummary(grantSummary.data);
      setSummaryNotice(grantSummary.notice);
    },
    { onError: setGlobalError, fallbackMessage: "访问治理数据读取失败", onStart: () => setGlobalError(null) },
  );
  // M002-F49（M002-F34／F08 裁决「本人可以直接解除，不会卡死」）：本人改了自身权限后，等刷新后的有效集渲染进来再重读，
  // 副资源门控按新有效集计算——按旧有效集请求已被拒绝的副资源会 403、盖住整页连同本人的解除入口。
  const [reloadAfterOwnChange, setReloadAfterOwnChange] = React.useState(false);
  React.useEffect(() => {
    if (!reloadAfterOwnChange) return;
    setReloadAfterOwnChange(false);
    void reload();
  }, [reloadAfterOwnChange, reload]);

  React.useEffect(() => {
    const firstUser = users[0]?.id ?? "";
    const firstRole = roles[0]?.id ?? "";
    if (firstUser) {
      setPositionForm((current) => current.userId ? current : { ...current, userId: firstUser });
      setRoleForm((current) => current.userId ? current : { ...current, userId: firstUser });
      setGrantForm((current) => current.targetUserId ? current : { ...current, targetUserId: firstUser });
    }
    if (firstRole) {
      setTemplateForm((current) => current.roleId ? current : { ...current, roleId: firstRole });
      setRoleForm((current) => current.roleId ? current : { ...current, roleId: firstRole });
      setGrantForm((current) => current.roleId ? current : { ...current, roleId: firstRole });
    }
  }, [roles, users]);

  function selectTemplate(row: PositionRoleTemplateRow) {
    setTemplateForm({
      id: row.id,
      positionCode: row.positionCode,
      staffLevelId: row.staffLevelId,
      roleId: row.roleId ?? "",
      templateId: row.templateId ?? "",
      status: row.status ?? "active",
    });
    setPositionForm((current) => ({ ...current, positionCode: row.positionCode, staffLevelId: row.staffLevelId }));
    // 选中=编辑该模板：直接打开引导弹窗（此时 id 非空走 PUT 更新）。
    setTemplateError("");
    setTemplateOpen(true);
  }

  const TEMPLATE_REQUIRED = "岗位模板需填写岗位、职级，并至少绑定角色或模板";

  async function savePositionTemplate() {
    if (!templateForm.positionCode.trim() || !templateForm.staffLevelId.trim() || (!templateForm.roleId.trim() && !templateForm.templateId.trim())) {
      setTemplateError(TEMPLATE_REQUIRED);
      setGlobalError(TEMPLATE_REQUIRED);
      return;
    }
    const body = {
      positionCode: templateForm.positionCode.trim(),
      staffLevelId: templateForm.staffLevelId.trim(),
      roleId: templateForm.roleId.trim() || undefined,
      templateId: templateForm.templateId.trim() || undefined,
      status: templateForm.status,
      operatorNote: "后台岗位角色模板维护",
    };
    // 提交失败留弹窗内显错（templateError），成功才关弹窗（照 phase-8 范式）。
    setTemplateError("");
    setGlobalError(null);
    setTemplateSaving(true);
    try {
      const saved = templateForm.id
        ? await api.put<PositionRoleTemplateRow>(`/api/v1/access/position-role-templates/${encodeURIComponent(templateForm.id)}`, body)
        : await api.post<PositionRoleTemplateRow>("/api/v1/access/position-role-templates", body);
      setActionResult(`岗位模板已保存：${saved.id}`);
      setTemplateForm({ ...templateForm, id: "" });
      setTemplateOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "岗位角色模板保存失败";
      setTemplateError(message);
      setGlobalError(message);
    } finally {
      setTemplateSaving(false);
    }
  }

  async function loadGrantPage(page: number) {
    setGrantPageBusy(true);
    try {
      const result = await api.getPaged<AccessGrantRequestRow>(`/api/v1/access/grant-requests?page=${page}`);
      setGrantRequests(result.items); setGrantPage(result.page); setGrantTotal(result.total);
    } catch (error) { setGlobalError(error instanceof Error ? error.message : "授权申请读取失败"); }
    finally { setGrantPageBusy(false); }
  }

  async function loadGrantPolicy() {
    try { setGrantPolicy(await api.get<GrantPolicy>("/api/v1/access/grant-policy")); }
    catch (error) { setGlobalError(error instanceof Error ? error.message : "授权政策读取失败"); }
  }

  async function saveGrantPolicy() {
    if (!grantPolicy || !grantPolicyReason.trim()) { setGlobalError("请读取政策并填写修改理由。"); return; }
    try {
      setGrantPolicy(await api.post<GrantPolicy>("/api/v1/access/grant-policy", {
        policy: grantPolicy, operatorNote: grantPolicyReason.trim(),
      }));
      setActionResult("授权政策已保存");
      await reload();
    } catch (error) { setGlobalError(error instanceof Error ? error.message : "授权政策保存失败"); }
  }

  async function changePositionLifecycle(id: string, action: string) {
    if (!lifecycleReason.trim()) { setGlobalError("请填写存档、删除或恢复理由。"); return; }
    try {
      await api.post(`/api/v1/access/position-role-templates/${encodeURIComponent(id)}/lifecycle`, {
        action, reason: lifecycleReason.trim(),
      });
      setActionResult(`岗位模板状态已更新：${id}`);
      await reload();
    } catch (error) { setGlobalError(error instanceof Error ? error.message : "岗位模板处理失败"); }
  }

  async function showArchivedTemplates(include: boolean) {
    try {
      const rows = await api.get<PositionRoleTemplateRow[]>(`/api/v1/access/position-role-templates?include_archived=${include}`);
      setPositionTemplates(rows); setIncludeArchivedTemplates(include);
    } catch (error) { setGlobalError(error instanceof Error ? error.message : "存档模板读取失败"); }
  }

  async function assignPosition() {
    if (!positionForm.userId.trim() || !positionForm.positionCode.trim() || !positionForm.staffLevelId.trim()) {
      setGlobalError("定职位需填写用户、岗位和职级");
      return;
    }
    await api.put(`/api/v1/access/users/${encodeURIComponent(positionForm.userId.trim())}/position`, {
      positionCode: positionForm.positionCode.trim(),
      staffLevelId: positionForm.staffLevelId.trim(),
      baseSalaryAmountCent: centFromYuan(positionForm.baseSalaryYuan),
      salaryCurrency: positionForm.baseSalaryYuan.trim() ? "CNY" : undefined,
      salaryDetail: positionForm.salaryDetail.trim() || undefined,
      operatorNote: positionForm.operatorNote.trim() || "后台定岗并按模板自动装配",
    });
    setActionResult(`已为用户 ${positionForm.userId.trim()} 定岗并按岗位模板自动适配权限`);
    await reload();
  }

  // M087-A-04（PGM-1.10.1「保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效」）：
  // 人员范围变更写入口——提交后旧派生立即失效，新范围岗位权限进主管链；可指定主线最终审批人（PGM-1.7.1）。
  async function changeUserScope() {
    const userId = (scopeForm.userId || positionForm.userId).trim();
    if (!userId) {
      setGlobalError("范围变更需选择用户");
      return;
    }
    if (!scopeForm.organizationId.trim() && !scopeForm.merchantId.trim()) {
      setGlobalError("范围变更需填写新的所属项目或商户");
      return;
    }
    if (!scopeForm.reason.trim()) {
      setGlobalError("范围变更需填写变更理由");
      return;
    }
    const result = await api.put<{ id?: string; reviewId?: string | null }>(`/api/v1/access/users/${encodeURIComponent(userId)}/scope`, {
      organizationId: scopeForm.organizationId.trim() || undefined,
      merchantId: scopeForm.merchantId.trim() || undefined,
      positionCode: scopeForm.positionCode.trim() || undefined,
      staffLevelId: scopeForm.staffLevelId.trim() || undefined,
      reason: scopeForm.reason.trim(),
      finalApproverId: scopeForm.finalApproverId.trim() || undefined,
      operatorNote: "后台人员范围变更",
    });
    setActionResult(`已提交用户 ${userId} 的范围变更：旧派生权限已失效，新范围待审批（复核单 ${result?.reviewId ?? "未生成"}）`);
    await reload();
  }

  async function roleAssignment(kind: "assign" | "unassign") {
    if (!roleForm.userId.trim() || !roleForm.roleId.trim()) {
      setGlobalError("直接装配角色需选择用户与角色");
      return;
    }
    const path = kind === "assign" ? "/api/v1/access/role-assignments" : "/api/v1/access/role-assignments/unassign";
    await api.post(path, {
      userId: roleForm.userId.trim(),
      roleId: roleForm.roleId.trim(),
      operatorNote: roleForm.operatorNote.trim() || `后台${kind === "assign" ? "装配" : "卸载"}角色`,
    });
    setActionResult(`已为用户 ${roleForm.userId.trim()} ${kind === "assign" ? "装配" : "卸载"}角色 ${roleForm.roleId.trim()}`);
    await reload();
  }

  // M006-G-01（PGM-1.10.1「员工单独授予 − 显式拒绝」「员工级完全自定义」）：员工级覆盖写入口。
  // 显式拒绝立即生效；单独授予与直接装配同口径（高危须走授权申请）。
  async function userPermissionOverride(kind: "set" | "remove") {
    const codes = splitTokens(overrideForm.permissionCodes);
    if (!overrideForm.userId.trim() || codes.length === 0) {
      setGlobalError("员工级覆盖需选择用户并填写权限码");
      return;
    }
    const path = `/api/v1/access/users/${encodeURIComponent(overrideForm.userId.trim())}/permission-overrides${kind === "remove" ? "/remove" : ""}`;
    await api.post(path, {
      permissionCodes: codes,
      effect: overrideForm.effect,
      reason: overrideForm.reason.trim() || (overrideForm.effect === "deny" ? "后台显式拒绝" : "后台单独授予"),
      operatorNote: overrideForm.reason.trim() || "后台员工级覆盖",
    });
    setActionResult(`已${kind === "set" ? "设置" : "移除"}用户 ${overrideForm.userId.trim()} 的${overrideForm.effect === "deny" ? "显式拒绝" : "单独授予"}：${codes.join("、")}`);
    if (overrideForm.userId.trim() === user.id && refreshEffectivePermissions) {
      await refreshEffectivePermissions(); // M002-F49
      setReloadAfterOwnChange(true);
    } else {
      await reload();
    }
  }

  const GRANT_USER_REQUIRED = "授权申请需填写目标用户";
  const GRANT_TEMP_REQUIRED = "临时高危授权需填写权限码和过期时间";
  const GRANT_CODES_REQUIRED = "员工单独授予、解除显式拒绝与岗位高危权限释放需填写权限码";
  // M002-F01：临时权限要权限码与过期时间；员工单独授予 / 解除显式拒绝只要权限码。
  function grantDetailError(): string | null {
    if (grantForm.requestType === "temporary_permission" && (!grantForm.permissionCodes.trim() || !grantForm.expiresAt)) return GRANT_TEMP_REQUIRED;
    if (OVERRIDE_GRANT_TYPES.includes(grantForm.requestType) && !splitTokens(grantForm.permissionCodes).length) return GRANT_CODES_REQUIRED;
    return null;
  }

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错（grantError），成功才关。原接口/请求体/双保险校验保留。
  async function applyGrantRequest() {
    if (!grantForm.targetUserId.trim()) {
      setGrantError(GRANT_USER_REQUIRED);
      return;
    }
    const detailError = grantDetailError();
    if (detailError) {
      setGrantError(detailError);
      return;
    }
    setGrantError("");
    setGlobalError(null);
    setGrantSaving(true);
    try {
      const result = await api.post<AccessGrantRequestRow>("/api/v1/access/grant-requests", {
        requestType: grantForm.requestType,
        targetUserId: grantForm.targetUserId.trim(),
        roleId: grantForm.requestType === "user_role" ? grantForm.roleId.trim() || undefined : undefined,
        positionCode: grantForm.requestType === "user_position" ? grantForm.positionCode.trim() : undefined,
        staffLevelId: grantForm.requestType === "user_position" ? grantForm.staffLevelId.trim() : undefined,
        permissionCodes: grantNeedsCodes(grantForm.requestType) ? splitTokens(grantForm.permissionCodes) : undefined,
        expiresAt: grantForm.requestType === "temporary_permission" ? toIso(grantForm.expiresAt) : undefined,
        singleUse: grantForm.requestType === "temporary_permission" ? grantForm.singleUse : undefined,
        reason: grantForm.reason.trim() || "后台高危授权申请",
        operatorNote: grantForm.reason.trim() || "后台高危授权申请",
      });
      setActionResult(`授权申请已提交：${result.id}（${grantStatusLabel(result.status)}）`);
      setGrantOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "高危授权申请失败";
      setGrantError(message);
      setGlobalError(message);
    } finally {
      setGrantSaving(false);
    }
  }

  async function reviewGrant(row: AccessGrantRequestRow, decision: "approve" | "reject") {
    const result = await api.post<AccessGrantRequestRow>(`/api/v1/access/grant-requests/${encodeURIComponent(row.id)}/review`, {
      decision,
      // M038-001：不填意见时留痕用中文，不把 approve／reject 当意见。
      comment: reviewForm.comment.trim() || (decision === "approve" ? "通过" : "驳回"),
      operatorNote: reviewForm.comment.trim() || (decision === "approve" ? "通过" : "驳回"),
      evidenceAttachmentIds: splitTokens(reviewForm.evidenceAttachmentIds),
    });
    setActionResult(`授权申请 ${row.id} 已${decision === "approve" ? "通过" : "驳回"}（${grantStatusLabel(result.status)}）`);
    await reload();
  }

  function projectNames(ids: string[]) {
    return ids.map((id) => id === ALL_PROJECTS ? "全部项目" : roleProjectOptions?.find((option) => option.id === id)?.name ?? id).join("、");
  }

  async function openRoleCreate() {
    setNewRole(NEW_ROLE);
    setNewRoleProjectsOpen(false);
    setRoleCreateError("");
    setRoleCreateOpen(true);
    try {
      const [permissionRows, optionRows] = await Promise.all([
        api.get<PermissionDefinitionRow[]>("/api/v1/access/permissions"),
        api.get<RoleProjectOption[]>("/api/v1/access/role-project-options"),
      ]);
      setRolePermissions((permissionRows ?? []).filter((item) => permissionCode(item)));
      setRoleProjectOptions(optionRows ?? []);
    } catch (loadError) {
      setRoleCreateError(loadError instanceof Error ? loadError.message : "权限点或可查看项目读取失败");
    }
  }

  async function toggleRoleProjects(roleId: string) {
    const opening = expandedRoleId !== roleId;
    setExpandedRoleId(opening ? roleId : "");
    if (!opening || roleProjectOptions !== null) return;
    try {
      setRoleProjectOptions((await api.get<RoleProjectOption[]>("/api/v1/access/role-project-options")) ?? []);
    } catch (loadError) { setGlobalError(loadError instanceof Error ? loadError.message : "可查看项目读取失败"); }
  }

  // 提交在确认步触发，经 runWrite：失败时弹窗留开并在弹窗内显错（roleCreateError），再抛给 runWrite 报到页面；成功才关。
  // 请求编号由 adminClient 按「一次操作」带（shared/operationKey），页面不另存。
  async function createRole() {
    setRoleCreateError("");
    try {
      const saved = await api.post<RoleRow>("/api/v1/access/roles", {
        roleName: newRole.roleName.trim(),
        description: newRole.description.trim() || undefined,
        permissionCodes: newRole.permissionCodes,
        viewAllProjects: newRole.viewAllProjects,
        viewableProjectIds: newRole.viewAllProjects ? undefined : newRole.viewableProjectIds,
        operatorNote: "后台新建角色",
      });
      setActionResult(`角色已创建：${saved.id}`);
      setRoleCreateOpen(false);
      await reload();
    } catch (actionError) {
      setRoleCreateError(actionError instanceof Error ? actionError.message : "新建角色失败");
      throw actionError;
    }
  }

  const roleViewableProjectsPath = (roleId: string) => `/api/v1/access/roles/${encodeURIComponent(roleId)}/viewable-projects`;

  // M736 第二步：打开「改可查看项目」——读现状；候选清单还没读过时读一次。
  // 预填按返回的编号原样放进状态，不按候选清单过滤：候选里已经没有的旧条目不能在提交时被当成减少。
  async function openRoleEdit(row: RoleRow) {
    setRoleEditError("");
    try {
      const [base, optionRows] = await Promise.all([
        api.get<RoleViewableProjectsView>(roleViewableProjectsPath(row.id)),
        roleProjectOptions === null ? api.get<RoleProjectOption[]>("/api/v1/access/role-project-options") : Promise.resolve(roleProjectOptions),
      ]);
      setRoleProjectOptions(optionRows ?? []);
      const viewAll = Boolean(base.viewAllProjects);
      const ids = base.viewableProjectIds ?? [];
      setRoleEdit({ role: row, base, viewAll, ids, open: !viewAll && ids.length > 0, finalApproverId: "" });
    } catch (loadError) { setGlobalError(loadError instanceof Error ? loadError.message : "可查看项目读取失败"); }
  }

  // 提交这次的增减（不是改后的整份清单），经 runWrite：失败时弹窗留开并在弹窗内显错（roleEditError），再抛给 runWrite 报到页面；成功才关。
  // 结果提示用同一句追加句里的说法（「增加的，所有持有人要各自重新审批通过，才能查看新增的项目；减少的，也要审批通过后才生效，审批期间持有人仍能查看。」）。
  async function saveRoleViewableProjects() {
    if (!roleEdit || !roleEditDelta) return;
    const delta = roleEditDelta;
    const reduces = delta.removeAll || delta.remove.length > 0;
    setRoleEditError("");
    try {
      await api.put<RoleViewableProjectsView>(roleViewableProjectsPath(roleEdit.role.id), {
        addViewAllProjects: delta.addAll || undefined,
        addViewableProjectIds: delta.add.length ? delta.add : undefined,
        removeViewAllProjects: delta.removeAll || undefined,
        removeViewableProjectIds: delta.remove.length ? delta.remove : undefined,
        finalApproverId: reduces ? roleEdit.finalApproverId.trim() || undefined : undefined,
        operatorNote: "后台改可查看项目",
      });
      setActionResult([
        delta.add.length ? `已增加 ${delta.add.length} 个可查看项目：所有持有人要各自重新审批通过，才能查看新增的项目。` : "",
        delta.addAll ? "已勾选“可以查看全部项目”：所有持有人要各自重新审批通过，才能查看新增的项目。" : "",
        delta.remove.length ? `减少的 ${delta.remove.length} 个可查看项目已提交审批：审批通过后才生效，审批期间持有人仍能查看。` : "",
        delta.removeAll ? "去掉“可以查看全部项目”已提交审批：审批通过后才生效，审批期间持有人仍能查看。" : "",
      ].join(""));
      setRoleEdit(null);
      await reload();
    } catch (actionError) {
      setRoleEditError(actionError instanceof Error ? actionError.message : "改可查看项目失败");
      throw actionError;
    }
  }

  // 对一个持有人发起重新审批（授权申请类型 role_project_reapproval，一张申请一个持有人）；成功后重读现状，只刷新「待重新审批的持有人」这一节，
  // 正在编辑的勾选与打开时读到的角色现状不动（角色在这期间被别人改了的，提交时由后端按 409 让他刷新）。
  async function requestRoleProjectReapproval(userId: string) {
    if (!roleEdit) return;
    const roleId = roleEdit.role.id;
    setRoleEditError("");
    try {
      const result = await api.post<AccessGrantRequestRow>("/api/v1/access/grant-requests", {
        requestType: "role_project_reapproval",
        targetUserId: userId,
        roleId,
        reason: "后台发起重新审批可查看项目",
        operatorNote: "后台发起重新审批可查看项目",
      });
      setActionResult(`重新审批申请已提交：${result.id}（${grantStatusLabel(result.status)}）`);
      const fresh = await api.get<RoleViewableProjectsView>(roleViewableProjectsPath(roleId));
      setRoleEdit((current) => current && current.role.id === roleId ? { ...current, base: { ...current.base, pendingReapprovals: fresh.pendingReapprovals } } : current);
    } catch (actionError) {
      setRoleEditError(actionError instanceof Error ? actionError.message : "发起重新审批失败");
      throw actionError;
    }
  }

  function openTemplateDetail(row: PositionRoleTemplateRow) {
    const role = roles.find((item) => item.id === row.roleId);
    setDetail({
      title: `岗位角色模板 · ${zhPosition(row.positionCode)}`,
      fields: [
        { label: "岗位", value: zhPosition(row.positionCode) },
        { label: "职级", value: zhLevel(row.staffLevelId) },
        { label: "角色", value: role ? roleLabel(role) : (row.roleId ?? "—") },
        { label: "权限模板", value: row.templateId ?? "—" },
        { label: "组织", value: row.organizationId ?? "—" },
        { label: "状态", value: formatBusinessStatus(row.status ?? "active") },
        { label: "模板编号", value: row.id },
      ],
    });
  }

  function openRoleDetail(row: RoleRow) {
    setDetail({
      title: `角色 · ${row.roleName ?? row.roleCode ?? row.id}`,
      fields: [
        { label: "角色名称", value: row.roleName ?? row.roleCode ?? "—" },
        { label: "状态", value: row.status ? formatBusinessStatus(row.status) : "—" },
        { label: "权限点", value: row.permissionCodes?.length ? row.permissionCodes.join("、") : "—" },
        { label: "可以查看全部项目", value: row.viewAllProjects ? "是" : "—" },
        { label: "可查看项目", value: viewableProjectsLabel(row) },
        { label: "角色编号", value: row.id },
      ],
    });
  }

  function openGrantDetail(row: AccessGrantRequestRow) {
    setDetail({
      title: `授权申请 · ${row.id}`,
      fields: [
        { label: "申请编号", value: row.id },
        { label: "类型", value: grantTypeLabel(row.requestType) },
        { label: "目标用户", value: row.targetUserId },
        { label: "角色", value: row.targetRoleId ?? "—" },
        { label: "岗位", value: row.positionCode ? `${zhPosition(row.positionCode)} / ${zhLevel(row.staffLevelId)}` : "—" },
        { label: "权限码", value: row.permissionCodes?.length ? row.permissionCodes.join("、") : "—" },
        { label: "状态", value: grantStatusLabel(row.status) },
        { label: "单次有效", value: row.singleUse ? "是" : "否" },
        { label: "过期时间", value: cnDate(row.expiresAt) },
        { label: "申请原因", value: row.reason ?? "—" },
        { label: "申请人", value: row.requestedBy ?? "—" },
        { label: "审批人", value: row.decidedBy ?? "—" },
        { label: "审批时间", value: cnDate(row.decidedAt) },
        { label: "申请时间", value: cnDate(row.createdAt) },
      ],
    });
  }

  function openSummaryPositionDetail(row: AccessGrantPositionSummaryRow) {
    setDetail({
      title: `岗位授权建议 · ${zhPosition(row.positionCode)}`,
      fields: [
        { label: "类型", value: "岗位" },
        { label: "岗位", value: `${zhPosition(row.positionCode)} / ${zhLevel(row.staffLevelId)}` },
        { label: "权限", value: row.permissionCode ?? "—" },
        { label: "次数", value: row.requestCount },
        { label: "建议", value: row.recommendation ?? "—" },
      ],
    });
  }

  function openSummaryUserDetail(row: AccessGrantUserSummaryRow) {
    setDetail({
      title: `用户授权异常 · ${row.userId}`,
      fields: [
        { label: "类型", value: "用户" },
        { label: "用户", value: row.userId },
        { label: "次数", value: row.requestCount },
        { label: "说明", value: row.summary ?? "—" },
      ],
    });
  }

  return (
    <section data-testid="access-governance-page">
      <AdminPageHeader title="访问治理工作台" description="岗位角色模板、用户定岗自动适配、直接角色装配和高危授权审批；审批总结用于发现频繁临时授权。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-preview" data-testid="access-governance-guide">
          <strong>这一页做什么：</strong>访问治理{canRoleView ? "四" : "三"}块——① 角色装配（职位↔模板映射、给用户定职位自动适配、直接装配角色）② 高危授权审批 ③ 智能总结（发现频繁临时授权）{canRoleView ? " ④ 角色（角色清单、新建角色）" : ""}。切换标签操作。
        </div>
        <Tabs
          data-testid="access-governance-tabs"
          items={[
            { key: "assign", label: "① 角色装配", forceRender: true, children: (<>
        <h3>职位 ↔ 模板映射</h3>
        <label>存档、删除或恢复理由<input data-testid="access-position-template-reason" value={lifecycleReason} onChange={(event) => setLifecycleReason(event.target.value)} /></label>
        <label><input type="checkbox" checked={includeArchivedTemplates} onChange={(event) => void showArchivedTemplates(event.target.checked)} />显示存档及已删除模板</label>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-position-role-templates-table">
            <thead><tr><th>编号</th><th>岗位</th><th>职级</th><th>角色</th><th>权限模板</th><th>组织</th><th>状态</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>{positionTemplates.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-position-role-template-${row.id}`} onClick={() => openTemplateDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTemplateDetail(row); } }}>
                <td>{row.id}</td><td>{zhPosition(row.positionCode)}</td><td>{zhLevel(row.staffLevelId)}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{row.deletedAt ? "已删除（可恢复）" : row.archivedAt ? "已存档" : statusBadge(row.status)}</td>
                <td onClick={(event) => event.stopPropagation()}>
                  <button type="button" data-testid={`access-position-role-template-select-${row.id}`} disabled={Boolean(row.archivedAt)} onClick={() => selectTemplate(row)}>选中</button>
                  {!row.archivedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-archive-${row.id}`} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "archive"), "岗位模板处理失败")}>存档</PermissionButton> : <>
                    {!row.deletedAt && !row.deleteReviewId ? <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "request_delete"), "岗位模板处理失败")}>{user.id === "usr_master_authority" && user.username === "master-authority" ? "最高权限直接删除" : "申请删除"}</PermissionButton> : null}
                    {!row.deletedAt && row.deleteReviewId ? <>
                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "approve_delete"), "岗位模板处理失败")}>最高权限批准删除</PermissionButton>
                      <PermissionButton permissionsState={effectivePermissions} permissions={["access.role.assign", "governance.supervisor_review.approve"]} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "reject_delete"), "岗位模板处理失败")}>驳回删除</PermissionButton>
                    </> : <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} disabled={writeBusy} onClick={() => void runWrite(() => changePositionLifecycle(row.id, "restore"), "岗位模板处理失败")}>{row.deletedAt ? "恢复" : "取消存档"}</PermissionButton>}
                  </>}
                </td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位角色模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
        <div className="admin-form-row" data-testid="access-position-role-template-form">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={ASSIGN}
            data-testid="access-position-role-template-create-open"
            onClick={() => { setTemplateForm({ id: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", roleId: roles[0]?.id ?? "", templateId: "", status: "active" }); setTemplateError(""); setTemplateOpen(true); }}
          >＋ 新增岗位角色模板</PermissionButton>
          <span className="admin-muted-action">表内「选中」可编辑既有模板（走更新）。</span>
        </div>

        <h3>给用户定职位自动适配</h3>
        <div className="admin-inline-form" data-testid="access-user-position-form">
          <select aria-label="用户" value={positionForm.userId} onChange={(event) => setPositionForm({ ...positionForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <input aria-label="岗位编码" placeholder="岗位编码" value={positionForm.positionCode} onChange={(event) => setPositionForm({ ...positionForm, positionCode: event.target.value })} />
          <select aria-label="职级" data-testid="access-user-position-level" value={positionForm.staffLevelId} onChange={(event) => setPositionForm({ ...positionForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select>
          <input aria-label="基础薪资元" type="number" min="0" placeholder="基础薪资(元)" value={positionForm.baseSalaryYuan} onChange={(event) => setPositionForm({ ...positionForm, baseSalaryYuan: event.target.value })} />
          <input aria-label="薪资备注" placeholder="薪资备注" value={positionForm.salaryDetail} onChange={(event) => setPositionForm({ ...positionForm, salaryDetail: event.target.value })} />
          <input aria-label="操作备注" placeholder="操作备注" value={positionForm.operatorNote} onChange={(event) => setPositionForm({ ...positionForm, operatorNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-position-assign" disabled={writeBusy} onClick={() => void runWrite(assignPosition, "定岗失败")}>定职位并适配</PermissionButton>
        </div>

        <h3>人员范围变更（所属商户 / 跨项目）</h3>
        <div className="admin-inline-form" data-testid="access-user-scope-form">
          <select aria-label="变更用户" data-testid="access-user-scope-user" value={scopeForm.userId || positionForm.userId} onChange={(event) => setScopeForm({ ...scopeForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <input aria-label="新所属项目编号" data-testid="access-user-scope-org" placeholder="新所属项目编号（不变可空）" value={scopeForm.organizationId} onChange={(event) => setScopeForm({ ...scopeForm, organizationId: event.target.value })} />
          <input aria-label="新所属商户编号" data-testid="access-user-scope-merchant" placeholder="新所属商户编号（平台员工留空）" value={scopeForm.merchantId} onChange={(event) => setScopeForm({ ...scopeForm, merchantId: event.target.value })} />
          <input aria-label="新范围岗位编码" data-testid="access-user-scope-position" placeholder="新范围岗位编码（沿用可空）" value={scopeForm.positionCode} onChange={(event) => setScopeForm({ ...scopeForm, positionCode: event.target.value })} />
          <select aria-label="新范围职级" data-testid="access-user-scope-level" value={scopeForm.staffLevelId} onChange={(event) => setScopeForm({ ...scopeForm, staffLevelId: event.target.value })}><option value="">沿用原职级</option>{staffLevelOptions()}</select>
          <input aria-label="变更理由" data-testid="access-user-scope-reason" placeholder="变更理由" value={scopeForm.reason} onChange={(event) => setScopeForm({ ...scopeForm, reason: event.target.value })} />
          <input aria-label="主线最终审批人" data-testid="access-user-scope-final-approver" placeholder="主线最终审批人账号（可空）" value={scopeForm.finalApproverId} onChange={(event) => setScopeForm({ ...scopeForm, finalApproverId: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-scope-change" disabled={writeBusy} onClick={() => void runWrite(changeUserScope, "范围变更提交失败")}>提交范围变更（先审批后生效）</PermissionButton>
        </div>
        <div className="admin-muted-action" data-testid="access-user-scope-boundary">保存所属商户或跨项目变更时，旧岗位或旧范围派生权限立即失效；新范围权限每次均须先审批，审批通过后方可生效；模板内既有高危权限不因在模板内而免审。</div>

        <h3>直接装配角色</h3>
        <div className="admin-inline-form" data-testid="access-role-assignment-form">
          <select aria-label="装配用户" value={roleForm.userId} onChange={(event) => setRoleForm({ ...roleForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <select aria-label="装配角色" value={roleForm.roleId} onChange={(event) => setRoleForm({ ...roleForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
          <input aria-label="装配备注" placeholder="操作备注" value={roleForm.operatorNote} onChange={(event) => setRoleForm({ ...roleForm, operatorNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-assign" disabled={writeBusy} onClick={() => void runWrite(() => roleAssignment("assign"), "装配角色失败")}>装配角色</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-unassign" disabled={writeBusy} onClick={() => void runWrite(() => roleAssignment("unassign"), "卸载角色失败")}>卸载角色</PermissionButton>
        </div>

        <h3>员工单独授予 / 显式拒绝</h3>
        <div className="admin-inline-form" data-testid="access-user-override-form">
          {/* M002-F34：最高权限本人总能选中自己（拒绝了 access.user.view 时用户清单为空）。 */}
          <select aria-label="覆盖用户" value={overrideForm.userId} onChange={(event) => setOverrideForm({ ...overrideForm, userId: event.target.value })}><option value="">请选择用户</option>{(isMaster && !users.some((row) => row.id === user.id) ? [...users, { id: user.id, username: user.username, accountType: user.accountType }] : users).map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <input aria-label="覆盖权限码" placeholder="权限码，逗号分隔" value={overrideForm.permissionCodes} onChange={(event) => setOverrideForm({ ...overrideForm, permissionCodes: event.target.value })} />
          <select aria-label="覆盖效果" value={overrideForm.effect} onChange={(event) => setOverrideForm({ ...overrideForm, effect: event.target.value })}>
            <option value="deny">显式拒绝</option>
            <option value="grant">单独授予</option>
          </select>
          <input aria-label="覆盖理由" placeholder="理由" value={overrideForm.reason} onChange={(event) => setOverrideForm({ ...overrideForm, reason: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-override-set" disabled={writeBusy} onClick={() => void runWrite(() => userPermissionOverride("set"), "员工级覆盖设置失败")}>设置</PermissionButton>
          {/* M002-F34：最高权限本人解除自己的显式拒绝不看 access.role.assign（与后端 M002-F09 门禁同口径）；选他人或单独授予仍按 ASSIGN。 */}
          <PermissionButton permissionsState={effectivePermissions} permissions={isMaster && overrideForm.userId === user.id && overrideForm.effect === "deny" ? [] : ASSIGN} data-testid="access-user-override-remove" disabled={writeBusy} onClick={() => void runWrite(() => userPermissionOverride("remove"), "员工级覆盖移除失败")}>移除</PermissionButton>
        </div>

          </>) },
          { key: "grant", label: "② 高危授权审批", forceRender: true, children: (<>
        <h3>高危授权审批</h3>
        {canGrantView ? <div className="admin-form-row">
          <span>第 {grantPage} 页，共 {grantTotal} 条</span>
          <button type="button" disabled={grantPageBusy || grantPage <= 1} onClick={() => void loadGrantPage(grantPage - 1)}>上一页</button>
          <button type="button" data-testid="access-grant-next-page" disabled={grantPageBusy || grantPage * 50 >= grantTotal} onClick={() => void loadGrantPage(grantPage + 1)}>下一页</button>
        </div> : null}
        <div className="admin-form-row" data-testid="access-grant-request-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_APPLY} data-testid="access-grant-request-create-open"
            onClick={() => { setGrantError(""); setGrantOpen(true); }}>＋ 发起授权申请</PermissionButton>
          <span className="admin-muted-action">下方表格「通过/驳回」为审批动作，不受本弹窗影响。</span>
        </div>
        <div className="admin-inline-form" data-testid="access-grant-review-form">
          <input aria-label="审批意见" placeholder="审批意见" value={reviewForm.comment} onChange={(event) => setReviewForm({ ...reviewForm, comment: event.target.value })} />
          <input aria-label="审批证据" placeholder="审批证据附件，逗号分隔" value={reviewForm.evidenceAttachmentIds} onChange={(event) => setReviewForm({ ...reviewForm, evidenceAttachmentIds: event.target.value })} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-grant-requests-table">
            <thead><tr><th>申请</th><th>类型</th><th>目标</th><th>角色/岗位</th><th>权限码</th><th>状态</th><th>过期</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>{grantRequests.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-request-${row.id}`} onClick={() => openGrantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openGrantDetail(row); } }}>
                <td>{row.id}</td><td>{grantTypeLabel(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {zhPosition(row.positionCode)}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status, grantStatusLabel)}</td><td>{cnDate(row.expiresAt)}</td>
                <td onClick={(event) => event.stopPropagation()}>{(() => {
                  const acts: RowAction[] = [];
                  if (row.status === "pending") {
                    acts.push({ key: "approve", label: "通过", permissions: GRANT_REVIEW, onClick: () => void runWrite(() => reviewGrant(row, "approve"), "授权审批失败"), testId: `access-grant-approve-${row.id}` });
                    acts.push({ key: "reject", label: "驳回", permissions: GRANT_REVIEW, danger: true, onClick: () => void runWrite(() => reviewGrant(row, "reject"), "授权审批失败"), testId: `access-grant-reject-${row.id}` });
                  }
                  return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`access-grant-actions-${row.id}`} />;
                })()}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权申请详情" onClick={(event) => { event.stopPropagation(); openGrantDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>

          </>) },
          { key: "summary", label: "③ 智能总结", forceRender: true, children: (<>
        <h3>智能总结</h3>
        <details>
          <summary>授权政策配置</summary>
          <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_VIEW} data-testid="access-grant-policy-load" onClick={() => void loadGrantPolicy()}>读取授权政策</PermissionButton>
          {grantPolicy ? <>
            {(["windowDays", "positionThreshold", "userThreshold"] as const).map((key) => <fieldset key={key}>
              <legend>{{ windowDays: "统计时间窗（天）", positionThreshold: "岗位频繁申请阈值", userThreshold: "用户频繁申请阈值" }[key]}</legend>
              {(["value", "min", "max"] as const).map((part) => <label key={part}>{{ value: "配置值", min: "下限", max: "上限" }[part]}
                <input type="number" min={1} step={1} data-testid={`grant-policy-${key}-${part}`} value={grantPolicy[key][part]}
                  onChange={(event) => setGrantPolicy({ ...grantPolicy, [key]: { ...grantPolicy[key], [part]: Number(event.target.value) } })} />
              </label>)}
            </fieldset>)}
            {(["low", "medium", "high", "critical"] as const).map((risk) => <fieldset key={risk}>
              <legend>{{ low: "低风险", medium: "中风险", high: "高风险", critical: "极高风险" }[risk]}权限审批</legend>
              <label>审批层级<select value={grantPolicy.riskReviews[risk].approvalLevel}
                onChange={(event) => setGrantPolicy({ ...grantPolicy, riskReviews: { ...grantPolicy.riskReviews,
                  [risk]: { ...grantPolicy.riskReviews[risk], approvalLevel: event.target.value } } })}>
                <option value="one_level_up">上一级</option><option value="two_levels_up">上两级</option><option value="top_authority">最高权限</option>
              </select></label>
              <label>主管级别<input type="number" min={1} max={3} step={1} value={grantPolicy.riskReviews[risk].requiredLevel}
                onChange={(event) => setGrantPolicy({ ...grantPolicy, riskReviews: { ...grantPolicy.riskReviews,
                  [risk]: { ...grantPolicy.riskReviews[risk], requiredLevel: Number(event.target.value) } } })} /></label>
            </fieldset>)}
            <label>修改理由<input data-testid="grant-policy-reason" value={grantPolicyReason} onChange={(event) => setGrantPolicyReason(event.target.value)} /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["admission.policy.manage"]} data-testid="access-grant-policy-save" disabled={writeBusy} onClick={() => void runWrite(saveGrantPolicy, "授权政策保存失败")}>保存授权政策</PermissionButton>
          </> : null}
        </details>

        <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_VIEW} data-testid="access-grant-summary-refresh" onClick={() => void reload()}>刷新总结</PermissionButton>
        {summaryNotice ? <div className="admin-state" data-testid="access-grant-summary-degraded">{summaryNotice}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-grant-summary-table">
            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th></tr></thead>
            <tbody>
              {(summary?.positionRecommendations ?? []).map((row) => <tr key={`position-${row.positionCode}-${row.permissionCode}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-position-${row.positionCode}`} onClick={() => openSummaryPositionDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryPositionDetail(row); } }}><td>岗位</td><td>{zhPosition(row.positionCode)} / {zhLevel(row.staffLevelId)}</td><td>{row.permissionCode ?? "-"}</td><td>{row.requestCount}</td><td>{row.recommendation ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位授权建议详情" onClick={(event) => { event.stopPropagation(); openSummaryPositionDetail(row); }}>查看详情</button></td></tr>)}
              {(summary?.userAnomalies ?? []).map((row) => <tr key={`user-${row.userId}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-user-${row.userId}`} onClick={() => openSummaryUserDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryUserDetail(row); } }}><td>用户</td><td>{row.userId}</td><td>-</td><td>{row.requestCount}</td><td>{row.summary ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看用户授权异常详情" onClick={(event) => { event.stopPropagation(); openSummaryUserDetail(row); }}>查看详情</button></td></tr>)}
            </tbody>
          </table>
        </div>
          </>) },
          ...(canRoleView ? [{ key: "roles", label: "④ 角色", forceRender: true, children: (<>
        <h3>角色</h3>
        {canRoleCreate ? <div className="admin-form-row" data-testid="access-role-create-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={ROLE_CREATE} data-testid="access-role-create-open" onClick={() => void openRoleCreate()}>＋ 新建角色</PermissionButton>
        </div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-roles-table">
            <thead><tr><th>角色名称</th><th>状态</th><th>权限点</th><th>可以查看全部项目</th><th>可查看项目</th><th>详情</th></tr></thead>
            <tbody>{roles.map((row) => {
              const viewableIds = row.viewAllProjects ? [] : row.viewableProjectIds ?? [];
              const expanded = expandedRoleId === row.id;
              return (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-role-row-${row.id}`} onClick={() => openRoleDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRoleDetail(row); } }}>
                <td>{row.roleName ?? row.roleCode ?? row.id}</td><td>{statusBadge(row.status)}</td><td>{row.permissionCodes?.length ? `${row.permissionCodes.length} 个` : "—"}</td><td>{row.viewAllProjects ? "是" : "—"}</td>
                {/* 逐个勾选的带「＋」展开列出各条名称；名称来自候选清单接口，没有 access.role.manage 的人只显示个数。 */}
                <td>{viewableProjectsLabel(row)}
                  {viewableIds.length > 0 && canRoleManage ? <button type="button" data-testid={`access-role-projects-toggle-${row.id}`} aria-label="展开可查看项目" aria-expanded={expanded} onClick={(event) => { event.stopPropagation(); void toggleRoleProjects(row.id); }} onKeyDown={(event) => event.stopPropagation()}>{expanded ? "－" : "＋"}</button> : null}
                  {viewableIds.length > 0 && expanded && roleProjectOptions ? <div data-testid={`access-role-projects-${row.id}`}>{projectNames(viewableIds)}</div> : null}
                </td>
              <td className="admin-row-detail-cell">
                <button type="button" className="admin-row-detail-btn" aria-label="查看角色详情" onClick={(event) => { event.stopPropagation(); openRoleDetail(row); }}>查看详情</button>
                {canRoleManage ? <button type="button" data-testid={`access-role-projects-edit-${row.id}`} onClick={(event) => { event.stopPropagation(); void openRoleEdit(row); }} onKeyDown={(event) => event.stopPropagation()}>改可查看项目</button> : null}
              </td></tr>
              );
            })}</tbody>
          </table>
        </div>
          </>) }] : []),
        ]}
      />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={templateOpen}
        title={templateForm.id ? "编辑岗位角色模板" : "新增岗位角色模板"}
        testId="access-position-role-template-guided"
        submitTestId="access-position-role-template-save"
        submitLabel="保存映射"
        submitting={templateSaving}
        error={templateError || undefined}
        onClose={() => setTemplateOpen(false)}
        onSubmit={() => void savePositionTemplate()}
        steps={[
          {
            title: "岗位与职级",
            description: "岗位、职级必填，并至少绑定一个角色或权限模板；用户定岗后按此映射自动装配权限。",
            validate: () => {
              if (!templateForm.positionCode.trim() || !templateForm.staffLevelId.trim() || (!templateForm.roleId.trim() && !templateForm.templateId.trim())) {
                return TEMPLATE_REQUIRED;
              }
              return null;
            },
            content: (
              <>
                <label>岗位编码<input data-testid="access-position-role-template-position" aria-label="岗位编码" placeholder="岗位编码" value={templateForm.positionCode} onChange={(event) => setTemplateForm({ ...templateForm, positionCode: event.target.value })} /></label>
                <label>职级<select data-testid="access-position-role-template-level" aria-label="职级" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select></label>
                <label>角色<select data-testid="access-position-role-template-role" aria-label="角色" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
                <label>权限模板编号<input data-testid="access-position-role-template-template-id" aria-label="权限模板编号" placeholder="权限模板编号" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} /></label>
              </>
            ),
          },
          {
            title: "状态",
            content: (
              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">{formatBusinessStatus("active")}</option><option value="inactive">{formatBusinessStatus("inactive")}</option></select></label>
            ),
          },
        ]}
        summary={[
          { label: templateForm.id ? "模板编号" : "操作", value: templateForm.id || "新增" },
          { label: "岗位", value: templateForm.positionCode.trim() || "未填写" },
          { label: "职级", value: templateForm.staffLevelId.trim() || "未填写" },
          { label: "角色", value: templateForm.roleId ? roleLabel(roles.find((r) => r.id === templateForm.roleId)) : "不绑定" },
          { label: "权限模板", value: templateForm.templateId.trim() || "—" },
          { label: "状态", value: formatBusinessStatus(templateForm.status) },
        ]}
      />
      <GuidedFlowModal
        open={grantOpen}
        title="发起授权申请"
        testId="access-grant-request-guided"
        submitTestId="access-grant-request-create"
        submitLabel="发起申请"
        submitting={grantSaving}
        error={grantError || undefined}
        onClose={() => setGrantOpen(false)}
        onSubmit={() => void applyGrantRequest()}
        steps={[
          {
            title: "类型与对象",
            description: "选择申请类型与目标用户；临时权限、员工单独授予、解除员工显式拒绝、岗位高危权限释放都走审批流。",
            validate: () => (grantForm.targetUserId.trim() ? null : GRANT_USER_REQUIRED),
            content: (
              <>
                <label>申请类型
                  <select aria-label="申请类型" value={grantForm.requestType} onChange={(event) => setGrantForm({ ...grantForm, requestType: event.target.value })}>
                    <option value="temporary_permission">临时权限</option>
                    <option value="user_role">用户角色</option>
                    <option value="user_position">用户职位</option>
                    {canRoleAssign ? <>
                      <option value="user_permission_grant">员工单独授予</option>
                      <option value="user_permission_deny_removal">解除员工显式拒绝</option>
                      <option value="position_high_risk_release">岗位高危权限释放</option>
                    </> : null}
                  </select>
                </label>
                <label>目标用户<select aria-label="目标用户" value={grantForm.targetUserId} onChange={(event) => setGrantForm({ ...grantForm, targetUserId: event.target.value })}><option value="">请选择目标用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select></label>
              </>
            ),
          },
          {
            title: "授权明细",
            description: "按申请类型填写：用户角色填角色；用户职位填岗位/职级；临时权限填权限码与过期时间（必填）；员工单独授予、解除员工显式拒绝填权限码（必填，只收须审批的权限）；岗位高危权限释放填该员工仍挂起、没有待审释放申请的权限码（释放申请被驳回后再次发起）。",
            validate: grantDetailError,
            content: (
              <>
                <label>申请角色<select aria-label="申请角色" value={grantForm.roleId} onChange={(event) => setGrantForm({ ...grantForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
                <label>申请岗位<input aria-label="申请岗位" placeholder="岗位编码" value={grantForm.positionCode} onChange={(event) => setGrantForm({ ...grantForm, positionCode: event.target.value })} /></label>
                <label>申请职级<select aria-label="申请职级" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })}>{staffLevelOptions()}</select></label>
                <label>{grantForm.requestType === "temporary_permission" ? "临时权限码" : "权限码"}<input aria-label={grantForm.requestType === "temporary_permission" ? "临时权限码" : "权限码"} placeholder="权限码，逗号分隔" value={grantForm.permissionCodes} onChange={(event) => setGrantForm({ ...grantForm, permissionCodes: event.target.value })} /></label>
                <label>过期时间<input aria-label="过期时间" type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} /></label>
                <label><input type="checkbox" checked={grantForm.singleUse} onChange={(event) => setGrantForm({ ...grantForm, singleUse: event.target.checked })} />单次有效</label>
                <label>申请原因<input aria-label="申请原因" placeholder="申请原因" value={grantForm.reason} onChange={(event) => setGrantForm({ ...grantForm, reason: event.target.value })} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "申请类型", value: grantTypeLabel(grantForm.requestType) },
          { label: "目标用户", value: grantForm.targetUserId.trim() || "未选择" },
          { label: "角色", value: grantForm.requestType === "user_role" ? (grantForm.roleId ? roleLabel(roles.find((r) => r.id === grantForm.roleId)) : "未选择") : "—" },
          { label: "岗位/职级", value: grantForm.requestType === "user_position" ? `${grantForm.positionCode.trim() || "—"} / ${grantForm.staffLevelId.trim() || "—"}` : "—" },
          { label: "权限码", value: grantNeedsCodes(grantForm.requestType) ? (splitTokens(grantForm.permissionCodes).join("、") || "未填写") : "—" },
          { label: "过期时间", value: grantForm.requestType === "temporary_permission" ? (grantForm.expiresAt ? cnDate(grantForm.expiresAt) : "未填写") : "—" },
          { label: "单次有效", value: grantForm.requestType === "temporary_permission" ? (grantForm.singleUse ? "是" : "否") : "—" },
          { label: "申请原因", value: grantForm.reason.trim() || "后台高危授权申请" },
        ]}
      />
      <GuidedFlowModal
        open={roleCreateOpen}
        title="新建角色"
        testId="access-role-create-guided"
        submitTestId="access-role-create-save"
        submitLabel="创建角色"
        submitting={writeBusy}
        error={roleCreateError || undefined}
        onClose={() => setRoleCreateOpen(false)}
        onSubmit={() => void runWrite(createRole, "新建角色失败")}
        steps={[
          {
            title: "名称与说明",
            validate: () => {
              const name = newRole.roleName.trim();
              if (!name || [...name].length > 120) return ROLE_NAME_REQUIRED;
              if ([...newRole.description.trim()].length > 1000) return "角色说明不能超过 1000 个字";
              return null;
            },
            content: (
              <>
                <label>角色名称<input data-testid="access-role-create-name" aria-label="角色名称" value={newRole.roleName} onChange={(event) => setNewRole({ ...newRole, roleName: event.target.value })} /></label>
                <label>说明<input data-testid="access-role-create-description" aria-label="说明" value={newRole.description} onChange={(event) => setNewRole({ ...newRole, description: event.target.value })} /></label>
              </>
            ),
          },
          {
            title: "权限配置",
            content: (
              <table className="admin-table" data-testid="access-role-create-permission-table">
                <tbody>
                  {groupedRolePermissions.map(([moduleName, items]) => (
                    <tr key={moduleName}>
                      <th scope="row">{moduleName}</th>
                      <td>{items.map((item) => {
                        const code = permissionCode(item);
                        return (
                          <label key={code} className="admin-permission-option">
                            <input data-testid={`access-role-create-code-${code}`} type="checkbox" checked={newRole.permissionCodes.includes(code)} onChange={(event) => setNewRole({ ...newRole, permissionCodes: toggled(newRole.permissionCodes, code, event.target.checked) })} />
                            <span>{code}</span>
                            <small>{item.permissionName ?? item.name ?? item.riskLevel ?? "权限点"}</small>
                          </label>
                        );
                      })}</td>
                    </tr>
                  ))}
                  {/* 勾上「可以查看全部项目」：收起并禁用逐项清单、清空已勾条目；不勾时点「＋」展开逐个勾选；两者都不勾就是普通角色。 */}
                  <ViewableProjectRows prefix="access-role-create" viewAll={newRole.viewAllProjects} ids={newRole.viewableProjectIds} open={newRoleProjectsOpen} options={roleProjectOptions ?? []}
                    onViewAll={(checked) => { setNewRole({ ...newRole, viewAllProjects: checked, viewableProjectIds: [] }); setNewRoleProjectsOpen(false); }}
                    onIds={(ids) => setNewRole({ ...newRole, viewableProjectIds: ids })}
                    onOpen={setNewRoleProjectsOpen} />
                </tbody>
              </table>
            ),
          },
        ]}
        summary={[
          { label: "角色名称", value: newRole.roleName.trim() || "未填写" },
          { label: "说明", value: newRole.description.trim() || "—" },
          { label: "已选权限", value: newRole.permissionCodes.length ? `${newRole.permissionCodes.length} 个：${newRole.permissionCodes.join("、")}` : "未选择" },
          { label: "可以查看全部项目", value: newRole.viewAllProjects ? "是" : "—" },
          { label: "可查看项目", value: newRole.viewAllProjects ? "全部项目" : newRole.viewableProjectIds.length ? projectNames(newRole.viewableProjectIds) : "—" },
        ]}
      />
      {roleEdit && roleEditDelta ? (() => {
        const delta = roleEditDelta;
        const reduces = delta.removeAll || delta.remove.length > 0;
        const pendingRemovalIds = roleEdit.base.pendingRemovalIds ?? [];
        const holders = roleEdit.base.pendingReapprovals ?? [];
        return (
          <GuidedFlowModal
            open
            title="改可查看项目"
            testId="access-role-edit-guided"
            submitTestId="access-role-edit-save"
            submitLabel="保存"
            submitting={writeBusy}
            error={roleEditError || undefined}
            onClose={() => setRoleEdit(null)}
            onSubmit={() => void runWrite(saveRoleViewableProjects, "改可查看项目失败")}
            steps={[
              {
                title: "权限配置",
                validate: () => (delta.addAll || delta.add.length > 0 || reduces ? null : "可查看项目没有变化"),
                content: (
                  <>
                    <table className="admin-table">
                      <tbody>
                        {/* 勾上「可以查看全部项目」：清单收起，已勾条目不清空；去掉时按原样展开。减少在待审的条目照常显示为勾选、禁用。 */}
                        <ViewableProjectRows prefix="access-role-edit" viewAll={roleEdit.viewAll} ids={roleEdit.ids} open={roleEdit.open} options={roleProjectOptions ?? []}
                          lockedIds={pendingRemovalIds.filter((id) => id !== ALL_PROJECTS)} viewAllLocked={pendingRemovalIds.includes(ALL_PROJECTS)}
                          onViewAll={(checked) => setRoleEdit({ ...roleEdit, viewAll: checked, open: !checked })}
                          onIds={(ids) => setRoleEdit({ ...roleEdit, ids })}
                          onOpen={(open) => setRoleEdit({ ...roleEdit, open })} />
                      </tbody>
                    </table>
                    {reduces ? <input aria-label="主线最终审批人" data-testid="access-role-edit-final-approver" placeholder="主线最终审批人账号（可空）" value={roleEdit.finalApproverId} onChange={(event) => setRoleEdit({ ...roleEdit, finalApproverId: event.target.value })} /> : null}
                  </>
                ),
              },
              ...(holders.length ? [{
                title: "待重新审批的持有人",
                content: (
                  <table className="admin-table" data-testid="access-role-edit-holders">
                    <thead><tr><th>账号</th><th>待重新审批的项目</th><th>操作</th></tr></thead>
                    <tbody>{holders.map((holder) => (
                      <tr key={holder.userId}>
                        <td>{holder.username ?? holder.userId}</td><td>{projectNames(holder.viewableIds ?? [])}</td>
                        <td>{holder.reapprovalPending ? "已发起，待审批" : canGrantApply ? <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_APPLY} data-testid={`access-role-edit-reapprove-${holder.userId}`} disabled={writeBusy} onClick={() => void runWrite(() => requestRoleProjectReapproval(holder.userId), "发起重新审批失败")}>发起重新审批</PermissionButton> : null}</td>
                      </tr>
                    ))}</tbody>
                  </table>
                ),
              }] : []),
            ]}
            summary={[
              { label: "增加", value: projectNames([...(delta.addAll ? [ALL_PROJECTS] : []), ...delta.add]) || "—" },
              { label: "减少（须审批）", value: projectNames([...(delta.removeAll ? [ALL_PROJECTS] : []), ...delta.remove]) || "—" },
              { label: "主线最终审批人", value: (reduces && roleEdit.finalApproverId.trim()) || "—" },
            ]}
          />
        );
      })() : null}
    </section>
  );
}
