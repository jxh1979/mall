import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import type { PagedResult } from "../client/adminClient";
import type { AdminPageProps, DividendPlanRow, DividendStatementDetail, DividendStatementRow } from "../types";
import { DIVIDEND_KIND_LABELS, DIVIDEND_LINE_LABELS, DIVIDEND_STATUS_LABELS } from "./FinanceDividendsPage";

// 商户专区 · 我的分红（主理人只读视图）：本商户每期分红计算单（基数/比例/扣税/实发/状态）+
// 算式明细逐项可查。后端按登录主体商户 scope 硬闸，只见本商户；月度预览单同样可见（只看不发）。
const TEXT = {
  title: "我的分红",
  description:
    "本商户的持股分红：每期净利润 = 收入 − 销货成本 − 费用，按方案比例劈分后，主理人份额代扣股息红利个税、净额发放。本页只读；对数字有异议请联系平台财务（驳回重算走平台侧）。",
  loadFailed: "分红数据读取失败",
  detailFailed: "计算单明细读取失败",
};

function centToYuan(cent: number | undefined | null): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

function bpsToPercentText(bps: number | undefined): string {
  return bps === undefined ? "—" : `${(bps / 100).toFixed(bps % 100 === 0 ? 0 : 2)}%`;
}

const PAGE_SIZE = 10;

export function MerchantZoneDividendsPage({ api, setGlobalError }: AdminPageProps) {
  const [plans, setPlans] = React.useState<DividendPlanRow[]>([]);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const plansResource = useAdminResource(
    async () => {
      // 后端按商户 scope 强制过滤，本商户通常至多一个生效中方案。
      setPlans(await api.get<DividendPlanRow[]>("/api/v1/finance/dividends/plans"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<DividendStatementRow>> =>
      api.getPaged<DividendStatementRow>(`/api/v1/finance/dividends/statements?page=${page}&page_size=${size}`),
    [api],
  );

  const rows = useServerPagedRows<DividendStatementRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: TEXT.loadFailed,
  });

  const activePlan = plans.find((plan) => plan.status === "active");

  async function openDetail(row: DividendStatementRow) {
    try {
      const data = await api.get<DividendStatementDetail>(
        `/api/v1/finance/dividends/statements/${encodeURIComponent(row.id)}`);
      const statement = data.statement ?? row;
      setDetail({
        title: `分红明细 · ${statement.periodLabel ?? statement.id}`,
        fields: [
          { label: "期间", value: `${statement.periodLabel ?? "—"}（${cnDate(statement.periodStart)} ~ ${cnDate(statement.periodEnd)}）` },
          { label: "类别 / 状态", value: `${DIVIDEND_KIND_LABELS[statement.kind ?? ""] ?? statement.kind} / ${DIVIDEND_STATUS_LABELS[statement.status ?? ""] ?? statement.status}` },
          { label: "分成比例（平台/主理人）", value: `${bpsToPercentText(statement.platformShareBps)} / ${bpsToPercentText(statement.platformShareBps === undefined ? undefined : 10000 - statement.platformShareBps)}` },
          { label: "代扣税率（股息红利）", value: bpsToPercentText(statement.taxRateBps) },
          ...(data.lines ?? []).map((line) => ({
            label: DIVIDEND_LINE_LABELS[line.lineType ?? ""] ?? line.lineType ?? "—",
            value: `¥${centToYuan(line.amountCent)}${line.detail ? `（${line.detail}）` : ""}`,
          })),
          { label: "算单时间", value: cnDate(statement.createdAt) },
          { label: "计算单号", value: statement.id },
        ],
      });
    } catch (detailError) {
      setGlobalError(detailError instanceof Error ? detailError.message : TEXT.detailFailed);
    }
  }

  return (
    <section data-testid="merchant-zone-dividends-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="dividend.statements_listed / dividend.statement_viewed" />

      {activePlan ? (
        <div className="admin-form-row" data-testid="merchant-dividend-plan-summary">
          <span>当前方案：平台 {bpsToPercentText(activePlan.platformShareBps)} / 主理人 {bpsToPercentText(activePlan.platformShareBps === undefined ? undefined : 10000 - activePlan.platformShareBps)}，代扣税率 {bpsToPercentText(activePlan.taxRateBps)}，{activePlan.periodType === "monthly" ? "月度出正式单" : "季度出正式单 + 月度预览"}。</span>
        </div>
      ) : null}

      <AdminDataState loading={rows.loading || plansResource.loading} error={rows.error ?? plansResource.error} empty={!rows.loading && rows.total === 0} onRetry={rows.reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-dividend-table">
            <thead><tr><th>期间</th><th>类别</th><th>净利润（元）</th><th>主理人应得（元）</th><th>代扣税（元）</th><th>净发放（元）</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {rows.rows.length === 0 ? (
                <tr data-testid="merchant-dividend-table-empty"><td colSpan={8} className="admin-muted-action">暂无分红计算单。</td></tr>
              ) : rows.rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-dividend-row-${row.id}`}
                  onClick={() => void openDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); void openDetail(row); } }}>
                  <td>{row.periodLabel ?? "—"}</td>
                  <td>{DIVIDEND_KIND_LABELS[row.kind ?? ""] ?? row.kind ?? "—"}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.netProfitCent)}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.principalGrossCent)}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.taxCent)}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.principalNetCent)}</td>
                  <td>{DIVIDEND_STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td className="admin-row-detail-cell" onClick={(event) => event.stopPropagation()}>
                    <button type="button" className="admin-row-detail-btn" aria-label="查看分红算式明细" onClick={(event) => { event.stopPropagation(); void openDetail(row); }}>查看算式</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-dividend-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
        </div>
      </AdminDataState>

      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
