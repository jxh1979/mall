import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";

// 后端写权限码（AnalyticsController：/risk-alerts/{id}/resolve）
const RISK_RESOLVE_PERMISSION = "analytics.risk_alert.manage";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, RiskAlertRow } from "../types";

const HIGH_LEVELS = ["high", "critical"];
const CLOSED_STATUSES = ["resolved", "dismissed", "ignored"];

const TEXT = {
  title: "风控预警",
  description: "集中处理高风险客户、员工行为、第三方异常和生产风险预警；创建、调查、解决和关闭都必须带原因、幂等键和证据附件。",
  evidence: "接口证据：读取/创建 /api/v1/analytics/risk-alerts；处理 POST /api/v1/analytics/risk-alerts/{id}/resolve。",
  loadFailed: "风控预警读取失败",
  actionFailed: "风控预警操作失败",
  noAlert: "当前没有可处理预警。",
  summaryRequired: "预警摘要不能为空。",
  reasonRequired: "处理原因不能为空。",
  total: "预警总数",
  highRisk: "高风险",
  open: "待处理",
  selected: "选中预警",
  boundary: "忽略高风险预警需审批；重复预警必须合并，处理动作不得绕过证据附件、幂等键和审计留痕。",
  alertType: "预警类型",
  riskLevel: "风险等级",
  objectType: "对象类型",
  objectId: "对象 ID",
  summary: "预警摘要",
  reason: "处理原因",
  create: "创建预警",
  resolve: "处理为已解决",
  select: "选择",
  owner: "负责人",
  status: "状态",
  createdAt: "时间",
  evidenceLabel: "证据附件",
  focusClear: "清除筛选，返回全部",
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function evidenceList(row: RiskAlertRow) {
  return (row.evidenceAttachmentIds ?? []).join(", ") || "-";
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, evidenceAttachmentIds: request.evidenceAttachmentIds, result };
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染预警表对应子集。
// URL 约定：?kpi=alerts[&subset=high|open]。
type KpiFocus = { subset: "all" | "high" | "open" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "alerts") return null;
  const subset = params.get("subset");
  return { subset: subset === "high" || subset === "open" ? subset : "all" };
}

function focusTitle(subset: KpiFocus["subset"]): string {
  if (subset === "high") return TEXT.highRisk;
  if (subset === "open") return TEXT.open;
  return TEXT.total;
}

export function RiskAlertsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<RiskAlertRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [alertType, setAlertType] = React.useState("staff_behavior");
  const [riskLevel, setRiskLevel] = React.useState("high");
  const [objectType, setObjectType] = React.useState("bar_order");
  const [objectId, setObjectId] = React.useState("order-1");
  const [summary, setSummary] = React.useState("员工异常折扣触发高风险预警");
  const [resolutionReason, setResolutionReason] = React.useState("已复核订单、员工和支付证据，确认关闭");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: KpiFocus["subset"]) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "alerts");
    if (subset !== "all") params.set("subset", subset);
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
    window.scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    window.scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<RiskAlertRow[]>("/api/v1/analytics/risk-alerts"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const highRiskRows = rows.filter((row) => HIGH_LEVELS.includes(row.riskLevel ?? ""));
  const openRows = rows.filter((row) => !CLOSED_STATUSES.includes(row.status ?? ""));
  const highRiskCount = highRiskRows.length;
  const openCount = openRows.length;

  // 聚焦子集：按被点 KPI 卡过滤已加载内存数组，不发起后端查询。
  const focusRows = kpiFocus?.subset === "high" ? highRiskRows : kpiFocus?.subset === "open" ? openRows : rows;
  const filteredRows = kpiFocus ? focusRows : rows.filter((row) => rowMatchesKeyword(row, query));
  const paged = usePagedRows(filteredRows, 10, kpiFocus ? kpiFocus.subset : query);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function createAlert() {
    if (!summary.trim()) {
      setFormError(TEXT.summaryRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("risk-alert-create"),
      operatorNote: "完整后台创建风控预警",
      evidenceAttachmentIds: ["att-risk-alert-create"],
      ruleId: "risk-rule-1",
      alertType,
      riskLevel,
      objectType,
      objectId,
      summary,
    };
    await runAction("risk_alert_created", request, () => api.post("/api/v1/analytics/risk-alerts", request));
  }

  function validateResolve() {
    if (!selected) {
      setFormError(TEXT.noAlert);
      return false;
    }
    if (!resolutionReason.trim()) {
      setFormError(TEXT.reasonRequired);
      return false;
    }
    return true;
  }

  async function resolveAlert() {
    if (!selected) {
      setFormError(TEXT.noAlert);
      return;
    }
    if (!resolutionReason.trim()) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("risk-alert-resolve"),
      operatorNote: "完整后台处理风控预警",
      evidenceAttachmentIds: ["att-risk-alert-resolve"],
      action: "resolved",
      reason: resolutionReason,
    };
    await runAction("risk_alert_resolved", request, () => api.post(`/api/v1/analytics/risk-alerts/${selected.id}/resolve`, request));
  }

  const alertsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="risk-alerts-table">
        <thead><tr><th>{TEXT.select}</th><th>{TEXT.alertType}</th><th>{TEXT.riskLevel}</th><th>{TEXT.objectType}</th><th>{TEXT.objectId}</th><th>{TEXT.summary}</th><th>{TEXT.owner}</th><th>{TEXT.status}</th><th>{TEXT.evidenceLabel}</th><th>{TEXT.createdAt}</th></tr></thead>
        <tbody>{paged.pageRows.map((row) => (
          <tr key={row.id}>
            <td><button type="button" data-testid={`risk-alert-select-${row.id}`} onClick={() => setSelectedId(row.id)}>{TEXT.select}</button></td>
            <td>{formatBusinessStatus(row.alertType)}</td>
            <td>{formatBusinessStatus(row.riskLevel)}</td>
            <td>{row.objectType ?? "-"}</td>
            <td>{row.objectId ?? "-"}</td>
            <td>{row.summary ?? "-"}</td>
            <td>{row.ownerUserId ?? "-"}</td>
            <td>{formatBusinessStatus(row.status)}</td>
            <td>{evidenceList(row)}</td>
            <td>{row.createdAt ?? "-"}</td>
          </tr>
        ))}</tbody>
      </table>
      <TablePager testid="risk-alerts-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
    </div>
  );

  return (
    <section data-testid="risk-alerts-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="risk-alerts-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="risk_alert.viewed / risk_alert.created / analytics.risk_alert.resolved" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="risk-alerts-kpi-focus-banner">
              正在查看：{focusTitle(kpiFocus.subset)}（{focusRows.length}）
              <button type="button" data-testid="risk-alerts-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {alertsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="risk-alerts-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.total}</span><button type="button" className="admin-kpi-value" data-testid="risk-alerts-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>{TEXT.status}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.highRisk}</span><button type="button" className="admin-kpi-value" data-testid="risk-alerts-kpi-high" onClick={() => enterKpiFocus("high")}>{highRiskCount}</button><small>{TEXT.riskLevel}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.open}</span><button type="button" className="admin-kpi-value" data-testid="risk-alerts-kpi-open" onClick={() => enterKpiFocus("open")}>{openCount}</button><small>{TEXT.status}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected?.summary ?? "-"}</strong><small>{formatBusinessStatus(selected?.riskLevel)}</small></div>
            </div>

            <div className="admin-form-grid" data-testid="risk-alert-form">
              <label>{TEXT.alertType}<select data-testid="risk-alert-type" value={alertType} onChange={(event) => setAlertType(event.target.value)}><option value="staff_behavior">员工行为</option><option value="customer_risk">客人风险</option><option value="third_party_exception">第三方异常</option><option value="production_risk">出品风险</option></select></label>
              <label>{TEXT.riskLevel}<select data-testid="risk-alert-level" value={riskLevel} onChange={(event) => setRiskLevel(event.target.value)}><option value="suspicious">可疑</option><option value="high">高</option><option value="critical">紧急</option></select></label>
              <label>{TEXT.objectType}<input data-testid="risk-alert-object-type" value={objectType} onChange={(event) => setObjectType(event.target.value)} /></label>
              <label>{TEXT.objectId}<input data-testid="risk-alert-object-id" value={objectId} onChange={(event) => setObjectId(event.target.value)} /></label>
              <label>{TEXT.summary}<input data-testid="risk-alert-summary-input" value={summary} onChange={(event) => setSummary(event.target.value)} /></label>
              <label>{TEXT.reason}<input data-testid="risk-alert-resolution-reason" value={resolutionReason} onChange={(event) => setResolutionReason(event.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="risk-alert-actions">
              <button type="button" data-testid="risk-alert-create" disabled={busyAction !== ""} onClick={() => void createAlert()}>{TEXT.create}</button>
              <HighRiskConfirm
                label={TEXT.resolve}
                impact="预警将被置为已解决并写入审计留痕，下游统计与风险看板按此关闭。"
                disabled={busyAction !== ""}
                precheck={validateResolve}
                onConfirm={() => void resolveAlert()}
                permissionsState={effectivePermissions}
                permissions={[RISK_RESOLVE_PERMISSION]}
                data-testid="risk-alert-resolve"
              />
            </div>
            <div className="admin-muted-action" data-testid="risk-alerts-boundary">{TEXT.boundary}</div>
            {formError ? <div data-testid="risk-alert-error" className="admin-inline-error">{formError}</div> : null}

            <div className="admin-form-row" data-testid="risk-alerts-filter">
              <input data-testid="risk-alerts-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索预警类型 / 对象 / 摘要 / 状态" />
              <span className="admin-muted-action" data-testid="risk-alerts-filter-count">匹配 {filteredRows.length} / {rows.length} 条</span>
            </div>
            {filteredRows.length === 0 ? (
              <div className="admin-state admin-empty" data-testid="risk-alerts-filter-empty">无匹配预警</div>
            ) : alertsTable}
          </>
        )}
      </AdminDataState>
    </section>
  );
}
