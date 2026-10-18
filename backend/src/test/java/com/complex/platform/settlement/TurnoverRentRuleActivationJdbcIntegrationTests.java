package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class TurnoverRentRuleActivationJdbcIntegrationTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcSettlementConfigWriteRepository repository;
    @Autowired private PlatformTransactionManager transactionManager;

    private String ruleId;
    private String merchantId;

    @BeforeEach
    void insertDraft() {
        String suffix = UUID.randomUUID().toString();
        ruleId = "trr_activation_" + suffix;
        merchantId = "trr_activation_merchant_" + suffix;
        jdbc.update("""
                INSERT INTO turnover_rent_rules
                    (id, contract_id, merchant_id, rule_name, rate_bps, effective_from,
                     status, is_draft, updated_by)
                VALUES (?, NULL, ?, '转正序号回归', 1000, DATE '2026-08-01',
                        'active', TRUE, 'activation_drafter')
                """, ruleId, merchantId);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM turnover_rent_rules WHERE merchant_id = ?", merchantId);
    }

    @Test
    void restrictedRuntimeRoleCanAllocateSequenceAndActivateDraft() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL ROLE complex_app");
            assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo("complex_app");
            assertThat(jdbc.queryForObject("""
                    SELECT has_sequence_privilege(current_user, 'turnover_rent_rule_activation_seq', 'USAGE')
                       AND has_sequence_privilege(current_user, 'turnover_rent_rule_activation_seq', 'SELECT')
                    """, Boolean.class)).isTrue();

            assertThat(activate()).isEqualTo(1);
            assertThat(sequence()).isPositive();
        });

        assertThat(jdbc.queryForObject("SELECT is_draft FROM turnover_rent_rules WHERE id = ?",
                Boolean.class, ruleId)).isFalse();
    }

    @Test
    void waitingApprovalRechecksDraftAfterFirstApprovalCommits() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        AtomicInteger waitingPid = new AtomicInteger();
        CountDownLatch workerStarted = new CountDownLatch(1);
        AtomicReference<Future<Integer>> waitingApproval = new AtomicReference<>();
        long[] originalSequence = new long[1];
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(activate()).isEqualTo(1);
                originalSequence[0] = sequence();
                waitingApproval.set(worker.submit(() ->
                        new TransactionTemplate(transactionManager).execute(secondStatus -> {
                            waitingPid.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                            workerStarted.countDown();
                            return activate();
                        })));
                awaitWorker(workerStarted);
                assertWaitingForAdvisoryLock(waitingPid.get());
            });

            assertThat(waitingApproval.get().get(10, TimeUnit.SECONDS)).isZero();
            assertThat(sequence()).isEqualTo(originalSequence[0]);
            assertThat(jdbc.queryForObject("SELECT status FROM turnover_rent_rules WHERE id = ?",
                    String.class, ruleId)).isEqualTo("active");
            assertThat(jdbc.queryForObject("SELECT is_draft FROM turnover_rent_rules WHERE id = ?",
                    Boolean.class, ruleId)).isFalse();
        } finally {
            worker.shutdownNow();
            assertThat(worker.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private int activate() {
        return repository.activateSingleRowDraft(
                JdbcSettlementConfigWriteRepository.SingleRowConfig.TURNOVER_RENT_RULE,
                ruleId, "activation_approver", Instant.now());
    }

    private long sequence() {
        return jdbc.queryForObject("SELECT activation_sequence FROM turnover_rent_rules WHERE id = ?",
                Long.class, ruleId);
    }

    private void assertWaitingForAdvisoryLock(int pid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        boolean waiting;
        do {
            waiting = Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_locks
                                    WHERE pid = ? AND locktype = 'advisory' AND NOT granted)
                    """, Boolean.class, pid));
            if (waiting) {
                break;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        } while (System.nanoTime() < deadline);
        assertThat(waiting).as("第二次批准确实等待首个事务的同范围审批锁").isTrue();
    }

    private static void awaitWorker(CountDownLatch started) {
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("等待并发批准线程时中断", interrupted);
        }
    }
}
