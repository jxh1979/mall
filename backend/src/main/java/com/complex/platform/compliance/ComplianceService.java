package com.complex.platform.compliance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.compliance.dto.ComplianceGraceRequests.GraceRequest;
import com.complex.platform.compliance.dto.ComplianceGraceRequests.ItemTypeConfigRequest;
import com.complex.platform.compliance.dto.ComplianceGraceRequests.RenewalRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class ComplianceService {

    private static final String CONTRACT_DATE_TYPE = "contract_key_date";
    private static final String EXPIRY_ITEM_TYPE = "compliance_expiry_item";
    private static final Set<String> CONTRACT_DATE_TYPES = Set.of(
            "contract_start", "contract_end", "renewal_notice", "deposit_return", "rent_free_end", "custom"
    );
    private static final Set<String> EXPIRY_SUBJECT_TYPES = Set.of("merchant", "contract", "supplier", "asset", "amusement_device");
    private static final List<String> EXPIRY_ITEM_TYPE_ORDER = List.of(
            "business_license", "food_license", "alcohol_license", "fire_certificate", "special_equipment",
            "insurance", "supplier_qualification", "inspection_certificate", "custom"
    );
    private static final Set<String> EXPIRY_ITEM_TYPES = Set.copyOf(EXPIRY_ITEM_TYPE_ORDER);
    private static final Set<String> STATUSES = Set.of("active", "completed", "waived", "archived");
    private static final Set<String> SOURCE_TYPES = Set.of("manual", "ai_suggestion", "imported");
    private static final Set<String> EXPIRY_ACTIONS = Set.of("remind_only", "block_operation", "supervisor_approval");
    /** 第十八批 E 段第 22 条：证照宽限期申请走统一审批的最高权限策略（SupervisorReviewService 策略 compliance_grace_period）。 */
    public static final String GRACE_ACTION = "compliance.expiry_item.grace";
    /** PGM:531「出厂默认提前三个月」：该证照类型预警提前量未配置时按此月数。 */
    static final int FACTORY_WARNING_MONTHS = 3;

    private final ComplianceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final SupervisorReviewService reviews;
    private final SupervisorReviewRepository reviewRepository;
    private final ObjectMapper json;

    public ComplianceService(
            ComplianceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            SupervisorReviewService reviews,
            SupervisorReviewRepository reviewRepository,
            ObjectMapper json
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.reviews = reviews;
        this.reviewRepository = reviewRepository;
        this.json = json;
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

    /** 第十八批 E 段第 22 条（PGM:531）：九类证照各自的宽限与预警配置；预警未配置按出厂 3 个月（上下限留空，首次调整一并填写）。 */
    public List<ComplianceItemTypeConfigRecord> listItemTypeConfigs() {
        Map<String, ComplianceItemTypeConfigRecord> configured = new HashMap<>();
        repository.findItemTypeConfigs().forEach(config -> configured.put(config.itemType(), config));
        return EXPIRY_ITEM_TYPE_ORDER.stream().map(type -> {
            ComplianceItemTypeConfigRecord config = configured.getOrDefault(type,
                    new ComplianceItemTypeConfigRecord(type, null, null, null, null, null, null));
            return config.warningMonths() != null ? config : new ComplianceItemTypeConfigRecord(type, config.graceDays(),
                    config.graceMinDays(), config.graceMaxDays(), FACTORY_WARNING_MONTHS, null, null);
        }).toList();
    }

    /**
     * 第十八批 E 段第 22 条：按证照类型配宽限天数与预警提前月数（PGM:531「按证照类型可配……须同时配置上下限」）。全平台统一，
     * 平台范围闸在幂等之前（商户账号可持 manage 为本商户建证照，但不能改全平台配置）；保险不适用宽限（宽限句只写「证照或资质」）。
     */
    public ComplianceItemTypeConfigRecord saveItemTypeConfig(String itemType, ItemTypeConfigRequest request) {
        return executeWrite(
                "saveComplianceItemTypeConfig:" + itemType,
                request == null ? null : request.idempotencyKey(),
                request,
                "compliance.item_type_config.save_failed",
                "compliance.expiry_item.manage",
                itemType == null ? "unknown" : itemType,
                () -> {
                    authorizationService.requirePlatformScope();
                    validateItemTypeConfig(itemType, request);
                },
                () -> {
                    ComplianceItemTypeConfigRecord record = new ComplianceItemTypeConfigRecord(itemType, request.graceDays(),
                            request.graceMinDays(), request.graceMaxDays(), request.warningMonths(), request.warningMinMonths(),
                            request.warningMaxMonths());
                    repository.saveItemTypeConfig(record, authorizationService.currentActor().id());
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("grace_days", text(record.graceDays()));
                    metadata.put("grace_min_days", text(record.graceMinDays()));
                    metadata.put("grace_max_days", text(record.graceMaxDays()));
                    metadata.put("warning_months", text(record.warningMonths()));
                    metadata.put("warning_min_months", text(record.warningMinMonths()));
                    metadata.put("warning_max_months", text(record.warningMaxMonths()));
                    auditEventService.recordSuccess("compliance.item_type_config.saved", "compliance.expiry_item.manage",
                            "compliance_item_type_config", itemType, requestId(), request.idempotencyKey(), metadata);
                    return record;
                },
                ComplianceItemTypeConfigRecord.class
        );
    }

    /**
     * 第十八批 E 段第 22 条：商户对本商户已到期的证照或资质申请宽限期（PGM:531「证照或资质到期后，可申请一个后台可配时长的宽限期……
     * 须经最高权限审批后生效……超过宽限期仍未更新的一律阻断，不得再次延长」）。范围闸在幂等之前、fail-closed；宽限截止日申请时固化
     * ＝有效期止＋该类型宽限天数；提交即进最高权限审批，批准才生效（{@link #graceDecided}）。
     */
    public ComplianceExpiryItemRecord requestGrace(String itemId, GraceRequest request) {
        return executeWrite(
                "requestComplianceGrace",
                request == null ? null : request.idempotencyKey(),
                request,
                "compliance.grace.submit_failed",
                "compliance.grace_period.request",
                itemId == null ? "unknown" : itemId,
                () -> {
                    String merchantId = authorizationService.currentMerchantScope()
                            .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
                    requireScopedItem(itemId, Optional.of(merchantId));
                    if (request == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
                    }
                    requireText(request.idempotencyKey(), "idempotency_key");
                    requireText(request.reason(), "reason");
                },
                () -> requestGraceOnce(itemId, request),
                ComplianceExpiryItemRecord.class
        );
    }

    /**
     * 第十八批 E 段第 22 条 最高权限审批结果回写（照 SettlementConfigWriteService.highValueThresholdDecided：核对象、动作、持久化状态，
     * 再按条目上的在审审批单号回写）。续期登记会清空审批单号，此后回来的旧审批结果不回写。
     */
    @EventListener
    @Transactional
    public void graceDecided(SupervisorReviewDecisionRecord decision) {
        if (!EXPIRY_ITEM_TYPE.equals(decision.objectType()) || !GRACE_ACTION.equals(decision.actionType())
                || !List.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        var persisted = reviewRepository.findSupervisorReviewById(decision.reviewId()).orElse(null);
        if (persisted == null || !Objects.equals(persisted.objectId(), decision.objectId())
                || !Objects.equals(persisted.reviewStatus(), decision.reviewStatus())) {
            return;
        }
        if (repository.decideGrace(decision.objectId(), decision.reviewId(), decision.reviewStatus()) == 1) {
            auditEventService.recordSuccess("compliance.grace.decided", "governance.supervisor_review.approve", EXPIRY_ITEM_TYPE,
                    decision.objectId(), requestId(), null, Map.of("review_id", decision.reviewId(), "grace_status", decision.reviewStatus()));
        }
    }

    /**
     * 第十八批 E 段第 22 条：证照续期登记（PGM:531「超过宽限期仍未更新的一律阻断」——更新后不再命中到期阻断）。原条目就地更新有效期，
     * 宽限申请随这次到期一并清空，新到期可重新申请。范围与新建一致（商户账号只能更新本商户条目），闸在幂等之前。
     */
    public ComplianceExpiryItemRecord renewExpiryItem(String itemId, RenewalRequest request) {
        return executeWrite(
                "renewComplianceExpiryItem",
                request == null ? null : request.idempotencyKey(),
                request,
                "compliance.expiry_item.renew_failed",
                "compliance.expiry_item.manage",
                itemId == null ? "unknown" : itemId,
                () -> {
                    requireScopedItem(itemId, authorizationService.currentMerchantScope());
                    if (request == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
                    }
                    requireText(request.idempotencyKey(), "idempotency_key");
                    requireDate(request.validTo(), "valid_to");
                },
                () -> renewExpiryItemOnce(itemId, request),
                ComplianceExpiryItemRecord.class
        );
    }

    /**
     * 第十八批 E 段第 22 条 每日自动扫描（{@link ComplianceReminderScanJob}）：全平台合同关键日期与证照一轮，无登录主体、不走权限与幂等，
     * 与 HTTP 扫描同一套生成逻辑并按去重键 upsert；合同关键日期沿用 HTTP 扫描缺省的提前 30 天。
     */
    public int scanAllOn(LocalDate scanDate) {
        List<ComplianceReminderRecord> generated = generateReminders(
                repository.findAllContractKeyDates(), repository.findAllExpiryItems(), scanDate, 30);
        auditEventService.recordSuccess("compliance.reminder.scan_completed", "not_applicable", "compliance_reminder", "all",
                requestId(), null, Map.of("generated_count", generated.size(), "scan_date", scanDate.toString(), "trigger", "scheduled"));
        return generated.size();
    }

    private ComplianceExpiryItemRecord requestGraceOnce(String itemId, GraceRequest request) {
        ComplianceExpiryItemRecord item = requireScopedItem(itemId, Optional.empty());
        LocalDate today = LocalDate.now(LocalZone.ID);
        if (!"active".equals(item.status()) || !item.validTo().isBefore(today)) {
            throw ApiException.conflict("GRACE_NOT_EXPIRED", "证照尚未到期，不能申请宽限期");
        }
        Integer graceDays = repository.findItemTypeConfigs().stream()
                .filter(config -> config.itemType().equals(item.itemType()))
                .map(ComplianceItemTypeConfigRecord::graceDays)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("GRACE_PERIOD_NOT_CONFIGURED", "该证照类型未配置宽限期，不能申请"));
        if ("approved".equals(item.graceStatus())) {
            throw ApiException.conflict("GRACE_ALREADY_USED", "本次到期已批准过宽限期，不得再次延长");
        }
        if ("pending_review".equals(item.graceStatus())) {
            throw ApiException.conflict("GRACE_REQUEST_PENDING", "宽限期申请正在最高权限审批中");
        }
        LocalDate graceUntil = item.validTo().plusDays(graceDays);
        if (today.isAfter(graceUntil)) {
            throw ApiException.conflict("GRACE_WINDOW_PASSED", "已超过宽限期，不得再次延长");
        }
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("item_name", item.itemName());
        facts.put("item_type", item.itemType());
        facts.put("document_no", item.documentNo() == null ? "" : item.documentNo());
        facts.put("valid_to", item.validTo().toString());
        facts.put("grace_days", graceDays);
        facts.put("grace_until", graceUntil.toString());
        facts.put("merchant_id", item.merchantId());
        String snapshot = request.reason().trim() + "\n" + encode(facts);
        SupervisorReviewDecisionRecord decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(
                request.idempotencyKey() + ":review", snapshot, List.of(), GRACE_ACTION, EXPIRY_ITEM_TYPE, itemId, 0L, snapshot,
                null, List.of()));
        if (!"pending_review".equals(decision.reviewStatus()) || !"top_authority".equals(decision.approvalLevel())) {
            throw ApiException.conflict("GRACE_APPROVAL_REQUIRED", "证照宽限期必须进入最高权限审批");
        }
        repository.markGraceRequested(itemId, graceUntil, decision.reviewId());
        auditEventService.recordSuccess("compliance.grace.submitted", "compliance.grace_period.request", EXPIRY_ITEM_TYPE, itemId,
                requestId(), request.idempotencyKey(), Map.of("review_id", decision.reviewId(), "valid_to", item.validTo().toString(),
                        "grace_until", graceUntil.toString()));
        return requireScopedItem(itemId, Optional.empty());
    }

    private ComplianceExpiryItemRecord renewExpiryItemOnce(String itemId, RenewalRequest request) {
        ComplianceExpiryItemRecord item = requireScopedItem(itemId, Optional.empty());
        if (!request.validTo().isAfter(item.validTo())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "新的有效期止必须晚于原有效期止");
        }
        LocalDate validFrom = request.validFrom() != null ? request.validFrom() : item.validFrom();
        if (validFrom != null && validFrom.isAfter(request.validTo())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效开始日期不能晚于结束日期");
        }
        String documentNo = trimToNull(request.documentNo()) != null ? trimToNull(request.documentNo()) : item.documentNo();
        List<String> evidence = evidenceIds(request.evidenceAttachmentIds());
        repository.renewExpiryItem(itemId, validFrom, request.validTo(), documentNo,
                evidence.isEmpty() ? item.evidenceAttachmentIds() : evidence);
        auditEventService.recordSuccess("compliance.expiry_item.renewed", "compliance.expiry_item.manage", EXPIRY_ITEM_TYPE, itemId,
                requestId(), request.idempotencyKey(), Map.of("previous_valid_to", item.validTo().toString(),
                        "valid_to", request.validTo().toString(), "previous_grace_status", text(item.graceStatus())));
        return requireScopedItem(itemId, Optional.empty());
    }

    /** 范围闸（fail-closed）：条目须存在；给了商户范围时只能是本商户的条目。事务内调用即锁住该条目。 */
    private ComplianceExpiryItemRecord requireScopedItem(String itemId, Optional<String> merchantScope) {
        ComplianceExpiryItemRecord item = repository.findExpiryItemForUpdate(requireText(itemId, "id"))
                .orElseThrow(() -> ApiException.notFound("COMPLIANCE_EXPIRY_ITEM_NOT_FOUND", "证照条目不存在"));
        if (merchantScope.isPresent() && !merchantScope.get().equals(item.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不能处理其他商户的证照");
        }
        return item;
    }

    private void validateItemTypeConfig(String itemType, ItemTypeConfigRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        if (itemType == null || !EXPIRY_ITEM_TYPES.contains(itemType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "证照类型不存在");
        }
        requireBounds(request.graceDays(), request.graceMinDays(), request.graceMaxDays(), "宽限天数");
        requireBounds(request.warningMonths(), request.warningMinMonths(), request.warningMaxMonths(), "预警提前月数");
        if ("insurance".equals(itemType) && request.graceDays() != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "保险不适用宽限期，宽限期只对证照或资质");
        }
    }

    /** 值与上下限同空或同填，同填时 0 ≤ 下限 ≤ 值 ≤ 上限（与 V568 库约束一致）。 */
    private static void requireBounds(Integer value, Integer min, Integer max, String name) {
        boolean allEmpty = value == null && min == null && max == null;
        boolean allSet = value != null && min != null && max != null;
        if (!allEmpty && !(allSet && 0 <= min && min <= value && value <= max)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    name + "须与其下限、上限同时填写或同时留空，且 0 ≤ 下限 ≤ " + name + " ≤ 上限");
        }
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException(error);
        }
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
                now,
                null,
                null
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
            throw ApiException.badRequest("VALIDATION_FAILED", "默认提前提醒天数必须在 0 到 366 之间");
        }
        Optional<String> scope = authorizationService.currentMerchantScope();
        List<ComplianceReminderRecord> generated = generateReminders(
                scope.isPresent() ? repository.findContractKeyDatesByMerchant(scope.get()) : repository.findAllContractKeyDates(),
                scope.isPresent() ? repository.findExpiryItemsByMerchant(scope.get()) : repository.findAllExpiryItems(),
                scanDate,
                defaultLookaheadDays);
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

    /** HTTP 扫描与每日任务共用：生效中的合同关键日期与证照逐条生成提醒并按去重键 upsert。 */
    private List<ComplianceReminderRecord> generateReminders(
            List<ContractKeyDateRecord> dates,
            List<ComplianceExpiryItemRecord> items,
            LocalDate scanDate,
            int defaultLookaheadDays
    ) {
        Map<String, Integer> warningMonths = new HashMap<>();
        repository.findItemTypeConfigs().stream()
                .filter(config -> config.warningMonths() != null)
                .forEach(config -> warningMonths.put(config.itemType(), config.warningMonths()));
        return java.util.stream.Stream.concat(
                        dates.stream()
                                .filter(record -> "active".equals(record.status()))
                                .flatMap(record -> reminderForContractDate(record, scanDate, defaultLookaheadDays).stream()),
                        items.stream()
                                .filter(record -> "active".equals(record.status()))
                                .flatMap(record -> reminderForExpiryItem(record, scanDate,
                                        warningMonths.getOrDefault(record.itemType(), FACTORY_WARNING_MONTHS)).stream())
                )
                .map(repository::saveReminder)
                .toList();
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

    /**
     * 证照条目：过了有效期止生成 expired；第十八批 E 段第 22 条（PGM:531）按该证照类型的预警提前月数（未配置按出厂 3 个月）生成 upcoming，
     * 不再用条目上的 reminder_days 和扫描请求的 default_lookahead_days；宽限期已批准且扫描日在宽限截止日（含）以内的，每日另生成一条 grace。
     */
    private List<ComplianceReminderRecord> reminderForExpiryItem(
            ComplianceExpiryItemRecord record,
            LocalDate scanDate,
            int warningMonths
    ) {
        boolean blocking = !"remind_only".equals(record.expiryAction());
        if (record.validTo().isBefore(scanDate)) {
            ComplianceReminderRecord expired = reminder(
                    record.merchantId(),
                    EXPIRY_ITEM_TYPE,
                    record.id(),
                    "expired",
                    record.validTo(),
                    scanDate,
                    blocking ? "critical" : "high",
                    blocking,
                    record.itemName() + "已于" + record.validTo() + "到期"
            );
            if ("approved".equals(record.graceStatus()) && record.graceUntil() != null && !scanDate.isAfter(record.graceUntil())) {
                return List.of(expired, reminder(
                        record.merchantId(),
                        EXPIRY_ITEM_TYPE,
                        record.id(),
                        "grace",
                        scanDate,
                        scanDate,
                        "high",
                        false,
                        record.itemName() + "宽限期至" + record.graceUntil() + "，到期仍未更新将阻断"
                ));
            }
            return List.of(expired);
        }
        if (!scanDate.isBefore(record.validTo().minusMonths(warningMonths))) {
            return List.of(reminder(
                    record.merchantId(),
                    EXPIRY_ITEM_TYPE,
                    record.id(),
                    "upcoming",
                    record.validTo(),
                    scanDate,
                    blocking ? "high" : "medium",
                    blocking,
                    record.itemName() + "将于" + record.validTo() + "到期"
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        normalizeAllowed(request.subjectType(), "subject_type", EXPIRY_SUBJECT_TYPES);
        requireText(request.subjectId(), "subject_id");
        requireText(request.merchantId(), "merchant_id");
        normalizeAllowed(request.itemType(), "item_type", EXPIRY_ITEM_TYPES);
        requireText(request.itemName(), "item_name");
        requireDate(request.validTo(), "valid_to");
        if (request.validFrom() != null && request.validFrom().isAfter(request.validTo())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效开始日期不能晚于结束日期");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "启用前必须人工确认（human_confirmed）");
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
                        "CROSS_MERCHANT_FORBIDDEN", "不能为其他商户创建合规记录");
            }
            return scope.get();
        }
        return requireText(requestedMerchantId, "merchant_id");
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR";
    }
}
