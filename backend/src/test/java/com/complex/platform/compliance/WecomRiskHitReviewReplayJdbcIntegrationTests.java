package com.complex.platform.compliance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.support.StaffRequestFixture;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BG 段第 261 条 261-B22-R1、261-B22-R2（真库、真实入口、真实授权链、蛇形键）。
 * 依据：PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」；PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」
 * 「两个操作同时发生冲突时，一律以“先处理成功”的那个为准」。
 * 企微违规命中复核带请求编号：同一编号重发返回首次处置结果并提示已完成、不重复留痕；不带编号 400；命中已被领取后别人再领取不生效、不改写复核人。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class WecomRiskHitReviewReplayJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "prj_b22_" + s;
    private RequestPostProcessor reviewerA;
    private RequestPostProcessor reviewerB;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "企微命中复核重发回归综合体" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        reviewerA = staffs.staff("usr_b22_a_" + s, "staff", project, null, "wecom.risk_hit.view", "wecom.risk_hit.review");
        reviewerB = staffs.staff("usr_b22_b_" + s, "staff", project, null, "wecom.risk_hit.view", "wecom.risk_hit.review");
    }

    /** 261-B22-R1：确认违规同一编号连发两次，第二次返回首次结果并提示已完成，成功留痕只一条；首次响应不带已完成标记。 */
    @Test
    void confirmResentWithSameKeyReturnsFirstResultOnce() throws Exception {
        String hit = hit("c");
        String body = "{\"idempotency_key\":\"b22-confirm-" + s + "\",\"status\":\"confirmed\",\"review_note\":\"确认违规\"}";
        review(reviewerA, hit, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("confirmed"))
                .andExpect(jsonPath("$.replayed").doesNotExist());
        review(reviewerA, hit, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.message").value(ApiResponse.REPLAYED_MESSAGE))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
        assertThat(reviewedEvents(hit)).isEqualTo(1);
    }

    /** 261-B22-R1：不带请求编号 400，命中不变、不落成功留痕。 */
    @Test
    void reviewWithoutKeyIsRejected() throws Exception {
        String hit = hit("n");
        review(reviewerA, hit, "{\"status\":\"confirmed\",\"review_note\":\"确认违规\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(state(hit)).isEqualTo("open/");
        assertThat(reviewedEvents(hit)).isZero();
    }

    /**
     * 261-B22-R2：甲领取成功；乙换新编号再领取 409、不改写复核人、不多留痕；甲同一编号重发返回首次结果；甲随后确认违规成功。
     * 261-B22-R1：领取（reviewing）同键重发留痕一条。
     */
    @Test
    void claimedHitCannotBeClaimedAgainByAnotherReviewer() throws Exception {
        String hit = hit("k");
        String claimA = "{\"idempotency_key\":\"b22-claim-a-" + s + "\",\"status\":\"reviewing\"}";
        review(reviewerA, hit, claimA).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("reviewing"));
        assertThat(state(hit)).isEqualTo("reviewing/usr_b22_a_" + s);

        review(reviewerB, hit, "{\"idempotency_key\":\"b22-claim-b-" + s + "\",\"status\":\"reviewing\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("WECOM_RISK_HIT_ALREADY_CLAIMED"));
        assertThat(state(hit)).isEqualTo("reviewing/usr_b22_a_" + s);
        assertThat(reviewedEvents(hit)).isEqualTo(1);

        review(reviewerA, hit, claimA).andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true))
                .andExpect(jsonPath("$.data.status").value("reviewing"));
        assertThat(reviewedEvents(hit)).isEqualTo(1);

        review(reviewerA, hit, "{\"idempotency_key\":\"b22-confirm-a-" + s + "\",\"status\":\"confirmed\",\"review_note\":\"核实属实\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
        assertThat(state(hit)).isEqualTo("confirmed/usr_b22_a_" + s);
        assertThat(reviewedEvents(hit)).isEqualTo(2);
    }

    // ---------- 夹具与请求 ----------

    /** 本项目里一条会话、一条消息与一条待复核（open）的企微违规命中。 */
    private String hit(String tag) {
        String conversationId = "wconv_b22_" + tag + "_" + s;
        String messageId = "wmsg_b22_" + tag + "_" + s;
        String hitId = "whit_b22_" + tag + "_" + s;
        jdbc.update("INSERT INTO wecom_conversations (id, external_conversation_id, project_id) VALUES (?, ?, ?)",
                conversationId, "ext_b22_" + tag + "_" + s, project);
        jdbc.update("INSERT INTO wecom_messages (id, conversation_id) VALUES (?, ?)", messageId, conversationId);
        jdbc.update("INSERT INTO wecom_risk_hits (hit_id, conversation_id, message_id, rule_code, risk_level, status, project_id) "
                + "VALUES (?, ?, ?, 'wecom_b22', 'high', 'open', ?)", hitId, conversationId, messageId, project);
        return hitId;
    }

    private ResultActions review(RequestPostProcessor who, String hitId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/compliance/wecom/risk-hits/" + hitId + "/review").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String state(String hitId) {
        return jdbc.queryForObject("SELECT status || '/' || coalesce(reviewer_id, '') FROM wecom_risk_hits WHERE hit_id=?", String.class, hitId);
    }

    private int reviewedEvents(String hitId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='wecom.risk.hit_reviewed' AND business_id=?",
                Integer.class, hitId);
    }
}
