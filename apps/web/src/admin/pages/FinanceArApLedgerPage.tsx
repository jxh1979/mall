import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import type { AdminPageProps, FinanceLedgerRow } from "../types";
import type { PagedResult } from "../client/adminClient";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// 后端写权限码（与 FinanceController.requirePermission 对齐）
const PAYMENT_PERMISSION = "finance.payment.register";
const PAGE_SIZE = 10;

// 排序指示符修复：共享 useSortedRows 的「↕」在当前 UI 字体下渲染成冒号状（表头看起来是「凭证:」）。
// ponytail: 本页改用 ▲/▼ 且仅在已排序列显示（键盘可排序与 aria-sort 由 thProps 保证）；

// 凭证号：后端 ar-ap-items 返回 item_no（camel 后 itemNo）；billNo/sourceDoc 仅作旧数据兜底。
function voucherNo(row: FinanceLedgerRow): string {
  return row.itemNo ?? row.billNo ?? row.sourceDoc ?? row.id;
}

// 金额列：分转元两位小数（等宽数字由 tabular-nums 保证），缺失不伪造。
function centToYuan(cent: number | undefined): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

export function FinanceArApLedgerPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [amount, setAmount] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [exporting, setExporting] = React.useState(false);
  // 搜索输入草稿（即时搜索降级：输入不触发请求，回车/点查询才 search() 提交跳回第 1 页）。
  const [draft, setDraft] = React.useState("");
  // 行明细抽屉：整行可点 → 该单据完整字段（方向/金额/来源/状态/日期，枚举中文·金额¥·日期中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const fetchPage = React.useCallback(
    (page: number, size: number, keyword: string): Promise<PagedResult<FinanceLedgerRow>> => {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (keyword.trim()) params.set("keyword", keyword.trim());
      return api.getPaged<FinanceLedgerRow>(`/api/v1/finance/ar-ap-items?${params.toString()}`);
    },
    [api],
  );

  const ledger = useServerPagedRows<FinanceLedgerRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: "应收应付台账读取失败",
  });
  const { setSelectedId, selected } = useSelectedRow(ledger.rows);

  // ponytail: 排序仅作用于当前服务端页的 rows（后端 ar-ap-items 未预埋排序参数）。
  // 升级路径：后端加 sort/order 参数后，把 sort.key/direction 传入 fetchPage 即可改为全局排序。
  const sort = useSortedRows(ledger.rows, {
    voucher: (row) => voucherNo(row),
    direction: (row) => row.direction,
    dueDate: (row) => row.dueDate,
    status: (row) => row.status,
  });

  function submitSearch() {
    ledger.search(draft);
  }

  function validatePayment() {
    if (!selected?.id) {
      setFormError("请先在台账中选择要核销/调账的凭证。");
      return false;
    }
    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setFormError("核销金额必须是大于 0 的数字。");
      return false;
    }
    if (!reason.trim()) {
      setFormError("核销或调整必须填写原因。");
      return false;
    }
    setFormError("");
    return true;
  }

  async function submitPayment() {
    if (!selected?.id) {
      setFormError("请先在台账中选择要核销/调账的凭证。");
      return;
    }
    const numericAmount = Number(amount);
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/finance/ar-ap-items/${selected.id}/payments`, {
        amount: Math.round(numericAmount * 100),
        reason,
      });
      setActionResult({ action: "ledger_payment", status: "partial", ledgerItemId: selected.id, result });
      ledger.reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "核销/调账登记失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "ledger_payment", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }


  async function exportLedger() {
    setExporting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.get<Record<string, unknown>>("/api/v1/finance/ar-ap-items/export");
      setActionResult({ action: "ledger_export", status: "exported", result });
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "应收应付导出失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "ledger_export", status: "failed", message });
    } finally {
      setExporting(false);
    }
  }

  function openLedgerDetail(row: FinanceLedgerRow) {
    setDetail({
      title: `台账单据明细 · ${voucherNo(row)}`,
      fields: [
        { label: "凭证", value: voucherNo(row) },
        { label: "方向", value: formatBusinessStatus(row.direction ?? "receivable") },
        { label: "往来单位", value: row.counterparty ?? "—" },
        { label: "来源单据", value: row.sourceDoc ?? "—" },
        { label: "总额", value: `¥${centToYuan(row.amountCent)}` },
        { label: "已收/已付", value: `¥${centToYuan(row.paidAmountCent)}` },
        { label: "未收/未付", value: `¥${centToYuan(row.unpaidAmountCent)}` },
        { label: "到期日", value: cnDate(row.dueDate) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "单据号", value: row.id },
      ],
    });
  }

  const empty = !ledger.loading && ledger.total === 0 && ledger.keyword === "";

  return (
    <section data-testid="finance-ledger-page">
      <AdminPageHeader title="应收应付台账" description="P0 完整台账支持应收、应付、部分核销、余额和调整原因留痕。" />
      <AdminDataState loading={ledger.loading} error={ledger.error} empty={empty} onRetry={ledger.reload}>
        <div className="admin-form-row">
          <label>核销金额（元）<input data-testid="finance-payment-amount" type="number" min={0} step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="如 100.00" /></label>
          <label>核销/调整原因<input data-testid="finance-payment-reason" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="必填，留痕用" /></label>
          <HighRiskConfirm
            label="登记核销"
            impact="将对选中凭证写入部分核销/调账并落库留痕，影响应收应付余额和后续对账。"
            disabled={submitting}
            precheck={validatePayment}
            onConfirm={() => void submitPayment()}
            permissionsState={effectivePermissions}
            permissions={[PAYMENT_PERMISSION]}
            data-testid="finance-payment-submit"
          />
          <button type="button" data-testid="finance-export" disabled={exporting} onClick={() => void exportLedger()}>{exporting ? "导出中" : "导出台账"}</button>
        </div>
        {formError ? <div data-testid="finance-ledger-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-list-toolbar" data-testid="finance-ledger-filter">
          <input
            data-testid="finance-ledger-search"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") submitSearch(); }}
            aria-label="搜索凭证 / 方向 / 状态（回车查询）"
            placeholder="搜索凭证 / 方向 / 状态（回车查询）"
          />
          <button type="button" data-testid="finance-ledger-search-submit" onClick={submitSearch}>查询</button>
          <span className="admin-muted-action" data-testid="finance-ledger-filter-count">{ledger.keyword ? `"${ledger.keyword}" 命中 ${ledger.total} 条` : `共 ${ledger.total} 条`}</span>
        </div>
        {ledger.total === 0 && ledger.keyword !== "" ? (
          <div className="admin-state admin-empty" data-testid="finance-ledger-filter-empty">无匹配凭证</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="finance-ledger-table">
            <thead><tr><th>选择</th><th className="admin-sort-th" data-testid="finance-ledger-sort-voucher" onClick={() => sort.toggle("voucher")} {...sort.thProps("voucher")}>凭证{sort.indicator("voucher")}</th><th className="admin-sort-th" data-testid="finance-ledger-sort-direction" onClick={() => sort.toggle("direction")} {...sort.thProps("direction")}>方向{sort.indicator("direction")}</th><th>金额（未收 / 总额，元）</th><th className="admin-sort-th" data-testid="finance-ledger-sort-due-date" onClick={() => sort.toggle("dueDate")} {...sort.thProps("dueDate")}>到期日{sort.indicator("dueDate")}</th><th className="admin-sort-th" data-testid="finance-ledger-sort-status" onClick={() => sort.toggle("status")} {...sort.thProps("status")}>状态{sort.indicator("status")}</th><th>详情</th></tr></thead>
            <tbody>{sort.sorted.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`finance-ledger-row-${row.id}`}
                onClick={() => openLedgerDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openLedgerDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择凭证 ${voucherNo(row)}`} data-testid={`finance-ledger-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                <td>{voucherNo(row)}</td>
                <td>{formatBusinessStatus(row.direction ?? "receivable")}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.unpaidAmountCent)} / {centToYuan(row.amountCent)}</td>
                <td>{row.dueDate ?? "—"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看台账单据详情" onClick={(event) => { event.stopPropagation(); openLedgerDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
          <TablePager testid="finance-ledger-pager" page={ledger.page} pageCount={ledger.pageCount} total={ledger.total} onPrev={ledger.prev} onNext={ledger.next} />
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
