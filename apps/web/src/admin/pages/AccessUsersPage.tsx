import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AccessUserRow, AdminPageProps, PermissionTemplateRow } from "../types";
import { PermissionButton } from "../security/Can";

function templateLabel(template: PermissionTemplateRow) {
  return template.templateName ?? template.name ?? template.id;
}

export function AccessUsersPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<AccessUserRow[]>([]);
  const [templates, setTemplates] = React.useState<PermissionTemplateRow[]>([]);
  const [username, setUsername] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [phone, setPhone] = React.useState("");
  const [accountType, setAccountType] = React.useState("staff");
  const [consentReference, setConsentReference] = React.useState("");
  const [evidenceAttachmentIds, setEvidenceAttachmentIds] = React.useState("");
  const [organizationId, setOrganizationId] = React.useState("org-1");
  const [merchantId, setMerchantId] = React.useState("merchant-1");
  const [status, setStatus] = React.useState("active");
  const [selectedTemplateId, setSelectedTemplateId] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [users, permissionTemplates] = await Promise.all([
        api.get<AccessUserRow[]>("/api/v1/access/users"),
        api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"),
        api.get("/api/v1/access/permissions"),
      ]);
      setRows(users);
      setTemplates(permissionTemplates);
      setSelectedTemplateId((current) => current || permissionTemplates[0]?.id || "");
    },
    { onError: setGlobalError, fallbackMessage: "账号数据读取失败", onStart: () => setGlobalError(null) },
  );

  function validateForm() {
    if (!username.trim()) {
      setFormError("员工账号不能为空。");
      return false;
    }
    if (!password.trim()) {
      setFormError("初始密码不能为空；后台手动建号必须能完成登录闭环。 ");
      return false;
    }
    if (phone.trim() && !/^1\d{10}$/.test(phone.trim())) {
      setFormError("手机号必须是 11 位大陆手机号；不实名场景可留空。 ");
      return false;
    }
    if (!organizationId.trim()) {
      setFormError("组织 ID 不能为空，用于数据范围和留痕。 ");
      return false;
    }
    setFormError("");
    return true;
  }

  async function createUser() {
    if (!validateForm()) return;
    setGlobalError(null);
    setSubmitting(true);
    try {
      const created = await api.post<AccessUserRow>("/api/v1/access/users", {
        username: username.trim(),
        password: password.trim(),
        phone: phone.trim() || undefined,
        accountType,
        status,
        organizationId: organizationId.trim(),
        merchantId: merchantId.trim() || undefined,
        consentReference: consentReference.trim() || undefined,
        evidenceAttachmentIds: evidenceAttachmentIds.split(/[,，\n]/).map((item) => item.trim()).filter(Boolean),
        operatorNote: "后台手动录入工作人员",
      });
      let appliedTemplate: unknown = null;
      if (selectedTemplateId) {
        appliedTemplate = await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selectedTemplateId)}/apply`, {
          targetUserIds: [created.id],
          mode: "append",
          operatorNote: "后台手动录入后应用权限模板",
        });
      }
      const enriched = { ...created, phone: phone.trim() || created.phone, organizationId: organizationId.trim(), merchantId: merchantId.trim() || created.merchantId, status, accountType };
      setRows((current) => [...current, enriched]);
      setActionResult({ action: selectedTemplateId ? "创建员工并赋权" : "创建员工", username: created.username, accountType, templateId: selectedTemplateId || null, appliedTemplate });
      setUsername("");
      setPassword("");
      setPhone("");
      setConsentReference("");
      setEvidenceAttachmentIds("");
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
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="access-user-name" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="员工账号" />
          <input data-testid="access-user-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="初始密码" />
          <input data-testid="access-user-phone" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="手机号（可选，后端仅留哈希）" />
          <select data-testid="access-user-account-type" value={accountType} onChange={(event) => setAccountType(event.target.value)}>
            <option value="staff">普通员工</option>
            <option value="sales">销售人员</option>
            <option value="supervisor">主管</option>
            <option value="merchant_admin">商户管理员</option>
            <option value="merchant">商户员工</option>
          </select>
          <input data-testid="access-user-org" value={organizationId} onChange={(event) => setOrganizationId(event.target.value)} placeholder="组织 ID" />
          <input data-testid="access-user-merchant" value={merchantId} onChange={(event) => setMerchantId(event.target.value)} placeholder="商户 ID（销售/商户账号必填）" />
          <input data-testid="access-user-consent" value={consentReference} onChange={(event) => setConsentReference(event.target.value)} placeholder="同意/授权凭据编号（可选）" />
          <input data-testid="access-user-evidence" value={evidenceAttachmentIds} onChange={(event) => setEvidenceAttachmentIds(event.target.value)} placeholder="证据附件 ID，逗号分隔（可选）" />
          <select data-testid="access-user-status" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="active">启用</option>
            <option value="invited">待激活</option>
            <option value="disabled">禁用</option>
          </select>
          <select data-testid="access-user-template" value={selectedTemplateId} onChange={(event) => setSelectedTemplateId(event.target.value)}>
            <option value="">暂不赋权</option>
            {templates.map((template) => <option key={template.id} value={template.id}>{templateLabel(template)}</option>)}
          </select>
          <PermissionButton permissionsState={effectivePermissions} permissions={["access.user.manage"]} data-testid="access-user-create" onClick={() => void createUser()} disabled={!username.trim() || !password.trim() || submitting}>新增员工并赋权</PermissionButton>
        </div>
        {formError ? <div data-testid="access-users-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="access-users-table">
            <thead><tr><th>员工账号</th><th>手机号</th><th>账号类型</th><th>组织</th><th>商户</th><th>状态</th></tr></thead>
            <tbody>{rows.length === 0 ? (
              <tr><td colSpan={6} className="admin-muted-action">暂无员工账号，可通过上方表单手动录入并套用权限模板。</td></tr>
            ) : rows.map((row) => (
              <tr key={row.id}>
                <td>{row.username}</td>
                <td>{row.phone ?? "-"}</td>
                <td>{row.accountType}</td>
                <td>{row.organizationId ?? "-"}</td>
                <td>{row.merchantId ?? "-"}</td>
                <td>{row.status ?? "active"}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
