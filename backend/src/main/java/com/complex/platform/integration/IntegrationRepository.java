package com.complex.platform.integration;

import java.util.List;
import java.util.Optional;

public interface IntegrationRepository {

    Optional<OpenAppCredentialRecord> findOpenAppCredential(String appKey);

    boolean reserveOpenNonce(String appKey, String nonce, long timestamp);

    OpenAppTokenRecord saveOpenToken(OpenAppTokenRecord token);

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
}
