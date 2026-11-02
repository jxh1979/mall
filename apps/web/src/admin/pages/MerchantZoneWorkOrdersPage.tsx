import React from "react";
import { isMerchantAppMode } from "../client/adminClient";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { FormModal } from "../components/FormModal";
import type { AdminPageProps, MerchantWorkOrderRow } from "../types";

const WO_CREATE = ["merchant.service_work_order.create"];

const TEXT = {
  title: "我的工单",
  description: "提交本商户的报修 / 保洁 / 投诉 / 其他服务请求工单并跟踪进展;派单与处理由平台侧闭环,本页只见本商户工单。投诉（以及平台配成须商户确认的报修、其他服务请求）到待商户确认或已处理待关闭时，点「确认已解决」即关闭，可同时打满意度。还没派单（已提交）的单可以填原因取消；已派单的照常处理。",
  loadFailed: "工单读取失败",
  actionFailed: "工单提交失败",
  titleRequired: "提交工单必须填写标题。",
  descriptionRequired: "提交工单必须填写问题描述。",
  submit: "提交工单",
  confirm: "确认已解决",
  cancel: "取消",
  cancelReasonRequired: "取消工单必须填写原因。",
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

// 第 93 条 93-R1：商户服务工单类型＝BSM:1323「报修/保洁/投诉/其他服务请求」（裁决 D-12 不含整改、含保洁）；后台工单页与商户授权工作台共用这一张。
export const MERCHANT_WORK_ORDER_TYPE_LABELS: Record<string, string> = {
  repair: "报修",
  cleaning: "保洁",
  complaint: "投诉",
  service: "其他服务请求",
};

const PRIORITY_LABELS: Record<string, string> = {
  low: "低",
  medium: "中",
  high: "高",
  critical: "紧急",
};

// 第 102 条 102-R2：ENUM:264 七态（第五态 resolved＝已处理待关闭）；后台工单页与商户授权工作台共用这一张，不走共享字典的「商户待处理」「已解决」。
export const MERCHANT_WORK_ORDER_STATUS_LABELS: Record<string, string> = {
  created: "已提交",
  assigned: "已派单",
  processing: "处理中",
  pending_merchant: "待商户确认",
  resolved: "已处理待关闭",
  closed: "已关闭",
  cancelled: "已取消",
};

// 第 102 条 102-R3（裁决 26）：报修、其他服务请求的关单方式；投诉照 BSM:1385 须商户确认。
type CloseMethodRow = { workOrderType: string; closeMethod: string };

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
  // 引导式流程 phase-8：新建工单收起为主按钮 + 分步引导弹窗（派单/处理等由平台侧闭环，不改）。
  const [createOpen, setCreateOpen] = React.useState(false);
  // 第 102 条 102-R2：商户确认已解决（可选满意度 1～5，BSM:1385「满意度在回访/验收时由提单方评分」）。
  const [closeMethods, setCloseMethods] = React.useState<Record<string, string>>({});
  const [confirmTarget, setConfirmTarget] = React.useState<MerchantWorkOrderRow | null>(null);
  const [confirmScore, setConfirmScore] = React.useState("");
  // 第 111 条 111-R2（第十八批裁决 27「派单前商户自己取消」）：已提交的单填原因取消。
  const [cancelTarget, setCancelTarget] = React.useState<MerchantWorkOrderRow | null>(null);
  const [cancelReason, setCancelReason] = React.useState("");

  function openWorkOrderDetail(row: MerchantWorkOrderRow) {
    setDetail({
      title: `工单明细 · ${row.title ?? row.id}`,
      fields: [
        { label: "标题", value: row.title ?? "—" },
        { label: "类型", value: MERCHANT_WORK_ORDER_TYPE_LABELS[row.workOrderType ?? ""] ?? row.workOrderType ?? "—" },
        { label: "优先级", value: PRIORITY_LABELS[row.priority ?? ""] ?? row.priority ?? "—" },
        { label: "状态", value: MERCHANT_WORK_ORDER_STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—" },
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
      const methods = await api.get<CloseMethodRow[]>("/api/v1/merchant-service/work-order-close-methods");
      setCloseMethods(Object.fromEntries(methods.map((method) => [method.workOrderType, method.closeMethod])));
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
      reportedChannel: isMerchantAppMode() ? "merchant_app" : "merchant_portal",
    };
    const ok = await runAction("merchant_zone_work_order_submitted", request, () => api.post("/api/v1/merchant-service/work-orders", request));
    // 成功才关弹窗并清空标题/描述；失败留弹窗内显错（formError 已由 runAction 置）。
    if (ok) {
      setTitle("");
      setDescription("");
      setCreateOpen(false);
    }
  }

  // 102-R2／R3：关单方式为「须商户确认」的单（投诉；配成须商户确认的报修、其他服务请求）在待商户确认、已处理待关闭时由商户确认关闭。
  function confirmable(row: MerchantWorkOrderRow) {
    return ["pending_merchant", "resolved"].includes(row.status ?? "")
      && (row.workOrderType === "complaint" || closeMethods[row.workOrderType ?? ""] === "merchant_confirm");
  }

  async function confirmWorkOrder() {
    if (!confirmTarget) return;
    const request = {
      idempotencyKey: nextKey("merchant-zone-work-order-confirm"),
      satisfactionScore: confirmScore ? Number(confirmScore) : null,
    };
    const ok = await runAction("merchant_zone_work_order_confirmed", request,
      () => api.post(`/api/v1/merchant-service/work-orders/${confirmTarget.id}/merchant-confirm`, request));
    if (ok) setConfirmTarget(null);
  }

  async function cancelWorkOrder() {
    if (!cancelTarget) return;
    const request = { idempotencyKey: nextKey("merchant-zone-work-order-cancel"), reason: cancelReason.trim() };
    const ok = await runAction("merchant_zone_work_order_cancelled", request,
      () => api.post(`/api/v1/merchant-service/work-orders/${cancelTarget.id}/cancel`, request));
    if (ok) setCancelTarget(null);
  }

  const openCount = rows.filter(nonTerminal).length;
  // 聚焦时按 KPI 子集收窄再分页；非聚焦=全量。
  const baseRows = kpiFocus?.table === "open" ? rows.filter(nonTerminal) : rows;
  const paged = usePagedRows(baseRows, 10, kpiFocus?.table ?? "none");

  return (
    <section data-testid="merchant-zone-work-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_service.work_order.viewed / merchant_service.work_order.created / merchant_service.work_order.merchant_confirmed / merchant_service.work_order.cancelled" />
      <div className="admin-form-row" data-testid="merchant-zone-work-order-form">
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={WO_CREATE}
          data-testid="merchant-zone-work-order-create-open"
          onClick={() => { setFormError(""); setCreateOpen(true); }}
        >＋ 新建工单</PermissionButton>
      </div>
      {formError ? <div className="admin-form-error" data-testid="merchant-zone-work-order-page-error">{formError}</div> : null}
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
              <tr><th>标题</th><th>类型</th><th>优先级</th><th>状态</th><th>截止时间</th><th>处理结论</th><th>提交时间</th><th>详情</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-work-order-row-${row.id}`}
                  onClick={() => openWorkOrderDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}>
                  <td>{row.title ?? row.id}</td>
                  <td>{MERCHANT_WORK_ORDER_TYPE_LABELS[row.workOrderType ?? ""] ?? row.workOrderType ?? "-"}</td>
                  <td>{PRIORITY_LABELS[row.priority ?? ""] ?? row.priority ?? "-"}</td>
                  <td>{MERCHANT_WORK_ORDER_STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
                  <td>{cnDate(row.dueAt)}</td>
                  <td>{row.resolution ?? "-"}</td>
                  <td>{cnDate(row.createdAt)}</td>
                <td className="admin-row-detail-cell" onClick={(event) => event.stopPropagation()}>
                  {confirmable(row) ? (
                    <PermissionButton permissionsState={effectivePermissions} permissions={WO_CREATE} data-testid={`merchant-zone-work-order-confirm-${row.id}`}
                      disabled={busyAction !== ""} onClick={() => { setFormError(""); setConfirmScore(""); setConfirmTarget(row); }}>{TEXT.confirm}</PermissionButton>
                  ) : null}
                  {row.status === "created" ? (
                    <PermissionButton permissionsState={effectivePermissions} permissions={WO_CREATE} data-testid={`merchant-zone-work-order-cancel-${row.id}`}
                      disabled={busyAction !== ""} onClick={() => { setFormError(""); setCancelReason(""); setCancelTarget(row); }}>{TEXT.cancel}</PermissionButton>
                  ) : null}
                  <button type="button" className="admin-row-detail-btn" aria-label="查看工单详情" onClick={() => openWorkOrderDetail(row)}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-zone-work-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <FormModal
        open={confirmTarget !== null}
        title="确认工单已解决"
        description={`确认「${confirmTarget?.title ?? confirmTarget?.id ?? ""}」已解决后工单即关闭；满意度可不填。`}
        testId="merchant-zone-work-order-confirm-form"
        submitTestId="merchant-zone-work-order-confirm-submit"
        submitLabel={TEXT.confirm}
        submitting={busyAction === "merchant_zone_work_order_confirmed"}
        error={formError || undefined}
        onClose={() => setConfirmTarget(null)}
        onSubmit={() => void confirmWorkOrder()}
      >
        <label>满意度<select data-testid="merchant-zone-work-order-confirm-score" value={confirmScore} onChange={(event) => setConfirmScore(event.target.value)}>
          <option value="">不评分</option>
          {["5", "4", "3", "2", "1"].map((score) => <option key={score} value={score}>{score} 分</option>)}
        </select></label>
      </FormModal>
      <FormModal
        open={cancelTarget !== null}
        title="取消工单"
        description={`取消「${cancelTarget?.title ?? cancelTarget?.id ?? ""}」后工单即为已取消，不再派单处理。`}
        testId="merchant-zone-work-order-cancel-form"
        submitTestId="merchant-zone-work-order-cancel-submit"
        submitLabel="确认取消"
        submitting={busyAction === "merchant_zone_work_order_cancelled"}
        error={formError || undefined}
        validate={() => (cancelReason.trim() ? null : TEXT.cancelReasonRequired)}
        onClose={() => setCancelTarget(null)}
        onSubmit={() => void cancelWorkOrder()}
      >
        <label>取消原因<input data-testid="merchant-zone-work-order-cancel-reason" value={cancelReason} placeholder="如：提错了、重复提单、问题已自行解决" onChange={(event) => setCancelReason(event.target.value)} /></label>
      </FormModal>
      <GuidedFlowModal
        open={createOpen}
        title="新建工单"
        testId="merchant-zone-work-order-guided"
        submitTestId="merchant-zone-work-order-submit"
        submitLabel={TEXT.submit}
        submitting={busyAction === "merchant_zone_work_order_submitted"}
        error={formError || undefined}
        onClose={() => setCreateOpen(false)}
        onSubmit={submitWorkOrder}
        steps={[
          {
            title: "工单信息",
            description: "选择工单类型与优先级，填写标题与问题描述（标题、描述必填）。",
            validate: () => {
              if (!title.trim()) return TEXT.titleRequired;
              if (!description.trim()) return TEXT.descriptionRequired;
              return null;
            },
            content: (
              <>
                <label>类型<select data-testid="merchant-zone-work-order-type" value={workOrderType} onChange={(event) => setWorkOrderType(event.target.value)}>
                  {Object.entries(MERCHANT_WORK_ORDER_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>优先级<select data-testid="merchant-zone-work-order-priority" value={priority} onChange={(event) => setPriority(event.target.value)}>
                  {Object.entries(PRIORITY_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>标题<input data-testid="merchant-zone-work-order-title" value={title} onChange={(event) => setTitle(event.target.value)} placeholder="如:空调漏水" /></label>
                <label>问题描述<input data-testid="merchant-zone-work-order-description" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="现场情况与期望处理" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "类型", value: MERCHANT_WORK_ORDER_TYPE_LABELS[workOrderType] ?? workOrderType },
          { label: "优先级", value: PRIORITY_LABELS[priority] ?? priority },
          { label: "标题", value: title.trim() || "未填写" },
          { label: "问题描述", value: description.trim() || "未填写" },
        ]}
      />
    </section>
  );
}
