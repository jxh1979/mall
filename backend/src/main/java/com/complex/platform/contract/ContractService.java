package com.complex.platform.contract;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractApproveRequest;
import com.complex.platform.contract.dto.ContractChangeApproveRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.SpaceExpiryColorRuleRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同租赁全生命周期（V162 重建，据 contract-lifecycle-prd-v1.0 V1.1 + M1 §2 + 用户裁决）。
 * 核心：起草 → 提交 → 分组并行审批（可配链，阶段内会签、阶段间串行）→ 生效固化快照 + 绑铺位 → 变更（6类）→ 退租逐例结算 + 争议挂起。
 */
@Service
public class ContractService {

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

    public ContractService(ContractRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
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
        String commissionMode = trimToNull(actual.commissionMode());
        if (commissionMode != null && !COMMISSION_MODES.contains(commissionMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "commission_mode is invalid");
        }
        String energyBasis = trimToNull(actual.energyFeeBasis()) == null ? "none" : actual.energyFeeBasis().trim();
        if (!ENERGY_BASES.contains(energyBasis)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "energy_fee_basis is invalid");
        }
        Instant now = Instant.now();
        ContractRecord saved = repository.saveContract(new ContractRecord(
                "ctr_" + UUID.randomUUID(),
                requireText(actual.merchantId(), "merchant_id"),
                requireText(actual.contractSubjectId(), "contract_subject_id"),
                trimToNull(actual.operationSubjectId()),
                requireText(actual.settlementSubjectId(), "settlement_subject_id"),
                requireDate(actual.effectiveDate(), "effective_date"),
                requireDate(actual.expiryDate(), "expiry_date"),
                actual.rentTerms() == null ? Map.of() : Map.copyOf(actual.rentTerms()),
                commissionMode, energyBasis, actual.depositCent(),
                actual.penaltyTerms() == null ? Map.of() : Map.copyOf(actual.penaltyTerms()),
                actual.settlementRules() == null ? List.of() : List.copyOf(actual.settlementRules()),
                null, null,
                actual.attachmentIds() == null ? List.of() : List.copyOf(actual.attachmentIds()),
                "drafting", trimToNull(actual.projectId()), null, null, null, now, now));
        auditEventService.recordSuccess("contract.drafted", "contract.draft.manage", "contract", saved.id(),
                null, actual.idempotencyKey(), Map.of("merchant_id", saved.merchantId()));
        return saved;
    }

    // ============ 提交 + 分组并行审批 ============
    /** 提交审批：按该综合体生效链物化第一阶段并行审批人，合同→待确认。 */
    @Transactional
    public ContractRecord submit(String contractId, String idempotencyKey) {
        authorizationService.requirePlatformScope();
        ContractRecord contract = requireContract(contractId);
        if (!"drafting".equals(contract.lifecycleStatus())) {
            throw ApiException.conflict("CONTRACT_NOT_DRAFTING", "only drafting contract can be submitted");
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
        ContractRecord effective = repository.saveContract(new ContractRecord(
                contract.id(), contract.merchantId(), contract.contractSubjectId(), contract.operationSubjectId(),
                contract.settlementSubjectId(), contract.effectiveDate(), contract.expiryDate(), contract.rentTerms(),
                contract.commissionMode(), contract.energyFeeBasis(), contract.depositCent(), contract.penaltyTerms(),
                contract.settlementRules(), buildSnapshot(contract), contract.shareholderRatioForReport(),
                contract.attachmentIds(), "effective", contract.projectId(), null, null,
                contract.renewedFromContractId(), contract.createdAt(), Instant.now()));
        auditEventService.recordSuccess("contract.activated", "contract.lifecycle.activate", "contract", contractId,
                null, actual.idempotencyKey(), Map.of("space_count", spaceIds.size()));
        return effective;
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
        ContractChangeRecord saved = repository.saveChange(new ContractChangeRecord(
                "cch_" + UUID.randomUUID(), contractId, type,
                actual.payload() == null ? Map.of() : Map.copyOf(actual.payload()), "pending", null,
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
        String status = dispute != null ? "disputed" : "settled";
        List<Map<String, Object>> items = dispute != null ? List.of() : runSettlementRules(contract);
        ContractSettlementRecord settlement = repository.saveSettlement(new ContractSettlementRecord(
                "cst_" + UUID.randomUUID(), contractId, trigger, status, items, dispute, null, null,
                contract.projectId(), now, now));
        if (dispute == null) {
            finalizeContractAfterSettlement(contract, trigger, now);
        }
        auditEventService.recordSuccess("contract.settled", "contract.settlement.manage", "contract_settlement",
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
        ContractSettlementRecord settled = repository.saveSettlement(new ContractSettlementRecord(settlement.id(),
                settlement.contractId(), settlement.triggerType(), "settled", runSettlementRules(contract),
                settlement.disputeReason(), authorizationService.currentActor().id(), now, settlement.projectId(),
                settlement.createdAt(), now));
        finalizeContractAfterSettlement(contract, settlement.triggerType(), now);
        auditEventService.recordSuccess("contract.dispute_resolved", "contract.settlement.dispute.resolve",
                "contract_settlement", settlement.id(), null, actual.idempotencyKey(), Map.of());
        return settled;
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
        // 逐例按合同 settlement_rules 顺序产出结算项（实际金额计算连财务，本切片留规则回执）。
        // ponytail: 金额结算接 ar_ap_items 在「合同↔财务」切片做；此处逐例回执保证顺序与项可见、可审计。
        List<Map<String, Object>> rules = contract.settlementRules() == null ? List.of() : contract.settlementRules();
        List<Map<String, Object>> items = new ArrayList<>();
        int order = 1;
        for (Map<String, Object> rule : rules) {
            Map<String, Object> item = new HashMap<>(rule);
            item.put("order", order++);
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
