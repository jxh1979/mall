import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps, MerchantZoneSettlementRow } from "../types";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

const TEXT = {
  title: "我的对账",
  description: "查看本商户结算单。退租金额异议在退租审批中提出，本页不提交异议、不改变结算状态、不暂停付款。",
  loadFailed: "对账数据读取失败",
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
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染结算单表对应子集。
// URL 约定：?kpi=settlements[&subset=open]。TraceLink（合同下钻）在聚焦表内保留。
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

export function MerchantZoneSettlementsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneSettlementRow[]>([]);
  // 行明细抽屉：整行可点 -> 该结算单完整字段（合同 TraceLink·触发/状态中文·开账时间中文）。
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

  const openRows = rows.filter((row) => row.status !== "settled");
  const openCount = openRows.length;
  const focusRows = kpiFocus?.subset === "open" ? openRows : rows;
  const paged = usePagedRows(kpiFocus ? focusRows : rows, 10, kpiFocus ? kpiFocus.subset : "");

  function openSettlementDetail(row: MerchantZoneSettlementRow) {
    setDetail({
      title: `结算单明细 · ${row.id}`,
      fields: [
        { label: "结算单", value: row.id },
        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> },
        { label: "触发", value: TRIGGER_LABELS[row.triggerType ?? ""] ?? row.triggerType ?? "-" },
        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-" },
        { label: "异议理由", value: row.disputeReason ?? "-" },
        { label: "开账时间", value: cnDate(row.createdAt) },
      ],
    });
  }

  const settlementsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="merchant-zone-settlements-table">
        <thead>
          <tr><th>结算单</th><th>合同</th><th>触发</th><th>状态</th><th>异议理由</th><th>开账时间</th><th>详情</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-settlement-row-${row.id}`}
              onClick={() => openSettlementDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(row); } }}>
              <td>{row.id}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink></td>
              <td>{TRIGGER_LABELS[row.triggerType ?? ""] ?? row.triggerType ?? "-"}</td>
              <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
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
      <AuditTrailHint event="merchant_portal.settlement.viewed" />
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
            {settlementsTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
