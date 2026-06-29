package com.complex.platform.notification;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository {

    NotificationSendLogRecord insert(NotificationSendLogRecord record);

    Optional<NotificationSendLogRecord> findByRequestId(String requestId);

    NotificationSendLogRecord updateStatus(
            String requestId,
            String status,
            String providerRef,
            String reason,
            Instant updatedAt
    );
}
