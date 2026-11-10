package com.complex.platform.access;

// 文件版本：V0.03
import com.complex.platform.common.api.ApiException;
import java.util.Map;
import java.util.Set;

/**
 * M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。
 * 第十八批 AD 段第 204 条 204-R1：审批通用时限改存 approval_deadline_settings（按审批类型），V617 已移走该行的 deadlineWorkingDays 键。
 */
public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
    public AccessGrantPolicy {
        if (windowDays == null || positionThreshold == null || userThreshold == null
                || riskReviews == null || !riskReviews.keySet().equals(Set.of("low", "medium", "high", "critical"))
                || riskReviews.values().stream().anyMatch(java.util.Objects::isNull)) {
            throw ApiException.badRequest("INVALID_GRANT_POLICY", "须配置时间窗、频繁阈值及全部既有风险等级的审批规则");
        }
        riskReviews = Map.copyOf(riskReviews);
    }

    public record BoundedInt(int value, int min, int max) {
        public BoundedInt {
            if (min < 1 || max < min || value < min || value > max) {
                throw ApiException.badRequest("GRANT_POLICY_OUT_OF_BOUNDS", "配置值须在有效的正整数上下限内");
            }
        }
        int checked(int candidate) {
            if (candidate < min || candidate > max) {
                throw ApiException.badRequest("GRANT_POLICY_OUT_OF_BOUNDS", "查询时间窗超出后台配置的上下限");
            }
            return candidate;
        }
    }

    public record ReviewRule(String approvalLevel, int requiredLevel) {
        public ReviewRule {
            if (!Set.of("one_level_up", "two_levels_up", "top_authority").contains(approvalLevel)
                    || requiredLevel < 1 || requiredLevel > 3
                    || ("top_authority".equals(approvalLevel) && requiredLevel != 3)) {
                throw ApiException.badRequest("INVALID_GRANT_REVIEW_POLICY", "审批策略须使用既有审批层级，最高权限须为第三级");
            }
        }
    }
    public record Update(String idempotencyKey, String operatorNote, AccessGrantPolicy policy) {}
}
