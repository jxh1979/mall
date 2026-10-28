package com.complex.platform.customer;

// 文件版本：V0.03

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
 *   租户隔离：签到阶梯 resolveId/预检查询按解析出的 project scope（token scope）而非请求体 project_id。
 * 第十八批第 12 条：会员等级与生日加倍规则全平台一套（无 project_id），预检与定位用全量列表，写入须全国范围授权。
 */
class LoyaltyConfigConflictAndReversibleTests {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private final LoyaltyConfigAdminService service =
            new LoyaltyConfigAdminService(repository, authorization, audit);

    private static CustomerMemberLevelRecord level(String id, String code, String status) {
        Instant now = Instant.now();
        return new CustomerMemberLevelRecord(id, code, "名", 1, 0L, 10000, status, now, now);
    }

    private void nationwide() {
        when(authorization.allowsPlatformScope(any(), any())).thenReturn(true);
    }

    // ---- 问题1：重复创建 → 409（含对已停用/归档项的冲突） ----

    @Test
    void createLevelWithDuplicateCodeReturnsConflictNotServerError() {
        nationwide();
        // 已存在同 code 的记录且为 archived（状态无关预检必须仍命中，否则会落裸 uq_cml 约束违例 500）。
        when(repository.listAllMemberLevels())
                .thenReturn(List.of(level("member_level_old", "gold", "archived")));

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "gold", "金卡", 2, 5000L, 15000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
        // 冲突时不得落库。
        verify(repository, org.mockito.Mockito.never()).saveMemberLevel(any());
    }

    @Test
    void createCheckInTierWithDuplicateStreakReturnsConflict() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(authorization.projectReach(any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(AuthorizationService.ProjectReach.ownOnly("prj_A"));
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
        nationwide();
        Instant now = Instant.now();
        when(repository.listAllBirthdayRules()).thenReturn(List.of(
                new CustomerBirthdayRuleRecord("birthday_rule_old", "生日双倍", "birthday_day", 20000, "active", now, now)));

        assertThatThrownBy(() -> service.saveBirthdayRule(new BirthdayRuleConfigRequest(
                null, "生日双倍", "birthday_day", 20000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createLevelWithFreshCodeStillSucceeds() {
        nationwide();
        when(repository.listAllMemberLevels())
                .thenReturn(List.of(level("member_level_old", "silver", "active")));
        when(repository.saveMemberLevel(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerMemberLevelRecord saved = service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "gold", "金卡", 2, 5000L, 15000, "active"));

        assertThat(saved.levelCode()).isEqualTo("gold");
        assertThat(saved.id()).startsWith("member_level_");
    }

    // ---- 问题2：停用可逆——inactive/archived 项仍可按 id 改回 active ----

    @Test
    void inactiveLevelIsResolvableByIdAndCanBeReactivated() {
        nationwide();
        // 已停用（archived）记录：GET 列表 (listMemberLevels) 已隐去，但状态无关的 listAllMemberLevels 仍返回它。
        when(repository.listAllMemberLevels())
                .thenReturn(List.of(level("member_level_x", "gold", "archived")));
        when(repository.saveMemberLevel(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerMemberLevelRecord saved = service.saveMemberLevel(new MemberLevelConfigRequest(
                "member_level_x", "gold", "金卡", 2, 5000L, 15000, "active"));

        assertThat(saved.id()).isEqualTo("member_level_x");
        assertThat(saved.status()).isEqualTo("active"); // 停用项被改回 active（可逆）
    }

    // ---- 全平台一套：只到本人项目的账号不能写等级与生日规则 ----

    @Test
    void projectScopedAdminCannotWriteLevelOrBirthdayRule() {
        when(authorization.allowsPlatformScope(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "gold", "金卡", 2, 5000L, 15000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.saveBirthdayRule(new BirthdayRuleConfigRequest(
                null, "生日双倍", "birthday_day", 20000, "active")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repository, org.mockito.Mockito.never()).saveMemberLevel(any());
        verify(repository, org.mockito.Mockito.never()).saveBirthdayRule(any());
    }
}
