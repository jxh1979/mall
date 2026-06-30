import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";

// 综合体经营参数行（后端 OperatingParamsRecord，客户端 unwrap 已 camel 化）。
type OperatingParamsRow = {
  projectId: string;
  operatingAreaSqm: number | string | null;
  status: string;
};

const MANAGE = ["analytics.operating_params.manage"];

/**
 * 综合体经营参数配置页（ADM-ANA-003，T4.2 坪效）：平台管理员维护可经营面积——
 * 坪效 = 净收入 / 可经营面积 据此派生（DashboardAggregationJob revenue.per_sqm）。
 * 仅 analytics.operating_params.manage 可改（决定坪效口径分母，安全敏感）。
 */
export function OperatingParamsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<OperatingParamsRow[]>([]);
  const [form, setForm] = React.useState({ projectId: "project_1", areaSqm: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<OperatingParamsRow[]>("/api/v1/analytics/operating-params");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "综合体经营参数读取失败" },
  );

  async function saveArea() {
    const area = Number(form.areaSqm);
    if (!form.projectId.trim() || !Number.isFinite(area) || area <= 0) {
      setGlobalError("请填写综合体 id 与大于 0 的可经营面积（㎡）");
      return;
    }
    await api.post("/api/v1/analytics/operating-params", {
      projectId: form.projectId.trim(),
      operatingAreaSqm: area,
      status: "active",
    });
    setActionResult({ action: "operating_params_saved", projectId: form.projectId.trim(), operatingAreaSqm: area });
    setForm({ projectId: form.projectId.trim(), areaSqm: "" });
    reload();
  }

  return (
    <section data-testid="operating-params-page">
      <AdminPageHeader
        title="经营参数"
        description="维护综合体可经营面积（㎡）。坪效 = 净收入 / 可经营面积 据此派生；面积未配置时坪效卡显示「未配置」。仅平台管理员可改（决定坪效口径分母）。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="operating-params-table">
            <thead>
              <tr>
                <th>综合体</th>
                <th>可经营面积（㎡）</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.projectId} data-testid={`operating-params-${row.projectId}`}>
                  <td><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId}</TraceLink></td>
                  <td>{row.operatingAreaSqm == null ? "未配置" : Number(row.operatingAreaSqm).toFixed(2)}</td>
                  <td>{row.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="operating-params-form">
          <input
            aria-label="综合体 id"
            placeholder="综合体 id"
            value={form.projectId}
            onChange={(event) => setForm({ ...form, projectId: event.target.value })}
          />
          <input
            aria-label="可经营面积（㎡）"
            type="number"
            min="0"
            placeholder="可经营面积（㎡）"
            value={form.areaSqm}
            onChange={(event) => setForm({ ...form, areaSqm: event.target.value })}
          />
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="operating-params-save"
            onClick={() => void saveArea()}
          >
            保存面积
          </PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
