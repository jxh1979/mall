// 文件版本：V0.01
import React from "react";
import { cnDate } from "../components/RowDetailDrawer";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, DividendCloseoutItem, DividendCloseoutRow, DividendPlanRow, MerchantAdmissionRow } from "../types";
import { LINE_LABELS } from "./ApprovalChainPage";

// 第十八批 AQ 段第 243 条 243-R1（BSM:1787「酒吧关闭须完成关停结算，依法核清本次分红、已算未发分红与发放保护线待释放款项」「已确认应付及待释款项的
// 法律性质和处理由财务、法务核实，不因关店自动核销或一概改为剩余财产分配；相关款项须按核实结果处理并留痕后才能结案。」；PGM:528「发起单据时只需指定
// 该事项的最终审批人……需要平行线审批的（如法务、财务），另行指定该条线的最终审批人」）：持股分红页末尾的酒吧关停结算——登记、列出三类款项、逐条填法律性质
// 与处理方式提交财务法务核实、核实通过后逐条登记处理、结案。写操作要 finance.dividend.manage。
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult" | "setGlobalError"> & {
  plans: DividendPlanRow[];
  merchants: MerchantAdmissionRow[];
};

const MANAGE_PERMS = ["finance.dividend.manage"];
// 同 V644 chk_dco_status。
const STATUS_LABELS: Record<string, string> = { open: "待提交核实", pending_review: "财务法务核实中", verified: "已核实待处理", closed: "已结案" };
// 同 V644 chk_dcoi_item_type。
const ITEM_LABELS: Record<string, string> = { current_period: "本次分红", unpaid_dividend: "已算未发分红", reserve_pending: "保护线待释放" };
const STATEMENT_STATUS_LABELS: Record<string, string> = { draft: "草稿", pending_review: "待审批", approved: "已审批", paid: "已发放", rejected: "已驳回" };
const SOURCE_LABELS: Record<string, string> = {
  dividend_payout: "持股分红发放", dividend_reserve_released: "分红留存已释放", dividend_reserve_withheld: "分红留存待释放",
};
// BSM:1787「由财务、法务核实」：必带财务、法务两线（同后端策略 dividend_closeout_verify）。
const REQUIRED_LINES = ["finance_review", "legal_review"];
const TEXT = {
  loadFailed: "关停结算读取失败",
  registerRequired: "请选择酒吧并填写关店最后营业日与关店原因。",
  submitRequired: "请为每条款项填写法律性质与处理方式，并填写主线、财务线、法务线最终审批人。",
  noteRequired: "请填写处理说明。",
  currentMissing: "关店所在期正式单未出，出单后才能提交核实",
};

const yuan = (cent: number) => (cent / 100).toFixed(2);
const nextKey = (prefix: string) => `${prefix}-${Date.now()}`;

/** 结果区只放登记本身（款项另在表里看）。 */
function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  const row = (result ?? {}) as Partial<DividendCloseoutRow>;
  return { action, idempotencyKey: request.idempotencyKey, result: { id: row.id, merchantId: row.merchantId, closedBusinessDate: row.closedBusinessDate, status: row.status } };
}

function itemDetail(item: DividendCloseoutItem): string {
  if (item.itemType === "current_period" && !item.statementId) return item.holdType === "skip" ? "本期暂不分红" : "正式单未出";
  if (item.sourceType) return SOURCE_LABELS[item.sourceType] ?? "分红应付";
  return `计算单${STATEMENT_STATUS_LABELS[item.statementStatus ?? ""] ?? ""}`;
}

export function FinanceDividendCloseoutPanel({ api, effectivePermissions, setActionResult, setGlobalError, plans, merchants }: Props) {
  const [closeouts, setCloseouts] = React.useState<DividendCloseoutRow[]>([]);
  const [form, setForm] = React.useState({ merchantId: "", closedBusinessDate: "", reason: "" });
  // 各登记的填写草稿：键＝登记号:字段[:序号]。
  const [drafts, setDrafts] = React.useState<Record<string, string>>({});

  const resource = useAdminResource(
    async () => {
      const rows = await api.get<DividendCloseoutRow[]>("/api/v1/finance/dividends/closeouts");
      setCloseouts(Array.isArray(rows) ? rows : []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed },
  );
  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: resource.reload, setGlobalError, setActionResult, makeResult, fallbackMessage: TEXT.loadFailed,
  });

  const merchantName = (id: string) => {
    const merchant = merchants.find((row) => row.id === id);
    return merchant?.merchantName ?? merchant?.displayName ?? merchant?.companyName ?? id;
  };
  const merchantIds = [...new Set(plans.map((plan) => plan.merchantId).filter((id): id is string => Boolean(id)))];
  const draft = (key: string) => (drafts[key] ?? "").trim();
  const input = (key: string, label: string, testId: string) => (
    <input aria-label={label} data-testid={testId} value={drafts[key] ?? ""} onChange={(event) => setDrafts({ ...drafts, [key]: event.target.value })} />
  );

  async function register() {
    const merchantId = form.merchantId || merchantIds[0] || "";
    if (!merchantId || !form.closedBusinessDate || !form.reason.trim()) return setFormError(TEXT.registerRequired);
    const request = { idempotencyKey: nextKey("dividend-closeout"), merchantId, closedBusinessDate: form.closedBusinessDate, reason: form.reason.trim() };
    const ok = await runAction("dividend_closeout_registered", request, () => api.post("/api/v1/finance/dividends/closeouts", request));
    if (ok) setForm({ merchantId: "", closedBusinessDate: "", reason: "" });
  }

  async function submit(row: DividendCloseoutRow) {
    const items = row.closeoutItems ?? [];
    const finalApproverId = draft(`${row.id}:final`);
    if (!finalApproverId || REQUIRED_LINES.some((code) => !draft(`${row.id}:line:${code}`))
        || items.some((_, index) => !draft(`${row.id}:nature:${index}`) || !draft(`${row.id}:handling:${index}`))) {
      return setFormError(TEXT.submitRequired);
    }
    const request = {
      idempotencyKey: nextKey("dividend-closeout-submit"),
      finalApproverId,
      parallelLines: REQUIRED_LINES.map((lineCode) => ({ lineCode, finalApproverId: draft(`${row.id}:line:${lineCode}`) })),
      items: items.map((item, index) => ({
        itemType: item.itemType, statementId: item.statementId ?? null, arApItemId: item.arApItemId ?? null,
        legalNature: draft(`${row.id}:nature:${index}`), handling: draft(`${row.id}:handling:${index}`),
      })),
    };
    await runAction("dividend_closeout_submitted", request, () =>
      api.post(`/api/v1/finance/dividends/closeouts/${encodeURIComponent(row.id)}/submit`, request));
  }

  async function handle(row: DividendCloseoutRow, item: DividendCloseoutItem, index: number) {
    const handledNote = draft(`${row.id}:note:${index}`);
    if (!handledNote) return setFormError(TEXT.noteRequired);
    const request = { idempotencyKey: nextKey("dividend-closeout-handle"), handledNote };
    await runAction("dividend_closeout_item_handled", request, () =>
      api.post(`/api/v1/finance/dividends/closeouts/${encodeURIComponent(row.id)}/items/${encodeURIComponent(item.id ?? "")}/handle`, request));
  }

  async function close(row: DividendCloseoutRow) {
    const request = { idempotencyKey: nextKey("dividend-closeout-close") };
    await runAction("dividend_closeout_closed", request, () =>
      api.post(`/api/v1/finance/dividends/closeouts/${encodeURIComponent(row.id)}/close`, request));
  }

  return (
    <section data-testid="dividend-closeout-panel" aria-label="酒吧关停结算">
      <h3>酒吧关停结算</h3>
      <p className="admin-muted-action">酒吧关闭须完成关停结算：列出本次分红、已算未发分红与发放保护线待释放款项，关店所在期正式单出单后逐条填写法律性质与处理方式，提交财务、法务核实；核实通过后逐条按核实结果登记处理，全部留痕后结案。不因关店自动核销任何款项；须付或须释放的仍走付款确认、登记付款与留存释放。</p>
      <div className="admin-form-row" data-testid="dividend-closeout-form">
        <label>酒吧<select data-testid="dividend-closeout-merchant" value={form.merchantId || merchantIds[0] || ""} onChange={(event) => setForm({ ...form, merchantId: event.target.value })}>
          {merchantIds.map((id) => <option key={id} value={id}>{merchantName(id)}</option>)}
        </select></label>
        <label>关店最后营业日<input type="date" data-testid="dividend-closeout-date" value={form.closedBusinessDate} onChange={(event) => setForm({ ...form, closedBusinessDate: event.target.value })} /></label>
        <label>关店原因<input data-testid="dividend-closeout-reason" value={form.reason} onChange={(event) => setForm({ ...form, reason: event.target.value })} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS} disabled={busyAction !== ""} data-testid="dividend-closeout-register" onClick={() => void register()}>登记关停结算</PermissionButton>
      </div>
      {formError ? <p role="alert" className="admin-inline-error" data-testid="dividend-closeout-error">{formError}</p> : null}
      {closeouts.length === 0 ? <p className="admin-muted-action" data-testid="dividend-closeout-empty">暂无关停结算登记。</p> : closeouts.map((row) => {
        const items = row.closeoutItems ?? [];
        const current = items.find((item) => item.itemType === "current_period");
        const currentMissing = Boolean(current && !current.statementId && current.holdType !== "skip");
        return (
          <div key={row.id} data-testid={`dividend-closeout-row-${row.id}`}>
            <h4>{merchantName(row.merchantId)} · 关店最后营业日 {row.closedBusinessDate} · {STATUS_LABELS[row.status] ?? "状态待核实"}</h4>
            <p>关店原因：{row.reason}{row.submittedAt ? `；提交核实：${row.submittedBy ?? "—"} / ${cnDate(row.submittedAt)}` : ""}{row.closedAt ? `；结案：${row.closedBy ?? "—"} / ${cnDate(row.closedAt)}` : ""}</p>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid={`dividend-closeout-items-${row.id}`}>
                <thead><tr><th>款项类别</th><th>期间</th><th>说明</th><th>金额（元）</th><th>法律性质</th><th>处理方式</th><th>处理留痕</th></tr></thead>
                <tbody>{items.map((item, index) => (
                  <tr key={`${item.itemType}-${item.statementId ?? ""}-${item.arApItemId ?? ""}`}>
                    <td>{ITEM_LABELS[item.itemType] ?? "其他款项"}</td>
                    <td>{item.periodLabel ?? "—"}</td>
                    <td>{itemDetail(item)}</td>
                    <td>{yuan(item.amountCent)}</td>
                    <td>{row.status === "open" ? input(`${row.id}:nature:${index}`, "法律性质", `dividend-closeout-nature-${row.id}-${index}`) : item.legalNature ?? "—"}</td>
                    <td>{row.status === "open" ? input(`${row.id}:handling:${index}`, "处理方式", `dividend-closeout-handling-${row.id}-${index}`) : item.handling ?? "—"}</td>
                    <td>{item.handledAt ? `${item.handledNote ?? "—"}（${item.handledBy ?? "—"} / ${cnDate(item.handledAt)}）` : row.status === "verified" ? (
                      <>
                        {input(`${row.id}:note:${index}`, "处理说明", `dividend-closeout-note-${row.id}-${index}`)}
                        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS} disabled={busyAction !== ""}
                          data-testid={`dividend-closeout-handle-${row.id}-${index}`} onClick={() => void handle(row, item, index)}>登记处理</PermissionButton>
                      </>
                    ) : "—"}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            {row.status === "open" ? (
              <div className="admin-form-row" data-testid={`dividend-closeout-submit-form-${row.id}`}>
                {currentMissing ? <p className="admin-inline-error" data-testid={`dividend-closeout-current-missing-${row.id}`}>{TEXT.currentMissing}</p> : null}
                <label>主线最终审批人{input(`${row.id}:final`, "主线最终审批人", `dividend-closeout-final-approver-${row.id}`)}</label>
                {REQUIRED_LINES.map((code) => (
                  <label key={code}>{`${LINE_LABELS[code] ?? code}最终审批人`}{input(`${row.id}:line:${code}`, `${LINE_LABELS[code] ?? code}最终审批人`, `dividend-closeout-line-${row.id}-${code}`)}</label>
                ))}
                <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS} disabled={busyAction !== "" || currentMissing}
                  data-testid={`dividend-closeout-submit-${row.id}`} onClick={() => void submit(row)}>提交财务法务核实</PermissionButton>
              </div>
            ) : null}
            {row.status === "verified" ? (
              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE_PERMS} disabled={busyAction !== ""}
                data-testid={`dividend-closeout-close-${row.id}`} onClick={() => void close(row)}>结案</PermissionButton>
            ) : null}
          </div>
        );
      })}
    </section>
  );
}
