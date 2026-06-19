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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TicketVerificationService {

    // R5 T15：赠饮权益类型——核销成功后触发 ticket.gift_verified，由 bar 域生成 0 元单并扣库存。
    private static final String GIFT_DRINK_ENTITLEMENT_TYPE = "gift_drink";

    private final TicketingRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public TicketVerificationService(TicketingRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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
        // T4：刷新窗口与 refresh_interval_seconds（票种级 {30,60}）联动，替换硬编码 300s；
        // window_started_at=now，截图离线展示的旧 token 超过本窗口即过期被拒。signature_value/version 为 P1 预留，本期为 null。
        int refreshIntervalSeconds = 60;
        TicketCredentialRecord saved = repository.saveTicketCredential(new TicketCredentialRecord(
                "credential_" + UUID.randomUUID().toString().replace("-", ""),
                entitlement.id(),
                entitlement.orderId(),
                token,
                UUID.randomUUID().toString().replace("-", ""),
                hash(token),
                "dynamic_qr",
                now,
                now.plusSeconds(refreshIntervalSeconds),
                "active",
                1,
                now,
                now,
                refreshIntervalSeconds,
                now,
                null,
                null
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
                Instant.now(),
                credential == null ? null : credential.nonce(),
                null,
                entitlement.merchantId(),
                null,
                null,
                entitlement.numPeople(),
                null,
                null,
                null
        ));
        return new VerificationResponse(verification.id(), verification.verificationNo(),
                verification.status(), verification.resultReason());
    }

    /**
     * R5 T5 核销冲正：对 status=success 的核销记录做冲正——回滚 issued_entitlements.used_times、
     * 原记录置 reversed/reversed_by/reversed_at、新建一条 status=reversed 的核销记录 reverses_verification_id=原id。
     * 不存在/非 success/已冲正 → 拒绝（幂等/重复冲正拒绝）。reason 必填、权限 ticket.verify.reverse、幂等键在端点层。
     */
    VerificationResponse reverseVerification(String verificationId, String reason, String reversedBy) {
        String id = requireText(verificationId, "verification_id");
        String reverseReason = requireText(reason, "reason");
        VerificationRecord original = repository.findVerificationById(id)
                .orElseThrow(() -> ApiException.notFound("VERIFICATION_NOT_FOUND", "verification not found"));
        if (!"success".equals(original.status())) {
            throw ApiException.conflict("VERIFICATION_NOT_REVERSIBLE", "only successful verification can be reversed");
        }
        if (original.reversedAt() != null) {
            throw ApiException.conflict("VERIFICATION_ALREADY_REVERSED", "verification has already been reversed");
        }
        Instant now = Instant.now();
        // 回滚权益核销次数（used_times-1，曾用满恢复 active）。原记录此前核销失败则无可回滚 → 拒绝。
        repository.atomicReverseEntitlement(original.entitlementId(), now)
                .orElseThrow(() -> ApiException.conflict("ENTITLEMENT_REVERSE_FAILED", "no used times to reverse for entitlement"));
        // 原 success 记录置 reversed/reversed_by/reversed_at。
        repository.markVerificationReversed(original.id(), reversedBy, now);
        // 新建一条 status=reversed 的核销记录，reverses_verification_id 指向原记录，可追溯。
        VerificationRecord reversal = repository.saveVerification(new VerificationRecord(
                "ver_" + UUID.randomUUID().toString().replace("-", ""),
                "V" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT),
                original.entitlementId(),
                original.entitlementCode(),
                original.credentialType(),
                original.credentialPayload(),
                original.deviceId(),
                original.pointId(),
                "reversed",
                reverseReason,
                "verification_reverse",
                null,
                now,
                original.nonce(),
                original.staffId(),
                original.merchantId(),
                null,
                null,
                original.verifiedPeople(),
                original.id(),
                reversedBy,
                now
        ));
        // R5 T15 赠饮库存回补联动：若原核销系赠饮权益，发 bar.gift_reversed → bar 域将赠饮单置 reversed 并回补已扣库存。
        // 以原核销 verification_id 关联 bar_gift_redemptions（唯一键），未触发过赠饮兑换则消费者幂等跳过。
        boolean giftDrink = repository.findIssuedEntitlementById(original.entitlementId())
                .map(entitlement -> GIFT_DRINK_ENTITLEMENT_TYPE.equals(entitlement.entitlementType()))
                .orElse(false);
        if (giftDrink) {
            eventPublisher.publishEvent(new GiftReversedEvent(original.id()));
        }
        // TODO R5 T7 结算冲正联动本期未落地，待对应任务实现后在此触发对应冲正。
        return new VerificationResponse(reversal.id(), reversal.verificationNo(), reversal.status(), reversal.resultReason());
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
        // R5 T5 多人票：本次核销人数 = 票面 num_people（同场次集中核销），不可超 num_people。
        int verifiedPeople = updated.numPeople();
        if (verifiedPeople > updated.numPeople()) {
            throw ApiException.conflict("VERIFIED_PEOPLE_EXCEEDED", "verified people exceeds ticket num_people");
        }
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
                now,
                credential == null ? null : credential.nonce(),
                null,
                updated.merchantId(),
                Boolean.TRUE,
                null,
                verifiedPeople,
                null,
                null,
                null
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
                    credential.refreshIntervalSeconds(),
                    credential.windowStartedAt(),
                    credential.signatureValue(),
                    credential.signatureVersion()
            ));
        }
        // R5 T15：赠饮权益核销成功 → 发同步事件 ticket.gift_verified，bar 域据此生成 0 元单并扣库存。
        // 同步发布以便测试与同事务；不与本核销主链耦合（主链已照常完成，下游异常不回滚核销由消费者自行兜底）。
        if (GIFT_DRINK_ENTITLEMENT_TYPE.equals(updated.entitlementType())) {
            // R5 TODO#2：赠饮→吧台 SKU 映射来源——权益模板(entitlement_template).bar_item_ref。
            // 已配置则下游对该 item 走 redeem_consume 扣减；为空（未配置）则下游跳过扣减并标注（向后兼容）。
            String barItemRef = repository.findEntitlementTemplateById(updated.entitlementTemplateId())
                    .map(EntitlementTemplateRecord::barItemRef)
                    .orElse(null);
            eventPublisher.publishEvent(new GiftVerifiedEvent(
                    updated.id(),
                    verification.id(),
                    barItemRef,
                    updated.pointId(),
                    null,
                    updated.merchantId()
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
