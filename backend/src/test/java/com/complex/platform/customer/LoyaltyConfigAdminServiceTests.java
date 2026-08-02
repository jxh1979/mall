package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.dto.BirthdayRuleConfigRequest;
import com.complex.platform.customer.dto.CheckInTierConfigRequest;
import com.complex.platform.customer.dto.MemberLevelConfigRequest;
import com.complex.platform.evidence.AuditEventService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 会员忠诚度配置服务的租户隔离 + 写路径 project_id 绑定 + DB CHECK 前置校验单测。
 * 用 Mock AuthorizationService 精确控制 token scope，无需真实 DB/权限授予。
 */
class LoyaltyConfigAdminServiceTests {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AuditEventService audit = mock(AuditEventService.class);
    private final LoyaltyConfigAdminService service =
            new LoyaltyConfigAdminService(repository, authorization, audit);

    @Test
    void createLevelBindsProjectScopeOverRequestBodyForScopedAdmin() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.saveMemberLevel(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "prj_HACK", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active"));

        ArgumentCaptor<CustomerMemberLevelRecord> saved = ArgumentCaptor.forClass(CustomerMemberLevelRecord.class);
        org.mockito.Mockito.verify(repository).saveMemberLevel(saved.capture());
        assertThat(saved.getValue().projectId()).isEqualTo("prj_A");
        assertThat(saved.getValue().id()).startsWith("member_level_");
    }

    @Test
    void createLevelByPlatformAdminRequiresExplicitProjectId() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                null, null, "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("project_id is required");
    }

    @Test
    void keepPeriodDaysMustBePositive() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                null, "prj_A", "gold", "金卡", 2, 5000L, 3000L, 0, 30, 15000, "active")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("keep_period_days");
    }

    @Test
    void updatingLevelWithUnknownIdInScopeIsNotFound() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.listMemberLevels("prj_A")).thenReturn(java.util.List.of());

        assertThatThrownBy(() -> service.saveMemberLevel(new MemberLevelConfigRequest(
                "member_level_ghost", "prj_A", "gold", "金卡", 2, 5000L, 3000L, 365, 30, 15000, "active")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未找到");
    }

    @Test
    void birthdayMultiplierMustExceedParity() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));

        assertThatThrownBy(() -> service.saveBirthdayRule(new BirthdayRuleConfigRequest(
                null, "prj_A", "生日双倍", "birthday_day", 10000, "active")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("multiplier_basis_points");
    }

    @Test
    void checkInTierStreakMustBeAtLeastOne() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));

        assertThatThrownBy(() -> service.saveCheckInTier(new CheckInTierConfigRequest(
                null, "prj_A", 0, 100L, "active")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("streak_days");
    }

    @Test
    void saveCheckInTierBindsScopeAndGeneratesId() {
        when(authorization.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repository.saveCheckInTier(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveCheckInTier(new CheckInTierConfigRequest(null, "prj_X", 7, 200L, "active"));

        ArgumentCaptor<CustomerCheckInTierRecord> saved = ArgumentCaptor.forClass(CustomerCheckInTierRecord.class);
        org.mockito.Mockito.verify(repository).saveCheckInTier(saved.capture());
        assertThat(saved.getValue().projectId()).isEqualTo("prj_A");
        assertThat(saved.getValue().streakDays()).isEqualTo(7);
    }
}
