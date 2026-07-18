import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, MerchantWorkOrderRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

const WO_CREATE = ["merchant.service_work_order.create"];
const WO_ASSIGN = ["merchant.service_work_order.assign"];
const WO_PROCESS = ["merchant.service_work_order.process"];
const WO_CLOSE = ["merchant.service_work_order.close"];

const TEXT = {
  title: "商户服务工单",
  description: "覆盖商户维修、投诉、整改和服务类工单的创建、派单、处理和关闭闭环，所有写动作必须带幂等键、操作原因和附件证据。",
  evidence: "接口证据：读取/创建 /api/v1/merchant-service/work-orders；派单 POST /api/v1/merchant-service/work-orders/{id}/assign；处理 POST /api/v1/merchant-service/work-orders/{id}/actions；关闭 POST /api/v1/merchant-service/work-orders/{id}/close。",
  loadFailed: "商户服务工单读取失败",
  actionFailed: "商户服务工单操作失败",
  noWorkOrder: "当前没有可操作工单。",
  titleRequired: "创建工单必须填写标题。",
  processNoteRequired: "处理动作必须填写处理说明。",
  resolutionRequired: "关闭工单必须填写处理结论。",
  workOrderCount: "工单总数",
  pending: "待处理",
  highPriority: "高优先级",
  selected: "选中工单",
  boundary: "维修 / 投诉 / 整改 / 服务均走统一工单；派单、处理、关闭必须留痕，生产附件与逾期提醒仍按生产证据台账回填。",
  create: "创建工单",
  assign: "派单",
  process: "追加处理",
  close: "关闭工单",
  workOrderTitle: "工单标题",
  descriptionLabel: "问题描述",
  type: "类型",
  priority: "优先级",
  merchant: "商户",
  ownerDepartment: "负责部门",
  ownerUser: "负责人",
  processNote: "处理说明",
  targetStatus: "目标状态",
  resolution: "处理结论",
  satisfaction: "满意度",
  select: "选择",
  status: "状态",
  dueAt: "截止时间",
  evidenceLabel: "附件证据",
  focusClear: "清除筛选，返回全部",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<all|pending|high>；点 KPI 数字=带参重进本页只渲染工单表（对应子集）。
// 本页单表：工单总数=全量、待处理=未终结子集、高优先级=high/critical 子集；选中工单=交互态无子集，KPI 数字保持纯文本不可点。
type KpiTarget = "all" | "pending" | "high";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "all" && table !== "pending" && table !== "high") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  if (focus.table === "pending") return "待处理工单";
  if (focus.table === "high") return "高优先级工单";
  return "全部工单";
}

// KPI 子集过滤：聚焦时先按被点 KPI 把 rows 收窄，再进搜索/排序/分页管线。
function kpiSubset(rows: MerchantWorkOrderRow[], focus: KpiFocus | null): MerchantWorkOrderRow[] {
  if (!focus || focus.table === "all") return rows;
  if (focus.table === "pending") return rows.filter(nonTerminal);
  return rows.filter((row) => ["high", "critical"].includes(row.priority ?? ""));
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function nonTerminal(row: MerchantWorkOrderRow) {
  return !["closed", "cancelled"].includes(row.status ?? "");
}

function evidenceList(row: MerchantWorkOrderRow) {
  return (row.evidenceAttachmentIds ?? []).join(", ") || "-";
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, evidenceAttachmentIds: request.evidenceAttachmentIds, result };
}

export function MerchantWorkOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantWorkOrderRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [title, setTitle] = React.useState("空调漏水复查");
  const [description, setDescription] = React.useState("商户反馈维修后仍需复查");
  const [workOrderType, setWorkOrderType] = React.useState("repair");
  const [priority, setPriority] = React.useState("high");
  const [ownerDepartment, setOwnerDepartment] = React.useState("property");
  const [ownerUserId, setOwnerUserId] = React.useState("staff_owner");
  const [processNote, setProcessNote] = React.useState("已到场处理，等待商户确认");
  const [targetStatus, setTargetStatus] = React.useState("processing");
  const [resolution, setResolution] = React.useState("已维修完成并确认关闭");
  const [satisfactionScore, setSatisfactionScore] = React.useState("5");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该工单完整字段（枚举中文·截止时间中文·引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-4：新建工单收起为主按钮 + 分步引导弹窗（派单/处理/关闭动作表单不变）。
  const [createOpen, setCreateOpen] = React.useState(false);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
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
      const workOrders = await api.get<MerchantWorkOrderRow[]>("/api/v1/merchant-service/work-orders");
      setRows(workOrders);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const pendingCount = rows.filter(nonTerminal).length;
  const highPriorityCount = rows.filter((row) => ["high", "critical"].includes(row.priority ?? "")).length;

  // 聚焦时先按 KPI 子集收窄，再进搜索/排序/分页；非聚焦=全量。
  const baseRows = kpiSubset(rows, kpiFocus);
  const filteredRows = baseRows.filter((row) => rowMatchesKeyword(row, query));
  const sort = useSortedRows(filteredRows, {
    title: (row) => row.title ?? row.id,
    type: (row) => row.workOrderType,
    priority: (row) => row.priority,
    status: (row) => row.status,
    dueAt: (row) => row.dueAt,
  });
  const paged = usePagedRows(sort.sorted, 10, `${kpiFocus?.table ?? "none"}:${query}:${sort.key}:${sort.direction}`);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function createWorkOrder() {
    if (!title.trim()) {
      setFormError(TEXT.titleRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-create"),
      operatorNote: "完整后台创建商户服务工单",
      evidenceAttachmentIds: ["att-merchant-work-order-create"],
      merchantId: "merchant-1",
      spaceId: "area-1",
      contractId: "contract-1",
      workOrderType,
      priority,
      title,
      description,
      reportedByUserId: "staff_reporter",
      reportedChannel: "web_admin",
      dueAt: "2026-06-16T10:00:00.000Z",
    };
    const ok = await runAction("merchant_work_order_created", request, () => api.post("/api/v1/merchant-service/work-orders", request));
    if (ok) setCreateOpen(false);
  }

  async function assignWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-assign"),
      operatorNote: "完整后台商户工单派单",
      evidenceAttachmentIds: ["att-merchant-work-order-assign"],
      ownerDepartment,
      ownerUserId,
      operatorUserId: "staff_dispatcher",
    };
    await runAction("merchant_work_order_assigned", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/assign`, request));
  }

  async function processWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    if (!processNote.trim()) {
      setFormError(TEXT.processNoteRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-process"),
      operatorNote: processNote,
      evidenceAttachmentIds: ["att-merchant-work-order-process"],
      targetStatus,
      operatorUserId: "staff_processor",
    };
    await runAction("merchant_work_order_processed", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/actions`, request));
  }

  async function closeWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    if (!resolution.trim()) {
      setFormError(TEXT.resolutionRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-close"),
      operatorNote: "完整后台关闭商户服务工单",
      evidenceAttachmentIds: ["att-merchant-work-order-close"],
      resolution,
      satisfactionScore: Number(satisfactionScore || 5),
      operatorUserId: "staff_closer",
    };
    await runAction("merchant_work_order_closed", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/close`, request));
  }

  function openWorkOrderDetail(row: MerchantWorkOrderRow) {
    setDetail({
      title: `工单明细 · ${row.title ?? row.id}`,
      fields: [
        { label: TEXT.workOrderTitle, value: row.title ?? row.id },
        { label: TEXT.merchant, value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink> },
        { label: TEXT.type, value: formatBusinessStatus(row.workOrderType) },
        { label: TEXT.priority, value: formatBusinessStatus(row.priority) },
        { label: TEXT.status, value: formatBusinessStatus(row.status) },
        { label: TEXT.ownerDepartment, value: row.ownerDepartment || "-" },
        { label: TEXT.ownerUser, value: <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.ownerUserId}>{row.ownerUserId || "-"}</TraceLink> },
        { label: TEXT.dueAt, value: cnDate(row.dueAt) },
        { label: TEXT.evidenceLabel, value: evidenceList(row) },
      ],
    });
  }

  return (
    <section data-testid="merchant-work-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="merchant-work-orders-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="merchant_service.work_order.viewed / created / assigned / action_added / closed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
        <div className="admin-state" data-testid="merchant-work-orders-kpi-focus-banner">
          正在查看：{kpiFocusTitle(kpiFocus)}（{baseRows.length}）
          <button type="button" data-testid="merchant-work-orders-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
        </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="merchant-work-orders-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.workOrderCount}</span><button type="button" className="admin-kpi-value" data-testid="merchant-work-orders-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>{TEXT.merchant}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.pending}</span><button type="button" className="admin-kpi-value" data-testid="merchant-work-orders-kpi-pending" onClick={() => enterKpiFocus("pending")}>{pendingCount}</button><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.highPriority}</span><button type="button" className="admin-kpi-value" data-testid="merchant-work-orders-kpi-high" onClick={() => enterKpiFocus("high")}>{highPriorityCount}</button><small>{TEXT.priority}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong className="admin-kpi-value--text" title={selected?.title ?? "-"}>{selected?.title ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div data-testid="merchant-work-order-create-form">
          <div className="admin-form-row">
            <PermissionButton permissionsState={effectivePermissions} permissions={WO_CREATE} data-testid="merchant-work-order-create-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ {TEXT.create}</PermissionButton>
            <span className="admin-muted-action" data-testid="merchant-work-orders-boundary">{TEXT.boundary}</span>
          </div>
          <GuidedFlowModal
            open={createOpen}
            title={TEXT.create}
            testId="merchant-work-order-guided"
            submitTestId="merchant-work-order-create"
            submitLabel={TEXT.create}
            submitting={busyAction !== ""}
            error={formError || undefined}
            onClose={() => setCreateOpen(false)}
            onSubmit={() => void createWorkOrder()}
            steps={[
              {
                title: "工单信息",
                description: "填写工单标题与问题描述。",
                validate: () => (title.trim() ? null : TEXT.titleRequired),
                content: (
                  <>
                    <label>{TEXT.workOrderTitle}<input data-testid="merchant-work-order-title" value={title} onChange={(event) => setTitle(event.target.value)} /></label>
                    <label>{TEXT.descriptionLabel}<input data-testid="merchant-work-order-description" value={description} onChange={(event) => setDescription(event.target.value)} /></label>
                  </>
                ),
              },
              {
                title: "分类与优先级",
                description: "选择工单类型与优先级。",
                content: (
                  <>
                    <label>{TEXT.type}<select data-testid="merchant-work-order-type" value={workOrderType} onChange={(event) => setWorkOrderType(event.target.value)}><option value="repair">报修</option><option value="complaint">投诉</option><option value="rectification">整改</option><option value="service">服务</option></select></label>
                    <label>{TEXT.priority}<select data-testid="merchant-work-order-priority" value={priority} onChange={(event) => setPriority(event.target.value)}><option value="low">低</option><option value="medium">中</option><option value="high">高</option><option value="critical">紧急</option></select></label>
                  </>
                ),
              },
            ]}
            summary={[
              { label: TEXT.workOrderTitle, value: title.trim() || "未填写" },
              { label: TEXT.descriptionLabel, value: description.trim() || "未填写" },
              { label: TEXT.type, value: formatBusinessStatus(workOrderType) },
              { label: TEXT.priority, value: formatBusinessStatus(priority) },
            ]}
          />
        </div>

        <div className="admin-form-grid" data-testid="merchant-work-order-action-form">
          <label>{TEXT.ownerDepartment}<input data-testid="merchant-work-order-owner-department" value={ownerDepartment} onChange={(event) => setOwnerDepartment(event.target.value)} /></label>
          <label>{TEXT.ownerUser}<input data-testid="merchant-work-order-owner-user" value={ownerUserId} onChange={(event) => setOwnerUserId(event.target.value)} /></label>
          <label>{TEXT.processNote}<input data-testid="merchant-work-order-process-note" value={processNote} onChange={(event) => setProcessNote(event.target.value)} /></label>
          <label>{TEXT.targetStatus}<select data-testid="merchant-work-order-target-status" value={targetStatus} onChange={(event) => setTargetStatus(event.target.value)}><option value="assigned">已派单</option><option value="processing">处理中</option><option value="pending_merchant">商户待处理</option><option value="resolved">已解决</option></select></label>
          <label>{TEXT.resolution}<input data-testid="merchant-work-order-resolution" value={resolution} onChange={(event) => setResolution(event.target.value)} /></label>
          <label>{TEXT.satisfaction}<input data-testid="merchant-work-order-satisfaction" value={satisfactionScore} onChange={(event) => setSatisfactionScore(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="merchant-work-order-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_ASSIGN} data-testid="merchant-work-order-assign" disabled={busyAction !== ""} onClick={() => void assignWorkOrder()}>{TEXT.assign}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_PROCESS} data-testid="merchant-work-order-process" disabled={busyAction !== ""} onClick={() => void processWorkOrder()}>{TEXT.process}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_CLOSE} data-testid="merchant-work-order-close" disabled={busyAction !== ""} onClick={() => void closeWorkOrder()}>{TEXT.close}</PermissionButton>
        </div>
        {formError ? <div data-testid="merchant-work-order-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        <div className="admin-list-toolbar" data-testid="merchant-work-orders-filter">
          <input data-testid="merchant-work-orders-search" value={query} onChange={(event) => setQuery(event.target.value)} aria-label="搜索标题 / 类型 / 优先级 / 状态 / 负责人" placeholder="搜索标题 / 类型 / 优先级 / 状态 / 负责人" />
          <span className="admin-muted-action" data-testid="merchant-work-orders-filter-count">匹配 {filteredRows.length} / {baseRows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="merchant-work-orders-filter-empty">无匹配工单</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-work-orders-table">
            <thead><tr><th>{TEXT.select}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-title" onClick={() => sort.toggle("title")} {...sort.thProps("title")}>{TEXT.workOrderTitle}{sort.indicator("title")}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-type" onClick={() => sort.toggle("type")} {...sort.thProps("type")}>{TEXT.type}{sort.indicator("type")}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-priority" onClick={() => sort.toggle("priority")} {...sort.thProps("priority")}>{TEXT.priority}{sort.indicator("priority")}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-status" onClick={() => sort.toggle("status")} {...sort.thProps("status")}>{TEXT.status}{sort.indicator("status")}</th><th>{TEXT.ownerDepartment}</th><th>{TEXT.ownerUser}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-dueAt" onClick={() => sort.toggle("dueAt")} {...sort.thProps("dueAt")}>{TEXT.dueAt}{sort.indicator("dueAt")}</th><th>{TEXT.evidenceLabel}</th></tr></thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-work-order-row-${row.id}`}
                  onClick={() => openWorkOrderDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此工单 ${row.id}`} data-testid={`merchant-work-order-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.title ?? row.id}<br /><span onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}><small>商户 {row.merchantId}</small></TraceLink></span></td>
                  <td>{formatBusinessStatus(row.workOrderType)}</td>
                  <td>{formatBusinessStatus(row.priority)}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td>{row.ownerDepartment || "-"}</td>
                  <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.ownerUserId}>{row.ownerUserId || "-"}</TraceLink></td>
                  <td>{cnDate(row.dueAt)}</td>
                  <td>{evidenceList(row)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-work-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
