// 文件版本：V0.01
package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementBusinessDayTestFixture;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.gate.AdmissionGuard;
import com.complex.platform.gate.JdbcAdmissionFactRepository;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.support.StaffRequestFixture;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 E 段第 22 条（PGM:531）：证照宽限期按类型配并同配上下限、商户到期后申请、最高权限审批、宽限内放行超期阻断且不得再延、
 * 宽限内每日提醒；预警提前量按类型配（出厂 3 个月）与每日自动扫描；续期登记。真库、真实授权链，请求体按前端蛇形键。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ComplianceGracePeriodJdbcIntegrationTests {

    private static final String BASE = "/api/v1/compliance";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcAdmissionFactRepository facts;
    @Autowired private AdmissionGuard admissionGuard;
    @Autowired private ComplianceService service;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private final LocalDate today = LocalDate.now(LocalZone.ID);
    private String project;
    private String merchant;
    private StaffRequestFixture staffs;
    private RequestPostProcessor merchantUser;
    private RequestPostProcessor platform;

    @BeforeEach
    void seed() {
        project = AmusementBusinessDayTestFixture.complex(jdbc);
        merchant = merchant("a");
        staffs = new StaffRequestFixture(jdbc);
        merchantUser = staffs.staff("cg_m_" + suffix, "staff", project, merchant, "compliance.grace_period.request",
                "compliance.expiry_item.view", "compliance.expiry_item.manage", "compliance.reminder.view");
        platform = staffs.staff("cg_p_" + suffix, "platform_admin", project, null,
                "compliance.expiry_item.manage", "compliance.expiry_item.view");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 22-R2① 平台按证照类型配宽限与预警（六个蛇形键）；三项不同填、越界、保险填宽限、类型不在九类一律 400；商户账号持 manage 也 403。 */
    @Test
    void platformConfiguresGraceAndWarningPerItemTypeWithBounds() throws Exception {
        configure(platform, "business_license", "\"grace_days\":15,\"grace_min_days\":7,\"grace_max_days\":30,"
                + "\"warning_months\":2,\"warning_min_months\":1,\"warning_max_months\":6")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grace_days").value(15))
                .andExpect(jsonPath("$.data.warning_months").value(2));
        invalid(configure(platform, "business_license", "\"grace_days\":15"));
        invalid(configure(platform, "business_license", "\"grace_days\":40,\"grace_min_days\":7,\"grace_max_days\":30"));
        invalid(configure(platform, "business_license", "\"warning_months\":9,\"warning_min_months\":1,\"warning_max_months\":6"));
        invalid(configure(platform, "insurance", "\"grace_days\":15,\"grace_min_days\":7,\"grace_max_days\":30"));
        invalid(configure(platform, "food_permit", "\"warning_months\":3,\"warning_min_months\":1,\"warning_max_months\":6"));
        configure(merchantUser, "business_license", "\"grace_days\":15,\"grace_min_days\":7,\"grace_max_days\":30")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));

        mockMvc.perform(get(BASE + "/item-type-configs").with(merchantUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.data[?(@.item_type == 'business_license')].grace_days").value(contains(15)))
                .andExpect(jsonPath("$.data[?(@.item_type == 'insurance')].warning_months").value(contains(3)))
                .andExpect(jsonPath("$.data[?(@.item_type == 'insurance')].grace_days").value(contains(nullValue())));
    }

    /** 22-R2②③ 商户对昨天（北京日期）到期的执照申请宽限：进最高权限审批、待审照旧阻断；最高权限批准后宽限内放行，过了截止日又阻断、不得再延。 */
    @Test
    void merchantGraceRequestNeedsTopAuthorityAndLiftsBlockOnlyWithinGrace() throws Exception {
        graceConfigured("business_license", 15);
        String id = item(merchant, "business_license", today.minusDays(1));
        String inspection = "insp_cg_" + suffix;

        requestGrace(merchantUser, id, key())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grace_status").value("pending_review"))
                .andExpect(jsonPath("$.data.grace_until").value(today.plusDays(14).toString()));
        String review = graceReview(id);
        assertThat(jdbc.queryForMap("SELECT review_status, approval_level FROM supervisor_review_decisions WHERE review_id = ?", review))
                .containsEntry("review_status", "pending_review")
                .containsEntry("approval_level", "top_authority");
        assertThat(jdbc.queryForList("SELECT approver_kind FROM supervisor_review_nodes WHERE review_id = ?", String.class, review))
                .as("商户账号无上级：链上只有最高权限节点").containsExactly("master");
        asMerchant();
        assertThat(facts.isQualificationAbnormalForMerchant(merchant)).as("待审期间照旧阻断").isTrue();
        assertThatThrownBy(() -> admissionGuard.check("amusement.reopen", "inspection", inspection))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ADMISSION_BLOCKED");

        decide(review, "approved");
        assertThat(graceStatus(id)).isEqualTo("approved");
        asMerchant();
        assertThat(facts.isQualificationAbnormalForMerchant(merchant)).as("宽限内放行").isFalse();
        assertThatCode(() -> admissionGuard.check("amusement.reopen", "inspection", inspection)).doesNotThrowAnyException();
        conflict(requestGrace(merchantUser, id, key()), "GRACE_ALREADY_USED");

        jdbc.update("UPDATE compliance_expiry_items SET grace_until = ? WHERE id = ?", today.minusDays(1), id);
        assertThat(facts.isQualificationAbnormalForMerchant(merchant)).as("超过宽限期仍未更新一律阻断").isTrue();
    }

    /** 22-R2④ 申请负例：未到期、未配置（含保险）、过了宽限窗口、待审再申请、无商户归属、别家商户、条目不存在。 */
    @Test
    void graceRequestRejections() throws Exception {
        graceConfigured("business_license", 15);
        conflict(requestGrace(merchantUser, item(merchant, "business_license", today), key()), "GRACE_NOT_EXPIRED");
        conflict(requestGrace(merchantUser, item(merchant, "custom", today.minusDays(1)), key()), "GRACE_PERIOD_NOT_CONFIGURED");
        conflict(requestGrace(merchantUser, item(merchant, "insurance", today.minusDays(1)), key()), "GRACE_PERIOD_NOT_CONFIGURED");
        conflict(requestGrace(merchantUser, item(merchant, "business_license", today.minusDays(16)), key()), "GRACE_WINDOW_PASSED");
        String pending = item(merchant, "business_license", today.minusDays(1));
        requestGrace(merchantUser, pending, key()).andExpect(status().isOk());
        conflict(requestGrace(merchantUser, pending, key()), "GRACE_REQUEST_PENDING");

        RequestPostProcessor noMerchant = staffs.staff("cg_s_" + suffix, "staff", project, null, "compliance.grace_period.request");
        requestGrace(noMerchant, pending, key()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        requestGrace(merchantUser, item(merchant("b"), "business_license", today.minusDays(1)), key())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        requestGrace(merchantUser, "cei_missing_" + suffix, key()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("COMPLIANCE_EXPIRY_ITEM_NOT_FOUND"));
    }

    /** 22-R2⑤ 同一幂等键重放只生成一张审批单；最高权限驳回后可以再申请。 */
    @Test
    void replayCreatesOneReviewAndRejectedGraceCanBeRequestedAgain() throws Exception {
        graceConfigured("business_license", 15);
        String id = item(merchant, "business_license", today.minusDays(1));
        String key = key();
        requestGrace(merchantUser, id, key).andExpect(status().isOk());
        requestGrace(merchantUser, id, key).andExpect(status().isOk());
        assertThat(reviewCount(id)).isEqualTo(1);

        decide(graceReview(id), "rejected");
        assertThat(graceStatus(id)).isEqualTo("rejected");
        asMerchant();
        assertThat(facts.isQualificationAbnormalForMerchant(merchant)).isTrue();
        requestGrace(merchantUser, id, key()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grace_status").value("pending_review"));
        assertThat(reviewCount(id)).isEqualTo(2);
    }

    /** 22-R2⑥ 宽限期内每日一条 grace 提醒（中文）：同日重扫不重复、次日新增、过了截止日不再生成；商户账号经提醒接口可见。 */
    @Test
    void approvedGraceGetsOneReminderPerDayVisibleToMerchant() throws Exception {
        graceConfigured("business_license", 15);
        String id = item(merchant, "business_license", today.minusDays(1));
        requestGrace(merchantUser, id, key()).andExpect(status().isOk());
        decide(graceReview(id), "approved");

        SecurityContextHolder.clearContext();
        service.scanAllOn(today);
        service.scanAllOn(today);
        assertThat(graceReminderCount(id)).isEqualTo(1);
        service.scanAllOn(today.plusDays(1));
        assertThat(graceReminderCount(id)).isEqualTo(2);
        service.scanAllOn(today.plusDays(15));
        assertThat(graceReminderCount(id)).as("宽限截止日（到期日＋15 天）之后不再生成").isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT message FROM compliance_reminders WHERE source_id = ? AND reminder_type = 'grace' AND due_date = ?",
                String.class, id, today)).isEqualTo("证照-business_license宽限期至" + today.plusDays(14) + "，到期仍未更新将阻断");

        mockMvc.perform(get(BASE + "/reminders").with(merchantUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.source_id == '" + id + "' && @.reminder_type == 'grace')]", hasSize(2)));
    }

    /** 22-R3①② 预警提前量按证照类型：未配置按出厂 3 个月（80 天后到期即预警）；配成 1 个月后 80 天的不预警、20 天的预警；提醒中文。 */
    @Test
    void upcomingWarningFollowsItemTypeMonthsWithFactoryThree() throws Exception {
        String far = item(merchant, "business_license", today.plusDays(80));
        SecurityContextHolder.clearContext();
        service.scanAllOn(today);
        assertThat(reminderTypes(far)).containsExactly("upcoming");
        assertThat(message(far, "upcoming")).isEqualTo("证照-business_license将于" + today.plusDays(80) + "到期");

        configure(platform, "business_license", "\"warning_months\":1,\"warning_min_months\":1,\"warning_max_months\":6")
                .andExpect(status().isOk());
        String far2 = item(merchant, "business_license", today.plusDays(80));
        String near = item(merchant, "business_license", today.plusDays(20));
        service.scanAllOn(today);
        assertThat(reminderTypes(far2)).isEmpty();
        assertThat(reminderTypes(near)).containsExactly("upcoming");
        invalid(configure(platform, "business_license", "\"warning_months\":1"));
        invalid(configure(platform, "business_license", "\"warning_months\":0,\"warning_min_months\":1,\"warning_max_months\":6"));
    }

    /** 22-R3④⑤ 每日任务：没有登录主体也为多个商户生成提醒（中文），商户账号只看到本商户的。 */
    @Test
    void scheduledScanRunsWithoutLoginForAllMerchantsAndMerchantSeesOnlyOwn() throws Exception {
        String mine = item(merchant, "fire_certificate", today.minusDays(3));
        String theirs = item(merchant("c"), "fire_certificate", today.minusDays(3));
        SecurityContextHolder.clearContext();
        assertThat(service.scanAllOn(today)).isPositive();
        assertThat(reminderTypes(mine)).containsExactly("expired");
        assertThat(reminderTypes(theirs)).containsExactly("expired");
        assertThat(message(mine, "expired")).isEqualTo("证照-fire_certificate已于" + today.minusDays(3) + "到期");

        mockMvc.perform(get(BASE + "/reminders").with(merchantUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.source_id == '" + mine + "')]", hasSize(1)))
                .andExpect(jsonPath("$.data.items[?(@.source_id == '" + theirs + "')]", hasSize(0)));
    }

    /** 22-R4① 宽限已过仍未更新的照旧阻断；登记续期（蛇形键）后不再阻断、宽限清空，留痕记新旧有效期止。 */
    @Test
    void renewalLiftsBlockClearsGraceAndRecordsOldAndNewValidTo() throws Exception {
        graceConfigured("business_license", 15);
        String id = item(merchant, "business_license", today.minusDays(10));
        requestGrace(merchantUser, id, key()).andExpect(status().isOk());
        decide(graceReview(id), "approved");
        jdbc.update("UPDATE compliance_expiry_items SET grace_until = ? WHERE id = ?", today.minusDays(1), id);
        asMerchant();
        assertThat(facts.isQualificationAbnormalForMerchant(merchant)).isTrue();

        renew(merchantUser, id, today.plusYears(1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid_to").value(today.plusYears(1).toString()))
                .andExpect(jsonPath("$.data.grace_status").doesNotExist());
        asMerchant();
        assertThat(facts.isQualificationAbnormalForMerchant(merchant)).isFalse();
        assertThat(jdbc.queryForMap("SELECT grace_status, grace_until, grace_review_id FROM compliance_expiry_items WHERE id = ?", id))
                .containsEntry("grace_status", null).containsEntry("grace_until", null).containsEntry("grace_review_id", null);
        assertThat(jdbc.queryForMap("SELECT metadata->>'previous_valid_to' AS before, metadata->>'valid_to' AS after FROM audit_events"
                + " WHERE event_name = 'compliance.expiry_item.renewed' AND business_id = ?", id))
                .containsEntry("before", today.minusDays(10).toString())
                .containsEntry("after", today.plusYears(1).toString());
    }

    /** 22-R4②③ 新有效期止不晚于原值 400；不持 manage 403；续别家商户的 403；续期后再到期可重新申请宽限，旧审批单回来也不回写。 */
    @Test
    void renewalRejectionsAndNewGraceAfterRenewedItemExpiresAgain() throws Exception {
        graceConfigured("business_license", 15);
        String id = item(merchant, "business_license", today.minusDays(10));
        requestGrace(merchantUser, id, key()).andExpect(status().isOk());
        decide(graceReview(id), "approved");

        invalid(renew(merchantUser, id, today.minusDays(10)));
        renew(staffs.staff("cg_v_" + suffix, "staff", project, merchant, "compliance.expiry_item.view"), id, today.plusYears(1))
                .andExpect(status().isForbidden());
        renew(merchantUser, item(merchant("d"), "business_license", today.minusDays(1)), today.plusYears(1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));

        renew(merchantUser, id, today.minusDays(1)).andExpect(status().isOk());
        requestGrace(merchantUser, id, key()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grace_until").value(today.plusDays(14).toString()));
        String pendingReview = graceReview(id);
        renew(merchantUser, id, today.plusYears(1)).andExpect(status().isOk());
        decide(pendingReview, "approved");
        assertThat(graceStatus(id)).as("续期后旧的宽限审批结果不回写").isNull();
    }

    private ResultActions configure(RequestPostProcessor who, String type, String fields) throws Exception {
        return mockMvc.perform(put(BASE + "/item-type-configs/" + type).with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key() + "\"," + fields + "}"));
    }

    private void graceConfigured(String type, int days) throws Exception {
        configure(platform, type, "\"grace_days\":" + days + ",\"grace_min_days\":1,\"grace_max_days\":60").andExpect(status().isOk());
    }

    private ResultActions requestGrace(RequestPostProcessor who, String itemId, String key) throws Exception {
        return mockMvc.perform(post(BASE + "/expiry-items/" + itemId + "/grace-requests").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + "\",\"reason\":\"新证在办，等待发证\"}"));
    }

    private ResultActions renew(RequestPostProcessor who, String itemId, LocalDate validTo) throws Exception {
        return mockMvc.perform(post(BASE + "/expiry-items/" + itemId + "/renewal").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key() + "\",\"valid_to\":\"" + validTo + "\",\"document_no\":\"LIC-2\"}"));
    }

    private static void invalid(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    private static void conflict(ResultActions result, String code) throws Exception {
        result.andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value(code));
    }

    /** 最高权限经真实审批接口裁决（商户账号无上级，链上只有最高权限节点）。 */
    private void decide(String review, String decision) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            AmusementBusinessDayTestFixture.master();
            reviews.approve(review, new SupervisorReviewApprovalRequest("cg-" + UUID.randomUUID(), "复核", List.of(), null,
                    decision, "核实新证办理进度"));
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    /** 准入闸与到期判定按当前登录商户取商户归属。 */
    private void asMerchant() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor("cg_m_" + suffix, "cg_m_" + suffix, "staff", project, merchant, "active"), null, List.of()));
    }

    private String merchant(String tag) {
        String id = "cg_mer_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)"
                + " VALUES(?,?,'bar',?,'active',now(),now())", id, id, project);
        return id;
    }

    private String item(String merchantId, String type, LocalDate validTo) {
        String id = "cei_cg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbc.update("""
                INSERT INTO compliance_expiry_items (id, subject_type, subject_id, merchant_id, item_type, item_name, document_no, valid_to,
                    reminder_days, expiry_action, status, owner_user_id, source_type, human_confirmed, evidence_attachment_ids, created_at, updated_at)
                VALUES (?, 'merchant', ?, ?, ?, ?, 'LIC-1', ?, '[]'::jsonb, 'block_operation', 'active', 'owner', 'manual', TRUE, '[]'::jsonb, now(), now())
                """, id, merchantId, merchantId, type, "证照-" + type, validTo);
        return id;
    }

    private String key() {
        return "cg-" + UUID.randomUUID();
    }

    private String graceReview(String itemId) {
        return jdbc.queryForObject("SELECT grace_review_id FROM compliance_expiry_items WHERE id = ?", String.class, itemId);
    }

    private String graceStatus(String itemId) {
        return jdbc.queryForObject("SELECT grace_status FROM compliance_expiry_items WHERE id = ?", String.class, itemId);
    }

    private int reviewCount(String itemId) {
        return jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_type = 'compliance_expiry_item'"
                + " AND object_id = ?", Integer.class, itemId);
    }

    private int graceReminderCount(String itemId) {
        return jdbc.queryForObject("SELECT count(*) FROM compliance_reminders WHERE source_id = ? AND reminder_type = 'grace'",
                Integer.class, itemId);
    }

    private List<String> reminderTypes(String itemId) {
        return jdbc.queryForList("SELECT reminder_type FROM compliance_reminders WHERE source_id = ? ORDER BY reminder_type",
                String.class, itemId);
    }

    private String message(String itemId, String type) {
        return jdbc.queryForObject("SELECT message FROM compliance_reminders WHERE source_id = ? AND reminder_type = ?",
                String.class, itemId, type);
    }
}
