package com.complex.platform.evidence;

import java.util.List;
import java.util.Optional;

public interface TraceabilityRepository {

    TraceabilityProcessInstanceRecord saveProcessInstance(TraceabilityProcessInstanceRecord record);

    Optional<TraceabilityProcessInstanceRecord> findProcessInstanceById(String id);

    /** null merchantScope = 平台级全量；否则按 merchant 过滤。 */
    List<TraceabilityProcessInstanceRecord> listProcessInstances(String merchantScope);

    TraceabilityStepRecord saveStep(TraceabilityStepRecord record);

    List<TraceabilityStepRecord> listStepsByInstance(String processInstanceId);

    TraceabilityResponsibilityRecord saveResponsibility(TraceabilityResponsibilityRecord record);

    Optional<TraceabilityResponsibilityRecord> findResponsibilityById(String id);

    List<TraceabilityResponsibilityRecord> listResponsibilities(String merchantScope);
}
