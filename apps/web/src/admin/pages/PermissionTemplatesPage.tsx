import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, PermissionTemplateRow } from "../types";

export function PermissionTemplatesPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [rows, setRows] = React.useState<PermissionTemplateRow[]>([]);
  const [singleUse, setSingleUse] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<PermissionTemplateRow[]>("/api/v1/access/permission-templates"));
    },
    { onError: setGlobalError, fallbackMessage: "权限模板读取失败" },
  );

  const baseTemplate = rows[0];

  async function copyTemplate() {
    if (!baseTemplate?.id) return;
    const copied = await api.post<PermissionTemplateRow>(`/api/v1/access/permission-templates/${encodeURIComponent(baseTemplate.id)}/copy`, { templateName: "普通员工01" });
    setActionResult({ action: "复制模板", sourceTemplateId: baseTemplate.id, copied });
  }

  async function applyTemplate() {
    if (!baseTemplate?.id) return;
    const applied = await api.post(`/api/v1/access/permission-templates/${encodeURIComponent(baseTemplate.id)}/apply`, { targetUserIds: ["user-1"], mode: singleUse ? "single_use" : "append", singleUse });
    setActionResult({ action: singleUse ? "单次有效临时模板已应用" : "模板已应用", sourceTemplateId: baseTemplate.id, applied });
  }

  return (
    <section data-testid="permission-templates-page">
      <AdminPageHeader title="权限模板" description="支持基础模板、复制模板、临时模板、时效和单次有效；复制不能影响原模板。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <button type="button" data-testid="permission-template-copy" onClick={() => void copyTemplate()} disabled={!baseTemplate?.id}>复制模板</button>
          <label className="admin-checkbox"><input data-testid="permission-template-single-use" type="checkbox" checked={singleUse} onChange={(event) => setSingleUse(event.target.checked)} /> 单次有效</label>
          <button type="button" data-testid="permission-template-apply" onClick={() => void applyTemplate()} disabled={!baseTemplate?.id}>应用模板</button>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="permission-templates-table">
            <thead><tr><th>模板名称</th><th>状态</th><th>有效性</th></tr></thead>
            <tbody>{rows.map((row) => <tr key={row.id}><td>{row.templateName ?? row.name}</td><td>{row.status}</td><td>{row.singleUse ? "单次" : "长期"}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
