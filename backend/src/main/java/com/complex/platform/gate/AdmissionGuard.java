package com.complex.platform.gate;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * R5 T3 合同/基础定义准入闸：动作执行前查 supervisor_review_policies（启用）判定 allow/warn/block。
 * <ul>
 *   <li>无匹配策略 → 放行（不误伤）。</li>
 *   <li>allow → 放行。</li>
 *   <li>warn → 放行 + 写 warned 留痕 + 发 admission.warned。</li>
 *   <li>block → 走 supervisor_review 建 pending + 发 admission.blocked + 抛 ADMISSION_BLOCKED(reviewId)。</li>
 * </ul>
 * trigger_mode 命中判定本期按策略 outcome 直接生效（命中数据源 contracts/spaces/资质 为 T10/T11 逐域接线，
 * 当前 always 类策略恒命中，其余触发模式按"策略已启用即视为命中受控动作"处理，宁严勿漏，fail-closed 倾向）。
 */
@Component
public class AdmissionGuard {

    private final AdmissionPolicyRepository policyRepository;
    private final SupervisorReviewService supervisorReviewService;
    private final AuditEventService auditEventService;

    public AdmissionGuard(
            AdmissionPolicyRepository policyRepository,
            SupervisorReviewService supervisorReviewService,
            AuditEventService auditEventService
    ) {
        this.policyRepository = policyRepository;
        this.supervisorReviewService = supervisorReviewService;
        this.auditEventService = auditEventService;
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
                                "policy_code", policy.policyCode())
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
}
