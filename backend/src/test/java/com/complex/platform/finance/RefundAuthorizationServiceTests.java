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
        repository.saveRule(rule("ticket", "expired_unused", true, null));
        assertThat(service.classify("ticket", 999_999L, "usr_a", "expired_unused", null)).isEqualTo(RefundTier.RULE_AUTO);
    }

    @Test
    void allDomainAutoRuleFallbackClassifiesAsRuleAuto() {
        repository.saveRule(rule("all", "expired_unused", true, null));
        assertThat(service.classify("bar", 999_999L, "usr_a", "expired_unused", null)).isEqualTo(RefundTier.RULE_AUTO);
    }

    @Test
    void disabledAutoRuleDoesNotAutoApprove() {
        repository.saveRule(rule("ticket", "expired_unused", false, null));
        // 规则未启用 + 无额度 → 复核。
        assertThat(service.classify("ticket", 100L, "usr_a", "expired_unused", null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void amountWithinDomainLimitClassifiesAsWithinLimit() {
        repository.saveLimit(limit("staff", "usr_a", "ticket", 50_000L, null));
        assertThat(service.classify("ticket", 50_000L, "usr_a", null, null)).isEqualTo(RefundTier.WITHIN_LIMIT);
        assertThat(service.classify("ticket", 49_999L, "usr_a", null, null)).isEqualTo(RefundTier.WITHIN_LIMIT);
    }

    @Test
    void amountOverLimitClassifiesAsReviewRequired() {
        repository.saveLimit(limit("staff", "usr_a", "ticket", 50_000L, null));
        assertThat(service.classify("ticket", 50_001L, "usr_a", null, null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void allDomainLimitFallbackApplies() {
        repository.saveLimit(limit("staff", "usr_a", "all", 30_000L, null));
        assertThat(service.classify("finance", 30_000L, "usr_a", null, null)).isEqualTo(RefundTier.WITHIN_LIMIT);
        assertThat(service.classify("finance", 30_001L, "usr_a", null, null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void noConfigDefaultsToReviewRequired() {
        // 默认无任何配置 → 一律复核（最安全）。
        assertThat(service.classify("ticket", 1L, "usr_a", "expired_unused", null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
        assertThat(service.classify("ticket", 1L, null, null, null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    // ─── AUD-008 综合体隔离（不同综合体同员工/规则各自命中，绝不跨综合体串配置） ───

    @Test
    void sameStaffDifferentLimitPerProjectDoesNotBleed() {
        // 对抗场景：员工 A 在 P1 额度 1 万、在 P2 额度 1.00 元。P2 发起 5,000 元退款绝不能命中 P1 的额度。
        repository.saveLimit(limit("staff", "usr_a", "ticket", 1_000_000L, "proj_p1"));
        repository.saveLimit(limit("staff", "usr_a", "ticket", 100L, "proj_p2"));
        assertThat(service.classify("ticket", 500_000L, "usr_a", null, "proj_p2")).isEqualTo(RefundTier.REVIEW_REQUIRED);
        assertThat(service.classify("ticket", 500_000L, "usr_a", null, "proj_p1")).isEqualTo(RefundTier.WITHIN_LIMIT);
    }

    @Test
    void projectSpecificLimitOverridesGlobalAndOtherProjectFallsBackToGlobal() {
        repository.saveLimit(limit("staff", "usr_b", "ticket", 100L, null));          // 全局 1.00
        repository.saveLimit(limit("staff", "usr_b", "ticket", 5_000_000L, "proj_p1")); // P1 覆盖为 5 万
        assertThat(service.classify("ticket", 4_000_000L, "usr_b", null, "proj_p1")).isEqualTo(RefundTier.WITHIN_LIMIT);
        // P2 无专属 → 回退全局 1.00 → 4 万超额复核。
        assertThat(service.classify("ticket", 4_000_000L, "usr_b", null, "proj_p2")).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void noProjectContextMatchesGlobalOnlyNotAnyProjectRow() {
        // 无综合体上下文（projectId=null）→ 只命中全局(NULL)行，绝不命中某综合体的行。
        repository.saveLimit(limit("staff", "usr_c", "ticket", 100L, "proj_p1"));
        assertThat(service.classify("ticket", 50L, "usr_c", null, null)).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    @Test
    void autoRuleOppositeTogglesPerProject() {
        repository.saveRule(rule("ticket", "expired_unused", true, "proj_p1"));
        repository.saveRule(rule("ticket", "expired_unused", false, "proj_p2"));
        assertThat(service.classify("ticket", 999_999L, "usr_x", "expired_unused", "proj_p1")).isEqualTo(RefundTier.RULE_AUTO);
        assertThat(service.classify("ticket", 999_999L, "usr_x", "expired_unused", "proj_p2")).isEqualTo(RefundTier.REVIEW_REQUIRED);
    }

    private static RefundAuthorityLimitRecord limit(String st, String sid, String dom, long lim, String pid) {
        Instant now = Instant.parse("2026-06-23T00:00:00Z");
        return new RefundAuthorityLimitRecord(
                "lim_" + st + sid + dom + (pid == null ? "g" : pid), st, sid, dom, lim, pid, "active", now, now);
    }

    private static RefundAutoRuleRecord rule(String dom, String type, boolean enabled, String pid) {
        Instant now = Instant.parse("2026-06-23T00:00:00Z");
        return new RefundAutoRuleRecord(
                "rule_" + dom + type + (pid == null ? "g" : pid), dom, type, enabled, pid, "active", now, now);
    }

    /** 内存桩：自然键含 project 槽，运行时查询「精确综合体 → 全局(NULL)回退」，对齐 DB 唯一约束与 JDBC 谓词。 */
    private static final class InMemoryRefundAuthorizationRepository implements RefundAuthorizationRepository {
        private final Map<String, RefundAuthorityLimitRecord> limits = new LinkedHashMap<>();
        private final Map<String, RefundAutoRuleRecord> rules = new LinkedHashMap<>();

        private static String slot(String pid) {
            return pid == null ? "__global__" : pid;
        }

        @Override
        public Optional<RefundAuthorityLimitRecord> findActiveLimit(
                String subjectType, String subjectId, String refundDomain, String projectId) {
            String base = subjectType + "|" + subjectId + "|" + refundDomain + "|";
            RefundAuthorityLimitRecord exact = limits.get(base + slot(projectId));
            if (exact != null && "active".equals(exact.status())) {
                return Optional.of(exact);
            }
            RefundAuthorityLimitRecord global = limits.get(base + "__global__");
            return global != null && "active".equals(global.status()) ? Optional.of(global) : Optional.empty();
        }

        @Override
        public boolean isAutoRuleEnabled(String refundDomain, String ruleType, String projectId) {
            String base = refundDomain + "|" + ruleType + "|";
            RefundAutoRuleRecord exact = rules.get(base + slot(projectId));
            if (exact != null && "active".equals(exact.status())) {
                return exact.enabled();
            }
            RefundAutoRuleRecord global = rules.get(base + "__global__");
            return global != null && global.enabled() && "active".equals(global.status());
        }

        @Override
        public RefundAuthorityLimitRecord saveLimit(RefundAuthorityLimitRecord limit) {
            limits.put(limit.subjectType() + "|" + limit.subjectId() + "|" + limit.refundDomain()
                    + "|" + slot(limit.projectId()), limit);
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
            rules.put(rule.refundDomain() + "|" + rule.ruleType() + "|" + slot(rule.projectId()), rule);
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
