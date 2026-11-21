package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.TemporaryGrant;
import com.complex.platform.common.api.ApiResponse;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 CA 段第 392 条 392-R1：储值退卡同一编号两笔同时到达（柜台双击「确认柜台退卡」、超时后同编号重发而第一笔仍在处理）——
 * 后到者等先到者提交后返回第一次的退款流水并提示已完成，钱只退一次，不再报已销户、余额不足或重复记录。依据：PGM:303 末句
 * 「同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；
 * 第十八批裁决 50。
 *
 * <p>真库、真实接口、蛇形键、不开测试事务（并发两笔要各自真实提交）。持锁方只用 SQL、在独立事务里锁住卡行，库里观测到两笔请求都在锁上排队后再放行。
 * 储值流水只追加不删（V140），客人、卡与流水按纯数字后缀隔离留在一次性库；员工及其授权、线上退款登记的财务退款待办在用例后删除。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class StoredValueRefundSameKeyConcurrencyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository accessRepository;
    @Autowired private CustomerRepository customers;
    @Autowired private PlatformTransactionManager txManager;

    private final String suffix = String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
    private final String projectId = "org_b99sv_" + suffix;
    private final String merchantId = "mch_b99sv_" + suffix;
    private final String customerId = "cus_b99sv_" + suffix;
    private final String staffId = "usr_b99sv_" + suffix;
    private final String staffName = "储值退卡并发员工" + suffix;
    private final String key = "b99-refund-" + suffix;
    private final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private String staffMerchant;
    private String cardId;

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        jdbc.update("DELETE FROM finance_refunds WHERE target_id = ?", cardId);
        jdbc.update("DELETE FROM temporary_permission_grants WHERE subject_id = ?", staffId);
        jdbc.update("DELETE FROM access_users WHERE id = ?", staffId);
    }

    /**
     * ① 柜台退款：商户账号对本商户卡双击确认（部分退、不销卡），两笔同一请求内容同一编号。
     * 修复前后到者在卡行锁上等先到者提交后照常往下走，写流水撞 uq_csvt_idempotency → 409 STATE_CONFLICT，页面显示退卡失败。
     */
    @Test
    void aDoubleClickedCounterRefundReturnsTheFirstTransactionToTheLaterOne() throws Exception {
        seed(merchantId, 8000L, "merchant_prepaid");
        String body = """
                {"idempotency_key": "%s", "project_id": "%s", "merchant_id": "%s", "amount_cent": 2000, "reason": "客人到店退卡",
                 "close_account": false, "redemption_voucher_no": "VCH-B99-%s"}
                """.formatted(key, projectId, merchantId, suffix);

        String firstId = concurrentlyTwiceExpectOneReplay("/api/v1/loyalty/stored-value/" + customerId + "/counter-refund", body);

        assertThat(refundRows("svcr_" + key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM customer_stored_value_transactions WHERE idempotency_key = ?",
                String.class, "svcr_" + key)).isEqualTo(firstId);
        assertThat(principal()).isEqualTo(6000L);
        assertThat(failures("stored_value.counter_refund_failed")).isZero();
    }

    /**
     * ② 线上退款：不挂商户的项目侧账号对本项目通用卡退全部本金并销卡，同一编号两笔同时到达。
     * 修复前后到者拿到锁时读到已销户的卡 → 409 STORED_VALUE_ACCOUNT_CLOSED。
     */
    @Test
    void aSameKeyGeneralCardRefundArrivingWhileTheFirstIsRunningGetsTheFirstTransaction() throws Exception {
        seed(null, 5000L, null);
        String body = """
                {"idempotency_key": "%s", "project_id": "%s", "amount_cent": 5000, "reason": "客人到店退卡", "close_account": true}
                """.formatted(key, projectId);

        String firstId = concurrentlyTwiceExpectOneReplay("/api/v1/loyalty/stored-value/" + customerId + "/refund", body);

        assertThat(refundRows(key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM customer_stored_value_transactions WHERE idempotency_key = ?",
                String.class, key)).isEqualTo(firstId);
        assertThat(jdbc.queryForObject("SELECT status FROM customer_stored_value_accounts WHERE id = ?", String.class, cardId))
                .isEqualTo("closed");
        assertThat(principal()).isZero();
        assertThat(failures("stored_value.refund_failed")).isZero();
    }

    /**
     * 持锁方锁住卡行，两笔同一请求依次排上队后放行：两笔都 200，恰一笔不带 replayed（第一次执行），另一笔 replayed=true 并提示已完成，
     * 两笔返回同一条退款流水。返回该流水号。
     */
    private String concurrentlyTwiceExpectOneReplay(String path, String body) throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<Object> holder = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.queryForList("SELECT id FROM customer_stored_value_accounts WHERE id = ? FOR UPDATE", cardId);
            held.countDown();
            await(release);
            return null;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("持锁方已锁住卡行").isTrue();
        Future<MockHttpServletResponse> a = pool.submit(() -> post(path, body));
        awaitLockWaiters(1);
        Future<MockHttpServletResponse> b = pool.submit(() -> post(path, body));
        awaitLockWaiters(2);
        release.countDown();
        holder.get(30, TimeUnit.SECONDS);

        Map<String, Object> first = ok(a.get(30, TimeUnit.SECONDS));
        Map<String, Object> second = ok(b.get(30, TimeUnit.SECONDS));
        List<Map<String, Object>> executed = List.of(first, second).stream().filter(r -> !r.containsKey("replayed")).toList();
        List<Map<String, Object>> replayed = List.of(first, second).stream().filter(r -> r.containsKey("replayed")).toList();
        assertThat(executed).as("恰一笔是第一次执行").hasSize(1);
        assertThat(replayed).as("另一笔是重复提交").hasSize(1);
        assertThat(replayed.get(0)).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
        String firstId = (String) data(executed.get(0)).get("id");
        assertThat(data(replayed.get(0))).containsEntry("id", firstId).containsEntry("txn_type", "refund");
        return firstId;
    }

    /** 员工（持储值退款权限；merchant 为空＝不挂商户的项目侧账号）、客人与一张本项目的卡（merchant 为空＝通用卡）。 */
    private void seed(String merchant, long principalCent, String fundOwnership) {
        staffMerchant = merchant;
        Instant now = Instant.now();
        accessRepository.createUser(new AccessUser(staffId, staffName, "staff", projectId, merchant, "active", now));
        accessRepository.createTemporaryGrant(new TemporaryGrant(UUID.randomUUID().toString(), staffId,
                List.of("customer.stored_value.refund"), now.minusSeconds(3600), now.plusSeconds(86_400), false, false,
                "active", "储值退卡并发回归" + suffix, "seed-admin", now, null, null));
        String phone = "135" + suffix.substring(2);
        customers.saveCustomer(new CustomerRecord(customerId, phone, "135****" + phone.substring(7), "active", null, null, now, now));
        cardId = "svacc_b99sv_" + suffix;
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(cardId, customerId, projectId, merchant,
                principalCent, "active", now, now, fundOwnership, principalCent, 0L));
    }

    private MockHttpServletResponse post(String path, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .with(request -> {
                    request.addHeader("X-Terminal-Type", "web");
                    return authentication(new UsernamePasswordAuthenticationToken(
                            new AccessActor(staffId, staffName, "staff", projectId, staffMerchant, "active"), null,
                            List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
                })
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse();
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

    private int refundRows(String txnKey) {
        return jdbc.queryForObject("SELECT count(*) FROM customer_stored_value_transactions WHERE idempotency_key = ? AND txn_type = 'refund'",
                Integer.class, txnKey);
    }

    private long principal() {
        return jdbc.queryForObject("SELECT principal_cent FROM customer_stored_value_accounts WHERE id = ?", Long.class, cardId);
    }

    /** 控制器失败留痕记的是请求原键（柜台退款不带 svcr_ 前缀）。 */
    private int failures(String eventName) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND idempotency_key = ?",
                Integer.class, eventName, key);
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
