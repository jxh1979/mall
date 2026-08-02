package com.complex.platform.common.idempotency;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IdempotencyService {

    private static final long DEFAULT_TTL_SECONDS = 86_400;
    private static final String RESPONSE_CODE_OK = "OK";
    private static final String RESPONSE_CODE_IN_PROGRESS = "IN_PROGRESS";
    private static final String RESPONSE_CODE_FAILED_RETRYABLE = "FAILED_RETRYABLE";

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    private CallerScopeResolver callerScopeResolver = () -> "";
    // B5 事务边界：可选事务模板。注入后把 action(多写) + complete(标记OK) 包成一个原子单元——
    // reserve 仍在外先提交(保住"先占位再执行"并发护栏)，失败留痕(markRetryableFailure)在外照常落。
    // 经 setter 可选注入(同 setCallerScopeResolver 范式)，单测 new 构造不传则保持原直跑行为，零破坏。
    private TransactionTemplate transactionTemplate;

    public IdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Autowired(required = false)
    public void setCallerScopeResolver(CallerScopeResolver callerScopeResolver) {
        this.callerScopeResolver = callerScopeResolver != null ? callerScopeResolver : () -> "";
    }

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
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
        String scopedId = scopedOperationId(operationId);
        String requestHash = hashJson(request);
        Instant now = Instant.now();
        IdempotencyRecord reservation = new IdempotencyRecord(
                UUID.randomUUID(),
                scopedId,
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

        Optional<IdempotencyRecord> existing = repository.findByOperationIdAndIdempotencyKey(scopedId, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (RESPONSE_CODE_FAILED_RETRYABLE.equals(record.responseCode()) && record.requestHash().equals(requestHash)) {
                IdempotencyRecord retryReservation = inProgressReservation(record);
                if (repository.tryUpdateWhenResponseCode(retryReservation, RESPONSE_CODE_FAILED_RETRYABLE)) {
                    return runAndComplete(retryReservation, action, responseType, storedResponseTransformer);
                }
                return repository.findByOperationIdAndIdempotencyKey(scopedId, idempotencyKey)
                        .map(latest -> replay(latest, requestHash, responseType))
                        .orElseThrow(() -> ApiException.conflict(
                                "IDEMPOTENCY_IN_PROGRESS",
                                "请求正在处理中，请稍后刷新查看结果"));
            }
            return replay(record, requestHash, responseType);
        }

        throw ApiException.conflict(
                "IDEMPOTENCY_IN_PROGRESS",
                "请求正在处理中，请稍后刷新查看结果");
    }

    private String scopedOperationId(String operationId) {
        String scope = callerScopeResolver.resolve();
        if (scope == null || scope.isEmpty()) {
            return operationId;
        }
        return scope + ":" + operationId;
    }

    private <T> T replay(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        if (!existing.requestHash().equals(requestHash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该幂等键已被不同的请求使用，请刷新后重试");
        }
        if (RESPONSE_CODE_IN_PROGRESS.equals(existing.responseCode()) || existing.responseBody() == null) {
            throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "请求正在处理中，请稍后刷新查看结果");
        }
        try {
            return objectMapper.readValue(existing.responseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("stored idempotency response is unreadable", exception);
        }
    }

    private IdempotencyRecord inProgressReservation(IdempotencyRecord record) {
        return new IdempotencyRecord(
                record.id(),
                record.operationId(),
                record.idempotencyKey(),
                record.requestHash(),
                RESPONSE_CODE_IN_PROGRESS,
                null,
                202,
                record.createdAt(),
                record.expiresAt()
        );
    }

    private <T> T runAndComplete(
            IdempotencyRecord reservation,
            Supplier<T> action,
            Class<T> responseType,
            Function<T, T> storedResponseTransformer
    ) {
        try {
            // action(多写) + complete(标记OK) 须原子：注入了事务模板则在一个事务内执行（无外层事务时新建、
            // 有则 REQUIRED 并入），否则保持原直跑（单测路径）。reserve 已在外提交、失败留痕在外，故并发护栏不变。
            T response = transactionTemplate == null
                    ? actionAndComplete(reservation, action, storedResponseTransformer)
                    : transactionTemplate.execute(status ->
                            actionAndComplete(reservation, action, storedResponseTransformer));
            return responseType.cast(response);
        } catch (RuntimeException | Error exception) {
            markRetryableFailure(reservation, exception);
            throw exception;
        }
    }

    private <T> T actionAndComplete(
            IdempotencyRecord reservation,
            Supplier<T> action,
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
        return response;
    }

    private void markRetryableFailure(IdempotencyRecord reservation, Throwable failure) {
        try {
            repository.complete(new IdempotencyRecord(
                    reservation.id(),
                    reservation.operationId(),
                    reservation.idempotencyKey(),
                    reservation.requestHash(),
                    RESPONSE_CODE_FAILED_RETRYABLE,
                    null,
                    500,
                    reservation.createdAt(),
                    reservation.expiresAt()
            ));
        } catch (RuntimeException | Error completionException) {
            failure.addSuppressed(completionException);
        }
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
