package com.complex.platform.integration;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/open")
public class OpenPlatformController {

    private final IntegrationService service;

    public OpenPlatformController(IntegrationService service) {
        this.service = service;
    }

    @GetMapping("/d/{endpoint_key}/events/{event_id}")
    public ApiResponse<OpenEventResultResponse> getEventResult(
            @PathVariable("endpoint_key") String endpointKey,
            @PathVariable("event_id") String eventId,
            @RequestHeader("X-Open-App-Key") String appKey,
            @RequestHeader("X-Open-Timestamp") Long timestamp,
            @RequestHeader("X-Open-Nonce") String nonce,
            @RequestHeader("X-Open-Signature") String signature
    ) {
        return ApiResponse.ok(service.getOpenEventResult(
                endpointKey,
                eventId,
                new OpenSignatureRequest(appKey, timestamp, nonce, signature)
        ));
    }

    @PostMapping("/d/{endpoint_key}/webhooks/{provider}/{event}")
    public ApiResponse<WebhookAcceptedResponse> receiveWebhook(
            @PathVariable("endpoint_key") String endpointKey,
            @PathVariable String provider,
            @PathVariable String event,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Open-Timestamp", required = false) Long timestamp,
            @RequestHeader(value = "X-Open-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyHeader,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        String idempotencyKey = idempotencyKey(idempotencyHeader, payload);
        return ApiResponse.ok(service.receiveWebhook(
                endpointKey, provider, event, signature, timestamp, nonce, idempotencyKey, payload));
    }

    private String idempotencyKey(String headerValue, Map<String, Object> payload) {
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        Object bodyValue = payload == null ? null : payload.get("idempotency_key");
        if (bodyValue instanceof String value && !value.isBlank()) {
            return value;
        }
        throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
    }
}
