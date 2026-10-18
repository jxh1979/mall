package com.complex.platform.amusement;

import java.util.List;
import java.util.Optional;

public interface AmusementSafetyRepository {

    List<AmusementSafetyNoticeRecord> findAllSafetyNotices();

    List<AmusementSafetyNoticeRecord> findSafetyNoticesByMerchant(String merchantId);

    Optional<AmusementSafetyNoticeRecord> findSafetyNoticeById(String id);

    AmusementSafetyNoticeRecord saveSafetyNotice(AmusementSafetyNoticeRecord record);

    AmusementWaiverAcknowledgementRecord saveWaiverAcknowledgement(AmusementWaiverAcknowledgementRecord record);

    List<AmusementIncidentRecord> findAllIncidents();

    List<AmusementIncidentRecord> findIncidentsByMerchant(String merchantId);

    Optional<AmusementIncidentRecord> findIncidentById(String id);

    AmusementIncidentRecord saveIncident(AmusementIncidentRecord record);

    AmusementIncidentActionRecord saveIncidentAction(AmusementIncidentActionRecord record);

    List<AmusementIncidentActionRecord> findIncidentActionsByIncidentId(String incidentId);

    /**
     * 事故停运联动：CAS 流转项目 open_status（仅当前等于 expectedCurrentStatus 才更新），返回是否更新成功。
     * 用于 high/critical 安全事故自动停运停售（open→paused）。与 AmusementRepository 同名方法同义。
     */
    boolean updateProjectOpenStatus(String projectId, String expectedCurrentStatus, String newStatus);

    /**
     * CG-02 系统自动停运（CAS，仅当前 open 才流转 open→paused）并记下原因 {@code amusement_projects.auto_paused_reason}
     * （V443：hard_capacity=硬超容，人数回落到硬上限以下由系统自动恢复；safety_incident=高危事故，不自动恢复）。
     * default 回落到不记原因的 CAS，以不破坏测试替身。
     */
    default boolean autoPauseProject(String projectId, String reason) {
        return updateProjectOpenStatus(projectId, "open", "paused");
    }

    /**
     * CG-02：硬超容停运期间发生高危事故 → 停运原因升级为 safety_incident，使出场回落不再自动恢复开放
     * （BSM「安全容量…自动停运」只对硬超容承诺自动恢复，事故停运须走巡检/检修复核）。返回是否升级。
     */
    default boolean escalateAutoPausedReasonToSafetyIncident(String projectId) {
        return false;
    }
}
