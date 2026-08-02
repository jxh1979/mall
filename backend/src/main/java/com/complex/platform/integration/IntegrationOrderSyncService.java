package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class IntegrationOrderSyncService {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(IntegrationOrderSyncService.class);

    private final IntegrationRepository repository;
    private final Clock clock;
    private final AuditedOperationExecutor executor;
    // reconcileChannel 留痕含结果派生 metadata（dry_run/difference_count），executor 无法表达，保留手写直连。
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    // 批6：ticket 回流能力开关（integration.ticket_channel.sync，V071 全局默认关）+ 本地出票端口。
    // 二者可空=未接线（旧构造/单测），ticket 回流 fail-closed 拒收，绝不静默跳过出票。
    private final com.complex.platform.gate.TerminalSwitchGuard switchGuard;
    private final ChannelTicketIssuePort ticketIssuePort;

    IntegrationOrderSyncService(IntegrationRepository repository, Clock clock, AuditedOperationExecutor executor,
                                IdempotencyService idempotencyService, AuditEventService auditEventService,
                                com.complex.platform.gate.TerminalSwitchGuard switchGuard,
                                ChannelTicketIssuePort ticketIssuePort) {
        this.repository = repository;
        this.clock = clock;
        this.executor = executor;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.switchGuard = switchGuard;
        this.ticketIssuePort = ticketIssuePort;
    }

    PageResponse<IntegrationOrderSyncRecord> listOrderSyncs() {
        return executor.readPage(AuditedOperation.of("integration.order_sync.viewed", "integration.order_sync.view_failed",
                "integration.order_sync.view", "integration_order_sync"), repository::listOrderSyncs);
    }

    IntegrationOrderSyncRecord createOrderSync(IntegrationOrderSyncRequest request) {
        return executor.executeWrite(
                "createIntegrationOrderSync",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationOrderSyncRecord.class,
                AuditedOperation.of(
                        "integration.order_sync.recorded",
                        "integration.order_sync.record_failed",
                        "integration.order_sync.manage",
                        "integration_order_sync"),
                () -> createOrderSyncOnce(request),
                saved -> ((IntegrationOrderSyncRecord) saved).id(),
                Map.of("operation_id", "createIntegrationOrderSync")
        );
    }

    // 未归属回流运营视图：列 merchant_id IS NULL 的 order 行，供人工补渠道门店映射后重跑落商户。
    PageResponse<IntegrationOrderSyncRecord> listUnattributedOrderSyncs() {
        return executor.readPage(AuditedOperation.of("integration.order_sync.unattributed_viewed",
                "integration.order_sync.unattributed_view_failed",
                "integration.order_sync.view", "integration_order_sync"), repository::listUnattributedOrderSyncs);
    }

    PageResponse<IntegrationChannelStoreMapRecord> listChannelStoreMaps() {
        return executor.readPage(AuditedOperation.of("integration.channel_store_map.viewed", "integration.channel_store_map.view_failed",
                "integration.channel_store_map.view", "integration_channel_store_map"), repository::listChannelStoreMaps);
    }

    IntegrationChannelStoreMapRecord saveChannelStoreMap(IntegrationChannelStoreMapRequest request) {
        return executor.executeWrite(
                "saveIntegrationChannelStoreMap",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationChannelStoreMapRecord.class,
                AuditedOperation.of(
                        "integration.channel_store_map.saved",
                        "integration.channel_store_map.save_failed",
                        "integration.channel_store_map.manage",
                        "integration_channel_store_map"),
                () -> saveChannelStoreMapOnce(request),
                saved -> ((IntegrationChannelStoreMapRecord) saved).id(),
                Map.of("operation_id", "saveIntegrationChannelStoreMap")
        );
    }

    IntegrationChannelStoreMapRecord archiveChannelStoreMap(String id, StatusChangeRequest request) {
        String mapId = requireTextValue(id, "id");
        String operationId = "archiveIntegrationChannelStoreMap:" + mapId;
        return executor.executeWrite(
                operationId,
                request == null ? null : request.idempotencyKey(),
                new ChannelStoreMapArchiveFingerprint(mapId, request),
                IntegrationChannelStoreMapRecord.class,
                AuditedOperation.of(
                        "integration.channel_store_map.archived",
                        "integration.channel_store_map.archive_failed",
                        "integration.channel_store_map.manage",
                        "integration_channel_store_map"),
                () -> archiveChannelStoreMapOnce(mapId, request),
                saved -> ((IntegrationChannelStoreMapRecord) saved).id(),
                Map.of("operation_id", operationId)
        );
    }

    // 落商户解析器（P1a·钱路①②上游钥匙）：
    //   源B（对账文件回流）行本带商户 → 校验 request.merchantId 存在性：命中落商户；查无此商户（拼错/过期/已删）→ 置 NULL 未归属（不静默塑给 bogus 商户）。
    //   源A（渠道 API 回流）→ 按 (provider_code, channel, payload.external_store_id) 查映射表反查活跃绑定；
    //   未命中 / 无门店号 → 留 NULL（未归属，不阻断入库，进「未归属回流」视图待人工补映射）。绝不静默塞给某商户。
    private String resolveMerchantId(IntegrationOrderSyncRequest request, Map<String, Object> payload, String channel) {
        if (request.merchantId() != null && !request.merchantId().isBlank()) {
            String claimed = request.merchantId();
            // 源B（对账文件回流）行自带商户号：先校验存在性。查无此商户（拼错 / 过期 / 已删）→ 置 NULL
            // 落未归属桶，诚实不静默——绝不把 bogus 值落库（否则店级抽成 / 租售比 / 未归属桶三处同时
            // 隐形，真实营业额报表消失），也不因商户号错就拒整条回流丢数据；留 NULL 进未归属视图待人工补映射。
            if (repository.merchantExists(claimed)) {
                return claimed;
            }
            LOGGER.warn("回流带商户号在 merchants 表查无此人，置未归属：provider={} channel={} externalBusinessId={} claimedMerchantId={}",
                    request.providerCode(), channel, request.externalBusinessId(), claimed);
            return null;
        }
        Object storeId = payload.get("external_store_id");
        if (storeId == null) {
            storeId = payload.get("store_id");
        }
        if (storeId == null || storeId.toString().isBlank()) {
            return null;
        }
        return repository.findActiveChannelStoreMapMerchant(request.providerCode(), channel, storeId.toString())
                .orElse(null);
    }

    private IntegrationChannelStoreMapRecord saveChannelStoreMapOnce(IntegrationChannelStoreMapRequest request) {
        validateChannelStoreMapRequest(request);
        Instant now = clock.instant();
        String channel = channel(request.channel());
        // 自然键 upsert：重录同一门店号复用既有 id（改绑商户 / 复活），返回值与 DB 一致。
        String id = repository.findChannelStoreMapByNaturalKey(request.providerCode(), channel, request.externalStoreId())
                .map(IntegrationChannelStoreMapRecord::id)
                .orElse("channel_store_map_" + UUID.randomUUID());
        IntegrationChannelStoreMapRecord record = new IntegrationChannelStoreMapRecord(
                id,
                request.providerCode(),
                channel,
                request.externalStoreId(),
                request.merchantId(),
                "active",
                now,
                now
        );
        return repository.saveChannelStoreMap(record);
    }

    private IntegrationChannelStoreMapRecord archiveChannelStoreMapOnce(String id, StatusChangeRequest request) {
        validateStatusChange(request);
        IntegrationChannelStoreMapRecord existing = repository.findChannelStoreMapById(id)
                .orElseThrow(() -> ApiException.notFound(
                        "INTEGRATION_CHANNEL_STORE_MAP_NOT_FOUND", "渠道门店映射不存在或已被删除"));
        IntegrationChannelStoreMapRecord updated = new IntegrationChannelStoreMapRecord(
                existing.id(),
                existing.providerCode(),
                existing.channel(),
                existing.externalStoreId(),
                existing.merchantId(),
                "inactive",
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveChannelStoreMap(updated);
    }

    private record ChannelStoreMapArchiveFingerprint(String id, StatusChangeRequest request) {
    }

    // ── 批6 渠道票种映射维护（范式照渠道门店映射）──────────────────────────────

    PageResponse<IntegrationChannelTicketTypeMapRecord> listChannelTicketTypeMaps() {
        return executor.readPage(AuditedOperation.of("integration.channel_ticket_type_map.viewed",
                "integration.channel_ticket_type_map.view_failed",
                "integration.channel_store_map.view", "integration_channel_ticket_type_map"),
                repository::listChannelTicketTypeMaps);
    }

    IntegrationChannelTicketTypeMapRecord saveChannelTicketTypeMap(IntegrationChannelTicketTypeMapRequest request) {
        return executor.executeWrite(
                "saveIntegrationChannelTicketTypeMap",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationChannelTicketTypeMapRecord.class,
                AuditedOperation.of(
                        "integration.channel_ticket_type_map.saved",
                        "integration.channel_ticket_type_map.save_failed",
                        "integration.channel_store_map.manage",
                        "integration_channel_ticket_type_map"),
                () -> saveChannelTicketTypeMapOnce(request),
                saved -> ((IntegrationChannelTicketTypeMapRecord) saved).id(),
                Map.of("operation_id", "saveIntegrationChannelTicketTypeMap")
        );
    }

    IntegrationChannelTicketTypeMapRecord archiveChannelTicketTypeMap(String id, StatusChangeRequest request) {
        String mapId = requireTextValue(id, "id");
        String operationId = "archiveIntegrationChannelTicketTypeMap:" + mapId;
        return executor.executeWrite(
                operationId,
                request == null ? null : request.idempotencyKey(),
                new ChannelTicketTypeMapArchiveFingerprint(mapId, request),
                IntegrationChannelTicketTypeMapRecord.class,
                AuditedOperation.of(
                        "integration.channel_ticket_type_map.archived",
                        "integration.channel_ticket_type_map.archive_failed",
                        "integration.channel_store_map.manage",
                        "integration_channel_ticket_type_map"),
                () -> archiveChannelTicketTypeMapOnce(mapId, request),
                saved -> ((IntegrationChannelTicketTypeMapRecord) saved).id(),
                Map.of("operation_id", operationId)
        );
    }

    private IntegrationChannelTicketTypeMapRecord saveChannelTicketTypeMapOnce(
            IntegrationChannelTicketTypeMapRequest request) {
        validateChannelTicketTypeMapRequest(request);
        Instant now = clock.instant();
        String channel = channel(request.channel());
        // 自然键 upsert：重录同一渠道商品复用既有 id（改绑票种 / 复活），返回值与 DB 一致。
        String id = repository.findChannelTicketTypeMapByNaturalKey(
                        request.providerCode(), channel, request.externalProductId())
                .map(IntegrationChannelTicketTypeMapRecord::id)
                .orElse("channel_ticket_map_" + UUID.randomUUID());
        String priceRuleId = request.priceRuleId() == null || request.priceRuleId().isBlank()
                ? null : request.priceRuleId();
        IntegrationChannelTicketTypeMapRecord record = new IntegrationChannelTicketTypeMapRecord(
                id,
                request.providerCode(),
                channel,
                request.externalProductId(),
                request.ticketTypeId(),
                priceRuleId,
                "active",
                now,
                now
        );
        return repository.saveChannelTicketTypeMap(record);
    }

    private IntegrationChannelTicketTypeMapRecord archiveChannelTicketTypeMapOnce(String id, StatusChangeRequest request) {
        validateStatusChange(request);
        IntegrationChannelTicketTypeMapRecord existing = repository.findChannelTicketTypeMapById(id)
                .orElseThrow(() -> ApiException.notFound(
                        "INTEGRATION_CHANNEL_TICKET_MAP_NOT_FOUND", "渠道票种映射不存在或已被删除"));
        IntegrationChannelTicketTypeMapRecord updated = new IntegrationChannelTicketTypeMapRecord(
                existing.id(),
                existing.providerCode(),
                existing.channel(),
                existing.externalProductId(),
                existing.ticketTypeId(),
                existing.priceRuleId(),
                "inactive",
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveChannelTicketTypeMap(updated);
    }

    private record ChannelTicketTypeMapArchiveFingerprint(String id, StatusChangeRequest request) {
    }

    PageResponse<IntegrationTaskRecord> listRetryTasks() {
        return executor.readPage(AuditedOperation.of("integration.retry_task.viewed", "integration.retry_task.view_failed",
                "integration.retry.view", "integration_retry_task"), repository::listRetryTasks);
    }

    IntegrationTaskRecord createRetryTask(IntegrationTaskRequest request) {
        return executor.executeWrite(
                "createIntegrationRetryTask",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationTaskRecord.class,
                AuditedOperation.of(
                        "integration.retry_task.created",
                        "integration.retry_task.create_failed",
                        "integration.retry.manage",
                        "integration_retry_task"),
                () -> createTaskOnce("retry_task", request),
                saved -> ((IntegrationTaskRecord) saved).id(),
                Map.of("operation_id", "createIntegrationRetryTask")
        );
    }

    IntegrationTaskRecord runRetryTaskNow(String id, StatusChangeRequest request) {
        String taskId = requireTextValue(id, "id");
        String operationId = "runIntegrationRetryTaskNow:" + taskId;
        return executor.executeWrite(
                operationId,
                request == null ? null : request.idempotencyKey(),
                new StatusChangeFingerprint(taskId, request),
                IntegrationTaskRecord.class,
                AuditedOperation.of(
                        "integration.retry_task.run_now",
                        "integration.retry_task.run_now_failed",
                        "integration.retry.manage",
                        "integration_retry_task"),
                () -> runRetryTaskNowOnce(taskId, request),
                saved -> ((IntegrationTaskRecord) saved).id(),
                Map.of("operation_id", operationId)
        );
    }

    PageResponse<IntegrationTaskRecord> listCompensationTasks() {
        return executor.readPage(AuditedOperation.of("integration.compensation_task.viewed", "integration.compensation_task.view_failed",
                "integration.compensation.view", "integration_compensation_task"), repository::listCompensationTasks);
    }

    IntegrationTaskRecord createCompensationTask(IntegrationTaskRequest request) {
        return executor.executeWrite(
                "createIntegrationCompensationTask",
                request == null ? null : request.idempotencyKey(),
                request,
                IntegrationTaskRecord.class,
                AuditedOperation.of(
                        "integration.compensation_task.created",
                        "integration.compensation_task.create_failed",
                        "integration.compensation.manage",
                        "integration_compensation_task"),
                () -> createTaskOnce("compensation_task", request),
                saved -> ((IntegrationTaskRecord) saved).id(),
                Map.of("operation_id", "createIntegrationCompensationTask")
        );
    }

    IntegrationTaskRecord resolveCompensationTask(String id, IntegrationTaskResolveRequest request) {
        String taskId = requireTextValue(id, "id");
        String operationId = "resolveIntegrationCompensationTask:" + taskId;
        return executor.executeWrite(
                operationId,
                request == null ? null : request.idempotencyKey(),
                new ResolveTaskFingerprint(taskId, request),
                IntegrationTaskRecord.class,
                AuditedOperation.of(
                        "integration.compensation_task.resolved",
                        "integration.compensation_task.resolve_failed",
                        "integration.compensation.manage",
                        "integration_compensation_task"),
                () -> resolveCompensationTaskOnce(taskId, request),
                saved -> ((IntegrationTaskRecord) saved).id(),
                Map.of("operation_id", operationId)
        );
    }

    /**
     * 自动对账（P0#11）：从 order_syncs 标准模型自动比对平台侧期望明细，生成对账批次 + 差异。
     *
     * <p>差异口径衔接 finance 侧四类（amount/count/status/missing），并在 integration 侧自产
     * duplicate（回流重复）补充；差异写既有 integration_reconciliation_differences 表，
     * source_domain=thirdparty 标记，进异常复核。开关 off（适配器 webhook 关）→ 强制 dryRun，
     * 不落库、不伪造对账数据。
     */
    ChannelReconciliationResult reconcileChannel(ChannelReconciliationRequest request) {
        try {
            ChannelReconciliationResult result = idempotencyService.execute(
                    "reconcileIntegrationChannel:" + (request == null ? "null" : request.providerCode()),
                    request == null ? null : request.idempotencyKey(),
                    request,
                    () -> reconcileChannelOnce(request),
                    ChannelReconciliationResult.class
            );
            auditEventService.recordSuccess(
                    "integration.channel_reconciliation.completed",
                    "integration.reconciliation.manage",
                    "integration_reconciliation_batch",
                    result.batchId() == null ? "dry_run" : result.batchId(),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    java.util.Map.of(
                            "dry_run", result.dryRun(),
                            "difference_count", result.differenceCount())
            );
            return result;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "integration.channel_reconciliation.failed",
                    "integration.reconciliation.manage",
                    "integration_reconciliation_batch",
                    request == null ? "unknown" : request.providerCode(),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    /** 保持本方法既有留痕口径不变：req_ 随机 request_id / 非业务异常兜底 INTERNAL_ERROR。 */
    private String requestId() {
        return "req_" + UUID.randomUUID();
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }

    private ChannelReconciliationResult reconcileChannelOnce(ChannelReconciliationRequest request) {
        validateChannelReconciliationRequest(request);
        String channel = channel(request.channel());

        // 开关解析：适配器 webhook 关 → 强制 dryRun（不伪造对账数据）。无适配器视为未接入 → 同样强制 dryRun。
        boolean switchOn = repository.listProviderAdapters().stream()
                .filter(adapter -> request.providerCode().equals(adapter.providerCode()))
                .filter(adapter -> !"disabled".equals(adapter.status()))
                .anyMatch(ProviderAdapterRecord::webhookEnabled);
        boolean requestedDryRun = request.dryRun() != null && request.dryRun();
        boolean dryRun = requestedDryRun || !switchOn;

        // 第三方侧 actual：order_syncs 标准模型，限定 provider/business/channel 范围。
        Map<String, List<IntegrationOrderSyncRecord>> actualByExternalId = new LinkedHashMap<>();
        for (IntegrationOrderSyncRecord record : repository.listOrderSyncs()) {
            if (!request.providerCode().equals(record.providerCode())
                    || !request.businessType().equals(record.businessType())
                    || !channel.equals(record.channel())) {
                continue;
            }
            actualByExternalId
                    .computeIfAbsent(record.externalBusinessId(), key -> new ArrayList<>())
                    .add(record);
        }

        List<ChannelReconciliationRequest.ExpectedEntry> expected =
                request.expectedEntries() == null ? List.of() : request.expectedEntries();
        // 批6 ticket 自动对账系统取数（P0-E 同口径）：未显式给期望明细时，平台侧期望=回流行对应的
        // 本地票订单实收（直读 ticket_orders 真源，不抄 payload 快照）。漏出票的回流行无期望 → count 差异；
        // 渠道实收 ≠ 本地订单额 → amount 差异。
        if ("ticket".equals(request.businessType()) && expected.isEmpty()) {
            expected = deriveTicketExpectedEntries(actualByExternalId);
        }
        int expectedCount = expected.size();
        int actualCount = actualByExternalId.values().stream().mapToInt(List::size).sum();

        Instant now = clock.instant();
        String batchId = "reconciliation_batch_" + UUID.randomUUID();
        List<DiffDraft> drafts = new ArrayList<>();

        for (ChannelReconciliationRequest.ExpectedEntry entry : expected) {
            String externalId = requireTextValue(entry.externalBusinessId(), "expected_entry.external_business_id");
            long expectedAmount = nonNegative(entry.amountCent(), "expected_entry.amount_cent");
            int expectedOccurrences = entry.expectedCount() == null ? 1 : Math.max(1, entry.expectedCount());
            List<IntegrationOrderSyncRecord> matches = actualByExternalId.remove(externalId);
            if (matches == null || matches.isEmpty()) {
                // missing：平台侧期望存在，第三方回流缺口。
                drafts.add(new DiffDraft(externalId, "missing", expectedAmount, 0L, entry.meta()));
                continue;
            }
            if (matches.size() > expectedOccurrences) {
                // duplicate：回流重复（integration 侧自产口径，衔接统一异常链）。
                drafts.add(new DiffDraft(externalId, "duplicate", expectedAmount,
                        matches.get(0).amountCent(), entry.meta()));
            }
            IntegrationOrderSyncRecord actual = matches.get(0);
            if (actual.amountCent() != expectedAmount) {
                // amount：金额差异（衔接 finance amount 口径）。
                drafts.add(new DiffDraft(externalId, "amount", expectedAmount, actual.amountCent(), entry.meta()));
            } else if (entry.status() != null && !entry.status().equals(actual.status())) {
                // status：状态差异（衔接 finance status 口径）。
                drafts.add(new DiffDraft(externalId, "status", expectedAmount, actual.amountCent(), entry.meta()));
            }
        }
        // 剩余 actual 未被任何期望匹配 → count（平台侧无对应取数，衔接 finance count 口径）。
        for (Map.Entry<String, List<IntegrationOrderSyncRecord>> leftover : actualByExternalId.entrySet()) {
            IntegrationOrderSyncRecord actual = leftover.getValue().get(0);
            drafts.add(new DiffDraft(leftover.getKey(), "count", 0L, actual.amountCent(), null));
            if (leftover.getValue().size() > 1) {
                drafts.add(new DiffDraft(leftover.getKey(), "duplicate", 0L, actual.amountCent(), null));
            }
        }

        long totalDifferenceCent = drafts.stream()
                .mapToLong(draft -> Math.abs(draft.internalAmountCent() - draft.providerAmountCent()))
                .sum();

        if (dryRun) {
            // 开关 off / dryRun：不落库，仅返回预览，绝不伪造对账数据。
            return new ChannelReconciliationResult(
                    null, true, expectedCount, actualCount, drafts.size(), totalDifferenceCent, List.of());
        }

        repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                batchId,
                request.providerCode(),
                request.businessType(),
                request.periodStart(),
                request.periodEnd(),
                drafts.isEmpty() ? "matched" : "difference_found",
                expectedCount,
                actualCount,
                drafts.size(),
                totalDifferenceCent,
                now,
                now
        ));
        List<ReconciliationDifferenceRecord> persisted = new ArrayList<>();
        for (DiffDraft draft : drafts) {
            ReconciliationDifferenceRecord difference = new ReconciliationDifferenceRecord(
                    "reconciliation_difference_" + UUID.randomUUID(),
                    batchId,
                    request.providerCode(),
                    request.businessType(),
                    draft.externalBusinessId(),
                    draft.differenceType(),
                    draft.internalAmountCent(),
                    draft.providerAmountCent(),
                    Math.abs(draft.internalAmountCent() - draft.providerAmountCent()),
                    "open",
                    "thirdparty", // 衔接统一异常链口径：第三方回流差异进异常复核。
                    null,
                    null,
                    autoReconcilePayload(channel, draft),
                    List.of(),
                    // P0-E：自动对账两侧金额均系统取数（平台侧=批次期望明细、第三方侧=回流），无人工登记人。
                    "system",
                    null,
                    now,
                    now
            );
            persisted.add(repository.saveReconciliationDifference(difference));
        }
        return new ChannelReconciliationResult(
                batchId, false, expectedCount, actualCount, drafts.size(), totalDifferenceCent, persisted);
    }

    /** 批6：从回流行反查本地票订单实收作平台侧期望（系统取数）。出票通道未接线 → 响亮拒绝，不伪造期望。 */
    private List<ChannelReconciliationRequest.ExpectedEntry> deriveTicketExpectedEntries(
            Map<String, List<IntegrationOrderSyncRecord>> actualByExternalId) {
        if (ticketIssuePort == null) {
            throw ApiException.conflict("INTEGRATION_TICKET_CHANNEL_DISABLED",
                    "渠道未启用：通票出票通道未接线，无法系统取数生成平台侧期望明细");
        }
        List<ChannelReconciliationRequest.ExpectedEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<IntegrationOrderSyncRecord>> group : actualByExternalId.entrySet()) {
            for (IntegrationOrderSyncRecord record : group.getValue()) {
                Object localOrderId = record.providerPayload().get("local_ticket_order_id");
                if (localOrderId == null) {
                    continue; // 未出票的回流行：无平台侧期望 → 留给 count 差异如实暴露。
                }
                Optional<Long> localAmount = ticketIssuePort.findOrderAmountCent(localOrderId.toString());
                if (localAmount.isEmpty()) {
                    continue; // 本地订单查无（脏引用）：同样留给 count 差异，不伪造期望金额。
                }
                entries.add(new ChannelReconciliationRequest.ExpectedEntry(
                        group.getKey(),
                        localAmount.get(),
                        null,
                        1,
                        Map.of("local_ticket_order_id", localOrderId.toString())
                ));
                break; // 每个外部单号一条期望（expectedCount=1，多行重复由 duplicate 口径吐差异）。
            }
        }
        return List.copyOf(entries);
    }

    private Map<String, Object> autoReconcilePayload(String channel, DiffDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", channel);
        payload.put("origin", "auto_channel_reconciliation");
        payload.put("difference_type", draft.differenceType());
        if (draft.meta() != null && !draft.meta().isEmpty()) {
            payload.put("expected_meta", Map.copyOf(draft.meta()));
        }
        return Map.copyOf(payload);
    }

    private record DiffDraft(
            String externalBusinessId,
            String differenceType,
            long internalAmountCent,
            long providerAmountCent,
            Map<String, Object> meta
    ) {
    }

    private IntegrationOrderSyncRecord createOrderSyncOnce(IntegrationOrderSyncRequest request) {
        validateOrderSyncRequest(request);
        Instant now = clock.instant();
        // 渠道语义解析（meituan/dianping/douyin/...，空值归一 unknown）。
        String channel = channel(request.channel());
        // mapPayload：把回流 payload 按渠道映射为标准订单/券核销投影，附入既有 provider_payload（不破坏原始信封）。
        Map<String, Object> providerPayload = new LinkedHashMap<>(safeMap(request.providerPayload()));
        ChannelMapping mapping = mapPayload(channel, request, providerPayload);
        if (!mapping.projection().isEmpty()) {
            providerPayload.put("standard_model", Map.copyOf(mapping.projection()));
        }
        providerPayload.put("channel_map_status", mapping.processStatus());
        Optional<IntegrationOrderSyncRecord> existing = repository.findOrderSyncByProviderAndExternalId(
                request.providerCode(),
                request.businessType(),
                request.syncType(),
                request.externalBusinessId()
        );
        String recordStatus = request.status();
        // ── 批6 ticket 分支：第三方渠道售出的通票回流 → 本地出票（走既有出票主链）────────────
        if ("ticket".equals(request.businessType())) {
            // 幂等（同渠道订单号只出一次票）：既有行已出过票 → 原样返回，不再触发出票链。
            if (existing.isPresent() && existing.get().providerPayload().containsKey("local_ticket_order_id")) {
                return existing.get();
            }
            // 能力开关闸（默认关，设计性 gating）：开关关 → 拒收，不落库不出票。
            requireTicketChannelEnabled();
            if ("order".equals(request.syncType())) {
                ChannelTicketIssuePort.IssueResult issued = issueLocalTicket(request, channel, providerPayload);
                providerPayload.put("local_ticket_order_id", issued.ticketOrderId());
                providerPayload.put("local_order_no", issued.orderNo());
                providerPayload.put("local_amount_cent", issued.amountCent());
                providerPayload.put("issued_ticket_count", issued.issuedTicketCount());
                providerPayload.put("issued_entitlement_count", issued.issuedEntitlementCount());
                // 渠道实收 vs 本地口径金额：不一致不拦（客人已在渠道付讫、票必须出），进自动对账吐 amount 差异。
                providerPayload.put("amount_matched",
                        nonNegative(request.amountCent(), "amount_cent") == issued.amountCent());
                recordStatus = "synced";
            }
            // refund / voucher_verification：首批只落台账进对账（HTML 首批四项）；本地退款必须走既有
            // 退款审批链（先审后行），绝不因渠道回流一条退款记录就自动退票退钱。
        }
        IntegrationOrderSyncRecord record = new IntegrationOrderSyncRecord(
                existing.map(IntegrationOrderSyncRecord::id).orElse("order_sync_" + UUID.randomUUID()),
                request.providerCode(),
                request.businessType(),
                request.syncType(),
                request.externalBusinessId(),
                recordStatus,
                nonNegative(request.amountCent(), "amount_cent"),
                currency(request.currency()),
                channel,
                request.externalChannelOrderId(),
                resolveMerchantId(request, providerPayload, channel),
                Map.copyOf(providerPayload),
                now,
                now
        );
        return repository.saveOrderSync(record);
    }

    // ── 批6 通票回流本地出票 ────────────────────────────────────────────────────

    /** 能力开关闸：integration.ticket_channel.sync（V071 全局默认关）。关/未接线 → 拒收（渠道未启用）。 */
    private void requireTicketChannelEnabled() {
        if (switchGuard == null || ticketIssuePort == null) {
            throw ApiException.forbidden("INTEGRATION_TICKET_CHANNEL_DISABLED",
                    "渠道未启用：第三方通票回流通道未接线，无法受理通票回流");
        }
        try {
            switchGuard.check("integration.ticket_channel.sync");
        } catch (ApiException exception) {
            // 开关闸已写 terminal.access_rejected 审计；对外统一报「渠道未启用」（明确中文口径）。
            throw ApiException.forbidden("INTEGRATION_TICKET_CHANNEL_DISABLED",
                    "渠道未启用：团购/渠道售票能力开关（integration.ticket_channel.sync）为关闭状态，"
                            + "请在「第三方开关」页开启后重试");
        }
    }

    /** 渠道商品 → 本地票种映射解析 + 走既有出票主链出票。映射缺失 fail-closed 拒收（不落半截数据）。 */
    private ChannelTicketIssuePort.IssueResult issueLocalTicket(
            IntegrationOrderSyncRequest request, String channel, Map<String, Object> payload) {
        Object externalProductId = payload.get("external_product_id");
        if (externalProductId == null || externalProductId.toString().isBlank()) {
            throw ApiException.badRequest("INTEGRATION_TICKET_PRODUCT_ID_REQUIRED",
                    "通票回流缺少渠道商品编号（payload.external_product_id），无法匹配本地票种");
        }
        IntegrationChannelTicketTypeMapRecord map = repository.findActiveChannelTicketTypeMap(
                        request.providerCode(), channel, externalProductId.toString())
                .orElseThrow(() -> ApiException.conflict("INTEGRATION_CHANNEL_TICKET_MAP_MISSING",
                        "渠道票种映射缺失：渠道 " + channel + " 商品 " + externalProductId
                                + " 未配置对应本地票种，请在「渠道映射维护」页录入后重试"));
        int quantity = ticketQuantity(payload.get("quantity"));
        java.time.LocalDate visitDate = ticketVisitDate(payload.get("visit_date"));
        Object buyerPhone = payload.get("buyer_phone");
        return ticketIssuePort.issue(new ChannelTicketIssuePort.IssueCommand(
                ticketIdempotencyBase(request, channel),
                map.ticketTypeId(),
                map.priceRuleId(),
                quantity,
                visitDate,
                channel,
                buyerPhone == null || buyerPhone.toString().isBlank() ? null : buyerPhone.toString(),
                "第三方渠道回流出票：" + channel + " " + request.externalBusinessId()
        ));
    }

    /**
     * 出票链幂等基（同渠道订单号只出一次票）：锚定 (provider, channel, externalBusinessId) 的哈希——
     * 重复回流（不同 idempotency_key）派生同一组下单/支付/出票幂等键，全链重放同一结果；
     * 哈希前置保证派生的 ticket_order id（slug 截 40 字符）不因外部单号超长而撞车。
     */
    private String ticketIdempotencyBase(IntegrationOrderSyncRequest request, String channel) {
        String natural = request.providerCode() + "|" + channel + "|" + request.externalBusinessId();
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(natural.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "tpk" + java.util.HexFormat.of().formatHex(digest, 0, 12);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private int ticketQuantity(Object raw) {
        if (raw == null) {
            return 1;
        }
        int quantity;
        try {
            quantity = raw instanceof Number number ? number.intValue() : Integer.parseInt(raw.toString());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "通票回流 payload.quantity 不是有效数字");
        }
        if (quantity < 1 || quantity > 100) {
            throw ApiException.badRequest("VALIDATION_FAILED", "通票回流 payload.quantity 必须在 1~100 之间");
        }
        return quantity;
    }

    private java.time.LocalDate ticketVisitDate(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return null; // 出票链回落当天（与后台直接下单同口径）。
        }
        try {
            return java.time.LocalDate.parse(raw.toString());
        } catch (java.time.format.DateTimeParseException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", "通票回流 payload.visit_date 不是有效日期(YYYY-MM-DD)");
        }
    }

    // ── 回流渠道 payload → 标准订单/券核销模型映射（mapPayload，复用适配器投影框架口径）─────────
    //
    // 复用 IntegrationService.mapPayload 的口径（适配器 field_mapping 作 request_mapping，
    // status_mapping 翻译供应商状态），但作用对象是 order_sync 标准模型而非 webhook 信封：
    //   · 无匹配适配器 / 适配器 webhook 开关关 / 无 field_mapping → unmapped（开关 off 不伪造，仅占位标准字段）；
    //   · 映射成功 → mapped，产出标准订单/券核销投影；
    //   · 真实供应商字段未定 → 走 mock/占位映射（fallback），projection 标 mock=true。
    //
    // 签名校验骨架：渠道回流签名待厂商资料；此处预留校验钩子，资料未到时仅记录 signature_state=skeleton，不拦截。
    private ChannelMapping mapPayload(String channel, IntegrationOrderSyncRequest request, Map<String, Object> payload) {
        Optional<ProviderAdapterRecord> adapterOpt = repository.listProviderAdapters().stream()
                .filter(adapter -> request.providerCode().equals(adapter.providerCode()))
                .filter(adapter -> !"disabled".equals(adapter.status()))
                .findFirst();
        // 占位标准模型：渠道语义 + 标准业务字段对齐（即便开关 off / 无适配器也保留，便于自动对账取数）。
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("channel", channel);
        projection.put("sync_type", request.syncType());
        projection.put("external_business_id", request.externalBusinessId());
        projection.put("external_channel_order_id", request.externalChannelOrderId());
        projection.put("amount_cent", nonNegative(request.amountCent(), "amount_cent"));
        projection.put("status", request.status());
        projection.put("signature_state", "skeleton"); // 渠道回流签名校验骨架（资料未到，不拦截）。

        if (adapterOpt.isEmpty()) {
            projection.put("mock", true);
            return ChannelMapping.unmapped(projection);
        }
        ProviderAdapterRecord adapter = adapterOpt.get();
        // 开关兜底：适配器 webhook 关 → 不做真实字段映射、不伪造对账数据，仅留占位标准字段。
        if (!adapter.webhookEnabled()) {
            projection.put("mock", true);
            return ChannelMapping.unmapped(projection);
        }
        Map<String, Object> fieldMapping = adapter.fieldMapping();
        if (fieldMapping == null || fieldMapping.isEmpty()) {
            projection.put("mock", true);
            return ChannelMapping.unmapped(projection);
        }
        try {
            Object statusMappingRaw = fieldMapping.get("status_mapping");
            for (Map.Entry<String, Object> entry : fieldMapping.entrySet()) {
                String standardField = entry.getKey();
                if ("status_mapping".equals(standardField)) {
                    continue;
                }
                if (!(entry.getValue() instanceof String sourcePath)) {
                    throw new IllegalArgumentException("invalid request_mapping entry for " + standardField);
                }
                Object mapped = resolvePath(payload, sourcePath);
                if (mapped != null) {
                    projection.put(standardField, mapped);
                }
            }
            if (statusMappingRaw instanceof Map<?, ?> statusMapping
                    && projection.get("status") instanceof String providerStatus) {
                Object platformStatus = statusMapping.get(providerStatus);
                if (platformStatus != null) {
                    projection.put("status", platformStatus);
                }
            }
            projection.put("mock", false);
            return ChannelMapping.mapped(projection);
        } catch (RuntimeException exception) {
            projection.put("mock", true);
            return ChannelMapping.unmapped(projection);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object resolvePath(Map<String, Object> payload, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        Object current = payload;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private record ChannelMapping(String processStatus, Map<String, Object> projection) {
        static ChannelMapping mapped(Map<String, Object> projection) {
            return new ChannelMapping("mapped", Map.copyOf(projection));
        }

        static ChannelMapping unmapped(Map<String, Object> projection) {
            return new ChannelMapping("unmapped", Map.copyOf(projection));
        }
    }

    private IntegrationTaskRecord createTaskOnce(String category, IntegrationTaskRequest request) {
        validateTaskRequest(request);
        Instant now = clock.instant();
        IntegrationTaskRecord record = new IntegrationTaskRecord(
                ("retry_task".equals(category) ? "retry_" : "compensation_") + UUID.randomUUID(),
                category,
                request.providerCode(),
                request.taskType(),
                request.businessType(),
                request.externalBusinessId(),
                taskStatus(request.status()),
                0,
                Math.max(0, request.maxRetryCount() == null ? 0 : request.maxRetryCount()),
                request.nextRetryAt(),
                null,
                null,
                safeMap(request.payload()),
                safeList(request.evidenceAttachmentIds()),
                now,
                now
        );
        return "retry_task".equals(category)
                ? repository.saveRetryTask(record)
                : repository.saveCompensationTask(record);
    }

    private IntegrationTaskRecord runRetryTaskNowOnce(String id, StatusChangeRequest request) {
        validateStatusChange(request);
        IntegrationTaskRecord existing = repository.findRetryTaskById(id)
                .orElseThrow(() -> ApiException.notFound("INTEGRATION_RETRY_TASK_NOT_FOUND", "集成重试任务不存在或已被删除"));
        IntegrationTaskRecord updated = new IntegrationTaskRecord(
                existing.id(),
                existing.taskCategory(),
                existing.providerCode(),
                existing.taskType(),
                existing.businessType(),
                existing.externalBusinessId(),
                "ready",
                existing.retryCount() + 1,
                existing.maxRetryCount(),
                clock.instant(),
                null,
                request.reason(),
                existing.payload(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveRetryTask(updated);
    }

    private IntegrationTaskRecord resolveCompensationTaskOnce(String id, IntegrationTaskResolveRequest request) {
        validateTaskResolveRequest(request);
        IntegrationTaskRecord existing = repository.findCompensationTaskById(id)
                .orElseThrow(() -> ApiException.notFound("INTEGRATION_COMPENSATION_TASK_NOT_FOUND", "集成补偿任务不存在或已被删除"));
        IntegrationTaskRecord updated = new IntegrationTaskRecord(
                existing.id(),
                existing.taskCategory(),
                existing.providerCode(),
                existing.taskType(),
                existing.businessType(),
                existing.externalBusinessId(),
                taskStatus(request.status()),
                existing.retryCount(),
                existing.maxRetryCount(),
                existing.nextRetryAt(),
                request.operatorUserId(),
                request.resolutionNote(),
                existing.payload(),
                safeList(request.evidenceAttachmentIds()),
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveCompensationTask(updated);
    }

    private record StatusChangeFingerprint(String id, StatusChangeRequest request) {
    }

    private record ResolveTaskFingerprint(String id, IntegrationTaskResolveRequest request) {
    }
}