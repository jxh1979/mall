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
}
