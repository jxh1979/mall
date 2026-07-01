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

    /**
     * 3.7 批量：一组下属在项目下的工单完成统计（assigneeId -> [total, done]）。
     * 默认逐个查（供 InMemory 测试双子），Jdbc 覆盖为 GROUP BY 单查（消除 N+1）。
     */
    default java.util.Map<String, int[]> countCompletionByAssignees(
            java.util.Collection<String> assigneeIds, String projectId) {
        java.util.Map<String, int[]> result = new java.util.LinkedHashMap<>();
        if (assigneeIds == null) {
            return result;
        }
        for (String assigneeId : assigneeIds) {
            java.util.List<FacilityWorkOrderRecord> list = listAssignedTo(assigneeId, projectId);
            int done = (int) list.stream().filter(w -> w.completedAt() != null).count();
            result.put(assigneeId, new int[] {list.size(), done});
        }
        return result;
    }

    default List<FacilityWorkOrderRecord> listAssignedTo(String assigneeId, String projectId) {
        return list().stream()
                .filter(record -> Objects.equals(assigneeId, record.assigneeId()))
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    List<FacilityWorkOrderRecord> listOverdue(Instant now);
}
