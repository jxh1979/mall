package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
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
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger success = new AtomicInteger();
        try {
            for (int i = 0; i < THREADS; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        if (repository.atomicDeductTicketStock(TT_ID, 1)) {
                            success.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown(); // 同时放行，制造真实竞争
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // 恰好 STOCK 笔成功，其余被 CAS 拒绝；sold_quantity 精确等于 STOCK，无超卖。
        assertThat(success.get()).isEqualTo(STOCK);
        Integer sold = jdbcTemplate.queryForObject(
                "SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class, TT_ID);
        assertThat(sold).isEqualTo(STOCK);
    }
}
