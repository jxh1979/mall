package com.complex.platform.notification;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class NotificationService {

    private static final String CHANNEL_SMS = "sms";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_SENT = "sent";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_THROTTLED = "throttled";
    private static final String STATUS_SKIPPED_SWITCH_OFF = "skipped_switch_off";
    private static final Set<String> FINAL_SENDER_STATUSES = Set.of(STATUS_SENT, STATUS_FAILED, STATUS_THROTTLED);
    private static final String OPERATION_NOTIFY_SMS = "notification.sms.notify";

    private final NotificationRepository notificationRepository;
    private final NotificationSwitchResolver switchResolver;
    private final SmsSender smsSender;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationSwitchResolver switchResolver,
            SmsSender smsSender,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.notificationRepository = notificationRepository;
        this.switchResolver = switchResolver;
        this.smsSender = smsSender;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public NotificationSendLogRecord notify(
            String domain,
            String event,
            String recipient,
            String templateCode,
            Map<String, Object> params,
            String idempotencyKey
    ) {
        String requestId = requireText(idempotencyKey, "idempotency_key");
        String normalizedDomain = requireText(domain, "domain");
        String normalizedEvent = requireText(event, "event");
        SmsMessage message = new SmsMessage(
                requestId,
                normalizedDomain,
                normalizedEvent,
                requireText(recipient, "recipient"),
                requireText(templateCode, "template_code"),
                params == null ? Map.of() : params
        );

        Optional<NotificationSendLogRecord> existing = notificationRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return existing.get();
        }

        return idempotencyService.execute(
                OPERATION_NOTIFY_SMS,
                requestId,
                message,
                () -> notificationRepository.findByRequestId(requestId)
                        .orElseGet(() -> sendOnce(message)),
                NotificationSendLogRecord.class
        );
    }

    private NotificationSendLogRecord sendOnce(SmsMessage message) {
        Instant now = Instant.now();
        NotificationSendLogRecord pending = notificationRepository.insert(new NotificationSendLogRecord(
                "notif_" + UUID.randomUUID().toString().replace("-", ""),
                message.requestId(),
                CHANNEL_SMS,
                message.domain(),
                message.eventKey(),
                message.recipient(),
                message.templateCode(),
                message.params(),
                STATUS_PENDING,
                null,
                null,
                now,
                now
        ));
        if (!STATUS_PENDING.equals(pending.status())) {
            return pending;
        }

        NotificationSwitchDecision decision;
        try {
            decision = switchResolver.resolve(message.domain(), message.eventKey());
        } catch (RuntimeException exception) {
            NotificationSendLogRecord failed = notificationRepository.updateStatus(
                    message.requestId(),
                    STATUS_FAILED,
                    null,
                    "switch_resolve_failed:" + codeOf(exception),
                    Instant.now()
            );
            recordAudit(failed);
            return failed;
        }

        if (!decision.allowed()) {
            NotificationSendLogRecord skipped = notificationRepository.updateStatus(
                    message.requestId(),
                    STATUS_SKIPPED_SWITCH_OFF,
                    null,
                    decision.reason(),
                    Instant.now()
            );
            recordAudit(skipped);
            return skipped;
        }

        SmsSendResult result = sendSafely(message);
        NotificationSendLogRecord updated = notificationRepository.updateStatus(
                message.requestId(),
                normalizeStatus(result.status()),
                trimToNull(result.providerRef()),
                trimToNull(result.reason()),
                Instant.now()
        );
        recordAudit(updated);
        return updated;
    }

    private SmsSendResult sendSafely(SmsMessage message) {
        try {
            SmsSendResult result = smsSender.send(message);
            if (result == null) {
                return new SmsSendResult(STATUS_FAILED, null, "sms_sender_returned_null");
            }
            String status = normalizeStatus(result.status());
            if (!status.equals(trimToNull(result.status()))) {
                return new SmsSendResult(status, result.providerRef(), "unsupported_sender_status:" + result.status());
            }
            return result;
        } catch (RuntimeException exception) {
            return new SmsSendResult(STATUS_FAILED, null, codeOf(exception));
        }
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null || !FINAL_SENDER_STATUSES.contains(normalized)) {
            return STATUS_FAILED;
        }
        return normalized;
    }

    private void recordAudit(NotificationSendLogRecord record) {
        try {
            switch (record.status()) {
                case STATUS_SENT -> auditEventService.recordSuccess(
                        "sms.notification.sent",
                        "not_applicable",
                        "notification_send_log",
                        record.id(),
                        record.requestId(),
                        record.requestId(),
                        auditMetadata(record)
                );
                case STATUS_SKIPPED_SWITCH_OFF -> auditEventService.recordSuccess(
                        "sms.notification.skipped",
                        "not_applicable",
                        "notification_send_log",
                        record.id(),
                        record.requestId(),
                        record.requestId(),
                        auditMetadata(record)
                );
                case STATUS_THROTTLED -> auditEventService.recordFailure(
                        "sms.notification.throttled",
                        "not_applicable",
                        "notification_send_log",
                        record.id(),
                        record.requestId(),
                        record.requestId(),
                        trimToDefault(record.reason(), STATUS_THROTTLED)
                );
                default -> auditEventService.recordFailure(
                        "sms.notification.failed",
                        "not_applicable",
                        "notification_send_log",
                        record.id(),
                        record.requestId(),
                        record.requestId(),
                        trimToDefault(record.reason(), STATUS_FAILED)
                );
            }
        } catch (RuntimeException ignored) {
            // Notification send_log is the authoritative delivery trail; audit is best-effort here.
        }
    }

    private Map<String, Object> auditMetadata(NotificationSendLogRecord record) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("channel", record.channel());
        metadata.put("domain", record.domain());
        metadata.put("event_key", record.eventKey());
        metadata.put("template_code", record.templateCode());
        metadata.put("status", record.status());
        if (record.providerRef() != null) {
            metadata.put("provider_ref", record.providerRef());
        }
        if (record.reason() != null) {
            metadata.put("reason", record.reason());
        }
        return metadata;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException && apiException.code() != null) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
