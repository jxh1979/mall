package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * AUD-009 并发续付：两笔续付同时抢同一账单的剩余应收——行锁(findBillByIdForUpdate)串行化后，
 * 后到者按已累计的 paidAmount 重新校验剩余额度，恰一笔成功、总收款不超应收。
 * 本类【非事务】，双线程各自独立提交，末尾清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class BarBillContinuationJdbcIntegrationTests {

    @Autowired private BarBillingService billingService;
    @Autowired private JdbcBarRepository barRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    void concurrentContinuationSettlementsNeverOverCollect() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String billId = "bill_cc_" + suffix;
        String orderId = "order_cc_" + suffix;
        String tableId = "table_cc_" + suffix;
        Instant now = Instant.now();
        // 已部分付款 40000/128800 的账单（账单不挂桌台=不触发低消闸；订单/桌台满足非空约束）。
        new TransactionTemplate(txManager).executeWithoutResult(s -> {
            barRepository.saveTable(new BarTableRecord(
                    tableId, "bar_cc", "merchant_cc", "T-CC" + suffix, "并发续付桌", "card_seat",
                    4, 1, 0L, "CNY", "active", now, now));
            barRepository.saveOrder(new BarOrderRecord(
                    orderId, "bar_cc", "merchant_cc", tableId, null, null,
                    "staff", "created", 128800L, "CNY", "web", null, null, now, now));
            barRepository.saveBill(new BarBillRecord(
                    billId, "BILL-CC-" + suffix, orderId, "bar_cc", "merchant_cc",
                    tableId, null, 128800L, 0L, 0L, 128800L, 40000L, "CNY",
                    "partial_paid", "partial_settled", null, null, now, now));
        });
        try {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            // 员工上下文为 ThreadLocal——两个工作线程各自装配/清理。
            Callable<Object> payRemaining1 = () -> {
                com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
                try {
                    return tx.execute(s -> billingService.settleBill(billId, new BarBillSettleRequest(
                            "cc-key-1", null, List.of(), "pay_cc_1", 88800L, "aggregate_pay")));
                } catch (ApiException e) {
                    return e;
                } finally {
                    com.complex.platform.support.TestSecurityActors.clear();
                }
            };
            Callable<Object> payRemaining2 = () -> {
                com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
                try {
                    return tx.execute(s -> billingService.settleBill(billId, new BarBillSettleRequest(
                            "cc-key-2", null, List.of(), "pay_cc_2", 88800L, "aggregate_pay")));
                } catch (ApiException e) {
                    return e;
                } finally {
                    com.complex.platform.support.TestSecurityActors.clear();
                }
            };
            CyclicBarrier barrier = new CyclicBarrier(2);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Object> f1 = pool.submit(() -> { barrier.await(); return payRemaining1.call(); });
                Future<Object> f2 = pool.submit(() -> { barrier.await(); return payRemaining2.call(); });
                Object r1 = f1.get();
                Object r2 = f2.get();
                long wins = List.of(r1, r2).stream().filter(r -> r instanceof BarBillRecord).count();
                assertThat(wins)
                        .withFailMessage("r1=%s r2=%s",
                                r1 instanceof ApiException e1 ? e1.code() + ":" + e1.getMessage() : "WIN",
                                r2 instanceof ApiException e2 ? e2.code() + ":" + e2.getMessage() : "WIN")
                        .isEqualTo(1L);
                Object loser = r1 instanceof ApiException ? r1 : r2;
                // 败者两种合法拒绝:赢家已把账单结清(ALREADY_SETTLED),或行锁后重校验剩余为0(remaining)。
                assertThat(((ApiException) loser).getMessage())
                        .satisfiesAnyOf(
                                m -> assertThat(m).contains("剩余未付"),
                                m -> assertThat(m).contains("已结账"));
            } finally {
                pool.shutdownNow();
            }
            // 账实核对：paidAmount 恰=128800，流水合计恰=88800（只入了一笔续付）。
            Long paid = jdbcTemplate.queryForObject(
                    "SELECT paid_amount_cent FROM bar_bills WHERE id = ?", Long.class, billId);
            assertThat(paid).isEqualTo(128800L);
            Long settled = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(settlement_amount_cent),0) FROM bar_bill_settlements WHERE bill_id = ?",
                    Long.class, billId);
            assertThat(settled).isEqualTo(88800L);
            String status = jdbcTemplate.queryForObject(
                    "SELECT settlement_status FROM bar_bills WHERE id = ?", String.class, billId);
            assertThat(status).isEqualTo("settled");
        } finally {
            jdbcTemplate.update("DELETE FROM bar_bill_settlements WHERE bill_id = ?", billId);
            jdbcTemplate.update("DELETE FROM bar_bills WHERE id = ?", billId);
            jdbcTemplate.update("DELETE FROM bar_orders WHERE id = ?", orderId);
            jdbcTemplate.update("DELETE FROM bar_tables WHERE id = ?", tableId);
        }
    }
}
