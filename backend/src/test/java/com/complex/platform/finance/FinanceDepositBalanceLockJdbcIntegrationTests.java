package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessActor;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.support.StaffRequestFixture;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 BH 段第 261 条 261-B14-R2（真库、真实服务、双线程各自独立事务）。依据：PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」；
 * BSM:1353「实际收款或退款成功后才更新已收押金余额」。同一商户的押金处理先取商户级的锁再读余额，逐笔串行：
 * 先到者在事务里停住，必须在库里观测到后到者真的在锁上等待才放行；后到者按先到者提交后的余额办。
 *
 * <p>本类不开测试事务（两条线程各自提交）。押金台账与应收应付只增不删，夹具按随机后缀隔离留在库里。押金余额都经真实路径落：
 * 变更押金补缴挂应收（{@link FinanceService#registerDepositAdjustment}），项目财务登记实收（{@link FinanceService#registerArApPayment}）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class FinanceDepositBalanceLockJdbcIntegrationTests {

    @Autowired private FinanceService finance;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager txManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_b14r2_" + s;
    private AccessActor cashier;
    private ExecutorService pool;
    private final CountDownLatch release = new CountDownLatch(1);

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','押金串行回归综合体','active',now(),now())",
                project);
        String userId = "usr_b14r2_" + s;
        new StaffRequestFixture(jdbc).staff(userId, "staff", project, null, "finance.payment.register", "finance.ar_ap.manage");
        cashier = new AccessActor(userId, userId, "staff", project, null, "active");
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        SecurityContextHolder.clearContext();
    }

    /** ① 余额 10000 的商户，两笔退租应退押金同时落账各要退 10000：后到者等先到者提交后读到余额已为 0，不再退。修复前后到者按旧余额再退 10000。 */
    @Test
    void secondRefundWaitsAndSeesTheBalanceAlreadyRefunded() throws Exception {
        String merchant = merchant("m");
        collect(merchant, 10000L, "seed");
        assertThat(balances(merchant)).containsExactly(10000L);

        CountDownLatch held = new CountDownLatch(1);
        Future<Long> first = pool.submit(() -> inTransaction(() -> {
            long refunded = finance.createDepositRefundPayable(merchant, 10000L, "stl_b14r2_a_" + s, null, project);
            held.countDown();
            await(release);
            return refunded;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("先到者已在事务内落账").isTrue();
        Future<Long> second = pool.submit(() -> inTransaction(
                () -> finance.createDepositRefundPayable(merchant, 10000L, "stl_b14r2_b_" + s, null, project)));
        awaitLockWaiter();
        release.countDown();

        assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(10000L);
        assertThat(second.get(30, TimeUnit.SECONDS)).isZero();
        assertThat(balances(merchant)).containsExactly(10000L, 0L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = 'deposit_refund' AND counterparty_id = ?",
                Integer.class, merchant)).isEqualTo(1);
    }

    /** ② 还没有押金台账的商户：先到者登记补缴实收 5000 未提交，后到者退 5000 须等先到者提交后按 5000 的余额退。修复前无行可锁，后到者不等、读到 0 不退。 */
    @Test
    void refundWaitsBehindTheFirstCollectionOfAMerchantWithoutLedger() throws Exception {
        String merchant = merchant("n");
        ArApItemRecord supplement = finance.registerDepositAdjustment(merchant, "ctr_b14r2_n_" + s, "chg_b14r2_n_" + s, 5000L,
                LocalDate.now().plusDays(30), project);

        CountDownLatch held = new CountDownLatch(1);
        Future<Long> first = pool.submit(() -> as(cashier, () -> inTransaction(() -> {
            finance.registerArApPayment(supplement.id(), payment(5000L, "n"));
            held.countDown();
            await(release);
            return 5000L;
        })));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("先到者已在事务内登记实收").isTrue();
        Future<Long> second = pool.submit(() -> inTransaction(
                () -> finance.createDepositRefundPayable(merchant, 5000L, "stl_b14r2_c_" + s, null, project)));
        awaitLockWaiter();
        release.countDown();

        assertThat(first.get(30, TimeUnit.SECONDS)).isEqualTo(5000L);
        assertThat(second.get(30, TimeUnit.SECONDS)).isEqualTo(5000L);
        assertThat(balances(merchant)).containsExactly(5000L, 0L);
    }

    // ---------- 夹具与工具 ----------

    private String merchant(String tag) {
        String id = "mch_b14r2_" + tag + "_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'retail','active',?,now(),now())", id, "押金串行回归商户" + tag + s, project);
        return id;
    }

    /** 经真实路径给商户落一笔押金实收：变更押金补缴挂应收，项目财务登记实收。 */
    private void collect(String merchant, long amountCent, String tag) {
        ArApItemRecord item = finance.registerDepositAdjustment(merchant, "ctr_b14r2_" + tag + "_" + s, "chg_b14r2_" + tag + "_" + s,
                amountCent, LocalDate.now().plusDays(30), project);
        as(cashier, () -> finance.registerArApPayment(item.id(), payment(amountCent, tag)));
    }

    private ArApPaymentRequest payment(long amountCent, String tag) {
        return new ArApPaymentRequest("pay-b14r2-" + tag + "-" + s, null, List.of(), amountCent, "bank_transfer", null, null, null);
    }

    private List<Long> balances(String merchant) {
        return jdbc.queryForList("SELECT balance_after_cent FROM deposit_ledger WHERE merchant_id = ? ORDER BY created_at, id", Long.class, merchant);
    }

    private <T> T inTransaction(Supplier<T> action) {
        return new TransactionTemplate(txManager).execute(status -> action.get());
    }

    private static <T> T as(AccessActor actor, Supplier<T> action) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** 必须在库里实际观测到有事务在锁上等待，才算后到者已排上队。 */
    private void awaitLockWaiter() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"
                    + " AND wait_event_type = 'Lock'", Integer.class);
            if (waiting != null && waiting >= 1) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new AssertionError("未观测到后到者在锁上等待");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("未获放行");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
