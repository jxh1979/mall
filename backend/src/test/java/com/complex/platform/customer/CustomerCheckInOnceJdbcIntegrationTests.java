package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 BN 段第 261 条 261-B13-R1：客人签到带请求编号走幂等层（真库、真实接口、蛇形键、不开测试事务）。依据：PGM:303「同一个操作被重复提交，
 * 系统只认一次，不会因为重试就重复扣款、重复入账或重复核销」、行末「同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作
 * 重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；BSM:830「客人每日签到记连续天数，按连续天数阶梯奖励积分。」；第十八批裁决 50。
 *
 * <p>造数：一条全局签到阶梯（用例后删除——全局阶梯留库会改变别的用例的签到积分）；夹具客人的积分账户余额 100、昨天已签、连签 2。
 * 其余夹具按随机后缀隔离、留在一次性库。所得积分以首次响应为准（库里别的用例可能留有更高档的全局阶梯）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class CustomerCheckInOnceJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CustomerRepository customers;
    @Autowired private PlatformTransactionManager txManager;

    private final String tier = "ccit_b86_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private CustomerTicketPaymentFixture f;
    private String key;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO customer_check_in_tiers (id, project_id, streak_days, award_points, status)"
                + " VALUES (?, NULL, 1, 7, 'active')", tier);
        f = new CustomerTicketPaymentFixture(mockMvc, jdbc, json, customers, 20_000L);
        key = "b86-checkin-" + f.suffix;
        Instant now = Instant.now();
        customers.savePoints(new CustomerPointsAccountRecord("points_" + f.customerId, f.customerId, 100L, "base", Map.of(),
                0L, "active", now, 0L, null, LocalDate.now(LocalZone.ID).minusDays(1), 2, now, now));
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        jdbc.update("DELETE FROM customer_check_in_tiers WHERE id = ?", tier);
    }

    /** 同一编号签到后再提交两次：都返回第一次的签到结果并提示已完成，只加一次积分；换新编号再签仍按「今天已签到」拒绝。 */
    @Test
    void sameKeyResubmitReturnsTheFirstCheckInAndAwardsOnce() throws Exception {
        Map<String, Object> firstBody = ok(checkIn(key));
        assertThat(firstBody).doesNotContainKey("replayed");
        Map<String, Object> first = data(firstBody);
        assertThat(first).containsEntry("streak", 3);
        long awarded = ((Number) first.get("awarded_points")).longValue();
        assertThat(awarded).isPositive();
        for (int attempt = 0; attempt < 2; attempt++) {
            Map<String, Object> again = ok(checkIn(key));
            assertThat(again).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
            assertThat(data(again)).containsEntry("id", first.get("id")).containsEntry("streak", 3)
                    .containsEntry("awarded_points", first.get("awarded_points"));
        }
        assertAwardedOnce(awarded);

        MockHttpServletResponse other = checkIn("b86-checkin-other-" + f.suffix);
        String otherBody = other.getContentAsString(StandardCharsets.UTF_8);
        assertThat(other.getStatus()).as(otherBody).isEqualTo(409);
        assertThat(JsonPath.<String>read(otherBody, "$.detail_code")).isEqualTo("ALREADY_CHECKED_IN");
        assertAwardedOnce(awarded);
    }

    /**
     * 网络重发：第一笔服务端还在处理（卡在积分账户锁上）时同一编号又到一笔。后到者等先到者提交后返回第一次的结果并提示已完成，
     * 只加一次积分、连签只算一次。持锁方只用 SQL、在独立事务里锁住积分账户行。
     */
    @Test
    void sameKeyArrivingWhileTheFirstIsStillRunningGetsTheFirstResult() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<Object> holder = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.queryForList("SELECT id FROM customer_points_accounts WHERE customer_id = ? FOR UPDATE", f.customerId);
            held.countDown();
            await(release);
            return null;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("持锁方已锁住积分账户").isTrue();
        Future<MockHttpServletResponse> a = pool.submit(() -> checkIn(key));
        awaitLockWaiters(1);
        Future<MockHttpServletResponse> b = pool.submit(() -> checkIn(key));
        awaitLockWaiters(2);
        release.countDown();
        holder.get(30, TimeUnit.SECONDS);

        Map<String, Object> firstBody = ok(a.get(30, TimeUnit.SECONDS));
        Map<String, Object> secondBody = ok(b.get(30, TimeUnit.SECONDS));
        assertThat(firstBody).doesNotContainKey("replayed");
        assertThat(secondBody).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
        Map<String, Object> first = data(firstBody);
        assertThat(data(secondBody)).containsEntry("id", first.get("id"));
        long awarded = ((Number) first.get("awarded_points")).longValue();
        assertThat(awarded).isPositive();
        assertAwardedOnce(awarded);
    }

    private MockHttpServletResponse checkIn(String idempotencyKey) throws Exception {
        return f.customerPost("/api/v1/customer/check-ins", Map.of("idempotency_key", idempotencyKey)).andReturn().getResponse();
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

    /** 余额只加一次、连签 3、该客人签到 1 行、签到积分流水 1 条。 */
    private void assertAwardedOnce(long awarded) {
        Map<String, Object> account = jdbc.queryForMap(
                "SELECT balance, check_in_streak FROM customer_points_accounts WHERE customer_id = ?", f.customerId);
        assertThat(((Number) account.get("balance")).longValue()).isEqualTo(100L + awarded);
        assertThat(((Number) account.get("check_in_streak")).intValue()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_check_ins WHERE customer_id = ?",
                Integer.class, f.customerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_points_ledger WHERE customer_id = ? AND source_type = 'check_in'",
                Integer.class, f.customerId)).isEqualTo(1);
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
