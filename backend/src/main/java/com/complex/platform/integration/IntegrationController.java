package com.complex.platform.integration;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integration")
public class IntegrationController {

    private static final String VIEW_PERMISSION = "integration.provider_adapter.view";
    private static final String CREATE_PERMISSION = "integration.provider_adapter.create";
    private static final String UPDATE_PERMISSION = "integration.provider_adapter.update";
    private static final String PRODUCTION_READY_PERMISSION = "integration.provider_adapter.production_ready";
    private static final String TEST_PERMISSION = "integration.provider_adapter.test";
    private static final String ORDER_SYNC_VIEW_PERMISSION = "integration.order_sync.view";
    private static final String ORDER_SYNC_MANAGE_PERMISSION = "integration.order_sync.manage";
    private static final String RETRY_VIEW_PERMISSION = "integration.retry.view";
    private static final String RETRY_MANAGE_PERMISSION = "integration.retry.manage";
    private static final String POLLING_MANAGE_PERMISSION = "integration.retry.manage";
    private static final String COMPENSATION_VIEW_PERMISSION = "integration.compensation.view";
    private static final String COMPENSATION_MANAGE_PERMISSION = "integration.compensation.manage";
    private static final String RECONCILIATION_VIEW_PERMISSION = "integration.reconciliation.view";
    private static final String RECONCILIATION_MANAGE_PERMISSION = "integration.reconciliation.manage";
    private static final String RECONCILIATION_RESOLVE_PERMISSION = "integration.reconciliation.resolve";
    private static final String VIDEO_VIEW_PERMISSION = "integration.video.view";

    private final IntegrationService service;
    private final AuthorizationService authorizationService;

    public IntegrationController(IntegrationService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/compensation-tasks")
    public ApiResponse<PageResponse<IntegrationTaskRecord>> listCompensationTasks() {
        authorizationService.requirePermission(COMPENSATION_VIEW_PERMISSION);
        return ApiResponse.ok(service.listCompensationTasks());
    }

    @PostMapping("/compensation-tasks")
    public ApiResponse<IntegrationTaskRecord> createCompensationTask(@RequestBody @jakarta.validation.Valid IntegrationTaskRequest request) {
        authorizationService.requirePermission(COMPENSATION_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createCompensationTask(request));
    }

    @PostMapping("/compensation-tasks/{id}/resolve")
    public ApiResponse<IntegrationTaskRecord> resolveCompensationTask(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid IntegrationTaskResolveRequest request
    ) {
        authorizationService.requirePermission(COMPENSATION_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveCompensationTask(id, request));
    }

    @GetMapping("/devices")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listDevices() {
        return listLedger("device", "integration.device.viewed", "integration.device.viewed_failed");
    }

    @GetMapping("/devices/events")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listDeviceEvents() {
        return listLedger("device_event", "integration.device_event.viewed", "integration.device_event.viewed_failed");
    }

    @GetMapping("/devices/offline-sync-batches")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listOfflineSyncBatches() {
        return listLedger("offline_sync_batch", "integration.offline_sync_batch.viewed", "integration.offline_sync_batch.viewed_failed");
    }

    @GetMapping("/order-syncs")
    public ApiResponse<PageResponse<IntegrationOrderSyncRecord>> listOrderSyncs() {
        authorizationService.requirePermission(ORDER_SYNC_VIEW_PERMISSION);
        return ApiResponse.ok(service.listOrderSyncs());
    }

    @PostMapping("/order-syncs")
    public ApiResponse<IntegrationOrderSyncRecord> createOrderSync(@RequestBody @jakarta.validation.Valid IntegrationOrderSyncRequest request) {
        authorizationService.requirePermission(ORDER_SYNC_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrderSync(request));
    }

    @GetMapping("/provider-adapters")
    public ApiResponse<PageResponse<ProviderAdapterRecord>> listProviderAdapters() {
        authorizationService.requirePermission(VIEW_PERMISSION);
        return ApiResponse.ok(service.listProviderAdapters());
    }

    @PostMapping("/provider-adapters")
    public ApiResponse<ProviderAdapterRecord> createProviderAdapter(@RequestBody @jakarta.validation.Valid ProviderAdapterRequest request) {
        authorizationService.requirePermission(CREATE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createProviderAdapter(request));
    }

    @PutMapping("/provider-adapters/{id}")
    public ApiResponse<ProviderAdapterRecord> updateProviderAdapter(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid ProviderAdapterUpdateRequest request
    ) {
        authorizationService.requirePermission(UPDATE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateProviderAdapter(id, request));
    }

    @PostMapping("/provider-adapters/{id}/mark-production-ready")
    public ApiResponse<ProviderAdapterRecord> markProductionReady(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission(PRODUCTION_READY_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markProductionReady(id, request));
    }

    @PostMapping("/provider-adapters/{id}/test-mock")
    public ApiResponse<AdapterTestRecord> testMock(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid ProviderAdapterTestRequest request
    ) {
        authorizationService.requirePermission(TEST_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.testProviderAdapterMock(id, request));
    }

    @PostMapping("/polling-checks")
    public ApiResponse<IntegrationPollingCheckRecord> recordPollingCheck(@RequestBody @jakarta.validation.Valid IntegrationPollingCheckRequest request) {
        authorizationService.requirePermission(POLLING_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordPollingCheck(request));
    }

    @GetMapping("/retail/skus")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listRetailSkus() {
        return listLedger("retail_sku", "integration.retail_sku.viewed", "integration.retail_sku.viewed_failed");
    }

    @GetMapping("/retry-tasks")
    public ApiResponse<PageResponse<IntegrationTaskRecord>> listRetryTasks() {
        authorizationService.requirePermission(RETRY_VIEW_PERMISSION);
        return ApiResponse.ok(service.listRetryTasks());
    }

    @PostMapping("/retry-tasks")
    public ApiResponse<IntegrationTaskRecord> createRetryTask(@RequestBody @jakarta.validation.Valid IntegrationTaskRequest request) {
        authorizationService.requirePermission(RETRY_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createRetryTask(request));
    }

    @PostMapping("/retry-tasks/{id}/run-now")
    public ApiResponse<IntegrationTaskRecord> runRetryTaskNow(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission(RETRY_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.runRetryTaskNow(id, request));
    }

    @GetMapping("/reconciliation/batches")
    public ApiResponse<PageResponse<ReconciliationBatchRecord>> listReconciliationBatches() {
        authorizationService.requirePermission(RECONCILIATION_VIEW_PERMISSION);
        return ApiResponse.ok(service.listReconciliationBatches());
    }

    @PostMapping("/reconciliation/batches")
    public ApiResponse<ReconciliationBatchRecord> createReconciliationBatch(@RequestBody @jakarta.validation.Valid ReconciliationBatchRequest request) {
        authorizationService.requirePermission(RECONCILIATION_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReconciliationBatch(request));
    }

    @GetMapping("/reconciliation/differences")
    public ApiResponse<PageResponse<ReconciliationDifferenceRecord>> listReconciliationDifferences() {
        authorizationService.requirePermission(RECONCILIATION_VIEW_PERMISSION);
        return ApiResponse.ok(service.listReconciliationDifferences());
    }

    @PostMapping("/reconciliation/differences")
    public ApiResponse<ReconciliationDifferenceRecord> createReconciliationDifference(@RequestBody @jakarta.validation.Valid ReconciliationDifferenceRequest request) {
        authorizationService.requirePermission(RECONCILIATION_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReconciliationDifference(request));
    }

    @PostMapping("/reconciliation/differences/{id}/resolve")
    public ApiResponse<ReconciliationDifferenceRecord> resolveReconciliationDifference(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid ReconciliationDifferenceResolveRequest request
    ) {
        authorizationService.requirePermission(RECONCILIATION_RESOLVE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveReconciliationDifference(id, request));
    }

    @PostMapping("/reconciliation/channel-auto")
    public ApiResponse<ChannelReconciliationResult> reconcileChannel(@RequestBody @jakarta.validation.Valid ChannelReconciliationRequest request) {
        authorizationService.requirePermission(RECONCILIATION_MANAGE_PERMISSION);
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reconcileChannel(request));
    }

    @PostMapping("/{type}/webhook")
    public ApiResponse<WebhookAcceptedResponse> receiveTypedWebhook(
            @PathVariable String type,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) @jakarta.validation.Valid java.util.Map<String, Object> payload
    ) {
        // 鉴权上下文 = webhook-signature（供应商回调）；门控在 service 内编程式调用 TerminalSwitchGuard。
        return ApiResponse.ok(service.receiveTypedWebhook(type, idempotencyKey, payload));
    }

    @PostMapping("/robot/report")
    public ApiResponse<RobotTaskEventRecord> reportRobotTaskEvent(@RequestBody @jakarta.validation.Valid RobotTaskEventRequest request) {
        // 鉴权上下文 = webhook-signature（机器人回调）；门控在 service 内编程式调用 TerminalSwitchGuard。
        return ApiResponse.ok(service.reportRobotTaskEvent(request));
    }

    @GetMapping("/video-anchors")
    public ApiResponse<PageResponse<VideoAnchorRecord>> listVideoAnchors(
            @org.springframework.web.bind.annotation.RequestParam(value = "approval_ref", required = false) String approvalRef
    ) {
        authorizationService.requirePermission(VIDEO_VIEW_PERMISSION);
        String viewerUserId = authorizationService.currentActor().id();
        return ApiResponse.ok(service.listVideoAnchors(viewerUserId, approvalRef));
    }

    @GetMapping("/robot/task-events")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listRobotTaskEvents() {
        return listLedger("robot_task_event", "integration.robot_task_event.viewed", "integration.robot_task_event.viewed_failed");
    }

    @GetMapping("/robot/tasks")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listRobotTasks() {
        return listLedger("robot_task", "integration.robot_task.viewed", "integration.robot_task.viewed_failed");
    }

    @GetMapping("/video/cameras")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listVideoCameras() {
        return listLedger("video_camera", "integration.video_camera.viewed", "integration.video_camera.viewed_failed");
    }

    @GetMapping("/voucher-verifications")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listVoucherVerifications() {
        return listLedger("voucher_verification", "integration.voucher_verification.viewed", "integration.voucher_verification.viewed_failed");
    }

    @GetMapping("/wecom/bindings")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listWecomBindings() {
        return listLedger("wecom_binding", "integration.wecom_binding.viewed", "integration.wecom_binding.viewed_failed");
    }

    @GetMapping("/wecom/conversations")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listWecomConversations() {
        return listLedger("wecom_conversation", "integration.wecom_conversation.viewed", "integration.wecom_conversation.viewed_failed");
    }

    @GetMapping("/wecom/messages")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listWecomMessages() {
        return listLedger("wecom_message", "integration.wecom_message.viewed", "integration.wecom_message.viewed_failed");
    }

    @GetMapping("/wecom/risk-hits")
    public ApiResponse<PageResponse<IntegrationLedgerRecord>> listWecomRiskHits() {
        return listLedger("wecom_risk_hit", "integration.wecom_risk_hit.viewed", "integration.wecom_risk_hit.viewed_failed");
    }

    private ApiResponse<PageResponse<IntegrationLedgerRecord>> listLedger(String ledgerType, String successEvent, String failureEvent) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        return ApiResponse.ok(service.listLedger(ledgerType, successEvent, failureEvent));
    }
}
