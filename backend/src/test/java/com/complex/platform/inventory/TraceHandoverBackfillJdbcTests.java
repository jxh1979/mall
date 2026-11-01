package com.complex.platform.inventory;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 F 段第 14 条 14-R2 交接链缺段人工补录真库回归（BSM 第 1201 行「酒水与可追责物品的交接，每一步都必须扫码——库管出库、后厨接收、
 * 服务员取走、送达客人或卡座，缺任何一步即视为该环节工作未完成……扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）并经主管复核后
 * 方生效，补录记录与原始扫码记录并列展示、互不覆盖。」）。建链、扫码、凭证上传、补录、查链与复核都按前端蛇形键走真实入口与
 * 真实授权链；复核走真实组织层级（同商户的录入员 → 主管 → 经理）。机器人配送段不计入完成（第十八批 I 段 85-R1），本类不打开机器人开关。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TraceHandoverBackfillJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_thb_" + suffix;
    private final String merchant = "mch_thb_" + suffix;
    private final String otherMerchant = "mch_thb_o_" + suffix;
    private RequestPostProcessor clerk;
    private RequestPostProcessor mid;
    private RequestPostProcessor outsider;
    private String order;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "交接补录项目 " + suffix);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                    + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", m, "交接补录商户 " + m, project);
        }
        order = TraceHandoverOutboundOrderJdbcTests.seedBottleOrder(jdbc, "thb_" + suffix, merchant, "TRC-" + suffix);
        String role = "role_thb_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'交接补录回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'inventory.handover.scan','evidence.attachment.create',"
                + "'governance.supervisor_review.view','governance.supervisor_review.approve')", role);
        clerk = account("clerk", merchant, role);
        mid = account("mid", merchant, role);
        account("boss", merchant, role);
        outsider = account("out", otherMerchant, role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("clerk"), user("mid"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mid"), user("boss"));
    }

    /**
     * 跳过后厨接收扫码，整链不完成、直接扫服务员取走 409（85-R1）；按前端蛇形键补录后厨接收段 → 待复核、链仍未完成、链视图并列一条待复核补录；主管批准 → 放行服务员取走
     * （85-R1），扫完服务员取走、送达客人后整链完成，后厨接收原段行（待扫码、无扫码人、无扫码时间）不变、补录单已通过、过程步骤记责任人与人工录入；
     * 该段再补 409；按溯源码查得到。
     */
    @Test
    void backfillOfTheSkippedSegmentCompletesTheChainOnlyAfterApprovalAndKeepsTheOriginalScan() throws Exception {
        String id = chainSkippingKitchen();
        assertThat(read(view(clerk, id), "$.data.handover.overall_status")).isEqualTo("in_progress");
        send(clerk, post("/api/v1/inventory/trace-handovers/" + id + "/scan-segment"), "{\"idempotency_key\":\"ths-early-" + suffix
                + "\",\"segment\":\"staff_pickup\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HANDOVER_PREVIOUS_SEGMENT_INCOMPLETE"));

        String photo = upload("wechat.png");
        String submitted = backfill(clerk, id, "kitchen_receive", "k1", Instant.now(), user("clerk"), photo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.target_type").value("trace_handover_step"))
                .andExpect(jsonPath("$.data.target_part").value("kitchen_receive"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String backfillId = JsonPath.read(submitted, "$.data.id");
        String review = JsonPath.read(submitted, "$.data.review_id");
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("inventory.trace.scan_backfill");

        String pending = view(clerk, id);
        assertThat(read(pending, "$.data.handover.overall_status")).isEqualTo("in_progress");
        assertThat(JsonPath.<List<Object>>read(pending, "$.data.backfills")).hasSize(1);
        assertThat(read(pending, "$.data.backfills[0].status")).isEqualTo("pending");

        approve(mid, review, "approved").andExpect(status().isOk());

        String released = view(clerk, id);
        assertThat(read(released, "$.data.handover.overall_status")).isEqualTo("in_progress");
        assertThat(read(released, "$.data.handover.current_segment")).isEqualTo("staff_pickup");
        scan(id, "staff_pickup");
        scan(id, "deliver_customer");

        String done = view(clerk, id);
        assertThat(read(done, "$.data.handover.overall_status")).isEqualTo("completed");
        assertThat(read(done, "$.data.backfills[0].id")).isEqualTo(backfillId);
        assertThat(read(done, "$.data.backfills[0].status")).isEqualTo("approved");
        assertThat(JsonPath.<List<Object>>read(done, "$.data.steps[?(@.segment=='kitchen_receive')].step_status")).containsExactly("pending");
        Map<String, Object> original = jdbc.queryForMap("SELECT step_status, operator_user_id, scan_at FROM inventory_trace_handover_steps "
                + "WHERE handover_id=? AND segment='kitchen_receive'", id);
        assertThat(original.get("step_status")).isEqualTo("pending");
        assertThat(original.get("operator_user_id")).isNull();
        assertThat(original.get("scan_at")).isNull();
        Map<String, Object> step = jdbc.queryForMap("SELECT actor_id, capture_source, requires_manual_reason FROM business_process_step_records "
                + "WHERE id=?", id + "_step_kitchen_receive");
        assertThat(step).containsEntry("actor_id", user("clerk")).containsEntry("capture_source", "manual_input")
                .containsEntry("requires_manual_reason", true);
        assertThat(jdbc.queryForObject("SELECT status FROM business_process_instances WHERE id=?", String.class, id)).isEqualTo("completed");

        backfill(clerk, id, "kitchen_receive", "k2", Instant.now(), user("clerk"), photo)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("HANDOVER_STEP_NOT_MISSING"));
        String found = mockMvc.perform(get("/api/v1/inventory/trace-handovers").param("trace_code", "TRC-" + suffix).with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(found, "$.data[*].id")).containsExactly(id);
    }

    /** 已扫码段 409；机器人配送段 400；原时点早于建链 400；他商户账号补录、查链 403 且按溯源码查不到。 */
    @Test
    void onlyMissingStaffSegmentsOfOwnMerchantCanBeBackfilled() throws Exception {
        String id = chainSkippingKitchen();
        String photo = upload("wechat.png");
        backfill(clerk, id, "out_verify", "o", Instant.now(), user("clerk"), photo)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("HANDOVER_STEP_NOT_MISSING"));
        backfill(clerk, id, "dispatch", "d", Instant.now(), user("clerk"), photo)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("HANDOVER_SEGMENT_NOT_STAFF"));
        backfill(clerk, id, "kitchen_receive", "early", Instant.now().minusSeconds(3600), user("clerk"), photo)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        backfill(outsider, id, "kitchen_receive", "x", Instant.now(), user("out"), photo)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/inventory/trace-handovers/" + id).with(outsider))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        String none = mockMvc.perform(get("/api/v1/inventory/trace-handovers").param("trace_code", "TRC-" + suffix).with(outsider))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<Object>>read(none, "$.data")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM manual_backfills WHERE target_id=?", Integer.class, id)).isZero();
    }

    /** 责任人本人不得复核；建链已过补录时限（出厂 3 天）409。 */
    @Test
    void responsiblePersonCannotReviewAndTheWindowRunsFromChainCreation() throws Exception {
        String id = chainSkippingKitchen();
        String photo = upload("wechat.png");
        String submitted = backfill(clerk, id, "kitchen_receive", "self", Instant.now(), user("mid"), photo)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        approve(mid, JsonPath.read(submitted, "$.data.review_id"), "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));

        jdbc.update("UPDATE inventory_trace_handovers SET created_at = created_at - interval '4 days' WHERE id=?", id);
        backfill(clerk, id, "kitchen_receive", "late", Instant.now(), user("clerk"), photo)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_WINDOW_EXPIRED"));
    }

    /** 提交后、批准前该段被正常扫码：批准 409、整笔批准回滚，补录单与复核单仍待审。 */
    @Test
    void approvalIsRefusedWhenTheSegmentWasScannedMeanwhile() throws Exception {
        String id = chainSkippingKitchen();
        String photo = upload("wechat.png");
        String submitted = backfill(clerk, id, "kitchen_receive", "race", Instant.now(), user("clerk"), photo)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String review = JsonPath.read(submitted, "$.data.review_id");
        scan(id, "kitchen_receive");

        approveInSavepoint(review).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_TARGET_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE review_id=?", String.class, review)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id=?", String.class, review))
                .isEqualTo("pending_review");
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    /** 建链 → 扫库管出库，跳过后厨接收（链停在后厨接收）。 */
    private String chainSkippingKitchen() throws Exception {
        String body = send(clerk, post("/api/v1/inventory/trace-handovers"), "{\"idempotency_key\":\"thc-" + suffix
                + "\",\"trace_code\":\"TRC-" + suffix + "\",\"order_id\":\"" + order + "\",\"device_id\":\"dev_thb_" + suffix + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(body, "$.data.handover.id");
        scan(id, "out_verify");
        return id;
    }

    private void scan(String id, String segment) throws Exception {
        send(clerk, post("/api/v1/inventory/trace-handovers/" + id + "/scan-segment"), "{\"idempotency_key\":\"ths-" + segment + "-"
                + suffix + "\",\"segment\":\"" + segment + "\"}").andExpect(status().isOk());
    }

    private ResultActions backfill(RequestPostProcessor who, String id, String segment, String key, Instant occurredAt,
            String responsible, String attachment) throws Exception {
        return send(who, post("/api/v1/inventory/trace-handovers/" + id + "/segments/" + segment + "/backfill"),
                "{\"idempotency_key\":\"thb-" + key + "-" + suffix + "\",\"occurred_at\":\"" + occurredAt
                        + "\",\"reason\":\"后厨接收时扫码枪没电，漏扫\",\"responsible_staff_id\":\"" + responsible
                        + "\",\"evidence_attachment_ids\":[\"" + attachment + "\"]}");
    }

    private String view(RequestPostProcessor who, String id) throws Exception {
        return mockMvc.perform(get("/api/v1/inventory/trace-handovers/" + id).with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String read(String json, String path) {
        return JsonPath.read(json, path);
    }

    /** 凭证走真实上传接口（business_type=manual_backfill）。 */
    private String upload(String fileName) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", fileName, "application/octet-stream", new byte[]{1, 2, 3}))
                        .param("business_type", "manual_backfill").with(clerk))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data.id");
    }

    private ResultActions approve(RequestPostProcessor who, String review, String decision) throws Exception {
        return send(who, post("/api/v1/platform/supervisor-review/" + review + "/approve"), "{\"decision\":\"" + decision
                + "\",\"comment\":\"已核对\",\"idempotency_key\":\"ap_" + UUID.randomUUID() + "\"}");
    }

    /** 批准放在保存点里：批准事务抛错被标为只回滚时回到保存点，等同生产中整笔批准回滚。 */
    private ResultActions approveInSavepoint(String review) {
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        ResultActions[] result = new ResultActions[1];
        try {
            nested.executeWithoutResult(tx -> {
                try {
                    result[0] = approve(mid, review, "approved");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (UnexpectedRollbackException rolledBack) {
            // 预期：批准失败把事务标为只回滚，保存点已回滚
        }
        return result[0];
    }

    private ResultActions send(RequestPostProcessor who, MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private RequestPostProcessor account(String tag, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_thb_" + tag + "_" + suffix;
    }
}
