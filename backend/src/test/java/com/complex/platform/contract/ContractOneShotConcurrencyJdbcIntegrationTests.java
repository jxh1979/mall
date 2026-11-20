package com.complex.platform.contract;

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
 * 第十八批 BH 段第 261 条 261-B16-R1 的同时提交（真库、真实入口、蛇形键、双线程各自独立事务）。依据：PGM:528「⑤同一笔操作重复提交不重复生效
 * （重复点击、网络重发、渠道重复回调都只算一次）」；PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」。双击时前端沿用同一请求编号：
 * 先到者在事务里停住，必须在库里观测到后到者真的在等待才放行；先到者提交后只有一行，后到者拿到第一次的结果与已完成标记。
 * 修复前后到者不等、各建一条（退租开账两张结算单、着色分类两条）。
 *
 * <p>本类不开测试事务；夹具按随机后缀建，用例结束逐表删除（留痕只增不删，留在库里）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class ContractOneShotConcurrencyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager txManager;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_b16c_" + s;
    private final String user = "usr_b16c_" + s;
    private final String role = "role_b16c_" + s;
    private final String contract = "ctr_b16c_" + s;
    private final String category = "同时提交回归" + s;
    private final CountDownLatch release = new CountDownLatch(1);
    private ExecutorService pool;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','合同同时提交回归项目','active',now(),now())",
                project);
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', ?, NULL, 'active', now())", user, user, project);
        jdbc.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", role, "合同同时提交回归角色" + s);
        for (String permission : List.of("contract.settlement.manage", "contract.space.color.manage")) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, permission);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", user, role);
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,DATE '2026-08-01',DATE '2027-07-31','effective',?,?::jsonb,now(),now())""",
                contract, "mch_b16c_" + s, "subj_b16c_" + s, "subj_b16c_" + s, project, "{\"baseRentCent\":100000}");
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        jdbc.update("DELETE FROM contract_settlements WHERE contract_id = ?", contract);
        jdbc.update("DELETE FROM contracts WHERE id = ?", contract);
        jdbc.update("DELETE FROM space_expiry_color_rules WHERE category_name = ?", category);
        jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "b16c-%-" + s);
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", user);
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", role);
        jdbc.update("DELETE FROM roles WHERE id = ?", role);
        jdbc.update("DELETE FROM access_users WHERE id = ?", user);
        jdbc.update("DELETE FROM projects WHERE id = ?", project);
    }

    @Test
    void doubleClickedSettleOpensOneSettlement() throws Exception {
        String id = race("/api/v1/contracts/" + contract + "/settle",
                "{\"idempotency_key\":\"b16c-stl-" + s + "\",\"trigger_type\":\"move_out\"}");
        assertThat(jdbc.queryForList("SELECT id FROM contract_settlements WHERE contract_id = ?", String.class, contract)).containsExactly(id);
    }

    @Test
    void doubleClickedColorRuleSavesOneRule() throws Exception {
        String id = race("/api/v1/contracts/expiry-color-rules", "{\"idempotency_key\":\"b16c-clr-" + s + "\",\"project_id\":\"" + project
                + "\",\"category_name\":\"" + category + "\",\"color\":\"#ff8800\",\"threshold_days\":30,\"seq\":1}");
        assertThat(jdbc.queryForList("SELECT id FROM space_expiry_color_rules WHERE category_name = ?", String.class, category))
                .containsExactly(id);
    }

    /** 同一请求体两条线程各提交一次，先到者持事务不提交直到观测到后到者在等。返回两次共同的对象编号。 */
    private String race(String url, String body) throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<MockHttpServletResponse> first = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            MockHttpServletResponse response = send(url, body);
            held.countDown();
            await(release);
            return response;
        }));
        assertThat(held.await(20, TimeUnit.SECONDS)).as("先到者已在事务内办完").isTrue();
        Future<MockHttpServletResponse> second = pool.submit(() -> send(url, body));
        awaitLockWaiter();
        release.countDown();

        Map<String, Object> won = json(first.get(30, TimeUnit.SECONDS));
        Map<String, Object> waited = json(second.get(30, TimeUnit.SECONDS));
        assertThat(won.get("replayed")).as("先到者是第一次办理").isNull();
        assertThat(waited.get("replayed")).as("后到者拿到已完成标记").isEqualTo(true);
        String id = JsonPath.read(won, "$.data.id");
        assertThat(JsonPath.<String>read(waited, "$.data.id")).isEqualTo(id);
        return id;
    }

    private MockHttpServletResponse send(String url, String body) {
        try {
            return mockMvc.perform(post(url).with(webSession()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Map<String, Object> json(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).as(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        return JsonPath.read(response.getContentAsString(StandardCharsets.UTF_8), "$");
    }

    private RequestPostProcessor webSession() {
        AccessActor actor = new AccessActor(user, user, "platform_admin", project, null, "active");
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
