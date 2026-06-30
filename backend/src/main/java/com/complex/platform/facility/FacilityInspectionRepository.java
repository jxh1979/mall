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
