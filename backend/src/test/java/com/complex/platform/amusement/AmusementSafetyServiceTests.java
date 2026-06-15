package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.amusement.dto.AmusementIncidentActionRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCloseRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCreateRequest;
import com.complex.platform.amusement.dto.AmusementSafetyNoticeCreateRequest;
import com.complex.platform.amusement.dto.AmusementWaiverAcknowledgementRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmusementSafetyServiceTests {

    private InMemoryAmusementSafetyRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AmusementSafetyService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAmusementSafetyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new AmusementSafetyService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void createSafetyNoticeRequiresCoreFieldsAndWritesFailureAudit() {
        assertThatThrownBy(() -> service.createSafetyNotice(new AmusementSafetyNoticeCreateRequest(
                "notice-bad",
                "missing project",
                List.of(),
                " ",
                "amu_project_1",
                "safety_notice",
                "Rider safety",
                "v1",
                "Read before riding",
                "active",
                Instant.parse("2026-06-15T00:00:00Z"),
                null,
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.safety_notice.create_failed");
    }

    @Test
    void createSafetyNoticePersistsRecordAndAuditEvent() {
        AmusementSafetyNoticeRecord created = service.createSafetyNotice(noticeRequest("notice-create"));

        assertThat(created.id()).startsWith("asn_");
        assertThat(created.projectId()).isEqualTo("prj_1");
        assertThat(created.status()).isEqualTo("active");
        assertThat(repository.noticeSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.safety_notice.created");
    }

    @Test
    void acknowledgeWaiverRejectsMissingNotice() {
        assertThatThrownBy(() -> service.acknowledgeWaiver("notice_missing", waiverRequest("waiver-missing")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SAFETY_NOTICE_NOT_FOUND"));
    }

    @Test
    void acknowledgeWaiverPersistsRecordAndAuditEvent() {
        AmusementSafetyNoticeRecord notice = service.createSafetyNotice(noticeRequest("notice-waiver"));

        AmusementWaiverAcknowledgementRecord acknowledgement = service.acknowledgeWaiver(
                notice.id(),
                waiverRequest("waiver-create")
        );

        assertThat(acknowledgement.id()).startsWith("awa_");
        assertThat(acknowledgement.noticeId()).isEqualTo(notice.id());
        assertThat(acknowledgement.contentVersion()).isEqualTo("v1");
        assertThat(repository.acknowledgementSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.waiver.acknowledged");
    }

    @Test
    void createIncidentPersistsInitialActionAndAuditEvent() {
        AmusementIncidentRecord incident = service.createIncident(incidentRequest("incident-create"));

        assertThat(incident.id()).startsWith("ami_");
        assertThat(incident.status()).isEqualTo("created");
        assertThat(incident.severity()).isEqualTo("high");
        assertThat(repository.actions).hasSize(1);
        assertThat(repository.actions.getFirst().actionType()).isEqualTo("created");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.incident.created");
    }

    @Test
    void addIncidentActionChangesStatusAndRecordsAction() {
        AmusementIncidentRecord incident = service.createIncident(incidentRequest("incident-action-create"));

        AmusementIncidentRecord updated = service.addIncidentAction(incident.id(), new AmusementIncidentActionRequest(
                "incident-action",
                "ops handling",
                List.of("att-action"),
                "processing",
                "staff_ops"
        ));

        assertThat(updated.status()).isEqualTo("processing");
        assertThat(repository.actions)
                .extracting(AmusementIncidentActionRecord::actionType)
                .contains("created", "status_changed");
        assertThat(repository.actions.getLast().fromStatus()).isEqualTo("created");
        assertThat(repository.actions.getLast().toStatus()).isEqualTo("processing");
    }

    @Test
    void closeIncidentRequiresResolutionAndRejectsLaterAction() {
        AmusementIncidentRecord incident = service.createIncident(incidentRequest("incident-close-create"));

        assertThatThrownBy(() -> service.closeIncident(incident.id(), new AmusementIncidentCloseRequest(
                "incident-close-bad",
                "close",
                List.of(),
                " ",
                "staff_manager"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        AmusementIncidentRecord closed = service.closeIncident(incident.id(), new AmusementIncidentCloseRequest(
                "incident-close",
                "close",
                List.of("att-close"),
                "Handled, documented, and customer contacted",
                "staff_manager"
        ));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.resolution()).isEqualTo("Handled, documented, and customer contacted");

        assertThatThrownBy(() -> service.addIncidentAction(incident.id(), new AmusementIncidentActionRequest(
                "incident-after-close",
                "try to update closed incident",
                List.of(),
                "processing",
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("INCIDENT_CLOSED"));
    }

    @Test
    void duplicateSafetyNoticeIdempotencyKeyWithSameRequestReturnsFirstResult() {
        AmusementSafetyNoticeCreateRequest request = noticeRequest("notice-once");

        AmusementSafetyNoticeRecord first = service.createSafetyNotice(request);
        AmusementSafetyNoticeRecord second = service.createSafetyNotice(request);

        assertThat(second).isEqualTo(first);
        assertThat(repository.noticeSaveCount).isEqualTo(1);
    }

    @Test
    void validationFailureDoesNotReserveIdempotencyKey() {
        assertThatThrownBy(() -> service.createIncident(new AmusementIncidentCreateRequest(
                "incident-retry",
                "missing title",
                List.of(),
                "prj_1",
                "amu_project_1",
                "session_1",
                "device_1",
                "customer_1",
                "high",
                " ",
                "Guest slipped near entrance",
                "staff_reporter",
                "staff_app",
                Instant.parse("2026-06-15T09:00:00Z")
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        AmusementIncidentRecord retry = service.createIncident(incidentRequest("incident-retry"));

        assertThat(retry.status()).isEqualTo("created");
    }

    private AmusementSafetyNoticeCreateRequest noticeRequest(String idempotencyKey) {
        return new AmusementSafetyNoticeCreateRequest(
                idempotencyKey,
                "create safety notice",
                List.of("att-notice"),
                "prj_1",
                "amu_project_1",
                "safety_notice",
                "Rider safety",
                "v1",
                "Read before riding",
                "active",
                Instant.parse("2026-06-15T00:00:00Z"),
                null,
                "staff_ops"
        );
    }

    private AmusementWaiverAcknowledgementRequest waiverRequest(String idempotencyKey) {
        return new AmusementWaiverAcknowledgementRequest(
                idempotencyKey,
                "visitor acknowledged",
                List.of("att-signature"),
                "prj_1",
                "amu_project_1",
                "session_1",
                "customer_1",
                "Visitor Zhang",
                "Parent Zhang",
                "onsite_tablet",
                "v1",
                "sha256:notice-v1",
                Instant.parse("2026-06-15T08:30:00Z"),
                "staff_gate"
        );
    }

    private AmusementIncidentCreateRequest incidentRequest(String idempotencyKey) {
        return new AmusementIncidentCreateRequest(
                idempotencyKey,
                "create incident",
                List.of("att-photo"),
                "prj_1",
                "amu_project_1",
                "session_1",
                "device_1",
                "customer_1",
                "high",
                "Guest slipped",
                "Guest slipped near entrance",
                "staff_reporter",
                "staff_app",
                Instant.parse("2026-06-15T09:00:00Z")
        );
    }

    private static final class InMemoryAmusementSafetyRepository implements AmusementSafetyRepository {
        private final Map<String, AmusementSafetyNoticeRecord> notices = new LinkedHashMap<>();
        private final Map<String, AmusementWaiverAcknowledgementRecord> acknowledgements = new LinkedHashMap<>();
        private final Map<String, AmusementIncidentRecord> incidents = new LinkedHashMap<>();
        private final List<AmusementIncidentActionRecord> actions = new ArrayList<>();
        private int noticeSaveCount;
        private int acknowledgementSaveCount;
        private int incidentSaveCount;

        @Override
        public List<AmusementSafetyNoticeRecord> findAllSafetyNotices() {
            return new ArrayList<>(notices.values());
        }

        @Override
        public Optional<AmusementSafetyNoticeRecord> findSafetyNoticeById(String id) {
            return Optional.ofNullable(notices.get(id));
        }

        @Override
        public AmusementSafetyNoticeRecord saveSafetyNotice(AmusementSafetyNoticeRecord record) {
            notices.put(record.id(), record);
            noticeSaveCount++;
            return record;
        }

        @Override
        public AmusementWaiverAcknowledgementRecord saveWaiverAcknowledgement(AmusementWaiverAcknowledgementRecord record) {
            acknowledgements.put(record.id(), record);
            acknowledgementSaveCount++;
            return record;
        }

        @Override
        public List<AmusementIncidentRecord> findAllIncidents() {
            return new ArrayList<>(incidents.values());
        }

        @Override
        public Optional<AmusementIncidentRecord> findIncidentById(String id) {
            return Optional.ofNullable(incidents.get(id));
        }

        @Override
        public AmusementIncidentRecord saveIncident(AmusementIncidentRecord record) {
            incidents.put(record.id(), record);
            incidentSaveCount++;
            return record;
        }

        @Override
        public AmusementIncidentActionRecord saveIncidentAction(AmusementIncidentActionRecord record) {
            actions.add(record);
            return record;
        }

        @Override
        public List<AmusementIncidentActionRecord> findIncidentActionsByIncidentId(String incidentId) {
            return actions.stream()
                    .filter(action -> action.incidentId().equals(incidentId))
                    .toList();
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
