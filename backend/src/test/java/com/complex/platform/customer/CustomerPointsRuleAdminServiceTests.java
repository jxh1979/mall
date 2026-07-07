package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.dto.CustomerPointsRuleRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 后台积分规则管理服务的租户隔离 + 写路径 project_id 绑定单测（REQ-12, P0-6）。
 * 用 Mock AuthorizationService 精确控制 token scope，无需真实 DB/权限授予。
 */
class CustomerPointsRuleAdminServiceTests {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private final CustomerPointsRuleAdminService service =
            new CustomerPointsRuleAdminService(repository, authorization, audit);

    private static CustomerPointsRuleRecord rule(String id, String projectId) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new CustomerPointsRuleRecord(id, projectId, "base earn", "consume_amount", 1L,
                "{}", "fixed_days", 365, "fifo_batch", null, "active", now, now);
    }

    @Test
    void projectScopedAdminCannotReadAnotherProjectsRule() {
        // 规则属 prj_B；当前管理员 token scope=prj_A → 越域按 404 处理（不泄漏他项目存在性）。
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.findPointsRuleById("rule_b")).thenReturn(Optional.of(rule("rule_b", "prj_B")));

        assertThatThrownBy(() -> service.get("rule_b"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("points rule not found");
    }

    @Test
    void projectScopedAdminReadsOwnProjectRule() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.findPointsRuleById("rule_a")).thenReturn(Optional.of(rule("rule_a", "prj_A")));

        assertThat(service.get("rule_a").projectId()).isEqualTo("prj_A");
    }

    @Test
    void createBindsProjectScopeOverRequestBodyForScopedAdmin() {
        // 写路径不信任请求体 project_id（AC-ISO-2）：管理员 scope=prj_A，即便请求体填 prj_HACK 也落 prj_A。
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.savePointsRule(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(new CustomerPointsRuleRequest(
                "prj_HACK", "vip earn", "consume_amount", 2L, "{}", "fixed_days", 180, "fifo_batch", null, "active"));

        ArgumentCaptor<CustomerPointsRuleRecord> saved = ArgumentCaptor.forClass(CustomerPointsRuleRecord.class);
        org.mockito.Mockito.verify(repository).savePointsRule(saved.capture());
        assertThat(saved.getValue().projectId()).isEqualTo("prj_A");
    }

    @Test
    void createByPlatformAdminRequiresExplicitProjectId() {
        // 平台管理员无 token scope → 必须显式给 project_id，否则 400。
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CustomerPointsRuleRequest(
                null, "vip earn", "consume_amount", 2L, "{}", "fixed_days", 180, "fifo_batch", null, "active")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("project_id is required");
    }

    @Test
    void platformAdminListsAllRulesUnscoped() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(repository.listPointsRules()).thenReturn(List.of(rule("r1", "prj_A"), rule("r2", "prj_B")));

        assertThat(service.list().items()).extracting(CustomerPointsRuleRecord::id).containsExactly("r1", "r2");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).listPointsRulesByProject(anyString());
    }
}
