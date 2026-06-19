package com.complex.platform.gate;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * R5 T3 合同/基础定义准入闸：动作执行前查 supervisor_review_policies（启用）判定 allow/warn/block。
 *
 * <p>判定分两步：</p>
 * <ol>
 *   <li><b>命中判定</b>（{@link #isTriggered}）：按 policy.trigger_mode 对真实数据求值，决定该受控动作此刻是否“触发”。</li>
 *   <li><b>结果处置</b>：仅在触发时按 outcome 处置；未触发 → 放行（不留痕、不阻断）。</li>
 * </ol>
 *
 * <h3>trigger_mode 真实判定（待确认 #4）</h3>
 * <ul>
 *   <li><b>always</b>：恒命中（维持现状）。</li>
 *   <li><b>expired</b>：当前 actor 的商户无生效合同（lifecycle_status='approved' 且日期覆盖今天的合同不存在）→ 命中。
 *       数据源 contracts（V005，字段 merchant_id/effective_date/expiry_date/lifecycle_status）。</li>
 *   <li><b>permission_insufficient</b>：currentActorLevelRank() &lt; policy.requiredLevel → 命中。
 *       数据源 staff_levels（经 AuthorizationService）。</li>
 *   <li><b>qualification_abnormal</b>：当前 actor 的商户存在已过期的 active 合规资质项 → 命中。
 *       数据源 compliance_expiry_items（V028，subject_type merchant/supplier、valid_to、status）。</li>
 *   <li><b>over_contract_scope</b>：<b>TODO</b>——contracts 表无 allowed_business_scope（经营范围）字段，
 *       且无 action→业务域→合同经营范围映射数据，暂无法真实判定 → 保守放行（不阻断）。</li>
 *   <li><b>over_space</b>：<b>TODO</b>——无 对象→空间→合同租赁空间 映射（spaces.merchant_id 仅到商户，
 *       无 contract_id；object_id→space 亦无通用映射）→ 保守放行（不阻断）。</li>
 * </ul>
 *
 * <h3>异常口径</h3>
 * 命中判定读取数据异常（DB 故障等）→ <b>fail-open 保守放行</b>（不误伤业务），并写
 * {@code admission.eval_skipped} 留痕（reason=eval_error），与 AdmissionGuard 既有“无策略即放行”的不误伤风格一致。
 */
@Component
public class AdmissionGuard {

    private final AdmissionPolicyRepository policyRepository;
    private final SupervisorReviewService supervisorReviewService;
    private final AuditEventService auditEventService;
    private final AdmissionFactRepository factRepository;
    private final AuthorizationService authorizationService;

    public AdmissionGuard(
            AdmissionPolicyRepository policyRepository,
            SupervisorReviewService supervisorReviewService,
            AuditEventService auditEventService,
            AdmissionFactRepository factRepository,
            AuthorizationService authorizationService
    ) {
        this.policyRepository = policyRepository;
        this.supervisorReviewService = supervisorReviewService;
        this.auditEventService = auditEventService;
        this.factRepository = factRepository;
        this.authorizationService = authorizationService;
    }

    public void check(String actionType, String objectType, String objectId) {
        if (actionType == null || actionType.isBlank()) {
            return;
        }
        Optional<AdmissionPolicyRecord> match =
                policyRepository.findEnabledPolicy(actionType, objectType == null ? "" : objectType);
        if (match.isEmpty()) {
            return; // 无策略=放行
        }
        AdmissionPolicyRecord policy = match.get();
        if (!isTriggered(policy, actionType, objectType, objectId)) {
            return; // 命中判定未触发=放行
        }
        switch (policy.outcome()) {
            case "allow" -> {
                // 放行，无留痕（与无策略一致的轻量路径）。
            }
            case "warn" -> {
                auditEventService.recordSuccess(
                        "admission.warned",
                        "admission.review",
                        "supervisor_review",
                        objectId == null ? actionType : objectId,
                        null,
                        null,
                        Map.of("action_type", actionType, "object_type", objectType == null ? "" : objectType,
                                "policy_code", policy.policyCode(), "trigger_mode", policy.triggerMode())
                );
            }
            case "block" -> {
                SupervisorReviewDecisionRecord pending = supervisorReviewService.blockForAdmission(
                        policy.policyCode(),
                        actionType,
                        objectType,
                        objectId,
                        policy.approvalLevel(),
                        policy.requiredLevel()
                );
                throw new ApiException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "ADMISSION_BLOCKED",
                        "action blocked pending supervisor review: " + pending.reviewId());
            }
            default -> {
                // 未知 outcome 不应出现（DB CHECK 约束），保守放行。
            }
        }
    }

    /**
     * 按 policy.trigger_mode 对真实数据求值：是否触发受控处置。
     * 求值异常一律 fail-open（返回 false=放行）并留痕，不让数据故障误伤业务。
     */
    private boolean isTriggered(AdmissionPolicyRecord policy, String actionType, String objectType, String objectId) {
        String triggerMode = policy.triggerMode() == null ? "" : policy.triggerMode();
        try {
            return switch (triggerMode) {
                case "always" -> true;
                case "expired" -> authorizationService.currentMerchantIdQuietly()
                        .map(factRepository::isContractExpiredForMerchant)
                        .orElse(false); // 无商户上下文（如平台管理员/无 merchant 员工）→ 不命中，放行
                case "qualification_abnormal" -> authorizationService.currentMerchantIdQuietly()
                        .map(factRepository::isQualificationAbnormalForMerchant)
                        .orElse(false);
                case "permission_insufficient" -> authorizationService.currentActorLevelRank() < policy.requiredLevel();
                // over_contract_scope / over_space：缺判定数据（见类注释 TODO）→ 保守放行，不阻断。
                case "over_contract_scope", "over_space" -> false;
                default -> false; // 未知 trigger_mode（DB CHECK 约束兜底）→ 放行
            };
        } catch (RuntimeException exception) {
            // fail-open：求值异常不误伤业务，放行并留痕。
            auditEventService.recordSuccess(
                    "admission.eval_skipped",
                    "admission.review",
                    "supervisor_review",
                    objectId == null ? actionType : objectId,
                    null,
                    null,
                    Map.of("action_type", actionType, "object_type", objectType == null ? "" : objectType,
                            "policy_code", policy.policyCode(), "trigger_mode", triggerMode,
                            "reason", "eval_error")
            );
            return false;
        }
    }
}
