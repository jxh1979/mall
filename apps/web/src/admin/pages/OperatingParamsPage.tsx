import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";

// 综合体经营参数行（后端 OperatingParamsRecord，客户端 unwrap 已 camel 化）。
type BusinessDayCutoffRow = {
  domain: string;
  cutoffHour: number;
  updatedAt?: string;
};

const DOMAIN_LABELS: Record<string, string> = {
  platform: "平台汇总基准",
  finance: "财务日结",
  ticketing: "票务",
  amusement: "游乐",
  customer: "会员",
  inventory: "供应链",
  governance: "治理/风险",
  bar: "酒吧",
  merchant: "零售/餐饮商户",
};

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
  const [cutoffs, setCutoffs] = React.useState<BusinessDayCutoffRow[]>([]);
  const [cutoffEdits, setCutoffEdits] = React.useState<Record<string, string>>({});
  const [form, setForm] = React.useState({ projectId: "project_1", areaSqm: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [items, cutoffRows] = await Promise.all([
        api.get<OperatingParamsRow[]>("/api/v1/analytics/operating-params"),
        api.get<BusinessDayCutoffRow[]>("/api/v1/analytics/business-day-cutoffs"),
      ]);
      setRows(items ?? []);
      setCutoffs(cutoffRows ?? []);
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

  async function saveCutoff(domain: string) {
    const raw = cutoffEdits[domain];
    const hour = Number(raw);
    if (raw === undefined || !Number.isInteger(hour) || hour < 0 || hour > 23) {
      setGlobalError("日切小时必须是 0-23 的整数");
      return;
    }
    await api.post("/api/v1/analytics/business-day-cutoffs", { domain, cutoffHour: hour });
    setActionResult({ action: "business_day_cutoff_saved", domain, cutoffHour: hour });
    setCutoffEdits((edits) => ({ ...edits, [domain]: "" }));
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
        <h3>经营日切分(各业务的「一天」从几点切)</h3>
        <div className="admin-muted-action">酒吧=6 点(晚 6 点到次日早 6 点算一天)、零售/餐饮商户=10 点、其余=4 点;「平台汇总基准」行影响全场对账口径,改动一分钟内生效。</div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="business-day-cutoffs-table">
            <thead>
              <tr><th>业务域</th><th>当前切分(点)</th><th>改为</th><th>操作</th></tr>
            </thead>
            <tbody>
              {cutoffs.map((row) => (
                <tr key={row.domain} data-testid={`cutoff-row-${row.domain}`}>
                  <td>{DOMAIN_LABELS[row.domain] ?? row.domain}</td>
                  <td data-testid={`cutoff-hour-${row.domain}`}>{row.cutoffHour}:00</td>
                  <td><input aria-label={`${row.domain} 日切小时`} type="number" min="0" max="23" style={{ width: 72 }} value={cutoffEdits[row.domain] ?? ""} onChange={(event) => setCutoffEdits((edits) => ({ ...edits, [row.domain]: event.target.value }))} placeholder={String(row.cutoffHour)} /></td>
                  <td><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`cutoff-save-${row.domain}`} onClick={() => void saveCutoff(row.domain)}>保存</PermissionButton></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
