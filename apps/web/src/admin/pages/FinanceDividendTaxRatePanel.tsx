// 文件版本：V0.01
import React from "react";
import { withBase } from "../../basePath";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, DividendTaxRateRow } from "../types";

// 第十八批 AX 段第 271 条 271-R2（BSM:1776「其分得的款项按「股息红利」计税，由发放的经营主体代扣个人所得税（安全阈值，全平台统一可配，调整须经财务、
// 税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限）」「税率以参数形式配置，不在业务页面硬编码」）：持股分红页的分红代扣个税税率——
// 列出全平台统一的税率版本；提交新税率（税率、上下限、生效起始日期、理由与财务、税务、法务三条线最终审批人）进三审与最高确认。写操作要 finance.dividend.manage。
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult" | "setGlobalError">;

const MANAGE_PERMS = ["finance.dividend.manage"];
// 同 V658 chk_dtrv_status。
const STATUS_LABELS: Record<string, string> = { pending_review: "财务、税务、法务审核与最高确认中", approved: "已生效", rejected: "已驳回" };
const LINES = { finance_review: "财务最终审批人", tax_review: "税务最终审批人", legal_review: "法务最终审批人" } as const;
type LineCode = keyof typeof LINES;
const LINE_CODES = Object.keys(LINES) as LineCode[];
const EMPTY_FORM: Record<"ratePercent" | "minPercent" | "maxPercent" | "effectiveFrom" | "reason" | LineCode, string> = {
  ratePercent: "", minPercent: "", maxPercent: "", effectiveFrom: "", reason: "", finance_review: "", tax_review: "", legal_review: "",
};
const TEXT = {
  loadFailed: "分红代扣税率读取失败",
  required: "请填写税率、下限、上限、生效起始日期、理由与财务、税务、法务三条线最终审批人。",
  bounds: "税率须在 0～100 之间，且下限 ≤ 税率 ≤ 上限。",
};

const percent = (bps?: number | null) => (bps === undefined || bps === null ? "—" : `${(bps / 100).toFixed(bps % 100 === 0 ? 0 : 2)}%`);
const toBps = (text: string) => {
  const value = Number(text);
  return text.trim() === "" || !Number.isFinite(value) || value < 0 || value > 100 ? null : Math.round(value * 100);
};
const nextKey = (prefix: string) => `${prefix}-${Date.now()}`;

/** 结果区只放版本本身。 */
function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  const row = (result ?? {}) as Partial<DividendTaxRateRow>;
  return { action, idempotencyKey: request.idempotencyKey, result: { id: row.id, rateBps: row.rateBps, effectiveFrom: row.effectiveFrom, status: row.status } };
}

export function FinanceDividendTaxRatePanel({ api, effectivePermissions, setActionResult, setGlobalError }: Props) {
  const [rates, setRates] = React.useState<DividendTaxRateRow[]>([]);
  const [form, setForm] = React.useState(EMPTY_FORM);

  const resource = useAdminResource(
    async () => {
      const rows = await api.get<DividendTaxRateRow[]>("/api/v1/finance/dividends/tax-rates");
      setRates(Array.isArray(rows) ? rows : []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
  );
  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: resource.reload, setGlobalError, setActionResult, makeResult, fallbackMessage: TEXT.loadFailed,
  });

  async function submit() {
    if ([form.ratePercent, form.minPercent, form.maxPercent, form.effectiveFrom, form.reason, ...LINE_CODES.map((code) => form[code])]
      .some((value) => !value.trim())) {
      return setFormError(TEXT.required);
    }
    const rateBps = toBps(form.ratePercent);
    const minBps = toBps(form.minPercent);
    const maxBps = toBps(form.maxPercent);
    if (rateBps === null || minBps === null || maxBps === null || minBps > rateBps || rateBps > maxBps) return setFormError(TEXT.bounds);
    const request = {
      idempotencyKey: nextKey("dividend-tax-rate"), rateBps, minBps, maxBps, effectiveFrom: form.effectiveFrom, reason: form.reason.trim(),
      parallelLines: LINE_CODES.map((lineCode) => ({ lineCode, finalApproverId: form[lineCode].trim() })),
    };
    if (await runAction("dividend_tax_rate_submitted", request, () => api.post("/api/v1/finance/dividends/tax-rates", request))) setForm(EMPTY_FORM);
  }

  const input = (key: keyof typeof EMPTY_FORM, label: string, testId: string, type = "text") => (
    <label>{label}<input data-testid={testId} type={type} value={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.value })} /></label>
  );

  return (
    <section data-testid="dividend-tax-rate-panel" aria-label="分红代扣个税税率">
      <h3>分红代扣个税税率（全平台统一）</h3>
      <p className="admin-muted-action">个人股东分得的款项按「股息红利」代扣个人所得税，税率全平台统一（出厂 20%）；每期出单按出单当日已生效的版本代扣，并把所用版本记在单上，公司股东不代扣。调整须同时填写上下限与生效起始日期，经财务、税务、法务审核并由顶层平台公司最终确认后生效，此前出的单不变。</p>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="dividend-tax-rate-table">
          <thead><tr><th>版本状态</th><th>税率</th><th>上下限</th><th>生效起始日</th><th>理由</th><th>审批单</th></tr></thead>
          <tbody>{rates.map((row) => (
            <tr key={row.id} data-testid={`dividend-tax-rate-row-${row.id}`}>
              <td>{STATUS_LABELS[row.status ?? ""] ?? "状态待核实"}</td>
              <td>{percent(row.rateBps)}</td>
              <td>{row.minBps === null || row.minBps === undefined ? "未配" : `${percent(row.minBps)}～${percent(row.maxBps)}`}</td>
              <td>{row.effectiveFrom ?? "出厂默认（自始有效）"}</td>
              <td>{row.reason ?? "—"}</td>
              <td>{row.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>查看审批链</a> : "—"}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-row" data-testid="dividend-tax-rate-form">
        {input("ratePercent", "税率（%）", "dividend-tax-rate-percent", "number")}
        {input("minPercent", "下限（%）", "dividend-tax-rate-min", "number")}
        {input("maxPercent", "上限（%）", "dividend-tax-rate-max", "number")}
        {input("effectiveFrom", "生效起始日期", "dividend-tax-rate-from", "date")}
        {input("reason", "理由", "dividend-tax-rate-reason")}
        {LINE_CODES.map((code) => <React.Fragment key={code}>{input(code, LINES[code], `dividend-tax-rate-${code}`)}</React.Fragment>)}
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS} disabled={busyAction !== ""}
          data-testid="dividend-tax-rate-submit" onClick={() => void submit()}>提交新税率</PermissionButton>
      </div>
      {formError ? <p role="alert" className="admin-inline-error" data-testid="dividend-tax-rate-error">{formError}</p> : null}
    </section>
  );
}
