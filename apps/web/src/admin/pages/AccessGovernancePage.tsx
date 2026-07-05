import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
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
  const value = status || "-";
  const kind = value === "approved" || value === "active" ? "is-ok" : value === "rejected" ? "is-danger" : value === "pending" ? "is-warn" : "is-info";
  return <span className={`admin-badge ${kind}`}>{value}</span>;
}

export function AccessGovernancePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [users, setUsers] = React.useState<AccessUserRow[]>([]);
  const [roles, setRoles] = React.useState<RoleRow[]>([]);
  const [positionTemplates, setPositionTemplates] = React.useState<PositionRoleTemplateRow[]>([]);
  const [grantRequests, setGrantRequests] = React.useState<AccessGrantRequestRow[]>([]);
  const [summary, setSummary] = React.useState<AccessGrantSummary | null>(null);
  const [templateForm, setTemplateForm] = React.useState({ id: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", roleId: "", templateId: "", status: "active" });
  const [positionForm, setPositionForm] = React.useState({ userId: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower", baseSalaryYuan: "", salaryDetail: "", operatorNote: "" });
  const [roleForm, setRoleForm] = React.useState({ userId: "", roleId: "", operatorNote: "" });
  const [grantForm, setGrantForm] = React.useState({
    requestType: "temporary_permission", targetUserId: "", roleId: "", positionCode: "ops_manager", staffLevelId: "lvl_m1_lower",
    permissionCodes: "access.role.assign", expiresAt: "", singleUse: true, reason: "",
  });
  const [reviewForm, setReviewForm] = React.useState({ comment: "", evidenceAttachmentIds: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [userRows, roleRows, templateRows, requestRows, grantSummary] = await Promise.all([
        api.get<AccessUserRow[]>("/api/v1/access/users"),
        api.get<RoleRow[]>("/api/v1/access/roles"),
        api.get<PositionRoleTemplateRow[]>("/api/v1/access/position-role-templates"),
        api.get<AccessGrantRequestRow[]>("/api/v1/access/grant-requests?window_days=30"),
        api.get<AccessGrantSummary>("/api/v1/access/grant-requests/summary?window_days=30&position_threshold=2&user_threshold=2"),
      ]);
      setUsers(userRows ?? []);
      setRoles(roleRows ?? []);
      setPositionTemplates(templateRows ?? []);
      setGrantRequests(requestRows ?? []);
      setSummary(grantSummary ?? null);
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
  }

  async function savePositionTemplate() {
    if (!templateForm.positionCode.trim() || !templateForm.staffLevelId.trim() || (!templateForm.roleId.trim() && !templateForm.templateId.trim())) {
      setGlobalError("岗位模板需填写岗位、职级，并至少绑定 roleId 或 templateId");
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
    const saved = templateForm.id
      ? await api.put<PositionRoleTemplateRow>(`/api/v1/access/position-role-templates/${encodeURIComponent(templateForm.id)}`, body)
      : await api.post<PositionRoleTemplateRow>("/api/v1/access/position-role-templates", body);
    setActionResult({ action: "access_position_role_template_saved", templateId: saved.id });
    setTemplateForm({ ...templateForm, id: "" });
    await reload();
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

  async function applyGrantRequest() {
    if (!grantForm.targetUserId.trim()) {
      setGlobalError("授权申请需填写目标用户");
      return;
    }
    if (grantForm.requestType === "temporary_permission" && (!grantForm.permissionCodes.trim() || !grantForm.expiresAt)) {
      setGlobalError("临时高危授权需填写权限码和过期时间");
      return;
    }
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
    await reload();
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

  return (
    <section data-testid="access-governance-page">
      <AdminPageHeader title="访问治理工作台" description="岗位角色模板、用户定岗自动适配、直接角色装配和高危授权审批；审批总结用于发现频繁临时授权。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>职位 ↔ 模板映射</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-position-role-templates-table">
            <thead><tr><th>id</th><th>岗位</th><th>职级</th><th>角色</th><th>权限模板</th><th>组织</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>{positionTemplates.map((row) => (
              <tr key={row.id} data-testid={`access-position-role-template-${row.id}`}>
                <td>{row.id}</td><td>{row.positionCode}</td><td>{row.staffLevelId}</td><td>{row.roleId ?? "-"}</td><td>{row.templateId ?? "-"}</td><td>{row.organizationId ?? "-"}</td><td>{statusBadge(row.status)}</td>
                <td>
                  <button type="button" data-testid={`access-position-role-template-select-${row.id}`} onClick={() => selectTemplate(row)}>选中</button>
                  <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid={`access-position-role-template-delete-${row.id}`} onClick={() => void deletePositionTemplate(row.id)}>删除</PermissionButton>
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="access-position-role-template-form">
          <input aria-label="模板 id" placeholder="模板 id(选中后更新)" value={templateForm.id} onChange={(event) => setTemplateForm({ ...templateForm, id: event.target.value })} />
          <input aria-label="岗位编码" placeholder="岗位编码" value={templateForm.positionCode} onChange={(event) => setTemplateForm({ ...templateForm, positionCode: event.target.value })} />
          <input aria-label="职级 id" placeholder="职级 id" value={templateForm.staffLevelId} onChange={(event) => setTemplateForm({ ...templateForm, staffLevelId: event.target.value })} />
          <select aria-label="角色 id" value={templateForm.roleId} onChange={(event) => setTemplateForm({ ...templateForm, roleId: event.target.value })}><option value="">不绑定角色</option>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
          <input aria-label="权限模板 id" placeholder="权限模板 id" value={templateForm.templateId} onChange={(event) => setTemplateForm({ ...templateForm, templateId: event.target.value })} />
          <select aria-label="模板状态" value={templateForm.status} onChange={(event) => setTemplateForm({ ...templateForm, status: event.target.value })}><option value="active">启用</option><option value="inactive">停用</option></select>
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN} data-testid="access-position-role-template-save" onClick={() => void savePositionTemplate()}>保存映射</PermissionButton>
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

        <h3>高危授权审批</h3>
        <div className="admin-inline-form" data-testid="access-grant-request-form">
          <select aria-label="申请类型" value={grantForm.requestType} onChange={(event) => setGrantForm({ ...grantForm, requestType: event.target.value })}>
            <option value="temporary_permission">临时权限</option>
            <option value="user_role">用户角色</option>
            <option value="user_position">用户职位</option>
          </select>
          <select aria-label="目标用户" value={grantForm.targetUserId} onChange={(event) => setGrantForm({ ...grantForm, targetUserId: event.target.value })}>{users.map((user) => <option key={user.id} value={user.id}>{user.username} ({user.id})</option>)}</select>
          <select aria-label="申请角色" value={grantForm.roleId} onChange={(event) => setGrantForm({ ...grantForm, roleId: event.target.value })}>{roles.map((role) => <option key={role.id} value={role.id}>{roleLabel(role)}</option>)}</select>
          <input aria-label="申请岗位" placeholder="岗位编码" value={grantForm.positionCode} onChange={(event) => setGrantForm({ ...grantForm, positionCode: event.target.value })} />
          <input aria-label="申请职级" placeholder="职级 id" value={grantForm.staffLevelId} onChange={(event) => setGrantForm({ ...grantForm, staffLevelId: event.target.value })} />
          <input aria-label="临时权限码" placeholder="权限码，逗号分隔" value={grantForm.permissionCodes} onChange={(event) => setGrantForm({ ...grantForm, permissionCodes: event.target.value })} />
          <input aria-label="过期时间" type="datetime-local" value={grantForm.expiresAt} onChange={(event) => setGrantForm({ ...grantForm, expiresAt: event.target.value })} />
          <label><input type="checkbox" checked={grantForm.singleUse} onChange={(event) => setGrantForm({ ...grantForm, singleUse: event.target.checked })} />单次有效</label>
          <input aria-label="申请原因" placeholder="申请原因" value={grantForm.reason} onChange={(event) => setGrantForm({ ...grantForm, reason: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_APPLY} data-testid="access-grant-request-create" onClick={() => void applyGrantRequest()}>发起申请</PermissionButton>
        </div>
        <div className="admin-inline-form" data-testid="access-grant-review-form">
          <input aria-label="审批意见" placeholder="审批意见" value={reviewForm.comment} onChange={(event) => setReviewForm({ ...reviewForm, comment: event.target.value })} />
          <input aria-label="审批证据" placeholder="审批证据附件，逗号分隔" value={reviewForm.evidenceAttachmentIds} onChange={(event) => setReviewForm({ ...reviewForm, evidenceAttachmentIds: event.target.value })} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-grant-requests-table">
            <thead><tr><th>申请</th><th>类型</th><th>目标</th><th>角色/岗位</th><th>权限码</th><th>状态</th><th>过期</th><th>操作</th></tr></thead>
            <tbody>{grantRequests.map((row) => (
              <tr key={row.id} data-testid={`access-grant-request-${row.id}`}>
                <td>{row.id}</td><td>{formatBusinessStatus(row.requestType)}</td><td>{row.targetUserId}</td><td>{row.targetRoleId ?? "-"} / {row.positionCode ?? "-"}</td><td>{row.permissionCodes?.join(", ") || "-"}</td><td>{statusBadge(row.status)}</td><td>{row.expiresAt ?? "-"}</td>
                <td>{row.status === "pending" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_REVIEW} data-testid={`access-grant-approve-${row.id}`} onClick={() => void reviewGrant(row, "approve")}>approve</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_REVIEW} data-testid={`access-grant-reject-${row.id}`} onClick={() => void reviewGrant(row, "reject")}>reject</PermissionButton>
                  </>
                ) : null}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>

        <h3>智能总结 summary</h3>
        <PermissionButton permissionsState={effectivePermissions} permissions={GRANT_VIEW} data-testid="access-grant-summary-refresh" onClick={() => void reload()}>刷新总结</PermissionButton>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-grant-summary-table">
            <thead><tr><th>类型</th><th>对象</th><th>权限</th><th>次数</th><th>建议</th></tr></thead>
            <tbody>
              {(summary?.positionRecommendations ?? []).map((row) => <tr key={`position-${row.positionCode}-${row.permissionCode}`} data-testid={`access-grant-summary-position-${row.positionCode}`}><td>position</td><td>{row.positionCode} / {row.staffLevelId ?? "-"}</td><td>{row.permissionCode ?? "-"}</td><td>{row.requestCount}</td><td>{row.recommendation ?? "-"}</td></tr>)}
              {(summary?.userAnomalies ?? []).map((row) => <tr key={`user-${row.userId}`} data-testid={`access-grant-summary-user-${row.userId}`}><td>user</td><td>{row.userId}</td><td>-</td><td>{row.requestCount}</td><td>{row.summary ?? "-"}</td></tr>)}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
