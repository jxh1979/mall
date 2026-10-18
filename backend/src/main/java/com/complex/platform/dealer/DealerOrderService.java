package com.complex.platform.dealer;

// 文件版本：V0.04

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerCancelRequest;
import com.complex.platform.dealer.dto.DealerContractTermsRequest;
import com.complex.platform.dealer.dto.DealerOrderDecisionRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerOrderLineRequest;
import com.complex.platform.dealer.dto.DealerOverpaymentUseRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPaymentVerifyRequest;
import com.complex.platform.dealer.dto.DealerRefundApprovalRequest;
import com.complex.platform.dealer.dto.DealerRefundExecuteRequest;
import com.complex.platform.dealer.dto.DealerShipRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
 * CG-11b 经销订单、付款与履约（BSM:1226-1234）。
 * <ul>
 *   <li>ENUM:173 十五态；订单先审核合同、价格、库存和交期，通过后才通知付款（BSM:1227）；审核通过时按中心仓、商品和规格预留库存（ENUM:177）；
 *       通知付款时固化合同价格目录版本与询价单、税率只作审计快照（ENUM:180）；订单固化销售主体、中心仓与运费模板版本（BSM:1229、1248）。</li>
 *   <li>只支持对公转账和平台线上支付（ENUM:176）；财务核验首付款达到约定门槛后才能出库；未约定账期全款到账后出库；尾款列入应收，
 *       全部到账才结清；发货、签收不表示货款已结清（BSM:1227、1230）。</li>
 *   <li>E-13（BSM:1231）：首付款截止未达门槛 → 取消、释放未出库预留；已收部分按既有退款审批（supervisor_review 的 refund 策略）原路退回；
 *       取消后迟到的款项同样进入待退款；退款完成后订单回到已取消。已出库订单尾款逾期 → 应收转逾期、暂停该经销商新增账期订单（全款订单仍允许），
 *       结清后解除；已发货订单不自动取消、不释放库存。</li>
 *   <li>ENUM:178 / BSM:1232：订单审核、到账核验、仓库出库不得由同一人完成全链；退款执行人与到账核验人不得为同一人；取消审批人≠申请人。</li>
 *   <li>ENUM:179：取消审批与到账核验只能有一项先完成；取消审批生效后不得出库；已到账订单取消必须走退款审批。</li>
 *   <li>BSM:1228 超收：单独记录尚未确认用途的金额，不自动抵账、不增加发货数量；确认后转预付款或抵扣同一销售主体指定订单，同一金额不得重复抵用。</li>
 * </ul>
 */
@Service
public class DealerOrderService {

    static final String VIEW = "dealer.order.view";
    static final String MANAGE = "dealer.order.manage";
    static final String REVIEW = "dealer.order.review";
    static final String PAYMENT_VERIFY = "dealer.order.payment.verify";
    static final String SHIP = "dealer.order.ship";
    static final String REFUND_EXECUTE = "dealer.order.refund.execute";

    static final Set<String> PAYMENT_METHODS = Set.of("bank_transfer", "platform_online");
    static final String REFUND_ACTION = "refund";
    static final String REFUND_OBJECT = "dealer_order_refund";

    private final JdbcDealerOrderRepository orders;
    private final JdbcDealerDeliveryRepository delivery;
    private final JdbcDealerRepository dealers;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final FinanceRepository finance;
    private final SupervisorReviewService reviews;
    private final com.complex.platform.finance.FinanceService financeService;
    private final com.complex.platform.evidence.EvidenceEventRepository evidence;
    private final com.complex.platform.evidence.EvidenceAttachmentValidator attachmentValidator;

    public DealerOrderService(JdbcDealerOrderRepository orders, JdbcDealerRepository dealers, AuthorizationService authorization,
            IdempotencyService idempotency, AuditEventService audit, FinanceRepository finance, SupervisorReviewService reviews,
            com.complex.platform.finance.FinanceService financeService,
            com.complex.platform.evidence.EvidenceEventRepository evidence,
            com.complex.platform.evidence.EvidenceAttachmentValidator attachmentValidator, JdbcDealerDeliveryRepository delivery) {
        this.orders = orders;
        this.delivery = delivery;
        this.dealers = dealers;
        this.authorization = authorization;
        this.idempotency = idempotency;
        this.audit = audit;
        this.finance = finance;
        this.reviews = reviews;
        this.financeService = financeService;
        this.evidence = evidence;
        this.attachmentValidator = attachmentValidator;
    }

    // ── 合同付款条款与运费模板 ──

    /** BSM:1227 / 1229：条款落合同；运费模板每次变更版本 +1，只影响新订单。 */
    @Transactional
    public DealerContractRecord updateContractTerms(String contractId, DealerContractTermsRequest request) {
        authorization.requirePermission(DealerService.MANAGE);
        try {
            requireContract(contractId);
            DealerContractRecord c = dealers.lockContract(contractId).orElseThrow();
            if (request == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "条款必填");
            }
            boolean credit = Boolean.TRUE.equals(request.creditTerm());
            if (credit && request.firstPaymentRatioBps() == null && request.firstPaymentAmountCent() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "约定账期的合同须约定首付款比例或金额");
            }
            validateDueRule(request.firstPaymentAnchor(), request.firstPaymentDeadlineDays(), request.firstPaymentFixedAt(), request.contractSignedAt(), false);
            if (credit) {
                validateDueRule(request.balanceDueAnchor(), request.balanceDueDays(), request.balanceDueFixedAt(), request.contractSignedAt(), true);
            }
            if (request.firstPaymentRatioBps() != null && (request.firstPaymentRatioBps() <= 0 || request.firstPaymentRatioBps() > 10000)
                    || request.firstPaymentAmountCent() != null && request.firstPaymentAmountCent() <= 0
                    || request.firstPaymentRatioBps() != null && request.firstPaymentAmountCent() != null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "首付款须按有效比例或正金额择一约定");
            }
            String bearer = request.freightBearer() == null ? "dealer" : request.freightBearer().trim();
            if (!Set.of("dealer", "sales_entity").contains(bearer)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "运费承担方须为经销商（dealer）或销售主体（sales_entity）");
            }
            return idempotency.execute("dealerContractTerms:" + contractId, request.idempotencyKey(), request, () -> {
                boolean freightChanged = !Objects.equals(c.freightCent(), request.freightCent())
                        || !Objects.equals(c.freeShippingThresholdCent(), request.freeShippingThresholdCent()) || !bearer.equals(c.freightBearer());
                DealerContractRecord saved = new DealerContractRecord(c.id(), c.dealerId(), c.contractNo(), c.salesEntityId(), c.centralWarehouseId(),
                        c.validFrom(), c.validTo(), c.status(), c.createdBy(), c.createdAt(), credit, request.firstPaymentRatioBps(),
                        request.firstPaymentAmountCent(), request.firstPaymentDeadlineDays(), request.balanceDueDays(), request.freightCent(),
                        request.freeShippingThresholdCent(), bearer, c.freightTemplateVersion() + (freightChanged ? 1 : 0),
                        request.returnFreightBearer() == null ? c.returnFreightBearer() : trim(request.returnFreightBearer()),
                        request.firstLegFreightRefundable() == null ? c.firstLegFreightRefundable() : request.firstLegFreightRefundable(),
                        c.termsVersion() + 1, request.contractSignedAt(), request.firstPaymentAnchor(), request.firstPaymentFixedAt(),
                        credit ? request.balanceDueAnchor() : null, credit ? request.balanceDueFixedAt() : null);
                dealers.saveContract(saved);
                audit.recordSuccess("dealer.contract_terms_updated", DealerService.MANAGE, "dealer_contract", contractId, null, request.idempotencyKey(),
                        meta("credit_term", credit, "freight_template_version", saved.freightTemplateVersion(), "terms_version", saved.termsVersion()));
                return saved;
            }, DealerContractRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.contract_terms_update_failed", DealerService.MANAGE, "dealer_contract", contractId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 查询 ──

    public PageResponse<DealerOrderRecord> listOrders(String dealerId) {
        authorization.requirePermission(VIEW);
        return listOrders(dealerId, VIEW);
    }

    /** 门户已核验所属经销商；内部列表另按锁定订单项目过滤。 */
    PageResponse<DealerOrderRecord> listOrders(String dealerId, String permission) {
        requireDealer(dealerId);
        List<DealerOrderRecord> items = orders.listOrders(dealerId);
        if (VIEW.equals(permission)) {
            items = items.stream().filter(order -> canAccessOrderProject(order.id(), VIEW)).toList();
        }
        audit.recordSuccess("dealer.order.viewed", permission, "dealer", dealerId, null, null, meta("count", items.size()));
        return page(items);
    }

    public DealerOrderRecord getOrder(String orderId) {
        authorization.requirePermission(VIEW);
        DealerOrderRecord order = getOrder(orderId, VIEW);
        requireOrderProject(order.id(), VIEW);
        return order;
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    DealerOrderRecord getOrder(String orderId, String permission) {
        return requireOrder(orderId);
    }

    public PageResponse<DealerOrderLineRecord> listLines(String orderId) {
        authorization.requirePermission(VIEW);
        requireOrder(orderId);
        requireOrderProject(orderId, VIEW);
        return listLines(orderId, VIEW);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    PageResponse<DealerOrderLineRecord> listLines(String orderId, String permission) {
        requireOrder(orderId);
        return page(orders.listLines(orderId));
    }

    public PageResponse<DealerOrderPaymentRecord> listPayments(String orderId) {
        authorization.requirePermission(VIEW);
        requireOrder(orderId);
        requireOrderProject(orderId, VIEW);
        return listPayments(orderId, VIEW);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    PageResponse<DealerOrderPaymentRecord> listPayments(String orderId, String permission) {
        requireOrder(orderId);
        return page(orders.listPayments(orderId));
    }

    public PageResponse<DealerOrderRefundRecord> listRefunds(String orderId) {
        authorization.requirePermission(VIEW);
        requireOrder(orderId);
        requireOrderProject(orderId, VIEW);
        return page(orders.listRefunds(orderId));
    }

    public PageResponse<DealerOrderOverpaymentRecord> listOverpayments(String dealerId) {
        authorization.requirePermission(VIEW);
        requireDealer(dealerId);
        return page(orders.listOverpayments(dealerId).stream()
                .filter(overpayment -> canAccessOrderProject(overpayment.orderId(), VIEW)).toList());
    }

    // ── 建单 / 提交 / 审核 ──

    @Transactional
    public DealerOrderRecord createOrderDraft(String dealerId, DealerOrderDraftRequest request) {
        authorization.requirePermission(MANAGE);
        return createOrderDraft(dealerId, request, MANAGE);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    DealerOrderRecord createOrderDraft(String dealerId, DealerOrderDraftRequest request, String permission) {
        try {
            DealerRecord dealer = requireActiveDealer(dealerId);
            DealerContractRecord contract = requireContract(requireText(request == null ? null : request.dealerContractId(), "dealer_contract_id"));
            if (!contract.dealerId().equals(dealerId)) {
                throw ApiException.badRequest("CONTRACT_DEALER_MISMATCH", "合同不属于该经销商");
            }
            if (request.lines() == null || request.lines().isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "订单至少一条明细");
            }
            for (DealerOrderLineRequest line : request.lines()) {
                if (line == null || line.itemId() == null || line.itemId().isBlank() || line.quantity() == null || line.quantity() <= 0) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "每条明细须填写商品与大于 0 的数量");
                }
                if (dealers.findItem(line.itemId()).isEmpty()) {
                    throw ApiException.notFound("ITEM_NOT_FOUND", "库存商品不存在：" + line.itemId());
                }
            }
            return idempotency.execute("dealerOrderDraft:" + dealerId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                String id = "dord_" + UUID.randomUUID();
                List<DealerOrderLineRecord> lines = new ArrayList<>();
                for (DealerOrderLineRequest line : request.lines()) {
                    Priced priced = price(contract, dealer.id(), line);
                    lines.add(new DealerOrderLineRecord("dol_" + UUID.randomUUID(), id, line.itemId().trim(), line.spec() == null ? "" : line.spec().trim(),
                            line.quantity(), priced.unitPriceCent(), priced.taxRateBps(), priced.source(), priced.inquiryId(),
                            priced.unitPriceCent() * line.quantity()));
                }
                Amounts amounts = amounts(contract, lines);
                DealerOrderRecord order = new DealerOrderRecord(id, "DO-" + now.toEpochMilli() + "-" + id.substring(5, 9).toUpperCase(Locale.ROOT), dealerId,
                        contract.id(), contract.salesEntityId(), contract.centralWarehouseId(), "draft", null, null, contract.creditTerm(),
                        amounts.goods(), amounts.freight(), contract.freightBearer(), contract.freightTemplateVersion(), amounts.total(), 0L, null, null, 0L, false,
                        null, null, null, null, null, null, null, null, null, null, null, trim(request.note()), actorId(), now, now);
                orders.saveOrder(order);
                lines.forEach(orders::saveLine);
                audit.recordSuccess("dealer.order.drafted", permission, "dealer_order", id, null, request.idempotencyKey(),
                        meta("dealer_id", dealerId, "contract_id", contract.id(), "total_amount_cent", amounts.total(), "line_count", lines.size()));
                return order;
            }, DealerOrderRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.draft_failed", permission, "dealer_order", dealerId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 草稿 / 已驳回 → 待审核；BSM:1231 逾期暂停期间账期合同不得新增订单（全款合同仍允许）。 */
    @Transactional
    public DealerOrderRecord submitOrder(String orderId, String idempotencyKey) {
        authorization.requirePermission(MANAGE);
        return submitOrder(orderId, idempotencyKey, MANAGE);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    DealerOrderRecord submitOrder(String orderId, String idempotencyKey, String permission) {
        try {
            DealerOrderRecord order = requireOrder(orderId);
            requireStatus(order, "draft", "rejected");
            DealerRecord dealer = requireActiveDealer(order.dealerId());
            if (order.creditTerm() && dealer.creditSuspended()) {
                throw ApiException.conflict("DEALER_CREDIT_SUSPENDED", "该经销商有逾期货款未结清，暂停新增账期订单（全款到账后发货的订单仍可提交）");
            }
            return idempotency.execute("dealerOrderSubmit:" + orderId, idempotencyKey, Map.of("id", orderId), () -> {
                DealerOrderRecord saved = withStatus(order, "pending_review");
                orders.saveOrder(saved);
                audit.recordSuccess("dealer.order.submitted", permission, "dealer_order", orderId, null, idempotencyKey, meta("from", order.status()));
                return saved;
            }, DealerOrderRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.submit_failed", permission, "dealer_order", orderId, null, idempotencyKey, codeOf(e));
            throw e;
        }
    }

    /**
     * BSM:1227「订单先审核合同、价格、库存和交期，通过后才通知付款」：通过 → 预留中心仓库存（ENUM:177）、固化价格目录版本与询价单（ENUM:180）、
     * 建订单应收、算首付款门槛与截止 → 待付款；驳回 → 已驳回。
     */
    @Transactional
    public DealerOrderRecord reviewOrder(String orderId, DealerOrderDecisionRequest request) {
        authorization.requirePermission(REVIEW);
        try {
            requireOrder(orderId);
            DealerOrderRecord order = orders.lockOrder(orderId).orElseThrow();
            requireStatus(order, "pending_review");
            String decision = requireDecision(request == null ? null : request.decision());
            if (Objects.equals(actorId(), order.createdBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "订单建单人不得审核自己的订单");
            }
            return idempotency.execute("dealerOrderReview:" + orderId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                if ("reject".equals(decision)) {
                    DealerOrderRecord saved = withStatus(order, "rejected");
                    orders.saveOrder(saved);
                    audit.recordSuccess("dealer.order.reviewed", REVIEW, "dealer_order", orderId, null, request.idempotencyKey(),
                            meta("decision", "reject", "note", nz(request.note())));
                    return saved;
                }
                DealerContractRecord contract = dealers.lockContract(order.dealerContractId()).orElseThrow();
                requireActiveDealer(order.dealerId());
                if (contract.validTo().isBefore(LocalDate.now()) || !"active".equals(contract.status())) {
                    throw ApiException.conflict("DEALER_CONTRACT_INVALID", "经销合同已到期或不可用");
                }
                if (contract.termsVersion() <= 0 || contract.firstPaymentAnchor() == null) {
                    throw ApiException.conflict("DEALER_CONTRACT_TERMS_INCOMPLETE", "合同尚未登记完整付款起点与期限，无法通知付款");
                }
                DealerPriceCatalogRecord catalog = dealers.findEffectiveCatalog(contract.id())
                        .orElseThrow(() -> ApiException.conflict("DEALER_CATALOG_NOT_EFFECTIVE", "合同尚无生效的价格目录"));
                // 价格复核：按现行生效目录 / 已报价询价单重新定价并固化
                List<DealerOrderLineRecord> lines = new ArrayList<>();
                for (DealerOrderLineRecord line : orders.listLines(orderId)) {
                    Priced priced = price(contract, order.dealerId(), new DealerOrderLineRequest(line.itemId(), line.spec(), line.quantity(), line.inquiryId()));
                    int available = orders.availableInWarehouse(line.itemId(), contract.centralWarehouseId());
                    if (available < line.quantity()) {
                        throw ApiException.conflict("DEALER_STOCK_INSUFFICIENT", "中心仓可用库存不足，无法审核通过：" + line.itemId());
                    }
                    lines.add(new DealerOrderLineRecord(line.id(), orderId, line.itemId(), line.spec(), line.quantity(), priced.unitPriceCent(),
                            priced.taxRateBps(), priced.source(), priced.inquiryId(), priced.unitPriceCent() * line.quantity()));
                }
                for (DealerOrderLineRecord line : lines) {
                    if (!orders.reserveInWarehouse(line.itemId(), contract.centralWarehouseId(), line.quantity())) {
                        throw ApiException.conflict("DEALER_STOCK_INSUFFICIENT", "中心仓可用库存不足，预留失败：" + line.itemId());
                    }
                    orders.saveLine(line);
                }
                Amounts amounts = amounts(contract, lines);
                long firstRequired = firstPaymentRequired(contract, amounts.total());
                Instant deadline = resolveDue(contract.firstPaymentAnchor(), contract.firstPaymentFixedAt(), contract.firstPaymentDeadlineDays(),
                        contract.contractSignedAt(), order.createdAt(), now, null, null);
                if (deadline == null || !deadline.isAfter(now)) {
                    throw ApiException.conflict("DEALER_FIRST_PAYMENT_DEADLINE_PASSED", "按合同计算的首付款截止已过，须核实合同后再审核");
                }
                Instant balanceDue = contract.creditTerm() ? resolveDue(contract.balanceDueAnchor(), contract.balanceDueFixedAt(), contract.balanceDueDays(),
                        contract.contractSignedAt(), order.createdAt(), now, null, null) : null;
                String arApId = "arap_do_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
                DealerRecord dealer = dealers.findDealer(order.dealerId()).orElseThrow();
                finance.saveArApItem(new ArApItemRecord(arApId, "ARAP-DO-" + now.toEpochMilli(), "receivable", order.dealerId(), "dealer_order", orderId,
                        amounts.total(), 0L, amounts.total(), "CNY", LocalDate.ofInstant(deadline, ZoneOffset.UTC), null, "open", now, now, null,
                        dealer.projectId()));
                DealerOrderRecord saved = new DealerOrderRecord(order.id(), order.orderNo(), order.dealerId(), order.dealerContractId(), contract.salesEntityId(),
                        contract.centralWarehouseId(), "pending_payment", null, catalog.id(), contract.creditTerm(), amounts.goods(), amounts.freight(),
                        contract.freightBearer(), contract.freightTemplateVersion(), amounts.total(), firstRequired, deadline, balanceDue, 0L, false, arApId,
                        actorId(), now, now, null, null, null, null, null, null, null, order.note(), order.createdBy(), order.createdAt(), now);
                orders.saveOrder(saved);
                orders.lockContractSnapshot(orderId, contract);
                audit.recordSuccess("dealer.order.reviewed", REVIEW, "dealer_order", orderId, null, request.idempotencyKey(),
                        meta("decision", "approve", "price_catalog_id", catalog.id(), "total_amount_cent", amounts.total(),
                                "first_payment_required_cent", firstRequired, "first_payment_deadline_at", deadline.toString(), "ar_ap_item_id", arApId));
                audit.recordSuccess("dealer.order.payment_notified", REVIEW, "dealer_order", orderId, null, null,
                        meta("total_amount_cent", amounts.total(), "first_payment_required_cent", firstRequired));
                return orders.findOrder(orderId).orElseThrow();
            }, DealerOrderRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.review_failed", REVIEW, "dealer_order", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 付款 ──

    /** ENUM:176：只接受对公转账 / 平台线上支付；订单已取消 / 待退款时的到账为迟到款，直接进入待退款（E-13）。 */
    @Transactional
    public DealerOrderPaymentRecord registerPayment(String orderId, DealerPaymentRequest request) {
        authorization.requirePermission(MANAGE);
        requireOrder(orderId);
        requireOrderProject(orderId, MANAGE);
        return registerPayment(orderId, request, MANAGE);
    }

    /** 门户 / 内部共用实现：调用方已完成权限核验，permission 只用于审计留痕。 */
    DealerOrderPaymentRecord registerPayment(String orderId, DealerPaymentRequest request, String permission) {
        try {
            requireOrder(orderId);
            String method = requireText(request == null ? null : request.method(), "method").trim().toLowerCase(Locale.ROOT);
            if (!PAYMENT_METHODS.contains(method)) {
                throw ApiException.badRequest("PAYMENT_METHOD_FORBIDDEN", "经销货款只支持对公转账和平台线上支付，禁止现金和个人账户收款");
            }
            if (request.amountCent() == null || request.amountCent() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "到账金额须大于 0");
            }
            return idempotency.execute("dealerOrderPayment:" + orderId, request.idempotencyKey(), request, () -> {
                DealerOrderRecord order = orders.lockOrder(orderId).orElseThrow();
                boolean late = Set.of("cancelled", "pending_refund").contains(order.status());
                if (!late) {
                    requireStatus(order, "pending_payment", "payment_verifying", "pending_shipment", "shipping", "signed", "returning", "partially_returned", "returned", "cancelling");
                }
                Instant now = Instant.now();
                DealerOrderPaymentRecord payment = new DealerOrderPaymentRecord("dpay_" + UUID.randomUUID(), orderId, method, request.amountCent(),
                        request.receivedAt() == null ? now : request.receivedAt(), trim(request.referenceNo()), late ? "late" : "pending_verification", 0L,
                        actorId(), null, null, null, now, now);
                orders.savePayment(payment);
                if (late) {
                    createRefund(order, payment, payment.amountCent(), "取消后迟到的款项（E-13）", actorId(), request.idempotencyKey() + ":refund");
                    if (!"pending_refund".equals(order.status())) {
                        orders.saveOrder(withStatus(order, "pending_refund"));
                    }
                } else if ("pending_payment".equals(order.status())) {
                    orders.saveOrder(withStatus(order, "payment_verifying"));
                }
                audit.recordSuccess("dealer.order.payment_registered", permission, "dealer_order_payment", payment.id(), null, request.idempotencyKey(),
                        meta("order_id", orderId, "method", method, "amount_cent", request.amountCent(), "late", late));
                return payment;
            }, DealerOrderPaymentRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.payment_register_failed", permission, "dealer_order_payment", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /**
     * 到账核验（财务）：核验人≠订单审核人（ENUM:178）；取消中不得核验（ENUM:179）；通过后累计已收、回写应收；
     * 首付款达门槛 → 待出库；超过应收的到账单独记超收（BSM:1228）；已签收且全款到账 → 已完成。
     */
    @Transactional
    public DealerOrderPaymentRecord verifyPayment(String paymentId, DealerPaymentVerifyRequest request) {
        authorization.requirePermission(PAYMENT_VERIFY);
        try {
            DealerOrderPaymentRecord payment = requirePayment(paymentId);
            requireOrderProject(payment.orderId(), PAYMENT_VERIFY);
            lockDealerForOrder(payment.orderId());
            DealerOrderRecord order = orders.lockOrder(payment.orderId()).orElseThrow();
            orders.lockPayment(payment.id());
            payment = orders.findPayment(payment.id()).orElseThrow();
            if (!Set.of("pending_verification", "late").contains(payment.status())) {
                throw ApiException.conflict("DEALER_PAYMENT_STATUS_INVALID", "只有待核验的到账可以核验");
            }
            DealerOrderPaymentRecord originalPayment = payment;
            if ("cancelling".equals(order.status())) {
                throw ApiException.conflict("DEALER_CANCEL_IN_PROGRESS", "订单取消审批进行中，取消审批与到账核验只能有一项先完成");
            }
            String decision = requireDecision(request == null ? null : request.decision());
            String actor = actorId();
            if (Objects.equals(actor, order.reviewedBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "订单审核人不得核验该订单到账");
            }
            return idempotency.execute("dealerPaymentVerify:" + paymentId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                if ("reject".equals(decision)) {
                    orders.savePayment(withPaymentStatus(originalPayment, "rejected", 0L, actor, now, trim(request.note())));
                    if ("payment_verifying".equals(order.status()) && orders.listPayments(order.id()).stream()
                            .noneMatch(p -> "pending_verification".equals(p.status()) && !p.id().equals(paymentId))) {
                        orders.saveOrder(withStatus(order, "pending_payment"));
                    }
                    audit.recordSuccess("dealer.order.payment_verified", PAYMENT_VERIFY, "dealer_order_payment", paymentId, null, request.idempotencyKey(),
                            meta("decision", "reject", "note", nz(request.note())));
                    if (order.cancelledAt() != null) {
                        for (DealerOrderRefundRecord refund : orders.listRefundsForPayment(paymentId)) {
                            if (refund.financeRefundId() != null) throw ApiException.conflict("DEALER_REFUND_ALREADY_PREPARED", "原款已交财务退款，不能否认到账");
                            orders.saveRefund(refundState(refund, "voided", null, null, null));
                        }
                        finishCancelledOrder(order.id());
                    }
                    return orders.findPayment(paymentId).orElseThrow();
                }
                if (order.cancelledAt() != null) {
                    DealerOrderPaymentRecord verified = withPaymentStatus(originalPayment, "verified", 0L, actor, now, trim(request.note()));
                    orders.savePayment(verified);
                    audit.recordSuccess("dealer.order.payment_verified", PAYMENT_VERIFY, "dealer_order_payment", paymentId, null, request.idempotencyKey(),
                            meta("decision", "approve", "applied_cent", 0L, "cancelled_order", true));
                    return verified;
                }
                long receivable = Math.max(0, order.netReceivableCent() - order.paidAmountCent());
                long applied = Math.min(originalPayment.amountCent(), receivable);
                long excess = originalPayment.amountCent() - applied;
                DealerOrderPaymentRecord verified = withPaymentStatus(originalPayment, "verified", applied, actor, now, trim(request.note()));
                orders.savePayment(verified);
                if (excess > 0) {
                    orders.saveOverpayment(new DealerOrderOverpaymentRecord("dovp_" + UUID.randomUUID(), order.id(), order.dealerId(), order.salesEntityId(),
                            paymentId, excess, "unconfirmed", null, null, null, "超过订单应收的到账，用途待双方确认", now, excess, 0L, 0L, List.of()));
                    audit.recordSuccess("dealer.order.overpayment_recorded", PAYMENT_VERIFY, "dealer_order", order.id(), null, null,
                            meta("payment_id", paymentId, "amount_cent", excess));
                }
                DealerOrderRecord after = applyReceipt(order, applied, now);
                audit.recordSuccess("dealer.order.payment_verified", PAYMENT_VERIFY, "dealer_order_payment", paymentId, null, request.idempotencyKey(),
                        meta("decision", "approve", "applied_cent", applied, "order_status", after.status(), "paid_amount_cent", after.paidAmountCent()));
                return verified;
            }, DealerOrderPaymentRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.payment_verify_failed", PAYMENT_VERIFY, "dealer_order_payment", paymentId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 已收款入账后的状态推进（到账核验通过 / 超收抵扣指定订单共用）。 */
    private DealerOrderRecord applyReceipt(DealerOrderRecord order, long applied, Instant now) {
        long paid = order.paidAmountCent() + applied;
        boolean overdue = order.overdue();
        String status = order.status();
        Instant completedAt = order.completedAt();
        if (Set.of("pending_payment", "payment_verifying").contains(status)) {
            status = paid >= order.firstPaymentRequiredCent() ? "pending_shipment" : "pending_payment";
        }
        if ("signed".equals(status) && paid >= order.netReceivableCent() && delivery.pendingRefundCent(order.id()) == 0) {
            status = "completed";
            completedAt = now;
        }
        if (overdue && paid >= order.netReceivableCent()) {
            overdue = false;
        }
        DealerOrderRecord saved = new DealerOrderRecord(order.id(), order.orderNo(), order.dealerId(), order.dealerContractId(), order.salesEntityId(),
                order.centralWarehouseId(), status, order.statusBeforeCancel(), order.priceCatalogId(), order.creditTerm(), order.goodsAmountCent(), order.freightCent(),
                order.freightBearer(), order.freightTemplateVersion(), order.totalAmountCent(), order.firstPaymentRequiredCent(), order.firstPaymentDeadlineAt(),
                order.balanceDueAt(), paid, overdue, order.arApItemId(), order.reviewedBy(), order.reviewedAt(), order.paymentNotifiedAt(), order.shippedBy(),
                order.shippedAt(), order.signedAt(), completedAt, order.cancelRequestedBy(), order.cancelReason(), order.cancelledAt(), order.note(),
                order.createdBy(), order.createdAt(), now);
        orders.saveOrder(saved);
        if (order.arApItemId() != null && applied > 0) {
            long unpaid = Math.max(0, order.netReceivableCent() - paid);
            orders.updateArApItem(order.arApItemId(), Math.min(paid, order.netReceivableCent()), unpaid, unpaid == 0 ? "settled" : (overdue ? "overdue" : "partial_paid"));
            if (order.creditTerm() && paid >= order.firstPaymentRequiredCent()) {
                orders.updateArApDueDate(order.arApItemId(), order.balanceDueAt());
            }
        }
        if (order.overdue() && !overdue && !orders.dealerHasOverdueOrders(order.dealerId())) {
            dealers.setCreditSuspended(order.dealerId(), false);
            audit.recordSuccess("dealer.credit_resumed", PAYMENT_VERIFY, "dealer", order.dealerId(), null, null, meta("order_id", order.id()));
        }
        return orders.findOrder(saved.id()).orElseThrow();
    }

    // ── 出库 / 签收 ──

    /** 仓库出库：首付款达门槛（BSM:1227）；出库人≠审核人且≠到账核验人（ENUM:178）；取消中 / 取消后不得出库（ENUM:179）；预留转实扣并落流水；账期单起算尾款到期。 */
    @Transactional
    public DealerOrderRecord shipOrder(String orderId, DealerShipRequest request) {
        authorization.requirePermission(SHIP);
        try {
            DealerOrderRecord order = orders.lockOrder(requireText(orderId, "order_id")).orElseThrow(() -> ApiException.notFound("DEALER_ORDER_NOT_FOUND", "经销订单不存在"));
            if ("cancelling".equals(order.status())) {
                throw ApiException.conflict("DEALER_CANCEL_IN_PROGRESS", "订单取消审批进行中，不得出库");
            }
            if (Set.of("pending_payment", "payment_verifying").contains(order.status())) {
                throw ApiException.conflict("DEALER_FIRST_PAYMENT_NOT_MET", "首付款尚未经财务核验达到合同门槛，不得出库");
            }
            requireStatus(order, "pending_shipment");
            if (order.paidAmountCent() < order.firstPaymentRequiredCent()) {
                throw ApiException.conflict("DEALER_FIRST_PAYMENT_NOT_MET", "首付款尚未达到合同门槛，不得出库");
            }
            String actor = actorId();
            boolean verifier = orders.listPayments(orderId).stream().anyMatch(p -> "verified".equals(p.status()) && Objects.equals(p.verifiedBy(), actor));
            if (Objects.equals(actor, order.reviewedBy()) || verifier) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "订单审核、到账核验和仓库出库不得由同一人完成全链");
            }
            return idempotency.execute("dealerOrderShip:" + orderId, request == null ? null : request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                for (DealerOrderLineRecord line : orders.listLines(orderId)) {
                    if (!orders.shipFromWarehouse(line.itemId(), order.centralWarehouseId(), line.quantity(), orderId, actor)) {
                        throw ApiException.conflict("DEALER_STOCK_INSUFFICIENT", "中心仓预留不足，无法出库：" + line.itemId());
                    }
                }
                delivery.createBatch(orderId, orderId, null, actor, now, orders.listLines(orderId));
                DealerContractRecord contract = requireSnapshot(order);
                Instant balanceDue = order.creditTerm() ? resolveDue(contract.balanceDueAnchor(), contract.balanceDueFixedAt(), contract.balanceDueDays(),
                        contract.contractSignedAt(), order.createdAt(), order.paymentNotifiedAt(), now, null) : null;
                DealerOrderRecord saved = new DealerOrderRecord(order.id(), order.orderNo(), order.dealerId(), order.dealerContractId(), order.salesEntityId(),
                        order.centralWarehouseId(), "shipping", null, order.priceCatalogId(), order.creditTerm(), order.goodsAmountCent(), order.freightCent(),
                        order.freightBearer(), order.freightTemplateVersion(), order.totalAmountCent(), order.firstPaymentRequiredCent(), order.firstPaymentDeadlineAt(),
                        balanceDue, order.paidAmountCent(), false, order.arApItemId(), order.reviewedBy(), order.reviewedAt(), order.paymentNotifiedAt(), actor, now,
                        null, null, order.cancelRequestedBy(), order.cancelReason(), order.cancelledAt(), order.note(), order.createdBy(), order.createdAt(), now);
                orders.saveOrder(saved);
                if (order.creditTerm()) orders.updateArApDueDate(order.arApItemId(), balanceDue);
                audit.recordSuccess("dealer.order.shipped", SHIP, "dealer_order", orderId, null, request == null ? null : request.idempotencyKey(),
                        meta("balance_due_at", balanceDue == null ? "" : balanceDue.toString(), "note", request == null ? "" : nz(request.note())));
                return orders.findOrder(orderId).orElseThrow();
            }, DealerOrderRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.ship_failed", SHIP, "dealer_order", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 取消 ──

    /** 取消申请（出库前）：有待核验到账时不得发起（ENUM:179 两者只能一项先完成）。 */
    @Transactional
    public DealerOrderRecord requestCancel(String orderId, DealerCancelRequest request) {
        authorization.requirePermission(MANAGE);
        try {
            requireOrder(orderId);
            requireOrderProject(orderId, MANAGE);
            String reason = requireText(request == null ? null : request.reason(), "reason");
            return idempotency.execute("dealerOrderCancelRequest:" + orderId, request.idempotencyKey(), request, () -> {
                DealerOrderRecord order = orders.lockOrder(orderId).orElseThrow();
                requireStatus(order, "pending_payment", "payment_verifying", "pending_shipment");
                if (orders.listPayments(orderId).stream().anyMatch(p -> "pending_verification".equals(p.status()))) {
                    throw ApiException.conflict("DEALER_PAYMENT_VERIFYING", "有到账正在核验，取消审批与到账核验只能有一项先完成");
                }
                Instant now = Instant.now();
                DealerOrderRecord saved = new DealerOrderRecord(order.id(), order.orderNo(), order.dealerId(), order.dealerContractId(), order.salesEntityId(),
                        order.centralWarehouseId(), "cancelling", order.status(), order.priceCatalogId(), order.creditTerm(), order.goodsAmountCent(), order.freightCent(),
                        order.freightBearer(), order.freightTemplateVersion(), order.totalAmountCent(), order.firstPaymentRequiredCent(), order.firstPaymentDeadlineAt(),
                        order.balanceDueAt(), order.paidAmountCent(), order.overdue(), order.arApItemId(), order.reviewedBy(), order.reviewedAt(), order.paymentNotifiedAt(),
                        order.shippedBy(), order.shippedAt(), order.signedAt(), order.completedAt(), actorId(), reason.trim(), null, order.note(), order.createdBy(),
                        order.createdAt(), now);
                orders.saveOrder(saved);
                audit.recordSuccess("dealer.order.cancel_requested", MANAGE, "dealer_order", orderId, null, request.idempotencyKey(), meta("reason", reason.trim()));
                return saved;
            }, DealerOrderRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.cancel_request_failed", MANAGE, "dealer_order", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 取消审批：审批人≠申请人；通过 → 释放预留，已到账走退款审批（待退款），无到账直接已取消；驳回 → 回到取消前状态。 */
    @Transactional
    public DealerOrderRecord decideCancel(String orderId, DealerOrderDecisionRequest request) {
        authorization.requirePermission(REVIEW);
        try {
            requireOrderProject(requireText(orderId, "order_id"), REVIEW);
            DealerOrderRecord order = orders.lockOrder(orderId).orElseThrow(() -> ApiException.notFound("DEALER_ORDER_NOT_FOUND", "经销订单不存在"));
            requireStatus(order, "cancelling");
            String decision = requireDecision(request == null ? null : request.decision());
            if (Objects.equals(actorId(), order.cancelRequestedBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "取消申请人不得审批自己的取消申请");
            }
            return idempotency.execute("dealerOrderCancelDecide:" + orderId, request.idempotencyKey(), request, () -> {
                if ("reject".equals(decision)) {
                    DealerOrderRecord saved = withStatus(order, order.statusBeforeCancel() == null ? "pending_payment" : order.statusBeforeCancel());
                    orders.saveOrder(saved);
                    audit.recordSuccess("dealer.order.cancel_decided", REVIEW, "dealer_order", orderId, null, request.idempotencyKey(),
                            meta("decision", "reject", "status", saved.status(), "note", nz(request.note())));
                    return saved;
                }
                DealerOrderRecord saved = cancelOrder(order, order.cancelReason(), actorId(), request.idempotencyKey());
                audit.recordSuccess("dealer.order.cancel_decided", REVIEW, "dealer_order", orderId, null, request.idempotencyKey(),
                        meta("decision", "approve", "status", saved.status(), "note", nz(request.note())));
                return saved;
            }, DealerOrderRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.cancel_decide_failed", REVIEW, "dealer_order", orderId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** E-13 作业：首付款截止未达门槛的订单取消（系统主体）；返回取消数。 */
    @Transactional
    public int cancelUnpaidAtDeadline(Instant now) {
        int count = 0;
        for (DealerOrderRecord order : orders.listPastFirstPaymentDeadline(now)) {
            DealerOrderRecord locked = orders.lockOrder(order.id()).orElse(null);
            if (locked == null || !Set.of("pending_payment", "payment_verifying").contains(locked.status())) {
                continue;
            }
            cancelOrder(locked, "首付款截止时仍未达到合同门槛（E-13）", actorId(), "deadline:" + order.id());
            audit.recordSuccess("dealer.order.deadline_cancelled", null, "dealer_order", order.id(), null, null,
                    meta("first_payment_deadline_at", String.valueOf(order.firstPaymentDeadlineAt()), "paid_amount_cent", order.paidAmountCent(),
                            "first_payment_required_cent", order.firstPaymentRequiredCent()));
            count++;
        }
        return count;
    }

    /** 取消落地：释放未出库预留；未核验到账按迟到款、已核验到账按已收款分别进待退款；无款项直接已取消；订单应收作废。 */
    private DealerOrderRecord cancelOrder(DealerOrderRecord order, String reason, String actor, String idemPrefix) {
        List<DealerOrderPaymentRecord> payments = orders.listPayments(order.id());
        List<DealerOverpaymentAllocationRecord> allocations = orders.listOrderAllocations(order.id());
        // 多个目标可能以相反顺序抵用同几笔原款；统一预锁顺序，不能边建退款边反序取锁。
        java.util.Set<String> paymentIds = new java.util.TreeSet<>();
        payments.forEach(payment -> paymentIds.add(payment.id()));
        allocations.stream().filter(allocation -> allocation.refundId() == null).forEach(allocation ->
                paymentIds.add(orders.findOverpayment(allocation.overpaymentId()).orElseThrow().paymentId()));
        paymentIds.forEach(orders::lockPayment);
        Instant now = Instant.now();
        for (DealerOrderLineRecord line : orders.listLines(order.id())) {
            orders.releaseInWarehouse(line.itemId(), order.centralWarehouseId(), line.quantity());
        }
        boolean refunds = false;
        for (DealerOrderPaymentRecord payment : payments) {
            if ("verified".equals(payment.status()) && payment.appliedCent() > 0) {
                createRefund(order, payment, payment.appliedCent(), "订单取消，已收部分原路退回", actor, idemPrefix + ":" + payment.id());
                refunds = true;
            } else if ("pending_verification".equals(payment.status())) {
                orders.savePayment(withPaymentStatus(payment, "late", 0L, null, null, "取消时尚未核验，按迟到款进入待退款"));
                createRefund(order, payment, payment.amountCent(), "取消后迟到的款项（E-13）", actor, idemPrefix + ":" + payment.id());
                refunds = true;
            }
        }
        for (DealerOverpaymentAllocationRecord allocation : allocations) {
            if (allocation.refundId() != null) continue;
            DealerOrderOverpaymentRecord source = orders.findOverpayment(allocation.overpaymentId()).orElseThrow();
            DealerOrderPaymentRecord payment = orders.findPayment(source.paymentId()).orElseThrow();
            DealerOrderRefundRecord refund = createRefund(order, payment, allocation.amountCent(),
                    "抵用预付款的订单取消，沿原始超收款收款路径退现金", actor, idemPrefix + ":" + allocation.id(), allocation.id());
            orders.linkAllocationRefund(allocation.id(), refund.id());
            refunds = true;
        }
        if (order.arApItemId() != null) {
            // 应收作废：保持 paid + unpaid = amount（chk_ar_ap_items_amount_balance），状态记作 cancelled（技术决定）
            long paid = Math.min(order.paidAmountCent(), order.netReceivableCent());
            orders.updateArApItem(order.arApItemId(), paid, order.netReceivableCent() - paid, "cancelled");
        }
        DealerOrderRecord saved = new DealerOrderRecord(order.id(), order.orderNo(), order.dealerId(), order.dealerContractId(), order.salesEntityId(),
                order.centralWarehouseId(), refunds ? "pending_refund" : "cancelled", null, order.priceCatalogId(), order.creditTerm(), order.goodsAmountCent(),
                order.freightCent(), order.freightBearer(), order.freightTemplateVersion(), order.totalAmountCent(), order.firstPaymentRequiredCent(),
                order.firstPaymentDeadlineAt(), order.balanceDueAt(), order.paidAmountCent(), false, order.arApItemId(), order.reviewedBy(), order.reviewedAt(),
                order.paymentNotifiedAt(), order.shippedBy(), order.shippedAt(), order.signedAt(), order.completedAt(), order.cancelRequestedBy(), reason, now,
                order.note(), order.createdBy(), order.createdAt(), now);
        orders.saveOrder(saved);
        audit.recordSuccess("dealer.order.cancelled", null, "dealer_order", order.id(), null, null, meta("status", saved.status(), "reason", nz(reason)));
        return saved;
    }

    // ── 退款（既有退款审批原路退回） ──

    /** 差异已完成主管与财务审批、实物已处理后，先减原单未收款，超出部分沿真实原收款退现金。 */
    void applyDeliveryAdjustment(DealerOrderRecord order, String resolutionId, long amountCent, String reviewId) {
        var resolution = delivery.resolution(resolutionId).orElseThrow();
        if (resolution.adjustmentAppliedAt() != null || resolution.adjustmentCent() != amountCent || !reviewId.equals(resolution.reviewId())) {
            throw ApiException.conflict("DEALER_DELIVERY_ADJUSTMENT_INVALID", "差异调款必须匹配已批准金额且只能执行一次");
        }
        var review = reviews.findReview(reviewId).orElseThrow();
        if (!"approved".equals(review.reviewStatus()) || !"dealer.delivery_resolution".equals(review.actionType())
                || !resolutionId.equals(review.objectId()) || amountCent <= 0 || amountCent > order.netReceivableCent()) {
            throw ApiException.conflict("DEALER_DELIVERY_ADJUSTMENT_INVALID", "须按本次已批准差异及原单应收办理调款");
        }
        long refundAmount = amountCent - Math.min(amountCent, Math.max(0L, order.netReceivableCent() - order.paidAmountCent()));
        Map<String, Long> sources = new java.util.TreeMap<>();
        orders.listPayments(order.id()).stream().filter(p -> "verified".equals(p.status()) && p.appliedCent() > 0)
                .forEach(p -> sources.merge(p.id(), p.appliedCent(), Math::addExact));
        for (var allocation : orders.listOrderAllocations(order.id())) {
            var source = orders.findOverpayment(allocation.overpaymentId()).orElseThrow();
            sources.merge(source.paymentId(), allocation.amountCent(), Math::addExact);
        }
        sources.keySet().forEach(orders::lockPayment);
        long remaining = refundAmount;
        for (var entry : sources.entrySet()) {
            if (remaining == 0) break;
            var payment = orders.findPayment(entry.getKey()).orElseThrow();
            requireOrderProject(payment.orderId(), SHIP);
            long usedForThisOrder = orders.listRefunds(order.id()).stream()
                    .filter(r -> entry.getKey().equals(r.paymentId()) && !"voided".equals(r.status())).mapToLong(DealerOrderRefundRecord::amountCent).sum();
            long capacity = Math.min(entry.getValue() - usedForThisOrder, payment.amountCent() - orders.reservedRefundAmount(payment.id()));
            long amount = Math.min(remaining, Math.max(0L, capacity));
            if (amount == 0) continue;
            if (!"verified".equals(payment.status())) throw ApiException.conflict("DEALER_RECEIPT_UNVERIFIED", "原收款必须经核实到账");
            Instant now = Instant.now();
            String id = "drf_" + UUID.randomUUID();
            orders.saveRefund(new DealerOrderRefundRecord(id, order.id(), payment.id(), amount, "approved", reviewId, actorId(),
                    null, null, null, "交付差异 " + resolutionId + "：按已批准方案原路退回", now, now, null, null));
            delivery.linkRefund(resolutionId, id);
            remaining -= amount;
        }
        if (remaining != 0) throw ApiException.conflict("DEALER_REFUND_EXCEEDS_RECEIPT", "差异退款缺少可核实且尚未占用的原收款，不能虚构退款或重复使用原款");
        delivery.applyAdjustment(order.id(), amountCent);
        if (order.overdue() && !orders.dealerHasOverdueOrders(order.dealerId())) {
            dealers.setCreditSuspended(order.dealerId(), false);
            audit.recordSuccess("dealer.credit_resumed", SHIP, "dealer", order.dealerId(), null, null,
                    meta("order_id", order.id(), "reason", "交付差异调款冲清全部逾期应收"));
        }
    }

    private DealerOrderRefundRecord createRefund(DealerOrderRecord order, DealerOrderPaymentRecord payment, long amountCent, String reason, String actor, String idemKey) {
        return createRefund(order, payment, amountCent, reason, actor, idemKey, null);
    }

    private DealerOrderRefundRecord createRefund(DealerOrderRecord order, DealerOrderPaymentRecord payment, long amountCent, String reason, String actor, String idemKey, String allocationId) {
        orders.lockPayment(payment.id());
        if (amountCent <= 0 || Math.addExact(orders.reservedRefundAmount(payment.id()), amountCent) > payment.amountCent()) {
            throw ApiException.conflict("DEALER_REFUND_EXCEEDS_RECEIPT", "待退、处理中及已退金额合计不得超过原收款");
        }
        Instant now = Instant.now();
        String id = "drf_" + UUID.randomUUID();
        String reviewId = null;
        String status = "pending_request";
        if (actor != null) {
            SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(idemKey + ":review", reason, List.of(), REFUND_ACTION,
                    REFUND_OBJECT, id, amountCent, "经销订单 " + order.orderNo() + " 退款 " + amountCent + " 分：" + reason, null, List.of(), null));
            reviewId = review.reviewId();
            status = "pending_approval";
        }
        DealerOrderRefundRecord refund = new DealerOrderRefundRecord(id, order.id(), payment.id(), amountCent, status, reviewId, actor, null, null, null, reason, now, now, null, allocationId);
        orders.saveRefund(refund);
        audit.recordSuccess("dealer.order.refund_requested", null, "dealer_order_refund", id, null, null,
                meta("order_id", order.id(), "payment_id", payment.id(), "amount_cent", amountCent, "review_id", nz(reviewId)));
        return refund;
    }

    /** 系统取消产生的待发起退款：由财务开启既有退款审批。 */
    @Transactional
    public DealerOrderRefundRecord openRefundApproval(String refundId, DealerRefundApprovalRequest request) {
        authorization.requirePermission(PAYMENT_VERIFY);
        try {
            DealerOrderRefundRecord found = requireRefund(refundId);
            requireOrderProject(found.orderId(), PAYMENT_VERIFY);
            return idempotency.execute("dealerRefundApproval:" + refundId, request == null ? null : request.idempotencyKey(), request, () -> {
                DealerOrderRefundRecord refund = orders.lockRefund(refundId).orElseThrow();
                if (!"pending_request".equals(refund.status()) && !"rejected".equals(refund.status())) {
                    throw ApiException.conflict("DEALER_REFUND_STATUS_INVALID", "只有待发起或已驳回的退款可以开启审批");
                }
                DealerOrderRecord order = requireOrder(refund.orderId());
                SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(request.idempotencyKey() + ":review", refund.reason(),
                        List.of(), REFUND_ACTION, REFUND_OBJECT, refundId, refund.amountCent(), "经销订单 " + order.orderNo() + " 退款 " + refund.amountCent() + " 分：" + refund.reason(),
                        trim(request.finalApproverId()), List.of(), null));
                DealerOrderRefundRecord saved = new DealerOrderRefundRecord(refund.id(), refund.orderId(), refund.paymentId(), refund.amountCent(), "pending_approval",
                        review.reviewId(), actorId(), null, null, null, refund.reason(), refund.createdAt(), Instant.now(), refund.financeRefundId(), refund.allocationId());
                orders.saveRefund(saved);
                audit.recordSuccess("dealer.order.refund_requested", PAYMENT_VERIFY, "dealer_order_refund", refundId, null, request.idempotencyKey(),
                        meta("review_id", review.reviewId()));
                return saved;
            }, DealerOrderRefundRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.refund_request_failed", PAYMENT_VERIFY, "dealer_order_refund", refundId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 既有退款审批的结论回写。 */
    @EventListener
    @Transactional
    public void afterRefundDecision(SupervisorReviewDecisionRecord decision) {
        if (!REFUND_ACTION.equals(decision.actionType()) || !REFUND_OBJECT.equals(decision.objectType())) {
            return;
        }
        DealerOrderRefundRecord found = orders.findRefundByReview(decision.reviewId()).orElse(null);
        if (found == null) return;
        DealerOrderRefundRecord refund = orders.lockRefund(found.id()).orElse(null);
        if (refund == null || !"pending_approval".equals(refund.status()) || !decision.reviewId().equals(refund.reviewId())) {
            return;
        }
        String status = "approved".equals(decision.reviewStatus()) ? "approved" : "rejected".equals(decision.reviewStatus()) ? "rejected" : null;
        if (status == null) {
            return;
        }
        orders.saveRefund(new DealerOrderRefundRecord(refund.id(), refund.orderId(), refund.paymentId(), refund.amountCent(), status, refund.reviewId(),
                refund.requestedBy(), null, null, null, refund.reason(), refund.createdAt(), Instant.now(), refund.financeRefundId(), refund.allocationId()));
        audit.recordSuccess("dealer.order.refund_decided", null, "dealer_order_refund", refund.id(), null, null, meta("status", status, "review_id", decision.reviewId()));
    }

    /** 交统一财务办理；登记待办不表示已出款，实际执行和独立结果复核沿用财务入口。 */
    @Transactional
    public DealerOrderRefundRecord executeRefund(String refundId, DealerRefundExecuteRequest request) {
        authorization.requirePermission(REFUND_EXECUTE);
        try {
            DealerOrderRefundRecord found = requireRefund(refundId);
            requireOrderProject(found.orderId(), REFUND_EXECUTE);
            return idempotency.execute("dealerRefundExecute:" + refundId, request == null ? null : request.idempotencyKey(), request, () -> {
                DealerOrderRefundRecord refund = orders.lockRefund(found.id()).orElseThrow();
                if (refund.financeRefundId() != null) return refund;
                if (!"approved".equals(refund.status())) {
                    throw ApiException.conflict("DEALER_REFUND_NOT_APPROVED", "退款须经既有退款审批通过后才能办理");
                }
                DealerOrderPaymentRecord payment = requirePayment(refund.paymentId());
                if (!"verified".equals(payment.status())) {
                    throw ApiException.conflict("DEALER_RECEIPT_UNVERIFIED", "须先核实原收款实际到账，待退款记录不代表已收款");
                }
                requireText(payment.referenceNo(), "原收款流水号");
                if (Objects.equals(payment.verifiedBy(), actorId())) {
                    throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "退款办理人与到账核验人不得为同一人");
                }
                DealerOrderRecord order = requireOrder(refund.orderId());
                String projectId = requireOrderProject(payment.orderId(), REFUND_EXECUTE);
                String financeId = financeService.recordDealerOrderRefund(refund.id(), refund.amountCent(), payment.method(),
                        payment.referenceNo(), projectId, refund.reason());
                DealerOrderRefundRecord saved = new DealerOrderRefundRecord(refund.id(), refund.orderId(), refund.paymentId(),
                        refund.amountCent(), refund.status(), refund.reviewId(), refund.requestedBy(), null, null, null,
                        refund.reason(), refund.createdAt(), Instant.now(), financeId, refund.allocationId());
                orders.saveRefund(saved);
                audit.recordSuccess("dealer.order.refund_executed", REFUND_EXECUTE, "dealer_order_refund", refundId, null, request.idempotencyKey(),
                        meta("finance_refund_id", financeId, "status", "requested", "amount_cent", refund.amountCent()));
                return saved;
            }, DealerOrderRefundRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.refund_execute_failed", REFUND_EXECUTE, "dealer_order_refund", refundId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 财务保存与经销回写处于同一事务；回写失败不得让财务先显示成功。 */
    @EventListener
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void afterFinanceRefundStateChanged(com.complex.platform.finance.FinanceRefundStateChanged event) {
        var state = event.refund();
        if (!"dealer_order_refund".equals(state.sourceType())) return;
        DealerOrderRefundRecord found = orders.findRefundByFinanceId(state.id())
                .orElseThrow(() -> ApiException.conflict("DEALER_REFUND_SOURCE_MISSING", "经销退款来源关联缺失"));
        DealerOrderRefundRecord refund = orders.lockRefund(found.id()).orElseThrow();
        if (refund.amountCent() != state.refundAmountCent() || !refund.id().equals(state.targetId())) {
            throw ApiException.conflict("DEALER_REFUND_SOURCE_MISMATCH", "统一财务退款与经销原单金额不一致");
        }
        if ("refunded".equals(refund.status())) return;
        String status = switch (state.status()) {
            case "success" -> "refunded";
            case "processing" -> "processing";
            case "failed" -> "failed";
            default -> "approved";
        };
        var execution = finance.listRefundAttempts(state.id()).stream()
                .filter(attempt -> "execution".equals(attempt.operation())).reduce((first, last) -> last).orElse(null);
        orders.saveRefund(refundState(refund, status, execution == null ? null : execution.paymentOperatorId(),
                "success".equals(state.status()) ? state.succeededAt() : null, state.gatewayRef()));
        if ("refunded".equals(status)) {
            finishCancelledOrder(refund.orderId());
            delivery.finishMoneyIfDeliveryCompleted(refund.orderId());
        }
        audit.recordSuccess("dealer.order.refund_result_recorded", null, "dealer_order_refund", refund.id(), null, null,
                meta("finance_refund_id", state.id(), "status", status));
    }

    private DealerOrderRefundRecord refundState(DealerOrderRefundRecord refund, String status, String executedBy, Instant executedAt, String reference) {
        return new DealerOrderRefundRecord(refund.id(), refund.orderId(), refund.paymentId(), refund.amountCent(), status,
                refund.reviewId(), refund.requestedBy(), executedBy, executedAt, reference, refund.reason(), refund.createdAt(),
                Instant.now(), refund.financeRefundId(), refund.allocationId());
    }

    private void finishCancelledOrder(String orderId) {
        DealerOrderRecord order = orders.lockOrder(orderId).orElseThrow();
        if ("pending_refund".equals(order.status()) && orders.listRefunds(orderId).stream()
                .allMatch(refund -> Set.of("refunded", "voided").contains(refund.status()))) {
            orders.saveOrder(withStatus(order, "cancelled"));
        }
    }

    // ── 逾期 ──

    /** BSM:1231：已出库订单尾款到期未收齐 → 应收转逾期、暂停该经销商新增账期订单；已发货订单不自动取消、不释放库存。 */
    @Transactional
    public int markOverdue(Instant now) {
        int count = 0;
        List<DealerOrderRecord> candidates = orders.listBalanceOverdue(now);
        // 扫描整批共用当前事务：先按统一顺序取得全部经销商锁，再取得任何订单锁。
        candidates.stream().map(DealerOrderRecord::dealerId).distinct().sorted()
                .forEach(id -> dealers.lockDealerCredit(id).orElseThrow());
        for (DealerOrderRecord candidate : candidates) {
            DealerOrderRecord order = orders.lockOrder(candidate.id()).orElseThrow();
            if (order.overdue() || order.paidAmountCent() >= order.netReceivableCent()
                    || order.balanceDueAt() == null || !order.balanceDueAt().isBefore(now)) {
                continue;
            }
            DealerOrderRecord saved = new DealerOrderRecord(order.id(), order.orderNo(), order.dealerId(), order.dealerContractId(), order.salesEntityId(),
                    order.centralWarehouseId(), order.status(), order.statusBeforeCancel(), order.priceCatalogId(), order.creditTerm(), order.goodsAmountCent(),
                    order.freightCent(), order.freightBearer(), order.freightTemplateVersion(), order.totalAmountCent(), order.firstPaymentRequiredCent(),
                    order.firstPaymentDeadlineAt(), order.balanceDueAt(), order.paidAmountCent(), true, order.arApItemId(), order.reviewedBy(), order.reviewedAt(),
                    order.paymentNotifiedAt(), order.shippedBy(), order.shippedAt(), order.signedAt(), order.completedAt(), order.cancelRequestedBy(),
                    order.cancelReason(), order.cancelledAt(), order.note(), order.createdBy(), order.createdAt(), Instant.now());
            orders.saveOrder(saved);
            if (order.arApItemId() != null) {
                orders.updateArApItem(order.arApItemId(), Math.min(order.paidAmountCent(), order.netReceivableCent()),
                        Math.max(0, order.netReceivableCent() - order.paidAmountCent()), "overdue");
            }
            dealers.setCreditSuspended(order.dealerId(), true);
            audit.recordSuccess("dealer.order.balance_overdue", null, "dealer_order", order.id(), null, null,
                    meta("balance_due_at", String.valueOf(order.balanceDueAt()), "unpaid_cent", order.netReceivableCent() - order.paidAmountCent()));
            count++;
        }
        return count;
    }

    // ── 超收用途确认 ──

    @Transactional
    public DealerOrderOverpaymentRecord confirmOverpaymentUse(String overpaymentId, DealerOverpaymentUseRequest request) {
        authorization.requirePermission(PAYMENT_VERIFY);
        try {
            DealerOrderOverpaymentRecord original = orders.findOverpayment(requireText(overpaymentId, "overpayment_id"))
                    .orElseThrow(() -> ApiException.notFound("OVERPAYMENT_NOT_FOUND", "超收记录不存在"));
            requireDealer(original.dealerId());
            String usage = requireText(request == null ? null : request.usage(), "usage");
            if (!Set.of("prepayment", "offset_order", "prepayment_offset").contains(usage)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "用途须为转预付款、未分配款抵订单或预付款抵订单");
            }
            if (request.amountCent() == null || request.amountCent() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "须填写本次分配的正金额");
            }
            String note = requireText(request.note(), "双方确认说明");
            String targetId = "prepayment".equals(usage) ? null : requireText(request.targetOrderId(), "target_order_id");
            if (targetId != null && targetId.equals(original.orderId())) {
                throw ApiException.conflict("OVERPAYMENT_TARGET_MISMATCH", "超收只能抵指定其他订单");
            }
            // 先核实同一经销商及销售主体，再锁共同经销商、编号排序的订单，最后沿原流程锁超收。
            List<String> orderIds = java.util.stream.Stream.of(original.orderId(), targetId).filter(Objects::nonNull).sorted().distinct().toList();
            orderIds.forEach(id -> {
                DealerOrderRecord candidate = requireOrder(id);
                requireOrderProject(id, PAYMENT_VERIFY);
                if (!candidate.dealerId().equals(original.dealerId()) || !candidate.salesEntityId().equals(original.salesEntityId())) {
                    throw ApiException.conflict("OVERPAYMENT_TARGET_MISMATCH", "只能抵扣该经销商向同一销售法律主体采购的订单");
                }
            });
            dealers.lockDealerCredit(original.dealerId()).orElseThrow();
            orderIds.forEach(id -> orders.lockOrder(id).orElseThrow());
            requireOverpaymentConfirmation(original.id(), request.confirmationAttachmentIds());
            return idempotency.execute("dealerOverpaymentUse:" + overpaymentId, request.idempotencyKey(), request, () -> {
                DealerOrderOverpaymentRecord over = orders.lockOverpayment(original.id()).orElseThrow();
                long amount = request.amountCent();
                long available = "prepayment_offset".equals(usage) ? over.prepaymentBalanceCent() : over.unconfirmedAmountCent();
                if (amount > available) throw ApiException.conflict("OVERPAYMENT_BALANCE_INSUFFICIENT", "本次金额超过对应可用余额，已抵用或待退款的金额不能重复使用");
                if (targetId != null) {
                    DealerOrderRecord target = orders.findOrder(targetId).orElseThrow();
                    if (!target.dealerId().equals(over.dealerId()) || !target.salesEntityId().equals(over.salesEntityId())) {
                        throw ApiException.conflict("OVERPAYMENT_TARGET_MISMATCH", "只能抵扣该经销商向同一销售法律主体采购的订单");
                    }
                    if (!Set.of("pending_payment", "payment_verifying", "pending_shipment", "shipping", "signed", "returning", "partially_returned", "returned").contains(target.status())) {
                        throw ApiException.conflict("DEALER_ORDER_STATUS_INVALID", "指定订单当前不可抵扣");
                    }
                    long unpaid = Math.max(0, target.netReceivableCent() - target.paidAmountCent());
                    if (amount > unpaid) throw ApiException.conflict("OVERPAYMENT_EXCEEDS_TARGET", "本次分配超过指定订单未收货款，须按双方确认拆分，不能自动改用途");
                    applyReceipt(target, amount, Instant.now());
                }
                Instant now = Instant.now();
                orders.saveAllocation(new DealerOverpaymentAllocationRecord("doa_" + UUID.randomUUID(), over.id(), usage, amount,
                        targetId, List.copyOf(request.confirmationAttachmentIds()), actorId(), now, note, null));
                DealerOrderOverpaymentRecord after = orders.findOverpayment(over.id()).orElseThrow();
                String status = after.unconfirmedAmountCent() > 0 ? "partially_allocated" : after.prepaymentBalanceCent() > 0 ? "prepayment" : "offset_applied";
                orders.saveOverpayment(new DealerOrderOverpaymentRecord(over.id(), over.orderId(), over.dealerId(), over.salesEntityId(), over.paymentId(),
                        over.amountCent(), status, targetId, actorId(), now, note, over.createdAt(), after.unconfirmedAmountCent(), after.prepaymentBalanceCent(),
                        after.appliedAmountCent(), after.allocations()));
                audit.recordSuccess("dealer.order.overpayment_use_confirmed", PAYMENT_VERIFY, "dealer_order_overpayment", over.id(), null, request.idempotencyKey(),
                        meta("usage", usage, "target_order_id", nz(targetId), "amount_cent", amount, "prepayment_balance_cent", after.prepaymentBalanceCent()));
                return orders.findOverpayment(over.id()).orElseThrow();
            }, DealerOrderOverpaymentRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.order.overpayment_use_failed", PAYMENT_VERIFY, "dealer_order_overpayment", overpaymentId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    private String orderProject(String orderId) {
        return orders.findOrderProjectId(orderId)
                .orElseThrow(() -> ApiException.conflict("DEALER_ORDER_PROJECT_MISSING", "订单锁定中心仓的项目缺失或与经销范围冲突，须先核实"));
    }

    private boolean canAccessOrderProject(String orderId, String permission) {
        return authorization.canReachProject(authorization.currentActor(), orderProject(orderId), permission);
    }

    private String requireOrderProject(String orderId, String permission) {
        String projectId = orderProject(orderId);
        if (!authorization.canReachProject(authorization.currentActor(), projectId, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "订单款项不在授权项目范围内");
        }
        return projectId;
    }

    private void requireOverpaymentConfirmation(String overpaymentId, List<String> ids) {
        if (ids == null || ids.isEmpty()) throw ApiException.badRequest("OVERPAYMENT_CONFIRMATION_REQUIRED", "须上传双方确认本次资金用途的凭证");
        for (String id : ids) {
            var attachment = evidence.findAttachmentById(id)
                    .orElseThrow(() -> ApiException.badRequest("EVIDENCE_ATTACHMENT_NOT_FOUND", "双方确认凭证不存在"));
            if (!"dealer_overpayment".equals(attachment.businessType()) || !overpaymentId.equals(attachment.businessId())) {
                throw ApiException.badRequest("OVERPAYMENT_EVIDENCE_MISMATCH", "双方确认凭证须关联本笔超收");
            }
            attachmentValidator.requireUploaded(attachment);
        }
    }

    // ── 定价 / 金额 ──

    private record Priced(long unitPriceCent, Integer taxRateBps, String source, String inquiryId) {
    }

    private record Amounts(long goods, long freight, long total) {
    }

    /** BSM:1220：已定合同价商品按现行生效目录；未定价商品须凭已报价（有效期内）的询价单。 */
    private Priced price(DealerContractRecord contract, String dealerId, DealerOrderLineRequest line) {
        String spec = line.spec() == null ? "" : line.spec().trim();
        if (line.inquiryId() != null && !line.inquiryId().isBlank()) {
            DealerInquiryRecord inquiry = dealers.findInquiry(line.inquiryId().trim())
                    .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND", "询价不存在"));
            if (!inquiry.dealerId().equals(dealerId) || !inquiry.dealerContractId().equals(contract.id()) || !inquiry.itemId().equals(line.itemId().trim())
                    || !inquiry.spec().equals(spec)) {
                throw ApiException.badRequest("INQUIRY_MISMATCH", "询价单与订单明细（经销商、合同、商品、规格）不一致");
            }
            if (!"quoted".equals(inquiry.status()) || (inquiry.quoteValidUntil() != null && inquiry.quoteValidUntil().isBefore(LocalDate.now()))) {
                throw ApiException.conflict("INQUIRY_NOT_QUOTED", "询价未经报价审批通过或报价已失效，不能订购");
            }
            return new Priced(inquiry.quotedPriceCent(), null, "inquiry", inquiry.id());
        }
        DealerPriceCatalogRecord catalog = dealers.findEffectiveCatalog(contract.id())
                .orElseThrow(() -> ApiException.conflict("DEALER_CATALOG_NOT_EFFECTIVE", "合同尚无生效的价格目录"));
        return dealers.listCatalogLines(catalog.id()).stream()
                .filter(l -> l.itemId().equals(line.itemId().trim()) && l.spec().equals(spec)).findFirst()
                .map(l -> new Priced(l.unitPriceCent(), l.taxRateBps(), "catalog", null))
                .orElseThrow(() -> ApiException.conflict("DEALER_LINE_UNPRICED", "该商品未定合同价，只能先询价：" + line.itemId()));
    }

    /** BSM:1229 运费模板：承担方为经销商且货款未达包邮门槛时收运费。 */
    private static Amounts amounts(DealerContractRecord contract, List<DealerOrderLineRecord> lines) {
        long goods = lines.stream().mapToLong(DealerOrderLineRecord::lineAmountCent).sum();
        long freight = 0;
        if ("dealer".equals(contract.freightBearer()) && contract.freightCent() != null
                && (contract.freeShippingThresholdCent() == null || goods < contract.freeShippingThresholdCent())) {
            freight = contract.freightCent();
        }
        return new Amounts(goods, freight, goods + freight);
    }

    /** BSM:1227 / 1231：约定账期按比例或金额；未约定账期＝全款。 */
    private static long firstPaymentRequired(DealerContractRecord contract, long total) {
        if (!contract.creditTerm()) {
            return total;
        }
        if (contract.firstPaymentAmountCent() != null) {
            return Math.min(contract.firstPaymentAmountCent(), total);
        }
        int bps = contract.firstPaymentRatioBps() == null ? 10000 : contract.firstPaymentRatioBps();
        return Math.min(total, (total * bps + 9999) / 10000);
    }

    // ── helpers ──

    private DealerRecord requireDealer(String id) {
        DealerRecord dealer = dealers.findDealer(requireText(id, "dealer_id")).orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
        String scope = DealerPortalAccountService.projectScopeOf(authorization);
        if (scope != null && dealer.projectId() != null && !scope.equals(dealer.projectId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权访问其他项目的经销商");
        }
        return dealer;
    }

    private DealerRecord requireActiveDealer(String id) {
        DealerRecord dealer = requireDealer(id);
        if (DealerService.DEACTIVATED.equals(dealer.status())) {
            throw ApiException.conflict("DEALER_DEACTIVATED", "经销商已停用，禁止新订单与门户内付款");
        }
        if (!DealerService.APPROVED.equals(dealer.status())) {
            throw ApiException.conflict("DEALER_NOT_APPROVED", "经销商准入尚未通过");
        }
        return dealer;
    }

    private DealerContractRecord requireContract(String id) {
        DealerContractRecord contract = dealers.findContract(requireText(id, "dealer_contract_id"))
                .orElseThrow(() -> ApiException.notFound("DEALER_CONTRACT_NOT_FOUND", "经销合同不存在"));
        requireDealer(contract.dealerId());
        return contract;
    }

    /** 调用入口先完成真实权限范围校验；所有信用状态写入须先持共同经销商锁，再锁订单并重读。 */
    void lockDealerForOrder(String orderId) {
        DealerOrderRecord candidate = requireOrder(orderId);
        dealers.lockDealerCredit(candidate.dealerId()).orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
    }

    private DealerOrderRecord requireOrder(String id) {
        DealerOrderRecord order = orders.findOrder(requireText(id, "order_id")).orElseThrow(() -> ApiException.notFound("DEALER_ORDER_NOT_FOUND", "经销订单不存在"));
        requireDealer(order.dealerId());
        return order;
    }

    private DealerOrderPaymentRecord requirePayment(String id) {
        DealerOrderPaymentRecord payment = orders.findPayment(requireText(id, "payment_id")).orElseThrow(() -> ApiException.notFound("DEALER_PAYMENT_NOT_FOUND", "到账记录不存在"));
        requireOrder(payment.orderId());
        return payment;
    }

    private DealerOrderRefundRecord requireRefund(String id) {
        DealerOrderRefundRecord refund = orders.findRefund(requireText(id, "refund_id")).orElseThrow(() -> ApiException.notFound("DEALER_REFUND_NOT_FOUND", "退款记录不存在"));
        requireOrder(refund.orderId());
        return refund;
    }

    private static void requireStatus(DealerOrderRecord order, String... allowed) {
        for (String s : allowed) {
            if (s.equals(order.status())) {
                return;
            }
        }
        throw ApiException.conflict("DEALER_ORDER_STATUS_INVALID", "订单当前状态不允许此操作：" + order.status());
    }

    private static DealerOrderRecord withStatus(DealerOrderRecord o, String status) {
        return new DealerOrderRecord(o.id(), o.orderNo(), o.dealerId(), o.dealerContractId(), o.salesEntityId(), o.centralWarehouseId(), status,
                "cancelling".equals(status) ? o.status() : null, o.priceCatalogId(), o.creditTerm(), o.goodsAmountCent(), o.freightCent(), o.freightBearer(),
                o.freightTemplateVersion(), o.totalAmountCent(), o.firstPaymentRequiredCent(), o.firstPaymentDeadlineAt(), o.balanceDueAt(), o.paidAmountCent(),
                o.overdue(), o.arApItemId(), o.reviewedBy(), o.reviewedAt(), o.paymentNotifiedAt(), o.shippedBy(), o.shippedAt(), o.signedAt(), o.completedAt(),
                o.cancelRequestedBy(), o.cancelReason(), "cancelled".equals(status) && o.cancelledAt() == null ? Instant.now() : o.cancelledAt(), o.note(),
                o.createdBy(), o.createdAt(), Instant.now());
    }

    private static DealerOrderPaymentRecord withPaymentStatus(DealerOrderPaymentRecord p, String status, long applied, String verifiedBy, Instant verifiedAt, String note) {
        return new DealerOrderPaymentRecord(p.id(), p.orderId(), p.method(), p.amountCent(), p.receivedAt(), p.referenceNo(), status, applied, p.registeredBy(),
                verifiedBy, verifiedAt, note == null ? p.note() : note, p.createdAt(), Instant.now());
    }

    private static void validateDueRule(String anchor, Integer days, Instant fixedAt, Instant signedAt, boolean balance) {
        Set<String> allowed = balance ? Set.of("contract_signed", "order_created", "payment_notified", "shipped", "signed", "fixed_date")
                : Set.of("contract_signed", "order_created", "payment_notified", "fixed_date");
        if (anchor == null || !allowed.contains(anchor)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "付款起点须按合同明确选择");
        }
        if ("fixed_date".equals(anchor)) {
            if (fixedAt == null || days != null) throw ApiException.badRequest("VALIDATION_FAILED", "固定截止须填写具体时间，不同时填写事件后天数");
        } else if (days == null || days <= 0 || fixedAt != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "事件起点须填写正天数，不同时填写固定截止");
        }
        if ("contract_signed".equals(anchor) && signedAt == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "以签约为起点须登记合同实际签署时间");
        }
    }

    private static Instant resolveDue(String anchor, Instant fixedAt, Integer days, Instant contractSigned, Instant created,
            Instant notified, Instant shipped, Instant signed) {
        if ("fixed_date".equals(anchor)) return fixedAt;
        Instant origin = switch (anchor) {
            case "contract_signed" -> contractSigned;
            case "order_created" -> created;
            case "payment_notified" -> notified;
            case "shipped" -> shipped;
            case "signed" -> signed;
            default -> throw ApiException.conflict("DEALER_CONTRACT_TERMS_INCOMPLETE", "合同未锁定有效付款起点");
        };
        return origin == null ? null : origin.plus(days, ChronoUnit.DAYS);
    }

    private static DealerContractRecord requireSnapshot(DealerOrderRecord order) {
        if (order.contractSnapshot() == null) {
            throw ApiException.conflict("DEALER_CONTRACT_SNAPSHOT_MISSING", "订单缺少已锁定合同，须核实原合同");
        }
        return order.contractSnapshot();
    }

    private static String requireDecision(String decision) {
        String value = requireText(decision, "decision");
        if (!List.of("approve", "reject").contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论须为通过（approve）或驳回（reject）");
        }
        return value;
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
