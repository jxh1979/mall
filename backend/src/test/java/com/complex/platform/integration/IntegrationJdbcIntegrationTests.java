package com.complex.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class IntegrationJdbcIntegrationTests {

    private static final String OPEN_APP_SECRET_REF = "config:complex.open-platform.secrets.p0-open-app";
    private static final String OPEN_WEBHOOK_SECRET_REF = "config:complex.open-platform.secrets.p0-webhook";

    @Autowired
    private JdbcIntegrationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesIntegrationTablesAndSeedsPermissions() {
        assertThat(regclass("open_app_credentials")).isEqualTo("open_app_credentials");
        assertThat(regclass("open_app_tokens")).isEqualTo("open_app_tokens");
        assertThat(regclass("open_app_nonce_replays")).isEqualTo("open_app_nonce_replays");
        assertThat(regclass("open_endpoint_configs")).isEqualTo("open_endpoint_configs");
        assertThat(regclass("open_api_events")).isEqualTo("open_api_events");
        assertThat(regclass("integration_provider_adapters")).isEqualTo("integration_provider_adapters");
        assertThat(regclass("integration_provider_adapter_tests")).isEqualTo("integration_provider_adapter_tests");
        assertThat(regclass("integration_read_ledger")).isEqualTo("integration_read_ledger");
        assertThat(regclass("integration_order_syncs")).isEqualTo("integration_order_syncs");
        assertThat(regclass("integration_retry_tasks")).isEqualTo("integration_retry_tasks");
        assertThat(regclass("integration_compensation_tasks")).isEqualTo("integration_compensation_tasks");
        assertThat(regclass("integration_reconciliation_batches")).isEqualTo("integration_reconciliation_batches");
        assertThat(regclass("integration_reconciliation_differences")).isEqualTo("integration_reconciliation_differences");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "integration.provider_adapter.view",
                "integration.provider_adapter.create",
                "integration.provider_adapter.update",
                "integration.provider_adapter.production_ready",
                "integration.provider_adapter.test",
                "integration.order_sync.view",
                "integration.order_sync.manage",
                "integration.retry.view",
                "integration.retry.manage",
                "integration.compensation.view",
                "integration.compensation.manage",
                "integration.reconciliation.view",
                "integration.reconciliation.manage",
                "integration.reconciliation.resolve"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "integration.provider_adapter.view",
                "integration.provider_adapter.create",
                "integration.provider_adapter.update",
                "integration.provider_adapter.production_ready",
                "integration.provider_adapter.test",
                "integration.order_sync.view",
                "integration.order_sync.manage",
                "integration.retry.view",
                "integration.retry.manage",
                "integration.compensation.view",
                "integration.compensation.manage",
                "integration.reconciliation.view",
                "integration.reconciliation.manage",
                "integration.reconciliation.resolve"
        );
    }

    @Test
    void tokenTableUsesHashOnlyColumnNamesForTokenPersistence() {
        assertThat(columnNames("open_app_tokens"))
                .contains("token_hash")
                .doesNotContain("token", "access_token", "raw_token", "plain_token", "secret_value", "plain_secret");
    }

    @Test
    void seededOpenPlatformRowsPersistSecretReferencesOnly() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT app_key, secret_ref
                  FROM open_app_credentials
                 WHERE app_key = 'p0-open-app'
                """))
                .extracting(row -> tuple(row.get("app_key"), row.get("secret_ref")))
                .containsExactly(tuple("p0-open-app", OPEN_APP_SECRET_REF));

        assertThat(jdbcTemplate.queryForList("""
                SELECT endpoint_key, secret_ref
                  FROM open_endpoint_configs
                 WHERE endpoint_key = 'p0-default'
                """))
                .extracting(row -> tuple(row.get("endpoint_key"), row.get("secret_ref")))
                .containsExactly(tuple("p0-default", OPEN_WEBHOOK_SECRET_REF));

        assertThat(columnNames("open_app_credentials")).contains("secret_ref").doesNotContain("secret_value", "plain_secret");
        assertThat(columnNames("open_endpoint_configs")).contains("secret_ref").doesNotContain("secret_value", "plain_secret");
    }

    @Test
    void providerAdapterCreateUpdateAndListPersistsBoundaryFields() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        ProviderAdapterRecord adapter = new ProviderAdapterRecord(
                "adapter-jdbc",
                "jdbc-provider",
                "payment",
                "mock",
                List.of("webhook"),
                "mock",
                "mock_ready",
                true,
                false,
                Map.of("provider_status", "status"),
                Map.of("enabled", true),
                Map.of("enabled", false),
                "manual-ledger:jdbc-provider",
                false,
                "jdbc",
                List.of("att-jdbc"),
                now,
                now
        );

        repository.saveProviderAdapter(adapter);
        repository.saveProviderAdapter(new ProviderAdapterRecord(
                adapter.id(),
                adapter.providerCode(),
                adapter.adapterType(),
                adapter.mode(),
                List.of("webhook", "polling"),
                "production",
                "test_ready",
                true,
                true,
                adapter.fieldMapping(),
                Map.of("enabled", true),
                Map.of("enabled", true),
                adapter.credentialSecretRef(),
                false,
                "updated",
                adapter.evidenceAttachmentIds(),
                now,
                now.plusSeconds(60)
        ));

        assertThat(repository.listProviderAdapters())
                .extracting(
                        ProviderAdapterRecord::id,
                        ProviderAdapterRecord::providerCode,
                        ProviderAdapterRecord::environment,
                        ProviderAdapterRecord::status,
                        ProviderAdapterRecord::credentialSecretRef
                )
                .contains(tuple("adapter-jdbc", "jdbc-provider", "production", "test_ready", "manual-ledger:jdbc-provider"));
    }

    @Test
    void openEventSaveAndQueryPersistsPayloadResultAndIdempotency() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        OpenApiEventRecord event = new OpenApiEventRecord(
                "evt-jdbc",
                "p0-default",
                "mock-provider",
                "payment.paid",
                "idem-jdbc",
                true,
                "accepted",
                Map.of("event_id", "evt-jdbc"),
                Map.of("normalized_status", "paid"),
                null,
                now,
                now
        );

        repository.saveOpenEvent(event);

        assertThat(repository.findOpenEventById("p0-default", "evt-jdbc"))
                .get()
                .extracting(OpenApiEventRecord::eventStatus, OpenApiEventRecord::resultJson)
                .containsExactly("accepted", Map.of("normalized_status", "paid"));
        assertThat(repository.findOpenEventByIdempotency("p0-default", "mock-provider", "idem-jdbc"))
                .get()
                .extracting(OpenApiEventRecord::id)
                .isEqualTo("evt-jdbc");
    }

    @Test
    void p1IntegrationOperationsPersistRetryCompensationAndReconciliationRows() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveOrderSync(new IntegrationOrderSyncRecord(
                "sync_jdbc",
                "mock-provider",
                "catering",
                "order",
                "external-order-jdbc",
                "synced",
                128800L,
                "CNY",
                Map.of("provider_private_status", "PAID"),
                now,
                now
        ));
        repository.saveRetryTask(new IntegrationTaskRecord(
                "retry_jdbc",
                "retry_task",
                "mock-provider",
                "webhook_process",
                "catering",
                "external-order-jdbc",
                "open",
                0,
                3,
                now.plusSeconds(300),
                null,
                null,
                Map.of("reason", "timeout"),
                List.of(),
                now,
                now
        ));
        repository.saveCompensationTask(new IntegrationTaskRecord(
                "compensation_jdbc",
                "compensation_task",
                "mock-provider",
                "manual_adjustment",
                "catering",
                "external-order-jdbc",
                "resolved",
                0,
                0,
                null,
                "finance_1",
                "manual ledger adjusted",
                Map.of("difference", 100),
                List.of("att-resolution"),
                now,
                now
        ));
        repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "batch_jdbc",
                "mock-provider",
                "catering",
                LocalDate.parse("2026-06-14"),
                LocalDate.parse("2026-06-14"),
                "difference_found",
                10,
                9,
                1,
                100L,
                now,
                now
        ));
        repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "difference_jdbc",
                "batch_jdbc",
                "mock-provider",
                "catering",
                "external-order-jdbc",
                "amount_mismatch",
                128800L,
                128700L,
                100L,
                "open",
                "finance_1",
                null,
                Map.of("provider_private_status", "DIFF"),
                List.of(),
                now,
                now
        ));

        assertThat(repository.listOrderSyncs())
                .extracting(IntegrationOrderSyncRecord::externalBusinessId, IntegrationOrderSyncRecord::status)
                .contains(tuple("external-order-jdbc", "synced"));
        assertThat(repository.findOrderSyncByProviderAndExternalId(
                "mock-provider",
                "catering",
                "order",
                "external-order-jdbc"
        ))
                .get()
                .extracting(IntegrationOrderSyncRecord::providerPayload)
                .isEqualTo(Map.of("provider_private_status", "PAID"));
        assertThat(repository.listRetryTasks())
                .extracting(IntegrationTaskRecord::id, IntegrationTaskRecord::status)
                .contains(tuple("retry_jdbc", "open"));
        assertThat(repository.findCompensationTaskById("compensation_jdbc"))
                .get()
                .extracting(IntegrationTaskRecord::status, IntegrationTaskRecord::resolutionNote)
                .containsExactly("resolved", "manual ledger adjusted");
        assertThat(repository.listReconciliationBatches())
                .extracting(ReconciliationBatchRecord::id, ReconciliationBatchRecord::differenceCount)
                .contains(tuple("batch_jdbc", 1));
        assertThat(repository.findReconciliationDifferenceById("difference_jdbc"))
                .get()
                .extracting(ReconciliationDifferenceRecord::differenceAmountCent, ReconciliationDifferenceRecord::status)
                .containsExactly(100L, "open");
    }

    private String regclass(String tableName) {
        String qualifiedTableName = "public.".concat(tableName);
        return jdbcTemplate.queryForObject("SELECT to_regclass(?)::text", String.class, qualifiedTableName);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> columnNames(String tableName) {
        return jdbcTemplate.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = ?
                 ORDER BY ordinal_position
                """, String.class, tableName);
    }
}
