package com.complex.platform.voucher;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 CA 段第 393 条 393-R1：发起人对已撤回的转赠再撤回（重复点击、网络重发，含同时到达）——返回该转赠并提示已完成，
 * 不再留痕 voucher_transfer.revoked。依据：PGM:303 末句「同一次操作重复提交时，返回第一次的结果并提示已完成。」；
 * PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」。
 *
 * <p>真库、真实接口，请求照客人端实发（X-Customer-Token、Idempotency-Key 头与请求体 idempotency_key 同值）；不开测试事务（并发两笔要各自真实提交）。
 * 客人、券与转赠按纯数字后缀隔离留在一次性库。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class VoucherTransferRevokeReplayJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private VoucherRepository vouchers;
    @Autowired private JdbcVoucherTransferRepository transfers;
    @Autowired private PlatformTransactionManager txManager;

    private final String suffix = String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
    private final String customerId = "cus_b99vt_" + suffix;
    private final String token = "cust_token_b99vt_" + suffix;
    private final String transferId = "vt_b99_" + suffix;
    private final String key = "b99-revoke-" + suffix;
    private final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService pool = Executors.newFixedThreadPool(3);

    /** 发起人名下一张已发放的券和它的一笔在途转赠（链接渠道）。 */
    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        String phone = "136" + suffix.substring(2);
        customers.saveCustomer(new CustomerRecord(customerId, phone, "136****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_b99vt_" + suffix, customerId, token, "miniapp", now,
                now.plusSeconds(3600), "active"));
        String definitionId = "vdef_b99vt_" + suffix;
        vouchers.insertDefinition(new VoucherDefinitionRecord(definitionId, "转赠撤回回归折扣券" + suffix, "merchant_discount",
                "in_store_staff", "none", "mch_b99vt_" + suffix, 5000L, null, null, null, null, 30, null, "active", true, now, now));
        String instanceId = "vi_b99vt_" + suffix;
        assertThat(vouchers.insertInstance(new VoucherInstanceRecord(instanceId, definitionId, customerId, "VC" + instanceId,
                "marketing_grant", "vgrant_b99vt_" + suffix, "issued", now, now.plusSeconds(86_400L * 30),
                null, null, null, false, now, now))).isTrue();
        transfers.save(new VoucherTransferRecord(transferId, instanceId, customerId, "link", null, null, null,
                "vclm_b99vt_" + suffix, null, "pending", null, now, null, null, null, now));
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
    }

    /** ① 同一编号连撤两次：第一次照常撤回、不带 replayed；第二次返回该转赠并提示已完成（修复前第二次与第一次一样不带标记）。 */
    @Test
    void revokingAnAlreadyRevokedTransferAgainReturnsItAndSaysItIsAlreadyCompleted() throws Exception {
        Map<String, Object> first = ok(revoke());
        assertThat(first).doesNotContainKey("replayed");
        assertThat(data(first)).containsEntry("id", transferId).containsEntry("status", "revoked");

        Map<String, Object> again = ok(revoke());
        assertThat(again).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
        assertThat(data(again)).containsEntry("id", transferId).containsEntry("status", "revoked");
        assertThat(revokedEvents()).isEqualTo(1);
    }

    /**
     * ② 同一编号两笔同时到达：两笔都已读到在途、都卡在转赠行的条件更新上，后到者更新 0 行、重读为已撤回。
     * 两笔都 200，恰一笔 replayed=true 并提示已完成，只留痕一次（修复前两笔都不带标记）。持锁方只用 SQL、在独立事务里锁住转赠行。
     */
    @Test
    void aSameKeyRevokeArrivingWhileTheFirstIsRunningSaysItIsAlreadyCompleted() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<Object> holder = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.queryForList("SELECT id FROM voucher_transfers WHERE id = ? FOR UPDATE", transferId);
            held.countDown();
            await(release);
            return null;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("持锁方已锁住转赠行").isTrue();
        Future<MockHttpServletResponse> a = pool.submit(this::revoke);
        awaitLockWaiters(1);
        Future<MockHttpServletResponse> b = pool.submit(this::revoke);
        awaitLockWaiters(2);
        release.countDown();
        holder.get(30, TimeUnit.SECONDS);

        List<Map<String, Object>> both = List.of(ok(a.get(30, TimeUnit.SECONDS)), ok(b.get(30, TimeUnit.SECONDS)));
        assertThat(both.stream().filter(r -> !r.containsKey("replayed")).count()).as("恰一笔是第一次执行").isEqualTo(1);
        List<Map<String, Object>> replayed = both.stream().filter(r -> r.containsKey("replayed")).toList();
        assertThat(replayed).as("另一笔是重复提交").hasSize(1);
        assertThat(replayed.get(0)).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
        both.forEach(r -> assertThat(data(r)).containsEntry("id", transferId).containsEntry("status", "revoked"));
        assertThat(revokedEvents()).isEqualTo(1);
    }

    private MockHttpServletResponse revoke() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/customer/voucher-transfers/" + transferId + "/revoke")
                .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("{\"idempotency_key\": \"" + key + "\"}"))
                .andReturn().getResponse();
    }

    private static Map<String, Object> ok(MockHttpServletResponse response) throws Exception {
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(body).isEqualTo(200);
        return JsonPath.read(body, "$");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }

    private int revokedEvents() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'voucher_transfer.revoked' AND business_id = ?",
                Integer.class, transferId);
    }

    /** 必须在库里实际观测到有这么多会话在锁上等待，才算对应的一笔已排上队。 */
    private void awaitLockWaiters(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"
                    + " AND wait_event_type = 'Lock'", Integer.class);
            if (waiting != null && waiting >= expected) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new AssertionError("未观测到 " + expected + " 个会话在锁上等待");
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
