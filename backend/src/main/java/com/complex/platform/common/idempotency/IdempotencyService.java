package com.complex.platform.common.idempotency;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private static final long DEFAULT_TTL_SECONDS = 86_400;
    private static final String RESPONSE_CODE_OK = "OK";
    private static final String RESPONSE_CODE_IN_PROGRESS = "IN_PROGRESS";

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public <T> T execute(
            String operationId,
            String idempotencyKey,
            Object request,
            Supplier<T> action,
            Class<T> responseType
    ) {
        return execute(operationId, idempotencyKey, request, action, responseType, Function.identity());
    }

    public <T> T execute(
            String operationId,
            String idempotencyKey,
            Object request,
            Supplier<T> action,
            Class<T> responseType,
            Function<T, T> storedResponseTransformer
    ) {
        requireIdempotencyKey(idempotencyKey);
        String requestHash = hashJson(request);
        Instant now = Instant.now();
        IdempotencyRecord reservation = new IdempotencyRecord(
                UUID.randomUUID(),
                operationId,
                idempotencyKey,
                requestHash,
                RESPONSE_CODE_IN_PROGRESS,
                null,
                202,
                now,
                now.plusSeconds(DEFAULT_TTL_SECONDS)
        );

        if (repository.tryReserve(reservation)) {
            return runAndComplete(reservation, action, responseType, storedResponseTransformer);
        }

        return repository.findByOperationIdAndIdempotencyKey(operationId, idempotencyKey)
                .map(existing -> replay(existing, requestHash, responseType))
                .orElseThrow(() -> ApiException.conflict(
                        "IDEMPOTENCY_IN_PROGRESS",
                        "idempotency request is still being processed"
                ));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
    }

    private <T> T replay(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        if (!existing.requestHash().equals(requestHash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "idempotency_key was already used with a different request");
        }
        if (RESPONSE_CODE_IN_PROGRESS.equals(existing.responseCode()) || existing.responseBody() == null) {
            throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "idempotency request is still being processed");
        }
        try {
            return objectMapper.readValue(existing.responseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored idempotency response is unreadable", exception);
        }
    }

    private <T> T runAndComplete(
            IdempotencyRecord reservation,
            Supplier<T> action,
            Class<T> responseType,
            Function<T, T> storedResponseTransformer
    ) {
        T response = action.get();
        T storedResponse = storedResponseTransformer == null
                ? response
                : storedResponseTransformer.apply(response);
        repository.complete(new IdempotencyRecord(
                reservation.id(),
                reservation.operationId(),
                reservation.idempotencyKey(),
                reservation.requestHash(),
                RESPONSE_CODE_OK,
                writeJson(storedResponse),
                200,
                reservation.createdAt(),
                reservation.expiresAt()
        ));
        return responseType.cast(response);
    }

    private String hashJson(Object value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(writeJson(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("request hashing unavailable", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("idempotency payload is not serializable", exception);
        }
    }
}
