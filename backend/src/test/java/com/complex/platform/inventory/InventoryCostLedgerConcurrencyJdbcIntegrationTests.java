package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * V296 成本台账并发裁决：同物料并发出入库的移动加权状态更新经 inventory_item_cost_state 行锁
 * （SELECT FOR UPDATE）串行化——后到者阻塞至先提交者落账后基于最新状态计算，不允许丢更新。
 * 本类【非事务】，双线程各自经 TransactionTemplate 独立提交（仿 TicketTransferConcurrency 模式），末尾清理。
 *
 * <p>⚠ 由主会话串行调度跑（共享 complex_test 库），本分支内不跑。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class InventoryCostLedgerConcurrencyJdbcIntegrationTests {

    @Autowired private InventoryRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    void twoConcurrentPricedInboundsBothLandExactly() throws Exception {
        String itemId = seedItem();
        try {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            // 并发入库 10@100 与 10@200：行锁串行化后总量 20、总成本 3000、均价 150——丢任一笔即断言失败。
            runBoth(
                    () -> tx.execute(s -> repository.saveStockMovement(
                            inbound(itemId, "mv_ca_" + itemId, 10, 100L), null)),
                    () -> tx.execute(s -> repository.saveStockMovement(
                            inbound(itemId, "mv_cb_" + itemId, 10, 200L), null)));
            InventoryItemCostStateRecord state = lockFreeState(itemId);
            assertThat(state.onHandQuantity()).isEqualTo(20L);
            assertThat(state.totalCostCent()).isEqualTo(3000L);
            assertThat(state.avgUnitCostCent()).isEqualTo(150L);
            assertThat(state.hasCostBasis()).isTrue();
        } finally {
            cleanup(itemId);
        }
    }

    @Test
    void concurrentInboundVersusOutboundKeepsInvariant() throws Exception {
        String itemId = seedItem();
        try {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            // 先建基准：10@100。
            tx.execute(s -> repository.saveStockMovement(inbound(itemId, "mv_seed_" + itemId, 10, 100L), null));
            // 并发：入 10@300 vs 出 5。两种串行化次序的合法终态：
            //   先入后出：20 件 4000 → 出 5×200=1000 → 剩 15 件 3000；
            //   先出后入：出 5×100=500 剩 5 件 500 → 入 10@300 → 15 件 3500。
            runBoth(
                    () -> tx.execute(s -> repository.saveStockMovement(
                            inbound(itemId, "mv_in2_" + itemId, 10, 300L), null)),
                    () -> tx.execute(s -> repository.saveStockMovement(
                            outbound(itemId, "mv_out_" + itemId, 5), null)));
            InventoryItemCostStateRecord state = lockFreeState(itemId);
            assertThat(state.onHandQuantity()).isEqualTo(15L);
            assertThat(state.totalCostCent()).isIn(3000L, 3500L);
            // 守恒：累计入 - 累计出 = 当前持有总成本（任一次序都必须成立，丢更新即破）。
            assertThat(state.cumulativeInCostCent() - state.cumulativeOutCostCent())
                    .isEqualTo(state.totalCostCent());
            // 出库流水成本已定稿且与终态口径一致（500 或 1000，取决于串行化次序）。
            Long outCost = jdbcTemplate.queryForObject(
                    "SELECT total_cost_cent FROM inventory_stock_movements WHERE id = ?",
                    Long.class, "mv_out_" + itemId);
            assertThat(outCost).isIn(500L, 1000L);
        } finally {
            cleanup(itemId);
        }
    }

    private StockMovementRecord inbound(String itemId, String id, int qty, Long unitCostCent) {
        return new StockMovementRecord(id, "inbound", itemId, null, "wh_cost_c", qty,
                unitCostCent, null, "manual", null, null, null, List.of(), "completed", Instant.now(), "m_cost_c");
    }

    private StockMovementRecord outbound(String itemId, String id, int qty) {
        return new StockMovementRecord(id, "outbound", itemId, "wh_cost_c", null, qty,
                null, null, "manual", null, null, null, List.of(), "completed", Instant.now(), "m_cost_c");
    }

    private InventoryItemCostStateRecord lockFreeState(String itemId) {
        return jdbcTemplate.queryForObject("""
                SELECT item_id, merchant_id, on_hand_quantity, total_cost_cent, avg_unit_cost_cent,
                       cumulative_in_cost_cent, cumulative_out_cost_cent, cost_method, has_cost_basis, updated_at
                  FROM inventory_item_cost_state WHERE item_id = ?
                """, (rs, i) -> new InventoryItemCostStateRecord(
                        rs.getString("item_id"), rs.getString("merchant_id"), rs.getLong("on_hand_quantity"),
                        rs.getLong("total_cost_cent"), rs.getLong("avg_unit_cost_cent"),
                        rs.getLong("cumulative_in_cost_cent"), rs.getLong("cumulative_out_cost_cent"),
                        rs.getString("cost_method"), rs.getBoolean("has_cost_basis"),
                        rs.getTimestamp("updated_at").toInstant()), itemId);
    }

    private void runBoth(Callable<?> a, Callable<?> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Object> wa = () -> { barrier.await(); return a.call(); };
        Callable<Object> wb = () -> { barrier.await(); return b.call(); };
        try {
            Future<Object> fa = pool.submit(wa);
            Future<Object> fb = pool.submit(wb);
            fa.get();
            fb.get();
        } finally {
            pool.shutdownNow();
        }
    }

    private String seedItem() {
        String itemId = "item_cost_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, min_quantity, status,
                                             created_at, updated_at, merchant_id)
                VALUES (?, '成本并发测试物料', 'beverage', '瓶', 0, 'active', now(), now(), 'm_cost_c')
                """, itemId);
        return itemId;
    }

    private void cleanup(String itemId) {
        jdbcTemplate.update("DELETE FROM inventory_stock_movements WHERE item_id = ?", itemId);
        jdbcTemplate.update("DELETE FROM inventory_item_cost_state WHERE item_id = ?", itemId);
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id = ?", itemId);
    }
}
