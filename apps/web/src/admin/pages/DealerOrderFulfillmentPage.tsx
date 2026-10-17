// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

/**
 * 经销订单与履约（ADM-DLR-002，CG-11b）。BSM:1226-1234：订单先审核合同、价格、库存和交期再通知付款；只支持对公转账与平台线上支付；
 * 财务核验首付款达门槛后才出库；审核 / 到账核验 / 出库不同人；取消审批与到账核验互斥；E-13 首付款截止未达门槛取消并退款；尾款逾期暂停账期订单；超收单独记录。
 */
type DealerRow = { id: string; dealerName?: string; status?: string; creditSuspended?: boolean };
type ContractRow = { id: string; contractNo?: string; creditTerm?: boolean; firstPaymentRatioBps?: number | null; firstPaymentAmountCent?: number | null; firstPaymentDeadlineDays?: number | null; balanceDueDays?: number | null; freightCent?: number | null; freeShippingThresholdCent?: number | null; freightBearer?: string; freightTemplateVersion?: number };
type OrderRow = { id: string; orderNo?: string; status?: string; creditTerm?: boolean; goodsAmountCent?: number; freightCent?: number; totalAmountCent?: number; firstPaymentRequiredCent?: number; firstPaymentDeadlineAt?: string | null; balanceDueAt?: string | null; paidAmountCent?: number; overdue?: boolean; reviewedBy?: string | null; shippedBy?: string | null; cancelRequestedBy?: string | null; note?: string | null };
type PaymentRow = { id: string; method?: string; amountCent?: number; status?: string; appliedCent?: number; verifiedBy?: string | null; referenceNo?: string | null };
type RefundRow = { id: string; paymentId?: string; amountCent?: number; status?: string; reviewId?: string | null; executedBy?: string | null };
type OverpaymentRow = { id: string; orderId?: string; amountCent?: number; status?: string; targetOrderId?: string | null };

const ORDER_STATUS: Record<string, string> = {
  draft: "草稿", pending_review: "待审核", rejected: "已驳回", pending_payment: "待付款", payment_verifying: "付款核验中", pending_shipment: "待出库",
  shipping: "配送中", signed: "已签收", completed: "已完成", cancelling: "取消中", cancelled: "已取消", pending_refund: "待退款",
  returning: "退货中", partially_returned: "部分退货", returned: "已退货",
};
const PAYMENT_METHOD: Record<string, string> = { bank_transfer: "对公转账", platform_online: "平台线上支付" };
const PAYMENT_STATUS: Record<string, string> = { pending_verification: "待核验", verified: "已核验", rejected: "核验不通过", late: "取消后迟到（待退款）" };
const REFUND_STATUS: Record<string, string> = { pending_request: "待发起审批", pending_approval: "退款审批中", approved: "已批准待执行", rejected: "审批驳回", refunded: "已退款" };
const OVERPAYMENT_STATUS: Record<string, string> = { unconfirmed: "用途未确认", prepayment: "已转预付款", offset_applied: "已抵扣指定订单" };
const DEALER_STATUS: Record<string, string> = { pending_submission: "待提交", under_review: "审核中", approved: "已通过", rejected: "已驳回", deactivated: "已停用" };

function money(cent?: number | null) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}
function label(map: Record<string, string>, value?: string | null) {
  return value ? (map[value] ?? value) : "—";
}
function cnTime(value?: string | null) {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export function DealerOrderFulfillmentPage({ api, setGlobalError, setActionResult, effectivePermissions }: AdminPageProps) {
  const [dealers, setDealers] = React.useState<DealerRow[]>([]);
  const [dealer, setDealer] = React.useState<DealerRow | null>(null);
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [orders, setOrders] = React.useState<OrderRow[]>([]);
  const [overpayments, setOverpayments] = React.useState<OverpaymentRow[]>([]);
  const [order, setOrder] = React.useState<OrderRow | null>(null);
  const [payments, setPayments] = React.useState<PaymentRow[]>([]);
  const [refunds, setRefunds] = React.useState<RefundRow[]>([]);
  const [formError, setFormError] = React.useState("");
  // 条款
  const [termsContract, setTermsContract] = React.useState("");
  const [creditTerm, setCreditTerm] = React.useState(false);
  const [ratio, setRatio] = React.useState("");
  const [deadlineDays, setDeadlineDays] = React.useState("");
  const [balanceDays, setBalanceDays] = React.useState("");
  const [freight, setFreight] = React.useState("");
  const [freeThreshold, setFreeThreshold] = React.useState("");
  const [bearer, setBearer] = React.useState("dealer");
  // 建单
  const [draftContract, setDraftContract] = React.useState("");
  const [draftLines, setDraftLines] = React.useState("");
  // 付款
  const [payMethod, setPayMethod] = React.useState("bank_transfer");
  const [payAmount, setPayAmount] = React.useState("");
  const [payRef, setPayRef] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const rows = await api.get<DealerRow[]>("/api/v1/dealers");
      setDealers(rows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "经销商列表读取失败", onStart: () => setGlobalError(null) },
  );

  const key = () => `dor-${Date.now()}-${Math.random().toString(16).slice(2)}`;

  async function run(action: string, fn: () => Promise<Record<string, unknown> | void>) {
    setFormError("");
    try {
      const result = await fn();
      setActionResult({ action, ...(result ?? {}) });
    } catch (e) {
      const message = e instanceof Error ? e.message : "操作失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  async function selectDealer(row: DealerRow) {
    setDealer(row);
    setOrder(null); setPayments([]); setRefunds([]);
    await loadDealer(row.id);
  }
  async function loadDealer(id: string) {
    try {
      const [cs, os, ops] = await Promise.all([
        api.get<ContractRow[]>(`/api/v1/dealers/${encodeURIComponent(id)}/contracts`),
        api.get<OrderRow[]>(`/api/v1/dealers/${encodeURIComponent(id)}/orders`),
        api.get<OverpaymentRow[]>(`/api/v1/dealers/${encodeURIComponent(id)}/overpayments`),
      ]);
      setContracts(cs ?? []); setOrders(os ?? []); setOverpayments(ops ?? []);
    } catch (e) { setFormError(e instanceof Error ? e.message : "经销商资料读取失败"); }
  }
  async function selectOrder(row: OrderRow) {
    setOrder(row);
    await loadOrder(row.id);
  }
  async function loadOrder(id: string) {
    try {
      const [o, ps, rs] = await Promise.all([
        api.get<OrderRow>(`/api/v1/dealers/orders/${encodeURIComponent(id)}`),
        api.get<PaymentRow[]>(`/api/v1/dealers/orders/${encodeURIComponent(id)}/payments`),
        api.get<RefundRow[]>(`/api/v1/dealers/orders/${encodeURIComponent(id)}/refunds`),
      ]);
      setOrder(o); setPayments(ps ?? []); setRefunds(rs ?? []);
      if (dealer) await loadDealer(dealer.id);
    } catch (e) { setFormError(e instanceof Error ? e.message : "订单读取失败"); }
  }

  const saveTerms = () => run("dealer_contract_terms_updated", async () => {
    if (!termsContract) throw new Error("请先选择合同");
    const c = await api.post<ContractRow>(`/api/v1/dealers/contracts/${encodeURIComponent(termsContract)}/terms`, {
      idempotencyKey: key(), creditTerm, firstPaymentRatioBps: creditTerm && ratio ? Math.round(Number(ratio) * 100) : undefined,
      firstPaymentDeadlineDays: Number(deadlineDays), balanceDueDays: creditTerm && balanceDays ? Number(balanceDays) : undefined,
      freightCent: freight ? Math.round(Number(freight) * 100) : undefined, freeShippingThresholdCent: freeThreshold ? Math.round(Number(freeThreshold) * 100) : undefined, freightBearer: bearer,
    });
    if (dealer) await loadDealer(dealer.id);
    return { 合同: c?.contractNo, 运费模板版本: c?.freightTemplateVersion };
  });

  const createDraft = () => run("dealer_order_drafted", async () => {
    if (!dealer || !draftContract) throw new Error("请先选择经销商与合同");
    const lines = draftLines.split("\n").map((l) => l.trim()).filter(Boolean).map((l) => {
      const [itemId, spec, qty, inquiryId] = l.split(",").map((s) => s.trim());
      if (!itemId || !Number.isFinite(Number(qty)) || Number(qty) <= 0) throw new Error(`明细行格式应为「商品id,规格,数量,询价单id(可空)」：${l}`);
      return { itemId, spec: spec || "", quantity: Number(qty), inquiryId: inquiryId || undefined };
    });
    const o = await api.post<OrderRow>(`/api/v1/dealers/${encodeURIComponent(dealer.id)}/orders`, { idempotencyKey: key(), dealerContractId: draftContract, lines });
    setDraftLines("");
    await loadDealer(dealer.id);
    return { 订单: o?.orderNo, 合计: money(o?.totalAmountCent), 状态: "草稿" };
  });

  const act = (path: string, body: Record<string, unknown>, action: string, result: Record<string, unknown>) => run(action, async () => {
    await api.post(path, { idempotencyKey: key(), ...body });
    if (order) await loadOrder(order.id);
    return result;
  });

  const registerPayment = () => run("dealer_order_payment_registered", async () => {
    if (!order) throw new Error("请先选择订单");
    const cent = Math.round(Number(payAmount) * 100);
    if (!Number.isFinite(cent) || cent <= 0) throw new Error("到账金额须大于 0");
    const p = await api.post<PaymentRow>(`/api/v1/dealers/orders/${encodeURIComponent(order.id)}/payments`, { idempotencyKey: key(), method: payMethod, amountCent: cent, referenceNo: payRef.trim() || undefined });
    setPayAmount(""); setPayRef("");
    await loadOrder(order.id);
    return { 到账: p?.id, 方式: label(PAYMENT_METHOD, payMethod), 状态: label(PAYMENT_STATUS, p?.status) };
  });

  const prompt = (text: string) => window.prompt(text) ?? "";

  return (
    <section data-testid="dealer-orders-page">
      <AdminPageHeader
        title="经销订单与履约"
        description="订单先审核合同、价格、库存和交期，通过后才通知付款并按中心仓预留库存；只支持对公转账和平台线上支付，禁止现金和个人账户收款；财务核验首付款达到合同门槛后才能出库，未约定账期须全款到账；订单审核、到账核验、仓库出库不得由同一人完成全链；取消审批与到账核验只能有一项先完成；首付款截止未达门槛的订单自动取消并按既有退款审批原路退回；已出库尾款逾期转逾期并暂停该经销商新增账期订单；超收单独记录、用途确认后才可用。"
      />
      <AuditTrailHint event="dealer.order.reviewed / dealer.order.payment_verified / dealer.order.shipped / dealer.order.deadline_cancelled / dealer.order.refund_executed" />
      <p className="admin-muted-action" data-testid="dealer-orders-evidence">接口证据：GET/POST /api/v1/dealers/{"{id}"}/orders；POST /api/v1/dealers/orders/{"{id}"}/review；POST /api/v1/dealers/payments/{"{id}"}/verify；POST /api/v1/dealers/orders/{"{id}"}/ship；POST /api/v1/dealers/refunds/{"{id}"}/execute。</p>
      {formError ? <div data-testid="dealer-orders-error" className="admin-inline-error">{formError}</div> : null}

      <AdminDataState loading={loading} error={error} empty={dealers.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="dealer-orders-dealer-table">
            <thead><tr><th>经销商</th><th>状态</th><th>账期</th><th>操作</th></tr></thead>
            <tbody>
              {dealers.map((row) => (
                <tr key={row.id} className={dealer?.id === row.id ? "admin-row-selected" : undefined}>
                  <td>{row.dealerName ?? row.id}</td><td>{label(DEALER_STATUS, row.status)}</td>
                  <td data-testid={`dealer-credit-${row.id}`}>{row.creditSuspended ? "逾期暂停账期订单" : "正常"}</td>
                  <td><button type="button" data-testid={`dealer-orders-select-${row.id}`} onClick={() => void selectDealer(row)}>选中</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>

      {dealer ? (
        <>
          <h3>合同付款条款与运费模板：{dealer.dealerName}</h3>
          <div className="admin-form-grid" data-testid="contract-terms-form">
            <label>合同<select data-testid="terms-contract-select" value={termsContract} onChange={(e) => setTermsContract(e.target.value)}>
              <option value="">请选择</option>{contracts.map((c) => <option key={c.id} value={c.id}>{c.contractNo ?? c.id}（{c.creditTerm ? "账期" : "全款"}，运费模板 V{c.freightTemplateVersion ?? 0}）</option>)}
            </select></label>
            <label><input type="checkbox" data-testid="terms-credit" checked={creditTerm} onChange={(e) => setCreditTerm(e.target.checked)} />约定账期</label>
            <label>首付款比例（%）<input data-testid="terms-ratio" value={ratio} onChange={(e) => setRatio(e.target.value)} /></label>
            <label>首付款截止（通知付款后天数）<input data-testid="terms-deadline-days" value={deadlineDays} onChange={(e) => setDeadlineDays(e.target.value)} /></label>
            <label>尾款到期（出库后天数）<input data-testid="terms-balance-days" value={balanceDays} onChange={(e) => setBalanceDays(e.target.value)} /></label>
            <label>运费（元）<input data-testid="terms-freight" value={freight} onChange={(e) => setFreight(e.target.value)} /></label>
            <label>包邮门槛（元）<input data-testid="terms-free-threshold" value={freeThreshold} onChange={(e) => setFreeThreshold(e.target.value)} /></label>
            <label>运费承担方<select data-testid="terms-bearer" value={bearer} onChange={(e) => setBearer(e.target.value)}><option value="dealer">经销商</option><option value="sales_entity">销售主体</option></select></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.manage"]} data-testid="terms-save" onClick={() => void saveTerms()}>保存条款（运费模板版本+1，只影响新订单）</PermissionButton>
          </div>

          <h3>建单（草稿）</h3>
          <div className="admin-form-grid" data-testid="order-draft-form">
            <label>合同<select data-testid="draft-contract-select" value={draftContract} onChange={(e) => setDraftContract(e.target.value)}>
              <option value="">请选择</option>{contracts.map((c) => <option key={c.id} value={c.id}>{c.contractNo ?? c.id}</option>)}
            </select></label>
            <label>明细（每行：商品id,规格,数量,询价单id）<textarea data-testid="draft-lines-input" rows={3} value={draftLines} onChange={(e) => setDraftLines(e.target.value)} /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.manage"]} data-testid="draft-create" onClick={() => void createDraft()}>建单</PermissionButton>
          </div>

          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="order-table">
              <thead><tr><th>订单号</th><th>状态</th><th>账期</th><th>货款</th><th>运费</th><th>合计</th><th>已收</th><th>首付款门槛</th><th>首付款截止</th><th>尾款到期</th><th>逾期</th><th>操作</th></tr></thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id} data-testid={`order-row-${o.id}`} className={order?.id === o.id ? "admin-row-selected" : undefined}>
                    <td>{o.orderNo ?? o.id}</td><td data-testid={`order-status-${o.id}`}>{label(ORDER_STATUS, o.status)}</td><td>{o.creditTerm ? "账期" : "全款"}</td>
                    <td>{money(o.goodsAmountCent)}</td><td>{money(o.freightCent)}</td><td>{money(o.totalAmountCent)}</td><td>{money(o.paidAmountCent)}</td>
                    <td>{money(o.firstPaymentRequiredCent)}</td><td>{cnTime(o.firstPaymentDeadlineAt)}</td><td>{cnTime(o.balanceDueAt)}</td><td>{o.overdue ? "逾期" : "—"}</td>
                    <td><button type="button" data-testid={`order-select-${o.id}`} onClick={() => void selectOrder(o)}>处理</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {order ? (
            <>
              <h3>订单 {order.orderNo}：{label(ORDER_STATUS, order.status)}</h3>
              <div className="admin-form-grid" data-testid="order-actions">
                {order.status === "draft" || order.status === "rejected" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.manage"]} data-testid="order-submit" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/submit`, {}, "dealer_order_submitted", { 订单: order.orderNo, 状态: "待审核" })}>提交审核</PermissionButton> : null}
                {order.status === "pending_review" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.review"]} data-testid="order-approve" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/review`, { decision: "approve", note: prompt("审核意见（合同、价格、库存、交期）：") || undefined }, "dealer_order_reviewed", { 订单: order.orderNo, 结论: "通过并通知付款" })}>审核通过（预留库存、通知付款）</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.review"]} data-testid="order-reject" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/review`, { decision: "reject", note: prompt("驳回原因：") || undefined }, "dealer_order_reviewed", { 订单: order.orderNo, 结论: "驳回" })}>驳回</PermissionButton>
                  </>
                ) : null}
                {order.status === "pending_shipment" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.ship"]} data-testid="order-ship" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/ship`, {}, "dealer_order_shipped", { 订单: order.orderNo, 状态: "配送中" })}>仓库出库</PermissionButton> : null}
                {order.status === "shipping" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.manage"]} data-testid="order-sign" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/sign`, {}, "dealer_order_signed", { 订单: order.orderNo, 说明: "签收不表示货款已结清" })}>登记签收</PermissionButton> : null}
                {["pending_payment", "payment_verifying", "pending_shipment"].includes(order.status ?? "") ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.manage"]} data-testid="order-cancel-request" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/cancel-request`, { reason: prompt("取消原因（必填）：") }, "dealer_order_cancel_requested", { 订单: order.orderNo, 状态: "取消中" })}>申请取消</PermissionButton> : null}
                {order.status === "cancelling" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.review"]} data-testid="order-cancel-approve" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/cancel-decision`, { decision: "approve" }, "dealer_order_cancel_decided", { 订单: order.orderNo, 结论: "取消生效（已到账走退款审批）" })}>批准取消</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.review"]} data-testid="order-cancel-reject" onClick={() => void act(`/api/v1/dealers/orders/${order.id}/cancel-decision`, { decision: "reject", note: prompt("驳回原因：") || undefined }, "dealer_order_cancel_decided", { 订单: order.orderNo, 结论: "继续履约" })}>驳回取消</PermissionButton>
                  </>
                ) : null}
              </div>

              <h3>到账登记与核验（只支持对公转账、平台线上支付）</h3>
              <div className="admin-form-grid" data-testid="payment-form">
                <label>支付方式<select data-testid="pay-method" value={payMethod} onChange={(e) => setPayMethod(e.target.value)}><option value="bank_transfer">对公转账</option><option value="platform_online">平台线上支付</option></select></label>
                <label>金额（元）<input data-testid="pay-amount" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} /></label>
                <label>流水号<input data-testid="pay-ref" value={payRef} onChange={(e) => setPayRef(e.target.value)} /></label>
                <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.manage"]} data-testid="pay-register" onClick={() => void registerPayment()}>登记到账</PermissionButton>
              </div>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="payment-table">
                  <thead><tr><th>到账</th><th>方式</th><th>金额</th><th>计入订单</th><th>状态</th><th>核验人</th><th>流水号</th><th>操作</th></tr></thead>
                  <tbody>
                    {payments.map((p) => (
                      <tr key={p.id} data-testid={`payment-row-${p.id}`}>
                        <td>{p.id}</td><td>{label(PAYMENT_METHOD, p.method)}</td><td>{money(p.amountCent)}</td><td>{money(p.appliedCent)}</td>
                        <td data-testid={`payment-status-${p.id}`}>{label(PAYMENT_STATUS, p.status)}</td><td>{p.verifiedBy ?? "—"}</td><td>{p.referenceNo ?? "—"}</td>
                        <td>
                          {p.status === "pending_verification" ? (
                            <>
                              <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.payment.verify"]} data-testid={`payment-verify-${p.id}`} onClick={() => void act(`/api/v1/dealers/payments/${p.id}/verify`, { decision: "approve" }, "dealer_order_payment_verified", { 到账: p.id, 结论: "核验通过" })}>核验通过</PermissionButton>
                              <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.payment.verify"]} data-testid={`payment-reject-${p.id}`} onClick={() => void act(`/api/v1/dealers/payments/${p.id}/verify`, { decision: "reject", note: prompt("不通过原因：") || undefined }, "dealer_order_payment_verified", { 到账: p.id, 结论: "核验不通过" })}>不通过</PermissionButton>
                            </>
                          ) : null}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <h3>退款（既有退款审批原路退回；执行人不得为到账核验人）</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="refund-table">
                  <thead><tr><th>退款</th><th>到账</th><th>金额</th><th>状态</th><th>复核单</th><th>执行人</th><th>操作</th></tr></thead>
                  <tbody>
                    {refunds.map((r) => (
                      <tr key={r.id} data-testid={`refund-row-${r.id}`}>
                        <td>{r.id}</td><td>{r.paymentId ?? "—"}</td><td>{money(r.amountCent)}</td><td data-testid={`refund-status-${r.id}`}>{label(REFUND_STATUS, r.status)}</td><td>{r.reviewId ?? "—"}</td><td>{r.executedBy ?? "—"}</td>
                        <td>
                          {r.status === "pending_request" || r.status === "rejected" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.payment.verify"]} data-testid={`refund-approval-${r.id}`} onClick={() => void act(`/api/v1/dealers/refunds/${r.id}/approval`, { finalApproverId: prompt("最终审批人账号（可空）：") || undefined }, "dealer_order_refund_requested", { 退款: r.id, 状态: "退款审批中" })}>发起退款审批</PermissionButton> : null}
                          {r.status === "approved" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.refund.execute"]} data-testid={`refund-execute-${r.id}`} onClick={() => void act(`/api/v1/dealers/refunds/${r.id}/execute`, { referenceNo: prompt("退款流水号：") || undefined }, "dealer_order_refund_executed", { 退款: r.id, 状态: "已退款" })}>执行退款</PermissionButton> : null}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : null}

          <h3>超收（用途未确认前不自动抵账）</h3>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="overpayment-table">
              <thead><tr><th>超收</th><th>来源订单</th><th>金额</th><th>状态</th><th>抵扣订单</th><th>操作</th></tr></thead>
              <tbody>
                {overpayments.map((v) => (
                  <tr key={v.id} data-testid={`overpayment-row-${v.id}`}>
                    <td>{v.id}</td><td>{v.orderId ?? "—"}</td><td>{money(v.amountCent)}</td><td data-testid={`overpayment-status-${v.id}`}>{label(OVERPAYMENT_STATUS, v.status)}</td><td>{v.targetOrderId ?? "—"}</td>
                    <td>
                      {v.status === "unconfirmed" ? (
                        <>
                          <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.payment.verify"]} data-testid={`overpayment-prepay-${v.id}`} onClick={() => void run("dealer_order_overpayment_use_confirmed", async () => { await api.post(`/api/v1/dealers/overpayments/${v.id}/use`, { idempotencyKey: key(), usage: "prepayment" }); if (dealer) await loadDealer(dealer.id); return { 超收: v.id, 用途: "转预付款" }; })}>转预付款</PermissionButton>
                          <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.order.payment.verify"]} data-testid={`overpayment-offset-${v.id}`} onClick={() => void run("dealer_order_overpayment_use_confirmed", async () => { const target = prompt("抵扣的订单 id（同一经销商、同一销售主体）："); await api.post(`/api/v1/dealers/overpayments/${v.id}/use`, { idempotencyKey: key(), usage: "offset_order", targetOrderId: target }); if (dealer) await loadDealer(dealer.id); return { 超收: v.id, 用途: "抵扣指定订单", 订单: target }; })}>抵扣指定订单</PermissionButton>
                        </>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </section>
  );
}
