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
  MerchantAdmissionRow,
  MerchantExpenseEntryRow,
  MerchantExpenseSummaryRow,
} from "../types";

// 商户费用归集台账（持股分红净利润制费用侧）：能耗/物业费自动挂接 + 人工/摊销/其他手工录入，
// 更正=红字冲销新行（资金类仅追加）。权限：view 只读、manage 录入/冲销/自动挂接。
const VIEW_PERMS = ["finance.merchant_expense.view"];
const MANAGE_PERMS = ["finance.merchant_expense.manage"];
const PAGE_SIZE = 20;

const EXPENSE_TYPE_LABELS: Record<string, string> = {
  rent_allocation: "房租分摊",
  energy: "能耗费",
  property_fee: "物业费",
  labor: "人工",
  amortization: "摊销",
  other: "其他",
};

const SOURCE_LABELS: Record<string, string> = {
  auto_energy: "自动·能耗账单",
  auto_property: "自动·物业费账单",
  manual: "手工录入",
};

const TEXT = {
  title: "商户费用台账",
  description:
    "把「这家店这个月花了多少钱」归集成台账：能耗费/物业费按期自动挂接系统已出账单，人工/摊销/其他手工录入（理由必填）。台账仅追加，更正走红字冲销；期间汇总是分红净利润基数的费用侧取数点。",
  loadFailed: "商户费用台账读取失败",
  summaryFailed: "费用期间汇总读取失败",
  createFailed: "费用录入失败",
  reverseFailed: "红字冲销失败",
  syncFailed: "自动挂接失败",
  merchantRequired: "请选择费用归属商户。",
  periodRequired: "请填写费用月份（如 2026-07）。",
  amountRequired: "金额必须是非零数字（元，可为负仅限冲销场景请走冲销）。",
  noteRequired: "理由必填（进入审计留痕）。",
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

export function FinanceMerchantExpensePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [summary, setSummary] = React.useState<MerchantExpenseSummaryRow[]>([]);
  const [summaryNotice, setSummaryNotice] = React.useState<string | null>(null);

  // 筛选（即选即查）：商户 / 月份 / 类型 / 来源。月份默认当月（期间汇总必须有月份口径）。
  const [merchantFilter, setMerchantFilter] = React.useState("");
  const [periodFilter, setPeriodFilter] = React.useState(() => new Date().toISOString().slice(0, 7));
  const [typeFilter, setTypeFilter] = React.useState("");
  const [sourceFilter, setSourceFilter] = React.useState("");

  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [reverseTarget, setReverseTarget] = React.useState<MerchantExpenseEntryRow | null>(null);

  // 手工录入表单
  const [formMerchant, setFormMerchant] = React.useState("");
  const [formPeriod, setFormPeriod] = React.useState("");
  const [formType, setFormType] = React.useState("labor");
  const [formAmountYuan, setFormAmountYuan] = React.useState("");
  const [formNote, setFormNote] = React.useState("");
  // 冲销理由
  const [reverseNote, setReverseNote] = React.useState("");

  const merchantsResource = useAdminResource(
    async () => {
      const rows = await api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants");
      setMerchants(rows);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<MerchantExpenseEntryRow>> => {
      const params = new URLSearchParams({ page: String(page), page_size: String(size) });
      if (merchantFilter) params.set("merchant_id", merchantFilter);
      if (periodFilter) params.set("period", periodFilter);
      if (typeFilter) params.set("expense_type", typeFilter);
      if (sourceFilter) params.set("source", sourceFilter);
      return api.getPaged<MerchantExpenseEntryRow>(`/api/v1/finance/merchant-expenses?${params.toString()}`);
    },
    [api, merchantFilter, periodFilter, typeFilter, sourceFilter],
  );

  const rows = useServerPagedRows<MerchantExpenseEntryRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: TEXT.loadFailed,
  });

  // 期间汇总卡：随月份 + 商户筛选刷新（类型/来源筛选不影响汇总口径——汇总始终是该期全类型合计）。
  const loadSummary = React.useCallback(async () => {
    if (!periodFilter) {
      setSummary([]);
      setSummaryNotice("填写月份后显示该期分类型费用合计。");
      return;
    }
    try {
      const params = new URLSearchParams({ period: periodFilter });
      if (merchantFilter) params.set("merchant_id", merchantFilter);
      const data = await api.get<MerchantExpenseSummaryRow[]>(`/api/v1/finance/merchant-expenses/summary?${params.toString()}`);
      setSummary(data);
      setSummaryNotice(null);
    } catch (summaryError) {
      setSummary([]);
      setSummaryNotice(summaryError instanceof Error ? summaryError.message : TEXT.summaryFailed);
    }
  }, [api, periodFilter, merchantFilter]);

  React.useEffect(() => {
    void loadSummary();
  }, [loadSummary]);

  // 筛选条件变化 → 列表回第 1 页重取。
  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [merchantFilter, periodFilter, typeFilter, sourceFilter]);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: async () => {
      rows.reload();
      await loadSummary();
    },
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.createFailed,
  });

  function openCreate() {
    setFormError("");
    setFormMerchant(merchantFilter || merchants[0]?.id || "");
    setFormPeriod(periodFilter);
    setFormType("labor");
    setFormAmountYuan("");
    setFormNote("");
    setCreateOpen(true);
  }

  function validateCreate(): string | null {
    if (!formMerchant) return TEXT.merchantRequired;
    if (!formPeriod.trim()) return TEXT.periodRequired;
    const amount = Number(formAmountYuan);
    if (!Number.isFinite(amount) || amount === 0) return TEXT.amountRequired;
    if (!formNote.trim()) return TEXT.noteRequired;
    return null;
  }

  async function submitCreate() {
    const invalid = validateCreate();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-expense"),
      merchantId: formMerchant,
      period: formPeriod.trim(),
      expenseType: formType,
      amountCent: Math.round(Number(formAmountYuan) * 100),
      note: formNote.trim(),
    };
    const ok = await runAction("merchant_expense_created", request,
      () => api.post("/api/v1/finance/merchant-expenses", request));
    if (ok) {
      setCreateOpen(false);
    }
  }

  function openReverse(row: MerchantExpenseEntryRow) {
    setFormError("");
    setReverseNote("");
    setReverseTarget(row);
  }

  async function submitReverse() {
    if (!reverseTarget) return;
    if (!reverseNote.trim()) {
      setFormError(TEXT.noteRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-expense-reverse"), note: reverseNote.trim() };
    const ok = await runAction("merchant_expense_reversed", request,
      () => api.post(`/api/v1/finance/merchant-expenses/${encodeURIComponent(reverseTarget.id)}/reverse`, request));
    if (ok) {
      setReverseTarget(null);
    }
  }

  async function runSync() {
    if (!periodFilter) {
      setFormError(TEXT.periodRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-expense-sync"), period: periodFilter };
    await runAction("merchant_expense_auto_synced", request,
      () => api.post("/api/v1/finance/merchant-expenses/sync-auto", request));
  }

  function openDetail(row: MerchantExpenseEntryRow) {
    const merchant = merchants.find((m) => m.id === row.merchantId);
    setDetail({
      title: `费用台账明细 · ${row.id}`,
      fields: [
        { label: "商户", value: merchant ? merchantLabel(merchant) : row.merchantId ?? "—" },
        { label: "费用月份", value: row.period ?? "—" },
        { label: "费用类型", value: EXPENSE_TYPE_LABELS[row.expenseType ?? ""] ?? row.expenseType ?? "—" },
        { label: "金额（元）", value: `¥${centToYuan(row.amountCent)}` },
        { label: "来源", value: SOURCE_LABELS[row.source ?? ""] ?? row.source ?? "—" },
        { label: "来源单据/原行", value: row.sourceRef ?? "—" },
        { label: "备注/理由", value: row.note ?? "—" },
        { label: "操作人", value: row.operatorUserId ?? "—" },
        { label: "记账时间", value: cnDate(row.createdAt) },
        { label: "台账行号", value: row.id },
      ],
    });
  }

  // 冲销行（manual + source_ref）不允许再冲销，按钮不渲染。
  function reversible(row: MerchantExpenseEntryRow): boolean {
    return !(row.source === "manual" && row.sourceRef);
  }

  const summaryTotal = summary.reduce((acc, row) => acc + (row.totalCent ?? 0), 0);
  const merchantOptions = merchants.filter((m) => m.status === "active" || m.status === undefined);
  const empty = !rows.loading && rows.total === 0 && !merchantFilter && !typeFilter && !sourceFilter;

  return (
    <section data-testid="finance-merchant-expense-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_expense.manual_created / merchant_expense.reversed / merchant_expense.auto_synced" />

      {/* ── 筛选 + 动作 ─────────────────────────────────────── */}
      <div className="admin-form-row" data-testid="merchant-expense-filter">
        <label>商户<select data-testid="merchant-expense-filter-merchant" value={merchantFilter} onChange={(event) => setMerchantFilter(event.target.value)}>
          <option value="">全部商户</option>
          {merchantOptions.map((m) => <option key={m.id} value={m.id}>{merchantLabel(m)}</option>)}
        </select></label>
        <label>月份<input data-testid="merchant-expense-filter-period" type="month" value={periodFilter} onChange={(event) => setPeriodFilter(event.target.value)} /></label>
        <label>类型<select data-testid="merchant-expense-filter-type" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)}>
          <option value="">全部类型</option>
          {Object.entries(EXPENSE_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>来源<select data-testid="merchant-expense-filter-source" value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
          <option value="">全部来源</option>
          {Object.entries(SOURCE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="merchant-expense-create-open"
          onClick={openCreate}
        >＋ 手工录入费用</PermissionButton>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="merchant-expense-sync-run"
          disabled={busyAction === "merchant_expense_auto_synced"}
          onClick={runSync}
        >自动挂接能耗/物业费</PermissionButton>
        <span className="admin-muted-action">自动挂接把所选月份已出账的能耗费应收与物业费账单汇入台账，重跑不重复计入。</span>
      </div>

      {/* ── 期间汇总卡（分红引擎费用侧取数口径）───────────────── */}
      <h3>期间费用汇总{periodFilter ? `（${periodFilter}）` : ""}</h3>
      {summaryNotice ? (
        <p className="admin-muted-action" data-testid="merchant-expense-summary-notice">{summaryNotice}</p>
      ) : (
        <div className="admin-form-row" data-testid="merchant-expense-summary">
          {summary.length === 0 ? (
            <span className="admin-muted-action" data-testid="merchant-expense-summary-empty">该期暂无费用记录。</span>
          ) : (
            <>
              {summary.map((row) => (
                <span key={`${row.merchantId}-${row.expenseType}`} className="admin-muted-action" data-testid={`merchant-expense-summary-${row.merchantId}-${row.expenseType}`}>
                  {(merchants.find((m) => m.id === row.merchantId) ? merchantLabel(merchants.find((m) => m.id === row.merchantId)!) : row.merchantId)}
                  · {EXPENSE_TYPE_LABELS[row.expenseType ?? ""] ?? row.expenseType}：¥{centToYuan(row.totalCent)}
                </span>
              ))}
              <strong data-testid="merchant-expense-summary-total">合计：¥{centToYuan(summaryTotal)}</strong>
            </>
          )}
        </div>
      )}

      {/* ── 台账列表 ─────────────────────────────────────────── */}
      <AdminDataState loading={rows.loading || merchantsResource.loading} error={rows.error ?? merchantsResource.error} empty={empty} onRetry={rows.reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-expense-table">
            <thead><tr><th>月份</th><th>商户</th><th>类型</th><th>金额（元）</th><th>来源</th><th>备注/理由</th><th>记账时间</th><th>操作</th></tr></thead>
            <tbody>
              {rows.rows.length === 0 ? (
                <tr data-testid="merchant-expense-table-empty"><td colSpan={8} className="admin-muted-action">当前筛选下暂无费用记录。</td></tr>
              ) : rows.rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-expense-row-${row.id}`}
                  onClick={() => openDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDetail(row); } }}>
                  <td>{row.period ?? "—"}</td>
                  <td>{merchants.find((m) => m.id === row.merchantId) ? merchantLabel(merchants.find((m) => m.id === row.merchantId)!) : row.merchantId ?? "—"}</td>
                  <td>{EXPENSE_TYPE_LABELS[row.expenseType ?? ""] ?? row.expenseType ?? "—"}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{centToYuan(row.amountCent)}</td>
                  <td>{SOURCE_LABELS[row.source ?? ""] ?? row.source ?? "—"}</td>
                  <td>{row.note ?? "—"}</td>
                  <td>{cnDate(row.createdAt)}</td>
                  <td className="admin-row-detail-cell" onClick={(event) => event.stopPropagation()}>
                    <button type="button" className="admin-row-detail-btn" aria-label="查看费用台账明细" onClick={(event) => { event.stopPropagation(); openDetail(row); }}>查看详情</button>
                    {reversible(row) ? (
                      <PermissionButton
                        permissionsState={effectivePermissions}
                        permissions={MANAGE_PERMS}
                        data-testid={`merchant-expense-reverse-open-${row.id}`}
                        onClick={() => openReverse(row)}
                      >红字冲销</PermissionButton>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-expense-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
        </div>
      </AdminDataState>

      {formError && !createOpen && !reverseTarget ? (
        <div className="admin-form-error" data-testid="merchant-expense-form-error">{formError}</div>
      ) : null}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />

      {/* ── 手工录入（引导式）─────────────────────────────────── */}
      <GuidedFlowModal
        open={createOpen}
        title="手工录入商户费用"
        testId="merchant-expense-create-guided"
        submitTestId="merchant-expense-create-save"
        submitLabel="确认录入"
        submitting={busyAction === "merchant_expense_created"}
        error={formError || undefined}
        onClose={() => setCreateOpen(false)}
        onSubmit={submitCreate}
        steps={[
          {
            title: "费用信息",
            description: "适用于人工/摊销/其他等系统无单据的费用；能耗费与物业费请用「自动挂接」从已出账单汇入。金额按元填写，可为负数（异常调整建议优先走红字冲销）。",
            validate: () => {
              if (!formMerchant) return TEXT.merchantRequired;
              if (!formPeriod.trim()) return TEXT.periodRequired;
              const amount = Number(formAmountYuan);
              if (!Number.isFinite(amount) || amount === 0) return TEXT.amountRequired;
              return null;
            },
            content: (
              <>
                <label>归属商户<select data-testid="merchant-expense-create-merchant" value={formMerchant} onChange={(event) => setFormMerchant(event.target.value)}>
                  {merchantOptions.length === 0 ? <option value="">（暂无商户）</option> : null}
                  {merchantOptions.map((m) => <option key={m.id} value={m.id}>{merchantLabel(m)}</option>)}
                </select></label>
                <label>费用月份<input data-testid="merchant-expense-create-period" type="month" value={formPeriod} onChange={(event) => setFormPeriod(event.target.value)} /></label>
                <label>费用类型<select data-testid="merchant-expense-create-type" value={formType} onChange={(event) => setFormType(event.target.value)}>
                  {Object.entries(EXPENSE_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>金额（元）<input data-testid="merchant-expense-create-amount" type="number" step="0.01" value={formAmountYuan} onChange={(event) => setFormAmountYuan(event.target.value)} placeholder="如 12000.00" /></label>
              </>
            ),
          },
          {
            title: "录入理由（必填）",
            description: "理由与操作人一并进入审计留痕；台账仅追加，录错请用红字冲销更正。",
            validate: () => (formNote.trim() ? null : TEXT.noteRequired),
            content: (
              <label>理由<textarea data-testid="merchant-expense-create-note" value={formNote} onChange={(event) => setFormNote(event.target.value)} placeholder="如：2026-07 店员工资及社保分摊" /></label>
            ),
          },
        ]}
        summary={[
          { label: "归属商户", value: merchantOptions.find((m) => m.id === formMerchant) ? merchantLabel(merchantOptions.find((m) => m.id === formMerchant)!) : (formMerchant || "未选择") },
          { label: "费用月份", value: formPeriod || "未填写" },
          { label: "费用类型", value: EXPENSE_TYPE_LABELS[formType] ?? formType },
          { label: "金额", value: formAmountYuan ? `¥${formAmountYuan}` : "未填写" },
          { label: "理由", value: formNote.trim() || "未填写" },
        ]}
      />

      {/* ── 红字冲销（引导式）─────────────────────────────────── */}
      <GuidedFlowModal
        open={reverseTarget !== null}
        title={`红字冲销 · ${reverseTarget?.id ?? ""}`}
        testId="merchant-expense-reverse-guided"
        submitTestId="merchant-expense-reverse-save"
        submitLabel="确认冲销"
        submitting={busyAction === "merchant_expense_reversed"}
        error={formError || undefined}
        onClose={() => setReverseTarget(null)}
        onSubmit={submitReverse}
        steps={[
          {
            title: "冲销理由（必填）",
            description: `将生成金额 ¥${centToYuan(reverseTarget ? -(reverseTarget.amountCent ?? 0) : 0)} 的红字新行冲抵原行，不改历史行；每行仅允许一次全额冲销。`,
            validate: () => (reverseNote.trim() ? null : TEXT.noteRequired),
            content: (
              <label>理由<textarea data-testid="merchant-expense-reverse-note" value={reverseNote} onChange={(event) => setReverseNote(event.target.value)} placeholder="如：金额录错，冲销后重新录入" /></label>
            ),
          },
        ]}
        summary={[
          { label: "原台账行", value: reverseTarget?.id ?? "—" },
          { label: "原金额", value: `¥${centToYuan(reverseTarget?.amountCent)}` },
          { label: "冲销金额", value: `¥${centToYuan(reverseTarget ? -(reverseTarget.amountCent ?? 0) : 0)}` },
          { label: "理由", value: reverseNote.trim() || "未填写" },
        ]}
      />
    </section>
  );
}
