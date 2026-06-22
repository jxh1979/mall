package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import org.mockito.ArgumentCaptor;
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
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmusementSafetyServiceTests {

    private InMemoryAmusementSafetyRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AmusementSafetyService service;
    private AuthorizationService authorizationService;
    private RiskAlertService riskAlertService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAmusementSafetyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        authorizationService = new AuthorizationService(mock(AccessRepository.class));
        riskAlertService = mock(RiskAlertService.class);
        TestSecurityActors.asPlatformAdmin();
        service = new AmusementSafetyService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                riskAlertService
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
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
    void acknowledgeWaiverRejectsCrossMerchantNotice() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyNoticeRecord notice = service.createSafetyNotice(noticeRequest("notice-xmerchant"));

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.acknowledgeWaiver(notice.id(), waiverRequest("waiver-xmerchant")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
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
    void highSeverityIncidentAutoPausesOpenProject() {
        // 事故停运联动：high 事故对处于 open 的项目自动停运停售（open→paused）。
        repository.seedProjectOpenStatus("prj_1", "open");
        service.createIncident(incidentRequest("incident-autopause")); // severity=high, projectId=prj_1
        assertThat(repository.projectOpenStatus.get("prj_1")).isEqualTo("paused");
        assertThat(auditEventRepository.events)
                .filteredOn(e -> "amusement.incident.created".equals(e.eventName()))
                .isNotEmpty();
    }

    @Test
    void lowSeverityIncidentDoesNotPauseProject() {
        // 低风险事故不自动停运（仅 high/critical 停运）。
        repository.seedProjectOpenStatus("prj_low", "open");
        service.createIncident(new AmusementIncidentCreateRequest(
                "incident-low", "create incident", List.of("att-photo"),
                "prj_low", "amu_project_1", "session_1", "device_1", "customer_1",
                "low", "Minor", "Minor issue", "staff_reporter", "staff_app",
                Instant.parse("2026-06-15T09:00:00Z")));
        assertThat(repository.projectOpenStatus.get("prj_low")).isEqualTo("open");
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

    // ─────────────────────────────────────────────────────────────────────────
    // 统一责任链双写（Wave 2）：安全事件创建后镜像到统一 risk_alerts。
    // source_domain='amusement'、alert_type='safety_incident'；severity→risk_level 映射。
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createIncidentDoubleWritesUnifiedRiskAlertKeepingHighLevel() {
        // 默认 incidentRequest severity=high → 统一表 risk_level 保持 high。
        AmusementIncidentRecord incident = service.createIncident(incidentRequest("incident-unified-high"));

        ArgumentCaptor<RiskAlertReportRequest> captor = ArgumentCaptor.forClass(RiskAlertReportRequest.class);
        verify(riskAlertService, atLeastOnce()).report(captor.capture());
        RiskAlertReportRequest req = captor.getValue();
        assertThat(req.sourceDomain()).isEqualTo("amusement");
        assertThat(req.alertType()).isEqualTo("safety_incident");
        assertThat(req.riskLevel()).isEqualTo("high");
        // sourceRef = incident id；幂等键稳定唯一。
        assertThat(req.sourceRef()).isEqualTo(incident.id());
        assertThat(req.idempotencyKey()).isEqualTo(incident.id() + ":unified");
    }

    @Test
    void createIncidentMapsMediumSeverityToSuspicious() {
        // severity=medium → 统一表 risk_level 必须映射为 suspicious（否则统一表校验失败丢数据）。
        service.createIncident(new AmusementIncidentCreateRequest(
                "incident-unified-medium",
                "create incident",
                List.of("att-photo"),
                "prj_1",
                "amu_project_1",
                "session_1",
                "device_1",
                "customer_1",
                "medium",
                "Guest minor scrape",
                "Guest minor scrape near queue",
                "staff_reporter",
                "staff_app",
                Instant.parse("2026-06-15T09:00:00Z")));

        ArgumentCaptor<RiskAlertReportRequest> captor = ArgumentCaptor.forClass(RiskAlertReportRequest.class);
        verify(riskAlertService, atLeastOnce()).report(captor.capture());
        assertThat(captor.getValue().riskLevel()).isEqualTo("suspicious");
    }

    @Test
    void createIncidentStillWritesIslandWhenUnifiedServiceAbsent() {
        // fail-safe：未注入统一责任链服务（riskAlertService=null），创建主流程照常、孤岛 incident 照写。
        AmusementSafetyService noUnified = new AmusementSafetyService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService);

        AmusementIncidentRecord incident = noUnified.createIncident(incidentRequest("incident-null-unified"));

        assertThat(incident.incidentType()).isEqualTo("accident");
        assertThat(repository.findIncidentById(incident.id())).isPresent();
    }

    @Test
    void createIncidentUnifiedReportFailureDoesNotBreakIslandWrite() {
        // fail-safe：统一表 report 抛 RuntimeException，创建主流程不受影响、孤岛 incident 照写。
        when(riskAlertService.report(any())).thenThrow(new RuntimeException("unified down"));

        AmusementIncidentRecord incident = service.createIncident(incidentRequest("incident-unified-fail"));

        assertThat(repository.findIncidentById(incident.id())).isPresent();
        verify(riskAlertService, atLeastOnce()).report(any());
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
        private final Map<String, String> projectOpenStatus = new LinkedHashMap<>();
        private int noticeSaveCount;
        private int acknowledgementSaveCount;
        private int incidentSaveCount;

        void seedProjectOpenStatus(String projectId, String openStatus) {
            projectOpenStatus.put(projectId, openStatus);
        }

        @Override
        public boolean updateProjectOpenStatus(String projectId, String expectedCurrentStatus, String newStatus) {
            if (!expectedCurrentStatus.equals(projectOpenStatus.get(projectId))) {
                return false;
            }
            projectOpenStatus.put(projectId, newStatus);
            return true;
        }

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

        @Override
        public List<AmusementSafetyNoticeRecord> findSafetyNoticesByMerchant(String merchantId) {
            return notices.values().stream()
                    .filter(n -> merchantId.equals(n.merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementIncidentRecord> findIncidentsByMerchant(String merchantId) {
            return incidents.values().stream()
                    .filter(i -> merchantId.equals(i.merchantId()))
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
