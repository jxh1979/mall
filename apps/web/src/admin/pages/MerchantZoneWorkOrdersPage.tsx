import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, MerchantWorkOrderRow } from "../types";

const WO_CREATE = ["merchant.service_work_order.create"];

const TEXT = {
  title: "我的工单",
  description: "提交本商户的报修 / 投诉 / 整改 / 服务工单并跟踪进展;派单与处理由平台侧闭环,本页只见本商户工单。",
  loadFailed: "工单读取失败",
  actionFailed: "工单提交失败",
  titleRequired: "提交工单必须填写标题。",
  descriptionRequired: "提交工单必须填写问题描述。",
  submit: "提交工单",
};

const TYPE_LABELS: Record<string, string> = {
  repair: "报修",
  complaint: "投诉",
  rectification: "整改",
  service: "服务",
};

const PRIORITY_LABELS: Record<string, string> = {
  low: "低",
  medium: "中",
  high: "高",
  critical: "紧急",
};

const STATUS_LABELS: Record<string, string> = {
  created: "已提交",
  assigned: "已派单",
  processing: "处理中",
  pending_merchant: "待商户确认",
  resolved: "已解决",
  closed: "已关闭",
  cancelled: "已取消",
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function nonTerminal(row: MerchantWorkOrderRow) {
  return !["closed", "cancelled"].includes(row.status ?? "");
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function MerchantZoneWorkOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantWorkOrderRow[]>([]);
  const [title, setTitle] = React.useState("");
  const [description, setDescription] = React.useState("");
  const [workOrderType, setWorkOrderType] = React.useState("repair");
  const [priority, setPriority] = React.useState("medium");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantWorkOrderRow[]>("/api/v1/merchant-service/work-orders"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function submitWorkOrder() {
    if (!title.trim()) {
      setFormError(TEXT.titleRequired);
      return;
    }
    if (!description.trim()) {
      setFormError(TEXT.descriptionRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-zone-work-order"),
      operatorNote: "商户专区自助提交工单",
      workOrderType,
      priority,
      title,
      description,
      reportedChannel: "merchant_portal",
    };
    await runAction("merchant_zone_work_order_submitted", request, () => api.post("/api/v1/merchant-service/work-orders", request));
    setTitle("");
    setDescription("");
  }

  const openCount = rows.filter(nonTerminal).length;
  const paged = usePagedRows(rows, 10);

  return (
    <section data-testid="merchant-zone-work-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_service.work_order.viewed / merchant_service.work_order.created" />
      <div className="admin-form-row" data-testid="merchant-zone-work-order-form">
        <label>类型<select data-testid="merchant-zone-work-order-type" value={workOrderType} onChange={(event) => setWorkOrderType(event.target.value)}>
          {Object.entries(TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>优先级<select data-testid="merchant-zone-work-order-priority" value={priority} onChange={(event) => setPriority(event.target.value)}>
          {Object.entries(PRIORITY_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>标题<input data-testid="merchant-zone-work-order-title" value={title} onChange={(event) => setTitle(event.target.value)} placeholder="如:空调漏水" /></label>
        <label>问题描述<input data-testid="merchant-zone-work-order-description" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="现场情况与期望处理" /></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={WO_CREATE}
          data-testid="merchant-zone-work-order-submit"
          disabled={busyAction === "merchant_zone_work_order_submitted"}
          onClick={submitWorkOrder}
        >{TEXT.submit}</PermissionButton>
      </div>
      {formError ? <div className="admin-form-error" data-testid="merchant-zone-work-order-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="merchant-zone-work-orders-summary">
          <div className="admin-kpi-card"><span>工单总数</span><strong>{rows.length}</strong><small>本商户</small></div>
          <div className="admin-kpi-card"><span>进行中</span><strong>{openCount}</strong><small>未关闭</small></div>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-zone-work-orders-table">
            <thead>
              <tr><th>标题</th><th>类型</th><th>优先级</th><th>状态</th><th>截止时间</th><th>处理结论</th><th>提交时间</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id}>
                  <td>{row.title ?? row.id}</td>
                  <td>{TYPE_LABELS[row.workOrderType ?? ""] ?? row.workOrderType ?? "-"}</td>
                  <td>{PRIORITY_LABELS[row.priority ?? ""] ?? row.priority ?? "-"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
                  <td>{row.dueAt ?? "-"}</td>
                  <td>{row.resolution ?? "-"}</td>
                  <td>{row.createdAt ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-zone-work-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
