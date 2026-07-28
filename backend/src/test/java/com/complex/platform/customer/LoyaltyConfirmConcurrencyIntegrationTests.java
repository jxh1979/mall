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

    // customer_points_ledger 是 append-only（V140 禁 DELETE）且 FK 引用 customers → 种子行不可删；
    // 全机唯一自增序（起点=nanoTime，单机 nanoTime 跨进程单调递增）保证 customer_id/明文手机号跨运行不撞，
    // 靠唯一种子隔离残留（残留行无害，无需清理）。
    // ponytail: 单机唯一序，跨机并行跑同库才需换 UUID/含机器码。
    private static final java.util.concurrent.atomic.AtomicLong SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.nanoTime());

    /** 种子一名会员 + 一条 pending earn 行（唯一 id，不清理）。 */
    private Seed seedPendingEarn() {
        Instant now = Instant.now();
        String uid = Long.toHexString(SEQ.getAndIncrement());
        String customerId = "cust_aud014_" + uid;
        // 明文手机号只需唯一：库存的是加密封套(enc:v1:%)，唯一性由 phone_hash 盲索引保证，明文无格式 CHECK。
        String phone = "1" + String.format("%012d", Math.floorMod(SEQ.getAndIncrement(), 1_000_000_000_000L));
        repository.saveCustomer(new CustomerRecord(
                customerId, phone, "1****" + phone.substring(phone.length() - 4), "active", null, now, now, now));
        String sourceDocId = "o_aud014_" + uid;
        long awarded = loyaltyService.earnPointsPending(customerId, 50, "ticket_order", sourceDocId, "aud-014 seed", null);
        return new Seed(customerId, sourceDocId, awarded);
    }

    /**
     * P0-B（V344）积分台账改追加：已确认份额的权威口径 = 该会员 confirm 分录的净额和
     * （正=确认、负=冲正），不再读 earn 行被就地改写的 confirmed_amount。
     */
    private long confirmedSum(String customerId) {
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(points_delta),0) FROM customer_points_ledger "
                        + "WHERE customer_id = ? AND change_type = 'confirm'", Long.class, customerId);
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
        // 确认(1/1)与冲正(0/1)并发：无论谁先，末态余额恒 = confirm 分录净额和（0 或 awarded），账实不分叉。
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
