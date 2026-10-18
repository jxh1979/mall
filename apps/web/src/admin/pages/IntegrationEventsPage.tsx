// 文件版本：V0.01
import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { rowMatchesKeyword } from "../components/rowSearch";

// 后端写权限码（IntegrationController）
const RETRY_MANAGE_PERMISSION = "integration.retry.manage";
const COMPENSATION_MANAGE_PERMISSION = "integration.compensation.manage";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, IntegrationEventRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

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
  totalHelp: "四类读取接口",
  pendingHelp: "待处理事件",
  providersHelp: "涉及供应商",
  focusClear: "清除筛选，返回全部",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点某一来源数字=带 ?kpi=<source> 参数重进本页，只渲染该来源的事件表。
// 合法值取自 ENDPOINTS.source：retry_task / compensation_task / order_sync / voucher_verification。
const KPI_SOURCES = ENDPOINTS.map((e) => e.source) as readonly string[];
function readKpiFocusFromUrl(): string | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  return target && KPI_SOURCES.includes(target) ? target : null;
}
function sourceLabelOf(source: string): string {
  return ENDPOINTS.find((e) => e.source === source)?.label ?? source;
}

type Endpoint = typeof ENDPOINTS[number];

type LoadedEventRow = IntegrationEventRow & {
  providerPayload?: { refundId?: string; rejectionCode?: string; refund_id?: string; rejection_code?: string };
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

export function IntegrationEventsPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<LoadedEventRow[]>([]);
  const [pollingNote, setPollingNote] = React.useState("已完成 Webhook 与定时轮询一致性校验");
  const [retryReason, setRetryReason] = React.useState("供应商超时后人工触发重试");
  const [compensationNote, setCompensationNote] = React.useState("已核对订单和券核销回流，处理补偿任务");
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<string | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该回调/轮询事件完整字段（状态中文·时间中文·任务号可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(source: string) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", source);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(source);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

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
  const sourceCount = (source: string) => rows.filter((row) => row.source === source).length;
  const selectedRetry = rows.find((row) => row.source === "retry_task");
  const selectedCompensation = rows.find((row) => row.source === "compensation_task");
  // 聚焦时先按来源过滤内存数组，再叠加关键词过滤；不发起后端查询。
  const kpiRows = kpiFocus ? rows.filter((row) => row.source === kpiFocus) : rows;
  const filteredRows = kpiRows.filter((row) => rowMatchesKeyword(row, query));
  const paged = usePagedRows(filteredRows, 10, query + "|" + (kpiFocus || ""));

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

  async function logPollingCheck() {
    if (!pollingNote.trim()) {
      setFormError("轮询校验说明不能为空。");
      return;
    }
    const request = {
      idempotency_key: nextKey(),
      operator_note: pollingNote,
      evidence_attachment_ids: ["att-integration-polling"],
      checked_endpoints: ENDPOINTS.map((endpoint) => endpoint.endpoint),
      pending_count: pendingCount,
      source_count: ENDPOINTS.length,
    };
    await runAction("integration_polling_checked", request, () => api.post("/api/v1/integration/polling-checks", request));
  }

  function validateRetry() {
    if (!selectedRetry?.id) {
      setFormError(TEXT.noRetry);
      return false;
    }
    return true;
  }

  function validateCompensation() {
    if (!selectedCompensation?.id) {
      setFormError(TEXT.noCompensation);
      return false;
    }
    return true;
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

  function openEventDetail(row: LoadedEventRow) {
    setDetail({
      title: `事件明细 · ${row.taskNo ?? row.orderNo ?? row.verificationNo ?? row.id ?? "-"}`,
      fields: [
        { label: TEXT.source, value: row.sourceLabel },
        {
          label: TEXT.taskNo,
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-TKT-003" objectId={row.orderNo}>{row.taskNo ?? row.orderNo ?? row.verificationNo ?? row.id ?? "-"}</TraceLink>,
        },
        { label: TEXT.provider, value: row.providerCode ?? row.channelCode ?? "-" },
        { label: TEXT.eventType, value: row.eventType ?? row.syncType ?? row.verificationType ?? "-" },
        { label: TEXT.status, value: formatBusinessStatus(row.status) },
        ...((row.providerPayload?.rejectionCode ?? row.providerPayload?.rejection_code) === "PARK_WIDE_FAULT_SALES_SUSPENDED" ? [
          { label: "受理结论", value: "全园故障停售，订单未受理且不出票" },
        ] : []),
        ...(row.providerPayload?.refundId || row.providerPayload?.refund_id ? [
          { label: "原路待退款", value: String(row.providerPayload.refundId ?? row.providerPayload.refund_id) },
          { label: "退款办理", value: <button type="button" onClick={() => navigateTo?.("ADM-FIN-001")}>前往财务台账查看退款实际状态</button> },
        ] : []),
        { label: TEXT.retryCount, value: row.retryCount ?? "-" },
        { label: "错误码", value: row.errorCode ?? "-" },
        { label: "发生时间", value: cnDate(row.occurredAt) },
        { label: "创建时间", value: cnDate(row.createdAt) },
        { label: TEXT.updatedAt, value: cnDate(row.updatedAt ?? row.createdAt ?? row.occurredAt) },
      ],
    });
  }

  return (
    <section data-testid="integration-events-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="integration-events-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="integration.retry_task.viewed / integration.compensation_task.viewed / integration.order_sync.viewed / integration.voucher_verification.viewed / local.integration_polling_checked" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="integration-events-kpi-focus-banner">
            正在查看：{sourceLabelOf(kpiFocus)}（{kpiRows.length}）
            <button type="button" data-testid="integration-events-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="integration-events-summary">
          <div className="admin-kpi-card"><span>{TEXT.total}</span><strong>{rows.length}</strong><small>{TEXT.totalHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.pending}</span><strong>{pendingCount}</strong><small>{TEXT.pendingHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.providers}</span><strong>{providerCount}</strong><small>{TEXT.providersHelp}</small></div>
          {ENDPOINTS.map((endpoint) => (
            <div key={endpoint.source} className="admin-kpi-card admin-kpi-card--clickable"><span>{endpoint.label}</span><button type="button" className="admin-kpi-value" data-testid={`integration-events-kpi-${endpoint.source}`} onClick={() => enterKpiFocus(endpoint.source)}>{sourceCount(endpoint.source)}</button><small>{TEXT.source}</small></div>
          ))}
        </div>

        <div className="admin-form-grid" data-testid="integration-events-form">
          <label>{TEXT.pollingNote}<input data-testid="integration-events-polling-note" value={pollingNote} onChange={(event) => setPollingNote(event.target.value)} /></label>
          <label>{TEXT.retryReason}<input data-testid="integration-events-retry-reason" value={retryReason} onChange={(event) => setRetryReason(event.target.value)} /></label>
          <label>{TEXT.compensationNote}<input data-testid="integration-events-compensation-note" value={compensationNote} onChange={(event) => setCompensationNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <button type="button" data-testid="integration-events-log-polling" disabled={busyAction !== ""} onClick={() => void logPollingCheck()}>{TEXT.logPolling}</button>
          <HighRiskConfirm
            label={TEXT.runRetry}
            impact="将立即触发选中重试任务并更新平台任务状态与审计；仅代表平台侧重试，不等于供应商生产补偿已执行。"
            disabled={busyAction !== ""}
            precheck={validateRetry}
            onConfirm={() => void runRetryNow()}
            permissionsState={effectivePermissions}
            permissions={[RETRY_MANAGE_PERMISSION]}
            data-testid="integration-events-run-retry"
          />
          <HighRiskConfirm
            label={TEXT.resolveCompensation}
            impact="将把选中补偿任务置为已处理并写入审计；仅更新平台任务状态，不代表供应商真实补偿已完成。"
            disabled={busyAction !== ""}
            precheck={validateCompensation}
            onConfirm={() => void resolveCompensation()}
            permissionsState={effectivePermissions}
            permissions={[COMPENSATION_MANAGE_PERMISSION]}
            data-testid="integration-events-resolve-compensation"
          />
          <span className="admin-muted-action" data-testid="integration-events-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="integration-events-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        <div className="admin-form-row" data-testid="integration-events-filter">
          <input data-testid="integration-events-search" value={query} onChange={(event) => setQuery(event.target.value)} aria-label="搜索来源 / 任务号 / 供应商 / 状态" placeholder="搜索来源 / 任务号 / 供应商 / 状态" />
          <span className="admin-muted-action" data-testid="integration-events-filter-count">匹配 {filteredRows.length} / {rows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="integration-events-filter-empty">无匹配事件</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="integration-events-table">
            <thead><tr><th>{TEXT.source}</th><th>{TEXT.taskNo}</th><th>{TEXT.provider}</th><th>{TEXT.eventType}</th><th>{TEXT.status}</th><th>{TEXT.retryCount}</th><th>{TEXT.updatedAt}</th><th>详情</th></tr></thead>
            <tbody>{paged.pageRows.map((row, index) => (
              <tr key={eventKey(row, index)} className="admin-row-clickable" tabIndex={0} data-testid={`integration-event-row-${row.id ?? row.taskNo ?? index}`}
                onClick={() => openEventDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEventDetail(row); } }}>
                <td>{row.sourceLabel}</td>
                <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-TKT-003" objectId={row.orderNo}>{row.taskNo ?? row.orderNo ?? row.verificationNo ?? row.id ?? "-"}</TraceLink></td>
                <td>{row.providerCode ?? row.channelCode ?? "-"}</td>
                <td>{row.eventType ?? row.syncType ?? row.verificationType ?? "-"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
                <td>{row.retryCount ?? "-"}</td>
                <td>{cnDate(row.updatedAt ?? row.createdAt ?? row.occurredAt)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看事件详情" onClick={(event) => { event.stopPropagation(); openEventDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
          <TablePager testid="integration-events-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
