package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.dto.BirthdayRuleConfigRequest;
import com.complex.platform.customer.dto.CheckInTierConfigRequest;
import com.complex.platform.customer.dto.MemberLevelConfigRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * PR#166 复查两问题的回归单测（无 DB，Mock 仓储/权限）：
 *   问题1 重复创建：同 project 已有同 level_code/streak_days/rule_name 时新增返回 409（非裸约束违例 500）；
 *   问题2 停用可逆：更新路径用状态无关 list*ByProject 定位，故 inactive/archived 项仍可按 id 改回 active；
 *   租户隔离：resolveId/预检查询按解析出的 project scope（token scope）而非请求体 project_id。
 */
class LoyaltyConfigConflictAndReversibleTests {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private final LoyaltyConfigAdminService service =
            new LoyaltyConfigAdminService(repository, authorization, audit);

    private static CustomerMemberLevelRecord level(String id, String projectId, String code, String status) {
        Instant now = Instant.now();
        return new CustomerMemberLevelRecord(id, projectId, code, "名", 1, 0L, 0L, 365, 0, 10000, status, now, now);
    }

    // ---- 问题1：重复创建 → 409（含对已停用/归档项的冲突） ----

    @Test
    void createLevelWithDuplicateCodeReturnsConflictNotServerError() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        // 已存在同 code 的记录且为 archived（状态无关预检必须仍命中，否则会落裸 uq_cml 约束违例 500）。
        when(repository.listMemberLevelsByProject("prj_A"))
                .thenReturn(List.of(level("member_level_old", "prj_A", "gold", "archived")));

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "prj_A", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
        // 冲突时不得落库。
        verify(repository, org.mockito.Mockito.never()).saveMemberLevel(any());
    }

    @Test
    void createCheckInTierWithDuplicateStreakReturnsConflict() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        Instant now = Instant.now();
        when(repository.listCheckInTiersByProject("prj_A")).thenReturn(List.of(
                new CustomerCheckInTierRecord("check_in_tier_old", "prj_A", 7, 100L, "active", now, now)));

        assertThatThrownBy(() -> service.saveCheckInTier(new CheckInTierConfigRequest(
                null, "prj_A", 7, 200L, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createBirthdayRuleWithDuplicateNameReturnsConflict() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        Instant now = Instant.now();
        when(repository.listBirthdayRulesByProject("prj_A")).thenReturn(List.of(
                new CustomerBirthdayRuleRecord("birthday_rule_old", "prj_A", "生日双倍", "birthday_day", 20000, "active", now, now)));

        assertThatThrownBy(() -> service.saveBirthdayRule(new BirthdayRuleConfigRequest(
                null, "prj_A", "生日双倍", "birthday_day", 20000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createLevelWithFreshCodeStillSucceeds() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.listMemberLevelsByProject("prj_A"))
                .thenReturn(List.of(level("member_level_old", "prj_A", "silver", "active")));
        when(repository.saveMemberLevel(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerMemberLevelRecord saved = service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "prj_A", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active"));

        assertThat(saved.levelCode()).isEqualTo("gold");
        assertThat(saved.id()).startsWith("member_level_");
    }

    // ---- 问题2：停用可逆——inactive/archived 项仍可按 id 改回 active ----

    @Test
    void inactiveLevelIsResolvableByIdAndCanBeReactivated() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        // 已停用（archived）记录：GET 列表 (listMemberLevels) 已隐去，但状态无关 ByProject 仍返回它。
        when(repository.listMemberLevelsByProject("prj_A"))
                .thenReturn(List.of(level("member_level_x", "prj_A", "gold", "archived")));
        when(repository.saveMemberLevel(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerMemberLevelRecord saved = service.saveMemberLevel(new MemberLevelConfigRequest(
                "member_level_x", "prj_A", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active"));

        assertThat(saved.id()).isEqualTo("member_level_x");
        assertThat(saved.status()).isEqualTo("active"); // 停用项被改回 active（可逆）
    }

    // ---- 租户隔离：预检/定位按解析出的 token scope，而非请求体伪造的 project_id ----

    @Test
    void updateResolvesWithinTokenScopeNotRequestBodyProject() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        // prj_A 名下无此 id；攻击者请求体伪称 prj_HACK 且借他项目的 id。
        when(repository.listMemberLevelsByProject("prj_A")).thenReturn(List.of());

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                "member_level_other_project", "prj_HACK", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
        // 定位查询严格走 token scope prj_A，绝不用请求体 prj_HACK。
        verify(repository).listMemberLevelsByProject("prj_A");
        verify(repository, org.mockito.Mockito.never()).listMemberLevelsByProject("prj_HACK");
    }

    @Test
    void createBindsProjectScopeOverRequestBodyForDuplicateCheck() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.listMemberLevelsByProject("prj_A")).thenReturn(List.of());
        when(repository.saveMemberLevel(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "prj_HACK", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active"));

        ArgumentCaptor<CustomerMemberLevelRecord> saved = ArgumentCaptor.forClass(CustomerMemberLevelRecord.class);
        verify(repository).saveMemberLevel(saved.capture());
        assertThat(saved.getValue().projectId()).isEqualTo("prj_A");
        verify(repository).listMemberLevelsByProject("prj_A");
    }
}
