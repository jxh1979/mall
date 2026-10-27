// 文件版本：V0.01
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
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useCan } from "../security/Can";

// 平台侧结算审阅(审计断层②):列出通票核销产生的结算流水与月度结算单,供平台操作员查看。
// 流水只读(确认/入账由月度 Job 负责);后端 /api/v1/settlement/transactions,权限 finance.settlement.view。
// 唯一写动作:超池结算单特批(POST /api/v1/settlement/bills/{id}/special-approval,
// 权限 settlement.bill.special_approve 仅授平台负责人;无权限者看得到超池状态但没有按钮)。
const PAGE_SIZE = 20;

/** 超池特批专属权限点(V357·高敏,仅授 role_platform_admin 平台负责人)。 */
const SPECIAL_APPROVE_PERMISSION = "settlement.bill.special_approve";

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
  // 超池特批入口(S9 遗留补齐):池分配合计 > 可分配池 = 超池;draft 超池单待平台负责人特批。
  specialApprovalRef: string | null;
  specialApprovalValidUntil: string | null;
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

function centToYuan(cent: number | undefined | null): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

/** 本地日(非 UTC):特批有效期是"到期日含当日",按操作员本地日历判定是否已到期。 */
function localToday(): string {
  return new Intl.DateTimeFormat("en-CA").format(new Date());
}

export function SettlementReviewPage({ api, setGlobalError, setActionResult, navigateTo, effectivePermissions }: AdminPageProps) {
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

  // 超池特批(S9 遗留补齐):仅超池 draft 单可批;无权限者看得到状态但没有按钮(整块入口按权限隐藏)。
  const canSpecialApprove = useCan(effectivePermissions, [SPECIAL_APPROVE_PERMISSION]);
  const [approvingBill, setApprovingBill] = React.useState<SettlementBillRow | null>(null);
  const [approveRef, setApproveRef] = React.useState("");
  const [approveError, setApproveError] = React.useState("");
  const [approveBusy, setApproveBusy] = React.useState(false);

  function isOverPool(bill: SettlementBillRow): boolean {
    return bill.pooledDistributedCent > bill.availablePoolCent;
  }
  function isOverPoolDraft(bill: SettlementBillRow): boolean {
    return bill.status === "draft" && isOverPool(bill);
  }
  function overPoolCent(bill: SettlementBillRow): number {
    return bill.pooledDistributedCent - bill.availablePoolCent;
  }

  function openApproval(bill: SettlementBillRow) {
    setApprovingBill(bill);
    setApproveRef("");
    setApproveError("");
  }

  async function submitSpecialApproval() {
    if (!approvingBill) return;
    setApproveBusy(true);
    setApproveError("");
    try {
      const result = await api.post<{ specialApprovalRef: string; validUntil: string }>(
        `/api/v1/settlement/bills/${approvingBill.id}/special-approval`,
        { specialApprovalRef: approveRef.trim() },
      );
      setActionResult(
        `超池特批已生效:结算月 ${approvingBill.periodStart?.slice(0, 7) ?? "—"} 结算单转「已确认」,特批引用 ${result.specialApprovalRef},有效期至 ${result.validUntil}(到期自动失效,须重新特批)。`,
      );
      setApprovingBill(null);
      setApproveRef("");
      loadBills();
    } catch (error) {
      setApproveError(error instanceof Error && error.message ? error.message : "超池特批失败,请稍后重试");
    } finally {
      setApproveBusy(false);
    }
  }

  /** 特批列:业务人员一眼看到该单是否超池待批、批过的有效到哪天、过期要重批。 */
  function specialApprovalCell(bill: SettlementBillRow): React.ReactNode {
    const expired = bill.specialApprovalValidUntil !== null && bill.specialApprovalValidUntil < localToday();
    if (isOverPoolDraft(bill)) {
      return (
        <span data-testid={`settlement-bill-overpool-${bill.id}`}>
          {bill.specialApprovalRef ? `特批已到期(原引用 ${bill.specialApprovalRef}),须重新特批` : "超池待特批"}
          {canSpecialApprove ? (
            <button
              type="button"
              className="admin-secondary-button"
              data-testid={`settlement-bill-approve-${bill.id}`}
              disabled={approveBusy}
              onClick={() => openApproval(bill)}
            >超池特批</button>
          ) : null}
        </span>
      );
    }
    if (bill.specialApprovalRef) {
      return `已特批 ${bill.specialApprovalRef}·${expired ? `已于 ${bill.specialApprovalValidUntil} 到期` : `有效至 ${bill.specialApprovalValidUntil ?? "—"}`}`;
    }
    return "—";
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
      <AdminPageHeader title="通票结算审阅" description="查看通票核销结算流水与月度结算单。流水只读，确认与入账由月度结算负责；超池停在「草稿」的结算单由平台负责人在此特批（短期激励，到期自动失效）。" />
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
              各酒吧分得总额不得超过可分配池：超池的结算单停在「草稿」，仅限平台负责人特批的短期激励活动放行（有效期按后台配置，默认不超过 3 个月，到期自动失效须重新特批）。
              赠付差额＝赠付预扣（分账时按票种规则预扣的赠送权益预估成本）减去赠付实付（客人核销后娱乐平台公司按内部结算价付给提供方商户的钱），核销一件付一件、没核销就不付；差额留在娱乐平台公司，逐期在差额台账留痕。
            </p>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="settlement-review-bills-table">
                <thead><tr><th>结算月</th><th>项目</th><th>通票收入（元）</th><th>通路费（元）</th><th>销售渠道费（元）</th><th>综合服务费·池链（元）</th><th>综合服务费·专属（元）</th><th>服务费归属</th><th>赠付预扣（元）</th><th>赠付实付（元）</th><th>赠付差额（元）</th><th>可分配池（元）</th><th>已分配（元）</th><th>分账尾差（元）</th><th>未分配池余额（元）</th><th>通票未核销减除额（归属主体）</th><th>暂挂（元）</th><th>状态</th><th>超池（元）</th><th>超池特批</th></tr></thead>
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
                    <td>{specialApprovalCell(bill)}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            {approvingBill ? (
              <div data-testid="settlement-special-approval-form">
                <h4>超池特批 · {approvingBill.periodStart?.slice(0, 7) ?? "—"} · {approvingBill.projectId ?? "平台默认"}</h4>
                <p className="admin-muted-action">
                  可分配池 {centToYuan(approvingBill.availablePoolCent)} 元，各酒吧分得合计 {centToYuan(approvingBill.pooledDistributedCent)} 元，超池 {centToYuan(overPoolCent(approvingBill))} 元。
                  特批仅限平台负责人核准的短期激励活动：特批后该单转「已确认」、超池分账获准过账；有效期自今日按后台配置期限起算（默认不超过 3 个月），到期自动失效须重新特批。
                </p>
                <div className="admin-form-grid">
                  <label>特批引用（审批凭据／理由，必填留痕）
                    <input
                      data-testid="settlement-special-approval-ref"
                      value={approveRef}
                      onChange={(event) => { setApproveRef(event.target.value); setApproveError(""); }}
                      placeholder="例：某月超池激励活动批件号及理由"
                    />
                  </label>
                </div>
                <div className="admin-form-row">
                  <HighRiskConfirm
                    label="确认超池特批"
                    impact={`结算月 ${approvingBill.periodStart?.slice(0, 7) ?? "—"} 结算单将转「已确认」,超池 ${centToYuan(overPoolCent(approvingBill))} 元的分账获准过账;有效期自今日按后台配置期限起算,到期自动失效。`}
                    precheck={() => {
                      if (!approveRef.trim()) {
                        setApproveError("特批引用必填（审计留痕），不能为空");
                        return false;
                      }
                      return true;
                    }}
                    disabled={approveBusy}
                    onConfirm={() => void submitSpecialApproval()}
                    permissionsState={effectivePermissions}
                    permissions={[SPECIAL_APPROVE_PERMISSION]}
                    data-testid="settlement-special-approval-submit"
                  />
                  <button type="button" data-testid="settlement-special-approval-cancel" onClick={() => { setApprovingBill(null); setApproveError(""); }}>取消</button>
                </div>
                {approveError ? <div className="admin-inline-error" data-testid="settlement-special-approval-error">{approveError}</div> : null}
              </div>
            ) : null}
          </div>
        ) : null}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
