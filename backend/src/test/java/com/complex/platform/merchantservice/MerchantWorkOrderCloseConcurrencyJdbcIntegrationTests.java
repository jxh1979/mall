package com.complex.platform.merchantservice;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.access.AccessActor;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 BI 段第 261 条 261-B17-R1 的两端同时关闭（真库、真实入口、蛇形键、两条线程各自独立事务）。依据：PGM:303「两个操作同时发生冲突时，
 * 一律以“先处理成功”的那个为准」；BSM:1385 商户确认与客户关怀部关闭两端。一张已处理待关闭的投诉单，一端已在事务里把它关闭（商户确认已解决，
 * 满意度 5 分）还没提交，另一端员工用自己的请求编号经真实接口关单：后到的一笔先锁工单行，等先到者提交后读到已关闭，409 WORK_ORDER_CLOSED，
 * 不覆盖先到者写下的满意度与处理结论、不多记一条关闭动作。修复前后到者按旧状态照关，覆盖先到者的结果。
 *
 * <p>本类不开测试事务；夹具按随机后缀建，用例结束逐表删除（留痕只增不删，留在库里）。先到的一端用一条更新语句代替（只为先占住这一行并改成已关闭或已派单）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class MerchantWorkOrderCloseConcurrencyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcMerchantServiceRepository repository;
    @Autowired private PlatformTransactionManager txManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String order = "mswo_b17c_" + s;
    private final String user = "usr_b17c_" + s;
    private final String role = "role_b17c_" + s;
    private final String merchantUser = "usr_b17c_m_" + s;
    private final String merchantRole = "role_b17c_m_" + s;
    private final CountDownLatch release = new CountDownLatch(1);
    private ExecutorService pool;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        repository.saveWorkOrder(new MerchantWorkOrderRecord(order, "mch_b17c_" + s, null, null, "complaint", "high", "resolved",
                "同时关闭回归" + s, "店内天花板漏水", "usr_b17c_reporter_" + s, "merchant_portal", "property", null, null, null,
                "已上门处理", null, List.of(), false, null, now, now));
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', NULL, NULL, 'active', now())", user, user);
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '同时关闭回归', 'project', 'active', now())",
                role, "同时关闭回归角色" + Long.parseLong(s, 16));
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'merchant.service_work_order.close')", role);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", user, role);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'staff', NULL, ?, 'active', now())", merchantUser, merchantUser, "mch_b17c_" + s);
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '商户取消并发回归', 'project', 'active', now())",
                merchantRole, "商户取消并发回归角色" + Long.parseLong(s, 16));
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'merchant.service_work_order.create')", merchantRole);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", merchantUser, merchantRole);
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        jdbc.update("DELETE FROM merchant_service_work_orders WHERE id = ?", order);   // 动作记录随工单级联删除
        jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key IN (?, ?)", "b17c-close-" + s, "b17c-cancel-" + s);
        jdbc.update("DELETE FROM user_roles WHERE user_id IN (?, ?)", user, merchantUser);
        jdbc.update("DELETE FROM role_permissions WHERE role_id IN (?, ?)", role, merchantRole);
        jdbc.update("DELETE FROM roles WHERE id IN (?, ?)", role, merchantRole);
        jdbc.update("DELETE FROM access_users WHERE id IN (?, ?)", user, merchantUser);
    }

    @Test
    void closeArrivingBehindAnotherCloseIsRejectedWithoutOverwriting() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<Object> first = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.update("UPDATE merchant_service_work_orders SET status = 'closed', closed_at = now(), satisfaction_score = 5, updated_at = now()"
                    + " WHERE id = ?", order);
            held.countDown();
            await(release);
            return null;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("先到的一端已在事务内关闭").isTrue();
        Future<MockHttpServletResponse> second = pool.submit(() -> mockMvc.perform(post("/api/v1/merchant-service/work-orders/" + order + "/close")
                .with(webSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"b17c-close-" + s + "\",\"operator_note\":\"关闭商户服务工单\",\"evidence_attachment_ids\":[],"
                        + "\"resolution\":\"客户关怀部关闭\",\"satisfaction_score\":null,\"close_reason\":\"商户多次联系未回复\"}"))
                .andReturn().getResponse());
        awaitLockWaiter();
        release.countDown();
        first.get(30, TimeUnit.SECONDS);

        MockHttpServletResponse response = second.get(30, TimeUnit.SECONDS);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(body).isEqualTo(409);
        assertThat(JsonPath.<String>read(body, "$.detail_code")).isEqualTo("WORK_ORDER_CLOSED");
        Map<String, Object> row = jdbc.queryForMap("SELECT status, satisfaction_score, resolution FROM merchant_service_work_orders WHERE id = ?", order);
        assertThat(row).containsEntry("status", "closed").containsEntry("satisfaction_score", 5).containsEntry("resolution", "已上门处理");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_service_work_order_actions WHERE work_order_id = ?", Integer.class, order)).isZero();
    }

    /**
     * 方案 261-B17-R1 测试④（复核修复项 F18）：员工派单先到、商户取消后到。依据 PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」；
     * 第十八批裁决 27（派单前商户自己取消，派单后由客户关怀部取消）。一张已提交未派单的单，一端已在事务里把它派出（改成已派单）还没提交，
     * 提单商户经真实接口取消后到：取消先锁工单行，等先到者提交后读到已派单，409 INVALID_STATE，库内仍是已派单、不记取消动作。
     * 取消执行体改回不锁的普通读时，后到者按旧的已提交状态照取消，把已派单的单写成已取消。
     */
    @Test
    void merchantCancelArrivingBehindAssignIsRejectedWithoutOverwriting() throws Exception {
        jdbc.update("UPDATE merchant_service_work_orders SET status = 'created', resolution = NULL WHERE id = ?", order);
        CountDownLatch held = new CountDownLatch(1);
        Future<Object> first = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.update("UPDATE merchant_service_work_orders SET status = 'assigned', updated_at = now() WHERE id = ?", order);
            held.countDown();
            await(release);
            return null;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("先到的一端已在事务内派单").isTrue();
        Future<MockHttpServletResponse> second = pool.submit(() -> mockMvc.perform(post("/api/v1/merchant-service/work-orders/" + order + "/cancel")
                .with(webSession(new AccessActor(merchantUser, merchantUser, "staff", null, "mch_b17c_" + s, "active")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"b17c-cancel-" + s + "\",\"reason\":\"误报，问题已自行解决\"}"))
                .andReturn().getResponse());
        awaitLockWaiter();
        release.countDown();
        first.get(30, TimeUnit.SECONDS);

        MockHttpServletResponse response = second.get(30, TimeUnit.SECONDS);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(body).isEqualTo(409);
        assertThat(JsonPath.<String>read(body, "$.detail_code")).isEqualTo("INVALID_STATE");
        assertThat(jdbc.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id = ?", String.class, order)).isEqualTo("assigned");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_service_work_order_actions WHERE work_order_id = ? AND action_type = 'cancelled'",
                Integer.class, order)).isZero();
    }

    private RequestPostProcessor webSession() {
        return webSession(new AccessActor(user, user, "platform_admin", null, null, "active"));
    }

    private RequestPostProcessor webSession(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
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
        throw new AssertionError("未观测到后到者在等待先到者");
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
