package com.complex.platform.integration;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class OpenPlatformCrypto {

    private final ObjectMapper signatureObjectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private final SecureRandom secureRandom = new SecureRandom();

    void verifySignature(String actual, String canonical, String key, String errorCode) {
        IntegrationValidation.requireText(actual, "signature");
        String expected = hmacHex(key, canonical);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.unauthorized(errorCode, "signature is invalid");
        }
    }

    String openAuthCanonical(String appKey, long timestamp, String nonce) {
        return "app_key=" + appKey + "\nnonce=" + nonce + "\ntimestamp=" + timestamp;
    }

    String webhookCanonical(String endpointKey, String provider, String event, String idempotencyKey,
            String payloadHash, long timestamp, String nonce) {
        // 纳入 nonce + timestamp 防重放：每次传输需带新鲜 timestamp 与唯一 nonce 并参与签名。
        return "endpoint_key=" + endpointKey + "\nevent=" + event + "\nidempotency_key=" + idempotencyKey
                + "\nnonce=" + nonce + "\npayload_hash=" + payloadHash + "\nprovider=" + provider
                + "\ntimestamp=" + timestamp;
    }

    String payloadHash(Map<String, Object> payload) {
        try {
            return sha256(signatureObjectMapper.writeValueAsString(payload == null ? Map.of() : payload));
        } catch (Exception exception) {
            throw ApiException.badRequest("OPEN_WEBHOOK_PAYLOAD_INVALID", "开放平台回调内容无法序列化");
        }
    }

    String newRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "p0_open_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String hmacHex(String key, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }
}
