package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.complex.platform.access.AccessActor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class FacilityInspectionScheduleJobTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inspectionDayFollowsLocalCalendarDay() {
        FacilityInspectionService service = mock(FacilityInspectionService.class);
        // 北京 2026-06-27 04:00 = UTC 2026-06-26 20:00 → 生成北京 6/27 的巡检任务。
        // 锚 UTC 时会去生成 6/26 的,当天该巡的点位排不出来、前一天的重复排。
        new FacilityInspectionScheduleJob(
                service, Clock.fixed(Instant.parse("2026-06-26T20:00:00Z"), ZoneOffset.UTC))
                .scheduledGenerateDueTasks();
        verify(service).generateScheduledTasks(LocalDate.parse("2026-06-27"));
    }

    @Test
    void scheduledGenerateDueTasksRunsWithSystemPlatformActor() {
        FacilityInspectionService service = mock(FacilityInspectionService.class);
        LocalDate expectedDate = LocalDate.parse("2026-06-26");
        doAnswer(invocation -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
            assertThat(authentication.getPrincipal()).isInstanceOf(AccessActor.class);
            AccessActor actor = (AccessActor) authentication.getPrincipal();
            assertThat(actor.id()).isEqualTo("system_scheduler");
            assertThat(actor.accountType()).isEqualTo("platform_admin");
            assertThat(actor.status()).isEqualTo("active");
            return 0;
        }).when(service).generateScheduledTasks(expectedDate);
        FacilityInspectionScheduleJob job = new FacilityInspectionScheduleJob(
                service, Clock.fixed(Instant.parse("2026-06-26T08:00:00Z"), ZoneOffset.UTC));

        job.scheduledGenerateDueTasks();

        verify(service).generateScheduledTasks(expectedDate);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
