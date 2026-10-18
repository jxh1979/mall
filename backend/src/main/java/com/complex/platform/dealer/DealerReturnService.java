package com.complex.platform.dealer;

// 文件版本：V0.01

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
    private final JdbcDealerRepository dealers;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final SupervisorReviewService reviews;

    public DealerReturnService(JdbcDealerReturnRepository returns, JdbcDealerOrderRepository orders, JdbcDealerRepository dealers,
            AuthorizationService authorization, IdempotencyService idempotency, AuditEventService audit, SupervisorReviewService reviews) {
        this.returns = returns;
        this.orders = orders;
        this.dealers = dealers;
        this.authorization = authorization;
        this.idempotency = idempotency;
        this.audit = audit;
        this.reviews = reviews;
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
            DealerOrderRecord order = requireOrder(orderId);
            if (!RETURNABLE_ORDER_STATUSES.contains(order.status())) {
                throw ApiException.conflict("DEALER_ORDER_STATUS_INVALID", "订单签收后才可申请退货：" + order.status());
            }
            String reason = requireText(request == null ? null : request.reasonType(), "reason_type").trim().toLowerCase(Locale.ROOT);
            if (!REASONS.contains(reason)) {
                throw ApiException.badRequest("RETURN_REASON_NOT_ALLOWED", "合同期限内仅未拆封可复售商品或质量问题商品可以申请退货");
            }
            DealerContractRecord contract = dealers.findContract(order.dealerContractId()).orElseThrow();
            if (contract.validTo().isBefore(LocalDate.now())) {
                throw ApiException.conflict("DEALER_CONTRACT_EXPIRED", "合同期限已过，不得申请退货");
            }
            if (request.lines() == null || request.lines().isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "退货至少一条明细");
            }
            List<DealerOrderLineRecord> orderLines = orders.listLines(orderId);
            List<DealerReturnLineRecord> lines = new ArrayList<>();
            int requested = 0;
            String returnId = "dret_" + UUID.randomUUID();
            for (DealerReturnLineRequest line : request.lines()) {
                if (line == null || line.itemId() == null || line.quantity() == null || line.quantity() <= 0) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "每条退货明细须填写商品与大于 0 的数量");
                }
                String spec = line.spec() == null ? "" : line.spec().trim();
                DealerOrderLineRecord orderLine = orderLines.stream().filter(l -> l.itemId().equals(line.itemId().trim()) && l.spec().equals(spec)).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("RETURN_LINE_NOT_IN_ORDER", "退货商品不在原订单内：" + line.itemId()));
                int returnable = orderLine.quantity() - orderLine.returnedQuantity() - returns.pendingReturnQuantity(orderLine.id());
                if (line.quantity() > returnable) {
                    throw ApiException.conflict("RETURN_QUANTITY_EXCEEDS", "退货数量超过已出库未退数量：" + line.itemId());
                }
                requested += line.quantity();
                lines.add(new DealerReturnLineRecord("drl_" + UUID.randomUUID(), returnId, orderLine.id(), orderLine.itemId(), spec, line.quantity(), 0, 0, 0,
                        orderLine.unitPriceCent(), 0L, null));
            }
            int total = requested;
            return idempotency.execute("dealerReturnRequest:" + orderId, request.idempotencyKey(), request, () -> {
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
     * 验收：按实际验收数量 × 原成交价确认退货金额；可复售回可用库存、质量问题隔离禁售；先冲减未收货款、超出部分待退款；
     * 已开票 → 待红冲；未开票 → 退款 > 0 待退款（开既有退款审批），否则直接结清。
     */
    @Transactional
    public DealerReturnRecord inspectReturn(String returnId, DealerReturnInspectionRequest request) {
        authorization.requirePermission(INSPECT);
        try {
            DealerReturnRecord ret = requireReturn(returnId);
            requireStatus(ret, "inspecting");
            if (request == null || request.lines() == null || request.lines().isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "验收至少一条明细");
            }
            DealerOrderRecord order = orders.lockOrder(ret.orderId()).orElseThrow();
            List<DealerReturnLineRecord> lines = returns.listLines(returnId);
            List<DealerReturnLineRecord> inspected = new ArrayList<>();
            for (DealerReturnInspectionLineRequest in : request.lines()) {
                String spec = in.spec() == null ? "" : in.spec().trim();
                DealerReturnLineRecord line = lines.stream().filter(l -> l.itemId().equals(in.itemId()) && l.spec().equals(spec)).findFirst()
                        .orElseThrow(() -> ApiException.badRequest("RETURN_LINE_NOT_FOUND", "验收明细不在退货单内：" + in.itemId()));
                int accepted = in.acceptedQuantity() == null ? 0 : in.acceptedQuantity();
                int resalable = in.resalableQuantity() == null ? 0 : in.resalableQuantity();
                int quarantined = in.quarantinedQuantity() == null ? 0 : in.quarantinedQuantity();
                if (accepted < 0 || accepted > line.requestedQuantity() || resalable + quarantined != accepted) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "验收数量须不超过申请数量，且可复售 + 隔离 = 验收数量");
                }
                inspected.add(new DealerReturnLineRecord(line.id(), returnId, line.orderLineId(), line.itemId(), line.spec(), line.requestedQuantity(), accepted,
                        resalable, quarantined, line.unitPriceCent(), line.unitPriceCent() * accepted, trim(in.note())));
            }
            return idempotency.execute("dealerReturnInspect:" + returnId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                String actor = actorId();
                int acceptedQty = 0;
                long acceptedAmount = 0;
                for (DealerReturnLineRecord line : inspected) {
                    returns.saveLine(line);
                    acceptedQty += line.acceptedQuantity();
                    acceptedAmount += line.acceptedAmountCent();
                    if (line.resalableQuantity() > 0) {
                        returns.restock(line.itemId(), order.centralWarehouseId(), line.resalableQuantity(), returnId, actor);
                    }
                    if (line.quarantinedQuantity() > 0) {
                        returns.quarantine(line.itemId(), order.centralWarehouseId(), line.quarantinedQuantity(), returnId, actor);
                    }
                    if (line.acceptedQuantity() > 0) {
                        orders.addReturnedQuantity(line.orderLineId(), line.acceptedQuantity());
                    }
                }
                long unpaid = Math.max(0, order.totalAmountCent() - order.returnedAmountCent() - order.paidAmountCent());
                long offset = Math.min(acceptedAmount, unpaid);
                long refund = acceptedAmount - offset;
                if (offset > 0 && order.arApItemId() != null) {
                    returns.offsetReceivable(order.arApItemId(), offset);
                }
                orders.applyReturn(order.id(), acceptedAmount, "returning");
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
        authorization.requirePermission(INVOICE_MANAGE);
        try {
            DealerReturnRecord ret = requireReturn(returnId);
            requireStatus(ret, "pending_reversal");
            DealerOrderRecord order = requireOrder(ret.orderId());
            DealerInvoiceRecord original = returns.findActiveInvoice(order.id()).filter(v -> "issued".equals(v.status()))
                    .orElseThrow(() -> ApiException.conflict("DEALER_INVOICE_NOT_ISSUED", "订单没有已开具的发票可红冲"));
            return idempotency.execute("dealerReturnReverse:" + returnId, request == null ? null : request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                String actor = actorId();
                returns.saveInvoice(new DealerInvoiceRecord(original.id(), original.orderId(), original.dealerId(), original.salesEntityId(), original.amountCent(),
                        original.taxRateBps(), "reversed", original.invoiceNo(), returnId, original.reissuedFromInvoiceId(), original.requestedBy(), original.requestedAt(),
                        original.issuedBy(), original.issuedAt(), now, original.note()));
                long remaining = order.totalAmountCent() - order.returnedAmountCent();
                boolean allReturned = orders.listLines(order.id()).stream().allMatch(l -> l.returnedQuantity() >= l.quantity());
                String reissuedId = null;
                // 全部退货红冲后不重开（BSM:1242）；部分退货按最新未退金额（含已含在发票内的运费）重开
                if (!allReturned && remaining > 0) {
                    String invoiceNo = request == null ? null : trim(request.invoiceNo());
                    reissuedId = "dinv_" + UUID.randomUUID();
                    returns.saveInvoice(new DealerInvoiceRecord(reissuedId, order.id(), order.dealerId(), order.salesEntityId(), remaining, original.taxRateBps(),
                            invoiceNo == null ? "pending" : "issued", invoiceNo, null, original.id(), actor, now, invoiceNo == null ? null : actor,
                            invoiceNo == null ? null : now, null, "红冲后按最新未退金额重开"));
                }
                String reviewId = ret.refundCent() > 0 ? openRefundReview(ret, ret.refundCent(), request == null ? returnId : request.idempotencyKey()) : null;
                String next = ret.refundCent() > 0 ? "pending_refund" : finalStatus(ret.requestedQuantity(), ret.acceptedQuantity());
                DealerReturnRecord saved = new DealerReturnRecord(ret.id(), ret.returnNo(), ret.orderId(), ret.dealerId(), ret.salesEntityId(), next, ret.reasonType(),
                        ret.requestedQuantity(), ret.acceptedQuantity(), ret.acceptedAmountCent(), ret.offsetReceivableCent(), ret.refundCent(), ret.refundedCent(),
                        reviewId, false, ret.returnFreightBearer(), ret.firstLegFreightRefundable(), ret.freightConfirmed(), ret.approvedBy(), ret.approvedAt(),
                        ret.receivedBy(), ret.receivedAt(), ret.inspectedBy(), ret.inspectedAt(), null, null, null, null, ret.note(), ret.createdBy(), ret.createdAt(), now);
                returns.saveReturn(saved);
                if (ret.refundCent() == 0) {
                    finalizeOrderStatus(order.id());
                }
                audit.recordSuccess("dealer.invoice.reversed", INVOICE_MANAGE, "dealer_invoice", original.id(), null, request == null ? null : request.idempotencyKey(),
                        meta("return_id", returnId, "reissued_invoice_id", nz(reissuedId), "remaining_amount_cent", remaining));
                return saved;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.invoice.reverse_failed", INVOICE_MANAGE, "dealer_return", returnId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 运费确认 ──

    /** BSM:1240：合同未约定时由双方确认退回运费承担方与首程运费退不退（留痕）。 */
    @Transactional
    public DealerReturnRecord confirmReturnFreight(String returnId, DealerReturnFreightConfirmRequest request) {
        authorization.requirePermission(MANAGE);
        try {
            DealerReturnRecord ret = requireReturn(returnId);
            String bearer = requireText(request == null ? null : request.returnFreightBearer(), "return_freight_bearer").trim();
            if (!Set.of("dealer", "sales_entity").contains(bearer)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "退回运费承担方须为经销商（dealer）或销售主体（sales_entity）");
            }
            if ("quality_issue".equals(ret.reasonType()) && !"sales_entity".equals(bearer)) {
                throw ApiException.conflict("RETURN_FREIGHT_RULE_FIXED", "质量问题的退回运费由合同销售主体承担，不得改为经销商");
            }
            return idempotency.execute("dealerReturnFreight:" + returnId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerReturnRecord saved = new DealerReturnRecord(ret.id(), ret.returnNo(), ret.orderId(), ret.dealerId(), ret.salesEntityId(), ret.status(), ret.reasonType(),
                        ret.requestedQuantity(), ret.acceptedQuantity(), ret.acceptedAmountCent(), ret.offsetReceivableCent(), ret.refundCent(), ret.refundedCent(),
                        ret.refundReviewId(), ret.refundApproved(), bearer, request.firstLegFreightRefundable() == null ? ret.firstLegFreightRefundable() : request.firstLegFreightRefundable(),
                        true, ret.approvedBy(), ret.approvedAt(), ret.receivedBy(), ret.receivedAt(), ret.inspectedBy(), ret.inspectedAt(), ret.refundExecutedBy(),
                        ret.refundExecutedAt(), ret.refundReferenceNo(), ret.failureReason(), ret.note(), ret.createdBy(), ret.createdAt(), now);
                returns.saveReturn(saved);
                audit.recordSuccess("dealer.return.freight_confirmed", MANAGE, "dealer_return", returnId, null, request.idempotencyKey(),
                        meta("return_freight_bearer", bearer, "first_leg_freight_refundable", String.valueOf(saved.firstLegFreightRefundable()), "note", nz(request.note())));
                return saved;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.return.freight_confirm_failed", MANAGE, "dealer_return", returnId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
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
        returns.saveReturn(withRefundApproved(ret));
        audit.recordSuccess("dealer.return.refund_approved", null, "dealer_return", ret.id(), null, null, meta("review_id", decision.reviewId()));
    }

    /** 退款执行结果：执行人≠验收人（ENUM:178）；成功 → 已退金额计入并结清；失败 → 退款失败（可重试）。 */
    @Transactional
    public DealerReturnRecord recordRefundResult(String returnId, DealerReturnRefundResultRequest request) {
        authorization.requirePermission(REFUND_EXECUTE);
        try {
            DealerReturnRecord ret = requireReturn(returnId);
            requireStatus(ret, "pending_refund", "refund_failed");
            if (!ret.refundApproved()) {
                throw ApiException.conflict("DEALER_REFUND_NOT_APPROVED", "退款须经既有退款审批通过后才能执行");
            }
            String actor = actorId();
            if (Objects.equals(actor, ret.inspectedBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "退货验收人不得执行退款");
            }
            String result = requireText(request == null ? null : request.result(), "result").trim().toLowerCase(Locale.ROOT);
            if (!Set.of("success", "failed").contains(result)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "退款结果须为成功（success）或失败（failed）");
            }
            return idempotency.execute("dealerReturnRefund:" + returnId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                boolean ok = "success".equals(result);
                String next = ok ? finalStatus(ret.requestedQuantity(), ret.acceptedQuantity()) : "refund_failed";
                DealerReturnRecord saved = new DealerReturnRecord(ret.id(), ret.returnNo(), ret.orderId(), ret.dealerId(), ret.salesEntityId(), next, ret.reasonType(),
                        ret.requestedQuantity(), ret.acceptedQuantity(), ret.acceptedAmountCent(), ret.offsetReceivableCent(), ret.refundCent(), ok ? ret.refundCent() : 0L,
                        ret.refundReviewId(), ret.refundApproved(), ret.returnFreightBearer(), ret.firstLegFreightRefundable(), ret.freightConfirmed(), ret.approvedBy(),
                        ret.approvedAt(), ret.receivedBy(), ret.receivedAt(), ret.inspectedBy(), ret.inspectedAt(), actor, now, ok ? trim(request.referenceNo()) : null,
                        ok ? null : trim(request.failureReason()), ret.note(), ret.createdBy(), ret.createdAt(), now);
                returns.saveReturn(saved);
                if (ok) {
                    finalizeOrderStatus(ret.orderId());
                }
                audit.recordSuccess(ok ? "dealer.return.refund_executed" : "dealer.return.refund_failed", REFUND_EXECUTE, "dealer_return", returnId, null, request.idempotencyKey(),
                        meta("amount_cent", ret.refundCent(), "status", next, "reference_no", nz(request.referenceNo()), "failure_reason", nz(request.failureReason())));
                return saved;
            }, DealerReturnRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.return.refund_execute_failed", REFUND_EXECUTE, "dealer_return", returnId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 发票 ──

    /** BSM:1242：订单签收后才可按最终未退金额申请电子发票（不以已收款封顶）；同一订单同时只有一张有效发票。 */
    @Transactional
    public DealerInvoiceRecord requestInvoice(String orderId, DealerInvoiceRequest request) {
        authorization.requirePermission(INVOICE_MANAGE);
        try {
            DealerOrderRecord order = requireOrder(orderId);
            if (!RETURNABLE_ORDER_STATUSES.contains(order.status()) && !"returned".equals(order.status())) {
                throw ApiException.conflict("DEALER_ORDER_STATUS_INVALID", "订单签收后才可申请电子发票：" + order.status());
            }
            if (returns.findActiveInvoice(orderId).isPresent()) {
                throw ApiException.conflict("DEALER_INVOICE_EXISTS", "该订单已有有效发票，退货须先红冲");
            }
            long amount = order.totalAmountCent() - order.returnedAmountCent();
            if (amount <= 0) {
                throw ApiException.conflict("DEALER_INVOICE_AMOUNT_ZERO", "最终未退金额为零，无可开票金额");
            }
            return idempotency.execute("dealerInvoiceRequest:" + orderId, request == null ? null : request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                Integer taxRate = orders.listLines(orderId).stream().map(DealerOrderLineRecord::taxRateBps).filter(Objects::nonNull).findFirst().orElse(null);
                DealerInvoiceRecord invoice = new DealerInvoiceRecord("dinv_" + UUID.randomUUID(), orderId, order.dealerId(), order.salesEntityId(), amount, taxRate,
                        "pending", null, null, null, actorId(), now, null, null, null, request == null ? null : trim(request.note()));
                returns.saveInvoice(invoice);
                audit.recordSuccess("dealer.invoice.requested", INVOICE_MANAGE, "dealer_invoice", invoice.id(), null, request == null ? null : request.idempotencyKey(),
                        meta("order_id", orderId, "amount_cent", amount, "sales_entity_id", order.salesEntityId()));
                return invoice;
            }, DealerInvoiceRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.invoice.request_failed", INVOICE_MANAGE, "dealer_invoice", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 开具：第三方开票平台回填发票号（本系统不生成）。 */
    @Transactional
    public DealerInvoiceRecord issueInvoice(String invoiceId, DealerInvoiceIssueRequest request) {
        authorization.requirePermission(INVOICE_MANAGE);
        try {
            DealerInvoiceRecord invoice = returns.findInvoice(requireText(invoiceId, "invoice_id"))
                    .orElseThrow(() -> ApiException.notFound("DEALER_INVOICE_NOT_FOUND", "发票不存在"));
            requireOrder(invoice.orderId());
            if (!Set.of("pending", "issuing", "failed").contains(invoice.status())) {
                throw ApiException.conflict("DEALER_INVOICE_STATUS_INVALID", "只有待开票 / 开票中 / 开票失败的发票可以开具");
            }
            String no = requireText(request == null ? null : request.invoiceNo(), "invoice_no");
            return idempotency.execute("dealerInvoiceIssue:" + invoiceId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerInvoiceRecord saved = new DealerInvoiceRecord(invoice.id(), invoice.orderId(), invoice.dealerId(), invoice.salesEntityId(), invoice.amountCent(),
                        invoice.taxRateBps(), "issued", no.trim(), null, invoice.reissuedFromInvoiceId(), invoice.requestedBy(), invoice.requestedAt(), actorId(), now, null, invoice.note());
                returns.saveInvoice(saved);
                audit.recordSuccess("dealer.invoice.issued", INVOICE_MANAGE, "dealer_invoice", invoiceId, null, request.idempotencyKey(),
                        meta("invoice_no", no.trim(), "amount_cent", invoice.amountCent()));
                return saved;
            }, DealerInvoiceRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.invoice.issue_failed", INVOICE_MANAGE, "dealer_invoice", invoiceId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── helpers ──

    /** 退货单结清后订单状态：全部已出库数量退回 → 已退货；否则 → 部分退货（仍有在途退货则保持退货中）。 */
    private void finalizeOrderStatus(String orderId) {
        if (returns.hasOpenReturn(orderId)) {
            orders.updateOrderStatus(orderId, "returning");
            return;
        }
        boolean allReturned = orders.listLines(orderId).stream().allMatch(l -> l.returnedQuantity() >= l.quantity());
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
