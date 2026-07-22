import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { CnDatePicker } from "../components/CnDatePicker";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";

type Amount = { amountCent?: number | null; count?: number | null };
type Row = { bucket: string; label: string; pass: Amount; ticket: Amount; rowTotal: Amount };
type AbandonedResponse = {
  start?: string;
  end?: string;
  rows?: Row[];
  passTotal?: Amount;
  ticketTotal?: Amount;
  grandTotal?: Amount;
  note?: string;
};

const TEXT = {
  title: "放弃权益额外收入",
  description:
    "客人买了票（通票 / 单独门票）但已过期未核销的权益，其未核销部分归项目公司的额外收入，按票种 × 权益类型交叉呈现。" +
    "数据源为每日结算作业写好的「未核销到期归项目」流水，金额为权威结算口径，本页只读不重算。",
  loadFailed: "放弃权益额外收入读取失败",
};

const pad = (n: number) => String(n).padStart(2, "0");
const firstOfMonth = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01`;

function money(cent?: number | null): string {
  if (cent === null || cent === undefined || !Number.isFinite(cent)) return "—";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
}

export function AbandonedEntitlementRevenuePage({ api, setGlobalError }: AdminPageProps) {
  const now = new Date();
  const [start, setStart] = React.useState<string>(firstOfMonth(now));
  const [end, setEnd] = React.useState<string>(firstOfMonth(new Date(now.getFullYear(), now.getMonth() + 1, 1)));
  const [report, setReport] = React.useState<AbandonedResponse | null>(null);
  const [rangeError, setRangeError] = React.useState<string | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 区间校验（start 含 / end 不含，start 必须早于 end）在前端先拦，避免无谓请求。
      if (!start || !end || start >= end) {
        setRangeError("起始日期必须早于结束日期（起始含、结束不含）");
        setReport(null);
        return;
      }
      setRangeError(null);
      const data = await api.get<AbandonedResponse>(
        `/api/v1/analytics/abandoned-entitlement-revenue?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`,
      );
      setReport(data);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const rows = report?.rows ?? [];

  return (
    <section data-testid="abandoned-entitlement-revenue-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="analytics.abandoned_entitlement_revenue.viewed" />
      <div className="admin-form-row" style={{ display: "flex", gap: 12, alignItems: "flex-end", flexWrap: "wrap" }}>
        <label>起始日期(含)
          <CnDatePicker value={start} onChange={setStart} data-testid="abandoned-start" aria-label="起始日期" today={firstOfMonth(now)} />
        </label>
        <label>结束日期(不含)
          <CnDatePicker value={end} onChange={setEnd} data-testid="abandoned-end" aria-label="结束日期" today={firstOfMonth(now)} />
        </label>
        <button type="button" className="admin-btn" data-testid="abandoned-query" onClick={() => void reload()}>查询</button>
      </div>
      {rangeError ? <div className="admin-state admin-error" data-testid="abandoned-range-error">{rangeError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="abandoned-window">
          统计区间:{report?.start ?? start} 至 {report?.end ?? end}(结束日不含)
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="abandoned-cross-table">
            <thead>
              <tr>
                <th rowSpan={2}>权益类型</th>
                <th colSpan={2}>通票</th>
                <th colSpan={2}>单独门票</th>
                <th colSpan={2}>行合计</th>
              </tr>
              <tr>
                <th>金额</th><th>笔数</th>
                <th>金额</th><th>笔数</th>
                <th>金额</th><th>笔数</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.bucket} data-testid={`abandoned-row-${row.bucket}`}>
                  <td>{row.label}</td>
                  <td data-testid={`abandoned-${row.bucket}-pass-amount`}>{money(row.pass?.amountCent)}</td>
                  <td>{row.pass?.count ?? 0}</td>
                  <td data-testid={`abandoned-${row.bucket}-ticket-amount`}>{money(row.ticket?.amountCent)}</td>
                  <td>{row.ticket?.count ?? 0}</td>
                  <td>{money(row.rowTotal?.amountCent)}</td>
                  <td>{row.rowTotal?.count ?? 0}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr data-testid="abandoned-col-total">
                <td>列合计</td>
                <td data-testid="abandoned-pass-total-amount">{money(report?.passTotal?.amountCent)}</td>
                <td>{report?.passTotal?.count ?? 0}</td>
                <td data-testid="abandoned-ticket-total-amount">{money(report?.ticketTotal?.amountCent)}</td>
                <td>{report?.ticketTotal?.count ?? 0}</td>
                <td data-testid="abandoned-grand-total-amount">{money(report?.grandTotal?.amountCent)}</td>
                <td>{report?.grandTotal?.count ?? 0}</td>
              </tr>
            </tfoot>
          </table>
        </div>
        {report?.note ? <p className="admin-muted-action" data-testid="abandoned-note">{report.note}</p> : null}
      </AdminDataState>
    </section>
  );
}
