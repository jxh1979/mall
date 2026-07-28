package com.complex.platform.integration;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcIntegrationRepository implements IntegrationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcIntegrationRowMapper rowMapper;

    JdbcIntegrationRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.rowMapper = new JdbcIntegrationRowMapper(this.objectMapper);
    }

    @Override
    public Optional<OpenAppCredentialRecord> findOpenAppCredential(String appKey) {
        return jdbcTemplate.query("""
                SELECT id, app_key, secret_ref, status, allowed_endpoint_keys, created_at, updated_at
                  FROM open_app_credentials
                 WHERE app_key = :app_key
                """, Map.of("app_key", appKey), rowMapper::mapOpenAppCredential).stream().findFirst();
    }

    @Override
    public boolean reserveOpenNonce(String appKey, String nonce, long timestamp) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO open_app_nonce_replays (id, app_key, nonce, timestamp_value, created_at)
                VALUES (:id, :app_key, :nonce, :timestamp_value, :created_at)
                ON CONFLICT (app_key, nonce) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", java.util.UUID.randomUUID().toString())
                .addValue("app_key", appKey)
                .addValue("nonce", nonce)
                .addValue("timestamp_value", timestamp)
                .addValue("created_at", timestampWithTimeZone(Instant.now())));
        return inserted == 1;
    }

    @Override
    public Optional<OpenEndpointConfigRecord> findEndpointConfig(String endpointKey) {
        return jdbcTemplate.query("""
                SELECT id, endpoint_key, provider_code, secret_ref, status,
                       webhook_enabled, polling_check_enabled, created_at, updated_at
                  FROM open_endpoint_configs
                 WHERE endpoint_key = :endpoint_key
                """, Map.of("endpoint_key", endpointKey), rowMapper::mapEndpointConfig).stream().findFirst();
    }

    @Override
    public Optional<OpenApiEventRecord> findOpenEventByIdempotency(String endpointKey, String providerCode, String idempotencyKey) {
        return jdbcTemplate.query("""
                SELECT id, endpoint_key, provider_code, event_name, idempotency_key,
                       signature_valid, event_status, payload_json, result_json,
                       failure_reason, received_at, processed_at
                  FROM open_api_events
                 WHERE endpoint_key = :endpoint_key
                   AND provider_code = :provider_code
                   AND idempotency_key = :idempotency_key
                """, Map.of(
                "endpoint_key", endpointKey,
                "provider_code", providerCode,
                "idempotency_key", idempotencyKey
        ), rowMapper::mapOpenApiEvent).stream().findFirst();
    }

    @Override
    public Optional<OpenApiEventRecord> findOpenEventById(String endpointKey, String eventId) {
        return jdbcTemplate.query("""
                SELECT id, endpoint_key, provider_code, event_name, idempotency_key,
                       signature_valid, event_status, payload_json, result_json,
                       failure_reason, received_at, processed_at
                  FROM open_api_events
                 WHERE endpoint_key = :endpoint_key
                   AND id = :id
                """, Map.of("endpoint_key", endpointKey, "id", eventId), rowMapper::mapOpenApiEvent).stream().findFirst();
    }

    @Override
    public OpenApiEventRecord saveOpenEvent(OpenApiEventRecord event) {
        jdbcTemplate.update("""
                INSERT INTO open_api_events (
                    id, endpoint_key, provider_code, event_name, idempotency_key,
                    signature_valid, event_status, payload_json, result_json,
                    failure_reason, received_at, processed_at
                )
                VALUES (
                    :id, :endpoint_key, :provider_code, :event_name, :idempotency_key,
                    :signature_valid, :event_status, CAST(:payload_json AS jsonb),
                    CAST(:result_json AS jsonb), :failure_reason, :received_at, :processed_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    event_status = EXCLUDED.event_status,
                    result_json = EXCLUDED.result_json,
                    failure_reason = EXCLUDED.failure_reason,
                    processed_at = EXCLUDED.processed_at
                """, openEventParameters(event));
        return event;
    }

    @Override
    public List<ProviderAdapterRecord> listProviderAdapters() {
        return jdbcTemplate.query("""
                SELECT id, provider_code, adapter_type, mode, capabilities, environment,
                       status, webhook_enabled, polling_check_enabled, field_mapping_json,
                       webhook_config_json, polling_config_json, credential_secret_ref,
                       production_ready, operator_note, evidence_attachment_ids, created_at, updated_at
                  FROM integration_provider_adapters
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, rowMapper::mapProviderAdapter);
    }

    @Override
    public Optional<ProviderAdapterRecord> findProviderAdapterById(String id) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, adapter_type, mode, capabilities, environment,
                       status, webhook_enabled, polling_check_enabled, field_mapping_json,
                       webhook_config_json, polling_config_json, credential_secret_ref,
                       production_ready, operator_note, evidence_attachment_ids, created_at, updated_at
                  FROM integration_provider_adapters
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapProviderAdapter).stream().findFirst();
    }

    @Override
    public ProviderAdapterRecord saveProviderAdapter(ProviderAdapterRecord adapter) {
        jdbcTemplate.update("""
                INSERT INTO integration_provider_adapters (
                    id, provider_code, adapter_type, mode, capabilities, environment,
                    status, webhook_enabled, polling_check_enabled, field_mapping_json,
                    webhook_config_json, polling_config_json, credential_secret_ref,
                    production_ready, operator_note, evidence_attachment_ids, created_at, updated_at
                )
                VALUES (
                    :id, :provider_code, :adapter_type, :mode, CAST(:capabilities AS jsonb), :environment,
                    :status, :webhook_enabled, :polling_check_enabled, CAST(:field_mapping_json AS jsonb),
                    CAST(:webhook_config_json AS jsonb), CAST(:polling_config_json AS jsonb), :credential_secret_ref,
                    :production_ready, :operator_note, CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    provider_code = EXCLUDED.provider_code,
                    adapter_type = EXCLUDED.adapter_type,
                    mode = EXCLUDED.mode,
                    capabilities = EXCLUDED.capabilities,
                    environment = EXCLUDED.environment,
                    status = EXCLUDED.status,
                    webhook_enabled = EXCLUDED.webhook_enabled,
                    polling_check_enabled = EXCLUDED.polling_check_enabled,
                    field_mapping_json = EXCLUDED.field_mapping_json,
                    webhook_config_json = EXCLUDED.webhook_config_json,
                    polling_config_json = EXCLUDED.polling_config_json,
                    credential_secret_ref = EXCLUDED.credential_secret_ref,
                    production_ready = EXCLUDED.production_ready,
                    operator_note = EXCLUDED.operator_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, providerAdapterParameters(adapter));
        return adapter;
    }

    @Override
    public AdapterTestRecord saveAdapterTest(AdapterTestRecord test) {
        jdbcTemplate.update("""
                INSERT INTO integration_provider_adapter_tests (
                    id, adapter_id, provider_code, scenario, mock_payload_json,
                    test_status, result_json, created_at
                )
                VALUES (
                    :id, :adapter_id, :provider_code, :scenario, CAST(:mock_payload_json AS jsonb),
                    :test_status, CAST(:result_json AS jsonb), :created_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", test.id())
                .addValue("adapter_id", test.adapterId())
                .addValue("provider_code", test.providerCode())
                .addValue("scenario", test.scenario())
                .addValue("mock_payload_json", writeJson(test.mockPayload()))
                .addValue("test_status", test.testStatus())
                .addValue("result_json", writeJson(test.result()))
                .addValue("created_at", timestampWithTimeZone(test.createdAt())));
        return test;
    }

    @Override
    public List<IntegrationLedgerRecord> listLedger(String ledgerType) {
        return jdbcTemplate.query("""
                SELECT id, ledger_type, provider_code, business_id, status,
                       payload_json, created_at, updated_at
                  FROM integration_read_ledger
                 WHERE ledger_type = :ledger_type
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, Map.of("ledger_type", ledgerType), rowMapper::mapLedger);
    }

    @Override
    public IntegrationOrderSyncRecord saveOrderSync(IntegrationOrderSyncRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_order_syncs (
                    id, provider_code, business_type, sync_type, external_business_id,
                    status, amount_cent, currency, channel, external_channel_order_id,
                    merchant_id, provider_payload_json, created_at, updated_at
                )
                VALUES (
                    :id, :provider_code, :business_type, :sync_type, :external_business_id,
                    :status, :amount_cent, :currency, :channel, :external_channel_order_id,
                    :merchant_id, CAST(:provider_payload_json AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (provider_code, business_type, sync_type, external_business_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    external_channel_order_id = EXCLUDED.external_channel_order_id,
                    merchant_id = EXCLUDED.merchant_id,
                    provider_payload_json = EXCLUDED.provider_payload_json,
                    updated_at = EXCLUDED.updated_at
                """, orderSyncParameters(record));
        return record;
    }

    @Override
    public List<IntegrationOrderSyncRecord> listOrderSyncs() {
        return jdbcTemplate.query("""
                SELECT id, provider_code, business_type, sync_type, external_business_id,
                       status, amount_cent, currency, channel, external_channel_order_id,
                       merchant_id, provider_payload_json, created_at, updated_at
                  FROM integration_order_syncs
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, rowMapper::mapOrderSync);
    }

    @Override
    public Optional<IntegrationOrderSyncRecord> findOrderSyncByProviderAndExternalId(
            String providerCode,
            String businessType,
            String syncType,
            String externalBusinessId
    ) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, business_type, sync_type, external_business_id,
                       status, amount_cent, currency, channel, external_channel_order_id,
                       merchant_id, provider_payload_json, created_at, updated_at
                  FROM integration_order_syncs
                 WHERE provider_code = :provider_code
                   AND business_type = :business_type
                   AND sync_type = :sync_type
                   AND external_business_id = :external_business_id
                """, Map.of(
                "provider_code", providerCode,
                "business_type", businessType,
                "sync_type", syncType,
                "external_business_id", externalBusinessId
        ), rowMapper::mapOrderSync).stream().findFirst();
    }

    @Override
    public IntegrationChannelStoreMapRecord saveChannelStoreMap(IntegrationChannelStoreMapRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_channel_store_map (
                    id, provider_code, channel, external_store_id, merchant_id,
                    status, created_at, updated_at
                )
                VALUES (
                    :id, :provider_code, :channel, :external_store_id, :merchant_id,
                    :status, :created_at, :updated_at
                )
                ON CONFLICT (provider_code, channel, external_store_id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("provider_code", record.providerCode())
                .addValue("channel", record.channel())
                .addValue("external_store_id", record.externalStoreId())
                .addValue("merchant_id", record.merchantId())
                .addValue("status", record.status())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    @Override
    public List<IntegrationChannelStoreMapRecord> listChannelStoreMaps() {
        return jdbcTemplate.query("""
                SELECT id, provider_code, channel, external_store_id, merchant_id,
                       status, created_at, updated_at
                  FROM integration_channel_store_map
                 ORDER BY updated_at DESC, id
                 LIMIT 200
                """, rowMapper::mapChannelStoreMap);
    }

    @Override
    public Optional<IntegrationChannelStoreMapRecord> findChannelStoreMapById(String id) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, channel, external_store_id, merchant_id,
                       status, created_at, updated_at
                  FROM integration_channel_store_map
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapChannelStoreMap).stream().findFirst();
    }

    @Override
    public Optional<IntegrationChannelStoreMapRecord> findChannelStoreMapByNaturalKey(
            String providerCode, String channel, String externalStoreId) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, channel, external_store_id, merchant_id,
                       status, created_at, updated_at
                  FROM integration_channel_store_map
                 WHERE provider_code = :provider_code
                   AND channel = :channel
                   AND external_store_id = :external_store_id
                """, Map.of(
                "provider_code", providerCode,
                "channel", channel,
                "external_store_id", externalStoreId
        ), rowMapper::mapChannelStoreMap).stream().findFirst();
    }

    @Override
    public Optional<String> findActiveChannelStoreMapMerchant(
            String providerCode, String channel, String externalStoreId) {
        return jdbcTemplate.query("""
                SELECT merchant_id
                  FROM integration_channel_store_map
                 WHERE provider_code = :provider_code
                   AND channel = :channel
                   AND external_store_id = :external_store_id
                   AND status = 'active'
                """, Map.of(
                "provider_code", providerCode,
                "channel", channel,
                "external_store_id", externalStoreId
        ), (rs, n) -> rs.getString("merchant_id")).stream().findFirst();
    }

    @Override
    public boolean merchantExists(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM merchants WHERE id = :id)",
                Map.of("id", merchantId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public List<IntegrationOrderSyncRecord> listUnattributedOrderSyncs() {
        return jdbcTemplate.query("""
                SELECT id, provider_code, business_type, sync_type, external_business_id,
                       status, amount_cent, currency, channel, external_channel_order_id,
                       merchant_id, provider_payload_json, created_at, updated_at
                  FROM integration_order_syncs
                 WHERE merchant_id IS NULL
                   AND sync_type = 'order'
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, rowMapper::mapOrderSync);
    }

    @Override
    public IntegrationTaskRecord saveRetryTask(IntegrationTaskRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_retry_tasks (
                    id, provider_code, task_type, business_type, external_business_id,
                    status, retry_count, max_retry_count, next_retry_at,
                    operator_user_id, resolution_note, payload_json, evidence_attachment_ids,
                    created_at, updated_at
                )
                VALUES (
                    :id, :provider_code, :task_type, :business_type, :external_business_id,
                    :status, :retry_count, :max_retry_count, :next_retry_at,
                    :operator_user_id, :resolution_note, CAST(:payload_json AS jsonb),
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    retry_count = EXCLUDED.retry_count,
                    next_retry_at = EXCLUDED.next_retry_at,
                    operator_user_id = EXCLUDED.operator_user_id,
                    resolution_note = EXCLUDED.resolution_note,
                    payload_json = EXCLUDED.payload_json,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, taskParameters(record));
        return record;
    }

    @Override
    public Optional<IntegrationTaskRecord> findRetryTaskById(String id) {
        return findTaskById("integration_retry_tasks", "retry_task", id);
    }

    @Override
    public List<IntegrationTaskRecord> listRetryTasks() {
        return listTasks("integration_retry_tasks", "retry_task");
    }

    @Override
    public IntegrationTaskRecord saveCompensationTask(IntegrationTaskRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_compensation_tasks (
                    id, provider_code, task_type, business_type, external_business_id,
                    status, retry_count, max_retry_count, next_retry_at,
                    operator_user_id, resolution_note, payload_json, evidence_attachment_ids,
                    created_at, updated_at
                )
                VALUES (
                    :id, :provider_code, :task_type, :business_type, :external_business_id,
                    :status, :retry_count, :max_retry_count, :next_retry_at,
                    :operator_user_id, :resolution_note, CAST(:payload_json AS jsonb),
                    CAST(:evidence_attachment_ids AS jsonb), :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    operator_user_id = EXCLUDED.operator_user_id,
                    resolution_note = EXCLUDED.resolution_note,
                    payload_json = EXCLUDED.payload_json,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                """, taskParameters(record));
        return record;
    }

    @Override
    public Optional<IntegrationTaskRecord> findCompensationTaskById(String id) {
        return findTaskById("integration_compensation_tasks", "compensation_task", id);
    }

    @Override
    public List<IntegrationTaskRecord> listCompensationTasks() {
        return listTasks("integration_compensation_tasks", "compensation_task");
    }

    @Override
    public ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_reconciliation_batches (
                    id, provider_code, business_type, period_start, period_end, status,
                    expected_count, actual_count, difference_count, total_difference_cent,
                    created_at, updated_at
                )
                VALUES (
                    :id, :provider_code, :business_type, :period_start, :period_end, :status,
                    :expected_count, :actual_count, :difference_count, :total_difference_cent,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    expected_count = EXCLUDED.expected_count,
                    actual_count = EXCLUDED.actual_count,
                    difference_count = EXCLUDED.difference_count,
                    total_difference_cent = EXCLUDED.total_difference_cent,
                    updated_at = EXCLUDED.updated_at
                """, reconciliationBatchParameters(record));
        return record;
    }

    @Override
    public Optional<ReconciliationBatchRecord> findReconciliationBatchById(String id) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, business_type, period_start, period_end, status,
                       expected_count, actual_count, difference_count, total_difference_cent,
                       created_at, updated_at
                  FROM integration_reconciliation_batches
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapReconciliationBatch).stream().findFirst();
    }

    @Override
    public List<ReconciliationBatchRecord> listReconciliationBatches() {
        return jdbcTemplate.query("""
                SELECT id, provider_code, business_type, period_start, period_end, status,
                       expected_count, actual_count, difference_count, total_difference_cent,
                       created_at, updated_at
                  FROM integration_reconciliation_batches
                 ORDER BY period_end DESC, created_at DESC, id
                 LIMIT 200
                """, rowMapper::mapReconciliationBatch);
    }

    @Override
    public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_reconciliation_differences (
                    id, batch_id, provider_code, business_type, external_business_id,
                    difference_type, internal_amount_cent, provider_amount_cent,
                    difference_amount_cent, status, source_domain, assigned_to_user_id, resolution_note,
                    provider_payload_json, evidence_attachment_ids, amount_source, registered_by_user_id,
                    created_at, updated_at
                )
                VALUES (
                    :id, :batch_id, :provider_code, :business_type, :external_business_id,
                    :difference_type, :internal_amount_cent, :provider_amount_cent,
                    :difference_amount_cent, :status, :source_domain, :assigned_to_user_id, :resolution_note,
                    CAST(:provider_payload_json AS jsonb), CAST(:evidence_attachment_ids AS jsonb),
                    :amount_source, :registered_by_user_id,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    assigned_to_user_id = EXCLUDED.assigned_to_user_id,
                    resolution_note = EXCLUDED.resolution_note,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    updated_at = EXCLUDED.updated_at
                -- P0-E：三个金额列 + amount_source + registered_by_user_id 刻意不进 DO UPDATE ——
                -- 差异一旦按系统取数落库，处置（resolve）只能改状态/处理人/结论，永远改不动金额与登记人。
                """, reconciliationDifferenceParameters(record));
        return record;
    }

    @Override
    public Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id) {
        return jdbcTemplate.query("""
                SELECT id, batch_id, provider_code, business_type, external_business_id,
                       difference_type, internal_amount_cent, provider_amount_cent,
                       difference_amount_cent, status, source_domain, assigned_to_user_id, resolution_note,
                       provider_payload_json, evidence_attachment_ids, amount_source, registered_by_user_id,
                       created_at, updated_at
                  FROM integration_reconciliation_differences
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapReconciliationDifference).stream().findFirst();
    }

    @Override
    public List<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return jdbcTemplate.query("""
                SELECT id, batch_id, provider_code, business_type, external_business_id,
                       difference_type, internal_amount_cent, provider_amount_cent,
                       difference_amount_cent, status, source_domain, assigned_to_user_id, resolution_note,
                       provider_payload_json, evidence_attachment_ids, amount_source, registered_by_user_id,
                       created_at, updated_at
                  FROM integration_reconciliation_differences
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """, rowMapper::mapReconciliationDifference);
    }

    /**
     * P0-E 第三方侧系统取数：该外部单号的回流实收 = 订单额 − 退款额（钳 0）。
     * 只认 status='synced'（已确认入账的回流）与 'difference_found'（已标差异但金额已回流）；
     * 'received'/'failed' 尚未确认，不作为取数依据。
     */
    @Override
    public long aggregateProviderTakingsForExternalBusiness(
            String providerCode, String businessType, String externalBusinessId) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT GREATEST(COALESCE(SUM(
                           CASE WHEN sync_type = 'refund' THEN -amount_cent ELSE amount_cent END), 0), 0)
                  FROM integration_order_syncs
                 WHERE provider_code = :provider_code
                   AND business_type = :business_type
                   AND external_business_id = :external_business_id
                   AND sync_type IN ('order', 'refund')
                   AND status IN ('synced', 'difference_found')
                """, new MapSqlParameterSource()
                .addValue("provider_code", providerCode)
                .addValue("business_type", businessType)
                .addValue("external_business_id", externalBusinessId), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public RobotTaskEventRecord saveRobotTaskEvent(RobotTaskEventRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_robot_task_events (
                    id, robot_task_ref, event_type, handover_id, payload, reported_at
                )
                VALUES (
                    :id, :robot_task_ref, :event_type, :handover_id, CAST(:payload AS jsonb), :reported_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("robot_task_ref", record.robotTaskRef())
                .addValue("event_type", record.eventType())
                .addValue("handover_id", record.handoverId())
                .addValue("payload", writeJson(record.payload()))
                .addValue("reported_at", timestampWithTimeZone(record.reportedAt())));
        return record;
    }

    @Override
    public VideoAnchorRecord saveVideoAnchor(VideoAnchorRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_video_anchors (
                    id, point_id, event_time, business_ref, clip_index_ref, created_at
                )
                VALUES (
                    :id, :point_id, :event_time, :business_ref, :clip_index_ref, :created_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("point_id", record.pointId())
                .addValue("event_time", timestampWithTimeZone(record.eventTime()))
                .addValue("business_ref", record.businessRef())
                .addValue("clip_index_ref", record.clipIndexRef())
                .addValue("created_at", timestampWithTimeZone(record.createdAt())));
        return record;
    }

    @Override
    public List<VideoAnchorRecord> listVideoAnchors() {
        return jdbcTemplate.query("""
                SELECT id, point_id, event_time, business_ref, clip_index_ref, created_at
                  FROM integration_video_anchors
                 ORDER BY event_time DESC, id
                 LIMIT 200
                """, rowMapper::mapVideoAnchor);
    }

    @Override
    public Optional<VideoAnchorRecord> findVideoAnchorById(String id) {
        return jdbcTemplate.query("""
                SELECT id, point_id, event_time, business_ref, clip_index_ref, created_at
                  FROM integration_video_anchors
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapVideoAnchor).stream().findFirst();
    }

    @Override
    public void saveVideoAccessAudit(String id, String anchorId, String viewerUserId, String approvalRef, Instant viewedAt) {
        jdbcTemplate.update("""
                INSERT INTO integration_video_access_audits (
                    id, anchor_id, viewer_user_id, approval_ref, viewed_at
                )
                VALUES (
                    :id, :anchor_id, :viewer_user_id, :approval_ref, :viewed_at
                )
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("anchor_id", anchorId)
                .addValue("viewer_user_id", viewerUserId)
                .addValue("approval_ref", approvalRef)
                .addValue("viewed_at", timestampWithTimeZone(viewedAt)));
    }

    @Override
    public Optional<VideoAccessApprovalRecord> findVideoAccessApproval(String approvalRef) {
        return jdbcTemplate.query("""
                SELECT id, viewer_user_id, point_id, status, approved_by, valid_until, created_at, updated_at
                  FROM integration_video_access_approvals
                 WHERE id = :id
                """, Map.of("id", approvalRef), (rs, n) -> new VideoAccessApprovalRecord(
                        rs.getString("id"), rs.getString("viewer_user_id"), rs.getString("point_id"),
                        rs.getString("status"), rs.getString("approved_by"),
                        com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "valid_until"),
                        com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "created_at"),
                        com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant(rs, "updated_at")))
                .stream().findFirst();
    }

    @Override
    public VideoAccessApprovalRecord saveVideoAccessApproval(VideoAccessApprovalRecord record) {
        jdbcTemplate.update("""
                INSERT INTO integration_video_access_approvals (
                    id, viewer_user_id, point_id, status, approved_by, valid_until, created_at, updated_at
                ) VALUES (
                    :id, :viewer_user_id, :point_id, :status, :approved_by, :valid_until, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    viewer_user_id = EXCLUDED.viewer_user_id, point_id = EXCLUDED.point_id,
                    status = EXCLUDED.status, approved_by = EXCLUDED.approved_by,
                    valid_until = EXCLUDED.valid_until, updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("viewer_user_id", record.viewerUserId())
                .addValue("point_id", record.pointId())
                .addValue("status", record.status())
                .addValue("approved_by", record.approvedBy())
                .addValue("valid_until", timestampWithTimeZone(record.validUntil()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt())));
        return record;
    }

    private Optional<IntegrationTaskRecord> findTaskById(String tableName, String taskCategory, String id) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, task_type, business_type, external_business_id,
                       status, retry_count, max_retry_count, next_retry_at,
                       operator_user_id, resolution_note, payload_json, evidence_attachment_ids,
                       created_at, updated_at
                  FROM %s
                 WHERE id = :id
                """.formatted(tableName), Map.of("id", id), (rs, row) -> rowMapper.mapTask(rs, row, taskCategory)).stream().findFirst();
    }

    private List<IntegrationTaskRecord> listTasks(String tableName, String taskCategory) {
        return jdbcTemplate.query("""
                SELECT id, provider_code, task_type, business_type, external_business_id,
                       status, retry_count, max_retry_count, next_retry_at,
                       operator_user_id, resolution_note, payload_json, evidence_attachment_ids,
                       created_at, updated_at
                  FROM %s
                 ORDER BY created_at DESC, id
                 LIMIT 200
                """.formatted(tableName), (rs, row) -> rowMapper.mapTask(rs, row, taskCategory));
    }

    private MapSqlParameterSource openEventParameters(OpenApiEventRecord event) {
        return new MapSqlParameterSource()
                .addValue("id", event.id())
                .addValue("endpoint_key", event.endpointKey())
                .addValue("provider_code", event.providerCode())
                .addValue("event_name", event.eventName())
                .addValue("idempotency_key", event.idempotencyKey())
                .addValue("signature_valid", event.signatureValid())
                .addValue("event_status", event.eventStatus())
                .addValue("payload_json", writeJson(event.payloadJson()))
                .addValue("result_json", writeJson(event.resultJson()))
                .addValue("failure_reason", event.failureReason())
                .addValue("received_at", timestampWithTimeZone(event.receivedAt()))
                .addValue("processed_at", timestampWithTimeZone(event.processedAt()));
    }

    private MapSqlParameterSource providerAdapterParameters(ProviderAdapterRecord adapter) {
        return new MapSqlParameterSource()
                .addValue("id", adapter.id())
                .addValue("provider_code", adapter.providerCode())
                .addValue("adapter_type", adapter.adapterType())
                .addValue("mode", adapter.mode())
                .addValue("capabilities", writeJson(adapter.capabilities()))
                .addValue("environment", adapter.environment())
                .addValue("status", adapter.status())
                .addValue("webhook_enabled", adapter.webhookEnabled())
                .addValue("polling_check_enabled", adapter.pollingCheckEnabled())
                .addValue("field_mapping_json", writeJson(adapter.fieldMapping()))
                .addValue("webhook_config_json", writeJson(adapter.webhookConfig()))
                .addValue("polling_config_json", writeJson(adapter.pollingConfig()))
                .addValue("credential_secret_ref", adapter.credentialSecretRef())
                .addValue("production_ready", adapter.productionReady())
                .addValue("operator_note", adapter.operatorNote())
                .addValue("evidence_attachment_ids", writeJson(adapter.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(adapter.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(adapter.updatedAt()));
    }

    private MapSqlParameterSource orderSyncParameters(IntegrationOrderSyncRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("provider_code", record.providerCode())
                .addValue("business_type", record.businessType())
                .addValue("sync_type", record.syncType())
                .addValue("external_business_id", record.externalBusinessId())
                .addValue("status", record.status())
                .addValue("amount_cent", record.amountCent())
                .addValue("currency", record.currency())
                .addValue("channel", record.channel())
                .addValue("external_channel_order_id", record.externalChannelOrderId())
                .addValue("merchant_id", record.merchantId())
                .addValue("provider_payload_json", writeJson(record.providerPayload()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource taskParameters(IntegrationTaskRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("provider_code", record.providerCode())
                .addValue("task_type", record.taskType())
                .addValue("business_type", record.businessType())
                .addValue("external_business_id", record.externalBusinessId())
                .addValue("status", record.status())
                .addValue("retry_count", record.retryCount())
                .addValue("max_retry_count", record.maxRetryCount())
                .addValue("next_retry_at", timestampWithTimeZone(record.nextRetryAt()))
                .addValue("operator_user_id", record.operatorUserId())
                .addValue("resolution_note", record.resolutionNote())
                .addValue("payload_json", writeJson(record.payload()))
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource reconciliationBatchParameters(ReconciliationBatchRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("provider_code", record.providerCode())
                .addValue("business_type", record.businessType())
                .addValue("period_start", record.periodStart())
                .addValue("period_end", record.periodEnd())
                .addValue("status", record.status())
                .addValue("expected_count", record.expectedCount())
                .addValue("actual_count", record.actualCount())
                .addValue("difference_count", record.differenceCount())
                .addValue("total_difference_cent", record.totalDifferenceCent())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private MapSqlParameterSource reconciliationDifferenceParameters(ReconciliationDifferenceRecord record) {
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("batch_id", record.batchId())
                .addValue("provider_code", record.providerCode())
                .addValue("business_type", record.businessType())
                .addValue("external_business_id", record.externalBusinessId())
                .addValue("difference_type", record.differenceType())
                .addValue("internal_amount_cent", record.internalAmountCent())
                .addValue("provider_amount_cent", record.providerAmountCent())
                .addValue("difference_amount_cent", record.differenceAmountCent())
                .addValue("status", record.status())
                .addValue("source_domain", record.sourceDomain())
                .addValue("assigned_to_user_id", record.assignedToUserId())
                .addValue("resolution_note", record.resolutionNote())
                .addValue("provider_payload_json", writeJson(record.providerPayload()))
                .addValue("evidence_attachment_ids", writeJson(record.evidenceAttachmentIds()))
                .addValue("amount_source", record.amountSource() == null ? "system" : record.amountSource())
                .addValue("registered_by_user_id", record.registeredByUserId())
                .addValue("created_at", timestampWithTimeZone(record.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(record.updatedAt()));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("json field is not serializable", exception);
        }
    }


}
