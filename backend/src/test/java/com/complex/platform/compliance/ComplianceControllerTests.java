package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.config.SecurityConfig;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(controllers = ComplianceController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, ComplianceControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class ComplianceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplianceService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedListReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/compliance/contract-key-dates"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void createContractKeyDateWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/compliance/contract-key-dates")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contract_id": "contract_1",
                                  "merchant_id": "merchant_1",
                                  "date_type": "contract_end",
                                  "title": "Contract end",
                                  "due_date": "2026-07-01",
                                  "owner_user_id": "staff_legal",
                                  "human_confirmed": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createContractKeyDateRequiresManagePermissionAndReturnsRecord() throws Exception {
        when(service.createContractKeyDate(any(ContractKeyDateRequest.class))).thenReturn(contractDateRecord());

        mockMvc.perform(post("/api/v1/compliance/contract-key-dates")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "contract-date-create",
                                  "operator_note": "record contract end date",
                                  "evidence_attachment_ids": ["att-contract"],
                                  "contract_id": "contract_1",
                                  "merchant_id": "merchant_1",
                                  "date_type": "contract_end",
                                  "title": "Contract end",
                                  "due_date": "2026-07-01",
                                  "reminder_days": [90, 30, 7],
                                  "status": "active",
                                  "owner_user_id": "staff_legal",
                                  "source_type": "manual",
                                  "human_confirmed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ckd_1"))
                .andExpect(jsonPath("$.data.date_type").value("contract_end"));

        verify(authorizationService).requirePermission("compliance.contract_key_date.manage");
    }

    @Test
    void listExpiryItemsUsesViewPermissionAndPageShape() throws Exception {
        when(service.listExpiryItems()).thenReturn(new PageResponse<>(List.of(expiryItemRecord()), 1, 50, 1));

        mockMvc.perform(get("/api/v1/compliance/expiry-items")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("cei_1"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("compliance.expiry_item.view");
    }

    @Test
    void scanRemindersRequiresScanPermissionAndReturnsGeneratedCount() throws Exception {
        when(service.scanReminders(any(ComplianceReminderScanRequest.class))).thenReturn(new ComplianceReminderScanResult(
                1,
                List.of(new ComplianceReminderRecord(
                        "cr_1",
                        "compliance_expiry_item",
                        "cei_1",
                        "expired",
                        LocalDate.parse("2026-06-10"),
                        LocalDate.parse("2026-06-14"),
                        "critical",
                        "open",
                        true,
                        "Alcohol license is expired",
                        Instant.parse("2026-06-14T00:00:00Z")
                ))
        ));

        mockMvc.perform(post("/api/v1/compliance/reminders/scan")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "scan-20260614",
                                  "operator_note": "daily scan",
                                  "scan_date": "2026-06-14",
                                  "default_lookahead_days": 30,
                                  "operator_user_id": "staff_ops"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated_count").value(1))
                .andExpect(jsonPath("$.data.reminders[0].blocking_suggested").value(true));

        verify(authorizationService).requirePermission("compliance.reminder.scan");
    }

    private ContractKeyDateRecord contractDateRecord() {
        return new ContractKeyDateRecord(
                "ckd_1",
                "contract_1",
                "merchant_1",
                "contract_end",
                "Contract end",
                LocalDate.parse("2026-07-01"),
                List.of(90, 30, 7),
                "active",
                "staff_legal",
                "manual",
                true,
                List.of("att-contract"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private ComplianceExpiryItemRecord expiryItemRecord() {
        return new ComplianceExpiryItemRecord(
                "cei_1",
                "merchant",
                "merchant_1",
                "merchant_1",
                "alcohol_license",
                "Alcohol license",
                "LIC-001",
                "Market Authority",
                LocalDate.parse("2025-06-01"),
                LocalDate.parse("2026-06-10"),
                List.of(60, 30, 7),
                "block_operation",
                "active",
                "staff_compliance",
                "manual",
                true,
                List.of("att-license"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        ComplianceService complianceService() {
            return Mockito.mock(ComplianceService.class);
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
