import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, MerchantWorkOrderRow } from "../types";

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
};

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

export function MerchantWorkOrdersPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
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

  const { loading, error, reload } = useAdminResource(
    async () => {
      const workOrders = await api.get<MerchantWorkOrderRow[]>("/api/v1/merchant-service/work-orders");
      setRows(workOrders);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const pendingCount = rows.filter(nonTerminal).length;
  const highPriorityCount = rows.filter((row) => ["high", "critical"].includes(row.priority ?? "")).length;

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
    await runAction("merchant_work_order_created", request, () => api.post("/api/v1/merchant-service/work-orders", request));
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

  return (
    <section data-testid="merchant-work-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="merchant-work-orders-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="merchant_service.work_order.viewed / created / assigned / action_added / closed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="merchant-work-orders-summary">
          <div className="admin-kpi-card"><span>{TEXT.workOrderCount}</span><strong>{rows.length}</strong><small>{TEXT.merchant}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.pending}</span><strong>{pendingCount}</strong><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.highPriority}</span><strong>{highPriorityCount}</strong><small>{TEXT.priority}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected?.title ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="merchant-work-order-create-form">
          <label>{TEXT.workOrderTitle}<input data-testid="merchant-work-order-title" value={title} onChange={(event) => setTitle(event.target.value)} /></label>
          <label>{TEXT.descriptionLabel}<input data-testid="merchant-work-order-description" value={description} onChange={(event) => setDescription(event.target.value)} /></label>
          <label>{TEXT.type}<select data-testid="merchant-work-order-type" value={workOrderType} onChange={(event) => setWorkOrderType(event.target.value)}><option value="repair">repair</option><option value="complaint">complaint</option><option value="rectification">rectification</option><option value="service">service</option></select></label>
          <label>{TEXT.priority}<select data-testid="merchant-work-order-priority" value={priority} onChange={(event) => setPriority(event.target.value)}><option value="low">low</option><option value="medium">medium</option><option value="high">high</option><option value="critical">critical</option></select></label>
        </div>
        <div className="admin-form-row">
          <button type="button" data-testid="merchant-work-order-create" disabled={busyAction !== ""} onClick={() => void createWorkOrder()}>{TEXT.create}</button>
          <span className="admin-muted-action" data-testid="merchant-work-orders-boundary">{TEXT.boundary}</span>
        </div>

        <div className="admin-form-grid" data-testid="merchant-work-order-action-form">
          <label>{TEXT.ownerDepartment}<input data-testid="merchant-work-order-owner-department" value={ownerDepartment} onChange={(event) => setOwnerDepartment(event.target.value)} /></label>
          <label>{TEXT.ownerUser}<input data-testid="merchant-work-order-owner-user" value={ownerUserId} onChange={(event) => setOwnerUserId(event.target.value)} /></label>
          <label>{TEXT.processNote}<input data-testid="merchant-work-order-process-note" value={processNote} onChange={(event) => setProcessNote(event.target.value)} /></label>
          <label>{TEXT.targetStatus}<select data-testid="merchant-work-order-target-status" value={targetStatus} onChange={(event) => setTargetStatus(event.target.value)}><option value="assigned">assigned</option><option value="processing">processing</option><option value="pending_merchant">pending_merchant</option><option value="resolved">resolved</option></select></label>
          <label>{TEXT.resolution}<input data-testid="merchant-work-order-resolution" value={resolution} onChange={(event) => setResolution(event.target.value)} /></label>
          <label>{TEXT.satisfaction}<input data-testid="merchant-work-order-satisfaction" value={satisfactionScore} onChange={(event) => setSatisfactionScore(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="merchant-work-order-actions">
          <button type="button" data-testid="merchant-work-order-assign" disabled={busyAction !== ""} onClick={() => void assignWorkOrder()}>{TEXT.assign}</button>
          <button type="button" data-testid="merchant-work-order-process" disabled={busyAction !== ""} onClick={() => void processWorkOrder()}>{TEXT.process}</button>
          <button type="button" data-testid="merchant-work-order-close" disabled={busyAction !== ""} onClick={() => void closeWorkOrder()}>{TEXT.close}</button>
        </div>
        {formError ? <div data-testid="merchant-work-order-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-work-orders-table">
            <thead><tr><th>{TEXT.select}</th><th>{TEXT.workOrderTitle}</th><th>{TEXT.type}</th><th>{TEXT.priority}</th><th>{TEXT.status}</th><th>{TEXT.ownerDepartment}</th><th>{TEXT.ownerUser}</th><th>{TEXT.dueAt}</th><th>{TEXT.evidenceLabel}</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`merchant-work-order-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.title ?? row.id}</td>
                  <td>{row.workOrderType ?? "-"}</td>
                  <td>{row.priority ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                  <td>{row.ownerDepartment || "-"}</td>
                  <td>{row.ownerUserId || "-"}</td>
                  <td>{row.dueAt ?? "-"}</td>
                  <td>{evidenceList(row)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
