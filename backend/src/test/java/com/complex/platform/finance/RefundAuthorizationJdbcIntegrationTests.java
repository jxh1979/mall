package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * B1d② 退款分级授权按综合体(project)隔离的 JDBC 验证（V174 唯一键含 project_id）：
 * 验 ON CONFLICT 表达式与唯一索引匹配（saveLimit/saveRule 可幂等 upsert）、不同综合体（及全局）同自然键互不覆盖、
 * listLimits/listRules 按 project 过滤（该综合体 + 全局）。@Transactional 回滚隔离，不污染共享库。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class RefundAuthorizationJdbcIntegrationTests {

    @Autowired
    private RefundAuthorizationRepository repository;

    private RefundAuthorityLimitRecord limit(String subjectId, String projectId, long amount) {
        return new RefundAuthorityLimitRecord(
                "rl_" + subjectId + "_" + (projectId == null ? "global" : projectId),
                "staff", subjectId, "ticket", amount, projectId, "active", null, null);
    }

    private RefundAutoRuleRecord rule(String ruleType, String projectId, boolean enabled) {
        return new RefundAutoRuleRecord(
                "rr_" + ruleType + "_" + (projectId == null ? "global" : projectId),
                "ticket", ruleType, enabled, projectId, "active", null, null);
    }

    @Test
    void limitsPerProjectCoexistAndUpsertIsIdempotentPerNaturalKey() {
        String subjectId = "b1d2_limit_subject";
        // 同 subject/domain，三个 project 槽（P1/P2/全局）应各自独立成行（唯一键含 COALESCE(project_id)）。
        repository.saveLimit(limit(subjectId, "proj_p1", 10_000L));
        repository.saveLimit(limit(subjectId, "proj_p2", 20_000L));
        repository.saveLimit(limit(subjectId, null, 5_000L));
        // 同自然键再 upsert（P1 改额度）→ 仍一行、额度更新（ON CONFLICT DO UPDATE 命中唯一索引）。
        repository.saveLimit(limit(subjectId, "proj_p1", 11_000L));

        List<RefundAuthorityLimitRecord> all = repository.listLimits(null).stream()
                .filter(l -> subjectId.equals(l.subjectId())).toList();
        assertThat(all).hasSize(3);
        assertThat(all).extracting(RefundAuthorityLimitRecord::projectId)
                .containsExactlyInAnyOrder("proj_p1", "proj_p2", null);
        assertThat(all.stream().filter(l -> "proj_p1".equals(l.projectId())).findFirst().orElseThrow().limitAmountCent())
                .isEqualTo(11_000L);

        // listLimits(P1) 只见 P1 + 全局，不见 P2（按综合体隔离 + 全局回退）。
        List<RefundAuthorityLimitRecord> p1 = repository.listLimits("proj_p1").stream()
                .filter(l -> subjectId.equals(l.subjectId())).toList();
        assertThat(p1).extracting(RefundAuthorityLimitRecord::projectId)
                .containsExactlyInAnyOrder("proj_p1", null);
    }

    @Test
    void autoRulesPerProjectCoexistAndListFiltersByProject() {
        String ruleType = "b1d2_rule_expired";
        repository.saveRule(rule(ruleType, "proj_p1", true));
        repository.saveRule(rule(ruleType, "proj_p2", false));
        repository.saveRule(rule(ruleType, null, true));
        repository.saveRule(rule(ruleType, "proj_p1", false));   // 同自然键 upsert → enabled 翻 false。

        List<RefundAutoRuleRecord> all = repository.listRules(null).stream()
                .filter(r -> ruleType.equals(r.ruleType())).toList();
        assertThat(all).hasSize(3);
        assertThat(all.stream().filter(r -> "proj_p1".equals(r.projectId())).findFirst().orElseThrow().enabled())
                .isFalse();

        List<RefundAutoRuleRecord> p2 = repository.listRules("proj_p2").stream()
                .filter(r -> ruleType.equals(r.ruleType())).toList();
        assertThat(p2).extracting(RefundAutoRuleRecord::projectId)
                .containsExactlyInAnyOrder("proj_p2", null);
    }
}
