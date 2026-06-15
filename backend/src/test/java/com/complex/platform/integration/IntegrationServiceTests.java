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

    @BeforeEach
    void setUp() {
        repository = new InMemoryIntegrationRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        secrets = new LinkedHashMap<>();
        secrets.put(OPEN_APP_SECRET_REF, OPEN_APP_TEST_SECRET);
        secrets.put(OPEN_WEBHOOK_SECRET_REF, OPEN_WEBHOOK_TEST_SECRET);
        service = new IntegrationService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                Clock.fixed(NOW, ZoneOffset.UTC),
                secretRef -> Optional.ofNullable(secrets.get(secretRef))
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
                        Map.of("provider_status", "SUCCESS"),
                        "operator note",
                        List.of()
                )
        );

        assertThat(result.testStatus()).isEqualTo("passed");
        assertThat(repository.adapterTests)
                .extracting(AdapterTestRecord::scenario)
                .containsExactly("webhook");
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
