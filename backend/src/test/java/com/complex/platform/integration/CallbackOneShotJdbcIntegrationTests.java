package com.complex.platform.integration;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 BN 段第 261 条 261-B23-R1、261-B23-R2：外部回调按幂等键只落一次（真库、真实接口、不开测试事务）。依据：PGM:528「⑤同一笔操作重复提交
 * 不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；PGM:303「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」、行末「同一次操作
 * 重复提交时，返回第一次的结果并提示已完成。」；BSM:1618「第三方回流失败、重复、延迟、差异。」；BSM:1579「机器人到达是被动留痕：由机器人上报的任务
 * 状态自动记录时间和点位。」；第十八批裁决 50。
 *
 * <p>R1：统一 webhook 与开放平台 webhook 同一幂等键重投（换 nonce）或同时投递，信封只落一条、返回首次事件编号并标已去重，不覆盖原事件；
 * 别的幂等键带着已被占用的事件编号 409 DUPLICATE_KEY。R2：机器人上报同一合作方同一幂等键只落一条被动留痕，重投返回首条并提示已完成；
 * 缺幂等键 400。统一 webhook 与机器人上报两个路径免登录（第十八批 CK 段 418-R1），与开放平台 webhook 一样不带后台会话、不带终端头发。</p>
 *
 * <p>第十八批 CK 段第 418 条 418-R1（BSM:1511「先开发自己的系统和开放接口：第三方可以在身份核验后读取或回写平台数据」；BSM:1579「机器人到达是被动留痕：
 * 由机器人上报的任务状态自动记录时间和点位。」）：两个回调入口免后台登录、免终端总开关闸，后台集成管理写接口照旧要登录。第 419 条 419-R1：回执是统一包络，
 * 事件编号与是否去重在 data 下；419-R2：验签失败与随机数重放回 401。</p>
 *
 * <p>签名密钥只用占位测试串（仓库公开）。第三方能力开关用后置回出厂的关；信封、被动留痕与留痕只增不删，夹具按纯数字后缀隔离、留在一次性库。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "complex.open-platform.secret-refs.integration-retail-pos-webhook=" + CallbackOneShotJdbcIntegrationTests.SECRET,
        "complex.open-platform.secret-refs.integration-robot-webhook=" + CallbackOneShotJdbcIntegrationTests.SECRET,
        CallbackOneShotJdbcIntegrationTests.OPEN_SECRET_PROPERTY + "=" + CallbackOneShotJdbcIntegrationTests.SECRET
})
@AutoConfigureMockMvc
class CallbackOneShotJdbcIntegrationTests {

    static final String SECRET = "b86-callback-placeholder-secret";
    static final String OPEN_SECRET_PROPERTY = "b86.callback.test.open-secret";
    private static final List<String> SWITCHES = List.of("integration.retail_pos.sync", "integration.robot.sync");
    private static final ObjectMapper SORTED = new ObjectMapper().findAndRegisterModules()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager txManager;

    private final String s = String.valueOf(System.nanoTime());
    private final String provider = "b86_pv_" + s;
    private final String endpoint = "b86_ep_" + s;
    private final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @BeforeEach
    void seed() {
        for (String capability : SWITCHES) {
            jdbc.update("UPDATE terminal_capability_switches SET enabled = TRUE WHERE capability_code = ? AND scope_type = 'global'", capability);
        }
        jdbc.update("INSERT INTO open_endpoint_configs (id, endpoint_key, provider_code, secret_ref, status, webhook_enabled, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'active', TRUE, now(), now())", "oec_" + endpoint, endpoint, provider, "config:" + OPEN_SECRET_PROPERTY);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        release.countDown();
        pool.shutdownNow();
        pool.awaitTermination(20, TimeUnit.SECONDS);
        for (String capability : SWITCHES) {
            jdbc.update("UPDATE terminal_capability_switches SET enabled = FALSE WHERE capability_code = ? AND scope_type = 'global'", capability);
        }
    }

    // ── 261-B23-R1 ──

    /**
     * 统一 webhook 同一幂等键换 nonce 连投三次（带与不带事件编号各一组）：都 200、返回首次事件编号，第 2、3 次标已去重；信封一行且不被改写，
     * 收到留痕三条（后两条标已去重），没有拒收留痕。修复前红：带事件编号组第 2 次不标已去重（且改写处理时间），不带组第 2 次 409。
     */
    @Test
    void typedWebhookRedeliveredWithTheSameKeyKeepsTheFirstEvent() throws Exception {
        for (boolean withEventId : List.of(true, false)) {
            String key = "b86-typed-seq-" + withEventId + "-" + s;
            Map<String, Object> payload = withEventId ? Map.of("event_id", "evt_b86_seq_" + s, "sku", "A1") : Map.of("sku", "A1");
            Map<String, Object> first = data(ok(typedWebhook(key, payload)));
            assertThat(first).containsEntry("deduplicated", false);
            Map<String, Object> stored = envelope(key);
            for (int attempt = 0; attempt < 2; attempt++) {
                assertThat(data(ok(typedWebhook(key, payload)))).as("第 %d 次重投", attempt + 2)
                        .containsEntry("event_id", first.get("event_id")).containsEntry("deduplicated", true);
            }
            assertThat(envelope(key)).isEqualTo(stored);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM open_api_events WHERE idempotency_key = ?", Long.class, key)).isEqualTo(1);
            assertThat(auditFlags("integration.webhook_received", key)).containsExactly("false", "true", "true");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'integration.webhook_rejected' AND idempotency_key = ?",
                    Long.class, key)).isZero();
        }
    }

    /** 另一幂等键带着已被占用的事件编号：409 DUPLICATE_KEY，原事件一字不改。修复前红：200 且原事件的状态、结果与处理时间被改写。 */
    @Test
    void anotherKeyCarryingATakenEventIdIsRejectedWithoutOverwriting() throws Exception {
        String eventId = "evt_b86_taken_" + s;
        ok(typedWebhook("b86-typed-own-" + s, Map.of("event_id", eventId, "sku", "A1")));
        Map<String, Object> stored = envelopeById(eventId);

        MockHttpServletResponse other = typedWebhook("b86-typed-other-" + s, Map.of("event_id", eventId, "sku", "B2"));
        String body = other.getContentAsString(StandardCharsets.UTF_8);
        assertThat(other.getStatus()).as(body).isEqualTo(409);
        assertThat(JsonPath.<String>read(body, "$.detail_code")).isEqualTo("DUPLICATE_KEY");
        assertThat(envelopeById(eventId)).isEqualTo(stored);
    }

    /** 开放平台 webhook 同一幂等键同时投递两笔（不带事件编号）：后到者等先到者提交后返回首次事件并标已去重，信封一行。修复前红：后到者 409。 */
    @Test
    void openWebhookDeliveredTwiceAtOnceKeepsTheFirstEvent() throws Exception {
        String key = "b86-open-once-" + s;
        List<Map<String, Object>> bodies = deliveredAtOnce(() -> openWebhook(key, Map.of("order_ref", "ord_b86_" + s, "status", "paid")));
        Map<String, Object> first = data(bodies.get(0));
        assertThat(first).containsEntry("deduplicated", false);
        assertThat(data(bodies.get(1))).containsEntry("event_id", first.get("event_id")).containsEntry("deduplicated", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM open_api_events WHERE endpoint_key = ? AND idempotency_key = ?",
                Long.class, endpoint, key)).isEqualTo(1);
    }

    /** 统一 webhook 同一幂等键同时投递两笔（带事件编号）：后到者返回首次事件并标已去重，信封一行且不被改写。修复前红：后到者不标已去重。 */
    @Test
    void typedWebhookDeliveredTwiceAtOnceKeepsTheFirstEvent() throws Exception {
        String key = "b86-typed-once-" + s;
        List<Map<String, Object>> bodies = deliveredAtOnce(() -> typedWebhook(key, Map.of("event_id", "evt_b86_once_" + s, "sku", "A1")));
        Map<String, Object> first = data(bodies.get(0));
        assertThat(first).containsEntry("deduplicated", false);
        assertThat(data(bodies.get(1))).containsEntry("event_id", first.get("event_id")).containsEntry("deduplicated", true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM open_api_events WHERE idempotency_key = ?", Long.class, key)).isEqualTo(1);
    }

    // ── 261-B23-R2 ──

    /**
     * 机器人上报同一幂等键换 nonce 连投三次：都 200、返回首条记录，第 2、3 次提示已完成；被动留痕一行，上报留痕三条（后两条标已去重）。
     * 修复前红：每次落一行、记录编号各不相同、不提示已完成。
     */
    @Test
    void robotReportRedeliveredWithTheSameKeyIsStoredOnce() throws Exception {
        String ref = "robot-task-b86-seq-" + s;
        String key = "b86-robot-seq-" + s;
        Map<String, Object> firstBody = ok(robotReport(ref, key));
        assertThat(firstBody).doesNotContainKey("replayed");
        for (int attempt = 0; attempt < 2; attempt++) {
            Map<String, Object> again = ok(robotReport(ref, key));
            assertThat(again).as("第 %d 次重投", attempt + 2).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
            assertThat(data(again)).containsEntry("id", data(firstBody).get("id"));
        }
        assertThat(robotRows(ref)).isEqualTo(1);
        assertThat(auditFlags("integration.robot_reported", key)).containsExactly("false", "true", "true");
    }

    /** 机器人上报同一幂等键同时投递两笔：后到者返回首条记录并提示已完成，被动留痕一行。修复前红：后到者不等锁直接落第二行、记录编号不同。 */
    @Test
    void robotReportDeliveredTwiceAtOnceIsStoredOnce() throws Exception {
        String ref = "robot-task-b86-once-" + s;
        String key = "b86-robot-once-" + s;
        List<Map<String, Object>> bodies = deliveredAtOnce(() -> robotReport(ref, key));
        assertThat(bodies.get(0)).doesNotContainKey("replayed");
        assertThat(bodies.get(1)).containsEntry("replayed", true).containsEntry("message", ApiResponse.REPLAYED_MESSAGE);
        assertThat(data(bodies.get(1))).containsEntry("id", data(bodies.get(0)).get("id"));
        assertThat(robotRows(ref)).isEqualTo(1);
    }

    /** 机器人上报缺幂等键：400 VALIDATION_FAILED，不落被动留痕，记一条上报失败留痕。修复前红：照常落库 200。 */
    @Test
    void robotReportWithoutKeyIsRejected() throws Exception {
        String ref = "robot-task-b86-nokey-" + s;
        MockHttpServletResponse response = robotReport(ref, null);
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(body).isEqualTo(400);
        assertThat(JsonPath.<String>read(body, "$.detail_code")).isEqualTo("VALIDATION_FAILED");
        assertThat(robotRows(ref)).isZero();
        assertThat(jdbc.queryForList("SELECT metadata->>'error_code' FROM audit_events WHERE event_name = 'integration.robot_report_failed'"
                + " AND business_id = ?", String.class, ref)).containsExactly("VALIDATION_FAILED");
    }

    // ── 第十八批 CK 段 418-R1、419-R1、419-R2 ──

    /**
     * 不带后台会话、不带任何终端头：签名正确的机器人上报与统一 webhook 都 200，回执是统一包络（事件编号与是否去重在 data 下、顶层没有）；
     * 后台集成管理写接口（补偿任务）照旧 401 AUTH_SESSION_INVALID。修复前红：两个回调都 401 AUTH_SESSION_INVALID。
     */
    @Test
    void callbacksNeedNoStaffSessionOrTerminalIdentity() throws Exception {
        String ref = "robot-task-b109-anon-" + s;
        Map<String, Object> robot = ok(robotReport(ref, "b109-robot-anon-" + s));
        assertThat(robot).containsEntry("code", "OK");
        assertThat(data(robot)).containsEntry("robot_task_ref", ref);
        assertThat(robotRows(ref)).isEqualTo(1);

        Map<String, Object> typed = ok(typedWebhook("b109-typed-anon-" + s, Map.of("sku", "A1")));
        assertThat(typed).containsEntry("success", true).containsEntry("code", "OK")
                .doesNotContainKeys("event_id", "deduplicated");
        assertThat(typed.get("request_id")).isNotNull();
        assertThat(typed.get("server_time")).isNotNull();
        assertThat(data(typed)).containsEntry("deduplicated", false);
        assertThat(data(typed).get("event_id")).isNotNull();

        MockHttpServletResponse admin = mockMvc.perform(post("/api/v1/integration/compensation-tasks")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse();
        String adminBody = admin.getContentAsString(StandardCharsets.UTF_8);
        assertThat(admin.getStatus()).as(adminBody).isEqualTo(401);
        assertThat(JsonPath.<String>read(adminBody, "$.detail_code")).isEqualTo("AUTH_SESSION_INVALID");
    }

    /**
     * 不带后台会话：缺签名的机器人上报与统一 webhook 回 401 INTEGRATION_WEBHOOK_SIGNATURE_INVALID；同一随机数再发一笔签名正确的机器人上报回 401
     * INTEGRATION_WEBHOOK_NONCE_REPLAYED，被动留痕不增行。修复前红：都是 401 AUTH_SESSION_INVALID。
     */
    @Test
    void callbacksFailingVerificationReturn401() throws Exception {
        String ref = "robot-task-b109-verify-" + s;
        assertUnauthorized(robotReport(ref, "b109-robot-unsigned-" + s, "n_" + UUID.randomUUID(), false),
                "INTEGRATION_WEBHOOK_SIGNATURE_INVALID");
        assertThat(robotRows(ref)).isZero();

        String nonce = "n_" + UUID.randomUUID();
        ok(robotReport(ref, "b109-robot-nonce-a-" + s, nonce, true));
        assertUnauthorized(robotReport(ref, "b109-robot-nonce-b-" + s, nonce, true), "INTEGRATION_WEBHOOK_NONCE_REPLAYED");
        assertThat(robotRows(ref)).isEqualTo(1);

        assertUnauthorized(typedWebhook("b109-typed-unsigned-" + s, Map.of("sku", "A1"), false),
                "INTEGRATION_WEBHOOK_SIGNATURE_INVALID");
    }

    private static void assertUnauthorized(MockHttpServletResponse response, String detailCode) throws Exception {
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(response.getStatus()).as(body).isEqualTo(401);
        assertThat(JsonPath.<String>read(body, "$.detail_code")).isEqualTo(detailCode);
    }

    // ── 夹具 ──

    /**
     * 先到的一笔在事务里经真实接口投递完成后持住不提交，同键的第二笔随即到达；观测到有会话在锁上等待或第二笔已返回（旧码后到者可能不等锁）
     * 二者先到其一就放行先到者。返回先到、后到两笔的响应体。
     */
    private List<Map<String, Object>> deliveredAtOnce(Callable<MockHttpServletResponse> deliver) throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        Future<MockHttpServletResponse> first = pool.submit(() -> new TransactionTemplate(txManager).execute(status -> {
            MockHttpServletResponse response = call(deliver);
            held.countDown();
            await(release);
            return response;
        }));
        assertThat(held.await(30, TimeUnit.SECONDS)).as("先到的一笔已在事务内投递完成").isTrue();
        Future<MockHttpServletResponse> second = pool.submit(deliver);
        awaitLockWaiterOr(second);
        release.countDown();
        return List.of(ok(first.get(30, TimeUnit.SECONDS)), ok(second.get(30, TimeUnit.SECONDS)));
    }

    /** 统一 webhook（零售 POS），每次新 nonce；幂等键只放请求头。 */
    private MockHttpServletResponse typedWebhook(String key, Map<String, Object> payload) throws Exception {
        return typedWebhook(key, payload, true);
    }

    /** signed 为 false 时不带 X-Signature。 */
    private MockHttpServletResponse typedWebhook(String key, Map<String, Object> payload, boolean signed) throws Exception {
        long ts = System.currentTimeMillis();
        String nonce = "n_" + UUID.randomUUID();
        String body = SORTED.writeValueAsString(payload);
        MockHttpServletRequestBuilder call = post("/api/v1/integration/retail_pos/webhook")
                .header("X-Provider", provider)
                .header("X-Timestamp", String.valueOf(ts)).header("X-Nonce", nonce).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (signed) {
            call.header("X-Signature", sign("retail_pos", "integration.retail_pos.webhook", key, body, ts, nonce));
        }
        return mockMvc.perform(call).andReturn().getResponse();
    }

    /** 开放平台 webhook（公开路径、本用例自建端点），每次新 nonce。 */
    private MockHttpServletResponse openWebhook(String key, Map<String, Object> payload) throws Exception {
        long ts = System.currentTimeMillis();
        String nonce = "n_" + UUID.randomUUID();
        String body = SORTED.writeValueAsString(payload);
        return mockMvc.perform(post("/api/open/d/" + endpoint + "/webhooks/" + provider + "/order.paid")
                .header("X-Webhook-Signature", sign(endpoint, "order.paid", key, body, ts, nonce))
                .header("X-Open-Timestamp", String.valueOf(ts)).header("X-Open-Nonce", nonce).header("X-Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse();
    }

    /** 机器人上报（蛇形键），每次新 nonce；签名覆盖任务引用、事件类型、交接与载荷。key 为 null 时请求体不带幂等键。 */
    private MockHttpServletResponse robotReport(String ref, String key) throws Exception {
        return robotReport(ref, key, "n_" + UUID.randomUUID(), true);
    }

    /** 随机数由调用方给（重放用例）；signed 为 false 时不带 X-Signature。 */
    private MockHttpServletResponse robotReport(String ref, String key, String nonce, boolean signed) throws Exception {
        long ts = System.currentTimeMillis();
        Map<String, Object> signable = new LinkedHashMap<>();
        signable.put("robot_task_ref", ref);
        signable.put("event_type", "delivered");
        signable.put("handover_id", "handover_b86_" + s);
        signable.put("payload", Map.of("floor", "3"));
        Map<String, Object> request = new LinkedHashMap<>(signable);
        if (key != null) {
            request.put("idempotency_key", key);
        }
        MockHttpServletRequestBuilder call = post("/api/v1/integration/robot/report")
                .header("X-Provider", provider)
                .header("X-Timestamp", String.valueOf(ts)).header("X-Nonce", nonce)
                .contentType(MediaType.APPLICATION_JSON).content(SORTED.writeValueAsString(request));
        if (signed) {
            call.header("X-Signature", sign("robot", "integration.robot.report", String.valueOf(key), SORTED.writeValueAsString(signable), ts, nonce));
        }
        return mockMvc.perform(call).andReturn().getResponse();
    }

    /** 与 OpenPlatformCrypto 同算法：规范串换行拼接后用占位测试密钥 HmacSHA256 取十六进制；载荷哈希为按键排序 JSON 的 SHA-256。 */
    private String sign(String endpointKey, String event, String key, String sortedPayload, long ts, String nonce) throws Exception {
        String canonical = "endpoint_key=" + endpointKey + "\nevent=" + event + "\nidempotency_key=" + key + "\nnonce=" + nonce
                + "\npayload_hash=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sortedPayload.getBytes(StandardCharsets.UTF_8)))
                + "\nprovider=" + provider + "\ntimestamp=" + ts;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private Map<String, Object> envelope(String key) {
        return jdbc.queryForMap("SELECT id, idempotency_key, payload_json::text AS payload, event_status, result_json::text AS result, processed_at"
                + " FROM open_api_events WHERE idempotency_key = ?", key);
    }

    private Map<String, Object> envelopeById(String eventId) {
        return jdbc.queryForMap("SELECT id, idempotency_key, payload_json::text AS payload, event_status, result_json::text AS result, processed_at"
                + " FROM open_api_events WHERE id = ?", eventId);
    }

    private List<String> auditFlags(String event, String key) {
        return jdbc.queryForList("SELECT coalesce(metadata->>'deduplicated', 'absent') FROM audit_events WHERE event_name = ? AND idempotency_key = ?"
                + " ORDER BY occurred_at, id", String.class, event, key);
    }

    private long robotRows(String ref) {
        return jdbc.queryForObject("SELECT count(*) FROM integration_robot_task_events WHERE robot_task_ref = ?", Long.class, ref);
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

    private static MockHttpServletResponse call(Callable<MockHttpServletResponse> deliver) {
        try {
            return deliver.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 库里观测到有会话在锁上等待，或后到的一笔已经返回（旧码下后到者可能不等锁），二者先到其一。 */
    private void awaitLockWaiterOr(Future<?> second) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"
                    + " AND wait_event_type = 'Lock'", Integer.class);
            if ((waiting != null && waiting >= 1) || second.isDone()) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new AssertionError("未观测到后到的一笔在等待先到者，也没有返回");
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
