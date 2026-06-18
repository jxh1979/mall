package com.complex.platform.compliance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {

    private static final String CONTRACT_DATE_TYPE = "contract_key_date";
    private static final String EXPIRY_ITEM_TYPE = "compliance_expiry_item";
    private static final Set<String> CONTRACT_DATE_TYPES = Set.of(
            "contract_start", "contract_end", "renewal_notice", "deposit_return", "rent_free_end", "custom"
    );
    private static final Set<String> EXPIRY_SUBJECT_TYPES = Set.of("merchant", "contract", "supplier", "asset", "amusement_device");
    private static final Set<String> EXPIRY_ITEM_TYPES = Set.of(
            "business_license", "food_license", "alcohol_license", "fire_certificate", "special_equipment",
            "insurance", "supplier_qualification", "inspection_certificate", "custom"
    );
    private static final Set<String> STATUSES = Set.of("active", "completed", "waived", "archived");
    private static final Set<String> SOURCE_TYPES = Set.of("manual", "ai_suggestion", "imported");
    private static final Set<String> EXPIRY_ACTIONS = Set.of("remind_only", "block_operation", "supervisor_approval");

    private final ComplianceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public ComplianceService(
            ComplianceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<ContractKeyDateRecord> listContractKeyDates() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<ContractKeyDateRecord> records = scope.isPresent()
                ? repository.findContractKeyDatesByMerchant(scope.get())
                : repository.findAllContractKeyDates();
        auditEventService.recordSuccess(
                "compliance.contract_key_date.viewed",
                "compliance.contract_key_date.view",
                CONTRACT_DATE_TYPE,
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public ContractKeyDateRecord createContractKeyDate(ContractKeyDateRequest request) {
        return executeWrite(
                "createContractKeyDate",
                request == null ? null : request.idempotencyKey(),
                request,
                "compliance.contract_key_date.create_failed",
                "compliance.contract_key_date.manage",
                "new",
                () -> validateContractKeyDateRequest(request),
                () -> createContractKeyDateOnce(request),
                ContractKeyDateRecord.class
        );
    }

    public PageResponse<ComplianceExpiryItemRecord> listExpiryItems() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<ComplianceExpiryItemRecord> records = scope.isPresent()
                ? repository.findExpiryItemsByMerchant(scope.get())
                : repository.findAllExpiryItems();
        auditEventService.recordSuccess(
                "compliance.expiry_item.viewed",
                "compliance.expiry_item.view",
                EXPIRY_ITEM_TYPE,
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public ComplianceExpiryItemRecord createExpiryItem(ComplianceExpiryItemRequest request) {
        return executeWrite(
                "createComplianceExpiryItem",
                request == null ? null : request.idempotencyKey(),
                request,
                "compliance.expiry_item.create_failed",
                "compliance.expiry_item.manage",
                "new",
                () -> validateExpiryItemRequest(request),
                () -> createExpiryItemOnce(request),
                ComplianceExpiryItemRecord.class
        );
    }

    public PageResponse<ComplianceReminderRecord> listReminders() {
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<ComplianceReminderRecord> records = scope.isPresent()
                ? repository.findRemindersByMerchant(scope.get())
                : repository.findAllReminders();
        auditEventService.recordSuccess(
                "compliance.reminder.viewed",
                "compliance.reminder.view",
                "compliance_reminder",
                "all",
                requestId(),
                null,
                Map.of("count", records.size())
        );
        return new PageResponse<>(records, 1, 50, records.size());
    }

    public ComplianceReminderScanResult scanReminders(ComplianceReminderScanRequest request) {
        return executeWrite(
                "scanComplianceReminders",
                request == null ? null : request.idempotencyKey(),
                request,
                "compliance.reminder.scan_failed",
                "compliance.reminder.scan",
                "all",
                () -> validateScanRequest(request),
                () -> scanRemindersOnce(request),
                ComplianceReminderScanResult.class
        );
    }

    private ContractKeyDateRecord createContractKeyDateOnce(ContractKeyDateRequest request) {
        Instant now = Instant.now();
        ContractKeyDateRecord record = repository.saveContractKeyDate(new ContractKeyDateRecord(
                "ckd_" + UUID.randomUUID(),
                requireText(request.contractId(), "contract_id"),
                resolveMerchantScopedId(request.merchantId()),
                normalizeAllowed(request.dateType(), "date_type", CONTRACT_DATE_TYPES),
                requireText(request.title(), "title"),
                requireDate(request.dueDate(), "due_date"),
                reminderDays(request.reminderDays()),
                normalizeAllowed(defaultText(request.status(), "active"), "status", STATUSES),
                requireText(request.ownerUserId(), "owner_user_id"),
                normalizeAllowed(defaultText(request.sourceType(), "manual"), "source_type", SOURCE_TYPES),
                request.humanConfirmed(),
                evidenceIds(request.evidenceAttachmentIds()),
                now,
                now
        ));
        auditEventService.recordSuccess(
                "compliance.contract_key_date.created",
                "compliance.contract_key_date.manage",
                CONTRACT_DATE_TYPE,
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("date_type", record.dateType(), "due_date", record.dueDate().toString())
        );
        return record;
    }

    private ComplianceExpiryItemRecord createExpiryItemOnce(ComplianceExpiryItemRequest request) {
        Instant now = Instant.now();
        ComplianceExpiryItemRecord record = repository.saveExpiryItem(new ComplianceExpiryItemRecord(
                "cei_" + UUID.randomUUID(),
                normalizeAllowed(request.subjectType(), "subject_type", EXPIRY_SUBJECT_TYPES),
                requireText(request.subjectId(), "subject_id"),
                resolveMerchantScopedId(request.merchantId()),
                normalizeAllowed(request.itemType(), "item_type", EXPIRY_ITEM_TYPES),
                requireText(request.itemName(), "item_name"),
                trimToNull(request.documentNo()),
                trimToNull(request.issuingAuthority()),
                request.validFrom(),
                requireDate(request.validTo(), "valid_to"),
                reminderDays(request.reminderDays()),
                normalizeAllowed(defaultText(request.expiryAction(), "remind_only"), "expiry_action", EXPIRY_ACTIONS),
                normalizeAllowed(defaultText(request.status(), "active"), "status", STATUSES),
                requireText(request.ownerUserId(), "owner_user_id"),
                normalizeAllowed(defaultText(request.sourceType(), "manual"), "source_type", SOURCE_TYPES),
                request.humanConfirmed(),
                evidenceIds(request.evidenceAttachmentIds()),
                now,
                now
        ));
        auditEventService.recordSuccess(
                "compliance.expiry_item.created",
                "compliance.expiry_item.manage",
                EXPIRY_ITEM_TYPE,
                record.id(),
                requestId(),
                request.idempotencyKey(),
                Map.of("item_type", record.itemType(), "valid_to", record.validTo().toString())
        );
        return record;
    }

    private ComplianceReminderScanResult scanRemindersOnce(ComplianceReminderScanRequest request) {
        LocalDate scanDate = requireDate(request.scanDate(), "scan_date");
        int defaultLookaheadDays = request.defaultLookaheadDays() == null ? 30 : request.defaultLookaheadDays();
        if (defaultLookaheadDays < 0 || defaultLookaheadDays > 366) {
            throw ApiException.badRequest("VALIDATION_FAILED", "default_lookahead_days must be between 0 and 366");
        }
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<ContractKeyDateRecord> dates = scope.isPresent()
                ? repository.findContractKeyDatesByMerchant(scope.get())
                : repository.findAllContractKeyDates();
        List<ComplianceExpiryItemRecord> items = scope.isPresent()
                ? repository.findExpiryItemsByMerchant(scope.get())
                : repository.findAllExpiryItems();
        List<ComplianceReminderRecord> generated = java.util.stream.Stream.concat(
                        dates.stream()
                                .filter(record -> "active".equals(record.status()))
                                .flatMap(record -> reminderForContractDate(record, scanDate, defaultLookaheadDays).stream()),
                        items.stream()
                                .filter(record -> "active".equals(record.status()))
                                .flatMap(record -> reminderForExpiryItem(record, scanDate, defaultLookaheadDays).stream())
                )
                .map(repository::saveReminder)
                .toList();
        auditEventService.recordSuccess(
                "compliance.reminder.scan_completed",
                "compliance.reminder.scan",
                "compliance_reminder",
                "all",
                requestId(),
                request.idempotencyKey(),
                Map.of("generated_count", generated.size())
        );
        return new ComplianceReminderScanResult(generated.size(), generated);
    }

    private List<ComplianceReminderRecord> reminderForContractDate(
            ContractKeyDateRecord record,
            LocalDate scanDate,
            int defaultLookaheadDays
    ) {
        long days = ChronoUnit.DAYS.between(scanDate, record.dueDate());
        if (days < 0) {
            boolean blocking = "contract_end".equals(record.dateType());
            return List.of(reminder(
                    record.merchantId(),
                    CONTRACT_DATE_TYPE,
                    record.id(),
                    "expired",
                    record.dueDate(),
                    scanDate,
                    blocking ? "high" : "medium",
                    blocking,
                    record.title() + " is overdue"
            ));
        }
        if (days <= lookaheadDays(record.reminderDays(), defaultLookaheadDays)) {
            return List.of(reminder(
                    record.merchantId(),
                    CONTRACT_DATE_TYPE,
                    record.id(),
                    "upcoming",
                    record.dueDate(),
                    scanDate,
                    "medium",
                    false,
                    record.title() + " is upcoming"
            ));
        }
        return List.of();
    }

    private List<ComplianceReminderRecord> reminderForExpiryItem(
            ComplianceExpiryItemRecord record,
            LocalDate scanDate,
            int defaultLookaheadDays
    ) {
        long days = ChronoUnit.DAYS.between(scanDate, record.validTo());
        boolean blocking = !"remind_only".equals(record.expiryAction());
        if (days < 0) {
            return List.of(reminder(
                    record.merchantId(),
                    EXPIRY_ITEM_TYPE,
                    record.id(),
                    "expired",
                    record.validTo(),
                    scanDate,
                    blocking ? "critical" : "high",
                    blocking,
                    record.itemName() + " is expired"
            ));
        }
        if (days <= lookaheadDays(record.reminderDays(), defaultLookaheadDays)) {
            return List.of(reminder(
                    record.merchantId(),
                    EXPIRY_ITEM_TYPE,
                    record.id(),
                    "upcoming",
                    record.validTo(),
                    scanDate,
                    blocking ? "high" : "medium",
                    blocking,
                    record.itemName() + " is upcoming"
            ));
        }
        return List.of();
    }

    private ComplianceReminderRecord reminder(
            String merchantId,
            String sourceType,
            String sourceId,
            String reminderType,
            LocalDate dueDate,
            LocalDate reminderAt,
            String severity,
            boolean blockingSuggested,
            String message
    ) {
        return new ComplianceReminderRecord(
                "cr_" + UUID.randomUUID(),
                merchantId,
                sourceType,
                sourceId,
                reminderType,
                dueDate,
                reminderAt,
                severity,
                "open",
                blockingSuggested,
                message,
                Instant.now()
        );
    }

    private void validateContractKeyDateRequest(ContractKeyDateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.contractId(), "contract_id");
        requireText(request.merchantId(), "merchant_id");
        normalizeAllowed(request.dateType(), "date_type", CONTRACT_DATE_TYPES);
        requireText(request.title(), "title");
        requireDate(request.dueDate(), "due_date");
        requireText(request.ownerUserId(), "owner_user_id");
        normalizeAllowed(defaultText(request.status(), "active"), "status", STATUSES);
        normalizeAllowed(defaultText(request.sourceType(), "manual"), "source_type", SOURCE_TYPES);
        requireHumanConfirmed(request.humanConfirmed());
        reminderDays(request.reminderDays());
    }

    private void validateExpiryItemRequest(ComplianceExpiryItemRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.subjectType(), "subject_type", EXPIRY_SUBJECT_TYPES);
        requireText(request.subjectId(), "subject_id");
        requireText(request.merchantId(), "merchant_id");
        normalizeAllowed(request.itemType(), "item_type", EXPIRY_ITEM_TYPES);
        requireText(request.itemName(), "item_name");
        requireDate(request.validTo(), "valid_to");
        if (request.validFrom() != null && request.validFrom().isAfter(request.validTo())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "valid_from cannot be after valid_to");
        }
        normalizeAllowed(defaultText(request.expiryAction(), "remind_only"), "expiry_action", EXPIRY_ACTIONS);
        normalizeAllowed(defaultText(request.status(), "active"), "status", STATUSES);
        requireText(request.ownerUserId(), "owner_user_id");
        normalizeAllowed(defaultText(request.sourceType(), "manual"), "source_type", SOURCE_TYPES);
        requireHumanConfirmed(request.humanConfirmed());
        reminderDays(request.reminderDays());
    }

    private void validateScanRequest(ComplianceReminderScanRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireDate(request.scanDate(), "scan_date");
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String failureEvent,
            String permissionCode,
            String businessId,
            Runnable preValidation,
            Supplier<T> action,
            Class<T> responseType
    ) {
        try {
            preValidation.run();
            return idempotencyService.execute(operationId, idempotencyKey, request, action, responseType);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    "compliance",
                    businessId,
                    requestId(),
                    idempotencyKey,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private void requireHumanConfirmed(boolean humanConfirmed) {
        if (!humanConfirmed) {
            throw ApiException.badRequest("VALIDATION_FAILED", "human_confirmed is required before activation");
        }
    }

    private int lookaheadDays(List<Integer> reminderDays, int fallback) {
        return reminderDays == null || reminderDays.isEmpty()
                ? fallback
                : reminderDays.stream().mapToInt(Integer::intValue).max().orElse(fallback);
    }

    private List<Integer> reminderDays(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return List.of(30, 7);
        }
        return values.stream()
                .filter(value -> value != null && value >= 0 && value <= 366)
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
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

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return trimmed;
    }

    private String defaultText(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    /**
     * Merchant ownership must come from the caller's security scope, never blindly from the request body.
     * For a merchant-scoped caller the body merchant_id (if present) must match their own scope; the stored
     * value is always the caller's scope. For an unscoped caller (e.g. platform admin) the body value is required.
     */
    private String resolveMerchantScopedId(String requestedMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent()) {
            String requested = trimToNull(requestedMerchantId);
            if (requested != null && !scope.get().equals(requested)) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "cannot create compliance records for another merchant");
            }
            return scope.get();
        }
        return requireText(requestedMerchantId, "merchant_id");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
