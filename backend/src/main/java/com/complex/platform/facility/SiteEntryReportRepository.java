package com.complex.platform.facility;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface SiteEntryReportRepository {
    void insert(SiteEntryReportRecord record);

    void update(SiteEntryReportRecord record);

    Optional<SiteEntryReportRecord> findById(String id);

    List<SiteEntryReportRecord> list();

    default List<SiteEntryReportRecord> listByProject(String projectId) {
        return list().stream()
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }

    default List<SiteEntryReportRecord> listAssignedTo(String assigneeId, String projectId) {
        return list().stream()
                .filter(record -> Objects.equals(assigneeId, record.vendorUserId()))
                .filter(record -> Objects.equals(projectId, record.projectId()))
                .toList();
    }
}
