package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.finance.RefundAuthorizationService.RefundTier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefundAuthorizationServiceTests {

    private final InMemoryRefundAuthorizationRepository repository = new InMemoryRefundAuthorizationRepository();
    private final RefundAuthorizationService service = new RefundAuthorizationService(repository);

    @Test
    void enabledAutoRuleClassifiesAsRuleAuto() {
        repository.saveRule(rule("ticket", "expired_unused", true));
        assertThat(service.classify("ticket", 999_999L, "usr_a", "expired_unused")).isEqualTo(RefundTier.RULE_AUTO);
    }

    @Test
    void allDomainAutoRuleFallbackClassifiesAsRuleAuto() {
        repository.saveRule(rule("all", "expired_unused", true));
        assertThat(service.classify("bar", 999_999L, "usr_a", "expired_unused")).isEqualTo(RefundTier.RULE_AUTO);
    }

    @Test
    void disabledAutoRuleDoesNotAutoApprove() {
        repository.saveRule(rule("ticket", "expired_unused", false));
        // 规则未启用 + 无额度 → 复核。
        assertThat(service.classify("ticket", 100L, "usr_a", "expired_unused")).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void amountWithinDomainLimitClassifiesAsWithinLimit() {
        repository.saveLimit(limit("staff", "usr_a", "ticket", 50_000L));
        assertThat(service.classify("ticket", 50_000L, "usr_a", null)).isEqualTo(RefundTier.WITHIN_LIMIT);
        assertThat(service.classify("ticket", 49_999L, "usr_a", null)).isEqualTo(RefundTier.WITHIN_LIMIT);
    }

    @Test
    void amountOverLimitClassifiesAsReviewRequired() {
        repository.saveLimit(limit("staff", "usr_a", "ticket", 50_000L));
        assertThat(service.classify("ticket", 50_001L, "usr_a", null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void allDomainLimitFallbackApplies() {
        repository.saveLimit(limit("staff", "usr_a", "all", 30_000L));
        assertThat(service.classify("finance", 30_000L, "usr_a", null)).isEqualTo(RefundTier.WITHIN_LIMIT);
        assertThat(service.classify("finance", 30_001L, "usr_a", null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void noConfigDefaultsToReviewRequired() {
        // 默认无任何配置 → 一律复核（最安全）。
        assertThat(service.classify("ticket", 1L, "usr_a", "expired_unused")).isEqualTo(RefundTier.REVIEW_REQUIRED);
        assertThat(service.classify("ticket", 1L, null, null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    private static RefundAuthorityLimitRecord limit(String st, String sid, String dom, long lim) {
        Instant now = Instant.parse("2026-06-23T00:00:00Z");
        return new RefundAuthorityLimitRecord("lim_" + st + sid + dom, st, sid, dom, lim, null, "active", now, now);
    }

    private static RefundAutoRuleRecord rule(String dom, String type, boolean enabled) {
        Instant now = Instant.parse("2026-06-23T00:00:00Z");
        return new RefundAutoRuleRecord("rule_" + dom + type, dom, type, enabled, null, "active", now, now);
    }

    /** 内存桩：用自然键去重，对齐 DB 唯一约束。 */
    private static final class InMemoryRefundAuthorizationRepository implements RefundAuthorizationRepository {
        private final Map<String, RefundAuthorityLimitRecord> limits = new LinkedHashMap<>();
        private final Map<String, RefundAutoRuleRecord> rules = new LinkedHashMap<>();

        @Override
        public Optional<RefundAuthorityLimitRecord> findActiveLimit(String subjectType, String subjectId, String refundDomain) {
            return Optional.ofNullable(limits.get(subjectType + "|" + subjectId + "|" + refundDomain))
                    .filter(l -> "active".equals(l.status()));
        }

        @Override
        public boolean isAutoRuleEnabled(String refundDomain, String ruleType) {
            RefundAutoRuleRecord r = rules.get(refundDomain + "|" + ruleType);
            return r != null && r.enabled() && "active".equals(r.status());
        }

        @Override
        public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord limit) {
            limits.put(limit.subjectType() + "|" + limit.subjectId() + "|" + limit.refundDomain(), limit);
            return limit;
        }

        @Override
        public List<RefundAuthorityLimitRecord> listLimits(String projectId) {
            return limits.values().stream()
                    .filter(l -> projectId == null || projectId.equals(l.projectId()) || l.projectId() == null)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        @Override
        public RefundAutoRuleRecord saveRule(RefundAutoRuleRecord rule) {
            rules.put(rule.refundDomain() + "|" + rule.ruleType(), rule);
            return rule;
        }

        @Override
        public List<RefundAutoRuleRecord> listRules(String projectId) {
            return rules.values().stream()
                    .filter(r -> projectId == null || projectId.equals(r.projectId()) || r.projectId() == null)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
    }
}
