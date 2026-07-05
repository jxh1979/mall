package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.auth.AuthService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 服务订单域资金安全 TDD(2026-07-05 审计四项)。
 * 并发用真多线程复现;非 @Transactional(并发跨连接需真提交,测试自行清理构造数据)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
class ServiceOrderSafetyTests {

    @Autowired
    private ServiceOrderService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        authenticateAs("usr_admin");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 1【P1】同一 paid 单并发多次 complete(不同幂等键)→ 只 1 次成功,审计只 1 条完成,提成只 1 条。 */
    @Test
    void concurrentCompleteOnlyOneWins() throws Exception {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "并发舞", 30000L, 15, true, "percent", 1000L));
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));

        long auditBefore = completedAuditCount(order.id());
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        try {
            List<Callable<Void>> tasks = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    authenticateAs("usr_admin");
                    try {
                        service.complete(order.id(),
                                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
                        ok.incrementAndGet();
                    } catch (RuntimeException e) {
                        rejected.incrementAndGet();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                });
            }
            for (Future<Void> f : pool.invokeAll(tasks)) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(ok.get()).as("并发 complete 只允许 1 次成功").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(threads - 1);
        assertThat(completedAuditCount(order.id()) - auditBefore)
                .as("完成审计只 1 条").isEqualTo(1);
        assertThat(commissionCount(order.id())).as("提成台账只 1 条").isEqualTo(1);
        cleanupOrder(order.id());
    }

    /** 2【P1】mark-paid 后产生一条公司应收/已收流水(带订单号+金额+确认人),供对账查询。 */
    @Test
    void markPaidCreatesReceivableLedgerRow() {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "对账舞", 12345L, 10, true, "none", 0L));
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));

        // 应收流水:一条 receivable、金额=订单价、source 关联订单号、已收=全额、确认人=paid_by。
        var row = jdbcTemplate.queryForMap(
                "SELECT amount_cent, paid_amount_cent, direction, status FROM ar_ap_items "
                        + "WHERE source_type='service_order' AND source_id=?", order.id());
        assertThat(((Number) row.get("amount_cent")).longValue()).isEqualTo(12345L);
        assertThat(((Number) row.get("paid_amount_cent")).longValue()).isEqualTo(12345L);
        assertThat(row.get("direction")).isEqualTo("receivable");
        // 确认人可从订单 paid_by 对账。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT paid_by FROM service_orders WHERE id=?", String.class, order.id()))
                .isEqualTo("usr_admin");
        cleanupOrder(order.id());
    }

    /** 3【P2】下单后改目录费率,完成时应按下单快照费率分账(非活目录)。 */
    @Test
    void completeUsesOrderTimeFeeSnapshotNotLiveCatalog() {
        String itemId = "svc_snap_" + key();
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), itemId, "快照舞", 30000L, 10, true, "percent", 1000L)); // 10%
        assertThat(item.id()).isEqualTo(itemId);
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        service.markPaid(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderMarkPaidRequest(key(), null));
        // 完成前把目录费率改高到 50%(想压舞者提成)。
        service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), itemId, "快照舞", 30000L, 10, true, "percent", 5000L));
        // 探针:确认活目录费率已被改到 50%(否则本用例不成立)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT mgmt_fee_value FROM service_catalog_items WHERE id=?", Long.class, itemId))
                .as("活目录已改为 50%").isEqualTo(5000L);

        ServiceOrderRecord done = service.complete(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key()));
        // 应按下单时 10% 快照:管理费 3000,舞者 27000(而非改后 50% → 15000/15000)。
        assertThat(done.mgmtFeeCent()).as("按下单快照 10% 分账").isEqualTo(3000L);
        assertThat(done.dancerShareCent()).isEqualTo(27000L);
        cleanupOrder(order.id());
        jdbcTemplate.update("DELETE FROM service_catalog_items WHERE id=?", itemId);
    }

    /** 4【P2】accepted 且 paid_at 为 null 的单 complete 应拒(无收款不得完成)。 */
    @Test
    void acceptedWithoutPaymentCannotComplete() {
        ServiceCatalogItemRecord item = service.upsertCatalogItem(
                new com.complex.platform.serviceorder.dto.ServiceCatalogItemRequest(
                        key(), null, "潜伏舞", 20000L, 10, true, "none", 0L));
        String performer = "usr_perf_" + key();
        ServiceOrderRecord order = service.createOrder(
                new com.complex.platform.serviceorder.dto.ServiceOrderCreateRequest(
                        key(), item.id(), performer, null, null, null));
        // 直接把状态改成 accepted 且 paid_at=null(模拟遗留 accepted 路径)。
        jdbcTemplate.update("UPDATE service_orders SET status='accepted', paid_at=NULL WHERE id=?", order.id());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.complete(order.id(),
                new com.complex.platform.serviceorder.dto.ServiceOrderActionRequest(key())))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("NOT_PAID_YET"));
        // 未产生提成、状态仍非 completed。
        assertThat(commissionCount(order.id())).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM service_orders WHERE id=?", String.class, order.id()))
                .isNotEqualTo("completed");
        cleanupOrder(order.id());
    }

    private long completedAuditCount(String orderId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name='service.order.completed' "
                        + "AND business_id=?", Long.class, orderId);
        return c == null ? 0 : c;
    }

    private long commissionCount(String orderId) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE source_type='service_order' AND source_id=?",
                Long.class, orderId);
        return c == null ? 0 : c;
    }

    private void cleanupOrder(String orderId) {
        // audit_events/staff_commission_ledger/ar_ap_items 均 append-only(触发器禁 DELETE),不清理——
        // 每单 source_id 为唯一 UUID,残留行不影响按 source_id 隔离的断言。
        jdbcTemplate.update("DELETE FROM service_orders WHERE id=?", orderId);
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void authenticateAs(String userId) {
        AuthService.UserRecord user = new AuthService.UserRecord(
                userId, userId, "hash", "platform_admin", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))));
    }
}
