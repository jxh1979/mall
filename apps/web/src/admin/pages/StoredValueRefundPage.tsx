// 文件版本：V0.02
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { accountFormOf } from "../adminRoutes";
import type { AdminPageProps } from "../types";

// 退卡查卡接口的一张卡（后端 StoredValueRefundLookupService.StoredValueRefundCard，unwrap 已 camel 化）；手机号只给掩码。
type RefundCard = {
  accountId: string;
  customerId: string;
  maskedPhone: string | null;
  projectId: string | null;
  merchantId: string | null;
  principalCent: number;
  bonusCent: number;
  status: string;
  refundAllowed: boolean;
  refundRatioBps: number;
  pendingRefundCent: number | null;
};

const LOOKUP = "/api/v1/loyalty/stored-value/refund-cards/lookup";
const REFUND = ["customer.stored_value.refund"];
const FREEZE = ["customer.stored_value.freeze"];
const STATUS_LABELS: Record<string, string> = { active: "正常", frozen: "已冻结" };
const yuan = (cent: number) => (cent / 100).toFixed(2);
const cardPath = (card: RefundCard, action: string) =>
  `/api/v1/loyalty/stored-value/${encodeURIComponent(card.customerId)}/${action}`;
const errorText = (error: unknown, fallback: string) => (error instanceof Error ? error.message : fallback);

/**
 * 储值退卡（ADM-CUS-006）。第十八批 AZ 段第 278 条 278-R2（BSM:833「商户卡仍由发卡商户直接收款和退款」「综合体通用卡的未消费本金默认全额可退，
 * 赠送金不退现」「单张储值卡退款时如申请同时销卡，本金未退清的须拒绝销卡并明确报错」；ENUM:86「余额管理与退款均由长沙项目公司负责」）：
 * 按手机号查卡（后端按当前账号范围只列可办理的卡）；不挂商户的账号办通用卡退卡（一次退还全部本金、赠送金作废、卡关闭，应退本金由财务按原收款路径办理）；
 * 商户账号在本商户柜台办商户卡退卡（按发卡商户规则退，填兑付凭证号）。页上有客人掩码手机号，属敏感页（PGM:687）。
 * 第十八批 BC 段第 365 条 365-R1（BSM:833「账户可冻结/解冻。」）：冻结、解冻入口也在本页，分支与退卡相同；账户关闭待退而冻结的商户卡不给解冻。
 */
export function StoredValueRefundPage({ api, user, setActionResult, setGlobalError, effectivePermissions, reportViewedObjects }: AdminPageProps) {
  const merchantAccount = accountFormOf(user) === "merchant";
  const [phone, setPhone] = React.useState("");
  const [cards, setCards] = React.useState<RefundCard[] | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [selected, setSelected] = React.useState<RefundCard | null>(null);
  const [reason, setReason] = React.useState("");
  const [amount, setAmount] = React.useState("");
  const [closeAccount, setCloseAccount] = React.useState(false);
  const [voucher, setVoucher] = React.useState("");
  const [outcome, setOutcome] = React.useState("");
  // 同一请求体重试沿用同一提交编号（响应丢失后重提只认一次），成功后清空。
  const requestKey = React.useRef<{ body: string; key: string } | null>(null);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("customer_stored_value",
    (cards ?? []).map((card) => ({ id: card.accountId, customerId: card.customerId, merchantId: card.merchantId }))), [cards, reportViewedObjects]);

  async function search() {
    const trimmed = phone.trim();
    if (!trimmed) {
      setGlobalError("请输入完整手机号（精确匹配）");
      return;
    }
    setLoading(true);
    try {
      // AUD-020：手机号属个人信息，走请求体，不进 URL。
      setCards((await api.post<RefundCard[]>(LOOKUP, { phone: trimmed })) ?? []);
      setSelected(null);
    } catch (error) {
      setGlobalError(errorText(error, "储值卡查询失败"));
    } finally {
      setLoading(false);
    }
  }

  function choose(card: RefundCard) {
    setSelected(card);
    setReason("");
    setVoucher("");
    setOutcome("");
    setAmount(yuan(card.principalCent));
    setCloseAccount(card.pendingRefundCent != null);
    requestKey.current = null;
  }

  async function submit(path: string, body: Record<string, unknown>, done: string) {
    const serialized = JSON.stringify([path, body]);
    if (requestKey.current?.body !== serialized) requestKey.current = { body: serialized, key: crypto.randomUUID() };
    try {
      const result = await api.post<unknown>(path, { ...body, idempotencyKey: requestKey.current.key });
      requestKey.current = null;
      setActionResult(result);
      setOutcome(done);
      await search();
    } catch (error) {
      setGlobalError(errorText(error, "退卡失败"));
    }
  }

  function requireReason(): string | null {
    const trimmed = reason.trim();
    if (!trimmed) setGlobalError("请填写退卡原因");
    return trimmed || null;
  }

  function refundGeneral(card: RefundCard) {
    const why = requireReason();
    if (!why) return;
    void submit(cardPath(card, "refund"), { projectId: card.projectId, amountCent: card.principalCent, reason: why, closeAccount: true },
      "已登记应退本金，由财务按原收款路径办理，实际退款成功才结清。");
  }

  function counterRefund(card: RefundCard) {
    const amountCent = Math.round(Number(amount) * 100);
    if (!Number.isFinite(amountCent) || amountCent <= 0 || amountCent > card.principalCent) {
      setGlobalError(`退还本金须大于 0 且不超过剩余本金 ${yuan(card.principalCent)} 元`);
      return;
    }
    if (!voucher.trim()) {
      setGlobalError("请填写兑付凭证号");
      return;
    }
    const why = requireReason();
    if (!why) return;
    void submit(cardPath(card, "counter-refund"), { projectId: card.projectId, merchantId: card.merchantId, amountCent, reason: why,
      closeAccount, redemptionVoucherNo: voucher.trim() }, "柜台退卡已办理，款项由本商户柜台现场兑付给客人。");
  }

  // 冻结、解冻接口不要求提交编号，确认后直接发请求。
  async function toggleFreeze(card: RefundCard, freeze: boolean) {
    try {
      const result = await api.post<unknown>(cardPath(card, freeze ? "freeze" : "unfreeze"),
        { projectId: card.projectId, merchantId: card.merchantId });
      setActionResult(result);
      setOutcome(freeze ? "已冻结" : "已解冻");
      await search();
    } catch (error) {
      setGlobalError(errorText(error, freeze ? "冻结失败" : "解冻失败"));
    }
  }

  function freezeAction(card: RefundCard) {
    const freeze = card.status === "active";
    if (!freeze && (card.status !== "frozen" || card.pendingRefundCent != null)) return null;
    return (
      <HighRiskConfirm
        label={freeze ? "冻结" : "解冻"}
        impact={freeze ? "冻结后该卡暂停消费，解冻前不能使用" : "解冻后该卡恢复消费"}
        irreversible={false}
        onConfirm={() => void toggleFreeze(card, freeze)}
        permissionsState={effectivePermissions}
        permissions={FREEZE}
        data-testid={`stored-value-${freeze ? "freeze" : "unfreeze"}-${card.accountId}`}
      />
    );
  }

  function action(card: RefundCard) {
    if (!card.merchantId) {
      return merchantAccount ? <span>通用卡由长沙项目公司服务台退款</span>
        : <><button type="button" onClick={() => choose(card)}>办理退卡</button>{freezeAction(card)}</>;
    }
    if (!merchantAccount) return <span>商户卡由发卡商户退款</span>;
    return <>{card.refundAllowed ? <button type="button" onClick={() => choose(card)}>柜台退卡</button>
      : <span>发卡商户不允许该商户卡退款</span>}{freezeAction(card)}</>;
  }

  const amountCent = Math.round(Number(amount) * 100);
  const pending = selected?.pendingRefundCent != null;

  return (
    <section data-testid="stored-value-refund-page">
      <AdminPageHeader
        title="储值退卡"
        description="按手机号查客人储值卡。综合体通用卡由长沙项目公司服务台退卡：一次退还全部剩余本金、赠送金不退现、卡关闭，应退本金由财务按原收款路径办理；商户卡由发卡商户柜台退款，按发卡商户规则退，须填兑付凭证号；申请同时销卡的须退清本金。也可在此冻结、解冻。"
      />
      <div className="admin-inline-form" data-testid="stored-value-refund-search">
        <input aria-label="客人手机号" placeholder="客人手机号（完整精确匹配）" value={phone} onChange={(e) => setPhone(e.target.value)} />
        <button type="button" disabled={loading} onClick={() => void search()}>{loading ? "查询中…" : "查询"}</button>
      </div>
      {outcome && <p role="status">{outcome}</p>}
      {cards !== null && cards.length === 0 && <p data-testid="stored-value-refund-empty">未找到当前账号可办理退卡的储值卡。</p>}
      {cards !== null && cards.length > 0 && (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="stored-value-refund-table">
            <thead>
              <tr><th>手机号</th><th>卡类型</th><th>本金（元）</th><th>赠送金（元）</th><th>状态</th><th>退款规则</th><th>待退事项</th><th>操作</th></tr>
            </thead>
            <tbody>
              {cards.map((card) => (
                <tr key={card.accountId}>
                  <td>{card.maskedPhone ?? "—"}</td>
                  <td>{card.merchantId ? `商户卡（${card.merchantId}）` : "综合体通用卡"}</td>
                  <td>{yuan(card.principalCent)}</td>
                  <td>{yuan(card.bonusCent)}</td>
                  <td>{STATUS_LABELS[card.status] ?? card.status}</td>
                  <td>{!card.merchantId ? "本金全额可退，赠送金不退现"
                    : card.refundAllowed ? `按发卡商户规则退 ${card.refundRatioBps / 100}%` : "发卡商户不允许退款"}</td>
                  <td>{card.pendingRefundCent != null ? `账户关闭待退 ${yuan(card.pendingRefundCent)} 元` : "—"}</td>
                  <td>{action(card)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {selected && !selected.merchantId && (
        <div className="admin-form-grid" data-testid="stored-value-general-refund-form">
          <label>退卡原因<input aria-label="退卡原因" value={reason} onChange={(e) => setReason(e.target.value)} /></label>
          <HighRiskConfirm
            label="退卡"
            impact={`一次退还全部剩余本金 ${yuan(selected.principalCent)} 元，赠送金 ${yuan(selected.bonusCent)} 元作废，卡关闭`}
            precheck={() => requireReason() !== null}
            onConfirm={() => refundGeneral(selected)}
            permissionsState={effectivePermissions}
            permissions={REFUND}
            data-testid="stored-value-general-refund-submit"
          />
        </div>
      )}
      {selected && selected.merchantId && (
        <div className="admin-form-grid" data-testid="stored-value-counter-refund-form">
          {pending && <p>账户关闭登记的待退本金 {yuan(selected.pendingRefundCent ?? 0)} 元：须一次退清全部本金并销卡。</p>}
          <label>退还本金（元）<input aria-label="退还本金（元）" type="number" min="0" step="0.01" value={amount}
            disabled={pending} onChange={(e) => setAmount(e.target.value)} /></label>
          <span>按发卡商户规则实退 {Number.isFinite(amountCent) ? yuan(Math.round((amountCent * selected.refundRatioBps) / 10000)) : "—"} 元</span>
          <label>同时销卡<input aria-label="同时销卡" type="checkbox" checked={closeAccount} disabled={pending}
            onChange={(e) => setCloseAccount(e.target.checked)} /></label>
          <label>兑付凭证号<input aria-label="兑付凭证号" value={voucher} onChange={(e) => setVoucher(e.target.value)} /></label>
          <label>退卡原因<input aria-label="退卡原因" value={reason} onChange={(e) => setReason(e.target.value)} /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={REFUND} data-testid="stored-value-counter-refund-submit"
            onClick={() => counterRefund(selected)}>确认柜台退卡</PermissionButton>
        </div>
      )}
    </section>
  );
}
