// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps, MerchantZoneSettlementRow } from "../types";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

const DISPUTE_PERMS = ["merchant.portal.settlement.dispute"];
// 与后端 dispute_reason 列宽（500）一致；后端另有合并后长度校验。
const REASON_MAX = 500;

const TEXT = {
  title: "我的对账",
  description: "查看本商户的退租 / 到期结算单与结算明细；有疑问可留言提异议。留言不改变结算状态、不暂停付款；退租金额异议的正式处理在退租审批流程中进行。",
  disputeHint: "留言不改变结算状态、不暂停付款；退租金额异议的正式处理在退租审批流程中进行。",
  loadFailed: "对账数据读取失败",
  actionFailed: "留言提交失败",
  reasonRequired: "提交留言必须填写理由。",
  reasonTooLong: "留言不能超过 500 字。",
  noSelection: "请先选择要留言的结算单。",
  notDisputable: "该结算单当前状态不可留言。",
  submit: "提交留言",
  focusClear: "清除筛选，返回全部",
};

const TRIGGER_LABELS: Record<string, string> = {
  move_out: "退租",
  expiry: "到期",
  early_termination: "提前终止",
};

const STATUS_LABELS: Record<string, string> = {
  pending_settlement: "待结算",
  deposit_offsetting: "押金抵扣中",
  arrears_collecting: "欠费追缴中",
  settled: "已结清",
  disputed: "有争议",
  disputed_litigation: "有争议，诉讼/仲裁",
  voided: "已作废",
};

// M023-006：结算明细条目（后端 TerminationSettlementMath 的 item 键）中文名。
const ITEM_LABELS: Record<string, string> = {
  unpaid_receivable_summary: "未清应收合计",
  deposit_disposal: "押金处置",
  termination_penalty: "提前退租违约金",
};

// 未知码一律中文占位，不把英文原码显示给用户（M023-003）。
const UNKNOWN_STATUS = "未知状态";
const UNKNOWN_TRIGGER = "未知触发";
const UNKNOWN_ITEM = "其他条目";

// 可留言的驻留态（与后端 MerchantPortalService.DISPUTABLE_STATUSES 一致）；已结清/已作废不计入「未结清」（M023-004）。
const DISPUTABLE = new Set(["pending_settlement", "deposit_offsetting", "arrears_collecting"]);
const CLOSED = new Set(["settled", "voided"]);

type SettlementItem = { item?: string; amountCent?: number | null; note?: string | null };

function statusLabel(code: string | null | undefined): string {
  if (!code) return "-";
  return STATUS_LABELS[code] ?? UNKNOWN_STATUS;
}

function triggerLabel(code: string | null | undefined): string {
  if (!code) return "-";
  return TRIGGER_LABELS[code] ?? UNKNOWN_TRIGGER;
}

function money(cent: number | null | undefined) {
  if (cent === undefined || cent === null || !Number.isFinite(cent)) return "-";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染结算单表对应子集。
// URL 约定：?kpi=settlements[&subset=open]。
type KpiFocus = { subset: "all" | "open" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "settlements") return null;
  return { subset: params.get("subset") === "open" ? "open" : "all" };
}

function focusTitle(subset: KpiFocus["subset"]): string {
  if (subset === "open") return "未结清";
  return "结算单";
}

export function MerchantZoneSettlementsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneSettlementRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [reason, setReason] = React.useState("");
  // 行明细抽屉：整行可点 -> 该结算单完整字段（结算明细数字·触发/状态中文·开账时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: KpiFocus["subset"]) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "settlements");
    if (subset !== "all") params.set("subset", subset);
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantZoneSettlementRow[]>("/api/v1/merchant-portal/settlements"));
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

  // M023-006：留言走既有 POST /merchant-portal/settlements/{id}/dispute（只记理由，不改状态、不暂停付款）。
  async function submitDispute() {
    if (!selected) {
      setFormError(TEXT.noSelection);
      return;
    }
    if (!DISPUTABLE.has(selected.status ?? "")) {
      setFormError(TEXT.notDisputable);
      return;
    }
    const trimmed = reason.trim();
    if (!trimmed) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    if (trimmed.length > REASON_MAX) {
      setFormError(TEXT.reasonTooLong);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-zone-settlement-dispute"), reason: trimmed };
    const ok = await runAction("merchant_zone_settlement_disputed", request,
      () => api.post(`/api/v1/merchant-portal/settlements/${selected.id}/dispute`, request));
    if (ok) setReason("");
  }

  const openRows = rows.filter((row) => !CLOSED.has(row.status ?? ""));
  const openCount = openRows.length;
  const focusRows = kpiFocus?.subset === "open" ? openRows : rows;
  const paged = usePagedRows(kpiFocus ? focusRows : rows, 10, kpiFocus ? kpiFocus.subset : "");

  function openSettlementDetail(row: MerchantZoneSettlementRow) {
    const items = (row.settlementItems ?? []) as SettlementItem[];
    setDetail({
      title: `结算单明细 · ${row.id}`,
      fields: [
        { label: "结算单", value: row.id },
        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-MER-006" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> },
        { label: "触发", value: triggerLabel(row.triggerType) },
        { label: "状态", value: statusLabel(row.status) },
        {
          label: "结算明细",
          value: items.length > 0
            ? <ul data-testid="merchant-zone-settlement-items">{items.map((item, index) => (
              <li key={`${item.item ?? "item"}-${index}`}>{ITEM_LABELS[item.item ?? ""] ?? UNKNOWN_ITEM}：{money(item.amountCent)}{item.note ? `（${item.note}）` : ""}</li>
            ))}</ul>
            : "-",
        },
        { label: "留言理由", value: row.disputeReason ?? "-" },
        { label: "开账时间", value: cnDate(row.createdAt) },
      ],
    });
  }

  const settlementsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="merchant-zone-settlements-table">
        <thead>
          <tr><th>选择</th><th>结算单</th><th>合同</th><th>触发</th><th>状态</th><th>留言理由</th><th>开账时间</th><th>详情</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.id} className={`admin-row-clickable${row.id === selectedId ? " admin-row-selected" : ""}`} tabIndex={0} data-testid={`merchant-zone-settlement-row-${row.id}`}
              onClick={() => openSettlementDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" name="mz-settlement" aria-label={`选择此结算单 ${row.id}`} data-testid={`merchant-zone-settlement-pick-${row.id}`} checked={row.id === selectedId} onChange={() => setSelectedId(row.id)} /></td>
              <td>{row.id}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-MER-006" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink></td>
              <td>{triggerLabel(row.triggerType)}</td>
              <td>{statusLabel(row.status)}</td>
              <td>{row.disputeReason ?? "-"}</td>
              <td>{cnDate(row.createdAt)}</td>
            <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看结算单详情" onClick={(event) => { event.stopPropagation(); openSettlementDetail(row); }}>查看详情</button></td></tr>
          ))}
        </tbody>
      </table>
      <TablePager testid="merchant-zone-settlements-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
    </div>
  );

  return (
    <section data-testid="merchant-zone-settlements-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_portal.settlement.viewed / merchant_portal.settlement.disputed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="merchant-zone-settlements-kpi-focus-banner">
              正在查看：{focusTitle(kpiFocus.subset)}（{focusRows.length}）
              <button type="button" data-testid="merchant-zone-settlements-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {settlementsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-zone-settlements-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>结算单</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-settlements-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>未结清</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-settlements-kpi-open" onClick={() => enterKpiFocus("open")}>{openCount}</button><small>待结算</small></div>
            </div>
            <div className="admin-form-row" data-testid="merchant-zone-settlement-dispute-form">
              <label>留言理由<input data-testid="merchant-zone-settlement-dispute-reason" maxLength={REASON_MAX} value={reason} onChange={(event) => setReason(event.target.value)} placeholder="如：违约金多算一期" /></label>
              <PermissionButton
                permissionsState={effectivePermissions}
                permissions={DISPUTE_PERMS}
                data-testid="merchant-zone-settlement-dispute-submit"
                disabled={busyAction === "merchant_zone_settlement_disputed" || !selected || !DISPUTABLE.has(selected.status ?? "")}
                onClick={() => { void submitDispute(); }}
              >{TEXT.submit}</PermissionButton>
              <span className="admin-muted-action" data-testid="merchant-zone-settlement-dispute-hint">{TEXT.disputeHint}</span>
            </div>
            {formError ? <div className="admin-form-error" data-testid="merchant-zone-settlement-form-error">{formError}</div> : null}
            {settlementsTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
