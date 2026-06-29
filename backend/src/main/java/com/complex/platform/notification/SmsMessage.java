package com.complex.platform.notification;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SmsMessage(
        String requestId,
        String domain,
        String eventKey,
        String recipient,
        String templateCode,
        Map<String, Object> params
) {
    public SmsMessage {
        params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
