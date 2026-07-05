package com.complex.platform.amusement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

@WebMvcTest(controllers = AmusementController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, AmusementControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AmusementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AmusementService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        when(service.listDeviceStatuses()).thenReturn(new PageResponse<>(List.of(deviceStatus()), 1, 50, 1));
        when(service.listManualOverrides()).thenReturn(new PageResponse<>(List.of(manualOverride()), 1, 50, 1));
        when(service.createManualOverride(any(AmusementManualOverrideRequest.class))).thenReturn(manualOverride());
        when(service.listProjects()).thenReturn(new PageResponse<>(List.of(project()), 1, 50, 1));
        when(service.createProject(any(AmusementProjectRequest.class))).thenReturn(project());
        when(service.getOpenEligibility("project_1")).thenReturn(new AmusementOpenEligibilityResponse("project_1", null, true, List.of()));
        when(service.createProjectRule(eq("project_1"), any(AmusementProjectRuleRequest.class))).thenReturn(projectRule());
        when(service.listProjectEntitlements("project_1")).thenReturn(new PageResponse<>(List.of(projectEntitlement()), 1, 50, 1));
        when(service.createProjectEntitlement(eq("project_1"), any(AmusementProjectEntitlementRequest.class))).thenReturn(projectEntitlement());
        when(service.deleteProjectEntitlement(eq("project_1"), eq("amusement_pass"), any())).thenReturn(projectEntitlement());
        when(service.listCapacityConfigs("project_1")).thenReturn(new PageResponse<>(List.of(capacityConfig()), 1, 50, 1));
        when(service.createCapacityConfig(eq("project_1"), any(AmusementCapacityConfigRequest.class))).thenReturn(capacityConfig());
        when(service.disableCapacityConfig(eq("amcap_1"), any())).thenReturn(capacityConfig());
        when(service.getOverCapacityPolicy("project_1")).thenReturn(overCapacityPolicy());
        when(service.configureOverCapacityPolicy(eq("project_1"), any(ConfigureOverCapacityPolicyRequest.class)))
                .thenReturn(overCapacityPolicy());
        when(service.listEntitlementSessions()).thenReturn(new PageResponse<>(List.of(entitlementSession()), 1, 50, 1));
        when(service.initiateEntitlementSession(any(InitiateEntitlementSessionRequest.class))).thenReturn(entitlementSession());
        when(service.getEntitlementSession("esession_1")).thenReturn(entitlementSession());
        when(service.claimEntitlementSession(eq("esession_1"), any(ClaimEntitlementSessionRequest.class)))
                .thenReturn(claimedEntitlementSession());
        when(service.listQueueEntries()).thenReturn(new PageResponse<>(List.of(queueEntry()), 1, 50, 1));
        when(service.createQueueEntry(any(AmusementQueueEntryRequest.class))).thenReturn(queueEntry());
        when(service.changeQueueEntryStatus(eq("queue_1"), any(StatusChangeRequest.class))).thenReturn(calledQueueEntry());
        when(service.listReservations()).thenReturn(new PageResponse<>(List.of(reservation()), 1, 50, 1));
        when(service.createReservation(any(AmusementReservationRequest.class))).thenReturn(reservation());
        when(service.changeReservationStatus(eq("reservation_1"), any(StatusChangeRequest.class))).thenReturn(completedReservation());
        when(service.listGroupBookings()).thenReturn(new PageResponse<>(List.of(groupBooking()), 1, 50, 1));
        when(service.createGroupBooking(any(AmusementGroupBookingRequest.class))).thenReturn(groupBooking());
        when(service.approveGroupBooking(eq("group_booking_1"), any(AmusementGroupBookingApprovalRequest.class))).thenReturn(approvedGroupBooking());
        when(service.releaseGroupBooking(eq("group_booking_1"), any(AmusementGroupBookingReleaseRequest.class))).thenReturn(releasedGroupBooking());
        when(service.listSafetyInspections()).thenReturn(new PageResponse<>(List.of(inspection()), 1, 50, 1));
        when(service.createSafetyInspection(any(AmusementSafetyInspectionRequest.class))).thenReturn(inspection());
        when(service.approveReopen(eq("inspection_1"), any(StatusChangeRequest.class))).thenReturn(reopenedInspection());
        when(service.recheckSafetyInspection(eq("inspection_1"), any(AmusementSafetyActionRequest.class))).thenReturn(recheckedInspection());
        when(service.rectifySafetyInspection(eq("inspection_1"), any(AmusementSafetyActionRequest.class))).thenReturn(rectifiedInspection());
        when(service.submitSafetyInspection(eq("inspection_1"), any(AmusementSafetyActionRequest.class))).thenReturn(submittedInspection());
        when(service.listSafetyTemplates()).thenReturn(new PageResponse<>(List.of(template()), 1, 50, 1));
        when(service.createSafetyTemplate(any(AmusementSafetyTemplateRequest.class))).thenReturn(template());
        when(service.listSessions()).thenReturn(new PageResponse<>(List.of(session()), 1, 50, 1));
        when(service.createSession(any(AmusementSessionRequest.class))).thenReturn(session());
        when(service.listVerifications()).thenReturn(new PageResponse<>(List.of(verification()), 1, 50, 1));
        when(service.createVerification(any(VerificationRequest.class))).thenReturn(verification());
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/v1/amusement/device-statuses,amusement.device_status.view",
            "GET,/api/v1/amusement/manual-overrides,amusement.verification.override",
            "POST,/api/v1/amusement/manual-overrides,amusement.verification.override",
            "GET,/api/v1/amusement/projects,amusement.session.view",
            "POST,/api/v1/amusement/projects,amusement.session.manage",
            "GET,/api/v1/amusement/projects/project_1/open-eligibility,amusement.safety_inspection.view",
            "POST,/api/v1/amusement/projects/project_1/rules,amusement.project.manage",
            "GET,/api/v1/amusement/projects/project_1/entitlements,amusement.project_entitlement.manage",
            "POST,/api/v1/amusement/projects/project_1/entitlements,amusement.project_entitlement.manage",
            "POST,/api/v1/amusement/projects/project_1/entitlements/amusement_pass/remove,amusement.project_entitlement.manage",
            "GET,/api/v1/amusement/projects/project_1/capacity-config,amusement.capacity_config.manage",
            "POST,/api/v1/amusement/projects/project_1/capacity-config,amusement.capacity_config.manage",
            "POST,/api/v1/amusement/capacity-config/amcap_1/disable,amusement.capacity_config.manage",
            "GET,/api/v1/amusement/projects/project_1/over-capacity-policy,amusement.over_capacity_policy.view",
            "PUT,/api/v1/amusement/projects/project_1/over-capacity-policy,amusement.over_capacity_policy.configure",
            "GET,/api/v1/amusement/entitlement-sessions,amusement.entitlement_session.view",
            "POST,/api/v1/amusement/entitlement-sessions,amusement.entitlement_session.initiate",
            "GET,/api/v1/amusement/entitlement-sessions/esession_1,amusement.entitlement_session.view",
            "POST,/api/v1/amusement/entitlement-sessions/esession_1/claim,amusement.entitlement_session.claim",
            "GET,/api/v1/amusement/queue-entries,amusement.queue.view",
            "POST,/api/v1/amusement/queue-entries,amusement.queue.manage",
            "POST,/api/v1/amusement/queue-entries/queue_1/status,amusement.queue.manage",
            "GET,/api/v1/amusement/reservations,amusement.reservation.view",
            "POST,/api/v1/amusement/reservations,amusement.reservation.manage",
            "POST,/api/v1/amusement/reservations/reservation_1/status,amusement.reservation.manage",
            "GET,/api/v1/amusement/group-bookings,amusement.group_booking.view",
            "POST,/api/v1/amusement/group-bookings,amusement.group_booking.manage",
            "POST,/api/v1/amusement/group-bookings/group_booking_1/approve,amusement.group_booking.approve",
            "POST,/api/v1/amusement/group-bookings/group_booking_1/release,amusement.group_booking.manage",
            "GET,/api/v1/amusement/safety-inspections,amusement.safety_inspection.view",
            "POST,/api/v1/amusement/safety-inspections,amusement.safety_inspection.create",
            "POST,/api/v1/amusement/safety-inspections/inspection_1/approve-reopen,amusement.safety_inspection.approve_reopen",
            "POST,/api/v1/amusement/safety-inspections/inspection_1/recheck,amusement.safety_inspection.recheck",
            "POST,/api/v1/amusement/safety-inspections/inspection_1/rectify,amusement.safety_inspection.rectify",
            "POST,/api/v1/amusement/safety-inspections/inspection_1/submit,amusement.safety_inspection.submit",
            "GET,/api/v1/amusement/safety-templates,amusement.safety_template.view",
            "POST,/api/v1/amusement/safety-templates,amusement.safety_template.manage",
            "GET,/api/v1/amusement/sessions,amusement.session.view",
            "POST,/api/v1/amusement/sessions,amusement.session.manage",
            "GET,/api/v1/amusement/verifications,amusement.verification.view",
            "POST,/api/v1/amusement/verifications,amusement.verification.execute"
    })
    void amusementRoutesUseExactOpenApiPermission(String method, String path, String permission) throws Exception {
        if ("GET".equals(method)) {
            mockMvc.perform(get(path).with(user("amusement").roles("P0_SMOKE")))
                    .andExpect(status().isOk());
        } else if ("PUT".equals(method)) {
            mockMvc.perform(put(path)
                            .with(user("amusement").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(path, true)))
                    .andExpect(status().isOk());
        } else {
            mockMvc.perform(post(path)
                            .with(user("amusement").roles("P0_SMOKE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(path, true)))
                    .andExpect(status().isOk());
        }

        verify(authorizationService).requirePermission(permission);
    }

    @org.junit.jupiter.api.Test
    void overCapacityPolicyPutRejectsMissingIdempotencyKey() throws Exception {
        mockMvc.perform(put("/api/v1/amusement/projects/project_1/over-capacity-policy")
                        .with(user("amusement").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyFor("/api/v1/amusement/projects/project_1/over-capacity-policy", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    @ParameterizedTest
    @CsvSource({
            "/api/v1/amusement/manual-overrides",
            "/api/v1/amusement/projects",
            "/api/v1/amusement/projects/project_1/rules",
            "/api/v1/amusement/projects/project_1/entitlements",
            "/api/v1/amusement/projects/project_1/entitlements/amusement_pass/remove",
            "/api/v1/amusement/projects/project_1/capacity-config",
            "/api/v1/amusement/capacity-config/amcap_1/disable",
            "/api/v1/amusement/entitlement-sessions",
            "/api/v1/amusement/entitlement-sessions/esession_1/claim",
            "/api/v1/amusement/queue-entries",
            "/api/v1/amusement/queue-entries/queue_1/status",
            "/api/v1/amusement/reservations",
            "/api/v1/amusement/reservations/reservation_1/status",
            "/api/v1/amusement/group-bookings",
            "/api/v1/amusement/group-bookings/group_booking_1/approve",
            "/api/v1/amusement/group-bookings/group_booking_1/release",
            "/api/v1/amusement/safety-inspections",
            "/api/v1/amusement/safety-inspections/inspection_1/approve-reopen",
            "/api/v1/amusement/safety-inspections/inspection_1/recheck",
            "/api/v1/amusement/safety-inspections/inspection_1/rectify",
            "/api/v1/amusement/safety-inspections/inspection_1/submit",
            "/api/v1/amusement/safety-templates",
            "/api/v1/amusement/sessions",
            "/api/v1/amusement/verifications"
    })
    void writeRoutesRejectMissingIdempotencyKey(String path) throws Exception {
        mockMvc.perform(post(path)
                        .with(user("amusement").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyFor(path, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    private String bodyFor(String path, boolean withIdempotencyKey) {
        String idempotency = withIdempotencyKey ? "\"idempotency_key\":\"amusement-once\"," : "";
        if (path.contains("manual-overrides")) {
            return "{" + idempotency + """
                    "project_id":"project_1","session_id":"session_1","override_type":"allow","reason":"supervisor approved","evidence_attachment_ids":["att_1"]}
                    """;
        }
        if (path.endsWith("/projects")) {
            return "{" + idempotency + """
                    "project_name":"Roller Coaster","capacity":2,"open_status":"open","safety_template_id":"template_1","eligibility_rule":{"min_age":12}}
                    """;
        }
        if (path.contains("/rules")) {
            return "{" + idempotency + """
                    "rule_type":"safety_notice","rule_value":"operator briefing required","effective_from":"2026-06-14","blocking":true}
                    """;
        }
        if (path.endsWith("/remove") || path.endsWith("/disable")) {
            return "{" + idempotency + "\"action\":\"remove\",\"reason\":\"operator config change\"}";
        }
        if (path.endsWith("/entitlements")) {
            return "{" + idempotency + "\"entitlement_type\":\"amusement_pass\",\"operator_note\":\"bind pass\"}";
        }
        if (path.endsWith("/capacity-config")) {
            return "{" + idempotency + "\"device_id\":\"gate_1\",\"max_concurrent\":3,\"operator_note\":\"point capacity\"}";
        }
        if (path.endsWith("/over-capacity-policy")) {
            return "{" + idempotency + "\"operating_capacity\":20,\"disposal_mode\":\"warn_only\",\"enabled\":true}";
        }
        if (path.endsWith("entitlement-sessions/esession_1/claim")) {
            return "{" + idempotency + "\"claimed_by\":\"kiosk_1\"}";
        }
        if (path.endsWith("entitlement-sessions")) {
            return "{" + idempotency + "\"entitlement_ref\":\"issued_ent_1\",\"ttl_seconds\":300,\"initiated_by\":\"phone_1\"}";
        }
        if (path.contains("queue-entries") && path.endsWith("/status")) {
            return "{" + idempotency + "\"action\":\"called\",\"reason\":\"notify customer\",\"evidence_attachment_ids\":[\"att_queue\"]}";
        }
        if (path.endsWith("queue-entries")) {
            return "{" + idempotency + """
                    "project_id":"project_1","session_id":"session_1","customer_id":"customer_1","queue_no":"A001"}
                    """;
        }
        if (path.contains("reservations") && path.endsWith("/status")) {
            return "{" + idempotency + "\"action\":\"completed\",\"reason\":\"customer arrived\",\"evidence_attachment_ids\":[\"att_reservation\"]}";
        }
        if (path.endsWith("reservations")) {
            return "{" + idempotency + """
                    "project_id":"project_1","session_id":"session_1","customer_id":"customer_1"}
                    """;
        }
        if (path.endsWith("/safety-inspections")) {
            return "{" + idempotency + """
                    "project_id":"project_1","template_id":"template_1","items":[{"name":"restraint","result":"pass"}],"overall_result":"pass","inspected_at":"2026-06-14T00:00:00Z","evidence_attachment_ids":["att_1"]}
                    """;
        }
        if (path.contains("approve-reopen")) {
            return "{" + idempotency + "\"action\":\"approve\",\"reason\":\"recheck passed\",\"evidence_attachment_ids\":[\"att_2\"]}";
        }
        if (path.contains("recheck")) {
            return "{" + idempotency + "\"action_result\":\"pass\",\"comment\":\"passed after repair\",\"attachment_ids\":[\"att_2\"]}";
        }
        if (path.contains("rectify")) {
            return "{" + idempotency + "\"action_result\":\"submitted\",\"comment\":\"fixed restraint sensor\",\"attachment_ids\":[\"att_2\"]}";
        }
        if (path.contains("submit")) {
            return "{" + idempotency + "\"action_result\":\"submit\",\"comment\":\"submit inspection\"}";
        }
        if (path.contains("safety-templates")) {
            return "{" + idempotency + """
                    "template_name":"Daily Opening","items":[{"name":"restraint","required":true}],"effective_from":"2026-06-14","evidence_attachment_ids":["att_1"]}
                    """;
        }
        if (path.contains("sessions")) {
            return "{" + idempotency + """
                    "project_id":"project_1","start_time":"2026-06-14T08:00:00Z","end_time":"2026-06-14T09:00:00Z","capacity":2,"status":"open","evidence_attachment_ids":["att_1"]}
                    """;
        }
        return "{" + idempotency + """
                "credential_type":"qr","credential_payload":"amu-ticket-1","device_id":"gate_1","point_id":"project_1","business_context":{"project_id":"project_1","session_id":"session_1","staff_id":"staff_1"}}
                """;
    }

    private AmusementProjectRecord project() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementProjectRecord("project_1", "Roller Coaster", 2, "open", "template_1", Map.of("min_age", 12), "point_1", now, now, null);
    }

    private AmusementProjectEntitlementRecord projectEntitlement() {
        return new AmusementProjectEntitlementRecord("ament_1", "project_1", "amusement_pass", Instant.parse("2026-06-14T00:00:00Z"));
    }

    private AmusementCapacityConfigRecord capacityConfig() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementCapacityConfigRecord("amcap_1", "project_1", null, "gate_1", 3, "", true, now, now);
    }

    private AmusementOverCapacityPolicy overCapacityPolicy() {
        return new AmusementOverCapacityPolicy("project_1", 20, AmusementOverCapacityPolicy.MODE_WARN_ONLY, true);
    }

    private EntitlementSessionRecord entitlementSession() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new EntitlementSessionRecord("esession_1", "escode_1", "issued_ent_1", null, null,
                "initiated", now.plusSeconds(300), "phone_1", null, null, now, now);
    }

    private EntitlementSessionRecord claimedEntitlementSession() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new EntitlementSessionRecord("esession_1", "escode_1", "issued_ent_1", null, null,
                "claimed", now.plusSeconds(300), "phone_1", "kiosk_1", now, now, now);
    }

    private AmusementSessionRecord session() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSessionRecord("session_1", "project_1", now, now.plusSeconds(3600), 2, 0, 0, "open", List.of("att_1"), now, now);
    }

    private AmusementSafetyTemplateRecord template() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSafetyTemplateRecord("template_1", "Daily Opening", List.of(Map.of("name", "restraint")), LocalDate.parse("2026-06-14"), "active", List.of("att_1"), now, now, null);
    }

    private AmusementSafetyInspectionRecord inspection() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSafetyInspectionRecord("inspection_1", "project_1", null, "template_1", List.of(Map.of("name", "restraint", "result", "pass")), "pass", "created", "none", "not_required", now, null, null, null, null, List.of("att_1"), now, now, "inspector_1", "pass", false, null);
    }

    private AmusementSafetyInspectionRecord submittedInspection() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSafetyInspectionRecord("inspection_1", "project_1", null, "template_1", List.of(Map.of("name", "restraint", "result", "pass")), "pass", "submitted", "none", "not_required", now, now, null, null, null, List.of("att_1"), now, now, "inspector_1", "pass", false, null);
    }

    private AmusementSafetyInspectionRecord rectifiedInspection() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSafetyInspectionRecord("inspection_1", "project_1", null, "template_1", List.of(Map.of("name", "restraint", "result", "fail")), "fail", "submitted", "rectification_submitted", "pending", now, now, null, null, "fixed restraint sensor", List.of("att_2"), now, now, "inspector_1", "fail", true, "restraint sensor abnormal");
    }

    private AmusementSafetyInspectionRecord recheckedInspection() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSafetyInspectionRecord("inspection_1", "project_1", null, "template_1", List.of(Map.of("name", "restraint", "result", "fail")), "fail", "submitted", "rechecked", "pending", now, now, now, null, "passed after repair", List.of("att_2"), now, now, "inspector_1", "fail", true, "restraint sensor abnormal");
    }

    private AmusementSafetyInspectionRecord reopenedInspection() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementSafetyInspectionRecord("inspection_1", "project_1", null, "template_1", List.of(Map.of("name", "restraint", "result", "fail")), "fail", "submitted", "rechecked", "approved", now, now, now, now, "recheck passed", List.of("att_2"), now, now, "inspector_1", "pass", false, null);
    }

    private AmusementVerificationRecord verification() {
        return AmusementVerificationRecord.entry("verification_1", "project_1", "session_1", "amu-ticket-1", "gate_1", "staff_1", "success", null, List.of(), Instant.parse("2026-06-14T00:00:00Z"), 1, null, null);
    }

    private AmusementManualOverrideRecord manualOverride() {
        return new AmusementManualOverrideRecord("override_1", "project_1", "session_1", "allow", "supervisor approved", "approved", null, List.of("att_1"), Instant.parse("2026-06-14T00:00:00Z"));
    }

    private AmusementDeviceStatusRecord deviceStatus() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementDeviceStatusRecord("device_status_1", "project_1", "gate_1", "mock_provider", "online", now, Map.of("battery", "ok"), now, now);
    }

    private AmusementQueueEntryRecord queueEntry() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementQueueEntryRecord("queue_1", "project_1", "session_1", "customer_1", "A001", "waiting", now, now);
    }

    private AmusementQueueEntryRecord calledQueueEntry() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementQueueEntryRecord("queue_1", "project_1", "session_1", "customer_1", "A001", "called", now, now);
    }

    private AmusementReservationRecord reservation() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementReservationRecord("reservation_1", "project_1", "session_1", "customer_1", "reserved", now, now);
    }

    private AmusementReservationRecord completedReservation() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementReservationRecord("reservation_1", "project_1", "session_1", "customer_1", "completed", now, now);
    }

    private AmusementProjectRuleRecord projectRule() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementProjectRuleRecord("rule_1", "project_1", "safety_notice", "operator briefing required", "2026-06-14", null, true, "active", List.of("att_1"), now, now);
    }

    private AmusementGroupBookingRecord groupBooking() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementGroupBookingRecord("group_booking_1", "project_1", "session_1", "group", "Company Team", "13800000000", 2, 2, "pending_approval", null, "operator note", List.of("att_group"), null, null, now, now);
    }

    private AmusementGroupBookingRecord approvedGroupBooking() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementGroupBookingRecord("group_booking_1", "project_1", "session_1", "group", "Company Team", "13800000000", 2, 2, "approved", "ops approved", "operator note", List.of("att_approve"), now, null, now, now);
    }

    private AmusementGroupBookingRecord releasedGroupBooking() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new AmusementGroupBookingRecord("group_booking_1", "project_1", "session_1", "group", "Company Team", "13800000000", 2, 2, "released", "ops approved", "customer changed plan", List.of("att_release"), now, now, now, now);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AmusementService amusementService() {
            return Mockito.mock(AmusementService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
