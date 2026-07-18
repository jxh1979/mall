import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { TablePager } from "../components/TablePager";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import type { PagedResult } from "../client/adminClient";
import type {
  AdminPageProps,
  DividendComputeResult,
  DividendPlanRow,
  DividendStatementDetail,
  DividendStatementRow,
  MerchantAdmissionRow,
} from "../types";

// 持股分红（V299）：方案配置（凡涉数据皆可配，80/20 与 20% 税率仅默认值）+ 期间作业（季度正式/月度预览）
// + 计算单审阅审批（draft→pending_review→approved 挂应付→paid；预览只读）。
const VIEW_PERMS = ["finance.dividend.view"];
const MANAGE_PERMS = ["finance.dividend.manage"];
const PAGE_SIZE = 20;

export const DIVIDEND_KIND_LABELS: Record<string, string> = {
  official: "正式单",
  preview: "月度预览",
};

export const DIVIDEND_STATUS_LABELS: Record<string, string> = {
  preview: "预览（只读）",
  draft: "草稿",
  pending_review: "待审批",
  approved: "已审批",
  paid: "已发放",
  rejected: "已驳回",
  voided: "已作废",
};

const PERIOD_TYPE_LABELS: Record<string, string> = {
  quarterly: "季度（正式单按季，月度出预览）",
  monthly: "月度（正式单按月）",
};

const DOMAIN_LABELS: Record<string, string> = {
  bar: "酒吧",
  ticket: "售票",
};

const EXPENSE_TYPE_LABELS: Record<string, string> = {
  rent_allocation: "房租分摊",
  energy: "能耗费",
  property_fee: "物业费",
  labor: "人工",
  amortization: "摊销",
  other: "其他",
};

export const DIVIDEND_LINE_LABELS: Record<string, string> = {
  revenue_bar: "酒吧实收",
  revenue_bar_refund: "酒吧退款",
  revenue_ticket: "售票分量",
  revenue_ticket_refund: "售票退款",
  cost_inventory: "销货成本",
  expense_rent_allocation: "费用·房租分摊",
  expense_energy: "费用·能耗",
  expense_property_fee: "费用·物业费",
  expense_labor: "费用·人工",
  expense_amortization: "费用·摊销",
  expense_other: "费用·其他",
  net_profit: "净利润",
  platform_share: "平台分成",
  principal_gross: "主理人应得",
  tax_withheld: "代扣个税",
  principal_net: "净发放额",
};

const TEXT = {
  title: "持股分红",
  description:
    "平台持股 + 主理人经营的分红周期作业：期间收入 − 销货成本（V296 成本台账）− 费用（V297 费用台账）= 净利润，按方案比例劈分，主理人侧按股息红利代扣个税后净额发放。比例/税率/周期/基数构成全部可配，80/20 与 20% 仅默认值；每期算式逐项留痕可追溯。",
  loadFailed: "分红数据读取失败",
  detailFailed: "计算单明细读取失败",
  merchantRequired: "请选择商户。",
  principalRequired: "请填写主理人账户 ID（工商登记持股的真实股东）。",
  shareInvalid: "平台比例须在 0~100 之间。",
  taxInvalid: "代扣税率须在 0~100 之间。",
  domainRequired: "至少勾选一个计入收入域。",
  expenseRequired: "至少勾选一种计入费用类别。",
  periodLabelRequired: "请填写期间标签（季度如 2026-Q3，月份如 2026-07）。",
  reasonRequired: "驳回理由必填（进入审计留痕）。",
};

function centToYuan(cent: number | undefined | null): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

function merchantLabel(merchant: MerchantAdmissionRow): string {
  return merchant.merchantName ?? merchant.displayName ?? merchant.companyName ?? merchant.id;
}

function bpsToPercentText(bps: number | undefined): string {
  return bps === undefined ? "—" : `${(bps / 100).toFixed(bps % 100 === 0 ? 0 : 2)}%`;
}

type PlanForm = {
  merchantId: string;
  principalUserId: string;
  principalName: string;
  platformSharePercent: string;
  taxRatePercent: string;
  periodType: string;
  revenueDomains: string[];
  includeInventoryCost: boolean;
  expenseTypes: string[];
  status: string;
  note: string;
};

const DEFAULT_PLAN_FORM: PlanForm = {
  merchantId: "",
  principalUserId: "",
  principalName: "",
  platformSharePercent: "80",
  taxRatePercent: "20",
  periodType: "quarterly",
  revenueDomains: ["bar", "ticket"],
  includeInventoryCost: true,
  expenseTypes: Object.keys(EXPENSE_TYPE_LABELS),
  status: "active",
  note: "",
};

export function FinanceDividendsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [plans, setPlans] = React.useState<DividendPlanRow[]>([]);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  // 计算单筛选
  const [merchantFilter, setMerchantFilter] = React.useState("");
  const [kindFilter, setKindFilter] = React.useState("");
  const [statusFilter, setStatusFilter] = React.useState("");
  const [periodFilter, setPeriodFilter] = React.useState("");

  // 期间作业输入
  const [computeLabel, setComputeLabel] = React.useState("");
  const [computeKind, setComputeKind] = React.useState("official");
  const [computeNotice, setComputeNotice] = React.useState<DividendComputeResult | null>(null);

  // 方案表单（新建/编辑共用引导式弹窗；编辑=同弹窗预填 + PUT）
  const [planModalOpen, setPlanModalOpen] = React.useState(false);
  const [editingPlan, setEditingPlan] = React.useState<DividendPlanRow | null>(null);
  const [planForm, setPlanForm] = React.useState<PlanForm>(DEFAULT_PLAN_FORM);

  // 驳回理由弹窗
  const [rejectTarget, setRejectTarget] = React.useState<DividendStatementRow | null>(null);
  const [rejectReason, setRejectReason] = React.useState("");

  const merchantsResource = useAdminResource(
    async () => {
      const rows = await api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants");
      setMerchants(rows);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const plansResource = useAdminResource(
    async () => {
      setPlans(await api.get<DividendPlanRow[]>("/api/v1/finance/dividends/plans"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
  );

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<DividendStatementRow>> => {
      const params = new URLSearchParams({ page: String(page), page_size: String(size) });
      if (merchantFilter) params.set("merchant_id", merchantFilter);
      if (kindFilter) params.set("kind", kindFilter);
      if (statusFilter) params.set("status", statusFilter);
      if (periodFilter) params.set("period_label", periodFilter.trim());
      return api.getPaged<DividendStatementRow>(`/api/v1/finance/dividends/statements?${params.toString()}`);
    },
    [api, merchantFilter, kindFilter, statusFilter, periodFilter],
  );

  const rows = useServerPagedRows<DividendStatementRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: TEXT.loadFailed,
  });

  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [merchantFilter, kindFilter, statusFilter, periodFilter]);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: async () => {
      rows.reload();
      await plansResource.reload();
    },
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.loadFailed,
  });

  // ── 方案配置 ─────────────────────────────────────────────

  function openCreatePlan() {
    setFormError("");
    setEditingPlan(null);
    setPlanForm({ ...DEFAULT_PLAN_FORM, merchantId: merchants[0]?.id ?? "" });
    setPlanModalOpen(true);
  }

  function openEditPlan(plan: DividendPlanRow) {
    setFormError("");
    setEditingPlan(plan);
    setPlanForm({
      merchantId: plan.merchantId ?? "",
      principalUserId: plan.principalUserId ?? "",
      principalName: plan.principalName ?? "",
      platformSharePercent: plan.platformShareBps === undefined ? "80" : String(plan.platformShareBps / 100),
      taxRatePercent: plan.taxRateBps === undefined ? "20" : String(plan.taxRateBps / 100),
      periodType: plan.periodType ?? "quarterly",
      revenueDomains: plan.revenueDomains ?? ["bar", "ticket"],
      includeInventoryCost: plan.includeInventoryCost !== false,
      expenseTypes: plan.expenseTypes ?? Object.keys(EXPENSE_TYPE_LABELS),
      status: plan.status ?? "active",
      note: plan.note ?? "",
    });
    setPlanModalOpen(true);
  }

  function toggleToken(list: string[], token: string): string[] {
    return list.includes(token) ? list.filter((item) => item !== token) : [...list, token];
  }

  function percentToBps(text: string): number | null {
    const value = Number(text);
    if (!Number.isFinite(value) || value < 0 || value > 100) return null;
    return Math.round(value * 100);
  }

  async function submitPlan() {
    const shareBps = percentToBps(planForm.platformSharePercent);
    const taxBps = percentToBps(planForm.taxRatePercent);
    if (!planForm.merchantId) return setFormError(TEXT.merchantRequired);
    if (!planForm.principalUserId.trim()) return setFormError(TEXT.principalRequired);
    if (shareBps === null) return setFormError(TEXT.shareInvalid);
    if (taxBps === null) return setFormError(TEXT.taxInvalid);
    const request = {
      idempotencyKey: nextKey("dividend-plan"),
      merchantId: planForm.merchantId,
      principalUserId: planForm.principalUserId.trim(),
      principalName: planForm.principalName.trim() || null,
      platformShareBps: shareBps,
      taxRateBps: taxBps,
      periodType: planForm.periodType,
      revenueDomains: planForm.revenueDomains,
      includeInventoryCost: planForm.includeInventoryCost,
      expenseTypes: planForm.expenseTypes,
      status: planForm.status,
      note: planForm.note.trim() || null,
    };
    const ok = await runAction(editingPlan ? "dividend_plan_updated" : "dividend_plan_created", request, () =>
      editingPlan
        ? api.put(`/api/v1/finance/dividends/plans/${encodeURIComponent(editingPlan.id)}`, request)
        : api.post("/api/v1/finance/dividends/plans", request));
    if (ok) {
      setPlanModalOpen(false);
    }
  }

  // ── 期间作业 ─────────────────────────────────────────────

  async function runCompute() {
    if (!computeLabel.trim()) {
      setFormError(TEXT.periodLabelRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("dividend-compute"),
      periodLabel: computeLabel.trim(),
      kind: computeKind,
    };
    await runAction("dividend_computed", request, async () => {
      const result = await api.post<DividendComputeResult>("/api/v1/finance/dividends/statements/compute", request);
      setComputeNotice(result ?? null);
      return result;
    });
  }

  // ── 状态机动作 ────────────────────────────────────────────

  async function runTransition(row: DividendStatementRow, action: string, path: string) {
    const request = { idempotencyKey: nextKey(`dividend-${action}`) };
    await runAction(action, request, () =>
      api.post(`/api/v1/finance/dividends/statements/${encodeURIComponent(row.id)}/${path}`, request));
  }

  async function submitReject() {
    if (!rejectTarget) return;
    if (!rejectReason.trim()) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("dividend-reject"), note: rejectReason.trim() };
    const ok = await runAction("dividend_rejected", request, () =>
      api.post(`/api/v1/finance/dividends/statements/${encodeURIComponent(rejectTarget.id)}/reject`, request));
    if (ok) {
      setRejectTarget(null);
    }
  }

  // ── 明细抽屉 ─────────────────────────────────────────────

  async function openDetail(row: DividendStatementRow) {
    try {
      const data = await api.get<DividendStatementDetail>(
        `/api/v1/finance/dividends/statements/${encodeURIComponent(row.id)}`);
      const statement = data.statement ?? row;
      const merchant = merchants.find((m) => m.id === statement.merchantId);
      const fields: RowDetailField[] = [
        { label: "商户", value: merchant ? merchantLabel(merchant) : statement.merchantId ?? "—" },
        { label: "期间", value: `${statement.periodLabel ?? "—"}（${cnDate(statement.periodStart)} ~ ${cnDate(statement.periodEnd)}）` },
        { label: "类别 / 状态", value: `${DIVIDEND_KIND_LABELS[statement.kind ?? ""] ?? statement.kind} / ${DIVIDEND_STATUS_LABELS[statement.status ?? ""] ?? statement.status}` },
        { label: "主理人", value: statement.principalUserId ?? "—" },
        { label: "分成比例（平台/主理人）", value: `${bpsToPercentText(statement.platformShareBps)} / ${bpsToPercentText(statement.platformShareBps === undefined ? undefined : 10000 - statement.platformShareBps)}` },
        { label: "代扣税率", value: bpsToPercentText(statement.taxRateBps) },
        { label: "基数构成", value: `收入域：${(statement.revenueDomains ?? []).map((d) => DOMAIN_LABELS[d] ?? d).join("、") || "—"}；${statement.includeInventoryCost === false ? "不扣销货成本" : "扣销货成本"}；费用类别：${(statement.expenseTypes ?? []).map((t) => EXPENSE_TYPE_LABELS[t] ?? t).join("、") || "—"}` },
        ...(data.lines ?? []).map((line) => ({
          label: DIVIDEND_LINE_LABELS[line.lineType ?? ""] ?? line.lineType ?? "—",
          value: `¥${centToYuan(line.amountCent)}${line.detail ? `（${line.detail}）` : ""}`,
        })),
        { label: "应付款项", value: statement.arApItemId ?? "（审批通过后生成）" },
        ...(statement.rejectReason ? [{ label: "驳回理由", value: statement.rejectReason }] : []),
        { label: "算单时间", value: cnDate(statement.createdAt) },
        { label: "计算单号", value: statement.id },
      ];
      setDetail({ title: `分红计算单 · ${statement.periodLabel ?? statement.id}`, fields });
    } catch (detailError) {
      setGlobalError(detailError instanceof Error ? detailError.message : TEXT.detailFailed);
    }
  }

  const merchantOptions = merchants.filter((m) => m.status === "active" || m.status === undefined);
  const empty = !rows.loading && rows.total === 0 && !merchantFilter && !kindFilter && !statusFilter && !periodFilter;

  return (
    <section data-testid="finance-dividends-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="dividend.plan_created / dividend.computed / dividend.approved / dividend.paid_marked" />

      {/* ── 分红方案配置 ─────────────────────────────────────── */}
      <h3>分红方案</h3>
      <div className="admin-form-row">
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="dividend-plan-create-open"
          onClick={openCreatePlan}
        >＋ 新建分红方案</PermissionButton>
        <span className="admin-muted-action">一个商户同一时期只允许一个生效中方案；改比例/口径即时生效于下次算单，历史单按单内快照追溯。</span>
      </div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="dividend-plan-table">
          <thead><tr><th>商户</th><th>主理人</th><th>平台/主理人</th><th>代扣税率</th><th>周期</th><th>基数构成</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            {plans.length === 0 ? (
              <tr data-testid="dividend-plan-table-empty"><td colSpan={8} className="admin-muted-action">暂无分红方案，点「新建分红方案」配置。</td></tr>
            ) : plans.map((plan) => (
              <tr key={plan.id} data-testid={`dividend-plan-row-${plan.id}`}>
                <td>{merchants.find((m) => m.id === plan.merchantId) ? merchantLabel(merchants.find((m) => m.id === plan.merchantId)!) : plan.merchantId}</td>
                <td>{plan.principalName ? `${plan.principalName}（${plan.principalUserId}）` : plan.principalUserId}</td>
                <td>{bpsToPercentText(plan.platformShareBps)} / {bpsToPercentText(plan.platformShareBps === undefined ? undefined : 10000 - plan.platformShareBps)}</td>
                <td>{bpsToPercentText(plan.taxRateBps)}</td>
                <td>{PERIOD_TYPE_LABELS[plan.periodType ?? ""] ?? plan.periodType}</td>
                <td>{(plan.revenueDomains ?? []).map((d) => DOMAIN_LABELS[d] ?? d).join("、")}{plan.includeInventoryCost === false ? "；不扣成本" : "；扣成本"}</td>
                <td>{plan.status === "ended" ? "已停用" : "生效中"}</td>
                <td>
                  <PermissionButton
                    permissionsState={effectivePermissions}
                    permissions={MANAGE_PERMS}
                    data-testid={`dividend-plan-edit-${plan.id}`}
                    onClick={() => openEditPlan(plan)}
                  >修改</PermissionButton>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ── 期间作业 ─────────────────────────────────────────── */}
      <h3>期间作业</h3>
      <div className="admin-form-row" data-testid="dividend-compute-row">
        <label>期间标签<input data-testid="dividend-compute-label" type="text" placeholder="季度 2026-Q3 / 月份 2026-07" value={computeLabel} onChange={(event) => setComputeLabel(event.target.value)} /></label>
        <label>类别<select data-testid="dividend-compute-kind" value={computeKind} onChange={(event) => setComputeKind(event.target.value)}>
          <option value="official">正式单（进入审批发放）</option>
          <option value="preview">月度预览（只看不发）</option>
        </select></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="dividend-compute-run"
          disabled={busyAction === "dividend_computed"}
          onClick={runCompute}
        >生成分红计算单</PermissionButton>
        <span className="admin-muted-action">对每个生效中方案算一张；重跑幂等：草稿/预览/已驳回作废重算，已进入审批流跳过。请先完成该期费用自动挂接，费用不全会低估扣减。</span>
      </div>
      {computeNotice ? (
        <div className="admin-form-row" data-testid="dividend-compute-notice">
          <span>本次生成 {computeNotice.computed?.length ?? 0} 张计算单{(computeNotice.skipped?.length ?? 0) > 0 ? `，跳过 ${computeNotice.skipped!.length} 家：` : "。"}</span>
          {(computeNotice.skipped ?? []).map((skip) => (
            <span key={`${skip.merchantId}-${skip.reason}`} className="admin-muted-action">{skip.merchantId}（{skip.reason}）</span>
          ))}
        </div>
      ) : null}

      {/* ── 计算单列表 ───────────────────────────────────────── */}
      <h3>分红计算单</h3>
      <div className="admin-form-row" data-testid="dividend-statement-filter">
        <label>商户<select data-testid="dividend-filter-merchant" value={merchantFilter} onChange={(event) => setMerchantFilter(event.target.value)}>
          <option value="">全部商户</option>
          {merchantOptions.map((m) => <option key={m.id} value={m.id}>{merchantLabel(m)}</option>)}
        </select></label>
        <label>类别<select data-testid="dividend-filter-kind" value={kindFilter} onChange={(event) => setKindFilter(event.target.value)}>
          <option value="">全部类别</option>
          {Object.entries(DIVIDEND_KIND_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>状态<select data-testid="dividend-filter-status" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="">全部状态</option>
          {Object.entries(DIVIDEND_STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>期间<input data-testid="dividend-filter-period" type="text" placeholder="2026-Q3 / 2026-07" value={periodFilter} onChange={(event) => setPeriodFilter(event.target.value)} /></label>
      </div>

      <AdminDataState loading={rows.loading || merchantsResource.loading || plansResource.loading} error={rows.error ?? merchantsResource.error ?? plansResource.error} empty={empty} onRetry={rows.reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="dividend-statement-table">
            <thead><tr><th>期间</th><th>商户</th><th>类别</th><th>净利润（元）</th><th>主理人应得（元）</th><th>代扣税（元）</th><th>净发放（元）</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {rows.rows.length === 0 ? (
                <tr data-testid="dividend-statement-table-empty"><td colSpan={9} className="admin-muted-action">当前筛选下暂无计算单。</td></tr>
              ) : rows.rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`dividend-statement-row-${row.id}`}
                  onClick={() => void openDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); void openDetail(row); } }}>
                  <td>{row.periodLabel ?? "—"}</td>
                  <td>{merchants.find((m) => m.id === row.merchantId) ? merchantLabel(merchants.find((m) => m.id === row.merchantId)!) : row.merchantId ?? "—"}</td>
                  <td>{DIVIDEND_KIND_LABELS[row.kind ?? ""] ?? row.kind ?? "—"}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.netProfitCent)}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.principalGrossCent)}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.taxCent)}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.principalNetCent)}</td>
                  <td>{DIVIDEND_STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td className="admin-row-detail-cell" onClick={(event) => event.stopPropagation()}>
                    <button type="button" className="admin-row-detail-btn" aria-label="查看分红计算单明细" onClick={(event) => { event.stopPropagation(); void openDetail(row); }}>查看详情</button>
                    {row.status === "draft" ? (
                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS}
                        data-testid={`dividend-submit-${row.id}`}
                        onClick={() => void runTransition(row, "dividend_submitted", "submit")}>提交审阅</PermissionButton>
                    ) : null}
                    {row.status === "pending_review" ? (
                      <>
                        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS}
                          data-testid={`dividend-approve-${row.id}`}
                          onClick={() => void runTransition(row, "dividend_approved", "approve")}>审批通过</PermissionButton>
                        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS}
                          data-testid={`dividend-reject-open-${row.id}`}
                          onClick={() => { setFormError(""); setRejectReason(""); setRejectTarget(row); }}>驳回</PermissionButton>
                      </>
                    ) : null}
                    {row.status === "approved" ? (
                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS}
                        data-testid={`dividend-mark-paid-${row.id}`}
                        onClick={() => void runTransition(row, "dividend_paid_marked", "mark-paid")}>标记发放</PermissionButton>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="dividend-statement-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
        </div>
      </AdminDataState>

      {formError && !planModalOpen && !rejectTarget ? (
        <div className="admin-form-error" data-testid="dividend-form-error">{formError}</div>
      ) : null}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />

      {/* ── 方案配置（引导式，新建/修改共用）───────────────────── */}
      <GuidedFlowModal
        open={planModalOpen}
        title={editingPlan ? `修改分红方案 · ${editingPlan.id}` : "新建分红方案"}
        testId="dividend-plan-guided"
        submitTestId="dividend-plan-save"
        submitLabel={editingPlan ? "确认修改" : "确认创建"}
        submitting={busyAction === "dividend_plan_created" || busyAction === "dividend_plan_updated"}
        error={formError || undefined}
        onClose={() => setPlanModalOpen(false)}
        onSubmit={submitPlan}
        steps={[
          {
            title: "商户与主理人",
            description: "主理人=工商登记持股的真实股东（税目按股息红利代扣）；发放走应付台账挂到该账户。",
            validate: () => {
              if (!planForm.merchantId) return TEXT.merchantRequired;
              if (!planForm.principalUserId.trim()) return TEXT.principalRequired;
              return null;
            },
            content: (
              <>
                <label>经营商户<select data-testid="dividend-plan-merchant" value={planForm.merchantId} disabled={editingPlan !== null} onChange={(event) => setPlanForm({ ...planForm, merchantId: event.target.value })}>
                  {merchantOptions.length === 0 ? <option value="">（暂无商户）</option> : null}
                  {merchantOptions.map((m) => <option key={m.id} value={m.id}>{merchantLabel(m)}</option>)}
                </select></label>
                <label>主理人账户 ID<input data-testid="dividend-plan-principal" type="text" value={planForm.principalUserId} onChange={(event) => setPlanForm({ ...planForm, principalUserId: event.target.value })} placeholder="如 usr_xxx（系统内账户）" /></label>
                <label>主理人姓名（选填）<input data-testid="dividend-plan-principal-name" type="text" value={planForm.principalName} onChange={(event) => setPlanForm({ ...planForm, principalName: event.target.value })} /></label>
              </>
            ),
          },
          {
            title: "比例、税率与周期",
            description: "全部可调：80/20 与 20% 仅默认值。调整即时生效于下次算单，已出历史单按单内快照追溯，不受影响。",
            validate: () => {
              if (percentToBps(planForm.platformSharePercent) === null) return TEXT.shareInvalid;
              if (percentToBps(planForm.taxRatePercent) === null) return TEXT.taxInvalid;
              return null;
            },
            content: (
              <>
                <label>平台比例（%）<input data-testid="dividend-plan-share" type="number" step="0.01" min="0" max="100" value={planForm.platformSharePercent} onChange={(event) => setPlanForm({ ...planForm, platformSharePercent: event.target.value })} /></label>
                <span className="admin-muted-action">主理人比例 = 100% − 平台比例（劈分余数归主理人，不丢分）。</span>
                <label>代扣税率（%）<input data-testid="dividend-plan-tax" type="number" step="0.01" min="0" max="100" value={planForm.taxRatePercent} onChange={(event) => setPlanForm({ ...planForm, taxRatePercent: event.target.value })} /></label>
                <label>分红周期<select data-testid="dividend-plan-period-type" value={planForm.periodType} onChange={(event) => setPlanForm({ ...planForm, periodType: event.target.value })}>
                  {Object.entries(PERIOD_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                {editingPlan ? (
                  <label>方案状态<select data-testid="dividend-plan-status" value={planForm.status} onChange={(event) => setPlanForm({ ...planForm, status: event.target.value })}>
                    <option value="active">生效中</option>
                    <option value="ended">停用（不再出单）</option>
                  </select></label>
                ) : null}
              </>
            ),
          },
          {
            title: "分红基数构成",
            description: "净利润 = 计入收入域合计 −（可选）销货成本 − 计入费用类别净额。凡涉数据皆可配。",
            validate: () => {
              if (planForm.revenueDomains.length === 0) return TEXT.domainRequired;
              if (planForm.expenseTypes.length === 0) return TEXT.expenseRequired;
              return null;
            },
            content: (
              <>
                <span>计入收入域：</span>
                {Object.entries(DOMAIN_LABELS).map(([value, label]) => (
                  <label key={value}><input data-testid={`dividend-plan-domain-${value}`} type="checkbox" checked={planForm.revenueDomains.includes(value)} onChange={() => setPlanForm({ ...planForm, revenueDomains: toggleToken(planForm.revenueDomains, value) })} />{label}</label>
                ))}
                <label><input data-testid="dividend-plan-include-cost" type="checkbox" checked={planForm.includeInventoryCost} onChange={() => setPlanForm({ ...planForm, includeInventoryCost: !planForm.includeInventoryCost })} />扣减销货成本（V296 移动加权出库成本）</label>
                <span>计入费用类别（V297 费用台账）：</span>
                {Object.entries(EXPENSE_TYPE_LABELS).map(([value, label]) => (
                  <label key={value}><input data-testid={`dividend-plan-expense-${value}`} type="checkbox" checked={planForm.expenseTypes.includes(value)} onChange={() => setPlanForm({ ...planForm, expenseTypes: toggleToken(planForm.expenseTypes, value) })} />{label}</label>
                ))}
                <label>备注（选填）<textarea data-testid="dividend-plan-note" value={planForm.note} onChange={(event) => setPlanForm({ ...planForm, note: event.target.value })} placeholder="如：与主理人协议约定 80/20" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "经营商户", value: merchantOptions.find((m) => m.id === planForm.merchantId) ? merchantLabel(merchantOptions.find((m) => m.id === planForm.merchantId)!) : (planForm.merchantId || "未选择") },
          { label: "主理人", value: planForm.principalName ? `${planForm.principalName}（${planForm.principalUserId}）` : (planForm.principalUserId || "未填写") },
          { label: "平台/主理人", value: `${planForm.platformSharePercent || "?"}% / ${planForm.platformSharePercent ? (100 - Number(planForm.platformSharePercent)).toFixed(2).replace(/\.?0+$/, "") : "?"}%` },
          { label: "代扣税率", value: `${planForm.taxRatePercent || "?"}%（股息红利）` },
          { label: "周期", value: PERIOD_TYPE_LABELS[planForm.periodType] ?? planForm.periodType },
          { label: "基数构成", value: `收入域：${planForm.revenueDomains.map((d) => DOMAIN_LABELS[d] ?? d).join("、") || "无"}；${planForm.includeInventoryCost ? "扣销货成本" : "不扣销货成本"}；费用：${planForm.expenseTypes.map((t) => EXPENSE_TYPE_LABELS[t] ?? t).join("、") || "无"}` },
          ...(editingPlan ? [{ label: "方案状态", value: planForm.status === "ended" ? "停用" : "生效中" }] : []),
        ]}
      />

      {/* ── 驳回（理由必填）─────────────────────────────────── */}
      <GuidedFlowModal
        open={rejectTarget !== null}
        title={`驳回分红计算单 · ${rejectTarget?.periodLabel ?? ""}`}
        testId="dividend-reject-guided"
        submitTestId="dividend-reject-save"
        submitLabel="确认驳回"
        submitting={busyAction === "dividend_rejected"}
        error={formError || undefined}
        onClose={() => setRejectTarget(null)}
        onSubmit={submitReject}
        steps={[
          {
            title: "驳回理由（必填）",
            description: "驳回后可修正数据（如补挂费用）再重算：旧单作废、按当前口径出新草稿单。",
            validate: () => (rejectReason.trim() ? null : TEXT.reasonRequired),
            content: (
              <label>理由<textarea data-testid="dividend-reject-note" value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} placeholder="如：能耗费未挂接完整，费用低估" /></label>
            ),
          },
        ]}
        summary={[
          { label: "计算单", value: rejectTarget ? `${rejectTarget.periodLabel}（${rejectTarget.id}）` : "—" },
          { label: "理由", value: rejectReason.trim() || "未填写" },
        ]}
      />
    </section>
  );
}
