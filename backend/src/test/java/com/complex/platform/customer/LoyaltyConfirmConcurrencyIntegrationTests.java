package com.complex.platform.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AUD-014 并发确认积分修复门禁（真实 Postgres，真并发提交）。
 *
 * <p>刻意 <b>不加类级 {@code @Transactional}</b>：种子必须真提交，两个工作线程才能在各自独立事务里看到 earn 行并竞争
 * {@code FOR UPDATE} 行锁。@Transactional 回滚 + 单连接会让种子只在测试连接可见，工作线程读不到，测不出锁行为。
 *
 * <p>核心不变量：<b>账户余额恒等于该会员 earn 行已确认份额（confirmed_amount）之和</b>。修复前两事务基于同一旧值
 * 各加 50 → 余额 100 而明细仍 50（账实分叉）；修复后 earn 行 FOR UPDATE 串行化 → 后到事务读到 50 → delta=0 幂等。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class LoyaltyConfirmConcurrencyIntegrationTests {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private JdbcCustomerRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> createdCustomerIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String customerId : createdCustomerIds) {
            jdbcTemplate.update("DELETE FROM customer_points_ledger WHERE customer_id = ?", customerId);
            jdbcTemplate.update("DELETE FROM customer_points_accounts WHERE customer_id = ?", customerId);
            jdbcTemplate.update("DELETE FROM customers WHERE id = ?", customerId);
        }
        createdCustomerIds.clear();
    }

    /** 种子一名会员 + 一条 pending earn 行，返回 [customerId, sourceDocId, awarded]。 */
    private Seed seedPendingEarn() {
        Instant now = Instant.now();
        String suffix = Long.toHexString(System.nanoTime());
        String customerId = "cust_aud014_" + suffix;
        String phone = "138" + String.format("%08d", Math.abs(suffix.hashCode()) % 100_000_000);
        repository.saveCustomer(new CustomerRecord(
                customerId, phone, "138****" + phone.substring(7), "active", null, now, now, now));
        createdCustomerIds.add(customerId);
        String sourceDocId = "o_aud014_" + suffix;
        long awarded = loyaltyService.earnPointsPending(customerId, 50, "ticket_order", sourceDocId, "aud-014 seed", null);
        return new Seed(customerId, sourceDocId, awarded);
    }

    private long confirmedSum(String customerId) {
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(confirmed_amount),0) FROM customer_points_ledger "
                        + "WHERE customer_id = ? AND change_type = 'earn'", Long.class, customerId);
        return sum == null ? 0 : sum;
    }

    private long accountBalance(String customerId) {
        return repository.findPointsByCustomerId(customerId).orElseThrow().balance();
    }

    @Test
    void concurrentConfirmSameTargetCreditsOnce() throws Exception {
        Seed seed = seedPendingEarn();
        List<Long> deltas = runConcurrently(
                () -> loyaltyService.confirmPoints("ticket_order", seed.sourceDocId(), 1, 1),
                () -> loyaltyService.confirmPoints("ticket_order", seed.sourceDocId(), 1, 1));

        // 两次确认合计只入账一次 awarded；余额恒等于明细已确认份额之和（不变量）。
        assertThat(deltas.stream().mapToLong(Long::longValue).sum()).isEqualTo(seed.awarded());
        assertThat(confirmedSum(seed.customerId())).isEqualTo(seed.awarded());
        assertThat(accountBalance(seed.customerId())).isEqualTo(seed.awarded());
    }

    @Test
    void concurrentConfirmAndReversalKeepBalanceEqualToConfirmed() throws Exception {
        Seed seed = seedPendingEarn();
        // 确认(1/1)与冲正(0/1)并发：无论谁先，末态余额恒 = earn.confirmed_amount（0 或 awarded），账实不分叉。
        runConcurrently(
                () -> loyaltyService.confirmPoints("ticket_order", seed.sourceDocId(), 1, 1),
                () -> loyaltyService.confirmPoints("ticket_order", seed.sourceDocId(), 0, 1));

        assertThat(accountBalance(seed.customerId())).isEqualTo(confirmedSum(seed.customerId()));
    }

    @Test
    void replayedConfirmIsIdempotent() {
        Seed seed = seedPendingEarn();
        long first = loyaltyService.confirmPoints("ticket_order", seed.sourceDocId(), 1, 1);
        long replay = loyaltyService.confirmPoints("ticket_order", seed.sourceDocId(), 1, 1); // 消息重放
        assertThat(first).isEqualTo(seed.awarded());
        assertThat(replay).isZero();
        assertThat(accountBalance(seed.customerId())).isEqualTo(confirmedSum(seed.customerId()));
        assertThat(accountBalance(seed.customerId())).isEqualTo(seed.awarded());
    }

    /** 两任务用 CyclicBarrier 尽量同刻起跑，最大化 FOR UPDATE 竞争窗口；返回各自 delta。 */
    private List<Long> runConcurrently(Callable<Long> a, Callable<Long> b) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (Callable<Long> task : List.of(a, b)) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    return task.call();
                }));
            }
            List<Long> results = new ArrayList<>();
            for (Future<Long> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private record Seed(String customerId, String sourceDocId, long awarded) {
    }
}
