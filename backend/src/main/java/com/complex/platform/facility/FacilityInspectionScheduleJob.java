package com.complex.platform.facility;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Thin scheduled wrapper for template-driven inspection task generation. */
@Component
public class FacilityInspectionScheduleJob {

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

    @ConditionalOnProperty(name = "facility.inspection.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${facility.inspection.scheduled.fixed-delay-ms:300000}")
    public void scheduledGenerateDueTasks() {
        service.generateScheduledTasks(LocalDate.now(clock));
    }
}
