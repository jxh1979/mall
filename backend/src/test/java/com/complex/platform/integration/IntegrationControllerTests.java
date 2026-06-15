package com.complex.platform.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = IntegrationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, IntegrationControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class IntegrationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
        when(service.listLedger(anyString(), anyString(), anyString())).thenReturn(new PageResponse<>(
                List.of(ledger("device", "device-1")),
                1,
                50,
                1
        ));
        when(service.listProviderAdapters()).thenReturn(new PageResponse<>(List.of(adapter("adapter-controller")), 1, 50, 1));
        when(service.createProviderAdapter(any(ProviderAdapterRequest.class))).thenReturn(adapter("adapter-created"));
        when(service.updateProviderAdapter(eq("adapter-controller"), any(ProviderAdapterUpdateRequest.class))).thenReturn(adapter("adapter-controller"));
        when(service.markProductionReady(eq("adapter-controller"), any(StatusChangeRequest.class))).thenReturn(adapter("adapter-controller"));
        when(service.testProviderAdapterMock(eq("adapter-controller"), any(ProviderAdapterTestRequest.class)))
                .thenReturn(new AdapterTestRecord(
                        "test-controller",
                        "adapter-controller",
                        "mock-provider",
                        "webhook",
                        Map.of("event", "paid"),
                        "passed",
                        Map.of("ok", true),
                        Instant.parse("2026-06-14T00:00:00Z")
                ));
        when(service.listOrderSyncs()).thenReturn(new PageResponse<>(List.of(orderSync()), 1, 50, 1));
        when(service.createOrderSync(any(IntegrationOrderSyncRequest.class))).thenReturn(orderSync());
        when(service.listRetryTasks()).thenReturn(new PageResponse<>(List.of(task("retry_task", "open")), 1, 50, 1));
        when(service.createRetryTask(any(IntegrationTaskRequest.class))).thenReturn(task("retry_task", "open"));
        when(service.runRetryTaskNow(eq("retry_1"), any(StatusChangeRequest.class))).thenReturn(task("retry_task", "ready"));
        when(service.listCompensationTasks()).thenReturn(new PageResponse<>(List.of(task("compensation_task", "open")), 1, 50, 1));
        when(service.createCompensationTask(any(IntegrationTaskRequest.class))).thenReturn(task("compensation_task", "open"));
        when(service.resolveCompensationTask(eq("compensation_1"), any(IntegrationTaskResolveRequest.class))).thenReturn(task("compensation_task", "resolved"));
        when(service.listReconciliationBatches()).thenReturn(new PageResponse<>(List.of(batch()), 1, 50, 1));
        when(service.createReconciliationBatch(any(ReconciliationBatchRequest.class))).thenReturn(batch());
        when(service.listReconciliationDifferences()).thenReturn(new PageResponse<>(List.of(difference("open")), 1, 50, 1));
        when(service.createReconciliationDifference(any(ReconciliationDifferenceRequest.class))).thenReturn(difference("open"));
        when(service.resolveReconciliationDifference(eq("difference_1"), any(ReconciliationDifferenceResolveRequest.class))).thenReturn(difference("resolved"));
    }

    @Test
    void integrationEndpointsRequireExactOpenApiPermissions() throws Exception {
        for (Endpoint endpoint : endpoints()) {
            Mockito.clearInvocations(authorizationService);

            perform(endpoint)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(authorizationService).requirePermission(endpoint.permission());
        }
    }

    @Test
    void writeEndpointsRejectMissingIdempotencyKeyWithValidationFailed() throws Exception {
        for (Endpoint endpoint : writeEndpointsMissingIdempotency()) {
            perform(endpoint)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    private ResultActions perform(Endpoint endpoint) throws Exception {
        if ("GET".equals(endpoint.method())) {
            return mockMvc.perform(get(endpoint.path()).with(user("ops").roles("P0_SMOKE")));
        }
        if ("POST".equals(endpoint.method())) {
            return mockMvc.perform(post(endpoint.path())
                    .with(user("ops").roles("P0_SMOKE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.body()));
        }
        if ("PUT".equals(endpoint.method())) {
            return mockMvc.perform(put(endpoint.path())
                    .with(user("ops").roles("P0_SMOKE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.body()));
        }
        throw new IllegalArgumentException("unsupported method " + endpoint.method());
    }

    private List<Endpoint> endpoints() {
        return List.of(
                new Endpoint("GET", "/api/v1/integration/compensation-tasks", "integration.compensation.view", null),
                new Endpoint("POST", "/api/v1/integration/compensation-tasks", "integration.compensation.manage", taskBody("compensation-create")),
                new Endpoint("POST", "/api/v1/integration/compensation-tasks/compensation_1/resolve", "integration.compensation.manage", taskResolveBody("compensation-resolve")),
                new Endpoint("GET", "/api/v1/integration/devices", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/devices/events", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/devices/offline-sync-batches", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/order-syncs", "integration.order_sync.view", null),
                new Endpoint("POST", "/api/v1/integration/order-syncs", "integration.order_sync.manage", orderSyncBody("order-sync-create")),
                new Endpoint("GET", "/api/v1/integration/provider-adapters", "integration.provider_adapter.view", null),
                new Endpoint("POST", "/api/v1/integration/provider-adapters", "integration.provider_adapter.create", providerCreateBody("create-controller")),
                new Endpoint("PUT", "/api/v1/integration/provider-adapters/adapter-controller", "integration.provider_adapter.update", providerUpdateBody("update-controller")),
                new Endpoint("POST", "/api/v1/integration/provider-adapters/adapter-controller/mark-production-ready", "integration.provider_adapter.production_ready", statusChangeBody("ready-controller")),
                new Endpoint("POST", "/api/v1/integration/provider-adapters/adapter-controller/test-mock", "integration.provider_adapter.test", mockTestBody("test-controller")),
                new Endpoint("GET", "/api/v1/integration/reconciliation/batches", "integration.reconciliation.view", null),
                new Endpoint("POST", "/api/v1/integration/reconciliation/batches", "integration.reconciliation.manage", batchBody("batch-create")),
                new Endpoint("GET", "/api/v1/integration/reconciliation/differences", "integration.reconciliation.view", null),
                new Endpoint("POST", "/api/v1/integration/reconciliation/differences", "integration.reconciliation.manage", differenceBody("difference-create")),
                new Endpoint("POST", "/api/v1/integration/reconciliation/differences/difference_1/resolve", "integration.reconciliation.resolve", differenceResolveBody("difference-resolve")),
                new Endpoint("GET", "/api/v1/integration/retail/skus", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/retry-tasks", "integration.retry.view", null),
                new Endpoint("POST", "/api/v1/integration/retry-tasks", "integration.retry.manage", taskBody("retry-create")),
                new Endpoint("POST", "/api/v1/integration/retry-tasks/retry_1/run-now", "integration.retry.manage", statusChangeBody("retry-now")),
                new Endpoint("GET", "/api/v1/integration/robot/task-events", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/robot/tasks", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/video/cameras", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/voucher-verifications", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/wecom/bindings", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/wecom/conversations", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/wecom/messages", "integration.provider_adapter.view", null),
                new Endpoint("GET", "/api/v1/integration/wecom/risk-hits", "integration.provider_adapter.view", null)
        );
    }

    private List<Endpoint> writeEndpointsMissingIdempotency() {
        return List.of(
                new Endpoint("POST", "/api/v1/integration/provider-adapters", "integration.provider_adapter.create", providerCreateBodyWithoutIdempotency()),
                new Endpoint("PUT", "/api/v1/integration/provider-adapters/adapter-controller", "integration.provider_adapter.update", providerUpdateBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/provider-adapters/adapter-controller/mark-production-ready", "integration.provider_adapter.production_ready", statusChangeBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/provider-adapters/adapter-controller/test-mock", "integration.provider_adapter.test", mockTestBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/order-syncs", "integration.order_sync.manage", orderSyncBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/retry-tasks", "integration.retry.manage", taskBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/retry-tasks/retry_1/run-now", "integration.retry.manage", statusChangeBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/compensation-tasks", "integration.compensation.manage", taskBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/compensation-tasks/compensation_1/resolve", "integration.compensation.manage", taskResolveBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/reconciliation/batches", "integration.reconciliation.manage", batchBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/reconciliation/differences", "integration.reconciliation.manage", differenceBodyWithoutIdempotency()),
                new Endpoint("POST", "/api/v1/integration/reconciliation/differences/difference_1/resolve", "integration.reconciliation.resolve", differenceResolveBodyWithoutIdempotency())
        );
    }

    private String providerCreateBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "provider_code": "mock-provider",
                  "adapter_type": "payment",
                  "mode": "mock",
                  "field_mapping": {"provider_status": "status"},
                  "webhook_config": {"enabled": true},
                  "polling_config": {"enabled": true},
                  "production_ready": false
                }
                """.formatted(idempotencyKey);
    }

    private String providerUpdateBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "provider_code": "mock-provider",
                  "capabilities": ["webhook", "polling"],
                  "environment": "production",
                  "status": "test_ready",
                  "webhook_enabled": true,
                  "polling_check_enabled": true,
                  "field_mapping_placeholder": {"provider_status": "status"},
                  "credential_secret_ref": "manual-ledger:p0-provider"
                }
                """.formatted(idempotencyKey);
    }

    private String statusChangeBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "action": "mark_production_ready",
                  "reason": "webhook and polling verified"
                }
                """.formatted(idempotencyKey);
    }

    private String mockTestBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "provider_code": "mock-provider",
                  "scenario": "webhook",
                  "mock_payload": {"provider_status": "SUCCESS"}
                }
                """.formatted(idempotencyKey);
    }

    private String providerCreateBodyWithoutIdempotency() {
        return providerCreateBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String providerUpdateBodyWithoutIdempotency() {
        return providerUpdateBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String statusChangeBodyWithoutIdempotency() {
        return statusChangeBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String mockTestBodyWithoutIdempotency() {
        return mockTestBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String orderSyncBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "provider_code": "mock-provider",
                  "business_type": "catering",
                  "sync_type": "order",
                  "external_business_id": "external-order-1",
                  "status": "synced",
                  "amount_cent": 128800,
                  "currency": "CNY",
                  "provider_payload": {"provider_private_status": "PAID"}
                }
                """.formatted(idempotencyKey);
    }

    private String taskBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "provider_code": "mock-provider",
                  "task_type": "webhook_process",
                  "business_type": "catering",
                  "external_business_id": "external-order-1",
                  "status": "open",
                  "max_retry_count": 3,
                  "next_retry_at": "2026-06-14T00:05:00Z",
                  "payload": {"reason": "timeout"}
                }
                """.formatted(idempotencyKey);
    }

    private String taskResolveBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "operator_user_id": "finance_1",
                  "status": "resolved",
                  "resolution_note": "manual ledger adjusted"
                }
                """.formatted(idempotencyKey);
    }

    private String batchBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "provider_code": "mock-provider",
                  "business_type": "catering",
                  "period_start": "2026-06-14",
                  "period_end": "2026-06-14",
                  "expected_count": 10,
                  "actual_count": 9,
                  "difference_count": 1,
                  "total_difference_cent": 100
                }
                """.formatted(idempotencyKey);
    }

    private String differenceBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "batch_id": "batch_1",
                  "provider_code": "mock-provider",
                  "business_type": "catering",
                  "external_business_id": "external-order-1",
                  "difference_type": "amount_mismatch",
                  "internal_amount_cent": 128800,
                  "provider_amount_cent": 128700,
                  "difference_amount_cent": 100,
                  "assigned_to_user_id": "finance_1",
                  "provider_payload": {"provider_private_status": "DIFF"}
                }
                """.formatted(idempotencyKey);
    }

    private String differenceResolveBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "operator_user_id": "finance_1",
                  "status": "resolved",
                  "resolution_note": "confirmed platform amount"
                }
                """.formatted(idempotencyKey);
    }

    private String orderSyncBodyWithoutIdempotency() {
        return orderSyncBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String taskBodyWithoutIdempotency() {
        return taskBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String taskResolveBodyWithoutIdempotency() {
        return taskResolveBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String batchBodyWithoutIdempotency() {
        return batchBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String differenceBodyWithoutIdempotency() {
        return differenceBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private String differenceResolveBodyWithoutIdempotency() {
        return differenceResolveBody("").replace("\"idempotency_key\": \"\",", "");
    }

    private IntegrationLedgerRecord ledger(String ledgerType, String id) {
        return new IntegrationLedgerRecord(
                id,
                ledgerType,
                "mock-provider",
                "biz-" + id,
                "synced",
                Map.of("id", id),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private ProviderAdapterRecord adapter(String id) {
        return new ProviderAdapterRecord(
                id,
                "mock-provider",
                "payment",
                "mock",
                List.of("webhook", "polling"),
                "mock",
                "mock_ready",
                true,
                true,
                Map.of("provider_status", "status"),
                Map.of("enabled", true),
                Map.of("enabled", true),
                "manual-ledger:p0-provider",
                false,
                "controller",
                List.of(),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        );
    }

    private IntegrationOrderSyncRecord orderSync() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new IntegrationOrderSyncRecord(
                "sync_1",
                "mock-provider",
                "catering",
                "order",
                "external-order-1",
                "synced",
                128800L,
                "CNY",
                Map.of("provider_private_status", "PAID"),
                now,
                now
        );
    }

    private IntegrationTaskRecord task(String category, String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new IntegrationTaskRecord(
                "retry_task".equals(category) ? "retry_1" : "compensation_1",
                category,
                "mock-provider",
                "webhook_process",
                "catering",
                "external-order-1",
                status,
                "ready".equals(status) ? 1 : 0,
                3,
                now.plusSeconds(300),
                null,
                null,
                Map.of("reason", "timeout"),
                List.of(),
                now,
                now
        );
    }

    private ReconciliationBatchRecord batch() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new ReconciliationBatchRecord(
                "batch_1",
                "mock-provider",
                "catering",
                java.time.LocalDate.parse("2026-06-14"),
                java.time.LocalDate.parse("2026-06-14"),
                "difference_found",
                10,
                9,
                1,
                100L,
                now,
                now
        );
    }

    private ReconciliationDifferenceRecord difference(String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new ReconciliationDifferenceRecord(
                "difference_1",
                "batch_1",
                "mock-provider",
                "catering",
                "external-order-1",
                "amount_mismatch",
                128800L,
                128700L,
                100L,
                status,
                "finance_1",
                "resolved".equals(status) ? "confirmed platform amount" : null,
                Map.of("provider_private_status", "DIFF"),
                List.of(),
                now,
                now
        );
    }

    private record Endpoint(String method, String path, String permission, String body) {
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        IntegrationService integrationService() {
            return Mockito.mock(IntegrationService.class);
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
