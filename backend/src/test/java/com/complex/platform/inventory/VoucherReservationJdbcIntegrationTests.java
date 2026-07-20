package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
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
 * V312 发券预占库存并发正确性（防超发）JDBC 集成测试（按约定交付会话不运行，待 DB 窗口串行跑）：
 * 守卫式 UPDATE（WHERE available_quantity>=qty）+ 行锁在真并发下恰好放行可用量、绝不超预占；
 * 预占/释放的余额三列语义（reserved/available/quantity）与 V012 CHECK 兜底。
 *
 * <p>非 @Transactional：真并发需各线程各自提交，故手动 seed/清理固定 fixture（vres_item_test/vres_loc_test）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class VoucherReservationJdbcIntegrationTests {

    private static final String ITEM = "vres_item_test";
    private static final String LOC = "vres_loc_test";

    @Autowired
    private InventoryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM inventory_stock_balances WHERE item_id = ?", ITEM);
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id = ?", ITEM);
    }

    private void seedBalance(int available) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at)
                VALUES (?, '预占并发测试货', 'goods', '件', 'active', ?, ?)
                """, ITEM, now, now);
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at)
                VALUES (?, ?, ?, ?, 0, ?, ?)
                """, "vres_bal_test", ITEM, LOC, 100, available, now);
    }

    private int[] balance() {
        return jdbcTemplate.queryForObject(
                "SELECT quantity, reserved_quantity, available_quantity FROM inventory_stock_balances WHERE item_id = ? AND location_id = ?",
                (rs, n) -> new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)}, ITEM, LOC);
    }

    @Test
    void concurrentReserveNeverOversellsAvailable() throws InterruptedException {
        seedBalance(3); // 可用 3 件，quantity 100
        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (repository.reserveStockBalance(ITEM, LOC, 1) > 0) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // 恰好 3 笔预占成功（等于可用量），其余撞守卫失败——绝不超发。
        assertThat(successes.get()).isEqualTo(3);
        int[] b = balance();
        assertThat(b[0]).as("quantity 预占不动").isEqualTo(100);
        assertThat(b[1]).as("reserved").isEqualTo(3);
        assertThat(b[2]).as("available 归零").isEqualTo(0);
    }

    @Test
    void reserveThenReleaseRoundTripsAndGuardsBoundaries() {
        seedBalance(2);
        // 预占 2 后可用不足再占失败。
        assertThat(repository.reserveStockBalance(ITEM, LOC, 2)).isEqualTo(1);
        assertThat(repository.reserveStockBalance(ITEM, LOC, 1)).isZero();
        int[] afterReserve = balance();
        assertThat(afterReserve).containsExactly(100, 2, 0);
        // 释放 1：reserved->available。
        assertThat(repository.releaseStockReservation(ITEM, LOC, 1)).isEqualTo(1);
        assertThat(balance()).containsExactly(100, 1, 1);
        // 释放超过 reserved 的量：守卫拒绝（reserved 不足）。
        assertThat(repository.releaseStockReservation(ITEM, LOC, 5)).isZero();
        assertThat(balance()).containsExactly(100, 1, 1);
    }

    @Test
    void reserveOnMissingBalanceRowFailsClosed() {
        // 无余额行（货品未在该库位建账）：预占返回 0 → 上层拒绝发券/兑换（不扣积分），对齐 PRD §5。
        assertThat(repository.reserveStockBalance(ITEM, LOC, 1)).isZero();
    }
}
