package com.complex.platform.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.dto.EvidenceAttachmentBindRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentCompleteRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentUploadIntentRequest;
import com.complex.platform.evidence.dto.EvidenceReviewRequest;
import com.complex.platform.evidence.dto.EventBatchRequest;
import com.complex.platform.evidence.dto.TerminalEventItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceEventServiceTests {

    private InMemoryEvidenceEventRepository evidenceEventRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private EvidenceEventService service;

    @BeforeEach
    void setUp() {
        evidenceEventRepository = new InMemoryEvidenceEventRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new EvidenceEventService(
                evidenceEventRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void eventBatchStoresAllEventsAndWritesOpenApiAuditEvent() {
        EventBatchRequest request = new EventBatchRequest(
                "events-report-once",
                "report",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(
                        event("terminal.login", "login"),
                        event("terminal.verification.online", "verification")
                )
        );

        EventBatchResponse first = service.createEvents(request);
        EventBatchResponse second = service.createEvents(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.accepted()).isEqualTo(2);
        assertThat(evidenceEventRepository.events)
                .extracting(EvidenceEventRecord::eventType)
                .containsExactly("terminal.login", "terminal.verification.online");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("evidence.events_reported");
    }

    @Test
    void eventBatchWithEmptyEventsReturnsValidationFailed() {
        assertThatThrownBy(() -> service.createEvents(new EventBatchRequest(
                "events-empty",
                "report",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of()
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("evidence.events_report_failed");
    }

    @Test
    void listEventsReturnsLatestEventsAndWritesOpenApiAuditEvent() {
        service.createEvents(new EventBatchRequest(
                "events-list",
                "report",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(event("terminal.login", "login"), event("terminal.logout", "logout"))
        ));

        List<EvidenceEventRecord> events = service.listEvents(1);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().eventType()).isEqualTo("terminal.logout");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("evidence.event_viewed");
    }

    @Test
    void attachmentUploadBindAndCompletePersistLifecycleAndAuditEvents() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(new EvidenceAttachmentUploadIntentRequest(
                "attachment-intent",
                "contract.pdf",
                "application/pdf",
                1024L,
                "contract",
                "ctr_1"
        ));
        EvidenceAttachmentRecord bound = service.bindAttachment(intent.id(), new EvidenceAttachmentBindRequest(
                "attachment-bind",
                "contract",
                "ctr_1",
                "contract"
        ));
        EvidenceAttachmentRecord completed = service.completeAttachment(intent.id(), new EvidenceAttachmentCompleteRequest(
                "attachment-complete",
                "evidence/contracts/ctr_1.pdf",
                "sha256:abc",
                2048L
        ));

        assertThat(intent.status()).isEqualTo("pending");
        assertThat(intent.uploadUrl()).contains(intent.id());
        assertThat(bound.businessType()).isEqualTo("contract");
        assertThat(bound.attachmentRole()).isEqualTo("contract");
        assertThat(completed.status()).isEqualTo("uploaded");
        assertThat(completed.checksum()).isEqualTo("sha256:abc");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("evidence.upload_intent_created", "evidence.attachment_bound", "evidence.attachment_completed");
    }

    @Test
    void reviewAndTimelineIncludeAuditEvidenceAttachmentAndReviewItems() {
        service.createEvents(new EventBatchRequest(
                "events-timeline",
                "report timeline",
                List.of(),
                "P0-ANDROID-1",
                "staff_1",
                List.of(new TerminalEventItemRequest(
                        "terminal.exception",
                        "android_terminal",
                        "success",
                        "exception evidence",
                        Map.of("business_type", "contract", "business_id", "ctr_1"),
                        Instant.parse("2026-06-14T00:00:00Z")
                ))
        ));
        EvidenceAttachmentRecord attachment = service.createAttachmentUploadIntent(new EvidenceAttachmentUploadIntentRequest(
                "timeline-attachment",
                "contract.pdf",
                "application/pdf",
                1024L,
                "contract",
                "ctr_1"
        ));
        service.completeAttachment(attachment.id(), new EvidenceAttachmentCompleteRequest(
                "timeline-attachment-complete",
                "evidence/contracts/ctr_1.pdf",
                "sha256:timeline",
                1024L
        ));
        service.createEvidenceReview(new EvidenceReviewRequest(
                "review-create",
                "review",
                List.of(attachment.id()),
                "contract",
                "ctr_1",
                "pass",
                "complete"
        ));

        List<EvidenceTimelineItem> timeline = service.getTimeline("contract", "ctr_1");

        assertThat(timeline)
                .extracting(EvidenceTimelineItem::itemType)
                .contains("audit", "evidence_event", "attachment", "review");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("evidence.review_created", "evidence.timeline_viewed");
    }

    private TerminalEventItemRequest event(String eventType, String summary) {
        return new TerminalEventItemRequest(
                eventType,
                "android_terminal",
                "success",
                summary,
                Map.of("source", "test"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private static final class InMemoryEvidenceEventRepository implements EvidenceEventRepository {
        private final List<EvidenceEventRecord> events = new ArrayList<>();
        private final Map<String, EvidenceAttachmentRecord> attachments = new LinkedHashMap<>();
        private final Map<String, EvidenceReviewRecord> reviews = new LinkedHashMap<>();

        @Override
        public List<EvidenceEventRecord> saveAll(List<EvidenceEventRecord> records) {
            events.addAll(records);
            return records;
        }

        @Override
        public List<EvidenceEventRecord> listLatest(int limit) {
            List<EvidenceEventRecord> newestFirst = new ArrayList<>(events);
            java.util.Collections.reverse(newestFirst);
            return newestFirst.stream()
                    .limit(limit)
                    .toList();
        }

        @Override
        public EvidenceAttachmentRecord saveAttachment(EvidenceAttachmentRecord attachment) {
            attachments.put(attachment.id(), attachment);
            return attachment;
        }

        @Override
        public Optional<EvidenceAttachmentRecord> findAttachmentById(String id) {
            return Optional.ofNullable(attachments.get(id));
        }

        @Override
        public EvidenceReviewRecord saveReview(EvidenceReviewRecord review) {
            reviews.put(review.id(), review);
            return review;
        }

        @Override
        public List<EvidenceTimelineItem> listTimeline(String objectType, String objectId) {
            List<EvidenceTimelineItem> timeline = new ArrayList<>();
            events.stream()
                    .filter(event -> objectType.equals(event.payload().get("business_type")))
                    .filter(event -> objectId.equals(event.payload().get("business_id")))
                    .map(event -> new EvidenceTimelineItem("evidence_event", event.id(), event.eventType(), event.result(), event.summary(), event.receivedAt()))
                    .forEach(timeline::add);
            attachments.values().stream()
                    .filter(attachment -> objectType.equals(attachment.businessType()))
                    .filter(attachment -> objectId.equals(attachment.businessId()))
                    .map(attachment -> new EvidenceTimelineItem("attachment", attachment.id(), "evidence.attachment_" + attachment.status(), attachment.status(), attachment.fileName(), attachment.updatedAt()))
                    .forEach(timeline::add);
            reviews.values().stream()
                    .filter(review -> objectType.equals(review.businessType()))
                    .filter(review -> objectId.equals(review.businessId()))
                    .map(review -> new EvidenceTimelineItem("review", review.id(), "evidence.review_created", review.decision(), review.comment(), review.createdAt()))
                    .forEach(timeline::add);
            timeline.add(new EvidenceTimelineItem("audit", "audit-memory", "evidence.upload_intent_created", "success", "audit memory", Instant.now()));
            return timeline;
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
