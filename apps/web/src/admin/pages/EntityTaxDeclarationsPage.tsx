// 文件版本：V0.02
import React from "react";
import { withBase } from "../../basePath";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { cnDate } from "../components/RowDetailDrawer";
import { TablePager } from "../components/TablePager";
import { useAdminAction } from "../hooks/useAdminAction";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { PermissionButton } from "../security/Can";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import type { PagedResult } from "../client/adminClient";
import type { AdminPageProps, BusinessEntityRow } from "../types";

// 税费申报登记（ADM-FIN-010；第十八批 AC 段第 210 条 210-R2，第十八批裁决 42）：财务按公司、税种、所属期手工登记「实际申报金额」，
// 经财务、税务、法务审核并由顶层平台公司老板最终确认后生效（BSM:1760）；分红扣本期已生效的申报金额。登记即提交审批，驳回后可重新登记。
// 第十八批 AR 段第 233 条：已生效行「登记实际缴纳」（233-R1，缴税差异按未缴、预缴、多缴、抵扣、待退分别处理，不计入税费成本）与「生成差异调整单」
// （233-R3，原期间已申报的不改原申报，在当前开放期间生成关联原期间的差异调整单，计入批准当月），同走财税法审核与最高确认。
// 233-R2「税率配置」页签（点开才加载）：每个主体 × 每类收入 × 每种税配「有没有这笔费用 + 收多少」（ENUM:201），填生效日、存为新版本（BSM:521），
// 同走财税法审核与最高确认；企业所得税按全部收入配一个比例，其他税费不配比例（第十八批裁决 59）。
// 233-R5：增值税、文化事业建设费、附加税登记时现算的系统计算金额、申报差异与差异处理（补提或冲回调整单；企业所得税与其他税费按季核对拉平，
// 第十八批裁决 59），系统计算金额格内可展开看逐月、逐收入类型的计算依据（BSM:1760「税费金额分三个阶段分别保存、互不覆盖」）。
// 233-R6「主体收入成本利润」页签（点开且选齐经营主体与起止月份才加载）：按第十八批裁决 60 汇总收入分项、成本分项与利润；233-R7 企业所得税系统计算＝
// 所属期利润×企业所得税比例，按季核对拉平的税种已生效后以申报额（财税软件结果）为准，显示拉平后金额。
type Breakdown = { category: string; amountCent: number };
type Profit = {
  revenueCent: number;
  revenueItems: { incomeType: string; amountCent: number }[];
  costCent: number;
  costItems: { costType: string; amountCent: number }[];
  profitCent: number;
  basis: string;
};
type Basis = {
  month: string; incomeType: string; incomeCent: number; rateBps: number; taxAmountCent: number; vatAmountCent?: number | null; profit?: Profit | null;
};
type Adjustment = {
  id: string;
  kind: string;
  amountCent: number;
  originPeriodStartMonth: string;
  originPeriodEndMonth: string;
  accountingMonth?: string | null;
  status: string;
};
type Declaration = {
  id: string;
  businessEntityId: string;
  entityName?: string | null;
  taxType: string;
  periodStartMonth: string;
  periodEndMonth: string;
  declaredAmountCent: number;
  status: string;
  reviewId?: string | null;
  createdBy?: string;
  createdAt?: string;
  paidAmountCent?: number | null;
  paymentDifferenceCent?: number | null;
  paymentBreakdown?: Breakdown[] | null;
  paymentPending?: boolean;
  adjustments?: Adjustment[];
  systemCalculatedAmountCent?: number | null;
  declarationDifferenceCent?: number | null;
  differenceMechanism?: string;
  calculationBasis?: Basis[] | null;
  alignedAmountCent?: number | null;
};
type Act = { mode: "payment" | "adjustment"; row: Declaration };
type Rate = {
  id: string;
  businessEntityId?: string | null;
  entityName?: string | null;
  incomeType: string;
  taxType: string;
  applicable: boolean;
  rateBps: number;
  effectiveFrom?: string | null;
  status: string;
  reviewId?: string | null;
};

const MANAGE = ["finance.entity_tax.manage"];
const PAGE_SIZE = 20;
const TAX_LABELS: Record<string, string> = { vat: "增值税", cultural_fee: "文化事业建设费", surcharge: "附加税", corporate_income: "企业所得税", other: "其他税费" };
const STATUS_LABELS: Record<string, string> = { pending_review: "待财务、税务、法务审核与最高确认", approved: "已生效", rejected: "已驳回（可重新登记）" };
const LINES = { finance_review: "财务最终审批人", tax_review: "税务最终审批人", legal_review: "法务最终审批人" } as const;
type LineCode = keyof typeof LINES;
const LINE_CODES = Object.keys(LINES) as LineCode[];
const EMPTY_FORM = { businessEntityId: "", taxType: "vat", periodStartMonth: "", periodEndMonth: "", amountYuan: "", reason: "" };
const EMPTY_REVIEWERS: Record<LineCode, string> = { finance_review: "", tax_review: "", legal_review: "" };
// BSM:1760「实缴与申报不一致的部分先按未缴、预缴、多缴、抵扣或待退分别处理」。
const CATEGORY_LABELS: Record<string, string> = { unpaid: "未缴", prepaid: "预缴", overpaid: "多缴", offset: "抵扣", refund_pending: "待退" };
const CATEGORY_CODES = Object.keys(CATEGORY_LABELS);
const ADJUSTMENT_KIND_LABELS: Record<string, string> = { supplement: "补提调整单", reversal: "冲回调整单", period_difference: "差异调整单" };
const EMPTY_ACT_FORM = { amountYuan: "", reason: "", categories: {} as Record<string, string> };
// 233-R2：可配比例的税种（其他税费不配）；收入类型取字典（all 只给企业所得税）。
const RATE_TAX_TYPES = ["vat", "cultural_fee", "surcharge", "corporate_income"];
const INCOME_LABELS: Record<string, string> = KEY_VALUE_LABELS.income_type ?? {};
const RATE_INCOME_TYPES = Object.keys(INCOME_LABELS).filter((code) => code !== "all");
const EMPTY_RATE_FORM = { businessEntityId: "", taxType: "vat", incomeType: "", applicable: true, percent: "", effectiveFrom: "", reason: "" };
const percent = (bps: number) => `${(bps / 100).toFixed(2)}%`;
// 233-R6 成本分项中文（字典 KEY_VALUE_LABELS.cost_type）与汇总口径说明（第十八批裁决 60）。
const COST_LABELS: Record<string, string> = KEY_VALUE_LABELS.cost_type ?? {};
const PROFIT_BASIS_TEXT: Record<string, string> = {
  platform: "平台主体口径：收入按收入归属归到本主体；成本为本主体在平台内作为付款方的内部结算应付与作为发放主体的工资单税前应发（第十八批裁决 60）。",
  dividend: "酒吧经营主体：沿用持股分红税前口径，利润＝收入−销货成本−费用。",
};

function yuan(cent: number): string {
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100);
}

const cents = (value: string) => Math.round(Number(value) * 100);
const breakdownText = (items: Breakdown[]) => items.map((b) => `${CATEGORY_LABELS[b.category] ?? b.category} ${yuan(b.amountCent)}`).join("、");
// 补提、冲回调整单存差额绝对值，与「差异处理」列同写不带符号；只有关联原期间的差异调整单可正可负，按正负带符号。
const adjustmentText = (a: Adjustment) => `${ADJUSTMENT_KIND_LABELS[a.kind] ?? a.kind} ${a.kind === "period_difference" && a.amountCent > 0 ? "+" : ""}${yuan(a.amountCent)} 元（关联原所属期 `
  + `${a.originPeriodStartMonth}～${a.originPeriodEndMonth}${a.accountingMonth ? `，计入 ${a.accountingMonth}` : ""}｜${STATUS_LABELS[a.status] ?? a.status}）`;

// 233-R5 差异处理（BSM:1760「两套差异机制按税种互斥适用、不得叠加」）：流转税按申报差异出补提或冲回调整单，企业所得税与其他税费按季核对拉平。
function mechanismText(row: Declaration): string {
  // 233-R7：已生效后本平台该税该期以申报额（财税软件结果，含已生效差异调整单）为准。
  if (row.differenceMechanism === "alignment") {
    return row.alignedAmountCent == null ? "按季核对拉平" : `按季核对拉平，以财税软件结果为准：${yuan(row.alignedAmountCent)} 元`;
  }
  const diff = row.declarationDifferenceCent;
  if (diff == null) return "—";
  return diff > 0 ? `补提调整单 ${yuan(diff)} 元` : diff < 0 ? `冲回调整单 ${yuan(-diff)} 元` : "与系统计算一致，不出调整单";
}

// 233-R1 缴税差异的基数：更正后的实际申报金额＝申报额＋已生效差异调整单（同后端 EntityTaxService.correctedDeclared）。
function correctedDeclared(row: Declaration): number {
  return row.declaredAmountCent + (row.adjustments ?? [])
    .filter((a) => a.kind === "period_difference" && a.status === "approved")
    .reduce((sum, a) => sum + a.amountCent, 0);
}

export function EntityTaxDeclarationsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [filter, setFilter] = React.useState({ businessEntityId: "", month: "", taxType: "", status: "" });
  const [open, setOpen] = React.useState(false);
  const [form, setForm] = React.useState(EMPTY_FORM);
  const [reviewers, setReviewers] = React.useState(EMPTY_REVIEWERS);
  const [requestKey, setRequestKey] = React.useState("");
  const [act, setAct] = React.useState<Act | null>(null);
  const [actForm, setActForm] = React.useState(EMPTY_ACT_FORM);
  const [tab, setTab] = React.useState<"declarations" | "rates" | "profits">("declarations");
  const [rates, setRates] = React.useState<Rate[] | null>(null);
  const [rateOpen, setRateOpen] = React.useState(false);
  const [rateForm, setRateForm] = React.useState(EMPTY_RATE_FORM);
  const [profitQuery, setProfitQuery] = React.useState({ businessEntityId: "", startMonth: "", endMonth: "" });
  const [profit, setProfit] = React.useState<Profit | null>(null);

  // 经营主体下拉（读不到不阻断列表：列表行自带主体名）。
  React.useEffect(() => {
    api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities")
      .then((rows) => setEntities(Array.isArray(rows) ? rows : []))
      .catch(() => setEntities([]));
  }, [api]);

  const fetchPage = React.useCallback((page: number, size: number): Promise<PagedResult<Declaration>> => {
    const params = new URLSearchParams({ page: String(page), page_size: String(size) });
    if (filter.businessEntityId) params.set("business_entity_id", filter.businessEntityId);
    if (filter.month) params.set("month", filter.month);
    if (filter.taxType) params.set("tax_type", filter.taxType);
    if (filter.status) params.set("status", filter.status);
    return api.getPaged<Declaration>(`/api/v1/finance/entity-taxes?${params.toString()}`);
  }, [api, filter]);
  const rows = useServerPagedRows<Declaration>(fetchPage, PAGE_SIZE, { onError: setGlobalError, fallbackMessage: "税费申报登记读取失败" });
  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  function loadRates() {
    return api.get<Rate[]>("/api/v1/finance/entity-taxes/rates")
      .then((list) => setRates(Array.isArray(list) ? list : []))
      .catch((error: unknown) => {
        setRates([]);
        setGlobalError(error instanceof Error ? error.message : "税率配置读取失败");
      });
  }
  React.useEffect(() => {
    if (tab === "rates") void loadRates();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab]);

  // 233-R6「主体收入成本利润」页签：点开且选齐经营主体与起止月份才请求。
  const profitReady = Boolean(profitQuery.businessEntityId && profitQuery.startMonth && profitQuery.endMonth
    && profitQuery.endMonth >= profitQuery.startMonth);
  React.useEffect(() => {
    setProfit(null);
    if (tab !== "profits" || !profitReady) return;
    let live = true;
    const params = new URLSearchParams({ business_entity_id: profitQuery.businessEntityId, start_month: profitQuery.startMonth, end_month: profitQuery.endMonth });
    api.get<Profit>(`/api/v1/finance/entity-taxes/profits?${params.toString()}`)
      .then((result) => { if (live) setProfit(result); })
      .catch((error: unknown) => { if (live) setGlobalError(error instanceof Error ? error.message : "主体收入成本利润读取失败"); });
    return () => { live = false; };
  }, [api, tab, profitReady, profitQuery, setGlobalError]);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: async () => (tab === "rates" ? loadRates() : rows.reload()),
    setGlobalError,
    setActionResult,
    makeResult: (action, request, result) => ({ action, idempotencyKey: request.idempotencyKey, result }),
    fallbackMessage: "税费申报登记失败",
  });

  function openCreate() {
    setFormError("");
    setForm({ ...EMPTY_FORM, businessEntityId: filter.businessEntityId });
    setReviewers(EMPTY_REVIEWERS);
    setRequestKey(`entity-tax-${Date.now()}`);
    setOpen(true);
  }

  function validateDeclaration(): string | null {
    if (!form.businessEntityId) return "请选择经营主体。";
    if (!form.periodStartMonth || !form.periodEndMonth) return "请填写所属期首月和末月（按季申报的填整季）。";
    if (form.periodEndMonth < form.periodStartMonth) return "所属期末月不能早于首月。";
    const amount = Number(form.amountYuan);
    if (!form.amountYuan.trim() || !Number.isFinite(amount) || amount < 0) return "请按申报表填写实际申报金额（元），不能为负数。";
    if (!form.reason.trim()) return "请填写登记原因（如按哪一期申报表登记）。";
    return null;
  }

  function validateReviewers(): string | null {
    return LINE_CODES.some((code) => !reviewers[code].trim()) ? "请填写财务、税务、法务三条线的最终审批人。" : null;
  }

  async function submit() {
    const invalid = validateDeclaration() ?? validateReviewers();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request = {
      idempotencyKey: requestKey,
      businessEntityId: form.businessEntityId,
      taxType: form.taxType,
      periodStartMonth: form.periodStartMonth,
      periodEndMonth: form.periodEndMonth,
      declaredAmountCent: Math.round(Number(form.amountYuan) * 100),
      reason: form.reason.trim(),
      parallelLines: LINE_CODES.map((lineCode) => ({ lineCode, finalApproverId: reviewers[lineCode].trim() })),
    };
    if (await runAction("entity_tax_declared", request, () => api.post("/api/v1/finance/entity-taxes", request))) setOpen(false);
  }

  function openAct(mode: Act["mode"], row: Declaration) {
    setFormError("");
    setActForm(EMPTY_ACT_FORM);
    setReviewers(EMPTY_REVIEWERS);
    setRequestKey(`entity-tax-${mode}-${Date.now()}`);
    setAct({ mode, row });
  }

  const actDeclared = act ? correctedDeclared(act.row) : 0;
  const actDifference = act?.mode === "payment" && actForm.amountYuan.trim() && Number.isFinite(Number(actForm.amountYuan))
    ? cents(actForm.amountYuan) - actDeclared : null;
  const actBreakdown: Breakdown[] = CATEGORY_CODES.filter((code) => (actForm.categories[code] ?? "").trim())
    .map((category) => ({ category, amountCent: cents(actForm.categories[category] ?? "") }));

  function validateAct(): string | null {
    if (!act) return null;
    const amount = Number(actForm.amountYuan);
    if (act.mode === "payment") {
      if (!actForm.amountYuan.trim() || !Number.isFinite(amount) || amount < 0) return "请按完税凭证填写实际缴纳金额（元），不能为负数。";
      if (actDifference) {
        if (actBreakdown.some((b) => !Number.isFinite(b.amountCent) || b.amountCent <= 0)) return "缴税差异分类金额须大于 0。";
        if (actBreakdown.reduce((sum, b) => sum + b.amountCent, 0) !== Math.abs(actDifference)) {
          return `缴税差异须按未缴、预缴、多缴、抵扣、待退分配，各类合计须等于 ${yuan(Math.abs(actDifference))} 元。`;
        }
      }
    } else if (!actForm.amountYuan.trim() || !Number.isFinite(amount) || amount === 0) {
      return "请填写更正差额（元），多申报的填负数，不能为 0。";
    }
    if (!actForm.reason.trim()) return act.mode === "payment" ? "请填写登记原因（如按哪一期完税凭证登记）。" : "请填写更正原因（如更正哪一期申报表）。";
    return null;
  }

  async function submitAct() {
    if (!act) return;
    const invalid = validateAct() ?? validateReviewers();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const parallelLines = LINE_CODES.map((lineCode) => ({ lineCode, finalApproverId: reviewers[lineCode].trim() }));
    const path = `/api/v1/finance/entity-taxes/${encodeURIComponent(act.row.id)}`;
    if (act.mode === "payment") {
      const request = { idempotencyKey: requestKey, paidAmountCent: cents(actForm.amountYuan), differenceBreakdown: actDifference ? actBreakdown : [],
        reason: actForm.reason.trim(), parallelLines };
      if (await runAction("entity_tax_payment_registered", request, () => api.post(`${path}/payments`, request))) setAct(null);
      return;
    }
    const request = { idempotencyKey: requestKey, amountCent: cents(actForm.amountYuan), reason: actForm.reason.trim(), parallelLines };
    if (await runAction("entity_tax_adjustment_registered", request, () => api.post(`${path}/adjustments`, request))) setAct(null);
  }

  function openRate() {
    setFormError("");
    setRateForm(EMPTY_RATE_FORM);
    setReviewers(EMPTY_REVIEWERS);
    setRequestKey(`entity-tax-rate-${Date.now()}`);
    setRateOpen(true);
  }

  function setRateTax(taxType: string) {
    const incomeType = taxType === "corporate_income" ? "all" : rateForm.incomeType === "all" ? "" : rateForm.incomeType;
    setRateForm({ ...rateForm, taxType, incomeType });
  }

  function validateRate(): string | null {
    if (!rateForm.incomeType) return "请选择收入类型。";
    const value = Number(rateForm.percent);
    if (rateForm.applicable && (!rateForm.percent.trim() || !Number.isFinite(value) || value < 0 || value > 100)) return "请填写比例（0～100%）。";
    if (!rateForm.effectiveFrom) return "请填写生效起始日期。";
    if (!rateForm.reason.trim()) return "请填写原因（如依据哪份税务文件调整）。";
    return null;
  }

  async function submitRate() {
    const invalid = validateRate() ?? validateReviewers();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request = {
      idempotencyKey: requestKey,
      businessEntityId: rateForm.businessEntityId || null,
      incomeType: rateForm.incomeType,
      taxType: rateForm.taxType,
      applicable: rateForm.applicable,
      rateBps: rateForm.applicable ? Math.round(Number(rateForm.percent) * 100) : 0,
      effectiveFrom: rateForm.effectiveFrom,
      reason: rateForm.reason.trim(),
      parallelLines: LINE_CODES.map((lineCode) => ({ lineCode, finalApproverId: reviewers[lineCode].trim() })),
    };
    if (await runAction("entity_tax_rate_submitted", request, () => api.post("/api/v1/finance/entity-taxes/rates", request))) setRateOpen(false);
  }

  const reviewerInputs = (prefix: string) => (
    <>
      {LINE_CODES.map((code) => (
        <label key={code}>{LINES[code]}<input data-testid={`${prefix}-${code}`} value={reviewers[code]} placeholder="员工账号编号" onChange={(event) => setReviewers({ ...reviewers, [code]: event.target.value })} /></label>
      ))}
    </>
  );

  const entityName = (id: string) => entities.find((e) => e.id === id)?.entityName ?? id;

  return (
    <section data-testid="entity-tax-page">
      <AdminPageHeader title="税费申报登记" description="「税率配置」页签按主体、收入类型、税种维护税率版本（填生效起始日期，经同样的审核与最终确认后按生效日起用）。按经营主体、税种、所属期登记每笔税的实际申报金额（按季申报的整季登记一条）；登记后经财务、税务、法务审核，再由顶层平台公司最终确认才生效，提交人不能审批自己的登记。持股分红扣本期已生效的申报金额。增值税、文化事业建设费、附加税登记时按该主体当月各类收入与当时生效的税率算出系统计算金额（可展开看计算依据），生效后按申报差异生成补提或冲回调整单、差异保留；企业所得税（系统计算金额＝所属期利润×企业所得税比例）与其他税费按季与财税软件核对、以财税软件为准拉平。已生效的申报可登记实际缴纳金额（缴税差异按未缴、预缴、多缴、抵扣、待退分别处理，不计入税费成本）；原期间已申报的更正不改原申报，只能生成关联原所属期的差异调整单，计入批准当月。「主体收入成本利润」页签按经营主体与起止月份汇总收入、成本与利润。" />
      <div className="admin-tabs" role="tablist" data-testid="entity-tax-tabs">
        <button type="button" role="tab" aria-selected={tab === "declarations"} className={tab === "declarations" ? "is-active" : ""} data-testid="entity-tax-tab-declarations" onClick={() => setTab("declarations")}>税费申报登记</button>
        <button type="button" role="tab" aria-selected={tab === "rates"} className={tab === "rates" ? "is-active" : ""} data-testid="entity-tax-tab-rates" onClick={() => setTab("rates")}>税率配置</button>
        <button type="button" role="tab" aria-selected={tab === "profits"} className={tab === "profits" ? "is-active" : ""} data-testid="entity-tax-tab-profits" onClick={() => setTab("profits")}>主体收入成本利润</button>
      </div>
      {tab === "profits" ? (
        <>
          <div className="admin-form-row" data-testid="entity-tax-profit-filter">
            <label>经营主体<select data-testid="entity-tax-profit-entity" value={profitQuery.businessEntityId} onChange={(event) => setProfitQuery({ ...profitQuery, businessEntityId: event.target.value })}>
              <option value="">请选择</option>
              {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
            </select></label>
            <label>起始月份<input data-testid="entity-tax-profit-start" type="month" value={profitQuery.startMonth} onChange={(event) => setProfitQuery({ ...profitQuery, startMonth: event.target.value })} /></label>
            <label>截止月份<input data-testid="entity-tax-profit-end" type="month" value={profitQuery.endMonth} onChange={(event) => setProfitQuery({ ...profitQuery, endMonth: event.target.value })} /></label>
          </div>
          {profit ? (
            <>
              <p data-testid="entity-tax-profit-basis">{PROFIT_BASIS_TEXT[profit.basis] ?? profit.basis}</p>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="entity-tax-profit-table">
                  <thead><tr><th>项目</th><th>金额（元）</th></tr></thead>
                  <tbody>
                    {profit.revenueItems.map((item) => (
                      <tr key={`revenue-${item.incomeType}`}><td>收入：{INCOME_LABELS[item.incomeType] ?? item.incomeType}</td><td className="admin-num" style={{ textAlign: "right" }}>{yuan(item.amountCent)}</td></tr>
                    ))}
                    <tr><td>收入合计</td><td className="admin-num" style={{ textAlign: "right" }}>{yuan(profit.revenueCent)}</td></tr>
                    {profit.costItems.map((item) => (
                      <tr key={`cost-${item.costType}`}><td>成本：{COST_LABELS[item.costType] ?? item.costType}</td><td className="admin-num" style={{ textAlign: "right" }}>{yuan(item.amountCent)}</td></tr>
                    ))}
                    <tr><td>成本合计</td><td className="admin-num" style={{ textAlign: "right" }}>{yuan(profit.costCent)}</td></tr>
                    <tr data-testid="entity-tax-profit-total"><td>利润</td><td className="admin-num" style={{ textAlign: "right" }}>{yuan(profit.profitCent)}</td></tr>
                  </tbody>
                </table>
              </div>
            </>
          ) : <p className="admin-muted-action">{profitReady ? "读取中…" : "请选择经营主体与起止月份（截止月份不能早于起始月份）。"}</p>}
        </>
      ) : tab === "rates" ? (
        <>
          <div className="admin-form-row">
            <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="entity-tax-rate-open" onClick={openRate}>＋ 提交税率版本</PermissionButton>
          </div>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="entity-tax-rate-table">
              <thead><tr><th>主体</th><th>收入类型</th><th>税种</th><th>有没有这笔费用</th><th>比例</th><th>生效起始日期</th><th>状态</th><th>审批</th></tr></thead>
              <tbody>
                {!rates?.length ? (
                  <tr><td colSpan={8} className="admin-muted-action">{rates === null ? "读取中…" : "暂无税率版本。"}</td></tr>
                ) : rates.map((r) => (
                  <tr key={r.id} data-testid={`entity-tax-rate-row-${r.id}`}>
                    <td>{r.businessEntityId ? r.entityName ?? entityName(r.businessEntityId) : "平台默认"}</td>
                    <td>{INCOME_LABELS[r.incomeType] ?? r.incomeType}</td>
                    <td>{TAX_LABELS[r.taxType] ?? r.taxType}</td>
                    <td>{r.applicable ? "有" : "没有"}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{r.applicable ? percent(r.rateBps) : "—"}</td>
                    <td>{r.effectiveFrom ?? "自始有效"}</td>
                    <td>{STATUS_LABELS[r.status] ?? r.status}</td>
                    <td>{r.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(r.reviewId)}`)}>查看审批链</a> : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
      <>
      <div className="admin-form-row" data-testid="entity-tax-filter">
        <label>经营主体<select data-testid="entity-tax-filter-entity" value={filter.businessEntityId} onChange={(event) => setFilter({ ...filter, businessEntityId: event.target.value })}>
          <option value="">全部主体</option>
          {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
        </select></label>
        <label>所属月<input data-testid="entity-tax-filter-month" type="month" value={filter.month} onChange={(event) => setFilter({ ...filter, month: event.target.value })} /></label>
        <label>税种<select data-testid="entity-tax-filter-type" value={filter.taxType} onChange={(event) => setFilter({ ...filter, taxType: event.target.value })}>
          <option value="">全部税种</option>
          {Object.entries(TAX_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>状态<select data-testid="entity-tax-filter-status" value={filter.status} onChange={(event) => setFilter({ ...filter, status: event.target.value })}>
          <option value="">全部状态</option>
          {Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="entity-tax-create-open" onClick={openCreate}>＋ 登记税费申报</PermissionButton>
      </div>
      {rows.error ? <div className="admin-form-error">{rows.error.message}</div> : null}
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="entity-tax-table">
          <thead><tr><th>经营主体</th><th>税种</th><th>所属期</th><th>实际申报金额（元）</th><th>系统计算金额（元）</th><th>申报差异（元）</th><th>差异处理</th><th>实际缴纳金额（元）</th><th>缴税差异（元）</th><th>差异调整单</th><th>状态</th><th>登记人</th><th>登记时间</th><th>审批</th><th>操作</th></tr></thead>
          <tbody>
            {rows.rows.length === 0 ? (
              <tr data-testid="entity-tax-table-empty"><td colSpan={15} className="admin-muted-action">{rows.loading ? "读取中…" : "当前筛选下暂无税费申报登记。"}</td></tr>
            ) : rows.rows.map((row) => (
              <tr key={row.id} data-testid={`entity-tax-row-${row.id}`}>
                <td>{row.entityName ?? entityName(row.businessEntityId)}</td>
                <td>{TAX_LABELS[row.taxType] ?? row.taxType}</td>
                <td>{row.periodStartMonth}～{row.periodEndMonth}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{yuan(row.declaredAmountCent)}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>
                  {row.systemCalculatedAmountCent == null ? "—" : yuan(row.systemCalculatedAmountCent)}
                  {row.calculationBasis?.length ? (
                    <details data-testid={`entity-tax-basis-${row.id}`} style={{ textAlign: "left" }}>
                      <summary>计算依据</summary>
                      <table className="admin-table">
                        <thead><tr><th>月份</th><th>收入类型</th><th>含税收入（元）</th><th>比例</th><th>税额（元）</th></tr></thead>
                        <tbody>
                          {row.calculationBasis.map((b, i) => (
                            <tr key={`${b.month}-${b.incomeType}-${i}`}>
                              <td>{b.month}</td>
                              <td>{INCOME_LABELS[b.incomeType] ?? b.incomeType}</td>
                              <td className="admin-num">{yuan(b.incomeCent)}</td>
                              <td>{percent(b.rateBps)}{b.vatAmountCent == null ? "" : `（以系统计算增值税 ${yuan(b.vatAmountCent)} 元为基数）`}
                                {b.profit ? `（以利润 ${yuan(Math.max(0, b.profit.profitCent))} 元为基数：收入 ${yuan(b.profit.revenueCent)} 元 − 成本 ${yuan(b.profit.costCent)} 元）` : ""}</td>
                              <td className="admin-num">{yuan(b.taxAmountCent)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </details>
                  ) : null}
                </td>
                <td className="admin-num" style={{ textAlign: "right" }}>{row.declarationDifferenceCent == null ? "—" : yuan(row.declarationDifferenceCent)}</td>
                <td>{mechanismText(row)}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{row.paidAmountCent == null ? "—" : yuan(row.paidAmountCent)}{row.paymentPending ? "（有新登记待审批）" : ""}</td>
                <td>{row.paymentDifferenceCent == null ? "—" : `${yuan(row.paymentDifferenceCent)}${row.paymentBreakdown?.length ? `（${breakdownText(row.paymentBreakdown)}）` : ""}`}</td>
                <td>{row.adjustments?.length ? row.adjustments.map((a) => <div key={a.id}>{adjustmentText(a)}</div>) : "—"}</td>
                <td>{STATUS_LABELS[row.status] ?? row.status}</td>
                <td>{row.createdBy ?? "—"}</td>
                <td>{cnDate(row.createdAt)}</td>
                <td>{row.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>查看审批链</a> : "—"}</td>
                <td>{row.status === "approved" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`entity-tax-payment-open-${row.id}`} onClick={() => openAct("payment", row)}>登记实际缴纳</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`entity-tax-adjust-open-${row.id}`} onClick={() => openAct("adjustment", row)}>生成差异调整单</PermissionButton>
                  </>
                ) : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <TablePager testid="entity-tax-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
      </div>
      </>
      )}

      <GuidedFlowModal
        open={rateOpen}
        title="提交税率版本"
        testId="entity-tax-rate-guided"
        submitTestId="entity-tax-rate-save"
        submitLabel="提交审批"
        submitting={busyAction === "entity_tax_rate_submitted"}
        error={formError || undefined}
        onClose={() => setRateOpen(false)}
        onSubmit={() => void submitRate()}
        steps={[
          {
            title: "税率版本",
            description: "每个主体 × 每类收入 × 每种税各自配「有没有这笔费用、收多少」；不选主体即平台默认（没有主体特例的都按它）。必须填写生效起始日期，此前发生的业务仍按原版本。企业所得税按全部收入配一个比例；其他税费不配比例，按季与财税软件核对、以财税软件为准。",
            validate: validateRate,
            content: (
              <>
                <label>主体<select data-testid="entity-tax-rate-entity" value={rateForm.businessEntityId} onChange={(event) => setRateForm({ ...rateForm, businessEntityId: event.target.value })}>
                  <option value="">平台默认</option>
                  {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
                </select></label>
                <label>税种<select data-testid="entity-tax-rate-tax" value={rateForm.taxType} onChange={(event) => setRateTax(event.target.value)}>
                  {RATE_TAX_TYPES.map((code) => <option key={code} value={code}>{TAX_LABELS[code]}</option>)}
                </select></label>
                <label>收入类型<select data-testid="entity-tax-rate-income" value={rateForm.incomeType} onChange={(event) => setRateForm({ ...rateForm, incomeType: event.target.value })}>
                  {rateForm.taxType === "corporate_income" ? <option value="all">{INCOME_LABELS.all}</option> : (
                    <>
                      <option value="">请选择</option>
                      {RATE_INCOME_TYPES.map((code) => <option key={code} value={code}>{INCOME_LABELS[code]}</option>)}
                    </>
                  )}
                </select></label>
                <label><input data-testid="entity-tax-rate-applicable" type="checkbox" checked={rateForm.applicable} onChange={(event) => setRateForm({ ...rateForm, applicable: event.target.checked })} />有这笔费用</label>
                {rateForm.applicable ? <label>比例（%）<input data-testid="entity-tax-rate-percent" type="number" step="0.01" min="0" max="100" value={rateForm.percent} onChange={(event) => setRateForm({ ...rateForm, percent: event.target.value })} /></label> : null}
                <label>生效起始日期<input data-testid="entity-tax-rate-from" type="date" value={rateForm.effectiveFrom} onChange={(event) => setRateForm({ ...rateForm, effectiveFrom: event.target.value })} /></label>
                <label>原因<textarea data-testid="entity-tax-rate-reason" value={rateForm.reason} onChange={(event) => setRateForm({ ...rateForm, reason: event.target.value })} placeholder="如：依据某税务文件调整餐饮增值税税率" /></label>
              </>
            ),
          },
          {
            title: "审批线",
            description: "财务、税务、法务三条线各填最终审批人（员工账号编号，须在该线起点人的直接上级链上）；三线通过后由顶层平台公司最终确认。",
            validate: validateReviewers,
            content: reviewerInputs("entity-tax-rate"),
          },
        ]}
        summary={[
          { label: "主体", value: rateForm.businessEntityId ? entityName(rateForm.businessEntityId) : "平台默认" },
          { label: "收入类型", value: rateForm.incomeType ? INCOME_LABELS[rateForm.incomeType] ?? rateForm.incomeType : "未选择" },
          { label: "税种", value: TAX_LABELS[rateForm.taxType] ?? rateForm.taxType },
          { label: "有没有这笔费用", value: rateForm.applicable ? "有" : "没有" },
          { label: "比例", value: rateForm.applicable ? `${rateForm.percent || "—"}%` : "—" },
          { label: "生效起始日期", value: rateForm.effectiveFrom || "未填写" },
          { label: "原因", value: rateForm.reason.trim() || "未填写" },
          { label: "生效条件", value: "财务、税务、法务全部通过后，由顶层平台公司最终确认" },
        ]}
      />

      <GuidedFlowModal
        open={open}
        title="登记税费实际申报金额"
        testId="entity-tax-create-guided"
        submitTestId="entity-tax-create-save"
        submitLabel="登记并提交审批"
        submitting={busyAction === "entity_tax_declared"}
        error={formError || undefined}
        onClose={() => setOpen(false)}
        onSubmit={() => void submit()}
        steps={[
          {
            title: "申报信息",
            description: "按税务申报表填写：所属期按月，按季申报的整季登记一条（首月～末月）。同一主体、税种、所属期在待审批或已生效时不能重复登记。",
            validate: validateDeclaration,
            content: (
              <>
                <label>经营主体<select data-testid="entity-tax-create-entity" value={form.businessEntityId} onChange={(event) => setForm({ ...form, businessEntityId: event.target.value })}>
                  <option value="">请选择</option>
                  {entities.map((e) => <option key={e.id} value={e.id}>{e.entityName ?? e.id}</option>)}
                </select></label>
                <label>税种<select data-testid="entity-tax-create-type" value={form.taxType} onChange={(event) => setForm({ ...form, taxType: event.target.value })}>
                  {Object.entries(TAX_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>所属期首月<input data-testid="entity-tax-create-start" type="month" value={form.periodStartMonth} onChange={(event) => setForm({ ...form, periodStartMonth: event.target.value })} /></label>
                <label>所属期末月<input data-testid="entity-tax-create-end" type="month" value={form.periodEndMonth} onChange={(event) => setForm({ ...form, periodEndMonth: event.target.value })} /></label>
                <label>实际申报金额（元）<input data-testid="entity-tax-create-amount" type="number" step="0.01" min="0" value={form.amountYuan} onChange={(event) => setForm({ ...form, amountYuan: event.target.value })} /></label>
                <label>登记原因<textarea data-testid="entity-tax-create-reason" value={form.reason} onChange={(event) => setForm({ ...form, reason: event.target.value })} placeholder="如：2026 年一季度增值税申报表" /></label>
              </>
            ),
          },
          {
            title: "审批线",
            description: "财务、税务、法务三条线各填最终审批人（员工账号编号，须在该线起点人的直接上级链上）；三线通过后由顶层平台公司最终确认。",
            validate: validateReviewers,
            content: reviewerInputs("entity-tax-create"),
          },
        ]}
        summary={[
          { label: "经营主体", value: form.businessEntityId ? entityName(form.businessEntityId) : "未选择" },
          { label: "税种", value: TAX_LABELS[form.taxType] ?? form.taxType },
          { label: "所属期", value: `${form.periodStartMonth || "—"}～${form.periodEndMonth || "—"}` },
          { label: "实际申报金额", value: form.amountYuan ? `¥${form.amountYuan}` : "未填写" },
          { label: "登记原因", value: form.reason.trim() || "未填写" },
          { label: "生效条件", value: "财务、税务、法务全部通过后，由顶层平台公司最终确认" },
        ]}
      />

      {act ? (
        <GuidedFlowModal
          open
          title={act.mode === "payment" ? "登记实际缴纳金额" : "生成差异调整单"}
          testId={act.mode === "payment" ? "entity-tax-payment-guided" : "entity-tax-adjust-guided"}
          submitTestId={act.mode === "payment" ? "entity-tax-payment-save" : "entity-tax-adjust-save"}
          submitLabel="登记并提交审批"
          submitting={busyAction === "entity_tax_payment_registered" || busyAction === "entity_tax_adjustment_registered"}
          error={formError || undefined}
          onClose={() => setAct(null)}
          onSubmit={() => void submitAct()}
          steps={[
            {
              title: act.mode === "payment" ? "实际缴纳" : "更正差额",
              description: act.mode === "payment"
                ? "按完税凭证填写该申报截至本次累计实际缴纳的金额。缴税差异＝实际缴纳−实际申报（含已生效差异调整单）；不为 0 时按未缴、预缴、多缴、抵扣、待退分别填写，合计等于差异，不计入税费成本。"
                : "原期间已申报的不改原申报，在当前开放期间生成关联原所属期的差异调整单：填更正差额（多申报的填负数），生效后计入批准当月所在期的分红应扣税费。",
              validate: validateAct,
              content: (
                <>
                  <label>{act.mode === "payment" ? "实际缴纳金额（元）" : "更正差额（元）"}<input data-testid="entity-tax-act-amount" type="number" step="0.01" value={actForm.amountYuan} onChange={(event) => setActForm({ ...actForm, amountYuan: event.target.value })} /></label>
                  {actDifference !== null ? <p data-testid="entity-tax-act-difference">缴税差异＝实际缴纳 {yuan(cents(actForm.amountYuan))} − 实际申报（含已生效差异调整单）{yuan(actDeclared)} ＝ {yuan(actDifference)} 元</p> : null}
                  {actDifference ? CATEGORY_CODES.map((code) => (
                    <label key={code}>{CATEGORY_LABELS[code]}（元）<input data-testid={`entity-tax-act-category-${code}`} type="number" step="0.01" min="0" value={actForm.categories[code] ?? ""} onChange={(event) => setActForm({ ...actForm, categories: { ...actForm.categories, [code]: event.target.value } })} /></label>
                  )) : null}
                  <label>{act.mode === "payment" ? "登记原因" : "更正原因"}<textarea data-testid="entity-tax-act-reason" value={actForm.reason} onChange={(event) => setActForm({ ...actForm, reason: event.target.value })} /></label>
                </>
              ),
            },
            {
              title: "审批线",
              description: "财务、税务、法务三条线各填最终审批人（员工账号编号，须在该线起点人的直接上级链上）；三线通过后由顶层平台公司最终确认。",
              validate: validateReviewers,
              content: reviewerInputs("entity-tax-act"),
            },
          ]}
          summary={[
            { label: "经营主体", value: act.row.entityName ?? entityName(act.row.businessEntityId) },
            { label: "税种", value: TAX_LABELS[act.row.taxType] ?? act.row.taxType },
            { label: act.mode === "payment" ? "所属期" : "关联原所属期", value: `${act.row.periodStartMonth}～${act.row.periodEndMonth}` },
            ...(act.mode === "payment" ? [
              { label: "实际申报金额（含已生效差异调整单）", value: `¥${yuan(actDeclared)}` },
              { label: "实际缴纳金额", value: `¥${actForm.amountYuan}` },
              { label: "缴税差异", value: actDifference ? `${yuan(actDifference)} 元（${breakdownText(actBreakdown)}）` : "0.00 元" },
            ] : [
              { label: "更正差额", value: `¥${actForm.amountYuan}（生效后计入批准当月）` },
            ]),
            { label: "原因", value: actForm.reason.trim() || "未填写" },
            { label: "生效条件", value: "财务、税务、法务全部通过后，由顶层平台公司最终确认" },
          ]}
        />
      ) : null}
    </section>
  );
}
