package com.complex.platform.amusement;

import java.util.List;
import java.util.Optional;

public interface AmusementSafetyRepository {

    List<AmusementSafetyNoticeRecord> findAllSafetyNotices();

    Optional<AmusementSafetyNoticeRecord> findSafetyNoticeById(String id);

    AmusementSafetyNoticeRecord saveSafetyNotice(AmusementSafetyNoticeRecord record);

    AmusementWaiverAcknowledgementRecord saveWaiverAcknowledgement(AmusementWaiverAcknowledgementRecord record);

    List<AmusementIncidentRecord> findAllIncidents();

    Optional<AmusementIncidentRecord> findIncidentById(String id);

    AmusementIncidentRecord saveIncident(AmusementIncidentRecord record);

    AmusementIncidentActionRecord saveIncidentAction(AmusementIncidentActionRecord record);

    List<AmusementIncidentActionRecord> findIncidentActionsByIncidentId(String incidentId);
}
