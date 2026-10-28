package com.complex.platform.dealer;

// 文件版本：V0.07

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerInvoiceIssueRequest;
import com.complex.platform.dealer.dto.DealerInvoiceRequest;
import com.complex.platform.dealer.dto.DealerReturnDecisionRequest;
import com.complex.platform.dealer.dto.DealerReturnFreightConfirmRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionLineRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionRequest;
import com.complex.platform.dealer.dto.DealerReturnLineRequest;
import com.complex.platform.dealer.dto.DealerReturnRefundResultRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-11c 经销退货、退款、红冲与资金归属（BSM:1238-1251）。
 * <ul>
 *   <li>合同期限内仅未拆封可复售 / 质量问题商品可申请退货；审批通过后收货验收，按实际验收数量与原成交价确认退货金额，支持部分退货（BSM:1240）。</li>
 *   <li>退货金额先冲减原订单未收货款，超过未收货款的部分才原路退还已收款；不跨订单抵扣；可复售回可用库存，质量问题隔离禁售（BSM:1240）。</li>
 *   <li>已达包邮不倒追首程运费；质量问题退回运费由销售主体承担；非质量按合同约定，未约定先由双方确认；首程运费退不退按合同约定（BSM:1240）。</li>
 *   <li>已开票部分退货先全额红冲原发票，再按最新未退金额重开；全部退货红冲后不重开；签收后才可按最终未退金额申请电子发票，不以已收款封顶（BSM:1242）。</li>
 *   <li>退款走既有退款审批，只有成功的退款才计入已退金额；退货验收与退款执行分离（ENUM:178）；退货单十态（ENUM:174）。</li>
 *   <li>资金归属：发票与应收主体均为订单固化的合同销售主体（ENUM:182），不计门店商户营业额。</li>
 * </ul>
 */
@Service
public class DealerReturnService {

    static final String VIEW = "dealer.return.view";
    static final String MANAGE = "dealer.return.manage";
    static final String REVIEW = "dealer.return.review";
    static final String INSPECT = "dealer.return.inspect";
    static final String REFUND_EXECUTE = "dealer.return.refund.execute";
    static final String INVOICE_MANAGE = "dealer.invoice.manage";

    static final Set<String> REASONS = Set.of("unopened_resalable", "quality_issue");
    static final Set<String> RETURNABLE_ORDER_STATUSES = Set.of("signed", "completed", "returning", "partially_returned");
    static final String REFUND_ACTION = "refund";
    static final String REFUND_OBJECT = "dealer_return";

    private final JdbcDealerReturnRepository returns;
    private final JdbcDealerOrderRepository orders;
    private final JdbcDealerDeliveryRepository delivery;
    private final JdbcDealerRepository dealers;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final SupervisorReviewService reviews;
    private final DealerReturnFinancialService financial;
    private final com.complex.platform.inventory.InventoryQuarantineService quarantine;
    private final DealerCounterpartyConfirmationService confirmations;

    public DealerReturnService(JdbcDealerReturnRepository returns, JdbcDealerOrderRepository orders, JdbcDealerRepository dealers,
            AuthorizationService authorization, IdempotencyService idempotency, AuditEventService audit, SupervisorReviewService reviews, JdbcDealerDeliveryRepository delivery, DealerReturnFinancialService financial, com.complex.platform.inventory.InventoryQuarantineService quarantine,
            DealerCounterpartyConfirmationService confirmations) {
        this.confirmations = confirmations;
        this.returns = returns;
        this.orders = orders;
        this.delivery = delivery;
        this.dealers = dealers;
        this.authorization = authorization;
        this.idempotency = idempotency;
        this.audit = audit;
        this.reviews = reviews;
        this.financial=financial;this.quarantine=quarantine;
    }

    // ── 查询 ──

    public DealerReturnRecord getReturn(String returnId) {
        authorization.requirePermission(VIEW);
        return requireReturn(returnId);
    }

    public PageResponse<DealerReturnRecord> listReturns(String orderId) {
        authorization.requirePermission(VIEW);
        return listReturns(orderId, VIEW);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    PageResponse<DealerReturnRecord> listReturns(String orderId, String permission) {
        requireOrder(orderId);
        return page(returns.listReturns(orderId));
    }

    public PageResponse<DealerReturnLineRecord> listReturnLines(String returnId) {
        authorization.requirePermission(VIEW);
        requireReturn(returnId);
        return page(returns.listLines(returnId));
    }

    public PageResponse<DealerInvoiceRecord> listInvoices(String orderId) {
        authorization.requirePermission(VIEW);
        return listInvoices(orderId, VIEW);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    PageResponse<DealerInvoiceRecord> listInvoices(String orderId, String permission) {
        requireOrder(orderId);
        return page(returns.listInvoices(orderId));
    }

    // ── 退货申请 / 审批 ──

    /** BSM:1240：合同期限内、仅两类原因、签收后、数量不超过已出库未退（含在途退货）数量。 */
    @Transactional
    public DealerReturnRecord requestReturn(String orderId, DealerReturnRequest request) {
        authorization.requirePermission(MANAGE);
        return requestReturn(orderId, request, MANAGE);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    DealerReturnRecord requestReturn(String orderId, DealerReturnRequest request, String permission) {
        try {
            requireOrder(orderId);
            if (MANAGE.equals(permission)) {
                String project=orders.findOrderProjectId(orderId).orElse(null);
                if(project==null || !authorization.canReachProject(authorization.currentActor(),project,permission))
                    throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","退货申请不在授权项目范围内");
            }
            return idempotency.execute("dealerReturnRequest:" + orderId, request == null ? null : request.idempotencyKey(), request, () -> {
                DealerOrderRecord order=orders.lockOrder(orderId).orElseThrow();
                // 第十八批第 31 条：经销商已停用时退货申请须用掉一条退货类对方确认（门户在停用后已拒绝，不会走到这里）。
                confirmations.consumeIfDeactivated(orderId, order.dealerId(), "return_request", request == null ? null : request.counterpartyConfirmationId());
                if (!RETURNABLE_ORDER_STATUSES.contains(order.status())) {
                    throw ApiException.conflict("DEALER_ORDER_STATUS_INVALID", "订单签收后才可申请退货：" + order.status());
                }
                String reason = requireText(request == null ? null : request.reasonType(), "reason_type").trim().toLowerCase(Locale.ROOT);
                if (!REASONS.contains(reason)) {
                    throw ApiException.badRequest("RETURN_REASON_NOT_ALLOWED", "合同期限内仅未拆封可复售商品或质量问题商品可以申请退货");
                }
                DealerContractRecord contract = order.contractSnapshot();
                if (contract == null) throw ApiException.conflict("DEALER_CONTRACT_SNAPSHOT_REQUIRED", "原订单缺少锁定合同快照，须先核实原合同事实");
                if (contract.validTo().isBefore(LocalDate.now())) {
                    throw ApiException.conflict("DEALER_CONTRACT_EXPIRED", "合同期限已过，不得申请退货");
                }
                if (request.lines() == null || request.lines().isEmpty()) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "退货至少一条明细");
                }
                List<DealerOrderLineRecord> orderLines = orders.listLines(orderId);
                List<DealerReturnLineRecord> lines = new ArrayList<>();
                int requested = 0;
                var sourceRemaining=new HashMap<String,Integer>();
                var sources=returns.returnSources(orderId);
                sources.forEach(v->sourceRemaining.put(v.receiptLineId(),v.availableQuantity()));
                String returnId = "dret_" + UUID.randomUUID();
                for (DealerReturnLineRequest line : request.lines()) {
                    if (line == null || line.itemId() == null || line.quantity() == null || line.quantity() <= 0) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "每条退货明细须填写商品与大于 0 的数量");
                    }
                    String spec = line.spec() == null ? "" : line.spec().trim();
                    DealerOrderLineRecord orderLine = orderLines.stream().filter(l -> l.itemId().equals(line.itemId().trim()) && l.spec().equals(spec)).findFirst()
                            .orElseThrow(() -> ApiException.badRequest("RETURN_LINE_NOT_IN_ORDER", "退货商品不在原订单内：" + line.itemId()));
                    int returnable = delivery.acceptedQuantity(orderId, orderLine.id()) - orderLine.returnedQuantity() - returns.pendingReturnQuantity(orderLine.id());
                    if (line.quantity() > returnable) {
                        throw ApiException.conflict("RETURN_QUANTITY_EXCEEDS", "退货数量超过实际合格接收且尚未退回的数量：" + line.itemId());
                    }
                    var matching=sources.stream().filter(v->v.orderLineId().equals(orderLine.id()) && sourceRemaining.get(v.receiptLineId())>0
                            && (line.sourceReceiptLineId()==null || line.sourceReceiptLineId().equals(v.receiptLineId()))).toList();
                    if(matching.size()!=1) throw ApiException.conflict("DEALER_RETURN_SOURCE_REQUIRED","存在多个实际签收批次，请逐批选择退回来源和数量");
                    String source=matching.getFirst().receiptLineId();
                    if(line.quantity()>sourceRemaining.get(source)) throw ApiException.conflict("RETURN_QUANTITY_EXCEEDS","退回数量超过本批实际签收未退数量");
                    sourceRemaining.put(source,sourceRemaining.get(source)-line.quantity());
                    requested += line.quantity();
                    lines.add(new DealerReturnLineRecord("drl_" + UUID.randomUUID(), returnId, orderLine.id(), orderLine.itemId(), spec, line.quantity(), 0, 0, 0,
                            orderLine.unitPriceCent(), 0L, null,source));
                }
                int total = requested;
                Instant now = Instant.now();
                // 退回运费承担方：质量问题＝销售主体；非质量＝合同约定，未约定＝待双方确认
                String bearer = "quality_issue".equals(reason) ? "sales_entity" : (contract.returnFreightBearer() == null ? "pending_confirmation" : contract.returnFreightBearer());
                DealerReturnRecord record = new DealerReturnRecord(returnId, "DR-" + now.toEpochMilli() + "-" + returnId.substring(5, 9).toUpperCase(Locale.ROOT),
                        orderId, order.dealerId(), order.salesEntityId(), "pending_approval", reason, total, 0, 0L, 0L, 0L, 0L, null, false, bearer,
                        contract.firstLegFreightRefundable(), !"pending_confirmation".equals(bearer) && contract.firstLegFreightRefundable() != null,
                        null, null, null, null, null, null, null, null, null, null, trim(request.note()), actorId(), now, now);
                returns.saveReturn(record);
                lines.forEach(returns::saveLine);
                audit.recordSuccess("dealer.return.requested", permission, "dealer_return", returnId, null, request.idempotencyKey(),
                        meta("order_id", orderId, "reason_type", reason, "requested_quantity", total, "return_freight_bearer", bearer));
                return record;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.return.request_failed", permission, "dealer_return", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 审批：审批人≠申请人；通过 → 待收货、订单转退货中；驳回 → 已驳回。 */
    @Transactional
    public DealerReturnRecord decideReturn(String returnId, DealerReturnDecisionRequest request) {
        authorization.requirePermission(REVIEW);
        try {
            DealerReturnRecord ret = requireReturn(returnId);
            requireStatus(ret, "pending_approval");
            String decision = requireDecision(request == null ? null : request.decision());
            if (Objects.equals(actorId(), ret.createdBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "退货申请人不得审批自己的退货");
            }
            return idempotency.execute("dealerReturnDecide:" + returnId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                boolean approve = "approve".equals(decision);
                DealerReturnRecord saved = with(ret, approve ? "pending_receipt" : "rejected").toBuilderApproved(actorId(), now);
                returns.saveReturn(saved);
                if (approve) {
                    orders.updateOrderStatus(ret.orderId(), "returning");
                }
                audit.recordSuccess("dealer.return.decided", REVIEW, "dealer_return", returnId, null, request.idempotencyKey(),
                        meta("decision", decision, "note", nz(request.note())));
                return saved;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.return.decide_failed", REVIEW, "dealer_return", returnId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 收货 / 验收 ──

    @Transactional
    public DealerReturnRecord receiveReturn(String returnId, String idempotencyKey) {
        authorization.requirePermission(INSPECT);
        try {
            DealerReturnRecord ret = requireReturn(returnId);
            requireStatus(ret, "pending_receipt");
            return idempotency.execute("dealerReturnReceive:" + returnId, idempotencyKey, Map.of("id", returnId), () -> {
                DealerReturnRecord saved = with(ret, "inspecting").toBuilderReceived(actorId(), Instant.now());
                returns.saveReturn(saved);
                audit.recordSuccess("dealer.return.received", INSPECT, "dealer_return", returnId, null, idempotencyKey, meta());
                return saved;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.return.receive_failed", INSPECT, "dealer_return", returnId, null, idempotencyKey, codeOf(e));
            throw e;
        }
    }

    /**
     * 验收：按实际验收数量 × 原成交价确认退货金额（原签收行含已完成减价留用的，按该行净额÷实收件数分摊到每件、尾差落最后一件，减价不重复退——2026-10-18 经销 D03 裁决）；
     * 可复售回可用库存、质量问题隔离禁售；先冲减未收货款、超出部分待退款；
     * 已开票 → 待红冲；未开票 → 退款 > 0 待退款（开既有退款审批），否则直接结清。
     */
    @Transactional
    public DealerReturnRecord inspectReturn(String returnId, DealerReturnInspectionRequest request) {
        authorization.requirePermission(INSPECT);
        try {
            DealerReturnRecord initial = requireReturn(returnId);
            requireStatus(initial, "inspecting");
            if (request == null || request.lines() == null || request.lines().isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "验收至少一条明细");
            }
            DealerOrderRecord candidate = requireOrder(initial.orderId());
            dealers.lockDealerCredit(candidate.dealerId()).orElseThrow();
            DealerOrderRecord order = orders.lockOrder(initial.orderId()).orElseThrow();
            DealerReturnRecord ret = requireReturn(returnId);
            requireStatus(ret, "inspecting");
            List<DealerReturnLineRecord> lines = returns.listLines(returnId);
            List<DealerReturnLineRecord> inspected = new ArrayList<>();
            var seen=new java.util.HashSet<String>();
            for (DealerReturnInspectionLineRequest in : request.lines()) {
                String spec = in.spec() == null ? "" : in.spec().trim();
                var matching=lines.stream().filter(l->l.itemId().equals(in.itemId()) && l.spec().equals(spec)
                        && (in.returnLineId()==null || in.returnLineId().equals(l.id()))).toList();
                if(matching.size()!=1) throw ApiException.badRequest("RETURN_LINE_NOT_FOUND","请选择具体退货明细及原签收来源");
                DealerReturnLineRecord line=matching.getFirst();
                if(!seen.add(line.id())) throw ApiException.badRequest("VALIDATION_FAILED","同一退货明细不能重复验收");
                int accepted = in.acceptedQuantity() == null ? 0 : in.acceptedQuantity();
                int resalable = in.resalableQuantity() == null ? 0 : in.resalableQuantity();
                int quarantined = in.quarantinedQuantity() == null ? 0 : in.quarantinedQuantity();
                if (accepted < 0 || resalable < 0 || quarantined < 0 || accepted > line.requestedQuantity() || resalable + quarantined != accepted) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "验收数量须不超过申请数量，且可复售 + 隔离 = 验收数量");
                }
                long unit = line.unitPriceCent(), amount = unit * accepted;
                if (accepted > 0 && line.sourceReceiptLineId() != null) {
                    // 经销 D03：签收行净额＝实收件数×原成交价－已完成减价留用的减价；÷实收件数分摊到每件，尾差落该行最后一件退回时
                    var net = returns.receiptLineNet(line.sourceReceiptLineId(), returnId);
                    long netCent = net.receivedQuantity() * unit - net.discountCent();
                    unit = netCent / net.receivedQuantity();
                    int end = net.returnedQuantity() + accepted;
                    amount = (end == net.receivedQuantity() ? netCent : unit * end) - unit * net.returnedQuantity();
                }
                inspected.add(new DealerReturnLineRecord(line.id(), returnId, line.orderLineId(), line.itemId(), line.spec(), line.requestedQuantity(), accepted,
                        resalable, quarantined, unit, amount, trim(in.note()),line.sourceReceiptLineId()));
            }
            if(seen.size()!=lines.size()) throw ApiException.badRequest("VALIDATION_FAILED","须逐条记录所有退货明细的实际验收结果，未接收明确填零");
            return idempotency.execute("dealerReturnInspect:" + returnId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                String actor = actorId();
                int acceptedQty = 0;
                long acceptedAmount = 0;
                for (DealerReturnLineRecord line : inspected) {
                    returns.saveLine(line);
                    acceptedQty += line.acceptedQuantity();
                    acceptedAmount += line.acceptedAmountCent();
                    if(line.acceptedQuantity()>0) quarantine.receiveDealerReturnLine(line.id(),now,request.evidenceAttachmentIds());
                    if (line.acceptedQuantity() > 0) {
                        orders.addReturnedQuantity(line.orderLineId(), line.acceptedQuantity());
                    }
                }
                if (acceptedAmount > order.netReceivableCent()) {
                    throw ApiException.conflict("DEALER_RETURN_AMOUNT_EXCEEDS_NET", "退货金额超过差异调整后的原单净额，须核实已批准价格依据");
                }
                long unpaid = Math.max(0, order.netReceivableCent() - order.paidAmountCent());
                long offset = Math.min(acceptedAmount, unpaid);
                long refund = acceptedAmount - offset;
                if (acceptedAmount > 0 && order.arApItemId() != null) {
                    returns.offsetReceivable(order.arApItemId(), acceptedAmount);
                }
                orders.applyReturn(order.id(), acceptedAmount, "returning");
                if (order.overdue() && !orders.dealerHasOverdueOrders(order.dealerId())) {
                    dealers.setCreditSuspended(order.dealerId(), false);
                    audit.recordSuccess("dealer.credit_resumed", INSPECT, "dealer", order.dealerId(), null, null,
                            meta("order_id", order.id(), "reason", "退货冲清全部逾期应收"));
                }
                boolean invoiced = returns.findActiveInvoice(order.id()).map(v -> "issued".equals(v.status())).orElse(false);
                String next = invoiced ? "pending_reversal" : (refund > 0 ? "pending_refund" : finalStatus(ret.requestedQuantity(), acceptedQty));
                String reviewId = null;
                boolean approved = false;
                if (!invoiced && refund > 0) {
                    reviewId = openRefundReview(ret, refund, request.idempotencyKey());
                }
                DealerReturnRecord saved = new DealerReturnRecord(ret.id(), ret.returnNo(), ret.orderId(), ret.dealerId(), ret.salesEntityId(), next, ret.reasonType(),
                        ret.requestedQuantity(), acceptedQty, acceptedAmount, offset, refund, 0L, reviewId, approved, ret.returnFreightBearer(),
                        ret.firstLegFreightRefundable(), ret.freightConfirmed(), ret.approvedBy(), ret.approvedAt(), ret.receivedBy(), ret.receivedAt(), actor, now,
                        null, null, null, null, ret.note(), ret.createdBy(), ret.createdAt(), now);
                returns.saveReturn(saved);
                financial.goodsAccepted(order.id());
                if (!invoiced && refund == 0) {
                    finalizeOrderStatus(order.id());
                }
                audit.recordSuccess("dealer.return.inspected", INSPECT, "dealer_return", returnId, null, request.idempotencyKey(),
                        meta("accepted_quantity", acceptedQty, "accepted_amount_cent", acceptedAmount, "offset_receivable_cent", offset, "refund_cent", refund,
                                "next_status", next, "invoiced", invoiced));
                return saved;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.return.inspect_failed", INSPECT, "dealer_return", returnId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 红冲 / 重开 ──

    /** BSM:1242：已开票退货先全额红冲原发票，再按最新未退金额重开；全部退货红冲后不重开。随后进入退款或结清。 */
    @Transactional
    public DealerReturnRecord reverseInvoice(String returnId, DealerInvoiceIssueRequest request) {
        return financial.requestReturnReversal(returnId,request);
    }

    // ── 运费确认 ──

    /** BSM:1240：合同未约定时由双方确认退回运费承担方与首程运费退不退（留痕）。 */
    @Transactional
    public DealerReturnRecord confirmReturnFreight(String returnId, DealerReturnFreightConfirmRequest request) {
        return financial.confirmFreight(returnId,request);
    }

    // ── 退款 ──

    private String openRefundReview(DealerReturnRecord ret, long amountCent, String idemKey) {
        if (actorId() == null) {
            return null;
        }
        SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(idemKey + ":return-refund", "经销退货退款", List.of(), REFUND_ACTION,
                REFUND_OBJECT, ret.id(), amountCent, "经销退货单 " + ret.returnNo() + " 超过未收货款部分原路退款 " + amountCent + " 分", null, List.of(), null));
        return review.reviewId();
    }

    /** 既有退款审批结论回写。 */
    @EventListener
    @Transactional
    public void afterRefundDecision(SupervisorReviewDecisionRecord decision) {
        if (!REFUND_ACTION.equals(decision.actionType()) || !REFUND_OBJECT.equals(decision.objectType())) {
            return;
        }
        DealerReturnRecord ret = returns.findReturnByReview(decision.reviewId()).orElse(null);
        if (ret == null || !"approved".equals(decision.reviewStatus())) {
            return;
        }
        var order=orders.findOrder(ret.orderId()).orElseThrow();dealers.lockDealerCredit(order.dealerId()).orElseThrow();orders.lockOrder(order.id()).orElseThrow();
        ret=returns.findReturnByReview(decision.reviewId()).orElseThrow();
        if(!ret.id().equals(decision.objectId()) || !Set.of("pending_refund","refund_failed").contains(ret.status())) return;
        returns.saveReturn(withRefundApproved(ret));
        audit.recordSuccess("dealer.return.refund_approved", null, "dealer_return", ret.id(), null, null, meta("review_id", decision.reviewId()));
    }

    /** 退款执行结果：执行人≠验收人（ENUM:178）；本入口拒绝手填结果，实际退款由统一财务执行与独立结果确认回写。 */
    @Transactional
    public DealerReturnRecord recordRefundResult(String returnId, DealerReturnRefundResultRequest request) {
        authorization.requirePermission(REFUND_EXECUTE);
        var ret=requireReturn(returnId);
        String project=orders.findOrderProjectId(ret.orderId()).orElse(null);
        if(project==null || !authorization.canReachProject(authorization.currentActor(),project,REFUND_EXECUTE)) throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN","退款不在授权项目范围内");
        audit.recordFailure("dealer.return.refund_execute_failed",REFUND_EXECUTE,"dealer_return",returnId,null,
                request==null?null:request.idempotencyKey(),"DEALER_REFUND_REAL_RESULT_REQUIRED");
        throw ApiException.conflict("DEALER_REFUND_REAL_RESULT_REQUIRED","请从退货退款待办进入统一财务执行和独立结果确认，不能手填退款成功");
    }

    // ── 发票 ──

    /** BSM:1242：订单签收后才可按最终未退金额申请电子发票（不以已收款封顶）；同一订单同时只有一张有效发票。 */
    @Transactional
    public DealerInvoiceRecord requestInvoice(String orderId, DealerInvoiceRequest request) {
        return financial.requestInvoice(orderId,request);
    }

    /** 开具：第三方开票平台回填发票号（本系统不生成）。 */
    @Transactional
    public DealerInvoiceRecord issueInvoice(String invoiceId, DealerInvoiceIssueRequest request) {
        return financial.issueInvoice(invoiceId,request);
    }

    // ── helpers ──

    /** 交付数量全部处理完且实际合格接收商品全退 → 已退货；仍有在途退货则保持退货中。 */
    private void finalizeOrderStatus(String orderId) {
        if (returns.hasOpenReturn(orderId)) {
            orders.updateOrderStatus(orderId, "returning");
            return;
        }
        boolean allReturned = delivery.allAcceptedGoodsReturned(orderId, orders.listLines(orderId));
        orders.updateOrderStatus(orderId, allReturned ? "returned" : "partially_returned");
    }

    private static String finalStatus(int requested, int accepted) {
        return accepted >= requested ? "completed" : "partially_completed";
    }

    private DealerReturnRecord requireReturn(String id) {
        DealerReturnRecord ret = returns.findReturn(requireText(id, "return_id")).orElseThrow(() -> ApiException.notFound("DEALER_RETURN_NOT_FOUND", "退货单不存在"));
        requireOrder(ret.orderId());
        return ret;
    }

    private DealerOrderRecord requireOrder(String id) {
        DealerOrderRecord order = orders.findOrder(requireText(id, "order_id")).orElseThrow(() -> ApiException.notFound("DEALER_ORDER_NOT_FOUND", "经销订单不存在"));
        DealerRecord dealer = dealers.findDealer(order.dealerId()).orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
        String scope = DealerPortalAccountService.projectScopeOf(authorization);
        if (scope != null && dealer.projectId() != null && !scope.equals(dealer.projectId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权访问其他项目的经销商");
        }
        return order;
    }

    private static void requireStatus(DealerReturnRecord ret, String... allowed) {
        for (String s : allowed) {
            if (s.equals(ret.status())) {
                return;
            }
        }
        throw ApiException.conflict("DEALER_RETURN_STATUS_INVALID", "退货单当前状态不允许此操作：" + ret.status());
    }

    private static String requireDecision(String decision) {
        String value = requireText(decision, "decision");
        if (!List.of("approve", "reject").contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论须为通过（approve）或驳回（reject）");
        }
        return value;
    }

    /** 轻量状态拷贝（其余字段不变）。 */
    private static Mutable with(DealerReturnRecord r, String status) {
        return new Mutable(r, status);
    }

    private record Mutable(DealerReturnRecord r, String status) {
        DealerReturnRecord toBuilderApproved(String by, Instant at) {
            return new DealerReturnRecord(r.id(), r.returnNo(), r.orderId(), r.dealerId(), r.salesEntityId(), status, r.reasonType(), r.requestedQuantity(), r.acceptedQuantity(),
                    r.acceptedAmountCent(), r.offsetReceivableCent(), r.refundCent(), r.refundedCent(), r.refundReviewId(), r.refundApproved(), r.returnFreightBearer(),
                    r.firstLegFreightRefundable(), r.freightConfirmed(), by, at, r.receivedBy(), r.receivedAt(), r.inspectedBy(), r.inspectedAt(), r.refundExecutedBy(),
                    r.refundExecutedAt(), r.refundReferenceNo(), r.failureReason(), r.note(), r.createdBy(), r.createdAt(), at);
        }

        DealerReturnRecord toBuilderReceived(String by, Instant at) {
            return new DealerReturnRecord(r.id(), r.returnNo(), r.orderId(), r.dealerId(), r.salesEntityId(), status, r.reasonType(), r.requestedQuantity(), r.acceptedQuantity(),
                    r.acceptedAmountCent(), r.offsetReceivableCent(), r.refundCent(), r.refundedCent(), r.refundReviewId(), r.refundApproved(), r.returnFreightBearer(),
                    r.firstLegFreightRefundable(), r.freightConfirmed(), r.approvedBy(), r.approvedAt(), by, at, r.inspectedBy(), r.inspectedAt(), r.refundExecutedBy(),
                    r.refundExecutedAt(), r.refundReferenceNo(), r.failureReason(), r.note(), r.createdBy(), r.createdAt(), at);
        }
    }

    private static DealerReturnRecord withRefundApproved(DealerReturnRecord r) {
        return new DealerReturnRecord(r.id(), r.returnNo(), r.orderId(), r.dealerId(), r.salesEntityId(), r.status(), r.reasonType(), r.requestedQuantity(), r.acceptedQuantity(),
                r.acceptedAmountCent(), r.offsetReceivableCent(), r.refundCent(), r.refundedCent(), r.refundReviewId(), true, r.returnFreightBearer(),
                r.firstLegFreightRefundable(), r.freightConfirmed(), r.approvedBy(), r.approvedAt(), r.receivedBy(), r.receivedAt(), r.inspectedBy(), r.inspectedAt(),
                r.refundExecutedBy(), r.refundExecutedAt(), r.refundReferenceNo(), r.failureReason(), r.note(), r.createdBy(), r.createdAt(), Instant.now());
    }

    private static <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    private String actorId() {
        AccessActor actor = authorization.currentActor();
        return actor == null ? null : actor.id();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String codeOf(RuntimeException e) {
        return e instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }

    private static Map<String, Object> meta(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return map;
    }
}
