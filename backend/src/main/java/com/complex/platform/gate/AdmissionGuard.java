package com.complex.platform.gate;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 *   <li><b>over_contract_scope</b>：从 actionType 第一段取业务域（bar.bill_waiver→bar /
 *       amusement.reopen→amusement / ticketing.special_release→ticketing / finance.*→finance）。
 *       查 actor 商户的生效合同（approved + 日期覆盖今天）的 allowed_business_scope（V080），
 *       不含该业务域 → 命中；scope 为空 [] → 视为“未配置=不限”放行（保守不误伤，可后台收紧）；
 *       无生效合同 → 命中（与 expired 语义一致）。数据源 contracts（V010 + V080）。</li>
 *   <li><b>over_space</b>：先按 objectType 解析 objectId→space（V084 扩大覆盖面：
 *       {@code merchant_service_work_order}→work_orders.space_id（V022）；{@code bar_bill}→
 *       bar_bills.table_id→bar_tables.space_id；{@code inspection}/{@code amusement_maintenance_record}/
 *       {@code amusement_incident}/{@code amusement_group_booking}→各自 project_id→amusement_projects.space_id），
 *       再查该 space 是否在 actor 商户生效合同的 contract_spaces（V080）租赁内，不在 → 命中。
 *       objectType 不支持解析 / 解析不到 space → 放行（不阻断）。数据源 contract_spaces + contracts。</li>
 * </ul>
 *
 * <h3>异常口径（C3 用户裁决：所有 trigger 统一 fail-closed）</h3>
 * <ul>
 *   <li><b>任何 trigger 求值异常</b>（DB 故障等）→ <b>统一 fail-closed 保守进复核</b>：与终端开关/人脸闸
 *       fail-closed 口径一致，按策略 approval_level 建 pending 复核并写 {@code admission.eval_failed_closed}
 *       留痕（reason=eval_error），由上一级/上两级/最高权限放行。包括 expired / qualification_abnormal /
 *       permission_insufficient / always 等非越界类——此前对非越界类 fail-open 放行（admission.eval_skipped），
 *       C3 后一并改为 fail-closed（DB 抖动期相关受控动作被暂时拦截，可用性略降，安全优先）。</li>
 *   <li><b>over_space 无法解析到 space</b>（resolveSpaceId 返回 empty）→ fail-closed 进复核（同上）。</li>
 *   <li><b>无商户上下文</b>（平台管理员 / 无 merchant 员工）对 over_* → 仍放行（合法越权角色，不误伤运营）。</li>
 * </ul>
 */
@Component
public class AdmissionGuard {

    private final JdbcAdmissionPolicyRepository policyRepository;
    private final SupervisorReviewService supervisorReviewService;
    private final AuditEventService auditEventService;
    private final JdbcAdmissionFactRepository factRepository;
    private final AuthorizationService authorizationService;
    // XM-M002-J：拦截复核单走的独立事务（REQUIRES_NEW）。经 setter 可选注入（同 IdempotencyService），单测 new 构造不传则直接调用。
    private TransactionTemplate independentTransaction;
    // XM-ONESHOT-RETURN B01 审查修正（C3「任何 trigger 求值异常 → 统一 fail-closed、按策略建 pending 复核」）：已在事务里时求值走保存点（NESTED），
    // 求值 SQL 报错只回滚到保存点，外层事务仍可继续认重放、消费放行单、建复核单或带警示放行。
    private TransactionTemplate savepoint;

    public AdmissionGuard(
            JdbcAdmissionPolicyRepository policyRepository,
            SupervisorReviewService supervisorReviewService,
            AuditEventService auditEventService,
            JdbcAdmissionFactRepository factRepository,
            AuthorizationService authorizationService
    ) {
        this.policyRepository = policyRepository;
        this.supervisorReviewService = supervisorReviewService;
        this.auditEventService = auditEventService;
        this.factRepository = factRepository;
        this.authorizationService = authorizationService;
    }

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        if (transactionManager == null) {
            this.independentTransaction = null;
            return;
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.independentTransaction = template;
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        this.savepoint = nested;
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
        if ("block".equals(policy.outcome())) {
            // M007-A-01（PGM-1.2 第 5 步、PGM-1.7.1 逐笔放行）：同对象已批准且未放行的复核单 → 消费一次并放行本次动作。
            // XM-ONESHOT-RETURN B01（治理地图第 303 行「同一个操作被重复提交，系统只认一次」）：本请求正是用掉放行单的那次成功请求的重放
            // → 按「只能重放」放行（幂等层返回第一次的结果），不再消费别的放行单、不建复核单；消费落空后再认一次——同键并发的后一笔
            // 在放行单行锁上等前一笔提交，提交后才看得到那次放行。
            var actor = authorizationService.currentActor();
            String actorId = actor == null ? null : actor.id();
            if (supervisorReviewService.admitReplayOfReleasingRequest(actionType, objectType, objectId, actorId)
                    || supervisorReviewService.consumeApprovedRelease(actionType, objectType, objectId, actorId).isPresent()
                    || supervisorReviewService.admitReplayOfReleasingRequest(actionType, objectType, objectId, actorId)) {
                return;
            }
        }
        switch (policy.outcome()) {
            case "allow" -> {
                // 放行，无留痕（与无策略一致的轻量路径）。
            }
            case "warn" -> {
                auditEventService.recordSuccessIndependently(
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
            case "block" -> blockToReview(policy, actionType, objectType, objectId);
            default -> {
                // 未知 outcome 不应出现（DB CHECK 约束），保守放行。
            }
        }
    }

    /**
     * block 处置：建 pending supervisor 复核并抛 ADMISSION_BLOCKED。over_* fail-closed（求值异常 /
     * over_space 不可解析）亦经此进复核，由上一级/上两级/最高权限放行。
     *
     * <p>XM-M002-J：复核单（含 admission.blocked 事件）在独立事务里建立并提交。随后抛出的 409 让调用方事务或
     * 单次授权请求事务回滚时，提示语里的复核单号在库里仍然存在，主管能看到并逐笔放行（PGM-1.2 第 5 步、PGM-1.7.1）。
     */
    private void blockToReview(AdmissionPolicyRecord policy, String actionType, String objectType, String objectId) {
        Supplier<SupervisorReviewDecisionRecord> block = () -> supervisorReviewService.blockForAdmission(
                policy.policyCode(),
                actionType,
                objectType,
                objectId,
                policy.approvalLevel(),
                policy.requiredLevel()
        );
        SupervisorReviewDecisionRecord pending = independentTransaction == null
                ? block.get()
                : independentTransaction.execute(status -> block.get());
        throw new ApiException(
                org.springframework.http.HttpStatus.CONFLICT,
                "ADMISSION_BLOCKED",
                "操作已被拦截，等待主管复核，复核单号：" + pending.reviewId());
    }


    /**
     * 按 policy.trigger_mode 对真实数据求值：是否触发受控处置（返回 true=触发，按 outcome 处置）。
     * P0 异常口径：over_contract_scope / over_space 越界类求值异常或 over_space 不可解析 → fail-closed
     * （返回 true，保守进复核）并留痕；其余非越界类求值异常 → fail-open（返回 false=放行）并留痕。
     */
    private boolean isTriggered(AdmissionPolicyRecord policy, String actionType, String objectType, String objectId) {
        String triggerMode = policy.triggerMode() == null ? "" : policy.triggerMode();
        try {
            BooleanSupplier evaluate = () -> switch (triggerMode) {
                case "always" -> true;
                case "expired" -> authorizationService.currentMerchantIdQuietly()
                        .map(factRepository::isContractExpiredForMerchant)
                        .orElse(false); // 无商户上下文（如平台管理员/无 merchant 员工）→ 不命中，放行
                case "qualification_abnormal" -> authorizationService.currentMerchantIdQuietly()
                        .map(factRepository::isQualificationAbnormalForMerchant)
                        .orElse(false);
                case "permission_insufficient" -> authorizationService.currentActorLevelRank() < policy.requiredLevel();
                case "over_contract_scope" -> authorizationService.currentMerchantIdQuietly()
                        .map(merchantId -> factRepository.isOverContractScopeForMerchant(
                                merchantId, businessDomainOf(actionType)))
                        .orElse(false); // 无商户上下文（平台管理员等）→ 放行
                case "over_space" -> {
                    Optional<String> merchantId = authorizationService.currentMerchantIdQuietly();
                    if (merchantId.isEmpty()) {
                        yield false; // 无商户上下文（平台管理员等）→ 放行，不误伤
                    }
                    Optional<String> spaceId = factRepository.resolveSpaceId(objectType, objectId);
                    if (spaceId.isEmpty()) {
                        // P0 fail-closed：配置了 over_space 却解析不到 space（对象无空间映射 / 行缺失 /
                        // space 为空）→ 无法确认"在租赁内"，保守命中进复核，消除原 silent pass。
                        auditEventService.recordSuccessIndependently(
                                "admission.space_unresolved_closed",
                                "admission.review",
                                "supervisor_review",
                                objectId == null ? actionType : objectId,
                                null,
                                null,
                                Map.of("action_type", actionType,
                                        "object_type", objectType == null ? "" : objectType,
                                        "policy_code", policy.policyCode(), "trigger_mode", triggerMode,
                                        "reason", "space_unresolvable")
                        );
                        yield true;
                    }
                    yield factRepository.isOverSpaceForMerchant(merchantId.get(), spaceId.get());
                }
                default -> false; // 未知 trigger_mode（DB CHECK 约束兜底）→ 放行
            };
            return savepoint == null || !TransactionSynchronizationManager.isActualTransactionActive()
                    ? evaluate.getAsBoolean()
                    : Boolean.TRUE.equals(savepoint.execute(status -> evaluate.getAsBoolean()));
        } catch (RuntimeException exception) {
            // C3（用户裁决：统一 fail-closed）：任何 trigger 的求值异常（DB 故障等）一律保守进复核、不放行，
            // 与终端开关/人脸闸 fail-closed 口径一致；非越界类（合同过期/资质异常/权限不足/职责分离等）亦不再
            // fail-open 放行。代价：DB 抖动期相关受控动作被暂时拦截（可用性略降，安全优先），由审批层级放行。
            auditEventService.recordSuccessIndependently(
                    "admission.eval_failed_closed",
                    "admission.review",
                    "supervisor_review",
                    objectId == null ? actionType : objectId,
                    null,
                    null,
                    Map.of("action_type", actionType, "object_type", objectType == null ? "" : objectType,
                            "policy_code", policy.policyCode(), "trigger_mode", triggerMode,
                            "reason", "eval_error")
            );
            return true;
        }
    }

    /** 取 actionType 第一段为业务域：bar.bill_waiver→bar、amusement.reopen→amusement、finance.*→finance。 */
    private static String businessDomainOf(String actionType) {
        if (actionType == null) {
            return "";
        }
        int dot = actionType.indexOf('.');
        return dot < 0 ? actionType : actionType.substring(0, dot);
    }
}
