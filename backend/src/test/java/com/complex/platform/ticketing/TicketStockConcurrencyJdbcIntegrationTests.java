package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 票务限量库存原子扣减并发超卖防护（真实多线程，非 @Transactional——需提交数据 + 跨线程可见）。
 * 补审计 P1「全测试库零并发用例」缺口：验证 atomicDeductTicketStock 的 CAS（sold+qty<=stock）在并发下精确防超卖。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class TicketStockConcurrencyJdbcIntegrationTests {

    private static final String TT_ID = "tt_concurrency_oversell";
    private static final int STOCK = 50;
    private static final int THREADS = 200;

    @Autowired
    private JdbcTicketingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", TT_ID);
        jdbcTemplate.update("""
                INSERT INTO ticket_types
                    (id, ticket_name, base_calendar_price_mode, stock_required, status,
                     stock_quantity, sold_quantity, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, 'active', ?, 0, ?, ?)
                """,
                TT_ID, "并发超卖测试票", "weekday_weekend_holiday", STOCK,
                java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now()));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", TT_ID);
    }

    @Test
    void atomicDeductPreventsOversellUnderConcurrency() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(THREADS);
        List<Throwable> unexpected = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                // submit(Callable) so每个任务的返回/异常都进入 Future——下面逐一 get()，
                // 工作线程抛出的运行时异常不再被吞掉（AUD-023）。
                futures.add(pool.submit(() -> {
                    start.await(); // 同时放行，制造真实竞争
                    if (repository.atomicDeductTicketStock(TT_ID, 1)) {
                        success.incrementAndGet();
                    } else {
                        rejected.incrementAndGet(); // CAS 明确拒绝（业务售罄），与异常区分
                    }
                    return null;
                }));
            }
            start.countDown();
            // 逐一 get()：任何工作线程异常在此以 ExecutionException 浮出，不再静默留在 Future 里。
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    unexpected.add(e.getCause() != null ? e.getCause() : e);
                } catch (TimeoutException e) {
                    unexpected.add(e);
                }
            }
        } finally {
            pool.shutdownNow();
        }

        // 意外异常必须为 0：区分「预期的售罄拒绝」与死锁/连接错误/代码异常。
        assertThat(unexpected)
                .as("worker threads must not throw unexpected exceptions, got: %s", unexpected)
                .isEmpty();
        // 恰好 STOCK 笔成功、其余 THREADS-STOCK 笔被 CAS 拒绝；sold_quantity 精确等于 STOCK，无超卖。
        assertThat(success.get()).isEqualTo(STOCK);
        assertThat(rejected.get()).isEqualTo(THREADS - STOCK);
        Integer sold = jdbcTemplate.queryForObject(
                "SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, TT_ID);
        assertThat(sold).isEqualTo(STOCK);
    }
}
