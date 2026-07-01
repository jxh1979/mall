package com.complex.platform.facility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface FacilityInspectionRepository {
    List<FacilityInspectionTemplateRecord> listTemplates();

    default List<FacilityInspectionTemplateRecord> listTemplatesByProject(String projectId) {
        return listTemplates().stream()
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    List<FacilityInspectionTemplateRecord> listSchedulableTemplates();

    Optional<FacilityInspectionTemplateRecord> findTemplateById(String id);

    void insertTemplate(FacilityInspectionTemplateRecord record);

    List<FacilityInspectionTaskRecord> listTasks();

    /**
     * 3.7 批量：一组下属在项目下的巡检任务完成统计（assigneeId -> [total, done]）。
     * 默认逐个查（供 InMemory 测试双子），Jdbc 覆盖为 GROUP BY 单查（消除 N+1）。
     */
    default java.util.Map<String, int[]> countCompletionByAssignees(
            java.util.Collection<String> assigneeIds, String projectId) {
        java.util.Map<String, int[]> result = new java.util.LinkedHashMap<>();
        if (assigneeIds == null) {
            return result;
        }
        for (String assigneeId : assigneeIds) {
            java.util.List<FacilityInspectionTaskRecord> list = listTasksAssignedTo(assigneeId, projectId);
            int done = (int) list.stream().filter(t -> t.completedAt() != null).count();
            result.put(assigneeId, new int[] {list.size(), done});
        }
        return result;
    }

    default List<FacilityInspectionTaskRecord> listTasksAssignedTo(String assigneeId, String projectId) {
        return listTasks().stream()
                .filter(record -> Objects.equals(assigneeId, record.assigneeId()))
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    Optional<FacilityInspectionTaskRecord> findTaskById(String id);

    Optional<LocalDate> latestScheduledDateForTemplate(String templateId);

    boolean existsTaskForTemplateDate(String templateId, LocalDate scheduledDate);

    void insertTask(FacilityInspectionTaskRecord record);

    void updateTask(FacilityInspectionTaskRecord record);

    void updateTaskWorkOrderId(String taskId, String workOrderId, Instant updatedAt);

    void insertRecord(FacilityInspectionRecordRecord record);

    List<FacilityInspectionRecordRecord> listRecords(String taskId);
}
