package com.complex.platform.common.idempotency;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTests {

    private final InMemoryIdempotencyRepository repository = new InMemoryIdempotencyRepository();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final IdempotencyService service = new IdempotencyService(repository, objectMapper);

    @Test
    void newRequestIsReservedBeforeActionRunsAndCompletedAfterSuccess() {
        TestRequest request = new TestRequest("same-payload");
        AtomicReference<Optional<IdempotencyRecord>> recordDuringAction = new AtomicReference<>();

        TestResponse response = service.execute(
                "operation",
                "key",
                request,
                () -> {
                    recordDuringAction.set(repository.findByOperationIdAndIdempotencyKey("operation", "key"));
                    return new TestResponse("created");
                },
                TestResponse.class
        );

        assertThat(response).isEqualTo(new TestResponse("created"));
        assertThat(recordDuringAction.get())
                .hasValueSatisfying(record -> {
                    assertThat(record.operationId()).isEqualTo("operation");
                    assertThat(record.idempotencyKey()).isEqualTo("key");
                    assertThat(record.requestHash()).isEqualTo(hashJson(request));
                    assertThat(record.responseCode()).isEqualTo("IN_PROGRESS");
                    assertThat(record.responseBody()).isNull();
                    assertThat(record.httpStatus()).isEqualTo(202);
                });
        assertThat(repository.findByOperationIdAndIdempotencyKey("operation", "key"))
                .hasValueSatisfying(record -> {
                    assertThat(record.responseCode()).isEqualTo("OK");
                    assertThat(record.responseBody()).isEqualTo("{\"value\":\"created\"}");
                    assertThat(record.httpStatus()).isEqualTo(200);
                });
    }

    @Test
    void duplicateSameRequestWhileReservedReturnsInProgressWithoutRunningAction() {
        TestRequest request = new TestRequest("same-payload");
        repository.put(new IdempotencyRecord(
                UUID.randomUUID(),
                "operation",
                "key",
                hashJson(request),
                "IN_PROGRESS",
                null,
                202,
                Instant.parse("2026-06-13T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        AtomicBoolean actionRan = new AtomicBoolean(false);

        assertThatThrownBy(() -> service.execute(
                "operation",
                "key",
                request,
                () -> {
                    actionRan.set(true);
                    return new TestResponse("should-not-run");
                },
                TestResponse.class
        ))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(409);
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
                });
        assertThat(actionRan).isFalse();
    }

    @Test
    void duplicateDifferentRequestWhileReservedReturnsConflictWithoutRunningAction() {
        repository.put(new IdempotencyRecord(
                UUID.randomUUID(),
                "operation",
                "key",
                hashJson(new TestRequest("first-payload")),
                "IN_PROGRESS",
                null,
                202,
                Instant.parse("2026-06-13T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        AtomicBoolean actionRan = new AtomicBoolean(false);

        assertThatThrownBy(() -> service.execute(
                "operation",
                "key",
                new TestRequest("different-payload"),
                () -> {
                    actionRan.set(true);
                    return new TestResponse("should-not-run");
                },
                TestResponse.class
        ))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(actionRan).isFalse();
    }

    @Test
    void duplicateSameRequestWithCompletedBodyReturnsStoredResponseWithoutRunningAction() {
        TestRequest request = new TestRequest("same-payload");
        repository.put(new IdempotencyRecord(
                UUID.randomUUID(),
                "operation",
                "key",
                hashJson(request),
                "OK",
                "{\"value\":\"cached\"}",
                200,
                Instant.parse("2026-06-13T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        AtomicBoolean actionRan = new AtomicBoolean(false);

        TestResponse response = service.execute(
                "operation",
                "key",
                request,
                () -> {
                    actionRan.set(true);
                    return new TestResponse("should-not-run");
                },
                TestResponse.class
        );

        assertThat(response).isEqualTo(new TestResponse("cached"));
        assertThat(actionRan).isFalse();
    }

    @Test
    void canStoreSanitizedResponseWhileReturningOriginalResponse() {
        TestRequest request = new TestRequest("same-payload");

        TestResponse first = service.execute(
                "operation",
                "key",
                request,
                () -> new TestResponse("plain-secret"),
                TestResponse.class,
                response -> new TestResponse(null)
        );

        assertThat(first).isEqualTo(new TestResponse("plain-secret"));
        assertThat(repository.findByOperationIdAndIdempotencyKey("operation", "key"))
                .hasValueSatisfying(record -> assertThat(record.responseBody()).isEqualTo("{\"value\":null}"));

        AtomicBoolean actionRan = new AtomicBoolean(false);
        TestResponse replay = service.execute(
                "operation",
                "key",
                request,
                () -> {
                    actionRan.set(true);
                    return new TestResponse("should-not-run");
                },
                TestResponse.class,
                response -> new TestResponse(null)
        );

        assertThat(replay).isEqualTo(new TestResponse(null));
        assertThat(actionRan).isFalse();
    }

    @Test
    void failedActionMarksReservationRetryableAndAllowsSameRequestToRunAgain() {
        TestRequest request = new TestRequest("same-payload");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> service.execute(
                "operation",
                "key",
                request,
                () -> {
                    attempts.incrementAndGet();
                    throw ApiException.conflict("UPSTREAM_TEMPORARY_FAILURE", "temporary failure");
                },
                TestResponse.class
        ))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("UPSTREAM_TEMPORARY_FAILURE"));

        assertThat(repository.findByOperationIdAndIdempotencyKey("operation", "key"))
                .hasValueSatisfying(record -> {
                    assertThat(record.responseCode()).isEqualTo("FAILED_RETRYABLE");
                    assertThat(record.responseBody()).isNull();
                    assertThat(record.httpStatus()).isEqualTo(500);
                });

        AtomicReference<Optional<IdempotencyRecord>> recordDuringRetry = new AtomicReference<>();
        TestResponse response = service.execute(
                "operation",
                "key",
                request,
                () -> {
                    attempts.incrementAndGet();
                    recordDuringRetry.set(repository.findByOperationIdAndIdempotencyKey("operation", "key"));
                    return new TestResponse("recovered");
                },
                TestResponse.class
        );

        assertThat(response).isEqualTo(new TestResponse("recovered"));
        assertThat(attempts).hasValue(2);
        assertThat(recordDuringRetry.get())
                .hasValueSatisfying(record -> assertThat(record.responseCode()).isEqualTo("IN_PROGRESS"));
        assertThat(repository.findByOperationIdAndIdempotencyKey("operation", "key"))
                .hasValueSatisfying(record -> {
                    assertThat(record.responseCode()).isEqualTo("OK");
                    assertThat(record.responseBody()).isEqualTo("{\"value\":\"recovered\"}");
                    assertThat(record.httpStatus()).isEqualTo(200);
                });
    }

    @Test
    void differentCallerScopesProduceIndependentIdempotencySlots() {
        // GIVEN: a scoped service with switchable caller scope
        AtomicReference<String> currentScope = new AtomicReference<>("");
        InMemoryIdempotencyRepository scopedRepo = new InMemoryIdempotencyRepository();
        IdempotencyService scopedService = new IdempotencyService(scopedRepo, objectMapper);
        scopedService.setCallerScopeResolver(currentScope::get);

        TestRequest request = new TestRequest("same-payload");
        AtomicInteger actionCount = new AtomicInteger();

        // WHEN: merchant-A completes a write
        currentScope.set("m:merchant-A");
        TestResponse responseA = scopedService.execute(
                "createItem", "key-1", request,
                () -> { actionCount.incrementAndGet(); return new TestResponse("for-A"); },
                TestResponse.class
        );

        // WHEN: merchant-B uses the SAME operationId + idempotencyKey
        currentScope.set("m:merchant-B");
        TestResponse responseB = scopedService.execute(
                "createItem", "key-1", request,
                () -> { actionCount.incrementAndGet(); return new TestResponse("for-B"); },
                TestResponse.class
        );

        // THEN: both actions ran (no cross-tenant collision)
        assertThat(actionCount).hasValue(2);
        assertThat(responseA).isEqualTo(new TestResponse("for-A"));
        assertThat(responseB).isEqualTo(new TestResponse("for-B"));
    }

    @Test
    void merchantBCannotReplayMerchantAStoredResponseViaSameKey() {
        // GIVEN: merchant-A stores a completed idempotency record
        AtomicReference<String> currentScope = new AtomicReference<>("m:merchant-A");
        InMemoryIdempotencyRepository scopedRepo = new InMemoryIdempotencyRepository();
        IdempotencyService scopedService = new IdempotencyService(scopedRepo, objectMapper);
        scopedService.setCallerScopeResolver(currentScope::get);

        TestRequest request = new TestRequest("same-payload");
        scopedService.execute("createItem", "key-1", request,
                () -> new TestResponse("secret-A"), TestResponse.class);

        // WHEN: merchant-B sends the same request with the same idempotency key
        currentScope.set("m:merchant-B");
        AtomicBoolean bActionRan = new AtomicBoolean();
        TestResponse responseB = scopedService.execute(
                "createItem", "key-1", request,
                () -> { bActionRan.set(true); return new TestResponse("for-B"); },
                TestResponse.class
        );

        // THEN: merchant-B ran its own action and did NOT get merchant-A's data
        assertThat(bActionRan).isTrue();
        assertThat(responseB).isEqualTo(new TestResponse("for-B"));
        assertThat(responseB).isNotEqualTo(new TestResponse("secret-A"));
    }


    // M006-031：范围变更不能重放归属不明的旧结果，也不能再次执行旧请求。
    @Test
    void changedCallerScopePreservesOldOperationsAndBoundsNewKeys() {
        service.setCallerScopeResolver(new CallerScopeResolver() {
            public String resolve() { return "u:" + "user".repeat(16) + ":" + "organization".repeat(6); }
            public String legacyScope() { return "p"; }
        });
        TestRequest request = new TestRequest("same-payload");
        AtomicInteger actions = new AtomicInteger();
        for (String code : java.util.List.of("OK", "IN_PROGRESS", "FAILED_RETRYABLE")) {
            IdempotencyRecord old = new IdempotencyRecord(UUID.randomUUID(), "p:operation", code,
                    hashJson(request), code, code.equals("OK") ? "{\"value\":\"old-result\"}" : null,
                    code.equals("OK") ? 200 : 202, Instant.now(), Instant.now().plusSeconds(86400));
            repository.put(old);
            assertThatThrownBy(() -> service.execute("operation", code, request,
                    () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class))
                    .isInstanceOfSatisfying(ApiException.class, failure ->
                            assertThat(failure.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
            assertThat(repository.findByOperationIdAndIdempotencyKey("p:operation", code)).contains(old);
        }
        assertThat(actions).hasValue(0);
        String operation = "createRole:" + "r".repeat(64);
        TestResponse first = service.execute(operation, "fresh", request,
                () -> { actions.incrementAndGet(); return new TestResponse("created"); }, TestResponse.class);
        TestResponse retry = service.execute(operation, "fresh", request,
                () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class);
        assertThat(retry).isEqualTo(first);
        assertThat(actions).hasValue(1);
        assertThat(repository.records.values()).allMatch(record -> record.operationId().length() <= 128);
    }

    private String hashJson(Object value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("test payload is not serializable", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("test hashing unavailable", exception);
        }
    }

    private record TestRequest(String value) {
    }

    private record TestResponse(String value) {
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(key(operationId, idempotencyKey)));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = key(record.operationId(), record.idempotencyKey());
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public boolean tryUpdateWhenResponseCode(IdempotencyRecord record, String expectedResponseCode) {
            String key = key(record.operationId(), record.idempotencyKey());
            IdempotencyRecord existing = records.get(key);
            if (existing == null
                    || !existing.requestHash().equals(record.requestHash())
                    || !expectedResponseCode.equals(existing.responseCode())) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(key(record.operationId(), record.idempotencyKey()), record);
            return record;
        }

        void put(IdempotencyRecord record) {
            records.put(key(record.operationId(), record.idempotencyKey()), record);
        }

        private String key(String operationId, String idempotencyKey) {
            return operationId + ":" + idempotencyKey;
        }
    }
}
