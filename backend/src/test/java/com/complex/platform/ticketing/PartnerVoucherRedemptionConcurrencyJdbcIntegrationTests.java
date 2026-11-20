package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 BN 段第 261 条 261-B08-R1：第三方券核销回流同一外部核销单号同时导入两笔（真库、真实接口、蛇形键、两条线程各自独立事务）。
 * 依据：PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」、行末「同一次操作重复提交时，返回第一次的结果并提示已完成。」；
 * PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；第十八批裁决 50。
 * 先到的一笔在事务里经真实接口导入完成后持住不提交，后到的一笔（另一个请求编号）同券同单号导入：等先到者提交后返回首次记录并提示已完成，
 * 只扣一次、回流与应付各一笔。修复前后到者在权益行上等锁，放行后单次券 409 ENTITLEMENT_USED_UP、多次券撞唯一约束 409 DUPLICATE_KEY。
 *
 * <p>本类不开测试事务；夹具按纯数字后缀隔离、留在一次性库（应付被只增不删的来源表引用，留痕只增不删）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class PartnerVoucherRedemptionConcurrencyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private PlatformTransactionManager txManager;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "pvc_prj_" + s;
    private final String partner = "pvc_cat_" + s;
    private final String template = "et_pvc_" + s;
    private final String ticketType = "tt_pvc_" + s;
    private final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                project, "券回流并发综合体" + s);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'catering','active',?,now(),now())",
                partner, "回流并发餐厅" + s, project);
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, merchant_id, created_at, updated_at)"
                + " VALUES (?, ?, 'voucher', 2, 0, 'active', ?, now(), now())", template, "通票含餐厅套餐券" + s, partner);
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, ?, 'fixed', 'active', now(), now())", ticketType, "含餐通票" + s);
        staff = new StaffRequestFixture(jdbc).staff("usr_pvc_" + s, "staff", project, null, "ticket.entitlement.manage", "verification.execute");
        MockHttpServletResponse config = mockMvc.perform(post("/api/v1/ticketing/partner-vouchers/configs").with(staff)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pvc-cfg-" + UUID.randomUUID(), "entitlement_template_id", template,
                        "settlement_mode", "internal_purchased", "partner_kind", "merchant", "partner_id", partner, "internal_price_cent", 2000))))
                .andReturn().getResponse();
        assertThat(config.getStatus()).as(config.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
    }

    @Test
    void sameReferenceArrivingTogetherOnASingleUseVoucherIsImportedOnce() throws Exception {
        importedOnceWhenArrivingTogether(1);
    }

    @Test
    void sameReferenceArrivingTogetherOnAMultiUseVoucherIsImportedOnce() throws Exception {
        importedOnceWhenArrivingTogether(2);
    }

    private void importedOnceWhenArrivingTogether(int totalTimes) throws Exception {
        String code = issue(totalTimes);
        String ref = "POS-PVC-" + totalTimes + "-" + s;
        CountDownLatch held = new CountDownLatch(1);
        Future<MockHttpServletResponse> first = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            MockHttpServletResponse response = importRedemption(code, ref);
            held.countDown();
            await(release);
            return response;
        }));
        assertThat(held.await(30, TimeUnit.SECONDS)).as("先到的一笔已在事务内导入完成").isTrue();
        Future<MockHttpServletResponse> second = pool.submit(() -> importRedemption(code, ref));
        awaitLockWaiter();
        release.countDown();

        Map<String, Object> firstBody = ok(first.get(30, TimeUnit.SECONDS));
        Map<String, Object> secondBody = ok(second.get(30, TimeUnit.SECONDS));
        assertThat(firstBody).doesNotContainKey("replayed");
        assertThat(secondBody).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
        assertThat(data(secondBody)).containsEntry("id", data(firstBody).get("id"));
        assertThat(jdbc.queryForObject("SELECT used_times FROM issued_entitlements WHERE entitlement_code=?", Integer.class, code)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM partner_voucher_redemptions WHERE partner_id=? AND external_redemption_ref=?",
                Long.class, partner, ref)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type='partner_voucher' AND counterparty_id=?",
                Long.class, partner)).isEqualTo(1);
    }

    /** 在本项目的已付订单下发一张合作方券（次数按参数），返回券码。 */
    private String issue(int totalTimes) {
        String order = "order_pvc_" + totalTimes + "_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, project_id) VALUES (?, ?, ?, 1, 'web', 0, 'paid', 'issued', 'created', now(), now(), ?)",
                order, "ONO_PVC_" + totalTimes + "_" + s, ticketType, project);
        String code = "PV-PVC-" + totalTimes + "-" + s;
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,"
                + " total_times, used_times, status, valid_from, valid_to, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 'voucher', ?, 0, 'active', now() - interval '1 day', now() + interval '30 days', now(), now())",
                "ie_pvc_" + totalTimes + "_" + s, ticketType, order, template, code, totalTimes);
        return code;
    }

    /** 票务策略页「核销回流导入」的请求形状：每次提交各带自己的请求编号。 */
    private MockHttpServletResponse importRedemption(String code, String ref) {
        try {
            return mockMvc.perform(post("/api/v1/ticketing/partner-vouchers/redemptions").with(staff).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("idempotency_key", "pvc-" + UUID.randomUUID(), "entitlement_code", code,
                            "external_redemption_ref", ref, "source", "reconciliation_file")))).andReturn().getResponse();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

    /** 必须在库里实际观测到有会话在锁上等待，才算后到的一笔已排上队。 */
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
        throw new AssertionError("未观测到后到的一笔在等待先到者");
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
