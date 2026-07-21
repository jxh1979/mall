package com.complex.platform.leasing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.ContractRecord;
import com.complex.platform.contract.ContractService;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformService;
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.MerchantProfileRequest;
import com.complex.platform.leasing.dto.LeasingBrandBlacklistRequest;
import com.complex.platform.leasing.dto.LeasingBrandRequest;
import com.complex.platform.leasing.dto.LeasingFollowUpRequest;
import com.complex.platform.leasing.dto.LeasingIntentAdvanceRequest;
import com.complex.platform.leasing.dto.LeasingIntentRequest;
import com.complex.platform.leasing.dto.LeasingLeadAssignRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingLeadStatusRequest;
import com.complex.platform.leasing.dto.LeasingSignRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.complex.platform.common.NotificationWindowConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招商获客（V161 重建，据 leasing-acquisition-prd-v1.0 + 业务 HTML §10 + M1 需求采集 §1）。
 * 纠正旧版：线索 7 态、意向 5 阶段独立流转、品牌库 4 类+黑名单拦截、跟进+3 天提醒、签约转合同四类主体默认同公司。
 */
@Service
public class LeasingService {

    private static final Set<String> LEAD_SOURCES = Set.of("outbound", "online_inquiry", "referral", "exhibition");
    private static final Set<String> LEAD_STATUSES = Set.of("pending_follow_up", "following_up",
            "converted_to_intent", "negotiating", "signed", "lost", "on_hold");
    private static final Set<String> LEAD_REASON_REQUIRED = Set.of("lost", "on_hold");
    private static final Set<String> INTENT_STAGES = Set.of("initial_contact", "requirement_confirmed",
            "proposal_quoted", "negotiation", "pending_signing");
    private static final Set<String> INTENT_KEY_STAGES = Set.of("proposal_quoted", "negotiation");
    private static final Set<String> BRAND_TYPES = Set.of("intentional", "prospective", "partner", "blacklist");
    private static final Set<String> FOLLOW_UP_REF_TYPES = Set.of("lead", "intent");
    // V219:招商跟进提醒窗天数迁入 notification_windows(key=leasing.lead_follow_up),缺配置兜底 3(行为不变)。
    private static final int FOLLOW_UP_REMINDER_DAYS = 3;
    private static final String FOLLOW_UP_WINDOW_KEY = "leasing.lead_follow_up";
    private static final int DEFAULT_PAGE_SIZE = 500;

    private final LeasingRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final ContractService contractService;
    private final PlatformService platformService;
    private final NotificationWindowConfig windowConfig;

    @org.springframework.beans.factory.annotation.Autowired
    public LeasingService(LeasingRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ContractService contractService,
            PlatformService platformService, NotificationWindowConfig windowConfig) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.contractService = contractService;
        this.platformService = platformService;
        this.windowConfig = windowConfig;
    }

    /** 测试便捷构造:提醒窗回落默认。 */
    public LeasingService(LeasingRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ContractService contractService,
            PlatformService platformService) {
        this(repository, auditEventService, authorizationService, contractService, platformService,
                new NotificationWindowConfig());
    }

    // ============ 线索 ============
    public PageResponse<LeasingLeadRecord> listLeads() {
        return listLeads(1, DEFAULT_PAGE_SIZE);
    }

    // AUD-005 真分页: controller 传 page/size, 仓储 SQL COUNT + LIMIT/OFFSET, total 取真值。
    public PageResponse<LeasingLeadRecord> listLeads(int page, int size) {
        authorizationService.requirePlatformScope();
        // 3.2 跨项目读隔离：项目员工只见本项目，平台管理员/全局平台员工全量。
        PageResponse<LeasingLeadRecord> result = repository.listLeadsPaged(currentProjectScope(), page, size);
        auditEventService.recordSuccess("leasing.lead.viewed", "leasing.lead.manage", "leasing_lead", "all",
                null, null, Map.of("count", result.items().size(), "total", result.total()));
        return result;
    }

    /** 3 天未跟进的活跃线索（负责人待办提醒）。 */
    public PageResponse<LeasingLeadRecord> listLeadsNeedingFollowUp() {
        return listLeadsNeedingFollowUp(1, DEFAULT_PAGE_SIZE);
    }

    public PageResponse<LeasingLeadRecord> listLeadsNeedingFollowUp(int page, int size) {
        authorizationService.requirePlatformScope();
        Instant cutoff = Instant.now().minus(
                windowConfig.days(FOLLOW_UP_WINDOW_KEY, FOLLOW_UP_REMINDER_DAYS), ChronoUnit.DAYS);
        // 3.6：下沉仓储层 WHERE due，不再拉全表内存过滤。
        return repository.listLeadsNeedingFollowUpPaged(cutoff, currentProjectScope(), page, size);
    }

    private Instant reference(LeasingLeadRecord l) {
        return l.lastFollowUpAt() != null ? l.lastFollowUpAt() : l.createdAt();
    }

    public LeasingLeadRecord createLead(LeasingLeadRequest request) {
        authorizationService.requirePlatformScope();
        LeasingLeadRequest actual = requireRequest(request);
        String source = requireEnum(actual.source(), LEAD_SOURCES, "source");
        String brandId = trimToNull(actual.brandId());
        requireBrandNotBlacklisted(brandId);
        requireSpaceExistsIfPresent(trimToNull(actual.intendedSpaceId()));
        Instant now = Instant.now();
        LeasingLeadRecord saved = repository.saveLead(new LeasingLeadRecord(
                "lead_" + UUID.randomUUID(), requireText(actual.contactName(), "contact_name"),
                trimToNull(actual.contactPhone()), source, trimToNull(actual.intendedBusinessType()),
                trimToNull(actual.intendedSpaceId()), actual.intendedAreaSqm(), brandId, null,
                "pending_follow_up", null, null, null, now, now));
        auditEventService.recordSuccess("leasing.lead.created", "leasing.lead.manage", "leasing_lead", saved.id(),
                null, actual.idempotencyKey(), Map.of("source", source));
        return saved;
    }

    public LeasingLeadRecord assignLead(String leadId, LeasingLeadAssignRequest request) {
        authorizationService.requirePlatformScope();
        LeasingLeadAssignRequest actual = requireRequest(request);
        String ownerUserId = requireText(actual.ownerUserId(), "owner_user_id");
        LeasingLeadRecord ex = requireLead(leadId);
        LeasingLeadRecord saved = repository.saveLead(new LeasingLeadRecord(ex.id(), ex.contactName(),
                ex.contactPhone(), ex.source(), ex.intendedBusinessType(), ex.intendedSpaceId(), ex.intendedAreaSqm(),
                ex.brandId(), ownerUserId, ex.status(), ex.lostOrHoldReason(), ex.lastFollowUpAt(), ex.projectId(),
                ex.createdAt(), Instant.now()));
        auditEventService.recordSuccess("leasing.lead.assigned", "leasing.lead.manage", "leasing_lead", ex.id(),
                null, actual.idempotencyKey(), Map.of("owner_user_id", ownerUserId));
        return saved;
    }

    public LeasingLeadRecord updateLeadStatus(String leadId, LeasingLeadStatusRequest request) {
        authorizationService.requirePlatformScope();
        LeasingLeadStatusRequest actual = requireRequest(request);
        String status = requireEnum(actual.status(), LEAD_STATUSES, "status");
        String reason = trimToNull(actual.reason());
        if (LEAD_REASON_REQUIRED.contains(status) && reason == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "reason is required for status " + status);
        }
        LeasingLeadRecord ex = requireLead(leadId);
        LeasingLeadRecord saved = repository.saveLead(new LeasingLeadRecord(ex.id(), ex.contactName(),
                ex.contactPhone(), ex.source(), ex.intendedBusinessType(), ex.intendedSpaceId(), ex.intendedAreaSqm(),
                ex.brandId(), ex.ownerUserId(), status, reason, ex.lastFollowUpAt(), ex.projectId(),
                ex.createdAt(), Instant.now()));
        auditEventService.recordSuccess("leasing.lead.status_changed", "leasing.lead.manage", "leasing_lead", ex.id(),
                null, actual.idempotencyKey(), Map.of("status", status));
        return saved;
    }

    // ============ 意向 ============
    public PageResponse<LeasingIntentRecord> listIntents() {
        return listIntents(1, DEFAULT_PAGE_SIZE);
    }

    public PageResponse<LeasingIntentRecord> listIntents(int page, int size) {
        authorizationService.requirePlatformScope();
        PageResponse<LeasingIntentRecord> result = repository.listIntentsPaged(currentProjectScope(), page, size);
        auditEventService.recordSuccess("leasing.intent.viewed", "leasing.intent.manage", "leasing_intent", "all",
                null, null, Map.of("count", result.items().size(), "total", result.total()));
        return result;
    }

    /** 线索转意向：lead → converted_to_intent，生成意向（stage 起始 initial_contact）。 */
    public LeasingIntentRecord createIntent(LeasingIntentRequest request) {
        authorizationService.requirePlatformScope();
        LeasingIntentRequest actual = requireRequest(request);
        LeasingLeadRecord lead = requireLead(actual.leadId());
        requireSpaceExistsIfPresent(trimToNull(actual.intendedSpaceId()));
        Instant now = Instant.now();
        LeasingIntentRecord saved = repository.saveIntent(new LeasingIntentRecord(
                "intent_" + UUID.randomUUID(), lead.id(), "initial_contact",
                trimToNull(actual.intendedSpaceId()) != null ? trimToNull(actual.intendedSpaceId()) : lead.intendedSpaceId(),
                actual.intendedAreaSqm() != null ? actual.intendedAreaSqm() : lead.intendedAreaSqm(),
                actual.rentTerms() == null ? Map.of() : actual.rentTerms(), now, null, lead.projectId(), now, now));
        repository.saveLead(new LeasingLeadRecord(lead.id(), lead.contactName(), lead.contactPhone(), lead.source(),
                lead.intendedBusinessType(), lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(),
                lead.ownerUserId(), "converted_to_intent", lead.lostOrHoldReason(), lead.lastFollowUpAt(),
                lead.projectId(), lead.createdAt(), now));
        auditEventService.recordSuccess("leasing.intent.created", "leasing.intent.manage", "leasing_intent", saved.id(),
                null, actual.idempotencyKey(), Map.of("lead_id", lead.id()));
        return saved;
    }

    /** 意向阶段推进；关键阶段（方案报价/商务谈判）须带主管确认人，否则阻断。 */
    public LeasingIntentRecord advanceIntent(String intentId, LeasingIntentAdvanceRequest request) {
        authorizationService.requirePlatformScope();
        LeasingIntentAdvanceRequest actual = requireRequest(request);
        String target = requireEnum(actual.targetStage(), INTENT_STAGES, "target_stage");
        String supervisor = trimToNull(actual.supervisorUserId());
        if (INTENT_KEY_STAGES.contains(target) && supervisor == null) {
            throw ApiException.conflict("LEASING_INTENT_SUPERVISOR_REQUIRED",
                    "advancing to " + target + " requires supervisor confirmation");
        }
        LeasingIntentRecord ex = requireIntent(intentId);
        Instant now = Instant.now();
        LeasingIntentRecord saved = repository.saveIntent(new LeasingIntentRecord(ex.id(), ex.leadId(), target,
                ex.intendedSpaceId(), ex.intendedAreaSqm(), ex.rentTerms(), now, supervisor, ex.projectId(),
                ex.createdAt(), now));
        auditEventService.recordSuccess("leasing.intent.advanced", "leasing.intent.manage", "leasing_intent", ex.id(),
                null, actual.idempotencyKey(), Map.of("stage", target, "supervisor_user_id",
                        supervisor == null ? "" : supervisor));
        return saved;
    }

    /**
     * 签约：pending_signing 意向 → 喂入合同 draft（V162 合同域 ContractService），lead → signed。
     * 四类主体默认同一公司：operation/settlement 缺省指向 contractSubjectId，后续可拆。
     * 合同后续在合同域走 提交→分组并行审批→生效固化；本入口只把意向条款喂入 draft（带 projectId 隔离）。
     * AUD-002：整入口开事务——商户/四主体绑定/合同/线索/审计一次业务转换，任一步失败全回滚，无半成品。
     */
    @Transactional
    public ContractRecord signIntent(String intentId, LeasingSignRequest request) {
        authorizationService.requirePlatformScope();
        LeasingSignRequest actual = requireRequest(request);
        LeasingIntentRecord intent = requireIntent(intentId);
        if (!"pending_signing".equals(intent.stage())) {
            throw ApiException.conflict("LEASING_INTENT_NOT_PENDING_SIGNING",
                    "only pending_signing intent can be signed");
        }
        String contractSubjectId = requireText(actual.contractSubjectId(), "contract_subject_id");
        String operationSubjectId = trimToNull(actual.operationSubjectId());
        String settlementSubjectId = trimToNull(actual.settlementSubjectId());
        // 1.4 决策A：merchant_id 空 → 签约自动建正式商户档案（带入线索/意向信息，四类主体默认同一公司=contractSubjectId）。
        String merchantId = trimToNull(actual.merchantId());
        if (merchantId == null) {
            merchantId = autoCreateMerchantForSign(intent, contractSubjectId);
            // AUD-003：签约建"四主体"权威绑定——与商户/合同/线索同一事务原子落库；缺此则权威表(business_entity_bindings)恒空、
            // 主体绑定页/结算/权限按权威表读取时会把正式商户当未绑定。经营/结算主体缺省=合同主体（默认同一公司，后续走既有变更流拆分）。
            createFourPartyBinding(intent, merchantId, contractSubjectId, operationSubjectId, settlementSubjectId,
                    actual.effectiveDate());
        }
        // 3.13：签约草稿带意向空间 → 起草即校验存在性+与生效合同冲突（早反馈，不等生效）。
        List<String> intendedSpaces = intent.intendedSpaceId() != null
                ? List.of(intent.intendedSpaceId()) : List.of();
        ContractRecord contract = contractService.createDraft(new ContractDraftRequest(
                "lease-sign-" + intent.id(),
                merchantId, contractSubjectId,
                operationSubjectId != null ? operationSubjectId : contractSubjectId,
                settlementSubjectId != null ? settlementSubjectId : contractSubjectId,
                actual.effectiveDate(), actual.expiryDate(), intent.rentTerms(),
                null, null, null, null, null, List.of(), intent.projectId(), null, intendedSpaces));
        repository.findLeadById(intent.leadId()).ifPresent(lead -> repository.saveLead(new LeasingLeadRecord(
                lead.id(), lead.contactName(), lead.contactPhone(), lead.source(), lead.intendedBusinessType(),
                lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(), lead.ownerUserId(), "signed",
                lead.lostOrHoldReason(), lead.lastFollowUpAt(), lead.projectId(), lead.createdAt(), Instant.now())));
        auditEventService.recordSuccess("leasing.intent.signed", "leasing.convert", "leasing_intent", intent.id(),
                null, actual.idempotencyKey(), Map.of("contract_id", contract.id()));
        return contract;
    }

    /**
     * 1.4 决策A：签约无既有商户 → 自动建正式商户档案，带入线索联系人/业态/空间/面积（四类主体默认同一公司=contractSubjectId）。
     * 稳定 idempotency key（按意向）→ 重签命中同一商户、不重复建；租金条款随合同 draft 带入，商户档案后续可完善。
     */
    private String autoCreateMerchantForSign(LeasingIntentRecord intent, String contractSubjectId) {
        LeasingLeadRecord lead = repository.findLeadById(intent.leadId()).orElse(null);
        String contactName = lead != null ? trimToNull(lead.contactName()) : null;
        String contactPhone = lead != null ? trimToNull(lead.contactPhone()) : null;
        String businessType = lead != null ? trimToNull(lead.intendedBusinessType()) : null;
        String spaceCode = intent.intendedSpaceId() != null ? intent.intendedSpaceId()
                : (lead != null ? lead.intendedSpaceId() : null);
        java.math.BigDecimal area = intent.intendedAreaSqm() != null ? intent.intendedAreaSqm()
                : (lead != null ? lead.intendedAreaSqm() : null);
        MerchantRecord merchant = platformService.createMerchant(new MerchantProfileRequest(
                "lease-sign-merchant-" + intent.id(),
                "auto-created on leasing sign", List.of(),
                contactName != null ? contactName : ("招商签约商户-" + intent.id()),
                businessType != null ? businessType : "retail",
                contractSubjectId, contractSubjectId, contractSubjectId,
                contactName, contactPhone, contactName,
                spaceCode, area, area,
                null, null, null, "leasing_auto", "active"));
        return merchant.id();
    }

    /**
     * AUD-003：招商签约建立"四主体"权威绑定，复用治理域既有绑定能力（每商户唯一 active + 版本化变更）。
     * 稳定 idempotency key（按意向）→ 重签命中同一 active 绑定、不重复建。经营/结算主体缺省=合同主体（默认同一公司，后续可拆）。
     */
    private void createFourPartyBinding(LeasingIntentRecord intent, String merchantId, String contractSubjectId,
            String operationSubjectId, String settlementSubjectId, LocalDate effectiveFrom) {
        platformService.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                "lease-sign-binding-" + intent.id(),
                "auto-created on leasing sign",
                List.of(),
                merchantId,
                operationSubjectId != null ? operationSubjectId : contractSubjectId,
                contractSubjectId,
                settlementSubjectId != null ? settlementSubjectId : contractSubjectId,
                effectiveFrom,
                "招商签约自动建立四主体权威绑定"));
    }

    // ============ 品牌库 ============
    public PageResponse<LeasingBrandRecord> listBrands() {
        return listBrands(1, DEFAULT_PAGE_SIZE);
    }

    public PageResponse<LeasingBrandRecord> listBrands(int page, int size) {
        authorizationService.requirePlatformScope();
        PageResponse<LeasingBrandRecord> result = repository.listBrandsPaged(currentProjectScope(), page, size);
        auditEventService.recordSuccess("leasing.brand.viewed", "leasing.brand.manage", "leasing_brand", "all",
                null, null, Map.of("count", result.items().size(), "total", result.total()));
        return result;
    }

    public LeasingBrandRecord createBrand(LeasingBrandRequest request) {
        authorizationService.requirePlatformScope();
        LeasingBrandRequest actual = requireRequest(request);
        String type = requireEnum(actual.brandType(), BRAND_TYPES, "brand_type");
        Instant now = Instant.now();
        LeasingBrandRecord saved = repository.saveBrand(new LeasingBrandRecord(
                "brand_" + UUID.randomUUID(), requireText(actual.brandName(), "brand_name"), type,
                trimToNull(actual.businessType()), null, null, null, null, now, now));
        auditEventService.recordSuccess("leasing.brand.created", "leasing.brand.manage", "leasing_brand", saved.id(),
                null, actual.idempotencyKey(), Map.of("brand_type", type));
        return saved;
    }

    /** 拉黑/解黑品牌（高危需审批 leasing.brand.blacklist）。拉黑后同品牌新线索被 createLead 拦截。 */
    public LeasingBrandRecord blacklistBrand(String brandId, LeasingBrandBlacklistRequest request) {
        authorizationService.requirePlatformScope();
        LeasingBrandBlacklistRequest actual = requireRequest(request);
        LeasingBrandRecord ex = repository.findBrandById(requireText(brandId, "brand_id"))
                .orElseThrow(() -> ApiException.notFound("LEASING_BRAND_NOT_FOUND", "brand not found"));
        Instant now = Instant.now();
        LeasingBrandRecord saved;
        if (actual.blacklist()) {
            String reason = requireText(actual.reason(), "reason");
            saved = repository.saveBrand(new LeasingBrandRecord(ex.id(), ex.brandName(), "blacklist",
                    ex.businessType(), reason, authorizationService.currentActor().id(), now, ex.projectId(),
                    ex.createdAt(), now));
        } else {
            saved = repository.saveBrand(new LeasingBrandRecord(ex.id(), ex.brandName(), "prospective",
                    ex.businessType(), null, null, null, ex.projectId(), ex.createdAt(), now));
        }
        auditEventService.recordSuccess("leasing.brand.blacklist_changed", "leasing.brand.blacklist", "leasing_brand",
                ex.id(), null, actual.idempotencyKey(), Map.of("blacklist", actual.blacklist()));
        return saved;
    }

    // ============ 跟进 ============
    public PageResponse<LeasingFollowUpRecord> listFollowUps(String refType, String refId) {
        return listFollowUps(refType, refId, 1, DEFAULT_PAGE_SIZE);
    }

    public PageResponse<LeasingFollowUpRecord> listFollowUps(String refType, String refId, int page, int size) {
        authorizationService.requirePlatformScope();
        // 招商工作台首屏无参加载=全量跟进；带 ref_type+ref_id=精确下钻（原语义保留）。
        boolean drillDown = trimToNull(refType) != null || trimToNull(refId) != null;
        if (drillDown) {
            String type = requireEnum(refType, FOLLOW_UP_REF_TYPES, "ref_type");
            return repository.listFollowUpsByRefPaged(
                    type, requireText(refId, "ref_id"), currentProjectScope(), page, size);
        }
        return repository.listAllFollowUpsPaged(currentProjectScope(), page, size);
    }

    public LeasingFollowUpRecord createFollowUp(LeasingFollowUpRequest request) {
        authorizationService.requirePlatformScope();
        LeasingFollowUpRequest actual = requireRequest(request);
        String type = requireEnum(actual.refType(), FOLLOW_UP_REF_TYPES, "ref_type");
        String refId = requireText(actual.refId(), "ref_id");
        Instant now = Instant.now();
        LeasingFollowUpRecord saved = repository.saveFollowUp(new LeasingFollowUpRecord(
                "follow_" + UUID.randomUUID(), type, refId, trimToNull(actual.method()),
                requireText(actual.content(), "content"), actual.nextPlanAt(),
                authorizationService.currentActor().id(), null, now));
        // 回写线索 last_follow_up_at（3 天提醒依据）：ref=lead 直接回写，ref=intent 回写其所属线索。
        String leadId = "lead".equals(type) ? refId
                : repository.findIntentById(refId).map(LeasingIntentRecord::leadId).orElse(null);
        if (leadId != null) {
            repository.findLeadById(leadId).ifPresent(lead -> repository.saveLead(new LeasingLeadRecord(
                    lead.id(), lead.contactName(), lead.contactPhone(), lead.source(), lead.intendedBusinessType(),
                    lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(), lead.ownerUserId(),
                    "pending_follow_up".equals(lead.status()) ? "following_up" : lead.status(),
                    lead.lostOrHoldReason(), now, lead.projectId(), lead.createdAt(), now)));
        }
        auditEventService.recordSuccess("leasing.follow_up.created", "leasing.lead.manage", "leasing_follow_up",
                saved.id(), null, actual.idempotencyKey(), Map.of("ref_type", type, "ref_id", refId));
        return saved;
    }

    // ============ helpers ============
    /** 3.2：项目员工读取按本项目过滤（organizationId=项目），平台管理员/全局平台员工 null=全量。 */
    private String currentProjectScope() {
        return authorizationService.currentProjectScopeQuietly().orElse(null);
    }

    private void requireBrandNotBlacklisted(String brandId) {
        if (brandId == null) {
            return;
        }
        repository.findBrandById(brandId).ifPresent(b -> {
            if ("blacklist".equals(b.brandType())) {
                throw ApiException.conflict("LEASING_BRAND_BLACKLISTED",
                        "brand is blacklisted; new lead blocked, needs higher authority to override");
            }
        });
    }

    /** 3.13：意向空间传了就必须真实存在（空间=地产主数据，不允许挂空编号；不传=尚未定位，放行）。 */
    private void requireSpaceExistsIfPresent(String spaceId) {
        if (spaceId != null && !platformService.spaceExists(spaceId)) {
            throw ApiException.badRequest("LEASING_SPACE_NOT_FOUND", "intended space does not exist: " + spaceId);
        }
    }

    private LeasingLeadRecord requireLead(String leadId) {
        return repository.findLeadById(requireText(leadId, "lead_id"))
                .orElseThrow(() -> ApiException.notFound("LEASING_LEAD_NOT_FOUND", "lead not found"));
    }

    private LeasingIntentRecord requireIntent(String intentId) {
        return repository.findIntentById(requireText(intentId, "intent_id"))
                .orElseThrow(() -> ApiException.notFound("LEASING_INTENT_NOT_FOUND", "intent not found"));
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        String t = value == null ? "" : value.trim();
        if (t.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return t;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String t = requireText(value, field);
        if (!allowed.contains(t)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return t;
    }

    private String trimToNull(String value) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? null : t;
    }
}
