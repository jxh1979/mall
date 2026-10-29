package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.analytics.AnalyticsService;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第十八批 F 段第 14 条 14-R1 通用人工补录主链真库回归（BSM 第 1642 行「记录缺失时允许人工补录，但必须写原因和责任人。补录时限为业务发生后的
 * 配置天数内……出厂默认 3 天……超时不得补录；补录必须附原始记录凭证（微信截图、通话录音等）并经审核，以通话录音为凭证的，录音起始时间必须早于
 * 客人到店时间、且早于客人开卡时间达到后台配置的时长（……出厂默认 30 分钟……），不满足的不予采信。补录一律进入主管复核队列，复核通过才生效；
 * 涉及金额、销售归属、提成或责任认定的补录，须由被影响方之外的主管复核。补录记录与原始记录并列展示」；BSM 第 1823 行「补录数量」）。
 *
 * <p>三类具体对象在 14-R2/R3/R6 实现；这里用测试内假对象（type=trace_handover_step，@Order 最先）给定业务发生、到店、开卡时刻与被影响方，
 * 凭证经真实上传接口（录入员为商户账号：证据上传走默认商户范围分支），复核走真实组织层级（同商户的录入员 → 主管 → 经理）与真实审批接口，查询与参数按前端蛇形键走真实入口。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ManualBackfillServiceJdbcTests {

    @TestConfiguration
    static class FakeTargetConfig {
        @Bean
        FakeTarget fakeManualBackfillTarget(AuthorizationService auth) {
            return new FakeTarget(auth);
        }
    }

    /** 假补录对象：只按商户闸，事实由测试给定；批准时记下补录单号，changed=true 时按「对象已变」拒绝。 */
    static final class FakeTarget implements ManualBackfillTarget, Ordered {
        private final AuthorizationService auth;
        String projectId;
        String merchantId;
        String beneficiary;
        Instant businessAt;
        Instant arrivalAt;
        Instant openCardAt;
        boolean changed;
        final List<String> applied = new CopyOnWriteArrayList<>();

        FakeTarget(AuthorizationService auth) {
            this.auth = auth;
        }

        @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
        @Override public String type() { return "trace_handover_step"; }
        @Override public String submitPermission() { return "inventory.handover.scan"; }
        @Override public String viewPermission() { return "inventory.handover.scan"; }
        @Override public String policyActionType() { return "inventory.trace.scan_backfill"; }

        @Override
        public void requireScope(String targetId) {
            auth.currentMerchantIdQuietly().filter(m -> !m.equals(merchantId)).ifPresent(m -> {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不是本商户的对象");
            });
        }

        @Override
        public Resolved resolve(String targetId, String part, ManualBackfillService.Request request) {
            return new Resolved(projectId, merchantId, businessAt, businessAt, arrivalAt, openCardAt, beneficiary,
                    Map.of("occurred_at", businessAt.toString()), Map.of("step_status", "pending"), "假对象 " + targetId + " 的 " + part + " 段");
        }

        @Override
        public void requireView(String targetId) {
            requireScope(targetId);
        }

        @Override
        public void apply(ManualBackfillService.Backfill backfill) {
            if (changed) {
                throw ApiException.conflict("MANUAL_BACKFILL_TARGET_CHANGED", "补录对象已变化，请重新核对");
            }
            applied.add(backfill.id());
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ManualBackfillService service;
    @Autowired private FakeTarget fake;
    @Autowired private AnalyticsService analytics;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private PlatformTransactionManager transactions;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_mbf_" + suffix;
    private final String merchant = "mch_mbf_" + suffix;
    private final String otherMerchant = "mch_mbf_o_" + suffix;
    private RequestPostProcessor clerk;
    private RequestPostProcessor mid;
    private RequestPostProcessor outsider;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "补录项目 " + suffix);
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                    + "VALUES (?, ?, 'bar', 'active', now(), now(), ?)", m, "补录商户 " + m, project);
        }
        String role = "role_mbf_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'人工补录回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'inventory.handover.scan','evidence.attachment.create','evidence.timeline.view',"
                + "'governance.supervisor_review.view','governance.supervisor_review.approve')", role);
        clerk = account("clerk", merchant, role);
        mid = account("mid", merchant, role);
        account("boss", merchant, role);
        outsider = account("out", otherMerchant, role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("clerk"), user("mid"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mid"), user("boss"));
        fake.projectId = project;
        fake.merchantId = merchant;
        fake.beneficiary = null;
        fake.businessAt = Instant.now().minus(Duration.ofHours(2));
        fake.arrivalAt = null;
        fake.openCardAt = null;
        fake.changed = false;
        fake.applied.clear();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 时限：业务发生 4 天前、出厂 3 天 → 409；最高权限把本项目改成 5 天后，同一请求再提交即进复核。 */
    @Test
    void submissionAfterTheWindowIsRefusedUntilTheProjectWindowIsWidened() throws Exception {
        fake.businessAt = Instant.now().minus(Duration.ofDays(4));
        String photo = upload("wechat.png");
        var request = request("win", List.of(photo), null);
        assertThat(failure(() -> submit(request))).isEqualTo("MANUAL_BACKFILL_WINDOW_EXPIRED");
        assertThat(rows()).isZero();

        send(master(), put("/api/v1/evidence/manual-backfill-policies/" + project),
                "{\"idempotency_key\":\"pol-" + suffix + "\",\"reason\":\"本项目放宽\",\"window_days\":5,\"window_min_days\":1,\"window_max_days\":10}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.window_days").value(5));
        assertThat(submit(request).status()).isEqualTo("pending");
    }

    /** 凭证：不附 → 400；附了未完成上传的 → 400；原因、责任人缺或责任人账号不存在 → 400 VALIDATION_FAILED。 */
    @Test
    void reasonResponsibleAndUploadedEvidenceAreRequired() throws Exception {
        assertThat(failure(() -> submit(request("none", List.of(), null)))).isEqualTo("MANUAL_BACKFILL_EVIDENCE_REQUIRED");
        String intent = "att_mbf_intent_" + suffix;
        jdbc.update("INSERT INTO evidence_attachments(id,file_name,content_type,file_size,business_type,attachment_role,object_key,upload_url,status,created_at,updated_at) "
                + "VALUES(?,'a.png','image/png',1,'manual_backfill','other','evidence-files/x.png','/x','pending',now(),now())", intent);
        assertThat(failure(() -> submit(request("intent", List.of(intent), null)))).isEqualTo("EVIDENCE_NOT_UPLOADED");

        String photo = upload("wechat.png");
        var base = request("v", List.of(photo), null);
        assertThat(failure(() -> submit(withReason(base, "a", " ", user("clerk"))))).isEqualTo("VALIDATION_FAILED");
        assertThat(failure(() -> submit(withReason(base, "b", "现场漏扫", null)))).isEqualTo("VALIDATION_FAILED");
        assertThat(failure(() -> submit(withReason(base, "c", "现场漏扫", "usr_not_exist_" + suffix)))).isEqualTo("VALIDATION_FAILED");
        assertThat(rows()).isZero();
    }

    /** 通话录音：附 .m4a 不填起始 → 400；起始＝到店前 20 分（出厂 30）→ 400 不予采信；开卡早于到店时按开卡算；到店前 40 分 → 进复核；只附截图不校验录音。 */
    @Test
    void recordingMustStartEarlyEnoughBeforeArrivalAndOpenCard() throws Exception {
        Instant arrival = Instant.now().minus(Duration.ofMinutes(30)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        fake.arrivalAt = arrival;
        String audio = upload("call.m4a");
        assertThat(failure(() -> submit(request("r0", List.of(audio), null)))).isEqualTo("RECORDING_START_REQUIRED");
        assertThat(failure(() -> submit(request("r1", List.of(audio), arrival.minus(Duration.ofMinutes(20))))))
                .isEqualTo("RECORDING_NOT_EARLY_ENOUGH");
        fake.openCardAt = arrival.minus(Duration.ofMinutes(30));
        assertThat(failure(() -> submit(request("r2", List.of(audio), arrival.minus(Duration.ofMinutes(40))))))
                .isEqualTo("RECORDING_NOT_EARLY_ENOUGH");
        fake.openCardAt = null;
        var accepted = submit(request("r3", List.of(audio), arrival.minus(Duration.ofMinutes(40))));
        assertThat(accepted.status()).isEqualTo("pending");
        assertThat(accepted.recordingStartedAt()).isEqualTo(arrival.minus(Duration.ofMinutes(40)));

        String photo = upload("wechat.png");
        assertThat(submit(request("r4", List.of(photo), null)).status()).isEqualTo("pending");
    }

    /** 同一幂等键重放返回第一次的结果、不新增；他商户账号 → 403，不落行、无成功留痕。 */
    @Test
    void replayReturnsTheFirstResultAndOtherMerchantIsRefused() throws Exception {
        String photo = upload("wechat.png");
        var request = request("rp", List.of(photo), null);
        var first = submit(request);
        assertThat(submit(request).id()).isEqualTo(first.id());
        assertThat(rows()).isEqualTo(1);

        String key = "mbf-x-" + suffix;
        actAs("out", otherMerchant);
        Throwable denied = catchThrowable(() -> service.submit("trace_handover_step", "tgt_" + suffix, "kitchen_receive",
                new ManualBackfillService.Request(key, "现场漏扫", user("out"), List.of(photo), null, null, null, null, null, null, null)));
        assertThat(denied).isInstanceOf(ApiException.class);
        assertThat(((ApiException) denied).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN");
        assertThat(rows()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='evidence.manual_backfill_submitted' AND idempotency_key=?",
                Integer.class, key)).isZero();
    }

    /**
     * 复核：进复核单（动作类型 inventory.trace.scan_backfill、原件区带补录原文与凭证）；上级批准后才由对象落业务，补录单 approved；
     * 前端按蛇形键查补录记录与原快照并列；他商户账号查 403；补录数量明细按补录类型计入。
     */
    @Test
    void approvalAppliesTheBackfillAndItIsListedBesideTheOriginal() throws Exception {
        String photo = upload("wechat.png");
        var backfill = submit(request("ok", List.of(photo), null));
        assertThat(fake.applied).isEmpty();
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id=?", String.class, backfill.reviewId()))
                .isEqualTo("inventory.trace.scan_backfill");
        assertThat(jdbc.queryForObject("SELECT reason FROM supervisor_review_decisions WHERE review_id=?", String.class, backfill.reviewId()))
                .startsWith("人工补录：假对象 tgt_" + suffix).contains("责任人 " + user("clerk")).contains("原因 现场漏扫");

        approve(mid, backfill.reviewId(), "approved").andExpect(status().isOk());
        assertThat(fake.applied).containsExactly(backfill.id());
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE id=?", String.class, backfill.id())).isEqualTo("approved");
        assertThat(jdbc.queryForObject("SELECT applied_at IS NOT NULL AND decided_at IS NOT NULL FROM manual_backfills WHERE id=?", Boolean.class,
                backfill.id())).isTrue();

        String body = mockMvc.perform(get("/api/v1/evidence/manual-backfills").param("target_type", "trace_handover_step")
                        .param("target_id", "tgt_" + suffix).with(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(backfill.id()))
                .andExpect(jsonPath("$.data[0].status").value("approved"))
                .andExpect(jsonPath("$.data[0].target_part").value("kitchen_receive"))
                .andExpect(jsonPath("$.data[0].original_snapshot.step_status").value("pending"))
                .andExpect(jsonPath("$.data[0].responsible_staff_id").value(user("clerk")))
                .andExpect(jsonPath("$.data[0].evidence_attachment_ids[0]").value(photo))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<Object>>read(body, "$.data")).hasSize(1);
        mockMvc.perform(get("/api/v1/evidence/manual-backfills").param("target_type", "trace_handover_step")
                        .param("target_id", "tgt_" + suffix).with(outsider))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));

        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        LocalDate d = businessDayResolver.businessDate(null, null, Instant.now());
        var detail = analytics.metricDetail("risk.manual_backfill", null, d, d, 1, 20, Set.of(project));
        assertThat(detail.rows()).anySatisfy(row -> assertThat(row).containsEntry("sourceType", "trace_handover_step")
                .containsEntry("id", backfill.id()).containsEntry("status", "approved"));
    }

    /** 被影响方本人不得复核；驳回不落业务。 */
    @Test
    void beneficiaryCannotReviewAndRejectionDoesNotApply() throws Exception {
        fake.beneficiary = user("mid");
        String photo = upload("wechat.png");
        var backfill = submit(request("bn", List.of(photo), null));
        approve(mid, backfill.reviewId(), "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));

        fake.beneficiary = null;
        var other = submit(request("rj", List.of(photo), null));
        approve(mid, other.reviewId(), "rejected").andExpect(status().isOk());
        assertThat(fake.applied).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE id=?", String.class, other.id())).isEqualTo("rejected");
    }

    /** 批准时对象已变：整笔批准回滚，补录单与复核单都仍待审；复核人可改选驳回结案。 */
    @Test
    void approvalRollsBackWhenTheTargetChanged() throws Exception {
        String photo = upload("wechat.png");
        var backfill = submit(request("ch", List.of(photo), null));
        fake.changed = true;
        approveInSavepoint(backfill.reviewId()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_TARGET_CHANGED"));
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE id=?", String.class, backfill.id())).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id=?", String.class,
                backfill.reviewId())).isEqualTo("pending_review");

        approve(mid, backfill.reviewId(), "rejected").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM manual_backfills WHERE id=?", String.class, backfill.id())).isEqualTo("rejected");
        assertThat(fake.applied).isEmpty();
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private ManualBackfillService.Backfill submit(ManualBackfillService.Request request) {
        actAs("clerk", merchant);
        return service.submit("trace_handover_step", "tgt_" + suffix, "kitchen_receive", request);
    }

    private ManualBackfillService.Request request(String key, List<String> files, Instant recording) {
        return new ManualBackfillService.Request("mbf-" + key + "-" + suffix, "现场漏扫", user("clerk"), files, recording, null,
                null, null, null, null, null);
    }

    private static ManualBackfillService.Request withReason(ManualBackfillService.Request base, String key, String reason, String responsible) {
        return new ManualBackfillService.Request(base.idempotencyKey() + key, reason, responsible, base.evidenceAttachmentIds(),
                base.recordingStartedAt(), null, null, null, null, null, null);
    }

    private int rows() {
        return jdbc.queryForObject("SELECT count(*) FROM manual_backfills WHERE target_id=?", Integer.class, "tgt_" + suffix);
    }

    private static String failure(ThrowingCallable call) {
        Throwable error = catchThrowable(call);
        assertThat(error).isInstanceOf(ApiException.class);
        return ((ApiException) error).code();
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

    private ResultActions send(RequestPostProcessor who, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String body) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private RequestPostProcessor account(String tag, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        return session(new AccessActor(userId, userId, "staff", project, merchantId, "active"));
    }

    private void actAs(String tag, String merchantId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user(tag), user(tag), "staff", project, merchantId, "active"), "test", List.of()));
    }

    private RequestPostProcessor master() {
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                "platform-company", null, "active"));
    }

    private RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_mbf_" + tag + "_" + suffix;
    }
}
