// 文件版本：V0.04
import React from "react";
import { DealerDeliveryPanel } from "./DealerDeliveryPanel";
import { DealerPortalApi, DealerPortalApiError } from "./dealerPortalClient";

/**
 * 独立经销商 Web 门户（CG-11d；BSM:1217-1218、1221、1226-1234、1240；ENUM:233 受控第十一端）。
 * 与商户门户及内部员工端隔离账号、权限和数据：只接受经销商门户账号，只见本企业资料、合同、询价、订单与退货；
 * 库存外显只显示有货 / 紧张 / 缺货 / 预计交期；付款只登记对公转账或平台线上支付；停用后立即退出。
 */
type Dealer = { id: string; dealerName?: string; status?: string; portalEnabled?: boolean; legalRepresentative?: string; contactName?: string; contactPhone?: string; reviewNote?: string | null };
type Contract = { id: string; contractNo?: string; validFrom?: string; validTo?: string; creditTerm?: boolean };
type Stock = { itemId: string; itemName?: string; spec?: string; unitPriceCent?: number; availability?: string; expectedRestockDate?: string | null };
type Inquiry = { id: string; itemId?: string; spec?: string; quantity?: number; status?: string; quotedUnitPriceCent?: number | null };
type Order = { id: string; orderNo?: string; status?: string; totalAmountCent?: number; paidAmountCent?: number; firstPaymentRequiredCent?: number; firstPaymentDeadlineAt?: string | null; returnedAmountCent?: number; deliveryAdjustmentCent?: number; firstLegAdjustmentCent?: number; balanceDueAt?: string | null; creditTerm?: boolean; overdue?: boolean };
type Payment = { id: string; method?: string; amountCent?: number; status?: string; referenceNo?: string | null };
type Return = { id: string; returnNo?: string; status?: string; reasonType?: string; requestedQuantity?: number; refundCent?: number; refundedCent?: number };

const DEALER_STATUS: Record<string, string> = { pending_submission: "待提交", under_review: "审核中", approved: "已通过", rejected: "已驳回", deactivated: "已停用" };
const AVAILABILITY: Record<string, string> = { in_stock: "有货", low: "紧张", out_of_stock: "缺货", expected_restock: "预计交期" };
const INQUIRY_STATUS: Record<string, string> = { pending_quote: "待报价", quote_approval: "报价审批中", quoted: "已报价", expired: "已失效" };
const ORDER_STATUS: Record<string, string> = {
  draft: "草稿", pending_review: "待审核", rejected: "已驳回", pending_payment: "待付款", payment_verifying: "付款核验中", pending_shipment: "待出库",
  shipping: "配送中", signed: "已签收", completed: "已完成", cancelling: "取消中", cancelled: "已取消", pending_refund: "待退款",
  returning: "退货中", partially_returned: "部分退货", returned: "已退货",
};
const PAYMENT_METHOD: Record<string, string> = { bank_transfer: "对公转账", platform_online: "平台线上支付" };
const PAYMENT_STATUS: Record<string, string> = { pending_verification: "待核验", verified: "已核验", rejected: "核验不通过", late: "取消后迟到（待退款）" };
const RETURN_STATUS: Record<string, string> = {
  pending_approval: "待审批", rejected: "已驳回", pending_receipt: "待收货", inspecting: "验收中", pending_reversal: "待红冲", pending_refund: "待退款",
  refunding: "退款中", refund_failed: "退款失败", partially_completed: "部分完成", completed: "已完成",
};
const REASON: Record<string, string> = { unopened_resalable: "未拆封可复售", quality_issue: "质量问题" };

function money(cent?: number | null) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}
function label(map: Record<string, string>, value?: string | null) {
  return value ? (map[value] ?? value) : "—";
}
function cnDate(value?: string | null) {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  return m ? `${m[1]}年${Number(m[2])}月${Number(m[3])}日` : value;
}
const key = () => `dp-${Date.now()}-${Math.random().toString(16).slice(2)}`;

export default function DealerPortalApp({ api: injected }: { api?: DealerPortalApi } = {}): React.ReactElement {
  const api = React.useMemo(() => injected ?? new DealerPortalApi(), [injected]);
  const [user, setUser] = React.useState<{ id: string; username: string } | null>(null);
  const [me, setMe] = React.useState<Dealer | null>(null);
  const [restoring, setRestoring] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [notice, setNotice] = React.useState<string | null>(null);
  const [loginName, setLoginName] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [contracts, setContracts] = React.useState<Contract[]>([]);
  const [contractId, setContractId] = React.useState("");
  const [stock, setStock] = React.useState<Stock[]>([]);
  const [inquiries, setInquiries] = React.useState<Inquiry[]>([]);
  const [orders, setOrders] = React.useState<Order[]>([]);
  const [order, setOrder] = React.useState<Order | null>(null);
  const [payments, setPayments] = React.useState<Payment[]>([]);
  const [returns, setReturns] = React.useState<Return[]>([]);
  const [returnSources,setReturnSources] = React.useState<{receiptLineId:string;orderLineId:string;batchId:string;availableQuantity:number}[]>([]);
  const [actualLines,setActualLines] = React.useState<{id:string;itemId:string;spec:string}[]>([]);
  const [returnFinancials,setReturnFinancials] = React.useState<{returnId:string;firstLegRefundCent:number;firstLegRefundedCent:number;firstLegOffsetCent:number}[]>([]);
  // 资料
  // 询价 / 下单 / 付款 / 退货
  const [inqItem, setInqItem] = React.useState("");
  const [inqSpec, setInqSpec] = React.useState("");
  const [inqQty, setInqQty] = React.useState("");
  const [orderLines, setOrderLines] = React.useState("");
  const [payMethod, setPayMethod] = React.useState("bank_transfer");
  const [payAmount, setPayAmount] = React.useState("");
  const [payRef, setPayRef] = React.useState("");
  const [retReason, setRetReason] = React.useState("unopened_resalable");
  const [retLines, setRetLines] = React.useState("");

  const signOut = React.useCallback((message?: string) => {
    setUser(null); setMe(null); setContracts([]); setOrders([]); setOrder(null);
    if (message) setError(message);
  }, []);

  React.useEffect(() => {
    api.onSessionExpired = () => signOut("登录已过期，请重新登录");
    let cancelled = false;
    (async () => {
      try {
        const dealer = await api.get<Dealer>("/api/v1/dealer-portal/me");
        if (!cancelled && dealer?.status === "approved" && dealer.portalEnabled) { setUser({ id: dealer.id, username: dealer.dealerName ?? "" }); setMe(dealer); }
      } catch {
        // 未登录：显示登录页
      } finally {
        if (!cancelled) setRestoring(false);
      }
    })();
    return () => { cancelled = true; };
  }, [api, signOut]);

  async function run(fn: () => Promise<string | void>) {
    setError(null); setNotice(null);
    try {
      const message = await fn();
      if (message) setNotice(message);
    } catch (e) {
      if (e instanceof DealerPortalApiError && (e.code === "DEALER_DEACTIVATED" || e.code === "DEALER_PORTAL_NOT_ENABLED" || e.code === "TERMINAL_DISABLED")) {
        signOut(e.message);
        return;
      }
      setError(e instanceof Error ? e.message : "操作失败");
    }
  }

  async function loadDealer() {
    const dealer = await api.get<Dealer>("/api/v1/dealer-portal/me");
    if (dealer.status !== "approved" || !dealer.portalEnabled) {
      throw new DealerPortalApiError("四类资料审核通过后才可登录经销商门户", 403, "DEALER_PORTAL_NOT_ENABLED");
    }
    setMe(dealer);
    if (dealer.portalEnabled) {
      const [cs, qs, os] = await Promise.all([
        api.get<Contract[]>("/api/v1/dealer-portal/contracts"),
        api.get<Inquiry[]>("/api/v1/dealer-portal/inquiries"),
        api.get<Order[]>("/api/v1/dealer-portal/orders"),
      ]);
      setContracts(cs ?? []); setInquiries(qs ?? []); setOrders(os ?? []);
    }
  }
  async function selectContract(id: string) {
    setContractId(id);
    setStock(id ? (await api.get<Stock[]>(`/api/v1/dealer-portal/contracts/${encodeURIComponent(id)}/external-stock`)) ?? [] : []);
  }
  async function selectOrder(row: Order) {
    const [o, ps, rs, ss, ls, fs] = await Promise.all([
      api.get<Order>(`/api/v1/dealer-portal/orders/${encodeURIComponent(row.id)}`),
      api.get<Payment[]>(`/api/v1/dealer-portal/orders/${encodeURIComponent(row.id)}/payments`),
      api.get<Return[]>(`/api/v1/dealer-portal/orders/${encodeURIComponent(row.id)}/returns`),
      api.get<typeof returnSources>(`/api/v1/dealer-portal/orders/${encodeURIComponent(row.id)}/return-sources`),
      api.get<typeof actualLines>(`/api/v1/dealer-portal/orders/${encodeURIComponent(row.id)}/lines`),
      api.get<typeof returnFinancials>(`/api/v1/dealer-portal/orders/${encodeURIComponent(row.id)}/return-financials`),
    ]);
    setOrder(o); setPayments(ps ?? []); setReturns(rs ?? []);setReturnSources(ss??[]);setActualLines(ls??[]);setReturnFinancials(fs??[]);
  }

  const onLogin = () => run(async () => {
    const u = await api.login(loginName.trim(), password);
    setUser({ id: u.id, username: u.username });
    setPassword("");
    await loadDealer();
  });
  const onLogout = () => run(async () => { await api.logout(); signOut(); });

  const createInquiry = () => run(async () => {
    if (!contractId) throw new Error("请先选择合同");
    await api.post("/api/v1/dealer-portal/inquiries", { idempotencyKey: key(), dealerContractId: contractId, itemId: inqItem.trim(), spec: inqSpec.trim(), quantity: Number(inqQty) });
    setInqItem(""); setInqSpec(""); setInqQty("");
    await loadDealer();
    return "询价已提交，报价审批通过后可订购";
  });
  const createOrder = () => run(async () => {
    if (!contractId) throw new Error("请先选择合同");
    const lines = orderLines.split("\n").map((l) => l.trim()).filter(Boolean).map((l) => {
      const [itemId, spec, qty, inquiryId] = l.split(",").map((s) => s.trim());
      return { itemId, spec: spec || "", quantity: Number(qty), inquiryId: inquiryId || undefined };
    });
    if (lines.length === 0) throw new Error("请填写订单明细");
    const created = await api.post<Order>("/api/v1/dealer-portal/orders", { idempotencyKey: key(), dealerContractId: contractId, lines });
    await api.post(`/api/v1/dealer-portal/orders/${encodeURIComponent(created.id)}/submit`, { idempotencyKey: key() });
    setOrderLines("");
    await loadDealer();
    return `订单 ${created.orderNo ?? ""} 已提交，平台审核通过后通知付款`;
  });
  const registerPayment = () => run(async () => {
    if (!order) throw new Error("请先选择订单");
    const cent = Math.round(Number(payAmount) * 100);
    if (!Number.isFinite(cent) || cent <= 0) throw new Error("付款金额须大于 0");
    await api.post(`/api/v1/dealer-portal/orders/${encodeURIComponent(order.id)}/payments`, { idempotencyKey: key(), method: payMethod, amountCent: cent, referenceNo: payRef.trim() || undefined });
    setPayAmount(""); setPayRef("");
    await selectOrder(order);
    return "付款已登记，等待财务核验到账";
  });
  const requestReturn = () => run(async () => {
    if (!order) throw new Error("请先选择订单");
    const lines = retLines.split("\n").map((l) => l.trim()).filter(Boolean).map((l) => {
      const [itemId, spec, qty, sourceReceiptLineId] = l.split(",").map((s) => s.trim());
      return { itemId, spec: spec || "", quantity: Number(qty), ...(sourceReceiptLineId ? {sourceReceiptLineId} : {}) };
    });
    if (lines.length === 0) throw new Error("请填写退货明细");
    await api.post(`/api/v1/dealer-portal/orders/${encodeURIComponent(order.id)}/returns`, { idempotencyKey: key(), reasonType: retReason, lines });
    setRetLines("");
    await selectOrder(order);
    return "退货申请已提交，等待平台审批";
  });

  if (restoring) return <div className="loading-state" data-testid="dealer-portal-restoring">加载经销商门户...</div>;

  if (!user || !me) {
    return (
      <main className="dealer-portal" data-testid="dealer-portal-login">
        <h1>经销商门户</h1>
        <p className="admin-muted-action">独立经销商 Web 门户，企业、合同、开票和银行四类资料审核通过后，已启用的经销商门户账号才可登录。</p>
        {error ? <div className="admin-inline-error" data-testid="dealer-portal-error">{error}</div> : null}
        <form onSubmit={(e) => { e.preventDefault(); void onLogin(); }} className="admin-form-grid">
          <label>登录名<input data-testid="dealer-login-name" value={loginName} onChange={(e) => setLoginName(e.target.value)} autoComplete="username" /></label>
          <label>密码<input data-testid="dealer-login-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" /></label>
          <button type="submit" data-testid="dealer-login-submit">登录</button>
        </form>
      </main>
    );
  }

  return (
    <main className="dealer-portal" data-testid="dealer-portal-home">
      <header className="admin-form-grid">
        <h1>经销商门户：{me.dealerName}</h1>
        <span data-testid="dealer-portal-status">准入状态：{label(DEALER_STATUS, me.status)}{me.portalEnabled ? "（门户已启用）" : "（审核通过后启用门户功能）"}</span>
        <button type="button" data-testid="dealer-logout" onClick={() => void onLogout()}>退出登录</button>
      </header>
      {error ? <div className="admin-inline-error" data-testid="dealer-portal-error">{error}</div> : null}
      {notice ? <div className="admin-notice" data-testid="dealer-portal-notice">{notice}</div> : null}

      <section data-testid="dealer-profile-section">
        <h2>企业资料</h2>
        <div className="admin-form-grid">
          <span>法定代表人：{me.legalRepresentative || "—"}</span>
          <span>联系人：{me.contactName || "—"}</span>
          <span>联系电话：{me.contactPhone || "—"}</span>
        </div>
      </section>

      {me.portalEnabled ? (
        <>
          <section data-testid="dealer-contract-section">
            <h2>合同与库存</h2>
            <label>合同<select data-testid="contract-select" value={contractId} onChange={(e) => void selectContract(e.target.value)}>
              <option value="">请选择</option>{contracts.map((c) => <option key={c.id} value={c.id}>{c.contractNo ?? c.id}（{cnDate(c.validFrom)} 至 {cnDate(c.validTo)}，{c.creditTerm ? "账期" : "全款"}）</option>)}
            </select></label>
            {contractId ? (
              <table className="admin-table" data-testid="stock-table">
                <thead><tr><th>商品</th><th>规格</th><th>合同价</th><th>库存</th><th>预计交期</th></tr></thead>
                <tbody>
                  {stock.map((s) => (
                    <tr key={`${s.itemId}-${s.spec ?? ""}`} data-testid={`stock-row-${s.itemId}`}>
                      <td>{s.itemName ?? s.itemId}</td><td>{s.spec || "—"}</td><td>{money(s.unitPriceCent)}</td>
                      <td data-testid={`stock-availability-${s.itemId}`}>{label(AVAILABILITY, s.availability)}</td><td>{cnDate(s.expectedRestockDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : null}
          </section>

          <section data-testid="dealer-inquiry-section">
            <h2>询价（未定价商品）</h2>
            <div className="admin-form-grid">
              <label>商品编号<input data-testid="inquiry-item" value={inqItem} onChange={(e) => setInqItem(e.target.value)} /></label>
              <label>规格<input data-testid="inquiry-spec" value={inqSpec} onChange={(e) => setInqSpec(e.target.value)} /></label>
              <label>数量<input data-testid="inquiry-qty" value={inqQty} onChange={(e) => setInqQty(e.target.value)} /></label>
              <button type="button" data-testid="inquiry-submit" onClick={() => void createInquiry()}>提交询价</button>
            </div>
            <table className="admin-table" data-testid="inquiry-table">
              <thead><tr><th>商品</th><th>规格</th><th>数量</th><th>状态</th><th>报价</th></tr></thead>
              <tbody>{inquiries.map((q) => <tr key={q.id}><td>{q.itemId}</td><td>{q.spec || "—"}</td><td>{q.quantity}</td><td data-testid={`inquiry-status-${q.id}`}>{label(INQUIRY_STATUS, q.status)}</td><td>{money(q.quotedUnitPriceCent)}</td></tr>)}</tbody>
            </table>
          </section>

          <section data-testid="dealer-order-section">
            <h2>下单（已定合同价商品，或报价审批通过的询价单）</h2>
            <div className="admin-form-grid">
              <label>明细（每行：商品编号,规格,数量,询价单编号）<textarea data-testid="order-lines" rows={3} value={orderLines} onChange={(e) => setOrderLines(e.target.value)} /></label>
              <button type="button" data-testid="order-submit" onClick={() => void createOrder()}>提交订单</button>
            </div>
            <table className="admin-table" data-testid="order-table">
              <thead><tr><th>订单号</th><th>状态</th><th>原合计</th><th>退货后未付</th><th>已付</th><th>首付款门槛</th><th>首付款截止</th><th>尾款到期</th><th>操作</th></tr></thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.id}>
                    <td>{o.orderNo ?? o.id}</td><td data-testid={`order-status-${o.id}`}>{label(ORDER_STATUS, o.status)}</td><td>{money(o.totalAmountCent)}</td><td data-testid={`order-unpaid-${o.id}`}>{money(Math.max(0, (o.totalAmountCent ?? 0) - (o.returnedAmountCent ?? 0) - (o.deliveryAdjustmentCent ?? 0) - (o.firstLegAdjustmentCent ?? 0) - (o.paidAmountCent ?? 0)))}</td><td>{money(o.paidAmountCent)}</td>
                    <td>{money(o.firstPaymentRequiredCent)}</td><td>{cnDate(o.firstPaymentDeadlineAt)}</td><td>{o.balanceDueAt ? cnDate(o.balanceDueAt) : o.creditTerm ? "待合同约定事件确定" : "—"}</td>
                    <td><button type="button" data-testid={`order-select-${o.id}`} onClick={() => void run(() => selectOrder(o))}>查看</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {order ? (
              <div data-testid="order-detail">
                <h3>订单 {order.orderNo}：{label(ORDER_STATUS, order.status)}</h3>
                {["pending_payment", "payment_verifying", "pending_shipment", "shipping", "signed", "returning", "partially_returned", "returned"].includes(order.status ?? "") ? (
                  <div className="admin-form-grid" data-testid="payment-form">
                    <label>付款方式<select data-testid="pay-method" value={payMethod} onChange={(e) => setPayMethod(e.target.value)}><option value="bank_transfer">对公转账</option><option value="platform_online">平台线上支付</option></select></label>
                    <label>金额（元）<input data-testid="pay-amount" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} /></label>
                    <label>流水号<input data-testid="pay-ref" value={payRef} onChange={(e) => setPayRef(e.target.value)} /></label>
                    <button type="button" data-testid="pay-submit" onClick={() => void registerPayment()}>登记付款</button>
                    <span className="admin-muted-action">禁止现金和个人账户收款；到账以财务核验为准。</span>
                  </div>
                ) : null}
                {["shipping", "signed", "completed", "returning", "partially_returned", "returned"].includes(order.status ?? "") && <DealerDeliveryPanel key={order.id} api={api} orderId={order.id} status={order.status} portal canManage onChanged={async () => { await selectOrder(order); await loadDealer(); }} />}
                <table className="admin-table" data-testid="payment-table">
                  <thead><tr><th>方式</th><th>金额</th><th>状态</th><th>流水号</th></tr></thead>
                  <tbody>{payments.map((p) => <tr key={p.id}><td>{label(PAYMENT_METHOD, p.method)}</td><td>{money(p.amountCent)}</td><td data-testid={`payment-status-${p.id}`}>{label(PAYMENT_STATUS, p.status)}</td><td>{p.referenceNo ?? "—"}</td></tr>)}</tbody>
                </table>
                {["signed", "completed", "returning", "partially_returned"].includes(order.status ?? "") ? (
                  <div className="admin-form-grid" data-testid="return-form">
                    <label>退货原因<select data-testid="return-reason" value={retReason} onChange={(e) => setRetReason(e.target.value)}><option value="unopened_resalable">未拆封可复售</option><option value="quality_issue">质量问题</option></select></label>
                    <div>选择实际签收批次：{returnSources.filter(source => source.availableQuantity > 0).map(source => {
                      const line = actualLines.find(item => item.id === source.orderLineId);
                      return line ? <button type="button" key={source.receiptLineId} onClick={() => setRetLines(previous => `${previous}${previous ? "\n" : ""}${line.itemId},${line.spec},${source.availableQuantity},${source.receiptLineId}`)}>{line.itemId} {line.spec}，批次 {source.batchId}，可退 {source.availableQuantity}</button> : null;
                    })}</div>
                    <label>退货明细（每行：商品编号,规格,数量,签收来源）<textarea data-testid="return-lines" rows={2} value={retLines} onChange={(e) => setRetLines(e.target.value)} /></label>
                    <button type="button" data-testid="return-submit" onClick={() => void requestReturn()}>申请退货</button>
                  </div>
                ) : null}
                <table className="admin-table" data-testid="return-table">
                  <thead><tr><th>退货单</th><th>货款办理状态</th><th>原因</th><th>数量</th><th>货款应退</th><th>货款已退</th><th>首程运费冲减未收</th><th>首程运费应退</th><th>首程运费已退</th></tr></thead>
                  <tbody>{returns.map((r) => <tr key={r.id}><td>{r.returnNo ?? r.id}</td><td data-testid={`return-status-${r.id}`}>{label(RETURN_STATUS, r.status)}</td><td>{label(REASON, r.reasonType)}</td><td>{r.requestedQuantity}</td><td>{money(r.refundCent)}</td><td>{money(r.refundedCent)}</td><td>{money(returnFinancials.find(f=>f.returnId===r.id)?.firstLegOffsetCent)}</td><td>{money(returnFinancials.find(f=>f.returnId===r.id)?.firstLegRefundCent)}</td><td>{money(returnFinancials.find(f=>f.returnId===r.id)?.firstLegRefundedCent)}</td></tr>)}</tbody>
                </table>
              </div>
            ) : null}
          </section>
        </>
      ) : (
        <p className="admin-muted-action" data-testid="dealer-portal-locked">企业资料、合同、开票和银行资料审核通过后，开放合同、库存、询价、下单、付款、签收与退货功能。</p>
      )}
    </main>
  );
}
