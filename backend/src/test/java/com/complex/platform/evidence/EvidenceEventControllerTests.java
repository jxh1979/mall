package com.complex.platform.evidence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.evidence.dto.EvidenceAttachmentBindRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentCompleteRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentUploadIntentRequest;
import com.complex.platform.evidence.dto.EvidenceReviewRequest;
import com.complex.platform.evidence.dto.EventBatchRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EvidenceEventController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, EvidenceEventControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class EvidenceEventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EvidenceEventService service;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private TraceabilityService traceabilityService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService, traceabilityService);
    }

    @Test
    void reportEventsMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/evidence/events")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "device_code": "P0-ANDROID-1",
                                  "staff_id": "staff_1",
                                  "events": [
                                    {
                                      "event_type": "terminal.login",
                                      "summary": "login",
                                      "occurred_at": "2026-06-14T00:00:00Z"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    @Test
    void reportEventsUsesOpenApiCreatePermissionAndReturnsAcceptedCount() throws Exception {
        when(service.createEvents(any(EventBatchRequest.class)))
                .thenReturn(new EventBatchResponse(1, Instant.parse("2026-06-14T00:00:01Z")));

        mockMvc.perform(post("/api/v1/evidence/events")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "events-report",
                                  "device_code": "P0-ANDROID-1",
                                  "staff_id": "staff_1",
                                  "events": [
                                    {
                                      "event_type": "terminal.login",
                                      "event_category": "android_terminal",
                                      "result": "success",
                                      "summary": "login",
                                      "payload": {"mode": "online"},
                                      "occurred_at": "2026-06-14T00:00:00Z"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(1));

        verify(authorizationService).requirePermission("evidence.event.create");
    }

    @Test
    void listEventsUsesOpenApiViewPermissionAndAndroidListShape() throws Exception {
        when(service.listEvents(20)).thenReturn(List.of(new EvidenceEventRecord(
                "evt_1",
                "P0-ANDROID-1",
                "staff_1",
                "terminal.login",
                "android_terminal",
                "success",
                "login",
                Map.of("mode", "online"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:01Z"),
                null
        )));

        mockMvc.perform(get("/api/v1/evidence/events?limit=20")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].event_type").value("terminal.login"))
                .andExpect(jsonPath("$.data[0].merchant_id").doesNotExist());

        verify(authorizationService).requirePermission("evidence.event.view");
    }

    @Test
    void uploadIntentUsesAttachmentCreatePermissionAndRequiresIdempotency() throws Exception {
        when(service.createAttachmentUploadIntent(any(EvidenceAttachmentUploadIntentRequest.class)))
                .thenReturn(new EvidenceAttachmentRecord(
                        "att_1",
                        "contract.pdf",
                        "application/pdf",
                        1024L,
                        "contract",
                        "ctr_1",
                        null,
                        "evidence/pending/att_1/contract.pdf",
                        "https://object-storage.local/upload/att_1",
                        "pending",
                        null,
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z"),
                        null
                ));

        mockMvc.perform(post("/api/v1/evidence/attachments/upload-intents")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "attachment-intent",
                                  "file_name": "contract.pdf",
                                  "content_type": "application/pdf",
                                  "file_size": 1024,
                                  "business_type": "contract",
                                  "business_id": "ctr_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.merchant_id").doesNotExist());

        verify(authorizationService).requirePermission("evidence.attachment.create");
    }

    @Test
    void bindAndCompleteAttachmentUseAttachmentCreatePermission() throws Exception {
        when(service.bindAttachment(eq("att_1"), any(EvidenceAttachmentBindRequest.class)))
                .thenReturn(new EvidenceAttachmentRecord(
                        "att_1",
                        "contract.pdf",
                        "application/pdf",
                        1024L,
                        "contract",
                        "ctr_1",
                        "contract",
                        "evidence/pending/att_1/contract.pdf",
                        "https://object-storage.local/upload/att_1",
                        "bound",
                        null,
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:01:00Z"),
                        null
                ));
        when(service.completeAttachment(eq("att_1"), any(EvidenceAttachmentCompleteRequest.class)))
                .thenReturn(new EvidenceAttachmentRecord(
                        "att_1",
                        "contract.pdf",
                        "application/pdf",
                        2048L,
                        "contract",
                        "ctr_1",
                        "contract",
                        "evidence/contracts/ctr_1.pdf",
                        "https://object-storage.local/upload/att_1",
                        "uploaded",
                        "sha256:abc",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:02:00Z"),
                        null
                ));

        mockMvc.perform(post("/api/v1/evidence/attachments/att_1/bind")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "attachment-bind",
                                  "business_type": "contract",
                                  "business_id": "ctr_1",
                                  "attachment_role": "contract"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachment_role").value("contract"));

        mockMvc.perform(post("/api/v1/evidence/attachments/att_1/complete")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "attachment-complete",
                                  "object_key": "evidence/contracts/ctr_1.pdf",
                                  "checksum": "sha256:abc",
                                  "file_size": 2048
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("uploaded"));

        verify(authorizationService, org.mockito.Mockito.times(2)).requirePermission("evidence.attachment.create");
    }

    @Test
    void reviewAndTimelineUseOpenApiPermissions() throws Exception {
        when(service.createEvidenceReview(any(EvidenceReviewRequest.class)))
                .thenReturn(new EvidenceReviewRecord(
                        "rev_1",
                        "contract",
                        "ctr_1",
                        "medium",
                        "pass",
                        "complete",
                        List.of("att_1"),
                        "created",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z"),
                        null
                ));
        when(service.getTimeline("contract", "ctr_1"))
                .thenReturn(List.of(new EvidenceTimelineItem(
                        "review",
                        "rev_1",
                        "evidence.review_created",
                        "pass",
                        "complete",
                        Instant.parse("2026-06-14T00:00:00Z")
                )));

        mockMvc.perform(post("/api/v1/evidence/reviews")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "review-create",
                                  "business_type": "contract",
                                  "business_id": "ctr_1",
                                  "review_result": "pass",
                                  "comment": "complete",
                                  "evidence_attachment_ids": ["att_1"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("pass"))
                .andExpect(jsonPath("$.data.merchant_id").doesNotExist());

        mockMvc.perform(get("/api/v1/evidence/timelines/contract/ctr_1")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].item_type").value("review"));

        verify(authorizationService).requirePermission("evidence.review.create");
        verify(authorizationService).requirePermission("evidence.timeline.view");
    }

    @Test
    void autoCaptureRateUsesTimelinePermissionAndReturnsThresholdResult() throws Exception {
        when(traceabilityService.autoCaptureRate(80))
                .thenReturn(new TraceabilityAutoCaptureRate(10, 9, 90.0, 80, true));

        mockMvc.perform(get("/api/v1/evidence/traceability/auto-capture-rate?threshold_percent=80")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_steps").value(10))
                .andExpect(jsonPath("$.data.auto_captured_steps").value(9))
                .andExpect(jsonPath("$.data.auto_capture_rate_percent").value(90.0))
                .andExpect(jsonPath("$.data.meets_threshold").value(true));

        verify(authorizationService).requirePermission("evidence.timeline.view");
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        EvidenceEventService evidenceEventService() {
            return Mockito.mock(EvidenceEventService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        TraceabilityService traceabilityService() {
            return Mockito.mock(TraceabilityService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
