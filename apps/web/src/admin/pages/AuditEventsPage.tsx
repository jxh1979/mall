import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, AuditEventRow } from "../types";

const TEXT = {
  title: "审计事件",
  description: "统一查询员工、客户、第三方、财务和生产操作留痕；审计事件只能追加和导出留痕，不允许在业务后台编辑或删除历史事件。",
  evidence: "接口证据：读取/追加 /api/v1/evidence/events；导出动作本身通过 EventBatchRequest 写入 evidence.events_reported 留痕。",
  loadFailed: "审计事件读取失败",
  exportFailed: "审计事件导出留痕失败",
  total: "事件总数",
  success: "成功事件",
  failed: "失败/阻断",
  categories: "事件分类",
  search: "搜索 trace / 事件 / 对象 / 摘要",
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
  trace: "Trace ID",
  object: "对象",
  summary: "摘要",
  occurredAt: "发生时间",
};

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

function matches(row: AuditEventRow, keyword: string) {
  if (!keyword.trim()) return true;
  const haystack = [row.eventType, row.event_type, row.eventCategory, row.event_category, row.summary, row.result, row.riskLevel, row.risk_level, eventTrace(row), eventObject(row)].map(textValue).join(" ").toLowerCase();
  return haystack.includes(keyword.trim().toLowerCase());
}

function eventKey(row: AuditEventRow, index: number) {
  return `${row.id ?? eventTrace(row)}-${index}`;
}

function nowIso() {
  return new Date().toISOString();
}

export function AuditEventsPage({ api, user, setActionResult, setGlobalError }: AdminPageProps) {
  const [rows, setRows] = React.useState<AuditEventRow[]>([]);
  const [query, setQuery] = React.useState("");
  const [exportReason, setExportReason] = React.useState("验收导出审计事件");
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const [showDetail, setShowDetail] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<AuditEventRow[]>("/api/v1/evidence/events"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const filteredRows = rows.filter((row) => matches(row, query));
  const paged = usePagedRows(filteredRows, 10, query);
  const selected = filteredRows[selectedIndex] ?? filteredRows[0] ?? rows[0];
  const successCount = rows.filter((row) => ["success", "succeeded"].includes(String(row.result ?? ""))).length;
  const failedCount = rows.filter((row) => ["failed", "blocked"].includes(String(row.result ?? ""))).length;
  const categoryCount = new Set(rows.map((row) => row.eventCategory ?? row.event_category).filter(Boolean)).size;

  React.useEffect(() => {
    if (selectedIndex >= filteredRows.length) setSelectedIndex(0);
  }, [filteredRows.length, selectedIndex]);

  async function exportAuditEvents() {
    if (!exportReason.trim()) {
      setFormError("导出原因不能为空。");
      return;
    }
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
        occurred_at: nowIso(),
        payload: {
          exported_count: filteredRows.length,
          query,
          trace_ids: filteredRows.map(eventTrace).filter((trace) => trace !== "-"),
        },
      }],
    };
    setBusyAction("audit_events_exported");
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/evidence/events", request);
      setActionResult({ action: "audit_events_exported", audit: "evidence.events_reported", request, result });
      await reload();
    } catch (exportError) {
      const message = exportError instanceof Error ? exportError.message : TEXT.exportFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "audit_events_exported", status: "failed", message });
    } finally {
      setBusyAction("");
    }
  }

  return (
    <section data-testid="audit-events-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="audit-events-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="evidence.event_viewed / evidence.events_reported" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="audit-events-summary">
          <div className="admin-kpi-card"><span>{TEXT.total}</span><strong>{rows.length}</strong><small>{TEXT.eventType}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.success}</span><strong>{successCount}</strong><small>{TEXT.result}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.failed}</span><strong>{failedCount}</strong><small>{TEXT.risk}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.categories}</span><strong>{categoryCount}</strong><small>{TEXT.category}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="audit-events-controls">
          <label>{TEXT.search}<input data-testid="audit-events-search" value={query} onChange={(event) => setQuery(event.target.value)} /></label>
          <label>{TEXT.exportReason}<input data-testid="audit-events-export-reason" value={exportReason} onChange={(event) => setExportReason(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <button type="button" data-testid="audit-events-show-detail" disabled={!selected} onClick={() => setShowDetail((value) => !value)}>{TEXT.detail}</button>
          <button type="button" data-testid="audit-events-export" disabled={busyAction !== ""} onClick={() => void exportAuditEvents()}>{TEXT.export}</button>
          <span className="admin-muted-action" data-testid="audit-events-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="audit-events-form-error" className="admin-inline-error">{formError}</div> : null}

        {filteredRows.length === 0 ? <div className="admin-state admin-empty" data-testid="audit-events-filter-empty">{TEXT.noMatch}</div> : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="audit-events-table">
              <thead><tr><th>{TEXT.detail}</th><th>{TEXT.eventType}</th><th>{TEXT.category}</th><th>{TEXT.result}</th><th>{TEXT.risk}</th><th>{TEXT.source}</th><th>{TEXT.trace}</th><th>{TEXT.object}</th><th>{TEXT.summary}</th><th>{TEXT.occurredAt}</th></tr></thead>
              <tbody>{paged.pageRows.map((row) => {
                const index = filteredRows.indexOf(row);
                return (
                <tr key={eventKey(row, index)}>
                  <td><input type="radio" data-testid={`audit-events-select-${index}`} checked={selected === row} onChange={() => setSelectedIndex(index)} /></td>
                  <td>{row.eventType ?? row.event_type ?? "-"}</td>
                  <td>{row.eventCategory ?? row.event_category ?? "-"}</td>
                  <td>{row.result ?? "-"}</td>
                  <td>{row.riskLevel ?? row.risk_level ?? "-"}</td>
                  <td>{row.sourceType ?? row.source_type ?? "-"}</td>
                  <td>{eventTrace(row)}</td>
                  <td>{eventObject(row)}</td>
                  <td>{row.summary ?? "-"}</td>
                  <td>{row.occurredAt ?? row.occurred_at ?? row.receivedAt ?? row.received_at ?? "-"}</td>
                </tr>
                );
              })}</tbody>
            </table>
            <TablePager testid="audit-events-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
          </div>
        )}

        {showDetail && selected ? <pre className="admin-result" data-testid="audit-events-detail">{JSON.stringify({ beforeAfter: selected.beforeAfter ?? selected.before_after ?? selected.payload ?? {}, event: selected }, null, 2)}</pre> : null}
      </AdminDataState>
    </section>
  );
}
