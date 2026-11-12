// 文件版本：V0.03
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { TablePager } from "../components/TablePager";
import type { AdminPageProps } from "../types";
import type { PagedResult } from "../client/adminClient";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

// 平台侧结算审阅(审计断层②):列出通票核销产生的结算流水与月度结算单,供平台操作员查看。
// 流水只读(确认/入账由月度 Job 负责);后端 /api/v1/settlement/transactions,权限 finance.settlement.view。
// 第 75 条(ENUM 第 52 行「超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”」「超出部分由娱乐平台公司承担」):
// 本页全只读,超池额单列,过账时由娱乐平台公司承担,不设结算期审批。
const PAGE_SIZE = 20;

export type SettlementRow = {
  id: string;
  transactionNo: string;
  projectId: string | null;
  merchantId: string | null;
  exclusiveMerchantId: string | null;
  ticketTypeId: string | null;
  entitlementType: string | null;
  beneficiaryType: string | null;
  verifiedCount: number;
  numPeople: number;
  verifiedAmountCent: number;
  settlementStatus: string | null;
  createdAt: string;
  // 第十八批 AI 段 236-R1（BSM 第 1754 行「每项权益都必须绑定实际提供方和审批版本」；ENUM 第 54 行「核销或结算时锁定当时版本」）：
  //   核销所用内部结算价目录行（审批版本）；不按内部结算价结算的为空。
  settlementCatalogEntryId: string | null;
};

// S4:月度结算单头(财务对账视角)。金额带符号——负数月 = 冲正多于新核销,当月净追回。
export type SettlementBillRow = {
  id: string;
  projectId: string | null;
  periodStart: string;
  periodEnd: string;
  ticketRevenueCent: number;
  // 第十八批第 3 条（BSM 第 749 行渠道费用分两层）：channelFeeCent＝销售渠道费（仅第三方渠道），routeFeeCent＝通路费（所有付款）。
  channelFeeCent: number;
  routeFeeCent: number;
  compServiceFeePooledCent: number;
  compServiceFeeExclusiveCent: number;
  feeBeneficiaryEntityId: string | null;
  feeBeneficiaryEntityName: string | null;
  availablePoolCent: number;
  distributedCent: number;
  comboDiscountCent: number;
  roundingResidualCent: number;
  distributionMethod: string | null;
  status: string | null;
  // 池分配合计 > 可分配池 = 超池;超出部分过账时由娱乐平台公司承担(第 75 条)。
  pooledDistributedCent: number;
  // 第十八批第 2 条（ENUM 第 52 行「三者即使同归一个主体，也必须分三栏分别列示」）：未分配池余额、通票未核销减除额（归属主体）与
  //   分账尾差分三栏；缺档位行暂挂的第二段金额另列。
  poolUndistributedCent: number;
  unverifiedDeductionCent: number;
  unverifiedDeductionEntityId: string | null;
  unverifiedDeductionEntityName: string | null;
  poolSuspendedCent: number;
  // S7 赠付差额:这个月预扣了多少、实际付出去多少、差额多少（一律留娱乐平台公司，18B 复核后裁决第 11 条）。
  giftCostCent: number;
  giftActualPaidCent: number;
  giftVarianceCent: number;
};

// 第十八批 AI 段 237-R3（BSM 第 1754 行「通票结算审阅财务侧设置独立审阅台账，按项目、经营域、责任主体和实际提供方列明第三方渠道费用、
//   域间份额、域内分配、内部结算价权益、酒吧入场可分配池和责任主体承担的差额」）：月度结算单审阅明细，后端 GET /api/v1/settlement/bills/{id}/review。
type DomainShareRow = {
  businessFormat: string; beneficiaryKind: string; beneficiaryEntityId: string | null; beneficiaryEntityName: string | null;
  grossRevenueCent: number; channelFeeCent: number; routeFeeCent: number; compServiceFeeCent: number; giftCostCent: number;
  giftActualPaidCent: number; netRevenueCent: number; interEntityNetCent: number; unverifiedNetCent: number;
  internalSettledCent: number; differenceCent: number | null;
};
type AllocationRow = {
  merchantId: string; merchantName: string | null; businessFormat: string; distributionMethod: string; verifiedCount: number;
  guestCount: number; rankNo: number | null; finalAmountCent: number; singleBarRouteFeeCent: number; singleBarChannelFeeCent: number;
  status: string;
};
type ProviderPriceRow = {
  merchantId: string; merchantName: string | null; businessFormat: string;
  giftDrinkPaidCent: number; paidDirectSettledCent: number; freeDirectSettledCent: number;
};
export type SettlementBillReview = {
  domainShares: DomainShareRow[]; allocations: AllocationRow[]; internalPriceByProvider: ProviderPriceRow[];
};

function centToYuan(cent: number | undefined | null): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

function yuanCell(cent: number | null, testid?: string) {
  return <td className="admin-num" style={{ textAlign: "right" }} data-testid={testid}>{centToYuan(cent)}</td>;
}

export function SettlementReviewPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [projectDraft, setProjectDraft] = React.useState("");
  const [merchantDraft, setMerchantDraft] = React.useState("");
  const [projectFilter, setProjectFilter] = React.useState("");
  const [merchantFilter, setMerchantFilter] = React.useState("");
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<SettlementRow>> => {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (projectFilter.trim()) params.set("projectId", projectFilter.trim());
      if (merchantFilter.trim()) params.set("merchantId", merchantFilter.trim());
      return api.getPaged<SettlementRow>(`/api/v1/settlement/transactions?${params.toString()}`);
    },
    [api, projectFilter, merchantFilter],
  );

  const rows = useServerPagedRows<SettlementRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: "结算流水读取失败",
  });

  // S4:月度结算单头(近 24 单)。财务对账三问:这单收了多少门票综合服务费、池链/专属各多少、归了谁。
  const [bills, setBills] = React.useState<SettlementBillRow[]>([]);
  const loadBills = React.useCallback(() => {
    api.get<SettlementBillRow[]>("/api/v1/settlement/bills")
      .then((data) => setBills(Array.isArray(data) ? data : []))
      .catch(() => setBills([])); // 结算单读取失败不拦流水审阅主功能
  }, [api]);
  React.useEffect(() => { loadBills(); }, [loadBills]);

  // 237-R3：点某张结算单的「审阅明细」，在结算单表下展开三表；后点的单覆盖先点的，迟到的旧响应不覆盖。
  const [review, setReview] = React.useState<{ bill: SettlementBillRow; data: SettlementBillReview | null; error: string | null } | null>(null);
  function openReview(bill: SettlementBillRow) {
    setReview({ bill, data: null, error: null });
    api.get<SettlementBillReview>(`/api/v1/settlement/bills/${encodeURIComponent(bill.id)}/review`)
      .then((data) => setReview((cur) => (cur?.bill.id === bill.id ? { bill, data, error: null } : cur)))
      .catch(() => setReview((cur) => (cur?.bill.id === bill.id ? { bill, data: null, error: "审阅明细读取失败" } : cur)));
  }

  function isOverPool(bill: SettlementBillRow): boolean {
    return bill.pooledDistributedCent > bill.availablePoolCent;
  }
  function overPoolCent(bill: SettlementBillRow): number {
    return bill.pooledDistributedCent - bill.availablePoolCent;
  }

  function submitSearch() {
    setProjectFilter(projectDraft);
    setMerchantFilter(merchantDraft);
  }

  // 过滤条件变化后回第 1 页重取。
  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectFilter, merchantFilter]);

  function openDetail(row: SettlementRow) {
    setDetail({
      title: `结算流水明细 · ${row.transactionNo}`,
      fields: [
        { label: "流水号", value: row.transactionNo },
        { label: "项目", value: row.projectId ?? "—" },
        { label: "商户", value: row.merchantId ?? "—" },
        { label: "专属商户", value: row.exclusiveMerchantId ?? "—" },
        { label: "权益类型", value: formatBusinessStatus(row.entitlementType ?? "—") },
        { label: "受益方", value: formatBusinessStatus(row.beneficiaryType ?? "—") },
        { label: "核销张数", value: String(row.verifiedCount) },
        { label: "人数", value: String(row.numPeople) },
        { label: "核销金额", value: `¥${centToYuan(row.verifiedAmountCent)}` },
        { label: "内部结算价版本", value: row.settlementCatalogEntryId ?? "—" },
        { label: "结算状态", value: formatBusinessStatus(row.settlementStatus ?? "—") },
        { label: "发生时间", value: cnDate(row.createdAt) },
      ],
    });
  }

  // 第四批接线（指标登记簿方案A）：结算笔数/核销金额/核销张数改走统一明细端点（平台口径全量）。
  // 有项目/商户筛选时=非平台口径 → enabled 关，卡回退本地（笔数=分页 total、金额/张数=不适用）并隐藏「完整明细」入口。
  const noFilter = !projectFilter.trim() && !merchantFilter.trim();
  const countKpi = useMetricKpiValue(api, "settlement.transaction_count", { enabled: noFilter });
  const amountKpi = useMetricKpiValue(api, "settlement.verified_amount", { enabled: noFilter });
  const ticketKpi = useMetricKpiValue(api, "settlement.verified_count", { enabled: noFilter });
  const countDisplay = countKpi.status === "ready" && countKpi.value !== null ? countKpi.value : rows.total;

  const empty = !rows.loading && rows.total === 0 && !projectFilter && !merchantFilter;

  return (
    <section data-testid="settlement-review-page">
      <AdminPageHeader title="通票结算审阅" description="查看通票核销结算流水与月度结算单；月度结算单的审阅明细按经营域、责任主体和实际提供方列明第三方渠道费用、域间份额、域内分配与内部结算价权益。流水只读，确认与入账由月度结算负责；第一段合计超过份池的部分由娱乐平台公司承担，结算不设超池待审批。" />
      <AdminDataState loading={rows.loading} error={rows.error} empty={empty} onRetry={rows.reload}>
        <div className="admin-kpi-grid" data-testid="settlement-review-summary">
          <div className="admin-kpi-card"><span>结算笔数</span><strong className="admin-kpi-value--text" data-testid="settlement-review-kpi-count">{countDisplay}</strong><small>{noFilter ? "平台全量" : "当前筛选"}{noFilter && countKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="settlement.transaction_count" testid="settlement-review-metric-detail-count" /> : null}</small></div>
          <div className="admin-kpi-card"><span>核销金额合计（元）</span><strong className="admin-kpi-value--text" data-testid="settlement-review-kpi-amount">{noFilter && amountKpi.status === "ready" && amountKpi.value !== null ? centToYuan(amountKpi.value) : "—"}</strong><small>{noFilter && amountKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="settlement.verified_amount" testid="settlement-review-metric-detail-amount" /> : "筛选态平台合计不适用"}</small></div>
          <div className="admin-kpi-card"><span>核销张数合计</span><strong className="admin-kpi-value--text" data-testid="settlement-review-kpi-tickets">{noFilter && ticketKpi.status === "ready" && ticketKpi.value !== null ? ticketKpi.value : "—"}</strong><small>{noFilter && ticketKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="settlement.verified_count" testid="settlement-review-metric-detail-tickets" /> : "筛选态平台合计不适用"}</small></div>
        </div>
        <div className="admin-list-toolbar" data-testid="settlement-review-filter">
          <input
            data-testid="settlement-review-project"
            value={projectDraft}
            onChange={(event) => setProjectDraft(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") submitSearch(); }}
            placeholder="项目（回车查询，可空）"
          />
          <input
            data-testid="settlement-review-merchant"
            value={merchantDraft}
            onChange={(event) => setMerchantDraft(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") submitSearch(); }}
            placeholder="商户（回车查询，可空）"
          />
          <button type="button" data-testid="settlement-review-search-submit" onClick={submitSearch}>查询</button>
          <span className="admin-muted-action" data-testid="settlement-review-count">共 {rows.total} 条</span>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-review-table">
            <thead><tr><th>流水号</th><th>项目</th><th>商户</th><th>核销张数</th><th>核销金额（元）</th><th>结算状态</th><th>详情</th></tr></thead>
            <tbody>{rows.rows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`settlement-review-row-${row.id}`}
                onClick={() => openDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDetail(row); } }}>
                <td>{row.transactionNo}</td>
                <td>{row.projectId ?? "—"}</td>
                <td>{row.merchantId ?? row.exclusiveMerchantId ?? "—"}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{row.verifiedCount}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.verifiedAmountCent)}</td>
                <td>{formatBusinessStatus(row.settlementStatus ?? "—")}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看结算流水详情" onClick={(event) => { event.stopPropagation(); openDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
          <TablePager testid="settlement-review-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
        </div>
        {bills.length > 0 ? (
          <div data-testid="settlement-review-bills">
            <h3>月度结算单（近 {bills.length} 单）</h3>
            <p className="admin-muted-action">
              门票综合服务费＝娱乐平台公司留存／项目管理费，同一项只扣一次，分「池链」「专属」两支：池链支从可分配池前扣、专属支从专属商户直付前扣。可分配池＝酒吧份额扣完通路费、销售渠道费与池链服务费后的剩余，仅用于各酒吧分配，池内不再二次扣留存。金额为负＝该月冲正多于新核销，属向对方追回。
              第一段合计超过份池的部分由娱乐平台公司承担（过账时单列为娱乐平台公司承担的补贴），结算不设超池待审批——超池在第一段配置生效前就已试算、确需超池须事先经短期经营激励审批。
              赠付差额＝赠付预扣（分账时按票种规则预扣的赠送权益预估成本）减去赠付实付（客人核销后娱乐平台公司按内部结算价付给提供方商户的钱），核销一件付一件、没核销就不付；差额留在娱乐平台公司，逐期在差额台账留痕。
            </p>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="settlement-review-bills-table">
                <thead><tr><th>结算月</th><th>项目</th><th>通票收入（元）</th><th>通路费（元）</th><th>销售渠道费（元）</th><th>综合服务费·池链（元）</th><th>综合服务费·专属（元）</th><th>服务费归属</th><th>赠付预扣（元）</th><th>赠付实付（元）</th><th>赠付差额（元）</th><th>可分配池（元）</th><th>已分配（元）</th><th>分账尾差（元）</th><th>未分配池余额（元）</th><th>通票未核销减除额（归属主体）</th><th>暂挂（元）</th><th>状态</th><th>超池（元，娱乐平台公司承担）</th><th>审阅明细</th></tr></thead>
                <tbody>{bills.map((bill) => (
                  <tr key={bill.id} data-testid={`settlement-review-bill-${bill.id}`}>
                    <td>{bill.periodStart?.slice(0, 7) ?? "—"}</td>
                    <td>{bill.projectId ?? "平台默认"}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(bill.ticketRevenueCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-route-fee-${bill.id}`}>{centToYuan(bill.routeFeeCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-sales-fee-${bill.id}`}>{centToYuan(bill.channelFeeCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(bill.compServiceFeePooledCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(bill.compServiceFeeExclusiveCent)}</td>
                    <td>{bill.feeBeneficiaryEntityName ?? bill.feeBeneficiaryEntityId ?? "归属未配置"}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-gift-cost-${bill.id}`}>{centToYuan(bill.giftCostCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-gift-paid-${bill.id}`}>{centToYuan(bill.giftActualPaidCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-gift-variance-${bill.id}`}>{centToYuan(bill.giftVarianceCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(bill.availablePoolCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(bill.distributedCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(bill.roundingResidualCent)}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-undistributed-${bill.id}`}>{centToYuan(bill.poolUndistributedCent)}</td>
                    <td data-testid={`settlement-bill-unverified-deduction-${bill.id}`}>
                      {centToYuan(bill.unverifiedDeductionCent)}（{bill.unverifiedDeductionEntityName ?? bill.unverifiedDeductionEntityId ?? "归属未配置"}）
                    </td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-suspended-${bill.id}`}>{centToYuan(bill.poolSuspendedCent)}</td>
                    <td>{formatBusinessStatus(bill.status ?? "—")}</td>
                    <td className="admin-num" style={{ textAlign: "right" }} data-testid={`settlement-bill-overpool-amount-${bill.id}`}>{isOverPool(bill) ? centToYuan(overPoolCent(bill)) : "—"}</td>
                    <td><button type="button" className="admin-row-detail-btn" data-testid={`settlement-bill-review-${bill.id}`} onClick={() => openReview(bill)}>审阅明细</button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            {review ? (
              <div data-testid="settlement-review-detail">
                <h4>审阅明细 · {review.bill.periodStart?.slice(0, 7) ?? "—"} · {review.bill.projectId ?? "平台默认"}</h4>
                {review.error ? <p className="admin-muted-action">{review.error}</p> : review.data === null ? <p className="admin-muted-action">读取中…</p> : (
                  <>
                    <h5>经营域份额</h5>
                    <p className="admin-muted-action">责任主体承担的差额＝业态净份额减内部结算价直付，为负由责任主体承担，为正归其所得；到期未核销归属＝该域份额中从未核销、到期的部分。</p>
                    {review.data.domainShares.length === 0 ? <p className="admin-muted-action">本单没有经营域份额</p> : (
                      <div className="admin-table-scroll">
                        <table className="admin-table" data-testid="settlement-review-domain-shares">
                          <thead><tr><th>经营域</th><th>责任主体</th><th>毛额（元）</th><th>第三方渠道费用（元）</th><th>通路费（元）</th><th>门票综合服务费（元）</th><th>赠送权益预估成本（元）</th><th>赠付实付（元）</th><th>业态净份额（元）</th><th>域间份额（元）</th><th>到期未核销归属（元）</th><th>内部结算价直付（元）</th><th>责任主体承担的差额（元）</th></tr></thead>
                          <tbody>{review.data.domainShares.map((s) => (
                            <tr key={s.businessFormat}>
                              <td data-testid={`settlement-share-format-${s.businessFormat}`}>{formatBusinessStatus(s.businessFormat)}</td>
                              <td>{s.beneficiaryEntityName ?? (s.beneficiaryKind === "per_merchant" ? "各提供商户" : "归属未配置")}</td>
                              {yuanCell(s.grossRevenueCent)}
                              {yuanCell(s.channelFeeCent)}
                              {yuanCell(s.routeFeeCent)}
                              {yuanCell(s.compServiceFeeCent)}
                              {yuanCell(s.giftCostCent)}
                              {yuanCell(s.giftActualPaidCent)}
                              {yuanCell(s.netRevenueCent)}
                              {yuanCell(s.interEntityNetCent, `settlement-share-inter-entity-${s.businessFormat}`)}
                              {yuanCell(s.unverifiedNetCent, `settlement-share-unverified-${s.businessFormat}`)}
                              {yuanCell(s.internalSettledCent)}
                              {yuanCell(s.differenceCent, `settlement-share-difference-${s.businessFormat}`)}
                            </tr>
                          ))}</tbody>
                        </table>
                      </div>
                    )}
                    <h5>域内分配</h5>
                    {review.data.allocations.length === 0 ? <p className="admin-muted-action">本单没有域内分配明细</p> : (
                      <div className="admin-table-scroll">
                        <table className="admin-table" data-testid="settlement-review-allocations">
                          <thead><tr><th>实际提供方</th><th>业态</th><th>分配方式</th><th>核销次数</th><th>人数</th><th>名次</th><th>金额（元）</th><th>单店门票销售渠道费（元）</th><th>单店门票通路费（元）</th><th>状态</th></tr></thead>
                          <tbody>{review.data.allocations.map((a) => (
                            <tr key={`${a.merchantId}-${a.distributionMethod}`}>
                              <td>{a.merchantName ?? a.merchantId}</td>
                              <td>{formatBusinessStatus(a.businessFormat)}</td>
                              <td>{formatBusinessStatus(a.distributionMethod)}</td>
                              <td className="admin-num" style={{ textAlign: "right" }}>{a.verifiedCount}</td>
                              <td className="admin-num" style={{ textAlign: "right" }}>{a.guestCount}</td>
                              <td className="admin-num" style={{ textAlign: "right" }}>{a.rankNo ?? "—"}</td>
                              {yuanCell(a.finalAmountCent)}
                              {yuanCell(a.singleBarChannelFeeCent, `settlement-allocation-channel-fee-${a.merchantId}-${a.distributionMethod}`)}
                              {yuanCell(a.singleBarRouteFeeCent, `settlement-allocation-route-fee-${a.merchantId}-${a.distributionMethod}`)}
                              <td>{formatBusinessStatus(a.status)}</td>
                            </tr>
                          ))}</tbody>
                        </table>
                      </div>
                    )}
                    <h5>内部结算价权益（按实际提供方）</h5>
                    {review.data.internalPriceByProvider.length === 0 ? <p className="admin-muted-action">本单没有内部结算价权益</p> : (
                      <div className="admin-table-scroll">
                        <table className="admin-table" data-testid="settlement-review-internal-price">
                          <thead><tr><th>实际提供方</th><th>业态</th><th>赠饮内部结算价应付（元）</th><th>有价权益按内部结算价直付（元）</th><th>零元平台出资权益直付（元，娱乐平台公司承担）</th></tr></thead>
                          <tbody>{review.data.internalPriceByProvider.map((p) => (
                            <tr key={p.merchantId}>
                              <td>{p.merchantName ?? p.merchantId}</td>
                              <td>{formatBusinessStatus(p.businessFormat)}</td>
                              {yuanCell(p.giftDrinkPaidCent, `settlement-provider-gift-${p.merchantId}`)}
                              {yuanCell(p.paidDirectSettledCent, `settlement-provider-paid-${p.merchantId}`)}
                              {yuanCell(p.freeDirectSettledCent, `settlement-provider-free-${p.merchantId}`)}
                            </tr>
                          ))}</tbody>
                        </table>
                      </div>
                    )}
                  </>
                )}
              </div>
            ) : null}
          </div>
        ) : null}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
