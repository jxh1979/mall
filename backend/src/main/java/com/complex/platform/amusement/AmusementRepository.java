package com.complex.platform.amusement;

import java.util.List;
import java.util.Optional;

public interface AmusementRepository {
    AmusementSafetyTemplateRecord saveSafetyTemplate(AmusementSafetyTemplateRecord template);

    List<AmusementSafetyTemplateRecord> listSafetyTemplates();

    List<AmusementSafetyTemplateRecord> listSafetyTemplatesByMerchant(String merchantId);

    Optional<AmusementSafetyTemplateRecord> findSafetyTemplateById(String id);

    AmusementProjectRecord saveProject(AmusementProjectRecord project);

    List<AmusementProjectRecord> listProjects();

    List<AmusementProjectRecord> listProjectsByMerchant(String merchantId);

    Optional<AmusementProjectRecord> findProjectById(String id);

    AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule);

    List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId);

    AmusementSessionRecord saveSession(AmusementSessionRecord session);

    List<AmusementSessionRecord> listSessions();

    List<AmusementSessionRecord> listSessionsByMerchant(String merchantId);

    Optional<AmusementSessionRecord> findSessionById(String id);

    boolean incrementSessionUsedCapacityIfAvailable(String sessionId);

    boolean lockSessionCapacityIfAvailable(String sessionId, int capacityToLock);

    void releaseSessionLockedCapacity(String sessionId, int capacityToRelease);

    AmusementGroupBookingRecord saveGroupBooking(AmusementGroupBookingRecord booking);

    List<AmusementGroupBookingRecord> listGroupBookings();

    List<AmusementGroupBookingRecord> listGroupBookingsByMerchant(String merchantId);

    Optional<AmusementGroupBookingRecord> findGroupBookingById(String id);

    Optional<AmusementGroupBookingRecord> approveGroupBookingWithCapacityLock(
            String id,
            String approvalNote,
            List<String> evidenceAttachmentIds,
            java.time.Instant approvedAt
    );

    Optional<AmusementGroupBookingRecord> releaseGroupBookingAndCapacity(
            String id,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            java.time.Instant releasedAt
    );

    AmusementSafetyInspectionRecord saveSafetyInspection(AmusementSafetyInspectionRecord inspection);

    List<AmusementSafetyInspectionRecord> listSafetyInspections();

    List<AmusementSafetyInspectionRecord> listSafetyInspectionsByMerchant(String merchantId);

    Optional<AmusementSafetyInspectionRecord> findSafetyInspectionById(String id);

    Optional<AmusementSafetyInspectionRecord> latestSafetyInspectionForProject(String projectId);

    AmusementSafetyActionRecord saveSafetyAction(AmusementSafetyActionRecord action);

    AmusementVerificationRecord saveVerification(AmusementVerificationRecord verification);

    List<AmusementVerificationRecord> listVerifications();

    List<AmusementVerificationRecord> listVerificationsByMerchant(String merchantId);

    AmusementManualOverrideRecord saveManualOverride(AmusementManualOverrideRecord override);

    List<AmusementManualOverrideRecord> listManualOverrides();

    List<AmusementManualOverrideRecord> listManualOverridesByMerchant(String merchantId);

    AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status);

    List<AmusementDeviceStatusRecord> listDeviceStatuses();

    List<AmusementDeviceStatusRecord> listDeviceStatusesByMerchant(String merchantId);

    AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry);

    List<AmusementQueueEntryRecord> listQueueEntries();

    List<AmusementQueueEntryRecord> listQueueEntriesByMerchant(String merchantId);

    Optional<AmusementQueueEntryRecord> findQueueEntryById(String id);

    AmusementReservationRecord saveReservation(AmusementReservationRecord reservation);

    List<AmusementReservationRecord> listReservations();

    List<AmusementReservationRecord> listReservationsByMerchant(String merchantId);

    Optional<AmusementReservationRecord> findReservationById(String id);
}
