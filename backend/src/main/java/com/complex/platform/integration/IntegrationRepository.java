package com.complex.platform.integration;

// 文件版本：V0.02

import java.util.List;
import java.util.Optional;

public interface IntegrationRepository {

    Optional<OpenAppCredentialRecord> findOpenAppCredential(String appKey);

    boolean reserveOpenNonce(String appKey, String nonce, long timestamp);

    Optional<OpenEndpointConfigRecord> findEndpointConfig(String endpointKey);

    Optional<OpenApiEventRecord> findOpenEventByIdempotency(String endpointKey, String providerCode, String idempotencyKey);

    Optional<OpenApiEventRecord> findOpenEventById(String endpointKey, String eventId);

    /** 第十八批 BN 段 261-B23-R1：按 (endpoint_key, provider_code, idempotency_key) 只写一次；true＝本次写入，false＝同一幂等键已有事件、未写。 */
    boolean saveOpenEvent(OpenApiEventRecord event);

    /**
     * 第十八批 BN 段 261-B23-R1（PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；
     * PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」）：落回调信封；同一幂等键已有事件（重投或同时投递的后到者）时不写，
     * 返回首次事件编号并标已去重。
     */
    default WebhookAcceptedResponse acceptOpenEvent(OpenApiEventRecord event) {
        if (saveOpenEvent(event)) {
            return new WebhookAcceptedResponse(event.id(), false);
        }
        return new WebhookAcceptedResponse(findOpenEventByIdempotency(event.endpointKey(), event.providerCode(), event.idempotencyKey())
                .orElseThrow().id(), true);
    }

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

    /**
     * 第 5 条（PGM:659）：渠道门店映射列表按读取人范围取数——merchantId 非空只取该商户的映射；否则 unrestricted（平台管理员）取全部，
     * 其余只取所属商户项目在 projectIds 内的映射（商户无项目的不给）。
     */
    List<IntegrationChannelStoreMapRecord> listChannelStoreMaps(boolean unrestricted, java.util.Set<String> projectIds, String merchantId);

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
    /** 第十八批 BN 段 261-B23-R2：按事件号只写一次；true＝本次写入，false＝同一事件号已有、未写。 */
    boolean saveRobotTaskEvent(RobotTaskEventRecord record);

    Optional<RobotTaskEventRecord> findRobotTaskEventById(String id);

    VideoAnchorRecord saveVideoAnchor(VideoAnchorRecord record);

    List<VideoAnchorRecord> listVideoAnchors();

    void saveVideoAccessAudit(String id, String anchorId, String viewerUserId, String approvalRef, java.time.Instant viewedAt);

    /** 2.2 视频调阅主管复核审批查找（approval_ref = id）。 */
    Optional<VideoAccessApprovalRecord> findVideoAccessApproval(String approvalRef);

}
