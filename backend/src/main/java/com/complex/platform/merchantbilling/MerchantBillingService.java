package com.complex.platform.merchantbilling;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.merchantbilling.dto.MerchantBillConfirmRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillCreateRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDisputeRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDunningRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillLineRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
public class MerchantBillingService {

    private static final String BUSINESS_TYPE = "merchant_bill";
    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> FEE_TYPES = Set.of("rent", "property_fee", "energy_fee", "cam", "deposit", "other");
    private static final Set<String> SOURCE_TYPES = Set.of("ar_ap", "manual", "contract", "meter", "billing_batch");
    private static final Set<String> CONFIRMABLE_STATUSES = Set.of("issued", "viewed", "overdue");
    private static final Set<String> DISPUTABLE_STATUSES = Set.of("issued", "viewed", "overdue", "confirmed");
    private static final Set<String> DUNNING_CHANNELS = Set.of("wecom", "sms", "email", "manual");
    private static final Set<String> DUNNING_STATUSES = Set.of("sent", "failed", "scheduled");

    private final MerchantBillingRepository repository;
    private final FinanceRepository financeRepository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public MerchantBillingService(
            MerchantBillingRepository repository,
            FinanceRepository financeRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.financeRepository = financeRepository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<MerchantBillRecord> listBills() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<MerchantBillRecord> records = scope.isPresent()
                ? repository.findBillsByMerchantId(scope.get())
                : repository.findAllBills();
        auditEventService.recordSuccess(
                "merchant_billing.bill.viewed",
                "merchant.billing.view",
                BUSINESS_TYPE,
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    @Transactional
    public MerchantBillRecord createBill(MerchantBillCreateRequest request) {
        String merchantId = request != null && request.merchantId() != null ? request.merchantId().trim() : "";
        return executeWrite(
                "createMerchantBill:" + merchantId,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantBillRecord.class,
                "merchant_billing.bill.create_failed",
                "merchant.billing.manage",
                "new",
                () -> validateCreateRequest(request),
                () -> createBillOnce(request)
        );
    }

    @Transactional
    public MerchantBillRecord confirmBill(String id, MerchantBillConfirmRequest request) {
        String billId = requireText(id, "id");
        return executeWrite(
                "confirmMerchantBill:" + billId,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantBillRecord.class,
                "merchant_billing.bill.confirm_failed",
                "merchant.billing.confirm",
                billId,
                () -> validateConfirmRequest(request),
                () -> confirmBillOnce(billId, request)
        );
    }

    @Transactional
    public MerchantBillDisputeRecord submitDispute(String id, MerchantBillDisputeRequest request) {
        String billId = requireText(id, "id");
        return executeWrite(
                "submitMerchantBillDispute:" + billId,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantBillDisputeRecord.class,
                "merchant_billing.dispute.submit_failed",
                "merchant.billing.dispute.submit",
                billId,
                () -> validateDisputeRequest(request),
                () -> submitDisputeOnce(billId, request)
        );
    }

    @Transactional
    public MerchantBillDunningRecord recordDunning(String id, MerchantBillDunningRequest request) {
        String billId = requireText(id, "id");
        return executeWrite(
                "recordMerchantBillDunning:" + billId,
                request == null ? null : request.idempotencyKey(),
                request,
                MerchantBillDunningRecord.class,
                "merchant_billing.dunning.record_failed",
                "merchant.billing.dunning.record",
                billId,
                () -> validateDunningRequest(request),
                () -> recordDunningOnce(billId, request)
        );
    }

    private MerchantBillRecord createBillOnce(MerchantBillCreateRequest request) {
        ArApItemRecord item = findArApItem(request.arApItemId());
        validateArApReference(request, item);
        List<MerchantBillLineRecord> lines = buildLines(request);
        Instant now = Instant.now();
        String slug = slug(request.idempotencyKey());
        MerchantBillRecord record = new MerchantBillRecord(
                "mbill_" + slug,
                requireText(request.merchantId(), "merchant_id"),
                trimToNull(request.arApItemId()),
                "MBILL" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                requireDate(request.billPeriodStart(), "bill_period_start"),
                requireDate(request.billPeriodEnd(), "bill_period_end"),
                requireDate(request.dueDate(), "due_date"),
                requirePositive(request.totalAmountCent(), "total_amount_cent"),
                CURRENCY_CNY,
                "issued",
                agingDays(request.dueDate()),
                optionalEnum(request.sourceType(), SOURCE_TYPES, "source_type"),
                trimToNull(request.sourceId()),
                lines,
                now,
                now
        );
        MerchantBillRecord saved = repository.saveBill(record);
        auditEventService.recordSuccess(
                "merchant_billing.bill.created",
                "merchant.billing.manage",
                BUSINESS_TYPE,
                saved.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("status", saved.status(), "ar_ap_item_id", saved.arApItemId())
        );
        return saved;
    }

    private MerchantBillRecord confirmBillOnce(String billId, MerchantBillConfirmRequest request) {
        MerchantBillRecord existing = findBill(billId);
        if (!CONFIRMABLE_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("BILL_NOT_CONFIRMABLE", "merchant bill cannot be confirmed in current status");
        }
        MerchantBillRecord confirmed = repository.saveBill(existing.withStatus("confirmed", Instant.now()));
        auditEventService.recordSuccess(
                "merchant_billing.bill.confirmed",
                "merchant.billing.confirm",
                BUSINESS_TYPE,
                confirmed.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("operator_user_id", requireText(request.operatorUserId(), "operator_user_id"))
        );
        return confirmed;
    }

    private MerchantBillDisputeRecord submitDisputeOnce(String billId, MerchantBillDisputeRequest request) {
        MerchantBillRecord existing = findBill(billId);
        if (!DISPUTABLE_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("BILL_NOT_DISPUTABLE", "merchant bill cannot be disputed in current status");
        }
        Instant now = Instant.now();
        MerchantBillDisputeRecord dispute = repository.saveDispute(new MerchantBillDisputeRecord(
                "mbill_dispute_" + slug(request.idempotencyKey()),
                existing.id(),
                existing.merchantId(),
                requireText(request.reasonCode(), "reason_code"),
                requireText(request.description(), "description"),
                "submitted",
                evidenceIds(request.evidenceAttachmentIds()),
                requireText(request.operatorUserId(), "operator_user_id"),
                now,
                now
        ));
        repository.saveBill(existing.withStatus("disputed", now));
        auditEventService.recordSuccess(
                "merchant_billing.dispute.submitted",
                "merchant.billing.dispute.submit",
                BUSINESS_TYPE,
                existing.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("reason_code", dispute.reasonCode())
        );
        return dispute;
    }

    private MerchantBillDunningRecord recordDunningOnce(String billId, MerchantBillDunningRequest request) {
        MerchantBillRecord existing = findBill(billId);
        if ("closed".equals(existing.status())) {
            throw ApiException.conflict("BILL_CLOSED", "closed merchant bill cannot be dunned");
        }
        MerchantBillDunningRecord record = repository.saveDunningRecord(new MerchantBillDunningRecord(
                "mbill_dunning_" + slug(request.idempotencyKey()),
                existing.id(),
                existing.merchantId(),
                normalizeAllowed(request.dunningChannel(), "dunning_channel", DUNNING_CHANNELS),
                requireText(request.messageTemplate(), "message_template"),
                normalizeAllowed(request.status(), "status", DUNNING_STATUSES),
                request.sentAt() == null ? Instant.now() : request.sentAt(),
                requireText(request.operatorUserId(), "operator_user_id"),
                Instant.now()
        ));
        auditEventService.recordSuccess(
                "merchant_billing.dunning.recorded",
                "merchant.billing.dunning.record",
                BUSINESS_TYPE,
                existing.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("dunning_channel", record.dunningChannel(), "status", record.status())
        );
        return record;
    }

    private void validateCreateRequest(MerchantBillCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.merchantId(), "merchant_id");
        requireText(request.arApItemId(), "ar_ap_item_id");
        LocalDate start = requireDate(request.billPeriodStart(), "bill_period_start");
        LocalDate end = requireDate(request.billPeriodEnd(), "bill_period_end");
        if (end.isBefore(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "bill period end must be on or after start");
        }
        requireDate(request.dueDate(), "due_date");
        requirePositive(request.totalAmountCent(), "total_amount_cent");
        optionalEnum(request.sourceType(), SOURCE_TYPES, "source_type");
        validateLines(request.lines(), request.totalAmountCent());
    }

    private void validateConfirmRequest(MerchantBillConfirmRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateDisputeRequest(MerchantBillDisputeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.reasonCode(), "reason_code");
        requireText(request.description(), "description");
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateDunningRequest(MerchantBillDunningRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.dunningChannel(), "dunning_channel", DUNNING_CHANNELS);
        requireText(request.messageTemplate(), "message_template");
        normalizeAllowed(request.status(), "status", DUNNING_STATUSES);
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateLines(List<MerchantBillLineRequest> lines, Long totalAmountCent) {
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "lines are required");
        }
        long sum = 0L;
        Set<String> seenFeeTypes = new HashSet<>();
        for (MerchantBillLineRequest line : lines) {
            if (line == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "line is required");
            }
            String feeType = normalizeAllowed(line.feeType(), "fee_type", FEE_TYPES);
            if (!seenFeeTypes.add(feeType)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "duplicate fee_type: " + feeType);
            }
            requireText(line.description(), "description");
            sum += requirePositive(line.amountCent(), "line.amount_cent");
        }
        if (totalAmountCent != null && sum != totalAmountCent) {
            throw ApiException.badRequest("VALIDATION_FAILED", "line amount sum must equal total_amount_cent");
        }
    }

    private void validateArApReference(MerchantBillCreateRequest request, ArApItemRecord item) {
        if (!"receivable".equals(item.direction())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant bill must reference receivable ar_ap_item");
        }
        if (!requireText(request.merchantId(), "merchant_id").equals(item.counterpartyId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_id must match ar_ap counterparty_id");
        }
        if (requirePositive(request.totalAmountCent(), "total_amount_cent") != item.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "total_amount_cent must match ar_ap amount_cent");
        }
    }

    private List<MerchantBillLineRecord> buildLines(MerchantBillCreateRequest request) {
        Instant now = Instant.now();
        String billId = "mbill_" + slug(request.idempotencyKey());
        List<MerchantBillLineRequest> lineRequests = request.lines();
        List<MerchantBillLineRecord> result = new ArrayList<>();
        for (int i = 0; i < lineRequests.size(); i++) {
            MerchantBillLineRequest line = lineRequests.get(i);
            result.add(new MerchantBillLineRecord(
                    "mbill_line_" + slug(request.idempotencyKey()) + "_" + i,
                    billId,
                    normalizeAllowed(line.feeType(), "fee_type", FEE_TYPES),
                    requireText(line.description(), "description"),
                    requirePositive(line.amountCent(), "line.amount_cent"),
                    CURRENCY_CNY,
                    now
            ));
        }
        return result;
    }

    private ArApItemRecord findArApItem(String id) {
        return financeRepository.findArApItemById(requireText(id, "ar_ap_item_id"))
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
    }

    private MerchantBillRecord findBill(String id) {
        MerchantBillRecord bill = repository.findBillById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("MERCHANT_BILL_NOT_FOUND", "merchant bill not found"));
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(bill.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "access to another merchant's bill is not allowed");
            }
        });
        return bill;
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<T> action
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(operationId, idempotencyKey, request, action, responseType);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    BUSINESS_TYPE,
                    businessId,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String optionalEnum(String value, Set<String> allowed, String field) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalizeAllowed(normalized, field, allowed);
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }

    private long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be positive");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> evidenceIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                cleaned.add(trimmed);
            }
        }
        return List.copyOf(cleaned);
    }

    private int agingDays(LocalDate dueDate) {
        long days = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        return days > 0 ? Math.toIntExact(Math.min(days, Integer.MAX_VALUE)) : 0;
    }

    private String slug(String value) {
        String text = requireText(value, "idempotency_key").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
        return text.length() <= 48 ? text : text.substring(0, 48);
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
