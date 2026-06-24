package com.complex.platform.finance;

import org.springframework.stereotype.Service;

/**
 * 退款分级授权引擎（审计 A2 升级，用户拍板「规则自动退 / 员工额度自助 / 超额复核」，适用所有退款）。
 *
 * <p>把一笔退款判到三档之一：
 * <ol>
 *   <li>{@link RefundTier#RULE_AUTO}：规则既定型（调用方标明 autoRuleType 且该规则启用，如到期未用退）→ 系统实时全自动退、零人工。</li>
 *   <li>{@link RefundTier#WITHIN_LIMIT}：金额 ≤ 发起人在该域的自助额度 → 员工当场自助、自动完成。</li>
 *   <li>{@link RefundTier#REVIEW_REQUIRED}：超额度/无额度 → 进人工复核（复核时强制审批人≠发起人，由调用方落实，见 Stage B）。</li>
 * </ol>
 *
 * <p>默认（无任何额度/规则配置）→ 一律 REVIEW_REQUIRED，最安全（不自动放行任何退款）。
 * 额度查找：先精确域，再回退 'all' 域。
 */
@Service
public class RefundAuthorizationService {

    public enum RefundTier { RULE_AUTO, WITHIN_LIMIT, REVIEW_REQUIRED }

    private final RefundAuthorizationRepository repository;

    public RefundAuthorizationService(RefundAuthorizationRepository repository) {
        this.repository = repository;
    }

    /**
     * @param refundDomain  退款域：ticket/bar/finance
     * @param amountCent    退款金额（分）
     * @param actorId       发起人 id（可空：无上下文/系统发起 → 不享额度自助）
     * @param autoRuleType  规则既定型提示（可空；非空且该规则启用 → RULE_AUTO，如 "expired_unused"）
     */
    public RefundTier classify(String refundDomain, long amountCent, String actorId, String autoRuleType) {
        if (autoRuleType != null && !autoRuleType.isBlank()
                && (repository.isAutoRuleEnabled(refundDomain, autoRuleType)
                        || repository.isAutoRuleEnabled("all", autoRuleType))) {
            return RefundTier.RULE_AUTO;
        }
        if (actorId != null && !actorId.isBlank()) {
            long limit = repository.findActiveLimit("staff", actorId, refundDomain)
                    .or(() -> repository.findActiveLimit("staff", actorId, "all"))
                    .map(RefundAuthorityLimitRecord::limitAmountCent)
                    .orElse(-1L);
            if (limit >= 0 && amountCent <= limit) {
                return RefundTier.WITHIN_LIMIT;
            }
        }
        return RefundTier.REVIEW_REQUIRED;
    }
}
