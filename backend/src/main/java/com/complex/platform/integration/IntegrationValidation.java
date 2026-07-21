package com.complex.platform.integration;

import com.complex.platform.common.api.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class IntegrationValidation {

    private static final Set<String> ADAPTER_TYPES = Set.of(
            "payment",
            "ticket_channel",
            "face_age",
            "wecom",
            "robot",
            "video",
            "device",
            "retail",
            "catering"
    );
    private static final Set<String> MODES = Set.of("mock", "sandbox", "production");
    private static final Set<String> STATUSES = Set.of("draft", "mock_ready", "test_ready", "production_ready", "disabled");
    private static final Set<String> TEST_SCENARIOS = Set.of("auth", "webhook", "polling", "reconcile", "field_mapping", "failure_retry");
    private static final Set<String> BUSINESS_TYPES = Set.of("catering", "retail");
    // 第三方回流渠道（third-party §14.11b/§2B）。资料未到时取占位集合，开关 off 仍允许识别但不伪造对账。
    private static final Set<String> CHANNELS = Set.of(
            "meituan", "dianping", "douyin", "koubei", "eleme", "alipay", "wechat", "internal", "unknown");
    private static final Set<String> SYNC_TYPES = Set.of("order", "refund", "voucher_verification");
    private static final Set<String> SYNC_STATUSES = Set.of("received", "synced", "failed", "difference_found");
    private static final Set<String> TASK_STATUSES = Set.of("open", "ready", "running", "failed", "resolved", "ignored");
    private static final Set<String> RECONCILIATION_STATUSES = Set.of("open", "difference_found", "matched", "resolved", "ignored");
    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i).*(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)\\s*[:=].+"),
            Pattern.compile("(?i).*(sk-live|sk_test|sk-proj|AKIA[0-9A-Z]{16}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----).*"),
            Pattern.compile("(?i).*bearer\\s+[a-z0-9._-]{20,}.*")
    );

    private IntegrationValidation() {
    }

    static void validateOpenSignatureRequest(OpenSignatureRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.appKey(), "app_key");
        if (request.timestamp() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "timestamp is required");
        }
        requireText(request.nonce(), "nonce");
        requireText(request.signature(), "signature");
    }

    static void validateCreateRequest(ProviderAdapterRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.providerCode(), "provider_code");
        requireEnum(ADAPTER_TYPES, request.adapterType(), "adapter_type");
        requireEnum(MODES, request.mode(), "mode");
    }

    static void validateUpdateRequest(ProviderAdapterUpdateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.providerCode(), "provider_code");
        if (request.capabilities() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "capabilities is required");
        }
        requireEnum(MODES, request.environment(), "environment");
        requireEnum(STATUSES, request.status(), "status");
    }

    static void validateStatusChange(StatusChangeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.reason(), "reason");
    }

    static void validateTestRequest(ProviderAdapterTestRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.providerCode(), "provider_code");
        requireEnum(TEST_SCENARIOS, request.scenario(), "scenario");
    }

    static void validateOrderSyncRequest(IntegrationOrderSyncRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        requireEnum(SYNC_TYPES, request.syncType(), "sync_type");
        requireText(request.externalBusinessId(), "external_business_id");
        requireEnum(SYNC_STATUSES, request.status(), "status");
        currency(request.currency());
        channel(request.channel());
    }

    /**
     * 回流渠道语义解析：空值归一为 unknown（资料未到时占位），非法值拒绝。
     * 与 external_channel_order_id 共同构成渠道级幂等键（V109）。
     */
    static String channel(String value) {
        String resolved = value == null || value.isBlank() ? "unknown" : value;
        if (!CHANNELS.contains(resolved)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "channel is invalid");
        }
        return resolved;
    }

    static void validateChannelStoreMapRequest(IntegrationChannelStoreMapRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        channel(request.channel());
        requireText(request.externalStoreId(), "external_store_id");
        requireText(request.merchantId(), "merchant_id");
    }

    static void validateTaskRequest(IntegrationTaskRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireText(request.taskType(), "task_type");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        requireText(request.externalBusinessId(), "external_business_id");
        taskStatus(request.status());
    }

    static void validateTaskResolveRequest(IntegrationTaskResolveRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
        taskStatus(request.status());
        requireText(request.resolutionNote(), "resolution_note");
    }

    static void validateReconciliationBatchRequest(ReconciliationBatchRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        if (request.periodStart() == null || request.periodEnd() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_start and period_end are required");
        }
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_end must be on or after period_start");
        }
    }

    static void validateReconciliationDifferenceRequest(ReconciliationDifferenceRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.batchId(), "batch_id");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        requireText(request.externalBusinessId(), "external_business_id");
        requireText(request.differenceType(), "difference_type");
    }

    static void validateChannelReconciliationRequest(ChannelReconciliationRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.providerCode(), "provider_code");
        requireEnum(BUSINESS_TYPES, request.businessType(), "business_type");
        channel(request.channel());
        if (request.periodStart() == null || request.periodEnd() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_start and period_end are required");
        }
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_end must be on or after period_start");
        }
    }

    static void validateReconciliationDifferenceResolveRequest(ReconciliationDifferenceResolveRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        requireText(request.idempotencyKey(), "idempotency_key");
        requireText(request.operatorUserId(), "operator_user_id");
        reconciliationStatus(request.status());
        requireText(request.resolutionNote(), "resolution_note");
    }

    static void requireProductionReady(ProviderAdapterRecord adapter) {
        if (!"production".equals(adapter.environment())
                || !adapter.webhookEnabled()
                || !adapter.pollingCheckEnabled()
                || adapter.capabilities().isEmpty()
                || adapter.credentialSecretRef() == null
                || adapter.credentialSecretRef().isBlank()) {
            throw ApiException.conflict("PROVIDER_ADAPTER_NOT_PRODUCTION_READY", "provider adapter is not production ready");
        }
        rejectSecretLikeCredentialRef(adapter.credentialSecretRef());
    }

    static void rejectSecretLikeCredentialRef(String credentialRef) {
        if (credentialRef == null || credentialRef.isBlank()) {
            return;
        }
        for (Pattern pattern : SECRET_PATTERNS) {
            if (pattern.matcher(credentialRef).matches()) {
                throw ApiException.badRequest("SECRET_VALUE_REJECTED", "credential_secret_ref must be a reference, not a secret value");
            }
        }
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
    }

    static String requireTextValue(String value, String fieldName) {
        requireText(value, fieldName);
        return value;
    }

    static void requireEnum(Set<String> values, String value, String fieldName) {
        requireText(value, fieldName);
        if (!values.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
    }

    static int nonNegative(Integer value, String fieldName) {
        int resolved = value == null ? 0 : value;
        if (resolved < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    static long nonNegative(Long value, String fieldName) {
        long resolved = value == null ? 0L : value;
        if (resolved < 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return resolved;
    }

    static String currency(String value) {
        String resolved = value == null || value.isBlank() ? "CNY" : value;
        if (!"CNY".equals(resolved)) {
            throw ApiException.badRequest("UNSUPPORTED_CURRENCY", "only CNY is supported");
        }
        return resolved;
    }

    static String taskStatus(String value) {
        requireEnum(TASK_STATUSES, value, "status");
        return value;
    }

    static String reconciliationStatus(String value) {
        requireEnum(RECONCILIATION_STATUSES, value, "status");
        return value;
    }

    static boolean booleanConfig(Map<String, Object> config, String fieldName) {
        Object value = config == null ? null : config.get(fieldName);
        return value instanceof Boolean enabled && enabled;
    }

    static Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : Map.copyOf(value);
    }

    static List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }
}
