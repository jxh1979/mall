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
import type { AccessUserRow, AdminPageProps } from "../types";

type RoleRow = { id: string; roleName?: string | null; roleCode?: string | null; status?: string | null; permissionCodes?: string[] };
type PositionRoleTemplateRow = {
  id: string; positionCode: string; staffLevelId: string; roleId?: string | null; templateId?: string | null;
  organizationId?: string | null; status?: string | null; createdBy?: string | null;
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
  return POSITION_LABELS[code] ?? code;
}
function zhLevel(code?: string | null): string {
  if (!code) return "—";
  const m = /^lvl_m(\d+)(?:_(lower|upper))?$/.exec(code);
  if (m) return m[2] ? `M${m[1]}·${m[2] === "lower" ? "初级" : "高级"}` : `M${m[1]}`;
  return code;
}
const GRANT_APPLY = ["access.grant_request.apply"];
const GRANT_VIEW = ["access.grant_request.view"];
const GRANT_REVIEW = ["access.grant_request.review", "governance.supervisor_review.approve"];

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

function statusBadge(status?: string | null) {
  const value = status || "";
  const kind = value === "approved" || value === "active" ? "is-ok" : value === "rejected" ? "is-danger" : value === "pending" ? "is-warn" : "is-info";
  // 徽标文字中文化（业务HTML零英文规则）；颜色仍按原始码判定。
  return <span className={`admin-badge ${kind}`}>{value ? formatBusinessStatus(value) : "—"}</span>;
}

export function AccessGovernancePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [users, setUsers] = React.useState<AccessUserRow[]>([]);
  const [roles, setRoles] = React.useState<RoleRow[]>([]);
  const [positionTemplates, setPositionTemplates] = React.useState<PositionRoleTemplateRow[]>([]);
  const [grantRequests, setGrantRequests] = React.useState<AccessGrantRequestRow[]>([]);
  const [summary, setSummary] = React.useState<AccessGrantSummary | null>(null);
  const [summaryNotice, setSummaryNotice] = React.useState<string | null>(null);
  const [templateForm, setTemplateForm] = React.useState({ id: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", roleId: "", templateId: "", status: "active" });
  const [positionForm, setPositionForm] = React.useState({ userId: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", baseSalaryYuan: "", salaryDetail: "", operatorNote: "" });
  const [roleForm, setRoleForm] = React.useState({ userId: "", roleId: "", operatorNote: "" });
  const [grantForm, setGrantForm] = React.useState({
    requestType: "temporary_permission", targetUserId: "", roleId: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower",
    permissionCodes: "access.role.assign", expiresAt: "", singleUse: true, reason: "",
  });
  const [reviewForm, setReviewForm] = React.useState({ comment: "", evidenceAttachmentIds: "" });
  // 行明细抽屉：三张业务表（岗位模板/授权申请/智能总结）整行可点 → 该行完整字段（枚举中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-9：岗位角色模板收起为主按钮 + 分步引导弹窗（其余授权申请/审批工作流原样不动）。
  const [templateOpen, setTemplateOpen] = React.useState(false);
  const [templateError, setTemplateError] = React.useState("");
  const [templateSaving, setTemplateSaving] = React.useState(false);
  // 引导式流程 phase-11：授权申请收起为主按钮 + 分步引导弹窗（岗位模板弹窗/审批/定岗/装配角色原样不动）。
  const [grantOpen, setGrantOpen] = React.useState(false);
  const [grantError, setGrantError] = React.useState("");
  const [grantSaving, setGrantSaving] = React.useState(false);

  // 无权限副资源不请求（根因修）：授权列表/概要卡需 access.grant_request.view、用户清单需 access.user.view、角色清单需 access.role.view，
  // 本页 gate 仅 access.role.assign 不含这三码——缺权限裸 await 会 403 reject 整个 Promise.all 致整页报错，故一并门控
  //（position-role-templates 与页面同码 access.role.assign，保持裸调用）。
  const canGrantView = hasPermissions(effectivePermissions, ["access.grant_request.view"]);
  const canUserView = hasPermissions(effectivePermissions, ["access.user.view"]);
  const canRoleView = hasPermissions(effectivePermissions, ["access.role.view"]);
  const { loading, error, reload } = useAdminResource(
    async () => {
      const [userRows, roleRows, templateRows, requestRows, grantSummary] = await Promise.all([
        canUserView ? api.get<AccessUserRow[]>("/api/v1/access/users") : Promise.resolve([] as AccessUserRow[]),
        canRoleView ? api.get<RoleRow[]>("/api/v1/access/roles") : Promise.resolve([] as RoleRow[]),
        api.get<PositionRoleTemplateRow[]>("/api/v1/access/position-role-templates"),
        canGrantView ? api.get<AccessGrantRequestRow[]>("/api/v1/access/grant-requests?window_days=30") : Promise.resolve([] as AccessGrantRequestRow[]),
        // 智能总结=纯统计副资源（GRANT_VIEW 权限，无权账号可能 403）：单独降级，不拖垮角色装配/审批主流程。
        loadSecondaryIfPermitted(canGrantView, () => api.get<AccessGrantSummary>("/api/v1/access/grant-requests/summary?window_days=30&position_threshold=2&user_threshold=2"), null as AccessGrantSummary | null),
      ]);
      setUsers(userRows ?? []);
      setRoles(roleRows ?? []);
      setPositionTemplates(templateRows ?? []);
      setGrantRequests(requestRows ?? []);
      setSummary(grantSummary.data);
      setSummaryNotice(grantSummary.notice);
    },
    { onError: setGlobalError, fallbackMessage: "访问治理数据读取失败", onStart: () => setGlobalError(null) },
  );

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

  const TEMPLATE_REQUIRED = "岗位模板需填写岗位、职级，并至少绑定 roleId 或 templateId";

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
      setActionResult({ action: "access_position_role_template_saved", templateId: saved.id });
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

  async function deletePositionTemplate(id: string) {
    await api.delete(`/api/v1/access/position-role-templates/${encodeURIComponent(id)}`);
    setActionResult({ action: "access_position_role_template_deleted", templateId: id });
    await reload();
  }

  async function assignPosition() {
    if (!positionForm.userId.trim() || !positionForm.positionCode.trim() || !positionForm.staffLevelId.trim()) {
      setGlobalError("定职位需填写用户、岗位和职级");
      return;
    }
    const result = await api.put(`/api/v1/access/users/${encodeURIComponent(positionForm.userId.trim())}/position`, {
      positionCode: positionForm.positionCode.trim(),
      staffLevelId: positionForm.staffLevelId.trim(),
      baseSalaryAmountCent: centFromYuan(positionForm.baseSalaryYuan),
      salaryCurrency: positionForm.baseSalaryYuan.trim() ? "CNY" : undefined,
      salaryDetail: positionForm.salaryDetail.trim() || undefined,
      operatorNote: positionForm.operatorNote.trim() || "后台定岗并按模板自动装配",
    });
    setActionResult({ action: "access_user_position_assigned", userId: positionForm.userId.trim(), result });
    await reload();
  }

  async function roleAssignment(kind: "assign" | "unassign") {
    if (!roleForm.userId.trim() || !roleForm.roleId.trim()) {
      setGlobalError("直接装配角色需填写用户 id 与角色 id");
      return;
    }
    const path = kind === "assign" ? "/api/v1/access/role-assignments" : "/api/v1/access/role-assignments/unassign";
    const result = await api.post(path, {
      userId: roleForm.userId.trim(),
      roleId: roleForm.roleId.trim(),
      operatorNote: roleForm.operatorNote.trim() || `后台${kind === "assign" ? "装配" : "卸载"}角色`,
    });
    setActionResult({ action: `access_role_${kind}`, userId: roleForm.userId.trim(), roleId: roleForm.roleId.trim(), result });
    await reload();
  }

  const GRANT_USER_REQUIRED = "授权申请需填写目标用户";
  const GRANT_TEMP_REQUIRED = "临时高危授权需填写权限码和过期时间";

  // 提交在确认步触发；校验失败/接口失败弹窗留开显错（grantError），成功才关。原接口/请求体/双保险校验保留。
  async function applyGrantRequest() {
    if (!grantForm.targetUserId.trim()) {
      setGrantError(GRANT_USER_REQUIRED);
      return;
    }
    if (grantForm.requestType === "temporary_permission" && (!grantForm.permissionCodes.trim() || !grantForm.expiresAt)) {
      setGrantError(GRANT_TEMP_REQUIRED);
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
        permissionCodes: grantForm.requestType === "temporary_permission" ? splitTokens(grantForm.permissionCodes) : undefined,
        expiresAt: grantForm.requestType === "temporary_permission" ? toIso(grantForm.expiresAt) : undefined,
        singleUse: grantForm.requestType === "temporary_permission" ? grantForm.singleUse : undefined,
        reason: grantForm.reason.trim() || "后台高危授权申请",
        operatorNote: grantForm.reason.trim() || "后台高危授权申请",
      });
      setActionResult({ action: "access_grant_request_applied", requestId: result.id, status: result.status });
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
      comment: reviewForm.comment.trim() || decision,
      operatorNote: reviewForm.comment.trim() || decision,
      evidenceAttachmentIds: splitTokens(reviewForm.evidenceAttachmentIds),
    });
    setActionResult({ action: `access_grant_request_${decision}`, requestId: row.id, status: result.status });
    await reload();
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

  function openGrantDetail(row: AccessGrantRequestRow) {
    setDetail({
      title: `授权申请 · ${row.id}`,
      fields: [
        { label: "申请编号", value: row.id },
        { label: "类型", value: formatBusinessStatus(row.requestType) },
        { label: "目标用户", value: row.targetUserId },
        { label: "角色", value: row.targetRoleId ?? "—" },
        { label: "岗位", value: row.positionCode ? `${zhPosition(row.positionCode)} / ${zhLevel(row.staffLevelId)}` : "—" },
        { label: "权限码", value: row.permissionCodes?.length ? row.permissionCodes.join("、") : "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
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
          <strong>这一页做什么：</strong>访问治理三块——① 角色装配（职位↔模板映射、给用户定职位自动适配、直接装配角色）② 高危授权审批 ③ 智能总结（发现频繁临时授权）。切换标签操作。
        </div>
        <Tabs
          data-testid="access-governance-tabs"
          items={[
            { key: "assign", label: "① 角色装配", forceRender: true, children: (<>
        <h3>职位 ↔ 模板映射</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-position-role-templates-table">
            <thead><tr><th>编号</th><th>岗位</th><th>职级</th><th>角色</th><th>权限模板</th><th>组织</th><th>状态</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>{positionTemplates.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`access-position-role-template-${row.id}`} onClick={() => openTemplateDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTemplateDetail(row); } }}>
                <td>{row.id}</td><td>{zhPosition(row.positionCode)}</td><td>{zhLevel(row.staffLevelId)}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{statusBadge(row.status)}</td>
                <td onClick={(event) => event.stopPropagation()}>
                  <button type="button" data-testid={`access-position-role-template-select-${row.id}`} onClick={() => selectTemplate(row)}>选中</button>
                  <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-delete-${row.id}`} onClick={() => void deletePositionTemplate(row.id)}>删除</PermissionButton>
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
          <select aria-label="用户 id" value={positionForm.userId} onChange={(event) => setPositionForm({ ...positionForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <input aria-label="岗位编码" placeholder="岗位编码" value={positionForm.positionCode} onChange={(event) => setPositionForm({ ...positionForm, positionCode: event.target.value })} />
          <input aria-label="职级 id" placeholder="职级 id" value={positionForm.staffLevelId} onChange={(event) => setPositionForm({ ...positionForm, staffLevelId: event.target.value })} />
          <input aria-label="基础薪资元" type="number" min="0" placeholder="基础薪资(元)" value={positionForm.baseSalaryYuan} onChange={(event) => setPositionForm({ ...positionForm, baseSalaryYuan: event.target.value })} />
          <input aria-label="薪资备注" placeholder="薪资备注" value={positionForm.salaryDetail} onChange={(event) => setPositionForm({ ...positionForm, salaryDetail: event.target.value })} />
          <input aria-label="操作备注" placeholder="操作备注" value={positionForm.operatorNote} onChange={(event) => setPositionForm({ ...positionForm, operatorNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-user-position-assign" onClick={() => void assignPosition()}>定职位并适配</PermissionButton>
        </div>

        <h3>直接装配角色</h3>
        <div className="admin-inline-form" data-testid="access-role-assignment-form">
          <select aria-label="装配用户 id" value={roleForm.userId} onChange={(event) => setRoleForm({ ...roleForm, userId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <select aria-label="装配角色 id" value={roleForm.roleId} onChange={(event) => setRoleForm({ ...roleForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
          <input aria-label="装配备注" placeholder="操作备注" value={roleForm.operatorNote} onChange={(event) => setRoleForm({ ...roleForm, operatorNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-assign" onClick={() => void roleAssignment("assign")}>装配角色</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-role-unassign" onClick={() => void roleAssignment("unassign")}>卸载角色</PermissionButton>
        </div>

          </>) },
          { key: "grant", label: "② 高危授权审批", forceRender: true, children: (<>
        <h3>高危授权审批</h3>
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
                <td>{row.id}</td><td>{formatBusinessStatus(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {zhPosition(row.positionCode)}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status)}</td><td>{cnDate(row.expiresAt)}</td>
                <td onClick={(event) => event.stopPropagation()}>{(() => {
                  const acts: RowAction[] = [];
                  if (row.status === "pending") {
                    acts.push({ key: "approve", label: "通过", permissions: GRANT_REVIEW, onClick: () => void reviewGrant(row, "approve"), testId: `access-grant-approve-${row.id}` });
                    acts.push({ key: "reject", label: "驳回", permissions: GRANT_REVIEW, danger: true, onClick: () => void reviewGrant(row, "reject"), testId: `access-grant-reject-${row.id}` });
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
        <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_VIEW} data-testid="access-grant-summary-refresh" onClick={() => void reload()}>刷新总结</PermissionButton>
        {summaryNotice ? <div className="admin-state" data-testid="access-grant-summary-degraded">{summaryNotice}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-grant-summary-table">
            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th><th>详情</th><th>详情</th></tr></thead>
            <tbody>
              {(summary?.positionRecommendations ?? []).map((row) => <tr key={`position-${row.positionCode}-${row.permissionCode}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-position-${row.positionCode}`} onClick={() => openSummaryPositionDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryPositionDetail(row); } }}><td>岗位</td><td>{zhPosition(row.positionCode)} / {zhLevel(row.staffLevelId)}</td><td>{row.permissionCode ?? "-"}</td><td>{row.requestCount}</td><td>{row.recommendation ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看岗位授权建议详情" onClick={(event) => { event.stopPropagation(); openSummaryPositionDetail(row); }}>查看详情</button></td></tr>)}
              {(summary?.userAnomalies ?? []).map((row) => <tr key={`user-${row.userId}`} className="admin-row-clickable" tabIndex={0} data-testid={`access-grant-summary-user-${row.userId}`} onClick={() => openSummaryUserDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSummaryUserDetail(row); } }}><td>用户</td><td>{row.userId}</td><td>-</td><td>{row.requestCount}</td><td>{row.summary ?? "-"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看用户授权异常详情" onClick={(event) => { event.stopPropagation(); openSummaryUserDetail(row); }}>查看详情</button></td></tr>)}
            </tbody>
          </table>
        </div>
          </>) },
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
                <label>职级 id<input data-testid="access-position-role-template-level" aria-label="职级 id" placeholder="职级 id" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })} /></label>
                <label>角色<select data-testid="access-position-role-template-role" aria-label="角色 id" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
                <label>权限模板 id<input data-testid="access-position-role-template-template-id" aria-label="权限模板 id" placeholder="权限模板 id" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} /></label>
              </>
            ),
          },
          {
            title: "状态",
            content: (
              <label>模板状态<select data-testid="access-position-role-template-status" aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">启用</option><option value="inactive">停用</option></select></label>
            ),
          },
        ]}
        summary={[
          { label: templateForm.id ? "模板编号" : "操作", value: templateForm.id || "新增" },
          { label: "岗位", value: templateForm.positionCode.trim() || "未填写" },
          { label: "职级", value: templateForm.staffLevelId.trim() || "未填写" },
          { label: "角色", value: templateForm.roleId ? roleLabel(roles.find((r) => r.id === templateForm.roleId)) : "不绑定" },
          { label: "权限模板", value: templateForm.templateId.trim() || "—" },
          { label: "状态", value: templateForm.status === "active" ? "启用" : "停用" },
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
            description: "选择申请类型与目标用户；临时权限走高危审批流。",
            validate: () => (grantForm.targetUserId.trim() ? null : GRANT_USER_REQUIRED),
            content: (
              <>
                <label>申请类型
                  <select aria-label="申请类型" value={grantForm.requestType} onChange={(event) => setGrantForm({ ...grantForm, requestType: event.target.value })}>
                    <option value="temporary_permission">临时权限</option>
                    <option value="user_role">用户角色</option>
                    <option value="user_position">用户职位</option>
                  </select>
                </label>
                <label>目标用户<select aria-label="目标用户" value={grantForm.targetUserId} onChange={(event) => setGrantForm({ ...grantForm, targetUserId: event.target.value })}><option value="">请选择目标用户</option>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select></label>
              </>
            ),
          },
          {
            title: "授权明细",
            description: "按申请类型填写：用户角色填角色；用户职位填岗位/职级；临时权限填权限码与过期时间（必填）。",
            validate: () => {
              if (grantForm.requestType === "temporary_permission" && (!grantForm.permissionCodes.trim() || !grantForm.expiresAt)) return GRANT_TEMP_REQUIRED;
              return null;
            },
            content: (
              <>
                <label>申请角色<select aria-label="申请角色" value={grantForm.roleId} onChange={(event) => setGrantForm({ ...grantForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select></label>
                <label>申请岗位<input aria-label="申请岗位" placeholder="岗位编码" value={grantForm.positionCode} onChange={(event) => setGrantForm({ ...grantForm, positionCode: event.target.value })} /></label>
                <label>申请职级<input aria-label="申请职级" placeholder="职级 id" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })} /></label>
                <label>临时权限码<input aria-label="临时权限码" placeholder="权限码，逗号分隔" value={grantForm.permissionCodes} onChange={(event) => setGrantForm({ ...grantForm, permissionCodes: event.target.value })} /></label>
                <label>过期时间<input aria-label="过期时间" type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} /></label>
                <label><input type="checkbox" checked={grantForm.singleUse} onChange={(event) => setGrantForm({ ...grantForm, singleUse: event.target.checked })} />单次有效</label>
                <label>申请原因<input aria-label="申请原因" placeholder="申请原因" value={grantForm.reason} onChange={(event) => setGrantForm({ ...grantForm, reason: event.target.value })} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "申请类型", value: formatBusinessStatus(grantForm.requestType) },
          { label: "目标用户", value: grantForm.targetUserId.trim() || "未选择" },
          { label: "角色", value: grantForm.requestType === "user_role" ? (grantForm.roleId ? roleLabel(roles.find((r) => r.id === grantForm.roleId)) : "未选择") : "—" },
          { label: "岗位/职级", value: grantForm.requestType === "user_position" ? `${grantForm.positionCode.trim() || "—"} / ${grantForm.staffLevelId.trim() || "—"}` : "—" },
          { label: "权限码", value: grantForm.requestType === "temporary_permission" ? (splitTokens(grantForm.permissionCodes).join("、") || "未填写") : "—" },
          { label: "过期时间", value: grantForm.requestType === "temporary_permission" ? (grantForm.expiresAt ? cnDate(grantForm.expiresAt) : "未填写") : "—" },
          { label: "单次有效", value: grantForm.requestType === "temporary_permission" ? (grantForm.singleUse ? "是" : "否") : "—" },
          { label: "申请原因", value: grantForm.reason.trim() || "后台高危授权申请" },
        ]}
      />
    </section>
  );
}
