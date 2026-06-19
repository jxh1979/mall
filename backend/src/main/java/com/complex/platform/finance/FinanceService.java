package com.complex.platform.finance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.RefundRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> DIRECTIONS = Set.of("receivable", "payable");
    private static final Set<String> SOURCE_TYPES = Set.of("contract", "order", "refund", "manual", "settlement", "bar_bill");
    private static final Set<String> DAILY_CLOSE_SCOPES = Set.of("project", "merchant", "bar", "ticketing", "amusement");
    private static final Set<String> OFFLINE_PAYMENT_METHODS = Set.of("cash", "bank_transfer", "pos", "other");
    // R5 T7：六源（与 finance_daily_close_summaries.chk_fdcs_source 一致）。
    private static final List<String> DAILY_CLOSE_SOURCES = List.of(
            "official_ticket", "channel_ticket", "bar", "amusement", "supply_cost", "thirdparty_reflow");
    private static final Set<String> ADJUSTMENT_DECISIONS = Set.of("approve", "reject");

    private final FinanceRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.common.BusinessDayResolver businessDayResolver;

    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.businessDayResolver = businessDayResolver;
    }

    public PageResponse<ArApItemRecord> listArApItems() {
        return readPage(
                "finance_ar_ap.viewed",
                "finance_ar_ap.view_failed",
                "finance.ar_ap.view",
                "ar_ap_item",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.isPresent()
                            ? repository.listArApItemsByMerchant(scope.get())
                            : repository.listArApItems();
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.ar_ap.manage", admissionAction = "finance.locked_backfill", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord createArApItem(ArApItemRequest request) {
        return executeWrite(
                "createArApItem",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_ar_ap.created",
                "finance_ar_ap.create_failed",
                "finance.ar_ap.manage",
                "ar_ap_item",
                () -> createItem(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public ArApPaymentResponse registerArApPayment(String arApItemId, ArApPaymentRequest request) {
        String itemId = requireText(arApItemId, "ar_ap_item_id");
        return executeWrite(
                "createArApPayment:" + itemId,
                request == null ? null : request.idempotencyKey(),
                request,
                ArApPaymentResponse.class,
                "finance_payment.registered",
                "finance_payment.register_failed",
                "finance.payment.register",
                "ar_ap_payment",
                () -> registerPayment(itemId, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.daily_close.lock")
    @Transactional
    public DailyCloseRecord createDailyClose(DailyCloseRequest request) {
        return executeWrite(
                "createDailyClose",
                request == null ? null : request.idempotencyKey(),
                request,
                DailyCloseRecord.class,
                "finance_daily_close.locked",
                "finance_daily_close.lock_failed",
                "finance.daily_close.lock",
                "finance_daily_close",
                () -> lockDailyClose(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public PaymentIntentRecord createPaymentIntent(PaymentIntentRequest request) {
        return executeWrite(
                "createPaymentIntent",
                request == null ? null : request.idempotencyKey(),
                request,
                PaymentIntentRecord.class,
                "payment.intent_created",
                "payment.intent_failed",
                "finance.payment_intent.create",
                "finance_payment_intent",
                () -> createIntent(request)
        );
    }

    /** 按 id 查支付意图（储值充值/会员开通：入账前校验支付意图存在且已成功）。 */
    public Optional<PaymentIntentRecord> findPaymentIntent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return Optional.empty();
        }
        return repository.findPaymentIntentById(paymentIntentId);
    }

    /**
     * 将支付意图标记为成功（占位代替真实支付渠道回调）。储值充值 / 付费会员开通在测试与本地联调期
     * 通过本方法把绑定的支付意图置 succeeded，再由各自服务校验后入账，保证"无支付不入账"。
     * 真实支付渠道接入后由渠道回调驱动此状态。
     */
    @Transactional
    public PaymentIntentRecord markPaymentIntentSucceeded(String paymentIntentId) {
        PaymentIntentRecord intent = repository.findPaymentIntentById(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        if ("succeeded".equals(intent.status())) {
            return intent;
        }
        if (!"created".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_INTENT_NOT_PENDING", "payment intent is not pending");
        }
        return repository.savePaymentIntent(new PaymentIntentRecord(
                intent.id(),
                intent.businessOrderType(),
                intent.businessOrderId(),
                intent.amountCent(),
                intent.currency(),
                intent.channel(),
                "succeeded",
                intent.createdAt(),
                Instant.now()));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register", admissionAction = "finance.offline_payment_confirm", objectType = "finance_offline_payment_confirmation")
    @Transactional
    public OfflinePaymentConfirmationRecord confirmOfflinePayment(
            String paymentId,
            OfflinePaymentConfirmRequest request
    ) {
        String pathPaymentId = requireText(paymentId, "payment_id");
        return executeWrite(
                "confirmOfflinePayment:" + pathPaymentId,
                request == null ? null : request.idempotencyKey(),
                request,
                OfflinePaymentConfirmationRecord.class,
                "finance_offline_payment.confirmed",
                "finance_offline_payment.confirm_failed",
                "finance.payment.register",
                "finance_offline_payment_confirmation",
                () -> confirmOffline(pathPaymentId, request)
        );
    }

    public PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return readPage(
                "finance.reconciliation_batch.viewed",
                "finance.reconciliation_batch.viewed_failed",
                "finance.reconciliation.view",
                "finance_reconciliation_batch",
                () -> {
                    requirePlatformReconciliationAccess();
                    return repository.listReconciliationBatches();
                }
        );
    }

    public PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return readPage(
                "finance_reconciliation.difference_found",
                "finance_reconciliation.view_failed",
                "openplatform.reconcile.execute",
                "finance_reconciliation_difference",
                () -> {
                    requirePlatformReconciliationAccess();
                    return repository.listReconciliationDifferences();
                }
        );
    }

    /**
     * Reconciliation batches/differences are platform/channel-level finance artifacts with no merchant
     * dimension in the data model. A merchant-scoped caller must not read them — otherwise it would see
     * every merchant's reconciliation data. Platform admins (no merchant scope) may read.
     */
    private void requirePlatformReconciliationAccess() {
        if (authorizationService.currentMerchantScope().isPresent()) {
            throw ApiException.forbidden(
                    "RECONCILIATION_PLATFORM_ONLY",
                    "reconciliation data is platform-scoped and not available to merchant-scoped users");
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.refund.create", admissionAction = "finance.refund", objectType = "finance_refund")
    @Transactional
    public FinanceRefundRecord createRefund(RefundRequest request) {
        return executeWrite(
                "createFinanceRefund",
                request == null ? null : request.idempotencyKey(),
                request,
                FinanceRefundRecord.class,
                "finance_refund.created",
                "finance_refund.create_failed",
                "finance.refund.create",
                "finance_refund",
                () -> refund(request)
        );
    }

    public PageResponse<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId) {
        String id = requireText(dailyCloseId, "daily_close_id");
        return readPage(
                "finance.daily_close_summary.viewed",
                "finance.daily_close_summary.view_failed",
                "finance.daily_close.manage",
                "finance_daily_close_summary",
                () -> {
                    DailyCloseRecord dailyClose = repository.findDailyCloseById(id)
                            .orElseThrow(() -> ApiException.notFound("DAILY_CLOSE_NOT_FOUND", "daily close not found"));
                    // 隔离：商户角色只能看自身 scope 的日结汇总。
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent()
                            && "merchant".equals(dailyClose.scopeType())
                            && !scope.get().equals(dailyClose.scopeId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "access to another merchant's daily close is not allowed");
                    }
                    return repository.listDailyCloseSummaries(id);
                });
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.locked_backfill.request")
    @Transactional
    public ArApAdjustmentRecord requestAdjustment(ArApAdjustmentRequest request) {
        return executeWrite(
                "requestArApAdjustment",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApAdjustmentRecord.class,
                "finance.locked_backfill_requested",
                "finance.locked_backfill_request_failed",
                "finance.locked_backfill.request",
                "ar_ap_adjustment",
                () -> createAdjustment(request));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.locked_backfill.approve", admissionAction = "finance.locked_backfill_approve", objectType = "ar_ap_adjustment")
    @Transactional
    public ArApAdjustmentRecord approveAdjustment(String adjustmentId, ArApAdjustmentDecisionRequest request) {
        String id = requireText(adjustmentId, "adjustment_id");
        String decision = requireEnum(request == null ? null : request.decision(), ADJUSTMENT_DECISIONS, "decision");
        boolean approve = "approve".equals(decision);
        return executeWrite(
                "decideArApAdjustment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                ArApAdjustmentRecord.class,
                approve ? "finance.locked_backfill_approved" : "finance.locked_backfill_rejected",
                "finance.locked_backfill_decision_failed",
                "finance.locked_backfill.approve",
                "ar_ap_adjustment",
                () -> decideAdjustment(id, approve));
    }

    private ArApAdjustmentRecord createAdjustment(ArApAdjustmentRequest request) {
        ArApAdjustmentRequest actual = requireRequest(request);
        String itemId = requireText(actual.arApItemId(), "ar_ap_item_id");
        long amountCent = requireNonZero(actual.adjustmentAmountCent(), "adjustment_amount_cent");
        String reason = requireText(actual.reason(), "reason");

        ArApItemRecord item = repository.findArApItemById(itemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        checkMerchantOwnership(item.merchantId());

        // 补录只针对已锁定台账（未锁定应直接改账，不走审批）。
        String lockedDayCloseId = repository.findArApItemLockedDayCloseId(itemId)
                .orElseThrow(() -> ApiException.badRequest("AR_AP_ITEM_NOT_LOCKED",
                        "AR/AP item is not locked; modify it directly instead of requesting a backfill"));

        Instant now = Instant.now();
        return repository.saveArApAdjustment(new ArApAdjustmentRecord(
                "arap_adj_" + slug(itemId + "_" + actual.idempotencyKey()),
                itemId,
                lockedDayCloseId,
                amountCent,
                "pending",
                authorizationService.currentActor().id(),
                null,
                reason,
                now,
                null));
    }

    private ArApAdjustmentRecord decideAdjustment(String adjustmentId, boolean approve) {
        ArApAdjustmentRecord adjustment = repository.findArApAdjustmentByIdForUpdate(adjustmentId)
                .orElseThrow(() -> ApiException.notFound("ADJUSTMENT_NOT_FOUND", "adjustment not found"));
        if (!"pending".equals(adjustment.approvalStatus())) {
            throw ApiException.conflict("ADJUSTMENT_ALREADY_DECIDED",
                    "adjustment is already " + adjustment.approvalStatus());
        }

        ArApItemRecord item = repository.findArApItemByIdForUpdate(adjustment.arApItemId())
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        checkMerchantOwnership(item.merchantId());

        Instant now = Instant.now();
        String approver = authorizationService.currentActor().id();

        if (approve) {
            // 审批通过：真正写入金额变动（带符号），同步刷新 paid/unpaid/status，并留痕。
            long newAmountCent = item.amountCent() + adjustment.adjustmentAmountCent();
            if (newAmountCent < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "adjustment would drive amount negative");
            }
            long unpaidAmountCent = newAmountCent - item.paidAmountCent();
            if (unpaidAmountCent < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "adjustment would drive unpaid amount negative");
            }
            String status = unpaidAmountCent == 0
                    ? "settled"
                    : ("receivable".equals(item.direction()) ? "pending_collection" : "pending_payment");
            repository.updateArApItemLedger(new ArApItemRecord(
                    item.id(),
                    item.itemNo(),
                    item.direction(),
                    item.counterpartyId(),
                    item.sourceType(),
                    item.sourceId(),
                    newAmountCent,
                    item.paidAmountCent(),
                    unpaidAmountCent,
                    item.currency(),
                    item.dueDate(),
                    item.accountingPeriod(),
                    status,
                    item.createdAt(),
                    now,
                    item.merchantId()));
        }
        // reject：不改账，只标状态。
        return repository.updateArApAdjustment(new ArApAdjustmentRecord(
                adjustment.id(),
                adjustment.arApItemId(),
                adjustment.lockedDayCloseId(),
                adjustment.adjustmentAmountCent(),
                approve ? "approved" : "rejected",
                adjustment.requestedBy(),
                approver,
                adjustment.reason(),
                adjustment.createdAt(),
                now));
    }

    private void requireNotLocked(String arApItemId) {
        if (repository.findArApItemLockedDayCloseId(arApItemId).isPresent()) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "AR/AP item is locked by a daily close; use the backfill adjustment workflow");
        }
    }

    private long requireNonZero(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value == 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-zero");
        }
        return value;
    }

    private ArApItemRecord createItem(ArApItemRequest request) {
        ArApItemRequest actual = requireRequest(request);
        String direction = requireEnum(actual.direction(), DIRECTIONS, "direction");
        String sourceType = optionalEnum(actual.sourceType(), SOURCE_TYPES, "source_type");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
        // R5 T7 硬拦截：禁止向已锁定经营日新增/回填台账，必须走补录审批。
        if (repository.lockedDailyCloseExistsForDate(dueDate)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + dueDate + " is locked; use the backfill adjustment workflow");
        }
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        String id = "arap_" + slug;
        Optional<String> scope = authorizationService.currentMerchantScope();
        ArApItemRecord item = new ArApItemRecord(
                id,
                "ARAP" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                direction,
                requireText(actual.counterpartyId(), "counterparty_id"),
                sourceType,
                trimToNull(actual.sourceId()),
                amountCent,
                0L,
                amountCent,
                CURRENCY_CNY,
                dueDate,
                trimToNull(actual.accountingPeriod()),
                "receivable".equals(direction) ? "pending_collection" : "pending_payment",
                now,
                now,
                scope.orElse(null)
        );
        ArApItemRecord saved = repository.saveArApItem(item);
        if (saved.sourceId() != null && saved.sourceType() != null) {
            repository.saveArApItemSource(new ArApItemSourceRecord(
                    "arap_src_" + slug(actual.idempotencyKey()),
                    saved.id(),
                    saved.sourceType(),
                    saved.sourceId(),
                    saved.amountCent(),
                    now
            ));
        }
        return saved;
    }

    private ArApPaymentResponse registerPayment(String arApItemId, ArApPaymentRequest request) {
        ArApPaymentRequest actual = requireRequest(request);
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");

        // P0 fix: SELECT FOR UPDATE prevents concurrent reads from both seeing unpaid > 0
        // and both writing a payment, which would silently double-charge the ledger.
        ArApItemRecord current = repository.findArApItemByIdForUpdate(arApItemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));

        // P1 fix: reject cross-merchant writes
        checkMerchantOwnership(current.merchantId());

        // R5 T7 硬拦截：已锁定台账禁止直接登记收付款，必须走补录审批。
        requireNotLocked(current.id());

        if (amountCent > current.unpaidAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment amount exceeds unpaid amount");
        }
        Instant now = Instant.now();
        // P0 fix: include arApItemId in the slug so the same idempotency key used on two
        // different items produces two different payment IDs, preventing ON CONFLICT overwrite.
        ArApPaymentRecord payment = repository.saveArApPayment(new ArApPaymentRecord(
                "arap_pay_" + slug(arApItemId + "_" + actual.idempotencyKey()),
                current.id(),
                amountCent,
                requireText(actual.paymentMethod(), "payment_method"),
                actual.paymentTime() == null ? now : actual.paymentTime(),
                trimToNull(actual.bankFlowRef()),
                now
        ));
        long paidAmountCent = current.paidAmountCent() + amountCent;
        long unpaidAmountCent = current.amountCent() - paidAmountCent;
        String status = unpaidAmountCent == 0 ? "settled" : "partial_paid";
        ArApItemRecord updated = repository.updateArApItemLedger(new ArApItemRecord(
                current.id(),
                current.itemNo(),
                current.direction(),
                current.counterpartyId(),
                current.sourceType(),
                current.sourceId(),
                current.amountCent(),
                paidAmountCent,
                unpaidAmountCent,
                current.currency(),
                current.dueDate(),
                current.accountingPeriod(),
                status,
                current.createdAt(),
                now,
                current.merchantId()
        ));
        return new ArApPaymentResponse(payment, updated);
    }

    private DailyCloseRecord lockDailyClose(DailyCloseRequest request) {
        DailyCloseRequest actual = requireRequest(request);
        LocalDate businessDate = requireDate(actual.businessDate(), "business_date");
        String scopeType = requireEnum(actual.scopeType(), DAILY_CLOSE_SCOPES, "scope_type");
        String scopeId = requireText(actual.scopeId(), "scope_id");

        // P1 fix: prevent duplicate locks for the same date + scope using different keys
        if (repository.dailyCloseExists(businessDate, scopeType, scopeId)) {
            throw ApiException.conflict("DAILY_CLOSE_EXISTS",
                    "daily close already locked for date " + businessDate + " scope " + scopeType + "/" + scopeId);
        }
        Instant now = Instant.now();
        String dailyCloseId = "daily_close_" + slug(actual.idempotencyKey());
        DailyCloseRecord saved = repository.saveDailyClose(new DailyCloseRecord(
                dailyCloseId,
                businessDate,
                scopeType,
                scopeId,
                actual.forceClose() != null && actual.forceClose(),
                "locked",
                now,
                now
        ));

        // R5 T7：六源汇总写入 finance_daily_close_summaries（口径见 r5-aggregation-metrics-draft §1.2，待财务复核）。
        summarizeDailyClose(dailyCloseId, businessDate, now);

        // R5 T7：把当日属于本 scope 的 ar_ap_items 打上 locked_day_close_id（merchant 角色仅锁本商户行）。
        Optional<String> merchantScope = authorizationService.currentMerchantScope();
        repository.lockArApItemsForDailyClose(dailyCloseId, businessDate, merchantScope.orElse(null));

        return saved;
    }

    /**
     * 六源汇总聚合（official_ticket / channel_ticket / bar / amusement / supply_cost / thirdparty_reflow），
     * 每源一行写入 finance_daily_close_summaries。聚合公式按聚合草案保守取值，口径未定处见草案 §四"待财务复核"。
     */
    private void summarizeDailyClose(String dailyCloseId, LocalDate businessDate, Instant now) {
        // 经营日窗口按可配日切（默认 04:00）派生，时间戳类源表统一用 [from, to) 分桶；official_ticket 以 visit_date 为经营日。
        Instant from = businessDayResolver.startInstant(businessDate);
        Instant to = businessDayResolver.endInstant(businessDate);
        for (String source : DAILY_CLOSE_SOURCES) {
            FinanceRepository.DailyCloseSourceTotals totals = switch (source) {
                case "official_ticket" -> repository.aggregateOfficialTicket(businessDate, from, to);
                case "channel_ticket" -> repository.aggregateChannelTicket(businessDate, from, to);
                case "bar" -> repository.aggregateBar(businessDate, from, to);
                case "amusement" -> repository.aggregateAmusement(businessDate, from, to);
                case "supply_cost" -> repository.aggregateSupplyCost(businessDate, from, to);
                case "thirdparty_reflow" -> repository.aggregateThirdpartyReflow(businessDate, from, to);
                default -> FinanceRepository.DailyCloseSourceTotals.empty();
            };
            repository.saveDailyCloseSummary(new DailyCloseSummaryRecord(
                    "fdcs_" + slug(dailyCloseId + "_" + source),
                    dailyCloseId,
                    source,
                    totals.amountCent(),
                    totals.refundCent(),
                    totals.orderCount(),
                    now
            ));
        }
        auditEventService.recordSuccess(
                "finance.daily_close_summarized",
                "finance.daily_close.manage",
                "finance_daily_close_summary",
                dailyCloseId,
                null,
                null,
                Map.of("source_count", DAILY_CLOSE_SOURCES.size()));
    }

    private PaymentIntentRecord createIntent(PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        Instant now = Instant.now();
        return repository.savePaymentIntent(new PaymentIntentRecord(
                "pay_intent_" + slug(actual.idempotencyKey()),
                requireText(actual.businessOrderType(), "business_order_type"),
                requireText(actual.businessOrderId(), "business_order_id"),
                requirePositive(actual.amountCent(), "amount_cent"),
                trimToDefault(actual.currency(), CURRENCY_CNY),
                requireText(actual.channel(), "channel"),
                "created",
                now,
                now
        ));
    }

    private OfflinePaymentConfirmationRecord confirmOffline(
            String pathPaymentId,
            OfflinePaymentConfirmRequest request
    ) {
        OfflinePaymentConfirmRequest actual = requireRequest(request);
        String bodyPaymentId = requireText(actual.paymentId(), "payment_id");
        if (!pathPaymentId.equals(bodyPaymentId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment_id must match path");
        }
        Instant now = Instant.now();
        return repository.saveOfflinePaymentConfirmation(new OfflinePaymentConfirmationRecord(
                "offline_confirm_" + slug(actual.idempotencyKey()),
                pathPaymentId,
                requirePositive(actual.amountCent(), "amount_cent"),
                requireEnum(actual.paymentMethod(), OFFLINE_PAYMENT_METHODS, "payment_method"),
                actual.confirmedAt() == null ? now : actual.confirmedAt(),
                actual.evidenceAttachmentIds() == null ? List.of() : List.copyOf(actual.evidenceAttachmentIds()),
                "confirmed",
                now
        ));
    }

    private FinanceRefundRecord refund(RefundRequest request) {
        RefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        long refundAmountCent = requirePositive(actual.refundAmountCent(), "refund_amount_cent");

        // P1 fix: validate target payment exists
        ArApPaymentRecord payment = repository.findArApPaymentById(targetId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "payment not found: " + targetId));

        // P1 fix: reject cross-merchant refund attempts
        ArApItemRecord item = repository.findArApItemById(payment.arApItemId())
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        checkMerchantOwnership(item.merchantId());

        // R5 T7 硬拦截：已锁定台账禁止直接退款（影响金额），必须走补录审批。
        requireNotLocked(item.id());

        // P1 fix: prevent refunding more than the original payment (cumulative check)
        long alreadyRefunded = repository.sumRefundedAmountByTargetId(targetId);
        if (alreadyRefunded + refundAmountCent > payment.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "refund amount exceeds refundable balance (payment: " + payment.amountCent()
                            + ", already refunded: " + alreadyRefunded + ")");
        }

        return repository.saveRefund(new FinanceRefundRecord(
                "refund_" + slug(actual.idempotencyKey()),
                targetId,
                refundAmountCent,
                requireText(actual.reason(), "reason"),
                trimToDefault(actual.refundChannel(), "manual"),
                "requested",
                Instant.now()
        ));
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "access to another merchant's resource is not allowed");
        }
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        return operations.readPage(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private long requirePositive(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = requireText(value, fieldName);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private String optionalEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }

    private String businessId(Object value) {
        if (value instanceof ArApItemRecord record) {
            return record.id();
        }
        if (value instanceof ArApPaymentResponse response) {
            return response.payment().id();
        }
        if (value instanceof DailyCloseRecord record) {
            return record.id();
        }
        if (value instanceof PaymentIntentRecord record) {
            return record.id();
        }
        if (value instanceof OfflinePaymentConfirmationRecord record) {
            return record.id();
        }
        if (value instanceof FinanceRefundRecord record) {
            return record.id();
        }
        if (value instanceof ArApAdjustmentRecord record) {
            return record.id();
        }
        return null;
    }
}
