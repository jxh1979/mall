// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import { TraceLink } from "../components/TraceLink";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { formatBusinessStatus } from "../i18n/statusLabels";

type OperatingParamsRow = {
  projectId: string;
  operatingAreaSqm: number | string | null;
  status: string;
};

const MANAGE = ["analytics.operating_params.manage"];
// 营业日配置页的两个查看码，持有其中一个即可打开（同 AdminApp 路由门禁）。
const BUSINESS_DAY_VIEW = ["amusement.business_day.view", "bar.business_day.view"];

/**
 * 综合体经营参数配置页（ADM-ANA-003，T4.2 坪效）：平台管理员维护可经营面积——
 * 坪效 = 净收入 / 可经营面积 据此派生（DashboardAggregationJob revenue.per_sqm）。
 * 仅 analytics.operating_params.manage 可改（决定坪效口径分母，安全敏感）。
 */
export function OperatingParamsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<OperatingParamsRow[]>([]);
  const [anomalyConfig, setAnomalyConfig] = React.useState<{ surgePct?: number | string; highPct?: number | string; enabled?: boolean } | null>(null);
  const [surgeEdit, setSurgeEdit] = React.useState("");
  const [holidays, setHolidays] = React.useState<Array<{ holidayName: string; holidayYear: number; startDate?: string; endDate?: string; source?: string }>>([]);
  const [holidayName, setHolidayName] = React.useState("");
  const [holidayYear, setHolidayYear] = React.useState("");
  const [holidayStart, setHolidayStart] = React.useState("");
  const [holidayEnd, setHolidayEnd] = React.useState("");
  const [fetchUrl, setFetchUrl] = React.useState("");
  const [highEdit, setHighEdit] = React.useState("");
  const [form, setForm] = React.useState({ projectId: "", areaSqm: "" });
  // 引导式流程 phase-6：运营参数录入收起为主按钮 + 分步引导弹窗（异常/节假日等其余表单不动）。
  const [paramsOpen, setParamsOpen] = React.useState(false);
  const [paramsError, setParamsError] = React.useState("");
  const [paramsSubmitting, setParamsSubmitting] = React.useState(false);
  const heldBusinessDayCode = BUSINESS_DAY_VIEW.find((code) => hasPermissions(effectivePermissions, [code]));

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [items, anomalyCfg, holidayRows] = await Promise.all([
        api.get<OperatingParamsRow[]>("/api/v1/analytics/operating-params"),
        api.get<{ surgePct?: number | string; highPct?: number | string; enabled?: boolean }>("/api/v1/analytics/anomaly-config"),
        api.get<Array<{ holidayName: string; holidayYear: number; startDate?: string; endDate?: string; source?: string }>>("/api/v1/analytics/holiday-calendar"),
      ]);
      setRows(items ?? []);
      setAnomalyConfig(anomalyCfg ?? null);
      setHolidays(holidayRows ?? []);
      setSurgeEdit(anomalyCfg?.surgePct === undefined ? "" : String(Number(anomalyCfg.surgePct) * 100));
      setHighEdit(anomalyCfg?.highPct === undefined ? "" : String(Number(anomalyCfg.highPct) * 100));
    },
    { onError: setGlobalError, fallbackMessage: "综合体经营参数读取失败" },
  );

  // 面积校验（步内 + 提交双保险）：综合体 id 非空 + 可经营面积为大于 0 的数。
  function areaInvalid() {
    const area = Number(form.areaSqm);
    return !form.projectId.trim() || !Number.isFinite(area) || area <= 0;
  }

  async function saveArea() {
    const area = Number(form.areaSqm);
    if (areaInvalid()) {
      setParamsError("请填写综合体 id 与大于 0 的可经营面积（㎡）");
      return;
    }
    setParamsSubmitting(true);
    try {
      await api.post("/api/v1/analytics/operating-params", {
        projectId: form.projectId.trim(),
        operatingAreaSqm: area,
        status: "active",
      });
      setActionResult({ action: "operating_params_saved", projectId: form.projectId.trim(), operatingAreaSqm: area });
      setForm({ projectId: "", areaSqm: "" });
      setParamsError("");
      setParamsOpen(false);
      reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "运营参数录入失败";
      setParamsError(message);
      setGlobalError(message);
    } finally {
      setParamsSubmitting(false);
    }
  }

  async function saveAnomalyConfig() {
    const surge = Number(surgeEdit) / 100;
    const high = Number(highEdit) / 100;
    if (!Number.isFinite(surge) || !Number.isFinite(high) || surge <= 0 || high < surge) {
      setGlobalError("阈值需满足:可疑线>0 且 高风险线≥可疑线(按百分比填写,如 50 与 80)");
      return;
    }
    await api.post("/api/v1/analytics/anomaly-config", { surgePct: surge, highPct: high, enabled: true });
    setActionResult({ action: "anomaly_config_saved", surgePct: surge, highPct: high });
    reload();
  }

  async function saveHoliday() {
    if (!holidayName.trim() || !holidayYear || !holidayStart || !holidayEnd) {
      setGlobalError("节假日名/年份/起止日期都要填(按国务院公布)");
      return;
    }
    await api.post("/api/v1/analytics/holiday-calendar", {
      holidayName: holidayName.trim(), holidayYear: Number(holidayYear),
      startDate: holidayStart, endDate: holidayEnd,
    });
    setActionResult({ action: "holiday_saved", holidayName: holidayName.trim(), holidayYear: Number(holidayYear) });
    setHolidayName(""); setHolidayYear(""); setHolidayStart(""); setHolidayEnd("");
    reload();
  }

  async function fetchHolidays() {
    if (!fetchUrl.trim()) {
      setGlobalError("请填节假日数据源 URL(holiday-cn JSON 格式)");
      return;
    }
    const result = await api.post<{ imported?: number }>("/api/v1/analytics/holiday-calendar/fetch", { url: fetchUrl.trim() });
    setActionResult({ action: "holiday_fetched", imported: result?.imported });
    reload();
  }

  return (
    <section data-testid="operating-params-page">
      <AdminPageHeader
        title="经营参数"
        description="维护综合体可经营面积（㎡）。坪效 = 净收入 / 可经营面积 据此派生；面积未配置时坪效卡显示「未配置」。仅平台管理员可改（决定坪效口径分母）。"
      />
      <p>营业日按综合体与每家酒吧分别配置，经最高权限审批后未来生效。<PermissionButton permissionsState={effectivePermissions} permissions={heldBusinessDayCode ? [heldBusinessDayCode] : BUSINESS_DAY_VIEW} onClick={() => navigateTo?.("ADM-AMU-008")}>打开营业日配置</PermissionButton></p>
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
                  <td>{formatBusinessStatus(row.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-form-row" data-testid="operating-params-form">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="operating-params-create-open"
            onClick={() => { setParamsError(""); setParamsOpen(true); }}
          >
            ＋ 录入运营参数
          </PermissionButton>
          <span className="admin-muted-action">配置综合体可经营面积（㎡），坪效 = 净收入 / 可经营面积 据此派生。</span>
        </div>
        <GuidedFlowModal
          open={paramsOpen}
          title="录入运营参数"
          testId="operating-params-guided"
          submitTestId="operating-params-save"
          submitLabel="保存面积"
          submitting={paramsSubmitting}
          error={paramsError || undefined}
          onClose={() => setParamsOpen(false)}
          onSubmit={() => void saveArea()}
          steps={[
            {
              title: "运营面积",
              description: "填写综合体 id 与大于 0 的可经营面积（㎡）。",
              validate: () => (areaInvalid() ? "请填写综合体 id 与大于 0 的可经营面积（㎡）" : null),
              content: (
                <>
                  <label>综合体 id<input aria-label="综合体 id" placeholder="综合体 id" value={form.projectId} onChange={(event) => setForm({ ...form, projectId: event.target.value })} /></label>
                  <label>可经营面积（㎡）<input aria-label="可经营面积（㎡）" type="number" min="0" placeholder="可经营面积（㎡）" value={form.areaSqm} onChange={(event) => setForm({ ...form, areaSqm: event.target.value })} /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "综合体 id", value: form.projectId.trim() || "未填写" },
            { label: "可经营面积（㎡）", value: form.areaSqm.trim() || "未填写" },
          ]}
        />
        <h3>指标异常检测阈值(环比突变,后台可调)</h3>
        <div className="admin-form-grid" data-testid="anomaly-config-editor">
          <label>可疑线(%)<input data-testid="anomaly-surge-input" value={surgeEdit} onChange={(event) => setSurgeEdit(event.target.value)} /></label>
          <label>高风险线(%)<input data-testid="anomaly-high-input" value={highEdit} onChange={(event) => setHighEdit(event.target.value)} /></label>
          <button type="button" data-testid="anomaly-config-save" onClick={saveAnomalyConfig}>保存阈值</button>
          <span className="admin-muted-action">当前:可疑 {anomalyConfig?.surgePct === undefined ? "—" : `${(Number(anomalyConfig.surgePct) * 100).toFixed(0)}%`} / 高风险 {anomalyConfig?.highPct === undefined ? "—" : `${(Number(anomalyConfig.highPct) * 100).toFixed(0)}%`};关键指标当日较前一经营日突变超线自动记异常,小基数日不报防误报。</span>
        </div>
        <h3>节假日日历(节假日对比与异常免报共用;农历+调休每年按国务院公布,URL 抓取或手动录入)</h3>
        <div className="admin-table-scroll" data-testid="holiday-calendar-section">
          <table className="admin-table" data-testid="holiday-calendar-table">
            <thead><tr><th>年份</th><th>节假日</th><th>起</th><th>止</th><th>来源</th></tr></thead>
            <tbody>
              {holidays.map((h) => (
                <tr key={`${h.holidayName}-${h.holidayYear}`}>
                  <td>{h.holidayYear}</td>
                  <td>{h.holidayName}</td>
                  <td>{h.startDate ?? "—"}</td>
                  <td>{h.endDate ?? "—"}</td>
                  <td>{h.source === "seed" ? "预填" : h.source === "fetched" ? "抓取" : "手动"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-form-grid" data-testid="holiday-manual-editor">
          <label>名称<input data-testid="holiday-name-input" value={holidayName} onChange={(e) => setHolidayName(e.target.value)} placeholder="春节" /></label>
          <label>年份<input data-testid="holiday-year-input" value={holidayYear} onChange={(e) => setHolidayYear(e.target.value)} placeholder="2026" /></label>
          <label>起<CnDatePicker data-testid="holiday-start-input" value={holidayStart} onChange={setHolidayStart} /></label>
          <label>止<CnDatePicker data-testid="holiday-end-input" value={holidayEnd} onChange={setHolidayEnd} /></label>
          <button type="button" data-testid="holiday-save" onClick={saveHoliday}>手动录入</button>
        </div>
        <div className="admin-form-grid" data-testid="holiday-fetch-editor">
          <label>数据源 URL<input data-testid="holiday-fetch-url" value={fetchUrl} onChange={(e) => setFetchUrl(e.target.value)} placeholder="https://.../2026.json (holiday-cn 格式)" /></label>
          <button type="button" data-testid="holiday-fetch" onClick={fetchHolidays}>从 URL 抓取</button>
          <span className="admin-muted-action">抓取失败不影响既有日历;内网不通外网时用手动录入。</span>
        </div>
      </AdminDataState>
    </section>
  );
}
