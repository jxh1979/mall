package com.complex.platform.common.idempotency;

// 文件版本：V0.03

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
    // M724：行键不含提交范围——"once:"＋操作号的 SHA-256，定长 69
    private static final String STORED = IdempotencyService.storedOperationId("operation");

    @Test
    void newRequestIsReservedBeforeActionRunsAndCompletedAfterSuccess() {
        TestRequest request = new TestRequest("same-payload");
        AtomicReference<Optional<IdempotencyRecord>> recordDuringAction = new AtomicReference<>();

        TestResponse response = service.execute(
                "operation",
                "key",
                request,
                () -> {
                    recordDuringAction.set(repository.findByOperationIdAndIdempotencyKey(STORED, "key"));
                    return new TestResponse("created");
                },
                TestResponse.class
        );

        assertThat(response).isEqualTo(new TestResponse("created"));
        assertThat(STORED).startsWith("once:").hasSize(69);
        assertThat(recordDuringAction.get())
                .hasValueSatisfying(record -> {
                    assertThat(record.operationId()).isEqualTo(STORED);
                    assertThat(record.idempotencyKey()).isEqualTo("key");
                    assertThat(record.requestHash()).isEqualTo(requestHash("", request)).hasSize(128);
                    assertThat(record.responseCode()).isEqualTo("IN_PROGRESS");
                    assertThat(record.responseBody()).isNull();
                    assertThat(record.httpStatus()).isEqualTo(202);
                });
        assertThat(repository.findByOperationIdAndIdempotencyKey(STORED, "key"))
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
                STORED,
                "key",
                requestHash("", request),
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
                STORED,
                "key",
                requestHash("", new TestRequest("first-payload")),
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
                STORED,
                "key",
                requestHash("", request),
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
        assertThat(repository.findByOperationIdAndIdempotencyKey(STORED, "key"))
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

    // XM-M002-J：只凭「用掉单次授权的那次成功请求」的重放过了权限核验的请求，只能返回第一次的成功结果：
    // 记录不在或第一次失败（可重试）即 403，不占位、不执行；正文不同照常 409。
    @Test
    void aReplayOnlyRequestReturnsTheCompletedResultAndNeverRunsTheAction() {
        TestRequest request = new TestRequest("same-payload");
        AtomicInteger runs = new AtomicInteger();
        java.util.function.Supplier<TestResponse> action = () -> {
            runs.incrementAndGet();
            return new TestResponse("should-not-run");
        };
        IdempotentReplayContext previous = IdempotentReplayContext.enter("key", "digest");
        try {
            IdempotentReplayContext.current().markReplayOnly();

            assertThatThrownBy(() -> service.execute("operation", "key", request, action, TestResponse.class))
                    .isInstanceOfSatisfying(ApiException.class, exception ->
                            assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));
            assertThat(repository.findByOperationIdAndIdempotencyKey(STORED, "key")).isEmpty();

            repository.put(new IdempotencyRecord(UUID.randomUUID(), STORED, "key", requestHash("", request),
                    "FAILED_RETRYABLE", null, 500,
                    Instant.parse("2026-06-13T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
            assertThatThrownBy(() -> service.execute("operation", "key", request, action, TestResponse.class))
                    .isInstanceOfSatisfying(ApiException.class, exception ->
                            assertThat(exception.code()).isEqualTo("PERMISSION_DENIED"));

            repository.put(new IdempotencyRecord(UUID.randomUUID(), STORED, "key", requestHash("", request),
                    "OK", "{\"value\":\"created\"}", 200,
                    Instant.parse("2026-06-13T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
            assertThat(service.execute("operation", "key", request, action, TestResponse.class))
                    .isEqualTo(new TestResponse("created"));
            assertThatThrownBy(() -> service.execute("operation", "key", new TestRequest("other-payload"), action,
                    TestResponse.class))
                    .isInstanceOfSatisfying(ApiException.class, exception ->
                            assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
            assertThat(runs).hasValue(0);
        } finally {
            IdempotentReplayContext.restore(previous);
        }
    }

    // M723-F04（操作员 2026-09-25 确认句「同一次操作重复提交时，直接返回第一次的结果并提示已完成」）：幂等层返回第一次的结果时，
    // 被重放的请求编号正是本请求自己的才记为已重放；首次执行不记；业务内部按别的键去重的回放不记；
    // 同一请求里另有一步新执行了动作的不算重复提交。
    @Test
    void aReplayIsMarkedOnlyWhenItReturnsTheFirstResultOfTheRequestsOwnKey() {
        TestRequest request = new TestRequest("same-payload");
        java.util.function.Supplier<TestResponse> action = () -> new TestResponse("created");

        IdempotentReplayContext previous = IdempotentReplayContext.enter("key", "digest");
        try {
            assertThat(service.execute("operation", "key", request, action, TestResponse.class))
                    .isEqualTo(new TestResponse("created"));
            assertThat(IdempotentReplayContext.current().replayed()).isFalse();
        } finally {
            IdempotentReplayContext.restore(previous);
        }

        previous = IdempotentReplayContext.enter("other-key", "digest");
        try {
            assertThat(service.execute("operation", "key", request, action, TestResponse.class))
                    .isEqualTo(new TestResponse("created"));
            assertThat(IdempotentReplayContext.current().replayed()).isFalse();
        } finally {
            IdempotentReplayContext.restore(previous);
        }

        previous = IdempotentReplayContext.enter("key", "digest");
        try {
            assertThat(service.execute("operation", "key", request, action, TestResponse.class))
                    .isEqualTo(new TestResponse("created"));
            assertThat(IdempotentReplayContext.current().replayed()).isTrue();
        } finally {
            IdempotentReplayContext.restore(previous);
        }

        previous = IdempotentReplayContext.enter("key", "digest");
        try {
            service.execute("operation", "key", request, action, TestResponse.class);
            service.execute("another-operation", "key", request, action, TestResponse.class);
            assertThat(IdempotentReplayContext.current().replayed()).isFalse();
        } finally {
            IdempotentReplayContext.restore(previous);
        }
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

        assertThat(repository.findByOperationIdAndIdempotencyKey(STORED, "key"))
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
                    recordDuringRetry.set(repository.findByOperationIdAndIdempotencyKey(STORED, "key"));
                    return new TestResponse("recovered");
                },
                TestResponse.class
        );

        assertThat(response).isEqualTo(new TestResponse("recovered"));
        assertThat(attempts).hasValue(2);
        assertThat(recordDuringRetry.get())
                .hasValueSatisfying(record -> assertThat(record.responseCode()).isEqualTo("IN_PROGRESS"));
        assertThat(repository.findByOperationIdAndIdempotencyKey(STORED, "key"))
                .hasValueSatisfying(record -> {
                    assertThat(record.responseCode()).isEqualTo("OK");
                    assertThat(record.responseBody()).isEqualTo("{\"value\":\"recovered\"}");
                    assertThat(record.httpStatus()).isEqualTo(200);
                });
    }

    // M724（操作员 2026-10-04 裁决「同一提交编号只能提交一次不允许多次提交」）：范围 A 成功之后，其他范围再用同一操作号＋请求编号，
    // 请求体相同或不同回答都一样——409 IDEMPOTENCY_KEY_USED、同一句提示，不执行、拿不到 A 的结果；该键在库内仍只有 A 的那一行。
    // 修复前不同范围各占一行，B 的动作被执行。
    @Test
    void anotherScopeReusingAKeyIsRejectedWithoutRunningOrSeeingTheFirstResult() {
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
        assertThat(responseA).isEqualTo(new TestResponse("for-A"));

        // WHEN: merchant-B uses the SAME operationId + idempotencyKey, once with the same body and once with another body
        currentScope.set("m:merchant-B");
        for (TestRequest fromB : java.util.List.of(request, new TestRequest("different-payload"))) {
            assertThatThrownBy(() -> scopedService.execute(
                    "createItem", "key-1", fromB,
                    () -> { actionCount.incrementAndGet(); return new TestResponse("for-B"); },
                    TestResponse.class
            ))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.status().value()).isEqualTo(409);
                        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_USED");
                        assertThat(exception.getMessage()).isEqualTo("该提交编号已被使用，本次操作未执行");
                    });
        }

        // THEN: only merchant-A's action ran and the key still has exactly merchant-A's row
        assertThat(actionCount).hasValue(1);
        assertThat(scopedRepo.records.values()).singleElement().satisfies(record -> {
            assertThat(record.operationId()).isEqualTo(IdempotencyService.storedOperationId("createItem"));
            assertThat(record.requestHash()).isEqualTo(requestHash("m:merchant-A", request));
            assertThat(record.responseBody()).isEqualTo("{\"value\":\"for-A\"}");
        });
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
        AtomicReference<TestResponse> responseB = new AtomicReference<>();
        assertThatThrownBy(() -> responseB.set(scopedService.execute(
                "createItem", "key-1", request,
                () -> { bActionRan.set(true); return new TestResponse("for-B"); },
                TestResponse.class
        )))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_USED");
                    assertThat(exception.getMessage()).doesNotContain("secret-A");
                });

        // THEN: merchant-B was rejected (M724), its action did not run and it did NOT get merchant-A's data
        assertThat(bActionRan).isFalse();
        assertThat(responseB.get()).isNotEqualTo(new TestResponse("secret-A"));
        assertThat(responseB.get()).isNull();
    }

    // M724 守护（修复前后都绿，由变异「范围与请求体比较顺序对调」证明有效）：同范围换请求体仍是 IDEMPOTENCY_CONFLICT，
    // 不是 IDEMPOTENCY_KEY_USED；同范围同请求体返回第一次的结果。
    @Test
    void sameScopeDifferentBodyStillGetsIdempotencyConflict() {
        service.setCallerScopeResolver(() -> "mo:2:O1:mch_1");
        TestRequest request = new TestRequest("same-payload");
        AtomicInteger actions = new AtomicInteger();
        java.util.function.Supplier<TestResponse> action = () -> {
            actions.incrementAndGet();
            return new TestResponse("created");
        };

        assertThat(service.execute("operation", "key", request, action, TestResponse.class))
                .isEqualTo(new TestResponse("created"));
        assertThatThrownBy(() -> service.execute("operation", "key", new TestRequest("different-payload"), action,
                TestResponse.class))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(service.execute("operation", "key", request, action, TestResponse.class))
                .isEqualTo(new TestResponse("created"));
        assertThat(actions).hasValue(1);
    }

    // M724（治理地图第 303 行「以“先处理成功”的那个为准」；失败的提交不占编号）：没有事务的实例里 A 失败留下失败可重试的行，
    // 其他范围、另一请求体的 B 用同一编号提交 → 接管并执行成功，库内仍一行、行上的散列是 B 的；随后 A 再提交 → 409 IDEMPOTENCY_KEY_USED。
    // 修复前 B 落在自己的行（库内两行），A 再提交会重跑成功。
    @Test
    void aFailedSubmissionDoesNotHoldTheKeyForAnotherScopeOrBody() {
        AtomicReference<String> currentScope = new AtomicReference<>("scope-A");
        service.setCallerScopeResolver(currentScope::get);
        TestRequest requestA = new TestRequest("payload-A");
        TestRequest requestB = new TestRequest("payload-B");

        assertThatThrownBy(() -> service.execute("operation", "key", requestA,
                () -> {
                    throw ApiException.conflict("UPSTREAM_TEMPORARY_FAILURE", "temporary failure");
                },
                TestResponse.class))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("UPSTREAM_TEMPORARY_FAILURE"));
        assertThat(repository.findByOperationIdAndIdempotencyKey(STORED, "key"))
                .hasValueSatisfying(record -> assertThat(record.responseCode()).isEqualTo("FAILED_RETRYABLE"));

        currentScope.set("scope-B");
        assertThat(service.execute("operation", "key", requestB, () -> new TestResponse("for-B"), TestResponse.class))
                .isEqualTo(new TestResponse("for-B"));
        assertThat(repository.records.values()).singleElement().satisfies(record -> {
            assertThat(record.responseCode()).isEqualTo("OK");
            assertThat(record.requestHash()).isEqualTo(requestHash("scope-B", requestB));
        });

        currentScope.set("scope-A");
        AtomicBoolean ranAgain = new AtomicBoolean(false);
        assertThatThrownBy(() -> service.execute("operation", "key", requestA,
                () -> {
                    ranAgain.set(true);
                    return new TestResponse("for-A");
                },
                TestResponse.class))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_USED"));
        assertThat(ranAgain).isFalse();
    }

    // M724：其他范围遇到看得见的处理中行（第一次还没成功）→ 与同范围同一句 409 IDEMPOTENCY_IN_PROGRESS，不执行，不说「已被使用」。
    // 修复前 B 落在另一行并执行。
    @Test
    void anotherScopeSeeingAVisibleInProgressRowGetsInProgress() {
        TestRequest request = new TestRequest("same-payload");
        repository.put(new IdempotencyRecord(UUID.randomUUID(), STORED, "key", requestHash("scope-A", request),
                "IN_PROGRESS", null, 202,
                Instant.parse("2026-06-13T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
        service.setCallerScopeResolver(() -> "scope-B");
        AtomicBoolean actionRan = new AtomicBoolean(false);

        assertThatThrownBy(() -> service.execute("operation", "key", request,
                () -> {
                    actionRan.set(true);
                    return new TestResponse("should-not-run");
                },
                TestResponse.class))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(409);
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
                });
        assertThat(actionRan).isFalse();
    }

    // M724：权限核验认定「只许重放」的来者，行却是别的范围的成功行（如本人已换项目或商户）→ 409 IDEMPOTENCY_KEY_USED，
    // 不返回结果、不记已重放。修复前按带范围的键查不到行（403）；改键后若漏了范围比较会回放别人的结果。
    @Test
    void replayOnlyFromAnotherScopeIsRejected() {
        TestRequest request = new TestRequest("same-payload");
        repository.put(new IdempotencyRecord(UUID.randomUUID(), STORED, "key", requestHash("scope-A", request),
                "OK", "{\"value\":\"created\"}", 200,
                Instant.parse("2026-06-13T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")));
        service.setCallerScopeResolver(() -> "scope-B");
        AtomicBoolean actionRan = new AtomicBoolean(false);
        AtomicReference<TestResponse> returned = new AtomicReference<>();
        IdempotentReplayContext previous = IdempotentReplayContext.enter("key", "digest");
        try {
            IdempotentReplayContext.current().markReplayOnly();

            assertThatThrownBy(() -> returned.set(service.execute("operation", "key", request,
                    () -> {
                        actionRan.set(true);
                        return new TestResponse("should-not-run");
                    },
                    TestResponse.class)))
                    .isInstanceOfSatisfying(ApiException.class, exception ->
                            assertThat(exception.code()).isEqualTo("IDEMPOTENCY_KEY_USED"));
            assertThat(returned.get()).isNull();
            assertThat(actionRan).isFalse();
            assertThat(IdempotentReplayContext.current().replayed()).isFalse();
        } finally {
            IdempotentReplayContext.restore(previous);
        }
    }

    // M724：占位语句等别的事务超过锁等待上限时，数据库报 SQLSTATE 55P03——按根因 SQLException 的 SQLSTATE 认出，转成
    // 409 IDEMPOTENCY_IN_PROGRESS，不执行。修复前异常原样抛出，成为 500。
    @Test
    void aLockTimeoutOnReservationBecomesInProgress() {
        IdempotencyRepository lockTimedOut = new IdempotencyRepository() {
            @Override
            public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
                return Optional.empty();
            }

            @Override
            public boolean tryReserve(IdempotencyRecord record) {
                throw new IllegalStateException("could not acquire lock",
                        new java.sql.SQLException("canceling statement due to lock timeout", "55P03"));
            }

            @Override
            public IdempotencyRecord complete(IdempotencyRecord record) {
                return record;
            }
        };
        IdempotencyService timedOutService = new IdempotencyService(lockTimedOut, objectMapper);
        AtomicBoolean actionRan = new AtomicBoolean(false);

        assertThatThrownBy(() -> timedOutService.execute("operation", "key", new TestRequest("same-payload"),
                () -> {
                    actionRan.set(true);
                    return new TestResponse("should-not-run");
                },
                TestResponse.class))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(409);
                    assertThat(exception.code()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
                });
        assertThat(actionRan).isFalse();
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

    // M724：行上的请求散列＝提交范围的 SHA-256（前 64 位）＋请求体散列（后 64 位），共 128 位
    private String requestHash(String scope, Object request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(scope.getBytes(StandardCharsets.UTF_8))) + hashJson(request);
        } catch (Exception exception) {
            throw new IllegalStateException("test hashing unavailable", exception);
        }
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
            // M724：与 JdbcIdempotencyRepository 同语义——只看状态，不再要求请求散列相同，行上的散列换成来者的
            if (existing == null || !expectedResponseCode.equals(existing.responseCode())) {
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
