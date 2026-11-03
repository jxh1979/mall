// 文件版本：V0.02
import React from "react";
import { formatActionResult } from "../components/ActionResultView";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { TablePager } from "../components/TablePager";
import type { AdminPageProps, AuditEventRow, AuditOperationEventRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TERMINAL_LABELS } from "../i18n/fieldLabels";
import type { PagedResult } from "../client/adminClient";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { firstSubmitTime, forgetSubmitTime } from "../../shared/operationKey";

const PAGE_SIZE = 10;
const OPERATION_LIMIT = 200;

// 第十八批 N 段第 122 条 122-R2（PGM:745）：终端列按终端中文名显示；没有终端信息的显示「—」。
const terminalLabel = (terminal?: string | null) => (terminal ? (TERMINAL_LABELS[terminal] ?? terminal) : "—");

const TEXT = {
  title: "审计事件",
  description: "统一查询员工、客户、第三方、财务和生产操作留痕；审计事件只能追加和导出留痕，不允许在业务后台编辑或删除历史事件。",
  evidence: "接口证据：读取/追加 /api/v1/evidence/events（服务端分页 page/size，可翻更早历史）；导出动作本身通过 EventBatchRequest 写入 evidence.events_reported 留痕。",
  loadFailed: "审计事件读取失败",
  exportFailed: "审计事件导出留痕失败",
  total: "事件总数",
  success: "本页成功",
  failed: "本页失败/阻断",
  categories: "本页分类",
  search: "本页内筛选 trace / 事件 / 对象 / 摘要",
  exportReason: "导出原因",
  export: "导出并留痕",
  detail: "查看详情",
  noMatch: "无匹配审计事件",
  boundary: "审计事件 append-only；导出必须产生新的 evidence event，页面不提供编辑、删除、覆盖历史记录能力。",
  eventType: "事件类型",
  category: "分类",
  result: "结果",
  risk: "风险",
  source: "来源",
  trace: "追踪号",
  object: "对象",
  summary: "摘要",
  occurredAt: "发生时间",
  focusClear: "清除筛选，返回全部",
  // 操作日志视图（真 audit_events）
  viewEvidence: "证据事件",
  viewOperations: "操作日志",
  opsEvidence: "接口证据：读取 /api/v1/audit/operation-events（服务端按业务类型/业务对象/操作人过滤真操作留痕 audit_events）。",
  opsLoadFailed: "操作日志读取失败",
  opsBusinessType: "业务类型",
  opsBusinessId: "业务对象",
  opsActor: "操作人",
  opsQuery: "查询",
  opsFocusHint: "正在按对象反查操作日志",
  opsEventName: "操作",
  opsActorCol: "操作人",
  opsTerminal: "终端",
  opsPermission: "权限码",
  opsRequestId: "请求 ID",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=events[&status=<success|failed>]；点 KPI 数字=带参重进本页只渲染事件表（对应子集，本页内）。
// 事件总数=本页事件表（服务端 total 跨页，聚焦仅呈现当前页表）；本页成功/失败=result 子集；本页分类=计数无行子集，KPI 数字保持纯文本不可点。
type KpiFocus = { status: "success" | "failed" | null };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "events") return null;
  const status = params.get("status");
  return { status: status === "success" || status === "failed" ? status : null };
}

// KPI result 子集：success=success/succeeded，failed=failed/blocked（与 KPI 计数口径一致）。
function matchesKpiStatus(row: AuditEventRow, status: KpiFocus["status"]): boolean {
  const result = String(row.result ?? "");
  if (status === "success") return ["success", "succeeded"].includes(result);
  if (status === "failed") return ["failed", "blocked"].includes(result);
  return true;
}

function textValue(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") return String(value);
  return JSON.stringify(value);
}

function eventTrace(row: AuditEventRow) {
  return row.traceId ?? row.trace_id ?? row.requestId ?? row.request_id ?? "-";
}

function eventObject(row: AuditEventRow) {
  const objectType = row.objectType ?? row.object_type ?? row.targetType ?? row.target_type ?? "-";
  const objectId = row.objectId ?? row.object_id ?? row.targetId ?? row.target_id ?? "-";
  return `${objectType}:${objectId}`;
}

// ponytail: 后端 evidence/events 只支持 page/size（无 keyword），故筛选仅作用于当前服务端页。
function matches(row: AuditEventRow, keyword: string) {
  if (!keyword.trim()) return true;
  const haystack = [row.eventType, row.event_type, row.eventCategory, row.event_category, row.summary, row.result, row.riskLevel, row.risk_level, eventTrace(row), eventObject(row)].map(textValue).join(" ").toLowerCase();
  return haystack.includes(keyword.trim().toLowerCase());
}

function eventKey(row: AuditEventRow, index: number) {
  return `${row.id ?? eventTrace(row)}-${index}`;
}

// 下钻定位：focus 可能是 `对象类型:对象id` 或纯 id；取 id 段作为页内筛选关键词（matches 的 haystack 含 `类型:id`）。
function focusKeyword(focus?: string) {
  if (!focus) return "";
  const trimmed = focus.trim();
  const colon = trimmed.lastIndexOf(":");
  return colon >= 0 ? trimmed.slice(colon + 1) : trimmed;
}

// 反查焦点解析：AuditTrailHint 传来的 `业务类型:业务id` 拆成操作日志的服务端过滤参数。
// 含冒号才认定为对象反查（切到操作日志视图并预填 business_type/business_id）；纯 id 仍走证据视图页内筛选。
function parseFocusObject(focus?: string): { businessType: string; businessId: string } | null {
  if (!focus) return null;
  const trimmed = focus.trim();
  const colon = trimmed.indexOf(":");
  if (colon <= 0 || colon >= trimmed.length - 1) return null;
  return { businessType: trimmed.slice(0, colon), businessId: trimmed.slice(colon + 1) };
}

export function AuditEventsPage({ api, user, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const focusObject = React.useMemo(() => parseFocusObject(focusObjectId), [focusObjectId]);
  // 反查（对象带 类型:id）默认进操作日志视图；否则保持证据事件视图。
  const [view, setView] = React.useState<"evidence" | "operations">(focusObject ? "operations" : "evidence");
  const [query, setQuery] = React.useState(() => focusKeyword(focusObjectId));
  const [exportReason, setExportReason] = React.useState("验收导出审计事件");
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const [showDetail, setShowDetail] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该审计事件完整字段（枚举中文·发生时间中文；trace/对象为机读标识保留原文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  // 操作日志视图（真 audit_events）状态：过滤条件 + 拉取结果。
  const [opBusinessType, setOpBusinessType] = React.useState(focusObject?.businessType ?? "");
  const [opBusinessId, setOpBusinessId] = React.useState(focusObject?.businessId ?? "");
  const [opActorId, setOpActorId] = React.useState("");
  const [opRows, setOpRows] = React.useState<AuditOperationEventRow[]>([]);
  const [opLoading, setOpLoading] = React.useState(false);
  const [opError, setOpError] = React.useState<string | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(status: KpiFocus["status"] = null) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "events");
    if (status) params.set("status", status);
    else params.delete("status");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ status });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("status");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<AuditEventRow>> => {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      return api.getPaged<AuditEventRow>(`/api/v1/evidence/events?${params.toString()}`);
    },
    [api],
  );

  const events = useServerPagedRows<AuditEventRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: TEXT.loadFailed,
  });

  // 操作日志拉取：按 business_type/business_id/actor_id 服务端过滤 audit_events。
  const loadOperations = React.useCallback(async () => {
    setOpLoading(true);
    setOpError(null);
    try {
      const params = new URLSearchParams({ limit: String(OPERATION_LIMIT) });
      if (opBusinessType.trim()) params.set("business_type", opBusinessType.trim());
      if (opBusinessId.trim()) params.set("business_id", opBusinessId.trim());
      if (opActorId.trim()) params.set("actor_id", opActorId.trim());
      const rows = await api.get<AuditOperationEventRow[]>(`/api/v1/audit/operation-events?${params.toString()}`);
      setOpRows(Array.isArray(rows) ? rows : []);
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : TEXT.opsLoadFailed;
      setOpError(message);
      setGlobalError(message);
    } finally {
      setOpLoading(false);
    }
  }, [api, opBusinessType, opBusinessId, opActorId, setGlobalError]);

  // 进入操作日志视图（含对象反查跳入）即自动拉取一次；后续手动改条件点「查询」。
  React.useEffect(() => {
    if (view === "operations") void loadOperations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view]);

  // 反查跳入（focus 带 类型:id）：切到操作日志视图并预填对象过滤。
  React.useEffect(() => {
    if (focusObject) {
      setView("operations");
      setOpBusinessType(focusObject.businessType);
      setOpBusinessId(focusObject.businessId);
    }
  }, [focusObject]);

  // 当前服务端页的行；筛选/KPI 均只作用于本页（后端未提供关键词与全量聚合）。
  const pageRows = events.rows;
  // 聚焦时叠加 KPI result 子集过滤（与关键词筛选并行，均限当前服务端页）。
  const filteredRows = pageRows.filter((row) => matches(row, query) && matchesKpiStatus(row, kpiFocus?.status ?? null));
  const kpiFocusTitle = kpiFocus?.status === "success" ? "本页成功事件" : kpiFocus?.status === "failed" ? "本页失败/阻断事件" : "本页全部事件";
  const selected = filteredRows[selectedIndex] ?? filteredRows[0] ?? pageRows[0];
  const successCount = pageRows.filter((row) => ["success", "succeeded"].includes(String(row.result ?? ""))).length;
  const failedCount = pageRows.filter((row) => ["failed", "blocked"].includes(String(row.result ?? ""))).length;
  const categoryCount = new Set(pageRows.map((row) => row.eventCategory ?? row.event_category).filter(Boolean)).size;

  React.useEffect(() => {
    if (selectedIndex >= filteredRows.length) setSelectedIndex(0);
  }, [filteredRows.length, selectedIndex]);

  // 下钻进入本页（?focus=对象）→ 用对象 id 预填页内筛选，聚焦相关审计事件；无 focus 时不改动查询。
  React.useEffect(() => {
    if (focusObjectId) setQuery(focusKeyword(focusObjectId));
  }, [focusObjectId]);

  async function exportAuditEvents() {
    if (!exportReason.trim()) {
      setFormError("导出原因不能为空。");
      return;
    }
    const payload = {
      // 导出留痕记录当前页快照与全量总数（后端未提供全量导出接口，保持诚实语义）。
      page: events.page,
      page_count: events.pageCount,
      total: events.total,
      page_exported_count: filteredRows.length,
      trace_ids: filteredRows.map(eventTrace).filter((trace) => trace !== "-"),
    };
    // M725-F02：导出发生时间在这次导出第一次点击时取一次，导出成功之前同样的导出内容都沿用（切页回来、中间导出过别的也一样）——
    // 没拿到结果再点时请求内容逐字不变，客户端才认得出是同一次操作、沿用首发的提交编号，不会再登记一条。
    const operation = ["audit-events-export", exportReason, payload];
    const request = {
      idempotency_key: `audit-export-${Date.now()}`,
      operator_note: exportReason,
      evidence_attachment_ids: ["att-audit-export"],
      device_code: "web-admin",
      staff_id: user.id,
      events: [{
        event_type: "audit_events_exported",
        event_category: "audit",
        result: "success",
        summary: exportReason,
        occurred_at: firstSubmitTime(operation),
        payload,
      }],
    };
    setBusyAction("audit_events_exported");
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/evidence/events", request);
      forgetSubmitTime(operation);
      setActionResult({ action: "audit_events_exported", audit: "evidence.events_reported", request, result });
      events.reload();
    } catch (exportError) {
      const message = exportError instanceof Error ? exportError.message : TEXT.exportFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "audit_events_exported", status: "failed", message });
    } finally {
      setBusyAction("");
    }
  }

  function openEventDetail(row: AuditEventRow) {
    setDetail({
      title: `${TEXT.title}明细 · ${eventTrace(row)}`,
      fields: [
        { label: TEXT.eventType, value: row.eventType ?? row.event_type ?? "-" },
        { label: TEXT.category, value: formatBusinessStatus(row.eventCategory ?? row.event_category) },
        { label: TEXT.result, value: formatBusinessStatus(row.result) },
        { label: TEXT.risk, value: formatBusinessStatus(row.riskLevel ?? row.risk_level) },
        { label: TEXT.source, value: formatBusinessStatus(row.sourceType ?? row.source_type) },
        { label: TEXT.trace, value: eventTrace(row) },
        { label: TEXT.object, value: eventObject(row) },
        { label: TEXT.summary, value: row.summary ?? "-" },
        { label: TEXT.occurredAt, value: cnDate(row.occurredAt ?? row.occurred_at ?? row.receivedAt ?? row.received_at) },
      ],
    });
  }

  function openOperationDetail(row: AuditOperationEventRow) {
    setDetail({
      title: `操作日志明细 · ${row.eventName ?? row.id ?? "-"}`,
      fields: [
        { label: TEXT.opsEventName, value: row.eventName ?? "-" },
        { label: TEXT.result, value: formatBusinessStatus(row.eventResult) },
        { label: TEXT.opsActorCol, value: `${formatBusinessStatus(row.actorType)} / ${row.actorId ?? "-"}` },
        { label: TEXT.opsBusinessType, value: formatBusinessStatus(row.businessType) },
        { label: TEXT.opsBusinessId, value: row.businessId ?? "-" },
        { label: TEXT.opsTerminal, value: terminalLabel(row.terminal) },
        { label: TEXT.opsPermission, value: row.permissionCode ?? "-" },
        { label: TEXT.opsRequestId, value: row.requestId ?? "-" },
        { label: TEXT.category, value: formatBusinessStatus(row.eventCategory) },
        { label: TEXT.occurredAt, value: cnDate(row.occurredAt) },
      ],
    });
  }

  const focusObjectLabel = focusObject ? `${focusObject.businessType}:${focusObject.businessId}` : "";

  return (
    <section data-testid="audit-events-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-form-row" data-testid="audit-events-view-switch">
        <button type="button" data-testid="audit-events-view-evidence" className={view === "evidence" ? "admin-tab admin-tab--active" : "admin-tab"} onClick={() => setView("evidence")}>{TEXT.viewEvidence}</button>
        <button type="button" data-testid="audit-events-view-operations" className={view === "operations" ? "admin-tab admin-tab--active" : "admin-tab"} onClick={() => setView("operations")}>{TEXT.viewOperations}</button>
      </div>

      {view === "operations" ? (
        <div data-testid="audit-events-operations">
          <TechDetails><div className="admin-generic-evidence" data-testid="audit-events-operations-evidence">{TEXT.opsEvidence}</div></TechDetails>
          {focusObject ? (
            <div className="admin-state" data-testid="audit-events-operations-focus-banner">
              {TEXT.opsFocusHint}：{focusObjectLabel}（{opRows.length}）
            </div>
          ) : null}
          <div className="admin-form-grid" data-testid="audit-events-operations-controls">
            <label>{TEXT.opsBusinessType}<input data-testid="audit-events-op-business-type" value={opBusinessType} onChange={(event) => setOpBusinessType(event.target.value)} /></label>
            <label>{TEXT.opsBusinessId}<input data-testid="audit-events-op-business-id" value={opBusinessId} onChange={(event) => setOpBusinessId(event.target.value)} /></label>
            <label>{TEXT.opsActor}<input data-testid="audit-events-op-actor-id" value={opActorId} onChange={(event) => setOpActorId(event.target.value)} /></label>
          </div>
          <div className="admin-form-row">
            <button type="button" data-testid="audit-events-op-query" disabled={opLoading} onClick={() => void loadOperations()}>{TEXT.opsQuery}</button>
          </div>
          <AdminDataState loading={opLoading} error={opError} empty={opRows.length === 0} onRetry={loadOperations}>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="audit-events-operations-table">
                <thead><tr><th>{TEXT.opsEventName}</th><th>{TEXT.result}</th><th>{TEXT.opsActorCol}</th><th>{TEXT.opsBusinessType}</th><th>{TEXT.opsBusinessId}</th><th>{TEXT.opsTerminal}</th><th>{TEXT.occurredAt}</th><th>详情</th></tr></thead>
                <tbody>{opRows.map((row, index) => (
                  <tr key={`${row.id ?? row.requestId ?? "op"}-${index}`} className="admin-row-clickable" tabIndex={0} data-testid={`audit-events-op-row-${index}`}
                    onClick={() => openOperationDetail(row)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOperationDetail(row); } }}>
                    <td>{row.eventName ?? "-"}</td>
                    <td>{formatBusinessStatus(row.eventResult)}</td>
                    <td>{row.actorId ?? "-"}</td>
                    <td>{formatBusinessStatus(row.businessType)}</td>
                    <td>{row.businessId ?? "-"}</td>
                    <td>{terminalLabel(row.terminal)}</td>
                    <td>{cnDate(row.occurredAt)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看操作日志详情" onClick={(event) => { event.stopPropagation(); openOperationDetail(row); }}>查看详情</button></td></tr>
                ))}</tbody>
              </table>
            </div>
          </AdminDataState>
        </div>
      ) : (
      <>
      <TechDetails><div className="admin-generic-evidence" data-testid="audit-events-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="evidence.event_viewed / evidence.events_reported" />
      <AdminDataState loading={events.loading} error={events.error} empty={events.total === 0} onRetry={events.reload}>
        {kpiFocus ? (
        <div className="admin-state" data-testid="audit-events-kpi-focus-banner">
          正在查看：{kpiFocusTitle}（{filteredRows.length}）
          <button type="button" data-testid="audit-events-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
        </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="audit-events-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.total}</span><button type="button" className="admin-kpi-value" data-testid="audit-events-kpi-total" onClick={() => enterKpiFocus()}>{events.total}</button><small>{TEXT.eventType}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.success}</span><button type="button" className="admin-kpi-value" data-testid="audit-events-kpi-success" onClick={() => enterKpiFocus("success")}>{successCount}</button><small>{TEXT.result}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.failed}</span><button type="button" className="admin-kpi-value" data-testid="audit-events-kpi-failed" onClick={() => enterKpiFocus("failed")}>{failedCount}</button><small>{TEXT.risk}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.categories}</span><strong>{categoryCount}</strong><small>{TEXT.category}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="audit-events-controls">
          <label>{TEXT.search}<input data-testid="audit-events-search" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          <label>{TEXT.exportReason}<input data-testid="audit-events-export-reason" value={exportReason} onChange={(event) => setExportReason(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <button type="button" data-testid="audit-events-show-detail" disabled={!selected} onClick={() => setShowDetail((value) => !value)}>{TEXT.detail}</button>
          <PermissionButton permissionsState={effectivePermissions} permissions={["evidence.event.create"]} data-testid="audit-events-export" disabled={busyAction !== ""} onClick={() => void exportAuditEvents()}>{TEXT.export}</PermissionButton>
          <span className="admin-muted-action" data-testid="audit-events-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="audit-events-form-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        {filteredRows.length === 0 ? <div className="admin-state admin-empty" data-testid="audit-events-filter-empty">{TEXT.noMatch}</div> : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="audit-events-table">
              <thead><tr><th>{TEXT.detail}</th><th>{TEXT.eventType}</th><th>{TEXT.category}</th><th>{TEXT.result}</th><th>{TEXT.risk}</th><th>{TEXT.source}</th><th>{TEXT.trace}</th><th>{TEXT.object}</th><th>{TEXT.summary}</th><th>{TEXT.occurredAt}</th><th>详情</th></tr></thead>
              <tbody>{filteredRows.map((row) => {
                const index = filteredRows.indexOf(row);
                return (
                <tr key={eventKey(row, index)} className="admin-row-clickable" tabIndex={0} data-testid={`audit-events-row-${index}`}
                  onClick={() => openEventDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEventDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择第 ${index + 1} 条审计事件`} data-testid={`audit-events-select-${index}`} checked={selected === row} onChange={() => setSelectedIndex(index)} /></td>
                  <td>{row.eventType ?? row.event_type ?? "-"}</td>
                  <td>{formatBusinessStatus(row.eventCategory ?? row.event_category)}</td>
                  <td>{formatBusinessStatus(row.result)}</td>
                  <td>{formatBusinessStatus(row.riskLevel ?? row.risk_level)}</td>
                  <td>{formatBusinessStatus(row.sourceType ?? row.source_type)}</td>
                  <td>{eventTrace(row)}</td>
                  <td>{eventObject(row)}</td>
                  <td>{row.summary ?? "-"}</td>
                  <td>{cnDate(row.occurredAt ?? row.occurred_at ?? row.receivedAt ?? row.received_at)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看审计事件详情" onClick={(event) => { event.stopPropagation(); openEventDetail(row); }}>查看详情</button></td></tr>
                );
              })}</tbody>
            </table>
            <TablePager testid="audit-events-pager" page={events.page} pageCount={events.pageCount} total={events.total} onPrev={events.prev} onNext={events.next} />
          </div>
        )}

        {showDetail && selected ? <pre className="admin-result" data-testid="audit-events-detail">{formatActionResult({ before_after: selected.beforeAfter ?? selected.before_after ?? selected.payload ?? {}, event: selected })}</pre> : null}
      </AdminDataState>
      </>
      )}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
