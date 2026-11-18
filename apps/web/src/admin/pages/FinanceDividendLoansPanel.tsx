// 文件版本：V0.01
import React from "react";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, BusinessEntityRow, DividendBarLoanRow, MerchantAdmissionRow } from "../types";

// 第十八批 AX 段第 273 条 273-R3（PGM:436「任何公司向酒吧提供的借款，出借时须写明金额、利率、还款期限与还款顺序；酒吧当期可分配利润必须先偿还全部到期借款
// 本息，剩余部分才可用于分红，存在逾期未还借款的一律不得分红，分红方案提交时须附借款清偿情况」；BSM:1785）：持股分红页的酒吧借款——列出各酒吧登记的借款与
// 未还本息、状态；登记借款（酒吧、出借方、本金、年利率、到期应付利息、出借日、还款期限、还款顺序、备注、凭证）同时挂一笔酒吧应付，还款在应收应付台账走付款确认
// 与登记付款。写操作要 finance.dividend.manage。
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult" | "setGlobalError"> & { merchants: MerchantAdmissionRow[] };

const MANAGE_PERMS = ["finance.dividend.manage"];
// 同后端 DividendLoanService.Loan#withStatus（按该酒吧当前营业日判）。
export const LOAN_STATUS_LABELS: Record<string, string> = { not_due: "未到期", due: "已到期未还", overdue: "逾期未还", repaid: "已还清" };
const EMPTY_FORM = {
  merchantId: "", lenderEntityId: "", lenderName: "", principalYuan: "", ratePercent: "", interestYuan: "", lentOn: "", dueDate: "",
  repayOrder: "1", note: "",
};
const TEXT = {
  loadFailed: "酒吧借款读取失败",
  required: "请选择酒吧，选择出借方主体或填写出借方名称，并填写本金、年利率、到期应付利息、出借日、还款期限与还款顺序。",
  invalid: "本金须大于 0，年利率在 0～100 之间，利息不小于 0，还款顺序为不小于 1 的整数，还款期限不早于出借日。",
};

const yuan = (cent?: number | null) => ((cent ?? 0) / 100).toFixed(2);
// 元转分、百分数转万分比都是乘 100。
const times100 = (text: string) => {
  const value = Number(text);
  return text.trim() === "" || !Number.isFinite(value) || value < 0 ? null : Math.round(value * 100);
};
const nextKey = (prefix: string) => `${prefix}-${Date.now()}`;

/** 计算单详情「借款清偿情况」：该酒吧登记的借款逐笔列出借方、到期日、未还本息与状态。 */
export function loanRepaymentText(loans: DividendBarLoanRow[]): string {
  if (loans.length === 0) return "该酒吧无登记借款";
  return loans.map((loan) => `${loan.lenderName ?? "—"}：到期 ${loan.dueDate ?? "—"}，未还本息 ¥${yuan(loan.outstandingCent)}，`
    + `${LOAN_STATUS_LABELS[loan.status ?? ""] ?? "状态待核实"}`).join("；");
}

/** 结果区只放借款本身。 */
function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  const row = (result ?? {}) as Partial<DividendBarLoanRow>;
  return { action, idempotencyKey: request.idempotencyKey, result: { id: row.id, merchantId: row.merchantId, dueDate: row.dueDate, outstandingCent: row.outstandingCent } };
}

export function FinanceDividendLoansPanel({ api, effectivePermissions, setActionResult, setGlobalError, merchants }: Props) {
  const [loans, setLoans] = React.useState<DividendBarLoanRow[]>([]);
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [form, setForm] = React.useState(EMPTY_FORM);
  const [files, setFiles] = React.useState<string[]>([]);

  const resource = useAdminResource(
    async () => {
      const rows = await api.get<DividendBarLoanRow[]>("/api/v1/finance/dividends/loans");
      setLoans(Array.isArray(rows) ? rows : []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
  );
  // 出借方主体下拉（读不到不阻断：外部公司直接填名称）。
  React.useEffect(() => {
    api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities")
      .then((rows) => setEntities(Array.isArray(rows) ? rows : []))
      .catch(() => setEntities([]));
  }, [api]);
  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: resource.reload, setGlobalError, setActionResult, makeResult, fallbackMessage: TEXT.loadFailed,
  });

  const bars = merchants.filter((merchant) => merchant.merchantType === "bar");
  const merchantName = (id?: string) => {
    const merchant = merchants.find((row) => row.id === id);
    return merchant?.merchantName ?? id ?? "—";
  };

  async function upload(selected: FileList | null) {
    if (!selected?.length) return;
    try {
      if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
      for (const file of Array.from(selected)) {
        const data = new FormData();
        data.append("file", file);
        data.append("business_type", "dividend_bar_loan");
        const row = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data);
        setFiles((old) => [...new Set([...old, row.id])]);
      }
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "凭证上传失败");
    }
  }

  async function register() {
    const merchantId = form.merchantId || bars[0]?.id || "";
    if (!merchantId || (!form.lenderEntityId && !form.lenderName.trim())
        || [form.principalYuan, form.ratePercent, form.interestYuan, form.lentOn, form.dueDate, form.repayOrder].some((value) => !value.trim())) {
      return setFormError(TEXT.required);
    }
    const principalCent = times100(form.principalYuan);
    const annualRateBps = times100(form.ratePercent);
    const interestCent = times100(form.interestYuan);
    const repayOrder = Number(form.repayOrder);
    if (!principalCent || annualRateBps === null || annualRateBps > 10000 || interestCent === null || !Number.isInteger(repayOrder)
        || repayOrder < 1 || form.dueDate < form.lentOn) {
      return setFormError(TEXT.invalid);
    }
    const request = {
      idempotencyKey: nextKey("dividend-bar-loan"), merchantId, lenderEntityId: form.lenderEntityId || null, lenderName: form.lenderName.trim() || null,
      principalCent, annualRateBps, interestCent, lentOn: form.lentOn, dueDate: form.dueDate, repayOrder, note: form.note.trim() || null,
      evidenceAttachmentIds: files,
    };
    if (await runAction("dividend_bar_loan_registered", request, () => api.post("/api/v1/finance/dividends/loans", request))) {
      setForm(EMPTY_FORM);
      setFiles([]);
    }
  }

  const input = (key: keyof typeof EMPTY_FORM, label: string, testId: string, type = "text") => (
    <label>{label}<input data-testid={testId} type={type} value={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.value })} /></label>
  );

  return (
    <section data-testid="dividend-loan-panel" aria-label="酒吧借款（分红前置）">
      <h3>酒吧借款（分红前置）</h3>
      <p className="admin-muted-action">公司向酒吧提供的借款须登记金额、利率、还款期限与还款顺序。每期出单先用可分配利润偿还已到期未还的借款本息，剩余部分才分红；有逾期未还借款的计算单不能提交、审批。登记时同时挂一笔该酒吧的应付，还款在应收应付台账走付款确认与登记付款。</p>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="dividend-loan-table">
          <thead><tr><th>酒吧</th><th>出借方</th><th>本金（元）</th><th>年利率</th><th>到期应付利息（元）</th><th>出借日</th><th>到期日</th><th>还款顺序</th><th>未还本息（元）</th><th>状态</th><th>台账单号</th></tr></thead>
          <tbody>{loans.length === 0 ? (
            <tr data-testid="dividend-loan-empty"><td colSpan={11} className="admin-muted-action">暂无登记借款。</td></tr>
          ) : loans.map((loan) => (
            <tr key={loan.id} data-testid={`dividend-loan-row-${loan.id}`}>
              <td>{merchantName(loan.merchantId)}</td>
              <td>{loan.lenderName ?? "—"}</td>
              <td>{yuan(loan.principalCent)}</td>
              <td>{`${((loan.annualRateBps ?? 0) / 100).toFixed(2)}%`}</td>
              <td>{yuan(loan.interestCent)}</td>
              <td>{loan.lentOn ?? "—"}</td>
              <td>{loan.dueDate ?? "—"}</td>
              <td>{loan.repayOrder ?? "—"}</td>
              <td>{yuan(loan.outstandingCent)}</td>
              <td>{LOAN_STATUS_LABELS[loan.status ?? ""] ?? "状态待核实"}</td>
              <td>{loan.itemNo ?? loan.arApItemId ?? "—"}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-row" data-testid="dividend-loan-form">
        <label>酒吧<select data-testid="dividend-loan-merchant" value={form.merchantId || bars[0]?.id || ""} onChange={(event) => setForm({ ...form, merchantId: event.target.value })}>
          {bars.map((bar) => <option key={bar.id} value={bar.id}>{merchantName(bar.id)}</option>)}
        </select></label>
        <label>出借方主体<select data-testid="dividend-loan-lender-entity" value={form.lenderEntityId} onChange={(event) => setForm({ ...form, lenderEntityId: event.target.value })}>
          <option value="">（外部公司，填名称）</option>
          {entities.map((entity) => <option key={entity.id} value={entity.id}>{entity.entityName ?? entity.id}</option>)}
        </select></label>
        {input("lenderName", "出借方名称", "dividend-loan-lender-name")}
        {input("principalYuan", "本金（元）", "dividend-loan-principal", "number")}
        {input("ratePercent", "年利率（%）", "dividend-loan-rate", "number")}
        {input("interestYuan", "到期应付利息（元）", "dividend-loan-interest", "number")}
        {input("lentOn", "出借日", "dividend-loan-lent-on", "date")}
        {input("dueDate", "还款期限", "dividend-loan-due-date", "date")}
        {input("repayOrder", "还款顺序", "dividend-loan-repay-order", "number")}
        {input("note", "备注", "dividend-loan-note")}
        <label>凭证<input data-testid="dividend-loan-evidence" type="file" multiple onChange={(event) => void upload(event.target.files)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS} disabled={busyAction !== ""}
          data-testid="dividend-loan-register" onClick={() => void register()}>登记借款</PermissionButton>
      </div>
      {formError ? <p role="alert" className="admin-inline-error" data-testid="dividend-loan-error">{formError}</p> : null}
    </section>
  );
}
