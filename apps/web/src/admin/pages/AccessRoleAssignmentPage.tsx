import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type UserRow = { id: string; username: string; organizationId?: string };
type RoleRow = { id: string; roleName: string; permissionCodes: string[]; organizationId?: string };
type PermRow = { permissionCode: string; permissionName: string };
type GrantRequestRow = { id: string; requestType: string; targetUserId?: string; targetRoleId: string; permissionCode?: string; status: string };

const ASSIGN = ["access.role.assign"];
const MANAGE = ["access.role.manage"];
const APPROVE = ["governance.supervisor_review.approve"];

/**
 * 角色装配（ADM-ACC-003，④ 集团治理 S1）：用户↔角色直接装配/解绑 + 角色权限运行时增删。
 * 补 RBAC「最后一公里」：此前能建角色/权限模板，却无法直接给员工派角色、也不能事后改角色权限。
 * 装配/增权过提权闸（不得超出授权人自身权限）与跨组织闸；高危授权审批（S2）后接同页"待审批"态。
 */
export function AccessRoleAssignmentPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [users, setUsers] = React.useState<UserRow[]>([]);
  const [roles, setRoles] = React.useState<RoleRow[]>([]);
  const [perms, setPerms] = React.useState<PermRow[]>([]);
  const [grantRequests, setGrantRequests] = React.useState<GrantRequestRow[]>([]);
  const [userId, setUserId] = React.useState("");
  const [assignRoleId, setAssignRoleId] = React.useState("");
  const [permRoleId, setPermRoleId] = React.useState("");
  const [permToAdd, setPermToAdd] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setUsers((await api.get<UserRow[]>("/api/v1/access/users")) ?? []);
      setRoles((await api.get<RoleRow[]>("/api/v1/access/roles")) ?? []);
      setPerms((await api.get<PermRow[]>("/api/v1/access/permissions")) ?? []);
      setGrantRequests((await api.get<GrantRequestRow[]>("/api/v1/access/grant-requests")) ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "角色装配数据读取失败", onStart: () => setGlobalError(null) },
  );

  const permRole = roles.find((r) => r.id === permRoleId);

  async function assign() {
    if (!userId || !assignRoleId) {
      setGlobalError("请选择用户与角色");
      return;
    }
    await api.post(`/api/v1/access/users/${userId}/roles`, { roleId: assignRoleId });
    setActionResult({ action: "user_role_assigned", userId, roleId: assignRoleId });
    reload();
  }

  async function unassign() {
    if (!userId || !assignRoleId) {
      setGlobalError("请选择用户与角色");
      return;
    }
    await api.post(`/api/v1/access/users/${userId}/roles/${assignRoleId}/unassign`, {});
    setActionResult({ action: "user_role_unassigned", userId, roleId: assignRoleId });
    reload();
  }

  async function addPerm() {
    if (!permRoleId || !permToAdd) {
      setGlobalError("请选择角色与权限");
      return;
    }
    await api.post(`/api/v1/access/roles/${permRoleId}/permissions`, { permissionCode: permToAdd });
    setActionResult({ action: "role_permission_added", roleId: permRoleId, permissionCode: permToAdd });
    reload();
  }

  async function removePerm(code: string) {
    await api.post(`/api/v1/access/roles/${permRoleId}/permissions/${code}/remove`, {});
    setActionResult({ action: "role_permission_removed", roleId: permRoleId, permissionCode: code });
    reload();
  }

  async function submitGrantRequest() {
    if (!userId || !assignRoleId) {
      setGlobalError("请选择用户与角色");
      return;
    }
    await api.post("/api/v1/access/grant-requests", { requestType: "user_role", userId, roleId: assignRoleId, reason: "高危角色装配申请" });
    setActionResult({ action: "grant_request_created", userId, roleId: assignRoleId });
    reload();
  }

  async function decide(id: string, decision: string) {
    await api.post(`/api/v1/access/grant-requests/${id}/decision`, { decision });
    setActionResult({ action: "grant_request_decided", grantRequestId: id, decision });
    reload();
  }

  return (
    <section data-testid="access-role-assignment-page">
      <AdminPageHeader
        title="角色装配"
        description="给员工直接派/解角色；运行时增删角色权限。装配=授该角色全部权限，受提权闸与跨组织闸约束。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <AdminPageHeader title="用户角色装配" description="选用户、选角色，装配或解绑。" />
        <div className="admin-inline-form" data-testid="user-role-assign-form">
          <select aria-label="选择用户" data-testid="assign-user-select" value={userId} onChange={(e) => setUserId(e.target.value)}>
            <option value="">选择用户…</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.username}（{u.id}）</option>)}
          </select>
          <select aria-label="选择角色" data-testid="assign-role-select" value={assignRoleId} onChange={(e) => setAssignRoleId(e.target.value)}>
            <option value="">选择角色…</option>
            {roles.map((r) => <option key={r.id} value={r.id}>{r.roleName}（{r.id}）</option>)}
          </select>
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN}
            data-testid="user-role-assign" onClick={() => void assign()}>装配</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN}
            data-testid="user-role-unassign" onClick={() => void unassign()}>解绑</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={ASSIGN}
            data-testid="user-role-request" onClick={() => void submitGrantRequest()}>提交授权申请</PermissionButton>
        </div>

        <AdminPageHeader title="角色权限" description="选角色查看其权限，运行时加/减。" />
        <div className="admin-inline-form">
          <select aria-label="选择角色看权限" data-testid="perm-role-select" value={permRoleId} onChange={(e) => setPermRoleId(e.target.value)}>
            <option value="">选择角色…</option>
            {roles.map((r) => <option key={r.id} value={r.id}>{r.roleName}（{r.id}）</option>)}
          </select>
        </div>
        {permRole && (
          <>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="role-permission-table">
                <thead><tr><th>权限码</th><th>操作</th></tr></thead>
                <tbody>
                  {(permRole.permissionCodes ?? []).map((code) => (
                    <tr key={code} data-testid={`role-permission-${code}`}>
                      <td>{code}</td>
                      <td><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                        data-testid={`role-permission-remove-${code}`} onClick={() => void removePerm(code)}>移除</PermissionButton></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="admin-inline-form" data-testid="role-permission-add-form">
              <select aria-label="选择权限" data-testid="perm-add-select" value={permToAdd} onChange={(e) => setPermToAdd(e.target.value)}>
                <option value="">选择权限…</option>
                {perms.map((p) => <option key={p.permissionCode} value={p.permissionCode}>{p.permissionCode}</option>)}
              </select>
              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                data-testid="role-permission-add" onClick={() => void addPerm()}>加权限</PermissionButton>
            </div>
          </>
        )}

        <AdminPageHeader title="高危授权申请" description="开启授权审批后，含高危权限的装配在此走申请→主管复核（通过才落地）。" />
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="grant-request-table">
            <thead><tr><th>id</th><th>类型</th><th>用户</th><th>角色</th><th>权限</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {grantRequests.map((g) => (
                <tr key={g.id} data-testid={`grant-request-${g.id}`}>
                  <td>{g.id}</td><td>{g.requestType}</td><td>{g.targetUserId ?? "-"}</td>
                  <td>{g.targetRoleId}</td><td>{g.permissionCode ?? "-"}</td><td>{g.status}</td>
                  <td>{g.status === "pending"
                    ? <>
                      <PermissionButton permissionsState={effectivePermissions} permissions={APPROVE}
                        data-testid={`grant-request-approve-${g.id}`} onClick={() => void decide(g.id, "approved")}>通过</PermissionButton>
                      <PermissionButton permissionsState={effectivePermissions} permissions={APPROVE}
                        data-testid={`grant-request-reject-${g.id}`} onClick={() => void decide(g.id, "rejected")}>驳回</PermissionButton>
                    </>
                    : "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
