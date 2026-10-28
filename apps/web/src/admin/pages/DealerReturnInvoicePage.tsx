// 文件版本：V0.03
import React from "react";
import { CounterpartyConfirmationPick } from "../../dealer/CounterpartyConfirmation";
import { DealerReturnFinancialPanel, DealerInvoiceActions, ReturnEvidenceUpload } from "./DealerReturnFinancialPanel";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

/**
 * 经销退货、退款、红冲与开票（ADM-DLR-003，CG-11c）。BSM:1238-1251：合同期限内仅未拆封可复售 / 质量问题商品可申请退货；审批后收货验收，
 * 按验收数量与原成交价确认退货金额；先冲减原订单未收货款，超出部分原路退还；可复售回可用库存、质量问题隔离禁售；已开票先全额红冲再按未退金额重开；
 * 签收后才可按最终未退金额申请电子发票；退货验收与退款执行不同人（ENUM:178）。
 */
type DealerRow = { id: string; dealerName?: string; status?: string };
type OrderRow = { id: string; orderNo?: string; status?: string; totalAmountCent?: number; paidAmountCent?: number; returnedAmountCent?: number; contractSnapshot?: { returnFreightBearer?: string | null; firstLegFreightRefundable?: boolean | null } | null };
type ReturnRow = {
  id: string; returnNo?: string; status?: string; reasonType?: string; requestedQuantity?: number; acceptedQuantity?: number; acceptedAmountCent?: number;
  offsetReceivableCent?: number; refundCent?: number; refundedCent?: number; refundReviewId?: string | null; refundApproved?: boolean; returnFreightBearer?: string;
  firstLegFreightRefundable?: boolean | null; freightConfirmed?: boolean; inspectedBy?: string | null; refundExecutedBy?: string | null; failureReason?: string | null;
};
type ReturnLineRow = { id: string; itemId?: string; spec?: string; requestedQuantity?: number; acceptedQuantity?: number; resalableQuantity?: number; quarantinedQuantity?: number; unitPriceCent?: number; acceptedAmountCent?: number };
type InvoiceRow = { id: string; amountCent?: number; status?: string; invoiceNo?: string | null; reversedByReturnId?: string | null; reissuedFromInvoiceId?: string | null; salesEntityId?: string; retainedGoodsCent?: number; retainedFreightCent?: number; requiresReversal?: boolean; reversalStatus?: string | null };

const ORDER_STATUS: Record<string, string> = {
  draft: "草稿", pending_review: "待审核", rejected: "已驳回", pending_payment: "待付款", payment_verifying: "付款核验中", pending_shipment: "待出库",
  shipping: "配送中", signed: "已签收", completed: "已完成", cancelling: "取消中", cancelled: "已取消", pending_refund: "待退款",
  returning: "退货中", partially_returned: "部分退货", returned: "已退货",
};
const RETURN_STATUS: Record<string, string> = {
  pending_approval: "待审批", rejected: "已驳回", pending_receipt: "待收货", inspecting: "验收中", pending_reversal: "待红冲", pending_refund: "待退款",
  refunding: "退款中", refund_failed: "退款失败", partially_completed: "部分完成", completed: "已完成",
};
const REASON: Record<string, string> = { unopened_resalable: "未拆封可复售", quality_issue: "质量问题" };
const FREIGHT_BEARER: Record<string, string> = { dealer: "经销商", sales_entity: "销售主体", pending_confirmation: "待双方确认" };
const INVOICE_STATUS: Record<string, string> = { pending: "待开票", issuing: "开票中", issued: "已开具", failed: "开票失败", reversed: "已红冲", voided: "已作废" };
const DEALER_STATUS: Record<string, string> = { pending_submission: "待提交", under_review: "审核中", approved: "已通过", rejected: "已驳回", deactivated: "已停用" };

function money(cent?: number | null) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}
function label(map: Record<string, string>, value?: string | null) {
  return value ? (map[value] ?? value) : "—";
}

export function DealerReturnInvoicePage(props: AdminPageProps) {
  const { api, setGlobalError, setActionResult, effectivePermissions } = props;
  const [dealers, setDealers] = React.useState<DealerRow[]>([]);
  const [dealer, setDealer] = React.useState<DealerRow | null>(null);
  const [orders, setOrders] = React.useState<OrderRow[]>([]);
  const [order, setOrder] = React.useState<OrderRow | null>(null);
  const [returns, setReturns] = React.useState<ReturnRow[]>([]);
  const [invoices, setInvoices] = React.useState<InvoiceRow[]>([]);
  const [ret, setRet] = React.useState<ReturnRow | null>(null);
  const [lines, setLines] = React.useState<ReturnLineRow[]>([]);
  const [formError, setFormError] = React.useState("");
  // 退货申请
  const [reason, setReason] = React.useState("unopened_resalable");
  const [reqLines, setReqLines] = React.useState("");
  const [reqNote, setReqNote] = React.useState("");
  const [reqConfirmation, setReqConfirmation] = React.useState("");
  // 第十八批第 31 条（BSM:1247）：经销商已停用后，退货申请须选指定经办人登记的对方确认留证。
  const deactivated = dealer?.status === "deactivated";
  // 验收
  const [inspLines, setInspLines] = React.useState("");
  // 运费确认
  const [inspectionEvidence, setInspectionEvidence] = React.useState<string[]>([]);
  const [sources,setSources] = React.useState<{ receiptLineId: string; orderLineId: string; batchId: string; availableQuantity: number }[]>([]);
  const [orderLines,setOrderLines] = React.useState<{id: string;itemId: string;spec: string}[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const rows = await api.get<DealerRow[]>("/api/v1/dealers");
      setDealers(rows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "经销商列表读取失败", onStart: () => setGlobalError(null) },
  );

  const key = () => `drt-${Date.now()}-${Math.random().toString(16).slice(2)}`;

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
    setDealer(row); setOrder(null); setReturns([]); setInvoices([]); setRet(null); setLines([]);
    try {
      const os = await api.get<OrderRow[]>(`/api/v1/dealers/${encodeURIComponent(row.id)}/orders`);
      setOrders(os ?? []);
    } catch (e) { setFormError(e instanceof Error ? e.message : "订单读取失败"); }
  }
  async function selectOrder(row: OrderRow) {
    setOrder(row); setRet(null); setLines([]);
    await loadOrder(row.id);
  }
  async function loadOrder(id: string) {
    try {
      const [o, rs, vs, ss, ol] = await Promise.all([
        api.get<OrderRow>(`/api/v1/dealers/orders/${encodeURIComponent(id)}`),
        api.get<ReturnRow[]>(`/api/v1/dealers/orders/${encodeURIComponent(id)}/returns`),
        api.get<InvoiceRow[]>(`/api/v1/dealers/orders/${encodeURIComponent(id)}/invoices`),
        api.get<typeof sources>(`/api/v1/dealers/orders/${encodeURIComponent(id)}/return-sources`),
        api.get<typeof orderLines>(`/api/v1/dealers/orders/${encodeURIComponent(id)}/lines`),
      ]);
      setOrder(o); setReturns(rs ?? []); setInvoices(vs ?? []); setSources(ss ?? []); setOrderLines(ol ?? []);
    } catch (e) { setFormError(e instanceof Error ? e.message : "订单退货与发票读取失败"); }
  }
  async function selectReturn(row: ReturnRow) {
    setRet(row); setInspectionEvidence([]);
    await loadReturn(row.id);
  }
  async function loadReturn(id: string) {
    try {
      const [r, ls] = await Promise.all([
        api.get<ReturnRow>(`/api/v1/dealers/returns/${encodeURIComponent(id)}`),
        api.get<ReturnLineRow[]>(`/api/v1/dealers/returns/${encodeURIComponent(id)}/lines`),
      ]);
      setRet(r); setLines(ls ?? []);
      if (r.status === "inspecting") setInspLines((ls ?? []).map(line => `${line.itemId},${line.spec ?? ""},0,0,0,,${line.id}`).join("\n"));
      if (order) await loadOrder(order.id);
    } catch (e) { setFormError(e instanceof Error ? e.message : "退货单读取失败"); }
  }

  const parseLines = (text: string, cols: number, what: string) => text.split("\n").map((l) => l.trim()).filter(Boolean).map((l) => {
    const parts = l.split(",").map((s) => s.trim());
    if (parts.length < cols) throw new Error(`${what}每行须为 ${cols} 列`);
    return parts;
  });

  const requestReturn = () => run("dealer_return_requested", async () => {
    if (!order) throw new Error("请先选择订单");
    const parsed = parseLines(reqLines, 3, "退货明细").map(([itemId, spec, qty, sourceReceiptLineId]) => ({ itemId, spec: spec || "", quantity: Number(qty), ...(sourceReceiptLineId ? {sourceReceiptLineId} : {}) }));
    if (parsed.length === 0) throw new Error("请填写退货明细");
    if (deactivated && !reqConfirmation) throw new Error("经销商已停用，请先选择指定经办人登记的对方确认留证");
    const r = await api.post<ReturnRow>(`/api/v1/dealers/orders/${encodeURIComponent(order.id)}/returns`, { idempotencyKey: key(), reasonType: reason, lines: parsed, note: reqNote.trim() || undefined, ...(deactivated ? { counterpartyConfirmationId: reqConfirmation } : {}) });
    setReqLines(""); setReqNote(""); setReqConfirmation("");
    await loadOrder(order.id);
    return { 退货单: r?.returnNo, 原因: label(REASON, reason), 退回运费承担方: label(FREIGHT_BEARER, r?.returnFreightBearer) };
  });

  const act = (path: string, body: Record<string, unknown>, action: string, result: Record<string, unknown>) => run(action, async () => {
    await api.post(path, { idempotencyKey: key(), ...body });
    if (ret) await loadReturn(ret.id);
    else if (order) await loadOrder(order.id);
    return result;
  });

  const inspect = () => run("dealer_return_inspected", async () => {
    if (!ret) throw new Error("请先选择退货单");
    const parsed = parseLines(inspLines, 5, "验收明细").map(([itemId, spec, accepted, resalable, quarantined, note, returnLineId]) => ({
      itemId, spec: spec || "", acceptedQuantity: Number(accepted), resalableQuantity: Number(resalable), quarantinedQuantity: Number(quarantined), note: note || undefined, ...(returnLineId ? {returnLineId} : {}),
    }));
    if (!inspectionEvidence.length) throw new Error("请上传实际验收凭证");
    const r = await api.post<ReturnRow>(`/api/v1/dealers/returns/${encodeURIComponent(ret.id)}/inspection`, { idempotencyKey: key(), lines: parsed, evidenceAttachmentIds: inspectionEvidence });
    setInspLines("");
    await loadReturn(ret.id);
    return { 退货单: r?.returnNo, 验收金额: money(r?.acceptedAmountCent), 冲减未收货款: money(r?.offsetReceivableCent), 待退款: money(r?.refundCent), 状态: label(RETURN_STATUS, r?.status) };
  });

  const prompt = (text: string) => window.prompt(text) ?? "";

  return (
    <section data-testid="dealer-returns-page">
      <AdminPageHeader
        title="经销退货与开票"
        description="合同期限内仅未拆封可复售或质量问题商品可申请退货；审批通过后收货验收，按实际验收数量与原成交价确认退货金额；退货金额先冲减原订单未收货款，超出部分经既有退款审批原路退还；可复售品回可用库存、质量问题隔离禁售；已开票退货先全额红冲再按最新未退金额重开，全部退货不重开；订单签收后才可按最终未退金额申请电子发票；退货验收与退款执行不同人。"
      />
      <AuditTrailHint event="dealer.return.requested / dealer.return.decided / dealer.return.inspected / dealer.invoice.reversal_result_recorded / dealer.return.refund_result_recorded / dealer.invoice.issued" />
      <p className="admin-muted-action" data-testid="dealer-returns-evidence">接口证据：GET/POST /api/v1/dealers/orders/{"{id}"}/returns；POST /api/v1/dealers/returns/{"{id}"}/decision｜receive｜inspection｜invoice-reversal｜freight-confirmation｜refunds/execute；GET/POST /api/v1/dealers/orders/{"{id}"}/invoices；POST /api/v1/dealers/invoices/{"{id}"}/issue</p>
      {formError ? <div data-testid="dealer-returns-error" className="admin-inline-error">{formError}</div> : null}

      <AdminDataState loading={loading} error={error} empty={dealers.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="dealer-returns-dealer-table">
            <thead><tr><th>经销商</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {dealers.map((row) => (
                <tr key={row.id} className={dealer?.id === row.id ? "admin-row-selected" : undefined}>
                  <td>{row.dealerName ?? row.id}</td><td>{label(DEALER_STATUS, row.status)}</td>
                  <td><button type="button" data-testid={`dealer-returns-select-${row.id}`} onClick={() => void selectDealer(row)}>选中</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>

      {dealer ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="return-order-table">
            <thead><tr><th>订单号</th><th>状态</th><th>合计</th><th>已收</th><th>已退货金额</th><th>操作</th></tr></thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.id} className={order?.id === o.id ? "admin-row-selected" : undefined}>
                  <td>{o.orderNo ?? o.id}</td><td data-testid={`return-order-status-${o.id}`}>{label(ORDER_STATUS, o.status)}</td>
                  <td>{money(o.totalAmountCent)}</td><td>{money(o.paidAmountCent)}</td><td>{money(o.returnedAmountCent)}</td>
                  <td><button type="button" data-testid={`return-order-select-${o.id}`} onClick={() => void selectOrder(o)}>处理</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {order ? (
        <>
          <h3>退货申请：订单 {order.orderNo}（{label(ORDER_STATUS, order.status)}）</h3>
          <div className="admin-form-grid" data-testid="return-request-form">
            <label>退货原因<select data-testid="return-reason" value={reason} onChange={(e) => setReason(e.target.value)}>
              <option value="unopened_resalable">未拆封可复售</option><option value="quality_issue">质量问题</option>
            </select></label>
            <div>选择实际签收来源：{sources.filter(source => source.availableQuantity > 0).map(source => {
              const line = orderLines.find(item => item.id === source.orderLineId);
              return line ? <button type="button" key={source.receiptLineId} onClick={() => setReqLines(previous => `${previous}${previous ? "\n" : ""}${line.itemId},${line.spec},${source.availableQuantity},${source.receiptLineId}`)}>{line.itemId} {line.spec}，批次 {source.batchId}，可退 {source.availableQuantity}</button> : null;
            })}</div>
            <label>退货明细（每行：商品id,规格,数量,签收来源）<textarea data-testid="return-lines-input" rows={3} value={reqLines} onChange={(e) => setReqLines(e.target.value)} /></label>
            <label>说明<input data-testid="return-note" value={reqNote} onChange={(e) => setReqNote(e.target.value)} /></label>
            {deactivated ? <CounterpartyConfirmationPick key={order.id} api={api} orderId={order.id} matterType="return_request" value={reqConfirmation} onChange={setReqConfirmation} /> : null}
            <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.manage"]} data-testid="return-request" onClick={() => void requestReturn()}>提交退货申请</PermissionButton>
          </div>

          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="return-table">
              <thead><tr><th>退货单号</th><th>状态</th><th>原因</th><th>申请数量</th><th>验收数量</th><th>退货金额</th><th>冲减未收货款</th><th>待退款</th><th>已退款</th><th>退回运费承担方</th><th>操作</th></tr></thead>
              <tbody>
                {returns.map((r) => (
                  <tr key={r.id} className={ret?.id === r.id ? "admin-row-selected" : undefined}>
                    <td>{r.returnNo ?? r.id}</td><td data-testid={`return-status-${r.id}`}>{label(RETURN_STATUS, r.status)}</td><td data-testid={`return-reason-${r.id}`}>{label(REASON, r.reasonType)}</td>
                    <td>{r.requestedQuantity ?? "—"}</td><td>{r.acceptedQuantity ?? "—"}</td><td>{money(r.acceptedAmountCent)}</td><td>{money(r.offsetReceivableCent)}</td>
                    <td>{money(r.refundCent)}</td><td>{money(r.refundedCent)}</td><td data-testid={`return-bearer-${r.id}`}>{label(FREIGHT_BEARER, r.returnFreightBearer)}</td>
                    <td><button type="button" data-testid={`return-select-${r.id}`} onClick={() => void selectReturn(r)}>处理</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {ret ? (
            <>
              <h3>退货单 {ret.returnNo}：{label(RETURN_STATUS, ret.status)}</h3>
              <div className="admin-form-grid" data-testid="return-actions">
                {ret.status === "pending_approval" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.review"]} data-testid="return-approve" onClick={() => void act(`/api/v1/dealers/returns/${encodeURIComponent(ret.id)}/decision`, { decision: "approve", note: prompt("审批意见") || undefined }, "dealer_return_decided", { 结论: "通过", 状态: "待收货" })}>审批通过</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.review"]} data-testid="return-reject" onClick={() => void act(`/api/v1/dealers/returns/${encodeURIComponent(ret.id)}/decision`, { decision: "reject", note: prompt("驳回原因") || undefined }, "dealer_return_decided", { 结论: "驳回" })}>驳回</PermissionButton>
                  </>
                ) : null}
                {ret.status === "pending_receipt" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.inspect"]} data-testid="return-receive" onClick={() => void act(`/api/v1/dealers/returns/${encodeURIComponent(ret.id)}/receive`, {}, "dealer_return_received", { 状态: "验收中" })}>确认收货</PermissionButton> : null}
                {ret.status === "pending_reversal" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.invoice.manage"]} data-testid="return-reverse-invoice" onClick={() => void act(`/api/v1/dealers/returns/${encodeURIComponent(ret.id)}/invoice-reversal`, {}, "dealer_invoice_reversal_requested", { 动作: "原发票待红冲，收到实际成功结果后重开" })}>提交原发票红冲</PermissionButton> : null}
                {(ret.status === "pending_refund" || ret.status === "refund_failed") && !ret.refundApproved ? <span data-testid="return-refund-waiting">退款审批中（复核单 {ret.refundReviewId ?? "未生成"}）</span> : null}
              </div>

              {ret.status === "inspecting" ? (
                <div className="admin-form-grid" data-testid="return-inspection-form">
                  <label>验收明细（每行：商品id,规格,验收数量,可复售数量,隔离数量,备注,退货明细）<textarea data-testid="inspection-lines-input" rows={3} value={inspLines} onChange={(e) => setInspLines(e.target.value)} /></label>
                  <ReturnEvidenceUpload key={ret.id} api={api} effectivePermissions={effectivePermissions} type="dealer_return" id={ret.id} permission="dealer.return.inspect" testId="inspection-evidence" onChanged={setInspectionEvidence} />
                  <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.inspect"]} data-testid="return-inspect" onClick={() => void inspect()}>确认验收（可复售回库，隔离禁售）</PermissionButton>
                </div>
              ) : null}

              <DealerReturnFinancialPanel key={`${ret.id}-${ret.status}`} {...props} returnId={ret.id} status={ret.status} refundApproved={ret.refundApproved} bearer={ret.returnFreightBearer} refundable={ret.firstLegFreightRefundable} reasonType={ret.reasonType} contract={order?.contractSnapshot} orderId={order?.id} deactivated={deactivated} onChanged={() => {void loadReturn(ret.id);}} />

              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="return-line-table">
                  <thead><tr><th>商品</th><th>规格</th><th>申请数量</th><th>验收数量</th><th>可复售（回库）</th><th>隔离禁售</th><th>原成交价</th><th>退货金额</th></tr></thead>
                  <tbody>
                    {lines.map((l) => (
                      <tr key={l.id}>
                        <td>{l.itemId}</td><td>{l.spec || "—"}</td><td>{l.requestedQuantity}</td><td>{l.acceptedQuantity}</td><td>{l.resalableQuantity}</td><td>{l.quarantinedQuantity}</td>
                        <td>{money(l.unitPriceCent)}</td><td>{money(l.acceptedAmountCent)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : null}

          <h3>电子发票（签收后按最终未退金额开具，主体为合同销售主体）</h3>
          <div className="admin-form-grid" data-testid="invoice-form">
            <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.invoice.manage"]} data-testid="invoice-request" onClick={() => void act(`/api/v1/dealers/orders/${encodeURIComponent(order.id)}/invoices`, {}, "dealer_invoice_requested", { 订单: order.orderNo, 办理状态: "按实际未退货款及未退运费核定" })}>申请电子发票</PermissionButton>
          </div>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="invoice-table">
              <thead><tr><th>发票</th><th>金额</th><th>状态</th><th>发票号</th><th>红冲退货单</th><th>重开自</th><th>操作</th></tr></thead>
              <tbody>
                {invoices.map((v) => (
                  <tr key={v.id}>
                    <td>{v.id}</td><td>{money(v.amountCent)}</td><td data-testid={`invoice-status-${v.id}`}>{label(INVOICE_STATUS, v.status)}</td><td>{v.invoiceNo ?? "—"}</td>
                    <td>{v.reversedByReturnId ?? "—"}</td><td>{v.reissuedFromInvoiceId ?? "—"}</td>
                    <td><DealerInvoiceActions key={`${v.id}-${v.status}`} {...props} invoice={v} onChanged={() => {void loadOrder(order.id);if(ret) void loadReturn(ret.id);}} />{v.requiresReversal ? <p>须红冲原票后重开</p> : null}</td>
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
