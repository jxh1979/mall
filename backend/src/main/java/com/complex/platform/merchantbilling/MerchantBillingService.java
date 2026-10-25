package com.complex.platform.merchantbilling;

// 文件版本：V0.01

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.merchantbilling.dto.MerchantBillConfirmRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillCreateRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDisputeRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillDunningRequest;
import com.complex.platform.merchantbilling.dto.MerchantBillLineRequest;
import com.complex.platform.notification.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;
import static com.complex.platform.common.Strings.requireText;

@Service
public class MerchantBillingService {

    private static final Logger log = LoggerFactory.getLogger(MerchantBillingService.class);

    private static final String BUSINESS_TYPE = "merchant_bill";
    private static final String CURRENCY_CNY = "CNY";
    private static final String MERCHANT_FEE_DOMAIN = "merchant_fee";
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
    private final NotificationService notificationService;
    private final PlatformRepository platformRepository;
    private final MerchantFeeNotificationRecipientResolver recipientResolver;

    @Autowired
    public MerchantBillingService(
            MerchantBillingRepository repository,
            FinanceRepository financeRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver recipientResolver
    ) {
        this.repository = repository;
        this.financeRepository = financeRepository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.platformRepository = platformRepository;
        this.recipientResolver = recipientResolver;
    }

    public MerchantBillingService(
            MerchantBillingRepository repository,
            FinanceRepository financeRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            NotificationService notificationService,
            PlatformRepository platformRepository
    ) {
        this(repository, financeRepository, idempotencyService, auditEventService,
                authorizationService, notificationService, platformRepository, null);
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
        String merchantId = requireText(request.merchantId(), "merchant_id");
        // P0 越权修复：createBill 此前不校验调用者商户范围，商户 A 可凭请求体为商户 B 建账单。
        // 平台管理员（scope 为空）可代任意商户建单；商户用户必须等于自身 scope。
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(merchantId)) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "不能为其他商户创建账单");
            }
        });
        ArApItemRecord item = findArApItem(request.arApItemId());
        validateArApReference(request, item);
        List<MerchantBillLineRecord> lines = buildLines(request);
        Instant now = Instant.now();
        // P0 串户修复：主键此前为 mbill_+slug(idempotencyKey) 不含 scope，两商户同幂等键 → 主键碰撞，
        // ON CONFLICT(id) DO UPDATE 静默覆盖前者整行。主键纳入 merchant_id 隔离。
        String slug = slug(merchantId + "_" + request.idempotencyKey());
        MerchantBillRecord record = new MerchantBillRecord(
                "mbill_" + slug,
                merchantId,
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
        notifyMerchantFeeBillAfterCommit(saved);
        return saved;
    }

    private MerchantBillRecord confirmBillOnce(String billId, MerchantBillConfirmRequest request) {
        MerchantBillRecord existing = findBill(billId);
        if (!CONFIRMABLE_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("BILL_NOT_CONFIRMABLE", "商户账单当前状态不可确认");
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
            throw ApiException.conflict("BILL_NOT_DISPUTABLE", "商户账单当前状态不可发起异议");
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
            throw ApiException.conflict("BILL_CLOSED", "已关闭的商户账单不能催缴");
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
        notifyMerchantFeeDunningAfterCommit(existing, record);
        return record;
    }

    private void notifyMerchantFeeBillAfterCommit(MerchantBillRecord bill) {
        if (notificationService == null) {
            return;
        }
        registerNotificationAfterCommit(
                () -> notifyMerchantFeeBillNow(bill),
                "bill_id=" + (bill == null ? null : bill.id()));
    }

    private void notifyMerchantFeeDunningAfterCommit(
            MerchantBillRecord bill,
            MerchantBillDunningRecord dunning
    ) {
        if (notificationService == null) {
            return;
        }
        registerNotificationAfterCommit(
                () -> notifyMerchantFeeDunningNow(bill, dunning),
                "dunning_id=" + (dunning == null ? null : dunning.id()));
    }

    private void registerNotificationAfterCommit(Runnable notification, String description) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()
                    && TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notification.run();
                    }
                });
                return;
            }
            notification.run();
        } catch (RuntimeException exception) {
            log.warn("merchant fee notification registration failed: {}", description, exception);
        }
    }

    private void notifyMerchantFeeBillNow(MerchantBillRecord bill) {
        if (notificationService == null || bill == null) {
            return;
        }
        for (String event : merchantFeeEvents(bill.lines())) {
            notifyMerchantFeeSafely(
                    event,
                    resolveMerchantFeeRecipientPhone(bill.merchantId(), event),
                    "sms.merchant_fee." + event,
                    merchantFeeBillParams(bill, event),
                    "mfbill:" + bill.id() + ":" + event,
                    bill.id());
        }
    }

    private void notifyMerchantFeeDunningNow(MerchantBillRecord bill, MerchantBillDunningRecord dunning) {
        if (notificationService == null || bill == null || dunning == null) {
            return;
        }
        for (String event : merchantFeeEvents(bill.lines())) {
            // OBS-13-04：账单出单不可关闭（BSM 第 1597、1598 行），催缴用独立事件键仍可关闭；收件人仍按费用事件配置。
            notifyMerchantFeeSafely(
                    event + "_dunning",
                    resolveMerchantFeeRecipientPhone(bill.merchantId(), event),
                    "sms.merchant_fee." + event + "_dunning",
                    merchantFeeDunningParams(bill, dunning, event),
                    "mfdun:" + dunning.id() + ":" + event,
                    bill.id());
        }
    }

    private void notifyMerchantFeeSafely(
            String event,
            String recipient,
            String templateCode,
            Map<String, Object> params,
            String idempotencyKey,
            String billId
    ) {
        try {
            notificationService.notify(
                    MERCHANT_FEE_DOMAIN,
                    event,
                    recipient,
                    templateCode,
                    params,
                    idempotencyKey);
        } catch (RuntimeException exception) {
            log.warn(
                    "merchant fee notification failed: event={}, bill_id={}, idempotency_key={}",
                    event,
                    billId,
                    idempotencyKey,
                    exception);
        }
    }

    private Set<String> merchantFeeEvents(List<MerchantBillLineRecord> lines) {
        LinkedHashSet<String> events = new LinkedHashSet<>();
        if (lines == null) {
            return events;
        }
        for (MerchantBillLineRecord line : lines) {
            String event = merchantFeeEvent(line == null ? null : line.feeType());
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private String merchantFeeEvent(String feeType) {
        String normalized = trimToNull(feeType);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "rent" -> "rent";
            case "energy_fee" -> "utilities";
            case "property_fee" -> "property";
            default -> null;
        };
    }

    private Map<String, Object> merchantFeeBillParams(MerchantBillRecord bill, String event) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfNotNull(params, "bill_id", bill.id());
        putIfNotNull(params, "bill_no", bill.billNo());
        putIfNotNull(params, "merchant_id", bill.merchantId());
        putIfNotNull(params, "event", event);
        putIfNotNull(params, "amount_cent", amountCentForEvent(bill, event));
        putIfNotNull(params, "total_amount_cent", bill.totalAmountCent());
        putIfNotNull(params, "currency", bill.currency());
        putIfNotNull(params, "bill_period_start", bill.billPeriodStart());
        putIfNotNull(params, "bill_period_end", bill.billPeriodEnd());
        putIfNotNull(params, "due_date", bill.dueDate());
        return params;
    }

    private Map<String, Object> merchantFeeDunningParams(
            MerchantBillRecord bill,
            MerchantBillDunningRecord dunning,
            String event
    ) {
        Map<String, Object> params = merchantFeeBillParams(bill, event);
        putIfNotNull(params, "dunning_id", dunning.id());
        putIfNotNull(params, "dunning_channel", dunning.dunningChannel());
        putIfNotNull(params, "dunning_status", dunning.status());
        putIfNotNull(params, "message_template", dunning.messageTemplate());
        putIfNotNull(params, "sent_at", dunning.sentAt());
        return params;
    }

    private long amountCentForEvent(MerchantBillRecord bill, String event) {
        if (bill.lines() == null) {
            return 0L;
        }
        long sum = 0L;
        for (MerchantBillLineRecord line : bill.lines()) {
            if (line != null && event.equals(merchantFeeEvent(line.feeType()))) {
                sum += line.amountCent();
            }
        }
        return sum;
    }

    private String resolveMerchantFeeRecipientPhone(String merchantId, String event) {
        if (recipientResolver != null) {
            try {
                String resolved = trimToNull(recipientResolver.resolvePhone(merchantId, event));
                if (resolved != null) {
                    return resolved;
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "failed to resolve merchant fee recipient: merchant_id={}, event={}",
                        merchantId,
                        event,
                        exception);
            }
        }
        return resolveMerchantPrimaryContactPhone(merchantId);
    }

    private String resolveMerchantPrimaryContactPhone(String merchantId) {
        String normalizedMerchantId = trimToNull(merchantId);
        if (normalizedMerchantId == null || platformRepository == null) {
            return null;
        }
        try {
            return platformRepository.listMerchants().stream()
                    .filter(merchant -> normalizedMerchantId.equals(merchant.id()))
                    .findFirst()
                    .map(MerchantRecord::contactPhone)
                    .map(Strings::trimToNull)
                    .orElse(null);
        } catch (RuntimeException exception) {
            log.warn("failed to resolve merchant primary contact phone: merchant_id={}", normalizedMerchantId, exception);
            return null;
        }
    }

    private void putIfNotNull(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    private void validateCreateRequest(MerchantBillCreateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.merchantId(), "merchant_id");
        requireText(request.arApItemId(), "ar_ap_item_id");
        LocalDate start = requireDate(request.billPeriodStart(), "bill_period_start");
        LocalDate end = requireDate(request.billPeriodEnd(), "bill_period_end");
        if (end.isBefore(start)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账单周期的结束日期不能早于开始日期");
        }
        requireDate(request.dueDate(), "due_date");
        requirePositive(request.totalAmountCent(), "total_amount_cent");
        optionalEnum(request.sourceType(), SOURCE_TYPES, "source_type");
        validateLines(request.lines(), request.totalAmountCent());
    }

    private void validateConfirmRequest(MerchantBillConfirmRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateDisputeRequest(MerchantBillDisputeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.reasonCode(), "reason_code");
        requireText(request.description(), "description");
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateDunningRequest(MerchantBillDunningRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.dunningChannel(), "dunning_channel", DUNNING_CHANNELS);
        requireText(request.messageTemplate(), "message_template");
        normalizeAllowed(request.status(), "status", DUNNING_STATUSES);
        requireText(request.operatorUserId(), "operator_user_id");
    }

    private void validateLines(List<MerchantBillLineRequest> lines, Long totalAmountCent) {
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账单明细不能为空");
        }
        long sum = 0L;
        Set<String> seenFeeTypes = new HashSet<>();
        for (MerchantBillLineRequest line : lines) {
            if (line == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "line is required");
            }
            String feeType = normalizeAllowed(line.feeType(), "fee_type", FEE_TYPES);
            if (!seenFeeTypes.add(feeType)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "费用类型重复：" + feeType);
            }
            requireText(line.description(), "description");
            sum += requirePositive(line.amountCent(), "line.amount_cent");
        }
        if (totalAmountCent != null && sum != totalAmountCent) {
            throw ApiException.badRequest("VALIDATION_FAILED", "明细金额合计必须等于账单总金额");
        }
    }

    private void validateArApReference(MerchantBillCreateRequest request, ArApItemRecord item) {
        if (!"receivable".equals(item.direction())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "商户账单必须关联应收款项（ar_ap_item）");
        }
        if (!requireText(request.merchantId(), "merchant_id").equals(item.counterpartyId())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账单商户必须与应收应付往来方一致");
        }
        if (requirePositive(request.totalAmountCent(), "total_amount_cent") != item.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账单总金额必须与应收应付金额一致");
        }
    }

    private List<MerchantBillLineRecord> buildLines(MerchantBillCreateRequest request) {
        Instant now = Instant.now();
        // P0 串户修复：与账单主键口径一致，slug 纳入 merchant_id（否则 bill_id 外键悬空 / 跨租户行撞键）。
        String scopedSlug = slug(requireText(request.merchantId(), "merchant_id") + "_" + request.idempotencyKey());
        String billId = "mbill_" + scopedSlug;
        List<MerchantBillLineRequest> lineRequests = request.lines();
        List<MerchantBillLineRecord> result = new ArrayList<>();
        for (int i = 0; i < lineRequests.size(); i++) {
            MerchantBillLineRequest line = lineRequests.get(i);
            result.add(new MerchantBillLineRecord(
                    "mbill_line_" + scopedSlug + "_" + i,
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
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "应收应付款项不存在或已被删除"));
    }

    private MerchantBillRecord findBill(String id) {
        MerchantBillRecord bill = repository.findBillById(requireText(id, "id"))
                .orElseThrow(() -> ApiException.notFound("MERCHANT_BILL_NOT_FOUND", "商户账单不存在或已被删除"));
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(bill.merchantId())) {
                throw ApiException.forbidden(
                        "CROSS_MERCHANT_FORBIDDEN", "无权查看其他商户的账单");
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

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }

    private long requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于 0");
        }
        return value;
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
