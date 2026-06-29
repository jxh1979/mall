package com.complex.platform.facility;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface FacilityWorkOrderRepository {
    Optional<FacilityWorkOrderPolicyRecord> findPolicy(String workOrderType, String priority);

    FacilityWorkOrderPolicyRecord savePolicy(FacilityWorkOrderPolicyRecord record);

    void insertWorkOrder(FacilityWorkOrderRecord record);

    void updateWorkOrder(FacilityWorkOrderRecord record);

    Optional<FacilityWorkOrderRecord> findById(String id);

    List<FacilityWorkOrderRecord> list();

    default List<FacilityWorkOrderRecord> listAssignedTo(String assigneeId, String projectId) {
        return list().stream()
                .filter(record -> Objects.equals(assigneeId, record.assigneeId()))
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    List<FacilityWorkOrderRecord> listOverdue(Instant now);
}
