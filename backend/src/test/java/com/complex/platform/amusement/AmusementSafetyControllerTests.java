package com.complex.platform.amusement;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.complex.platform.amusement.dto.AmusementIncidentActionRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCloseRequest;
import com.complex.platform.amusement.dto.AmusementIncidentCreateRequest;
import com.complex.platform.amusement.dto.AmusementSafetyNoticeCreateRequest;
import com.complex.platform.amusement.dto.AmusementWaiverAcknowledgementRequest;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = AmusementSafetyController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, AmusementSafetyControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AmusementSafetyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AmusementSafetyService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedListReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/amusement/safety-notices"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void createSafetyNoticeWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/amusement/safety-notices")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "project_id": "prj_1",
                                  "notice_type": "safety_notice",
                                  "title": "Rider safety",
                                  "content_version": "v1",
                                  "content_text": "Read before riding",
                                  "status": "active",
                                  "operator_user_id": "staff_ops"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createSafetyNoticeUsesManagePermissionAndReturnsNotice() throws Exception {
        when(service.createSafetyNotice(any(AmusementSafetyNoticeCreateRequest.class))).thenReturn(notice());

        mockMvc.perform(post("/api/v1/amusement/safety-notices")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "notice-create",
                                  "operator_note": "create safety notice",
                                  "evidence_attachment_ids": ["att-notice"],
                                  "project_id": "prj_1",
                                  "amusement_project_id": "amu_project_1",
                                  "notice_type": "safety_notice",
                                  "title": "Rider safety",
                                  "content_version": "v1",
                                  "content_text": "Read before riding",
                                  "status": "active",
                                  "operator_user_id": "staff_ops"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("asn_1"))
                .andExpect(jsonPath("$.data.title").value("Rider safety"));

        verify(authorizationService).requirePermission("amusement.safety_notice.manage");
    }

    @Test
    void listSafetyNoticesUsesViewPermission() throws Exception {
        when(service.listSafetyNotices()).thenReturn(new PageResponse<>(List.of(notice()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/amusement/safety-notices")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("asn_1"));

        verify(authorizationService).requirePermission("amusement.safety_notice.view");
    }

    @Test
    void acknowledgeWaiverUsesPermissionAndReturnsRecord() throws Exception {
        when(service.acknowledgeWaiver(eq("asn_1"), any(AmusementWaiverAcknowledgementRequest.class)))
                .thenReturn(acknowledgement());

        mockMvc.perform(post("/api/v1/amusement/safety-notices/asn_1/acknowledgements")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "waiver-create",
                                  "operator_note": "visitor acknowledged",
                                  "project_id": "prj_1",
                                  "amusement_project_id": "amu_project_1",
                                  "session_id": "session_1",
                                  "customer_id": "customer_1",
                                  "visitor_name": "Visitor Zhang",
                                  "signer_name": "Parent Zhang",
                                  "acknowledgement_method": "onsite_tablet",
                                  "content_version": "v1",
                                  "content_hash": "sha256:notice-v1",
                                  "operator_user_id": "staff_gate"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("awa_1"))
                .andExpect(jsonPath("$.data.notice_id").value("asn_1"));

        verify(authorizationService).requirePermission("amusement.waiver.acknowledge");
    }

    @Test
    void incidentRoutesUseExactPermissions() throws Exception {
        when(service.listIncidents()).thenReturn(new PageResponse<>(List.of(incident()), 1, 50, 1));
        when(service.createIncident(any(AmusementIncidentCreateRequest.class))).thenReturn(incident());
        when(service.addIncidentAction(eq("ami_1"), any(AmusementIncidentActionRequest.class))).thenReturn(incident());
        when(service.closeIncident(eq("ami_1"), any(AmusementIncidentCloseRequest.class))).thenReturn(closedIncident());

        mockMvc.perform(get("/api/v1/amusement/incidents")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("ami_1"));
        verify(authorizationService).requirePermission("amusement.incident.view");

        mockMvc.perform(post("/api/v1/amusement/incidents")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentCreateJson("incident-create")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("created"));
        verify(authorizationService).requirePermission("amusement.incident.create");

        mockMvc.perform(post("/api/v1/amusement/incidents/ami_1/actions")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "incident-action",
                                  "operator_note": "ops handling",
                                  "target_status": "processing",
                                  "operator_user_id": "staff_ops"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ami_1"));
        verify(authorizationService).requirePermission("amusement.incident.process");

        mockMvc.perform(post("/api/v1/amusement/incidents/ami_1/close")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "incident-close",
                                  "operator_note": "close",
                                  "resolution": "Handled and documented",
                                  "closed_by_user_id": "staff_manager"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));
        verify(authorizationService).requirePermission("amusement.incident.close");
    }

    @Test
    void createIncidentWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/amusement/incidents")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentCreateJson(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String incidentCreateJson(String idempotencyKey) {
        String idempotencyLine = idempotencyKey == null ? "" : "\"idempotency_key\": \"" + idempotencyKey + "\",";
        return """
                {
                  %s
                  "operator_note": "create incident",
                  "project_id": "prj_1",
                  "amusement_project_id": "amu_project_1",
                  "session_id": "session_1",
                  "device_id": "device_1",
                  "customer_id": "customer_1",
                  "severity": "high",
                  "title": "Guest slipped",
                  "description": "Guest slipped near entrance",
                  "reported_by_user_id": "staff_reporter",
                  "reported_channel": "staff_app",
                  "occurred_at": "2026-06-15T09:00:00Z"
                }
                """.formatted(idempotencyLine);
    }

    private AmusementSafetyNoticeRecord notice() {
        return new AmusementSafetyNoticeRecord(
                "asn_1",
                "prj_1",
                "amu_project_1",
                "safety_notice",
                "Rider safety",
                "v1",
                "Read before riding",
                "active",
                Instant.parse("2026-06-15T00:00:00Z"),
                null,
                "staff_ops",
                List.of("att-notice"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private AmusementWaiverAcknowledgementRecord acknowledgement() {
        return new AmusementWaiverAcknowledgementRecord(
                "awa_1",
                "asn_1",
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
                "staff_gate",
                List.of("att-signature"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private AmusementIncidentRecord incident() {
        return new AmusementIncidentRecord(
                "ami_1",
                "prj_1",
                "amu_project_1",
                "session_1",
                "device_1",
                "customer_1",
                "high",
                "created",
                "Guest slipped",
                "Guest slipped near entrance",
                "staff_reporter",
                "staff_app",
                Instant.parse("2026-06-15T09:00:00Z"),
                null,
                null,
                List.of("att-photo"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private AmusementIncidentRecord closedIncident() {
        return incident().close("Handled and documented", Instant.parse("2026-06-14T01:00:00Z"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AmusementSafetyService amusementSafetyService() {
            return Mockito.mock(AmusementSafetyService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
