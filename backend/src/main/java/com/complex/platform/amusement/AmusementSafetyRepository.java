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
}
