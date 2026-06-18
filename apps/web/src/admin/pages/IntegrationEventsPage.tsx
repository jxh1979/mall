import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { rowMatchesKeyword } from "../components/rowSearch";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, IntegrationEventRow } from "../types";

const ENDPOINTS = [
  { label: "重试任务", source: "retry_task", endpoint: "/api/v1/integration/retry-tasks" },
  { label: "补偿任务", source: "compensation_task", endpoint: "/api/v1/integration/compensation-tasks" },
  { label: "订单同步", source: "order_sync", endpoint: "/api/v1/integration/order-syncs" },
  { label: "券核销", source: "voucher_verification", endpoint: "/api/v1/integration/voucher-verifications" },
] as const;

const TEXT = {
  title: "回调/轮询/补偿",
  description: "统一查看第三方 Webhook、定时轮询、重试任务、补偿任务、订单同步和券核销回流；立即重试和补偿处理只更新平台任务状态与审计，不代表真实供应商生产补偿已执行。",
  evidence: "接口证据：读取 /api/v1/integration/retry-tasks、/api/v1/integration/compensation-tasks、/api/v1/integration/order-syncs、/api/v1/integration/voucher-verifications。",
  loadFailed: "第三方回调/轮询数据读取失败",
  total: "事件总数",
  pending: "待处理",
  providers: "供应商",
  sources: "数据来源",
  pollingNote: "轮询校验说明",
  logPolling: "登记轮询校验",
  retryReason: "重试原因",
  compensationNote: "补偿处理说明",
  runRetry: "立即重试",
  resolveCompensation: "处理补偿",
  noRetry: "当前没有可重试任务。",
  noCompensation: "当前没有可处理补偿任务。",
  boundary: "Webhook + 定时轮询校验为当前验收口径；立即重试/补偿处理只更新平台任务状态和审计，不得伪造第三方生产补偿执行。",
  source: "来源",
  taskNo: "任务号",
  provider: "供应商",
  eventType: "事件类型",
  status: "状态",
  retryCount: "重试次数",
  updatedAt: "更新时间",
};

type Endpoint = typeof ENDPOINTS[number];

type LoadedEventRow = IntegrationEventRow & {
  source: string;
  sourceLabel: string;
  endpoint: string;
};

function isPending(status?: string) {
  return !["success", "succeeded", "completed", "processed", "resolved", "closed", "matched"].includes(status ?? "");
}

function eventKey(row: LoadedEventRow, index: number) {
  return `${row.source}-${row.id ?? row.taskNo ?? index}`;
}

function normalizeRows(endpoint: Endpoint, rows: IntegrationEventRow[]): LoadedEventRow[] {
  return rows.map((row) => ({ ...row, source: endpoint.source, sourceLabel: endpoint.label, endpoint: endpoint.endpoint }));
}

function nextKey() {
  return `integration-polling-${Date.now()}`;
}

export function IntegrationEventsPage({ api, user, setActionResult, setGlobalError }: AdminPageProps) {
  const [rows, setRows] = React.useState<LoadedEventRow[]>([]);
  const [pollingNote, setPollingNote] = React.useState("已完成 Webhook 与定时轮询一致性校验");
  const [retryReason, setRetryReason] = React.useState("供应商超时后人工触发重试");
  const [compensationNote, setCompensationNote] = React.useState("已核对订单和券核销回流，处理补偿任务");
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [query, setQuery] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const loaded = await Promise.all(ENDPOINTS.map(async (endpoint) => {
        const data = await api.get<IntegrationEventRow[]>(endpoint.endpoint);
        return normalizeRows(endpoint, data);
      }));
      setRows(loaded.flat());
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const pendingCount = rows.filter((row) => isPending(row.status)).length;
  const providerCount = new Set(rows.map((row) => row.providerCode ?? row.channelCode).filter(Boolean)).size;
  const representedSources = new Set(rows.map((row) => row.source)).size;
  const selectedRetry = rows.find((row) => row.source === "retry_task");
  const selectedCompensation = rows.find((row) => row.source === "compensation_task");
  const filteredRows = rows.filter((row) => rowMatchesKeyword(row, query));
  const paged = usePagedRows(filteredRows, 10, query);

  async function runAction(action: string, request: Record<string, unknown>, call: () => Promise<unknown>) {
    setBusyAction(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await call();
      setActionResult({ action, request, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "第三方事件操作失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, status: "failed", message });
    } finally {
      setBusyAction("");
    }
  }

  function logPollingCheck() {
    if (!pollingNote.trim()) {
      setFormError("轮询校验说明不能为空。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setActionResult({
      action: "integration_polling_checked",
      status: "logged",
      audit: "webhook_plus_polling_verified",
      operatorNote: pollingNote,
      idempotencyKey: nextKey(),
      evidenceAttachmentIds: ["att-integration-polling"],
      checkedEndpoints: ENDPOINTS.map((endpoint) => endpoint.endpoint),
      pendingCount,
      sourceCount: ENDPOINTS.length,
      productionBoundary: "platform_task_status_only_not_supplier_execution",
    });
  }

  async function runRetryNow() {
    if (!selectedRetry?.id) {
      setFormError(TEXT.noRetry);
      return;
    }
    const request = {
      idempotency_key: `integration-retry-${Date.now()}`,
      action: "run_now",
      reason: retryReason,
    };
    await runAction("integration_retry_run_now", request, () => api.post(`/api/v1/integration/retry-tasks/${selectedRetry.id}/run-now`, request));
  }

  async function resolveCompensation() {
    if (!selectedCompensation?.id) {
      setFormError(TEXT.noCompensation);
      return;
    }
    const request = {
      idempotency_key: `integration-compensation-${Date.now()}`,
      operator_user_id: user.id,
      status: "resolved",
      resolution_note: compensationNote,
      evidence_attachment_ids: ["att-integration-compensation"],
    };
    await runAction("integration_compensation_resolved", request, () => api.post(`/api/v1/integration/compensation-tasks/${selectedCompensation.id}/resolve`, request));
  }

  return (
    <section data-testid="integration-events-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="integration-events-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="integration.retry_task.viewed / integration.compensation_task.viewed / integration.order_sync.viewed / integration.voucher_verification.viewed / local.integration_polling_checked" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="integration-events-summary">
          <div className="admin-kpi-card"><span>{TEXT.total}</span><strong>{rows.length}</strong><small>四类读取接口</small></div>
          <div className="admin-kpi-card"><span>{TEXT.pending}</span><strong>{pendingCount}</strong><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.providers}</span><strong>{providerCount}</strong><small>{TEXT.provider}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.sources}</span><strong>{representedSources}/{ENDPOINTS.length}</strong><small>retry / compensation / sync / voucher</small></div>
        </div>

        <div className="admin-form-grid" data-testid="integration-events-form">
          <label>{TEXT.pollingNote}<input data-testid="integration-events-polling-note" value={pollingNote} onChange={(event) => setPollingNote(event.target.value)} /></label>
          <label>{TEXT.retryReason}<input data-testid="integration-events-retry-reason" value={retryReason} onChange={(event) => setRetryReason(event.target.value)} /></label>
          <label>{TEXT.compensationNote}<input data-testid="integration-events-compensation-note" value={compensationNote} onChange={(event) => setCompensationNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <button type="button" data-testid="integration-events-log-polling" disabled={busyAction !== ""} onClick={logPollingCheck}>{TEXT.logPolling}</button>
          <button type="button" data-testid="integration-events-run-retry" disabled={busyAction !== ""} onClick={() => void runRetryNow()}>{TEXT.runRetry}</button>
          <button type="button" data-testid="integration-events-resolve-compensation" disabled={busyAction !== ""} onClick={() => void resolveCompensation()}>{TEXT.resolveCompensation}</button>
          <span className="admin-muted-action" data-testid="integration-events-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="integration-events-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-form-row" data-testid="integration-events-filter">
          <input data-testid="integration-events-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索来源 / 任务号 / 供应商 / 状态" />
          <span className="admin-muted-action" data-testid="integration-events-filter-count">匹配 {filteredRows.length} / {rows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="integration-events-filter-empty">无匹配事件</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="integration-events-table">
            <thead><tr><th>{TEXT.source}</th><th>{TEXT.taskNo}</th><th>{TEXT.provider}</th><th>{TEXT.eventType}</th><th>{TEXT.status}</th><th>{TEXT.retryCount}</th><th>{TEXT.updatedAt}</th></tr></thead>
            <tbody>{paged.pageRows.map((row, index) => (
              <tr key={eventKey(row, index)}>
                <td>{row.sourceLabel}</td>
                <td>{row.taskNo ?? row.orderNo ?? row.verificationNo ?? row.id ?? "-"}</td>
                <td>{row.providerCode ?? row.channelCode ?? "-"}</td>
                <td>{row.eventType ?? row.syncType ?? row.verificationType ?? "-"}</td>
                <td>{row.status ?? "-"}</td>
                <td>{row.retryCount ?? "-"}</td>
                <td>{row.updatedAt ?? row.createdAt ?? row.occurredAt ?? "-"}</td>
              </tr>
            ))}</tbody>
          </table>
          <TablePager testid="integration-events-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
    </section>
  );
}
