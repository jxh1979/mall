package com.complex.platform.evidence;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
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

class EvidenceEventServiceTests {

    private InMemoryEvidenceEventRepository evidenceEventRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AuthorizationService authorizationService;
    private EvidenceEventService service;

    @BeforeEach
    void setUp() {
        evidenceEventRepository = new InMemoryEvidenceEventRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        // Default actor: platform admin — unrestricted scope, sees all
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        when(authorizationService.isPlatformAdmin(any())).thenReturn(true);
        // Set up SecurityContextHolder so AuditEventService can read actorId directly
        TestSecurityActors.asPlatformAdmin();

        service = new EvidenceEventService(
                evidenceEventRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    // ─── P0-1: audit actorId must be recorded from security context ───────────

    @Test
    void auditEventRecordsActorIdFromSecurityContext() {
        service.listEvents(1, 10);

        AuditEvent auditEvent = auditEventRepository.events.getLast();
        assertThat(auditEvent.actorId())
                .as("actorId must be populated from SecurityContextHolder, never null")
                .isEqualTo("usr_admin");
        assertThat(auditEvent.authContext())
                .as("authContext must be populated from SecurityContextHolder, never null or hardcoded")
                .isNotNull()
                .isNotEqualTo("staff-token");
    }

    // ─── P0-2: listEvents must be scoped to the caller's merchant ─────────────

    @Test
    void listEventsFiltersToMerchantScope() {
        // Merchant A creates an event
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_m_a", "user", "staff", "org_1", "m_a", "active"));
        service.createEvents(new EventBatchRequest(
                "events-list-a", "report", List.of(), "DEVICE-A", null,
                List.of(event("terminal.login", "merchant-a-event"))
        ));

        // Merchant B creates an event
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_m_b", "user", "staff", "org_1", "m_b", "active"));
        service.createEvents(new EventBatchRequest(
                "events-list-b", "report", List.of(), "DEVICE-B", null,
                List.of(event("terminal.logout", "merchant-b-event"))
        ));

        // Merchant A lists — must only see their own event
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        List<EvidenceEventRecord> merchantAEvents = service.listEvents(1, 10).items();
        assertThat(merchantAEvents)
                .extracting(EvidenceEventRecord::eventType)
                .containsExactly("terminal.login");

        // Platform admin lists — sees all
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        List<EvidenceEventRecord> allEvents = service.listEvents(1, 10).items();
        assertThat(allEvents).hasSize(2);
    }

    // ─── P0-3: bindAttachment/completeAttachment must enforce ownership ────────

    @Test
    void bindAttachmentRejectsAttachmentOwnedByDifferentMerchant() {
        // Merchant A creates an attachment
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_m_a", "user", "staff", "org_1", "m_a", "active"));
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-own-a", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_a")
        );

        // Merchant B attempts to bind merchant A's attachment
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_m_b", "user", "staff", "org_1", "m_b", "active"));

        assertThatThrownBy(() -> service.bindAttachment(intent.id(),
                new EvidenceAttachmentBindRequest("bind-cross-merchant", "contract", "ctr_b", "contract")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void completeAttachmentRejectsAttachmentOwnedByDifferentMerchant() {
        // Merchant A creates an attachment
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_m_a", "user", "staff", "org_1", "m_a", "active"));
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-complete-a", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_a")
        );

        // Merchant B attempts to complete merchant A's attachment
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_m_b", "user", "staff", "org_1", "m_b", "active"));

        assertThatThrownBy(() -> service.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("complete-cross-merchant", "evidence/other.pdf", "sha256:bad", null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // ─── P0-4: idempotency key must be scoped to the current actor ────────────

    @Test
    void idempotencyKeyIsActorScoped() {
        // Actor A creates events
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_actor_a", "a", "staff", "org_1", null, "active"));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        service.createEvents(new EventBatchRequest(
                "shared-idem-key", "report", List.of(), "DEVICE-A", null,
                List.of(event("terminal.login", "actor-a-event"))
        ));

        // Actor B uses the same idempotency key — must get independent execution, not Actor A's result
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_actor_b", "b", "staff", "org_1", null, "active"));
        service.createEvents(new EventBatchRequest(
                "shared-idem-key", "report", List.of(), "DEVICE-B", null,
                List.of(event("terminal.logout", "actor-b-event-1"), event("terminal.exception", "actor-b-event-2"))
        ));

        // Both actors' events must be stored (Actor B's action ran independently)
        assertThat(evidenceEventRepository.events)
                .extracting(EvidenceEventRecord::eventType)
                .containsExactlyInAnyOrder("terminal.login", "terminal.logout", "terminal.exception");
    }

    // ─── P1-2: state machine guards on bind/complete ──────────────────────────

    @Test
    void bindAttachmentRejectsWhenNotPending() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-state", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_1")
        );
        service.bindAttachment(intent.id(),
                new EvidenceAttachmentBindRequest("bind-state-1", "contract", "ctr_1", "contract"));

        // Attempting to bind an already-bound attachment must fail
        assertThatThrownBy(() -> service.bindAttachment(intent.id(),
                new EvidenceAttachmentBindRequest("bind-state-2", "contract", "ctr_1", "contract")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_STATE");
    }

    @Test
    void completeAttachmentRejectsWhenAlreadyUploaded() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-complete-state", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_1")
        );
        service.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("complete-state-1", "evidence/ctr_1.pdf", "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", null));

        // Attempting to complete an already-uploaded attachment must fail
        assertThatThrownBy(() -> service.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("complete-state-2", "evidence/evil.pdf", "sha256:evil", null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_STATE");
    }

    @Test
    void completeAttachmentRejectsMalformedChecksum() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-bad-checksum", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_1")
        );

        // A garbage / non-sha256 integrity token must not be accepted as tamper-evidence
        assertThatThrownBy(() -> service.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("complete-bad-checksum", "evidence/ctr_1.pdf", "not-a-digest", null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("EVIDENCE_CHECKSUM_INVALID");

        // The attachment must remain un-uploaded after the rejected completion
        assertThat(evidenceEventRepository.findAttachmentById(intent.id()))
                .get().extracting(EvidenceAttachmentRecord::status).isEqualTo("pending");
    }

    @Test
    void completeAttachmentNormalizesUppercaseChecksumToLowercase() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-upper-checksum", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_1")
        );

        EvidenceAttachmentRecord completed = service.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("complete-upper-checksum", "evidence/ctr_1.pdf",
                        "SHA256:E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855", null));

        assertThat(completed.checksum())
                .isEqualTo("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    // ─── P1-3: createEvidenceReview must validate attachment IDs ──────────────

    @Test
    void createReviewRejectsNonExistentAttachmentId() {
        assertThatThrownBy(() -> service.createEvidenceReview(new EvidenceReviewRequest(
                "review-bad-att", "review", List.of("att_does_not_exist"),
                "contract", "ctr_1", "pass", null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void createReviewRejectsAttachmentWithWrongBusinessObject() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-review-wrong", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_other")
        );
        service.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("complete-review-wrong", "evidence/other.pdf", "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", null));

        assertThatThrownBy(() -> service.createEvidenceReview(new EvidenceReviewRequest(
                "review-wrong-biz", "review", List.of(intent.id()),
                "contract", "ctr_1", "pass", null  // different businessId than attachment
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void createReviewRejectsAttachmentNotYetUploaded() {
        EvidenceAttachmentRecord intent = service.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("intent-review-pending", "doc.pdf", "application/pdf", 1024L, "contract", "ctr_1")
        );
        // Attachment is still in "pending" state — not yet uploaded

        assertThatThrownBy(() -> service.createEvidenceReview(new EvidenceReviewRequest(
                "review-pending-att", "review", List.of(intent.id()),
                "contract", "ctr_1", "pass", null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // ─── existing functional tests (preserved) ────────────────────────────────

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

        List<EvidenceEventRecord> events = service.listEvents(1, 1).items();

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
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                2048L
        ));

        assertThat(intent.status()).isEqualTo("pending");
        assertThat(intent.uploadUrl()).contains(intent.id());
        assertThat(bound.businessType()).isEqualTo("contract");
        assertThat(bound.attachmentRole()).isEqualTo("contract");
        assertThat(completed.status()).isEqualTo("uploaded");
        assertThat(completed.checksum()).isEqualTo("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
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
                "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
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
        public List<EvidenceEventRecord> listLatest(int limit, String merchantId) {
            List<EvidenceEventRecord> newestFirst = new ArrayList<>(events);
            java.util.Collections.reverse(newestFirst);
            return newestFirst.stream()
                    .filter(e -> merchantId == null || merchantId.equals(e.merchantId()))
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
        public List<EvidenceTimelineItem> listTimeline(String objectType, String objectId, String merchantId) {
            List<EvidenceTimelineItem> timeline = new ArrayList<>();
            events.stream()
                    .filter(event -> objectType.equals(event.payload().get("business_type")))
                    .filter(event -> objectId.equals(event.payload().get("business_id")))
                    .filter(event -> merchantId == null || merchantId.equals(event.merchantId()))
                    .map(event -> new EvidenceTimelineItem("evidence_event", event.id(), event.eventType(), event.result(), event.summary(), event.receivedAt()))
                    .forEach(timeline::add);
            attachments.values().stream()
                    .filter(attachment -> objectType.equals(attachment.businessType()))
                    .filter(attachment -> objectId.equals(attachment.businessId()))
                    .filter(attachment -> merchantId == null || merchantId.equals(attachment.merchantId()))
                    .map(attachment -> new EvidenceTimelineItem("attachment", attachment.id(), "evidence.attachment_" + attachment.status(), attachment.status(), attachment.fileName(), attachment.updatedAt()))
                    .forEach(timeline::add);
            reviews.values().stream()
                    .filter(review -> objectType.equals(review.businessType()))
                    .filter(review -> objectId.equals(review.businessId()))
                    .filter(review -> merchantId == null || merchantId.equals(review.merchantId()))
                    .map(review -> new EvidenceTimelineItem("review", review.id(), "evidence.review_created", review.decision(), review.comment(), review.createdAt()))
                    .forEach(timeline::add);
            timeline.add(new EvidenceTimelineItem("audit", "audit-memory", "evidence.upload_intent_created", "success", "audit memory", Instant.now()));
            return timeline;
        }
    }
}
