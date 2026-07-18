package com.complex.platform.contract;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractAttachmentRequest;
import com.complex.platform.contract.dto.ContractApproveRequest;
import com.complex.platform.contract.dto.ContractChangeApproveRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractRentBillRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.SpaceExpiryColorRuleRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import com.complex.platform.governance.SpaceRecord;
import com.complex.platform.merchantbilling.MerchantFeeNotificationRecipientResolver;
import com.complex.platform.notification.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 合同租赁全生命周期（V162 重建，据 contract-lifecycle-prd-v1.0 V1.1 + M1 §2 + 用户裁决）。
 * 核心：起草 → 提交 → 分组并行审批（可配链，阶段内会签、阶段间串行）→ 生效固化快照 + 绑铺位 → 变更（6类）→ 退租逐例结算 + 争议挂起。
 */
@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private static final String MERCHANT_FEE_DOMAIN = "merchant_fee";
    private static final String MERCHANT_FEE_RENEWAL_EVENT = "renewal";
    private static final String MERCHANT_FEE_RENEWAL_TEMPLATE = "sms.merchant_fee.renewal";
    private static final Set<String> COMMISSION_MODES = Set.of(
            "guarantee_or_commission_higher", "fixed_ratio", "tiered_ratio", "guarantee_plus_commission");
    private static final Set<String> ENERGY_BASES = Set.of("none", "by_contract", "by_area", "by_usage");
    private static final Set<String> CHANGE_TYPES = Set.of(
            "renewal", "move_out", "area_change", "rent_change", "business_scope_change", "early_termination");
    private static final Set<String> SETTLE_TRIGGERS = Set.of("move_out", "expiry", "early_termination");
    private static final Set<String> DECISIONS = Set.of("approve", "reject");

    private final ContractRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final PlatformRepository platformRepository;
    private final MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver;
    // 批2b:提成端口(finance 实现)。构造注入(@Lazy 防潜在初始化环);旧构造传 null=收租方式退化纯月租。
    private final com.complex.platform.finance.TurnoverRentPort turnoverRentPort;

    @Autowired
    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver,
            @org.springframework.context.annotation.Lazy com.complex.platform.finance.TurnoverRentPort turnoverRentPort) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.platformRepository = platformRepository;
        this.merchantFeeRecipientResolver = merchantFeeRecipientResolver;
        this.turnoverRentPort = turnoverRentPort;
    }

    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository,
            MerchantFeeNotificationRecipientResolver merchantFeeRecipientResolver) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository, merchantFeeRecipientResolver, null);
    }

    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, NotificationService notificationService,
            PlatformRepository platformRepository) {
        this(repository, auditEventService, authorizationService, notificationService, platformRepository, null, null);
    }

    // ============ 起草 ============
    public PageResponse<ContractRecord> listContracts() {
        List<ContractRecord> items = repository.listContracts();
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /** 起草合同（手工或招商签约带入），状态 drafting。也是招商 signIntent 接线入口。 */
    public ContractRecord createDraft(ContractDraftRequest request) {
        authorizationService.requirePlatformScope();
        ContractDraftRequest actual = requireRequest(request);
        // 1.5 幂等：带 idempotencyKey 时合同 id 由其确定性派生；重复提交/招商重签命中既有草稿直接返回，不重复建。
        String idempotencyKey = trimToNull(actual.idempotencyKey());
        String contractId = idempotencyKey != null
                ? "ctr_" + deterministicSuffix(idempotencyKey)
                : "ctr_" + UUID.randomUUID();
        if (idempotencyKey != null) {
            ContractRecord existing = repository.findContractById(contractId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        String commissionMode = trimToNull(actual.commissionMode());
        if (commissionMode != null && !COMMISSION_MODES.contains(commissionMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "commission_mode is invalid");
        }
        String energyBasis = trimToNull(actual.energyFeeBasis()) == null ? "none" : actual.energyFeeBasis().trim();
        if (!ENERGY_BASES.contains(energyBasis)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "energy_fee_basis is invalid");
        }
        // 清单 P1-3:固定能源费金额仅 by_contract 包干允许;按面积/按用量(需抄表)或无能源费的合同填固定额=脏数据,下单即拒。
        if (numLong((actual.rentTerms() == null ? Map.of() : actual.rentTerms()).get("energyFeeCent")) > 0
                && !"by_contract".equals(energyBasis)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "energyFeeCent is only allowed when energy_fee_basis is by_contract");
        }
        Instant now = Instant.now();
        Map<String, Object> rentTerms = new HashMap<>(actual.rentTerms() == null ? Map.of() : actual.rentTerms());
        if (trimToNull(actual.businessScope()) != null) {
            rentTerms.put("businessScope", actual.businessScope().trim());   // 经营范围文字描述存 rent_terms
        }
        // 经营范围结构化（2026-07-18 拍板）：取值校验限既有业务类目；存 rent_terms.allowedCategories，
        // 下单侧（bar/票务）读此键拦超范围经营；空/缺省=不拦截（存量合同兼容）。
        List<String> allowedCategories = ContractScopeGuard.validateAllowedCategories(actual.allowedCategories());
        if (allowedCategories != null && !allowedCategories.isEmpty()) {
            rentTerms.put(ContractScopeGuard.RENT_TERMS_KEY, allowedCategories);
        }
        ContractRecord saved = repository.saveContract(new ContractRecord(
                contractId,
                requireText(actual.merchantId(), "merchant_id"),
                requireText(actual.contractSubjectId(), "contract_subject_id"),
                trimToNull(actual.operationSubjectId()),
                requireText(actual.settlementSubjectId(), "settlement_subject_id"),
                requireDate(actual.effectiveDate(), "effective_date"),
                requireDate(actual.expiryDate(), "expiry_date"),
                rentTerms,
                commissionMode, energyBasis, actual.depositCent(),
                actual.penaltyTerms() == null ? Map.of() : Map.copyOf(actual.penaltyTerms()),
                actual.settlementRules() == null ? List.of() : List.copyOf(actual.settlementRules()),
                null, null,
                actual.attachmentIds() == null ? List.of() : List.copyOf(actual.attachmentIds()),
                "drafting", trimToNull(actual.projectId()), null, null, null, now, now));
        auditEventService.recordSuccess("contract.drafted", "contract.draft.manage", "contract", saved.id(),
                null, actual.idempotencyKey(), Map.of("merchant_id", saved.merchantId()));
        // 3.13 冲突早检：草稿带意向空间 → 空间必须真实存在 + 与生效合同租期重叠即拒（不必等生效终检），并预绑定。
        bindDraftSpacesWithEarlyConflictCheck(saved, actual.spaceIds());
        return saved;
    }

    /** 幂等键 → 稳定 id 后缀（sha256 前 48 位 hex，避免长键截断碰撞）。 */
    private String deterministicSuffix(String idempotencyKey) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 48);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** 追加合同附件（电子签章扫描件归档），去重合并。 */
    @Transactional
    public ContractRecord addAttachments(String contractId, ContractAttachmentRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        ContractAttachmentRequest actual = requireRequest(request);
        List<String> add = (actual.attachmentIds() == null ? List.<String>of() : actual.attachmentIds())
                .stream().map(s -> requireText(s, "attachment_id")).distinct().toList();
        if (add.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "attachment_ids is required");
        }
        ContractRecord c = requireContract(contractId);
        List<String> merged = new ArrayList<>(c.attachmentIds() == null ? List.of() : c.attachmentIds());
        for (String a : add) {
            if (!merged.contains(a)) {
                merged.add(a);
            }
        }
        ContractRecord saved = repository.saveContract(withAttachments(c, merged));
        auditEventService.recordSuccess("contract.attachments_added", "contract.draft.manage", "contract", contractId,
                null, actual.idempotencyKey(), Map.of("added", add.size(), "total", merged.size()));
        return saved;
    }

    // ============ 提交 + 分组并行审批 ============
    /** 提交审批：按该综合体生效链物化第一阶段并行审批人，合同→待确认。 */
    @Transactional
    /** 3.13：空间存在性 + 与生效合同租期重叠早检 + 预绑定（草稿期，activate 仍保留终检）。 */
    private void bindDraftSpacesWithEarlyConflictCheck(ContractRecord contract, List<String> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            return;
        }
        List<String> resolved = spaceIds.stream().map(id -> requireText(id, "space_id")).distinct().toList();
        for (String spaceId : resolved) {
            if (platformRepository != null && platformRepository.findSpaceById(spaceId).isEmpty()) {
                throw ApiException.badRequest("CONTRACT_SPACE_NOT_FOUND", "space does not exist: " + spaceId);
            }
            requireNoOverlappingEffectiveContract(contract, spaceId);
        }
        repository.bindContractSpaces(contract.id(), resolved, contract.projectId());
    }

    private void requireNoOverlappingEffectiveContract(ContractRecord contract, String spaceId) {
        if (contract.effectiveDate() == null || contract.expiryDate() == null) {
            return; // 租期未定无从判重叠；生效终检兜底。
        }
        if (repository.hasOverlappingEffectiveContractForSpace(
                spaceId, contract.effectiveDate(), contract.expiryDate(), contract.id())) {
            throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                    "space already let by an overlapping effective contract: " + spaceId);
        }
    }

    public ContractRecord submit(String contractId, String idempotencyKey) {
        authorizationService.requirePlatformScope();
        ContractRecord contract = requireContract(contractId);
        if (!"drafting".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_DRAFTING", "only drafting contract can be submitted");
        }
        // 3.13：提交审批前对草稿预绑定空间再检冲突——审批人不该审一份注定冲突的合同。
        for (String spaceId : repository.listContractSpaceIds(contractId)) {
            requireNoOverlappingEffectiveContract(contract, spaceId);
        }
        List<ContractApprovalFlowStepRecord> steps = repository.listActiveFlowSteps(contract.projectId());
        if (steps.isEmpty()) {
            throw ApiException.conflict("CONTRACT_NO_APPROVAL_FLOW", "no active approval flow configured");
        }
        repository.clearApprovals(contractId);
        int firstStage = steps.stream().mapToInt(ContractApprovalFlowStepRecord::stageSeq).min().orElseThrow();
        materializeStage(contractId, null, steps, firstStage);
        ContractRecord saved = repository.saveContract(withStatus(contract, "pending_confirmation"));
        auditEventService.recordSuccess("contract.submitted", "contract.draft.manage", "contract", contractId,
                null, idempotencyKey, Map.of("first_stage", firstStage));
        return saved;
    }

    /**
     * 审批当前阶段的一个并行槽：approve/reject。
     * 当前阶段全 approved → 有下一阶段则物化之，否则合同→待生效；任一 reject → 整单退回起草。
     */
    @Transactional
    public ContractRecord approve(String contractId, ContractApproveRequest request) {
        ContractApproveRequest actual = requireRequest(request);
        String code = requireText(actual.approverPermissionCode(), "approver_permission_code");
        authorizationService.requirePermission(code);
        String decision = requireEnum(actual.decision(), DECISIONS, "decision");
        ContractRecord contract = requireContract(contractId);
        if (!"pending_confirmation".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_IN_APPROVAL", "contract is not under approval");
        }
        StageOutcome outcome = recordSlotDecision(contractId, null, repository.listApprovalsByContract(contractId),
                contract.projectId(), code, decision, trimToNull(actual.comment()));
        ContractRecord result = contract;
        if (outcome == StageOutcome.REJECTED) {
            repository.clearApprovals(contractId);
            result = repository.saveContract(withStatus(contract, "drafting"));
        } else if (outcome == StageOutcome.COMPLETED) {
            result = repository.saveContract(withStatus(contract, "pending_effective"));
        }
        auditEventService.recordSuccess("contract.approved_slot", code, "contract", contractId,
                null, actual.idempotencyKey(), Map.of("decision", decision, "status", result.lifecycleStatus()));
        return result;
    }

    public PageResponse<ContractApprovalRecord> listApprovals(String contractId) {
        List<ContractApprovalRecord> items = repository.listApprovalsByContract(contractId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    // ============ 生效 + 固化 ============
    /** 生效：固化收益快照 + 绑定铺位（防重复出租）。合同须待生效。 */
    @Transactional
    public ContractRecord activate(String contractId, ContractActivateRequest request) {
        authorizationService.requirePermission("contract.lifecycle.activate");
        ContractActivateRequest actual = requireRequest(request);
        List<String> spaceIds = (actual.spaceIds() == null ? List.<String>of() : actual.spaceIds())
                .stream().map(s -> requireText(s, "space_id")).distinct().toList();
        if (spaceIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "space_ids is required");
        }
        ContractRecord contract = requireContract(contractId);
        if (!"pending_effective".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_PENDING_EFFECTIVE",
                    "contract must pass full approval before activation");
        }
        for (String spaceId : spaceIds) {
            if (repository.hasOverlappingEffectiveContractForSpace(
                    spaceId, contract.effectiveDate(), contract.expiryDate(), contractId)) {
                throw ApiException.conflict("CONTRACT_SPACE_CONFLICT",
                        "space already let by an overlapping effective contract: " + spaceId);
            }
        }
        repository.bindContractSpaces(contractId, spaceIds, contract.projectId());
        // 批2b:每平米租金生效固化——用绑定铺位登记面积×每平米单价算月租(baseRentCent)固化进生效快照。
        ContractRecord contractEff = withRentTerms(contract, resolvePerSqmBaseRent(contract.rentTerms(), spaceIds));
        ContractRecord effective = repository.saveContract(new ContractRecord(
                contractEff.id(), contractEff.merchantId(), contractEff.contractSubjectId(), contractEff.operationSubjectId(),
                contractEff.settlementSubjectId(), contractEff.effectiveDate(), contractEff.expiryDate(), contractEff.rentTerms(),
                contractEff.commissionMode(), contractEff.energyFeeBasis(), contractEff.depositCent(), contractEff.penaltyTerms(),
                contractEff.settlementRules(), buildSnapshot(contractEff), contractEff.shareholderRatioForReport(),
                contract.attachmentIds(), "effective", contract.projectId(), null, null,
                contract.renewedFromContractId(), contract.createdAt(), Instant.now()));
        // P1-A 收入闭环：生效即按整租期逐期幂等排租金/物业/能源 AR（base+递增−免租，免租/0 跳过）。
        // ponytail: 整租期一次性预排 vs @Scheduled 逐期挂账——选预排：确定可测 + 幂等（genFeeBill 查重，重复生效不重挂），
        //   且每期账单各带自身 due_date（期首），看板按 due_date 逐期确认收入，不在生效日堆叠整租期。
        //   若改 @Scheduled 累积巨量空跑作业，反更重；catch-up 仍可手工调 period-bills 端点（P1-B 按钮）。
        int billed = scheduleTermBills(effective);
        auditEventService.recordSuccess("contract.activated", "contract.lifecycle.activate", "contract", contractId,
                null, actual.idempotencyKey(), Map.of("space_count", spaceIds.size(), "rent_bills", billed));
        return effective;
    }

    /** 按整租期（生效月→到期月）逐期幂等挂账各费目；返回新挂账条数。生效自动调用，亦可手工补排。 */
    private int scheduleTermBills(ContractRecord c) {
        YearMonth end = YearMonth.from(c.expiryDate());
        int count = 0;
        for (YearMonth ym = YearMonth.from(c.effectiveDate()); !ym.isAfter(end); ym = ym.plusMonths(1)) {
            count += billPeriodFees(c, ym.toString(), "activate:" + c.id() + ":" + ym).size();
        }
        return count;
    }

    // ============ 变更（6类） ============
    @Transactional
    public ContractChangeRecord raiseChange(String contractId, ContractChangeRequest request) {
        authorizationService.requirePermission("contract.change.manage");
        ContractChangeRequest actual = requireRequest(request);
        String type = requireEnum(actual.changeType(), CHANGE_TYPES, "change_type");
        ContractRecord contract = requireContract(contractId);
        if (!"effective".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "only effective contract can be changed");
        }
        List<ContractApprovalFlowStepRecord> steps = repository.listActiveFlowSteps(contract.projectId());
        if (steps.isEmpty()) {
            throw ApiException.conflict("CONTRACT_NO_APPROVAL_FLOW", "no active approval flow configured");
        }
        Instant now = Instant.now();
        // 经营范围变更载荷规范化：web 客户端会把嵌套键蛇形化（allowed_categories），统一归一到
        // rent_terms 既有的 camel 键 allowedCategories 并做词表校验，approveChange→applyChange putAll 后即生效。
        Map<String, Object> payload = new HashMap<>(actual.payload() == null ? Map.of() : actual.payload());
        Object rawCategories = payload.remove("allowed_categories");
        if (rawCategories == null) {
            rawCategories = payload.get(ContractScopeGuard.RENT_TERMS_KEY);
        }
        List<String> changedCategories = ContractScopeGuard.validateAllowedCategories(rawCategories);
        if (changedCategories != null) {
            payload.put(ContractScopeGuard.RENT_TERMS_KEY, changedCategories);
        }
        ContractChangeRecord saved = repository.saveChange(new ContractChangeRecord(
                "cch_" + UUID.randomUUID(), contractId, type,
                Map.copyOf(payload), "pending", null,
                contract.projectId(), now, now));
        // 变更走同一可配并行链：物化第一阶段并行审批槽（changeId 维度）。
        int firstStage = steps.stream().mapToInt(ContractApprovalFlowStepRecord::stageSeq).min().orElseThrow();
        materializeStage(contractId, saved.id(), steps, firstStage);
        auditEventService.recordSuccess("contract.change_raised", "contract.change.manage", "contract_change",
                saved.id(), null, actual.idempotencyKey(), Map.of("change_type", type, "contract_id", contractId));
        return saved;
    }

    /**
     * 变更审批：走与新签同一套可配分组并行链（contract_approvals 的 change 维度）。
     * 全阶段通过即应用变更 + 生成新版本快照（不改历史固化）；任一驳回则整变更退回。
     */
    @Transactional
    public ContractChangeRecord approveChange(String changeId, ContractChangeApproveRequest request) {
        ContractChangeApproveRequest actual = requireRequest(request);
        String code = requireText(actual.approverPermissionCode(), "approver_permission_code");
        authorizationService.requirePermission(code);
        String decision = requireEnum(actual.decision(), DECISIONS, "decision");
        ContractChangeRecord change = repository.findChangeById(requireText(changeId, "change_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_CHANGE_NOT_FOUND", "change not found"));
        if (!"pending".equals(change.approvalStatus())) {
            throw ApiException.conflict("CONTRACT_CHANGE_NOT_PENDING", "change is not pending");
        }
        StageOutcome outcome = recordSlotDecision(change.contractId(), changeId,
                repository.listApprovalsByChange(changeId), change.projectId(), code, decision,
                trimToNull(actual.comment()));
        Instant now = Instant.now();
        if (outcome == StageOutcome.REJECTED) {
            repository.clearApprovalsByChange(changeId);
            ContractChangeRecord rejected = repository.saveChange(new ContractChangeRecord(change.id(),
                    change.contractId(), change.changeType(), change.payload(), "rejected", null, change.projectId(),
                    change.createdAt(), now));
            auditEventService.recordSuccess("contract.change_rejected", code, "contract_change", change.id(),
                    null, actual.idempotencyKey(), Map.of());
            return rejected;
        }
        if (outcome == StageOutcome.COMPLETED) {
            ContractRecord contract = requireContract(change.contractId());
            Map<String, Object> newSnapshot = applyChange(contract, change);
            ContractChangeRecord approved = repository.saveChange(new ContractChangeRecord(change.id(),
                    change.contractId(), change.changeType(), change.payload(), "approved", newSnapshot,
                    change.projectId(), change.createdAt(), now));
            auditEventService.recordSuccess("contract.change_approved", code, "contract_change", change.id(),
                    null, actual.idempotencyKey(), Map.of("change_type", change.changeType()));
            return approved;
        }
        // ADVANCED：阶段推进中，变更仍 pending。
        auditEventService.recordSuccess("contract.change_approved_slot", code, "contract_change", change.id(),
                null, actual.idempotencyKey(), Map.of("change_type", change.changeType()));
        return change;
    }

    public PageResponse<ContractChangeRecord> listChanges(String contractId) {
        List<ContractChangeRecord> items = repository.listChangesByContract(contractId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    // ============ 退租 / 到期结算 ============
    /** 退租/到期结算：按合同 settlement_rules 逐例算；带 disputeReason 则挂起「有争议」。 */
    @Transactional
    public ContractSettlementRecord settle(String contractId, ContractSettleRequest request) {
        authorizationService.requirePermission("contract.settlement.manage");
        ContractSettleRequest actual = requireRequest(request);
        String trigger = requireEnum(actual.triggerType(), SETTLE_TRIGGERS, "trigger_type");
        ContractRecord contract = requireContract(contractId);
        if (!Set.of("effective", "expired").contains(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_SETTLEABLE", "contract is not effective/expired");
        }
        String dispute = trimToNull(actual.disputeReason());
        Instant now = Instant.now();
        // 开账：非争议→待结算（合同仍 effective，待 completeSettlement 结清才终止）；争议→挂起。
        String status = dispute != null ? "disputed" : "pending_settlement";
        List<Map<String, Object>> items = dispute != null ? List.of() : runSettlementRules(contract);
        ContractSettlementRecord settlement = repository.saveSettlement(new ContractSettlementRecord(
                "cst_" + UUID.randomUUID(), contractId, trigger, status, items, dispute, null, null,
                contract.projectId(), now, now));
        auditEventService.recordSuccess("contract.settle_opened", "contract.settlement.manage", "contract_settlement",
                settlement.id(), null, actual.idempotencyKey(), Map.of("trigger", trigger, "status", status));
        return settlement;
    }

    /** 主管裁决退租争议，续算至结清。 */
    @Transactional
    public ContractSettlementRecord resolveDispute(String settlementId, ContractResolveDisputeRequest request) {
        authorizationService.requirePermission("contract.settlement.dispute.resolve");
        ContractResolveDisputeRequest actual = requireRequest(request);
        ContractSettlementRecord settlement = repository.findSettlementById(requireText(settlementId, "settlement_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "settlement not found"));
        if (!"disputed".equals(settlement.status())) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_NOT_DISPUTED", "settlement is not disputed");
        }
        ContractRecord contract = requireContract(settlement.contractId());
        Instant now = Instant.now();
        ContractSettlementRecord resumed = repository.saveSettlement(new ContractSettlementRecord(settlement.id(),
                settlement.contractId(), settlement.triggerType(), "pending_settlement", runSettlementRules(contract),
                settlement.disputeReason(), authorizationService.currentActor().id(), now, settlement.projectId(),
                settlement.createdAt(), now));
        // 裁决后回到待结算，待 completeSettlement 结清并终止合同。
        auditEventService.recordSuccess("contract.dispute_resolved", "contract.settlement.dispute.resolve",
                "contract_settlement", settlement.id(), null, actual.idempotencyKey(), Map.of());
        return resumed;
    }

    /**
     * 结清退租结算 → 已结清 + 终止合同。接受 待结算/保证金抵扣中/欠费追缴中。
     * ponytail: 两个中间态(保证金抵扣中/欠费追缴中)为枚举可选相，留作逐相推进的人工流；本切片 开账→结清 两段直推。
     */
    @Transactional
    public ContractSettlementRecord completeSettlement(String settlementId, ContractResolveDisputeRequest request) {
        authorizationService.requirePermission("contract.settlement.manage");
        ContractResolveDisputeRequest actual = requireRequest(request);
        ContractSettlementRecord settlement = repository.findSettlementById(requireText(settlementId, "settlement_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "settlement not found"));
        if (!Set.of("pending_settlement", "deposit_offsetting", "arrears_collecting").contains(settlement.status())) {
            throw ApiException.conflict("CONTRACT_SETTLEMENT_NOT_OPEN", "settlement is not open for completion");
        }
        ContractRecord contract = requireContract(settlement.contractId());
        Instant now = Instant.now();
        ContractSettlementRecord settled = repository.saveSettlement(new ContractSettlementRecord(settlement.id(),
                settlement.contractId(), settlement.triggerType(), "settled", settlement.settlementItems(),
                settlement.disputeReason(), settlement.resolvedBy(), settlement.resolvedAt(), settlement.projectId(),
                settlement.createdAt(), now));
        finalizeContractAfterSettlement(contract, settlement.triggerType(), now);
        auditEventService.recordSuccess("contract.settlement_completed", "contract.settlement.manage",
                "contract_settlement", settlement.id(), null, actual.idempotencyKey(),
                Map.of("trigger", settlement.triggerType()));
        return settled;
    }

    /** 逐相推进退租结算：待结算→保证金抵扣中→欠费追缴中→已结清；到已结清即终止/到期合同（让 2 中间态成真实驻留态）。 */
    @Transactional
    public ContractSettlementRecord advanceSettlement(String settlementId, ContractResolveDisputeRequest request) {
        authorizationService.requirePermission("contract.settlement.manage");
        ContractResolveDisputeRequest actual = requireRequest(request);
        ContractSettlementRecord s = repository.findSettlementById(requireText(settlementId, "settlement_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_SETTLEMENT_NOT_FOUND", "settlement not found"));
        String next = switch (s.status()) {
            case "pending_settlement" -> "deposit_offsetting";
            case "deposit_offsetting" -> "arrears_collecting";
            case "arrears_collecting" -> "settled";
            default -> throw ApiException.conflict("CONTRACT_SETTLEMENT_NOT_ADVANCEABLE",
                    "settlement at " + s.status() + " cannot advance");
        };
        Instant now = Instant.now();
        ContractSettlementRecord moved = repository.saveSettlement(new ContractSettlementRecord(s.id(),
                s.contractId(), s.triggerType(), next, s.settlementItems(), s.disputeReason(), s.resolvedBy(),
                s.resolvedAt(), s.projectId(), s.createdAt(), now));
        if ("settled".equals(next)) {
            finalizeContractAfterSettlement(requireContract(s.contractId()), s.triggerType(), now);
        }
        auditEventService.recordSuccess("contract.settlement_advanced", "contract.settlement.manage",
                "contract_settlement", s.id(), null, actual.idempotencyKey(), Map.of("to", next));
        return moved;
    }

    public PageResponse<ContractSettlementRecord> listSettlements(String contractId) {
        List<ContractSettlementRecord> items = repository.listSettlementsByContract(contractId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /** 到期批转（替代旧 governance ContractExpiryJob 路径）：过期 effective 合同 → expired。系统作业调用，无用户 scope。 */
    public int generateExpiredTransitions(LocalDate asOf) {
        int count = repository.markContractsExpired(asOf);
        if (count > 0) {
            auditEventService.recordSuccess("contract.expired_batch", "contract.lifecycle.activate", "contract",
                    "batch", null, null, Map.of("count", count, "as_of", asOf.toString()));
        }
        return count;
    }

    /**
     * 商户费用续约短信提醒扫描入口。只追加通知 hook：不变更合同状态、不发起续约流程、不写业务表。
     * 由 ContractRenewalNotificationJob 薄 @Scheduled 包装调用，亦供单测直调对拍通知日志。
     */
    public void processMerchantFeeRenewalNotifications(LocalDate asOf, int noticeDays) {
        if (notificationService == null) {
            return;
        }
        LocalDate baseDate = asOf == null ? LocalDate.now(ZoneOffset.UTC) : asOf;
        LocalDate untilDate = baseDate.plusDays(Math.max(0, noticeDays));
        List<ContractRecord> contracts;
        try {
            contracts = repository.listEffectiveContractsExpiringBetween(baseDate, untilDate);
        } catch (RuntimeException exception) {
            log.warn("failed to list contracts for merchant fee renewal notification", exception);
            return;
        }
        for (ContractRecord contract : contracts) {
            notifyMerchantFeeRenewalAfterCommit(contract, baseDate);
        }
    }

    private void notifyMerchantFeeRenewalAfterCommit(ContractRecord contract, LocalDate asOf) {
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()
                    && TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyMerchantFeeRenewalSafely(contract, asOf);
                    }
                });
                return;
            }
            notifyMerchantFeeRenewalSafely(contract, asOf);
        } catch (RuntimeException exception) {
            log.warn(
                    "merchant fee renewal notification registration failed: contract_id={}",
                    contract == null ? null : contract.id(),
                    exception);
        }
    }

    private void notifyMerchantFeeRenewalSafely(ContractRecord contract, LocalDate asOf) {
        if (contract == null) {
            return;
        }
        try {
            notificationService.notify(
                    MERCHANT_FEE_DOMAIN,
                    MERCHANT_FEE_RENEWAL_EVENT,
                    resolveMerchantFeeRecipientPhone(contract.merchantId(), MERCHANT_FEE_RENEWAL_EVENT),
                    MERCHANT_FEE_RENEWAL_TEMPLATE,
                    merchantFeeRenewalParams(contract, asOf),
                    "mfrenewal:" + contract.id() + ":" + contract.expiryDate());
        } catch (RuntimeException exception) {
            log.warn(
                    "merchant fee renewal notification failed: contract_id={}, idempotency_key={}",
                    contract.id(),
                    "mfrenewal:" + contract.id() + ":" + contract.expiryDate(),
                    exception);
        }
    }

    private Map<String, Object> merchantFeeRenewalParams(ContractRecord contract, LocalDate asOf) {
        Map<String, Object> params = new LinkedHashMap<>();
        putIfNotNull(params, "contract_id", contract.id());
        putIfNotNull(params, "merchant_id", contract.merchantId());
        putIfNotNull(params, "project_id", contract.projectId());
        putIfNotNull(params, "effective_date", contract.effectiveDate());
        putIfNotNull(params, "expiry_date", contract.expiryDate());
        if (contract.expiryDate() != null && asOf != null) {
            params.put("days_to_expiry", ChronoUnit.DAYS.between(asOf, contract.expiryDate()));
        }
        return params;
    }

    private String resolveMerchantFeeRecipientPhone(String merchantId, String event) {
        if (merchantFeeRecipientResolver != null) {
            try {
                String resolved = trimToNull(merchantFeeRecipientResolver.resolvePhone(merchantId, event));
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
                    .map(this::trimToNull)
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

    // ============ 配置：审批链 + 到期着色 ============
    public List<ContractApprovalFlowStepRecord> saveApprovalFlow(ContractApprovalFlowRequest request) {
        authorizationService.requirePermission("contract.approval.flow.manage");
        ContractApprovalFlowRequest actual = requireRequest(request);
        if (actual.steps() == null || actual.steps().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "steps is required");
        }
        String projectId = trimToNull(actual.projectId());
        List<ContractApprovalFlowStepRecord> steps = new ArrayList<>();
        for (ContractApprovalFlowRequest.Step s : actual.steps()) {
            steps.add(new ContractApprovalFlowStepRecord(null, null, projectId, s.stageSeq(),
                    requireText(s.stageName(), "stage_name"), requireText(s.approverPermissionCode(), "code"), true));
        }
        repository.saveFlowConfig(projectId, actual.name(), steps);
        auditEventService.recordSuccess("contract.approval_flow_saved", "contract.approval.flow.manage",
                "contract_approval_flow", projectId == null ? "default" : projectId, null, actual.idempotencyKey(),
                Map.of("stage_count", steps.size()));
        return repository.listFlowStepsByProject(projectId);
    }

    public List<ContractApprovalFlowStepRecord> listApprovalFlow(String projectId) {
        return repository.listFlowStepsByProject(trimToNull(projectId));
    }

    public SpaceExpiryColorRuleRecord saveColorRule(SpaceExpiryColorRuleRequest request) {
        authorizationService.requirePermission("contract.space.color.manage");
        SpaceExpiryColorRuleRequest actual = requireRequest(request);
        int days = actual.thresholdDays() == null ? 0 : actual.thresholdDays();
        if (days <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "threshold_days must be positive");
        }
        SpaceExpiryColorRuleRecord saved = repository.saveColorRule(new SpaceExpiryColorRuleRecord(
                "secr_" + UUID.randomUUID(), trimToNull(actual.projectId()),
                requireText(actual.categoryName(), "category_name"), requireText(actual.color(), "color"),
                days, actual.seq() == null ? 0 : actual.seq(), true));
        auditEventService.recordSuccess("contract.color_rule_saved", "contract.space.color.manage",
                "space_expiry_color_rule", saved.id(), null, actual.idempotencyKey(),
                Map.of("threshold_days", days));
        return saved;
    }

    public List<SpaceExpiryColorRuleRecord> listColorRules(String projectId) {
        return repository.listColorRules(trimToNull(projectId));
    }

    /** 铺位平面图数据源：每铺租赁状态 + 套用着色分类（已租按距到期命中最紧一档→即将到期着该色）。 */
    public List<SpaceRentalStatusRecord> spaceRentalStatus(String projectId) {
        String pid = trimToNull(projectId);
        LocalDate asOf = LocalDate.now(java.time.ZoneOffset.UTC);
        List<SpaceExpiryColorRuleRecord> rules = repository.listColorRules(pid).stream()
                .sorted((a, b) -> Integer.compare(a.thresholdDays(), b.thresholdDays())).toList();
        List<SpaceRentalStatusRecord> out = new ArrayList<>();
        for (SpaceRentalStatusRecord raw : repository.listSpaceRentalRaw(pid, asOf)) {
            if (!"let".equals(raw.rentalStatus()) || raw.daysToExpiry() == null) {
                out.add(raw);
                continue;
            }
            SpaceExpiryColorRuleRecord hit = rules.stream()
                    .filter(r -> raw.daysToExpiry() <= r.thresholdDays()).findFirst().orElse(null);
            out.add(hit == null ? raw
                    : new SpaceRentalStatusRecord(raw.spaceId(), raw.spaceName(), "expiring", hit.color(),
                            raw.daysToExpiry()));
        }
        return out;
    }

    // ============ 租金 AR 逐期挂账（用 ContractRentMath：base+递增−免租）============
    /** 生效合同某期挂账：算应收 → 联动 ar_ap_items(receivable)+contract_rent_bills。免租/生效前返回 null。 */
    @Transactional
    public ContractRentBillRecord generateRentBill(String contractId, ContractRentBillRequest request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        ContractRentBillRequest actual = requireRequest(request);
        String period = requireText(actual.period(), "period");
        if (!period.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period must be YYYY-MM");
        }
        ContractRecord c = requireContract(contractId);
        if (!"effective".equals(c.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "only effective contract bills rent");
        }
        if (repository.rentBillExists(contractId, period, "rent")) {
            throw ApiException.conflict("CONTRACT_RENT_ALREADY_BILLED", "rent already billed for " + period);
        }
        long baseRent = ContractRentMath.periodAmountCent(c.rentTerms(), c.effectiveDate(), period);
        long amountCent = resolveBillingRent(c, c.rentTerms() == null ? Map.of() : c.rentTerms(), period, baseRent);
        if (amountCent <= 0) {
            return null;   // 免租期 / 生效前 / 收租方式组合后仍为 0：无应收
        }
        return genFeeBill(c, period, "rent", amountCent, dueDateOf(period), actual.idempotencyKey());
    }

    /**
     * 生成某期全部应收：租金 + 物业费(rent_terms.propertyFeeCent) + 能源费(基非 none 取 rent_terms.energyFeeCent)。
     * 各费目幂等(合同,期,费目)；免租/0 金额跳过。
     * ponytail: 能源费按面积/按用量的金额由能耗域/上游派生后落 rent_terms.energyFeeCent，本处只挂账；不分摊则 0。
     */
    @Transactional
    public List<ContractRentBillRecord> generatePeriodBills(String contractId, ContractRentBillRequest request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        ContractRentBillRequest actual = requireRequest(request);
        String period = requireText(actual.period(), "period");
        if (!period.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period must be YYYY-MM");
        }
        ContractRecord c = requireContract(contractId);
        if (!"effective".equals(c.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_EFFECTIVE", "only effective contract bills");
        }
        return billPeriodFees(c, period, actual.idempotencyKey());
    }

    /**
     * 单期各费目幂等挂账核心（无权限校验，由调用方门控：generatePeriodBills 查 finance.ar_ap.manage，
     * activate 已查 contract.lifecycle.activate）。租金 + 物业费 + 能源费(基非 none)；免租/0/已挂账跳过。
     */
    private List<ContractRentBillRecord> billPeriodFees(ContractRecord c, String period, String idemKey) {
        Map<String, Object> rt = c.rentTerms() == null ? Map.of() : c.rentTerms();
        LocalDate dueDate = dueDateOf(period);
        long baseRent = ContractRentMath.periodAmountCent(rt, c.effectiveDate(), period);
        long rent = resolveBillingRent(c, rt, period, baseRent);
        long property = numLong(rt.get("propertyFeeCent"));
        List<ContractRentBillRecord> out = new ArrayList<>();
        ContractRentBillRecord rb = genFeeBill(c, period, "rent", rent, dueDate, idemKey);
        if (rb != null) {
            out.add(rb);
        }
        ContractRentBillRecord pb = genFeeBill(c, period, "property_fee", property, dueDate, idemKey);
        if (pb != null) {
            out.add(pb);
        }
        // 清单 P1-3 方案B:能源费(含 by_contract 包干固定额)统一由能耗抄表域 EnergyService.settleEnergyFee 出账,
        // 合同域不再挂能源费,消除合同域(contract_energy_fee)×能耗域(energy_fee)对同一户同期双挂账。
        return out;
    }

    /** 单费目挂账：金额≤0 或已挂账则跳过返 null；否则建 ar_ap_items 应收 + contract_rent_bills。 */
    private ContractRentBillRecord genFeeBill(ContractRecord c, String period, String feeType, long amountCent,
            LocalDate dueDate, String idemKey) {
        if (amountCent <= 0 || repository.rentBillExists(c.id(), period, feeType)) {
            return null;
        }
        ContractRentBillRecord bill = repository.insertRentBill(new ContractRentBillRecord(
                "crb_" + UUID.randomUUID(), c.id(), c.merchantId(), c.projectId(), period, feeType, amountCent,
                "arap_" + feeType + "_" + UUID.randomUUID(), Instant.now()), dueDate);
        auditEventService.recordSuccess("contract.fee_billed", "finance.ar_ap.manage", "contract_rent_bill",
                bill.id(), null, idemKey, Map.of("period", period, "fee_type", feeType, "amount_cent", amountCent));
        return bill;
    }

    private LocalDate dueDateOf(String period) {
        String[] ym = period.split("-");
        return LocalDate.of(Integer.parseInt(ym[0]), Integer.parseInt(ym[1]), 1);
    }

    /** 批2b:按合同「收租方式」组合月租(baseRent)与提成。fixed_only/空=纯月租;其余需提成端口(缺则退月租)。 */
    private long resolveBillingRent(ContractRecord c, Map<String, Object> rt, String period, long baseRent) {
        String mode = strVal(rt.get("rentBillingMode"));
        if (mode == null || "fixed_only".equals(mode)) {
            return baseRent;
        }
        long turnover = turnoverRentPort == null ? 0L
                : turnoverRentPort.computePeriodTurnoverRent(c.merchantId(), c.id(), period);
        return switch (mode) {
            case "turnover_only" -> turnover;
            case "higher_of" -> Math.max(baseRent, turnover);
            case "sum" -> baseRent + turnover;
            default -> baseRent;
        };
    }

    /** 每平米生效固化:rentMode=per_sqm 时用绑定铺位登记面积×单价算 baseRentCent,写回 rentTerms 副本;否则原样返回。 */
    private Map<String, Object> resolvePerSqmBaseRent(Map<String, Object> rentTerms, List<String> spaceIds) {
        if (rentTerms == null || !"per_sqm".equals(strVal(rentTerms.get("rentMode")))) {
            return rentTerms;
        }
        long perSqm = numLong(rentTerms.get("perSqmRentCent"));
        if (perSqm <= 0 || platformRepository == null) {
            return rentTerms;
        }
        java.math.BigDecimal totalArea = java.math.BigDecimal.ZERO;
        for (String sid : spaceIds) {
            java.math.BigDecimal a = platformRepository.findSpaceById(sid).map(SpaceRecord::areaSquareMeter).orElse(null);
            if (a != null) {
                totalArea = totalArea.add(a);
            }
        }
        long baseRent = totalArea.multiply(java.math.BigDecimal.valueOf(perSqm))
                .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        Map<String, Object> out = new HashMap<>(rentTerms);
        out.put("baseRentCent", baseRent);
        out.put("resolvedAreaSqm", totalArea.toPlainString());
        return out;
    }

    private String strVal(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private long numLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    // ============ engine helpers ============
    private void materializeStage(String contractId, String changeId,
            List<ContractApprovalFlowStepRecord> steps, int stageSeq) {
        Instant now = Instant.now();
        for (ContractApprovalFlowStepRecord step : steps) {
            if (step.stageSeq() == stageSeq) {
                repository.saveApproval(new ContractApprovalRecord("capr_" + UUID.randomUUID(), contractId, changeId,
                        stageSeq, step.approverPermissionCode(), null, "pending", null, null, now));
            }
        }
    }

    private enum StageOutcome { ADVANCED, COMPLETED, REJECTED }

    /**
     * 分组并行链共享引擎：记录一个并行槽决议（合同新签 changeId=null 或合同变更 changeId 非空走同一引擎）。
     * 当前阶段全 approved → 有下一阶段则物化、否则 COMPLETED；任一 reject → REJECTED。
     */
    private StageOutcome recordSlotDecision(String contractId, String changeId,
            List<ContractApprovalRecord> approvals, String projectId, String code, String decision, String comment) {
        int stage = currentPendingStage(approvals);
        ContractApprovalRecord slot = approvals.stream()
                .filter(a -> a.stageSeq() == stage && code.equals(a.approverPermissionCode())
                        && "pending".equals(a.decision()))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("CONTRACT_APPROVAL_SLOT_NOT_PENDING",
                        "no pending approval for " + code + " at current stage"));
        Instant now = Instant.now();
        repository.saveApproval(new ContractApprovalRecord(slot.id(), slot.contractId(), slot.changeId(),
                slot.stageSeq(), code, authorizationService.currentActor().id(),
                "reject".equals(decision) ? "rejected" : "approved", comment, now, slot.createdAt()));
        if ("reject".equals(decision)) {
            return StageOutcome.REJECTED;
        }
        List<ContractApprovalRecord> after = changeId == null
                ? repository.listApprovalsByContract(contractId) : repository.listApprovalsByChange(changeId);
        if (stageAllApproved(after, stage)) {
            List<ContractApprovalFlowStepRecord> steps = repository.listActiveFlowSteps(projectId);
            Integer next = nextStageSeq(steps, stage);
            if (next != null) {
                materializeStage(contractId, changeId, steps, next);
                return StageOutcome.ADVANCED;
            }
            return StageOutcome.COMPLETED;
        }
        return StageOutcome.ADVANCED;
    }

    private int currentPendingStage(List<ContractApprovalRecord> approvals) {
        return approvals.stream().filter(a -> "pending".equals(a.decision()))
                .mapToInt(ContractApprovalRecord::stageSeq).min()
                .orElseThrow(() -> ApiException.conflict("CONTRACT_NO_PENDING_STAGE", "no pending approval stage"));
    }

    private boolean stageAllApproved(List<ContractApprovalRecord> approvals, int stage) {
        List<ContractApprovalRecord> inStage = approvals.stream().filter(a -> a.stageSeq() == stage).toList();
        return !inStage.isEmpty() && inStage.stream().allMatch(a -> "approved".equals(a.decision()));
    }

    private Integer nextStageSeq(List<ContractApprovalFlowStepRecord> steps, int currentStage) {
        return steps.stream().mapToInt(ContractApprovalFlowStepRecord::stageSeq)
                .filter(s -> s > currentStage).min().stream().boxed().findFirst().orElse(null);
    }

    private Map<String, Object> buildSnapshot(ContractRecord c) {
        // 固化 4 项：①租金规则 ②物业费+能源费口径 ③分成/抽成 ④保证金+违约金
        Map<String, Object> snap = new HashMap<>();
        snap.put("rentTerms", c.rentTerms() == null ? Map.of() : c.rentTerms());
        snap.put("energyFeeBasis", c.energyFeeBasis());
        snap.put("commissionMode", c.commissionMode());
        snap.put("depositCent", c.depositCent());
        snap.put("penaltyTerms", c.penaltyTerms() == null ? Map.of() : c.penaltyTerms());
        snap.put("frozenAt", Instant.now().toString());
        return snap;
    }

    /** 应用已批变更：可改条款的（面积/租金/经营范围）改 contract 并重固化；续租置续签中。退租/提前终止留待 settle。 */
    private Map<String, Object> applyChange(ContractRecord contract, ContractChangeRecord change) {
        Map<String, Object> payload = change.payload() == null ? Map.of() : change.payload();
        ContractRecord updated = contract;
        switch (change.changeType()) {
            case "rent_change", "area_change", "business_scope_change" -> {
                Map<String, Object> newTerms = new HashMap<>(contract.rentTerms() == null ? Map.of()
                        : contract.rentTerms());
                newTerms.putAll(payload); // payload 覆盖对应条款（面积/租金/经营范围细节）
                updated = repository.saveContract(new ContractRecord(contract.id(), contract.merchantId(),
                        contract.contractSubjectId(), contract.operationSubjectId(), contract.settlementSubjectId(),
                        contract.effectiveDate(), contract.expiryDate(), newTerms, contract.commissionMode(),
                        contract.energyFeeBasis(), contract.depositCent(), contract.penaltyTerms(),
                        contract.settlementRules(), buildSnapshot(withRentTerms(contract, newTerms)),
                        contract.shareholderRatioForReport(), contract.attachmentIds(), "effective",
                        contract.projectId(), null, null, contract.renewedFromContractId(), contract.createdAt(),
                        Instant.now()));
            }
            case "renewal" -> updated = repository.saveContract(withStatus(contract, "renewing"));
            default -> { /* move_out / early_termination：实际终止走 settle */ }
        }
        return updated.snapshot();
    }

    private List<Map<String, Object>> runSettlementRules(ContractRecord contract) {
        // 逐例按合同 settlement_rules 顺序产出结算项 + 真实金额：保证金抵扣(deposit)/违约金(penalty)/欠费追缴(未付AR)。
        List<Map<String, Object>> rules = contract.settlementRules() == null ? List.of() : contract.settlementRules();
        long deposit = contract.depositCent() == null ? 0L : contract.depositCent();
        long penalty = numLong(contract.penaltyTerms() == null ? null : contract.penaltyTerms().get("amountCent"));
        long arrears = repository.sumUnpaidArForContract(contract.id());
        List<Map<String, Object>> items = new ArrayList<>();
        int order = 1;
        for (Map<String, Object> rule : rules) {
            Map<String, Object> item = new HashMap<>(rule);
            long amount = switch (String.valueOf(rule.get("item"))) {
                case "deposit_offset" -> deposit;
                case "penalty" -> penalty;
                case "arrears", "unpaid_fee" -> arrears;
                default -> numLong(rule.get("amountCent"));
            };
            item.put("order", order++);
            item.put("amountCent", amount);
            item.put("status", "applied");
            items.add(item);
        }
        return items;
    }

    private void finalizeContractAfterSettlement(ContractRecord contract, String trigger, Instant now) {
        String status = "expiry".equals(trigger) ? "expired" : "terminated";
        repository.saveContract(new ContractRecord(contract.id(), contract.merchantId(), contract.contractSubjectId(),
                contract.operationSubjectId(), contract.settlementSubjectId(), contract.effectiveDate(),
                contract.expiryDate(), contract.rentTerms(), contract.commissionMode(), contract.energyFeeBasis(),
                contract.depositCent(), contract.penaltyTerms(), contract.settlementRules(), contract.snapshot(),
                contract.shareholderRatioForReport(), contract.attachmentIds(), status, contract.projectId(),
                now, "settled via " + trigger, contract.renewedFromContractId(), contract.createdAt(), now));
    }

    // ============ small helpers ============
    private ContractRecord withStatus(ContractRecord c, String status) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), c.rentTerms(), c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), c.attachmentIds(), status, c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), Instant.now());
    }

    private ContractRecord withAttachments(ContractRecord c, List<String> attachmentIds) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), c.rentTerms(), c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), attachmentIds, c.lifecycleStatus(), c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), Instant.now());
    }

    private ContractRecord withRentTerms(ContractRecord c, Map<String, Object> rentTerms) {
        return new ContractRecord(c.id(), c.merchantId(), c.contractSubjectId(), c.operationSubjectId(),
                c.settlementSubjectId(), c.effectiveDate(), c.expiryDate(), rentTerms, c.commissionMode(),
                c.energyFeeBasis(), c.depositCent(), c.penaltyTerms(), c.settlementRules(), c.snapshot(),
                c.shareholderRatioForReport(), c.attachmentIds(), c.lifecycleStatus(), c.projectId(), c.terminatedAt(),
                c.terminationReason(), c.renewedFromContractId(), c.createdAt(), c.updatedAt());
    }

    private ContractRecord requireContract(String contractId) {
        return repository.findContractById(requireText(contractId, "contract_id"))
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "contract not found"));
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

    private LocalDate requireDate(LocalDate value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }

    private String trimToNull(String value) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? null : t;
    }
}
