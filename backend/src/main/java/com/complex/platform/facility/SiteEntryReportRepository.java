package com.complex.platform.facility;

// 文件版本：V0.02

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface SiteEntryReportRepository {
    void insert(SiteEntryReportRecord record);

    void update(SiteEntryReportRecord record);

    Optional<SiteEntryReportRecord> findById(String id);

    Optional<SiteEntryReportRecord> findByIdForUpdate(String id);

    List<SiteEntryHistoryRecord> history(String reportId);

    void appendHistory(SiteEntryHistoryRecord record);

    List<SiteEntryReportRecord> list();

    default List<SiteEntryReportRecord> listByProject(String projectId) {
        return list().stream()
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    /** 18E（第 19 条 19-R3）：归属给定项目之一的报备；空集合返回空表。 */
    default List<SiteEntryReportRecord> listByProjects(java.util.Collection<String> projectIds) {
        return list().stream().filter(record -> projectIds.contains(record.projectId())).toList();
    }

    default List<SiteEntryReportRecord> listAssignedTo(String assigneeId, String projectId) {
        return list().stream()
                .filter(record -> Objects.equals(assigneeId, record.vendorUserId()))
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }
}
