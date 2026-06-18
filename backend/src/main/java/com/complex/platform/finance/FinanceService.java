package com.complex.platform.finance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
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

    private final FinanceRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
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

    private ArApItemRecord createItem(ArApItemRequest request) {
        ArApItemRequest actual = requireRequest(request);
        String direction = requireEnum(actual.direction(), DIRECTIONS, "direction");
        String sourceType = optionalEnum(actual.sourceType(), SOURCE_TYPES, "source_type");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
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
        return repository.saveDailyClose(new DailyCloseRecord(
                "daily_close_" + slug(actual.idempotencyKey()),
                businessDate,
                scopeType,
                scopeId,
                actual.forceClose() != null && actual.forceClose(),
                "locked",
                now,
                now
        ));
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
        return null;
    }
}
