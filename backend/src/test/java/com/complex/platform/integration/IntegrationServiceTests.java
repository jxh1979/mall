package com.complex.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationServiceTests {

    private static final Instant NOW = Instant.parse("2026-06-14T00:00:00Z");
    private static final String OPEN_APP_SECRET_REF = "config:complex.open-platform.secrets.p0-open-app";
    private static final String OPEN_WEBHOOK_SECRET_REF = "config:complex.open-platform.secrets.p0-webhook";
    private static final String OPEN_APP_TEST_SECRET = "test-open-app-secret-from-stub";
    private static final String OPEN_WEBHOOK_TEST_SECRET = "test-webhook-secret-from-stub";

    private InMemoryIntegrationRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private IntegrationService service;
    private Map<String, String> secrets;
    private com.complex.platform.terminal.TerminalDeviceRepository terminalDeviceRepository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIntegrationRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        secrets = new LinkedHashMap<>();
        secrets.put(OPEN_APP_SECRET_REF, OPEN_APP_TEST_SECRET);
        secrets.put(OPEN_WEBHOOK_SECRET_REF, OPEN_WEBHOOK_TEST_SECRET);
        terminalDeviceRepository = org.mockito.Mockito.mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        // 默认无显式开关行 → T2 语义"默认开启"，放行（第三方默认关闭由迁移种子在真实库表达）。
        org.mockito.Mockito.when(terminalDeviceRepository.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap())).thenReturn(Optional.empty());
        com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard =
                new com.complex.platform.gate.TerminalSwitchGuard(
                        terminalDeviceRepository, new AuditEventService(auditEventRepository));
        service = new IntegrationService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                Clock.fixed(NOW, ZoneOffset.UTC),
                secretRef -> Optional.ofNullable(secrets.get(secretRef)),
                terminalSwitchGuard
        );
        repository.appCredentials.put("p0-open-app", new OpenAppCredentialRecord(
                "cred-p0",
                "p0-open-app",
                OPEN_APP_SECRET_REF,
                "active",
                List.of("p0-default"),
                NOW,
                NOW
        ));
        repository.endpointConfigs.put("p0-default", new OpenEndpointConfigRecord(
                "endpoint-p0",
                "p0-default",
                "mock-provider",
                OPEN_WEBHOOK_SECRET_REF,
                "active",
                true,
                true,
                NOW,
                NOW
        ));
    }

    @Test
    void p1ReconciliationDifferenceAmountIsComputedServerSideIgnoringRequestValue() {
        ReconciliationBatchRecord batch = service.createReconciliationBatch(new ReconciliationBatchRequest(
                "batch-for-diff-amount-test",
                "batch note",
                List.of(),
                "mock-provider",
                "catering",
                LocalDate.parse("2026-06-14"),
                LocalDate.parse("2026-06-14"),
                10,
                9,
                1,
                100L
        ));

        // Caller passes wrong differenceAmountCent=999; server must compute Math.abs(128800-128700)=100
        ReconciliationDifferenceRecord difference = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "diff-for-amount-test",
                "diff note",
                List.of(),
                batch.id(),
                "mock-provider",
                "catering",
                "ext-order-1",
                "amount_mismatch",
                128800L,
                128700L,
                999L,
                null,
                Map.of()
        ));

        assertThat(difference.differenceAmountCent()).isEqualTo(100L);
    }

    @Test
    void p1ReconciliationBatchRejectsZeroDifferenceCountWhenActualDiffersFromExpected() {
        assertThatThrownBy(() -> service.createReconciliationBatch(new ReconciliationBatchRequest(
                "batch-zero-diff-mismatch",
                "bad batch",
                List.of(),
                "mock-provider",
                "catering",
                LocalDate.parse("2026-06-14"),
                LocalDate.parse("2026-06-14"),
                10,
                9,
                0,
                0L
        ))).isInstanceOfSatisfying(ApiException.class, e ->
                assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void openAuthStoresOnlyTokenHashAndRejectsNonceReplay() {
        long timestamp = NOW.toEpochMilli();
        OpenAuthTokenRequest request = new OpenAuthTokenRequest(
                "p0-open-app",
                timestamp,
                "nonce-auth",
                authSignature("p0-open-app", timestamp, "nonce-auth")
        );

        OpenAuthTokenResponse response = service.issueOpenToken(request);

        assertThat(response.token()).isNotBlank();
        assertThat(repository.tokens).hasSize(1);
        assertThat(repository.tokens.getFirst().tokenHash())
                .hasSize(64)
                .isNotEqualTo(response.token())
                .isEqualTo(sha256(response.token()));
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("open_auth.succeeded");

        assertThatThrownBy(() -> service.issueOpenToken(request))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("OPEN_NONCE_REPLAYED"));
    }

    @Test
    void openAuthRejectsInvalidSignatureAndAuditsFailure() {
        long timestamp = NOW.toEpochMilli();

        assertThatThrownBy(() -> service.issueOpenToken(new OpenAuthTokenRequest(
                "p0-open-app",
                timestamp,
                "nonce-invalid",
                "bad-signature"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("OPEN_SIGNATURE_INVALID"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("open_auth.failed");
    }

    @Test
    void openAuthFailsClosedWhenCredentialSecretRefCannotBeResolved() {
        long timestamp = NOW.toEpochMilli();
        secrets.remove(OPEN_APP_SECRET_REF);

        assertThatThrownBy(() -> service.issueOpenToken(new OpenAuthTokenRequest(
                "p0-open-app",
                timestamp,
                "nonce-missing-secret",
                "signature-not-used"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("OPEN_SECRET_UNAVAILABLE"));

        assertThat(repository.tokens).isEmpty();
        assertThat(repository.nonces).isEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("open_auth.failed");
    }

    @Test
    void webhookPersistsEventAndDuplicateIdempotencyReturnsOriginalResult() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_id", "evt-webhook");
        payload.put("idempotency_key", "idem-webhook");
        payload.put("payload", Map.of("normalized_status", "paid"));
        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-webhook", payload);

        WebhookAcceptedResponse first = service.receiveWebhook(
                "p0-default",
                "mock-provider",
                "payment.paid",
                signature,
                null,
                payload
        );
        WebhookAcceptedResponse second = service.receiveWebhook(
                "p0-default",
                "mock-provider",
                "payment.paid",
                signature,
                null,
                payload
        );

        assertThat(first.eventId()).isEqualTo("evt-webhook");
        assertThat(first.deduplicated()).isFalse();
        assertThat(second.eventId()).isEqualTo("evt-webhook");
        assertThat(second.deduplicated()).isTrue();
        assertThat(repository.events).hasSize(1);
        assertThat(repository.events.getFirst().payloadJson()).containsEntry("event_id", "evt-webhook");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("api_call.logged");
    }

    @Test
    void webhookRejectsSignatureWhenPayloadChanges() {
        Map<String, Object> signedPayload = new LinkedHashMap<>();
        signedPayload.put("event_id", "evt-webhook-tamper");
        signedPayload.put("idempotency_key", "idem-webhook-tamper");
        signedPayload.put("payload", Map.of("normalized_status", "paid"));
        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-webhook-tamper", signedPayload);

        Map<String, Object> tamperedPayload = new LinkedHashMap<>();
        tamperedPayload.put("event_id", "evt-webhook-tamper");
        tamperedPayload.put("idempotency_key", "idem-webhook-tamper");
        tamperedPayload.put("payload", Map.of("normalized_status", "refunded"));

        assertThatThrownBy(() -> service.receiveWebhook(
                "p0-default",
                "mock-provider",
                "payment.paid",
                signature,
                null,
                tamperedPayload
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("OPEN_WEBHOOK_SIGNATURE_INVALID"));

        assertThat(repository.events).isEmpty();
    }

    @Test
    void webhookWithoutMappingConfigStaysAcceptedEnvelopeOnly() {
        // 未注册任何 mock-provider 适配器 → 无映射配置 → accepted（回归现状）。
        Map<String, Object> payload = webhookPayload("evt-no-map", "idem-no-map");
        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-no-map", payload);

        service.receiveWebhook("p0-default", "mock-provider", "payment.paid", signature, null, payload);

        OpenApiEventRecord saved = repository.events.getFirst();
        assertThat(saved.eventStatus()).isEqualTo("accepted");
        assertThat(saved.resultJson()).doesNotContainKey("mapping");
        assertThat(saved.failureReason()).isNull();
    }

    @Test
    void webhookWithMappingConfigProducesStandardEventProjection() {
        repository.providerAdapters.put("map-adapter", webhookMappingAdapter(Map.of(
                "order_no", "payload.order_id",
                "status", "payload.normalized_status")));
        Map<String, Object> payload = webhookPayload("evt-map", "idem-map");

        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-map", payload);
        service.receiveWebhook("p0-default", "mock-provider", "payment.paid", signature, null, payload);

        OpenApiEventRecord saved = repository.events.getFirst();
        assertThat(saved.eventStatus()).isEqualTo("mapped");
        assertThat(saved.resultJson()).containsEntry("mapped", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> mapping = (Map<String, Object>) saved.resultJson().get("mapping");
        assertThat(mapping).containsEntry("order_no", "ord-1").containsEntry("status", "paid");
    }

    @Test
    void webhookStatusMappingTranslatesProviderStatusToPlatformStatus() {
        repository.providerAdapters.put("status-adapter", webhookMappingAdapter(Map.of(
                "status", "payload.normalized_status",
                "status_mapping", Map.of("paid", "SETTLED"))));
        Map<String, Object> payload = webhookPayload("evt-status", "idem-status");

        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-status", payload);
        service.receiveWebhook("p0-default", "mock-provider", "payment.paid", signature, null, payload);

        OpenApiEventRecord saved = repository.events.getFirst();
        assertThat(saved.eventStatus()).isEqualTo("mapped");
        @SuppressWarnings("unchecked")
        Map<String, Object> mapping = (Map<String, Object>) saved.resultJson().get("mapping");
        assertThat(mapping).containsEntry("status", "SETTLED");
    }

    @Test
    void webhookMappingFailureRecordsMapFailedWithoutBreakingIngest() {
        // request_mapping 值非字符串路径 → mapPayload 抛异常 → map_failed，但事件仍落库、不抛 5xx。
        repository.providerAdapters.put("bad-adapter", webhookMappingAdapter(Map.of(
                "order_no", List.of("not", "a", "path"))));
        Map<String, Object> payload = webhookPayload("evt-bad", "idem-bad");

        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-bad", payload);
        WebhookAcceptedResponse response =
                service.receiveWebhook("p0-default", "mock-provider", "payment.paid", signature, null, payload);

        assertThat(response.deduplicated()).isFalse();
        OpenApiEventRecord saved = repository.events.getFirst();
        assertThat(saved.eventStatus()).isEqualTo("map_failed");
        assertThat(saved.failureReason()).isNotBlank();
        @SuppressWarnings("unchecked")
        Map<String, Object> failurePlaceholder = (Map<String, Object>) saved.resultJson().get("mapping");
        assertThat(failurePlaceholder).containsEntry("reason_code", "WEBHOOK_MAP_FAILURE");
        // 幂等去重仍生效。
        WebhookAcceptedResponse dup =
                service.receiveWebhook("p0-default", "mock-provider", "payment.paid", signature, null, payload);
        assertThat(dup.deduplicated()).isTrue();
        assertThat(repository.events).hasSize(1);
    }

    @Test
    void webhookMappingDisabledBySwitchFallsBackToAccepted() {
        // 适配器有 fieldMapping，但 webhookEnabled=false（开关关）→ accepted（裁决兜底）。
        ProviderAdapterRecord disabledSwitch = withFieldMapping(
                webhookMappingAdapter(Map.of("order_no", "payload.order_id")), Map.of("order_no", "payload.order_id"), false);
        repository.providerAdapters.put("switch-off-adapter", disabledSwitch);
        Map<String, Object> payload = webhookPayload("evt-off", "idem-off");

        String signature = webhookSignature("p0-default", "mock-provider", "payment.paid", "idem-off", payload);
        service.receiveWebhook("p0-default", "mock-provider", "payment.paid", signature, null, payload);

        OpenApiEventRecord saved = repository.events.getFirst();
        assertThat(saved.eventStatus()).isEqualTo("accepted");
        assertThat(saved.resultJson()).doesNotContainKey("mapping");
    }

    @Test
    void providerAdapterProductionReadyRequiresSecretWebhookPollingCapabilitiesAndProductionEnvironment() {
        ProviderAdapterRecord draft = adapter(
                "adapter-draft",
                "mock-provider",
                List.of(),
                "production",
                "test_ready",
                true,
                true,
                null,
                false
        );
        repository.providerAdapters.put(draft.id(), draft);

        assertThatThrownBy(() -> service.markProductionReady(
                "adapter-draft",
                new StatusChangeRequest("ready-missing-secret", "mark_production_ready", "verified", List.of())
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("PROVIDER_ADAPTER_NOT_PRODUCTION_READY"));

        ProviderAdapterRecord readyCandidate = adapter(
                "adapter-ready",
                "ready-provider",
                List.of("webhook", "polling"),
                "production",
                "test_ready",
                true,
                true,
                "manual-ledger:p0-provider",
                false
        );
        repository.providerAdapters.put(readyCandidate.id(), readyCandidate);

        ProviderAdapterRecord ready = service.markProductionReady(
                "adapter-ready",
                new StatusChangeRequest("ready-success", "mark_production_ready", "verified", List.of("att-ready"))
        );

        assertThat(ready.productionReady()).isTrue();
        assertThat(ready.status()).isEqualTo("production_ready");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("provider_adapter.production_ready_marked");
    }

    @Test
    void mockAdapterTestScenariosAreRecorded() {
        ProviderAdapterRecord adapter = adapter(
                "adapter-test",
                "mock-provider",
                List.of("webhook"),
                "mock",
                "mock_ready",
                true,
                true,
                "manual-ledger:p0-provider",
                false
        );
        repository.providerAdapters.put(adapter.id(), adapter);

        AdapterTestRecord result = service.testProviderAdapterMock(
                "adapter-test",
                new ProviderAdapterTestRequest(
                        "mock-test",
                        "mock-provider",
                        "webhook",
                        // 显式 success 语义（expect=passed），避免依赖"默认 passed"兜底掩盖回归。
                        Map.of("provider_status", "SUCCESS", "expect", "passed"),
                        "operator note",
                        List.of()
                )
        );

        assertThat(result.testStatus()).isEqualTo("passed");
        assertThat(result.result()).doesNotContainKey("reason_code");
        assertThat(repository.adapterTests)
                .extracting(AdapterTestRecord::scenario)
                .containsExactly("webhook");
    }

    @Test
    void mockTestReturnsFailedWhenScenarioIndicatesFailure() {
        repository.providerAdapters.put("adapter-fail", adapter(
                "adapter-fail", "mock-provider", List.of("webhook"), "mock", "mock_ready",
                true, true, "manual-ledger:p0-provider", false));

        AdapterTestRecord result = service.testProviderAdapterMock(
                "adapter-fail",
                new ProviderAdapterTestRequest(
                        "mock-fail", "mock-provider", "failure_retry", Map.of(), "note", List.of()));

        assertThat(result.testStatus()).isEqualTo("failed");
        assertThat(result.result())
                .containsEntry("reason_code", "MOCK_SCENARIO_FAILURE")
                .containsEntry("scenario", "failure_retry");
    }

    @Test
    void mockTestReturnsPassedWhenScenarioIndicatesSuccess() {
        repository.providerAdapters.put("adapter-ok", adapter(
                "adapter-ok", "mock-provider", List.of("webhook"), "mock", "mock_ready",
                true, true, "manual-ledger:p0-provider", false));

        AdapterTestRecord result = service.testProviderAdapterMock(
                "adapter-ok",
                new ProviderAdapterTestRequest(
                        "mock-ok", "mock-provider", "webhook", Map.of(), "note", List.of()));

        assertThat(result.testStatus()).isEqualTo("passed");
        assertThat(result.result()).doesNotContainKey("reason_code");
    }

    @Test
    void mockTestRespectsExplicitMockPayloadForceStatus() {
        repository.providerAdapters.put("adapter-force", adapter(
                "adapter-force", "mock-provider", List.of("webhook"), "mock", "mock_ready",
                true, true, "manual-ledger:p0-provider", false));

        // scenario 语义为 passed（webhook 兜底），但 mockPayload.expect=failed 覆盖之。
        AdapterTestRecord result = service.testProviderAdapterMock(
                "adapter-force",
                new ProviderAdapterTestRequest(
                        "mock-force", "mock-provider", "webhook", Map.of("expect", "failed"), "note", List.of()));

        assertThat(result.testStatus()).isEqualTo("failed");
        assertThat(result.result()).containsEntry("mock_expect", "failed");
    }

    @Test
    void mockTestDefaultsToPassedForUnknownScenario() {
        repository.providerAdapters.put("adapter-unknown", adapter(
                "adapter-unknown", "mock-provider", List.of("webhook"), "mock", "mock_ready",
                true, true, "manual-ledger:p0-provider", false));

        // auth 既非 success/failure 前缀，走兜底 passed（向后兼容）。
        AdapterTestRecord result = service.testProviderAdapterMock(
                "adapter-unknown",
                new ProviderAdapterTestRequest(
                        "mock-unknown", "mock-provider", "auth", Map.of(), "note", List.of()));

        assertThat(result.testStatus()).isEqualTo("passed");
    }

    @Test
    void mockTestStillRejectsNonTestableAdapter() {
        repository.providerAdapters.put("adapter-disabled", adapter(
                "adapter-disabled", "mock-provider", List.of("webhook"), "mock", "disabled",
                true, true, "manual-ledger:p0-provider", false));

        assertThatThrownBy(() -> service.testProviderAdapterMock(
                "adapter-disabled",
                new ProviderAdapterTestRequest(
                        "mock-disabled", "mock-provider", "webhook", Map.of(), "note", List.of())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PROVIDER_ADAPTER_NOT_TESTABLE"));
    }

    @Test
    void readLedgerRoutingReturnsOnlyRequestedLedgerTypeAndAuditsOpenApiEventName() {
        repository.ledgers.add(new IntegrationLedgerRecord(
                "ledger-device",
                "device",
                "mock-provider",
                "device-1",
                "online",
                Map.of("device_id", "device-1"),
                NOW,
                NOW
        ));
        repository.ledgers.add(new IntegrationLedgerRecord(
                "ledger-order",
                "order_sync",
                "mock-provider",
                "order-1",
                "synced",
                Map.of("order_id", "order-1"),
                NOW,
                NOW
        ));

        var page = service.listLedger(
                "device",
                "integration.device.viewed",
                "integration.device.viewed_failed"
        );

        assertThat(page.items())
                .extracting(IntegrationLedgerRecord::ledgerType)
                .containsExactly("device");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("integration.device.viewed");
    }

    @Test
    void orderSyncRetryCompensationAndReconciliationKeepProviderPayloadOutOfCoreModels() {
        IntegrationOrderSyncRecord orderSync = service.createOrderSync(new IntegrationOrderSyncRequest(
                "sync-order",
                "record catering order",
                List.of("att-sync"),
                "mock-provider",
                "catering",
                "order",
                "external-order-1",
                "synced",
                128800L,
                "CNY",
                Map.of("provider_private_status", "PAID", "tableNo", "A1")
        ));
        IntegrationTaskRecord retry = service.createRetryTask(new IntegrationTaskRequest(
                "retry-create",
                "webhook process failed",
                List.of(),
                "mock-provider",
                "webhook_process",
                "catering",
                "external-order-1",
                "open",
                3,
                NOW.plusSeconds(300),
                Map.of("reason", "timeout")
        ));
        IntegrationTaskRecord retryReady = service.runRetryTaskNow(
                retry.id(),
                new StatusChangeRequest("retry-now", "run_now", "operator retry", List.of("att-retry"))
        );
        IntegrationTaskRecord compensation = service.createCompensationTask(new IntegrationTaskRequest(
                "compensation-create",
                "manual compensation",
                List.of(),
                "mock-provider",
                "manual_adjustment",
                "catering",
                "external-order-1",
                "open",
                0,
                null,
                Map.of("difference", 100)
        ));
        IntegrationTaskRecord resolvedCompensation = service.resolveCompensationTask(
                compensation.id(),
                new IntegrationTaskResolveRequest(
                        "compensation-resolve",
                        "resolved by finance",
                        List.of("att-resolution"),
                        "finance_1",
                        "resolved",
                        "manual ledger adjusted"
                )
        );
        ReconciliationBatchRecord batch = service.createReconciliationBatch(new ReconciliationBatchRequest(
                "reconcile-batch",
                "daily batch",
                List.of(),
                "mock-provider",
                "catering",
                LocalDate.parse("2026-06-14"),
                LocalDate.parse("2026-06-14"),
                10,
                9,
                1,
                100L
        ));
        ReconciliationDifferenceRecord difference = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "reconcile-difference",
                "missing voucher",
                List.of(),
                batch.id(),
                "mock-provider",
                "catering",
                "external-order-1",
                "amount_mismatch",
                128800L,
                128700L,
                100L,
                "finance_1",
                Map.of("provider_private_status", "DIFF")
        ));
        ReconciliationDifferenceRecord resolvedDifference = service.resolveReconciliationDifference(
                difference.id(),
                new ReconciliationDifferenceResolveRequest(
                        "reconcile-resolve",
                        "resolved",
                        List.of("att-diff"),
                        "finance_1",
                        "resolved",
                        "confirmed platform amount"
                )
        );

        assertThat(orderSync.providerPayload()).containsEntry("provider_private_status", "PAID");
        assertThat(retryReady.retryCount()).isEqualTo(1);
        assertThat(retryReady.status()).isEqualTo("ready");
        assertThat(resolvedCompensation.status()).isEqualTo("resolved");
        assertThat(batch.differenceCount()).isEqualTo(1);
        assertThat(resolvedDifference.status()).isEqualTo("resolved");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "integration.order_sync.recorded",
                        "integration.retry_task.created",
                        "integration.retry_task.run_now",
                        "integration.compensation_task.created",
                        "integration.compensation_task.resolved",
                        "integration.reconciliation_batch.created",
                        "integration.reconciliation_difference.created",
                        "integration.reconciliation_difference.resolved"
                );
    }

    private ProviderAdapterRecord adapter(
            String id,
            String providerCode,
            List<String> capabilities,
            String environment,
            String status,
            boolean webhookEnabled,
            boolean pollingCheckEnabled,
            String credentialSecretRef,
            boolean productionReady
    ) {
        return new ProviderAdapterRecord(
                id,
                providerCode,
                "payment",
                environment,
                capabilities,
                environment,
                status,
                webhookEnabled,
                pollingCheckEnabled,
                Map.of("provider_status", "status"),
                Map.of("enabled", webhookEnabled),
                Map.of("enabled", pollingCheckEnabled),
                credentialSecretRef,
                productionReady,
                "service-test",
                List.of(),
                NOW,
                NOW
        );
    }

    private Map<String, Object> webhookPayload(String eventId, String idempotencyKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_id", eventId);
        payload.put("idempotency_key", idempotencyKey);
        payload.put("payload", Map.of("order_id", "ord-1", "normalized_status", "paid"));
        return payload;
    }

    private ProviderAdapterRecord webhookMappingAdapter(Map<String, Object> fieldMapping) {
        return withFieldMapping(
                adapter("map-adapter", "mock-provider", List.of("webhook"), "production", "production_ready",
                        true, true, "manual-ledger:p0-provider", true),
                fieldMapping,
                true);
    }

    private ProviderAdapterRecord withFieldMapping(ProviderAdapterRecord base, Map<String, Object> fieldMapping, boolean webhookEnabled) {
        return new ProviderAdapterRecord(
                base.id(), base.providerCode(), base.adapterType(), base.mode(), base.capabilities(),
                base.environment(), base.status(), webhookEnabled, base.pollingCheckEnabled(),
                fieldMapping, base.webhookConfig(), base.pollingConfig(), base.credentialSecretRef(),
                base.productionReady(), base.operatorNote(), base.evidenceAttachmentIds(),
                base.createdAt(), base.updatedAt());
    }

    private String authSignature(String appKey, long timestamp, String nonce) {
        return hmac(OPEN_APP_TEST_SECRET, "app_key=" + appKey + "\nnonce=" + nonce + "\ntimestamp=" + timestamp);
    }

    private String webhookSignature(String endpointKey, String provider, String event, String idempotencyKey, Map<String, Object> payload) {
        return hmac(OPEN_WEBHOOK_TEST_SECRET, "endpoint_key=" + endpointKey + "\nevent=" + event + "\nidempotency_key=" + idempotencyKey + "\npayload_hash=" + payloadHash(payload) + "\nprovider=" + provider);
    }

    private String hmac(String key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String payloadHash(Map<String, Object> payload) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .findAndRegisterModules()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return sha256(mapper.writeValueAsString(payload == null ? Map.of() : payload));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    // ── R5 T14 第三方接口/开关预留骨架 ───────────────────────────────────────────

    @Test
    void t14TypedWebhookRejectedWhenSwitchDisabled() {
        org.mockito.Mockito.when(terminalDeviceRepository.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("integration.catering_pos.sync"),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.complex.platform.terminal.TerminalCapabilitySwitchRecord(
                        "tcs_int_catering_pos_off", "*", "integration.catering_pos.sync", "global", "global",
                        false, "off", NOW, NOW)));
        assertThatThrownBy(() -> service.receiveTypedWebhook(
                "catering_pos", "idem-catering-1", Map.of("event_id", "ev1")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("TERMINAL_DISABLED");
    }

    @Test
    void t14TypedWebhookAcceptedDoesNotFakeReconciliation() {
        WebhookAcceptedResponse response = service.receiveTypedWebhook(
                "retail_pos", "idem-retail-1", Map.of("event_id", "evt-retail-1"));
        assertThat(response.eventId()).isEqualTo("evt-retail-1");
        OpenApiEventRecord stored = repository.events.stream()
                .filter(e -> "evt-retail-1".equals(e.id())).findFirst().orElseThrow();
        assertThat(stored.eventStatus()).isEqualTo("accepted");
        assertThat(stored.resultJson().get("mapped")).isEqualTo(false);
        assertThat(stored.resultJson().get("mapping")).isEqualTo(Map.of());
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.webhook_received".equals(e.eventName()));
    }

    @Test
    void t14TypedWebhookRejectsUnsupportedType() {
        assertThatThrownBy(() -> service.receiveTypedWebhook("unknown_provider", "idem-x", Map.of()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("THIRD_PARTY_TYPE_UNSUPPORTED");
    }

    @Test
    void t14RobotReportWritesPassiveTraceWithEventTypeCheck() {
        RobotTaskEventRecord record = service.reportRobotTaskEvent(new RobotTaskEventRequest(
                "robot-task-1", "delivered", "handover-9", Map.of("floor", "3"), "idem-robot-1"));
        assertThat(record.eventType()).isEqualTo("delivered");
        assertThat(record.handoverId()).isEqualTo("handover-9");
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.robot_reported".equals(e.eventName()));

        assertThatThrownBy(() -> service.reportRobotTaskEvent(new RobotTaskEventRequest(
                "robot-task-2", "exploded", null, Map.of(), "idem-robot-2")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("ROBOT_EVENT_TYPE_INVALID");
    }

    @Test
    void t14RecordVideoAnchorLeavesClipIndexEmpty() {
        VideoAnchorRecord anchor = service.recordVideoAnchor(
                "point-1", Instant.parse("2026-06-18T10:00:00Z"), "verification-77");
        assertThat(anchor.pointId()).isEqualTo("point-1");
        assertThat(anchor.clipIndexRef()).isNull();
        assertThat(anchor.businessRef()).isEqualTo("verification-77");
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.video_anchor_recorded".equals(e.eventName()));
    }

    @Test
    void t14ListVideoAnchorsWritesAccessAudit() {
        service.recordVideoAnchor("point-9", Instant.parse("2026-06-18T11:00:00Z"), "handover-1");
        var page = service.listVideoAnchors("user-viewer-1", "approval-555");
        assertThat(page.items()).hasSize(1);
        assertThat(repository.videoAccessAudits)
                .anyMatch(a -> a.contains("user-viewer-1") && a.contains("approval-555"));
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.video_clip_viewed".equals(e.eventName()));
    }

    private static final class InMemoryIntegrationRepository implements IntegrationRepository {
        private final Map<String, OpenAppCredentialRecord> appCredentials = new LinkedHashMap<>();
        private final Map<String, OpenEndpointConfigRecord> endpointConfigs = new LinkedHashMap<>();
        private final List<OpenAppTokenRecord> tokens = new ArrayList<>();
        private final List<String> nonces = new ArrayList<>();
        private final List<OpenApiEventRecord> events = new ArrayList<>();
        private final Map<String, ProviderAdapterRecord> providerAdapters = new LinkedHashMap<>();
        private final List<AdapterTestRecord> adapterTests = new ArrayList<>();
        private final List<IntegrationLedgerRecord> ledgers = new ArrayList<>();
        private final Map<String, IntegrationOrderSyncRecord> orderSyncs = new LinkedHashMap<>();
        private final Map<String, IntegrationTaskRecord> retryTasks = new LinkedHashMap<>();
        private final Map<String, IntegrationTaskRecord> compensationTasks = new LinkedHashMap<>();
        private final Map<String, ReconciliationBatchRecord> reconciliationBatches = new LinkedHashMap<>();
        private final Map<String, ReconciliationDifferenceRecord> reconciliationDifferences = new LinkedHashMap<>();

        @Override
        public Optional<OpenAppCredentialRecord> findOpenAppCredential(String appKey) {
            return Optional.ofNullable(appCredentials.get(appKey));
        }

        @Override
        public boolean reserveOpenNonce(String appKey, String nonce, long timestamp) {
            String key = appKey + ":" + nonce;
            if (nonces.contains(key)) {
                return false;
            }
            nonces.add(key);
            return true;
        }

        @Override
        public OpenAppTokenRecord saveOpenToken(OpenAppTokenRecord token) {
            tokens.add(token);
            return token;
        }

        @Override
        public Optional<OpenEndpointConfigRecord> findEndpointConfig(String endpointKey) {
            return Optional.ofNullable(endpointConfigs.get(endpointKey));
        }

        @Override
        public Optional<OpenApiEventRecord> findOpenEventByIdempotency(String endpointKey, String providerCode, String idempotencyKey) {
            return events.stream()
                    .filter(event -> event.endpointKey().equals(endpointKey))
                    .filter(event -> event.providerCode().equals(providerCode))
                    .filter(event -> event.idempotencyKey().equals(idempotencyKey))
                    .findFirst();
        }

        @Override
        public Optional<OpenApiEventRecord> findOpenEventById(String endpointKey, String eventId) {
            return events.stream()
                    .filter(event -> event.endpointKey().equals(endpointKey))
                    .filter(event -> event.id().equals(eventId))
                    .findFirst();
        }

        @Override
        public OpenApiEventRecord saveOpenEvent(OpenApiEventRecord event) {
            events.add(event);
            return event;
        }

        @Override
        public List<ProviderAdapterRecord> listProviderAdapters() {
            return new ArrayList<>(providerAdapters.values());
        }

        @Override
        public Optional<ProviderAdapterRecord> findProviderAdapterById(String id) {
            return Optional.ofNullable(providerAdapters.get(id));
        }

        @Override
        public ProviderAdapterRecord saveProviderAdapter(ProviderAdapterRecord adapter) {
            providerAdapters.put(adapter.id(), adapter);
            return adapter;
        }

        @Override
        public AdapterTestRecord saveAdapterTest(AdapterTestRecord test) {
            adapterTests.add(test);
            return test;
        }

        @Override
        public List<IntegrationLedgerRecord> listLedger(String ledgerType) {
            return ledgers.stream()
                    .filter(ledger -> ledger.ledgerType().equals(ledgerType))
                    .toList();
        }

        @Override
        public IntegrationOrderSyncRecord saveOrderSync(IntegrationOrderSyncRecord record) {
            orderSyncs.put(record.id(), record);
            return record;
        }

        @Override
        public List<IntegrationOrderSyncRecord> listOrderSyncs() {
            return new ArrayList<>(orderSyncs.values());
        }

        @Override
        public Optional<IntegrationOrderSyncRecord> findOrderSyncByProviderAndExternalId(
                String providerCode,
                String businessType,
                String syncType,
                String externalBusinessId
        ) {
            return orderSyncs.values().stream()
                    .filter(record -> record.providerCode().equals(providerCode))
                    .filter(record -> record.businessType().equals(businessType))
                    .filter(record -> record.syncType().equals(syncType))
                    .filter(record -> record.externalBusinessId().equals(externalBusinessId))
                    .findFirst();
        }

        @Override
        public IntegrationTaskRecord saveRetryTask(IntegrationTaskRecord record) {
            retryTasks.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<IntegrationTaskRecord> findRetryTaskById(String id) {
            return Optional.ofNullable(retryTasks.get(id));
        }

        @Override
        public List<IntegrationTaskRecord> listRetryTasks() {
            return new ArrayList<>(retryTasks.values());
        }

        @Override
        public IntegrationTaskRecord saveCompensationTask(IntegrationTaskRecord record) {
            compensationTasks.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<IntegrationTaskRecord> findCompensationTaskById(String id) {
            return Optional.ofNullable(compensationTasks.get(id));
        }

        @Override
        public List<IntegrationTaskRecord> listCompensationTasks() {
            return new ArrayList<>(compensationTasks.values());
        }

        @Override
        public ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord record) {
            reconciliationBatches.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<ReconciliationBatchRecord> findReconciliationBatchById(String id) {
            return Optional.ofNullable(reconciliationBatches.get(id));
        }

        @Override
        public List<ReconciliationBatchRecord> listReconciliationBatches() {
            return new ArrayList<>(reconciliationBatches.values());
        }

        @Override
        public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord record) {
            reconciliationDifferences.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id) {
            return Optional.ofNullable(reconciliationDifferences.get(id));
        }

        @Override
        public List<ReconciliationDifferenceRecord> listReconciliationDifferences() {
            return new ArrayList<>(reconciliationDifferences.values());
        }

        private final List<RobotTaskEventRecord> robotTaskEvents = new ArrayList<>();
        private final Map<String, VideoAnchorRecord> videoAnchors = new LinkedHashMap<>();
        private final List<String> videoAccessAudits = new ArrayList<>();

        @Override
        public RobotTaskEventRecord saveRobotTaskEvent(RobotTaskEventRecord record) {
            robotTaskEvents.add(record);
            return record;
        }

        @Override
        public VideoAnchorRecord saveVideoAnchor(VideoAnchorRecord record) {
            videoAnchors.put(record.id(), record);
            return record;
        }

        @Override
        public List<VideoAnchorRecord> listVideoAnchors() {
            return new ArrayList<>(videoAnchors.values());
        }

        @Override
        public Optional<VideoAnchorRecord> findVideoAnchorById(String id) {
            return Optional.ofNullable(videoAnchors.get(id));
        }

        @Override
        public void saveVideoAccessAudit(String id, String anchorId, String viewerUserId, String approvalRef, Instant viewedAt) {
            videoAccessAudits.add(id + ":" + anchorId + ":" + viewerUserId + ":" + approvalRef);
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
