package com.complex.platform.integration;

import java.util.List;
import java.util.Optional;

public interface IntegrationRepository {

    Optional<OpenAppCredentialRecord> findOpenAppCredential(String appKey);

    boolean reserveOpenNonce(String appKey, String nonce, long timestamp);

    Optional<OpenEndpointConfigRecord> findEndpointConfig(String endpointKey);

    Optional<OpenApiEventRecord> findOpenEventByIdempotency(String endpointKey, String providerCode, String idempotencyKey);

    Optional<OpenApiEventRecord> findOpenEventById(String endpointKey, String eventId);

    OpenApiEventRecord saveOpenEvent(OpenApiEventRecord event);

    List<ProviderAdapterRecord> listProviderAdapters();

    Optional<ProviderAdapterRecord> findProviderAdapterById(String id);

    ProviderAdapterRecord saveProviderAdapter(ProviderAdapterRecord adapter);

    AdapterTestRecord saveAdapterTest(AdapterTestRecord test);

    List<IntegrationLedgerRecord> listLedger(String ledgerType);

    IntegrationOrderSyncRecord saveOrderSync(IntegrationOrderSyncRecord record);

    List<IntegrationOrderSyncRecord> listOrderSyncs();

    Optional<IntegrationOrderSyncRecord> findOrderSyncByProviderAndExternalId(
            String providerCode,
            String businessType,
            String syncType,
            String externalBusinessId
    );

    IntegrationChannelStoreMapRecord saveChannelStoreMap(IntegrationChannelStoreMapRecord record);

    List<IntegrationChannelStoreMapRecord> listChannelStoreMaps();

    Optional<IntegrationChannelStoreMapRecord> findChannelStoreMapById(String id);

    Optional<IntegrationChannelStoreMapRecord> findChannelStoreMapByNaturalKey(
            String providerCode, String channel, String externalStoreId);

    Optional<String> findActiveChannelStoreMapMerchant(
            String providerCode, String channel, String externalStoreId);

    /** 落商户解析器存在性校验（P1 资金复核）：非空商户号须指向真实 merchants 行，否则解析器置未归属。 */
    boolean merchantExists(String merchantId);

    // ── 批6 渠道票种映射（第三方商品 id → 本地票种）───────────────────────────────
    IntegrationChannelTicketTypeMapRecord saveChannelTicketTypeMap(IntegrationChannelTicketTypeMapRecord record);

    List<IntegrationChannelTicketTypeMapRecord> listChannelTicketTypeMaps();

    Optional<IntegrationChannelTicketTypeMapRecord> findChannelTicketTypeMapById(String id);

    Optional<IntegrationChannelTicketTypeMapRecord> findChannelTicketTypeMapByNaturalKey(
            String providerCode, String channel, String externalProductId);

    Optional<IntegrationChannelTicketTypeMapRecord> findActiveChannelTicketTypeMap(
            String providerCode, String channel, String externalProductId);

    /** 批6 核销回传：按本地票订单 id 反查回流行（payload.local_ticket_order_id，仅渠道单命中）。 */
    Optional<IntegrationOrderSyncRecord> findTicketOrderSyncByLocalOrderId(String localTicketOrderId);

    List<IntegrationOrderSyncRecord> listUnattributedOrderSyncs();

    IntegrationTaskRecord saveRetryTask(IntegrationTaskRecord record);

    Optional<IntegrationTaskRecord> findRetryTaskById(String id);

    List<IntegrationTaskRecord> listRetryTasks();

    IntegrationTaskRecord saveCompensationTask(IntegrationTaskRecord record);

    Optional<IntegrationTaskRecord> findCompensationTaskById(String id);

    List<IntegrationTaskRecord> listCompensationTasks();

    ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord record);

    Optional<ReconciliationBatchRecord> findReconciliationBatchById(String id);

    List<ReconciliationBatchRecord> listReconciliationBatches();

    ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord record);

    Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id);

    List<ReconciliationDifferenceRecord> listReconciliationDifferences();

    /**
     * P0-E 第三方侧系统取数：按 (provider_code, business_type, external_business_id) 聚合回流实收金额（分）。
     * 口径 = SUM(order.amount_cent) − SUM(refund.amount_cent)，负数钳 0（退款超单额无业务意义）。
     * 返回 0 表示该单第三方根本没有回流落库 → 服务层据此拒绝人工登记差异（无系统取数支撑）。
     *
     * <p>default 返回 0 以兼容既有测试双子（与 FinanceRepository.aggregatePlatformExpectedForBusinessRef 同做法）。
     */
    default long aggregateProviderTakingsForExternalBusiness(
            String providerCode, String businessType, String externalBusinessId) {
        return 0L;
    }

    // ── R5 T14 第三方接口/开关预留骨架 ───────────────────────────────────────────
    RobotTaskEventRecord saveRobotTaskEvent(RobotTaskEventRecord record);

    VideoAnchorRecord saveVideoAnchor(VideoAnchorRecord record);

    List<VideoAnchorRecord> listVideoAnchors();

    void saveVideoAccessAudit(String id, String anchorId, String viewerUserId, String approvalRef, java.time.Instant viewedAt);

    /** 2.2 视频调阅主管复核审批查找（approval_ref = id）。 */
    Optional<VideoAccessApprovalRecord> findVideoAccessApproval(String approvalRef);

}
