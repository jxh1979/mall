package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.dto.CustomerTicketTransferRequest;
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
 * AUD-077 转赠并发裁决:领取/撤回全落同一行 CAS(UPDATE ... WHERE status='pending'),
 * 数据库行锁天然串行化 → 先提交者赢,后到者 0 行。本类【非事务】,双线程独立提交,末尾清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class TicketTransferConcurrencyJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TicketTransferService transferService;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    void twoConcurrentClaimsExactlyOneWins() throws Exception {
        Ctx c = seed();
        try {
            List<Integer> results = runBoth(
                    () -> repository.claimTicketTransfer(c.transferId, c.receiverA, Instant.now()),
                    () -> repository.claimTicketTransfer(c.transferId, c.receiverB, Instant.now()));
            assertThat(results.stream().filter(r -> r == 1).count()).isEqualTo(1L); // 恰一人成功
            assertThat(repository.findTicketTransferById(c.transferId).orElseThrow().status()).isEqualTo("claimed");
        } finally {
            cleanup(c);
        }
    }

    @Test
    void claimVsRevokeExactlyOneWins() throws Exception {
        Ctx c = seed();
        try {
            List<Integer> results = runBoth(
                    () -> repository.claimTicketTransfer(c.transferId, c.receiverA, Instant.now()),
                    () -> repository.revokeTicketTransfer(c.transferId, c.initiator, Instant.now()));
            assertThat(results.stream().filter(r -> r == 1).count()).isEqualTo(1L); // 恰一方生效
            String finalStatus = repository.findTicketTransferById(c.transferId).orElseThrow().status();
            assertThat(finalStatus).isIn("claimed", "revoked"); // 终态与赢家一致,非 pending
        } finally {
            cleanup(c);
        }
    }

    // ── clients-R40 发起转赠 vs 核销消费:不冻结、先成功者为准,但不允许"已用票 + 在途转赠"并存 ────────────
    // 发起方=真服务(@Transactional + FOR UPDATE 锁定 slot 权益行再检未用再插 pending);
    // 核销方=atomicUseEntitlement(消费成功同事务作废该人份在途转赠)。核销先提交→发起读到已用被拒;发起先提交→核销照常成功并作废它。
    @Test
    void initiateVsConsumeNeverLeavesPendingOnUsedTicket() throws Exception {
        Ctx c = seedForInitiateRace();
        try {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            List<Integer> results = runBoth(
                    () -> {
                        try {
                            transferService.initiate(c.initiator, null, new CustomerTicketTransferRequest(
                                    UUID.randomUUID().toString(), c.parent, null, "link", null));
                            return 1;
                        } catch (ApiException e) {
                            // 复核建议:只放行预期的竞态拒绝码——夹具坏了导致 initiate 恒抛其他错时测试必须红,不许假绿。
                            if (!"TICKET_TRANSFER_SLOT_USED".equals(e.code())) {
                                throw e;
                            }
                            return 0; // 核销先提交,发起被拒
                        }
                    },
                    () -> tx.execute(status -> repository.atomicUseEntitlement(c.entitlementId, Instant.now()).isPresent() ? 1 : 0));
            assertThat(results.get(1)).isEqualTo(1); // 核销从不被在途转赠挡住
            boolean pending = repository.existsPendingTransferForSlot(c.parent, 1);
            Integer usedTimes = jdbcTemplate.queryForObject(
                    "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, c.entitlementId);
            assertThat(pending && usedTimes != null && usedTimes > 0).isFalse(); // 不变式:在途转赠 ⇒ 未核销
        } finally {
            cleanup(c);
        }
    }

    // ── BSM 第 809 行「接收人确认领取……原持有人再次转赠——以先处理成功的那个动作为准，其余动作自动失效」：再次转赠在锁上等到对方领取提交后，
    //    须按锁后的持有人重判——票已归对方，原持有人的再次转赠失效，不得留下一笔可被第三人领走的在途转赠。
    @Test
    void retransferWaitingBehindClaimRechecksHolderAfterLock() throws Exception {
        Ctx c = seedForInitiateRace();
        Instant now = Instant.now();
        String claimCode = "clm_" + UUID.randomUUID().toString().replace("-", "");
        c.transferId = "tkt_transfer_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveTicketTransfer(new TicketTransferRecord(c.transferId, c.parent, 1, c.initiator, "link",
                null, null, null, claimCode, null, "pending", null, now, null, null, null, now));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            java.util.concurrent.CountDownLatch claimHoldsLock = new java.util.concurrent.CountDownLatch(1);
            Future<?> claim = pool.submit(() -> tx.executeWithoutResult(status -> {
                transferService.claim(c.receiverA, null, c.transferId, claimCode);
                claimHoldsLock.countDown();
                try {
                    Thread.sleep(3000); // 领取已改持有人、仍未提交，期间原持有人发起再次转赠
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(claimHoldsLock.await(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            Future<String> retransfer = pool.submit(() -> {
                try {
                    transferService.initiate(c.initiator, null, new CustomerTicketTransferRequest(
                            UUID.randomUUID().toString(), c.parent, null, "link", null));
                    return "created";
                } catch (ApiException e) {
                    return e.code();
                }
            });
            claim.get();
            assertThat(retransfer.get()).isEqualTo("TICKET_TRANSFER_NOT_HOLDER");
            assertThat(repository.existsPendingTransferForSlot(c.parent, 1)).isFalse();
        } finally {
            pool.shutdownNow();
            cleanup(c);
        }
    }

    private List<Integer> runBoth(Callable<Integer> a, Callable<Integer> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Integer> wa = () -> { barrier.await(); return a.call(); };
        Callable<Integer> wb = () -> { barrier.await(); return b.call(); };
        try {
            Future<Integer> fa = pool.submit(wa);
            Future<Integer> fb = pool.submit(wb);
            return List.of(fa.get(), fb.get());
        } finally {
            pool.shutdownNow();
        }
    }

    private Ctx seed() {
        Instant now = Instant.now();
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES ('tt_tr','转赠测试票','fixed','active', now(), now()) ON CONFLICT (id) DO NOTHING");
        Ctx c = new Ctx();
        c.initiator = saveCustomer("138" + rnd(), now);
        c.receiverA = saveCustomer("139" + rnd(), now);
        c.receiverB = saveCustomer("137" + rnd(), now);
        c.parent = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveTicketInstance(new TicketInstanceRecord(
                c.parent, "TNO_" + c.parent, "ord_" + c.parent, "tt_tr", 1, 1, 1,
                c.initiator, c.initiator, "active", null, null, now, now));
        repository.upsertSlotHolder(c.parent, 1, c.initiator, now);
        c.transferId = "tkt_transfer_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveTicketTransfer(new TicketTransferRecord(
                c.transferId, c.parent, 1, c.initiator, "link",
                null, null, null, "clm_" + UUID.randomUUID().toString().replace("-", ""), null,
                "pending", null, now, null, null, null, now));
        return c;
    }

    /** AUD-018 竞态场景:不预置转赠,带 1 条未核销权益(发起与消费竞争同一行)。 */
    private Ctx seedForInitiateRace() {
        Instant now = Instant.now();
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES ('tt_tr','转赠测试票','fixed','active', now(), now()) ON CONFLICT (id) DO NOTHING");
        jdbcTemplate.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at) "
                + "VALUES ('et_tr','转赠测试权益','ticket',1,100,'active', now(), now()) ON CONFLICT (id) DO NOTHING");
        Ctx c = new Ctx();
        c.initiator = saveCustomer("138" + rnd(), now);
        c.receiverA = saveCustomer("139" + rnd(), now);
        c.receiverB = saveCustomer("137" + rnd(), now);
        c.parent = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveTicketInstance(new TicketInstanceRecord(
                c.parent, "TNO_" + c.parent, "ord_" + c.parent, "tt_tr", 1, 1, 1,
                c.initiator, c.initiator, "active", null, null, now, now));
        repository.upsertSlotHolder(c.parent, 1, c.initiator, now);
        // V159 FK：issued_entitlements.order_id 必须指向真实订单行。
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, "
                + "pay_status, issue_status, status, created_at, updated_at) "
                + "VALUES (?, ?, 'tt_tr', 1, 'web', 0, 'paid', 'issued', 'created', now(), now())",
                "ord_" + c.parent, "ONO_" + c.parent.substring(4, 16));
        c.entitlementId = "ent_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                c.entitlementId, "code_" + c.entitlementId, "tt_tr", "ord_" + c.parent, "et_tr", "ticket",
                "m1", null, null, 1, 0, 100L, "active", now.minusSeconds(60), now.plusSeconds(86_400),
                now, now, 1, null, null, null, null, null, c.parent, 1, null, null));
        return c;
    }

    private String saveCustomer(String phone, Instant now) {
        String id = "cust_" + UUID.randomUUID().toString().replace("-", "");
        String mask = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        customerRepository.saveCustomer(new CustomerRecord(id, phone, mask, "active", null, now, now, now, null));
        return id;
    }

    private void cleanup(Ctx c) {
        jdbcTemplate.update("DELETE FROM ticket_transfers WHERE parent_ticket_id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM issued_entitlements WHERE parent_ticket_id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM ticket_slot_holders WHERE parent_ticket_id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM ticket_instances WHERE id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "ord_" + c.parent);
        jdbcTemplate.update("DELETE FROM customers WHERE id IN (?,?,?)", c.initiator, c.receiverA, c.receiverB);
    }

    private static String rnd() {
        return String.valueOf(100_000_00L + (long) (Math.random() * 89_999_99L));
    }

    private static final class Ctx {
        String initiator, receiverA, receiverB, parent, transferId, entitlementId;
    }
}
