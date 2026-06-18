package com.complex.platform.ticketing;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TicketVerificationService {

    private final TicketingRepository repository;

    public TicketVerificationService(TicketingRepository repository) {
        this.repository = repository;
    }

    TicketCredentialResponse refreshCredential(String ticketId, String customerId) {
        IssuedEntitlementRecord entitlement = repository.findIssuedEntitlementById(requireText(ticketId, "ticket_id"))
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        if (customerId != null) {
            TicketOrderRecord order = repository.findTicketOrderById(entitlement.orderId())
                    .orElseThrow(() -> ApiException.notFound("TICKET_ORDER_NOT_FOUND", "ticket order not found"));
            if (!customerId.equals(order.customerId())) {
                throw ApiException.forbidden("CUSTOMER_ORDER_FORBIDDEN", "ticket does not belong to customer");
            }
        }
        Instant now = Instant.now();
        String token = "cred_" + UUID.randomUUID().toString().replace("-", "");
        TicketCredentialRecord saved = repository.saveTicketCredential(new TicketCredentialRecord(
                "credential_" + UUID.randomUUID().toString().replace("-", ""),
                entitlement.id(),
                entitlement.orderId(),
                token,
                UUID.randomUUID().toString().replace("-", ""),
                hash(token),
                "dynamic_qr",
                now,
                now.plusSeconds(300),
                "active",
                1,
                now,
                now,
                60
        ));
        return toCredentialResponse(saved);
    }

    VerificationPreviewResponse previewVerification(VerificationRequest request) {
        IssuedEntitlementRecord entitlement = entitlementByCredential(request);
        Instant now = Instant.now();
        boolean withinValidity = (entitlement.validFrom() == null || !now.isBefore(entitlement.validFrom()))
                && (entitlement.validTo() == null || !now.isAfter(entitlement.validTo()));
        boolean allowed = entitlement.remainingTimes() > 0
                && "active".equals(entitlement.status())
                && withinValidity;
        return new VerificationPreviewResponse(
                allowed,
                allowed ? "ok" : "not_allowed",
                TicketEntitlementMapper.toOfflineEntitlementResponse(entitlement)
        );
    }

    VerificationResponse createVerification(VerificationRequest request) {
        return verifyEntitlement(request, "android_terminal_online", null);
    }

    SyncTasksResponse syncTerminalTasks(TerminalSyncTasksRequest request) {
        String deviceCode = requireText(request.deviceCode(), "device_code");
        List<TerminalSyncTaskItemRequest> tasks = request.tasks() == null ? List.of() : request.tasks();
        if (tasks.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "tasks is required");
        }
        String staffId = trimToNull(request.staffId());
        List<SyncTaskResult> results = tasks.stream()
                .map(task -> syncTask(deviceCode, staffId, task))
                .toList();
        return new SyncTasksResponse(Instant.now().toString(), results);
    }

    /**
     * C1/REQ-05：人工放行（manual override）。主管对核销做人工放行决策，留痕为 status=manual_overridden 的核销记录，
     * 对齐权威状态/事件 verification.manual_overridden。放行不自动扣减权益（是否扣减为后续策略）；权限在端点层按主管权限码控制。
     */
    VerificationResponse manualOverride(VerificationRequest request, String reason) {
        TicketCredentialRecord credential = credentialByPayload(request == null ? null : request.credentialPayload()).orElse(null);
        IssuedEntitlementRecord entitlement = credential == null
                ? entitlementByCredential(request)
                : repository.findIssuedEntitlementById(credential.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        VerificationRecord verification = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                entitlement.id(),
                entitlement.entitlementCode(),
                requireText(request.credentialType(), "credential_type"),
                verificationPayload(request.credentialPayload(), credential),
                requireText(request.deviceId(), "device_id"),
                requireText(request.pointId(), "point_id"),
                "manual_overridden",
                reason == null || reason.isBlank() ? "manual override" : reason.trim(),
                "manual_override",
                null,
                Instant.now()
        ));
        return new VerificationResponse(verification.id(), verification.verificationNo(),
                verification.status(), verification.resultReason());
    }

    private VerificationResponse verifyEntitlement(VerificationRequest request, String sourceType, String clientTaskId) {
        TicketCredentialRecord credential = credentialByPayload(request == null ? null : request.credentialPayload()).orElse(null);
        IssuedEntitlementRecord entitlement = credential == null
                ? entitlementByCredential(request)
                : repository.findIssuedEntitlementById(credential.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        if (!"active".equals(entitlement.status())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_ACTIVE", "entitlement is not active");
        }
        Instant now = Instant.now();
        if (entitlement.validFrom() != null && now.isBefore(entitlement.validFrom())) {
            throw ApiException.conflict("ENTITLEMENT_NOT_YET_VALID", "entitlement is not yet valid");
        }
        if (entitlement.validTo() != null && now.isAfter(entitlement.validTo())) {
            throw ApiException.conflict("ENTITLEMENT_EXPIRED", "entitlement has expired");
        }
        IssuedEntitlementRecord updated = repository.atomicUseEntitlement(entitlement.id(), now)
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_USED_UP", "entitlement has no remaining times"));
        VerificationRecord verification = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                updated.id(),
                updated.entitlementCode(),
                requireText(request.credentialType(), "credential_type"),
                verificationPayload(request.credentialPayload(), credential),
                requireText(request.deviceId(), "device_id"),
                requireText(request.pointId(), "point_id"),
                "success",
                null,
                sourceType,
                clientTaskId,
                now
        ));
        if (credential != null) {
            repository.saveTicketCredential(new TicketCredentialRecord(
                    credential.id(),
                    credential.entitlementId(),
                    credential.orderId(),
                    credential.credentialToken(),
                    credential.nonce(),
                    credential.credentialHash(),
                    credential.credentialType(),
                    credential.issuedAt(),
                    credential.expiresAt(),
                    "consumed",
                    credential.refreshedCount(),
                    credential.createdAt(),
                    now,
                    credential.refreshIntervalSeconds()
            ));
        }
        return new VerificationResponse(verification.id(), verification.verificationNo(), verification.status(), verification.resultReason());
    }

    private SyncTaskResult syncTask(String deviceCode, String staffId, TerminalSyncTaskItemRequest task) {
        String clientTaskId = requireText(task.clientTaskId(), "client_task_id");
        return repository.findTerminalSyncTask(deviceCode, clientTaskId)
                .map(this::toSyncTaskResult)
                .orElseGet(() -> createSyncTask(deviceCode, staffId, task));
    }

    private SyncTaskResult createSyncTask(String deviceCode, String staffId, TerminalSyncTaskItemRequest task) {
        String taskType = requireText(task.taskType(), "task_type");
        String clientTaskId = requireText(task.clientTaskId(), "client_task_id");
        String occurredAt = requireText(task.occurredAt(), "occurred_at");
        Map<String, String> payload = task.payload() == null ? Map.of() : task.payload();
        String verificationId = null;
        String verificationNo = null;

        if ("offline_verification".equals(taskType)) {
            String entitlementCode = firstPayload(payload, "entitlementCode", "entitlement_code", "credentialPayload", "credential_payload");
            String pointId = firstPayloadOptional(payload, "pointId", "point_id", "gateId", "gate_id", "point");
            VerificationResponse verification = verifyEntitlement(
                    new VerificationRequest(
                            "sync-task-" + clientTaskId,
                            "qr",
                            entitlementCode,
                            deviceCode,
                            pointId == null ? deviceCode : pointId,
                            Map.of("source", "terminal_sync_task")
                    ),
                    "android_terminal_offline",
                    clientTaskId
            );
            verificationId = verification.id();
            verificationNo = verification.verificationNo();
        }

        TerminalSyncTaskRecord saved = repository.saveTerminalSyncTask(new TerminalSyncTaskRecord(
                "sync_" + UUID.randomUUID().toString().replace("-", ""),
                deviceCode,
                staffId,
                clientTaskId,
                taskType,
                "accepted",
                null,
                verificationId,
                verificationNo,
                occurredAt,
                sanitizedSyncPayload(payload),
                Instant.now()
        ));
        return toSyncTaskResult(saved);
    }

    private SyncTaskResult toSyncTaskResult(TerminalSyncTaskRecord task) {
        return new SyncTaskResult(
                task.clientTaskId(),
                task.status(),
                task.reason(),
                task.verificationId(),
                task.verificationNo(),
                null
        );
    }

    private IssuedEntitlementRecord entitlementByCredential(VerificationRequest request) {
        String entitlementCode = requireText(request == null ? null : request.credentialPayload(), "credential_payload");
        if (entitlementCode.startsWith("cred_")) {
            TicketCredentialRecord credential = credentialByPayload(entitlementCode)
                    .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found"));
            return repository.findIssuedEntitlementById(credential.entitlementId())
                    .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
        }
        return repository.findIssuedEntitlementByCode(entitlementCode)
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "entitlement not found"));
    }

    private Optional<TicketCredentialRecord> credentialByPayload(String credentialPayload) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (!payload.startsWith("cred_")) {
            return Optional.empty();
        }
        TicketCredentialRecord credential = repository.findTicketCredentialByToken(payload)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND", "credential not found"));
        if (!"active".equals(credential.status()) || credential.expiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("CREDENTIAL_EXPIRED", "credential is expired");
        }
        return Optional.of(credential);
    }

    private String verificationPayload(String credentialPayload, TicketCredentialRecord credential) {
        String payload = requireText(credentialPayload, "credential_payload");
        if (credential == null) {
            return payload;
        }
        return "sha256:" + hash(payload);
    }

    private Map<String, String> sanitizedSyncPayload(Map<String, String> payload) {
        return TerminalSyncPayloadSanitizer.sanitize(payload, this::hash);
    }

    private TicketCredentialResponse toCredentialResponse(TicketCredentialRecord credential) {
        return new TicketCredentialResponse(
                credential.id(),
                credential.entitlementId(),
                credential.orderId(),
                credential.credentialToken(),
                credential.credentialType(),
                credential.issuedAt(),
                credential.expiresAt(),
                credential.refreshedCount()
        );
    }

    private String firstPayload(Map<String, String> payload, String... keys) {
        String value = firstPayloadOptional(payload, keys);
        return requireText(value, keys[0]);
    }

    private String firstPayloadOptional(Map<String, String> payload, String... keys) {
        for (String key : keys) {
            String value = payload.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("credential hashing unavailable", exception);
        }
    }
}
