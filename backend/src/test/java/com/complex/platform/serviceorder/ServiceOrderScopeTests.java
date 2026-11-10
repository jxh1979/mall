// 文件版本：V0.04
package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** 清单 P1(2026-07-03 审计):服务点单跨综合体写/动作隔离——项目A调用者不得操作项目B的服务单。 */
class ServiceOrderScopeTests {

    private JdbcServiceOrderRepository repo;
    private AuthorizationService authz;
    private JdbcStaffRepository staffRepo;

    private ServiceOrderService build() {
        return build(new com.complex.platform.common.BusinessDayResolver());
    }

    private ServiceOrderService build(com.complex.platform.common.BusinessDayResolver businessDays) {
        repo = mock(JdbcServiceOrderRepository.class);
        AuditEventService audit = mock(AuditEventService.class);
        authz = mock(AuthorizationService.class);
        staffRepo = mock(JdbcStaffRepository.class);
        IdempotencyService idem = mock(IdempotencyService.class);
        // 幂等封装直接执行 action(不测幂等,只测 scope 守卫)。
        when(idem.execute(any(), any(), any(), any(), any()))
                .thenAnswer(i -> ((Supplier<?>) i.getArgument(3)).get());
        return new ServiceOrderService(repo, audit, authz, staffRepo, idem, businessDays,
                mock(com.complex.platform.notification.NotificationService.class));
    }

    private ServiceOrderRecord orderFor(String performer, String status) {
        return new ServiceOrderRecord("svo_x", "svc1", "点舞", 30000L, null, null, performer, "usr_op",
                null, status, null, null, null, null, null, 0, 0, Instant.now(), Instant.now(), null, null, null);
    }

    @Test
    void createRejectsCrossProjectPerformer() {
        ServiceOrderService svc = build();
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(authz.projectReach(any(), any(String[].class))).thenReturn(AuthorizationService.ProjectReach.ownOnly("prj_A"));
        when(staffRepo.findUserProjectId("usr_b")).thenReturn(Optional.of("prj_B"));
        assertThatThrownBy(() -> svc.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                "k1", "svc1", "usr_b", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void markPaidRejectsCrossProjectOrder() {
        ServiceOrderService svc = build();
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(authz.projectReach(any(), any(String[].class))).thenReturn(AuthorizationService.ProjectReach.ownOnly("prj_A"));
        when(repo.findOrder("svo_x")).thenReturn(Optional.of(orderFor("usr_b", "created")));
        when(staffRepo.findUserProjectId("usr_b")).thenReturn(Optional.of("prj_B"));
        assertThatThrownBy(() -> svc.markPaid("svo_x",
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest("k2", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void platformAdminEmptyScopeBypassesProjectGuard() {
        ServiceOrderService svc = build();
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.empty());
        when(authz.projectReach(any(), any(String[].class)))
                .thenReturn(new AuthorizationService.ProjectReach(true, true, null, true, java.util.Set.of()));
        when(repo.findCatalogItem(any())).thenReturn(Optional.empty());
        // 空 scope 不触发跨项目拒;继续走目录查找 → 目录不存在(证明守卫已放行,非 CROSS_PROJECT)。
        assertThatThrownBy(() -> svc.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                "k3", "svc_missing", "usr_b", null, null, null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SERVICE_ITEM_NOT_FOUND"));
    }

    // 第十八批 E 段 19-R1（18A 同根）：持可跨项目角色的人（本人项目 A、可触及 B）给 B 的履约人下单、未选酒吧时，
    // 生效营业日按履约人档案项目 B 取（新单的 project_id 本就按履约人档案落），不按本人项目 A。
    @Test
    void holderOrderingForPerformerInAnotherProjectUsesPerformersBusinessDay() {
        com.complex.platform.common.BusinessDayResolver businessDays = mock(com.complex.platform.common.BusinessDayResolver.class);
        ServiceOrderService svc = build(businessDays);
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(authz.projectReach(any(), any(String[].class)))
                .thenReturn(new AuthorizationService.ProjectReach(false, false, "prj_A", true, java.util.Set.of("prj_B")));
        when(staffRepo.findUserProjectId("usr_b")).thenReturn(Optional.of("prj_B"));
        when(repo.findCatalogItem("svc1")).thenReturn(Optional.of(new ServiceCatalogItemRecord(
                "svc1", "点舞", 30000L, 10, true, "none", 0L, "usr_op", Instant.now())));
        when(repo.saveOrder(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(businessDays.businessDate(any(), any(), any())).thenReturn(java.time.LocalDate.parse("2099-07-10"));

        assertThatCode(() -> svc.createOrder(new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                "k4", "svc1", "usr_b", null, null, null))).doesNotThrowAnyException();
        verify(businessDays).businessDate(eq("prj_B"), isNull(), any());
    }
}
