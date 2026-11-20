package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 BJ 段第 261 条 261-B18-R1 的两名主管同时派单（真库、真实入口、蛇形键、两条线程各自独立事务）。依据：PGM:303「两个操作同时发生冲突时，
 * 一律以“先处理成功”的那个为准」。一张待派单的设施工单，一端已在事务里把它派给甲还没提交，另一端主管用自己的请求编号经真实接口派给乙：
 * 后到的一笔先锁工单行，等先到者提交后读到已派单，400 INVALID_STATE，处理人仍是甲、不多记派单留痕。修复前派单不锁行，后到者按旧状态照派、
 * 把处理人覆盖成乙。
 *
 * <p>本类不开测试事务；夹具按随机后缀建，用例结束逐表删除（留痕只增不删，留在库里）。先到的一端用一条更新语句代替（只为先占住这一行并改成已派单）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class FacilityWorkOrderAssignConcurrencyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager txManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "b18c_prj_" + s;
    private final String order = "b18c_fwo_" + s;
    private final String user = "b18c_usr_" + s;
    private final String role = "b18c_role_" + s;
    private final CountDownLatch release = new CountDownLatch(1);
    private ExecutorService pool;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','同时派单回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'同时派单回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'facility.work_order.manage')", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'platform_admin','org',NULL,'active',now())",
                user, user);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        jdbc.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source, space_id,
                    reporter_id, reporter_type, response_due_at, handle_due_at, project_id)
                VALUES (?, ?, 'cleaning', 'normal', 'pending_assign', '同时派单回归', 'platform', 'sp_hall',
                    'system', 'system', now() + interval '1 hour', now() + interval '12 hours', ?)""",
                order, "FWO-" + s, project);
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        jdbc.update("DELETE FROM facility_work_orders WHERE id = ?", order);
        jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key = ?", "b18c-assign-" + s);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", user);
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", role);
        jdbc.update("DELETE FROM roles WHERE id = ?", role);
        jdbc.update("DELETE FROM access_users WHERE id = ?", user);
        jdbc.update("DELETE FROM projects WHERE id = ?", project);
    }

    @Test
    void assignArrivingBehindAnotherAssignIsRejectedWithoutOverwriting() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<Object> first = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            jdbc.update("UPDATE facility_work_orders SET status = 'assigned', assignee_id = ?, assigned_at = now(), updated_at = now() WHERE id = ?",
                    "b18c_worker_a_" + s, order);
            held.countDown();
            await(release);
            return null;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("先到的一端已在事务内派单").isTrue();
        Future<MockHttpServletResponse> second = pool.submit(() -> mockMvc.perform(post("/api/v1/facility/work-orders/" + order + "/assign")
                .with(webSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignee_id\":\"b18c_worker_b_" + s + "\",\"idempotency_key\":\"b18c-assign-" + s + "\"}"))
                .andReturn().getResponse());
        awaitLockWaiter();
        release.countDown();
        first.get(30, TimeUnit.SECONDS);

        MockHttpServletResponse response = second.get(30, TimeUnit.SECONDS);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(body).isEqualTo(400);
        assertThat(JsonPath.<String>read(body, "$.detail_code")).isEqualTo("INVALID_STATE");
        Map<String, Object> row = jdbc.queryForMap("SELECT status, assignee_id FROM facility_work_orders WHERE id = ?", order);
        assertThat(row).containsEntry("status", "assigned").containsEntry("assignee_id", "b18c_worker_a_" + s);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'facility.work_order_assigned' AND business_id = ?",
                Integer.class, order)).isZero();
    }

    private RequestPostProcessor webSession() {
        AccessActor actor = new AccessActor(user, user, "platform_admin", "org", null, "active");
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
