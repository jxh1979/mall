package com.complex.platform.access.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** 清单 P1(2026-07-03 审计):员工写/审批跨综合体隔离——项目A主管不得给项目B员工排班/批假/记薪。 */
class StaffScopeTests {

    private StaffRepository repo;
    private AuthorizationService authz;

    private StaffService build() {
        repo = mock(StaffRepository.class);
        AccessRepository access = mock(AccessRepository.class);
        IdempotencyService idem = mock(IdempotencyService.class);
        AuditEventService audit = mock(AuditEventService.class);
        authz = mock(AuthorizationService.class);
        when(idem.execute(any(), any(), any(), any(), any()))
                .thenAnswer(i -> ((Supplier<?>) i.getArgument(3)).get());
        return new StaffService(repo, access, idem, audit, authz);
    }

    @Test
    void upsertShiftRejectsCrossProjectUser() {
        StaffService svc = build();
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repo.findUserProjectId("usr_b")).thenReturn(Optional.of("prj_B"));
        assertThatThrownBy(() -> svc.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                "k1", "usr_b", LocalDate.parse("2026-07-10"),
                Instant.parse("2026-07-10T10:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"), List.of(), null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void reviewLeaveRejectsCrossProjectLeave() {
        StaffService svc = build();
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repo.findLeaveById("lv_x")).thenReturn(Optional.of(new StaffLeaveRequestRecord(
                "lv_x", "usr_b", "annual", LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-11"),
                "假", "pending", null, null, Instant.now(), Instant.now())));
        when(repo.findUserProjectId("usr_b")).thenReturn(Optional.of("prj_B"));
        assertThatThrownBy(() -> svc.reviewLeave("lv_x",
                new com.complex.platform.access.staff.dto.StaffLeaveReviewRequest("k2", "approve", "ok")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void sameProjectSupervisorPassesGuard() {
        StaffService svc = build();
        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("prj_A"));
        when(repo.findUserProjectId("usr_same")).thenReturn(Optional.of("prj_A"));
        when(repo.upsertShift(any())).thenAnswer(i -> i.getArgument(0));
        // 同项目 → 守卫放行,不抛 CROSS_PROJECT(落到正常保存)。
        svc.upsertShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                "k3", "usr_same", LocalDate.parse("2026-07-10"),
                Instant.parse("2026-07-10T10:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"), List.of(), null));
    }
}
