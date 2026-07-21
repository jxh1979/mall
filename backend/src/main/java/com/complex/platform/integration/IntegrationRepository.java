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

    // ── R5 T14 第三方接口/开关预留骨架 ───────────────────────────────────────────
    RobotTaskEventRecord saveRobotTaskEvent(RobotTaskEventRecord record);

    VideoAnchorRecord saveVideoAnchor(VideoAnchorRecord record);

    List<VideoAnchorRecord> listVideoAnchors();

    Optional<VideoAnchorRecord> findVideoAnchorById(String id);

    void saveVideoAccessAudit(String id, String anchorId, String viewerUserId, String approvalRef, java.time.Instant viewedAt);

    /** 2.2 视频调阅主管复核审批查找（approval_ref = id）。 */
    Optional<VideoAccessApprovalRecord> findVideoAccessApproval(String approvalRef);

    VideoAccessApprovalRecord saveVideoAccessApproval(VideoAccessApprovalRecord record);
}
