import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import type { AdminPageProps, MerchantFeeStatement, MerchantZoneBillRow } from "../types";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

const TEXT = {
  title: "我的账单",
  description: "本商户的费用总表、当期综合账单与各张账单账龄，只读。费用缴纳仍走平台既有收款流程，本页仅展示与提醒。",
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
  deposit: "押金",
  other: "其他",
  manual: "手工登记",
  contract: "合同",
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

// 18E 第 21 条（BSM:1323「商户专区须设一张费用总表，列明平台向本商户收取的全部费用名目、计费基数、承担方与本期金额……系统须向商户出具一张综合账单，
// 把当期各名目的费用汇总列示」）：放在 AdminDataState 外面，没有账单时也显示；月份不选时由后端取当月（计费自然月）。
function FeeStatementSection({ api, focusObjectId }: Pick<AdminPageProps, "api" | "focusObjectId">) {
  // 第 92 条：从首页未读账单提醒带账期进来时直接打开该月（后端随即记该月账单已读）。
  const [month, setMonth] = React.useState(() => (focusObjectId && /^\d{4}-\d{2}$/.test(focusObjectId) ? focusObjectId : ""));
  const [data, setData] = React.useState<MerchantFeeStatement | null>(null);
  const [error, setError] = React.useState("");
  React.useEffect(() => {
    setError("");
    api.get<MerchantFeeStatement>(`/api/v1/merchant-portal/fee-statement${month ? `?month=${encodeURIComponent(month)}` : ""}`)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "费用总表读取失败"));
  }, [api, month]);
  const bill = data?.bill;
  return (
    <div data-testid="merchant-zone-fee-statement">
      <label>账期（计费自然月）{" "}
        <input type="month" data-testid="merchant-zone-fee-month" value={month || data?.month || ""} onChange={(event) => setMonth(event.target.value)} />
      </label>
      {error ? <div className="admin-form-error">{error}</div> : null}
      <h3>费用总表</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="merchant-zone-fee-items">
          <thead><tr><th>费用名目</th><th>计费基数</th><th>承担方</th><th>本期金额</th></tr></thead>
          <tbody>{(data?.feeItems ?? []).map((row) => (
            <tr key={row.feeName}>
              <td>{row.feeName}</td>
              <td>{row.billingBasis ?? "-"}</td>
              <td>{row.bearer ?? "-"}</td>
              <td>{row.amountCent == null ? "本期未出账" : money(row.amountCent)}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <h3>当期综合账单</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="merchant-zone-fee-bill">
          <thead><tr><th>费用名目</th><th>金额</th><th>已付</th><th>未付</th></tr></thead>
          <tbody>
            {(bill?.lines ?? []).map((line) => (
              <tr key={line.feeName}><td>{line.feeName}</td><td>{money(line.amountCent)}</td><td>{money(line.paidAmountCent)}</td><td>{money(line.unpaidAmountCent)}</td></tr>
            ))}
            <tr><td>合计</td><td>{money(bill?.totalAmountCent ?? 0)}</td><td>{money(bill?.paidAmountCent ?? 0)}</td><td>{money(bill?.unpaidAmountCent ?? 0)}</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
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

export function MerchantZoneBillsPage({ api, setGlobalError, navigateTo, focusObjectId }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneBillRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 -> 该账单完整字段（账期/到期中文·金额¥·费用构成·来源合同 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

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

  function openBillDetail(row: MerchantZoneBillRow) {
    setDetail({
      title: `账单明细 · ${row.billNo ?? row.id}`,
      fields: [
        { label: "账单号", value: row.billNo ?? row.id },
        { label: "账期", value: `${cnDate(row.billPeriodStart)} ~ ${cnDate(row.billPeriodEnd)}` },
        { label: "到期日", value: cnDate(row.dueDate) },
        { label: "金额", value: money(row.totalAmountCent, row.currency) },
        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-" },
        { label: "账龄(天)", value: row.agingDays ?? 0 },
        { label: "费用构成", value: lineSummary(row) },
        { label: "来源合同", value: row.sourceType === "contract" && row.sourceId
            ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.sourceId}>{row.sourceId}</TraceLink>
            : "-" },
        { label: "账单号", value: row.id },
      ],
    });
  }

  const billsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="merchant-zone-bills-table">
        <thead>
          <tr><th>账单号</th><th>账期</th><th>到期日</th><th>金额</th><th>状态</th><th>账龄(天)</th><th>费用构成</th><th>来源合同</th><th>详情</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-bill-row-${row.id}`}
              onClick={() => openBillDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBillDetail(row); } }}>
              <td>{row.billNo ?? row.id}</td>
              <td>{cnDate(row.billPeriodStart)} ~ {cnDate(row.billPeriodEnd)}</td>
              <td>{cnDate(row.dueDate)}</td>
              <td>{money(row.totalAmountCent, row.currency)}</td>
              <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "-"}</td>
              <td>{row.agingDays ?? 0}</td>
              <td>{lineSummary(row)}</td>
              <td className="admin-id-cell" onClick={(event) => event.stopPropagation()}>{row.sourceType === "contract" && row.sourceId
                ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.sourceId}>{row.sourceId}</TraceLink>
                : "-"}</td>
            <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看账单详情" onClick={(event) => { event.stopPropagation(); openBillDetail(row); }}>查看详情</button></td></tr>
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
      <FeeStatementSection api={api} focusObjectId={focusObjectId} />
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
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
