import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AccessUserRow, AdminPageProps, PermissionDefinitionRow, PermissionTemplateRow } from "../types";

const TEMPLATE_MANAGE_PERMISSION = "access.permission_template.manage";
const TEMPLATE_APPLY_PERMISSION = "access.permission_template.apply";

type Mode = "existing" | "new";

function permissionCode(row: PermissionDefinitionRow) {
  return row.permissionCode ?? row.code ?? "";
}

function templatePermissionCodes(row: PermissionTemplateRow | undefined) {
  return row?.permissionCodes ?? row?.permissions ?? [];
}

function displayTemplateName(row: PermissionTemplateRow) {
  return row.templateName ?? row.name ?? row.templateCode ?? row.id;
}

function toDateTimeLocal(value?: string | null) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
  return date.toISOString().slice(0, 16);
}

function toIsoOrUndefined(value: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toISOString();
}

function parseTargetUsers(value: string) {
  return value
    .split(/[\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function PermissionTemplatesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<PermissionTemplateRow[]>([]);
  const [permissions, setPermissions] = React.useState<PermissionDefinitionRow[]>([]);
  const [users, setUsers] = React.useState<AccessUserRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [mode, setMode] = React.useState<Mode>("existing");
  const [templateName, setTemplateName] = React.useState("");
  const [templateType, setTemplateType] = React.useState("base");
  const [validFrom, setValidFrom] = React.useState("");
  const [validUntil, setValidUntil] = React.useState("");
  const [singleUse, setSingleUse] = React.useState(false);
  const [selectedPermissionCodes, setSelectedPermissionCodes] = React.useState<string[]>([]);
  const [targetUsersText, setTargetUsersText] = React.useState("user-1");
  const [copyName, setCopyName] = React.useState("普通员工01");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const selected = mode === "existing" ? rows.find((row) => row.id === selectedId) : undefined;

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [templateRows, permissionRows, userRows] = await Promise.all([
        api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"),
        api.get<PermissionDefinitionRow[]>("/api/v1/access/permissions"),
        api.get<AccessUserRow[]>("/api/v1/access/users"),
      ]);
      setRows(templateRows);
      setPermissions(permissionRows.filter((item) => permissionCode(item)));
      setUsers(userRows);
      setSelectedId((current) => current || templateRows[0]?.id || "");
    },
    { onError: setGlobalError, fallbackMessage: "权限模板读取失败", onStart: () => setGlobalError(null) },
  );

  React.useEffect(() => {
    if (mode === "new") return;
    const next = rows.find((row) => row.id === selectedId) ?? rows[0];
    if (!next) return;
    if (next.id !== selectedId) setSelectedId(next.id);
    setTemplateName(displayTemplateName(next));
    setTemplateType(next.templateType ?? "base");
    setValidFrom(toDateTimeLocal(next.validFrom));
    setValidUntil(toDateTimeLocal(next.validUntil));
    setSingleUse(Boolean(next.singleUse));
    setSelectedPermissionCodes(templatePermissionCodes(next));
    setCopyName(`${displayTemplateName(next)}01`);
  }, [mode, rows, selectedId]);

  React.useEffect(() => {
    if (users.length > 0 && targetUsersText === "user-1") {
      setTargetUsersText(users[0]?.id ?? "user-1");
    }
  }, [targetUsersText, users]);

  const groupedPermissions = React.useMemo(() => {
    const groups = new Map<string, PermissionDefinitionRow[]>();
    for (const row of permissions) {
      const domain = row.domain ?? permissionCode(row).split(".")[0] ?? "other";
      groups.set(domain, [...(groups.get(domain) ?? []), row]);
    }
    return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [permissions]);

  function startNewTemplate() {
    setMode("new");
    setSelectedId("");
    setTemplateName("");
    setTemplateType("base");
    setValidFrom("");
    setValidUntil("");
    setSingleUse(false);
    setSelectedPermissionCodes([]);
    setFormError("");
  }

  function togglePermission(code: string, checked: boolean) {
    setSelectedPermissionCodes((current) => {
      const set = new Set(current);
      if (checked) set.add(code);
      else set.delete(code);
      return [...set].sort();
    });
  }

  function validateTemplateForm() {
    if (!templateName.trim()) {
      setFormError("模板名称不能为空。");
      return false;
    }
    if (selectedPermissionCodes.length === 0) {
      setFormError("至少选择一个权限点。");
      return false;
    }
    if (templateType === "temporary" && !validUntil) {
      setFormError("临时模板必须设置有效截止时间。");
      return false;
    }
    setFormError("");
    return true;
  }

  async function saveTemplate() {
    if (!validateTemplateForm()) return;
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = {
        templateName: templateName.trim(),
        templateType,
        sourceTemplateId: selected?.sourceTemplateId ?? undefined,
        permissionCodes: selectedPermissionCodes,
        dataScope: "project",
        validFrom: toIsoOrUndefined(validFrom),
        validUntil: toIsoOrUndefined(validUntil),
        singleUse,
        operatorNote: templateType === "temporary" ? "后台权限模板临时授权配置" : "后台权限模板配置",
      };
      const saved = mode === "existing" && selected?.id
        ? await api.put<PermissionTemplateRow>(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}`, body)
        : await api.post<PermissionTemplateRow>("/api/v1/access/permission-templates", body);
      setActionResult({ action: "保存权限模板", templateId: saved.id, permissionCount: selectedPermissionCodes.length, singleUse });
      setMode("existing");
      setSelectedId(saved.id);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "保存权限模板失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "保存权限模板", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  async function copyTemplate() {
    if (!selected?.id) {
      setFormError("请先在表格中选择基础模板。");
      return;
    }
    if (!copyName.trim()) {
      setFormError("复制模板名称不能为空。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    try {
      const copied = await api.post<PermissionTemplateRow>(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}/copy`, {
        templateName: copyName.trim(),
        templateType: "copy",
        operatorNote: "后台复制权限模板",
      });
      setActionResult({ action: "复制模板", sourceTemplateId: selected.id, copied });
      setMode("existing");
      setSelectedId(copied.id);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "复制模板失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  function validateApply() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要应用的模板。");
      return false;
    }
    if (parseTargetUsers(targetUsersText).length === 0) {
      setFormError("至少输入一个目标员工 ID。");
      return false;
    }
    setFormError("");
    return true;
  }

  async function applyTemplate() {
    if (!validateApply() || !selected?.id) return;
    setGlobalError(null);
    try {
      const applied = await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}/apply`, {
        targetUserIds: parseTargetUsers(targetUsersText),
        mode: singleUse ? "single_use" : "append",
        operatorNote: singleUse ? "后台应用单次有效权限模板" : "后台应用权限模板",
      });
      setActionResult({ action: singleUse ? "单次有效临时模板已应用" : "模板已应用", sourceTemplateId: selected.id, targetUserIds: parseTargetUsers(targetUsersText), applied });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "应用模板失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "应用权限模板", status: "failed", message });
    }
  }

  return (
    <section data-testid="permission-templates-page">
      <AdminPageHeader title="权限模板" description="支持基础模板、复制模板、临时模板、时效和单次有效；复制不能影响原模板。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-form-row admin-permission-template-form">
          <label>模板名称
            <input data-testid="permission-template-name" value={templateName} onChange={(event) => setTemplateName(event.target.value)} placeholder="例如：普通员工01" />
          </label>
          <label>模板类型
            <select data-testid="permission-template-type" value={templateType} onChange={(event) => setTemplateType(event.target.value)}>
              <option value="base">基础模板</option>
              <option value="temporary">临时模板</option>
              <option value="copy">复制模板</option>
            </select>
          </label>
          <label>生效时间
            <input data-testid="permission-template-valid-from" type="datetime-local" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} />
          </label>
          <label>截止时间
            <input data-testid="permission-template-valid-until" type="datetime-local" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} />
          </label>
          <label className="admin-checkbox"><input data-testid="permission-template-single-use" type="checkbox" checked={singleUse} onChange={(event) => setSingleUse(event.target.checked)} /> 单次有效</label>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_MANAGE_PERMISSION]}
            disabled={submitting}
            onClick={() => void saveTemplate()}
            data-testid="permission-template-save"
          >
            保存模板
          </PermissionButton>
          <button type="button" className="admin-secondary-button" data-testid="permission-template-new" onClick={startNewTemplate}>新建模板</button>
          <span data-testid="permission-template-selected-count" className="admin-muted-action">已选 {selectedPermissionCodes.length} 个权限</span>
        </div>

        <div className="admin-form-row admin-permission-apply-row">
          <label>复制名称
            <input data-testid="permission-template-copy-name" value={copyName} onChange={(event) => setCopyName(event.target.value)} placeholder="复制后的模板名称" />
          </label>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_MANAGE_PERMISSION]}
            disabled={!selected?.id}
            onClick={() => void copyTemplate()}
            data-testid="permission-template-copy"
          >
            复制模板
          </PermissionButton>
          <label>目标员工 ID
            <input data-testid="permission-template-target-users" value={targetUsersText} onChange={(event) => setTargetUsersText(event.target.value)} placeholder="多个用逗号分隔" />
          </label>
          <HighRiskConfirm
            label="应用模板"
            impact="将把选中模板的权限项应用到目标账号并落库；单次有效模式会影响目标账号实际可操作范围。"
            disabled={!selected?.id}
            precheck={validateApply}
            onConfirm={() => void applyTemplate()}
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_APPLY_PERMISSION]}
            data-testid="permission-template-apply"
          />
        </div>

        {formError ? <div className="admin-inline-error" data-testid="permission-templates-error">{formError}</div> : null}

        <div className="admin-permission-grid" data-testid="permission-template-permission-grid">
          {groupedPermissions.map(([domain, items]) => (
            <fieldset key={domain} className="admin-permission-domain">
              <legend>{domain}</legend>
              {items.map((item) => {
                const code = permissionCode(item);
                return (
                  <label key={code} className="admin-permission-option">
                    <input
                      data-testid={`permission-code-${code}`}
                      type="checkbox"
                      checked={selectedPermissionCodes.includes(code)}
                      onChange={(event) => togglePermission(code, event.target.checked)}
                    />
                    <span>{code}</span>
                    <small>{item.permissionName ?? item.name ?? item.riskLevel ?? "权限点"}</small>
                  </label>
                );
              })}
            </fieldset>
          ))}
        </div>

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="permission-templates-table">
            <thead><tr><th>选择</th><th>模板名称</th><th>类型</th><th>状态</th><th>有效性</th><th>权限数</th></tr></thead>
            <tbody>{rows.length === 0 ? (
              <tr><td colSpan={6} className="admin-muted-action">暂无权限模板，可先新建基础模板或临时模板。</td></tr>
            ) : rows.map((row) => (
              <tr key={row.id}>
                <td><input type="radio" data-testid={`permission-template-select-${row.id}`} checked={mode === "existing" && selectedId === row.id} onChange={() => { setMode("existing"); setSelectedId(row.id); }} /></td>
                <td>{displayTemplateName(row)}</td>
                <td>{row.templateType ?? "base"}</td>
                <td>{row.status}</td>
                <td>{row.singleUse ? "单次" : row.validUntil ? "限时" : "长期"}</td>
                <td>{templatePermissionCodes(row).length || "-"}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
