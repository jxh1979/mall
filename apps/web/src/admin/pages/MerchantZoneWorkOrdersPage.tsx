import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
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
  focusClear: "清除筛选，返回全部",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<all|open>；点 KPI 数字=带参重进本页只渲染工单表（对应子集）。
// 本页单表：工单总数=全量、进行中=未终结子集。
type KpiTarget = "all" | "open";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "all" && table !== "open") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  return focus.table === "open" ? "进行中工单" : "全部工单";
}

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
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该工单完整字段（类型/优先级/状态中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openWorkOrderDetail(row: MerchantWorkOrderRow) {
    setDetail({
      title: `工单明细 · ${row.title ?? row.id}`,
      fields: [
        { label: "标题", value: row.title ?? "—" },
        { label: "类型", value: TYPE_LABELS[row.workOrderType ?? ""] ?? row.workOrderType ?? "—" },
        { label: "优先级", value: PRIORITY_LABELS[row.priority ?? ""] ?? row.priority ?? "—" },
        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—" },
        { label: "问题描述", value: row.description ?? "—" },
        { label: "截止时间", value: cnDate(row.dueAt) },
        { label: "处理结论", value: row.resolution ?? "—" },
        { label: "提交时间", value: cnDate(row.createdAt) },
        { label: "工单编号", value: row.id },
      ],
    });
  }

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
  // 聚焦时按 KPI 子集收窄再分页；非聚焦=全量。
  const baseRows = kpiFocus?.table === "open" ? rows.filter(nonTerminal) : rows;
  const paged = usePagedRows(baseRows, 10, kpiFocus?.table ?? "none");

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
        {kpiFocus ? (
        <div className="admin-state" data-testid="merchant-zone-work-orders-kpi-focus-banner">
          正在查看：{kpiFocusTitle(kpiFocus)}（{baseRows.length}）
          <button type="button" data-testid="merchant-zone-work-orders-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
        </div>
        ) : (
        <div className="admin-kpi-grid" data-testid="merchant-zone-work-orders-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>工单总数</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-work-orders-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>进行中</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-work-orders-kpi-open" onClick={() => enterKpiFocus("open")}>{openCount}</button><small>未关闭</small></div>
        </div>
        )}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-zone-work-orders-table">
            <thead>
              <tr><th>标题</th><th>类型</th><th>优先级</th><th>状态</th><th>截止时间</th><th>处理结论</th><th>提交时间</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-work-order-row-${row.id}`}
                  onClick={() => openWorkOrderDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}>
                  <td>{row.title ?? row.id}</td>
                  <td>{TYPE_LABELS[row.workOrderType ?? ""] ?? row.workOrderType ?? "-"}</td>
                  <td>{PRIORITY_LABELS[row.priority ?? ""] ?? row.priority ?? "-"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
                  <td>{cnDate(row.dueAt)}</td>
                  <td>{row.resolution ?? "-"}</td>
                  <td>{cnDate(row.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-zone-work-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
