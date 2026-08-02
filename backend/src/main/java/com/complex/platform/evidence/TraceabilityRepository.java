package com.complex.platform.evidence;

import java.util.List;
import java.util.Optional;

public interface TraceabilityRepository {

    TraceabilityProcessInstanceRecord saveProcessInstance(TraceabilityProcessInstanceRecord record);

    Optional<TraceabilityProcessInstanceRecord> findProcessInstanceById(String id);

    TraceabilityStepRecord saveStep(TraceabilityStepRecord record);

    List<TraceabilityStepRecord> listStepsByInstance(String processInstanceId);

    default TraceabilityAutoCaptureRate autoCaptureRate(String merchantScope, int thresholdPercent) {
        return new TraceabilityAutoCaptureRate(0, 0, 100.0, thresholdPercent, true);
    }

    TraceabilityResponsibilityRecord saveResponsibility(TraceabilityResponsibilityRecord record);

    List<TraceabilityResponsibilityRecord> listResponsibilities(String merchantScope);
}
