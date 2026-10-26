package com.complex.platform.integration;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.complex.platform.common.BusinessDayResolver;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.complex.platform.finance.FinanceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class IntegrationJdbcIntegrationTests {

    private static final String OPEN_APP_SECRET_REF = "config:complex.open-platform.secrets.p0-open-app";
    private static final String OPEN_WEBHOOK_SECRET_REF = "config:complex.open-platform.secrets.p0-webhook";

    @Autowired
    private JdbcIntegrationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FinanceRepository financeRepository;

    @Autowired
    private BusinessDayResolver businessDayResolver;

    /**
     * ROOT-04-R4：餐饮/零售回流后到入账（或改额、补门店映射后重跑）改写的是首次落库那一商户营业日的第三方回流收入，
     * 登记该营业日待看板重算（实施台账 ROOT-04「第三方回流后到/改额」；ENUM 第 187 行「全系统的钱账统一按「营业日」归集」）。
     * 当日回流由当日刷新覆盖不登记；渠道团购票已计入售票收入，不登记。
     */
    @Test
    void lateSyncedReflowMarksOriginalMerchantBusinessDate() {
        jdbcTemplate.update("DELETE FROM analytics_revenue_refresh_dates");
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        LocalDate original = businessDayResolver.businessDate(null, null, twoDaysAgo);
        jdbcTemplate.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, external_business_id, status,
                        amount_cent, created_at, updated_at)
                VALUES ('sync_late', 'pos-late', 'catering', 'order', 'ext-late', 'received', 5000, ?, ?)
                """, java.sql.Timestamp.from(twoDaysAgo), java.sql.Timestamp.from(twoDaysAgo));
        Instant now = Instant.now();
        repository.saveOrderSync(orderSync("sync_late_again", "catering", "ext-late", 6000L, now));
        repository.saveOrderSync(orderSync("sync_today", "retail", "ext-today", 3000L, now));
        repository.saveOrderSync(orderSync("sync_ticket", "ticket", "ext-ticket", 9000L, now.minus(3, ChronoUnit.DAYS)));

        assertThat(jdbcTemplate.queryForList("SELECT business_date FROM analytics_revenue_refresh_dates", LocalDate.class))
                .containsExactly(original);
    }

    private static IntegrationOrderSyncRecord orderSync(String id, String businessType, String externalId, long cent, Instant at) {
        return new IntegrationOrderSyncRecord(id, "pos-late", businessType, "order", externalId, "synced", cent, "CNY",
                "meituan", null, null, Map.of(), at, at);
    }

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
    void videoAccessApprovalPersistsAndRejectsInvalidStatus() {
        // 2.2 真实后端活证据：主管审批表落库+回读（approved/viewer/point）；非法 status 被 CHECK 拒（fail-closed）。
        assertThat(regclass("integration_video_access_approvals")).isEqualTo("integration_video_access_approvals");
        jdbcTemplate.update("""
                INSERT INTO integration_video_access_approvals (
                    id, viewer_user_id, point_id, status, approved_by, valid_until, created_at, updated_at
                ) VALUES ('vaa_jdbc_1', 'viewer-jdbc', 'point-jdbc', 'approved', 'supervisor-jdbc',
                          '2026-06-18T01:00:00Z', '2026-06-18T00:00:00Z', '2026-06-18T00:00:00Z')
                """);
        assertThat(repository.findVideoAccessApproval("vaa_jdbc_1"))
                .get()
                .extracting(VideoAccessApprovalRecord::status, VideoAccessApprovalRecord::viewerUserId,
                        VideoAccessApprovalRecord::pointId)
                .containsExactly("approved", "viewer-jdbc", "point-jdbc");
        assertThat(permissionCodes()).contains("integration.video.approve");
        // 非法 status 被 CHECK 拒（放最后：约束违反会中止本 @Transactional 事务，其后不可再查库）。
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbcTemplate.update("""
                                INSERT INTO integration_video_access_approvals (
                                    id, viewer_user_id, point_id, status, approved_by, valid_until, created_at, updated_at
                                ) VALUES ('vaa_jdbc_bad', 'v', NULL, 'granted', NULL, NULL,
                                          '2026-06-18T00:00:00Z', '2026-06-18T00:00:00Z')
                                """))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
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
                "pending",
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
                "testing",
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
                .contains(tuple("adapter-jdbc", "jdbc-provider", "production", "testing", "manual-ledger:jdbc-provider"));
    }

    // 第十六批第 34 条（ENUM L211「接入状态 待接入、联调中、已启用、暂停、异常、已停用、归档」）：V549 把存量状态按语义映射到七态
    // （V015 种子 mock_ready → pending 待接入），库约束只收七态。
    @Test
    void providerAdapterStatusConstraintIsEnumSevenStates() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM integration_provider_adapters WHERE id = 'adapter_p0_mock_provider'", String.class))
                .isEqualTo("pending");
        for (String status : List.of("pending", "testing", "active", "paused", "abnormal", "disabled", "archived")) {
            assertThatCode(() -> jdbcTemplate.update(
                    "UPDATE integration_provider_adapters SET status = ? WHERE id = 'adapter_p0_mock_provider'", status))
                    .doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE integration_provider_adapters SET status = 'mock_ready' WHERE id = 'adapter_p0_mock_provider'"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
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
                "meituan",
                "mt-jdbc-1",
                null,
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
                "thirdparty",
                "finance_1",
                null,
                Map.of("provider_private_status", "DIFF"),
                List.of(),
                "system",
                null,
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

    @Test
    void turnoverAttributionNetsRefundsAndExcludesUnattributedRows() {
        // P1b 抽成取数真查询：实收 = SUM(order) − SUM(refund)（口径①含税毛额 ②扣退款），
        // 仅计已归属该商户且 status='synced' 的 [from,to) 窗口行；merchant_id IS NULL（未归属）
        // 与他商户与非 synced 行皆不计——未归属只进平台级 / 未归属视图，绝不静默塞给某商户。
        Instant base = Instant.parse("2026-05-10T00:00:00Z");
        seedMerchant("mch_turn_a");
        seedMerchant("mch_turn_b");
        saveOrder("os_a_o1", "mch_turn_a", "order", "synced", 100000L, base);
        saveOrder("os_a_o2", "mch_turn_a", "order", "synced", 50000L, base.plusSeconds(3600));
        saveOrder("os_a_r1", "mch_turn_a", "refund", "synced", 30000L, base.plusSeconds(7200));
        saveOrder("os_a_pending", "mch_turn_a", "order", "received", 99999L, base);
        saveOrder("os_b_o1", "mch_turn_b", "order", "synced", 88888L, base);
        saveOrder("os_null_o1", null, "order", "synced", 77777L, base);

        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-01T00:00:00Z");
        assertThat(financeRepository.aggregateMerchantReflowTurnover("mch_turn_a", from, to)).isEqualTo(120000L);
        assertThat(financeRepository.aggregateMerchantReflowTurnover("mch_turn_a",
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"))).isEqualTo(0L);
        assertThat(repository.listUnattributedOrderSyncs())
                .extracting(IntegrationOrderSyncRecord::id).contains("os_null_o1")
                .doesNotContain("os_a_o1");
    }

    @Test
    void channelStoreMapPersistsAndResolvesActiveBindingOnly() {
        // P1a 渠道门店映射：登记 → 活跃反查命中；软删（inactive）后活跃反查落空（同 id upsert）。
        assertThat(regclass("integration_channel_store_map")).isEqualTo("integration_channel_store_map");
        assertThat(permissionCodes()).contains(
                "integration.channel_store_map.view", "integration.channel_store_map.manage");
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES ('mch_map_a', '映射商户', 'bar', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        Instant now = Instant.parse("2026-05-10T00:00:00Z");
        repository.saveChannelStoreMap(new IntegrationChannelStoreMapRecord(
                "csm_1", "mock-provider", "douyin", "store-abc", "mch_map_a", "active", now, now));
        assertThat(repository.findActiveChannelStoreMapMerchant("mock-provider", "douyin", "store-abc"))
                .contains("mch_map_a");
        assertThat(repository.findActiveChannelStoreMapMerchant("mock-provider", "douyin", "store-none")).isEmpty();
        repository.saveChannelStoreMap(new IntegrationChannelStoreMapRecord(
                "csm_1", "mock-provider", "douyin", "store-abc", "mch_map_a", "inactive", now, now));
        assertThat(repository.findActiveChannelStoreMapMerchant("mock-provider", "douyin", "store-abc")).isEmpty();
        assertThat(repository.findChannelStoreMapByNaturalKey("mock-provider", "douyin", "store-abc"))
                .get().extracting(IntegrationChannelStoreMapRecord::status).isEqualTo("inactive");
    }

    @Test
    void orderSyncMerchantIdForeignKeyRejectsUnknownMerchant() {
        // 整型 backstop（P1 资金复核）：非空 merchant_id 指向不存在商户 → FK 拒绝。落商户解析器已保证
        // 到不了这（查无此商户 → 置 NULL），此 FK 仅挡绕开解析器的直插脏值，防真实营业额报表隐形。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, "
                + "external_business_id, status, amount_cent, currency, created_at, updated_at, merchant_id) "
                + "VALUES ('ios_fk_bad', 'meituan', 'catering', 'order', 'ext_fk_bad', 'synced', 100, 'CNY', "
                + "now(), now(), 'merchant_does_not_exist')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private void seedMerchant(String id) {
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, 'bar', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, id, id);
    }

    private void saveOrder(String id, String merchantId, String syncType, String status, long amountCent, Instant at) {
        repository.saveOrderSync(new IntegrationOrderSyncRecord(
                id, "mock-provider", "catering", syncType, "ext-" + id, status, amountCent, "CNY",
                "meituan", null, merchantId, Map.of(), at, at));
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
