package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 I 段第 85 条 85-R1、85-R2 交接链规则真库回归（BSM 第 1201 行「酒水与可追责物品的交接，每一步都必须扫码——库管出库、后厨接收、服务员取走、
 * 送达客人或卡座，缺任何一步即视为该环节工作未完成，不得进入下一环节；扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）并经主管复核后
 * 方生效」；第 1579 行「机器人到达是被动留痕」；第 1167 行「机器人或人工配送」）：四个人工扫码段按序放行，机器人配送段不计入完成（机器人开关出厂关，
 * 本类不打开）。建链、扫码、凭证上传、补录与复核都按前端蛇形键走真实入口与真实授权链；复核走真实组织层级（同商户的录入员 → 主管 → 经理）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TraceHandoverChainRulesJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_thr_" + suffix;
    private final String merchant = "mch_thr_" + suffix;
    private RequestPostProcessor clerk;
    private RequestPostProcessor mid;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "交接规则项目 " + suffix);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", merchant, "交接规则商户 " + suffix, project);
        String role = "role_thr_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'交接规则回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'inventory.handover.scan','inventory.handover.review','evidence.attachment.create',"
                + "'governance.supervisor_review.view','governance.supervisor_review.approve')", role);
        clerk = account("clerk", role);
        mid = account("mid", role);
        account("boss", role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("clerk"), user("mid"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mid"), user("boss"));
    }

    /**
     * 扫完库管出库，表头指向后厨接收；跳过后厨接收直接扫服务员取走 409、该段仍待扫码；按序扫完四个人工段即完成，
     * 机器人配送段仍待上报，过程实例收口，完成审计恰好一条。
     */
    @Test
    void staffSegmentsAreReleasedInOrderAndTheChainCompletesWithoutTheRobotSegment() throws Exception {
        String id = createChain();
        scan(id, "out_verify", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.current_segment").value("kitchen_receive"));

        scan(id, "staff_pickup", "early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HANDOVER_PREVIOUS_SEGMENT_INCOMPLETE"))
                .andExpect(jsonPath("$.message").value("上一环节（后厨接收）尚未扫码或补录未经复核通过，不得进入下一环节"));
        Map<String, Object> skipped = stepRow(id, "staff_pickup");
        assertThat(skipped.get("step_status")).isEqualTo("pending");
        assertThat(skipped.get("scan_at")).isNull();

        scan(id, "kitchen_receive", "1").andExpect(status().isOk());
        scan(id, "staff_pickup", "1").andExpect(status().isOk());
        scan(id, "deliver_customer", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.overall_status").value("completed"));
        assertThat(stepRow(id, "dispatch").get("step_status")).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT status FROM business_process_instances WHERE id=?", String.class, id)).isEqualTo("completed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='inventory.handover_completed' AND business_id=?",
                Integer.class, id)).isEqualTo(1);
    }

    /** 后厨接收缺扫码、补录待复核期间扫服务员取走仍 409；上级经复核接口批准后再扫 200。 */
    @Test
    void aPendingBackfillDoesNotReleaseTheNextSegmentUntilApproved() throws Exception {
        String id = createChain();
        scan(id, "out_verify", "1").andExpect(status().isOk());
        String submitted = backfill(id, "kitchen_receive", upload())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        scan(id, "staff_pickup", "pending").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HANDOVER_PREVIOUS_SEGMENT_INCOMPLETE"));

        approve(JsonPath.read(submitted, "$.data.review_id")).andExpect(status().isOk());
        scan(id, "staff_pickup", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.current_segment").value("deliver_customer"));
    }

    /**
     * 85-R2（BSM 第 1201 行「补录记录与原始扫码记录并列展示、互不覆盖」「交接几次就记几条，全部保留」；第 1581 行「机器人上报与人工扫码出现缺口、
     * 重复或差异时进入留痕与异常复核」）：已扫码的后厨接收再扫一次 200，原扫码行的状态、扫码人、时刻都不变；另记一条待处理的重复异常（原因中文），
     * 两次扫码的执行审计各一条；扫完其余两段表头仍是异常，经异常复核接口通过后整链完成。
     */
    @Test
    void aDuplicateScanKeepsTheOriginalScanAndTheChainCompletesOnceTheExceptionIsReviewed() throws Exception {
        String id = createChain();
        scan(id, "out_verify", "1").andExpect(status().isOk());
        scan(id, "kitchen_receive", "1").andExpect(status().isOk());
        String scanned = "SELECT step_status, operator_user_id, scan_at FROM inventory_trace_handover_steps WHERE handover_id=? AND segment='kitchen_receive'";
        Map<String, Object> original = jdbc.queryForMap(scanned, id);

        scan(id, "kitchen_receive", "dup").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.overall_status").value("exception"));
        assertThat(jdbc.queryForMap(scanned, id)).isEqualTo(original);
        assertThat(original.get("step_status")).isEqualTo("done");
        Map<String, Object> exception = jdbc.queryForMap(
                "SELECT id, segment, exception_type, review_status, reason FROM inventory_trace_handover_exceptions WHERE handover_id=?", id);
        assertThat(exception).containsEntry("segment", "kitchen_receive").containsEntry("exception_type", "duplicate")
                .containsEntry("review_status", "pending").containsEntry("reason", "同段重复扫码：后厨接收");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='inventory.handover_segment_scanned' AND business_id=? "
                + "AND idempotency_key IN (?,?)", Integer.class, id, "thr-s-kitchen_receive-1-" + suffix, "thr-s-kitchen_receive-dup-" + suffix))
                .isEqualTo(2);

        scan(id, "staff_pickup", "1").andExpect(status().isOk());
        scan(id, "deliver_customer", "1").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.overall_status").value("exception"));

        send(mid, post("/api/v1/inventory/trace-handovers/exceptions/" + exception.get("id") + "/review"),
                "{\"idempotency_key\":\"thr-r1-" + suffix + "\",\"decision\":\"approved\",\"reason\":\"误扫\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(jdbc.queryForObject("SELECT overall_status FROM inventory_trace_handovers WHERE id=?", String.class, id))
                .isEqualTo("completed");
    }

    /**
     * 84-R2（BSM 第 1891 行工作手机「库房和酒水交接扫码」；PGM 第 633 行「工作手机还负责酒水和物品交接扫码」；BSM 第 612 行「平板只做酒吧点酒下单」）：
     * 按工作手机 APK 的请求形状（登记的工作手机、设备签名头、蛇形键）建链、扫库管出库；后厨接收缺扫码就签名上传证据补录、上级批准，
     * 再扫服务员取走与送达，整链完成。过程步骤里人工扫码段记这台已验签的工作手机，补录段不记终端与设备。
     */
    @Test
    void workPhoneHandoverRecordsTheVerifiedPhoneOnScannedSegments() throws Exception {
        String device = "dev_thr_phone_" + suffix;
        String profile = "sp_thr_" + suffix;
        jdbc.update("INSERT INTO staff_profiles(id,project_id,merchant_id,user_id,name,registration_status,status,created_at,updated_at) "
                + "VALUES(?,?,?,?,'交接库管','approved','active',now(),now())", profile, project, merchant, user("clerk"));
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, staff_id, status, bound_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'staff_work_phone', 'space', ?, 'bound', now(), now(), now())", "td_thr_" + suffix, device, "DC-thr-" + suffix, profile);
        KeyPair keys = KeyPairGenerator.getInstance("EC").generateKeyPair();
        jdbc.update("INSERT INTO terminal_device_signing_keys(device_id, public_key, status) VALUES (?, ?, 'active')",
                device, Base64.getEncoder().encodeToString(keys.getPublic().getEncoded()));

        String created = phonePost(device, keys, "/api/v1/inventory/trace-handovers", "{\"idempotency_key\":\"thr-pc-" + suffix
                + "\",\"trace_code\":\"TRC-P-" + suffix + "\",\"device_id\":\"" + device + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(created, "$.data.handover.id");
        phoneScan(device, keys, id, "out_verify").andExpect(status().isOk());

        String upload = "/api/v1/evidence/attachments/upload";
        byte[] photo = {1, 2, 3};
        String parts = "p:business_type=manual_backfill\nf:file:evidence_1.jpg:" + sha256Hex(photo);
        String uploaded = mockMvc.perform(signed(multipart(upload)
                                .file(new MockMultipartFile("file", "evidence_1.jpg", "application/octet-stream", photo))
                                .param("business_type", "manual_backfill"), device, keys, upload, sha256Hex(parts.getBytes(StandardCharsets.UTF_8)))
                        .with(phoneUser()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String submitted = phonePost(device, keys, "/api/v1/inventory/trace-handovers/" + id + "/segments/kitchen_receive/backfill",
                "{\"idempotency_key\":\"thr-pb-" + suffix + "\",\"occurred_at\":\"" + Instant.now()
                        + "\",\"reason\":\"后厨接收时漏扫\",\"responsible_staff_id\":\"" + user("clerk")
                        + "\",\"evidence_attachment_ids\":[\"" + JsonPath.read(uploaded, "$.data.id") + "\"]}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        approve(JsonPath.read(submitted, "$.data.review_id")).andExpect(status().isOk());
        phoneScan(device, keys, id, "staff_pickup").andExpect(status().isOk());
        phoneScan(device, keys, id, "deliver_customer").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handover.overall_status").value("completed"));

        String step = "SELECT terminal_type, device_id FROM business_process_step_records WHERE process_instance_id=? AND step_code=?";
        assertThat(jdbc.queryForMap(step, id, "out_verify")).containsEntry("terminal_type", "staff_work_phone").containsEntry("device_id", device);
        assertThat(jdbc.queryForMap(step, id, "staff_pickup")).containsEntry("terminal_type", "staff_work_phone").containsEntry("device_id", device);
        Map<String, Object> backfilled = jdbc.queryForMap(step, id, "kitchen_receive");
        assertThat(backfilled.get("terminal_type")).isNull();
        assertThat(backfilled.get("device_id")).isNull();
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String createChain() throws Exception {
        String body = send(clerk, post("/api/v1/inventory/trace-handovers"), "{\"idempotency_key\":\"thr-c-" + suffix
                + "\",\"trace_code\":\"TRC-R-" + suffix + "\",\"device_id\":\"dev_thr_" + suffix + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.handover.id");
    }

    private ResultActions scan(String id, String segment, String key) throws Exception {
        return send(clerk, post("/api/v1/inventory/trace-handovers/" + id + "/scan-segment"), "{\"idempotency_key\":\"thr-s-" + segment
                + "-" + key + "-" + suffix + "\",\"segment\":\"" + segment + "\"}");
    }

    private Map<String, Object> stepRow(String id, String segment) {
        return jdbc.queryForMap("SELECT step_status, scan_at FROM inventory_trace_handover_steps WHERE handover_id=? AND segment=?",
                id, segment);
    }

    /** 凭证走真实上传接口（business_type=manual_backfill）。 */
    private String upload() throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "photo.png", "application/octet-stream", new byte[]{1, 2, 3}))
                        .param("business_type", "manual_backfill").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions backfill(String id, String segment, String attachment) throws Exception {
        return send(clerk, post("/api/v1/inventory/trace-handovers/" + id + "/segments/" + segment + "/backfill"),
                "{\"idempotency_key\":\"thr-b-" + suffix + "\",\"occurred_at\":\"" + Instant.now()
                        + "\",\"reason\":\"后厨接收时扫码枪没电，漏扫\",\"responsible_staff_id\":\"" + user("clerk")
                        + "\",\"evidence_attachment_ids\":[\"" + attachment + "\"]}");
    }

    private ResultActions approve(String review) throws Exception {
        return send(mid, post("/api/v1/platform/supervisor-review/" + review + "/approve"),
                "{\"decision\":\"approved\",\"comment\":\"已核对\",\"idempotency_key\":\"ap_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 84-R2 工作手机：同一员工账号（clerk），终端类型报 staff_work_phone。 */
    private RequestPostProcessor phoneUser() {
        AccessActor actor = new AccessActor(user("clerk"), user("clerk"), "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "staff_work_phone");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions phonePost(String device, KeyPair keys, String path, String body) throws Exception {
        return mockMvc.perform(signed(post(path), device, keys, path, sha256Hex(body.getBytes(StandardCharsets.UTF_8)))
                .with(phoneUser()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions phoneScan(String device, KeyPair keys, String id, String segment) throws Exception {
        return phonePost(device, keys, "/api/v1/inventory/trace-handovers/" + id + "/scan-segment",
                "{\"idempotency_key\":\"thr-ps-" + segment + "-" + suffix + "\",\"segment\":\"" + segment + "\"}");
    }

    /** 与 Android deviceRequestHeaders 同式：设备号、毫秒时间戳、nonce、方法、路径、正文摘要逐行拼接后 ECDSA 签名。 */
    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B signed(B request, String device, KeyPair keys, String path, String bodyDigest)
            throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString();
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keys.getPrivate());
        signer.update(String.join("\n", device, Long.toString(timestamp), nonce, "POST", path, bodyDigest).getBytes(StandardCharsets.UTF_8));
        request.header("X-Device-Id", device).header("X-Device-Timestamp", Long.toString(timestamp))
                .header("X-Device-Nonce", nonce).header("X-Device-Signature", Base64.getEncoder().encodeToString(signer.sign()));
        return request;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private String user(String tag) {
        return "usr_thr_" + tag + "_" + suffix;
    }
}
