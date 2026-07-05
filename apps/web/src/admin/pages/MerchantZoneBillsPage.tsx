import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps, MerchantZoneBillRow } from "../types";

const TEXT = {
  title: "我的账单",
  description: "本商户的租金 / 物业费 / 能源费 / 保证金账单与账龄，只读。费用缴纳仍走平台既有收款流程，本页仅展示与提醒。",
  loadFailed: "账单读取失败",
  focusClear: "清除筛选，返回全部",
};

const STATUS_LABELS: Record<string, string> = {
  issued: "已出账",
  viewed: "已查看",
  confirmed: "已确认",
  disputed: "有异议",
  overdue: "已逾期",
  closed: "已结清",
};

const FEE_TYPE_LABELS: Record<string, string> = {
  rent: "租金",
  property_fee: "物业费",
  energy_fee: "能源费",
  cam: "公摊",
  deposit: "保证金",
  other: "其他",
};

function money(cent: number | undefined, currency?: string) {
  if (cent === undefined || cent === null) return "-";
  const yuan = cent / 100;
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(yuan)} ${currency ?? "CNY"}`;
}

function lineSummary(row: MerchantZoneBillRow) {
  if (!row.lines || row.lines.length === 0) return row.sourceType ? (FEE_TYPE_LABELS[row.sourceType] ?? row.sourceType) : "-";
  return row.lines.map((line) => `${FEE_TYPE_LABELS[line.feeType ?? ""] ?? line.feeType ?? "-"} ${money(line.amountCent, line.currency)}`).join("；");
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染账单表对应子集。
// URL 约定：?kpi=bills[&subset=open|overdue]。TraceLink（合同下钻）在聚焦表内保留。
type KpiFocus = { subset: "all" | "open" | "overdue" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "bills") return null;
  const subset = params.get("subset");
  return { subset: subset === "open" || subset === "overdue" ? subset : "all" };
}

function focusTitle(subset: KpiFocus["subset"]): string {
  if (subset === "open") return "未结账单";
  if (subset === "overdue") return "逾期/账龄";
  return "账单";
}

export function MerchantZoneBillsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneBillRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: KpiFocus["subset"]) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "bills");
    if (subset !== "all") params.set("subset", subset);
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantZoneBillRow[]>("/api/v1/merchant-billing/bills"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const openBills = rows.filter((row) => row.status !== "closed");
  const overdueBills = rows.filter((row) => row.status === "overdue" || (row.agingDays ?? 0) > 0);
  const openAmountCent = openBills.reduce((sum, row) => sum + (row.totalAmountCent ?? 0), 0);
  const maxAging = rows.reduce((max, row) => Math.max(max, row.agingDays ?? 0), 0);
  const focusRows = kpiFocus?.subset === "open" ? openBills : kpiFocus?.subset === "overdue" ? overdueBills : rows;
  const paged = usePagedRows(kpiFocus ? focusRows : rows, 10, kpiFocus ? kpiFocus.subset : "");

  const billsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="merchant-zone-bills-table">
        <thead>
          <tr><th>账单号</th><th>账期</th><th>到期日</th><th>金额</th><th>状态</th><th>账龄(天)</th><th>费用构成</th><th>来源合同</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.id}>
              <td>{row.billNo ?? row.id}</td>
              <td>{row.billPeriodStart ?? "-"} ~ {row.billPeriodEnd ?? "-"}</td>
              <td>{row.dueDate ?? "-"}</td>
              <td>{money(row.totalAmountCent, row.currency)}</td>
              <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
              <td>{row.agingDays ?? 0}</td>
              <td>{lineSummary(row)}</td>
              <td className="admin-id-cell">{row.sourceType === "contract" && row.sourceId
                ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.sourceId}>{row.sourceId}</TraceLink>
                : "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <TablePager testid="merchant-zone-bills-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
    </div>
  );

  return (
    <section data-testid="merchant-zone-bills-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_billing.bill.viewed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="merchant-zone-bills-kpi-focus-banner">
              正在查看：{focusTitle(kpiFocus.subset)}（{focusRows.length}）
              <button type="button" data-testid="merchant-zone-bills-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {billsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-zone-bills-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>账单总数</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-bills-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>未结账单</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-bills-kpi-open" onClick={() => enterKpiFocus("open")}>{openBills.length}</button><small>金额 {money(openAmountCent)}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>逾期/账龄</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-bills-kpi-overdue" onClick={() => enterKpiFocus("overdue")}>{overdueBills.length}</button><small>最大账龄 {maxAging} 天</small></div>
            </div>
            {billsTable}
          </>
        )}
      </AdminDataState>
    </section>
  );
}
