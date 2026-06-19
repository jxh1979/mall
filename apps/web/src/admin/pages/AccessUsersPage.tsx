import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AccessUserRow, AdminPageProps, PermissionTemplateRow } from "../types";

export function AccessUsersPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [rows, setRows] = React.useState<AccessUserRow[]>([]);
  const [templates, setTemplates] = React.useState<PermissionTemplateRow[]>([]);
  const [username, setUsername] = React.useState("");
  const [role, setRole] = React.useState("BAR_MANAGER");
  const [selectedTemplateId, setSelectedTemplateId] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [users, permissionTemplates] = await Promise.all([
        api.get<AccessUserRow[]>("/api/v1/access/users"),
        api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"),
        api.get("/api/v1/access/roles"),
        api.get("/api/v1/access/permissions"),
      ]);
      setRows(users);
      setTemplates(permissionTemplates);
      setSelectedTemplateId((current) => current || permissionTemplates[0]?.id || "");
    },
    { onError: setGlobalError, fallbackMessage: "账号数据读取失败" },
  );

  async function createUser() {
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    try {
      const created = await api.post<AccessUserRow>("/api/v1/access/users", {
        username,
        accountType: role.toLowerCase(),
        status: "active",
        organizationId: "org-1",
        operatorNote: "后台手动录入工作人员",
      });
      let appliedTemplate: unknown = null;
      if (selectedTemplateId) {
        appliedTemplate = await api.post(`/api/v1/access/permission-templates/${selectedTemplateId}/apply`, {
          targetUserIds: [created.id],
          mode: "append",
          operatorNote: "后台手动录入后应用权限模板",
        });
      }
      setRows((current) => [...current, created]);
      setActionResult({ action: selectedTemplateId ? "创建员工并赋权" : "创建员工", username: created.username, role, templateId: selectedTemplateId || null, appliedTemplate });
      setUsername("");
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "创建员工失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: selectedTemplateId ? "创建员工并赋权" : "创建员工", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="access-users-page">
      <AdminPageHeader title="员工账号" description="管理员工账号、岗位角色和企业微信同步结果；禁用、手动录入和分配权限模板必须留痕。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="access-user-name" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="员工账号" />
          <input data-testid="access-user-role" value={role} onChange={(event) => setRole(event.target.value)} placeholder="岗位角色" />
          <select data-testid="access-user-template" value={selectedTemplateId} onChange={(event) => setSelectedTemplateId(event.target.value)}>
            <option value="">暂不赋权</option>
            {templates.map((template) => <option key={template.id} value={template.id}>{template.templateName ?? template.name ?? template.id}</option>)}
          </select>
          <button type="button" data-testid="access-user-create" onClick={() => void createUser()} disabled={!username.trim() || submitting}>新增员工并赋权</button>
        </div>
        {formError ? <div data-testid="access-users-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-users-table">
            <thead><tr><th>员工账号</th><th>岗位角色</th><th>状态</th></tr></thead>
            <tbody>{rows.map((row) => <tr key={row.id}><td>{row.username}</td><td>{row.accountType}</td><td>{row.status ?? "active"}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
