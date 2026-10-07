package com.complex.platform.common.idempotency;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.api.ApiException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * M724（操作员 2026-10-04 裁决「同一商户、不同项目的两名员工用同一个提交编号，现在各执行一次？？？同一提交编号只能提交一次不允许多次提交」；
 * 治理地图第 303 行「两个操作同时发生冲突时，一律以“先处理成功”的那个为准……其余自动失效并提示」「同一个操作被重复提交，系统只认一次」）：
 * 经生产装配的幂等服务（真实幂等表，提交范围由 AuthorizationService 按登录主体解析）的真库回归。
 *
 * <p>同一「操作号＋请求编号」全平台只生效一次：先处理成功的那次生效；失败、被拒、回滚的提交不占编号；同范围同请求体的重复提交返回
 * 第一次的结果；其他范围再用这个编号 409 {@code IDEMPOTENCY_KEY_USED}，不执行、也不返回第一次的结果。两名员工取同一商户、两个项目。
 *
 * <p>本类不开测试事务：占位是否提交、后到者是否等待、回滚后行在不在，都要真实的多连接才看得见。并发用例的同步屏障是数据库会话视图里
 * 「卡在幂等占位语句上的锁等待」，等不到即失败。幂等行按请求编号后缀隔离、结束时删除。须在一次性空库上跑。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class IdempotencyOnceOnlyJdbcIntegrationTests {

    private static final Map<String, String> BODY = Map.of("effect", "deny");
    private static final Map<String, String> OTHER_BODY = Map.of("effect", "grant");

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String operationId = "m724Once:" + suffix;
    // 同一商户、两个项目的两名在职员工：提交范围不同（mo:<项目长度>:<项目>:<商户>）
    private final AccessActor staffA = new AccessActor("usr_once_a_" + suffix, "usr_once_a_" + suffix, "staff",
            "org_once_a_" + suffix, "mch_once_" + suffix, "active");
    private final AccessActor staffB = new AccessActor("usr_once_b_" + suffix, "usr_once_b_" + suffix, "staff",
            "org_once_b_" + suffix, "mch_once_" + suffix, "active");
    // 与 staffA 同项目同商户的同事：同一提交范围
    private final AccessActor colleagueOfA = new AccessActor("usr_once_c_" + suffix, "usr_once_c_" + suffix, "staff",
            "org_once_a_" + suffix, "mch_once_" + suffix, "active");
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void cleanUp() {
        pool.shutdownNow();
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "once-%" + suffix);
    }

    // 两个范围并发首次提交：首个停在动作里（占位未提交），后到的他范围提交在占位上等；首个提交后，后到者 409 IDEMPOTENCY_KEY_USED。
    // 恰好一个执行，库内该键一行。修复前两个范围是两行，两个动作都执行。
    @Test
    void twoScopesSubmittingConcurrentlyExecuteExactlyOnce() throws Exception {
        String key = "once-race-" + suffix;
        AtomicInteger executed = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<String> first = submitHeldInAction(staffA, key, BODY, "A", executed, entered, release);
        assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue();
        Future<String> second = submitAsync(staffB, key, BODY, () -> "B-" + executed.incrementAndGet());
        awaitBlockedOnReservation(second);
        release.countDown();

        assertThat(first.get(60, TimeUnit.SECONDS)).isEqualTo("A");
        assertThat(rejection(second).code()).isEqualTo("IDEMPOTENCY_KEY_USED");
        assertThat(executed).hasValue(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id || '/' || response_code FROM idempotency_records WHERE idempotency_key = ?", String.class, key))
                .containsExactly(IdempotencyService.storedOperationId(operationId) + "/OK");
    }

    // 首个提交者在调用方的外层事务里，动作已完成但外层未提交；他范围此时提交须等待。外层回滚 → 他范围执行成功并成为第一次；
    // 外层提交 → 他范围 409 IDEMPOTENCY_KEY_USED。修复前他范围不等待，立即按自己的行执行，两段都是两次执行。
    @Test
    void anotherScopeWaitsForAnUncommittedFirstSubmitter() throws Exception {
        for (boolean commit : List.of(false, true)) {
            String key = "once-outer-" + commit + "-" + suffix;
            AtomicInteger executed = new AtomicInteger();
            CountDownLatch actionDone = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Supplier<String> inOuterTransaction = () -> new TransactionTemplate(transactionManager).execute(status -> {
                String result = idempotencyService.execute(operationId, key, BODY, () -> "A-" + executed.incrementAndGet(),
                        String.class);
                actionDone.countDown();
                await(release);
                if (!commit) {
                    status.setRollbackOnly();
                }
                return result;
            });
            Callable<String> firstCall = () -> actingAs(staffA, inOuterTransaction);

            Future<String> first = pool.submit(firstCall);
            assertThat(actionDone.await(30, TimeUnit.SECONDS)).isTrue();
            Future<String> second = submitAsync(staffB, key, BODY, () -> "B-" + executed.incrementAndGet());
            awaitBlockedOnReservation(second);
            assertThat(second.isDone()).as("他范围在首个提交者的事务结束前一直等待").isFalse();
            release.countDown();

            assertThat(first.get(60, TimeUnit.SECONDS)).isEqualTo("A-1");
            if (commit) {
                assertThat(rejection(second).code()).isEqualTo("IDEMPOTENCY_KEY_USED");
                assertThat(executed).hasValue(1);
            } else {
                assertThat(second.get(60, TimeUnit.SECONDS)).isEqualTo("B-2");
                assertThat(executed).hasValue(2);
            }
            assertThat(rows(key)).isEqualTo(1);
        }
    }

    // 失败的提交不占编号：调用方没有外层事务，A 的动作抛错——该键不留行；随后 A 换请求体、他范围的 B 各自用这个编号都能执行
    // （两种来者各用一个新编号重复一遍场景）。修复前占位先提交，失败后留下失败可重试的行，同范围换请求体得 409 IDEMPOTENCY_CONFLICT。
    @Test
    void aFailedSubmissionLeavesNoRowAndAnyScopeMaySubmitAgain() {
        for (boolean anotherScope : List.of(false, true)) {
            String key = "once-fail-" + anotherScope + "-" + suffix;
            assertThatThrownBy(() -> submit(staffA, operationId, key, BODY, () -> {
                throw ApiException.conflict("UPSTREAM_TEMPORARY_FAILURE", "temporary failure");
            })).isInstanceOfSatisfying(ApiException.class, exception ->
                    assertThat(exception.code()).isEqualTo("UPSTREAM_TEMPORARY_FAILURE"));
            assertThat(rows(key)).isZero();

            String retried = anotherScope
                    ? submit(staffB, operationId, key, BODY, () -> "retried")
                    : submit(staffA, operationId, key, OTHER_BODY, () -> "retried");
            assertThat(retried).isEqualTo("retried");
            assertThat(jdbcTemplate.queryForList(
                    "SELECT response_code FROM idempotency_records WHERE idempotency_key = ?", String.class, key))
                    .containsExactly("OK");
        }
    }

    // 调用方没有外层事务时占位不先提交：动作执行中，本事务的连接看得到占位行，另一个连接查不到。
    // 进程在占位后中断时连接断开即回滚，不会留下卡死的处理中行。修复前占位已提交，另一个连接能看到处理中行。
    @Test
    void theReservationIsInvisibleToOtherConnectionsWhileTheActionRuns() {
        String key = "once-invisible-" + suffix;
        AtomicInteger seenBySameConnection = new AtomicInteger(-1);
        AtomicInteger seenByAnotherConnection = new AtomicInteger(-1);
        Callable<Integer> countOnAnotherConnection = () -> rows(key);

        String result = submit(staffA, operationId, key, BODY, () -> {
            seenBySameConnection.set(rows(key));
            try {
                seenByAnotherConnection.set(pool.submit(countOnAnotherConnection).get(30, TimeUnit.SECONDS));
            } catch (Exception failure) {
                throw new IllegalStateException("另一个连接查询失败", failure);
            }
            return "A";
        });

        assertThat(result).isEqualTo("A");
        assertThat(seenBySameConnection).hasValue(1);
        assertThat(seenByAnotherConnection).hasValue(0);
        assertThat(rows(key)).isEqualTo(1);
    }

    // 同范围同请求体并发两次（调用方没有外层事务，后到的是同项目同商户的同事）：后到者等首个提交后返回第一次的结果，动作只执行一次。
    // 修复前后到者立即得到 409 IDEMPOTENCY_IN_PROGRESS。
    @Test
    void aConcurrentDuplicateInTheSameScopeGetsTheFirstResult() throws Exception {
        String key = "once-dup-" + suffix;
        AtomicInteger executed = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<String> first = submitHeldInAction(staffA, key, BODY, "A", executed, entered, release);
        assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue();
        Future<String> second = submitAsync(colleagueOfA, key, BODY, () -> "C-" + executed.incrementAndGet());
        awaitBlockedOnReservation(second);
        release.countDown();

        assertThat(first.get(60, TimeUnit.SECONDS)).isEqualTo("A");
        assertThat(second.get(60, TimeUnit.SECONDS)).isEqualTo("A");
        assertThat(executed).hasValue(1);
        assertThat(rows(key)).isEqualTo(1);
    }

    // 占位等待有上限：首个提交者停在动作里超过 5 秒，后到的他范围提交约 5 秒后得 409 IDEMPOTENCY_IN_PROGRESS、不执行；
    // 首个随后正常完成。修复前后到者不等待，立即在自己的行上执行。
    @Test
    void aWaiterGivesUpAfterTheLockTimeout() throws Exception {
        String key = "once-timeout-" + suffix;
        AtomicInteger executed = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<String> first = submitHeldInAction(staffA, key, BODY, "A", executed, entered, release);
        assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue();
        long startedAt = System.nanoTime();
        Future<String> second = submitAsync(staffB, key, BODY, () -> "B-" + executed.incrementAndGet());
        ApiException rejected = rejection(second);
        long waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertThat(rejected.status().value()).isEqualTo(409);
        assertThat(rejected.code()).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
        assertThat(waitedMillis).isBetween(4_500L, 30_000L);
        assertThat(executed).hasValue(1);

        release.countDown();
        assertThat(first.get(60, TimeUnit.SECONDS)).isEqualTo("A");
        assertThat(rows(key)).isEqualTo(1);
    }

    // 没有登录主体的调用方共用一个提交范围（空）：自己重试返回第一次的结果；登录员工再用这个编号 409 IDEMPOTENCY_KEY_USED、不执行。
    // 反方向同样：登录员工先成功，没有登录主体的调用方再用这个编号被拒。修复前登录员工落在自己的行，动作被执行
    // （无主体自身重试一段是守护）。
    @Test
    void callersWithoutALoginRetrySafelyAndDoNotShareWithLoggedInCallers() {
        AtomicInteger executed = new AtomicInteger();
        Supplier<String> action = () -> "run-" + executed.incrementAndGet();

        String jobKey = "once-nologin-job-" + suffix;
        assertThat(submit(null, operationId, jobKey, BODY, action)).isEqualTo("run-1");
        assertThat(submit(null, operationId, jobKey, BODY, action)).isEqualTo("run-1");
        assertRejected(() -> submit(staffA, operationId, jobKey, BODY, action), "IDEMPOTENCY_KEY_USED");
        assertThat(executed).hasValue(1);
        assertThat(rows(jobKey)).isEqualTo(1);

        String staffKey = "once-nologin-staff-" + suffix;
        assertThat(submit(staffA, operationId, staffKey, BODY, action)).isEqualTo("run-2");
        assertThat(submit(staffA, operationId, staffKey, BODY, action)).isEqualTo("run-2");
        assertRejected(() -> submit(null, operationId, staffKey, BODY, action), "IDEMPOTENCY_KEY_USED");
        assertThat(executed).hasValue(2);
        assertThat(rows(staffKey)).isEqualTo(1);
    }

    // 守护（修复前后都绿）：「全平台只生效一次」按操作号＋请求编号判，不是只按请求编号——一次业务的外层与内层用同一请求编号做两个
    // 不同的操作，同范围下两层各执行一次、各占一行。实现误按编号单独判定时红。
    @Test
    void nestedSameKeyDifferentOperationBothRun() {
        String key = "once-nested-" + suffix;
        AtomicInteger outerRuns = new AtomicInteger();
        AtomicInteger innerRuns = new AtomicInteger();

        String result = submit(staffA, operationId + ":outer", key, BODY, () -> {
            outerRuns.incrementAndGet();
            return "outer+" + idempotencyService.execute(operationId + ":inner", key, BODY, () -> {
                innerRuns.incrementAndGet();
                return "inner";
            }, String.class);
        });

        assertThat(result).isEqualTo("outer+inner");
        assertThat(outerRuns).hasValue(1);
        assertThat(innerRuns).hasValue(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT operation_id FROM idempotency_records WHERE idempotency_key = ? AND response_code = 'OK'", String.class, key))
                .containsExactlyInAnyOrder(IdempotencyService.storedOperationId(operationId + ":outer"),
                        IdempotencyService.storedOperationId(operationId + ":inner"));
    }

    // 防回退：他范围的人先用这个编号提交、在动作内被拒（403）——被拒的提交不占编号；随后有权的人用同一编号照常执行。
    // 相对修复前不红（那时两人各一行）；须由组合变异（去掉外层事务包裹且去掉失败行接管放宽）证明有效，实跑前不算证据。
    @Test
    void aRejectedOutsiderDoesNotBlockTheRightfulCaller() {
        String key = "once-outsider-" + suffix;
        assertThatThrownBy(() -> submit(staffB, operationId, key, BODY, () -> {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能操作其他项目的对象");
        })).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("CROSS_ORG_FORBIDDEN"));

        assertThat(submit(staffA, operationId, key, BODY, () -> "A")).isEqualTo("A");
        assertThat(jdbcTemplate.queryForList(
                "SELECT response_code FROM idempotency_records WHERE idempotency_key = ?", String.class, key))
                .containsExactly("OK");
    }

    // 以该主体身份（null＝没有登录主体）在当前线程执行；提交范围由 AuthorizationService 按安全上下文里的主体解析。
    private <T> T actingAs(AccessActor actor, Supplier<T> call) {
        if (actor == null) {
            SecurityContextHolder.clearContext();
        } else {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    actor, null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF"))));
        }
        try {
            return call.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String submit(AccessActor actor, String operation, String key, Object body, Supplier<String> action) {
        Supplier<String> call = () -> idempotencyService.execute(operation, key, body, action, String.class);
        return actingAs(actor, call);
    }

    private Future<String> submitAsync(AccessActor actor, String key, Object body, Supplier<String> action) {
        Callable<String> call = () -> submit(actor, operationId, key, body, action);
        return pool.submit(call);
    }

    // 首个提交者：动作里先报到（entered）、再等放行（release）才返回——占位、动作都在幂等层自己开的事务里，尚未提交。
    private Future<String> submitHeldInAction(AccessActor actor, String key, Object body, String result,
                                              AtomicInteger executed, CountDownLatch entered, CountDownLatch release) {
        return submitAsync(actor, key, body, () -> {
            executed.incrementAndGet();
            entered.countDown();
            await(release);
            return result;
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待放行超时");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待放行被中断", interrupted);
        }
    }

    // 同步屏障：等到有会话卡在幂等占位语句的锁等待上（后到者在等首个提交者的事务结束）。等不到即失败——
    // 后到者没有真的等待的并发用例不算数，不能退化成先后执行还照样通过。
    private void awaitBlockedOnReservation(Future<?> waiter) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline && !waiter.isDone()) {
            Integer waiting = jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                     WHERE datname = current_database() AND pid <> pg_backend_pid()
                       AND wait_event_type = 'Lock' AND query LIKE ?
                    """, Integer.class, "%INSERT INTO idempotency_records%");
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("同步屏障未命中：后到的提交没有在幂等占位语句上等待");
    }

    // 该提交须被拒绝：返回幂等层抛出的业务异常
    private static ApiException rejection(Future<?> submission) throws Exception {
        try {
            submission.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException failure) {
            assertThat(failure.getCause()).isInstanceOf(ApiException.class);
            return (ApiException) failure.getCause();
        }
        throw new AssertionError("该提交应被拒绝，却正常返回");
    }

    private static void assertRejected(ThrowingCallable submission, String code) {
        assertThatThrownBy(submission).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.status().value()).isEqualTo(409);
            assertThat(exception.code()).isEqualTo(code);
        });
    }

    private Integer rows(String key) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class, key);
    }
}
