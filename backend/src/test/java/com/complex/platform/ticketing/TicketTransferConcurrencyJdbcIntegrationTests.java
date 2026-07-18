package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
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

/**
 * AUD-077 转赠并发裁决:领取/撤回全落同一行 CAS(UPDATE ... WHERE status='pending'),
 * 数据库行锁天然串行化 → 先提交者赢,后到者 0 行。本类【非事务】,双线程独立提交,末尾清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
class TicketTransferConcurrencyJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

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

    private String saveCustomer(String phone, Instant now) {
        String id = "cust_" + UUID.randomUUID().toString().replace("-", "");
        String mask = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        customerRepository.saveCustomer(new CustomerRecord(id, phone, mask, "active", null, now, now, now, null));
        return id;
    }

    private void cleanup(Ctx c) {
        jdbcTemplate.update("DELETE FROM ticket_transfers WHERE parent_ticket_id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM ticket_slot_holders WHERE parent_ticket_id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM ticket_instances WHERE id = ?", c.parent);
        jdbcTemplate.update("DELETE FROM customers WHERE id IN (?,?,?)", c.initiator, c.receiverA, c.receiverB);
    }

    private static String rnd() {
        return String.valueOf(100_000_00L + (long) (Math.random() * 89_999_99L));
    }

    private static final class Ctx {
        String initiator, receiverA, receiverB, parent, transferId;
    }
}
