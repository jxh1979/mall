package com.complex.platform.amusement;

import java.util.List;
import java.util.Optional;

public interface AmusementRepository {
    AmusementSafetyTemplateRecord saveSafetyTemplate(AmusementSafetyTemplateRecord template);

    List<AmusementSafetyTemplateRecord> listSafetyTemplates();

    Optional<AmusementSafetyTemplateRecord> findSafetyTemplateById(String id);

    AmusementProjectRecord saveProject(AmusementProjectRecord project);

    List<AmusementProjectRecord> listProjects();

    Optional<AmusementProjectRecord> findProjectById(String id);

    AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule);

    List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId);

    AmusementSessionRecord saveSession(AmusementSessionRecord session);

    List<AmusementSessionRecord> listSessions();

    Optional<AmusementSessionRecord> findSessionById(String id);

    boolean incrementSessionUsedCapacityIfAvailable(String sessionId);

    AmusementSafetyInspectionRecord saveSafetyInspection(AmusementSafetyInspectionRecord inspection);

    List<AmusementSafetyInspectionRecord> listSafetyInspections();

    Optional<AmusementSafetyInspectionRecord> findSafetyInspectionById(String id);

    Optional<AmusementSafetyInspectionRecord> latestSafetyInspectionForProject(String projectId);

    AmusementSafetyActionRecord saveSafetyAction(AmusementSafetyActionRecord action);

    AmusementVerificationRecord saveVerification(AmusementVerificationRecord verification);

    List<AmusementVerificationRecord> listVerifications();

    AmusementManualOverrideRecord saveManualOverride(AmusementManualOverrideRecord override);

    List<AmusementManualOverrideRecord> listManualOverrides();

    AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status);

    List<AmusementDeviceStatusRecord> listDeviceStatuses();

    AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry);

    List<AmusementQueueEntryRecord> listQueueEntries();

    Optional<AmusementQueueEntryRecord> findQueueEntryById(String id);

    AmusementReservationRecord saveReservation(AmusementReservationRecord reservation);

    List<AmusementReservationRecord> listReservations();

    Optional<AmusementReservationRecord> findReservationById(String id);
}
