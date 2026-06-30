package com.complex.platform.facility;

import com.complex.platform.access.AccessActor;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Thin scheduled wrapper for template-driven inspection task generation. */
@Component
@ConditionalOnProperty(name = "facility.inspection.scheduled.enabled", havingValue = "true")
public class FacilityInspectionScheduleJob {

    private static final AccessActor SCHEDULER_ACTOR = new AccessActor(
            "system_scheduler", "system_scheduler", "platform_admin", null, null, "active");

    private final FacilityInspectionService service;
    private final Clock clock;

    @Autowired
    public FacilityInspectionScheduleJob(FacilityInspectionService service) {
        this(service, Clock.systemUTC());
    }

    FacilityInspectionScheduleJob(FacilityInspectionService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${facility.inspection.scheduled.fixed-delay-ms:300000}")
    public void scheduledGenerateDueTasks() {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        Authentication previousAuthentication = previousContext.getAuthentication();
        try {
            SecurityContext systemContext = SecurityContextHolder.createEmptyContext();
            systemContext.setAuthentication(
                    new UsernamePasswordAuthenticationToken(SCHEDULER_ACTOR, null, List.of()));
            SecurityContextHolder.setContext(systemContext);
            service.generateScheduledTasks(LocalDate.now(clock));
        } finally {
            if (previousAuthentication == null) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.setContext(previousContext);
            }
        }
    }
}
