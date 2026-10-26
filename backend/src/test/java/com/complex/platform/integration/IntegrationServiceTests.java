package com.complex.platform.integration;

// 文件版本：V0.03

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
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
    private static final String INTEGRATION_WEBHOOK_TEST_SECRET = "test-integration-webhook-secret";

    private InMemoryIntegrationRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private IntegrationService service;
    private Map<String, String> secrets;
    private com.complex.platform.terminal.TerminalDeviceRepository terminalDeviceRepository;
    // 批6：假出票端口（记录出票指令；本地订单金额可配，供 amount_matched / 对账系统取数断言）。
    private FakeChannelTicketIssuePort ticketIssuePort;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIntegrationRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        secrets = new LinkedHashMap<>();
        secrets.put(OPEN_APP_SECRET_REF, OPEN_APP_TEST_SECRET);
        secrets.put(OPEN_WEBHOOK_SECRET_REF, OPEN_WEBHOOK_TEST_SECRET);
        secrets.put("integration.retail_pos.webhook", INTEGRATION_WEBHOOK_TEST_SECRET);
        secrets.put("integration.robot.webhook", INTEGRATION_WEBHOOK_TEST_SECRET);
        terminalDeviceRepository = org.mockito.Mockito.mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        // 默认无显式开关行 → T2 语义"默认开启"，放行（第三方默认关闭由迁移种子在真实库表达）。
        org.mockito.Mockito.when(terminalDeviceRepository.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap())).thenReturn(Optional.empty());
        com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard =
                new com.complex.platform.gate.TerminalSwitchGuard(
                        terminalDeviceRepository, new AuditEventService(auditEventRepository));
        ticketIssuePort = new FakeChannelTicketIssuePort();
        service = new IntegrationService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                Clock.fixed(NOW, ZoneOffset.UTC),
                secretRef -> Optional.ofNullable(secrets.get(secretRef)),
                terminalSwitchGuard,
                ticketIssuePort
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

    // ─── P0-E（v7 审计）对账旁路手填封堵 ────────────────────────────────────────

    private ReconciliationBatchRecord seedReconciliationBatch(String idempotencyKey) {
        return service.createReconciliationBatch(new ReconciliationBatchRequest(
                idempotencyKey,
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
    }

    private void seedProviderReflow(String idempotencyKey, String externalBusinessId, long amountCent) {
        service.createOrderSync(new IntegrationOrderSyncRequest(
                idempotencyKey,
                "record catering order",
                List.of(),
                "mock-provider",
                "catering",
                "order",
                externalBusinessId,
                "synced",
                amountCent,
                "CNY",
                "meituan",
                "mt-" + externalBusinessId,
                null,
                Map.of()
        ));
    }

    private ReconciliationDifferenceRequest registrationRequest(
            String idempotencyKey, String batchId, String externalBusinessId,
            Long internal, Long provider, Long difference) {
        return new ReconciliationDifferenceRequest(
                idempotencyKey,
                "diff note",
                List.of(),
                batchId,
                "mock-provider",
                "catering",
                externalBusinessId,
                "amount_mismatch",
                internal,
                provider,
                difference,
                null,
                Map.of()
        );
    }

    @Test
    void p0eReconciliationDifferenceRejectsManuallyFilledAmounts() {
        ReconciliationBatchRecord batch = seedReconciliationBatch("batch-p0e-manual-amount");
        seedProviderReflow("sync-p0e-manual", "ext-order-manual", 128700L);

        // 平台侧金额手填 → 拒（旁路封堵的核心断言）。
        assertThatThrownBy(() -> service.createReconciliationDifference(
                registrationRequest("diff-p0e-manual-internal", batch.id(), "ext-order-manual", 128800L, null, null),
                "ops_1"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN");

        // 第三方侧金额手填 → 拒。
        assertThatThrownBy(() -> service.createReconciliationDifference(
                registrationRequest("diff-p0e-manual-provider", batch.id(), "ext-order-manual", null, 128700L, null),
                "ops_1"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN");

        // 差额手填 → 拒。
        assertThatThrownBy(() -> service.createReconciliationDifference(
                registrationRequest("diff-p0e-manual-diff", batch.id(), "ext-order-manual", null, null, 999L),
                "ops_1"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN");
    }

    @Test
    void p0eReconciliationDifferenceRejectsRegistrationWithoutSystemTakings() {
        ReconciliationBatchRecord batch = seedReconciliationBatch("batch-p0e-no-takings");

        // 该外部单号第三方毫无回流 → 纯手填差异无处可生，直接拒。
        assertThatThrownBy(() -> service.createReconciliationDifference(
                registrationRequest("diff-p0e-no-takings", batch.id(), "ext-order-ghost", null, null, null),
                "ops_1"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECONCILIATION_NO_SYSTEM_TAKINGS");
    }

    @Test
    void p0eReconciliationDifferenceUsesSystemTakingsAndForcesReview() {
        ReconciliationBatchRecord batch = seedReconciliationBatch("batch-p0e-system");
        seedProviderReflow("sync-p0e-system", "ext-order-system", 128700L);

        ReconciliationDifferenceRecord difference = service.createReconciliationDifference(
                registrationRequest("diff-p0e-system", batch.id(), "ext-order-system", null, null, null),
                "ops_1");

        // 第三方侧 = 系统取数；平台侧本域无取数源恒 0；差额 = 两者之差。
        assertThat(difference.providerAmountCent()).isEqualTo(128700L);
        assertThat(difference.internalAmountCent()).isZero();
        assertThat(difference.differenceAmountCent()).isEqualTo(128700L);
        // 人工登记强制进复核 + 留登记人。
        assertThat(difference.status()).isEqualTo("needs_review");
        assertThat(difference.amountSource()).isEqualTo("manual_registration");
        assertThat(difference.registeredByUserId()).isEqualTo("ops_1");
    }

    @Test
    void p0eReconciliationDifferenceRegistrantCannotSelfReview() {
        ReconciliationBatchRecord batch = seedReconciliationBatch("batch-p0e-self-review");
        seedProviderReflow("sync-p0e-self-review", "ext-order-self-review", 50000L);
        ReconciliationDifferenceRecord difference = service.createReconciliationDifference(
                registrationRequest("diff-p0e-self-review", batch.id(), "ext-order-self-review", null, null, null),
                "ops_1");

        ReconciliationDifferenceResolveRequest resolveRequest = new ReconciliationDifferenceResolveRequest(
                "resolve-p0e-self-review",
                "resolved",
                List.of("att-diff"),
                "ops_1",
                "resolved",
                "自审企图");

        // 登记人自己复核 → 403。
        assertThatThrownBy(() -> service.resolveReconciliationDifference(difference.id(), resolveRequest, "ops_1"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SELF_REVIEW_FORBIDDEN");

        // 换一位同事复核 → 通过，且金额不因复核而改变。
        ReconciliationDifferenceRecord resolved = service.resolveReconciliationDifference(
                difference.id(),
                new ReconciliationDifferenceResolveRequest(
                        "resolve-p0e-self-review-2",
                        "resolved",
                        List.of("att-diff"),
                        "ops_2",
                        "resolved",
                        "复核通过"),
                "ops_2");
        assertThat(resolved.status()).isEqualTo("resolved");
        assertThat(resolved.providerAmountCent()).isEqualTo(50000L);
        assertThat(resolved.internalAmountCent()).isZero();
        assertThat(resolved.registeredByUserId()).isEqualTo("ops_1");
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


    /** HP-P01：第三方停车系统可登记为供应商适配器（BSM「连接第三方接入，停车系统回传入出场记录、缴费结果和车位占用」）。 */
    @Test
    void parkingAdapterTypeAccepted() {
        org.assertj.core.api.Assertions.assertThatCode(() -> IntegrationValidation.validateCreateRequest(new ProviderAdapterRequest(
                "pk-adapter", null, java.util.List.of(), "pk-vendor", "parking", "mock", Map.of(), Map.of(), Map.of(), false)))
                .doesNotThrowAnyException();
    }

    @Test
    void webhookPersistsEventAndDuplicateIdempotencyReturnsOriginalResult() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_id", "evt-webhook");
        payload.put("idempotency_key", "idem-webhook");
        payload.put("payload", Map.of("normalized_status", "paid"));

        // 合法重试：相同 idempotency_key + 新 nonce → 第二次幂等去重返回原结果。
        WebhookAcceptedResponse first =
                postWebhook("p0-default", "mock-provider", "payment.paid", "idem-webhook", payload);
        WebhookAcceptedResponse second =
                postWebhook("p0-default", "mock-provider", "payment.paid", "idem-webhook", payload);

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
    void webhookRejectsReplayOfSameNonce() {
        // 原报文重放（同 nonce + 同签名）→ OPEN_WEBHOOK_NONCE_REPLAYED，堵捕获报文重放。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_id", "evt-replay");
        payload.put("idempotency_key", "idem-replay");
        payload.put("payload", Map.of("normalized_status", "paid"));

        WebhookAcceptedResponse first =
                postWebhook("p0-default", "mock-provider", "payment.paid", "idem-replay", payload, "nonce-fixed");
        assertThat(first.deduplicated()).isFalse();

        assertThatThrownBy(() ->
                postWebhook("p0-default", "mock-provider", "payment.paid", "idem-replay", payload, "nonce-fixed"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("OPEN_WEBHOOK_NONCE_REPLAYED"));
    }

    @Test
    void webhookRejectsStaleTimestamp() {
        // 时效性：过期 timestamp（超容差）→ OPEN_TIMESTAMP_STALE。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_id", "evt-stale");
        payload.put("idempotency_key", "idem-stale");
        payload.put("payload", Map.of("normalized_status", "paid"));
        long staleTs = NOW.toEpochMilli() - java.time.Duration.ofDays(2).toMillis();
        String signature = webhookSignature(
                "p0-default", "mock-provider", "payment.paid", "idem-stale", payload, staleTs, "nonce-stale");

        assertThatThrownBy(() -> service.receiveWebhook(
                "p0-default", "mock-provider", "payment.paid", signature, staleTs, "nonce-stale", null, payload))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("OPEN_TIMESTAMP_STALE"));
    }

    @Test
    void webhookRejectsSignatureWhenPayloadChanges() {
        Map<String, Object> signedPayload = new LinkedHashMap<>();
        signedPayload.put("event_id", "evt-webhook-tamper");
        signedPayload.put("idempotency_key", "idem-webhook-tamper");
        signedPayload.put("payload", Map.of("normalized_status", "paid"));
        long ts = NOW.toEpochMilli();
        String signature = webhookSignature(
                "p0-default", "mock-provider", "payment.paid", "idem-webhook-tamper", signedPayload, ts, "nonce-tamper");

        Map<String, Object> tamperedPayload = new LinkedHashMap<>();
        tamperedPayload.put("event_id", "evt-webhook-tamper");
        tamperedPayload.put("idempotency_key", "idem-webhook-tamper");
        tamperedPayload.put("payload", Map.of("normalized_status", "refunded"));

        assertThatThrownBy(() -> service.receiveWebhook(
                "p0-default",
                "mock-provider",
                "payment.paid",
                signature,
                ts,
                "nonce-tamper",
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
        postWebhook("p0-default", "mock-provider", "payment.paid", "idem-no-map", payload);

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
        postWebhook("p0-default", "mock-provider", "payment.paid", "idem-map", payload);

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
        postWebhook("p0-default", "mock-provider", "payment.paid", "idem-status", payload);

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
        WebhookAcceptedResponse response =
                postWebhook("p0-default", "mock-provider", "payment.paid", "idem-bad", payload);

        assertThat(response.deduplicated()).isFalse();
        OpenApiEventRecord saved = repository.events.getFirst();
        assertThat(saved.eventStatus()).isEqualTo("map_failed");
        assertThat(saved.failureReason()).isNotBlank();
        @SuppressWarnings("unchecked")
        Map<String, Object> failurePlaceholder = (Map<String, Object>) saved.resultJson().get("mapping");
        assertThat(failurePlaceholder).containsEntry("reason_code", "WEBHOOK_MAP_FAILURE");
        // 幂等去重仍生效（合法重试用新 nonce + 同 idempotency_key）。
        WebhookAcceptedResponse dup =
                postWebhook("p0-default", "mock-provider", "payment.paid", "idem-bad", payload);
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
        postWebhook("p0-default", "mock-provider", "payment.paid", "idem-off", payload);

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
                "testing",
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
                "testing",
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
        assertThat(ready.status()).isEqualTo("active");
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
                "pending",
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
                "adapter-fail", "mock-provider", List.of("webhook"), "mock", "pending",
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
                "adapter-ok", "mock-provider", List.of("webhook"), "mock", "pending",
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
                "adapter-force", "mock-provider", List.of("webhook"), "mock", "pending",
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
                "adapter-unknown", "mock-provider", List.of("webhook"), "mock", "pending",
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

    // 第十六批第 34 条（ENUM L211「接入状态 待接入、联调中、已启用、暂停、异常、已停用、归档」）：新建一律「待接入」；
    // 修改按前端真实请求形状（蛇形键）只收七态、旧五态码不再收；只有改为「已启用」才标生产就绪（仍按生产条件校验）。
    @Test
    void providerAdapterAccessStatusUsesEnumSevenStates() throws Exception {
        ProviderAdapterRecord created = service.createProviderAdapter(new ProviderAdapterRequest(
                "seven-create", null, List.of(), "seven-provider", "payment", "mock", Map.of(), Map.of(), Map.of(), false));
        assertThat(created.status()).isEqualTo("pending");

        ObjectMapper snake = new ObjectMapper()
                .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        String body = """
                {"idempotency_key":"seven-%s","provider_code":"seven-provider","capabilities":["webhook","polling"],
                 "environment":"production","status":"%s","webhook_enabled":true,"polling_check_enabled":true,
                 "field_mapping_placeholder":{},"credential_secret_ref":"manual-ledger:seven-provider"}
                """;
        assertThatThrownBy(() -> service.updateProviderAdapter(created.id(),
                snake.readValue(body.formatted("old", "test_ready"), ProviderAdapterUpdateRequest.class)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        for (String status : List.of("pending", "testing", "paused", "abnormal", "disabled", "archived")) {
            ProviderAdapterRecord saved = service.updateProviderAdapter(created.id(),
                    snake.readValue(body.formatted(status, status), ProviderAdapterUpdateRequest.class));
            assertThat(saved.status()).isEqualTo(status);
            assertThat(saved.productionReady()).isFalse();
        }
        ProviderAdapterRecord active = service.updateProviderAdapter(created.id(),
                snake.readValue(body.formatted("active", "active"), ProviderAdapterUpdateRequest.class));
        assertThat(active.status()).isEqualTo("active");
        assertThat(active.productionReady()).isTrue();
    }

    // 第十六批第 34 条（第三方接入 PRD §5「暂停：临时停止回流或使用」「已停用：不再作为当前经营来源」「归档：历史接入关系」）：
    // 暂停、已停用、归档的适配器与已停用同口径——回调不按其映射、不能做模拟测试。
    @Test
    void pausedOrArchivedAdapterIsNotUsedForMappingOrMockTests() {
        repository.providerAdapters.put("map-adapter", withFieldMapping(adapter("map-adapter", "mock-provider", List.of("webhook"),
                "production", "paused", true, true, "manual-ledger:p0-provider", false), Map.of("order_no", "payload.order_id"), true));
        postWebhook("p0-default", "mock-provider", "payment.paid", "idem-paused", webhookPayload("evt-paused", "idem-paused"));
        assertThat(repository.events.getFirst().eventStatus()).isEqualTo("accepted");

        for (String status : List.of("paused", "archived")) {
            repository.providerAdapters.put("adapter-" + status, adapter("adapter-" + status, "mock-provider", List.of("webhook"),
                    "mock", status, true, true, "manual-ledger:p0-provider", false));
            assertThatThrownBy(() -> service.testProviderAdapterMock("adapter-" + status, new ProviderAdapterTestRequest(
                    "mock-" + status, "mock-provider", "webhook", Map.of(), "note", List.of())))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PROVIDER_ADAPTER_NOT_TESTABLE"));
        }
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
                "meituan",
                "mt-channel-order-1",
                null,
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
        // P0-E：登记差异不带任何金额（金额一律系统取数：external-order-1 的回流 128800）。
        ReconciliationDifferenceRecord difference = service.createReconciliationDifference(new ReconciliationDifferenceRequest(
                "reconcile-difference",
                "missing voucher",
                List.of(),
                batch.id(),
                "mock-provider",
                "catering",
                "external-order-1",
                "amount_mismatch",
                null,
                null,
                null,
                "finance_1",
                Map.of("provider_private_status", "DIFF")
        ), "ops_register_1");
        // 登记人 ops_register_1 不得自审 → 由 finance_1 复核。
        ReconciliationDifferenceRecord resolvedDifference = service.resolveReconciliationDifference(
                difference.id(),
                new ReconciliationDifferenceResolveRequest(
                        "reconcile-resolve",
                        "resolved",
                        List.of("att-diff"),
                        "finance_1",
                        "resolved",
                        "confirmed platform amount"
                ),
                "finance_1"
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

    @Test
    void p0ChannelOrderSyncsAreDistinguishableByChannelAndExternalChannelOrderId() {
        IntegrationOrderSyncRecord meituan = service.createOrderSync(orderSyncRequest(
                "ch-mt", "ext-shared-1", "meituan", "mt-1", 10000L, "synced"));
        IntegrationOrderSyncRecord douyin = service.createOrderSync(orderSyncRequest(
                "ch-dy", "ext-shared-2", "douyin", "dy-1", 20000L, "synced"));

        assertThat(meituan.channel()).isEqualTo("meituan");
        assertThat(meituan.externalChannelOrderId()).isEqualTo("mt-1");
        assertThat(douyin.channel()).isEqualTo("douyin");
        // mapPayload 占位标准模型保留渠道语义，开关 off / 无适配器时 mock=true 不伪造。
        @SuppressWarnings("unchecked")
        Map<String, Object> standard = (Map<String, Object>) meituan.providerPayload().get("standard_model");
        assertThat(standard).containsEntry("channel", "meituan").containsEntry("mock", true);
        assertThat(meituan.providerPayload()).containsEntry("channel_map_status", "unmapped");
    }

    @Test
    void p0AutoReconciliationGeneratesDifferencesFromStandardModelWhenSwitchOn() {
        // 适配器 webhook 开 → 自动对账落库（非 dryRun）。
        repository.providerAdapters.put("recon-adapter", adapter(
                "recon-adapter", "mock-provider", List.of("webhook"), "production", "active",
                true, true, "manual-ledger:p0", true));
        // 第三方侧 actual：order_syncs 标准模型。
        service.createOrderSync(orderSyncRequest("a1", "ext-amount", "meituan", "mt-amount", 9900L, "synced"));
        service.createOrderSync(orderSyncRequest("a2", "ext-extra", "meituan", "mt-extra", 5000L, "synced"));

        ChannelReconciliationResult result = service.reconcileChannel(new ChannelReconciliationRequest(
                "recon-1",
                "mock-provider",
                "catering",
                "meituan",
                LocalDate.parse("2026-06-14"),
                LocalDate.parse("2026-06-14"),
                false,
                List.of(
                        // 平台侧期望：金额差异（10000 vs 9900）。
                        new ChannelReconciliationRequest.ExpectedEntry("ext-amount", 10000L, "synced", 1, Map.of()),
                        // 平台侧期望但第三方缺口（missing）。
                        new ChannelReconciliationRequest.ExpectedEntry("ext-missing", 8000L, "synced", 1, Map.of())
                )
        ));

        assertThat(result.dryRun()).isFalse();
        assertThat(result.batchId()).isNotNull();
        // amount(ext-amount) + missing(ext-missing) + count(ext-extra 未被期望匹配) = 3。
        assertThat(result.differences())
                .extracting(ReconciliationDifferenceRecord::differenceType)
                .containsExactlyInAnyOrder("amount", "missing", "count");
        assertThat(result.differences())
                .allSatisfy(d -> assertThat(d.sourceDomain()).isEqualTo("thirdparty"));
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.channel_reconciliation.completed".equals(e.eventName()));
    }

    @Test
    void p0AutoReconciliationDoesNotFabricateWhenSwitchOff() {
        // 无 webhook 适配器（开关 off）→ 强制 dryRun，不落任何对账数据。
        service.createOrderSync(orderSyncRequest("o1", "ext-x", "meituan", "mt-x", 9900L, "synced"));

        ChannelReconciliationResult result = service.reconcileChannel(new ChannelReconciliationRequest(
                "recon-off",
                "mock-provider",
                "catering",
                "meituan",
                LocalDate.parse("2026-06-14"),
                LocalDate.parse("2026-06-14"),
                false,
                List.of(new ChannelReconciliationRequest.ExpectedEntry("ext-x", 10000L, "synced", 1, Map.of()))
        ));

        assertThat(result.dryRun()).isTrue();
        assertThat(result.batchId()).isNull();
        assertThat(result.differences()).isEmpty();
        // 预览仍计算差异数（amount 不匹配），但不落库、不伪造。
        assertThat(result.differenceCount()).isEqualTo(1);
        assertThat(repository.reconciliationBatches).isEmpty();
        assertThat(repository.reconciliationDifferences).isEmpty();
    }

    @Test
    void orderSyncResolvesMerchantViaStoreMapDirectAndLeavesNullWhenUnmapped() {
        // 源B：对账文件回流行本带商户（真实存在）→ 直落。
        repository.merchants.add("merchant_direct");
        IntegrationOrderSyncRecord direct = service.createOrderSync(new IntegrationOrderSyncRequest(
                "k-src-b", "对账文件回流", List.of(), "mock-provider", "catering", "order",
                "ext-b-1", "synced", 50000L, "CNY", "meituan", "ch-b-1",
                "merchant_direct", Map.of()));
        assertThat(direct.merchantId()).isEqualTo("merchant_direct");

        // 源A：登记渠道门店映射，回流带门店号 → 反查命中写商户。
        service.saveChannelStoreMap(new IntegrationChannelStoreMapRequest(
                "k-map-1", "mock-provider", "douyin", "store-777", "merchant_mapped"));
        IntegrationOrderSyncRecord mapped = service.createOrderSync(new IntegrationOrderSyncRequest(
                "k-src-a", "渠道回流", List.of(), "mock-provider", "catering", "order",
                "ext-a-1", "synced", 60000L, "CNY", "douyin", "ch-a-1",
                null, Map.of("external_store_id", "store-777")));
        assertThat(mapped.merchantId()).isEqualTo("merchant_mapped");

        // 未命中门店号 → 留 NULL，进未归属视图，不静默塞给某商户。
        IntegrationOrderSyncRecord unmapped = service.createOrderSync(new IntegrationOrderSyncRequest(
                "k-src-a2", "渠道回流", List.of(), "mock-provider", "catering", "order",
                "ext-a-2", "synced", 70000L, "CNY", "douyin", "ch-a-2",
                null, Map.of("external_store_id", "store-unknown")));
        assertThat(unmapped.merchantId()).isNull();
        assertThat(service.listUnattributedOrderSyncs().items())
                .extracting(IntegrationOrderSyncRecord::externalBusinessId).contains("ext-a-2");

        // 软删映射后同门店号回流不再命中（inactive 绑定不参与反查）。
        IntegrationChannelStoreMapRecord map = service.listChannelStoreMaps(true, java.util.Set.of(), null).items().stream()
                .filter(m -> "store-777".equals(m.externalStoreId())).findFirst().orElseThrow();
        service.archiveChannelStoreMap(map.id(),
                new StatusChangeRequest("k-arch-1", "archive", "门店解绑", List.of()));
        IntegrationOrderSyncRecord afterArchive = service.createOrderSync(new IntegrationOrderSyncRequest(
                "k-src-a3", "渠道回流", List.of(), "mock-provider", "catering", "order",
                "ext-a-3", "synced", 80000L, "CNY", "douyin", "ch-a-3",
                null, Map.of("external_store_id", "store-777")));
        assertThat(afterArchive.merchantId()).isNull();
    }

    @Test
    void orderSyncSourceBWithUnknownMerchantFallsToUnattributedNotBogusAttribution() {
        // P1 资金复核：源B 对账文件回流带商户号，但该号在 merchants 表查无此人（拼错 / 过期）→ 置 NULL
        // 进未归属，绝不静默塞给不存在的商户（否则店级抽成 / 租售比 / 未归属桶三处同时隐形）。
        repository.merchants.add("merchant_real");

        // 存在的商户号 → 正常归属。
        IntegrationOrderSyncRecord attributed = service.createOrderSync(new IntegrationOrderSyncRequest(
                "k-b-real", "对账文件回流", List.of(), "mock-provider", "catering", "order",
                "ext-b-real", "synced", 50000L, "CNY", "meituan", "ch-b-real",
                "merchant_real", Map.of()));
        assertThat(attributed.merchantId()).isEqualTo("merchant_real");

        // 查无此人的商户号 → 落 NULL 未归属。
        IntegrationOrderSyncRecord bogus = service.createOrderSync(new IntegrationOrderSyncRequest(
                "k-b-bogus", "对账文件回流", List.of(), "mock-provider", "catering", "order",
                "ext-b-bogus", "synced", 60000L, "CNY", "meituan", "ch-b-bogus",
                "merchant_typo_999", Map.of()));
        assertThat(bogus.merchantId()).isNull();
        assertThat(service.listUnattributedOrderSyncs().items())
                .extracting(IntegrationOrderSyncRecord::externalBusinessId)
                .contains("ext-b-bogus")
                .doesNotContain("ext-b-real");
    }

    // ─── 批6 第三方通票适配（TDD：出票链路动钱动票，先测后行）─────────────────────

    private void seedTicketTypeMap(String externalProductId, String ticketTypeId, String priceRuleId) {
        service.saveChannelTicketTypeMap(new IntegrationChannelTicketTypeMapRequest(
                "map-" + externalProductId, "mock-provider", "meituan", externalProductId, ticketTypeId, priceRuleId));
    }

    private IntegrationOrderSyncRequest ticketOrderSyncRequest(
            String idempotencyKey, String externalBusinessId, long amountCent, Map<String, Object> payload) {
        return new IntegrationOrderSyncRequest(
                idempotencyKey,
                "ticket-test",
                List.of(),
                "mock-provider",
                "ticket",
                "order",
                externalBusinessId,
                "received",
                amountCent,
                "CNY",
                "meituan",
                "mt-" + externalBusinessId,
                null,
                payload);
    }

    private void closeTicketChannelSwitch() {
        org.mockito.Mockito.when(terminalDeviceRepository.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("integration.ticket_channel.sync"),
                org.mockito.ArgumentMatchers.anyMap())).thenReturn(Optional.of(
                new com.complex.platform.terminal.TerminalCapabilitySwitchRecord(
                        "tcs_int_ticket_chan_off", "*", "integration.ticket_channel.sync",
                        "global", "global", false, "批6 默认关", NOW, NOW)));
    }

    @Test
    void ticketReflowRejectedWhenChannelSwitchOff() {
        // 开关关（V071 种子语义）→ 拒收：明确报「渠道未启用」，不落台账、不出票。
        closeTicketChannelSwitch();
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);

        assertThatThrownBy(() -> service.createOrderSync(ticketOrderSyncRequest(
                "tk-off", "ext-tk-off", 8800L, Map.of("external_product_id", "mt-prod-1"))))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("INTEGRATION_TICKET_CHANNEL_DISABLED");
                    assertThat(exception.getMessage()).contains("渠道未启用");
                });
        assertThat(ticketIssuePort.commands).isEmpty();
        assertThat(repository.orderSyncs).isEmpty();
    }

    @Test
    void ticketReflowRejectedWhenMappingMissing() {
        // 票种映射缺失 → fail-closed 中文拒收，不落台账、不出票。
        assertThatThrownBy(() -> service.createOrderSync(ticketOrderSyncRequest(
                "tk-nomap", "ext-tk-nomap", 8800L, Map.of("external_product_id", "mt-prod-x"))))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("INTEGRATION_CHANNEL_TICKET_MAP_MISSING");
                    assertThat(exception.getMessage()).contains("渠道票种映射缺失");
                });
        assertThat(ticketIssuePort.commands).isEmpty();
        assertThat(repository.orderSyncs).isEmpty();
    }

    @Test
    void ticketReflowRejectedWhenProductIdMissing() {
        assertThatThrownBy(() -> service.createOrderSync(ticketOrderSyncRequest(
                "tk-noprod", "ext-tk-noprod", 8800L, Map.of())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INTEGRATION_TICKET_PRODUCT_ID_REQUIRED"));
        assertThat(ticketIssuePort.commands).isEmpty();
    }

    @Test
    void ticketReflowIssuesLocalTicketThroughMappedTicketType() {
        // 回流 → 映射解析 → 走出票主链（fake 端口）：记录 sales_channel=meituan（S11 渠道费按渠道解析的钥匙）。
        seedTicketTypeMap("mt-prod-1", "tt_pass", "rule_groupbuy");
        ticketIssuePort.amountCent = 8800L;

        IntegrationOrderSyncRecord record = service.createOrderSync(ticketOrderSyncRequest(
                "tk-ok", "ext-tk-ok", 8800L,
                Map.of("external_product_id", "mt-prod-1", "quantity", 2, "visit_date", "2026-06-15",
                        "buyer_phone", "13800000000")));

        assertThat(record.status()).isEqualTo("synced");
        assertThat(record.providerPayload())
                .containsKey("local_ticket_order_id")
                .containsEntry("local_amount_cent", 8800L)
                .containsEntry("issued_ticket_count", 2)
                .containsEntry("amount_matched", true);
        assertThat(ticketIssuePort.commands).hasSize(1);
        ChannelTicketIssuePort.IssueCommand command = ticketIssuePort.commands.get(0);
        assertThat(command.ticketTypeId()).isEqualTo("tt_pass");
        assertThat(command.priceRuleId()).isEqualTo("rule_groupbuy");
        assertThat(command.salesChannel()).isEqualTo("meituan");
        assertThat(command.quantity()).isEqualTo(2);
        assertThat(command.visitDate()).isEqualTo(LocalDate.parse("2026-06-15"));
        assertThat(command.buyerPhone()).isEqualTo("13800000000");
        assertThat(command.idempotencyBase()).startsWith("tpk");
    }

    @Test
    void duplicateTicketReflowIssuesOnlyOnce() {
        // 同渠道订单号重复回流（不同 idempotency_key）→ 只出一次票，返回既有台账行。
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        IntegrationOrderSyncRecord first = service.createOrderSync(ticketOrderSyncRequest(
                "tk-dup-1", "ext-tk-dup", 8800L, Map.of("external_product_id", "mt-prod-1")));
        IntegrationOrderSyncRecord second = service.createOrderSync(ticketOrderSyncRequest(
                "tk-dup-2", "ext-tk-dup", 8800L, Map.of("external_product_id", "mt-prod-1")));

        assertThat(ticketIssuePort.commands).hasSize(1);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.orderSyncs).hasSize(1);
    }

    @Test
    void suspendedLateReflowPersistsRefundAndNeverBecomesIssuedAfterLift() {
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        ticketIssuePort.suspended = true;
        IntegrationOrderSyncRecord first = service.createOrderSync(ticketOrderSyncRequest(
                "tk-blocked", "ext-tk-blocked", 8800L,
                Map.of("external_product_id", "mt-prod-1", "paid_at", "2026-06-14T19:59:00Z")));
        assertThat(first.status()).isEqualTo("failed");
        assertThat(first.providerPayload()).containsEntry("rejection_code", "PARK_WIDE_FAULT_SALES_SUSPENDED")
                .containsKey("refund_id").doesNotContainKey("local_ticket_order_id");
        assertThat(ticketIssuePort.commands).isEmpty();
        ticketIssuePort.suspended = false;
        IntegrationOrderSyncRecord replay = service.createOrderSync(ticketOrderSyncRequest(
                "tk-blocked-replay", "ext-tk-blocked", 8800L, Map.of("external_product_id", "mt-prod-1")));
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.providerPayload().get("refund_id")).isEqualTo(first.providerPayload().get("refund_id"));
        assertThat(ticketIssuePort.commands).isEmpty();
        assertThat(ticketIssuePort.refundCount).isEqualTo(1);
    }

    @Test
    void suspendedZeroAmountReflowIsRejectedWithoutCreatingRefundAndCannotIssueAfterLift() {
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        ticketIssuePort.suspended = true;
        IntegrationOrderSyncRecord first = service.createOrderSync(ticketOrderSyncRequest(
                "tk-zero", "ext-tk-zero", 0L, Map.of("external_product_id", "mt-prod-1")));
        assertThat(first.status()).isEqualTo("failed");
        assertThat(first.providerPayload()).containsEntry("rejection_code", "PARK_WIDE_FAULT_SALES_SUSPENDED")
                .doesNotContainKeys("refund_id", "local_ticket_order_id");
        ticketIssuePort.suspended = false;
        IntegrationOrderSyncRecord replay = service.createOrderSync(ticketOrderSyncRequest(
                "tk-zero-replay", "ext-tk-zero", 0L, Map.of("external_product_id", "mt-prod-1")));
        assertThat(replay).isEqualTo(first);
        assertThat(ticketIssuePort.commands).isEmpty();
        assertThat(ticketIssuePort.refundCount).isZero();
    }

    @Test
    void previouslyAcceptedReflowStaysAcceptedDuringFault() {
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        IntegrationOrderSyncRecord first = service.createOrderSync(ticketOrderSyncRequest(
                "tk-accepted", "ext-tk-accepted", 8800L, Map.of("external_product_id", "mt-prod-1")));
        ticketIssuePort.suspended = true;
        IntegrationOrderSyncRecord replay = service.createOrderSync(ticketOrderSyncRequest(
                "tk-accepted-replay", "ext-tk-accepted", 8800L, Map.of("external_product_id", "mt-prod-1")));
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.status()).isEqualTo("synced");
        assertThat(ticketIssuePort.commands).hasSize(1);
        assertThat(ticketIssuePort.refundCount).isZero();
    }

    @Test
    void ticketReflowAmountMismatchFlaggedNotBlocked() {
        // 渠道实收 9900 ≠ 本地口径 8800：客人已在渠道付讫，票照出，amount_matched=false 进对账吐差异。
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        ticketIssuePort.amountCent = 8800L;

        IntegrationOrderSyncRecord record = service.createOrderSync(ticketOrderSyncRequest(
                "tk-mis", "ext-tk-mis", 9900L, Map.of("external_product_id", "mt-prod-1")));

        assertThat(record.status()).isEqualTo("synced");
        assertThat(record.providerPayload()).containsEntry("amount_matched", false);
        assertThat(ticketIssuePort.commands).hasSize(1);
    }

    @Test
    void ticketRefundReflowRecordedWithoutLocalAutoRefund() {
        // 退款回流：首批只落台账进对账；本地退款必须走既有退款审批链（先审后行），绝不自动退。
        IntegrationOrderSyncRecord record = service.createOrderSync(new IntegrationOrderSyncRequest(
                "tk-refund", "ticket-test", List.of(), "mock-provider", "ticket", "refund",
                "ext-tk-refund", "received", 8800L, "CNY", "meituan", "mt-ext-tk-refund", null, Map.of()));

        assertThat(record.status()).isEqualTo("received");
        assertThat(record.syncType()).isEqualTo("refund");
        assertThat(ticketIssuePort.commands).isEmpty();
    }

    @Test
    void channelTicketTypeMapUpsertsByNaturalKeyAndArchives() {
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        String firstId = repository.channelTicketTypeMaps.values().iterator().next().id();
        // 重录同一渠道商品 → 复用既有 id 改绑票种（自然键 upsert，不长重复行）。
        service.saveChannelTicketTypeMap(new IntegrationChannelTicketTypeMapRequest(
                "map-rebind", "mock-provider", "meituan", "mt-prod-1", "tt_pass_v2", "rule_v2"));
        assertThat(repository.channelTicketTypeMaps).hasSize(1);
        IntegrationChannelTicketTypeMapRecord rebound = repository.channelTicketTypeMaps.get(firstId);
        assertThat(rebound.ticketTypeId()).isEqualTo("tt_pass_v2");
        assertThat(rebound.priceRuleId()).isEqualTo("rule_v2");
        // 停用（软删）→ active 查询不再命中，回流拒收。
        service.archiveChannelTicketTypeMap(firstId, new StatusChangeRequest("map-archive", null, "停用测试", List.of()));
        assertThat(repository.findActiveChannelTicketTypeMap("mock-provider", "meituan", "mt-prod-1")).isEmpty();
        assertThatThrownBy(() -> service.createOrderSync(ticketOrderSyncRequest(
                "tk-after-archive", "ext-tk-arch", 8800L, Map.of("external_product_id", "mt-prod-1"))))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INTEGRATION_CHANNEL_TICKET_MAP_MISSING"));
    }

    @Test
    void ticketChannelAutoReconciliationDerivesExpectedFromLocalOrders() {
        // ticket 自动对账系统取数：平台侧期望=本地票订单实收（经端口直读），金额一致 → matched 零差异。
        repository.providerAdapters.put("tk-recon-adapter", adapter(
                "tk-recon-adapter", "mock-provider", List.of("webhook"), "production", "active",
                true, true, "manual-ledger:p0", true));
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        ticketIssuePort.amountCent = 8800L;
        service.createOrderSync(ticketOrderSyncRequest(
                "tk-recon-1", "ext-tk-recon", 8800L, Map.of("external_product_id", "mt-prod-1")));

        ChannelReconciliationResult matched = service.reconcileChannel(new ChannelReconciliationRequest(
                "tk-recon-run-1", "mock-provider", "ticket", "meituan",
                LocalDate.parse("2026-06-14"), LocalDate.parse("2026-06-14"), false, null));
        assertThat(matched.dryRun()).isFalse();
        assertThat(matched.differenceCount()).isZero();
        assertThat(matched.expectedCount()).isEqualTo(1);
        assertThat(matched.actualCount()).isEqualTo(1);
    }

    @Test
    void ticketChannelAutoReconciliationFlagsAmountDifference() {
        // 渠道实收 9900 ≠ 本地订单 8800 → amount 差异（source_domain=thirdparty 进异常复核）。
        repository.providerAdapters.put("tk-recon-adapter", adapter(
                "tk-recon-adapter", "mock-provider", List.of("webhook"), "production", "active",
                true, true, "manual-ledger:p0", true));
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        ticketIssuePort.amountCent = 8800L;
        service.createOrderSync(ticketOrderSyncRequest(
                "tk-recon-mis", "ext-tk-recon-mis", 9900L, Map.of("external_product_id", "mt-prod-1")));

        ChannelReconciliationResult result = service.reconcileChannel(new ChannelReconciliationRequest(
                "tk-recon-run-2", "mock-provider", "ticket", "meituan",
                LocalDate.parse("2026-06-14"), LocalDate.parse("2026-06-14"), false, null));
        assertThat(result.differences())
                .extracting(ReconciliationDifferenceRecord::differenceType)
                .containsExactly("amount");
        assertThat(result.differences().get(0).internalAmountCent()).isEqualTo(8800L);
        assertThat(result.differences().get(0).providerAmountCent()).isEqualTo(9900L);
    }

    @Test
    void verificationCallbackEnqueuedOnlyForChannelTickets() {
        // 渠道票核销 → 落回传队列（integration_retry_tasks，task_type=verification_callback，锚定核销 id 幂等）；
        // 本地售出票（无回流行）→ 静默跳过。
        seedTicketTypeMap("mt-prod-1", "tt_pass", null);
        IntegrationOrderSyncRecord record = service.createOrderSync(ticketOrderSyncRequest(
                "tk-cb", "ext-tk-cb", 8800L, Map.of("external_product_id", "mt-prod-1")));
        String localOrderId = record.providerPayload().get("local_ticket_order_id").toString();

        TicketChannelCallbackEnqueuer enqueuer = new TicketChannelCallbackEnqueuer(
                repository, Clock.fixed(NOW, ZoneOffset.UTC));
        com.complex.platform.ticketing.SettlementSettledEvent event =
                new com.complex.platform.ticketing.SettlementSettledEvent(
                        "ver_1", NOW, "ent_1", localOrderId, "tt_pass", "bar", null,
                        2, 1, 1, 8800L, NOW.plusSeconds(86400), null, null, null, null, "tkt_parent_1");
        enqueuer.onVerified(event);
        enqueuer.onVerified(event); // 幂等：同核销 id 重复事件 → upsert 同一行不翻倍。
        assertThat(repository.retryTasks).hasSize(1);
        IntegrationTaskRecord task = repository.retryTasks.values().iterator().next();
        assertThat(task.id()).isEqualTo("retry_vcb_ver_1");
        assertThat(task.taskType()).isEqualTo("verification_callback");
        assertThat(task.businessType()).isEqualTo("ticket");
        assertThat(task.externalBusinessId()).isEqualTo("ext-tk-cb");
        assertThat(task.payload()).containsEntry("channel", "meituan");

        // 冲正 → 冲正回传任务。
        enqueuer.onReversed(new com.complex.platform.ticketing.VerificationReversedEvent(
                "ver_1", "ver_rev_1", "ent_1", localOrderId, null, 1));
        assertThat(repository.retryTasks).containsKey("retry_vrb_ver_rev_1");

        // 非渠道订单：查无回流行 → 不入队。
        enqueuer.onVerified(new com.complex.platform.ticketing.SettlementSettledEvent(
                "ver_2", NOW, "ent_2", "ticket_order_local_only", "tt_pass", "bar", null,
                1, 1, 1, 100L, NOW.plusSeconds(86400), null, null, null, null, "tkt_parent_2"));
        assertThat(repository.retryTasks).hasSize(2);
    }

    /** 批6 假出票端口：记录指令、按可配金额造本地订单（对账系统取数走 orderAmounts）。 */
    private static final class FakeChannelTicketIssuePort implements ChannelTicketIssuePort {
        final List<IssueCommand> commands = new ArrayList<>();
        final Map<String, Long> orderAmounts = new LinkedHashMap<>();
        long amountCent = 8800L;
        boolean suspended;
        int refundCount;

        public Optional<RejectionResult> rejectSuspendedPayment(IssueCommand command, String sourceId,
                long amount, String originalPaymentRef, String merchantId) {
            if (!suspended) return Optional.empty();
            if (amount == 0) return Optional.of(new RejectionResult(null));
            refundCount++;
            return Optional.of(new RejectionResult("refund_" + sourceId));
        }

        @Override
        public IssueResult issue(IssueCommand command) {
            commands.add(command);
            String orderId = "ticket_order_" + command.idempotencyBase() + "_order";
            orderAmounts.put(orderId, amountCent);
            return new IssueResult(orderId, "TO-TEST-" + command.idempotencyBase(), amountCent,
                    command.quantity(), command.quantity());
        }

        @Override
        public Optional<Long> findOrderAmountCent(String ticketOrderId) {
            return Optional.ofNullable(orderAmounts.get(ticketOrderId));
        }
    }

    private IntegrationOrderSyncRequest orderSyncRequest(
            String idempotencyKey,
            String externalBusinessId,
            String channel,
            String externalChannelOrderId,
            long amountCent,
            String status
    ) {
        return new IntegrationOrderSyncRequest(
                idempotencyKey,
                "auto-test",
                List.of(),
                "mock-provider",
                "catering",
                "order",
                externalBusinessId,
                status,
                amountCent,
                "CNY",
                channel,
                externalChannelOrderId,
                null,
                Map.of());
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
                adapter("map-adapter", "mock-provider", List.of("webhook"), "production", "active",
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


    private final java.util.concurrent.atomic.AtomicInteger webhookNonceSeq = new java.util.concurrent.atomic.AtomicInteger();

    /** 完整发起一次签名 webhook（自动唯一 nonce，代表一次合法传输/重试）。 */
    private WebhookAcceptedResponse postWebhook(
            String endpointKey, String provider, String event, String idempotencyKey, Map<String, Object> payload) {
        return postWebhook(endpointKey, provider, event, idempotencyKey, payload,
                "nonce-" + webhookNonceSeq.incrementAndGet());
    }

    /** 完整发起一次签名 webhook（显式 nonce，用于原报文重放用例）。 */
    private WebhookAcceptedResponse postWebhook(
            String endpointKey, String provider, String event, String idempotencyKey,
            Map<String, Object> payload, String nonce) {
        long ts = NOW.toEpochMilli();
        String signature = webhookSignature(endpointKey, provider, event, idempotencyKey, payload, ts, nonce);
        return service.receiveWebhook(endpointKey, provider, event, signature, ts, nonce, null, payload);
    }

    private String webhookSignature(String endpointKey, String provider, String event, String idempotencyKey,
            Map<String, Object> payload, long timestamp, String nonce) {
        return hmac(OPEN_WEBHOOK_TEST_SECRET, "endpoint_key=" + endpointKey + "\nevent=" + event
                + "\nidempotency_key=" + idempotencyKey + "\nnonce=" + nonce + "\npayload_hash=" + payloadHash(payload)
                + "\nprovider=" + provider + "\ntimestamp=" + timestamp);
    }

    private String integrationWebhookSignature(String type, String provider, String event, String idempotencyKey,
            Map<String, Object> payload, long timestamp, String nonce) {
        return hmac(INTEGRATION_WEBHOOK_TEST_SECRET, "endpoint_key=" + type + "\nevent=" + event
                + "\nidempotency_key=" + idempotencyKey + "\nnonce=" + nonce + "\npayload_hash=" + payloadHash(payload)
                + "\nprovider=" + provider + "\ntimestamp=" + timestamp);
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
                "catering_pos", null, null, null, null, "idem-catering-1", Map.of("event_id", "ev1")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("TERMINAL_DISABLED");
    }

    @Test
    void t14TypedWebhookAcceptedDoesNotFakeReconciliation() {
        long ts = NOW.toEpochMilli();
        String nonce = "nonce-retail-1";
        Map<String, Object> payload = Map.of("event_id", "evt-retail-1");
        String sig = integrationWebhookSignature("retail_pos", "prov-retail", "integration.retail_pos.webhook",
                "idem-retail-1", payload, ts, nonce);
        WebhookAcceptedResponse response = service.receiveTypedWebhook(
                "retail_pos", "prov-retail", sig, ts, nonce, "idem-retail-1", payload);
        assertThat(response.eventId()).isEqualTo("evt-retail-1");
        OpenApiEventRecord stored = repository.events.stream()
                .filter(e -> "evt-retail-1".equals(e.id())).findFirst().orElseThrow();
        assertThat(stored.eventStatus()).isEqualTo("accepted");
        assertThat(stored.signatureValid()).isTrue();
        assertThat(stored.resultJson().get("mapped")).isEqualTo(false);
        assertThat(stored.resultJson().get("mapping")).isEqualTo(Map.of());
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.webhook_received".equals(e.eventName()));
    }

    @Test
    void t14TypedWebhookRejectsUnsupportedType() {
        assertThatThrownBy(() -> service.receiveTypedWebhook("unknown_provider", null, null, null, null, "idem-x", Map.of()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("THIRD_PARTY_TYPE_UNSUPPORTED");
    }

    @Test
    void t14RobotReportWritesPassiveTraceWithEventTypeCheck() {
        long ts = NOW.toEpochMilli();
        java.util.Map<String, Object> signable = new java.util.LinkedHashMap<>();
        signable.put("robot_task_ref", "robot-task-1");
        signable.put("event_type", "delivered");
        signable.put("handover_id", "handover-9");
        signable.put("payload", Map.of("floor", "3"));
        String sig = integrationWebhookSignature("robot", "prov-robot", "integration.robot.report",
                "idem-robot-1", signable, ts, "nonce-robot-1");
        RobotTaskEventRecord record = service.reportRobotTaskEvent(new RobotTaskEventRequest(
                "robot-task-1", "delivered", "handover-9", Map.of("floor", "3"), "idem-robot-1"),
                "prov-robot", sig, ts, "nonce-robot-1");
        assertThat(record.eventType()).isEqualTo("delivered");
        assertThat(record.handoverId()).isEqualTo("handover-9");
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.robot_reported".equals(e.eventName()));

        // 事件类型非法在验签前先拒（任意签名不影响）。
        assertThatThrownBy(() -> service.reportRobotTaskEvent(new RobotTaskEventRequest(
                "robot-task-2", "exploded", null, Map.of(), "idem-robot-2"), "prov-robot", "anysig", ts, "nonce-robot-2"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("ROBOT_EVENT_TYPE_INVALID");
    }

    @Test
    void t14TypedWebhookRejectsMissingSignature() {
        // 2.1：无签名 → 401（落库前拒，零写入）。
        assertThatThrownBy(() -> service.receiveTypedWebhook(
                "retail_pos", "prov", null, NOW.toEpochMilli(), "nonce-nosig", "idem-nosig", Map.of("event_id", "e")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("INTEGRATION_WEBHOOK_SIGNATURE_INVALID");
    }

    @Test
    void t14TypedWebhookRejectsReplayedNonce() {
        // 2.1：同 nonce 原报文重放 → 401（换 idempotency_key 仍拒）。
        long ts = NOW.toEpochMilli();
        Map<String, Object> payload = Map.of("event_id", "evt-replay");
        String sig = integrationWebhookSignature("retail_pos", "prov", "integration.retail_pos.webhook",
                "idem-replay-1", payload, ts, "nonce-replay");
        service.receiveTypedWebhook("retail_pos", "prov", sig, ts, "nonce-replay", "idem-replay-1", payload);
        String sig2 = integrationWebhookSignature("retail_pos", "prov", "integration.retail_pos.webhook",
                "idem-replay-2", payload, ts, "nonce-replay");
        assertThatThrownBy(() -> service.receiveTypedWebhook(
                "retail_pos", "prov", sig2, ts, "nonce-replay", "idem-replay-2", payload))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("INTEGRATION_WEBHOOK_NONCE_REPLAYED");
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
        repository.seedVideoAccessApproval(new VideoAccessApprovalRecord(
                "approval-555", "user-viewer-1", null, "approved", "supervisor-1", null, NOW, NOW));
        var page = service.listVideoAnchors("user-viewer-1", "approval-555");
        assertThat(page.items()).hasSize(1);
        assertThat(repository.videoAccessAudits)
                .anyMatch(a -> a.contains("user-viewer-1") && a.contains("approval-555"));
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.video_clip_viewed".equals(e.eventName()));
    }

    @Test
    void t14VideoAccessRequiresApprovedApprovalMatchingViewer() {
        // 2.2：approval_ref 必填→403；非 approved→403；查看人不匹配→403（全部落失败审计）。
        service.recordVideoAnchor("point-1", Instant.parse("2026-06-18T11:00:00Z"), "h1");
        assertThatThrownBy(() -> service.listVideoAnchors("viewer-x", null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code()).isEqualTo("VIDEO_ACCESS_APPROVAL_REQUIRED");
        repository.seedVideoAccessApproval(new VideoAccessApprovalRecord(
                "appr-pending", "viewer-x", null, "pending", null, null, NOW, NOW));
        assertThatThrownBy(() -> service.listVideoAnchors("viewer-x", "appr-pending"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code()).isEqualTo("VIDEO_ACCESS_NOT_APPROVED");
        repository.seedVideoAccessApproval(new VideoAccessApprovalRecord(
                "appr-other", "viewer-other", null, "approved", "sup", null, NOW, NOW));
        assertThatThrownBy(() -> service.listVideoAnchors("viewer-x", "appr-other"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code()).isEqualTo("VIDEO_ACCESS_VIEWER_MISMATCH");
        assertThat(auditEventRepository.events)
                .anyMatch(e -> "integration.video_clip_view_failed".equals(e.eventName()));
    }

    private static final class InMemoryIntegrationRepository implements IntegrationRepository {
        private final Map<String, OpenAppCredentialRecord> appCredentials = new LinkedHashMap<>();
        private final Map<String, OpenEndpointConfigRecord> endpointConfigs = new LinkedHashMap<>();
        private final List<String> nonces = new ArrayList<>();
        private final List<OpenApiEventRecord> events = new ArrayList<>();
        private final Map<String, ProviderAdapterRecord> providerAdapters = new LinkedHashMap<>();
        private final List<AdapterTestRecord> adapterTests = new ArrayList<>();
        private final List<IntegrationLedgerRecord> ledgers = new ArrayList<>();
        private final Map<String, IntegrationOrderSyncRecord> orderSyncs = new LinkedHashMap<>();
        private final Map<String, IntegrationChannelStoreMapRecord> channelStoreMaps = new LinkedHashMap<>();
        private final Map<String, IntegrationChannelTicketTypeMapRecord> channelTicketTypeMaps = new LinkedHashMap<>();
        private final java.util.Set<String> merchants = new java.util.LinkedHashSet<>();
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

        /** P0-E：第三方侧系统取数（订单额 − 退款额，钳 0），与 JdbcIntegrationRepository 同口径。 */
        @Override
        public long aggregateProviderTakingsForExternalBusiness(
                String providerCode, String businessType, String externalBusinessId) {
            long total = orderSyncs.values().stream()
                    .filter(record -> record.providerCode().equals(providerCode))
                    .filter(record -> record.businessType().equals(businessType))
                    .filter(record -> record.externalBusinessId().equals(externalBusinessId))
                    .filter(record -> "order".equals(record.syncType()) || "refund".equals(record.syncType()))
                    .filter(record -> "synced".equals(record.status()) || "difference_found".equals(record.status()))
                    .mapToLong(record -> "refund".equals(record.syncType()) ? -record.amountCent() : record.amountCent())
                    .sum();
            return Math.max(total, 0L);
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
        public IntegrationChannelStoreMapRecord saveChannelStoreMap(IntegrationChannelStoreMapRecord record) {
            channelStoreMaps.put(record.id(), record);
            return record;
        }

        @Override
        public List<IntegrationChannelStoreMapRecord> listChannelStoreMaps(boolean unrestricted, java.util.Set<String> projectIds,
                String merchantId) {
            return new ArrayList<>(channelStoreMaps.values());
        }

        @Override
        public Optional<IntegrationChannelStoreMapRecord> findChannelStoreMapById(String id) {
            return Optional.ofNullable(channelStoreMaps.get(id));
        }

        @Override
        public Optional<IntegrationChannelStoreMapRecord> findChannelStoreMapByNaturalKey(
                String providerCode, String channel, String externalStoreId) {
            return channelStoreMaps.values().stream()
                    .filter(record -> record.providerCode().equals(providerCode))
                    .filter(record -> record.channel().equals(channel))
                    .filter(record -> record.externalStoreId().equals(externalStoreId))
                    .findFirst();
        }

        @Override
        public Optional<String> findActiveChannelStoreMapMerchant(
                String providerCode, String channel, String externalStoreId) {
            return channelStoreMaps.values().stream()
                    .filter(record -> record.providerCode().equals(providerCode))
                    .filter(record -> record.channel().equals(channel))
                    .filter(record -> record.externalStoreId().equals(externalStoreId))
                    .filter(record -> "active".equals(record.status()))
                    .map(IntegrationChannelStoreMapRecord::merchantId)
                    .findFirst();
        }

        @Override
        public boolean merchantExists(String merchantId) {
            return merchantId != null && merchants.contains(merchantId);
        }

        @Override
        public IntegrationChannelTicketTypeMapRecord saveChannelTicketTypeMap(IntegrationChannelTicketTypeMapRecord record) {
            channelTicketTypeMaps.put(record.id(), record);
            return record;
        }

        @Override
        public List<IntegrationChannelTicketTypeMapRecord> listChannelTicketTypeMaps() {
            return new ArrayList<>(channelTicketTypeMaps.values());
        }

        @Override
        public Optional<IntegrationChannelTicketTypeMapRecord> findChannelTicketTypeMapById(String id) {
            return Optional.ofNullable(channelTicketTypeMaps.get(id));
        }

        @Override
        public Optional<IntegrationChannelTicketTypeMapRecord> findChannelTicketTypeMapByNaturalKey(
                String providerCode, String channel, String externalProductId) {
            return channelTicketTypeMaps.values().stream()
                    .filter(record -> record.providerCode().equals(providerCode))
                    .filter(record -> record.channel().equals(channel))
                    .filter(record -> record.externalProductId().equals(externalProductId))
                    .findFirst();
        }

        @Override
        public Optional<IntegrationChannelTicketTypeMapRecord> findActiveChannelTicketTypeMap(
                String providerCode, String channel, String externalProductId) {
            return findChannelTicketTypeMapByNaturalKey(providerCode, channel, externalProductId)
                    .filter(record -> "active".equals(record.status()));
        }

        @Override
        public Optional<IntegrationOrderSyncRecord> findTicketOrderSyncByLocalOrderId(String localTicketOrderId) {
            return orderSyncs.values().stream()
                    .filter(record -> "ticket".equals(record.businessType()))
                    .filter(record -> localTicketOrderId.equals(record.providerPayload().get("local_ticket_order_id")))
                    .findFirst();
        }

        @Override
        public List<IntegrationOrderSyncRecord> listUnattributedOrderSyncs() {
            return orderSyncs.values().stream()
                    .filter(record -> record.merchantId() == null)
                    .filter(record -> "order".equals(record.syncType()))
                    .toList();
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
        public void saveVideoAccessAudit(String id, String anchorId, String viewerUserId, String approvalRef, Instant viewedAt) {
            videoAccessAudits.add(id + ":" + anchorId + ":" + viewerUserId + ":" + approvalRef);
        }

        private final Map<String, VideoAccessApprovalRecord> videoApprovals = new LinkedHashMap<>();

        @Override
        public Optional<VideoAccessApprovalRecord> findVideoAccessApproval(String approvalRef) {
            return Optional.ofNullable(videoApprovals.get(approvalRef));
        }

        public VideoAccessApprovalRecord seedVideoAccessApproval(VideoAccessApprovalRecord record) {
            videoApprovals.put(record.id(), record);
            return record;
        }
    }
}
