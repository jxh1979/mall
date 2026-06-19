import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, PermissionTemplateRow } from "../types";

// 后端写权限码（AccessController）
const TEMPLATE_MANAGE_PERMISSION = "access.permission_template.manage"; // copy
const TEMPLATE_APPLY_PERMISSION = "access.permission_template.apply"; // apply

export function PermissionTemplatesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<PermissionTemplateRow[]>([]);
  const { setSelectedId, selected } = useSelectedRow(rows);
  const [singleUse, setSingleUse] = React.useState(false);
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"));
    },
    { onError: setGlobalError, fallbackMessage: "权限模板读取失败", onStart: () => setGlobalError(null) },
  );

  async function copyTemplate() {
    if (!selected?.id) {
      setFormError("请先在表格中选择基础模板。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    try {
      const copied = await api.post<PermissionTemplateRow>(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}/copy`, { templateName: "普通员工01" });
      setActionResult({ action: "复制模板", sourceTemplateId: selected.id, copied });
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
    setFormError("");
    return true;
  }

  async function applyTemplate() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要应用的模板。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    try {
      const applied = await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(selected.id)}/apply`, { targetUserIds: ["user-1"], mode: singleUse ? "single_use" : "append", singleUse });
      setActionResult({ action: singleUse ? "单次有效临时模板已应用" : "模板已应用", sourceTemplateId: selected.id, applied });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "应用模板失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  return (
    <section data-testid="permission-templates-page">
      <AdminPageHeader title="权限模板" description="支持基础模板、复制模板、临时模板、时效和单次有效；复制不能影响原模板。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_MANAGE_PERMISSION]}
            disabled={!selected?.id}
            onClick={() => void copyTemplate()}
            data-testid="permission-template-copy"
          >
            复制模板
          </PermissionButton>
          <label className="admin-checkbox"><input data-testid="permission-template-single-use" type="checkbox" checked={singleUse} onChange={(event) => setSingleUse(event.target.checked)} /> 单次有效</label>
          <HighRiskConfirm
            label="应用模板"
            impact="将把选中模板的权限项应用到目标账号并落库；单次有效模式下生成临时授权，影响目标账号实际可操作范围。"
            disabled={!selected?.id}
            precheck={validateApply}
            onConfirm={() => void applyTemplate()}
            permissionsState={effectivePermissions}
            permissions={[TEMPLATE_APPLY_PERMISSION]}
            data-testid="permission-template-apply"
          />
        </div>
        {formError ? <div className="admin-inline-error" data-testid="permission-templates-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="permission-templates-table">
            <thead><tr><th>选择</th><th>模板名称</th><th>状态</th><th>有效性</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id}>
                <td><input type="radio" data-testid={`permission-template-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                <td>{row.templateName ?? row.name}</td>
                <td>{row.status}</td>
                <td>{row.singleUse ? "单次" : "长期"}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
